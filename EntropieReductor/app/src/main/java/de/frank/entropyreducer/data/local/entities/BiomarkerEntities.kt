package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.frank.entropyreducer.domain.model.StackType

@Entity(tableName = "biomarker_snapshots")
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
)

@Entity(tableName = "supplement_logs")
data class SupplementLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val stackType: StackType,
    val notes: String?,
    val skippedItems: List<String>,
)
