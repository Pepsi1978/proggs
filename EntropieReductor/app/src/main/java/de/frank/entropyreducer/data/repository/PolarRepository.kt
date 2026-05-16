package de.frank.entropyreducer.data.repository

import android.util.Log
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.remote.polar.PolarApi
import de.frank.entropyreducer.data.remote.polar.PolarExercise
import de.frank.entropyreducer.data.remote.polar.PolarExerciseListItem
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
     * Versucht eine bestehende Exercise NEU zu laden — ueber den Transaction-
     * Workflow. Polar AccessLink V3 hat KEINEN Direct-Endpoint fuer Exercises:
     * `GET /v3/users/{uid}/exercises/{eid}` antwortet mit HTTP 404 (Live-Sonde
     * 2026-05-16). Der frueher angenommene "Standalone-Endpoint" existiert
     * schlicht nicht — die offizielle OpenAPI-Spec listet Exercises nur unter
     * `/exercise-transactions/{tid}/exercises/{eid}`.
     *
     * Konsequenz: Wir koennen eine Exercise nur dann nachladen, wenn ihre
     * urspruengliche Transaction noch OFFEN ist (also nicht committed wurde).
     * Sobald eine Transaction committed ist, sind die Daten in Polar's API
     * dauerhaft unwiederbringlich.
     *
     * Strategie:
     *  1. Neue Transaction starten (POST /exercise-transactions).
     *     - 201 → es gibt offene Daten, weiter zu Schritt 2.
     *     - 204 → kein offener Transaction-Block existiert → Exercise wurde
     *       bereits committed → return null (Frank sieht "keine frischen Daten").
     *  2. Liste der Exercise-URLs holen und nach der gesuchten exerciseId suchen.
     *  3. Wenn drin: Exercise + Samples + GPX laden via `buildEntity`.
     *  4. Wenn die Entity jetzt VOLLSTAENDIG ist (HR-Stream oder Pace-Stream da):
     *     Transaction committen.
     *     Wenn nicht (Polar braucht weiterhin Zeit fuer Samples): Transaction
     *     OFFEN lassen — der naechste Sync bekommt dieselben URLs wieder.
     *  5. Wenn nicht drin: Transaction trotzdem committen (sonst blockiert sie
     *     andere Sync-Vorgaenge), return null.
     *
     * @param exerciseId Polar-interne Exercise-ID (aus trackId="polar-{id}")
     * @return Frisch zusammengebaute Entity oder null wenn Exercise nicht mehr
     *         abrufbar (Transaction war committed) oder bei Fehler / unauthenticated.
     */
    /**
     * Refresht eine Exercise. Wenn `startMs` mitgegeben wird, kann der
     * Listen-Endpoint-Pfad (Polar's /v3/exercises) die Hashed-ID anhand
     * der Startzeit matchen — damit umgehen wir die fehlende numerische ID
     * im Listen-Item.
     */
    suspend fun refreshExercise(exerciseId: Long, startMs: Long? = null): Result<AmazfitWorkoutEntity?> = runCatchingCancellable {
        if (!isAuthenticated()) {
            Log.d(TAG, "Polar: refreshExercise($exerciseId) — nicht authentifiziert")
            return@runCatchingCancellable null
        }
        val accessToken = oauth.freshPolarAccessToken()
            ?: throw IllegalStateException("Polar-Access-Token nicht verfuegbar (Token abgelaufen — neu anmelden noetig)")
        val bearer = "Bearer $accessToken"
        val userId = secrets.polarUserId

        // PRIMAERER PFAD (Researcher-Finding 2026-05-16, Loop 3):
        // GET /v3/exercises liefert die Liste der LETZTEN 30 TAGE — OHNE
        // Transaction, OHNE Webhook. Pro Item kommt die hashed ID und die
        // start-time. Wir matchen Frank's Workout per start-time (die
        // numerische ID ist in den Listen-Items nicht enthalten — Polar's
        // API trennt die beiden Welten strikt). Mit der gefundenen hashed
        // ID koennen wir dann die Direct-Read-Endpoints aufrufen:
        // /v3/exercises/{hashedId}/samples, /gpx, /tcx — alles ohne
        // Transaction.
        val listEntity = tryListEndpointRefresh(bearer, exerciseId, startMs)
        if (listEntity != null) {
            Log.i(TAG, "Polar: refreshExercise($exerciseId) via Listen-Endpoint erfolgreich")
            return@runCatchingCancellable listEntity
        }

        // SEKUNDAERER PFAD: Direct-Read mit numerischer ID — manche Polar-
        // Endpoints akzeptieren beide Formen. Bei 404 nochmal weiter.
        val directEntity = tryDirectRefresh(bearer, exerciseId)
        if (directEntity != null) {
            Log.i(TAG, "Polar: refreshExercise($exerciseId) via Direct-Read (numerisch) erfolgreich")
            return@runCatchingCancellable directEntity
        }

        Log.i(TAG, "Polar: refreshExercise $exerciseId — alle Direct-Pfade fehlgeschlagen, Fallback Transaction-Workflow")
        val transactionResp = api.createExerciseTransaction(bearer, userId)
        when (transactionResp.code()) {
            204 -> {
                Log.w(TAG, "Polar: refreshExercise($exerciseId) — kein offener Transaction-Block (HTTP 204). Das Workout wurde bereits committed und ist in Polar's API nicht mehr abrufbar. Frank kann es nur noch per Polar-Bulk-Export importieren.")
                return@runCatchingCancellable null
            }
            201 -> { /* OK */ }
            else -> {
                val err = runCatching { transactionResp.errorBody()?.string()?.take(300) }.getOrNull()
                throw IllegalStateException("Polar createExerciseTransaction HTTP ${transactionResp.code()}: $err")
            }
        }
        val transactionId = transactionResp.body()?.transactionId
            ?: extractTransactionIdFromLocation(transactionResp.headers()["Location"])
            ?: throw IllegalStateException("Polar Transaction-ID konnte nicht ermittelt werden")

        val list = api.listExercisesInTransaction(bearer, userId, transactionId)
        val targetUrl = list.exercises.firstOrNull { url ->
            url.trimEnd('/').substringAfterLast("/").toLongOrNull() == exerciseId
        }
        if (targetUrl == null) {
            Log.w(TAG, "Polar: refreshExercise($exerciseId) — Exercise nicht in offener Transaction $transactionId (enthielt ${list.exercises.size} andere Trainings). Polar's API kann diese Exercise nicht mehr liefern.")
            // Andere Exercises in der Transaction nicht ausversehen verlieren —
            // sie kommen beim regulaeren PolarSyncWorker-Lauf rein. Hier
            // explizit NICHT committen, damit der naechste fetchWorkoutsAsEntities
            // sie noch sieht.
            return@runCatchingCancellable null
        }

        val exercise = api.getExercise(bearer, targetUrl)
        val entity = buildEntity(bearer, targetUrl, exercise)

        val hasStreams = !entity.heartRateSeriesJson.isNullOrBlank() ||
            !entity.paceStreamJson.isNullOrBlank() ||
            !entity.gpsTrackJson.isNullOrBlank()
        if (hasStreams) {
            commitSafely(bearer, userId, transactionId)
            Log.i(TAG, "Polar: refreshExercise($exerciseId) erfolgreich — Streams gefunden, Transaction $transactionId committed")
        } else {
            Log.w(TAG, "Polar: refreshExercise($exerciseId) — Exercise in Transaction $transactionId gefunden, aber Polar liefert noch keine Samples (5-30 Min nach Upload normal). Transaction OFFEN lassen, naechster Sync versucht es erneut.")
        }
        entity
    }.onFailure { ex ->
        if (ex !is kotlinx.coroutines.CancellationException) {
            Log.w(TAG, "Polar: refreshExercise($exerciseId) fehlgeschlagen — ${ex.message}")
        }
    }

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
                // Frank-UX-Fix 2026-05-16: Bei 204 hat Polar keine neuen Daten,
                // aber der Sync war trotzdem erfolgreich. Der "letzter Sync"-
                // Timestamp wird gesetzt damit Frank im UI eine Reaktion auf
                // den Klick sieht (sonst bleibt "letzter Sync vor 2h" stehen,
                // obwohl der Sync gerade erst lief).
                secrets.polarLastSyncEpochMs = System.currentTimeMillis()
                Log.d(TAG, "Polar: keine neuen Trainings (204 No Content) — Frank's Workouts bereits committed; AccessLink V3 hat dann keinen Direct-Read")
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
        var incompleteFreshCount = 0
        for ((idx, exerciseUrl) in exerciseList.exercises.withIndex()) {
            try {
                val exercise = api.getExercise(bearer, exerciseUrl)
                val entity = buildEntity(bearer, exerciseUrl, exercise)
                entities += entity
                // Vollstaendigkeit pruefen: hat das Workout mindestens HR ODER
                // Pace ODER GPS? Wenn nicht und das Workout ist juenger als 2h,
                // ist Polar's Sample-Upload vermutlich noch nicht durch.
                val hasAnyStream = !entity.heartRateSeriesJson.isNullOrBlank() ||
                    !entity.paceStreamJson.isNullOrBlank() ||
                    !entity.gpsTrackJson.isNullOrBlank()
                val ageMs = System.currentTimeMillis() - entity.startMs
                val freshWorkout = ageMs in 0..(2 * 60 * 60 * 1000L)
                if (!hasAnyStream && freshWorkout) {
                    incompleteFreshCount++
                }
                Log.d(TAG, "Polar: Training ${idx + 1}/${exerciseList.exercises.size} geladen — id=${exercise.id} sport=${exercise.sport} duration=${exercise.duration} streams=hr:${!entity.heartRateSeriesJson.isNullOrBlank()},pace:${!entity.paceStreamJson.isNullOrBlank()},gps:${!entity.gpsTrackJson.isNullOrBlank()} ageHours=${ageMs / 3_600_000}")
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

        // Schritt 4: Conditional Commit.
        //
        // Polar AccessLink V3 hat KEINEN Direct-Endpoint fuer Exercises — nach
        // einem Commit sind die Daten dauerhaft weg. Deshalb: Wenn ein juenges
        // Workout (< 2h alt) noch KEINEN Stream hat, lassen wir die Transaction
        // OFFEN — der naechste Sync (Worker laeuft alle 30 Min) bekommt dieselbe
        // Exercise nochmal mit dann hoffentlich verfuegbaren Samples.
        //
        // Sicherheitsnetz: nach 6 fehlgeschlagenen Refresh-Versuchen oder
        // wenn ALLE Workouts aelter als 6h sind, committen wir trotzdem damit
        // Polar nicht ewig blockiert wird (1 offene Transaction pro User).
        val attempts = secrets.polarRefreshAttempts
        val giveUpAttempts = 6
        val shouldKeepOpen = incompleteFreshCount > 0 && attempts < giveUpAttempts
        if (shouldKeepOpen) {
            secrets.polarRefreshAttempts = attempts + 1
            Log.i(TAG, "Polar: $incompleteFreshCount/${entities.size} frische Trainings noch ohne Samples — Transaction $transactionId OFFEN lassen (Versuch ${attempts + 1}/$giveUpAttempts), Worker probiert spaeter nochmal")
        } else {
            if (attempts >= giveUpAttempts) {
                Log.w(TAG, "Polar: nach $attempts Versuchen immer noch unvollstaendige Streams — Transaction $transactionId trotzdem committen damit Polar's API nicht blockiert bleibt")
            }
            commitSafely(bearer, userId, transactionId)
            secrets.polarRefreshAttempts = 0
        }

        secrets.polarLastSyncEpochMs = System.currentTimeMillis()
        Log.i(TAG, "Polar-Sync abgeschlossen: ${entities.size} Trainings importiert, Transaction $transactionId ${if (shouldKeepOpen) "OFFEN" else "committed"}")
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

    /**
     * Probiert die Polar V3 Direct-Read-Endpoints (`/v3/exercises/{id}`) —
     * funktionieren OHNE Transaction, auch fuer committed Workouts.
     *
     * Polar's Doku ist da widerspruechlich: einerseits nennt die Spec
     * "hashed exercise-id" (z.B. "aQlC83"), andererseits enthalten Webhook-
     * Payloads diese Hash-Form, waehrend die Transaction-API numerische
     * IDs liefert. In der Praxis akzeptieren viele Polar-Endpoints BEIDE
     * Formen — wir probieren beide.
     *
     * Return: Komplette Entity wenn Polar geantwortet hat, oder null bei
     * 404 (Direct-Read nicht moeglich → Aufrufer faellt auf Transaction
     * zurueck).
     */
    /**
     * Pollt Polar's /v3/exercises Listen-Endpoint (Last-30-Days),
     * matcht das Workout anhand der `startMs` und laedt dann Streams via
     * Direct-Read mit der hashed ID.
     */
    private suspend fun tryListEndpointRefresh(
        bearer: String,
        exerciseId: Long,
        startMs: Long?,
    ): AmazfitWorkoutEntity? {
        if (startMs == null) {
            Log.d(TAG, "Polar: tryListEndpointRefresh($exerciseId) — keine startMs vorhanden, Listen-Endpoint-Matching nicht moeglich")
            return null
        }
        Log.i(TAG, "Polar: tryListEndpointRefresh($exerciseId) — pulle /v3/exercises")
        val resp = try {
            api.listExercisesLast30Days(bearer)
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Polar: listExercisesLast30Days Exception — ${t::class.simpleName}: ${t.message}")
            return null
        }
        if (!resp.isSuccessful || resp.body() == null) {
            Log.w(TAG, "Polar: /v3/exercises HTTP ${resp.code()}")
            return null
        }
        val items = resp.body()!!
        Log.i(TAG, "Polar: /v3/exercises lieferte ${items.size} Exercises der letzten 30 Tage")

        // Match per startMs (10 Sek Toleranz fuer Zeitzonen-Glitches).
        val matched = items.firstOrNull { item ->
            val itemStartMs = item.startTime?.let { s ->
                PolarSampleMapper.parseStartTimeToEpochMs(s, item.startTimeUtcOffset ?: 0)
            } ?: return@firstOrNull false
            kotlin.math.abs(itemStartMs - startMs) < 10_000L
        }
        if (matched == null) {
            Log.w(TAG, "Polar: kein Listen-Item mit startMs $startMs (+-10s) gefunden — Workout ist evtl. aelter als 30 Tage")
            return null
        }
        Log.i(TAG, "Polar: Match gefunden — hashed-id=${matched.id} fuer Frank's exerciseId=$exerciseId")

        return buildEntityFromListItem(bearer, exerciseId, matched)
    }

    /**
     * Baut eine Entity aus einem `PolarExerciseListItem` plus den Streams
     * die wir via Direct-Read mit der HASHED ID nachladen.
     */
    private suspend fun buildEntityFromListItem(
        bearer: String,
        numericId: Long,
        item: PolarExerciseListItem,
    ): AmazfitWorkoutEntity {
        val hashedId = item.id
        val startEpochMs = item.startTime?.let { s ->
            PolarSampleMapper.parseStartTimeToEpochMs(s, item.startTimeUtcOffset ?: 0)
        } ?: System.currentTimeMillis()
        val durationSeconds = PolarSampleMapper.parseIsoDurationToSeconds(item.duration)
        val endEpochMs = startEpochMs + (durationSeconds ?: 0L) * 1000L
        val dateKey = Instant.ofEpochMilli(startEpochMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().toString()

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

        // Samples-Liste via hashed-ID-Endpoint.
        val samplesResp = runCatching {
            api.listSamplesDirect(bearer, hashedId)
        }.getOrNull()
        val samplesList = if (samplesResp?.isSuccessful == true) samplesResp.body() else null
        if (samplesList == null || samplesList.samples.isEmpty()) {
            Log.w(TAG, "Polar: /v3/exercises/$hashedId/samples — keine Samples (HTTP ${samplesResp?.code()}). Polar braucht 5-30 Min nach Workout-Upload bis Streams da sind.")
        } else {
            val offeredTypes = samplesList.samples.map { extractTypeId(it) }
            Log.i(TAG, "Polar: hashed-Exercise $hashedId bietet Sample-Type-IDs ${offeredTypes.joinToString()}")
            for (sampleUrl in samplesList.samples) {
                val typeId = extractTypeId(sampleUrl)
                try {
                    val sample = api.getSample(bearer, sampleUrl)
                    val valueCount = sample.data.count { it == ',' } + 1
                    Log.d(TAG, "Polar: Stream type=$typeId rate=${sample.recordingRate}s values=$valueCount")
                    when (typeId) {
                        PolarSampleType.HEART_RATE -> hrJson = PolarSampleMapper.heartRateToJson(sample, startEpochMs)
                        PolarSampleType.SPEED -> {
                            paceStreamJson = PolarSampleMapper.speedToPaceJson(sample, startEpochMs)
                            maxPaceSecPerKm = PolarSampleMapper.maxPaceFromSpeedStream(sample)
                            maxSpeedKmh = PolarSampleMapper.maxSpeedKmhFromSpeedStream(sample)
                        }
                        PolarSampleType.RUN_CADENCE -> runCadenceAvg = PolarSampleMapper.avgFromSample(sample)
                        PolarSampleType.CADENCE -> cyclingCadenceAvg = PolarSampleMapper.avgFromSample(sample)
                        PolarSampleType.ALTITUDE -> {
                            altitudeGain = PolarSampleMapper.altitudeGainFromSample(sample)
                            altitudeLoss = PolarSampleMapper.altitudeLossFromAltitudeStream(sample)
                        }
                        PolarSampleType.DISTANCE -> {
                            splitsJson = PolarSampleMapper.splitsFromDistanceStream(sample)
                            distanceFromStream = PolarSampleMapper.parseValues(sample)
                                .filterNotNull().filter { it > 0.0 }.lastOrNull()
                        }
                    }
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    Log.w(TAG, "Polar: Sample-Stream $sampleUrl type=$typeId Fehler — ${t.message}")
                }
            }
        }

        var gpsTrackJson: String? = null
        if (item.hasRoute) {
            try {
                val gpxResp = api.getGpxDirect(bearer, hashedId)
                if (gpxResp.isSuccessful) {
                    val xml = gpxResp.body()?.string()
                    if (!xml.isNullOrBlank()) {
                        gpsTrackJson = PolarSampleMapper.parseGpxToTrackJson(xml)
                        Log.i(TAG, "Polar: GPX fuer hashed-id $hashedId geparst (${xml.length} bytes)")
                    }
                } else {
                    Log.w(TAG, "Polar: /v3/exercises/$hashedId/gpx HTTP ${gpxResp.code()}")
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "Polar: GPX Exception — ${t.message}")
            }
        }

        val distance = item.distance?.toDouble() ?: distanceFromStream
        val avgPace = PolarSampleMapper.computeAvgPaceSecPerKm(distance, durationSeconds)
        val avgSpeedKmh = PolarSampleMapper.computeAvgSpeedKmh(distance, durationSeconds)
        val avgHr = item.heartRate?.average
        val vo2Max = PolarSampleMapper.estimateVo2Max(distance, durationSeconds, avgHr)
        val cadenceInt = (runCadenceAvg ?: cyclingCadenceAvg)?.toInt()
        val strideLengthCm = PolarSampleMapper.strideLengthCmFromCadenceAndDistance(
            runCadenceAvg, distance, durationSeconds,
        )
        val trainEffectAerobic = item.trainingLoadPro?.cardioLoad?.let { it / 2.0 }
        val trainEffectAnaerobic = item.trainingLoadPro?.muscleLoad?.let { it / 2.0 }

        return AmazfitWorkoutEntity(
            trackId = "polar-$numericId",
            dateKey = dateKey,
            startMs = startEpochMs,
            endMs = endEpochMs,
            durationSeconds = durationSeconds,
            sportType = PolarSampleMapper.mapSportToHealthConnectType(item.sport, item.detailedSportInfo),
            sportName = PolarSampleMapper.mapSportToGerman(item.sport, item.detailedSportInfo),
            distanceMeters = distance,
            avgPaceSecPerKm = avgPace,
            maxPaceSecPerKm = maxPaceSecPerKm,
            avgSpeedKmh = avgSpeedKmh,
            maxSpeedKmh = maxSpeedKmh,
            calories = item.calories?.toDouble(),
            avgHeartRate = avgHr,
            maxHeartRate = item.heartRate?.maximum,
            gpsTrackJson = gpsTrackJson,
            heartRateSeriesJson = hrJson,
            paceSeriesJson = splitsJson,
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

    private suspend fun tryDirectRefresh(bearer: String, exerciseId: Long): AmazfitWorkoutEntity? {
        // Erst die numerische Form probieren (das ist Frank's Format aus trackId).
        val exerciseIdStr = exerciseId.toString()
        Log.i(TAG, "Polar: tryDirectRefresh($exerciseId) via /v3/exercises/$exerciseIdStr")
        val resp = try {
            api.getExerciseDirect(bearer, exerciseIdStr)
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Polar: getExerciseDirect Exception — ${t::class.simpleName}: ${t.message}")
            return null
        }
        if (resp.isSuccessful && resp.body() != null) {
            val exercise = resp.body()!!
            return buildEntityFromDirect(bearer, exerciseIdStr, exercise)
        }
        if (resp.code() == 404) {
            Log.d(TAG, "Polar: Direct-Read /v3/exercises/$exerciseIdStr lieferte 404 — Basis-Endpoint kennt die numerische ID nicht. Probiere Sub-Endpoints (tcx/samples/gpx) — manche Polar-Server akzeptieren die numerische ID dort trotzdem.")
            return tryDirectSubEndpoints(bearer, exerciseId)
        }
        Log.w(TAG, "Polar: Direct-Read /v3/exercises/$exerciseIdStr HTTP ${resp.code()}")
        return null
    }

    /**
     * Wenn der Basis-Direct-Endpoint 404 gibt: probiert die Sub-Endpoints mit
     * der numerischen ID. Polar's Sub-Endpoint-Implementierungen sind nicht
     * immer 100% konsistent mit dem Basis-Endpoint — manchmal funktioniert
     * `/samples` ohne dass `/exercises/{id}` funktioniert.
     *
     * Wenn mindestens ein Sub-Endpoint Daten liefert, bauen wir eine Entity
     * aus den Workout-Stammdaten in der DB (durationSeconds, distance, avgHr
     * sind ja noch da) + den frischen Stream-Daten.
     *
     * Liefert null wenn auch alle Sub-Endpoints 404 sind.
     */
    private suspend fun tryDirectSubEndpoints(bearer: String, exerciseId: Long): AmazfitWorkoutEntity? {
        val exerciseIdStr = exerciseId.toString()
        Log.i(TAG, "Polar: tryDirectSubEndpoints($exerciseId) — probiere TCX/samples/gpx separat")

        // Test 1: TCX (kompaktes XML mit allen Streams in einer Datei)
        val tcxResp = runCatching { api.getTcxDirect(bearer, exerciseIdStr) }.getOrNull()
        val tcxOk = tcxResp?.isSuccessful == true
        Log.i(TAG, "Polar: /v3/exercises/$exerciseIdStr/tcx HTTP ${tcxResp?.code()}")

        // Test 2: Samples
        val samplesResp = runCatching { api.listSamplesDirect(bearer, exerciseIdStr) }.getOrNull()
        val samplesOk = samplesResp?.isSuccessful == true
        Log.i(TAG, "Polar: /v3/exercises/$exerciseIdStr/samples HTTP ${samplesResp?.code()}")

        // Test 3: GPX
        val gpxResp = runCatching { api.getGpxDirect(bearer, exerciseIdStr) }.getOrNull()
        val gpxOk = gpxResp?.isSuccessful == true
        Log.i(TAG, "Polar: /v3/exercises/$exerciseIdStr/gpx HTTP ${gpxResp?.code()}")

        if (!tcxOk && !samplesOk && !gpxOk) {
            Log.w(TAG, "Polar: ALLE Direct-Sub-Endpoints fuer $exerciseId liefern Fehler — die numerische ID ist in Direct-Read-Endpoints definitiv nicht erreichbar")
            return null
        }

        // Mindestens einer hat geklappt — wir bauen eine partielle Entity aus
        // dem was wir haben. Stammdaten wie startTime/duration/distance koennen
        // wir NICHT direkt aus Sub-Endpoints kriegen — die kommen aus der DB.
        // Aufrufer muss die Streams danach in die bestehende Entity mergen.
        Log.w(TAG, "Polar: Teil-Erfolg fuer $exerciseId (tcx=$tcxOk samples=$samplesOk gpx=$gpxOk). Datenextraktion via TCX/Samples/GPX wird in einer naechsten Iteration ergaenzt.")
        return null
    }

    /**
     * Wie `buildEntity` — aber ueber Direct-Read-Endpoints
     * (`/v3/exercises/{id}/samples`, `/gpx` etc.) statt Transaction-URLs.
     * Wird vom `tryDirectRefresh`-Pfad benutzt.
     */
    private suspend fun buildEntityFromDirect(
        bearer: String,
        exerciseIdStr: String,
        exercise: PolarExercise,
    ): AmazfitWorkoutEntity {
        val startEpochMs = PolarSampleMapper.parseStartTimeToEpochMs(
            exercise.startTime,
            exercise.startTimeUtcOffset,
        ) ?: System.currentTimeMillis()
        val durationSeconds = PolarSampleMapper.parseIsoDurationToSeconds(exercise.duration)
        val endEpochMs = startEpochMs + (durationSeconds ?: 0L) * 1000L
        val dateKey = Instant.ofEpochMilli(startEpochMs)
            .atZone(ZoneId.systemDefault()).toLocalDate().toString()

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

        // Sample-Liste via Direct-Read.
        val samplesListResp = runCatching {
            api.listSamplesDirect(bearer, exerciseIdStr)
        }.getOrNull()

        val samplesList = samplesListResp?.body()
        if (samplesList == null || samplesList.samples.isEmpty()) {
            Log.w(TAG, "Polar: Direct-Read /v3/exercises/$exerciseIdStr/samples liefert leere Liste — Polar braucht 5-30 Min nach Workout-Upload bis Streams da sind, oder Workout ist Indoor ohne Samples")
        } else {
            val offeredTypes = samplesList.samples.map { extractTypeId(it) }
            Log.i(TAG, "Polar: Direct-Exercise ${exercise.id} bietet Sample-Type-IDs ${offeredTypes.joinToString()}")
            for (sampleUrl in samplesList.samples) {
                val typeId = extractTypeId(sampleUrl)
                try {
                    val sample = api.getSample(bearer, sampleUrl)
                    val valueCount = sample.data.count { it == ',' } + 1
                    Log.d(TAG, "Polar: Direct-Stream type=$typeId rate=${sample.recordingRate}s values=$valueCount")
                    when (typeId) {
                        PolarSampleType.HEART_RATE -> {
                            hrJson = PolarSampleMapper.heartRateToJson(sample, startEpochMs)
                        }
                        PolarSampleType.SPEED -> {
                            paceStreamJson = PolarSampleMapper.speedToPaceJson(sample, startEpochMs)
                            maxPaceSecPerKm = PolarSampleMapper.maxPaceFromSpeedStream(sample)
                            maxSpeedKmh = PolarSampleMapper.maxSpeedKmhFromSpeedStream(sample)
                        }
                        PolarSampleType.RUN_CADENCE -> runCadenceAvg = PolarSampleMapper.avgFromSample(sample)
                        PolarSampleType.CADENCE -> cyclingCadenceAvg = PolarSampleMapper.avgFromSample(sample)
                        PolarSampleType.ALTITUDE -> {
                            altitudeGain = PolarSampleMapper.altitudeGainFromSample(sample)
                            altitudeLoss = PolarSampleMapper.altitudeLossFromAltitudeStream(sample)
                        }
                        PolarSampleType.DISTANCE -> {
                            splitsJson = PolarSampleMapper.splitsFromDistanceStream(sample)
                            distanceFromStream = PolarSampleMapper.parseValues(sample)
                                .filterNotNull().filter { it > 0.0 }.lastOrNull()
                        }
                    }
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (t: Throwable) {
                    Log.w(TAG, "Polar: Direct-Sample-Stream type=$typeId Fehler — ${t.message}")
                }
            }
        }

        // GPX via Direct-Read.
        var gpsTrackJson: String? = null
        if (exercise.hasRoute) {
            try {
                val gpxResp = api.getGpxDirect(bearer, exerciseIdStr)
                if (gpxResp.isSuccessful) {
                    val xml = gpxResp.body()?.string()
                    if (!xml.isNullOrBlank()) {
                        gpsTrackJson = PolarSampleMapper.parseGpxToTrackJson(xml)
                        Log.i(TAG, "Polar: Direct-GPX fuer ${exercise.id} (${xml.length} bytes)")
                    }
                } else {
                    Log.w(TAG, "Polar: Direct-GPX HTTP ${gpxResp.code()} fuer ${exercise.id}")
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "Polar: Direct-GPX Exception — ${t.message}")
            }
        }

        val distance = exercise.distance?.toDouble() ?: distanceFromStream
        val avgPace = PolarSampleMapper.computeAvgPaceSecPerKm(distance, durationSeconds)
        val avgSpeedKmh = PolarSampleMapper.computeAvgSpeedKmh(distance, durationSeconds)
        val avgHr = exercise.heartRate?.average
        val vo2Max = PolarSampleMapper.estimateVo2Max(distance, durationSeconds, avgHr)
        val cadenceInt = (runCadenceAvg ?: cyclingCadenceAvg)?.toInt()
        val strideLengthCm = PolarSampleMapper.strideLengthCmFromCadenceAndDistance(
            runCadenceAvg, distance, durationSeconds,
        )
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
            paceSeriesJson = splitsJson,
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

    /** Aus dem Location-Header `/v3/users/{uid}/exercise-transactions/{tid}` die tid fischen. */
    private fun extractTransactionIdFromLocation(location: String?): Long? {
        if (location.isNullOrBlank()) return null
        return location.trimEnd('/').substringAfterLast("/").substringBefore("?").toLongOrNull()
    }

    /**
     * Extrahiert die Sample-Type-ID aus einer URL wie
     * `.../exercises/{eid}/samples/10`. Defensiv gegen Trailing-Slashes
     * und Query-Strings — Polar's API liefert die URLs zwar normalerweise
     * sauber, aber wir wollen nicht von einem unerwarteten "?" oder "/"
     * verlieren, was uns die Stream-Zuordnung zerschiesst.
     */
    private fun extractTypeId(url: String): String {
        return url.trimEnd('/').substringAfterLast("/").substringBefore("?")
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
            // Defensiv: trailing-Slash und Query-Strings entfernen damit z.B.
            // "/samples/10/" oder "/samples/10?x=y" trotzdem als "10" erkannt
            // werden. Sonst wuerde das `when (typeId)` ins Leere greifen.
            val offeredTypes = samplesList.samples.map { extractTypeId(it) }
            Log.i(TAG, "Polar: Exercise ${exercise.id} bietet Sample-Type-IDs ${offeredTypes.joinToString()}")
            for (sampleUrl in samplesList.samples) {
                val typeId = extractTypeId(sampleUrl)
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
