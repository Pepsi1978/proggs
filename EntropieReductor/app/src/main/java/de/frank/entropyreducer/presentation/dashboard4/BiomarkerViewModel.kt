package de.frank.entropyreducer.presentation.dashboard4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.AmazfitDailyEntity
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.OuraActivityEntity
import de.frank.entropyreducer.data.local.entities.OuraDailySleepEntity
import de.frank.entropyreducer.data.local.entities.OuraReadinessEntity
import de.frank.entropyreducer.data.local.entities.OuraResilienceEntity
import de.frank.entropyreducer.data.local.entities.OuraSleepDetailEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity
import de.frank.entropyreducer.data.health.HealthConnectManager
import de.frank.entropyreducer.data.remote.drive.SyncCoordinator
import de.frank.entropyreducer.data.repository.AmazfitRepository
import de.frank.entropyreducer.data.repository.BiomarkerCardOrderRepository
import de.frank.entropyreducer.data.repository.OuraRepository
import de.frank.entropyreducer.data.repository.WhoopRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.domain.status.StatusObserver
import de.frank.entropyreducer.workers.BackgroundScheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class StatusBundle(
    val refreshing: Boolean,
    val message: String?,
    val breakdown: StatusBreakdown?,
    val lastWhoopSyncMs: Long,
    val selectedDate: java.time.LocalDate,
)

/** Frank-Wunsch 2026-05-10: einheitlicher Sync-Zeitstempel-Pool fuer den
 *  'Zuletzt synchronisiert'-Header. Pro Biomarker-Quelle eigener Wert. */
private data class SyncTimes(
    val oura: Long,
    val amazfit: Long,
    val healthConnect: Long,
)

/** Whoop-Daten gebuendelt damit der outer combine() unter 5 Flows bleibt.
 *  last30 wird NICHT als Flow eingebunden, sondern in der Transform-Lambda
 *  aus history mit dem aktuellen System.currentTimeMillis() abgeleitet —
 *  sonst friert das 30-Tage-Fenster auf den Konstrukturzeitpunkt ein und
 *  wird stale wenn die App ueber Mitternacht offen bleibt. */
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

/** Oura-Ring-Daten gebuendelt (Frank-Wunsch 2026-05-10). Fuenf Listen,
 *  pro Endpunkt eine. Latest und Selected-Day werden im inner-combine
 *  daraus abgeleitet. */
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
    /** VOLLSTAENDIGE Historie aller Whoop-Snapshots — nicht mehr auf 30 Tage limitiert
     *  (Frank-Wunsch 2026-05-08). Trends, Detail-Screen und Korrelationen nutzen das.
     *  history30Days bleibt als 30-Tage-Slice für die Mini-Card-Deltas. */
    val history: List<BiomarkerSnapshotEntity> = emptyList(),
    val history30Days: List<BiomarkerSnapshotEntity> = emptyList(),
    /** Aktuell ausgewaehlter Tag (Frank-Wunsch 2026-05-08: zwischen Heute / gestern /
     *  vorgestern wechseln). Default = Heute. Der Snapshot für diesen Tag wird in
     *  selectedSnapshot gehalten und in den Mini-Cards + Recovery-Ring angezeigt. */
    val selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
    val selectedSnapshot: BiomarkerSnapshotEntity? = null,
    val isRefreshing: Boolean = false,
    val message: String? = null,
    val statusBreakdown: StatusBreakdown? = null,
    /** Zeitstempel der letzten erfolgreichen Whoop-Synchronisation in ms.
     *  0 = noch nie erfolgreich gesynced. Frank-Wunsch 2026-05-09:
     *  „Zuletzt synchronisiert"-Zeile als kleine Info unter dem Header. */
    val lastWhoopSyncMs: Long = 0L,
    /** Frank-Wunsch 2026-05-10: pro Biomarker-Quelle eigener Zeitstempel,
     *  damit der Header das Minimum aller vier (= "alles aktuell"-Zeitpunkt) zeigt. */
    val lastOuraSyncMs: Long = 0L,
    val lastAmazfitSyncMs: Long = 0L,
    val lastHealthConnectSyncMs: Long = 0L,
    /** Alle Whoop-Workouts, juengste zuerst (Frank-Wunsch 2026-05-09: kompletter
     *  Workout-Bereich mit Sportart, Strain, HR-Zonen). UI gruppiert nach Tag. */
    val workouts: List<WhoopWorkoutEntity> = emptyList(),
    /** Workouts des aktuell ausgewaehlten Tages — sortiert nach Startzeit aufsteigend. */
    val workoutsForSelectedDay: List<WhoopWorkoutEntity> = emptyList(),
    /** Eigenberechnung — Erholsamer Schlaf in % = (REM + Tiefschlaf) / Zeit im Bett.
     *  Zeit im Bett = REM + Tiefschlaf + Leichtschlaf + Wach (alle Phasen zusammen).
     *  So berechnet Whoop den Wert — Frank-Vorgabe 2026-05-10 (91,9% Whoop-Referenz). */
    val restorativeSleepPercent: Double? = null,
    /** Eigenberechnung — Hauttemperatur-Abweichung gegenueber dem 30-Tage-Schnitt
     *  vor dem ausgewaehlten Tag. Whoop liefert nur den Absolutwert in °C, das
     *  Delta wird hier mit der eigenen Baseline berechnet. */
    val skinTempBaseline: Double? = null,
    val skinTempDelta: Double? = null,
    /** Frank-Wunsch 2026-05-09: PAI/BioCharge/Hauttemperatur von der Amazfit T-Rex 3
     *  als zusaetzliche Werte mit eigenem Quellen-Label in den Biomarker-Bereich. */
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
    /** Oura-Historie sortiert nach Tag aufsteigend (aelteste zuerst). Wird fuer die
     *  Mini-Verlaufsbalken und die Plus/Minus-Anzeige zum 30-Tage-Mittel auf den
     *  Karten gebraucht. Frank-Wunsch 2026-05-10 (Etappe D). */
    val ouraReadinessHistory: List<OuraReadinessEntity> = emptyList(),
    val ouraSleepHistory: List<OuraDailySleepEntity> = emptyList(),
    val ouraActivityHistory: List<OuraActivityEntity> = emptyList(),
    val ouraResilienceHistory: List<OuraResilienceEntity> = emptyList(),
)

/**
 * Weight-State fuer die Health-Connect-Mini-Karte (Frank-Wunsch 2026-05-10).
 * Wird separat vom Hauptstate gehalten, weil Health Connect kein Flow ist sondern
 * suspend-basiert — der Wert wird beim Init und beim Refresh einmalig gelesen.
 *
 * Mit  - permissionGranted=false: Karte zeigt "Tippen um zu erlauben"
 * Mit  - permissionGranted=true + latestKg=null: Karte zeigt "Keine Daten in HC"
 * Mit  - permissionGranted=true + latestKg!=null: Karte zeigt Wert + Delta
 */
@androidx.compose.runtime.Immutable
data class WeightState(
    val healthConnectAvailable: Boolean = false,
    /**
     * True wenn ALLE drei Permissions (Weight, BodyFat, LeanBodyMass) erteilt
     * sind. Wird vom Permission-Launcher in einem Rutsch angefordert — wenn
     * Frank nur Weight erlaubt aber nicht BodyFat, ist das hier false. Damit
     * stoppt Frank's Tap-to-Refresh-Loop und zeigt stattdessen einen Hinweis.
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
class BiomarkerViewModel @Inject constructor(
    private val repo: WhoopRepository,
    private val amazfitRepo: AmazfitRepository,
    private val ouraRepo: OuraRepository,
    private val scheduler: BackgroundScheduler,
    private val cardOrderRepo: BiomarkerCardOrderRepository,
    private val syncCoordinator: SyncCoordinator,
    private val healthConnect: HealthConnectManager,
    statusObserver: StatusObserver,
    private val settings: AppSettings,
) : ViewModel() {

    private val _refreshing = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _selectedDate = MutableStateFlow(java.time.LocalDate.now())

    // Health-Connect-Gewicht: separater State, wird beim Init und nach jedem
    // erfolgreichen Permission-Grant aktualisiert.
    private val _weight = MutableStateFlow(WeightState())
    val weight: StateFlow<WeightState> = _weight

    init {
        viewModelScope.launch { refreshWeight() }
    }

    /**
     * Liest Gewicht, Koerperfett und Magermasse aus Health Connect. Setzt
     * waehrend des Lesens [WeightState.isLoading] = true damit die UI einen
     * Spinner anzeigen kann.
     *
     * Frank-Wunsch 2026-05-10: Tap auf eine Mini-Karte ruft genau diese
     * Methode auf — sofortiges Refresh ohne in die Settings zu gehen. Wenn
     * Permission fehlt, zeigt der Karten-Tap stattdessen einen Permission-
     * Dialog (siehe Screen-Logik). Permission gilt fuer ALLE drei Records
     * gleichzeitig — wenn Frank nur eine Permission erteilt, wird permissionGranted
     * weiterhin false sein und die Karten zeigen "Tippen".
     */
    fun refreshWeight() {
        viewModelScope.launch {
            android.util.Log.i("BiomarkerVM", "refreshWeight() called")
            val available = healthConnect.isAvailable()
            android.util.Log.i("BiomarkerVM", "  HC available=$available")
            if (!available) {
                _weight.value = WeightState(healthConnectAvailable = false)
                return@launch
            }
            val weightOk = healthConnect.hasWeightReadPermission()
            val bodyFatOk = healthConnect.hasBodyFatReadPermission()
            val leanOk = healthConnect.hasLeanBodyMassReadPermission()
            val waterOk = healthConnect.hasBodyWaterMassReadPermission()
            val boneOk = healthConnect.hasBoneMassReadPermission()
            val historyOk = healthConnect.hasHistoryReadPermission()
            android.util.Log.i("BiomarkerVM", "  permissions: weight=$weightOk bodyFat=$bodyFatOk lean=$leanOk water=$waterOk bone=$boneOk history=$historyOk")
            // permissionGranted = ALLE noetigen erteilt (5 Datentypen + History).
            // Ohne History-Permission lesen wir nur 30 Tage zurueck — bei seltenen
            // Wiegungen ist das oft leer (Frank-Befund 2026-05-10).
            if (!(weightOk && bodyFatOk && leanOk && waterOk && boneOk && historyOk)) {
                _weight.value = WeightState(
                    healthConnectAvailable = true,
                    permissionGranted = false,
                )
                return@launch
            }
            android.util.Log.i("BiomarkerVM", "  alle permissions ok — lese Werte ...")
            // Loading-State setzen damit die Karten einen Spinner zeigen koennen
            _weight.value = _weight.value.copy(isLoading = true)
            val latestKg = healthConnect.readLatestWeightKg()
            val avgKg = healthConnect.averageWeightKg(730)
            val historyKg = healthConnect.readWeightHistory(730)
            val latestBf = healthConnect.readLatestBodyFatPercent()
            val avgBf = healthConnect.averageBodyFatPercent(730)
            val historyBf = healthConnect.readBodyFatHistory(730)
            val latestLean = healthConnect.readLatestLeanBodyMassKg()
            val avgLean = healthConnect.averageLeanBodyMassKg(730)
            val historyLean = healthConnect.readLeanBodyMassHistory(730)
            val latestWater = healthConnect.readLatestBodyWaterMassKg()
            val avgWater = healthConnect.averageBodyWaterMassKg(730)
            val historyWater = healthConnect.readBodyWaterMassHistory(730)
            val latestBone = healthConnect.readLatestBoneMassKg()
            val avgBone = healthConnect.averageBoneMassKg(730)
            val historyBone = healthConnect.readBoneMassHistory(730)
            _weight.value = WeightState(
                healthConnectAvailable = true,
                permissionGranted = true,
                latestKg = latestKg,
                avg30dKg = avgKg,
                history30d = historyKg,
                latestBodyFatPercent = latestBf,
                avg30dBodyFatPercent = avgBf,
                bodyFatHistory30d = historyBf,
                latestLeanBodyMassKg = latestLean,
                avg30dLeanBodyMassKg = avgLean,
                leanBodyMassHistory30d = historyLean,
                latestBodyWaterMassKg = latestWater,
                avg30dBodyWaterMassKg = avgWater,
                bodyWaterMassHistory30d = historyWater,
                latestBoneMassKg = latestBone,
                avg30dBoneMassKg = avgBone,
                boneMassHistory30d = historyBone,
                lastReadAtMs = System.currentTimeMillis(),
                isLoading = false,
            )
            // Frank-Wunsch 2026-05-10: einheitlicher Sync-Zeitstempel-Pool fuer
            // den 'Zuletzt synchronisiert'-Header im Biomarker-Screen.
            settings.setLastHealthConnectSync(System.currentTimeMillis())
        }
    }

    /**
     * Aktuelle Reihenfolge der Biomarker-Karten (Frank-Wunsch 2026-05-10).
     * Wird vom Screen via collectAsState() beobachtet. Neu hinzugefuegte Karten
     * (z.B. nach App-Update) werden vom Repository automatisch ans Ende angehaengt.
     */
    val cardOrder: StateFlow<List<String>> = cardOrderRepo.orderedCardIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BiomarkerCardId.DEFAULT_ORDER)

    /**
     * Speichert die neue Reihenfolge nach jedem Drag & Drop. Wird vom Screen
     * im onMove-Callback der reorderable LazyColumn aufgerufen.
     *
     * Frank-Wunsch 2026-05-10: Nach jedem Reorder soll automatisch ein
     * Drive-Backup-Sync laufen. requestSync() ist debounced (1500 ms) — wenn
     * Frank schnell mehrere Karten hintereinander verschiebt, wird das zu einem
     * einzigen Upload zusammengefasst statt 5x hochzuladen. Wenn Drive-Backup
     * deaktiviert oder kein Konto verbunden ist, ist requestSync() ein No-Op.
     */
    fun saveCardOrder(newOrder: List<String>) {
        viewModelScope.launch {
            cardOrderRepo.saveOrder(newOrder)
            syncCoordinator.requestSync()
        }
    }

    /**
     * Setzt die Reihenfolge auf die Werks-Einstellung zurueck. Wird vom
     * Settings-Menue oder einem Long-Press-Hold-Reset-Knopf ausgeloest.
     * Auch hier triggern wir einen Drive-Sync, damit der Reset auf anderen
     * Geraeten durchschlaegt.
     */
    fun resetCardOrder() {
        viewModelScope.launch {
            cardOrderRepo.resetToDefault()
            syncCoordinator.requestSync()
        }
    }

    /**
     * Liefert das Set ALLER Health-Connect-Permission-Strings, die beim
     * Permission-Dialog angefordert werden sollen. Frank-Wunsch 2026-05-10:
     * prophylaktisch ALLE Datentypen erlauben damit zukuenftige Plugins ohne
     * erneuten Permission-Flow auskommen.
     */
    fun allHealthConnectPermissions(): Set<String> = healthConnect.requiredReadPermissions()

    /**
     * Frank-Wunsch 2026-05-10: Im Nachhinein einzelne HC-Permissions zurueckziehen
     * koennen. Oeffnet die Health-Connect-spezifische Permissions-UI fuer unsere App.
     */
    fun openHealthConnectPermissionsEditor() = healthConnect.openAppPermissionsInHealthConnect()

    val state: StateFlow<BiomarkerUiState> = combine(
        combine(
            repo.observeLatest(),
            repo.observeAll(),
            repo.observeWorkouts(),
        ) { l, a, w -> WhoopBundle(l, a, w) },
        combine(
            amazfitRepo.observeLatestDaily(),
            amazfitRepo.observeAllDaily(),
            amazfitRepo.observeAllWorkouts(),
        ) { lD, aD, w -> AmazfitBundle(lD, aD, w) },
        combine(
            ouraRepo.observeReadiness(),
            ouraRepo.observeDailySleep(),
            ouraRepo.observeActivity(),
            ouraRepo.observeResilience(),
            ouraRepo.observeSleepDetails(),
        ) { r, s, a, res, sd -> OuraBundle(r, s, a, res, sd) },
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
        ) { o, a, h -> SyncTimes(o, a, h) },
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
        val selStartMs = selDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val selEndMs = selStartMs + 24L * 60 * 60 * 1000
        val selSnap = all.lastOrNull { it.capturedAt in selStartMs until selEndMs }
            ?: if (selDate == java.time.LocalDate.now()) latest else null

        // Workouts fuer den gewaehlten Tag — Whoop liefert dateKey im UTC-Tag,
        // wir vergleichen lokal (Frank arbeitet in lokaler Zeit) per Bereichsfilter.
        val workoutsForDay = workouts
            .filter { it.startMs in selStartMs until selEndMs }
            .sortedBy { it.startMs }

        // Amazfit-Tag-Eintrag fuer den gewaehlten Tag (per date-String matchen).
        val selDateKey = selDate.toString()
        val amazfitForDay = amazfit.allDaily.firstOrNull { it.date == selDateKey }
            ?: if (selDate == java.time.LocalDate.now()) amazfit.latestDaily else null

        // Amazfit-Workouts fuer den gewaehlten Tag.
        val amazfitWorkoutsForDay = amazfit.workouts
            .filter { it.startMs in selStartMs until selEndMs }
            .sortedBy { it.startMs }

        // Oura-Daten fuer den gewaehlten Tag — Tagesschluessel ist YYYY-MM-DD.
        // Bei "Heute" Fallback auf den juengsten Eintrag falls Sync noch nicht
        // durchgelaufen ist (Oura-Cloud hat Sync-Delay 40-50 Min nach Ring-Sync).
        val ouraReadinessForDay = oura.readiness.firstOrNull { it.day == selDateKey }
            ?: if (selDate == java.time.LocalDate.now()) {
                oura.readiness.maxByOrNull { it.day }
            } else {
                null
            }
        val ouraSleepForDay = oura.dailySleep.firstOrNull { it.day == selDateKey }
            ?: if (selDate == java.time.LocalDate.now()) {
                oura.dailySleep.maxByOrNull { it.day }
            } else {
                null
            }
        val ouraActivityForDay = oura.activity.firstOrNull { it.day == selDateKey }
            ?: if (selDate == java.time.LocalDate.now()) {
                oura.activity.maxByOrNull { it.day }
            } else {
                null
            }
        val ouraResilienceForDay = oura.resilience.firstOrNull { it.day == selDateKey }
            ?: if (selDate == java.time.LocalDate.now()) {
                oura.resilience.maxByOrNull { it.day }
            } else {
                null
            }
        val ouraSleepDetailsForDay = oura.sleepDetail.filter { it.day == selDateKey }
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
        val restorativePct = if (timeInBed > 0) {
            (rem + deep).toDouble() / timeInBed.toDouble() * 100.0
        } else null

        // Eigenberechnung — Hauttemperatur-Delta. Baseline = Mittel der letzten
        // 30 Tage VOR dem ausgewaehlten Tag. Mindestens 7 Werte sonst NICHT
        // anzeigen damit der Wert verlaesslich ist.
        val baselineEndMs = selStartMs
        val baselineStartMs = baselineEndMs - 30L * 24 * 60 * 60 * 1000
        val baselineValues = all
            .filter { it.capturedAt in baselineStartMs until baselineEndMs }
            .mapNotNull { it.skinTempCelsius }
        val baseline = if (baselineValues.size >= 7) baselineValues.average() else null
        val skinTempDelta = if (baseline != null && selSnap?.skinTempCelsius != null) {
            selSnap.skinTempCelsius - baseline
        } else null

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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), BiomarkerUiState())

    fun selectDate(date: java.time.LocalDate) {
        _selectedDate.value = date
    }

    fun goToToday() {
        _selectedDate.value = java.time.LocalDate.now()
    }

    fun shiftDay(delta: Int) {
        _selectedDate.value = _selectedDate.value.plusDays(delta.toLong())
    }

    fun refreshNow() {
        // Frank-Wunsch 2026-05-10: Refresh-Button aktualisiert ALLE Datenquellen
        // (Whoop, Amazfit, Oura, Health Connect) parallel. Vorher nur Whoop —
        // jetzt sammelt das ViewModel die Counts pro Quelle und zeigt eine
        // zusammengefasste Status-Meldung.
        viewModelScope.launch {
            _refreshing.value = true
            _message.value = "Sync läuft (Whoop + Amazfit + Oura + Health Connect) …"
            val whoopJob = async { repo.syncLastDays(365) }
            val amazfitJob = async {
                runCatching { amazfitRepo.syncLastDays(365) }.getOrElse { Result.failure(it) }
            }
            val ouraJob = async {
                runCatching { ouraRepo.syncLastDays(365) }.getOrElse { Result.failure(it) }
            }
            // Health Connect parallel mit-aktualisieren — refreshWeight() startet
            // seinen eigenen viewModelScope.launch, wir warten nicht ab und
            // markieren HC einfach als 'angestossen'. Die UI bekommt das Update
            // ueber den weight-StateFlow sobald die HC-Reads fertig sind.
            refreshWeight()

            val whoopRes = whoopJob.await()
            val amazfitRes = amazfitJob.await()
            val ouraRes = ouraJob.await()
            _refreshing.value = false

            val parts = mutableListOf<String>()
            whoopRes.fold(
                onSuccess = { c -> parts.add("Whoop $c") },
                onFailure = {
                    val msg = it.message.orEmpty()
                    parts.add(
                        when {
                            it is IllegalStateException && msg.contains("Access-Token") -> "Whoop ✗ (abgelaufen)"
                            it is IllegalStateException && msg.contains("Client-Secret") -> "Whoop ✗ (Secret fehlt)"
                            else -> "Whoop ✗"
                        },
                    )
                },
            )
            amazfitRes.fold(
                onSuccess = { c -> parts.add("Amazfit $c") },
                onFailure = { parts.add("Amazfit ✗") },
            )
            ouraRes.fold(
                onSuccess = { m ->
                    val total = if (m is Map<*, *>) m.values.filterIsInstance<Int>().sum() else 0
                    parts.add("Oura $total")
                },
                onFailure = { parts.add("Oura ✗") },
            )
            // Health Connect: refreshWeight() oben laeuft fire-and-forget in
            // einem eigenen viewModelScope.launch. Status hier ehrlich aus
            // dem aktuell sichtbaren Permission/Availability-Stand ableiten —
            // statt wie frueher unconditional "HC ✓" anzuhaengen, was bei
            // fehlender Permission oder unverfuegbarem HC eine Falschmeldung
            // war. Bei "↻" weiss Frank dass HC angestossen wurde, aber das
            // tatsaechliche Ergebnis kommt ueber den weight-StateFlow rein.
            val hcStatus = when {
                !healthConnect.isAvailable() -> "HC —"
                !_weight.value.permissionGranted -> "HC ✗ (Permission)"
                else -> "HC ↻"
            }
            parts.add(hcStatus)
            _message.value = "Sync fertig: " + parts.joinToString(" · ")
        }
    }

    fun clearMessage() { _message.value = null }
}
