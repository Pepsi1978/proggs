package de.frank.jarvis.tts

import android.media.MediaPlayer
import android.media.audiofx.Visualizer
import android.os.SystemClock
import java.util.WeakHashMap
import kotlin.math.sqrt

/**
 * Lautstärke der gerade gesprochenen Antwort für die Sprachblase. Jeder Player läuft über [SpeechLoudness], dort meldet
 * er sich hier an. Gemessen wird nur, solange die Blase hängt ([aktiv]): ein Visualizer auf der Tonspur des Players mit
 * wenigen Wellenformen je Sekunde. Klappt das nicht, bleibt [pegel] null und die Blase bewegt sich ersatzweise; die
 * Wiedergabe selbst merkt davon nie etwas.
 */
object WiedergabePegel {
    @Volatile var aktiv = false
    private val messer = WeakHashMap<MediaPlayer, Visualizer>()
    @Volatile private var wert = 0f
    @Volatile private var gemessenUm = 0L

    /** Letzter Pegel 0…1, null ohne frische Messung. */
    val pegel: Float? get() = if (SystemClock.uptimeMillis() - gemessenUm < 400) wert else null

    fun anmelden(player: MediaPlayer) {
        if (!aktiv) return
        runCatching {
            val v = Visualizer(player.audioSessionId)
            v.captureSize = Visualizer.getCaptureSizeRange()[0]
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer?, form: ByteArray?, rate: Int) {
                    val f = form ?: return
                    var summe = 0.0
                    for (b in f) { val s = (b.toInt() and 0xFF) - 128; summe += (s * s).toDouble() }
                    val rms = sqrt(summe / f.size.coerceAtLeast(1)) / 128.0
                    wert = (sqrt(rms) * 1.5).toFloat().coerceIn(0f, 1f)
                    gemessenUm = SystemClock.uptimeMillis()
                }
                override fun onFftDataCapture(vis: Visualizer?, fft: ByteArray?, rate: Int) = Unit
            }, minOf(Visualizer.getMaxCaptureRate(), 20_000), true, false)
            v.enabled = true
            synchronized(messer) { messer[player] = v }
        }
    }

    fun abmelden(player: MediaPlayer?) {
        val v = synchronized(messer) { messer.remove(player) } ?: return
        runCatching { v.enabled = false; v.release() }
    }
}
