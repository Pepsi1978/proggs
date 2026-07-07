package com.nems.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object DateUtils {
    val isoDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val isoDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun getMonthDays(yearMonth: YearMonth): List<LocalDate?> {
        val firstOfMonth = yearMonth.atDay(1)
        val daysInMonth = yearMonth.lengthOfMonth()
        // Monday = 1, Sunday = 7 (ISO)
        val firstDayOfWeek = firstOfMonth.dayOfWeek.value // 1=Mon..7=Sun
        val leadingNulls = firstDayOfWeek - 1 // days before first of month (Monday start)

        val days = mutableListOf<LocalDate?>()
        repeat(leadingNulls) { days.add(null) }
        for (day in 1..daysInMonth) {
            days.add(yearMonth.atDay(day))
        }
        // Pad trailing nulls to complete the last week
        while (days.size % 7 != 0) {
            days.add(null)
        }
        return days
    }

    fun formatMonthYear(yearMonth: YearMonth): String {
        val month = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
        return "$month ${yearMonth.year}"
    }

    fun formatDayHeader(date: LocalDate): String {
        val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.GERMAN)
        val day = date.dayOfMonth
        val month = date.month.getDisplayName(TextStyle.FULL, Locale.GERMAN)
        val year = date.year
        return "$dayName, $day. $month $year"
    }

    fun weekDayHeaders(): List<String> {
        return DayOfWeek.entries.map {
            it.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
        }
    }
}
