package de.frank.perfectmoment.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.Closeable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
            Keys.THEME -> _themeFlow.value = prefs.getString(Keys.THEME, Defaults.THEME)
                ?: Defaults.THEME
            Keys.APP_LOCK_ENABLED -> _appLockEnabledFlow.value =
                prefs.getBoolean(Keys.APP_LOCK_ENABLED, Defaults.APP_LOCK_ENABLED)
        }
    }

    init {
        preferences?.registerOnSharedPreferenceChangeListener(listener)
    }

    var ttsProvider: String
        get() = readString(Keys.TTS_PROVIDER, Defaults.TTS_PROVIDER)
        set(value) = writeString(Keys.TTS_PROVIDER, value)

    var edgeTtsVoice: String
        get() = readString(Keys.EDGE_TTS_VOICE, Defaults.EDGE_TTS_VOICE)
        set(value) = writeString(Keys.EDGE_TTS_VOICE, value)

    var googleTtsApiKey: String
        get() = readString(Keys.GOOGLE_TTS_API_KEY, Defaults.GOOGLE_TTS_API_KEY)
        set(value) = writeString(Keys.GOOGLE_TTS_API_KEY, value)

    var googleTtsVoice: String
        get() = readString(Keys.GOOGLE_TTS_VOICE, Defaults.GOOGLE_TTS_VOICE)
        set(value) = writeString(Keys.GOOGLE_TTS_VOICE, value)

    var favoriteTtsVoices: Set<String>
        get() = preferences?.getStringSet(Keys.FAVORITE_TTS_VOICES, emptySet())?.toSet().orEmpty()
        set(value) {
            preferences?.edit()?.putStringSet(Keys.FAVORITE_TTS_VOICES, value.toSet())?.apply()
        }

    var groqApiKey: String
        get() = readString(Keys.GROQ_API_KEY, Defaults.GROQ_API_KEY)
        set(value) = writeString(Keys.GROQ_API_KEY, value)

    var pauseRepSeconds: Int
        get() = preferences?.getInt(Keys.PAUSE_REP_SECONDS, Defaults.PAUSE_REP_SECONDS)
            ?.coerceIn(1, 30) ?: Defaults.PAUSE_REP_SECONDS
        set(value) = writeInt(Keys.PAUSE_REP_SECONDS, value.coerceIn(1, 30))

    var pauseNextSeconds: Int
        get() = preferences?.getInt(Keys.PAUSE_NEXT_SECONDS, Defaults.PAUSE_NEXT_SECONDS)
            ?.coerceIn(1, 60) ?: Defaults.PAUSE_NEXT_SECONDS
        set(value) = writeInt(Keys.PAUSE_NEXT_SECONDS, value.coerceIn(1, 60))

    var repsPerQuestion: Int
        get() = preferences?.getInt(Keys.REPS_PER_QUESTION, Defaults.REPS_PER_QUESTION)
            ?.coerceIn(1, 10) ?: Defaults.REPS_PER_QUESTION
        set(value) = writeInt(Keys.REPS_PER_QUESTION, value.coerceIn(1, 10))

    var sessionDurationMin: Int
        get() = preferences?.getInt(Keys.SESSION_DURATION_MIN, Defaults.SESSION_DURATION_MIN)
            ?.takeIf(ALLOWED_DURATIONS::contains) ?: Defaults.SESSION_DURATION_MIN
        set(value) = writeInt(
            Keys.SESSION_DURATION_MIN,
            value.takeIf(ALLOWED_DURATIONS::contains) ?: Defaults.SESSION_DURATION_MIN,
        )

    var theme: String
        get() = readString(Keys.THEME, Defaults.THEME)
        set(value) {
            val normalized = value.takeIf(ALLOWED_THEMES::contains) ?: Defaults.THEME
            _themeFlow.value = normalized
            writeString(Keys.THEME, normalized)
        }

    var activeSkillId: Long
        get() = preferences?.getLong(Keys.ACTIVE_SKILL_ID, Defaults.ACTIVE_SKILL_ID)
            ?: Defaults.ACTIVE_SKILL_ID
        set(value) {
            preferences?.edit()?.putLong(Keys.ACTIVE_SKILL_ID, value)?.apply()
        }

    var operatingModeText: String
        get() = readString(Keys.OPERATING_MODE_TEXT, Defaults.OPERATING_MODE_TEXT)
        set(value) = writeString(Keys.OPERATING_MODE_TEXT, value)

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

    var appLockEnabled: Boolean
        get() = preferences?.getBoolean(Keys.APP_LOCK_ENABLED, Defaults.APP_LOCK_ENABLED)
            ?: Defaults.APP_LOCK_ENABLED
        set(value) {
            _appLockEnabledFlow.value = value
            preferences?.edit()?.putBoolean(Keys.APP_LOCK_ENABLED, value)?.apply()
        }

    override fun close() {
        preferences?.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun readString(key: String, default: String): String =
        preferences?.getString(key, default) ?: default

    private fun writeString(key: String, value: String) {
        preferences?.edit()?.putString(key, value)?.apply()
    }

    private fun writeInt(key: String, value: Int) {
        preferences?.edit()?.putInt(key, value)?.apply()
    }

    object Keys {
        const val TTS_PROVIDER = "tts_provider"
        const val EDGE_TTS_VOICE = "edge_tts_voice"
        const val GOOGLE_TTS_API_KEY = "google_tts_api_key"
        const val GOOGLE_TTS_VOICE = "google_tts_voice"
        const val FAVORITE_TTS_VOICES = "favorite_tts_voices"
        const val GROQ_API_KEY = "groq_api_key"
        const val PAUSE_REP_SECONDS = "pause_rep_seconds"
        const val PAUSE_NEXT_SECONDS = "pause_next_seconds"
        const val REPS_PER_QUESTION = "reps_per_question"
        const val SESSION_DURATION_MIN = "session_duration_min"
        const val THEME = "theme"
        const val ACTIVE_SKILL_ID = "active_skill_id"
        const val OPERATING_MODE_TEXT = "operating_mode_text"
        const val MODEL = "model"
        const val REASONING = "reasoning"
        const val CHAT_GPT_CONNECTED_AT = "chat_gpt_connected_at"
        const val APP_LOCK_ENABLED = "app_lock_enabled"
    }

    object Defaults {
        const val TTS_PROVIDER = "edge_tts"
        const val EDGE_TTS_VOICE = "de-DE-SeraphinaMultilingualNeural"
        const val GOOGLE_TTS_API_KEY = ""
        const val GOOGLE_TTS_VOICE = "de-DE-Chirp3-HD-Kore"
        const val GROQ_API_KEY = ""
        const val PAUSE_REP_SECONDS = 8
        const val PAUSE_NEXT_SECONDS = 12
        const val REPS_PER_QUESTION = 3
        const val SESSION_DURATION_MIN = 30
        const val THEME = "dark"
        const val ACTIVE_SKILL_ID = 1L
        const val OPERATING_MODE_TEXT = """Erzeuge genau 30 Fragen als Liste. Jede Frage beginnt mit einem passenden Emoji, ist offen formuliert, wird nicht beantwortet und richtet sich direkt an den Hörer („du"). Keine Nummerierung, keine Erklärungen, nur die Fragen."""
        const val MODEL = "gpt-5.6-terra"
        const val REASONING = "medium"
        const val APP_LOCK_ENABLED = false
    }

    companion object {
        const val STORE_NAME = "perfect_moment_secure_prefs"
        val ALLOWED_DURATIONS = setOf(10, 20, 30, 45, 60, 90, 120)
        val ALLOWED_THEMES = setOf("light", "dark", "system")
    }
}
