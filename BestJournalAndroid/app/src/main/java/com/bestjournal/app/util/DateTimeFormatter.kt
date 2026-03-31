package com.bestjournal.app.util

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
            diff < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diff)
                "vor $days ${if (days == 1L) "Tag" else "Tagen"}"
            }
            else -> formatDate(timestamp)
        }
    }

    fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.GERMAN, "%02d:%02d", minutes, secs)
    }

    /**
     * Groups timestamps into section labels for the journal timeline.
     * Returns: "Heute", "Gestern", "Letzte Woche", "März 2026", "Februar 2026", etc.
     * For years before current: "2025 — Februar"
     */
    fun getSectionLabel(timestamp: Long): String {
        val now = Calendar.getInstance()
        val entry = Calendar.getInstance().apply { timeInMillis = timestamp }

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val yesterdayStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val weekStart = (todayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }

        return when {
            timestamp >= todayStart.timeInMillis -> "Heute"
            timestamp >= yesterdayStart.timeInMillis -> "Gestern"
            timestamp >= weekStart.timeInMillis -> "Letzte Woche"
            entry.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
                // Same year: just month name + year
                monthYearFormat.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
            }
            else -> {
                // Different year: "2025 — Februar"
                val month = monthOnlyFormat.format(Date(timestamp)).replaceFirstChar { it.uppercase() }
                "${entry.get(Calendar.YEAR)} \u2014 $month"
            }
        }
    }
}
