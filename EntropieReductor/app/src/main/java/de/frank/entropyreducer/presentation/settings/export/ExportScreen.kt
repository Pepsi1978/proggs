package de.frank.entropyreducer.presentation.settings.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.ExportViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun ExportScreen(onBack: () -> Unit, vm: ExportViewModel = hiltViewModel()) {
    val cosmos = LocalCosmos.current
    var confirmDeleteEntries by remember { mutableStateOf(false) }
    var confirmDeleteMemories by remember { mutableStateOf(false) }

    CosmosScaffold(
        title = "Datenexport / Datenschutz",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Datenexport", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { /* Stufe 4 — JSON-Export */ },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Alle Daten als JSON exportieren (Stufe 4)") }
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Loeschen", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { confirmDeleteEntries = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.Critical, contentColor = Color.White),
                    ) { Text("Alle Eintraege loeschen") }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { confirmDeleteMemories = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.Critical, contentColor = Color.White),
                    ) { Text("Alle Memory-Eintraege loeschen") }
                }
            }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Alle Daten werden ausschliesslich lokal auf deinem Geraet gespeichert. Inhalte deiner Eintraege werden an Groq, Google (Gemini, TTS), Google Calendar und Whoop gesendet, soweit fuer die jeweilige Funktion noetig.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }

    if (confirmDeleteEntries) {
        ConfirmDialog(
            title = "Alle Eintraege loeschen?",
            text = "Diese Aktion kann nicht rueckgaengig gemacht werden.",
            onConfirm = { vm.deleteAllEntries(); confirmDeleteEntries = false },
            onCancel = { confirmDeleteEntries = false },
        )
    }
    if (confirmDeleteMemories) {
        ConfirmDialog(
            title = "Alle Memory-Eintraege loeschen?",
            text = "Diese Aktion kann nicht rueckgaengig gemacht werden.",
            onConfirm = { vm.deleteAllMemories(); confirmDeleteMemories = false },
            onCancel = { confirmDeleteMemories = false },
        )
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.Critical, contentColor = Color.White),
            ) { Text("Loeschen") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Abbrechen") } },
        title = { Text(title) },
        text = { Text(text) },
    )
}
