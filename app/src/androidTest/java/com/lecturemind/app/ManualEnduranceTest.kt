package com.lecturemind.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Inspect a recording started and stopped in the real app, without a runner attached during capture. */
@RunWith(AndroidJUnit4::class)
class ManualEnduranceCheckTest {
    @Test fun verifyNormalAppHourAndRemoveOnlyItsAudio(): Unit = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as LectureMindApp
        while(!app.ready.value)delay(100)
        val dao=app.database.dao()
        val lecture=dao.all().single{it.title=="Lecture mind endurance check 2026-11-16" && it.subject=="Verification"}
        assertEquals("saved",lecture.status)
        assertTrue(lecture.message.isBlank())
        val parts=dao.audio(lecture.id)
        assertTrue(parts.size>=12)
        assertTrue(parts.all{it.finalized})
        assertEquals(parts.sumOf{it.durationMs},lecture.durationMs)
        assertTrue(lecture.durationMs>=3_600_000)
        assertTrue(parts.sumOf{File(it.path).length()} in 115_000_000..120_000_000)
        val probe=EnduranceProbe(instrumentation,app)
        probe.verifyAndReport(parts,InstrumentationRegistry.getArguments().getString("screenLocked")=="true")
        val directory=File(app.getExternalFilesDir(null),"verification")
        val reportFile=File(directory,"endurance-result.json")
        val report=JSONObject(reportFile.readText()).put("captureMode","Normal app recording UI; instrumentation attached only after Save")
            .put("screenLockEvidence","Host power-state samples; not a claim that the screen was off for every second")
        reportFile.writeText(report.toString(2))
        val audioRoot=File(app.filesDir,"audio").canonicalFile
        val target=File(audioRoot,lecture.id).canonicalFile
        check(target.parentFile==audioRoot)
        assertTrue(target.deleteRecursively())
        assertFalse(target.exists())
        dao.delete(lecture.id)
        probe.markAudioDeleted()
    }
}

/** The earlier runner lost its host connection; preserve only metadata before removing its local audio. */
@RunWith(AndroidJUnit4::class)
class InterruptedEnduranceCleanupTest {
    @Test fun reportAndRemoveOnlyTheInterruptedTest(): Unit = runBlocking {
        val app=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as LectureMindApp
        while(!app.ready.value)delay(100)
        val dao=app.database.dao()
        val rows=JSONArray()
        dao.all().filter{it.title=="60-minute local recorder endurance test" && it.subject=="Verification"}.forEach { lecture ->
            val parts=dao.audio(lecture.id)
            rows.put(JSONObject().put("status",lecture.status).put("message",lecture.message).put("durationMs",lecture.durationMs)
                .put("parts",JSONArray().apply{parts.forEach{part->put(JSONObject().put("startMs",part.startMs).put("durationMs",part.durationMs).put("finalized",part.finalized).put("bytes",File(part.path).length()))}}))
            val root=File(app.filesDir,"audio").canonicalFile
            val target=File(root,lecture.id).canonicalFile
            check(target.parentFile==root)
            assertTrue(target.deleteRecursively())
            dao.delete(lecture.id)
        }
        val directory=File(app.getExternalFilesDir(null),"verification").apply{mkdirs()}
        File(directory,"endurance-interrupted-cleanup.json").writeText(JSONObject().put("recordings",rows).put("audioDeleted",true).put("audioUploaded",false).toString(2))
    }
}
