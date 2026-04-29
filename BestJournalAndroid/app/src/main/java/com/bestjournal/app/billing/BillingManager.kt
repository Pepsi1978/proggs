package com.bestjournal.app.billing

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.billingclient.api.*
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class BillingManager
@Inject
constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val encryptedPrefs: SharedPreferences,
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val MONTHLY_PRODUCT_ID = "bestjournal_ai_monthly"
        const val YEARLY_PRODUCT_ID = "bestjournal_ai_yearly"
        const val LIFETIME_PRODUCT_ID = "bestjournal_lifetime"
        // Base plan IDs for the main (non-retention, non-promo) plans
        private const val MONTHLY_BASE_PLAN_ID = "monthly"
        private const val YEARLY_BASE_PLAN_ID = "yearly"
    }

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

    // Guard against duplicate purchase flows triggered by double-tap
    private val isPurchaseInFlight = AtomicBoolean(false)

    private val connectionListener =
        object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscriptions()
                    queryInAppPurchases()
                    queryProductDetails()
                    queryLifetimeDetails()
                }
            }

            // K-1 fix: Reconnect when billing service disconnects
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected — attempting reconnect")
                billingClient?.startConnection(this)
            }
        }

    fun initialize(context: Context) {
        billingClient =
            BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()

        billingClient?.startConnection(connectionListener)
    }

    private fun querySubscriptions() {
        val params =
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val activePurchase = purchases.firstOrNull { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.isAcknowledged
                }
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
                    // Detect renewals on monthly subscription so promo countdown
                    // stays in sync with Google Play, regardless of test cycles
                    syncPromoRenewal(activePurchase)
                    // Recover active base plan from Google's stamp if local state is missing
                    // (covers app re-install or upgrade from pre-v0.18.2 versions)
                    recoverActiveBasePlanFromStamp(activePurchase)
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
    private fun syncPromoRenewal(purchase: Purchase) {
        if (!purchase.products.contains(MONTHLY_PRODUCT_ID)) return
        val totalMonths = encryptedPrefs.getInt(Constants.PREF_PROMO_TOTAL_MONTHS, 0)
        if (totalMonths <= 0) return

        val currentPt = purchase.purchaseTime
        val lastPt = encryptedPrefs.getLong(Constants.PREF_PROMO_LAST_PURCHASE_TIME, 0L)

        // First sighting after purchase: anchor without decrementing
        if (lastPt == 0L) {
            encryptedPrefs.edit()
                .putLong(Constants.PREF_PROMO_LAST_PURCHASE_TIME, currentPt)
                .apply()
            return
        }

        // Renewal detected — Google bumped the purchaseTime
        if (currentPt != lastPt) {
            val newRemaining = (totalMonths - 1).coerceAtLeast(0)
            encryptedPrefs.edit()
                .putInt(Constants.PREF_PROMO_TOTAL_MONTHS, newRemaining)
                .putLong(Constants.PREF_PROMO_LAST_PURCHASE_TIME, currentPt)
                .apply()
            Log.d(TAG, "Promo renewal detected: $totalMonths -> $newRemaining months remaining")
        }
    }

    /** Clears stale promo + active-plan state if the subscription is no longer active. */
    private fun clearPromoStateIfNoSubscription() {
        encryptedPrefs.edit()
            .putInt(Constants.PREF_PROMO_TOTAL_MONTHS, 0)
            .remove(Constants.PREF_PROMO_LAST_PURCHASE_TIME)
            .remove(Constants.PREF_ACTIVE_BASE_PLAN_ID)
            .remove(Constants.PREF_ACTIVE_OFFER_ID)
            .apply()
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
        return offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice
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
            val launched = billingClient?.launchBillingFlow(activity, billingFlowParams)
            // If billingClient was null the SafeCall returned null — onPurchasesUpdated will
            // never fire, so release the in-flight guard here to avoid a permanent lockout.
            if (launched == null) isPurchaseInFlight.set(false)
            return
        }
        val productDetails = if (isYearly) yearlyProductDetails else monthlyProductDetails
        if (productDetails == null) {
            isPurchaseInFlight.set(false)
            return
        }
        // Diagnostic: log all available offers for debugging promo display issues
        Log.e(TAG, "=== launchPurchaseFlow: isYearly=$isYearly, promoOfferToken=${if (promoOfferToken != null) "PROVIDED" else "null"} ===")
        productDetails.subscriptionOfferDetails?.forEachIndexed { idx, offer ->
            val phases = offer.pricingPhases.pricingPhaseList.joinToString("; ") { phase ->
                "${phase.formattedPrice}/${phase.billingPeriod} (cycles=${phase.billingCycleCount}, recurMode=${phase.recurrenceMode})"
            }
            Log.e(TAG, "  [$idx] basePlanId='${offer.basePlanId}' offerId='${offer.offerId}' tokenSuffix='${offer.offerToken.takeLast(8)}' phases=[$phases]")
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
        Log.e(TAG, "=== USING offerToken suffix='${offerToken.takeLast(8)}' ===")
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

        val launched = billingClient?.launchBillingFlow(activity, billingFlowParamsBuilder.build())
        // Same guard as the lifetime branch: if billingClient is null, onPurchasesUpdated
        // never fires, so we must release isPurchaseInFlight here.
        if (launched == null) isPurchaseInFlight.set(false)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        // Always release the in-flight guard regardless of outcome (success, error, or cancel)
        isPurchaseInFlight.set(false)

        if (
            billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null
        ) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    } else {
                        _subscriptionState.value = SubscriptionState.Subscribed
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
                Log.e(TAG, "Acknowledge failed after 3 retries: ${result.debugMessage}")
            }
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
