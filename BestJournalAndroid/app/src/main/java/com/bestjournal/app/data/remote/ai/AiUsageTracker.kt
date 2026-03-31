package com.bestjournal.app.data.remote.ai

import android.content.SharedPreferences
import com.bestjournal.app.util.Constants
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
        private const val KEY_DASHBOARD_DAILY_COUNT = "dashboard_daily_count"
        private const val KEY_DASHBOARD_DAILY_DATE = "dashboard_daily_date"
        private const val KEY_DASHBOARD_COOLDOWN_UNTIL = "dashboard_cooldown_until"
        private const val KEY_HOURLY_AI_COUNT = "hourly_ai_count"
        private const val KEY_HOURLY_AI_RESET = "hourly_ai_reset"
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

    // ── Dashboard daily limit ──────────────────────────────

    fun recordDashboardRefresh() {
        resetDashboardCounterIfNewDay()
        val count = prefs.getInt(KEY_DASHBOARD_DAILY_COUNT, 0)
        prefs.edit().putInt(KEY_DASHBOARD_DAILY_COUNT, count + 1).apply()
    }

    fun getDashboardDailyCount(): Int {
        resetDashboardCounterIfNewDay()
        return prefs.getInt(KEY_DASHBOARD_DAILY_COUNT, 0)
    }

    fun getDashboardAccessResult(): DashboardAccessResult {
        resetDashboardCounterIfNewDay()
        val count = prefs.getInt(KEY_DASHBOARD_DAILY_COUNT, 0)

        // Check cooldown first
        val cooldownUntil = prefs.getLong(KEY_DASHBOARD_COOLDOWN_UNTIL, 0L)
        if (cooldownUntil > 0 && System.currentTimeMillis() < cooldownUntil) {
            val minutesLeft = ((cooldownUntil - System.currentTimeMillis()) / 60_000).toInt() + 1
            return DashboardAccessResult.Cooldown(minutesLeft)
        }

        return when {
            count < Constants.DASHBOARD_SILENT_LIMIT -> DashboardAccessResult.Allowed
            count < Constants.DASHBOARD_COOLDOWN_LIMIT -> {
                // Set cooldown timer
                val cooldownMs = Constants.DASHBOARD_COOLDOWN_MINUTES * 60_000L
                prefs.edit().putLong(KEY_DASHBOARD_COOLDOWN_UNTIL, System.currentTimeMillis() + cooldownMs).apply()
                DashboardAccessResult.Cooldown(Constants.DASHBOARD_COOLDOWN_MINUTES)
            }
            else -> DashboardAccessResult.DailyLimitReached
        }
    }

    private fun resetDashboardCounterIfNewDay() {
        val today = LocalDate.now().format(dateFormatter)
        val savedDate = prefs.getString(KEY_DASHBOARD_DAILY_DATE, "") ?: ""
        if (savedDate != today) {
            prefs.edit()
                .putInt(KEY_DASHBOARD_DAILY_COUNT, 0)
                .putString(KEY_DASHBOARD_DAILY_DATE, today)
                .putLong(KEY_DASHBOARD_COOLDOWN_UNTIL, 0L)
                .apply()
        }
    }

    // ── Spam protection (hourly) ───────────────────────────

    fun recordHourlyAiUsage() {
        resetHourlyCounterIfNeeded()
        val count = prefs.getInt(KEY_HOURLY_AI_COUNT, 0)
        prefs.edit().putInt(KEY_HOURLY_AI_COUNT, count + 1).apply()
    }

    fun isHourlySpamLimitReached(): Boolean {
        resetHourlyCounterIfNeeded()
        return prefs.getInt(KEY_HOURLY_AI_COUNT, 0) >= Constants.SPAM_HOURLY_AI_LIMIT
    }

    private fun resetHourlyCounterIfNeeded() {
        val nextReset = prefs.getLong(KEY_HOURLY_AI_RESET, 0L)
        if (System.currentTimeMillis() >= nextReset) {
            prefs.edit()
                .putInt(KEY_HOURLY_AI_COUNT, 0)
                .putLong(KEY_HOURLY_AI_RESET, System.currentTimeMillis() + 3_600_000L)
                .apply()
        }
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

sealed class DashboardAccessResult {
    data object Allowed : DashboardAccessResult()
    data class Cooldown(val minutesLeft: Int) : DashboardAccessResult()
    data object DailyLimitReached : DashboardAccessResult()
}
