package de.frank.genialeideen.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.Closeable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONException
import org.json.JSONObject

/**
 * Alle Einstellungen und Schlüssel — ausschliesslich in [EncryptedSharedPreferences],
 * nie im Klartext (Baustein G).
 */
class SecureSettings(context: Context) : Closeable {
    private val appContext = context.applicationContext

    private val preferences: SharedPreferences? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                STORE_NAME,
                masterKeyAlias,
                appContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            null
        }
    }

    private val _themeFlow = MutableStateFlow(readString(Keys.THEME, Defaults.THEME))
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _appLockEnabledFlow = MutableStateFlow(
        preferences?.getBoolean(Keys.APP_LOCK_ENABLED, Defaults.APP_LOCK_ENABLED)
            ?: Defaults.APP_LOCK_ENABLED,
    )
    val appLockEnabledFlow: StateFlow<Boolean> = _appLockEnabledFlow.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        when (key) {
            Keys.THEME -> _themeFlow.value = prefs.getString(Keys.THEME, Defaults.THEME) ?: Defaults.THEME
            Keys.APP_LOCK_ENABLED ->
                _appLockEnabledFlow.value = prefs.getBoolean(Keys.APP_LOCK_ENABLED, Defaults.APP_LOCK_ENABLED)
        }
    }

    init {
        preferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    // ---- Vorlesen (Baustein D und E) ----

    var ttsProvider: String
        get() = readString(Keys.TTS_PROVIDER, Defaults.TTS_PROVIDER)
        set(value) = writeString(Keys.TTS_PROVIDER, value)

    var edgeTtsVoice: String
        get() = readString(Keys.EDGE_TTS_VOICE, Defaults.EDGE_TTS_VOICE)
        set(value) = writeString(Keys.EDGE_TTS_VOICE, value)

    var googleTtsApiKey: String
        get() = readString(Keys.GOOGLE_TTS_API_KEY, "")
        set(value) = writeString(Keys.GOOGLE_TTS_API_KEY, value)

    var googleTtsVoice: String
        get() = readString(Keys.GOOGLE_TTS_VOICE, Defaults.GOOGLE_TTS_VOICE)
        set(value) = writeString(Keys.GOOGLE_TTS_VOICE, value)

    var qwenTtsApiKey: String
        get() = readString(Keys.QWEN_TTS_API_KEY, "")
        set(value) = writeString(Keys.QWEN_TTS_API_KEY, value)

    /** Die gewählte Alibaba-Standardstimme (nicht geklont). */
    var qwenStandardVoice: String
        get() = readString(Keys.QWEN_STANDARD_VOICE, Defaults.QWEN_STANDARD_VOICE)
        set(value) = writeString(Keys.QWEN_STANDARD_VOICE, value)

    var qwenTtsVoiceId: String
        get() = readString(Keys.QWEN_TTS_VOICE_ID, "")
        set(value) = writeString(Keys.QWEN_TTS_VOICE_ID, value)

    /** Namen der geklonten Stimmen, nach Stimm-Kennung. Alibaba kann sie selbst nicht umbenennen. */
    var qwenVoiceNames: Map<String, String>
        get() {
            val stored = readString(Keys.QWEN_VOICE_NAMES, "")
            if (stored.isBlank()) return emptyMap()
            return try {
                val json = JSONObject(stored)
                json.keys().asSequence().associateWith { json.optString(it) }.filterValues(String::isNotBlank)
            } catch (_: JSONException) {
                emptyMap()
            }
        }
        set(value) = writeString(Keys.QWEN_VOICE_NAMES, JSONObject(value.toMap<String, Any>()).toString())

    /** Die gezogene Reihenfolge der eigenen Stimmen. */
    var qwenVoiceOrder: List<String>
        get() = readString(Keys.QWEN_VOICE_ORDER, "").split('\n').filter(String::isNotBlank)
        set(value) = writeString(Keys.QWEN_VOICE_ORDER, value.filter(String::isNotBlank).joinToString("\n"))

    var ttsSpeechRate: Float
        get() = preferences?.getFloat(Keys.TTS_SPEECH_RATE, Defaults.TTS_SPEECH_RATE)
            ?.coerceIn(MIN_TTS_SPEECH_RATE, MAX_TTS_SPEECH_RATE) ?: Defaults.TTS_SPEECH_RATE
        set(value) {
            preferences?.edit()
                ?.putFloat(Keys.TTS_SPEECH_RATE, value.coerceIn(MIN_TTS_SPEECH_RATE, MAX_TTS_SPEECH_RATE))
                ?.apply()
        }

    /**
     * Erzwingt die deutsche Aussprache, auch wenn der Text englisch aussehende Wörter enthält.
     * Mehrsprachige Stimmen (Edge-„Multilingual", Google Chirp 3) schalten sonst mitten im Satz
     * auf Englisch um, was gerade bei Ideentiteln stört.
     */
    var immerDeutschVorlesen: Boolean
        get() = readFlag(Keys.IMMER_DEUTSCH, true)
        set(value) = writeFlag(Keys.IMMER_DEUTSCH, value)

    var favoriteTtsVoices: Set<String>
        get() = preferences?.getStringSet(Keys.FAVORITE_TTS_VOICES, emptySet())?.toSet().orEmpty()
        set(value) {
            preferences?.edit()?.putStringSet(Keys.FAVORITE_TTS_VOICES, value.toSet())?.apply()
        }

    // ---- Spracheingabe (Baustein F) ----

    var groqApiKey: String
        get() = readString(Keys.GROQ_API_KEY, "")
        set(value) = writeString(Keys.GROQ_API_KEY, value)

    var filterStilleVorabAn: Boolean
        get() = readFlag(Keys.FILTER_VAD, true)
        set(value) = writeFlag(Keys.FILTER_VAD, value)

    var filterSegmentmetrikenAn: Boolean
        get() = readFlag(Keys.FILTER_SEGMENTS, true)
        set(value) = writeFlag(Keys.FILTER_SEGMENTS, value)

    var filterZeitstempelAn: Boolean
        get() = readFlag(Keys.FILTER_TIMESTAMPS, true)
        set(value) = writeFlag(Keys.FILTER_TIMESTAMPS, value)

    var filterFloskelnAn: Boolean
        get() = readFlag(Keys.FILTER_PHRASES, true)
        set(value) = writeFlag(Keys.FILTER_PHRASES, value)

    // ---- KI (Baustein O) ----

    /** `abo` (Anmeldung über das ChatGPT-Abo) oder `schluessel` (eigener API-Schlüssel). */
    var kiZugang: String
        get() = readString(Keys.KI_ZUGANG, Defaults.KI_ZUGANG)
        set(value) = writeString(Keys.KI_ZUGANG, value)

    var geminiApiKey: String
        get() = readString(Keys.GEMINI_API_KEY, "")
        set(value) = writeString(Keys.GEMINI_API_KEY, value)

    var model: String
        get() = readString(Keys.MODEL, Defaults.MODEL)
        set(value) = writeString(Keys.MODEL, value)

    var reasoning: String
        get() = readString(Keys.REASONING, Defaults.REASONING)
        set(value) = writeString(Keys.REASONING, value)

    var chatGptConnectedAt: Long
        get() = preferences?.getLong(Keys.CHAT_GPT_CONNECTED_AT, 0L) ?: 0L
        set(value) {
            preferences?.edit()?.putLong(Keys.CHAT_GPT_CONNECTED_AT, value.coerceAtLeast(0L))?.apply()
        }

    // ---- Darstellung und Sicherheit ----

    var theme: String
        get() = readString(Keys.THEME, Defaults.THEME)
        set(value) {
            val normalized = value.takeIf(ALLOWED_THEMES::contains) ?: Defaults.THEME
            _themeFlow.value = normalized
            writeString(Keys.THEME, normalized)
        }

    var schriftgroesse: Float
        get() = preferences?.getFloat(Keys.SCHRIFTGROESSE, 1f)?.coerceIn(0.85f, 1.4f) ?: 1f
        set(value) {
            preferences?.edit()?.putFloat(Keys.SCHRIFTGROESSE, value.coerceIn(0.85f, 1.4f))?.apply()
        }

    var appLockEnabled: Boolean
        get() = readFlag(Keys.APP_LOCK_ENABLED, Defaults.APP_LOCK_ENABLED)
        set(value) {
            _appLockEnabledFlow.value = value
            writeFlag(Keys.APP_LOCK_ENABLED, value)
        }

    /** Wartezeit im Hintergrund bis zur Sperre: 0 = sofort, -1 = nie. */
    var appLockDelayMinutes: Int
        get() = preferences?.getInt(Keys.APP_LOCK_DELAY, Defaults.APP_LOCK_DELAY) ?: Defaults.APP_LOCK_DELAY
        set(value) {
            preferences?.edit()?.putInt(Keys.APP_LOCK_DELAY, value)?.apply()
        }

    // ---- Sicherung (Baustein J) ----

    var autoBackupEnabled: Boolean
        get() = readFlag(Keys.AUTO_BACKUP, false)
        set(value) = writeFlag(Keys.AUTO_BACKUP, value)

    var letzteSicherungAm: Long
        get() = preferences?.getLong(Keys.LETZTE_SICHERUNG, 0L) ?: 0L
        set(value) {
            preferences?.edit()?.putLong(Keys.LETZTE_SICHERUNG, value)?.apply()
        }

    override fun close() {
        preferences?.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun readString(key: String, default: String): String =
        preferences?.getString(key, default) ?: default

    private fun writeString(key: String, value: String) {
        preferences?.edit()?.putString(key, value)?.apply()
    }

    private fun readFlag(key: String, default: Boolean): Boolean =
        preferences?.getBoolean(key, default) ?: default

    private fun writeFlag(key: String, value: Boolean) {
        preferences?.edit()?.putBoolean(key, value)?.apply()
    }

    object Keys {
        const val TTS_PROVIDER = "tts_provider"
        const val EDGE_TTS_VOICE = "edge_tts_voice"
        const val GOOGLE_TTS_API_KEY = "google_tts_api_key"
        const val GOOGLE_TTS_VOICE = "google_tts_voice"
        const val QWEN_TTS_API_KEY = "qwen_tts_api_key"
        const val QWEN_TTS_VOICE_ID = "qwen_tts_voice_id"
        const val QWEN_STANDARD_VOICE = "qwen_standard_voice"
        const val QWEN_VOICE_NAMES = "qwen_voice_names"
        const val QWEN_VOICE_ORDER = "qwen_voice_order"
        const val TTS_SPEECH_RATE = "tts_speech_rate"
        const val IMMER_DEUTSCH = "immer_deutsch_vorlesen"
        const val FAVORITE_TTS_VOICES = "favorite_tts_voices"
        const val GROQ_API_KEY = "groq_api_key"
        const val FILTER_VAD = "filter_vad"
        const val FILTER_SEGMENTS = "filter_segments"
        const val FILTER_TIMESTAMPS = "filter_timestamps"
        const val FILTER_PHRASES = "filter_phrases"
        const val KI_ZUGANG = "ki_zugang"
        const val GEMINI_API_KEY = "gemini_api_key"
        const val MODEL = "model"
        const val REASONING = "reasoning"
        const val CHAT_GPT_CONNECTED_AT = "chat_gpt_connected_at"
        const val THEME = "theme"
        const val SCHRIFTGROESSE = "schriftgroesse"
        const val APP_LOCK_ENABLED = "app_lock_enabled"
        const val APP_LOCK_DELAY = "app_lock_delay_minutes"
        const val AUTO_BACKUP = "auto_backup"
        const val LETZTE_SICHERUNG = "letzte_sicherung"
    }

    object Defaults {
        const val TTS_PROVIDER = "edge_tts"
        const val EDGE_TTS_VOICE = "de-DE-SeraphinaMultilingualNeural"
        const val GOOGLE_TTS_VOICE = "de-DE-Chirp3-HD-Kore"
        const val QWEN_STANDARD_VOICE = "Cherry"
        const val TTS_SPEECH_RATE = 1f
        const val KI_ZUGANG = "abo"
        const val MODEL = "gpt-5.6-terra"
        const val REASONING = "medium"
        const val THEME = "light"
        const val APP_LOCK_ENABLED = false
        const val APP_LOCK_DELAY = 1
    }

    companion object {
        const val STORE_NAME = "geniale_ideen_secure_prefs"
        /** Genau zwei Modi — die App folgt der Systemvorgabe bewusst nicht (Baustein A). */
        val ALLOWED_THEMES = setOf("light", "dark")
        const val MIN_TTS_SPEECH_RATE = 0.5f
        const val MAX_TTS_SPEECH_RATE = 2.0f
    }
}
