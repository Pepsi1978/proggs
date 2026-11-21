package de.frank.jarvis.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Fallender Doppelton „DÖ-DO“ am echten Ende einer Sprachsession: erst höher, dann tiefer, ohne Lücke.
 * Über die Medienlautstärke wie Piepton und Antwort.
 */
object EndeTon {
    private const val RATE = 44_100
    private const val HOCH_HZ = 1_175.0   // D6
    private const val TIEF_HZ = 784.0     // G5
    private const val HOCH_MS = 110
    private const val TIEF_MS = 170
    private const val RAMPE_MS = 8

    private val pcm: ShortArray by lazy { ton(HOCH_HZ, HOCH_MS) + ton(TIEF_HZ, TIEF_MS) }

    private fun ton(hz: Double, ms: Int): ShortArray {
        val n = RATE * ms / 1000
        val rampe = RATE * RAMPE_MS / 1000
        return ShortArray(n) { i ->
            val huelle = min(1.0, min(i.toDouble() / rampe, (n - 1 - i).toDouble() / rampe))
            (sin(2 * PI * hz * i / RATE) * huelle * 0.55 * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** true = der Ton lief wirklich. */
    suspend fun spielen(ueberKanal: Boolean): Boolean = withContext(Dispatchers.IO) {
        val daten = pcm
        val spur = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(if (ueberKanal) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(daten.size * 2)
                .build()
        }.getOrNull() ?: return@withContext false
        try {
            if (spur.write(daten, 0, daten.size) <= 0) return@withContext false
            spur.play()
            // Großzügig nachlaufen lassen: Über Bluetooth kommt der Ton verzögert, ein frühes stop() schnitte den tiefen ab.
            delay((HOCH_MS + TIEF_MS + 300).toLong())
            true
        } catch (e: Exception) {
            false
        } finally {
            runCatching { spur.stop() }
            spur.release()
        }
    }
}
