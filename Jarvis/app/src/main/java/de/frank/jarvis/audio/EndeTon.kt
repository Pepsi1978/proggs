package de.frank.jarvis.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Fallender Doppelton „DÖ-DO“ am echten Ende einer Sprachsession: zwei tiefe, klar getrennte Töne, der zweite tiefer.
 * Über die Medienlautstärke wie Piepton und Antwort; steht der Freisprech-Kanal zum Konferenz-Lautsprecher, über den
 * Gesprächskanal (dann gilt die Gesprächslautstärke).
 */
object EndeTon {
    private const val RATE = 44_100
    // Zwei tiefe, klar getrennte Töne (Franks Wunsch): erst H4, nach kurzer Pause E4.
    private const val HOCH_HZ = 494.0     // H4
    private const val TIEF_HZ = 330.0     // E4
    private const val HOCH_MS = 240
    private const val PAUSE_MS = 90
    private const val TIEF_MS = 360
    private const val RAMPE_MS = 15
    /** Drei Viertel der bisherigen Lautstärke (Franks Wunsch): 0,7 × 0,75. */
    private const val PEGEL = 0.7 * 0.75
    /** Nach dem letzten Abtastwert noch etwas nachlaufen lassen: Über Bluetooth kommt der Ton verzögert an. */
    private const val NACHLAUF_MS = 400L

    private val pcm: ShortArray by lazy { ton(HOCH_HZ, HOCH_MS) + ShortArray(RATE * PAUSE_MS / 1000) + ton(TIEF_HZ, TIEF_MS) }

    private fun ton(hz: Double, ms: Int): ShortArray {
        val n = RATE * ms / 1000
        val rampe = RATE * RAMPE_MS / 1000
        return ShortArray(n) { i ->
            val huelle = min(1.0, min(i.toDouble() / rampe, (n - 1 - i).toDouble() / rampe))
            (sin(2 * PI * hz * i / RATE) * huelle * PEGEL * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /**
     * Spielt den Doppelton. [abbrechen] wird laufend gefragt: true (neue Session, Aufnahme, Vorlesen) beendet den Ton sofort.
     * true = der Ton lief vollständig.
     */
    suspend fun spielen(context: Context, ueberKanal: Boolean, abbrechen: () -> Boolean): Boolean {
        val daten = pcm
        val attribute = AudioAttributes.Builder()
            .setUsage(if (ueberKanal) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val spur = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(attribute)
                .setAudioFormat(AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(daten.size * 2)
                .build()
        }.getOrNull() ?: return false
        // Kurz den Ton für sich: Musik, die nach dem Gespräch weiterläuft, wird für den Doppelton leiser.
        val am = context.applicationContext.getSystemService(AudioManager::class.java)
        val fokus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attribute).build()
        try {
            if (spur.write(daten, 0, daten.size) != daten.size) return false
            am?.let { runCatching { it.requestAudioFocus(fokus) } }
            spur.play()
            // Bis der letzte Abtastwert abgespielt ist (mit Obergrenze), dann der Nachlauf; jederzeit abbrechbar.
            val frist = SystemClock.elapsedRealtime() + (HOCH_MS + PAUSE_MS + TIEF_MS) + 1_500L
            while (spur.playbackHeadPosition < daten.size && SystemClock.elapsedRealtime() < frist) {
                if (abbrechen()) return false
                delay(20)
            }
            val ende = SystemClock.elapsedRealtime() + NACHLAUF_MS
            while (SystemClock.elapsedRealtime() < ende) {
                if (abbrechen()) return false
                delay(20)
            }
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return false
        } finally {
            runCatching { spur.stop() }
            spur.release()
            am?.let { runCatching { it.abandonAudioFocusRequest(fokus) } }
        }
    }
}
