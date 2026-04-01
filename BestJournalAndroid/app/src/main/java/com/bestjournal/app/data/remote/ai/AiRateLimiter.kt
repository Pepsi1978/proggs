package com.bestjournal.app.data.remote.ai

import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRateLimiter @Inject constructor(private val usageTracker: AiUsageTracker) {
    fun checkDashboardAccess(subscriptionState: SubscriptionState): TieredAccessResult {
        // Spam protection applies to ALL users including subscribers
        if (usageTracker.isHourlySpamLimitReached()) {
            return TieredAccessResult.Cooldown(
                minutesLeft = 5,
                totalToday = usageTracker.getDashboardDailyCount(),
            )
        }
        val phase = usageTracker.getCurrentPhase()
        return when {
            subscriptionState is SubscriptionState.Subscribed ->
                usageTracker.getDashboardAccessResult(
                    premiumLimit = Constants.SUB_PREMIUM_LIMIT,
                    cooldownAt = Constants.SUB_COOLDOWN_AT,
                    hardLimit = Constants.SUB_HARD_LIMIT,
                )
            phase == AiPhase.TRIAL ->
                usageTracker.getDashboardAccessResult(
                    premiumLimit = Constants.TRIAL_PREMIUM_LIMIT,
                    cooldownAt = Constants.TRIAL_COOLDOWN_AT,
                    hardLimit = Constants.TRIAL_HARD_LIMIT,
                )
            else -> {
                // FREEMIUM: weekly limit, always Lite
                if (usageTracker.isFreeDashboardAllowed()) {
                    TieredAccessResult.Allowed(FirebaseAiService.MODEL_FLASH_LITE)
                } else {
                    TieredAccessResult.HardLimitReached
                }
            }
        }
    }

    fun checkTextAccess(subscriptionState: SubscriptionState): TieredAccessResult {
        // Spam protection applies to ALL users including subscribers
        if (usageTracker.isHourlySpamLimitReached()) {
            return TieredAccessResult.Cooldown(
                minutesLeft = 5,
                totalToday = usageTracker.getTextDailyCount(),
            )
        }
        val phase = usageTracker.getCurrentPhase()
        return when {
            subscriptionState is SubscriptionState.Subscribed ->
                usageTracker.getTextAccessResult(
                    premiumLimit = Constants.SUB_PREMIUM_LIMIT,
                    cooldownAt = Constants.SUB_COOLDOWN_AT,
                    hardLimit = Constants.SUB_HARD_LIMIT,
                )
            phase == AiPhase.TRIAL ->
                usageTracker.getTextAccessResult(
                    premiumLimit = Constants.TRIAL_PREMIUM_LIMIT,
                    cooldownAt = Constants.TRIAL_COOLDOWN_AT,
                    hardLimit = Constants.TRIAL_HARD_LIMIT,
                )
            else -> {
                // FREEMIUM: weekly limit, always Lite
                if (usageTracker.isFreeTextAllowed()) {
                    TieredAccessResult.Allowed(FirebaseAiService.MODEL_FLASH_LITE)
                } else {
                    TieredAccessResult.HardLimitReached
                }
            }
        }
    }

    fun getMaxEntriesForAnalysis(subscriptionState: SubscriptionState): Int {
        if (subscriptionState is SubscriptionState.Subscribed)
            return Constants.MAX_ENTRIES_SUBSCRIBED_ANALYSIS
        return when (usageTracker.getCurrentPhase()) {
            AiPhase.TRIAL -> Constants.MAX_ENTRIES_TRIAL_ANALYSIS
            AiPhase.FREEMIUM -> Constants.MAX_ENTRIES_FREE_ANALYSIS
        }
    }

    fun recordDashboardRefresh() {
        usageTracker.recordDashboardRefresh()
        usageTracker.recordHourlyAiUsage()
    }

    fun recordTextImprovement() {
        usageTracker.recordTextImprovement()
        usageTracker.recordHourlyAiUsage()
    }
}
