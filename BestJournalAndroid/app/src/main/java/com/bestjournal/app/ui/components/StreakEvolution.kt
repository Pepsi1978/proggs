package com.bestjournal.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// Streak level colors — evolve from warm orange to intense red
private val FlameOrange = Color(0xFFFF8C00)
private val FlameDarkOrange = Color(0xFFFF6D00)
private val FlameOrangeRed = Color(0xFFFF4500)
private val FlameRed = Color(0xFFFF1744)
private val ParticleOrange = Color(0xFFFFAB40)

/**
 * A flame icon that visually evolves with the streak level.
 * - Days 1-6: Small orange flame, no glow
 * - Days 7-29: Medium flame with subtle glow
 * - Days 30-89: Large flame with pulsing glow
 * - Days 90+: Extra large flame with pulsing glow and orbiting particles
 */
@Composable
fun EvolvingStreakIcon(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val baseOpacityMultiplier = if (isDark) 1f else 0.65f

    when {
        streakDays >= 90 -> StreakLevel4Icon(isDark, baseOpacityMultiplier, modifier)
        streakDays >= 30 -> StreakLevel3Icon(isDark, baseOpacityMultiplier, modifier)
        streakDays >= 7 -> StreakLevel2Icon(isDark, baseOpacityMultiplier, modifier)
        else -> StreakLevel1Icon(modifier)
    }
}

@Composable
private fun StreakLevel1Icon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Rounded.Whatshot,
        contentDescription = null,
        tint = FlameOrange,
        modifier = modifier.size(40.dp),
    )
}

@Composable
private fun StreakLevel2Icon(
    isDark: Boolean,
    opacityMultiplier: Float,
    modifier: Modifier = Modifier,
) {
    val glowAlpha = 0.25f * opacityMultiplier
    val glowColor = FlameDarkOrange.copy(alpha = glowAlpha)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(60.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Whatshot,
            contentDescription = null,
            tint = FlameDarkOrange,
            modifier = Modifier
                .size(48.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(glowColor, Color.Transparent),
                            radius = 60.dp.toPx(),
                        ),
                        radius = 60.dp.toPx(),
                    )
                },
        )
    }
}

@Composable
private fun StreakLevel3Icon(
    isDark: Boolean,
    opacityMultiplier: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_pulse_3")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f * opacityMultiplier,
        targetValue = 0.6f * opacityMultiplier,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha_3",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(72.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Whatshot,
            contentDescription = null,
            tint = FlameOrangeRed,
            modifier = Modifier
                .size(56.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                FlameOrangeRed.copy(alpha = pulseAlpha),
                                Color.Transparent,
                            ),
                            radius = 80.dp.toPx(),
                        ),
                        radius = 80.dp.toPx(),
                    )
                },
        )
    }
}

@Composable
private fun StreakLevel4Icon(
    isDark: Boolean,
    opacityMultiplier: Float,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "streak_pulse_4")

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f * opacityMultiplier,
        targetValue = 0.6f * opacityMultiplier,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha_4",
    )

    // Orbiting particle angle (full revolution in 4 seconds)
    val particleAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_orbit",
    )

    // Second set of particles offset by 120 degrees, slightly different speed
    val particleAngle2 by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = 480f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_orbit_2",
    )

    val particleAngle3 by infiniteTransition.animateFloat(
        initialValue = 240f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particle_orbit_3",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(88.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Whatshot,
            contentDescription = null,
            tint = FlameRed,
            modifier = Modifier
                .size(64.dp)
                .drawBehind {
                    // Pulsing glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                FlameRed.copy(alpha = pulseAlpha),
                                FlameOrangeRed.copy(alpha = pulseAlpha * 0.5f),
                                Color.Transparent,
                            ),
                            radius = 100.dp.toPx(),
                        ),
                        radius = 100.dp.toPx(),
                    )

                    // Orbiting particles
                    val orbitRadius = 42.dp.toPx()
                    val particleRadius = 3.dp.toPx()
                    val particleColor = ParticleOrange.copy(alpha = 0.8f)

                    drawParticle(particleAngle, orbitRadius, particleRadius, particleColor)
                    drawParticle(particleAngle2, orbitRadius * 0.85f, particleRadius * 0.7f, particleColor.copy(alpha = 0.6f))
                    drawParticle(particleAngle3, orbitRadius * 1.1f, particleRadius * 0.5f, particleColor.copy(alpha = 0.5f))
                },
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawParticle(
    angleDegrees: Float,
    orbitRadius: Float,
    particleRadius: Float,
    color: Color,
) {
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val x = center.x + (orbitRadius * cos(angleRad)).toFloat()
    val y = center.y + (orbitRadius * sin(angleRad)).toFloat()
    drawCircle(
        color = color,
        radius = particleRadius,
        center = Offset(x, y),
    )
}
