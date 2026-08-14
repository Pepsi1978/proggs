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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        Column(Modifier.fillMaxSize()) {
            GlassHeader("Auswertung", onBack = callbacks.onBack)
            Text(
                state.evaluationMeta,
                Modifier.fillMaxWidth().background(StackLaborTheme.colors.elevated).padding(horizontal = 12.dp, vertical = 8.dp),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = StackLaborTheme.colors.textMuted,
            )
            if (state.evaluationState == EvaluationState.Running) {
                Column(Modifier.weight(1f).padding(16.dp)) {
                    Text("Prüfe Wechselwirkungen und gewichte die priorisierten Ziele …", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = StackLaborTheme.colors.accent)
                    Spacer(Modifier.height(12.dp))
                    Text(state.streamedEvaluationText, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(state.evaluationParagraphs) { index, paragraph ->
                        Text(
                            paragraph,
                            Modifier.fillMaxWidth().then(
                                if (state.spokenParagraphIndex == index) {
                                    Modifier.background(StackLaborTheme.colors.elevated, RoundedCornerShape(12.dp)).padding(10.dp)
                                } else Modifier
                            ),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            PlaybackFooter(state.playbackState, callbacks)
        }
    }
}

@Composable
private fun PlaybackFooter(playback: PlaybackState, callbacks: StackLaborCallbacks) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).background(StackLaborTheme.colors.glass).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaybackLevel(playback, Modifier.padding(horizontal = 8.dp))
        PlaybackAction("Auswertung vorlesen", "Vorlesen", Icons.Default.PlayArrow, playback == PlaybackState.Playing, { callbacks.onEvent(StackLaborEvent.Play) }, Modifier.weight(1f))
        PlaybackAction("Vorlesen pausieren", "Pause", Icons.Default.Pause, playback == PlaybackState.Paused, { callbacks.onEvent(StackLaborEvent.Pause) }, Modifier.weight(1f))
        PlaybackAction("Vorlesen stoppen", "Stopp", Icons.Default.Stop, false, { callbacks.onEvent(StackLaborEvent.Stop) }, Modifier.weight(1f))
    }
}

@Composable
private fun PlaybackLevel(playback: PlaybackState, modifier: Modifier = Modifier) {
    val active = playback == PlaybackState.Playing
    val color = if (active) StackLaborTheme.colors.accent else StackLaborTheme.colors.textMuted.copy(alpha = 0.5f)
    Row(
        modifier
            .width(20.dp)
            .height(24.dp)
            .semantics { contentDescription = if (active) "Vorlesepegel aktiv" else "Vorlesepegel inaktiv" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(8.dp, 15.dp, 22.dp).forEach { barHeight ->
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (active) StackLaborTheme.colors.accent else StackLaborTheme.colors.textMuted
    Row(
        modifier.height(44.dp).clickable(onClick = onClick).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector, description, Modifier.size(20.dp), tint = color)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1, overflow = TextOverflow.Clip)
    }
}
