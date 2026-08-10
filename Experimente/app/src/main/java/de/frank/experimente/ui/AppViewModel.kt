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
import de.frank.experimente.ui.theme.Erscheinung
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Der Tag auf B-01 (Funktions-Spec §4). */
enum class TagZustand { LEER, AUFNAHME, LAGE_STEHT, VORSCHLAEGE, LAEUFT, ABEND }

/** Welcher Bildschirm gerade zu sehen ist. */
enum class Ziel(val kennung: String) {
    HEUTE("B-01"),
    GESPRAECH("B-02"),
    AUSWERTUNG("B-03"),
    ZIELE("B-04"),
    MERKLISTE("B-05"),
    ERKENNTNISSE("B-06"),
    LOGBUCH("B-07"),
    EINSTELLUNGEN("B-08"),
    SELBSTBILD("B-09"),
    ;

    companion object {
        /** Die fünf Hauptbildschirme in der Reihenfolge der unteren Leiste (F-27). */
        val hauptreihe = listOf(HEUTE, ZIELE, MERKLISTE, ERKENNTNISSE, LOGBUCH)

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

    /** Der Schnellschalter auf B-01: Hell → Dunkel → Wie das System → Hell. */
    fun naechsteErscheinung() = setzeErscheinung(_erscheinung.value.naechste())

    // --- Navigation ----------------------------------------------------------------------

    private val _ziel = MutableStateFlow(Ziel.HEUTE)
    val ziel: StateFlow<Ziel> = _ziel.asStateFlow()

    private val _gespraechZu = MutableStateFlow<Long?>(null)
    val gespraechZu: StateFlow<Long?> = _gespraechZu.asStateFlow()

    fun gehe(ziel: Ziel) {
        _ziel.value = ziel
    }

    /** Verdrahtung der `fuehrtZu`-Angaben aus der Messung. */
    fun geheZuKennung(kennung: String) {
        Ziel.aus(kennung)?.let { gehe(it) }
    }

    fun zurueck() {
        _ziel.value = when (_ziel.value) {
            Ziel.GESPRAECH, Ziel.AUSWERTUNG -> Ziel.HEUTE
            Ziel.SELBSTBILD -> Ziel.EINSTELLUNGEN
            Ziel.EINSTELLUNGEN -> Ziel.HEUTE
            else -> Ziel.HEUTE
        }
    }

    /**
     * F-27 — Wischen zwischen den fünf Hauptbildschirmen. An den Enden passiert nichts,
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
        _ziel.value = Ziel.HEUTE
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

    // --- Datenströme ----------------------------------------------------------------------

    val lage = ablage.beobachteLage(heute).alsZustand(null)
    val vorschlaege = ablage.beobachteVorschlaege(heute).alsZustand(emptyList<Suggestion>())
    val offene = ablage.beobachteOffene().alsZustand(emptyList<Experiment>())
    val ziele = ablage.beobachteZiele().alsZustand(emptyList<Goal>())
    val merkliste = ablage.beobachteMerkliste().alsZustand(emptyList<WatchlistItem>())
    val erkenntnisse = ablage.beobachteErkenntnisse().alsZustand(emptyList<Insight>())
    val logAusfuehrlich = ablage.beobachteLogAusfuehrlich().alsZustand(emptyList<LogDay>())
    val logVerdichtet = ablage.beobachteLogVerdichtet().alsZustand(emptyList<LogDay>())
    val selbstbild = ablage.beobachteSelbstbild().alsZustand(null)
    val auswertungenHeute = ablage.beobachteAuswertungen(heute).alsZustand(emptyList<Evaluation>())

    /** Die Aufgaben der offenen Experimente — für die eine To-Do-Liste des Tages (F-07). */
    @Suppress("OPT_IN_USAGE")
    val aufgaben: StateFlow<List<Task>> = offene
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

    init {
        viewModelScope.launch {
            // §6: Beim ersten Öffnen an einem neuen Kalendertag läuft F-15, BEVOR etwas
            // anderes angezeigt wird. Dauert es länger, zeigt sich der Wartezustand.
            if (ablage.verdichtungFaellig(heute)) {
                _wartet.value = "Ich ordne die älteren Tage ein …"
                runCatching { ablage.verdichteFaellige(heute) }
                _wartet.value = null
            }
            bestimmeZustand()
        }
    }

    /** Leitet den Zustand von B-01 aus dem ab, was gespeichert ist. */
    private suspend fun bestimmeZustand() {
        val hatLage = ablage.kontext(heute).heutigeLage?.isNotBlank() == true
        val offeneZahl = offene.value.size
        _tagZustand.value = when {
            _abendOffen.value && offeneZahl > 0 -> TagZustand.ABEND
            offeneZahl > 0 -> TagZustand.LAEUFT
            !hatLage -> TagZustand.LEER
            vorschlaege.value.isNotEmpty() -> TagZustand.VORSCHLAEGE
            else -> TagZustand.LAGE_STEHT
        }
        if (hatLage && _lageFeld.value.text.isBlank()) {
            _lageFeld.value = Feld(text = ablage.kontext(heute).heutigeLage.orEmpty())
        }
    }

    // --- F-01: Aufnahme -------------------------------------------------------------------

    private val _nimmtAuf = MutableStateFlow(false)
    val nimmtAuf: StateFlow<Boolean> = _nimmtAuf.asStateFlow()

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
        ruettle() // M-03: Vibration bei Aufnahmebeginn
    }

    private fun beendeAufnahme() {
        _nimmtAuf.value = false
        ruettle() // M-03: Vibration bei Aufnahmeende
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

    private fun ruettle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ruettler?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

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
            if (offene.value.size >= 3) {
                _meldung.value = "Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues beginnst."
                _tagZustand.value = TagZustand.LAEUFT
                return@launch
            }
            _wartet.value = "Ich denke nach …"
            try {
                if (ablage.erzeugeVorschlaege(heute)) _tagZustand.value = TagZustand.VORSCHLAEGE
                ablage.schreibeLogbuchFort("Lage heute: $text", heute)
            } catch (fehler: Exception) {
                _meldung.value = fehler.freundlich()
            } finally {
                _wartet.value = null
            }
        }
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

    fun starte(vorschlag: Suggestion) {
        viewModelScope.launch {
            val id = ablage.starte(vorschlag.id, heute)
            if (id == null) {
                _meldung.value = "Drei Experimente laufen schon. Schließ eines ab, bevor du ein neues beginnst."
            } else {
                _tagZustand.value = TagZustand.LAEUFT
            }
        }
    }

    // --- F-08 -----------------------------------------------------------------------------

    fun hakenSchalten(aufgabe: Task) {
        viewModelScope.launch { ablage.schalteHaken(aufgabe.id) }
    }

    /** Die heutigen Aufgaben eines Experiments — nach Tagesabschnitt gefiltert (F-07). */
    fun heutigeAufgaben(experiment: Experiment): List<Task> {
        val tagNr = ablage.tagNummer(experiment, heute)
        return aufgaben.value.filter { it.experimentId == experiment.id && it.dayIndex == tagNr }
            .sortedBy { it.order }
    }

    fun tagText(experiment: Experiment): String =
        if (experiment.days <= 1) "heute" else "Tag ${ablage.tagNummer(experiment, heute)} von ${experiment.days}"

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

    fun oeffneAuswertung() {
        _wertetAus.value = offene.value.firstOrNull()?.id
        gehe(Ziel.AUSWERTUNG)
    }

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
            _wartet.value = "Ich sehe mir das an …"
            try {
                ablage.werteAus(id, text, heute)
                _auswertungsFeld.value = Feld()
                // Der Reihe nach: das nächste offene Experiment.
                _wertetAus.value = ablage.beobachteOffene().let { offene.value.firstOrNull { it.id != id }?.id }
                bestimmeZustand()
            } catch (fehler: Exception) {
                _meldung.value = fehler.freundlich()
            } finally {
                _wartet.value = null
            }
        }
    }

    /** F-13, Weg „Nicht umgesetzt“. */
    fun nichtUmgesetzt(experiment: Experiment) {
        viewModelScope.launch {
            ablage.nichtUmgesetzt(experiment.id, _auswertungsFeld.value.text, heute)
            _meldung.value = "„${experiment.title}“ liegt wieder auf der Merkliste."
            bestimmeZustand()
        }
    }

    // --- F-12 -----------------------------------------------------------------------------

    val liestVor: StateFlow<Boolean> get() = app.vorleser.laeuft

    fun lies(text: String) {
        if (app.vorleser.laeuft.value) app.vorleser.umschalten() else app.vorleser.lies(text) { _meldung.value = it }
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

    /** Aus einem Fehler wird ein Satz, den man lesen kann. */
    private fun Exception.freundlich(): String = when {
        this is CodexFehler && art == FehlerArt.ANMELDUNG ->
            "Deine Anmeldung ist abgelaufen. In den Einstellungen kannst du sie erneuern."
        this is CodexFehler && art == FehlerArt.KONTINGENT ->
            "Dein Kontingent ist erschöpft. Versuch es später noch einmal."
        this is CodexFehler -> "Dafür brauche ich Netz."
        else -> message ?: "Das hat nicht geklappt."
    }
}
