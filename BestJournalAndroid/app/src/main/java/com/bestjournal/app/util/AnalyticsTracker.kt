package com.bestjournal.app.util

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized analytics tracker for all Firebase Analytics events.
 * Injected as a Singleton via Hilt into ViewModels and utility classes.
 */
@Singleton
class AnalyticsTracker @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) {

    // ── Onboarding ──────────────────────────────────────────────────────

    fun trackOnboardingStarted() {
        firebaseAnalytics.logEvent("onboarding_started", null)
    }

    fun trackOnboardingCompleted() {
        firebaseAnalytics.logEvent("onboarding_completed", null)
    }

    fun trackOnboardingSkipped(screenIndex: Int) {
        firebaseAnalytics.logEvent("onboarding_skipped", Bundle().apply {
            putInt("screen_index", screenIndex)
        })
    }

    fun trackOnboardingScreenViewed(screenIndex: Int) {
        firebaseAnalytics.logEvent("onboarding_screen_viewed", Bundle().apply {
            putInt("screen_index", screenIndex)
        })
    }

    fun trackOnboardingGoalsSelected(goals: String) {
        firebaseAnalytics.logEvent("onboarding_goals_selected", Bundle().apply {
            putString("goals", goals.take(100))
        })
    }

    fun trackTrialStartedOnboarding() {
        firebaseAnalytics.logEvent("trial_started_onboarding", null)
    }

    // ── Entry Events ────────────────────────────────────────────────────

    fun trackEntryCreated(source: String) {
        firebaseAnalytics.logEvent("entry_created", Bundle().apply {
            putString("source", source)
        })
    }

    fun trackEntryImproved() {
        firebaseAnalytics.logEvent("entry_improved", null)
    }

    fun trackEntryDeleted() {
        firebaseAnalytics.logEvent("entry_deleted", null)
    }

    // ── Dashboard Events ────────────────────────────────────────────────

    fun trackDashboardViewed(scenario: Int) {
        firebaseAnalytics.logEvent("dashboard_viewed", Bundle().apply {
            putInt("scenario", scenario)
        })
    }

    fun trackDashboardRefreshed(scenario: Int) {
        firebaseAnalytics.logEvent("dashboard_refreshed", Bundle().apply {
            putInt("scenario", scenario)
        })
    }

    fun trackProfileSwitched(fromScenario: Int, toScenario: Int) {
        firebaseAnalytics.logEvent("profile_switched", Bundle().apply {
            putInt("from_scenario", fromScenario)
            putInt("to_scenario", toScenario)
        })
    }

    // ── Paywall Events ──────────────────────────────────────────────────

    fun trackPaywallShown(source: String) {
        firebaseAnalytics.logEvent("paywall_shown", Bundle().apply {
            putString("source", source)
        })
    }

    fun trackPaywallDismissed() {
        firebaseAnalytics.logEvent("paywall_dismissed", null)
    }

    fun trackTrialCtaClicked() {
        firebaseAnalytics.logEvent("trial_cta_clicked", null)
    }

    fun trackMonthlyCtaClicked() {
        firebaseAnalytics.logEvent("monthly_cta_clicked", null)
    }

    fun trackLifetimeCtaClicked() {
        firebaseAnalytics.logEvent("lifetime_cta_clicked", null)
    }

    fun trackYearlyCtaClicked() {
        firebaseAnalytics.logEvent("yearly_cta_clicked", null)
    }

    fun trackNoThanksClicked() {
        firebaseAnalytics.logEvent("no_thanks_clicked", null)
    }

    fun trackSubscriptionPurchased(type: String, value: Double = 0.0, currency: String = "EUR") {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.PURCHASE, Bundle().apply {
            putString("subscription_type", type)
            putDouble(FirebaseAnalytics.Param.VALUE, value)
            putString(FirebaseAnalytics.Param.CURRENCY, currency)
        })
    }

    fun trackPremiumBenefitsViewed() {
        firebaseAnalytics.logEvent("premium_benefits_viewed", null)
    }

    // ── Retention Events ────────────────────────────────────────────────

    fun trackStreakUpdated(length: Int) {
        firebaseAnalytics.logEvent("streak_updated", Bundle().apply {
            putInt("streak_length", length)
        })
    }

    fun trackStreakBroken() {
        firebaseAnalytics.logEvent("streak_broken", null)
    }

    fun trackStreakMilestone(days: Int) {
        firebaseAnalytics.logEvent("streak_milestone", Bundle().apply {
            putInt("days", days)
        })
    }

    fun trackReminderEnabled() {
        firebaseAnalytics.logEvent("reminder_enabled", null)
    }

    fun trackReminderDisabled() {
        firebaseAnalytics.logEvent("reminder_disabled", null)
    }

    fun trackReminderTimeChanged(hour: Int) {
        firebaseAnalytics.logEvent("reminder_time_changed", Bundle().apply {
            putInt("hour", hour)
        })
    }

    fun trackReminderOpened() {
        firebaseAnalytics.logEvent("reminder_opened", null)
    }

    fun trackReminderNotificationShown() {
        firebaseAnalytics.logEvent("reminder_notification_shown", null)
    }

    // ── Review Events ─────────────────────────────────────────────────

    fun trackReviewPromptConditionsMet() {
        firebaseAnalytics.logEvent("review_prompt_conditions_met", null)
    }

    fun trackReviewFlowLaunched() {
        firebaseAnalytics.logEvent("review_flow_launched", null)
    }

    // ── Upsell Events ───────────────────────────────────────────────────

    fun trackUpsellBannerShown(source: String) {
        firebaseAnalytics.logEvent("upsell_banner_shown", Bundle().apply {
            putString("source", source)
        })
    }

    fun trackUpsellBannerClicked(source: String) {
        firebaseAnalytics.logEvent("upsell_banner_clicked", Bundle().apply {
            putString("source", source)
        })
    }
}
