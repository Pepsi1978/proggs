package de.frank.entropyreducer.presentation.navigation

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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.components.MicButton
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Bottom-Bar mit zentralem Mic-Button (FAB-Style).
 *
 * Zeigt unten immer nur die vier Hauptreiter Aufgaben/Analyse/Forscher/Biomarker plus Mic.
 * Sub-Navigation sitzt ausschliesslich oben in [SubTabRow]. Die alten Sub-Bar-Parameter bleiben als
 * No-Op erhalten, damit bestehende Screen-Signaturen nicht gleichzeitig umgebaut werden muessen.
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

    // Poka-Yoke gegen Regression: Diese BottomBar darf nie wieder in den alten Sub-Bar-Modus
    // wechseln. Subtabs sind oben, unten bleibt die Hauptnavigation immer gleich.

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Glut: warme, solide Bar-Flaeche (Mockup --bg2) statt halbtransparentem Navy.
    val barBg = cosmos.barBg.copy(alpha = 0.97f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(72.dp + bottomInset)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(barBg)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(72.dp).align(Alignment.TopCenter)) {
            NormalTabsRow(
                currentTab = currentTab,
                onTabSelected = onTabSelected,
            )
            MicButton(
                state = micState,
                onClick = onMicClick,
                size = 56.dp,
                accentColor = cosmos.accent,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun NormalTabsRow(currentTab: String, onTabSelected: (String) -> Unit) {
    // Glut (2026-06-12): In der Uebersicht sind ALLE vier Tabs in Orange-Glut
    // (= Primaerakzent, passend zum Mic-Button) — wie im Mockup. Die Tab-Farbklassen
    // (Orange/Smaragd/Violett/Rosé) erscheinen ERST wenn ein Tab aktiv ist (Sub-Bar-Modus).
    val overviewTint = LocalCosmos.current.accent
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
    subTint: Color,
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

    // Pre-compute: pro Slot entweder Parent oder Sub-Icon (mit Index 1..3 bzw. 1..4).
    // Frank-Wunsch 2026-06-21: Fuer den Analyse-Tab wird das Parent-Icon durch ein
    // Sub-Icon "2" ersetzt — die Sub-Bar zeigt dann 1, 2, 3, 4 (kein Parent mehr).
    val parentIsReplaced = parentTab == Routes.ANALYSIS
    val items: List<SlotItem> = buildList {
        var subCounter = 0
        for (slotTab in slots) {
            if (slotTab == parentTab && !parentIsReplaced) {
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
                        tint = subTint,
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
        // Frank-Wunsch 2026-06-10: Journal ist vom Aufgaben- in den Forscher-Bereich
        // umgezogen (Slot 3). An seiner Stelle: Slot 1 = "Priorität" (Flaggen-Icon,
        // vorerst Platzhalter-Screen). Slot 2 = Mental.
        Routes.TASKS ->
            listOf(
                SubIconMeta(Icons.Outlined.Flag, "Gewohnheit"),
                // Frank-Wunsch 2026-06-09: Slot 2 = Mentalboard.
                SubIconMeta(Icons.Outlined.Psychology, "Mental"),
                // Frank-Wunsch 2026-06-10: Slot 3 = Ideen (Gluehbirne) — 1:1-Klon des
                // Entropie-Bereichs.
                SubIconMeta(Icons.Outlined.AutoAwesome, "Ideen"),
            )
        // Frank-Wunsch 2026-06-21: Analyse-Tab hat kein Parent-Icon mehr in der
        // Sub-Bar — alle 4 Slots sind Sub-Icons mit Labels 1, 2, 3, 4. Sub-Icon "2"
        // (an der ehem. Parent-Position) fuehrt zum Platzhalter "Punkt 2 — bald
        // verfuegbar".
        Routes.ANALYSIS ->
            listOf(
                SubIconMeta(Icons.Outlined.Inbox, "1"),
                SubIconMeta(Icons.Outlined.Tune, "2"),
                SubIconMeta(Icons.Outlined.Insights, "3"),
                SubIconMeta(Icons.Outlined.Hub, "4"),
            )
        // Frank-Wunsch 2026-06-09: Forscher bekommt Entropie (Slot 1, Warndreieck mit
        // Ausrufezeichen) und Thesen (Slot 2, Gluehbirne). Frank-Wunsch 2026-06-10:
        // Slot 3 = Journal (Buch-Icon, vom Aufgaben-Bereich umgezogen).
        Routes.SCIENTIST ->
            listOf(
                SubIconMeta(Icons.Outlined.Warning, "Entropie"),
                SubIconMeta(Icons.Outlined.Lightbulb, "Thesen"),
                SubIconMeta(Icons.Outlined.Book, "Journal"),
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
 * Tab-Tint fuer Sub-Mode: gilt fuer Icons, Texte, Mic-Button (Frank-Wunsch 2026-05-17). Glut
 * (2026-06-12): Theme-abhaengige Tab-Farbklassen aus dem Mockup — Dark nutzt die helleren, Light
 * die tieferen Varianten (Orange/Smaragd/Violett/Rosé). Quelle: LocalCosmos.
 *
 * Frank-Wunsch 2026-06-18: Sub-Tabs unter Aufgaben sind BLAU (nicht Orange).
 * Orange ist nur fuer die Hauptreiter.
 */
@Composable
private fun subModeTint(tab: String): Color {
    val cosmos = LocalCosmos.current
    return when (tab) {
        Routes.TASKS -> cosmos.accentTasksSub // Blau — auch der Parent-Button
        Routes.ANALYSIS -> cosmos.accentAnalyse // Smaragd
        Routes.SCIENTIST -> cosmos.accentForscher // Violett
        Routes.BIOMARKER -> cosmos.accentBio // Rosé
        else -> cosmos.accent
    }
}

/**
 * Sub-Tab-Tint: Gibt die Farbe fuer die UNTERMENUES unter dem jeweiligen Parent-Tab.
 * - Aufgaben: Blau (Frank-Wunsch 2026-06-18)
 * - Alle anderen: gleich wie Parent (Smaragd/Violett/Rosé)
 */
@Composable
private fun subModeSubTint(tab: String): Color {
    val cosmos = LocalCosmos.current
    return when (tab) {
        Routes.TASKS -> cosmos.accentTasksSub // Blau — Sub-Tabs
        Routes.ANALYSIS -> cosmos.accentAnalyse
        Routes.SCIENTIST -> cosmos.accentForscher
        Routes.BIOMARKER -> cosmos.accentBio
        else -> cosmos.accent
    }
}

