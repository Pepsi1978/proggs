package de.frank.entropyreducer.presentation.dashboard4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.health.HealthConnectManager
import de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.OuraActivityEntity
import de.frank.entropyreducer.data.local.entities.OuraDailySleepEntity
import de.frank.entropyreducer.data.local.entities.OuraReadinessEntity
import de.frank.entropyreducer.data.local.entities.OuraResilienceEntity
import de.frank.entropyreducer.data.local.entities.OuraSleepDetailEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.repository.AmazfitRepository
import de.frank.entropyreducer.data.repository.BiomarkerCardOrderRepository
import de.frank.entropyreducer.data.repository.ZeppBodyRepository
import de.frank.entropyreducer.data.repository.OuraRepository
import de.frank.entropyreducer.data.repository.WhoopRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.domain.status.StatusObserver
import de.frank.entropyreducer.workers.BackgroundScheduler
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class StatusBundle(
    val refreshing: Boolean,
    val message: String?,
    val breakdown: StatusBreakdown?,
    val lastWhoopSyncMs: Long,
    val selectedDate: java.time.LocalDate,
)

/**
 * Frank-Wunsch 2026-05-10: einheitlicher Sync-Zeitstempel-Pool fuer den 'Zuletzt
 * synchronisiert'-Header. Pro Biomarker-Quelle eigener Wert.
 */
private data class SyncTimes(val oura: Long, val amazfit: Long, val zepp: Long)

/**
 * Whoop-Daten gebuendelt damit der outer combine() unter 5 Flows bleibt. last30 wird NICHT als Flow
 * eingebunden, sondern in der Transform-Lambda aus history mit dem aktuellen
 * System.currentTimeMillis() abgeleitet — sonst friert das 30-Tage-Fenster auf den
 * Konstrukturzeitpunkt ein und wird stale wenn die App ueber Mitternacht offen bleibt.
 */
private data class WhoopBundle(
    val latest: BiomarkerSnapshotEntity?,
    val history: List<BiomarkerSnapshotEntity>,
    val workouts: List<WhoopWorkoutEntity>,
)

/** Amazfit-T-Rex-3-Daten gebuendelt — gleiche Idee wie StatusBundle. */
private data class AmazfitBundle(
    val latestDaily: AmazfitDailyEntity?,
    val allDaily: List<AmazfitDailyEntity>,
    val workouts: List<AmazfitWorkoutEntity>,
)

/**
 * Oura-Ring-Daten gebuendelt (Frank-Wunsch 2026-05-10). Fuenf Listen, pro Endpunkt eine. Latest und
 * Selected-Day werden im inner-combine daraus abgeleitet.
 */
private data class OuraBundle(
    val readiness: List<OuraReadinessEntity>,
    val dailySleep: List<OuraDailySleepEntity>,
    val activity: List<OuraActivityEntity>,
    val resilience: List<OuraResilienceEntity>,
    val sleepDetail: List<OuraSleepDetailEntity>,
)

@androidx.compose.runtime.Immutable
data class BiomarkerUiState(
    val latest: BiomarkerSnapshotEntity? = null,
    /**
     * VOLLSTAENDIGE Historie aller Whoop-Snapshots — nicht mehr auf 30 Tage limitiert (Frank-Wunsch
     * 2026-05-08). Trends, Detail-Screen und Korrelationen nutzen das. history30Days bleibt als
     * 30-Tage-Slice für die Mini-Card-Deltas.
     */
    val history: List<BiomarkerSnapshotEntity> = emptyList(),
    val history30Days: List<BiomarkerSnapshotEntity> = emptyList(),
    /**
     * Aktuell ausgewaehlter Tag (Frank-Wunsch 2026-05-08: zwischen Heute / gestern / vorgestern
     * wechseln). Default = Heute. Der Snapshot für diesen Tag wird in selectedSnapshot gehalten und
     * in den Mini-Cards + Recovery-Ring angezeigt.
     */
    val selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
    val selectedSnapshot: BiomarkerSnapshotEntity? = null,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val statusBreakdown: StatusBreakdown? = null,
    /**
     * Zeitstempel der letzten erfolgreichen Whoop-Synchronisation in ms. 0 = noch nie erfolgreich
     * gesynced. Frank-Wunsch 2026-05-09: „Zuletzt synchronisiert"-Zeile als kleine Info unter dem
     * Header.
     */
    val lastWhoopSyncMs: Long = 0L,
    /**
     * Frank-Wunsch 2026-05-10: pro Biomarker-Quelle eigener Zeitstempel, damit der Header das
     * Minimum aller vier (= "alles aktuell"-Zeitpunkt) zeigt.
     */
    val lastOuraSyncMs: Long = 0L,
    val lastAmazfitSyncMs: Long = 0L,
    val lastZeppBodySyncMs: Long = 0L,
    /**
     * Alle Whoop-Workouts, juengste zuerst (Frank-Wunsch 2026-05-09: kompletter Workout-Bereich mit
     * Sportart, Strain, HR-Zonen). UI gruppiert nach Tag.
     */
    val workouts: List<WhoopWorkoutEntity> = emptyList(),
    /** Workouts des aktuell ausgewaehlten Tages — sortiert nach Startzeit aufsteigend. */
    val workoutsForSelectedDay: List<WhoopWorkoutEntity> = emptyList(),
    /**
     * Eigenberechnung — Erholsamer Schlaf in % = (REM + Tiefschlaf) / Zeit im Bett. Zeit im Bett =
     * REM + Tiefschlaf + Leichtschlaf + Wach (alle Phasen zusammen). So berechnet Whoop den Wert —
     * Frank-Vorgabe 2026-05-10 (91,9% Whoop-Referenz).
     */
    val restorativeSleepPercent: Double? = null,
    /**
     * Eigenberechnung — Hauttemperatur-Abweichung gegenueber dem 30-Tage-Schnitt vor dem
     * ausgewaehlten Tag. Whoop liefert nur den Absolutwert in °C, das Delta wird hier mit der
     * eigenen Baseline berechnet.
     */
    val skinTempBaseline: Double? = null,
    val skinTempDelta: Double? = null,
    /**
     * Frank-Wunsch 2026-05-09: PAI/BioCharge/Hauttemperatur von der Amazfit T-Rex 3 als
     * zusaetzliche Werte mit eigenem Quellen-Label in den Biomarker-Bereich.
     */
    val amazfitDailyLatest: AmazfitDailyEntity? = null,
    val amazfitDailyHistory: List<AmazfitDailyEntity> = emptyList(),
    val amazfitDailyForSelectedDay: AmazfitDailyEntity? = null,
    val amazfitWorkouts: List<AmazfitWorkoutEntity> = emptyList(),
    val amazfitWorkoutsForSelectedDay: List<AmazfitWorkoutEntity> = emptyList(),
    /** Oura-Ring-Daten fuer den aktuell ausgewaehlten Tag (Frank-Wunsch 2026-05-10). */
    val ouraReadinessForSelectedDay: OuraReadinessEntity? = null,
    val ouraSleepForSelectedDay: OuraDailySleepEntity? = null,
    val ouraActivityForSelectedDay: OuraActivityEntity? = null,
    val ouraResilienceForSelectedDay: OuraResilienceEntity? = null,
    val ouraSleepDetailsForSelectedDay: List<OuraSleepDetailEntity> = emptyList(),
    /**
     * Oura-Historie sortiert nach Tag aufsteigend (aelteste zuerst). Wird fuer die
     * Mini-Verlaufsbalken und die Plus/Minus-Anzeige zum 30-Tage-Mittel auf den Karten gebraucht.
     * Frank-Wunsch 2026-05-10 (Etappe D).
     */
    val ouraReadinessHistory: List<OuraReadinessEntity> = emptyList(),
    val ouraSleepHistory: List<OuraDailySleepEntity> = emptyList(),
    val ouraActivityHistory: List<OuraActivityEntity> = emptyList(),
    val ouraResilienceHistory: List<OuraResilienceEntity> = emptyList(),
    /**
     * Performance-Audit E1 (2026-05-10): vorberechnete Chart-Punkte pro Metric-Key. Frueher liefen
     * die mapNotNull-Ketten pro Recomposition von BiomarkerCardForId (~22 Ketten, jede O(N) ueber
     * bis zu 365 Datenpunkte) — jetzt einmalig im VM.
     */
    val chartData: BiomarkerChartData = BiomarkerChartData(),
    /**
     * Performance 2026-05-23: Tiefschlaf-/REM-/Wachzeit-Verlauf werden EINMAL hier im
     * VM (Default-Dispatcher) vorberechnet — vorher lief das teure groupBy+sort ueber
     * die volle Historie bei jedem Rein-Scrollen der Karte auf dem Main-Thread und
     * verursachte Frame-Spitzen beim schnellen Fling. Werte sind identisch (gleiche
     * Funktion). null = noch nicht berechnet → Karte faellt auf lokale Berechnung zurueck.
     */
    val deepSleepDerived: DeepSleepDerived? = null,
    val remSleepDerived: RemSleepDerived? = null,
    val wakeTimeDerived: WakeTimeDerived? = null,
)

/**
 * Vorberechneter Cache aller Chart-Punkte fuer den Biomarker-Screen.
 *
 * - `pointsLast70`: Last 70 Tage Slice — was die Chart-Cards in der normalen Ansicht zeigen.
 * - `fullPoints`: vollstaendige Historie — was die MetricHistoryCard fuer Average/Trendlinie nutzt.
 * - Keys sind `MetricKey`-Konstanten (HRV, RHR, SLEEP_TOTAL, ...).
 * - `restorativeSleepAvg30dPercent`: 30-Tage-Schnitt fuer die RestorativeSleepCard.
 *
 * @Immutable damit Compose den ganzen Cache als stable behandelt — dadurch wird BiomarkerCardForId
 *   skippable wenn sich der Cache nicht aendert.
 */
@androidx.compose.runtime.Immutable
data class BiomarkerChartData(
    val pointsLast70: Map<String, List<Pair<Long, Double>>> = emptyMap(),
    val fullPoints: Map<String, List<Pair<Long, Double>>> = emptyMap(),
    val restorativeSleepAvg30dPercent: Double? = null,
)

/** Effective body histories include the retained HC cache and direct Zepp measurements. */
data class WeightState(
    val histories: Map<String, List<Pair<Long, Double>>> = emptyMap(),
    val averages30d: Map<String, Double?> = emptyMap(),
    val zeppAvailable: Boolean = false,
    val error: String? = null,
    val lastReadAtMs: Long = 0L,
    val isLoading: Boolean = false,
) {
    fun history(metric: BodyMetric): List<Pair<Long, Double>> = histories[metric.repositoryKey].orEmpty()
    fun latest(metric: BodyMetric): Double? = history(metric).lastOrNull()?.second
    fun average(metric: BodyMetric): Double? = averages30d[metric.repositoryKey]
}

@HiltViewModel
class BiomarkerViewModel
@Inject
constructor(
    private val repo: WhoopRepository,
    private val amazfitRepo: AmazfitRepository,
    private val ouraRepo: OuraRepository,
    private val scheduler: BackgroundScheduler,
    private val cardOrderRepo: BiomarkerCardOrderRepository,
    private val cardColorRepo: de.frank.entropyreducer.data.repository.CardColorRepository,
    private val syncCoordinator: SyncCoordinator,
    private val healthConnect: HealthConnectManager,
    statusObserver: StatusObserver,
    private val settings: AppSettings,
    private val zeppBodyRepository: ZeppBodyRepository,
    // Frank-Wunsch 2026-06-19 (Sync-Etappe 1.1): API-Sync beim Oeffnen des Biomarker-Tabs.
    private val foregroundSync: de.frank.entropyreducer.domain.usecase.ForegroundSyncManager,
) : ViewModel() {

    private val _refreshing = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _selectedDate = MutableStateFlow(java.time.LocalDate.now())

    val weight: StateFlow<WeightState> = combine(
        zeppBodyRepository.observeHistories(),
        zeppBodyRepository.status,
    ) { histories, status ->
        val sorted = histories.mapValues { (_, points) -> points.sortedBy { it.first } }
        val now = System.currentTimeMillis()
        val cutoff = now - 30L * 24 * 60 * 60 * 1000
        WeightState(
            histories = sorted,
            averages30d = sorted.mapValues { (_, points) ->
                points.filter { it.first in cutoff..now }.map { it.second }
                    .takeIf { it.isNotEmpty() }?.average()
            },
            zeppAvailable = zeppBodyRepository.isAvailable(),
            error = status.error?.let { "Zepp öffnen und erneut aktualisieren" },
            lastReadAtMs = status.lastReadAtMs,
            isLoading = status.isLoading,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightState())

    /**
     * Frank-Wunsch 2026-05-18: Map cardId -> ColorIndex (0-29). Wird vom
     * BiomarkerScreen genutzt um pro Karte den Hintergrund-Override zu setzen
     * (siehe [LIGHT_CARD_COLORS] und [GlassCard.backgroundOverride]).
     * Leere Map = noch keine Farben gewaehlt → alle Karten Standard.
     */
    val cardColors: StateFlow<Map<String, Int>> =
        cardColorRepo.cardColors.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyMap(),
        )

    /** Setzt die Hintergrundfarbe einer Karte. Index 0 = Standard (Override entfernt). */
    fun setCardColor(cardId: String, colorIndex: Int, isDark: Boolean = false) {
        viewModelScope.launch { cardColorRepo.setCardColor(cardId, colorIndex, isDark) }
    }

    init {
        // Frank-Wunsch 2026-05-17: Persistenten Footer beim App-Start aus
        // DataStore laden — damit "Letzter Sync: 17.05. 14:32 …" auch nach
        // Force-Stop oder Geraete-Neustart sichtbar bleibt. Reine UI-Anzeige, kein Sync.
        viewModelScope.launch {
            val savedFooter = settings.lastRefreshFooterFlow.first()
            if (savedFooter.isNotBlank() && _message.value == null) {
                // A persisted HC body count is not a successful direct Zepp read.
                _message.value = savedFooter.split(" · ")
                    .filterNot { it.startsWith("Health Connect ") }
                    .joinToString(" · ")
            }
        }
        // Body data is observed from the cache; initialization never calls the Zepp API.
    }

    /**
     * Frank-Wunsch 2026-06-19 (Sync-Etappe 1.1): Beim Sichtbarwerden des Biomarker-Tabs die
     * teuren Fitness-APIs (Whoop/Oura/Health Connect/Kalender) aktualisieren — und NUR
     * dann (nicht bei jedem App-Start). So zahlt Frank den API-Verkehr nur, wenn er die Werte
     * wirklich anschaut. Setzt zugleich den 8h-Timer neu (siehe [ForegroundSyncManager]); dessen
     * apiMutex verhindert parallele Doppellaeufe (z.B. mit dem 8h-Automatik-Sync beim Foreground).
     * Wird vom BiomarkerHostScreen bei jedem ON_RESUME aufgerufen (der Tab nutzt saveState, daher
     * reicht ein einmaliger LaunchedEffect nicht).
     */
    fun onBiomarkerOpened() {
        viewModelScope.launch { foregroundSync.syncApisNow("Biomarker-Tab") }
    }

    /** Manual body refresh; the repository owns loading, errors and the persistent history. */
    fun refreshWeight() {
        viewModelScope.launch(Dispatchers.IO) { syncBody() }
    }

    private suspend fun syncBody(): Result<Int> = try {
        val count = zeppBodyRepository.syncToCache()
        Result.success(count)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Diag.w(DiagnosticArea.BIOMARKER, "BiomarkerVM", "Zepp-Körperwerte nicht aktualisiert", error)
        _message.value = "Zepp öffnen und erneut aktualisieren"
        Result.failure(error)
    }

    /**
     * Aktuelle Reihenfolge der Biomarker-Karten (Frank-Wunsch 2026-05-10). Wird vom Screen via
     * collectAsState() beobachtet. Neu hinzugefuegte Karten (z.B. nach App-Update) werden vom
     * Repository automatisch ans Ende angehaengt.
     */
    val cardOrder: StateFlow<List<String>> =
        cardOrderRepo.orderedCardIds.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            BiomarkerCardId.DEFAULT_ORDER,
        )

    /**
     * Speichert die neue Reihenfolge nach jedem Drag & Drop. Wird vom Screen im onMove-Callback der
     * reorderable LazyColumn aufgerufen.
     *
     * Frank-Wunsch 2026-05-10: Nach jedem Reorder soll automatisch ein Drive-Backup-Sync laufen.
     * requestSync() ist debounced (1500 ms) — wenn Frank schnell mehrere Karten hintereinander
     * verschiebt, wird das zu einem einzigen Upload zusammengefasst statt 5x hochzuladen. Wenn
     * Drive-Backup deaktiviert oder kein Konto verbunden ist, ist requestSync() ein No-Op.
     */
    fun saveCardOrder(newOrder: List<String>) {
        viewModelScope.launch {
            cardOrderRepo.saveOrder(newOrder)
            syncCoordinator.requestSync("Biomarker: Kartenreihenfolge geaendert")
        }
    }

    /**
     * Setzt die Reihenfolge auf die Werks-Einstellung zurueck. Wird vom Settings-Menue oder einem
     * Long-Press-Hold-Reset-Knopf ausgeloest. Auch hier triggern wir einen Drive-Sync, damit der
     * Reset auf anderen Geraeten durchschlaegt.
     */
    fun resetCardOrder() {
        viewModelScope.launch {
            cardOrderRepo.resetToDefault()
            syncCoordinator.requestSync("Biomarker: Kartenreihenfolge zurueckgesetzt")
        }
    }

    /**
     * Liefert das Set ALLER Health-Connect-Permission-Strings, die beim Permission-Dialog
     * angefordert werden sollen. Frank-Wunsch 2026-05-10: prophylaktisch ALLE Datentypen erlauben
     * damit zukuenftige Plugins ohne erneuten Permission-Flow auskommen.
     */
    fun allHealthConnectPermissions(): Set<String> = healthConnect.requiredReadPermissions()

    /**
     * Frank-Wunsch 2026-05-10: Im Nachhinein einzelne HC-Permissions zurueckziehen koennen. Oeffnet
     * die Health-Connect-spezifische Permissions-UI fuer unsere App.
     */
    fun openHealthConnectPermissionsEditor() = healthConnect.openAppPermissionsInHealthConnect()

    val state: StateFlow<BiomarkerUiState> =
        combine(
                combine(repo.observeLatest(), repo.observeAll(), repo.observeWorkouts()) { l, a, w
                    ->
                    WhoopBundle(l, a, w)
                },
                combine(
                    amazfitRepo.observeLatestDaily(),
                    amazfitRepo.observeAllDaily(),
                    amazfitRepo.observeAllWorkouts(),
                ) { lD, aD, w ->
                    AmazfitBundle(lD, aD, w)
                },
                combine(
                    ouraRepo.observeReadiness(),
                    ouraRepo.observeDailySleep(),
                    ouraRepo.observeActivity(),
                    ouraRepo.observeResilience(),
                    ouraRepo.observeSleepDetails(),
                ) { r, s, a, res, sd ->
                    OuraBundle(r, s, a, res, sd)
                },
                combine(
                    _refreshing,
                    _message,
                    statusObserver.observe(),
                    settings.lastWhoopSyncMsFlow,
                    _selectedDate,
                ) { r, m, b, sync, sel ->
                    StatusBundle(r, m, b, sync, sel)
                },
                combine(
                    settings.lastOuraSyncMsFlow,
                    settings.lastAmazfitSyncMsFlow,
                    settings.lastZeppBodySyncMsFlow,
                ) { o, a, h ->
                    SyncTimes(o, a, h)
                },
            ) { whoop, amazfit, oura, status, syncTimes ->
                val latest = whoop.latest
                val all = whoop.history
                val workouts = whoop.workouts
                // 30-Tage-Slice pro Emission frisch berechnen — verhindert stale
                // Fenster wenn die App ueber Mitternacht offen bleibt.
                val nowMs = System.currentTimeMillis()
                val thirtyDaysAgoMs = nowMs - 30L * 24 * 60 * 60 * 1000
                val last30 = all.filter { it.capturedAt in thirtyDaysAgoMs..nowMs }
                val selDate = status.selectedDate
                // Snapshot für den gewählten Tag finden — wenn kein Snapshot für das
                // exakte Datum existiert, wird der nächste juengere Snapshot vor dem
                // gewählten Tag genommen (Whoop syncs typischerweise einmal pro Tag).
                val selStartMs =
                    selDate
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                val selEndMs = selStartMs + 24L * 60 * 60 * 1000
                val selSnap =
                    all.lastOrNull { it.capturedAt in selStartMs until selEndMs }
                        ?: if (selDate == java.time.LocalDate.now()) latest else null

                // Workouts fuer den gewaehlten Tag — Whoop liefert dateKey im UTC-Tag,
                // wir vergleichen lokal (Frank arbeitet in lokaler Zeit) per Bereichsfilter.
                val workoutsForDay =
                    workouts
                        .filter { it.startMs in selStartMs until selEndMs }
                        .sortedBy { it.startMs }

                // Amazfit-Tag-Eintrag fuer den gewaehlten Tag (per date-String matchen).
                val selDateKey = selDate.toString()
                val amazfitForDay =
                    amazfit.allDaily.firstOrNull { it.date == selDateKey }
                        ?: if (selDate == java.time.LocalDate.now()) amazfit.latestDaily else null

                // Amazfit-Workouts fuer den gewaehlten Tag.
                val amazfitWorkoutsForDay =
                    amazfit.workouts
                        .filter { it.startMs in selStartMs until selEndMs }
                        .sortedBy { it.startMs }

                // Oura-Daten fuer den gewaehlten Tag — Tagesschluessel ist YYYY-MM-DD.
                // Bei "Heute" Fallback auf den juengsten Eintrag falls Sync noch nicht
                // durchgelaufen ist (Oura-Cloud hat Sync-Delay 40-50 Min nach Ring-Sync).
                val ouraReadinessForDay =
                    oura.readiness.firstOrNull { it.day == selDateKey }
                        ?: if (selDate == java.time.LocalDate.now()) {
                            oura.readiness.maxByOrNull { it.day }
                        } else {
                            null
                        }
                val ouraSleepForDay =
                    oura.dailySleep.firstOrNull { it.day == selDateKey }
                        ?: if (selDate == java.time.LocalDate.now()) {
                            oura.dailySleep.maxByOrNull { it.day }
                        } else {
                            null
                        }
                val ouraActivityForDay =
                    oura.activity.firstOrNull { it.day == selDateKey }
                        ?: if (selDate == java.time.LocalDate.now()) {
                            oura.activity.maxByOrNull { it.day }
                        } else {
                            null
                        }
                val ouraResilienceForDay =
                    oura.resilience.firstOrNull { it.day == selDateKey }
                        ?: if (selDate == java.time.LocalDate.now()) {
                            oura.resilience.maxByOrNull { it.day }
                        } else {
                            null
                        }
                val ouraSleepDetailsForDay =
                    oura.sleepDetail
                        .filter { it.day == selDateKey }
                        .ifEmpty {
                            if (selDate == java.time.LocalDate.now()) {
                                val latestDay = oura.sleepDetail.maxByOrNull { it.day }?.day
                                if (latestDay != null) {
                                    oura.sleepDetail.filter { it.day == latestDay }
                                } else {
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }
                        }

                // Eigenberechnung — Erholsamer Schlaf %: (REM + Tiefschlaf) / Zeit im Bett.
                // Zeit im Bett = REM + Tiefschlaf + Leichtschlaf + Wach. So berechnet
                // Whoop den Wert (Frank-Vorgabe 2026-05-10, Whoop-Referenz 91,9%).
                val rem = selSnap?.sleepRemMinutes ?: 0
                val deep = selSnap?.sleepDeepMinutes ?: 0
                val light = selSnap?.sleepLightMinutes ?: 0
                val awake = selSnap?.sleepAwakeMinutes ?: 0
                val timeInBed = rem + deep + light + awake
                val restorativePct =
                    if (timeInBed > 0) {
                        (rem + deep).toDouble() / timeInBed.toDouble() * 100.0
                    } else null

                // Eigenberechnung — Hauttemperatur-Delta. Baseline = Mittel der letzten
                // 30 Tage VOR dem ausgewaehlten Tag. Mindestens 7 Werte sonst NICHT
                // anzeigen damit der Wert verlaesslich ist.
                val baselineEndMs = selStartMs
                val baselineStartMs = baselineEndMs - 30L * 24 * 60 * 60 * 1000
                val baselineValues =
                    all.filter { it.capturedAt in baselineStartMs until baselineEndMs }
                        .mapNotNull { it.skinTempCelsius }
                val baseline = if (baselineValues.size >= 7) baselineValues.average() else null
                val skinTempDelta =
                    if (baseline != null && selSnap?.skinTempCelsius != null) {
                        selSnap.skinTempCelsius - baseline
                    } else null

                // Performance-Audit E1 (2026-05-10): vorberechnete Chart-Punkte pro Metric-Key.
                // Frueher liefen ~22 mapNotNull-Ketten pro Recomposition von BiomarkerCardForId.
                // Jetzt einmalig hier im combine{} (laeuft auf Dispatchers.Default).
                val seventyDaysAgoMs = System.currentTimeMillis() - 70L * 24 * 60 * 60 * 1000
                val last70Slice = all.filter { it.capturedAt >= seventyDaysAgoMs }
                val todayStartMs =
                    java.time.LocalDate.now()
                        .atStartOfDay(java.time.ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                val chartData =
                    buildChartData(
                        historyLast70 = last70Slice,
                        fullHistory = all,
                        history30Days = last30,
                        todayStartMs = todayStartMs,
                        amazfitWorkouts = amazfit.workouts,
                    )

                // Performance 2026-05-23: Sleep-Verlauf-Derivate hier (Default-Dispatcher,
                // off-main) vorberechnen — identische Eingaben wie der Karten-Aufruf
                // (selSnap ?: latest, all). Spart das groupBy+sort beim Rein-Scrollen.
                val sleepSnap = selSnap ?: latest
                val deepDerived = deepSleepDerived(sleepSnap, all)
                val remDerived = remSleepDerived(sleepSnap, all)
                val wakeDerived = wakeTimeDerived(sleepSnap, all)

                BiomarkerUiState(
                    latest = latest,
                    history = all,
                    history30Days = last30,
                    selectedDate = selDate,
                    selectedSnapshot = selSnap,
                    isRefreshing = status.refreshing,
                    message = status.message,
                    statusBreakdown = status.breakdown,
                    lastWhoopSyncMs = status.lastWhoopSyncMs,
                    lastOuraSyncMs = syncTimes.oura,
                    lastAmazfitSyncMs = syncTimes.amazfit,
                    lastZeppBodySyncMs = syncTimes.zepp,
                    workouts = workouts,
                    workoutsForSelectedDay = workoutsForDay,
                    restorativeSleepPercent = restorativePct,
                    skinTempBaseline = baseline,
                    skinTempDelta = skinTempDelta,
                    amazfitDailyLatest = amazfit.latestDaily,
                    amazfitDailyHistory = amazfit.allDaily,
                    amazfitDailyForSelectedDay = amazfitForDay,
                    amazfitWorkouts = amazfit.workouts,
                    amazfitWorkoutsForSelectedDay = amazfitWorkoutsForDay,
                    ouraReadinessForSelectedDay = ouraReadinessForDay,
                    ouraSleepForSelectedDay = ouraSleepForDay,
                    ouraActivityForSelectedDay = ouraActivityForDay,
                    ouraResilienceForSelectedDay = ouraResilienceForDay,
                    ouraSleepDetailsForSelectedDay = ouraSleepDetailsForDay,
                    ouraReadinessHistory = oura.readiness.sortedBy { it.day },
                    ouraSleepHistory = oura.dailySleep.sortedBy { it.day },
                    ouraActivityHistory = oura.activity.sortedBy { it.day },
                    ouraResilienceHistory = oura.resilience.sortedBy { it.day },
                    chartData = chartData,
                    deepSleepDerived = deepDerived,
                    remSleepDerived = remDerived,
                    wakeTimeDerived = wakeDerived,
                )
            }
            // Performance-Audit Loop 1 (2026-05-10): combine{} enthaelt 30+ filter/sortedBy/
            // mapNotNull-Operationen ueber 365-Tage-Listen. Ohne flowOn liefen die auf dem
            // Collector-Dispatcher (Main bei stateIn(viewModelScope)). Bei jedem Sync oder
            // Selected-Date-Wechsel war das ein sichtbarer Frame-Drop. Default-Dispatcher
            // ist der richtige fuer CPU-bound Arbeit (sortedBy/filter).
            .flowOn(Dispatchers.Default)
            // Frank-Wunsch 2026-05-23: 60s (wie Tasks/Analyse/Forscher) statt 5s —
            // beim Tab-Wechsel bleiben die Biomarker-Daten im Speicher und muessen
            // nicht bei jeder Rueckkehr neu berechnet werden.
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), BiomarkerUiState())

    fun selectDate(date: java.time.LocalDate) {
        _selectedDate.value = date
    }

    fun goToToday() {
        _selectedDate.value = java.time.LocalDate.now()
    }

    /**
     * Frank-Wunsch 2026-05-17: Manuelle Override-Werte fuer ein Training
     * speichern. Wird vom Edit-Dialog in der Hero-Card ausgeloest.
     * Delegiert an AmazfitRepository.applyManualOverrides.
     */
    fun applyWorkoutOverrides(
        trackId: String,
        overrides: de.frank.entropyreducer.presentation.amazfit.ManualWorkoutOverrides,
    ) {
        viewModelScope.launch {
            amazfitRepo.applyManualOverrides(
                trackId = trackId,
                durationSeconds = overrides.durationSeconds,
                distanceMeters = overrides.distanceMeters,
                avgPaceSecPerKm = overrides.avgPaceSecPerKm,
                maxPaceSecPerKm = overrides.maxPaceSecPerKm,
                avgHeartRate = overrides.avgHeartRate,
                maxHeartRate = overrides.maxHeartRate,
                altitudeGainMeters = overrides.altitudeGainMeters,
                altitudeLossMeters = overrides.altitudeLossMeters,
                cadence = overrides.cadence,
                strideLengthCm = overrides.strideLengthCm,
                calories = overrides.calories,
            )
        }
    }

    fun shiftDay(delta: Int) {
        _selectedDate.value = _selectedDate.value.plusDays(delta.toLong())
    }

    fun refreshNow() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            _message.value = "Wird synchronisiert: Whoop · Training · Oura · Zepp …"
            try {
                val whoopJob = async {
                    runCatching { repo.syncLastDays(365) }.getOrElse {
                        if (it is CancellationException) throw it
                        Result.failure(it)
                    }
                }
                val ouraJob = async {
                    runCatching { ouraRepo.syncLastDays(365) }.getOrElse {
                        if (it is CancellationException) throw it
                        Result.failure(it)
                    }
                }
                // Training remains on Health Connect; only body data switches to Zepp.
                val trainingJob = async {
                    runCatching { amazfitRepo.mergeFromHealthConnect(days = 30) }.getOrElse {
                        if (it is CancellationException) throw it
                        0
                    }
                }
                val bodyJob = async(Dispatchers.IO) { syncBody() }
                val whoopRes = whoopJob.await()
                val ouraRes = ouraJob.await()
                val trainingCount = trainingJob.await()
                val bodyRes = bodyJob.await()
                val ouraCount = ouraRes.getOrNull()?.let { m ->
                    if (m is Map<*, *>) m.values.filterIsInstance<Int>().sum() else 0
                } ?: 0
                val bodySummary = bodyRes.fold(
                    onSuccess = { "Zepp $it" },
                    onFailure = { "Zepp öffnen und erneut aktualisieren" },
                )
                val now = java.time.ZonedDateTime.now()
                val ts = "%02d.%02d. %02d:%02d".format(
                    now.dayOfMonth, now.monthValue, now.hour, now.minute,
                )
                val summary = "Whoop ${whoopRes.getOrNull() ?: 0} · " +
                    "Training ${trainingCount.coerceAtLeast(0)} · Oura $ouraCount · $bodySummary"
                val finalMessage = "$ts · $summary"
                _message.value = finalMessage
                runCatching {
                    settings.setLastRefreshFooter(finalMessage,
                        if (bodyRes.isSuccess && whoopRes.isSuccess && ouraRes.isSuccess) {
                            System.currentTimeMillis()
                        } else settings.lastRefreshFooterAtMsFlow.first())
                }
                syncCoordinator.requestSync(
                    "Biomarker: Voll-Refresh aller Quellen (Whoop/Oura/Training/Zepp)",
                    SyncCoordinator.BIOMARKER_DEBOUNCE_MS,
                )
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

}

/**
 * Performance-Audit E1 (2026-05-10): vorberechneter Chart-Daten-Cache fuer den Biomarker-Screen.
 * Frueher liefen ~22 mapNotNull-Ketten pro Recomposition von BiomarkerCardForId. Jetzt einmalig
 * hier, ausgeloest nur bei tatsaechlicher Aenderung des state.history (laeuft auf
 * Dispatchers.Default via flowOn).
 *
 * Keys sind die String-Konstanten aus `de.frank.entropyreducer.presentation.dashboard4.MetricKey`
 * (HRV, RHR, RESPIRATORY, SPO2, SKIN_TEMP, SLEEP_PERF, SLEEP_TOTAL, SLEEP_EFFICIENCY,
 * SLEEP_CONSISTENCY, SLEEP_DEBT, KILOJOULES, STRAIN).
 */
private fun buildChartData(
    historyLast70: List<BiomarkerSnapshotEntity>,
    fullHistory: List<BiomarkerSnapshotEntity>,
    history30Days: List<BiomarkerSnapshotEntity>,
    todayStartMs: Long,
    amazfitWorkouts: List<de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity> =
        emptyList(),
): BiomarkerChartData {
    // Liste aller Metric-Extractor-Paare. Jeder Extractor liest einen Wert aus dem
    // Snapshot und liefert ihn als Double (oder null). Reihenfolge ist irrelevant.
    val extractors: List<Pair<String, (BiomarkerSnapshotEntity) -> Double?>> =
        listOf(
            "hrv" to { it.hrvMs },
            "rhr" to { it.restingHeartRate?.toDouble() },
            "respiratory" to { it.respiratoryRate },
            "spo2" to { it.spo2Percent },
            "skin_temp" to { it.skinTempCelsius },
            "sleep_perf" to { it.sleepPerformance?.toDouble() },
            // Frank-Wunsch 2026-05-16: Pattern "Schlafdauer" und Detail-Liste ohne
            // Wachzeit (Tief + REM + Leicht). Whoop liefert sleepTotalMinutes als
            // Zeit im Bett — Wachzeit aktiv abziehen.
            "sleep_total" to { snap ->
                snap.sleepTotalMinutes?.let { total ->
                    val awake = snap.sleepAwakeMinutes ?: 0
                    (total - awake).coerceAtLeast(0).toDouble().takeIf { it > 0 }
                }
            },
            "sleep_efficiency" to { it.sleepEfficiencyPercent?.toDouble() },
            "sleep_consistency" to { it.sleepConsistencyPercent?.toDouble() },
            "sleep_debt" to { it.sleepDebtMinutes?.toDouble() },
            "strain" to { it.dayStrain },
        )

    // Frank-Bug 2026-05-13: Crash "Key X was already used" in LazyColumn — wenn
    // mehrere Snapshots denselben capturedAt-Timestamp haben (Whoop kann das in
    // seltenen DST-/Sync-Edge-Cases), nutzte BiomarkerDetailScreen den Timestamp
    // als LazyColumn-Key und stuerzte ab. Defensive Dedup hier: per Timestamp
    // den juengsten Snapshot behalten (durch Ueberschreiben in toMap).
    fun List<Pair<Long, Double>>.dedupByTimestamp(): List<Pair<Long, Double>> = toMap().toList()

    val pointsLast70 = mutableMapOf<String, List<Pair<Long, Double>>>()
    val fullPoints = mutableMapOf<String, List<Pair<Long, Double>>>()
    for ((key, extractor) in extractors) {
        pointsLast70[key] =
            historyLast70
                .mapNotNull { snap -> extractor(snap)?.let { snap.capturedAt to it } }
                .dedupByTimestamp()
        fullPoints[key] =
            fullHistory
                .mapNotNull { snap -> extractor(snap)?.let { snap.capturedAt to it } }
                .dedupByTimestamp()
    }

    // KILOJOULES braucht zusaetzlich Today-Filter (Tag baut sich auf, heute = unvollstaendig)
    // und Whoop-kJ → kcal Umrechnung (Faktor 4.184).
    pointsLast70["kilojoules"] =
        historyLast70
            .filter { it.capturedAt < todayStartMs }
            .mapNotNull { snap -> snap.dayKilojoules?.let { snap.capturedAt to (it / 4.184) } }
            .dedupByTimestamp()
    fullPoints["kilojoules"] =
        fullHistory
            .filter { it.capturedAt < todayStartMs }
            .mapNotNull { snap -> snap.dayKilojoules?.let { snap.capturedAt to (it / 4.184) } }
            .dedupByTimestamp()

    // Frank-Wunsch 2026-05-18: VO2max-Verlauf aus VO2max-faehigen Workouts
    // (Laufen/Trail/Walk) berechnen. Pro Workout den Whoop-Ruhepuls fuer
    // den Workout-Tag suchen (Vortag-Fallback), dann ACSM-Formel anwenden.
    // Wert ist ml/(kg·min). Sortiert nach Workout-Start aufsteigend.
    //
    // Frank-Wunsch 2026-05-18 (Nachtrag): Loesch-Robustheit. mapNotNull filtert
    // automatisch Workouts ohne VO2max-Wert raus (z.B. Krafttraining,
    // Crosstrainer, Yoga — computeVo2MaxOrNull liefert null wenn isVo2MaxSport
    // false ist). Wenn Frank das letzte VO2-faehige Training oder ein anderes
    // VO2max-Training loescht, wird durch Room's invalidation tracker der
    // amazfitWorkouts-Flow neu emittet → buildChartData laeuft neu → die
    // Liste vo2MaxAll hat einen Eintrag weniger → lastOrNull() in
    // MiniVo2MaxCard zeigt automatisch den naechst-juengsten VO2max-Wert.
    // Wenn das juengste Training z.B. Krafttraining ist, taucht es gar nicht
    // erst auf — die Mini-Karte zeigt den juengsten Trail-/Lauf-Wert.
    val vo2MaxAll: List<Pair<Long, Double>> =
        amazfitWorkouts
            .mapNotNull { w ->
                val restingHr =
                    de.frank.entropyreducer.presentation.amazfit.findRestingHrForWorkoutDay(
                        fullHistory,
                        w.startMs,
                    )
                de.frank.entropyreducer.presentation.dashboard4
                    .computeVo2MaxOrNull(w, restingHr)
                    ?.let { w.startMs to it }
            }
            .sortedBy { it.first }
    val seventyDaysAgoMs = todayStartMs - 70L * 24 * 60 * 60 * 1000
    pointsLast70["vo2max"] = vo2MaxAll.filter { it.first >= seventyDaysAgoMs }
    fullPoints["vo2max"] = vo2MaxAll

    // Erholsamer Schlaf 30-Tage-Schnitt (RestorativeSleepCard).
    val restorativeAvg =
        history30Days
            .mapNotNull { snap ->
                val total = snap.sleepTotalMinutes ?: return@mapNotNull null
                val rem = snap.sleepRemMinutes ?: return@mapNotNull null
                val deep = snap.sleepDeepMinutes ?: return@mapNotNull null
                if (total > 0) (rem + deep).toDouble() / total * 100.0 else null
            }
            .takeIf { it.isNotEmpty() }
            ?.average()

    return BiomarkerChartData(
        pointsLast70 = pointsLast70,
        fullPoints = fullPoints,
        restorativeSleepAvg30dPercent = restorativeAvg,
    )
}
