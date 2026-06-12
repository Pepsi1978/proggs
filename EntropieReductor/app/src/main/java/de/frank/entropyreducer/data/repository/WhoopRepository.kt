package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
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
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException

/**
 * Holt Cycles, Recovery und Sleep aus der Whoop-API und faltet sie pro Tag in einen
 * BiomarkerSnapshot zusammen. Workouts werden derzeit nur gezaehlt (kein Snapshot-Feld).
 *
 * Strategie:
 * - Sync-Fenster ist standardmaessig 30 Tage zurück (Spec §15.4 — Trends).
 * - Recovery + Cycle + Sleep werden parallel gepullt, dann nach Cycle-Tag gegroupt.
 * - Pro Cycle ein Snapshot. capturedAt = Sleep-End (Aufwach-Zeit) damit der Snapshot auf dem
 *   Aufwach-Tag landet — nicht auf dem Cycle-Start-Abend.
 * - Rate-Limit-Handling: Bei 429 → Exponential-Backoff (1s, 2s, 4s, max 3 Retries).
 */
@Singleton
class WhoopRepository
@Inject
constructor(
    private val dao: BiomarkerSnapshotDao,
    private val workoutDao: WhoopWorkoutDao,
    private val api: WhoopApi,
    private val oauth: OAuthService,
    private val settings: AppSettings,
    private val diagnostics: DiagnosticLogger,
) {

    fun observeLatest(): Flow<BiomarkerSnapshotEntity?> = dao.getLatest()

    fun observeRange(from: Long, to: Long): Flow<List<BiomarkerSnapshotEntity>> =
        dao.getRange(from, to)

    /** Vollstaendige Historie aller Snapshots (Frank-Wunsch 2026-05-08). */
    fun observeAll(): Flow<List<BiomarkerSnapshotEntity>> = dao.getAll()

    /** Alle Whoop-Workouts (juengste zuerst). UI gruppiert nach dateKey im VM. */
    fun observeWorkouts(): Flow<List<WhoopWorkoutEntity>> = workoutDao.observeAll()

    /**
     * Synchronisiert die letzten [days] Tage. Liefert die Anzahl gespeicherter Snapshots zurück.
     *
     * Default auf 365 Tage erhoeht (Frank-Wunsch 2026-05-08: "möchte alle existierenden Daten von
     * Whoop herunterladen"). Whoop selbst speichert nicht länger als ~2 Jahre, daher reichen 365
     * Tage als pragmatische Obergrenze für den taeglichen Sync. Für den ersten Full-Sync existiert
     * syncFullHistory() die bis 2018 zurück zieht (Whoop-Gruendungs-Jahr).
     */
    suspend fun syncLastDays(days: Int = 365): Result<Int> =
        runCatchingCancellable {
                val token =
                    oauth.freshWhoopAccessToken()
                        ?: throw IllegalStateException(
                            "Kein Whoop-Access-Token — bitte erneut anmelden."
                        )

                // Frank-Wunsch 2026-05-09: Daten erst ab Geraete-Kaufdatum 25.02.2026 holen.
                // Whoop's API gibt davor 0-Werte (z.B. Avg-HR=0) was die Charts/Korrelationen
                // verfaelscht. Vor jedem Sync alte DB-Eintraege loeschen — idempotent.
                dao.deleteOlderThan(WHOOP_DATA_START_MS)
                workoutDao.deleteOlderThan(WHOOP_DATA_START_MS)

                val end = OffsetDateTime.now(ZoneOffset.UTC)
                // Frank-Wunsch 2026-05-23 (Schritt 4): inkrementeller Sync. Wurde schon einmal
                // erfolgreich synchronisiert, nur seit dem letzten Sync laden — minus 7 Tage
                // Sicherheits-Ueberlapp, weil Whoop Erholungs-/Schlaf-Werte nachtraeglich neu
                // berechnet. Beim ersten Sync (lastSync == 0) das volle [days]-Fenster.
                // dao.upsert nutzt REPLACE -> der Ueberlapp erzeugt keine Duplikate.
                val lastWhoopSyncMs = settings.lastWhoopSyncMsFlow.first()
                val requestedStart =
                    if (lastWhoopSyncMs > 0L) {
                        OffsetDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(lastWhoopSyncMs - INCREMENTAL_OVERLAP_MS),
                            ZoneOffset.UTC,
                        )
                    } else {
                        end.minusDays(days.toLong())
                    }
                // Untergrenze auf Geraete-Kaufdatum clampen: nie davor anfragen.
                val start =
                    if (requestedStart.toInstant().toEpochMilli() < WHOOP_DATA_START_MS) {
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
                    api.listCycles(
                            authorization = auth,
                            start = isoStart,
                            end = isoEnd,
                            nextToken = token2,
                        )
                        .let { it.records to it.nextToken }
                }
                val recoveries = paged { token2 ->
                    api.listRecovery(
                            authorization = auth,
                            start = isoStart,
                            end = isoEnd,
                            nextToken = token2,
                        )
                        .let { it.records to it.nextToken }
                }
                val sleeps = paged { token2 ->
                    api.listSleep(
                            authorization = auth,
                            start = isoStart,
                            end = isoEnd,
                            nextToken = token2,
                        )
                        .let { it.records to it.nextToken }
                }
                // Workouts werden pro Tag mehrfach abgerufen — jeweils eigene Tabelle
                // (whoop_workouts), das Aggregat fuer den Tag macht das ViewModel.
                val workouts = paged { token2 ->
                    api.listWorkouts(
                            authorization = auth,
                            start = isoStart,
                            end = isoEnd,
                            nextToken = token2,
                        )
                        .let { it.records to it.nextToken }
                }

                val recByCycleId = recoveries.associateBy { it.cycleId }
                // Bug-Fix 2026-05-19: Recovery hat ein offizielles sleep_id-Feld das den
                // exakten Sleep verlinkt. Vorher matchte mapToSnapshot Sleep nur ueber
                // cycle.start.localDate, was bei Cycles deren start morgens liegt die
                // FOLGENDE Nacht statt der vorherigen zugeordnet hat. Folge: zwei Cycles
                // konnten am Ende denselben Sleep referenzieren und auf demselben Datum
                // landen — z.B. 19.05 morgens zeigte zwei Eintraege (94% und 71%) waehrend
                // der 18.05 komplett fehlte. Mit sleepById ist die Zuordnung eindeutig.
                val sleepById = sleeps.filter { !it.id.isNullOrBlank() }.associateBy { it.id!! }
                val sleepByDate =
                    sleeps
                        .filter { it.nap != true && !it.start.isNullOrBlank() }
                        .groupBy {
                            Instant.parse(it.start!!)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString()
                        }
                        .mapValues {
                            it.value.maxByOrNull { s ->
                                Instant.parse(s.end ?: s.start!!).toEpochMilli()
                            }
                        }

                val snapshots = cycles.mapNotNull { cycle ->
                    mapToSnapshot(cycle, recByCycleId[cycle.id], sleepById, sleepByDate)
                }
                snapshots.forEach { dao.upsert(it) }

                // Workouts persistieren — eigene Tabelle (whoop_workouts), ein
                // Eintrag pro Training. Manual-Edit-Schutz ist hier nicht noetig —
                // Whoop-Workouts leben in einer SEPARATEN Tabelle als die editierbaren
                // amazfit_workouts. Frank's manuelle Edits in der Trainings-Liste
                // betreffen nur amazfit_workouts.
                val workoutEntities = workouts.mapNotNull { mapToWorkoutEntity(it) }
                if (workoutEntities.isNotEmpty()) {
                    workoutDao.upsertAll(workoutEntities)
                }
                Diag.i(DiagnosticArea.WHOOP, 
                    TAG,
                    "Whoop-Sync: ${snapshots.size} Snapshots + ${workoutEntities.size} Workouts geschrieben",
                )

                settings.setLastWhoopSync(System.currentTimeMillis())
                snapshots.size
            }
            .onSuccess {
                diagnostics.success(DiagnosticArea.WHOOP, "Sync OK — $it Snapshots gespeichert")
            }
            .onFailure {
                Diag.e(DiagnosticArea.WHOOP, TAG, "Whoop-Sync fehlgeschlagen", it)
                diagnostics.error(
                    DiagnosticArea.WHOOP,
                    "Sync fehlgeschlagen: ${it.message ?: it::class.java.simpleName}",
                    it,
                )
            }

    /**
     * Voller Initial-Sync seit 2018 (Whoop-Start). Wird einmalig vom Benutzer angestossen damit
     * ALLE jemals erfassten Daten in der lokalen DB landen. Danach reichen die taeglichen
     * 365-Tage-Syncs.
     */
    suspend fun syncFullHistory(): Result<Int> =
        runCatchingCancellable {
                val today = OffsetDateTime.now(ZoneOffset.UTC)
                val startOfWhoop = today.toLocalDate().minusYears(7) // ~2018
                val days =
                    java.time.temporal.ChronoUnit.DAYS.between(startOfWhoop, today.toLocalDate())
                        .toInt()
                Diag.i(DiagnosticArea.WHOOP, TAG, "Full-Sync gestartet: $days Tage zurück")
                syncLastDays(days).getOrThrow()
            }
            .onFailure { Diag.e(DiagnosticArea.WHOOP, TAG, "Whoop-Full-Sync fehlgeschlagen", it) }

    private fun mapToSnapshot(
        cycle: WhoopCycle,
        recovery: WhoopRecovery?,
        sleepById: Map<String, WhoopSleep>,
        sleepByDate: Map<String, WhoopSleep?>,
    ): BiomarkerSnapshotEntity? {
        val startStr = cycle.start ?: return null
        val cycleStartInstant = Instant.parse(startStr)
        val date = cycleStartInstant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
        // Bug-Fix 2026-05-19: zuerst ueber Recovery.sleepId matchen (eindeutige
        // Whoop-API-Verknuepfung Recovery → Sleep). Fallback auf sleepByDate[date] nur
        // wenn kein sleepId vorhanden ist (alte Daten / Recovery ohne Score-State).
        val sleep = recovery?.sleepId?.let { sleepById[it] } ?: sleepByDate[date]
        val sleepScore = sleep?.score
        // Frank-Wunsch 2026-05-13: capturedAt = AUFWACH-Zeit, NICHT cycle.start.
        // Whoop-Cycles starten am Vortag-Abend (~17:00). Die HRV-/Recovery-Messung
        // kommt aber morgens beim Aufwachen. Vorher: capturedAt = cycle.start, also
        // 12.05 17:00 → Chart-Datum 12.05 fuer einen Wert der heute morgen kam.
        // Jetzt: capturedAt = sleep.end (Aufwach-Zeit, z.B. 13.05 07:00) → Chart-Datum
        // 13.05. Falls Sleep fehlt, fallback auf cycle.start + 14h (~ Aufwach-Zeit).
        //
        // Crash-Fix 2026-05-13: Wenn zwei Cycles dieselbe Sleep zugeordnet bekommen
        // (DST-Edge-Case oder Sync-Glitch), bekamen sie sonst identischen capturedAt
        // → IllegalArgumentException "Key X was already used" in LazyColumn. Wir
        // disambiguieren mit cycle.id (0..59s Offset) damit jeder Snapshot eindeutig
        // bleibt ohne den Aufwach-Tag zu verschieben.
        val baseCapturedAt =
            sleep?.end?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                ?: cycleStartInstant.plus(java.time.Duration.ofHours(14)).toEpochMilli()
        val cycleIdOffset = ((cycle.id ?: 0L) % 60L) * 1000L
        val capturedAt = baseCapturedAt + cycleIdOffset
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
            sleepTotalMinutes =
                sleepScore?.stageSummary?.totalInBedMilli?.let { (it / 60_000L).toInt() },
            sleepRemMinutes =
                sleepScore?.stageSummary?.totalRemSleepMilli?.let { (it / 60_000L).toInt() },
            sleepDeepMinutes =
                sleepScore?.stageSummary?.totalDeepSleepMilli?.let { (it / 60_000L).toInt() },
            sleepLightMinutes =
                sleepScore?.stageSummary?.totalLightSleepMilli?.let { (it / 60_000L).toInt() },
            sleepAwakeMinutes =
                sleepScore?.stageSummary?.totalAwakeMilli?.let { (it / 60_000L).toInt() },
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
     * Wandelt ein Whoop-Workout in eine WhoopWorkoutEntity um. Workouts ohne Start- oder
     * End-Zeitstempel werden uebersprungen (= ungueltig).
     */
    private fun mapToWorkoutEntity(workout: WhoopWorkout): WhoopWorkoutEntity? {
        val id = workout.id ?: return null
        val startStr = workout.start ?: return null
        val endStr = workout.end ?: return null
        val startMs =
            runCatching { Instant.parse(startStr).toEpochMilli() }.getOrNull() ?: return null
        val endMs = runCatching { Instant.parse(endStr).toEpochMilli() }.getOrNull() ?: return null
        val dateKey =
            Instant.ofEpochMilli(startMs).atZone(ZoneId.systemDefault()).toLocalDate().toString()
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
     * Generischer Pagination-Helper mit Rate-Limit-Backoff. `fetch(token) -> (records, nextToken)`
     */
    private suspend inline fun <T> paged(
        crossinline fetch: suspend (String?) -> Pair<List<T>, String?>
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
                    Diag.w(DiagnosticArea.WHOOP, TAG, "Whoop 429 — backing off ${backoffMs}ms")
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

        /**
         * Sicherheits-Ueberlapp fuer inkrementelle Syncs: 7 Tage vor dem letzten Sync, um
         * nachtraeglich von Whoop neu berechnete Erholungs-/Schlaf-Werte zu erfassen. Dank
         * REPLACE-upsert entstehen durch den Ueberlapp keine Duplikate.
         */
        private const val INCREMENTAL_OVERLAP_MS = 7L * 24L * 60L * 60L * 1000L
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

        /**
         * Frank-Wunsch 2026-05-09: Geraete-Kaufdatum 25.02.2026 — frueher gibt Whoop nur
         * Phantom-Werte (Avg-HR=0 etc.). Hardcoded weil sich das nicht aendert. UTC-Mitternacht
         * reicht: Whoop's API ist tag-granular, ein paar Stunden Versatz schadet nicht.
         */
        private val WHOOP_DATA_START_MS: Long =
            java.time.LocalDate.of(2026, 2, 25)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
    }
}
