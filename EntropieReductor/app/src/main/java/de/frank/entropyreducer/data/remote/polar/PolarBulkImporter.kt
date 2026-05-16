package de.frank.entropyreducer.data.remote.polar

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Importer fuer Polar-Flow-Bulk-Export (ZIP).
 *
 * Frank-Wunsch 2026-05-16 (Iteration 7): nach mehreren Iterationen mit Format-
 * Diskrepanzen habe ich die echte ZIP ausgepackt und das tatsaechliche Format
 * ermittelt. Polar nutzt camelCase-Feldnamen (startTime, durationMillis, hrAvg,
 * hrMax) statt der hyphenated Form aus der Doku.
 *
 * Aktueller Stand: Metadaten-Import.
 *  - 956 Trainings aus Frank's ZIP werden alle importiert
 *  - Sportart via PolarBulkSportMap aus sport.id
 *  - Datum aus startTime + timezoneOffsetMinutes
 *  - Dauer aus durationMillis (in Sekunden umrechnen)
 *  - Avg/Max-HR direkt aus Top-Level (hrAvg/hrMax)
 *  - Kalorien direkt aus Top-Level (calories)
 *  - Training-Load aus trainingLoadReport
 *
 * NICHT-Import (Folge-Iteration):
 *  - Pulsverlauf (Stream)
 *  - GPS-Track
 *  - Pace-Stream
 *  - Splits
 *
 * Idempotenz: trackId "polar-{identifier.id}" — bei Live-API werden dieselben
 * IDs durch REPLACE ueberschrieben.
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
        Log.i(TAG, "Polar-Bulk-Import fertig: ZIP hatte $entriesSeen Eintraege gesamt, $trainingEntriesSeen davon Trainings — $filesProcessed verarbeitet, ${entities.size} entities erzeugt, $skipped uebersprungen")
        entities
    }

    private fun isTrainingEntry(name: String): Boolean {
        val lc = name.lowercase()
        return lc.endsWith(".json") &&
            (lc.contains("training-session") || lc.contains("/training/"))
    }

    /**
     * Konvertiert eine PolarBulkSession in eine AmazfitWorkoutEntity.
     *
     * Echtes Polar-Format (Frank-Live-Sonde 2026-05-16):
     *  - identifier.id            -> trackId "polar-{id}"
     *  - startTime "2025-07-26T15:38:28" + timezoneOffsetMinutes 120 -> startMs
     *  - durationMillis           -> durationSeconds
     *  - hrAvg/hrMax              -> avgHeartRate/maxHeartRate
     *  - calories (Int)           -> calories (Double)
     *  - sport.id                 -> PolarBulkSportMap.nameOf
     *  - trainingLoadReport       -> trainingEffectAerobic (cardio/2), -Anaerobic (muscle/2)
     */
    private fun sessionToEntity(session: PolarBulkSession): AmazfitWorkoutEntity? {
        val sessionId = session.identifier?.id ?: return null
        val startTimeStr = session.startTime ?: return null
        val offset = session.timezoneOffsetMinutes ?: 0

        val startEpochMs = parseStartTimeToEpochMs(startTimeStr, offset) ?: return null

        val durationSeconds = session.durationMillis?.let { it / 1000L }
        val endEpochMs = startEpochMs + (durationSeconds ?: 0L) * 1000L
        val dateKey = Instant.ofEpochMilli(startEpochMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().toString()

        // Falls Top-Level hrAvg/hrMax fehlen, aus statistics.statistics[] fallback'en
        val firstExercise = session.exercises.firstOrNull()
        val statsHr = firstExercise?.statistics?.statistics?.firstOrNull { stat ->
            stat.type == "STATISTICS_TYPE_HEART_RATE"
        }
        val avgHr = session.hrAvg ?: statsHr?.avg?.toInt()
        val maxHr = session.hrMax ?: statsHr?.max?.toInt()

        val calories = session.calories?.toDouble()
        val trainEffectAerobic = session.trainingLoadReport?.cardioLoad?.let { it / 2.0 }
        val trainEffectAnaerobic = session.trainingLoadReport?.muscleLoad?.let { it / 2.0 }

        // Sport-ID kann auf Session-Ebene oder auf Exercise-Ebene stehen.
        val sportId = session.sport?.id ?: firstExercise?.sport?.id

        // Gerete-Modell als Suffix damit Frank sieht woher das Training kam
        val sportName = PolarBulkSportMap.nameOf(sportId)
        val deviceModel = session.product?.modelName

        return AmazfitWorkoutEntity(
            trackId = "polar-$sessionId",
            dateKey = dateKey,
            startMs = startEpochMs,
            endMs = endEpochMs,
            durationSeconds = durationSeconds,
            sportType = PolarBulkSportMap.toHealthConnectType(sportId),
            sportName = sportName,
            distanceMeters = null,  // Polar liefert distance NICHT auf Top-Level — kommt aus zones/samples (Folge-Iteration)
            avgPaceSecPerKm = null,
            maxPaceSecPerKm = null,
            avgSpeedKmh = null,
            maxSpeedKmh = null,
            calories = calories,
            avgHeartRate = avgHr,
            maxHeartRate = maxHr,
            gpsTrackJson = null,
            heartRateSeriesJson = null,
            paceSeriesJson = null,
            splitsJson = null,
            altitudeGainMeters = null,
            altitudeLossMeters = null,
            trainingEffectAerobic = trainEffectAerobic,
            trainingEffectAnaerobic = trainEffectAnaerobic,
            vo2Max = null,
            cadence = null,
            strideLengthCm = null,
            recoveryTimeHours = null,
            skinTempCelsius = null,
            swolf = null,
            poolLaps = null,
            poolLengthMeters = null,
            source = "polar-bulk",
            city = deviceModel,  // Reuse city-Feld fuer Geraete-Modell als Anzeige
            paceStreamJson = null,
            createdAt = System.currentTimeMillis(),
        )
    }

    /**
     * Parst Polar's startTime "2025-07-26T15:38:28" (lokal ohne TZ) plus
     * timezoneOffsetMinutes 120 in Unix-Millis.
     */
    private fun parseStartTimeToEpochMs(startTime: String, offsetMinutes: Int): Long? {
        return runCatching {
            val zone = ZoneOffset.ofTotalSeconds(offsetMinutes * 60)
            LocalDateTime.parse(startTime).atOffset(zone).toInstant().toEpochMilli()
        }.getOrNull()
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
