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

        val (headline, subtitle, goalType) = when {
            firstGoal.contains("Stress", ignoreCase = true) -> Triple(
                context.getString(R.string.paywall_headline_stress),
                context.getString(R.string.paywall_headline_stress_sub),
                "stress",
            )
            firstGoal.contains("Klarheit", ignoreCase = true) -> Triple(
                context.getString(R.string.paywall_headline_clarity),
                context.getString(R.string.paywall_headline_clarity_sub),
                "klarheit",
            )
            firstGoal.contains("Wachstum", ignoreCase = true) -> Triple(
                context.getString(R.string.paywall_headline_growth),
                context.getString(R.string.paywall_headline_growth_sub),
                "wachstum",
            )
            firstGoal.contains("Gedanken", ignoreCase = true) -> Triple(
                context.getString(R.string.paywall_headline_thoughts),
                context.getString(R.string.paywall_headline_thoughts_sub),
                "gedanken",
            )
            else -> Triple(
                context.getString(R.string.paywall_headline_default),
                context.getString(R.string.paywall_headline_default_sub),
                "default",
            )
        }

        personalizedHeadline = MutableStateFlow(headline to subtitle).asStateFlow()
        matchedGoalType = goalType
    }

    /** Whether a real promotional offer exists in Google Play for the monthly plan. */
    val hasPromoOffer: Boolean
        get() = billingManager.getMonthlyPromoOfferToken() != null

    // Track whether exit-intent purchase flow was started — trial extension
    // is only granted AFTER Google confirms the purchase, not on flow start.
    private var exitIntentPending = false

    init {
        viewModelScope.launch {
            billingManager.subscriptionState.collect { state ->
                if (state is SubscriptionState.Subscribed && exitIntentPending) {
                    exitIntentPending = false
                    prefs.edit().putBoolean(Constants.PREF_EXIT_INTENT_TRIAL_EXTENDED, true).apply()
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
