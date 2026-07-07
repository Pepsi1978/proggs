package de.frank.entropyreducer.data.remote.oura

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Oura Cloud API v2 — BasePath https://api.ouraring.com/v2/usercollection/.
 *
 * Frank-Wunsch 2026-05-10: Anbindung des Oura Rings als dritte Datenquelle neben
 * Whoop und Amazfit T-Rex 3. Auth ueber Personal Access Token im Authorization-
 * Header (Bearer <Token>). Abgefragte Endpunkte (Frank-Auswahl):
 *   1. /daily_readiness   — Readiness-Score + Hauttemperatur-Abweichung + Contributors
 *   2. /daily_sleep       — Sleep-Score + Contributors
 *   3. /daily_activity    — Activity-Score + Schritte + Kalorien
 *   6. /daily_resilience  — Resilienz-Score + Levels
 *   8. /sleep             — Detaillierte Schlaf-Sessions mit Phasen alle 5 Min
 *  18. /personal_info     — Stammdaten (Alter, Groesse, Gewicht, Geschlecht, Email)
 *
 * Pagination: alle Listen-Endpunkte liefern {"data": [...], "next_token": "..."}
 * — wenn next_token != null, gleicher Aufruf mit nextToken-Parameter wiederholen.
 *
 * Rate Limit: 5000 Requests pro 5 Minuten. Wir bleiben klar darunter (max ca.
 * 30 Requests pro Sync-Lauf bei 365 Tagen Historie).
 */
interface OuraApi {

    @GET("v2/usercollection/daily_readiness")
    suspend fun listDailyReadiness(
        @Header("Authorization") authorization: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("next_token") nextToken: String? = null,
    ): OuraPaged<OuraDailyReadiness>

    @GET("v2/usercollection/daily_sleep")
    suspend fun listDailySleep(
        @Header("Authorization") authorization: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("next_token") nextToken: String? = null,
    ): OuraPaged<OuraDailySleep>

    @GET("v2/usercollection/daily_activity")
    suspend fun listDailyActivity(
        @Header("Authorization") authorization: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("next_token") nextToken: String? = null,
    ): OuraPaged<OuraDailyActivity>

    @GET("v2/usercollection/daily_resilience")
    suspend fun listDailyResilience(
        @Header("Authorization") authorization: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("next_token") nextToken: String? = null,
    ): OuraPaged<OuraDailyResilience>

    @GET("v2/usercollection/sleep")
    suspend fun listSleep(
        @Header("Authorization") authorization: String,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("next_token") nextToken: String? = null,
    ): OuraPaged<OuraSleep>

    @GET("v2/usercollection/personal_info")
    suspend fun getPersonalInfo(
        @Header("Authorization") authorization: String,
    ): OuraPersonalInfo
}

/* ---------------------------- Paged Wrapper ---------------------------- */

@Serializable
data class OuraPaged<T>(
    @SerialName("data") val data: List<T> = emptyList(),
    @SerialName("next_token") val nextToken: String? = null,
)

/* ----------------------------- Domain DTOs ----------------------------- */

/**
 * Readiness Score aus /daily_readiness — Oura's Haupt-Erholungswert.
 *
 * `temperature_deviation` ist die Abweichung der Hauttemperatur vom 30-Tage-
 * Schnitt in °C — frueher Krankheits-Indikator. `contributors` sind 8 Sub-Scores
 * (alle 0-100, hoeher = besser) die zusammen den Gesamt-Readiness ergeben.
 */
@Serializable
data class OuraDailyReadiness(
    @SerialName("id") val id: String? = null,
    @SerialName("day") val day: String,
    @SerialName("score") val score: Int? = null,
    @SerialName("temperature_deviation") val temperatureDeviation: Double? = null,
    @SerialName("temperature_trend_deviation") val temperatureTrendDeviation: Double? = null,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("contributors") val contributors: OuraReadinessContributors? = null,
)

@Serializable
data class OuraReadinessContributors(
    @SerialName("activity_balance") val activityBalance: Int? = null,
    @SerialName("body_temperature") val bodyTemperature: Int? = null,
    @SerialName("hrv_balance") val hrvBalance: Int? = null,
    @SerialName("previous_day_activity") val previousDayActivity: Int? = null,
    @SerialName("previous_night") val previousNight: Int? = null,
    @SerialName("recovery_index") val recoveryIndex: Int? = null,
    @SerialName("resting_heart_rate") val restingHeartRate: Int? = null,
    @SerialName("sleep_balance") val sleepBalance: Int? = null,
)

/** Sleep Score aus /daily_sleep — kompakte Tagesbewertung der letzten Nacht. */
@Serializable
data class OuraDailySleep(
    @SerialName("id") val id: String? = null,
    @SerialName("day") val day: String,
    @SerialName("score") val score: Int? = null,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("contributors") val contributors: OuraSleepContributors? = null,
)

@Serializable
data class OuraSleepContributors(
    @SerialName("deep_sleep") val deepSleep: Int? = null,
    @SerialName("efficiency") val efficiency: Int? = null,
    @SerialName("latency") val latency: Int? = null,
    @SerialName("rem_sleep") val remSleep: Int? = null,
    @SerialName("restfulness") val restfulness: Int? = null,
    @SerialName("timing") val timing: Int? = null,
    @SerialName("total_sleep") val totalSleep: Int? = null,
)

/** Activity Score aus /daily_activity — Bewegungs-Tag-Bewertung. */
@Serializable
data class OuraDailyActivity(
    @SerialName("id") val id: String? = null,
    @SerialName("day") val day: String,
    @SerialName("score") val score: Int? = null,
    @SerialName("active_calories") val activeCalories: Int? = null,
    @SerialName("total_calories") val totalCalories: Int? = null,
    @SerialName("target_calories") val targetCalories: Int? = null,
    @SerialName("steps") val steps: Int? = null,
    @SerialName("equivalent_walking_distance") val equivalentWalkingDistance: Int? = null,
    @SerialName("high_activity_time") val highActivityTime: Int? = null,
    @SerialName("medium_activity_time") val mediumActivityTime: Int? = null,
    @SerialName("low_activity_time") val lowActivityTime: Int? = null,
    @SerialName("non_wear_time") val nonWearTime: Int? = null,
    @SerialName("resting_time") val restingTime: Int? = null,
    @SerialName("sedentary_time") val sedentaryTime: Int? = null,
    @SerialName("inactivity_alerts") val inactivityAlerts: Int? = null,
    @SerialName("timestamp") val timestamp: String? = null,
)

/**
 * Resilience Score aus /daily_resilience — Langzeit-Stress-Belastbarkeit.
 * Levels: "limited", "adequate", "solid", "strong", "exceptional".
 */
@Serializable
data class OuraDailyResilience(
    @SerialName("id") val id: String? = null,
    @SerialName("day") val day: String,
    @SerialName("level") val level: String? = null,
    @SerialName("contributors") val contributors: OuraResilienceContributors? = null,
)

@Serializable
data class OuraResilienceContributors(
    @SerialName("sleep_recovery") val sleepRecovery: Double? = null,
    @SerialName("daytime_recovery") val daytimeRecovery: Double? = null,
    @SerialName("stress") val stress: Double? = null,
)

/**
 * Detaillierte Schlaf-Session aus /sleep — eine pro Nacht (manchmal mehrere
 * bei Naps). `sleep_phase_5_min` ist eine Zeichenkette wie "44443322221111…"
 * mit einem Zeichen pro 5-Min-Intervall: 1=Tief, 2=Leicht, 3=REM, 4=Wach.
 */
@Serializable
data class OuraSleep(
    @SerialName("id") val id: String,
    @SerialName("day") val day: String,
    @SerialName("bedtime_start") val bedtimeStart: String? = null,
    @SerialName("bedtime_end") val bedtimeEnd: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("total_sleep_duration") val totalSleepDuration: Int? = null,
    @SerialName("time_in_bed") val timeInBed: Int? = null,
    @SerialName("awake_time") val awakeTime: Int? = null,
    @SerialName("light_sleep_duration") val lightSleepDuration: Int? = null,
    @SerialName("deep_sleep_duration") val deepSleepDuration: Int? = null,
    @SerialName("rem_sleep_duration") val remSleepDuration: Int? = null,
    @SerialName("efficiency") val efficiency: Int? = null,
    @SerialName("latency") val latency: Int? = null,
    @SerialName("restless_periods") val restlessPeriods: Int? = null,
    @SerialName("average_breath") val averageBreath: Double? = null,
    @SerialName("average_heart_rate") val averageHeartRate: Double? = null,
    @SerialName("average_hrv") val averageHrv: Int? = null,
    @SerialName("lowest_heart_rate") val lowestHeartRate: Int? = null,
    @SerialName("sleep_phase_5_min") val sleepPhase5Min: String? = null,
    @SerialName("low_battery_alert") val lowBatteryAlert: Boolean? = null,
)

/** Stammdaten aus /personal_info — einmalige Abfrage. */
@Serializable
data class OuraPersonalInfo(
    @SerialName("id") val id: String? = null,
    @SerialName("age") val age: Int? = null,
    @SerialName("weight") val weight: Double? = null,
    @SerialName("height") val height: Double? = null,
    @SerialName("biological_sex") val biologicalSex: String? = null,
    @SerialName("email") val email: String? = null,
)
