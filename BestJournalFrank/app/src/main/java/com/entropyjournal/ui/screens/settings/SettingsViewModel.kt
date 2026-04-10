package com.entropyjournal.ui.screens.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.local.dao.JournalEntryDao
import com.entropyjournal.data.remote.googledrive.NeedConsentException
import com.entropyjournal.domain.model.UserProfile
import com.entropyjournal.domain.usecase.SignInWithGoogleUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.util.Constants
import com.entropyjournal.util.DailyReminderManager
import com.entropyjournal.util.PdfExporter
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
    val groqApiKey: String = "",
    val geminiApiKey: String = "",
    val selectedModel: String = Constants.DEFAULT_GEMINI_MODEL,
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
    private val journalEntryDao: JournalEntryDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private lateinit var reminderManager: DailyReminderManager

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
        reminderManager = DailyReminderManager(context, encryptedPrefs)
        try {
            reminderManager.ensureWeeklyReviewScheduled()
            reminderManager.ensureMonthlyReviewScheduled()
            reminderManager.ensureYearlyReviewScheduled()
        } catch (e: Exception) {
            android.util.Log.e("SettingsVM", "Failed to schedule review alarms: ${e.message}")
        }
        loadSettings()
        encryptedPrefs.registerOnSharedPreferenceChangeListener(prefsListener)
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
                groqApiKey = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: "",
                geminiApiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: "",
                selectedModel =
                    encryptedPrefs.getString(
                        Constants.PREF_GEMINI_MODEL,
                        Constants.DEFAULT_GEMINI_MODEL,
                    ) ?: Constants.DEFAULT_GEMINI_MODEL,
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

    fun updateSelectedModel(modelId: String) {
        encryptedPrefs.edit().putString(Constants.PREF_GEMINI_MODEL, modelId).apply()
        _uiState.value = _uiState.value.copy(selectedModel = modelId)
    }

    fun updateGroqApiKey(key: String) {
        encryptedPrefs.edit().putString(Constants.PREF_GROQ_API_KEY, key).apply()
        _uiState.value = _uiState.value.copy(groqApiKey = key)
    }

    fun updateGeminiApiKey(key: String) {
        encryptedPrefs.edit().putString(Constants.PREF_GEMINI_API_KEY, key).apply()
        _uiState.value = _uiState.value.copy(geminiApiKey = key)
    }

    fun updateTextImprovementDefault(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, enabled).apply()
        _uiState.value = _uiState.value.copy(textImprovementDefault = enabled)
    }

    fun updateMaxRecordingDuration(minutes: Int) {
        encryptedPrefs.edit().putInt(Constants.PREF_MAX_RECORDING_DURATION, minutes).apply()
        _uiState.value = _uiState.value.copy(maxRecordingDuration = minutes)
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

    fun updateReminderEnabled(enabled: Boolean) {
        if (enabled) {
            val hour = _uiState.value.reminderHour
            val minute = _uiState.value.reminderMinute
            reminderManager.scheduleReminder(hour, minute)
        } else {
            reminderManager.cancelReminder()
        }
        _uiState.value = _uiState.value.copy(reminderEnabled = enabled)
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        reminderManager.scheduleReminder(hour, minute)
        _uiState.value = _uiState.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    fun updateWeeklyReviewEnabled(enabled: Boolean) {
        if (enabled) {
            reminderManager.scheduleWeeklyReview()
        } else {
            reminderManager.cancelWeeklyReview()
        }
        _uiState.value = _uiState.value.copy(weeklyReviewEnabled = enabled)
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

    fun signIn(activityContext: android.content.Context) {
        viewModelScope.launch {
            signInUseCase(activityContext)
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(userProfile = profile, syncMessage = null)
                    // Auto-restore backup
                    try {
                        if (syncUseCase.hasBackup()) {
                            syncUseCase.restore()
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
                    } catch (_: Exception) {}
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            syncMessage = "Anmeldung fehlgeschlagen: ${error.message}"
                        )
                }
        }
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
                            exportMessage = "Keine Einträge vorhanden",
                        )
                    delay(3000)
                    _uiState.value = _uiState.value.copy(exportMessage = null)
                    return@launch
                }

                val count =
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            PdfExporter.export(entries, outputStream)
                        }
                    }

                if (count != null) {
                    _uiState.value =
                        _uiState.value.copy(
                            isExporting = false,
                            exportMessage = "$count Einträge exportiert",
                        )
                } else {
                    _uiState.value =
                        _uiState.value.copy(
                            isExporting = false,
                            exportMessage = "Fehler: Datei konnte nicht geöffnet werden",
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
            // Save device-specific settings BEFORE clearing everything
            val groqKey = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: ""
            val geminiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
            val selectedModel =
                encryptedPrefs.getString(
                    Constants.PREF_GEMINI_MODEL,
                    Constants.DEFAULT_GEMINI_MODEL,
                ) ?: Constants.DEFAULT_GEMINI_MODEL
            val isDark = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, true)
            val biometricLock = encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false)

            // Clear ALL prefs, then restore only device-specific ones — single atomic operation
            encryptedPrefs
                .edit()
                .clear()
                .putString(Constants.PREF_GROQ_API_KEY, groqKey)
                .putString(Constants.PREF_GEMINI_API_KEY, geminiKey)
                .putString(Constants.PREF_GEMINI_MODEL, selectedModel)
                .putBoolean(Constants.PREF_DARK_THEME, isDark)
                .putBoolean(Constants.PREF_BIOMETRIC_LOCK, biometricLock)
                .commit() // commit() is synchronous — guarantees write before restart

            reminderManager.cancelReminder()
            reminderManager.cancelWeeklyReview()

            // Delete local databases — data belongs to the account
            context.deleteDatabase("entropy_journal_db")
            context.getDatabasePath("entropy_journal_db-wal")?.delete()
            context.getDatabasePath("entropy_journal_db-shm")?.delete()
            context.deleteDatabase("retrospective_db")
            context.getDatabasePath("retrospective_db-wal")?.delete()
            context.getDatabasePath("retrospective_db-shm")?.delete()
            java.io.File(context.filesDir, ".retro_cleaned_v3").delete()
        } catch (_: Exception) {}

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
