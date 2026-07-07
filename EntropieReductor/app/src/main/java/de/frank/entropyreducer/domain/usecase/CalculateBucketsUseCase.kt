package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.TimeBucket
import de.frank.entropyreducer.domain.model.priorityBucketForScore
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Bestimmt den passenden Aufgabenbereich. Die frühere Kalenderlogik ist abgelöst:
 * Buckets sind jetzt Prioritätsbereiche und werden ausschließlich aus dem effektiven
 * Prioritätswert bestimmt.
 */
class CalculateBucketsUseCase @Inject constructor() {

    fun bucketFor(
        entry: EntropyEntryEntity,
        calendarByDate: Map<String, CalendarDayEntity>,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
        manualOverride: TimeBucket? = null,
    ): TimeBucket {
        manualOverride?.let { return it }
        return priorityBucketForScore(entry.manualPriorityScore ?: entry.priorityScore)
    }

    /**
     * Ueberprueft, ob der vom KI vergebene Bucket angesichts des aktualisierten Kalenders
     * korrigiert werden sollte (z.B. wenn HEUTE inzwischen Tagdienst ist und der Eintrag
     * 90 Minuten braucht).
     *
     * Liefert null, wenn keine Korrektur noetig ist.
     */
    fun suggestRebucket(
        entry: EntropyEntryEntity,
        calendarByDate: Map<String, CalendarDayEntity>,
        today: LocalDate = LocalDate.now(ZoneId.systemDefault()),
    ): TimeBucket? {
        val computed = bucketFor(entry, calendarByDate, today, manualOverride = null)
        return computed.takeIf { it != entry.timeBucket }
    }

}
