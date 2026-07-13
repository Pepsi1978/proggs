package de.frank.cortex.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Spielt MP3-Haeppchen (Edge-TTS: 24 kHz, 96 kbit mono) NACHEINANDER ueber MediaPlayer ab.
 *
 * Anders als der PcmPlayer (durchgehender AudioTrack-Stream) baut MediaPlayer pro Haeppchen kurz
 * auf - zwischen zwei Haeppchen entsteht also eine minimale Pause, genau wie in BestJournalFrank
 * (dort pro Absatz ein MediaPlayer). Die Chunk-Vorbereitung im Voraus haelt es trotzdem fluessig.
 *
 * `playAndAwait` ist suspend und kehrt erst zurueck, wenn das Haeppchen fertig gespielt ist -
 * so laufen die Haeppchen sauber sequenziell, nie ueberlappend. stop() bricht sofort ab.
 */
class Mp3Player(private val context: Context) {

    @Volatile private var player: MediaPlayer? = null
    @Volatile private var playing = false
    private var counter = 0

    // stop() muss einen wartenden playAndAwait-Aufrufer AUFWECKEN: MediaPlayer.stop()+release()
    // feuert weder onCompletion noch onError — die geparkte Coroutine hing sonst fuer immer
    // (und ihr finally mit dem Temp-Datei-Delete lief nie).
    @Volatile private var finishActive: (() -> Unit)? = null

    suspend fun playAndAwait(mp3Data: ByteArray, speed: Float = 1.0f) {
        if (mp3Data.isEmpty()) return
        stop() // evtl. Reste sicher beenden

        // MediaPlayer braucht eine Datei-/FD-Quelle. Pro Haeppchen eine eigene Temp-Datei, damit
        // sich aufeinanderfolgende Haeppchen nicht in die Quere kommen. Pfad VOR dem try bilden,
        // Delete im finally — so bleibt auch bei Cancel WAEHREND des Schreibens nichts liegen.
        val file = File(context.cacheDir, "edge_tts_${counter++}.mp3")
        try {
            withContext(Dispatchers.IO) { file.writeBytes(mp3Data) }
            suspendCancellableCoroutine<Unit> { cont ->
                val mp = MediaPlayer()
                player = mp
                playing = true
                var resumed = false
                fun finishOnce() {
                    if (!resumed) {
                        resumed = true
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
                finishActive = { finishOnce() }
                try {
                    mp.setDataSource(file.absolutePath)
                    mp.setOnPreparedListener {
                        try {
                            if (speed != 1.0f) {
                                mp.playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.5f, 2.0f))
                            }
                        } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                            CortexLog.warn("Mp3Player", "playAndAwait", "Tempo nicht setzbar: ${e.message}")
                        }
                        try {
                            mp.start()
                        } catch (e: Exception) {   // Player zwischen prepare und Callback gestoppt/released
                            CortexLog.warn("Mp3Player", "playAndAwait", "start nach prepare nicht möglich: ${e.message}")
                            finishOnce()
                        }
                    }
                    mp.setOnCompletionListener { finishOnce() }
                    mp.setOnErrorListener { _, what, extra ->
                        CortexLog.warn("Mp3Player", "playAndAwait", "MediaPlayer-Fehler ($what/$extra)")
                        finishOnce()
                        true
                    }
                    mp.prepareAsync()
                } catch (e: Exception) {   // no-cancellation-rethrow (kein suspend im try)
                    CortexLog.error("Mp3Player", "playAndAwait", "Abspiel-Fehler: ${e.message}")
                    finishOnce()
                }
                cont.invokeOnCancellation {
                    try { mp.stop() } catch (_: Exception) {}
                    try { mp.release() } catch (_: Exception) {}
                }
            }
        } finally {
            finishActive = null
            try { player?.release() } catch (_: Exception) {}
            player = null
            playing = false
            try { file.delete() } catch (_: Exception) {}
        }
    }

    /** Bricht laufende Wiedergabe SOFORT ab und weckt einen wartenden playAndAwait-Aufrufer. */
    fun stop() {
        playing = false
        val p = player
        player = null
        try { p?.stop() } catch (_: Exception) {}
        try { p?.release() } catch (_: Exception) {}
        finishActive?.invoke()
    }

    fun isPlaying(): Boolean = playing
}
