package com.entropyjournal.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeFormatter {

    private val fullFormat = SimpleDateFormat("EE, d. MMMM yyyy · HH:mm 'Uhr'", Locale.GERMAN)
    private val timeOnly = SimpleDateFormat("HH:mm", Locale.GERMAN)
    private val dateOnly = SimpleDateFormat("d. MMMM yyyy", Locale.GERMAN)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.GERMAN)
    private val monthOnlyFormat = SimpleDateFormat("MMMM", Locale.GERMAN)

    fun formatFull(timestamp: Long): String {
        return fullFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeOnly.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateOnly.format(Date(timestamp))
    }

    fun formatRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "gerade eben"
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                "vor $minutes ${if (minutes == 1L) "Minute" else "Minuten"}"
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                "vor $hours ${if (hours == 1L) "Stunde" else "Stunden"}"
            }
            days < 365L -> "vor $days ${if (days == 1L) "Tag" else "Tagen"}"
            days < 1825L -> {
                val months = days / 30L
                "vor $months ${if (months == 1L) "Monat" else "Monaten"}"
            }
            else -> {
                val years = days / 365L
                "vor $years ${if (years == 1L) "Jahr" else "Jahren"}"
            }
        }
    }

    fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.GERMAN, "%02d:%02d", minutes, secs)
    }

    /**
     * Groups timestamps into section labels for the journal timeline. Hierarchy: Diese Woche >
     * Letzte Woche > Vor 2/3/4 Wochen > Monatsname > Jahr — Monatsname. Weeks use ISO convention:
     * Monday = first day, Sunday = last day. Week labels only apply within the current month — once
     * entries cross into a previous month, the month name is shown instead.
     */
    fun getSectionLabel(timestamp: Long): String {
        val now = Calendar.getInstance()
        val entry = Calendar.getInstance().apply { timeInMillis = timestamp }

        val todayStart =
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

        // Monday of current ISO week (Mon=first day, Sun=last day)
        val dow = todayStart.get(Calendar.DAY_OF_WEEK)
        val daysSinceMonday = if (dow == Calendar.SUNDAY) 6 else dow - Calendar.MONDAY
        val thisWeekMonday =
            (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -daysSinceMonday) }
        val lastWeekMonday =
            (thisWeekMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
        val twoWeeksAgo =
            (thisWeekMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -14) }
        val threeWeeksAgo =
            (thisWeekMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -21) }
        val fourWeeksAgo =
            (thisWeekMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -28) }

        // Week labels only apply within the current month
        val sameMonth =
            entry.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                entry.get(Calendar.YEAR) == now.get(Calendar.YEAR)

        return when {
            timestamp >= thisWeekMonday.timeInMillis && sameMonth -> "Diese Woche"
            timestamp >= lastWeekMonday.timeInMillis && sameMonth -> "Letzte Woche"
            timestamp >= twoWeeksAgo.timeInMillis && sameMonth -> "Vor 2 Wochen"
            timestamp >= threeWeeksAgo.timeInMillis && sameMonth -> "Vor 3 Wochen"
            timestamp >= fourWeeksAgo.timeInMillis && sameMonth -> "Vor 4 Wochen"
            entry.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
                monthYearFormat.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
            }
            else -> {
                val month =
                    monthOnlyFormat.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
                "${entry.get(Calendar.YEAR)} \u2014 $month"
            }
        }
    }
}
