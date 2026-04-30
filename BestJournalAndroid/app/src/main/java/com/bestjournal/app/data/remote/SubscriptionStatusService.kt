package com.bestjournal.app.data.remote

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * Result of fetching subscription status from the server-side Cloud Function.
 * All fields nullable because legacy purchases or expired subscriptions may
 * return partial data.
 */
data class SubscriptionStatusResult(
    val basePlanId: String?,
    val offerId: String?,
    val productId: String?,
    val expiryTime: String?,
    val subscriptionState: String?,
    val autoRenewing: Boolean?,
)

/**
 * Calls the Firebase Cloud Function `getSubscriptionStatus` (region europe-west1).
 * The function authenticates with a Service Account stored as a Firebase Secret
 * and calls the Google Play Subscriptions API v2 to retrieve the active basePlanId
 * and offerId for any purchaseToken — including legacy purchases that lack the
 * obfuscatedAccountId stamp introduced in v0.18.2.
 *
 * App Check is enforced server-side, so only the genuine BestJournal app can
 * invoke the function.
 */
@Singleton
class SubscriptionStatusService @Inject constructor(
    private val functions: FirebaseFunctions,
) {

    /**
     * Returns the active subscription details for the given purchase, or null
     * if the call fails for any reason. Failure is silent by design: the caller
     * falls back to the local stamp / list price.
     */
    suspend fun fetch(purchaseToken: String, productId: String): SubscriptionStatusResult? {
        if (purchaseToken.isBlank() || productId.isBlank()) return null
        return try {
            val payload = mapOf(
                "purchaseToken" to purchaseToken,
                "productId" to productId,
            )
            val callable = functions.getHttpsCallable("getSubscriptionStatus")
            val result = callable.call(payload).await()
            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any?> ?: return null
            SubscriptionStatusResult(
                basePlanId = map["basePlanId"] as? String,
                offerId = map["offerId"] as? String,
                productId = map["productId"] as? String,
                expiryTime = map["expiryTime"] as? String,
                subscriptionState = map["subscriptionState"] as? String,
                autoRenewing = map["autoRenewing"] as? Boolean,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cloud Function call failed: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "SubscriptionStatusSvc"
    }
}
