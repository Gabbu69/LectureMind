package com.lecturemind.app

import android.Manifest
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.lecturemind.app.data.*
import com.lecturemind.app.recording.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DeviceTests {
    @get:Rule val microphone: GrantPermissionRule=GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val app get()=instrumentation.targetContext.applicationContext as LectureMindApp
    @Test fun encryptedCredentialsRoundTripAndRemoval() {
        val settings=app.settings;val test="lm-instrumentation-dummy-not-a-real-api-key"
        // Use a separate name so an actual provider key is never touched.
        settings.saveKey("TestOnly",test);assertEquals(test,settings.key("TestOnly"))
        val xml=File(app.applicationInfo.dataDir,"shared_prefs/settings.xml").readText()
        assertFalse(xml.contains(test));settings.removeKey("TestOnly");assertFalse(settings.hasKey("TestOnly"))
    }
    @Test fun databasePersistsAndCascadesOnlyOwnedLecture() = runBlocking {
        val dao=app.database.dao();val lecture=Lecture(title="Instrumentation fixture")
        dao.insert(lecture);dao.putAudio(AudioPart("${lecture.id}-0",lecture.id,0,"unused",0,1000,true,"[]","Gemini"))
        dao.bookmark(Bookmark(lectureId=lecture.id,positionMs=500));dao.notes(lecture.id,"Edited notes")
        assertEquals("Edited notes",dao.lecture(lecture.id)!!.notes);assertEquals(1,dao.audio(lecture.id).size)
        dao.delete(lecture.id);assertNull(dao.lecture(lecture.id));assertTrue(dao.audio(lecture.id).isEmpty())
    }
    @Test fun recordPauseBookmarkAndPlaybackFiles() = runBlocking {
        val dao=app.database.dao();while(!app.ready.value)delay(100)
        val fixture=Lecture(title="Device recording verification",subject="Verification",status="recording")
        dao.insert(fixture)
        app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));delay(2000)
        instrumentation.runOnMainSync { RecordingService.command(app,RecordingService.START,fixture.id) }
        try {
            waitUntil(15000){RecordingService.state.value.durationMs>=3000}
            instrumentation.runOnMainSync{RecordingService.command(app,RecordingService.MARK);RecordingService.command(app,RecordingService.PAUSE)}
            waitUntil(3000){RecordingService.state.value.paused}
            val paused=RecordingService.state.value.durationMs;delay(1500);assertTrue(RecordingService.state.value.durationMs-paused<400)
            instrumentation.runOnMainSync{RecordingService.command(app,RecordingService.PAUSE)}
            waitUntil(5000){RecordingService.state.value.durationMs>=paused+1500}
            UiDevice.getInstance(instrumentation).pressHome();delay(2000)
            assertTrue(RecordingService.state.value.durationMs>=paused+3000)
        } finally {
            instrumentation.runOnMainSync{RecordingService.command(app,RecordingService.STOP)}
            waitUntil(10000){RecordingService.state.value.lectureId==null}
        }
        val parts=dao.audio(fixture.id);assertTrue(parts.isNotEmpty());assertTrue(parts.all{it.finalized && File(it.path).length()>44})
        assertEquals(parts.sumOf{it.durationMs},dao.lecture(fixture.id)!!.durationMs)
        // Fixture microphone audio stays local and is deleted after assertions. Never uploaded.
        File(app.filesDir,"audio/${fixture.id}").deleteRecursively();dao.delete(fixture.id)
    }
    private suspend fun waitUntil(timeout: Long,predicate:()->Boolean){val end=System.currentTimeMillis()+timeout;while(!predicate()){check(System.currentTimeMillis()<end){"Device condition timed out"};delay(100)}}
}
