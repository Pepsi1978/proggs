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
class AiUsageTracker @Inject constructor(private val prefs: SharedPreferences) {
    companion object {
        private const val KEY_USAGE_DAYS = "ai_usage_days"
        private const val KEY_FIRST_USE_DATE = "ai_first_use_date"
        private const val KEY_WEEKLY_DASHBOARD_COUNT = "weekly_dashboard_count"
        private const val KEY_WEEKLY_TEXT_COUNT = "weekly_text_count"
        private const val KEY_WEEKLY_RESET_DATE = "ai_weekly_reset"
        private const val KEY_BANNER_LAST_SHOWN = "ai_banner_last_shown"
        const val TRIAL_DAYS = 8

        // Dashboard daily tracking (per scenario)
        private const val KEY_DASHBOARD_DAILY_COUNT = "dashboard_daily_count"
        private const val KEY_DASHBOARD_DAILY_DATE = "dashboard_daily_date"
        private const val KEY_DASHBOARD_COOLDOWN_UNTIL = "dashboard_cooldown_until"

        // Text improvement daily tracking
        private const val KEY_TEXT_DAILY_COUNT = "text_improvement_daily_count"
        private const val KEY_TEXT_DAILY_DATE = "text_improvement_daily_date"
        private const val KEY_TEXT_COOLDOWN_UNTIL = "text_improvement_cooldown_until"

        // Spam protection (hourly)
        private const val KEY_HOURLY_AI_COUNT = "hourly_ai_count"
        private const val KEY_HOURLY_AI_RESET = "hourly_ai_reset"
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ── Usage day tracking ──────────────────────────────

    fun recordUsageDay() {
        val today = LocalDate.now().format(dateFormatter)
        val days = getUsageDaysSet().toMutableSet()
        days.add(today)
        prefs.edit().putString(KEY_USAGE_DAYS, days.joinToString(",")).apply()
        // Store first-use date if not set yet (for calendar-day trial)
        if (prefs.getString(KEY_FIRST_USE_DATE, "").isNullOrBlank()) {
            prefs.edit().putString(KEY_FIRST_USE_DATE, today).apply()
        }
    }

    fun getUsageDayCount(): Int = getUsageDaysSet().size

    fun getCurrentPhase(): AiPhase {
        var firstUse = prefs.getString(KEY_FIRST_USE_DATE, "") ?: ""

        // Migration: existing users have usage days but no first-use date.
        // Derive first-use date from their earliest recorded usage day.
        if (firstUse.isBlank()) {
            val usageDays = getUsageDaysSet()
            if (usageDays.isNotEmpty()) {
                val earliest = usageDays.mapNotNull {
                    runCatching { LocalDate.parse(it, dateFormatter) }.getOrNull()
                }.minOrNull()
                if (earliest != null) {
                    firstUse = earliest.format(dateFormatter)
                    prefs.edit().putString(KEY_FIRST_USE_DATE, firstUse).apply()
                }
            }
        }

        if (firstUse.isBlank()) return AiPhase.TRIAL // Truly never used → still trial
        val firstDate = LocalDate.parse(firstUse, dateFormatter)
        val daysSinceFirst = java.time.temporal.ChronoUnit.DAYS.between(firstDate, LocalDate.now())
        return when {
            daysSinceFirst < TRIAL_DAYS -> AiPhase.TRIAL
            else -> AiPhase.FREEMIUM
        }
    }

    // ── Weekly limits for free users ────────────────────

    fun getWeeklyDashboardCount(): Int {
        resetWeeklyCounterIfNeeded()
        return prefs.getInt(KEY_WEEKLY_DASHBOARD_COUNT, 0)
    }

    fun getWeeklyTextCount(): Int {
        resetWeeklyCounterIfNeeded()
        return prefs.getInt(KEY_WEEKLY_TEXT_COUNT, 0)
    }

    fun recordWeeklyDashboardUse() {
        resetWeeklyCounterIfNeeded()
        val count = prefs.getInt(KEY_WEEKLY_DASHBOARD_COUNT, 0)
        prefs.edit().putInt(KEY_WEEKLY_DASHBOARD_COUNT, count + 1).apply()
    }

    fun recordWeeklyTextUse() {
        resetWeeklyCounterIfNeeded()
        val count = prefs.getInt(KEY_WEEKLY_TEXT_COUNT, 0)
        prefs.edit().putInt(KEY_WEEKLY_TEXT_COUNT, count + 1).apply()
    }

    fun getRemainingFreeDashboardUses(): Int {
        return (Constants.FREE_WEEKLY_DASHBOARD_LIMIT - getWeeklyDashboardCount()).coerceAtLeast(0)
    }

    fun getRemainingFreeTextUses(): Int {
        return (Constants.FREE_WEEKLY_TEXT_LIMIT - getWeeklyTextCount()).coerceAtLeast(0)
    }

    fun isFreeDashboardAllowed(): Boolean {
        return when (getCurrentPhase()) {
            AiPhase.TRIAL -> true
            AiPhase.FREEMIUM -> getWeeklyDashboardCount() < Constants.FREE_WEEKLY_DASHBOARD_LIMIT
        }
    }

    fun isFreeTextAllowed(): Boolean {
        return when (getCurrentPhase()) {
            AiPhase.TRIAL -> true
            AiPhase.FREEMIUM -> getWeeklyTextCount() < Constants.FREE_WEEKLY_TEXT_LIMIT
        }
    }

    // ── AI info banner ──────────────────────────────────

    fun shouldShowAiInfoBanner(): Boolean {
        if (getCurrentPhase() != AiPhase.TRIAL) return false
        val firstUse = prefs.getString(KEY_FIRST_USE_DATE, "") ?: ""
        if (firstUse.isBlank()) return false
        val daysSinceFirst = java.time.temporal.ChronoUnit.DAYS.between(
            LocalDate.parse(firstUse, dateFormatter), LocalDate.now()
        )
        if (daysSinceFirst < 5) return false // Only show on calendar days 5-8 of trial
        val today = LocalDate.now().format(dateFormatter)
        val lastShown = prefs.getString(KEY_BANNER_LAST_SHOWN, "") ?: ""
        return lastShown != today
    }

    fun markAiInfoBannerShown() {
        val today = LocalDate.now().format(dateFormatter)
        prefs.edit().putString(KEY_BANNER_LAST_SHOWN, today).apply()
    }

    // ── Dashboard daily limit (4-tier, per scenario) ──────────────────

    private var currentScenario: Int = 0

    fun setCurrentScenario(scenario: Int) {
        currentScenario = scenario
    }

    private fun dashboardCountKey() = "${KEY_DASHBOARD_DAILY_COUNT}_$currentScenario"
    private fun dashboardDateKey() = "${KEY_DASHBOARD_DAILY_DATE}_$currentScenario"
    private fun dashboardCooldownKey() = "${KEY_DASHBOARD_COOLDOWN_UNTIL}_$currentScenario"

    fun recordDashboardRefresh() {
        resetDashboardCounterIfNewDay()
        val count = prefs.getInt(dashboardCountKey(), 0)
        prefs.edit().putInt(dashboardCountKey(), count + 1).apply()
    }

    fun getDashboardDailyCount(): Int {
        resetDashboardCounterIfNewDay()
        return prefs.getInt(dashboardCountKey(), 0)
    }

    fun getDashboardAccessResult(
        premiumLimit: Int,
        cooldownAt: Int,
        hardLimit: Int,
    ): TieredAccessResult {
        resetDashboardCounterIfNewDay()
        val count = prefs.getInt(dashboardCountKey(), 0)
        return computeTieredResult(
            count,
            dashboardCooldownKey(),
            premiumLimit,
            cooldownAt,
            hardLimit,
        )
    }

    private fun resetDashboardCounterIfNewDay() {
        val today = LocalDate.now().format(dateFormatter)
        val savedDate = prefs.getString(dashboardDateKey(), "") ?: ""
        if (savedDate != today) {
            prefs
                .edit()
                .putInt(dashboardCountKey(), 0)
                .putString(dashboardDateKey(), today)
                .putLong(dashboardCooldownKey(), 0L)
                .putBoolean("${dashboardCooldownKey()}_served", false)
                .apply()
        }
    }

    // ── Text improvement daily limit (4-tier) ───────────

    fun recordTextImprovement() {
        resetTextCounterIfNewDay()
        val count = prefs.getInt(KEY_TEXT_DAILY_COUNT, 0)
        prefs.edit().putInt(KEY_TEXT_DAILY_COUNT, count + 1).apply()
    }

    fun getTextDailyCount(): Int {
        resetTextCounterIfNewDay()
        return prefs.getInt(KEY_TEXT_DAILY_COUNT, 0)
    }

    fun getTextAccessResult(
        premiumLimit: Int,
        cooldownAt: Int,
        hardLimit: Int,
    ): TieredAccessResult {
        resetTextCounterIfNewDay()
        val count = prefs.getInt(KEY_TEXT_DAILY_COUNT, 0)
        return computeTieredResult(
            count,
            KEY_TEXT_COOLDOWN_UNTIL,
            premiumLimit,
            cooldownAt,
            hardLimit,
        )
    }

    private fun resetTextCounterIfNewDay() {
        val today = LocalDate.now().format(dateFormatter)
        val savedDate = prefs.getString(KEY_TEXT_DAILY_DATE, "") ?: ""
        if (savedDate != today) {
            prefs
                .edit()
                .putInt(KEY_TEXT_DAILY_COUNT, 0)
                .putString(KEY_TEXT_DAILY_DATE, today)
                .putLong(KEY_TEXT_COOLDOWN_UNTIL, 0L)
                .putBoolean("${KEY_TEXT_COOLDOWN_UNTIL}_served", false)
                .apply()
        }
    }

    // ── Shared 4-tier logic ─────────────────────────────

    private fun computeTieredResult(
        count: Int,
        cooldownKey: String,
        premiumLimit: Int,
        cooldownAt: Int,
        hardLimit: Int,
    ): TieredAccessResult {
        // Check active cooldown first
        val cooldownUntil = prefs.getLong(cooldownKey, 0L)
        if (cooldownUntil > 0 && System.currentTimeMillis() < cooldownUntil) {
            val minutesLeft = ((cooldownUntil - System.currentTimeMillis()) / 60_000).toInt() + 1
            return TieredAccessResult.Cooldown(minutesLeft, count)
        }

        return when {
            count < premiumLimit -> TieredAccessResult.Allowed(FirebaseAiService.MODEL_FLASH)
            count < cooldownAt -> TieredAccessResult.Allowed(FirebaseAiService.MODEL_FLASH_LITE)
            count < hardLimit -> {
                // Post-premium tier: check if cooldown was already served
                val cooldownServed = prefs.getBoolean("${cooldownKey}_served", false)
                if (!cooldownServed && count >= cooldownAt) {
                    // First time hitting cooldown threshold: start 30-min cooldown
                    val cooldownMs = Constants.COOLDOWN_MINUTES * 60_000L
                    prefs
                        .edit()
                        .putLong(cooldownKey, System.currentTimeMillis() + cooldownMs)
                        .putBoolean("${cooldownKey}_served", true)
                        .apply()
                    TieredAccessResult.Cooldown(Constants.COOLDOWN_MINUTES, count)
                } else {
                    // Cooldown already served — extended tier with Lite
                    TieredAccessResult.Allowed(FirebaseAiService.MODEL_FLASH_LITE)
                }
            }
            else -> TieredAccessResult.HardLimitReached
        }
    }

    // ── Spam protection (hourly) ────────────────────────

    fun recordHourlyAiUsage() {
        resetHourlyCounterIfNeeded()
        val count = prefs.getInt(KEY_HOURLY_AI_COUNT, 0)
        prefs.edit().putInt(KEY_HOURLY_AI_COUNT, count + 1).apply()
    }

    fun isHourlySpamLimitReached(isPremium: Boolean = false): Boolean {
        resetHourlyCounterIfNeeded()
        val limit = if (isPremium) Constants.SPAM_HOURLY_AI_LIMIT_PREMIUM
            else Constants.SPAM_HOURLY_AI_LIMIT
        return prefs.getInt(KEY_HOURLY_AI_COUNT, 0) >= limit
    }

    private fun resetHourlyCounterIfNeeded() {
        val nextReset = prefs.getLong(KEY_HOURLY_AI_RESET, 0L)
        if (System.currentTimeMillis() >= nextReset) {
            prefs
                .edit()
                .putInt(KEY_HOURLY_AI_COUNT, 0)
                .putLong(KEY_HOURLY_AI_RESET, System.currentTimeMillis() + 3_600_000L)
                .apply()
        }
    }

    // ── Private helpers ─────────────────────────────────

    private fun getUsageDaysSet(): Set<String> {
        val raw = prefs.getString(KEY_USAGE_DAYS, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    private fun resetWeeklyCounterIfNeeded() {
        val nextReset = prefs.getString(KEY_WEEKLY_RESET_DATE, "") ?: ""
        val today = LocalDate.now()
        if (nextReset.isBlank() || today >= LocalDate.parse(nextReset, dateFormatter)) {
            val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            prefs
                .edit()
                .putInt(KEY_WEEKLY_DASHBOARD_COUNT, 0)
                .putInt(KEY_WEEKLY_TEXT_COUNT, 0)
                .putString(KEY_WEEKLY_RESET_DATE, nextMonday.format(dateFormatter))
                .apply()
        }
    }
}

enum class AiPhase {
    TRIAL,
    FREEMIUM,
}

sealed class TieredAccessResult {
    data class Allowed(val modelName: String) : TieredAccessResult()

    data class Cooldown(val minutesLeft: Int, val totalToday: Int) : TieredAccessResult()

    data object HardLimitReached : TieredAccessResult()
}
