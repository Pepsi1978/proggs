package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.InsightEntity
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Berechnet die Confidence eines Insights nach §16.6:
 *
 *   confidence = min(100, 30 + (successCount * 15) + (avgFeltImpact * 2) + biomarkerImpactBonus)
 *
 * - biomarkerImpactBonus: 0 wenn keine Daten, +10 wenn HRV-Verbesserung > 2ms,
 *   +5 wenn Recovery-Verbesserung > 5%
 * - Bei mehrfachen erfolglosen Versuchen (3+ Outcome ERFOLGLOS) sinkt der Wert auf max. 20.
 */
@Singleton
class ConfidenceCalculator @Inject constructor(
    private val biomarkerDao: BiomarkerSnapshotDao,
) {

    suspend fun computeForInsight(
        insight: InsightEntity,
        completedHypotheses: List<HypothesisEntity>,
    ): InsightEntity {
        val successes = completedHypotheses.count { it.outcome == HypothesisOutcome.ERFOLGREICH }
        val partial = completedHypotheses.count { it.outcome == HypothesisOutcome.TEILWEISE_ERFOLGREICH }
        val failures = completedHypotheses.count { it.outcome == HypothesisOutcome.ERFOLGLOS }

        // Erfolgs-Score: ein TEILWEISE zaehlt als 0.5 Erfolg.
        val effectiveSuccess = successes + partial * 0.5

        val felt = completedHypotheses.mapNotNull { it.felltEntropyChange }
        val avgFeltImpact = if (felt.isEmpty()) 0.0 else felt.average()

        val (biomarkerBonus, biomarkerSummary) = computeBiomarkerStats(completedHypotheses)

        val raw = 30.0 + (effectiveSuccess * 15.0) + (avgFeltImpact * 2.0) + biomarkerBonus
        var confidence = raw.coerceIn(0.0, 100.0).toInt()

        if (failures >= 3) confidence = confidence.coerceAtMost(20)

        return insight.copy(
            confidence = confidence,
            successCount = successes,
            attemptCount = completedHypotheses.size,
            avgFeltImpact = if (felt.isEmpty()) null else avgFeltImpact,
            avgBiomarkerImpact = biomarkerSummary,
            updatedAt = System.currentTimeMillis(),
        )
    }

    private suspend fun computeBiomarkerStats(hyps: List<HypothesisEntity>): Pair<Double, String?> {
        data class Delta(val hrv: Double, val recovery: Double)
        val deltas = mutableListOf<Delta>()
        for (h in hyps) {
            val beforeId = h.biomarkerBeforeId ?: continue
            val afterId = h.biomarkerAfterId ?: continue
            val before = biomarkerDao.getById(beforeId) ?: continue
            val after = biomarkerDao.getById(afterId) ?: continue
            val hrvDelta = (after.hrvMs ?: 0.0) - (before.hrvMs ?: 0.0)
            val recDelta = (after.recoveryScore ?: 0).toDouble() - (before.recoveryScore ?: 0).toDouble()
            deltas += Delta(hrvDelta, recDelta)
        }
        if (deltas.isEmpty()) return 0.0 to null

        val avgHrv = deltas.map { it.hrv }.average()
        val avgRec = deltas.map { it.recovery }.average()

        var bonus = 0.0
        if (avgHrv > 2.0) bonus += 10.0
        if (avgRec > 5.0) bonus += 5.0

        val summary = "HRV ${formatSigned(avgHrv)}ms, Recovery ${formatSigned(avgRec)}%"
        return bonus to summary
    }

    private fun formatSigned(v: Double): String {
        val sign = if (v >= 0) "+" else ""
        return "$sign${"%.1f".format(v)}"
    }
}
