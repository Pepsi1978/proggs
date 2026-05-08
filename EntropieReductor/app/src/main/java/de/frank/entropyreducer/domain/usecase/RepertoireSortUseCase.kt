package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.entities.InsightEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Sortiert Insights für „Mein Repertoire" (Spec §14.1) nach:
 *
 *   confidence × Anzahl Wiederholungen × durchschnittliche reduzierte Severity
 *
 * Wir haben keine direkte „reduzierte Severity" — als Naeherung verwenden wir die
 * gefuehlte Wirkung (avgFeltImpact, -10..+10), normalisiert auf [0..1] mit Offset.
 */
@Singleton
class RepertoireSortUseCase @Inject constructor() {

    operator fun invoke(insights: List<InsightEntity>, minConfidence: Int = 50): List<InsightEntity> {
        return insights
            .filter { it.confidence >= minConfidence }
            .map { it to score(it) }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private fun score(i: InsightEntity): Double {
        val confidence = i.confidence.toDouble()
        val repetitions = max(1, i.successCount).toDouble()
        // Normalisiere felt auf [0..1] (felt = -10..+10 → (felt+10)/20).
        val feltNormalized = i.avgFeltImpact?.let { ((it + 10.0) / 20.0).coerceIn(0.0, 1.0) }
            ?: 0.5
        return confidence * repetitions * (0.5 + feltNormalized) // 0.5 = Mindestgewicht
    }
}
