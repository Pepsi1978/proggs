package com.bestjournal.app.ui.screens.paywall

import android.app.Activity
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class PaywallViewModel
@Inject
constructor(
    private val billingManager: BillingManager,
    val analyticsTracker: AnalyticsTracker,
    prefs: SharedPreferences,
    savedStateHandle: SavedStateHandle,
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
                "Finde deine innere Ruhe",
                "Erkenne was dich belastet, bevor es dich überwältigt",
                "stress",
            )
            firstGoal.contains("Klarheit", ignoreCase = true) -> Triple(
                "Verstehe dich selbst besser",
                "Die KI erkennt Muster, die dir verborgen bleiben",
                "klarheit",
            )
            firstGoal.contains("Wachstum", ignoreCase = true) -> Triple(
                "Entdecke das Gute in jedem Tag",
                "Dein persönlicher Raum für Dankbarkeit und Wachstum",
                "wachstum",
            )
            firstGoal.contains("Gedanken", ignoreCase = true) -> Triple(
                "Lass deine Gedanken fließen",
                "Dein kreativer Begleiter ohne Grenzen",
                "gedanken",
            )
            else -> Triple(
                "Entdecke dich selbst\nJeden Tag ein Stück mehr",
                "Dein persönlicher KI-Begleiter ohne Grenzen",
                "default",
            )
        }

        personalizedHeadline = MutableStateFlow(headline to subtitle).asStateFlow()
        matchedGoalType = goalType
    }

    /** Whether a real promotional offer exists in Google Play for the monthly plan. */
    val hasPromoOffer: Boolean
        get() = billingManager.getMonthlyPromoOfferToken() != null

    /** Returns false if product details are not loaded yet (billing unavailable). */
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
            val type = when {
                isLifetime -> "lifetime"
                isYearly -> "yearly"
                else -> "monthly"
            }
            Log.w("PaywallViewModel", "Product details not loaded for $type")
            return false
        }
        val promoToken = if (usePromoOffer) billingManager.getMonthlyPromoOfferToken() else null
        billingManager.launchPurchaseFlow(
            activity,
            isYearly = isYearly,
            isLifetime = isLifetime,
            promoOfferToken = promoToken,
        )
        return true
    }
}
