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
    val size: Float,
    val delay: Float,
    val speed: Float
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
                size = Random.nextFloat() * 3f + 2f,
                delay = Random.nextFloat() * 6.28f,
                speed = Random.nextFloat() * 4000f + 5000f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "stars")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starTime"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        stars.forEach { star ->
            val phase = (time * (12000f / star.speed) + star.delay) % 6.28f
            val brightness = ((sin(phase.toDouble()) + 1.0) / 2.0)
            val alpha = (brightness * brightness * 0.8f).toFloat()

            if (alpha > 0.05f) {
                val cx = star.x * size.width
                val cy = star.y * size.height
                drawStar(
                    center = Offset(cx, cy),
                    size = star.size * density,
                    color = Color.White.copy(alpha = alpha)
                )
            }
        }
    }
}

private fun DrawScope.drawStar(center: Offset, size: Float, color: Color) {
    val path = Path()
    val points = 4
    val outerRadius = size
    val innerRadius = size * 0.4f

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
