package de.frank.genialeideen.speech

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import de.frank.genialeideen.data.settings.SecureSettings
import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.tts.TtsManager
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
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
    val fehler: String? = null,
)

/**
 * Die Absatz-Pipeline aus Baustein D 4.2.
 *
 * Ein Absatz ist eine Vorlese-Einheit. Während Absatz *n* läuft, sind *n+1* und *n+2* bereits
 * beim Sprachdienst in Arbeit, sodass der nächste Ton ohne Loch anschliesst. Zwischen zwei
 * Absätzen liegt rund eine Sekunde Atem.
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
    private var spieler: MediaPlayer? = null
    private var fokusAnfrage: AudioFocusRequest? = null

    /** Wurde wegen eines Anrufs pausiert — danach wird von allein fortgesetzt. */
    private var pausiertDurchFokus = false

    fun sprich(quelle: String, titel: String, rohText: String) {
        if (_stand.value.quelle == quelle && _stand.value.zustand != VorleseZustand.AUS) {
            stopp()
            return
        }
        stopp()
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
        VorleseDienst.starten(appContext)
        IdeenLog.info(
            "Vorleser",
            "sprich",
            "Vorlesen beginnt",
            mapOf("absaetze" to absaetze.size, "chars" to text.length),
        )
        laufenderJob = bereich.launch {
            try {
                if (synthese.kannVorausschauen()) pipelineMitVorausschau(absaetze) else reihum(absaetze)
                _stand.value = _stand.value.copy(zustand = VorleseZustand.AUS, quelle = null)
            } catch (abbruch: CancellationException) {
                throw abbruch
            } catch (fehler: SyntheseAbbruch) {
                IdeenLog.error("Vorleser", "sprich", "Pipeline angehalten", mapOf("grund" to fehler.javaClass.simpleName))
                _stand.value = VorleseStand(fehler = fehler.message)
            } catch (fehler: Exception) {
                IdeenLog.error("Vorleser", "sprich", "Vorlesen fehlgeschlagen", mapOf("art" to fehler.javaClass.simpleName))
                _stand.value = VorleseStand(
                    fehler = fehler.message ?: "Das Vorlesen ist unterwegs abgebrochen.",
                )
            } finally {
                gibFokusFrei()
                VorleseDienst.beenden(appContext)
                synthese.raeumeAuf()
            }
        }
    }

    /** Google und die eigene Stimme können vorab erzeugen — hier greift die Vorausschau. */
    private suspend fun pipelineMitVorausschau(absaetze: List<String>) {
        val inArbeit = HashMap<Int, Deferred<File>>()
        fun beauftrage(index: Int) {
            if (index in absaetze.indices && index !in inArbeit) {
                inArbeit[index] = bereich.async(Dispatchers.IO) { synthetisiereMitTeilung(absaetze[index]) }
            }
        }
        // Absatz 0 sowie die beiden folgenden gehen sofort in Arbeit.
        (0..VORAUSSCHAU).forEach(::beauftrage)

        absaetze.indices.forEach { index ->
            val datei = inArbeit.remove(index)?.await()
                ?: withContext(Dispatchers.IO) { synthetisiereMitTeilung(absaetze[index]) }
            beauftrage(index + VORAUSSCHAU + 1)
            _stand.value = _stand.value.copy(
                zustand = VorleseZustand.SPRICHT,
                absatzNummer = index + 1,
            )
            VorleseDienst.aktualisiere(
                appContext,
                _stand.value.titel,
                index + 1,
                absaetze.size,
                pausiert = false,
            )
            spieleDatei(datei)
            datei.delete()
            if (index < absaetze.lastIndex) delay(ABSATZ_PAUSE_MS)
        }
    }

    /**
     * Fällt ein Absatz beim Dienst durch, wird er halbiert und erneut geschickt
     * (Retry-Split, Baustein D 4.4).
     */
    private suspend fun synthetisiereMitTeilung(absatz: String): File = try {
        synthese.synthetisiere(absatz)
    } catch (fehler: SyntheseFehler) {
        val mitte = absatz.length / 2
        val schnitt = absatz.lastIndexOf(' ', mitte).takeIf { it > 0 } ?: mitte
        IdeenLog.warn("Vorleser", "synthetisiereMitTeilung", "Absatz halbiert", mapOf("chars" to absatz.length))
        val ersteHaelfte = synthese.synthetisiere(absatz.substring(0, schnitt).trim())
        // Die zweite Hälfte wird nach der ersten geholt; ein zweiter Fehlschlag zählt als echt.
        val zweiteHaelfte = synthese.synthetisiere(absatz.substring(schnitt).trim())
        spieleDatei(ersteHaelfte)
        ersteHaelfte.delete()
        zweiteHaelfte
    }

    /** Rückfall für Microsoft Edge: Absatz für Absatz über den vorhandenen Player. */
    private suspend fun reihum(absaetze: List<String>) {
        absaetze.forEachIndexed { index, absatz ->
            _stand.value = _stand.value.copy(zustand = VorleseZustand.SPRICHT, absatzNummer = index + 1)
            VorleseDienst.aktualisiere(appContext, _stand.value.titel, index + 1, absaetze.size, pausiert = false)
            suspendCancellableCoroutine { fortsetzung ->
                fortsetzung.invokeOnCancellation { ttsManager.stop() }
                ttsManager.speak(
                    text = absatz,
                    onStart = {},
                    onComplete = { if (fortsetzung.isActive) fortsetzung.resume(Unit) },
                    onError = { fehler ->
                        if (fortsetzung.isActive) {
                            fortsetzung.resume(Unit)
                            IdeenLog.warn("Vorleser", "reihum", "Absatz übersprungen", mapOf("art" to fehler.javaClass.simpleName))
                        }
                    },
                )
            }
            if (index < absaetze.lastIndex) delay(ABSATZ_PAUSE_MS)
        }
    }

    private suspend fun spieleDatei(datei: File) {
        fordereFokusAn()
        var player: MediaPlayer? = null
        try {
            suspendCancellableCoroutine { fortsetzung ->
                val neuerPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    setDataSource(datei.absolutePath)
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
                    prepare()
                    start()
                }
                player = neuerPlayer
                spieler = neuerPlayer
                fortsetzung.invokeOnCancellation { runCatching { neuerPlayer.stop() } }
            }
        } finally {
            player?.let { alter ->
                runCatching { alter.release() }
                if (spieler === alter) spieler = null
            }
        }
    }

    fun pause() {
        val player = spieler ?: return
        runCatching { player.pause() }
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
        val player = spieler ?: return
        runCatching { player.start() }
        _stand.value = _stand.value.copy(zustand = VorleseZustand.SPRICHT)
        VorleseDienst.aktualisiere(
            appContext,
            _stand.value.titel,
            _stand.value.absatzNummer,
            _stand.value.absaetzeGesamt,
            pausiert = false,
        )
    }

    fun stopp() {
        laufenderJob?.cancel()
        laufenderJob = null
        ttsManager.stop()
        spieler?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        spieler = null
        gibFokusFrei()
        VorleseDienst.beenden(appContext)
        _stand.value = VorleseStand()
    }

    fun fehlerGelesen() {
        _stand.value = _stand.value.copy(fehler = null)
    }

    private fun fordereFokusAn() {
        if (fokusAnfrage != null) return
        val zuhoerer = AudioManager.OnAudioFocusChangeListener { aenderung ->
            when (aenderung) {
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                -> if (_stand.value.zustand == VorleseZustand.SPRICHT) {
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val anfrage = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setOnAudioFocusChangeListener(zuhoerer)
                .build()
            fokusAnfrage = anfrage
            audioManager?.requestAudioFocus(anfrage)
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
    const val PAUSE = "de.frank.genialeideen.VORLESEN_PAUSE"
    const val WEITER = "de.frank.genialeideen.VORLESEN_WEITER"
    const val STOPP = "de.frank.genialeideen.VORLESEN_STOPP"

    fun intent(context: Context, aktion: String): Intent =
        Intent(context, VorleseDienst::class.java).setAction(aktion)
}
