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
 * Eintraege (Frank-Spezifikation 2026-05-09, erweitert 2026-05-11, geaendert 2026-05-23):
 *   - HEUTE: wird NICHT mehr automatisch aufgefuellt. Der HEUTE-Bestand ist
 *            eingefroren — was drin ist bleibt drin, neue Aufgaben rutschen NICHT
 *            von selbst nach HEUTE. Die Mitgliedschaft aendert sich nur durch
 *            (a) manuelle Zuordnung, (b) Tag-Rollover (MORGEN->HEUTE) oder
 *            (c) den "Neue Aufgaben hinzufuegen"-Button (refillHeute im ViewModel).
 *   - MORGEN: KI fuellt bis 5, manuell darf ueberschreiten
 *   - FREIBLOCK: KI fuellt bis 10, manuell darf ueberschreiten
 *   - SPAETER: immer unbegrenzt
 *
 * Frank-Wunsch 2026-05-23: "Wenn ich Aufgaben aus dem Heute-Bereich abarbeite,
 * sollen keine neuen automatisch nachgefuellt werden — erst wenn alle erledigt sind
 * kommt ein Button, der 5 neue aus aelteren Bereichen holt."
 *
 * Diese Verteilung ist die EINZIGE autoritative Quelle fuer timeBucket-Werte
 * aktiver Eintraege. Sowohl der TasksViewModel (beim App-Start, nach Aenderungen)
 * als auch der CalendarSyncWorker (nach Kalender-Sync) rufen sie auf — damit
 * kein anderer Code-Pfad die Verteilung ungewollt zerstoeren kann.
 *
 * Algorithmus:
 * 1) Manuelle Eintraege belegen ALLE Slots in ihrem Wunsch-Bucket — ohne Cap
 *    (auch HEUTE, falls Frank manuell zuweist).
 * 1.5) HEUTE-Bestand einfrieren: aktive Eintraege die schon in HEUTE liegen und
 *      nicht manuell woanders platziert sind bleiben in HEUTE.
 * 2) Restliche Slots werden mit dem KI-Pool nach priorityScore desc aufgefuellt —
 *    NUR MORGEN → FREIBLOCK → SPAETER (HEUTE ist KEIN Auto-Fill-Ziel mehr).
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

        // Manuelle Wuensche koennen JEDEN Bucket betreffen (auch HEUTE).
        val manualBuckets = listOf(
            TimeBucket.HEUTE,
            TimeBucket.MORGEN,
            TimeBucket.FREIBLOCK,
            TimeBucket.SPAETER,
        )
        // Auto-Fill betrifft HEUTE NICHT mehr (Frank-Wunsch 2026-05-23).
        val autoFillBuckets = listOf(
            TimeBucket.MORGEN,
            TimeBucket.FREIBLOCK,
            TimeBucket.SPAETER,
        )
        val capacityLeft = mutableMapOf(
            TimeBucket.MORGEN to 5,
            TimeBucket.FREIBLOCK to 10,
            // SPAETER hat kein Limit
        )
        val placement = mutableMapOf<String, TimeBucket>()

        // Pass 1: Manuelle Eintraege belegen ihre Wunsch-Buckets.
        //
        // Frank-Befund 2026-05-11: Manuelle Aufgaben haben HOECHSTE Prioritaet —
        // sie duerfen das Cap ueberschreiten. KI darf nicht "korrigieren".
        // Das Cap wird trotzdem dekrementiert (kann negativ werden) damit
        // Pass 2 keinen weiteren KI-Eintrag in einen schon ueberbelegten
        // Bucket schiebt.
        for (bucket in manualBuckets) {
            val candidates = byPrio.filter { it.manualBucket == bucket }
            candidates.forEach { placement[it.id] = bucket }
            if (bucket != TimeBucket.SPAETER) {
                capacityLeft[bucket]?.let { capacityLeft[bucket] = it - candidates.size }
            }
        }

        // Pass 1.5: HEUTE-Bestand einfrieren (Frank-Wunsch 2026-05-23).
        // Alle aktiven Eintraege die schon in HEUTE liegen und nicht bereits
        // manuell woanders platziert wurden bleiben in HEUTE. So verschwindet
        // keine begonnene HEUTE-Aufgabe und es wird auch keine automatisch
        // nachgefuellt — HEUTE waechst nur ueber refillHeute() oder den Rollover.
        byPrio.filter { it.id !in placement && it.timeBucket == TimeBucket.HEUTE }
            .forEach { placement[it.id] = TimeBucket.HEUTE }

        // Pass 2: AI-Pool + verdraengte Manuelle fuellen freie Slots auf —
        // NUR noch MORGEN/FREIBLOCK/SPAETER, niemals HEUTE.
        val pool = byPrio.filter { it.id !in placement }
        var poolIndex = 0
        for (bucket in autoFillBuckets) {
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
