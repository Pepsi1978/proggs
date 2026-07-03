package de.frank.entropyreducer.presentation.dashboard3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.ThemeToggleIcon
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Dashboard 3 — Forscher-Tab (Spec §12).
 *
 * Frank-Wunsch 2026-06-21: Der Wissenschaftler-Chat (Session-Picker, Hypothesen,
 * Chat-Bubbles, Mic-Aufnahme) wurde komplett durch einen Platzhalter "Punkt 4 —
 * bald verfuegbar" ersetzt. ViewModel + UseCases + DAOs bleiben in der Codebase
 * fuer spaetere Wiederverwendung erhalten. Die Sub-Bereiche Tagebuch/Entropie,
 * Thesen und Journal sind hiervon NICHT betroffen — sie bleiben voll funktionsfaehig.
 */
@Composable
fun ScientistScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    currentTab: String,
    onOpenSubArea: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    showBottomBar: Boolean = true,
    vm: ScientistViewModel = hiltViewModel(),
) {
    val cosmos = LocalCosmos.current

    val themeVm: ThemeViewModel = hiltViewModel()
    val themeMode by themeVm.themeMode.collectAsStateWithLifecycle()

    CosmosScaffold(
        title = "Forscher",
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
                onMicClick = { /* Platzhalter — kein Chat mehr */ },
                onSubAreaSelected = onOpenSubArea,
            )
        },
    ) { padding ->
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
                            .background(LocalCosmos.current.accentForscher.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = null,
                        tint = LocalCosmos.current.accentForscher,
                        modifier = Modifier.size(56.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Punkt 4",
                    style = MaterialTheme.typography.headlineSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(LocalCosmos.current.accentForscher.copy(alpha = 0.18f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Bald verfügbar",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalCosmos.current.accentForscher,
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
        }
    }
}
