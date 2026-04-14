package com.bestjournal.app.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.bestjournal.app.util.AnalyticsTracker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class BillingManager @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"
        const val MONTHLY_PRODUCT_ID = "bestjournal_ai_monthly"
        const val YEARLY_PRODUCT_ID = "bestjournal_ai_yearly"
        const val LIFETIME_PRODUCT_ID = "bestjournal_lifetime"
    }

    private var billingClient: BillingClient? = null
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

    private var monthlyProductDetails: ProductDetails? = null
    private var yearlyProductDetails: ProductDetails? = null
    private var lifetimeProductDetails: ProductDetails? = null

    // Store active purchase token for subscription updates (retention offers)
    private var activePurchaseToken: String? = null

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
                    _subscriptionType.value = when {
                        activePurchase.products.contains(YEARLY_PRODUCT_ID) -> SubscriptionType.YEARLY
                        activePurchase.products.contains(MONTHLY_PRODUCT_ID) -> SubscriptionType.MONTHLY
                        else -> SubscriptionType.MONTHLY
                    }
                } else {
                    _subscriptionState.value = SubscriptionState.Free
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

    private fun queryInAppPurchases() {
        val params =
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
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
                    val price =
                        details.subscriptionOfferDetails
                            ?.firstOrNull()
                            ?.pricingPhases
                            ?.pricingPhaseList
                            ?.firstOrNull()
                            ?.formattedPrice ?: ""
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
                    .build(),
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
     * Look for a promotional/introductory offer on the monthly plan.
     * First checks for a dedicated "monthly-50-off-first" base plan,
     * then falls back to looking for offers with multiple pricing phases (intro + base).
     * Returns the offerToken if found, null otherwise.
     */
    fun getMonthlyPromoOfferToken(): String? {
        val details = monthlyProductDetails ?: return null
        // First: look for dedicated 50%-off base plan by ID
        val promoBasePlan = details.subscriptionOfferDetails
            ?.firstOrNull { offer -> offer.basePlanId == "monthly-50-off-first" }
        if (promoBasePlan != null) return promoBasePlan.offerToken
        // Fallback: look for offers with more than 1 pricing phase (intro + base)
        return details.subscriptionOfferDetails
            ?.firstOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.size > 1
            }?.offerToken
    }

    /**
     * Look for a retention base plan on the given subscription (monthly or yearly).
     * These are separate base plans configured in Play Console with developer-determined visibility.
     * Using basePlanId (not offerId) because permanent discounts require a base plan, not an offer.
     */
    fun getRetentionOfferToken(isYearly: Boolean): String? {
        val details = if (isYearly) yearlyProductDetails else monthlyProductDetails
        details ?: return null
        val targetBasePlanId = if (isYearly) "retention-yearly-75" else "retention-monthly-75"
        return details.subscriptionOfferDetails
            ?.firstOrNull { offer -> offer.basePlanId == targetBasePlanId }
            ?.offerToken
    }

    fun getRetentionPrice(isYearly: Boolean): String? {
        val details = if (isYearly) yearlyProductDetails else monthlyProductDetails
        details ?: return null
        val targetBasePlanId = if (isYearly) "retention-yearly-75" else "retention-monthly-75"
        return details.subscriptionOfferDetails
            ?.firstOrNull { offer -> offer.basePlanId == targetBasePlanId }
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
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
        if (isLifetime) {
            val details = lifetimeProductDetails ?: return
            val productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(details)
                    .build()
            val billingFlowParams =
                BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productDetailsParams))
                    .build()
            billingClient?.launchBillingFlow(activity, billingFlowParams)
            return
        }
        val productDetails = if (isYearly) yearlyProductDetails else monthlyProductDetails
        productDetails ?: return
        val offerToken = promoOfferToken
            ?: productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productDetailsParams =
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        val billingFlowParamsBuilder =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productDetailsParams))

        // If user already has an active subscription, add update params
        // to allow offer changes (retention, plan switch) without ITEM_ALREADY_OWNED
        val oldToken = activePurchaseToken
        if (oldToken != null && _subscriptionState.value is SubscriptionState.Subscribed) {
            billingFlowParamsBuilder.setSubscriptionUpdateParams(
                BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                    .setOldPurchaseToken(oldToken)
                    .setSubscriptionReplacementMode(
                        BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION,
                    )
                    .build(),
            )
        }

        billingClient?.launchBillingFlow(activity, billingFlowParamsBuilder.build())
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
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
        activePurchaseToken = purchase.purchaseToken
        _subscriptionType.value = when {
            purchase.products.contains(LIFETIME_PRODUCT_ID) -> SubscriptionType.LIFETIME
            purchase.products.contains(YEARLY_PRODUCT_ID) -> SubscriptionType.YEARLY
            purchase.products.contains(MONTHLY_PRODUCT_ID) -> SubscriptionType.MONTHLY
            else -> _subscriptionType.value
        }
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
                    val type = when {
                        isYearly -> "yearly"
                        purchase.products.contains(MONTHLY_PRODUCT_ID) -> "monthly"
                        else -> "unknown"
                    }
                    val details = if (isYearly) yearlyProductDetails else monthlyProductDetails
                    val pricingPhase = details?.subscriptionOfferDetails
                        ?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()
                    val valueMicros = pricingPhase?.priceAmountMicros ?: 0L
                    val currency = pricingPhase?.priceCurrencyCode ?: "EUR"
                    analyticsTracker.trackSubscriptionPurchased(
                        type = type,
                        value = valueMicros / 1_000_000.0,
                        currency = currency,
                    )
                }
            } else if (retryCount < 3) {
                Log.w(TAG, "Acknowledge failed (attempt ${retryCount + 1}/3): ${result.debugMessage}")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    acknowledgePurchase(purchase, retryCount + 1)
                }, 2000L * (retryCount + 1))
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
