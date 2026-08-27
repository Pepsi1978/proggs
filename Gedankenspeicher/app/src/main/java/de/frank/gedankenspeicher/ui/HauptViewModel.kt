package de.frank.gedankenspeicher.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.frank.gedankenspeicher.audio.AufnahmeDienst
import de.frank.gedankenspeicher.audio.AufnahmeFernbedienung
import de.frank.gedankenspeicher.audio.MicRecorder
import de.frank.gedankenspeicher.hintergrund.AuswertungsDienst
import de.frank.gedankenspeicher.hintergrund.AuswertungsFernbedienung
import de.frank.gedankenspeicher.hintergrund.VorleseAnzeige
import de.frank.gedankenspeicher.hintergrund.VorleseDienst
import de.frank.gedankenspeicher.hintergrund.VorleseFernbedienung
import androidx.documentfile.provider.DocumentFile
import de.frank.gedankenspeicher.data.Anhang
import de.frank.gedankenspeicher.data.Anhangsspeicher
import de.frank.gedankenspeicher.data.alsJson
import de.frank.gedankenspeicher.data.anhaengeAusJson
import de.frank.gedankenspeicher.data.Auswertungsprofil
import de.frank.gedankenspeicher.data.Datenbank
import de.frank.gedankenspeicher.data.Sicherung
import de.frank.gedankenspeicher.data.KiAntwort
import de.frank.gedankenspeicher.data.Notiz
import de.frank.gedankenspeicher.data.Notizzustand
import de.frank.gedankenspeicher.data.Nachtraege
import de.frank.gedankenspeicher.data.Ordner
import de.frank.gedankenspeicher.data.Repository
import de.frank.gedankenspeicher.data.Sitzung
import de.frank.gedankenspeicher.data.Suchtreffer
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
import de.frank.gedankenspeicher.ui.verlauf.Reichtext
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

    // F-07 hat sein eigenes Modell: die Textverbesserung soll schnell sein, die Auswertung gründlich.
    private val _verbesserungModell = MutableStateFlow(einstellungen.verbesserungModell)
    val verbesserungModell: StateFlow<String> = _verbesserungModell

    private val _verbesserungEffort = MutableStateFlow(einstellungen.verbesserungEffort)
    val verbesserungEffort: StateFlow<String> = _verbesserungEffort

    private val _verbesserungPrompt = MutableStateFlow(einstellungen.verbesserungPrompt)
    val verbesserungPrompt: StateFlow<String> = _verbesserungPrompt

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
        _verlauf.update { it.copy(fingerabdruckAn = repo.einstellungen.fingerabdruckAn) }
        viewModelScope.launch {
            repo.legeProfileAnWennNoetig()
            repo.angefangeneAufraeumen()
            // **Beim Start wird bewusst keine Sitzung geöffnet.**
            //
            // Die App fing bisher dort an, wo sie zuletzt aufgehört hat (F-13). Bei einer
            // geschützten Sitzung hiess das: der erste Bildschirm ist eine Fingerabdruck-
            // Abfrage für etwas, das man vielleicht gar nicht wollte. Jetzt steht die
            // Auswahl offen, und die Sitzung sucht sich Frank selbst aus.
            _verlauf.update { it.copy(sitzung = null, laedt = false, eintraege = emptyList()) }
            reicheWartendeNach()
            holeFehlendeUeberschriften()
        }
        viewModelScope.launch {
            repo.sitzungen.collectLatest { liste -> _verlauf.update { it.copy(sitzungen = liste) } }
        }
        viewModelScope.launch {
            repo.letzteAktivitaeten.collectLatest { zeiten ->
                _verlauf.update { it.copy(letzteAktivitaet = zeiten) }
            }
        }
        viewModelScope.launch {
            repo.ordner.collectLatest { liste -> _verlauf.update { it.copy(ordner = liste) } }
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
                // Solange gesprochen wird, hält ein Vordergrunddienst die Wiedergabe am
                // Leben — sonst friert Android den Prozess ein, sobald eine andere App vorn
                // ist, und es wird mitten im Satz still.
                if (laeuft) VorleseDienst.starte(ctx) else VorleseDienst.beende(ctx)
            }
        }
        viewModelScope.launch {
            // Die Benachrichtigung soll „Anhalten" und „Weiter" richtig herum zeigen.
            vorleser.pausiert.collect { VorleseAnzeige.pausiert.value = it }
        }
        viewModelScope.launch {
            // Die Knöpfe der Vorlese-Benachrichtigung. Der Dienst kennt den Vorleser nicht.
            VorleseFernbedienung.befehle.collect { befehl ->
                when (befehl) {
                    VorleseFernbedienung.Befehl.UMSCHALTEN -> vorleser.umschalten()
                    VorleseFernbedienung.Befehl.STOPP -> vorleser.halteAn()
                }
            }
        }
        viewModelScope.launch {
            // Der Abbruch-Knopf der Auswertungs-Benachrichtigung.
            AuswertungsFernbedienung.abbruch.collect { auswertungJob?.cancel() }
        }
        viewModelScope.launch {
            // B-09: der Stopp-Knopf der Aufnahme-Benachrichtigung. Er kommt aus dem Dienst,
            // der die Aufnahme selbst nicht kennt — beendet wird sie immer nur hier.
            AufnahmeFernbedienung.stopp.collect { if (_verlauf.value.nimmtAuf) beendeAufnahme() }
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
        // Sitzung und Freigabe wechseln in **einem** Zug.
        //
        // Getrennt zu setzen hiess, dass es einen Augenblick lang die alte, geschuetzte
        // Sitzung ohne ihre Freigabe gab — und in genau diesem Augenblick fuhr die Sperre
        // hoch und fragte nach dem Fingerabdruck, obwohl man laengst in einer offenen
        // Sitzung war. Die Freigabe ueberlebt nur, wenn sie zu genau dieser Sitzung gehoert.
        _verlauf.update {
            it.copy(
                sitzung = sitzung,
                laedt = true,
                eintraege = emptyList(),
                freigegebeneSitzung = it.freigegebeneSitzung?.takeIf { frei -> frei == sitzung.id },
            )
        }
        verlaufJob = viewModelScope.launch {
            repo.verlauf(sitzung.id).collectLatest { eintraege ->
                val kontext = repo.alsKontext(
                    eintraege.filterNot {
                        it is Verlaufseintrag.NotizEintrag && it.notiz.zustand != Notizzustand.FERTIG
                    },
                )
                _verlauf.update {
                    it.copy(
                        eintraege = eintraege,
                        kontextZeilen = if (kontext.isBlank()) 0 else kontext.lineSequence().count(),
                        kontextWoerter = WORT.findAll(kontext).count(),
                        kontextTokens = if (kontext.isBlank()) 0 else (kontext.length + 3) / 4,
                        laedt = false,
                    )
                }
            }
        }
    }

    /**
     * Gibt genau diese Sitzung frei — nach erfolgreichem Fingerabdruck.
     *
     * Die Freigabe hängt an der Kennung, nicht an einem Schalter: sie endet von selbst,
     * sobald eine andere Sitzung geöffnet wird, und spätestens im Hintergrund.
     */
    fun gibFrei(id: Long) = _verlauf.update { it.copy(freigegebeneSitzung = id) }

    /** Ist der Inhalt dieser Sitzung gerade zu? */
    fun istGesperrt(sitzung: Sitzung?): Boolean =
        sitzung != null && sitzung.geschuetzt && _verlauf.value.freigegebeneSitzung != sitzung.id

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

    // --- Favoriten, Schutz, Papierkorb und Ordner ----------------------------------------------

    fun favoritUmschalten(sitzung: Sitzung) {
        viewModelScope.launch {
            repo.favoritUmschalten(sitzung.id)
            frischeOffeneSitzung(sitzung.id)
        }
    }

    /**
     * Schaltet den Schutz um. Der Fingerabdruck wurde vorher in der Oberfläche geprüft —
     * und weil er gerade gegeben wurde, bleibt die Notiz danach offen statt sich vor den
     * Augen dessen zuzusperren, der sie eben geschützt hat.
     */
    fun setzeSchutz(sitzung: Sitzung, geschuetzt: Boolean) {
        viewModelScope.launch {
            repo.setzeSchutz(sitzung.id, geschuetzt)
            frischeOffeneSitzung(sitzung.id)
            // Wer gerade den Fingerabdruck gegeben hat, um zu schuetzen, soll nicht im
            // selben Augenblick vor seiner eigenen Notiz stehen.
            if (geschuetzt) _verlauf.update { it.copy(freigegebeneSitzung = sitzung.id) }
            melde(
                if (geschuetzt) {
                    "Geschützt. Ab dem nächsten Öffnen braucht sie den Fingerabdruck."
                } else {
                    "Der Schutz wurde aufgehoben."
                },
            )
        }
    }

    fun inPapierkorb(sitzung: Sitzung) {
        val z = _verlauf.value
        if (z.nimmtAuf) return melde("Erst die Aufnahme beenden.")
        if (z.wertetAus) return melde("Die Auswertung läuft noch.")
        viewModelScope.launch {
            repo.setzePapierkorb(sitzung.id, true)
            if (sitzung.id == _verlauf.value.sitzung?.id) beobachteSitzung(repo.naechsteSichtbare())
            melde("Die Notiz liegt im Papierkorb.")
        }
    }

    fun ausPapierkorb(sitzung: Sitzung) {
        viewModelScope.launch {
            repo.setzePapierkorb(sitzung.id, false)
            melde("Die Notiz wurde wiederhergestellt.")
        }
    }

    fun leerePapierkorb() {
        viewModelScope.launch {
            repo.leerePapierkorb()
            melde("Der Papierkorb ist geleert.")
        }
    }

    fun verschiebeInOrdner(sitzung: Sitzung, ordnerId: Long?) {
        viewModelScope.launch {
            repo.verschiebeInOrdner(sitzung.id, ordnerId)
            frischeOffeneSitzung(sitzung.id)
        }
    }

    fun legeOrdnerAn(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.legeOrdnerAn(name) }
    }

    fun benenneOrdnerUm(ordner: Ordner, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.benenneOrdnerUm(ordner, name) }
    }

    fun loescheOrdner(ordner: Ordner) {
        viewModelScope.launch {
            repo.loescheOrdner(ordner.id)
            if (_verlauf.value.gewaehlterOrdner == ordner.id) {
                _verlauf.update { it.copy(ansicht = Schubladenansicht.ALLE, gewaehlterOrdner = null) }
            }
        }
    }

    fun waehleAnsicht(ansicht: Schubladenansicht) = _verlauf.update {
        it.copy(ansicht = ansicht, gewaehlterOrdner = if (ansicht == Schubladenansicht.ORDNER) it.gewaehlterOrdner else null)
    }

    fun waehleOrdner(id: Long) = _verlauf.update {
        it.copy(ansicht = Schubladenansicht.ORDNER, gewaehlterOrdner = id)
    }



    /**
     * Der Schalter in den Einstellungen entscheidet, ob neue Notizen geschützt werden
     * dürfen. Bereits geschützte bleiben geschützt: ein Schalter, der den Schutz
     * stillschweigend abräumt, wäre selbst die grösste Lücke.
     */
    fun setzeFingerabdruck(an: Boolean) {
        repo.einstellungen.fingerabdruckAn = an
        _verlauf.update { it.copy(fingerabdruckAn = an) }
    }

    /** Nach einer Änderung an der offenen Sitzung deren Kopf im Zustand nachziehen. */
    private suspend fun frischeOffeneSitzung(id: Long) {
        if (id != _verlauf.value.sitzung?.id) return
        repo.offeneSitzung().let { s -> _verlauf.update { it.copy(sitzung = s) } }
    }

    // --- Notiz tippen (F-02) -------------------------------------------------------------------

    fun setzeEntwurf(text: String) = _verlauf.update { it.copy(entwurf = text) }

    // --- Anhänge (Plus-Menü) -------------------------------------------------------------------

    /** Nimmt einen fertigen Anhang in den Entwurf auf. */
    fun fuegeAnhangHinzu(anhang: Anhang) =
        _verlauf.update { it.copy(anhaenge = it.anhaenge + anhang) }

    /** Nimmt einen Anhang aus dem Entwurf und räumt seine Datei gleich mit weg. */
    fun entferneAnhang(anhang: Anhang) {
        _verlauf.update { it.copy(anhaenge = it.anhaenge.filterNot { vorhanden -> vorhanden.id == anhang.id }) }
        Anhangsspeicher(ctx).loesche(listOf(anhang))
    }

    fun meldeFehler(text: String) = _verlauf.update { it.copy(meldung = text) }

    /**
     * Ersetzt einen Anhang in einer schon gespeicherten Notiz — für den eigenen Titel einer
     * Sprachaufnahme und für die nachträglich bearbeitete Tabelle.
     */
    fun aendereAnhang(notiz: Notiz, anhang: Anhang) {
        viewModelScope.launch {
            val vorhanden = repo.notiz(notiz.id) ?: return@launch
            val neue = anhaengeAusJson(vorhanden.anhaengeJson).map { einer ->
                if (einer.id == anhang.id) anhang else einer
            }
            repo.aendere(vorhanden.copy(anhaengeJson = neue.alsJson()))
        }
    }

    /**
     * Hängt einen frischen Anhang an eine **schon gespeicherte** Notiz — das Plus an der
     * Karte. Dieselbe Technik wie beim Entwurf: die Datei liegt im App-Speicher, die Notiz
     * merkt sich nur ihre Beschreibung.
     */
    fun fuegeAnhangZuNotiz(notiz: Notiz, anhang: Anhang) {
        viewModelScope.launch {
            val vorhanden = repo.notiz(notiz.id) ?: return@launch
            val neue = anhaengeAusJson(vorhanden.anhaengeJson) + anhang
            repo.aendere(vorhanden.copy(anhaengeJson = neue.alsJson()))
        }
    }

    fun sendeEntwurf() {
        val text = _verlauf.value.entwurf.trim()
        val anhaenge = _verlauf.value.anhaenge
        if (text.isEmpty() && anhaenge.isEmpty()) return
        _verlauf.update { it.copy(entwurf = "", anhaenge = emptyList()) }
        viewModelScope.launch {
            // Wer ohne offene Sitzung lostippt, bekommt eine neue — der leere Anfangs-
            // bildschirm soll keine Sackgasse sein.
            val sitzung = _verlauf.value.sitzung ?: repo.neueSitzung().also(::beobachteSitzung)
            val id = repo.legeGetippteNotizAn(sitzung.id, text, anhaenge)
            // Überschrift und Sitzungstitel entstehen aus dem Text; ohne Text gibt es
            // nichts zu benennen — die Anhänge sprechen dann für sich.
            if (text.isNotEmpty()) versorgeNeueNotiz(id, sitzung.id, text)
        }
    }

    // --- Notiz einsprechen (F-01) ---------------------------------------------------------------

    fun mikrofonAbgelehnt() = _verlauf.update { it.copy(mikrofonAbgelehnt = true) }

    fun mikrofonErlaubt() = _verlauf.update { it.copy(mikrofonAbgelehnt = false) }

    /** Ein Tipp startet, ein zweiter beendet — es wird nicht gehalten (F-01, Auslöser). */
    fun aufnahmeUmschalten() {
        if (_verlauf.value.nimmtAuf) {
            beendeAufnahme()
        } else if (_verlauf.value.sitzung == null) {
            // Dasselbe beim Mikrofon: erst eine Sitzung, dann läuft die Aufnahme.
            viewModelScope.launch {
                beobachteSitzung(repo.neueSitzung())
                aufnahmeziel = Aufnahmeziel.VERLAUF
                starteAufnahme()
            }
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
        // B-09: erst der Vordergrunddienst, dann das Mikrofon. Andersherum nähme Android der
        // App das Mikrofon in dem Moment, in dem Frank die App verlässt.
        AufnahmeDienst.starte(ctx)
        if (!mikrofon.start(viewModelScope)) {
            AufnahmeDienst.beende(ctx)
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
        AufnahmeDienst.beende(ctx)
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
        AufnahmeDienst.beende(ctx)
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
            // Einmal nachgetragen bleibt nachgetragen — auch wenn danach noch mitten im
            // Text etwas eingeschoben wird.
            nachgetragen = z.nachgetragen || nachtrag,
            // Jeder Nachtrag merkt sich, wo er beginnt und wann er gesprochen wurde —
            // daraus entsteht beim Speichern seine eigene Überschriftenzeile.
            nachtragsStellen =
                if (nachtrag) {
                    z.nachtragsStellen + (kopf.length + 2 to System.currentTimeMillis())
                } else {
                    z.nachtragsStellen
                },
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
                // Abschnitt für Abschnitt verbessern, nicht den ganzen Text auf einmal:
                // Die Nachtragszeilen mit ihren Zeitpunkten bleiben unangetastet stehen,
                // und jeder Abschnitt wird genau dort wieder eingesetzt, wo er war. So
                // bleibt der Nachtrag nach der Verbesserung ein Nachtrag.
                val neu = buildString {
                    Nachtraege.abschnitte(notiz.text).forEachIndexed { nr, abschnitt ->
                        if (nr > 0) append("\n\n")
                        val verbessert = runCatching { repo.verbessere(abschnitt.text.trim()) }
                            .getOrNull()
                            ?.takeIf(String::isNotBlank)
                            ?: abschnitt.text
                        if (abschnitt.nachtragVom != null) {
                            append(Nachtraege.zeileVon(abschnitt.nachtragVom)).append('\n')
                        }
                        append(verbessert)
                    }
                }
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
            // Vor jeden frischen Nachtrag kommt seine Überschriftenzeile. Von hinten nach
            // vorn einsetzen, damit die gemerkten Stellen beim Einfügen nicht verrutschen.
            var text = z.text
            val zeiten = mutableListOf<Long>()
            for ((stelle, zeit) in z.nachtragsStellen.sortedByDescending { it.first }) {
                if (stelle > text.length) continue
                // Nur wenn wirklich etwas dasteht: ein leerer Nachtrag bekommt keine Zeile.
                val naechste = z.nachtragsStellen.map { it.first }.filter { it < stelle }.maxOrNull()
                    ?: text.length
                if (text.substring(stelle, naechste.coerceAtMost(text.length)).isBlank()) continue
                text = text.substring(0, stelle) + Nachtraege.zeile(zeit) + "\n" +
                    text.substring(stelle)
                zeiten += zeit
            }
            repo.bearbeiteNotiz(notiz, z.ueberschrift, text, neueNachtragZeiten = zeiten)
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
            ladeKontextUndFrage(sitzung.id)
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

    fun setzeWebsuche(an: Boolean) = _kiBlatt.update { it.copy(websuche = an, websucheKiEntscheidet = false) }

    fun setzeWebsucheKiEntscheidet() =
        _kiBlatt.update { it.copy(websucheKiEntscheidet = true, websuche = false) }

    fun setzeKiAntwort(text: String) = _kiBlatt.update { it.copy(antwort = text) }

    private suspend fun ladeKontextUndFrage(sitzungId: Long) {
        val eintraege = repo.kontextEintraege(sitzungId)
        _kiBlatt.update {
            it.copy(
                kontextzahl = eintraege.size,
                rueckfrage = "",
                fehler = null,
            )
        }
        if (eintraege.isEmpty()) return
        if (!codex.isConnected) {
            _kiBlatt.update { it.copy(codexFehlt = true, holtFrage = false) }
            return
        }
        _kiBlatt.update { it.copy(holtFrage = true, codexFehlt = false) }
        try {
            val frage = repo.holeRueckfrage(repo.alsKontext(eintraege))
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
        viewModelScope.launch { ladeKontextUndFrage(sitzung.id) }
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
        val websuche = blatt.websuche || blatt.websucheKiEntscheidet
        val rueckfrage = blatt.rueckfrage
        _kiBlatt.value = KiBlattzustand()
        _verlauf.update { it.copy(wertetAus = true) }
        // Eine gründliche Auswertung dauert Minuten. Ohne Vordergrunddienst friert Android
        // den Prozess ein, sobald Frank in eine andere App wechselt, und schneidet die
        // offene Verbindung ab — man kam zurück und es war nichts da.
        AuswertungsDienst.starte(ctx)

        auswertungJob = viewModelScope.launch {
            try {
                val eintraege = repo.kontextEintraege(sitzung.id)
                val profil = repo.holeAktivesProfil()
                // Quellenangaben werden hier gestrichen, nicht erst beim Anzeigen: was
                // gespeichert ist, ist auch das, was exportiert und vorgelesen wird.
                val text = Reichtext.ohneQuellen(
                    repo.holeAuswertung(
                        notizen = repo.alsKontext(eintraege),
                        rueckfrage = rueckfrage,
                        antwort = antwort,
                        profilAnweisung = profil?.anweisung.orEmpty(),
                        websuche = websuche,
                    ),
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
                        ganzeSitzung = true,
                    ),
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                melde(fehler.message ?: "Die Auswertung ist nicht durchgekommen.")
            } finally {
                _verlauf.update { it.copy(wertetAus = false) }
                AuswertungsDienst.beende(ctx)
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
            _suche.update { it.copy(treffer = ohneGesperrte(treffer), sucht = false) }
        }
    }

    fun leereSuche() { _suche.value = Suchzustand() }

    /**
     * Nimmt Treffer aus geschützten Sitzungen heraus, solange der Fingerabdruck aussteht.
     *
     * Ohne das wäre der Schutz über die Suche zu umgehen: der Trefferausschnitt zeigt den
     * Notiztext, und genau der soll ja zu bleiben.
     */
    private fun ohneGesperrte(treffer: List<Suchtreffer>): List<Suchtreffer> {
        val z = _verlauf.value
        val gesperrt = z.sitzungen
            .filter { it.geschuetzt && it.id != z.freigegebeneSitzung }
            .map { it.id }
            .toSet()
        return treffer.filterNot { it.sitzungId in gesperrt }
    }

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

    fun setzeVerbesserungModell(apiId: String) {
        einstellungen.verbesserungModell = apiId
        _verbesserungModell.value = apiId
    }

    fun setzeVerbesserungEffort(apiValue: String) {
        einstellungen.verbesserungEffort = apiValue
        _verbesserungEffort.value = apiValue
    }

    fun setzeVerbesserungPrompt(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty()) return
        einstellungen.verbesserungPrompt = prompt
        _verbesserungPrompt.value = prompt
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

    /** „Ohne Profil": die Auswertung läuft allein mit dem Grundauftrag. */
    fun deaktiviereProfile() {
        viewModelScope.launch { repo.deaktiviereProfile() }
    }

    fun speichereProfil(profil: Auswertungsprofil) {
        viewModelScope.launch {
            repo.speichereProfil(profil)
            // Wird dem aktiven Profil der Text genommen, hat es der KI nichts mehr zu
            // sagen — dann steht die Auswertung eben ohne Profil da.
            if (profil.istAktiv && profil.anweisung.isBlank()) {
                repo.deaktiviereProfile()
                melde("Ein Profil ohne Text kann nicht aktiv sein — die KI hat jetzt freie Hand.")
            }
        }
    }

    fun setzeProfilZurueck(profil: Auswertungsprofil) {
        viewModelScope.launch { repo.setzeProfilZurueck(profil.nummer, profil.istAktiv) }
    }

    /**
     * Eine Auswertung nachträglich bearbeiten. Läuft sie gerade als Vorlesung, hört diese
     * auf: der gesprochene Text wäre sonst ein anderer als der, der auf der Karte steht.
     */
    fun aendereAntwort(antwort: KiAntwort) {
        if (_verlauf.value.liestVor == "antwort:" + antwort.id) {
            vorleser.halteAn()
            _verlauf.update { it.copy(liestVor = null, vorleseAbsatz = -1) }
        }
        viewModelScope.launch { repo.aendereAntwort(antwort) }
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

    /**
     * Es gibt genau **zwei** Sicherungen: die neueste und die davor.
     *
     * Beide haben einen festen Namen. Ein Zeitstempel im Namen hiesse, dass jede Sicherung
     * eine neue Datei anlegt und der Bestand nur durch nachträgliches Löschen begrenzt bleibt
     * — und weil die Sicherung bei jedem Schliessen der App läuft, häuften sich die Dateien
     * genau dann, wenn das Löschen im Drive-Ordner einmal nicht durchkam. Mit festen Namen
     * kann sich nichts anhäufen: es wird immer in dieselben zwei Dateien geschrieben.
     */
    private suspend fun fuehreSicherungAus(ordner: Uri, still: Boolean = false) {
        try {
            val baum = DocumentFile.fromTreeUri(ctx, ordner)
            if (baum == null || !baum.canWrite()) {
                if (!still) melde("Auf den Sicherungsordner kann nicht zugegriffen werden.")
                return
            }

            val steckbrief = withContext(Dispatchers.IO) {
                // **Erst vollstaendig danebenlegen, dann erst die alte Sicherung anfassen.**
                //
                // Vorher wurde die bestehende Sicherung sofort verschoben und ueberschrieben
                // und erst dabei zeigte sich, ob das Schreiben ueberhaupt durchkommt. Riss es
                // ab, war die alte Sicherung schon fort und die neue halb — beide unbrauchbar.
                // Jetzt entsteht die Sicherung zuerst im Zwischenspeicher der App, wird dort
                // geprueft, und nur eine geprueft heile Datei wandert in den Ordner.
                val entwurf = File(ctx.cacheDir, "sicherung-entwurf.zip")
                entwurf.delete()
                val brief = entwurf.outputStream().use { aus ->
                    Sicherung.packe(ctx, db, einstellungen, codex.alleWerte(), aus)
                }
                if (entwurf.length() == 0L) throw IllegalStateException("Die Sicherung blieb leer.")

                try {
                    // Die bisherige Sicherung rutscht eine Stelle nach hinten — aber nur,
                    // wenn das auch wirklich gelingt. Schlaegt es fehl, bleibt alles stehen
                    // wie es war, statt beide Staende zu verlieren.
                    val bisher = baum.findFile(SICHERUNG_AKTUELL)
                    if (bisher != null && bisher.length() > 0) {
                        val vorher = baum.findFile(SICHERUNG_VORHER)
                            ?: baum.createFile("application/octet-stream", SICHERUNG_VORHER)
                        if (vorher != null) {
                            val kopiert = schreibe(vorher.uri) { aus ->
                                ctx.contentResolver.openInputStream(bisher.uri)?.use { ein ->
                                    ein.copyTo(aus)
                                } ?: throw IllegalStateException(
                                    "Die bisherige Sicherung liess sich nicht lesen.",
                                )
                            }
                            if (kopiert != bisher.length()) {
                                throw IllegalStateException(
                                    "Die bisherige Sicherung liess sich nicht zur Seite legen.",
                                )
                            }
                        }
                    }

                    val ziel = bisher
                        ?: baum.createFile("application/octet-stream", SICHERUNG_AKTUELL)
                        ?: throw IllegalStateException("Die Sicherungsdatei liess sich nicht anlegen.")
                    val geschrieben = schreibe(ziel.uri) { aus ->
                        entwurf.inputStream().use { ein -> ein.copyTo(aus) }
                    }
                    // Nachgezaehlt, nicht gehofft: eine abgerissene Uebertragung in den
                    // Drive-Ordner sah bisher wie eine gelungene Sicherung aus.
                    if (geschrieben != entwurf.length()) {
                        throw IllegalStateException(
                            "Die Sicherung kam unvollstaendig an (${geschrieben} von ${entwurf.length()} Bytes).",
                        )
                    }
                    einstellungen.letzteSicherungGroesse = entwurf.length()
                } finally {
                    entwurf.delete()
                }
                brief
            }

            einstellungen.letzteSicherungZeit = System.currentTimeMillis()
            if (!still) melde("Gesichert: ${steckbrief.beschreibung()}.")
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            android.util.Log.w("Sicherung", "fehlgeschlagen", fehler)
            if (!still) melde(fehler.message ?: "Die Sicherung ist fehlgeschlagen.")
        }
    }

    /**
     * Schreibt in eine bestehende Datei — mit `"wt"`, also **abschneidend**.
     *
     * Der blosse Modus `"w"` kürzt bei manchen Anbietern nicht: wird eine kleinere Datenbank
     * über eine grössere geschrieben, bliebe der Rest der alten am Ende stehen. Die Sicherung
     * sähe heil aus und wäre beim Wiederherstellen unbrauchbar.
     */
    private fun schreibe(ziel: Uri, inhalt: (java.io.OutputStream) -> Unit): Long {
        val strom = ctx.contentResolver.openOutputStream(ziel, "wt")
            ?: ctx.contentResolver.openOutputStream(ziel)
            ?: throw IllegalStateException("In die Sicherungsdatei liess sich nicht schreiben.")
        // Mitgezaehlt wird beim Schreiben, nicht hinterher abgefragt: `DocumentFile.length()`
        // kommt bei Drive erst mit Verzoegerung nach und meldete eine halbe Datei als heil.
        val zaehler = ZaehlenderStrom(strom)
        zaehler.use(inhalt)
        return zaehler.gezaehlt
    }

    /** Zaehlt mit, wie viel wirklich durchging. */
    private class ZaehlenderStrom(private val darunter: java.io.OutputStream) : java.io.OutputStream() {
        var gezaehlt = 0L
            private set

        override fun write(b: Int) {
            darunter.write(b)
            gezaehlt++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            darunter.write(b, off, len)
            gezaehlt += len
        }

        override fun flush() = darunter.flush()

        override fun close() {
            runCatching { darunter.flush() }
            darunter.close()
        }
    }

    // Das frühere `raeumeAlteSicherungenWeg` ist ersatzlos gestrichen. Es löschte **jede**
    // Datei im Sicherungsordner, deren Name mit `gedankenspeicher-` begann und nicht einer
    // der beiden festen war — auch eine von Hand danebengelegte Kopie. Genau die will man
    // aber behalten, wenn eine Sicherung einmal nicht stimmt. Die Aufräumarbeit von damals
    // ist längst getan; das Risiko blieb.

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
                val arbeitsordner = File(ctx.cacheDir, "wiederherstellung")

                // **Erst pruefen, dann anfassen.**
                //
                // Vorher wurde die laufende Datenbank geschlossen und ersetzt und erst
                // danach zeigte sich, ob die gewaehlte Datei ueberhaupt etwas taugt. Griff
                // man daneben — eine leere Datei, ein halber Download, die falsche Datei —,
                // war der bisherige Stand mit fort. Jetzt wird die Sicherung ausgepackt,
                // als SQLite geoeffnet und von ihr selbst auf Unversehrtheit geprueft,
                // bevor irgendetwas ersetzt wird.
                val befund = withContext(Dispatchers.IO) {
                    val strom = ctx.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Die Sicherungsdatei liess sich nicht lesen.")
                    Sicherung.pruefe(strom, arbeitsordner)
                }
                if (befund is Sicherung.Befund.Untauglich) {
                    withContext(Dispatchers.IO) { arbeitsordner.deleteRecursively() }
                    return@launch melde(befund.grund)
                }

                vorleser.halteAn()
                if (_verlauf.value.nimmtAuf) beendeAufnahme()
                auswertungJob?.cancel()
                verlaufJob?.cancel()

                val ziel = repo.datenbankdatei()
                val bericht = withContext(Dispatchers.IO) {
                    try {
                        Datenbank.schliesse()
                        // `-wal` und `-shm` muessen weg: bleiben sie von der alten Datenbank
                        // stehen, haelt SQLite sie fuer den gueltigen juengsten Stand und
                        // ueberschreibt die wiederhergestellte Datei damit — die
                        // Wiederherstellung saehe dann aus, als waere nichts passiert.
                        listOf("-wal", "-shm").forEach { anhang ->
                            runCatching { File(ziel.absolutePath + anhang).delete() }
                        }

                        when (befund) {
                            is Sicherung.Befund.NurDatenbank -> {
                                befund.datei.inputStream().use { ein ->
                                    ziel.outputStream().use { aus -> ein.copyTo(aus) }
                                }
                                // Eine Sicherung aus der Zeit vor dem vollstaendigen Format:
                                // die Anhaenge dieses Geraets bleiben stehen, denn sie sind
                                // alles, was es davon noch gibt.
                                "Wiederhergestellt — eine aeltere Sicherung ohne Anhaenge und Einstellungen."
                            }

                            is Sicherung.Befund.Archiv -> {
                                File(befund.ordner, Sicherung.EINTRAG_DATENBANK).inputStream().use { ein ->
                                    ziel.outputStream().use { aus -> ein.copyTo(aus) }
                                }

                                // Die Anhaenge gehoeren zur Datenbank: bleiben alte stehen,
                                // zeigen sie auf Notizen, die es nicht mehr gibt, und die
                                // wiederhergestellten fehlten. Deshalb komplett ersetzt.
                                val anhangziel = File(ctx.filesDir, Anhangsspeicher.ORDNER)
                                val anhangquelle = File(befund.ordner, Sicherung.ORDNER_ANHAENGE)
                                anhangziel.deleteRecursively()
                                anhangziel.mkdirs()
                                anhangquelle.listFiles()?.forEach { datei ->
                                    runCatching { datei.copyTo(File(anhangziel, datei.name), overwrite = true) }
                                }

                                File(befund.ordner, Sicherung.EINTRAG_EINSTELLUNGEN)
                                    .takeIf { it.exists() }
                                    ?.let { einstellungen.uebernimm(Sicherung.werteAusJson(it.readText())) }

                                File(befund.ordner, Sicherung.EINTRAG_CODEX)
                                    .takeIf { it.exists() }
                                    ?.let { codex.uebernimm(Sicherung.werteAusJson(it.readText())) }

                                "Wiederhergestellt: ${befund.steckbrief.beschreibung()}."
                            }

                            is Sicherung.Befund.Untauglich -> error("schon oben behandelt")
                        }
                    } finally {
                        arbeitsordner.deleteRecursively()
                    }
                }
                melde(bericht)
                _neustartNoetig.value = true
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                android.util.Log.w("Sicherung", "Wiederherstellung fehlgeschlagen", fehler)
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
        // Der Schutz schliesst sich wieder, sobald die App aus dem Blick ist. Ohne das
        // gälte ein einziger Fingerabdruck bis zum nächsten Neustart der App.
        _verlauf.update { it.copy(freigegebeneSitzung = null) }
        // B-09: die Notiz-Aufnahme läuft bewusst weiter. Wer etwas nachschlagen geht, während
        // er spricht, soll weitersprechen können; beendet wird sie in der App oder über die
        // Benachrichtigung. Der Vordergrunddienst hält das Mikrofon so lange offen.
        // Auch die Stimmprobe: sie lief bisher im Hintergrund weiter und hielt das
        // Mikrofon besetzt, bis die App wiederkam.
        if (_nimmtStimmeAuf.value) beendeStimmaufnahme()
        // Das Vorlesen läuft bewusst weiter — wie die Aufnahme. Wer während einer langen
        // Auswertung etwas nachschlägt, soll den Faden nicht mitten im Satz verlieren;
        // beendet wird es in der App oder über die Benachrichtigung. Der Vordergrunddienst
        // (`VorleseDienst`) hält die Wiedergabe so lange offen.
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
        // Stirbt das ViewModel, gibt es niemanden mehr, der die Aufnahme beenden könnte —
        // die Benachrichtigung bliebe sonst als Geist stehen.
        AufnahmeDienst.beende(ctx)
        VorleseDienst.beende(ctx)
        AuswertungsDienst.beende(ctx)
        super.onCleared()
    }

    private companion object {
        val WORT = Regex("\\S+")
        const val HOECHSTDAUER_MS = 10 * 60_000L

        /** Der feste Beispielsatz des Probe-Knopfs (F-18). */
        const val PROBESATZ = "So klinge ich, wenn ich dir deine Notizen vorlese."

        /** Eine Stimmprobe unter zwei Sekunden gibt keinen brauchbaren Klon her. */
        const val MINDESTGROESSE_STIMMPROBE = 44 + 44_100 * 2 * 2

        /** Der Name, unter dem eine neue eigene Stimme bei Alibaba steht. */
        const val STIMMNAME = "gedankenspeicher"

        /** Unter etwa 0,4 s bei 16 kHz Mono ist nichts Verwertbares dabei (F-01, Fehlerfall). */
        const val MINDESTGROESSE_WAV = 44 + 16_000 * 2 * 4 / 10

        /** F-17: die beiden Sicherungen — die neueste und die davor. Mehr werden es nie. */
        const val SICHERUNG_AKTUELL = "gedankenspeicher-aktuell.db"
        const val SICHERUNG_VORHER = "gedankenspeicher-vorher.db"
    }
}
