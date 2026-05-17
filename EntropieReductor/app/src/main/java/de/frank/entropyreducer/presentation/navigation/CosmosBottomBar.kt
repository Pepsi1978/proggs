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
 * Bottom-Bar mit zentralem Mic-Button (FAB-Style).
 *
 * Zwei Modi (Frank-Wunsch 2026-05-17, zweite Iteration):
 *  - **Sub-Bar (Default)** — pro aktivem Top-Level-Tab wird sofort die 3-Symbol-
 *    Sub-Bar angezeigt, mit eigener Akzent-Farbe fuer ALLE Icons + Texte + Mic
 *    (Aufgaben mint, Analyse cyan, Forscher violett, Biomarker rosé). Der
 *    Hintergrund bleibt grau wie der Rest der App.
 *  - **5-Tab-Switcher** — nach einer Zurueck-Geste sichtbar. Zeigt die vier
 *    Top-Level-Tabs (Aufgaben/Analyse/Forscher/Biomarker) und den Mic-Button
 *    in Standardfarben. Klick auf einen Tab navigiert dorthin und kehrt sofort
 *    in die Sub-Bar zurueck.
 *
 * [forcedSubMode] erzwingt die Sub-Bar fuer einen bestimmten Parent-Tab — wird
 * vom Sub-Platzhalter-Screen genutzt, damit die Bar dort konsistent in der
 * Tab-Farbe leuchtet.
 *
 * [onSubAreaSelected] wird beim Klick auf eines der 3 Sub-Icons aufgerufen
 * (parentTab + index 1..3). Der Aufrufer navigiert dann auf den entsprechenden
 * Platzhalter-Screen.
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

    // Default = Sub-Bar fuer currentTab. Wird via Zurueck-Geste auf den
    // 5-Tab-Switcher umgeschaltet. Tap auf einen Switcher-Tab schaltet zurueck
    // auf Sub-Bar (passiert automatisch durch Re-Composition nach Navigation).
    var showSwitcher by remember { mutableStateOf(false) }

    val isOnMainTab = currentTab in MAIN_TABS
    val activeSubMode: String? = when {
        forcedSubMode != null -> forcedSubMode
        showSwitcher -> null
        isOnMainTab -> currentTab
        else -> null
    }

    // Zurueck-Geste:
    //  - Sub-Bar sichtbar (und kein forced/sub-screen) → Switcher anzeigen
    //  - forcedSubMode aktiv → BackHandler aus, NavGraph pop wird greifen
    BackHandler(enabled = forcedSubMode == null && !showSwitcher && isOnMainTab) {
        showSwitcher = true
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barBg = if (cosmos.isDark) {
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
                        // Aus dem Switcher heraus: navigieren UND Switcher schliessen.
                        // Die naechste Re-Composition zeigt automatisch die Sub-Bar
                        // fuer den neuen Tab.
                        showSwitcher = false
                        onTabSelected(tab)
                    },
                )
                MicButton(
                    state = micState,
                    onClick = onMicClick,
                    size = 56.dp,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                val tint = subModeTint(activeSubMode)
                SubModeRow(
                    parentTab = activeSubMode,
                    tint = tint,
                    onParentClick = {
                        // Klick auf das Parent-Icon links:
                        //  - Auf Sub-Screen (forcedSubMode): zurueck zum Parent-Tab
                        //    (popBackStack via onTabSelected → tabSwitch).
                        //  - Auf Main-Screen: 5-Tab-Switcher anzeigen.
                        if (forcedSubMode != null) {
                            onTabSelected(activeSubMode)
                        } else {
                            showSwitcher = true
                        }
                    },
                    onSubAreaClick = { index ->
                        onSubAreaSelected(activeSubMode, index)
                    },
                )
                MicButton(
                    state = micState,
                    onClick = onMicClick,
                    size = 56.dp,
                    accentColor = tint,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
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
            tint = if (currentTab == Routes.TASKS) subModeTint(Routes.TASKS) else null,
            onClick = { onTabSelected(Routes.TASKS) },
        )
        TabItem(
            label = "Analyse",
            icon = Icons.Outlined.Analytics,
            tint = if (currentTab == Routes.ANALYSIS) subModeTint(Routes.ANALYSIS) else null,
            onClick = { onTabSelected(Routes.ANALYSIS) },
        )
        // Luecke fuer den Mic-Button
        Spacer(Modifier.width(64.dp))
        TabItem(
            label = "Forscher",
            icon = Icons.Outlined.Science,
            tint = if (currentTab == Routes.SCIENTIST) subModeTint(Routes.SCIENTIST) else null,
            onClick = { onTabSelected(Routes.SCIENTIST) },
        )
        TabItem(
            label = "Biomarker",
            icon = Icons.Outlined.MonitorHeart,
            tint = if (currentTab == Routes.BIOMARKER) subModeTint(Routes.BIOMARKER) else null,
            onClick = { onTabSelected(Routes.BIOMARKER) },
        )
    }
}

@Composable
private fun SubModeRow(
    parentTab: String,
    tint: Color,
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
        TabItem(
            label = parentMeta.label,
            icon = parentMeta.icon,
            tint = tint,
            onClick = onParentClick,
        )
        TabItem(
            label = "1",
            icon = Icons.Outlined.Inbox,
            tint = tint,
            onClick = { onSubAreaClick(1) },
        )
        Spacer(Modifier.width(64.dp))
        TabItem(
            label = "2",
            icon = Icons.Outlined.Tune,
            tint = tint,
            onClick = { onSubAreaClick(2) },
        )
        TabItem(
            label = "3",
            icon = Icons.Outlined.Insights,
            tint = tint,
            onClick = { onSubAreaClick(3) },
        )
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    tint: Color?,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val resolvedTint = tint ?: cosmos.textSecondary
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
                tint = resolvedTint,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = resolvedTint,
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
 * Tab-Tint fuer Sub-Mode: gilt fuer Icons, Texte, Mic-Button (Frank-Wunsch 2026-05-17).
 * Bewusst lebendige Farben, damit Frank auf einen Blick erkennt in welchem Pattern
 * er gerade ist. Identisch zwischen Dark + Light Mode, der graue Hintergrund liefert
 * ausreichend Kontrast in beiden Themes.
 */
private fun subModeTint(tab: String): Color = when (tab) {
    Routes.TASKS -> Color(0xFF34D399)      // Mint / Smaragd — "erledigt"
    Routes.ANALYSIS -> Color(0xFF22D3EE)   // Cyan — "Daten / Diagramm"
    Routes.SCIENTIST -> Color(0xFFA78BFA)  // Violett — "KI / Wissenschaft"
    Routes.BIOMARKER -> Color(0xFFFB7185)  // Rosé — "Herz / Koerper"
    else -> CosmosColors.AccentPrimary
}

private val MAIN_TABS = setOf(
    Routes.TASKS,
    Routes.ANALYSIS,
    Routes.SCIENTIST,
    Routes.BIOMARKER,
)
