package com.bestjournal.app.ui.screens.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.R
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.data.local.dao.EntryFollowUpDao
import com.bestjournal.app.data.local.dao.EntryPhotoDao
import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.remote.googledrive.DriveBackupManager
import com.bestjournal.app.data.remote.googledrive.NeedConsentException
import com.bestjournal.app.domain.model.UserProfile
import com.bestjournal.app.domain.usecase.ImproveTextUseCase
import com.bestjournal.app.domain.usecase.RecordAudioUseCase
import com.bestjournal.app.domain.usecase.SignInWithGoogleUseCase
import com.bestjournal.app.domain.usecase.SyncWithDriveUseCase
import com.bestjournal.app.domain.usecase.TranscribeAudioUseCase
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.DailyReminderManager
import com.bestjournal.app.util.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val userProfile: UserProfile? = null,
    val textImprovementDefault: Boolean = false,
    val maxRecordingDuration: Int = 5,
    val autoUpdateDashboard: Boolean = true,
    val isDarkTheme: Boolean = false,
    val followSystem: Boolean = false,
    val selectedThemeKey: String = "neutral",
    val biometricLock: Boolean = false,
    val lastSyncTimestamp: Long? = null,
    val backupPhotos: Boolean = false,
    val backupVideos: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val showLogoutDialog: Boolean = false,
    val consentIntent: Intent? = null,
    val isSubscribed: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val weeklyReviewEnabled: Boolean = true,
    val weeklyReviewDay: Int = java.util.Calendar.SUNDAY,
    val weeklyReviewHour: Int = 15,
    val weeklyReviewMinute: Int = 0,
    val monthlyReviewEnabled: Boolean = true,
    val yearlyReviewEnabled: Boolean = true,
    val userTimezone: String = "",
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    // Locale-safe way to color the export message red vs green — replaces
    // the former `msg.startsWith("Fehler")` check which only worked in German.
    val exportIsError: Boolean = false,
    // Account-deletion state (DSGVO Art. 17 / CCPA Right to Delete).
    val deleteAccountInProgress: Boolean = false,
    // null = no error; non-null reason string triggers the error dialog in the screen.
    val deleteAccountDriveError: String? = null,
    // Voice-input state for the Custom Prompt dialog (Individuelle Analyse).
    // Mirrors the Journal mic/pen flow: record → transcribe → optional improve.
    val promptRecState: PromptRecState = PromptRecState.IDLE,
    // One-shot events consumed by the dialog (dialog owns the text field).
    val promptPendingTranscription: PromptTranscription? = null,
    val promptPendingImprovement: String? = null,
    val promptTranscriptionModel: String? = null,
    val promptError: String? = null,
)

data class PromptTranscription(val text: String, val model: String)

enum class PromptRecState {
    IDLE,
    RECORDING,
    TRANSCRIBING,
    IMPROVING,
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val signInUseCase: SignInWithGoogleUseCase,
    private val syncUseCase: SyncWithDriveUseCase,
    private val encryptedPrefs: SharedPreferences,
    private val billingManager: BillingManager,
    private val reminderManager: DailyReminderManager,
    private val journalEntryDao: JournalEntryDao,
    private val entryPhotoDao: EntryPhotoDao,
    private val entryFollowUpDao: EntryFollowUpDao,
    val analyticsTracker: AnalyticsTracker,
    private val profileChangeBus: com.bestjournal.app.util.ProfileChangeBus,
    private val driveBackupManager: DriveBackupManager,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val improveTextUseCase: ImproveTextUseCase,
) : ViewModel() {

    // Expose amplitude for potential animation inside the prompt dialog.
    val promptAmplitude: StateFlow<Float> = recordAudioUseCase.amplitude

    private var promptAudioFile: java.io.File? = null
    private var promptRecordingJob: kotlinx.coroutines.Job? = null

    /** Call this whenever the user switches the dashboard scenario / profile. */
    fun notifyProfileChanged() {
        profileChangeBus.notifyProfileChanged()
    }

    fun launchSubscription(activity: Activity, isYearly: Boolean) {
        billingManager.launchPurchaseFlow(activity, isYearly)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    /**
     * Forces the BillingManager to re-query Google Play so the displayed subscription state, promo
     * countdown and current price reflect any renewals or plan changes that happened since the
     * BillingClient first connected. Called from SettingsScreen on each open.
     */
    fun refreshSubscriptionStatus() {
        billingManager.refreshSubscriptionStatus()
    }

    val subscriptionType = billingManager.subscriptionType

    fun getCurrentPrice(): String {
        // Priority order:
        // 1. Server-authoritative price from Cloud Function (knows the active
        //    pricing phase — intro vs. recurring — which BillingClient does not).
        val serverMicros = billingManager.serverCurrentPriceMicros.value
        if (serverMicros > 0) {
            val currency = billingManager.serverCurrentPriceCurrency.value
            val formatted = formatMicrosAsPrice(serverMicros, currency)
            if (formatted != null) return formatted
        }
        // 2. Local active-base-plan price (for retention/promo offers).
        billingManager.getActiveBasePlanPrice()?.let {
            return it
        }
        // 3. Fallback to the main base plan price.
        return when (billingManager.subscriptionType.value) {
            com.bestjournal.app.billing.SubscriptionType.YEARLY ->
                billingManager.yearlyPrice.value.ifEmpty { Constants.YEARLY_PRICE_DISPLAY }
            else -> billingManager.monthlyPrice.value.ifEmpty { Constants.MONTHLY_PRICE_DISPLAY }
        }
    }

    /**
     * Format micros (e.g. 3_990_000) as a localised price string ("3,99 €"). Returns null if the
     * inputs are invalid so the caller can fall back.
     */
    private fun formatMicrosAsPrice(micros: Long, currencyCode: String): String? {
        if (micros <= 0L) return null
        val amount = micros / 1_000_000.0
        val symbol =
            when (currencyCode.uppercase()) {
                "EUR" -> " €"
                "USD" -> " $"
                "GBP" -> " £"
                "" -> ""
                else -> " $currencyCode"
            }
        // German formatting (comma decimal separator) — matches BillingClient output.
        val rounded = String.format(java.util.Locale.GERMANY, "%.2f", amount)
        return "$rounded$symbol"
    }

    /**
     * Loop-7: live snapshot of the subscription's CURRENT pricing phase, fed directly by Google's
     * Subscriptions API v2 via the Firebase cloud function. There is intentionally no counter or
     * local guess — the dialog renders exactly what Google says right now:
     * - currentPrice actual price being charged in the active phase
     * - baseAfterwardsPrice regular recurring price after the intro phase
     * - phaseEndDate ISO-8601 timestamp when the active phase ends (= next renewal, or end of
     *   service if cancelled)
     *
     * Only set when offerPhase == "INTRO" — i.e. the user is truly in the promotional pricing
     * phase. BASE phase shows no extra promo card.
     */
    data class PromoInfo(
        val currentPrice: String,
        val baseAfterwardsPrice: String,
        val phaseEndDate: String,
    )

    fun getActivePromoInfo(): PromoInfo? = promoInfoState.value

    /**
     * Reactive promo info — observable from Compose. Re-emits whenever the cloud function delivers
     * a fresh offerPhase / expiryTime / price for the active subscription.
     */
    val promoInfoState: StateFlow<PromoInfo?> =
        combine(
                billingManager.subscriptionType,
                billingManager.monthlyPrice,
                billingManager.expiryTime,
                billingManager.serverCurrentPriceMicros,
                billingManager.serverCurrentPriceCurrency,
                billingManager.activeBasePlanId,
            ) { values: Array<Any?> ->
                val type = values[0] as com.bestjournal.app.billing.SubscriptionType
                val monthlyPrice = values[1] as String
                val expiry = values[2] as String?
                val priceMicros = values[3] as Long
                val priceCurrency = values[4] as String
                val activeBasePlan = values[5] as String
                if (type != com.bestjournal.app.billing.SubscriptionType.MONTHLY)
                    return@combine null
                if (expiry.isNullOrBlank()) return@combine null
                if (priceMicros <= 0L || monthlyPrice.isBlank()) return@combine null
                // Loop-10 fix (Frank, 2026-04-30): observe activeBasePlanId as a
                // proper StateFlow so this combine re-fires the second the cloud
                // confirms a plan switch. Show the "Sonderpreis aktiv, danach
                // 3,99 €" notice only when the user is on the main "monthly" plan
                // (or the very first install where the value is still empty).
                // Retention plans have a permanent lower price, no return to base.
                val onMainMonthlyPlan = activeBasePlan.isEmpty() || activeBasePlan == "monthly"
                if (!onMainMonthlyPlan) return@combine null
                // Phase comparison: only the "main" monthly plan can be in INTRO.
                // priceMicros < baseMicros => still benefitting from the discount.
                val baseMicros = parsePriceToMicros(monthlyPrice) ?: return@combine null
                if (priceMicros >= baseMicros) return@combine null
                val currentPrice =
                    formatMicrosAsPrice(priceMicros, priceCurrency) ?: return@combine null
                PromoInfo(
                    currentPrice = currentPrice,
                    baseAfterwardsPrice = monthlyPrice,
                    phaseEndDate = expiry,
                )
            }
            .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    /**
     * Loop-7 helper: parse a localised "3,99 €" / "$3.99" / "3.99 EUR" formatted price back into
     * micros so promoInfoState can compare it against the cloud-delivered priceMicros. Returns null
     * when the input is empty or contains no numeric portion.
     */
    private fun parsePriceToMicros(formatted: String): Long? {
        if (formatted.isBlank()) return null
        val numStr = formatted.replace("[^0-9,.]".toRegex(), "")
        if (numStr.isBlank()) return null
        val lastComma = numStr.lastIndexOf(',')
        val lastDot = numStr.lastIndexOf('.')
        val usesCommaDecimal = lastComma > lastDot && numStr.length - lastComma <= 3
        val normalised =
            if (usesCommaDecimal) {
                numStr.replace(".", "").replace(",", ".")
            } else {
                numStr.replace(",", "")
            }
        val amount = normalised.toDoubleOrNull() ?: return null
        return (amount * 1_000_000.0).toLong()
    }

    /** Reactive expiry timestamp (ISO-8601) for the CURRENTLY active phase. */
    val expiryTimeState: StateFlow<String?> = billingManager.expiryTime

    /**
     * Loop-10 fix (Frank, 2026-04-30): the previous incarnation of this flow read
     * PREF_ACTIVE_BASE_PLAN_ID inside a map { } lambda gated on subscriptionType — but a switch
     * from "monthly" to "retention-monthly-75" keeps subscriptionType=MONTHLY on both sides of the
     * change. The lambda never re-fired and the flow stayed stuck on its initial value. Now we
     * observe BillingManager's activeBasePlanId StateFlow directly: every code path that writes
     * PREF_ACTIVE_BASE_PLAN_ID also writes _activeBasePlanId, so this derived flow updates the
     * second the cloud function (or a fresh purchase) confirms the new base plan.
     */
    val isOnRetentionPlanState: StateFlow<Boolean> =
        billingManager.activeBasePlanId
            .map { basePlan ->
                basePlan == Constants.RETENTION_OFFER_ID_MONTHLY ||
                    basePlan == Constants.RETENTION_OFFER_ID_YEARLY
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue =
                    run {
                        val initial = billingManager.activeBasePlanId.value
                        initial == Constants.RETENTION_OFFER_ID_MONTHLY ||
                            initial == Constants.RETENTION_OFFER_ID_YEARLY
                    },
            )

    /**
     * Reactive current price for the active subscription. Re-evaluates when any of the price
     * sources update — including the server-authoritative currentPriceMicros from the Cloud
     * Function (which knows the active pricing phase).
     */
    val currentPriceState: StateFlow<String> =
        combine(
                billingManager.subscriptionType,
                billingManager.monthlyPrice,
                billingManager.yearlyPrice,
                billingManager.serverCurrentPriceMicros,
                billingManager.offerPhase,
            ) { _, _, _, _, _ ->
                getCurrentPrice()
            }
            .stateIn(
                scope = viewModelScope,
                // Eagerly so the dialog never gets a null/empty initial value.
                started = SharingStarted.Eagerly,
                initialValue = getCurrentPrice(),
            )

    /** Reactive retention offer price for the active subscription type. */
    val retentionPriceState: StateFlow<String?> =
        combine(
                billingManager.subscriptionType,
                billingManager.monthlyPrice,
                billingManager.yearlyPrice,
            ) { _, _, _ ->
                getRetentionPrice()
            }
            .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null)

    /** Reactive auto-renewing flag — false after user cancels in Google Play. */
    val autoRenewingState: StateFlow<Boolean> = billingManager.autoRenewing

    private fun formatHalfPrice(originalPrice: String): String? {
        if (originalPrice.isBlank()) return null
        val numStr = originalPrice.replace("[^0-9,.]".toRegex(), "")
        if (numStr.isBlank()) return null
        val lastComma = numStr.lastIndexOf(',')
        val lastDot = numStr.lastIndexOf('.')
        val usesCommaDecimal = lastComma > lastDot && numStr.length - lastComma <= 3
        val normalized =
            if (usesCommaDecimal) {
                numStr.replace(".", "").replace(",", ".")
            } else {
                numStr.replace(",", "")
            }
        val amount = normalized.toDoubleOrNull() ?: return null
        val halfAmount = amount * 0.5
        val hasDecimals =
            (usesCommaDecimal && lastComma >= 0) || (!usesCommaDecimal && lastDot >= 0)
        val formatted =
            if (hasDecimals) {
                val f = String.format(java.util.Locale.US, "%.2f", halfAmount)
                if (usesCommaDecimal) f.replace(".", ",") else f
            } else {
                halfAmount.toInt().toString()
            }
        return originalPrice.replaceFirst(Regex("[0-9][0-9.,   ]*[0-9]|[0-9]+"), formatted)
    }

    fun getRetentionPrice(): String? {
        val isYearly =
            billingManager.subscriptionType.value ==
                com.bestjournal.app.billing.SubscriptionType.YEARLY
        return billingManager.getRetentionPrice(isYearly)
    }

    fun launchRetentionOffer(activity: Activity) {
        val isYearly =
            billingManager.subscriptionType.value ==
                com.bestjournal.app.billing.SubscriptionType.YEARLY
        val offerToken = billingManager.getRetentionOfferToken(isYearly)
        if (offerToken != null) {
            billingManager.launchPurchaseFlow(
                activity,
                isYearly = isYearly,
                promoOfferToken = offerToken,
            )
        } else {
            // Fallback: open Google Play subscription management
            try {
                val intent =
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://play.google.com/store/account/subscriptions"),
                    )
                activity.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    // Listen for external changes (theme toggle, auto-backup timestamp, etc.)
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Constants.PREF_DARK_THEME,
            Constants.PREF_THEME_FOLLOW_SYSTEM,
            Constants.PREF_APP_THEME -> {
                _uiState.value =
                    _uiState.value.copy(
                        isDarkTheme = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false),
                        followSystem =
                            encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false),
                        selectedThemeKey =
                            encryptedPrefs.getString(Constants.PREF_APP_THEME, "neutral")
                                ?: "neutral",
                    )
            }
            Constants.PREF_LAST_SYNC_TIMESTAMP -> {
                _uiState.value =
                    _uiState.value.copy(
                        lastSyncTimestamp =
                            encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L).takeIf {
                                it > 0
                            }
                    )
            }
        }
    }

    init {
        // Bug-Fix (Loop-4): Read the BillingManager StateFlow's CURRENT value
        // synchronously so loadSettings() does not start with the stale default
        // isSubscribed=false. Without this, premium users briefly see the
        // "Premium kaufen" screen on every Settings open while the collect
        // below is still warming up.
        val initialSubscribed =
            billingManager.subscriptionState.value is SubscriptionState.Subscribed
        loadSettings(initialSubscribed)
        encryptedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
        // Ensure review alarms are scheduled (default: enabled)
        try {
            reminderManager.ensureWeeklyReviewScheduled()
            reminderManager.ensureMonthlyReviewScheduled()
            reminderManager.ensureYearlyReviewScheduled()
        } catch (e: Exception) {
            android.util.Log.e("SettingsVM", "Failed to schedule review alarms: ${e.message}")
        }
        viewModelScope.launch {
            billingManager.subscriptionState.collect { state ->
                _uiState.value =
                    _uiState.value.copy(isSubscribed = state is SubscriptionState.Subscribed)
            }
        }
        if (signInUseCase.getProfile() != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val merged = syncUseCase.mergeFromDrive()
                    if (merged > 0) android.util.Log.d("SettingsVM", "Auto-merged $merged entries")
                } catch (e: Exception) {
                    android.util.Log.e("SettingsVM", "Auto-merge failed: ${e.message}")
                }
                // Pull the custom-analysis prompt from Drive if another device
                // uploaded a newer copy — runs every time Settings opens so a
                // second phone without a fresh re-install still gets it.
                try {
                    val updated = syncUseCase.syncCustomPromptFromDriveIfNewer()
                    if (updated) {
                        android.util.Log.d("SettingsVM", "Custom prompt pulled from Drive")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SettingsVM", "Custom prompt sync failed: ${e.message}")
                }
                try {
                    val count = syncUseCase.downloadMissingPhotos()
                    if (count > 0) android.util.Log.d("SettingsVM", "Photo download: $count files")
                } catch (e: Exception) {
                    android.util.Log.e("SettingsVM", "Photo download failed: ${e.message}")
                } finally {
                    encryptedPrefs.edit().putBoolean(Constants.PREF_RESTORE_PENDING, false).apply()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        encryptedPrefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun loadSettings(initialSubscribed: Boolean = false) {
        _uiState.value =
            SettingsUiState(
                isSubscribed = initialSubscribed,
                userProfile = signInUseCase.getProfile(),
                textImprovementDefault =
                    encryptedPrefs.getBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, false),
                maxRecordingDuration =
                    encryptedPrefs.getInt(Constants.PREF_MAX_RECORDING_DURATION, 5),
                autoUpdateDashboard =
                    encryptedPrefs.getBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, true),
                isDarkTheme = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false),
                followSystem = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false),
                selectedThemeKey =
                    encryptedPrefs.getString(Constants.PREF_APP_THEME, "neutral") ?: "neutral",
                biometricLock = encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false),
                backupPhotos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_PHOTOS, true),
                backupVideos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_VIDEOS, true),
                lastSyncTimestamp =
                    encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L).takeIf {
                        it > 0
                    },
                reminderEnabled = reminderManager.isReminderEnabled(),
                reminderHour = reminderManager.getReminderHour(),
                reminderMinute = reminderManager.getReminderMinute(),
                weeklyReviewEnabled = reminderManager.isWeeklyReviewEnabled(),
                weeklyReviewDay = reminderManager.getWeeklyReviewDay(),
                weeklyReviewHour = reminderManager.getWeeklyReviewHour(),
                weeklyReviewMinute = reminderManager.getWeeklyReviewMinute(),
                monthlyReviewEnabled = reminderManager.isMonthlyReviewEnabled(),
                yearlyReviewEnabled = reminderManager.isYearlyReviewEnabled(),
                userTimezone = encryptedPrefs.getString(Constants.PREF_USER_TIMEZONE, "") ?: "",
            )
    }

    fun updateDarkTheme(enabled: Boolean) {
        encryptedPrefs
            .edit()
            .putBoolean(Constants.PREF_DARK_THEME, enabled)
            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
            .apply()
        _uiState.value =
            _uiState.value.copy(isDarkTheme = enabled, followSystem = false)
    }

    fun updateFollowSystem(enabled: Boolean) {
        encryptedPrefs
            .edit()
            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, enabled)
            .putBoolean(Constants.PREF_DARK_THEME, false)
            .apply()
        _uiState.value =
            _uiState.value.copy(followSystem = enabled, isDarkTheme = false)
    }

    fun updateSelectedTheme(themeKey: String) {
        encryptedPrefs.edit().putString(Constants.PREF_APP_THEME, themeKey).apply()
        _uiState.value = _uiState.value.copy(selectedThemeKey = themeKey)
    }

    fun updateTextImprovementDefault(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, enabled).apply()
        _uiState.value = _uiState.value.copy(textImprovementDefault = enabled)
    }

    fun updateAutoUpdateDashboard(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, enabled).apply()
        _uiState.value = _uiState.value.copy(autoUpdateDashboard = enabled)
    }

    fun updateBiometricLock(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_BIOMETRIC_LOCK, enabled).apply()
        _uiState.value = _uiState.value.copy(biometricLock = enabled)
    }

    fun setBackupPhotos(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_BACKUP_PHOTOS, enabled).apply()
        _uiState.value = _uiState.value.copy(backupPhotos = enabled)
    }

    fun setBackupVideos(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_BACKUP_VIDEOS, enabled).apply()
        _uiState.value = _uiState.value.copy(backupVideos = enabled)
    }

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            syncUseCase
                .backup()
                .onSuccess {
                    _uiState.value =
                        _uiState.value.copy(
                            isSyncing = false,
                            syncMessage = context.getString(R.string.settings_sync_success),
                            lastSyncTimestamp = System.currentTimeMillis(),
                        )
                    // Auto-dismiss message after 3 seconds
                    delay(3000)
                    _uiState.value = _uiState.value.copy(syncMessage = null)
                }
                .onFailure { error ->
                    if (error is NeedConsentException) {
                        _uiState.value =
                            _uiState.value.copy(
                                isSyncing = false,
                                consentIntent = error.consentIntent,
                            )
                    } else {
                        _uiState.value =
                            _uiState.value.copy(
                                isSyncing = false,
                                syncMessage =
                                    context.getString(R.string.error_generic, error.message ?: ""),
                            )
                    }
                }
        }
    }

    fun restoreFromCloud(context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncMessage = null)
            syncUseCase
                .restore()
                .onSuccess {
                    // Restart app so Room picks up the restored database
                    val intent =
                        context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    )
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }
                .onFailure { error ->
                    if (error is NeedConsentException) {
                        _uiState.value =
                            _uiState.value.copy(
                                isSyncing = false,
                                consentIntent = error.consentIntent,
                            )
                    } else {
                        _uiState.value =
                            _uiState.value.copy(
                                isSyncing = false,
                                syncMessage =
                                    context.getString(R.string.error_generic, error.message ?: ""),
                            )
                    }
                }
        }
    }

    fun signIn(activityContext: android.content.Context) {
        viewModelScope.launch {
            signInUseCase(activityContext)
                .onSuccess { profile ->
                    android.util.Log.e("SIGNIN", "=== onSuccess called: ${profile.email} ===")
                    _uiState.value = _uiState.value.copy(userProfile = profile, syncMessage = null)
                    // Auto-restore backup
                    try {
                        android.util.Log.e("AutoRestore", "Checking for backup...")
                        val hasBackup = syncUseCase.hasBackup()
                        android.util.Log.w("AutoRestore", "hasBackup = $hasBackup")
                        if (hasBackup) {
                            // Block retrospective generation until restore+download complete.
                            // commit() (not apply()) so the flag lands on disk BEFORE
                            // Runtime.exit(0) below — async apply() would be discarded.
                            @Suppress("ApplySharedPref")
                            encryptedPrefs
                                .edit()
                                .putBoolean(Constants.PREF_RESTORE_PENDING, true)
                                .commit()
                            val result = syncUseCase.restore()
                            android.util.Log.w(
                                "AutoRestore",
                                "restore result: success=${result.isSuccess}, failure=${result.exceptionOrNull()?.message}",
                            )
                            if (result.isSuccess) {
                                // Restart to load restored database
                                val intent =
                                    activityContext.packageManager.getLaunchIntentForPackage(
                                        activityContext.packageName
                                    )
                                intent?.addFlags(
                                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                        android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                )
                                activityContext.startActivity(intent)
                                Runtime.getRuntime().exit(0)
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e(
                            "AutoRestore",
                            "Auto-restore CRASHED: ${e.javaClass.simpleName}: ${e.message}",
                            e,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            syncMessage =
                                context.getString(
                                    R.string.settings_signin_failed,
                                    error.message ?: "",
                                )
                        )
                }
        }
    }

    fun updateWeeklyReviewEnabled(enabled: Boolean) {
        if (enabled) {
            reminderManager.scheduleWeeklyReview()
        } else {
            reminderManager.cancelWeeklyReview()
        }
        _uiState.value = _uiState.value.copy(weeklyReviewEnabled = enabled)
    }

    fun updateMonthlyReviewEnabled(enabled: Boolean) {
        if (enabled) reminderManager.scheduleMonthlyReview()
        else reminderManager.cancelMonthlyReview()
        _uiState.value = _uiState.value.copy(monthlyReviewEnabled = enabled)
    }

    fun updateYearlyReviewEnabled(enabled: Boolean) {
        if (enabled) reminderManager.scheduleYearlyReview()
        else reminderManager.cancelYearlyReview()
        _uiState.value = _uiState.value.copy(yearlyReviewEnabled = enabled)
    }

    fun setUserTimezone(timezone: String) {
        encryptedPrefs.edit().putString(Constants.PREF_USER_TIMEZONE, timezone).apply()
        _uiState.value = _uiState.value.copy(userTimezone = timezone)
    }

    fun updateWeeklyReviewSchedule(dayOfWeek: Int, hour: Int, minute: Int) {
        reminderManager.scheduleWeeklyReview(dayOfWeek, hour, minute)
        _uiState.value =
            _uiState.value.copy(
                weeklyReviewDay = dayOfWeek,
                weeklyReviewHour = hour,
                weeklyReviewMinute = minute,
            )
    }

    fun updateReminderEnabled(enabled: Boolean) {
        if (enabled) {
            val hour = _uiState.value.reminderHour
            val minute = _uiState.value.reminderMinute
            reminderManager.scheduleReminder(hour, minute)
            analyticsTracker.trackReminderEnabled()
        } else {
            reminderManager.cancelReminder()
            analyticsTracker.trackReminderDisabled()
        }
        _uiState.value = _uiState.value.copy(reminderEnabled = enabled)
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(reminderHour = hour, reminderMinute = minute)
        if (_uiState.value.reminderEnabled) {
            reminderManager.scheduleReminder(hour, minute)
        } else {
            // Just persist the time even if not enabled yet
            encryptedPrefs
                .edit()
                .putInt(Constants.PREF_REMINDER_HOUR, hour)
                .putInt(Constants.PREF_REMINDER_MINUTE, minute)
                .apply()
        }
        analyticsTracker.trackReminderTimeChanged(hour)
    }

    fun showLogoutDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showLogoutDialog = show)
    }

    fun clearConsentIntent() {
        _uiState.value = _uiState.value.copy(consentIntent = null)
    }

    fun exportToPdf(context: android.content.Context, uri: Uri, includePhotos: Boolean = false) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(isExporting = true, exportMessage = null, exportIsError = false)
            try {
                val entries = journalEntryDao.getAllEntriesOnce()
                if (entries.isEmpty()) {
                    _uiState.value =
                        _uiState.value.copy(
                            isExporting = false,
                            exportMessage = context.getString(R.string.settings_export_no_entries),
                            exportIsError = false,
                        )
                    delay(3000)
                    _uiState.value = _uiState.value.copy(exportMessage = null)
                    return@launch
                }

                // PDF generation is CPU+IO intensive — run off Main thread
                val count =
                    withContext(Dispatchers.IO) {
                        // Build photo map if requested
                        val photosPerEntry =
                            if (includePhotos) {
                                entries.associate { entry ->
                                    entry.id to entryPhotoDao.getPhotoOnlyForEntryOnce(entry.id)
                                }
                            } else {
                                emptyMap()
                            }

                        // Always include Nachtraege in the PDF — they are part
                        // of the entry content, not an optional extra like photos.
                        val followUpsPerEntry = entries.associate { entry ->
                            entry.id to entryFollowUpDao.getForEntryOnce(entry.id)
                        }

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            PdfExporter.export(
                                entries,
                                outputStream,
                                photosPerEntry,
                                context,
                                followUpsPerEntry,
                            )
                        }
                    }

                if (count != null) {
                    analyticsTracker.trackExportCompleted(count)
                    _uiState.value =
                        _uiState.value.copy(
                            isExporting = false,
                            exportMessage =
                                context.resources.getQuantityString(
                                    R.plurals.settings_export_success,
                                    count,
                                    count,
                                ),
                            exportIsError = false,
                        )
                } else {
                    _uiState.value =
                        _uiState.value.copy(
                            isExporting = false,
                            exportMessage = context.getString(R.string.error_file_open_failed),
                            exportIsError = true,
                        )
                }

                delay(4000)
                _uiState.value = _uiState.value.copy(exportMessage = null)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        isExporting = false,
                        exportMessage = context.getString(R.string.error_generic, e.message ?: ""),
                        exportIsError = true,
                    )
                delay(4000)
                _uiState.value = _uiState.value.copy(exportMessage = null)
            }
        }
    }

    /**
     * Full account deletion per Google Play 2024 requirement + Art. 17 DSGVO / CCPA Right to
     * Delete. Removes:
     * - Drive appDataFolder backup (verified empty afterwards — see
     *   [DriveBackupManager.deleteAllAppData])
     * - App sign-in state (cached Google ID profile in encrypted preferences — not the Google
     *   account itself)
     * - Local on-device photos and videos (filesDir/photos)
     * - Cached audio recordings and temp DB snapshots from cacheDir — these can contain journal
     *   content
     * - Everything signOut() also removes: local Room DBs, encrypted prefs, reminder alarms
     *
     * Bombensicher-Semantik:
     * - Drive deletion is verified and runs FIRST. On hard failure (timeout/network/verify
     *   mismatch), we STOP and expose the error via [SettingsUiState.deleteAccountDriveError] so
     *   the UI can show an honest dialog instead of pretending deletion succeeded.
     * - The user can then retry or explicitly choose "delete locally anyway" via
     *   [forceLocalDelete].
     * - Restarts the app process only when local deletion completes.
     */
    fun deleteAccount(context: android.content.Context, forceLocalDelete: Boolean = false) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(deleteAccountInProgress = true, deleteAccountDriveError = null)

            // 1) Drive appDataFolder — verified delete. Runs FIRST because signOut() clears the
            //    Google account email needed to authenticate the Drive API.
            if (!forceLocalDelete) {
                val driveResult = driveBackupManager.deleteAllAppData()
                if (
                    driveResult
                        is
                        com.bestjournal.app.data.remote.googledrive.DriveBackupManager.DeleteAllResult.Failed
                ) {
                    android.util.Log.w(
                        "DeleteAccount",
                        "Drive deletion failed: ${driveResult.reason} — stopping for user confirmation",
                    )
                    _uiState.value =
                        _uiState.value.copy(
                            deleteAccountInProgress = false,
                            deleteAccountDriveError = driveResult.reason,
                        )
                    return@launch
                }
                android.util.Log.d("DeleteAccount", "Drive appDataFolder cleanup OK: $driveResult")
            } else {
                android.util.Log.w(
                    "DeleteAccount",
                    "User chose to skip Drive deletion — proceeding with local-only wipe",
                )
            }

            // 2) Local photo/video directory — not cleared by signOut().
            try {
                java.io.File(context.filesDir, "photos").deleteRecursively()
            } catch (e: Exception) {
                android.util.Log.w("DeleteAccount", "Photo dir cleanup skipped: ${e.message}")
            }

            // 3) cacheDir — contains audio recordings (recording_*.wav), Edge-TTS cache
            //    (tts_audio.mp3), and — CRITICALLY — drive_merge_temp.db which holds a FULL
            //    copy of the journal database during sync. Wiping the whole cache is safe
            //    because cache content is by definition regenerable.
            try {
                context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            } catch (e: Exception) {
                android.util.Log.w("DeleteAccount", "Cache cleanup skipped: ${e.message}")
            }

            // 4) Fall through to signOut: clears Room DBs, encrypted prefs, alarms, restarts app.
            signOut(context)
        }
    }

    /** Dismiss the Drive-delete error dialog without retrying or forcing local delete. */
    fun dismissDeleteAccountError() {
        _uiState.value = _uiState.value.copy(deleteAccountDriveError = null)
    }

    fun signOut(context: android.content.Context) {
        try {
            // Cancel any active alarms before clearing prefs
            reminderManager.cancelReminder()
            reminderManager.cancelWeeklyReview()

            // Save device-specific settings BEFORE clearing everything
            val isDark = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, true)
            val biometricLock = encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false)

            // Clear ALL prefs, then restore only device-specific ones — single atomic operation
            encryptedPrefs
                .edit()
                .clear()
                .putBoolean(Constants.PREF_DARK_THEME, isDark)
                .putBoolean(Constants.PREF_BIOMETRIC_LOCK, biometricLock)
                .commit() // commit() is synchronous — guarantees write before restart

            // Delete local databases — data belongs to the account
            context.deleteDatabase("entropy_journal_db")
            context.getDatabasePath("entropy_journal_db-wal")?.delete()
            context.getDatabasePath("entropy_journal_db-shm")?.delete()
            context.deleteDatabase("retrospective_db")
            context.getDatabasePath("retrospective_db-wal")?.delete()
            context.getDatabasePath("retrospective_db-shm")?.delete()
            context.deleteDatabase("dashboard_db")
            context.getDatabasePath("dashboard_db-wal")?.delete()
            context.getDatabasePath("dashboard_db-shm")?.delete()
            // Reset one-time cleanup flag so reviews are regenerated fresh after next sign-in
            java.io.File(context.filesDir, ".retro_cleaned_v3").delete()
        } catch (e: Exception) {
            android.util.Log.e("Settings", "Sign-out cleanup failed", e)
        }

        // Restart the app process so Room clears its in-memory cache
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }

    // --- Custom-Prompt voice input (mirrors JournalViewModel.toggleRecording/improveText) ---

    fun togglePromptRecording() {
        when (_uiState.value.promptRecState) {
            PromptRecState.RECORDING -> stopPromptRecording()
            PromptRecState.IDLE -> startPromptRecording()
            else -> Unit
        }
    }

    private fun startPromptRecording() {
        val audioFile =
            java.io.File(context.cacheDir, "prompt_recording_${System.currentTimeMillis()}.wav")
        promptAudioFile = audioFile
        _uiState.value =
            _uiState.value.copy(
                promptRecState = PromptRecState.RECORDING,
                promptError = null,
                promptPendingTranscription = null,
                promptPendingImprovement = null,
            )

        promptRecordingJob = viewModelScope.launch {
            // Beep sound before recording
            val soundsEnabled = encryptedPrefs.getBoolean(Constants.PREF_SOUNDS_ENABLED, true)
            val beepMs = 150
            if (soundsEnabled)
                try {
                    val sampleRate = 44100
                    val beepSamples = sampleRate * beepMs / 1000
                    val samples = ShortArray(beepSamples)
                    val freq = 880.0
                    val fadeLen = beepSamples / 8
                    for (i in 0 until beepSamples) {
                        val t = i.toDouble() / sampleRate
                        val envelope =
                            when {
                                i < fadeLen -> i.toDouble() / fadeLen
                                i > beepSamples - fadeLen -> (beepSamples - i).toDouble() / fadeLen
                                else -> 1.0
                            }
                        samples[i] =
                            (Short.MAX_VALUE *
                                    0.5 *
                                    envelope *
                                    kotlin.math.sin(2 * Math.PI * freq * t))
                                .toInt()
                                .toShort()
                    }
                    val track =
                        android.media.AudioTrack(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build(),
                            android.media.AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                            beepSamples * 2,
                            android.media.AudioTrack.MODE_STATIC,
                            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE,
                        )
                    track.write(samples, 0, beepSamples)
                    track.play()
                    delay(beepMs.toLong() + 100)
                    track.release()
                } catch (_: Exception) {
                    /* Tone is optional */
                }

            try {
                recordAudioUseCase.startRecording(audioFile)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        promptRecState = PromptRecState.IDLE,
                        promptError =
                            context.getString(R.string.journal_recording_error, e.message ?: ""),
                    )
            }
        }

        // Auto-stop after max duration
        viewModelScope.launch {
            val maxMinutes = encryptedPrefs.getInt(Constants.PREF_MAX_RECORDING_DURATION, 5)
            delay(maxMinutes * 60 * 1000L)
            if (_uiState.value.promptRecState == PromptRecState.RECORDING) {
                stopPromptRecording()
            }
        }
    }

    private fun stopPromptRecording() {
        recordAudioUseCase.stopRecording()

        viewModelScope.launch {
            promptRecordingJob?.join()
            _uiState.value = _uiState.value.copy(promptRecState = PromptRecState.TRANSCRIBING)

            val audioFile = promptAudioFile ?: return@launch
            transcribeAudioUseCase(audioFile)
                .onSuccess { result ->
                    val transcribed = result.text.trim()
                    _uiState.value =
                        _uiState.value.copy(
                            promptRecState = PromptRecState.IDLE,
                            promptPendingTranscription =
                                if (transcribed.isNotBlank())
                                    PromptTranscription(transcribed, result.model)
                                else null,
                            promptTranscriptionModel = result.model,
                        )
                    audioFile.delete()
                    // Auto-improve is triggered by the dialog after it appends the text
                    // so the ENTIRE field content gets improved, not just the new chunk.
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            promptRecState = PromptRecState.IDLE,
                            promptError =
                                context.getString(
                                    R.string.journal_transcription_error,
                                    error.message ?: "",
                                ),
                        )
                    audioFile.delete()
                }
        }
    }

    /** Improves whatever is CURRENTLY in the text field (not just the latest transcription). */
    fun improvePromptText(fullText: String) {
        if (fullText.isBlank()) return
        _uiState.value = _uiState.value.copy(promptRecState = PromptRecState.IMPROVING)
        viewModelScope.launch {
            improveTextUseCase(fullText)
                .onSuccess { improved ->
                    _uiState.value =
                        _uiState.value.copy(
                            promptRecState = PromptRecState.IDLE,
                            promptPendingImprovement = improved,
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            promptRecState = PromptRecState.IDLE,
                            promptError =
                                context.getString(R.string.error_generic, error.message ?: ""),
                        )
                }
        }
    }

    fun consumePromptTranscription() {
        _uiState.value = _uiState.value.copy(promptPendingTranscription = null)
    }

    fun consumePromptImprovement() {
        _uiState.value = _uiState.value.copy(promptPendingImprovement = null)
    }

    /**
     * Uploads the full list of custom analyses (names + prompts) to Drive. Called after any
     * add/remove/rename/prompt-save in the Individuelle-Analyse settings so all devices see the
     * same list.
     */
    fun backupCustomAnalysesToDrive() {
        viewModelScope.launch {
            try {
                syncUseCase.backupCustomAnalyses()
            } catch (e: Exception) {
                android.util.Log.e(
                    "SettingsViewModel",
                    "Custom analyses backup failed (non-critical): ${e.message}",
                )
            }
        }
    }

    /**
     * Legacy entry point — prefer [backupCustomAnalysesToDrive] which uploads the full list. Kept
     * so any remaining callers continue to work.
     */
    fun backupCustomPromptToDrive(promptText: String) {
        viewModelScope.launch {
            try {
                syncUseCase.backupCustomPrompt(promptText)
            } catch (e: Exception) {
                android.util.Log.e(
                    "SettingsViewModel",
                    "Custom prompt backup failed (non-critical): ${e.message}",
                )
            }
        }
    }

    fun clearPromptVoiceState() {
        promptAudioFile?.delete()
        promptAudioFile = null
        _uiState.value =
            _uiState.value.copy(
                promptRecState = PromptRecState.IDLE,
                promptPendingTranscription = null,
                promptPendingImprovement = null,
                promptTranscriptionModel = null,
                promptError = null,
            )
    }

    fun clearPromptError() {
        _uiState.value = _uiState.value.copy(promptError = null)
    }
}
