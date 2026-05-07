package de.frank.entropyreducer.presentation.settings.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.domain.model.MemorySource
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.MemoryViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun MemoryScreen(onBack: () -> Unit, vm: MemoryViewModel = hiltViewModel()) {
    val memories by vm.memories.collectAsState()
    val cosmos = LocalCosmos.current
    var adding by remember { mutableStateOf(false) }

    CosmosScaffold(
        title = "Gedaechtnis",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 3 Buttons oben
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = { adding = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Manuell hinzufuegen", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = { /* Stufe 4 — Vorschlaege */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.RateReview, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("KI-Vorschlaege pruefen", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = { /* Stufe 4 — Profil destillieren */ }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Aus Profil neu generieren", style = MaterialTheme.typography.labelSmall)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (memories.isEmpty()) {
                    item {
                        Text(
                            "Noch keine Gedaechtnis-Eintraege. Manuell hinzufuegen oder aus dem Profil generieren.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cosmos.textSecondary,
                            modifier = Modifier.padding(top = 24.dp),
                        )
                    }
                }
                items(memories, key = { it.id }) { m ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(m.content, style = MaterialTheme.typography.bodyMedium, color = cosmos.textPrimary)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SourcePill(m.source)
                                if (m.source == MemorySource.KI_VORSCHLAG) {
                                    Spacer(Modifier.width(8.dp))
                                    ConfidenceBar(m.confidence)
                                }
                                Spacer(Modifier.weight(1f))
                                Switch(
                                    checked = m.isActive,
                                    onCheckedChange = { vm.toggle(m) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = CosmosColors.AccentPrimary),
                                )
                                IconButton(onClick = { vm.delete(m) }) {
                                    Icon(Icons.Outlined.DeleteOutline, "Loeschen", tint = CosmosColors.Critical)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (adding) {
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adding = false },
            confirmButton = {
                Button(
                    onClick = { if (content.isNotBlank()) { vm.add(content.trim()); adding = false } },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.AccentPrimary, contentColor = Color.Black),
                ) { Text("Speichern") }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("Abbrechen") } },
            title = { Text("Memory hinzufuegen") },
            text = {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Was soll die KI ueber dich wissen?") },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
            },
        )
    }
}

@Composable
private fun SourcePill(source: MemorySource) {
    val (label, color) = when (source) {
        MemorySource.MANUELL -> "MANUELL" to CosmosColors.AccentPrimary
        MemorySource.KI_VORSCHLAG -> "KI-VORSCHLAG" to CosmosColors.AccentSecondary
        MemorySource.AUS_PROFIL -> "AUS PROFIL" to CosmosColors.Success
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun ConfidenceBar(percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Vertrauen", style = MaterialTheme.typography.labelSmall, color = LocalCosmos.current.textSecondary)
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(width = 64.dp, height = 6.dp)
                .clip(RoundedCornerShape(50))
                .background(LocalCosmos.current.glassBg),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CosmosColors.AccentPrimary),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text("$percent %", style = MaterialTheme.typography.labelSmall, color = LocalCosmos.current.textSecondary)
    }
}
