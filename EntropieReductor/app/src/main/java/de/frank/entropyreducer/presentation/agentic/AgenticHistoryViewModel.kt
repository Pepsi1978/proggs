package de.frank.entropyreducer.presentation.agentic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.PromptExecutionEntity
import de.frank.entropyreducer.data.local.entities.PromptExecutionStepEntity
import de.frank.entropyreducer.data.repository.PromptExecutionRepository
import de.frank.entropyreducer.domain.model.ExecutionStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel fuer den AgenticHistoryScreen (Frank-Wunsch 2026-05-21).
 *
 * Zeigt entweder ALLE juengsten Ausfuehrungen (initialPromptId = null) oder die
 * Historie EINES bestimmten Prompts (initialPromptId gesetzt). Beim Tap auf
 * einen Eintrag wird `loadStepsFor(executionId)` aufgerufen — die Steps landen
 * in `selectedExecutionSteps` fuer die Detail-Ansicht.
 *
 * Pruning: ein Refresh-Knopf in der UI ruft `pruneOldest(N)` auf — Frank kann
 * alte Eintraege manuell aufraeumen wenn die Tabelle zu gross wird.
 */
@HiltViewModel
class AgenticHistoryViewModel
@Inject
constructor(private val executionRepo: PromptExecutionRepository) : ViewModel() {

    private val _filterPromptId = MutableStateFlow<String?>(null)
    val filterPromptId: StateFlow<String?> = _filterPromptId.asStateFlow()

    /**
     * Status-Filter (Etappe 15): null = alle, sonst nur Eintraege mit diesem Status.
     */
    private val _filterStatus = MutableStateFlow<ExecutionStatus?>(null)
    val filterStatus: StateFlow<ExecutionStatus?> = _filterStatus.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val executions: StateFlow<List<PromptExecutionEntity>> =
        _filterPromptId
            .flatMapLatest { promptId ->
                if (promptId == null) executionRepo.getRecent(limit = 200)
                else executionRepo.getByPrompt(promptId, limit = 200)
            }
            .combine(_filterStatus) { list, status ->
                if (status == null) list else list.filter { it.status == status }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _selectedExecutionId = MutableStateFlow<String?>(null)
    val selectedExecutionId: StateFlow<String?> = _selectedExecutionId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedExecutionSteps: StateFlow<List<PromptExecutionStepEntity>> =
        _selectedExecutionId
            .flatMapLatest { execId ->
                if (execId == null) flowOf(emptyList())
                else executionRepo.getStepsForExecution(execId)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    fun setFilterPromptId(promptId: String?) {
        _filterPromptId.value = promptId
    }

    fun setFilterStatus(status: ExecutionStatus?) {
        _filterStatus.value = status
    }

    fun openDetail(executionId: String) {
        _selectedExecutionId.value = executionId
    }

    fun closeDetail() {
        _selectedExecutionId.value = null
    }

    fun pruneOldest(count: Int) {
        viewModelScope.launch { executionRepo.pruneOldest(count) }
    }
}
