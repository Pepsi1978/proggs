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
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
 * - **Sub-Bar (Default)** — pro aktivem Top-Level-Tab wird sofort die 3-Symbol- Sub-Bar angezeigt,
 *   mit eigener Akzent-Farbe fuer ALLE Icons + Texte + Mic (Aufgaben mint, Analyse cyan, Forscher
 *   violett, Biomarker rosé). Der Hintergrund bleibt grau wie der Rest der App.
 * - **5-Tab-Switcher** — nach einer Zurueck-Geste sichtbar. Zeigt die vier Top-Level-Tabs
 *   (Aufgaben/Analyse/Forscher/Biomarker) und den Mic-Button in Standardfarben. Klick auf einen Tab
 *   navigiert dorthin und kehrt sofort in die Sub-Bar zurueck.
 *
 * [forcedSubMode] erzwingt die Sub-Bar fuer einen bestimmten Parent-Tab — wird vom
 * Sub-Platzhalter-Screen genutzt, damit die Bar dort konsistent in der Tab-Farbe leuchtet.
 *
 * [onSubAreaSelected] wird beim Klick auf eines der 3 Sub-Icons aufgerufen (parentTab + index
 * 1..3). Der Aufrufer navigiert dann auf den entsprechenden Platzhalter-Screen.
 */
@Composable
fun CosmosBottomBar(
    currentTab: String,
    micState: MicState,
    onTabSelected: (String) -> Unit,
    onMicClick: () -> Unit,
    onSubAreaSelected: (parentTab: String, index: Int) -> Unit = { _, _ -> },
    forcedSubMode: String? = null,
    selectedSubIndex: Int? = null,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current

    // Geteilter Switcher-State (Frank-Wunsch 2026-05-17, dritte Iteration):
    // - Beim Erststart der App: showSwitcher = true → 5-Tab-Uebersicht.
    // - Tap auf einen Tab in der Uebersicht: showSwitcher = false → Sub-Bar.
    // - Zurueck-Geste aus Sub-Bar: showSwitcher = true → Uebersicht.
    // - Tab-Wechsel: State bleibt erhalten (CompositionLocal lebt in AppNavGraph),
    //   d.h. die Uebersicht erscheint nicht ungewollt wieder waehrend Navigation.
    val switcher = LocalBottomBarSwitcher.current
    val showSwitcher = switcher.showSwitcher

    val isOnMainTab = currentTab in MAIN_TABS
    // Frank-Wunsch 2026-05-25: Die Uebersichtsleiste (showSwitcher) gewinnt IMMER —
    // auch auf einem Sub-Screen (forcedSubMode). Dadurch kann man durch erneutes
    // Tippen auf den bereits offenen Unterbereich (z.B. Journal/Entropie/Thesen)
    // zurueck zur Standardleiste (Aufgaben/Analyse/Forscher/Biomarker) gelangen —
    // nicht nur ueber den Haupt-Tab.
    val activeSubMode: String? =
        when {
            showSwitcher -> null
            forcedSubMode != null -> forcedSubMode
            isOnMainTab -> currentTab
            else -> null
        }

    // Zurueck-Geste:
    //  - Sub-Bar sichtbar (und kein forced/sub-screen) → Switcher anzeigen
    //  - forcedSubMode aktiv → BackHandler aus, NavGraph pop wird greifen
    BackHandler(enabled = forcedSubMode == null && !showSwitcher && isOnMainTab) {
        switcher.showSwitcher = true
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barBg =
        if (cosmos.isDark) {
            CosmosColors.BgDarkAccent.copy(alpha = 0.92f)
        } else {
            CosmosColors.BgLightAccent.copy(alpha = 0.92f)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(72.dp + bottomInset)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(barBg)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(72.dp).align(Alignment.TopCenter)) {
            if (activeSubMode == null) {
                NormalTabsRow(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        // Aus dem Switcher heraus: navigieren UND Switcher schliessen.
                        // Die naechste Re-Composition zeigt automatisch die Sub-Bar
                        // fuer den neuen Tab.
                        switcher.showSwitcher = false
                        onTabSelected(tab)
                    },
                )
                MicButton(
                    state = micState,
                    onClick = onMicClick,
                    size = 56.dp,
                    accentColor = OverviewTint,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                val tint = subModeTint(activeSubMode)
                SubModeRow(
                    parentTab = activeSubMode,
                    tint = tint,
                    selectedSubIndex = selectedSubIndex,
                    onParentClick = {
                        // Klick auf das Parent-Icon links:
                        //  - Auf Sub-Screen (forcedSubMode): zurueck zum Parent-Tab
                        //    (popBackStack via onTabSelected → tabSwitch).
                        //  - Auf Main-Screen: 5-Tab-Switcher anzeigen.
                        if (forcedSubMode != null) {
                            onTabSelected(activeSubMode)
                        } else {
                            switcher.showSwitcher = true
                        }
                    },
                    onSubAreaClick = { index ->
                        if (index == selectedSubIndex) {
                            // Re-Tap auf den bereits offenen Unterbereich → zurueck zur
                            // Uebersichtsleiste UND zur Parent-Hauptansicht (Frank-Wunsch
                            // 2026-05-25, Praezisierung): vorher blieb die ANSICHT im
                            // Unterbereich (z.B. Thesen / Biomarker-"1") haengen, waehrend
                            // die Leiste schon die Uebersicht mit markiertem Parent zeigte.
                            // onTabSelected springt auf das Parent-Hauptpattern
                            // (Aufgaben/Analyse/Forscher/Biomarker) — passend zur Markierung.
                            switcher.showSwitcher = true
                            onTabSelected(activeSubMode)
                        } else {
                            onSubAreaSelected(activeSubMode, index)
                        }
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
private fun NormalTabsRow(currentTab: String, onTabSelected: (String) -> Unit) {
    // Frank-Wunsch 2026-05-17 (vierte Praezisierung): In der Uebersicht sind ALLE
    // vier Tabs in der gleichen hellblauen Akzent-Farbe (passend zum Mic-Button).
    // Die Tab-spezifischen Farben (Orange/Gruen/Lila/Rosé) erscheinen ERST wenn
    // ein Tab aktiv ist (Sub-Bar-Modus).
    val overviewTint = OverviewTint
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        TabItem(
            label = "Aufgaben",
            icon = Icons.Outlined.Checklist,
            tint = overviewTint,
            onClick = { onTabSelected(Routes.TASKS) },
            selected = currentTab == Routes.TASKS,
        )
        TabItem(
            label = "Analyse",
            icon = Icons.Outlined.Analytics,
            tint = overviewTint,
            onClick = { onTabSelected(Routes.ANALYSIS) },
            selected = currentTab == Routes.ANALYSIS,
        )
        // Luecke fuer den Mic-Button
        Spacer(Modifier.width(64.dp))
        TabItem(
            label = "Forscher",
            icon = Icons.Outlined.Science,
            tint = overviewTint,
            onClick = { onTabSelected(Routes.SCIENTIST) },
            selected = currentTab == Routes.SCIENTIST,
        )
        TabItem(
            label = "Biomarker",
            icon = Icons.Outlined.MonitorHeart,
            tint = overviewTint,
            onClick = { onTabSelected(Routes.BIOMARKER) },
            selected = currentTab == Routes.BIOMARKER,
        )
    }
}

@Composable
private fun SubModeRow(
    parentTab: String,
    tint: Color,
    selectedSubIndex: Int?,
    onParentClick: () -> Unit,
    onSubAreaClick: (Int) -> Unit,
) {
    // Frank-Wunsch 2026-05-17 (zweite Praezisierung): Der aktive Tab bleibt an
    // SEINER ursprunglichen Position. Die anderen drei Slots werden mit den
    // Sub-Icons 1..3 (in dieser Reihenfolge) gefuellt — nicht aufgeruckt.
    //
    // Beispiel: Tap auf Biomarker (rechts aussen) → Sub-Icons 1/2/3 stehen an
    // den Positionen Aufgaben/Analyse/Forscher, Biomarker bleibt rechts.
    val parentMeta = parentMetaFor(parentTab)
    val slots = listOf(Routes.TASKS, Routes.ANALYSIS, Routes.SCIENTIST, Routes.BIOMARKER)
    val subIcons = subIconsFor(parentTab)

    // Pre-compute: pro Slot entweder Parent oder Sub-Icon (mit Index 1..3).
    val items: List<SlotItem> = buildList {
        var subCounter = 0
        for (slotTab in slots) {
            if (slotTab == parentTab) {
                add(SlotItem.Parent)
            } else {
                subCounter++
                add(SlotItem.Sub(index = subCounter, meta = subIcons[subCounter - 1]))
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        items.forEachIndexed { index, item ->
            // Mic-Luecke zwischen Slot 2 (Analyse-Position) und Slot 3 (Forscher-Position)
            if (index == 2) Spacer(Modifier.width(64.dp))

            when (item) {
                is SlotItem.Parent ->
                    TabItem(
                        label = parentMeta.label,
                        icon = parentMeta.icon,
                        tint = tint,
                        onClick = onParentClick,
                        // Parent ist "offen" wenn wir auf dem Haupt-Screen sind
                        // (kein Sub-Bereich aktiv → selectedSubIndex == null).
                        selected = selectedSubIndex == null,
                    )
                is SlotItem.Sub -> {
                    val capturedIndex = item.index
                    TabItem(
                        label = item.meta.label,
                        icon = item.meta.icon,
                        tint = tint,
                        onClick = { onSubAreaClick(capturedIndex) },
                        selected = capturedIndex == selectedSubIndex,
                    )
                }
            }
        }
    }
}

private sealed class SlotItem {
    object Parent : SlotItem()

    data class Sub(val index: Int, val meta: SubIconMeta) : SlotItem()
}

private data class SubIconMeta(val icon: ImageVector, val label: String)

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    tint: Color?,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    val cosmos = LocalCosmos.current
    val resolvedTint = tint ?: cosmos.textSecondary
    // Frank-Wunsch 2026-05-25: Der aktive Reiter bleibt DAUERHAFT grau hinterlegt
    // (vorher zeigte sich das Grau nur waehrend des Klick-Ripples und verschwand
    // beim Loslassen). So ist jederzeit sichtbar welcher Reiter offen ist.
    // Neutrales Grau, in Hell und Dunkel separat abgestimmt.
    val selectedBg =
        if (cosmos.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    Box(
        modifier =
            Modifier.size(width = 64.dp, height = 56.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(if (selected) Modifier.background(selectedBg) else Modifier)
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

/**
 * Frank-Wunsch 2026-05-18: Sub-Icons koennen pro Parent-Tab unterschiedlich sein. Aktuell ist nur
 * Sub-Bereich 1 des Aufgaben-Tabs explizit belegt ("Entropie" mit Tagebuch-Buch-Icon). Andere
 * Sub-Bereiche bleiben generisch 2/3 mit Standard-Icons bis sie inhaltlich belegt werden.
 */
private fun subIconsFor(parentTab: String): List<SubIconMeta> =
    when (parentTab) {
        // Frank-Wunsch 2026-06-09: Entropie + Thesen sind vom Aufgaben- in den
        // Forscher-Bereich umgezogen. Aufgaben behaelt nur "Journal" (Slot 1, Buch-
        // Icon); Slot 2/3 sind frei und zeigen generische "bald verfuegbar"-Platzhalter.
        Routes.TASKS ->
            listOf(
                SubIconMeta(Icons.Outlined.Book, "Journal"),
                SubIconMeta(Icons.Outlined.Inbox, "2"),
                SubIconMeta(Icons.Outlined.Insights, "3"),
            )
        // Frank-Wunsch 2026-06-09: Forscher bekommt Entropie (Slot 1, Warndreieck mit
        // Ausrufezeichen) und Thesen (Slot 2, Gluehbirne); Slot 3 bleibt Platzhalter.
        Routes.SCIENTIST ->
            listOf(
                SubIconMeta(Icons.Outlined.Warning, "Entropie"),
                SubIconMeta(Icons.Outlined.Lightbulb, "Thesen"),
                SubIconMeta(Icons.Outlined.Insights, "3"),
            )
        else ->
            listOf(
                SubIconMeta(Icons.Outlined.Inbox, "1"),
                SubIconMeta(Icons.Outlined.Tune, "2"),
                SubIconMeta(Icons.Outlined.Insights, "3"),
            )
    }

private data class ParentMeta(val label: String, val icon: ImageVector)

private fun parentMetaFor(tab: String): ParentMeta =
    when (tab) {
        Routes.TASKS -> ParentMeta("Aufgaben", Icons.Outlined.Checklist)
        Routes.ANALYSIS -> ParentMeta("Analyse", Icons.Outlined.Analytics)
        Routes.SCIENTIST -> ParentMeta("Forscher", Icons.Outlined.Science)
        Routes.BIOMARKER -> ParentMeta("Biomarker", Icons.Outlined.MonitorHeart)
        else -> ParentMeta("Aufgaben", Icons.Outlined.Checklist)
    }

/**
 * Tab-Tint fuer Sub-Mode: gilt fuer Icons, Texte, Mic-Button (Frank-Wunsch 2026-05-17). Bewusst
 * kraeftige, dunklere Farben, damit sie auch auf dem hellgrauen Bar- Hintergrund gut lesbar bleiben
 * (Frank-Reklamation 2026-05-17: Mint und Cyan waren zu hell, der Text war kaum zu sehen). Lila und
 * Rosé hat Frank explizit als okay bestaetigt — die bleiben.
 */
private fun subModeTint(tab: String): Color =
    when (tab) {
        Routes.TASKS -> Color(0xFFEA580C) // Kraeftiges Orange (orange-600)
        Routes.ANALYSIS -> Color(0xFF16A34A) // Mittleres Gruen (green-600), klar
        // unterscheidbar vom Aufgaben-Orange
        Routes.SCIENTIST -> Color(0xFFA78BFA) // Violett — Frank: "Lila ist okay"
        Routes.BIOMARKER -> Color(0xFFFB7185) // Rosé — Frank: "Rot ist okay"
        else -> CosmosColors.AccentPrimary
    }

private val MAIN_TABS = setOf(Routes.TASKS, Routes.ANALYSIS, Routes.SCIENTIST, Routes.BIOMARKER)

/**
 * Akzent-Farbe in der 5-Tab-Uebersicht (Frank-Wunsch 2026-05-17, fuenfte Praezisierung): bewusst
 * dunkler als der Standard-Cyan-Akzent, damit die Beschriftungen unter den Icons auch auf dem
 * hellgrauen Bar-Hintergrund klar lesbar sind. Wird sowohl auf die 4 Tab-Icons/Texte als auch auf
 * den Mic-Button angewandt.
 */
private val OverviewTint: Color = Color(0xFF0891B2) // cyan-600

/**
 * Geteilter Switcher-State (5-Tab-Uebersicht vs. Sub-Bar). Wird per [CompositionLocalProvider] in
 * [AppNavGraph] bereitgestellt, damit der Zustand Tab-Wechsel ueberlebt. Initialwert `true` ist
 * beim App-Start gewollt — Frank will dort zuerst die 5-Tab-Uebersicht sehen und einen Tab waehlen.
 */
class BottomBarSwitcherState {
    var showSwitcher by mutableStateOf(true)
}

val LocalBottomBarSwitcher =
    staticCompositionLocalOf<BottomBarSwitcherState> { BottomBarSwitcherState() }
