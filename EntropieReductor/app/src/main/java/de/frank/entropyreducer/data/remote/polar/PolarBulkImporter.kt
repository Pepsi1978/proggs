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
 * Iteration 6 (2026-05-16): Nach mehreren Format-Ueberraschungen importieren
 * wir erstmal NUR die METADATEN — kein Pulsverlauf, kein GPS-Track, keine
 * Pace-Streams. Frank's 956 Trainings landen alle in der Liste mit Sportart,
 * Datum, Dauer, Distanz, Avg/Max-HR, Kalorien. Detail-Streams kommen in
 * einer Folge-Iteration nachdem das Sample-Format dokumentiert ist.
 *
 * Streaming-Strategie:
 *  - ContentResolver.openInputStream(uri) → BufferedInputStream → ZipInputStream
 *  - Pro ZipEntry wird der Inhalt nur dann gelesen wenn der Name auf
 *    "training/training-session-*.json" passt
 *  - JSON wird mit ignoreUnknownKeys + isLenient + coerceInputValues
 *    geparst — robust gegen Format-Variationen
 *
 * Idempotenz: trackId "polar-{id}" — bei Live-API-Sync werden dieselben IDs
 * mit REPLACE ueberschrieben, kein Duplikat-Problem.
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
                    if (entriesSeen <= 10) {
                        Log.d(TAG, "Polar-Bulk: ZIP-Eintrag #$entriesSeen: '$name' (dir=${entry.isDirectory})")
                    }
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
                            // Nur die ersten 5 Fehler loggen — sonst wuerden 956 Stack-
                            // Traces das Logcat fluten und nichts bringen.
                            if (skipped <= 5) {
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

    /** Erkennt eine Trainings-Session-Datei im ZIP. */
    private fun isTrainingEntry(name: String): Boolean {
        val lc = name.lowercase()
        return lc.endsWith(".json") &&
            (lc.contains("training-session") || lc.contains("/training/"))
    }

    /**
     * Konvertiert eine PolarBulkSession in eine AmazfitWorkoutEntity.
     *
     * Iteration 6: NUR Metadaten — Sample-Streams werden NICHT mehr geparst
     * weil das Bulk-Format komplexer ist als die Doku sagt (samples ist ein
     * Wrapper-Objekt mit verschachteltem samples-Array, nicht direkt ein
     * Array). Frank kriegt erstmal seine 956 Trainings in die Liste,
     * Detail-Charts kommen leer. Sample-Format-Reverse-Engineering ist
     * eine eigene Folge-Iteration.
     */
    private fun sessionToEntity(session: PolarBulkSession): AmazfitWorkoutEntity? {
        if (session.exercises.isEmpty()) return null
        val exercise = session.exercises.first()

        val startEpochMs = PolarSampleMapper.parseStartTimeToEpochMs(
            session.startTime,
            session.startTimeUtcOffset,
        ) ?: return null

        val durationStr = session.duration ?: exercise.duration
        val durationSeconds = PolarSampleMapper.parseIsoDurationToSeconds(durationStr)
        val endEpochMs = startEpochMs + (durationSeconds ?: 0L) * 1000L
        val dateKey = Instant.ofEpochMilli(startEpochMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().toString()

        val distance = (session.distance ?: exercise.distance)?.toDouble()
        val calories = (session.calories ?: exercise.calories)?.toDouble()
        val avgPace = PolarSampleMapper.computeAvgPaceSecPerKm(distance, durationSeconds)
        val avgSpeedKmh = PolarSampleMapper.computeAvgSpeedKmh(distance, durationSeconds)
        val vo2Max = session.runningIndex?.toDouble()
        val trainEffectAerobic = session.trainingLoadPro?.cardioLoad?.let { it / 2.0 }
        val trainEffectAnaerobic = session.trainingLoadPro?.muscleLoad?.let { it / 2.0 }

        val heartRateSummary = session.heartRate ?: exercise.heartRate
        val sportId = exercise.sport?.id

        return AmazfitWorkoutEntity(
            trackId = "polar-${session.id}",
            dateKey = dateKey,
            startMs = startEpochMs,
            endMs = endEpochMs,
            durationSeconds = durationSeconds,
            sportType = PolarBulkSportMap.toHealthConnectType(sportId),
            sportName = PolarBulkSportMap.nameOf(sportId),
            distanceMeters = distance,
            avgPaceSecPerKm = avgPace,
            maxPaceSecPerKm = null,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = null,
            calories = calories,
            avgHeartRate = heartRateSummary?.average,
            maxHeartRate = heartRateSummary?.maximum,
            gpsTrackJson = null,
            heartRateSeriesJson = null,
            paceSeriesJson = null,
            splitsJson = null,
            altitudeGainMeters = null,
            altitudeLossMeters = null,
            trainingEffectAerobic = trainEffectAerobic,
            trainingEffectAnaerobic = trainEffectAnaerobic,
            vo2Max = vo2Max,
            cadence = null,
            strideLengthCm = null,
            recoveryTimeHours = null,
            skinTempCelsius = null,
            swolf = null,
            poolLaps = null,
            poolLengthMeters = null,
            source = "polar-bulk",
            city = null,
            paceStreamJson = null,
            createdAt = System.currentTimeMillis(),
        )
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
