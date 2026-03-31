# BestJournalAndroid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create BestJournalAndroid — a Play Store version of Best Journal that uses Firebase AI Logic instead of user-provided API keys, with a Freemium system and Google Play Billing subscriptions.

**Architecture:** Copy BestJournalFrank as base, replace Gemini Retrofit + Groq API with Firebase AI Logic SDK, add AiUsageTracker for Freemium phase logic, add Google Play Billing for subscriptions. All prompts and UI stay the same except Settings (simplified) and new subscription screen.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Room, Firebase AI Logic SDK, Firebase App Check, Google Play Billing Library 7.x, sherpa-onnx (Offline Whisper)

---

## File Map

### New Files (BestJournalAndroid)
| File | Purpose |
|---|---|
| `data/remote/ai/FirebaseAiService.kt` | Firebase AI Logic wrapper — replaces GeminiApi + Retrofit |
| `data/remote/ai/AiUsageTracker.kt` | Tracks usage days, weekly AI usage count, phase detection |
| `data/remote/ai/AiRateLimiter.kt` | Checks if AI is allowed (phase + subscription + weekly limit) |
| `billing/BillingManager.kt` | Google Play Billing Library wrapper |
| `billing/SubscriptionState.kt` | Sealed class for subscription states |
| `ui/screens/subscription/SubscriptionScreen.kt` | Abo purchase/manage screen |
| `ui/components/AiInfoBanner.kt` | KI info banner for days 4-7 |
| `ui/components/AiLimitReachedSheet.kt` | Bottom sheet when weekly limit reached |
| `di/FirebaseModule.kt` | Hilt module for Firebase AI + App Check |

### Modified Files (from BestJournalFrank base)
| File | Changes |
|---|---|
| `build.gradle.kts` (root) | Add google-services plugin |
| `app/build.gradle.kts` | Change applicationId, add Firebase BOM + AI + Billing deps, remove Groq |
| `settings.gradle.kts` | Change rootProject.name |
| `gradle/libs.versions.toml` | Add Firebase, Billing versions |
| `di/NetworkModule.kt` | Remove Gemini/Groq Retrofit, keep Moshi + OkHttp for Drive only |
| `domain/usecase/ImproveTextUseCase.kt` | Use FirebaseAiService instead of GeminiApi |
| `domain/usecase/SummarizeEntryUseCase.kt` | Use FirebaseAiService instead of GeminiApi |
| `data/repository/AdviceRepository.kt` | Use FirebaseAiService instead of GeminiApi |
| `data/repository/TranscriptionRepository.kt` | Remove Groq API path, offline-only |
| `ui/screens/settings/SettingsScreen.kt` | Remove API key + model sections, add subscription section |
| `ui/screens/settings/SettingsViewModel.kt` | Remove API key fields, add subscription state |
| `ui/screens/dashboard/DashboardScreen.kt` | Add AiInfoBanner for days 4-7 |
| `ui/screens/journal/JournalViewModel.kt` | Check AiRateLimiter before AI calls |
| `util/Constants.kt` | Remove Groq/Gemini API constants, add Freemium constants |
| `MainActivity.kt` | Initialize Firebase App Check |
| `EntropyJournalApp.kt` | Initialize Firebase |
| `AndroidManifest.xml` | Update package references |

### Deleted Files (from BestJournalFrank base)
| File | Reason |
|---|---|
| `data/remote/gemini/GeminiApi.kt` | Replaced by FirebaseAiService |
| `data/remote/gemini/GeminiRequest.kt` | Firebase SDK has own types |
| `data/remote/gemini/GeminiResponse.kt` | Firebase SDK has own types |
| `data/remote/groq/GroqApi.kt` | No Groq in Play Store version |
| `data/remote/groq/GroqRequest.kt` | No Groq in Play Store version |
| `data/remote/groq/GroqResponse.kt` | No Groq in Play Store version |

---

## Task 1: Copy Project and Rename

**Files:**
- Create: `BestJournalAndroid/` (entire directory)
- Modify: Multiple config files inside the copy

- [ ] **Step 1: Copy the project directory (excluding build artifacts)**

```bash
cd /c/Users/barwa/proggs
cp -r BestJournalFrank BestJournalAndroid
rm -rf BestJournalAndroid/.gradle BestJournalAndroid/.kotlin BestJournalAndroid/build BestJournalAndroid/app/build BestJournalAndroid/app/libs/.gitkeep
```

- [ ] **Step 2: Update settings.gradle.kts**

Change `rootProject.name`:

```kotlin
rootProject.name = "BestJournal"
```

- [ ] **Step 3: Update app/build.gradle.kts — applicationId and namespace**

Change these values:

```kotlin
android {
    namespace = "com.bestjournal.app"
    // ...
    defaultConfig {
        applicationId = "com.bestjournal.app"
        // ...
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

- [ ] **Step 4: Rename Java package directories**

```bash
cd /c/Users/barwa/proggs/BestJournalAndroid/app/src/main/java
mkdir -p com/bestjournal/app
cp -r com/entropyjournal/* com/bestjournal/app/
rm -rf com/entropyjournal
```

- [ ] **Step 5: Rename package declarations in all Kotlin files**

```bash
cd /c/Users/barwa/proggs
python3 -c "
import glob, re
for f in sorted(glob.glob('BestJournalAndroid/app/src/main/java/**/*.kt', recursive=True)):
    with open(f, 'r', encoding='utf-8') as fh:
        content = fh.read()
    original = content
    content = content.replace('com.entropyjournal', 'com.bestjournal.app')
    if content != original:
        with open(f, 'w', encoding='utf-8', newline='\n') as fh:
            fh.write(content)
        print(f'Renamed: {f}')
"
```

- [ ] **Step 6: Update AndroidManifest.xml package references**

Replace all `com.entropyjournal` with `com.bestjournal.app` in `AndroidManifest.xml`.

- [ ] **Step 7: Update res/xml and res/values references**

Check `res/xml/network_security_config.xml`, `res/values/strings.xml` for any `entropyjournal` references and update.

- [ ] **Step 8: Verify build compiles**

```bash
cd /c/Users/barwa/proggs/BestJournalAndroid
./gradlew assembleDebug 2>&1 | tail -5
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
cd /c/Users/barwa/proggs
git add BestJournalAndroid/
git commit -m "#926 - Create BestJournalAndroid project as copy of BestJournalFrank with new package name com.bestjournal.app"
```

---

## Task 2: Add Firebase Dependencies and Remove Groq/Gemini Retrofit

**Files:**
- Modify: `BestJournalAndroid/build.gradle.kts` (root)
- Modify: `BestJournalAndroid/app/build.gradle.kts`
- Modify: `BestJournalAndroid/gradle/libs.versions.toml`
- Delete: `data/remote/gemini/` (entire directory)
- Delete: `data/remote/groq/` (entire directory)

- [ ] **Step 1: Add google-services plugin to root build.gradle.kts**

Add to the plugins block:

```kotlin
plugins {
    // existing plugins...
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

- [ ] **Step 2: Update libs.versions.toml — add Firebase and Billing versions**

Add to `[versions]`:

```toml
firebaseBom = "34.11.0"
playBilling = "7.1.1"
```

Add to `[libraries]`:

```toml
# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-ai = { group = "com.google.firebase", name = "firebase-ai" }
firebase-appcheck-playintegrity = { group = "com.google.firebase", name = "firebase-appcheck-playintegrity" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }

# Billing
play-billing = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "playBilling" }
```

Add to `[plugins]`:

```toml
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

- [ ] **Step 3: Update app/build.gradle.kts — add Firebase, remove Groq-specific deps**

Add plugin:

```kotlin
plugins {
    // existing...
    alias(libs.plugins.google.services)
}
```

Replace the Network dependencies section:

```kotlin
// Network (kept for Google Drive API only)
implementation(libs.retrofit)
implementation(libs.retrofit.moshi)
implementation(libs.retrofit.scalars)
implementation(libs.moshi.kotlin)
ksp(libs.moshi.codegen)
implementation(libs.okhttp)
implementation(libs.okhttp.logging)

// Firebase AI Logic
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.ai)
implementation(libs.firebase.appcheck.playintegrity)
implementation(libs.firebase.auth)

// Google Play Billing
implementation(libs.play.billing)
```

- [ ] **Step 4: Delete Gemini Retrofit files**

```bash
rm -rf /c/Users/barwa/proggs/BestJournalAndroid/app/src/main/java/com/bestjournal/app/data/remote/gemini/
```

- [ ] **Step 5: Delete Groq API files**

```bash
rm -rf /c/Users/barwa/proggs/BestJournalAndroid/app/src/main/java/com/bestjournal/app/data/remote/groq/
```

- [ ] **Step 6: Create placeholder google-services.json**

Create `BestJournalAndroid/app/google-services.json` with a placeholder structure.
This will be replaced with the real file from Firebase Console later.

```json
{
  "project_info": {
    "project_number": "TODO_FROM_FIREBASE_CONSOLE",
    "project_id": "bestjournal-app",
    "storage_bucket": "bestjournal-app.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "TODO_FROM_FIREBASE_CONSOLE",
        "android_client_info": {
          "package_name": "com.bestjournal.app"
        }
      },
      "api_key": [
        {
          "current_key": "TODO_FROM_FIREBASE_CONSOLE"
        }
      ]
    }
  ],
  "configuration_version": "1"
}
```

- [ ] **Step 7: Commit**

```bash
git add BestJournalAndroid/
git commit -m "#927 - Add Firebase AI Logic + Play Billing deps, remove Gemini Retrofit + Groq API files"
```

---

## Task 3: Create FirebaseAiService

**Files:**
- Create: `data/remote/ai/FirebaseAiService.kt`

- [ ] **Step 1: Create the ai directory**

```bash
mkdir -p /c/Users/barwa/proggs/BestJournalAndroid/app/src/main/java/com/bestjournal/app/data/remote/ai/
```

- [ ] **Step 2: Write FirebaseAiService.kt**

```kotlin
package com.bestjournal.app.data.remote.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAiService @Inject constructor() {

    companion object {
        private const val MODEL_NAME = "gemini-2.5-flash"
    }

    private fun createModel(
        temperature: Float = 0.4f,
        maxOutputTokens: Int = 8192,
        systemPrompt: String? = null
    ) = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                this.temperature = temperature
                this.maxOutputTokens = maxOutputTokens
            },
            systemInstruction = systemPrompt?.let {
                com.google.firebase.ai.type.content { text(it) }
            }
        )

    suspend fun generateContent(
        prompt: String,
        temperature: Float = 0.4f,
        maxOutputTokens: Int = 8192,
        systemPrompt: String? = null
    ): Result<String> {
        return try {
            val model = createModel(temperature, maxOutputTokens, systemPrompt)
            val response = model.generateContent(prompt)
            val text = response.text
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("No response text from Gemini"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/data/remote/ai/
git commit -m "#928 - Add FirebaseAiService wrapper for Firebase AI Logic SDK"
```

---

## Task 4: Create AiUsageTracker (Freemium Logic)

**Files:**
- Create: `data/remote/ai/AiUsageTracker.kt`

- [ ] **Step 1: Write AiUsageTracker.kt**

```kotlin
package com.bestjournal.app.data.remote.ai

import android.content.SharedPreferences
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiUsageTracker @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val KEY_USAGE_DAYS = "ai_usage_days"          // "2026-03-28,2026-03-29,..."
        private const val KEY_WEEKLY_AI_COUNT = "ai_weekly_count"    // Int
        private const val KEY_WEEKLY_RESET_DATE = "ai_weekly_reset"  // "YYYY-MM-DD"
        const val TRIAL_DAYS = 7
        const val FREE_WEEKLY_LIMIT = 3
        const val HONEYMOON_DAYS = 3
    }

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Record that the user created an entry today. */
    fun recordUsageDay() {
        val today = LocalDate.now().format(dateFormatter)
        val days = getUsageDaysSet().toMutableSet()
        days.add(today)
        prefs.edit().putString(KEY_USAGE_DAYS, days.joinToString(",")).apply()
    }

    /** Record one AI usage (text improve, summary, or dashboard analysis). */
    fun recordAiUsage() {
        resetWeeklyCounterIfNeeded()
        val count = prefs.getInt(KEY_WEEKLY_AI_COUNT, 0)
        prefs.edit().putInt(KEY_WEEKLY_AI_COUNT, count + 1).apply()
    }

    /** How many usage days has this user had? */
    fun getUsageDayCount(): Int = getUsageDaysSet().size

    /** Current phase based on usage days. */
    fun getCurrentPhase(): AiPhase {
        val days = getUsageDayCount()
        return when {
            days <= HONEYMOON_DAYS -> AiPhase.HONEYMOON
            days <= TRIAL_DAYS -> AiPhase.EDUCATION
            else -> AiPhase.FREEMIUM
        }
    }

    /** How many AI uses this week? */
    fun getWeeklyAiUsageCount(): Int {
        resetWeeklyCounterIfNeeded()
        return prefs.getInt(KEY_WEEKLY_AI_COUNT, 0)
    }

    /** How many free AI uses remain this week? */
    fun getRemainingFreeUses(): Int {
        return (FREE_WEEKLY_LIMIT - getWeeklyAiUsageCount()).coerceAtLeast(0)
    }

    /** Is AI allowed right now (ignoring subscription — that's checked separately)? */
    fun isAiAllowedForFreeUser(): Boolean {
        val phase = getCurrentPhase()
        return when (phase) {
            AiPhase.HONEYMOON, AiPhase.EDUCATION -> true // Unlimited during trial
            AiPhase.FREEMIUM -> getWeeklyAiUsageCount() < FREE_WEEKLY_LIMIT
        }
    }

    /** Should the AI info banner be shown? */
    fun shouldShowAiInfoBanner(): Boolean {
        val phase = getCurrentPhase()
        if (phase != AiPhase.EDUCATION) return false
        // Show once per day
        val today = LocalDate.now().format(dateFormatter)
        val lastShown = prefs.getString("ai_banner_last_shown", "") ?: ""
        return lastShown != today
    }

    fun markAiInfoBannerShown() {
        val today = LocalDate.now().format(dateFormatter)
        prefs.edit().putString("ai_banner_last_shown", today).apply()
    }

    private fun getUsageDaysSet(): Set<String> {
        val raw = prefs.getString(KEY_USAGE_DAYS, "") ?: ""
        return if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    private fun resetWeeklyCounterIfNeeded() {
        val nextReset = prefs.getString(KEY_WEEKLY_RESET_DATE, "") ?: ""
        val today = LocalDate.now()

        if (nextReset.isBlank() || today >= LocalDate.parse(nextReset, dateFormatter)) {
            // Reset: next Monday
            val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            prefs.edit()
                .putInt(KEY_WEEKLY_AI_COUNT, 0)
                .putString(KEY_WEEKLY_RESET_DATE, nextMonday.format(dateFormatter))
                .apply()
        }
    }
}

enum class AiPhase {
    HONEYMOON,  // Day 1-3: unlimited, no banners
    EDUCATION,  // Day 4-7: unlimited, show AI info banner
    FREEMIUM    // Day 8+: 3 free/week, then paywall
}
```

- [ ] **Step 2: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/data/remote/ai/AiUsageTracker.kt
git commit -m "#929 - Add AiUsageTracker with usage day counting and Freemium phase logic"
```

---

## Task 5: Create AiRateLimiter

**Files:**
- Create: `data/remote/ai/AiRateLimiter.kt`

- [ ] **Step 1: Write AiRateLimiter.kt**

```kotlin
package com.bestjournal.app.data.remote.ai

import com.bestjournal.app.billing.SubscriptionState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRateLimiter @Inject constructor(
    private val usageTracker: AiUsageTracker
) {
    /**
     * Check if AI usage is allowed.
     * @param subscriptionState Current subscription status
     * @return AiAccessResult indicating whether AI is allowed and why not
     */
    fun checkAccess(subscriptionState: SubscriptionState): AiAccessResult {
        // Subscribers always have access
        if (subscriptionState is SubscriptionState.Subscribed) {
            return AiAccessResult.Allowed
        }

        val phase = usageTracker.getCurrentPhase()
        return when (phase) {
            AiPhase.HONEYMOON, AiPhase.EDUCATION -> AiAccessResult.Allowed
            AiPhase.FREEMIUM -> {
                if (usageTracker.isAiAllowedForFreeUser()) {
                    AiAccessResult.Allowed
                } else {
                    AiAccessResult.LimitReached(
                        weeklyUsed = usageTracker.getWeeklyAiUsageCount(),
                        weeklyLimit = AiUsageTracker.FREE_WEEKLY_LIMIT
                    )
                }
            }
        }
    }

    /** Max entries for entropy analysis based on subscription/phase. */
    fun getMaxEntriesForAnalysis(subscriptionState: SubscriptionState): Int {
        if (subscriptionState is SubscriptionState.Subscribed) return 30
        val phase = usageTracker.getCurrentPhase()
        return when (phase) {
            AiPhase.HONEYMOON, AiPhase.EDUCATION -> Int.MAX_VALUE // Unlimited during trial
            AiPhase.FREEMIUM -> 10
        }
    }
}

sealed class AiAccessResult {
    data object Allowed : AiAccessResult()
    data class LimitReached(val weeklyUsed: Int, val weeklyLimit: Int) : AiAccessResult()
}
```

- [ ] **Step 2: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/data/remote/ai/AiRateLimiter.kt
git commit -m "#930 - Add AiRateLimiter combining subscription state with Freemium limits"
```

---

## Task 6: Create Subscription State and BillingManager

**Files:**
- Create: `billing/SubscriptionState.kt`
- Create: `billing/BillingManager.kt`

- [ ] **Step 1: Create billing directory**

```bash
mkdir -p /c/Users/barwa/proggs/BestJournalAndroid/app/src/main/java/com/bestjournal/app/billing/
```

- [ ] **Step 2: Write SubscriptionState.kt**

```kotlin
package com.bestjournal.app.billing

sealed class SubscriptionState {
    data object Free : SubscriptionState()
    data object Subscribed : SubscriptionState()
    data object Expired : SubscriptionState()
}
```

- [ ] **Step 3: Write BillingManager.kt**

```kotlin
package com.bestjournal.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor() : PurchasesUpdatedListener {

    companion object {
        const val MONTHLY_PRODUCT_ID = "bestjournal_ai_monthly"
        const val YEARLY_PRODUCT_ID = "bestjournal_ai_yearly"
    }

    private var billingClient: BillingClient? = null
    private val _subscriptionState = MutableStateFlow<SubscriptionState>(SubscriptionState.Free)
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    private val _monthlyPrice = MutableStateFlow("")
    val monthlyPrice: StateFlow<String> = _monthlyPrice.asStateFlow()

    private val _yearlyPrice = MutableStateFlow("")
    val yearlyPrice: StateFlow<String> = _yearlyPrice.asStateFlow()

    private var monthlyProductDetails: ProductDetails? = null
    private var yearlyProductDetails: ProductDetails? = null

    fun initialize(context: Context) {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscriptions()
                    queryProductDetails()
                }
            }
            override fun onBillingServiceDisconnected() {
                // Retry connection on next access
            }
        })
    }

    private fun querySubscriptions() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActive = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.isAcknowledged
                }
                _subscriptionState.value = if (hasActive) {
                    SubscriptionState.Subscribed
                } else {
                    SubscriptionState.Free
                }

                // Acknowledge unacknowledged purchases
                purchases.filter {
                    it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
                }.forEach { purchase ->
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient?.acknowledgePurchase(ackParams) { /* acknowledged */ }
                    _subscriptionState.value = SubscriptionState.Subscribed
                }
            }
        }
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(MONTHLY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(YEARLY_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (details in productDetailsList) {
                    val price = details.subscriptionOfferDetails
                        ?.firstOrNull()?.pricingPhases?.pricingPhaseList
                        ?.firstOrNull()?.formattedPrice ?: ""
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

    fun launchPurchaseFlow(activity: Activity, isYearly: Boolean) {
        val productDetails = if (isYearly) yearlyProductDetails else monthlyProductDetails
        productDetails ?: return

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (!purchase.isAcknowledged) {
                        val ackParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                        billingClient?.acknowledgePurchase(ackParams) { /* done */ }
                    }
                    _subscriptionState.value = SubscriptionState.Subscribed
                }
            }
        }
    }

    fun destroy() {
        billingClient?.endConnection()
        billingClient = null
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/billing/
git commit -m "#931 - Add Google Play Billing with BillingManager and SubscriptionState"
```

---

## Task 7: Create Firebase DI Module and Update NetworkModule

**Files:**
- Create: `di/FirebaseModule.kt`
- Modify: `di/NetworkModule.kt`

- [ ] **Step 1: Write FirebaseModule.kt**

```kotlin
package com.bestjournal.app.di

import android.content.Context
import android.content.SharedPreferences
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.data.remote.ai.AiRateLimiter
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAiService(): FirebaseAiService = FirebaseAiService()

    @Provides
    @Singleton
    fun provideAiUsageTracker(prefs: SharedPreferences): AiUsageTracker = AiUsageTracker(prefs)

    @Provides
    @Singleton
    fun provideAiRateLimiter(usageTracker: AiUsageTracker): AiRateLimiter = AiRateLimiter(usageTracker)

    @Provides
    @Singleton
    fun provideBillingManager(): BillingManager = BillingManager()
}
```

- [ ] **Step 2: Update NetworkModule.kt — remove Gemini and Groq**

Remove the following from NetworkModule.kt:
- `provideGroqOkHttpClient` function
- `provideGeminiOkHttpClient` function
- `provideGroqApi` function
- `provideGeminiApi` function
- All `@Named("groq")` and `@Named("gemini")` qualifiers
- Imports for `GeminiApi`, `GroqApi`, `Constants.GROQ_BASE_URL`, `Constants.GEMINI_BASE_URL`

Keep:
- `provideMoshi()` (used by Google Drive API)
- `provideLoggingInterceptor()` (used by Google Drive API)
- A single `provideOkHttpClient()` for Drive API only

- [ ] **Step 3: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/di/
git commit -m "#932 - Add FirebaseModule, simplify NetworkModule (remove Gemini/Groq Retrofit)"
```

---

## Task 8: Update Use Cases to Use FirebaseAiService

**Files:**
- Modify: `domain/usecase/ImproveTextUseCase.kt`
- Modify: `domain/usecase/SummarizeEntryUseCase.kt`
- Modify: `data/repository/AdviceRepository.kt`

- [ ] **Step 1: Update ImproveTextUseCase.kt**

Replace `GeminiApi` dependency with `FirebaseAiService`. Remove `encryptedPrefs` (no API key needed). Keep the prompt and chunking logic identical.

Key changes:
- Constructor: `FirebaseAiService` instead of `GeminiApi` + `SharedPreferences`
- `rewriteChunk()`: Call `firebaseAiService.generateContent(buildPrompt(text))` instead of `geminiApi.generateContent(...)`
- Remove `getApiKey()` and `getSelectedModel()` methods
- Remove the API key check at the start of `invoke()`

- [ ] **Step 2: Update SummarizeEntryUseCase.kt**

Replace `GeminiApi` + `SharedPreferences` with `FirebaseAiService`. Keep prompt identical.

Key changes:
- Constructor: `FirebaseAiService` instead of `GeminiApi` + `SharedPreferences`
- Call `firebaseAiService.generateContent(buildPrompt(displayText), temperature = 0.3f, maxOutputTokens = 512)`
- Remove API key check

- [ ] **Step 3: Update AdviceRepository.kt**

Replace `GeminiApi` + `SharedPreferences` with `FirebaseAiService`. Keep the entire prompt and parsing logic identical.

Key changes:
- Constructor: `FirebaseAiService` instead of `GeminiApi` + `SharedPreferences`
- `analyzeEntropy()`: Call `firebaseAiService.generateContent(userText, systemPrompt = entropyAnalysisSystemPrompt)`
- Remove `getSelectedModel()` and API key check

- [ ] **Step 4: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/domain/usecase/
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/data/repository/AdviceRepository.kt
git commit -m "#933 - Migrate ImproveTextUseCase, SummarizeEntryUseCase, AdviceRepository to FirebaseAiService"
```

---

## Task 9: Update Constants and Remove Groq/Gemini References

**Files:**
- Modify: `util/Constants.kt`

- [ ] **Step 1: Update Constants.kt**

Remove:
- `GROQ_BASE_URL`, `GEMINI_BASE_URL`
- `GROQ_TRANSCRIPTION_MODEL`, `GROQ_LANGUAGE`
- `PREF_GROQ_API_KEY`, `PREF_GEMINI_API_KEY`, `PREF_GEMINI_MODEL`
- `GeminiModel` data class and `GEMINI_FLASH_MODELS` list
- `DEFAULT_GEMINI_MODEL`

Add:
```kotlin
// Freemium
const val TRIAL_USAGE_DAYS = 7
const val HONEYMOON_USAGE_DAYS = 3
const val FREE_WEEKLY_AI_LIMIT = 3
const val MAX_ENTRIES_FREE_ANALYSIS = 10
const val MAX_ENTRIES_SUBSCRIBED_ANALYSIS = 30
```

- [ ] **Step 2: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/util/Constants.kt
git commit -m "#934 - Remove Groq/Gemini constants, add Freemium constants"
```

---

## Task 10: Update Settings Screen (Remove API Keys, Add Subscription)

**Files:**
- Modify: `ui/screens/settings/SettingsScreen.kt`
- Modify: `ui/screens/settings/SettingsViewModel.kt`

- [ ] **Step 1: Update SettingsScreen.kt**

Remove these sections entirely:
- Section 4 "API-Schlüssel" (GlassCard with Groq/Gemini API key fields)
- Section 4 "Gemini-Modell" (GlassCard with model dropdown)
- The `ApiKeyField` composable function

Add a new section "KI-Abo" after "Sicherheit":

```kotlin
// 4. KI-Abo
GlassCard {
    Column {
        Text("KI-Abo", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        // Show current status
        val phase = uiState.aiPhase
        val isSubscribed = uiState.isSubscribed
        Text(
            text = when {
                isSubscribed -> "Premium-Abo aktiv"
                phase == "HONEYMOON" || phase == "EDUCATION" -> "Kostenlose Testphase"
                else -> "Kostenlos (${uiState.remainingFreeUses} von 3 KI-Nutzungen übrig)"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!isSubscribed && phase == "FREEMIUM") {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { /* navigate to subscription screen */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Auf Premium upgraden")
            }
        }
    }
}
```

- [ ] **Step 2: Update SettingsViewModel.kt**

Remove:
- `groqApiKey`, `geminiApiKey`, `selectedModel` from UI state
- `updateGroqApiKey()`, `updateGeminiApiKey()`, `updateSelectedModel()` methods

Add:
- `aiPhase: String` to UI state (from AiUsageTracker)
- `isSubscribed: Boolean` to UI state (from BillingManager)
- `remainingFreeUses: Int` to UI state (from AiUsageTracker)

- [ ] **Step 3: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/ui/screens/settings/
git commit -m "#935 - Simplify Settings: remove API keys, add KI-Abo section"
```

---

## Task 11: Update TranscriptionRepository (Remove Groq)

**Files:**
- Modify: `data/repository/TranscriptionRepository.kt`

- [ ] **Step 1: Update TranscriptionRepository.kt**

Remove the Groq API transcription path. Keep only the offline sherpa-onnx Whisper path.
Remove GroqApi injection from constructor. Remove any toggle between online/offline transcription.

- [ ] **Step 2: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/data/repository/TranscriptionRepository.kt
git commit -m "#936 - Remove Groq API from TranscriptionRepository, offline Whisper only"
```

---

## Task 12: Add AI Rate Limiting to JournalViewModel

**Files:**
- Modify: `ui/screens/journal/JournalViewModel.kt`

- [ ] **Step 1: Update JournalViewModel**

Inject `AiRateLimiter`, `AiUsageTracker`, and `BillingManager`.

Before every AI call (text improvement, summary), check:
```kotlin
val access = aiRateLimiter.checkAccess(billingManager.subscriptionState.value)
when (access) {
    is AiAccessResult.Allowed -> {
        // proceed with AI call
        aiUsageTracker.recordAiUsage()
    }
    is AiAccessResult.LimitReached -> {
        // show limit reached state in UI
        _uiState.update { it.copy(showAiLimitReached = true) }
    }
}
```

Also call `aiUsageTracker.recordUsageDay()` when a new entry is created.

- [ ] **Step 2: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/ui/screens/journal/JournalViewModel.kt
git commit -m "#937 - Add AI rate limiting checks before every AI call in JournalViewModel"
```

---

## Task 13: Create AI Info Banner Component

**Files:**
- Create: `ui/components/AiInfoBanner.kt`

- [ ] **Step 1: Write AiInfoBanner.kt**

```kotlin
package com.bestjournal.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AiInfoBanner(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(true) }

    AnimatedVisibility(visible = visible) {
        Card(
            modifier = modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Dein Tagebuch wird von KI unterstützt",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { visible = false; onDismiss() }) {
                        Icon(Icons.Rounded.Close, "Schließen",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "• Diktierte Texte werden automatisch verbessert\n" +
                    "• Zusammenfassungen für jeden Eintrag\n" +
                    "• Intelligente Lebensratschläge aus deinen Mustern\n" +
                    "• Alles in deiner kostenlosen Testphase enthalten",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/ui/components/AiInfoBanner.kt
git commit -m "#938 - Add AiInfoBanner component for education phase (days 4-7)"
```

---

## Task 14: Create AI Limit Reached Bottom Sheet

**Files:**
- Create: `ui/components/AiLimitReachedSheet.kt`

- [ ] **Step 1: Write AiLimitReachedSheet.kt**

```kotlin
package com.bestjournal.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiLimitReachedSheet(
    monthlyPrice: String,
    yearlyPrice: String,
    onSubscribeMonthly: () -> Unit,
    onSubscribeYearly: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Deine KI-Nutzungen für diese Woche sind aufgebraucht",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Mit dem Premium-Abo bekommst du unbegrenzte KI-Power:\n" +
                "Textverbesserung, Zusammenfassungen und Lebensratschläge — ohne Limit.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Monthly option
            OutlinedButton(
                onClick = onSubscribeMonthly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Monatsabo — $monthlyPrice/Monat")
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Yearly option (highlighted)
            Button(
                onClick = onSubscribeYearly,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Jahresabo — $yearlyPrice/Jahr (spare 20%)")
            }
            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    "Nein danke, weiter ohne KI",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/ui/components/AiLimitReachedSheet.kt
git commit -m "#939 - Add AiLimitReachedSheet bottom sheet for Freemium paywall"
```

---

## Task 15: Initialize Firebase in Application and MainActivity

**Files:**
- Modify: `EntropyJournalApp.kt` (rename to `BestJournalApp.kt`)
- Modify: `MainActivity.kt`

- [ ] **Step 1: Rename and update Application class**

Rename file to `BestJournalApp.kt`. Add Firebase App Check initialization:

```kotlin
package com.bestjournal.app

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BestJournalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
    }
}
```

- [ ] **Step 2: Update AndroidManifest.xml**

Change `android:name` to `.BestJournalApp`.

- [ ] **Step 3: Update MainActivity.kt**

Add BillingManager initialization in `onCreate()`:
```kotlin
@Inject lateinit var billingManager: BillingManager

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    billingManager.initialize(this)
    // ... rest unchanged
}

override fun onDestroy() {
    billingManager.destroy()
    super.onDestroy()
}
```

- [ ] **Step 4: Commit**

```bash
git add BestJournalAndroid/app/src/main/java/com/bestjournal/app/
git add BestJournalAndroid/app/src/main/AndroidManifest.xml
git commit -m "#940 - Initialize Firebase App Check + Billing in Application and MainActivity"
```

---

## Task 16: Update .gitattributes for LFS and Build Verification

**Files:**
- Modify: `.gitattributes` (root repo level)

- [ ] **Step 1: Add LFS rules for BestJournalAndroid**

Add these lines to the root `.gitattributes`:

```
BestJournalAndroid/app/src/main/assets/whisper/*.onnx filter=lfs diff=lfs merge=lfs -text
BestJournalAndroid/app/libs/*.aar filter=lfs diff=lfs merge=lfs -text
```

- [ ] **Step 2: Build the project to verify everything compiles**

```bash
cd /c/Users/barwa/proggs/BestJournalAndroid
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL (may have warnings about placeholder google-services.json)

- [ ] **Step 3: Commit all remaining changes**

```bash
cd /c/Users/barwa/proggs
git add .gitattributes BestJournalAndroid/
git commit -m "#941 - Add LFS rules for BestJournalAndroid, verify build"
```

---

## Task 17: Token Cost Calculation (Research, No Code)

This task is research — calculate the actual token costs to determine subscription pricing.

- [ ] **Step 1: Measure token usage per feature**

Estimate tokens for each KI feature:
- **Text improvement**: ~500 tokens prompt + ~2000 tokens input text + ~2000 tokens output = ~4500 tokens per entry
- **Summary**: ~300 tokens prompt + ~2000 tokens input + ~200 tokens output = ~2500 tokens per entry
- **Entropy analysis (10 entries)**: ~2000 tokens system prompt + ~20000 tokens entries + ~3000 tokens output = ~25000 tokens
- **Entropy analysis (30 entries)**: ~2000 tokens system prompt + ~60000 tokens entries + ~5000 tokens output = ~67000 tokens

- [ ] **Step 2: Calculate cost per user per month**

Assume an active subscriber writes 20 entries/month and refreshes the dashboard 4 times:
- Text improvement: 20 × 4500 = 90,000 tokens
- Summaries: 20 × 2500 = 50,000 tokens
- Dashboard: 4 × 67,000 = 268,000 tokens
- Total: ~408,000 tokens/month

With Gemini 2.5 Flash ($0.30/1M input, $2.50/1M output):
- Input cost: ~350K input tokens × $0.30/1M = $0.105
- Output cost: ~58K output tokens × $2.50/1M = $0.145
- **Total cost per active subscriber: ~$0.25/month**

- [ ] **Step 3: Propose subscription prices**

With $0.25/month cost per user and targeting 5-10x markup for profit + free tier subsidy:
- **Monthly subscription: €2.99/month** (covers ~12 free users per subscriber)
- **Yearly subscription: €24.99/year** (~€2.08/month, 30% savings)

Document this in the spec file for the user's review.

---

## Summary: Execution Order

Tasks 1-16 are code changes. Task 17 is research/calculation.

**Dependencies:**
- Task 1 must be first (creates the project)
- Tasks 2-5 can run in parallel after Task 1
- Task 6 can run in parallel with Tasks 3-5
- Task 7 depends on Tasks 3, 4, 5, 6
- Task 8 depends on Task 3 (needs FirebaseAiService)
- Tasks 9-11 can run in parallel after Task 8
- Tasks 12-14 can run in parallel after Tasks 5, 6
- Task 15 depends on Task 6 (needs BillingManager)
- Task 16 depends on all other code tasks
- Task 17 can run anytime (research only)

**Parallelizable groups:**
1. Task 1 (sequential, must be first)
2. Tasks 2, 3, 4, 5, 6 (parallel)
3. Tasks 7, 8 (parallel after group 2)
4. Tasks 9, 10, 11, 12, 13, 14 (parallel after group 3)
5. Task 15 (after group 4)
6. Task 16 (final verification)
7. Task 17 (anytime, independent)
