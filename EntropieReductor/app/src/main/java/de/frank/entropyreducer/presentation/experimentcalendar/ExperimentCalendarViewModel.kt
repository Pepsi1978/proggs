package de.frank.entropyreducer.presentation.experimentcalendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.local.dao.BiomarkerSnapshotDao
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.repository.HypothesisRepository
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.usecase.MatchInsightUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

enum class CalendarView { TAG, WOCHE, MONAT }

data class ExperimentCalendarUiState(
    val view: CalendarView = CalendarView.MONAT,
    val anchorDate: LocalDate = LocalDate.now(),
    val hypothesesByDate: Map<LocalDate, List<HypothesisEntity>> = emptyMap(),
    val selectedHypothesis: HypothesisEntity? = null,
    val biomarkerBefore: BiomarkerSnapshotEntity? = null,
    val biomarkerAfter: BiomarkerSnapshotEntity? = null,
    val showInsightPrompt: Boolean = false,
)

@HiltViewModel
class ExperimentCalendarViewModel @Inject constructor(
    private val hypotheses: HypothesisRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val matchInsight: MatchInsightUseCase,
) : ViewModel() {

    private val viewFlow = MutableStateFlow(CalendarView.MONAT)
    private val anchorFlow = MutableStateFlow(LocalDate.now())
    private val selectedFlow = MutableStateFlow<HypothesisEntity?>(null)
    private val biomarkerPair = MutableStateFlow<Pair<BiomarkerSnapshotEntity?, BiomarkerSnapshotEntity?>>(null to null)
    private val insightPromptFlow = MutableStateFlow(false)

    val state: StateFlow<ExperimentCalendarUiState> = combine(
        viewFlow,
        anchorFlow,
        hypotheses.observeAll(),
        combine(selectedFlow, biomarkerPair, insightPromptFlow) { sel, pair, prompt ->
            Triple(sel, pair, prompt)
        },
    ) { view, anchor, all, (selected, pair, prompt) ->
        val byDate = expandByDate(all)
        ExperimentCalendarUiState(
            view = view,
            anchorDate = anchor,
            hypothesesByDate = byDate,
            selectedHypothesis = selected,
            biomarkerBefore = pair.first,
            biomarkerAfter = pair.second,
            showInsightPrompt = prompt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExperimentCalendarUiState())

    fun setView(v: CalendarView) {
        viewFlow.value = v
    }

    fun shiftAnchor(deltaMonths: Int = 0, deltaWeeks: Int = 0, deltaDays: Int = 0) {
        anchorFlow.value = anchorFlow.value
            .plusMonths(deltaMonths.toLong())
            .plusWeeks(deltaWeeks.toLong())
            .plusDays(deltaDays.toLong())
    }

    fun goToToday() {
        anchorFlow.value = LocalDate.now()
    }

    fun openHypothesis(h: HypothesisEntity) {
        selectedFlow.value = h
        viewModelScope.launch {
            val before = h.biomarkerBeforeId?.let { biomarkerDao.getById(it) }
            val after = h.biomarkerAfterId?.let { biomarkerDao.getById(it) }
                ?: biomarkerDao.getLatest().first()
            biomarkerPair.value = before to after
        }
    }

    fun closeDetail() {
        selectedFlow.value = null
        biomarkerPair.value = null to null
        insightPromptFlow.value = false
    }

    fun setStatus(status: HypothesisStatus) {
        val current = selectedFlow.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = when (status) {
                HypothesisStatus.AKTIV -> current.copy(
                    status = status,
                    actualStartDate = current.actualStartDate ?: now,
                )
                HypothesisStatus.ABGEBROCHEN, HypothesisStatus.ABGESCHLOSSEN -> current.copy(
                    status = status,
                    actualEndDate = now,
                    biomarkerAfterId = current.biomarkerAfterId
                        ?: biomarkerDao.getLatest().first()?.id,
                )
                HypothesisStatus.VORGESCHLAGEN -> current.copy(status = status)
            }
            hypotheses.update(updated)
            selectedFlow.value = updated
        }
    }

    fun setOutcome(outcome: HypothesisOutcome) {
        val current = selectedFlow.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val updated = current.copy(
                outcome = outcome,
                actualEndDate = current.actualEndDate ?: now,
                status = if (current.status != HypothesisStatus.ABGESCHLOSSEN)
                    HypothesisStatus.ABGESCHLOSSEN else current.status,
                biomarkerAfterId = current.biomarkerAfterId ?: biomarkerDao.getLatest().first()?.id,
            )
            hypotheses.update(updated)
            selectedFlow.value = updated
            // Bei ERFOLGREICH dem Nutzer den Insight-Vorschlag anbieten.
            if (outcome == HypothesisOutcome.ERFOLGREICH) {
                insightPromptFlow.value = true
            }
        }
    }

    fun setOutcomeNotes(notes: String) {
        val current = selectedFlow.value ?: return
        viewModelScope.launch {
            val updated = current.copy(outcomeNotes = notes.ifBlank { null })
            hypotheses.update(updated)
            selectedFlow.value = updated
        }
    }

    fun setFeltChange(value: Int) {
        val current = selectedFlow.value ?: return
        viewModelScope.launch {
            val updated = current.copy(felltEntropyChange = value.coerceIn(-10, 10))
            hypotheses.update(updated)
            selectedFlow.value = updated
        }
    }

    fun confirmInsightCreation(create: Boolean) {
        val current = selectedFlow.value ?: return
        insightPromptFlow.value = false
        if (!create) return
        viewModelScope.launch {
            matchInsight(current, forceCreateNew = false)
        }
    }

    fun deleteHypothesis() {
        val current = selectedFlow.value ?: return
        viewModelScope.launch {
            hypotheses.delete(current)
            closeDetail()
        }
    }

    private fun expandByDate(all: List<HypothesisEntity>): Map<LocalDate, List<HypothesisEntity>> {
        val map = mutableMapOf<LocalDate, MutableList<HypothesisEntity>>()
        all.forEach { h ->
            val start = epochToLocalDate(h.plannedStartDate)
            val end = epochToLocalDate(h.plannedEndDate)
            var d = start
            while (!d.isAfter(end)) {
                map.getOrPut(d) { mutableListOf() }.add(h)
                d = d.plusDays(1)
            }
        }
        return map
    }

    private fun epochToLocalDate(ms: Long) =
        java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    /** Liefert die Tage des aktuellen Anchor-Monats (Mo..So-Raster, fixe 6 Wochen). */
    fun monthGrid(): List<LocalDate> {
        val ym = YearMonth.from(anchorFlow.value)
        val firstOfMonth = ym.atDay(1)
        // Weekday-Index 0=Mo .. 6=So
        val firstWeekdayIdx = (firstOfMonth.dayOfWeek.value + 6) % 7
        val gridStart = firstOfMonth.minusDays(firstWeekdayIdx.toLong())
        return (0 until 42).map { gridStart.plusDays(it.toLong()) }
    }
}
