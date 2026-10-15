package de.frank.entropyreducer.data.repository

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.diagnostics.DiagnosticLogger
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.dao.WhoopWorkoutDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity
import de.frank.entropyreducer.data.local.entities.mergeWhoopSnapshot
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import de.frank.entropyreducer.data.remote.oauth.WhoopReauthRequiredException
import de.frank.entropyreducer.domain.usecase.WhoopAbrufPlan
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

    private val syncMutex = Mutex()

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
        // Zentrale Sperre fuer JEDEN Whoop-Abruf (2026-10-15): Hintergrund, Biomarker-Reiter, Knopf, Jarvis,
        // Abruf nach der Anmeldung und "Alles neu laden" laufen alle hier durch — nie zwei gleichzeitig.
        syncMutex.withLock { syncIntern(days) }

    private suspend fun syncIntern(days: Int): Result<Int> {
        val versuchMs = System.currentTimeMillis()
        val vorher = settings.whoopAbrufStandFlow.first()
        if (vorher.gesperrtBisMs > versuchMs) {
            return Result.failure(WhoopAbruflimitException(vorher.gesperrtBisMs))
        }
        runCatchingCancellable {
            settings.aendereWhoopAbrufStand { WhoopAbrufPlan.nachVersuch(it, versuchMs) }
            settings.setLastWhoopAttempt(versuchMs)
        }
        var neuesterSchlaf: WhoopAbrufPlan.Schlaf? = null
        return runCatchingCancellable {
                // freshWhoopAccessToken wirft selbst sprechende Fehler (Anmeldung abgelaufen,
                // Whoop nicht erreichbar). null heisst nur noch: gar nicht mit Whoop verbunden.
                var token =
                    oauth.freshWhoopAccessToken()
                        ?: throw IllegalStateException(
                            "Whoop ist nicht verbunden — bitte unter Einstellungen → API-Schlüssel anmelden."
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
                val incrementalStart =
                    if (lastWhoopSyncMs > 0L) {
                        OffsetDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(lastWhoopSyncMs - INCREMENTAL_OVERLAP_MS),
                            ZoneOffset.UTC,
                        )
                    } else {
                        end.minusDays(days.toLong())
                    }
                // Bug-Fix 2026-09-10 (fehlende Whoop-Tage, z.B. 20.–22.08.): der rein
                // inkrementelle Sync holte Luecken, die vor dem 7-Tage-Ueberlapp lagen, nie
                // wieder nach. Jetzt reicht das Fenster bis zum fruehesten Tag ohne HRV-Wert
                // zurueck — Luecken heilen beim naechsten Sync von selbst.
                // 2026-10-13: Luecken-Nachholen hoechstens alle 12 h. Eine dauerhafte Luecke (Band
                // nicht getragen) liess sonst JEDEN Sync zwei Monate neu laden — mit dem
                // Hintergrund-Abgleich alle 30 Minuten waere das unnoetiger API-Verkehr.
                // 2026-10-14: Zeitpunkt gespeichert statt nur im Speicher. Der Hintergrund-Abgleich
                // startet oft einen frischen Prozess, dann galt der 12-h-Abstand praktisch nie.
                val nowMs = System.currentTimeMillis()
                val lastGapFillMs = settings.lastWhoopGapFillMsFlow.first()
                val gapStart =
                    if (nowMs - lastGapFillMs >= GAP_FILL_INTERVAL_MS) earliestGapStart() else null
                var lueckenGeprueftBis: String? = null
                val requestedStart =
                    if (gapStart != null && gapStart.isBefore(incrementalStart)) {
                        Diag.i(DiagnosticArea.WHOOP, TAG, "Whoop-Luecke ab $gapStart — Fenster erweitert")
                        settings.setLastWhoopGapFill(nowMs)
                        lueckenGeprueftBis = java.time.LocalDate.now().minusDays(2).toString()
                        gapStart
                    } else {
                        incrementalStart
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
                val isoStart = start.format(ISO)
                val isoEnd = end.format(ISO)

                // 401 auf einen Daten-Abruf: Der Zugangs-Token wurde verworfen (etwa nach einer Anmeldung
                // auf einem anderen Geraet). Einmal frisch holen und wiederholen, erst dann aufgeben —
                // ein einzelnes 401 heisst noch nicht, dass Frank sich neu anmelden muss.
                val daten =
                    try {
                        holeAlles("Bearer $token", isoStart, isoEnd)
                    } catch (e: HttpException) {
                        if (e.code() != 401) throw e
                        // Wurde der Token gerade erst frisch geholt, haette ein weiterer Refresh nur einen
                        // zusaetzlichen Tausch des Refresh-Tokens zur Folge — dann aufgeben.
                        if (System.currentTimeMillis() - oauth.letzterWhoopRefreshMs < FRISCH_MS) throw e
                        Diag.w(DiagnosticArea.WHOOP, TAG, "Whoop 401 — Zugangs-Token verworfen, einmal neu holen")
                        oauth.verwerfeWhoopZugangsToken()
                        token = oauth.freshWhoopAccessToken() ?: throw e
                        holeAlles("Bearer $token", isoStart, isoEnd)
                    }
                val cycles = daten.cycles
                val recoveries = daten.recoveries
                val sleeps = daten.sleeps
                val workouts = daten.workouts
                neuesterSchlaf =
                    WhoopAbrufPlan.neuesterSchlaf(
                        sleeps.map { sl ->
                            WhoopAbrufPlan.SchlafRoh(
                                id = sl.id,
                                endeMs = sl.end?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L,
                                nap = sl.nap == true,
                                scoreState = sl.scoreState,
                            )
                        },
                        recoveries.map { WhoopAbrufPlan.ErholungRoh(it.sleepId, it.scoreState) },
                    )

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
                // Bug-Fix 2026-09-10: ein noch nicht fertig berechneter Whoop-Wert (score = null)
                // darf einen vorhandenen vollstaendigen Tag nicht mit Leerwerten ersetzen.
                val existing = dao.getAll().first().associateBy { it.id }
                val merged = snapshots.map { snap ->
                    existing[snap.id]?.let { mergeWhoopSnapshot(fresh = snap, fallback = it) } ?: snap
                }
                dao.upsertAll(merged)

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
                // Was Whoop beim Nachholen fuer alte Tage nicht hatte, hat es auch spaeter nicht (Band nicht
                // getragen): diese Tage nicht alle 12 Stunden erneut anfragen (2026-10-15).
                lueckenGeprueftBis?.let { settings.setWhoopLueckenGeprueftBis(it) }
                snapshots.size
            }
            .onSuccess {
                val schlaf = neuesterSchlaf
                runCatchingCancellable {
                    settings.aendereWhoopAbrufStand { WhoopAbrufPlan.nachErfolg(it, System.currentTimeMillis(), schlaf) }
                }
                Diag.i(
                    DiagnosticArea.WHOOP,
                    TAG,
                    "Whoop-Schlafstand: Ende=${schlaf?.endeMs?.let { Instant.ofEpochMilli(it) }} vollständig=${schlaf?.vollstaendig}",
                )
                diagnostics.success(DiagnosticArea.WHOOP, "Sync OK — $it Snapshots gespeichert")
            }
            .onFailure { fehler ->
                // Eine abgelaufene Anmeldung ist kein voruebergehender Fehler: die Pause davor regelt
                // OAuthService. Netz, Server und Abruflimit verlaengern dagegen den Abstand.
                if (fehler !is WhoopReauthRequiredException) {
                    runCatchingCancellable {
                        settings.aendereWhoopAbrufStand {
                            WhoopAbrufPlan.nachFehler(it, (fehler as? WhoopAbruflimitException)?.bisMs ?: 0L)
                        }
                    }
                }
            }
            .onFailure {
                Diag.e(DiagnosticArea.WHOOP, TAG, "Whoop-Sync fehlgeschlagen", it)
                diagnostics.error(
                    DiagnosticArea.WHOOP,
                    "Sync fehlgeschlagen: ${it.message ?: it::class.java.simpleName}",
                    it,
                )
            }
    }

    private class WhoopDaten(
        val cycles: List<WhoopCycle>,
        val recoveries: List<WhoopRecovery>,
        val sleeps: List<WhoopSleep>,
        val workouts: List<WhoopWorkout>,
    )

    /** Holt Cycles, Erholung, Schlaf und Workouts im Fenster [isoStart]..[isoEnd]. */
    private suspend fun holeAlles(auth: String, isoStart: String, isoEnd: String): WhoopDaten {
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
        return WhoopDaten(cycles, recoveries, sleeps, workouts)
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
     * Fruehester Kalendertag (ab Kaufdatum bis vorgestern) ohne gespeicherten HRV-Wert, minus
     * einen Tag Puffer (Cycles starten am Vorabend). null = keine Luecke.
     */
    private suspend fun earliestGapStart(): OffsetDateTime? {
        val zone = ZoneId.systemDefault()
        val daysWithHrv =
            dao.getAll()
                .first()
                .filter { it.hrvMs != null }
                .mapTo(HashSet()) { Instant.ofEpochMilli(it.capturedAt).atZone(zone).toLocalDate() }
        val geprueftBis =
            runCatching { java.time.LocalDate.parse(settings.whoopLueckenGeprueftBisFlow.first()) }.getOrNull()
        var day = Instant.ofEpochMilli(WHOOP_DATA_START_MS).atZone(zone).toLocalDate().plusDays(1)
        if (geprueftBis != null && !geprueftBis.isBefore(day)) day = geprueftBis.plusDays(1)
        // Heute und gestern auslassen: dort ist die Recovery evtl. noch nicht berechnet —
        // diese Tage deckt der 7-Tage-Ueberlapp ohnehin ab.
        val last = java.time.LocalDate.now(zone).minusDays(2)
        while (!day.isAfter(last)) {
            if (day !in daysWithHrv) {
                return day.minusDays(1).atStartOfDay(zone).toOffsetDateTime()
            }
            day = day.plusDays(1)
        }
        return null
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
        // while(true) statt do/while: ein "continue" nach einem 429 auf der ERSTEN Seite sprang frueher zur
        // Bedingung (token == null) und beendete die Schleife mit einer leeren Liste, als waere alles gut.
        while (true) {
            try {
                val (records, next) = fetch(token)
                out += records
                token = next
                attempt = 0
                if (token == null) break
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    // Whoop nennt im Retry-After (bzw. X-RateLimit-Reset) die Wartezeit in Sekunden. Kurze
                    // Wartezeiten hier abwarten, laengere als Sperre speichern statt weiter anzuklopfen.
                    val warteMs = wartezeitMs(e) ?: (1_000L shl attempt) // ohne Angabe: 1 s, 2 s, 4 s
                    if (attempt < 3 && warteMs <= KURZ_WARTEN_MS) {
                        Diag.w(DiagnosticArea.WHOOP, TAG, "Whoop 429 — warte ${warteMs} ms")
                        delay(warteMs)
                        attempt++
                        continue
                    }
                    val bis = System.currentTimeMillis() + maxOf(warteMs, MIN_SPERRE_MS)
                    Diag.w(DiagnosticArea.WHOOP, TAG, "Whoop 429 — Abruflimit, gesperrt bis ${Instant.ofEpochMilli(bis)}")
                    throw WhoopAbruflimitException(bis)
                }
                throw e
            }
        }
        return out
    }

    companion object {
        private const val TAG = "WhoopRepository"
        private const val KURZ_WARTEN_MS = 10_000L
        private const val FRISCH_MS = 10L * 60L * 1000L
        private const val MIN_SPERRE_MS = 15L * 60L * 1000L

        /** Wartezeit aus Retry-After oder X-RateLimit-Reset (Sekunden), null = keine Angabe. */
        fun wartezeitMs(e: HttpException): Long? {
            val headers = e.response()?.headers() ?: return null
            val sekunden =
                (headers["Retry-After"] ?: headers["X-RateLimit-Reset"])?.trim()?.toLongOrNull() ?: return null
            return sekunden.coerceIn(0L, 24L * 3600L) * 1000L
        }

        /**
         * Sicherheits-Ueberlapp fuer inkrementelle Syncs: 7 Tage vor dem letzten Sync, um
         * nachtraeglich von Whoop neu berechnete Erholungs-/Schlaf-Werte zu erfassen. Dank
         * REPLACE-upsert entstehen durch den Ueberlapp keine Duplikate.
         */
        private const val INCREMENTAL_OVERLAP_MS = 7L * 24L * 60L * 60L * 1000L
        private const val GAP_FILL_INTERVAL_MS = 12L * 60L * 60L * 1000L
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

/** Whoop hat mit 429 geantwortet: bis [bisMs] nicht wieder fragen. Voruebergehend, keine neue Anmeldung noetig. */
class WhoopAbruflimitException(val bisMs: Long) :
    java.io.IOException("Whoop-Abruflimit erreicht, nächster Versuch frühestens ${java.time.Instant.ofEpochMilli(bisMs).atZone(java.time.ZoneId.systemDefault()).toLocalTime().withNano(0)} Uhr")
