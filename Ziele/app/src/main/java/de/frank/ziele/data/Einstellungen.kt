package de.frank.ziele.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.ziele.auth.CodexModel
import de.frank.ziele.auth.ReasoningEffort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Alle Einstellungen und Schlüssel, verschlüsselt. Die Vorlese-Schlüssel teilt sich diese Datei mit
 * dem [de.frank.ziele.tts.TtsManager] — gleicher Speichername, gleiche Schlüsselnamen.
 */
class Einstellungen private constructor(context: Context) {
    private val roh: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            NAME,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) { // no-cancellation-rethrow
        context.applicationContext.getSharedPreferences("ziele_prefs_fallback", Context.MODE_PRIVATE)
    }

    /** Zählt jede Änderung hoch, damit die Oberfläche neu liest. */
    private val _stand = MutableStateFlow(0)
    val stand: StateFlow<Int> = _stand.asStateFlow()

    /** Jeder Lesezugriff liest diesen Zähler mit — so zeichnet Compose nach jeder Änderung neu. */
    private val version = androidx.compose.runtime.mutableIntStateOf(0)
    private val prefs get() = version.intValue.let { roh }

    private fun s(key: String, def: String) = prefs.getString(key, def) ?: def
    private fun put(block: SharedPreferences.Editor.() -> Unit) {
        roh.edit().apply(block).apply()
        _stand.value++
        version.intValue++
    }

    // ---- Spracheingabe ----
    var groqKey: String get() = s("groq_api_key", ""); set(v) = put { putString("groq_api_key", v.trim()) }
    var filterStille: Boolean get() = prefs.getBoolean("filter_vad", true); set(v) = put { putBoolean("filter_vad", v) }
    var filterMetriken: Boolean get() = prefs.getBoolean("filter_segments", true); set(v) = put { putBoolean("filter_segments", v) }
    var filterZeitstempel: Boolean get() = prefs.getBoolean("filter_timestamps", true); set(v) = put { putBoolean("filter_timestamps", v) }
    var filterFloskeln: Boolean get() = prefs.getBoolean("filter_phrases", true); set(v) = put { putBoolean("filter_phrases", v) }
    /** Whisper-Modell bei Groq: Turbo (schnell, Standard) oder Large V3 (genauer). */
    var whisperModell: String
        get() = de.frank.ziele.audio.GroqTranscriber.gueltigesModell(prefs.getString("whisper_model", null))
        set(v) { de.frank.ziele.audio.GroqTranscriber.sperreAufheben(); put { putString("whisper_model", de.frank.ziele.audio.GroqTranscriber.gueltigesModell(v)) } }
    /** Persönliches Wörterbuch: Begriffe gehen als Prompt an Whisper. */
    var woerterbuchAn: Boolean get() = prefs.getBoolean("woerterbuch_an", true); set(v) = put { putBoolean("woerterbuch_an", v) }
    var woerterbuch: String get() = s("woerterbuch", de.frank.ziele.audio.Woerterbuch.STANDARD_WOERTERBUCH); set(v) = put { putString("woerterbuch", v) }
    /** Prompt für die nächste Aufnahme, oder null (Schalter aus / keine Begriffe). */
    val woerterbuchPrompt: String? get() = de.frank.ziele.audio.Woerterbuch.prompt(woerterbuchAn, woerterbuch)

    // ---- KI ----
    var modell: CodexModel
        get() = CodexModel.entries.firstOrNull { it.name == s("model", "") } ?: CodexModel.SOL
        set(v) = put { putString("model", v.name) }
    var denkstufe: ReasoningEffort
        get() = ReasoningEffort.entries.firstOrNull { it.name == s("reasoning", "") } ?: ReasoningEffort.HIGH
        set(v) = put { putString("reasoning", v.name) }
    /** Die Auswertung recherchiert zuerst im Internet (Websuche des Modells). */
    var recherche: Boolean get() = prefs.getBoolean("recherche", true); set(v) = put { putBoolean("recherche", v) }
    /** Jarvis' Live-Gedächtnis geht als Kontext mit (über die Jarvis-Brücke). */
    var jarvisKontext: Boolean get() = prefs.getBoolean("jarvis_kontext", true); set(v) = put { putBoolean("jarvis_kontext", v) }
    /** Wie viele Vorschläge eine Auswertung etwa liefert. */
    var vorschlagMenge: String get() = s("vorschlag_menge", "normal"); set(v) = put { putString("vorschlag_menge", v) }

    // ---- Vorlesen (Schlüssel wie im TtsManager) ----
    var ttsAnbieter: String get() = s("tts_provider", "edge_tts"); set(v) = put { putString("tts_provider", v) }
    var edgeStimme: String get() = s("edge_tts_voice", "de-DE-SeraphinaMultilingualNeural"); set(v) = put { putString("edge_tts_voice", v) }
    var googleKey: String get() = s("google_tts_api_key", ""); set(v) = put { putString("google_tts_api_key", v.trim()) }
    var googleStimme: String get() = s("google_tts_voice", "de-DE-Chirp3-HD-Kore"); set(v) = put { putString("google_tts_voice", v) }
    var qwenKey: String get() = s("qwen_tts_api_key", ""); set(v) = put { putString("qwen_tts_api_key", v.trim()) }
    var qwenStimme: String get() = s("qwen_tts_voice_id", ""); set(v) = put { putString("qwen_tts_voice_id", v.trim()) }
    /** Mehrsprachige Stimmen an Deutsch binden (gleicher Schlüssel wie im TtsManager). */
    val immerDeutsch: Boolean get() = prefs.getBoolean("immer_deutsch_vorlesen", true)
    var sprechtempo: Float
        get() = prefs.getFloat("tts_speech_rate", 1f)
        set(v) = put { putFloat("tts_speech_rate", v.coerceIn(0.7f, 1.3f)) }

    // ---- Gemini 3.8 Flash / Flash-Lite TTS und Azure Neural (Schlüssel wie im TtsManager) ----
    /** Favoriten aller Stimmlisten (langes Drücken), stehen oben. */
    var favoriten: Set<String>
        get() = prefs.getStringSet("favorite_tts_voices", emptySet())?.toSet().orEmpty()
        set(v) = put { putStringSet("favorite_tts_voices", v.toSet()) }
    var geminiFlashStimme: String get() = s("gemini_flash_tts_voice", de.frank.ziele.tts.TtsCatalog.DEFAULT_GEMINI_FLASH_VOICE); set(v) = put { putString("gemini_flash_tts_voice", v) }
    var geminiLiteStimme: String get() = s("gemini_lite_tts_voice", de.frank.ziele.tts.TtsCatalog.DEFAULT_GEMINI_LITE_VOICE); set(v) = put { putString("gemini_lite_tts_voice", v) }
    var azureStimme: String get() = s("azure_tts_voice", de.frank.ziele.tts.TtsCatalog.DEFAULT_AZURE_VOICE); set(v) = put { putString("azure_tts_voice", v) }
    var geminiKeyFrei: String get() = s("gemini_tts_key_frei", ""); set(v) = put { putString("gemini_tts_key_frei", v.filterNot(Char::isWhitespace)) }
    var geminiKeyBezahlt: String get() = s("gemini_tts_key_bezahlt", ""); set(v) = put { putString("gemini_tts_key_bezahlt", v.filterNot(Char::isWhitespace)) }
    /** "Erst kostenlos, dann bezahlt" (Standard), "Nur kostenlos" oder "Nur bezahlt". */
    var geminiSchluesselWahl: String
        get() = s("gemini_tts_schluessel_wahl", de.frank.ziele.tts.NeueStimmenPlayer.GEMINI_TTS_FREI_DANN_BEZAHLT)
        set(v) = put { putString("gemini_tts_schluessel_wahl", v) }
    /** Scheitert der kostenlose Gemini-Schlüssel, liest Chirp 3 HD mit derselben Stimme weiter statt des bezahlten. */
    var geminiChirpRueckfall: Boolean get() = prefs.getBoolean("gemini_tts_chirp_rueckfall", false); set(v) = put { putBoolean("gemini_tts_chirp_rueckfall", v) }
    var azureKey: String get() = s("azure_tts_key", ""); set(v) = put { putString("azure_tts_key", v.filterNot(Char::isWhitespace)) }
    var azureRegion: String get() = s("azure_tts_region", de.frank.ziele.tts.TtsCatalog.DEFAULT_AZURE_REGION); set(v) = put { putString("azure_tts_region", v.trim()) }

    /** Schlüssel-Schnappschuss für die Gemini- und Azure-Stimmen. */
    fun neueStimmenZugang() = de.frank.ziele.tts.NeueStimmenPlayer.Zugang(
        geminiKeyFrei = geminiKeyFrei, geminiKeyBezahlt = geminiKeyBezahlt, schluesselWahl = geminiSchluesselWahl,
        chirpRueckfall = geminiChirpRueckfall, googleKey = googleKey, azureKey = azureKey, azureRegion = azureRegion,
        erzwingeDeutsch = prefs.getBoolean("immer_deutsch_vorlesen", true),
    )

    // ---- Aussehen ----
    /** system | hell | dunkel */
    var modus: String get() = s("modus", "system"); set(v) = put { putString("modus", v) }
    /** Zuletzt gewählte Sortierung der Vorschläge. */
    var sortierung: Sortierung get() = Sortierung.von(s("sortierung", "")); set(v) = put { putString("sortierung", v.name) }
    /** Erreichte und pausierte Ziele in der Liste zeigen. */
    var alleZeigen: Boolean get() = prefs.getBoolean("alle_zeigen", true); set(v) = put { putBoolean("alle_zeigen", v) }

    companion object {
        const val NAME = "ziele_secure_prefs"
        @Volatile private var instanz: Einstellungen? = null
        fun get(context: Context): Einstellungen = instanz ?: synchronized(this) {
            instanz ?: Einstellungen(context).also { instanz = it }
        }
    }
}
