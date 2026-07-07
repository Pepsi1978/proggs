package de.frank.entropyreducer.presentation.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Vollbild-Detail einer Loop-Vorlage (Frank-Wunsch 2026-06-01).
 *
 * Frank wollte die Loop-Aufgaben NICHT mehr ueber die kleinen Perlen in der Karte bearbeiten,
 * sondern durch Reinklicken in die Karte ein eigenes Bearbeitungs-Fenster oeffnen — genau wie bei
 * den Aufgaben. Hier kann er ALLES einstellen: Titel, Beschreibung, Wiederkehr-Intervall
 * (alle N Tage / KI entscheidet), Priorität, Ziel-Priorität und aktiv/pausiert — plus Löschen.
 *
 * Nutzt das bestehende RecurringTemplatesViewModel (alle Setter ziehen offene Instanzen im
 * Aufgaben-Reiter automatisch mit). Die Vorlage wird per templateId aus dem templates-Flow
 * herausgefiltert, sodass Aenderungen sofort reaktiv erscheinen.
 */
@Composable
fun LoopTemplateDetailScreen(
    templateId: String,
    onBack: () -> Unit,
    viewModel: RecurringTemplatesViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current
    val loopAccent = LocalCosmos.current.accentTasks
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

            // ---- Wiederkehr-Intervall ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Wiederkehr", loopAccent)
                    Text(
                        text =
                            "Wie oft soll diese Aufgabe vorgeschlagen werden? Bei einem festen " +
                                "Intervall erscheint sie erst wieder, wenn so viele Tage seit der " +
                                "letzten Erledigung vergangen sind. \"KI entscheidet\" überlässt der " +
                                "App die Planung.",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                    // Frank-Wunsch 2026-06-01: Dropdown mit voller Bandbreite (taeglich, jeder
                    // Tageswert lueckenlos, Wochen-/Monats-Schritte, jaehrlich) statt nur
                    // weniger Chips.
                    var intervalOpen by remember(t.id) { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { intervalOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(loopIntervalLabel(t.intervalDays), color = loopAccent)
                        }
                        DropdownMenu(
                            expanded = intervalOpen,
                            onDismissRequest = { intervalOpen = false },
                        ) {
                            loopIntervalChoices.forEach { (label, days) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setIntervalDays(t, days)
                                        intervalOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // ---- Prioritaet ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Priorität: ${t.priorityScore}", loopAccent)
                    val initialPrio = t.priorityScore.toFloat()
                    var prio by remember(t.id) { mutableStateOf(initialPrio) }
                    Slider(
                        value = prio.coerceIn(0f, 100f),
                        onValueChange = { prio = it },
                        onValueChangeFinished = { viewModel.setPriority(t, Math.round(prio)) },
                        valueRange = 0f..100f,
                        steps = 19,
                        colors =
                            SliderDefaults.colors(
                                thumbColor = loopAccent,
                                activeTrackColor = loopAccent,
                            ),
                    )
                }
            }

            // ---- Ziel-Priorität ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("Ziel-Priorität", loopAccent)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = t.targetBucket == null,
                            onClick = { viewModel.setTargetBucket(t, null) },
                            label = { Text("KI / nach Priorität") },
                            colors = chipColors(loopAccent),
                        )
                        loopBucketOptions.forEach { b ->
                            FilterChip(
                                selected = t.targetBucket == b,
                                onClick = { viewModel.setTargetBucket(t, b) },
                                label = { Text(loopBucketLabel(b)) },
                                colors = chipColors(loopAccent),
                            )
                        }
                    }
                }
            }

            // ---- Aktiv / Pausiert ----
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel(if (t.isActive) "Aktiv" else "Pausiert", loopAccent)
                        Text(
                            text =
                                if (t.isActive) "Erzeugt automatisch neue Aufgaben."
                                else "Erzeugt keine neuen Aufgaben.",
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                        )
                    }
                    Switch(
                        checked = t.isActive,
                        onCheckedChange = { viewModel.toggleActive(t) },
                        colors = SwitchDefaults.colors(checkedTrackColor = loopAccent),
                    )
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

@Composable
private fun chipColors(accent: Color) =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = accent.copy(alpha = 0.25f),
        selectedLabelColor = LocalCosmos.current.textPrimary,
    )
