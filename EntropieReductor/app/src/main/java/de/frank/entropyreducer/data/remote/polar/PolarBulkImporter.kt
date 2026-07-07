package de.frank.entropyreducer.data.remote.polar

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import java.io.BufferedInputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
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
        Diag.i(DiagnosticArea.POLAR, TAG, "Polar-Bulk-Importer: starte Import von $zipUri")
        val entities = mutableListOf<AmazfitWorkoutEntity>()
        var filesProcessed = 0
        var skipped = 0
        var entriesSeen = 0
        var trainingEntriesSeen = 0

        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(zipUri)
            ?: throw IllegalStateException("Konnte ZIP-Datei nicht oeffnen: $zipUri")

        // Plus: ist die Eingabe-Datei ITSELF eine TCX-Datei (kein ZIP)?
        // Polar Flow Web exportiert oft einzelne Workouts als .tcx ohne ZIP-Wrapper.
        val maybeTcxDirect = runCatching {
            resolver.openInputStream(zipUri)?.use { stream ->
                val buffer = CharArray(4096)
                stream.bufferedReader().use { reader ->
                    val read = reader.read(buffer, 0, buffer.size)
                    if (read > 0) String(buffer, 0, read) else ""
                }
            }
        }.getOrNull()
        val isDirectTcx = maybeTcxDirect?.let {
            val t = it.trimStart()
            t.startsWith("<?xml") && t.contains("TrainingCenterDatabase")
        } ?: false

        if (isDirectTcx) {
            // Bugfix 2026-07-03: der eingangs geoeffnete ZIP-InputStream wird in diesem Zweig
            // nie konsumiert — schliessen statt leaken (nur der else-Zweig nutzt ihn per use{}).
            try { inputStream.close() } catch (_: java.io.IOException) { /* ignore */ }
            // Datei direkt als TCX-XML laden
            val fullText = resolver.openInputStream(zipUri)?.bufferedReader()?.use { it.readText() }
            if (!fullText.isNullOrBlank()) {
                val entity = parseTcxBytes(fullText, "direct-tcx-input")
                if (entity != null) {
                    entities += entity
                    filesProcessed = 1
                    trainingEntriesSeen = 1
                    Diag.i(DiagnosticArea.POLAR, TAG, "Polar-Bulk: Direkt-TCX-Import erfolgreich")
                }
            }
        } else {
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
                                val lcName = name.lowercase()
                                val entity = when {
                                    lcName.endsWith(".tcx") -> {
                                        // Polar Flow Web Export — TCX-XML
                                        val xml = bytes.toString(Charsets.UTF_8)
                                        parseTcxBytes(xml, name)
                                    }
                                    lcName.endsWith(".gpx") -> {
                                        // Nur GPS-Track — minimale Entity
                                        val xml = bytes.toString(Charsets.UTF_8)
                                        parseGpxBytes(xml, name)
                                    }
                                    else -> {
                                        // Polar's Bulk-Export-JSON
                                        val json = bytes.toString(Charsets.UTF_8)
                                        val session = JSON.decodeFromString(PolarBulkSession.serializer(), json)
                                        sessionToEntity(session)
                                    }
                                }
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
                                    Diag.w(DiagnosticArea.POLAR, TAG, "Polar-Bulk: Datei $name konnte nicht geparst werden — ${t.message}")
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
        }

        onProgress(Progress(filesProcessed, entities.size, skipped, finished = true))
        Diag.i(DiagnosticArea.POLAR, TAG, "Polar-Bulk-Import fertig: $entriesSeen Eintraege gesamt, $trainingEntriesSeen Trainings — ${entities.size} entities, $skipped uebersprungen")
        entities
    }

    private fun isTrainingEntry(name: String): Boolean {
        val lc = name.lowercase()
        // Polar Flow Web Export liefert eine .tcx-Datei pro Workout (manchmal
        // mit GPX-Datei daneben). Polar Bulk Export liefert .json mit
        // training-session.json oder /training/ im Pfad.
        return when {
            lc.endsWith(".tcx") -> true
            lc.endsWith(".gpx") -> true
            lc.endsWith(".json") && (lc.contains("training-session") || lc.contains("/training/")) -> true
            else -> false
        }
    }

    /**
     * Parst TCX-XML zu einer Workout-Entity. Polar Flow Web Export-Format:
     * TrainingCenterDatabase > Activities > Activity > Lap > Track > Trackpoint.
     * Jeder Trackpoint hat Time, Position (LatitudeDegrees/LongitudeDegrees),
     * AltitudeMeters, DistanceMeters, HeartRateBpm, Extensions (Speed/Cadence).
     */
    private fun parseTcxBytes(xml: String, srcName: String): AmazfitWorkoutEntity? {
        return runCatching {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(xml))
            var sport: String? = null
            var totalSeconds: Long? = null
            var totalDistance: Double? = null
            var calories: Int? = null
            var maxHr: Int? = null
            var avgHr: Int? = null
            val hrPairs = mutableListOf<Pair<Long, Int>>()
            val speedPairs = mutableListOf<Pair<Long, Double>>() // km/h
            val altPairs = mutableListOf<Pair<Long, Double>>()
            val cadencePairs = mutableListOf<Pair<Long, Int>>()
            val distPairs = mutableListOf<Pair<Long, Double>>()
            data class Gp(val lat: Double, val lon: Double, val alt: Double, val tsMs: Long)
            val gpsPoints = mutableListOf<Gp>()
            var lat: Double? = null; var lon: Double? = null; var alt: Double? = null
            var time: Long? = null; var hr: Int? = null; var distM: Double? = null
            var cadence: Int? = null; var speedMs: Double? = null
            var currentTag = ""
            var idStr: String? = null
            var startMs = 0L
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (event) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("Activity", true) && sport == null) {
                            sport = parser.getAttributeValue(null, "Sport")
                        }
                        if (currentTag.equals("Trackpoint", true)) {
                            lat = null; lon = null; alt = null; time = null; hr = null
                            distM = null; cadence = null; speedMs = null
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            when (currentTag.lowercase()) {
                                "id" -> if (idStr == null) idStr = text
                                "time" -> time = runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()
                                "latitudedegrees" -> lat = text.toDoubleOrNull()
                                "longitudedegrees" -> lon = text.toDoubleOrNull()
                                "altitudemeters" -> alt = text.toDoubleOrNull()
                                "distancemeters" -> distM = text.toDoubleOrNull()
                                "value" -> hr = text.toIntOrNull()
                                "cadence", "runcadence" -> cadence = text.toIntOrNull()
                                "speed" -> speedMs = text.toDoubleOrNull()
                                "totaltimeseconds" -> totalSeconds = (text.toDoubleOrNull() ?: 0.0).toLong()
                                "calories" -> calories = text.toIntOrNull()
                                "maximumheartratebpm", "maxheartratebpm" -> maxHr = text.toIntOrNull()
                                "averageheartratebpm", "avgheartratebpm" -> avgHr = text.toIntOrNull()
                            }
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name.equals("Trackpoint", true)) {
                            val t = time
                            if (t != null) {
                                if (startMs == 0L) startMs = t
                                val la = lat; val lo = lon
                                if (la != null && lo != null) {
                                    gpsPoints += Gp(la, lo, alt ?: 0.0, t)
                                }
                                if (hr != null) hrPairs += t to hr!!
                                if (alt != null) altPairs += t to alt!!
                                if (distM != null) distPairs += t to distM!!
                                if (cadence != null) cadencePairs += t to cadence!!
                                if (speedMs != null) speedPairs += t to speedMs!! * 3.6
                            }
                        }
                        currentTag = ""
                    }
                }
                event = parser.next()
            }
            if (startMs == 0L) {
                Diag.w(DiagnosticArea.POLAR, TAG, "TCX[$srcName]: kein Trackpoint mit Zeit gefunden")
                return@runCatching null
            }
            if (totalDistance == null && distPairs.isNotEmpty()) totalDistance = distPairs.last().second
            val dateKey = Instant.ofEpochMilli(startMs)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val endMs = (hrPairs.lastOrNull()?.first ?: gpsPoints.lastOrNull()?.tsMs ?: startMs)
            val durSec = totalSeconds ?: ((endMs - startMs) / 1000L).coerceAtLeast(0L)
            Diag.i(DiagnosticArea.POLAR, TAG, "TCX[$srcName]: trackpoints hr=${hrPairs.size} gps=${gpsPoints.size} alt=${altPairs.size} dist=${totalDistance} totalSec=$durSec sport=$sport")

            // Stream-JSONs
            val hrJson = if (hrPairs.isNotEmpty())
                hrPairs.joinToString(",", "[", "]") { "[${it.first},${it.second}]" }
            else null
            val paceStreamJson = if (speedPairs.isNotEmpty()) {
                speedPairs.filter { it.second > 0 }.joinToString(",", "[", "]") {
                    "[${it.first},${3600.0 / it.second}]"
                }
            } else null
            val gpsJson = if (gpsPoints.isNotEmpty())
                gpsPoints.joinToString(",", "[", "]") {
                    "[${it.lat},${it.lon},${it.alt},${it.tsMs}]"
                }
            else null
            val maxSpeed = speedPairs.maxOfOrNull { it.second }
            val maxPace = maxSpeed?.takeIf { it > 0 }?.let { 3600.0 / it }
            val altGain = altPairs.zipWithNext().sumOf { (a, b) ->
                (b.second - a.second).coerceAtLeast(0.0)
            }.takeIf { it > 0.5 }
            val altLoss = altPairs.zipWithNext().sumOf { (a, b) ->
                (-(b.second - a.second)).coerceAtLeast(0.0)
            }.takeIf { it > 0.5 }
            val cadenceAvg = if (cadencePairs.isNotEmpty())
                cadencePairs.filter { it.second > 0 }.map { it.second.toDouble() }.average()
            else null
            // Km-Splits aus dist-Stream
            val splitsJson = if (distPairs.size >= 2) {
                val kmTimes = mutableListOf<Double>()
                var nextKm = 1
                var prevT = distPairs[0].first.toDouble()
                var prevD = distPairs[0].second
                for (i in 1 until distPairs.size) {
                    val (t, d) = distPairs[i]
                    if (d < prevD) { prevD = d; prevT = t.toDouble(); continue }
                    while (d >= nextKm * 1000.0) {
                        val span = d - prevD
                        val frac = if (span > 0) (nextKm * 1000.0 - prevD) / span else 0.0
                        val splitT = prevT + frac * (t.toDouble() - prevT)
                        kmTimes += splitT / 1000.0
                        nextKm++
                    }
                    prevT = t.toDouble()
                    prevD = d
                }
                val splits = mutableListOf<Double>()
                var prev = 0.0
                for (kt in kmTimes) {
                    val split = kt - prev
                    if (split in 120.0..1800.0) splits += split
                    prev = kt
                }
                if (splits.isEmpty()) null else splits.joinToString("|") { "%.1f".format(it).replace(",", ".") }
            } else null

            val computedAvgHr = avgHr ?: if (hrPairs.isNotEmpty()) hrPairs.map { it.second }.average().toInt() else null
            val computedMaxHr = maxHr ?: hrPairs.maxOfOrNull { it.second }
            val avgPace = if (totalDistance != null && totalDistance!! > 0 && durSec > 0)
                durSec / (totalDistance!! / 1000.0) else null
            val avgSpd = if (totalDistance != null && durSec > 0)
                (totalDistance!! / 1000.0) / (durSec / 3600.0) else null

            // VO2max (ACSM Formel)
            val vo2 = if (totalDistance != null && totalDistance!! >= 200.0 && durSec > 60L &&
                computedAvgHr != null && computedAvgHr >= 60) {
                val maxHrFrank = 180; val restHrFrank = 65
                val durationMin = durSec / 60.0
                val speedMperMin = totalDistance!! / durationMin
                val vo2Workout = 0.2 * speedMperMin + 3.5
                val hrReserve = (computedAvgHr - restHrFrank).toDouble() / (maxHrFrank - restHrFrank).toDouble()
                if (hrReserve > 0.1) (vo2Workout / hrReserve + 2.0).takeIf { it in 20.0..80.0 } else null
            } else null

            val strideLengthCm = if (cadenceAvg != null && cadenceAvg > 0.0 &&
                totalDistance != null && totalDistance!! > 0 && durSec > 0) {
                val totalSteps = cadenceAvg * (durSec / 60.0)
                if (totalSteps > 0) {
                    val cm = (totalDistance!! / totalSteps * 100.0).toInt()
                    if (cm in 30..250) cm else null
                } else null
            } else null

            // Sport-Mapping
            val sportName = when (sport?.lowercase()) {
                "running" -> "Laufen"
                "biking", "cycling" -> "Radfahren"
                "walking" -> "Walking"
                "hiking" -> "Wandern"
                "swimming" -> "Schwimmen"
                else -> sport ?: "Training"
            }

            // ID extrahieren — Polar Flow Web TCX hat oft die Polar-ID im Id-Tag
            // ODER aus dem Dateinamen (z.B. "8341167400.tcx")
            val numericIdFromName = srcName.substringAfterLast("/").substringBeforeLast(".")
                .toLongOrNull()
            val numericIdFromId = idStr?.toLongOrNull()
            val finalId = numericIdFromName ?: numericIdFromId ?: startMs

            AmazfitWorkoutEntity(
                trackId = "polar-flow-tcx-$finalId",
                dateKey = dateKey,
                startMs = startMs,
                endMs = startMs + durSec * 1000L,
                durationSeconds = durSec,
                sportType = null, // wir mappen nicht zu Health-Connect-Code
                sportName = sportName,
                distanceMeters = totalDistance,
                avgPaceSecPerKm = avgPace,
                maxPaceSecPerKm = maxPace,
                avgSpeedKmh = avgSpd,
                maxSpeedKmh = maxSpeed,
                calories = calories?.toDouble(),
                avgHeartRate = computedAvgHr,
                maxHeartRate = computedMaxHr,
                gpsTrackJson = gpsJson,
                heartRateSeriesJson = hrJson,
                paceSeriesJson = splitsJson,
                splitsJson = null,
                altitudeGainMeters = altGain,
                altitudeLossMeters = altLoss,
                trainingEffectAerobic = null,
                trainingEffectAnaerobic = null,
                vo2Max = vo2,
                cadence = cadenceAvg?.toInt(),
                strideLengthCm = strideLengthCm,
                recoveryTimeHours = null,
                skinTempCelsius = null,
                swolf = null,
                poolLaps = null,
                poolLengthMeters = null,
                source = "polar-flow-tcx",
                city = null,
                paceStreamJson = paceStreamJson,
                createdAt = System.currentTimeMillis(),
            )
        }.onFailure { ex ->
            Diag.w(DiagnosticArea.POLAR, TAG, "TCX[$srcName] Parse-Fehler: ${ex.message}")
        }.getOrNull()
    }

    /** Minimaler GPX-Parser — nur GPS-Track. */
    private fun parseGpxBytes(xml: String, srcName: String): AmazfitWorkoutEntity? {
        return runCatching {
            val parser = android.util.Xml.newPullParser()
            parser.setInput(java.io.StringReader(xml))
            data class Gp(val lat: Double, val lon: Double, val alt: Double, val ts: Long)
            val pts = mutableListOf<Gp>()
            var lat: Double? = null; var lon: Double? = null; var alt: Double? = null; var ts: Long? = null
            var current = ""
            var event = parser.eventType
            while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                when (event) {
                    org.xmlpull.v1.XmlPullParser.START_TAG -> {
                        current = parser.name
                        if (current.equals("trkpt", true)) {
                            lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                            lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                            alt = null; ts = null
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.TEXT -> {
                        val txt = parser.text?.trim().orEmpty()
                        if (txt.isNotEmpty()) {
                            when (current.lowercase()) {
                                "ele" -> alt = txt.toDoubleOrNull()
                                "time" -> ts = runCatching { Instant.parse(txt).toEpochMilli() }.getOrNull()
                            }
                        }
                    }
                    org.xmlpull.v1.XmlPullParser.END_TAG -> {
                        if (parser.name.equals("trkpt", true)) {
                            val la = lat; val lo = lon
                            if (la != null && lo != null) pts += Gp(la, lo, alt ?: 0.0, ts ?: 0L)
                            lat = null; lon = null; alt = null; ts = null
                        }
                        current = ""
                    }
                }
                event = parser.next()
            }
            if (pts.isEmpty()) return@runCatching null
            val startMs = pts.firstOrNull { it.ts > 0L }?.ts ?: System.currentTimeMillis()
            val dateKey = Instant.ofEpochMilli(startMs)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val gpsJson = pts.joinToString(",", "[", "]") { "[${it.lat},${it.lon},${it.alt},${it.ts}]" }
            val finalId = srcName.substringAfterLast("/").substringBeforeLast(".").toLongOrNull() ?: startMs
            AmazfitWorkoutEntity(
                trackId = "polar-flow-gpx-$finalId",
                dateKey = dateKey,
                startMs = startMs,
                endMs = pts.last().ts.takeIf { it > 0L } ?: startMs,
                durationSeconds = null,
                sportType = null, sportName = "Training",
                distanceMeters = null, avgPaceSecPerKm = null, maxPaceSecPerKm = null,
                avgSpeedKmh = null, maxSpeedKmh = null,
                calories = null, avgHeartRate = null, maxHeartRate = null,
                gpsTrackJson = gpsJson,
                heartRateSeriesJson = null, paceSeriesJson = null, splitsJson = null,
                altitudeGainMeters = null, altitudeLossMeters = null,
                trainingEffectAerobic = null, trainingEffectAnaerobic = null,
                vo2Max = null, cadence = null, strideLengthCm = null,
                recoveryTimeHours = null, skinTempCelsius = null,
                swolf = null, poolLaps = null, poolLengthMeters = null,
                source = "polar-flow-gpx",
                city = null, paceStreamJson = null,
                createdAt = System.currentTimeMillis(),
            )
        }.getOrNull()
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
