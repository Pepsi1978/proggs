package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * 30 helle Farbtoene fuer den Frank-Wunsch 2026-05-18: Jede Biomarker-Karte
 * (und spaeter auch Trainings-Hero, Resilienz, Gewicht usw.) bekommt eine
 * individuelle Hintergrundfarbe — damit Frank die Karten optisch ordnen kann.
 *
 * Die Auswahl ist bewusst hell und pastellig damit der Text (in cosmos.textPrimary
 * Dark-Mode-Hell oder Light-Mode-Dunkel) auf jeder Karte lesbar bleibt. Index 0
 * ist die Default-Karte (Standardweiß) — wer keine Farbe auswaehlt, behaelt das
 * urspruengliche Design.
 */
val LIGHT_CARD_COLORS: List<Color> =
    listOf(
        Color(0xFFFFFFFF), // 0 — Standard (Weiß = kein Override)
        Color(0xFFFFF8E1), // 1 — pastellgelb
        Color(0xFFFCE4EC), // 2 — pastellrosa
        Color(0xFFE3F2FD), // 3 — pastellblau
        Color(0xFFE8F5E9), // 4 — pastellgruen
        Color(0xFFFFF3E0), // 5 — pastellorange
        Color(0xFFEDE7F6), // 6 — pastellviolett
        Color(0xFFE0F7FA), // 7 — pastelltuerkis
        Color(0xFFF3E5F5), // 8 — pastellflieder
        Color(0xFFFFEBEE), // 9 — pastellrot
        Color(0xFFE0F2F1), // 10 — pastellpetrol
        Color(0xFFFFFDE7), // 11 — pastellzitrone
        Color(0xFFF1F8E9), // 12 — pastelllime
        Color(0xFFFBE9E7), // 13 — pastellkoralle
        Color(0xFFE8EAF6), // 14 — pastellindigo
        Color(0xFFF9FBE7), // 15 — pastellchartreuse
        Color(0xFFFEF7E1), // 16 — champagner
        Color(0xFFE7F5FE), // 17 — himmelblau
        Color(0xFFFFEFD5), // 18 — pfirsich
        Color(0xFFE6FFE6), // 19 — honigtau
        Color(0xFFF0FFF0), // 20 — mintgruen
        Color(0xFFFFF0F5), // 21 — lavendelrose
        Color(0xFFFFF5EE), // 22 — muschelweiss
        Color(0xFFFAFAD2), // 23 — hellgoldenrute
        Color(0xFFE0FFFF), // 24 — hellcyan
        Color(0xFFFFE4E1), // 25 — nebelrose
        Color(0xFFF0F8FF), // 26 — alice-blau
        Color(0xFFF5F5DC), // 27 — beige
        Color(0xFFFAFAFA), // 28 — sehr helles grau
        Color(0xFFFFFAF0), // 29 — pastell-elfenbein
    )

/**
 * Mappt einen gespeicherten Farb-Index auf die zugehoerige [Color] aus der
 * [LIGHT_CARD_COLORS]-Palette. Index 0 oder ausserhalb der Range → null,
 * was bedeutet "kein Override, Standard-Hintergrund nutzen".
 */
fun cardColorOverrideForIndex(index: Int?): Color? {
    if (index == null || index <= 0 || index >= LIGHT_CARD_COLORS.size) return null
    return LIGHT_CARD_COLORS[index]
}

/**
 * Farb-Palette-Bar, horizontal scrollbar. Zeigt alle 30 Toene als runde
 * Kreise — der aktuell ausgewaehlte Kreis hat einen kraeftigeren Border.
 * Klick auf einen Kreis ruft [onPick] mit dem Farb-Index auf.
 *
 * Frank-Wunsch 2026-05-18: Wird oben im Detail-Sheet jedes Patterns
 * eingebaut. Auswahl wirkt sich beim Zurueck-Navigieren auf den
 * Karten-Hintergrund im Uebersichts-Screen aus.
 */
@Composable
fun ColorPaletteBar(
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Hintergrundfarbe für diese Karte",
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cosmos.glassBg)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LIGHT_CARD_COLORS.forEachIndexed { idx, color ->
                val isSelected = idx == selectedIndex
                Box(
                    modifier =
                        Modifier.size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) cosmos.textPrimary else cosmos.glassBorder,
                                shape = CircleShape,
                            )
                            .clickable { onPick(idx) },
                )
            }
        }
    }
}
