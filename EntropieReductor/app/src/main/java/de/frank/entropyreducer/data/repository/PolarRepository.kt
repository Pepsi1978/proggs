package de.frank.entropyreducer.data.repository

import android.util.Log
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.remote.polar.PolarApi
import de.frank.entropyreducer.data.remote.polar.PolarExercise
import de.frank.entropyreducer.data.remote.polar.PolarRegisterRequest
import de.frank.entropyreducer.data.remote.polar.PolarSample
import de.frank.entropyreducer.data.remote.polar.PolarSampleMapper
import de.frank.entropyreducer.data.remote.polar.PolarSampleType
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.util.runCatchingCancellable
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polar-Workout-Sync (Frank-Wunsch 2026-05-16).
 *
 * Holt neue Trainings via Polar's Transaction-Workflow und konvertiert jede
 * Einheit zu einer `AmazfitWorkoutEntity` mit `source="polar"` und `trackId=
 * "polar-{exerciseId}"`. Damit landen die Workouts ohne UI-Aenderung in der
 * existierenden Workout-Pipeline (Detail-Screen, Listen, Backup).
 *
 * Transaction-Workflow (KRITISCH):
 *  1. POST /v3/users/{uid}/exercise-transactions
 *     → 201 mit Transaction-ID ODER 204 No-Content (nichts Neues)
 *  2. GET .../{tid} → Liste von Exercise-URLs
 *  3. Pro Exercise:
 *       - GET URL → Detail (Sport, Dauer, HR-Summary, Distanz)
 *       - GET URL/samples → Liste verfuegbarer Sample-Streams
 *       - GET URL/samples/0 → HR-Stream (vom H10 wenn gekoppelt)
 *       - GET URL/samples/1 → Speed-Stream (fuer Pace-Verlauf)
 *       - 200ms Rate-Limit-Schutz zwischen Exercises
 *  4. PUT /v3/users/{uid}/exercise-transactions/{tid} → Transaction committen
 *     (PFLICHT — sonst kommen die gleichen Eintraege beim naechsten Sync wieder)
 *
 * One-Time User-Registrierung:
 *  Nach dem allerersten Login muessen wir den User in Polar AccessLink
 *  registrieren (POST /v3/users mit eigener member-id). Das Flag
 *  `secrets.polarUserRegistered` schuetzt vor wiederholten Registrierungs-
 *  Versuchen. Bei 409 (schon registriert) setzen wir das Flag trotzdem
 *  damit die Sync-Phase nicht jedes Mal die Registrierung erneut versucht.
 *
 * Bei fehlender Auth liefert die Funktion eine leere Liste (kein Fehler).
 */
@Singleton
class PolarRepository @Inject constructor(
    private val api: PolarApi,
    private val oauth: OAuthService,
    private val secrets: EncryptedSecretsStore,
) {

    /** Sind wir bei Polar authentifiziert UND haben eine User-ID? */
    fun isAuthenticated(): Boolean =
        oauth.loadPolarAuthState().isAuthorized && secrets.polarUserId > 0L

    /**
     * Pollt die naechste Polar-Transaction und mappt alle enthaltenen Exercises
     * auf AmazfitWorkoutEntities. Aufrufer schreibt das Ergebnis in die DB.
     *
     * @return Liste der frischen Workouts (leer wenn nichts Neues oder nicht
     *         authentifiziert). Bei Auth-Fehler oder Token-Ablauf gibt es
     *         keinen Exception sondern eine leere Liste — der Aufrufer sieht
     *         in den Logs warum.
     */
    suspend fun fetchWorkoutsAsEntities(): Result<List<AmazfitWorkoutEntity>> = runCatchingCancellable {
        if (!isAuthenticated()) {
            Log.d(TAG, "Polar: nicht authentifiziert — kein Sync")
            return@runCatchingCancellable emptyList()
        }
        val accessToken = oauth.freshPolarAccessToken()
            ?: throw IllegalStateException("Polar-Access-Token nicht verfuegbar (Token abgelaufen oder widerrufen — neu anmelden noetig)")
        val bearer = "Bearer $accessToken"
        val userId = secrets.polarUserId

        // Schritt 0: User-Registrierung (idempotent — laeuft max 1x pro Installation).
        ensureUserRegistered(bearer)

        // Schritt 1: Transaction starten.
        val transactionResp = api.createExerciseTransaction(bearer, userId)
        when (transactionResp.code()) {
            204 -> {
                Log.d(TAG, "Polar: keine neuen Trainings (204 No Content)")
                return@runCatchingCancellable emptyList()
            }
            201 -> { /* OK, fortfahren */ }
            else -> {
                val errBody = runCatching { transactionResp.errorBody()?.string()?.take(500) }.getOrNull()
                throw IllegalStateException("Polar createExerciseTransaction HTTP ${transactionResp.code()}: $errBody")
            }
        }
        val transactionId = transactionResp.body()?.transactionId
            ?: extractTransactionIdFromLocation(transactionResp.headers()["Location"])
            ?: throw IllegalStateException("Polar Transaction-ID konnte nicht ermittelt werden (Body=${transactionResp.body()}, Location=${transactionResp.headers()["Location"]})")
        Log.i(TAG, "Polar: Transaction $transactionId gestartet")

        // Schritt 2: Liste der Exercise-URLs holen.
        val exerciseList = api.listExercisesInTransaction(bearer, userId, transactionId)
        if (exerciseList.exercises.isEmpty()) {
            Log.d(TAG, "Polar: Transaction $transactionId leer — committe und beende")
            commitSafely(bearer, userId, transactionId)
            return@runCatchingCancellable emptyList()
        }
        Log.i(TAG, "Polar: Transaction $transactionId enthaelt ${exerciseList.exercises.size} neue Trainings")

        // Schritt 3: Pro Exercise Detail + Samples laden.
        val entities = mutableListOf<AmazfitWorkoutEntity>()
        for ((idx, exerciseUrl) in exerciseList.exercises.withIndex()) {
            try {
                val exercise = api.getExercise(bearer, exerciseUrl)
                val entity = buildEntity(bearer, exerciseUrl, exercise)
                entities += entity
                Log.d(TAG, "Polar: Training ${idx + 1}/${exerciseList.exercises.size} geladen — id=${exercise.id} sport=${exercise.sport} duration=${exercise.duration}")
            } catch (ce: kotlinx.coroutines.CancellationException) {
                // Direktive 3: CancellationException nie schlucken.
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "Polar: Training $exerciseUrl konnte nicht geladen werden — ${t::class.simpleName}: ${t.message}")
                // Wir machen mit den anderen weiter — ein einzelner kaputter
                // Eintrag soll die ganze Transaction nicht versemmeln.
            }
            // 200ms Rate-Limit-Schutz — siehe Polar-Limits oben.
            delay(200)
        }

        // Schritt 4: Transaction committen — PFLICHT.
        commitSafely(bearer, userId, transactionId)

        secrets.polarLastSyncEpochMs = System.currentTimeMillis()
        Log.i(TAG, "Polar-Sync abgeschlossen: ${entities.size} Trainings importiert, Transaction $transactionId committed")
        entities
    }.onFailure { ex ->
        if (ex !is kotlinx.coroutines.CancellationException) {
            Log.e(TAG, "Polar-Sync fehlgeschlagen", ex)
        }
    }

    /**
     * Erster-Login-Setup: User in Polar AccessLink registrieren. Wird mehrfach
     * aufgerufen (jeder Sync) aber laeuft nur einmal echt durch — das Flag
     * `polarUserRegistered` schaltet bei Erfolg auf true.
     *
     * Bei 409 (User schon registriert von einer fruehren Installation) setzen
     * wir das Flag trotzdem — Polar weiss schon Bescheid, wir muessen es nicht
     * jedes Mal probieren.
     */
    private suspend fun ensureUserRegistered(bearer: String) {
        if (secrets.polarUserRegistered) return
        val memberId = secrets.polarMemberId ?: UUID.randomUUID().toString().also {
            secrets.polarMemberId = it
        }
        try {
            val resp = api.registerUser(bearer, PolarRegisterRequest(memberId = memberId))
            when (resp.code()) {
                200, 204 -> {
                    secrets.polarUserRegistered = true
                    Log.i(TAG, "Polar: User registriert mit memberId=$memberId (HTTP ${resp.code()})")
                }
                409 -> {
                    secrets.polarUserRegistered = true
                    Log.i(TAG, "Polar: User war bereits registriert (HTTP 409) — Flag gesetzt")
                }
                else -> {
                    val err = runCatching { resp.errorBody()?.string()?.take(300) }.getOrNull()
                    Log.w(TAG, "Polar: registerUser HTTP ${resp.code()} unerwartet — $err. Sync trotzdem versuchen.")
                    // Wir setzen das Flag NICHT — beim naechsten Sync nochmal probieren.
                }
            }
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Polar: registerUser Exception — ${t.message}. Sync trotzdem versuchen.")
        }
    }

    /**
     * Committet die Transaction. Sollte das fehlschlagen waere das schlimm —
     * der naechste Sync bekommt dann nochmal dieselben Trainings (Duplikat-
     * Schutz auf trackId-Ebene faengt das ab, weil wir REPLACE-Insert nutzen,
     * aber Rate-Limit wuerde unnoetig verbraucht).
     */
    private suspend fun commitSafely(bearer: String, userId: Long, transactionId: Long) {
        try {
            val resp = api.commitExerciseTransaction(bearer, userId, transactionId)
            if (resp.isSuccessful) {
                Log.d(TAG, "Polar: Transaction $transactionId committed (HTTP ${resp.code()})")
            } else {
                Log.w(TAG, "Polar: Transaction-Commit fehlgeschlagen HTTP ${resp.code()} — naechster Sync bekommt evtl. Duplikate")
            }
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Polar: Transaction-Commit Exception — ${t.message}")
        }
    }

    /** Aus dem Location-Header `/v3/users/{uid}/exercise-transactions/{tid}` die tid fischen. */
    private fun extractTransactionIdFromLocation(location: String?): Long? {
        if (location.isNullOrBlank()) return null
        return location.substringAfterLast("/").toLongOrNull()
    }

    /**
     * Baut aus einer Polar-Exercise + Sample-Streams + GPX ein AmazfitWorkoutEntity
     * — vollstaendige Daten so wie der BulkImporter (Frank-Wunsch 2026-05-16:
     * "die gleichen Daten wie historische").
     *
     * Pro Exercise werden geladen:
     *  - GET .../samples           → Liste verfuegbarer Stream-URLs
     *  - GET .../samples/0..11     → einzelne Streams (HR, Speed, Distance,
     *                                Altitude, Run-Cadence, ...)
     *  - GET .../gpx (wenn has-route=true) → GPX-Track fuer gpsTrackJson
     *
     * VO2max wird IMMER selbst berechnet (Frank-Konstanten maxHr=180, restHr=65,
     * +2-Offset wie bei Zepp) — Polar's running-index wird ignoriert.
     */
    private suspend fun buildEntity(
        bearer: String,
        exerciseUrl: String,
        exercise: PolarExercise,
    ): AmazfitWorkoutEntity {
        // Start-Zeit konvertieren — Polar liefert lokale Zeit + UTC-Offset.
        val startEpochMs = PolarSampleMapper.parseStartTimeToEpochMs(
            exercise.startTime,
            exercise.startTimeUtcOffset,
        ) ?: System.currentTimeMillis()
        val durationSeconds = PolarSampleMapper.parseIsoDurationToSeconds(exercise.duration)
        val endEpochMs = startEpochMs + (durationSeconds ?: 0L) * 1000L
        val dateKey = Instant.ofEpochMilli(startEpochMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().toString()

        // Sammelvariablen fuer Sample-Streams — befuellt wenn Polar den
        // jeweiligen Stream liefert (sonst null = "vom Server nicht geliefert").
        var hrJson: String? = null
        var paceStreamJson: String? = null
        var maxPaceSecPerKm: Double? = null
        var maxSpeedKmh: Double? = null
        var runCadenceAvg: Double? = null
        var cyclingCadenceAvg: Double? = null
        var altitudeGain: Double? = null
        var altitudeLoss: Double? = null
        var splitsJson: String? = null
        var distanceFromStream: Double? = null

        val samplesList = runCatching {
            api.listSamples(bearer, "$exerciseUrl/samples")
        }.onFailure {
            Log.w(TAG, "Polar: Sample-Liste konnte nicht geladen werden — ${it.message}")
        }.getOrNull()

        if (samplesList == null || samplesList.samples.isEmpty()) {
            Log.w(TAG, "Polar: Exercise ${exercise.id} liefert KEINE Sample-Streams. Polar braucht oft 5-30 Min nach Workout-Upload bis Streams verfuegbar sind. Beim naechsten Sync nochmal versuchen.")
        } else {
            // Welche Type-IDs hat Polar tatsaechlich angeboten? — fuer Diagnose.
            val offeredTypes = samplesList.samples.map { it.substringAfterLast("/") }
            Log.i(TAG, "Polar: Exercise ${exercise.id} bietet Sample-Type-IDs ${offeredTypes.joinToString()}")
            for (sampleUrl in samplesList.samples) {
                val typeId = sampleUrl.substringAfterLast("/")
                try {
                    val sample = api.getSample(bearer, sampleUrl)
                    val valueCount = sample.data.count { it == ',' } + 1
                    Log.d(TAG, "Polar: Stream type=$typeId rate=${sample.recordingRate}s values=$valueCount")
                    when (typeId) {
                        PolarSampleType.HEART_RATE -> {
                            hrJson = PolarSampleMapper.heartRateToJson(sample, startEpochMs)
                        }
                        PolarSampleType.SPEED -> {
                            paceStreamJson = PolarSampleMapper.speedToPaceJson(sample, startEpochMs)
                            maxPaceSecPerKm = PolarSampleMapper.maxPaceFromSpeedStream(sample)
                            maxSpeedKmh = PolarSampleMapper.maxSpeedKmhFromSpeedStream(sample)
                        }
                        PolarSampleType.RUN_CADENCE -> {
                            runCadenceAvg = PolarSampleMapper.avgFromSample(sample)
                        }
                        PolarSampleType.CADENCE -> {
                            cyclingCadenceAvg = PolarSampleMapper.avgFromSample(sample)
                        }
                        PolarSampleType.ALTITUDE -> {
                            altitudeGain = PolarSampleMapper.altitudeGainFromSample(sample)
                            altitudeLoss = PolarSampleMapper.altitudeLossFromAltitudeStream(sample)
                        }
                        PolarSampleType.DISTANCE -> {
                            splitsJson = PolarSampleMapper.splitsFromDistanceStream(sample)
                            // Letzter Wert ist die Gesamtdistanz — Fallback wenn
                            // exercise.distance fehlt.
                            distanceFromStream = PolarSampleMapper.parseValues(sample)
                                .filterNotNull().filter { it > 0.0 }.lastOrNull()
                        }
                        else -> {
                            // POWER, AIR_PRESSURE, TEMPERATURE, RR_INTERVAL — fuer
                            // Recovery aktuell nicht genutzt, aber Log fuer spaeter.
                        }
                    }
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    Log.w(TAG, "Polar: Sample-Stream $sampleUrl (type=$typeId) konnte nicht geladen werden — ${t.message}")
                }
            }
        }

        // GPS-Track: GPX-Endpoint nur abrufen wenn Polar sagt has-route=true.
        var gpsTrackJson: String? = null
        if (exercise.hasRoute) {
            val gpxUrl = "$exerciseUrl/gpx"
            try {
                val resp = api.getGpx(bearer, gpxUrl)
                if (resp.isSuccessful) {
                    val xml = resp.body()?.string()
                    if (!xml.isNullOrBlank()) {
                        gpsTrackJson = PolarSampleMapper.parseGpxToTrackJson(xml)
                        if (gpsTrackJson != null) {
                            Log.i(TAG, "Polar: GPX fuer Exercise ${exercise.id} geparst (${xml.length} bytes XML)")
                        } else {
                            Log.w(TAG, "Polar: GPX-XML lieferte 0 Trackpunkte fuer Exercise ${exercise.id}")
                        }
                    } else {
                        Log.w(TAG, "Polar: GPX-Endpoint lieferte leeren Body fuer Exercise ${exercise.id}")
                    }
                } else {
                    Log.w(TAG, "Polar: GPX-Endpoint HTTP ${resp.code()} fuer Exercise ${exercise.id}")
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "Polar: GPX-Endpoint Exception fuer Exercise ${exercise.id} — ${t.message}")
            }
        } else {
            Log.d(TAG, "Polar: Exercise ${exercise.id} hat keine Route (has-route=false)")
        }

        // Distance: Summary bevorzugt, Sample-Stream als Fallback.
        val distance = exercise.distance?.toDouble() ?: distanceFromStream
        val avgPace = PolarSampleMapper.computeAvgPaceSecPerKm(distance, durationSeconds)
        val avgSpeedKmh = PolarSampleMapper.computeAvgSpeedKmh(distance, durationSeconds)

        // VO2max IMMER selbst berechnen — Polar's running-index wird ignoriert
        // (Frank-Wunsch 2026-05-16: gleiche Formel wie Zepp, +2-Offset).
        val avgHr = exercise.heartRate?.average
        val vo2Max = PolarSampleMapper.estimateVo2Max(distance, durationSeconds, avgHr)

        // Cadence: Run-Cadence bevorzugt (Laufen), sonst Cycling-Cadence.
        val cadenceInt = (runCadenceAvg ?: cyclingCadenceAvg)?.toInt()

        // Schrittlaenge: aus Run-Cadence-Mittelwert + Distanz + Dauer.
        val strideLengthCm = PolarSampleMapper.strideLengthCmFromCadenceAndDistance(
            runCadenceAvg, distance, durationSeconds,
        )

        // Training-Effekt: Polar liefert cardio-load und muscle-load 0-10.
        // Halbierung mappt grob auf die 0-5-Garmin-Skala.
        val trainEffectAerobic = exercise.trainingLoadPro?.cardioLoad?.let { it / 2.0 }
        val trainEffectAnaerobic = exercise.trainingLoadPro?.muscleLoad?.let { it / 2.0 }

        return AmazfitWorkoutEntity(
            trackId = "polar-${exercise.id}",
            dateKey = dateKey,
            startMs = startEpochMs,
            endMs = endEpochMs,
            durationSeconds = durationSeconds,
            sportType = PolarSampleMapper.mapSportToHealthConnectType(exercise.sport, exercise.detailedSportInfo),
            sportName = PolarSampleMapper.mapSportToGerman(exercise.sport, exercise.detailedSportInfo),
            distanceMeters = distance,
            avgPaceSecPerKm = avgPace,
            maxPaceSecPerKm = maxPaceSecPerKm,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            calories = exercise.calories?.toDouble(),
            avgHeartRate = avgHr,
            maxHeartRate = exercise.heartRate?.maximum,
            gpsTrackJson = gpsTrackJson,
            heartRateSeriesJson = hrJson,
            paceSeriesJson = splitsJson,            // splitsJson liefert Km-Splits fuer parsePipeDoubleList
            splitsJson = null,
            altitudeGainMeters = altitudeGain,
            altitudeLossMeters = altitudeLoss,
            trainingEffectAerobic = trainEffectAerobic,
            trainingEffectAnaerobic = trainEffectAnaerobic,
            vo2Max = vo2Max,
            cadence = cadenceInt,
            strideLengthCm = strideLengthCm,
            recoveryTimeHours = null,
            skinTempCelsius = null,
            swolf = null,
            poolLaps = null,
            poolLengthMeters = null,
            source = "polar",
            city = null,
            paceStreamJson = paceStreamJson,
            createdAt = System.currentTimeMillis(),
        )
    }

    companion object {
        private const val TAG = "PolarRepository"
    }
}
