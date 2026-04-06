package com.entropyjournal.ui.components

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
    val delay: Float,
    val cycleDuration: Float
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
                maxSize = Random.nextFloat() * 2.5f + 2f,
                delay = Random.nextFloat() * 6.28f,
                cycleDuration = Random.nextFloat() * 6000f + 7000f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "stars")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { star ->
            val phase = (time * (18000f / star.cycleDuration) + star.delay) % 6.28f

            // Smooth sine curve: 0 → 1 → 0 over the cycle
            // Only visible for ~40% of the cycle (rest is dark/invisible)
            val raw = sin(phase.toDouble()).toFloat()
            val lifecycle = (raw * 2.5f).coerceIn(0f, 1f)

            if (lifecycle > 0.01f) {
                val cx = star.x * size.width
                val cy = star.y * size.height

                // Size grows from 0 to maxSize and shrinks back
                val currentSize = star.maxSize * lifecycle * density

                // Alpha follows the same curve: dark → bright → dark
                val alpha = lifecycle * 0.75f

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
    if (size < 0.5f) return
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
