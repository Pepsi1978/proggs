package com.bestjournal.app.util

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Handles Google In-App Review prompts at positive moments.
 * Conditions: 5+ entries, streak > 2, not shown in last 90 days.
 */
@Singleton
class InAppReviewHelper @Inject constructor(
    private val prefs: SharedPreferences,
    private val streakTracker: StreakTracker,
) {
    companion object {
        private const val TAG = "InAppReview"
    }

    /**
     * Shows the review dialog if all conditions are met.
     * Call after a successful entry save for maximum positive sentiment.
     *
     * @param activity The current Activity (needed by ReviewManager)
     * @param totalEntries Total number of journal entries in the database
     */
    suspend fun maybeShowReview(activity: Activity, totalEntries: Int) {
        val currentStreak = streakTracker.getCurrentStreak()
        val lastPromptDate = prefs.getLong(Constants.PREF_LAST_REVIEW_PROMPT_DATE, 0L)
        val daysSinceLastPrompt = if (lastPromptDate > 0L) {
            (System.currentTimeMillis() - lastPromptDate) / (1000 * 60 * 60 * 24)
        } else {
            Long.MAX_VALUE
        }

        val hasEnoughEntries = totalEntries >= Constants.REVIEW_MIN_ENTRIES
        val hasStreak = currentStreak > Constants.REVIEW_MIN_STREAK
        val cooldownPassed = daysSinceLastPrompt >= Constants.REVIEW_COOLDOWN_DAYS

        Log.d(TAG, "Conditions: entries=$totalEntries (need ${Constants.REVIEW_MIN_ENTRIES}), " +
            "streak=$currentStreak (need >${Constants.REVIEW_MIN_STREAK}), " +
            "daysSince=$daysSinceLastPrompt (need ${Constants.REVIEW_COOLDOWN_DAYS})")

        if (!hasEnoughEntries || !hasStreak || !cooldownPassed) {
            Log.d(TAG, "Conditions not met — skipping review prompt")
            return
        }

        Log.d(TAG, "Event: review_prompt_conditions_met")

        try {
            val reviewManager = ReviewManagerFactory.create(activity)
            val reviewInfo = reviewManager.requestReviewFlow().await()
            reviewManager.launchReviewFlow(activity, reviewInfo).await()

            prefs.edit()
                .putLong(Constants.PREF_LAST_REVIEW_PROMPT_DATE, System.currentTimeMillis())
                .apply()

            Log.d(TAG, "Event: review_flow_launched")
        } catch (e: Exception) {
            Log.e(TAG, "Review flow failed: ${e.message}")
        }
    }
}
