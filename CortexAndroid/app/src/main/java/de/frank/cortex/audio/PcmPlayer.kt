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
 * Spielt rohes PCM (Gemini-TTS: 24 kHz, 16-bit, mono) ab.
 *
 * WICHTIG: `playAndAwait` ist suspend und kehrt erst zurueck, wenn das Haeppchen WIRKLICH
 * fertig abgespielt ist. So koennen mehrere Haeppchen sauber NACHEINANDER laufen, ohne sich
 * gegenseitig abzuschneiden (das war der Bug: das alte fire-and-forget play() stoppte das
 * vorige Haeppchen sofort wieder). Abbrechbar: stop() ODER Coroutine-Cancel beendet sofort.
 */
class PcmPlayer {

    @Volatile private var player: AudioTrack? = null
    @Volatile private var playing = false

    companion object {
        const val SAMPLE_RATE = 24000
        const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_FRAME = 2 // 16-bit mono
    }

    suspend fun playAndAwait(pcmData: ByteArray) = withContext(Dispatchers.IO) {
        stop() // evtl. Reste sicher beenden
        if (pcmData.isEmpty()) return@withContext

        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING).coerceAtLeast(8192)
        val track = AudioTrack.Builder()
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
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        player = track
        playing = true
        track.play()

        try {
            // 1) Alle Daten in den Stream schreiben (write blockiert, wenn der Puffer voll ist).
            var offset = 0
            while (offset < pcmData.size && isActive && playing) {
                val n = track.write(pcmData, offset, minOf(minBuf, pcmData.size - offset))
                if (n <= 0) break
                offset += n
            }
            // 2) Auf das ECHTE Ende der Wiedergabe warten (Puffer leerspielen lassen).
            val totalFrames = pcmData.size / BYTES_PER_FRAME
            while (isActive && playing &&
                track.playState == AudioTrack.PLAYSTATE_PLAYING &&
                track.playbackHeadPosition < totalFrames
            ) {
                delay(40)
            }
        } catch (e: Exception) {
            CortexLog.error("PcmPlayer", "playAndAwait", "Abspiel-Fehler: ${e.message}")
        } finally {
            try { track.stop() } catch (_: Exception) {}
            try { track.release() } catch (_: Exception) {}
            if (player === track) {
                player = null
                playing = false
            }
        }
    }

    /** Bricht laufende Wiedergabe SOFORT ab (Lautsprecher-Knopf / neuer Text). */
    fun stop() {
        playing = false
        val p = player
        player = null
        try { p?.stop() } catch (_: Exception) {}
        try { p?.release() } catch (_: Exception) {}
    }

    fun isPlaying(): Boolean = playing
}
