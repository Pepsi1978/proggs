package de.frank.entropyreducer.presentation.experimentcalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.domain.model.HypothesisOutcome
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Eigener App-Kalender (NICHT Google Calendar) — Spec §14.3.
 * Zeigt aktive + vorgeschlagene Hypothesen in Tag/Woche/Monat-Ansicht.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentCalendarScreen(
    onBack: () -> Unit,
    vm: ExperimentCalendarViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val cosmos = LocalCosmos.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    CosmosScaffold(
        title = "Experiment-Kalender",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
            }
        },
        actions = {
            IconButton(onClick = vm::goToToday) {
                Icon(Icons.Outlined.Today, "Heute", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ViewSwitcher(state.view, vm::setView)
            HeaderRow(state, vm)

            when (state.view) {
                CalendarView.MONAT -> MonthView(state, vm)
                CalendarView.WOCHE -> WeekView(state, vm)
                CalendarView.TAG -> DayView(state, vm)
            }
        }
    }

    state.selectedHypothesis?.let { h ->
        ModalBottomSheet(
            onDismissRequest = vm::closeDetail,
            sheetState = sheetState,
            containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLight,
        ) {
            HypothesisDetailContent(
                hypothesis = h,
                before = state.biomarkerBefore,
                after = state.biomarkerAfter,
                onSetStatus = vm::setStatus,
                onSetOutcome = vm::setOutcome,
                onSetNotes = vm::setOutcomeNotes,
                onSetFelt = vm::setFeltChange,
                onDelete = vm::deleteHypothesis,
            )
        }
    }

    if (state.showInsightPrompt) {
        AlertDialog(
            onDismissRequest = { vm.confirmInsightCreation(false) },
            title = { Text("Daraus einen Insight machen?") },
            text = {
                Text(
                    "Soll dieser Erfolg in dein Repertoire wandern? Bestehende Insights werden " +
                        "automatisch erkannt und ihre Confidence wird neu berechnet.",
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmInsightCreation(true) }) {
                    Text("Ja, in Repertoire", color = CosmosColors.AccentPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.confirmInsightCreation(false) }) { Text("Spaeter") }
            },
            containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
        )
    }
}

@Composable
private fun ViewSwitcher(current: CalendarView, onSelect: (CalendarView) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CalendarView.values().forEach { v ->
            FilterChip(
                selected = current == v,
                onClick = { onSelect(v) },
                label = {
                    Text(
                        when (v) {
                            CalendarView.TAG -> "Tag"
                            CalendarView.WOCHE -> "Woche"
                            CalendarView.MONAT -> "Monat"
                        },
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CosmosColors.AccentPrimary.copy(alpha = 0.20f),
                    selectedLabelColor = LocalCosmos.current.textPrimary,
                    containerColor = LocalCosmos.current.glassBg,
                    labelColor = LocalCosmos.current.textSecondary,
                ),
            )
        }
    }
}

@Composable
private fun HeaderRow(state: ExperimentCalendarUiState, vm: ExperimentCalendarViewModel) {
    val cosmos = LocalCosmos.current
    val title: String
    val onPrev: () -> Unit
    val onNext: () -> Unit
    when (state.view) {
        CalendarView.MONAT -> {
            title = YearMonth.from(state.anchorDate).format(MONTH_FORMAT)
            onPrev = { vm.shiftAnchor(deltaMonths = -1) }
            onNext = { vm.shiftAnchor(deltaMonths = 1) }
        }
        CalendarView.WOCHE -> {
            val wf = WeekFields.of(Locale.GERMANY)
            title = "KW ${state.anchorDate.get(wf.weekOfWeekBasedYear())} / ${state.anchorDate.year}"
            onPrev = { vm.shiftAnchor(deltaWeeks = -1) }
            onNext = { vm.shiftAnchor(deltaWeeks = 1) }
        }
        CalendarView.TAG -> {
            title = state.anchorDate.format(DAY_FORMAT)
            onPrev = { vm.shiftAnchor(deltaDays = -1) }
            onNext = { vm.shiftAnchor(deltaDays = 1) }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Outlined.ChevronLeft, "Zurueck", tint = cosmos.textPrimary)
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, "Vorwaerts", tint = cosmos.textPrimary)
        }
    }
}

@Composable
private fun MonthView(state: ExperimentCalendarUiState, vm: ExperimentCalendarViewModel) {
    val cosmos = LocalCosmos.current
    val grid = remember(state.anchorDate) { vm.monthGrid() }
    val month = YearMonth.from(state.anchorDate)
    val today = LocalDate.now()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        // Wochentag-Header
        Row {
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach { wd ->
                Text(
                    text = wd,
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        // 6 Wochen-Reihen
        for (row in 0 until 6) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (col in 0 until 7) {
                    val date = grid[row * 7 + col]
                    val inMonth = YearMonth.from(date) == month
                    val items = state.hypothesesByDate[date].orEmpty()
                    val events = state.eventsByDate[date].orEmpty()
                    val shift = state.shiftByDate[date]
                    DayCell(
                        date = date,
                        items = items,
                        events = events,
                        shift = shift,
                        isCurrentMonth = inMonth,
                        isToday = date == today,
                        onClick = { items.firstOrNull()?.let(vm::openHypothesis) },
                        modifier = Modifier.weight(1f).height(72.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    items: List<HypothesisEntity>,
    events: List<de.frank.entropyreducer.data.local.entities.CalendarEventEntity> = emptyList(),
    shift: de.frank.entropyreducer.domain.model.ShiftCode? = null,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    val borderColor = if (isToday) CosmosColors.AccentPrimary else cosmos.glassBorder
    val textColor = if (isCurrentMonth) cosmos.textPrimary else cosmos.textSecondary.copy(alpha = 0.5f)
    val shiftTint = shift?.let { shiftBackgroundFor(it) }
    val cellBg = shiftTint ?: cosmos.glassBg
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(cellBg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .clickable(enabled = items.isNotEmpty() || events.isNotEmpty(), onClick = onClick)
            .padding(4.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium,
                )
                if (shift != null && shift != de.frank.entropyreducer.domain.model.ShiftCode.FREI) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = shiftBadgeFor(shift),
                        color = CosmosColors.AccentPrimary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            // Hypothesen-Markers (farbiger Streifen) — max 2 sichtbar
            items.take(2).forEach { h ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colorForStatus(h.status)),
                )
                Spacer(Modifier.height(2.dp))
            }
            // Google-Calendar-Event-Markers (kleine Punkte) — bis zu 3 dots
            if (events.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val visible = events.take(3)
                    visible.forEach { ev ->
                        Box(
                            Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (ev.allDay) CosmosColors.AccentSecondary
                                    else CosmosColors.AccentPrimary,
                                ),
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    if (events.size > 3) {
                        Text(
                            text = "+${events.size - 3}",
                            color = cosmos.textSecondary,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            if (items.size > 2) {
                Text(
                    text = "+${items.size - 2}",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

/** Pastell-Hintergrund je Schicht — passt zum Cosmos-Theme. */
@Composable
private fun shiftBackgroundFor(shift: de.frank.entropyreducer.domain.model.ShiftCode): androidx.compose.ui.graphics.Color {
    val cosmos = LocalCosmos.current
    return when (shift) {
        de.frank.entropyreducer.domain.model.ShiftCode.TAGDIENST ->
            CosmosColors.AccentPrimary.copy(alpha = 0.15f)
        de.frank.entropyreducer.domain.model.ShiftCode.NACHTDIENST ->
            CosmosColors.AccentSecondary.copy(alpha = 0.20f)
        de.frank.entropyreducer.domain.model.ShiftCode.URLAUB ->
            CosmosColors.Success.copy(alpha = 0.18f)
        de.frank.entropyreducer.domain.model.ShiftCode.FREI -> cosmos.glassBg
        de.frank.entropyreducer.domain.model.ShiftCode.UNBEKANNT -> cosmos.glassBg
    }
}

private fun shiftBadgeFor(shift: de.frank.entropyreducer.domain.model.ShiftCode): String =
    when (shift) {
        de.frank.entropyreducer.domain.model.ShiftCode.TAGDIENST -> "T"
        de.frank.entropyreducer.domain.model.ShiftCode.NACHTDIENST -> "N"
        de.frank.entropyreducer.domain.model.ShiftCode.URLAUB -> "U"
        de.frank.entropyreducer.domain.model.ShiftCode.FREI -> ""
        de.frank.entropyreducer.domain.model.ShiftCode.UNBEKANNT -> ""
    }

@Composable
private fun WeekView(state: ExperimentCalendarUiState, vm: ExperimentCalendarViewModel) {
    val anchor = state.anchorDate
    val weekday = (anchor.dayOfWeek.value + 6) % 7
    val weekStart = anchor.minusDays(weekday.toLong())
    val days = (0 until 7).map { weekStart.plusDays(it.toLong()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEach { day ->
            item(day.toString()) {
                DayBlock(day, state.hypothesesByDate[day].orEmpty(), vm::openHypothesis)
            }
        }
    }
}

@Composable
private fun DayBlock(day: LocalDate, items: List<HypothesisEntity>, onClick: (HypothesisEntity) -> Unit) {
    val cosmos = LocalCosmos.current
    val isToday = day == LocalDate.now()
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = day.format(DAY_LONG_FORMAT) + if (isToday) " · Heute" else "",
                color = if (isToday) CosmosColors.AccentPrimary else cosmos.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (items.isEmpty()) {
                Text(
                    text = "Keine Experimente",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                items.forEach { h ->
                    HypothesisChip(h, onClick = { onClick(h) })
                }
            }
        }
    }
}

@Composable
private fun DayView(state: ExperimentCalendarUiState, vm: ExperimentCalendarViewModel) {
    val items = state.hypothesesByDate[state.anchorDate].orEmpty()
    DayBlock(state.anchorDate, items, vm::openHypothesis)
}

@Composable
private fun HypothesisChip(h: HypothesisEntity, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colorForStatus(h.status).copy(alpha = 0.20f))
            .border(BorderStroke(1.dp, colorForStatus(h.status)), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Column {
            Text(
                text = h.title,
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Status: ${h.status.name.lowercase()}" +
                    (h.outcome?.let { " · ${it.name.lowercase()}" } ?: ""),
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun HypothesisDetailContent(
    hypothesis: HypothesisEntity,
    before: BiomarkerSnapshotEntity?,
    after: BiomarkerSnapshotEntity?,
    onSetStatus: (HypothesisStatus) -> Unit,
    onSetOutcome: (HypothesisOutcome) -> Unit,
    onSetNotes: (String) -> Unit,
    onSetFelt: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    // rememberSaveable: ueberlebt Foldable-Klappung + Drehung. Vorher (remember) gingen
    // ungespeicherte Notes + Slider-Werte beim Configuration Change verloren.
    var notes by rememberSaveable(hypothesis.id) {
        mutableStateOf(hypothesis.outcomeNotes.orEmpty())
    }
    var felt by rememberSaveable(hypothesis.id) {
        mutableStateOf((hypothesis.felltEntropyChange ?: 0).toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = hypothesis.title,
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (hypothesis.description.isNotBlank()) {
            Text(hypothesis.description, color = cosmos.textPrimary, style = MaterialTheme.typography.bodyMedium)
        }
        if (hypothesis.rationale.isNotBlank()) {
            Text(
                text = "Begruendung: ${hypothesis.rationale}",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Status
        Text("Status", color = cosmos.textSecondary, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HypothesisStatus.values().forEach { st ->
                FilterChip(
                    selected = hypothesis.status == st,
                    onClick = { onSetStatus(st) },
                    label = { Text(st.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        // Outcome
        Text("Outcome", color = cosmos.textSecondary, style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HypothesisOutcome.values().forEach { oc ->
                FilterChip(
                    selected = hypothesis.outcome == oc,
                    onClick = { onSetOutcome(oc) },
                    label = { Text(oc.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        // Felt change slider
        Text(
            text = "Gefuehlte Entropie-Veraenderung: ${felt.toInt().coerceIn(-10, 10)}",
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = felt,
            onValueChange = { felt = it },
            onValueChangeFinished = { onSetFelt(felt.toInt()) },
            valueRange = -10f..10f,
            steps = 19,
            colors = SliderDefaults.colors(
                thumbColor = CosmosColors.AccentPrimary,
                activeTrackColor = CosmosColors.AccentPrimary,
                inactiveTrackColor = cosmos.glassBorder,
            ),
        )

        // Notes
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Outcome-Notes") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = cosmos.textPrimary,
                unfocusedTextColor = cosmos.textPrimary,
                focusedBorderColor = CosmosColors.AccentPrimary,
                unfocusedBorderColor = cosmos.glassBorder,
            ),
            minLines = 2,
        )
        TextButton(onClick = { onSetNotes(notes) }) { Text("Notes speichern") }

        // Biomarker-Vergleich
        if (before != null || after != null) {
            BiomarkerCompare(before, after)
        }

        // Lösch-Button
        Button(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = CosmosColors.Critical.copy(alpha = 0.20f),
                contentColor = CosmosColors.Critical,
            ),
        ) {
            Icon(Icons.Outlined.Delete, null)
            Spacer(Modifier.width(8.dp))
            Text("Hypothese loeschen")
        }
    }
}

@Composable
private fun BiomarkerCompare(
    before: BiomarkerSnapshotEntity?,
    after: BiomarkerSnapshotEntity?,
) {
    val cosmos = LocalCosmos.current
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Biomarker-Vergleich",
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            CompareRow("HRV", before?.hrvMs?.let { "%.1fms".format(it) }, after?.hrvMs?.let { "%.1fms".format(it) })
            CompareRow("Recovery", before?.recoveryScore?.let { "$it%" }, after?.recoveryScore?.let { "$it%" })
            CompareRow("Schlaf", before?.sleepPerformance?.let { "$it%" }, after?.sleepPerformance?.let { "$it%" })
        }
    }
}

@Composable
private fun CompareRow(label: String, beforeStr: String?, afterStr: String?) {
    val cosmos = LocalCosmos.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = beforeStr ?: "—",
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Outlined.ChevronRight, null, tint = cosmos.textSecondary)
        Text(
            text = afterStr ?: "—",
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun colorForStatus(s: HypothesisStatus) = when (s) {
    HypothesisStatus.AKTIV -> CosmosColors.AccentPrimary
    HypothesisStatus.VORGESCHLAGEN -> CosmosColors.AccentSecondary
    HypothesisStatus.ABGESCHLOSSEN -> CosmosColors.Success
    HypothesisStatus.ABGEBROCHEN -> CosmosColors.Critical
}

private val MONTH_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMANY)
private val DAY_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMANY)
private val DAY_LONG_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, dd.MM.", Locale.GERMANY)

