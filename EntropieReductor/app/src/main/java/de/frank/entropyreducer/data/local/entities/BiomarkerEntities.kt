package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.StackType

@Entity(
    tableName = "biomarker_snapshots",
    indices = [Index("capturedAt")],
)
data class BiomarkerSnapshotEntity(
    @PrimaryKey val id: String,
    val capturedAt: Long,
    val recoveryScore: Int?,
    val hrvMs: Double?,
    val restingHeartRate: Int?,
    val sleepPerformance: Int?,
    val sleepTotalMinutes: Int?,
    val sleepRemMinutes: Int?,
    val sleepDeepMinutes: Int?,
    val sleepLightMinutes: Int?,
    val sleepAwakeMinutes: Int?,
    val sleepDisturbances: Int?,
    val dayStrain: Double?,
    val dayKilojoules: Double?,
    val createdAt: Long,
    // Frank-Wunsch 2026-05-08: alle Whoop-Werte die die API liefert.
    val respiratoryRate: Double? = null,           // Atemfrequenz (Atemzuege/Minute)
    val sleepConsistencyPercent: Int? = null,      // Schlafregelmaessigkeit %
    val sleepEfficiencyPercent: Int? = null,       // Schlafeffizienz %
    val sleepNeedMinutes: Int? = null,             // Schlafbedarf
    val sleepDebtMinutes: Int? = null,             // Schlafdefizit
    val spo2Percent: Double? = null,               // Sauerstoffsaettigung
    val skinTempCelsius: Double? = null,           // Hauttemperatur
    val averageHeartRate: Int? = null,             // Durchschnittliche Herzfrequenz
    val maxHeartRate: Int? = null,                 // Max Herzfrequenz
    val sleepCycleCount: Int? = null,              // Anzahl der Schlafzyklen pro Nacht
)

/**
 * Ein einzelnes Whoop-Workout. Ein Tag kann mehrere Workouts enthalten —
 * deshalb eigene Tabelle (1:N pro Cycle) statt Felder im BiomarkerSnapshot.
 *
 * `dateKey` ist im Format "YYYY-MM-DD" (lokale Zeitzone) und dient zum schnellen
 * Filtern aller Workouts eines Tages — entspricht dem Cycle-Tag der App.
 *
 * Felder:
 *  - `strain`: Belastungs-Score 0-21 fuer dieses einzelne Training
 *  - `kilojoule`: Energie in Kilojoule (1 kcal = 4.184 kJ)
 *  - `sportId` + `sportName`: Whoop-Sport-ID + lesbarer deutscher Name (z.B. "Krafttraining")
 *  - `zoneZeroMilli`..`zoneFiveMilli`: Aufenthaltsdauer in den 6 Herzfrequenz-Zonen in ms
 */
@Entity(
    tableName = "whoop_workouts",
    indices = [Index("startMs"), Index("dateKey")],
)
data class WhoopWorkoutEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val startMs: Long,
    val endMs: Long,
    val sportId: Int?,
    val sportName: String?,
    val strain: Double?,
    val kilojoule: Double?,
    val averageHeartRate: Int?,
    val maxHeartRate: Int?,
    val percentRecorded: Double?,
    val distanceMeter: Double?,
    val altitudeGainMeter: Double?,
    val zoneZeroMilli: Long?,
    val zoneOneMilli: Long?,
    val zoneTwoMilli: Long?,
    val zoneThreeMilli: Long?,
    val zoneFourMilli: Long?,
    val zoneFiveMilli: Long?,
    val createdAt: Long,
)

@Entity(tableName = "supplement_logs")
data class SupplementLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val stackType: StackType,
    val notes: String?,
    val skippedItems: List<String>,
)

/**
 * Bug-Fix 2026-09-10 (fehlende Whoop-Tage): fuehrt zwei Staende desselben Whoop-Tages
 * zusammen. [fresh] gewinnt, [fallback] fuellt nur Felder, die in [fresh] fehlen. Verhindert,
 * dass ein noch nicht fertig berechneter Whoop-Wert (score_state PENDING_SCORE → score = null)
 * oder ein aelterer Backup-Stand einen vollstaendigen Tag mit Leerwerten ueberschreibt — solche
 * Tage fielen sonst aus HRV-, Ruhepuls- und Erholungs-Charts heraus.
 */
fun mergeWhoopSnapshot(
    fresh: BiomarkerSnapshotEntity,
    fallback: BiomarkerSnapshotEntity,
): BiomarkerSnapshotEntity {
    val sleepFromFallback = fresh.sleepPerformance == null && fallback.sleepPerformance != null
    return fresh.copy(
        // capturedAt haengt am Schlaf-Ende; kommt der Schlaf aus dem Fallback, auch dessen Zeit.
        capturedAt = if (sleepFromFallback) fallback.capturedAt else fresh.capturedAt,
        recoveryScore = fresh.recoveryScore ?: fallback.recoveryScore,
        hrvMs = fresh.hrvMs ?: fallback.hrvMs,
        restingHeartRate = fresh.restingHeartRate ?: fallback.restingHeartRate,
        sleepPerformance = fresh.sleepPerformance ?: fallback.sleepPerformance,
        sleepTotalMinutes = fresh.sleepTotalMinutes ?: fallback.sleepTotalMinutes,
        sleepRemMinutes = fresh.sleepRemMinutes ?: fallback.sleepRemMinutes,
        sleepDeepMinutes = fresh.sleepDeepMinutes ?: fallback.sleepDeepMinutes,
        sleepLightMinutes = fresh.sleepLightMinutes ?: fallback.sleepLightMinutes,
        sleepAwakeMinutes = fresh.sleepAwakeMinutes ?: fallback.sleepAwakeMinutes,
        sleepDisturbances = fresh.sleepDisturbances ?: fallback.sleepDisturbances,
        dayStrain = fresh.dayStrain ?: fallback.dayStrain,
        dayKilojoules = fresh.dayKilojoules ?: fallback.dayKilojoules,
        respiratoryRate = fresh.respiratoryRate ?: fallback.respiratoryRate,
        sleepConsistencyPercent = fresh.sleepConsistencyPercent ?: fallback.sleepConsistencyPercent,
        sleepEfficiencyPercent = fresh.sleepEfficiencyPercent ?: fallback.sleepEfficiencyPercent,
        sleepNeedMinutes = fresh.sleepNeedMinutes ?: fallback.sleepNeedMinutes,
        sleepDebtMinutes = fresh.sleepDebtMinutes ?: fallback.sleepDebtMinutes,
        spo2Percent = fresh.spo2Percent ?: fallback.spo2Percent,
        skinTempCelsius = fresh.skinTempCelsius ?: fallback.skinTempCelsius,
        averageHeartRate = fresh.averageHeartRate ?: fallback.averageHeartRate,
        maxHeartRate = fresh.maxHeartRate ?: fallback.maxHeartRate,
        sleepCycleCount = fresh.sleepCycleCount ?: fallback.sleepCycleCount,
    )
}
