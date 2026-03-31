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
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonMagenta
import com.bestjournal.app.ui.theme.NeonViolet
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val speedX: Float,
    val speedY: Float,
    val phase: Float
)

@Composable
fun ParticleBackground(
    modifier: Modifier = Modifier,
    particleCount: Int = 40
) {
    val colors = listOf(
        NeonCyan.copy(alpha = 0.15f),
        NeonViolet.copy(alpha = 0.12f),
        NeonMagenta.copy(alpha = 0.10f)
    )

    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 2f + 1.5f,
                color = colors[it % colors.size],
                speedX = (Random.nextFloat() - 0.5f) * 0.3f,
                speedY = (Random.nextFloat() - 0.5f) * 0.3f,
                phase = Random.nextFloat() * 6.28f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val offsetX = sin(time + particle.phase) * particle.speedX * size.width * 0.1f
            val offsetY = cos(time + particle.phase * 1.3f) * particle.speedY * size.height * 0.1f

            val x = (particle.x * size.width + offsetX) % size.width
            val y = (particle.y * size.height + offsetY) % size.height

            drawCircle(
                color = particle.color,
                radius = particle.radius * density,
                center = Offset(
                    x.coerceIn(0f, size.width),
                    y.coerceIn(0f, size.height)
                )
            )
        }
    }
}
