package de.frank.jarvis.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableIntStateOf
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.ReasoningEffort
import java.security.SecureRandom

/** Alle Einstellungen und Schlüssel, verschlüsselt abgelegt. */
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
        context.applicationContext.getSharedPreferences("jarvis_prefs_fallback", Context.MODE_PRIVATE)
    }

    /** Jeder Lesezugriff liest diesen Zähler mit — so zeichnet Compose nach jeder Änderung neu. */
    private val version = mutableIntStateOf(0)
    private val prefs get() = version.intValue.let { roh }

    private fun s(key: String, def: String) = prefs.getString(key, def) ?: def
    private fun put(block: SharedPreferences.Editor.() -> Unit) {
        roh.edit().apply(block).apply()
        version.intValue++
    }

    // ---- Das eigene Modell von Jarvis ----
    var modell: CodexModel
        get() = CodexModel.entries.firstOrNull { it.name == s("model", "") } ?: CodexModel.TERRA
        set(v) = put { putString("model", v.name) }
    var denkstufe: ReasoningEffort
        get() = ReasoningEffort.entries.firstOrNull { it.name == s("reasoning", "") } ?: ReasoningEffort.LOW
        set(v) = put { putString("reasoning", v.name) }

    // ---- Plugin und Server ----
    /** Adresse des eigenen Servers (Jarvis-Relay). Vorgabe aus dem Bau, hier nur zum Überschreiben. */
    var serverHost: String
        get() = s("server_host", "").ifEmpty { BuildConfig.RELAY_HOST }
        set(v) = put { putString("server_host", v.trim().removePrefix("https://").removePrefix("wss://").trimEnd('/')) }
    /** Schlüssel, mit dem sich dieses Handy am Server ausweist. Vorgabe aus dem Bau (~/SK/Jarvis). */
    var serverToken: String
        get() = s("server_token", "").ifEmpty { BuildConfig.RELAY_TOKEN }
        set(v) = put { putString("server_token", v.trim()) }
    /** Der Dienst läuft, sobald die App einmal geöffnet wurde; hier lässt er sich abschalten. */
    var dienstAn: Boolean get() = prefs.getBoolean("dienst_an", true); set(v) = put { putBoolean("dienst_an", v) }

    /**
     * Geheimer Teil der Plugin-Adresse. Wer ihn kennt, darf die Werkzeuge aufrufen — deshalb lang, zufällig
     * und nur auf Wunsch erneuerbar (dann muss die Adresse in ChatGPT neu eingetragen werden).
     */
    val geheimnis: String
        get() = s("geheimnis", "").ifEmpty { neuesGeheimnis() }

    fun neuesGeheimnis(): String {
        val zeichen = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val zufall = SecureRandom()
        val neu = (1..40).map { zeichen[zufall.nextInt(zeichen.length)] }.joinToString("")
        put { putString("geheimnis", neu) }
        return neu
    }

    // ---- E-Mail (Gmail mit App-Passwort) ----
    var mailAdresse: String get() = s("mail_adresse", "barwandt@gmail.com"); set(v) = put { putString("mail_adresse", v.trim()) }
    var mailPasswort: String get() = s("mail_passwort", ""); set(v) = put { putString("mail_passwort", v.trim()) }
    /** Weitere Adressen, an die Jarvis senden darf (durch Komma getrennt). Die eigene ist immer erlaubt. */
    var mailEmpfaenger: String get() = s("mail_empfaenger", ""); set(v) = put { putString("mail_empfaenger", v.trim()) }

    // ---- Internet-Recherche der Agenten ----
    var suchSchluessel: String
        get() = s("such_schluessel", "").ifEmpty { BuildConfig.TAVILY_KEY }
        set(v) = put { putString("such_schluessel", v.trim()) }

    // ---- Wetter ----
    /** Ort der Wettervorhersage, mit seinen Koordinaten. */
    var wetterOrt: String get() = s("wetter_ort", "Neuenhagen bei Berlin"); set(v) = put { putString("wetter_ort", v.trim()) }
    var wetterBreite: Double get() = s("wetter_breite", "52.529").toDoubleOrNull() ?: 52.529; set(v) = put { putString("wetter_breite", v.toString()) }
    var wetterLaenge: Double get() = s("wetter_laenge", "13.689").toDoubleOrNull() ?: 13.689; set(v) = put { putString("wetter_laenge", v.toString()) }

    // ---- Tagesauswertung ----
    var auswertungAn: Boolean get() = prefs.getBoolean("auswertung_an", true); set(v) = put { putBoolean("auswertung_an", v) }
    /** Bis zu drei Uhrzeiten als „HH:MM,HH:MM,HH:MM“. */
    var auswertungZeiten: String get() = s("auswertung_zeiten", "04:25,12:00,16:25"); set(v) = put { putString("auswertung_zeiten", v) }

    /** Einmaliger Zusatzlauf (Zeitpunkt in Millisekunden), wenn morgens der Schlafwert noch fehlte. 0 = keiner. */
    var nachbesserungUm: Long get() = prefs.getLong("nachbesserung_um", 0L); set(v) = put { putLong("nachbesserung_um", v) }

    // ---- Sperre und Vorlesen ----
    /** App nur nach Fingerabdruck (oder Gerätesperre) öffnen. Das Plugin arbeitet unabhängig davon. */
    var appSperre: Boolean get() = prefs.getBoolean("app_sperre", true); set(v) = put { putBoolean("app_sperre", v) }
    /** Antworten vorlesen, wenn die Frage gesprochen wurde. */
    var antwortenVorlesen: Boolean get() = prefs.getBoolean("antworten_vorlesen", true); set(v) = put { putBoolean("antworten_vorlesen", v) }
    /** Fertige Agenten-Ergebnisse von selbst vorlesen. */
    var ergebnisseVorlesen: Boolean get() = prefs.getBoolean("ergebnisse_vorlesen", false); set(v) = put { putBoolean("ergebnisse_vorlesen", v) }

    /** system | hell | dunkel */
    var modus: String get() = s("modus", "system"); set(v) = put { putString("modus", v) }

    companion object {
        const val NAME = "jarvis_secure_prefs"
        @Volatile private var instanz: Einstellungen? = null
        fun get(context: Context): Einstellungen = instanz ?: synchronized(this) {
            instanz ?: Einstellungen(context).also { instanz = it }
        }
    }
}
