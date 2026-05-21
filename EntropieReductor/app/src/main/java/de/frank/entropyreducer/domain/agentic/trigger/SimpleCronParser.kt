package de.frank.entropyreducer.domain.agentic.trigger

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Sehr einfacher Cron-Parser fuer Frank's Auto-Trigger (Frank-Wunsch 2026-05-21).
 *
 * Unterstuetzte DSL-Formate:
 *  - `"daily HH:mm"`           — jeden Tag zur Uhrzeit, z.B. "daily 19:00"
 *  - `"weekly DOW HH:mm"`      — jede Woche an einem Wochentag, z.B. "weekly SUN 19:00"
 *                                DOW: MON | TUE | WED | THU | FRI | SAT | SUN
 *  - `"hourly :MM"`            — jede Stunde zur Minute, z.B. "hourly :05"
 *
 * Mehr Formate koennen spaeter ergaenzt werden. Das hier sind die 3 Faelle die
 * Frank in der TODO-Spec konkret nannte.
 *
 * Zeitzone: System-Default (lokale Zeit). DST wird ueber LocalDateTime + ZoneId
 * korrekt behandelt.
 */
object SimpleCronParser {

    private val TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm")

    /**
     * Berechnet den naechsten Trigger-Zeitpunkt (als Epoch-Millis) ab `now`.
     * Returnt null wenn der cron-String nicht geparst werden kann.
     */
    fun nextFireAt(cron: String, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): Long? {
        val tokens = cron.trim().split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return null

        val nowLdt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(now), zone)

        return when (tokens[0].lowercase()) {
            "daily" -> parseDaily(tokens, nowLdt, zone)
            "weekly" -> parseWeekly(tokens, nowLdt, zone)
            "hourly" -> parseHourly(tokens, nowLdt, zone)
            else -> null
        }
    }

    private fun parseDaily(tokens: List<String>, nowLdt: LocalDateTime, zone: ZoneId): Long? {
        if (tokens.size < 2) return null
        val time = parseTime(tokens[1]) ?: return null
        // Heute zur Zielzeit; wenn schon vorbei: morgen.
        var target = nowLdt.toLocalDate().atTime(time)
        if (!target.isAfter(nowLdt)) {
            target = target.plusDays(1)
        }
        return target.atZone(zone).toInstant().toEpochMilli()
    }

    private fun parseWeekly(tokens: List<String>, nowLdt: LocalDateTime, zone: ZoneId): Long? {
        if (tokens.size < 3) return null
        val dow = parseDayOfWeek(tokens[1]) ?: return null
        val time = parseTime(tokens[2]) ?: return null

        // Naechster Tag mit dem passenden DayOfWeek + Time
        var target = nowLdt.toLocalDate().atTime(time)
        // Zur Ziel-DOW vorwaerts gehen
        while (target.dayOfWeek != dow || !target.isAfter(nowLdt)) {
            target = target.plusDays(1)
        }
        return target.atZone(zone).toInstant().toEpochMilli()
    }

    private fun parseHourly(tokens: List<String>, nowLdt: LocalDateTime, zone: ZoneId): Long? {
        if (tokens.size < 2) return null
        // Format ":MM" — fuehrender Doppelpunkt erlaubt aber optional
        val minutePart = tokens[1].removePrefix(":")
        val minute = minutePart.toIntOrNull()?.takeIf { it in 0..59 } ?: return null

        var target = nowLdt.withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(nowLdt)) target = target.plusHours(1)
        return target.atZone(zone).toInstant().toEpochMilli()
    }

    private fun parseTime(s: String): LocalTime? =
        runCatching { LocalTime.parse(s, TIME_FORMAT) }.getOrNull()

    private fun parseDayOfWeek(s: String): DayOfWeek? =
        when (s.uppercase()) {
            "MON" -> DayOfWeek.MONDAY
            "TUE" -> DayOfWeek.TUESDAY
            "WED" -> DayOfWeek.WEDNESDAY
            "THU" -> DayOfWeek.THURSDAY
            "FRI" -> DayOfWeek.FRIDAY
            "SAT" -> DayOfWeek.SATURDAY
            "SUN" -> DayOfWeek.SUNDAY
            else -> null
        }

    fun examples(): List<String> =
        listOf(
            "daily 19:00",
            "daily 7:30",
            "weekly SUN 19:00",
            "weekly MON 8:00",
            "hourly :05",
        )

    fun describe(cron: String): String {
        val tokens = cron.trim().split(" ").filter { it.isNotBlank() }
        if (tokens.isEmpty()) return "leerer Cron-Ausdruck"
        return when (tokens.getOrNull(0)?.lowercase()) {
            "daily" -> "jeden Tag um ${tokens.getOrNull(1) ?: "?"} Uhr"
            "weekly" -> "jeden ${dayName(tokens.getOrNull(1))} um ${tokens.getOrNull(2) ?: "?"} Uhr"
            "hourly" -> "jede Stunde zur Minute ${tokens.getOrNull(1) ?: "?"}"
            else -> "nicht erkannt"
        }
    }

    private fun dayName(token: String?): String =
        when (token?.uppercase()) {
            "MON" -> "Montag"
            "TUE" -> "Dienstag"
            "WED" -> "Mittwoch"
            "THU" -> "Donnerstag"
            "FRI" -> "Freitag"
            "SAT" -> "Samstag"
            "SUN" -> "Sonntag"
            else -> "???"
        }
}
