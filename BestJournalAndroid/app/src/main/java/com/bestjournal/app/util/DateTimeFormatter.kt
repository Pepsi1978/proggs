package com.bestjournal.app.util

import android.content.Context
import com.bestjournal.app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeFormatter {

    // Locale-aware date/time formats — get() ensures they pick up runtime locale changes
    private val fullFormat: SimpleDateFormat
        get() {
            val locale = DeviceLocale.locale
            val datePattern =
                android.text.format.DateFormat.getBestDateTimePattern(locale, "EEdMMMMyyyy")
            val timePattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "HHmm")
            return SimpleDateFormat("$datePattern · $timePattern", locale)
        }

    private val timeOnly: SimpleDateFormat
        get() {
            val locale = DeviceLocale.locale
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "HHmm")
            return SimpleDateFormat(pattern, locale)
        }

    private val dateOnly: SimpleDateFormat
        get() {
            val locale = DeviceLocale.locale
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "dMMMMyyyy")
            return SimpleDateFormat(pattern, locale)
        }

    private val monthYearFormat: SimpleDateFormat
        get() {
            val locale = DeviceLocale.locale
            val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "MMMMyyyy")
            return SimpleDateFormat(pattern, locale)
        }

    fun formatFull(timestamp: Long): String {
        return fullFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeOnly.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateOnly.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        val locale = DeviceLocale.locale
        return monthYearFormat.format(Date(timestamp)).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }

    fun formatQuarterYear(timestamp: Long): String {
        val locale = DeviceLocale.locale
        val date =
            java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        return date.format(java.time.format.DateTimeFormatter.ofPattern("QQQQ yyyy", locale))
    }

    fun formatRelative(context: Context, timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> context.getString(R.string.datetime_just_now)
            diff < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                context.resources.getQuantityString(
                    R.plurals.datetime_minutes_ago,
                    minutes.toInt(),
                    minutes.toInt(),
                )
            }
            diff < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                context.resources.getQuantityString(
                    R.plurals.datetime_hours_ago,
                    hours.toInt(),
                    hours.toInt(),
                )
            }
            days < 365L -> {
                context.resources.getQuantityString(
                    R.plurals.datetime_days_ago,
                    days.toInt(),
                    days.toInt(),
                )
            }
            days < 1825L -> {
                val months = (days / 30L).toInt()
                context.resources.getQuantityString(
                    R.plurals.datetime_months_relative,
                    months,
                    months,
                )
            }
            else -> {
                val years = (days / 365L).toInt()
                context.resources.getQuantityString(R.plurals.datetime_years_relative, years, years)
            }
        }
    }

    fun formatDuration(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.ROOT, "%02d:%02d", minutes, secs)
    }

    /**
     * Groups timestamps into section labels for the journal timeline. Hierarchy: Diese Woche >
     * Letzte Woche > Vor 2/3/4 Wochen > Monat/Jahr. Weeks use ISO convention: Monday = first day,
     * Sunday = last day. Week labels only apply within the current month — once entries cross into
     * a previous month, the month name is shown instead.
     */
    fun getSectionLabel(context: Context, timestamp: Long): String {
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
            timestamp >= thisWeekMonday.timeInMillis && sameMonth ->
                context.getString(R.string.datetime_this_week)
            timestamp >= lastWeekMonday.timeInMillis && sameMonth ->
                context.getString(R.string.datetime_last_week)
            timestamp >= twoWeeksAgo.timeInMillis && sameMonth ->
                context.getString(R.string.datetime_weeks_ago, 2)
            timestamp >= threeWeeksAgo.timeInMillis && sameMonth ->
                context.getString(R.string.datetime_weeks_ago, 3)
            timestamp >= fourWeeksAgo.timeInMillis && sameMonth ->
                context.getString(R.string.datetime_weeks_ago, 4)
            else -> formatMonthYear(timestamp)
        }
    }
}
