package com.entropyjournal.util

import android.content.SharedPreferences
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakTracker @Inject constructor(
    private val prefs: SharedPreferences,
) {
    companion object {
        private const val KEY_CURRENT = "streak_current"
        private const val KEY_LONGEST = "streak_longest"
        private const val KEY_LAST_DATE = "streak_last_date"
    }

    fun recordEntry() {
        val today = LocalDate.now()
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null)
        val lastDate = lastDateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        if (lastDate == today) return

        val previousStreak = prefs.getInt(KEY_CURRENT, 0)
        val yesterday = today.minusDays(1)

        val newStreak = if (lastDate == yesterday) {
            previousStreak + 1
        } else {
            1
        }

        val longest = maxOf(prefs.getInt(KEY_LONGEST, 0), newStreak)

        prefs.edit()
            .putInt(KEY_CURRENT, newStreak)
            .putInt(KEY_LONGEST, longest)
            .putString(KEY_LAST_DATE, today.toString())
            .apply()
    }

    fun getCurrentStreak(): Int {
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null) ?: return 0
        val lastDate = runCatching { LocalDate.parse(lastDateStr) }.getOrNull() ?: return 0
        val today = LocalDate.now()
        return if (lastDate == today || lastDate == today.minusDays(1)) {
            prefs.getInt(KEY_CURRENT, 0)
        } else {
            0
        }
    }

    fun getLongestStreak(): Int = prefs.getInt(KEY_LONGEST, 0)
}
