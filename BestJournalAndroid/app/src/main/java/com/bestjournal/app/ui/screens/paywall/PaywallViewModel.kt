package com.bestjournal.app.ui.screens.paywall

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.R
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PaywallViewModel
@Inject
constructor(
    private val billingManager: BillingManager,
    val analyticsTracker: AnalyticsTracker,
    private val prefs: SharedPreferences,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val source: String = savedStateHandle["source"] ?: "limit_reached"
    val monthlyPrice: StateFlow<String> = billingManager.monthlyPrice
    val yearlyPrice: StateFlow<String> = billingManager.yearlyPrice
    val lifetimePrice: StateFlow<String> = billingManager.lifetimePrice

    /** Personalized headline + subtitle based on onboarding goals. */
    val personalizedHeadline: StateFlow<Pair<String, String>>

    /** The matched goal type for analytics ("stress", "klarheit", etc. or "default"). */
    val matchedGoalType: String

    init {
        val goalsRaw = prefs.getString(Constants.PREF_ONBOARDING_GOALS, "") ?: ""
        val firstGoal = goalsRaw.split(",").firstOrNull()?.trim() ?: ""

        // Match new language-neutral keys first, then fallback for legacy German strings
        val goalType = when {
            firstGoal == "stress" || firstGoal.contains("Stress", ignoreCase = true) -> "stress"
            firstGoal == "clarity" || firstGoal.contains("Klarheit", ignoreCase = true) -> "clarity"
            firstGoal == "growth" || firstGoal.contains("Wachstum", ignoreCase = true) -> "growth"
            firstGoal == "thoughts" || firstGoal.contains("Gedanken", ignoreCase = true) -> "thoughts"
            else -> "default"
        }
        val (headline, subtitle) = when (goalType) {
            "stress" -> context.getString(R.string.paywall_headline_stress) to
                context.getString(R.string.paywall_headline_stress_sub)
            "clarity" -> context.getString(R.string.paywall_headline_clarity) to
                context.getString(R.string.paywall_headline_clarity_sub)
            "growth" -> context.getString(R.string.paywall_headline_growth) to
                context.getString(R.string.paywall_headline_growth_sub)
            "thoughts" -> context.getString(R.string.paywall_headline_thoughts) to
                context.getString(R.string.paywall_headline_thoughts_sub)
            else -> context.getString(R.string.paywall_headline_default) to
                context.getString(R.string.paywall_headline_default_sub)
        }

        personalizedHeadline = MutableStateFlow(headline to subtitle).asStateFlow()
        matchedGoalType = goalType
    }

    /** Whether a real promotional offer exists in Google Play for the monthly plan. */
    val hasPromoOffer: Boolean
        get() = billingManager.getMonthlyPromoOfferToken() != null

    /**
     * Loop-7 (Frank, 2026-04-30): expose the live subscription state so the
     * Paywall composable can auto-dismiss the moment Google confirms the
     * purchase. Without this the user stays on the upsell screen, has to
     * tap the close button manually, and then sees the offer again before
     * the Settings screen reflects the new Premium state.
     */
    val subscriptionState: StateFlow<SubscriptionState> = billingManager.subscriptionState

    // Track whether exit-intent purchase flow was started — trial extension
    // is only granted AFTER Google confirms the purchase, not on flow start.
    private var exitIntentPending = false

    init {
        viewModelScope.launch {
            billingManager.subscriptionState.collect { state ->
                if (state is SubscriptionState.Subscribed && exitIntentPending) {
                    exitIntentPending = false
                    // Loop-7: just record that the trial bonus was granted.
                    // The promo phase itself (price, end date) is read directly
                    // from Google via the cloud function — no local counter.
                    prefs.edit()
                        .putBoolean(Constants.PREF_EXIT_INTENT_TRIAL_EXTENDED, true)
                        .putLong(Constants.PREF_PROMO_PURCHASE_TIME, System.currentTimeMillis())
                        .commit()
                }
            }
        }
    }

    /** Mark that exit-intent promo purchase flow was started. Trial extension
     *  will be applied automatically when the purchase is confirmed by Google. */
    fun onExitIntentPurchaseStarted() {
        exitIntentPending = true
    }

    /** Returns false if product details are not loaded yet or promo unavailable. */
    fun launchPurchaseFlow(
        activity: Activity,
        isYearly: Boolean = false,
        isLifetime: Boolean = false,
        usePromoOffer: Boolean = false,
    ): Boolean {
        val priceLoaded = when {
            isLifetime -> lifetimePrice.value.isNotEmpty()
            isYearly -> yearlyPrice.value.isNotEmpty()
            else -> monthlyPrice.value.isNotEmpty()
        }
        if (!priceLoaded) {
            Log.w("PaywallViewModel", "Product details not loaded")
            return false
        }
        val promoToken = if (usePromoOffer) billingManager.getMonthlyPromoOfferToken() else null
        // Safety: if promo was requested but not available, don't launch at full price
        if (usePromoOffer && promoToken == null) {
            Log.w("PaywallViewModel", "Promo offer requested but not available from Play")
            return false
        }
        billingManager.launchPurchaseFlow(
            activity,
            isYearly = isYearly,
            isLifetime = isLifetime,
            promoOfferToken = promoToken,
        )
        return true
    }
}
