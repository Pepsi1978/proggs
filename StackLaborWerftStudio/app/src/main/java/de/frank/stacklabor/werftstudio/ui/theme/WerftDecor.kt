package de.frank.stacklabor.werftstudio.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shared visual language of StackLabor: metal rims, bevelled surfaces and the
 * depth shadows every raised element uses.
 *
 * Everything here is derived from the current palette, so the same code produces a
 * gold rim in the light theme and the matching accent metal in the dark themes.
 */

/** Warm brown the drop shadows are tinted with — a neutral grey would look flat next to gold. */
private val ShadowTint = Color(0xFF3A2405)

fun Color.lightenBy(fraction: Float): Color = lerp(this, Color.White, fraction.coerceIn(0f, 1f))

fun Color.darkenBy(fraction: Float): Color = lerp(this, Color.Black, fraction.coerceIn(0f, 1f))

/**
 * A continuous metal rim.
 *
 * The previous rim used a sweep gradient that faded to fully transparent, which made the
 * border bright on one side and invisible on the other. Every stop here is opaque, so the
 * rim runs all the way around and only changes its brightness like brushed metal does.
 */
@Composable
fun metalRim(alpha: Float = 1f): Brush {
    val accent = StackLaborTheme.colors.accent
    return Brush.linearGradient(
        0.00f to accent.lightenBy(0.55f).copy(alpha = alpha),
        0.22f to accent.lightenBy(0.12f).copy(alpha = alpha),
        0.46f to accent.darkenBy(0.18f).copy(alpha = alpha),
        0.62f to accent.lightenBy(0.42f).copy(alpha = alpha),
        0.84f to accent.darkenBy(0.28f).copy(alpha = alpha),
        1.00f to accent.lightenBy(0.30f).copy(alpha = alpha),
    )
}

/** Slightly quieter rim for rows that should not compete with the stack cards. */
@Composable
fun softMetalRim(alpha: Float = 0.75f): Brush = metalRim(alpha)

/** Raised card body: lit from the top, settling into the elevated tone at the bottom. */
@Composable
fun raisedSurface(): Brush {
    val colors = StackLaborTheme.colors
    return Brush.verticalGradient(
        listOf(
            colors.surface.lightenBy(if (StackLaborTheme.dark) 0.06f else 0.35f),
            colors.surface,
            colors.elevated.copy(alpha = 0.55f),
        ),
    )
}

/** Gold face of every primary button, lit from above so it reads as a physical key. */
@Composable
fun goldActionSurface(): Brush = Brush.verticalGradient(
    listOf(
        Color(0xFFF6E0AA),
        Color(0xFFD9AC50),
        Color(0xFFA87523),
        Color(0xFF6E430F),
    ),
)

/**
 * Bevel drawn on top of the background but underneath the text: a bright lip along the
 * upper edge and a soft shade along the lower one. That pair is what makes a flat
 * rectangle look raised.
 */
fun Modifier.bevel(strength: Float = 1f, glossHeight: Float = 0.42f): Modifier = drawBehind {
    drawRect(
        Brush.verticalGradient(
            0.00f to Color.White.copy(alpha = 0.18f * strength),
            glossHeight to Color.Transparent,
            0.86f to Color.Transparent,
            1.00f to Color.Black.copy(alpha = 0.13f * strength),
        ),
    )
    val line = 1.dp.toPx()
    drawLine(Color.White.copy(alpha = 0.30f * strength), Offset(0f, line / 2f), Offset(size.width, line / 2f), line)
    drawLine(
        Color.Black.copy(alpha = 0.16f * strength),
        Offset(0f, size.height - line / 2f),
        Offset(size.width, size.height - line / 2f),
        line,
    )
}

/** Diagonal sheen across the upper half — used on the gold buttons only. */
fun Modifier.sheen(): Modifier = drawBehind {
    drawRect(
        Brush.linearGradient(
            0.00f to Color.White.copy(alpha = 0.34f),
            0.34f to Color.White.copy(alpha = 0.06f),
            0.52f to Color.Transparent,
            1.00f to Color.Black.copy(alpha = 0.10f),
            start = Offset(0f, 0f),
            end = Offset(size.width * 0.7f, size.height),
        ),
    )
}

/** The one drop shadow recipe: warm, tinted, and stronger the higher the element sits. */
fun Modifier.depthShadow(shape: Shape, elevation: Dp, strength: Float = 1f): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = ShadowTint.copy(alpha = 0.34f * strength),
    spotColor = ShadowTint.copy(alpha = 0.30f * strength),
)

/**
 * Presses the element into the surface: it shrinks a little and its shadow collapses,
 * so a tap feels like pushing a real key. Falls back to no motion when the user asked
 * for reduced motion.
 */
@Composable
fun Modifier.pressDepth(
    interactionSource: InteractionSource,
    shape: Shape,
    restingElevation: Dp,
    strength: Float = 1f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = StackLaborTheme.motionEnabled
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.972f else 1f,
        animationSpec = tween(if (motion) 110 else 0),
        label = "pressScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) restingElevation * 0.28f else restingElevation,
        animationSpec = tween(if (motion) 110 else 0),
        label = "pressElevation",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .depthShadow(shape, elevation, strength)
}
