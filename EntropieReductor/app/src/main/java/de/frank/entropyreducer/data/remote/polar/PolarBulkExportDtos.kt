package de.frank.entropyreducer.data.remote.polar

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * DTOs fuer den Polar-Flow-Bulk-Export (ZIP-Datei die Polar per Mail verschickt).
 *
 * Frank-Wunsch 2026-05-16: Polar-API liefert nur die letzten 4 Wochen — die
 * gesamte Trainings-Historie (10 Jahre) kommt nur ueber den manuellen Export
 * via flow.polar.com/data/export-data. Format: ZIP mit pro Training EINER
 * JSON-Datei im Ordner `training/training-session-YYYY-MM-DD-{id}-{uuid}.json`.
 *
 * Format-Unterschiede zur Live-AccessLink-API:
 *  - Bulk: Session-Wrapper hat `exercises[]`-Array (fuer Multi-Sport wie Duathlon
 *    > 1, sonst genau 1). Live-API liefert Exercise direkt.
 *  - Bulk: Samples sind INLINE als Array von {timestamp, values[]}. Live-API hat
 *    separate Endpoints pro Sample-Typ mit kommagetrennten Strings.
 *  - Bulk: `sampleTypes`-Array definiert die Reihenfolge der Werte in `values`.
 *    Wenn `sampleTypes = ["HEART_RATE", "SPEED", "ALTITUDE"]`, dann ist
 *    `values[0]` HR, `values[1]` Speed, `values[2]` Altitude.
 *  - Bulk: GPS als `recordedRoute` inline. Live-API als separater GPX-Endpoint.
 *  - Bulk: Feldnamen mit camelCase (z.B. `sampleTypes`), Top-Level mit Bindestrich
 *    (z.B. `start-time`). Beides beruecksichtigt via @SerialName.
 *
 * Defensiv parsen: viele Felder optional, da aeltere Geraete (z.B. ohne GPS oder
 * HR) entsprechende Sub-Felder weglassen. `ignoreUnknownKeys = true` ist Pflicht.
 */

/**
 * Session-Wrapper — entspricht einer Trainings-Session-Datei aus dem ZIP.
 * Multi-Sport-Sessions (Duathlon, Triathlon) haben mehrere Eintraege in
 * `exercises`. Bei normalen Single-Sport-Trainings genau einen.
 */
@Serializable
data class PolarBulkSession(
    val id: Long,
    /** Lokale Startzeit OHNE Zeitzone, z.B. "2024-11-15T07:30:00". */
    @SerialName("start-time") val startTime: String,
    /** UTC-Offset in Minuten (z.B. 60 fuer MEZ). */
    @SerialName("start-time-utc-offset") val startTimeUtcOffset: Int = 0,
    /** ISO-8601-Dauer, z.B. "PT1H30M45S". */
    val duration: String? = null,
    val device: String? = null,
    @SerialName("device-id") val deviceId: String? = null,
    @SerialName("upload-time") val uploadTime: String? = null,
    /** Gesamtkalorien der Session. Bei Multi-Sport: Summe ueber Exercises. */
    val calories: Int? = null,
    /** Gesamtstrecke in Metern. */
    val distance: Float? = null,
    @SerialName("heart-rate") val heartRate: PolarHeartRateSummary? = null,
    @SerialName("training-load") val trainingLoad: Double? = null,
    @SerialName("training-load-pro") val trainingLoadPro: PolarTrainingLoadPro? = null,
    @SerialName("running-index") val runningIndex: Int? = null,
    /** Multi-Sport-faehig. Bei Single-Sport: 1 Eintrag. */
    val exercises: List<PolarBulkExercise> = emptyList(),
)

/**
 * Einzelne Exercise innerhalb einer Session.
 *
 * Wichtige Felder fuer den Mapper:
 *  - sport: ALL_CAPS-Enum, z.B. "RUNNING", "CYCLING", "POOL_SWIMMING"
 *  - sampleTypes: Array von Sample-Typ-Namen (z.B. ["HEART_RATE", "SPEED"]).
 *    Index in diesem Array = Position in jedem samples[].values[]-Array
 *  - samples: Array von Sample-Punkten (typischerweise 1/Sekunde)
 *  - recordedRoute: GPS-Track-Punkte
 */
@Serializable
data class PolarBulkExercise(
    val sport: String? = null,
    @SerialName("detailed-sport-info") val detailedSportInfo: String? = null,
    val duration: String? = null,
    val distance: Float? = null,
    val calories: Int? = null,
    @SerialName("heart-rate") val heartRate: PolarHeartRateSummary? = null,
    /**
     * Reihenfolge der Werte in jedem samples[].values-Array.
     * Bekannte Typen: HEART_RATE, SPEED, CADENCE, ALTITUDE, POWER, PACE,
     * TEMPERATURE, DISTANCE, RR_INTERVAL, GROUND_CONTACT_TIME,
     * VERTICAL_OSCILLATION, RUNNING_STRIDE_LENGTH.
     */
    val sampleTypes: List<String> = emptyList(),
    val samples: List<PolarBulkSample> = emptyList(),
    val recordedRoute: List<PolarBulkRoutePoint> = emptyList(),
    val laps: JsonElement? = null,
    val zones: JsonElement? = null,
)

/**
 * Ein einzelner Sample-Punkt. `values[i]` korrespondiert mit `sampleTypes[i]`
 * im Eltern-Exercise. Werte koennen `null` sein (Lücke im Stream).
 *
 * Polar liefert teilweise Float und Int gemischt — wir parsen als JsonArray
 * und konvertieren on-demand.
 */
@Serializable
data class PolarBulkSample(
    /** Unix-Millis. Live-API-Samples haben das nicht — Bulk schon. */
    val timestamp: Long? = null,
    /**
     * Werte-Array. Mischformat aus Int (HR, Cadence) und Float (Speed,
     * Altitude). JsonArray um Parser-Crash bei Mischformaten zu vermeiden.
     */
    val values: JsonArray? = null,
)

/**
 * GPS-Track-Punkt mit Zeitstempel + Position + Hoehe.
 * Sekuendliche Aufloesung bei den meisten Polar-Geraeten.
 */
@Serializable
data class PolarBulkRoutePoint(
    /** Unix-Millis. */
    val timestamp: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
)

/**
 * Bekannte Sample-Typen aus Polar-Bulk-Export. Die Strings sind genau so wie
 * sie in `sampleTypes` erscheinen — Mapper sucht damit den Index in `values`.
 */
object PolarBulkSampleType {
    const val HEART_RATE = "HEART_RATE"
    const val SPEED = "SPEED"
    const val CADENCE = "CADENCE"
    const val ALTITUDE = "ALTITUDE"
    const val POWER = "POWER"
    /** Polar liefert PACE oft direkt als Sekunden pro km — bevorzugt nutzen. */
    const val PACE = "PACE"
    const val TEMPERATURE = "TEMPERATURE"
    const val DISTANCE = "DISTANCE"
    const val RR_INTERVAL = "RR_INTERVAL"
    const val GROUND_CONTACT_TIME = "GROUND_CONTACT_TIME"
    const val VERTICAL_OSCILLATION = "VERTICAL_OSCILLATION"
    const val RUNNING_STRIDE_LENGTH = "RUNNING_STRIDE_LENGTH"
}
