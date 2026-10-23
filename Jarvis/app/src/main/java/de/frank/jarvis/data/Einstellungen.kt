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
import de.frank.jarvis.auth.KiAnbieter
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

    /** Neue Zugangsdaten, abgelegt vom adb-Skript per run-as (siehe [zugangPruefen]). */
    private val zugangDatei = java.io.File(context.applicationContext.filesDir, "zugang.json")
    /** Gestört: Stand (Änderungszeit) der zuletzt übernommenen zugang.json, damit jedes Wiederverbinden sie nicht neu einliest. */
    private var zugangStand = 0L

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
        // Freibetrag der früheren Guthaben-Abfrage (bis 1.8.221); den gewährten Betrag nennt jetzt Anthropic selbst.
        if (roh.contains("anthropic_freibetrag")) roh.edit().remove("anthropic_freibetrag").apply()
        // Gestört: Zugangsdaten, die eine ältere Fassung im Klartext-Ersatz abgelegt hat, für diese Sitzung übernehmen und
        // dort entfernen. Das Plugin-Geheimnis bleibt liegen und gilt weiter, damit die eingetragene Adresse nicht wechselt.
        if (gestoert) {
            val alt = GEHEIM.mapNotNull { k -> roh.getString(k, null)?.takeIf { it.isNotEmpty() }?.let { k to it } }
            alt.forEach { (k, v) -> sitzung[k] = v }
            val weg = alt.map { it.first }.filter { it != "geheimnis" }
            if (weg.isNotEmpty()) roh.edit().apply { weg.forEach { remove(it) } }.apply()
        }
        // Adressen, die ein Bau am PC aus ~/SK mitbringt, dauerhaft übernehmen. Ein späteres Update aus dem Cloud-Bau
        // (ohne ~/SK) verliert sie dann nicht mehr. Schlüssel und Tokens kommen bewusst nicht mehr aus dem Bau: In der APK
        // stünden sie im Klartext, und wer die APK hat, hätte den Server-Zugang (siehe zugang.json unten).
        val eingebaut = mapOf(
            "server_host" to BuildConfig.RELAY_HOST, "drive_client_id" to BuildConfig.DRIVE_CLIENT_ID, "routine_url" to BuildConfig.ROUTINE_URL,
        ).filter { (k, v) -> v.isNotBlank() && roh.getString(k, "").isNullOrEmpty() }
        // Gestört: nichts davon in den Klartext-Ersatz schreiben; die Getter greifen ohnehin auf die Werte aus dem Bau zurück.
        if (eingebaut.isNotEmpty() && !gestoert) roh.edit().apply { eingebaut.forEach { (k, v) -> putString(k, v) } }.apply()
        zugangPruefen()
    }

    /**
     * Neue Zugangsdaten kommen über `files/zugang.json`, die ein adb-Skript per run-as ablegt
     * ({"server_token": …, "routine_token": …, "such_schluessel": …, "maps_schluessel": …, "drive_client_secret": …, "server_host"?: …}).
     * Jeder nicht leere Wert ersetzt den gespeicherten. Geprüft wird beim Start und vor jedem Verbindungsversuch des Tunnels
     * (nur ein exists, solange keine Datei da ist): Der Vordergrunddienst hält den Prozess, das Skript startet nichts neu.
     * - Ungestört: verschlüsselt und synchron gespeichert; erst danach wird die Datei gelöscht.
     * - Gestört: Die Werte gelten nur für diese Sitzung. Die Datei bleibt deshalb liegen (sie ist app-privat) und wird bei
     *   jedem Start erneut gelesen; im selben Prozess nur, wenn sie sich geändert hat.
     * Eine unlesbare Datei wird gelöscht (sie würde nie lesbar). Protokolliert wird nie ein Wert.
     * Rückgabe: true, wenn etwas übernommen wurde (der Tunnel verbindet dann mit den neuen Daten).
     */
    @Synchronized
    fun zugangPruefen(): Boolean {
        val datei = zugangDatei
        if (!datei.exists()) return false
        val stand = datei.lastModified()
        if (gestoert && stand == zugangStand) return false
        val j = try {
            org.json.JSONObject(datei.readText())
        } catch (e: Exception) {
            // Nur die Art des Fehlers: Die Meldung einer JSONException enthielte Teile der Datei, also Schlüssel.
            Log.w(TAG, "zugang.json unlesbar (${e.javaClass.simpleName})")
            Logbuch.fehler("Einstellungen: zugang.json unlesbar", null, e.javaClass.simpleName)
            if (!datei.delete()) Log.w(TAG, "zugang.json ließ sich nicht löschen")
            return false
        }
        val werte = ZUGANG.mapNotNull { k -> j.optString(k).trim().takeIf { it.isNotEmpty() }?.let { k to it } }
            .map { (k, v) -> k to if (k == "server_host") v.removePrefix("https://").removePrefix("wss://").trimEnd('/') else v }
        // Ein anderer Maps-Schlüssel: Die Planung der Losfahr-Meldung beginnt damit von vorn (wie früher beim Bau-Schlüssel).
        val mapsNeu = werte.any { (k, v) -> k == "maps_schluessel" && v != s(k, "") }
        val gespeichert = if (gestoert) {
            werte.forEach { (k, v) -> if (k in GEHEIM) sitzung[k] = v }
            // Die Adresse ist kein Geheimnis: Sie darf in den Ersatzspeicher.
            roh.edit().apply { werte.filter { it.first !in GEHEIM }.forEach { (k, v) -> putString(k, v) }; if (mapsNeu) remove("abfahrt_zustand") }.apply()
            zugangStand = stand
            true
        } else {
            // commit statt apply: Die Datei wird gleich gelöscht, die Werte müssen vorher sicher auf der Platte stehen.
            roh.edit().apply { werte.forEach { (k, v) -> putString(k, v) }; if (mapsNeu) remove("abfahrt_zustand") }.commit()
        }
        version.intValue++
        val namen = werte.joinToString { it.first }
        if (!gespeichert) {
            Log.w(TAG, "zugang.json nicht gespeichert; sie bleibt für den nächsten Versuch liegen")
            Logbuch.fehler("Einstellungen: zugang.json nicht gespeichert", null, namen)
            return false
        }
        if (!gestoert && !datei.delete()) Log.w(TAG, "zugang.json ließ sich nicht löschen")
        Log.i(TAG, "Zugangsdaten aus zugang.json übernommen: $namen")
        runCatching { Protokoll.melde(Quelle.SYSTEM, "Einstellungen", "Zugangsdaten übernommen: $namen" + if (gestoert) " (nur bis zum Neustart; die Datei bleibt liegen)" else "") }
        return werte.isNotEmpty()
    }

    // ---- Das eigene Modell von Jarvis ----
    var modell: CodexModel
        get() = CodexModel.entries.firstOrNull { it.name == s("model", "") } ?: CodexModel.TERRA
        set(v) {
            // Beim Anbieterwechsel das bisherige Modell festhalten (vor 1.8.212 stand es nur unter „model“).
            val alt = modell
            // Denkstufe je Modell merken: beim Wechsel die zuletzt für das neue Modell gewählte Stufe wieder einstellen.
            val altStufe = denkstufe
            val neueStufe = if (alt == v) null
                else v.normalizeEffort(ReasoningEffort.entries.firstOrNull { it.name == s("reasoning_${v.name}", "") } ?: altStufe)
            put {
                if (alt.anbieter != v.anbieter) putString(if (alt.istAnthropic) "model_anthropic" else "model_openai", alt.name)
                putString("model", v.name); putString(if (v.istAnthropic) "model_anthropic" else "model_openai", v.name)
                if (neueStufe != null) {
                    putString("reasoning_${alt.name}", altStufe.name)
                    putString("reasoning", neueStufe.name)
                }
            }
        }
    /** Zuletzt gewähltes OpenAI-Modell: Bildwerkzeug und Websuche laufen immer über das ChatGPT-Konto, auch wenn Jarvis mit Claude denkt. */
    val openAiModell: CodexModel
        get() = letztesModell(KiAnbieter.OPENAI)
    /** Zuletzt gewähltes Modell je Anbieter, für den Wechsel zwischen OpenAI und Anthropic. */
    fun letztesModell(anbieter: KiAnbieter): CodexModel =
        if (modell.anbieter == anbieter) modell
        else CodexModel.von(anbieter).firstOrNull { it.name == s(if (anbieter == KiAnbieter.ANTHROPIC) "model_anthropic" else "model_openai", "") }
            ?: if (anbieter == KiAnbieter.ANTHROPIC) CodexModel.OPUS else CodexModel.TERRA
    var denkstufe: ReasoningEffort
        get() = ReasoningEffort.entries.firstOrNull { it.name == s("reasoning", "") } ?: ReasoningEffort.LOW
        set(v) = put { putString("reasoning", v.name); putString("reasoning_${modell.name}", v.name) }
    /** Höchstens so viele Bearbeitungsrunden (Modellschritt samt Werkzeugaufrufen) je Anfrage im Gespräch; 0 = unbegrenzt. */
    var schrittlimit: Int
        get() = prefs.getInt("schrittlimit", STANDARD_SCHRITTLIMIT).takeIf { it in SCHRITTLIMITS } ?: STANDARD_SCHRITTLIMIT
        set(v) {
            // Ein ungültiger Wert ist ein Fehler des Aufrufers (Oberfläche, Sprachbefehl): laut ablehnen statt still auf 10 zu setzen.
            require(v in SCHRITTLIMITS) { "Ungültiges Schrittlimit $v; erlaubt: ${SCHRITTLIMITS.joinToString { schrittlimitLabel(it) }}" }
            put { putInt("schrittlimit", v) }
        }
    /** So viele Ausgabe-Token (Denken samt Text) darf eine Aufgabe im Chat oder ein Agent verbrauchen, bevor Jarvis fragt, ob er weitermachen soll. */
    var tokenlimit: Int
        get() = prefs.getInt("tokenlimit", STANDARD_TOKENLIMIT).takeIf { it in TOKENLIMITS } ?: STANDARD_TOKENLIMIT
        set(v) {
            require(v in TOKENLIMITS) { "Ungültiges Tokenlimit $v; erlaubt: ${TOKENLIMITS.joinToString { tokenlimitLabel(it) }}" }
            put { putInt("tokenlimit", v) }
        }

    // ---- Plugin und Server ----
    /** Adresse des eigenen Servers (Jarvis-Relay). Vorgabe aus dem Bau oder zugang.json, hier nur zum Überschreiben. */
    var serverHost: String
        get() = s("server_host", "").ifEmpty { BuildConfig.RELAY_HOST }.ifEmpty { STANDARD_SERVER }
        set(v) = put { putString("server_host", v.trim().removePrefix("https://").removePrefix("wss://").trimEnd('/')) }
    /** Schlüssel, mit dem sich dieses Handy am Server ausweist (Rolle „Handy“). Kommt über zugang.json oder die Einstellungen, nie aus dem Bau. */
    var serverToken: String
        get() = s("server_token", "")
        set(v) = putGeheim("server_token", v.trim())
    /** Der Dienst läuft, sobald die App einmal geöffnet wurde; hier lässt er sich abschalten. */
    var dienstAn: Boolean get() = prefs.getBoolean("dienst_an", true); set(v) = put { putBoolean("dienst_an", v) }

    /**
     * Geheimer Teil der Plugin-Adresse. Wer ihn kennt, darf die Werkzeuge aufrufen — deshalb lang, zufällig
     * und nur auf Wunsch erneuerbar (dann muss die Adresse in ChatGPT neu eingetragen werden).
     */
    val geheimnis: String
        // Gestört: das schon gespeicherte Geheimnis aus dem Ersatzspeicher (siehe init), sonst eines nur für diese Sitzung.
        // Gesperrt und unter der Sperre erneut gelesen: Lesen Dienst und Oberfläche beim allerersten Start gleichzeitig,
        // entstünden sonst zwei Geheimnisse, und der Tunnel meldete ein anderes, als die App als Plugin-Adresse zeigt.
        get() = s("geheimnis", "").ifEmpty { synchronized(this) { s("geheimnis", "").ifEmpty { neuesGeheimnis() } } }

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
        get() = s("such_schluessel", "")
        set(v) = putGeheim("such_schluessel", v.trim())

    // ---- Repo auf GitHub ----
    var repoName: String get() = s("repo_name", "").ifEmpty { "Pepsi1978/proggs" }; set(v) = put { putString("repo_name", v.trim().removePrefix("https://github.com/").trim('/')) }
    /** Zugriffsschlüssel für GitHub. Lesen geht beim öffentlichen Repo auch ohne; er erlaubt die Code-Suche und mehr Abfragen. Jarvis schreibt nie. */
    var githubToken: String get() = s("github_token", ""); set(v) = putGeheim("github_token", v.trim())

    // ---- Programmieren in der Cloud (Claude-Code-Routine mit API-Trigger) ----
    /** Auslöse-Adresse der Routine (…/routines/trig_…/fire). Vorgabe aus dem Bau (~/SK/Jarvis/routine.properties). */
    var routineUrl: String get() = s("routine_url", "").ifEmpty { BuildConfig.ROUTINE_URL }.ifEmpty { STANDARD_ROUTINE }; set(v) = put { putString("routine_url", v.trim()) }
    /** Token des API-Triggers dieser Routine; claude.ai zeigt ihn nur einmal an. Kommt über zugang.json oder die Einstellungen, nie aus dem Bau. */
    var routineToken: String get() = s("routine_token", ""); set(v) = putGeheim("routine_token", v.trim())

    // ---- Google-Zugang für „Berechtigung erneuern“ (Tagebuch) ----
    var driveClientId: String
        get() = s("drive_client_id", "").ifEmpty { BuildConfig.DRIVE_CLIENT_ID }
        set(v) = putGeheim("drive_client_id", v.trim())
    var driveClientSecret: String
        get() = s("drive_client_secret", "")
        set(v) = putGeheim("drive_client_secret", v.trim())

    // ---- Wetter ----
    /** Ort der Wettervorhersage, mit seinen Koordinaten. */
    var wetterOrt: String get() = s("wetter_ort", "Neuenhagen bei Berlin"); set(v) = put { putString("wetter_ort", v.trim()) }
    var wetterBreite: Double get() = s("wetter_breite", "52.529").toDoubleOrNull() ?: 52.529; set(v) = put { putString("wetter_breite", v.toString()) }
    var wetterLaenge: Double get() = s("wetter_laenge", "13.689").toDoubleOrNull() ?: 13.689; set(v) = put { putString("wetter_laenge", v.toString()) }

    // ---- Pünktlich losfahren (Google Routes API) ----
    var mapsSchluessel: String
        get() = s("maps_schluessel", "")
        set(v) = putGeheim("maps_schluessel", v.trim())
    /** An Arbeitstagen rechtzeitig vor der nötigen Abfahrt melden. */
    var abfahrtAn: Boolean get() = prefs.getBoolean("abfahrt_an", true); set(v) = put { putBoolean("abfahrt_an", v) }
    var abfahrtVorlesen: Boolean get() = prefs.getBoolean("abfahrt_vorlesen", true); set(v) = put { putBoolean("abfahrt_vorlesen", v) }
    /** Lautstärke der vorgelesenen Losfahr-Meldung in Prozent der höchsten Medienlautstärke. */
    var abfahrtLautstaerke: Int get() = prefs.getInt("abfahrt_lautstaerke", 100).coerceIn(0, 100); set(v) = put { putInt("abfahrt_lautstaerke", v.coerceIn(0, 100)) }

    // ---- Ansagen anderer eigener Apps (UpdateStation: „Ein Update von … ist bereit“) ----
    var ansageVorlesen: Boolean get() = prefs.getBoolean("ansage_vorlesen", true); set(v) = put { putBoolean("ansage_vorlesen", v) }
    /** Stimm-Kennung aus dem TtsCatalog; leer = dieselbe Stimme wie Jarvis. */
    var ansageStimme: String get() = s("ansage_stimme", ""); set(v) = put { putString("ansage_stimme", v) }
    /** Sprechtempo der Ansage; 0 = wie Jarvis. */
    var ansageTempo: Float get() = prefs.getFloat("ansage_tempo", 0f); set(v) = put { putFloat("ansage_tempo", v) }
    /** Lautstärke der Ansage in Prozent der höchsten Medienlautstärke. */
    var ansageLautstaerke: Int get() = prefs.getInt("ansage_lautstaerke", 100).coerceIn(0, 100); set(v) = put { putInt("ansage_lautstaerke", v.coerceIn(0, 100)) }
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
    /** Die erste Antwort vorlesen, wenn die Frage mit dem Diktier-Mikrofon eingesprochen und dann von Hand gesendet wurde. */
    var diktatAntwortVorlesen: Boolean get() = prefs.getBoolean("diktat_antwort_vorlesen", true); set(v) = put { putBoolean("diktat_antwort_vorlesen", v) }
    /** Fertige Agenten-Ergebnisse von selbst vorlesen, als kurze Zusammenfassung (Vorlesefassung), nicht den ganzen Bericht. */
    var ergebnisseVorlesen: Boolean get() = prefs.getBoolean("ergebnisse_vorlesen", false); set(v) = put { putBoolean("ergebnisse_vorlesen", v) }

    // ---- Hey Jarvis ----
    /** Dauerhaft auf „Hey Jarvis“ lauschen, auch bei gesperrtem Handy. */
    var weckwortAn: Boolean get() = prefs.getBoolean("weckwort_an", true); set(v) = put { putBoolean("weckwort_an", v) }
    /** niedrig | normal | hoch | sehr_hoch: wie leicht „Hey Jarvis“ anspringt und wie großzügig die Stimmerkennung urteilt. */
    var weckwortEmpfindlichkeit: String get() = s("weckwort_empfindlichkeit", "normal"); set(v) = put { putString("weckwort_empfindlichkeit", v) }
    /** Ab diesem Wert (0 bis 1) gilt „Hey Jarvis“ als erkannt. */
    val weckwortSchwelle: Float get() = when (weckwortEmpfindlichkeit) { "niedrig" -> 0.7f; "hoch" -> 0.3f; "sehr_hoch" -> 0.2f; else -> 0.5f }
    /** So viel tiefer liegt die Schwelle der Stimmerkennung je Empfindlichkeit (Stimmprofil.schwellenAbzug). */
    val weckwortStimmAbzug: Float get() = when (weckwortEmpfindlichkeit) { "niedrig" -> 0f; "hoch" -> 0.07f; "sehr_hoch" -> 0.10f; else -> 0.03f }
    /** jarvis = „Hey Jarvis“ (openWakeWord) | phrase = eigene Phrase (sherpa-onnx, englische Lautung). */
    var weckwortArt: String get() = s("weckwort_art", "jarvis"); set(v) = put { putString("weckwort_art", v) }
    /** Die eigene Weckwort-Phrase in Großbuchstaben, z. B. „OKAY COMPUTER JARVIS“. */
    var weckwortPhrase: String get() = s("weckwort_phrase", "OKAY COMPUTER JARVIS"); set(v) = put { putString("weckwort_phrase", v) }

    /** system | hell | dunkel */
    var modus: String get() = s("modus", "system"); set(v) = put { putString("modus", v) }
    // ---- Timer ----
    /** true = ein abgelaufener Timer klingelt wie ein Wecker, bis Frank ihn ausschaltet; false = kurze Meldung mit Ton. */
    var timerWecker: Boolean get() = prefs.getBoolean("timer_wecker", true); set(v) = put { putBoolean("timer_wecker", v) }
    var timerVibration: Boolean get() = prefs.getBoolean("timer_vibration", true); set(v) = put { putBoolean("timer_vibration", v) }
    /** Lautstärke des Wecktons in Prozent der Wecker-Lautstärke des Handys. */
    var timerLautstaerke: Int get() = prefs.getInt("timer_lautstaerke", 100).coerceIn(10, 100); set(v) = put { putInt("timer_lautstaerke", v.coerceIn(10, 100)) }
    /** Höchstens so viele Minuten klingeln, danach bleibt eine stille Meldung; 0 = bis zum Ausschalten. */
    var timerKlingelMax: Int get() = prefs.getInt("timer_klingel_max", 0).coerceIn(0, 60); set(v) = put { putInt("timer_klingel_max", v.coerceIn(0, 60)) }
    /** Minuten für „Schlummern“ in der Meldung. */
    var timerSchlummern: Int get() = prefs.getInt("timer_schlummern", 5).coerceIn(1, 30); set(v) = put { putInt("timer_schlummern", v.coerceIn(1, 30)) }

    // ---- Benachrichtigungen (je Meldungsart über meldung/Meldungen.kt) ----
    fun wahr(key: String, def: Boolean) = prefs.getBoolean(key, def)
    fun setzeWahr(key: String, v: Boolean) = put { putBoolean(key, v) }
    fun ganz(key: String, def: Int) = prefs.getInt(key, def)
    fun setzeGanz(key: String, v: Int) = put { putInt(key, v) }
    fun komma(key: String, def: Float) = prefs.getFloat(key, def)
    fun setzeKomma(key: String, v: Float) = put { putFloat(key, v) }
    fun wort(key: String, def: String) = s(key, def)
    fun setzeWort(key: String, v: String) = put { putString(key, v) }
    /** Ruhezeit: Ergebnis-Meldungen und Update-Ansagen kommen dann lautlos, ohne Vorlesen und Wecker. */
    var meldungRuheAn: Boolean get() = prefs.getBoolean("meldung_ruhe_an", false); set(v) = put { putBoolean("meldung_ruhe_an", v) }
    var meldungRuheVon: String get() = s("meldung_ruhe_von", "21:00"); set(v) = put { putString("meldung_ruhe_von", v) }
    var meldungRuheBis: String get() = s("meldung_ruhe_bis", "06:00"); set(v) = put { putString("meldung_ruhe_bis", v) }
    /** Auf dem Sperrbildschirm nur „Jarvis“ zeigen, nicht den Inhalt. */
    var meldungInhaltVerbergen: Boolean get() = prefs.getBoolean("meldung_inhalt_verbergen", false); set(v) = put { putBoolean("meldung_inhalt_verbergen", v) }

    /** Farbdesign: blau (Jarvis) oder gold (wie Geniale Ideen); hell oder dunkel regelt [modus]. */
    var design: String get() = s("design", "blau"); set(v) = put { putString("design", v) }
    /** Ansicht der Ablage wie im Windows-Explorer: gross, mittel, klein (Kacheln), details oder liste. */
    var ablageAnsicht: String get() = s("ablage_ansicht", "mittel"); set(v) = put { putString("ablage_ansicht", v) }

    companion object {
        const val NAME = "jarvis_secure_prefs"
        /** Wählbare Schrittlimits pro Anfrage; 0 = unbegrenzt. */
        val SCHRITTLIMITS = (10..100 step 10).toList() + 0
        const val STANDARD_SCHRITTLIMIT = 10
        /** So heißt ein Schrittlimit in Oberfläche und Sprachantworten. */
        fun schrittlimitLabel(n: Int): String = if (n == 0) "Unbegrenzt" else "$n Schritte"
        val TOKENLIMITS = (50_000..500_000 step 50_000).toList()
        const val STANDARD_TOKENLIMIT = 150_000
        /** So heißt ein Tokenlimit in Oberfläche und Sprachantworten. */
        fun tokenlimitLabel(n: Int): String = "${n / 1000} 000 Token"
        private const val TAG = "JarvisEinstellungen"
        /** Die Schlüssel, die zugang.json setzen darf (siehe zugangPruefen). */
        private val ZUGANG = listOf("server_token", "routine_token", "such_schluessel", "maps_schluessel", "drive_client_secret", "server_host")
        /** Schlüssel, die nie unverschlüsselt abgelegt werden dürfen. */
        private val GEHEIM = listOf(
            "geheimnis", "server_token", "mail_passwort", "github_token", "such_schluessel", "drive_client_id", "drive_client_secret",
            "maps_schluessel", "maps_schluessel_bau", "routine_token",
        )
        /** Der eigene Jarvis-Relay (öffentlich bekannte Adresse, siehe README). Der Schlüssel bleibt geheim. */
        const val STANDARD_SERVER = "srv1774016.hstgr.cloud"
        /** Auslöse-Adresse der Routine „Jarvis: Programmieren“ (claude.ai/code/routines). Kein Geheimnis; ohne Token wirkungslos. */
        const val STANDARD_ROUTINE = "https://api.anthropic.com/v1/claude_code/routines/trig_01HkrhPAiYAJ7msGLn5UUcyo/fire"
        @Volatile private var instanz: Einstellungen? = null
        /** Die Instanz, falls schon angelegt (für Stellen ohne Context wie den Tunnel). */
        fun vorhanden(): Einstellungen? = instanz
        fun get(context: Context): Einstellungen = instanz ?: synchronized(this) {
            instanz ?: Einstellungen(context).also { instanz = it }
        }
    }
}
