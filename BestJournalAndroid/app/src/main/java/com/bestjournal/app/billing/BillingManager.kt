package com.bestjournal.app.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.*
import com.bestjournal.app.data.remote.SubscriptionStatusService
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class BillingManager
@Inject
constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val encryptedPrefs: SharedPreferences,
    private val subscriptionStatusService: SubscriptionStatusService,
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val MONTHLY_PRODUCT_ID = "bestjournal_ai_monthly"
        const val YEARLY_PRODUCT_ID = "bestjournal_ai_yearly"
        const val LIFETIME_PRODUCT_ID = "bestjournal_lifetime"
        // Base plan IDs for the main (non-retention, non-promo) plans
        private const val MONTHLY_BASE_PLAN_ID = "monthly"
        private const val YEARLY_BASE_PLAN_ID = "yearly"
        // Cloud Function call cache window. Currently 0 — every refresh hits
        // the server, which is fine while the user base is small (well below
        // the Firebase free-tier limit of 2M invocations / month). Re-enable
        // (e.g. 5 minutes = 5L * 60L * 1000L) when monthly invocations approach
        // ~1.5M (~70% of free tier) — Firebase will email a warning before that.
        // Monitoring: console.firebase.google.com/project/bestjurnal-a15b9/functions
        private const val CLOUD_STATUS_CACHE_MS = 0L
    }

    // Single supervisor scope for fire-and-forget background calls (Cloud Function).
    // SupervisorJob ensures one failing child does not cancel the scope.
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile private var billingClient: BillingClient? = null
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _subscriptionType = MutableStateFlow(SubscriptionType.NONE)
    val subscriptionType: StateFlow<SubscriptionType> = _subscriptionType.asStateFlow()

    private val _monthlyPrice = MutableStateFlow("")
    val monthlyPrice: StateFlow<String> = _monthlyPrice.asStateFlow()

    private val _yearlyPrice = MutableStateFlow("")
    val yearlyPrice: StateFlow<String> = _yearlyPrice.asStateFlow()

    private val _lifetimePrice = MutableStateFlow("")
    val lifetimePrice: StateFlow<String> = _lifetimePrice.asStateFlow()

    @Volatile private var monthlyProductDetails: ProductDetails? = null
    @Volatile private var yearlyProductDetails: ProductDetails? = null
    @Volatile private var lifetimeProductDetails: ProductDetails? = null

    // Store active purchase token for subscription updates (retention offers)
    @Volatile private var activePurchaseToken: String? = null

    // Serialises subscription-state writes from the cloud function so two
    // racing refresh batches cannot interleave their SharedPreferences edits.
    private val promoSyncLock = Any()

    // Server-authoritative current price in micros (from Cloud Function).
    // 0 means "not yet known". UI must check this before falling back to
    // BillingClient.pricingPhases (which doesn't know which phase is active).
    private val _serverCurrentPriceMicros = MutableStateFlow(
        encryptedPrefs.getLong(Constants.PREF_CURRENT_PRICE_MICROS, 0L)
    )
    val serverCurrentPriceMicros: StateFlow<Long> = _serverCurrentPriceMicros.asStateFlow()

    private val _serverCurrentPriceCurrency = MutableStateFlow(
        encryptedPrefs.getString(Constants.PREF_CURRENT_PRICE_CURRENCY, "") ?: ""
    )
    val serverCurrentPriceCurrency: StateFlow<String> =
        _serverCurrentPriceCurrency.asStateFlow()

    // True if the user has cancelled the subscription (it stays active until
    // expiryTime but won't auto-renew). UI should show "Abo gekuendigt — laeuft ab am ...".
    private val _autoRenewing = MutableStateFlow(
        encryptedPrefs.getBoolean(Constants.PREF_AUTO_RENEWING, true)
    )
    val autoRenewing: StateFlow<Boolean> = _autoRenewing.asStateFlow()

    // Currently-active pricing phase from Cloud Function. "BASE" means the
    // promo is over and recurring price applies; "INTRO" means user is still
    // in the discounted phase. Null if Cloud Function unavailable.
    private val _offerPhase = MutableStateFlow(
        encryptedPrefs.getString(Constants.PREF_OFFER_PHASE, null)
    )
    val offerPhase: StateFlow<String?> = _offerPhase.asStateFlow()

    // ISO-8601 expiry time string from Cloud Function — the moment the
    // current pricing phase ends (next renewal or, if cancelled, end of
    // service). Persisted so it survives app restarts and is immediately
    // available when the dialog opens, before the cloud call returns.
    private val _expiryTime = MutableStateFlow<String?>(
        encryptedPrefs.getString(Constants.PREF_LAST_KNOWN_EXPIRY, null)
    )
    val expiryTime: StateFlow<String?> = _expiryTime.asStateFlow()

    // Loop-10 fix (Frank, 2026-04-30): the previous attempt at exposing
    // "is the user on a retention plan" derived the answer from
    // subscriptionType + a one-shot prefs read inside a map { } lambda.
    // That lambda only re-fires when subscriptionType ITSELF changes, but
    // a switch from basePlanId="monthly" to basePlanId="retention-monthly-75"
    // keeps subscriptionType=MONTHLY for both sides of the change, so
    // isOnRetentionPlanState stayed stuck on its initial value forever.
    // SharedPreferences are not reactive on their own, so we surface the
    // active base-plan id as a proper StateFlow that every code path
    // updating PREF_ACTIVE_BASE_PLAN_ID has to keep in sync.
    private val _activeBasePlanId = MutableStateFlow(
        encryptedPrefs.getString(Constants.PREF_ACTIVE_BASE_PLAN_ID, "") ?: ""
    )
    val activeBasePlanId: StateFlow<String> = _activeBasePlanId.asStateFlow()

    // Guard against duplicate purchase flows triggered by double-tap
    private val isPurchaseInFlight = AtomicBoolean(false)

    // Race-condition fix (Loop-4 Bug #1): The Free state may only be set when
    // BOTH querySubscriptions AND queryInAppPurchases have returned without
    // finding an active purchase. Otherwise the user briefly sees the
    // "Premium kaufen" screen while one query is still in flight.
    private val activeQueryFoundPurchase =
        java.util.concurrent.atomic.AtomicBoolean(false)
    private val pendingPurchaseQueries =
        java.util.concurrent.atomic.AtomicInteger(0)

    // Loop-5 fix: When a queryPurchasesAsync callback comes back with a non-OK
    // responseCode, BillingClient does NOT carry purchase data — but it also
    // does not mean the user is unsubscribed. The previous code never invoked
    // finalizePurchaseQueryBatch in that branch, so the pending counter could
    // stick at 1 and break the next refresh; AND the same "no purchase found"
    // path was used for both genuinely empty responses and transient errors.
    // This flag lets finalizePurchaseQueryBatch keep the previous state in
    // the error case instead of falsely downgrading to Free.
    private val anyQueryFailed =
        java.util.concurrent.atomic.AtomicBoolean(false)

    // Loop-5 fix: When finalizePurchaseQueryBatch decides we are no longer
    // Subscribed but the previous state WAS Subscribed, it must verify the
    // expiration with the Cloud Function before downgrading the UI. To avoid
    // sending three or four parallel verification calls in the same refresh
    // cycle (one per finalize call, one per dialog open), only one is allowed
    // at a time.
    private val verifyExpirationInFlight =
        java.util.concurrent.atomic.AtomicBoolean(false)

    private val connectionListener =
        object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(
                    TAG,
                    "onBillingSetupFinished: responseCode=${billingResult.responseCode}",
                )
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // 1. Reset bookkeeping then run BOTH purchase queries.
                    //    Free state is only set after BOTH return without any
                    //    purchase — see querySubscriptions / queryInAppPurchases.
                    activeQueryFoundPurchase.set(false)
                    anyQueryFailed.set(false)
                    pendingPurchaseQueries.set(2)
                    queryInAppPurchases()
                    querySubscriptions()
                    queryProductDetails()
                    queryLifetimeDetails()
                    // 2. Retry any acknowledgement that failed in a previous
                    //    session (otherwise Google auto-refunds after 3 days).
                    retryPendingAcknowledgement()
                }
            }

            // K-1 fix: Reconnect when billing service disconnects
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected — attempting reconnect")
                billingClient?.startConnection(this)
            }
        }

    /**
     * If the previous app session left a pending acknowledgement token
     * (PREF_PENDING_ACK_TOKEN), retry it now. Required because acknowledge
     * must happen within 3 days or Google auto-refunds the purchase.
     */
    private fun retryPendingAcknowledgement() {
        val pending = encryptedPrefs.getString(Constants.PREF_PENDING_ACK_TOKEN, null)
        if (pending.isNullOrBlank()) return
        Log.d(TAG, "retryPendingAcknowledgement: retrying token=${pending.take(12)}...")
        val ackParams =
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(pending).build()
        billingClient?.acknowledgePurchase(ackParams) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "retryPendingAcknowledgement: SUCCESS")
                encryptedPrefs.edit().remove(Constants.PREF_PENDING_ACK_TOKEN).commit()
            } else {
                Log.w(
                    TAG,
                    "retryPendingAcknowledgement: still failed responseCode=${result.responseCode}",
                )
            }
        }
    }

    fun initialize(context: Context) {
        billingClient =
            BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()

        billingClient?.startConnection(connectionListener)
    }

    /**
     * Forces a full re-query against Google Play. Should be called whenever the
     * UI screen that displays subscription state opens (e.g. SettingsScreen) so
     * promo renewals, plan changes and price updates are picked up without
     * needing a full app restart. Cheap to call — no UI side effects.
     *
     * If the BillingClient is not yet connected, the call is silently skipped;
     * the connection listener will run a fresh query as soon as it is ready.
     */
    fun refreshSubscriptionStatus() {
        val client = billingClient ?: run {
            Log.d(TAG, "refreshSubscriptionStatus: billingClient is null, skipping")
            return
        }
        if (!client.isReady) {
            // Loop-11 fix (Frank, 2026-05-01): silently skipping here meant
            // that if the BillingClient had disconnected (e.g. long
            // background, OS service restart), the user could open Settings
            // forever and the cached "Premium aktiv" / "Lebenszeit" tile
            // would never refresh. For LIFETIME there is no "Abo verwalten"
            // button to manually trigger a re-query either, so the user was
            // stuck. Now we restart the connection — onBillingSetupFinished
            // runs a full re-query batch as soon as it succeeds.
            Log.d(
                TAG,
                "refreshSubscriptionStatus: billingClient not ready, restarting connection",
            )
            try {
                client.startConnection(connectionListener)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "refreshSubscriptionStatus: startConnection failed: ${e.message}")
            }
            return
        }
        Log.d(TAG, "refreshSubscriptionStatus: triggering full re-query")
        // Reset purchase-detection bookkeeping for this query batch. Only the
        // last completing query will be allowed to set Free state, and only
        // when the bookkeeping shows no purchase was found by anyone.
        activeQueryFoundPurchase.set(false)
        anyQueryFailed.set(false)
        pendingPurchaseQueries.set(2) // querySubscriptions + queryInAppPurchases
        querySubscriptions()
        queryInAppPurchases()
        queryProductDetails()
        queryLifetimeDetails()
    }

    private fun querySubscriptions() {
        Log.d(TAG, "querySubscriptions: starting query")
        val params =
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            Log.d(
                TAG,
                "querySubscriptions: result=${billingResult.responseCode}, purchases=${purchases.size}",
            )
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEachIndexed { i, p ->
                    Log.d(
                        TAG,
                        "querySubscriptions: purchase[$i] products=${p.products}, " +
                            "state=${p.purchaseState}, ack=${p.isAcknowledged}, " +
                            "purchaseTime=${p.purchaseTime}, " +
                            "tokenPrefix=${p.purchaseToken.take(12)}",
                    )
                }
                // Multiple subscriptions can be active during plan switches (old
                // monthly + new yearly). Pick the most recent acknowledged purchase
                // deterministically: yearly preferred over monthly, then by purchaseTime.
                val activePurchase = purchases
                    .filter { purchase ->
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                            purchase.isAcknowledged
                    }
                    .sortedWith(
                        compareByDescending<Purchase> { p ->
                            when {
                                p.products.contains(YEARLY_PRODUCT_ID) -> 2
                                p.products.contains(MONTHLY_PRODUCT_ID) -> 1
                                else -> 0
                            }
                        }
                            .thenByDescending { it.purchaseTime }
                    )
                    .firstOrNull()
                Log.d(TAG, "querySubscriptions: activePurchase=${activePurchase != null}")
                if (activePurchase != null) {
                    activeQueryFoundPurchase.set(true)
                    _subscriptionState.value = SubscriptionState.Subscribed
                    activePurchaseToken = activePurchase.purchaseToken
                    _subscriptionType.value =
                        when {
                            activePurchase.products.contains(YEARLY_PRODUCT_ID) ->
                                SubscriptionType.YEARLY
                            activePurchase.products.contains(MONTHLY_PRODUCT_ID) ->
                                SubscriptionType.MONTHLY
                            else -> SubscriptionType.MONTHLY
                        }
                    // Loop-5 fix: persist token+productId so finalizePurchaseQueryBatch
                    // can ask the Cloud Function whether the subscription truly
                    // expired before downgrading to Free on a transient empty
                    // BillingClient response.
                    encryptedPrefs.edit()
                        .putString(
                            Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN,
                            activePurchase.purchaseToken,
                        )
                        .putString(
                            Constants.PREF_LAST_KNOWN_PRODUCT_ID,
                            activePurchase.products.firstOrNull() ?: "",
                        )
                        .apply()
                    // Loop-5 fix: BillingClient.Purchase.isAutoRenewing is cached
                    // locally and stays "true" for several minutes after the
                    // user cancels in the Play Store. Once the Cloud Function
                    // has provided an authoritative value (PREF_AUTO_RENEWING_CLOUD_SEEN),
                    // we MUST NOT overwrite it with the stale local value or
                    // the Settings dialog flickers between "gekuendigt" and
                    // "automatisch" on every refresh.
                    val cloudSeen = encryptedPrefs.getBoolean(
                        Constants.PREF_AUTO_RENEWING_CLOUD_SEEN,
                        false,
                    )
                    if (!cloudSeen) {
                        _autoRenewing.value = activePurchase.isAutoRenewing
                        encryptedPrefs.edit()
                            .putBoolean(
                                Constants.PREF_AUTO_RENEWING,
                                activePurchase.isAutoRenewing,
                            )
                            .commit()
                    } else {
                        Log.d(
                            TAG,
                            "querySubscriptions: cloud-seen flag set, " +
                                "keeping autoRenewing=${_autoRenewing.value} " +
                                "(local would have set ${activePurchase.isAutoRenewing})",
                        )
                    }
                    // Recover active base plan from Google's stamp if local state is missing
                    // (covers app re-install or upgrade from pre-v0.18.2 versions)
                    recoverActiveBasePlanFromStamp(activePurchase)
                    // Final fallback for legacy purchases without stamp: ask the
                    // server-side Cloud Function which calls Subscriptions API v2.
                    maybeRefreshActiveBasePlanFromCloud(activePurchase)
                    // Loop-7 (Frank, 2026-04-30): pull the AUTHORITATIVE current
                    // price, offer phase, autoRenewing flag and expiryTime
                    // straight from Google's Subscriptions API v2. No local
                    // counter is decremented anymore — the cloud's offerPhase
                    // (INTRO vs BASE) is the single source of truth.
                    syncSubscriptionStatusFromCloud(activePurchase)
                }
                // Free is only set in finalizePurchaseQueryBatch() after BOTH
                // querySubscriptions AND queryInAppPurchases finished without
                // finding any purchase (race-condition fix).
                finalizePurchaseQueryBatch()

                // K-2 fix: Acknowledge unacknowledged purchases — only set Subscribed AFTER success
                purchases
                    .filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
                    }
                    .forEach { purchase -> acknowledgePurchase(purchase) }
            } else {
                // Loop-5 fix: non-OK response is a transient error, not a
                // confirmation that the user is unsubscribed. Mark the batch
                // as failed and STILL run finalizePurchaseQueryBatch so the
                // pending counter does not get stuck at 1.
                Log.w(
                    TAG,
                    "querySubscriptions: non-OK response code=${billingResult.responseCode}, " +
                        "msg=${billingResult.debugMessage}",
                )
                anyQueryFailed.set(true)
                finalizePurchaseQueryBatch()
            }
        }
    }

    /**
     * Loop-7 fix (Frank, 2026-04-30): pulls the authoritative subscription
     * status from the Firebase Cloud Function (which speaks directly to
     * Google's Subscriptions API v2) and writes the FACTS into the local
     * StateFlows / SharedPreferences. There is intentionally NO counter or
     * client-side guess about how many promo cycles are left — Google's
     * `offerPhase` field ("INTRO" vs "BASE") is the single source of truth,
     * and `expiryTime` tells us to the second when the current phase ends.
     *
     * Throttled to one call per CLOUD_STATUS_CACHE_MS window so the function
     * stays in the Firebase free tier even with frequent Settings opens.
     */
    private fun syncSubscriptionStatusFromCloud(purchase: Purchase) {
        val now = System.currentTimeMillis()
        val lastFetch = encryptedPrefs.getLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, 0L)
        if (now - lastFetch < CLOUD_STATUS_CACHE_MS) {
            Log.d(TAG, "syncSubscriptionStatusFromCloud: skipped (within cache window)")
            return
        }

        val productId = purchase.products.firstOrNull() ?: return
        val token = purchase.purchaseToken

        backgroundScope.launch {
            Log.d(TAG, "syncSubscriptionStatusFromCloud: calling cloud function")
            val fetchResult = try {
                kotlinx.coroutines.withTimeout(15_000L) {
                    subscriptionStatusService.fetchWithStatus(token, productId)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "syncSubscriptionStatusFromCloud: cloud call timed out")
                com.bestjournal.app.data.remote.FetchResult.Error
            }

            // Loop-5 fix: distinguish between "transient error" (keep state)
            // and "subscription truly expired" (downgrade to Free). The legacy
            // null-return-value lumped both cases together which left the app
            // showing "Premium aktiv" for hours after a real expiry.
            if (fetchResult is com.bestjournal.app.data.remote.FetchResult.NotFound) {
                synchronized(promoSyncLock) {
                    Log.d(
                        TAG,
                        "syncSubscriptionStatusFromCloud: cloud confirms expired, downgrading to Free",
                    )
                    _subscriptionState.value = SubscriptionState.Free
                    _subscriptionType.value = SubscriptionType.NONE
                    activePurchaseToken = null
                    clearPromoStateIfNoSubscription()
                    encryptedPrefs.edit()
                        .remove(Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN)
                        .remove(Constants.PREF_LAST_KNOWN_PRODUCT_ID)
                        .putLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, now)
                        .commit()
                }
                return@launch
            }

            val result =
                (fetchResult as? com.bestjournal.app.data.remote.FetchResult.Found)?.data
                    ?: run {
                        Log.d(
                            TAG,
                            "syncSubscriptionStatusFromCloud: transient error — keeping state",
                        )
                        encryptedPrefs.edit()
                            .putLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, now)
                            .commit()
                        return@launch
                    }

            // Loop-7 (Frank, 2026-04-30): write the AUTHORITATIVE cloud values
            // straight into the StateFlows. No counters. No anchors. No "is
            // this a first sighting" guesses. Google tells us:
            //   - autoRenewing       (true/false)
            //   - offerPhase         ("INTRO" | "BASE" | "FREE_TRIAL" | null)
            //   - currentPriceMicros (what is being charged right now)
            //   - expiryTime         (when the current phase ends)
            //   - latestOrderId      (kept for debug only — no UI impact)
            // The dialog renders directly from these fields, on the second.
            synchronized(promoSyncLock) {
                result.currentPriceMicros?.let { micros ->
                    encryptedPrefs.edit()
                        .putLong(Constants.PREF_CURRENT_PRICE_MICROS, micros)
                        .putString(
                            Constants.PREF_CURRENT_PRICE_CURRENCY,
                            result.currentPriceCurrency ?: "",
                        )
                        .commit()
                    _serverCurrentPriceMicros.value = micros
                    _serverCurrentPriceCurrency.value = result.currentPriceCurrency ?: ""
                }
                result.autoRenewing?.let { renewing ->
                    encryptedPrefs.edit()
                        .putBoolean(Constants.PREF_AUTO_RENEWING, renewing)
                        .putBoolean(Constants.PREF_AUTO_RENEWING_CLOUD_SEEN, true)
                        .commit()
                    _autoRenewing.value = renewing
                }
                result.offerPhase?.let { phase ->
                    encryptedPrefs.edit()
                        .putString(Constants.PREF_OFFER_PHASE, phase)
                        .commit()
                    _offerPhase.value = phase
                }
                if (!result.expiryTime.isNullOrBlank()) {
                    encryptedPrefs.edit()
                        .putString(Constants.PREF_LAST_KNOWN_EXPIRY, result.expiryTime)
                        .commit()
                }
                _expiryTime.value = result.expiryTime
                // Loop-9 audit fix (Frank, 2026-04-30): rememberActiveBasePlan
                // writes the chosen base-plan id OPTIMISTICALLY before
                // launchBillingFlow even fires — if the user cancels in the
                // Play sheet the prefs would be stuck with a wrong value
                // (e.g. "retention-monthly-75" while the user is still on
                // "monthly"). The cloud is the single source of truth, so
                // overwrite the local optimistic stamp with whatever Google
                // actually reports for the active subscription.
                if (!result.basePlanId.isNullOrBlank()) {
                    encryptedPrefs.edit()
                        .putString(Constants.PREF_ACTIVE_BASE_PLAN_ID, result.basePlanId)
                        .putString(Constants.PREF_ACTIVE_OFFER_ID, result.offerId ?: "")
                        .commit()
                    _activeBasePlanId.value = result.basePlanId
                }
                encryptedPrefs.edit()
                    .putLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, now)
                    .commit()
                Log.d(
                    TAG,
                    "syncSubscriptionStatusFromCloud: " +
                        "phase=${result.offerPhase}, " +
                        "autoRenewing=${result.autoRenewing}, " +
                        "currentPrice=${result.currentPriceMicros}, " +
                        "expiry=${result.expiryTime}",
                )
            }
        }
    }

    /** Clears stale promo + active-plan state if the subscription is no longer active. */
    private fun clearPromoStateIfNoSubscription() {
        encryptedPrefs.edit()
            .putInt(Constants.PREF_PROMO_TOTAL_MONTHS, 0)
            .remove(Constants.PREF_PROMO_LAST_PURCHASE_TIME)
            .remove(Constants.PREF_ACTIVE_BASE_PLAN_ID)
            .remove(Constants.PREF_ACTIVE_OFFER_ID)
            .remove(Constants.PREF_LAST_CLOUD_STATUS_FETCH)
            .remove(Constants.PREF_LAST_PROMO_CLOUD_FETCH)
            .remove(Constants.PREF_LAST_KNOWN_EXPIRY)
            // Loop-5 fix: when the subscription is genuinely gone, reset the
            // cloud-seen flag and last-known token so a later re-subscription
            // starts from a clean slate (otherwise stale flags could mask
            // BillingClient updates the next time the user purchases).
            .remove(Constants.PREF_AUTO_RENEWING_CLOUD_SEEN)
            .remove(Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN)
            .remove(Constants.PREF_LAST_KNOWN_PRODUCT_ID)
            .remove(Constants.PREF_AUTO_RENEWING)
            .remove(Constants.PREF_OFFER_PHASE)
            .remove(Constants.PREF_CURRENT_PRICE_MICROS)
            .remove(Constants.PREF_CURRENT_PRICE_CURRENCY)
            .commit()
        _autoRenewing.value = true
        _offerPhase.value = null
        _expiryTime.value = null
        _serverCurrentPriceMicros.value = 0L
        _serverCurrentPriceCurrency.value = ""
        _activeBasePlanId.value = ""
    }

    /**
     * Records which base plan + offer the user is buying so getActiveBasePlanPrice()
     * can return the truthful current price (e.g. retention-yearly-75 = 22,49 €/year)
     * instead of the main base plan default. Falls back silently if no match.
     */
    private fun rememberActiveBasePlan(productDetails: ProductDetails, offerToken: String) {
        val matched =
            productDetails.subscriptionOfferDetails?.firstOrNull { it.offerToken == offerToken }
                ?: return
        encryptedPrefs.edit()
            .putString(Constants.PREF_ACTIVE_BASE_PLAN_ID, matched.basePlanId)
            .putString(Constants.PREF_ACTIVE_OFFER_ID, matched.offerId ?: "")
            .apply()
        _activeBasePlanId.value = matched.basePlanId
    }

    /**
     * Reconstructs the active base plan + offer from Google's obfuscatedAccountId stamp
     * if local prefs are empty. Format: "bp:<basePlanId>|of:<offerId>". Survives app
     * re-installs because Google retains the stamp on the subscription record.
     */
    private fun recoverActiveBasePlanFromStamp(purchase: Purchase) {
        val haveLocal = !encryptedPrefs.getString(Constants.PREF_ACTIVE_BASE_PLAN_ID, null).isNullOrEmpty()
        if (haveLocal) return
        val stamp = purchase.accountIdentifiers?.obfuscatedAccountId ?: return
        val match = Regex("^bp:([^|]+)\\|of:(.*)$").matchEntire(stamp) ?: return
        val basePlan = match.groupValues[1]
        val offerId = match.groupValues[2]
        encryptedPrefs.edit()
            .putString(Constants.PREF_ACTIVE_BASE_PLAN_ID, basePlan)
            .putString(Constants.PREF_ACTIVE_OFFER_ID, offerId)
            .apply()
        _activeBasePlanId.value = basePlan
        Log.d(TAG, "Recovered active base plan from stamp: $basePlan / $offerId")
    }

    /**
     * Final fallback when neither local prefs nor the obfuscatedAccountId stamp
     * carry the active base plan info — typically legacy purchases made before
     * v0.18.2 or v0.18.4. Asks the Firebase Cloud Function which queries
     * Google Play Subscriptions API v2 server-side. Throttled to one call per
     * hour to stay well within the free tier and avoid unnecessary network IO.
     */
    private fun maybeRefreshActiveBasePlanFromCloud(purchase: Purchase) {
        val haveLocal =
            !encryptedPrefs.getString(Constants.PREF_ACTIVE_BASE_PLAN_ID, null).isNullOrEmpty()
        if (haveLocal) return

        val now = System.currentTimeMillis()
        val lastFetch = encryptedPrefs.getLong(Constants.PREF_LAST_CLOUD_STATUS_FETCH, 0L)
        if (now - lastFetch < CLOUD_STATUS_CACHE_MS) return

        val productId = purchase.products.firstOrNull() ?: return
        val token = purchase.purchaseToken

        backgroundScope.launch {
            val result = subscriptionStatusService.fetch(token, productId)
            if (result?.basePlanId.isNullOrBlank()) {
                // Mark the attempt so we don't retry within the cache window even
                // when the call returned null (network error, expired token, etc.).
                encryptedPrefs.edit()
                    .putLong(Constants.PREF_LAST_CLOUD_STATUS_FETCH, now)
                    .apply()
                return@launch
            }
            encryptedPrefs.edit()
                .putString(Constants.PREF_ACTIVE_BASE_PLAN_ID, result.basePlanId)
                .putString(Constants.PREF_ACTIVE_OFFER_ID, result.offerId ?: "")
                .putLong(Constants.PREF_LAST_CLOUD_STATUS_FETCH, now)
                .apply()
            _activeBasePlanId.value = result.basePlanId.orEmpty()
            Log.d(
                TAG,
                "Recovered active base plan from Cloud Function: " +
                    "${result.basePlanId} / ${result.offerId ?: ""}",
            )
        }
    }

    /**
     * Returns the formatted price of the currently active base plan + offer, or null
     * if no remembered plan or product details are not yet loaded. Used by the in-app
     * subscription overview so the displayed price always matches what Google charges.
     */
    fun getActiveBasePlanPrice(): String? {
        val basePlan =
            encryptedPrefs.getString(Constants.PREF_ACTIVE_BASE_PLAN_ID, null) ?: return null
        val offerId = encryptedPrefs.getString(Constants.PREF_ACTIVE_OFFER_ID, "") ?: ""
        val productDetails =
            when (subscriptionType.value) {
                SubscriptionType.YEARLY -> yearlyProductDetails
                SubscriptionType.MONTHLY -> monthlyProductDetails
                else -> null
            } ?: return null
        val offer =
            productDetails.subscriptionOfferDetails?.firstOrNull {
                it.basePlanId == basePlan && (it.offerId ?: "") == offerId
            } ?: return null
        // Pricing phases are chronological: index 0 = currently-active phase
        // (intro/promo), last = regular price after promo ends. Show the
        // CURRENT phase to the user (what they actually pay right now). For
        // single-phase offers (regular plan) this is identical to lastOrNull().
        val phases = offer.pricingPhases.pricingPhaseList
        return phases.firstOrNull()?.formattedPrice
    }

    private fun queryInAppPurchases() {
        val params =
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val lifetimePurchase = purchases.firstOrNull { purchase ->
                    purchase.products.contains(LIFETIME_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.isAcknowledged
                }
                val hasLifetime = lifetimePurchase != null
                if (hasLifetime) {
                    activeQueryFoundPurchase.set(true)
                    _subscriptionState.value = SubscriptionState.Subscribed
                    _subscriptionType.value = SubscriptionType.LIFETIME
                    // Loop-12 fix (Frank, 2026-05-01): persist the Lifetime
                    // token so verifyLifetimeWithCloud() can ask the Cloud
                    // Function — which speaks to Products API — whether the
                    // purchase is still PURCHASED or has been revoked /
                    // refunded. BillingClient's local cache cannot tell us.
                    encryptedPrefs.edit()
                        .putString(
                            Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN,
                            lifetimePurchase!!.purchaseToken,
                        )
                        .putString(
                            Constants.PREF_LAST_KNOWN_PRODUCT_ID,
                            LIFETIME_PRODUCT_ID,
                        )
                        .apply()
                    // Server-side verification kicks off in the background.
                    // If the cloud reports NotFound, the user is downgraded
                    // to Free immediately; transient errors keep the state.
                    verifyLifetimeWithCloud(lifetimePurchase.purchaseToken)
                }
                purchases
                    .filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
                    }
                    .forEach { purchase -> acknowledgePurchase(purchase) }
            } else {
                // Loop-5 fix: non-OK response means BillingClient could not
                // tell us anything — keep previous state by recording the
                // failure flag, but still drain the pending counter.
                Log.w(
                    TAG,
                    "queryInAppPurchases: non-OK response code=${billingResult.responseCode}, " +
                        "msg=${billingResult.debugMessage}",
                )
                anyQueryFailed.set(true)
            }
            // Free state can only be set after BOTH purchase queries are done
            // (race-condition fix — Loop-4 Bug #1).
            finalizePurchaseQueryBatch()
        }
    }

    /**
     * Called by both querySubscriptions and queryInAppPurchases when their
     * callbacks complete. Only the LAST one (when pending counter hits 0)
     * actually decides whether to set Free state — and only if neither query
     * found an active purchase. This prevents the Premium-screen-flash bug
     * where querySubscriptions returned first with no subs but the Lifetime
     * check in queryInAppPurchases was still pending.
     *
     * Loop-5 fix: BillingClient.queryPurchasesAsync can briefly return empty
     * during reconnects or in the few minutes right after a Play Store
     * cancellation, even though the user is still entitled to Premium until
     * the expiry date. The previous logic caused the underlying Settings
     * screen to flash to "Premium freischalten" when the user clicked
     * "Verwalten" — exactly the symptom Frank reported. We now defer the
     * downgrade until either:
     *   - the Cloud Function confirms expiration (FetchResult.NotFound), OR
     *   - the previous state was already Free (no regression to fix).
     * Transient errors keep the previous state, eliminating the flicker.
     */
    private fun finalizePurchaseQueryBatch() {
        val remaining = pendingPurchaseQueries.decrementAndGet()
        // Loop-5 fix: strict equality so a stale callback from a previous
        // refresh batch (which made the counter go negative when a second
        // refresh happened mid-flight) does not run finalize twice and
        // accidentally downgrade to Free on the second pass.
        if (remaining != 0) return
        if (activeQueryFoundPurchase.get()) return
        if (anyQueryFailed.get()) {
            Log.d(
                TAG,
                "finalizePurchaseQueryBatch: at least one query failed — " +
                    "keeping previous state (state=${_subscriptionState.value})",
            )
            return
        }
        val previousState = _subscriptionState.value
        if (previousState is SubscriptionState.Subscribed) {
            // Loop-11 fix (Frank, 2026-05-01): LIFETIME purchases are INAPP,
            // not subscriptions — the Cloud Function (which queries the
            // Subscriptions API v2) cannot verify them. queryInAppPurchases
            // succeeded with an empty result, so we can trust BillingClient
            // here and downgrade directly. Without this branch the call
            // below would either hit Cloud with a stale SUB token (giving
            // NotFound and a correct downgrade by accident) or, if no token
            // was persisted, fall through anyway — both paths worked but
            // were confusing. Make the LIFETIME case explicit and skip the
            // unnecessary cloud roundtrip.
            val previousType = _subscriptionType.value
            if (previousType == SubscriptionType.LIFETIME) {
                Log.d(
                    TAG,
                    "finalizePurchaseQueryBatch: was LIFETIME but no INAPP purchase " +
                        "found — downgrading directly (Cloud Function cannot verify INAPP)",
                )
                // Fall through to the Free state setter below.
            } else {
                // Both queries succeeded but reported nothing while we were
                // Subscribed (subscription case). This is the suspicious case
                // — a real Play Store cancellation only takes effect at
                // expiryTime, so during the grace window the BillingClient
                // cache should still report it. Verify with the Cloud
                // Function before downgrading.
                val lastToken = encryptedPrefs.getString(
                    Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN,
                    null,
                )
                val lastProduct = encryptedPrefs.getString(
                    Constants.PREF_LAST_KNOWN_PRODUCT_ID,
                    null,
                )
                if (!lastToken.isNullOrBlank() && !lastProduct.isNullOrBlank()) {
                    Log.d(
                        TAG,
                        "finalizePurchaseQueryBatch: was Subscribed but BillingClient " +
                            "is empty — verifying with Cloud Function before downgrade",
                    )
                    verifyExpirationWithCloud(lastToken, lastProduct)
                    return
                }
                // No persisted token. Fall through: this is a fresh install or
                // the user never had a subscription, so treating empty as Free
                // is the correct behaviour.
                Log.d(
                    TAG,
                    "finalizePurchaseQueryBatch: was Subscribed but no persisted " +
                        "token — assuming legacy state, downgrading to Free",
                )
            }
        }
        _subscriptionState.value = SubscriptionState.Free
        _subscriptionType.value = SubscriptionType.NONE
        activePurchaseToken = null
        clearPromoStateIfNoSubscription()
    }

    /**
     * Loop-5 fix: when BillingClient claims the user has no purchase but we
     * were just Subscribed, ask the Cloud Function (which speaks directly to
     * the Subscriptions API v2) whether the subscription truly expired. We
     * only downgrade to Free when the Cloud Function explicitly returns
     * NotFound — anything else (including transient errors) keeps the
     * previous Subscribed state.
     */
    private fun verifyExpirationWithCloud(token: String, productId: String) {
        if (!verifyExpirationInFlight.compareAndSet(false, true)) {
            Log.d(TAG, "verifyExpirationWithCloud: already running, skipping")
            return
        }
        backgroundScope.launch {
            try {
                val result = try {
                    kotlinx.coroutines.withTimeout(15_000L) {
                        subscriptionStatusService.fetchWithStatus(token, productId)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.w(TAG, "verifyExpirationWithCloud: timed out")
                    com.bestjournal.app.data.remote.FetchResult.Error
                }
                when (result) {
                    is com.bestjournal.app.data.remote.FetchResult.NotFound -> {
                        // Loop-9 fix (Frank, 2026-04-30): if a fresh purchase
                        // landed via onPurchasesUpdated WHILE this verify
                        // call was in-flight, the in-memory activePurchaseToken
                        // already points at the NEW token. The cloud verdict
                        // for the OLD token (NotFound) is therefore stale —
                        // do not downgrade. A subsequent refresh will hit the
                        // cloud with the new token and confirm Subscribed.
                        if (activePurchaseToken != null && activePurchaseToken != token) {
                            Log.d(
                                TAG,
                                "verifyExpirationWithCloud: stale NotFound for old token " +
                                    "(new token already active) — ignoring",
                            )
                            return@launch
                        }
                        Log.d(
                            TAG,
                            "verifyExpirationWithCloud: cloud confirms expired — setting Free",
                        )
                        _subscriptionState.value = SubscriptionState.Free
                        _subscriptionType.value = SubscriptionType.NONE
                        activePurchaseToken = null
                        clearPromoStateIfNoSubscription()
                        encryptedPrefs.edit()
                            .remove(Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN)
                            .remove(Constants.PREF_LAST_KNOWN_PRODUCT_ID)
                            .apply()
                    }
                    is com.bestjournal.app.data.remote.FetchResult.Found -> {
                        // Cloud says still active — BillingClient cache was wrong.
                        // Keep the Subscribed state and apply any fresh fields
                        // from the cloud response so the UI reflects truth.
                        Log.d(
                            TAG,
                            "verifyExpirationWithCloud: cloud says still active — " +
                                "keeping Subscribed",
                        )
                        val data = result.data
                        synchronized(promoSyncLock) {
                            data.autoRenewing?.let { renewing ->
                                encryptedPrefs.edit()
                                    .putBoolean(Constants.PREF_AUTO_RENEWING, renewing)
                                    .putBoolean(
                                        Constants.PREF_AUTO_RENEWING_CLOUD_SEEN,
                                        true,
                                    )
                                    .commit()
                                _autoRenewing.value = renewing
                            }
                            data.offerPhase?.let { phase ->
                                encryptedPrefs.edit()
                                    .putString(Constants.PREF_OFFER_PHASE, phase)
                                    .commit()
                                _offerPhase.value = phase
                            }
                            data.currentPriceMicros?.let { micros ->
                                encryptedPrefs.edit()
                                    .putLong(Constants.PREF_CURRENT_PRICE_MICROS, micros)
                                    .putString(
                                        Constants.PREF_CURRENT_PRICE_CURRENCY,
                                        data.currentPriceCurrency ?: "",
                                    )
                                    .commit()
                                _serverCurrentPriceMicros.value = micros
                                _serverCurrentPriceCurrency.value =
                                    data.currentPriceCurrency ?: ""
                            }
                            _expiryTime.value = data.expiryTime
                        }
                    }
                    is com.bestjournal.app.data.remote.FetchResult.Error -> {
                        Log.w(
                            TAG,
                            "verifyExpirationWithCloud: cloud unavailable — " +
                                "keeping previous state",
                        )
                    }
                }
            } finally {
                verifyExpirationInFlight.set(false)
            }
        }
    }

    /**
     * Loop-12 fix (Frank, 2026-05-01): asks the Cloud Function whether the
     * Lifetime purchase backing [token] is still PURCHASED. The BillingClient
     * cache can claim the user owns Lifetime for hours after a refund /
     * revocation — only the Products API tells the truth.
     *
     * If the Cloud Function returns NotFound, the in-memory state is set to
     * Free immediately. Transient errors keep the previous state to avoid
     * downgrading on a flaky network.
     */
    private fun verifyLifetimeWithCloud(token: String) {
        if (!verifyExpirationInFlight.compareAndSet(false, true)) {
            Log.d(TAG, "verifyLifetimeWithCloud: already running, skipping")
            return
        }
        backgroundScope.launch {
            try {
                val result = try {
                    kotlinx.coroutines.withTimeout(15_000L) {
                        subscriptionStatusService.fetchWithStatus(token, LIFETIME_PRODUCT_ID)
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    Log.w(TAG, "verifyLifetimeWithCloud: timed out")
                    com.bestjournal.app.data.remote.FetchResult.Error
                }
                when (result) {
                    is com.bestjournal.app.data.remote.FetchResult.NotFound -> {
                        Log.d(
                            TAG,
                            "verifyLifetimeWithCloud: cloud confirms Lifetime revoked / " +
                                "refunded — setting Free",
                        )
                        _subscriptionState.value = SubscriptionState.Free
                        _subscriptionType.value = SubscriptionType.NONE
                        activePurchaseToken = null
                        clearPromoStateIfNoSubscription()
                        encryptedPrefs.edit()
                            .remove(Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN)
                            .remove(Constants.PREF_LAST_KNOWN_PRODUCT_ID)
                            .apply()
                    }
                    is com.bestjournal.app.data.remote.FetchResult.Found -> {
                        Log.d(
                            TAG,
                            "verifyLifetimeWithCloud: cloud confirms Lifetime still active",
                        )
                    }
                    is com.bestjournal.app.data.remote.FetchResult.Error -> {
                        Log.w(
                            TAG,
                            "verifyLifetimeWithCloud: cloud unavailable — keeping previous state",
                        )
                    }
                }
            } finally {
                verifyExpirationInFlight.set(false)
            }
        }
    }

    private fun queryProductDetails() {
        val productList =
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(MONTHLY_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(YEARLY_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build(),
            )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (details in productDetailsList) {
                    // Get price from the MAIN base plan only — not from retention or promo plans
                    val mainBasePlanId =
                        when (details.productId) {
                            MONTHLY_PRODUCT_ID -> MONTHLY_BASE_PLAN_ID
                            YEARLY_PRODUCT_ID -> YEARLY_BASE_PLAN_ID
                            else -> null
                        }
                    val mainOffer =
                        details.subscriptionOfferDetails?.firstOrNull {
                            it.basePlanId == mainBasePlanId
                        } ?: details.subscriptionOfferDetails?.firstOrNull()
                    val price =
                        mainOffer?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                            ?: ""
                    when (details.productId) {
                        MONTHLY_PRODUCT_ID -> {
                            monthlyProductDetails = details
                            _monthlyPrice.value = price
                        }
                        YEARLY_PRODUCT_ID -> {
                            yearlyProductDetails = details
                            _yearlyPrice.value = price
                        }
                    }
                }
            }
        }
    }

    private fun queryLifetimeDetails() {
        val productList =
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(LIFETIME_PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetailsList
                    .firstOrNull { it.productId == LIFETIME_PRODUCT_ID }
                    ?.let { details ->
                        lifetimeProductDetails = details
                        _lifetimePrice.value =
                            details.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                    }
            }
        }
    }

    /**
     * Look for the "monthly-50-off-first" promotional offer attached to the monthly base plan.
     * Matches by offerId (the discount is configured as an Offer on the existing base plan,
     * not as a separate base plan). Falls back to any offer with more than one pricing phase.
     */
    fun getMonthlyPromoOfferToken(): String? {
        val details = monthlyProductDetails ?: return null
        // First: match by offerId (the discount is an Offer on the monthly base plan)
        val promoOffer =
            details.subscriptionOfferDetails?.firstOrNull { offer ->
                offer.offerId == "monthly-50-off-first"
            }
        if (promoOffer != null) return promoOffer.offerToken
        // Fallback: any offer with multiple pricing phases (intro + base)
        return details.subscriptionOfferDetails
            ?.firstOrNull { offer -> offer.pricingPhases.pricingPhaseList.size > 1 }
            ?.offerToken
    }

    /**
     * Look for a retention base plan on the given subscription (monthly or yearly). These are
     * separate base plans configured in Play Console with developer-determined visibility. Using
     * basePlanId (not offerId) because permanent discounts require a base plan, not an offer.
     */
    fun getRetentionOfferToken(isYearly: Boolean): String? {
        val details = if (isYearly) yearlyProductDetails else monthlyProductDetails
        details ?: return null
        val targetBasePlanId =
            if (isYearly) Constants.RETENTION_OFFER_ID_YEARLY
            else Constants.RETENTION_OFFER_ID_MONTHLY
        return details.subscriptionOfferDetails
            ?.firstOrNull { offer -> offer.basePlanId == targetBasePlanId }
            ?.offerToken
    }

    fun getRetentionPrice(isYearly: Boolean): String? {
        val details = if (isYearly) yearlyProductDetails else monthlyProductDetails
        details ?: return null
        val targetBasePlanId =
            if (isYearly) Constants.RETENTION_OFFER_ID_YEARLY
            else Constants.RETENTION_OFFER_ID_MONTHLY
        return details.subscriptionOfferDetails
            ?.firstOrNull { offer -> offer.basePlanId == targetBasePlanId }
            ?.pricingPhases
            ?.pricingPhaseList
            ?.firstOrNull()
            ?.formattedPrice
    }

    fun restorePurchases() {
        querySubscriptions()
        queryInAppPurchases()
    }

    fun launchPurchaseFlow(
        activity: Activity,
        isYearly: Boolean = false,
        isLifetime: Boolean = false,
        promoOfferToken: String? = null,
    ) {
        // Prevent duplicate purchase dialogs from double-tap
        if (!isPurchaseInFlight.compareAndSet(false, true)) {
            Log.d(TAG, "Purchase already in flight — ignoring duplicate tap")
            return
        }

        if (isLifetime) {
            val details = lifetimeProductDetails
            if (details == null) {
                isPurchaseInFlight.set(false)
                return
            }
            val productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            val billingFlowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()
            val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
            // If billingClient was null OR the flow failed synchronously, onPurchasesUpdated
            // never fires, so we must release isPurchaseInFlight here.
            if (result == null ||
                result.responseCode != BillingClient.BillingResponseCode.OK
            ) {
                Log.w(
                    TAG,
                    "launchBillingFlow (lifetime) failed: ${result?.responseCode} ${result?.debugMessage}",
                )
                isPurchaseInFlight.set(false)
            }
            return
        }
        val productDetails = if (isYearly) yearlyProductDetails else monthlyProductDetails
        if (productDetails == null) {
            isPurchaseInFlight.set(false)
            return
        }
        // Diagnostic: log all available offers for debugging promo display issues
        Log.d(TAG, "=== launchPurchaseFlow: isYearly=$isYearly, promoOfferToken=${if (promoOfferToken != null) "PROVIDED" else "null"} ===")
        productDetails.subscriptionOfferDetails?.forEachIndexed { idx, offer ->
            val phases = offer.pricingPhases.pricingPhaseList.joinToString("; ") { phase ->
                "${phase.formattedPrice}/${phase.billingPeriod} (cycles=${phase.billingCycleCount}, recurMode=${phase.recurrenceMode})"
            }
            Log.d(TAG, "  [$idx] basePlanId='${offer.basePlanId}' offerId='${offer.offerId}' tokenSuffix='${offer.offerToken.takeLast(8)}' phases=[$phases]")
        }
        // Use promo token if provided, otherwise find the BARE base plan (no offer attached)
        // so the Google Play sheet shows ONLY the regular recurring price, not intro + recurring
        val mainBasePlanId = if (isYearly) YEARLY_BASE_PLAN_ID else MONTHLY_BASE_PLAN_ID
        val offerToken =
            promoOfferToken
                ?: productDetails.subscriptionOfferDetails
                    ?.firstOrNull { it.basePlanId == mainBasePlanId && it.offerId.isNullOrEmpty() }
                    ?.offerToken
                ?: productDetails.subscriptionOfferDetails
                    ?.firstOrNull { it.basePlanId == mainBasePlanId }
                    ?.offerToken
                ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "=== NO OFFER TOKEN FOUND — purchase aborted ===")
            isPurchaseInFlight.set(false)
            return
        }
        Log.d(TAG, "=== USING offerToken suffix='${offerToken.takeLast(8)}' ===")
        // Remember which base plan + offer the user is buying so the Settings
        // overview can show the correct active price (not just the main plan price)
        rememberActiveBasePlan(productDetails, offerToken)
        // Also stamp the chosen base plan into Google's obfuscatedAccountId field so it
        // survives app re-install: querySubscriptions can recover it from purchase.accountIdentifiers.
        val matchedOfferForStamp =
            productDetails.subscriptionOfferDetails?.firstOrNull { it.offerToken == offerToken }
        val basePlanStamp = matchedOfferForStamp?.let {
            "bp:${it.basePlanId}|of:${it.offerId ?: ""}".take(64)
        }
        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        val billingFlowParamsBuilder =
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productDetailsParams))

        // Loop-7 (Frank, 2026-04-30): the legacy code reused
        // obfuscatedAccountId as a "which base-plan did the user pick"
        // marker. That works on the FIRST purchase but Google rejects
        // every later subscription update with responseCode=5
        // "Account identifiers don't match the previous subscription."
        // because Play treats obfuscatedAccountId as a stable user id —
        // it must be identical across the entire lifetime of the
        // subscription. We therefore only stamp the base-plan info on
        // the FIRST purchase (no oldToken yet) and leave the field
        // untouched on every retention/plan switch.
        val isSubscriptionUpdate =
            activePurchaseToken != null &&
                _subscriptionState.value is SubscriptionState.Subscribed
        if (basePlanStamp != null && !isSubscriptionUpdate) {
            billingFlowParamsBuilder.setObfuscatedAccountId(basePlanStamp)
        }

        // If user already has an active subscription, add update params
        // to allow offer changes (retention, plan switch) without ITEM_ALREADY_OWNED.
        //
        // Loop-8 final fix (Frank, 2026-04-30): four prior modes failed —
        // the diagnostic logging in #1937 made the truth visible.
        //   - WITH_TIME_PRORATION  → "replacement mode not supported"
        //   - DEFERRED             → "replacement mode not supported"
        //   - CHARGE_PRORATED_PRICE → upgrades only
        //   - CHARGE_FULL_PRICE    → would charge user 2,99 € on top of
        //                            already-paid 3,99 € (bad UX for retention)
        //   - WITHOUT_PRORATION    → ✅ documented to work for cross-base-plan
        //                            switches and is the user-friendly choice
        //                            for downgrades: the cheaper plan kicks
        //                            in at the next renewal date, no extra
        //                            charge today, and the user keeps their
        //                            current paid period in full.
        // Confirmed against the official Subscriptions API v2 reference
        // and the react-native-iap GitHub issue #2729 which describes the
        // exact same DEVELOPER_ERROR=5 "replacement mode not supported"
        // for the disallowed modes.
        val oldToken = activePurchaseToken
        if (oldToken != null && _subscriptionState.value is SubscriptionState.Subscribed) {
            billingFlowParamsBuilder.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(oldToken)
                    .setSubscriptionReplacementMode(
                        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
                            .WITHOUT_PRORATION
                    )
                    .build()
            )
        }

        val result = billingClient?.launchBillingFlow(activity, billingFlowParamsBuilder.build())
        // Same guard as the lifetime branch: if billingClient is null OR the
        // flow returned a non-OK response synchronously, onPurchasesUpdated
        // will never fire, so we must release isPurchaseInFlight here.
        if (result == null ||
            result.responseCode != BillingClient.BillingResponseCode.OK
        ) {
            Log.w(
                TAG,
                "launchBillingFlow (subs) failed: ${result?.responseCode} ${result?.debugMessage}",
            )
            isPurchaseInFlight.set(false)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        // Always release the in-flight guard regardless of outcome (success, error, or cancel)
        isPurchaseInFlight.set(false)

        // Loop-7 (Frank, 2026-04-30): full diagnostics on every purchase
        // callback so a Google rejection like "Abo kann nicht geaendert
        // werden" leaves a breadcrumb in logcat instead of disappearing
        // silently. Helpful when the Play sheet error is unspecific.
        Log.d(
            TAG,
            "onPurchasesUpdated: responseCode=${billingResult.responseCode}, " +
                "debugMessage='${billingResult.debugMessage}', " +
                "purchases=${purchases?.size ?: 0}",
        )

        if (
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null
        ) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Loop-9 fix (Frank, 2026-04-30): WITHOUT_PRORATION
                    // retention switches caused a brief "Premium
                    // freischalten" flash because the OLD purchase token
                    // was still in PREF_LAST_KNOWN_PURCHASE_TOKEN when a
                    // parallel ON_RESUME refresh fired finalizePurchase-
                    // QueryBatch -> verifyExpirationWithCloud. The cloud
                    // (correctly) reported the OLD token as not-found and
                    // the app downgraded to Free seconds before the NEW
                    // token landed in the BillingClient cache.
                    //
                    // Fix: optimistically set Subscribed + persist the
                    // new token RIGHT NOW, before acknowledge. The acknowledge
                    // callback will set the same fields again (idempotent),
                    // but more importantly the verify-before-downgrade path
                    // will now read the NEW token — Google will report
                    // ACTIVE — and the brief Free flash is gone.
                    _subscriptionState.value = SubscriptionState.Subscribed
                    activePurchaseToken = purchase.purchaseToken
                    encryptedPrefs.edit()
                        .putString(
                            Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN,
                            purchase.purchaseToken,
                        )
                        .putString(
                            Constants.PREF_LAST_KNOWN_PRODUCT_ID,
                            purchase.products.firstOrNull() ?: "",
                        )
                        .commit()

                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    } else {
                        updateSubscriptionType(purchase)
                    }
                }
            }
        }
    }

    private fun updateSubscriptionType(purchase: Purchase) {
        val isLifetime = purchase.products.contains(LIFETIME_PRODUCT_ID)
        // Only store purchase token for subscriptions — lifetime (INAPP) tokens
        // cannot be used for subscription update params
        if (!isLifetime) {
            activePurchaseToken = purchase.purchaseToken
        } else {
            activePurchaseToken = null
        }
        // Loop-5 fix: also persist last-known token+productId so the verify-
        // before-downgrade path in finalizePurchaseQueryBatch can ask the
        // Cloud Function whether a later "BillingClient empty" is real.
        encryptedPrefs.edit()
            .putString(Constants.PREF_LAST_KNOWN_PURCHASE_TOKEN, purchase.purchaseToken)
            .putString(
                Constants.PREF_LAST_KNOWN_PRODUCT_ID,
                purchase.products.firstOrNull() ?: "",
            )
            .apply()
        _subscriptionType.value =
            when {
                isLifetime -> SubscriptionType.LIFETIME
                purchase.products.contains(YEARLY_PRODUCT_ID) -> SubscriptionType.YEARLY
                purchase.products.contains(MONTHLY_PRODUCT_ID) -> SubscriptionType.MONTHLY
                else -> _subscriptionType.value
            }
        // Loop-7: subscription state (price, phase, expiry, autoRenewing) is
        // pulled directly from Google via the cloud function in
        // syncSubscriptionStatusFromCloud — no local promo bookkeeping here.
    }

    // K-2 fix: Centralized acknowledge — only sets Subscribed after Google confirms
    // Retry up to 3 times on transient failures
    private fun acknowledgePurchase(purchase: Purchase, retryCount: Int = 0) {
        val ackParams =
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient?.acknowledgePurchase(ackParams) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _subscriptionState.value = SubscriptionState.Subscribed
                updateSubscriptionType(purchase)
                val isLifetime = purchase.products.contains(LIFETIME_PRODUCT_ID)
                val isYearly = purchase.products.contains(YEARLY_PRODUCT_ID)
                if (isLifetime) {
                    val offer = lifetimeProductDetails?.oneTimePurchaseOfferDetails
                    analyticsTracker.trackLifetimePurchased(
                        value = (offer?.priceAmountMicros ?: 0L) / 1_000_000.0,
                        currency = offer?.priceCurrencyCode ?: "EUR",
                    )
                } else {
                    val type =
                        when {
                            isYearly -> "yearly"
                            purchase.products.contains(MONTHLY_PRODUCT_ID) -> "monthly"
                            else -> "unknown"
                        }
                    val details = if (isYearly) yearlyProductDetails else monthlyProductDetails
                    val mainPlanId = if (isYearly) YEARLY_BASE_PLAN_ID else MONTHLY_BASE_PLAN_ID
                    val pricingPhase =
                        (details?.subscriptionOfferDetails?.firstOrNull {
                                it.basePlanId == mainPlanId
                            } ?: details?.subscriptionOfferDetails?.firstOrNull())
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.firstOrNull()
                    val valueMicros = pricingPhase?.priceAmountMicros ?: 0L
                    val currency = pricingPhase?.priceCurrencyCode ?: "EUR"
                    analyticsTracker.trackSubscriptionPurchased(
                        type = type,
                        value = valueMicros / 1_000_000.0,
                        currency = currency,
                    )
                }
            } else if (retryCount < 3) {
                Log.w(
                    TAG,
                    "Acknowledge failed (attempt ${retryCount + 1}/3): ${result.debugMessage}",
                )
                android.os
                    .Handler(android.os.Looper.getMainLooper())
                    .postDelayed(
                        { acknowledgePurchase(purchase, retryCount + 1) },
                        2000L * (retryCount + 1),
                    )
            } else {
                // All 3 retries exhausted. Persist the token (commit, synchronous)
                // so the next BillingClient connection retries — otherwise Google
                // auto-refunds after 3 days because the purchase never got
                // acknowledged. Stops the silent money-loss bug (EC-2).
                Log.e(
                    TAG,
                    "Acknowledge failed after 3 retries — persisting for next session: ${result.debugMessage}",
                )
                encryptedPrefs.edit()
                    .putString(Constants.PREF_PENDING_ACK_TOKEN, purchase.purchaseToken)
                    .commit()
            }
        }
    }

    fun destroy() {
        // Cancel any in-flight Cloud Function calls so they cannot write to
        // SharedPreferences after the BillingManager is no longer in use.
        backgroundScope.cancel()
        billingClient?.endConnection()
        billingClient = null
    }
}
