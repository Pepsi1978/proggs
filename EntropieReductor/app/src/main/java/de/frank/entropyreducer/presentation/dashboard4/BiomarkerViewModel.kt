package de.frank.entropyreducer.presentation.dashboard4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.WhoopWorkoutEntity
import de.frank.entropyreducer.data.repository.WhoopRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.domain.status.StatusObserver
import de.frank.entropyreducer.workers.BackgroundScheduler
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
    /** Alle Whoop-Workouts, juengste zuerst (Frank-Wunsch 2026-05-09: kompletter
     *  Workout-Bereich mit Sportart, Strain, HR-Zonen). UI gruppiert nach Tag. */
    val workouts: List<WhoopWorkoutEntity> = emptyList(),
    /** Workouts des aktuell ausgewaehlten Tages — sortiert nach Startzeit aufsteigend. */
    val workoutsForSelectedDay: List<WhoopWorkoutEntity> = emptyList(),
    /** Eigenberechnung — Erholsamer Schlaf in % = (REM + Tiefschlaf) / Gesamtschlaf.
     *  Whoop's eigenes Restorative-Sleep-Feature ist nicht ueber die API abrufbar,
     *  aber die Formel ergibt sehr nahe Werte (Frank-Recherche 2026-05-09). */
    val restorativeSleepPercent: Double? = null,
    /** Eigenberechnung — Hauttemperatur-Abweichung gegenueber dem 30-Tage-Schnitt
     *  vor dem ausgewaehlten Tag. Whoop liefert nur den Absolutwert in °C, das
     *  Delta wird hier mit der eigenen Baseline berechnet. */
    val skinTempBaseline: Double? = null,
    val skinTempDelta: Double? = null,
)

@HiltViewModel
class BiomarkerViewModel @Inject constructor(
    private val repo: WhoopRepository,
    private val scheduler: BackgroundScheduler,
    statusObserver: StatusObserver,
    settings: AppSettings,
) : ViewModel() {

    private val _refreshing = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _selectedDate = MutableStateFlow(java.time.LocalDate.now())

    private val now = System.currentTimeMillis()
    private val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

    val state: StateFlow<BiomarkerUiState> = combine(
        repo.observeLatest(),
        repo.observeAll(),
        repo.observeWorkouts(),
        repo.observeRange(thirtyDaysAgo, now),
        combine(
            _refreshing,
            _message,
            statusObserver.observe(),
            settings.lastWhoopSyncMsFlow,
            _selectedDate,
        ) { r, m, b, sync, sel ->
            StatusBundle(r, m, b, sync, sel)
        },
    ) { latest, all, workouts, last30, status ->
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

        // Eigenberechnung — Erholsamer Schlaf %: (REM + Tiefschlaf) / Gesamtschlaf.
        // Wachzeit zaehlt NICHT zum Gesamtschlaf — nur die echten Schlafphasen.
        val rem = selSnap?.sleepRemMinutes ?: 0
        val deep = selSnap?.sleepDeepMinutes ?: 0
        val light = selSnap?.sleepLightMinutes ?: 0
        val totalSleep = rem + deep + light
        val restorativePct = if (totalSleep > 0) {
            (rem + deep).toDouble() / totalSleep.toDouble() * 100.0
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
            workouts = workouts,
            workoutsForSelectedDay = workoutsForDay,
            restorativeSleepPercent = restorativePct,
            skinTempBaseline = baseline,
            skinTempDelta = skinTempDelta,
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
        // Direkter Sync (Frank-Wunsch 2026-05-09: "Refresh tut nichts").
        // Vorher: nur scheduler.runWhoopSyncNow() — Worker laeuft asynchron im
        // Hintergrund, ohne sichtbares Feedback. Jetzt: direkt im VM-Coroutine,
        // mit Fortschrittsmeldung und konkreten Fehlermeldungen wenn was schief
        // geht (Token abgelaufen / Netzwerk / API-Limit).
        viewModelScope.launch {
            _refreshing.value = true
            _message.value = "Whoop-Sync läuft …"
            val result = repo.syncLastDays(365)
            _refreshing.value = false
            result.onSuccess { count ->
                _message.value = if (count == 0) {
                    "Sync OK, aber 0 Snapshots — pruefe ob deine Whoop-API-Berechtigung noch gueltig ist."
                } else {
                    "$count Whoop-Snapshots geladen ($count letzte Tage)."
                }
            }.onFailure { ex ->
                // Direktive 3 — Diagnose: Token-Probleme als eigene Fehlerklasse
                // unterscheiden, damit Frank im Banner sofort sieht was zu tun ist.
                // "Whoop-Sync fehlgeschlagen: …" war zu generisch — bei abgelaufenem
                // Token wusste man nicht ob es Internet, Whoop-API oder Login ist.
                val msg = ex.message.orEmpty()
                _message.value = when {
                    ex is IllegalStateException && msg.contains("Access-Token") ->
                        "Whoop-Anmeldung abgelaufen. Bitte unter Einstellungen → API-Schluessel neu anmelden."
                    ex is IllegalStateException && msg.contains("Client-Secret") ->
                        "Whoop-Client-Secret fehlt — bitte in den API-Schluessel-Settings eintragen."
                    else ->
                        "Whoop-Sync fehlgeschlagen: ${ex.message ?: ex.javaClass.simpleName}"
                }
            }
        }
    }

    fun clearMessage() { _message.value = null }
}
