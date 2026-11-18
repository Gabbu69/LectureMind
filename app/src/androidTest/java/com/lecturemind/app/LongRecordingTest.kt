package com.lecturemind.app

import android.Manifest
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.lecturemind.app.data.Lecture
import com.lecturemind.app.recording.RecordingService
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** Opt in with -e longRecording true; collects no network traffic or lecture content. */
@RunWith(AndroidJUnit4::class)
class LongRecordingTest {
    @get:Rule val permission: GrantPermissionRule=GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    @Test fun sixtyMinutesWithScreenOff() = runBlocking {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("longRecording")=="true")
        val instrumentation=InstrumentationRegistry.getInstrumentation();val app=instrumentation.targetContext.applicationContext as LectureMindApp
        while(!app.ready.value)delay(100)
        val lecture=Lecture(title="60-minute local recorder endurance test",subject="Verification",status="recording");app.database.dao().insert(lecture)
        app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));delay(2000)
        instrumentation.runOnMainSync{RecordingService.command(app,RecordingService.START,lecture.id)}
        val probe=EnduranceProbe(instrumentation,app)
        val start=System.currentTimeMillis();var locked=false;var reportedMinute=-1L
        try {
            while(RecordingService.state.value.durationMs<3600000) {
                delay(1000)
                check(System.currentTimeMillis()-start<3650000){"Recorder did not capture a full hour in time"}
                if(System.currentTimeMillis()-start>15000 && !locked){UiDevice.getInstance(instrumentation).sleep();locked=true}
                assertEquals(lecture.id,RecordingService.state.value.lectureId)
                val minute=RecordingService.state.value.durationMs/60000
                if(minute!=reportedMinute){reportedMinute=minute;probe.tick(RecordingService.state.value.durationMs);instrumentation.sendStatus(2,android.os.Bundle().apply{putString("stream","Captured ${com.lecturemind.app.recording.formatTime(RecordingService.state.value.durationMs)} / 60:00; app PSS ${android.os.Debug.getPss()/1024} MB\n")})}
            }
        } finally {
            instrumentation.runOnMainSync{RecordingService.command(app,RecordingService.STOP)}
            repeat(100){if(RecordingService.state.value.lectureId==null)return@repeat;delay(100)}
        }
        val parts=app.database.dao().audio(lecture.id)
        assertTrue(parts.size>=12);assertTrue(parts.all{it.finalized});assertTrue(parts.sumOf{it.durationMs}>=3600000)
        assertTrue(parts.sumOf{File(it.path).length()} in 115_000_000..116_000_000)
        probe.verifyAndReport(parts,locked)
        val testAudio=File(app.filesDir,"audio/${lecture.id}")
        assertEquals(File(app.filesDir,"audio").canonicalFile,testAudio.canonicalFile.parentFile)
        assertTrue(testAudio.deleteRecursively())
        assertFalse(testAudio.exists())
        app.database.dao().delete(lecture.id)
        probe.markAudioDeleted()
        UiDevice.getInstance(instrumentation).wakeUp()
    }
}
