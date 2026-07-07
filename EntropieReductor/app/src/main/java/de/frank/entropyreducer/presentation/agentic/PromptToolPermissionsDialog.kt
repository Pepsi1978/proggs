package de.frank.entropyreducer.presentation.agentic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.domain.agentic.AgenticTool

/**
 * UI-Zustand fuer eine einzelne Tool-Permission im Editor. Wird vom
 * PromptsViewModel exponiert als Map<toolName, ToolPermissionState>.
 */
data class ToolPermissionState(
    val granted: Boolean = false,
    val trustMode: Boolean = false,
)

/**
 * Dialog zur Verwaltung der Write-Tool-Permissions eines Prompts
 * (Frank-Wunsch 2026-05-21).
 *
 * Frank sieht eine Liste aller Write-Tools mit zwei Checkboxen pro Tool:
 *  - Freigeschaltet: ob das Tool ueberhaupt aufgerufen werden darf
 *  - Trust: ob es ohne Confirm-Dialog ausgefuehrt wird (zusaetzlich zur
 *    promptweiten trustModeDefault-Einstellung)
 *
 * Warnhinweis am Kopf: Trust-Modus + freigeschaltete Loesch-/Profil-Tools
 * sind eine gefaehrliche Kombination. Frank wird darauf hingewiesen.
 *
 * Verwendung:
 *   permissionEditFor?.let { p ->
 *       PromptToolPermissionsDialog(
 *           promptName = p.name,
 *           writeTools = vm.writeTools,
 *           permissionsFlow = vm.permissionsForPrompt(p.id),
 *           onSet = { tool, granted, trust ->
 *               vm.setToolPermission(p.id, tool, granted, trust)
 *           },
 *           onDismiss = { permissionEditFor = null },
 *       )
 *   }
 */
@Composable
fun PromptToolPermissionsDialog(
    promptName: String,
    writeTools: List<AgenticTool>,
    permissionsFlow: kotlinx.coroutines.flow.Flow<Map<String, ToolPermissionState>>,
    onSet: (toolName: String, granted: Boolean, trustMode: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val permissions by permissionsFlow.collectAsStateWithLifecycle(initialValue = emptyMap())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Tool-Berechtigungen",
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
                        "Read-Tools sind immer aktiv. Hier konfigurierst du nur welche " +
                            "schreibenden Tools (Aufgaben anlegen, Memory aendern usw.) " +
                            "dieser Prompt aufrufen darf.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text =
                            "ACHTUNG: Trust-Modus + freigeschaltete Loesch- oder " +
                                "Profil-Tools wirken sofort und ohne Bestaetigungs-Dialog. " +
                                "Bitte ueberlege bewusst pro Tool ob Trust sicher ist.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                if (writeTools.isEmpty()) {
                    Text(
                        text = "Keine Write-Tools registriert.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(writeTools, key = { it.name }) { tool ->
                            val state = permissions[tool.name] ?: ToolPermissionState()
                            ToolPermissionRow(
                                tool = tool,
                                state = state,
                                onChange = { granted, trust ->
                                    onSet(tool.name, granted, trust)
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )
}

@Composable
private fun ToolPermissionRow(
    tool: AgenticTool,
    state: ToolPermissionState,
    onChange: (granted: Boolean, trustMode: Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = tool.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = tool.description.take(180) +
                    if (tool.description.length > 180) "..." else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Checkbox(
                        checked = state.granted,
                        onCheckedChange = { newGranted ->
                            // Wenn Permission entzogen wird, auch Trust deaktivieren
                            val newTrust = if (!newGranted) false else state.trustMode
                            onChange(newGranted, newTrust)
                        },
                    )
                    Text(
                        text = "Freigeschaltet",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.trustMode,
                        enabled = state.granted,
                        onCheckedChange = { newTrust -> onChange(state.granted, newTrust) },
                    )
                    Text(
                        text = "Trust",
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (state.granted) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
    Box(modifier = Modifier.height(2.dp))
}
