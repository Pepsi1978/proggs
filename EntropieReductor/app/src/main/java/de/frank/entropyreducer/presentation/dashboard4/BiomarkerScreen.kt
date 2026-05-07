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
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RecoveryRing(score = state.latest?.recoveryScore)
                }
            }
            item { KeyValueGrid(state) }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("HRV-Verlauf (30 Tage)", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        HrvLineChart(values = state.history30Days.mapNotNull { it.hrvMs })
                    }
                }
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
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Strain (30 Tage)", style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
                        Spacer(Modifier.height(8.dp))
                        HrvLineChart(
                            values = state.history30Days.mapNotNull { it.dayStrain },
                            accent = CosmosColors.Warning,
                            unit = "",
                            title = "Strain (30 Tage)",
                        )
                    }
                }
            }
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KeyCard(
            modifier = Modifier.weight(1f),
            label = "HRV",
            value = latest?.hrvMs?.let { "${"%.0f".format(it)} ms" } ?: "—",
        )
        KeyCard(
            modifier = Modifier.weight(1f),
            label = "RHR",
            value = latest?.restingHeartRate?.let { "$it" } ?: "—",
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val sleepMin = latest?.sleepTotalMinutes ?: 0
        val sleepLabel = if (sleepMin == 0) {
            "—"
        } else {
            "${sleepMin / 60}h${(sleepMin % 60).toString().padStart(2, '0')}"
        }
        KeyCard(modifier = Modifier.weight(1f), label = "Schlaf", value = sleepLabel)
        KeyCard(
            modifier = Modifier.weight(1f),
            label = "Sleep-Perf.",
            value = latest?.sleepPerformance?.let { "$it %" } ?: "—",
        )
    }
}

@Composable
private fun KeyCard(modifier: Modifier = Modifier, label: String, value: String) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = modifier) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = cosmos.textSecondary)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = cosmos.textPrimary)
        }
    }
}
