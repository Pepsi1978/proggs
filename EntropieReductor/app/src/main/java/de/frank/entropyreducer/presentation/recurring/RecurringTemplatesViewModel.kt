package de.frank.entropyreducer.presentation.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.RecurringTemplateRepository
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.model.defaultPriorityForBucket
import de.frank.entropyreducer.domain.model.priorityBucketForScore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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
    private val entryRepo: EntryRepository,
) : ViewModel() {

    val templates: StateFlow<List<RecurringTemplateEntity>> =
        repo.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Loop-Vorlagen werden nur noch manuell per Hinzufügen in Aufgaben kopiert. */
    fun addToTasks(template: RecurringTemplateEntity, bucket: TimeBucket?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            entryRepo.upsert(buildManualEntry(template, bucket, now))
        }
    }

    fun delete(template: RecurringTemplateEntity) {
        viewModelScope.launch {
            // Auch beim Loeschen alle offenen Instanzen aufraeumen.
            val openEntries = entryRepo.getActive().first()
            val toDelete = openEntries.filter {
                it.source == EntrySource.RECURRING_TEMPLATE &&
                    it.id.startsWith("rec-${template.id}-") &&
                    it.status == EntryStatus.OFFEN
            }
            for (e in toDelete) entryRepo.delete(e)
            repo.deleteById(template.id)
        }
    }

    /**
     * Frank-Wunsch 2026-06-01: Titel einer wiederkehrenden Aufgabe bearbeiten.
     * Setzt den Titel der Vorlage und zieht offene Instanzen sofort mit, damit der
     * neue Titel auch im Aufgaben-Reiter sichtbar wird. Leere Eingaben werden ignoriert.
     */
    fun setTitle(template: RecurringTemplateEntity, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isBlank() || clean == template.title) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.upsert(template.copy(title = clean, updatedAt = now))
            entryRepo.getActive().first()
                .filter {
                    it.source == EntrySource.RECURRING_TEMPLATE &&
                        it.id.startsWith("rec-${template.id}-") &&
                        it.status == EntryStatus.OFFEN
                }
                .forEach {
                    entryRepo.upsert(it.copy(title = clean, updatedAt = now))
                }
        }
    }

    /**
     * Frank-Wunsch 2026-06-01: Beschreibung einer wiederkehrenden Aufgabe im Loop-Detail
     * bearbeiten. Leerer Text = keine Beschreibung (null). Offene Instanzen werden mitgezogen,
     * damit die Aenderung auch im Aufgaben-Reiter sichtbar wird.
     */
    fun setDescription(template: RecurringTemplateEntity, newDescription: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val clean = newDescription.trim().ifBlank { null }
            repo.upsert(template.copy(description = clean, updatedAt = now))
            entryRepo.getActive().first()
                .filter {
                    it.source == EntrySource.RECURRING_TEMPLATE &&
                        it.id.startsWith("rec-${template.id}-") &&
                        it.status == EntryStatus.OFFEN
                }
                .forEach { entryRepo.upsert(it.copy(description = clean ?: "", updatedAt = now)) }
        }
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
                    isActive = false,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    /**
     * Manuelles Kopieren einer Loop-Vorlage in die Aufgabenliste. Jede Button-Bestätigung erzeugt
     * genau eine neue Aufgabe; es gibt keinen Automatismus und keinen Aktiv-/Pausiert-Zustand mehr.
     */
    private fun buildManualEntry(
        template: RecurringTemplateEntity,
        bucket: TimeBucket?,
        nowMs: Long,
    ): EntropyEntryEntity {
        val effectivePriority =
            bucket?.let { defaultPriorityForBucket(it) } ?: template.priorityScore.toDouble()

        return EntropyEntryEntity(
            id = "rec-manual-${template.id}-$nowMs",
            rawTranscript = "[Wiederkehrend] ${template.title}",
            title = template.title,
            description = template.description ?: "",
            category = template.category,
            severity = template.severity,
            priorityScore = effectivePriority,
            priorityReason = "Wiederkehrende Aufgabe aus Vorlage \"${template.title}\"",
            status = EntryStatus.OFFEN,
            // Vorgegebener Zielbereich; null = aus der Priorität der Vorlage berechnet.
            timeBucket = bucket ?: priorityBucketForScore(effectivePriority),
            manualBucket = bucket,
            manualBucketSetAt = if (bucket != null) nowMs else null,
            estimatedDurationMinutes = template.estimatedDurationMinutes,
            createdAt = nowMs,
            updatedAt = nowMs,
            resolvedAt = null,
            tags = emptyList(),
            aiNotes = null,
            source = EntrySource.RECURRING_TEMPLATE,
            biomarkerSnapshotId = null,
            durationManuallySet = template.estimatedDurationMinutes != null,
            dueAtMs = null,
        )
    }
}
