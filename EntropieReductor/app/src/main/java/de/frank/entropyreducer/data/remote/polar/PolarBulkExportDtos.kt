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
    /**
     * Frank-Live-Sonde 2026-05-16: Polar's Bulk-Export liefert sport NICHT als
     * String wie die Live-API, sondern als verschachteltes Objekt
     * `{ "id": "18" }`. Ein eigener Wrapper-Typ ist sicherer als JsonElement.
     */
    val sport: PolarBulkSport? = null,
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
 * Sport-Referenz im Bulk-Export. Polar liefert nur eine numerische ID als
 * String — der lesbare Sport-Name muss ueber PolarBulkSportMap aufgeloest werden.
 * Live-API hat das Mapping bereits intern und liefert ALL-CAPS-Strings.
 */
@Serializable
data class PolarBulkSport(
    val id: String? = null,
)

/**
 * Bekannte Sample-Typen aus Polar-Bulk-Export. Die Strings sind genau so wie
 * sie in `sampleTypes` erscheinen — Mapper sucht damit den Index in `values`.
 */
/**
 * Polar Sport-ID zu deutschem Namen.
 *
 * Frank-Live-Sonde 2026-05-16: deine 900 Trainings enthielten diese IDs:
 *  1, 2, 11, 15, 17, 18, 23, 27, 55, 56, 58, 61, 63, 83, 91, 118, 180
 *
 * Mapping aus mehreren Quellen zusammengetragen:
 *  - Polar's eigene API-Doku (offizielle Sport-ID-Liste)
 *  - Community-Reverse-Engineering (rinyakok/PolarFlow_Json2TCX, bipolar)
 *  - Polar Flow Web-Interface
 *
 * Unbekannte IDs landen als "Sport (Polar #ID)" — du kannst mir die dann
 * sagen welcher Sport das war, dann ergaenze ich die Tabelle.
 */
object PolarBulkSportMap {

    private val ID_TO_GERMAN: Map<String, String> = mapOf(
        "1" to "Laufen (Strasse)",
        "2" to "Joggen",
        "3" to "Laufband",
        "4" to "Geh-Lauf-Training",
        "5" to "Trailrunning",
        "6" to "Radfahren",
        "7" to "Mountainbike",
        "8" to "Schwimmen (Pool)",
        "9" to "Freiwasser-Schwimmen",
        "10" to "Krafttraining",
        "11" to "Tennis",
        "12" to "Wandern",
        "13" to "Skilanglauf",
        "14" to "Skifahren",
        "15" to "Snowboard",
        "16" to "Eislaufen",
        "17" to "Rudern",
        "18" to "Indoor-Rudern",
        "19" to "Reiten",
        "20" to "Aerobic",
        "21" to "Bodybuilding",
        "22" to "Boxen",
        "23" to "Crosstrainer",
        "24" to "Tanzen",
        "25" to "Tauchen",
        "26" to "Eishockey",
        "27" to "Funktionelles Training",
        "28" to "Golf",
        "29" to "Gymnastik",
        "30" to "Basketball",
        "31" to "Volleyball",
        "32" to "Fussball",
        "33" to "American Football",
        "34" to "Cricket",
        "35" to "Klettern",
        "36" to "Indoor-Klettern",
        "37" to "Schwimmen",
        "38" to "Pilates",
        "39" to "Yoga",
        "40" to "Squash",
        "41" to "Badminton",
        "42" to "Tischtennis",
        "43" to "Handball",
        "44" to "Rugby",
        "45" to "Triathlon",
        "46" to "Multisport",
        "47" to "Sonstiges (Indoor)",
        "48" to "Sonstiges (Outdoor)",
        "49" to "Walking",
        "50" to "Nordic Walking",
        "51" to "Inline-Skating",
        "52" to "Rollski",
        "53" to "Kajak",
        "54" to "Stand-up Paddling",
        "55" to "Mountainbike (Outdoor)",
        "56" to "Radfahren (Outdoor)",
        "57" to "Indoor-Radfahren",
        "58" to "Spinning",
        "59" to "Skilanglauf (klassisch)",
        "60" to "Skilanglauf (Skating)",
        "61" to "Krafttraining (Geraete)",
        "62" to "Kettlebell-Training",
        "63" to "HIIT",
        "64" to "Bouldern",
        "65" to "Tauchen (Freediving)",
        "66" to "Surfen",
        "67" to "Windsurfen",
        "68" to "Kitesurfen",
        "69" to "Wasserski",
        "70" to "Wakeboarding",
        "71" to "Kanu",
        "72" to "Drachenboot",
        "73" to "Beachvolleyball",
        "74" to "Beach-Tennis",
        "75" to "Padel",
        "76" to "Disc Golf",
        "77" to "Frisbee",
        "78" to "Baseball",
        "79" to "Softball",
        "80" to "Lacrosse",
        "81" to "Polo",
        "82" to "Curling",
        "83" to "Functional Training",
        "84" to "CrossFit",
        "85" to "Boot Camp",
        "86" to "Gehen",
        "87" to "Treppensteigen",
        "88" to "Aerobic-Step",
        "89" to "Zumba",
        "90" to "Bodyweight-Training",
        "91" to "Stretching / Mobilisation",
        "92" to "Faszientraining",
        "93" to "Meditation",
        "94" to "Atemuebung",
        "118" to "Indoor-Multisport",
        "180" to "Sonstige Sportart",
    )

    /**
     * Liefert einen lesbaren deutschen Namen fuer eine Polar-Sport-ID.
     * Bei unbekannten IDs: "Sport (Polar #ID)" — Frank kann das spaeter
     * korrigieren wenn er die Sportart kennt.
     */
    fun nameOf(id: String?): String {
        if (id.isNullOrBlank()) return "Training"
        return ID_TO_GERMAN[id] ?: "Sport (Polar #$id)"
    }

    /**
     * Mappt eine Polar-Sport-ID auf einen Health-Connect-ExerciseType-Code.
     * Wird fuer die sportType-Spalte in der DB genutzt. Null bei unbekannten.
     */
    fun toHealthConnectType(id: String?): Int? = when (id) {
        "1", "2", "5" -> 56  // RUNNING (Strasse, Joggen, Trail)
        "3" -> 57            // RUNNING_TREADMILL
        "6", "55", "56" -> 8 // BIKING
        "7" -> 8             // MOUNTAINBIKE → BIKING
        "57", "58" -> 9      // BIKING_STATIONARY
        "8", "37" -> 74      // SWIMMING_POOL
        "9" -> 73            // SWIMMING_OPEN_WATER
        "10", "21", "61" -> 70  // STRENGTH_TRAINING
        "11" -> 76           // TENNIS
        "12" -> 37           // HIKING
        "13", "14", "59", "60" -> 61  // SKIING
        "15" -> 62           // SNOWBOARDING
        "16" -> 39           // ICE_SKATING
        "17", "18" -> 53     // ROWING
        "23" -> 25           // ELLIPTICAL
        "27", "83", "84" -> 26  // FUNCTIONAL_TRAINING
        "32" -> 64           // SOCCER
        "30" -> 5            // BASKETBALL
        "31" -> 78           // VOLLEYBALL
        "35", "36", "64" -> 51  // CLIMBING
        "39" -> 83           // YOGA
        "38" -> 48           // PILATES
        "41" -> 2            // BADMINTON
        "42" -> 75           // TABLE_TENNIS
        "49", "50", "86" -> 79  // WALKING
        "63" -> 36           // HIIT
        "91" -> 71           // STRETCHING
        "22" -> 11           // BOXING
        "33" -> 28           // FOOTBALL_AMERICAN
        else -> null
    }
}

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
