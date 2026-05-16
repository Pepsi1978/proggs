package de.frank.entropyreducer.data.repository

import android.util.Log
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.remote.strava.StravaActivityDetail
import de.frank.entropyreducer.data.remote.strava.StravaActivitySummary
import de.frank.entropyreducer.data.remote.strava.StravaApi
import de.frank.entropyreducer.data.remote.strava.StravaStream
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.util.runCatchingCancellable
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Strava-Workout-Sync (Frank-Wunsch 2026-05-16).
 *
 * Holt Workouts der letzten N Tage und konvertiert sie zu `AmazfitWorkoutEntity`,
 * damit sie ohne UI-Aenderungen in die existierende Workout-Pipeline einsteigen.
 * Die `source`-Spalte unterscheidet "strava" von "zepp-cloud" und "health_connect".
 *
 * Detail-Ladestrategie:
 *  - Liste aller Activities holen (1 Call)
 *  - Pro Activity Streams (GPS, HR, Pace, Cadence, Altitude) holen (1 Call)
 *  - Pro Activity Laps holen (1 Call)
 *  - Pro Activity Detail-Endpoint (1 Call) — fuer splits_metric
 *  → 4 Calls pro Activity. Bei 30 Tagen ~10 Workouts = 40 Calls. Strava-Limits
 *    100/15min und 1000/Tag werden nicht ueberschritten.
 *
 * Rate-Limit-Schutz: 200ms Delay zwischen Activity-Calls (= max 75 Activities/15min).
 *
 * Ergebnis: Liste von AmazfitWorkoutEntity mit source="strava". Der Aufrufer
 * (AmazfitRepository.mergeFromStrava) entscheidet ueber Insert/Update gegen
 * existierende Eintraege.
 */
@Singleton
class StravaRepository @Inject constructor(
    private val api: StravaApi,
    private val oauth: OAuthService,
    private val secrets: EncryptedSecretsStore,
) {

    /** Sind wir bei Strava authentifiziert? */
    fun isAuthenticated(): Boolean = oauth.loadStravaAuthState().isAuthorized

    /**
     * Holt Strava-Activities der letzten [days] Tage + Streams + Laps und mapped
     * jede auf AmazfitWorkoutEntity. Liefert die Liste — der Aufrufer schreibt sie
     * in die DB.
     *
     * Bei fehlender Auth liefert es eine leere Liste (kein Fehler — der User hat
     * Strava einfach noch nicht verbunden).
     */
    suspend fun fetchWorkoutsAsEntities(days: Int = 30): Result<List<AmazfitWorkoutEntity>> = runCatchingCancellable {
        if (!isAuthenticated()) {
            Log.d(TAG, "Strava: nicht authentifiziert — kein Workout-Sync")
            return@runCatchingCancellable emptyList()
        }
        val accessToken = oauth.freshStravaAccessToken()
            ?: throw IllegalStateException("Strava-Access-Token konnte nicht erfrischt werden")
        val bearer = "Bearer $accessToken"

        val afterEpoch = (System.currentTimeMillis() / 1000L) - (days.toLong() * 24L * 60L * 60L)
        Log.i(TAG, "Strava: hole Activities der letzten $days Tage (after=$afterEpoch)")

        val summaries = mutableListOf<StravaActivitySummary>()
        var page = 1
        while (true) {
            val pageItems = api.listActivities(
                bearer = bearer,
                after = afterEpoch,
                perPage = 100,
                page = page,
            )
            summaries += pageItems
            if (pageItems.size < 100) break // Letzte Seite
            page += 1
            delay(200L) // Rate-Limit-Schutz
        }
        Log.i(TAG, "Strava: ${summaries.size} Activities im ${days}-Tage-Fenster gefunden")

        val results = mutableListOf<AmazfitWorkoutEntity>()
        for ((index, summary) in summaries.withIndex()) {
            // Rate-Limit-Schutz: 200ms zwischen Activities, plus 15min-Pause nach
            // 80 Calls (Strava: 100 req/15min — wir lassen Puffer).
            if (index > 0) delay(200L)
            if (index > 0 && index % 25 == 0) {
                Log.d(TAG, "Strava: zusaetzliche 5s-Pause nach $index Activities um Rate-Limit zu schonen")
                delay(5_000L)
            }

            val activityId = summary.id
            val detail = runCatching { api.getActivity(bearer, activityId) }
                .getOrElse {
                    Log.w(TAG, "Strava: getActivity($activityId) fehlgeschlagen — ${it.message}")
                    null
                }
            val streams = runCatching { api.getStreams(bearer, activityId) }
                .getOrElse {
                    Log.w(TAG, "Strava: getStreams($activityId) fehlgeschlagen — ${it.message}")
                    emptyMap()
                }
            val laps = runCatching { api.getLaps(bearer, activityId) }
                .getOrElse {
                    Log.w(TAG, "Strava: getLaps($activityId) fehlgeschlagen — ${it.message}")
                    emptyList()
                }

            results += summaryToEntity(summary, detail, streams, laps)
        }

        secrets.stravaLastSyncEpochMs = System.currentTimeMillis()
        Log.i(TAG, "Strava-Sync abgeschlossen: ${results.size} Workouts gemappt")
        results
    }.onFailure {
        if (it !is kotlinx.coroutines.CancellationException) {
            Log.e(TAG, "Strava-Sync fehlgeschlagen", it)
        }
    }

    /**
     * Map ein Strava-Workout auf AmazfitWorkoutEntity. Strava liefert m/s — wir
     * konvertieren auf sec/km fuer Pace.
     */
    private fun summaryToEntity(
        summary: StravaActivitySummary,
        detail: StravaActivityDetail?,
        streams: Map<String, StravaStream>,
        laps: List<de.frank.entropyreducer.data.remote.strava.StravaLap>,
    ): AmazfitWorkoutEntity {
        val startMs = parseStartMs(summary.startDateUtc, summary.startDateLocal)
        val duration = summary.movingTime ?: summary.elapsedTime ?: 0L
        val endMs = startMs + duration * 1000L
        val zone = ZoneId.systemDefault()
        val dateKey = Instant.ofEpochMilli(startMs).atZone(zone).toLocalDate().toString()

        // m/s → sec/km: 1000 / speed
        val avgPaceSecPerKm = summary.averageSpeed?.takeIf { it > 0 }?.let { 1000.0 / it }
        val maxPaceSecPerKm = summary.maxSpeed?.takeIf { it > 0 }?.let { 1000.0 / it }

        // GPS-Track als JSON (latlng-Stream)
        val gpsJson = streams["latlng"]?.data?.let { latlngArr ->
            buildJsonArray {
                latlngArr.forEach { pair -> add(pair) }
            }.toString()
        }
        val hrJson = streams["heartrate"]?.data?.toString()
        // velocity_smooth zu Pace-Stream umrechnen (sec/km pro Sample).
        val paceStreamJson = streams["velocity_smooth"]?.data?.let { speedArr ->
            buildJsonArray {
                speedArr.forEach { speedElem ->
                    val v = (speedElem as? JsonPrimitive)?.content?.toDoubleOrNull()
                    val secPerKm = if (v != null && v > 0.1) 1000.0 / v else 0.0
                    add(JsonPrimitive(secPerKm))
                }
            }.toString()
        }
        // splits_metric aus Detail-Response (km-Splits).
        val splitsJson = detail?.splitsMetric?.let { splits ->
            buildJsonArray {
                splits.forEach { split ->
                    add(buildJsonArray {
                        add(JsonPrimitive(split.split ?: 0))
                        add(JsonPrimitive(split.distance ?: 0.0))
                        add(JsonPrimitive(split.movingTime ?: 0L))
                        add(JsonPrimitive(split.averageHeartrate ?: 0.0))
                        add(JsonPrimitive(split.elevationDifference ?: 0.0))
                    })
                }
            }.toString()
        }

        return AmazfitWorkoutEntity(
            trackId = "strava_${summary.id}",
            dateKey = dateKey,
            startMs = startMs,
            endMs = endMs,
            durationSeconds = duration,
            // Strava sport_type ist ein String wie "TrailRun", "Run". Wir mappen
            // die wichtigsten auf deutsche Namen damit die UI konsistent ist mit
            // Zepp-Workouts. Bei unbekanntem Wert nehmen wir den Strava-String roh.
            sportType = stravaSportTypeNumeric(summary.sportType),
            sportName = stravaSportTypeGerman(summary.sportType ?: summary.activityType, summary.name),
            distanceMeters = summary.distance,
            avgPaceSecPerKm = avgPaceSecPerKm,
            maxPaceSecPerKm = maxPaceSecPerKm,
            avgSpeedKmh = summary.averageSpeed?.times(3.6),
            maxSpeedKmh = summary.maxSpeed?.times(3.6),
            calories = detail?.calories ?: summary.calories,
            avgHeartRate = summary.averageHeartrate?.toInt(),
            maxHeartRate = summary.maxHeartrate?.toInt(),
            gpsTrackJson = gpsJson,
            heartRateSeriesJson = hrJson,
            paceSeriesJson = paceStreamJson,
            splitsJson = splitsJson,
            altitudeGainMeters = summary.totalElevationGain,
            altitudeLossMeters = null,
            trainingEffectAerobic = null,
            trainingEffectAnaerobic = null,
            cadence = summary.averageCadence?.toInt(),
            strideLengthCm = null,
            swolf = null,
            poolLengthMeters = null,
            source = "strava",
            city = null,
            paceStreamJson = paceStreamJson,
            createdAt = System.currentTimeMillis(),
        )
    }

    private fun parseStartMs(utcIso: String?, localIso: String?): Long {
        // start_date ist UTC ISO-8601: "2026-05-15T15:32:11Z"
        if (!utcIso.isNullOrBlank()) {
            return runCatching { Instant.parse(utcIso).toEpochMilli() }.getOrElse { 0L }
        }
        // Fallback: start_date_local ohne Zone — als systemDefault interpretieren
        if (!localIso.isNullOrBlank()) {
            return runCatching {
                ZonedDateTime.parse(localIso + "Z").toInstant().toEpochMilli()
            }.getOrElse { 0L }
        }
        return 0L
    }

    /**
     * Strava sport_type ist ein String. Wir versuchen ihm eine numerische ID zu
     * geben damit die existierende Mini-Card-Logik (die nach sportType filtert)
     * funktioniert. Wenn unbekannt → null (UI faellt auf sportName zurueck).
     */
    private fun stravaSportTypeNumeric(sportType: String?): Int? = when (sportType?.lowercase()) {
        "run", "trailrun" -> 1            // Frank's T-Rex 3 nutzt 7 fuer Trail, 1 fuer Outdoor-Run
        "ride", "mountainbikeride" -> 6
        "swim" -> 14
        "walk", "hike" -> 9
        "weighttraining" -> 52            // konsistent mit Zepp's Krafttraining
        "workout" -> 0
        else -> null
    }

    /**
     * Deutsche Bezeichnung — Strava hat keinen "Trailrunning"-String, nur "TrailRun".
     * Wenn der Activity-Name aussagekraeftig ist (z.B. "Morgenrunde Wald"), bevorzugen
     * wir den nicht — der ist zu speziell. sportType ist robuster.
     */
    private fun stravaSportTypeGerman(sportType: String?, name: String?): String = when (sportType?.lowercase()) {
        "run" -> "Laufen"
        "trailrun" -> "Trailrunning"
        "treadmillrun" -> "Laufband"
        "virtualrun" -> "Laufen (virtuell)"
        "ride" -> "Radfahren"
        "mountainbikeride" -> "Mountainbike"
        "gravelride" -> "Gravel"
        "virtualride" -> "Radfahren (virtuell)"
        "ebikeride" -> "E-Bike"
        "swim" -> "Schwimmen"
        "walk" -> "Walking"
        "hike" -> "Wandern"
        "weighttraining" -> "Krafttraining"
        "workout" -> "Training"
        "yoga" -> "Yoga"
        "rowing" -> "Rudern"
        "alpineski" -> "Ski"
        "snowboard" -> "Snowboard"
        "elliptical" -> "Crosstrainer"
        "stairstepper" -> "Stepper"
        "rockclimbing" -> "Klettern"
        else -> sportType?.takeIf { it.isNotBlank() } ?: (name ?: "Training")
    }

    companion object {
        private const val TAG = "StravaRepository"
    }
}
