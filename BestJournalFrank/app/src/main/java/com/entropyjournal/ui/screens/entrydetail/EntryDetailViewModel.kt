package com.entropyjournal.ui.screens.entrydetail

import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.local.entity.EntryPhotoEntity
import com.entropyjournal.data.repository.JournalRepository
import com.entropyjournal.data.repository.PhotoRepository
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.domain.usecase.AnalyzeEntropyUseCase
import com.entropyjournal.domain.usecase.ImproveTextUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class EntryDetailUiState(
    val entry: JournalEntry? = null,
    val editedDisplayText: String = "",
    val isEditing: Boolean = false,
    val showOriginalText: Boolean = true,
    val showDeleteDialog: Boolean = false,
    val isDeleted: Boolean = false,
    val isSaving: Boolean = false,
    val isImproving: Boolean = false,
    val improveError: String? = null,
    val photos: List<EntryPhotoEntity> = emptyList(),
)

@HiltViewModel
class EntryDetailViewModel
@Inject
constructor(
    private val journalRepository: JournalRepository,
    private val photoRepository: PhotoRepository,
    private val syncWithDriveUseCase: SyncWithDriveUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val improveTextUseCase: ImproveTextUseCase,
    private val encryptedPrefs: SharedPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState

    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L
    private var autoSaveJob: Job? = null

    init {
        loadEntry()
        loadPhotos()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            val entry = journalRepository.getEntryById(entryId)
            _uiState.value =
                _uiState.value.copy(entry = entry, editedDisplayText = entry?.displayText ?: "")
        }
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            photoRepository.getPhotosForEntry(entryId).collect { photos ->
                _uiState.value = _uiState.value.copy(photos = photos)
            }
        }
    }

    fun addPhotos(uris: List<Uri>) {
        viewModelScope.launch { uris.forEach { uri -> photoRepository.addPhoto(entryId, uri) } }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch { photoRepository.deletePhoto(photoId) }
    }

    fun updateDisplayText(newText: String) {
        _uiState.value = _uiState.value.copy(editedDisplayText = newText, isEditing = true)
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
        val updatedEntry =
            entry.copy(
                displayText = newText,
                rawText = if (!entry.isImproved) newText else entry.rawText,
                improvedText = if (entry.isImproved) newText else entry.improvedText,
            )
        journalRepository.updateEntry(updatedEntry)
        _uiState.value =
            _uiState.value.copy(entry = updatedEntry, isEditing = false, isSaving = false)
    }

    fun saveNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch { saveEditedText() }
    }

    fun updateRawText(newText: String) {
        val entry = _uiState.value.entry ?: return
        _uiState.value = _uiState.value.copy(entry = entry.copy(rawText = newText))
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            val current = _uiState.value.entry ?: return@launch
            journalRepository.updateEntry(current)
        }
    }

    fun improveTextWithAi() {
        val entry = _uiState.value.entry ?: return
        if (entry.rawText.isBlank()) return

        _uiState.value = _uiState.value.copy(isImproving = true, improveError = null)

        viewModelScope.launch {
            improveTextUseCase(entry.rawText)
                .onSuccess { improved ->
                    val updatedEntry =
                        entry.copy(
                            improvedText = improved,
                            isImproved = true,
                            displayText = improved,
                        )
                    journalRepository.updateEntry(updatedEntry)
                    _uiState.value =
                        _uiState.value.copy(
                            entry = updatedEntry,
                            editedDisplayText = improved,
                            isImproving = false,
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        _uiState.value.copy(
                            isImproving = false,
                            improveError = error.message ?: "Textverbesserung fehlgeschlagen",
                        )
                }
        }
    }

    fun clearImproveError() {
        _uiState.value = _uiState.value.copy(improveError = null)
    }

    fun toggleTextVersion() {
        val state = _uiState.value
        val entry = state.entry ?: return
        val showOriginal = !state.showOriginalText
        _uiState.value =
            state.copy(
                showOriginalText = showOriginal,
                editedDisplayText = if (showOriginal) entry.rawText else entry.displayText,
            )
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = show)
    }

    fun deleteEntry() {
        viewModelScope.launch {
            _uiState.value.entry?.let { entry ->
                journalRepository.deleteEntry(entry)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    try {
                        encryptedPrefs
                            .edit()
                            .putBoolean(Constants.PREF_SYNC_IN_PROGRESS, true)
                            .apply()
                        syncWithDriveUseCase.backup()
                        encryptedPrefs
                            .edit()
                            .putBoolean(Constants.PREF_SYNC_IN_PROGRESS, false)
                            .putLong(Constants.PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
                            .apply()
                    } catch (_: Exception) {
                        encryptedPrefs
                            .edit()
                            .putBoolean(Constants.PREF_SYNC_IN_PROGRESS, false)
                            .apply()
                    }
                    val autoUpdate =
                        encryptedPrefs.getBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, true)
                    if (autoUpdate) {
                        try {
                            encryptedPrefs
                                .edit()
                                .putBoolean(Constants.PREF_DASHBOARD_UPDATE_IS_DELETE, true)
                                .putBoolean(Constants.PREF_DASHBOARD_UPDATING, true)
                                .apply()
                            analyzeEntropyUseCase(freshAnalysis = true)
                            val scenario =
                                encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                            encryptedPrefs
                                .edit()
                                .putLong(
                                    "dashboard_last_updated_$scenario",
                                    System.currentTimeMillis(),
                                )
                                .apply()
                        } finally {
                            encryptedPrefs
                                .edit()
                                .putBoolean(Constants.PREF_DASHBOARD_UPDATING, false)
                                .apply()
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(isDeleted = true, showDeleteDialog = false)
            }
        }
    }
}
