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
        if (!erlaubt) { meldung = "Ohne Mikrofon-Erlaubnis kann ich dich nicht hören."; return }
        vorleser.stopp()
        if (mikro.start(viewModelScope)) nimmtAuf = true else meldung = "Die Aufnahme ließ sich nicht starten."
    }

    private fun aufnahmeBeenden() {
        nimmtAuf = false
        schreibtMit = true
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

    /** Speichert die drei Uhrzeiten. Liefert false, wenn eine davon nicht lesbar ist. */
    fun auswertungZeitenSpeichern(eingaben: List<String>): Boolean {
        val gefuellt = eingaben.map { it.trim() }.filter { it.isNotEmpty() }
        val zeiten = gefuellt.map { de.frank.jarvis.auswertung.Zeitplan.leseZeit(it) }
        if (zeiten.isEmpty() || zeiten.any { it == null }) {
            meldung = "Bitte Uhrzeiten als Stunde:Minute eingeben, zum Beispiel 4:25."
            return false
        }
        einstellungen.auswertungZeiten = zeiten.filterNotNull().sorted().joinToString(",") { "%02d:%02d".format(it.hour, it.minute) }
        de.frank.jarvis.auswertung.Zeitplan.stelle(getApplication())
        meldung = "Uhrzeiten gespeichert."
        return true
    }

    val naechsteAuswertung: String
        get() = de.frank.jarvis.auswertung.Zeitplan.naechster(getApplication())?.let {
            (if (it.toLocalDate() == java.time.LocalDate.now()) "heute" else "morgen") + " um " + it.toLocalTime().toString().take(5) + " Uhr"
        } ?: "ausgeschaltet"
}
