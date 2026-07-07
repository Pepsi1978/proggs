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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
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
    /** Google-Calendar-Events pro Tag (Stufe 4 Erweiterung) — wird im Kalender als
     *  zusaetzliche Tag-Marker gerendert und im Detail-Sheet als Liste gezeigt. */
    val eventsByDate: Map<LocalDate, List<de.frank.entropyreducer.data.local.entities.CalendarEventEntity>> = emptyMap(),
    /** Schichtcode pro Tag — für Tag-Hintergrund/Marker. */
    val shiftByDate: Map<LocalDate, de.frank.entropyreducer.domain.model.ShiftCode> = emptyMap(),
    /** Roh-Text aus dem Kalendereintrag pro Tag — wird im Tag-Cell angezeigt
     *  damit "X", "F", "U", "Tag 1" etc. lesbar sind statt nur Schicht-Hintergrund. */
    val shiftRawByDate: Map<LocalDate, String> = emptyMap(),
    val selectedHypothesis: HypothesisEntity? = null,
    val biomarkerBefore: BiomarkerSnapshotEntity? = null,
    val biomarkerAfter: BiomarkerSnapshotEntity? = null,
    val showInsightPrompt: Boolean = false,
    /** Aktuell im Tag-Detail-Sheet ausgewaehltes Datum. null = kein Sheet sichtbar. */
    val selectedDate: LocalDate? = null,
)

/** Hilfsklasse damit selectedFlow + biomarkerPair + insightPromptFlow + selectedDateFlow
 *  als ein einziger Eintrag im combine-Builder rein passen (combine erlaubt max 5 Quellen). */
private data class CalendarSelectionState(
    val selectedHypothesis: HypothesisEntity?,
    val biomarkerPair: Pair<BiomarkerSnapshotEntity?, BiomarkerSnapshotEntity?>,
    val insightPrompt: Boolean,
    val selectedDate: LocalDate?,
)

@HiltViewModel
class ExperimentCalendarViewModel @Inject constructor(
    private val hypotheses: HypothesisRepository,
    private val biomarkerDao: BiomarkerSnapshotDao,
    private val matchInsight: MatchInsightUseCase,
    private val calendarRepo: de.frank.entropyreducer.data.repository.CalendarRepository,
) : ViewModel() {

    private val viewFlow = MutableStateFlow(CalendarView.MONAT)
    private val anchorFlow = MutableStateFlow(LocalDate.now())
    private val selectedFlow = MutableStateFlow<HypothesisEntity?>(null)
    private val biomarkerPair = MutableStateFlow<Pair<BiomarkerSnapshotEntity?, BiomarkerSnapshotEntity?>>(null to null)
    private val insightPromptFlow = MutableStateFlow(false)
    private val selectedDateFlow = MutableStateFlow<LocalDate?>(null)

    /**
     * Beobachtungs-Fenster für den Kalender — 60 Tage zurück, 5 Jahre nach vorne.
     * Frank-Wunsch 2026-05-08: alle bekannten Dienstplaene + Termine sichtbar
     * sobald sie gesynct sind, nicht nur 60 Tage. Sync-Worker zieht passend.
     */
    private val calendarRangeFrom = LocalDate.now().minusDays(60).toString()
    private val calendarRangeTo = LocalDate.now().plusDays(1825).toString()

    val state: StateFlow<ExperimentCalendarUiState> = combine(
        viewFlow,
        anchorFlow,
        hypotheses.observeAll(),
        combine(selectedFlow, biomarkerPair, insightPromptFlow, selectedDateFlow) { sel, pair, prompt, selDate ->
            CalendarSelectionState(sel, pair, prompt, selDate)
        },
        combine(
            calendarRepo.observeEventsRange(calendarRangeFrom, calendarRangeTo),
            calendarRepo.observeRange(calendarRangeFrom, calendarRangeTo),
        ) { events, days -> events to days },
    ) { view, anchor, all, selection, calendar ->
        val byDate = expandByDate(all)
        val (events, days) = calendar
        val eventsByDate = events.groupBy { runCatching { LocalDate.parse(it.date) }.getOrNull() }
            .mapNotNull { (k, v) -> k?.let { it to v } }
            .toMap()
        val shiftByDate = days.associate { entry ->
            LocalDate.parse(entry.date) to entry.shiftCode
        }
        val shiftRawByDate = days.associate { entry ->
            LocalDate.parse(entry.date) to entry.rawCalendarText
        }
        ExperimentCalendarUiState(
            view = view,
            anchorDate = anchor,
            hypothesesByDate = byDate,
            eventsByDate = eventsByDate,
            shiftByDate = shiftByDate,
            shiftRawByDate = shiftRawByDate,
            selectedHypothesis = selection.selectedHypothesis,
            biomarkerBefore = selection.biomarkerPair.first,
            biomarkerAfter = selection.biomarkerPair.second,
            showInsightPrompt = selection.insightPrompt,
            selectedDate = selection.selectedDate,
        )
    }
        // Performance-Fix Loop 3.2: Der combine-Block macht
        //   - expandByDate(all): pro Hypothese Map-Builds ueber den ganzen
        //     plannedStartDate..plannedEndDate-Span (potenziell wochenlang)
        //   - events.groupBy { LocalDate.parse(it.date) }: 1 LocalDate.parse
        //     pro Event
        //   - days.associate { LocalDate.parse(entry.date) to entry.shiftCode }
        //     ZWEI Mal: bei einem 1825-Tage-Range (5 Jahre) = bis zu 1825
        //     LocalDate.parse-Calls PRO combine-Tick, JE associate-Call
        // Ohne flowOn lief das auf Main bei jedem hypotheses-/events-/days-/
        // anchor-/view-Update — sichtbar als Stutter beim Monatsblaettern und
        // beim Ein-/Ausklappen einer Hypothese. Mit flowOn(Default) auf
        // Default-Pool, ExperimentCalendarUiState wird thread-safe an Main
        // propagiert.
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExperimentCalendarUiState())

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

    /** Oeffnet das Tag-Detail-Sheet für einen Datum-Klick im Kalender. */
    fun selectDay(date: LocalDate) {
        selectedDateFlow.value = date
    }

    fun closeDayDetail() {
        selectedDateFlow.value = null
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
            // Bei Aktivierung MUSS biomarkerBeforeId gesetzt werden (Spec §14.3 —
            // "Biomarker-Vergleich anzeigen" zeigt vor- vs. nach-Snapshot). Ohne
            // diesen Snapshot kann ConfidenceCalculator keinen Biomarker-Bonus
            // berechnen. Bei direktem Sprung von VORGESCHLAGEN nach ABGESCHLOSSEN
            // wird before als Fallback ebenfalls aus dem aktuellen Snapshot gezogen.
            val latestBio = biomarkerDao.getLatest().first()?.id
            val updated = when (status) {
                HypothesisStatus.AKTIV -> current.copy(
                    status = status,
                    actualStartDate = current.actualStartDate ?: now,
                    biomarkerBeforeId = current.biomarkerBeforeId ?: latestBio,
                )
                HypothesisStatus.ABGEBROCHEN, HypothesisStatus.ABGESCHLOSSEN -> current.copy(
                    status = status,
                    actualEndDate = now,
                    biomarkerBeforeId = current.biomarkerBeforeId ?: latestBio,
                    biomarkerAfterId = current.biomarkerAfterId ?: latestBio,
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
            val latestBio = biomarkerDao.getLatest().first()?.id
            // Outcome-Setzen impliziert ABGESCHLOSSEN — sicherstellen dass before+after
            // gesetzt sind, damit ConfidenceCalculator (Spec §16.6) den Biomarker-Bonus
            // (HRV, Recovery) korrekt berechnen kann.
            val updated = current.copy(
                outcome = outcome,
                actualEndDate = current.actualEndDate ?: now,
                status = if (current.status != HypothesisStatus.ABGESCHLOSSEN)
                    HypothesisStatus.ABGESCHLOSSEN else current.status,
                biomarkerBeforeId = current.biomarkerBeforeId ?: latestBio,
                biomarkerAfterId = current.biomarkerAfterId ?: latestBio,
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
            // Soll-Bild 19/29: Slider-Range -100..+100. Frank bewertet starke
            // Veraenderungen praezise — wir clampen daher auf -100..+100, nicht -10..+10.
            val updated = current.copy(felltEntropyChange = value.coerceIn(-100, 100))
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
