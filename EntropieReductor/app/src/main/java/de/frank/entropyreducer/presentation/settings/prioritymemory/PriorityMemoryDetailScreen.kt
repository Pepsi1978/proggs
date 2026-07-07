package de.frank.entropyreducer.presentation.settings.prioritymemory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.priorityRampColor
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlin.math.roundToInt

/**
 * Prioritaets-Gedaechtnis — Detail-Editor (Frank-Wunsch 2026-06-19).
 * Titel und vollstaendige Beschreibung per Tastatur editierbar, Prio-Schieberegler, Speichern + Loeschen.
 */
@Composable
fun PriorityMemoryDetailScreen(
    onBack: () -> Unit,
    vm: PriorityMemoryDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val entity = state

    CosmosScaffold(
        title = "Eintrag bearbeiten",
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
        if (entity == null) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@CosmosScaffold
        }

        var title by remember(entity.id) { mutableStateOf(entity.title) }
        var description by remember(entity.id) { mutableStateOf(entity.description) }
        var prio by remember(entity.id) { mutableStateOf(entity.priority.toFloat()) }
        val ramp = priorityRampColor(prio.toDouble())

        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titel") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Beschreibung") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Priorität ${prio.roundToInt()}", color = cosmos.textSecondary)
            Slider(
                value = prio.coerceIn(0f, 100f),
                onValueChange = { prio = it },
                valueRange = 0f..100f,
                steps = 19,
                colors = SliderDefaults.colors(thumbColor = ramp, activeTrackColor = ramp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { vm.save(title, description, prio.toDouble(), onBack) },
                    modifier = Modifier.weight(1f),
                ) { Text("Speichern") }
                OutlinedButton(
                    onClick = { vm.delete(onBack) },
                    modifier = Modifier.weight(1f),
                ) { Text("Löschen", color = cosmos.crit) }
            }
        }
    }
}
