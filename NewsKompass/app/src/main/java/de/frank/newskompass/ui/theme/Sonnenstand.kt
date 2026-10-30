package de.frank.newskompass.ui.theme

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Wechselzeiten für Neuenhagen bei Berlin, offline berechnet (Sonnenaufgangsgleichung): hell ab Sonnenaufgang, dunkel ab dem Ende der bürgerlichen Dämmerung. */
object Sonnenstand {
    private const val BREITE = 52.529
    private const val LAENGE = 13.689
    private const val TAG_MS = 86_400_000L
    private const val HOEHE_AUFGANG = -0.833
    private const val HOEHE_DUNKEL = -6.0

    /** Beginn von Hell und Dunkel als Epochen-Millisekunden. Beide liegen in Neuenhagen stets auf demselben UTC-Tag. */
    private fun aufUndUntergang(utcTag: Long): Pair<Long, Long> {
        val j = utcTag - 10957 - LAENGE / 360
        val m = 357.5291 + 0.98560028 * j
        val c = 1.9148 * sinG(m) + 0.02 * sinG(2 * m) + 0.0003 * sinG(3 * m)
        val laenge = m + c + 282.9372
        val mittag = j + 0.0053 * sinG(m) - 0.0069 * sinG(2 * laenge)
        val sinDekl = sinG(laenge) * sinG(23.4397)
        val cosDekl = sqrt(1 - sinDekl * sinDekl)
        fun stundenwinkel(hoehe: Double) = Math.toDegrees(acos((sinG(hoehe) - sinG(BREITE) * sinDekl) / (cos(Math.toRadians(BREITE)) * cosDekl)))
        return zeit(mittag - stundenwinkel(HOEHE_AUFGANG) / 360) to zeit(mittag + stundenwinkel(HOEHE_DUNKEL) / 360)
    }

    private fun sinG(grad: Double) = sin(Math.toRadians(grad))

    /** Tage seit dem 1.1.2000, 12 Uhr UTC, als Epochen-Millisekunden. */
    private fun zeit(j2000: Double) = ((j2000 + 10957.5) * TAG_MS).toLong()

    fun istDunkel(jetzt: Long = System.currentTimeMillis()): Boolean {
        val (auf, unter) = aufUndUntergang(Math.floorDiv(jetzt, TAG_MS))
        return jetzt < auf || jetzt >= unter
    }
}
