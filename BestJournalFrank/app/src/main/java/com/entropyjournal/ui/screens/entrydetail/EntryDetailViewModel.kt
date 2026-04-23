package com.entropyjournal.ui.screens.entrydetail

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.entropyjournal.data.local.entity.EntryFollowUpEntity
import com.entropyjournal.data.local.entity.EntryPhotoEntity
import com.entropyjournal.data.repository.EntryFollowUpRepository
import com.entropyjournal.data.repository.JournalRepository
import com.entropyjournal.data.repository.PhotoRepository
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.domain.usecase.AnalyzeEntropyUseCase
import com.entropyjournal.domain.usecase.ImproveTextUseCase
import com.entropyjournal.domain.usecase.RecordAudioUseCase
import com.entropyjournal.domain.usecase.SummarizeEntryUseCase
import com.entropyjournal.domain.usecase.SyncWithDriveUseCase
import com.entropyjournal.domain.usecase.TranscribeAudioUseCase
import com.entropyjournal.ui.screens.journal.RecordingState
import com.entropyjournal.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    val followUps: List<EntryFollowUpEntity> = emptyList(),
    val showFollowUpDialog: Boolean = false,
    val activeFollowUpId: Long? = null,
    val showDeleteFollowUpDialog: Boolean = false,
    val followUpDraftText: String = "",
    val followUpImprovedText: String? = null,
    val isUsingImprovedFollowUp: Boolean = false,
    val followUpRecordingState: RecordingState = RecordingState.IDLE,
    val followUpError: String? = null,
)

@HiltViewModel
class EntryDetailViewModel
@Inject
constructor(
    private val journalRepository: JournalRepository,
    private val photoRepository: PhotoRepository,
    private val entryFollowUpRepository: EntryFollowUpRepository,
    private val syncWithDriveUseCase: SyncWithDriveUseCase,
    private val analyzeEntropyUseCase: AnalyzeEntropyUseCase,
    private val improveTextUseCase: ImproveTextUseCase,
    private val recordAudioUseCase: RecordAudioUseCase,
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val summarizeEntryUseCase: SummarizeEntryUseCase,
    @ApplicationContext private val context: Context,
    private val encryptedPrefs: SharedPreferences,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EntryDetailUiState())
    val uiState: StateFlow<EntryDetailUiState> = _uiState

    val followUpAmplitude: StateFlow<Float> = recordAudioUseCase.amplitude
    val followUpDurationSeconds: StateFlow<Int> = recordAudioUseCase.durationSeconds

    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L
    private var autoSaveJob: Job? = null
    private var autoBackupJob: Job? = null
    private var followUpRecordingJob: Job? = null
    private var currentFollowUpAudioFile: File? = null
    // Debounced follow-up dashboard refresh: saving (or inline-editing) a Nachtrag
    // counts as new content for the active profile, so the dashboard has to rerun
    // the Gemini analysis. A single shared job is cancelled on repeated edits so
    // fast typing collapses into one API call 3s after the user stops.
    private var followUpAnalysisDebounceJob: Job? = null

    init {
        loadEntry()
        loadPhotos()
        loadFollowUps()
    }

    private fun loadEntry() {
        viewModelScope.launch {
            val entry = journalRepository.getEntryById(entryId)
            _uiState.update {
                it.copy(
                    entry = entry,
                    editedDisplayText = entry?.displayText ?: "",
                )
            }
        }
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            photoRepository.getPhotosForEntry(entryId).collect { photos ->
                _uiState.value = _uiState.value.copy(photos = photos)
            }
        }
    }

    private fun loadFollowUps() {
        viewModelScope.launch {
            entryFollowUpRepository.observeForEntry(entryId).collect { followUps ->
                _uiState.update { it.copy(followUps = followUps) }
            }
        }
    }

    fun isGroqActive(): Boolean {
        val key = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: ""
        return key.isNotBlank()
    }

    @Suppress("OPT_IN_USAGE")
    private fun triggerAutoBackup() {
        val email = encryptedPrefs.getString(Constants.PREF_GOOGLE_ACCOUNT_EMAIL, null)
        if (email.isNullOrBlank()) {
            android.util.Log.w("SyncDebug", "triggerAutoBackup: skipped — no email")
            return
        }
        android.util.Log.d("SyncDebug", "triggerAutoBackup: scheduled in 5s")
        autoBackupJob?.cancel()
        autoBackupJob =
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                delay(5000)
                try {
                    android.util.Log.d("SyncDebug", "triggerAutoBackup: starting backup now")
                    encryptedPrefs.edit().putBoolean(Constants.PREF_SYNC_IN_PROGRESS, true).apply()
                    val result = syncWithDriveUseCase.backup()
                    if (result.isSuccess) {
                        android.util.Log.d("SyncDebug", "triggerAutoBackup: backup SUCCESS")
                    } else {
                        android.util.Log.e(
                            "SyncDebug",
                            "triggerAutoBackup: backup FAILED: ${result.exceptionOrNull()?.message}",
                        )
                    }
                    encryptedPrefs
                        .edit()
                        .putBoolean(Constants.PREF_SYNC_IN_PROGRESS, false)
                        .putLong(Constants.PREF_LAST_SYNC_TIMESTAMP, System.currentTimeMillis())
                        .apply()
                } catch (e: Exception) {
                    android.util.Log.e("SyncDebug", "triggerAutoBackup: EXCEPTION: ${e.message}", e)
                    encryptedPrefs.edit().putBoolean(Constants.PREF_SYNC_IN_PROGRESS, false).apply()
                }
            }
    }

    fun addMedia(uris: List<Uri>) {
        viewModelScope.launch {
            uris.forEach { uri -> photoRepository.addMedia(entryId, uri) }
            triggerAutoBackup()
        }
    }

    fun createCameraUri() = photoRepository.createCameraUri()

    fun onCameraPhotoTaken(file: File) {
        viewModelScope.launch {
            photoRepository.addPhotoFromFile(entryId, file)
            triggerAutoBackup()
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photoId)
            triggerAutoBackup()
        }
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
        triggerAutoBackup()
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
            triggerAutoBackup()
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
                    triggerAutoBackup()
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

    fun openNewFollowUpDialog() {
        _uiState.update {
            it.copy(
                showFollowUpDialog = true,
                activeFollowUpId = null,
                showDeleteFollowUpDialog = false,
                followUpDraftText = "",
                followUpImprovedText = null,
                isUsingImprovedFollowUp = false,
                followUpRecordingState = RecordingState.PREVIEW,
                followUpError = null,
            )
        }
    }

    fun editFollowUp(followUpId: Long) {
        val followUp = _uiState.value.followUps.firstOrNull { it.id == followUpId } ?: return
        _uiState.update {
            it.copy(
                showFollowUpDialog = true,
                activeFollowUpId = followUp.id,
                showDeleteFollowUpDialog = false,
                followUpDraftText = followUp.text,
                followUpImprovedText = null,
                isUsingImprovedFollowUp = false,
                followUpRecordingState = RecordingState.PREVIEW,
                followUpError = null,
            )
        }
    }

    fun dismissFollowUpDialog() {
        if (_uiState.value.followUpRecordingState == RecordingState.TRANSCRIBING) return
        _uiState.update {
            it.copy(
                showFollowUpDialog = false,
                activeFollowUpId = null,
                showDeleteFollowUpDialog = false,
                followUpDraftText = "",
                followUpImprovedText = null,
                isUsingImprovedFollowUp = false,
                followUpRecordingState = RecordingState.IDLE,
                followUpError = null,
            )
        }
    }

    fun requestFollowUpDeletion() {
        if (_uiState.value.activeFollowUpId == null) return
        _uiState.update { it.copy(showDeleteFollowUpDialog = true) }
    }

    fun showDeleteFollowUpDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteFollowUpDialog = show) }
    }

    fun updateFollowUpDraft(newText: String) {
        _uiState.update { state ->
            if (state.isUsingImprovedFollowUp && state.followUpImprovedText != null) {
                state.copy(followUpImprovedText = newText)
            } else {
                state.copy(followUpDraftText = newText)
            }
        }
    }

    fun setUseImprovedFollowUp(useImproved: Boolean) {
        _uiState.update { it.copy(isUsingImprovedFollowUp = useImproved) }
    }

    fun improveFollowUp() {
        val rawText = _uiState.value.followUpDraftText.trim()
        if (rawText.isBlank()) return

        _uiState.update {
            it.copy(
                showFollowUpDialog = true,
                followUpRecordingState = RecordingState.IMPROVING,
                followUpError = null,
            )
        }

        viewModelScope.launch {
            improveTextUseCase(rawText)
                .onSuccess { improved ->
                    _uiState.update {
                        it.copy(
                            showFollowUpDialog = true,
                            followUpImprovedText = improved,
                            isUsingImprovedFollowUp = true,
                            followUpRecordingState = RecordingState.PREVIEW,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            showFollowUpDialog = true,
                            followUpRecordingState = RecordingState.PREVIEW,
                            followUpError =
                                error.message ?: "Textverbesserung fehlgeschlagen",
                        )
                    }
                }
        }
    }

    fun saveFollowUp() {
        val state = _uiState.value
        val rawText = state.followUpDraftText.trim()
        val improvedText = state.followUpImprovedText?.trim()?.takeIf { it.isNotBlank() }
        val isImproved = state.isUsingImprovedFollowUp && improvedText != null

        if (rawText.isBlank() && improvedText.isNullOrBlank()) return

        viewModelScope.launch {
            val existingFollowUp = currentActiveFollowUp()
            if (existingFollowUp == null) {
                entryFollowUpRepository.saveNewFollowUp(
                    entryId = entryId,
                    rawText = rawText.ifBlank { improvedText.orEmpty() },
                    improvedText = improvedText,
                    isImproved = isImproved,
                )
            } else {
                val display = if (isImproved && improvedText != null) improvedText else rawText
                entryFollowUpRepository.updateFollowUp(
                    existingFollowUp.copy(
                        text = display,
                        rawText = rawText.ifBlank { existingFollowUp.rawText },
                        improvedText = improvedText ?: existingFollowUp.improvedText,
                        isImproved = isImproved,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
            }
            syncLatestFollowUpSummary()
            _uiState.update {
                it.copy(
                    showFollowUpDialog = false,
                    activeFollowUpId = null,
                    showDeleteFollowUpDialog = false,
                    followUpDraftText = "",
                    followUpImprovedText = null,
                    isUsingImprovedFollowUp = false,
                    followUpRecordingState = RecordingState.IDLE,
                    followUpError = null,
                )
            }
            triggerAutoBackup()
            triggerDebouncedAnalysis()
        }
    }

    fun toggleFollowUpRecording() {
        when (_uiState.value.followUpRecordingState) {
            RecordingState.RECORDING -> stopFollowUpRecording()
            RecordingState.IDLE, RecordingState.PREVIEW -> startFollowUpRecording()
            else -> Unit
        }
    }

    private fun startFollowUpRecording() {
        val audioFile = File(context.cacheDir, "follow_up_${System.currentTimeMillis()}.wav")
        currentFollowUpAudioFile = audioFile
        _uiState.update {
            it.copy(
                showFollowUpDialog = true,
                followUpRecordingState = RecordingState.RECORDING,
                followUpImprovedText = null,
                isUsingImprovedFollowUp = false,
                followUpError = null,
            )
        }

        followUpRecordingJob = viewModelScope.launch {
            playStartToneIfEnabled()
            try {
                recordAudioUseCase.startRecording(audioFile)
            } catch (e: Exception) {
                currentFollowUpAudioFile = null
                _uiState.update {
                    it.copy(
                        showFollowUpDialog = true,
                        followUpRecordingState = RecordingState.PREVIEW,
                        followUpError = "Aufnahme fehlgeschlagen: ${e.message}",
                    )
                }
            }
        }
    }

    private fun stopFollowUpRecording() {
        recordAudioUseCase.stopRecording()

        viewModelScope.launch {
            followUpRecordingJob?.join()
            _uiState.update { it.copy(followUpRecordingState = RecordingState.TRANSCRIBING) }

            val audioFile = currentFollowUpAudioFile ?: return@launch
            transcribeAudioUseCase(audioFile)
                .onSuccess { outcome ->
                    currentFollowUpAudioFile = null
                    audioFile.delete()
                    val fallbackNotice = outcome.groqError?.let {
                        "Groq fehlgeschlagen ($it) — lokales Whisper verwendet."
                    }
                    _uiState.update {
                        it.copy(
                            showFollowUpDialog = true,
                            followUpRecordingState = RecordingState.PREVIEW,
                            followUpDraftText = outcome.text.trim(),
                            followUpImprovedText = null,
                            isUsingImprovedFollowUp = false,
                            followUpError = fallbackNotice,
                        )
                    }

                    val autoImprove =
                        encryptedPrefs.getBoolean(Constants.PREF_TEXT_IMPROVEMENT_DEFAULT, false)
                    if (autoImprove) {
                        improveFollowUp()
                    }
                }
                .onFailure { error ->
                    currentFollowUpAudioFile = null
                    audioFile.delete()
                    _uiState.update {
                        it.copy(
                            showFollowUpDialog = true,
                            followUpRecordingState = RecordingState.PREVIEW,
                            followUpError =
                                "Transkription fehlgeschlagen: ${error.message}",
                        )
                    }
                }
        }
    }

    fun clearFollowUpError() {
        _uiState.update { it.copy(followUpError = null) }
    }

    fun deleteActiveFollowUp() {
        val activeFollowUp = currentActiveFollowUp() ?: return
        viewModelScope.launch {
            entryFollowUpRepository.deleteFollowUp(activeFollowUp)
            syncLatestFollowUpSummary()
            _uiState.update {
                it.copy(
                    showFollowUpDialog = false,
                    activeFollowUpId = null,
                    showDeleteFollowUpDialog = false,
                    followUpDraftText = "",
                    followUpImprovedText = null,
                    isUsingImprovedFollowUp = false,
                    followUpRecordingState = RecordingState.IDLE,
                    followUpError = null,
                )
            }
            triggerAutoBackup()
        }
    }

    // ── Inline follow-up editing (no dialog) ──

    private var followUpAutoSaveJobs: MutableMap<Long, Job> = mutableMapOf()

    fun updateInlineFollowUpRaw(followUpId: Long, newText: String) {
        val current =
            _uiState.value.followUps.firstOrNull { it.id == followUpId } ?: return
        val showingImproved = current.isImproved && current.improvedText != null
        val updated =
            current.copy(
                rawText = newText,
                text = if (showingImproved) current.text else newText,
                updatedAt = System.currentTimeMillis(),
            )
        _uiState.update { state ->
            state.copy(followUps = state.followUps.map { if (it.id == followUpId) updated else it })
        }
        scheduleFollowUpAutoSave(followUpId)
    }

    fun updateInlineFollowUpImproved(followUpId: Long, newText: String) {
        val current =
            _uiState.value.followUps.firstOrNull { it.id == followUpId } ?: return
        val showingImproved = current.isImproved && current.improvedText != null
        val updated =
            current.copy(
                improvedText = newText,
                text = if (showingImproved) newText else current.text,
                updatedAt = System.currentTimeMillis(),
            )
        _uiState.update { state ->
            state.copy(followUps = state.followUps.map { if (it.id == followUpId) updated else it })
        }
        scheduleFollowUpAutoSave(followUpId)
    }

    fun toggleInlineFollowUpVersion(followUpId: Long, showImproved: Boolean) {
        val current =
            _uiState.value.followUps.firstOrNull { it.id == followUpId } ?: return
        if (current.improvedText.isNullOrBlank()) return
        val newDisplay = if (showImproved) current.improvedText else current.rawText
        val updated =
            current.copy(
                isImproved = showImproved,
                text = newDisplay,
                updatedAt = System.currentTimeMillis(),
            )
        _uiState.update { state ->
            state.copy(followUps = state.followUps.map { if (it.id == followUpId) updated else it })
        }
        scheduleFollowUpAutoSave(followUpId, delayMs = 0L)
    }

    fun improveInlineFollowUp(followUpId: Long) {
        val current =
            _uiState.value.followUps.firstOrNull { it.id == followUpId } ?: return
        if (current.rawText.isBlank()) return

        viewModelScope.launch {
            improveTextUseCase(current.rawText)
                .onSuccess { improved ->
                    val updated =
                        current.copy(
                            improvedText = improved,
                            isImproved = true,
                            text = improved,
                            updatedAt = System.currentTimeMillis(),
                        )
                    entryFollowUpRepository.updateFollowUp(updated)
                    _uiState.update { state ->
                        state.copy(
                            followUps =
                                state.followUps.map { if (it.id == followUpId) updated else it }
                        )
                    }
                    syncLatestFollowUpSummary()
                    triggerAutoBackup()
                    triggerDebouncedAnalysis()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            followUpError =
                                error.message ?: "Textverbesserung fehlgeschlagen"
                        )
                    }
                }
        }
    }

    fun deleteInlineFollowUp(followUpId: Long) {
        val target =
            _uiState.value.followUps.firstOrNull { it.id == followUpId } ?: return
        followUpAutoSaveJobs.remove(followUpId)?.cancel()
        viewModelScope.launch {
            entryFollowUpRepository.deleteFollowUp(target)
            syncLatestFollowUpSummary()
            triggerAutoBackup()
        }
    }

    private fun scheduleFollowUpAutoSave(followUpId: Long, delayMs: Long = 1500L) {
        followUpAutoSaveJobs[followUpId]?.cancel()
        followUpAutoSaveJobs[followUpId] =
            viewModelScope.launch {
                if (delayMs > 0L) delay(delayMs)
                val target =
                    _uiState.value.followUps.firstOrNull { it.id == followUpId } ?: return@launch
                entryFollowUpRepository.updateFollowUp(target)
                syncLatestFollowUpSummary()
                triggerAutoBackup()
                triggerDebouncedAnalysis()
            }
    }

    private fun currentActiveFollowUp(): EntryFollowUpEntity? {
        val activeId = _uiState.value.activeFollowUpId ?: return null
        return _uiState.value.followUps.firstOrNull { it.id == activeId }
    }

    private suspend fun syncLatestFollowUpSummary() {
        val latestFollowUpText =
            entryFollowUpRepository.getForEntryOnce(entryId).maxByOrNull { it.createdAt }?.text
        journalRepository.updateFollowUpSummary(entryId, latestFollowUpText)
        _uiState.update { state ->
            state.copy(entry = state.entry?.copy(followUpText = latestFollowUpText))
        }
        // Regenerate title + bullet-point summary so it covers main entry + all follow-ups.
        val entry = _uiState.value.entry ?: return
        if (entry.displayText.isNotBlank()) {
            summarizeEntryUseCase(entryId, entry.displayText)
            // Reload the entry so the freshly-written summary/title appear in the UI.
            val refreshed = journalRepository.getEntryById(entryId)
            if (refreshed != null) {
                _uiState.update { it.copy(entry = refreshed) }
            }
        }
    }

    /**
     * Debounced dashboard refresh after a Nachtrag (follow-up) was saved, edited or
     * improved. Mirrors JournalViewModel.triggerDebouncedAnalysis: cancels the
     * previous scheduled run so rapid edits collapse into a single Gemini call 3s
     * after the last save. Respects the user's PREF_AUTO_UPDATE_DASHBOARD switch
     * and sets the runtime flags the DashboardViewModel's polling loop observes.
     */
    private fun triggerDebouncedAnalysis() {
        val autoUpdate = encryptedPrefs.getBoolean(Constants.PREF_AUTO_UPDATE_DASHBOARD, true)
        if (!autoUpdate) return

        followUpAnalysisDebounceJob?.cancel()
        followUpAnalysisDebounceJob = viewModelScope.launch {
            encryptedPrefs
                .edit()
                .putBoolean(Constants.PREF_DASHBOARD_UPDATE_IS_DELETE, false)
                .putBoolean(Constants.PREF_DASHBOARD_UPDATING, true)
                .apply()
            try {
                delay(3_000)
                analyzeEntropyUseCase(freshAnalysis = true)
                val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
                encryptedPrefs
                    .edit()
                    .putLong("dashboard_last_updated_$scenario", System.currentTimeMillis())
                    .apply()
            } finally {
                encryptedPrefs.edit().putBoolean(Constants.PREF_DASHBOARD_UPDATING, false).apply()
            }
        }
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

    private suspend fun playStartToneIfEnabled() {
        val soundsEnabled = encryptedPrefs.getBoolean(Constants.PREF_SOUNDS_ENABLED, true)
        val beepMs = 150
        if (!soundsEnabled) return

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
            delay(beepMs.toLong() + 100)
            track.release()
        } catch (_: Exception) {
            // The confirmation tone is optional.
        }
    }
}
