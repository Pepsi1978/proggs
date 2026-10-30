package de.frank.jarvis.speech

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.IdeenLog
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.tts.SpeechLoudness
import de.frank.jarvis.tts.TtsManager
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** Was der Lautsprecher-Knopf gerade anzeigt (Baustein D 4.4). */
enum class VorleseZustand { AUS, LAEDT, SPRICHT, PAUSIERT }

data class VorleseStand(
    val zustand: VorleseZustand = VorleseZustand.AUS,
    /** Kennung dessen, was gerade gelesen wird — damit nur der richtige Knopf leuchtet. */
    val quelle: String? = null,
    val titel: String = "",
    val absatzNummer: Int = 0,
    val absaetzeGesamt: Int = 0,
    /** Der Wortlaut des laufenden Absatzes — die Anzeige hebt ihn hervor (N.7). */
    val absatzText: String = "",
    val fehler: String? = null,
)

/**
 * Die Absatz-Pipeline aus Baustein D 4.2.
 *
 * Ein Absatz ist eine Vorlese-Einheit. Während Absatz *n* läuft, sind *n+1* und *n+2* bereits
 * beim Sprachdienst in Arbeit, sodass der nächste Ton ohne Loch anschliesst. Zwischen zwei
 * Absätzen liegt bei langen Texten rund eine Sekunde Atem, bei Gesprächsantworten weniger.
 * Der erste Absatz wird nach seinem ersten Satz geteilt, damit der erste Ton früh kommt.
 */
class Vorleser(
    context: Context,
    private val settings: SecureSettings,
) {
    private val appContext = context.applicationContext
    private val synthese = Synthese(appContext, settings)
    private val ttsManager = TtsManager(appContext)
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _stand = MutableStateFlow(VorleseStand())
    val stand: StateFlow<VorleseStand> = _stand.asStateFlow()

    private var laufenderJob: Job? = null

    /**
     * Zaehlt jeden Vorlese-Auftrag durch. Beim Wechsel auf eine andere Idee laeuft das
     * Aufraeumen des alten Auftrags erst, wenn der neue schon spricht — ohne diese Nummer
     * riss es dem neuen Lauf den Tonfokus, die Zwischendateien und den Dienst wieder weg.
     */
    private val laufNummer = java.util.concurrent.atomic.AtomicLong(0)
    private var spieler: MediaPlayer? = null
    private var fokusAnfrage: AudioFocusRequest? = null

    /** Wurde wegen eines Anrufs pausiert — danach wird von allein fortgesetzt. */
    private var pausiertDurchFokus = false

    /** Angehalten — gilt auch für den nächsten Absatz, nicht nur für den laufenden Player. */
    @Volatile private var pausiert = false
    private var zustandVorPause = VorleseZustand.SPRICHT

    /**
     * Kurzes holt den Tonfokus nur vorübergehend: Musik anderer Apps pausiert und läuft danach von selbst weiter.
     * Gesprächsantworten exklusiv (keine Hinweistöne währenddessen), lange Ablage-Texte dauerhaft.
     */
    private var fokusArt = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT

    /** Für die Messung, wie lange es bis zum ersten Ton dauert. */
    @Volatile private var laufBeginn = 0L
    @Volatile private var ersterTonGemessen = true

    /** [wahl]: eigene Stimme und eigenes Tempo nur für diesen Auftrag (Update-Ansagen), sonst wie eingestellt. */
    fun sprich(quelle: String, titel: String, rohText: String, wiederholen: Boolean = false, wahl: StimmWahl? = null) {
        if (_stand.value.quelle == quelle && _stand.value.zustand != VorleseZustand.AUS) {
            stopp()
            return
        }
        // Beim Wechsel auf eine andere Idee bleibt der Dienst stehen und wird gleich darauf
        // nur aktualisiert. Ihn zu beenden und im selben Atemzug neu zu starten, brachte die
        // App zum Absturz (ForegroundServiceDidNotStartInTimeException).
        stopp(dienstBeenden = false)
        val text = SprechText.fuerStimme(rohText)
        val absaetze = SprechText.absaetze(text)
        if (absaetze.isEmpty()) {
            _stand.value = VorleseStand(fehler = "Hier gibt es nichts zum Vorlesen.")
            return
        }
        _stand.value = VorleseStand(
            zustand = VorleseZustand.LAEDT,
            quelle = quelle,
            titel = titel,
            absatzNummer = 0,
            absaetzeGesamt = absaetze.size,
        )
        // Gesprächsantworten und Ansagen: kurzer Atem und nur vorübergehender Tonfokus.
        val kurz = quelle.startsWith("gespraech:") || quelle.startsWith("abfahrt:") || quelle.startsWith("ansage:") || quelle.startsWith("agent:") || quelle == "probe"
        val pauseMs = if (kurz) KURZE_PAUSE_MS else ABSATZ_PAUSE_MS
        // Exklusiv nur bei Gesprächsantworten: Solange Jarvis antwortet, spielt Android keine Hinweistöne (etwa die Claude-App beim
        // Start eines Programmierauftrags); die Meldungen erscheinen trotzdem, nur stumm. Längeres (Auswertung, Agent, Ablage) nicht,
        // es soll nicht minutenlang jeden Hinweiston schlucken. Ein ablaufender Timer beendet die Antwort (Timer.abgelaufen).
        fokusArt = when {
            quelle.startsWith("ablage:") -> AudioManager.AUDIOFOCUS_GAIN
            quelle.startsWith("gespraech:") -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            else -> AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        }
        VorleseDienst.starten(appContext)
        IdeenLog.info(
            "Vorleser",
            "sprich",
            "Vorlesen beginnt",
            mapOf("absaetze" to absaetze.size, "chars" to text.length, "quelle" to quelle.substringBefore(':')),
        )
        laufBeginn = System.currentTimeMillis()
        ersterTonGemessen = false
        val meinLauf = laufNummer.incrementAndGet()
        laufenderJob = bereich.launch {
            try {
                do {
                    if (synthese.kannVorausschauen(wahl)) pipelineMitVorausschau(absaetze, pauseMs, wahl) else reihum(absaetze, pauseMs)
                    // Derselbe abbrechbare Auftrag bleibt samt Dienst über alle Runden aktiv.
                    if (wiederholen) delay(ABSATZ_PAUSE_MS)
                } while (wiederholen)
                // Läuft schon die Fortsetzung (sprichNachVorab), gehört ihr der Zustand: Ein kurzes „aus“ öffnete sonst das Mikrofon.
                if (laufNummer.get() == meinLauf) _stand.value = _stand.value.copy(zustand = VorleseZustand.AUS, quelle = null)
                Logbuch.dauer("Vorlesen fertig", System.currentTimeMillis() - laufBeginn, "absaetze=${absaetze.size} zeichen=${text.length}")
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: SyntheseAbbruch) {
                IdeenLog.error("Vorleser", "sprich", "Pipeline angehalten", mapOf("grund" to fehler.javaClass.simpleName))
                Logbuch.fehler("Vorlesen angehalten", fehler)
                _stand.value = VorleseStand(fehler = fehler.message)
            } catch (fehler: Exception) {
                IdeenLog.error("Vorleser", "sprich", "Vorlesen fehlgeschlagen", mapOf("art" to fehler.javaClass.simpleName))
                Logbuch.fehler("Vorlesen fehlgeschlagen", fehler)
                _stand.value = VorleseStand(
                    fehler = fehler.message ?: "Das Vorlesen ist unterwegs abgebrochen.",
                )
            } finally {
                // Nur der jeweils juengste Lauf raeumt auf. Sonst nimmt der beendete Vorgaenger
                // dem neuen Vorlesen Tonfokus, Zwischendateien und Dienst wieder weg.
                if (laufNummer.get() == meinLauf) {
                    gibFokusFrei()
                    VorleseDienst.beenden(appContext)
                    synthese.raeumeAuf()
                }
            }
        }
    }

    /** Quelle und gesprochener Text des vorab gelesenen ersten Satzes; ein Stopp oder neues Vorlesen löscht beides. */
    private var vorabQuelle: String? = null
    private var vorabText = ""
    /** Der noch sprechende erste Satz, während die Fortsetzung schon läuft: Ein Stopp muss beide beenden. */
    private var vorabJob: Job? = null

    /** Liest den ersten Satz, während das Modell noch schreibt. Nur, wenn gerade nichts anderes läuft. */
    fun sprichVorab(quelle: String, titel: String, satz: String): Boolean {
        if (_stand.value.zustand != VorleseZustand.AUS) return false
        sprich(quelle, titel, satz)
        if (_stand.value.quelle != quelle) return false
        vorabQuelle = quelle
        vorabText = SprechText.fuerStimme(satz)
        return true
    }

    /**
     * Die ganze Antwort ist da: liest nach dem vorab gesprochenen Satz den Rest, ohne Pause im selben Dienst. Hat Frank das Vorlesen
     * inzwischen beendet, bleibt es still. Weicht der Text ab (Modellfehler, Neuversuch), kommt sicherheitshalber alles.
     */
    fun sprichNachVorab(vorab: String, quelle: String, titel: String, rohText: String) {
        if (vorabQuelle != vorab) return
        vorabQuelle = null
        val text = SprechText.fuerStimme(rohText)
        val rest = nachVorab(text, vorabText) ?: text
        val absaetze = SprechText.absaetze(rest)
        val vorher = laufenderJob
        vorabJob = vorher
        val spricht = _stand.value.zustand == VorleseZustand.SPRICHT
        if (absaetze.isEmpty()) return
        _stand.value = _stand.value.copy(zustand = if (spricht) VorleseZustand.SPRICHT else VorleseZustand.LAEDT, quelle = quelle, titel = titel, absaetzeGesamt = absaetze.size)
        VorleseDienst.starten(appContext)
        val meinLauf = laufNummer.incrementAndGet()
        laufenderJob = bereich.launch {
            try {
                // Der neue Text wird schon erzeugt, während der erste Satz noch spricht; abgespielt wird erst danach.
                val fertig = async { vorher?.join() }
                pipelineMitVorausschau(absaetze, KURZE_PAUSE_MS, null, fertig)
                if (laufNummer.get() == meinLauf) _stand.value = _stand.value.copy(zustand = VorleseZustand.AUS, quelle = null)
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: Exception) {
                Logbuch.fehler("Vorlesen fehlgeschlagen", fehler)
                _stand.value = VorleseStand(fehler = fehler.message ?: "Das Vorlesen ist unterwegs abgebrochen.")
            } finally {
                if (laufNummer.get() == meinLauf) {
                    gibFokusFrei()
                    VorleseDienst.beenden(appContext)
                    synthese.raeumeAuf()
                }
            }
        }
    }

    /** Der Text hinter [anfang]; verglichen werden nur Buchstaben und Ziffern, Satzzeichen dürfen abweichen. Null, wenn er anders beginnt. */
    private fun nachVorab(text: String, anfang: String): String? {
        var i = 0
        anfang.filter(Char::isLetterOrDigit).forEach { c ->
            while (i < text.length && !text[i].isLetterOrDigit()) i++
            if (i >= text.length || text[i] != c) return null
            i++
        }
        return text.substring(i).trimStart { !it.isLetterOrDigit() && it != '"' && it != '„' }
    }

    /** Baut die Verbindung zum Sprachdienst schon während der Aufnahme auf. */
    fun vorwaermen() = synthese.vorwaermen()

    /** Ein vorausgeschickter Erzeugungsauftrag samt Text — für den Wiederholversuch. */
    private class Auftrag(val text: String, val ergebnis: Deferred<Result<List<File>>>)

    /** Alle Anbieter erzeugen vorab (Edge inzwischen auch) — hier greift die Vorausschau. */
    private suspend fun pipelineMitVorausschau(absaetze: List<String>, pauseMs: Long, wahl: StimmWahl?, vorlauf: Deferred<Unit?>? = null) = coroutineScope {
        // Je Absatz ein oder (beim ersten) zwei Teile, die ohne Pause hintereinander laufen.
        val inArbeit = HashMap<Int, List<Auftrag>>()

        // Bewusst aus diesem Bereich, nicht aus dem langlebigen: So brechen die vorausgeschickten
        // Auftraege mit ab, sobald auf eine andere Idee gewechselt wird. Aus dem App-Bereich
        // heraus liefen sie weiter und schrieben in einen Ordner, den niemand mehr las.
        // Das Ergebnis kommt als Result: Ein gescheiterter Vorab-Auftrag darf den Bereich nicht
        // abbrechen und damit den gerade gesprochenen Absatz mitten im Satz abwürgen.
        fun erzeuge(teil: String): Auftrag = Auftrag(
            teil,
            // Sofort auf diesem Faden gestartet: So stellen sich die Teile in Lesereihenfolge an, und der erste Satz wartet nie hinter dem Rest.
            async(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                try {
                    Result.success(synthetisiereMitTeilung(teil, wahl))
                } catch (abbruch: CancellationException) {
                    throw abbruch
                } catch (fehler: Exception) {
                    Result.failure(fehler)
                }
            },
        )

        fun beauftrage(index: Int) {
            if (index !in absaetze.indices || index in inArbeit) return
            val absatz = absaetze[index]
            // Der erste Satz geht allein voraus, damit der erste Ton nicht auf den ganzen Absatz wartet.
            val schnitt = if (index == 0) SprechText.ersterSatzSchnitt(absatz) else null
            inArbeit[index] = if (schnitt != null) {
                listOf(erzeuge(absatz.substring(0, schnitt).trim()), erzeuge(absatz.substring(schnitt).trim()))
            } else {
                listOf(erzeuge(absatz))
            }
        }
        // Absatz 0 sowie die beiden folgenden gehen sofort in Arbeit.
        (0..VORAUSSCHAU).forEach(::beauftrage)

        vorlauf?.await()
        var fehlgeschlagen = 0
        absaetze.indices.forEach { index ->
            val teile = inArbeit.remove(index) ?: listOf(erzeuge(absaetze[index]))
            var angekuendigt = false
            var gesprochen = false
            teile.forEach { auftrag ->
                val dateien = hole(auftrag, wahl)
                if (!angekuendigt) {
                    angekuendigt = true
                    beauftrage(index + VORAUSSCHAU + 1)
                    warteSolangePausiert()
                    _stand.value = _stand.value.copy(
                        zustand = VorleseZustand.SPRICHT,
                        absatzNummer = index + 1,
                        absatzText = absaetze[index],
                    )
                    VorleseDienst.aktualisiere(
                        appContext,
                        _stand.value.titel,
                        index + 1,
                        absaetze.size,
                        pausiert = false,
                    )
                }
                dateien.forEach { datei ->
                    warteSolangePausiert()
                    spieleDatei(datei)
                    datei.delete()
                    gesprochen = true
                }
            }
            if (!gesprochen) fehlgeschlagen++
            if (index < absaetze.lastIndex) delay(pauseMs)
        }
        // Kam kein einziger Absatz durch, darf das nicht als stilles „fertig“ enden.
        if (fehlgeschlagen == absaetze.size) {
            error("Das Vorlesen kam nicht durch. Prüf die Internetverbindung.")
        }
    }

    /**
     * Wartet auf einen Vorab-Auftrag. Ist er gescheitert, etwa in einem Funkloch lange vor seiner
     * Zeit, wird er jetzt einmal direkt neu versucht; scheitert auch das, wird der Teil
     * übersprungen. Nur ein Abbruch der ganzen Sitzung (Schlüssel fehlt) hält alles an.
     */
    private suspend fun hole(auftrag: Auftrag, wahl: StimmWahl?): List<File> {
        val erstes = auftrag.ergebnis.await()
        erstes.getOrNull()?.let { return it }
        val grund = erstes.exceptionOrNull()
        if (grund is SyntheseAbbruch) throw grund
        // Ein Zeitlimit hat schon seine volle Frist gewartet: kein zweiter Versuch, sonst stünde das
        // Vorlesen (und mit ihm das Weckwort) noch einmal so lange still.
        if (grund is SyntheseZeitlimit) {
            IdeenLog.warn("Vorleser", "hole", "Absatz übersprungen (Zeitlimit)", mapOf("chars" to auftrag.text.length))
            Logbuch.fehler("Vorlese-Absatz übersprungen", grund, "zeichen=${auftrag.text.length}")
            return emptyList()
        }
        return try {
            withContext(Dispatchers.IO) { synthetisiereMitTeilung(auftrag.text, wahl) }
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (abbruch: SyntheseAbbruch) {
            throw abbruch
        } catch (zweiter: Exception) {
            IdeenLog.warn(
                "Vorleser",
                "hole",
                "Absatz übersprungen",
                mapOf("art" to zweiter.javaClass.simpleName, "chars" to auftrag.text.length),
            )
            Logbuch.fehler("Vorlese-Absatz übersprungen", zweiter, "zeichen=${auftrag.text.length}")
            emptyList()
        }
    }

    /**
     * Fällt ein Absatz beim Dienst durch, wird er halbiert und erneut geschickt
     * (Retry-Split, Baustein D 4.4). Ein reiner Netzfehler (Zeitlimit, Verbindungsabbruch) wird
     * einmal ungeteilt wiederholt. Hier wird nur erzeugt, nie abgespielt — die Vorausschau
     * läuft nebenher, und ein Abspielen von hier spräche mitten in den laufenden Absatz.
     */
    private suspend fun synthetisiereMitTeilung(absatz: String, wahl: StimmWahl?): List<File> = try {
        listOf(synthese.synthetisiere(absatz, wahl))
    } catch (zeitlimit: SyntheseZeitlimit) {
        // Weder halbieren noch wiederholen: Jeder weitere Versuch wartete erneut die volle Frist.
        throw zeitlimit
    } catch (netz: IOException) {
        IdeenLog.warn("Vorleser", "synthetisiereMitTeilung", "Netzfehler, neuer Versuch", mapOf("art" to netz.javaClass.simpleName))
        delay(NETZ_WIEDERHOLUNG_MS)
        listOf(synthese.synthetisiere(absatz, wahl))
    } catch (fehler: SyntheseFehler) {
        val mitte = absatz.length / 2
        val schnitt = absatz.lastIndexOf(' ', mitte).takeIf { it > 0 } ?: mitte
        IdeenLog.warn("Vorleser", "synthetisiereMitTeilung", "Absatz halbiert", mapOf("chars" to absatz.length))
        val ersteHaelfte = synthese.synthetisiere(absatz.substring(0, schnitt).trim(), wahl)
        // Die zweite Hälfte wird nach der ersten geholt; ein zweiter Fehlschlag zählt als echt.
        val zweiteHaelfte = try {
            synthese.synthetisiere(absatz.substring(schnitt).trim(), wahl)
        } catch (zweiter: Exception) {
            ersteHaelfte.delete()
            throw zweiter
        }
        listOf(ersteHaelfte, zweiteHaelfte)
    }

    /** Zwischen den Absätzen hält eine Pause an, bevor der nächste beginnt. */
    private suspend fun warteSolangePausiert() {
        while (pausiert) delay(PAUSE_PRUEF_MS)
    }

    /** Rückfall für Stimmen ohne Vorab-Erzeugung: Absatz für Absatz über den vorhandenen Player. */
    private suspend fun reihum(absaetze: List<String>, pauseMs: Long) {
        // Auch hier den Tonfokus holen — sonst liest die Stimme während eines Anrufs einfach weiter.
        holeFokus()
        var fehlgeschlagen = 0
        absaetze.forEachIndexed { index, absatz ->
            warteSolangePausiert()
            _stand.value = _stand.value.copy(
                zustand = VorleseZustand.SPRICHT,
                absatzNummer = index + 1,
                absatzText = absatz,
            )
            VorleseDienst.aktualisiere(appContext, _stand.value.titel, index + 1, absaetze.size, pausiert = false)
            suspendCancellableCoroutine { fortsetzung ->
                fortsetzung.invokeOnCancellation { ttsManager.stop() }
                ttsManager.speak(
                    text = absatz,
                    onStart = {},
                    onComplete = { if (fortsetzung.isActive) fortsetzung.resume(Unit) },
                    onError = { fehler ->
                        if (fortsetzung.isActive) {
                            fehlgeschlagen++
                            fortsetzung.resume(Unit)
                            IdeenLog.warn("Vorleser", "reihum", "Absatz übersprungen", mapOf("art" to fehler.javaClass.simpleName))
                        }
                    },
                )
            }
            if (index < absaetze.lastIndex) delay(pauseMs)
        }
        // Kam kein einziger Absatz durch, darf das nicht als stilles „fertig“ enden.
        if (fehlgeschlagen == absaetze.size) {
            error("Das Vorlesen kam nicht durch. Prüf die Internetverbindung.")
        }
    }

    private suspend fun spieleDatei(datei: File) {
        holeFokus()
        if (!ersterTonGemessen) {
            ersterTonGemessen = true
            Logbuch.sprache("Erster Ton", "anbieter=${settings.ttsProvider}", System.currentTimeMillis() - laufBeginn)
            de.frank.jarvis.observability.Antwortkette.ende()
        }
        // Zuerst anlegen und merken — wirft prepare(), wird der Player trotzdem freigegeben.
        val neuerPlayer = MediaPlayer()
        spieler = neuerPlayer
        try {
            suspendCancellableCoroutine { fortsetzung ->
                neuerPlayer.apply {
                    setAudioAttributes(SpeechLoudness.attributes)
                    // Hält die CPU wach, solange der Ton läuft — sonst stockt es bei dunklem Bildschirm.
                    setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
                    setOnCompletionListener { if (fortsetzung.isActive) fortsetzung.resume(Unit) }
                    setOnErrorListener { _, was, extra ->
                        IdeenLog.warn(
                            "Vorleser",
                            "spieleDatei",
                            "Wiedergabefehler",
                            mapOf("was" to was, "extra" to extra),
                        )
                        if (fortsetzung.isActive) fortsetzung.resume(Unit)
                        true
                    }
                    setDataSource(datei.absolutePath)
                    prepare()
                    // Dieselbe Lautstärke wie in den Playern der Anbieter.
                    SpeechLoudness.boost(this)
                    if (!pausiert) start()
                }
                fortsetzung.invokeOnCancellation { runCatching { neuerPlayer.stop() } }
            }
        } finally {
            SpeechLoudness.release(neuerPlayer)
            runCatching { neuerPlayer.release() }
            if (spieler === neuerPlayer) spieler = null
        }
    }

    /**
     * Wirkt auch zwischen zwei Absätzen und im Edge-Weg, wo gerade kein eigener Player läuft:
     * Das Merkzeichen hält den nächsten Absatz an.
     */
    fun pause() {
        if (pausiert || _stand.value.zustand == VorleseZustand.AUS) return
        pausiert = true
        zustandVorPause = _stand.value.zustand
        spieler?.let { runCatching { it.pause() } }
        ttsManager.pause()
        _stand.value = _stand.value.copy(zustand = VorleseZustand.PAUSIERT)
        VorleseDienst.aktualisiere(
            appContext,
            _stand.value.titel,
            _stand.value.absatzNummer,
            _stand.value.absaetzeGesamt,
            pausiert = true,
        )
    }

    fun weiter() {
        if (!pausiert) return
        // War der Tonfokus verweigert (Telefonat), jetzt erneut fragen — sonst bleibt es angehalten.
        if (fokusAnfrage == null && _stand.value.zustand != VorleseZustand.AUS && !fordereFokusAn()) return
        pausiert = false
        spieler?.let { runCatching { it.start() } }
        ttsManager.resume()
        _stand.value = _stand.value.copy(zustand = zustandVorPause)
        VorleseDienst.aktualisiere(
            appContext,
            _stand.value.titel,
            _stand.value.absatzNummer,
            _stand.value.absaetzeGesamt,
            pausiert = false,
        )
    }

    /**
     * @param dienstBeenden false, wenn gleich darauf weitergelesen wird — der Dienst laeuft dann
     *   durch und wird nur neu beschriftet, statt beendet und sofort wieder gestartet zu werden.
     */
    @JvmOverloads
    fun stopp(dienstBeenden: Boolean = true) {
        // Zuerst hochzaehlen: Damit weiss das finally des abgebrochenen Laufs, dass es nichts
        // mehr aufzuraeumen hat.
        laufNummer.incrementAndGet()
        vorabQuelle = null
        vorabJob?.cancel()
        vorabJob = null
        laufenderJob?.cancel()
        laufenderJob = null
        pausiert = false
        ttsManager.stop()
        spieler?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        spieler = null
        if (dienstBeenden) {
            gibFokusFrei()
            VorleseDienst.beenden(appContext)
        }
        // Die Zwischendateien des abgebrochenen Laufs sind hier noch die einzigen im Ordner —
        // der neue Lauf legt seine erst danach an.
        synthese.raeumeAuf()
        _stand.value = VorleseStand()
    }

    fun fehlerGelesen() {
        _stand.value = _stand.value.copy(fehler = null)
    }

    /** Holt den Tonfokus; bekommt das Vorlesen ihn nicht sofort, hält es an. */
    private fun holeFokus() {
        if (!fordereFokusAn()) pause()
    }

    /**
     * @return true, wenn sofort gesprochen werden darf. Bei verzögertem Fokus kommt das
     *   Weitersprechen später über den Zuhörer; bei verweigertem (Telefonat) bleibt es angehalten,
     *   bis Frank auf Weiter tippt.
     */
    private fun fordereFokusAn(): Boolean {
        if (fokusAnfrage != null) return true
        val zuhoerer = AudioManager.OnAudioFocusChangeListener { aenderung ->
            when (aenderung) {
                // Ein kurzer Ton einer anderen App (Hinweiston, der den Fokus mit Leiserstellen will): nicht anhalten, Android stellt
                // die Stimme dafür selbst kurz leiser. Sonst brach jeder fremde Piepton das Vorlesen ab.
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {}
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                -> if (_stand.value.zustand == VorleseZustand.SPRICHT ||
                    _stand.value.zustand == VorleseZustand.LAEDT
                ) {
                    pausiertDurchFokus = true
                    pause()
                }
                AudioManager.AUDIOFOCUS_GAIN -> if (pausiertDurchFokus) {
                    pausiertDurchFokus = false
                    weiter()
                }
                AudioManager.AUDIOFOCUS_LOSS -> stopp()
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val anfrage = AudioFocusRequest.Builder(fokusArt)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAcceptsDelayedFocusGain(true)
            .setOnAudioFocusChangeListener(zuhoerer)
            .build()
        // Vor der Anfrage merken, damit gibFokusFrei sie in jedem Fall wieder abmeldet.
        fokusAnfrage = anfrage
        return when (audioManager?.requestAudioFocus(anfrage)) {
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                // Das System meldet AUDIOFOCUS_GAIN später, der Zuhörer setzt dann fort.
                IdeenLog.info("Vorleser", "fordereFokusAn", "Tonfokus verzögert")
                pausiertDurchFokus = true
                false
            }
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                // Ohne Fokus kommt kein Zuhörer-Ruf — die Anfrage gilt als nicht gestellt.
                IdeenLog.warn("Vorleser", "fordereFokusAn", "Tonfokus verweigert")
                fokusAnfrage = null
                false
            }
            else -> true
        }
    }

    private fun gibFokusFrei() {
        val anfrage = fokusAnfrage ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) audioManager?.abandonAudioFocusRequest(anfrage)
        fokusAnfrage = null
        pausiertDurchFokus = false
    }

    fun herunterfahren() {
        stopp()
        ttsManager.shutdown()
    }

    companion object {
        /** Während Absatz n läuft, sind n+1 und n+2 schon in Arbeit. */
        const val VORAUSSCHAU = 2

        /** Hörbarer Atem zwischen zwei Absätzen, kein Loch. */
        const val ABSATZ_PAUSE_MS = 1000L

        /** Kürzerer Atem bei Gesprächsantworten und Ansagen — sonst wirkt Jarvis träge. */
        const val KURZE_PAUSE_MS = 400L

        /** Wartezeit vor dem Wiederholversuch nach einem Netzfehler. */
        private const val NETZ_WIEDERHOLUNG_MS = 500L

        /** Wie oft während einer Pause nachgesehen wird, ob es weitergeht. */
        private const val PAUSE_PRUEF_MS = 150L

        /** Der Vorleser lebt so lange wie die App — der Dienst hält ihn am Leben. */
        @Volatile private var geteilt: Vorleser? = null

        fun hole(context: Context, settings: SecureSettings): Vorleser =
            geteilt ?: synchronized(this) {
                geteilt ?: Vorleser(context, settings).also { geteilt = it }
            }

        internal fun aktuell(): Vorleser? = geteilt
    }
}

/** Der Intent, mit dem die Benachrichtigungsknöpfe zurück in die App sprechen. */
internal object VorleseAktion {
    const val PAUSE = "de.frank.jarvis.VORLESEN_PAUSE"
    const val WEITER = "de.frank.jarvis.VORLESEN_WEITER"
    const val STOPP = "de.frank.jarvis.VORLESEN_STOPP"

    /** Räumt nur den Dienst ab, ohne das Vorlesen noch einmal zu stoppen. */
    const val SCHLIESSEN = "de.frank.jarvis.VORLESEN_SCHLIESSEN"

    fun intent(context: Context, aktion: String): Intent =
        Intent(context, VorleseDienst::class.java).setAction(aktion)
}
