package de.frank.perfectmoment.data.settings

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

internal const val MIN_PAUSE_SECONDS = 2
internal const val MAX_PAUSE_SECONDS = 60
internal const val MAX_REPETITIONS_PER_QUESTION = 30

internal fun normalizePauseSeconds(value: Int): Int =
    ((value.coerceIn(MIN_PAUSE_SECONDS, MAX_PAUSE_SECONDS) + 1) / 2) * 2

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

    var qwenTtsApiKey: String
        get() = readString(Keys.QWEN_TTS_API_KEY, Defaults.QWEN_TTS_API_KEY)
        set(value) = writeString(Keys.QWEN_TTS_API_KEY, value)

    var qwenTtsVoiceId: String
        get() = readString(Keys.QWEN_TTS_VOICE_ID, Defaults.QWEN_TTS_VOICE_ID)
        set(value) = writeString(Keys.QWEN_TTS_VOICE_ID, value)

    /**
     * The names given to cloned voices in the app, keyed by voice id.
     *
     * Alibaba bakes the name into the id when a voice is created and offers no way to change it
     * afterwards, so a renamed voice keeps its own title here instead.
     */
    var qwenVoiceNames: Map<String, String>
        get() {
            val stored = readString(Keys.QWEN_VOICE_NAMES, "")
            if (stored.isBlank()) return emptyMap()
            return try {
                val json = JSONObject(stored)
                json.keys().asSequence().associateWith { json.optString(it) }
                    .filterValues(String::isNotBlank)
            } catch (error: JSONException) {
                emptyMap()
            }
        }
        set(value) = writeString(Keys.QWEN_VOICE_NAMES, JSONObject(value.toMap<String, Any>()).toString())

    /**
     * The order the own voices appear in, as voice ids.
     *
     * Alibaba returns its own order, so the order the list is dragged into lives here. Ids that
     * are not in this list yet keep following after the known ones.
     */
    var qwenVoiceOrder: List<String>
        get() = readString(Keys.QWEN_VOICE_ORDER, "").split('\n').filter(String::isNotBlank)
        set(value) = writeString(Keys.QWEN_VOICE_ORDER, value.filter(String::isNotBlank).joinToString("\n"))

    /**
     * Die Reihenfolge der eigenen Sessions, als Kennungen.
     *
     * Die Datenbank kennt nur den Zeitpunkt der Anlage, darum lebt die gezogene Reihenfolge hier.
     * Sessions, die noch nicht in der Liste stehen, folgen hinter den bekannten.
     */
    var readingSessionOrder: List<Long>
        get() = readString(Keys.READING_SESSION_ORDER, "").split('\n').mapNotNull(String::toLongOrNull)
        set(value) = writeString(Keys.READING_SESSION_ORDER, value.joinToString("\n"))

    var ttsSpeechRate: Float
        get() = preferences?.getFloat(Keys.TTS_SPEECH_RATE, Defaults.TTS_SPEECH_RATE)
            ?.coerceIn(MIN_TTS_SPEECH_RATE, MAX_TTS_SPEECH_RATE) ?: Defaults.TTS_SPEECH_RATE
        set(value) {
            preferences?.edit()?.putFloat(
                Keys.TTS_SPEECH_RATE,
                value.coerceIn(MIN_TTS_SPEECH_RATE, MAX_TTS_SPEECH_RATE),
            )?.apply()
        }

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
            ?.let(::normalizePauseSeconds) ?: Defaults.PAUSE_REP_SECONDS
        set(value) = writeInt(Keys.PAUSE_REP_SECONDS, normalizePauseSeconds(value))

    var pauseNextSeconds: Int
        get() = preferences?.getInt(Keys.PAUSE_NEXT_SECONDS, Defaults.PAUSE_NEXT_SECONDS)
            ?.let(::normalizePauseSeconds) ?: Defaults.PAUSE_NEXT_SECONDS
        set(value) = writeInt(Keys.PAUSE_NEXT_SECONDS, normalizePauseSeconds(value))

    var repsPerQuestion: Int
        get() = preferences?.getInt(Keys.REPS_PER_QUESTION, Defaults.REPS_PER_QUESTION)
            ?.coerceIn(1, MAX_REPETITIONS_PER_QUESTION) ?: Defaults.REPS_PER_QUESTION
        set(value) = writeInt(Keys.REPS_PER_QUESTION, value.coerceIn(1, MAX_REPETITIONS_PER_QUESTION))

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

    var questionPerspective: String
        get() = readString(Keys.QUESTION_PERSPECTIVE, Defaults.QUESTION_PERSPECTIVE)
        set(value) = writeString(Keys.QUESTION_PERSPECTIVE, value)

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

    var varyVoicePerRepetition: Boolean
        get() = preferences?.getBoolean(Keys.VARY_VOICE, Defaults.VARY_VOICE) ?: Defaults.VARY_VOICE
        set(value) {
            preferences?.edit()?.putBoolean(Keys.VARY_VOICE, value)?.apply()
        }

    var varySpeechRate: Boolean
        get() = preferences?.getBoolean(Keys.VARY_SPEECH_RATE, Defaults.VARY_SPEECH_RATE)
            ?: Defaults.VARY_SPEECH_RATE
        set(value) {
            preferences?.edit()?.putBoolean(Keys.VARY_SPEECH_RATE, value)?.apply()
        }

    var varyPitch: Boolean
        get() = preferences?.getBoolean(Keys.VARY_PITCH, Defaults.VARY_PITCH) ?: Defaults.VARY_PITCH
        set(value) {
            preferences?.edit()?.putBoolean(Keys.VARY_PITCH, value)?.apply()
        }

    var doNotDisturbAccessAsked: Boolean
        get() = preferences?.getBoolean(Keys.DND_ACCESS_ASKED, false) ?: false
        set(value) {
            preferences?.edit()?.putBoolean(Keys.DND_ACCESS_ASKED, value)?.apply()
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
        const val QWEN_TTS_API_KEY = "qwen_tts_api_key"
        const val QWEN_TTS_VOICE_ID = "qwen_tts_voice_id"
        const val QWEN_VOICE_NAMES = "qwen_voice_names"
        const val QWEN_VOICE_ORDER = "qwen_voice_order"
        const val READING_SESSION_ORDER = "reading_session_order"
        const val TTS_SPEECH_RATE = "tts_speech_rate"
        const val FAVORITE_TTS_VOICES = "favorite_tts_voices"
        const val GROQ_API_KEY = "groq_api_key"
        const val PAUSE_REP_SECONDS = "pause_rep_seconds"
        const val PAUSE_NEXT_SECONDS = "pause_next_seconds"
        const val REPS_PER_QUESTION = "reps_per_question"
        const val SESSION_DURATION_MIN = "session_duration_min"
        const val THEME = "theme"
        const val ACTIVE_SKILL_ID = "active_skill_id"
        const val OPERATING_MODE_TEXT = "operating_mode_text"
        const val QUESTION_PERSPECTIVE = "question_perspective"
        const val MODEL = "model"
        const val REASONING = "reasoning"
        const val CHAT_GPT_CONNECTED_AT = "chat_gpt_connected_at"
        const val APP_LOCK_ENABLED = "app_lock_enabled"
        const val DND_ACCESS_ASKED = "dnd_access_asked"
        const val VARY_VOICE = "vary_voice_per_repetition"
        const val VARY_SPEECH_RATE = "vary_speech_rate"
        const val VARY_PITCH = "vary_pitch"
    }

    object Defaults {
        const val TTS_PROVIDER = "edge_tts"
        const val EDGE_TTS_VOICE = "de-DE-SeraphinaMultilingualNeural"
        const val GOOGLE_TTS_API_KEY = ""
        const val GOOGLE_TTS_VOICE = "de-DE-Chirp3-HD-Kore"
        const val QWEN_TTS_API_KEY = ""
        const val QWEN_TTS_VOICE_ID = ""
        const val TTS_SPEECH_RATE = 1f
        const val GROQ_API_KEY = ""
        const val PAUSE_REP_SECONDS = 8
        const val PAUSE_NEXT_SECONDS = 12
        const val REPS_PER_QUESTION = 3
        const val SESSION_DURATION_MIN = 30
        const val THEME = "dark"
        const val ACTIVE_SKILL_ID = 1L
        const val OPERATING_MODE_TEXT = """Erzeuge genau 30 Fragen als Liste. Jede Frage beginnt mit einem passenden Emoji, ist offen formuliert, wird nicht beantwortet und richtet sich direkt an den Hörer („du"). Keine Nummerierung, keine Erklärungen, nur die Fragen."""
        const val QUESTION_PERSPECTIVE = "second_person"
        const val MODEL = "gpt-5.6-terra"
        const val REASONING = "medium"
        const val APP_LOCK_ENABLED = false
        const val VARY_VOICE = true
        const val VARY_SPEECH_RATE = true
        const val VARY_PITCH = true
    }

    companion object {
        const val STORE_NAME = "perfect_moment_secure_prefs"
        val ALLOWED_DURATIONS = setOf(0, 10, 20, 30, 45, 60, 90, 120)
        val ALLOWED_THEMES = setOf("light", "dark", "system")
        const val MIN_TTS_SPEECH_RATE = 0.7f
        const val MAX_TTS_SPEECH_RATE = 1.3f
    }
}
