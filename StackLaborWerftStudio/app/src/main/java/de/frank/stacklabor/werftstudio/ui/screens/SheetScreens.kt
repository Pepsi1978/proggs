package de.frank.stacklabor.werftstudio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.stacklabor.werftstudio.ui.components.BreathingFab
import de.frank.stacklabor.werftstudio.ui.components.BottomSheetFrame
import de.frank.stacklabor.werftstudio.ui.components.PrimaryAction
import de.frank.stacklabor.werftstudio.ui.components.color
import de.frank.stacklabor.werftstudio.ui.model.GoalUi
import de.frank.stacklabor.werftstudio.ui.model.SignalState
import de.frank.stacklabor.werftstudio.ui.model.RelationshipUi
import de.frank.stacklabor.werftstudio.ui.model.Solubility
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.navigation.Origin
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute
import de.frank.stacklabor.werftstudio.ui.theme.GoldDarkContent
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme
import de.frank.stacklabor.werftstudio.ui.theme.bevel
import de.frank.stacklabor.werftstudio.ui.theme.darkenBy
import de.frank.stacklabor.werftstudio.ui.theme.depthShadow
import de.frank.stacklabor.werftstudio.ui.theme.lightenBy
import de.frank.stacklabor.werftstudio.ui.theme.metalRim
import kotlinx.coroutines.delay

@Composable
fun StackGoalsSheet(
    stackId: String,
    state: StackLaborUiState,
    callbacks: StackLaborCallbacks,
    underlay: @Composable () -> Unit,
) {
    var expandedGoalId by rememberSaveable { mutableStateOf<String?>(state.goals.firstOrNull { it.reason.isNotEmpty() }?.id) }
    GoldDarkContent {
        val borderColor = StackLaborTheme.colors.border
        BottomSheetFrame(
            onDismiss = callbacks.onBack,
            underlay = underlay,
            heightFraction = 0.70f,
            showGrip = true,
        ) {
            SheetHeader("Ziele — ${state.stackName}", height = 40.dp)
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.goals, key = { it.id }) { goal ->
                    GoalSelectionRow(
                        goal = goal,
                        expanded = expandedGoalId == goal.id,
                        onExpand = { expandedGoalId = if (expandedGoalId == goal.id) null else goal.id },
                        onToggle = { callbacks.onEvent(StackLaborEvent.ToggleGoal(stackId, goal.id)) },
                        onBreakdown = { callbacks.onNavigate(StackLaborRoute.Breakdown(stackId, goal.id, Origin.StackDetail)) },
                    )
                }
                if (state.goals.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxHeight(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = { callbacks.onNavigate(StackLaborRoute.GoalCatalog(Origin.StackDetail)) }) {
                                Text("Erst Ziele anlegen", color = StackLaborTheme.colors.accent)
                            }
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().height(52.dp).background(StackLaborTheme.colors.elevated)
                    .drawBehind {
                        drawLine(borderColor, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
                    }.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SecondaryAction("Ordnen", { callbacks.onNavigate(StackLaborRoute.ReorderGoals(stackId)) }, Modifier.weight(1f))
                SheetButton("Fertig", StackLaborTheme.colors.onAccent, StackLaborTheme.colors.accent, onClick = callbacks.onBack)
            }
        }
    }
}

@Composable
private fun GoalSelectionRow(
    goal: GoalUi,
    expanded: Boolean,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onBreakdown: () -> Unit,
) {
    val isExpanded = expanded && goal.reason.isNotEmpty()
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.fillMaxWidth().height(if (isExpanded) 112.dp else 64.dp)
            .depthShadow(shape, 14.dp)
            .clip(shape).background(StackLaborTheme.colors.surface)
            .border(1.dp, metalRim(0.7f), shape),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                SelectionMark(goal.selected, onToggle, Modifier.width(44.dp).fillMaxHeight())
                Row(
                    Modifier.weight(1f).fillMaxHeight().clickable(onClick = onExpand),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(30.dp), contentAlignment = Alignment.CenterStart) {
                        Box(
                            Modifier.size(20.dp).border(1.dp, StackLaborTheme.colors.border, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(goal.rank.toString(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        goal.text,
                        Modifier.weight(1f),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.width(44.dp).fillMaxHeight().clickable(onClick = onBreakdown))
            }
            if (isExpanded) {
                Text(
                    goal.reason,
                    Modifier.fillMaxWidth().height(48.dp).padding(start = 74.dp, end = 44.dp),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = StackLaborTheme.colors.textMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(Modifier.align(Alignment.CenterEnd).width(3.dp).fillMaxHeight().background(goal.signal.color()))
    }
}

@Composable
fun MedicineEditSheet(
    medicineId: String?,
    stackId: String?,
    state: StackLaborUiState,
    callbacks: StackLaborCallbacks,
    underlay: @Composable () -> Unit,
) {
    val source = state.medicines.firstOrNull { it.id == medicineId }
        ?: state.catalogMedicines.firstOrNull { it.id == medicineId }
    var name by rememberSaveable(medicineId) { mutableStateOf(source?.name.orEmpty()) }
    var manufacturer by rememberSaveable(medicineId) { mutableStateOf(source?.manufacturer.orEmpty()) }
    var form by rememberSaveable(medicineId) { mutableStateOf(source?.form ?: "Kapsel") }
    var solubility by rememberSaveable(medicineId) { mutableStateOf(source?.solubility ?: de.frank.stacklabor.werftstudio.ui.model.Solubility.Water) }
    var amount by rememberSaveable(medicineId) { mutableStateOf(source?.amount.orEmpty()) }
    var pieces by rememberSaveable(medicineId) { mutableStateOf(source?.pieces ?: "1") }
    var unit by rememberSaveable(medicineId) { mutableStateOf(source?.unit.orEmpty()) }
    var frequency by rememberSaveable(medicineId) { mutableStateOf(source?.frequency ?: "Täglich") }
    var secondDose by rememberSaveable(medicineId) { mutableStateOf(source?.secondDose ?: false) }
    var diarrheaRisk by rememberSaveable(medicineId) { mutableStateOf(source?.diarrheaRisk ?: false) }
    var excipients by rememberSaveable(medicineId) { mutableStateOf(source?.excipients.orEmpty()) }
    var together by rememberSaveable(medicineId) { mutableStateOf(source?.together ?: false) }
    var note by rememberSaveable(medicineId) { mutableStateOf(source?.note.orEmpty()) }
    var alternates by rememberSaveable(medicineId) { mutableStateOf(source?.alternates.orEmpty()) }
    var saveAttempted by rememberSaveable(medicineId) { mutableStateOf(false) }
    val warning = source?.warningReason.orEmpty()
    val usage = state.catalogMedicines.firstOrNull { it.id == medicineId }?.catalogMeta
        ?.substringBefore(" · ")?.takeIf { it.startsWith("in ") }?.let { "Gilt $it" }.orEmpty()
    GoldDarkContent {
        val borderColor = StackLaborTheme.colors.border
        BottomSheetFrame(
            onDismiss = callbacks.onBack,
            underlay = underlay,
            heightFraction = 0.90f,
            showGrip = false,
        ) {
            if (warning.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().background(StackLaborTheme.colors.red.copy(alpha = 0.10f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        "Auswertung",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = StackLaborTheme.colors.red,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        warning,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = StackLaborTheme.colors.textStrong,
                    )
                }
            }
            SheetHeader(if (medicineId == null) "Mittel anlegen" else "Mittel bearbeiten", height = 56.dp)
            LazyColumn(Modifier.weight(1f)) {
                item { FormSectionHeader("Stammdaten", usage) }
                item { CompactField("Name", name, invalid = saveAttempted && name.isBlank(), onValueChange = { name = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("name", it)) }) }
                item {
                    Column(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("Löslichkeit", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Row(Modifier.height(44.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectionPill("Wasser", solubility == Solubility.Water, Modifier.weight(1f)) {
                                solubility = Solubility.Water
                                callbacks.onEvent(StackLaborEvent.UpdateMedicineField("solubility", "water"))
                            }
                            SelectionPill("Fett", solubility == Solubility.Fat, Modifier.weight(1f)) {
                                solubility = Solubility.Fat
                                callbacks.onEvent(StackLaborEvent.UpdateMedicineField("solubility", "fat"))
                            }
                            SelectionPill("Beides", solubility == Solubility.Both, Modifier.weight(1f)) {
                                solubility = Solubility.Both
                                callbacks.onEvent(StackLaborEvent.UpdateMedicineField("solubility", "both"))
                            }
                        }
                    }
                }
                item { CompactField("Darreichungsform", form, options = MedicineFormOptions, onValueChange = { form = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("form", it)) }) }
                item { CompactField("Hersteller", manufacturer, onValueChange = { manufacturer = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("manufacturer", it)) }) }
                item { CompactToggleRow("Durchfallrisiko", diarrheaRisk, smallSwitch = true) { diarrheaRisk = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("diarrheaRisk", it.toString())) } }
                item { CompactField("Beistoffe", excipients, onValueChange = { excipients = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("excipients", it)) }) }
                item { FormSectionHeader("In diesem Stack") }
                item {
                    Column(Modifier.fillMaxWidth().height(72.dp).padding(start = 12.dp, top = 4.dp, end = 12.dp)) {
                        Row(Modifier.height(44.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompactInput(pieces, "Stückzahl", { pieces = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("pieces", it)) }, Modifier.width(88.dp))
                            CompactInput(amount, "Menge", { amount = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("amount", it)) }, Modifier.width(88.dp))
                            CompactInput(unit, "Einheit", { unit = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("unit", it)) }, Modifier.width(88.dp), options = MedicineUnitOptions)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(dosePreview(pieces, amount, unit), style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                    }
                }
                item {
                    CompactToggleRow("Zweite Dosis-Variante", secondDose, height = 72.dp, supportingText = "Frei / Dienst", smallSwitch = true) {
                        secondDose = it
                        callbacks.onEvent(StackLaborEvent.UpdateMedicineField("secondDose", it.toString()))
                    }
                }
                item { CompactField("Frequenz", frequency, options = MedicineFrequencyOptions, onValueChange = { frequency = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("frequency", it)) }) }
                item { CompactField("alterniert mit", alternates, onValueChange = { alternates = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("alternates", it)) }) }
                item {
                    CompactToggleRow("Kombi-Gruppe „zusammen einnehmen“", together, smallSwitch = true) {
                        together = it
                        callbacks.onEvent(StackLaborEvent.UpdateMedicineField("together", it.toString()))
                    }
                }
                item {
                    CompactField(
                        "Zusatztext für die KI",
                        note,
                        rowHeight = 120.dp,
                        inputHeight = 92.dp,
                        singleLine = false,
                        onValueChange = { note = it; callbacks.onEvent(StackLaborEvent.UpdateMedicineField("ai-note", it)) },
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().height(60.dp).background(StackLaborTheme.colors.elevated)
                    .drawBehind {
                        drawLine(borderColor, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
                    }.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                PrimaryAction(
                    "Sichern",
                    {
                        saveAttempted = true
                        if (name.isNotBlank()) {
                            callbacks.onEvent(StackLaborEvent.SaveMedicine(medicineId, stackId))
                            callbacks.onBack()
                        }
                    },
                    Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun BreakdownSheet(
    stackId: String,
    subjectId: String,
    state: StackLaborUiState,
    callbacks: StackLaborCallbacks,
    underlay: @Composable () -> Unit,
) {
    var showNeutral by rememberSaveable { mutableStateOf(false) }
    val subject = state.medicines.firstOrNull { it.id == subjectId }?.name
        ?: state.goals.firstOrNull { it.id == subjectId }?.text
        ?: state.medicineName
    BottomSheetFrame(
        onDismiss = callbacks.onBack,
        underlay = underlay,
        heightFraction = 0.70f,
        showGrip = false,
    ) {
        SheetHeader(subject, height = 56.dp)
        CompactToggleRow("Auch neutrale zeigen", showNeutral, background = StackLaborTheme.colors.elevated) {
            showNeutral = it
        }
        LazyColumn(Modifier.weight(1f)) {
            items(state.breakdownItems.filter { showNeutral || it.signal != SignalState.Gray }, key = { it.id }) { item ->
                RelationshipRow(item)
            }
        }
    }
}

@Composable
private fun RelationshipRow(item: RelationshipUi) {
    val verdict = when (item.signal) {
        SignalState.Green -> "stützt"
        SignalState.Gray -> "neutral"
        SignalState.Yellow, SignalState.Red -> "stört"
    }
    val reason = item.reason.ifEmpty {
        when (item.signal) {
            SignalState.Green -> "Unterstützt dieses Ziel in der aktuellen Zusammenstellung."
            SignalState.Gray -> "Keine stützende oder störende Verbindung bewertet."
            SignalState.Yellow, SignalState.Red -> "Beeinträchtigt dieses Ziel in der aktuellen Zusammenstellung."
        }
    }
    Box(
        Modifier.fillMaxWidth().height(76.dp)
            .depthShadow(RoundedCornerShape(12.dp), 14.dp)
            .background(StackLaborTheme.colors.surface),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Box(Modifier.width(29.dp), contentAlignment = Alignment.TopStart) {
                Box(Modifier.size(20.dp).border(1.dp, StackLaborTheme.colors.border, CircleShape), contentAlignment = Alignment.Center) {
                        Text(item.rank.toString(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth().height(28.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        item.title,
                        Modifier.weight(1f).padding(end = 8.dp),
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        verdict,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(lineHeight = 20.sp),
                        color = if (verdict == "stört") StackLaborTheme.colors.red else item.signal.color(),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(reason, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(StackLaborTheme.colors.border))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsSheet(
    stackId: String,
    state: StackLaborUiState,
    callbacks: StackLaborCallbacks,
    underlay: @Composable () -> Unit,
) {
    var questionText by rememberSaveable { mutableStateOf("") }
    var editingQuestionId by rememberSaveable { mutableStateOf<String?>(null) }
    var undoVisible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(undoVisible) {
        if (undoVisible) {
            kotlinx.coroutines.delay(6_000)
            undoVisible = false
        }
    }
    GoldDarkContent {
        val borderColor = StackLaborTheme.colors.border
        BottomSheetFrame(
            onDismiss = callbacks.onBack,
            underlay = underlay,
            fixedHeight = 421.dp,
            showGrip = false,
        ) {
            SheetHeader("Eigene Fragen", height = 56.dp)
            Box(Modifier.weight(1f)) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                    if (state.questions.isEmpty() && editingQuestionId != NewQuestionId) {
                        item {
                            Text(
                                "Ohne eigene Fragen antwortet die Auswertung allgemein.",
                                Modifier.padding(12.dp),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = StackLaborTheme.colors.textMuted,
                            )
                        }
                    }
                    items(state.questions, key = { it.id }) { question ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    callbacks.onEvent(StackLaborEvent.RemoveQuestion(question.id))
                                    undoVisible = true
                                    if (editingQuestionId == question.id) editingQuestionId = null
                                    true
                                } else false
                            },
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    Modifier.fillMaxSize().background(StackLaborTheme.colors.red).padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) { Text("Entfernen", color = Color.White) }
                            },
                        ) {
                            if (editingQuestionId == question.id) {
                                InlineQuestionField(questionText, { questionText = it }) {
                                    editingQuestionId = null
                                    if (questionText.isNotBlank()) callbacks.onEvent(StackLaborEvent.SaveQuestion(stackId, question.id, questionText.trim()))
                                }
                            } else {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 60.dp).background(StackLaborTheme.colors.surface)
                                        .depthShadow(RoundedCornerShape(12.dp), 14.dp)
                                        .drawBehind {
                                            drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                                        }.clickable {
                                            questionText = question.text
                                            editingQuestionId = question.id
                                        }.padding(start = 16.dp, end = 68.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(question.text, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (editingQuestionId == NewQuestionId) {
                        item(key = NewQuestionId) {
                            InlineQuestionField(questionText, { questionText = it }) {
                                editingQuestionId = null
                                if (questionText.isNotBlank()) callbacks.onEvent(StackLaborEvent.SaveQuestion(stackId, null, questionText.trim()))
                            }
                        }
                    }
                }
                if (undoVisible) {
                    Row(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = 12.dp, end = 84.dp, bottom = 16.dp).height(56.dp)
                            .background(StackLaborTheme.colors.elevated, RoundedCornerShape(12.dp))
                            .border(1.dp, StackLaborTheme.colors.border, RoundedCornerShape(12.dp)).padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Entfernt", Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { undoVisible = false; callbacks.onEvent(StackLaborEvent.Undo) }) {
                            Text("Rückgängig", color = StackLaborTheme.colors.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                BreathingFab(
                    description = "Eigene Frage hinzufügen",
                    animationsEnabled = StackLaborTheme.motionEnabled,
                    onClick = {
                        questionText = ""
                        editingQuestionId = NewQuestionId
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                )
            }
        }
    }
}

private const val NewQuestionId = "__new_question__"

@Composable
private fun InlineQuestionField(value: String, onValueChange: (String) -> Unit, onCommit: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    val borderColor = StackLaborTheme.colors.border
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Box(
        Modifier.fillMaxWidth().height(60.dp).background(StackLaborTheme.colors.surface)
            .drawBehind {
                drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }.padding(start = 16.dp, end = 68.dp, top = 8.dp, bottom = 8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize().focusRequester(focusRequester).onFocusChanged {
                if (it.isFocused) hadFocus = true else if (hadFocus) onCommit()
            },
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = StackLaborTheme.colors.textStrong, fontSize = 16.sp),
            cursorBrush = SolidColor(StackLaborTheme.colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxSize().border(1.dp, StackLaborTheme.colors.border, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { inner() }
            },
        )
    }
}

@Composable
fun StackEditSheet(
    stackId: String?,
    state: StackLaborUiState,
    callbacks: StackLaborCallbacks,
    underlay: @Composable () -> Unit,
) {
    var name by rememberSaveable(stackId, state.stackName) { mutableStateOf(if (stackId == null) "" else state.stackName) }
    var time by rememberSaveable(stackId, state.stackTime) { mutableStateOf(if (stackId == null) "" else state.stackTime) }
    var note by rememberSaveable(stackId, state.stackNote) { mutableStateOf(if (stackId == null) "" else state.stackNote) }
    var deleteWarning by rememberSaveable { mutableStateOf(false) }
    var saveAttempted by rememberSaveable { mutableStateOf(false) }
    GoldDarkContent {
        BottomSheetFrame(
            onDismiss = callbacks.onBack,
            underlay = underlay,
            fixedHeight = 340.dp,
            showGrip = true,
        ) {
            SheetHeader(if (stackId == null) "Stack anlegen" else "Stack bearbeiten", height = 40.dp, showDivider = false)
            if (deleteWarning) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(12.dp)
                        .border(1.dp, StackLaborTheme.colors.red, RoundedCornerShape(12.dp)).padding(20.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("Stack löschen?", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = StackLaborTheme.colors.red)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Dieser Stack enthält ${state.medicines.size} Mittel.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = StackLaborTheme.colors.textMuted,
                    )
                }
                SheetFooter {
                    SheetButton("Abbrechen", StackLaborTheme.colors.textStrong, StackLaborTheme.colors.surface, StackLaborTheme.colors.border) {
                        deleteWarning = false
                    }
                    SheetButton("Stack löschen", Color.White, StackLaborTheme.colors.red) {
                        deleteWarning = false
                        stackId?.let { callbacks.onEvent(StackLaborEvent.DeleteStack(it)) }
                        callbacks.onBack()
                    }
                }
            } else {
                Column(Modifier.weight(1f)) {
                    StackField("Name", name, invalid = saveAttempted && name.isBlank()) { name = it; callbacks.onEvent(StackLaborEvent.UpdateStackName(it)) }
                    StackField("Zeitpunkt", time) { time = it; callbacks.onEvent(StackLaborEvent.UpdateStackTime(it)) }
                    StackField("Einnahme-Hinweis", note) { note = it; callbacks.onEvent(StackLaborEvent.UpdateStackNote(it)) }
                }
                SheetFooter {
                    SheetButton(
                        "Stack löschen",
                        if (stackId == null) StackLaborTheme.colors.disabled else StackLaborTheme.colors.red,
                        Color.Transparent,
                        enabled = stackId != null,
                    ) { deleteWarning = true }
                    SheetButton("Sichern", StackLaborTheme.colors.onAccent, StackLaborTheme.colors.accent) {
                        saveAttempted = true
                        if (name.isNotBlank()) {
                            callbacks.onEvent(StackLaborEvent.SaveStack(stackId))
                            callbacks.onBack()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistorySheet(
    stackId: String,
    state: StackLaborUiState,
    callbacks: StackLaborCallbacks,
    underlay: @Composable () -> Unit,
) {
    val selected = state.history.filter { it.selectedForComparison }
    GoldDarkContent {
        BottomSheetFrame(
            onDismiss = callbacks.onBack,
            underlay = underlay,
            heightFraction = 0.70f,
            showGrip = false,
        ) {
            SheetHeader("Auswertungs-Historie", height = 56.dp)
            LazyColumn(Modifier.weight(1f)) {
                items(state.history, key = { it.id }) { run ->
                    Box(
                        Modifier.fillMaxWidth().height(64.dp)
                            .depthShadow(RoundedCornerShape(12.dp), 14.dp)
                            .background(if (run.selectedForComparison) StackLaborTheme.colors.elevated else StackLaborTheme.colors.surface),
                    ) {
                        Row(Modifier.fillMaxSize()) {
                            Column(
                                Modifier.weight(1f).fillMaxHeight()
                                    .clickable { callbacks.onNavigate(StackLaborRoute.Evaluation(run.id, Origin.StackDetail)) }
                                    .padding(start = 16.dp, top = 8.dp, end = 4.dp, bottom = 8.dp),
                            ) {
                                Row(Modifier.fillMaxWidth().height(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        run.date,
                                        Modifier.weight(1f),
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(run.model, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                                }
                                Row(Modifier.height(20.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${run.counts.green} grün", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.green)
                                    Text("·", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                                    Text("${run.counts.yellow} gelb", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.yellowText)
                                    Text("·", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                                    Text("${run.counts.red} rot", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.red)
                                }
                            }
                            SelectionMark(
                                selected = run.selectedForComparison,
                                onClick = { callbacks.onEvent(StackLaborEvent.SelectHistoryRun(stackId, run.id)) },
                                modifier = Modifier.width(52.dp).fillMaxHeight(),
                            )
                        }
                        if (run.selectedForComparison) {
                            Box(Modifier.width(3.dp).fillMaxHeight().background(StackLaborTheme.colors.accent))
                        }
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(StackLaborTheme.colors.border))
                    }
                }
                if (selected.size == 2) {
                    item {
                        Column(Modifier.fillMaxWidth().background(StackLaborTheme.colors.surface)) {
                            Box(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
                                Text("Unterschiede", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = StackLaborTheme.colors.textMuted, fontWeight = FontWeight.SemiBold)
                            }
                            if (state.historyChanges.isEmpty()) {
                                Box(
                                    Modifier.fillMaxWidth().height(56.dp).border(1.dp, StackLaborTheme.colors.border).padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text("Keine Zieländerungen", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                state.historyChanges.forEach { change ->
                                    Row(
                                        Modifier.fillMaxWidth().height(56.dp).border(1.dp, StackLaborTheme.colors.border).padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            change.title,
                                            Modifier.weight(1f),
                                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(change.from.historyLabel(), color = change.from.color(), fontWeight = FontWeight.SemiBold)
                                        Text(" → ", color = StackLaborTheme.colors.textMuted)
                                        Text(change.to.historyLabel(), color = change.to.color(), fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(title: String, height: Dp, showDivider: Boolean = true) {
    Box(Modifier.fillMaxWidth().height(height).background(StackLaborTheme.colors.surface)) {
        Text(
            title,
            Modifier.align(Alignment.CenterStart).fillMaxWidth().padding(horizontal = 16.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showDivider) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(StackLaborTheme.colors.border))
    }
}

@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(12.dp)).background(StackLaborTheme.colors.surface)
            .border(1.dp, StackLaborTheme.colors.border, RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = StackLaborTheme.colors.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SelectionMark(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)).then(
                if (selected) Modifier.background(StackLaborTheme.colors.accent)
                else Modifier.background(StackLaborTheme.colors.surface).border(1.dp, StackLaborTheme.colors.textMuted, RoundedCornerShape(4.dp)),
            ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = StackLaborTheme.colors.onAccent)
        }
    }
}

@Composable
private fun FormSectionHeader(title: String, meta: String = "") {
    val borderColor = StackLaborTheme.colors.border
    Row(
        Modifier.fillMaxWidth().height(32.dp).background(StackLaborTheme.colors.elevated)
            .drawBehind {
                drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        if (meta.isNotEmpty()) Text(meta, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    rowHeight: Dp = 72.dp,
    inputHeight: Dp = 44.dp,
    singleLine: Boolean = true,
    options: List<String> = emptyList(),
    invalid: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().height(rowHeight).padding(horizontal = 12.dp, vertical = 4.dp)) {
        Text(
            if (invalid) "$label fehlt" else label,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = if (invalid) StackLaborTheme.colors.red else StackLaborTheme.colors.textMuted,
        )
        Spacer(Modifier.height(4.dp))
        CompactInput(value, "", onValueChange, Modifier.fillMaxWidth(), inputHeight, singleLine, options, invalid)
    }
}

@Composable
private fun CompactInput(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    singleLine: Boolean = true,
    options: List<String> = emptyList(),
    invalid: Boolean = false,
) {
    if (options.isNotEmpty()) {
        var expanded by remember { mutableStateOf(false) }
        Box(modifier.height(height)) {
            Row(
                Modifier.fillMaxSize()
                    .depthShadow(RoundedCornerShape(12.dp), 14.dp)
                    .clip(RoundedCornerShape(12.dp)).background(StackLaborTheme.colors.surface)
                    .border(1.dp, if (invalid) StackLaborTheme.colors.red else StackLaborTheme.colors.border, RoundedCornerShape(12.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    value.ifEmpty { placeholder },
                    Modifier.weight(1f),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = if (value.isEmpty()) StackLaborTheme.colors.textMuted else StackLaborTheme.colors.textStrong,
                    fontSize = 16.sp,
                )
                Text("⌄", color = StackLaborTheme.colors.textMuted, fontSize = 16.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        return
    }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (focused) {
            delay(250L)
            bringIntoViewRequester.bringIntoView()
        }
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(height)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { focused = it.isFocused }
            .depthShadow(RoundedCornerShape(12.dp), 14.dp)
            .clip(RoundedCornerShape(12.dp)).background(StackLaborTheme.colors.surface)
            .border(1.dp, if (invalid) StackLaborTheme.colors.red else StackLaborTheme.colors.border, RoundedCornerShape(12.dp)),
        singleLine = singleLine,
        textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = StackLaborTheme.colors.textStrong, fontSize = 16.sp),
        cursorBrush = SolidColor(StackLaborTheme.colors.accent),
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 10.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
                    }
                    inner()
                }
            }
        },
    )
}

private val MedicineFormOptions = listOf("Kapsel", "Tablette", "Löffel", "Tasse", "Pulver", "Tropfen", "Sonstige")
private val MedicineUnitOptions = listOf("mg", "µg", "g", "ml", "IE", "Stück", "Tasse")
private val MedicineFrequencyOptions = listOf("Täglich", "Alle 2 Tage", "Alle 3 Tage", "Alle 7 Tage")

@Composable
private fun SelectionPill(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(CircleShape)
            .background(if (selected) StackLaborTheme.colors.accent else StackLaborTheme.colors.surface)
            .border(1.dp, if (selected) StackLaborTheme.colors.accent else StackLaborTheme.colors.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (label == "Fett") {
                Box(
                    Modifier.size(8.dp).background(StackLaborTheme.colors.fat, CircleShape)
                        .border(1.5.dp, StackLaborTheme.colors.fatBorder, CircleShape),
                )
            }
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = if (selected) StackLaborTheme.colors.onAccent else StackLaborTheme.colors.textStrong,
            )
        }
    }
}

@Composable
private fun CompactToggleRow(
    label: String,
    checked: Boolean,
    height: Dp = 56.dp,
    supportingText: String = "",
    background: Color = Color.Transparent,
    smallSwitch: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(height).background(background).clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
            if (supportingText.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    supportingText,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = if (checked) StackLaborTheme.colors.accent else StackLaborTheme.colors.textMuted,
                )
            }
        }
        CompactSwitch(checked, smallSwitch)
    }
}

@Composable
private fun CompactSwitch(checked: Boolean, small: Boolean = false) {
    val trackWidth = if (small) 36.dp else 40.dp
    val trackHeight = if (small) 20.dp else 24.dp
    val thumbSize = if (small) 16.dp else 20.dp
    val thumbOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 18.dp else 2.dp,
        animationSpec = androidx.compose.animation.core.tween(if (StackLaborTheme.motionEnabled) 180 else 0),
        label = "switchThumb",
    )
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.width(trackWidth).height(trackHeight).clip(CircleShape)
                .background(if (checked) StackLaborTheme.colors.accent else StackLaborTheme.colors.gray)
        ) {
            Box(
                Modifier.offset { IntOffset(thumbOffset.roundToPx(), 2.dp.roundToPx()) }.size(thumbSize)
                    .clip(CircleShape).background(StackLaborTheme.colors.surface),
            )
        }
    }
}

private fun dosePreview(pieces: String, amount: String, unit: String): String {
    val count = pieces.replace(',', '.').toBigDecimalOrNull()
    val quantity = amount.replace(',', '.').toBigDecimalOrNull()
    if (quantity == null) return "${pieces.ifBlank { "0" }} ${unit.ifBlank { "Stück" }}"
    val total = if (count != null) count.multiply(quantity).stripTrailingZeros().toPlainString() else "0"
    return "${pieces.ifBlank { "0" }} × ${amount.ifBlank { "0" }} $unit = $total $unit"
}

private fun SignalState.historyLabel(): String = when (this) {
    SignalState.Green -> "grün"
    SignalState.Yellow -> "gelb"
    SignalState.Red -> "rot"
    SignalState.Gray -> "neutral"
}

@Composable
private fun StackField(label: String, value: String, invalid: Boolean = false, onValueChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column(
            Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(StackLaborTheme.colors.surface)
                .border(1.dp, if (invalid) StackLaborTheme.colors.red else StackLaborTheme.colors.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                if (invalid) "Name fehlt" else label,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = if (invalid) StackLaborTheme.colors.red else StackLaborTheme.colors.textMuted,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().weight(1f),
                singleLine = true,
                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = StackLaborTheme.colors.textStrong, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(StackLaborTheme.colors.accent),
            )
        }
    }
}

@Composable
private fun SheetFooter(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    val borderColor = StackLaborTheme.colors.border
    Row(
        Modifier.fillMaxWidth().height(60.dp).background(StackLaborTheme.colors.elevated)
            .drawBehind {
                drawLine(borderColor, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
            }.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SheetButton(
    label: String,
    contentColor: Color,
    background: Color,
    borderColor: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(12.dp)).background(background)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp)) else Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
