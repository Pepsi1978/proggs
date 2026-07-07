package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.priorityBucketForScore
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Autoritative Einsortierung der Aufgaben-Sektionen nach effektiver Priorität.
 * Die alten TimeBucket-Enum-Namen bleiben nur als persistierte Schlüssel bestehen.
 * Fachlich gilt seit 2026-07-07:
 * 80-100 sehr hoch, 60-79 hoch, 40-59 mittel, 20-39 gering, 0-19 später.
 */
@Singleton
class BalanceBucketsUseCase @Inject constructor(
    private val entries: EntryRepository,
) {

    suspend operator fun invoke(): Int {
        val all = entries.getActive().first()
        val active = all.filter {
            it.status == EntryStatus.OFFEN || it.status == EntryStatus.IN_ARBEIT
        }
        val now = System.currentTimeMillis()
        var updated = 0
        active.forEach { e ->
            val target = priorityBucketForScore(e.manualPriorityScore ?: e.priorityScore)
            if (e.timeBucket != target) {
                entries.update(e.copy(timeBucket = target, updatedAt = now))
                updated++
            }
        }
        return updated
    }
}
