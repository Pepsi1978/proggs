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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Kompakte Farb-Palette-Zeile: links das Label "Hintergrundfarbe", rechts
 * EIN Farbsymbol das die aktuelle Auswahl zeigt. Klick auf das Symbol oeffnet
 * einen Dialog mit allen 30 Farben in einem Grid.
 *
 * Frank-Wunsch 2026-05-18 (Folgeauftrag 3): Vorher war die ganze Palette
 * permanent als horizontal scrollbare Leiste sichtbar — Frank wollte ein
 * unauffaelliges Symbol stattdessen, das auf Klick ein kleines Auswahlfenster
 * oeffnet.
 */
@Composable
fun ColorPaletteBar(
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    var dialogOpen by remember { mutableStateOf(false) }
    val currentColor =
        cardColorOverrideForIndex(selectedIndex) ?: cosmos.glassBg

    // Frank-Wunsch 2026-05-18 (Folgeauftrag 4): Label + Farbsymbol gehoeren
    // visuell zusammen — beide bündig rechts an der Kante. Vorher war das
    // Label linksbündig und das Symbol rechts: man konnte nicht sofort
    // erkennen dass die Beschriftung zum Symbol gehoert.
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = "Hintergrundfarbe",
            style = MaterialTheme.typography.labelMedium,
            color = cosmos.textSecondary,
        )
        Spacer(Modifier.size(10.dp))
        Box(
            modifier =
                Modifier.size(32.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(
                        width = 1.5.dp,
                        color = cosmos.glassBorder,
                        shape = CircleShape,
                    )
                    .clickable { dialogOpen = true },
        )
    }

    if (dialogOpen) {
        ColorPaletteDialog(
            selectedIndex = selectedIndex,
            onPick = { idx ->
                onPick(idx)
                dialogOpen = false
            },
            onDismiss = { dialogOpen = false },
        )
    }
}

/**
 * Auswahl-Dialog: alle 30 Farben in einem 5-Spalten-Grid. Tap auf eine Farbe
 * waehlt sie aus und schliesst den Dialog automatisch.
 */
@Composable
private fun ColorPaletteDialog(
    selectedIndex: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hintergrundfarbe waehlen") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier =
                    Modifier.fillMaxWidth()
                        .heightIn(min = 240.dp, max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(count = LIGHT_CARD_COLORS.size, key = { it }) { idx ->
                    val color = LIGHT_CARD_COLORS[idx]
                    val isSelected = idx == selectedIndex
                    Box(
                        modifier =
                            Modifier.size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color =
                                        if (isSelected) cosmos.textPrimary
                                        else cosmos.glassBorder,
                                    shape = CircleShape,
                                )
                                .clickable { onPick(idx) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schliessen") }
        },
    )
}

