package de.frank.entropyreducer.presentation.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/** Tab-Höhe (Frank-Wunsch 2026-06-23: ~8 mm statt vorher ~5 mm). */
private val SUB_TAB_HEIGHT = 32.dp

/** Abstand Tab-Leiste → Screen-Kopf darunter (Frank-Wunsch 2026-06-23: 3,5 mm). */
private val SUB_TAB_BOTTOM_PADDING = 9.dp

/**
 * Obere Tab-Leiste fuer Unterreiter (Frank-Wunsch 2026-06-23).
 *
 * Frank-Wunsch 2026-06-23 (Abstands-Reduktion): Der fruehere Kopfbereich (Titel + Aktualisieren-/
 * Einstellungs-Buttons) UEBER der Tab-Leiste ist entfernt — er war doppelt mit den Screen-Koepfen
 * (TasksScreen & Co.), die jeweils ihren eigenen Titel + Aktualisieren + Einstellungen zeigen.
 * Jetzt enthaelt SubTabRow NUR noch die Tab-Leiste und sitzt direkt unter der Status-Bar. Die
 * Screen-Koepfe darunter kleben dank [LocalBelowSubTabRow] mit ~3,5 mm Abstand direkt dran.
 *
 * Frank-Wunsch 2026-06-23 (zweite Iteration): Tab-Leiste hoeher (32 dp ≈ 8 mm) + Schrift groesser
 * (13 sp statt 10 sp). Zusaetzlich ein sliding Indikator-Hintergrund der beim Tab-Wechsel smooth
 * zur ausgewaehlten Position gleitet (Scroll-Effekt, animateFloatAsState 250 ms).
 *
 * - Unter der Status Bar (Kamera, Uhrzeit, etc.)
 * - Nur die Tab-Leiste; kein Titel/Actions-Kopf mehr darueber
 * - Alle Tabs gleich breit
 * - Abgerundete Ecken links/rechts
 * - Trennlinien zwischen den Tabs
 * - Aktivierter Tab: Hintergrundfarbe des Hauptreiters + sliding Indikator
 */
@Composable
fun SubTabRow(
    tabs: List<String>,
    selectedIndex: Int,
    tabColor: Color,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    // Sliding Indikator: gleitet smooth zur ausgewaehlten Tab-Position (Scroll-Effekt).
    val animatedIndex by
        animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = tween(durationMillis = 250),
            label = "tabSlide",
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = statusBarPadding.calculateTopPadding())
    ) {
        // Tab-Leiste — sitzt direkt unter der Status-Bar. Unten 9 dp (≈3,5 mm) Abstand zum
        // darunterliegenden Screen-Kopf. BoxWithConstraints wrappt den Inhalt (feste Hoehe kommt
        // vom inneren Row + Indikator), damit nichts ueber den Screen waechst.
        BoxWithConstraints(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = SUB_TAB_BOTTOM_PADDING)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cosmos.barBg.copy(alpha = 0.95f)),
        ) {
            val tabWidth = maxWidth / tabs.size

            // Sliding Indikator-Hintergrund (hinter den Tab-Texten, gleitet zur Position).
            // Feste Hoehe statt fillMaxHeight — sonst waechst der Indikator ueber den Screen.
            // Performance-Fix 2026-07-03: Lambda-offset statt offset(x=) — der animierte Wert
            // wird erst in der Layout-Phase gelesen. Vorher recomposete die gesamte SubTabRow
            // in JEDEM Frame der 250-ms-Slide-Animation (parallel zum Pager-Swipe → Ruckeln).
            Box(
                modifier =
                    Modifier.width(tabWidth)
                        .height(SUB_TAB_HEIGHT)
                        .padding(horizontal = 3.dp, vertical = 3.dp)
                        .offset { IntOffset(x = (tabWidth * animatedIndex).roundToPx(), y = 0) }
                        .clip(RoundedCornerShape(9.dp))
                        .background(tabColor.copy(alpha = 0.3f)),
            )

            // Tab-Texte (transparent, liegen ueber dem Indikator)
            Row(
                modifier = Modifier.fillMaxWidth().height(SUB_TAB_HEIGHT),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEachIndexed { index, tab ->
                    val isSelected = index == selectedIndex

                    // Animierte Textfarbe
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) tabColor else cosmos.textSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "tabTextColor",
                    )

                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .height(SUB_TAB_HEIGHT)
                                .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab,
                            style =
                                MaterialTheme.typography.labelMedium.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Trennlinie zwischen den Tabs (nicht nach dem letzten)
                    if (index < tabs.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.height(20.dp).width(1.dp),
                            color = cosmos.textSecondary.copy(alpha = 0.2f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Liefert die Unterreiter-Labels fuer einen Hauptreiter.
 *
 * Frank-Wunsch 2026-06-24: Der Hauptreiter-Name steht IMMER ganz vorne (Index 0).
 * Beim Klick auf einen Hauptreiter in der Bottom-Bar oeffnet sich somit sofort der
 * passende Haupt-Sub-Screen und der zugehoerige Label ist der erste in der Sub-Leiste.
 * Die restlichen Sub-Slots folgen in ihrer logischen Reihenfolge.
 */
fun subTabsFor(parentTab: String): List<String> =
    when (parentTab) {
        Routes.TASKS -> listOf("Aufgaben", "Gewohnheit", "Mental", "Ideen")
        Routes.ANALYSIS -> listOf("Analyse", "2", "3", "4")
        Routes.SCIENTIST -> listOf("Lernen", "Entropie", "Thesen", "Journal")
        Routes.BIOMARKER -> listOf("Biomarker", "2", "3", "4")
        else -> emptyList()
    }

/**
 * Liefert den Index des Hauptreiters als ersten Unterreiter.
 * (Der Hauptreiter ist immer der erste Unterreiter — Index 0.)
 */
fun initialSubIndex(parentTab: String): Int = 0
