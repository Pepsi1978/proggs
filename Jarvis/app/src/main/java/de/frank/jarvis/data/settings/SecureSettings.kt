package de.frank.jarvis.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.ReasoningEffort
import java.io.Closeable
import java.util.WeakHashMap
import javax.crypto.AEADBadTagException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONException
import org.json.JSONObject

/**
 * Alle Einstellungen und Schlüssel — ausschliesslich in [EncryptedSharedPreferences],
 * nie im Klartext (Baustein G).
 *
 * Der verschlüsselte Speicher wird nur einmal je Prozess geöffnet (Keystore und Schlüsselsatz kosten jedes Mal
 * spürbar Zeit); jede Instanz teilt ihn. Bevorzugt [get] verwenden, das liefert immer dieselbe Instanz.
 */
class SecureSettings(context: Context) : Closeable {
    private val appContext = context.applicationContext

    // Vor den Flows deklariert, damit sie schon stehen, wenn deren Startwerte [preferences] lesen.
    /** true, sobald der Aufbau fertig ist: Erst dann darf der Hörer [beiAenderung] rufen (braucht die Flows). */
    @Volatile private var aufgebaut = false
    /** true = beim geteilten Hörer angemeldet oder per [close] abgemeldet; dann nicht (wieder) anmelden. */
    @Volatile private var angemeldet = false

    /**
     * Kein `lazy`: Das hätte ein einmaliges Scheitern beim Öffnen (Keystore beim Start noch nicht bereit) für immer als
     * null behalten. [speicher] gibt nach dem ersten Erfolg die gecachte Instanz zurück und versucht es sonst erneut.
     */
    private val preferences: SharedPreferences?
        get() = speicher(appContext)?.also { if (aufgebaut && !angemeldet) anmelden(spaet = true) }

    private val _themeFlow = MutableStateFlow(readString(Keys.THEME, Defaults.THEME))
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    /** Änderungen am geteilten Speicher; ein einziger Hörer im Begleitobjekt verteilt sie an alle lebenden Instanzen. */
    private fun beiAenderung(prefs: SharedPreferences, key: String?) {
        when (key) {
            Keys.THEME -> _themeFlow.value = prefs.getString(Keys.THEME, Defaults.THEME) ?: Defaults.THEME
        }
    }

    init {
        // Schwach gehalten: Instanzen ohne close() (etwa je Vorlesen) bleiben so nicht ewig am Speicher hängen.
        aufgebaut = true
        if (preferences != null) anmelden(spaet = false)
    }

    /**
     * Meldet diese Instanz beim geteilten Hörer an. [spaet] = der Speicher ließ sich erst nach dem Aufbau öffnen; dann
     * standen die Flows noch auf den Standardwerten und werden einmal aus dem Speicher nachgezogen.
     */
    private fun anmelden(spaet: Boolean) {
        val neu = synchronized(instanzen) {
            if (angemeldet) false else { instanzen[this] = true; angemeldet = true; true }
        }
        if (neu && spaet) {
            _themeFlow.value = readString(Keys.THEME, Defaults.THEME)
        }
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

    /** Gemini-Schlüssel aus AI Studio, Projekt ohne Abrechnung (kostenloses Kontingent); für Gemini Live. */
    var geminiApiKey: String
        get() = readString(Keys.GEMINI_API_KEY, "")
        set(value) = writeString(Keys.GEMINI_API_KEY, value)

    /** Gemini-Schlüssel aus einem AI-Studio-Projekt mit Abrechnung; Ersatz, wenn das kostenlose Kontingent erschöpft ist. */
    var geminiApiKeyBezahlt: String
        get() = readString(Keys.GEMINI_API_KEY_BEZAHLT, "")
        set(value) = writeString(Keys.GEMINI_API_KEY_BEZAHLT, value)

    // ---- Live-Gespräch (OpenAI Realtime) ----

    /** Eigener OpenAI-Plattform-Schlüssel (sk-…) für das Live-Gespräch; das ChatGPT-Abo reicht dafür nicht. */
    var openAiApiKey: String
        get() = readString(Keys.OPENAI_API_KEY, "")
        set(value) = writeString(Keys.OPENAI_API_KEY, value)

    /** Eine Einstellung des Live-Gesprächs (Liste in [de.frank.jarvis.live.LiveOptionen]); leer = Standard. */
    fun liveOption(id: String): String = readString("liveopt_$id", "")

    fun setzeLiveOption(id: String, wert: String) = writeString("liveopt_$id", wert)

    var model: String
        get() = readString(Keys.MODEL, Defaults.MODEL)
        set(value) {
            val effort = CodexModel.fromLabel(value).normalizeEffort(ReasoningEffort.fromLabel(reasoning))
            preferences?.edit()?.putString(Keys.MODEL, value)?.putString(Keys.REASONING, effort.apiValue)?.apply()
        }

    var reasoning: String
        get() = CodexModel.fromLabel(model).normalizeEffort(
            ReasoningEffort.fromLabel(readString(Keys.REASONING, Defaults.REASONING)),
        ).apiValue
        set(value) = writeString(Keys.REASONING,
            CodexModel.fromLabel(model).normalizeEffort(ReasoningEffort.fromLabel(value)).apiValue)

    var chatGptConnectedAt: Long
        get() = preferences?.getLong(Keys.CHAT_GPT_CONNECTED_AT, 0L) ?: 0L
        set(value) {
            preferences?.edit()?.putLong(Keys.CHAT_GPT_CONNECTED_AT, value.coerceAtLeast(0L))?.apply()
        }

    // ---- Darstellung ----

    var theme: String
        get() = readString(Keys.THEME, Defaults.THEME)
        set(value) {
            val normalized = value.takeIf(ALLOWED_THEMES::contains) ?: Defaults.THEME
            _themeFlow.value = normalized
            writeString(Keys.THEME, normalized)
        }

    // ---- Sicherung (Baustein J) ----

    var letzteSicherungAm: Long
        get() = preferences?.getLong(Keys.LETZTE_SICHERUNG, 0L) ?: 0L
        set(value) {
            preferences?.edit()?.putLong(Keys.LETZTE_SICHERUNG, value)?.apply()
        }

    override fun close() {
        // Die geteilte Instanz aus get() bleibt angemeldet; sie lebt so lange wie die App.
        if (this !== instanz) synchronized(instanzen) { instanzen.remove(this); angemeldet = true }
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
        const val GEMINI_API_KEY = "gemini_api_key"
        const val GEMINI_API_KEY_BEZAHLT = "gemini_api_key_bezahlt"
        const val OPENAI_API_KEY = "openai_api_key"
        const val MODEL = "model"
        const val REASONING = "reasoning"
        const val CHAT_GPT_CONNECTED_AT = "chat_gpt_connected_at"
        const val THEME = "theme"
        const val LETZTE_SICHERUNG = "letzte_sicherung"
    }

    object Defaults {
        const val TTS_PROVIDER = "edge_tts"
        const val EDGE_TTS_VOICE = "de-DE-SeraphinaMultilingualNeural"
        const val GOOGLE_TTS_VOICE = "de-DE-Chirp3-HD-Kore"
        const val QWEN_STANDARD_VOICE = "Cherry"
        const val TTS_SPEECH_RATE = 1f
        const val MODEL = "gpt-5.6-terra"
        const val REASONING = "medium"
        const val THEME = "light"
    }

    companion object {
        const val STORE_NAME = "jarvis_sprache_prefs"
        private const val TAG = "SecureSettings"

        @Volatile private var geteilt: SharedPreferences? = null
        @Volatile private var instanz: SecureSettings? = null
        private val instanzen = WeakHashMap<SecureSettings, Boolean>()
        private val hoerer = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            val alle = synchronized(instanzen) { instanzen.keys.toList() }
            alle.forEach { it.beiAenderung(prefs, key) }
        }

        /** Letzter gescheiterter Öffnungsversuch: Jeder Lesezugriff versucht neu, aber höchstens alle paar Sekunden. */
        @Volatile private var letzterFehlversuch = 0L
        private const val WIEDERHOLEN_NACH_MS = 5_000L

        private fun verschluesselt(context: Context): SharedPreferences = EncryptedSharedPreferences.create(
            STORE_NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

        /**
         * Der verschlüsselte Speicher, einmal je Prozess geöffnet. Scheitert es, wird beim nächsten Zugriff neu versucht
         * (gedrosselt). Passt der Schlüssel nicht mehr zu den Daten (AEADBadTag, etwa nach einer Wiederherstellung auf
         * einem neuen Gerät), sind sie ohnehin unlesbar: dann wie in Einstellungen löschen und neu anlegen.
         */
        private fun speicher(context: Context): SharedPreferences? = geteilt ?: synchronized(this) {
            geteilt ?: run {
                if (letzterFehlversuch != 0L && SystemClock.elapsedRealtime() - letzterFehlversuch < WIEDERHOLEN_NACH_MS) return@run null
                val app = context.applicationContext
                val offen = try {
                    // Ein zweiter Versuch nach kurzer Pause, wie in Einstellungen: Ein einmaliger Keystore-Fehler beim Kaltstart
                    // darf nicht alle Schlüssel löschen.
                    runCatching { verschluesselt(app) }.getOrElse { Thread.sleep(200); verschluesselt(app) }
                } catch (e: Exception) {
                    if (generateSequence<Throwable>(e) { it.cause }.any { it is AEADBadTagException }) {
                        Log.e(TAG, "Verschlüsselter Speicher war unlesbar und wird neu angelegt. Schlüssel bitte neu eintragen.", e)
                        runCatching { app.deleteSharedPreferences(STORE_NAME) }
                        runCatching { verschluesselt(app) }.onFailure { Log.w(TAG, "Neu anlegen fehlgeschlagen", it) }.getOrNull()
                    } else {
                        Log.w(TAG, "Verschlüsselter Speicher ließ sich nicht öffnen; neuer Versuch beim nächsten Zugriff", e)
                        null
                    }
                }
                if (offen == null) { letzterFehlversuch = SystemClock.elapsedRealtime(); null }
                else offen.also { it.registerOnSharedPreferenceChangeListener(hoerer); geteilt = it; letzterFehlversuch = 0L }
            }
        }

        /** Die eine geteilte Instanz für die ganze App. Am besten einmal beim Start im Hintergrund aufrufen (vorwärmen). */
        fun get(context: Context): SecureSettings = instanz ?: synchronized(this) {
            instanz ?: SecureSettings(context.applicationContext).also { instanz = it }
        }
        /** Genau zwei Modi — die App folgt der Systemvorgabe bewusst nicht (Baustein A). */
        val ALLOWED_THEMES = setOf("light", "dark")
        const val MIN_TTS_SPEECH_RATE = 0.5f
        const val MAX_TTS_SPEECH_RATE = 2.0f
    }
}
