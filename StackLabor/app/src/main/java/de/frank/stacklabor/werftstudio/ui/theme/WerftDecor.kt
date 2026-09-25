package de.frank.stacklabor.werftstudio.ui.theme

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The shared visual language of StackLabor: calm cards with a fine hairline border,
 * soft neutral shadows and one indigo accent surface for primary actions.
 *
 * The helper names still come from the former gold/metal look, so every screen picks up
 * the new style without touching its layout code.
 */

/** Cool blue-grey the drop shadows are tinted with — softer than pure black. */
private val ShadowTint = Color(0xFF1E2A4A)

fun Color.lightenBy(fraction: Float): Color = lerp(this, Color.White, fraction.coerceIn(0f, 1f))

fun Color.darkenBy(fraction: Float): Color = lerp(this, Color.Black, fraction.coerceIn(0f, 1f))

/**
 * The card border: a fine, even hairline in the border tone. A high [alpha] (selected
 * elements) tints it towards the accent so selection stays visible.
 */
@Composable
fun metalRim(alpha: Float = 1f): Brush {
    val colors = StackLaborTheme.colors
    return remember(colors.border, colors.accent, alpha) {
        val base = if (alpha >= 0.99f) lerp(colors.border, colors.accent, 0.55f) else colors.border
        SolidColor(base.copy(alpha = (0.55f + alpha * 0.45f).coerceAtMost(1f)))
    }
}

/** Slightly quieter border for rows that should not compete with the stack cards. */
@Composable
fun softMetalRim(alpha: Float = 0.75f): Brush = metalRim(alpha.coerceAtMost(0.9f))

/** Card body: a flat surface — depth comes from the soft shadow, not from gradients. */
@Composable
fun raisedSurface(): Brush {
    val surface = StackLaborTheme.colors.surface
    return remember(surface) { SolidColor(surface) }
}

/** Face of every primary action: indigo into violet, read with [OnActionColor]. */
@Composable
fun goldActionSurface(): Brush {
    val dark = StackLaborTheme.dark
    return remember(dark) {
        Brush.linearGradient(
            if (dark) listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)) else listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)),
        )
    }
}

/** Former bevel — kept as a very light top highlight so surfaces do not look dull. */
fun Modifier.bevel(strength: Float = 1f, glossHeight: Float = 0.42f): Modifier = drawWithCache {
    val brush = Brush.verticalGradient(
        0.00f to Color.White.copy(alpha = 0.04f * strength),
        glossHeight to Color.Transparent,
    )
    onDrawBehind { drawRect(brush) }
}

/** Soft light across the upper half of action surfaces. */
fun Modifier.sheen(): Modifier = drawWithCache {
    val brush = Brush.verticalGradient(
        0.00f to Color.White.copy(alpha = 0.14f),
        0.55f to Color.Transparent,
    )
    onDrawBehind { drawRect(brush) }
}

/** The one drop shadow recipe: soft, cool, and lower than before so cards float gently. */
fun Modifier.depthShadow(shape: Shape, elevation: Dp, strength: Float = 1f): Modifier = shadow(
    elevation = elevation * 0.45f,
    shape = shape,
    clip = false,
    ambientColor = ShadowTint.copy(alpha = 0.16f * strength),
    spotColor = ShadowTint.copy(alpha = 0.20f * strength),
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
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = tween(if (motion) 110 else 0),
        label = "pressScale",
    )
    val elevation by animateDpAsState(
        targetValue = if (pressed) restingElevation * 0.12f else restingElevation * 0.45f,
        animationSpec = tween(if (motion) 110 else 0),
        label = "pressElevation",
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .graphicsLayer {
            shadowElevation = elevation.toPx()
            this.shape = shape
            clip = false
            ambientShadowColor = ShadowTint.copy(alpha = 0.16f * strength)
            spotShadowColor = ShadowTint.copy(alpha = 0.20f * strength)
        }
}
