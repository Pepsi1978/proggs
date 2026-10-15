package de.frank.epigenetik.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.epigenetik.auth.CodexModel
import de.frank.epigenetik.auth.ReasoningEffort
import de.frank.epigenetik.tts.TtsCatalog

/** Wie gründlich eine Recherche arbeitet. */
enum class Tiefe(val anzeige: String, val beschreibung: String) {
    SCHNELL("Schnell", "Mittlere Denkstufe, kürzere Suche. Ergebnis meist in 1–2 Minuten."),
    GRUENDLICH("Gründlich", "Hohe Denkstufe, ausführliche Websuche. Empfohlen."),
    MAXIMAL("Maximal", "Höchste Denkstufe, sehr ausführlich. Dauert am längsten."),
}

/** Alle Einstellungen und Schlüssel, verschlüsselt gespeichert (Vorlese-Einstellungen liest der TtsManager mit). */
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
        context.applicationContext.getSharedPreferences("epigenetik_prefs_fallback", Context.MODE_PRIVATE)
    }

    /** Jeder Lesezugriff liest diesen Zähler mit — so zeichnet Compose nach jeder Änderung neu. */
    private val version = androidx.compose.runtime.mutableIntStateOf(0)
    private val prefs get() = version.intValue.let { roh }

    private fun s(key: String, def: String) = prefs.getString(key, def) ?: def
    private fun put(block: SharedPreferences.Editor.() -> Unit) {
        roh.edit().apply(block).apply()
        version.intValue++
    }

    // ---- Spracheingabe ----
    /** Leer = Spracheingabe des Handys (Google), sonst Groq Whisper. */
    var groqKey: String get() = s("groq_api_key", ""); set(v) = put { putString("groq_api_key", v.trim()) }
    var filterStille: Boolean get() = prefs.getBoolean("filter_vad", true); set(v) = put { putBoolean("filter_vad", v) }
    var filterMetriken: Boolean get() = prefs.getBoolean("filter_segments", true); set(v) = put { putBoolean("filter_segments", v) }
    var filterZeitstempel: Boolean get() = prefs.getBoolean("filter_timestamps", true); set(v) = put { putBoolean("filter_timestamps", v) }
    var filterFloskeln: Boolean get() = prefs.getBoolean("filter_phrases", true); set(v) = put { putBoolean("filter_phrases", v) }
    /** Nach dem Einsprechen sofort recherchieren, ohne erst auf den Knopf zu tippen. */
    var sofortNachDiktat: Boolean get() = prefs.getBoolean("sofort_nach_diktat", false); set(v) = put { putBoolean("sofort_nach_diktat", v) }

    // ---- KI ----
    var modell: CodexModel
        get() = CodexModel.entries.firstOrNull { it.name == s("model", "") } ?: CodexModel.ASTRA
        set(v) = put { putString("model", v.name) }
    var tiefe: Tiefe
        get() = Tiefe.entries.firstOrNull { it.name == s("tiefe", "") } ?: Tiefe.GRUENDLICH
        set(v) = put { putString("tiefe", v.name) }
    val denkstufe: ReasoningEffort
        get() = modell.normalizeEffort(
            when (tiefe) { Tiefe.SCHNELL -> ReasoningEffort.MEDIUM; Tiefe.GRUENDLICH -> ReasoningEffort.HIGH; Tiefe.MAXIMAL -> ReasoningEffort.XHIGH },
        )
    /** Wie viele Recherchen gleichzeitig laufen dürfen. */
    var parallel: Int get() = prefs.getInt("parallel", 3).coerceIn(1, 5); set(v) = put { putInt("parallel", v.coerceIn(1, 5)) }

    // ---- Kurzprofil (fließt in jede Anfrage ein) ----
    var alter: String get() = s("profil_alter", ""); set(v) = put { putString("profil_alter", v.trim()) }
    var geschlecht: String get() = s("profil_geschlecht", ""); set(v) = put { putString("profil_geschlecht", v) }
    var profil: String get() = s("profil_text", ""); set(v) = put { putString("profil_text", v) }

    // ---- Vorlesen (Schlüssel teilt sich der TtsManager) ----
    var ttsAnbieter: String get() = s("tts_provider", TtsCatalog.DEFAULT_PROVIDER.id); set(v) = put { putString("tts_provider", v) }
    var edgeStimme: String get() = s("edge_tts_voice", TtsCatalog.DEFAULT_EDGE_VOICE); set(v) = put { putString("edge_tts_voice", v) }
    var googleKey: String get() = s("google_tts_api_key", ""); set(v) = put { putString("google_tts_api_key", v.trim()) }
    var googleStimme: String get() = s("google_tts_voice", TtsCatalog.DEFAULT_GOOGLE_VOICE); set(v) = put { putString("google_tts_voice", v) }
    var tempo: Float get() = prefs.getFloat("tts_speech_rate", 1f); set(v) = put { putFloat("tts_speech_rate", v.coerceIn(0.7f, 1.3f)) }

    // ---- Aussehen ----
    var design: String get() = s("design", "orange"); set(v) = put { putString("design", v) }
    /** system | hell | dunkel */
    var modus: String get() = s("modus", "system"); set(v) = put { putString("modus", v) }

    /** Das Kurzprofil als Text für die KI, leer wenn nichts angegeben ist. */
    fun profilText(): String = buildList {
        if (alter.isNotBlank()) add("Alter: $alter")
        if (geschlecht.isNotBlank()) add("Geschlecht: $geschlecht")
        if (profil.isNotBlank()) add("Weitere Angaben: ${profil.trim()}")
    }.joinToString("; ")

    companion object {
        const val NAME = "epigenetik_secure_prefs"
        @Volatile private var instanz: Einstellungen? = null
        fun get(context: Context): Einstellungen = instanz ?: synchronized(this) {
            instanz ?: Einstellungen(context.applicationContext).also { instanz = it }
        }
    }
}
