package de.frank.jarvis.mentalitaet

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import de.frank.jarvis.atem.AtemSitzung
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.SprechText
import de.frank.jarvis.speech.Synthese
import de.frank.jarvis.speech.SyntheseAbbruch
import de.frank.jarvis.speech.SyntheseZeitlimit
import de.frank.jarvis.speech.VorleseDienst
import de.frank.jarvis.speech.VorleseZustand
import de.frank.jarvis.speech.Vorleser
import de.frank.jarvis.tts.SpeechLoudness
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/** Aktuelle Phase der Mentalität-Vorleseschleife. */
enum class MentalPhase { AUS, LAEDT, SPRICHT, PAUSE }

/** Ein einzelner Sprechschritt im Durchlauf (z. B. Satz B, Wiederholung 2 von 3). */
data class MentalSchritt(
    val satzId: Long,
    val text: String,
    val wiederholung: Int,
    val wiederholungenGesamt: Int,
)

/** Beobachtbarer Zustand für die Oberfläche. */
data class MentalSpielStand(
    val phase: MentalPhase = MentalPhase.AUS,
    val aktiverSatzId: Long? = null,
    val wiederholung: Int = 0,
    val wiederholungenGesamt: Int = 0,
    val durchlauf: Int = 0,
    val pauseSekunden: Int = STANDARD_PAUSE_SEKUNDEN,
    val fehler: String? = null,
    /** Vom Pause-/Weiter-Knopf angehalten; die Phase bleibt dabei erhalten. */
    val pausiert: Boolean = false,
    /** Die Stimme kommt wegen Netzproblemen nicht oder nur langsam; der Satz wird wiederholt angefragt. */
    val internetProbleme: Boolean = false,
) {
    val laeuft: Boolean get() = phase != MentalPhase.AUS
}

/**
 * Baut den geordneten Durchlaufplan von oben nach unten:
 * Zwei Sätze A (2) und B (3) ergeben [A 1/2, A 2/2, B 1/3, B 2/3, B 3/3].
 * Leere Sätze werden übersprungen; Wiederholungen liegen stets in 1..10.
 */
fun mentalitaetPlan(saetze: List<MentalSatz>): List<MentalSchritt> = buildList {
    for (satz in saetze) {
        val text = satz.text.trim()
        if (text.isEmpty()) continue
        val anzahl = satz.wiederholungen.coerceIn(MIN_WIEDERHOLUNGEN, MAX_WIEDERHOLUNGEN)
        for (w in 1..anzahl) {
            add(MentalSchritt(satz.id, text, w, anzahl))
        }
    }
}

/** Eine im Hintergrund frisch erzeugte (oder noch laufende) Ausgabe für genau einen Schritt. */
private class Vorab(val schritt: MentalSchritt) {
    /** Die erzeugte Datei, bis sie abgespielt oder beim Verwerfen gelöscht wird (auch wenn der Abbruch sie überholt). */
    val datei = AtomicReference<File?>(null)
    lateinit var ergebnis: Deferred<Result<File>>
}

/** Gleiche Ausgabe? Die Gesamtzahl der Wiederholungen ändert den gesprochenen Text nicht. */
private fun gleicheAusgabe(a: MentalSchritt, b: MentalSchritt): Boolean =
    a.satzId == b.satzId && a.wiederholung == b.wiederholung && a.text == b.text

/**
 * Bestimmt den nächsten Schritt im AKTUELLEN Plan nach dem zuletzt ausgegebenen Schritt [letzter]:
 * - Hat derselbe Satz (nach aktueller Wiederholungszahl) noch eine Wiederholung, folgt diese.
 * - Sonst folgt der Satz, der in der aktuellen Reihenfolge direkt danach steht; nach dem letzten Satz beginnt
 *   ein neuer Durchlauf.
 * - Wurde der Satz gelöscht, geht es beim nächsten noch vorhandenen Satz aus der vorherigen Reihenfolge weiter.
 * Liefert den Index im aktuellen Plan und ob dabei ein neuer Durchlauf beginnt.
 */
internal fun mentalitaetNaechster(
    plan: List<MentalSchritt>,
    letzterPlan: List<MentalSchritt>,
    letzter: MentalSchritt?,
): Pair<Int, Boolean> {
    if (letzter == null || plan.isEmpty()) return 0 to false
    val weiter = plan.indexOfFirst { it.satzId == letzter.satzId && it.wiederholung == letzter.wiederholung + 1 }
    if (weiter >= 0) return weiter to false
    val letzteStelle = plan.indexOfLast { it.satzId == letzter.satzId }
    if (letzteStelle >= 0) {
        val n = letzteStelle + 1
        return if (n >= plan.size) 0 to true else n to false
    }
    val vorhanden = plan.mapTo(HashSet()) { it.satzId }
    val alteStelle = letzterPlan.indexOfLast { it.satzId == letzter.satzId }
    for (s in letzterPlan.drop(alteStelle + 1)) {
        if (s.satzId in vorhanden) return plan.indexOfFirst { it.satzId == s.satzId } to false
    }
    return 0 to true
}

/**
 * Kern der Endlosschleife nach dem Vorbild von CortexAndroid (`startStreamingSpeech`), mit Live-Übernahme:
 * - Während ein Satz vorgelesen wird, wird die nächste Ausgabe bereits frisch im Hintergrund erzeugt.
 * - Welcher Schritt der nächste ist, wird vor jedem Schritt, während „Lädt“ und in jedem Pausentakt aus dem AKTUELLEN
 *   Plan ([planAbrufen]) neu bestimmt: neue Reihenfolge, geänderte Wiederholungszahl, geänderter oder gelöschter Satz
 *   greifen ab dem nächsten möglichen Schritt. Eine nicht mehr passende Vorbereitung wird erst verworfen, wenn die
 *   Änderung [stabilMs] lang besteht (schnelles Hin- und Herschieben am Regler kostet keine TTS-Anfragen); vor dem
 *   Abspielen entscheidet immer der aktuelle Plan.
 * - Die Pause ([pauseMsAbrufen]) wird in Takten von [pausenTaktMs] gewartet und jedes Mal neu gelesen, sodass eine
 *   verkürzte oder verlängerte Pause auch eine laufende Pause sofort anpasst. Der laufende Satz wird nie abgebrochen.
 * - Jede Datei wird genau einmal gespielt und erst DANACH (oder bei Abbruch/Verwerfen) gelöscht.
 */
internal suspend fun mentalitaetSchleife(
    plan: List<MentalSchritt> = emptyList(),
    planAbrufen: () -> List<MentalSchritt> = { plan },
    pauseMs: Long = MentalitaetSpieler.PAUSE_MS,
    pauseMsAbrufen: () -> Long = { pauseMs },
    pausenTaktMs: Long = Long.MAX_VALUE,
    stabilMs: Long = 1_000L,
    maxFehlerInFolge: Int = 3,
    maxVersucheJeSchritt: Int = 3,
    langsamMs: Long = 8_000L,
    wiederholungMs: (Int) -> Long = { versuch -> if (versuch <= 1) 500L else minOf(8_000L, 1_000L shl (versuch - 2).coerceIn(0, 3)) },
    istNetzFehler: (Throwable) -> Boolean = { it is IOException },
    sofortUeberspringen: (Throwable) -> Boolean = { false },
    beiInternet: (Boolean) -> Unit = {},
    vorSchritt: suspend () -> Unit = {},
    vorAbspielen: suspend () -> Unit = {},
    warteWennAngehalten: suspend () -> Unit = {},
    erzeuge: suspend (String) -> File,
    spiele: suspend (File) -> Unit,
    loesche: (File) -> Unit,
    pause: suspend (Long) -> Unit = { delay(it) },
    beiStand: (MentalPhase, MentalSchritt, Int, Int, Int) -> Unit = { _, _, _, _, _ -> },
    beiFehler: (Throwable) -> Unit = {},
) {
    if (planAbrufen().isEmpty()) return
    val takt = pausenTaktMs.coerceAtLeast(1L)

    coroutineScope {
        fun starteVorab(schritt: MentalSchritt): Vorab = Vorab(schritt).also { v ->
            v.ergebnis = async(Dispatchers.IO) {
                try {
                    val datei = erzeuge(schritt.text)
                    v.datei.set(datei)
                    Result.success(datei)
                } catch (abbruch: CancellationException) {
                    throw abbruch
                } catch (fehler: Exception) {
                    Result.failure(fehler)
                }
            }
        }

        fun verwerfe(v: Vorab) {
            v.ergebnis.cancel()
            v.ergebnis.invokeOnCompletion {
                v.datei.getAndSet(null)?.let { runCatching { loesche(it) } }
            }
        }

        var letzter: MentalSchritt? = null
        var letzterPlan: List<MentalSchritt> = emptyList()
        var vorab: Vorab? = null

        // Entprellung: ein abweichendes Ziel muss [stabilMs] lang gleich bleiben, bevor neu erzeugt wird.
        var kandidat: MentalSchritt? = null
        var kandidatMs = 0L
        fun stabilGeaendert(soll: MentalSchritt, vorbereitet: MentalSchritt, vergangenMs: Long): Boolean {
            if (gleicheAusgabe(soll, vorbereitet)) {
                kandidat = null
                kandidatMs = 0L
                return false
            }
            val k = kandidat
            if (k != null && gleicheAusgabe(k, soll)) kandidatMs += vergangenMs else {
                kandidat = soll
                kandidatMs = 0L
            }
            return kandidatMs >= stabilMs
        }

        /** Hält die Vorbereitung passend zum aktuellen Plan (nach Drag-and-drop, Zahl- oder Textänderung). */
        fun vorabAbgleichen(vergangenMs: Long) {
            val aktuell = planAbrufen()
            if (aktuell.isEmpty()) return
            val soll = aktuell[mentalitaetNaechster(aktuell, letzterPlan, letzter).first]
            val v = vorab
            if (v != null && !stabilGeaendert(soll, v.schritt, vergangenMs)) return
            v?.let(::verwerfe)
            vorab = starteVorab(soll)
            kandidat = null
            kandidatMs = 0L
        }

        try {
            var fehlerInFolge = 0
            var versuche = 0
            var schrittFehler = 0
            var netzInFolge = 0
            var versuchSchritt: MentalSchritt? = null
            var durchlauf = 1
            while (isActive) {
                vorSchritt()
                var aktPlan = planAbrufen()
                if (aktPlan.isEmpty()) break
                var (index, neuerDurchlauf) = mentalitaetNaechster(aktPlan, letzterPlan, letzter)
                var schritt = aktPlan[index]

                var v = vorab
                vorab = null
                if (v != null && !gleicheAusgabe(v.schritt, schritt)) {
                    verwerfe(v)
                    v = null
                }
                if (v == null) v = starteVorab(schritt)
                // Bis zum Abholen als Vorbereitung vermerkt, damit ein Stopp beim Laden die Datei sicher aufräumt.
                vorab = v
                kandidat = null
                kandidatMs = 0L
                // „Lädt“ nur, solange die Ausgabe noch nicht fertig bereitliegt; Änderungen greifen auch hier noch.
                var ladenGezeigt = false
                var geladenMs = 0L
                while (!v!!.ergebnis.isCompleted) {
                    // Angehalten zählt nicht als langsames Netz.
                    warteWennAngehalten()
                    if (!ladenGezeigt) {
                        ladenGezeigt = true
                        val dl = if (neuerDurchlauf) durchlauf + 1 else durchlauf
                        beiStand(MentalPhase.LAEDT, schritt, dl, index, aktPlan.size)
                    }
                    withTimeoutOrNull(takt) { v!!.ergebnis.join() }
                    if (v!!.ergebnis.isCompleted) break
                    geladenMs += takt
                    // Dauert die Vorbereitung ungewöhnlich lange, ist meist das Netz schuld: sichtbar machen, weiter warten.
                    if (geladenMs >= langsamMs) beiInternet(true)
                    val neuPlan = planAbrufen()
                    if (neuPlan.isEmpty()) continue
                    val (i2, n2) = mentalitaetNaechster(neuPlan, letzterPlan, letzter)
                    if (stabilGeaendert(neuPlan[i2], v!!.schritt, takt)) {
                        verwerfe(v!!)
                        aktPlan = neuPlan
                        index = i2
                        neuerDurchlauf = n2
                        schritt = neuPlan[i2]
                        v = starteVorab(schritt)
                        vorab = v
                        kandidat = null
                        kandidatMs = 0L
                        ladenGezeigt = false
                        geladenMs = 0L
                    }
                }
                val ergebnis = v!!.ergebnis.await()
                vorab = null

                val datei = ergebnis.getOrNull()
                if (versuchSchritt == null || !gleicheAusgabe(versuchSchritt!!, schritt)) {
                    versuchSchritt = schritt
                    versuche = 0
                    schrittFehler = 0
                    netzInFolge = 0
                }
                if (datei == null) {
                    // Nicht erzeugt: denselben Schritt erneut anfragen statt ihn zu überspringen. Die Pause verlängert sich
                    // dadurch von selbst (die eingestellte Grundpause bleibt unverändert). Netzfehler werden ohne Grenze
                    // wiederholt; andere Fehler höchstens [maxVersucheJeSchritt]-mal, dann wird nur dieser Schritt übersprungen.
                    val grund = ergebnis.exceptionOrNull()
                    if (grund is CancellationException) throw grund
                    if (grund is SyntheseAbbruch) throw grund
                    if (grund != null) beiFehler(grund)
                    val netz = grund != null && istNetzFehler(grund)
                    if (netz) {
                        netzInFolge++
                        // Ein einzelner Aussetzer wird still wiederholt; erst ab dem zweiten in Folge gibt es den Hinweis.
                        if (netzInFolge >= 2) beiInternet(true)
                    } else {
                        netzInFolge = 0
                        schrittFehler++
                    }
                    if (!netz && (schrittFehler >= maxVersucheJeSchritt || (grund != null && sofortUeberspringen(grund)))) {
                        fehlerInFolge++
                        if (fehlerInFolge >= maxFehlerInFolge) {
                            throw IllegalStateException(
                                grund?.message ?: "Die Stimme konnte nicht erzeugt werden. Prüf die Internetverbindung.",
                            )
                        }
                        // Nur diesen Schritt auslassen, die Session läuft mit dem nächsten weiter.
                        if (neuerDurchlauf) durchlauf++
                        letzter = schritt
                        letzterPlan = aktPlan
                        versuchSchritt = null
                        continue
                    }
                    versuche++
                    beiStand(MentalPhase.LAEDT, schritt, if (neuerDurchlauf) durchlauf + 1 else durchlauf, index, aktPlan.size)
                    pause(wiederholungMs(versuche))
                    continue
                }
                versuchSchritt = null
                // Vor dem Abspielen warten (Pause-Knopf, Tonfokus); danach entscheidet erneut der aktuelle Plan,
                // damit ein während der Pause geänderter oder gelöschter Satz nicht mehr gesprochen wird.
                try {
                    vorAbspielen()
                } catch (abbruch: Throwable) {
                    runCatching { loesche(datei) }
                    throw abbruch
                }
                val nachWarten = planAbrufen()
                if (nachWarten.isNotEmpty() &&
                    !gleicheAusgabe(nachWarten[mentalitaetNaechster(nachWarten, letzterPlan, letzter).first], schritt)
                ) {
                    runCatching { loesche(datei) }
                    continue
                }
                fehlerInFolge = 0
                if (neuerDurchlauf) durchlauf++
                letzter = schritt
                letzterPlan = aktPlan
                try {
                    // Nächste Ausgabe schon während dieses Satzes frisch vorbereiten.
                    vorabAbgleichen(0L)

                    currentCoroutineContext().ensureActive()
                    beiInternet(false)
                    beiStand(MentalPhase.SPRICHT, schritt, durchlauf, index, aktPlan.size)
                    spiele(datei)
                } finally {
                    // Einmal gespielt → sofort löschen, nie erneut abspielen.
                    runCatching { loesche(datei) }
                }

                // Eingestellte Pause zwischen je zwei Ausgaben, live nachgeführt: jede Änderung der Dauer gilt sofort,
                // auch für die gerade laufende Pause (bereits gewartete Zeit zählt mit).
                vorabAbgleichen(0L)
                var gewartet = 0L
                var gezeigtMs = -1L
                while (true) {
                    val ziel = pauseMsAbrufen().coerceAtLeast(0L)
                    if (ziel != gezeigtMs) {
                        gezeigtMs = ziel
                        beiStand(MentalPhase.PAUSE, schritt, durchlauf, index, aktPlan.size)
                    }
                    val rest = ziel - gewartet
                    if (rest <= 0L) break
                    val stueck = minOf(rest, takt)
                    pause(stueck)
                    gewartet += stueck
                    vorabAbgleichen(stueck)
                }
            }
        } finally {
            vorab?.let(::verwerfe)
            vorab = null
        }
    }
}

/**
 * Vorleseschleife für „Mentalität“:
 * - Nutzt das in Jarvis standardmäßig eingestellte Vorlesemodell samt Stimme ([Synthese] ohne Sonderwahl).
 * - Liest die Sätze von oben nach unten; jeder Satz wird gemäß seiner Zahl direkt hintereinander wiederholt.
 * - Bereitet die jeweils nächste Ausgabe schon während des laufenden Satzes frisch im Hintergrund vor,
 *   hält die eingestellte Pause (Standard [PAUSE_MS], 5 Sekunden) zwischen zwei Ausgaben und spielt jede Aufnahme genau einmal.
 * - Läuft in Endlosschleife, bis [stopp] aufgerufen wird (sofortiger Abbruch in Ansage, Laden oder Pause).
 */
object MentalitaetSpieler {
    const val PAUSE_MS = 5_000L
    private const val PAUSE_STUECK_MS = 50L
    private const val PAUSEN_TAKT_MS = 200L
    private const val FOKUS_PRUEF_MS = 150L
    private const val WACH_MS = 5 * 60_000L

    /** Schützt einen MediaPlayer vor doppeltem Freigeben bei gleichzeitigem Stopp und Abschluss. */
    private class AktiverSpieler(val mp: MediaPlayer) {
        private val geschlossen = AtomicBoolean(false)
        /** Fertig abgespielt: Ein Weiter darf ihn dann nicht noch einmal starten. */
        @Volatile var fertig = false
        fun schliessen() {
            if (!geschlossen.compareAndSet(false, true)) return
            SpeechLoudness.release(mp)
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.release() }
        }
    }

    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val haupt = Handler(Looper.getMainLooper())
    private val laufNummer = AtomicLong(0)

    private val _stand = MutableStateFlow(MentalSpielStand())
    val stand: StateFlow<MentalSpielStand> = _stand.asStateFlow()

    val aktiv: Boolean get() = _stand.value.laeuft

    private var job: Job? = null
    private var appContext: Context? = null
    @Volatile private var syntheseInstanz: Synthese? = null
    private var aktiverSpieler: AktiverSpieler? = null

    private var fokusAnfrage: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null
    private var wach: PowerManager.WakeLock? = null
    private var pausiertDurchFokus = false

    /** Vom Pause-/Weiter-Knopf angehalten (unabhängig von einer Pause durch den Tonfokus). */
    private val manuellPausiert = MutableStateFlow(false)
    @Volatile private var internetProbleme = false
    private var letzteNummer = 1
    private var letzteGesamt = 1

    /** Darf der Ton gerade laufen? Gemeinsames Tor für Pause-Knopf und Tonfokus. */
    private fun darfSpielen() = !pausiertDurchFokus && !manuellPausiert.value

    private fun synthese(app: Context): Synthese =
        syntheseInstanz ?: synchronized(this) {
            syntheseInstanz ?: Synthese(app, SecureSettings.get(app)).also { syntheseInstanz = it }
        }

    fun umschalten(context: Context, saetze: List<MentalSatz>) {
        if (aktiv) stopp() else starten(context, saetze)
    }

    /**
     * Startet die Vorleseschleife mit der aktuellen Listenreihenfolge.
     * Bei leerer Liste startet keine Wiedergabe.
     */
    fun starten(context: Context, saetze: List<MentalSatz>) {
        // Die Schleife liest live aus dem Speicher; der Start zeigt deshalb denselben Stand (Rückfall: übergebene Liste).
        val plan = mentalitaetPlan(MentalitaetSpeicher.stand.value).ifEmpty { mentalitaetPlan(saetze) }
        if (plan.isEmpty()) return

        val app = context.applicationContext
        appContext = app
        stoppIntern(fehler = null, dienstBeenden = false)
        manuellPausiert.value = false
        internetProbleme = false
        letzteNummer = 1
        letzteGesamt = plan.size

        // Andere laufende Sprachausgaben in Jarvis beenden, damit sich nichts überlagert.
        runCatching { Vorleser.aktuell()?.stopp(dienstBeenden = false) }
        if (AtemSitzung.offen) runCatching { AtemSitzung.beenden() }

        val einstellungen = Einstellungen.get(app)
        var aktuellePauseSekunden = einstellungen.mentalitaetPauseSekunden
        val meinLauf = laufNummer.incrementAndGet()
        val erster = plan.first()
        _stand.value = MentalSpielStand(
            phase = MentalPhase.LAEDT,
            aktiverSatzId = erster.satzId,
            wiederholung = erster.wiederholung,
            wiederholungenGesamt = erster.wiederholungenGesamt,
            durchlauf = 1,
            pauseSekunden = aktuellePauseSekunden,
        )

        if (!fokusHolen(app)) {
            _stand.value = MentalSpielStand(fehler = "Tonfokus wurde verweigert (läuft gerade ein Anruf?).")
            dienstBeendenWennFrei(app)
            return
        }
        VorleseDienst.starten(app)
        VorleseDienst.aktualisiere(app, "Mentalität", 1, plan.size, pausiert = false)
        wachErneuern(app)
        val syn = synthese(app)
        Logbuch.sprache("Mentalität gestartet", "schritte=${plan.size}, pause=${aktuellePauseSekunden}s")

        var planQuelle: List<MentalSatz>? = null
        var planCache: List<MentalSchritt> = plan
        job = bereich.launch {
            try {
                mentalitaetSchleife(
                    // Live: jede Änderung an Liste, Reihenfolge oder Wiederholungszahl greift ab dem nächsten Schritt.
                    planAbrufen = {
                        // Nur neu bauen, wenn sich die Liste wirklich geändert hat (Pausentakt alle 200 ms).
                        val quelle = MentalitaetSpeicher.stand.value
                        if (quelle !== planQuelle) {
                            planQuelle = quelle
                            planCache = mentalitaetPlan(quelle)
                        }
                        planCache
                    },
                    pauseMsAbrufen = {
                        val sek = einstellungen.mentalitaetPauseSekunden
                        aktuellePauseSekunden = sek
                        sek * 1_000L
                    },
                    pausenTaktMs = PAUSEN_TAKT_MS,
                    vorSchritt = { wachErneuern(app) },
                    vorAbspielen = {
                        warteWennPausiert()
                        warteBeiFokusPause(meinLauf)
                    },
                    warteWennAngehalten = { warteWennPausiert() },
                    // Nur echte Verbindungsfehler gelten als Internetprobleme; ein Speicher- oder Dateifehler nicht.
                    istNetzFehler = ::istNetzFehler,
                    // Eine Stimme, die ihre Frist überschritten hat, wird nicht erneut abgewartet: nur diesen Satz auslassen.
                    sofortUeberspringen = { it is SyntheseZeitlimit },
                    erzeuge = { text -> erzeugeFrischFuerLauf(syn, bereiteTextAuf(text), meinLauf) },
                    spiele = { datei -> spieleDatei(app, datei, meinLauf) },
                    loesche = { datei -> datei.delete() },
                    // Satzpause in kleinen Stücken; angehalten zählt keine Zeit, sodass sie genau dort weiterläuft.
                    pause = { ms ->
                        var rest = ms
                        while (rest > 0L) {
                            warteWennPausiert()
                            val stueck = minOf(rest, PAUSE_STUECK_MS)
                            delay(stueck)
                            rest -= stueck
                        }
                    },
                    beiInternet = { problem ->
                        if (laufNummer.get() == meinLauf && internetProbleme != problem) {
                            internetProbleme = problem
                            if (problem) Logbuch.netz("Mentalität: Internetprobleme", "Stimme kommt nicht oder langsam, Satz wird erneut angefragt")
                            val s = _stand.value
                            if (s.laeuft) _stand.value = s.copy(internetProbleme = problem)
                        }
                    },
                    beiStand = { phase, schritt, durchlauf, schrittIndex, planGroesse ->
                        if (laufNummer.get() == meinLauf) {
                            _stand.value = MentalSpielStand(
                                phase = phase,
                                aktiverSatzId = schritt.satzId,
                                wiederholung = schritt.wiederholung,
                                wiederholungenGesamt = schritt.wiederholungenGesamt,
                                durchlauf = durchlauf,
                                pauseSekunden = aktuellePauseSekunden,
                                pausiert = manuellPausiert.value,
                                internetProbleme = internetProbleme,
                            )
                            if (phase == MentalPhase.SPRICHT) {
                                letzteNummer = schrittIndex + 1
                                letzteGesamt = planGroesse.coerceAtLeast(schrittIndex + 1)
                                VorleseDienst.aktualisiere(
                                    app, "Mentalität", letzteNummer, letzteGesamt, pausiert = manuellPausiert.value,
                                )
                            }
                        }
                    },
                    beiFehler = { fehler -> Logbuch.fehler("Mentalität-Ausgabe wird erneut angefragt", fehler) },
                )
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (abbruch: SyntheseAbbruch) {
                Logbuch.fehler("Mentalität angehalten", abbruch)
                if (laufNummer.get() == meinLauf) {
                    _stand.value = MentalSpielStand(fehler = abbruch.message)
                }
            } catch (fehler: Exception) {
                Logbuch.fehler("Mentalität fehlgeschlagen", fehler)
                if (laufNummer.get() == meinLauf) {
                    _stand.value = MentalSpielStand(
                        fehler = fehler.message ?: "Das Vorlesen wurde unerwartet beendet.",
                    )
                }
            } finally {
                if (laufNummer.get() == meinLauf) {
                    spielerBeenden()
                    fokusAbgeben()
                    wachLos()
                    if (_stand.value.laeuft) _stand.value = MentalSpielStand()
                    dienstBeendenWennFrei(app)
                }
            }
        }
    }

    /** Stoppt jede laufende Ansage, Erzeugung oder Pause sofort. */
    @JvmOverloads
    fun stopp(dienstBeenden: Boolean = true) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            stoppIntern(fehler = null, dienstBeenden = dienstBeenden)
        } else {
            haupt.post { stoppIntern(fehler = null, dienstBeenden = dienstBeenden) }
        }
    }

    /**
     * Hält die laufende Session genau an der aktuellen Stelle an: mitten im Satz (der Player pausiert),
     * in der Satzpause (die Restzeit bleibt) oder beim Laden (der fertige Satz startet erst nach Weiter).
     */
    fun pausieren() {
        if (Looper.myLooper() != Looper.getMainLooper()) { haupt.post { pausieren() }; return }
        if (!aktiv || manuellPausiert.value) return
        manuellPausiert.value = true
        aktiverSpieler?.mp?.let { runCatching { if (it.isPlaying) it.pause() } }
        // Angehalten braucht die CPU nicht wach zu bleiben; Weiter holt die Sperre wieder.
        wachLos()
        _stand.value = _stand.value.copy(pausiert = true)
        appContext?.let { VorleseDienst.aktualisiere(it, "Mentalität", letzteNummer, letzteGesamt, pausiert = true) }
        Logbuch.sprache("Mentalität pausiert", "phase=${_stand.value.phase}")
    }

    /** Setzt dieselbe Session an der pausierten Stelle fort. */
    fun fortsetzen() {
        if (Looper.myLooper() != Looper.getMainLooper()) { haupt.post { fortsetzen() }; return }
        if (!aktiv || !manuellPausiert.value) return
        val app = appContext
        // Während der Pause gestartete Ausgaben anderer Funktionen beenden, damit sich nichts überlagert.
        runCatching { Vorleser.aktuell()?.takeIf { it.stand.value.zustand != VorleseZustand.AUS }?.stopp(dienstBeenden = false) }
        if (AtemSitzung.offen) runCatching { AtemSitzung.beenden() }
        // Ging der Tonfokus in der Pause an eine andere Ausgabe, wird er jetzt neu geholt.
        if (fokusAnfrage == null && app != null && !fokusHolen(app)) {
            _stand.value = _stand.value.copy(fehler = "Tonfokus wurde verweigert (läuft gerade ein Anruf?).")
            return
        }
        manuellPausiert.value = false
        app?.let { wachErneuern(it) }
        if (!pausiertDurchFokus) {
            aktiverSpieler?.takeIf { !it.fertig }?.mp?.let { runCatching { if (!it.isPlaying) it.start() } }
        }
        _stand.value = _stand.value.copy(pausiert = false)
        app?.let { VorleseDienst.aktualisiere(it, "Mentalität", letzteNummer, letzteGesamt, pausiert = false) }
        Logbuch.sprache("Mentalität fortgesetzt", "phase=${_stand.value.phase}")
    }

    /**
     * Eine andere Jarvis-Funktion (Diktat, Vorlesen, Atemübung, Gespräch) braucht Ton oder Mikrofon:
     * Die Mentalität wird nur angehalten, nie beendet. Dieselbe Session bleibt mit ihrer Stelle erhalten
     * und läuft erst nach Weiter dort fort.
     */
    fun zurueckstellen() {
        if (Looper.myLooper() != Looper.getMainLooper()) { haupt.post { zurueckstellen() }; return }
        if (aktiv) pausieren()
    }

    /** Zeigt die Meldung der pausierten Session wieder, nachdem ein anderes Vorlesen den gemeinsamen Dienst räumen wollte. */
    fun meldungZeigen() {
        val app = appContext ?: return
        if (aktiv) VorleseDienst.aktualisiere(app, "Mentalität", letzteNummer, letzteGesamt, pausiert = manuellPausiert.value)
    }

    fun pauseUmschalten() {
        if (manuellPausiert.value) fortsetzen() else pausieren()
    }

    private fun istNetzFehler(fehler: Throwable): Boolean =
        fehler is java.net.SocketException || fehler is java.net.UnknownHostException ||
            fehler is java.io.InterruptedIOException || fehler is javax.net.ssl.SSLException ||
            fehler is java.io.EOFException || fehler.javaClass.name.startsWith("okhttp3.")

    private suspend fun warteWennPausiert() {
        if (manuellPausiert.value) manuellPausiert.first { !it }
    }

    fun fehlerGelesen() {
        if (_stand.value.fehler != null) {
            _stand.value = _stand.value.copy(fehler = null)
        }
    }

    private fun stoppIntern(fehler: String?, dienstBeenden: Boolean) {
        val warAktiv = aktiv || job != null || aktiverSpieler != null || fokusAnfrage != null
        laufNummer.incrementAndGet()
        job?.cancel()
        job = null
        pausiertDurchFokus = false
        manuellPausiert.value = false
        internetProbleme = false
        spielerBeenden()
        fokusAbgeben()
        wachLos()
        _stand.value = MentalSpielStand(fehler = fehler)
        if (dienstBeenden && warAktiv) {
            appContext?.let { dienstBeendenWennFrei(it) }
        }
    }

    private fun dienstBeendenWennFrei(app: Context) {
        val vorleserAktiv = Vorleser.aktuell()?.stand?.value?.zustand.let { it != null && it != VorleseZustand.AUS }
        if (!vorleserAktiv && !aktiv) VorleseDienst.beenden(app)
    }

    private fun bereiteTextAuf(roh: String): String =
        SprechText.fuerStimme(roh).ifBlank { roh.trim() }

    /** Erzeugt genau eine frische Audiodatei; verwirft sie sofort, falls der Lauf inzwischen gestoppt wurde. */
    private suspend fun erzeugeFrischFuerLauf(syn: Synthese, text: String, meinLauf: Long): File {
        // Netzfehler und abgeschnittene Antworten wiederholt die Schleife selbst (gleicher Schritt, mit Anzeige).
        val datei = syn.synthetisiere(text, wahl = null, frisch = true)
        if (laufNummer.get() != meinLauf || !currentCoroutineContext().isActive) {
            runCatching { datei.delete() }
            throw CancellationException("Mentalität-Lauf beendet")
        }
        return datei
    }

    private suspend fun warteBeiFokusPause(meinLauf: Long) {
        while (pausiertDurchFokus && laufNummer.get() == meinLauf) {
            delay(FOKUS_PRUEF_MS)
        }
    }

    /**
     * Bereitet den Player abseits des Hauptfadens vor und startet ihn anschließend atomar auf dem Hauptfaden,
     * sodass Fokus-Wechsel und Stopp nie mit `prepare()` kollidieren.
     */
    private suspend fun spieleDatei(app: Context, datei: File, meinLauf: Long) {
        // Bricht die Wiedergabe mit einem Fehler ab, wird dieselbe Datei noch einmal ganz abgespielt (sie wird erst danach gelöscht).
        for (versuch in 1..2) {
            if (spieleDateiEinmal(app, datei, meinLauf) || laufNummer.get() != meinLauf) return
            Logbuch.sprache("Mentalität-Wiedergabe wiederholt", "versuch=$versuch")
        }
    }

    /** true = vollständig abgespielt oder bewusst beendet; false = Wiedergabe vorzeitig gescheitert. */
    private suspend fun spieleDateiEinmal(app: Context, datei: File, meinLauf: Long): Boolean {
        val lokal = AktiverSpieler(MediaPlayer())
        try {
            val vorbereitet = withContext(Dispatchers.IO) {
                try {
                    lokal.mp.setAudioAttributes(SpeechLoudness.attributes)
                    lokal.mp.setWakeMode(app, PowerManager.PARTIAL_WAKE_LOCK)
                    lokal.mp.setDataSource(datei.absolutePath)
                    lokal.mp.prepare()
                    true
                } catch (fehler: Exception) {
                    Logbuch.fehler("Mentalität-Wiedergabefehler", fehler)
                    false
                }
            }
            if (!vorbereitet) return false
            if (laufNummer.get() != meinLauf) return true
            warteWennPausiert()
            warteBeiFokusPause(meinLauf)
            if (laufNummer.get() != meinLauf) return true

            val dauerMs = runCatching { lokal.mp.duration }.getOrDefault(-1)
            aktiverSpieler = lokal
            return suspendCancellableCoroutine { fortsetzung ->
                lokal.mp.setOnCompletionListener {
                    lokal.fertig = true
                    val position = runCatching { it.currentPosition }.getOrDefault(-1)
                    // Manche Geräte melden nach dem Ende Position 0: das gilt nicht als vorzeitiges Ende.
                    val frueh = dauerMs > 1_000 && position in 1 until dauerMs - 500
                    if (frueh) Logbuch.sprache("Mentalität-Wiedergabe endete früh", "position=${position}ms dauer=${dauerMs}ms")
                    if (fortsetzung.isActive) fortsetzung.resume(!frueh)
                }
                lokal.mp.setOnErrorListener { _, was, extra ->
                    lokal.fertig = true
                    Logbuch.fehler("Mentalität-Wiedergabefehler", IllegalStateException("MediaPlayer what=$was extra=$extra"))
                    if (fortsetzung.isActive) fortsetzung.resume(false)
                    true
                }
                SpeechLoudness.boost(lokal.mp)
                if (laufNummer.get() != meinLauf) {
                    if (fortsetzung.isActive) fortsetzung.resume(true)
                } else if (darfSpielen()) {
                    runCatching { lokal.mp.start() }.onFailure { fehler ->
                        Logbuch.fehler("Mentalität-Wiedergabefehler", fehler)
                        if (fortsetzung.isActive) fortsetzung.resume(false)
                    }
                }
                fortsetzung.invokeOnCancellation { lokal.schliessen() }
            }
        } finally {
            lokal.schliessen()
            if (aktiverSpieler === lokal) aktiverSpieler = null
        }
    }

    private fun spielerBeenden() {
        val s = aktiverSpieler ?: return
        aktiverSpieler = null
        s.schliessen()
    }

    private fun fokusHolen(app: Context): Boolean {
        val am = app.getSystemService(AudioManager::class.java) ?: return true
        fokusAbgeben()
        audioManager = am
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val anfrage = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(SpeechLoudness.attributes)
            .setOnAudioFocusChangeListener({ aenderung ->
                when (aenderung) {
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> Unit
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        pausiertDurchFokus = true
                        aktiverSpieler?.mp?.let { runCatching { if (it.isPlaying) it.pause() } }
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> {
                        if (pausiertDurchFokus) {
                            pausiertDurchFokus = false
                            // Vom Pause-Knopf Angehaltenes bleibt angehalten.
                            if (darfSpielen()) {
                                aktiverSpieler?.takeIf { !it.fertig }?.mp?.let { runCatching { if (!it.isPlaying) it.start() } }
                            }
                        }
                    }
                    // Dauerhaft verloren (andere Ausgabe übernimmt): anhalten statt beenden; Weiter holt den Fokus neu.
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        fokusAbgeben()
                        pausiertDurchFokus = false
                        pausieren()
                    }
                }
            }, haupt)
            .build()
        fokusAnfrage = anfrage
        return when (runCatching { am.requestAudioFocus(anfrage) }.getOrDefault(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)) {
            AudioManager.AUDIOFOCUS_REQUEST_FAILED -> {
                fokusAnfrage = null
                false
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                pausiertDurchFokus = true
                true
            }
            else -> true
        }
    }

    private fun fokusAbgeben() {
        val anfrage = fokusAnfrage ?: return
        fokusAnfrage = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { audioManager?.abandonAudioFocusRequest(anfrage) }
        }
    }

    private fun wachErneuern(app: Context) {
        val w = wach ?: runCatching {
            app.getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "jarvis:mentalitaet")
                .apply { setReferenceCounted(false) }
        }.getOrNull().also { wach = it }
        runCatching { w?.acquire(WACH_MS) }
    }

    private fun wachLos() {
        val w = wach ?: return
        wach = null
        runCatching { if (w.isHeld) w.release() }
    }
}
