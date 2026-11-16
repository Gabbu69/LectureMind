package com.lecturemind.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lecturemind.app.ai.AiProvider
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Explicit development-only provisioning; no credential is embedded in either APK. */
@RunWith(AndroidJUnit4::class)
class CredentialSetupTest {
    @Test fun saveProvidedKeyAndVerifyConnection(): Unit = runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val app=instrumentation.targetContext.applicationContext as LectureMindApp
        val provider=instrumentation.arguments.getString("provider") ?: "Gemini"
        check(provider in listOf("Gemini","OpenAI"))
        val input=File(app.filesDir,"provider-key.tmp")
        check(input.isFile) { "Private key setup input is missing." }
        try {
            val key=input.readText().trim()
            app.settings.saveKey(provider,key)
            check(app.settings.key(provider)==key) { "Encrypted key round-trip failed." }
            input.delete()
            app.settings.provider=provider
            AiProvider.create(provider,app.settings.key(provider),app.settings.model(provider)).testConnection()
        } finally {
            input.delete()
        }
    }
}
