package de.frank.entropyreducer.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.repository.RecurringTemplateRepository
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
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
            val newActive = !template.isActive
            repo.upsert(template.copy(isActive = newActive, updatedAt = System.currentTimeMillis()))
            // Frank-Wunsch 2026-05-22 Phase 2 (Aufgabe 5): wenn die Checkbox
            // aktiviert wird, soll sofort eine Aufgabe in der Liste erscheinen.
            // generator() prueft alle aktiven Vorlagen und legt faellige Instanzen
            // an — durch lastGeneratedAt=0 wird die naechste Occurrence sofort
            // generiert.
            if (newActive) {
                repo.upsert(
                    template.copy(
                        isActive = true,
                        lastGeneratedAt = 0L,
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                generator()
            }
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

    /**
     * Sprint 6 (Frank-Wunsch 2026-05-22 abend, dritte Iteration): Plus-FAB im
     * Loop-Reiter liefert einen fertigen Text (von Mic oder Schreiben). Wir
     * leiten daraus eine Default-Vorlage ab — täglich um 08:00, Titel = erste
     * Zeile (max 80 Zeichen), Beschreibung = Rest. Frank kann sie danach im
     * Editor-Sheet feinjustieren.
     *
     * Bewusst KEINE KI-Bewertung in diesem Pfad — eine wiederkehrende Vorlage
     * soll schnell anlegbar sein, ohne Wartezeit. Wenn KI-Werte gebraucht
     * werden, kann Frank den Convert-Pfad aus einer normalen Aufgabe nutzen
     * (EntryDetailScreen → "Aufgabe zu wiederkehrenden hinzufügen").
     */
    fun createFromText(text: String, source: EntrySource) {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val firstLine = cleaned.lineSequence().firstOrNull().orEmpty()
            val title = if (firstLine.length > 80) firstLine.take(80) else firstLine.ifBlank { cleaned.take(80) }
            val rest = cleaned.removePrefix(firstLine).trim()
            repo.upsert(
                RecurringTemplateEntity(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    description = rest.ifBlank { null },
                    category = EntropyCategory.SONSTIGES,
                    priorityScore = 50,
                    severity = 5,
                    estimatedDurationMinutes = null,
                    rrule = "FREQ=DAILY",
                    timeOfDayMinutes = 480,
                    untilEpochMs = null,
                    nextOccurrenceAt = null,
                    lastGeneratedAt = 0L,
                    occurrenceCount = 0,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }
}
