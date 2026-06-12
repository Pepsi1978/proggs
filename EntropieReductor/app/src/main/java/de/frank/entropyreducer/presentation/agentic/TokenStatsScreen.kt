package de.frank.entropyreducer.presentation.agentic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Token-Stats-Screen (Frank-Wunsch 2026-05-21).
 *
 * Zeigt pro Prompt:
 *  - heutigen Token-Verbrauch + Run-Anzahl
 *  - optionales Tages-Limit (mit Fortschrittsbalken wenn gesetzt)
 *  - 30-Tage-Mini-Balkendiagramm (lazy nachgeladen beim Expand)
 *  - Inline-Limit-Editor (Tap auf das Limit-Feld)
 *
 * Sortierung: heutiger Verbrauch absteigend — die "Top-Token-Frasser" stehen oben.
 *
 * Recompute-Button in der Top-Bar baut die Tagesaggregation neu auf
 * (Drift-Schutz gegen die denormalisierte token_usage_daily-Tabelle).
 */
@Composable
fun TokenStatsScreen(onBack: () -> Unit, vm: TokenStatsViewModel = hiltViewModel()) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val historyCache by vm.historyCache.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    var expandedPromptId by remember { mutableStateOf<String?>(null) }
    var limitEditFor by remember { mutableStateOf<TokenStatsRow?>(null) }

    CosmosScaffold(
        title = "Token-Verbrauch",
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
            IconButton(onClick = { vm.recomputeStats() }) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = "Stats neu berechnen",
                    tint = cosmos.textPrimary,
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (rows.isEmpty()) {
                Text(
                    text =
                        "Noch keine Prompt-Ausfuehrungen heute. Sobald du einen Prompt mit dem " +
                            "Play-Button startest, taucht er hier auf.",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            text =
                                "Heutiger Token-Verbrauch pro Prompt. Tippe auf einen Eintrag fuer " +
                                    "die 30-Tage-Historie oder das Tages-Limit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textSecondary,
                        )
                    }
                    items(rows, key = { it.prompt.id }) { row ->
                        val expanded = expandedPromptId == row.prompt.id
                        TokenRowCard(
                            row = row,
                            expanded = expanded,
                            historyBars = historyCache[row.prompt.id].orEmpty(),
                            onToggle = {
                                expandedPromptId =
                                    if (expanded) null else row.prompt.id
                                if (!expanded) vm.loadHistory(row.prompt.id)
                            },
                            onEditLimit = { limitEditFor = row },
                        )
                    }
                }
            }
        }
    }

    limitEditFor?.let { row ->
        LimitEditDialog(
            currentLimit = row.prompt.tokenLimitPerDay,
            promptName = row.prompt.name,
            onSave = { newLimit ->
                vm.setTokenLimit(row.prompt.id, newLimit)
                limitEditFor = null
            },
            onCancel = { limitEditFor = null },
        )
    }
}

@Composable
private fun TokenRowCard(
    row: TokenStatsRow,
    expanded: Boolean,
    historyBars: List<HistoryBar>,
    onToggle: () -> Unit,
    onEditLimit: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cosmos.glassBg,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header-Zeile
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.prompt.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Kategorie: ${row.prompt.category.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${row.todayTotal}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (row.limitReached) LocalCosmos.current.crit else LocalCosmos.current.accent,
                    )
                    Text(
                        text = "Tokens heute",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
            }

            // Limit-Anzeige + Edit-Toggle
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onEditLimit) {
                    Text(
                        text =
                            if (row.hasLimit)
                                "Limit: ${row.prompt.tokenLimitPerDay} / Tag"
                            else "Limit setzen",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                if (row.todayRunCount > 0) {
                    Text(
                        text = "${row.todayRunCount} Run(s) heute",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
            }

            if (row.hasLimit) {
                LinearProgressIndicator(
                    progress = { row.limitProgressFraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color =
                        when {
                            row.limitProgressFraction >= 1f -> LocalCosmos.current.crit
                            row.limitProgressFraction >= 0.8f -> LocalCosmos.current.accent
                            else -> LocalCosmos.current.accent
                        },
                )
            }

            // Expand: 30-Tage-Bars
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onToggle) {
                Text(
                    text = if (expanded) "30-Tage-Historie ausblenden" else "30-Tage-Historie anzeigen",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                HistoryBars(bars = historyBars)
            }
        }
    }
}

@Composable
private fun HistoryBars(bars: List<HistoryBar>) {
    if (bars.isEmpty()) {
        Text(
            text = "Lade Historie…",
            style = MaterialTheme.typography.labelSmall,
        )
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                bars.forEach { bar ->
                    val barHeight = (bar.relative.coerceIn(0f, 1f) * 56f).dp.coerceAtLeast(2.dp)
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .height(barHeight)
                                .background(
                                    color =
                                        if (bar.tokensTotal > 0) LocalCosmos.current.accent
                                        else LocalCosmos.current.accent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(2.dp),
                                ),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = bars.firstOrNull()?.day ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalCosmos.current.textSecondary,
                )
                Text(
                    text = "heute",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalCosmos.current.textSecondary,
                )
            }
            Text(
                text =
                    "Spitze: ${bars.maxOfOrNull { it.tokensTotal } ?: 0} Tokens" +
                        " · Summe: ${bars.sumOf { it.tokensTotal }} Tokens",
                style = MaterialTheme.typography.labelSmall,
                color = LocalCosmos.current.textSecondary,
            )
        }
    }
}

@Composable
private fun LimitEditDialog(
    currentLimit: Int?,
    promptName: String,
    onSave: (Int?) -> Unit,
    onCancel: () -> Unit,
) {
    var input by remember(currentLimit) { mutableStateOf(currentLimit?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Tages-Limit fuer \"$promptName\"") },
        text = {
            Column {
                Text(
                    text =
                        "Setze ein hartes Tages-Token-Limit fuer diesen Prompt. Wenn der heutige " +
                            "Verbrauch das Limit erreicht, blockiert der WorkflowRunner weitere " +
                            "Aufrufe bis Mitternacht. Leer lassen = kein Limit.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { v -> input = v.filter { it.isDigit() }.take(7) },
                    label = { Text("Tokens pro Tag") },
                    placeholder = { Text("z.B. 50000 oder leer") },
                    singleLine = true,
                    keyboardOptions =
                        androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                        ),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(input.toIntOrNull()) },
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Abbrechen") } },
    )
}
