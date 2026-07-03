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
import androidx.compose.material.icons.outlined.Analytics
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
 * 1. Status-Balken
 * 2. Schnellstatistik (Grid 2x2)
 * 3. Trend-Chart 30/90/365 Tage mit Schichtblock-Hintergrund
 * 4. Markdown-Analyse-Card mit „Jetzt analysieren"-Button
 */
@Composable
fun AnalysisScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    currentTab: String,
    onOpenSubArea: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    showBottomBar: Boolean = true,
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
        showBottomBar = showBottomBar,
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
        // Frank-Wunsch 2026-06-21: Der Analyse-Hauptscreen (Trend-Chart,
        // Quick-Stats, "Jetzt analysieren"-Button) wurde durch einen Platzhalter
        // ersetzt. Der Inhalt kommt spaeter. ViewModel + UseCases bleiben fuer
        // spaetere Wiederverwendung erhalten.
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier =
                        Modifier.size(112.dp)
                            .clip(RoundedCornerShape(56.dp))
                            .background(LocalCosmos.current.accentAnalyse.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = LocalCosmos.current.accentAnalyse,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Analyse",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(LocalCosmos.current.accentAnalyse.copy(alpha = 0.18f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Bald verfügbar",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalCosmos.current.accentAnalyse,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Dieser Bereich ist noch in Arbeit. Inhalt folgt in einem der nächsten Updates.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            ) {
                Snackbar(it)
            }
            // Frank-Wunsch 2026-05-22: einheitliche Mic-Aktion mit BottomBar-Farbe.
            // Switcher offen → Cyan, sonst Gruen (Analyse-Sub).
            val switcher =
                de.frank.entropyreducer.presentation.navigation.LocalBottomBarSwitcher.current
            val micAccent = if (switcher.showSwitcher) LocalCosmos.current.accent else LocalCosmos.current.accentAnalyse
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
