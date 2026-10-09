package de.frank.jarvis.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.CodexModel
import de.frank.jarvis.auth.DeviceAuthInfo
import de.frank.jarvis.auth.ReasoningEffort
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.faehigkeit.Register
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Reiter(val anzeige: String) { JARVIS("Jarvis"), ABLAGE("Ablage"), AKTIVITAET("Aktivität"), EINSTELLUNGEN("Einstellungen") }

data class Nachricht(val vonMir: Boolean, val text: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val einstellungen = Einstellungen.get(app)
    private val agent = JarvisAgent(app)
    private val auth get() = agent.auth

    var reiter by mutableStateOf(Reiter.JARVIS)

    // ---- Sperre ----
    /** true, solange die App auf den Fingerabdruck wartet. Der Dienst im Hintergrund läuft davon unberührt. */
    var gesperrt by mutableStateOf(false)
    /** Wann die App zuletzt sichtbar war; 0 = seit dem Start noch nie entsperrt. */
    var zuletztSichtbar = 0L
    /** Bis wann die Sperre nach einem bewusst gestarteten Teilen, Öffnen oder „Speichern unter“ wartet (sonst ginge das Ergebnis verloren). */
    var sperrAufschubBis = 0L
    var entsperrenAnfragen: () -> Unit = {}

    // ---- Sprache ----
    val sprache = de.frank.jarvis.data.settings.SecureSettings(app)
    val vorleser: de.frank.jarvis.speech.Vorleser = de.frank.jarvis.speech.Vorleser.hole(app, sprache)
    private val mikro = de.frank.jarvis.audio.MicRecorder(app)
    val pegel get() = mikro.pegel
    var nimmtAuf by mutableStateOf(false); private set
    var schreibtMit by mutableStateOf(false); private set
    var mikrofonAnfragen: () -> Unit = {}
    /** Die laufende Frage kam über das Mikrofon: Dann wird die Antwort vorgelesen. */
    private var gesprochen = false

    // ---- Gespräch mit Jarvis ----
    val gespraech = mutableStateListOf<Nachricht>()
    var denkt by mutableStateOf(false); private set
    /** Was Jarvis gerade tut („Aufgaben lesen“), solange er arbeitet. */
    var schritt by mutableStateOf(""); private set
    private var lauf: Job? = null

    // ---- ChatGPT-Anmeldung ----
    var kiVerbunden by mutableStateOf(auth.isConnected); private set
    var kiVerbindet by mutableStateOf(false); private set
    var kiKonto by mutableStateOf(auth.email.orEmpty()); private set
    var geraeteCode by mutableStateOf<DeviceAuthInfo?>(null); private set

    // ---- Lage ----
    /** null = Geniale Aufgaben ist angebunden, sonst der Grund. */
    var aufgabenStoerung by mutableStateOf<String?>(null); private set
    var aufgabenGeprueft by mutableStateOf(false); private set
    /** Je angebundener App: null = bereit, sonst der Grund. Fehlt der Eintrag, läuft die Prüfung noch. */
    var stoerungen by mutableStateOf<Map<String, String?>>(emptyMap()); private set
    var akkuFrei by mutableStateOf(false); private set
    var hinweiseAn by mutableStateOf(true); private set

    var meldung by mutableStateOf<String?>(null)
    var hinweiseAnfragen: () -> Unit = {}
    var kalenderAnfragen: () -> Unit = {}

    init { lagePruefen() }

    fun lagePruefen() {
        val app = getApplication<Application>()
        kiVerbunden = auth.isConnected
        kiKonto = auth.email.orEmpty()
        akkuFrei = app.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(app.packageName)
        hinweiseAn = NotificationManagerCompat.from(app).areNotificationsEnabled()
        viewModelScope.launch {
            val lage = withContext(Dispatchers.IO) { Register.alle(app).associate { it.id to it.stoerung() } }
            stoerungen = lage
            aufgabenStoerung = lage["aufgaben"]
            aufgabenGeprueft = true
        }
    }

    // ---- Gespräch ----

    fun sende(eingabe: String) {
        val text = eingabe.trim()
        if (text.isEmpty() || denkt) return
        if (!kiVerbunden) {
            meldung = "Bitte zuerst mit ChatGPT verbinden."
            reiter = Reiter.EINSTELLUNGEN
            return
        }
        val verlauf = gespraech.map { ChatTurn(if (it.vonMir) "user" else "assistant", it.text) }
        gespraech += Nachricht(true, text)
        denkt = true
        schritt = ""
        lauf = viewModelScope.launch {
            val antwort = try {
                agent.frage(text, verlauf) { schritt = it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "Das hat nicht geklappt."
            }
            gespraech += Nachricht(false, antwort)
            denkt = false
            schritt = ""
            if (gesprochen && einstellungen.antwortenVorlesen) vorleser.sprich("gespraech:" + (gespraech.size - 1), "Jarvis", antwort)
            gesprochen = false
        }
    }

    // ---- Mikrofon und Vorlesen ----

    // ---- Franks Regeln und Wissen über Frank ----

    /** Eine der beiden Merkdateien samt dem, was ihr Bildschirm braucht. */
    inner class Merkliste(
        private val datei: de.frank.jarvis.faehigkeit.Merkdatei,
        val titel: String,
        /** „Regel“ oder „Notiz“, für Meldungen und Beschriftungen. */
        val wort: String,
        val erklaerung: String,
        val platzhalter: String,
        private val korrekturAuftrag: String,
    ) {
        var eintraege by mutableStateOf(emptyList<de.frank.jarvis.faehigkeit.Regel>()); private set
        /** Der in diesem Bildschirm zuletzt hinzugefügte Eintrag: nur er bekommt die KI-Korrektur. */
        var letzter by mutableStateOf<Int?>(null); private set
        var korrigiert by mutableStateOf(false); private set
        /** Stände vor den letzten Änderungen, für „Rückgängig“. */
        private val verlauf = mutableStateListOf<List<de.frank.jarvis.faehigkeit.Regel>>()
        val rueckgaengigMoeglich: Boolean get() = verlauf.isNotEmpty()

        fun laden() { eintraege = datei.alle(getApplication()).sortedByDescending { it.id } }

        private fun aendere(aenderung: () -> Unit) {
            verlauf += datei.alle(getApplication())
            if (verlauf.size > 20) verlauf.removeAt(0)
            aenderung()
            laden()
        }

        fun neu(text: String) {
            if (text.isBlank()) return
            aendere {
                val eintrag = datei.speichere(getApplication(), text)
                if (eintrag == null) meldung = "Die Datei ist voll. Lösche zuerst einen überholten Eintrag." else letzter = eintrag.id
            }
        }

        fun aendern(id: Int, text: String) {
            if (text.isBlank()) { meldung = "Ein leerer Eintrag geht nicht. Zum Entfernen bitte löschen."; return }
            aendere { datei.speichere(getApplication(), text, id) }
        }

        fun loeschen(id: Int) {
            aendere { datei.loesche(getApplication(), id) }
            if (letzter == id) letzter = null
            meldung = "$wort gelöscht. Mit „Rückgängig“ holst du sie zurück."
        }

        fun rueckgaengig() {
            val vorher = verlauf.removeLastOrNull() ?: return
            datei.setze(getApplication(), vorher)
            laden()
            if (eintraege.none { it.id == letzter }) letzter = null
            meldung = "Letzte Änderung zurückgenommen."
        }

        /** Formuliert den zuletzt hinzugefügten Eintrag um, und nur ihn. */
        fun korrektur() {
            val eintrag = eintraege.firstOrNull { it.id == letzter } ?: return
            if (korrigiert) return
            if (!kiVerbunden) { meldung = "Für die KI-Korrektur bitte zuerst mit ChatGPT verbinden."; return }
            korrigiert = true
            viewModelScope.launch {
                try {
                    val neu = auth.streamChat(korrekturAuftrag, listOf(ChatTurn("user", eintrag.text)), einstellungen.modell, einstellungen.denkstufe).trim().trim('"', '„', '“')
                    when {
                        neu.length < 10 -> meldung = "Die KI-Korrektur hat nichts Brauchbares geliefert. Der Text bleibt, wie er ist."
                        neu == eintrag.text -> meldung = "Das ist schon klar formuliert."
                        else -> { aendere { datei.speichere(getApplication(), neu, eintrag.id) }; meldung = "$wort neu formuliert." }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    meldung = e.message ?: "Die KI-Korrektur hat nicht geklappt."
                } finally {
                    korrigiert = false
                }
            }
        }
    }

    val regelListe = Merkliste(
        de.frank.jarvis.faehigkeit.Regeln, "Franks Regeln", "Regel",
        "Diese Regeln beachtet Jarvis bei jeder Antwort, bei seinen Agenten und in der Tagesauswertung. Er lernt sie auch selbst, wenn du ihm sagst, wie du etwas künftig haben möchtest.",
        "Neue Regel eintippen oder einsprechen", REGEL_KORREKTUR,
    )
    val infoListe = Merkliste(
        de.frank.jarvis.faehigkeit.UeberFrank, "Über Frank", "Notiz",
        "Das weiß Jarvis über dich und hat es bei jeder Antwort, bei seinen Agenten und in der Tagesauswertung vor Augen. Er notiert es auch selbst, wenn du etwas über dich erzählst.",
        "Neue Notiz über dich eintippen oder einsprechen", INFO_KORREKTUR,
    )
    /** Die Merkliste, die gerade im Vollbild offen ist. */
    var merkOffen by mutableStateOf<Merkliste?>(null); private set
    /** Wohin der Text der laufenden Aufnahme geht, wenn nicht ins Gespräch. */
    private var diktatZiel: ((String) -> Unit)? = null

    fun merkLaden() { regelListe.laden(); infoListe.laden() }
    fun merkOeffnen(liste: Merkliste) { liste.laden(); merkOffen = liste }
    fun merkSchliessen() { merkOffen = null }

    /** Mikrofon im Vollbild einer Merkliste: Die Aufnahme wird als neuer Eintrag gespeichert, nicht an Jarvis geschickt. */
    fun merkMikrofonTippen(liste: Merkliste) {
        when {
            schreibtMit -> Unit
            nimmtAuf -> aufnahmeBeenden()
            sprache.groqApiKey.isBlank() -> meldung = "Für das Mikrofon bitte zuerst den Groq-Schlüssel in den Einstellungen eintragen."
            else -> { diktatZiel = liste::neu; mikrofonAnfragen() }
        }
    }

    /** Tipp auf den Mikrofon-Knopf: Aufnahme starten oder beenden und abschicken. */
    fun mikrofonTippen() {
        when {
            schreibtMit || denkt -> Unit
            nimmtAuf -> aufnahmeBeenden()
            sprache.groqApiKey.isBlank() -> { meldung = "Für das Mikrofon bitte zuerst den Groq-Schlüssel eintragen."; reiter = Reiter.EINSTELLUNGEN }
            else -> mikrofonAnfragen()
        }
    }

    fun mikrofonErlaubt(erlaubt: Boolean) {
        if (!erlaubt) { diktatZiel = null; meldung = "Ohne Mikrofon-Erlaubnis kann ich dich nicht hören."; return }
        vorleser.stopp()
        if (mikro.start(viewModelScope)) nimmtAuf = true else { diktatZiel = null; meldung = "Die Aufnahme ließ sich nicht starten." }
    }

    private fun aufnahmeBeenden() {
        nimmtAuf = false
        schreibtMit = true
        val ziel = diktatZiel
        diktatZiel = null
        viewModelScope.launch {
            try {
                val wav = mikro.stop()
                if (wav == null || wav.size < 2000) { meldung = "Die Aufnahme war zu kurz."; return@launch }
                val groq = de.frank.jarvis.audio.GroqTranscriber(
                    apiKey = sprache.groqApiKey,
                    filterStille = sprache.filterStilleVorabAn, filterMetriken = sprache.filterSegmentmetrikenAn,
                    filterZeitstempel = sprache.filterZeitstempelAn, filterFloskeln = sprache.filterFloskelnAn,
                )
                val text = try { withContext(Dispatchers.IO) { de.frank.jarvis.audio.Diktat(groq).transkribiere(wav).text.trim() } } finally { groq.shutdown() }
                if (text.isBlank()) meldung = "Ich habe nichts verstanden."
                else if (ziel != null) ziel(text)
                else { schreibtMit = false; gesprochen = true; sende(text) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                meldung = e.message ?: "Die Aufnahme konnte nicht in Text umgewandelt werden."
            } finally {
                schreibtMit = false
            }
        }
    }

    /** Lautsprecher-Knopf: vorlesen, oder anhalten, wenn genau das schon läuft. */
    fun lies(quelle: String, titel: String, text: String) {
        val stand = vorleser.stand.value
        if (stand.quelle == quelle && stand.zustand != de.frank.jarvis.speech.VorleseZustand.AUS) vorleser.stopp() else vorleser.sprich(quelle, titel, text)
    }

    override fun onCleared() {
        mikro.release()
        super.onCleared()
    }

    fun abbrechen() {
        lauf?.cancel()
        auth.cancelChat()
        denkt = false
        schritt = ""
    }

    fun gespraechLeeren() {
        abbrechen()
        gespraech.clear()
    }

    // ---- ChatGPT-Anmeldung ----

    fun kiVerbinden(activity: ComponentActivity) {
        if (kiVerbindet) return
        kiVerbindet = true
        viewModelScope.launch {
            try {
                auth.login(activity) { info -> geraeteCode = info }
                kiVerbunden = true
                kiKonto = auth.email.orEmpty()
                meldung = "Mit ChatGPT verbunden."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                meldung = e.message ?: "Die Anmeldung ist fehlgeschlagen."
            } finally {
                geraeteCode = null
                kiVerbindet = false
            }
        }
    }

    fun kiAbbrechen() { auth.cancelLogin(); geraeteCode = null; kiVerbindet = false }

    fun kiTrennen() {
        auth.cancelLogin(); auth.logout()
        kiVerbunden = false; kiKonto = ""; geraeteCode = null
    }

    fun setzeModell(modell: CodexModel) {
        einstellungen.modell = modell
        einstellungen.denkstufe = modell.normalizeEffort(einstellungen.denkstufe)
    }

    fun setzeDenkstufe(stufe: ReasoningEffort) { einstellungen.denkstufe = stufe }

    // ---- Plugin und Tunnel ----

    fun serverSpeichern(host: String, token: String) {
        einstellungen.serverHost = host
        einstellungen.serverToken = token
        JarvisDienst.abgleichen(getApplication())
        meldung = "Gespeichert. Jarvis verbindet sich neu."
    }

    fun dienstSchalten(an: Boolean) {
        einstellungen.dienstAn = an
        JarvisDienst.abgleichen(getApplication())
    }

    fun adresseErneuern() {
        einstellungen.neuesGeheimnis()
        // Neu verbinden: Der Server lernt die neue Adresse erst beim Anmelden des Handys.
        JarvisDienst.abgleichen(getApplication())
        meldung = "Neue Plugin-Adresse erzeugt. Bitte in ChatGPT neu eintragen."
    }

    val pluginAdresse: String
        get() = einstellungen.serverHost.takeIf { it.isNotEmpty() }?.let { "https://$it/j/${einstellungen.geheimnis}/mcp" }.orEmpty()

    fun kopiere(text: String, was: String) {
        val app = getApplication<Application>()
        app.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(was, text))
        meldung = "$was kopiert."
    }

    fun ausZwischenablage(): String {
        val app = getApplication<Application>()
        return (app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
            ?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(app)?.toString()?.trim().orEmpty()
    }

    fun protokollLeeren() = Protokoll.leere()

    // ---- Wetter ----
    var wetterSuchtOrt by mutableStateOf(false); private set

    /** Sucht die Koordinaten zum Ortsnamen (Open-Meteo) und holt die Vorhersage für den neuen Ort. */
    fun wetterOrtSetzen(name: String) {
        if (name.isBlank() || wetterSuchtOrt) return
        wetterSuchtOrt = true
        viewModelScope.launch {
            val gefunden = withContext(Dispatchers.IO) {
                runCatching {
                    val adresse = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=de&name=" + java.net.URLEncoder.encode(name.trim(), "UTF-8")
                    okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(adresse).build()).execute().use { a ->
                        org.json.JSONObject(a.body?.string().orEmpty()).optJSONArray("results")?.optJSONObject(0)
                    }
                }.getOrNull()
            }
            if (gefunden == null) meldung = "Diesen Ort habe ich nicht gefunden."
            else {
                einstellungen.wetterBreite = gefunden.optDouble("latitude")
                einstellungen.wetterLaenge = gefunden.optDouble("longitude")
                einstellungen.wetterOrt = listOf(gefunden.optString("name"), gefunden.optString("admin1")).filter { it.isNotBlank() }.distinct().joinToString(", ")
                withContext(Dispatchers.IO) { Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.WetterFaehigkeit>().firstOrNull()?.synchronisiere() }
                meldung = "Wetter-Ort: ${einstellungen.wetterOrt}."
            }
            wetterSuchtOrt = false
        }
    }

    val kalenderSchreibenErlaubt: Boolean
        get() = androidx.core.content.ContextCompat.checkSelfPermission(getApplication(), android.Manifest.permission.WRITE_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED

    // ---- Tagebuch: Drive-Berechtigung ----
    var driveErneuertGerade by mutableStateOf(false); private set

    /** Öffnet die Zustimmungsseite von Google und gibt den neuen Nur-Lese-Zugang an den Server weiter. */
    fun driveBerechtigungErneuern() {
        if (driveErneuertGerade) return
        driveErneuertGerade = true
        viewModelScope.launch {
            val (erfolg, text) = de.frank.jarvis.tunnel.DriveFreigabe.erneuere(getApplication())
            if (erfolg) withContext(Dispatchers.IO) { Register.alle(getApplication()).filterIsInstance<de.frank.jarvis.faehigkeit.TagebuchFaehigkeit>().firstOrNull()?.synchronisiere() }
            meldung = text
            driveErneuertGerade = false
            lagePruefen()
        }
    }

    // ---- E-Mail ----

    fun mailSpeichern(adresse: String, passwort: String, empfaenger: String) {
        einstellungen.mailAdresse = adresse
        // Leeres Feld heißt: das gespeicherte Passwort behalten.
        if (passwort.isNotBlank()) einstellungen.mailPasswort = passwort
        einstellungen.mailEmpfaenger = empfaenger
        lagePruefen()
        meldung = "E-Mail-Einstellungen gespeichert."
    }

    fun mailEntfernen() {
        einstellungen.mailPasswort = ""
        lagePruefen()
        meldung = "App-Passwort entfernt."
    }

    // ---- Tagesauswertung ----

    fun auswertungJetzt() {
        JarvisDienst.auswerten(getApplication(), "von Hand in der App gestartet")
        meldung = "Jarvis erstellt die Tagesauswertung. Das dauert ein bis drei Minuten."
    }

    fun auswertungSchalten(an: Boolean) {
        einstellungen.auswertungAn = an
        de.frank.jarvis.auswertung.Zeitplan.stelle(getApplication())
    }

    fun auswertungAbstand(stunden: Int) { einstellungen.auswertungAbstand = stunden; de.frank.jarvis.auswertung.Zeitplan.stelle(getApplication()) }
    fun auswertungSchlafpause(an: Boolean) { einstellungen.auswertungSchlafpause = an; de.frank.jarvis.auswertung.Zeitplan.stelle(getApplication()) }
    fun auswertungAbgleich(an: Boolean) { einstellungen.auswertungAbgleich = an }
    fun auswertungDeutung(an: Boolean) { einstellungen.auswertungDeutung = an }

    val naechsteAuswertung: String
        get() = de.frank.jarvis.auswertung.Zeitplan.naechster(getApplication())?.let {
            (if (it.toLocalDate() == java.time.LocalDate.now()) "heute" else "morgen") + " um " + it.toLocalTime().toString().take(5) + " Uhr"
        } ?: "ausgeschaltet"
}

/** Auftrag an das Modell für die KI-Korrektur einer einzelnen Regel. */
private const val REGEL_KORREKTUR =
    "Du formulierst eine Vorgabe von Frank zu einer klaren Regel für seinen Assistenten Jarvis um. Frank hat sie eingesprochen oder eingetippt; " +
        "Jarvis bekommt die Regel danach bei jeder Antwort als Anweisung beigelegt und muss sie ohne weiteres Wissen verstehen. " +
        "Schreibe sie in gutem Deutsch als vollständige, für sich allein verständliche Anweisung an Jarvis: wann sie gilt und was dann zu tun ist. " +
        "Behalte jeden inhaltlichen Punkt und jeden genannten Wert, füge nichts hinzu und deute nichts um. Lass Füllwörter, Wiederholungen und Selbstkorrekturen weg " +
        "und berichtige Hörfehler der Spracherkennung (zum Beispiel „Javis“ für Jarvis, „WUPP“ für Whoop). So kurz wie möglich, höchstens 400 Zeichen. " +
        "Antworte nur mit der Regel, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung."

/** Auftrag an das Modell für die KI-Korrektur einer einzelnen Notiz über Frank. */
private const val INFO_KORREKTUR =
    "Du formulierst eine Angabe, die Frank über sich selbst eingesprochen oder eingetippt hat, zu einer klaren Notiz für seinen Assistenten Jarvis um. " +
        "Jarvis hat die Notiz danach bei jeder Antwort vor Augen und muss sie ohne weiteres Wissen verstehen. " +
        "Schreibe sie in gutem Deutsch als kurzen, für sich allein verständlichen Satz über Frank in der dritten Person (bei mehreren Angaben höchstens drei Sätze). " +
        "Behalte jede Tatsache, jeden Namen und jede Zahl genau so, füge nichts hinzu und deute nichts um. Lass Füllwörter, Wiederholungen und Selbstkorrekturen weg " +
        "und berichtige offensichtliche Hörfehler der Spracherkennung. Höchstens 300 Zeichen. " +
        "Antworte nur mit der Notiz, ohne Anführungszeichen, ohne Einleitung und ohne Erklärung."
