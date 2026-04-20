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
import kotlin.random.Random

private data class Particle(
    val startX: Float,
    val startY: Float,
    val radius: Float,
    val baseColor: Color,
    val phaseOffset: Float,
    val speedMultiplier: Float,
)

@Composable
fun ParticleBackground(modifier: Modifier = Modifier, particleCount: Int = 15) {
    // Base colors without pre-applied alpha — alpha is now driven by lifecycle curve.
    val colors = remember { listOf(NeonCyan, NeonViolet, NeonMagenta) }

    val particles = remember {
        List(particleCount) {
            Particle(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                radius = Random.nextFloat() * 2f + 1.5f,
                baseColor = colors[it % colors.size],
                phaseOffset = Random.nextFloat() * 1000f,
                speedMultiplier = Random.nextFloat() * 0.5f + 0.75f,
            )
        }
    }

    // Long overall cycle so restarts are not visually perceptible.
    val transition = rememberInfiniteTransition(label = "particles")
    val time by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(60000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "particleTime",
        )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val t = (time + particle.phaseOffset) * particle.speedMultiplier
            // 0..1 within this particle's personal cycle of 100 time units.
            val phase = (t % 100f) / 100f

            // Bell curve: particle is visible only in the middle ~35% of its cycle
            // phase 0.0-0.35: invisible, 0.35-0.5: fade-in, 0.5-0.65: fade-out, 0.65-1.0: invisible
            val lifecycle =
                when {
                    phase < 0.35f -> 0f
                    phase < 0.5f -> (phase - 0.35f) / 0.15f
                    phase < 0.65f -> 1f - (phase - 0.5f) / 0.15f
                    else -> 0f
                }

            if (lifecycle > 0.01f) {
                // Re-seed position per cycle using integer cycle index as noise input,
                // so every time a particle respawns it appears in a new random location.
                val cycleIndex = (t / 100f).toInt()
                val seed = particle.phaseOffset.toInt() + cycleIndex * 31
                val rng = Random(seed)
                val cx = rng.nextFloat() * size.width
                val cy = rng.nextFloat() * size.height

                // Peak alpha 0.12 (matches original look); lifecycle drives fade-in/out.
                val alpha = lifecycle * 0.12f
                drawCircle(
                    color = particle.baseColor.copy(alpha = alpha),
                    radius = particle.radius * density,
                    center = Offset(cx, cy),
                )
            }
        }
    }
}
