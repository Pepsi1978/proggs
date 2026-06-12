package de.frank.entropyreducer.presentation.agentic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.data.local.entities.PromptExecutionEntity
import de.frank.entropyreducer.data.local.entities.PromptExecutionStepEntity
import de.frank.entropyreducer.domain.model.ExecutionStatus
import de.frank.entropyreducer.domain.model.StepType
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * History-Screen fuer alle agentic-Prompt-Ausfuehrungen (Frank-Wunsch 2026-05-21).
 *
 * Liste der juengsten 200 Ausfuehrungen mit:
 *  - Status-Farbe (gruen=Success, rot=Failed, gelb=Blocked, grau=Cancelled)
 *  - Prompt-Name + Zeitstempel
 *  - Token + Tool-Call-Bilanz
 *
 * Tap auf Eintrag → Detail-Dialog mit allen Steps des Runs.
 *
 * Optional: Filter auf einen bestimmten Prompt via `initialPromptId`.
 */
@Composable
fun AgenticHistoryScreen(
    onBack: () -> Unit,
    initialPromptId: String? = null,
    vm: AgenticHistoryViewModel = hiltViewModel(),
) {
    val executions by vm.executions.collectAsStateWithLifecycle()
    val selectedExecutionId by vm.selectedExecutionId.collectAsStateWithLifecycle()
    val filterStatus by vm.filterStatus.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    var showPruneConfirm by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(initialPromptId) {
        vm.setFilterPromptId(initialPromptId)
    }

    CosmosScaffold(
        title = if (initialPromptId == null) "Verlauf — alle Prompts" else "Verlauf",
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    "Zurueck",
                    tint = cosmos.textPrimary,
                )
            }
        },
        actions = {
            IconButton(onClick = { showPruneConfirm = true }) {
                Icon(
                    Icons.Outlined.DeleteSweep,
                    contentDescription = "Alte Eintraege loeschen",
                    tint = cosmos.textPrimary,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Status-Filter-Chips (Etappe 15)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        FilterChip(
                            selected = filterStatus == null,
                            onClick = { vm.setFilterStatus(null) },
                            label = { Text("Alle") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterStatus == ExecutionStatus.SUCCESS,
                            onClick = {
                                vm.setFilterStatus(
                                    if (filterStatus == ExecutionStatus.SUCCESS) null
                                    else ExecutionStatus.SUCCESS
                                )
                            },
                            label = { Text("Erfolge") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterStatus == ExecutionStatus.FAILED,
                            onClick = {
                                vm.setFilterStatus(
                                    if (filterStatus == ExecutionStatus.FAILED) null
                                    else ExecutionStatus.FAILED
                                )
                            },
                            label = { Text("Fehler") },
                        )
                    }
                    item {
                        FilterChip(
                            selected =
                                filterStatus == ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT,
                            onClick = {
                                vm.setFilterStatus(
                                    if (filterStatus == ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT)
                                        null
                                    else ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT
                                )
                            },
                            label = { Text("Token-Block") },
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterStatus == ExecutionStatus.RUNNING,
                            onClick = {
                                vm.setFilterStatus(
                                    if (filterStatus == ExecutionStatus.RUNNING) null
                                    else ExecutionStatus.RUNNING
                                )
                            },
                            label = { Text("Laufend") },
                        )
                    }
                }

                if (executions.isEmpty()) {
                    Text(
                        text =
                            if (filterStatus != null)
                                "Keine Eintraege mit diesem Filter."
                            else
                                "Noch keine Ausfuehrungen. Sobald du einen Prompt mit " +
                                    "dem Play-Button startest, taucht er hier mit allen " +
                                    "Schritten auf.",
                        modifier = Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textSecondary,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 16.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(executions, key = { it.id }) { exec ->
                            ExecutionRow(exec = exec, onClick = { vm.openDetail(exec.id) })
                        }
                    }
                }
            }
        }
    }

    if (showPruneConfirm) {
        AlertDialog(
            onDismissRequest = { showPruneConfirm = false },
            title = { Text("Alte Audit-Eintraege loeschen") },
            text = {
                Text(
                    text =
                        "Loescht die ALLEN 100 aeltesten Ausfuehrungen aus dem Verlauf. " +
                            "Die zugehoerigen Schritte werden via CASCADE automatisch mit " +
                            "geloescht. Token-Statistiken bleiben unveraendert. Diese " +
                            "Aktion kann nicht rueckgaengig gemacht werden.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.pruneOldest(100)
                        showPruneConfirm = false
                    },
                ) { Text("Loeschen") }
            },
            dismissButton = {
                TextButton(onClick = { showPruneConfirm = false }) { Text("Abbrechen") }
            },
        )
    }

    selectedExecutionId?.let {
        val steps by vm.selectedExecutionSteps.collectAsStateWithLifecycle()
        val exec = executions.firstOrNull { it.id == selectedExecutionId }
        ExecutionDetailDialog(
            execution = exec,
            steps = steps,
            onDismiss = { vm.closeDetail() },
        )
    }
}

@Composable
private fun ExecutionRow(exec: PromptExecutionEntity, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    val statusColor =
        when (exec.status) {
            ExecutionStatus.SUCCESS -> LocalCosmos.current.accent
            ExecutionStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
            ExecutionStatus.FAILED -> LocalCosmos.current.crit
            ExecutionStatus.INTERRUPTED -> LocalCosmos.current.crit
            ExecutionStatus.CANCELLED -> MaterialTheme.colorScheme.outline
            ExecutionStatus.BLOCKED_BY_TOKEN_LIMIT,
            ExecutionStatus.BLOCKED_BY_PERMISSION,
            ExecutionStatus.BLOCKED_BY_TIMEOUT,
            ExecutionStatus.BLOCKED_BY_USER_REJECT -> MaterialTheme.colorScheme.tertiary
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cosmos.glassBg,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .height(10.dp)
                            .padding(end = 8.dp),
                ) {
                    Surface(
                        modifier = Modifier.height(10.dp).fillMaxWidth(),
                        color = statusColor,
                        shape = RoundedCornerShape(5.dp),
                        content = {},
                    )
                }
                Text(
                    text = exec.snapshotName,
                    style = MaterialTheme.typography.titleSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Text(
                text = "${exec.status.label}  ·  ${formatDate(exec.startedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
            if (exec.tokensTotal > 0 || exec.toolCallCount > 0) {
                Text(
                    text =
                        "Tokens: ${exec.tokensTotal}  ·  Tools: ${exec.toolCallCount}  ·  " +
                            "Modell: ${exec.modelUsed}",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
            exec.finalAnswer?.takeIf { it.isNotBlank() }?.let { answer ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = answer.take(220) + if (answer.length > 220) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textPrimary,
                )
            }
            exec.errorMessage?.takeIf { it.isNotBlank() }?.let { err ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Fehler: $err",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalCosmos.current.crit,
                )
            }
        }
    }
}

@Composable
private fun ExecutionDetailDialog(
    execution: PromptExecutionEntity?,
    steps: List<PromptExecutionStepEntity>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = execution?.snapshotName ?: "Ausfuehrungs-Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                if (execution != null) {
                    Text(
                        text =
                            "${execution.status.label}  ·  " +
                                formatDate(execution.startedAt) +
                                (execution.finishedAt?.let {
                                    "  ·  Dauer: ${(it - execution.startedAt) / 1000}s"
                                } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Modell: ${execution.modelUsed}  ·  " +
                            "Tokens ${execution.tokensInput} in / ${execution.tokensOutput} out",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = "Schritte (${steps.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                if (steps.isEmpty()) {
                    Text(
                        text = "Keine Schritte protokolliert.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(steps, key = { it.id }) { step -> StepDetailCard(step) }
                    }
                }
                if (execution?.finalAnswer != null) {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Finale Antwort:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = execution.finalAnswer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schliessen") } },
    )
}

@Composable
private fun StepDetailCard(step: PromptExecutionStepEntity) {
    val (icon, accent) =
        when (step.stepType) {
            StepType.LLM_CALL -> "🧠" to MaterialTheme.colorScheme.primary
            StepType.TOOL_CALL -> "🔧" to MaterialTheme.colorScheme.tertiary
            StepType.USER_CONFIRM -> "✓" to MaterialTheme.colorScheme.secondary
            StepType.BLOCKED -> "⛔" to MaterialTheme.colorScheme.error
        }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text =
                    "$icon Schritt ${step.stepIndex + 1}: ${step.stepType.name}" +
                        if (step.toolName != null) "  ·  ${step.toolName}" else "",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
            step.llmTextOutput?.takeIf { it.isNotBlank() }?.let { text ->
                Text(
                    text = text.take(400),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            step.toolArgsJson?.takeIf { it.isNotBlank() }?.let { args ->
                Text(
                    text = "Args: ${args.take(200)}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            step.toolResultJson?.takeIf { it.isNotBlank() }?.let { result ->
                Text(
                    text = "Result: ${result.take(200)}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            step.confirmDecision?.let { decision ->
                Text(
                    text = "Entscheidung: ${decision.name}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            step.error?.takeIf { it.isNotBlank() }?.let { err ->
                Text(
                    text = "Fehler: $err",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (step.createdEntityIds.isNotEmpty() ||
                step.updatedEntityIds.isNotEmpty() ||
                step.deletedEntityIds.isNotEmpty()
            ) {
                val touched =
                    listOfNotNull(
                        step.createdEntityIds.size.takeIf { it > 0 }?.let { "+$it" },
                        step.updatedEntityIds.size.takeIf { it > 0 }?.let { "~$it" },
                        step.deletedEntityIds.size.takeIf { it > 0 }?.let { "-$it" },
                    )
                Text(
                    text = "Aenderungen: ${touched.joinToString(" / ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val DATE_FORMAT = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.GERMAN)

private fun formatDate(millis: Long): String = DATE_FORMAT.format(Date(millis))
