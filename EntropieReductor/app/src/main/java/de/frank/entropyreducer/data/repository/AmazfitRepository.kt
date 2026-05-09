package de.frank.entropyreducer.data.repository

import android.util.Log
import de.frank.entropyreducer.data.local.dao.AmazfitDailyDao
import de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao
import de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.remote.zepp.ZeppApi
import de.frank.entropyreducer.data.remote.zepp.ZeppAuthService
import de.frank.entropyreducer.data.remote.zepp.ZeppEndpoints
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holt taegliche Werte und Workouts aus der Zepp-Cloud (Amazfit T-Rex 3) und
 * speichert sie in zwei eigenen Tabellen (`amazfit_daily`, `amazfit_workouts`).
 *
 * Frank-Wunsch 2026-05-09:
 *  - PAI, BioCharge, Hauttemperatur, Stress, SpO2, Atemfrequenz im Biomarker-Bereich
 *  - Sport-Daten (Trainingsart, Dauer, GPS-Track, Pace, Pulsverlauf, etc.)
 *    in einem eigenen Sport-Bereich unterhalb der Biomarker-Karten
 *
 * Strategie:
 *  - Sync-Fenster default 365 Tage (analog zu Whoop)
 *  - Bei abgelaufenem App-Token automatisch Re-Login mit gespeicherten Credentials
 *  - Defensives JSON-Parsing: viele Felder werden vom Zepp-Server in Sub-Objekten
 *    geliefert deren genaue Form je nach App-Version variiert. Wir parsen jedes
 *    Feld einzeln und ueberspringen Fehler statt komplette Tage zu verwerfen.
 *
 * Status-Notiz: Der Workout-DETAIL-Endpoint mit GPS-Track + HR-Verlauf ist nicht
 * im Open-Source-Code dokumentiert. Wir holen erstmal die Workout-Liste (Summary)
 * und ergaenzen Detail-Daten (gpsTrackJson, heartRateSeriesJson, paceSeriesJson,
 * splitsJson) sobald der Endpoint via Network-Inspection identifiziert ist.
 */
@Singleton
class AmazfitRepository @Inject constructor(
    private val dailyDao: AmazfitDailyDao,
    private val workoutDao: AmazfitWorkoutDao,
    private val api: ZeppApi,
    private val auth: ZeppAuthService,
    private val secrets: EncryptedSecretsStore,
) {

    fun observeLatestDaily(): Flow<AmazfitDailyEntity?> = dailyDao.getLatest()
    fun observeAllDaily(): Flow<List<AmazfitDailyEntity>> = dailyDao.getAll()
    fun observeDailyRange(from: Long, to: Long): Flow<List<AmazfitDailyEntity>> =
        dailyDao.getRange(from, to)

    fun observeAllWorkouts(): Flow<List<AmazfitWorkoutEntity>> = workoutDao.observeAll()
    fun observeWorkoutsByDate(dateKey: String): Flow<List<AmazfitWorkoutEntity>> =
        workoutDao.observeByDateKey(dateKey)
    fun observeWorkoutById(trackId: String): Flow<AmazfitWorkoutEntity?> =
        workoutDao.observeById(trackId)

    /** Manueller Auslöser: synchronisiert die letzten [days] Tage. */
    suspend fun syncLastDays(days: Int = 365): Result<Int> = runCatching {
        var appToken = auth.freshAppToken()
            ?: throw IllegalStateException("Kein Zepp-App-Token — bitte erneut anmelden.")
        val userId = secrets.zeppUserId
            ?: throw IllegalStateException("Keine Zepp-User-ID — bitte erneut anmelden.")
        val region = secrets.zeppRegion ?: "de2"

        // Daily-Daten holen
        val today = LocalDate.now()
        val from = today.minusDays(days.toLong())
        val dailyEntities = mutableListOf<AmazfitDailyEntity>()

        try {
            dailyEntities += fetchDailyRange(region, appToken, userId, from, today)
        } catch (t: Throwable) {
            // Bei 401/403 versuchen Re-Login + 1x Retry
            Log.w(TAG, "Daily-Fetch fehlgeschlagen, versuche Re-Login: ${t.message}")
            if (auth.reloginIfPossible()) {
                appToken = secrets.zeppAppToken!!
                dailyEntities += fetchDailyRange(region, appToken, userId, from, today)
            } else {
                throw t
            }
        }

        if (dailyEntities.isNotEmpty()) {
            dailyDao.upsertAll(dailyEntities)
        }

        // Workouts holen (Summary)
        val workoutEntities = try {
            fetchWorkoutSummaries(region, appToken, userId, from, today)
        } catch (t: Throwable) {
            Log.w(TAG, "Workout-Fetch fehlgeschlagen: ${t.message}")
            emptyList()
        }
        if (workoutEntities.isNotEmpty()) {
            workoutDao.upsertAll(workoutEntities)
        }

        secrets.zeppLastSyncEpochMs = System.currentTimeMillis()
        Log.i(
            TAG,
            "Amazfit-Sync: ${dailyEntities.size} Daily-Eintraege + ${workoutEntities.size} Workouts geschrieben",
        )
        dailyEntities.size + workoutEntities.size
    }.onFailure { Log.e(TAG, "Amazfit-Sync fehlgeschlagen", it) }

    /* =========================== Fetcher =========================== */

    private suspend fun fetchDailyRange(
        region: String,
        appToken: String,
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<AmazfitDailyEntity> {
        val params = baseParams(userId).toMutableMap().apply {
            put("from_date", from.toString())
            put("to_date", to.toString())
            put("query_type", "summary")
        }
        val resp = api.bandData(
            url = ZeppEndpoints.bandDataUrl(region),
            headers = ZeppEndpoints.dataHeaders(appToken, UUID.randomUUID().toString()),
            params = params,
        )
        if (resp.code != null && resp.code != 1) {
            Log.w(TAG, "bandData lieferte code=${resp.code} (${resp.message})")
            return emptyList()
        }
        return resp.data.mapNotNull { day ->
            val date = day.date ?: return@mapNotNull null
            val capturedAt = parseDateAtMidnight(date) ?: return@mapNotNull null
            val parsed = parseSummaryJson(day.summary)
            AmazfitDailyEntity(
                date = date,
                capturedAt = capturedAt,
                steps = parsed["steps"]?.intOrNull(),
                distanceMeters = parsed["distance"]?.doubleOrNull(),
                activeCalories = parsed["calories"]?.doubleOrNull(),
                activeMinutes = parsed["active_minutes"]?.intOrNull(),
                averageHeartRate = parsed["avg_hr"]?.intOrNull(),
                restingHeartRate = parsed["resting_hr"]?.intOrNull(),
                createdAt = System.currentTimeMillis(),
            )
        }
    }

    private suspend fun fetchWorkoutSummaries(
        region: String,
        appToken: String,
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ): List<AmazfitWorkoutEntity> {
        val params = baseParams(userId).toMutableMap().apply {
            put("from_track_id", "0")
            put("source", "all")
            put("from_date", from.toString())
            put("to_date", to.toString())
        }
        val resp = api.workoutHistory(
            url = ZeppEndpoints.sportHistoryUrl(region),
            headers = ZeppEndpoints.dataHeaders(appToken, UUID.randomUUID().toString()),
            params = params,
        )
        val items = resp.data?.summary ?: return emptyList()
        return items.mapNotNull { s ->
            val trackId = s.trackId ?: return@mapNotNull null
            val end = s.endTime ?: return@mapNotNull null
            val duration = s.durationSeconds ?: return@mapNotNull null
            val endMs = end * 1000L
            val startMs = endMs - duration * 1000L
            val dateKey = Instant.ofEpochMilli(startMs)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            AmazfitWorkoutEntity(
                trackId = trackId,
                dateKey = dateKey,
                startMs = startMs,
                endMs = endMs,
                durationSeconds = duration,
                sportType = s.type,
                sportName = AmazfitSportNames.nameOf(s.type),
                distanceMeters = s.distanceMeters,
                avgPaceSecPerKm = s.avgPace,
                maxPaceSecPerKm = s.maxPace,
                avgSpeedKmh = s.avgSpeed,
                maxSpeedKmh = null,
                calories = s.calories,
                avgHeartRate = s.avgHr,
                maxHeartRate = s.maxHr,
                createdAt = System.currentTimeMillis(),
            )
        }
    }

    /* =========================== Helpers =========================== */

    private fun baseParams(userId: String): Map<String, String> = mapOf(
        "userid" to userId,
        "appid" to UUID.randomUUID().mostSignificantBits.toULong().toString(),
        "channel" to ZeppEndpoints.CHANNEL,
        "country" to "DE",
        "cv" to "${ZeppEndpoints.APP_BUILD}_${ZeppEndpoints.APP_VERSION}",
        "device" to "android_32",
        "device_type" to "android_phone",
        "lang" to "de_DE",
        "timezone" to "Europe/Berlin",
        "v" to "2.0",
    )

    /** Parst ein base64-kodiertes Summary-JSON in eine flache Key-Value-Map. */
    private fun parseSummaryJson(base64Or: String?): Map<String, JsonElement> {
        if (base64Or.isNullOrBlank()) return emptyMap()
        return runCatching {
            val raw = if (base64Or.startsWith("{")) {
                base64Or
            } else {
                String(android.util.Base64.decode(base64Or, android.util.Base64.DEFAULT))
            }
            val parsed = JSON.parseToJsonElement(raw)
            if (parsed is JsonObject) flattenJsonObject(parsed) else emptyMap()
        }.getOrElse { emptyMap() }
    }

    /** Rekursiv alle Primitives einer verschachtelten JSON-Struktur einsammeln. */
    private fun flattenJsonObject(obj: JsonObject, prefix: String = ""): Map<String, JsonElement> {
        val out = mutableMapOf<String, JsonElement>()
        for ((k, v) in obj) {
            val key = if (prefix.isEmpty()) k else "${prefix}_$k"
            if (v is JsonObject) {
                out += flattenJsonObject(v, key)
            } else {
                out[key] = v
                // Auch unter dem unqualifizierten Namen ablegen, damit Lookups
                // wie parsed["steps"] auch dann funktionieren wenn der Wert in
                // einem Sub-Objekt sitzt (typischerweise "stp.ttl" oder "step.total").
                if (k !in out) out[k] = v
            }
        }
        return out
    }

    private fun parseDateAtMidnight(date: String): Long? = runCatching {
        LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()

    private fun JsonElement.intOrNull(): Int? =
        runCatching { jsonPrimitive.intOrNull }.getOrNull()

    private fun JsonElement.doubleOrNull(): Double? =
        runCatching { jsonPrimitive.doubleOrNull }.getOrNull()

    @Suppress("unused")
    private fun JsonElement.stringOrNull(): String? =
        runCatching { jsonPrimitive.contentOrNull }.getOrNull()

    companion object {
        private const val TAG = "AmazfitRepository"
        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}

/**
 * Mapping von Zepp-Sportart-Codes auf deutsche Namen.
 * Quelle: Community-Reverse-Engineering der Zepp-App. Unbekannte Codes werden als
 * "Sport (Code N)" zurueckgegeben damit nichts verloren geht.
 */
internal object AmazfitSportNames {
    private val MAP = mapOf(
        1 to "Laufen draussen",
        2 to "Walking",
        3 to "Wandern",
        4 to "Bergsteigen",
        5 to "Trail Running",
        6 to "Radfahren draussen",
        7 to "Indoor Cycling",
        8 to "Laufband",
        9 to "Schwimmen Pool",
        10 to "Schwimmen Freiwasser",
        11 to "Klettern",
        12 to "Bouldern",
        13 to "Skifahren",
        14 to "Snowboarden",
        15 to "Eislaufen",
        16 to "Yoga",
        17 to "Pilates",
        18 to "Tanzen",
        19 to "Krafttraining",
        20 to "Gymnastik",
        21 to "HIIT",
        22 to "Crosstrainer",
        23 to "Rudergeraet",
        24 to "Stepper",
        25 to "Fussball",
        26 to "Basketball",
        27 to "Volleyball",
        28 to "Tennis",
        29 to "Tischtennis",
        30 to "Badminton",
        31 to "Squash",
        32 to "Golf",
        33 to "Boxen",
        34 to "Kampfsport",
        35 to "Frei",
        36 to "Aerobic",
        37 to "Stand-up-Paddling",
        38 to "Kayaking",
        39 to "Rudern draussen",
        40 to "Reiten",
        41 to "Fechten",
        42 to "Tauchen",
        43 to "Free Diving",
        44 to "Surfen",
        45 to "Skateboarding",
        46 to "Inlineskaten",
        47 to "Triathlon",
        48 to "Parkour",
        49 to "Bowling",
        50 to "Darts",
        51 to "Frisbee",
        52 to "Curling",
        53 to "Ski-Touring",
        54 to "Schneeschuhwandern",
        55 to "Bergradfahren",
        60 to "Stretching",
    )

    fun nameOf(type: Int?): String =
        if (type == null) "Unbekannt" else MAP[type] ?: "Sport (Code $type)"
}
