package de.frank.experimente.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.experimente.ExperimenteApp
import de.frank.experimente.audio.GroqTranscriber
import de.frank.experimente.audio.MicRecorder
import de.frank.experimente.auth.CodexAuthException
import de.frank.experimente.auth.AuthErrorKind
import de.frank.experimente.auth.DeviceAuthInfo
import de.frank.experimente.notify.Erinnerungen
import de.frank.experimente.tts.QwenVoiceEnrollment
import de.frank.experimente.data.local.Aufgabe
import de.frank.experimente.data.local.Auswertung
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.GespraechsRunde
import de.frank.experimente.data.local.LogTag
import de.frank.experimente.data.local.MerkEintrag
import de.frank.experimente.data.local.Vorschlag
import de.frank.experimente.data.local.Ziel
import de.frank.experimente.data.repo.Ablage
import de.frank.experimente.tts.Vorleser
import de.frank.experimente.ui.theme.Erscheinung
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/** Der Tagesstand von B-01 (01-FUNKTIONS-SPEC §4). */
enum class TagesStand { LEER, AUFNAHME, LAGE_STEHT, VORSCHLAEGE, LAEUFT }

/** Wohin eine Aufnahme fließt. */
enum class Sprechziel { LAGE, GESPRAECH, AUSWERTUNG, ZIEL, EIGENE_IDEE, SELBSTBILD }

@Suppress("OPT_IN_USAGE")
class AppViewModel(app: Application) : AndroidViewModel(app) {

    val einstellungen = (app as ExperimenteApp).einstellungen
    private val codex = de.frank.experimente.auth.CodexAuthManager(app)
    val ablage = Ablage(app, einstellungen, codex)
    private val mikro = MicRecorder(app)
    private val vorleser = Vorleser(app, einstellungen)

    // --- Erscheinung (F-26) ---
    val erscheinung: StateFlow<Erscheinung> = einstellungen.erscheinungFlow
        .map(Erscheinung::ausId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, Erscheinung.ausId(einstellungen.erscheinung))

    fun erscheinungWeiterschalten() {
        einstellungen.erscheinung = erscheinung.value.naechste().id
    }

    fun erscheinungSetzen(neu: Erscheinung) {
        einstellungen.erscheinung = neu.id
    }

    // --- Tag ---
    private val _heute = MutableStateFlow(LocalDate.now())
    val heute: StateFlow<LocalDate> = _heute.asStateFlow()

    // --- Zustände ---
    private val _lageText = MutableStateFlow("")
    val lageText: StateFlow<String> = _lageText.asStateFlow()

    private val _stand = MutableStateFlow(TagesStand.LEER)
    val stand: StateFlow<TagesStand> = _stand.asStateFlow()

    private val _laedt = MutableStateFlow(false)
    val laedt: StateFlow<Boolean> = _laedt.asStateFlow()

    private val _fehler = MutableStateFlow<String?>(null)
    val fehler: StateFlow<String?> = _fehler.asStateFlow()

    private val _nimmtAuf = MutableStateFlow(false)
    val nimmtAuf: StateFlow<Boolean> = _nimmtAuf.asStateFlow()

    private val _liestVor = MutableStateFlow(false)
    val liestVor: StateFlow<Boolean> = _liestVor.asStateFlow()

    /** Bisherige Fassungen je Textfeld, damit „verbessern“ jedes Mal etwas Neues liefert (F-02). */
    private val fassungen = mutableMapOf<Sprechziel, MutableList<String>>()

    private var aufnahmeZiel: Sprechziel = Sprechziel.LAGE
    private var aufgabe: Job? = null

    // --- Ströme aus der Ablage ---
    val offeneExperimente: StateFlow<List<Experiment>> = ablage.offeneExperimenteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val vorschlaege: StateFlow<List<Vorschlag>> = _heute
        .flatMapLatest { ablage.vorschlaegeFlow(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ziele: StateFlow<List<Ziel>> = ablage.zieleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val merkliste: StateFlow<List<MerkEintrag>> = ablage.merklisteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val erkenntnisse = ablage.erkenntnisseFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val logAusfuehrlich: StateFlow<List<LogTag>> = ablage.logAusfuehrlichFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val logLangzeit: StateFlow<List<LogTag>> = ablage.logLangzeitFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selbstbild: StateFlow<String> = ablage.selbstbildFlow
        .map { it?.text.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Alle heutigen Aufgaben, nach Experiment gruppiert — **eine** Liste für den Tag (F-07). */
    private val _tagesaufgaben = MutableStateFlow<List<Pair<Experiment, List<Aufgabe>>>>(emptyList())
    val tagesaufgaben: StateFlow<List<Pair<Experiment, List<Aufgabe>>>> = _tagesaufgaben.asStateFlow()

    init {
        viewModelScope.launch {
            // F-15 läuft, bevor sonst etwas geschieht.
            val tag = _heute.value
            if (ablage.verdichtungFaellig(tag)) {
                lauf { ablage.verdichtenFaellige(tag) }
            }
            _lageText.value = ablage.lage(tag).orEmpty()
            standNeuBestimmen()
        }
        viewModelScope.launch {
            ablage.offeneExperimenteFlow.collect { standNeuBestimmen() }
        }
        viewModelScope.launch {
            ablage.vorschlaegeFlow(_heute.value).collect { standNeuBestimmen() }
        }
    }

    private suspend fun standNeuBestimmen() {
        val tag = _heute.value
        val offene = offeneExperimente.value
        _tagesaufgaben.value = offene.map { it to ablage.aufgabenHeute(it, tag) }
        _stand.value = when {
            _nimmtAuf.value -> TagesStand.AUFNAHME
            offene.isNotEmpty() -> TagesStand.LAEUFT
            vorschlaege.value.isNotEmpty() -> TagesStand.VORSCHLAEGE
            _lageText.value.isNotBlank() -> TagesStand.LAGE_STEHT
            else -> TagesStand.LEER
        }
    }

    fun aufgabenNeuLaden() {
        viewModelScope.launch { standNeuBestimmen() }
    }

    // -----------------------------------------------------------------------
    // Aufnahme und Transkription
    // -----------------------------------------------------------------------

    fun aufnahmeUmschalten(ziel: Sprechziel, beiText: (String) -> Unit) {
        if (_nimmtAuf.value) {
            beendeAufnahme(beiText)
        } else {
            aufnahmeZiel = ziel
            val gestartet = mikro.start(viewModelScope)
            if (!gestartet) {
                _fehler.value = "Ohne Mikrofon kann ich dich nicht hören."
                return
            }
            _nimmtAuf.value = true
            viewModelScope.launch { standNeuBestimmen() }
        }
    }

    private fun beendeAufnahme(beiText: (String) -> Unit) {
        aufgabe?.cancel()
        aufgabe = viewModelScope.launch {
            _nimmtAuf.value = false
            val wav = mikro.stop()
            standNeuBestimmen()
            if (wav == null || wav.isEmpty()) {
                _fehler.value = "Da war nichts zu hören."
                return@launch
            }
            val schluessel = einstellungen.groqSchluessel
            if (schluessel.isBlank()) {
                _fehler.value = "Für die Spracherkennung fehlt der Groq-Schlüssel."
                return@launch
            }
            _laedt.value = true
            try {
                val text = GroqTranscriber(schluessel).transcribe(wav)
                if (text.isBlank()) {
                    _fehler.value = "Da war nichts zu hören."
                } else {
                    beiText(text)
                }
            } catch (e: Exception) {
                _fehler.value = lesbar(e)
            } finally {
                _laedt.value = false
            }
        }
    }

    // -----------------------------------------------------------------------
    // F-01 / F-02 — Lage und Verbessern
    // -----------------------------------------------------------------------

    fun lageSprechen() = aufnahmeUmschalten(Sprechziel.LAGE) { text -> lageSetzen(text) }

    fun lageSetzen(text: String) {
        _lageText.value = text
        viewModelScope.launch { standNeuBestimmen() }
    }

    fun lageBestaetigen() {
        val text = _lageText.value.trim()
        if (text.isEmpty()) return
        lauf {
            ablage.lageSpeichern(_heute.value, text)
            standNeuBestimmen()
            ablage.vorschlaegeErzeugen(_heute.value, erneut = false)
        }
    }

    fun textVerbessern(ziel: Sprechziel, aktuell: String, beiText: (String) -> Unit) {
        val bisher = fassungen.getOrPut(ziel) { mutableListOf() }
        lauf {
            val neu = ablage.textVerbessern(aktuell, bisher.toList())
            if (neu.isNotBlank()) {
                bisher += neu
                beiText(neu)
            }
        }
    }

    fun verbesserungZuruecknehmen(ziel: Sprechziel, beiText: (String) -> Unit) {
        val bisher = fassungen[ziel] ?: return
        if (bisher.isEmpty()) return
        bisher.removeAt(bisher.lastIndex)
        beiText(bisher.lastOrNull().orEmpty())
    }

    fun hatVerbesserung(ziel: Sprechziel): Boolean = fassungen[ziel]?.isNotEmpty() == true

    // -----------------------------------------------------------------------
    // F-04 / F-05 / F-06 — Vorschläge
    // -----------------------------------------------------------------------

    fun vorschlaegeAktualisieren() = lauf {
        ablage.vorschlaegeErzeugen(_heute.value, erneut = true)
    }

    fun merken(vorschlag: Vorschlag) = lauf { ablage.merken(vorschlag) }

    fun experimentStarten(vorschlag: Vorschlag) = lauf {
        ablage.experimentStarten(vorschlag, _heute.value)
        standNeuBestimmen()
    }

    // -----------------------------------------------------------------------
    // F-08 — Haken
    // -----------------------------------------------------------------------

    fun hakenUmschalten(aufgabe: Aufgabe) = lauf {
        ablage.hakenUmschalten(aufgabe)
        standNeuBestimmen()
    }

    // -----------------------------------------------------------------------
    // F-09 — Gespräch
    // -----------------------------------------------------------------------

    fun gespraech(experimentId: Long) = ablage.gespraechFlow(experimentId)

    fun gespraechSprechen(experimentId: Long) =
        aufnahmeUmschalten(Sprechziel.GESPRAECH) { text -> gespraechSenden(experimentId, text) }

    fun gespraechSenden(experimentId: Long, frage: String) {
        if (frage.isBlank()) return
        lauf {
            val antwort = ablage.gespraechFortsetzen(experimentId, frage, _heute.value)
            vorlesen(antwort)
        }
    }

    // -----------------------------------------------------------------------
    // F-10 / F-11 / F-13 — Auswertung
    // -----------------------------------------------------------------------

    fun auswertungen(experimentId: Long) = ablage.auswertungenFlow(experimentId)

    fun auswertungSprechen(experiment: Experiment, beiText: (String) -> Unit) =
        aufnahmeUmschalten(Sprechziel.AUSWERTUNG, beiText)

    fun auswerten(experiment: Experiment, text: String, nichtUmgesetzt: Boolean = false) {
        if (text.isBlank()) return
        lauf {
            ablage.auswerten(experiment, _heute.value, text, nichtUmgesetzt)
            ablage.logbuchFortschreiben(_heute.value, "Auswertung zu „${experiment.titel}“: $text")
            standNeuBestimmen()
        }
    }

    // -----------------------------------------------------------------------
    // F-12 — Vorlesen
    // -----------------------------------------------------------------------

    fun vorlesen(text: String) {
        if (_liestVor.value) {
            vorleser.stopp()
            _liestVor.value = false
            return
        }
        vorleser.sprich(
            text = text,
            onStart = { _liestVor.value = true },
            onFertig = { _liestVor.value = false },
            onFehler = {
                _liestVor.value = false
                _fehler.value = it.message ?: "Das Vorlesen hat nicht geklappt."
            },
        )
    }

    // -----------------------------------------------------------------------
    // F-16 / F-18 / F-19 / F-20 / F-21 — Listen und Texte
    // -----------------------------------------------------------------------

    fun logtagSpeichern(tag: LogTag) = lauf { ablage.logtagSpeichern(tag) }
    fun logtagLoeschen(tag: LogTag) = lauf { ablage.logtagLoeschen(tag) }

    fun eigeneIdeeAnlegen(text: String) {
        if (text.isBlank()) return
        lauf { ablage.eigeneIdeeAnlegen(text, _heute.value) }
    }

    fun merkeintragLoeschen(eintrag: MerkEintrag) = lauf { ablage.merkeintragLoeschen(eintrag) }

    fun zielSpeichern(text: String, vorhandenes: Ziel? = null) {
        if (text.isBlank()) return
        lauf {
            ablage.zielSpeichern(
                vorhandenes?.copy(text = text.trim(), geaendertAm = Instant.now())
                    ?: Ziel(text = text.trim(), angelegtAm = Instant.now(), geaendertAm = Instant.now()),
            )
        }
    }

    fun zielLoeschen(ziel: Ziel) = lauf { ablage.zielLoeschen(ziel) }

    fun selbstbildSpeichern(text: String) = lauf { ablage.selbstbildSpeichern(text) }

    // -----------------------------------------------------------------------
    // F-23 / F-24 / F-25 — Stimme, Zugänge, Erinnerungen (B-08)
    // -----------------------------------------------------------------------

    private val _codexKonto = MutableStateFlow(if (codex.isConnected) codex.email ?: "angemeldet" else null)

    /** Die angemeldete Kennung, oder `null`, solange kein Zugang besteht. */
    val codexKonto: StateFlow<String?> = _codexKonto.asStateFlow()

    private val _geraetecode = MutableStateFlow<DeviceAuthInfo?>(null)

    /** Code und Adresse der laufenden Geräteanmeldung (F-24). */
    val geraetecode: StateFlow<DeviceAuthInfo?> = _geraetecode.asStateFlow()

    fun anmelden(activity: ComponentActivity) = lauf {
        try {
            val ergebnis = codex.login(activity) { _geraetecode.value = it }
            _codexKonto.value = ergebnis.email ?: "angemeldet"
        } finally {
            _geraetecode.value = null
        }
    }

    fun abmelden() {
        codex.logout()
        _codexKonto.value = null
    }

    /** F-25 — nach jeder Änderung an den Erinnerungen neu stellen. */
    fun erinnerungenNeuSetzen() {
        Erinnerungen.allesNeuSetzen(getApplication(), einstellungen)
    }

    /** F-23 — eigene Stimme aufnehmen und bei DashScope registrieren. */
    fun eigeneStimmeUmschalten() {
        if (_nimmtAuf.value) {
            aufgabe?.cancel()
            aufgabe = viewModelScope.launch {
                _nimmtAuf.value = false
                val wav = mikro.stop()
                if (wav == null || wav.isEmpty()) {
                    _fehler.value = "Da war nichts zu hören."
                    return@launch
                }
                val schluessel = einstellungen.qwenSchluessel
                if (schluessel.isBlank()) {
                    _fehler.value = "Für diese Stimme fehlt der Schlüssel."
                    return@launch
                }
                _laedt.value = true
                try {
                    einstellungen.stimmeEigen = QwenVoiceEnrollment().create(schluessel, "frank", wav)
                } catch (e: Exception) {
                    _fehler.value = lesbar(e)
                } finally {
                    _laedt.value = false
                }
            }
            return
        }
        if (!mikro.start(viewModelScope)) {
            _fehler.value = "Ohne Mikrofon kann ich dich nicht hören."
            return
        }
        _nimmtAuf.value = true
    }

    // -----------------------------------------------------------------------
    // Hilfen
    // -----------------------------------------------------------------------

    fun fehlerVerwerfen() { _fehler.value = null }

    private fun lauf(block: suspend () -> Unit) {
        viewModelScope.launch {
            _laedt.value = true
            _fehler.value = null
            try {
                block()
            } catch (e: Exception) {
                _fehler.value = lesbar(e)
            } finally {
                _laedt.value = false
            }
        }
    }

    /** Die Fehlertexte aus 02-UI-SPEC.md §8 — jeder Fall mit eigenem Text, keiner still. */
    private fun lesbar(e: Exception): String = when {
        e is CodexAuthException && e.kind == AuthErrorKind.REAUTH -> "Deine Anmeldung ist abgelaufen."
        e is CodexAuthException && e.kind == AuthErrorKind.QUOTA -> "Dein Kontingent ist erschöpft."
        e is CodexAuthException && e.kind == AuthErrorKind.NETWORK -> "Dafür brauche ich Netz."
        e is java.io.IOException -> "Dafür brauche ich Netz."
        else -> e.message ?: "Die Antwort war unbrauchbar."
    }

    override fun onCleared() {
        super.onCleared()
        mikro.release()
        vorleser.beenden()
    }
}
