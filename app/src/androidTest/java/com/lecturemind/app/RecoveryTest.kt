package com.lecturemind.app

import android.Manifest
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.lecturemind.app.data.Lecture
import com.lecturemind.app.recording.RecordingService
import com.lecturemind.app.recording.WavFile
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class RecoverySeedTest {
    @get:Rule val permission: GrantPermissionRule=GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)
    @Test fun leaveRecordingForExternalProcessDeath() = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val app=instrumentation.targetContext.applicationContext as LectureMindApp
        while(!app.ready.value)delay(100)
        val lecture=Lecture(title="Device recovery verification",subject="Recovery verification",status="recording")
        app.database.dao().insert(lecture)
        check(app.getSharedPreferences("verification",0).edit().putString("recoveryId",lecture.id).commit())
        app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));delay(2000)
        instrumentation.runOnMainSync{RecordingService.command(app,RecordingService.START,lecture.id)}
        val deadline=System.currentTimeMillis()+15000
        while(RecordingService.state.value.durationMs<4000){check(System.currentTimeMillis()<deadline){"Recording did not start"};delay(100)}
        // The host now force-stops this package; do not call the graceful Stop action here.
    }
}
@RunWith(AndroidJUnit4::class)
class RecoveryCheckTest {
    @Test fun startupRepairsTheInterruptedWavAndRetainsAudio(): Unit = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val app=instrumentation.targetContext.applicationContext as LectureMindApp
        while(!app.ready.value)delay(100)
        val prefs=app.getSharedPreferences("verification",0);val id=prefs.getString("recoveryId",null) ?: error("No recovery fixture was created")
        val dao=app.database.dao();val lecture=dao.lecture(id) ?: error("Recovery fixture disappeared")
        assertEquals("saved",lecture.status);assertTrue(lecture.message.contains("interrupted"));assertTrue(lecture.durationMs>=3000)
        val parts=dao.audio(id);assertTrue(parts.isNotEmpty());assertTrue(parts.all{it.finalized})
        for(part in parts){val file=File(part.path);val header=ByteArray(44);RandomAccessFile(file,"r").use{it.readFully(header)};assertEquals(file.length()-44,ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(40).toLong())}
        assertNull(RecordingService.state.value.lectureId)
        val root=File(app.filesDir,"audio").canonicalFile;val target=File(root,id).canonicalFile;check(target.parentFile==root)
        target.deleteRecursively();dao.delete(id);prefs.edit().remove("recoveryId").commit()
    }
}
