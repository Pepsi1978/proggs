package de.frank.entropyreducer.domain.status

import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.CalendarDayEntity
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.ShiftCode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Berechnet den globalen Status-Balken-Wert (0-100 %).
 * Spec §4.1 — Gewichtung 40/35/25.
 *
 * Komponenten:
 *  1. Biomarker-Score (40 %): aus Recovery, HRV vs. 30-Tage-Median und Sleep-Performance.
 *  2. Aufgaben-Reduktion (35 %): 100 minus gewichtete Severity der offenen Eintraege.
 *  3. Kontext-Stimmigkeit (25 %): Match zwischen aktuellem Schichtcode und passend
 *     reduzierter Entropie heute.
 *
 * Wenn Whoop nicht verbunden ist (keine Snapshots), wird Komponente 1 ausgeblendet
 * und der gesamte Status auf Aufgaben-Reduktion zurueckskaliert.
 */
@Singleton
class CalculateStatusUseCase @Inject constructor() {

    fun calculate(
        entries: List<EntropyEntryEntity>,
        latestSnapshot: BiomarkerSnapshotEntity?,
        recentSnapshots: List<BiomarkerSnapshotEntity>,
        todayCalendar: CalendarDayEntity?,
    ): StatusBreakdown {
        val openEntries = entries.filter { it.status != EntryStatus.ARCHIVIERT }
        val resolvedToday = entries.filter {
            it.status == EntryStatus.REDUZIERT && isToday(it.resolvedAt)
        }

        val tasksScore = computeTasksScore(openEntries, resolvedToday)
        val contextScore = computeContextScore(todayCalendar, openEntries, resolvedToday)
        val biomarkerScore = computeBiomarkerScore(latestSnapshot, recentSnapshots)

        val total = if (biomarkerScore != null) {
            (biomarkerScore * BIOMARKER_WEIGHT +
                tasksScore * TASKS_WEIGHT +
                contextScore * CONTEXT_WEIGHT).roundToInt().coerceIn(0, 100)
        } else {
            // Whoop nicht verbunden — Aufgaben+Kontext re-skaliert auf 100 %
            val partial = tasksScore * 0.6 + contextScore * 0.4
            partial.roundToInt().coerceIn(0, 100)
        }

        return StatusBreakdown(
            total = total,
            biomarkerScore = biomarkerScore?.roundToInt(),
            tasksScore = tasksScore.roundToInt(),
            contextScore = contextScore.roundToInt(),
            openEntryCount = openEntries.count { it.status == EntryStatus.OFFEN },
            resolvedTodayCount = resolvedToday.size,
            shiftCode = todayCalendar?.shiftCode ?: ShiftCode.UNBEKANNT,
            recoveryScore = latestSnapshot?.recoveryScore,
            hrvMs = latestSnapshot?.hrvMs,
            sleepPerformance = latestSnapshot?.sleepPerformance,
        )
    }

    /* ---------------------------- Komponenten ---------------------------- */

    private fun computeTasksScore(
        openEntries: List<EntropyEntryEntity>,
        resolvedToday: List<EntropyEntryEntity>,
    ): Double {
        // Frank-Reklamation 2026-05-09: 'Wenn ich Aufgaben erledige, bleibt der
        // Gesamtwert auf 0%. Reduzierungen muessen sichtbar ins Total einfliessen.'
        // Loesung: (a) Skala vergroessert (100 Punkte Severity = 0% statt 50),
        // sodass viele kleine Aufgaben nicht sofort auf Null clippen,
        // (b) jeder heute reduzierte Eintrag bringt einen DIREKTEN Bonus von
        // +5 pro Severity-Punkt — unabhaengig von der Restlast. Damit sieht
        // Frank bei jeder erledigten Aufgabe wie der Score nach oben springt.
        val openOpen = openEntries.filter { it.status == EntryStatus.OFFEN }
        val inWork = openEntries.filter { it.status == EntryStatus.IN_ARBEIT }
        val sumOpen = openOpen.sumOf { it.severity.toDouble() }
        val sumInWork = inWork.sumOf { it.severity.toDouble() } * 0.5 // halbe Last
        val totalLoad = sumOpen + sumInWork
        val reduceBonus = resolvedToday.sumOf { it.severity.toDouble() } * 5.0
        val baseScore = 100.0 - (totalLoad / 100.0 * 100.0)
        return (baseScore + reduceBonus).coerceIn(0.0, 100.0)
    }

    private fun computeBiomarkerScore(
        latest: BiomarkerSnapshotEntity?,
        history: List<BiomarkerSnapshotEntity>,
    ): Double? {
        if (latest == null) return null
        val recovery = latest.recoveryScore?.toDouble()
        val hrv = latest.hrvMs
        val sleep = latest.sleepPerformance?.toDouble()
        // 30-Tage-Median für HRV-Vergleich.
        val hrvMedian = history.mapNotNull { it.hrvMs }.takeIf { it.isNotEmpty() }
            ?.sorted()?.let { it[it.size / 2] }
        // Median > 0 absichern — sonst Division durch null bei degenerierten Daten.
        val hrvBonus = if (hrv != null && hrvMedian != null && hrvMedian > 0.0) {
            val delta = hrv - hrvMedian
            (delta / hrvMedian * 50.0).coerceIn(-25.0, 25.0)
        } else 0.0

        val parts = listOfNotNull(
            recovery?.let { it * 1.0 },
            sleep?.let { it * 1.0 },
        )
        if (parts.isEmpty()) return null
        val base = parts.average()
        return (base + hrvBonus).coerceIn(0.0, 100.0)
    }

    private fun computeContextScore(
        today: CalendarDayEntity?,
        openEntries: List<EntropyEntryEntity>,
        resolvedToday: List<EntropyEntryEntity>,
    ): Double {
        val shift = today?.shiftCode ?: ShiftCode.UNBEKANNT
        val baseline = when (shift) {
            ShiftCode.FREI -> 60.0
            ShiftCode.URLAUB -> 70.0
            ShiftCode.TAGDIENST -> 50.0
            ShiftCode.NACHTDIENST -> 45.0
            ShiftCode.UNBEKANNT -> 50.0
        }
        // Frank-Wunsch 2026-05-09: 'Aufgaben mit starker Prioritaet beendet werden
        // sollten den Zustand viel staerker verbessern.' Vorher gab jede erledigte
        // Aufgabe flache 8 Punkte (Cap 20) — egal ob banal oder topprior. Jetzt
        // priorityScore-gewichtet:
        //   priorityScore=100  ->  12 Punkte pro Aufgabe (max-Hebel)
        //   priorityScore=50   ->   6 Punkte
        //   priorityScore=20   ->   2.4 Punkte
        // Cap auf 30 (vorher 20) damit drei Top-Prio-Aufgaben den Bonus voll
        // ausschoepfen koennen.
        val resolvedBonus = resolvedToday
            .sumOf { (it.priorityScore / 100.0) * 12.0 }
            .coerceAtMost(30.0)
        // Wenn an Frei-Tag noch viele Umgebungs-Eintraege offen sind, -15 Punkte.
        val unmatchedFrei = if (shift == ShiftCode.FREI) {
            openEntries.count { it.category.name == "UMGEBUNG" } * -3.0
        } else 0.0
        return (baseline + resolvedBonus + unmatchedFrei).coerceIn(0.0, 100.0)
    }

    private fun isToday(timestamp: Long?): Boolean {
        if (timestamp == null) return false
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000
        val midnight = now - now % dayMs
        return timestamp in midnight..(midnight + dayMs)
    }

    companion object {
        const val BIOMARKER_WEIGHT = 0.40
        const val TASKS_WEIGHT = 0.35
        const val CONTEXT_WEIGHT = 0.25
    }
}

/** Aufschluesselung des Status — wird im Detail-Sheet angezeigt (Spec §4.1). */
data class StatusBreakdown(
    val total: Int,
    val biomarkerScore: Int?,
    val tasksScore: Int,
    val contextScore: Int,
    val openEntryCount: Int,
    val resolvedTodayCount: Int,
    val shiftCode: ShiftCode,
    val recoveryScore: Int?,
    val hrvMs: Double?,
    val sleepPerformance: Int?,
)
