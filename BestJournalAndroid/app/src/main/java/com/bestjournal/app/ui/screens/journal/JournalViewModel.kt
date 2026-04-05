package com.bestjournal.app.ui.screens.journal

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bestjournal.app.billing.BillingManager
import com.bestjournal.app.data.repository.JournalRepository
import com.bestjournal.app.domain.model.JournalEntry
import com.bestjournal.app.domain.usecase.AnalyzeEntropyUseCase
import com.bestjournal.app.domain.usecase.ImproveTextUseCase
import com.bestjournal.app.domain.usecase.RecordAudioUseCase
import com.bestjournal.app.domain.usecase.SaveJournalEntryUseCase
import com.bestjournal.app.domain.usecase.SummarizeEntryUseCase
import com.bestjournal.app.domain.usecase.SyncWithDriveUseCase
import com.bestjournal.app.domain.usecase.TranscribeAudioUseCase
import com.bestjournal.app.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val showAiLimitReached: Boolean = false,
    val transcriptionModel: String = "Lokales Whisper-Modell",
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR,
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
) : ViewModel() {

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

    private var currentAudioFile: File? = null
    private var recordingJob: Job? = null
    private var syncDebounceJob: Job? = null
    private var analysisDebounceJob: Job? = null

    init {
        // Set initial sync status: check if signed in AND last backup timestamp exists
        val isSignedIn = encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, "")?.isNotBlank() == true
        val lastSync = encryptedPrefs.getLong(Constants.PREF_LAST_SYNC_TIMESTAMP, 0L)
        if (isSignedIn && lastSync > 0L) {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCED)
        } else if (isSignedIn) {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.IDLE)
        }

        // Backfill summaries for existing entries without title/summary.
        // Sequential with pauses to avoid hitting Gemini rate limits.
        viewModelScope.launch {
            entries.collect { list ->
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
                return@collect
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
        _uiState.value = _uiState.value.copy(
            recordingState = RecordingState.RECORDING,
            errorMessage = null,
            transcriptionModel = ""
        )

        recordingJob = viewModelScope.launch {
            // Play tone FIRST, then start recording after tone finishes
            val soundsEnabled = encryptedPrefs.getBoolean(com.bestjournal.app.util.Constants.PREF_SOUNDS_ENABLED, true)
            val beepMs = 150
            if (soundsEnabled) try {
                val sampleRate = 44100
                val beepSamples = sampleRate * beepMs / 1000
                val samples = ShortArray(beepSamples)
                val freq = 880.0
                val fadeLen = beepSamples / 8
                for (i in 0 until beepSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = when {
                        i < fadeLen -> i.toDouble() / fadeLen
                        i > beepSamples - fadeLen -> (beepSamples - i).toDouble() / fadeLen
                        else -> 1.0
                    }
                    samples[i] = (Short.MAX_VALUE * 0.5 * envelope * kotlin.math.sin(2 * Math.PI * freq * t)).toInt().toShort()
                }
                val track = android.media.AudioTrack(
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
                    android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
                )
                track.write(samples, 0, beepSamples)
                track.play()
                kotlinx.coroutines.delay(beepMs.toLong() + 100) // Wait for tone to finish
                track.release()
            } catch (_: Exception) { /* Tone is optional */ }

            // NOW start recording — tone is done
            try {
                recordAudioUseCase.startRecording(audioFile)
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(
                        recordingState = RecordingState.IDLE,
                        errorMessage = "Aufnahme fehlgeschlagen: ${e.message}",
                    )
            }
        }

        // Auto-stop after max duration
        viewModelScope.launch {
            delay(com.bestjournal.app.util.Constants.MAX_RECORDING_DURATION_MINUTES * 60 * 1000L)
            if (_uiState.value.recordingState == RecordingState.RECORDING) {
                stopRecording()
            }
        }
    }

    private fun stopRecording() {
        recordAudioUseCase.stopRecording()

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
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            recordingState = RecordingState.IDLE,
                            errorMessage = "Transkription fehlgeschlagen: ${error.message}",
                        )
                    audioFile.delete()
                }
        }
    }

    fun improveText() {
        val rawText = _uiState.value.rawText
        if (rawText.isBlank()) return

        _uiState.value = _uiState.value.copy(recordingState = RecordingState.IMPROVING)

        viewModelScope.launch {
            improveTextUseCase(rawText)
                .onSuccess { improved ->
                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.PREVIEW,
                            improvedText = improved,
                            isImproveEnabled = true,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            recordingState = RecordingState.PREVIEW,
                            errorMessage =
                                "Textverbesserung fehlgeschlagen: ${error.message}",
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
        android.util.Log.d("SaveEntry", "saveEntry called, rawText=${_uiState.value.rawText.take(30)}")
        val state = _uiState.value
        val displayText =
            if (state.isImproveEnabled && state.improvedText != null) {
                state.improvedText
            } else {
                state.rawText
            }

        if (displayText.isBlank()) return

        _uiState.value = state.copy(recordingState = RecordingState.SAVING)

        // Use independent scope — viewModelScope can be cancelled by Android
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
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
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    resetState()
                }
                // Background tasks — best effort
                try { summarizeEntryUseCase(savedId, displayText) } catch (_: Exception) {}
                try { triggerSync() } catch (_: Exception) {}
                try { triggerDebouncedAnalysis() } catch (_: Exception) {}
            } catch (e: Exception) {
                android.util.Log.e("SaveEntry", "SAVE FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        recordingState = RecordingState.PREVIEW,
                        errorMessage = "Speichern fehlgeschlagen: ${e.message}"
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
            )
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

    private fun resetState() {
        _uiState.value = JournalUiState()
    }

    private fun triggerSync() {
        syncDebounceJob?.cancel()
        syncDebounceJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCING)
            syncWithDriveUseCase
                .backup()
                .onSuccess { _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCED) }
                .onFailure { _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.ERROR) }
        }
    }

    private fun triggerDebouncedAnalysis() {
        val autoUpdate = encryptedPrefs.getBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, true)
        if (!autoUpdate) return

        analysisDebounceJob?.cancel()
        analysisDebounceJob = viewModelScope.launch {
            encryptedPrefs.edit().putBoolean(Constants.PREF_DASHBOARD_UPDATING, true).apply()
            try {
                delay(3_000)
                analyzeEntropyUseCase(freshAnalysis = true)
                encryptedPrefs
                    .edit()
                    .putLong(Constants.PREF_DASHBOARD_LAST_UPDATED, System.currentTimeMillis())
                    .apply()
            } finally {
                encryptedPrefs.edit().putBoolean(Constants.PREF_DASHBOARD_UPDATING, false).apply()
            }
        }
    }
}
