package de.frank.experimente.ui

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.experimente.ExperimenteApp
import de.frank.experimente.audio.GroqTranscriber
import de.frank.experimente.audio.MicRecorder
import de.frank.experimente.auth.CodexFehler
import de.frank.experimente.auth.FehlerArt
import de.frank.experimente.auth.Geraetecode
import de.frank.experimente.data.local.ChatTurn
import de.frank.experimente.data.local.Evaluation
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.Goal
import de.frank.experimente.data.local.Insight
import de.frank.experimente.data.local.LogDay
import de.frank.experimente.data.local.Suggestion
import de.frank.experimente.data.local.Task
import de.frank.experimente.data.local.WatchlistItem
import de.frank.experimente.data.repo.Ablage
import de.frank.experimente.ui.theme.Bewegung
import de.frank.experimente.ui.theme.Effektstufe
import de.frank.experimente.ui.theme.Erscheinung
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Der Tag auf B-01 (Funktions-Spec §4).
 *
 * **`LAEUFT` ist auf B-10 gewandert:** laufende Experimente und die To-Do-Liste stehen seit
 * dieser Fassung im Monitor, nicht mehr auf „Heute“.
 */
enum class TagZustand { LEER, AUFNAHME, LAGE_STEHT, WARTET, VORSCHLAEGE, ABEND }

/**
 * Der Zustand des Monitors B-10 (Funktions-Spec §4).
 *
 * `ANLEGEN` liegt als Fläche darüber und wird deshalb getrennt geführt.
 */
enum class MonitorZustand { LAEDT, LEER, NUR_ANSTEHEND, LAEUFT, VOLL }

/** Der Weg durch B-03 (Funktions-Spec F-10/F-11). */
enum class AuswertungZustand { AUFNAHME, TEXT, WARTET, ANTWORT }

/** Welcher Bildschirm gerade zu sehen ist. */
enum class Ziel(val kennung: String, val beschriftung: String) {
    MONITOR("B-10", "Monitor"),
    HEUTE("B-01", "Heute"),
    GESPRAECH("B-02", "Gespräch"),
    AUSWERTUNG("B-03", "Auswertung"),
    ZIELE("B-04", "Ziele"),
    MERKLISTE("B-05", "Merkliste"),
    ERKENNTNISSE("B-06", "Erkenntnisse"),
    LOGBUCH("B-07", "Logbuch"),
    EINSTELLUNGEN("B-08", "Einstellungen"),
    SELBSTBILD("B-09", "Selbstbild"),
    ;

    companion object {
        /**
         * Die **sechs** Hauptbildschirme in der Reihenfolge der unteren Leiste.
         * F-27 (Wischen) läuft über alle sechs in genau dieser Reihenfolge.
         */
        val hauptreihe = listOf(MONITOR, HEUTE, ZIELE, MERKLISTE, ERKENNTNISSE, LOGBUCH)

        fun aus(kennung: String): Ziel? = entries.firstOrNull { it.kennung == kennung }
    }
}

/**
 * Ein Textfeld mit Fassungsgeschichte (F-02): jedes Verbessern merkt sich die vorige Fassung,
 * der Knopf wird zu „Zurücknehmen“, und erneutes Verbessern liefert eine neue Formulierung.
 */
data class Feld(
    val text: String = "",
    val fassungen: List<String> = emptyList(),
    val laeuft: Boolean = false,
) {
    val kannZurueck: Boolean get() = fassungen.isNotEmpty()
}

class AppViewModel(anwendung: Application) : AndroidViewModel(anwendung) {

    private val app = anwendung as ExperimenteApp
    private val ablage get() = app.ablage
    private val einstellungen get() = app.einstellungen

    private val aufnahme = MicRecorder(anwendung)
    private val ruettler: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        anwendung.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        anwendung.getSystemService(Vibrator::class.java)
    }

    val heute: LocalDate = LocalDate.now()

    // --- Erscheinung (F-26) --------------------------------------------------------------

    private val _erscheinung = MutableStateFlow(Erscheinung.aus(einstellungen.erscheinung))
    val erscheinung: StateFlow<Erscheinung> = _erscheinung.asStateFlow()

    fun setzeErscheinung(neue: Erscheinung) {
        _erscheinung.value = neue
        einstellungen.erscheinung = neue.schluessel
    }

    /** Der Schnellschalter auf B-10 und B-01: Hell → Dunkel → Wie das System → Hell. */
    fun naechsteErscheinung() = setzeErscheinung(_erscheinung.value.naechste())

    // --- F-41: Effekt-Stärke ---------------------------------------------------------------

    private val _effektstufe = MutableStateFlow(Effektstufe.aus(einstellungen.effektstufe))
    val effektstufe: StateFlow<Effektstufe> = _effektstufe.asStateFlow()

    /** Die Wahl wirkt sofort, ohne Neustart, auf allen Bildschirmen. */
    fun setzeEffektstufe(neue: Effektstufe) {
        _effektstufe.value = neue
        einstellungen.effektstufe = neue.schluessel
    }

    // --- Navigation ----------------------------------------------------------------------

    /** **A-21:** Beim Starten der App erscheint der Monitor, nicht „Heute“. */
    private val _ziel = MutableStateFlow(Ziel.MONITOR)
    val ziel: StateFlow<Ziel> = _ziel.asStateFlow()

    /**
     * Wohin der Zurück-Pfeil führt. Der Entwurf merkt sich den Bildschirm, von dem aus
     * B-02, B-03, B-08 oder B-09 geöffnet wurde — sonst landet Frank nach einem Gespräch
     * aus dem Monitor auf „Heute“.
     */
    private val _zurueckZu = MutableStateFlow(Ziel.MONITOR)

    private val _gespraechZu = MutableStateFlow<Long?>(null)
    val gespraechZu: StateFlow<Long?> = _gespraechZu.asStateFlow()

    fun gehe(ziel: Ziel) {
        if (ziel in MERKT_HERKUNFT) _zurueckZu.value = _ziel.value
        _ziel.value = ziel
    }

    /** Verdrahtung der `fuehrtZu`-Angaben aus dem Entwurf. */
    fun geheZuKennung(kennung: String) {
        Ziel.aus(kennung)?.let { gehe(it) }
    }

    fun zurueck() {
        _ziel.value = when (_ziel.value) {
            Ziel.SELBSTBILD -> Ziel.EINSTELLUNGEN
            else -> _zurueckZu.value
        }
    }

    /**
     * F-27 — Wischen zwischen den **sechs** Hauptbildschirmen. An den Enden passiert nichts,
     * es wird nicht umlaufend gewechselt. Andere Bildschirme reagieren nicht auf Wischen.
     */
    fun wische(nachLinks: Boolean) {
        val reihe = Ziel.hauptreihe
        val jetzt = reihe.indexOf(_ziel.value)
        if (jetzt < 0) return
        val neu = if (nachLinks) jetzt + 1 else jetzt - 1
        if (neu in reihe.indices) _ziel.value = reihe[neu]
    }

    // --- Zustand von B-01 ----------------------------------------------------------------

    private val _tagZustand = MutableStateFlow(TagZustand.LEER)
    val tagZustand: StateFlow<TagZustand> = _tagZustand.asStateFlow()

    private val _abendOffen = MutableStateFlow(false)

    fun zeigeAbend() {
        _abendOffen.value = true
        _zurueckZu.value = Ziel.MONITOR
        _ziel.value = Ziel.HEUTE
    }

    fun zeigeMorgen() {
        _abendOffen.value = false
        viewModelScope.launch { bestimmeZustand() }
    }

    // --- Meldungen und Wartezustand ------------------------------------------------------

    private val _meldung = MutableStateFlow<String?>(null)
    val meldung: StateFlow<String?> = _meldung.asStateFlow()

    private val _wartet = MutableStateFlow<String?>(null)
    val wartet: StateFlow<String?> = _wartet.asStateFlow()

    fun schliesseMeldung() {
        _meldung.value = null
    }

    // --- Felder ---------------------------------------------------------------------------

    private val _lageFeld = MutableStateFlow(Feld())
    val lageFeld: StateFlow<Feld> = _lageFeld.asStateFlow()

    private val _zielFeld = MutableStateFlow(Feld())
    val zielFeld: StateFlow<Feld> = _zielFeld.asStateFlow()

    private val _merkFeld = MutableStateFlow(Feld())
    val merkFeld: StateFlow<Feld> = _merkFeld.asStateFlow()

    private val _selbstbildFeld = MutableStateFlow(Feld())
    val selbstbildFeld: StateFlow<Feld> = _selbstbildFeld.asStateFlow()

    private val _auswertungsFeld = MutableStateFlow(Feld())
    val auswertungsFeld: StateFlow<Feld> = _auswertungsFeld.asStateFlow()

    /** Das Feld der Anlegefläche auf B-10 (F-35). */
    private val _anlegeFeld = MutableStateFlow(Feld())
    val anlegeFeld: StateFlow<Feld> = _anlegeFeld.asStateFlow()

    // --- Datenströme ----------------------------------------------------------------------

    val lage = ablage.beobachteLage(heute).alsZustand(null)
    val vorschlaege = ablage.beobachteVorschlaege(heute).alsZustand(emptyList<Suggestion>())

    /** B-10, Abschnitt „Läuft“ — höchstens drei (F-34). */
    val laufende = ablage.beobachteLaufende().alsZustand(emptyList<Experiment>())

    /** B-10, Abschnitt „Steht an“ — beliebig viele (F-34). */
    val anstehende = ablage.beobachteAnstehende().alsZustand(emptyList<Experiment>())

    val ziele = ablage.beobachteZiele().alsZustand(emptyList<Goal>())
    val merkliste = ablage.beobachteMerkliste().alsZustand(emptyList<WatchlistItem>())
    val erkenntnisse = ablage.beobachteErkenntnisse().alsZustand(emptyList<Insight>())
    val logAusfuehrlich = ablage.beobachteLogAusfuehrlich().alsZustand(emptyList<LogDay>())
    val logVerdichtet = ablage.beobachteLogVerdichtet().alsZustand(emptyList<LogDay>())
    val selbstbild = ablage.beobachteSelbstbild().alsZustand(null)
    val auswertungenHeute = ablage.beobachteAuswertungen(heute).alsZustand(emptyList<Evaluation>())

    /** Die Aufgaben der laufenden Experimente — für die eine To-Do-Liste des Tages (F-07). */
    @Suppress("OPT_IN_USAGE")
    val aufgaben: StateFlow<List<Task>> = laufende
        .flatMapLatest { liste -> ablage.beobachteAufgaben(liste.map { it.id }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Der Gesprächsfaden zum gerade geöffneten Experiment (F-09). */
    @Suppress("OPT_IN_USAGE")
    val gespraech: StateFlow<List<ChatTurn>> = _gespraechZu
        .flatMapLatest { id -> if (id == null) kotlinx.coroutines.flow.flowOf(emptyList()) else ablage.beobachteGespraech(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun <T> kotlinx.coroutines.flow.Flow<T>.alsZustand(anfang: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), anfang)

    // --- Start ----------------------------------------------------------------------------

    /** Leitet den Zustand von B-01 aus dem ab, was gespeichert ist. */
    private suspend fun bestimmeZustand() {
        val hatLage = ablage.kontext(heute).heutigeLage?.isNotBlank() == true
        _tagZustand.value = when {
            _abendOffen.value && laufende.value.isNotEmpty() -> TagZustand.ABEND
            !hatLage -> TagZustand.LEER
            vorschlaege.value.isNotEmpty() -> TagZustand.VORSCHLAEGE
            else -> TagZustand.LAGE_STEHT
        }
        if (hatLage && _lageFeld.value.text.isBlank()) {
            _lageFeld.value = Feld(text = ablage.kontext(heute).heutigeLage.orEmpty())
        }
    }

    // --- F-34: der Monitor B-10 -----------------------------------------------------------

    private val _monitorLaedt = MutableStateFlow(false)

    /**
     * Der Zustand des Monitors leitet sich aus dem ab, was gespeichert ist — genau wie im
     * Entwurf (`zustand()`): leer → nur anstehend → läuft → voll.
     */
    val monitorZustand: StateFlow<MonitorZustand> = combine(
        _monitorLaedt, laufende, anstehende,
    ) { laedt, laufen, anstehen ->
        when {
            laedt -> MonitorZustand.LAEDT
            laufen.isEmpty() && anstehen.isEmpty() -> MonitorZustand.LEER
            laufen.isEmpty() -> MonitorZustand.NUR_ANSTEHEND
            laufen.size >= Ablage.MAX_LAUFEND -> MonitorZustand.VOLL
            else -> MonitorZustand.LAEUFT
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonitorZustand.LEER)

    /** Laufen bereits drei? Dann sind alle „Starten“-Knöpfe gesperrt (A-26). */
    val istVoll: StateFlow<Boolean> = laufende
        .map { it.size >= Ablage.MAX_LAUFEND }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * F-40 — welche Karte aufgeklappt ist. Es ist immer höchstens **eine** offen; das Öffnen
     * einer zweiten schließt die erste. Der Zustand überlebt einen Bildschirmwechsel, aber
     * nicht den App-Neustart — deshalb steht er hier und nicht in der Datenbank.
     */
    private val _aufgeklappt = MutableStateFlow<Long?>(null)
    val aufgeklappt: StateFlow<Long?> = _aufgeklappt.asStateFlow()

    fun klappeUm(experimentId: Long) {
        _aufgeklappt.value = if (_aufgeklappt.value == experimentId) null else experimentId
    }

    /** E-15 — welche Karte gerade funkelt (nach dem Start, 1200 ms lang). */
    private val _funkelt = MutableStateFlow<Long?>(null)
    val funkelt: StateFlow<Long?> = _funkelt.asStateFlow()

    /** E-16 — die Lichtblüte beim Abschließen, 1600 ms. */
    private val _bluete = MutableStateFlow(false)
    val bluete: StateFlow<Boolean> = _bluete.asStateFlow()

    /** F-35 — die Anlegefläche liegt über dem Monitor (`ANLEGEN`). */
    private val _anlegenOffen = MutableStateFlow(false)
    val anlegenOffen: StateFlow<Boolean> = _anlegenOffen.asStateFlow()

    fun oeffneAnlegen() {
        _anlegenOffen.value = true
        _anlegeFeld.value = Feld()
    }

    /** F-33 — Abbrechen schließt die Fläche, ohne zu speichern; der Text wird verworfen. */
    fun schliesseAnlegen() {
        _anlegenOffen.value = false
        _anlegeFeld.value = Feld()
    }

    fun anlegeFeldFluss() = _anlegeFeld

    /** F-35 — speichern. Ohne Netz wird trotzdem gespeichert; nichts geht verloren. */
    fun legeEigenesImMonitorAn() {
        val text = _anlegeFeld.value.text
        if (text.isBlank()) return
        _anlegenOffen.value = false
        viewModelScope.launch {
            _wartet.value = "Ich ordne das ein …"
            try {
                val id = ablage.legeEigenesImMonitorAn(text)
                _anlegeFeld.value = Feld()
                gehe(Ziel.MONITOR)
                lassFunkeln(id)
                _meldung.value = "Steht jetzt unter „Steht an“."
            } finally {
                _wartet.value = null
            }
        }
    }

    // --- F-36 / F-37 / F-38 / F-39 --------------------------------------------------------

    /** F-36 — einen KI-Vorschlag in den Monitor übernehmen. Übernehmen ist nicht starten. */
    fun uebernimm(vorschlag: Suggestion) {
        viewModelScope.launch {
            val id = ablage.uebernimm(vorschlag.id)
            _meldung.value = if (id == null) {
                "„${vorschlag.title}“ steht schon im Monitor."
            } else {
                "„${vorschlag.title}“ steht jetzt im Monitor unter „Steht an“."
            }
        }
    }

    /** F-37 — ein anstehendes Experiment starten. */
    fun starteAnstehendes(experiment: Experiment) {
        viewModelScope.launch {
            if (ablage.starte(experiment.id, heute)) {
                lassFunkeln(experiment.id)
                ruettleAufsteigend()
            } else {
                _meldung.value = DREI_LAUFEN
            }
        }
    }

    /** F-06 — „Jetzt starten“ auf einer Vorschlagskarte: übernehmen und starten in einem Zug. */
    fun starteSofort(vorschlag: Suggestion) {
        viewModelScope.launch {
            val id = ablage.starteSofort(vorschlag.id, heute)
            if (id == null) {
                _meldung.value = DREI_LAUFEN
            } else {
                gehe(Ziel.MONITOR)
                lassFunkeln(id)
                ruettleAufsteigend()
            }
        }
    }

    /** F-38 — die Reihenfolge unter „Steht an“ ändern. */
    fun sortiere(experiment: Experiment, nachIndex: Int) {
        viewModelScope.launch {
            ablage.sortiere(experiment.id, nachIndex)
            _meldung.value = "Reihenfolge geändert."
        }
    }

    /** F-39 — ein anstehendes Experiment aus dem Monitor nehmen. */
    fun nimmAusMonitor(experiment: Experiment, aufMerkliste: Boolean) {
        viewModelScope.launch {
            ablage.nimmAusMonitor(experiment.id, aufMerkliste)
            _meldung.value = if (aufMerkliste) {
                "„${experiment.title}“ ist wieder auf der Merkliste."
            } else {
                "„${experiment.title}“ ist gelöscht."
            }
        }
    }

    /** E-15 · M-84 — die Funken laufen 1200 ms und nur auf ausdrückliche Handlung. */
    private fun lassFunkeln(experimentId: Long) {
        _funkelt.value = experimentId
        viewModelScope.launch {
            kotlinx.coroutines.delay(Bewegung.FUNKEN.toLong())
            if (_funkelt.value == experimentId) _funkelt.value = null
        }
    }

    // --- F-01: Aufnahme -------------------------------------------------------------------

    private val _nimmtAuf = MutableStateFlow(false)
    val nimmtAuf: StateFlow<Boolean> = _nimmtAuf.asStateFlow()

    /** Die laufende Aufnahmedauer in Sekunden — der Entwurf zeigt sie als „00:07“. */
    private val _aufnahmeSekunden = MutableStateFlow(0)
    val aufnahmeSekunden: StateFlow<Int> = _aufnahmeSekunden.asStateFlow()

    private var uhr: kotlinx.coroutines.Job? = null

    /** Welches Feld die laufende Aufnahme füllt. */
    private var aufnahmeZiel: MutableStateFlow<Feld>? = null
    private var aufnahmeNachher: ((String) -> Unit)? = null

    /**
     * F-01 Schritte 2 bis 6: Aufnahme starten, kurze Vibration, beim zweiten Druck beenden,
     * Vorfilter, Transkription, Halluzinationsfilter.
     */
    fun sprechknopf(
        feld: MutableStateFlow<Feld>? = _lageFeld,
        nachher: ((String) -> Unit)? = null,
    ) {
        if (_nimmtAuf.value) {
            beendeAufnahme()
            return
        }
        val schluessel = einstellungen.groqSchluessel
        if (schluessel.isBlank()) {
            _meldung.value = "Für die Spracherkennung fehlt der Groq-Schlüssel. Er steht in den Einstellungen."
            return
        }
        aufnahmeZiel = feld
        aufnahmeNachher = nachher
        val ging = aufnahme.start(viewModelScope)
        if (!ging) {
            _meldung.value = "Ohne Mikrofon kann ich dich nicht hören. Die Erlaubnis steht in den Systemeinstellungen."
            return
        }
        _nimmtAuf.value = true
        if (_ziel.value == Ziel.HEUTE && _tagZustand.value == TagZustand.LEER) {
            _tagZustand.value = TagZustand.AUFNAHME
        }
        // Die Sekundenanzeige laeuft, solange aufgenommen wird.
        _aufnahmeSekunden.value = 0
        uhr?.cancel()
        uhr = viewModelScope.launch {
            while (true) {
                delay(1_000)
                _aufnahmeSekunden.value += 1
            }
        }
        ruettleDoppelt() // M-03: doppelte Vibration bei Aufnahmebeginn
    }

    private fun beendeAufnahme() {
        _nimmtAuf.value = false
        uhr?.cancel()
        uhr = null
        ruettleDoppelt() // M-03: doppelte Vibration bei Aufnahmeende
        val feld = aufnahmeZiel
        val nachher = aufnahmeNachher
        viewModelScope.launch {
            val wav = aufnahme.stop()
            if (wav == null || wav.isEmpty()) {
                _meldung.value = "Da war nichts zu hören."
                bestimmeZustand()
                return@launch
            }
            _wartet.value = "Ich höre zu …"
            try {
                val schreiber = GroqTranscriber(einstellungen.groqSchluessel)
                val text = schreiber.transcribe(wav)
                schreiber.shutdown()
                if (text.isBlank()) {
                    _meldung.value = "Da war nichts zu hören."
                } else {
                    feld?.value = Feld(text = text)
                    nachher?.invoke(text)
                    if (feld === _lageFeld) {
                        ablage.speichereLage(text, heute)
                        _tagZustand.value = TagZustand.LAGE_STEHT
                    }
                }
            } catch (fehler: Exception) {
                _meldung.value = fehler.freundlich()
            } finally {
                _wartet.value = null
                if (_tagZustand.value == TagZustand.AUFNAHME) bestimmeZustand()
            }
        }
    }

    // --- E-23: Haptik ----------------------------------------------------------------------
    // Feste Muster aus UI-Spec §7.3. Auf der Stufe *Aus* schweigt das Gerät.

    private fun ruettle(muster: LongArray = longArrayOf(0, 30), staerke: IntArray? = null) {
        if (_effektstufe.value == Effektstufe.AUS) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val wirkung = if (staerke == null) {
            VibrationEffect.createWaveform(muster, -1)
        } else {
            VibrationEffect.createWaveform(muster, staerke, -1)
        }
        ruettler?.vibrate(wirkung)
    }

    /** Kurz (10 ms) beim Abhaken. */
    private fun ruettleKurz() = ruettle(longArrayOf(0, 10))

    /** Doppelt beim Aufnahmebeginn und -ende (M-03). */
    private fun ruettleDoppelt() = ruettle(longArrayOf(0, 25, 60, 25))

    /** Aufsteigend beim Starten eines Experiments. */
    private fun ruettleAufsteigend() =
        ruettle(longArrayOf(0, 20, 40, 30, 40, 45), intArrayOf(0, 70, 0, 140, 0, 220))

    /** Lang-weich beim Abschließen. */
    private fun ruettleLangWeich() = ruettle(longArrayOf(0, 220), intArrayOf(0, 90))

    /** Fehler: zwei harte Stöße. */
    private fun ruettleFehler() =
        ruettle(longArrayOf(0, 60, 80, 60), intArrayOf(0, 255, 0, 255))

    // --- F-02 -----------------------------------------------------------------------------

    /** F-02 — Text mit KI verbessern. Die vorige Fassung wird gemerkt. */
    fun verbessere(feld: MutableStateFlow<Feld>) {
        val jetzt = feld.value
        if (jetzt.text.isBlank() || jetzt.laeuft) return
        feld.value = jetzt.copy(laeuft = true)
        viewModelScope.launch {
            try {
                val neu = ablage.verbessere(jetzt.text, jetzt.fassungen + jetzt.text)
                feld.value = Feld(text = neu, fassungen = jetzt.fassungen + jetzt.text)
            } catch (fehler: Exception) {
                // Der ursprüngliche Text bleibt unangetastet.
                feld.value = jetzt
                _meldung.value = "Der Text konnte nicht verbessert werden."
            }
        }
    }

    /** F-02 Schritt 3 — Zurücknehmen stellt die vorige Fassung wieder her. */
    fun nimmZurueck(feld: MutableStateFlow<Feld>) {
        val jetzt = feld.value
        val vorige = jetzt.fassungen.lastOrNull() ?: return
        feld.value = Feld(text = vorige, fassungen = jetzt.fassungen.dropLast(1))
    }

    fun setzeText(feld: MutableStateFlow<Feld>, text: String) {
        feld.value = feld.value.copy(text = text)
    }

    // Zugriff für die Bildschirme — die Felder selbst bleiben gekapselt.
    fun lageFeldFluss() = _lageFeld
    fun zielFeldFluss() = _zielFeld
    fun merkFeldFluss() = _merkFeld
    fun selbstbildFeldFluss() = _selbstbildFeld
    fun auswertungsFeldFluss() = _auswertungsFeld

    // --- F-03 / F-04 ----------------------------------------------------------------------

    /** „Weiter“ auf B-01: die Lage steht, jetzt die fünf Vorschläge (F-03). */
    fun weiter() {
        val text = _lageFeld.value.text
        if (text.isBlank()) return
        viewModelScope.launch {
            ablage.speichereLage(text, heute)
            // Laufen bereits drei, wird VORSCHLAEGE übersprungen: B-01 nennt den Grund und
            // verweist auf den Monitor (Funktions-Spec §4).
            if (laufende.value.size >= Ablage.MAX_LAUFEND) {
                _meldung.value = DREI_LAUFEN
                _tagZustand.value = TagZustand.LAGE_STEHT
                return@launch
            }
            _tagZustand.value = TagZustand.WARTET
            _wartet.value = "Ich sehe mir an, was ich über dich weiß …"
            try {
                if (ablage.erzeugeVorschlaege(heute)) {
                    _tagZustand.value = TagZustand.VORSCHLAEGE
                } else {
                    _tagZustand.value = TagZustand.LAGE_STEHT
                }
                ablage.schreibeLogbuchFort("Lage heute: $text", heute)
            } catch (fehler: Exception) {
                _tagZustand.value = TagZustand.LAGE_STEHT
                _meldung.value = fehler.freundlich()
            } finally {
                _wartet.value = null
            }
        }
    }

    /** F-28 — lieber tippen: der Zustand springt ohne Aufnahme auf `LAGE_STEHT`. */
    fun lieberTippen() {
        _tagZustand.value = TagZustand.LAGE_STEHT
    }

    /** F-04 — „Andere Vorschläge“. Bei Fehlschlag bleiben die alten fünf stehen. */
    fun andereVorschlaege() {
        viewModelScope.launch {
            _wartet.value = "Ich suche andere …"
            try {
                ablage.aktualisiereVorschlaege(heute)
            } catch (fehler: Exception) {
                _meldung.value = fehler.freundlich()
            } finally {
                _wartet.value = null
            }
        }
    }

    // --- F-05 / F-06 ----------------------------------------------------------------------

    fun merke(vorschlag: Suggestion) {
        viewModelScope.launch {
            if (ablage.merke(vorschlag)) {
                _meldung.value = "„${vorschlag.title}“ liegt auf der Merkliste."
            }
        }
    }

    /** F-05 — steht dieser Vorschlag schon im Monitor? Der Knopf zeigt dann den Zustand. */
    suspend fun stehtImMonitor(titel: String): Boolean = ablage.stehtImMonitor(titel)

    // --- F-08 -----------------------------------------------------------------------------

    fun hakenSchalten(aufgabe: Task) {
        ruettleKurz() // E-23: kurz (10 ms) beim Abhaken
        viewModelScope.launch { ablage.schalteHaken(aufgabe.id) }
    }

    /** Die heutigen Aufgaben eines Experiments — nach Tagesabschnitt gefiltert (F-07). */
    fun heutigeAufgaben(experiment: Experiment): List<Task> {
        val tagNr = ablage.tagNummer(experiment, heute)
        return aufgaben.value.filter { it.experimentId == experiment.id && it.dayIndex == tagNr }
            .sortedBy { it.order }
    }

    /** Das Etikett einer Laufkarte: „Tag 2 von 3“ — bei eintägigen schlicht „heute“. */
    fun tagText(experiment: Experiment): String =
        if (experiment.days <= 1) "heute" else "Tag ${ablage.tagNummer(experiment, heute)} von ${experiment.days}"

    /** Die Meta-Zeile eines Merklisten-Eintrags: „mittel · 2 Tage“. */
    fun merkMeta(eintrag: WatchlistItem): String {
        val tage = if (eintrag.days > 1) "${eintrag.days} Tage" else "1 Tag"
        return "${ablage.stufenwort(eintrag.level)} · $tage"
    }

    /** Die Meta-Zeile einer Vorschlagskarte: „leicht · 1 Tag“. */
    fun vorschlagsMeta(vorschlag: Suggestion): String {
        val tage = if (vorschlag.days > 1) "${vorschlag.days} Tage" else "1 Tag"
        return "${ablage.stufenwort(vorschlag.level)} · $tage"
    }

    /** Das Etikett einer anstehenden Karte: „fordernd · 3 Tage“. */
    fun dauerwort(experiment: Experiment): String {
        val tage = if (experiment.days > 1) "${experiment.days} Tage" else "1 Tag"
        return "${ablage.stufenwort(experiment.level)} · $tage"
    }

    fun stufenwort(experiment: Experiment): String = ablage.stufenwort(experiment.level)

    // --- F-09 -----------------------------------------------------------------------------

    fun oeffneGespraech(experiment: Experiment) {
        _gespraechZu.value = experiment.id
        gehe(Ziel.GESPRAECH)
    }

    /**
     * F-09 — eine Runde sprechen. Die Antwort wird **sofort vorgelesen**, ohne weiteren
     * Druck, mit der in B-08 gewählten Stimme.
     */
    fun sprich(text: String) {
        val id = _gespraechZu.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            _wartet.value = "Ich überlege …"
            try {
                val antwort = ablage.sprich(id, text)
                app.vorleser.lies(antwort) { _meldung.value = it }
            } catch (fehler: Exception) {
                _meldung.value = fehler.freundlich()
            } finally {
                _wartet.value = null
            }
        }
    }

    /** Der Sprechknopf auf B-02 füllt kein Feld, sondern schickt direkt ab. */
    fun gespraechAufnehmen() {
        val zwischenfeld = MutableStateFlow(Feld())
        sprechknopf(zwischenfeld) { text -> sprich(text) }
    }

    // --- F-10 / F-11 / F-13 ---------------------------------------------------------------

    /** Welches offene Experiment auf B-03 gerade ausgewertet wird. */
    private val _wertetAus = MutableStateFlow<Long?>(null)
    val wertetAus: StateFlow<Long?> = _wertetAus.asStateFlow()

    fun oeffneAuswertung(experiment: Experiment? = null) {
        _wertetAus.value = (experiment ?: laufende.value.firstOrNull())?.id
        _auswertungsFeld.value = Feld()
        _auswertungsZustand.value = AuswertungZustand.AUFNAHME
        gehe(Ziel.AUSWERTUNG)
    }

    /** Der Weg durch B-03: einsprechen → Text → warten → Antwort (Funktions-Spec §4). */
    private val _auswertungsZustand = MutableStateFlow(AuswertungZustand.AUFNAHME)
    val auswertungsZustand: StateFlow<AuswertungZustand> = _auswertungsZustand.asStateFlow()

    fun auswertungTippen() {
        _auswertungsZustand.value = AuswertungZustand.TEXT
    }

    /** Die zuletzt erzeugte KI-Auswertung, in Abschnitte geteilt fürs Mitlesen (E-21). */
    private val _einschaetzung = MutableStateFlow<List<String>>(emptyList())
    val einschaetzung: StateFlow<List<String>> = _einschaetzung.asStateFlow()

    /** E-21 — welcher Abschnitt gerade gesprochen wird; −1 heißt: es liest niemand. */
    private val _mitlese = MutableStateFlow(-1)
    val mitlese: StateFlow<Int> = _mitlese.asStateFlow()

    fun waehleAuswertung(experiment: Experiment) {
        _wertetAus.value = experiment.id
        _auswertungsFeld.value = Feld()
    }

    /** F-10 Schritt 3 und F-11: nach dem Eintrag läuft die KI-Auswertung. */
    fun werteAus() {
        val id = _wertetAus.value ?: return
        val text = _auswertungsFeld.value.text
        if (text.isBlank()) return
        viewModelScope.launch {
            _auswertungsZustand.value = AuswertungZustand.WARTET
            _wartet.value = "Ich denke darüber nach …"
            try {
                val kiText = ablage.werteAus(id, text, heute)
                _einschaetzung.value = kiText?.let(::inAbschnitte).orEmpty()
                _auswertungsZustand.value = AuswertungZustand.ANTWORT
                bestimmeZustand()
            } catch (fehler: Exception) {
                _auswertungsZustand.value = AuswertungZustand.TEXT
                _meldung.value = fehler.freundlich()
            } finally {
                _wartet.value = null
            }
        }
    }

    /** F-11 — die Auswertung noch einmal erzeugen lassen. */
    fun nochmalVersuchen() {
        _auswertungsZustand.value = AuswertungZustand.TEXT
        _mitlese.value = -1
    }

    /** F-30 — überspringen: das Experiment bleibt offen und kommt am nächsten Abend erneut. */
    fun ueberspringeAuswertung() {
        _auswertungsFeld.value = Feld()
        _einschaetzung.value = emptyList()
        zurueck()
    }

    /**
     * F-13 — abschließen. Die Karte verlässt den Monitor mit der Lichtblüte (`E-16`, M-93),
     * danach ist ein Platz der drei wieder frei.
     */
    fun schliesseAb() {
        viewModelScope.launch {
            _bluete.value = true
            ruettleLangWeich()
            kotlinx.coroutines.delay(Bewegung.BLUETE.toLong())
            _bluete.value = false
            _auswertungsFeld.value = Feld()
            _einschaetzung.value = emptyList()
            gehe(Ziel.MONITOR)
            _meldung.value = "Abgeschlossen. Der Eintrag steht im Logbuch."
        }
    }

    /** F-13, Weg „Nicht umgesetzt“. */
    fun nichtUmgesetzt(experiment: Experiment) {
        viewModelScope.launch {
            ablage.nichtUmgesetzt(experiment.id, _auswertungsFeld.value.text, heute)
            _meldung.value = "Nicht umgesetzt — der Eintrag steht im Logbuch."
            bestimmeZustand()
        }
    }

    /**
     * E-21 — den vorgelesenen Text in Abschnitte teilen, damit der gerade gesprochene
     * hervorgehoben werden kann. Ohne Zeitmarken des Anbieters wird gleichmäßig geschätzt.
     */
    private fun inAbschnitte(text: String): List<String> =
        Regex("(?<=[.!?])\\s+").split(text).filter { it.isNotBlank() }.map { "$it " }

    // --- F-12 -----------------------------------------------------------------------------

    val liestVor: StateFlow<Boolean> get() = app.vorleser.laeuft

    /**
     * F-12 — vorlesen. **E-21:** der gerade gesprochene Abschnitt wird hervorgehoben; ohne
     * Zeitmarken des Anbieters wird gleichmäßig über die Dauer geschätzt.
     */
    fun lies(text: String) {
        if (app.vorleser.laeuft.value) {
            app.vorleser.umschalten()
            _mitlese.value = -1
            return
        }
        app.vorleser.lies(text) { _meldung.value = it }
        val abschnitte = _einschaetzung.value
        if (abschnitte.isEmpty()) return
        viewModelScope.launch {
            _mitlese.value = 0
            while (_mitlese.value in abschnitte.indices && app.vorleser.laeuft.value) {
                delay(1_800)
                val naechster = _mitlese.value + 1
                _mitlese.value = if (naechster >= abschnitte.size) -1 else naechster
            }
            _mitlese.value = -1
        }
    }

    // --- F-16 -----------------------------------------------------------------------------

    fun aendereLogtag(tag: LogDay, neuerText: String) {
        viewModelScope.launch { ablage.aendereLogtag(tag, neuerText) }
    }

    fun loescheLogtag(tag: LogDay) {
        viewModelScope.launch { ablage.loescheLogtag(tag) }
    }

    // --- F-18 / F-19 ----------------------------------------------------------------------

    fun legeEigenesAn() {
        val text = _merkFeld.value.text
        if (text.isBlank()) return
        viewModelScope.launch {
            _wartet.value = "Ich ordne das ein …"
            try {
                ablage.legeEigenesAn(text)
                _merkFeld.value = Feld()
            } catch (fehler: Exception) {
                _meldung.value = fehler.freundlich()
            } finally {
                _wartet.value = null
            }
        }
    }

    fun loescheMerkliste(eintrag: WatchlistItem) {
        viewModelScope.launch { ablage.loescheMerkliste(eintrag) }
    }

    // --- F-20 / F-21 ----------------------------------------------------------------------

    /** F-20 Schritt 2: nach dem Speichern steht der Sprechknopf sofort wieder bereit. */
    fun legeZielAn() {
        val text = _zielFeld.value.text
        if (text.isBlank()) return
        viewModelScope.launch {
            ablage.legeZielAn(text)
            _zielFeld.value = Feld()
        }
    }

    fun aendereZiel(ziel: Goal, text: String) {
        viewModelScope.launch { ablage.aendereZiel(ziel, text) }
    }

    fun loescheZiel(ziel: Goal) {
        viewModelScope.launch { ablage.loescheZiel(ziel) }
    }

    fun speichereSelbstbild() {
        viewModelScope.launch { ablage.speichereSelbstbild(_selbstbildFeld.value.text) }
    }

    fun ladeSelbstbildInsFeld() {
        if (_selbstbildFeld.value.text.isBlank()) {
            _selbstbildFeld.value = Feld(text = selbstbild.value?.text.orEmpty())
        }
    }

    // --- F-22 bis F-25 --------------------------------------------------------------------

    fun einstellungenZugriff() = einstellungen

    private val _geraetecode = MutableStateFlow<Geraetecode?>(null)
    val geraetecode: StateFlow<Geraetecode?> = _geraetecode.asStateFlow()

    private val _angemeldet = MutableStateFlow(app.codex.istAngemeldet)
    val angemeldet: StateFlow<Boolean> = _angemeldet.asStateFlow()

    val angemeldetAls: String? get() = app.codex.email

    /** F-24 — Geräteanmeldung: Benutzercode anzeigen, Seite öffnen, auf Bestätigung warten. */
    fun meldeAn() {
        viewModelScope.launch {
            try {
                app.codex.anmelden { code -> _geraetecode.value = code }
                _angemeldet.value = true
                _geraetecode.value = null
                _meldung.value = "Angemeldet." + (app.codex.email?.let { " ($it)" } ?: "")
            } catch (fehler: Exception) {
                _geraetecode.value = null
                _meldung.value = fehler.freundlich()
            }
        }
    }

    fun meldeAb() {
        app.codex.abmelden()
        _angemeldet.value = false
    }

    /** F-25 — nach jeder Änderung an den Erinnerungen die Weckzeiten neu setzen. */
    fun erinnerungenNeuSetzen() {
        de.frank.experimente.notify.Erinnerungen.setzeAlle(getApplication(), einstellungen)
    }

    /**
     * F-23 Schritt 3 — „Meine Stimme": eine Stimmprobe aufnehmen und bei Alibaba DashScope
     * registrieren (Verfahren aus `QwenVoiceEnrollment`). Der zweite Druck beendet die
     * Aufnahme und schickt sie ab.
     */
    fun nimmStimmeAuf() {
        if (_nimmtAuf.value) {
            _nimmtAuf.value = false
            uhr?.cancel()
            ruettleDoppelt()
            viewModelScope.launch {
                val wav = aufnahme.stop()
                if (wav == null || wav.isEmpty()) {
                    _meldung.value = "Da war nichts zu hören."
                    return@launch
                }
                val schluessel = einstellungen.qwenSchluessel
                if (schluessel.isBlank()) {
                    _meldung.value = "Für diese Stimme fehlt der Schlüssel."
                    return@launch
                }
                _wartet.value = "Ich richte deine Stimme ein …"
                val registrierung = de.frank.experimente.tts.QwenVoiceEnrollment()
                try {
                    val kennung = registrierung.create(schluessel, "frank", wav)
                    einstellungen.stimmeQwen = kennung
                    _meldung.value = "Stimmprobe aufgenommen. Alibaba erzeugt daraus deine Stimme."
                } catch (fehler: Exception) {
                    _meldung.value = fehler.freundlich()
                } finally {
                    registrierung.shutdown()
                    _wartet.value = null
                }
            }
            return
        }
        if (!aufnahme.start(viewModelScope)) {
            _meldung.value = "Ohne Mikrofon kann ich dich nicht hören."
            return
        }
        _nimmtAuf.value = true
        ruettleDoppelt()
    }

    /** F-23 Schritt 6 — der Probe-Knopf liest einen Beispielsatz vor. */
    fun hoerProbe() {
        app.vorleser.lies("So klinge ich, wenn ich dir etwas vorlese.") { _meldung.value = it }
    }

    // --- Lebenszyklus ---------------------------------------------------------------------

    /**
     * §6 — Geht die App in den Hintergrund, wird eine laufende Aufnahme beendet und
     * verworfen, eine laufende Wiedergabe gestoppt. Laufende KI-Anfragen laufen zu Ende.
     */
    fun beimVerlassen() {
        if (_nimmtAuf.value) {
            _nimmtAuf.value = false
            viewModelScope.launch { aufnahme.stop() }
        }
        app.vorleser.halteAn()
    }

    override fun onCleared() {
        super.onCleared()
        aufnahme.release()
    }

    private companion object {
        /** Der Satz aus UI-Spec §8 — wörtlich, an jeder Stelle gleich. */
        const val DREI_LAUFEN =
            "Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues beginnst."

        /** Diese Bildschirme merken sich, von wo aus sie geöffnet wurden. */
        val MERKT_HERKUNFT = setOf(Ziel.GESPRAECH, Ziel.AUSWERTUNG, Ziel.EINSTELLUNGEN, Ziel.SELBSTBILD)
    }

    /**
     * Aus einem Fehler wird ein Satz, den man lesen kann — die Wortlaute stehen in
     * UI-Spec §8. Dazu das Fehler-Muster aus `E-23`: zwei harte Stöße.
     */
    private fun Exception.freundlich(): String {
        ruettleFehler()
        return when {
            this is CodexFehler && art == FehlerArt.ANMELDUNG ->
                "Deine Anmeldung ist abgelaufen. In den Einstellungen kannst du sie erneuern."
            this is CodexFehler && art == FehlerArt.KONTINGENT ->
                "Dein Kontingent ist erschöpft. Versuch es später noch einmal."
            this is CodexFehler -> "Dafür brauche ich Netz."
            else -> message ?: "Das hat nicht geklappt."
        }
    }

    // --- Start ------------------------------------------------------------------------------

    /**
     * Der Startlauf steht bewusst **am Ende der Klasse**.
     *
     * Kotlin führt Eigenschaften und `init`-Blöcke in Deklarationsreihenfolge aus. Stand der
     * Block weiter oben, las er `_monitorLaedt`, das erst darunter angelegt wurde — und die
     * App stürzte beim Start mit einer `NullPointerException` ab. Ganz unten kann das
     * konstruktionsbedingt nicht mehr passieren: dort ist jede Eigenschaft fertig.
     *
     * Wer hier etwas ergänzt, lässt den Block unten stehen.
     */
    init {
        viewModelScope.launch {
            // §6: Beim ersten Öffnen an einem neuen Kalendertag läuft F-15, BEVOR etwas
            // anderes angezeigt wird. Solange zeigt der Monitor die Schimmer-Skelette (E-13).
            if (ablage.verdichtungFaellig(heute)) {
                _monitorLaedt.value = true
                _wartet.value = "Ich ordne die älteren Tage ein …"
                runCatching { ablage.verdichteFaellige(heute) }
                _wartet.value = null
                _monitorLaedt.value = false
            }
            bestimmeZustand()
        }
    }
}
