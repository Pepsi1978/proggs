package de.frank.aufgaben.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.aufgaben.auth.CodexModel
import de.frank.aufgaben.auth.ReasoningEffort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Alle Einstellungen und Schlüssel, verschlüsselt. Die Vorlese-Schlüssel teilt sich diese Datei mit
 * dem [de.frank.aufgaben.tts.TtsManager] — gleicher Speichername, gleiche Schlüsselnamen.
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
    } catch (_: Exception) {
        context.applicationContext.getSharedPreferences("aufgaben_prefs_fallback", Context.MODE_PRIVATE)
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
    /** Beim Tipp aufs Plus gleich die Aufnahme starten. */
    var autoMikro: Boolean get() = prefs.getBoolean("auto_mikro", true); set(v) = put { putBoolean("auto_mikro", v) }

    // ---- KI ----
    var modell: CodexModel
        get() = CodexModel.entries.firstOrNull { it.name == s("model", "") } ?: CodexModel.TERRA
        set(v) = put { putString("model", v.name) }
    var denkstufe: ReasoningEffort
        get() = ReasoningEffort.entries.firstOrNull { it.name == s("reasoning", "") } ?: ReasoningEffort.LOW
        set(v) = put { putString("reasoning", v.name) }
    var kiTitel: Boolean get() = prefs.getBoolean("ki_titel", true); set(v) = put { putBoolean("ki_titel", v) }

    // ---- Vorlesen (Schlüssel wie im TtsManager) ----
    var ttsAnbieter: String get() = s("tts_provider", "edge_tts"); set(v) = put { putString("tts_provider", v) }
    var edgeStimme: String get() = s("edge_tts_voice", "de-DE-SeraphinaMultilingualNeural"); set(v) = put { putString("edge_tts_voice", v) }
    var googleKey: String get() = s("google_tts_api_key", ""); set(v) = put { putString("google_tts_api_key", v.trim()) }
    var googleStimme: String get() = s("google_tts_voice", "de-DE-Chirp3-HD-Kore"); set(v) = put { putString("google_tts_voice", v) }
    var qwenKey: String get() = s("qwen_tts_api_key", ""); set(v) = put { putString("qwen_tts_api_key", v.trim()) }
    var qwenStimme: String get() = s("qwen_tts_voice_id", ""); set(v) = put { putString("qwen_tts_voice_id", v.trim()) }
    var sprechtempo: Float
        get() = prefs.getFloat("tts_speech_rate", 1f)
        set(v) = put { putFloat("tts_speech_rate", v.coerceIn(0.7f, 1.3f)) }

    // ---- Aussehen ----
    var design: String get() = s("design", "orange"); set(v) = put { putString("design", v) }
    /** system | hell | dunkel */
    var modus: String get() = s("modus", "system"); set(v) = put { putString("modus", v) }
    var szeneZeigen: Boolean get() = prefs.getBoolean("szene", true); set(v) = put { putBoolean("szene", v) }

    // ---- Erinnerungen ----
    /** Eigener Ton (classic, bells, pulse, chime) oder "uri:<Systemton>". */
    var ton: String get() = s("ton", "chime"); set(v) = put { putString("ton", v) }
    var tonName: String get() = s("ton_name", "Erinnerungszeichen"); set(v) = put { putString("ton_name", v) }
    var lautstaerke: Float get() = prefs.getFloat("lautstaerke", 0.8f); set(v) = put { putFloat("lautstaerke", v.coerceIn(0f, 1f)) }
    var vorlaufStandard: Int get() = prefs.getInt("vorlauf", 10); set(v) = put { putInt("vorlauf", v) }
    var vibration: Boolean get() = prefs.getBoolean("vibration", true); set(v) = put { putBoolean("vibration", v) }

    // ---- Zeitleiste (Minuten ab 0 Uhr, 30-Minuten-Schritte) ----
    var zeitleisteVon: Int get() = prefs.getInt("zeitleiste_von", 5 * 60); set(v) = put { putInt("zeitleiste_von", v.coerceIn(0, 23 * 60)) }
    var zeitleisteBis: Int get() = prefs.getInt("zeitleiste_bis", 22 * 60); set(v) = put { putInt("zeitleiste_bis", v.coerceIn(60, 24 * 60)) }
    /** Leiste passt sich an die eingetragenen Termine an; beim Ziehen gilt wieder Von/Bis. */
    var zeitleisteAuto: Boolean get() = prefs.getBoolean("zeitleiste_auto", false); set(v) = put { putBoolean("zeitleiste_auto", v) }

    // ---- Fokus ----
    var fokusMinuten: Int get() = prefs.getInt("fokus", 25); set(v) = put { putInt("fokus", v.coerceIn(5, 90)) }

    companion object {
        const val NAME = "aufgaben_secure_prefs"
        @Volatile private var instanz: Einstellungen? = null
        fun get(context: Context): Einstellungen = instanz ?: synchronized(this) {
            instanz ?: Einstellungen(context).also { instanz = it }
        }
    }
}
