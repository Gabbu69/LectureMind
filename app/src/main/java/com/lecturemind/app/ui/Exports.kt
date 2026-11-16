package com.lecturemind.app.ui

import android.content.*
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

object Exports {
    fun share(context: Context,title: String,body: String,pdf: Boolean,lectureId: String? = null) {
        val directory=File(context.cacheDir,"exports").apply { mkdirs() }
        directory.listFiles()?.filter { it.lastModified()<System.currentTimeMillis()-24*60*60*1000 }?.forEach { it.delete() }
        val name=title.replace(Regex("[^\\p{L}\\p{N} ._-]"),"_").take(70).ifBlank { "LectureMind" }
        val prefix=lectureId?.let { UUID.fromString(it).toString()+"-" } ?: ""
        val file=File(directory,"$prefix$name-${System.currentTimeMillis()}.${if(pdf) "pdf" else "txt"}")
        if(pdf) writePdf(file,title,body) else file.writeText("$title\n\n$body",Charsets.UTF_8)
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
        val intent=Intent(Intent.ACTION_SEND).setType(if(pdf) "application/pdf" else "text/plain").putExtra(Intent.EXTRA_STREAM,uri).putExtra(Intent.EXTRA_SUBJECT,title)
            .apply { clipData=ClipData.newRawUri("LectureMind export",uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(intent,"Share ${if(pdf) "PDF" else "text"}"))
    }
    fun clearLectureCache(context: Context,lectureId: String) {
        val prefix=UUID.fromString(lectureId).toString()+"-"
        val root=File(context.cacheDir,"exports").canonicalFile
        root.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { file ->
            check(file.canonicalFile.parentFile==root && file.isFile)
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
            context.revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            check(file.delete()) { "A local export could not be removed. Try deleting the lecture again." }
        }
    }
    private fun writePdf(file: File,title: String,body: String) {
        val doc=PdfDocument()
        try {
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.rgb(25,43,39);textSize=12f;typeface=Typeface.create("sans-serif",Typeface.NORMAL) }
            var pageNumber=0;var page: PdfDocument.Page?=null;var y=0f
            fun nextPage() {
                page?.let { doc.finishPage(it) };pageNumber++
                page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNumber).create());y=52f
                val footer=Paint(paint).apply { textSize=9f;color=Color.DKGRAY }
                page!!.canvas.drawText("LectureMind · $pageNumber",42f,810f,footer)
            }
            nextPage()
            for(line in ("# $title\n\n$body").lines()) {
                val heading=line.startsWith("#")
                paint.textSize=if(heading) 16f else 12f;paint.typeface=Typeface.create("sans-serif",if(heading) Typeface.BOLD else Typeface.NORMAL)
                var remaining=line.trimStart('#',' ')
                if(remaining.isEmpty()) { y+=12;continue }
                while(remaining.isNotEmpty()) {
                    if(y>775)nextPage()
                    var count=paint.breakText(remaining,true,510f,null).coerceAtLeast(1)
                    if(count<remaining.length){val space=remaining.lastIndexOf(' ',count-1);if(space>count/2)count=space}
                    page!!.canvas.drawText(remaining.substring(0,count),42f,y,paint);remaining=remaining.substring(count).trimStart();y+=if(heading)24f else 19f
                }
                y+=5f
            }
            page?.let { doc.finishPage(it) };file.outputStream().use { doc.writeTo(it) }
        } finally { doc.close() }
    }
}
