package com.bestjournal.app.util

import android.content.SharedPreferences
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks daily journal entry streaks using SharedPreferences.
 * A streak increments when the user writes at least one entry per consecutive day.
 */
@Singleton
class StreakTracker @Inject constructor(
    private val prefs: SharedPreferences,
    private val analyticsTracker: AnalyticsTracker,
) {
    companion object {
        private const val KEY_CURRENT = "streak_current"
        private const val KEY_LONGEST = "streak_longest"
        private const val KEY_LAST_DATE = "streak_last_date"

        private val MILESTONES = setOf(7, 14, 30, 60, 90, 180, 365)
    }

    /**
     * Call after a journal entry is saved.
     * Increments streak if today is new, resets to 1 if a day was skipped.
     */
    fun recordEntry() {
        val today = LocalDate.now()
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null)
        val lastDate = lastDateStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        if (lastDate == today) {
            return
        }

        val previousStreak = prefs.getInt(KEY_CURRENT, 0)
        val yesterday = today.minusDays(1)

        val newStreak = if (lastDate == yesterday) {
            previousStreak + 1
        } else {
            if (previousStreak > 1 && lastDate != null) {
                analyticsTracker.trackStreakBroken()
            }
            1
        }

        val longest = maxOf(prefs.getInt(KEY_LONGEST, 0), newStreak)

        prefs.edit()
            .putInt(KEY_CURRENT, newStreak)
            .putInt(KEY_LONGEST, longest)
            .putString(KEY_LAST_DATE, today.toString())
            .apply()

        analyticsTracker.trackStreakUpdated(newStreak)

        if (newStreak in MILESTONES) {
            analyticsTracker.trackStreakMilestone(newStreak)
        }
    }

    fun getCurrentStreak(): Int {
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null)
            ?: return 0
        val lastDate = runCatching { LocalDate.parse(lastDateStr) }.getOrNull()
            ?: return 0
        val today = LocalDate.now()

        return if (lastDate == today || lastDate == today.minusDays(1)) {
            prefs.getInt(KEY_CURRENT, 0)
        } else {
            0
        }
    }

    fun getLongestStreak(): Int = prefs.getInt(KEY_LONGEST, 0)
}
