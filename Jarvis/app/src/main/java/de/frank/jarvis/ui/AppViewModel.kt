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

enum class Reiter(val anzeige: String) { JARVIS("Jarvis"), AKTIVITAET("Aktivität"), EINSTELLUNGEN("Einstellungen") }

data class Nachricht(val vonMir: Boolean, val text: String)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val einstellungen = Einstellungen.get(app)
    private val agent = JarvisAgent(app)
    private val auth get() = agent.auth

    var reiter by mutableStateOf(Reiter.JARVIS)

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
        }
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
}
