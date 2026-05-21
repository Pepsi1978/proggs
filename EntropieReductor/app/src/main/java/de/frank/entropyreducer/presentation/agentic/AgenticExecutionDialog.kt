package de.frank.entropyreducer.presentation.agentic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.StepType

/**
 * Modaler Dialog der einen laufenden agentic-Prompt-Run live anzeigt.
 *
 * Verwendung: aus PromptsScreen mit `executePromptId by remember { ... }`
 * heraus auf den Button-Klick:
 *   `executePromptId = promptId`
 *
 * Dann irgendwo (z.B. am Ende der Screen-Composable):
 *   `if (executePromptId != null) {
 *      AgenticExecutionDialog(
 *          promptId = executePromptId!!,
 *          onDismiss = { executePromptId = null }
 *      )
 *   }`
 *
 * Der Dialog laesst sich erst schliessen wenn der Run beendet ist — oder
 * der Nutzer drueckt explizit "Im Hintergrund laufen lassen".
 */
@Composable
fun AgenticExecutionDialog(
    promptId: String,
    userInputContext: String? = null,
    onDismiss: () -> Unit,
    vm: AgenticPromptViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val pendingConfirm by vm.pendingConfirmationFlow.collectAsStateWithLifecycle()

    // Beim ersten Compose den Run starten (nur einmal pro promptId)
    androidx.compose.runtime.LaunchedEffect(promptId) {
        if (state.promptId != promptId || (!state.isRunning && state.finalStatus == null)) {
            vm.runPrompt(promptId, userInputContext)
        }
    }

    // Confirm-Dialog (vor dem normalen Execution-Dialog) wenn etwas ansteht
    pendingConfirm?.let { req ->
        AlertDialog(
            onDismissRequest = { vm.rejectConfirmation("Dialog geschlossen") },
            title = {
                Text(
                    text = "Schreibender Tool-Aufruf bestaetigen",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Column {
                    Text(
                        text = "Werkzeug: ${req.tool.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = req.tool.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Argumente (von Gemini vorgeschlagen):",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = req.args.toString().take(800),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    req.previewText?.let { preview ->
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = preview,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { vm.approveConfirmation() }) { Text("Genehmigen") }
            },
            dismissButton = {
                TextButton(onClick = { vm.rejectConfirmation() }) { Text("Ablehnen") }
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = {
            if (!state.isRunning) onDismiss()
        },
        title = {
            Column {
                Text(
                    text = state.promptName ?: "Prompt-Ausfuehrung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (state.modelUsed != null) {
                    Text(
                        text = "Modell: ${state.modelUsed}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                StatusBar(state)
                Spacer(Modifier.size(8.dp))
                HorizontalDivider()
                Spacer(Modifier.size(8.dp))
                StepList(state)
                if (state.finalAnswer != null) {
                    Spacer(Modifier.size(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Antwort",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.finalAnswer.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (state.errorMessage != null && state.finalStatus != ExecutionStatus.SUCCESS) {
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "Fehler: ${state.errorMessage}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            if (state.isRunning) {
                OutlinedButton(onClick = { vm.cancelCurrentRun() }) {
                    Text("Abbrechen")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Schliessen")
                }
            }
        },
        dismissButton = {
            if (state.isRunning) {
                TextButton(onClick = { vm.dismissDialog(); onDismiss() }) {
                    Text("Im Hintergrund weiter")
                }
            } else null
        },
    )
}

@Composable
private fun StatusBar(state: AgenticRunUiState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.isRunning) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        }
        Text(
            text = state.statusLine ?: "Bereit.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (state.tokensInputTotal + state.tokensOutputTotal > 0) {
        Text(
            text = "Tokens: ${state.tokensInputTotal} in / ${state.tokensOutputTotal} out " +
                "(Σ ${state.tokensInputTotal + state.tokensOutputTotal})" +
                if (state.toolCallCount > 0) " · Tool-Calls: ${state.toolCallCount}" else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepList(state: AgenticRunUiState) {
    if (state.steps.isEmpty()) {
        Text(
            text = "Noch keine Schritte protokolliert.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    LazyColumn(
        modifier = Modifier.heightIn(max = 320.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(state.steps, key = { "${it.index}_${it.type}" }) { step ->
            StepCard(step)
        }
    }
}

@Composable
private fun StepCard(step: StepDisplay) {
    val (icon, accent) =
        when (step.type) {
            StepType.LLM_CALL -> "🧠" to MaterialTheme.colorScheme.primary
            StepType.TOOL_CALL -> "🔧" to MaterialTheme.colorScheme.tertiary
            StepType.USER_CONFIRM -> "✓" to MaterialTheme.colorScheme.secondary
            StepType.BLOCKED -> "⛔" to MaterialTheme.colorScheme.error
        }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(modifier = Modifier.padding(PaddingValues(horizontal = 10.dp, vertical = 6.dp))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$icon  Schritt ${step.index + 1}" +
                        if (step.toolName != null) "  ·  ${step.toolName}" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (step.message.isNotBlank()) {
                Text(
                    text = step.message,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
