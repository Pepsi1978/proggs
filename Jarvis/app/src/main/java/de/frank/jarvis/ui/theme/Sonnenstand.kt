package de.frank.jarvis.ui.theme

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Wechselzeiten für Berlin, offline berechnet (Sonnenaufgangsgleichung): hell ab Beginn der bürgerlichen Morgendämmerung, dunkel ab Sonnenuntergang (Beginn der Abenddämmerung). */
object Sonnenstand {
    private const val BREITE = 52.520
    private const val LAENGE = 13.405
    private const val TAG_MS = 86_400_000L
    /** Absichtlich ungleich: hell schon ab Beginn der bürgerlichen Morgendämmerung (Sonne 6° unter dem Horizont). */
    private const val HOEHE_HELL = -6.0
    /** Sonnenuntergang (Oberrand am Horizont, mit Refraktion): Beginn der Abenddämmerung; ab hier gilt die Darstellung als dunkel. */
    private const val HOEHE_SONNENUNTERGANG = -0.833

    /** Sonnenuntergang (Beginn der Abenddämmerung) in Berlin. */
    const val SONNENUNTERGANG = HOEHE_SONNENUNTERGANG
    /** Ende der bürgerlichen Abenddämmerung: draußen ist es dunkel. */
    const val DUNKEL = -6.0

    /** Abendzeitpunkt am Kalendertag [tag] (Berlin), an dem die Sonne auf [hoehe] Grad sinkt, als Epochen-Millisekunden. */
    fun abend(tag: java.time.LocalDate, hoehe: Double): Long {
        val utcTag = tag.toEpochDay()
        return morgenUndAbend(utcTag, HOEHE_HELL, hoehe).second
    }

    /** Beginn von Hell und Dunkel als Epochen-Millisekunden. Beide liegen in Berlin stets auf demselben UTC-Tag. */
    private fun hellUndDunkelBeginn(utcTag: Long): Pair<Long, Long> = morgenUndAbend(utcTag, HOEHE_HELL, HOEHE_SONNENUNTERGANG)

    private fun morgenUndAbend(utcTag: Long, hoeheMorgen: Double, hoeheAbend: Double): Pair<Long, Long> {
        val j = utcTag - 10957 - LAENGE / 360
        val m = 357.5291 + 0.98560028 * j
        val c = 1.9148 * sinG(m) + 0.02 * sinG(2 * m) + 0.0003 * sinG(3 * m)
        val laenge = m + c + 282.9372
        val mittag = j + 0.0053 * sinG(m) - 0.0069 * sinG(2 * laenge)
        val sinDekl = sinG(laenge) * sinG(23.4397)
        val cosDekl = sqrt(1 - sinDekl * sinDekl)
        fun stundenwinkel(hoehe: Double) = Math.toDegrees(acos((sinG(hoehe) - sinG(BREITE) * sinDekl) / (cos(Math.toRadians(BREITE)) * cosDekl)))
        return zeit(mittag - stundenwinkel(hoeheMorgen) / 360) to zeit(mittag + stundenwinkel(hoeheAbend) / 360)
    }

    private fun sinG(grad: Double) = sin(Math.toRadians(grad))

    /** Tage seit dem 1.1.2000, 12 Uhr UTC, als Epochen-Millisekunden. */
    private fun zeit(j2000: Double) = ((j2000 + 10957.5) * TAG_MS).toLong()

    fun istDunkel(jetzt: Long = System.currentTimeMillis()): Boolean {
        val (hell, dunkel) = hellUndDunkelBeginn(Math.floorDiv(jetzt, TAG_MS))
        return jetzt < hell || jetzt >= dunkel
    }
}
