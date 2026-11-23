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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
    val fehler: String? = null,
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

/**
 * Kern der Endlosschleife, unabhängig von Android-Klassen für Unit-Tests:
 * - Erzeugt jede einzelne Ausgabe frisch über [erzeuge].
 * - Löscht jede erzeugte Datei direkt nach dem Abspielen (oder bei Abbruch) über [loesche].
 * - Hält zwischen je zwei gesprochenen Ausgaben [pauseMs] Pause; während der Pause wird die nächste
 *   Ausgabe bereits frisch erzeugt und bei Abbruch während der Pause verworfen und gelöscht.
 */
internal suspend fun mentalitaetSchleife(
    plan: List<MentalSchritt>,
    pauseMs: Long = MentalitaetSpieler.PAUSE_MS,
    maxFehlerInFolge: Int = 3,
    vorSchritt: suspend () -> Unit = {},
    vorAbspielen: suspend () -> Unit = {},
    erzeuge: suspend (String) -> File,
    spiele: suspend (File) -> Unit,
    loesche: (File) -> Unit,
    pause: suspend (Long) -> Unit = { delay(it) },
    beiStand: (MentalPhase, MentalSchritt, Int, Int) -> Unit = { _, _, _, _ -> },
    beiFehler: (Throwable) -> Unit = {},
) {
    if (plan.isEmpty()) return
    var index = 0
    var durchlauf = 1
    var fehlerInFolge = 0
    var bereit: Result<File>? = null
    try {
        while (currentCoroutineContext().isActive) {
            vorSchritt()
            val schritt = plan[index]

            val dateiErgebnis = bereit ?: run {
                beiStand(MentalPhase.LAEDT, schritt, durchlauf, index)
                try {
                    Result.success(erzeuge(schritt.text))
                } catch (abbruch: CancellationException) {
                    throw abbruch
                } catch (fehler: Exception) {
                    Result.failure(fehler)
                }
            }
            bereit = null

            val datei = dateiErgebnis.getOrNull()
            if (datei == null) {
                val grund = dateiErgebnis.exceptionOrNull()
                if (grund is CancellationException) throw grund
                if (grund is SyntheseAbbruch) throw grund
                fehlerInFolge++
                if (grund != null) beiFehler(grund)
                if (fehlerInFolge >= maxFehlerInFolge) {
                    throw IllegalStateException(
                        grund?.message ?: "Die Stimme konnte nicht erzeugt werden. Prüf die Internetverbindung.",
                    )
                }
            } else {
                fehlerInFolge = 0
                try {
                    vorAbspielen()
                    currentCoroutineContext().ensureActive()
                    beiStand(MentalPhase.SPRICHT, schritt, durchlauf, index)
                    spiele(datei)
                } finally {
                    // Einmal gespielt → sofort löschen, nie erneut abspielen.
                    runCatching { loesche(datei) }
                }
            }

            // Nächsten Schritt bestimmen (von oben nach unten, nach dem letzten wieder oben).
            val naechsterIndex = (index + 1) % plan.size
            val naechsterDurchlauf = if (naechsterIndex == 0) durchlauf + 1 else durchlauf
            val naechsterSchritt = plan[naechsterIndex]

            // Exakt [pauseMs] (5 Sekunden) Pause zwischen je zwei Ausgaben: zwischen Wiederholungen,
            // zwischen verschiedenen Sätzen und beim Übergang zum nächsten Durchlauf.
            beiStand(MentalPhase.PAUSE, schritt, durchlauf, index)
            val erstellteDatei = AtomicReference<File?>(null)
            val uebernommen = AtomicBoolean(false)
            val vorab = coroutineScope {
                val erzeugung = async(Dispatchers.IO) {
                    try {
                        val datei = erzeuge(naechsterSchritt.text)
                        erstellteDatei.set(datei)
                        Result.success(datei)
                    } catch (abbruch: CancellationException) {
                        throw abbruch
                    } catch (fehler: Exception) {
                        Result.failure(fehler)
                    }
                }
                erzeugung.invokeOnCompletion {
                    if (!uebernommen.get()) {
                        erstellteDatei.getAndSet(null)?.let { runCatching { loesche(it) } }
                    }
                }
                try {
                    pause(pauseMs)
                    if (!erzeugung.isCompleted) {
                        beiStand(MentalPhase.LAEDT, naechsterSchritt, naechsterDurchlauf, naechsterIndex)
                    }
                    val ergebnis = erzeugung.await()
                    uebernommen.set(true)
                    erstellteDatei.set(null)
                    ergebnis
                } catch (abbruch: CancellationException) {
                    erzeugung.cancel()
                    if (!uebernommen.get()) {
                        erstellteDatei.getAndSet(null)?.let { runCatching { loesche(it) } }
                    }
                    throw abbruch
                }
            }
            bereit = vorab
            index = naechsterIndex
            durchlauf = naechsterDurchlauf
        }
    } finally {
        bereit?.getOrNull()?.let { runCatching { loesche(it) } }
    }
}

/**
 * Vorleseschleife für „Mentalität“:
 * - Nutzt das in Jarvis standardmäßig eingestellte Vorlesemodell samt Stimme ([Synthese] ohne Sonderwahl).
 * - Liest die Sätze von oben nach unten; jeder Satz wird gemäß seiner Zahl direkt hintereinander wiederholt.
 * - Hält zwischen je zwei gesprochenen Ausgaben exakt [PAUSE_MS] (5 Sekunden) Pause: zwischen Wiederholungen,
 *   zwischen verschiedenen Sätzen und beim Übergang zum nächsten Durchlauf.
 * - Lässt JEDE einzelne Ausgabe frisch aus dem gespeicherten Text erzeugen (`frisch = true`), spielt keine
 *   Aufnahme erneut ab und löscht jede erzeugte Audiodatei direkt nach der Wiedergabe.
 * - Läuft in Endlosschleife, bis [stopp] aufgerufen wird (sofortiger Abbruch in Ansage, Laden oder Pause).
 */
object MentalitaetSpieler {
    const val PAUSE_MS = 5_000L
    private const val NETZ_WIEDERHOLUNG_MS = 500L
    private const val FOKUS_PRUEF_MS = 150L
    private const val WACH_MS = 5 * 60_000L

    /** Schützt einen MediaPlayer vor doppeltem Freigeben bei gleichzeitigem Stopp und Abschluss. */
    private class AktiverSpieler(val mp: MediaPlayer) {
        private val geschlossen = AtomicBoolean(false)
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
        val plan = mentalitaetPlan(saetze)
        if (plan.isEmpty()) return

        val app = context.applicationContext
        appContext = app
        stoppIntern(fehler = null, dienstBeenden = false)

        // Andere laufende Sprachausgaben in Jarvis beenden, damit sich nichts überlagert.
        runCatching { Vorleser.aktuell()?.stopp(dienstBeenden = false) }
        if (AtemSitzung.offen) runCatching { AtemSitzung.beenden() }

        val meinLauf = laufNummer.incrementAndGet()
        val erster = plan.first()
        _stand.value = MentalSpielStand(
            phase = MentalPhase.LAEDT,
            aktiverSatzId = erster.satzId,
            wiederholung = erster.wiederholung,
            wiederholungenGesamt = erster.wiederholungenGesamt,
            durchlauf = 1,
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
        Logbuch.sprache("Mentalität gestartet", "schritte=${plan.size}")

        job = bereich.launch {
            try {
                mentalitaetSchleife(
                    plan = plan,
                    pauseMs = PAUSE_MS,
                    vorSchritt = { wachErneuern(app) },
                    vorAbspielen = { warteBeiFokusPause(meinLauf) },
                    erzeuge = { text -> erzeugeFrischFuerLauf(syn, bereiteTextAuf(text), meinLauf) },
                    spiele = { datei -> spieleDatei(app, datei, meinLauf) },
                    loesche = { datei -> datei.delete() },
                    beiStand = { phase, schritt, durchlauf, schrittIndex ->
                        if (laufNummer.get() == meinLauf) {
                            _stand.value = MentalSpielStand(
                                phase = phase,
                                aktiverSatzId = schritt.satzId,
                                wiederholung = schritt.wiederholung,
                                wiederholungenGesamt = schritt.wiederholungenGesamt,
                                durchlauf = durchlauf,
                            )
                            if (phase == MentalPhase.SPRICHT) {
                                VorleseDienst.aktualisiere(app, "Mentalität", schrittIndex + 1, plan.size, pausiert = false)
                            }
                        }
                    },
                    beiFehler = { fehler -> Logbuch.fehler("Mentalität-Ausgabe übersprungen", fehler) },
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
        val datei = try {
            syn.synthetisiere(text, wahl = null, frisch = true)
        } catch (zeitlimit: SyntheseZeitlimit) {
            throw zeitlimit
        } catch (netz: IOException) {
            delay(NETZ_WIEDERHOLUNG_MS)
            syn.synthetisiere(text, wahl = null, frisch = true)
        }
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
            if (!vorbereitet || laufNummer.get() != meinLauf) return
            warteBeiFokusPause(meinLauf)
            if (laufNummer.get() != meinLauf) return

            aktiverSpieler = lokal
            suspendCancellableCoroutine { fortsetzung ->
                lokal.mp.setOnCompletionListener { if (fortsetzung.isActive) fortsetzung.resume(Unit) }
                lokal.mp.setOnErrorListener { _, _, _ ->
                    if (fortsetzung.isActive) fortsetzung.resume(Unit)
                    true
                }
                SpeechLoudness.boost(lokal.mp)
                if (laufNummer.get() != meinLauf) {
                    if (fortsetzung.isActive) fortsetzung.resume(Unit)
                } else if (!pausiertDurchFokus) {
                    runCatching { lokal.mp.start() }.onFailure { fehler ->
                        Logbuch.fehler("Mentalität-Wiedergabefehler", fehler)
                        if (fortsetzung.isActive) fortsetzung.resume(Unit)
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
                            aktiverSpieler?.mp?.let { runCatching { it.start() } }
                        }
                    }
                    AudioManager.AUDIOFOCUS_LOSS -> stopp()
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
