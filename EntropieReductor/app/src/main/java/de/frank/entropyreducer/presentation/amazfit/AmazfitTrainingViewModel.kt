package de.frank.entropyreducer.presentation.amazfit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.entities.AmazfitWorkoutEntity
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.repository.AmazfitRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    // Frank-Wunsch 2026-05-17: BiomarkerSnapshots fuer den Tages-Ruhepuls-
    // Lookup in der VO2max-Spalte der Trainings-Liste.
    val snapshots: List<BiomarkerSnapshotEntity> = emptyList(),
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
    private val biomarkerDao: BiomarkerSnapshotDao,
) : ViewModel() {

    private val _sportFilter = MutableStateFlow<Int?>(null)
    private val _rangeFilter = MutableStateFlow(Range.LAST_90)

    val state: StateFlow<AmazfitTrainingsUiState> = combine(
        repo.observeAllWorkouts(),
        biomarkerDao.getAll(),
        _sportFilter,
        _rangeFilter,
    ) { workouts, snapshots, sport, range ->
        val now = System.currentTimeMillis()
        val cutoff = if (range == Range.ALL) Long.MIN_VALUE
                     else now - range.days * 24L * 60 * 60 * 1000
        val filtered = workouts.filter { w ->
            (sport == null || w.sportType == sport) &&
                w.startMs >= cutoff
        }
        // Crash-Fix 2026-08-05 (Almanach jetpack-compose §4.2): toSet() dedupliziert das PAAR,
        // nicht den sportType. Derselbe Sportart-Code kann aber mehrere Namen tragen (Polar
        // liefert den frei benennbaren Trainings-Titel, dazu die Umbenenn-Funktion im Detail-
        // Screen). Ergebnis waren zwei Chips mit demselben LazyRow-Key -> IllegalArgumentException
        // "Key ... was already used". distinctBy auf den Code haelt die Chips eindeutig; gefiltert
        // wird ohnehin nur ueber sportType, daher kein Funktionsverlust.
        val sportsInData = workouts
            .mapNotNull { w -> w.sportType?.let { it to (w.sportName ?: "Sport $it") } }
            .distinctBy { it.first }
            .sortedBy { it.second }
        AmazfitTrainingsUiState(
            workouts = workouts,
            filtered = filtered,
            sportFilter = sport,
            rangeFilter = range,
            availableSports = sportsInData,
            snapshots = snapshots,
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
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AmazfitTrainingDetailViewModel @Inject constructor(
    private val repo: AmazfitRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val trackId: String = savedStateHandle["trackId"] ?: ""

    val workout: StateFlow<AmazfitWorkoutEntity?> = repo.observeWorkoutById(trackId)
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    /**
     * Frank-Wunsch 2026-05-17: Tages-Ruhepuls aus dem Whoop-Snapshot fuer
     * den Workout-Tag. Wird in [estimateVo2Max] verwendet — ersetzt den
     * frueher hardcoded Wert von 65 bpm.
     *
     * Suchstrategie via [findRestingHrForWorkoutDay]: Snapshot vom gleichen
     * Kalendertag, Fallback Vortag, sonst null.
     */
    val restingHrForWorkoutDay: StateFlow<Int?> = workout.flatMapLatest { w ->
        if (w == null) {
            flowOf<Int?>(null)
        } else {
            val zone = ZoneId.systemDefault()
            val workoutDate = Instant.ofEpochMilli(w.startMs).atZone(zone).toLocalDate()
            val from = workoutDate.minusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val to = workoutDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            biomarkerDao.getRange(from, to).map { snapshots ->
                findRestingHrForWorkoutDay(snapshots, w.startMs)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), null)

    init {
        // Beim ersten Laden: Detail-Daten holen falls noch nicht in DB.
        if (trackId.isNotBlank()) {
            viewModelScope.launch {
                repo.ensureWorkoutDetail(trackId)
            }
        }
    }

    // reloadDetail() entfernt 2026-05-17 (Frank-Wunsch — Erneut-Laden-Button raus).

    /**
     * Frank-Wunsch 2026-05-17: Manuelle Override-Werte aus dem Edit-Dialog
     * speichern. UI-Layer ([EditTrainingValuesDialog]) liefert ein
     * [ManualWorkoutOverrides] Objekt mit allen Eingaben — Repository macht
     * den Merge (nur non-null Eingaben werden gespeichert).
     */
    fun applyManualOverrides(overrides: ManualWorkoutOverrides) {
        if (trackId.isBlank()) return
        viewModelScope.launch {
            repo.applyManualOverrides(
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

    /**
     * Frank-Wunsch 2026-05-18: Aktuelles Workout aus der Detail-Ansicht
     * loeschen. UI ruft das aus dem 3-Punkte-Menue "Aktivitaet loeschen"
     * auf und navigiert nach erfolgreichem Loeschen zurueck.
     */
    fun deleteCurrentWorkout(onDeleted: () -> Unit) {
        if (trackId.isBlank()) return
        viewModelScope.launch {
            repo.deleteWorkoutByTrackId(trackId)
            onDeleted()
        }
    }
}
