package de.frank.entropyreducer.data.remote.whoop

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Whoop Developer API (v1, BasePath https://api.prod.whoop.com/developer/).
 * Spec §15.4. Wir nutzen die paginierte Ueberblick-Form: pro Endpoint
 * erhalten wir eine Liste plus optionalen `next_token`.
 */
interface WhoopApi {

    // Whoop API v2 — v1 wurde 2025/2026 abgeschaltet (Quelle: developer.whoop.com/api).

    @GET("v2/recovery")
    suspend fun listRecovery(
        @Header("Authorization") authorization: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int = 25,
        @Query("nextToken") nextToken: String? = null,
    ): WhoopPagedRecovery

    @GET("v2/cycle")
    suspend fun listCycles(
        @Header("Authorization") authorization: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int = 25,
        @Query("nextToken") nextToken: String? = null,
    ): WhoopPagedCycle

    @GET("v2/activity/sleep")
    suspend fun listSleep(
        @Header("Authorization") authorization: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int = 25,
        @Query("nextToken") nextToken: String? = null,
    ): WhoopPagedSleep

    @GET("v2/activity/workout")
    suspend fun listWorkouts(
        @Header("Authorization") authorization: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("limit") limit: Int = 25,
        @Query("nextToken") nextToken: String? = null,
    ): WhoopPagedWorkout

    @GET("v2/user/profile/basic")
    suspend fun getProfile(
        @Header("Authorization") authorization: String,
    ): WhoopProfile
}

/* ---------------------------- Paged Wrappers ---------------------------- */

@Serializable
data class WhoopPagedRecovery(
    @SerialName("records") val records: List<WhoopRecovery> = emptyList(),
    @SerialName("next_token") val nextToken: String? = null,
)

@Serializable
data class WhoopPagedCycle(
    @SerialName("records") val records: List<WhoopCycle> = emptyList(),
    @SerialName("next_token") val nextToken: String? = null,
)

@Serializable
data class WhoopPagedSleep(
    @SerialName("records") val records: List<WhoopSleep> = emptyList(),
    @SerialName("next_token") val nextToken: String? = null,
)

@Serializable
data class WhoopPagedWorkout(
    @SerialName("records") val records: List<WhoopWorkout> = emptyList(),
    @SerialName("next_token") val nextToken: String? = null,
)

/* ----------------------------- Domain DTOs ----------------------------- */

@Serializable
data class WhoopRecovery(
    @SerialName("cycle_id") val cycleId: Long? = null,
    @SerialName("sleep_id") val sleepId: String? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("score_state") val scoreState: String? = null,
    @SerialName("score") val score: WhoopRecoveryScore? = null,
)

@Serializable
data class WhoopRecoveryScore(
    @SerialName("user_calibrating") val userCalibrating: Boolean? = null,
    @SerialName("recovery_score") val recoveryScore: Double? = null,
    @SerialName("resting_heart_rate") val restingHeartRate: Double? = null,
    @SerialName("hrv_rmssd_milli") val hrvRmssdMilli: Double? = null,
    @SerialName("spo2_percentage") val spo2Percentage: Double? = null,
    @SerialName("skin_temp_celsius") val skinTempCelsius: Double? = null,
)

@Serializable
data class WhoopCycle(
    @SerialName("id") val id: Long? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("start") val start: String? = null,
    @SerialName("end") val end: String? = null,
    @SerialName("timezone_offset") val timezoneOffset: String? = null,
    @SerialName("score_state") val scoreState: String? = null,
    @SerialName("score") val score: WhoopCycleScore? = null,
)

@Serializable
data class WhoopCycleScore(
    @SerialName("strain") val strain: Double? = null,
    @SerialName("kilojoule") val kilojoule: Double? = null,
    @SerialName("average_heart_rate") val averageHeartRate: Double? = null,
    @SerialName("max_heart_rate") val maxHeartRate: Double? = null,
)

@Serializable
data class WhoopSleep(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("start") val start: String? = null,
    @SerialName("end") val end: String? = null,
    @SerialName("timezone_offset") val timezoneOffset: String? = null,
    @SerialName("nap") val nap: Boolean? = null,
    @SerialName("score_state") val scoreState: String? = null,
    @SerialName("score") val score: WhoopSleepScore? = null,
)

@Serializable
data class WhoopSleepScore(
    @SerialName("stage_summary") val stageSummary: WhoopSleepStages? = null,
    @SerialName("sleep_needed") val sleepNeeded: WhoopSleepNeeded? = null,
    @SerialName("respiratory_rate") val respiratoryRate: Double? = null,
    @SerialName("sleep_performance_percentage") val sleepPerformancePercentage: Double? = null,
    @SerialName("sleep_consistency_percentage") val sleepConsistencyPercentage: Double? = null,
    @SerialName("sleep_efficiency_percentage") val sleepEfficiencyPercentage: Double? = null,
)

@Serializable
data class WhoopSleepStages(
    @SerialName("total_in_bed_time_milli") val totalInBedMilli: Long? = null,
    @SerialName("total_awake_time_milli") val totalAwakeMilli: Long? = null,
    @SerialName("total_no_data_time_milli") val totalNoDataMilli: Long? = null,
    @SerialName("total_light_sleep_time_milli") val totalLightSleepMilli: Long? = null,
    @SerialName("total_slow_wave_sleep_time_milli") val totalDeepSleepMilli: Long? = null,
    @SerialName("total_rem_sleep_time_milli") val totalRemSleepMilli: Long? = null,
    @SerialName("sleep_cycle_count") val sleepCycleCount: Int? = null,
    @SerialName("disturbance_count") val disturbanceCount: Int? = null,
)

@Serializable
data class WhoopSleepNeeded(
    @SerialName("baseline_milli") val baselineMilli: Long? = null,
    @SerialName("need_from_sleep_debt_milli") val needFromSleepDebtMilli: Long? = null,
    @SerialName("need_from_recent_strain_milli") val needFromRecentStrainMilli: Long? = null,
    @SerialName("need_from_recent_nap_milli") val needFromRecentNapMilli: Long? = null,
)

@Serializable
data class WhoopWorkout(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("start") val start: String? = null,
    @SerialName("end") val end: String? = null,
    @SerialName("timezone_offset") val timezoneOffset: String? = null,
    @SerialName("sport_id") val sportId: Int? = null,
    @SerialName("score_state") val scoreState: String? = null,
    @SerialName("score") val score: WhoopWorkoutScore? = null,
)

@Serializable
data class WhoopWorkoutScore(
    @SerialName("strain") val strain: Double? = null,
    @SerialName("kilojoule") val kilojoule: Double? = null,
    @SerialName("average_heart_rate") val averageHeartRate: Double? = null,
    @SerialName("max_heart_rate") val maxHeartRate: Double? = null,
    @SerialName("percent_recorded") val percentRecorded: Double? = null,
    @SerialName("distance_meter") val distanceMeter: Double? = null,
    @SerialName("altitude_gain_meter") val altitudeGainMeter: Double? = null,
    @SerialName("altitude_change_meter") val altitudeChangeMeter: Double? = null,
    @SerialName("zone_duration") val zoneDuration: WhoopWorkoutZones? = null,
)

/**
 * Whoop teilt jedes Workout in 6 Herzfrequenz-Zonen auf (0 = sehr leicht, 5 = max).
 * Werte sind Aufenthaltsdauer pro Zone in Millisekunden. Manche Whoop-Versionen
 * liefern nur 5 Zonen — `zoneFiveMilli` ist daher optional.
 */
@Serializable
data class WhoopWorkoutZones(
    @SerialName("zone_zero_milli") val zoneZeroMilli: Long? = null,
    @SerialName("zone_one_milli") val zoneOneMilli: Long? = null,
    @SerialName("zone_two_milli") val zoneTwoMilli: Long? = null,
    @SerialName("zone_three_milli") val zoneThreeMilli: Long? = null,
    @SerialName("zone_four_milli") val zoneFourMilli: Long? = null,
    @SerialName("zone_five_milli") val zoneFiveMilli: Long? = null,
)

@Serializable
data class WhoopProfile(
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
)
