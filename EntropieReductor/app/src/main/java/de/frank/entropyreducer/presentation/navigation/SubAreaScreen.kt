package de.frank.entropyreducer.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Sub-Bereich-Platzhalter (Sprint 4, Frank-Wunsch 2026-05-22).
 *
 * Vorher (2026-05-17): rein weisse Flaeche mit dezentem Text. Frank wollte das polierter haben —
 * jetzt zeigt jeder Slot ein passendes Material-Icon in einem Pastell-Kreis, einen
 * aussagekraeftigen Titel und einen 1-2-Satz-Teaser. Die Akzent-Farbe spiegelt die jeweilige
 * Tab-Farbe wider (Analyse gruen, Forscher lila, Biomarker rosé). So sieht der Benutzer sofort was
 * hier mal kommen wird, ohne dass es nach "Bug" aussieht.
 *
 * Frank-Wunsch 2026-06-10: Journal zog (nach Entropie + Thesen am 2026-06-09) eben- falls in den
 * Forscher-Bereich um. FORSCHER-Slot 1 (Tagebuch/"Entropie"), Slot 2 (Thesen) und Slot 3 (Journal)
 * routen NICHT hierdurch (eigene Screens). Platzhalter sind jetzt: TASKS-Slot 1/3 und die Slots der
 * Tabs Analyse/Biomarker.
 *
 * Navigation:
 * - Zurueck-Geste → popBackStack → zurueck zum Parent-Tab
 * - Klick auf Parent-Icon (links in der Sub-Bar) → ebenfalls zurueck
 * - Klick auf ein anderes Sub-Icon → wechselt zum anderen Sub-Screen
 */
@Composable
fun SubAreaScreen(
    parentTab: String,
    subIndex: Int,
    onBack: () -> Unit,
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
    showBottomBar: Boolean = true,
) {
    val cosmos = LocalCosmos.current
    val meta = subSlotMeta(parentTab, subIndex)
    val accent = subSlotAccent(parentTab)
    val parentLabel =
        when (parentTab) {
            Routes.TASKS -> "Aufgaben"
            Routes.ANALYSIS -> "Analyse"
            Routes.SCIENTIST -> "Lernen"
            Routes.BIOMARKER -> "Biomarker"
            else -> "Bereich"
        }

    CosmosScaffold(
        title = "$parentLabel · ${meta.title}",
        showBottomBar = showBottomBar,
        bottomBar = {
            CosmosBottomBar(
                currentTab = parentTab,
                micState = MicState.IDLE,
                onTabSelected = { route -> onSwitchTab(route) },
                onMicClick = { /* Mic-Button ist Platzhalter auf Sub-Screens */ },
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = parentTab,
                selectedSubIndex = subIndex,
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(if (cosmos.isDark) Color(0xFF12100D) else Color(0xFFFAF7F3))
                    .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Icon-Kreis in Tab-Farbe (Pastell-Hintergrund)
                Box(
                    modifier =
                        Modifier.size(112.dp)
                            .clip(RoundedCornerShape(56.dp))
                            .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = meta.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(56.dp),
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = meta.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                // "Bald verfuegbar"-Pill in Tab-Farbe
                Box(
                    modifier =
                        Modifier.clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.18f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Bald verfügbar",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = meta.teaser,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private data class SubSlotMeta(val icon: ImageVector, val title: String, val teaser: String)

/**
 * Frank-Wunsch 2026-05-22 (Sprint 4): pro leeren Sub-Slot eine passende Vorschau. TASKS ist bereits
 * komplett belegt (Tagebuch/Thesen/Wiederkehrend), wird hier nur als Fallback gehalten.
 */
private fun subSlotMeta(parent: String, index: Int): SubSlotMeta {
    return when (parent) {
        // Frank-Wunsch 2026-06-10: Journal zog in den Forscher-Bereich um (Slot 3,
        // eigener Screen). An seiner alten Stelle: TASKS-Slot 1 = "Priorität"
        // (vorerst Platzhalter). Slot 2 (Mental) routet nicht hierdurch; Slot 3
        // bleibt leerer Platzhalter. Beim Forscher routen Slot 1 (Entropie),
        // Slot 2 (Thesen) und Slot 3 (Journal) ueber eigene Screens.
        Routes.TASKS ->
            when (index) {
                1 ->
                    SubSlotMeta(
                        icon = Icons.Outlined.Flag,
                        title = "Gewohnheit",
                        teaser =
                            "Deine wichtigsten Aufgaben und Themen — priorisiert auf einen " +
                                "Blick. Inhalt folgt in einem der nächsten Updates.",
                    )
                else ->
                    SubSlotMeta(
                        icon = Icons.Outlined.Lightbulb,
                        title = "Bereich $index",
                        teaser =
                            "Inhalt folgt — dieser Slot ist für eine zukünftige Funktion reserviert.",
                    )
            }
        Routes.ANALYSIS ->
            when (index) {
                // Frank-Wunsch 2026-06-21: Das Parent-Icon "Analyse" wurde durch ein
                // Sub-Icon "2" ersetzt. Index 2 ist der NEUE Platzhalter-Slot — ein
                // bewusst generischer "Punkt 2 — bald verfügbar" ohne inhaltliche
                // Vorschau (kein Wochenrückblick/Monatsrückblick). Die bisherigen
                // Vorschauen rutschen durch das eingefuegte Index-2 um eins auf:
                // Wochenrückblick bleibt Index 1, Monatsrückblick wandert 2→3,
                // Korrelations-Matrix wandert 3→4.
                1 ->
                    SubSlotMeta(
                        icon = Icons.Outlined.CalendarViewWeek,
                        title = "Wochenrückblick",
                        teaser =
                            "Auf einen Blick: Dein Entropie-Trend der letzten 7 Tage, " +
                                "die wichtigsten Erfolge und was diese Woche besonders Energie gekostet hat.",
                    )
                2 ->
                    SubSlotMeta(
                        icon = Icons.Outlined.Tune,
                        title = "Punkt 2",
                        teaser =
                            "Dieser Bereich ist noch in Arbeit. Inhalt folgt in einem der nächsten Updates.",
                    )
                3 ->
                    SubSlotMeta(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Monatsrückblick",
                        teaser =
                            "Monatsbilanz mit Vergleich zum Vormonat, KI-bewertete Highlights " +
                                "und Empfehlungen für den nächsten Monat.",
                    )
                else ->
                    SubSlotMeta(
                        icon = Icons.Outlined.Hub,
                        title = "Korrelations-Matrix",
                        teaser =
                            "Welche Aufgaben-Kategorien beeinflussen welche Biomarker am stärksten — " +
                                "datengestützte Muster aus deinen letzten 90 Tagen.",
                    )
            }
        Routes.SCIENTIST ->
            when (index) {
                1 ->
                    SubSlotMeta(
                        icon = Icons.Outlined.Science,
                        title = "Hypothesen-Archiv",
                        teaser =
                            "Alle laufenden und abgeschlossenen Experimente mit Ergebnis-Status, " +
                                "Biomarker-Vergleich und Verlinkung zu beteiligten Aufgaben.",
                    )
                2 ->
                    SubSlotMeta(
                        icon = Icons.Outlined.Forum,
                        title = "Forscher-Verlauf",
                        teaser =
                            "Frühere Diskussionen mit dem Forscher — durchsuchbar, " +
                                "thematisch gruppiert, mit Querverweisen zu Hypothesen.",
                    )
                else ->
                    SubSlotMeta(
                        icon = Icons.Outlined.Lightbulb,
                        title = "Bestätigte Methoden",
                        teaser =
                            "Insight-Board mit allen Methoden die mehrfach erfolgreich " +
                                "deine Entropie reduziert haben — sortiert nach Konfidenz.",
                    )
            }
        Routes.BIOMARKER ->
            when (index) {
                1 ->
                    SubSlotMeta(
                        icon = Icons.Outlined.NightsStay,
                        title = "Schlafdetails",
                        teaser =
                            "REM-, Tief- und Leichtschlaf-Phasen im Verlauf, Schlafeffizienz und " +
                                "Korrelationen zwischen Tagesaktivität und Schlafqualität.",
                    )
                2 ->
                    SubSlotMeta(
                        icon = Icons.Outlined.DirectionsRun,
                        title = "Trainings-Übersicht",
                        teaser =
                            "Alle Workouts mit GPS-Track, Pulsverlauf und Pace-Splits — " +
                                "mit Trend-Vergleich und Training-Load-Score.",
                    )
                else ->
                    SubSlotMeta(
                        icon = Icons.Outlined.EmojiEvents,
                        title = "Persönliche Bestleistungen",
                        teaser =
                            "Rekorde, Streaks und Meilensteine — automatisch erkannt aus deinen " +
                                "Trainings, Biomarker-Werten und Erfolgs-Aufgaben.",
                    )
            }
        else ->
            SubSlotMeta(
                icon = Icons.Outlined.Lightbulb,
                title = "Bereich $index",
                teaser = "Inhalt folgt — dieser Slot ist für eine zukünftige Funktion reserviert.",
            )
    }
}

/**
 * Akzent-Farbe pro Tab (gespiegelt aus CosmosBottomBar.subModeTint). Wird fuer Icon-Tint,
 * Pill-Hintergrund und Icon-Kreis-Hintergrund verwendet.
 */
@Composable
private fun subSlotAccent(parent: String): Color =
    when (parent) {
        Routes.TASKS -> LocalCosmos.current.accentTasks // Orange
        Routes.ANALYSIS -> LocalCosmos.current.accentAnalyse // Grün
        Routes.SCIENTIST -> LocalCosmos.current.accentForscher // Violett
        Routes.BIOMARKER -> LocalCosmos.current.accentBio // Rosé
        else -> LocalCosmos.current.accent // Cyan-600 (Default)
    }
