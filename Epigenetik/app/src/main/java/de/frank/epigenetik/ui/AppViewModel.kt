package de.frank.epigenetik.ui

import android.app.Application
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.epigenetik.audio.Diktat
import de.frank.epigenetik.audio.GroqTranscriber
import de.frank.epigenetik.audio.MicRecorder
import de.frank.epigenetik.auth.CodexAuthManager
import de.frank.epigenetik.auth.DeviceAuthInfo
import de.frank.epigenetik.bruecke.Eingabe
import de.frank.epigenetik.data.Bericht
import de.frank.epigenetik.data.Einstellungen
import de.frank.epigenetik.data.Kategorie
import de.frank.epigenetik.data.Recherche
import de.frank.epigenetik.data.Repository
import de.frank.epigenetik.data.SchalterWirkung
import de.frank.epigenetik.data.Typ
import de.frank.epigenetik.ki.Forscher
import de.frank.epigenetik.ki.Live
import de.frank.epigenetik.tts.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Tab(val titel: String) { START("Start"), WISSEN("Wissen"), ATLAS("Atlas"), ZIELE("Ziele"), STACK("Stack") }

sealed interface Seite {
    data object Haupt : Seite
    data class Detail(val id: Long) : Seite
    data class SchalterSeite(val id: String) : Seite
    data object Einstellungen : Seite
}

enum class Aufnahme { AUS, LAEUFT, VERARBEITET }

/** Wohin der diktierte Text geht. */
enum class DiktatZiel { START, ZIEL }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val einstellungen = Einstellungen.get(app)
    val repo = Repository.get(app)
    val auth = CodexAuthManager(app)
    private val tts = TtsManager(app)
    private val mikro = MicRecorder(app)

    val recherchen: StateFlow<List<Recherche>> = repo.alle.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val wirkungen: StateFlow<List<SchalterWirkung>> = repo.wirkungen.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val live = Live.staende

    // ---- Navigation ----
    var tab by mutableStateOf(Tab.START); private set
    private val stapel = mutableStateListOf<Seite>()
    val seite: Seite get() = stapel.lastOrNull() ?: Seite.Haupt

    fun zeigeTab(t: Tab) { stapel.clear(); tab = t }
    fun oeffne(s: Seite) { if (seite != s) stapel.add(s) }
    fun oeffne(id: Long) { oeffne(Seite.Detail(id)); viewModelScope.launch { repo.gelesen(id) } }
    fun zurueck(): Boolean { if (stapel.isEmpty()) return false; stapel.removeAt(stapel.lastIndex); return true }

    // ---- Meldungen ----
    var meldung by mutableStateOf<String?>(null); private set
    fun melde(text: String) { meldung = text }
    fun meldungWeg() { meldung = null }

    // ---- Eingabe ----
    var eingabe by mutableStateOf(""); private set
    var gewaehlterTyp by mutableStateOf<Typ?>(null); private set
    var gewaehlteKategorie by mutableStateOf<Kategorie?>(null); private set
    val erkannterTyp: Typ get() = gewaehlterTyp ?: if (eingabe.isBlank()) Typ.MITTEL else Eingabe.rateTyp(eingabe)
    val erkannteKategorie: Kategorie get() = gewaehlteKategorie ?: Eingabe.rateKategorie(eingabe)

    fun eingabeAendern(t: String) { eingabe = t; if (t.isBlank()) { gewaehlterTyp = null; gewaehlteKategorie = null } }
    fun waehleTyp(t: Typ) { gewaehlterTyp = t }
    fun waehleKategorie(k: Kategorie) { gewaehlteKategorie = k }

    var zielEingabe by mutableStateOf(""); private set
    fun zielEingabeAendern(t: String) { zielEingabe = t }

    /** Startet die Recherche(n) aus dem Eingabefeld. Mehrere Mittel lassen sich mit Komma trennen. */
    fun absenden() {
        if (erstNachAufnahme(::absenden)) return
        val text = eingabe.trim()
        if (text.isBlank()) { melde("Gib ein Mittel, eine Wirkung, ein Ziel oder eine Frage ein."); return }
        val typ = erkannterTyp
        val namen = if (typ == Typ.MITTEL) text.split(',', ';', '\n').map { it.trim() }.filter { it.isNotBlank() } else listOf(text)
        val kategorie = if (typ == Typ.MITTEL) gewaehlteKategorie else null
        eingabe = ""; gewaehlterTyp = null; gewaehlteKategorie = null
        recherchiere(typ, namen, kategorie)
    }

    fun recherchiere(typ: Typ, namen: List<String>, kategorie: Kategorie? = null, neu: Boolean = false) {
        if (!auth.isConnected) { melde("Bitte zuerst in den Einstellungen mit ChatGPT verbinden."); oeffne(Seite.Einstellungen); return }
        hinweiseAnfragen()
        viewModelScope.launch {
            val ergebnisse = namen.map { n -> repo.anlegen(typ, n, kategorie ?: if (typ == Typ.MITTEL) Eingabe.rateKategorie(n) else null, neu = neu) }
            val schon = ergebnisse.count { it.fertig && !neu }
            when {
                ergebnisse.size == 1 && schon == 1 -> { melde("„${ergebnisse[0].name}“ ist schon recherchiert."); oeffne(ergebnisse[0].id) }
                ergebnisse.size == 1 -> { melde("Recherche gestartet: ${ergebnisse[0].name}"); oeffne(ergebnisse[0].id) }
                else -> melde("${ergebnisse.size - schon} Recherchen gestartet" + if (schon > 0) ", $schon schon vorhanden" else "")
            }
        }
    }

    fun zielAbsenden() {
        if (erstNachAufnahme(::zielAbsenden)) return
        val t = zielEingabe.trim()
        if (t.isBlank()) return
        zielEingabe = ""
        recherchiere(Typ.ZIEL, listOf(t))
    }

    fun neuRecherchieren(r: Recherche) = recherchiere(r.typEnum, listOf(r.name), if (r.typEnum == Typ.MITTEL) r.kategorieEnum else null, neu = true)

    /** Öffnet ein Mittel aus einer Liste: vorhanden → Detail, sonst Recherche starten. */
    fun mittelAntippen(name: String, kategorie: Kategorie? = null) {
        viewModelScope.launch {
            val r = repo.findeMittel(name)
            if (r != null) oeffne(r.id) else recherchiere(Typ.MITTEL, listOf(name), kategorie)
        }
    }

    fun favorit(r: Recherche) = viewModelScope.launch { repo.setzeFavorit(r, !r.favorit); melde(if (!r.favorit) "Als Favorit markiert" else "Kein Favorit mehr") }
    fun stack(r: Recherche) = viewModelScope.launch { repo.setzeStack(r, !r.imStack); melde(if (!r.imStack) "${r.name} ist jetzt in deinem Stack" else "${r.name} aus dem Stack genommen") }
    fun notiz(r: Recherche, text: String) = viewModelScope.launch { repo.setzeNotiz(r, text) }
    fun loeschen(r: Recherche) = viewModelScope.launch {
        if (vorleseId == r.id) vorlesenStopp()
        repo.loeschen(r.id); zurueck(); melde("„${r.name}“ gelöscht")
    }
    fun abbrechen(r: Recherche) = viewModelScope.launch { Forscher.abbrechen(getApplication(), r.id) }

    fun stackAnalysieren() {
        val stack = recherchen.value.filter { it.imStack && it.fertig }
        if (stack.size < 2) { melde("Nimm zuerst mindestens zwei recherchierte Mittel in deinen Stack."); return }
        recherchiere(Typ.STACK, listOf("Mein Stack"), neu = true)
    }

    // ---- Vorlesen ----
    var vorleseId by mutableStateOf<Long?>(null); private set
    fun vorlesen(r: Recherche) {
        if (vorleseId == r.id) { vorlesenStopp(); return }
        vorleseId = r.id
        tts.speak(
            Bericht.zumVorlesen(r),
            onStart = {},
            onComplete = { if (vorleseId == r.id) vorleseId = null },
            onError = { e -> vorleseId = null; melde("Vorlesen fehlgeschlagen: ${e.message}") },
        )
    }
    fun vorlesenStopp() { tts.stop(); vorleseId = null }
    fun probeVorlesen() {
        vorlesenStopp()
        tts.speak("Sulforaphan aktiviert NRF2 und schaltet damit über zweihundert Schutzgene an.", {}, {}, { e -> melde("Vorlesen fehlgeschlagen: ${e.message}") })
    }

    // ---- Spracheingabe ----
    var aufnahme by mutableStateOf(Aufnahme.AUS); private set
    var aufnahmeStart by mutableLongStateOf(0L); private set
    var mikrofonAnfragen: () -> Unit = {}
    var hinweiseAnfragen: () -> Unit = {}
    var sprachIntentStarten: (Intent) -> Unit = {}
    private var diktatZiel = DiktatZiel.START
    private var nachAufnahme: (() -> Unit)? = null
    private var aufnahmeJob: Job? = null

    fun mikroTippen(ziel: DiktatZiel) {
        when (aufnahme) {
            Aufnahme.AUS -> {
                diktatZiel = ziel
                if (einstellungen.groqKey.isBlank()) handySpracheingabe() else mikrofonAnfragen()
            }
            Aufnahme.LAEUFT -> aufnahmeBeenden()
            Aufnahme.VERARBEITET -> Unit
        }
    }

    /** Ohne Groq-Schlüssel: die Spracheingabe des Handys (Google), funktioniert ohne Einrichtung. */
    private fun handySpracheingabe() {
        val intent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, if (diktatZiel == DiktatZiel.ZIEL) "Dein Ziel …" else "Mittel, Wirkung, Ziel oder Frage …")
        runCatching { sprachIntentStarten(intent) }.onFailure { melde("Keine Spracheingabe auf dem Handy gefunden. Trag einen Groq-Schlüssel in den Einstellungen ein.") }
    }

    fun handyDiktatErgebnis(text: String?) {
        if (text.isNullOrBlank()) return
        diktatEinfuegen(text)
    }

    private fun diktatEinfuegen(text: String) {
        when (diktatZiel) {
            DiktatZiel.START -> { eingabe = if (eingabe.isBlank()) text else eingabe.trimEnd() + " " + text; if (einstellungen.sofortNachDiktat && nachAufnahme == null) absenden() }
            DiktatZiel.ZIEL -> { zielEingabe = if (zielEingabe.isBlank()) text else zielEingabe.trimEnd() + " " + text; if (einstellungen.sofortNachDiktat && nachAufnahme == null) zielAbsenden() }
        }
    }

    fun mikrofonErlaubnisErgebnis(ok: Boolean) {
        if (!ok) { melde("Ohne Mikrofon-Erlaubnis geht die Spracheingabe nicht."); return }
        if (aufnahme != Aufnahme.AUS) return
        if (mikro.start(viewModelScope)) {
            aufnahme = Aufnahme.LAEUFT
            aufnahmeStart = System.currentTimeMillis()
        } else melde("Die Aufnahme konnte nicht gestartet werden.")
    }

    private fun erstNachAufnahme(weiter: () -> Unit): Boolean = when (aufnahme) {
        Aufnahme.LAEUFT -> { nachAufnahme = weiter; aufnahmeBeenden(); true }
        Aufnahme.VERARBEITET -> { nachAufnahme = weiter; true }
        Aufnahme.AUS -> false
    }

    private fun aufnahmeBeenden() {
        aufnahme = Aufnahme.VERARBEITET
        aufnahmeJob = viewModelScope.launch {
            val transcriber = GroqTranscriber(
                apiKey = einstellungen.groqKey,
                filterStille = einstellungen.filterStille,
                filterMetriken = einstellungen.filterMetriken,
                filterZeitstempel = einstellungen.filterZeitstempel,
                filterFloskeln = einstellungen.filterFloskeln,
            )
            try {
                val wav = mikro.stop()
                if (wav == null) { melde("Es wurde nichts aufgenommen."); nachAufnahme = null; return@launch }
                val text = Diktat(transcriber).transkribiere(wav).text.trim()
                if (text.isBlank()) { melde("Keine Sprache erkannt."); nachAufnahme = null } else diktatEinfuegen(text)
            } catch (c: CancellationException) {
                throw c
            } catch (e: Throwable) {
                melde(e.message ?: "Die Spracherkennung ist fehlgeschlagen.")
                nachAufnahme = null
            } finally {
                transcriber.shutdown()
                aufnahme = Aufnahme.AUS
                nachAufnahme?.let { w -> nachAufnahme = null; w() }
            }
        }
    }

    fun aufnahmeAbbrechen() {
        if (aufnahme == Aufnahme.AUS) return
        nachAufnahme = null
        mikro.release()
        aufnahmeJob?.cancel()
        aufnahme = Aufnahme.AUS
    }

    // ---- KI-Verbindung ----
    var kiVerbunden by mutableStateOf(auth.isConnected); private set
    var kiVerbindet by mutableStateOf(false); private set
    var geraeteCode by mutableStateOf<DeviceAuthInfo?>(null); private set
    var kiFehler by mutableStateOf<String?>(null); private set

    fun pruefen() { kiVerbunden = auth.isConnected }

    fun kiVerbinden(activity: ComponentActivity) {
        if (kiVerbindet) return
        kiFehler = null
        kiVerbindet = true
        viewModelScope.launch {
            try {
                auth.login(activity) { info -> geraeteCode = info }
                kiVerbunden = true
                melde("Mit ChatGPT verbunden")
                repo.offeneFortsetzen()
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
    fun kiTrennen() { auth.cancelLogin(); auth.logout(); kiVerbunden = false; geraeteCode = null }

    override fun onCleared() {
        mikro.release()
        tts.shutdown()
        super.onCleared()
    }
}
