package de.frank.modellkompass.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.modellkompass.auth.CodexAuthManager
import de.frank.modellkompass.auth.DeviceAuthInfo
import de.frank.modellkompass.data.Bereich
import de.frank.modellkompass.data.Einstellungen
import de.frank.modellkompass.data.Ergebnis
import de.frank.modellkompass.data.Speicher
import de.frank.modellkompass.ki.Recherche
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

sealed interface Bildschirm {
    data object Start : Bildschirm
    data class Detail(val bereich: Bereich) : Bildschirm
    data object Einstellungen : Bildschirm
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val einstellungen = Einstellungen.get(app)
    val auth = CodexAuthManager(app)
    private val recherche = Recherche(auth, einstellungen)

    var bildschirm by mutableStateOf<Bildschirm>(Bildschirm.Start); private set
    var meldung by mutableStateOf<String?>(null); private set

    /** Ergebnisse je Bereich-Kennung. */
    val ergebnisse = mutableStateMapOf<String, Ergebnis>()
    /** Zwischenstand je Bereich, solange dessen Recherche läuft oder wartet. */
    val status = mutableStateMapOf<String, String>()
    val fehler = mutableStateMapOf<String, String>()

    var laufAktiv by mutableStateOf(false); private set
    var laufGesamt by mutableIntStateOf(0); private set
    var laufFertig by mutableIntStateOf(0); private set
    /** Die Rückfrage vor dem vollständigen Lauf ist offen. */
    var rueckfrage by mutableStateOf(false); private set
    private var lauf: Job? = null

    var kiVerbunden by mutableStateOf(auth.isConnected); private set
    var kiVerbindet by mutableStateOf(false); private set
    var kiFehler by mutableStateOf<String?>(null); private set
    var geraeteCode by mutableStateOf<DeviceAuthInfo?>(null); private set

    init {
        viewModelScope.launch {
            val gespeichert = withContext(Dispatchers.IO) { Speicher.lade(getApplication()) }
            ergebnisse.putAll(gespeichert)
        }
    }

    fun oeffne(b: Bereich) { bildschirm = Bildschirm.Detail(b) }
    fun einstellungenOeffnen() { bildschirm = Bildschirm.Einstellungen }
    fun zurueck() { bildschirm = Bildschirm.Start }
    fun melde(text: String) { meldung = text }
    fun meldungWeg() { meldung = null }

    fun modusWechseln(dunkelJetzt: Boolean) { einstellungen.modus = if (dunkelJetzt) "hell" else "dunkel" }

    // ================= Recherche =================

    fun rueckfrageZeigen() {
        if (verbunden()) rueckfrage = true
    }
    fun rueckfrageSchliessen() { rueckfrage = false }

    fun vollstaendigerLauf() {
        rueckfrage = false
        starte(Bereich.entries)
    }

    fun einzelLauf(b: Bereich) = starte(listOf(b))

    fun abbrechen() {
        lauf?.cancel()
        auth.cancelChat()
        melde("Recherche abgebrochen")
    }

    /** Ohne Anmeldung führt der Weg in die Einstellungen statt in eine Fehlermeldung. */
    private fun verbunden(): Boolean {
        kiVerbunden = auth.isConnected
        if (kiVerbunden) return true
        melde("Bitte zuerst mit ChatGPT verbinden")
        bildschirm = Bildschirm.Einstellungen
        return false
    }

    private fun starte(bereiche: List<Bereich>) {
        if (!verbunden()) return
        if (laufAktiv) { melde("Es läuft bereits eine Recherche"); return }
        auth.webSucheZuruecksetzen()
        laufAktiv = true
        laufGesamt = bereiche.size
        laufFertig = 0
        bereiche.forEach { status[it.id] = "wartet …"; fehler.remove(it.id) }
        lauf = viewModelScope.launch {
            // Drei Bereiche gleichzeitig: deutlich schneller als nacheinander, ohne das Kontingent zu überrennen.
            val tor = Semaphore(3)
            try {
                coroutineScope { bereiche.forEach { b -> launch { tor.withPermit { recherchiere(b) } } } }
                val gescheitert = bereiche.count { fehler.containsKey(it.id) }
                melde(
                    when {
                        gescheitert == 0 && bereiche.size == 1 -> "„${bereiche.first().titel}“ ist aktuell"
                        gescheitert == 0 -> "Alle ${bereiche.size} Bereiche sind aktuell"
                        else -> "$gescheitert von ${bereiche.size} Bereichen sind gescheitert"
                    },
                )
            } finally {
                status.clear()
                laufAktiv = false
            }
        }
    }

    private suspend fun recherchiere(b: Bereich) {
        try {
            val ergebnis = recherche.suche(b) { text -> viewModelScope.launch { if (status.containsKey(b.id)) status[b.id] = text } }
            withContext(Dispatchers.IO) { Speicher.sichere(getApplication(), ergebnis) }
            ergebnisse[b.id] = ergebnis
        } catch (c: CancellationException) {
            throw c
        } catch (e: Throwable) {
            fehler[b.id] = e.message ?: "Unbekannter Fehler"
        } finally {
            status.remove(b.id)
            laufFertig++
        }
    }

    // ================= KI-Verbindung =================

    fun kiVerbinden(activity: ComponentActivity) {
        if (kiVerbindet) return
        kiFehler = null
        kiVerbindet = true
        viewModelScope.launch {
            try {
                auth.login(activity) { info -> geraeteCode = info }
                kiVerbunden = true
                melde("Mit ChatGPT verbunden")
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                kiFehler = e.message
            } finally {
                geraeteCode = null
                kiVerbindet = false
            }
        }
    }

    fun kiAbbrechen() { auth.cancelLogin(); geraeteCode = null; kiVerbindet = false }

    fun kiTrennen() {
        auth.cancelLogin(); auth.logout()
        kiVerbunden = false; geraeteCode = null
    }
}
