package de.frank.wecker

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Gewünschte Schlafdauer je Wecker: ungefähre Schlafenszeit = geplante Weckzeit minus echte Dauer. Rein informativ,
 * kein Tracking. Die optionale Erinnerung 15 Minuten vorher plant SchlafErinnerung. Über eine Zeitumstellung bleibt die tatsächliche Schlafdauer erhalten.
 */
object Schlaf {
    const val STEP = 30
    const val MIN = 30
    const val MAX = 24 * 60

    fun valid(minutes: Int) = minutes == 0 || (minutes in MIN..MAX && minutes % STEP == 0)

    fun bedtime(wakeAt: Long, minutes: Int): Long = wakeAt - minutes * 60_000L

    /** "30 Min.", "1 Std.", "8,5 Std.", "24 Std." */
    fun dauer(minutes: Int): String = when {
        minutes < 60 -> "$minutes Min."
        minutes % 60 == 0 -> "${minutes / 60} Std."
        else -> "${minutes / 60},5 Std."
    }

    /** Relative to today, so a time several days away is never shown as a bare clock time. */
    fun tagLabel(time: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val at = Instant.ofEpochMilli(time).atZone(zone)
        val days = ChronoUnit.DAYS.between(Instant.ofEpochMilli(now).atZone(zone).toLocalDate(), at.toLocalDate())
        val clock = at.format(DateTimeFormatter.ofPattern("HH:mm"))
        return when (days) {
            0L -> "heute $clock"
            1L -> "morgen $clock"
            -1L -> "gestern $clock"
            else -> at.format(DateTimeFormatter.ofPattern("EEE, dd.MM. · HH:mm", Locale.GERMAN))
        }
    }

    /** Card and header text; a bedtime in the past is marked explicitly. */
    fun hinweis(wakeAt: Long, minutes: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val bed = bedtime(wakeAt, minutes)
        val label = tagLabel(bed, now, zone)
        return if (bed > now) "Schlafenszeit ≈ $label · ${dauer(minutes)} Schlaf" else "Schlafenszeit war ≈ $label · ${dauer(minutes)} Schlaf"
    }
}
