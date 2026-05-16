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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedInputStream
import java.time.Instant
import java.time.ZoneId
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Importer fuer Polar-Flow-Bulk-Export (ZIP).
 *
 * Frank-Wunsch 2026-05-16: gesamte 10-Jahre-Trainings-Historie aus Polar
 * laden. Polar liefert sie per Mail als ZIP — diese Klasse streamt den
 * Inhalt und konvertiert jede Trainings-JSON-Datei in eine
 * AmazfitWorkoutEntity.
 *
 * Streaming-Strategie:
 *  - ContentResolver.openInputStream(uri) → BufferedInputStream → ZipInputStream
 *  - Pro ZipEntry wird der Inhalt nur dann gelesen wenn der Name auf
 *    "training/training-session-*.json" passt — Schlaf/Aktivitaet/Profil etc.
 *    werden uebersprungen
 *  - Komplette ZIP wird NIE entpackt — nur einzelne Trainings im Speicher
 *  - Bei 3000 Trainings a 40 KB sind das ~120 MB unkomprimiert — kein Problem
 *    wenn pro Eintrag entpackt wird, aber NICHT auf einmal
 *
 * Idempotenz: trackId-Schema "polar-{id}" identisch zur Live-API. Wenn ein
 * Training spaeter auch ueber die Live-API kommt, gewinnt der spaetere upsert
 * (REPLACE-Strategie) — meistens haben beide Quellen identische Daten.
 *
 * Robustheit: Bei JSON-Parser-Fehler einer einzelnen Datei wird sie
 * uebersprungen, der Rest des Imports laeuft weiter. Logcat zeigt
 * uebersprungene Eintraege.
 */
@Singleton
class PolarBulkImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Callback fuer Progress-Updates aus dem laufenden Import.
     * filesProcessed = Anzahl Trainings-JSONs bisher gelesen (auch geskippte).
     * entitiesParsed = Anzahl erfolgreich konvertierter Entities.
     * skipped = Anzahl uebersprungener (Parse-Fehler oder kein Training).
     */
    data class Progress(
        val filesProcessed: Int,
        val entitiesParsed: Int,
        val skipped: Int,
        val currentFileName: String? = null,
        val finished: Boolean = false,
    )

    /**
     * Importiert ALLE Trainings aus der ZIP. Liefert eine Liste der erfolgreich
     * geparsten Entities zurueck — der Aufrufer schreibt sie in die DB.
     *
     * Bei einer 500-MB-ZIP mit 3000 Trainings dauert das auf einem S23 Ultra
     * ca. 30-60 Sekunden. Wir geben den Aufrufer pro 50 Entries einen
     * Progress-Callback damit eine Notification updaten kann.
     */
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
                    // Diagnose: erste 5 Eintraege loggen damit wir die
                    // ZIP-Struktur live sehen koennen
                    if (entriesSeen <= 10) {
                        Log.d(TAG, "Polar-Bulk: ZIP-Eintrag #$entriesSeen: '$name' (dir=${entry.isDirectory})")
                    }
                    if (!entry.isDirectory && isTrainingEntry(name)) {
                        trainingEntriesSeen++
                        filesProcessed++
                        try {
                            // Datei vollstaendig in den Speicher lesen — ein
                            // einzelner Eintrag ist max ein paar hundert KB.
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
                            Log.w(TAG, "Polar-Bulk: Datei $name konnte nicht geparst werden — ${t.message}")
                        }

                        // Progress alle 50 Eintraege oder bei kleinen Imports
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
        Log.i(TAG, "Polar-Bulk-Import fertig: ZIP hatte $entriesSeen Eintraege gesamt, $trainingEntriesSeen davon Trainings — $filesProcessed verarbeitet, ${entities.size} entities erzeugt, $skipped uebersprungen")
        entities
    }

    /** Erkennt eine Trainings-Session-Datei im ZIP. */
    private fun isTrainingEntry(name: String): Boolean {
        // Aktuelles Schema: training/training-session-YYYY-MM-DD-{id}-{uuid}.json
        // Defensiv: auch tolerieren falls Polar das Schema je aendert
        val lc = name.lowercase()
        return lc.endsWith(".json") &&
            (lc.contains("training-session") || lc.contains("/training/"))
    }

    /**
     * Konvertiert eine PolarBulkSession in eine AmazfitWorkoutEntity.
     *
     * Multi-Sport (Triathlon, Duathlon): exercises[] hat > 1 Eintrag. Aktuell
     * nutzen wir die ERSTE Exercise + summieren Distanz/Kalorien aus dem
     * Session-Header (Polar liefert die Summen schon auf Session-Ebene).
     * Spaetere Verbesserung: pro Exercise eine eigene Workout-Zeile.
     */
    private fun sessionToEntity(session: PolarBulkSession): AmazfitWorkoutEntity? {
        if (session.exercises.isEmpty()) return null
        val exercise = session.exercises.first()

        val startEpochMs = PolarSampleMapper.parseStartTimeToEpochMs(
            session.startTime,
            session.startTimeUtcOffset,
        ) ?: return null

        // Dauer entweder aus Session-Header oder aus erster Exercise
        val durationStr = session.duration ?: exercise.duration
        val durationSeconds = PolarSampleMapper.parseIsoDurationToSeconds(durationStr)
        val endEpochMs = startEpochMs + (durationSeconds ?: 0L) * 1000L
        val dateKey = Instant.ofEpochMilli(startEpochMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().toString()

        // Sample-Streams konvertieren — Index-basiert via sampleTypes
        val hrIndex = exercise.sampleTypes.indexOf(PolarBulkSampleType.HEART_RATE)
        val speedIndex = exercise.sampleTypes.indexOf(PolarBulkSampleType.SPEED)
        val paceIndex = exercise.sampleTypes.indexOf(PolarBulkSampleType.PACE)
        val cadenceIndex = exercise.sampleTypes.indexOf(PolarBulkSampleType.CADENCE)
        val altitudeIndex = exercise.sampleTypes.indexOf(PolarBulkSampleType.ALTITUDE)

        val hrJson = if (hrIndex >= 0 && exercise.samples.isNotEmpty()) {
            buildHeartRateJson(exercise.samples, hrIndex, startEpochMs)
        } else null

        // Pace bevorzugt nehmen, sonst aus Speed berechnen
        val paceJson = when {
            paceIndex >= 0 && exercise.samples.isNotEmpty() ->
                buildPaceJsonFromPaceSamples(exercise.samples, paceIndex, startEpochMs)
            speedIndex >= 0 && exercise.samples.isNotEmpty() ->
                buildPaceJsonFromSpeedSamples(exercise.samples, speedIndex, startEpochMs)
            else -> null
        }
        val maxPaceSecPerKm = if (speedIndex >= 0 && exercise.samples.isNotEmpty()) {
            maxPaceFromSpeedSamples(exercise.samples, speedIndex)
        } else null

        val cadence = if (cadenceIndex >= 0 && exercise.samples.isNotEmpty()) {
            avgIntFromSamples(exercise.samples, cadenceIndex)
        } else null

        val altitudeGain = if (altitudeIndex >= 0 && exercise.samples.isNotEmpty()) {
            altitudeGainFromSamples(exercise.samples, altitudeIndex)
        } else null

        // GPS-Track im AmazfitEntity-Format: [[lat, lon, alt, ts], ...]
        val gpsJson = if (exercise.recordedRoute.isNotEmpty()) {
            buildGpsJson(exercise.recordedRoute)
        } else null

        val distance = (session.distance ?: exercise.distance)?.toDouble()
        val calories = (session.calories ?: exercise.calories)?.toDouble()
        val avgPace = PolarSampleMapper.computeAvgPaceSecPerKm(distance, durationSeconds)
        val avgSpeedKmh = PolarSampleMapper.computeAvgSpeedKmh(distance, durationSeconds)
        val vo2Max = session.runningIndex?.toDouble()
        val trainEffectAerobic = session.trainingLoadPro?.cardioLoad?.let { it / 2.0 }
        val trainEffectAnaerobic = session.trainingLoadPro?.muscleLoad?.let { it / 2.0 }

        val heartRateSummary = session.heartRate ?: exercise.heartRate

        return AmazfitWorkoutEntity(
            trackId = "polar-${session.id}",
            dateKey = dateKey,
            startMs = startEpochMs,
            endMs = endEpochMs,
            durationSeconds = durationSeconds,
            sportType = PolarSampleMapper.mapSportToHealthConnectType(
                exercise.sport,
                exercise.detailedSportInfo,
            ),
            sportName = PolarSampleMapper.mapSportToGerman(
                exercise.sport,
                exercise.detailedSportInfo,
            ),
            distanceMeters = distance,
            avgPaceSecPerKm = avgPace,
            maxPaceSecPerKm = maxPaceSecPerKm,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = null,
            calories = calories,
            avgHeartRate = heartRateSummary?.average,
            maxHeartRate = heartRateSummary?.maximum,
            gpsTrackJson = gpsJson,
            heartRateSeriesJson = hrJson,
            paceSeriesJson = null,
            splitsJson = null,
            altitudeGainMeters = altitudeGain,
            altitudeLossMeters = null,
            trainingEffectAerobic = trainEffectAerobic,
            trainingEffectAnaerobic = trainEffectAnaerobic,
            vo2Max = vo2Max,
            cadence = cadence,
            strideLengthCm = null,
            recoveryTimeHours = null,
            skinTempCelsius = null,
            swolf = null,
            poolLaps = null,
            poolLengthMeters = null,
            source = "polar-bulk",
            city = null,
            paceStreamJson = paceJson,
            createdAt = System.currentTimeMillis(),
        )
    }

    /* ================ Stream-Konvertierungen ================ */

    /** HR-Stream nach `[[ts, hr], ...]`. Werte <= 0 werden uebersprungen. */
    private fun buildHeartRateJson(
        samples: List<PolarBulkSample>,
        hrIndex: Int,
        sessionStartMs: Long,
    ): String {
        val arr = buildJsonArray {
            samples.forEach { sample ->
                val ts = sample.timestamp ?: return@forEach
                val v = sample.values?.getOrNull(hrIndex) ?: return@forEach
                val hr = v.jsonPrimitive.intOrNull
                if (hr != null && hr > 0) {
                    addJsonArray {
                        add(JsonPrimitive(ts))
                        add(JsonPrimitive(hr))
                    }
                }
            }
            // Wenn die Sample-Timestamps fehlen (was sehr selten passieren kann),
            // ist die Liste am Ende leer — Aufrufer setzt dann auf null
        }
        return JSON.encodeToString(JsonArray.serializer(), arr)
    }

    /** Pace direkt aus PACE-Samples (Sekunden pro km). */
    private fun buildPaceJsonFromPaceSamples(
        samples: List<PolarBulkSample>,
        paceIndex: Int,
        sessionStartMs: Long,
    ): String {
        val arr = buildJsonArray {
            samples.forEach { sample ->
                val ts = sample.timestamp ?: return@forEach
                val v = sample.values?.getOrNull(paceIndex) ?: return@forEach
                val paceSec = v.jsonPrimitive.doubleOrNull
                addJsonArray {
                    add(JsonPrimitive(ts))
                    if (paceSec != null && paceSec > 0.0) {
                        add(JsonPrimitive(paceSec))
                    } else {
                        add(JsonPrimitive(null as String?))
                    }
                }
            }
        }
        return JSON.encodeToString(JsonArray.serializer(), arr)
    }

    /** Pace aus Speed (km/h) berechnen: 3600 / kmh. */
    private fun buildPaceJsonFromSpeedSamples(
        samples: List<PolarBulkSample>,
        speedIndex: Int,
        sessionStartMs: Long,
    ): String {
        val arr = buildJsonArray {
            samples.forEach { sample ->
                val ts = sample.timestamp ?: return@forEach
                val v = sample.values?.getOrNull(speedIndex) ?: return@forEach
                val kmh = v.jsonPrimitive.doubleOrNull
                addJsonArray {
                    add(JsonPrimitive(ts))
                    if (kmh != null && kmh > 0.0) {
                        add(JsonPrimitive(3600.0 / kmh))
                    } else {
                        add(JsonPrimitive(null as String?))
                    }
                }
            }
        }
        return JSON.encodeToString(JsonArray.serializer(), arr)
    }

    /** Schnellste Sekunde im Speed-Stream als Pace zurueckgeben. */
    private fun maxPaceFromSpeedSamples(samples: List<PolarBulkSample>, speedIndex: Int): Double? {
        var maxKmh = 0.0
        samples.forEach { sample ->
            val kmh = sample.values?.getOrNull(speedIndex)?.jsonPrimitive?.doubleOrNull ?: return@forEach
            if (kmh > maxKmh) maxKmh = kmh
        }
        return if (maxKmh > 0.0) 3600.0 / maxKmh else null
    }

    /** Durchschnittswert eines Samples als Int. */
    private fun avgIntFromSamples(samples: List<PolarBulkSample>, valueIndex: Int): Int? {
        var sum = 0.0
        var count = 0
        samples.forEach { sample ->
            val v = sample.values?.getOrNull(valueIndex)?.jsonPrimitive?.doubleOrNull ?: return@forEach
            if (v > 0.0) {
                sum += v
                count++
            }
        }
        return if (count > 0) (sum / count).toInt() else null
    }

    /** Hoehengewinn aus Altitude-Stream (alle positiven Delta aufsummiert). */
    private fun altitudeGainFromSamples(samples: List<PolarBulkSample>, altIndex: Int): Double? {
        var gain = 0.0
        var last: Double? = null
        samples.forEach { sample ->
            val v = sample.values?.getOrNull(altIndex)?.jsonPrimitive?.doubleOrNull ?: return@forEach
            if (last != null) {
                val delta = v - last!!
                if (delta > 0.0) gain += delta
            }
            last = v
        }
        return if (gain > 0.0) gain else null
    }

    /** GPS-Track in AmazfitWorkoutEntity-Format: `[[lat, lon, alt, ts], ...]`. */
    private fun buildGpsJson(route: List<PolarBulkRoutePoint>): String {
        val arr = buildJsonArray {
            route.forEach { p ->
                val lat = p.latitude ?: return@forEach
                val lon = p.longitude ?: return@forEach
                val ts = p.timestamp ?: return@forEach
                addJsonArray {
                    add(JsonPrimitive(lat))
                    add(JsonPrimitive(lon))
                    add(JsonPrimitive(p.altitude ?: 0.0))
                    add(JsonPrimitive(ts))
                }
            }
        }
        return JSON.encodeToString(JsonArray.serializer(), arr)
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
