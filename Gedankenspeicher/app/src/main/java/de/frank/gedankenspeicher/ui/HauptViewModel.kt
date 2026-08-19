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
import de.frank.gedankenspeicher.ui.theme.Erscheinung
import de.frank.gedankenspeicher.auth.CodexAuthManager
import de.frank.gedankenspeicher.tts.ClonedVoice
import de.frank.gedankenspeicher.tts.QwenVoiceDirectory
import de.frank.gedankenspeicher.tts.QwenVoiceEnrollment
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

    // Diese drei fehlten als Fluss — der Einstellungs-Bildschirm las sie direkt aus den
    // Einstellungen. Ein gewöhnlicher Lesezugriff löst in Compose aber **keine** neue
    // Zeichnung aus: die Wahl wurde gespeichert und wirkte auch, aber die Oberfläche zeigte
    // weiter die alte. Es sah aus, als liesse sich nichts umstellen.
    private val _codexModell = MutableStateFlow(einstellungen.codexModell)
    val codexModell: StateFlow<String> = _codexModell

    private val _codexEffort = MutableStateFlow(einstellungen.codexEffort)
    val codexEffort: StateFlow<String> = _codexEffort

    private val _websucheGrundhaltung = MutableStateFlow(einstellungen.websucheGrundhaltung)
    val websucheGrundhaltung: StateFlow<String> = _websucheGrundhaltung

    /** Ob Codex verbunden ist — als Fluss, damit B-04 den Wechsel sofort zeigt. */
    private val _codexVerbunden = MutableStateFlow(codex.isConnected)
    val codexVerbunden: StateFlow<Boolean> = _codexVerbunden

    private val _codexKonto = MutableStateFlow(codex.email)
    val codexKonto: StateFlow<String?> = _codexKonto

    // --- Die eigenen Stimmen bei Alibaba (F-18) ------------------------------------------
    private val _eigeneStimmen = MutableStateFlow<List<ClonedVoice>>(emptyList())
    val eigeneStimmen: StateFlow<List<ClonedVoice>> = _eigeneStimmen

    private val _stimmenLaden = MutableStateFlow(false)
    val stimmenLaden: StateFlow<Boolean> = _stimmenLaden

    /** Läuft gerade die Aufnahme einer neuen eigenen Stimme? */
    private val _nimmtStimmeAuf = MutableStateFlow(false)
    val nimmtStimmeAuf: StateFlow<Boolean> = _nimmtStimmeAuf

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
     * Dieselbe Aufnahme- und Transkriptionskette bedient drei Ziele: den Verlauf (F-01), das
     * Antwortfeld im KI-Blatt (F-09, Schritt 5) und das Textfeld des Bearbeiten-Blattes
     * (B-08). Getrennte Ketten wären dreimal derselbe Code — und damit drei Stellen, an
     * denen die Halluzinations-Abwehr auseinanderlaufen kann.
     */
    private var aufnahmeziel = Aufnahmeziel.VERLAUF

    private enum class Aufnahmeziel { VERLAUF, KI_BLATT, BEARBEITUNG }

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
        beobachteNetz()
    }

    /**
     * F-04, Schritt 2 — auf die Rückkehr des Netzes warten und dann nachreichen.
     *
     * Ohne diesen Beobachter blieb eine im Funkloch gesprochene Notiz liegen, bis die App
     * einmal in den Hintergrund und wieder nach vorn kam. Wer die App offen liess und
     * weiterging, bis das Netz zurückkam, sah seine Notiz stundenlang auf „Wartet auf Netz".
     */
    private fun beobachteNetz() {
        val cm = ctx.getSystemService(ConnectivityManager::class.java) ?: return
        netzWaechter = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                reicheWartendeNach()
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(netzWaechter!!) }
    }

    private var netzWaechter: ConnectivityManager.NetworkCallback? = null

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
            aufnahmeziel = Aufnahmeziel.VERLAUF
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
            aufnahmeziel = Aufnahmeziel.KI_BLATT
            starteAufnahme()
        }
    }

    /**
     * B-08 — Text in die Notiz **nachsprechen**, statt ihn tippen zu müssen.
     *
     * Spracherkennung ist schneller als eine Bildschirmtastatur, und wer eine Notiz ohnehin
     * eingesprochen hat, will sie auch mit der Stimme ergänzen. Das Transkript landet genau
     * an der Cursorstelle — nicht am Ende, sonst müsste man es dorthin schieben, wo es
     * hingehört.
     */
    fun bearbeitungsAufnahmeUmschalten() {
        if (_bearbeitung.value.notiz == null) return
        if (_verlauf.value.nimmtAuf) {
            beendeAufnahme()
        } else {
            aufnahmeziel = Aufnahmeziel.BEARBEITUNG
            _bearbeitung.update { it.copy(fehler = null) }
            starteAufnahme()
            _bearbeitung.update { it.copy(nimmtAuf = _verlauf.value.nimmtAuf) }
        }
    }

    private fun starteAufnahme() {
        if (_verlauf.value.sitzung == null) return
        // Es gibt nur ein Mikrofon. Läuft gerade eine Stimmprobe, hat sie Vorrang — sonst
        // landete sie als Notiz im Verlauf.
        if (_nimmtStimmeAuf.value) {
            melde("Erst die Stimmaufnahme beenden.")
            return
        }
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
        _bearbeitung.update { it.copy(nimmtAuf = false) }
        aufnahmeJob?.cancel()
        val ziel = aufnahmeziel
        aufnahmeziel = Aufnahmeziel.VERLAUF
        viewModelScope.launch {
            val wav = mikrofon.stop()
            if (wav == null || wav.size < MINDESTGROESSE_WAV) {
                if (ziel == Aufnahmeziel.BEARBEITUNG) {
                    _bearbeitung.update { it.copy(fehler = "Zu kurz — dabei ist nichts angekommen.") }
                } else {
                    melde("Zu kurz — dabei ist nichts angekommen.")
                }
                return@launch
            }
            if (ziel == Aufnahmeziel.KI_BLATT) {
                schreibeInsAntwortfeld(wav)
                return@launch
            }
            if (ziel == Aufnahmeziel.BEARBEITUNG) {
                schreibeInsBearbeitungsfeld(wav)
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

    /**
     * Bricht eine laufende Aufnahme ab, ohne dass etwas davon übrig bleibt.
     *
     * Wird das Blatt geschlossen, für das gesprochen wurde, gibt es kein Ziel mehr. Ohne
     * diesen Weg landete das Gesprochene ersatzweise als Notiz im Verlauf — eine Karte, die
     * niemand angelegt hat.
     */
    private fun verwirfAufnahme() {
        if (!_verlauf.value.nimmtAuf) return
        _verlauf.update { it.copy(nimmtAuf = false, aufnahmeDauerMs = 0) }
        _bearbeitung.update { it.copy(nimmtAuf = false) }
        aufnahmeziel = Aufnahmeziel.VERLAUF
        aufnahmeJob?.cancel()
        viewModelScope.launch { mikrofon.stop() }
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

    /**
     * Transkribiert und setzt das Ergebnis an der Cursorstelle des Bearbeiten-Blattes ein.
     *
     * Eingesetzt, nicht angehängt: der Sinn des Nachsprechens ist, mitten im Text etwas zu
     * ergänzen. Steht etwas ausgewählt, ersetzt das Gesprochene die Auswahl — so verhält
     * sich auch das Einfügen aus der Zwischenablage.
     */
    private suspend fun schreibeInsBearbeitungsfeld(wav: ByteArray) {
        if (_bearbeitung.value.notiz == null) return
        val transkriber = repo.transkriber()
        if (!transkriber.isConfigured) {
            _bearbeitung.update { it.copy(fehler = "Für die Transkription fehlt der Groq-Schlüssel.") }
            return
        }
        _bearbeitung.update { it.copy(transkribiert = true, fehler = null) }
        try {
            val text = transkriber.transcribe(wav)
            if (text.isBlank()) {
                _bearbeitung.update { it.copy(fehler = "Nichts verstanden — versuch es noch einmal.") }
                return
            }
            _bearbeitung.update { z ->
                // Das Blatt kann zwischenzeitlich geschlossen worden sein; dann gibt es
                // keine Stelle mehr, an die etwas gehört.
                if (z.notiz == null) return@update z
                setzeEin(z, text)
            }
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            _bearbeitung.update {
                it.copy(fehler = fehler.message ?: "Die Transkription ist nicht durchgekommen.")
            }
        } finally {
            _bearbeitung.update { it.copy(transkribiert = false) }
            transkriber.shutdown()
        }
    }

    /**
     * Setzt [einschub] an der gemerkten Stelle ein und schiebt den Cursor dahinter.
     *
     * Die Trennzeichen entstehen hier und nicht beim Transkribieren: ob und was davor
     * gehört, hängt von der Stelle ab, nicht vom Gesprochenen.
     *
     * Steht der Cursor am Textende — also auch, wenn gar keiner gesetzt wurde und man
     * direkt aufs Mikrofon drückt —, ist das Gesprochene ein **Nachtrag** und bekommt eine
     * Leerzeile zum Bisherigen. Mitten im Text bleibt es ein Einschub und bekommt nur ein
     * Leerzeichen; ein Absatz risse dort den Satz auseinander.
     */
    private fun setzeEin(z: Bearbeitungszustand, einschub: String): Bearbeitungszustand {
        val laenge = z.text.length
        val von = z.auswahlStart.coerceIn(0, laenge)
        val bis = z.auswahlEnde.coerceIn(von, laenge)
        val davor = z.text.substring(0, von)
        val danach = z.text.substring(bis)
        // Nachtrag: hinter der Stelle steht nichts mehr (auch kein blosser Leerraum). Das
        // ist auch der Fall, wenn gar kein Cursor gesetzt wurde — der steht dann am Ende.
        val nachtrag = danach.isBlank() && davor.isNotBlank()
        val kopf = if (nachtrag) davor.trimEnd() else davor
        val rest = if (nachtrag) "" else danach
        val fuege = buildString {
            when {
                nachtrag -> append("\n\n")
                kopf.isNotEmpty() && !kopf.last().isWhitespace() -> append(' ')
            }
            append(einschub.trim())
            if (!nachtrag && rest.isNotEmpty() && !rest.first().isWhitespace()) append(' ')
        }
        val cursor = kopf.length + fuege.length
        return z.copy(
            text = kopf + fuege + rest,
            auswahlStart = cursor,
            auswahlEnde = cursor,
            einfuegeMarke = z.einfuegeMarke + 1,
            fehler = null,
        )
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
        _bearbeitung.value = Bearbeitungszustand(
            notiz = notiz,
            ueberschrift = notiz.ueberschrift ?: "",
            text = notiz.text,
            // Der Cursor steht anfangs am Ende: das ist die Stelle, an der ohne weiteres
            // Zutun weitergesprochen wird.
            auswahlStart = notiz.text.length,
            auswahlEnde = notiz.text.length,
        )
    }

    fun setzeBearbeitung(ueberschrift: String, text: String) =
        _bearbeitung.update { it.copy(ueberschrift = ueberschrift, text = text) }

    /** Meldet jede Änderung im Textfeld samt Cursorstelle (B-08, Nachsprechen). */
    fun setzeBearbeitungText(text: String, auswahlStart: Int, auswahlEnde: Int) =
        _bearbeitung.update {
            it.copy(text = text, auswahlStart = auswahlStart, auswahlEnde = auswahlEnde)
        }

    fun schliesseBearbeitung() {
        // Eine laufende Aufnahme endet mit dem Blatt — sonst liefe das Mikrofon weiter und
        // ihr Transkript käme in ein Feld, das es nicht mehr gibt.
        if (_verlauf.value.nimmtAuf && aufnahmeziel == Aufnahmeziel.BEARBEITUNG) verwirfAufnahme()
        _bearbeitung.value = Bearbeitungszustand()
    }

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
        if (_verlauf.value.nimmtAuf && aufnahmeziel == Aufnahmeziel.KI_BLATT) {
            verwirfAufnahme()
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

    /**
     * Der Umschalter in der Kopfleiste: hell ↔ dunkel, **innerhalb derselben Familie**.
     *
     * Gold bleibt Gold, Neutral bleibt Neutral. Wer sich für die goldene Welt entschieden
     * hat, will beim Umschalten am Abend nicht plötzlich in der blauen landen — er will es
     * nur dunkler haben.
     */
    fun erscheinungUmschalten() {
        setzeErscheinung(
            when (Erscheinung.vonId(einstellungen.erscheinung)) {
                Erscheinung.HELL -> Erscheinung.DUNKEL
                Erscheinung.DUNKEL -> Erscheinung.HELL
                Erscheinung.GOLD_HELL -> Erscheinung.GOLD_DUNKEL
                Erscheinung.GOLD_DUNKEL -> Erscheinung.GOLD_HELL
            }.id,
        )
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
        _codexVerbunden.value = codex.isConnected
        _codexKonto.value = codex.email
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
        _codexVerbunden.value = false
        _codexKonto.value = null
        _anmeldung.value = Anmeldezustand()
        melde("Codex getrennt.")
    }

    // --- Einstellungen schreiben ------------------------------------------------------------------

    fun setzeModell(apiId: String) {
        einstellungen.codexModell = apiId
        _codexModell.value = apiId
    }

    fun setzeEffort(apiValue: String) {
        einstellungen.codexEffort = apiValue
        _codexEffort.value = apiValue
    }

    fun setzeWebsucheGrundhaltung(id: String) {
        einstellungen.websucheGrundhaltung = id
        _websucheGrundhaltung.value = id
    }

    fun setzeGroqSchluessel(wert: String) {
        einstellungen.groqSchluessel = wert.trim()
        _groq.value = wert.trim()
        // Was am fehlenden Schlüssel gescheitert ist, bekommt jetzt seine zweite Chance.
        if (wert.isNotBlank()) holeLiegengebliebeneNach()
    }

    /**
     * Notizen, die mangels Schlüssel nicht transkribiert wurden, in die Warteschlange
     * zurückholen. Sie stehen dann als „Wartet auf Netz" da und laufen von selbst durch.
     */
    private fun holeLiegengebliebeneNach() {
        viewModelScope.launch {
            val offene = repo.notizenOhneSchluessel()
            if (offene.isEmpty()) return@launch
            offene.forEach { notiz ->
                repo.aendere(notiz.copy(zustand = Notizzustand.WARTET_AUF_TRANSKRIPTION))
            }
            reicheWartendeNach()
        }
    }

    fun setzeGoogleSchluessel(wert: String) {
        einstellungen.googleTtsSchluessel = wert.trim()
        _google.value = wert.trim()
    }

    fun setzeQwenSchluessel(wert: String) {
        einstellungen.qwenSchluessel = wert.trim()
        _qwen.value = wert.trim()
        // Mit dem Schlüssel kommen die Stimmen: sonst müsste Frank raten, ob er richtig ist,
        // bis er das nächste Mal etwas vorlesen lässt.
        if (wert.isNotBlank()) ladeEigeneStimmen()
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

    /**
     * F-18 — die bei Alibaba hinterlegten eigenen Stimmen holen.
     *
     * Ohne Schlüssel gar nicht erst versuchen: der Aufruf käme mit einem Anmeldefehler
     * zurück, und der sähe aus wie ein Fehler der App.
     */
    fun ladeEigeneStimmen() {
        val schluessel = einstellungen.qwenSchluessel
        if (schluessel.isBlank()) {
            _eigeneStimmen.value = emptyList()
            return
        }
        if (_stimmenLaden.value) return
        _stimmenLaden.value = true
        viewModelScope.launch {
            val verzeichnis = QwenVoiceDirectory()
            try {
                val liste = verzeichnis.list(schluessel)
                _eigeneStimmen.value = liste
                // Steht noch keine Stimme fest, wird die jüngste vorbelegt — sonst zeigt die
                // Auswahl eine leere Kennung, obwohl Stimmen vorhanden sind.
                if (einstellungen.stimmeQwen.isBlank() && liste.isNotEmpty()) {
                    setzeTtsStimme(liste.first().id)
                }
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                melde(fehler.message ?: "Die Stimmen liessen sich nicht laden.")
            } finally {
                verzeichnis.shutdown()
                _stimmenLaden.value = false
            }
        }
    }

    /**
     * F-18 — eine neue eigene Stimme aufnehmen und bei Alibaba registrieren.
     *
     * Aufgenommen wird mit 44,1 kHz statt der 16 kHz der Diktate: der Stimmklon braucht die
     * höhere Auflösung, sonst klingt die erzeugte Stimme dumpf.
     */
    fun nimmStimmeAuf() {
        if (_nimmtStimmeAuf.value) {
            beendeStimmaufnahme()
            return
        }
        // Dasselbe von der anderen Seite: eine laufende Notiz-Aufnahme darf die Stimmprobe
        // nicht überschreiben.
        if (_verlauf.value.nimmtAuf) {
            melde("Erst die Notiz-Aufnahme beenden.")
            return
        }
        if (einstellungen.qwenSchluessel.isBlank()) {
            melde("Für die eigene Stimme fehlt der Alibaba-Schlüssel.")
            return
        }
        vorleser.halteAn()
        if (!mikrofon.start(viewModelScope, requestedSampleRate = 44_100)) {
            melde("Die Aufnahme liess sich nicht starten.")
            return
        }
        _nimmtStimmeAuf.value = true
    }

    private fun beendeStimmaufnahme() {
        _nimmtStimmeAuf.value = false
        viewModelScope.launch {
            val wav = mikrofon.stop()
            if (wav == null || wav.size < MINDESTGROESSE_STIMMPROBE) {
                melde("Zu kurz — sprich einige Sätze, damit die Stimme etwas hergibt.")
                return@launch
            }
            val enrollment = QwenVoiceEnrollment()
            try {
                _stimmenLaden.value = true
                val kennung = enrollment.create(einstellungen.qwenSchluessel, STIMMNAME, wav)
                setzeTtsStimme(kennung)
                setzeTtsAnbieter(TtsProvider.QWEN_CLONE.id)
                melde("Deine Stimme ist angelegt.")
                ladeEigeneStimmen()
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                melde(fehler.message ?: "Die Stimme liess sich nicht anlegen.")
            } finally {
                enrollment.shutdown()
                _stimmenLaden.value = false
            }
        }
    }

    /** F-18 — eine registrierte Stimme wieder löschen. */
    fun loescheEigeneStimme(kennung: String) {
        if (kennung.isBlank()) return
        viewModelScope.launch {
            val enrollment = QwenVoiceEnrollment()
            try {
                enrollment.delete(einstellungen.qwenSchluessel, kennung)
                if (einstellungen.stimmeQwen == kennung) {
                    // Ausdrücklich die Qwen-Stimme, nicht „die Stimme des gewählten
                    // Anbieters": beides fällt nur zufällig zusammen.
                    einstellungen.stimmeQwen = ""
                    if (einstellungen.ttsAnbieter == TtsProvider.QWEN_CLONE.id) _ttsStimme.value = ""
                }
                melde("Stimme gelöscht.")
                ladeEigeneStimmen()
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                melde(fehler.message ?: "Die Stimme liess sich nicht löschen.")
            } finally {
                enrollment.shutdown()
            }
        }
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

    private suspend fun fuehreSicherungAus(ordner: Uri, still: Boolean = false) {
        try {
            val quelle = repo.datenbankdatei()
            if (!quelle.exists()) {
                if (!still) melde("Es gibt noch nichts zu sichern.")
                return
            }
            val baum = DocumentFile.fromTreeUri(ctx, ordner)
            if (baum == null) {
                if (!still) melde("Auf den Ordner kann nicht zugegriffen werden.")
                return
            }
            val name = "gedankenspeicher-" + System.currentTimeMillis() + ".db"
            val ziel = baum.createFile("application/octet-stream", name)
            if (ziel == null) {
                if (!still) melde("Die Sicherungsdatei liess sich nicht anlegen.")
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
            if (!still) melde("Gesichert.")
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            if (!still) melde(fehler.message ?: "Die Sicherung ist fehlgeschlagen.")
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
        // Auch die Stimmprobe: sie lief bisher im Hintergrund weiter und hielt das
        // Mikrofon besetzt, bis die App wiederkam.
        if (_nimmtStimmeAuf.value) beendeStimmaufnahme()
        vorleser.halteAn()
        // F-17: die Sicherung läuft beim Schliessen, sofern sie eingeschaltet ist und ein
        // Ordner feststeht. Ohne diesen Aufruf war der Schalter eine blosse Absichtserklärung
        // — gesichert wurde nur auf ausdrücklichen Knopfdruck.
        if (einstellungen.driveSicherungAn && einstellungen.sicherungsordner.isNotBlank()) {
            viewModelScope.launch { fuehreSicherungAus(Uri.parse(einstellungen.sicherungsordner), still = true) }
        }
    }

    override fun onCleared() {
        netzWaechter?.let { waechter ->
            runCatching {
                ctx.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(waechter)
            }
        }
        vorleser.schliesse()
        mikrofon.release()
        super.onCleared()
    }

    private companion object {
        const val HOECHSTDAUER_MS = 10 * 60_000L

        /** Der feste Beispielsatz des Probe-Knopfs (F-18). */
        const val PROBESATZ = "So klinge ich, wenn ich dir deine Notizen vorlese."

        /** Eine Stimmprobe unter zwei Sekunden gibt keinen brauchbaren Klon her. */
        const val MINDESTGROESSE_STIMMPROBE = 44 + 44_100 * 2 * 2

        /** Der Name, unter dem eine neue eigene Stimme bei Alibaba steht. */
        const val STIMMNAME = "gedankenspeicher"

        /** Unter etwa 0,4 s bei 16 kHz Mono ist nichts Verwertbares dabei (F-01, Fehlerfall). */
        const val MINDESTGROESSE_WAV = 44 + 16_000 * 2 * 4 / 10
    }
}
