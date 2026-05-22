package de.frank.entropyreducer.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.repository.RecurringTemplateRepository
import de.frank.entropyreducer.domain.usecase.GenerateRecurringInstancesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel fuer die Verwaltung wiederkehrender Aufgaben (Sprint 2, Frank-Wunsch 2026-05-22).
 */
@HiltViewModel
class RecurringTemplatesViewModel @Inject constructor(
    private val repo: RecurringTemplateRepository,
    private val generator: GenerateRecurringInstancesUseCase,
) : ViewModel() {

    val templates: StateFlow<List<RecurringTemplateEntity>> =
        repo.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun toggleActive(template: RecurringTemplateEntity) {
        viewModelScope.launch {
            repo.upsert(template.copy(isActive = !template.isActive, updatedAt = System.currentTimeMillis()))
        }
    }

    fun delete(template: RecurringTemplateEntity) {
        viewModelScope.launch { repo.deleteById(template.id) }
    }

    /** Erstellt eine neue Vorlage oder aktualisiert eine bestehende. */
    fun save(template: RecurringTemplateEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val toSave = if (template.id.isBlank()) {
                template.copy(id = UUID.randomUUID().toString(), createdAt = now, updatedAt = now)
            } else {
                template.copy(updatedAt = now)
            }
            repo.upsert(toSave)
        }
    }

    /** Erzeugt sofort die naechste Instanz (Long-Press "Jetzt ausfuehren"). */
    fun runNow() {
        viewModelScope.launch { generator() }
    }
}
