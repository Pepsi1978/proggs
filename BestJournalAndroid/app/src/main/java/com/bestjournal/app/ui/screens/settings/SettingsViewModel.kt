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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val userProfile: UserProfile? = null,
    val textImprovementDefault: Boolean = false,
    val maxRecordingDuration: Int = 5,
    val autoUpdateDashboard: Boolean = true,
    val isDarkTheme: Boolean = false,
    val followSystem: Boolean = false,
    val followSun: Boolean = false,
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

    val subscriptionType = billingManager.subscriptionType

    fun getCurrentPrice(): String {
        return when (billingManager.subscriptionType.value) {
            com.bestjournal.app.billing.SubscriptionType.YEARLY ->
                billingManager.yearlyPrice.value.ifEmpty { Constants.YEARLY_PRICE_DISPLAY }
            else -> billingManager.monthlyPrice.value.ifEmpty { Constants.MONTHLY_PRICE_DISPLAY }
        }
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
            Constants.PREF_THEME_FOLLOW_SUN -> {
                _uiState.value =
                    _uiState.value.copy(
                        isDarkTheme = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false),
                        followSystem =
                            encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false),
                        followSun =
                            encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SUN, false),
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
        loadSettings()
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

    private fun loadSettings() {
        _uiState.value =
            SettingsUiState(
                userProfile = signInUseCase.getProfile(),
                textImprovementDefault =
                    encryptedPrefs.getBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, false),
                maxRecordingDuration =
                    encryptedPrefs.getInt(Constants.PREF_MAX_RECORDING_DURATION, 5),
                autoUpdateDashboard =
                    encryptedPrefs.getBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, true),
                isDarkTheme = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, false),
                followSystem = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false),
                followSun = encryptedPrefs.getBoolean(Constants.PREF_THEME_FOLLOW_SUN, false),
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
            .putBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
            .apply()
        _uiState.value =
            _uiState.value.copy(isDarkTheme = enabled, followSystem = false, followSun = false)
    }

    fun updateFollowSystem(enabled: Boolean) {
        encryptedPrefs
            .edit()
            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, enabled)
            .putBoolean(Constants.PREF_DARK_THEME, false)
            .putBoolean(Constants.PREF_THEME_FOLLOW_SUN, false)
            .apply()
        _uiState.value =
            _uiState.value.copy(followSystem = enabled, isDarkTheme = false, followSun = false)
    }

    fun updateFollowSun(enabled: Boolean) {
        encryptedPrefs
            .edit()
            .putBoolean(Constants.PREF_THEME_FOLLOW_SUN, enabled)
            .putBoolean(Constants.PREF_DARK_THEME, false)
            .putBoolean(Constants.PREF_THEME_FOLLOW_SYSTEM, false)
            .apply()
        _uiState.value =
            _uiState.value.copy(followSun = enabled, isDarkTheme = false, followSystem = false)
    }

    fun saveLocation(lat: Double, lon: Double) {
        encryptedPrefs
            .edit()
            .putFloat(Constants.PREF_LATITUDE, lat.toFloat())
            .putFloat(Constants.PREF_LONGITUDE, lon.toFloat())
            .apply()
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

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            PdfExporter.export(entries, outputStream, photosPerEntry, context)
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
     * Full account deletion per Google Play 2024 requirement + Art. 17 DSGVO / CCPA Right to Delete.
     * Removes:
     *  - Drive appDataFolder backup (verified empty afterwards — see [DriveBackupManager.deleteAllAppData])
     *  - App sign-in state (cached Google ID profile in encrypted preferences — not the Google account itself)
     *  - Local on-device photos and videos (filesDir/photos)
     *  - Cached audio recordings and temp DB snapshots from cacheDir — these can contain journal content
     *  - Everything signOut() also removes: local Room DBs, encrypted prefs, reminder alarms
     *
     * Bombensicher-Semantik:
     *  - Drive deletion is verified and runs FIRST. On hard failure (timeout/network/verify mismatch),
     *    we STOP and expose the error via [SettingsUiState.deleteAccountDriveError] so the UI can
     *    show an honest dialog instead of pretending deletion succeeded.
     *  - The user can then retry or explicitly choose "delete locally anyway" via [forceLocalDelete].
     *  - Restarts the app process only when local deletion completes.
     */
    fun deleteAccount(context: android.content.Context, forceLocalDelete: Boolean = false) {
        viewModelScope.launch {
            _uiState.value =
                _uiState.value.copy(
                    deleteAccountInProgress = true,
                    deleteAccountDriveError = null,
                )

            // 1) Drive appDataFolder — verified delete. Runs FIRST because signOut() clears the
            //    Google account email needed to authenticate the Drive API.
            if (!forceLocalDelete) {
                val driveResult = driveBackupManager.deleteAllAppData()
                if (driveResult is com.bestjournal.app.data.remote.googledrive.DriveBackupManager
                        .DeleteAllResult
                        .Failed) {
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
                android.util.Log.d(
                    "DeleteAccount",
                    "Drive appDataFolder cleanup OK: $driveResult",
                )
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

        promptRecordingJob =
            viewModelScope.launch {
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
                                    i > beepSamples - fadeLen ->
                                        (beepSamples - i).toDouble() / fadeLen
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
                                    .setContentType(
                                        android.media.AudioAttributes.CONTENT_TYPE_MUSIC
                                    )
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
