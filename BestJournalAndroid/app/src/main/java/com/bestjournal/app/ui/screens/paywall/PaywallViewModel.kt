package com.bestjournal.app.ui.screens.paywall

import android.app.Activity
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.bestjournal.app.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PaywallViewModel
@Inject
constructor(
    private val billingManager: BillingManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val source: String = savedStateHandle["source"] ?: "limit_reached"
    val monthlyPrice: StateFlow<String> = billingManager.monthlyPrice
    val yearlyPrice: StateFlow<String> = billingManager.yearlyPrice

    fun launchPurchaseFlow(activity: Activity, isYearly: Boolean) {
        billingManager.launchPurchaseFlow(activity, isYearly)
    }

    fun trackEvent(name: String, params: Map<String, String> = emptyMap()) {
        val paramStr =
            if (params.isNotEmpty()) {
                " (${params.entries.joinToString(", ") { "${it.key}=${it.value}" }})"
            } else {
                ""
            }
        Log.d("PaywallAnalytics", "Event: $name$paramStr")
        // TODO: Replace with Firebase Analytics when SDK is added
        // firebaseAnalytics.logEvent(name) { params.forEach { (k, v) -> param(k, v) } }
    }
}
