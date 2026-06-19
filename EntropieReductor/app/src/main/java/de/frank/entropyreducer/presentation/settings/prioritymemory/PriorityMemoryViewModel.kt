package de.frank.entropyreducer.presentation.settings.prioritymemory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import de.frank.entropyreducer.data.repository.PriorityMemoryRepository
import de.frank.entropyreducer.data.settings.AppSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel der Prioritaets-Gedaechtnis-Liste (Frank-Wunsch 2026-06-19).
 * Exponiert Eintraege, Gesamtzahl (fuer die Limit-Warnung) sowie An/Aus + Limit als StateFlows.
 */
@HiltViewModel
class PriorityMemoryListViewModel
@Inject
constructor(
    private val repo: PriorityMemoryRepository,
    private val settings: AppSettings,
) : ViewModel() {

    val memories: StateFlow<List<PriorityMemoryEntity>> =
        repo.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val count: StateFlow<Int> =
        repo.observeCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val enabled: StateFlow<Boolean> =
        settings.priorityMemoryEnabledFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val limit: StateFlow<Int> =
        settings.priorityMemoryLimitFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 300)

    fun setEnabled(value: Boolean) = viewModelScope.launch { settings.setPriorityMemoryEnabled(value) }

    fun setLimit(value: Int) = viewModelScope.launch { settings.setPriorityMemoryLimit(value) }

    fun setPriority(id: String, prio: Double) =
        viewModelScope.launch { repo.updatePriority(id, prio, System.currentTimeMillis()) }

    fun delete(id: String) = viewModelScope.launch { repo.delete(id) }
}

/**
 * ViewModel des Detail-Editors (Frank-Wunsch 2026-06-19). Laedt einen Eintrag per ID
 * (SavedStateHandle, ueberlebt Prozesstod) und erlaubt Titel/Beschreibung/Prio editieren + loeschen.
 */
@HiltViewModel
class PriorityMemoryDetailViewModel
@Inject
constructor(
    private val repo: PriorityMemoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val id: String = savedStateHandle.get<String>("memoryId").orEmpty()

    private val _state = MutableStateFlow<PriorityMemoryEntity?>(null)
    val state: StateFlow<PriorityMemoryEntity?> = _state

    init {
        viewModelScope.launch { _state.value = repo.getById(id) }
    }

    fun save(title: String, description: String, priority: Double, onDone: () -> Unit) =
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.updateContent(id, title, description, now)
            repo.updatePriority(id, priority, now)
            onDone()
        }

    fun delete(onDone: () -> Unit) =
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
}
