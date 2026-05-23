package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
import de.frank.entropyreducer.data.health.HealthConnectManager
import de.frank.entropyreducer.data.local.dao.HealthConnectValueDao
import de.frank.entropyreducer.data.local.entities.HealthConnectValueEntity
import de.frank.entropyreducer.data.settings.AppSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Repository fuer Health-Connect-Koerperwerte (Frank-Wunsch 2026-05-23) — analog zu
 * [WhoopRepository] / [OuraRepository].
 *
 * Hintergrund: Frueher las das BiomarkerViewModel die HC-Werte direkt aus Health Connect in
 * seinen Anzeige-State. Damit konnte der frische App-Start sie NICHT mitladen (das ViewModel
 * existiert da noch nicht) und der Tab musste bei jedem Oeffnen einen langsamen HC-Live-Read
 * (Binder-IPC) machen.
 *
 * Loesung: Diese Klasse kapselt "HC lesen -> DB-Cache schreiben" ([syncToCache]) und "DB-Cache
 * lesen" ([cachedHistory]). Der frische Start ruft [syncToCache] auf (wie Whoop/Oura), der
 * Biomarker-Tab liest beim Oeffnen nur noch schnell aus der DB.
 *
 * Die DB-Tabelle (hc_value_cache, [HealthConnectValueEntity]) existierte bereits als
 * Cross-Device-Cache; sie wird hier zur primaeren Quelle fuer den Tab-Start.
 */
@Singleton
class HealthConnectRepository
@Inject
constructor(
    private val hc: HealthConnectManager,
    private val dao: HealthConnectValueDao,
    private val settings: AppSettings,
    private val diagnostics: DiagnosticLogger,
) {

    /** True wenn Health Connect verfuegbar ist UND die Gewicht-Leseberechtigung erteilt wurde. */
    suspend fun isReadable(): Boolean =
        runCatching { hc.isAvailable() && hc.hasWeightReadPermission() }.getOrDefault(false)

    /**
     * Liest alle Koerperwert-Verlaeufe aus Health Connect und schreibt sie in den DB-Cache.
     * Returnt die Anzahl gecachter Werte. Aufgerufen beim frischen App-Start (StartupViewModel).
     * Jeder einzelne Read ist defensiv (runCatching) — eine fehlende Einzel-Permission laesst die
     * anderen Werte unberuehrt.
     */
    suspend fun syncToCache(): Int {
        if (!isReadable()) {
            diagnostics.info(DiagnosticArea.HEALTH_CONNECT, "Sync uebersprungen — keine Verbindung/Berechtigung")
            return 0
        }
        val now = System.currentTimeMillis()
        // Frank-Wunsch 2026-05-23 (Schritt 4): inkrementell. Wurde schon einmal gecached, nur
        // die Tage seit dem letzten Sync (+ 7 Tage Ueberlapp) lesen statt fix 730. Das spart
        // beim App-Start viele Binder-IPC-Reads (= schnellerer Start). hc_value_cache nutzt
        // REPLACE -> der Ueberlapp erzeugt keine Duplikate. Erster Sync: volle 730 Tage.
        val lastSyncMs = settings.lastHealthConnectSyncMsFlow.first()
        val days =
            if (lastSyncMs > 0L) {
                val elapsedDays = ((now - lastSyncMs) / (24L * 60L * 60L * 1000L)).toInt()
                (elapsedDays + OVERLAP_DAYS).coerceIn(1, HISTORY_DAYS)
            } else {
                HISTORY_DAYS
            }
        val rows =
            buildList {
                addMetric(METRIC_WEIGHT, readSafe { hc.readWeightHistory(days) }, now)
                addMetric(METRIC_BODY_FAT, readSafe { hc.readBodyFatHistory(days) }, now)
                addMetric(METRIC_LEAN, readSafe { hc.readLeanBodyMassHistory(days) }, now)
                addMetric(METRIC_WATER, readSafe { hc.readBodyWaterMassHistory(days) }, now)
                addMetric(METRIC_BONE, readSafe { hc.readBoneMassHistory(days) }, now)
            }
        if (rows.isNotEmpty()) {
            runCatching { dao.upsertAll(rows) }
                .onFailure {
                    diagnostics.error(
                        DiagnosticArea.HEALTH_CONNECT,
                        "Cache-Schreiben fehlgeschlagen: ${it.message ?: it::class.java.simpleName}",
                        it,
                    )
                }
        }
        settings.setLastHealthConnectSync(now)
        diagnostics.success(DiagnosticArea.HEALTH_CONNECT, "Sync OK — ${rows.size} Werte gecached")
        return rows.size
    }

    /** Liest den gecachten Verlauf einer Metrik aus der DB (timestampMs -> value, aufsteigend). */
    suspend fun cachedHistory(metric: String): List<Pair<Long, Double>> =
        runCatching { dao.getByMetric(metric).map { it.timestampMs to it.value } }
            .getOrDefault(emptyList())

    private suspend fun readSafe(block: suspend () -> List<Pair<Long, Double>>): List<Pair<Long, Double>> =
        runCatching { block() }.getOrDefault(emptyList())

    private fun MutableList<HealthConnectValueEntity>.addMetric(
        metric: String,
        pairs: List<Pair<Long, Double>>,
        createdAt: Long,
    ) {
        pairs.forEach { add(HealthConnectValueEntity(metric, it.first, it.second, createdAt)) }
    }

    companion object {
        // Muessen exakt mit den Metric-Keys in BiomarkerViewModel uebereinstimmen.
        const val METRIC_WEIGHT = "weight"
        const val METRIC_BODY_FAT = "body_fat"
        const val METRIC_LEAN = "lean_body_mass"
        const val METRIC_WATER = "body_water"
        const val METRIC_BONE = "bone_mass"
        private const val HISTORY_DAYS = 730

        /** Sicherheits-Ueberlapp fuer inkrementelle Syncs: 7 Tage vor dem letzten Sync. */
        private const val OVERLAP_DAYS = 7
    }
}
