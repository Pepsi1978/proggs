package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Wiederverwendbarer Glas-Container — entspricht dem Design der Referenzbilder.
 * - Dark: weisses Overlay alpha 0.04, weisser Border alpha 0.08
 * - Light: weiss alpha 0.80, schwarzer Border alpha 0.08
 *
 * PERFORMANCE 2026-05-09: clip() entfernt — `background(color, shape)` und
 * `border(stroke, shape)` zeichnen ihre Form direkt ohne separaten GraphicsLayer.
 * Inhalte mit padding(16.dp) und maxLines koennen den Rand visuell nicht
 * ueberschreiten, daher ist clip() rein performance-relevant. Beim Aufgaben-
 * Bereich werden ~10 GlassCards gleichzeitig sichtbar — vorher 10 zusaetzliche
 * Layer fuer Clipping, jetzt 0. Die Shape wird via remember gecacht.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    tintColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val borderStroke = remember(cosmos.glassBorder) { BorderStroke(1.dp, cosmos.glassBorder) }
    // Frank-Wunsch 2026-05-10 (zweite Iteration): Bucket-Toenung NICHT mehr als
    // gleichmaessige Flaeche, sondern als diagonaler Verlauf von OBEN-LINKS
    // (transparent — glassBg scheint hell durch) nach UNTEN-RECHTS (volle Toenung).
    // Damit ist das Orange/Gelb/Gruen/Blau in der oberen Card-Haelfte praktisch
    // unsichtbar und nur unten rechts schwach erkennbar — die Karten wirken viel
    // heller und der Tint stoert nicht mehr. Offset.Zero = top-left,
    // Offset.Infinite wird auf die Box-Groesse gemappt = bottom-right.
    val tintBrush = remember(tintColor) {
        tintColor?.let { color ->
            Brush.linearGradient(
                colors = listOf(Color.Transparent, color),
                start = Offset.Zero,
                end = Offset.Infinite,
            )
        }
    }
    val tintModifier = if (tintBrush != null) Modifier.background(tintBrush, shape) else Modifier
    Box(
        modifier = modifier
            .background(cosmos.glassBg, shape)
            .then(tintModifier)
            .border(borderStroke, shape)
            .padding(contentPadding),
    ) {
        content()
    }
}
