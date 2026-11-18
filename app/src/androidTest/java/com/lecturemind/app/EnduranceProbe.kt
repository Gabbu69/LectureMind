package com.lecturemind.app

import android.app.Instrumentation
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.lecturemind.app.data.AudioPart
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class EnduranceProbe(private val instrumentation: Instrumentation,private val app: LectureMindApp) {
    private val memory=JSONArray()
    private val directory=File(app.getExternalFilesDir(null),"verification").apply{mkdirs()}
    private val resultFile=File(directory,"endurance-result.json")
    fun tick(elapsed: Long) {
        memory.put(JSONObject().put("elapsedMs",elapsed).put("pssKb",android.os.Debug.getPss()))
        File(directory,"endurance-progress.json").writeText(memory.toString(2))
    }
    suspend fun verifyAndReport(parts: List<AudioPart>,locked: Boolean) {
        val rows=JSONArray()
        var start=0L
        parts.forEachIndexed { index,part ->
            assertEquals(start,part.startMs)
            if(index<parts.lastIndex)assertTrue(part.durationMs in 300000..300200)
            val file=File(part.path)
            val header=ByteArray(44)
            RandomAccessFile(file,"r").use{it.readFully(header)}
            val payload=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(40).toLong()
            assertEquals(file.length()-44,payload)
            rows.put(JSONObject().put("index",index).put("startMs",part.startMs).put("durationMs",part.durationMs).put("bytes",file.length()).put("headerDataBytes",payload))
            start+=part.durationMs
        }
        playback(parts)
        resultFile.writeText(JSONObject().put("durationMs",start).put("bytes",parts.sumOf{File(it.path).length()})
            .put("screenLocked",locked).put("parts",rows).put("memorySamples",memory)
            .put("boundaryPlayback",true).put("audioUploaded",false).put("audioDeletedAfterChecks",false).toString(2))
    }
    fun markAudioDeleted() {
        val result=JSONObject(resultFile.readText()).put("audioDeletedAfterChecks",true)
        resultFile.writeText(result.toString(2))
    }
    private suspend fun playback(parts: List<AudioPart>) {
        val player=AtomicReference<ExoPlayer>()
        val ready=AtomicBoolean(false)
        val error=AtomicReference<PlaybackException>()
        instrumentation.runOnMainSync {
            val instance=ExoPlayer.Builder(app).build()
            player.set(instance)
            instance.volume=0f
            instance.addListener(object:Player.Listener {
                override fun onPlaybackStateChanged(state: Int){if(state==Player.STATE_READY)ready.set(true)}
                override fun onPlayerError(failure: PlaybackException){error.set(failure)}
            })
            instance.setMediaItems(parts.map{MediaItem.fromUri(Uri.fromFile(File(it.path)))})
            instance.prepare()
        }
        try {
            var deadline=System.currentTimeMillis()+15000
            while(!ready.get() && error.get()==null && System.currentTimeMillis()<deadline)delay(100)
            assertNull(error.get());assertTrue(ready.get())
            instrumentation.runOnMainSync{player.get().seekTo(0,parts.first().durationMs-100);player.get().play()}
            var crossed=false
            deadline=System.currentTimeMillis()+10000
            while(!crossed && error.get()==null && System.currentTimeMillis()<deadline){delay(100);instrumentation.runOnMainSync{crossed=player.get().currentMediaItemIndex>=1}}
            assertNull(error.get());assertTrue(crossed)
        } finally {instrumentation.runOnMainSync{player.get().release()}}
    }
}
