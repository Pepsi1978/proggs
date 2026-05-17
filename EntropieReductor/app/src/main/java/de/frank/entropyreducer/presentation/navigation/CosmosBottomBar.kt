package de.frank.entropyreducer.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.components.MicButton
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Bottom-Bar mit 4 Tabs + zentralem Mic-Button (FAB-Style).
 * Mic-Button uebersteht die Tabs und ist global verfuegbar.
 *
 * Sub-Mode (Frank-Wunsch 2026-05-17):
 * Tippt der Nutzer auf einen Tab, wechselt die Bar in einen "Sub-Mode" fuer diesen
 * Tab. Statt der 5 Top-Level-Tabs zeigt sie dann links das Tab-Icon und rechts
 * 3 Platzhalter-Sub-Icons (Inbox / Tune / Insights). Jeder Sub-Mode hat eine
 * eigene Akzent-Hintergrundfarbe. Zurueck-Geste oder Klick auf das Parent-Icon
 * verlaesst den Sub-Mode wieder.
 *
 * Klick auf ein Sub-Icon ruft [onSubAreaSelected] mit (parentTab, index 0..2) auf —
 * der Aufrufer navigiert dann zum entsprechenden weissen Platzhalter-Screen.
 *
 * Wird die Bar auf einem Sub-Screen angezeigt, setzt der Aufrufer [forcedSubMode]
 * auf den Parent-Tab — die Bar zeigt dann sofort und ohne erneuten Tap die Sub-Bar.
 */
@Composable
fun CosmosBottomBar(
    currentTab: String,
    micState: MicState,
    onTabSelected: (String) -> Unit,
    onMicClick: () -> Unit,
    onSubAreaSelected: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    forcedSubMode: String? = null,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current

    // Sub-Mode aus drei Quellen ableiten (Prioritaet: forced > internal):
    //   - forcedSubMode (vom Aufrufer gesetzt, z.B. auf einem Sub-Screen)
    //   - internalSubModeFor (vom Tap auf einen Tab)
    var internalSubModeFor by remember { mutableStateOf<String?>(null) }
    val activeSubMode: String? = forcedSubMode ?: internalSubModeFor

    // Zurueck-Geste verlaesst den lokalen Sub-Mode. Wenn forcedSubMode aktiv ist,
    // gehoert die Zurueck-Geste dem Aufrufer (NavGraph popBackStack) — wir greifen
    // nur ein wenn WIR den Sub-Mode lokal aktiviert haben.
    BackHandler(enabled = forcedSubMode == null && internalSubModeFor != null) {
        internalSubModeFor = null
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barBg = if (activeSubMode != null) {
        subBarBackground(activeSubMode, cosmos.isDark)
    } else if (cosmos.isDark) {
        CosmosColors.BgDarkAccent.copy(alpha = 0.92f)
    } else {
        CosmosColors.BgLightAccent.copy(alpha = 0.92f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp + bottomInset)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(barBg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.TopCenter),
        ) {
            if (activeSubMode == null) {
                NormalTabsRow(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        onTabSelected(tab)
                        internalSubModeFor = tab
                    },
                )
            } else {
                SubModeRow(
                    parentTab = activeSubMode,
                    onParentClick = {
                        // Zurueck zum 5-Tab-View. Wenn forced (= wir sind auf einem
                        // Sub-Screen), navigieren wir zurueck zum Parent-Tab — das
                        // verlaesst den Sub-Screen.
                        if (forcedSubMode != null) {
                            onTabSelected(activeSubMode)
                        } else {
                            internalSubModeFor = null
                        }
                    },
                    onSubAreaClick = { index ->
                        onSubAreaSelected(activeSubMode, index)
                    },
                )
            }
            MicButton(
                state = micState,
                onClick = onMicClick,
                size = 56.dp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun NormalTabsRow(
    currentTab: String,
    onTabSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TabItem(
            label = "Aufgaben",
            icon = Icons.Outlined.Checklist,
            selected = currentTab == Routes.TASKS,
            onClick = { onTabSelected(Routes.TASKS) },
        )
        TabItem(
            label = "Analyse",
            icon = Icons.Outlined.Analytics,
            selected = currentTab == Routes.ANALYSIS,
            onClick = { onTabSelected(Routes.ANALYSIS) },
        )
        // Luecke fuer den Mic-Button
        Spacer(Modifier.width(64.dp))
        TabItem(
            label = "Forscher",
            icon = Icons.Outlined.Science,
            selected = currentTab == Routes.SCIENTIST,
            onClick = { onTabSelected(Routes.SCIENTIST) },
        )
        TabItem(
            label = "Biomarker",
            icon = Icons.Outlined.MonitorHeart,
            selected = currentTab == Routes.BIOMARKER,
            onClick = { onTabSelected(Routes.BIOMARKER) },
        )
    }
}

@Composable
private fun SubModeRow(
    parentTab: String,
    onParentClick: () -> Unit,
    onSubAreaClick: (Int) -> Unit,
) {
    val parentMeta = parentMetaFor(parentTab)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Links: Parent-Tab als Anker (Klick → zurueck zum normalen 5-Tab-View)
        TabItem(
            label = parentMeta.label,
            icon = parentMeta.icon,
            selected = true,
            onClick = onParentClick,
        )
        // Sub-Bereich 1
        TabItem(
            label = "1",
            icon = Icons.Outlined.Inbox,
            selected = false,
            onClick = { onSubAreaClick(1) },
        )
        // Luecke fuer den Mic-Button
        Spacer(Modifier.width(64.dp))
        // Sub-Bereich 2
        TabItem(
            label = "2",
            icon = Icons.Outlined.Tune,
            selected = false,
            onClick = { onSubAreaClick(2) },
        )
        // Sub-Bereich 3
        TabItem(
            label = "3",
            icon = Icons.Outlined.Insights,
            selected = false,
            onClick = { onSubAreaClick(3) },
        )
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val tint = if (selected) CosmosColors.AccentPrimary else cosmos.textSecondary
    Box(
        modifier = Modifier
            .size(width = 64.dp, height = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

private data class ParentMeta(val label: String, val icon: ImageVector)

private fun parentMetaFor(tab: String): ParentMeta = when (tab) {
    Routes.TASKS -> ParentMeta("Aufgaben", Icons.Outlined.Checklist)
    Routes.ANALYSIS -> ParentMeta("Analyse", Icons.Outlined.Analytics)
    Routes.SCIENTIST -> ParentMeta("Forscher", Icons.Outlined.Science)
    Routes.BIOMARKER -> ParentMeta("Biomarker", Icons.Outlined.MonitorHeart)
    else -> ParentMeta("Aufgaben", Icons.Outlined.Checklist)
}

/**
 * Pro Sub-Mode eine eigene dezente Akzent-Farbe — bleibt nah am Standard-Grau
 * damit es nicht knallt, aber gibt sofort visuelles Feedback dass man "drin" ist.
 */
private fun subBarBackground(tab: String, isDark: Boolean): Color = when (tab) {
    // Aufgaben → mint (passt zum "erledigen / Haken setzen"-Gefuehl)
    Routes.TASKS -> if (isDark) Color(0xFF143028).copy(alpha = 0.94f) else Color(0xFFD6EFE2).copy(alpha = 0.94f)
    // Analyse → cyan/blau (passt zu Diagrammen / Daten)
    Routes.ANALYSIS -> if (isDark) Color(0xFF112A38).copy(alpha = 0.94f) else Color(0xFFD3E7F5).copy(alpha = 0.94f)
    // Forscher → violett (passt zu KI / Wissenschaft)
    Routes.SCIENTIST -> if (isDark) Color(0xFF221A38).copy(alpha = 0.94f) else Color(0xFFE2DAF5).copy(alpha = 0.94f)
    // Biomarker → warmes Rosé (passt zu Herz / Koerper)
    Routes.BIOMARKER -> if (isDark) Color(0xFF341A24).copy(alpha = 0.94f) else Color(0xFFF5DBE3).copy(alpha = 0.94f)
    else -> if (isDark) CosmosColors.BgDarkAccent.copy(alpha = 0.92f) else CosmosColors.BgLightAccent.copy(alpha = 0.92f)
}
