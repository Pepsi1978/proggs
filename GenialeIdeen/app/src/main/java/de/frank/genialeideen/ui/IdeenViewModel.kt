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
import de.frank.genialeideen.data.local.NachrichtEntity
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.observability.IdeenCrashHandler
import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.speech.VorleseStand
import de.frank.genialeideen.text.UmlautKorrektur
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

    val vorleseStand: StateFlow<VorleseStand> = vorleser.stand

    private val _meldung = MutableStateFlow<Meldung?>(null)
    val meldung: StateFlow<Meldung?> = _meldung.asStateFlow()

    private val _aufnahme = MutableStateFlow(AufnahmeStand())
    val aufnahme: StateFlow<AufnahmeStand> = _aufnahme.asStateFlow()

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

    val chatGptVerbunden: Boolean get() = codex.isConnected
    val chatGptKonto: String? get() = codex.email

    // ---- Ideen ----

    fun oeffne(id: Long?) {
        _offeneIdee.value = id
        IdeenCrashHandler.letzteAktion = if (id == null) "Liste" else "Idee geöffnet"
    }

    fun legeAn(titel: String, text: String, aufnahmePfad: String? = null, originalText: String? = null) {
        viewModelScope.launch {
            val name = titel.trim().ifBlank { text.trim().take(60).ifBlank { "Neue Idee" } }
            repository.lege(name, text.trim(), aufnahmePfad, originalText)
            zeige(Meldung("Idee gespeichert."))
        }
    }

    fun aendere(idee: IdeeEntity, titel: String, text: String) {
        viewModelScope.launch { repository.aendere(idee, titel, text) }
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
     * Glättet einen diktierten Text (Baustein O.3). Das Original bleibt erhalten und ist
     * jederzeit wiederherstellbar.
     */
    fun glaetteText(roh: String, fertig: (String) -> Unit) {
        if (roh.isBlank() || _ki.value.antwortet) return
        viewModelScope.launch {
            _ki.value = KiStand(antwortet = true)
            try {
                val antwort = codex.streamChat(
                    instructions = GLAETTUNG,
                    turns = listOf(ChatTurn("user", roh)),
                    model = CodexModel.fromLabel(settings.model),
                    reasoningEffort = ReasoningEffort.fromLabel(settings.reasoning),
                )
                _ki.value = KiStand()
                fertig(UmlautKorrektur.korrigiere(antwort.trim()))
            } catch (fehler: Exception) {
                _ki.value = KiStand()
                zeige(
                    Meldung(
                        fehlerText(fehler),
                        istFehler = true,
                        wiederholen = { glaetteText(roh, fertig) },
                    ),
                )
            }
        }
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

    fun themeWeiterschalten() {
        setzeTheme(
            when (settings.theme) {
                "light" -> "dark"
                "dark" -> "system"
                else -> "light"
            },
        )
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
                de.frank.genialeideen.speech.Synthese(getApplication(), settings).synthetisiere("Probe.")
            }
            fertig(
                ergebnis.fold(
                    onSuccess = { datei -> datei.delete(); "Die Stimme antwortet." },
                    onFailure = { "Abgelehnt: ${it.message}" },
                ),
            )
        }
    }

    fun probeStimme(stimmeId: String) {
        val vorher = settings.googleTtsVoice
        settings.googleTtsVoice = stimmeId
        vorleser.sprich("probe", "Stimmprobe", "So klingt diese Stimme, wenn sie deine Ideen vorliest.")
        // Die Wahl bleibt, bis der Nutzer eine andere trifft — die Probe setzt sie also.
        if (vorher.isBlank()) settings.googleTtsVoice = stimmeId
    }

    // ---- Sicherung (Baustein J) ----

    fun exportiere(ziel: Uri) {
        viewModelScope.launch {
            runCatching { sicherung.exportiere(ziel) }
                .onSuccess { anzahl -> zeige(Meldung("$anzahl Ideen in die Datei geschrieben.")) }
                .onFailure { fehler ->
                    zeige(Meldung("Export fehlgeschlagen: ${fehler.message}", istFehler = true))
                }
        }
    }

    fun vorschau(quelle: Uri, fertig: (SicherungsVorschau?) -> Unit) {
        viewModelScope.launch {
            runCatching { sicherung.vorschau(quelle) }
                .onSuccess(fertig)
                .onFailure { fehler ->
                    fertig(null)
                    zeige(Meldung("Die Datei liess sich nicht lesen: ${fehler.message}", istFehler = true))
                }
        }
    }

    fun importiere(quelle: Uri, ersetzen: Boolean) {
        viewModelScope.launch {
            runCatching { sicherung.importiere(quelle, ersetzen) }
                .onSuccess { anzahl -> zeige(Meldung("$anzahl Ideen eingespielt.")) }
                .onFailure { fehler ->
                    zeige(Meldung("Import fehlgeschlagen: ${fehler.message}", istFehler = true))
                }
        }
    }

    fun sichereNachDrive() {
        viewModelScope.launch {
            runCatching { sicherung.nachDriveSichern() }
                .onSuccess { stand -> zeige(Meldung("Bei Google gesichert. $stand")) }
                .onFailure { fehler ->
                    zeige(
                        Meldung(
                            "Die Google-Sicherung ging nicht: ${fehler.message}",
                            istFehler = true,
                            wiederholen = ::sichereNachDrive,
                        ),
                    )
                }
        }
    }

    fun holeVonDrive(ersetzen: Boolean) {
        viewModelScope.launch {
            runCatching { sicherung.vonDriveHolen(ersetzen) }
                .onSuccess { anzahl -> zeige(Meldung("$anzahl Ideen aus der Google-Sicherung geholt.")) }
                .onFailure { fehler ->
                    zeige(Meldung("Wiederherstellen ging nicht: ${fehler.message}", istFehler = true))
                }
        }
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
            "Der Text wird vorgelesen. Schreib ihn deshalb vorlesefreundlich: ganze Sätze, " +
                "keine Sonderzeichen, keine Aufzählungszeichen, keine Sternchen oder Rauten, " +
                "keine Internetadressen, keine Quellenangaben und keine Klammerzusätze wie " +
                "„vgl." + "“ oder Seitenzahlen. Zahlen und Abkürzungen ausschreiben, wo es " +
                "natürlich klingt."

        const val GLAETTUNG =
            "Bring den folgenden diktierten Text in gutes Deutsch: Füllwörter raus, Satzzeichen " +
                "und Absätze rein, Versprecher bereinigen. Verändere den Inhalt nicht, erfinde " +
                "nichts hinzu und lass nichts weg. Gib nur den geglätteten Text zurück, ohne " +
                "Vorrede.\n\n" + TTS_REGEL
    }
}
