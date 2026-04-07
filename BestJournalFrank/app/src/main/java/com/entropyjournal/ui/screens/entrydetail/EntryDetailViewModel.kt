package com.entropyjournal.ui.screens.entrydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.SharedPreferences
import com.entropyjournal.data.repository.JournalRepository
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EntryDetailUiState(
    val entry: JournalEntry? = null,
    val editedDisplayText: String = "",
    val isEditing: Boolean = false,
    val showOriginalText: Boolean = true,
    val showDeleteDialog: Boolean = false,
    val isDeleted: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class EntryDetailViewModel @Inject constructor(
    private val journalRepository: JournalRepository,
    private val syncWithDriveUseCase: SyncWithDriveUseCase,
    private val encryptedPrefs: SharedPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState

    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L
    private var autoSaveJob: Job? = null

    init {
        loadEntry()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            val entry = journalRepository.getEntryById(entryId)
            _uiState.value = _uiState.value.copy(
                entry = entry,
                editedDisplayText = entry?.displayText ?: ""
            )
        }
    }

    fun updateDisplayText(newText: String) {
        _uiState.value = _uiState.value.copy(
            editedDisplayText = newText,
            isEditing = true
        )
        // Debounced auto-save after 1.5 seconds of inactivity
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            saveEditedText()
        }
    }

    private suspend fun saveEditedText() {
        val state = _uiState.value
        val entry = state.entry ?: return
        val newText = state.editedDisplayText.trim()
        if (newText == entry.displayText) {
            _uiState.value = state.copy(isEditing = false)
            return
        }

        _uiState.value = state.copy(isSaving = true)
        val updatedEntry = entry.copy(
            displayText = newText,
            rawText = if (!entry.isImproved) newText else entry.rawText,
            improvedText = if (entry.isImproved) newText else entry.improvedText
        )
        journalRepository.updateEntry(updatedEntry)
        _uiState.value = _uiState.value.copy(
            entry = updatedEntry,
            isEditing = false,
            isSaving = false
        )
    }

    fun saveNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch { saveEditedText() }
    }

    fun updateRawText(newText: String) {
        val entry = _uiState.value.entry ?: return
        _uiState.value = _uiState.value.copy(
            entry = entry.copy(rawText = newText)
        )
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            val current = _uiState.value.entry ?: return@launch
            journalRepository.updateEntry(current)
        }
    }

    fun toggleTextVersion() {
        val state = _uiState.value
        val entry = state.entry ?: return
        val showOriginal = !state.showOriginalText
        _uiState.value = state.copy(
            showOriginalText = showOriginal,
            editedDisplayText = if (showOriginal) entry.rawText else entry.displayText
        )
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = show)
    }

    fun deleteEntry() {
        viewModelScope.launch {
            _uiState.value.entry?.let { entry ->
                journalRepository.deleteEntry(entry)
                // Launch backup in independent scope — viewModelScope gets cancelled on navigation
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        syncWithDriveUseCase.backup()
                        encryptedPrefs.edit()
                            .putLong(Constants.PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
                            .apply()
                    } catch (_: Exception) {}
                }
                _uiState.value = _uiState.value.copy(isDeleted = true, showDeleteDialog = false)
            }
        }
    }
}
