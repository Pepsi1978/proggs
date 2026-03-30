package com.entropyjournal.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonMagenta
import com.entropyjournal.ui.theme.NeonViolet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private data class SplashParticle(
    val angle: Float,        // start angle on outer circle (radians)
    val distance: Float,     // start distance from center (normalized)
    val spiralSpeed: Float,  // spiral rotation speed
    val size: Float,         // particle radius
    val color: Color,
    val trailCount: Int      // number of trail dots
)

@Composable
fun SplashScreen(
    onSplashFinished: (isSignedIn: Boolean) -> Unit,
    viewModel: SplashViewModel
) {
    // Animation state
    val convergence = remember { Animatable(0f) }    // 0 = particles at edge, 1 = at center
    val textScale = remember { Animatable(0f) }       // text scale for 3D bounce
    val textOffsetY = remember { Animatable(0f) }     // vertical offset (dp) for jump
    val textAlpha = remember { Animatable(0f) }       // text visibility
    val impactRing = remember { Animatable(0f) }      // shockwave ring expansion
    val particleAlpha = remember { Animatable(1f) }   // particle fade-out
    val glowAlpha = remember { Animatable(0f) }       // impact glow flash

    // Particles evenly distributed around a circle (not rectangular!)
    val particles = remember {
        val colors = listOf(NeonCyan, NeonViolet, NeonMagenta)
        List(150) { i ->
            SplashParticle(
                // Evenly spread around 360° with slight random jitter
                angle = (i.toFloat() / 150f) * 2f * PI.toFloat() + Random.nextFloat() * 0.4f,
                // Start beyond screen edge
                distance = 0.7f + Random.nextFloat() * 0.5f,
                spiralSpeed = 1.5f + Random.nextFloat() * 3f,
                size = 1.5f + Random.nextFloat() * 2.5f,
                color = colors[i % 3].copy(alpha = 0.5f + Random.nextFloat() * 0.5f),
                trailCount = 2 + Random.nextInt(4)
            )
        }
    }

    LaunchedEffect(Unit) {
        // Phase 1: Particles spiral inward from circular edge (1800ms)
        convergence.animateTo(1f, tween(1800, easing = FastOutSlowInEasing))

        // Phase 2: Fade particles + text JUMPS UP big (3D spring up)
        launch { particleAlpha.animateTo(0f, tween(300)) }
        launch { textAlpha.animateTo(1f, tween(150)) }
        launch { textOffsetY.animateTo(-120f, tween(350, easing = FastOutSlowInEasing)) }
        textScale.animateTo(1.9f, tween(350, easing = FastOutSlowInEasing))

        // Phase 3: SLAM DOWN with impact — text falls back, shockwave expands
        launch { textOffsetY.animateTo(15f, tween(200)) }
        launch { impactRing.animateTo(1f, tween(600)) }
        launch {
            glowAlpha.animateTo(0.7f, tween(80))
            glowAlpha.animateTo(0f, tween(400))
        }
        textScale.animateTo(0.82f, tween(200))

        // Phase 4: Bounce settle — spring physics for natural feel
        launch { textOffsetY.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 350f)) }
        textScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 350f))

        delay(400)
        onSplashFinished(viewModel.isUserSignedIn())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Particle + effect canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = sqrt(centerX * centerX + centerY * centerY)
            val progress = convergence.value
            val pAlpha = particleAlpha.value

            // Draw spiraling particles
            if (pAlpha > 0.01f) {
                particles.forEach { particle ->
                    val currentDist = particle.distance * (1f - progress) * maxRadius
                    val currentAngle = particle.angle + progress * particle.spiralSpeed * PI.toFloat()

                    val x = centerX + cos(currentAngle.toDouble()).toFloat() * currentDist
                    val y = centerY + sin(currentAngle.toDouble()).toFloat() * currentDist
                    val pSize = particle.size * (0.5f + 0.5f * progress) * density

                    // Trailing dots for motion blur effect
                    for (t in 1..particle.trailCount) {
                        val trailProg = (progress - t * 0.015f).coerceAtLeast(0f)
                        val trailDist = particle.distance * (1f - trailProg) * maxRadius
                        val trailAngle = particle.angle + trailProg * particle.spiralSpeed * PI.toFloat()
                        val tx = centerX + cos(trailAngle.toDouble()).toFloat() * trailDist
                        val ty = centerY + sin(trailAngle.toDouble()).toFloat() * trailDist
                        val trailA = (1f - t.toFloat() / particle.trailCount) * 0.3f * pAlpha

                        drawCircle(
                            color = particle.color.copy(alpha = trailA),
                            radius = pSize * (1f - t * 0.15f).coerceAtLeast(0.3f),
                            center = Offset(tx, ty)
                        )
                    }

                    // Main particle
                    drawCircle(
                        color = particle.color.copy(alpha = particle.color.alpha * pAlpha),
                        radius = pSize,
                        center = Offset(x, y)
                    )
                }
            }

            // Impact glow flash behind text
            val gA = glowAlpha.value
            if (gA > 0.01f) {
                drawCircle(
                    color = NeonCyan.copy(alpha = gA * 0.25f),
                    radius = 140f * density,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = NeonViolet.copy(alpha = gA * 0.15f),
                    radius = 90f * density,
                    center = Offset(centerX, centerY)
                )
            }

            // Expanding shockwave ring on impact
            val ringProg = impactRing.value
            if (ringProg in 0.01f..0.99f) {
                val ringRadius = ringProg * maxRadius * 0.35f
                val ringA = (1f - ringProg) * 0.5f
                drawCircle(
                    color = NeonCyan.copy(alpha = ringA),
                    radius = ringRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = (4f * (1f - ringProg) * density))
                )
                // Second thinner ring slightly behind
                val ring2Prog = (ringProg - 0.08f).coerceAtLeast(0f)
                if (ring2Prog > 0.01f) {
                    val ring2Radius = ring2Prog * maxRadius * 0.35f
                    val ring2A = (1f - ring2Prog) * 0.25f
                    drawCircle(
                        color = NeonViolet.copy(alpha = ring2A),
                        radius = ring2Radius,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = (2f * (1f - ring2Prog) * density))
                    )
                }
            }
        }

        // "Best Journal" text — two lines, centered, with 3D bounce
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = textScale.value
                scaleY = textScale.value
                translationY = textOffsetY.value * density
                shadowElevation = textScale.value * 20f
            }
        ) {
            Text(
                text = "Best",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value)
            )
            Text(
                text = "Journal",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value)
            )
        }
    }
}
