package de.frank.entropyreducer.presentation.settings.prompts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.settings.PromptsViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

@Composable
fun PromptsScreen(onBack: () -> Unit, vm: PromptsViewModel = hiltViewModel()) {
    val prompts by vm.prompts.collectAsState()
    val cosmos = LocalCosmos.current
    var editing by remember { mutableStateOf<SavedPromptEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    CosmosScaffold(
        title = "Eigene Prompts",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurueck", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(prompts, key = { it.id }) { p ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary,
                                    modifier = Modifier.weight(1f))
                                Switch(
                                    checked = p.isActive,
                                    onCheckedChange = { vm.toggle(p) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CosmosColors.AccentPrimary,
                                    ),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                p.content.take(140) + if (p.content.length > 140) "…" else "",
                                style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { editing = p }) {
                                    Icon(Icons.Outlined.Edit, "Bearbeiten", tint = CosmosColors.AccentPrimary)
                                }
                                IconButton(onClick = { vm.delete(p) }) {
                                    Icon(Icons.Outlined.DeleteOutline, "Loeschen", tint = CosmosColors.Critical)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }

            FloatingActionButton(
                onClick = { creating = true },
                containerColor = CosmosColors.AccentPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Outlined.Add, "Neuer Prompt", tint = Color.Black)
            }
        }
    }

    editing?.let { p ->
        EditDialog(
            initialName = p.name,
            initialContent = p.content,
            onSave = { name, content -> vm.save(p.copy(name = name, content = content, updatedAt = System.currentTimeMillis())); editing = null },
            onCancel = { editing = null },
        )
    }
    if (creating) {
        EditDialog(
            initialName = "",
            initialContent = "",
            onSave = { n, c -> vm.create(n, c); creating = false },
            onCancel = { creating = false },
        )
    }
}

@Composable
private fun EditDialog(initialName: String, initialContent: String, onSave: (String, String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var content by remember { mutableStateOf(initialContent) }
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && content.isNotBlank()) onSave(name.trim(), content.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = CosmosColors.AccentPrimary, contentColor = Color.Black),
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Abbrechen") } },
        title = { Text("Prompt bearbeiten") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Inhalt") },
                    modifier = Modifier.height(180.dp).fillMaxWidth(),
                )
            }
        },
    )
}
