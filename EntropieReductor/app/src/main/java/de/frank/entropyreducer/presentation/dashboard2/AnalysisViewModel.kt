package de.frank.entropyreducer.presentation.dashboard2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.dao.CalendarDayDao
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.repository.EntryRepository
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.domain.status.StatusBreakdown
import de.frank.entropyreducer.domain.status.StatusObserver
import de.frank.entropyreducer.domain.usecase.GenerateAnalysisUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Zoom-Stufen für den Trend-Chart (Spec §11.1.4). */
enum class TrendRange(val days: Int, val label: String) {
    SEVEN(7, "7 T"),
    THIRTY(30, "30 T"),
    NINETY(90, "90 T"),
    YEAR(365, "365 T"),
}

@androidx.compose.runtime.Immutable
data class AnalysisUiState(
    val openCount: Int = 0,
    /** Eintraege mit priorityScore >= 80 — werden in der Big-Stat-Card als "kritisch" gezeigt. */
    val criticalCount: Int = 0,
    val totalEntropyLoad: Int = 0,
    val dominantCategory: EntropyCategory? = null,
    val sevenDayTrendDelta: Int = 0,
    val trendRange: TrendRange = TrendRange.THIRTY,
    val trendSeries: Map<EntropyCategory, List<Double>> = emptyMap(),
    val trendShifts: List<ShiftCode> = emptyList(),
    val markdown: String = "",
    val markdownAt: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val statusBreakdown: StatusBreakdown? = null,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val entries: EntryRepository,
    private val calendarDao: CalendarDayDao,
    private val settings: AppSettings,
    private val statusObserver: StatusObserver,
    private val generateAnalysis: GenerateAnalysisUseCase,
) : ViewModel() {

    private val rangeFlow = MutableStateFlow(TrendRange.SEVEN)
    private val analysisFlow = MutableStateFlow<Pair<String, Long>>("" to 0L)
    private val uiFlow = MutableStateFlow(UiOnly())

    private data class UiOnly(val isLoading: Boolean = false, val error: String? = null)

    /**
     * Kalender-Tage als eigener reaktiver Flow — wechselt automatisch wenn der
     * Range-Zoom (30/90/365 Tage) geaendert wird. Vorher wurde calendarDao.getRange
     * via .first() in jedem combine-Tick neu subscribt + droppt → DB-Roundtrip pro
     * UI-Update. Mit flatMapLatest ist es genau eine aktive Subscription pro
     * Range-Auswahl, und Kalender-Aenderungen propagieren live in den Trend-Chart.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val calendarFlow = rangeFlow.flatMapLatest { range ->
        val today = LocalDate.now()
        val from = today.minusDays(range.days.toLong() - 1)
        calendarDao.getRange(from.toString(), today.plusDays(1).toString())
    }

    val state: StateFlow<AnalysisUiState> = combine(
        entries.getActive(),
        rangeFlow,
        statusObserver.observe(),
        analysisFlow,
        combine(uiFlow, calendarFlow) { ui, calendar -> ui to calendar },
    ) { active, range, breakdown, (md, mdAt), (ui, calendarDays) ->
        val (series, shifts) = computeTrendFromSnapshot(active, range, calendarDays)
        val open = active.count { it.status == EntryStatus.OFFEN }
        val critical = active.count { it.status == EntryStatus.OFFEN && it.priorityScore >= 80.0 }
        val totalLoad = active.sumOf { it.severity }.coerceIn(0, 1000)
        val dominant = active.groupingBy { it.category }.eachCount()
            .maxByOrNull { it.value }?.key
        val sevenDayDelta = compute7DayTrend(active)

        AnalysisUiState(
            openCount = open,
            criticalCount = critical,
            totalEntropyLoad = totalLoad,
            dominantCategory = dominant,
            sevenDayTrendDelta = sevenDayDelta,
            trendRange = range,
            trendSeries = series,
            trendShifts = shifts,
            markdown = md,
            markdownAt = mdAt,
            isLoading = ui.isLoading,
            error = ui.error,
            statusBreakdown = breakdown,
        )
    }
        // Performance-Fix Loop 1.1: combine-Block macht reine Berechnung
        // (computeTrendFromSnapshot, count, sumOf, groupingBy). Auf Default-
        // Dispatcher verlagern, damit Main-Thread bei jedem Eintrag-/Status-Update
        // nicht ueber die komplette aktive Liste iteriert.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), AnalysisUiState())

    init {
        // Cache aus den Settings holen, falls vorhanden
        viewModelScope.launch {
            val md = settings.cachedAnalysisMarkdownFlow.first()
            val at = settings.cachedAnalysisAtMsFlow.first()
            if (md.isNotBlank()) analysisFlow.value = md to at
        }
    }

    fun setRange(range: TrendRange) {
        rangeFlow.value = range
    }

    fun runAnalysis() {
        if (uiFlow.value.isLoading) return
        viewModelScope.launch {
            uiFlow.value = UiOnly(isLoading = true)
            generateAnalysis()
                .onSuccess { md ->
                    val now = System.currentTimeMillis()
                    settings.setCachedAnalysis(md, now)
                    analysisFlow.value = md to now
                    uiFlow.value = UiOnly(isLoading = false)
                }
                .onFailure { ex ->
                    uiFlow.value = UiOnly(
                        isLoading = false,
                        error = ex.message ?: "Analyse fehlgeschlagen",
                    )
                }
        }
    }

    fun dismissError() {
        uiFlow.value = uiFlow.value.copy(error = null)
    }

    /**
     * Reine Synchron-Berechnung aus bereits aufgeloesten Snapshots — keine Flow-
     * Subscriptions, kein DB-Roundtrip. Aufgerufen aus combine() mit den frischen
     * Werten der Source-Flows.
     */
    private fun computeTrendFromSnapshot(
        active: List<EntropyEntryEntity>,
        range: TrendRange,
        calendar: List<de.frank.entropyreducer.data.local.entities.CalendarDayEntity>,
    ): Pair<Map<EntropyCategory, List<Double>>, List<ShiftCode>> {
        val today = LocalDate.now()
        val from = today.minusDays(range.days.toLong() - 1)
        val days = ArrayList<LocalDate>(range.days)
        // Performance-Fix Loop 1.1: HashMap<LocalDate, Int> statt days.indexOf(date)
        // pro Eintrag. Vorher O(N×M) — bei 1000 Eintraegen × 365 Tagen waren das
        // 365.000 LocalDate.equals-Vergleiche pro emit. Jetzt O(N+M) mit Hashtable.
        val dayIndex = HashMap<LocalDate, Int>(range.days * 2)
        for (i in 0 until range.days) {
            val d = from.plusDays(i.toLong())
            days += d
            dayIndex[d] = i
        }

        val shiftByDate = calendar.associate { it.date to it.shiftCode }
        val shifts = days.map { shiftByDate[it.toString()] ?: ShiftCode.UNBEKANNT }

        val byCategory = mutableMapOf<EntropyCategory, MutableList<Double>>()
        EntropyCategory.values().forEach { byCategory[it] = MutableList(range.days) { 0.0 } }

        val zone = ZoneId.systemDefault()
        active.forEach { e ->
            val date = Instant.ofEpochMilli(e.createdAt).atZone(zone).toLocalDate()
            val idx = dayIndex[date] ?: return@forEach
            byCategory.getValue(e.category)[idx] += e.severity.toDouble()
        }

        val nonEmpty = byCategory
            .filterValues { values -> values.any { it > 0.0 } }
            .mapValues { it.value.toList() }
        return nonEmpty to shifts
    }

    private fun compute7DayTrend(active: List<EntropyEntryEntity>): Int {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)
        val fourteenDaysAgo = now - TimeUnit.DAYS.toMillis(14)

        val recent = active.filter { it.createdAt > sevenDaysAgo }.sumOf { it.severity }
        val previous = active.filter { it.createdAt in fourteenDaysAgo..sevenDaysAgo }.sumOf { it.severity }
        if (previous == 0) return 0
        val delta = (recent - previous).toDouble() / previous * 100.0
        return delta.toInt().coerceIn(-100, 100)
    }
}
