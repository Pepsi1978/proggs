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
import javax.inject.Inject

data class BiomarkerUiState(
    val latest: BiomarkerSnapshotEntity? = null,
    val history30Days: List<BiomarkerSnapshotEntity> = emptyList(),
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

    private val now = System.currentTimeMillis()
    private val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000

    val state: StateFlow<BiomarkerUiState> = combine(
        repo.observeLatest(),
        repo.observeRange(thirtyDaysAgo, now),
        _refreshing,
        _message,
        statusObserver.observe(),
    ) { latest, history, refreshing, msg, breakdown ->
        BiomarkerUiState(latest, history, refreshing, msg, breakdown)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BiomarkerUiState())

    fun refreshNow() {
        scheduler.runWhoopSyncNow()
        _message.value = "Whoop-Sync gestartet … Dauer typischerweise unter 30 Sekunden."
    }

    fun clearMessage() { _message.value = null }
}
