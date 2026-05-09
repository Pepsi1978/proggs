package de.frank.entropyreducer.presentation.amazfit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.repository.AmazfitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@androidx.compose.runtime.Immutable
data class AmazfitTrainingsUiState(
    val workouts: List<AmazfitWorkoutEntity> = emptyList(),
    val filtered: List<AmazfitWorkoutEntity> = emptyList(),
    val sportFilter: Int? = null,
    val rangeFilter: Range = Range.ALL,
    val availableSports: List<Pair<Int, String>> = emptyList(),
)

enum class Range(val days: Int) { LAST_30(30), LAST_90(90), LAST_365(365), ALL(0) }

/**
 * State + Filter-Logik fuer den Sport-Bereich. Filtert die Workouts nach
 * Sportart und Zeitraum. Frank-Wunsch 2026-05-09: "macht alles rein, alles
 * was mit Sport zu tun hat — ich sortiere im Nachhinein aus".
 */
@HiltViewModel
class AmazfitTrainingsViewModel @Inject constructor(
    private val repo: AmazfitRepository,
) : ViewModel() {

    private val _sportFilter = MutableStateFlow<Int?>(null)
    private val _rangeFilter = MutableStateFlow(Range.LAST_90)

    val state: StateFlow<AmazfitTrainingsUiState> = combine(
        repo.observeAllWorkouts(),
        _sportFilter,
        _rangeFilter,
    ) { workouts, sport, range ->
        val now = System.currentTimeMillis()
        val cutoff = if (range == Range.ALL) Long.MIN_VALUE
                     else now - range.days * 24L * 60 * 60 * 1000
        val filtered = workouts.filter { w ->
            (sport == null || w.sportType == sport) &&
                w.startMs >= cutoff
        }
        val sportsInData = workouts
            .mapNotNull { w -> w.sportType?.let { it to (w.sportName ?: "Sport $it") } }
            .toSet()
            .toList()
            .sortedBy { it.second }
        AmazfitTrainingsUiState(
            workouts = workouts,
            filtered = filtered,
            sportFilter = sport,
            rangeFilter = range,
            availableSports = sportsInData,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(60_000),
        AmazfitTrainingsUiState(),
    )

    fun setSport(type: Int?) {
        _sportFilter.value = type
    }

    fun setRange(range: Range) {
        _rangeFilter.value = range
    }
}

/**
 * State fuer den Detail-Screen eines einzelnen Workouts. Holt das Workout
 * per trackId aus SavedStateHandle und triggert ON-DEMAND den Detail-API-Call
 * (GPS-Track + Pulsverlauf + Pace pro km + Splits).
 */
@HiltViewModel
class AmazfitTrainingDetailViewModel @Inject constructor(
    private val repo: AmazfitRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val trackId: String = savedStateHandle["trackId"] ?: ""

    val workout: StateFlow<AmazfitWorkoutEntity?> = repo.observeWorkoutById(trackId)
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    init {
        // Beim ersten Laden: Detail-Daten holen falls noch nicht in DB.
        if (trackId.isNotBlank()) {
            viewModelScope.launch {
                repo.ensureWorkoutDetail(trackId)
            }
        }
    }
}
