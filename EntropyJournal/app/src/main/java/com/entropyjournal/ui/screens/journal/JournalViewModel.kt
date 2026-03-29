package com.entropyjournal.ui.screens.journal

import android.content.Context
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
    @ApplicationContext private val context: Context
) : ViewModel() {

    val entries = journalRepository.getAllEntries()
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
                        rawText = text,
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

        _uiState.value = state.copy(recordingState = RecordingState.SAVING)

        viewModelScope.launch {
            val entry = JournalEntry(
                timestamp = System.currentTimeMillis(),
                rawText = state.rawText,
                improvedText = state.improvedText,
                isImproved = state.isImproveEnabled && state.improvedText != null,
                displayText = displayText,
                audioDurationSeconds = durationSeconds.value
            )
            val savedId = saveJournalEntryUseCase(entry)
            resetState()
            // Generate summary in background (non-blocking)
            launch { summarizeEntryUseCase(savedId, displayText) }
            triggerDebouncedSync()
            triggerDebouncedAnalysis()
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

    private fun triggerDebouncedSync() {
        syncDebounceJob?.cancel()
        syncDebounceJob = viewModelScope.launch {
            delay(30_000)
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
        analysisDebounceJob?.cancel()
        analysisDebounceJob = viewModelScope.launch {
            delay(60_000)
            analyzeEntropyUseCase()
        }
    }
}
