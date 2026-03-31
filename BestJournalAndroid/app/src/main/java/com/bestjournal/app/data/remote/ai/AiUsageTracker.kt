package com.bestjournal.app.data.remote.ai

import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiUsageTracker @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_USAGE_DAYS = "ai_usage_days"
        private const val KEY_WEEKLY_AI_COUNT = "ai_weekly_count"
        private const val KEY_WEEKLY_RESET_DATE = "ai_weekly_reset"
        private const val KEY_BANNER_LAST_SHOWN = "ai_banner_last_shown"
        const val TRIAL_DAYS = 7
        const val FREE_WEEKLY_LIMIT = 3
        const val HONEYMOON_DAYS = 3
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun recordUsageDay() {
        val today = LocalDate.now().format(dateFormatter)
        val days = getUsageDaysSet().toMutableSet()
        days.add(today)
        prefs.edit().putString(KEY_USAGE_DAYS, days.joinToString(",")).apply()
    }

    fun recordAiUsage() {
        resetWeeklyCounterIfNeeded()
        val count = prefs.getInt(KEY_WEEKLY_AI_COUNT, 0)
        prefs.edit().putInt(KEY_WEEKLY_AI_COUNT, count + 1).apply()
    }

    fun getUsageDayCount(): Int = getUsageDaysSet().size

    fun getCurrentPhase(): AiPhase {
        val days = getUsageDayCount()
        return when {
            days <= HONEYMOON_DAYS -> AiPhase.HONEYMOON
            days <= TRIAL_DAYS -> AiPhase.EDUCATION
            else -> AiPhase.FREEMIUM
        }
    }

    fun getWeeklyAiUsageCount(): Int {
        resetWeeklyCounterIfNeeded()
        return prefs.getInt(KEY_WEEKLY_AI_COUNT, 0)
    }

    fun getRemainingFreeUses(): Int {
        return (FREE_WEEKLY_LIMIT - getWeeklyAiUsageCount()).coerceAtLeast(0)
    }

    fun isAiAllowedForFreeUser(): Boolean {
        val phase = getCurrentPhase()
        return when (phase) {
            AiPhase.HONEYMOON, AiPhase.EDUCATION -> true
            AiPhase.FREEMIUM -> getWeeklyAiUsageCount() < FREE_WEEKLY_LIMIT
        }
    }

    fun shouldShowAiInfoBanner(): Boolean {
        if (getCurrentPhase() != AiPhase.EDUCATION) return false
        val today = LocalDate.now().format(dateFormatter)
        val lastShown = prefs.getString(KEY_BANNER_LAST_SHOWN, "") ?: ""
        return lastShown != today
    }

    fun markAiInfoBannerShown() {
        val today = LocalDate.now().format(dateFormatter)
        prefs.edit().putString(KEY_BANNER_LAST_SHOWN, today).apply()
    }

    private fun getUsageDaysSet(): Set<String> {
        val raw = prefs.getString(KEY_USAGE_DAYS, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    private fun resetWeeklyCounterIfNeeded() {
        val nextReset = prefs.getString(KEY_WEEKLY_RESET_DATE, "") ?: ""
        val today = LocalDate.now()
        if (nextReset.isBlank() || today >= LocalDate.parse(nextReset, dateFormatter)) {
            val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            prefs.edit()
                .putInt(KEY_WEEKLY_AI_COUNT, 0)
                .putString(KEY_WEEKLY_RESET_DATE, nextMonday.format(dateFormatter))
                .apply()
        }
    }
}

enum class AiPhase {
    HONEYMOON,
    EDUCATION,
    FREEMIUM
}
