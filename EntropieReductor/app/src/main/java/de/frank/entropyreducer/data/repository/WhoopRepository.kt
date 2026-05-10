package de.frank.entropyreducer.data.repository

import android.util.Log
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.WhoopWorkoutDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity
import de.frank.entropyreducer.data.remote.oauth.OAuthService
import de.frank.entropyreducer.data.remote.whoop.WhoopApi
import de.frank.entropyreducer.data.remote.whoop.WhoopCycle
import de.frank.entropyreducer.data.remote.whoop.WhoopRecovery
import de.frank.entropyreducer.data.remote.whoop.WhoopSleep
import de.frank.entropyreducer.data.remote.whoop.WhoopSportNames
import de.frank.entropyreducer.data.remote.whoop.WhoopWorkout
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.util.runCatchingCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import retrofit2.HttpException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holt Cycles, Recovery und Sleep aus der Whoop-API und faltet sie pro Tag in einen
 * BiomarkerSnapshot zusammen. Workouts werden derzeit nur gezaehlt (kein Snapshot-Feld).
 *
 * Strategie:
 *  - Sync-Fenster ist standardmaessig 30 Tage zurück (Spec §15.4 — Trends).
 *  - Recovery + Cycle + Sleep werden parallel gepullt, dann nach Cycle-Tag gegroupt.
 *  - Pro Cycle ein Snapshot (Cycle-Start = capturedAt).
 *  - Rate-Limit-Handling: Bei 429 → Exponential-Backoff (1s, 2s, 4s, max 3 Retries).
 */
@Singleton
class WhoopRepository @Inject constructor(
    private val dao: BiomarkerSnapshotDao,
    private val workoutDao: WhoopWorkoutDao,
    private val api: WhoopApi,
    private val oauth: OAuthService,
    private val settings: AppSettings,
) {

    fun observeLatest(): Flow<BiomarkerSnapshotEntity?> = dao.getLatest()
    fun observeRange(from: Long, to: Long): Flow<List<BiomarkerSnapshotEntity>> =
        dao.getRange(from, to)

    /** Vollstaendige Historie aller Snapshots (Frank-Wunsch 2026-05-08). */
    fun observeAll(): Flow<List<BiomarkerSnapshotEntity>> = dao.getAll()

    /** Alle Whoop-Workouts (juengste zuerst). UI gruppiert nach dateKey im VM. */
    fun observeWorkouts(): Flow<List<WhoopWorkoutEntity>> = workoutDao.observeAll()

    /**
     * Synchronisiert die letzten [days] Tage. Liefert die Anzahl gespeicherter
     * Snapshots zurück.
     *
     * Default auf 365 Tage erhoeht (Frank-Wunsch 2026-05-08: "möchte alle
     * existierenden Daten von Whoop herunterladen"). Whoop selbst speichert
     * nicht länger als ~2 Jahre, daher reichen 365 Tage als pragmatische
     * Obergrenze für den taeglichen Sync. Für den ersten Full-Sync existiert
     * syncFullHistory() die bis 2018 zurück zieht (Whoop-Gruendungs-Jahr).
     */
    suspend fun syncLastDays(days: Int = 365): Result<Int> = runCatchingCancellable {
        val token = oauth.freshWhoopAccessToken()
            ?: throw IllegalStateException("Kein Whoop-Access-Token — bitte erneut anmelden.")

        // Frank-Wunsch 2026-05-09: Daten erst ab Geraete-Kaufdatum 25.02.2026 holen.
        // Whoop's API gibt davor 0-Werte (z.B. Avg-HR=0) was die Charts/Korrelationen
        // verfaelscht. Vor jedem Sync alte DB-Eintraege loeschen — idempotent.
        dao.deleteOlderThan(WHOOP_DATA_START_MS)
        workoutDao.deleteOlderThan(WHOOP_DATA_START_MS)

        val end = OffsetDateTime.now(ZoneOffset.UTC)
        val requestedStart = end.minusDays(days.toLong())
        // Untergrenze auf Geraete-Kaufdatum clampen: nie davor anfragen.
        val start = if (requestedStart.toInstant().toEpochMilli() < WHOOP_DATA_START_MS) {
            OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(WHOOP_DATA_START_MS),
                ZoneOffset.UTC,
            )
        } else {
            requestedStart
        }
        val auth = "Bearer $token"
        val isoStart = start.format(ISO)
        val isoEnd = end.format(ISO)

        val cycles = paged { token2 ->
            api.listCycles(authorization = auth, start = isoStart, end = isoEnd, nextToken = token2)
                .let { it.records to it.nextToken }
        }
        val recoveries = paged { token2 ->
            api.listRecovery(authorization = auth, start = isoStart, end = isoEnd, nextToken = token2)
                .let { it.records to it.nextToken }
        }
        val sleeps = paged { token2 ->
            api.listSleep(authorization = auth, start = isoStart, end = isoEnd, nextToken = token2)
                .let { it.records to it.nextToken }
        }
        // Workouts werden pro Tag mehrfach abgerufen — jeweils eigene Tabelle
        // (whoop_workouts), das Aggregat fuer den Tag macht das ViewModel.
        val workouts = paged { token2 ->
            api.listWorkouts(authorization = auth, start = isoStart, end = isoEnd, nextToken = token2)
                .let { it.records to it.nextToken }
        }

        val recByCycleId = recoveries.associateBy { it.cycleId }
        // Sleeps haben keine direkte cycle_id im Public-Schema — wir matchen über das Datum.
        val sleepByDate = sleeps
            .filter { it.nap != true && !it.start.isNullOrBlank() }
            .groupBy { Instant.parse(it.start!!).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
            .mapValues { it.value.maxByOrNull { s -> Instant.parse(s.end ?: s.start!!).toEpochMilli() } }

        val snapshots = cycles.mapNotNull { cycle -> mapToSnapshot(cycle, recByCycleId[cycle.id], sleepByDate) }
        snapshots.forEach { dao.upsert(it) }

        // Workouts persistieren — eigene Tabelle, ein Eintrag pro Training.
        val workoutEntities = workouts.mapNotNull { mapToWorkoutEntity(it) }
        if (workoutEntities.isNotEmpty()) {
            workoutDao.upsertAll(workoutEntities)
        }
        Log.i(TAG, "Whoop-Sync: ${snapshots.size} Snapshots + ${workoutEntities.size} Workouts geschrieben")

        settings.setLastWhoopSync(System.currentTimeMillis())
        snapshots.size
    }.onFailure { Log.e(TAG, "Whoop-Sync fehlgeschlagen", it) }

    /**
     * Voller Initial-Sync seit 2018 (Whoop-Start). Wird einmalig vom Benutzer
     * angestossen damit ALLE jemals erfassten Daten in der lokalen DB landen.
     * Danach reichen die taeglichen 365-Tage-Syncs.
     */
    suspend fun syncFullHistory(): Result<Int> = runCatchingCancellable {
        val today = OffsetDateTime.now(ZoneOffset.UTC)
        val startOfWhoop = today.toLocalDate().minusYears(7) // ~2018
        val days = java.time.temporal.ChronoUnit.DAYS.between(startOfWhoop, today.toLocalDate()).toInt()
        Log.i(TAG, "Full-Sync gestartet: $days Tage zurück")
        syncLastDays(days).getOrThrow()
    }.onFailure { Log.e(TAG, "Whoop-Full-Sync fehlgeschlagen", it) }

    private fun mapToSnapshot(
        cycle: WhoopCycle,
        recovery: WhoopRecovery?,
        sleepByDate: Map<String, WhoopSleep?>,
    ): BiomarkerSnapshotEntity? {
        val startStr = cycle.start ?: return null
        val capturedAt = Instant.parse(startStr).toEpochMilli()
        val date = Instant.parse(startStr).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val sleep = sleepByDate[date]
        val sleepScore = sleep?.score
        val recoveryScore = recovery?.score

        // Frank-Wunsch 2026-05-08: alle Whoop-Felder mappen die die API liefert.
        val sleepNeed = sleepScore?.sleepNeeded
        val sleepNeedMillis = sleepNeed?.baselineMilli
        val sleepDebtMillis = sleepNeed?.needFromSleepDebtMilli
        return BiomarkerSnapshotEntity(
            id = "cycle-${cycle.id}",
            capturedAt = capturedAt,
            recoveryScore = recoveryScore?.recoveryScore?.toInt(),
            hrvMs = recoveryScore?.hrvRmssdMilli,
            restingHeartRate = recoveryScore?.restingHeartRate?.toInt(),
            sleepPerformance = sleepScore?.sleepPerformancePercentage?.toInt(),
            sleepTotalMinutes = sleepScore?.stageSummary?.totalInBedMilli?.let { (it / 60_000L).toInt() },
            sleepRemMinutes = sleepScore?.stageSummary?.totalRemSleepMilli?.let { (it / 60_000L).toInt() },
            sleepDeepMinutes = sleepScore?.stageSummary?.totalDeepSleepMilli?.let { (it / 60_000L).toInt() },
            sleepLightMinutes = sleepScore?.stageSummary?.totalLightSleepMilli?.let { (it / 60_000L).toInt() },
            sleepAwakeMinutes = sleepScore?.stageSummary?.totalAwakeMilli?.let { (it / 60_000L).toInt() },
            sleepDisturbances = sleepScore?.stageSummary?.disturbanceCount,
            dayStrain = cycle.score?.strain,
            dayKilojoules = cycle.score?.kilojoule,
            createdAt = System.currentTimeMillis(),
            respiratoryRate = sleepScore?.respiratoryRate,
            sleepConsistencyPercent = sleepScore?.sleepConsistencyPercentage?.toInt(),
            sleepEfficiencyPercent = sleepScore?.sleepEfficiencyPercentage?.toInt(),
            sleepNeedMinutes = sleepNeedMillis?.let { (it / 60_000L).toInt() },
            sleepDebtMinutes = sleepDebtMillis?.let { (it / 60_000L).toInt() },
            spo2Percent = recoveryScore?.spo2Percentage,
            skinTempCelsius = recoveryScore?.skinTempCelsius,
            averageHeartRate = cycle.score?.averageHeartRate?.toInt(),
            maxHeartRate = cycle.score?.maxHeartRate?.toInt(),
            sleepCycleCount = sleepScore?.stageSummary?.sleepCycleCount,
        )
    }

    /**
     * Wandelt ein Whoop-Workout in eine WhoopWorkoutEntity um. Workouts ohne
     * Start- oder End-Zeitstempel werden uebersprungen (= ungueltig).
     */
    private fun mapToWorkoutEntity(workout: WhoopWorkout): WhoopWorkoutEntity? {
        val id = workout.id ?: return null
        val startStr = workout.start ?: return null
        val endStr = workout.end ?: return null
        val startMs = runCatching { Instant.parse(startStr).toEpochMilli() }.getOrNull() ?: return null
        val endMs = runCatching { Instant.parse(endStr).toEpochMilli() }.getOrNull() ?: return null
        val dateKey = Instant.ofEpochMilli(startMs).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        val score = workout.score
        val zones = score?.zoneDuration
        return WhoopWorkoutEntity(
            id = id,
            dateKey = dateKey,
            startMs = startMs,
            endMs = endMs,
            sportId = workout.sportId,
            sportName = WhoopSportNames.nameOf(workout.sportId),
            strain = score?.strain,
            kilojoule = score?.kilojoule,
            averageHeartRate = score?.averageHeartRate?.toInt(),
            maxHeartRate = score?.maxHeartRate?.toInt(),
            percentRecorded = score?.percentRecorded,
            distanceMeter = score?.distanceMeter,
            altitudeGainMeter = score?.altitudeGainMeter,
            zoneZeroMilli = zones?.zoneZeroMilli,
            zoneOneMilli = zones?.zoneOneMilli,
            zoneTwoMilli = zones?.zoneTwoMilli,
            zoneThreeMilli = zones?.zoneThreeMilli,
            zoneFourMilli = zones?.zoneFourMilli,
            zoneFiveMilli = zones?.zoneFiveMilli,
            createdAt = System.currentTimeMillis(),
        )
    }

    /**
     * Generischer Pagination-Helper mit Rate-Limit-Backoff.
     * `fetch(token) -> (records, nextToken)`
     */
    private suspend inline fun <T> paged(
        crossinline fetch: suspend (String?) -> Pair<List<T>, String?>,
    ): List<T> {
        val out = mutableListOf<T>()
        var token: String? = null
        var attempt = 0
        do {
            try {
                val (records, next) = fetch(token)
                out += records
                token = next
                attempt = 0
            } catch (e: HttpException) {
                if (e.code() == 429 && attempt < 3) {
                    val backoffMs = (1_000L shl attempt) // 1s, 2s, 4s
                    Log.w(TAG, "Whoop 429 — backing off ${backoffMs}ms")
                    delay(backoffMs)
                    attempt++
                    continue
                }
                throw e
            }
        } while (token != null)
        return out
    }

    companion object {
        private const val TAG = "WhoopRepository"
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        /** Frank-Wunsch 2026-05-09: Geraete-Kaufdatum 25.02.2026 — frueher gibt
         *  Whoop nur Phantom-Werte (Avg-HR=0 etc.). Hardcoded weil sich das nicht
         *  aendert. UTC-Mitternacht reicht: Whoop's API ist tag-granular, ein paar
         *  Stunden Versatz schadet nicht. */
        private val WHOOP_DATA_START_MS: Long = java.time.LocalDate.of(2026, 2, 25)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    }
}
