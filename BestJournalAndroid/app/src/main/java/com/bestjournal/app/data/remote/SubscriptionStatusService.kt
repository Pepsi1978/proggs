package com.bestjournal.app.data.remote

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
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
    /** Authoritative current recurring price in micros (e.g. 3,990,000 for 3,99 €). */
    val currentPriceMicros: Long?,
    val currentPriceCurrency: String?,
    /** Industry-standard renewal indicator — changes on every successful renewal. */
    val latestOrderId: String?,
    /**
     * Pricing phase the user is currently in: "BASE" (regular recurring),
     * "INTRO" (introductory price), "FREE_TRIAL", or null. The KEY signal
     * for the app to know whether the promo is still active.
     */
    val offerPhase: String?,
)

/**
 * Loop-5 fix: callers need to distinguish "subscription truly does not exist"
 * (so we should set Free immediately) from "we could not reach the server"
 * (where keeping the previous state is safer than guessing). The legacy
 * `fetch()` return value of nullable lumped both cases together — that
 * ambiguity caused the app to keep showing "Premium aktiv" for hours after
 * a subscription expired.
 */
sealed class FetchResult {
    /** Server returned valid subscription data. */
    data class Found(val data: SubscriptionStatusResult) : FetchResult()
    /** Server confirms the subscription is not active (404/410, expired, never existed). */
    data object NotFound : FetchResult()
    /** Network error, auth error, timeout, or any other transient failure. */
    data object Error : FetchResult()
}

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
     *
     * Legacy method kept for callers that do not need to distinguish between
     * "not found" and "network error". Prefer [fetchWithStatus] for the
     * subscription-state pipeline so genuine expirations can be detected.
     */
    suspend fun fetch(purchaseToken: String, productId: String): SubscriptionStatusResult? {
        return when (val result = fetchWithStatus(purchaseToken, productId)) {
            is FetchResult.Found -> result.data
            FetchResult.NotFound, FetchResult.Error -> null
        }
    }

    /**
     * Loop-5 entry point: returns a structured result so callers can downgrade
     * to Free only on confirmed expirations and keep the previous state on
     * transient errors. See [FetchResult] for the contract.
     */
    suspend fun fetchWithStatus(purchaseToken: String, productId: String): FetchResult {
        if (purchaseToken.isBlank() || productId.isBlank()) return FetchResult.Error
        return try {
            val payload = mapOf(
                "purchaseToken" to purchaseToken,
                "productId" to productId,
            )
            val callable = functions.getHttpsCallable("getSubscriptionStatus")
            val result = callable.call(payload).await()
            @Suppress("UNCHECKED_CAST")
            val map = result.data as? Map<String, Any?> ?: return FetchResult.Error
            val parsed = SubscriptionStatusResult(
                basePlanId = map["basePlanId"] as? String,
                offerId = map["offerId"] as? String,
                productId = map["productId"] as? String,
                expiryTime = map["expiryTime"] as? String,
                subscriptionState = map["subscriptionState"] as? String,
                autoRenewing = map["autoRenewing"] as? Boolean,
                currentPriceMicros = (map["currentPriceMicros"] as? Number)?.toLong(),
                currentPriceCurrency = map["currentPriceCurrency"] as? String,
                latestOrderId = map["latestOrderId"] as? String,
                offerPhase = map["offerPhase"] as? String,
            )
            // Treat explicit EXPIRED state as NotFound so the app downgrades
            // to Free even when Google still serves the purchase record. The
            // Cloud Function returns the literal Subscriptions API v2 enum.
            if (parsed.subscriptionState == "SUBSCRIPTION_STATE_EXPIRED") {
                Log.d(TAG, "Subscription state is EXPIRED — treating as NotFound")
                return FetchResult.NotFound
            }
            FetchResult.Found(parsed)
        } catch (e: FirebaseFunctionsException) {
            // The Cloud Function maps Subscriptions API v2 404/410 to NOT_FOUND
            // (functions/index.js line 107-108). Any other code is treated as
            // a transient error so the app keeps the previous state.
            if (e.code == FirebaseFunctionsException.Code.NOT_FOUND) {
                Log.d(TAG, "Cloud Function reports NOT_FOUND — subscription truly expired")
                FetchResult.NotFound
            } else {
                Log.w(
                    TAG,
                    "Cloud Function transient error (code=${e.code}): ${e.message}",
                )
                FetchResult.Error
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud Function call failed: ${e.message}")
            FetchResult.Error
        }
    }

    companion object {
        private const val TAG = "SubscriptionStatusSvc"
    }
}
