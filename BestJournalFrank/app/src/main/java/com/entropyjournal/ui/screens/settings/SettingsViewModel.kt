package com.entropyjournal.ui.screens.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.local.dao.EntryFollowUpDao
import com.entropyjournal.data.local.dao.EntryPhotoDao
import com.entropyjournal.data.local.dao.JournalEntryDao
import com.entropyjournal.data.remote.codex.CodexAuthManager
import com.entropyjournal.data.remote.codex.CodexModel
import com.entropyjournal.data.remote.codex.ReasoningEffort
import com.entropyjournal.data.remote.googledrive.NeedConsentException
import com.entropyjournal.data.remote.qwen.ClonedVoice
import com.entropyjournal.data.remote.qwen.QwenVoiceDirectory
import com.entropyjournal.data.remote.qwen.QwenVoiceEnrollment
import com.entropyjournal.domain.model.UserProfile
import com.entropyjournal.domain.usecase.ImproveTextUseCase
import com.entropyjournal.domain.usecase.RecordAudioUseCase
import com.entropyjournal.domain.usecase.SignInWithGoogleUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.domain.usecase.TranscribeAudioUseCase
import com.entropyjournal.util.Constants
import com.entropyjournal.util.DailyReminderManager
import com.entropyjournal.util.PdfExporter
import com.entropyjournal.util.VoiceCloneRecorder
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
    // Eigene Stimme (Alibaba Voice Cloning)
    val qwenApiKey: String = "",
    val qwenVoiceId: String = "",
    val qwenVoices: List<ClonedVoice> = emptyList(),
    val qwenVoicesLoading: Boolean = false,
    val qwenVoiceNames: Map<String, String> = emptyMap(),
    val voiceBusy: Boolean = false,
    val voiceMessage: String? = null,
    val voiceRecorderState: VoiceRecorderState = VoiceRecorderState.IDLE,
    val voiceRecorderName: String = "",
    val voiceRecorderSeconds: Int = 0,
    val voiceRecorderMessage: String? = null,
    /** true, sobald eine Aufnahme erfolgreich zu einer Stimme geworden ist. */
    val voiceRecorderDone: Boolean = false,
    // KI-Anbieter (Gemini oder ChatGPT/Codex)
    val aiProvider: String = Constants.AI_PROVIDER_GEMINI,
    val codexModel: String = CodexModel.DEFAULT.apiId,
    val codexEffort: String = ReasoningEffort.DEFAULT.apiValue,
    val codexConnected: Boolean = false,
    val codexEmail: String? = null,
    val codexLoggingIn: Boolean = false,
    val codexUserCode: String? = null,
    val codexMessage: String? = null,
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

/** Zustand der Aufnahme, aus der eine neue eigene Stimme entsteht. */
enum class VoiceRecorderState {
    IDLE,
    RECORDING,
    SAVING,
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
    private val codexAuthManager: CodexAuthManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    private lateinit var reminderManager: DailyReminderManager

    private val qwenVoiceDirectory = QwenVoiceDirectory()
    private val qwenVoiceEnrollment = QwenVoiceEnrollment()
    private val voiceCloneRecorder = VoiceCloneRecorder(context)
    private var voiceTimerJob: kotlinx.coroutines.Job? = null
    private var codexLoginJob: kotlinx.coroutines.Job? = null

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
        voiceCloneRecorder.release()
        qwenVoiceDirectory.shutdown()
        qwenVoiceEnrollment.shutdown()
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
                qwenApiKey = encryptedPrefs.getString(Constants.PREF_QWEN_API_KEY, "") ?: "",
                qwenVoiceId = encryptedPrefs.getString(Constants.PREF_QWEN_VOICE_ID, "") ?: "",
                qwenVoiceNames = readVoiceNames(),
                aiProvider =
                    encryptedPrefs.getString(
                        Constants.PREF_AI_PROVIDER,
                        Constants.AI_PROVIDER_GEMINI,
                    ) ?: Constants.AI_PROVIDER_GEMINI,
                codexModel =
                    CodexModel.fromId(encryptedPrefs.getString(Constants.PREF_CODEX_MODEL, null))
                        .apiId,
                codexEffort =
                    ReasoningEffort.fromValue(
                            encryptedPrefs.getString(Constants.PREF_CODEX_EFFORT, null)
                        )
                        .apiValue,
                codexConnected = codexAuthManager.isConnected,
                codexEmail = codexAuthManager.email,
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
            val qwenKey = encryptedPrefs.getString(Constants.PREF_QWEN_API_KEY, "") ?: ""
            val qwenVoiceId = encryptedPrefs.getString(Constants.PREF_QWEN_VOICE_ID, "") ?: ""
            val qwenVoiceNames = encryptedPrefs.getString(Constants.PREF_QWEN_VOICE_NAMES, "") ?: ""
            val aiProvider =
                encryptedPrefs.getString(Constants.PREF_AI_PROVIDER, Constants.AI_PROVIDER_GEMINI)
                    ?: Constants.AI_PROVIDER_GEMINI
            val codexModel = encryptedPrefs.getString(Constants.PREF_CODEX_MODEL, null)
            val codexEffort = encryptedPrefs.getString(Constants.PREF_CODEX_EFFORT, null)

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
                .putString(Constants.PREF_QWEN_API_KEY, qwenKey)
                .putString(Constants.PREF_QWEN_VOICE_ID, qwenVoiceId)
                .putString(Constants.PREF_QWEN_VOICE_NAMES, qwenVoiceNames)
                .putString(Constants.PREF_AI_PROVIDER, aiProvider)
                .putString(Constants.PREF_CODEX_MODEL, codexModel)
                .putString(Constants.PREF_CODEX_EFFORT, codexEffort)
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

    // ─── Eigene Stimme (Alibaba Model Studio, Qwen3-TTS) ──────────────────────────

    fun updateQwenApiKey(key: String) {
        // Weder Schluessel noch Kennung enthalten je Leerzeichen, aber Tastaturen setzen nach
        // jedem Punkt eines und eingefuegte Werte tragen Zeilenumbrueche — beides endet in 401.
        val clean = key.filterNot(Char::isWhitespace)
        encryptedPrefs.edit().putString(Constants.PREF_QWEN_API_KEY, clean).apply()
        _uiState.value = _uiState.value.copy(qwenApiKey = clean)
        disableOwnVoiceIfIncomplete()
        // Nicht bei jedem Zeichen nachladen — die Liste holt sich der Stimmen-Dialog beim Oeffnen.
        if (clean.isBlank()) _uiState.value = _uiState.value.copy(qwenVoices = emptyList())
    }

    fun refreshQwenVoices() {
        val key = _uiState.value.qwenApiKey
        if (key.isBlank()) {
            _uiState.value = _uiState.value.copy(qwenVoices = emptyList())
            return
        }
        _uiState.value = _uiState.value.copy(qwenVoicesLoading = true)
        viewModelScope.launch {
            val voices = qwenVoiceDirectory.list(key)
            _uiState.value = _uiState.value.copy(qwenVoices = voices, qwenVoicesLoading = false)
        }
    }

    /** Waehlt die Stimme aus und schaltet das Vorlesen zugleich auf die eigene Stimme um. */
    fun selectQwenVoice(voice: ClonedVoice) {
        encryptedPrefs
            .edit()
            .putString(Constants.PREF_QWEN_VOICE_ID, voice.id)
            .putString(Constants.PREF_TTS_PROVIDER, Constants.TTS_PROVIDER_QWEN)
            .apply()
        _uiState.value =
            _uiState.value.copy(
                qwenVoiceId = voice.id,
                ttsProvider = Constants.TTS_PROVIDER_QWEN,
            )
    }

    /** Der Titel, den eine Stimme in der App traegt: der umbenannte, sonst der von Alibaba. */
    fun voiceTitle(voice: ClonedVoice): String =
        _uiState.value.qwenVoiceNames[voice.id] ?: voice.name

    fun renameQwenVoice(voiceId: String, title: String) {
        val trimmed = title.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(voiceMessage = "Der Name darf nicht leer sein.")
            return
        }
        val names = _uiState.value.qwenVoiceNames + (voiceId to trimmed)
        writeVoiceNames(names)
        _uiState.value = _uiState.value.copy(qwenVoiceNames = names)
    }

    /** Entfernt die Stimme bei Alibaba — es gibt kein Zurueck, deshalb fragt die UI vorher nach. */
    fun deleteQwenVoice(voice: ClonedVoice) {
        _uiState.value = _uiState.value.copy(voiceBusy = true)
        viewModelScope.launch {
            try {
                qwenVoiceEnrollment.delete(_uiState.value.qwenApiKey, voice.id)
                val title = voiceTitle(voice)
                val names = _uiState.value.qwenVoiceNames - voice.id
                writeVoiceNames(names)
                val wasSelected = _uiState.value.qwenVoiceId == voice.id
                if (wasSelected) {
                    encryptedPrefs.edit().putString(Constants.PREF_QWEN_VOICE_ID, "").apply()
                }
                _uiState.value =
                    _uiState.value.copy(
                        qwenVoiceNames = names,
                        qwenVoiceId = if (wasSelected) "" else _uiState.value.qwenVoiceId,
                        voiceMessage = "Die Stimme „$title“ wurde gelöscht.",
                    )
                disableOwnVoiceIfIncomplete()
                refreshQwenVoices()
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        voiceMessage = e.message ?: "Die Stimme konnte nicht gelöscht werden."
                    )
            } finally {
                _uiState.value = _uiState.value.copy(voiceBusy = false)
            }
        }
    }

    fun openVoiceRecorder() {
        _uiState.value =
            _uiState.value.copy(
                voiceRecorderName = "",
                voiceRecorderSeconds = 0,
                voiceRecorderMessage = null,
                voiceRecorderState = VoiceRecorderState.IDLE,
                voiceRecorderDone = false,
            )
    }

    fun updateVoiceRecorderName(value: String) {
        _uiState.value = _uiState.value.copy(voiceRecorderName = value)
    }

    fun onVoiceRecorderTapped(permissionGranted: Boolean, requestPermission: () -> Unit) {
        when (_uiState.value.voiceRecorderState) {
            VoiceRecorderState.IDLE -> {
                if (_uiState.value.voiceRecorderName.isBlank()) {
                    _uiState.value =
                        _uiState.value.copy(
                            voiceRecorderMessage = "Bitte zuerst einen Namen für die Stimme eingeben."
                        )
                    return
                }
                if (permissionGranted) startVoiceSampleRecording() else requestPermission()
            }
            VoiceRecorderState.RECORDING -> finishVoiceSampleRecording()
            VoiceRecorderState.SAVING -> Unit
        }
    }

    private fun startVoiceSampleRecording() {
        if (!voiceCloneRecorder.start(viewModelScope)) {
            _uiState.value =
                _uiState.value.copy(
                    voiceRecorderMessage = "Die Aufnahme konnte nicht gestartet werden."
                )
            return
        }
        _uiState.value =
            _uiState.value.copy(
                voiceRecorderMessage = null,
                voiceRecorderSeconds = 0,
                voiceRecorderState = VoiceRecorderState.RECORDING,
            )
        voiceTimerJob =
            viewModelScope.launch {
                while (_uiState.value.voiceRecorderState == VoiceRecorderState.RECORDING) {
                    delay(1_000)
                    if (_uiState.value.voiceRecorderState != VoiceRecorderState.RECORDING) return@launch
                    val seconds = _uiState.value.voiceRecorderSeconds + 1
                    _uiState.value = _uiState.value.copy(voiceRecorderSeconds = seconds)
                    // Alibaba lehnt alles jenseits einer Minute ab, also stoppt die Aufnahme selbst.
                    if (seconds >= MAX_SAMPLE_SECONDS) {
                        // Zuerst geleert: diese Coroutine endet hier, und der Stopp darf sie nicht
                        // abbrechen, waehrend sie die Registrierung startet.
                        voiceTimerJob = null
                        finishVoiceSampleRecording()
                        return@launch
                    }
                }
            }
    }

    private fun finishVoiceSampleRecording() {
        voiceTimerJob?.cancel()
        voiceTimerJob = null
        val seconds = _uiState.value.voiceRecorderSeconds
        val name = _uiState.value.voiceRecorderName.trim()
        _uiState.value = _uiState.value.copy(voiceRecorderState = VoiceRecorderState.SAVING)
        viewModelScope.launch {
            try {
                val wav = voiceCloneRecorder.stop()
                if (wav == null) {
                    _uiState.value =
                        _uiState.value.copy(voiceRecorderMessage = "Die Aufnahme ist leer geblieben.")
                    return@launch
                }
                if (seconds < MIN_SAMPLE_SECONDS) {
                    _uiState.value =
                        _uiState.value.copy(
                            voiceRecorderMessage =
                                "Die Aufnahme ist zu kurz. Lies bitte mindestens $MIN_SAMPLE_SECONDS Sekunden vor."
                        )
                    return@launch
                }
                val voiceId = qwenVoiceEnrollment.create(_uiState.value.qwenApiKey, name, wav)
                // Alibaba behaelt nur Buchstaben und Ziffern des Namens, deshalb merkt sich die
                // App den eingegebenen Namen selbst.
                val names = _uiState.value.qwenVoiceNames + (voiceId to name)
                writeVoiceNames(names)
                encryptedPrefs
                    .edit()
                    .putString(Constants.PREF_QWEN_VOICE_ID, voiceId)
                    .putString(Constants.PREF_TTS_PROVIDER, Constants.TTS_PROVIDER_QWEN)
                    .apply()
                _uiState.value =
                    _uiState.value.copy(
                        qwenVoiceId = voiceId,
                        qwenVoiceNames = names,
                        ttsProvider = Constants.TTS_PROVIDER_QWEN,
                        voiceMessage = "Die Stimme „$name“ ist fertig und ausgewählt.",
                        voiceRecorderMessage = null,
                        voiceRecorderDone = true,
                    )
                refreshQwenVoices()
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        voiceRecorderMessage = e.message ?: "Die Stimme konnte nicht erstellt werden."
                    )
            } finally {
                _uiState.value = _uiState.value.copy(voiceRecorderState = VoiceRecorderState.IDLE)
            }
        }
    }

    fun cancelVoiceRecording() {
        voiceTimerJob?.cancel()
        voiceTimerJob = null
        voiceCloneRecorder.release()
        _uiState.value =
            _uiState.value.copy(
                voiceRecorderState = VoiceRecorderState.IDLE,
                voiceRecorderSeconds = 0,
            )
    }

    fun clearVoiceMessage() {
        _uiState.value = _uiState.value.copy(voiceMessage = null)
    }

    /** Faellt auf Edge zurueck, wenn Schluessel oder Stimme fuer die eigene Stimme fehlen. */
    private fun disableOwnVoiceIfIncomplete() {
        if (_uiState.value.ttsProvider != Constants.TTS_PROVIDER_QWEN) return
        if (_uiState.value.qwenApiKey.isNotBlank() && _uiState.value.qwenVoiceId.isNotBlank()) return
        encryptedPrefs
            .edit()
            .putString(Constants.PREF_TTS_PROVIDER, Constants.TTS_PROVIDER_EDGE)
            .apply()
        _uiState.value =
            _uiState.value.copy(
                ttsProvider = Constants.TTS_PROVIDER_EDGE,
                voiceMessage = "Eigene Stimme wurde abgeschaltet, weil Schlüssel oder Stimme fehlen.",
            )
    }

    private fun readVoiceNames(): Map<String, String> {
        val stored = encryptedPrefs.getString(Constants.PREF_QWEN_VOICE_NAMES, "") ?: ""
        if (stored.isBlank()) return emptyMap()
        return try {
            val json = org.json.JSONObject(stored)
            json.keys()
                .asSequence()
                .associateWith { json.optString(it) }
                .filterValues(String::isNotBlank)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun writeVoiceNames(names: Map<String, String>) {
        val json = org.json.JSONObject()
        names.forEach { (id, title) -> json.put(id, title) }
        encryptedPrefs
            .edit()
            .putString(Constants.PREF_QWEN_VOICE_NAMES, json.toString())
            .apply()
    }

    // ─── KI-Anbieter: Gemini oder ChatGPT/Codex ───────────────────────────────────

    fun updateAiProvider(provider: String) {
        encryptedPrefs.edit().putString(Constants.PREF_AI_PROVIDER, provider).apply()
        _uiState.value = _uiState.value.copy(aiProvider = provider)
    }

    fun updateCodexModel(apiId: String) {
        encryptedPrefs.edit().putString(Constants.PREF_CODEX_MODEL, apiId).apply()
        _uiState.value = _uiState.value.copy(codexModel = apiId)
    }

    fun updateCodexEffort(apiValue: String) {
        encryptedPrefs.edit().putString(Constants.PREF_CODEX_EFFORT, apiValue).apply()
        _uiState.value = _uiState.value.copy(codexEffort = apiValue)
    }

    /**
     * Startet die Geraete-Anmeldung: die App holt einen achtstelligen Code, oeffnet die
     * Bestaetigungsseite im Browser und wartet, bis der Code dort eingegeben wurde.
     */
    fun loginToCodex(activity: androidx.activity.ComponentActivity) {
        if (_uiState.value.codexLoggingIn) return
        _uiState.value =
            _uiState.value.copy(codexLoggingIn = true, codexMessage = null, codexUserCode = null)
        codexLoginJob =
            viewModelScope.launch {
                try {
                    val result =
                        codexAuthManager.login(activity) { info ->
                            _uiState.value = _uiState.value.copy(codexUserCode = info.userCode)
                        }
                    _uiState.value =
                        _uiState.value.copy(
                            codexConnected = true,
                            codexEmail = result.email ?: codexAuthManager.email,
                            codexMessage = "Mit ChatGPT verbunden.",
                            codexUserCode = null,
                        )
                    updateAiProvider(Constants.AI_PROVIDER_CODEX)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    _uiState.value = _uiState.value.copy(codexUserCode = null)
                    throw e
                } catch (e: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            codexMessage = e.message ?: "Die Anmeldung ist fehlgeschlagen.",
                            codexUserCode = null,
                        )
                } finally {
                    _uiState.value = _uiState.value.copy(codexLoggingIn = false)
                }
            }
    }

    fun cancelCodexLogin() {
        codexAuthManager.cancelLogin()
        codexLoginJob?.cancel()
        codexLoginJob = null
        _uiState.value =
            _uiState.value.copy(codexLoggingIn = false, codexUserCode = null, codexMessage = null)
    }

    fun logoutFromCodex() {
        codexAuthManager.logout()
        encryptedPrefs
            .edit()
            .putString(Constants.PREF_AI_PROVIDER, Constants.AI_PROVIDER_GEMINI)
            .apply()
        _uiState.value =
            _uiState.value.copy(
                codexConnected = false,
                codexEmail = null,
                aiProvider = Constants.AI_PROVIDER_GEMINI,
                codexMessage = "Von ChatGPT abgemeldet.",
            )
    }

    fun clearCodexMessage() {
        _uiState.value = _uiState.value.copy(codexMessage = null)
    }

    private companion object {
        /** Alibaba nimmt hoechstens 60 Sekunden Referenzton an. */
        const val MAX_SAMPLE_SECONDS = 58
        const val MIN_SAMPLE_SECONDS = 10
    }
}
