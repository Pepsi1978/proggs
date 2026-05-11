package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bucket-Verteilung mit Soft-Limits fuer KI-Einordnung, harte Bevorzugung manueller
 * Eintraege (Frank-Spezifikation 2026-05-09, erweitert 2026-05-11):
 *   - HEUTE: KI fuellt bis 5 Eintraege, manuell darf ueberschreiten (5/6/7/...)
 *   - MORGEN: KI fuellt bis 5, manuell darf ueberschreiten
 *   - FREIBLOCK: KI fuellt bis 10, manuell darf ueberschreiten
 *   - SPAETER: immer unbegrenzt
 *
 * Diese Verteilung ist die EINZIGE autoritative Quelle fuer timeBucket-Werte
 * aktiver Eintraege. Sowohl der TasksViewModel (beim App-Start, nach Aenderungen)
 * als auch der CalendarSyncWorker (nach Kalender-Sync) rufen sie auf — damit
 * kein anderer Code-Pfad die Verteilung ungewollt zerstoeren kann.
 *
 * Algorithmus:
 * 1) Manuelle Eintraege belegen ALLE Slots in ihrem Wunsch-Bucket — ohne Cap.
 *    Das Cap wird trotzdem dekrementiert (kann negativ werden) damit Pass 2
 *    keinen KI-Pool-Eintrag mehr in den ueberbelegten Bucket schiebt.
 * 2) Restliche Slots werden mit dem KI-Pool nach priorityScore desc aufgefuellt —
 *    HEUTE → MORGEN → FREIBLOCK → SPAETER. Bei negativem Rest-Cap landet der
 *    Pool weiter unten.
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

        // Pass 1: Manuelle Eintraege belegen ihre Wunsch-Buckets.
        //
        // Frank-Befund 2026-05-11: Manuelle Aufgaben haben HOECHSTE Prioritaet —
        // sie duerfen das Cap ueberschreiten. Wenn Frank manuell 7 Aufgaben in
        // HEUTE schiebt, bleiben es 7 (vorher: 5, die anderen 2 wurden in Pass 2
        // automatisch in MORGEN weitergereicht). KI darf nicht "korrigieren".
        // Das Cap wird trotzdem dekrementiert (kann negativ werden) damit
        // Pass 2 keinen weiteren KI-Eintrag in einen schon ueberbelegten
        // Bucket schiebt.
        for (bucket in orderedBuckets) {
            val candidates = byPrio.filter { it.manualBucket == bucket }
            if (bucket == TimeBucket.SPAETER) {
                // Unbegrenzte Kapazitaet — alle manuellen SPAETER-Wuensche akzeptieren.
                candidates.forEach { placement[it.id] = bucket }
            } else {
                // KEIN take(cap) mehr — alle manuellen Kandidaten kommen rein.
                candidates.forEach { placement[it.id] = bucket }
                val cap = capacityLeft[bucket] ?: 0
                capacityLeft[bucket] = cap - candidates.size
            }
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
