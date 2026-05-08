package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strikte Bucket-Verteilung mit harten Limits (Frank-Spezifikation 2026-05-09):
 *   - HEUTE: max 5 Eintraege
 *   - MORGEN: max 5 Eintraege
 *   - FREIBLOCK: max 10 Eintraege
 *   - SPAETER: unbegrenzt
 *
 * Diese Verteilung ist die EINZIGE autoritative Quelle fuer timeBucket-Werte
 * aktiver Eintraege. Sowohl der TasksViewModel (beim App-Start, nach Aenderungen)
 * als auch der CalendarSyncWorker (nach Kalender-Sync) rufen sie auf — damit
 * kein anderer Code-Pfad die Verteilung ungewollt zerstoeren kann.
 *
 * Algorithmus:
 * 1) Manuelle Eintraege belegen Slots in ihrem Wunsch-Bucket zuerst, sortiert
 *    nach priorityScore desc. Limit-Ueberschuss faellt durch.
 * 2) Restliche Slots werden mit dem Pool (KI-Eintraege + verdraengte Manuelle)
 *    nach priorityScore desc aufgefuellt — HEUTE → MORGEN → FREIBLOCK → SPAETER.
 *
 * Idempotent: schreibt nur wenn sich timeBucket aendert.
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
        val byPrio = active.sortedByDescending { it.priorityScore }

        val orderedBuckets = listOf(
            TimeBucket.HEUTE,
            TimeBucket.MORGEN,
            TimeBucket.FREIBLOCK,
            TimeBucket.SPAETER,
        )
        val capacityLeft = mutableMapOf(
            TimeBucket.HEUTE to 5,
            TimeBucket.MORGEN to 5,
            TimeBucket.FREIBLOCK to 10,
            // SPAETER hat kein Limit
        )
        val placement = mutableMapOf<String, TimeBucket>()

        // Pass 1: Manuelle Eintraege belegen ihre Wunsch-Buckets (mit Limit)
        for (bucket in orderedBuckets) {
            if (bucket == TimeBucket.SPAETER) continue
            val cap = capacityLeft[bucket] ?: 0
            val candidates = byPrio.filter { it.manualBucket == bucket }
            val placed = candidates.take(cap)
            placed.forEach { placement[it.id] = bucket }
            capacityLeft[bucket] = cap - placed.size
        }

        // Pass 2: AI-Pool + verdraengte Manuelle fuellen freie Slots auf
        val pool = byPrio.filter { it.id !in placement }
        var poolIndex = 0
        for (bucket in orderedBuckets) {
            if (bucket == TimeBucket.SPAETER) {
                while (poolIndex < pool.size) {
                    placement[pool[poolIndex].id] = TimeBucket.SPAETER
                    poolIndex++
                }
                break
            }
            val cap = capacityLeft[bucket] ?: 0
            val toFill = minOf(cap, pool.size - poolIndex)
            for (i in 0 until toFill) {
                placement[pool[poolIndex].id] = bucket
                poolIndex++
            }
        }

        // Schreibe Aenderungen — idempotent (nur wenn sich timeBucket aendert)
        val now = System.currentTimeMillis()
        var updated = 0
        active.forEach { e ->
            val target = placement[e.id] ?: TimeBucket.SPAETER
            if (e.timeBucket != target) {
                entries.update(e.copy(timeBucket = target, updatedAt = now))
                updated++
            }
        }
        return updated
    }
}
