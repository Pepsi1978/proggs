package de.frank.entropyreducer.presentation.agentic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.data.local.entities.PromptTriggerEntity
import de.frank.entropyreducer.domain.agentic.trigger.SimpleCronParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog fuer Auto-Trigger-Konfiguration pro Prompt (Frank-Wunsch 2026-05-21).
 *
 * Funktioniert nur fuer CRON-Trigger (zeitgesteuert). Event- und Chain-Trigger
 * folgen in Etappe 12.
 *
 * Frank gibt einen Cron-Ausdruck im einfachen Format ein:
 *  - "daily 19:00"           — jeden Tag um 19 Uhr
 *  - "weekly SUN 19:00"      — jeden Sonntag um 19 Uhr
 *  - "hourly :05"            — jede Stunde zur 5. Minute
 *
 * Bestehende Trigger werden gelistet mit Active-Switch und Loesch-Knopf.
 */
@Composable
fun TriggerConfigDialog(
    promptName: String,
    triggersFlow: kotlinx.coroutines.flow.Flow<List<PromptTriggerEntity>>,
    onAddCron: (cronExpression: String) -> Unit,
    onDelete: (PromptTriggerEntity) -> Unit,
    onToggleActive: (PromptTriggerEntity, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val triggers by triggersFlow.collectAsState(initial = emptyList())
    var newCron by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Auto-Trigger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Prompt: $promptName",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column {
                Text(
                    text =
                        "Trigger laufen alle 15 Minuten im Hintergrund. " +
                            "Format-Beispiele: \"daily 19:00\", \"weekly SUN 19:00\", " +
                            "\"hourly :05\". Schreib-Tools ohne Trust-Modus werden " +
                            "blockiert (kein UI-Dialog im Hintergrund).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Bestehende Trigger (${triggers.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                if (triggers.isEmpty()) {
                    Text(
                        text = "Noch keine Trigger.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(triggers, key = { it.id }) { trigger ->
                            TriggerRow(
                                trigger = trigger,
                                onToggleActive = { onToggleActive(trigger, it) },
                                onDelete = { onDelete(trigger) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Neuen CRON-Trigger anlegen",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = newCron,
                    onValueChange = { newCron = it },
                    label = { Text("Cron-Ausdruck") },
                    placeholder = { Text("z.B. daily 19:00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val preview =
                    if (newCron.isBlank()) null else SimpleCronParser.nextFireAt(newCron)
                if (newCron.isNotBlank()) {
                    Text(
                        text =
                            if (preview != null)
                                "Naechster Lauf: ${formatDate(preview)} " +
                                    "(${SimpleCronParser.describe(newCron)})"
                            else "Format nicht erkannt — bitte daily / weekly / hourly verwenden.",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (preview != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (preview != null) {
                            onAddCron(newCron.trim())
                            newCron = ""
                        }
                    },
                    enabled = newCron.isNotBlank() && preview != null,
                ) { Text("Trigger anlegen") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}

@Composable
private fun TriggerRow(
    trigger: PromptTriggerEntity,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trigger.cronExpression ?: "(kein cron-Ausdruck)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                trigger.cronExpression?.let { cron ->
                    Text(
                        text = SimpleCronParser.describe(cron),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trigger.nextScheduledAt?.let { next ->
                    Text(
                        text = "Naechster Lauf: ${formatDate(next)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trigger.lastRunAt?.let { last ->
                    Text(
                        text = "Letzter Lauf: ${formatDate(last)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = trigger.isActive, onCheckedChange = onToggleActive)
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Trigger loeschen",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private val DATE_FORMAT = SimpleDateFormat("dd.MM. HH:mm", Locale.GERMAN)

private fun formatDate(millis: Long): String = DATE_FORMAT.format(Date(millis))
