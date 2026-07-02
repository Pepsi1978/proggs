package de.frank.cortex.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import de.frank.cortex.observability.CortexLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Spielt rohes PCM (Gemini-TTS: 24 kHz, 16-bit, mono) NAHTLOS am Stueck ab, waehrend die
 * Haeppchen einzeln im Hintergrund nachgeliefert werden.
 *
 * 2026-07-01 (Performance-Ueberarbeitung): Frueher wurde PRO Haeppchen ein komplett neuer
 * AudioTrack gebaut, gestartet und danach wieder verworfen (`playAndAwait`) - das kostete bei
 * JEDEM Uebergang zwischen zwei Haeppchen erneut Builder+MinBufferSize+play()-Overhead und eine
 * winzige Stille. Jetzt laeuft EIN durchgehender `AudioTrack`-Stream fuer die gesamte Antwort:
 * `start()` einmal zu Beginn (darf parallel zur ersten TTS-Netzwerkanfrage laufen), danach pro
 * Haeppchen nur noch `writeAndAwait()`.
 *
 * WICHTIG: `writeAndAwait` ist suspend und kehrt erst zurueck, wenn GENAU DIESES Haeppchen
 * fertig abgespielt ist (Reihenfolge = Wiedergabe-Reihenfolge). So laufen mehrere Haeppchen
 * weiterhin sauber NACHEINANDER, nie ueberlappend (das war der urspruengliche Bug: fire-and-forget
 * play() stoppte das vorige Haeppchen sofort wieder). Abbrechbar: stop() ODER Coroutine-Cancel
 * beendet sofort.
 */
class PcmPlayer {

    @Volatile private var track: AudioTrack? = null
    @Volatile private var playing = false
    @Volatile private var framesWrittenTotal = 0L
    private var minBufBytes = 8192

    companion object {
        const val SAMPLE_RATE = 24000
        const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_FRAME = 2 // 16-bit mono
    }

    /**
     * Startet die durchgehende Wiedergabe-Sitzung fuer EINE komplette Antwort. Darf aufgerufen
     * werden, WAEHREND der erste Text-Haeppchen noch beim TTS-Dienst synthetisiert wird (spart
     * Zeit bis zum ersten hoerbaren Ton, weil AudioTrack-Setup und Netzwerk-Roundtrip parallel
     * statt nacheinander laufen).
     */
    suspend fun start(speed: Float = 1.0f) = withContext(Dispatchers.IO) {
        stop() // evtl. Reste einer vorigen Sitzung sicher beenden
        minBufBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING).coerceAtLeast(8192)
        val newTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL)
                    .setEncoding(ENCODING)
                    .build()
            )
            .setBufferSizeInBytes(minBufBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        // Sprechtempo: zeitdehnend, ohne Tonhoehen-Aenderung (setSpeed laesst pitch bei 1.0).
        if (speed != 1.0f) {
            try {
                newTrack.playbackParams = android.media.PlaybackParams().setSpeed(speed.coerceIn(0.5f, 2.0f))
            } catch (e: Exception) {
                CortexLog.warn("PcmPlayer", "start", "Tempo nicht setzbar: ${e.message}")
            }
        }
        newTrack.play()
        framesWrittenTotal = 0L
        track = newTrack
        playing = true
    }

    /**
     * Schreibt ein Haeppchen in den LAUFENDEN Stream (kein Stop/Neustart des Tracks dazwischen -
     * das ist der Kern der Nahtlosigkeit) und wartet, bis GENAU DIESES Haeppchen fertig
     * abgespielt ist, bevor die Funktion zurueckkehrt.
     */
    suspend fun writeAndAwait(pcmData: ByteArray) = withContext(Dispatchers.IO) {
        val t = track
        if (t == null || pcmData.isEmpty() || !playing) return@withContext

        try {
            var offset = 0
            while (offset < pcmData.size && isActive && playing && track === t) {
                val n = t.write(pcmData, offset, minOf(minBufBytes, pcmData.size - offset))
                if (n <= 0) break
                offset += n
            }
            // Nur die WIRKLICH geschriebenen Bytes zaehlen - sonst wuerde bei einem Abbruch
            // mitten im Schreiben unten auf eine Position gewartet, die nie erreicht wird.
            framesWrittenTotal += offset / BYTES_PER_FRAME

            while (isActive && playing && track === t &&
                t.playState == AudioTrack.PLAYSTATE_PLAYING &&
                t.playbackHeadPosition < framesWrittenTotal
            ) {
                delay(40)
            }
        } catch (e: Exception) {
            CortexLog.error("PcmPlayer", "writeAndAwait", "Abspiel-Fehler: ${e.message}")
        }
    }

    /**
     * Bequemlichkeits-Variante fuer EIN einzelnes Haeppchen (z.B. Teststimme in den Einstellungen):
     * startet eine kurze Sitzung, spielt ab, raeumt danach sofort wieder auf.
     */
    suspend fun playAndAwait(pcmData: ByteArray, speed: Float = 1.0f) {
        // finally: auch bei Coroutine-Abbruch mitten im Abspielen (z.B. Stimmen-Test wird durch
        // Screen-Wechsel gecancelt) wird der AudioTrack sicher gestoppt + freigegeben (kein Leak).
        try {
            start(speed)
            writeAndAwait(pcmData)
        } finally {
            stop()
        }
    }

    /** Bricht laufende Wiedergabe SOFORT ab (Lautsprecher-Knopf / neuer Text / Sitzungsende). */
    fun stop() {
        playing = false
        val t = track
        track = null
        framesWrittenTotal = 0L
        try { t?.stop() } catch (_: Exception) {}
        try { t?.release() } catch (_: Exception) {}
    }

    fun isPlaying(): Boolean = playing
}
