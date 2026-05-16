package de.frank.entropyreducer.data.remote.polar

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedInputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Polar-Flow-Bulk-Export Importer (Iteration 8 — Vollausbau).
 *
 * Aus Frank's echter ZIP (96 MB, 956 Trainings, Outdoor mit Polar Vantage V3)
 * habe ich folgendes Format ermittelt:
 *
 * Top-Level (camelCase):
 *  - identifier.id (String), startTime, stopTime, durationMillis (Long)
 *  - calories, hrAvg, hrMax (alle Int)
 *  - timezoneOffsetMinutes
 *  - sport.id (String), product.modelName
 *  - trainingLoadReport.cardioLoad/muscleLoad (Double)
 *
 * exercises[0]:
 *  - statistics.statistics[]  -> Min/Avg/Max pro Metrik
 *  - samples.samples[]        -> typed Streams (HR, SPEED, DISTANCE, ...)
 *  - routes.route.wayPoints[] -> GPS-Punkte
 *
 * samples.samples[] Beispiel-Typen aus Frank's Trainings:
 *  HEART_RATE, SPEED, DISTANCE, CADENCE, ALTITUDE, TEMPERATURE,
 *  STRIDE_LENGTH, LEFT_CRANK_CURRENT_POWER
 *
 * Werte sind teilweise "NaN" als String (Indoor-Training ohne GPS) — wir
 * filtern die heraus.
 *
 * VOLL-Extraktion in dieser Iteration:
 *  - Distanz (letzter non-NaN DISTANCE-Wert)
 *  - Avg/Max-Speed (aus SPEED-Stream)
 *  - Avg/Max-Pace (3600/avgSpeed)
 *  - Avg/Max-HR (Top-Level + statistics-Fallback)
 *  - Pulsverlauf (heartRateSeriesJson)
 *  - Pace-Verlauf (paceStreamJson)
 *  - Hoehengewinn + -verlust (aus ALTITUDE-Stream)
 *  - Cadence (Mittelwert)
 *  - GPS-Track (routes.route.wayPoints -> gpsTrackJson)
 *  - Trainingseffekt aerob+anaerob (trainingLoadReport)
 *  - Geraete-Modell (product.modelName -> city-Feld)
 */
@Singleton
class PolarBulkImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class Progress(
        val filesProcessed: Int,
        val entitiesParsed: Int,
        val skipped: Int,
        val currentFileName: String? = null,
        val finished: Boolean = false,
    )

    suspend fun import(
        zipUri: Uri,
        onProgress: (Progress) -> Unit = {},
    ): List<AmazfitWorkoutEntity> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Polar-Bulk-Importer: starte Import von $zipUri")
        val entities = mutableListOf<AmazfitWorkoutEntity>()
        var filesProcessed = 0
        var skipped = 0
        var entriesSeen = 0
        var trainingEntriesSeen = 0

        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(zipUri)
            ?: throw IllegalStateException("Konnte ZIP-Datei nicht oeffnen: $zipUri")

        BufferedInputStream(inputStream, 65536).use { buf ->
            ZipInputStream(buf).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    entriesSeen++
                    val name = entry.name
                    if (!entry.isDirectory && isTrainingEntry(name)) {
                        trainingEntriesSeen++
                        filesProcessed++
                        try {
                            val bytes = zip.readBytes()
                            val json = bytes.toString(Charsets.UTF_8)
                            val session = JSON.decodeFromString(PolarBulkSession.serializer(), json)
                            val entity = sessionToEntity(session)
                            if (entity != null) {
                                entities += entity
                            } else {
                                skipped++
                            }
                        } catch (ce: kotlinx.coroutines.CancellationException) {
                            throw ce
                        } catch (t: Throwable) {
                            skipped++
                            if (skipped <= 3) {
                                Log.w(TAG, "Polar-Bulk: Datei $name konnte nicht geparst werden — ${t.message}")
                            }
                        }
                        if (filesProcessed % 50 == 0 || filesProcessed < 50) {
                            onProgress(Progress(filesProcessed, entities.size, skipped, name))
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        onProgress(Progress(filesProcessed, entities.size, skipped, finished = true))
        Log.i(TAG, "Polar-Bulk-Import fertig: $entriesSeen Eintraege gesamt, $trainingEntriesSeen Trainings — ${entities.size} entities, $skipped uebersprungen")
        entities
    }

    private fun isTrainingEntry(name: String): Boolean {
        val lc = name.lowercase()
        return lc.endsWith(".json") &&
            (lc.contains("training-session") || lc.contains("/training/"))
    }

    private fun sessionToEntity(session: PolarBulkSession): AmazfitWorkoutEntity? {
        val sessionId = session.identifier?.id ?: return null
        val startTimeStr = session.startTime ?: return null
        val offset = session.timezoneOffsetMinutes ?: 0

        val startEpochMs = parseStartTimeToEpochMs(startTimeStr, offset) ?: return null
        val durationSeconds = session.durationMillis?.let { it / 1000L }
        val endEpochMs = startEpochMs + (durationSeconds ?: 0L) * 1000L
        val dateKey = Instant.ofEpochMilli(startEpochMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().toString()

        // Streams aus exercises[0].samples.samples[] by type extrahieren
        val firstExercise = session.exercises.firstOrNull()
        val sampleEntries = firstExercise?.samples?.samples ?: emptyList()
        val hrStream = sampleEntries.firstOrNull { it.type == "HEART_RATE" }
        val speedStream = sampleEntries.firstOrNull { it.type == "SPEED" }
        val distanceStream = sampleEntries.firstOrNull { it.type == "DISTANCE" }
        val altitudeStream = sampleEntries.firstOrNull { it.type == "ALTITUDE" }
        val cadenceStream = sampleEntries.firstOrNull { it.type == "CADENCE" }
        val strideStream = sampleEntries.firstOrNull { it.type == "STRIDE_LENGTH" }

        // Avg/Max-HR mit Fallback aus statistics
        val statsHr = firstExercise?.statistics?.statistics?.firstOrNull {
            it.type == "STATISTICS_TYPE_HEART_RATE"
        }
        val avgHr = session.hrAvg ?: statsHr?.avg?.toInt()
        val maxHr = session.hrMax ?: statsHr?.max?.toInt() ?: hrStream?.let { maxIntFromStream(it) }

        // DISTANZ: letzter non-NaN-Wert ist die Gesamtdistanz in Metern
        val distanceMeters = distanceStream?.let { lastFiniteValue(it) }

        // Frank-Live-Sonde 2026-05-16: Polar's SPEED-Stream-Werte sind BEREITS
        // in km/h — NICHT in m/s wie urspruenglich angenommen. Beweis: 50-min-
        // Outdoor-Lauf mit 6.5 km zeigte SPEED-Werte 6.3-7.6 (passt zu km/h
        // Joggen, nicht m/s das waere 23-27 km/h Sprint). Kein *3.6 mehr.
        val speedValues = speedStream?.let { extractFiniteDoubles(it) } ?: emptyList()
        val positiveSpeeds = speedValues.filter { it > 0.0 }
        val avgSpeedKmh = positiveSpeeds.takeIf { it.isNotEmpty() }?.average()
        val maxSpeedKmh = positiveSpeeds.maxOrNull()
        // Avg-Pace bevorzugt aus Distanz+Dauer (verlaesslicher als Speed-Stream-Avg)
        val avgPaceSecPerKm = if (distanceMeters != null && distanceMeters > 0.0 &&
            durationSeconds != null && durationSeconds > 0L) {
            durationSeconds.toDouble() / (distanceMeters / 1000.0)
        } else avgSpeedKmh?.let { 3600.0 / it }
        // Max-Pace = schnellster Wert in sec/km — bei schnellster km/h-
        // Geschwindigkeit. 20 km/h → 180 sec/km (3:00 min/km).
        val maxPaceSecPerKm = maxSpeedKmh?.takeIf { it > 0.5 }?.let { 3600.0 / it }

        // Hoehengewinn/-verlust aus ALTITUDE
        val altValues = altitudeStream?.let { extractFiniteDoubles(it) } ?: emptyList()
        var altGain = 0.0
        var altLoss = 0.0
        if (altValues.size >= 2) {
            for (i in 1 until altValues.size) {
                val delta = altValues[i] - altValues[i - 1]
                if (delta > 0.0) altGain += delta else altLoss += -delta
            }
        }

        // Cadence-Mittelwert
        val cadenceValues = cadenceStream?.let { extractFiniteDoubles(it) }?.filter { it > 0.0 }
        val cadenceAvg = cadenceValues?.takeIf { it.isNotEmpty() }?.average()?.toInt()

        // Stride-Length-Mittelwert in cm
        val strideValues = strideStream?.let { extractFiniteDoubles(it) }?.filter { it > 0.0 }
        val strideAvgCm = strideValues?.takeIf { it.isNotEmpty() }?.average()?.toInt()

        // Streams als JSON fuer Charts
        val heartRateSeriesJson = hrStream?.let { buildHrSeriesJson(it, startEpochMs) }
        val paceStreamJson = speedStream?.let { buildPaceStreamJson(it, startEpochMs) }

        // GPS-Track aus routes.route.wayPoints
        val wayPoints = firstExercise?.routes?.route?.wayPoints ?: emptyList()
        val gpsTrackJson = if (wayPoints.isNotEmpty()) {
            buildGpsTrackJson(wayPoints, startEpochMs)
        } else null

        val calories = session.calories?.toDouble()
        val trainEffectAerobic = session.trainingLoadReport?.cardioLoad?.let { it / 2.0 }
        val trainEffectAnaerobic = session.trainingLoadReport?.muscleLoad?.let { it / 2.0 }
        val sportId = session.sport?.id ?: firstExercise?.sport?.id
        val deviceModel = session.product?.modelName

        return AmazfitWorkoutEntity(
            trackId = "polar-$sessionId",
            dateKey = dateKey,
            startMs = startEpochMs,
            endMs = endEpochMs,
            durationSeconds = durationSeconds,
            sportType = PolarBulkSportMap.toHealthConnectType(sportId),
            sportName = PolarBulkSportMap.nameOf(sportId),
            distanceMeters = distanceMeters,
            avgPaceSecPerKm = avgPaceSecPerKm,
            maxPaceSecPerKm = maxPaceSecPerKm,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            calories = calories,
            avgHeartRate = avgHr,
            maxHeartRate = maxHr,
            gpsTrackJson = gpsTrackJson,
            heartRateSeriesJson = heartRateSeriesJson,
            paceSeriesJson = null,
            splitsJson = null,
            altitudeGainMeters = altGain.takeIf { it > 0.5 },
            altitudeLossMeters = altLoss.takeIf { it > 0.5 },
            trainingEffectAerobic = trainEffectAerobic,
            trainingEffectAnaerobic = trainEffectAnaerobic,
            vo2Max = null,
            cadence = cadenceAvg,
            strideLengthCm = strideAvgCm,
            recoveryTimeHours = null,
            skinTempCelsius = null,
            swolf = null,
            poolLaps = null,
            poolLengthMeters = null,
            source = "polar-bulk",
            city = deviceModel,
            paceStreamJson = paceStreamJson,
            createdAt = System.currentTimeMillis(),
        )
    }

    /**
     * Parst Polar's startTime "2025-07-26T15:38:28" + timezoneOffsetMinutes 120
     * in Unix-Millis.
     */
    private fun parseStartTimeToEpochMs(startTime: String, offsetMinutes: Int): Long? {
        return runCatching {
            val zone = ZoneOffset.ofTotalSeconds(offsetMinutes * 60)
            // startTime kann mit oder ohne ".000"-Suffix kommen
            val parsed = if (startTime.contains('.')) {
                LocalDateTime.parse(startTime.substringBefore('.'))
            } else {
                LocalDateTime.parse(startTime)
            }
            parsed.atOffset(zone).toInstant().toEpochMilli()
        }.getOrNull()
    }

    /**
     * Wandelt einen Sample-Eintrag in eine Liste finiter Double-Werte.
     * "NaN"-Strings und JSON-null werden uebersprungen.
     */
    private fun extractFiniteDoubles(entry: PolarBulkSampleEntry): List<Double> {
        return entry.values.mapNotNull { v -> toFiniteDouble(v) }
    }

    private fun toFiniteDouble(v: JsonElement): Double? {
        if (v is JsonNull) return null
        return runCatching {
            val p = v.jsonPrimitive
            val s = p.content
            if (s == "NaN" || s.equals("nan", ignoreCase = true)) return null
            val d = p.doubleOrNull ?: s.toDoubleOrNull() ?: return null
            if (d.isNaN() || d.isInfinite()) null else d
        }.getOrNull()
    }

    /** Letzter finiter Wert eines Streams — Distanz akkumuliert. */
    private fun lastFiniteValue(entry: PolarBulkSampleEntry): Double? {
        for (i in entry.values.indices.reversed()) {
            val d = toFiniteDouble(entry.values[i])
            if (d != null && d > 0.0) return d
        }
        return null
    }

    private fun maxIntFromStream(entry: PolarBulkSampleEntry): Int? {
        var max = -1.0
        for (v in entry.values) {
            val d = toFiniteDouble(v) ?: continue
            if (d > max) max = d
        }
        return if (max > 0) max.toInt() else null
    }

    /**
     * Heart-Rate-Stream zu JSON `[[ts, hr], ...]`.
     */
    private fun buildHrSeriesJson(entry: PolarBulkSampleEntry, startEpochMs: Long): String? {
        val rate = (entry.intervalMillis ?: 1000L).coerceAtLeast(100L)
        var anyPushed = false
        val arr = buildJsonArray {
            entry.values.forEachIndexed { idx, v ->
                val hr = toFiniteDouble(v)?.toInt() ?: return@forEachIndexed
                if (hr <= 0) return@forEachIndexed
                val ts = startEpochMs + idx.toLong() * rate
                addJsonArray {
                    add(JsonPrimitive(ts))
                    add(JsonPrimitive(hr))
                }
                anyPushed = true
            }
        }
        return if (anyPushed) JSON.encodeToString(JsonArray.serializer(), arr) else null
    }

    /**
     * SPEED-Stream (km/h, Polar-Bulk-Format) -> Pace-Verlauf in Sekunden pro km.
     * Format `[[ts, paceSecPerKm], ...]`.
     *
     * Frank-Korrektur 2026-05-16: SPEED ist km/h, nicht m/s wie ich erst dachte.
     * Pace = 3600 / kmh (nicht 1000/ms).
     */
    private fun buildPaceStreamJson(entry: PolarBulkSampleEntry, startEpochMs: Long): String? {
        val rate = (entry.intervalMillis ?: 1000L).coerceAtLeast(100L)
        var anyPushed = false
        val arr = buildJsonArray {
            entry.values.forEachIndexed { idx, v ->
                val kmh = toFiniteDouble(v) ?: return@forEachIndexed
                val ts = startEpochMs + idx.toLong() * rate
                addJsonArray {
                    add(JsonPrimitive(ts))
                    if (kmh > 0.5) {
                        // km/h -> sec/km : 3600/kmh
                        // Beispiel: 10 km/h -> 360 sec/km = 6:00 min/km
                        add(JsonPrimitive(3600.0 / kmh))
                        anyPushed = true
                    } else {
                        add(JsonPrimitive(null as String?))
                    }
                }
            }
        }
        return if (anyPushed) JSON.encodeToString(JsonArray.serializer(), arr) else null
    }

    /**
     * GPS-Track aus wayPoints im AmazfitEntity-Format `[[lat, lon, alt, ts], ...]`.
     * timestamp = startEpochMs + elapsedMillis.
     */
    private fun buildGpsTrackJson(points: List<PolarBulkWayPoint>, startEpochMs: Long): String? {
        var anyPushed = false
        val arr = buildJsonArray {
            points.forEach { p ->
                val lat = p.latitude ?: return@forEach
                val lon = p.longitude ?: return@forEach
                val elapsed = p.elapsedMillis ?: 0L
                val ts = startEpochMs + elapsed
                addJsonArray {
                    add(JsonPrimitive(lat))
                    add(JsonPrimitive(lon))
                    add(JsonPrimitive(p.altitude ?: 0.0))
                    add(JsonPrimitive(ts))
                }
                anyPushed = true
            }
        }
        return if (anyPushed) JSON.encodeToString(JsonArray.serializer(), arr) else null
    }

    companion object {
        private const val TAG = "PolarBulkImporter"
        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}
