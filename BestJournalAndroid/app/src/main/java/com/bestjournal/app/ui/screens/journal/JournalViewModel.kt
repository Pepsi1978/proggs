package com.bestjournal.app.ui.screens.journal

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.R
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.billing.SubscriptionState
import com.bestjournal.app.data.remote.ai.TieredAccessResult
import com.bestjournal.app.data.repository.JournalRepository
import com.bestjournal.app.data.seed.TestDataSeeder
import com.bestjournal.app.domain.model.JournalEntry
import com.bestjournal.app.domain.usecase.AnalyzeEntropyUseCase
import com.bestjournal.app.domain.usecase.ImproveTextUseCase
import com.bestjournal.app.domain.usecase.RecordAudioUseCase
import com.bestjournal.app.domain.usecase.SaveJournalEntryUseCase
import com.bestjournal.app.domain.usecase.SummarizeEntryUseCase
import com.bestjournal.app.domain.usecase.SyncWithDriveUseCase
import com.bestjournal.app.domain.usecase.TranscribeAudioUseCase
import com.bestjournal.app.util.AchievementStats
import com.bestjournal.app.util.AchievementTracker
import com.bestjournal.app.util.AnalyticsTracker
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.DailyPromptProvider
import com.bestjournal.app.util.InAppReviewHelper
import com.bestjournal.app.util.StreakTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RecordingState {
    IDLE,
    RECORDING,
    TRANSCRIBING,
    IMPROVING,
    PREVIEW,
    SAVING,
}

data class JournalUiState(
    val recordingState: RecordingState = RecordingState.IDLE,
    val rawText: String = "",
    val improvedText: String? = null,
    val isImproveEnabled: Boolean = false,
    val showPreviewDialog: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val errorMessage: String? = null,
    val syncStatus: SyncStatus = SyncStatus.NOT_SIGNED_IN,
    val downloadCurrent: Int = 0,
    val downloadTotal: Int = 0,
    val showAiLimitReached: Boolean = false,
    val transcriptionModel: String = "",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val showTextUpsellBanner: Boolean = false,
    val dailyPromptText: String = "",
    val dailyPromptCategory: String = "",
    val dailyPromptId: String = "",
    val isPremiumUser: Boolean = false,
    val showPromptBanner: Boolean = true,
    val activePrompt: String = "",
    val lastSyncTimestamp: Long = 0L,
    val seedRunning: Boolean = false,
    val seedSeededCount: Int = 0,
    val seedToastMessage: String? = null,
    val showSeedDialog: Boolean = false,
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    UPLOADING,
    DOWNLOADING,
    SYNCED,
    ERROR,
    NOT_SIGNED_IN,
}

@HiltViewModel
class JournalViewModel
@Inject
constructor(
    private val journalRepository: JournalRepository,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val improveTextUseCase: ImproveTextUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val summarizeEntryUseCase: SummarizeEntryUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val syncWithDriveUseCase: SyncWithDriveUseCase,
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences,
    private val billingManager: BillingManager,
    private val streakTracker: StreakTracker,
    private val inAppReviewHelper: InAppReviewHelper,
    private val analyticsTracker: AnalyticsTracker,
    private val achievementTracker: AchievementTracker,
    private val aiRateLimiter: com.bestjournal.app.data.remote.ai.AiRateLimiter,
    private val testDataSeeder: TestDataSeeder,
) : ViewModel() {

    // AudioFocus: request exclusive focus during recording to prevent call contamination
    private var audioFocusRequest: android.media.AudioFocusRequest? = null

    private fun requestAudioFocus(): Boolean {
        val audioManager =
            context.getSystemService(android.content.Context.AUDIO_SERVICE)
                as android.media.AudioManager
        val attrs =
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        val req =
            android.media.AudioFocusRequest.Builder(
                    android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { change ->
                    if (
                        change == android.media.AudioManager.AUDIOFOCUS_LOSS ||
                            change == android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    ) {
                        stopRecording()
                    }
                }
                .build()
        audioFocusRequest = req
        return audioManager.requestAudioFocus(req) ==
            android.media.AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun releaseAudioFocus() {
        val audioManager =
            context.getSystemService(android.content.Context.AUDIO_SERVICE)
                as android.media.AudioManager
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    // Emits achievement title when a new one is unlocked (UI shows gold Snackbar)
    private val _achievementUnlocked = MutableStateFlow<String?>(null)
    val achievementUnlocked: StateFlow<String?> = _achievementUnlocked

    fun clearAchievementEvent() {
        _achievementUnlocked.value = null
    }

    fun launchSubscription(activity: android.app.Activity, isYearly: Boolean) {
        billingManager.launchPurchaseFlow(activity, isYearly)
    }

    val entries =
        journalRepository
            .getAllEntries()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState

    val amplitude: StateFlow<Float> = recordAudioUseCase.amplitude
    val durationSeconds: StateFlow<Int> = recordAudioUseCase.durationSeconds

    // Emits total entry count when a review should be attempted (UI observes and provides Activity)
    private val _reviewEvent = MutableStateFlow<Int?>(null)
    val reviewEvent: StateFlow<Int?> = _reviewEvent

    private var currentAudioFile: File? = null
    private var recordingJob: Job? = null
    private var syncDebounceJob: Job? = null
    private var analysisDebounceJob: Job? = null

    // Stored listener reference so it can be unregistered in onCleared() (prevents leak).
    private val syncPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Constants.PREF_SYNC_IN_PROGRESS -> {
                val inProgress = encryptedPrefs.getBoolean(Constants.PREF_SYNC_IN_PROGRESS, false)
                if (inProgress) {
                    _uiState.update { it.copy(syncStatus = SyncStatus.SYNCING) }
                } else {
                    _uiState.update { it.copy(syncStatus = SyncStatus.SYNCED) }
                }
            }
            Constants.PREF_LAST_SYNC_TIMESTAMP -> {
                val ts = encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L)
                _uiState.update { it.copy(lastSyncTimestamp = ts) }
            }
        }
    }

    /**
     * Verify sign-in state: email pref AND Google account still exists on device. External account
     * removal (system settings) is detected here and triggers local sign-out.
     */
    fun refreshSignInStatus() {
        val signedInEmail = encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, "")
        val hasEmailPref = !signedInEmail.isNullOrBlank()

        // External-removal detection via AccountManager.getAccountsByType("com.google").
        // On Android 8+ this returns an EMPTY LIST (not a SecurityException) when the
        // app is not Google-signed and lacks GET_ACCOUNTS permission. BestJournal has
        // neither — so an empty list means "cannot verify", NOT "account removed".
        //
        // Only treat it as an external removal when the list is non-empty AND our
        // email is not in it: that's the single case where we can be CERTAIN the
        // user removed this specific account. Otherwise trust the local pref;
        // a genuinely revoked account will surface as UserRecoverableAuthException
        // on the next Drive call, where it's handled separately.
        val accountExternallyRemoved =
            hasEmailPref &&
                try {
                    val am = android.accounts.AccountManager.get(context)
                    val accounts = am.getAccountsByType("com.google")
                    accounts.isNotEmpty() && accounts.none { it.name == signedInEmail }
                } catch (e: SecurityException) {
                    android.util.Log.w("JournalVM", "AccountManager check skipped: ${e.message}")
                    false
                }

        if (accountExternallyRemoved) {
            android.util.Log.w(
                "JournalVM",
                "Google account removed externally — clearing local sign-in",
            )
            encryptedPrefs
                .edit()
                .remove(Constants.PREF_GOOGLE_ACCOUNT_EMAIL)
                .remove(Constants.PREF_GOOGLE_ACCOUNT_NAME)
                .remove(Constants.PREF_GOOGLE_AVATAR_URL)
                .remove(Constants.PREF_LAST_SYNC_TIMESTAMP)
                .apply()
        }

        val isSignedIn = hasEmailPref && !accountExternallyRemoved
        val lastSync =
            if (isSignedIn) encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L) else 0L

        val newStatus =
            when {
                !isSignedIn -> SyncStatus.NOT_SIGNED_IN
                lastSync > 0L -> SyncStatus.SYNCED
                else -> SyncStatus.IDLE
            }
        _uiState.value = _uiState.value.copy(syncStatus = newStatus, lastSyncTimestamp = lastSync)
    }

    init {
        refreshSignInStatus()

        // Observe background download progress from SyncProgressHolder
        viewModelScope.launch {
            com.bestjournal.app.domain.usecase.SyncProgressHolder.status.collect { status ->
                if (status == SyncStatus.DOWNLOADING || status == SyncStatus.UPLOADING) {
                    _uiState.value = _uiState.value.copy(syncStatus = status)
                } else if (status == SyncStatus.SYNCED) {
                    _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCED)
                }
            }
        }
        viewModelScope.launch {
            com.bestjournal.app.domain.usecase.SyncProgressHolder.downloadCurrent.collect { current
                ->
                _uiState.value = _uiState.value.copy(downloadCurrent = current)
            }
        }
        viewModelScope.launch {
            com.bestjournal.app.domain.usecase.SyncProgressHolder.downloadTotal.collect { total ->
                _uiState.value = _uiState.value.copy(downloadTotal = total)
            }
        }

        // Load current streak into UI state
        _uiState.value =
            _uiState.value.copy(
                currentStreak = streakTracker.getCurrentStreak(),
                longestStreak = streakTracker.getLongestStreak(),
            )

        // Load today's writing prompt
        val todaysPrompt = DailyPromptProvider.getTodaysPrompt(context)
        val promptDismissedDate = encryptedPrefs.getString(Constants.PREF_PROMPT_DISMISSED_DATE, "")
        val todayStr = java.time.LocalDate.now().toString()
        val isPromptDismissed = promptDismissedDate == todayStr
        _uiState.value =
            _uiState.value.copy(
                dailyPromptText = todaysPrompt.text,
                dailyPromptCategory = context.getString(todaysPrompt.category.displayNameResId),
                dailyPromptId = todaysPrompt.id,
                isPremiumUser =
                    billingManager.subscriptionState.value is SubscriptionState.Subscribed,
                showPromptBanner = !isPromptDismissed,
            )
        if (!isPromptDismissed) {
            analyticsTracker.trackDailyPromptShown(todaysPrompt.id)
        }

        // Listen for sync-in-progress changes from other ViewModels (e.g. delete in EntryDetail).
        // Stored as a field so it can be unregistered in onCleared() to prevent a listener leak.
        encryptedPrefs.registerOnSharedPreferenceChangeListener(syncPrefsListener)

        // Auto-pull the custom analysis prompt from Drive at app start, so a
        // second device that just opens the app (without visiting Settings) still
        // picks up a newer copy uploaded from another device.
        val isSignedIn =
            encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, "")?.isNotBlank() == true
        if (isSignedIn) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val updated = syncWithDriveUseCase.syncCustomPromptFromDriveIfNewer()
                    if (updated) {
                        android.util.Log.d(
                            "JournalVM",
                            "Custom analysis prompt pulled from Drive at app start",
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "JournalVM",
                        "Startup custom-prompt sync failed (non-critical): ${e.message}",
                    )
                }
            }
        }

        // Observe subscription state changes
        viewModelScope.launch {
            billingManager.subscriptionState.collect { state ->
                _uiState.update { it.copy(isPremiumUser = state is SubscriptionState.Subscribed) }
            }
        }

        // Backfill summaries for existing entries without title/summary.
        // Runs exactly once at startup (first non-empty snapshot). Sequential with pauses to avoid
        // hitting Gemini rate limits. Using first{} instead of collect{} prevents repeated calls
        // on every DB change which would cause duplicate Gemini requests.
        viewModelScope.launch {
            val list = entries.first { it.isNotEmpty() }
            val missing =
                list
                    .filter {
                        (it.summary.isNullOrBlank() || it.title.isNullOrBlank()) &&
                            it.displayText.isNotBlank()
                    }
                    .take(3)
            for (entry in missing) {
                summarizeEntryUseCase(entry.id, entry.displayText)
                delay(3_000)
            }
        }
    }

    fun toggleRecording() {
        if (_uiState.value.recordingState == RecordingState.RECORDING) {
            stopRecording()
        } else if (_uiState.value.recordingState == RecordingState.IDLE) {
            startRecording()
        }
    }

    private fun startRecording() {
        val audioFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
        currentAudioFile = audioFile
        _uiState.value =
            _uiState.value.copy(
                recordingState = RecordingState.RECORDING,
                errorMessage = null,
                transcriptionModel = "",
            )

        recordingJob = viewModelScope.launch {
            // Play tone FIRST, then start recording after tone finishes
            val soundsEnabled =
                encryptedPrefs.getBoolean(
                    com.bestjournal.app.util.Constants.PREF_SOUNDS_ENABLED,
                    true,
                )
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
                    kotlinx.coroutines.delay(beepMs.toLong() + 100) // Wait for tone to finish
                    track.release()
                } catch (_: Exception) {
                    /* Tone is optional */
                }

            // NOW start recording — tone is done; request audio focus first
            requestAudioFocus()
            try {
                recordAudioUseCase.startRecording(audioFile)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        recordingState = RecordingState.IDLE,
                        errorMessage =
                            context.getString(R.string.journal_recording_error, e.message ?: ""),
                    )
            }
        }

        // Auto-stop after max duration
        viewModelScope.launch {
            val maxMinutes =
                encryptedPrefs.getInt(
                    com.bestjournal.app.util.Constants.PREF_MAX_RECORDING_DURATION,
                    5, // Default 5 minutes (matches SettingsViewModel default)
                )
            delay(maxMinutes * 60 * 1000L)
            if (_uiState.value.recordingState == RecordingState.RECORDING) {
                stopRecording()
            }
        }
    }

    private fun stopRecording() {
        recordAudioUseCase.stopRecording()
        releaseAudioFocus()

        viewModelScope.launch {
            // Wait for the recording coroutine to finish writing the WAV header
            recordingJob?.join()

            _uiState.value = _uiState.value.copy(recordingState = RecordingState.TRANSCRIBING)

            val audioFile = currentAudioFile ?: return@launch
            transcribeAudioUseCase(audioFile)
                .onSuccess { result ->
                    _uiState.value =
                        _uiState.value.copy(
                            recordingState = RecordingState.PREVIEW,
                            rawText = result.text.trim(),
                            showPreviewDialog = true,
                            transcriptionModel = result.model,
                        )
                    audioFile.delete()

                    // Auto-trigger AI text improvement if enabled in settings
                    val autoImprove =
                        encryptedPrefs.getBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, false)
                    if (autoImprove) {
                        improveText()
                    }
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            recordingState = RecordingState.IDLE,
                            errorMessage =
                                context.getString(
                                    R.string.journal_transcription_error,
                                    error.message ?: "",
                                ),
                        )
                    audioFile.delete()
                }
        }
    }

    fun improveText() {
        val rawText = _uiState.value.rawText
        if (rawText.isBlank()) return

        viewModelScope.launch {
            // Check access INSIDE the coroutine to prevent race conditions on double-tap
            val subState = billingManager.subscriptionState.value
            val accessResult = aiRateLimiter.checkTextAccess(subState)

            when (accessResult) {
                is TieredAccessResult.HardLimitReached -> {
                    _uiState.update { it.copy(showAiLimitReached = true) }
                    return@launch
                }
                is TieredAccessResult.Cooldown -> {
                    _uiState.update {
                        it.copy(
                            errorMessage =
                                context.getString(
                                    R.string.journal_rate_limit,
                                    accessResult.minutesLeft,
                                )
                        )
                    }
                    return@launch
                }
                is TieredAccessResult.Allowed -> {
                    // Proceed with the allowed model
                }
            }

            val modelName =
                (accessResult as? TieredAccessResult.Allowed)?.modelName ?: return@launch
            _uiState.value = _uiState.value.copy(recordingState = RecordingState.IMPROVING)

            // Record attempt (daily + hourly counters) — errors are OK here
            aiRateLimiter.recordTextAttempt()
            improveTextUseCase(rawText, modelName = modelName)
                .onSuccess { improved ->
                    // Only count toward the free weekly limit on SUCCESS — errors don't count
                    aiRateLimiter.recordTextSuccess()
                    // Track text improvement count
                    val count = encryptedPrefs.getInt(Constants.PREF_TEXT_IMPROVEMENT_COUNT, 0) + 1
                    encryptedPrefs
                        .edit()
                        .putInt(Constants.PREF_TEXT_IMPROVEMENT_COUNT, count)
                        .apply()

                    // Check if this is the first text improvement for free users
                    val isFree = billingManager.subscriptionState.value is SubscriptionState.Free
                    val alreadyShown =
                        encryptedPrefs.getBoolean(Constants.PREF_FIRST_TEXT_UPSELL_SHOWN, false)
                    val onboardingDone =
                        encryptedPrefs.getBoolean(Constants.PREF_ONBOARDING_COMPLETED, false)
                    val showUpsell = isFree && !alreadyShown && onboardingDone && count >= 1

                    analyticsTracker.trackEntryImproved()
                    if (showUpsell) {
                        analyticsTracker.trackUpsellBannerShown("first_text")
                    }

                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.PREVIEW,
                            improvedText = improved,
                            isImproveEnabled = true,
                            showTextUpsellBanner = showUpsell,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.PREVIEW,
                            errorMessage =
                                context.getString(
                                    R.string.journal_improve_error,
                                    error.message ?: "",
                                ),
                        )
                    }
                }
        }
    }

    fun setUseImprovedText(use: Boolean) {
        _uiState.value = _uiState.value.copy(isImproveEnabled = use)
    }

    fun updatePreviewText(newText: String) {
        val state = _uiState.value
        if (state.isImproveEnabled && state.improvedText != null) {
            _uiState.value = state.copy(improvedText = newText)
        } else {
            _uiState.value = state.copy(rawText = newText)
        }
    }

    fun saveEntry() {
        // Guard against double-save (e.g. rapid taps before resetState runs)
        if (_uiState.value.recordingState == RecordingState.SAVING) return
        android.util.Log.d(
            "SaveEntry",
            "saveEntry called, rawText=${_uiState.value.rawText.take(30)}",
        )
        val state = _uiState.value
        val userText =
            if (state.isImproveEnabled && state.improvedText != null) {
                state.improvedText
            } else {
                state.rawText
            }

        if (userText.isBlank()) return

        // Prepend the writing prompt if active
        val displayText =
            if (state.activePrompt.isNotBlank()) {
                "${state.activePrompt}\n\n$userText"
            } else {
                userText
            }

        _uiState.value = state.copy(recordingState = RecordingState.SAVING)

        viewModelScope.launch(Dispatchers.IO) {
            val entry =
                JournalEntry(
                    timestamp = System.currentTimeMillis(),
                    rawText = state.rawText,
                    improvedText = state.improvedText,
                    isImproved = state.isImproveEnabled && state.improvedText != null,
                    displayText = displayText,
                    audioDurationSeconds = durationSeconds.value,
                )
            try {
                android.util.Log.d("SaveEntry", "Saving entry, displayText=${displayText.take(30)}")
                val savedId = saveJournalEntryUseCase(entry)
                android.util.Log.d("SaveEntry", "Entry saved with id=$savedId")
                val source = if (entry.audioDurationSeconds > 0) "voice" else "text"
                analyticsTracker.trackEntryCreated(source)
                // If entry was from a writing prompt, dismiss the banner for today
                if (state.activePrompt.isNotBlank()) {
                    val todayStr = java.time.LocalDate.now().toString()
                    // Use commit() (synchronous) so the dismissed date survives abrupt process
                    // termination — apply() can lose the write if Android force-kills the process.
                    encryptedPrefs
                        .edit()
                        .putString(Constants.PREF_PROMPT_DISMISSED_DATE, todayStr)
                        .commit()
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    resetState()
                    // Hide prompt banner after using it
                    if (state.activePrompt.isNotBlank()) {
                        _uiState.update { it.copy(showPromptBanner = false) }
                    }
                }
                // Update streak
                try {
                    streakTracker.recordEntry()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                currentStreak = streakTracker.getCurrentStreak(),
                                longestStreak = streakTracker.getLongestStreak(),
                            )
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("JournalVM", "Streak update failed", e)
                }
                // Background tasks — best effort
                // Background tasks — best effort, but CancellationException must propagate
                try {
                    summarizeEntryUseCase(savedId, displayText)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("JournalVM", "Background summarize failed", e)
                }
                try {
                    triggerSync()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("JournalVM", "Background triggerSync failed", e)
                }
                try {
                    triggerDebouncedAnalysis()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("JournalVM", "Background analysis failed", e)
                }
                // Check achievements after entry save
                try {
                    val totalEntries = journalRepository.getEntryCount()
                    val wordCount = countWords(displayText)
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    val nightCount = encryptedPrefs.getInt("stat_night_entries", 0)
                    val morningCount = encryptedPrefs.getInt("stat_morning_entries", 0)
                    val voiceCount = encryptedPrefs.getInt("stat_voice_entries", 0)
                    val dashCount = encryptedPrefs.getInt("stat_dashboard_refreshes", 0)
                    val photoCount = encryptedPrefs.getInt("stat_photo_count", 0)
                    val longestWords = encryptedPrefs.getInt("stat_longest_entry_words", 0)

                    // Update running stats
                    // "nach 22 Uhr" = 22:00-23:59, "vor 7 Uhr" = 0:00-6:59
                    val isNight = hour >= 22
                    val isMorning = hour < 7
                    val editor = encryptedPrefs.edit()
                    if (isNight) editor.putInt("stat_night_entries", nightCount + 1)
                    if (isMorning) editor.putInt("stat_morning_entries", morningCount + 1)
                    if (entry.audioDurationSeconds > 0)
                        editor.putInt("stat_voice_entries", voiceCount + 1)
                    if (wordCount > longestWords)
                        editor.putInt("stat_longest_entry_words", wordCount)
                    editor.apply()

                    val stats =
                        AchievementStats(
                            totalEntries = totalEntries,
                            nightEntries = if (isNight) nightCount + 1 else nightCount,
                            morningEntries = if (isMorning) morningCount + 1 else morningCount,
                            longestEntryWords = maxOf(longestWords, wordCount),
                            currentStreak = streakTracker.getCurrentStreak(),
                            photoCount = photoCount,
                            voiceEntries =
                                if (entry.audioDurationSeconds > 0) voiceCount + 1 else voiceCount,
                            // photoCount and dashboardRefreshes are incremented elsewhere
                            dashboardRefreshes = dashCount,
                        )
                    val newlyUnlocked = achievementTracker.checkAchievements(stats)
                    if (newlyUnlocked.isNotEmpty()) {
                        // Inner try so that getTitle() failure doesn't prevent _reviewEvent update
                        val title =
                            try {
                                achievementTracker.getTitle(context, newlyUnlocked.first())
                            } catch (e: Exception) {
                                android.util.Log.w("JournalVM", "getTitle failed", e)
                                null
                            }
                        if (title != null) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                _achievementUnlocked.value = title
                            }
                        }
                    }

                    // Signal UI to trigger in-app review at this positive moment
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _reviewEvent.value = totalEntries
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    android.util.Log.w("JournalVM", "Achievement check failed", e)
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "SaveEntry",
                    "SAVE FAILED: ${e.javaClass.simpleName}: ${e.message}",
                    e,
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value =
                        _uiState.value.copy(
                            recordingState = RecordingState.PREVIEW,
                            errorMessage =
                                context.getString(R.string.journal_save_error, e.message ?: ""),
                        )
                }
            }
        }
    }

    fun startTextEntry() {
        _uiState.value =
            _uiState.value.copy(
                recordingState = RecordingState.PREVIEW,
                rawText = "",
                improvedText = null,
                isImproveEnabled = false,
                showPreviewDialog = true,
                transcriptionModel = "",
            )
    }

    fun startTextEntryWithPrompt(prompt: String) {
        analyticsTracker.trackDailyPromptUsed(_uiState.value.dailyPromptId)
        _uiState.value =
            _uiState.value.copy(
                recordingState = RecordingState.PREVIEW,
                rawText = "",
                improvedText = null,
                isImproveEnabled = false,
                showPreviewDialog = true,
                transcriptionModel = "",
                activePrompt = prompt,
            )
    }

    fun dismissPreviewForRecording() {
        _uiState.update { it.copy(showPreviewDialog = false, recordingState = RecordingState.IDLE) }
        // activePrompt is intentionally preserved — dialog reopens after transcription
    }

    fun dismissPromptBanner() {
        val todayStr = java.time.LocalDate.now().toString()
        // commit() (synchronous) ensures the dismissed date is written to disk before returning,
        // so the banner stays dismissed even if Android force-kills the process right after.
        encryptedPrefs.edit().putString(Constants.PREF_PROMPT_DISMISSED_DATE, todayStr).commit()
        _uiState.update { it.copy(showPromptBanner = false) }
    }

    fun onPromptPremiumBlocked() {
        analyticsTracker.trackDailyPromptPremiumBlocked()
    }

    fun dismissPreview() {
        resetState()
    }

    fun dismissAiLimitReached() {
        _uiState.update { it.copy(showAiLimitReached = false) }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSearch() {
        _uiState.value =
            _uiState.value.copy(isSearchActive = !_uiState.value.isSearchActive, searchQuery = "")
    }

    fun searchEntries(query: String) = journalRepository.searchEntries(query)

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun consumeReviewEvent() {
        _reviewEvent.value = null
    }

    fun dismissTextUpsellBanner() {
        encryptedPrefs.edit().putBoolean(Constants.PREF_FIRST_TEXT_UPSELL_SHOWN, true).apply()
        _uiState.update { it.copy(showTextUpsellBanner = false) }
    }

    fun onTextUpsellClicked() {
        analyticsTracker.trackUpsellBannerClicked("first_text")
        dismissTextUpsellBanner()
    }

    suspend fun triggerInAppReview(activity: android.app.Activity, totalEntries: Int) {
        inAppReviewHelper.maybeShowReview(activity, totalEntries)
    }

    private fun resetState() {
        // If the text upsell banner was shown but not explicitly dismissed, mark it as shown
        if (_uiState.value.showTextUpsellBanner) {
            encryptedPrefs.edit().putBoolean(Constants.PREF_FIRST_TEXT_UPSELL_SHOWN, true).apply()
        }
        val currentState = _uiState.value
        _uiState.value =
            JournalUiState(
                // Preserve streak, sync, and prompt state across resets to avoid UI flicker
                currentStreak = currentState.currentStreak,
                longestStreak = currentState.longestStreak,
                syncStatus = currentState.syncStatus,
                lastSyncTimestamp = currentState.lastSyncTimestamp,
                dailyPromptText = currentState.dailyPromptText,
                dailyPromptCategory = currentState.dailyPromptCategory,
                dailyPromptId = currentState.dailyPromptId,
                isPremiumUser = currentState.isPremiumUser,
                showPromptBanner = currentState.showPromptBanner,
            )
    }

    private fun triggerSync() {
        syncDebounceJob?.cancel()
        syncDebounceJob =
            viewModelScope.launch(Dispatchers.IO) {
                _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.UPLOADING)
                syncWithDriveUseCase
                    .backup()
                    .onSuccess {
                        _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCED)
                        encryptedPrefs
                            .edit()
                            .putLong(Constants.PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
                            .apply()
                    }
                    .onFailure {
                        android.util.Log.e("SyncDebug", "Backup failed: ${it.message}", it)
                        _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.ERROR)
                    }
            }
    }

    fun retrySyncNow() {
        triggerSync()
    }

    /**
     * Count words using BreakIterator — works for all languages including CJK (Chinese, Japanese,
     * Korean) where words are not separated by spaces.
     */
    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        val iterator = java.text.BreakIterator.getWordInstance(java.util.Locale.getDefault())
        iterator.setText(text)
        var count = 0
        var start = iterator.first()
        var end = iterator.next()
        while (end != java.text.BreakIterator.DONE) {
            val word = text.substring(start, end).trim()
            if (word.isNotEmpty() && word.any { it.isLetterOrDigit() }) {
                count++
            }
            start = end
            end = iterator.next()
        }
        return count
    }

    override fun onCleared() {
        super.onCleared()
        try {
            encryptedPrefs.unregisterOnSharedPreferenceChangeListener(syncPrefsListener)
        } catch (e: Exception) {
            android.util.Log.w("JournalVM", "Failed to unregister prefs listener", e)
        }
        // Release AudioFocus on ViewModel teardown so other audio apps (Spotify, phone call)
        // can reclaim focus immediately after the app is killed mid-recording. Without this
        // the focus request remains registered until the OS process cleanup runs.
        try {
            releaseAudioFocus()
        } catch (e: Exception) {
            android.util.Log.w("JournalVM", "Failed to release audio focus on teardown", e)
        }
    }

    private fun triggerDebouncedAnalysis() {
        val autoUpdate = encryptedPrefs.getBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, true)
        if (!autoUpdate) return

        analysisDebounceJob?.cancel()
        analysisDebounceJob = viewModelScope.launch {
            delay(3_000)
            // Sync scenario so per-scenario counter tracks the correct profile
            val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
            aiRateLimiter.setCurrentScenario(scenario)
            // Check access for auto-update — silently skip if limit reached
            val subState = billingManager.subscriptionState.value
            val accessResult = aiRateLimiter.checkDashboardAccess(subState)
            if (accessResult !is TieredAccessResult.Allowed) {
                return@launch // Silently skip — don't show error for background updates
            }
            encryptedPrefs
                .edit()
                .putBoolean(Constants.PREF_DASHBOARD_UPDATE_IS_DELETE, false)
                .putBoolean(Constants.PREF_DASHBOARD_UPDATING, true)
                .putLong("dashboard_update_started_at", System.currentTimeMillis())
                .apply()
            try {
                aiRateLimiter.recordDashboardAttempt()
                analyzeEntropyUseCase(freshAnalysis = true, modelName = accessResult.modelName)
                aiRateLimiter.recordDashboardSuccess()
                // Write timestamp to the scenario-specific key so getLastUpdatedText() finds it
                encryptedPrefs
                    .edit()
                    .putLong("dashboard_last_updated_$scenario", System.currentTimeMillis())
                    .apply()
                // Increment analysis count so the dashboard upsell banner can trigger
                val count = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_ANALYSIS_COUNT, 0) + 1
                encryptedPrefs.edit().putInt(Constants.PREF_DASHBOARD_ANALYSIS_COUNT, count).apply()
            } finally {
                encryptedPrefs.edit().putBoolean(Constants.PREF_DASHBOARD_UPDATING, false).apply()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DEBUG: Test data seeder (toggle button in journal top bar, debug builds only)
    // ─────────────────────────────────────────────────────────────────────────

    init {
        _uiState.update { it.copy(seedSeededCount = testDataSeeder.getSeededCount()) }
    }

    // --- Debug-Test-Daten-Feature (Dialog: Hinzufuegen / Alle loeschen) ---

    fun onSeedButtonClicked() {
        _uiState.update { it.copy(showSeedDialog = true) }
    }

    fun dismissSeedDialog() {
        _uiState.update { it.copy(showSeedDialog = false) }
    }

    /** Dialog-Aktion "Hinzufuegen": erzeugt 70 Test-Eintraege. */
    fun seedTestData() {
        runSeedAction { testDataSeeder.seed().map { count -> SeedActionResult.Seeded(count) } }
    }

    /**
     * Dialog-Aktion "Alle loeschen": loescht ROBUST alle Eintraege via DELETE-Query
     * (nicht mehr per fragilem ID-Tracking, das verwaisen konnte) und vergisst das Seed-Tracking.
     */
    fun deleteAllEntriesNow() {
        runSeedAction {
            val count = journalRepository.deleteAllEntries()
            testDataSeeder.forgetSeededIds()
            Result.success(SeedActionResult.Deleted(count))
        }
    }

    private fun runSeedAction(block: suspend () -> Result<SeedActionResult>) {
        if (_uiState.value.seedRunning) return
        _uiState.update { it.copy(showSeedDialog = false) }
        viewModelScope.launch {
            _uiState.update { it.copy(seedRunning = true, seedToastMessage = null) }
            val result =
                try {
                    block()
                } catch (e: Exception) {
                    Result.failure(e)
                }
            result.fold(
                onSuccess = { action ->
                    val message =
                        when (action) {
                            is SeedActionResult.Seeded ->
                                context.getString(R.string.dev_seed_done, action.count)
                            is SeedActionResult.Deleted ->
                                context.getString(R.string.dev_seed_deleted, action.count)
                        }
                    _uiState.update {
                        it.copy(
                            seedRunning = false,
                            seedSeededCount = testDataSeeder.getSeededCount(),
                            seedToastMessage = message,
                        )
                    }
                },
                onFailure = { err ->
                    val message =
                        context.getString(
                            R.string.dev_seed_error,
                            err.message ?: err.javaClass.simpleName,
                        )
                    _uiState.update {
                        it.copy(
                            seedRunning = false,
                            seedSeededCount = testDataSeeder.getSeededCount(),
                            seedToastMessage = message,
                        )
                    }
                },
            )
        }
    }

    fun consumeSeedToast() {
        _uiState.update { it.copy(seedToastMessage = null) }
    }

    private sealed class SeedActionResult {
        data class Seeded(val count: Int) : SeedActionResult()
        data class Deleted(val count: Int) : SeedActionResult()
    }
}
