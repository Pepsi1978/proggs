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
import de.frank.entropyreducer.data.repository.HealthConnectRepository
import de.frank.entropyreducer.data.repository.OuraRepository
import de.frank.entropyreducer.data.repository.WhoopRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.domain.status.StatusObserver
import de.frank.entropyreducer.workers.BackgroundScheduler
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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
private data class SyncTimes(val oura: Long, val amazfit: Long, val healthConnect: Long)

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
    val lastHealthConnectSyncMs: Long = 0L,
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

/**
 * Weight-State fuer die Health-Connect-Mini-Karte (Frank-Wunsch 2026-05-10). Wird separat vom
 * Hauptstate gehalten, weil Health Connect kein Flow ist sondern suspend-basiert — der Wert wird
 * beim Init und beim Refresh einmalig gelesen.
 *
 * Mit - permissionGranted=false: Karte zeigt "Tippen um zu erlauben" Mit - permissionGranted=true +
 * latestKg=null: Karte zeigt "Keine Daten in HC" Mit - permissionGranted=true + latestKg!=null:
 * Karte zeigt Wert + Delta
 */
@androidx.compose.runtime.Immutable
data class WeightState(
    val healthConnectAvailable: Boolean = false,
    /**
     * True wenn ALLE drei Permissions (Weight, BodyFat, LeanBodyMass) erteilt sind. Wird vom
     * Permission-Launcher in einem Rutsch angefordert — wenn Frank nur Weight erlaubt aber nicht
     * BodyFat, ist das hier false. Damit stoppt Frank's Tap-to-Refresh-Loop und zeigt stattdessen
     * einen Hinweis.
     */
    val permissionGranted: Boolean = false,
    val latestKg: Double? = null,
    val avg30dKg: Double? = null,
    val history30d: List<Pair<Long, Double>> = emptyList(),
    /** Frank-Wunsch 2026-05-10: Koerperfett aus Health Connect (Smart-Scale via Zepp). */
    val latestBodyFatPercent: Double? = null,
    val avg30dBodyFatPercent: Double? = null,
    val bodyFatHistory30d: List<Pair<Long, Double>> = emptyList(),
    /** Magermasse — alles ausser Fett (Muskeln + Wasser + Knochen). */
    val latestLeanBodyMassKg: Double? = null,
    val avg30dLeanBodyMassKg: Double? = null,
    val leanBodyMassHistory30d: List<Pair<Long, Double>> = emptyList(),
    /** Koerperwasser in kg (Frank-Wunsch 2026-05-10). */
    val latestBodyWaterMassKg: Double? = null,
    val avg30dBodyWaterMassKg: Double? = null,
    val bodyWaterMassHistory30d: List<Pair<Long, Double>> = emptyList(),
    /** Knochenmasse in kg. */
    val latestBoneMassKg: Double? = null,
    val avg30dBoneMassKg: Double? = null,
    val boneMassHistory30d: List<Pair<Long, Double>> = emptyList(),
    /** Zeitstempel des letzten erfolgreichen Reads — fuer User-Feedback bei Tap-Refresh. */
    val lastReadAtMs: Long = 0L,
    /** True waehrend gerade gelesen wird (zeigt einen Spinner auf der Karte). */
    val isLoading: Boolean = false,
)

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
    private val hcValueDao: de.frank.entropyreducer.data.local.dao.HealthConnectValueDao,
    private val healthConnectRepository:
        de.frank.entropyreducer.data.repository.HealthConnectRepository,
    // Frank-Wunsch 2026-06-19 (Sync-Etappe 1.1): API-Sync beim Oeffnen des Biomarker-Tabs.
    private val foregroundSync: de.frank.entropyreducer.domain.usecase.ForegroundSyncManager,
) : ViewModel() {

    private val _refreshing = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _selectedDate = MutableStateFlow(java.time.LocalDate.now())

    // Health-Connect-Gewicht: separater State, wird beim Init und nach jedem
    // erfolgreichen Permission-Grant aktualisiert.
    private val _weight = MutableStateFlow(WeightState())
    val weight: StateFlow<WeightState> = _weight

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
    fun setCardColor(cardId: String, colorIndex: Int) {
        viewModelScope.launch { cardColorRepo.setCardColor(cardId, colorIndex) }
    }

    init {
        // Frank-Wunsch 2026-05-17: Persistenten Footer beim App-Start aus
        // DataStore laden — damit "Letzter Sync: 17.05. 14:32 …" auch nach
        // Force-Stop oder Geraete-Neustart sichtbar bleibt. Reine UI-Anzeige, kein Sync.
        viewModelScope.launch {
            val savedFooter = settings.lastRefreshFooterFlow.first()
            if (savedFooter.isNotBlank() && _message.value == null) {
                _message.value = savedFooter
            }
        }
        // Frank-Wunsch 2026-05-23: KEIN refreshNow() mehr beim Oeffnen des Biomarker-Tabs
        // (Moment 3 raus). Die Daten-APIs (Whoop/Oura/Kalender) synchronisieren NUR
        // noch beim frischen App-Start (zentral im StartupViewModel) und beim manuellen
        // Aktualisieren-Knopf. Der Tab zeigt die bereits in der DB liegenden Daten.
        //
        // Gewicht/Koerperwerte werden jetzt — wie Whoop/Oura — in der DB (hc_value_cache)
        // gehalten. Beim Tab-Oeffnen laden wir sie NUR aus dem Cache (schnell, kein Health-
        // Connect-Live-Read, kein Sync). Gefuellt wird der Cache beim frischen App-Start
        // (HealthConnectRepository.syncToCache) und beim manuellen Aktualisieren-Knopf.
        viewModelScope.launch { loadWeightFromCache() }
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

    /**
     * Liest Gewicht, Koerperfett und Magermasse aus Health Connect. Setzt waehrend des Lesens
     * [WeightState.isLoading] = true damit die UI einen Spinner anzeigen kann.
     *
     * Frank-Wunsch 2026-05-10: Tap auf eine Mini-Karte ruft genau diese Methode auf — sofortiges
     * Refresh ohne in die Settings zu gehen. Wenn Permission fehlt, zeigt der Karten-Tap
     * stattdessen einen Permission- Dialog (siehe Screen-Logik). Permission gilt fuer ALLE drei
     * Records gleichzeitig — wenn Frank nur eine Permission erteilt, wird permissionGranted
     * weiterhin false sein und die Karten zeigen "Tippen".
     */
    fun refreshWeight() {
        viewModelScope.launch(Dispatchers.IO) {
            // Performance-Audit Loop 1 (2026-05-10): 15 parallele HC-Reads auf IO.
            //
            // BUGFIX (Frank-Befund 2026-05-10 abend, S23 Ultra): Wenn IRGENDEINER der
            // 15 async-Reads warf (z.B. SecurityException weil eine Permission auf
            // dem Geraet anders eingestuft wird, oder ein Read fuer 730 Tage die
            // Plattform-Grenze ueberschreitet), brach die gesamte Coroutine VOR der
            // _weight.value = WeightState(...) Zeile ab. UI blieb im isLoading=true
            // Zustand und Frank dachte "Aktualisieren-Button macht nichts".
            //
            // Fix: jeden Read in runCatching wrappen. Exception → null bzw. emptyList()
            // als Default, plus debug-Log. WeightState wird IMMER gesetzt.
            val tag = "BiomarkerVM"
            Diag.i(DiagnosticArea.BIOMARKER, tag, "refreshWeight() start")
            val available = healthConnect.isAvailable()
            if (!available) {
                Diag.i(DiagnosticArea.BIOMARKER, tag, "refreshWeight: HC NOT available")
                _weight.value = WeightState(healthConnectAvailable = false)
                return@launch
            }
            val weightOk =
                runCatching { healthConnect.hasWeightReadPermission() }.getOrDefault(false)
            val bodyFatOk =
                runCatching { healthConnect.hasBodyFatReadPermission() }.getOrDefault(false)
            val leanOk =
                runCatching { healthConnect.hasLeanBodyMassReadPermission() }.getOrDefault(false)
            val waterOk =
                runCatching { healthConnect.hasBodyWaterMassReadPermission() }.getOrDefault(false)
            val boneOk =
                runCatching { healthConnect.hasBoneMassReadPermission() }.getOrDefault(false)
            val historyOk =
                runCatching { healthConnect.hasHistoryReadPermission() }.getOrDefault(false)
            Diag.i(DiagnosticArea.BIOMARKER, 
                tag,
                "refreshWeight perms: weight=$weightOk bf=$bodyFatOk lean=$leanOk water=$waterOk bone=$boneOk hist=$historyOk",
            )
            if (!(weightOk && bodyFatOk && leanOk && waterOk && boneOk && historyOk)) {
                Diag.w(DiagnosticArea.BIOMARKER, 
                    tag,
                    "refreshWeight: nicht alle Permissions erteilt — Karten zeigen 'Tippen'",
                )
                _weight.value =
                    WeightState(healthConnectAvailable = true, permissionGranted = false)
                return@launch
            }
            // Loading-State setzen damit die Karten einen Spinner zeigen koennen
            _weight.value = _weight.value.copy(isLoading = true)
            // Alle 15 HC-Reads parallel + defensiv mit runCatching. Jeder Read kann
            // einzeln scheitern ohne dass die anderen ausbleiben und ohne dass die
            // UI im isLoading-Zustand stecken bleibt.
            suspend fun <T> safeAsync(
                label: String,
                default: T,
                block: suspend () -> T,
            ): kotlinx.coroutines.Deferred<T> = async {
                runCatching { block() }
                    .onFailure { Diag.w(DiagnosticArea.BIOMARKER, tag, "HC-Read '$label' fehlgeschlagen", it) }
                    .getOrDefault(default)
            }
            val latestKgD =
                safeAsync("latestKg", null as Double?) { healthConnect.readLatestWeightKg() }
            val avgKgD = safeAsync("avgKg", null as Double?) { healthConnect.averageWeightKg(730) }
            val historyKgD =
                safeAsync("historyKg", emptyList<Pair<Long, Double>>()) {
                    healthConnect.readWeightHistory(730)
                }
            val latestBfD =
                safeAsync("latestBf", null as Double?) { healthConnect.readLatestBodyFatPercent() }
            val avgBfD =
                safeAsync("avgBf", null as Double?) { healthConnect.averageBodyFatPercent(730) }
            val historyBfD =
                safeAsync("historyBf", emptyList<Pair<Long, Double>>()) {
                    healthConnect.readBodyFatHistory(730)
                }
            val latestLeanD =
                safeAsync("latestLean", null as Double?) {
                    healthConnect.readLatestLeanBodyMassKg()
                }
            val avgLeanD =
                safeAsync("avgLean", null as Double?) { healthConnect.averageLeanBodyMassKg(730) }
            val historyLeanD =
                safeAsync("historyLean", emptyList<Pair<Long, Double>>()) {
                    healthConnect.readLeanBodyMassHistory(730)
                }
            val latestWaterD =
                safeAsync("latestWater", null as Double?) {
                    healthConnect.readLatestBodyWaterMassKg()
                }
            val avgWaterD =
                safeAsync("avgWater", null as Double?) { healthConnect.averageBodyWaterMassKg(730) }
            val historyWaterD =
                safeAsync("historyWater", emptyList<Pair<Long, Double>>()) {
                    healthConnect.readBodyWaterMassHistory(730)
                }
            val latestBoneD =
                safeAsync("latestBone", null as Double?) { healthConnect.readLatestBoneMassKg() }
            val avgBoneD =
                safeAsync("avgBone", null as Double?) { healthConnect.averageBoneMassKg(730) }
            val historyBoneD =
                safeAsync("historyBone", emptyList<Pair<Long, Double>>()) {
                    healthConnect.readBoneMassHistory(730)
                }
            val latestKg = latestKgD.await()
            val avgKg = avgKgD.await()
            val historyKg = historyKgD.await()
            val latestBf = latestBfD.await()
            val avgBf = avgBfD.await()
            val historyBf = historyBfD.await()
            val latestLean = latestLeanD.await()
            val avgLean = avgLeanD.await()
            val historyLean = historyLeanD.await()
            val latestWater = latestWaterD.await()
            val avgWater = avgWaterD.await()
            val historyWater = historyWaterD.await()
            val latestBone = latestBoneD.await()
            val avgBone = avgBoneD.await()
            val historyBone = historyBoneD.await()
            Diag.i(DiagnosticArea.BIOMARKER, 
                tag,
                "refreshWeight done: latestKg=$latestKg latestBf=$latestBf historyKgCount=${historyKg.size}",
            )
            // Cross-Device-Cache (Frank-Wunsch 2026-05-10 abend): alle aus HC
            // gelesenen Werte zusaetzlich in die DB-Tabelle hc_value_cache
            // schreiben. Beim Drive-Backup wird die Tabelle mit-exportiert,
            // beim Restore auf einem anderen Geraet eingespielt. Damit landen
            // Werte vom Fold 6 auch auf dem S23 Ultra, obwohl Zepp dort nicht
            // rueckwirkend in HC schreibt.
            val now = System.currentTimeMillis()
            val cacheRows = buildList {
                fun addAll(metric: String, pairs: List<Pair<Long, Double>>) {
                    pairs.forEach {
                        add(
                            de.frank.entropyreducer.data.local.entities.HealthConnectValueEntity(
                                metric = metric,
                                timestampMs = it.first,
                                value = it.second,
                                createdAt = now,
                            )
                        )
                    }
                }
                addAll("weight", historyKg)
                addAll("body_fat", historyBf)
                addAll("lean_body_mass", historyLean)
                addAll("body_water", historyWater)
                addAll("bone_mass", historyBone)
            }
            runCatching { hcValueDao.upsertAll(cacheRows) }
                .onFailure { Diag.w(DiagnosticArea.BIOMARKER, tag, "HC-Cache-Write fehlgeschlagen", it) }

            // UI-State: HC-Live + Cache mergen. Bei doppeltem Timestamp gewinnt
            // der HC-Live-Wert (er ist frisch direkt aus der Quelle).
            val cachedAll = runCatching { hcValueDao.getAll() }.getOrDefault(emptyList())
            val mergedKg = mergeHcWithCache(historyKg, cachedAll, "weight")
            val mergedBf = mergeHcWithCache(historyBf, cachedAll, "body_fat")
            val mergedLean = mergeHcWithCache(historyLean, cachedAll, "lean_body_mass")
            val mergedWater = mergeHcWithCache(historyWater, cachedAll, "body_water")
            val mergedBone = mergeHcWithCache(historyBone, cachedAll, "bone_mass")
            // Latest = juengster aus dem gemergten Verlauf (oder HC-Live falls Cache leer)
            val effLatestKg = mergedKg.maxByOrNull { it.first }?.second ?: latestKg
            val effLatestBf = mergedBf.maxByOrNull { it.first }?.second ?: latestBf
            val effLatestLean = mergedLean.maxByOrNull { it.first }?.second ?: latestLean
            val effLatestWater = mergedWater.maxByOrNull { it.first }?.second ?: latestWater
            val effLatestBone = mergedBone.maxByOrNull { it.first }?.second ?: latestBone
            // Avg-Berechnung jetzt ueber den Merge (mehr Datenpunkte = aussagekraeftiger)
            val effAvgKg =
                mergedKg.takeIf { it.isNotEmpty() }?.map { it.second }?.average() ?: avgKg
            val effAvgBf =
                mergedBf.takeIf { it.isNotEmpty() }?.map { it.second }?.average() ?: avgBf
            val effAvgLean =
                mergedLean.takeIf { it.isNotEmpty() }?.map { it.second }?.average() ?: avgLean
            val effAvgWater =
                mergedWater.takeIf { it.isNotEmpty() }?.map { it.second }?.average() ?: avgWater
            val effAvgBone =
                mergedBone.takeIf { it.isNotEmpty() }?.map { it.second }?.average() ?: avgBone

            _weight.value =
                WeightState(
                    healthConnectAvailable = true,
                    permissionGranted = true,
                    latestKg = effLatestKg,
                    avg30dKg = effAvgKg,
                    history30d = mergedKg,
                    latestBodyFatPercent = effLatestBf,
                    avg30dBodyFatPercent = effAvgBf,
                    bodyFatHistory30d = mergedBf,
                    latestLeanBodyMassKg = effLatestLean,
                    avg30dLeanBodyMassKg = effAvgLean,
                    leanBodyMassHistory30d = mergedLean,
                    latestBodyWaterMassKg = effLatestWater,
                    avg30dBodyWaterMassKg = effAvgWater,
                    bodyWaterMassHistory30d = mergedWater,
                    latestBoneMassKg = effLatestBone,
                    avg30dBoneMassKg = effAvgBone,
                    boneMassHistory30d = mergedBone,
                    lastReadAtMs = System.currentTimeMillis(),
                    isLoading = false,
                )
            Diag.i(DiagnosticArea.BIOMARKER, 
                tag,
                "refreshWeight merged: weight=${mergedKg.size} bodyFat=${mergedBf.size} lean=${mergedLean.size} water=${mergedWater.size} bone=${mergedBone.size}",
            )
            // Frank-Wunsch 2026-05-10: einheitlicher Sync-Zeitstempel-Pool fuer
            // den 'Zuletzt synchronisiert'-Header im Biomarker-Screen.
            settings.setLastHealthConnectSync(System.currentTimeMillis())
            // Frank-Wunsch 2026-05-10 abend: nach jedem refresh sofort Backup
            // triggern damit das andere Geraet die neuen Werte beim naechsten
            // App-Start sieht.
            if (cacheRows.isNotEmpty()) {
                syncCoordinator.requestSync(
                    "Biomarker: Health-Connect-Werte aktualisiert",
                    SyncCoordinator.BIOMARKER_DEBOUNCE_MS,
                )
            }
        }
    }

    /**
     * Frank-Wunsch 2026-05-10 abend: HC-Live-Werte und Cross-Device-Cache mergen. Bei doppeltem
     * Timestamp gewinnt der HC-Live-Wert. Ergebnis ist sortiert nach Timestamp aufsteigend.
     */
    private fun mergeHcWithCache(
        hcLive: List<Pair<Long, Double>>,
        cachedAll: List<de.frank.entropyreducer.data.local.entities.HealthConnectValueEntity>,
        metric: String,
    ): List<Pair<Long, Double>> {
        val byTs = hcLive.associate { it.first to it.second }.toMutableMap()
        cachedAll
            .filter { it.metric == metric }
            .forEach { row -> byTs.putIfAbsent(row.timestampMs, row.value) }
        return byTs.entries.map { it.key to it.value }.sortedBy { it.first }
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
                    settings.lastHealthConnectSyncMsFlow,
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
                    lastHealthConnectSyncMs = syncTimes.healthConnect,
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
        // Frank-Wunsch 2026-05-10: Refresh-Button aktualisiert ALLE Datenquellen
        // (Whoop, Amazfit, Oura, Health Connect) parallel.
        // Frank-Wunsch 2026-05-17: Permanenter Status-Footer ganz unten —
        // syncFooterRunning + syncFooterText + Timestamp bleiben dauerhaft im
        // State erhalten (bis zum naechsten Refresh).
        viewModelScope.launch {
            _refreshing.value = true
            _message.value = "⟳ Wird synchronisiert: Whoop · Training · Oura · Health Connect …"

            val whoopJob = async { repo.syncLastDays(365) }
            val ouraJob = async {
                runCatching { ouraRepo.syncLastDays(365) }.getOrElse { Result.failure(it) }
            }
            // Frank-Wunsch 2026-07-03: Trainings kommen jetzt aus Health Connect (Strava entfernt).
            // Die Zepp-/Polar-App schreibt die Sessions in Health Connect; mergeFromHealthConnect
            // uebernimmt sie inkl. GPS, Puls-/Tempoverlauf, Hoehenmeter, Cadence.
            val trainingJob = async {
                runCatching { amazfitRepo.mergeFromHealthConnect(days = 30) }.getOrElse { 0 }
            }
            refreshWeight()

            val whoopRes = whoopJob.await()
            val ouraRes = ouraJob.await()
            val trainingCount = trainingJob.await()
            _refreshing.value = false

            // Frank-Wunsch 2026-05-18: Footer aufgeraeumt.
            // (1) "Training" = Workouts aus Health Connect (frueher Strava).
            // (2) HC zu "Health Connect" ausgeschrieben.
            // (3) Bei jeder Quelle "0" statt "✗" wenn keine neuen Werte —
            //     egal ob der Sync fehlschlug oder einfach nichts Neues kam.
            val parts = mutableListOf<String>()
            val whoopCount = whoopRes.getOrNull() ?: 0
            parts.add("Whoop $whoopCount")
            parts.add("Training ${trainingCount.coerceAtLeast(0)}")
            val ouraCount =
                ouraRes.getOrNull()?.let { m ->
                    if (m is Map<*, *>) m.values.filterIsInstance<Int>().sum() else 0
                } ?: 0
            parts.add("Oura $ouraCount")
            val hcCount =
                if (!healthConnect.isAvailable() || !_weight.value.permissionGranted) {
                    0
                } else {
                    _weight.value.history30d.size
                }
            parts.add("Health Connect $hcCount")
            val summary = parts.joinToString(" · ")
            // Frank-Wunsch 2026-05-17: Permanenter Footer mit Datum+Uhrzeit.
            // Bleibt sichtbar bis zum naechsten Refresh.
            val now = java.time.ZonedDateTime.now()
            val ts = "%02d.%02d. %02d:%02d".format(
                now.dayOfMonth, now.monthValue, now.hour, now.minute,
            )
            val finalMessage = "✓ $ts · $summary"
            _message.value = finalMessage
            // Frank-Wunsch 2026-05-17: Footer-Text + Zeitstempel in DataStore
            // persistieren — verschwindet sonst beim naechsten App-Start.
            runCatching {
                settings.setLastRefreshFooter(finalMessage, System.currentTimeMillis())
            }
            // Frank-Wunsch 2026-06-19: nach dem Voll-Refresh aller Quellen EINEN Backup-Trigger
            // mit 5s-Debounce. Erfasst auch Whoop/Oura, die selbst kein requestSync ausloesen;
            // dirtyDuringUpload sorgt fuer einen zweiten Lauf, falls nach den 5s noch Werte kommen.
            syncCoordinator.requestSync(
                "Biomarker: Voll-Refresh aller Quellen (Whoop/Oura/HC)",
                SyncCoordinator.BIOMARKER_DEBOUNCE_MS,
            )
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /**
     * Frank-Wunsch 2026-05-23: Laedt Gewicht/Koerperwerte NUR aus dem DB-Cache (hc_value_cache)
     * in den Anzeige-State — ohne Health-Connect-Live-Read. Schnell genug fuer jedes Tab-Oeffnen.
     * Der Cache wird beim frischen App-Start (HealthConnectRepository.syncToCache) und beim
     * manuellen Aktualisieren-Knopf (refreshWeight) gefuellt.
     */
    fun loadWeightFromCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val available = runCatching { healthConnect.isAvailable() }.getOrDefault(false)
            if (!available) {
                _weight.value = WeightState(healthConnectAvailable = false)
                return@launch
            }
            val permission =
                runCatching { healthConnect.hasWeightReadPermission() }.getOrDefault(false)
            if (!permission) {
                _weight.value = WeightState(healthConnectAvailable = true, permissionGranted = false)
                return@launch
            }
            val kg = healthConnectRepository.cachedHistory(HealthConnectRepository.METRIC_WEIGHT)
            val bf = healthConnectRepository.cachedHistory(HealthConnectRepository.METRIC_BODY_FAT)
            val lean = healthConnectRepository.cachedHistory(HealthConnectRepository.METRIC_LEAN)
            val water = healthConnectRepository.cachedHistory(HealthConnectRepository.METRIC_WATER)
            val bone = healthConnectRepository.cachedHistory(HealthConnectRepository.METRIC_BONE)
            fun List<Pair<Long, Double>>.avgOrNull(): Double? =
                takeIf { it.isNotEmpty() }?.map { it.second }?.average()
            _weight.value =
                WeightState(
                    healthConnectAvailable = true,
                    permissionGranted = true,
                    latestKg = kg.maxByOrNull { it.first }?.second,
                    avg30dKg = kg.avgOrNull(),
                    history30d = kg,
                    latestBodyFatPercent = bf.maxByOrNull { it.first }?.second,
                    avg30dBodyFatPercent = bf.avgOrNull(),
                    bodyFatHistory30d = bf,
                    latestLeanBodyMassKg = lean.maxByOrNull { it.first }?.second,
                    avg30dLeanBodyMassKg = lean.avgOrNull(),
                    leanBodyMassHistory30d = lean,
                    latestBodyWaterMassKg = water.maxByOrNull { it.first }?.second,
                    avg30dBodyWaterMassKg = water.avgOrNull(),
                    bodyWaterMassHistory30d = water,
                    latestBoneMassKg = bone.maxByOrNull { it.first }?.second,
                    avg30dBoneMassKg = bone.avgOrNull(),
                    boneMassHistory30d = bone,
                    lastReadAtMs = System.currentTimeMillis(),
                    isLoading = false,
                )
        }
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
