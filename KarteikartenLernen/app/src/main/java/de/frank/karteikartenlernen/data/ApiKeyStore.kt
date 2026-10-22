package de.frank.karteikartenlernen.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import de.frank.karteikartenlernen.model.ApiKeys
import de.frank.karteikartenlernen.model.DEFAULT_GEMINI_MODEL
import de.frank.karteikartenlernen.transcription.GroqTranscriber
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
            groqModel = prefs.getString(K_GROQ_MODEL, null) ?: de.frank.karteikartenlernen.BuildConfig.GROQ_TRANSCRIPTION_MODEL,
            vocabularyEnabled = prefs.getBoolean(K_VOCABULARY_ENABLED, true),
            vocabulary = prefs.getString(K_VOCABULARY, null) ?: GroqTranscriber.STANDARD_WOERTERBUCH,
            googleTtsKey = prefs.getString(K_GOOGLE_TTS, null).orEmpty(),
            geminiTtsKeyFrei = prefs.getString(K_GEMINI_TTS_FREI, null).orEmpty(),
            geminiTtsKeyBezahlt = prefs.getString(K_GEMINI_TTS_BEZAHLT, null).orEmpty(),
            geminiTtsSchluesselWahl = prefs.getString(K_GEMINI_TTS_WAHL, null) ?: de.frank.karteikartenlernen.audio.ZusatzStimmen.GEMINI_TTS_FREI_DANN_BEZAHLT,
            geminiTtsChirpRueckfall = prefs.getBoolean(K_GEMINI_TTS_CHIRP, false),
            azureTtsKey = prefs.getString(K_AZURE_TTS, null).orEmpty(),
            azureTtsRegion = prefs.getString(K_AZURE_REGION, null) ?: de.frank.karteikartenlernen.audio.ZusatzStimmen.DEFAULT_AZURE_REGION,
        )
    }.getOrDefault(ApiKeys())

    fun save(value: ApiKeys) {
        _keys.value = value
        runCatching {
            prefs.edit()
                .putString(K_GROQ, value.groqApiKey.trim())
                .putString(K_GEMINI, value.geminiApiKey.trim())
                .putString(K_GEMINI_MODEL, value.geminiModel.trim())
                .putString(K_GROQ_MODEL, value.groqModel)
                .putBoolean(K_VOCABULARY_ENABLED, value.vocabularyEnabled)
                .putString(K_VOCABULARY, value.vocabulary)
                .putString(K_GOOGLE_TTS, value.googleTtsKey.trim())
                .putString(K_GEMINI_TTS_FREI, value.geminiTtsKeyFrei.trim())
                .putString(K_GEMINI_TTS_BEZAHLT, value.geminiTtsKeyBezahlt.trim())
                .putString(K_GEMINI_TTS_WAHL, value.geminiTtsSchluesselWahl)
                .putBoolean(K_GEMINI_TTS_CHIRP, value.geminiTtsChirpRueckfall)
                .putString(K_AZURE_TTS, value.azureTtsKey.trim())
                .putString(K_AZURE_REGION, value.azureTtsRegion.trim())
                .apply()
        }
    }

    private companion object {
        const val K_GROQ = "groq_api_key"
        const val K_GEMINI = "gemini_api_key"
        const val K_GEMINI_MODEL = "gemini_model"
        const val K_GROQ_MODEL = "groq_model"
        const val K_VOCABULARY_ENABLED = "vocabulary_enabled"
        const val K_VOCABULARY = "vocabulary"
        const val K_GOOGLE_TTS = "google_tts_api_key"
        const val K_GEMINI_TTS_FREI = "gemini_tts_key_frei"
        const val K_GEMINI_TTS_BEZAHLT = "gemini_tts_key_bezahlt"
        const val K_GEMINI_TTS_WAHL = "gemini_tts_schluessel_wahl"
        const val K_GEMINI_TTS_CHIRP = "gemini_tts_chirp_rueckfall"
        const val K_AZURE_TTS = "azure_tts_key"
        const val K_AZURE_REGION = "azure_tts_region"
    }
}
