package de.frank.genialeideen.ui

import android.app.Application
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.genialeideen.audio.Diktat
import de.frank.genialeideen.audio.GroqTranscriber
import de.frank.genialeideen.audio.MicRecorder
import de.frank.genialeideen.auth.AuthErrorKind
import de.frank.genialeideen.auth.ChatTurn
import de.frank.genialeideen.auth.CodexAuthException
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.DeviceAuthInfo
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.backup.Sicherung
import de.frank.genialeideen.backup.SicherungsVorschau
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.data.local.Kategorieart
import de.frank.genialeideen.data.local.NachrichtEntity
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.observability.IdeenCrashHandler
import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.speech.VorleseStand
import de.frank.genialeideen.text.UmlautKorrektur
import de.frank.genialeideen.tts.ClonedVoice
import de.frank.genialeideen.tts.QwenVoiceDirectory
import de.frank.genialeideen.tts.QwenVoiceEnrollment
import de.frank.genialeideen.tts.TtsProvider
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Kurze Rückmeldungen, die als Streifen erscheinen (Baustein L). */
data class Meldung(
    val text: String,
    val istFehler: Boolean = false,
    val wiederholen: (() -> Unit)? = null,
    val zuEinstellungen: Boolean = false,
)

data class AufnahmeStand(
    val laeuft: Boolean = false,
    val wirdUebertragen: Boolean = false,
    val seit: Long = 0L,
    /** Echte Lautstärke von 0 bis 1 für die Pegel-Anzeige (N.7). */
    val pegel: Float = 0f,
)

/** Was vor der Korrektur dastand — damit „Rückgängig" es zurückholen kann (Baustein O.4). */
data class KorrekturStand(
    val original: String,
    val korrigiert: String,
)

data class KiStand(
    val antwortet: Boolean = false,
    val teilAntwort: String = "",
    val fehler: String? = null,
)

data class AnmeldeStand(
    val laeuft: Boolean = false,
    val code: String? = null,
    val adresse: String? = null,
    val gueltigBis: Long = 0L,
)

class IdeenViewModel(
    application: Application,
    private val container: AppContainer,
) : AndroidViewModel(application) {

    private val repository = container.ideenRepository
    val settings = container.settings
    private val vorleser = container.vorleser
    private val codex = container.codexAuthManager
    private val sicherung = Sicherung(application, container.database)
    private val recorder = MicRecorder(application)
    private val verzeichnis = QwenVoiceDirectory()
    private val stimmAnmeldung = QwenVoiceEnrollment()

    val vorleseStand: StateFlow<VorleseStand> = vorleser.stand

    private val _meldung = MutableStateFlow<Meldung?>(null)
    val meldung: StateFlow<Meldung?> = _meldung.asStateFlow()

    private val _aufnahme = MutableStateFlow(AufnahmeStand())
    val aufnahme: StateFlow<AufnahmeStand> = _aufnahme.asStateFlow()

    private val _titelLaeuft = MutableStateFlow(false)

    /** Solange true, sucht die KI gerade einen Titel für das frische Diktat. */
    val titelLaeuft: StateFlow<Boolean> = _titelLaeuft.asStateFlow()

    private val _korrektur = MutableStateFlow<KorrekturStand?>(null)

    /** Steht ein Wert drin, zeigt der Knopf „Rückgängig" statt „Korrigieren". */
    val korrektur: StateFlow<KorrekturStand?> = _korrektur.asStateFlow()

    private val _ki = MutableStateFlow(KiStand())
    val ki: StateFlow<KiStand> = _ki.asStateFlow()

    private val _anmeldung = MutableStateFlow(AnmeldeStand())
    val anmeldung: StateFlow<AnmeldeStand> = _anmeldung.asStateFlow()

    private val _laedt = MutableStateFlow(true)
    val laedt: StateFlow<Boolean> = _laedt.asStateFlow()

    private val _suchtext = MutableStateFlow("")
    val suchtext: StateFlow<String> = _suchtext.asStateFlow()

    private val _suchtreffer = MutableStateFlow<List<IdeeEntity>>(emptyList())
    val suchtreffer: StateFlow<List<IdeeEntity>> = _suchtreffer.asStateFlow()

    private val _theme = MutableStateFlow(settings.theme)
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _schriftgroesse = MutableStateFlow(settings.schriftgroesse)
    val schriftgroesse: StateFlow<Float> = _schriftgroesse.asStateFlow()

    private val _abgestuerzt = MutableStateFlow(IdeenCrashHandler.berichte(application).isNotEmpty())
    val abgestuerzt: StateFlow<Boolean> = _abgestuerzt.asStateFlow()

    private val _offeneIdee = MutableStateFlow<Long?>(null)

    /** Die Kategorie, nach der die Liste gerade gefiltert wird - null heisst alle. */
    private val _gewaehlteKategorie = MutableStateFlow<Long?>(null)
    val gewaehlteKategorie: StateFlow<Long?> = _gewaehlteKategorie.asStateFlow()

    private val _eigeneStimmen = MutableStateFlow<List<ClonedVoice>>(emptyList())
    val eigeneStimmen: StateFlow<List<ClonedVoice>> = _eigeneStimmen.asStateFlow()

    private val _stimmenLaden = MutableStateFlow(false)
    val stimmenLaden: StateFlow<Boolean> = _stimmenLaden.asStateFlow()

    private val _gewaehlteEigeneStimme = MutableStateFlow(settings.qwenTtsVoiceId)
    val gewaehlteEigeneStimme: StateFlow<String> = _gewaehlteEigeneStimme.asStateFlow()

    private val _gewaehlteStimme = MutableStateFlow(aktuelleStimmenId())
    val gewaehlteStimme: StateFlow<String> = _gewaehlteStimme.asStateFlow()

    private val _favoriten = MutableStateFlow(settings.favoriteTtsVoices)
    val favoriten: StateFlow<Set<String>> = _favoriten.asStateFlow()

    private val _stimmenFehler = MutableStateFlow<String?>(null)

    private var stimmenJob: Job? = null
    private var suchJob: Job? = null
    private var kiJob: Job? = null
    private var aufnahmeJob: Job? = null

    val alleIdeen: StateFlow<List<IdeeEntity>> = repository.alleIdeen()
        .map { liste -> _laedt.value = false; liste }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val offeneIdeen: StateFlow<List<IdeeEntity>> = alleIdeen
        .map { liste -> liste.filter { it.status == IdeenStatus.OFFEN.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val umgesetzteIdeen: StateFlow<List<IdeeEntity>> = alleIdeen
        .map { liste -> liste.filter { it.status == IdeenStatus.UMGESETZT.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Halbfertiges, das beim Verlassen des Erfassen-Bildschirms von allein gesichert wurde. */
    val entwuerfe: StateFlow<List<IdeeEntity>> = alleIdeen
        .map { liste ->
            liste.filter { it.status == IdeenStatus.ENTWURF.name }
                .sortedByDescending(IdeeEntity::geaendertAm)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val kategorien: StateFlow<List<KategorieEntity>> = repository.alleKategorien()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val letzteSuchanfragen: StateFlow<List<String>> = repository.letzteSuchanfragen()
        .map { liste -> liste.map { it.anfrage } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @Suppress("OPT_IN_USAGE")
    val nachrichten: StateFlow<List<NachrichtEntity>> = _offeneIdee
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repository.nachrichten(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @Suppress("OPT_IN_USAGE")
    val aktuelleIdee: StateFlow<IdeeEntity?> = _offeneIdee
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.beobachteIdee(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Die **eine** Stimmenliste über alle Engines (Kapitel 4.6). Sie wird neu gebaut, sobald
     * sich die eigenen Stimmen, die Favoriten oder ein Schlüssel ändern.
     */
    val stimmenEintraege: StateFlow<List<StimmenEintrag>> =
        combine(_eigeneStimmen, _favoriten, _stimmenFehler) { eigene, markierte, fehler ->
            Stimmenliste.baue(
                eigene = eigene,
                eigeneNamen = settings.qwenVoiceNames,
                favoriten = markierte,
                alibabaSchluessel = settings.qwenTtsApiKey.isNotBlank(),
                googleSchluessel = settings.googleTtsApiKey.isNotBlank(),
                eigeneFehler = fehler,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val chatGptVerbunden: Boolean get() = codex.isConnected
    val chatGptKonto: String? get() = codex.email

    init {
        ladeEigeneStimmen()
        // Der gemessene Pegel wandert in den Aufnahmestand, damit die Anzeige am Knopf
        // wirklich auf die Stimme reagiert und nichts simuliert.
        viewModelScope.launch {
            recorder.pegel.collect { wert ->
                if (_aufnahme.value.laeuft) _aufnahme.value = _aufnahme.value.copy(pegel = wert)
            }
        }
    }

    // ---- Ideen ----

    fun oeffne(id: Long?) {
        _offeneIdee.value = id
        IdeenCrashHandler.letzteAktion = if (id == null) "Liste" else "Idee geöffnet"
    }

    fun legeAn(
        titel: String,
        text: String,
        aufnahmePfad: String? = null,
        originalText: String? = null,
        kategorieId: Long,
    ) {
        viewModelScope.launch {
            // Auch der Ersatztitel bleibt bei drei Wörtern — sonst sprengt er die Liste.
            val name = titel.trim().ifBlank { ersatzTitel(text) }
            val entwurf = _entwurf.value
            if (entwurf != null) {
                repository.ausEntwurfUebernehmen(entwurf.id, name, text, kategorieId, originalText)
            } else {
                repository.lege(name, text.trim(), aufnahmePfad, originalText, kategorieId)
            }
            _entwurf.value = null
            zeige(Meldung("Idee gespeichert."))
        }
    }

    // ---- Entwürfe ----

    /** Der Entwurf, an dem der Erfassen-Bildschirm gerade hängt — null heisst: eine frische Idee. */
    private val _entwurf = MutableStateFlow<IdeeEntity?>(null)
    val entwurf: StateFlow<IdeeEntity?> = _entwurf.asStateFlow()

    /** Der Erfassen-Bildschirm beginnt leer. */
    fun beginneNeueIdee() {
        _entwurf.value = null
    }

    /** Einen gesicherten Entwurf zum Weiterschreiben öffnen. */
    fun oeffneEntwurf(idee: IdeeEntity) {
        _entwurf.value = idee
    }

    /**
     * Hält fest, was gerade dasteht — beim Zurückwischen, beim Zurück-Knopf und wenn die App in
     * den Hintergrund geht. Ohne Inhalt passiert nichts, ein leerer Entwurf verschwindet wieder.
     */
    fun sichereEntwurf(titel: String, text: String, kategorieId: Long?, originalText: String? = null) {
        val hatInhalt = titel.isNotBlank() || text.isNotBlank()
        val bisher = _entwurf.value
        if (!hatInhalt) {
            if (bisher != null) {
                _entwurf.value = null
                viewModelScope.launch { repository.loescheEntwurf(bisher.id) }
            }
            return
        }
        viewModelScope.launch {
            val id = repository.sichereEntwurf(bisher?.id, titel, text, kategorieId, originalText)
            _entwurf.value = repository.lade(id)
            if (bisher == null) zeige(Meldung("Als Entwurf gesichert."))
        }
    }

    fun verwirfEntwurf(idee: IdeeEntity) {
        viewModelScope.launch {
            repository.loescheEntwurf(idee.id)
            if (_entwurf.value?.id == idee.id) _entwurf.value = null
            zeige(Meldung("Entwurf verworfen."))
        }
    }

    // ---- Kategorien (Baustein P) ----

    fun waehleKategorie(id: Long?) {
        _gewaehlteKategorie.value = id
    }

    fun setzeKategorie(ideeId: Long, kategorieId: Long?) {
        viewModelScope.launch { repository.setzeKategorie(ideeId, kategorieId) }
    }

    /** Legt eine Kategorie nur nach ausdrücklicher Nutzeraktion an. */
    fun legeKategorieAn(name: String, art: Kategorieart, fertig: (Long?) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.legeKategorieAn(name, art)
            if (id == null) zeige(Meldung("Der Name der Kategorie war leer.", istFehler = true))
            fertig(id)
        }
    }

    fun benenneKategorieUm(id: Long, name: String) {
        viewModelScope.launch {
            if (!repository.benenneKategorieUm(id, name)) {
                zeige(Meldung("Der Kategoriename ist leer oder in dieser Art schon vergeben.", istFehler = true))
            }
        }
    }

    fun loescheKategorie(id: Long) {
        viewModelScope.launch {
            repository.loescheKategorie(id)
            if (_gewaehlteKategorie.value == id) _gewaehlteKategorie.value = null
        }
    }

    // ---- Chatverlauf aufräumen (Baustein P.2) ----

    fun loescheNachricht(nachricht: NachrichtEntity) {
        viewModelScope.launch {
            repository.loescheNachricht(nachricht.id)
            zeige(Meldung(if (nachricht.rolle == "user") "Frage gelöscht." else "Antwort gelöscht."))
        }
    }

    /** Löscht die Frage samt der Antwort, die unmittelbar darauf folgte. */
    fun loescheFrageUndAntwort(nachricht: NachrichtEntity) {
        viewModelScope.launch {
            val alle = repository.nachrichtenEinmal(nachricht.ideeId)
            val stelle = alle.indexOfFirst { it.id == nachricht.id }
            if (stelle < 0) return@launch
            val ids = mutableListOf(nachricht.id)
            if (nachricht.rolle == "user") {
                alle.drop(stelle + 1).takeWhile { it.rolle == "assistant" }.forEach { ids += it.id }
            } else {
                alle.take(stelle).lastOrNull { it.rolle == "user" }?.let { ids += it.id }
            }
            repository.loescheNachrichten(ids)
            zeige(Meldung("Frage und Antwort gelöscht."))
        }
    }

    fun loescheKonversation(ideeId: Long) {
        viewModelScope.launch {
            repository.loescheKonversation(ideeId)
            zeige(Meldung("Die Unterhaltung ist gelöscht."))
        }
    }

    fun aendere(idee: IdeeEntity, titel: String, text: String) {
        viewModelScope.launch {
            // Ein leerer Titel wuerde die Idee in der Liste unauffindbar machen — dann greift
            // derselbe Ersatztitel wie beim Anlegen.
            val name = titel.trim().ifBlank { ersatzTitel(text) }
            repository.aendere(idee, name, text)
            zeige(Meldung("Änderung übernommen."))
        }
    }

    fun setzeUmgesetzt(idee: IdeeEntity) {
        viewModelScope.launch {
            repository.setzeStatus(idee, IdeenStatus.UMGESETZT)
            zeige(Meldung("„${idee.titel}“ ist jetzt umgesetzt."))
        }
    }

    fun zurueckZuOffen(idee: IdeeEntity) {
        viewModelScope.launch {
            repository.setzeStatus(idee, IdeenStatus.OFFEN)
            zeige(Meldung("„${idee.titel}“ steht wieder unter den offenen Ideen."))
        }
    }

    fun loesche(idee: IdeeEntity) {
        viewModelScope.launch {
            repository.loesche(idee)
            zeige(Meldung("Idee gelöscht."))
        }
    }

    fun schreibeReihenfolge(ids: List<Long>) {
        viewModelScope.launch { repository.schreibeReihenfolge(ids) }
    }

    // ---- Suche (Baustein K) ----

    fun suche(text: String) {
        _suchtext.value = text
        suchJob?.cancel()
        if (text.isBlank()) {
            _suchtreffer.value = emptyList()
            return
        }
        suchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(250) // Entprellung beim Tippen
            _suchtreffer.value = repository.suche(text)
            repository.merkeSuchanfrage(text)
        }
    }

    fun leereSuche() {
        _suchtext.value = ""
        _suchtreffer.value = emptyList()
    }

    fun leereSuchverlauf() {
        viewModelScope.launch { repository.leereSuchverlauf() }
    }

    // ---- Vorlesen (Baustein D) ----

    fun lies(quelle: String, titel: String, text: String) = vorleser.sprich(quelle, titel, text)

    fun vorlesenStoppen() = vorleser.stopp()

    fun vorlesenFehlerGelesen() = vorleser.fehlerGelesen()

    // ---- Diktat (Baustein F) ----

    fun starteAufnahme(): Boolean {
        if (_aufnahme.value.laeuft) return true
        val gestartet = recorder.start(viewModelScope)
        if (!gestartet) {
            zeige(
                Meldung(
                    "Die Aufnahme ging nicht los. Prüf, ob die App das Mikrofon benutzen darf.",
                    istFehler = true,
                ),
            )
            return false
        }
        _aufnahme.value = AufnahmeStand(laeuft = true, seit = System.currentTimeMillis())
        IdeenCrashHandler.letzteAktion = "Aufnahme"
        return true
    }

    /** Beendet die Aufnahme und liefert den erkannten Text an [fertig]. */
    fun beendeAufnahme(fertig: (String) -> Unit) {
        if (!_aufnahme.value.laeuft) return
        _aufnahme.value = _aufnahme.value.copy(laeuft = false, wirdUebertragen = true)
        aufnahmeJob = viewModelScope.launch {
            try {
                val wav = recorder.stop()
                if (wav == null || wav.size < 2_000) {
                    _aufnahme.value = AufnahmeStand()
                    zeige(Meldung("Die Aufnahme war zu kurz — es kam kein Ton an.", istFehler = true))
                    return@launch
                }
                val schluessel = settings.groqApiKey
                if (schluessel.isBlank()) {
                    _aufnahme.value = AufnahmeStand()
                    zeige(
                        Meldung(
                            "Für die Spracheingabe fehlt der Groq-Schlüssel. Trag ihn in den Einstellungen ein.",
                            istFehler = true,
                            zuEinstellungen = true,
                        ),
                    )
                    return@launch
                }
                val diktat = Diktat(GroqTranscriber(schluessel))
                val ergebnis = diktat.transkribiere(wav)
                _aufnahme.value = AufnahmeStand()
                if (ergebnis.text.isBlank()) {
                    zeige(Meldung("Es war nichts Verständliches zu hören.", istFehler = true))
                    return@launch
                }
                if (ergebnis.teileFehlend > 0) {
                    zeige(
                        Meldung(
                            "${ergebnis.teileFehlend} von ${ergebnis.teileGesamt} Teilen des Diktats " +
                                "kamen nicht durch — der Rest steht im Feld.",
                            istFehler = true,
                        ),
                    )
                }
                fertig(ergebnis.text)
            } catch (fehler: Exception) {
                _aufnahme.value = AufnahmeStand()
                IdeenLog.error("Diktat", "beendeAufnahme", "Übertragung fehlgeschlagen", mapOf("art" to fehler.javaClass.simpleName))
                zeige(
                    Meldung(
                        fehler.message ?: "Die Aufnahme konnte nicht übertragen werden.",
                        istFehler = true,
                        wiederholen = { beendeAufnahme(fertig) },
                    ),
                )
            }
        }
    }

    fun brichAufnahmeAb() {
        aufnahmeJob?.cancel()
        viewModelScope.launch { runCatching { recorder.stop() } }
        _aufnahme.value = AufnahmeStand()
    }

    // ---- KI (Baustein O) ----

    fun frage(idee: IdeeEntity, eingabe: String) {
        if (eingabe.isBlank() || _ki.value.antwortet) return
        kiJob = viewModelScope.launch {
            repository.ergaenzeNachricht(idee.id, "user", eingabe.trim())
            _ki.value = KiStand(antwortet = true)
            val verlauf = nachrichten.value.map { ChatTurn(it.rolle, it.text) } +
                ChatTurn("user", eingabe.trim())
            val puffer = StringBuilder()
            try {
                val antwort = codex.streamChat(
                    instructions = anweisung(idee),
                    turns = verlauf,
                    model = CodexModel.fromLabel(settings.model),
                    reasoningEffort = ReasoningEffort.fromLabel(settings.reasoning),
                ) { stueck ->
                    puffer.append(stueck)
                    _ki.value = _ki.value.copy(teilAntwort = puffer.toString())
                }
                val sauber = UmlautKorrektur.korrigiere(antwort)
                repository.ergaenzeNachricht(idee.id, "assistant", sauber)
                _ki.value = KiStand()
            } catch (abbruch: kotlinx.coroutines.CancellationException) {
                // Das bereits Empfangene bleibt erhalten und wird als unvollständig gekennzeichnet.
                if (puffer.isNotEmpty()) {
                    repository.ergaenzeNachricht(
                        idee.id,
                        "assistant",
                        UmlautKorrektur.korrigiere(puffer.toString()),
                        unvollstaendig = true,
                    )
                }
                _ki.value = KiStand()
                throw abbruch
            } catch (fehler: Exception) {
                if (puffer.isNotEmpty()) {
                    repository.ergaenzeNachricht(
                        idee.id,
                        "assistant",
                        UmlautKorrektur.korrigiere(puffer.toString()),
                        unvollstaendig = true,
                    )
                }
                _ki.value = KiStand(fehler = fehlerText(fehler))
                zeige(
                    Meldung(
                        fehlerText(fehler),
                        istFehler = true,
                        wiederholen = { frage(idee, eingabe) },
                        zuEinstellungen = fehler is CodexAuthException && fehler.kind == AuthErrorKind.REAUTH,
                    ),
                )
            }
        }
    }

    fun brichKiAb() {
        codex.cancelChat()
        kiJob?.cancel()
        _ki.value = KiStand()
    }

    /**
     * Lässt die KI einen Titel für die eingesprochene Idee finden — höchstens drei Wörter.
     *
     * Kommt kein Zugang zustande, greift der Ersatz aus den ersten Wörtern des Diktats: Ein
     * Titelfeld darf nie leer bleiben, nur weil die KI schweigt (Baustein L).
     */
    fun schlageTitelVor(text: String, fertig: (String) -> Unit) {
        val quelle = text.trim()
        if (quelle.isBlank() || _titelLaeuft.value) return
        viewModelScope.launch {
            _titelLaeuft.value = true
            try {
                val antwort = codex.streamChat(
                    instructions = TITEL,
                    turns = listOf(ChatTurn("user", quelle.take(4000))),
                    model = CodexModel.fromLabel(settings.model),
                    reasoningEffort = ReasoningEffort.fromLabel(settings.reasoning),
                )
                val titel = kuerzeAufDreiWoerter(UmlautKorrektur.korrigiere(antwort))
                _titelLaeuft.value = false
                if (titel.isBlank()) {
                    fertig(ersatzTitel(quelle))
                    return@launch
                }
                IdeenLog.info(
                    "Titel",
                    "schlageTitelVor",
                    "Titel von der KI",
                    mapOf("woerter" to titel.split(" ").size),
                )
                fertig(titel)
            } catch (fehler: Exception) {
                _titelLaeuft.value = false
                fertig(ersatzTitel(quelle))
                IdeenLog.warn(
                    "Titel",
                    "schlageTitelVor",
                    "Kein Titel von der KI, Ersatz gesetzt",
                    mapOf("art" to fehler.javaClass.simpleName),
                )
                zeige(
                    Meldung(
                        if (fehler is CodexAuthException && fehler.kind == AuthErrorKind.REAUTH) {
                            "Für den Titelvorschlag fehlt der KI-Zugang. Der Titel kommt " +
                                "solange aus deinen ersten Worten."
                        } else {
                            "Der Titelvorschlag kam nicht durch — der Titel kommt aus deinen " +
                                "ersten Worten. Du kannst ihn überschreiben."
                        },
                        istFehler = true,
                        wiederholen = { schlageTitelVor(quelle, fertig) },
                        zuEinstellungen = fehler is CodexAuthException &&
                            fehler.kind == AuthErrorKind.REAUTH,
                    ),
                )
            }
        }
    }

    /**
     * Sicherheitsnetz: Auch wenn das Modell mehr liefert, bleiben höchstens drei Wörter übrig.
     * Anführungszeichen und Schlusspunkt fallen weg — ein Titel trägt keine Satzzeichen.
     */
    internal fun kuerzeAufDreiWoerter(roh: String): String = roh
        .lineSequence()
        .firstOrNull { it.isNotBlank() }
        .orEmpty()
        .trim()
        .trim('"', '\'', '„', '“', '”', '«', '»')
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .take(3)
        .joinToString(" ")
        .trimEnd('.', ',', ';', ':', '!')
        .trim()

    /** Der Ersatz ohne KI: die ersten drei Wörter des Diktats. */
    private fun ersatzTitel(text: String): String =
        kuerzeAufDreiWoerter(text).ifBlank { "Neue Idee" }

    /**
     * Bringt einen diktierten Text in gutes Deutsch (Baustein O.4).
     *
     * Das Original bleibt erhalten: [KorrekturStand] hält es fest, solange die korrigierte
     * Fassung steht. Aus dem Korrektur-Knopf wird dadurch ein Rückgängig-Knopf.
     */
    fun korrigiereText(roh: String, fertig: (String) -> Unit) {
        if (roh.isBlank() || _ki.value.antwortet) return
        viewModelScope.launch {
            _ki.value = KiStand(antwortet = true)
            try {
                val antwort = codex.streamChat(
                    instructions = KORREKTUR,
                    turns = listOf(ChatTurn("user", roh)),
                    model = CodexModel.fromLabel(settings.model),
                    reasoningEffort = ReasoningEffort.fromLabel(settings.reasoning),
                )
                val sauber = UmlautKorrektur.korrigiere(antwort.trim())
                _ki.value = KiStand()
                if (sauber.isBlank()) {
                    zeige(Meldung("Es kam keine korrigierte Fassung zurück.", istFehler = true))
                    return@launch
                }
                _korrektur.value = KorrekturStand(original = roh, korrigiert = sauber)
                fertig(sauber)
                IdeenLog.info(
                    "Korrektur",
                    "korrigiereText",
                    "Text korrigiert",
                    mapOf("vorherChars" to roh.length, "nachherChars" to sauber.length),
                )
            } catch (fehler: Exception) {
                _ki.value = KiStand()
                zeige(
                    Meldung(
                        fehlerText(fehler),
                        istFehler = true,
                        wiederholen = { korrigiereText(roh, fertig) },
                    ),
                )
            }
        }
    }

    /** Stellt das ungeglättete Diktat wieder her. */
    fun korrekturZuruecknehmen(fertig: (String) -> Unit) {
        val stand = _korrektur.value ?: return
        _korrektur.value = null
        fertig(stand.original)
        zeige(Meldung("Dein Originaltext ist wieder da."))
    }

    /** Wird der Text von Hand geändert, ist die Korrektur nicht mehr rücknehmbar. */
    fun korrekturVergessen() {
        _korrektur.value = null
    }

    fun meldeAn(activity: ComponentActivity) {
        viewModelScope.launch {
            _anmeldung.value = AnmeldeStand(laeuft = true)
            try {
                val ergebnis = codex.login(activity) { info: DeviceAuthInfo ->
                    _anmeldung.value = AnmeldeStand(
                        laeuft = true,
                        code = info.userCode,
                        adresse = info.verificationUri,
                        gueltigBis = System.currentTimeMillis() + 15 * 60_000L,
                    )
                }
                settings.chatGptConnectedAt = System.currentTimeMillis()
                _anmeldung.value = AnmeldeStand()
                zeige(Meldung("Angemeldet als ${ergebnis.email ?: "ChatGPT-Konto"}."))
            } catch (fehler: Exception) {
                _anmeldung.value = AnmeldeStand()
                zeige(Meldung(fehlerText(fehler), istFehler = true))
            }
        }
    }

    fun brichAnmeldungAb() {
        codex.cancelLogin()
        _anmeldung.value = AnmeldeStand()
    }

    fun meldeAb() {
        codex.logout()
        settings.chatGptConnectedAt = 0L
        zeige(Meldung("Vom ChatGPT-Konto abgemeldet."))
    }

    // ---- Einstellungen ----

    fun setzeTheme(wert: String) {
        settings.theme = wert
        _theme.value = settings.theme
    }

    /** Ein Tipp schaltet zwischen genau zwei Modi um — hell und dunkel (Baustein C). */
    fun themeWeiterschalten() {
        setzeTheme(if (settings.theme == "dark") "light" else "dark")
    }

    fun setzeSchriftgroesse(wert: Float) {
        settings.schriftgroesse = wert
        _schriftgroesse.value = settings.schriftgroesse
    }

    fun pruefeGroqSchluessel(fertig: (String) -> Unit) {
        viewModelScope.launch {
            val schluessel = settings.groqApiKey
            if (schluessel.isBlank()) {
                fertig("Es ist kein Schlüssel eingetragen.")
                return@launch
            }
            // Eine winzige Stille-Aufnahme reicht als echter Aufruf.
            val ergebnis = runCatching {
                GroqTranscriber(schluessel).transcribe(stilleWav())
            }
            fertig(
                ergebnis.fold(
                    onSuccess = { "Der Schlüssel wird angenommen." },
                    onFailure = { "Abgelehnt: ${it.message}" },
                ),
            )
        }
    }

    fun pruefeGoogleSchluessel(fertig: (String) -> Unit) {
        viewModelScope.launch {
            val ergebnis = runCatching {
                de.frank.genialeideen.speech.Synthese(getApplication(), settings).pruefeGoogle()
            }
            fertig(
                ergebnis.fold(
                    onSuccess = { datei -> datei.delete(); "Die Stimme antwortet." },
                    onFailure = { "Abgelehnt: ${it.message}" },
                ),
            )
        }
    }

    /**
     * Übernimmt eine Stimme aus der gemeinsamen Liste. Mit der Stimme wird die zugehörige
     * Engine mitgeschaltet — ich wähle die Stimme, nicht die Engine (Kapitel 4.6).
     */
    fun waehleStimme(eintrag: StimmenEintrag) {
        when (eintrag.anbieter) {
            TtsProvider.GOOGLE_CLOUD -> settings.googleTtsVoice = eintrag.id
            TtsProvider.EDGE -> settings.edgeTtsVoice = eintrag.id
            TtsProvider.QWEN -> settings.qwenStandardVoice = eintrag.id
            TtsProvider.QWEN_CLONE -> settings.qwenTtsVoiceId = eintrag.id
        }
        settings.ttsProvider = eintrag.anbieter.id
        _gewaehlteStimme.value = eintrag.id
        IdeenLog.info(
            "Stimmen",
            "waehleStimme",
            "Stimme gewählt",
            mapOf("anbieter" to eintrag.anbieter.id),
        )
    }

    /**
     * Spielt eine Kostprobe mit genau dieser Stimme und übernimmt sie dabei — sonst hörte man
     * eine Stimme, die danach gar nicht liest.
     */
    fun probeStimme(eintrag: StimmenEintrag) {
        if (vorleseStand.value.quelle == "probe") {
            vorleser.stopp()
            return
        }
        waehleStimme(eintrag)
        vorleser.sprich(
            "probe",
            "Stimmprobe",
            "So klingt diese Stimme, wenn sie deine genialen Ideen vorliest.",
        )
    }

    /** Baut die Stimmenliste neu — nötig, sobald ein Schlüssel dazukommt oder wegfällt. */
    fun stimmenlisteAuffrischen() {
        _favoriten.value = settings.favoriteTtsVoices
    }

    /** Probe einer eigenen Stimme, aufgerufen aus dem Stimmen-Bildschirm. */
    fun probeEigeneStimme(id: String) {
        val eintrag = stimmenEintraege.value.firstOrNull { it.id == id }
            ?: StimmenEintrag(
                id = id,
                name = id,
                anbieter = TtsProvider.QWEN_CLONE,
                gruppe = Stimmenliste.GRUPPE_MEINE,
                herkunft = TtsProvider.QWEN_CLONE.kurz,
            )
        probeStimme(eintrag)
    }

    fun schalteFavorit(id: String) {
        val bisher = settings.favoriteTtsVoices
        settings.favoriteTtsVoices = if (id in bisher) bisher - id else bisher + id
        _favoriten.value = settings.favoriteTtsVoices
    }

    // ---- Eigene Stimmen (Baustein E) ----

    /**
     * Lädt die geklonten Stimmen des Kontos. Ohne diesen Aufruf stünde nur die rohe Kennung da,
     * die niemand von Hand eintippt.
     */
    fun ladeEigeneStimmen(zeigeFehler: Boolean = false) {
        val schluessel = settings.qwenTtsApiKey
        if (schluessel.isBlank()) {
            _eigeneStimmen.value = emptyList()
            return
        }
        stimmenJob?.cancel()
        stimmenJob = viewModelScope.launch {
            _stimmenLaden.value = true
            runCatching { verzeichnis.list(schluessel) }
                .onSuccess { liste ->
                    _eigeneStimmen.value = liste
                    _stimmenFehler.value = null
                    IdeenLog.info("Stimmen", "ladeEigeneStimmen", "Eigene Stimmen geladen", mapOf("anzahl" to liste.size))
                    pruefeGemerkteStimme()
                    if (zeigeFehler && liste.isEmpty()) {
                        zeige(
                            Meldung(
                                "Zu diesem Schlüssel gibt es noch keine geklonte Stimme. " +
                                    "Nimm unter „Eigene Stimme aufnehmen“ eine auf.",
                            ),
                        )
                    }
                }
                .onFailure { fehler ->
                    _eigeneStimmen.value = emptyList()
                    // Der Fehlschlag einer Engine leert das Menü nicht — die betroffene Gruppe
                    // zeigt eine eigene Zeile, alle anderen bleiben bedienbar (Kapitel 4.6).
                    _stimmenFehler.value = fehler.message
                    IdeenLog.warn("Stimmen", "ladeEigeneStimmen", "Liste nicht geladen", mapOf("art" to fehler.javaClass.simpleName))
                    zeige(
                        Meldung(
                            fehler.message ?: "Die eigenen Stimmen liessen sich nicht laden.",
                            istFehler = true,
                            wiederholen = { ladeEigeneStimmen(zeigeFehler) },
                        ),
                    )
                }
            _stimmenLaden.value = false
        }
    }

    /** Der Name, unter dem eine geklonte Stimme in der App steht. */
    fun stimmenName(stimme: ClonedVoice): String =
        settings.qwenVoiceNames[stimme.id] ?: stimme.name

    fun benenneStimme(id: String, name: String) {
        settings.qwenVoiceNames = settings.qwenVoiceNames + (id to name.trim())
        _eigeneStimmen.value = _eigeneStimmen.value.toList()
    }

    fun waehleEigeneStimme(id: String) {
        settings.qwenTtsVoiceId = id
        settings.ttsProvider = TtsProvider.QWEN_CLONE.id
        _gewaehlteEigeneStimme.value = id
        _gewaehlteStimme.value = id
    }

    fun loescheEigeneStimme(id: String) {
        viewModelScope.launch {
            runCatching { stimmAnmeldung.delete(settings.qwenTtsApiKey, id) }
                .onSuccess {
                    _eigeneStimmen.value = _eigeneStimmen.value.filterNot { it.id == id }
                    if (settings.qwenTtsVoiceId == id) {
                        settings.qwenTtsVoiceId = ""
                        _gewaehlteEigeneStimme.value = ""
                    }
                    zeige(Meldung("Stimme gelöscht."))
                }
                .onFailure { fehler ->
                    zeige(Meldung("Löschen ging nicht: ${fehler.message}", istFehler = true))
                }
        }
    }

    /** Legt aus einer fertigen Aufnahme eine neue geklonte Stimme an. */
    fun legeStimmeAn(name: String, wav: ByteArray, fertig: (Boolean) -> Unit) {
        viewModelScope.launch {
            _stimmenLaden.value = true
            runCatching { stimmAnmeldung.create(settings.qwenTtsApiKey, name.trim(), wav) }
                .onSuccess { id ->
                    benenneStimme(id, name)
                    waehleEigeneStimme(id)
                    ladeEigeneStimmen()
                    zeige(Meldung("Die Stimme steht bereit."))
                    fertig(true)
                }
                .onFailure { fehler ->
                    zeige(
                        Meldung(
                            "Die Stimme konnte nicht angelegt werden: ${fehler.message}",
                            istFehler = true,
                        ),
                    )
                    fertig(false)
                }
            _stimmenLaden.value = false
        }
    }

    // ---- Sicherung (Baustein J) ----

    /** Der Name des gemerkten Sicherungsordners — null, solange keiner gewählt wurde. */
    private val _sicherungsOrdner = MutableStateFlow(sicherung.ordnerName())
    val sicherungsOrdner: StateFlow<String?> = _sicherungsOrdner.asStateFlow()

    /**
     * Sichert in den gemerkten Ordner. Fehlt einer, öffnet [ordnerWaehlen] den Ordnerwähler —
     * es passiert nie stillschweigend nichts.
     */
    fun sichereJetzt(ordnerWaehlen: () -> Unit) {
        if (sicherung.sicherungsOrdner == null) {
            zeige(Meldung("Wähl zuerst den Ordner aus, in den gesichert werden soll."))
            ordnerWaehlen()
            return
        }
        viewModelScope.launch {
            runCatching { sicherung.sichere() }
                .onSuccess { stand ->
                    zeige(Meldung("Gesichert. $stand"))
                }
                .onFailure { fehler ->
                    zeige(
                        Meldung(
                            "Die Sicherung ging nicht: ${fehler.message}",
                            istFehler = true,
                            wiederholen = ordnerWaehlen,
                        ),
                    )
                }
        }
    }

    /** Der frisch gewählte Ordner wird gemerkt und sofort beschrieben. */
    fun merkeOrdnerUndSichere(ordner: Uri) {
        sicherung.merkeOrdner(ordner)
        _sicherungsOrdner.value = sicherung.ordnerName()
        viewModelScope.launch {
            runCatching { sicherung.sichere() }
                .onSuccess { stand -> zeige(Meldung("Gesichert. $stand")) }
                .onFailure { fehler ->
                    zeige(
                        Meldung(
                            "In diesen Ordner liess sich nicht schreiben: ${fehler.message}",
                            istFehler = true,
                        ),
                    )
                }
        }
    }

    /**
     * Holt die jüngste Sicherung aus dem gemerkten Ordner zurück. Vor dem Einspielen wird
     * gezeigt, was drinsteckt — erst der zweite Tipp schreibt wirklich in den Bestand.
     */
    fun stelleWiederHer(ordnerWaehlen: () -> Unit) {
        if (sicherung.sicherungsOrdner == null) {
            zeige(Meldung("Wähl zuerst den Ordner aus, in dem die Sicherung liegt."))
            ordnerWaehlen()
            return
        }
        viewModelScope.launch {
            val neueste = runCatching { sicherung.neuesteSicherung() }.getOrNull()
            if (neueste == null) {
                zeige(
                    Meldung(
                        "In diesem Ordner liegt noch keine Sicherung dieser App.",
                        istFehler = true,
                        wiederholen = ordnerWaehlen,
                    ),
                )
                return@launch
            }
            zeigeVorschau(neueste.uri, neueste.name)
        }
    }

    private suspend fun zeigeVorschau(quelle: Uri, name: String) {
        runCatching { sicherung.vorschauVon(quelle) }
            .onSuccess { vorschau ->
                zeige(
                    Meldung(
                        "$name: ${vorschau.ideen} Ideen vom ${vorschau.erstelltAm}. Deine " +
                            "${vorschau.bestehende} bestehenden bleiben stehen. " +
                            "Zum Einspielen noch einmal tippen.",
                        wiederholen = { spieleEin(quelle) },
                    ),
                )
            }
            .onFailure { fehler ->
                zeige(
                    Meldung(
                        "Die Sicherung liess sich nicht lesen: ${fehler.message}",
                        istFehler = true,
                    ),
                )
            }
    }

    private fun spieleEin(quelle: Uri) {
        viewModelScope.launch {
            runCatching { sicherung.stelleWiederHerAus(quelle, ersetzen = false) }
                .onSuccess { anzahl -> zeige(Meldung("$anzahl Ideen eingespielt.")) }
                .onFailure { fehler ->
                    zeige(Meldung("Einspielen ging nicht: ${fehler.message}", istFehler = true))
                }
        }
    }

    /** Trennt den gemerkten Ordner — die Sicherungen darin bleiben liegen. */
    fun vergissSicherungsOrdner() {
        sicherung.vergissOrdner()
        _sicherungsOrdner.value = null
        zeige(Meldung("Der Sicherungsordner ist vergessen. Die Dateien bleiben liegen."))
    }

    fun sicherungsStand(): String = sicherung.standText()

    // ---- Diagnose (Baustein P) ----

    fun absturzberichte(): List<File> = IdeenCrashHandler.berichte(getApplication())

    fun verwerfeAbstuerze() {
        IdeenCrashHandler.verwerfen(getApplication())
        _abgestuerzt.value = false
    }

    fun leereProtokoll() {
        IdeenLog.leeren()
        zeige(Meldung("Protokoll geleert."))
    }

    /**
     * Die gemerkte Stimme. Ist sie nicht mehr verfügbar (Stimme gelöscht, Schlüssel weg), fällt
     * die App auf Edge zurück und sagt es einmal im Klartext (Kapitel 4.6).
     */
    private fun aktuelleStimmenId(): String = when (settings.ttsProvider) {
        TtsProvider.GOOGLE_CLOUD.id -> settings.googleTtsVoice
        TtsProvider.QWEN.id -> settings.qwenStandardVoice
        TtsProvider.QWEN_CLONE.id -> settings.qwenTtsVoiceId
        else -> settings.edgeTtsVoice
    }

    /** Prüft nach dem Laden, ob die gemerkte Stimme noch existiert. */
    private fun pruefeGemerkteStimme() {
        val id = aktuelleStimmenId()
        val anbieter = settings.ttsProvider
        val nochDa = when (anbieter) {
            TtsProvider.QWEN_CLONE.id -> _eigeneStimmen.value.any { it.id == id }
            TtsProvider.QWEN.id -> settings.qwenTtsApiKey.isNotBlank()
            TtsProvider.GOOGLE_CLOUD.id -> settings.googleTtsApiKey.isNotBlank()
            else -> true
        }
        if (nochDa || id.isBlank() && anbieter == TtsProvider.EDGE.id) return
        if (!nochDa) {
            settings.ttsProvider = TtsProvider.EDGE.id
            _gewaehlteStimme.value = settings.edgeTtsVoice
            zeige(
                Meldung(
                    "Die zuletzt gewählte Stimme ist nicht mehr verfügbar. Es liest jetzt " +
                        "Microsoft Edge vor — such dir in den Einstellungen eine neue aus.",
                    istFehler = true,
                    zuEinstellungen = true,
                ),
            )
        }
    }

    // ---- Hilfen ----

    fun zeige(meldung: Meldung) {
        _meldung.value = meldung
    }

    fun meldungGelesen() {
        _meldung.value = null
    }

    private fun anweisung(idee: IdeeEntity): String = buildString {
        append("Du hilfst beim Weiterdenken einer Idee. Die Idee lautet: „")
        append(idee.titel)
        append("“. Beschreibung: ")
        append(idee.text.take(4000))
        append("\n\n")
        append(
            "Antworte kurz, konkret und in ganzen Sätzen. Stell Rückfragen, wenn etwas unklar ist, " +
                "und mach umsetzbare Vorschläge.",
        )
        append("\n\n")
        append(TTS_REGEL)
    }

    private fun fehlerText(fehler: Throwable): String = when {
        fehler is CodexAuthException && fehler.kind == AuthErrorKind.REAUTH ->
            "Die Anmeldung ist abgelaufen. Bitte neu anmelden."
        fehler is CodexAuthException && fehler.kind == AuthErrorKind.QUOTA ->
            "Das Kontingent ist gerade ausgeschöpft. Versuch es später erneut."
        else -> fehler.message ?: "Da ist etwas schiefgegangen."
    }

    /** Eine winzige, gültige WAV-Datei aus Stille — nur für den Schlüssel-Test. */
    private fun stilleWav(): ByteArray {
        val daten = ByteArray(3_200)
        val kopf = ByteArray(44)
        "RIFF".toByteArray().copyInto(kopf, 0)
        "WAVEfmt ".toByteArray().copyInto(kopf, 8)
        kopf[16] = 16
        kopf[20] = 1
        kopf[22] = 1
        kopf[24] = 0x80.toByte()
        kopf[25] = 0x3E
        kopf[32] = 2
        kopf[34] = 16
        "data".toByteArray().copyInto(kopf, 36)
        val gesamt = kopf + daten
        val groesse = gesamt.size - 8
        gesamt[4] = (groesse and 0xFF).toByte()
        gesamt[5] = ((groesse shr 8) and 0xFF).toByte()
        gesamt[40] = (daten.size and 0xFF).toByte()
        gesamt[41] = ((daten.size shr 8) and 0xFF).toByte()
        return gesamt
    }

    override fun onCleared() {
        recorder.release()
        super.onCleared()
    }

    companion object {
        /**
         * Alles, was einen Lautsprecher-Knopf bekommt, muss auch gut klingen: keine Sonderzeichen,
         * keine Internetadressen, keine Quellenangaben.
         */
        const val TTS_REGEL =
            "Dieser Text wird vorgelesen. Schreib ihn in ganzen, gesprochenen Sätzen. " +
                "Verzichte auf Markdown, Aufzählungszeichen, Sternchen, Rauten, Klammern, " +
                "Tabellen, Emoji und Abkürzungen. Schreib Zahlen, Einheiten und Abkürzungen " +
                "aus. Keine Internetadressen, keine Quellenangaben. Antworte auf Deutsch mit " +
                "echten Umlauten (ä ö ü Ä Ö Ü ß)."

        /**
         * Der Auftrag für den Korrektur-Knopf. Bewusst eng gefasst: Inhalt bleibt, nur die
         * Sprache wird besser. Gekürzt werden ausschliesslich echte Wiederholungen.
         */
        /**
         * Der Auftrag für den Titel. Höchstens drei Wörter — der Titel steht in einer Liste
         * und muss auf dem schmalen Cover-Display in eine Zeile passen.
         */
        const val TITEL =
            "Du bekommst eine eingesprochene Idee. Gib ihr einen Titel aus HÖCHSTENS DREI " +
                "WÖRTERN, der den Kern trifft.\n\n" +
                "Regeln:\n" +
                "- Höchstens drei Wörter, lieber zwei.\n" +
                "- Keine Anführungszeichen, kein Punkt am Ende, keine Nummerierung.\n" +
                "- Keine Einleitung wie „Titel:" + "“ — gib nur den Titel selbst zurück.\n" +
                "- Deutsch mit echten Umlauten (ä ö ü Ä Ö Ü ß).\n" +
                "- Benutze die Worte der Idee, erfinde kein neues Thema."

        const val KORREKTUR =
            "Du bekommst einen diktierten Text. Erkenne, was gemeint ist, und gib ihn in " +
                "grammatikalisch und orthografisch einwandfreiem Deutsch zurück: richtige " +
                "Zeichensetzung, saubere Satzstellung, sinnvolle Absätze, Füllwörter und " +
                "Versprecher raus.\n\n" +
                "Strenge Regeln:\n" +
                "- Lass keine Information weg. Jede Aussage des Originals steht auch in deiner Fassung.\n" +
                "- Füge keine neue Information hinzu, erfinde nichts, deute nichts aus.\n" +
                "- Kürzen darfst du ausschliesslich echte Wiederholungen: Wird dieselbe Sache " +
                "zweimal gesagt, bleibt sie einmal stehen.\n" +
                "- Behalte Ton und Sichtweise des Originals bei; aus einer Notiz wird kein Aufsatz.\n" +
                "- Gib nur den korrigierten Text zurück, ohne Vorrede, ohne Anführungszeichen, " +
                "ohne Kommentar."

        const val GLAETTUNG =
            "Bring den folgenden diktierten Text in gutes Deutsch: Füllwörter raus, Satzzeichen " +
                "und Absätze rein, Versprecher bereinigen. Verändere den Inhalt nicht, erfinde " +
                "nichts hinzu und lass nichts weg. Gib nur den geglätteten Text zurück, ohne " +
                "Vorrede.\n\n" + TTS_REGEL
    }
}
