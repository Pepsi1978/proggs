package com.bestjournal.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,
    val y: Float,
    val maxSize: Float,
    val phaseOffset: Float,
    val speedMultiplier: Float
)

@Composable
fun TwinklingStars(
    modifier: Modifier = Modifier,
    starCount: Int = 12
) {
    val stars = remember {
        List(starCount) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                maxSize = when (Random.nextInt(3)) {
                    0 -> Random.nextFloat() * 1.2f + 1f    // small distant star
                    1 -> Random.nextFloat() * 1.8f + 1.5f  // medium star
                    else -> Random.nextFloat() * 2.5f + 2f  // large close star
                },
                phaseOffset = Random.nextFloat() * 1000f,
                speedMultiplier = Random.nextFloat() * 0.5f + 0.75f
            )
        }
    }

    // Use a very long cycle (60 seconds) to avoid visible "cut" when restarting.
    // Each star has its own speed multiplier + phase offset so they never sync up.
    val transition = rememberInfiniteTransition(label = "stars")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { star ->
            // Each star has its own independent cycle via phaseOffset + speedMultiplier
            val t = (time + star.phaseOffset) * star.speedMultiplier
            val phase = (t % 100f) / 100f  // 0..1 within this star's personal cycle

            // Bell curve: star is visible only in the middle ~35% of its cycle
            // phase 0.0-0.3: invisible, 0.3-0.5: growing, 0.5-0.7: shrinking, 0.7-1.0: invisible
            val lifecycle = when {
                phase < 0.3f -> 0f
                phase < 0.5f -> (phase - 0.3f) / 0.2f  // 0→1 grow
                phase < 0.7f -> 1f - (phase - 0.5f) / 0.2f  // 1→0 shrink
                else -> 0f
            }

            if (lifecycle > 0.01f) {
                val cx = star.x * size.width
                val cy = star.y * size.height
                val currentSize = star.maxSize * lifecycle * density
                val alpha = lifecycle * 0.7f

                drawStar(
                    center = Offset(cx, cy),
                    size = currentSize,
                    color = Color.White.copy(alpha = alpha)
                )
            }
        }
    }
}

private fun DrawScope.drawStar(center: Offset, size: Float, color: Color) {
    if (size < 0.3f) return
    val path = Path()
    val points = 4
    val outerRadius = size
    val innerRadius = size * 0.35f

    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = Math.PI / 2 + i * Math.PI / points
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y - (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
