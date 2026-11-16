package com.lecturemind.app

import android.app.Application
import com.lecturemind.app.data.*
import com.lecturemind.app.recording.WavFile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class LectureMindApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val ready = MutableStateFlow(false)
    val database by lazy { LectureDatabase.get(this) }
    val settings by lazy { SecureSettings(this) }
    override fun onCreate() {
        super.onCreate()
        scope.launch {
            try {
                database.dao().all().filter { it.status in listOf("recording", "paused", "stopping") }.forEach { lecture ->
                    val parts = database.dao().audio(lecture.id)
                    var total = 0L
                    parts.forEach { part ->
                        val duration = WavFile.repair(File(part.path))
                        database.dao().putAudio(part.copy(startMs = total, durationMs = duration, finalized = true)); total += duration
                    }
                    database.dao().duration(lecture.id, total)
                    database.dao().status(lecture.id, "saved", "Recording was interrupted. Recovered $total ms of saved audio; microphone is off.")
                }
            } finally { ready.value = true }
        }
    }
}
