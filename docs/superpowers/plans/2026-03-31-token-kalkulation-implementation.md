# Token-Kalkulation & Kostenkontrolle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement subscription pricing (€3.99/month, €39.99/year), dashboard daily limit (16 silent → 20 cooldown → hard stop), spam protection, and entry limit (max 40) in BestJournalAndroid.

**Architecture:** Add dashboard rate limiting to AiUsageTracker (SharedPreferences counter), check limits in DashboardViewModel before API calls, show appropriate UI messages. Update Constants with new values. Update AiLimitReachedSheet with real prices.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, SharedPreferences, Firebase AI Logic

---

## File Map

### Modified Files
| File | Changes |
|---|---|
| `app/src/main/java/com/bestjournal/app/util/Constants.kt` | Add dashboard limit constants, update entry limit to 40 |
| `app/src/main/java/com/bestjournal/app/data/remote/ai/AiUsageTracker.kt` | Add dashboard daily counter, cooldown logic, spam detection |
| `app/src/main/java/com/bestjournal/app/data/remote/ai/AiRateLimiter.kt` | Add dashboard limit check, update entry limits to 40 |
| `app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardViewModel.kt` | Check dashboard limit before refresh, expose limit state |
| `app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt` | Show cooldown/limit messages |
| `app/src/main/java/com/bestjournal/app/ui/components/AiLimitReachedSheet.kt` | Update price text (spare 33%) |
| `app/src/main/java/com/bestjournal/app/billing/BillingManager.kt` | No code change — prices set in Google Play Console |

---

## Task 1: Update Constants

**Files:**
- Modify: `app/src/main/java/com/bestjournal/app/util/Constants.kt`

- [ ] **Step 1: Add dashboard limit constants and update entry limit**

```kotlin
// In Constants.kt, replace the Freemium section:

    // Freemium
    const val TRIAL_USAGE_DAYS = 7
    const val HONEYMOON_USAGE_DAYS = 3
    const val FREE_WEEKLY_AI_LIMIT = 3
    const val MAX_ENTRIES_FREE_ANALYSIS = 10
    const val MAX_ENTRIES_SUBSCRIBED_ANALYSIS = 40
    const val MAX_ENTRIES_TRIAL_ANALYSIS = 40

    // Dashboard daily limits
    const val DASHBOARD_SILENT_LIMIT = 16
    const val DASHBOARD_COOLDOWN_LIMIT = 20
    const val DASHBOARD_COOLDOWN_MINUTES = 30

    // Spam protection
    const val SPAM_HOURLY_AI_LIMIT = 25

    // Subscription pricing (display only — actual prices set in Google Play Console)
    const val MONTHLY_PRICE_DISPLAY = "4,99\u00A0\u20AC"
    const val YEARLY_PRICE_DISPLAY = "39,99\u00A0\u20AC"
    const val YEARLY_MONTHLY_EQUIVALENT = "3,33\u00A0\u20AC"
```

- [ ] **Step 2: Build to verify no compilation errors**

Run: `cd /Users/frank/proggs/BestJournalAndroid && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bestjournal/app/util/Constants.kt
git commit -m "#943 - Add dashboard limit constants, update entry limit to 40"
```

---

## Task 2: Add Dashboard Rate Limiting to AiUsageTracker

**Files:**
- Modify: `app/src/main/java/com/bestjournal/app/data/remote/ai/AiUsageTracker.kt`

- [ ] **Step 1: Add dashboard daily counter and cooldown logic**

Add these new methods and keys to AiUsageTracker:

```kotlin
// Add to companion object:
        private const val KEY_DASHBOARD_DAILY_COUNT = "dashboard_daily_count"
        private const val KEY_DASHBOARD_DAILY_DATE = "dashboard_daily_date"
        private const val KEY_DASHBOARD_COOLDOWN_UNTIL = "dashboard_cooldown_until"
        private const val KEY_HOURLY_AI_COUNT = "hourly_ai_count"
        private const val KEY_HOURLY_AI_RESET = "hourly_ai_reset"
```

Add these methods after the existing methods:

```kotlin
    // ── Dashboard daily limit ──────────────────────────────

    fun recordDashboardRefresh() {
        resetDashboardCounterIfNewDay()
        val count = prefs.getInt(KEY_DASHBOARD_DAILY_COUNT, 0)
        prefs.edit().putInt(KEY_DASHBOARD_DAILY_COUNT, count + 1).apply()
    }

    fun getDashboardDailyCount(): Int {
        resetDashboardCounterIfNewDay()
        return prefs.getInt(KEY_DASHBOARD_DAILY_COUNT, 0)
    }

    fun getDashboardAccessResult(): DashboardAccessResult {
        resetDashboardCounterIfNewDay()
        val count = prefs.getInt(KEY_DASHBOARD_DAILY_COUNT, 0)

        // Check cooldown first
        val cooldownUntil = prefs.getLong(KEY_DASHBOARD_COOLDOWN_UNTIL, 0L)
        if (cooldownUntil > 0 && System.currentTimeMillis() < cooldownUntil) {
            val minutesLeft = ((cooldownUntil - System.currentTimeMillis()) / 60_000).toInt() + 1
            return DashboardAccessResult.Cooldown(minutesLeft)
        }

        return when {
            count < Constants.DASHBOARD_SILENT_LIMIT -> DashboardAccessResult.Allowed
            count < Constants.DASHBOARD_COOLDOWN_LIMIT -> {
                // Set cooldown timer
                val cooldownMs = Constants.DASHBOARD_COOLDOWN_MINUTES * 60_000L
                prefs.edit().putLong(KEY_DASHBOARD_COOLDOWN_UNTIL, System.currentTimeMillis() + cooldownMs).apply()
                DashboardAccessResult.Cooldown(Constants.DASHBOARD_COOLDOWN_MINUTES)
            }
            else -> DashboardAccessResult.DailyLimitReached
        }
    }

    private fun resetDashboardCounterIfNewDay() {
        val today = LocalDate.now().format(dateFormatter)
        val savedDate = prefs.getString(KEY_DASHBOARD_DAILY_DATE, "") ?: ""
        if (savedDate != today) {
            prefs.edit()
                .putInt(KEY_DASHBOARD_DAILY_COUNT, 0)
                .putString(KEY_DASHBOARD_DAILY_DATE, today)
                .putLong(KEY_DASHBOARD_COOLDOWN_UNTIL, 0L)
                .apply()
        }
    }

    // ── Spam protection (hourly) ───────────────────────────

    fun recordHourlyAiUsage() {
        resetHourlyCounterIfNeeded()
        val count = prefs.getInt(KEY_HOURLY_AI_COUNT, 0)
        prefs.edit().putInt(KEY_HOURLY_AI_COUNT, count + 1).apply()
    }

    fun isHourlySpamLimitReached(): Boolean {
        resetHourlyCounterIfNeeded()
        return prefs.getInt(KEY_HOURLY_AI_COUNT, 0) >= Constants.SPAM_HOURLY_AI_LIMIT
    }

    private fun resetHourlyCounterIfNeeded() {
        val nextReset = prefs.getLong(KEY_HOURLY_AI_RESET, 0L)
        if (System.currentTimeMillis() >= nextReset) {
            prefs.edit()
                .putInt(KEY_HOURLY_AI_COUNT, 0)
                .putLong(KEY_HOURLY_AI_RESET, System.currentTimeMillis() + 3_600_000L)
                .apply()
        }
    }
```

Add this sealed class at the bottom of the file (after the AiPhase enum):

```kotlin
sealed class DashboardAccessResult {
    data object Allowed : DashboardAccessResult()
    data class Cooldown(val minutesLeft: Int) : DashboardAccessResult()
    data object DailyLimitReached : DashboardAccessResult()
}
```

- [ ] **Step 2: Build to verify**

Run: `cd /Users/frank/proggs/BestJournalAndroid && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bestjournal/app/data/remote/ai/AiUsageTracker.kt
git commit -m "#943 - Add dashboard daily counter, cooldown logic, and spam protection to AiUsageTracker"
```

---

## Task 3: Update AiRateLimiter Entry Limits

**Files:**
- Modify: `app/src/main/java/com/bestjournal/app/data/remote/ai/AiRateLimiter.kt`

- [ ] **Step 1: Update getMaxEntriesForAnalysis to use 40 and add dashboard check**

Replace the entire `getMaxEntriesForAnalysis` method:

```kotlin
    fun getMaxEntriesForAnalysis(subscriptionState: SubscriptionState): Int {
        if (subscriptionState is SubscriptionState.Subscribed) return Constants.MAX_ENTRIES_SUBSCRIBED_ANALYSIS
        val phase = usageTracker.getCurrentPhase()
        return when (phase) {
            AiPhase.HONEYMOON, AiPhase.EDUCATION -> Constants.MAX_ENTRIES_TRIAL_ANALYSIS
            AiPhase.FREEMIUM -> Constants.MAX_ENTRIES_FREE_ANALYSIS
        }
    }

    fun checkDashboardAccess(): DashboardAccessResult {
        return usageTracker.getDashboardAccessResult()
    }

    fun recordDashboardRefresh() {
        usageTracker.recordDashboardRefresh()
        usageTracker.recordHourlyAiUsage()
    }
```

Add the import at the top:

```kotlin
import com.bestjournal.app.util.Constants
```

- [ ] **Step 2: Build to verify**

Run: `cd /Users/frank/proggs/BestJournalAndroid && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bestjournal/app/data/remote/ai/AiRateLimiter.kt
git commit -m "#943 - Update entry limits to 40, add dashboard access check to AiRateLimiter"
```

---

## Task 4: Update DashboardViewModel with Limit Checks

**Files:**
- Modify: `app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardViewModel.kt`

- [ ] **Step 1: Add limit state to DashboardUiState and check before refresh**

Replace the entire file content:

```kotlin
package com.bestjournal.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.data.remote.ai.AiRateLimiter
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.ai.DashboardAccessResult
import com.bestjournal.app.data.repository.AdviceRepository
import com.bestjournal.app.domain.usecase.AnalyzeEntropyUseCase
import com.bestjournal.app.domain.usecase.GenerateAdviceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = false,
    val selectedCategoryIndex: Int = 0,
    val errorMessage: String? = null,
    val canUndo: Boolean = false,
    val showAiInfoBanner: Boolean = false,
    val dashboardLimitMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val generateAdviceUseCase: GenerateAdviceUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val adviceRepository: AdviceRepository,
    private val aiUsageTracker: AiUsageTracker,
    private val aiRateLimiter: AiRateLimiter
) : ViewModel() {

    val adviceBlocks = generateAdviceUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        if (aiUsageTracker.shouldShowAiInfoBanner()) {
            _uiState.update { it.copy(showAiInfoBanner = true) }
        }
    }

    fun selectCategory(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index)
    }

    fun refreshDashboard() {
        // Check dashboard daily limit before making API call
        when (val access = aiRateLimiter.checkDashboardAccess()) {
            is DashboardAccessResult.Allowed -> performRefresh()
            is DashboardAccessResult.Cooldown -> {
                _uiState.update {
                    it.copy(dashboardLimitMessage = "Das System ist gerade ausgelastet. Bitte versuche es in ${access.minutesLeft} Minuten erneut.")
                }
            }
            is DashboardAccessResult.DailyLimitReached -> {
                _uiState.update {
                    it.copy(dashboardLimitMessage = "Neue Aktualisierungen sind morgen wieder verf\u00fcgbar.")
                }
            }
        }
    }

    private fun performRefresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, dashboardLimitMessage = null)
            aiRateLimiter.recordDashboardRefresh()
            analyzeEntropyUseCase(freshAnalysis = true)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        canUndo = adviceRepository.canUndo,
                        selectedCategoryIndex = 0
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Analyse fehlgeschlagen"
                    )
                }
        }
    }

    fun undoDashboard() {
        viewModelScope.launch {
            val success = adviceRepository.undoLastRefresh()
            _uiState.value = _uiState.value.copy(
                canUndo = false,
                selectedCategoryIndex = 0
            )
        }
    }

    fun dismissAiInfoBanner() {
        aiUsageTracker.markAiInfoBannerShown()
        _uiState.update { it.copy(showAiInfoBanner = false) }
    }

    fun dismissLimitMessage() {
        _uiState.update { it.copy(dashboardLimitMessage = null) }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `cd /Users/frank/proggs/BestJournalAndroid && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardViewModel.kt
git commit -m "#943 - Add dashboard limit checks to DashboardViewModel"
```

---

## Task 5: Show Limit Messages in DashboardScreen

**Files:**
- Modify: `app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt`

- [ ] **Step 1: Add limit message UI after the loading section**

Find this block in DashboardScreen (around line 126-133):

```kotlin
            if (uiState.isLoading) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerLoadingEffect(height = 60.dp, cornerRadius = 16.dp)
                        Text("Dashboard wird aktualisiert...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
```

Add this block directly AFTER it:

```kotlin
            // Dashboard limit message
            if (uiState.dashboardLimitMessage != null) {
                item {
                    GlassCard(glowIntensity = 0.1f) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "\u23F3",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.dashboardLimitMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.dismissLimitMessage() }) {
                                Text("Verstanden", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
```

- [ ] **Step 2: Build to verify**

Run: `cd /Users/frank/proggs/BestJournalAndroid && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bestjournal/app/ui/screens/dashboard/DashboardScreen.kt
git commit -m "#943 - Show cooldown and daily limit messages in DashboardScreen"
```

---

## Task 6: Update AiLimitReachedSheet with Real Prices

**Files:**
- Modify: `app/src/main/java/com/bestjournal/app/ui/components/AiLimitReachedSheet.kt`

- [ ] **Step 1: Update the savings text from 20% to 33%**

Find this line:

```kotlin
                Text("Jahresabo \u2014 $yearlyPrice/Jahr (spare 20%)")
```

Replace with:

```kotlin
                Text("Jahresabo \u2014 $yearlyPrice/Jahr (spare 33%)")
```

- [ ] **Step 2: Build to verify**

Run: `cd /Users/frank/proggs/BestJournalAndroid && ./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/bestjournal/app/ui/components/AiLimitReachedSheet.kt
git commit -m "#943 - Update AiLimitReachedSheet savings text to 33%"
```

---

## Task 7: Build, Install, and Verify on Device

- [ ] **Step 1: Full clean build**

```bash
cd /Users/frank/proggs/BestJournalAndroid
./gradlew clean assembleDebug
```

- [ ] **Step 2: Install on device**

```bash
adb -s R52W800205N install -r app/build/outputs/apk/debug/app-debug.apk
adb -s R52W800205N shell am force-stop com.bestjournal.app.debug
adb -s R52W800205N shell am start -n com.bestjournal.app.debug/com.bestjournal.app.MainActivity
```

- [ ] **Step 3: Manual test checklist**

Verify on device:
1. Dashboard refresh works normally (first 16 times)
2. After several refreshes, check that the counter increments (via logcat)
3. App doesn't crash
4. Entries still work (text improvement + summary)

- [ ] **Step 4: Final commit with all changes**

```bash
git stash
git fetch origin
git rebase origin/main
git stash pop
git add -A BestJournalAndroid/ docs/superpowers/
git commit -m "#943 - Implement token cost controls: dashboard daily limit (16/20), entry limit 40, spam protection, pricing €3.99/€39.99"
git push
```
