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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Zoom-Stufen fuer den Trend-Chart (Spec §11.1.4). */
enum class TrendRange(val days: Int, val label: String) {
    THIRTY(30, "30 T"),
    NINETY(90, "90 T"),
    YEAR(365, "365 T"),
}

data class AnalysisUiState(
    val openCount: Int = 0,
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

    private val rangeFlow = MutableStateFlow(TrendRange.THIRTY)
    private val analysisFlow = MutableStateFlow<Pair<String, Long>>("" to 0L)
    private val uiFlow = MutableStateFlow(UiOnly())

    private data class UiOnly(val isLoading: Boolean = false, val error: String? = null)

    val state: StateFlow<AnalysisUiState> = combine(
        entries.getActive(),
        rangeFlow,
        statusObserver.observe(),
        analysisFlow,
        uiFlow,
    ) { active, range, breakdown, (md, mdAt), ui ->
        val (series, shifts) = computeTrend(active, range)
        val open = active.count { it.status == EntryStatus.OFFEN }
        val totalLoad = active.sumOf { it.severity }.coerceIn(0, 1000)
        val dominant = active.groupingBy { it.category }.eachCount()
            .maxByOrNull { it.value }?.key
        val sevenDayDelta = compute7DayTrend(active)

        AnalysisUiState(
            openCount = open,
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalysisUiState())

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

    private suspend fun computeTrend(
        active: List<EntropyEntryEntity>,
        range: TrendRange,
    ): Pair<Map<EntropyCategory, List<Double>>, List<ShiftCode>> {
        val today = LocalDate.now()
        val from = today.minusDays(range.days.toLong() - 1)
        val days = (0 until range.days).map { from.plusDays(it.toLong()) }

        // 1. Schichtcodes pro Tag
        val calendar = calendarDao.getRange(from.toString(), today.plusDays(1).toString()).first()
        val shiftByDate = calendar.associate { it.date to it.shiftCode }
        val shifts = days.map { shiftByDate[it.toString()] ?: ShiftCode.UNBEKANNT }

        // 2. Entropy-Last je Kategorie und Tag — Severity-Summe der an dem Tag erstellten Eintraege
        val byCategory = mutableMapOf<EntropyCategory, MutableList<Double>>()
        EntropyCategory.values().forEach { byCategory[it] = MutableList(range.days) { 0.0 } }

        active.forEach { e ->
            val date = Instant.ofEpochMilli(e.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
            val idx = days.indexOf(date)
            if (idx >= 0) {
                byCategory.getValue(e.category)[idx] += e.severity.toDouble()
            }
        }

        // 3. Nur Kategorien mit Daten zurueckgeben (rest weg, damit der Chart nicht gleich aussieht)
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
