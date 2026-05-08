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
    content: @Composable () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val borderStroke = remember(cosmos.glassBorder) { BorderStroke(1.dp, cosmos.glassBorder) }
    Box(
        modifier = modifier
            .background(cosmos.glassBg, shape)
            .border(borderStroke, shape)
            .padding(contentPadding),
    ) {
        content()
    }
}
