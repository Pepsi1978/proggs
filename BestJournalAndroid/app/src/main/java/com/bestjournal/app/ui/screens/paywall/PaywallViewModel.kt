package com.bestjournal.app.ui.screens.paywall

import android.app.Activity
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.util.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PaywallViewModel
@Inject
constructor(
    private val billingManager: BillingManager,
    val analyticsTracker: AnalyticsTracker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val source: String = savedStateHandle["source"] ?: "limit_reached"
    val monthlyPrice: StateFlow<String> = billingManager.monthlyPrice
    val yearlyPrice: StateFlow<String> = billingManager.yearlyPrice

    /** Returns false if product details are not loaded yet (billing unavailable). */
    fun launchPurchaseFlow(activity: Activity, isYearly: Boolean): Boolean {
        val priceLoaded =
            if (isYearly) yearlyPrice.value.isNotEmpty() else monthlyPrice.value.isNotEmpty()
        if (!priceLoaded) {
            Log.w(
                "PaywallViewModel",
                "Product details not loaded for ${if (isYearly) "yearly" else "monthly"}",
            )
            return false
        }
        billingManager.launchPurchaseFlow(activity, isYearly)
        return true
    }
}
