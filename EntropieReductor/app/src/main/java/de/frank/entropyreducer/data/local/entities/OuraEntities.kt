package de.frank.entropyreducer.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Oura Ring Datenmodell (Frank-Wunsch 2026-05-10) — sechs Tabellen analog zur
 * Amazfit-Trennung. Bewusst SEPARATE Tabellen parallel zu biomarker_snapshots
 * (Whoop) und amazfit_daily, damit jede Quelle eigene Werte halten kann ohne
 * sich gegenseitig zu ueberschreiben. Werte die mehrere Geraete liefern (HRV,
 * Ruhepuls, Hauttemperatur) erscheinen also dreifach — mit klarer Quelle.
 *
 * Tagesschluessel `day` ist im Format "YYYY-MM-DD" (lokale Zeit aus Oura's
 * day-Feld) und Primaerschluessel — pro Tag ein Eintrag, beim naechsten Sync
 * wird der Eintrag aktualisiert (REPLACE-onConflict).
 */

/**
 * Tages-Readiness-Score aus /daily_readiness. Hauptwert: `score` (0-100).
 * Wichtige Sonderfelder: `temperatureDeviation` (Abweichung vom 30-Tage-Schnitt
 * der Hauttemperatur in °C, frueher Krankheits-Indikator), plus 8 Contributor-
 * Sub-Scores.
 */
@Entity(tableName = "oura_daily_readiness")
data class OuraReadinessEntity(
    @PrimaryKey val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val temperatureDeviation: Double? = null,
    val temperatureTrendDeviation: Double? = null,
    val activityBalance: Int? = null,
    val bodyTemperature: Int? = null,
    val hrvBalance: Int? = null,
    val previousDayActivity: Int? = null,
    val previousNight: Int? = null,
    val recoveryIndex: Int? = null,
    val restingHeartRate: Int? = null,
    val sleepBalance: Int? = null,
    val createdAt: Long,
)

/** Tages-Sleep-Score aus /daily_sleep mit 7 Contributors. */
@Entity(tableName = "oura_daily_sleep")
data class OuraDailySleepEntity(
    @PrimaryKey val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val deepSleepScore: Int? = null,
    val efficiencyScore: Int? = null,
    val latencyScore: Int? = null,
    val remSleepScore: Int? = null,
    val restfulnessScore: Int? = null,
    val timingScore: Int? = null,
    val totalSleepScore: Int? = null,
    val createdAt: Long,
)

/** Tages-Activity-Score aus /daily_activity. */
@Entity(tableName = "oura_daily_activity")
data class OuraActivityEntity(
    @PrimaryKey val day: String,
    val capturedAt: Long,
    val score: Int? = null,
    val activeCalories: Int? = null,
    val totalCalories: Int? = null,
    val targetCalories: Int? = null,
    val steps: Int? = null,
    val walkingDistanceMeters: Int? = null,
    val highActivitySeconds: Int? = null,
    val mediumActivitySeconds: Int? = null,
    val lowActivitySeconds: Int? = null,
    val nonWearSeconds: Int? = null,
    val restingSeconds: Int? = null,
    val sedentarySeconds: Int? = null,
    val inactivityAlerts: Int? = null,
    val createdAt: Long,
)

/**
 * Tages-Resilience-Score aus /daily_resilience. Oura liefert hier statt
 * Zahl 0-100 ein Level ("limited", "adequate", "solid", "strong",
 * "exceptional") plus drei Sub-Faktoren als Doubles.
 */
@Entity(tableName = "oura_daily_resilience")
data class OuraResilienceEntity(
    @PrimaryKey val day: String,
    val capturedAt: Long,
    val level: String? = null,
    val sleepRecovery: Double? = null,
    val daytimeRecovery: Double? = null,
    val stress: Double? = null,
    val createdAt: Long,
)

/**
 * Detaillierte Schlaf-Session aus /sleep — eine bis mehrere pro Tag (zweite ist
 * meist ein Nap). `id` als Primaerschluessel (Oura-UUID). `day` ist nur Index
 * fuer Tag-Filter. `sleepPhase5Min` ist eine Zeichenkette wie "44443322..."
 * mit einem Zeichen pro 5-Min-Intervall: 1=Tief, 2=Leicht, 3=REM, 4=Wach.
 */
@Entity(
    tableName = "oura_sleep_detail",
    indices = [Index("day")],
)
data class OuraSleepDetailEntity(
    @PrimaryKey val id: String,
    val day: String,
    val capturedAt: Long,
    val bedtimeStart: String? = null,
    val bedtimeEnd: String? = null,
    val type: String? = null,
    val totalSleepSeconds: Int? = null,
    val timeInBedSeconds: Int? = null,
    val awakeSeconds: Int? = null,
    val lightSeconds: Int? = null,
    val deepSeconds: Int? = null,
    val remSeconds: Int? = null,
    val efficiency: Int? = null,
    val latencySeconds: Int? = null,
    val restlessPeriods: Int? = null,
    val averageBreath: Double? = null,
    val averageHeartRate: Double? = null,
    val averageHrv: Int? = null,
    val lowestHeartRate: Int? = null,
    val sleepPhase5Min: String? = null,
    val createdAt: Long,
)

/**
 * Stammdaten aus /personal_info — Singleton-Tabelle mit fester ID = 1L.
 * Wird einmalig nach erfolgreichem Token-Setzen abgefragt und bei
 * Sync-Laeufen aktualisiert.
 */
@Entity(tableName = "oura_personal_info")
data class OuraPersonalInfoEntity(
    @PrimaryKey val id: Long = 1L,
    val ouraUserId: String? = null,
    val age: Int? = null,
    val weightKg: Double? = null,
    val heightMeters: Double? = null,
    val biologicalSex: String? = null,
    val email: String? = null,
    val capturedAt: Long,
)
