package com.bestjournal.app.data.remote.ai

import com.bestjournal.app.billing.SubscriptionState
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
        if (subscriptionState is SubscriptionState.Subscribed) return 30
        val phase = usageTracker.getCurrentPhase()
        return when (phase) {
            AiPhase.HONEYMOON, AiPhase.EDUCATION -> Int.MAX_VALUE
            AiPhase.FREEMIUM -> 10
        }
    }
}

sealed class AiAccessResult {
    data object Allowed : AiAccessResult()
    data class LimitReached(val weeklyUsed: Int, val weeklyLimit: Int) : AiAccessResult()
}
