package de.frank.stacklabor.werftstudio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.frank.stacklabor.werftstudio.ui.components.GlassHeader
import de.frank.stacklabor.werftstudio.ui.components.WerftScreen
import de.frank.stacklabor.werftstudio.ui.components.color
import de.frank.stacklabor.werftstudio.ui.model.GoalUi
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme

@Composable
fun ReorderGoalsScreen(stackId: String, state: StackLaborUiState, callbacks: StackLaborCallbacks) {
    val goals = remember(state.goals) { mutableStateListOf<GoalUi>().apply { addAll(state.goals.filter { it.selected }) } }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    WerftScreen(goldDark = true) {
        Column(Modifier.fillMaxSize()) {
            GlassHeader("Ziele ordnen — ${state.stackName}", onBack = callbacks.onBack)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(goals, key = { _, goal -> goal.id }) { index, goal ->
                    ReorderGoalRow(
                        goal = goal.copy(rank = index + 1),
                        expanded = expandedId == goal.id,
                        onClick = { if (goal.reason.isNotEmpty()) expandedId = if (expandedId == goal.id) null else goal.id },
                        onDrag = { callbacks.onEvent(StackLaborEvent.ReorderGoal(stackId, goal.id, index + 1)) },
                        modifier = Modifier
                            .graphicsLayer {
                                translationY = if (draggingId == goal.id) dragOffset else 0f
                                scaleX = if (draggingId == goal.id) 1.02f else 1f
                                scaleY = if (draggingId == goal.id) 1.02f else 1f
                            }
                            .pointerInput(goal.id, index) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { draggingId = goal.id; dragOffset = 0f },
                                    onDragCancel = { draggingId = null; dragOffset = 0f },
                                    onDragEnd = { draggingId = null; dragOffset = 0f },
                                ) { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    val direction = if (dragOffset > 56f) 1 else if (dragOffset < -56f) -1 else 0
                                    val target = (index + direction).coerceIn(0, goals.lastIndex)
                                    if (direction != 0 && target != index) {
                                        goals.removeAt(index).also { goals.add(target, it) }
                                        callbacks.onEvent(StackLaborEvent.ReorderGoal(stackId, goal.id, target + 1))
                                        dragOffset = 0f
                                    }
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReorderGoalRow(
    goal: GoalUi,
    expanded: Boolean,
    onClick: () -> Unit,
    onDrag: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StackLaborTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.4f)),
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().height(40.dp).clickable(enabled = goal.reason.isNotEmpty(), onClick = onClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(goal.signal.color()))
                Box(Modifier.width(34.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Text(
                        goal.rank.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
                Text(
                    goal.text,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    Modifier.width(44.dp).fillMaxHeight().clickable(onClick = onDrag),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.DragHandle, contentDescription = "${goal.text} verschieben", Modifier.size(20.dp), tint = colors.textMuted)
                }
            }
            AnimatedVisibility(
                visible = expanded && goal.reason.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Text(
                    goal.reason,
                    Modifier.fillMaxWidth().background(colors.elevated).padding(start = 37.dp, end = 12.dp, top = 5.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
