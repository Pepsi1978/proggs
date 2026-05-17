package de.frank.entropyreducer.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.MicState

/**
 * Weisser Platzhalter-Screen fuer die Sub-Bereiche unter einem Top-Level-Tab
 * (Frank-Wunsch 2026-05-17). Zeigt einen voellig weissen Inhaltsbereich, damit
 * Frank sofort sieht: "hier kann ich spaeter was einbauen". Die Bottom-Bar
 * laeuft in Sub-Mode fuer den Parent-Tab (eigene Akzent-Farbe + 3 Sub-Icons).
 *
 * Navigation:
 * - Zurueck-Geste → popBackStack → zurueck zum Parent-Tab (5-Tab-Bar)
 * - Klick auf das Parent-Icon (links in der Sub-Bar) → ebenfalls zurueck zum Parent-Tab
 * - Klick auf ein anderes Sub-Icon → wechselt zum anderen Sub-Screen desselben Parents
 *
 * Inhalt: leer (nur ein dezenter Hinweistext oben mittig, damit klar ist es
 * ist ein Platzhalter und kein Bug).
 */
@Composable
fun SubAreaScreen(
    parentTab: String,
    subIndex: Int,
    onBack: () -> Unit,
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
) {
    val parentLabel = when (parentTab) {
        Routes.TASKS -> "Aufgaben"
        Routes.ANALYSIS -> "Analyse"
        Routes.SCIENTIST -> "Forscher"
        Routes.BIOMARKER -> "Biomarker"
        else -> "Bereich"
    }
    CosmosScaffold(
        title = "$parentLabel · Bereich $subIndex",
        bottomBar = {
            CosmosBottomBar(
                currentTab = parentTab,
                micState = MicState.IDLE,
                onTabSelected = { route ->
                    // Klick auf Parent-Tab (links in Sub-Bar) → zurueck zum Parent-Tab.
                    onSwitchTab(route)
                },
                onMicClick = { /* Mic-Button ist Platzhalter auf Sub-Screens */ },
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = parentTab,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = "Bereich $subIndex — Platzhalter",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFAAAAAA),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
