package com.quizverse.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Shape variants for particles. */
private enum class ParticleShape { CIRCLE, RECTANGLE, STAR, HEART }

/**
 * Particle data for a single confetti piece.
 *
 * @property x         Fractional X position [0..1] relative to canvas width.
 * @property startY    Fractional starting Y position [-0.3..0] (above the top edge).
 * @property size      Radius/half-size in px.
 * @property color     Fill colour.
 * @property speed     Fall speed multiplier.
 * @property angle     Initial rotation angle in degrees.
 * @property rotSpeed  Rotation speed multiplier.
 * @property shape     Geometric shape of the particle.
 */
private data class Particle(
    val x: Float,
    val startY: Float,
    val size: Float,
    val color: Color,
    val speed: Float,
    val angle: Float,
    val rotSpeed: Float,
    val shape: ParticleShape,
)

/** Premium confetti palette with gold accents. */
private val CONFETTI_COLORS = listOf(
    Color(0xFFFFD700), // Gold
    Color(0xFFFFE57F), // Gold Light
    Color(0xFFF4C430), // Gold Dark
    Color(0xFFFF6B6B), Color(0xFFFFE66D), Color(0xFF4ECDC4),
    Color(0xFF6C63FF), Color(0xFFFF9F43), Color(0xFF48DBFB),
    Color(0xFFFF9FF3), Color(0xFF54A0FF), Color(0xFF5F27CD),
    Color(0xFF00D2D3), Color(0xFFFF6348), Color(0xFFFECA57),
)

/**
 * Draws a 5-pointed star shape on the canvas.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    center: Offset,
    radius: Float,
    color: Color,
    alpha: Float,
) {
    val path = Path()
    val innerRadius = radius * 0.45f
    val angleStep = (PI / 5).toFloat()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = i * angleStep - (PI / 2).toFloat()
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color.copy(alpha = alpha))
}

/**
 * Draws a heart shape on the canvas.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(
    center: Offset,
    size: Float,
    color: Color,
    alpha: Float,
) {
    val path = Path()
    val s = size * 0.9f
    path.moveTo(center.x, center.y + s * 0.4f)
    path.cubicTo(
        center.x - s, center.y - s * 0.2f,
        center.x - s, center.y - s,
        center.x, center.y - s * 0.5f,
    )
    path.cubicTo(
        center.x + s, center.y - s,
        center.x + s, center.y - s * 0.2f,
        center.x, center.y + s * 0.4f,
    )
    path.close()
    drawPath(path = path, color = color.copy(alpha = alpha))
}

/**
 * Full-screen premium confetti particle system drawn on a [Canvas].
 *
 * Particles include circles, rectangles, stars, and hearts with gold accents.
 * They fall from the top with random colours, sizes, and rotations.
 *
 * @param isActive  Start / stop the animation.
 * @param count     Number of confetti pieces (default 120 for dense effect).
 * @param modifier  Modifier (typically [fillMaxSize]).
 */
@Composable
fun ParticleEffect(
    isActive: Boolean,
    count: Int = 120,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    if (!isActive) return

    // Generate particles once per activation
    val particles = remember(isActive) {
        List(count) {
            val shape = when (it % 4) {
                0    -> ParticleShape.CIRCLE
                1    -> ParticleShape.RECTANGLE
                2    -> ParticleShape.STAR
                else -> ParticleShape.HEART
            }
            // Gold particles appear more frequently for premium feel
            val color = if (it % 5 == 0) {
                listOf(Color(0xFFFFD700), Color(0xFFFFE57F), Color(0xFFF4C430)).random()
            } else {
                CONFETTI_COLORS.random()
            }
            Particle(
                x        = Random.nextFloat(),
                startY   = Random.nextFloat() * -0.4f,
                size     = Random.nextFloat() * 14f + 5f,
                color    = color,
                speed    = Random.nextFloat() * 0.55f + 0.35f,
                angle    = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 5f,
                shape    = shape,
            )
        }
    }

    // Master clock: 0 → 1 over the animation duration, then loops
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val clock by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "confettiClock",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val t = ((clock * p.speed + p.startY + 1f) % 1f)
            val baseX = p.x * w
            val y = t * (h + p.size * 4)

            // Gentle horizontal sway
            val sway = sin((t * PI * 4 + p.x * PI).toFloat()) * 20f
            val cx = baseX + sway
            val rotation = p.angle + clock * p.rotSpeed * 360f

            // Fade in/out near edges
            val alpha = when {
                t < 0.05f -> t / 0.05f
                t > 0.88f -> (1f - t) / 0.12f
                else      -> 0.92f
            }

            val center = Offset(cx, y)

            rotate(degrees = rotation, pivot = center) {
                when (p.shape) {
                    ParticleShape.CIRCLE -> drawCircle(
                        color  = p.color.copy(alpha = alpha),
                        radius = p.size,
                        center = center,
                    )
                    ParticleShape.RECTANGLE -> drawRect(
                        color   = p.color.copy(alpha = alpha),
                        topLeft = Offset(cx - p.size / 2f, y - p.size),
                        size    = Size(p.size, p.size * 2),
                    )
                    ParticleShape.STAR -> drawStar(
                        center = center,
                        radius = p.size,
                        color  = p.color,
                        alpha  = alpha,
                    )
                    ParticleShape.HEART -> drawHeart(
                        center = center,
                        size   = p.size,
                        color  = p.color,
                        alpha  = alpha,
                    )
                }
            }
        }
    }
}
