package de.frank.entropyreducer.presentation.settings.prioritymemory

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.entities.PriorityMemoryEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.priorityRampColor
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlin.math.roundToInt

/**
 * Prioritaets-Gedaechtnis — Liste (Frank-Wunsch 2026-06-19).
 * An/Aus-Schalter, einstellbares Limit, blinkende Warnung bei Limit-Erreichung und schlanke Karten
 * (Titel + Beschreibung + Prio-Schieberegler, gleiche Optik wie eine Aufgabe). Tippen oeffnet das Detail.
 */
@Composable
fun PriorityMemoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    vm: PriorityMemoryListViewModel = hiltViewModel(),
) {
    val memories by vm.memories.collectAsStateWithLifecycle()
    val count by vm.count.collectAsStateWithLifecycle()
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val limit by vm.limit.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current

    CosmosScaffold(
        title = "Prioritäts-Gedächtnis",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Zurück",
                    tint = cosmos.textPrimary,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { EnabledRow(enabled = enabled, onChange = vm::setEnabled) }
            item { LimitRow(limit = limit, onSave = vm::setLimit) }
            if (count >= limit) {
                item { LimitWarningBanner(count = count, limit = limit) }
            }
            items(memories, key = { it.id }) { m ->
                PriorityMemoryCard(
                    memory = m,
                    onClick = { onOpenDetail(m.id) },
                    onSetPriority = { vm.setPriority(m.id, it) },
                )
            }
        }
    }
}

@Composable
private fun EnabledRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    val cosmos = LocalCosmos.current
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Prioritäts-Gedächtnis nutzen",
                modifier = Modifier.weight(1f),
                color = cosmos.textPrimary,
            )
            Switch(checked = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun LimitRow(limit: Int, onSave: (Int) -> Unit) {
    val cosmos = LocalCosmos.current
    var text by remember(limit) { mutableStateOf(limit.toString()) }
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Von der KI berücksichtigte Einträge", color = cosmos.textPrimary)
                Text(
                    "Älteste darüber werden beim Abgleich ignoriert (bleiben aber gespeichert).",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { s -> text = s.filter { it.isDigit() }.take(4) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(96.dp),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { text.toIntOrNull()?.let(onSave) }) { Text("Speichern") }
        }
    }
}

@Composable
private fun LimitWarningBanner(count: Int, limit: Int) {
    val cosmos = LocalCosmos.current
    val transition = rememberInfiniteTransition(label = "limitWarn")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    GlassCard(Modifier.fillMaxWidth().graphicsLayer { this.alpha = alpha }) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = cosmos.crit)
            Spacer(Modifier.width(12.dp))
            Text(
                "Limit erreicht ($count von $limit). Ältere Einträge werden von der KI nicht mehr " +
                    "berücksichtigt. Lösche Einträge oder erhöhe das Limit.",
                color = cosmos.crit,
            )
        }
    }
}

@Composable
private fun PriorityMemoryCard(
    memory: PriorityMemoryEntity,
    onClick: () -> Unit,
    onSetPriority: (Double) -> Unit,
) {
    val cosmos = LocalCosmos.current
    var sliderActive by remember(memory.id) { mutableStateOf(false) }
    var liveSlider by remember(memory.id) { mutableStateOf<Float?>(null) }
    val effective = liveSlider?.toDouble() ?: memory.priority
    val ramp = priorityRampColor(effective)
    val brush = remember(ramp) { Brush.horizontalGradient(listOf(ramp.copy(alpha = 0.20f), ramp)) }

    GlassCard(Modifier.fillMaxWidth().clickable { onClick() }, tintBrush = brush) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                memory.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cosmos.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                memory.description,
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val label =
                    liveSlider?.let { "Priorität ${it.roundToInt()}" }
                        ?: "Priorität ${memory.priority.toInt()}"
                Text(label, modifier = Modifier.weight(1f), color = cosmos.textSecondary)
                TextButton(onClick = { sliderActive = !sliderActive }) {
                    Text(if (sliderActive) "Schließen" else "Ändern")
                }
            }
            if (sliderActive) {
                Slider(
                    value = (liveSlider ?: memory.priority.toFloat()).coerceIn(0f, 100f),
                    onValueChange = { liveSlider = it },
                    onValueChangeFinished = {
                        liveSlider?.let { onSetPriority(it.toDouble()) }
                        sliderActive = false
                    },
                    valueRange = 0f..100f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = ramp, activeTrackColor = ramp),
                )
            }
        }
    }
}
