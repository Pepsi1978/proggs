package com.entropyjournal.ui.screens.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.local.dao.EntryFollowUpDao
import com.entropyjournal.data.local.dao.EntryPhotoDao
import com.entropyjournal.data.local.dao.JournalEntryDao
import com.entropyjournal.data.remote.googledrive.NeedConsentException
import com.entropyjournal.domain.model.UserProfile
import com.entropyjournal.domain.usecase.ImproveTextUseCase
import com.entropyjournal.domain.usecase.RecordAudioUseCase
import com.entropyjournal.domain.usecase.SignInWithGoogleUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.domain.usecase.TranscribeAudioUseCase
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
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "",
    val ttsProvider: String = Constants.TTS_PROVIDER_EDGE,
    val edgeTtsVoice: String = Constants.DEFAULT_EDGE_TTS_VOICE,
    val googleTtsApiKey: String = "",
    val googleTtsVoice: String = Constants.DEFAULT_GOOGLE_TTS_VOICE,
    val selectedModel: String = Constants.DEFAULT_GEMINI_MODEL,
    val textImprovementDefault: Boolean = false,
    val maxRecordingDuration: Int = 5,
    val autoUpdateDashboard: Boolean = true,
    val verboseDashboard: Boolean = false,
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
    val dailyPromptEnabled: Boolean = true,
    // Voice-input state for the Custom Prompt dialog (Individuelle Analyse).
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
    private val signInUseCase: SignInWithGoogleUseCase,
    private val syncUseCase: SyncWithDriveUseCase,
    private val encryptedPrefs: SharedPreferences,
    private val journalEntryDao: JournalEntryDao,
    private val entryPhotoDao: EntryPhotoDao,
    private val entryFollowUpDao: EntryFollowUpDao,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val improveTextUseCase: ImproveTextUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private lateinit var reminderManager: DailyReminderManager

    val promptAmplitude: StateFlow<Float> = recordAudioUseCase.amplitude

    private var promptAudioFile: java.io.File? = null
    private var promptRecordingJob: kotlinx.coroutines.Job? = null

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
                        // Reload UI settings so the text field reflects the new value
                        loadSettings()
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

    private fun loadSettings() {
        _uiState.value =
            SettingsUiState(
                userProfile = signInUseCase.getProfile(),
                groqApiKey = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: "",
                geminiApiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: "",
                elevenLabsApiKey = encryptedPrefs.getString(Constants.PREF_ELEVENLABS_API_KEY, "") ?: "",
                elevenLabsVoiceId = encryptedPrefs.getString(Constants.PREF_ELEVENLABS_VOICE_ID, "") ?: "",
                ttsProvider = encryptedPrefs.getString(Constants.PREF_TTS_PROVIDER, Constants.TTS_PROVIDER_EDGE) ?: Constants.TTS_PROVIDER_EDGE,
                edgeTtsVoice = encryptedPrefs.getString(Constants.PREF_EDGE_TTS_VOICE, Constants.DEFAULT_EDGE_TTS_VOICE) ?: Constants.DEFAULT_EDGE_TTS_VOICE,
                googleTtsApiKey = encryptedPrefs.getString(Constants.PREF_GOOGLE_TTS_API_KEY, "") ?: "",
                googleTtsVoice = encryptedPrefs.getString(Constants.PREF_GOOGLE_TTS_VOICE, Constants.DEFAULT_GOOGLE_TTS_VOICE) ?: Constants.DEFAULT_GOOGLE_TTS_VOICE,
                selectedModel = run {
                    val stored = encryptedPrefs.getString(
                        Constants.PREF_GEMINI_MODEL,
                        Constants.DEFAULT_GEMINI_MODEL,
                    )
                    val valid = Constants.resolveValidModel(stored)
                    // Falls das gespeicherte Modell nicht mehr in der Liste ist,
                    // sofort migrieren — sonst schicken die UseCases weiter den alten Namen.
                    if (stored != valid) {
                        encryptedPrefs.edit().putString(Constants.PREF_GEMINI_MODEL, valid).apply()
                    }
                    valid
                },
                textImprovementDefault =
                    encryptedPrefs.getBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, false),
                maxRecordingDuration =
                    encryptedPrefs.getInt(Constants.PREF_MAX_RECORDING_DURATION, 5),
                autoUpdateDashboard =
                    encryptedPrefs.getBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, true),
                verboseDashboard =
                    encryptedPrefs.getBoolean(Constants.PREF_VERBOSE_DASHBOARD, false),
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
                dailyPromptEnabled =
                    encryptedPrefs.getBoolean(Constants.PREF_DAILY_PROMPT_ENABLED, true),
            )
    }

    fun updateDailyPromptEnabled(enabled: Boolean) {
        encryptedPrefs.edit().putBoolean(Constants.PREF_DAILY_PROMPT_ENABLED, enabled).apply()
        _uiState.value = _uiState.value.copy(dailyPromptEnabled = enabled)
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

    fun updateElevenLabsApiKey(key: String) {
        encryptedPrefs.edit().putString(Constants.PREF_ELEVENLABS_API_KEY, key).apply()
        _uiState.value = _uiState.value.copy(elevenLabsApiKey = key)
    }

    fun updateElevenLabsVoiceId(voiceId: String) {
        encryptedPrefs.edit().putString(Constants.PREF_ELEVENLABS_VOICE_ID, voiceId).apply()
        _uiState.value = _uiState.value.copy(elevenLabsVoiceId = voiceId)
    }

    fun updateTtsProvider(provider: String) {
        encryptedPrefs.edit().putString(Constants.PREF_TTS_PROVIDER, provider).apply()
        _uiState.value = _uiState.value.copy(ttsProvider = provider)
    }

    fun updateEdgeTtsVoice(voice: String) {
        encryptedPrefs.edit().putString(Constants.PREF_EDGE_TTS_VOICE, voice).apply()
        _uiState.value = _uiState.value.copy(edgeTtsVoice = voice)
    }

    fun updateGoogleTtsApiKey(key: String) {
        encryptedPrefs.edit().putString(Constants.PREF_GOOGLE_TTS_API_KEY, key).apply()
        _uiState.value = _uiState.value.copy(googleTtsApiKey = key)
    }

    fun updateGoogleTtsVoice(voice: String) {
        encryptedPrefs.edit().putString(Constants.PREF_GOOGLE_TTS_VOICE, voice).apply()
        _uiState.value = _uiState.value.copy(googleTtsVoice = voice)
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

    fun updateVerboseDashboard(enabled: Boolean) {
        encryptedPrefs
            .edit()
            .putBoolean(Constants.PREF_VERBOSE_DASHBOARD, enabled)
            .putLong(Constants.PREF_VERBOSE_DASHBOARD_CHANGED_AT, System.currentTimeMillis())
            .apply()
        _uiState.value = _uiState.value.copy(verboseDashboard = enabled)
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
                    // Auto-enable TTS after login (last selected voice + favorites stay intact)
                    encryptedPrefs.edit().putBoolean(Constants.PREF_TTS_ENABLED, true).apply()
                    // Auto-restore backup
                    try {
                        if (syncUseCase.hasBackup()) {
                            // Block retrospective generation until restore+download is complete
                            encryptedPrefs
                                .edit()
                                .putBoolean(Constants.PREF_RESTORE_PENDING, true)
                                .apply()
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

    fun exportToPdf(context: android.content.Context, uri: Uri, includePhotos: Boolean = false) {
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
                        val photosPerEntry = if (includePhotos) {
                            entries.associate { entry ->
                                entry.id to entryPhotoDao.getPhotoOnlyForEntryOnce(entry.id)
                            }
                        } else {
                            emptyMap()
                        }
                        val followUpsPerEntry =
                            entries.associate { entry ->
                                entry.id to entryFollowUpDao.getForEntryOnce(entry.id)
                            }

                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            PdfExporter.export(
                                entries = entries,
                                outputStream = outputStream,
                                photosPerEntry = photosPerEntry,
                                followUpsPerEntry = followUpsPerEntry,
                            )
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
            val elevenLabsKey = encryptedPrefs.getString(Constants.PREF_ELEVENLABS_API_KEY, "") ?: ""
            val elevenLabsVoice = encryptedPrefs.getString(Constants.PREF_ELEVENLABS_VOICE_ID, "") ?: ""
            val ttsProvider = encryptedPrefs.getString(Constants.PREF_TTS_PROVIDER, Constants.TTS_PROVIDER_EDGE) ?: Constants.TTS_PROVIDER_EDGE
            val edgeTtsVoice = encryptedPrefs.getString(Constants.PREF_EDGE_TTS_VOICE, Constants.DEFAULT_EDGE_TTS_VOICE) ?: Constants.DEFAULT_EDGE_TTS_VOICE
            val googleTtsKey = encryptedPrefs.getString(Constants.PREF_GOOGLE_TTS_API_KEY, "") ?: ""
            val googleTtsVoice = encryptedPrefs.getString(Constants.PREF_GOOGLE_TTS_VOICE, Constants.DEFAULT_GOOGLE_TTS_VOICE) ?: Constants.DEFAULT_GOOGLE_TTS_VOICE
            val selectedModel =
                encryptedPrefs.getString(
                    Constants.PREF_GEMINI_MODEL,
                    Constants.DEFAULT_GEMINI_MODEL,
                ) ?: Constants.DEFAULT_GEMINI_MODEL
            val isDark = encryptedPrefs.getBoolean(Constants.PREF_DARK_THEME, true)
            val appTheme = encryptedPrefs.getString(Constants.PREF_APP_THEME, null)
            val headingFont = encryptedPrefs.getString(Constants.PREF_HEADING_FONT, null)
            val bodyFont = encryptedPrefs.getString(Constants.PREF_BODY_FONT, null)
            val headingFontScale = encryptedPrefs.getFloat(Constants.PREF_HEADING_FONT_SCALE, 1f)
            val bodyFontScale = encryptedPrefs.getFloat(Constants.PREF_BODY_FONT_SCALE, 1f)
            val biometricLock = encryptedPrefs.getBoolean(Constants.PREF_BIOMETRIC_LOCK, false)
            val ttsFavorites = encryptedPrefs.getString(Constants.PREF_TTS_FAVORITES, "") ?: ""

            // Clear ALL prefs, then restore only device-specific ones — single atomic operation
            encryptedPrefs
                .edit()
                .clear()
                .putString(Constants.PREF_GROQ_API_KEY, groqKey)
                .putString(Constants.PREF_GEMINI_API_KEY, geminiKey)
                .putString(Constants.PREF_ELEVENLABS_API_KEY, elevenLabsKey)
                .putString(Constants.PREF_ELEVENLABS_VOICE_ID, elevenLabsVoice)
                .putString(Constants.PREF_TTS_PROVIDER, ttsProvider)
                .putString(Constants.PREF_EDGE_TTS_VOICE, edgeTtsVoice)
                .putString(Constants.PREF_GOOGLE_TTS_API_KEY, googleTtsKey)
                .putString(Constants.PREF_GOOGLE_TTS_VOICE, googleTtsVoice)
                .putString(Constants.PREF_GEMINI_MODEL, selectedModel)
                .putBoolean(Constants.PREF_DARK_THEME, isDark)
                .putString(Constants.PREF_APP_THEME, appTheme)
                .putString(Constants.PREF_HEADING_FONT, headingFont)
                .putString(Constants.PREF_BODY_FONT, bodyFont)
                .putFloat(Constants.PREF_HEADING_FONT_SCALE, headingFontScale)
                .putFloat(Constants.PREF_BODY_FONT_SCALE, bodyFontScale)
                .putBoolean(Constants.PREF_BIOMETRIC_LOCK, biometricLock)
                .putString(Constants.PREF_TTS_FAVORITES, ttsFavorites)
                .commit() // commit() is synchronous — guarantees write before restart

            reminderManager.cancelReminder()
            reminderManager.cancelWeeklyReview()
            // Favorites survive logout — backed up to Drive and restored to this device's prefs

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

    fun backupFavoritesToDrive(favorites: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            syncUseCase.backupFavorites(favorites.joinToString(","))
        }
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
                            promptError = "Aufnahme fehlgeschlagen: ${e.message}",
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
                .onSuccess { outcome ->
                    val transcribed = outcome.text.trim()
                    val modelLabel =
                        when (outcome.engine) {
                            com.entropyjournal.data.repository.TranscriptionEngine.GROQ ->
                                "Groq Whisper Large V3 Turbo"
                            com.entropyjournal.data.repository.TranscriptionEngine.LOCAL ->
                                "Lokales Whisper-Modell"
                        }
                    val fallbackNotice = outcome.groqError?.let {
                        "Groq fehlgeschlagen ($it) — lokales Whisper verwendet."
                    }
                    _uiState.value =
                        _uiState.value.copy(
                            promptRecState = PromptRecState.IDLE,
                            promptPendingTranscription =
                                if (transcribed.isNotBlank())
                                    PromptTranscription(transcribed, modelLabel)
                                else null,
                            promptTranscriptionModel = modelLabel,
                            promptError = fallbackNotice,
                        )
                    audioFile.delete()
                    // Auto-improve is triggered by the dialog after appending the text,
                    // so the ENTIRE field gets improved — not just the new chunk.
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            promptRecState = PromptRecState.IDLE,
                            promptError = "Transkription fehlgeschlagen: ${error.message}",
                        )
                    audioFile.delete()
                }
        }
    }

    /**
     * Improves the custom analysis focus text using the dedicated prompt optimizer.
     * Rewrites the user's rough wording into a precise, mode-aware task description
     * that the downstream analysis system can act on. Used by the "Text verbessern"
     * button in the Individuelle Analyse settings field — NOT for journal entries.
     */
    fun improvePromptText(fullText: String) {
        if (fullText.isBlank()) return
        _uiState.value = _uiState.value.copy(promptRecState = PromptRecState.IMPROVING)
        viewModelScope.launch {
            improveTextUseCase.optimizeCustomAnalysisPrompt(fullText)
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
                            promptError = "Textverbesserung fehlgeschlagen: ${error.message}",
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
     * Uploads the current custom-analysis prompt to Google Drive immediately.
     * Called right after the user saves the Individuelle-Analyse focus text
     * so it syncs to other devices and survives a fresh sign-in.
     *
     * Legacy entry point — prefer [backupCustomAnalysesToDrive] which uploads
     * the full list. Kept so any remaining callers continue to work.
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

    /**
     * Uploads the full list of custom analyses (names + prompts) to Drive.
     * Called after any add/remove/rename/prompt-save in the Individuelle-Analyse
     * settings so all devices see the same list.
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
