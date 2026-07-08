package de.frank.entropyreducer.presentation.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Vollbild-Detail einer Loop-Vorlage (Frank-Wunsch 2026-06-01).
 *
 * Frank wollte die Loop-Aufgaben NICHT mehr ueber die kleinen Perlen in der Karte bearbeiten,
 * sondern durch Reinklicken in die Karte ein eigenes Bearbeitungs-Fenster oeffnen — genau wie bei
 * den Aufgaben. Hier kann er Titel und Beschreibung bearbeiten, die Vorlage manuell in die
 * Aufgabenliste kopieren und sie löschen.
 *
 * Nutzt das bestehende RecurringTemplatesViewModel. Die Vorlage wird per templateId aus dem
 * templates-Flow herausgefiltert, sodass Aenderungen sofort reaktiv erscheinen.
 */
@Composable
fun LoopTemplateDetailScreen(
    templateId: String,
    onBack: () -> Unit,
    viewModel: RecurringTemplatesViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val loopAccent = LocalCosmos.current.accentTasks
    val context = LocalContext.current
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val template = templates.firstOrNull { it.id == templateId }

    // Vorlage geloescht (oder nie existiert) → zurueck zur Liste.
    LaunchedEffect(template, templates.size) {
        if (templates.isNotEmpty() && template == null) onBack()
    }

    CosmosScaffold(
        title = "Loop-Aufgabe",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        val t = template ?: return@CosmosScaffold
        // Lokale Editier-States fuer Titel + Beschreibung (gespeichert per Button).
        var title by remember(t.id) { mutableStateOf(t.title) }
        var description by remember(t.id) { mutableStateOf(t.description ?: "") }
        val titleChanged = title.trim().isNotBlank() && title.trim() != t.title
        val descChanged = description.trim() != (t.description ?: "").trim()

        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ---- Titel + Beschreibung ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("Titel", loopAccent)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SectionLabel("Beschreibung", loopAccent)
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                    )
                    if (titleChanged || descChanged) {
                        OutlinedButton(
                            onClick = {
                                if (titleChanged) viewModel.setTitle(t, title)
                                if (descChanged) viewModel.setDescription(t, description)
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text("Speichern", color = loopAccent)
                        }
                    }
                }
            }

            // ---- Manuell hinzufügen ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Aufgabe hinzufügen", loopAccent)
                    Text(
                        text = "Diese Loop-Vorlage wird erst dann zur Aufgabe, wenn du sie manuell hinzufügst.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                    var addOpen by remember(t.id) { mutableStateOf(false) }
                    Box {
                        Button(
                            onClick = { addOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                            ),
                        ) {
                            Text("Hinzufügen")
                        }
                        DropdownMenu(expanded = addOpen, onDismissRequest = { addOpen = false }) {
                            loopAddChoices.forEach { (label, bucket) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.addToTasks(t, bucket)
                                        android.widget.Toast.makeText(
                                            context,
                                            "Aufgabe hinzugefügt",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                        addOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ---- Loeschen ----
            OutlinedButton(
                onClick = {
                    viewModel.delete(t)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = LocalCosmos.current.crit)
                Spacer(Modifier.height(0.dp))
                Text("  Loop-Aufgabe löschen", color = LocalCosmos.current.crit)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, accent: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = accent,
        fontWeight = FontWeight.SemiBold,
    )
}
