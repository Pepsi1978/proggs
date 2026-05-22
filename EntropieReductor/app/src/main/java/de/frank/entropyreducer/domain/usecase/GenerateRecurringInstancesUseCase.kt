package de.frank.entropyreducer.domain.usecase

import android.util.Log
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.repository.RecurringTemplateRepository
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.model.TimeBucket
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generiert beim App-Start (oder manuell) fuer alle aktiven RecurringTemplates
 * die seit dem letzten Lauf faelligen Eintraege. Idempotent — mehrfache Aufrufe
 * am gleichen Tag erzeugen keine Duplikate (deterministische ID-Bildung aus
 * templateId + occurrenceMs).
 *
 * Frank-Wunsch 2026-05-22: Wiederkehrende Aufgaben mit voller RFC-5545-Recurrence.
 * Library: org.dmfs:lib-recur.
 */
@Singleton
class GenerateRecurringInstancesUseCase @Inject constructor(
    private val templateRepo: RecurringTemplateRepository,
    private val entryRepo: EntryRepository,
) {
    /**
     * Laeuft alle aktiven Vorlagen durch, erzeugt fuer jedes seit lastGeneratedAt
     * faellige Vorkommen eine EntropyEntryEntity, und aktualisiert die Vorlage.
     *
     * @return Anzahl tatsaechlich erzeugter neuer Eintraege.
     */
    suspend operator fun invoke(): Int {
        val now = System.currentTimeMillis()
        val templates = templateRepo.getActive()
        var totalCreated = 0

        for (template in templates) {
            try {
                val created = processSingleTemplate(template, now)
                totalCreated += created
            } catch (e: Exception) {
                // Eine fehlerhafte Vorlage darf nicht alle anderen blockieren.
                Log.w(
                    TAG,
                    "Vorlage '${template.title}' (${template.id}) uebersprungen: ${e.message}",
                )
            }
        }

        if (totalCreated > 0) {
            Log.i(TAG, "Wiederkehrende Aufgaben: $totalCreated neue Eintraege erzeugt.")
        }
        return totalCreated
    }

    private suspend fun processSingleTemplate(
        template: RecurringTemplateEntity,
        now: Long,
    ): Int {
        val rule = RecurrenceRule(template.rrule)
        // Startpunkt: lastGeneratedAt falls schon mal gelaufen, sonst createdAt.
        val startMs = if (template.lastGeneratedAt > 0L) {
            template.lastGeneratedAt
        } else {
            template.createdAt
        }

        // Iterator ab Start. Falls Vorlage neu (lastGeneratedAt=0), erzeugen wir
        // BEIM ersten Lauf KEINE rueckwirkenden Eintraege — nur ab "ab jetzt".
        // Faengt der Iterator beim Start an, springen wir auf now() vor und
        // erzeugen ab dem naechsten echten Vorkommen.
        val iterStart = DateTime(startMs)
        val iter = rule.iterator(iterStart)

        // Wenn die Vorlage NEU ist (noch nie gelaufen): fastForward bis NOW
        // damit wir keine rueckwirkenden Eintraege erstellen.
        val rewindBoundary = if (template.lastGeneratedAt == 0L) now else startMs + 1
        if (rewindBoundary > startMs) {
            iter.fastForward(DateTime(rewindBoundary))
        }

        var created = 0
        var lastSeenMs = startMs

        // Maximal 50 Eintraege pro Vorlage pro Lauf — Schutz gegen lange Pausen
        // bei taeglichen Vorlagen (z.B. App nach 2 Monaten geoeffnet).
        val limit = 50
        while (iter.hasNext() && created < limit) {
            val occurrenceMs = iter.nextMillis()
            if (occurrenceMs > now) break
            if (template.untilEpochMs != null && occurrenceMs > template.untilEpochMs) break

            val entry = buildEntry(template, occurrenceMs)
            entryRepo.upsert(entry)
            created++
            lastSeenMs = occurrenceMs
        }

        // Naechstes Vorkommen nach now() berechnen (fuer Anzeige in der UI).
        val nextOccurrence: Long? = if (iter.hasNext()) {
            iter.nextMillis().takeIf {
                template.untilEpochMs == null || it <= template.untilEpochMs
            }
        } else {
            null
        }

        templateRepo.upsert(
            template.copy(
                lastGeneratedAt = now,
                nextOccurrenceAt = nextOccurrence,
                occurrenceCount = template.occurrenceCount + created,
                updatedAt = now,
            )
        )
        return created
    }

    private fun buildEntry(
        template: RecurringTemplateEntity,
        occurrenceMs: Long,
    ): EntropyEntryEntity {
        // Deterministische ID: gleicher Tag der gleichen Vorlage erzeugt gleiche ID
        // — verhindert Duplikate bei mehrfachem Aufruf am gleichen Tag.
        val deterministicId = "rec-${template.id}-$occurrenceMs"

        // Faelligkeitszeit aus timeOfDayMinutes ableiten (zur lokalen Mitternacht
        // des Vorkommens-Tages).
        val dueAtMs = computeDueTime(occurrenceMs, template.timeOfDayMinutes)

        return EntropyEntryEntity(
            id = deterministicId,
            rawTranscript = "[Wiederkehrend] ${template.title}",
            title = template.title,
            description = template.description ?: "",
            category = template.category,
            severity = template.severity,
            priorityScore = template.priorityScore.toDouble(),
            priorityReason = "Wiederkehrende Aufgabe aus Vorlage \"${template.title}\"",
            status = EntryStatus.OFFEN,
            timeBucket = TimeBucket.HEUTE,
            estimatedDurationMinutes = template.estimatedDurationMinutes,
            createdAt = occurrenceMs,
            updatedAt = occurrenceMs,
            resolvedAt = null,
            tags = emptyList(),
            aiNotes = null,
            source = EntrySource.RECURRING_TEMPLATE,
            biomarkerSnapshotId = null,
            durationManuallySet = template.estimatedDurationMinutes != null,
            dueAtMs = dueAtMs,
        )
    }

    /** Setzt die Uhrzeit (Stunden+Minuten) auf die lokale Mitternacht des Tages. */
    private fun computeDueTime(occurrenceMs: Long, timeOfDayMinutes: Int): Long {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getDefault())
        cal.timeInMillis = occurrenceMs
        cal.set(java.util.Calendar.HOUR_OF_DAY, timeOfDayMinutes / 60)
        cal.set(java.util.Calendar.MINUTE, timeOfDayMinutes % 60)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    companion object {
        private const val TAG = "RecurringInstances"
    }
}
