package de.frank.jarvis.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.ReasoningEffort
import de.frank.jarvis.observability.Logbuch
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.AEADBadTagException

/** Alle Einstellungen und Schlüssel, verschlüsselt abgelegt. */
class Einstellungen private constructor(context: Context) {
    /**
     * true = der verschlüsselte Speicher ließ sich nicht öffnen (Keystore gestört). Dann gelten Schlüssel und Passwörter
     * nur bis zum Neustart (sie liegen nur im Speicher, nie im Klartext), und es entsteht kein neues dauerhaftes
     * Plugin-Geheimnis. Die übrigen Einstellungen bleiben im Ersatzspeicher. Die Oberfläche kann darauf hinweisen.
     */
    @Volatile var gestoert: Boolean = false
        private set

    private val roh: SharedPreferences = oeffne(context.applicationContext)

    private fun verschluesselt(app: Context): SharedPreferences = EncryptedSharedPreferences.create(
        NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        app,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    /**
     * Öffnet den verschlüsselten Speicher. Keystore-Fehler sind oft vorübergehend: einmal kurz warten und erneut
     * versuchen. Passt der Schlüssel nicht mehr zu den Daten (AEADBadTag, etwa nach einer Wiederherstellung auf einem
     * neuen Gerät), sind die Daten ohnehin unlesbar: dann neu anlegen und das melden, statt still umzuschalten.
     */
    private fun oeffne(app: Context): SharedPreferences {
        try { return verschluesselt(app) } catch (e: Exception) { Log.w(TAG, "Verschlüsselter Speicher: erster Versuch fehlgeschlagen", e) }
        SystemClock.sleep(200)
        val fehler = try { return verschluesselt(app) } catch (e: Exception) { e }
        if (generateSequence<Throwable>(fehler) { it.cause }.any { it is AEADBadTagException }) {
            runCatching { app.deleteSharedPreferences(NAME) }
            try {
                val neu = verschluesselt(app)
                meldeStoerung("Verschlüsselter Speicher war unlesbar und wurde neu angelegt. Zugangsdaten bitte neu eintragen.", fehler)
                return neu
            } catch (e: Exception) {
                Log.w(TAG, "Verschlüsselter Speicher ließ sich nicht neu anlegen", e)
            }
        }
        gestoert = true
        meldeStoerung("Verschlüsselter Speicher gestört: Zugangsdaten gelten nur bis zum Neustart, die Plugin-Adresse ist bis dahin eine vorläufige und muss nach Behebung nicht neu eingetragen werden.", fehler)
        return app.getSharedPreferences("jarvis_prefs_fallback", Context.MODE_PRIVATE)
    }

    private fun meldeStoerung(text: String, fehler: Throwable) {
        Log.e(TAG, text, fehler)
        Logbuch.fehler("Einstellungen: Speicher", fehler, text)
        runCatching { Protokoll.melde(Quelle.SYSTEM, "Einstellungen", text, ok = false) }
    }

    /** Jeder Lesezugriff liest diesen Zähler mit — so zeichnet Compose nach jeder Änderung neu. */
    private val version = mutableIntStateOf(0)
    private val prefs get() = version.intValue.let { roh }

    /**
     * Im gestörten Zustand: Schlüssel und Passwörter nur für diese Sitzung im Speicher, statt sie im unverschlüsselten
     * Ersatzspeicher abzulegen oder still zu verwerfen.
     */
    private val sitzung = ConcurrentHashMap<String, String>()

    private fun s(key: String, def: String): String {
        val p = prefs
        if (gestoert && key in GEHEIM) return sitzung[key] ?: def
        return p.getString(key, def) ?: def
    }
    private fun put(block: SharedPreferences.Editor.() -> Unit) {
        roh.edit().apply(block).apply()
        version.intValue++
    }
    /** Schreibt einen geheimen Wert: verschlüsselt, oder im gestörten Zustand nur in den Sitzungsspeicher. */
    private fun putGeheim(key: String, wert: String) {
        if (gestoert) { sitzung[key] = wert; version.intValue++ } else put { putString(key, wert) }
    }

    init {
        // Gestört: Zugangsdaten, die eine ältere Fassung im Klartext-Ersatz abgelegt hat, für diese Sitzung übernehmen und
        // dort entfernen. Das Plugin-Geheimnis bleibt liegen und gilt weiter, damit die eingetragene Adresse nicht wechselt.
        if (gestoert) {
            val alt = GEHEIM.mapNotNull { k -> roh.getString(k, null)?.takeIf { it.isNotEmpty() }?.let { k to it } }
            alt.forEach { (k, v) -> sitzung[k] = v }
            val weg = alt.map { it.first }.filter { it != "geheimnis" }
            if (weg.isNotEmpty()) roh.edit().apply { weg.forEach { remove(it) } }.apply()
        }
        // Zugangsdaten, die ein Bau am PC aus ~/SK mitbringt, dauerhaft übernehmen. Ein späteres Update aus dem
        // Cloud-Bau (ohne ~/SK) verliert sie dann nicht mehr; vorher lebten sie nur in der jeweiligen APK.
        val eingebaut = mapOf(
            "server_host" to BuildConfig.RELAY_HOST, "server_token" to BuildConfig.RELAY_TOKEN, "such_schluessel" to BuildConfig.TAVILY_KEY,
            "drive_client_id" to BuildConfig.DRIVE_CLIENT_ID, "drive_client_secret" to BuildConfig.DRIVE_CLIENT_SECRET, "maps_schluessel" to BuildConfig.MAPS_KEY,
        ).filter { (k, v) -> v.isNotBlank() && roh.getString(k, "").isNullOrEmpty() }
        // Gestört: nichts davon in den Klartext-Ersatz schreiben; die Getter greifen ohnehin auf die Werte aus dem Bau zurück.
        if (eingebaut.isNotEmpty() && !gestoert) roh.edit().apply { eingebaut.forEach { (k, v) -> putString(k, v) } }.apply()
        // Bringt ein Bau einen anderen Maps-Schlüssel mit als der vorige (eigener Schlüssel für Jarvis statt des geliehenen),
        // gilt der neue; die Planung der Losfahr-Meldung beginnt damit von vorn.
        if (!gestoert && BuildConfig.MAPS_KEY.isNotBlank() && roh.getString("maps_schluessel_bau", "") != BuildConfig.MAPS_KEY) {
            roh.edit().putString("maps_schluessel", BuildConfig.MAPS_KEY).putString("maps_schluessel_bau", BuildConfig.MAPS_KEY).remove("abfahrt_zustand").apply()
        }
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
        get() = s("server_host", "").ifEmpty { BuildConfig.RELAY_HOST }.ifEmpty { STANDARD_SERVER }
        set(v) = put { putString("server_host", v.trim().removePrefix("https://").removePrefix("wss://").trimEnd('/')) }
    /** Schlüssel, mit dem sich dieses Handy am Server ausweist. Vorgabe aus dem Bau (~/SK/Jarvis). */
    var serverToken: String
        get() = s("server_token", "").ifEmpty { BuildConfig.RELAY_TOKEN }
        set(v) = putGeheim("server_token", v.trim())
    /** Der Dienst läuft, sobald die App einmal geöffnet wurde; hier lässt er sich abschalten. */
    var dienstAn: Boolean get() = prefs.getBoolean("dienst_an", true); set(v) = put { putBoolean("dienst_an", v) }

    /**
     * Geheimer Teil der Plugin-Adresse. Wer ihn kennt, darf die Werkzeuge aufrufen — deshalb lang, zufällig
     * und nur auf Wunsch erneuerbar (dann muss die Adresse in ChatGPT neu eingetragen werden).
     */
    val geheimnis: String
        // Gestört: das schon gespeicherte Geheimnis aus dem Ersatzspeicher (siehe init), sonst eines nur für diese Sitzung.
        get() = s("geheimnis", "").ifEmpty { neuesGeheimnis() }

    fun neuesGeheimnis(): String {
        val zeichen = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val zufall = SecureRandom()
        val neu = (1..40).map { zeichen[zufall.nextInt(zeichen.length)] }.joinToString("")
        putGeheim("geheimnis", neu)
        return neu
    }

    // ---- E-Mail (Gmail mit App-Passwort) ----
    var mailAdresse: String get() = s("mail_adresse", "barwandt@gmail.com"); set(v) = put { putString("mail_adresse", v.trim()) }
    var mailPasswort: String get() = s("mail_passwort", ""); set(v) = putGeheim("mail_passwort", v.trim())
    /** Weitere Adressen, an die Jarvis senden darf (durch Komma getrennt). Die eigene ist immer erlaubt. */
    var mailEmpfaenger: String get() = s("mail_empfaenger", ""); set(v) = put { putString("mail_empfaenger", v.trim()) }

    // ---- Internet-Recherche der Agenten ----
    var suchSchluessel: String
        get() = s("such_schluessel", "").ifEmpty { BuildConfig.TAVILY_KEY }
        set(v) = putGeheim("such_schluessel", v.trim())

    // ---- Repo auf GitHub ----
    var repoName: String get() = s("repo_name", "").ifEmpty { "Pepsi1978/proggs" }; set(v) = put { putString("repo_name", v.trim().removePrefix("https://github.com/").trim('/')) }
    /** Zugriffsschlüssel für GitHub. Lesen geht beim öffentlichen Repo auch ohne; er erlaubt die Code-Suche und mehr Abfragen. Jarvis schreibt nie. */
    var githubToken: String get() = s("github_token", ""); set(v) = putGeheim("github_token", v.trim())

    // ---- Google-Zugang für „Berechtigung erneuern“ (Tagebuch) ----
    var driveClientId: String
        get() = s("drive_client_id", "").ifEmpty { BuildConfig.DRIVE_CLIENT_ID }
        set(v) = putGeheim("drive_client_id", v.trim())
    var driveClientSecret: String
        get() = s("drive_client_secret", "").ifEmpty { BuildConfig.DRIVE_CLIENT_SECRET }
        set(v) = putGeheim("drive_client_secret", v.trim())

    // ---- Wetter ----
    /** Ort der Wettervorhersage, mit seinen Koordinaten. */
    var wetterOrt: String get() = s("wetter_ort", "Neuenhagen bei Berlin"); set(v) = put { putString("wetter_ort", v.trim()) }
    var wetterBreite: Double get() = s("wetter_breite", "52.529").toDoubleOrNull() ?: 52.529; set(v) = put { putString("wetter_breite", v.toString()) }
    var wetterLaenge: Double get() = s("wetter_laenge", "13.689").toDoubleOrNull() ?: 13.689; set(v) = put { putString("wetter_laenge", v.toString()) }

    // ---- Pünktlich losfahren (Google Routes API) ----
    var mapsSchluessel: String
        get() = s("maps_schluessel", "").ifEmpty { BuildConfig.MAPS_KEY }
        set(v) = putGeheim("maps_schluessel", v.trim())
    /** An Arbeitstagen rechtzeitig vor der nötigen Abfahrt melden. */
    var abfahrtAn: Boolean get() = prefs.getBoolean("abfahrt_an", true); set(v) = put { putBoolean("abfahrt_an", v) }
    var abfahrtVorlesen: Boolean get() = prefs.getBoolean("abfahrt_vorlesen", true); set(v) = put { putBoolean("abfahrt_vorlesen", v) }
    /** Lautstärke der vorgelesenen Losfahr-Meldung in Prozent der höchsten Medienlautstärke. */
    var abfahrtLautstaerke: Int get() = prefs.getInt("abfahrt_lautstaerke", 100).coerceIn(10, 100); set(v) = put { putInt("abfahrt_lautstaerke", v.coerceIn(10, 100)) }
    var adresseZuhause: String get() = s("adresse_zuhause", "Niederheidenstraße 42, 15366 Neuenhagen bei Berlin"); set(v) = put { putString("adresse_zuhause", v.trim()) }
    var adresseArbeit: String get() = s("adresse_arbeit", "Bodestraße 1-3, 10178 Berlin"); set(v) = put { putString("adresse_arbeit", v.trim()) }
    /** Gewünschte Ankunft auf Arbeit als HH:MM, je Dienstart. */
    var ankunftNacht: String get() = s("ankunft_nacht", "17:00"); set(v) = put { putString("ankunft_nacht", v) }
    var ankunftTag: String get() = s("ankunft_tag", "05:00"); set(v) = put { putString("ankunft_tag", v) }
    /** So viele Minuten vor der nötigen Abfahrt kommt die Meldung. */
    var abfahrtVorlauf: Int get() = prefs.getInt("abfahrt_vorlauf", 10).coerceIn(0, 60); set(v) = put { putInt("abfahrt_vorlauf", v) }
    /** Stand der laufenden Planung (JSON), siehe fahrt/Abfahrt.kt. */
    var abfahrtZustand: String get() = s("abfahrt_zustand", ""); set(v) = put { putString("abfahrt_zustand", v) }
    /** Zähler der Routen-Abfragen im laufenden Monat (JJJJ-MM), damit Jarvis im kostenlosen Kontingent bleibt. */
    var routenMonat: String get() = s("routen_monat", ""); set(v) = put { putString("routen_monat", v) }
    var routenZaehler: Int get() = prefs.getInt("routen_zaehler", 0); set(v) = put { putInt("routen_zaehler", v) }

    // ---- Tagesauswertung ----
    var auswertungAn: Boolean get() = prefs.getBoolean("auswertung_an", true); set(v) = put { putBoolean("auswertung_an", v) }
    /** Abstand der Läufe in Stunden (1 = jede volle Stunde). */
    var auswertungAbstand: Int get() = prefs.getInt("auswertung_abstand", 1).coerceIn(1, 12); set(v) = put { putInt("auswertung_abstand", v) }
    /** Ruhen, solange Frank laut Dienstplan schläft (vor Tagdienst 20 bis 4 Uhr, nach Nachtdienst 6 bis 15 Uhr). */
    var auswertungSchlafpause: Boolean get() = prefs.getBoolean("auswertung_schlafpause", true); set(v) = put { putBoolean("auswertung_schlafpause", v) }
    /** Vor jedem Lauf frische Biodaten von Whoop, Oura und Waage holen. */
    var auswertungAbgleich: Boolean get() = prefs.getBoolean("auswertung_abgleich", true); set(v) = put { putBoolean("auswertung_abgleich", v) }
    /** Nach der Auswertung aus dem Tagebuch lernen: alte Monate verdichten, neue Tatsachen über Frank notieren. */
    var auswertungLernen: Boolean get() = prefs.getBoolean("auswertung_lernen", true); set(v) = put { putBoolean("auswertung_lernen", v) }
    /** Tag (JJJJ-MM-TT), an dem Jarvis zuletzt Tatsachen aus dem Tagebuch gelernt hat. */
    var lernlaufTag: String get() = s("lernlauf_tag", ""); set(v) = put { putString("lernlauf_tag", v) }
    /** Der neueste Tagebuchtag, den der Lernlauf schon gelesen hat (ISO); der nächste Lauf beginnt einen Tag davor. */
    var lernlaufBis: String get() = s("lernlauf_bis", ""); set(v) = put { putString("lernlauf_bis", v) }
    /** Wiederholungen nach einem Modellausfall der Tagesauswertung, siehe Zeitplan.nachModellausfall. */
    var auswertungWiederholungen: Int get() = prefs.getInt("auswertung_wiederholungen", 0); set(v) = put { putInt("auswertung_wiederholungen", v) }
    /** Das Modell schreibt die Deutung. Aus = nur die Daten synchronisieren. */
    var auswertungDeutung: Boolean get() = prefs.getBoolean("auswertung_deutung", true); set(v) = put { putBoolean("auswertung_deutung", v) }

    // ---- Sperre und Vorlesen ----
    /** App nur nach Fingerabdruck (oder Gerätesperre) öffnen. Das Plugin arbeitet unabhängig davon. */
    var appSperre: Boolean get() = prefs.getBoolean("app_sperre", true); set(v) = put { putBoolean("app_sperre", v) }
    /** Antworten vorlesen, wenn die Frage gesprochen wurde. */
    var antwortenVorlesen: Boolean get() = prefs.getBoolean("antworten_vorlesen", true); set(v) = put { putBoolean("antworten_vorlesen", v) }
    /** Fertige Agenten-Ergebnisse von selbst vorlesen. */
    var ergebnisseVorlesen: Boolean get() = prefs.getBoolean("ergebnisse_vorlesen", false); set(v) = put { putBoolean("ergebnisse_vorlesen", v) }

    // ---- Hey Jarvis ----
    /** Dauerhaft auf „Hey Jarvis“ lauschen, auch bei gesperrtem Handy. */
    var weckwortAn: Boolean get() = prefs.getBoolean("weckwort_an", true); set(v) = put { putBoolean("weckwort_an", v) }
    /** niedrig | normal | hoch: wie leicht „Hey Jarvis“ anspringt. */
    var weckwortEmpfindlichkeit: String get() = s("weckwort_empfindlichkeit", "normal"); set(v) = put { putString("weckwort_empfindlichkeit", v) }
    /** Ab diesem Wert (0 bis 1) gilt „Hey Jarvis“ als erkannt. */
    val weckwortSchwelle: Float get() = when (weckwortEmpfindlichkeit) { "niedrig" -> 0.7f; "hoch" -> 0.3f; else -> 0.5f }
    /** jarvis = „Hey Jarvis“ (openWakeWord) | phrase = eigene Phrase (sherpa-onnx, englische Lautung). */
    var weckwortArt: String get() = s("weckwort_art", "jarvis"); set(v) = put { putString("weckwort_art", v) }
    /** Die eigene Weckwort-Phrase in Großbuchstaben, z. B. „OKAY COMPUTER JARVIS“. */
    var weckwortPhrase: String get() = s("weckwort_phrase", "OKAY COMPUTER JARVIS"); set(v) = put { putString("weckwort_phrase", v) }

    /** system | hell | dunkel */
    var modus: String get() = s("modus", "system"); set(v) = put { putString("modus", v) }
    /** Farbdesign: blau (Jarvis) oder gold (wie Geniale Ideen); hell oder dunkel regelt [modus]. */
    var design: String get() = s("design", "blau"); set(v) = put { putString("design", v) }

    companion object {
        const val NAME = "jarvis_secure_prefs"
        private const val TAG = "JarvisEinstellungen"
        /** Schlüssel, die nie unverschlüsselt abgelegt werden dürfen. */
        private val GEHEIM = listOf(
            "geheimnis", "server_token", "mail_passwort", "github_token", "such_schluessel", "drive_client_id", "drive_client_secret",
            "maps_schluessel", "maps_schluessel_bau",
        )
        /** Der eigene Jarvis-Relay (öffentlich bekannte Adresse, siehe README). Der Schlüssel bleibt geheim. */
        const val STANDARD_SERVER = "srv1774016.hstgr.cloud"
        @Volatile private var instanz: Einstellungen? = null
        fun get(context: Context): Einstellungen = instanz ?: synchronized(this) {
            instanz ?: Einstellungen(context).also { instanz = it }
        }
    }
}
