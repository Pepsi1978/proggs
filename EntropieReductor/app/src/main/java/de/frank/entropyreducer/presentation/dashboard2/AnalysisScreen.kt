package de.frank.entropyreducer.presentation.dashboard2

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MarkdownView
import de.frank.entropyreducer.presentation.components.StatusBar
import de.frank.entropyreducer.presentation.components.ThemeToggleIcon
import de.frank.entropyreducer.presentation.components.charts.EntropyTrendChart
import de.frank.entropyreducer.presentation.components.charts.EntropyTrendLegend
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.label
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Dashboard 2 — Entropie-Analyse (Spec §11).
 *
 * Aufbau:
 *  1. Status-Balken
 *  2. Schnellstatistik (Grid 2x2)
 *  3. Trend-Chart 30/90/365 Tage mit Schichtblock-Hintergrund
 *  4. Markdown-Analyse-Card mit „Jetzt analysieren"-Button
 */
@Composable
fun AnalysisScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    currentTab: String,
    onOpenSubArea: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    vm: AnalysisViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
    }

    val themeVm: ThemeViewModel = hiltViewModel()
    val themeMode by themeVm.themeMode.collectAsStateWithLifecycle()

    // Frank-Wunsch 2026-05-22: Einheitliches Mic-Verhalten ueber alle Reiter.
    // Mic-Tap oeffnet zwei runde Buttons (Schreiben/Aufnehmen) in Gruen — der
    // resultierende Text wird ueber TasksViewModel.processCapturedText als
    // neue Aufgabe gespeichert.
    var micActionsOpen by remember { mutableStateOf(false) }
    val tasksVm: de.frank.entropyreducer.presentation.dashboard1.TasksViewModel = hiltViewModel()

    CosmosScaffold(
        title = "Analyse",
        actions = {
            ThemeToggleIcon(current = themeMode, onCycle = themeVm::cycle)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, "Einstellungen", tint = cosmos.textPrimary)
            }
        },
        bottomBar = {
            CosmosBottomBar(
                currentTab = currentTab,
                micState = de.frank.entropyreducer.presentation.components.MicState.IDLE,
                onTabSelected = onSwitchTab,
                onMicClick = { micActionsOpen = !micActionsOpen },
                onSubAreaSelected = onOpenSubArea,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                StatusBar(percent = state.statusBreakdown?.total ?: 50, breakdown = state.statusBreakdown)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { QuickStatsGrid(state) }

                    item {
                        GlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Trend",
                                        color = cosmos.textPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    ALL_TREND_RANGES.forEach { r ->
                                        FilterChip(
                                            selected = state.trendRange == r,
                                            onClick = { vm.setRange(r) },
                                            label = { Text(r.label) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CosmosColors.AccentPrimary.copy(alpha = 0.20f),
                                                selectedLabelColor = cosmos.textPrimary,
                                                containerColor = cosmos.glassBg,
                                                labelColor = cosmos.textSecondary,
                                            ),
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                        )
                                    }
                                }
                                EntropyTrendChart(
                                    series = state.trendSeries,
                                    shifts = state.trendShifts,
                                )
                                if (state.trendSeries.isNotEmpty()) {
                                    EntropyTrendLegend(state.trendSeries.keys.toList())
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = vm::runAnalysis,
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmosColors.AccentPrimary,
                                contentColor = CosmosColors.BgDark,
                                disabledContainerColor = CosmosColors.AccentPrimary.copy(alpha = 0.4f),
                            ),
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = CosmosColors.BgDark,
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("KI denkt nach …")
                            } else {
                                Icon(Icons.Outlined.AutoAwesome, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Jetzt analysieren", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (state.markdown.isNotBlank()) {
                        item {
                            GlassCard {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Letzte Analyse: ${formatTimestamp(state.markdownAt)}",
                                        color = cosmos.textSecondary,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    MarkdownView(text = state.markdown)
                                }
                            }
                        }
                    } else {
                        item {
                            GlassCard {
                                Text(
                                    text = "Noch keine Analyse vorhanden. Tippe auf \"Jetzt analysieren\", um Muster, verborgene Zusammenhaenge und strategische Empfehlungen zu erhalten.",
                                    color = cosmos.textSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(120.dp)) }
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            ) { Snackbar(it) }
            // Frank-Wunsch 2026-05-22: einheitliche Mic-Aktion mit BottomBar-Farbe.
            // Switcher offen → Cyan, sonst Gruen (Analyse-Sub).
            val switcher = de.frank.entropyreducer.presentation.navigation.LocalBottomBarSwitcher.current
            val micAccent = if (switcher.showSwitcher) Color(0xFF0891B2) else Color(0xFF16A34A)
            de.frank.entropyreducer.presentation.components.MicCaptureActions(
                visible = micActionsOpen,
                accent = micAccent,
                onTextCommit = { text, source -> tasksVm.processCapturedText(text, source) },
                onClose = { micActionsOpen = false },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun QuickStatsGrid(state: AnalysisUiState) {
    // Soll-Design: 2 große Cards nebeneinander (Bild 13/23) mit Icon-Kreis,
    // Title, großer Zahl und Detail-Text darunter.
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BigStatCard(
            icon = Icons.Outlined.AutoAwesome,
            tint = CosmosColors.AccentSecondary,
            title = "Gesamt-Entropie-Last",
            value = state.totalEntropyLoad.toString(),
            valueSuffix = "/100",
            detail = entropyLevelLabel(state.totalEntropyLoad),
            modifier = Modifier.weight(1f),
        )
        BigStatCard(
            icon = Icons.Outlined.Folder,
            tint = CosmosColors.AccentPrimary,
            title = "Offene Einträge",
            value = state.openCount.toString(),
            valueSuffix = "",
            detail = openEntriesDetail(state),
            modifier = Modifier.weight(1f),
        )
    }
}

private fun entropyLevelLabel(load: Int): String = when {
    load >= 80 -> "Sehr hoch — kritisch"
    load >= 60 -> "Hoch — über Zielbereich"
    load >= 40 -> "Mittel — Aufmerksamkeit"
    load >= 20 -> "Niedrig — gut"
    else -> "Sehr niedrig — exzellent"
}

private fun openEntriesDetail(state: AnalysisUiState): String {
    val critical = state.criticalCount
    val normal = state.openCount - critical
    return if (critical > 0) "$critical kritisch — $normal normal" else "$normal normal"
}

@Composable
private fun BigStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    value: String,
    valueSuffix: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                if (valueSuffix.isNotBlank()) {
                    Text(
                        text = valueSuffix,
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = detail,
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = modifier) {
        Column {
            Text(
                text = title,
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TrendTile(deltaPct: Int, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    val (icon, color) = when {
        deltaPct > 5 -> Icons.Outlined.TrendingUp to CosmosColors.Critical
        deltaPct < -5 -> Icons.Outlined.TrendingDown to CosmosColors.Success
        else -> Icons.Outlined.TrendingFlat to CosmosColors.Warning
    }
    GlassCard(modifier = modifier) {
        Column {
            Text(
                text = "7-Tage-Trend",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${if (deltaPct > 0) "+" else ""}$deltaPct%",
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM. HH:mm")

// Performance-Audit Loop 6 (2026-05-10): TrendRange-Werte als top-level
// statt enum.values()-Array-Allokation pro Recomposition.
private val ALL_TREND_RANGES: List<TrendRange> = TrendRange.entries.toList()

private fun formatTimestamp(ms: Long): String {
    if (ms == 0L) return "—"
    return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime().format(FORMATTER)
}
