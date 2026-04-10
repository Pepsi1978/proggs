package com.bestjournal.app.ui.screens.settings

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.data.local.dao.JournalEntryDao
import com.bestjournal.app.data.remote.googledrive.NeedConsentException
import com.bestjournal.app.domain.model.UserProfile
import com.bestjournal.app.domain.usecase.SignInWithGoogleUseCase
import com.bestjournal.app.domain.usecase.SyncWithDriveUseCase
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.DailyReminderManager
import com.bestjournal.app.util.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val signInUseCase: SignInWithGoogleUseCase,
    private val syncUseCase: SyncWithDriveUseCase,
    private val encryptedPrefs: SharedPreferences,
    private val billingManager: BillingManager,
    private val reminderManager: DailyReminderManager,
    private val journalEntryDao: JournalEntryDao,
    val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    fun launchSubscription(activity: Activity, isYearly: Boolean) {
        billingManager.launchPurchaseFlow(activity, isYearly)
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
        // Download missing photos in background after app restart (post-restore)
        if (signInUseCase.getProfile() != null) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val count = syncUseCase.downloadMissingPhotos()
                    if (count > 0) {
                        android.util.Log.d("SettingsVM", "Background photo download: $count files")
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "SettingsVM",
                        "Background photo download failed: ${e.message}",
                    )
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
                backupPhotos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_PHOTOS, false),
                backupVideos = encryptedPrefs.getBoolean(Constants.PREF_BACKUP_VIDEOS, false),
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
                            syncMessage = "Erfolgreich gesichert",
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
                                syncMessage = "Fehler: ${error.message}",
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
                                syncMessage = "Fehler: ${error.message}",
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
                            syncMessage = "Anmeldung fehlgeschlagen: ${error.message}"
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

    fun exportToPdf(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, exportMessage = null)
            try {
                val entries = journalEntryDao.getAllEntriesOnce()
                if (entries.isEmpty()) {
                    _uiState.value =
                        _uiState.value.copy(
                            isExporting = false,
                            exportMessage = "Keine Eintr\u00e4ge vorhanden",
                        )
                    delay(3000)
                    _uiState.value = _uiState.value.copy(exportMessage = null)
                    return@launch
                }

                // PDF generation is CPU+IO intensive — run off Main thread
                val count =
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            PdfExporter.export(entries, outputStream)
                        }
                    }

                if (count != null) {
                    analyticsTracker.trackExportCompleted(count)
                    _uiState.value =
                        _uiState.value.copy(
                            isExporting = false,
                            exportMessage = "$count Eintr\u00e4ge exportiert",
                        )
                } else {
                    _uiState.value =
                        _uiState.value.copy(
                            isExporting = false,
                            exportMessage = "Fehler: Datei konnte nicht ge\u00f6ffnet werden",
                        )
                }

                delay(4000)
                _uiState.value = _uiState.value.copy(exportMessage = null)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(isExporting = false, exportMessage = "Fehler: ${e.message}")
                delay(4000)
                _uiState.value = _uiState.value.copy(exportMessage = null)
            }
        }
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

            // Delete local database — data belongs to the account
            context.deleteDatabase("entropy_journal_db")
            context.getDatabasePath("entropy_journal_db-wal")?.delete()
            context.getDatabasePath("entropy_journal_db-shm")?.delete()
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
}
