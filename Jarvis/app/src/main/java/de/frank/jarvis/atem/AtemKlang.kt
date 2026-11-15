package de.frank.jarvis.atem

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Atemgeräusch, zur Laufzeit erzeugt: Phasen von 3 bis 20 Sekunden lassen sich mit einer festen Aufnahme nicht sauber strecken.
 * Rosa Rauschen durch wandernde Bandpässe (Mund-/Nasenraum), weiche Hüllkurve und ein leichtes Flattern wie bei echtem Atem.
 * Einatmen: hell werdend, Luft strömt herein. Ausatmen: weicher, tiefer werdend, langsam auslaufend.
 */
object AtemKlang {
    const val RATE = 24_000
    private val cache = HashMap<String, ShortArray>()

    fun ton(einatmen: Boolean, ms: Long): ShortArray = synchronized(cache) {
        cache.getOrPut("${einatmen}_$ms") { erzeuge(einatmen, ms) }
    }

    private class Bandpass {
        private var x1 = 0.0; private var x2 = 0.0; private var y1 = 0.0; private var y2 = 0.0
        private var b0 = 0.0; private var b2 = 0.0; private var a1 = 0.0; private var a2 = 0.0
        fun setze(f: Double, q: Double) {
            val w = 2 * PI * f / RATE
            val alpha = sin(w) / (2 * q)
            val a0 = 1 + alpha
            b0 = alpha / a0; b2 = -alpha / a0; a1 = -2 * cos(w) / a0; a2 = (1 - alpha) / a0
        }
        fun lauf(x: Double): Double {
            val y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1; x1 = x; y2 = y1; y1 = y
            return y
        }
    }

    private fun glatt(a: Double, b: Double, x: Double): Double {
        val t = ((x - a) / (b - a)).coerceIn(0.0, 1.0)
        return t * t * (3 - 2 * t)
    }

    private fun logLerp(a: Double, b: Double, t: Double) = exp(ln(a) + (ln(b) - ln(a)) * t)

    private fun erzeuge(einatmen: Boolean, ms: Long): ShortArray {
        val n = (ms * RATE / 1000).toInt().coerceAtLeast(1)
        val zufall = Random(if (einatmen) 17 else 29)
        val koerper = Bandpass(); val luft = Bandpass(); val tief = Bandpass()
        // Rosa Rauschen (Paul Kellet, sparsame Fassung).
        var p0 = 0.0; var p1 = 0.0; var p2 = 0.0
        var flatter = 0.0; var flatterZiel = 0.0
        val roh = DoubleArray(n)
        var spitze = 1e-9
        for (i in 0 until n) {
            val x = i.toDouble() / n
            if (i % 64 == 0) {
                if (einatmen) {
                    koerper.setze(logLerp(520.0, 1350.0, glatt(0.0, 0.9, x)), 0.85)
                    luft.setze(logLerp(2100.0, 3300.0, x), 1.4)
                    tief.setze(320.0, 0.7)
                } else {
                    koerper.setze(logLerp(1150.0, 430.0, glatt(0.0, 1.0, x)), 0.7)
                    luft.setze(logLerp(2600.0, 1700.0, x), 1.2)
                    tief.setze(260.0, 0.6)
                }
            }
            if (i % 2400 == 0) flatterZiel = zufall.nextDouble(-1.0, 1.0)
            flatter += (flatterZiel - flatter) * 0.0004
            val w = zufall.nextDouble(-1.0, 1.0)
            p0 = 0.99765 * p0 + w * 0.0990460
            p1 = 0.96300 * p1 + w * 0.2965164
            p2 = 0.57000 * p2 + w * 1.0526913
            val rosa = (p0 + p1 + p2 + w * 0.1848) * 0.25
            val huelle = if (einatmen) {
                glatt(0.0, 0.22, x) * (1 - glatt(0.80, 1.0, x)) * (0.78 + 0.22 * sin(PI * x))
            } else {
                glatt(0.0, 0.10, x) * (1 - x).pow(0.45) * (1 - glatt(0.88, 1.0, x))
            }
            val mix = if (einatmen) koerper.lauf(rosa) * 1.0 + luft.lauf(rosa) * 0.38 + tief.lauf(rosa) * 0.25
            else koerper.lauf(rosa) * 1.0 + luft.lauf(rosa) * 0.22 + tief.lauf(rosa) * 0.45
            val v = mix * huelle * (1 + 0.09 * flatter)
            roh[i] = v
            if (abs(v) > spitze) spitze = abs(v)
        }
        val ziel = (if (einatmen) 0.30 else 0.27) * Short.MAX_VALUE / spitze
        return ShortArray(n) { (roh[it] * ziel).toInt().coerceIn(-32767, 32767).toShort() }
    }

    /** Spielbarer Ton im Static-Modus: pausier- und fortsetzbar. */
    fun spur(daten: ShortArray, lautstaerke: Float): AudioTrack? = runCatching {
        AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(daten.size * 2)
            .build().apply { write(daten, 0, daten.size); setVolume(lautstaerke) }
    }.getOrNull()
}
