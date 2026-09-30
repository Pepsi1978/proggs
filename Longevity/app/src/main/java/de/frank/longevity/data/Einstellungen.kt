package de.frank.longevity.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.longevity.auth.CodexModel
import de.frank.longevity.auth.ReasoningEffort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Alle Einstellungen und Schlüssel, verschlüsselt gespeichert. */
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
        context.applicationContext.getSharedPreferences("longevity_prefs_fallback", Context.MODE_PRIVATE)
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

    // ---- KI für Auswertung, Vertiefung und Aktualisierung ----
    var modell: CodexModel
        get() = CodexModel.entries.firstOrNull { it.name == s("model", "") } ?: CodexModel.ASTRA
        set(v) = put { putString("model", v.name) }
    var denkstufe: ReasoningEffort
        get() = ReasoningEffort.entries.firstOrNull { it.name == s("reasoning", "") } ?: ReasoningEffort.HIGH
        set(v) = put { putString("reasoning", v.name) }

    // ---- KI für die Textkorrektur ----
    var korrekturModell: CodexModel
        get() = CodexModel.entries.firstOrNull { it.name == s("korrektur_model", "") } ?: CodexModel.TERRA
        set(v) = put { putString("korrektur_model", v.name) }
    var korrekturDenkstufe: ReasoningEffort
        get() = ReasoningEffort.entries.firstOrNull { it.name == s("korrektur_reasoning", "") } ?: ReasoningEffort.LOW
        set(v) = put { putString("korrektur_reasoning", v.name) }

    // ---- Kurzprofil (fließt in jede KI-Anfrage ein, leer = allgemein) ----
    var alter: String get() = s("profil_alter", ""); set(v) = put { putString("profil_alter", v.trim()) }
    var geschlecht: String get() = s("profil_geschlecht", ""); set(v) = put { putString("profil_geschlecht", v) }
    var profil: String get() = s("profil_text", ""); set(v) = put { putString("profil_text", v) }

    // ---- Aussehen ----
    var design: String get() = s("design", "morgenrot"); set(v) = put { putString("design", v) }
    /** system | hell | dunkel */
    var modus: String get() = s("modus", "system"); set(v) = put { putString("modus", v) }
    var szeneZeigen: Boolean get() = prefs.getBoolean("szene", true); set(v) = put { putBoolean("szene", v) }

    // ---- Stand ----
    /** Die alten Verbots-Faktoren sind in Lebenszeit-Räuber mit Minus-Jahren umgestellt. */
    var verboteUmgestellt: Boolean get() = prefs.getBoolean("verbote_umgestellt", false); set(v) = put { putBoolean("verbote_umgestellt", v) }
    /** Eigener Aktualisierungs-Prompt (Markdown mit „## “-Abschnitten); leer = Standard aus assets/aktualisierung.md. */
    var aktualisierungsPrompt: String get() = s("aktualisierungs_prompt", ""); set(v) = put { putString("aktualisierungs_prompt", v) }
    var letzteAktualisierung: Long get() = prefs.getLong("letzte_aktualisierung", 0L); set(v) = put { putLong("letzte_aktualisierung", v) }

    /** Das Kurzprofil als Text für die KI, leer wenn nichts angegeben ist. */
    fun profilText(): String = buildList {
        if (alter.isNotBlank()) add("Alter: $alter")
        if (geschlecht.isNotBlank()) add("Geschlecht: $geschlecht")
        if (profil.isNotBlank()) add("Weitere Angaben: ${profil.trim()}")
    }.joinToString("; ")

    companion object {
        const val NAME = "longevity_secure_prefs"
        @Volatile private var instanz: Einstellungen? = null
        fun get(context: Context): Einstellungen = instanz ?: synchronized(this) {
            instanz ?: Einstellungen(context).also { instanz = it }
        }
    }
}
