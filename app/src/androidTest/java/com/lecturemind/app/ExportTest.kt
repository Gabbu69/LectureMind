package com.lecturemind.app

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.lecturemind.app.ui.Exports
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExportTest {
    @Test fun deletingLectureRemovesOnlyItsLocalExportCopies() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val owner=java.util.UUID.randomUUID().toString();val otherOwner=java.util.UUID.randomUUID().toString()
        val directory=File(context.cacheDir,"exports").apply{mkdirs()}
        val owned=File(directory,"$owner-notes.txt");val other=File(directory,"$otherOwner-notes.txt")
        try { owned.writeText("Owned fixture");other.writeText("Other fixture");Exports.clearLectureCache(context,owner);assertFalse(owned.exists());assertTrue(other.exists()) }
        finally { owned.delete();other.delete() }
    }
    @Test fun rendersMultiPagePdfAndExportsUtf8Text() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val title="Verification export ${System.currentTimeMillis()}"
        val body=("## Photosynthesis\nPlants use light to make food. Ang chlorophyll ay tumutulong sumipsip ng light.\nSources: p0s0\n\n").repeat(35)
        val device=UiDevice.getInstance(instrumentation)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity{Exports.share(it,title,body,true)}
            device.waitForIdle();device.pressBack()
            val directory=File(context.cacheDir,"exports")
            val pdf=directory.listFiles()!!.first{it.name.startsWith(title) && it.extension=="pdf"}
            assertTrue(pdf.length()>1000)
            PdfRenderer(ParcelFileDescriptor.open(pdf,ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                assertTrue(renderer.pageCount>1)
                val captures=File(context.getExternalFilesDir(null),"verification").apply{mkdirs()}
                listOf(0,renderer.pageCount-1).forEach { index -> renderer.openPage(index).use { page ->
                    val bitmap=Bitmap.createBitmap(595,842,Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE);page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    File(captures,"export-page-$index.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
                } }
            }
            scenario.onActivity{Exports.share(it,title,body,false)}
            device.waitForIdle();device.pressBack()
            val text=directory.listFiles()!!.first{it.name.startsWith(title) && it.extension=="txt"}
            assertEquals("$title\n\n$body",text.readText(Charsets.UTF_8))
            directory.listFiles()!!.filter{it.name.startsWith(title)}.forEach{it.delete()}
        }
    }
}
