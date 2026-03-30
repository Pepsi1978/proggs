package com.entropyjournal.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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

// Main spiral particles — fly inward from all sides
private data class SplashParticle(
    val angle: Float,
    val distance: Float,
    val spiralSpeed: Float,
    val size: Float,
    val color: Color,
    val trailCount: Int,
    val waveAmp: Float,     // wave wobble amplitude
    val waveFreq: Float,    // wave wobble frequency
    val depth: Float        // 3D depth: 0.5 = far/small, 1.5 = close/big
)

// Background rain particles — fall from above for atmosphere
private data class RainParticle(
    val x: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
    val depth: Float
)

@Composable
fun SplashScreen(
    onSplashFinished: (isSignedIn: Boolean) -> Unit,
    viewModel: SplashViewModel
) {
    val convergence = remember { Animatable(0f) }
    val textScale = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val impactRing = remember { Animatable(0f) }
    val particleAlpha = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }

    // 250 spiral particles — evenly distributed around a circle
    val particles = remember {
        val colors = listOf(NeonCyan, NeonViolet, NeonMagenta)
        List(250) { i ->
            val depth = 0.4f + Random.nextFloat() * 1.2f
            SplashParticle(
                angle = (i.toFloat() / 250f) * 2f * PI.toFloat() + Random.nextFloat() * 0.5f,
                distance = 0.6f + Random.nextFloat() * 0.6f,
                spiralSpeed = 1.2f + Random.nextFloat() * 3.5f,
                size = (1f + Random.nextFloat() * 2.5f) * depth,
                color = colors[i % 3].copy(alpha = (0.3f + depth * 0.35f).coerceAtMost(1f)),
                trailCount = 2 + Random.nextInt(5),
                waveAmp = 0.3f + Random.nextFloat() * 0.7f,
                waveFreq = 2f + Random.nextFloat() * 4f,
                depth = depth
            )
        }
    }

    // 80 rain particles — gentle downward drift for atmosphere
    val rainParticles = remember {
        val colors = listOf(NeonCyan, NeonViolet, NeonMagenta)
        List(80) { i ->
            val depth = 0.3f + Random.nextFloat() * 0.8f
            RainParticle(
                x = Random.nextFloat(),
                speed = 0.3f + Random.nextFloat() * 0.7f,
                size = (0.8f + Random.nextFloat() * 1.5f) * depth,
                color = colors[i % 3].copy(alpha = 0.15f + depth * 0.2f),
                depth = depth
            )
        }
    }

    LaunchedEffect(Unit) {
        // Phase 1: Particles spiral inward with wave motion (2200ms)
        convergence.animateTo(1f, tween(2200, easing = FastOutSlowInEasing))

        // Phase 2: Fade particles + text JUMPS UP big
        launch { particleAlpha.animateTo(0f, tween(400)) }
        launch { textAlpha.animateTo(1f, tween(150)) }
        launch { textOffsetY.animateTo(-130f, tween(380, easing = FastOutSlowInEasing)) }
        textScale.animateTo(2.0f, tween(380, easing = FastOutSlowInEasing))

        // Phase 3: SLAM DOWN — no bounce, just impact
        launch { textOffsetY.animateTo(0f, tween(220, easing = FastOutSlowInEasing)) }
        launch { impactRing.animateTo(1f, tween(700)) }
        launch {
            glowAlpha.animateTo(0.8f, tween(60))
            glowAlpha.animateTo(0f, tween(500))
        }
        // Brief squash on landing, then smooth settle to 1.0 — NO spring, NO bounce
        textScale.animateTo(0.85f, tween(180))
        textScale.animateTo(1f, tween(300, easing = FastOutSlowInEasing))

        // Hold visible for 2.5 seconds so user can read "Best Journal"
        delay(2500)
        onSplashFinished(viewModel.isUserSignedIn())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = sqrt(centerX * centerX + centerY * centerY)
            val progress = convergence.value
            val pAlpha = particleAlpha.value

            // --- Layer 1: Background rain (drawn first = behind everything) ---
            if (pAlpha > 0.01f) {
                rainParticles.forEach { rain ->
                    val rainY = (-0.1f + progress * (1.2f + rain.speed)) * size.height
                    val rainX = rain.x * size.width
                    if (rainY in 0f..size.height) {
                        val rSize = rain.size * density
                        // Vertical streak for rain feel
                        drawCircle(
                            color = rain.color.copy(alpha = rain.color.alpha * pAlpha * 0.6f),
                            radius = rSize,
                            center = Offset(rainX, rainY)
                        )
                        // Streak tail
                        drawCircle(
                            color = rain.color.copy(alpha = rain.color.alpha * pAlpha * 0.2f),
                            radius = rSize * 0.6f,
                            center = Offset(rainX, rainY - rSize * 3f)
                        )
                    }
                }
            }

            // --- Layer 2: Far spiral particles (depth < 0.8) ---
            if (pAlpha > 0.01f) {
                particles.filter { it.depth < 0.8f }.forEach { particle ->
                    drawSpiralParticle(particle, progress, pAlpha, centerX, centerY, maxRadius)
                }
            }

            // --- Layer 3: Close spiral particles (depth >= 0.8) ---
            if (pAlpha > 0.01f) {
                particles.filter { it.depth >= 0.8f }.forEach { particle ->
                    drawSpiralParticle(particle, progress, pAlpha, centerX, centerY, maxRadius)
                }
            }

            // --- Impact glow flash ---
            val gA = glowAlpha.value
            if (gA > 0.01f) {
                drawCircle(
                    color = NeonCyan.copy(alpha = gA * 0.2f),
                    radius = 160f * density,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = NeonViolet.copy(alpha = gA * 0.12f),
                    radius = 100f * density,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = NeonMagenta.copy(alpha = gA * 0.08f),
                    radius = 60f * density,
                    center = Offset(centerX, centerY)
                )
            }

            // --- Shockwave rings on impact ---
            val ringProg = impactRing.value
            if (ringProg in 0.01f..0.99f) {
                // Main ring
                val ringRadius = ringProg * maxRadius * 0.4f
                val ringA = (1f - ringProg) * 0.5f
                drawCircle(
                    color = NeonCyan.copy(alpha = ringA),
                    radius = ringRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = (5f * (1f - ringProg) * density))
                )
                // Second ring
                val r2 = (ringProg - 0.06f).coerceAtLeast(0f)
                if (r2 > 0.01f) {
                    drawCircle(
                        color = NeonViolet.copy(alpha = (1f - r2) * 0.3f),
                        radius = r2 * maxRadius * 0.4f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = (3f * (1f - r2) * density))
                    )
                }
                // Third faint ring
                val r3 = (ringProg - 0.14f).coerceAtLeast(0f)
                if (r3 > 0.01f) {
                    drawCircle(
                        color = NeonMagenta.copy(alpha = (1f - r3) * 0.15f),
                        radius = r3 * maxRadius * 0.4f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = (2f * (1f - r3) * density))
                    )
                }
            }
        }

        // "Best Journal" — two lines, centered, 3D bounce
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = textScale.value
                scaleY = textScale.value
                translationY = textOffsetY.value * density
                shadowElevation = textScale.value * 24f
            }
        ) {
            Text(
                text = "Best",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value)
            )
            Text(
                text = "Journal",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value)
            )
        }
    }
}

// Draw a single spiral particle with wave modulation and trail
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpiralParticle(
    particle: SplashParticle,
    progress: Float,
    pAlpha: Float,
    centerX: Float,
    centerY: Float,
    maxRadius: Float
) {
    val currentDist = particle.distance * (1f - progress) * maxRadius
    val currentAngle = particle.angle + progress * particle.spiralSpeed * PI.toFloat()

    // Wave modulation — perpendicular to travel direction
    val waveOffset = sin(progress * particle.waveFreq * PI.toFloat() * 2f).toFloat() *
            particle.waveAmp * (1f - progress) * 40f * density
    val perpAngle = currentAngle + PI.toFloat() / 2f

    val baseX = centerX + cos(currentAngle.toDouble()).toFloat() * currentDist
    val baseY = centerY + sin(currentAngle.toDouble()).toFloat() * currentDist
    val x = baseX + cos(perpAngle.toDouble()).toFloat() * waveOffset
    val y = baseY + sin(perpAngle.toDouble()).toFloat() * waveOffset
    val pSize = particle.size * (0.4f + 0.6f * progress) * density

    // Trail dots
    for (t in 1..particle.trailCount) {
        val trailProg = (progress - t * 0.012f).coerceAtLeast(0f)
        val trailDist = particle.distance * (1f - trailProg) * maxRadius
        val trailAngle = particle.angle + trailProg * particle.spiralSpeed * PI.toFloat()
        val trailWave = sin(trailProg * particle.waveFreq * PI.toFloat() * 2f).toFloat() *
                particle.waveAmp * (1f - trailProg) * 40f * density
        val trailPerp = trailAngle + PI.toFloat() / 2f
        val tx = centerX + cos(trailAngle.toDouble()).toFloat() * trailDist +
                cos(trailPerp.toDouble()).toFloat() * trailWave
        val ty = centerY + sin(trailAngle.toDouble()).toFloat() * trailDist +
                sin(trailPerp.toDouble()).toFloat() * trailWave
        val trailA = (1f - t.toFloat() / particle.trailCount) * 0.25f * pAlpha

        drawCircle(
            color = particle.color.copy(alpha = trailA),
            radius = pSize * (1f - t * 0.12f).coerceAtLeast(0.2f),
            center = Offset(tx, ty)
        )
    }

    // Main particle with depth-based glow
    drawCircle(
        color = particle.color.copy(alpha = particle.color.alpha * pAlpha),
        radius = pSize,
        center = Offset(x, y)
    )
    // Soft glow halo for close particles
    if (particle.depth > 1.0f) {
        drawCircle(
            color = particle.color.copy(alpha = particle.color.alpha * pAlpha * 0.15f),
            radius = pSize * 2.5f,
            center = Offset(x, y)
        )
    }
}
