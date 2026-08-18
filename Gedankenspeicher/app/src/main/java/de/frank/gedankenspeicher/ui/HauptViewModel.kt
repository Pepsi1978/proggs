package de.frank.gedankenspeicher.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.gedankenspeicher.audio.MicRecorder
import androidx.documentfile.provider.DocumentFile
import de.frank.gedankenspeicher.data.Auswertungsprofil
import de.frank.gedankenspeicher.data.Datenbank
import de.frank.gedankenspeicher.data.KiAntwort
import de.frank.gedankenspeicher.data.Notiz
import de.frank.gedankenspeicher.data.Notizzustand
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.data.Sitzung
import de.frank.gedankenspeicher.data.Verlaufseintrag
import de.frank.gedankenspeicher.data.settings.Einstellungen
import de.frank.gedankenspeicher.data.settings.Websuche
import de.frank.gedankenspeicher.auth.CodexAuthManager
import de.frank.gedankenspeicher.tts.TtsProvider
import de.frank.gedankenspeicher.tts.Vorleser
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * **Der Ablauf der App.**
 *
 * Hier laufen die Fäden zusammen, die das Funktions-Spec beschreibt: aufnehmen und
 * transkribieren (F-01 bis F-04), Überschriften holen (F-05), vorlesen (F-06), verbessern
 * (F-07) und auswerten (F-09). Die Bildschirme rufen nur noch die Absichten auf.
 */
class HauptViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context = app.applicationContext
    val einstellungen = Einstellungen(ctx)
    private val db = Datenbank.hole(ctx)
    val codex = CodexAuthManager(ctx)
    val repo = Repository(ctx, db, einstellungen, codex)

    private val mikrofon = MicRecorder(ctx)
    val vorleser = Vorleser(ctx, einstellungen)

    private val _verlauf = MutableStateFlow(Verlaufszustand())
    val verlauf: StateFlow<Verlaufszustand> = _verlauf

    private val _kiBlatt = MutableStateFlow(KiBlattzustand())
    val kiBlatt: StateFlow<KiBlattzustand> = _kiBlatt

    private val _suche = MutableStateFlow(Suchzustand())
    val suche: StateFlow<Suchzustand> = _suche

    private val _bearbeitung = MutableStateFlow(Bearbeitungszustand())
    val bearbeitung: StateFlow<Bearbeitungszustand> = _bearbeitung

    private val _anmeldung = MutableStateFlow(Anmeldezustand())
    val anmeldung: StateFlow<Anmeldezustand> = _anmeldung

    /** Die Erscheinung als beobachtbarer Wert — F-15 wirkt sofort und überall, ohne Neustart. */
    private val _erscheinung = MutableStateFlow(einstellungen.erscheinung)
    val erscheinung: StateFlow<String> = _erscheinung

    // Die Einstellungen liegen in EncryptedSharedPreferences und melden von sich aus keine
    // Änderung. Damit die Oberfläche trotzdem sofort nachzieht, spiegelt jeder Wert, den ein
    // Bildschirm anzeigt, in einen beobachtbaren Fluss — geschrieben wird weiterhin dorthin.
    private val _groq = MutableStateFlow(einstellungen.groqSchluessel)
    val groqSchluessel: StateFlow<String> = _groq

    private val _ttsAnbieter = MutableStateFlow(einstellungen.ttsAnbieter)
    val ttsAnbieter: StateFlow<String> = _ttsAnbieter

    private val _ttsStimme = MutableStateFlow(stimmeZu(einstellungen.ttsAnbieter))
    val ttsStimme: StateFlow<String> = _ttsStimme

    private val _google = MutableStateFlow(einstellungen.googleTtsSchluessel)
    val googleSchluessel: StateFlow<String> = _google

    private val _qwen = MutableStateFlow(einstellungen.qwenSchluessel)
    val qwenSchluessel: StateFlow<String> = _qwen

    private val _drive = MutableStateFlow(einstellungen.driveSicherungAn)
    val driveAn: StateFlow<Boolean> = _drive

    /** Meldet der Oberfläche, dass sie den Ordner-Wähler öffnen soll (F-17). */
    private val _sicherungsordnerFehlt = MutableStateFlow(false)
    val sicherungsordnerFehlt: StateFlow<Boolean> = _sicherungsordnerFehlt

    /** Meldet der Oberfläche, dass sie den Datei-Wähler für die Wiederherstellung öffnen soll. */
    private val _sucheSicherungsdatei = MutableStateFlow(false)
    val sucheSicherungsdatei: StateFlow<Boolean> = _sucheSicherungsdatei

    /** Steht auf true, wenn die App nach einer Wiederherstellung neu starten muss. */
    private val _neustartNoetig = MutableStateFlow(false)
    val neustartNoetig: StateFlow<Boolean> = _neustartNoetig

    /** Die sechs Profile (F-10). */
    val profile = repo.profile

    private var verlaufJob: Job? = null
    private var aufnahmeJob: Job? = null
    private var auswertungJob: Job? = null

    /**
     * Wohin das nächste Transkript geht.
     *
     * Dieselbe Aufnahme- und Transkriptionskette bedient zwei Ziele: den Verlauf (F-01) und
     * das Antwortfeld im KI-Blatt (F-09, Schritt 5). Getrennte Ketten wären zweimal derselbe
     * Code — und damit zwei Stellen, an denen die Halluzinations-Abwehr auseinanderlaufen kann.
     */
    private var aufnahmeGehtInsKiBlatt = false

    init {
        viewModelScope.launch {
            repo.legeProfileAnWennNoetig()
            repo.angefangeneAufraeumen()
            val sitzung = repo.offeneSitzung()
            beobachteSitzung(sitzung)
            reicheWartendeNach()
            holeFehlendeUeberschriften()
        }
        viewModelScope.launch {
            repo.sitzungen.collectLatest { liste -> _verlauf.update { it.copy(sitzungen = liste) } }
        }
        viewModelScope.launch {
            mikrofon.pegel.collectLatest { p -> _verlauf.update { it.copy(pegel = p) } }
        }
        viewModelScope.launch {
            vorleser.absatzNr.collectLatest { nr -> _verlauf.update { it.copy(vorleseAbsatz = nr) } }
        }
        viewModelScope.launch {
            vorleser.laeuft.collectLatest { laeuft ->
                if (!laeuft) _verlauf.update { it.copy(liestVor = null, vorleseAbsatz = -1) }
            }
        }
    }

    // --- Sitzungen ---------------------------------------------------------------------------

    private fun beobachteSitzung(sitzung: Sitzung) {
        verlaufJob?.cancel()
        _verlauf.update { it.copy(sitzung = sitzung, laedt = true, eintraege = emptyList()) }
        verlaufJob = viewModelScope.launch {
            repo.verlauf(sitzung.id).collectLatest { eintraege ->
                _verlauf.update { it.copy(eintraege = eintraege, laedt = false) }
            }
        }
    }

    fun wechsleSitzung(id: Long) {
        // Während einer Aufnahme oder Auswertung ist der Wechsel gesperrt (F-13, Fehlerfall):
        // ein halb aufgenommener Gedanke landete sonst in der falschen Sitzung.
        val z = _verlauf.value
        if (z.nimmtAuf) return melde("Erst die Aufnahme beenden.")
        if (z.wertetAus) return melde("Die Auswertung läuft noch.")
        viewModelScope.launch {
            repo.oeffneSitzung(id)
            repo.offeneSitzung().let(::beobachteSitzung)
        }
    }

    fun neueSitzung() {
        val z = _verlauf.value
        if (z.nimmtAuf) return melde("Erst die Aufnahme beenden.")
        viewModelScope.launch { beobachteSitzung(repo.neueSitzung()) }
    }

    fun benenneSitzungUm(id: Long, titel: String) {
        if (titel.isBlank()) return
        viewModelScope.launch {
            repo.benenneSitzungUm(id, titel)
            if (id == _verlauf.value.sitzung?.id) {
                repo.offeneSitzung().let { s -> _verlauf.update { it.copy(sitzung = s) } }
            }
        }
    }

    fun loescheSitzung(sitzung: Sitzung) {
        viewModelScope.launch {
            val naechste = repo.loescheSitzung(sitzung)
            if (sitzung.id == _verlauf.value.sitzung?.id) beobachteSitzung(naechste)
        }
    }

    // --- Notiz tippen (F-02) -------------------------------------------------------------------

    fun setzeEntwurf(text: String) = _verlauf.update { it.copy(entwurf = text) }

    fun sendeEntwurf() {
        val text = _verlauf.value.entwurf.trim()
        val sitzung = _verlauf.value.sitzung ?: return
        if (text.isEmpty()) return
        _verlauf.update { it.copy(entwurf = "") }
        viewModelScope.launch {
            val id = repo.legeGetippteNotizAn(sitzung.id, text)
            versorgeNeueNotiz(id, sitzung.id, text)
        }
    }

    // --- Notiz einsprechen (F-01) ---------------------------------------------------------------

    fun mikrofonAbgelehnt() = _verlauf.update { it.copy(mikrofonAbgelehnt = true) }

    fun mikrofonErlaubt() = _verlauf.update { it.copy(mikrofonAbgelehnt = false) }

    /** Ein Tipp startet, ein zweiter beendet — es wird nicht gehalten (F-01, Auslöser). */
    fun aufnahmeUmschalten() {
        if (_verlauf.value.nimmtAuf) {
            beendeAufnahme()
        } else {
            aufnahmeGehtInsKiBlatt = false
            starteAufnahme()
        }
    }

    /**
     * F-09, Schritt 5: die Antwort auf die Rückfrage einsprechen.
     *
     * Das Transkript landet im Antwortfeld des Blattes, nicht als Notiz im Verlauf — sonst
     * stünde die Antwort auf eine Auswertung selbst wieder als auszuwertende Notiz da.
     */
    fun antwortAufnahmeUmschalten() {
        if (_verlauf.value.nimmtAuf) {
            beendeAufnahme()
        } else {
            aufnahmeGehtInsKiBlatt = true
            starteAufnahme()
        }
    }

    private fun starteAufnahme() {
        if (_verlauf.value.sitzung == null) return
        // Es spricht immer nur einer: die laufende Sprachausgabe endet hier (F-01, Regeln).
        vorleser.halteAn()
        if (!mikrofon.start(viewModelScope)) {
            melde("Die Aufnahme ließ sich nicht starten.")
            return
        }
        _verlauf.update { it.copy(nimmtAuf = true, aufnahmeDauerMs = 0) }
        aufnahmeJob = viewModelScope.launch {
            val begonnen = System.currentTimeMillis()
            while (_verlauf.value.nimmtAuf) {
                val gelaufen = System.currentTimeMillis() - begonnen
                _verlauf.update { it.copy(aufnahmeDauerMs = gelaufen) }
                if (gelaufen >= HOECHSTDAUER_MS) {
                    // Zehn Minuten sind die Grenze; danach wird von selbst beendet und
                    // transkribiert, statt eine Aufnahme entstehen zu lassen, die Groq ablehnt.
                    beendeAufnahme()
                    break
                }
                delay(200)
            }
        }
    }

    /** Beendet die Aufnahme und legt die Karte an — auch wenn nichts brauchbar war. */
    fun beendeAufnahme() {
        if (!_verlauf.value.nimmtAuf) return
        val sitzung = _verlauf.value.sitzung ?: return
        _verlauf.update { it.copy(nimmtAuf = false, aufnahmeDauerMs = 0) }
        aufnahmeJob?.cancel()
        viewModelScope.launch {
            val wav = mikrofon.stop()
            if (wav == null || wav.size < MINDESTGROESSE_WAV) {
                melde("Zu kurz — dabei ist nichts angekommen.")
                aufnahmeGehtInsKiBlatt = false
                return@launch
            }
            if (aufnahmeGehtInsKiBlatt) {
                aufnahmeGehtInsKiBlatt = false
                schreibeInsAntwortfeld(wav)
                return@launch
            }
            if (!hatNetz()) {
                // F-04: die Aufnahme wandert in den dauerhaften Speicher, die Karte entsteht
                // trotzdem. Der Cache reichte nicht — Android räumt ihn ohne Vorwarnung weg.
                val datei = puffere(wav)
                repo.legeGesprocheneNotizAn(sitzung.id, Notizzustand.WARTET_AUF_TRANSKRIPTION, datei.absolutePath)
                return@launch
            }
            val id = repo.legeGesprocheneNotizAn(sitzung.id, Notizzustand.TRANSKRIBIERT_GERADE, null)
            transkribiere(id, sitzung.id, wav, null)
        }
    }

    private fun puffere(wav: ByteArray): File {
        val ordner = File(ctx.filesDir, "wartend").apply { mkdirs() }
        return File(ordner, "aufnahme-${System.currentTimeMillis()}.wav").apply { writeBytes(wav) }
    }

    // --- Transkription (F-03) --------------------------------------------------------------------

    private suspend fun transkribiere(notizId: Long, sitzungId: Long, wav: ByteArray, datei: File?) {
        val notiz = repo.notiz(notizId) ?: return
        val transkriber = repo.transkriber()
        if (!transkriber.isConfigured) {
            repo.aendere(notiz.copy(zustand = Notizzustand.KEIN_SCHLUESSEL, audioPfad = datei?.absolutePath))
            return
        }
        try {
            val text = transkriber.transcribe(wav)
            if (text.isBlank()) {
                // Alle vier Schichten haben nichts durchgelassen. Es wird ausdrücklich
                // **nichts erfunden** (F-01, Fehlerfall): die Karte sagt, dass nichts ankam.
                repo.aendere(
                    notiz.copy(
                        zustand = Notizzustand.NICHTS_VERSTANDEN,
                        audioPfad = datei?.absolutePath,
                        versucheTranskription = notiz.versucheTranskription + 1,
                    ),
                )
                return
            }
            datei?.let { runCatching { it.delete() } }
            repo.aendere(notiz.copy(text = text, zustand = Notizzustand.FERTIG, audioPfad = null))
            versorgeNeueNotiz(notizId, sitzungId, text)
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            repo.aendere(
                notiz.copy(
                    zustand = Notizzustand.TRANSKRIPTION_FEHLGESCHLAGEN,
                    audioPfad = datei?.absolutePath ?: puffere(wav).absolutePath,
                    versucheTranskription = notiz.versucheTranskription + 1,
                ),
            )
        } finally {
            transkriber.shutdown()
        }
    }

    /**
     * Transkribiert und hängt das Ergebnis an das Antwortfeld des KI-Blattes an.
     *
     * Angehängt, nicht ersetzt: wer schon etwas getippt hat und dann noch etwas nachspricht,
     * soll nicht sein Getipptes verlieren.
     */
    private suspend fun schreibeInsAntwortfeld(wav: ByteArray) {
        val transkriber = repo.transkriber()
        if (!transkriber.isConfigured) {
            _kiBlatt.update { it.copy(fehler = "Für die Transkription fehlt der Groq-Schlüssel.") }
            return
        }
        try {
            val text = transkriber.transcribe(wav)
            if (text.isBlank()) {
                _kiBlatt.update { it.copy(fehler = "Nichts verstanden — versuch es noch einmal.") }
                return
            }
            _kiBlatt.update { blatt ->
                val bisher = blatt.antwort.trim()
                blatt.copy(
                    antwort = if (bisher.isEmpty()) text else "$bisher $text",
                    fehler = null,
                )
            }
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            _kiBlatt.update { it.copy(fehler = fehler.message ?: "Die Transkription ist nicht durchgekommen.") }
        } finally {
            transkriber.shutdown()
        }
    }

    /** Der Wiederholen-Knopf an einer fehlgeschlagenen Karte. */
    fun versucheTranskriptionErneut(notiz: Notiz) {
        val pfad = notiz.audioPfad
        if (pfad == null) {
            melde("Die Aufnahme ist nicht mehr da.")
            return
        }
        viewModelScope.launch {
            val datei = File(pfad)
            if (!datei.exists()) {
                melde("Die Aufnahme ist nicht mehr da.")
                return@launch
            }
            repo.aendere(notiz.copy(zustand = Notizzustand.TRANSKRIBIERT_GERADE))
            transkribiere(notiz.id, notiz.sitzungId, datei.readBytes(), datei)
        }
    }

    /**
     * F-04, Schritt 2: die wartenden Aufnahmen nachreichen, in der Reihenfolge ihrer
     * Entstehung. Jede Karte füllt sich an ihrer Stelle — sie springt nicht ans Ende.
     */
    fun reicheWartendeNach() {
        if (!hatNetz()) return
        viewModelScope.launch {
            repo.wartendeNotizen().forEach { notiz ->
                val pfad = notiz.audioPfad ?: return@forEach
                val datei = File(pfad)
                if (!datei.exists()) {
                    repo.aendere(notiz.copy(zustand = Notizzustand.TRANSKRIPTION_FEHLGESCHLAGEN, audioPfad = null))
                    return@forEach
                }
                repo.aendere(notiz.copy(zustand = Notizzustand.TRANSKRIBIERT_GERADE))
                transkribiere(notiz.id, notiz.sitzungId, datei.readBytes(), datei)
            }
        }
    }

    // --- Überschriften (F-05) ------------------------------------------------------------------

    /** Überschrift holen und, falls es die erste Notiz war, den Sitzungstitel dazu. */
    private fun versorgeNeueNotiz(notizId: Long, sitzungId: Long, text: String) {
        viewModelScope.launch {
            runCatching {
                val ueberschrift = repo.holeUeberschrift(text)
                if (ueberschrift.isNotBlank()) {
                    repo.notiz(notizId)?.let { aktuell ->
                        // Hat Frank in der Zwischenzeit selbst eine vergeben, gewinnt seine.
                        if (!aktuell.ueberschriftVonHand && aktuell.ueberschrift == null) {
                            repo.aendere(aktuell.copy(ueberschrift = ueberschrift))
                        }
                    }
                }
            }
            runCatching { repo.setzeTitelWennNochKeiner(sitzungId, text) }
        }
    }

    /** F-05, Fehlerfall: was beim letzten Mal nicht klappte, wird beim Start nachgeholt. */
    private fun holeFehlendeUeberschriften() {
        viewModelScope.launch {
            if (!codex.isConnected || !hatNetz()) return@launch
            repo.notizenOhneUeberschrift().forEach { notiz ->
                if (notiz.text.isBlank()) return@forEach
                runCatching {
                    val u = repo.holeUeberschrift(notiz.text)
                    if (u.isNotBlank()) repo.aendere(notiz.copy(ueberschrift = u))
                }
            }
        }
    }

    // --- Vorlesen (F-06) --------------------------------------------------------------------------

    /**
     * Derselbe Knopf schaltet an und aus; ein anderer beendet den laufenden und beginnt neu
     * (F-06, Schritte 1 und 2).
     */
    fun lesenUmschalten(kennung: String, text: String) {
        if (_verlauf.value.liestVor == kennung) {
            vorleser.halteAn()
            _verlauf.update { it.copy(liestVor = null, vorleseAbsatz = -1) }
            return
        }
        if (text.isBlank()) return
        _verlauf.update { it.copy(liestVor = kennung, vorleseAbsatz = -1) }
        vorleser.merkeQuelle(kennung)
        vorleser.lies(text) { fehler -> melde(fehler) }
    }

    // --- Verbessern (F-07) ------------------------------------------------------------------------

    fun verbessere(notiz: Notiz) {
        if (notiz.istVerbessert) return
        if (notiz.text.isBlank()) return
        _verlauf.update { it.copy(verbessertGerade = it.verbessertGerade + notiz.id) }
        viewModelScope.launch {
            try {
                val neu = repo.verbessere(notiz.text)
                if (neu.isBlank()) {
                    melde("Die Verbesserung kam leer zurück — der Text bleibt, wie er war.")
                    return@launch
                }
                val aktuell = repo.notiz(notiz.id) ?: return@launch
                repo.aendere(
                    aktuell.copy(
                        // Nur setzen, wenn noch nichts drinsteht: `textOriginal` ist der
                        // wirklich gesprochene Wortlaut und darf nie überschrieben werden.
                        textOriginal = aktuell.textOriginal ?: aktuell.text,
                        text = neu,
                        istVerbessert = true,
                    ),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                melde(fehler.message ?: "Die Verbesserung ist nicht durchgekommen.")
            } finally {
                _verlauf.update { it.copy(verbessertGerade = it.verbessertGerade - notiz.id) }
            }
        }
    }

    fun macheVerbesserungRueckgaengig(notiz: Notiz) {
        val original = notiz.textOriginal ?: return
        viewModelScope.launch {
            repo.aendere(notiz.copy(text = original, textOriginal = null, istVerbessert = false))
        }
    }

    // --- Notiz-Menü (F-08) --------------------------------------------------------------------------

    fun loescheNotiz(notiz: Notiz) {
        if (_verlauf.value.liestVor == "notiz:${notiz.id}") vorleser.halteAn()
        viewModelScope.launch { repo.loescheNotiz(notiz) }
    }

    fun verschiebeNotiz(notiz: Notiz, zielSitzung: Long) {
        viewModelScope.launch { repo.verschiebeNotiz(notiz, zielSitzung) }
    }

    fun oeffneBearbeitung(notiz: Notiz) {
        _bearbeitung.value = Bearbeitungszustand(notiz, notiz.ueberschrift ?: "", notiz.text)
    }

    fun setzeBearbeitung(ueberschrift: String, text: String) =
        _bearbeitung.update { it.copy(ueberschrift = ueberschrift, text = text) }

    fun schliesseBearbeitung() { _bearbeitung.value = Bearbeitungszustand() }

    fun speichereBearbeitung() {
        val z = _bearbeitung.value
        val notiz = z.notiz ?: return
        viewModelScope.launch {
            repo.bearbeiteNotiz(notiz, z.ueberschrift, z.text)
            schliesseBearbeitung()
        }
    }

    // --- KI-Auswertung (F-09) ------------------------------------------------------------------------

    fun oeffneKiBlatt() {
        val sitzung = _verlauf.value.sitzung ?: return
        if (_verlauf.value.wertetAus) return
        val grund = Websuche.vonId(einstellungen.websucheGrundhaltung)
        _kiBlatt.value = KiBlattzustand(
            offen = true,
            websuche = grund == Websuche.IMMER,
            websucheKiEntscheidet = grund == Websuche.KI_ENTSCHEIDET,
            codexFehlt = !codex.isConnected,
        )
        viewModelScope.launch {
            _kiBlatt.update { it.copy(profil = repo.holeAktivesProfil()) }
            ladeKontextUndFrage(sitzung.id, ganzeSitzung = false)
        }
    }

    fun schliesseKiBlatt() {
        // Vor der Antwort geschlossen: nichts wird gespeichert, die Notizen bleiben
        // unausgewertet (F-09, Fehlerfall). Genau so ist es gewollt.
        codex.cancelQuestionGeneration()
        // Eine laufende Antwort-Aufnahme endet mit dem Blatt — sonst liefe das Mikrofon
        // weiter und ihr Transkript käme in ein Feld, das es nicht mehr gibt.
        if (_verlauf.value.nimmtAuf && aufnahmeGehtInsKiBlatt) {
            aufnahmeGehtInsKiBlatt = false
            beendeAufnahme()
        }
        _kiBlatt.value = KiBlattzustand()
    }

    fun setzeGanzeSitzung(an: Boolean) {
        val sitzung = _verlauf.value.sitzung ?: return
        _kiBlatt.update { it.copy(ganzeSitzung = an) }
        viewModelScope.launch { ladeKontextUndFrage(sitzung.id, an) }
    }

    fun setzeWebsuche(an: Boolean) = _kiBlatt.update { it.copy(websuche = an, websucheKiEntscheidet = false) }

    fun setzeWebsucheKiEntscheidet() =
        _kiBlatt.update { it.copy(websucheKiEntscheidet = true, websuche = false) }

    fun setzeKiAntwort(text: String) = _kiBlatt.update { it.copy(antwort = text) }

    private suspend fun ladeKontextUndFrage(sitzungId: Long, ganzeSitzung: Boolean) {
        val notizen = repo.kontextNotizen(sitzungId, ganzeSitzung)
        _kiBlatt.update {
            it.copy(
                kontextzahl = notizen.size,
                nichtsNeues = notizen.isEmpty() && !ganzeSitzung,
                rueckfrage = "",
                fehler = null,
            )
        }
        if (notizen.isEmpty()) return
        if (!codex.isConnected) {
            _kiBlatt.update { it.copy(codexFehlt = true, holtFrage = false) }
            return
        }
        _kiBlatt.update { it.copy(holtFrage = true, codexFehlt = false) }
        try {
            val frage = repo.holeRueckfrage(repo.alsKontext(notizen))
            _kiBlatt.update { it.copy(rueckfrage = frage, holtFrage = false) }
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            _kiBlatt.update {
                it.copy(holtFrage = false, fehler = fehler.message ?: "Die Rückfrage kam nicht durch.")
            }
        }
    }

    fun holeRueckfrageErneut() {
        val sitzung = _verlauf.value.sitzung ?: return
        viewModelScope.launch { ladeKontextUndFrage(sitzung.id, _kiBlatt.value.ganzeSitzung) }
    }

    /** Der Knopf „Auswerten" — ab hier läuft es im Verlauf weiter, das Blatt schließt sich. */
    fun werteAus() {
        val sitzung = _verlauf.value.sitzung ?: return
        val blatt = _kiBlatt.value
        val antwort = blatt.antwort.trim()
        if (antwort.isEmpty()) {
            _kiBlatt.update { it.copy(fehler = "Sag noch, worauf ich mich konzentrieren soll.") }
            return
        }
        val ganzeSitzung = blatt.ganzeSitzung
        val websuche = blatt.websuche || blatt.websucheKiEntscheidet
        val rueckfrage = blatt.rueckfrage
        _kiBlatt.value = KiBlattzustand()
        _verlauf.update { it.copy(wertetAus = true) }

        auswertungJob = viewModelScope.launch {
            try {
                val notizen = repo.kontextNotizen(sitzung.id, ganzeSitzung)
                val profil = repo.holeAktivesProfil()
                val text = repo.holeAuswertung(
                    notizen = repo.alsKontext(notizen),
                    rueckfrage = rueckfrage,
                    antwort = antwort,
                    profilAnweisung = profil?.anweisung.orEmpty(),
                    websuche = websuche,
                )
                if (text.isBlank()) {
                    melde("Die Auswertung kam leer zurück.")
                    return@launch
                }
                repo.speichereAntwort(
                    KiAntwort(
                        sitzungId = sitzung.id,
                        erstelltAm = System.currentTimeMillis(),
                        rueckfrage = rueckfrage,
                        antwortDesNutzers = antwort,
                        text = text,
                        profilName = profil?.name.orEmpty(),
                        modell = einstellungen.codexModell,
                        effort = einstellungen.codexEffort,
                        websucheAn = websuche,
                        ganzeSitzung = ganzeSitzung,
                    ),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                melde(fehler.message ?: "Die Auswertung ist nicht durchgekommen.")
            } finally {
                _verlauf.update { it.copy(wertetAus = false) }
            }
        }
    }

    // --- Suche (F-14) ------------------------------------------------------------------------------

    fun setzeSuchbegriff(begriff: String) {
        _suche.update { it.copy(begriff = begriff, sucht = begriff.trim().length >= 2) }
        viewModelScope.launch {
            // Kurz warten: bei jedem Tastendruck sofort zu suchen, hieße bei einem langen
            // Wort ein Dutzend Abfragen, von denen nur die letzte zählt.
            delay(180)
            if (_suche.value.begriff != begriff) return@launch
            val treffer = repo.suche(begriff)
            if (_suche.value.begriff != begriff) return@launch
            _suche.update { it.copy(treffer = treffer, sucht = false) }
        }
    }

    fun leereSuche() { _suche.value = Suchzustand() }

    /** Sprung aus der Suche: Sitzung öffnen und die Notiz einmal aufleuchten lassen (M-11). */
    fun springeZu(sitzungId: Long, notizId: Long) {
        viewModelScope.launch {
            if (sitzungId != _verlauf.value.sitzung?.id) {
                repo.oeffneSitzung(sitzungId)
                repo.offeneSitzung().let(::beobachteSitzung)
            }
            _verlauf.update { it.copy(hebeHervor = notizId) }
            delay(1200)
            _verlauf.update { if (it.hebeHervor == notizId) it.copy(hebeHervor = null) else it }
        }
    }

    // --- Erscheinung (F-15) --------------------------------------------------------------------------

    fun setzeErscheinung(id: String) {
        einstellungen.erscheinung = id
        _erscheinung.value = id
    }

    // --- Export (F-16) ---------------------------------------------------------------------------------

    /** Bereitet die Datei vor; das Teilen selbst löst die Oberfläche aus (sie hat die Activity). */
    suspend fun exportdatei(sitzung: Sitzung): File {
        val eintraege = repo.verlaufEinmal(sitzung.id)
        return repo.exportdatei(sitzung, repo.alsMarkdown(sitzung, eintraege))
    }

    // --- Codex-Anmeldung (F-11) ----------------------------------------------------------------

    fun anmeldungBeginnt() {
        _anmeldung.value = Anmeldezustand(holtCode = true)
    }

    fun anmeldungCodeDa(code: String, adresse: String) {
        _anmeldung.value = Anmeldezustand(code = code, adresse = adresse, wartet = true)
    }

    fun anmeldungErfolgreich(email: String?) {
        _anmeldung.update { it.copy(wartet = false, erfolgreich = true) }
        melde(email?.let { "Codex verbunden als " + it } ?: "Codex verbunden.")
        // Was beim letzten Mal ohne Verbindung liegenblieb, wird jetzt nachgeholt.
        holeFehlendeUeberschriften()
    }

    fun anmeldungFehlgeschlagen(text: String) {
        val abgelaufen = text.contains("abgelaufen", ignoreCase = true)
        _anmeldung.update { it.copy(holtCode = false, wartet = false, abgelaufen = abgelaufen, fehler = text) }
    }

    fun trenneCodex() {
        codex.logout()
        _anmeldung.value = Anmeldezustand()
        melde("Codex getrennt.")
    }

    // --- Einstellungen schreiben ------------------------------------------------------------------

    fun setzeModell(apiId: String) { einstellungen.codexModell = apiId }

    fun setzeEffort(apiValue: String) { einstellungen.codexEffort = apiValue }

    fun setzeWebsucheGrundhaltung(id: String) { einstellungen.websucheGrundhaltung = id }

    fun setzeGroqSchluessel(wert: String) {
        einstellungen.groqSchluessel = wert.trim()
        _groq.value = wert.trim()
    }

    fun setzeGoogleSchluessel(wert: String) {
        einstellungen.googleTtsSchluessel = wert.trim()
        _google.value = wert.trim()
    }

    fun setzeQwenSchluessel(wert: String) {
        einstellungen.qwenSchluessel = wert.trim()
        _qwen.value = wert.trim()
    }

    fun setzeTtsAnbieter(id: String) {
        einstellungen.ttsAnbieter = id
        _ttsAnbieter.value = id
        // Jeder Anbieter hat seine eigene Stimme: nach dem Wechsel muss die Anzeige die
        // seine zeigen, nicht die des vorigen.
        _ttsStimme.value = stimmeZu(id)
    }

    fun setzeTtsStimme(id: String) {
        when (einstellungen.ttsAnbieter) {
            TtsProvider.GOOGLE_CLOUD.id -> einstellungen.stimmeGoogle = id
            TtsProvider.QWEN_CLONE.id -> einstellungen.stimmeQwen = id
            else -> einstellungen.stimmeEdge = id
        }
        _ttsStimme.value = id
    }

    private fun stimmeZu(anbieter: String): String = when (anbieter) {
        TtsProvider.GOOGLE_CLOUD.id -> einstellungen.stimmeGoogle
        TtsProvider.QWEN_CLONE.id -> einstellungen.stimmeQwen
        else -> einstellungen.stimmeEdge
    }

    /** Der Probe-Knopf in B-04. Läuft schon eine Probe, hält derselbe Knopf sie an. */
    fun spieleProbe() {
        if (vorleser.laeuft.value) {
            vorleser.halteAn()
            return
        }
        vorleser.merkeQuelle("probe")
        vorleser.lies(PROBESATZ) { fehler -> melde(fehler) }
    }

    // --- Profile (F-10) ---------------------------------------------------------------------------

    suspend fun aktiviereProfil(profil: Auswertungsprofil): Boolean = repo.aktiviereProfil(profil)

    fun speichereProfil(profil: Auswertungsprofil) {
        viewModelScope.launch {
            repo.speichereProfil(profil)
            // Wird dem aktiven Profil der Text genommen, kann es nicht aktiv bleiben —
            // sonst liefe die nächste Auswertung ohne jede Anweisung.
            if (profil.istAktiv && profil.anweisung.isBlank()) {
                repo.setzeProfilZurueck(profil.nummer, warAktiv = true)
                melde("Ein Profil ohne Text kann nicht aktiv sein.")
            }
        }
    }

    fun setzeProfilZurueck(profil: Auswertungsprofil) {
        viewModelScope.launch { repo.setzeProfilZurueck(profil.nummer, profil.istAktiv) }
    }

    fun loescheAntwort(antwort: KiAntwort) {
        if (_verlauf.value.liestVor == "antwort:" + antwort.id) vorleser.halteAn()
        viewModelScope.launch { repo.loescheAntwort(antwort) }
    }

    // --- Sicherung (F-17) --------------------------------------------------------------------------

    fun setzeDrive(an: Boolean) {
        einstellungen.driveSicherungAn = an
        _drive.value = an
    }

    /**
     * Sichert die Datenbankdatei in den gewählten Ordner.
     *
     * Kein Zugriff über die Google-Drive-Schnittstelle, sondern über den Ordner, den Android
     * bereitstellt: wählt Frank dort den Drive-Ordner, landet die Sicherung in Drive — ohne
     * dass die App eine zweite Anmeldung und ein zweites Zugriffsrecht braucht.
     */
    fun sichereJetzt() {
        val ordner = einstellungen.sicherungsordner
        if (ordner.isBlank()) {
            _sicherungsordnerFehlt.value = true
            return
        }
        viewModelScope.launch { fuehreSicherungAus(Uri.parse(ordner)) }
    }

    fun ordnerwahlErledigt() { _sicherungsordnerFehlt.value = false }

    fun merkeSicherungsordner(uri: Uri) {
        einstellungen.sicherungsordner = uri.toString()
        _sicherungsordnerFehlt.value = false
        viewModelScope.launch { fuehreSicherungAus(uri) }
    }

    private suspend fun fuehreSicherungAus(ordner: Uri) {
        try {
            val quelle = repo.datenbankdatei()
            if (!quelle.exists()) {
                melde("Es gibt noch nichts zu sichern.")
                return
            }
            val baum = DocumentFile.fromTreeUri(ctx, ordner)
            if (baum == null) {
                melde("Auf den Ordner kann nicht zugegriffen werden.")
                return
            }
            val name = "gedankenspeicher-" + System.currentTimeMillis() + ".db"
            val ziel = baum.createFile("application/octet-stream", name)
            if (ziel == null) {
                melde("Die Sicherungsdatei liess sich nicht anlegen.")
                return
            }
            withContext(Dispatchers.IO) {
                ctx.contentResolver.openOutputStream(ziel.uri)?.use { aus ->
                    quelle.inputStream().use { ein -> ein.copyTo(aus) }
                }
            }
            einstellungen.letzteSicherungZeit = System.currentTimeMillis()
            einstellungen.letzteSicherungGroesse = quelle.length()
            // Fünf Stände werden vorgehalten, der älteste fällt heraus (F-17, Regeln).
            baum.listFiles()
                .filter { it.name?.startsWith("gedankenspeicher-") == true }
                .sortedByDescending { it.name }
                .drop(5)
                .forEach { runCatching { it.delete() } }
            melde("Gesichert.")
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            melde(fehler.message ?: "Die Sicherung ist fehlgeschlagen.")
        }
    }

    /** Der Knopf „Wiederherstellen" — er öffnet den Datei-Wähler. */
    fun stelleWiederHer() {
        _sucheSicherungsdatei.value = true
    }

    fun dateiwahlErledigt() { _sucheSicherungsdatei.value = false }

    /**
     * Ersetzt den gesamten Datenbestand durch die gewählte Sicherung.
     *
     * Der Ablauf ist unbequem, aber der einzige sichere: alles anhalten, die Datenbank
     * schliessen, die Dateien austauschen, die App neu starten lassen. Room hält offene
     * Verbindungen und einen Journal-Puffer; würde man die Datei unter der laufenden
     * Datenbank tauschen, schriebe Room seinen alten Puffer in die neue Datei.
     *
     * Auch `-wal` und `-shm` müssen weg: bleiben sie von der alten Datenbank stehen, hält
     * SQLite sie für den gültigen jüngsten Stand und überschreibt die wiederhergestellte
     * Datei damit — die Wiederherstellung sähe dann aus, als wäre nichts passiert.
     */
    fun stelleWiederHerAus(uri: Uri) {
        _sucheSicherungsdatei.value = false
        viewModelScope.launch {
            try {
                vorleser.halteAn()
                if (_verlauf.value.nimmtAuf) beendeAufnahme()
                auswertungJob?.cancel()
                verlaufJob?.cancel()

                val ziel = repo.datenbankdatei()
                withContext(Dispatchers.IO) {
                    Datenbank.schliesse()
                    listOf("-wal", "-shm").forEach { anhang ->
                        runCatching { java.io.File(ziel.absolutePath + anhang).delete() }
                    }
                    ctx.contentResolver.openInputStream(uri)?.use { ein ->
                        ziel.outputStream().use { aus -> ein.copyTo(aus) }
                    } ?: throw IllegalStateException("Die Sicherungsdatei liess sich nicht lesen.")
                }
                _neustartNoetig.value = true
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                melde(fehler.message ?: "Die Wiederherstellung ist fehlgeschlagen.")
            }
        }
    }

    // --- Kleinkram -------------------------------------------------------------------------------------

    fun melde(text: String) = _verlauf.update { it.copy(meldung = text) }

    fun meldungGesehen() = _verlauf.update { it.copy(meldung = null) }

    private fun hatNetz(): Boolean {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return true
        val netz = cm.activeNetwork ?: return false
        val faehig = cm.getNetworkCapabilities(netz) ?: return false
        return faehig.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Die App geht in den Hintergrund. `01-FUNKTIONS-SPEC.md` §6: eine laufende Aufnahme wird
     * beendet und wie ein zweiter Tipp behandelt, die Sprachausgabe hört auf. Die Auswertung
     * läuft weiter — sie hängt am ViewModel, nicht am Bildschirm.
     */
    fun inDenHintergrund() {
        if (_verlauf.value.nimmtAuf) beendeAufnahme()
        vorleser.halteAn()
    }

    override fun onCleared() {
        vorleser.schliesse()
        mikrofon.release()
        super.onCleared()
    }

    private companion object {
        const val HOECHSTDAUER_MS = 10 * 60_000L

        /** Der feste Beispielsatz des Probe-Knopfs (F-18). */
        const val PROBESATZ = "So klinge ich, wenn ich dir deine Notizen vorlese."

        /** Unter etwa 0,4 s bei 16 kHz Mono ist nichts Verwertbares dabei (F-01, Fehlerfall). */
        const val MINDESTGROESSE_WAV = 44 + 16_000 * 2 * 4 / 10
    }
}
