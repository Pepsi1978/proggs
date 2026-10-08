package de.frank.modellkompass.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.modellkompass.auth.CodexModel
import de.frank.modellkompass.auth.ReasoningEffort

/** Alle Einstellungen, verschlüsselt abgelegt. */
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
        context.applicationContext.getSharedPreferences("modellkompass_prefs_fallback", Context.MODE_PRIVATE)
    }

    /** Jeder Lesezugriff liest diesen Zähler mit — so zeichnet Compose nach jeder Änderung neu. */
    private val version = androidx.compose.runtime.mutableIntStateOf(0)
    private val prefs get() = version.intValue.let { roh }

    private fun s(key: String, def: String) = prefs.getString(key, def) ?: def
    private fun put(block: SharedPreferences.Editor.() -> Unit) {
        roh.edit().apply(block).apply()
        version.intValue++
    }

    // ---- KI ----
    var modell: CodexModel
        get() = CodexModel.entries.firstOrNull { it.name == s("model", "") } ?: CodexModel.TERRA
        set(v) = put { putString("model", v.name) }
    var denkstufe: ReasoningEffort
        get() = ReasoningEffort.entries.firstOrNull { it.name == s("reasoning", "") } ?: ReasoningEffort.MEDIUM
        set(v) = put { putString("reasoning", v.name) }
    /** Priority-Verarbeitung: schneller, verbraucht aber deutlich mehr Kontingent. */
    var prioritaet: Boolean get() = prefs.getBoolean("prioritaet", false); set(v) = put { putBoolean("prioritaet", v) }

    // ---- Recherche ----
    /** So viele Modelle soll die Recherche je Bereich liefern. */
    var anzahl: Int get() = prefs.getInt("anzahl", 5); set(v) = put { putInt("anzahl", v.coerceIn(3, 8)) }
    var hardware: String get() = s("hardware", HARDWARE_STANDARD); set(v) = put { putString("hardware", v) }

    // ---- Aussehen ----
    /** system | hell | dunkel */
    var modus: String get() = s("modus", "system"); set(v) = put { putString("modus", v) }

    companion object {
        const val NAME = "modellkompass_secure_prefs"
        const val HARDWARE_STANDARD = "NVIDIA RTX 3090 mit 24 GB VRAM"
        @Volatile private var instanz: Einstellungen? = null
        fun get(context: Context): Einstellungen = instanz ?: synchronized(this) {
            instanz ?: Einstellungen(context).also { instanz = it }
        }
    }
}
