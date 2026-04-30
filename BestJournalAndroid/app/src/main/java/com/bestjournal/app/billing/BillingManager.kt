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
        // The single promo offer id that grants the 50%-off-for-N-months discount.
        // When the active subscription's offerId matches this, the promo counter
        // is meaningful; otherwise the counter is stale and gets cleared.
        private const val PROMO_OFFER_ID = "monthly-50-off-first"
        // Cloud Function call cache window. The truthful basePlanId/offerId can
        // only change on renewal or upgrade events, so refreshing once an hour
        // is plenty even for very active users — keeps free-tier usage minimal.
        private const val CLOUD_STATUS_CACHE_MS = 60L * 60L * 1000L
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

    // Marker that the next successful purchase confirmation should initialize
    // PREF_PROMO_TOTAL_MONTHS. Set by launchPurchaseFlow ONLY when the offer
    // token belongs to the actual 50%-off-first promo (not retention or main).
    // AtomicReference avoids the read-modify-write hazard of @Volatile var.
    private val pendingPromoIntent = java.util.concurrent.atomic.AtomicBoolean(false)

    // Serialises promo-counter writes between syncPromoRenewal (purchaseTime
    // path) and syncPromoFromCloudExpiry (cloud path) — both can fire from
    // different threads against the same SharedPreferences keys.
    private val promoSyncLock = Any()

    // Reactive remaining-promo-months counter. Mirrors PREF_PROMO_TOTAL_MONTHS
    // but is observable from Compose (collectAsStateWithLifecycle) so the UI
    // re-renders the moment the promo is decremented or cleared. SettingsViewModel
    // exposes this via getActivePromoInfo as a StateFlow.
    private val _promoTotalMonths = MutableStateFlow(
        encryptedPrefs.getInt(Constants.PREF_PROMO_TOTAL_MONTHS, 0)
    )
    val promoTotalMonths: StateFlow<Int> = _promoTotalMonths.asStateFlow()

    // Guard against duplicate purchase flows triggered by double-tap
    private val isPurchaseInFlight = AtomicBoolean(false)

    private val connectionListener =
        object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(
                    TAG,
                    "onBillingSetupFinished: responseCode=${billingResult.responseCode}",
                )
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // 1. Lifetime check FIRST so subscription queries cannot
                    //    transiently flip the state to Free before lifetime is
                    //    detected (race condition fix).
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
            Log.d(TAG, "refreshSubscriptionStatus: billingClient not ready, skipping")
            return
        }
        Log.d(TAG, "refreshSubscriptionStatus: triggering full re-query")
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
                    // Detect renewals via local purchaseTime (legacy fallback) and
                    // clean up stale promo state on plan switches.
                    syncPromoRenewal(activePurchase)
                    // Recover active base plan from Google's stamp if local state is missing
                    // (covers app re-install or upgrade from pre-v0.18.2 versions)
                    recoverActiveBasePlanFromStamp(activePurchase)
                    // Final fallback for legacy purchases without stamp: ask the
                    // server-side Cloud Function which calls Subscriptions API v2.
                    maybeRefreshActiveBasePlanFromCloud(activePurchase)
                    // PRIMARY renewal detection: ask the Cloud Function for the
                    // current expiryTime (server-authoritative, unlike the
                    // BillingClient cache which keeps the original purchaseTime).
                    syncPromoFromCloudExpiry(activePurchase)
                } else if (_subscriptionType.value != SubscriptionType.LIFETIME) {
                    // Only set Free if no lifetime purchase was already detected
                    // (prevents race condition between querySubscriptions and queryInAppPurchases)
                    _subscriptionState.value = SubscriptionState.Free
                    clearPromoStateIfNoSubscription()
                }

                // K-2 fix: Acknowledge unacknowledged purchases — only set Subscribed AFTER success
                purchases
                    .filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
                    }
                    .forEach { purchase -> acknowledgePurchase(purchase) }
            }
        }
    }

    /**
     * Compares the current Purchase.purchaseTime with the last observed value.
     * When Google Play renews the subscription, purchaseTime updates — so each
     * change is exactly one renewal cycle. Decrements the stored remaining promo
     * months accordingly. Works identically in 5-min test cycles and 28-31 day
     * production cycles because we don't compute elapsed time, we react to
     * Google's authoritative subscription state.
     */
    private fun syncPromoRenewal(purchase: Purchase): Unit = synchronized(promoSyncLock) {
        Log.d(
            TAG,
            "syncPromoRenewal: ENTER products=${purchase.products}, " +
                "purchaseTime=${purchase.purchaseTime}",
        )
        // Cleanup-Phase 1: Promo only applies to monthly. If the user has switched
        // to yearly (or any other product) but the counter is still positive,
        // clear it so the leftover promo state cannot resurface later.
        if (!purchase.products.contains(MONTHLY_PRODUCT_ID)) {
            val total = encryptedPrefs.getInt(Constants.PREF_PROMO_TOTAL_MONTHS, 0)
            if (total > 0) {
                Log.d(TAG, "syncPromoRenewal: not monthly, clearing stale promo counter")
                clearPromoCounter()
            } else {
                Log.d(TAG, "syncPromoRenewal: SKIP not a monthly subscription")
            }
            return
        }
        // Cleanup-Phase 2: Counter only valid for the actual promo offer. If the
        // user is on a different offer (regular monthly or retention) but the
        // counter is still set, we treat that as stale state from a previous
        // subscription cycle and clear it.
        val activeOfferId = encryptedPrefs.getString(Constants.PREF_ACTIVE_OFFER_ID, "") ?: ""
        val totalMonths = encryptedPrefs.getInt(Constants.PREF_PROMO_TOTAL_MONTHS, 0)
        Log.d(
            TAG,
            "syncPromoRenewal: totalMonths=$totalMonths, activeOfferId='$activeOfferId'",
        )
        if (totalMonths > 0 &&
            activeOfferId.isNotBlank() &&
            activeOfferId != PROMO_OFFER_ID
        ) {
            Log.d(
                TAG,
                "syncPromoRenewal: active offer is '$activeOfferId' (not promo), clearing counter",
            )
            clearPromoCounter()
            return
        }
        if (totalMonths <= 0) {
            Log.d(TAG, "syncPromoRenewal: SKIP totalMonths<=0 (no active promo)")
            return
        }

        val currentPt = purchase.purchaseTime
        val lastPt = encryptedPrefs.getLong(Constants.PREF_PROMO_LAST_PURCHASE_TIME, 0L)
        Log.d(TAG, "syncPromoRenewal: currentPt=$currentPt, lastPt=$lastPt")

        // First sighting after purchase: anchor without decrementing
        if (lastPt == 0L) {
            Log.d(TAG, "syncPromoRenewal: anchor first sighting (no decrement)")
            encryptedPrefs.edit()
                .putLong(Constants.PREF_PROMO_LAST_PURCHASE_TIME, currentPt)
                .commit()
            return
        }

        // Renewal detected — Google bumped the purchaseTime (rare with v7 client
        // for auto-renewals, but kept as fallback for plan switches and other
        // events where purchaseTime DOES change).
        if (currentPt != lastPt) {
            val newRemaining = (totalMonths - 1).coerceAtLeast(0)
            // commit() (synchronous) instead of apply() so a process kill in the
            // next ~100ms cannot lose the decrement.
            encryptedPrefs.edit()
                .putInt(Constants.PREF_PROMO_TOTAL_MONTHS, newRemaining)
                .putLong(Constants.PREF_PROMO_LAST_PURCHASE_TIME, currentPt)
                .commit()
            _promoTotalMonths.value = newRemaining
            Log.d(
                TAG,
                "syncPromoRenewal: RENEWAL DETECTED via purchaseTime " +
                    "$totalMonths -> $newRemaining months remaining",
            )
        } else {
            Log.d(TAG, "syncPromoRenewal: no purchaseTime change")
        }
    }

    /**
     * Clears the promo counter and all related state (synchronously — commit()).
     * Used when the active subscription is no longer eligible for the promo
     * (e.g. switched to yearly, switched to retention plan, family-shared).
     */
    private fun clearPromoCounter(): Unit = synchronized(promoSyncLock) {
        encryptedPrefs.edit()
            .putInt(Constants.PREF_PROMO_TOTAL_MONTHS, 0)
            .remove(Constants.PREF_PROMO_LAST_PURCHASE_TIME)
            .remove(Constants.PREF_LAST_KNOWN_EXPIRY)
            .commit()
        _promoTotalMonths.value = 0
    }

    /**
     * Authoritative renewal detection via Cloud Function. Calls Google Play
     * Subscriptions API v2 server-side which returns the CURRENT expiryTime
     * (not stale like BillingClient.purchase.purchaseTime). Compares against
     * PREF_LAST_KNOWN_EXPIRY — when it advances, a renewal happened and the
     * promo counter is decremented.
     *
     * Throttled to one call per CLOUD_STATUS_CACHE_MS window so the function
     * stays in the Firebase free tier even with frequent Settings opens.
     */
    private fun syncPromoFromCloudExpiry(purchase: Purchase) {
        if (!purchase.products.contains(MONTHLY_PRODUCT_ID)) return
        if (_promoTotalMonths.value <= 0) return

        val now = System.currentTimeMillis()
        val lastFetch = encryptedPrefs.getLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, 0L)
        if (now - lastFetch < CLOUD_STATUS_CACHE_MS) {
            Log.d(TAG, "syncPromoFromCloudExpiry: skipped (within cache window)")
            return
        }

        val productId = purchase.products.firstOrNull() ?: return
        val token = purchase.purchaseToken

        backgroundScope.launch {
            Log.d(TAG, "syncPromoFromCloudExpiry: calling cloud function")
            val result = try {
                kotlinx.coroutines.withTimeout(15_000L) {
                    subscriptionStatusService.fetch(token, productId)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "syncPromoFromCloudExpiry: cloud call timed out")
                null
            }
            // Apply the result under the same lock as syncPromoRenewal so the
            // two paths cannot race against each other on the same prefs keys.
            synchronized(promoSyncLock) {
                if (result?.expiryTime.isNullOrBlank()) {
                    Log.d(TAG, "syncPromoFromCloudExpiry: no expiryTime returned")
                    encryptedPrefs.edit()
                        .putLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, now)
                        .commit()
                    return@synchronized
                }
                val newExpiry = result?.expiryTime
                val lastExpiry =
                    encryptedPrefs.getString(Constants.PREF_LAST_KNOWN_EXPIRY, null)
                Log.d(
                    TAG,
                    "syncPromoFromCloudExpiry: newExpiry=$newExpiry, lastExpiry=$lastExpiry",
                )

                if (lastExpiry.isNullOrBlank()) {
                    // First sighting via cloud — anchor without decrementing.
                    encryptedPrefs.edit()
                        .putString(Constants.PREF_LAST_KNOWN_EXPIRY, newExpiry)
                        .putLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, now)
                        .commit()
                    Log.d(TAG, "syncPromoFromCloudExpiry: anchored first sighting")
                    return@synchronized
                }

                if (newExpiry == lastExpiry) {
                    Log.d(TAG, "syncPromoFromCloudExpiry: no expiry change")
                    encryptedPrefs.edit()
                        .putLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, now)
                        .commit()
                    return@synchronized
                }

                // Renewal detected — expiryTime moved forward
                val total = encryptedPrefs.getInt(Constants.PREF_PROMO_TOTAL_MONTHS, 0)
                val newRemaining = (total - 1).coerceAtLeast(0)
                encryptedPrefs.edit()
                    .putInt(Constants.PREF_PROMO_TOTAL_MONTHS, newRemaining)
                    .putString(Constants.PREF_LAST_KNOWN_EXPIRY, newExpiry)
                    .putLong(Constants.PREF_LAST_PROMO_CLOUD_FETCH, now)
                    .commit()
                _promoTotalMonths.value = newRemaining
                Log.d(
                    TAG,
                    "syncPromoFromCloudExpiry: RENEWAL DETECTED via cloud " +
                        "$total -> $newRemaining months remaining",
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
            .commit()
        _promoTotalMonths.value = 0
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
                val hasLifetime = purchases.any { purchase ->
                    purchase.products.contains(LIFETIME_PRODUCT_ID) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.isAcknowledged
                }
                if (hasLifetime) {
                    _subscriptionState.value = SubscriptionState.Subscribed
                    _subscriptionType.value = SubscriptionType.LIFETIME
                }
                purchases
                    .filter {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
                    }
                    .forEach { purchase -> acknowledgePurchase(purchase) }
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
        // Remember whether this is a promo purchase so onPurchasesUpdated can
        // initialize PREF_PROMO_TOTAL_MONTHS regardless of which screen launched
        // the flow (Paywall, ChurnFlowDialog, or anywhere else).
        // Mark only if the offer is the genuine 50%-off-first promo offer.
        // Retention offers also pass a non-null promoOfferToken but are NOT
        // promos — they get their own counter rules (currently none).
        val truePromoToken = getMonthlyPromoOfferToken()
        pendingPromoIntent.set(promoOfferToken != null && promoOfferToken == truePromoToken)

        if (isLifetime) {
            val details = lifetimeProductDetails
            if (details == null) {
                isPurchaseInFlight.set(false)
                pendingPromoIntent.set(false)
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
                pendingPromoIntent.set(false)
            }
            return
        }
        val productDetails = if (isYearly) yearlyProductDetails else monthlyProductDetails
        if (productDetails == null) {
            isPurchaseInFlight.set(false)
            pendingPromoIntent.set(false)
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
            pendingPromoIntent.set(false)
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
        if (basePlanStamp != null) {
            billingFlowParamsBuilder.setObfuscatedAccountId(basePlanStamp)
        }

        // If user already has an active subscription, add update params
        // to allow offer changes (retention, plan switch) without ITEM_ALREADY_OWNED
        val oldToken = activePurchaseToken
        if (oldToken != null && _subscriptionState.value is SubscriptionState.Subscribed) {
            billingFlowParamsBuilder.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(oldToken)
                    .setSubscriptionReplacementMode(
                        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION
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
            pendingPromoIntent.set(false)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        // Always release the in-flight guard regardless of outcome (success, error, or cancel)
        isPurchaseInFlight.set(false)

        if (
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null
        ) {
            // Read pending intent ONCE before the loop so multiple acknowledged
            // purchases in the same callback do not eat each other's intent.
            val isPromoPurchase = pendingPromoIntent.getAndSet(false)
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (isPromoPurchase &&
                        purchase.products.contains(MONTHLY_PRODUCT_ID)
                    ) {
                        // Bug A fix: initialize the promo counter regardless of
                        // which screen launched the flow (Paywall, ChurnFlowDialog).
                        // Uses setPromoTotalMonths so the StateFlow stays in sync.
                        setPromoTotalMonths(Constants.EXIT_INTENT_DISCOUNT_MONTHS)
                        encryptedPrefs.edit()
                            .putLong(
                                Constants.PREF_PROMO_PURCHASE_TIME,
                                System.currentTimeMillis(),
                            )
                            .remove(Constants.PREF_PROMO_LAST_PURCHASE_TIME)
                            .remove(Constants.PREF_LAST_KNOWN_EXPIRY)
                            .commit()
                        Log.d(
                            TAG,
                            "Promo purchase confirmed — counter set to " +
                                Constants.EXIT_INTENT_DISCOUNT_MONTHS,
                        )
                    }

                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    } else {
                        _subscriptionState.value = SubscriptionState.Subscribed
                        updateSubscriptionType(purchase)
                    }
                }
            }
        } else {
            // Non-OK result OR null purchases (cancel, error). Discard any pending
            // promo intent so a later non-promo purchase does not get the counter.
            pendingPromoIntent.set(false)
        }
    }

    /**
     * Public setter for PREF_PROMO_TOTAL_MONTHS that ALSO updates the reactive
     * StateFlow. Use this from anywhere (BillingManager, PaywallViewModel) so
     * the UI stays in sync. Synchronous commit() so a process kill cannot lose
     * the value.
     */
    fun setPromoTotalMonths(months: Int) {
        synchronized(promoSyncLock) {
            encryptedPrefs.edit()
                .putInt(Constants.PREF_PROMO_TOTAL_MONTHS, months)
                .commit()
            _promoTotalMonths.value = months
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
        _subscriptionType.value =
            when {
                isLifetime -> SubscriptionType.LIFETIME
                purchase.products.contains(YEARLY_PRODUCT_ID) -> SubscriptionType.YEARLY
                purchase.products.contains(MONTHLY_PRODUCT_ID) -> SubscriptionType.MONTHLY
                else -> _subscriptionType.value
            }
        // Detect promo renewals (or anchor on initial purchase) for monthly subs
        syncPromoRenewal(purchase)
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
