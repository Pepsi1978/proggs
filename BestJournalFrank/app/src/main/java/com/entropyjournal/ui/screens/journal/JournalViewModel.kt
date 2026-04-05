package com.entropyjournal.ui.screens.journal

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.repository.JournalRepository
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.domain.usecase.AnalyzeEntropyUseCase
import com.entropyjournal.domain.usecase.ImproveTextUseCase
import com.entropyjournal.domain.usecase.RecordAudioUseCase
import com.entropyjournal.domain.usecase.SaveJournalEntryUseCase
import com.entropyjournal.domain.usecase.SummarizeEntryUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.domain.usecase.TranscribeAudioUseCase
import com.entropyjournal.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class RecordingState {
    IDLE, RECORDING, TRANSCRIBING, IMPROVING, PREVIEW, SAVING
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
    val syncStatus: SyncStatus = SyncStatus.IDLE
)

enum class SyncStatus { IDLE, SYNCING, SYNCED, ERROR }

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val improveTextUseCase: ImproveTextUseCase,
    private val saveJournalEntryUseCase: SaveJournalEntryUseCase,
    private val summarizeEntryUseCase: SummarizeEntryUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val syncWithDriveUseCase: SyncWithDriveUseCase,
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences
) : ViewModel() {

    val entries = journalRepository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(JournalUiState())
    val uiState: StateFlow<JournalUiState> = _uiState

    val amplitude: StateFlow<Float> = recordAudioUseCase.amplitude
    val durationSeconds: StateFlow<Int> = recordAudioUseCase.durationSeconds

    fun isGroqActive(): Boolean {
        val key = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: ""
        return key.isNotBlank()
    }

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

        // Backfill summaries for existing entries that don't have one yet
        viewModelScope.launch {
            entries.collect { list ->
                val missing = list.filter { (it.summary.isNullOrBlank() || it.title.isNullOrBlank()) && it.displayText.isNotBlank() }
                if (missing.isNotEmpty()) {
                    for (entry in missing) {
                        launch { summarizeEntryUseCase(entry.id, entry.displayText) }
                    }
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
        _uiState.value = _uiState.value.copy(recordingState = RecordingState.RECORDING, errorMessage = null)

        recordingJob = viewModelScope.launch {
            // Play tone FIRST, then start recording after tone finishes
            val soundsEnabled = encryptedPrefs.getBoolean(com.entropyjournal.util.Constants.PREF_SOUNDS_ENABLED, true)
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
                kotlinx.coroutines.delay(beepMs.toLong() + 100)
                track.release()
            } catch (_: Exception) { /* Tone is optional */ }

            // NOW start recording — tone is done
            try {
                recordAudioUseCase.startRecording(audioFile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    recordingState = RecordingState.IDLE,
                    errorMessage = "Aufnahme fehlgeschlagen: ${e.message}"
                )
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
                .onSuccess { text ->
                    _uiState.value = _uiState.value.copy(
                        recordingState = RecordingState.PREVIEW,
                        rawText = text.trim(),
                        showPreviewDialog = true
                    )
                    audioFile.delete()
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        recordingState = RecordingState.IDLE,
                        errorMessage = "Transkription fehlgeschlagen: ${error.message}"
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
                    _uiState.value = _uiState.value.copy(
                        recordingState = RecordingState.PREVIEW,
                        improvedText = improved,
                        isImproveEnabled = true
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        recordingState = RecordingState.PREVIEW,
                        errorMessage = "Textverbesserung fehlgeschlagen: ${error.message}"
                    )
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
        val state = _uiState.value
        val displayText = if (state.isImproveEnabled && state.improvedText != null) {
            state.improvedText
        } else {
            state.rawText
        }

        if (displayText.isBlank()) return

        _uiState.value = state.copy(recordingState = RecordingState.SAVING)

        // Use independent scope — viewModelScope can be cancelled by Android
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val entry = JournalEntry(
                timestamp = System.currentTimeMillis(),
                rawText = state.rawText,
                improvedText = state.improvedText,
                isImproved = state.isImproveEnabled && state.improvedText != null,
                displayText = displayText,
                audioDurationSeconds = durationSeconds.value
            )
            try {
                val savedId = saveJournalEntryUseCase(entry)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    resetState()
                }
                try { summarizeEntryUseCase(savedId, displayText) } catch (_: Exception) {}
                try { triggerSync() } catch (_: Exception) {}
                try { triggerDebouncedAnalysis() } catch (_: Exception) {}
            } catch (e: Exception) {
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
        _uiState.value = _uiState.value.copy(
            recordingState = RecordingState.PREVIEW,
            rawText = "",
            improvedText = null,
            isImproveEnabled = false,
            showPreviewDialog = true
        )
    }

    fun dismissPreview() {
        resetState()
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSearch() {
        _uiState.value = _uiState.value.copy(
            isSearchActive = !_uiState.value.isSearchActive,
            searchQuery = ""
        )
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
            syncWithDriveUseCase.backup()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.SYNCED)
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(syncStatus = SyncStatus.ERROR)
                }
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
            } finally {
                encryptedPrefs.edit().putBoolean(Constants.PREF_DASHBOARD_UPDATING, false).apply()
            }
        }
    }
}
