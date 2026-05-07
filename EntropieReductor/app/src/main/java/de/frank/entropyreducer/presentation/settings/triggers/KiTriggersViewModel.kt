package de.frank.entropyreducer.presentation.settings.triggers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.KiTriggerEntity
import de.frank.entropyreducer.data.repository.KiTriggerRepository
import de.frank.entropyreducer.workers.BackgroundScheduler
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class KiTriggersUiState(
    val pending: List<KiTriggerEntity> = emptyList(),
    val active: List<KiTriggerEntity> = emptyList(),
)

/**
 * Verwaltung aller KI-vorgeschlagenen Trigger (Spec §16.2).
 *  - `pending`: noch nicht entschieden — Annehmen oder Ablehnen.
 *  - `active`: vom Benutzer angenommen — werden vom TriggerPollingWorker
 *    alle 15 Min ausgewertet.
 */
@HiltViewModel
class KiTriggersViewModel @Inject constructor(
    private val repo: KiTriggerRepository,
    private val scheduler: BackgroundScheduler,
) : ViewModel() {

    val state: StateFlow<KiTriggersUiState> = combine(
        repo.observePendingApproval(),
        repo.observeActive(),
    ) { pending, active ->
        KiTriggersUiState(pending = pending, active = active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KiTriggersUiState())

    fun approve(trigger: KiTriggerEntity) = viewModelScope.launch { repo.approve(trigger) }
    fun reject(trigger: KiTriggerEntity) = viewModelScope.launch { repo.reject(trigger) }
    fun deactivate(trigger: KiTriggerEntity) =
        viewModelScope.launch { repo.update(trigger.copy(isActive = false)) }

    fun delete(trigger: KiTriggerEntity) = viewModelScope.launch { repo.deleteById(trigger.id) }

    /**
     * Manueller "Trigger jetzt erzeugen"-Button — startet einen OneTime-Worker
     * mit `force=true`, der Mi/So-Wochentagscheck wird uebersprungen.
     */
    fun generateNow() = scheduler.runKiTriggerNow()
}
