package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Frank-Wunsch 2026-05-18: optionaler Hintergrund-Override fuer alle `GlassCard`-Instanzen in einem
 * Composition-Subtree. Wird vom Biomarker- Screen pro Karten-Item via `CompositionLocalProvider`
 * gesetzt — die Cards selbst muessen nichts wissen, sie picken die Farbe automatisch auf.
 *
 * `null` bedeutet "Standard-Hintergrund" (cosmos.glassBg).
 */
val LocalCardBackgroundOverride = compositionLocalOf<Color?> { null }

/**
 * Wiederverwendbarer Glas-Container — entspricht dem Design der Referenzbilder.
 * - Dark: weisses Overlay alpha 0.04, weisser Border alpha 0.08
 * - Light: weiss alpha 0.80, schwarzer Border alpha 0.08
 *
 * PERFORMANCE 2026-05-09: clip() entfernt — `background(color, shape)` und `border(stroke, shape)`
 * zeichnen ihre Form direkt ohne separaten GraphicsLayer. Inhalte mit padding(16.dp) und maxLines
 * koennen den Rand visuell nicht ueberschreiten, daher ist clip() rein performance-relevant. Beim
 * Aufgaben- Bereich werden ~10 GlassCards gleichzeitig sichtbar — vorher 10 zusaetzliche Layer fuer
 * Clipping, jetzt 0. Die Shape wird via remember gecacht.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    tintColor: Color? = null,
    /**
     * Frank-Wunsch 2026-05-31: optionaler, fertig konstruierter Farbverlauf als Toenungs-Overlay.
     * Hat Vorrang vor `tintColor`. Wird von der Aufgaben-Kachel genutzt, um einen echten
     * Links→Rechts-Verlauf (links dezent, rechts volle Prioritaetsfarbe) zu zeichnen statt der
     * Standard-Diagonal-Toenung.
     */
    tintBrush: Brush? = null,
    /**
     * Frank-Wunsch 2026-05-18: optionale Hintergrundfarbe pro Karte. Wenn gesetzt, ersetzt sie die
     * Standard-Hintergrundfarbe (`cosmos.glassBg`) vollstaendig — anders als `tintColor`, das nur
     * einen Gradient-Overlay macht. Wird von Card-Color-System verwendet damit Frank einzelne
     * Patterns optisch markieren kann.
     */
    backgroundOverride: Color? = null,
    content: @Composable () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    val borderStroke = remember(cosmos.glassBorder) { BorderStroke(1.dp, cosmos.glassBorder) }
    val baseBackground = backgroundOverride ?: LocalCardBackgroundOverride.current ?: cosmos.glassBg
    // Frank-Wunsch 2026-05-10 (dritte Iteration): Verlauf-Richtung umgedreht —
    // Toenung ist jetzt OBEN RECHTS am staerksten, nach UNTEN LINKS verblasst sie
    // zu transparent. Frank wollte den Farbeffekt OBEN sehen (nicht unten), damit
    // beim Blick auf die Card der Farbton (Rosa/Gelb/Gruen/Blau) sofort klar ist.
    //
    // Gradient-Achse: bottom-left (transparent) → top-right (volle Toenung).
    // Compose mappt Float.POSITIVE_INFINITY auf die jeweilige Box-Dimension, also
    // (0f, ∞) = bottom-left und (∞, 0f) = top-right.
    val diagonalTintBrush =
        remember(tintColor) {
            tintColor?.let { color ->
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, color),
                    start = Offset(0f, Float.POSITIVE_INFINITY),
                    end = Offset(Float.POSITIVE_INFINITY, 0f),
                )
            }
        }
    // tintBrush (explizit uebergeben) hat Vorrang vor der Standard-Diagonal-Toenung.
    val effectiveTintBrush = tintBrush ?: diagonalTintBrush
    val tintModifier =
        if (effectiveTintBrush != null) Modifier.background(effectiveTintBrush, shape) else Modifier
    Box(
        modifier =
            modifier
                .background(baseBackground, shape)
                .then(tintModifier)
                .border(borderStroke, shape)
                .padding(contentPadding)
    ) {
        content()
    }
}
