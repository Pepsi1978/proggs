package de.frank.entropyreducer.data.remote.polar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * DTOs fuer den Polar-Flow-Bulk-Export (ZIP-Datei die Polar per Mail verschickt).
 *
 * Frank-Live-Sonde 2026-05-16 (Iteration 7): Nach mehreren Iterationen die alle
 * an Format-Diskrepanzen scheiterten habe ich Frank's echte ZIP ausgepackt und
 * ein Training-JSON inspiziert. Das Format ist KOMPLETT anders als die Doku
 * und der Researcher-Bericht sagten:
 *
 * Top-Level (camelCase, KEINE Hyphens):
 *  - identifier.id (STRING, nicht Long)
 *  - startTime (NICHT "start-time"): "2025-07-26T15:38:28" lokal ohne TZ
 *  - stopTime, durationMillis (Long), timezoneOffsetMinutes
 *  - calories (Int), hrAvg (Int), hrMax (Int) - alle direkt auf Top-Level
 *  - sport: { id: String }
 *  - product: { modelName: String }
 *  - exercises: List<...>
 *  - trainingLoadReport: { cardioLoad, muscleLoad, ... }
 *
 * ALLE Felder OPTIONAL machen — Polar variiert je nach Geraet und Jahrgang.
 */
@Serializable
data class PolarBulkSession(
    val identifier: PolarBulkIdentifier? = null,
    val startTime: String? = null,
    val stopTime: String? = null,
    val durationMillis: Long? = null,
    val calories: Int? = null,
    val timezoneOffsetMinutes: Int? = null,
    val hrAvg: Int? = null,
    val hrMax: Int? = null,
    val name: String? = null,
    val deviceId: String? = null,
    val sport: PolarBulkSport? = null,
    val product: PolarBulkProduct? = null,
    val trainingLoadReport: PolarBulkTrainingLoadReport? = null,
    val exercises: List<PolarBulkExercise> = emptyList(),
    val carboPercentage: Int? = null,
    val fatPercentage: Int? = null,
    val proteinPercentage: Int? = null,
    val recoveryTimeMillis: String? = null,  // Polar liefert das als String mit Zahl drin
    val trainingBenefit: String? = null,
)

/**
 * Sub-Eintrag fuer Multi-Sport (Triathlon etc.). Bei normalen Sessions
 * genau ein Eintrag.
 */
@Serializable
data class PolarBulkExercise(
    val identifier: PolarBulkIdentifier? = null,
    val startTime: String? = null,
    val stopTime: String? = null,
    val durationMillis: Long? = null,
    val calories: Int? = null,
    val timezoneOffsetMinutes: Int? = null,
    val sport: PolarBulkSport? = null,
    val trainingLoadReport: PolarBulkTrainingLoadReport? = null,
    val statistics: PolarBulkStatisticsWrapper? = null,
    val zones: JsonElement? = null,
    /**
     * Frank-Live-Sonde 2026-05-16: samples.samples[] enthaelt pro Metrik-Typ
     * ein Eintrag mit type + intervalMillis + values[]. Bekannte Typen:
     * HEART_RATE, SPEED, DISTANCE, CADENCE, ALTITUDE, TEMPERATURE,
     * STRIDE_LENGTH, LEFT_CRANK_CURRENT_POWER.
     */
    val samples: PolarBulkSamplesWrapper? = null,
    /**
     * Frank-Live-Sonde 2026-05-16: routes.route.wayPoints[] enthaelt GPS-Punkte
     * mit {longitude, latitude, altitude, elapsedMillis}. Bei Indoor-Trainings ist
     * routes null oder leer.
     */
    val routes: PolarBulkRoutesWrapper? = null,
    val laps: JsonElement? = null,
    val strengthTrainingResults: JsonElement? = null,
)

@Serializable
data class PolarBulkSamplesWrapper(
    val samples: List<PolarBulkSampleEntry> = emptyList(),
)

/**
 * Ein einzelner typisierter Sample-Stream.
 *
 * `values` ist JsonElement-Liste weil Polar in den Werten "NaN" als String
 * mischen kann — z.B. bei Indoor-Trainings wo DISTANCE und SPEED keine
 * Werte haben. kotlinx.serialization wirft sonst NumberFormatException.
 */
@Serializable
data class PolarBulkSampleEntry(
    val type: String? = null,
    val intervalMillis: Long? = null,
    val values: List<JsonElement> = emptyList(),
)

@Serializable
data class PolarBulkRoutesWrapper(
    val route: PolarBulkRoute? = null,
)

@Serializable
data class PolarBulkRoute(
    val startTime: String? = null,
    val wayPoints: List<PolarBulkWayPoint> = emptyList(),
)

@Serializable
data class PolarBulkWayPoint(
    val longitude: Double? = null,
    val latitude: Double? = null,
    val altitude: Double? = null,
    val elapsedMillis: Long? = null,
)

@Serializable
data class PolarBulkIdentifier(
    val id: String? = null,
)

@Serializable
data class PolarBulkSport(
    val id: String? = null,
)

@Serializable
data class PolarBulkProduct(
    val modelName: String? = null,
)

@Serializable
data class PolarBulkTrainingLoadReport(
    val cardioLoad: Double? = null,
    val muscleLoad: Double? = null,
    val cardioLoadInterpretation: String? = null,
    val muscleLoadInterpretation: String? = null,
    val perceivedLoadInterpretation: String? = null,
)

@Serializable
data class PolarBulkStatisticsWrapper(
    val statistics: List<PolarBulkStatisticsEntry> = emptyList(),
)

@Serializable
data class PolarBulkStatisticsEntry(
    val type: String? = null,  // z.B. "STATISTICS_TYPE_HEART_RATE"
    val min: Double? = null,
    val avg: Double? = null,
    val max: Double? = null,
)

/**
 * Polar Sport-ID zu deutschem Namen.
 *
 * Frank-Live-Sonde 2026-05-16: 17 verschiedene IDs in 956 Trainings:
 *  1, 2, 11, 15, 17, 18, 23, 27, 55, 56, 58, 61, 63, 83, 91, 118, 180
 *
 * Mapping aus mehreren Quellen + community Reverse-Engineering. Bei
 * unbekannten IDs landen wir bei "Sport (Polar #ID)".
 */
object PolarBulkSportMap {

    private val ID_TO_GERMAN: Map<String, String> = mapOf(
        "1" to "Laufen (Strasse)",
        "2" to "Joggen",
        "3" to "Laufband",
        "5" to "Trailrunning",
        "11" to "Tennis",
        "12" to "Wandern",
        "15" to "Snowboard",
        "17" to "Rudern",
        // Frank-Wunsch 2026-05-17: "Indoor-Rudern" wird als "Crosstrainer" gefuehrt
        // (sein Polar zeichnet Crosstrainer-Workouts mit diesem Polar-Sportcode auf).
        "18" to "Crosstrainer",
        "23" to "Crosstrainer",
        // Frank-Wunsch 2026-05-17: Code 27 ist bei Frank's Polar tatsaechlich
        // Trailrunning (alle 15 Eintraege haben Distanz 6km+, GPS, HR-Stream
        // und Pace-Stream). Die generische Polar-Bezeichnung "Funktionelles
        // Training" passt nicht zu seiner Nutzung.
        "27" to "Trailrunning",
        "32" to "Fussball",
        "55" to "Mountainbike (Outdoor)",
        "56" to "Radfahren (Outdoor)",
        "57" to "Indoor-Radfahren",
        "58" to "Spinning",
        "61" to "Krafttraining (Geraete)",
        "63" to "HIIT",
        "83" to "Functional Training",
        "91" to "Stretching / Mobilisation",
        "118" to "Indoor-Multisport",
        "180" to "Sonstige Sportart",
    )

    fun nameOf(id: String?): String {
        if (id.isNullOrBlank()) return "Training"
        return ID_TO_GERMAN[id] ?: "Sport (Polar #$id)"
    }

    fun toHealthConnectType(id: String?): Int? = when (id) {
        "1", "2", "5" -> 56
        "3" -> 57
        "11" -> 76
        "12" -> 37
        "15" -> 62
        "17", "18" -> 53
        "23" -> 25
        "27", "83" -> 26
        "32" -> 64
        "55", "56" -> 8
        "57", "58" -> 9
        "61" -> 70
        "63" -> 36
        "91" -> 71
        else -> null
    }
}
