package com.lecturemind.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.*
import com.lecturemind.app.ai.*
import com.lecturemind.app.data.*
import com.lecturemind.app.recording.WavFile
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UiAcceptanceTest {
    @Test fun studyFlowShowsSourcesScoresAndShares() = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation();val app=instrumentation.targetContext.applicationContext as LectureMindApp
        val device=UiDevice.getInstance(instrumentation);while(!app.ready.value)delay(100)
        val lecture=Lecture(title="Sample · How plants make food",subject="Biology · test fixture",durationMs=3000,status="ready",
            notes="## Photosynthesis\nPlants use light to make food. Ang chlorophyll ay tumutulong sumipsip ng light.\nSources: p0s0",
            reviewer="## Key idea\nPhotosynthesis uses light to make food.\nSources: p0s0\n\n## Self-review\nWhat does chlorophyll help absorb?\nSources: p0s0",
            quiz=ContentCodec.quizJson(listOf(Question("What do plants use to make food?",listOf("Light","Sound"),0,"The sample lecture says plants use light.",listOf("p0s0")))),generatedProvider="Local fixture",generatedLanguage="English")
        val dao=app.database.dao();dao.insert(lecture)
        repeat(3) { index ->
            val file=File(app.filesDir,"audio/${lecture.id}/$index.wav");WavFile.create(file).use{it.write(ByteArray(32000))};WavFile.repair(file)
            dao.putAudio(AudioPart("${lecture.id}-$index",lecture.id,index,file.absolutePath,index*1000L,1000,true,ContentCodec.speechJson(listOf(Speech("p${index}s0",index*1000L,(index+1)*1000L,"Plants use light to make food. Ang chlorophyll ay tumutulong sumipsip ng light."))),"Local fixture"))
        }
        try {
            app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));delay(2500)
            assertTrue(device.wait(Until.hasObject(By.text(lecture.title)),10000))
            device.findObject(By.text(lecture.title)).click();assertTrue(device.wait(Until.hasObject(By.text("Your notes")),10000))
            device.findObject(By.desc("Play audio")).click()
            assertTrue(device.wait(Until.hasObject(By.text("00:03 / 00:03")),10000))
            val captures=File(app.getExternalFilesDir(null),"verification").apply{mkdirs()}
            device.takeScreenshot(File(captures,"workspace-notes.png"))
            device.findObject(By.desc("Edit notes")).click();assertTrue(device.wait(Until.hasObject(By.text("Save notes")),5000))
            device.findObject(By.clazz("android.widget.EditText")).text="## Photosynthesis\nPlants use light to make food. Checked locally.\nSources: p0s0"
            device.findObject(By.text("Save notes")).click()
            device.findObject(By.text("Quiz")).click();assertTrue(device.wait(Until.hasObject(By.text("Start practice quiz")),5000));device.findObject(By.text("Start practice quiz")).click()
            val scroll=device.findObject(By.scrollable(true));scroll?.scroll(Direction.DOWN,0.8f)
            assertTrue(device.wait(Until.hasObject(By.text("Light")),5000));device.findObject(By.text("Light")).click()
            scroll?.scroll(Direction.DOWN,0.8f);assertTrue(device.wait(Until.hasObject(By.text("Submit answers")),5000));device.findObject(By.text("Submit answers")).click()
            scroll?.scroll(Direction.UP,0.9f)
            assertTrue(device.wait(Until.hasObject(By.textContains("1 / 1 correct")),5000))
            device.takeScreenshot(File(captures,"quiz-results.png"))
            assertTrue(dao.lecture(lecture.id)!!.notes.contains("Checked locally."))
        } finally {
            File(app.filesDir,"audio/${lecture.id}").deleteRecursively();dao.delete(lecture.id)
            app.startActivity(Intent(app,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }
}
