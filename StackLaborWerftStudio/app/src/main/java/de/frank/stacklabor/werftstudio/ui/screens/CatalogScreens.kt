package de.frank.stacklabor.werftstudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.werftstudio.ui.components.BreathingFab
import de.frank.stacklabor.werftstudio.ui.components.EmptyState
import de.frank.stacklabor.werftstudio.ui.components.GlassHeader
import de.frank.stacklabor.werftstudio.ui.components.IconTouchButton
import de.frank.stacklabor.werftstudio.ui.components.PrimaryAction
import de.frank.stacklabor.werftstudio.ui.components.SearchField
import de.frank.stacklabor.werftstudio.ui.components.SolubilityMarks
import de.frank.stacklabor.werftstudio.ui.components.WerftScreen
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.navigation.Origin
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme
import de.frank.stacklabor.werftstudio.ui.theme.bevel
import de.frank.stacklabor.werftstudio.ui.theme.darkenBy
import de.frank.stacklabor.werftstudio.ui.theme.depthShadow
import de.frank.stacklabor.werftstudio.ui.theme.lightenBy
import de.frank.stacklabor.werftstudio.ui.theme.metalRim

@Composable
fun GoalCatalogScreen(state: StackLaborUiState, animationsEnabled: Boolean, callbacks: StackLaborCallbacks) {
    var selectedGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var goalText by rememberSaveable { mutableStateOf("") }
    val goals = state.goals.filter { it.text.contains(state.searchQuery, ignoreCase = true) }
    val openEditor: (String?, String) -> Unit = { id, text ->
        selectedGoalId = id
        goalText = text
        editorOpen = true
    }
    val closeEditor = {
        selectedGoalId = null
        goalText = ""
        editorOpen = false
    }
    WerftScreen(goldDark = true) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader("Ziel-Katalog", onBack = callbacks.onBack)
            SearchField(state.searchQuery, "", { callbacks.onEvent(StackLaborEvent.ChangeSearch(it)) }, Modifier.padding(horizontal = 12.dp))
            GoalCatalogList(
                goals = goals,
                editorOpen = editorOpen,
                selectedGoalId = selectedGoalId,
                editorText = goalText,
                onEditorTextChange = { goalText = it.take(200) },
                onEdit = { openEditor(it.id, it.text) },
                onAdd = { openEditor(null, "") },
                onSave = {
                    if (goalText.isNotBlank()) {
                        callbacks.onEvent(
                            selectedGoalId?.let { StackLaborEvent.EditGoal(it, goalText) }
                                ?: StackLaborEvent.AddGoal(goalText),
                        )
                        closeEditor()
                    }
                },
                onDelete = {
                    selectedGoalId?.let { callbacks.onEvent(StackLaborEvent.DeleteGoal(it)) }
                    closeEditor()
                },
                onCancel = closeEditor,
                modifier = Modifier.fillMaxSize(),
            )
        }
        BreathingFab("Ziel hinzufügen", animationsEnabled, { openEditor(null, "") }, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }
}

@Composable
private fun GoalCatalogList(
    goals: List<de.frank.stacklabor.werftstudio.ui.model.GoalUi>,
    editorOpen: Boolean,
    selectedGoalId: String?,
    editorText: String,
    onEditorTextChange: (String) -> Unit,
    onEdit: (de.frank.stacklabor.werftstudio.ui.model.GoalUi) -> Unit,
    onAdd: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    if (goals.isEmpty() && !editorOpen) {
        EmptyState("Nichts gefunden", "Ziel hinzufügen", modifier, onAdd)
    } else {
        LazyColumn(
            modifier.padding(horizontal = 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (editorOpen && selectedGoalId == null) {
                item(key = "new-goal") {
                    GoalCatalogCard(
                        title = editorText,
                        meta = "in 0 Stacks verwendet",
                        editing = true,
                        adding = true,
                        onTitleChange = onEditorTextChange,
                        onEdit = {},
                        onSave = onSave,
                        onDelete = onCancel,
                    )
                }
            }
            items(goals, key = { it.id }) { goal ->
                val editing = editorOpen && selectedGoalId == goal.id
                GoalCatalogCard(
                    title = if (editing) editorText else goal.text,
                    meta = goal.usage,
                    editing = editing,
                    adding = false,
                    onTitleChange = onEditorTextChange,
                    onEdit = { onEdit(goal) },
                    onSave = onSave,
                    onDelete = onDelete,
                )
            }
        }
    }
}

@Composable
private fun GoalCatalogCard(
    title: String,
    meta: String,
    editing: Boolean,
    adding: Boolean,
    onTitleChange: (String) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = StackLaborTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(editing) {
        if (editing) focusRequester.requestFocus()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .depthShadow(shape, 14.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, metalRim(0.7f), shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp), verticalArrangement = Arrangement.Center) {
            if (editing) {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 40.dp)
                        .focusRequester(focusRequester)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent.copy(alpha = 0.08f))
                        .drawBehind { drawRect(colors.accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                        color = colors.textStrong,
                        fontWeight = FontWeight.Medium,
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
                )
            } else {
                Text(
                    title,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(meta, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1)
        }
        if (editing) {
            IconTouchButton(if (adding) "Hinzufügen abbrechen" else "Ziel löschen", onDelete) {
                Icon(
                    if (adding) Icons.Default.Close else Icons.Default.Delete,
                    null,
                    Modifier.size(20.dp),
                    tint = if (adding) colors.textMuted else colors.red,
                )
            }
            IconTouchButton("Ziel sichern", { if (title.isNotBlank()) onSave() }) {
                Icon(Icons.Default.Check, null, Modifier.size(20.dp), tint = if (title.isNotBlank()) colors.accent else colors.disabled)
            }
        } else {
            IconTouchButton("$title bearbeiten", onEdit) {
                Icon(Icons.Default.Edit, null, Modifier.size(24.dp), tint = colors.textMuted)
            }
        }
    }
}

@Composable
fun MedicineCatalogScreen(stackId: String?, state: StackLaborUiState, animationsEnabled: Boolean, callbacks: StackLaborCallbacks) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var mergeMode by rememberSaveable { mutableStateOf(false) }
    var mergeSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var undoVisible by rememberSaveable { mutableStateOf(false) }
    val medicines = state.catalogMedicines.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
    val selectMedicine: (String) -> Unit = { id ->
        if (mergeMode) {
            when (id) {
                mergeSourceId -> {
                    mergeSourceId = selectedId
                    selectedId = null
                }
                selectedId -> selectedId = null
                else -> if (mergeSourceId == null) mergeSourceId = id else if (selectedId == null) selectedId = id
            }
        }
    }
    val finishMerge: (String, String) -> Unit = { keepId, replaceId ->
        callbacks.onEvent(StackLaborEvent.MergeMedicines(keepId, replaceId))
        undoVisible = true
        mergeMode = false
        mergeSourceId = null
        selectedId = null
    }
    val cancelMerge = {
        mergeMode = false
        mergeSourceId = null
        selectedId = null
    }
    WerftScreen(goldDark = true) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader("Mittel-Katalog", onBack = callbacks.onBack, onOverflow = { menuOpen = !menuOpen })
            Box(Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 12.dp, vertical = 2.dp)) {
                SearchField(
                    state.searchQuery,
                    "Suchen",
                    { callbacks.onEvent(StackLaborEvent.ChangeSearch(it)) },
                    fieldHeight = 36.dp,
                )
            }
            MedicineCatalogList(
                stackId = stackId,
                medicines = medicines,
                mergeMode = mergeMode,
                selectedIds = setOfNotNull(mergeSourceId, selectedId),
                onSelect = selectMedicine,
                callbacks = callbacks,
                modifier = Modifier.fillMaxSize(),
            )
        }
        BreathingFab(
            "Mittel hinzufügen",
            animationsEnabled,
            { callbacks.onNavigate(StackLaborRoute.MedicineEdit(null, stackId, Origin.MedicineCatalog)) },
            Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
        if (menuOpen) {
            val menuShape = RoundedCornerShape(12.dp)
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 8.dp)
                    .width(192.dp)
                    .depthShadow(menuShape, 26.dp, strength = 1.4f)
                    .clip(menuShape)
                    .background(Brush.verticalGradient(listOf(StackLaborTheme.colors.elevated, StackLaborTheme.colors.surface)))
                    .border(1.dp, metalRim(0.85f), menuShape),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            menuOpen = false
                            mergeMode = true
                            mergeSourceId = null
                            selectedId = null
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("Zusammenführen", color = StackLaborTheme.colors.textStrong, fontWeight = FontWeight.Medium)
                }
            }
        }
        AnimatedVisibility(mergeMode, Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 84.dp)) {
            val source = medicines.firstOrNull { it.id == mergeSourceId }
            val selected = medicines.firstOrNull { it.id == selectedId }
            Column(
                Modifier
                    .fillMaxWidth()
                    .depthShadow(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), 26.dp, strength = 1.4f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(StackLaborTheme.colors.elevated)
                    .border(1.dp, metalRim(0.85f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(12.dp),
            ) {
                Text(
                    if (source != null && selected != null) "Bleibendes Mittel wählen" else "Zwei Mittel wählen",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                if (source != null && selected != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryAction(source.name, { finishMerge(source.id, selected.id) }, Modifier.weight(1f))
                        PrimaryAction(selected.name, { finishMerge(selected.id, source.id) }, Modifier.weight(1f))
                    }
                }
                TextButton(onClick = cancelMerge, Modifier.fillMaxWidth()) { Text("Abbrechen", color = StackLaborTheme.colors.textStrong) }
            }
        }
        LaunchedEffect(undoVisible) {
            if (undoVisible) {
                kotlinx.coroutines.delay(6_000)
                undoVisible = false
            }
        }
        AnimatedVisibility(undoVisible, Modifier.align(Alignment.BottomCenter).padding(start = 12.dp, end = 12.dp, bottom = 16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(end = 68.dp)
                    .height(48.dp)
                    .depthShadow(RoundedCornerShape(12.dp), 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StackLaborTheme.colors.elevated)
                    .border(1.dp, metalRim(0.7f), RoundedCornerShape(12.dp))
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Entfernt")
                TextButton(onClick = { callbacks.onEvent(StackLaborEvent.Undo); undoVisible = false }) { Text("Rückgängig") }
            }
        }
    }
}

@Composable
private fun MedicineCatalogList(
    stackId: String?,
    medicines: List<de.frank.stacklabor.werftstudio.ui.model.MedicineUi>,
    mergeMode: Boolean,
    selectedIds: Set<String>,
    onSelect: (String) -> Unit,
    callbacks: StackLaborCallbacks,
    modifier: Modifier,
) {
    if (medicines.isEmpty()) {
        EmptyState("Nichts gefunden", "Neu anlegen", modifier) { callbacks.onNavigate(StackLaborRoute.MedicineEdit(null, stackId, Origin.MedicineCatalog)) }
    } else {
        LazyColumn(
            modifier.padding(horizontal = 12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = if (mergeMode) 190.dp else 84.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(medicines, key = { it.id }) { medicine ->
                MedicineCatalogCard(
                    medicine = medicine,
                    mergeMode = mergeMode,
                    selected = medicine.id in selectedIds,
                    onClick = {
                        if (mergeMode) onSelect(medicine.id)
                        else if (stackId != null) callbacks.onEvent(StackLaborEvent.AddMedicineToStack(stackId, medicine.id))
                        else callbacks.onNavigate(StackLaborRoute.MedicineEdit(medicine.id, stackId, Origin.MedicineCatalog))
                    },
                )
            }
        }
    }
}

@Composable
private fun MedicineCatalogCard(
    medicine: de.frank.stacklabor.werftstudio.ui.model.MedicineUi,
    mergeMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = StackLaborTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .depthShadow(shape, 14.dp)
            .clip(shape)
            .background(
                if (selected) Brush.linearGradient(listOf(colors.elevated, colors.elevated))
                else Brush.linearGradient(listOf(colors.surface, colors.accent.copy(alpha = 0.12f))),
            )
            .then(if (selected) Modifier.border(1.dp, metalRim(1f), shape) else Modifier)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (mergeMode) {
            Box(Modifier.width(46.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (selected) Modifier.background(colors.accent)
                            else Modifier.border(1.dp, colors.border, RoundedCornerShape(50)),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(colors.surface))
                }
            }
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SolubilityMarks(medicine.solubility)
                Spacer(Modifier.width(6.dp))
                Text(
                    medicine.name,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                medicine.catalogMeta,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
