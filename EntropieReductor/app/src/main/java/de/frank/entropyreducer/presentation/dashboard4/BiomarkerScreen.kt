package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.components.StatusBar
import de.frank.entropyreducer.presentation.components.ThemeToggleIcon
import de.frank.entropyreducer.presentation.components.charts.HrvLineChart
import de.frank.entropyreducer.presentation.components.charts.RecoveryRing
import de.frank.entropyreducer.presentation.components.charts.SleepStagesBar
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Dashboard 4 — Biomarker. Spec §13.
 * Recovery-Ring + Schluesselwerte (HRV/RHR/Schlaf/Sleep-Performance) + HRV-Verlauf
 * + Schlafstadien gestern + Strain. Wenn Whoop nicht verbunden ist, zeigen wir
 * einen Empty-State mit Verweis auf Settings.
 */
@Composable
fun BiomarkerHostScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    vm: BiomarkerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val cosmos = LocalCosmos.current
    val themeVm: ThemeViewModel = hiltViewModel()
    val themeMode by themeVm.themeMode.collectAsState()

    CosmosScaffold(
        title = "Biomarker",
        actions = {
            ThemeToggleIcon(current = themeMode, onCycle = themeVm::cycle)
            IconButton(onClick = vm::refreshNow) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Whoop-Sync starten",
                    tint = cosmos.textPrimary,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Einstellungen",
                    tint = cosmos.textPrimary,
                )
            }
        },
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.BIOMARKER,
                micState = MicState.IDLE,
                onTabSelected = onSwitchTab,
                onMicClick = { onSwitchTab(Routes.TASKS) },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatusBar(
                    percent = state.statusBreakdown?.total ?: 0,
                    breakdown = state.statusBreakdown,
                )
            }
            item { GesamterholungCard(state) }
            item { KeyValueGrid(state) }

            // History-Charts — alle 8 Whoop-Werte mit eigenem 30-Tage-Verlauf.
            // Frank-Wunsch 2026-05-08: jeder Whoop-Wert braucht eigene History-Card,
            // damit Trends sichtbar sind (vorher waren nur HRV + Strain als Charts da).
            item {
                MetricHistoryCard(
                    title = "HRV-Verlauf (30 Tage)",
                    accent = CosmosColors.AccentPrimary,
                    values = state.history30Days.mapNotNull { it.hrvMs },
                    unit = "ms",
                )
            }
            item {
                MetricHistoryCard(
                    title = "Resting Heart Rate (30 Tage)",
                    accent = CosmosColors.Critical,
                    values = state.history30Days.mapNotNull { it.restingHeartRate?.toDouble() },
                    unit = "bpm",
                )
            }
            item {
                MetricHistoryCard(
                    title = "Schlaf-Performance (30 Tage)",
                    accent = CosmosColors.Success,
                    values = state.history30Days.mapNotNull { it.sleepPerformance?.toDouble() },
                    unit = "%",
                )
            }
            item {
                MetricHistoryCard(
                    title = "Schlafdauer (30 Tage)",
                    accent = CosmosColors.AccentSecondary,
                    values = state.history30Days.mapNotNull { it.sleepTotalMinutes?.toDouble() },
                    unit = "min",
                )
            }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Schlafstadien gestern Nacht", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                        Spacer(Modifier.height(12.dp))
                        SleepStagesBar(
                            remMinutes = state.latest?.sleepRemMinutes,
                            deepMinutes = state.latest?.sleepDeepMinutes,
                            lightMinutes = state.latest?.sleepLightMinutes,
                            awakeMinutes = state.latest?.sleepAwakeMinutes,
                        )
                    }
                }
            }
            item {
                MetricHistoryCard(
                    title = "Strain (30 Tage)",
                    accent = CosmosColors.Warning,
                    values = state.history30Days.mapNotNull { it.dayStrain },
                    unit = "",
                )
            }
            item {
                MetricHistoryCard(
                    title = "Day Kilojoules (30 Tage)",
                    accent = CosmosColors.AccentPrimary,
                    values = state.history30Days.mapNotNull { it.dayKilojoules },
                    unit = "kJ",
                )
            }
            // Korrelations-Card: zeigt Pearson-Korrelation HRV ↔ Schlafdauer
            // ueber die 30 Tage — hilft Frank Muster zu sehen.
            item { CorrelationCard(state) }
            if (state.latest == null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                "Noch keine Biomarker",
                                style = MaterialTheme.typography.titleMedium,
                                color = cosmos.textPrimary,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Verbinde dein Whoop-Armband in den Einstellungen, um Recovery, " +
                                    "HRV und Schlafdaten in den Status einzubeziehen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textSecondary,
                            )
                        }
                    }
                }
            }
            state.message?.let { msg ->
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = cosmos.textSecondary)
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun KeyValueGrid(state: BiomarkerUiState) {
    val latest = state.latest
    val history = state.history30Days
    // 30-Tage-Mittel pro Metrik (ohne den heutigen Wert) — Basis fuer Trend-Pfeil + Delta.
    val avgHrv = history.mapNotNull { it.hrvMs }.takeIf { it.isNotEmpty() }?.average()
    val avgRhr = history.mapNotNull { it.restingHeartRate }.takeIf { it.isNotEmpty() }?.average()
    val avgSleep = history.mapNotNull { it.sleepTotalMinutes }.takeIf { it.isNotEmpty() }?.average()
    val avgSleepPerf = history.mapNotNull { it.sleepPerformance }.takeIf { it.isNotEmpty() }?.average()

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "HRV",
            value = latest?.hrvMs?.let { "${"%.0f".format(it)} ms" } ?: "—",
            delta = formatDelta(latest?.hrvMs, avgHrv, "ms"),
            deltaPositive = (latest?.hrvMs ?: 0.0) > (avgHrv ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Herzfrequenz",
            value = latest?.restingHeartRate?.let { "$it bpm" } ?: "—",
            delta = formatDelta(latest?.restingHeartRate?.toDouble(), avgRhr, "bpm"),
            // Bei RHR ist NIEDRIGER besser — Pfeil-Logik invertiert.
            deltaPositive = (latest?.restingHeartRate?.toDouble() ?: 0.0) < (avgRhr ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val sleepMin = latest?.sleepTotalMinutes ?: 0
        val sleepLabel = if (sleepMin == 0) {
            "—"
        } else {
            "${sleepMin / 60} h ${(sleepMin % 60).toString().padStart(2, '0')} min"
        }
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Schlaf",
            value = sleepLabel,
            delta = formatDelta(latest?.sleepTotalMinutes?.toDouble(), avgSleep, "min", asMinutes = true),
            deltaPositive = (latest?.sleepTotalMinutes?.toDouble() ?: 0.0) > (avgSleep ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Performance",
            value = latest?.sleepPerformance?.let { "$it %" } ?: "—",
            delta = formatDelta(latest?.sleepPerformance?.toDouble(), avgSleepPerf, "%"),
            deltaPositive = (latest?.sleepPerformance?.toDouble() ?: 0.0) > (avgSleepPerf ?: 0.0),
            footnote = "vs. 30-Tage-Mittel",
        )
    }
}

/**
 * Formatiert die Differenz zwischen aktuellem Wert und 30-Tage-Mittel als
 * "+X" / "-X" inkl. Einheit. Bei null-Werten leerer String.
 */
private fun formatDelta(current: Double?, avg: Double?, unit: String, asMinutes: Boolean = false): String {
    if (current == null || avg == null) return ""
    val diff = current - avg
    val sign = if (diff >= 0) "+" else ""
    return if (asMinutes) {
        val minutes = kotlin.math.abs(diff).toInt()
        val h = minutes / 60
        val m = minutes % 60
        val signActual = if (diff >= 0) "+" else "-"
        if (h > 0) "$signActual${h} h ${m.toString().padStart(2, '0')} $unit"
        else "$signActual${m} $unit"
    } else {
        "$sign${"%.0f".format(diff)} $unit"
    }
}

@Composable
private fun MetricMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    delta: String,
    deltaPositive: Boolean,
    footnote: String,
) {
    val cosmos = LocalCosmos.current
    val deltaColor = if (deltaPositive) CosmosColors.Success else CosmosColors.Critical
    GlassCard(modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cosmos.textSecondary)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = cosmos.textPrimary)
            if (delta.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(delta, color = deltaColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(2.dp))
            Text(footnote, color = cosmos.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Generische History-Chart-Card. Wird fuer alle Whoop-Metriken wiederverwendet
 * (HRV, RHR, Schlaf-Performance, Schlafdauer, Strain, Day Kilojoules) damit jede
 * Metrik einen 30-Tage-Verlauf bekommt. Faellt bei leeren Daten auf einen kurzen
 * Placeholder zurueck statt einen leeren Chart anzuzeigen.
 */
@Composable
private fun MetricHistoryCard(
    title: String,
    accent: androidx.compose.ui.graphics.Color,
    values: List<Double>,
    unit: String,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
            Spacer(Modifier.height(8.dp))
            if (values.isEmpty()) {
                Text(
                    text = "Noch keine Daten — sync dein Whoop-Armband.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                HrvLineChart(values = values, accent = accent, unit = unit, title = title)
            }
        }
    }
}

/**
 * Korrelations-Card: berechnet Pearson-Korrelation zwischen HRV und Schlafdauer
 * ueber die letzten 30 Tage. Frank-Wunsch (Soll-Bild 15/25): "zeigt ob mehr Schlaf
 * mit hoeherer HRV einhergeht".
 */
@Composable
private fun CorrelationCard(state: BiomarkerUiState) {
    val cosmos = LocalCosmos.current
    val pairs = state.history30Days.mapNotNull { snap ->
        val hrv = snap.hrvMs ?: return@mapNotNull null
        val sleep = snap.sleepTotalMinutes ?: return@mapNotNull null
        hrv to sleep.toDouble()
    }
    val r = if (pairs.size >= 3) pearson(pairs) else null
    val (label, color) = when {
        r == null -> "Nicht genug Daten" to cosmos.textSecondary
        r >= 0.5 -> "Starke positive Korrelation" to CosmosColors.Success
        r >= 0.2 -> "Schwache positive Korrelation" to CosmosColors.AccentPrimary
        r >= -0.2 -> "Keine klare Korrelation" to cosmos.textSecondary
        r >= -0.5 -> "Schwache negative Korrelation" to CosmosColors.Warning
        else -> "Starke negative Korrelation" to CosmosColors.Critical
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "HRV ↔ Schlafdauer",
                style = MaterialTheme.typography.titleMedium,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (r != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Pearson r = ${"%.2f".format(r)} (n=${pairs.size})",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Hoehere Werte deuten an: mehr Schlaf -> hoehere HRV. " +
                    "Negative Werte heissen: mehr Schlaf -> niedrigere HRV (selten).",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

private fun pearson(pairs: List<Pair<Double, Double>>): Double {
    val n = pairs.size
    val sumX = pairs.sumOf { it.first }
    val sumY = pairs.sumOf { it.second }
    val sumXY = pairs.sumOf { it.first * it.second }
    val sumX2 = pairs.sumOf { it.first * it.first }
    val sumY2 = pairs.sumOf { it.second * it.second }
    val numerator = n * sumXY - sumX * sumY
    val denomSq = (n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY)
    if (denomSq <= 0) return 0.0
    return numerator / kotlin.math.sqrt(denomSq)
}

/**
 * Gesamterholung-Card im Soll-Design (Bild 15/25).
 * Layout: links Title + Status-Sub-Text + Erlaeuterung; rechts grosser Recovery-Ring.
 */
@Composable
private fun GesamterholungCard(state: BiomarkerUiState) {
    val cosmos = LocalCosmos.current
    val score = state.latest?.recoveryScore
    val statusLabel = when {
        score == null -> "Noch keine Daten"
        score >= 75 -> "Dein Koerper ist im Hoch."
        score >= 50 -> "Dein Koerper ist im Gleichgewicht."
        score >= 25 -> "Dein Koerper braucht heute Schonung."
        else -> "Dein Koerper ist erschoepft."
    }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Gesamterholung",
                    style = MaterialTheme.typography.titleMedium,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    statusLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Erholung basiert auf mehreren Biomarkern und Trends.",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
            androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier.width(120.dp).height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                RecoveryRing(score = score)
            }
        }
    }
}
