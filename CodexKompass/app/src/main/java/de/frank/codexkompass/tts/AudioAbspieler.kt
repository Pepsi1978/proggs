package de.frank.codexkompass.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import de.frank.codexkompass.observability.KompassLog
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * Spielt fertige Audio-Häppchen der Reihe nach ab.
 *
 * Der Kern ist [spieleUndWarte]: Die Funktion kehrt erst zurück, wenn GENAU DIESES Häppchen
 * zu Ende gespielt ist. Damit laufen die Absätze sauber nacheinander statt übereinander —
 * das ist der Fehler, den ein „abschicken und vergessen" macht.
 *
 * Der Audiofokus wird für die ganze Vorlese-Reihe einmal angefordert und am Ende wieder
 * abgegeben (Referenz, Baustein D). Nimmt ein Anruf ihn weg, meldet [beiFokusVerlust] das
 * nach oben, damit die Pipeline anhält statt in den Anruf hineinzusprechen.
 */
class AudioAbspieler(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val sperre = Any()

    @Volatile private var spieler: MediaPlayer? = null
    @Volatile private var laeuft = false
    @Volatile private var fokusAnfrage: AudioFocusRequest? = null

    /** Wird gerufen, wenn ein anderer Ton den Fokus übernimmt (Anruf, Wecker). */
    @Volatile var beiFokusVerlust: (() -> Unit)? = null

    fun fordereFokusAn(): Boolean {
        val manager = audioManager ?: return true
        val attribute = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        val anfrage = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attribute)
            .setOnAudioFocusChangeListener { aenderung ->
                when (aenderung) {
                    AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    -> {
                        KompassLog.info("AudioAbspieler", "fokus", "Audiofokus verloren, halte an", mapOf("aenderung" to aenderung))
                        pausiere()
                        beiFokusVerlust?.invoke()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> fortsetzen()
                }
            }
            .build()
        fokusAnfrage = anfrage
        val ergebnis = manager.requestAudioFocus(anfrage)
        return ergebnis == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun gibFokusFrei() {
        val manager = audioManager ?: return
        val anfrage = fokusAnfrage ?: return
        fokusAnfrage = null
        runCatching { manager.abandonAudioFocusRequest(anfrage) }
    }

    /**
     * Schreibt die Bytes in eine Datei im Zwischenspeicher, spielt sie ab und wartet auf das
     * Ende. Die Datei wird danach in jedem Fall gelöscht — auch wenn die Coroutine dabei
     * abgebrochen wird.
     */
    suspend fun spieleUndWarte(audio: ByteArray, endung: String, tempo: Float) = withContext(Dispatchers.IO) {
        if (audio.isEmpty()) return@withContext
        val datei = File(appContext.cacheDir, "kompass_tts_${UUID.randomUUID()}.$endung")
        try {
            datei.writeBytes(audio)
            spieleDatei(datei, tempo)
        } finally {
            datei.delete()
        }
    }

    private suspend fun spieleDatei(datei: File, tempo: Float) =
        suspendCancellableCoroutine { fortsetzung: CancellableContinuation<Unit> ->
            val neuerSpieler = MediaPlayer()
            var beendet = false

            fun beende() {
                if (beendet) return
                beendet = true
                synchronized(sperre) {
                    if (spieler === neuerSpieler) {
                        spieler = null
                        laeuft = false
                    }
                }
                SpeechLoudness.gibFrei(neuerSpieler)
                runCatching { neuerSpieler.release() }
                if (fortsetzung.isActive) fortsetzung.resume(Unit)
            }

            fortsetzung.invokeOnCancellation {
                runCatching { neuerSpieler.stop() }
                SpeechLoudness.gibFrei(neuerSpieler)
                runCatching { neuerSpieler.release() }
                synchronized(sperre) {
                    if (spieler === neuerSpieler) {
                        spieler = null
                        laeuft = false
                    }
                }
            }

            try {
                neuerSpieler.setAudioAttributes(SpeechLoudness.attribute)
                neuerSpieler.setDataSource(datei.absolutePath)
                neuerSpieler.setOnCompletionListener { beende() }
                neuerSpieler.setOnErrorListener { _, was, extra ->
                    KompassLog.error(
                        "AudioAbspieler",
                        "spieleDatei",
                        "Abspielfehler",
                        mapOf("was" to was, "extra" to extra),
                    )
                    beende()
                    true
                }
                neuerSpieler.prepare()
                if (tempo != 1f) {
                    // Zeitdehnend, ohne die Tonhöhe zu verschieben.
                    runCatching {
                        neuerSpieler.playbackParams =
                            neuerSpieler.playbackParams.setSpeed(tempo.coerceIn(0.5f, 2.0f))
                    }.onFailure {
                        KompassLog.warn("AudioAbspieler", "spieleDatei", "Tempo nicht setzbar", mapOf("grund" to it.message))
                    }
                }
                SpeechLoudness.verstaerke(neuerSpieler)
                synchronized(sperre) {
                    spieler = neuerSpieler
                    laeuft = true
                }
                neuerSpieler.start()
            } catch (fehler: Exception) {
                KompassLog.error("AudioAbspieler", "spieleDatei", "Start fehlgeschlagen", mapOf("grund" to fehler.message))
                beende()
            }
        }

    fun pausiere(): Boolean = synchronized(sperre) {
        val aktiv = spieler ?: return@synchronized false
        runCatching { if (aktiv.isPlaying) aktiv.pause() }.isSuccess
    }

    fun fortsetzen(): Boolean = synchronized(sperre) {
        val aktiv = spieler ?: return@synchronized false
        runCatching { aktiv.start() }.isSuccess
    }

    fun stoppe() {
        val aktiv = synchronized(sperre) {
            val alt = spieler
            spieler = null
            laeuft = false
            alt
        }
        runCatching { aktiv?.stop() }
        SpeechLoudness.gibFrei(aktiv)
        runCatching { aktiv?.release() }
        gibFokusFrei()
    }

    fun spieltGerade(): Boolean = laeuft
}
