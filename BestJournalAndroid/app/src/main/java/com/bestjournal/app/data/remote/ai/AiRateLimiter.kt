package com.bestjournal.app.data.remote.ai

import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRateLimiter @Inject constructor(
    private val usageTracker: AiUsageTracker
) {
    fun checkAccess(subscriptionState: SubscriptionState): AiAccessResult {
        if (subscriptionState is SubscriptionState.Subscribed) {
            return AiAccessResult.Allowed
        }
        val phase = usageTracker.getCurrentPhase()
        return when (phase) {
            AiPhase.HONEYMOON, AiPhase.EDUCATION -> AiAccessResult.Allowed
            AiPhase.FREEMIUM -> {
                if (usageTracker.isAiAllowedForFreeUser()) {
                    AiAccessResult.Allowed
                } else {
                    AiAccessResult.LimitReached(
                        weeklyUsed = usageTracker.getWeeklyAiUsageCount(),
                        weeklyLimit = AiUsageTracker.FREE_WEEKLY_LIMIT
                    )
                }
            }
        }
    }

    fun getMaxEntriesForAnalysis(subscriptionState: SubscriptionState): Int {
        if (subscriptionState is SubscriptionState.Subscribed) return Constants.MAX_ENTRIES_SUBSCRIBED_ANALYSIS
        val phase = usageTracker.getCurrentPhase()
        return when (phase) {
            AiPhase.HONEYMOON, AiPhase.EDUCATION -> Constants.MAX_ENTRIES_TRIAL_ANALYSIS
            AiPhase.FREEMIUM -> Constants.MAX_ENTRIES_FREE_ANALYSIS
        }
    }

    fun checkDashboardAccess(): DashboardAccessResult {
        return usageTracker.getDashboardAccessResult()
    }

    fun recordDashboardRefresh() {
        usageTracker.recordDashboardRefresh()
        usageTracker.recordHourlyAiUsage()
    }
}

sealed class AiAccessResult {
    data object Allowed : AiAccessResult()
    data class LimitReached(val weeklyUsed: Int, val weeklyLimit: Int) : AiAccessResult()
}
