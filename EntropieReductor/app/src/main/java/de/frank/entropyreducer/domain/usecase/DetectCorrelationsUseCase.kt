package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.SupplementLogDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.SupplementLogEntity
import de.frank.entropyreducer.domain.model.StackType
import javax.inject.Inject
import kotlin.math.sqrt
import kotlinx.coroutines.flow.first

/**
 * Korrelations-Engine (Spec §16.1) — Supplements ↔ Biomarker.
 *
 * Algorithmus (60-Tage-Fenster):
 *  1. Hole alle SupplementLogs + BiomarkerSnapshots.
 *  2. Pro StackType: bilde 2 Gruppen — Tage MIT vs. Tage OHNE diesen Stack.
 *     Ein Snapshot zaehlt zu "MIT" wenn am gleichen Tag oder am Tag davor
 *     ein Log mit diesem StackType eingetragen wurde.
 *  3. Pro Metrik (HRV, Recovery, Sleep) berechne Mittelwert + Stdabw beider
 *     Gruppen. Cohen's d = (m1 - m2) / pooled_sd.
 *  4. Wenn |d| > 0.3 UND n >= 7 in beiden Gruppen: Beobachtung melden.
 *
 * Strikte Sprachregelung: "Beobachtung, keine Kausalitaet" — Result-Eintraege
 * tragen das immer mit, damit das Genie es später nicht in Kausal-Aussagen
 * verwandelt.
 */
class DetectCorrelationsUseCase @Inject constructor(
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val supplementLogDao: SupplementLogDao,
) {

    data class Observation(
        val stackType: StackType,
        val metric: Metric,
        val effectSize: Double,
        val nWith: Int,
        val nWithout: Int,
        val meanWith: Double,
        val meanWithout: Double,
    )

    enum class Metric { HRV, RECOVERY, SLEEP_PERFORMANCE }

    suspend operator fun invoke(): List<Observation> {
        val now = System.currentTimeMillis()
        val sixtyDaysAgo = now - 60L * 24 * 60 * 60 * 1000

        val biomarkers = biomarkerDao.getRange(sixtyDaysAgo, now).first()
        val logs = supplementLogDao.getRange(sixtyDaysAgo, now).first()
        if (biomarkers.size < 14 || logs.isEmpty()) return emptyList()

        val observations = mutableListOf<Observation>()
        StackType.entries.forEach { stack ->
            val withStack = biomarkers.filter { snap -> hasStackOnOrBefore(snap, stack, logs) }
            val withoutStack = biomarkers - withStack.toSet()
            if (withStack.size < 7 || withoutStack.size < 7) return@forEach

            Metric.entries.forEach { metric ->
                val effect = cohensD(withStack, withoutStack, metric)
                if (effect != null && Math.abs(effect.d) > 0.3) {
                    observations += Observation(
                        stackType = stack,
                        metric = metric,
                        effectSize = effect.d,
                        nWith = withStack.size,
                        nWithout = withoutStack.size,
                        meanWith = effect.m1,
                        meanWithout = effect.m2,
                    )
                }
            }
        }
        return observations
    }

    private fun hasStackOnOrBefore(
        snap: BiomarkerSnapshotEntity,
        stack: StackType,
        logs: List<SupplementLogEntity>,
    ): Boolean {
        val windowStart = snap.capturedAt - 36 * 60 * 60 * 1000L
        return logs.any { it.stackType == stack && it.timestamp in windowStart..snap.capturedAt }
    }

    private data class Effect(val d: Double, val m1: Double, val m2: Double)

    private fun cohensD(
        a: List<BiomarkerSnapshotEntity>,
        b: List<BiomarkerSnapshotEntity>,
        metric: Metric,
    ): Effect? {
        val va = a.mapNotNull { extract(it, metric) }
        val vb = b.mapNotNull { extract(it, metric) }
        if (va.size < 7 || vb.size < 7) return null
        val m1 = va.average()
        val m2 = vb.average()
        val s1 = stdev(va, m1)
        val s2 = stdev(vb, m2)
        val n1 = va.size
        val n2 = vb.size
        val pooled = sqrt(((n1 - 1) * s1 * s1 + (n2 - 1) * s2 * s2) / (n1 + n2 - 2.0))
        if (pooled == 0.0) return null
        return Effect(d = (m1 - m2) / pooled, m1 = m1, m2 = m2)
    }

    private fun stdev(values: List<Double>, mean: Double): Double {
        if (values.size < 2) return 0.0
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / (values.size - 1.0))
    }

    private fun extract(s: BiomarkerSnapshotEntity, metric: Metric): Double? = when (metric) {
        Metric.HRV -> s.hrvMs
        Metric.RECOVERY -> s.recoveryScore?.toDouble()
        Metric.SLEEP_PERFORMANCE -> s.sleepPerformance?.toDouble()
    }
}
