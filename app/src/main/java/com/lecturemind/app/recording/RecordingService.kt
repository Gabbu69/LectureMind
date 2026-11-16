package com.lecturemind.app.recording

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.lecturemind.app.*
import com.lecturemind.app.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlin.math.abs

data class RecordingState(val lectureId: String? = null, val durationMs: Long = 0, val paused: Boolean = false, val level: Float = 0f, val saving: Boolean = false)

/** One continuous AudioRecord stream; rotating the output file never restarts the microphone. */
class RecordingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var endMessage = ""
    private var recorder: AudioRecord? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var captureJob: Job? = null
    private val app get() = application as LectureMindApp
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("recording", "Lecture recording", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START -> {
                if (running || captureJob?.isActive == true) return START_NOT_STICKY
                val id = intent.getStringExtra("id") ?: return START_NOT_STICKY
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    scope.launch { app.database.dao().status(id, "saved", "Microphone permission is needed. No audio was recorded."); stopSelf() }
                    return START_NOT_STICKY
                }
                try {
                    ServiceCompat.startForeground(this, 12, notification(false), if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0)
                    running = true; paused = false; endMessage = ""
                    state.value = RecordingState(id)
                    captureJob = scope.launch { capture(id) }
                } catch (_: Exception) {
                    scope.launch { app.database.dao().status(id, "saved", "Android could not start the microphone. Open the app and try again."); stopSelf() }
                }
            }
            PAUSE -> if (running) {
                paused = !paused
                state.value = state.value.copy(paused = paused, level = 0f)
                scope.launch { state.value.lectureId?.let { app.database.dao().status(it, if (paused) "paused" else "recording") } }
                getSystemService(NotificationManager::class.java).notify(12, notification(paused))
            }
            MARK -> if (running) {
                val snapshot=state.value
                snapshot.lectureId?.let { id -> scope.launch { runCatching { app.database.dao().bookmark(Bookmark(lectureId=id,positionMs=snapshot.durationMs)) } } }
            }
            STOP -> { running = false; state.value = state.value.copy(saving = true) }
        }
        return START_NOT_STICKY
    }
    private fun notification(isPaused: Boolean): Notification {
        fun action(action: String) = PendingIntent.getService(this, action.hashCode(), Intent(this, RecordingService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, "recording").setSmallIcon(R.drawable.ic_notification).setContentTitle(if(isPaused) "LectureMind · Paused" else "LectureMind · Recording")
            .setContentText("${formatTime(state.value.durationMs)} · Tap to open your lecture").setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(0, if(isPaused) "Resume" else "Pause", action(PAUSE)).addAction(0, "Important", action(MARK)).addAction(0, "Save", action(STOP)).build()
    }
    private suspend fun capture(id: String) {
        val dao = app.database.dao()
        var file: File? = null
        var output: java.io.RandomAccessFile? = null
        var index = 0; var partBytes = 0L; var totalBytes = 0L; var partStart = 0L
        var lastCheckpoint = 0L
        var part: AudioPart? = null
        try {
            check(File(filesDir, "audio").apply { mkdirs() }.usableSpace > 64L * 1024 * 1024) { "Low storage. Free at least 64 MB before recording." }
            val minimum = AudioRecord.getMinBufferSize(WavFile.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            check(minimum > 0) { "This phone could not initialize speech recording." }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Microphone permission is needed to record.")
            }
            val audio = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(AudioFormat.Builder().setSampleRate(WavFile.SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(maxOf(minimum * 4, 32000)).build()
            recorder = audio
            check(audio.state == AudioRecord.STATE_INITIALIZED) { "Microphone is unavailable. Close other audio apps and try again." }
            wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LectureMind:recording").apply { acquire(12 * 60 * 60 * 1000L) }
            audio.startRecording()
            check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "The microphone could not start." }
            dao.status(id, "recording")
            val buffer = ByteArray(6400)
            var microphonePaused = false
            while (running && currentCoroutineContext().isActive) {
                if (paused) {
                    if (!microphonePaused) { audio.stop(); microphonePaused = true }
                    state.value = state.value.copy(level = 0f)
                    delay(100)
                    continue
                }
                if (microphonePaused) { audio.startRecording(); microphonePaused = false }
                val read = audio.read(buffer, 0, buffer.size)
                check(read >= 0) { "Microphone disconnected. Saved audio has been retained." }
                if (paused || read == 0) { state.value = state.value.copy(level = 0f); continue }
                if (Build.VERSION.SDK_INT >= 29 && audio.activeRecordingConfiguration?.isClientSilenced == true) { endMessage = "Android silenced the microphone, possibly for a call or another app. Recording stopped; saved audio is retained."; break }
                if (output == null) {
                    partStart = totalBytes * 1000 / WavFile.BYTES_PER_SECOND
                    file = File(filesDir, "audio/$id/${index.toString().padStart(4,'0')}.wav")
                    output = WavFile.create(file)
                    part = AudioPart("$id-$index", id, index, file.absolutePath, partStart)
                    dao.putAudio(part); index++; partBytes = 0
                }
                val currentFile = requireNotNull(file)
                val currentPart = requireNotNull(part)
                val sink = requireNotNull(output)
                val bytes = read / 2 * 2
                sink.write(buffer, 0, bytes); partBytes += bytes; totalBytes += bytes
                var peak = 0
                for (i in 0 until bytes step 2) peak = maxOf(peak, abs(((buffer[i].toInt() and 255) or (buffer[i+1].toInt() shl 8)).toShort().toInt()))
                val elapsed = totalBytes * 1000 / WavFile.BYTES_PER_SECOND
                state.value = state.value.copy(durationMs = elapsed, level = (peak / 16000f).coerceIn(0f, 1f))
                if (elapsed - lastCheckpoint >= 2000) {
                    sink.fd.sync(); dao.duration(id, elapsed); lastCheckpoint = elapsed
                    if (currentFile.usableSpace < 32L * 1024 * 1024) { endMessage = "Recording stopped because storage is low. Your saved audio is safe."; break }
                    getSystemService(NotificationManager::class.java).notify(12, notification(paused))
                }
                if (partBytes >= WavFile.SEGMENT_BYTES) {
                    sink.close(); output = null
                    val duration = WavFile.repair(currentFile)
                    dao.putAudio(currentPart.copy(durationMs = duration, finalized = true))
                }
            }
        } catch (_: SecurityException) {
            endMessage = "Microphone permission was removed. Saved audio is retained."
        } catch (e: Exception) {
            endMessage = when(e) { is SecurityException -> "Microphone permission was removed. Saved audio is retained."; else -> e.message?.takeIf { it.length < 160 } ?: "Recording interrupted. Saved audio is retained." }
        } finally {
            running = false
            runCatching { recorder?.stop() }; recorder?.release(); recorder = null
            runCatching { output?.close() }
            val finalFile = file
            val finalPart = part
            withContext(NonCancellable) {
                if (finalFile != null && finalPart != null) {
                    val duration = WavFile.repair(finalFile)
                    dao.putAudio(finalPart.copy(durationMs = duration, finalized = true))
                }
                dao.duration(id, dao.audio(id).sumOf { it.durationMs })
                dao.status(id, "saved", endMessage)
            }
            if (wakeLock?.isHeld == true) wakeLock?.release()
            state.value = RecordingState()
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
        }
    }
    override fun onDestroy() { running = false; super.onDestroy() }
    companion object {
        const val START = "record.start"; const val PAUSE = "record.pause"; const val MARK = "record.mark"; const val STOP = "record.stop"
        val state = MutableStateFlow(RecordingState())
        fun command(context: Context, action: String, id: String? = null) {
            val intent = Intent(context, RecordingService::class.java).setAction(action).putExtra("id", id)
            if(action == START) ContextCompat.startForegroundService(context, intent) else context.startService(intent)
        }
    }
}
fun formatTime(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return if(seconds >= 3600) "%d:%02d:%02d".format(seconds/3600, seconds/60%60, seconds%60) else "%02d:%02d".format(seconds/60, seconds%60)
}
