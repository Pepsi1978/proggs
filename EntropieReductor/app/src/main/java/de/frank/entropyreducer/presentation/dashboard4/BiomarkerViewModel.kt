package de.frank.entropyreducer.presentation.dashboard4

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.repository.WhoopRepository
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
)

@HiltViewModel
class BiomarkerViewModel @Inject constructor(
    private val repo: WhoopRepository,
    private val scheduler: BackgroundScheduler,
    statusObserver: StatusObserver,
) : ViewModel() {

    private val _refreshing = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)
    private val _selectedDate = MutableStateFlow(java.time.LocalDate.now())

    private val now = System.currentTimeMillis()
    private val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

    val state: StateFlow<BiomarkerUiState> = combine(
        repo.observeLatest(),
        repo.observeAll(),
        repo.observeRange(thirtyDaysAgo, now),
        combine(_refreshing, _message, statusObserver.observe()) { r, m, b -> Triple(r, m, b) },
        _selectedDate,
    ) { latest, all, last30, status, selDate ->
        // Snapshot für den gewaehlten Tag finden — wenn kein Snapshot für das
        // exakte Datum existiert, wird der naechste juengere Snapshot vor dem
        // gewaehlten Tag genommen (Whoop syncs typischerweise einmal pro Tag).
        val selStartMs = selDate.atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val selEndMs = selStartMs + 24L * 60 * 60 * 1000
        val selSnap = all.lastOrNull { it.capturedAt in selStartMs until selEndMs }
            ?: if (selDate == java.time.LocalDate.now()) latest else null
        BiomarkerUiState(
            latest = latest,
            history = all,
            history30Days = last30,
            selectedDate = selDate,
            selectedSnapshot = selSnap,
            isRefreshing = status.first,
            message = status.second,
            statusBreakdown = status.third,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BiomarkerUiState())

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
