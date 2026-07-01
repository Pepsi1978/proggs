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

    suspend fun playAndAwait(mp3Data: ByteArray, speed: Float = 1.0f) {
        if (mp3Data.isEmpty()) return
        stop() // evtl. Reste sicher beenden

        // MediaPlayer braucht eine Datei-/FD-Quelle. Pro Haeppchen eine eigene Temp-Datei, damit
        // sich aufeinanderfolgende Haeppchen nicht in die Quere kommen.
        val file = withContext(Dispatchers.IO) {
            val f = File(context.cacheDir, "edge_tts_${counter++}.mp3")
            f.writeBytes(mp3Data)
            f
        }

        try {
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
                try {
                    mp.setDataSource(file.absolutePath)
                    mp.setOnPreparedListener {
                        try {
                            if (speed != 1.0f) {
                                mp.playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.5f, 2.0f))
                            }
                        } catch (e: Exception) {
                            CortexLog.warn("Mp3Player", "playAndAwait", "Tempo nicht setzbar: ${e.message}")
                        }
                        mp.start()
                    }
                    mp.setOnCompletionListener { finishOnce() }
                    mp.setOnErrorListener { _, what, extra ->
                        CortexLog.warn("Mp3Player", "playAndAwait", "MediaPlayer-Fehler ($what/$extra)")
                        finishOnce()
                        true
                    }
                    mp.prepareAsync()
                } catch (e: Exception) {
                    CortexLog.error("Mp3Player", "playAndAwait", "Abspiel-Fehler: ${e.message}")
                    finishOnce()
                }
                cont.invokeOnCancellation {
                    try { mp.stop() } catch (_: Exception) {}
                    try { mp.release() } catch (_: Exception) {}
                }
            }
        } finally {
            try { player?.release() } catch (_: Exception) {}
            player = null
            playing = false
            try { file.delete() } catch (_: Exception) {}
        }
    }

    /** Bricht laufende Wiedergabe SOFORT ab. */
    fun stop() {
        playing = false
        val p = player
        player = null
        try { p?.stop() } catch (_: Exception) {}
        try { p?.release() } catch (_: Exception) {}
    }

    fun isPlaying(): Boolean = playing
}
