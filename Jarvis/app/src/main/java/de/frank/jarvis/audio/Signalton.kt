package de.frank.jarvis.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTimestamp
import android.media.AudioTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Spielt kurze, selbst erzeugte Töne (Bereit-Piepton, Endton) und liefert, wann ihr letzter Abtastwert wirklich zu hören war.
 * Grundlage ist der Zeitstempel der Tonausgabe ([AudioTrack.getTimestamp]), der die Latenz von Lautsprecher und Bluetooth
 * enthält: Ab diesem Ende zählt die Stillezeit („so, wie Frank den Ton hört“).
 */
object Signalton {
    const val RATE = 44_100

    /** Sinus-Mischung mit weicher Ein- und Ausblendung; [anteile] = Frequenz zu Gewicht. */
    fun ton(anteile: List<Pair<Double, Double>>, ms: Int, pegel: Double, rampeMs: Int): ShortArray {
        val n = RATE * ms / 1000
        val rampe = (RATE * rampeMs / 1000).coerceAtLeast(1)
        val summe = anteile.sumOf { it.second }
        return ShortArray(n) { i ->
            val huelle = min(1.0, min(i.toDouble() / rampe, (n - 1 - i).toDouble() / rampe))
            var wert = 0.0
            for ((hz, gewicht) in anteile) wert += gewicht * sin(2 * PI * hz * i / RATE)
            (wert / summe * huelle * pegel * Short.MAX_VALUE).toInt().toShort()
        }
    }

    fun stille(ms: Int) = ShortArray(RATE * ms / 1000)

    /**
     * Spielt [daten] und kehrt zurück, sobald der letzte Abtastwert zu hören war. Ergebnis: dieser Zeitpunkt
     * (System.currentTimeMillis), null = nicht gespielt oder per [abbrechen] beendet. [fokus] = Musik kurz leiser.
     */
    suspend fun spielen(context: Context, daten: ShortArray, ueberKanal: Boolean, fokus: Boolean, abbrechen: () -> Boolean = { false }): Long? {
        val attribute = AudioAttributes.Builder()
            .setUsage(if (ueberKanal) AudioAttributes.USAGE_VOICE_COMMUNICATION else AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
        val minPuffer = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(2_048)
        val spur = runCatching {
            AudioTrack.Builder().setAudioAttributes(attribute).setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM).setBufferSizeInBytes(maxOf(minPuffer, daten.size * 2)).build()
        }.getOrNull() ?: return null
        val am = if (fokus) context.applicationContext.getSystemService(AudioManager::class.java) else null
        val fokusAnfrage = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(attribute).build()
        try {
            // Mit Stille bis zur vollen Puffergröße auffüllen: Ein Strom-Track startet erst, wenn sein Puffer gefüllt ist (ab API 31),
            // und die Stille danach lässt die Ausgabe weiterlaufen, sodass ihr Zeitstempel über das Ton-Ende hinaus meldet.
            val tonFrames = daten.size
            val gesamt = maxOf(spur.bufferSizeInFrames, tonFrames) + RATE / 10
            val voll = daten.copyOf(gesamt)
            am?.let { runCatching { it.requestAudioFocus(fokusAnfrage) } }
            spur.play()
            val geschrieben = withContext(Dispatchers.IO) { spur.write(voll, 0, voll.size, AudioTrack.WRITE_NON_BLOCKING) }
            if (geschrieben < tonFrames) return null
            val frist = System.nanoTime() + (tonFrames * 1000L / RATE + 2_000L) * 1_000_000L
            val ts = AudioTimestamp()
            while (System.nanoTime() < frist) {
                if (abbrechen()) return null
                if (runCatching { spur.getTimestamp(ts) }.getOrDefault(false) && ts.framePosition >= tonFrames) {
                    // Gemeldet: Abtastwert framePosition kam um nanoTime am Ausgang an; das Ton-Ende lag entsprechend davor.
                    val endeNano = ts.nanoTime - (ts.framePosition - tonFrames) * 1_000_000_000L / RATE
                    return System.currentTimeMillis() - (System.nanoTime() - endeNano) / 1_000_000L
                }
                if (spur.playbackHeadPosition >= tonFrames && !runCatching { spur.getTimestamp(ts) }.getOrDefault(false)) {
                    // Gerät ohne Zeitstempel (oft Gesprächskanal): Abspielkopf plus geschätzte Ausgabelatenz.
                    val latenz = if (ueberKanal) 150L else 40L
                    delay(latenz)
                    return System.currentTimeMillis()
                }
                delay(5)
            }
            // Nie gestartet: Ersatzweg des Aufrufers (ToneGenerator) nehmen.
            return null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        } finally {
            runCatching { spur.stop() }
            spur.release()
            am?.let { runCatching { it.abandonAudioFocusRequest(fokusAnfrage) } }
        }
    }
}
