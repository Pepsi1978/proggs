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
import de.frank.entropyreducer.domain.usecase.GenerateRecurringInstancesUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel fuer die Verwaltung wiederkehrender Aufgaben (Sprint 2, Frank-Wunsch 2026-05-22).
 */
@HiltViewModel
class RecurringTemplatesViewModel @Inject constructor(
    private val repo: RecurringTemplateRepository,
    private val entryRepo: EntryRepository,
    private val generator: GenerateRecurringInstancesUseCase,
) : ViewModel() {

    val templates: StateFlow<List<RecurringTemplateEntity>> =
        repo.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Frank-Wunsch 2026-05-22 Phase 2 (Bugfix #948):
     * - Beim Toggle (egal ob aktivieren oder deaktivieren) werden ZUERST alle
     *   noch OFFENEN RECURRING_TEMPLATE-Eintraege fuer diese Vorlage geloescht.
     *   Damit verschwinden Duplikate die durch den frueheren lastGeneratedAt=0
     *   Bug entstanden sind.
     * - Beim AKTIVIEREN wird DIREKT GENAU 1 neue Aufgabe in den Aufgaben-Reiter
     *   geschrieben (deterministische ID basierend auf dem heutigen Tag —
     *   verhindert weitere Duplikate bei mehrfachem Toggeln am gleichen Tag).
     * - Beim DEAKTIVIEREN passiert nur das Cleanup; keine neue Aufgabe.
     *
     * Damit ist die Logik: "Solange aktiv = genau eine offene Instanz in der
     * Liste. Nach Abschluss erscheint die naechste Instanz erst beim naechsten
     * RRULE-Termin (taeglich/woechentlich) via GenerateRecurringInstancesUseCase."
     */
    fun toggleActive(template: RecurringTemplateEntity) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newActive = !template.isActive

            // 1. Cleanup: alle OFFENEN RECURRING_TEMPLATE-Eintraege fuer diese
            // Vorlage entfernen. ID-Praefix "rec-${templateId}-" ist garantiert
            // eindeutig pro Vorlage (siehe GenerateRecurringInstancesUseCase).
            val openEntries = entryRepo.getActive().first()
            val toDelete = openEntries.filter {
                it.source == EntrySource.RECURRING_TEMPLATE &&
                    it.id.startsWith("rec-${template.id}-") &&
                    it.status == EntryStatus.OFFEN
            }
            for (e in toDelete) entryRepo.delete(e)

            // 2. Template-State persistieren.
            repo.upsert(
                template.copy(
                    isActive = newActive,
                    updatedAt = now,
                    // lastGeneratedAt=now verhindert dass der App-Start-Generator
                    // rueckwirkende Eintraege erzeugt.
                    lastGeneratedAt = now,
                )
            )

            // 3. Wenn aktiviert: GENAU 1 Aufgabe fuer heute anlegen.
            if (newActive) {
                entryRepo.upsert(buildEntryForToday(template, now))
            }
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
     * Frank-Wunsch 2026-05-31: manuelle Prioritaet einer wiederkehrenden Aufgabe.
     * Setzt den priorityScore der Vorlage und zieht offene Instanzen sofort mit,
     * damit Farbe/Prio direkt im Aufgaben-Reiter sichtbar werden.
     */
    fun setPriority(template: RecurringTemplateEntity, score: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val clamped = score.coerceIn(0, 100)
            repo.upsert(template.copy(priorityScore = clamped, updatedAt = now))
            entryRepo.getActive().first()
                .filter {
                    it.source == EntrySource.RECURRING_TEMPLATE &&
                        it.id.startsWith("rec-${template.id}-") &&
                        it.status == EntryStatus.OFFEN
                }
                .forEach {
                    // Frank-Wunsch 2026-06-01: auch manualPriorityScore mitziehen, damit die
                    // offene Instanz die im Loop gesetzte Prio behaelt (Vorrang vor KI) und nicht
                    // als "KI" angezeigt oder rescored wird.
                    entryRepo.upsert(
                        it.copy(
                            priorityScore = clamped.toDouble(),
                            manualPriorityScore = clamped.toDouble(),
                            // Frank-Regel 2026-06-19: Setzung im Loop-Template-Screen ist
                            // Template-getrieben -> Instanz-Marker zuruecksetzen, damit die
                            // Loop-Pflege diese Prio weiter durchsetzt (kein "manuell"-Lock).
                            manualPriorityScoreSetAt = null,
                            updatedAt = now,
                        ),
                    )
                }
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

    /**
     * Frank-Wunsch 2026-06-01: festes Wiederkehr-Intervall (alle N Tage). null = "KI entscheidet"
     * (kein Cooldown, taeglich verfuegbar). Bei N >= 2 erscheint die naechste Instanz erst N Tage
     * nach der letzten Erledigung (siehe GenerateRecurringInstancesUseCase). Die rrule wird
     * mitgepflegt, damit die Karte "Alle N Tage" bzw. "Täglich" korrekt anzeigt.
     */
    fun setIntervalDays(template: RecurringTemplateEntity, days: Int?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val newRrule =
                if (days != null && days >= 2) "FREQ=DAILY;INTERVAL=$days" else "FREQ=DAILY"
            repo.upsert(
                template.copy(intervalDays = days, rrule = newRrule, updatedAt = now),
            )
            // Frank-Wunsch 2026-06-01: Nach jeder Intervall-Aenderung SOFORT das gesamte
            // Aufgabenfeld neu bewerten — eine faelschlich offene Instanz im Cooldown
            // (z.B. Kraftsport heute, gestern erledigt, jetzt "alle 5 Tage") verschwindet
            // dadurch sofort, faellige Instanzen werden erzeugt. Nicht erst beim naechsten Start.
            runCatching { generator.cleanupAndEnsureSingle() }
        }
    }

    /**
     * Ziel-Prioritätsbereich einer wiederkehrenden Aufgabe.
     * null = automatisch aus der Vorlagen-Priorität. Offene Instanzen werden sofort
     * in den gewünschten Bereich verschoben.
     */
    fun setTargetBucket(template: RecurringTemplateEntity, bucket: TimeBucket?) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val nextPriority = bucket?.let { defaultPriorityForBucket(it).toInt() } ?: template.priorityScore
            repo.upsert(template.copy(targetBucket = bucket, priorityScore = nextPriority, updatedAt = now))
            val target = bucket ?: priorityBucketForScore(nextPriority.toDouble())
            entryRepo.getActive().first()
                .filter {
                    it.source == EntrySource.RECURRING_TEMPLATE &&
                        it.id.startsWith("rec-${template.id}-") &&
                        it.status == EntryStatus.OFFEN
                }
                .forEach {
                    entryRepo.upsert(
                        it.copy(
                            priorityScore = nextPriority.toDouble(),
                            manualPriorityScore = nextPriority.toDouble(),
                            timeBucket = target,
                            manualBucket = bucket,
                            manualBucketSetAt = if (bucket != null) now else null,
                            updatedAt = now,
                        ),
                    )
                }
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

    /**
     * Frank-Wunsch 2026-05-22 (Bugfix #948): Direkt 1 Aufgabe fuer "heute"
     * erzeugen, deterministische ID basierend auf Tages-Mitternacht. Mehrfaches
     * Aktivieren am gleichen Tag fuehrt zur gleichen ID → kein Duplikat
     * (Room upsert ist idempotent).
     */
    private fun buildEntryForToday(template: RecurringTemplateEntity, nowMs: Long): EntropyEntryEntity {
        val midnight = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val dueMs = Calendar.getInstance().apply {
            timeInMillis = midnight
            set(Calendar.HOUR_OF_DAY, template.timeOfDayMinutes / 60)
            set(Calendar.MINUTE, template.timeOfDayMinutes % 60)
        }.timeInMillis

        val effectivePriority =
            template.targetBucket?.let { defaultPriorityForBucket(it) } ?: template.priorityScore.toDouble()

        return EntropyEntryEntity(
            id = "rec-${template.id}-$midnight",
            rawTranscript = "[Wiederkehrend] ${template.title}",
            title = template.title,
            description = template.description ?: "",
            category = template.category,
            severity = template.severity,
            priorityScore = effectivePriority,
            priorityReason = "Wiederkehrende Aufgabe aus Vorlage \"${template.title}\"",
            status = EntryStatus.OFFEN,
            // Vorgegebener Zielbereich; null = aus der Priorität der Vorlage berechnet.
            timeBucket = template.targetBucket ?: priorityBucketForScore(effectivePriority),
            manualBucket = template.targetBucket,
            manualBucketSetAt = if (template.targetBucket != null) nowMs else null,
            estimatedDurationMinutes = template.estimatedDurationMinutes,
            createdAt = nowMs,
            updatedAt = nowMs,
            resolvedAt = null,
            tags = emptyList(),
            aiNotes = null,
            source = EntrySource.RECURRING_TEMPLATE,
            biomarkerSnapshotId = null,
            durationManuallySet = template.estimatedDurationMinutes != null,
            dueAtMs = dueMs,
        )
    }
}
