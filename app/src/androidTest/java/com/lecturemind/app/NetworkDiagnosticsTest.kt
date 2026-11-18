package com.lecturemind.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.InetAddress
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Read-only model metadata checks. Reports never include headers or credentials. */
@RunWith(AndroidJUnit4::class)
class NetworkDiagnosticsTest {
    @Test fun inspectHttpsReachability(): Unit = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as LectureMindApp
        val model=app.settings.model("Gemini")
        val results=JSONArray()
        val key=app.settings.key("Gemini")
        for(revision in listOf(false,true)) {
            val result=JSONObject().put("revisionHeader",revision)
            val start=System.currentTimeMillis()
            var connection: HttpsURLConnection?=null
            try {
                connection=URL("https://generativelanguage.googleapis.com/v1beta/models/$model").openConnection() as HttpsURLConnection
                connection.connectTimeout=10000
                connection.readTimeout=10000
                connection.instanceFollowRedirects=false
                connection.setRequestProperty("x-goog-api-key",key)
                if(revision) connection.setRequestProperty("Api-Revision","2026-05-20")
                result.put("httpStatus",connection.responseCode)
                val stream=if(connection.responseCode<400) connection.inputStream else connection.errorStream
                stream?.use { it.read() }
            } catch(e: Exception) {
                result.put("exception",e.javaClass.name)
                result.put("cause",e.cause?.javaClass?.name ?: "")
                // No exception message/body is logged: either may contain provider data.
            } finally { connection?.disconnect() }
            result.put("elapsedMs",System.currentTimeMillis()-start)
            results.put(result)
            instrumentation.sendStatus(2,android.os.Bundle().apply { putString("stream",result.toString()+"\n") })
        }
        val addresses=JSONArray()
        runCatching { InetAddress.getAllByName("generativelanguage.googleapis.com").forEach { addresses.put(it.javaClass.simpleName) } }
        val report=JSONObject().put("model",model).put("addresses",addresses).put("results",results)
        val directory=File(app.getExternalFilesDir(null),"verification").apply{mkdirs()}
        File(directory,"network-diagnostics.json").writeText(report.toString(2))
    }
}
