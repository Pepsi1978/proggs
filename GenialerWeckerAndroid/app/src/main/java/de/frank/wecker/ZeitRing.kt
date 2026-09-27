package de.frank.wecker

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Geometrie des Restzeit-Rings als normale 12-Stunden-Uhr: 12 oben, im Uhrzeigersinn. Winkel in Grad wie bei
 * Canvas.drawArc (0° = 3 Uhr, -90° = 12 Uhr). Ein Bogen entsteht nur, wenn er eindeutig ist: höchstens 12 Stunden
 * Restzeit und keine Zeitumstellung dazwischen. Sonst trägt die Ringmitte eine kurze Kennzeichnung.
 */
object ZeitRing {
    private const val HALF_DAY_MS = 12 * 60 * 60_000L

    data class Geometrie(
        val nowAngle: Float,
        val targetAngle: Float,
        /** Bogenlänge im Uhrzeigersinn ab [nowAngle]; null, wenn kein eindeutiger Bogen möglich ist. */
        val sweep: Float?,
        /** Echte Restzeit, auf volle Minuten aufgerundet. */
        val remainingMinutes: Long,
        /** Kennzeichnung für die Ringmitte, nur ohne Bogen. */
        val centerLabel: String?,
    )

    /** Position auf einem 12-Stunden-Blatt nach lokaler Wanduhrzeit. */
    fun angle(time: Long, zone: ZoneId): Float {
        val seconds = Instant.ofEpochMilli(time).atZone(zone).toLocalTime().toNanoOfDay() / 1_000_000_000.0
        return ((seconds % 43_200.0) / 43_200.0 * 360.0 - 90.0).toFloat()
    }

    /** Aufrunden auf volle Minuten, damit die Restzeit zur angezeigten HH:mm-Uhrzeit passt. */
    fun ceilMinutes(ms: Long): Long = if (ms <= 0) 0 else (ms + 59_999) / 60_000

    fun berechne(now: Long, target: Long, zone: ZoneId = ZoneId.systemDefault()): Geometrie {
        val diff = target - now
        val minutes = ceilMinutes(diff)
        val offsetChanges = zone.rules.getOffset(Instant.ofEpochMilli(now)) != zone.rules.getOffset(Instant.ofEpochMilli(target))
        val unambiguous = diff in 1..HALF_DAY_MS && !offsetChanges
        val label = when {
            unambiguous -> null
            minutes > 24 * 60 -> Instant.ofEpochMilli(target).atZone(zone).format(DateTimeFormatter.ofPattern("EEE", Locale.GERMAN)).removeSuffix(".")
            offsetChanges -> "Umstellung"
            else -> "> 12 Std."
        }
        return Geometrie(angle(now, zone), angle(target, zone), if (unambiguous) diff / HALF_DAY_MS.toFloat() * 360f else null, minutes, label)
    }
}
