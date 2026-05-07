package de.frank.entropyreducer.presentation.dashboard1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.audio.AudioRecorder
import de.frank.entropyreducer.data.audio.RecordingService
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.KiQuestionRepository
import de.frank.entropyreducer.domain.kiquestion.KiQuestion
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.domain.status.StatusObserver
import de.frank.entropyreducer.domain.usecase.CalculateBucketsUseCase
import de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase
import de.frank.entropyreducer.domain.usecase.TranscribeAudioUseCase
import de.frank.entropyreducer.presentation.components.MicState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State der Aufgaben-Ansicht.
 */
data class TasksUiState(
    val entriesByBucket: Map<TimeBucket, List<EntropyEntryEntity>> = emptyMap(),
    val activeCategories: Set<EntropyCategory> = emptySet(),
    val statusPercent: Int = 50,
    val statusBreakdown: StatusBreakdown? = null,
    val openCount: Int = 0,
    val micState: MicState = MicState.IDLE,
    val processingMessage: String? = null,
    val errorMessage: String? = null,
    val recentlyCreatedId: String? = null,
    val kiQuestion: KiQuestion? = null,
    /** Eintrag der aktuell im Detail-Bottom-Sheet angezeigt wird (null = geschlossen). */
    val detailEntry: EntropyEntryEntity? = null,
)

private data class UiOnlyState(
    val micState: MicState = MicState.IDLE,
    val processingMessage: String? = null,
    val errorMessage: String? = null,
    val recentlyCreatedId: String? = null,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    application: Application,
    private val entries: EntryRepository,
    private val recorder: AudioRecorder,
    private val transcribe: TranscribeAudioUseCase,
    private val process: ProcessEntryUseCase,
    private val statusObserver: StatusObserver,
    private val kiQuestions: KiQuestionRepository,
    @Suppress("unused") private val bucketingUseCase: CalculateBucketsUseCase,
) : AndroidViewModel(application) {

    private val activeCategoriesFlow = MutableStateFlow<Set<EntropyCategory>>(emptySet())
    private val uiOnlyFlow = MutableStateFlow(UiOnlyState())
    private val detailEntryIdFlow = MutableStateFlow<String?>(null)

    val state: StateFlow<TasksUiState> = combine(
        entries.getActive(),
        activeCategoriesFlow,
        uiOnlyFlow,
        combine(statusObserver.observe(), kiQuestions.currentQuestion) { b, q -> b to q },
        detailEntryIdFlow,
    ) { list, cats, ui, (breakdown, question), detailId ->
        val filtered = if (cats.isEmpty()) list else list.filter { it.category in cats }
        val grouped = filtered.groupBy { it.timeBucket }
        val openCount = list.count { it.status == EntryStatus.OFFEN }
        val detail = detailId?.let { id -> list.firstOrNull { it.id == id } }
        TasksUiState(
            entriesByBucket = grouped,
            activeCategories = cats,
            statusPercent = breakdown.total,
            statusBreakdown = breakdown,
            openCount = openCount,
            micState = ui.micState,
            processingMessage = ui.processingMessage,
            errorMessage = ui.errorMessage,
            recentlyCreatedId = ui.recentlyCreatedId,
            kiQuestion = question,
            detailEntry = detail,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun openEntryDetail(id: String) { detailEntryIdFlow.value = id }
    fun closeEntryDetail() { detailEntryIdFlow.value = null }

    /** Loescht einen Eintrag (nicht nur archivieren — komplett weg). Aus dem Detail-Sheet. */
    fun deleteEntry(id: String) {
        viewModelScope.launch {
            val entry = entries.get(id) ?: return@launch
            entries.delete(entry)
            detailEntryIdFlow.value = null
        }
    }

    /** Status setzen aus dem Detail-Sheet (4-Buttons: OFFEN / IN_ARBEIT / REDUZIERT / ARCHIVIERT). */
    fun setEntryStatus(id: String, status: EntryStatus) {
        viewModelScope.launch {
            val entry = entries.get(id) ?: return@launch
            val now = System.currentTimeMillis()
            entries.update(
                entry.copy(
                    status = status,
                    resolvedAt = if (status == EntryStatus.REDUZIERT || status == EntryStatus.ARCHIVIERT) now else null,
                    updatedAt = now,
                ),
            )
        }
    }

    fun snoozeKiQuestion() = viewModelScope.launch { kiQuestions.snoozeFor24Hours() }

    /**
     * Markiert einen Eintrag als REDUZIERT (= erledigt) — wird vom Haken-Button
     * auf der Eintrag-Card aufgerufen. resolvedAt wird auf jetzt gesetzt damit
     * die Analyse "heute reduziert" zaehlen kann.
     */
    fun markEntryResolved(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            val now = System.currentTimeMillis()
            entries.update(
                entry.copy(
                    status = EntryStatus.REDUZIERT,
                    resolvedAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    /**
     * Setzt einen erledigten Eintrag wieder auf OFFEN — fuer Undo-Snackbar oder
     * wenn der Benutzer sich vertippt hat.
     */
    fun reopenEntry(entryId: String) {
        viewModelScope.launch {
            val entry = entries.get(entryId) ?: return@launch
            entries.update(
                entry.copy(
                    status = EntryStatus.OFFEN,
                    resolvedAt = null,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun toggleCategory(cat: EntropyCategory) {
        val current = activeCategoriesFlow.value.toMutableSet()
        if (cat in current) current.remove(cat) else current.add(cat)
        activeCategoriesFlow.value = current
    }

    fun clearCategoryFilter() { activeCategoriesFlow.value = emptySet() }

    fun onMicClick() {
        val app = getApplication<Application>()
        when (uiOnlyFlow.value.micState) {
            MicState.IDLE -> {
                runCatching {
                    recorder.start()
                    RecordingService.start(app)
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.RECORDING,
                        processingMessage = "Aufnahme laeuft …",
                    )
                }.onFailure { ex ->
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(
                        micState = MicState.IDLE,
                        errorMessage = ex.message ?: "Aufnahme konnte nicht gestartet werden",
                    )
                }
            }
            MicState.RECORDING -> {
                val file = recorder.stop()
                RecordingService.stop(app)
                if (file == null || !file.exists() || file.length() == 0L) {
                    uiOnlyFlow.value = uiOnlyFlow.value.copy(micState = MicState.IDLE, processingMessage = null)
                    return
                }
                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                    micState = MicState.PROCESSING,
                    processingMessage = "Transkribiere …",
                )
                viewModelScope.launch {
                    transcribe(file).onSuccess { transcript ->
                        if (transcript.isBlank()) {
                            uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                micState = MicState.IDLE, processingMessage = null,
                            )
                            return@onSuccess
                        }
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(processingMessage = "Verarbeite …")
                        process(transcript, EntrySource.NUTZER_MIC)
                            .onSuccess { e ->
                                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                    micState = MicState.IDLE,
                                    processingMessage = null,
                                    recentlyCreatedId = e.id,
                                )
                            }
                            .onFailure { ex ->
                                uiOnlyFlow.value = uiOnlyFlow.value.copy(
                                    micState = MicState.IDLE,
                                    processingMessage = null,
                                    errorMessage = ex.message ?: "KI-Verarbeitung fehlgeschlagen",
                                )
                            }
                    }.onFailure { ex ->
                        uiOnlyFlow.value = uiOnlyFlow.value.copy(
                            micState = MicState.IDLE,
                            processingMessage = null,
                            errorMessage = ex.message ?: "Transkription fehlgeschlagen",
                        )
                    }
                }
            }
            MicState.PROCESSING -> Unit
        }
    }

    fun dismissError() {
        uiOnlyFlow.value = uiOnlyFlow.value.copy(errorMessage = null)
    }

    fun changeStatus(entry: EntropyEntryEntity, newStatus: EntryStatus) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            entries.update(
                entry.copy(
                    status = newStatus,
                    updatedAt = now,
                    resolvedAt = if (newStatus == EntryStatus.REDUZIERT) now else entry.resolvedAt,
                )
            )
        }
    }

    fun delete(entry: EntropyEntryEntity) {
        viewModelScope.launch { entries.delete(entry) }
    }

    override fun onCleared() {
        super.onCleared()
        if (recorder.isRecording()) recorder.discard()
    }
}
