package de.frank.karteikartenlernen.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.karteikartenlernen.model.ApiKeys
import de.frank.karteikartenlernen.model.DEFAULT_GEMINI_MODEL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** API-Schlüssel für Groq und Gemini, verschlüsselt auf dem Gerät gespeichert. */
class ApiKeyStore(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val masterKey = MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            appContext,
            "karteikarten_api_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _keys = MutableStateFlow(read())
    val keys: StateFlow<ApiKeys> = _keys.asStateFlow()

    val current: ApiKeys get() = _keys.value

    private fun read(): ApiKeys = runCatching {
        ApiKeys(
            groqApiKey = prefs.getString(K_GROQ, null).orEmpty(),
            geminiApiKey = prefs.getString(K_GEMINI, null).orEmpty(),
            geminiModel = prefs.getString(K_GEMINI_MODEL, null) ?: DEFAULT_GEMINI_MODEL,
        )
    }.getOrDefault(ApiKeys())

    fun save(value: ApiKeys) {
        _keys.value = value
        runCatching {
            prefs.edit()
                .putString(K_GROQ, value.groqApiKey.trim())
                .putString(K_GEMINI, value.geminiApiKey.trim())
                .putString(K_GEMINI_MODEL, value.geminiModel.trim())
                .apply()
        }
    }

    private companion object {
        const val K_GROQ = "groq_api_key"
        const val K_GEMINI = "gemini_api_key"
        const val K_GEMINI_MODEL = "gemini_model"
    }
}
