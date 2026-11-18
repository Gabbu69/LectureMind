package com.lecturemind.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.lecturemind.app.ai.LectureLanguages
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

// https://developer.android.com/privacy-and-security/keystore
class SecureSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    var provider: String
        get() = prefs.getString("provider", "Gemini") ?: "Gemini"
        set(value) { require(value in listOf("Gemini", "OpenAI")); prefs.edit().putString("provider", value).apply() }
    var language: String
        get() = LectureLanguages.canonicalOutput(prefs.getString("language", "English") ?: "English") ?: "English"
        set(value) { val selected=LectureLanguages.canonicalOutput(value) ?: error("Choose a supported study language.");prefs.edit().putString("language", selected).apply() }
    var appearance: String
        get() = prefs.getString("appearance", "System") ?: "System"
        set(value) { prefs.edit().putString("appearance", value).apply() }
    fun model(provider: String) = prefs.getString("model_$provider", if (provider == "Gemini") "gemini-3.8-flash" else "gpt-5.4-mini")!!
    fun setModel(provider: String, model: String) { require(model.matches(Regex("[A-Za-z0-9._-]{1,100}"))); prefs.edit().putString("model_$provider", model).apply() }
    fun hasKey(provider: String) = prefs.contains("key_$provider")
    private fun encryptionKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("lecturemind.credentials", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("lecturemind.credentials", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
        }.generateKey()
    }
    fun saveKey(provider: String, raw: String) {
        val value = raw.trim()
        require(value.length in 16..512 && value.none { it.isWhitespace() }) { "Enter a valid API key without spaces." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, encryptionKey()) }
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("key_$provider", Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).commit())
    }
    fun key(provider: String): String {
        val value = prefs.getString("key_$provider", null) ?: error("Add your $provider API key in Settings first.")
        return try {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) { error("Your saved key could not be unlocked. Please enter it again in Settings.") }
    }
    fun removeKey(provider: String) { prefs.edit().remove("key_$provider").commit() }
}
