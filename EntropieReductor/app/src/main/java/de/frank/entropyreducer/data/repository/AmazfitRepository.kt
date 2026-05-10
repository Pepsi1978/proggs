package de.frank.entropyreducer.data.repository

import android.util.Log
import androidx.room.withTransaction
import de.frank.entropyreducer.data.local.AppDatabase
import de.frank.entropyreducer.data.local.dao.AmazfitDailyDao
import de.frank.entropyreducer.data.local.dao.AmazfitWorkoutDao
import de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.remote.zepp.ZeppApi
import de.frank.entropyreducer.data.remote.zepp.ZeppAuthService
import de.frank.entropyreducer.data.remote.zepp.ZeppBandDataResponse
import de.frank.entropyreducer.data.remote.zepp.ZeppEndpoints
import de.frank.entropyreducer.data.remote.zepp.ZeppWorkoutHistoryResponse
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.util.runCatchingCancellable
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
    private val appSettings: de.frank.entropyreducer.data.settings.AppSettings,
    private val appDatabase: AppDatabase,
    /**
     * Frank-Wunsch 2026-05-11: Nach jedem Sync und nach jedem Detail-Reload
     * automatisch ein Drive-Backup ausloesen — gleiches Pattern wie bei
     * HealthConnect-Updates (siehe BiomarkerViewModel.refreshWeight). Lazy
     * weil SyncCoordinator selbst Repositories nutzt — vermeidet zirkulaere
     * Init-Reihenfolge bei Hilt. requestSync() ist debounced (1500 ms) und
     * ein No-Op wenn Drive-Backup deaktiviert oder kein Konto verbunden ist.
     */
    private val syncCoordinatorLazy: dagger.Lazy<de.frank.entropyreducer.data.remote.drive.SyncCoordinator>,
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

    /**
     * Korrigiert bestehende Workout-Eintraege deren sportName auf einer veralteten
     * Mapping-Logik basiert. Frank-Wunsch 2026-05-10: Die T-Rex 3 sendet ALLE
     * Workouts mit source="run.NNN.huami.com", was den source-Prefix-Check als
     * primaeres Sportart-Signal unbrauchbar macht. Frueher gespeicherte Workouts
     * mit Code 7 (Trailrunning), 12 (Crosstrainer) oder 52 (Krafttraining) wurden
     * deshalb faelschlich als "Laufen" gespeichert.
     *
     * Diese Migration laeuft idempotent — `updateSportNameByType` aktualisiert nur
     * Zeilen wo der Name aktuell abweicht. Bei wiederholten Aufrufen passiert
     * nichts (0 Zeilen geaendert). Wird beim App-Start einmal in
     * EntropyReducerApp.onCreate via applicationScope gestartet.
     *
     * @return Anzahl korrigierter Zeilen (Summe ueber alle Override-Codes)
     */
    suspend fun applyFrankSportOverrides(): Int {
        // Performance-Audit Loop 1 (2026-05-10): Atomare Transaktion ueber alle
        // Override-Updates. Vorher: N separate Commits — bei Process-Kill mid-update
        // partielle Korrektur. Jetzt 1 Commit fuer alle Overrides.
        var changed = 0
        appDatabase.withTransaction {
            AmazfitSportNames.frankOverrides().forEach { (code, name) ->
                changed += workoutDao.updateSportNameByType(code, name)
            }
        }
        if (changed > 0) {
            Log.i("AmazfitRepo", "Frank-Sport-Overrides angewendet: $changed Zeilen korrigiert")
        }
        return changed
    }

    /** Manueller Auslöser: synchronisiert die letzten [days] Tage. */
    suspend fun syncLastDays(days: Int = 365): Result<Int> = runCatchingCancellable {
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
        } catch (ce: kotlinx.coroutines.CancellationException) {
            // Direktive 3: CancellationException NIEMALS schlucken — sie steuert
            // die Coroutinen-Cancellation (Worker-Stop, Lifecycle-Abbruch).
            throw ce
        } catch (t: ZeppAuthException) {
            // Nur bei echtem Auth-Fehler (401/403) Re-Login + 1x Retry.
            Log.w(TAG, "Daily-Fetch Auth-Fehler ${t.statusCode}, versuche Re-Login: ${t.message}")
            if (auth.reloginIfPossible()) {
                appToken = secrets.zeppAppToken!!
                try {
                    dailyEntities += fetchDailyRange(region, appToken, userId, from, today)
                } catch (ce: kotlinx.coroutines.CancellationException) {
                    throw ce
                } catch (retry: Throwable) {
                    Log.w(TAG, "Daily-Fetch nach Re-Login erneut fehlgeschlagen: ${retry::class.simpleName}: ${retry.message}")
                }
            } else {
                Log.w(TAG, "Re-Login nicht moeglich (keine Credentials gespeichert)")
            }
        } catch (t: ZeppEmptyBodyException) {
            // 200 mit leerem Body — Server liefert nichts fuer den Range. Kein
            // Crash, kein Re-Login (das hilft nicht), nur Log + leere Liste.
            Log.w(TAG, "Daily-Fetch lieferte leeren Body (status=${t.statusCode}). Keine Daten fuer Range $from..$today.")
        } catch (t: Throwable) {
            // Andere Fehler (5xx, Netz, Parse-Fehler in inneren Strukturen):
            // protokollieren, aber Sync nicht komplett abbrechen — Workouts
            // werden noch versucht.
            Log.w(TAG, "Daily-Fetch fehlgeschlagen: ${t::class.simpleName}: ${t.message}")
        }

        if (dailyEntities.isNotEmpty()) {
            dailyDao.upsertAll(dailyEntities)
        }

        // PAI/BioCharge/Hauttemperatur-Endpoints ENTFERNT 2026-05-09:
        // Frank-Befund nach mehreren Test-Iterationen — diese Werte sind in
        // der Zepp-Cloud-API nicht zugaenglich (alle Probe-URLs lieferten 404).
        // Die UI-Cards wurden ebenfalls entfernt damit keine leeren "—"-
        // Eintraege im Biomarker-Bereich erscheinen.

        // Workouts holen (Summary)
        val workoutEntities = try {
            fetchWorkoutSummaries(region, appToken, userId, from, today)
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Log.w(TAG, "Workout-Fetch fehlgeschlagen: ${t::class.simpleName}: ${t.message}")
            emptyList()
        }
        if (workoutEntities.isNotEmpty()) {
            // Frank-Bug 2026-05-11: Der Workout-Summary-Endpoint liefert KEINE
            // Detail-Felder (GPS-Track, Pulsverlauf, Tempoverlauf, Splits) — die
            // kommen erst beim ON-DEMAND-Aufruf von ensureWorkoutDetail. Wenn
            // wir hier direkt mit upsertAll (REPLACE-Strategie) schreiben, werden
            // bereits geladene Detail-Felder mit null UEBERSCHRIEBEN — Frank's
            // muehsam nachgeladene Pulsdiagramme, GPS-Tracks und Tempoverlaeufe
            // sind nach JEDEM Sync weg. Gleiche Bug-Klasse wie der ensureWorkout-
            // Detail-Bug von 2026-05-09 — damals wurde nur dort gefixt, hier nicht.
            // Jetzt: Existierende Eintraege lesen und Detail-Felder bewahren.
            val merged = workoutEntities.map { fresh ->
                val existing = workoutDao.getById(fresh.trackId)
                if (existing == null) {
                    fresh
                } else {
                    fresh.copy(
                        gpsTrackJson = existing.gpsTrackJson ?: fresh.gpsTrackJson,
                        heartRateSeriesJson = existing.heartRateSeriesJson ?: fresh.heartRateSeriesJson,
                        paceSeriesJson = existing.paceSeriesJson ?: fresh.paceSeriesJson,
                        paceStreamJson = existing.paceStreamJson ?: fresh.paceStreamJson,
                        splitsJson = existing.splitsJson ?: fresh.splitsJson,
                        // createdAt vom alten Eintrag uebernehmen damit der 6h-
                        // Detail-Cache-Check (in ensureWorkoutDetail) korrekt
                        // bleibt — sonst wuerde jeder Summary-Sync den Cache-
                        // Timer zuruecksetzen.
                        createdAt = existing.createdAt,
                    )
                }
            }
            workoutDao.upsertAll(merged)
        }

        val syncTs = System.currentTimeMillis()
        secrets.zeppLastSyncEpochMs = syncTs
        // Frank-Wunsch 2026-05-10: einheitlicher Sync-Zeitstempel-Pool fuer den
        // 'Zuletzt synchronisiert'-Header im Biomarker-Screen.
        appSettings.setLastAmazfitSync(syncTs)
        Log.i(
            TAG,
            "Amazfit-Sync: ${dailyEntities.size} Daily-Eintraege + ${workoutEntities.size} Workouts geschrieben",
        )
        // Frank-Wunsch 2026-05-11: Wenn neue Trainings reinkamen, sofort Drive-
        // Backup ausloesen — gleicher Pattern wie HealthConnect-Updates. Damit
        // hat das andere Geraet (S23 Ultra) beim naechsten Restore garantiert
        // den frischen Stand inkl. neuer Detail-Streams. Debounced (1500ms),
        // No-Op wenn Backup deaktiviert.
        if (workoutEntities.isNotEmpty() || dailyEntities.isNotEmpty()) {
            syncCoordinatorLazy.get().requestSync()
        }
        dailyEntities.size + workoutEntities.size
    }.onFailure {
        if (it !is kotlinx.coroutines.CancellationException) {
            Log.e(TAG, "Amazfit-Sync fehlgeschlagen", it)
        }
    }

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
        val rawResp = api.bandData(
            url = ZeppEndpoints.bandDataUrl(region),
            headers = ZeppEndpoints.dataHeaders(appToken, UUID.randomUUID().toString()),
            params = params,
        )
        // HTTP-Status differenziert behandeln (Direktive 3: nicht ratendes Re-Login,
        // sondern statusbasiert). Body wird MANUELL deserialisiert damit ein leerer
        // Body keinen Crash im retrofit2-kotlinx-serialization-converter ausloest
        // (Issue #55: Converter wirft JsonDecodingException sync beim parseResponse).
        val status = rawResp.code()
        if (status == 401 || status == 403) {
            throw ZeppAuthException(status, "Auth-Fehler beim Daily-Fetch")
        }
        if (!rawResp.isSuccessful) {
            val errBody = runCatching { rawResp.errorBody()?.string()?.take(500) }.getOrNull()
            throw IllegalStateException("bandData HTTP $status: $errBody")
        }
        val bodyString = runCatching { rawResp.body()?.string() }.getOrNull()
        if (bodyString.isNullOrEmpty()) {
            throw ZeppEmptyBodyException(status, "Body leer fuer Range $from..$to")
        }
        val resp = runCatching { JSON.decodeFromString(ZeppBandDataResponse.serializer(), bodyString) }
            .getOrElse { decodeError ->
                Log.w(TAG, "bandData Body parse-Fehler (status=$status, len=${bodyString.length}, preview=${bodyString.take(200)}): ${decodeError.message}")
                throw IllegalStateException("bandData JSON-Parse fehlgeschlagen", decodeError)
            }
        if (resp.code != null && resp.code != 1) {
            Log.w(TAG, "bandData lieferte code=${resp.code} (${resp.message})")
            return emptyList()
        }
        return resp.data.mapNotNull { day ->
            val date = day.date ?: return@mapNotNull null
            val capturedAt = parseDateAtMidnight(date) ?: return@mapNotNull null
            val parsed = parseSummaryJson(day.summary)
            // PARSER-PROBE-Diagnose-Sonde entfernt (Loop 1 Performance-Audit
            // 2026-05-10): Datenschutz (Zepp-Rohdaten im Logcat) + ungenutzte
            // String-Allokation. Field-Namen sind unten dokumentiert.
            // Echte Zepp-Schluesselnamen aus Live-Sonde 2026-05-09:
            //   stp_ttl/ttl  = Schritte gesamt
            //   stp_dis/dis  = Distanz in Metern
            //   stp_cal/cal  = aktive Kalorien
            //   rhr/slp_rhr  = Ruhepuls
            //   spob         = SpO2-Baseline %
            //   slp_to       = Schlafdauer gesamt (Min)
            //   slp_dp       = Tiefschlaf (Min)
            //   slp_lt       = Leichtschlaf (Min)
            //   slp_wk       = Wachzeit (Min)
            // PAI/BioCharge/Hauttemperatur/Stress sind hier NICHT — kommen aus
            // separaten Endpoints die das Repository spaeter aufruft.
            AmazfitDailyEntity(
                date = date,
                capturedAt = capturedAt,
                steps = firstInt(parsed, "stp_ttl", "ttl", "steps"),
                distanceMeters = firstDouble(parsed, "stp_dis", "dis", "distance"),
                activeCalories = firstDouble(parsed, "stp_cal", "cal", "calories"),
                activeMinutes = firstInt(parsed, "active_minutes"),
                averageHeartRate = firstInt(parsed, "avg_hr"),
                restingHeartRate = firstInt(parsed, "rhr", "slp_rhr"),
                spo2Percent = firstDouble(parsed, "spob", "spol", "spor"),
                // Schlaf-Felder aus dem slp_-Sub-Objekt (Live-Sonde 2026-05-09):
                //   slp_to = Total-Schlaf in Minuten
                //   slp_dp = Tiefschlaf
                //   slp_lt = Leichtschlaf
                //   slp_wk = Wachzeit
                //   slp_ss = Sleep-Score
                sleepTotalMinutes = firstInt(parsed, "slp_to", "to"),
                sleepDeepMinutes = firstInt(parsed, "slp_dp", "dp"),
                sleepLightMinutes = firstInt(parsed, "slp_lt", "lt"),
                sleepWakeMinutes = firstInt(parsed, "slp_wk", "wk"),
                sleepScore = firstInt(parsed, "slp_ss", "ss"),
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
        val rawResp = api.workoutHistory(
            url = ZeppEndpoints.sportHistoryUrl(region),
            headers = ZeppEndpoints.dataHeaders(appToken, UUID.randomUUID().toString()),
            params = params,
        )
        val status = rawResp.code()
        if (status == 401 || status == 403) {
            throw ZeppAuthException(status, "Auth-Fehler beim Workout-Fetch")
        }
        if (!rawResp.isSuccessful) {
            val errBody = runCatching { rawResp.errorBody()?.string()?.take(500) }.getOrNull()
            Log.w(TAG, "workoutHistory HTTP $status: $errBody")
            return emptyList()
        }
        val bodyString = runCatching { rawResp.body()?.string() }.getOrNull()
        if (bodyString.isNullOrEmpty()) {
            Log.w(TAG, "workoutHistory lieferte leeren Body (status=$status)")
            return emptyList()
        }
        val resp = runCatching { JSON.decodeFromString(ZeppWorkoutHistoryResponse.serializer(), bodyString) }
            .getOrElse { decodeError ->
                Log.w(TAG, "workoutHistory Body parse-Fehler (status=$status, len=${bodyString.length}, preview=${bodyString.take(200)}): ${decodeError.message}")
                return emptyList()
            }
        val items = resp.data?.summary ?: return emptyList()
        // WORKOUT-PROBE-Diagnose-Sonde entfernt (Loop 1 Performance-Audit
        // 2026-05-10): Datenschutz (Workout-Rohdaten + GPS im Logcat) +
        // 800-Zeichen-String-Allokation pro Sync.
        return items.mapNotNull { s ->
            val trackId = s.trackId ?: return@mapNotNull null
            // Zepp-Server liefert numerische Felder als Strings — defensiv parsen.
            val end = s.endTime?.toLongOrNull() ?: return@mapNotNull null
            val duration = s.durationSeconds?.toLongOrNull() ?: return@mapNotNull null
            val endMs = end * 1000L
            val startMs = endMs - duration * 1000L
            val dateKey = Instant.ofEpochMilli(startMs)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val typeInt = s.type?.toIntOrNull()
            // Zepp liefert avg_pace in Sekunden pro METER (Live-Sonde 2026-05-09:
            // 0.5042761 sec/m bei 7.16km/3612s = 8:24 min/km). Wir konvertieren auf
            // Sekunden pro Kilometer indem wir mit 1000 multiplizieren.
            val avgPaceSecPerKm = s.avgPace?.toDoubleOrNull()?.let { it * 1000.0 }
            val maxPaceSecPerKm = s.maxPace?.toDoubleOrNull()?.let { it * 1000.0 }
            // Speed berechnen falls nicht direkt geliefert: km/h aus Distanz und Dauer.
            val computedSpeedKmh = if (duration > 0 && s.distanceMeters?.toDoubleOrNull() != null) {
                (s.distanceMeters.toDouble() / 1000.0) / (duration / 3600.0)
            } else null
            // Trainingseffekt kommt als Integer 0-50 — geteilt durch 10 ergibt
            // die uebliche Anzeige 0.0-5.0 (Garmin-Skala).
            val trainEffectAerobic = s.trainingEffect?.toDoubleOrNull()?.div(10.0)
            val trainEffectAnaerobic = s.anaerobicTrainingEffect?.toDoubleOrNull()?.div(10.0)
            AmazfitWorkoutEntity(
                trackId = trackId,
                dateKey = dateKey,
                startMs = startMs,
                endMs = endMs,
                durationSeconds = duration,
                sportType = typeInt,
                sportName = AmazfitSportNames.nameOf(typeInt, s.source),
                distanceMeters = s.distanceMeters?.toDoubleOrNull(),
                avgPaceSecPerKm = avgPaceSecPerKm,
                maxPaceSecPerKm = maxPaceSecPerKm,
                avgSpeedKmh = s.avgSpeed?.toDoubleOrNull() ?: computedSpeedKmh,
                maxSpeedKmh = null,
                calories = s.calories?.toDoubleOrNull(),
                avgHeartRate = s.avgHr?.toDoubleOrNull()?.toInt(),
                maxHeartRate = s.maxHr?.toDoubleOrNull()?.toInt(),
                altitudeGainMeters = s.altitudeAscendMeters?.toDoubleOrNull(),
                altitudeLossMeters = s.altitudeDescendMeters?.toDoubleOrNull(),
                trainingEffectAerobic = trainEffectAerobic,
                trainingEffectAnaerobic = trainEffectAnaerobic,
                cadence = s.avgFrequency?.toDoubleOrNull()?.toInt(),
                strideLengthCm = s.avgStrideLength?.toIntOrNull(),
                swolf = s.swolf?.toIntOrNull(),
                poolLengthMeters = s.swimPoolLength?.toDoubleOrNull(),
                source = s.source,
                city = s.city,
                createdAt = System.currentTimeMillis(),
            )
        }
    }

    /**
     * Holt fuer ein bereits gespeichertes Workout die Detail-Daten (GPS-Track,
     * Pulsverlauf, Pace pro km, Splits) und schreibt sie in die Workout-Tabelle.
     *
     * Wird ON-DEMAND aufgerufen — wenn der Detail-Screen ein Training oeffnet —
     * statt beim grossen Sync (sonst: 288 Calls = lange + Rate-Limit).
     *
     * Cache-Verhalten (Frank-Befund 2026-05-09):
     *  - paceStreamJson NICHT null/blank → Cache-Hit, kein API-Call
     *  - paceStreamJson = " " (Marker fuer "Server lieferte nichts") → max alle 6h erneut versuchen
     *  - force=true → Cache ignorieren, immer neu laden
     */
    suspend fun ensureWorkoutDetail(trackId: String, force: Boolean = false): Result<Boolean> = runCatching {
        val workout = workoutDao.getById(trackId) ?: return@runCatching false
        val source = workout.source
            ?: return@runCatching false
        // Cache-Logik: nicht jeder Detail-Open soll den Server quaelen, aber bei
        // alten Trainings ohne Daten soll man trotzdem nochmal probieren koennen.
        if (!force) {
            val pace = workout.paceStreamJson
            // Vollstaendiger Cache-Hit: echter Stream da → fertig.
            if (!pace.isNullOrBlank() && pace != " ") {
                return@runCatching false
            }
            // Marker-Hit (" "): Server hatte nichts. Max alle 6h erneut probieren.
            if (pace == " ") {
                val ageMs = System.currentTimeMillis() - (workout.createdAt)
                if (ageMs < 6 * 60 * 60 * 1000L) {
                    return@runCatching false
                }
            }
        }
        val region = secrets.zeppRegion ?: "de2"
        val appToken = auth.freshAppToken() ?: return@runCatching false
        val userId = secrets.zeppUserId ?: return@runCatching false

        val params = baseParams(userId).toMutableMap().apply {
            put("trackid", trackId)
            put("source", source)
        }
        val resp = api.workoutDetail(
            url = ZeppEndpoints.sportDetailUrl(region, trackId),
            headers = ZeppEndpoints.webDataHeaders(appToken, UUID.randomUUID().toString()),
            params = params,
        )
        if (!resp.isSuccessful) {
            Log.w(TAG, "Workout-Detail HTTP ${resp.code()} fuer $trackId")
            return@runCatching false
        }
        val bodyString = resp.body()?.string()
        if (bodyString.isNullOrEmpty()) {
            Log.w(TAG, "Workout-Detail Body leer fuer $trackId")
            return@runCatching false
        }
        val detail = runCatching {
            JSON.decodeFromString(de.frank.entropyreducer.data.remote.zepp.ZeppWorkoutDetailResponse.serializer(), bodyString)
        }.getOrElse {
            Log.w(TAG, "Workout-Detail parse-Fehler fuer $trackId: ${it.message}; preview=${bodyString.take(200)}")
            return@runCatching false
        }
        val data = detail.data ?: return@runCatching false
        Log.i(TAG, "Workout-Detail OK fuer $trackId: gpsLen=${data.longitudeLatitude?.length ?: 0} hrLen=${data.heartRate?.length ?: 0} kiloPaceLen=${data.kiloPace?.length ?: 0}")
        // Frank-Bug 2026-05-09: Nicht mit null ueberschreiben falls der Server
        // fuer einzelne Felder bei aelteren Workouts nichts mehr liefert. Sonst
        // sind die einmal geladenen GPS/HR-Daten bei einem Re-Sync weg.
        // createdAt wird auf "jetzt" aktualisiert damit der 6h-Cache-Check
        // weiss wann zuletzt versucht wurde.
        workoutDao.upsert(
            workout.copy(
                gpsTrackJson = data.longitudeLatitude?.takeIf { it.isNotBlank() } ?: workout.gpsTrackJson,
                heartRateSeriesJson = data.heartRate?.takeIf { it.isNotBlank() } ?: workout.heartRateSeriesJson,
                paceSeriesJson = data.kiloPace?.takeIf { it.isNotBlank() } ?: workout.paceSeriesJson,
                splitsJson = data.lap?.takeIf { it.isNotBlank() } ?: workout.splitsJson,
                // Wenn data.pace immer null ist, Marker " " setzen damit das
                // Workout als "Detail geladen" gilt und kein Endlos-Reload entsteht.
                paceStreamJson = data.pace?.takeIf { it.isNotBlank() }
                    ?: workout.paceStreamJson ?: " ",
                createdAt = System.currentTimeMillis(),
            ),
        )
        // Frank-Wunsch 2026-05-11: Detail-Daten wandern direkt ins Drive-Backup,
        // damit die teuren Streams (GPS, Pulsverlauf, Tempoverlauf, Splits)
        // sofort cross-device verfuegbar sind. Debounced (1500ms) — wenn Frank
        // mehrere Trainings hintereinander oeffnet wird das zu einem Upload
        // zusammengefasst.
        syncCoordinatorLazy.get().requestSync()
        true
    }.onFailure {
        if (it !is kotlinx.coroutines.CancellationException) {
            Log.w(TAG, "ensureWorkoutDetail fehlgeschlagen fuer $trackId: ${it.message}")
        }
    }

    /**
     * Holt PAI- und Stress-Events ueber den verifizierten /events-Endpoint
     * (Quelle: bentasker/zepp_to_influxdb Issue #1 + #7). Plus probiert
     * BioCharge mit `eventType=BioChargeInfo` (unbestaetigt — T-Rex 3
     * BioCharge ist erst seit Sept 2025 im Geraet).
     *
     * Body-Preview wird ins Logcat geloggt — Frank kann mit seinem ersten
     * Sync sehen ob die Endpoints antworten und welches Format der Server
     * liefert. Ergebnis wird defensiv in amazfit_daily gemerged.
     */
    private suspend fun fetchEventBasedData(
        region: String,
        appToken: String,
        userId: String,
        from: LocalDate,
        to: LocalDate,
    ) {
        val zone = ZoneId.systemDefault()
        val fromMs = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMs = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val url = ZeppEndpoints.eventsUrl(region, userId)

        suspend fun fetch(label: String, eventType: String): String? {
            return try {
                val resp = api.rawGet(
                    url = url,
                    headers = ZeppEndpoints.webDataHeaders(appToken, UUID.randomUUID().toString()),
                    params = mapOf(
                        "eventType" to eventType,
                        "from" to fromMs.toString(),
                        "to" to toMs.toString(),
                        "limit" to "1000",
                    ),
                )
                val status = resp.code()
                val body = runCatching { resp.body()?.string() ?: resp.errorBody()?.string() }.getOrNull()
                // EVENTS-preview-Log entfernt (Loop 1 Performance-Audit 2026-05-10):
                // 3x pro Sync eager 500-Zeichen take + replace ohne BuildConfig.DEBUG-
                // Guard. Datenschutz (Zepp-Event-Rohdaten im Logcat) + Heap-Allokation.
                if (status in 200..299) body else null
            } catch (ce: kotlinx.coroutines.CancellationException) {
                throw ce
            } catch (t: Throwable) {
                Log.w(TAG, "EVENTS label=$label EXCEPTION ${t::class.simpleName}: ${t.message}")
                null
            }
        }

        val paiBody = fetch("PAI", ZeppEndpoints.EventType.PAI)
        val stressBody = fetch("STRESS", ZeppEndpoints.EventType.STRESS_ALL_DAY)
        // BioCharge nur probieren — eventType ist unbestaetigt.
        fetch("BIOCHARGE", ZeppEndpoints.EventType.BIOCHARGE)

        // Defensive Auswertung: aus den Bodies pro Tag den entsprechenden Wert
        // extrahieren und in amazfit_daily mergen. Format ist nicht 100% sicher —
        // wir parsen breit und fallen zurueck wenn unklar.
        if (paiBody != null) mergePaiIntoDaily(paiBody)
        if (stressBody != null) mergeStressIntoDaily(stressBody)
    }

    /**
     * Versucht PAI-Werte aus dem events-Body zu lesen und in die amazfit_daily-
     * Eintraege zu mergen. Erwartet ein items[]-Array mit timestamp + data
     * (data enthaelt totalPai/dailyPai/etc.). Bei unbekanntem Format: leer.
     */
    private suspend fun mergePaiIntoDaily(body: String) = runCatching {
        val root = JSON.parseToJsonElement(body) as? JsonObject ?: return@runCatching
        val items = (root["items"] as? kotlinx.serialization.json.JsonArray)
            ?: (root["data"] as? kotlinx.serialization.json.JsonArray)
            ?: return@runCatching
        for (item in items) {
            val obj = item as? JsonObject ?: continue
            val ts = obj["timestamp"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: continue
            // PAI-Daten koennen direkt im item liegen oder im "data"-Sub-Objekt.
            val data = (obj["data"] as? JsonObject) ?: obj
            val flat = flattenJsonObject(data)
            val daily = firstInt(flat, "dailyPai", "daily_pai", "totalPai", "total_pai", "pai")
                ?: continue
            val date = Instant.ofEpochMilli(ts)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val existing = dailyDao.getByDate(date)
            if (existing != null) {
                dailyDao.upsert(existing.copy(paiScore = daily))
            }
        }
    }.onFailure { Log.w(TAG, "mergePaiIntoDaily Fehler: ${it.message}") }

    /** Wie mergePaiIntoDaily, aber fuer Stress. */
    private suspend fun mergeStressIntoDaily(body: String) = runCatching {
        val root = JSON.parseToJsonElement(body) as? JsonObject ?: return@runCatching
        val items = (root["items"] as? kotlinx.serialization.json.JsonArray)
            ?: (root["data"] as? kotlinx.serialization.json.JsonArray)
            ?: return@runCatching
        for (item in items) {
            val obj = item as? JsonObject ?: continue
            val ts = obj["timestamp"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: obj["time"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
                ?: continue
            val data = (obj["data"] as? JsonObject) ?: obj
            val flat = flattenJsonObject(data)
            val avg = firstInt(flat, "avg", "average", "avgStress", "avg_stress")
                ?: continue
            val date = Instant.ofEpochMilli(ts)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val existing = dailyDao.getByDate(date)
            if (existing != null) {
                dailyDao.upsert(existing.copy(stressScore = avg))
            }
        }
    }.onFailure { Log.w(TAG, "mergeStressIntoDaily Fehler: ${it.message}") }

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
            ?: runCatching { jsonPrimitive.contentOrNull?.toIntOrNull() }.getOrNull()

    private fun JsonElement.doubleOrNull(): Double? =
        runCatching { jsonPrimitive.doubleOrNull }.getOrNull()
            ?: runCatching { jsonPrimitive.contentOrNull?.toDoubleOrNull() }.getOrNull()

    @Suppress("unused")
    private fun JsonElement.stringOrNull(): String? =
        runCatching { jsonPrimitive.contentOrNull }.getOrNull()

    /** Liefert den ersten nicht-null Int aus einer Liste von moeglichen Schluesseln. */
    private fun firstInt(map: Map<String, JsonElement>, vararg keys: String): Int? {
        for (k in keys) map[k]?.intOrNull()?.let { return it }
        return null
    }

    /** Liefert den ersten nicht-null Double aus einer Liste von moeglichen Schluesseln. */
    private fun firstDouble(map: Map<String, JsonElement>, vararg keys: String): Double? {
        for (k in keys) map[k]?.doubleOrNull()?.let { return it }
        return null
    }

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
/**
 * Sportart-Codes der Zepp-Cloud — empirisch aus Frank's echten Workouts gewonnen
 * (Live-Test 2026-05-09: Trailrunning kam als Code 7 zurueck).
 * Codes ohne Verifikation sind als "Sport (Code N)" markiert damit Frank weiss:
 * unklar, kann falsch sein. Sobald wir weitere Sportarten in echten Daten sehen,
 * wird die Liste erweitert.
 */
/**
 * Sportart-Bestimmung — robust gegen falsche Code-Tabellen.
 *
 * Frank-Befund 2026-05-09 (dritte Iteration): das urspruengliche Code-zu-Name-Mapping
 * aus Community-Quellen war komplett falsch — Frank's Workouts wurden als "Bouldern" /
 * "Curling" / "Triathlon" angezeigt obwohl er das nie gemacht hat.
 *
 * Recherche 2026-05-09 ueber Gadgetbridge-Quellcode (HuamiSportsActivityType.java,
 * lokal aus dem Repo gelesen): Die Cloud-Sport-API verwendet ein anderes Mapping als
 * die Bluetooth-Geraete-Steuerung. Selbst innerhalb der Cloud-API koennen Geraete-
 * Versionen abweichen. Frank's "Code 7 = Trailrunning"-Live-Test widersprach sogar
 * der offiziellen HuamiSportsActivityType-Tabelle (Code 7 = OpenWaterSwimming).
 *
 * Korrekte Strategie (2026-05-09 final):
 *  1. SOURCE-PREFIX zuerst — am robustesten weil Zepp die Sportart-Familie selbst im
 *     source-Identifier kodiert ("run.NNN.huami.com" → Laufen, immer korrekt).
 *  2. Code-Map als Fallback wenn source fehlt — nur die in HuamiSportsActivityType
 *     verifizierten Codes, sonst "Sport (Code N)" neutral.
 *  3. KEINE geratenen Codes mehr — lieber neutral "Sport (Code N)" als faelschlich
 *     "Bouldern" anzuzeigen. Frank kann eine Sportart-Liste pflegen sobald er
 *     verifizierte Codes aus echten Workouts hat.
 *
 * Source-Beispiel aus Frank's Workout-Body: "run.8716545.huami.com" → Laufen.
 */
internal object AmazfitSportNames {

    /**
     * Verifizierte Cloud-API-Codes aus Gadgetbridge HuamiSportsActivityType.java
     * (Repository-Snapshot 2026-05-09). Diese Codes wurden vom Open-Source-Projekt
     * empirisch aus echten Zepp-Cloud-Antworten extrahiert.
     */
    private val VERIFIED_CODES: Map<Int, String> = mapOf(
        1 to "Laufen",                    // OutdoorRunning
        2 to "Laufband",                  // Treadmill
        3 to "Walking",                   // Walking
        4 to "Radfahren",                 // Cycling (Outdoor)
        5 to "Freies Training",           // Exercise / Free training
        6 to "Schwimmen (Pool)",          // Swimming
        7 to "Trailrunning",              // Frank-Befund 2026-05-10: T-Rex 3 sendet
                                          // Code 7 fuer Trailrunning (23 Workouts in
                                          // Frank's DB). Gadgetbridge sagt OpenWaterSwimming —
                                          // die T-Rex 3 weicht hier ab.
        8 to "Indoor-Radfahren",          // IndoorCycling
        9 to "Crosstrainer",              // EllipticalTrainer
        10 to "Klettern",                 // Climbing
        12 to "Crosstrainer",             // Frank-Befund 2026-05-10: T-Rex 3 sendet
                                          // Code 12 fuer Crosstrainer (haeufigster Code,
                                          // 132 Workouts in Frank's DB)
        15 to "Wandern",                  // OutdoorHiking
        18 to "Fussball",                 // Soccer (0x12)
        21 to "Seilspringen",             // JumpRope (0x15)
        23 to "Rudermaschine",            // RowingMachine (0x17)
        52 to "Krafttraining",            // StrengthTraining (0x34)
        60 to "Yoga",                     // Yoga (0x3c)
        78 to "Cricket",                  // Cricket (0x4e)
        85 to "Basketball",               // Basketball (0x55)
        89 to "Tischtennis",              // PingPong (0x59)
        92 to "Badminton",                // Badminton (0x5c)
    )

    /**
     * Frank-bestaetigte Override-Codes (T-Rex 3 spezifisch, Stand 2026-05-10).
     *
     * Hintergrund: Die T-Rex 3 sendet ALLE Workouts mit source="run.NNN.huami.com"
     * (NNN = Geraete-ID, nicht Sportart). Das macht den source-Prefix als primaeres
     * Sportart-Signal unbrauchbar — er liefert immer "Laufen", auch fuer Crosstrainer
     * oder Krafttraining.
     *
     * Diese Codes hat Frank manuell gegen seine Trainings-Erinnerung verifiziert
     * (15.04.2026 18:15 = Crosstrainer mit Code 12; 14.03.2026 16:27 = Krafttraining
     * mit Code 52). Sie werden VOR dem source-Prefix-Check angewendet, sodass der
     * generische "run."-Prefix sie nicht ueberstimmen kann.
     *
     * Andere unbekannte T-Rex-3-Codes (16, 22, 24, 47, 57, 66) bleiben ohne Override —
     * dort wird der bisherige Pfad genutzt (source-Prefix oder "Sport (Code N)"),
     * bis Frank die Sportart bestaetigt.
     */
    private val FRANK_VERIFIED_OVERRIDES: Map<Int, String> = mapOf(
        7 to "Trailrunning",   // Frank-Befund 2026-05-10 (Live-Test 2026-05-09)
        12 to "Crosstrainer",  // Frank-Befund 15.04.2026 (132 historische Workouts)
        52 to "Krafttraining", // Frank-Befund 14.03.2026
    )

    /**
     * Liefert die Frank-bestaetigten Code-Overrides fuer DB-Migrationen.
     * Wird von [AmazfitRepository.applyFrankSportOverrides] genutzt um bestehende
     * Workout-Eintraege mit faelschlich gespeicherten Namen ("Laufen" statt
     * Crosstrainer/Krafttraining) zu korrigieren.
     */
    fun frankOverrides(): Map<Int, String> = FRANK_VERIFIED_OVERRIDES

    fun nameOf(type: Int?, source: String? = null): String {
        // Schritt 0: Frank-Override-Codes — diese stehen UEBER dem source-Prefix,
        // weil die T-Rex 3 fuer ALLE Sportarten den gleichen "run.*"-Prefix sendet.
        // Frank hat diese Codes manuell verifiziert; sie sind das verlaesslichste
        // Signal fuer die Sportart (Frank-Befund 2026-05-10).
        if (type != null) {
            FRANK_VERIFIED_OVERRIDES[type]?.let { return it }
        }
        // Schritt 1: SOURCE-PREFIX als zweite Wahl. Bei Geraeten die keine generische
        // run.*-Source senden (alte Amazfit-Modelle, andere Hersteller) ist der
        // Prefix verlaesslich — daher bleibt diese Logik fuer Codes erhalten, die
        // noch nicht im Override stehen.
        val srcPrefix = source?.substringBefore(".")?.lowercase()
        when (srcPrefix) {
            "run" -> return "Laufen"
            "trailrun", "trail" -> return "Trailrunning"
            "bike", "cycle", "cycling" -> return "Radfahren"
            "indoorbike", "indoorcycle" -> return "Indoor-Radfahren"
            "swim", "swimming" -> return "Schwimmen"
            "openswim" -> return "Freiwasser-Schwimmen"
            "walk", "walking" -> return "Walking"
            "hike", "hiking" -> return "Wandern"
            "ski", "skiing" -> return "Skifahren"
            "yoga" -> return "Yoga"
            "strength" -> return "Krafttraining"
            "treadmill" -> return "Laufband"
            "elliptical" -> return "Crosstrainer"
            "rowing" -> return "Rudern"
            "climb", "climbing" -> return "Klettern"
        }
        // Schritt 2: Code-Map als Fallback. Nur verifizierte Codes verwenden.
        if (type != null) {
            VERIFIED_CODES[type]?.let { return it }
            return "Sport (Code $type)"
        }
        return "Unbekannt"
    }
}

/**
 * Wird geworfen wenn die Zepp-API mit 401/403 antwortet — Token ist abgelaufen
 * oder ungueltig. Repository reagiert mit Re-Login + 1x Retry.
 */
internal class ZeppAuthException(
    val statusCode: Int,
    message: String,
) : RuntimeException("$message (HTTP $statusCode)")

/**
 * Wird geworfen wenn die Zepp-API mit 2xx antwortet aber leerem Body. Tritt auf
 * wenn der Server keine Daten fuer den angefragten Range hat oder wenn der
 * Endpoint die Region/Account-Konfiguration nicht unterstuetzt. Re-Login hilft
 * NICHT — der Caller soll nur loggen und mit leerer Liste weitermachen.
 */
internal class ZeppEmptyBodyException(
    val statusCode: Int,
    message: String,
) : RuntimeException("$message (HTTP $statusCode, body empty)")
