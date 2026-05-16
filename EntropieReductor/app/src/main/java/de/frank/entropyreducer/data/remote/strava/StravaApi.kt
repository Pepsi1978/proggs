package de.frank.entropyreducer.data.remote.strava

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Strava API V3 — offizielle REST-API fuer Athleten-Workouts.
 *
 * Authentifizierung: OAuth 2.0 (Confidential Client), Bearer-Token im Header.
 * Token-Lifecycle: Access-Token gueltig 6 Stunden, Refresh-Token rotiert bei
 * jedem Refresh. AppAuth verwaltet das im AuthState (siehe OAuthService.strava*).
 *
 * Rate-Limits (Personal Use App, ohne erhoehten Zugang):
 *  - 100 Read-Requests pro 15 Minuten
 *  - 1000 Read-Requests pro Tag
 *  - Pro Workout brauchen wir ~3 Calls (Detail + Streams + Laps) — bei 10 neuen
 *    Workouts/Woche also weit unter dem Limit.
 *
 * Frank-Wunsch 2026-05-16: Strava als primaere Workout-Quelle nachdem die
 * Zepp-Cloud seit Update 10.x in der Zepp-App nur noch abgespeckte Daten oder
 * gar keine neuen Workouts mehr ausliefert. Strava bekommt die Workouts direkt
 * von der Zepp-App via Drittanbieter-Push (Profil → Verknuepfung Drittanbieter).
 */
interface StravaApi {
    /**
     * Liefert die Workouts des authentifizierten Athleten in absteigender Reihenfolge
     * nach `start_date_local`. Pro Page max 200 (wir nutzen 100 als stabiler Default).
     *
     * @param bearer Vollwertiger Authorization-Header (z.B. "Bearer 1234...")
     * @param after Unix-Timestamp (Sekunden) — nur Activities NACH diesem Zeitpunkt
     * @param before Unix-Timestamp (Sekunden) — nur Activities VOR diesem Zeitpunkt
     * @param page 1-basiert, 0 ist nicht erlaubt
     */
    @GET("api/v3/athlete/activities")
    suspend fun listActivities(
        @Header("Authorization") bearer: String,
        @Query("after") after: Long? = null,
        @Query("before") before: Long? = null,
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
    ): List<StravaActivitySummary>

    /**
     * Detail-Daten fuer ein einzelnes Workout — enthaelt Splits, Laps, Segment-Efforts,
     * Beschreibung. Wird auf Tap in die Detail-Card geladen, plus beim Initial-Sync.
     */
    @GET("api/v3/activities/{id}")
    suspend fun getActivity(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long,
        @Query("include_all_efforts") includeAllEfforts: Boolean = false,
    ): StravaActivityDetail

    /**
     * Zeitreihen-Daten (GPS, Puls, Geschwindigkeit, Cadence, Hoehe). Strava nennt
     * das "Streams". key_by_type=true liefert eine JSON-Map { typ → Stream } statt
     * Array — viel einfacher zu deserialisieren.
     *
     * Keys-Konvention bei Strava (kommagetrennt):
     *  - time            Sekunden seit Start
     *  - latlng          [lat, lng]-Paare
     *  - heartrate       bpm
     *  - velocity_smooth m/s (geglaettet)
     *  - cadence         rpm (Strides pro Minute fuer Laufen)
     *  - altitude        m
     *  - distance        m kumuliert
     *  - grade_smooth    Prozent Steigung (geglaettet)
     *  - temp            Celsius (selten)
     *  - watts           W (nur bei Rad-Power-Metern)
     *
     * resolution=high liefert bis 10.000 Datenpunkte — perfekt fuer GPS-Karten.
     */
    @GET("api/v3/activities/{id}/streams")
    suspend fun getStreams(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long,
        @Query("keys") keys: String = "time,latlng,heartrate,velocity_smooth,cadence,altitude,distance,grade_smooth",
        @Query("key_by_type") keyByType: Boolean = true,
        @Query("resolution") resolution: String = "high",
    ): Map<String, StravaStream>

    /** km-Splits oder Auto-Laps. Liefert avg HR + Pace + Dauer pro Lap. */
    @GET("api/v3/activities/{id}/laps")
    suspend fun getLaps(
        @Header("Authorization") bearer: String,
        @Path("id") id: Long,
    ): List<StravaLap>
}

/* ---------------------- DTOs ---------------------- */

/**
 * Summary-Eintrag aus listActivities — leichtgewichtig, keine Streams oder Splits.
 * Felder die wir aktuell NICHT brauchen sind ausgelassen (Strava liefert ~70 Felder
 * pro Activity, wir mappen nur die ~20 die wir wirklich verwenden).
 */
@Serializable
data class StravaActivitySummary(
    val id: Long,
    val name: String? = null,
    @SerialName("sport_type") val sportType: String? = null,
    @SerialName("type") val activityType: String? = null,
    @SerialName("start_date") val startDateUtc: String? = null,
    @SerialName("start_date_local") val startDateLocal: String? = null,
    val timezone: String? = null,
    /** Seconds */
    @SerialName("elapsed_time") val elapsedTime: Long? = null,
    /** Seconds */
    @SerialName("moving_time") val movingTime: Long? = null,
    /** Meters */
    val distance: Double? = null,
    @SerialName("total_elevation_gain") val totalElevationGain: Double? = null,
    /** m/s */
    @SerialName("average_speed") val averageSpeed: Double? = null,
    /** m/s */
    @SerialName("max_speed") val maxSpeed: Double? = null,
    /** Beats per minute, Double weil Strava Dezimalpraezision liefert. */
    @SerialName("average_heartrate") val averageHeartrate: Double? = null,
    @SerialName("max_heartrate") val maxHeartrate: Double? = null,
    @SerialName("average_cadence") val averageCadence: Double? = null,
    /** kcal — Strava liefert das in der Summary nicht immer; im Detail meist ja. */
    val calories: Double? = null,
    @SerialName("has_heartrate") val hasHeartrate: Boolean = false,
    /** Polyline (encoded) — wir nutzen es nicht weil wir die latlng-Streams holen. */
    val map: StravaMap? = null,
)

@Serializable
data class StravaMap(
    val id: String? = null,
    @SerialName("summary_polyline") val summaryPolyline: String? = null,
)

/**
 * Detailed Activity — alles aus dem Summary plus Splits, Laps, Beschreibung, Gear.
 * Strava liefert hier zusaetzlich `description`, `gear`, `splits_metric` (km-Splits),
 * `segment_efforts` (wir ignorieren — Strava-spezifisch).
 */
@Serializable
data class StravaActivityDetail(
    val id: Long,
    val name: String? = null,
    @SerialName("sport_type") val sportType: String? = null,
    @SerialName("start_date") val startDateUtc: String? = null,
    @SerialName("start_date_local") val startDateLocal: String? = null,
    @SerialName("elapsed_time") val elapsedTime: Long? = null,
    @SerialName("moving_time") val movingTime: Long? = null,
    val distance: Double? = null,
    @SerialName("total_elevation_gain") val totalElevationGain: Double? = null,
    @SerialName("average_speed") val averageSpeed: Double? = null,
    @SerialName("max_speed") val maxSpeed: Double? = null,
    @SerialName("average_heartrate") val averageHeartrate: Double? = null,
    @SerialName("max_heartrate") val maxHeartrate: Double? = null,
    @SerialName("average_cadence") val averageCadence: Double? = null,
    val calories: Double? = null,
    val description: String? = null,
    @SerialName("splits_metric") val splitsMetric: List<StravaSplit>? = null,
    @SerialName("device_name") val deviceName: String? = null,
)

@Serializable
data class StravaSplit(
    /** 1-basiert */
    val split: Int? = null,
    val distance: Double? = null,
    @SerialName("elapsed_time") val elapsedTime: Long? = null,
    @SerialName("moving_time") val movingTime: Long? = null,
    @SerialName("elevation_difference") val elevationDifference: Double? = null,
    /** m/s */
    @SerialName("average_speed") val averageSpeed: Double? = null,
    @SerialName("average_heartrate") val averageHeartrate: Double? = null,
    @SerialName("pace_zone") val paceZone: Int? = null,
)

@Serializable
data class StravaLap(
    val id: Long? = null,
    val name: String? = null,
    @SerialName("lap_index") val lapIndex: Int? = null,
    @SerialName("split") val split: Int? = null,
    @SerialName("elapsed_time") val elapsedTime: Long? = null,
    @SerialName("moving_time") val movingTime: Long? = null,
    val distance: Double? = null,
    @SerialName("total_elevation_gain") val totalElevationGain: Double? = null,
    @SerialName("average_speed") val averageSpeed: Double? = null,
    @SerialName("max_speed") val maxSpeed: Double? = null,
    @SerialName("average_cadence") val averageCadence: Double? = null,
    @SerialName("average_heartrate") val averageHeartrate: Double? = null,
    @SerialName("max_heartrate") val maxHeartrate: Double? = null,
)

/**
 * Stream — eine einzelne Zeitreihe. data ist immer ein JSON-Array, der Typ wird
 * vom `type`-Feld bestimmt. Wir behalten data hier als JsonArray weil es entweder
 * Number-Liste (heartrate, velocity, altitude, distance, cadence, grade_smooth) oder
 * Array-of-Arrays ([lat, lng]-Paare bei latlng) ist.
 */
@Serializable
data class StravaStream(
    val type: String? = null,
    /** Stets ein JSON-Array — wir deserialisieren on-demand zu List<Double> oder List<List<Double>>. */
    val data: kotlinx.serialization.json.JsonArray? = null,
    @SerialName("series_type") val seriesType: String? = null,
    @SerialName("original_size") val originalSize: Int? = null,
    val resolution: String? = null,
)
