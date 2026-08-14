package de.frank.stacklabor.werftstudio.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import de.frank.stacklabor.werftstudio.ui.components.GlassHeader
import de.frank.stacklabor.werftstudio.ui.components.WerftScreen
import de.frank.stacklabor.werftstudio.ui.model.EvaluationState
import de.frank.stacklabor.werftstudio.ui.model.PlaybackState
import de.frank.stacklabor.werftstudio.ui.model.StackLaborCallbacks
import de.frank.stacklabor.werftstudio.ui.model.StackLaborEvent
import de.frank.stacklabor.werftstudio.ui.model.StackLaborUiState
import de.frank.stacklabor.werftstudio.ui.theme.StackLaborTheme

@Composable
fun EvaluationScreen(state: StackLaborUiState, callbacks: StackLaborCallbacks) {
    WerftScreen {
            val colors = StackLaborTheme.colors
            Column(Modifier.fillMaxSize()) {
                GlassHeader("Auswertung", onBack = callbacks.onBack)
                Box(
                    Modifier.fillMaxWidth().height(32.dp).background(colors.elevated)
                        .drawBehind {
                            drawLine(colors.border, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
                        }.padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        state.evaluationMeta,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = StackLaborTheme.colors.textMuted,
                    )
                }
                Box(Modifier.weight(1f)) {
                    if (state.evaluationState == EvaluationState.Running) {
                        Column(Modifier.fillMaxSize().padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 84.dp)) {
                            Text(
                                // Ohne Ziele gibt es nichts zu gewichten — dann sagt die Zeile auch das.
                                if (state.goals.none { it.selected }) {
                                    "Prüfe Konkurrenzen, Reihenfolge und Verträglichkeit …"
                                } else {
                                    "Prüfe Wechselwirkungen und gewichte die priorisierten Ziele …"
                                },
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = StackLaborTheme.colors.accent,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(state.streamedEvaluationText, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                        }
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 84.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            itemsIndexed(state.evaluationParagraphs) { index, paragraph ->
                                Text(
                                    paragraph,
                                    Modifier.fillMaxWidth().then(
                                        if (state.spokenParagraphIndex == index) {
                                            Modifier.drawBehind {
                                                drawRect(colors.accent.copy(alpha = 0.08f))
                                                drawRect(colors.accent, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                                            }.padding(start = 12.dp)
                                        } else Modifier
                                    ),
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                    PlaybackFooter(state.playbackState, callbacks, Modifier.align(Alignment.BottomCenter))
                }
            }
    }
}

@Composable
private fun PlaybackFooter(playback: PlaybackState, callbacks: StackLaborCallbacks, modifier: Modifier = Modifier) {
    val colors = StackLaborTheme.colors
    Row(
        modifier.fillMaxWidth().height(60.dp).background(colors.glass)
            .drawBehind {
                drawLine(colors.border, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx())
            }.padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackAction("Auswertung vorlesen", "Vorlesen", Icons.Default.PlayArrow, playback == PlaybackState.Playing, true, { callbacks.onEvent(StackLaborEvent.Play) })
        PlaybackAction(
            "Vorlesen pausieren oder fortsetzen",
            "Pause",
            Icons.Default.Pause,
            playback == PlaybackState.Paused,
            playback != PlaybackState.Ready,
            { callbacks.onEvent(if (playback == PlaybackState.Paused) StackLaborEvent.Play else StackLaborEvent.Pause) },
        )
        PlaybackAction("Vorlesen stoppen", "Stopp", Icons.Default.Stop, false, playback != PlaybackState.Ready, { callbacks.onEvent(StackLaborEvent.Stop) })
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            PlaybackLevel(playback, Modifier.padding(horizontal = 8.dp))
        }
    }
}

@Composable
private fun PlaybackLevel(playback: PlaybackState, modifier: Modifier = Modifier) {
    val active = playback == PlaybackState.Playing
    val motionEnabled = StackLaborTheme.motionEnabled
    val frames = listOf(
        listOf(8.dp, 20.dp, 12.dp),
        listOf(18.dp, 10.dp, 22.dp),
        listOf(12.dp, 24.dp, 8.dp),
        listOf(22.dp, 14.dp, 18.dp),
        listOf(10.dp, 18.dp, 24.dp),
        listOf(16.dp, 8.dp, 20.dp),
    )
    var frame by remember(active, motionEnabled) { mutableIntStateOf(0) }
    LaunchedEffect(active, motionEnabled) {
        if (active && motionEnabled) {
            delay(400)
            while (true) {
                frame = (frame + 1) % frames.size
                delay(90)
            }
        }
    }
    val color = if (active) StackLaborTheme.colors.accent else StackLaborTheme.colors.textMuted.copy(alpha = 0.5f)
    val heights = when {
        active && !motionEnabled -> listOf(12.dp, 12.dp, 12.dp)
        active -> frames[frame]
        else -> listOf(8.dp, 16.dp, 10.dp)
    }
    Row(
        modifier
            .width(28.dp)
            .height(24.dp)
            .semantics { contentDescription = if (active) "Vorlesepegel aktiv" else "Vorlesepegel inaktiv" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { barHeight ->
            Box(Modifier.width(3.dp).height(barHeight).background(color, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun PlaybackAction(
    description: String,
    label: String,
    imageVector: ImageVector,
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = when {
        !enabled -> StackLaborTheme.colors.disabled
        active -> StackLaborTheme.colors.accent
        else -> StackLaborTheme.colors.textMuted
    }
    Row(
        modifier.height(44.dp).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector, description, Modifier.size(24.dp), tint = color)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = color, maxLines = 1, overflow = TextOverflow.Clip)
    }
}
