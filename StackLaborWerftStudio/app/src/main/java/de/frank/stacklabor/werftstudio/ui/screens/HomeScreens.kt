package de.frank.stacklabor.werftstudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.stacklabor.werftstudio.ui.components.AdaptiveSplit
import de.frank.stacklabor.werftstudio.ui.components.AnimatedGradientHeader
import de.frank.stacklabor.werftstudio.ui.components.BreathingFab
import de.frank.stacklabor.werftstudio.ui.components.GlassHeader
import de.frank.stacklabor.werftstudio.ui.components.IconTouchButton
import de.frank.stacklabor.werftstudio.ui.components.PrimaryAction
import de.frank.stacklabor.werftstudio.ui.components.SearchField
import de.frank.stacklabor.werftstudio.ui.components.SectionTitle
import de.frank.stacklabor.werftstudio.ui.components.SelectPill
import de.frank.stacklabor.werftstudio.ui.components.SignalCountsRow
import de.frank.stacklabor.werftstudio.ui.components.SolubilityLabel
import de.frank.stacklabor.werftstudio.ui.components.rememberReorder
import androidx.compose.ui.zIndex
import de.frank.stacklabor.werftstudio.ui.components.StackCard
import de.frank.stacklabor.werftstudio.ui.components.WerftScreen
import de.frank.stacklabor.werftstudio.ui.components.color
import de.frank.stacklabor.werftstudio.ui.model.DoseVariant
import de.frank.stacklabor.werftstudio.ui.model.EvaluationState
import de.frank.stacklabor.werftstudio.ui.model.GoalInputState
import de.frank.stacklabor.werftstudio.ui.model.MedicineUi
import de.frank.stacklabor.werftstudio.ui.model.SignalState
import de.frank.stacklabor.werftstudio.ui.model.SortMode
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.navigation.Origin
import de.frank.stacklabor.werftstudio.ui.navigation.StackLaborRoute
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme
import de.frank.stacklabor.werftstudio.ui.components.GoldSurface
import de.frank.stacklabor.werftstudio.ui.components.RaisedPanel
import de.frank.stacklabor.werftstudio.ui.components.SignalBar
import de.frank.stacklabor.werftstudio.ui.components.WerftCheckbox
import de.frank.stacklabor.werftstudio.ui.theme.bevel
import de.frank.stacklabor.werftstudio.ui.theme.darkenBy
import de.frank.stacklabor.werftstudio.ui.theme.depthShadow
import de.frank.stacklabor.werftstudio.ui.theme.lightenBy
import de.frank.stacklabor.werftstudio.ui.theme.metalRim

@Composable
fun HomeScreen(state: StackLaborUiState, animationsEnabled: Boolean, callbacks: StackLaborCallbacks) {
    var homeMenuOpen by rememberSaveable { mutableStateOf(false) }
    WerftScreen {
        val headerContentColor = if (StackLaborTheme.dark) Color(0xFF141A24) else Color.White
        Column(Modifier.fillMaxSize()) {
            AnimatedGradientHeader(animationsEnabled) {
                Text(
                    "StackLabor",
                    Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 12.dp),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 28.sp,
                    color = headerContentColor,
                )
                Row(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 12.dp)) {
                    IconTouchButton(
                        if (state.appearance.name == "Light") "Dunkle Darstellung einschalten" else "Helle Darstellung einschalten",
                        { callbacks.onEvent(StackLaborEvent.ToggleAppearance) },
                    ) {
                        Icon(
                            if (state.appearance.name == "Light") Icons.Default.LightMode else Icons.Default.DarkMode,
                            null,
                            Modifier.size(22.dp),
                            tint = headerContentColor,
                        )
                    }
                    IconTouchButton("Ziel-Katalog öffnen", { callbacks.onNavigate(StackLaborRoute.GoalCatalog(Origin.Home)) }) {
                        Icon(Icons.Default.TrackChanges, null, Modifier.size(22.dp), tint = headerContentColor)
                    }
                    IconTouchButton("Einstellungen öffnen", { callbacks.onNavigate(StackLaborRoute.Settings) }) {
                        Icon(Icons.Default.Settings, null, Modifier.size(22.dp), tint = headerContentColor)
                    }
                    IconTouchButton("Weitere Optionen", { homeMenuOpen = !homeMenuOpen }) {
                        Icon(Icons.Default.MoreVert, null, Modifier.size(24.dp), tint = headerContentColor)
                    }
                }
                DoseVariantSwitch(state.doseVariant, callbacks, Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 12.dp))
            }
            val stripBorder = StackLaborTheme.colors.border
            Row(
                Modifier.fillMaxWidth().height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                StackLaborTheme.colors.elevated.lightenBy(0.10f),
                                StackLaborTheme.colors.elevated.darkenBy(0.06f),
                            ),
                        ),
                    )
                    .drawBehind {
                        drawLine(stripBorder.copy(alpha = 0.6f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                    }
                    .clickable {
                        callbacks.onNavigate(StackLaborRoute.AllStacks)
                    }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Alle Stacks zusammen prüfen", Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                Text(state.evaluationMeta, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
            }
            Box(Modifier.weight(1f)) {
                AdaptiveSplit(
                    narrow = {
                        StackList(state, animationsEnabled, callbacks, Modifier.fillMaxSize())
                    },
                    primary = {
                        StackList(state, animationsEnabled, callbacks, Modifier.fillMaxSize())
                    },
                    secondary = {
                        Column(Modifier.fillMaxSize().padding(12.dp)) {
                            SectionTitle("Gesamtauswertung")
                            EvaluationSummaryCard(
                                title = "Alle Stacks im Blick",
                                body = state.evaluationMeta,
                                action = "Gesamtauswertung öffnen",
                            ) { callbacks.onNavigate(StackLaborRoute.AllStacks) }
                        }
                    },
                )
                BreathingFab(
                    description = "Stack anlegen",
                    animationsEnabled = animationsEnabled,
                    onClick = { callbacks.onNavigate(StackLaborRoute.StackEdit(null, Origin.Home)) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
        if (homeMenuOpen) {
            OverflowMenu(Modifier.align(Alignment.TopEnd).padding(top = 96.dp, end = 8.dp)) {
                MenuItem("Alle Stacks exportieren") {
                    homeMenuOpen = false
                    callbacks.onEvent(StackLaborEvent.ExportAllStacks)
                }
                MenuTrenner()
                MenuItem("Alle Stacks zusammen prüfen") {
                    homeMenuOpen = false
                    callbacks.onNavigate(StackLaborRoute.AllStacks)
                }
                MenuTrenner()
                MenuItem("Daten als JSON sichern") {
                    homeMenuOpen = false
                    callbacks.onEvent(StackLaborEvent.ExportData)
                }
            }
        }
    }
}

@Composable
private fun DoseVariantSwitch(variant: DoseVariant, callbacks: StackLaborCallbacks, modifier: Modifier = Modifier) {
    val colors = StackLaborTheme.colors
    Row(
        modifier
            .width(112.dp)
            .height(28.dp)
            .clip(CircleShape)
            // Sunken track, so the selected half reads as a raised knob inside it.
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.22f), colors.elevated)))
            .border(1.dp, Color.Black.copy(alpha = 0.25f), CircleShape)
            .padding(2.dp),
    ) {
        DoseVariantOption("Frei", variant == DoseVariant.Frei, Modifier.weight(1f)) {
            callbacks.onEvent(StackLaborEvent.ChangeDoseVariant(DoseVariant.Frei))
        }
        DoseVariantOption("Dienst", variant == DoseVariant.Dienst, Modifier.weight(1f)) {
            callbacks.onEvent(StackLaborEvent.ChangeDoseVariant(DoseVariant.Dienst))
        }
    }
}

@Composable
private fun DoseVariantOption(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = StackLaborTheme.colors
    Box(
        modifier
            .fillMaxHeight()
            .then(if (selected) Modifier.depthShadow(CircleShape, 6.dp) else Modifier)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier
                        .background(Brush.verticalGradient(listOf(colors.surface.lightenBy(0.4f), colors.surface)))
                        .border(1.dp, metalRim(0.55f), CircleShape)
                } else Modifier,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.accent else colors.textMuted,
        )
    }
}

@Composable
private fun StackList(
    state: StackLaborUiState,
    animationsEnabled: Boolean,
    callbacks: StackLaborCallbacks,
    modifier: Modifier = Modifier,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorder = rememberReorder(
        source = state.stacks,
        keyOf = { it.id },
        listState = listState,
        rowHeight = StackLaborTheme.dimens.stackHeight + 8.dp,
        onDropped = { ids -> callbacks.onEvent(StackLaborEvent.ApplyStackOrder(ids)) },
    )
    LazyColumn(
        modifier.padding(horizontal = 12.dp).then(reorder.dragModifier()),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(reorder.items, key = { it.id }) { stack ->
            val gezogen = reorder.isDragged(stack)
            StackCard(
                stack = stack,
                animationsEnabled = animationsEnabled,
                onOpen = { callbacks.onNavigate(StackLaborRoute.StackDetail(stack.id)) },
                onCatalog = { callbacks.onNavigate(StackLaborRoute.MedicineCatalog(stack.id, Origin.Home)) },
                modifier = Modifier
                    .zIndex(if (gezogen) 1f else 0f)
                    .then(if (animationsEnabled && !gezogen) Modifier.animateItem() else Modifier)
                    .then(reorder.liftModifier(stack)),
            )
        }
    }
}

@Composable
fun StackDetailScreen(stackId: String, state: StackLaborUiState, animationsEnabled: Boolean, callbacks: StackLaborCallbacks) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var deleteCandidateId by rememberSaveable { mutableStateOf<String?>(null) }
    val filtered = state.medicines.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
    val deleteCandidate = deleteCandidateId?.let { id -> state.medicines.firstOrNull { it.id == id } }
    WerftScreen {
        Column(Modifier.fillMaxSize()) {
            GlassHeader(
                title = state.stackName,
                subtitle = state.stackTime,
                onBack = callbacks.onBack,
                onOverflow = { menuOpen = !menuOpen },
            )
            TargetStrip(state) { callbacks.onNavigate(StackLaborRoute.StackGoals(stackId, Origin.StackDetail)) }
            ToolsRow(state, searchOpen, { searchOpen = !searchOpen }, callbacks)
            AnimatedVisibility(searchOpen) {
                SearchField(state.searchQuery, "Mittel suchen", { callbacks.onEvent(StackLaborEvent.ChangeSearch(it)) }, Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
            }
            AdaptiveSplit(
                narrow = {
                    Box(Modifier.fillMaxSize()) {
                        MedicineList(stackId = stackId, filtered = filtered, sortMode = state.sortMode, fatFirst = state.solubilityFatFirst, evaluationId = state.latestEvaluationId, evaluationMeta = state.evaluationMeta, callbacks = callbacks, onRequestDelete = { deleteCandidateId = it.id }, modifier = Modifier.fillMaxSize().padding(bottom = if (state.evaluationState == EvaluationState.Running) 148.dp else 52.dp))
                        EvaluateFooter(
                            callbacks = callbacks,
                            running = state.evaluationState == EvaluationState.Running,
                            stage = state.streamedEvaluationText.lines().lastOrNull { it.isNotBlank() }.orEmpty(),
                            animationsEnabled = animationsEnabled,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                        BreathingFab("Mittel hinzufügen", animationsEnabled, { callbacks.onNavigate(StackLaborRoute.MedicineCatalog(stackId, Origin.StackDetail)) }, Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 68.dp))
                    }
                },
                primary = {
                    Box(Modifier.fillMaxSize()) {
                        MedicineList(stackId, filtered, state.sortMode, state.solubilityFatFirst, state.latestEvaluationId, state.evaluationMeta, callbacks, { deleteCandidateId = it.id }, Modifier.fillMaxSize().padding(bottom = 68.dp))
                        BreathingFab("Mittel hinzufügen", animationsEnabled, { callbacks.onNavigate(StackLaborRoute.MedicineCatalog(stackId, Origin.StackDetail)) }, Modifier.align(Alignment.BottomEnd).padding(12.dp))
                    }
                },
                secondary = {
                    Column(Modifier.fillMaxSize().padding(12.dp)) {
                        SectionTitle("Auswertungsstatus")
                        EvaluationStateCard(state.evaluationState, callbacks, withoutGoals = state.goals.none { it.selected })
                        Spacer(Modifier.height(8.dp))
                        EvaluationSummaryCard(
                            "Auswertung im Vollbild",
                            state.evaluationMeta,
                            "Öffnen",
                        ) { state.latestEvaluationId?.let { callbacks.onNavigate(StackLaborRoute.Evaluation(it, Origin.StackDetail)) } }
                        Spacer(Modifier.weight(1f))
                        PrimaryAction("Diesen Stack auswerten", { callbacks.onEvent(StackLaborEvent.EvaluateStack) }, Modifier.fillMaxWidth())
                    }
                },
            )
        }
        if (menuOpen) {
            val menuShape = RoundedCornerShape(12.dp)
            Column(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 8.dp)
                    .width(232.dp)
                    .depthShadow(menuShape, 28.dp, strength = 1.5f)
                    .clip(menuShape)
                    .background(Brush.verticalGradient(listOf(StackLaborTheme.colors.surface.lightenBy(0.20f), StackLaborTheme.colors.elevated)))
                    .bevel()
                    .border(1.5.dp, metalRim(0.95f), menuShape),
            ) {
                MenuItem("Stack bearbeiten") { menuOpen = false; callbacks.onNavigate(StackLaborRoute.StackEdit(stackId, Origin.StackDetail)) }
                MenuTrenner()
                MenuItem("Stack exportieren") { menuOpen = false; callbacks.onEvent(StackLaborEvent.ExportStack(stackId)) }
                MenuTrenner()
                MenuItem("Eigene Fragen") { menuOpen = false; callbacks.onNavigate(StackLaborRoute.Questions(stackId, Origin.StackDetail)) }
                MenuTrenner()
                MenuItem("Historie") { menuOpen = false; callbacks.onNavigate(StackLaborRoute.History(stackId, Origin.StackDetail)) }
            }
        }
        if (deleteCandidate != null) {
            DeleteMedicineDialog(
                name = deleteCandidate.name,
                onConfirm = {
                    callbacks.onEvent(StackLaborEvent.RemoveMedicineFromStack(stackId, deleteCandidate.id))
                    deleteCandidateId = null
                },
                onDismiss = { deleteCandidateId = null },
            )
        }
    }
}

@Composable
private fun TargetStrip(state: StackLaborUiState, onClick: () -> Unit) {
    val selectedGoals = state.goals.filter { it.selected }
    val colors = StackLaborTheme.colors
    Row(
        Modifier.fillMaxWidth().height(40.dp)
            .background(Brush.verticalGradient(listOf(colors.surface.lightenBy(0.18f), colors.surface)))
            .drawBehind {
                drawLine(colors.border.copy(alpha = 0.5f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .clickable(onClick = onClick).padding(start = 13.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().padding(vertical = 6.dp).clip(CircleShape).background(colors.red))
        Spacer(Modifier.width(10.dp))
        Text("Ziele ${selectedGoals.size}", style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        SignalCountsRow(
            de.frank.stacklabor.werftstudio.ui.model.SignalCounts(
                selectedGoals.count { it.signal == de.frank.stacklabor.werftstudio.ui.model.SignalState.Green },
                selectedGoals.count { it.signal == de.frank.stacklabor.werftstudio.ui.model.SignalState.Yellow },
                selectedGoals.count { it.signal == de.frank.stacklabor.werftstudio.ui.model.SignalState.Red },
            ),
        )
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.ExpandMore, null, Modifier.size(24.dp), tint = StackLaborTheme.colors.textMuted)
    }
}

@Composable
private fun ToolsRow(state: StackLaborUiState, searchOpen: Boolean, onSearch: () -> Unit, callbacks: StackLaborCallbacks) {
    val toolsBorder = StackLaborTheme.colors.border
    Row(
        Modifier.fillMaxWidth().height(36.dp)
            .background(
                Brush.verticalGradient(
                    listOf(StackLaborTheme.colors.elevated.darkenBy(0.06f), StackLaborTheme.colors.elevated.lightenBy(0.06f)),
                ),
            )
            .drawBehind {
                drawLine(toolsBorder.copy(alpha = 0.45f), Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .padding(start = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ist die Ansicht aktiv, sagt die Beschriftung auch, welche Richtung gerade gilt.
        SelectPill(
            if (state.sortMode == SortMode.Loeslichkeit) {
                if (state.solubilityFatFirst) "Fett zuerst" else "Wasser zuerst"
            } else "Löslichkeit",
            state.sortMode == SortMode.Loeslichkeit,
        ) { callbacks.onEvent(StackLaborEvent.ChangeSortMode(SortMode.Loeslichkeit)) }
        SelectPill("Einnahme", state.sortMode == SortMode.Einnahme) { callbacks.onEvent(StackLaborEvent.ChangeSortMode(SortMode.Einnahme)) }
        Spacer(Modifier.weight(1f))
        IconTouchButton("Mittel suchen", onSearch) { Icon(Icons.Default.Search, null, tint = if (searchOpen) StackLaborTheme.colors.accent else StackLaborTheme.colors.textStrong) }
    }
}

/** Höhe einer Mittel-Zeile samt Abstand — Maß für das Umsortieren per Ziehen. */
private val MedicineRowHeight = 64.dp

@Composable
private fun MedicineList(
    stackId: String,
    filtered: List<de.frank.stacklabor.werftstudio.ui.model.MedicineUi>,
    sortMode: SortMode,
    fatFirst: Boolean,
    evaluationId: String?,
    evaluationMeta: String,
    callbacks: StackLaborCallbacks,
    onRequestDelete: (MedicineUi) -> Unit,
    modifier: Modifier,
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorder = rememberReorder(
        source = filtered,
        keyOf = { it.id },
        listState = listState,
        rowHeight = MedicineRowHeight,
        onGrabbed = {
            // Die Löslichkeits-Ansicht sortiert selbst — eine von Hand gezogene Reihenfolge
            // wäre dort sofort wieder überschrieben. Deshalb beim Greifen umschalten.
            if (sortMode == SortMode.Loeslichkeit) callbacks.onEvent(StackLaborEvent.ChangeSortMode(SortMode.Einnahme))
        },
        onDropped = { ids -> callbacks.onEvent(StackLaborEvent.ApplyMedicineOrder(stackId, ids)) },
    )
    // LazyColumn verankert sich am obersten sichtbaren Schlüssel. Während eines Griffs darf
    // ein Sortierwechsel die Geometrie der laufenden Geste jedoch nicht teleportieren.
    LaunchedEffect(sortMode, fatFirst) {
        if (reorder.draggedKey == null) listState.scrollToItem(0)
    }
    LazyColumn(
        modifier.padding(horizontal = 12.dp).then(reorder.dragModifier()),
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 80.dp),
    ) {
        if (filtered.isEmpty()) {
            item { de.frank.stacklabor.werftstudio.ui.components.EmptyState("Nichts gefunden", "Neu anlegen") { callbacks.onNavigate(StackLaborRoute.MedicineEdit(null, stackId, Origin.StackDetail)) } }
        }
        itemsIndexed(reorder.items, key = { _, medicine -> medicine.id }) { index, medicine ->
            var horizontalOffset by remember(medicine.id) { mutableFloatStateOf(0f) }
            var swipeWidth by remember(medicine.id) { mutableFloatStateOf(1f) }
            var swiping by remember(medicine.id) { mutableStateOf(false) }
            val gezogen = reorder.isDragged(medicine)
            // Beim Loslassen fährt die Zeile weich zurück, statt zu springen.
            val restingOffset by animateFloatAsState(
                targetValue = horizontalOffset,
                animationSpec = tween(if (StackLaborTheme.motionEnabled) 180 else 0),
                label = "swipeOffset",
            )
            val shownOffset = if (swiping) horizontalOffset else restingOffset
            val gestureModifier = Modifier
                // Die Zeile in der Hand liegt über allen anderen und weicht nicht aus.
                .zIndex(if (gezogen) 1f else 0f)
                .then(if (StackLaborTheme.motionEnabled && !gezogen) Modifier.animateItem() else Modifier)
                .then(reorder.liftModifier(medicine))
            SwipeToDeleteRow(
                offset = shownOffset,
                width = swipeWidth,
                modifier = gestureModifier,
                onWidth = { swipeWidth = it },
                onDrag = { amount ->
                    swiping = true
                    horizontalOffset = (horizontalOffset + amount).coerceIn(-swipeWidth, 0f)
                },
                onDragEnd = {
                    swiping = false
                    // Weit genug gewischt: erst fragen, dann löschen.
                    if (horizontalOffset < -swipeWidth * 0.32f) onRequestDelete(medicine)
                    horizontalOffset = 0f
                },
                onDragCancel = { swiping = false; horizontalOffset = 0f },
            ) {
                StackMedicineRow(
                    medicine = medicine,
                    intakePosition = if (sortMode == SortMode.Einnahme) index + 1 else null,
                    onOpen = { callbacks.onNavigate(StackLaborRoute.MedicineEdit(medicine.id, stackId, Origin.StackDetail)) },
                    onSignal = { callbacks.onNavigate(StackLaborRoute.Breakdown(stackId, medicine.id, Origin.StackDetail)) },
                    onToggle = { callbacks.onEvent(StackLaborEvent.ToggleMedicine(stackId, medicine.id)) },
                )
            }
        }
        item {
            EvaluationLink(evaluationMeta) {
                evaluationId?.let { callbacks.onNavigate(StackLaborRoute.Evaluation(it, Origin.StackDetail)) }
            }
        }
    }
}

/** Rückfrage, bevor ein weggewischtes Mittel wirklich aus dem Stack fliegt. */
@Composable
private fun DeleteMedicineDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = StackLaborTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.fillMaxWidth().depthShadow(RoundedCornerShape(18.dp), 28.dp, strength = 1.5f),
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(1.5.dp, metalRim(0.9f)),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Delete, null, Modifier.size(24.dp), tint = colors.red)
                    Spacer(Modifier.width(10.dp))
                    Text("Aus dem Stack entfernen?", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "„$name“ wird aus diesem Stack entfernt. Das Mittel selbst bleibt im Katalog erhalten.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Nein") }
                    PrimaryAction("Ja, entfernen", onConfirm, Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Legt hinter die Zeile ein rotes Löschfeld, das beim Wischen nach links zum Vorschein kommt.
 *
 * Gelöscht wird hier noch nichts — beim Loslassen wird nur gefragt; das schützt vor einem
 * Wisch, der aus Versehen passiert.
 */
@Composable
private fun SwipeToDeleteRow(
    offset: Float,
    width: Float,
    modifier: Modifier,
    onWidth: (Float) -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = StackLaborTheme.colors
    val shape = RoundedCornerShape(12.dp)
    // Ab hier ist das Wischen weit genug, damit die Rückfrage kommt.
    val ausgeloest = width > 1f && offset < -width * 0.32f
    val anteil = if (width > 1f) (-offset / width).coerceIn(0f, 1f) else 0f
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .onSizeChanged { onWidth(it.width.toFloat()) },
    ) {
        if (anteil > 0.01f) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .align(Alignment.TopCenter)
                    .clip(shape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(colors.red.copy(alpha = 0.35f * anteil), colors.red.copy(alpha = 0.55f + 0.45f * anteil)),
                        ),
                    )
                    .border(1.dp, colors.red.copy(alpha = 0.75f), shape)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (ausgeloest) "Loslassen zum Löschen" else "Löschen",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                )
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.Delete, null, Modifier.size(24.dp), tint = Color.White)
            }
        }
        Box(
            Modifier.graphicsLayer { translationX = offset }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount -> change.consume(); onDrag(amount) },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragCancel,
                    )
                },
        ) { content() }
    }
}

@Composable
private fun StackMedicineRow(
    medicine: MedicineUi,
    intakePosition: Int?,
    onOpen: () -> Unit,
    onSignal: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StackLaborTheme.colors
    val opacity = if (medicine.active) 1f else 0.38f
    Box(modifier.fillMaxWidth().height(64.dp)) {
        RaisedPanel(
            Modifier.fillMaxWidth().height(56.dp),
            elevation = if (medicine.active) 12.dp else 4.dp,
            rimAlpha = if (medicine.active) 0.7f else 0.3f,
            bevelStrength = if (medicine.active) 1f else 0.35f,
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    SignalBar(if (medicine.active) medicine.signal.color() else colors.disabled)
                    Column(Modifier.weight(1f).clickable(onClick = onOpen).padding(horizontal = 10.dp, vertical = 7.dp).alpha(opacity)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (intakePosition != null) {
                                Box(
                                    Modifier.depthShadow(CircleShape, 4.dp).size(20.dp).clip(CircleShape)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(colors.accent.copy(alpha = 0.22f), colors.accent.copy(alpha = 0.06f)),
                                            ),
                                        )
                                        .border(1.dp, metalRim(0.8f), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(intakePosition.toString(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.accent)
                                }
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                medicine.name,
                                Modifier.weight(1f),
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(8.dp))
                            if (medicine.warningReason.isNotEmpty()) {
                                Text("!", color = colors.red, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                            SolubilityLabel(medicine.solubility)
                        }
                        Text(medicine.dose, Modifier.fillMaxWidth(), style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconTouchButton(if (medicine.active) "${medicine.name} deaktivieren" else "${medicine.name} aktivieren", onToggle) {
                        WerftCheckbox(medicine.active)
                    }
                }
                Box(Modifier.width(44.dp).fillMaxHeight().clickable(onClick = onSignal))
            }
        }
    }
}

@Composable
private fun EvaluationLink(meta: String, onClick: () -> Unit) {
    val colors = StackLaborTheme.colors
    RaisedPanel(Modifier.fillMaxWidth().height(56.dp), rimAlpha = 0.95f, onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(24.dp), tint = colors.accent)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Auswertung im Vollbild", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EvaluateFooter(
    callbacks: StackLaborCallbacks,
    running: Boolean,
    stage: String,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().background(StackLaborTheme.colors.glass)) {
        // Solange die KI arbeitet, zeigt ein laufendes Band ueber dem Knopf, dass etwas passiert —
        // ohne das wirkte der Knopf wie tot.
        AnimatedVisibility(running) {
            WorkingBand(stage, animationsEnabled)
        }
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp, vertical = 4.dp)) {
            if (running) {
                EvaluationAction("Auswertung läuft — Abbrechen", { callbacks.onEvent(StackLaborEvent.CancelEvaluation) })
            } else {
                EvaluationAction("Diesen Stack auswerten", { callbacks.onEvent(StackLaborEvent.EvaluateStack) })
            }
        }
    }
}

/**
 * Das laufende Band ueber dem Auswerten-Knopf: ein wanderndes Goldlicht plus die Stufe, an der
 * die KI gerade arbeitet. Ohne diese Rueckmeldung sah es aus, als passiere beim Antippen nichts.
 */
@Composable
private fun WorkingBand(stage: String, animationsEnabled: Boolean) {
    val colors = StackLaborTheme.colors
    val fortschritt = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "arbeitsband")
        val wert by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Restart),
            label = "arbeitsbandLauf",
        )
        wert
    } else 0.5f
    Column(
        Modifier.fillMaxWidth()
            .drawBehind {
                drawLine(colors.border.copy(alpha = 0.6f), Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .padding(start = 12.dp, top = 8.dp, end = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp), tint = colors.accent)
            Spacer(Modifier.width(8.dp))
            Text(
                "Die KI wertet aus …",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accent,
            )
        }
        if (stage.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                stage,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.16f))
                .drawBehind {
                    // Ein Lichtstreifen wandert von links nach rechts durch — unbestimmte Dauer,
                    // deshalb kein Prozentbalken, der etwas verspricht was niemand weiss.
                    val breite = size.width * 0.34f
                    val start = (size.width + breite) * fortschritt - breite
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Transparent, colors.accent, Color.Transparent),
                            startX = start,
                            endX = start + breite,
                        ),
                    )
                },
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EvaluationAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GoldSurface(modifier.fillMaxWidth().height(44.dp), onClick = onClick) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, Modifier.size(24.dp), tint = Color(0xFF2A1B05))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2A1B05))
        }
    }
}

@Composable
private fun MenuItem(label: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(44.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
    }
}

/** Eingravierte Trennlinie zwischen zwei Menüeinträgen. */
@Composable
private fun MenuTrenner() {
    Box(
        Modifier.fillMaxWidth().height(1.dp)
            .background(StackLaborTheme.colors.border.copy(alpha = 0.7f)),
    )
}

/**
 * Das Drei-Punkte-Menü, das über den Bildschirmen aufklappt — gleiche Optik wie das im Stack.
 */
@Composable
private fun OverflowMenu(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val menuShape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .width(232.dp)
            .depthShadow(menuShape, 28.dp, strength = 1.5f)
            .clip(menuShape)
            .background(
                Brush.verticalGradient(
                    listOf(StackLaborTheme.colors.surface.lightenBy(0.20f), StackLaborTheme.colors.elevated),
                ),
            )
            .bevel()
            .border(1.5.dp, metalRim(0.95f), menuShape),
        content = content,
    )
}

@Composable
fun AllStacksScreen(state: StackLaborUiState, callbacks: StackLaborCallbacks) {
    WerftScreen(goldDark = true) {
        val borderColor = StackLaborTheme.colors.border
        Column(Modifier.fillMaxSize()) {
            GlassHeader("Alle Stacks zusammen", onBack = callbacks.onBack)
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
            ) {
                state.allStacksEvaluationId?.let { evaluationId ->
                    item {
                        Spacer(Modifier.height(10.dp))
                        EvaluationSummaryCard(
                            title = "Gesamtauswertung im Vollbild",
                            body = "Vollständige gemeinsame Auswertung der ausgewählten Stacks",
                            action = "Öffnen",
                        ) { callbacks.onNavigate(StackLaborRoute.Evaluation(evaluationId, Origin.AllStacks)) }
                    }
                }
                item { AllStacksSectionTitle("Tagesgesamtdosis") }
                item {
                    Column {
                        state.dailyDoses.forEach { row ->
                            DoseRow(row.name, row.dose, row.meta, doseSignal(row.name, state))
                        }
                    }
                }
                item { AllStacksSectionTitle("Konkurrenzen über Stacks hinweg") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.competitions.forEach { row ->
                            CompetitionCard(row.title, row.detail, competitionSignal(row.detail)) {
                                state.allStacksEvaluationId?.let { callbacks.onNavigate(StackLaborRoute.Evaluation(it, Origin.AllStacks)) }
                            }
                        }
                    }
                }
            }
            Box(
                Modifier.fillMaxWidth().height(52.dp).background(StackLaborTheme.colors.glass)
                    .drawBehind {
                        drawLine(borderColor.copy(alpha = 0.55f), Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                val running = state.allStacksEvaluationState == EvaluationState.Running
                EvaluationAction(
                    if (running) "Auswertung läuft — Abbrechen" else "Alles prüfen",
                    { callbacks.onEvent(if (running) StackLaborEvent.CancelEvaluation else StackLaborEvent.EvaluateAll) },
                )
            }
        }
    }
    if (state.allStacksSetupOpen) AllStacksSetupDialog(state, callbacks)
}

@Composable
private fun AllStacksSetupDialog(state: StackLaborUiState, callbacks: StackLaborCallbacks) {
    val colors = StackLaborTheme.colors
    val busy = state.allStacksInputState == GoalInputState.Transcribing || state.allStacksInputState == GoalInputState.Improving
    Dialog(
        onDismissRequest = { callbacks.onEvent(StackLaborEvent.DismissAllStacksSetup) },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .imePadding()
                .depthShadow(RoundedCornerShape(18.dp), 30.dp, strength = 1.6f),
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            border = BorderStroke(1.5.dp, metalRim(0.95f)),
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 8.dp)) {
                    Text("Gesamtauswertung vorbereiten", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Nur Stacks mit Häkchen werden gemeinsam ausgewertet.",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                    )
                }
                LazyColumn(
                    Modifier.weight(1f).padding(horizontal = 12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.stacks, key = { it.id }) { stack ->
                        val selected = stack.id in state.selectedAllStackIds
                        RaisedPanel(
                            Modifier.fillMaxWidth().height(58.dp),
                            elevation = if (selected) 12.dp else 5.dp,
                            onClick = { callbacks.onEvent(StackLaborEvent.ToggleAllStackSelection(stack.id)) },
                        ) {
                            Row(
                                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                WerftCheckbox(selected)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        stack.name,
                                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        stack.meta.ifBlank { "Keine Zeit- oder Einnahmeangabe" },
                                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Zusätzliche Informationen",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                    item { AllStacksAdditionalInfoEditor(state, callbacks) }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { callbacks.onEvent(StackLaborEvent.DismissAllStacksSetup) },
                        modifier = Modifier.weight(0.36f),
                    ) { Text("Abbrechen") }
                    PrimaryAction(
                        "${state.selectedAllStackIds.size} Stacks auswerten",
                        { if (!busy) callbacks.onEvent(StackLaborEvent.ConfirmEvaluateAll) },
                        Modifier.weight(0.64f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AllStacksAdditionalInfoEditor(state: StackLaborUiState, callbacks: StackLaborCallbacks) {
    val colors = StackLaborTheme.colors
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var focused by remember { mutableStateOf(false) }
    val busy = state.allStacksInputState == GoalInputState.Transcribing || state.allStacksInputState == GoalInputState.Improving
    LaunchedEffect(focused) {
        if (focused) {
            kotlinx.coroutines.delay(250L)
            bringIntoViewRequester.bringIntoView()
        }
    }
    Column(
        Modifier.fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .depthShadow(RoundedCornerShape(12.dp), 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.elevated)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
    ) {
        BasicTextField(
            value = state.allStacksAdditionalInfo,
            onValueChange = { callbacks.onEvent(StackLaborEvent.UpdateAllStacksAdditionalInfo(it)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 128.dp)
                .onFocusChanged { focused = it.isFocused }
                .padding(12.dp),
            textStyle = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = colors.textStrong),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize()) {
                    if (state.allStacksAdditionalInfo.isBlank()) {
                        Text(
                            "Zum Beispiel Einnahme rund um Sport, Mahlzeiten, Wasserflasche oder persönliche Beobachtungen …",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                        )
                    }
                    inner()
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (state.allStacksInputState) {
                    GoalInputState.Recording -> "Aufnahme läuft · zum Stoppen tippen"
                    GoalInputState.Transcribing -> "Groq transkribiert …"
                    GoalInputState.Improving -> "Codex verbessert …"
                    GoalInputState.Idle -> "Spracheingabe oder KI-Verbesserung"
                },
                Modifier.weight(1f),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = if (state.allStacksInputState == GoalInputState.Recording) colors.red else colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconTouchButton(
                if (state.allStacksInputState == GoalInputState.Recording) "Aufnahme stoppen" else "Zusatzinformationen einsprechen",
                { if (!busy) callbacks.onEvent(StackLaborEvent.ToggleAllStacksRecording) },
            ) {
                if (state.allStacksInputState == GoalInputState.Transcribing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.accent)
                } else {
                    Icon(
                        if (state.allStacksInputState == GoalInputState.Recording) Icons.Default.Stop else Icons.Default.Mic,
                        null,
                        Modifier.size(22.dp),
                        tint = if (state.allStacksInputState == GoalInputState.Recording) colors.red else if (busy) colors.disabled else colors.accent,
                    )
                }
            }
            IconTouchButton(
                "Zusatzinformationen mit Codex verbessern",
                { if (!busy && state.allStacksAdditionalInfo.isNotBlank()) callbacks.onEvent(StackLaborEvent.ImproveAllStacksAdditionalInfo) },
            ) {
                if (state.allStacksInputState == GoalInputState.Improving) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.accent)
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        Modifier.size(22.dp),
                        tint = if (!busy && state.allStacksAdditionalInfo.isNotBlank()) colors.accent else colors.disabled,
                    )
                }
            }
            if (state.canUndoAllStacksImprovement) {
                IconTouchButton("KI-Verbesserung rückgängig machen", { callbacks.onEvent(StackLaborEvent.UndoAllStacksImprovement) }) {
                    Icon(Icons.AutoMirrored.Filled.Undo, null, Modifier.size(22.dp), tint = colors.accent)
                }
            }
        }
    }
}

@Composable
private fun DoseRow(name: String, dose: String, meta: String, signal: SignalState? = null) {
    val borderColor = StackLaborTheme.colors.border
    Box(
        Modifier.fillMaxWidth().height(44.dp).drawBehind {
            drawLine(borderColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        },
    ) {
        if (signal != null && signal != SignalState.Green) {
            SignalBar(signal.color())
        }
        Row(Modifier.fillMaxSize().padding(start = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(meta, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(dose, Modifier.width(88.dp).padding(end = 1.dp), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), textAlign = androidx.compose.ui.text.style.TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun AllStacksSectionTitle(title: String) {
    Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.CenterStart) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun CompetitionCard(title: String, detail: String, signal: SignalState?, onClick: () -> Unit) {
    val colors = StackLaborTheme.colors
    RaisedPanel(
        Modifier.fillMaxWidth().height(72.dp),
        elevation = 16.dp,
        rimWidth = 1.5.dp,
        rimAlpha = 1f,
        onClick = onClick,
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            if (signal != null) SignalBar(signal.color()) else Spacer(Modifier.width(5.dp))
            Column(Modifier.weight(1f).padding(start = 13.dp, end = 4.dp), verticalArrangement = Arrangement.Center) {
                Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(detail, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = signal?.color() ?: colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.width(44.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.BarChart, null, Modifier.size(24.dp), tint = colors.textMuted)
            }
        }
    }
}

private fun doseSignal(name: String, state: StackLaborUiState): SignalState? = state.medicines
    .firstOrNull { medicine ->
        medicine.name.equals(name, ignoreCase = true) ||
            medicine.name.contains(name, ignoreCase = true) ||
            name.contains(medicine.name, ignoreCase = true)
    }
    ?.signal

private fun competitionSignal(detail: String): SignalState? = when {
    detail.contains("nicht bedient", ignoreCase = true) -> SignalState.Gray
    detail.contains(",") && detail.contains("stört", ignoreCase = true) -> SignalState.Red
    detail.contains("stört", ignoreCase = true) -> SignalState.Yellow
    else -> null
}

@Composable
private fun EvaluationSummaryCard(title: String, body: String, action: String, onClick: () -> Unit) {
    RaisedPanel(Modifier.fillMaxWidth().height(64.dp), onClick = onClick) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(body, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(action, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = StackLaborTheme.colors.accent)
        }
    }
}

@Composable
private fun EvaluationStateCard(
    state: EvaluationState,
    callbacks: StackLaborCallbacks,
    allStacks: Boolean = false,
    withoutGoals: Boolean = false,
) {
    val title: String
    val body: String
    val action: String?
    val event: StackLaborEvent?
    when (state) {
        EvaluationState.Valid -> {
            title = if (allStacks) "Gesamtprüfung aktuell" else "Auswertung aktuell"
            body = "Gespeicherter Auswertungsstand"
            action = null
            event = null
        }
        EvaluationState.Stale -> {
            title = "Stand veraltet"
            body = "Die zuletzt gültigen Ampeln bleiben sichtbar."
            action = "Neu auswerten"
            event = if (allStacks) StackLaborEvent.EvaluateAll else StackLaborEvent.EvaluateStack
        }
        EvaluationState.Running -> {
            title = if (allStacks) "Gesamtprüfung läuft" else "Auswertung läuft"
            body = when {
                allStacks -> "Addiere Tagesdosen und prüfe Konkurrenzen …"
                // Ohne Ziele gibt es nichts zu gewichten — dann sagt die Zeile auch das.
                withoutGoals -> "Prüfe Konkurrenzen, Reihenfolge und Verträglichkeit …"
                else -> "Prüfe Wechselwirkungen und gewichte die priorisierten Ziele …"
            }
            action = "Abbrechen"
            event = StackLaborEvent.CancelEvaluation
        }
        EvaluationState.Offline -> {
            title = "Ohne Netz"
            body = "Ampeln und Häkchen funktionieren weiter."
            action = null
            event = null
        }
        EvaluationState.Reauth -> {
            title = "Anmeldung abgelaufen."
            body = "Die zuletzt gültigen Ampeln bleiben sichtbar."
            action = "Neu anmelden"
            event = null
        }
        EvaluationState.Quota -> {
            title = "Kontingent erschöpft."
            body = "Wieder verfügbar in 2 Std. 18 Min."
            action = "Später erneut"
            event = StackLaborEvent.ScheduleRetry
        }
        EvaluationState.NetworkError -> {
            title = "Keine Verbindung."
            body = "Der vorherige Auswertungsstand bleibt erhalten."
            action = "Erneut versuchen"
            event = StackLaborEvent.RetryEvaluation
        }
    }
    RaisedPanel(Modifier.fillMaxWidth()) {
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Text(title, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Text(body, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = StackLaborTheme.colors.textMuted)
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            PrimaryAction(action, {
                if (state == EvaluationState.Reauth) callbacks.onNavigate(StackLaborRoute.CodexLogin(if (allStacks) Origin.AllStacks else Origin.StackDetail))
                else if (event != null) callbacks.onEvent(event)
            })
        }
    }
    }
}
