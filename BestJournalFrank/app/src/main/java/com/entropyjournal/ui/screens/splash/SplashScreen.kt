package com.entropyjournal.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    val angle: Float,
    val distance: Float,
    val spiralSpeed: Float,
    val size: Float,
    val color: Color,
    val trailCount: Int,
    val waveAmp: Float,
    val waveFreq: Float,
    val depth: Float
)

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
    // --- Animation state ---
    val convergence = remember { Animatable(0f) }
    val textScale = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val impactRing = remember { Animatable(0f) }
    val particleAlpha = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }

    // Emoji element animations
    val elem1Alpha = remember { Animatable(0f) }
    val elem1OffsetX = remember { Animatable(-200f) }
    val elem2Alpha = remember { Animatable(0f) }
    val elem2OffsetY = remember { Animatable(100f) }
    val elem3Alpha = remember { Animatable(0f) }
    val elem3OffsetX = remember { Animatable(200f) }

    // Particles
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
        // Phase 1: Particles spiral in (1600ms)
        convergence.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))

        // Phase 2: Text JUMPS UP big (350ms)
        launch { particleAlpha.animateTo(0f, tween(350)) }
        launch { textAlpha.animateTo(1f, tween(120)) }
        launch { textOffsetY.animateTo(-130f, tween(350, easing = FastOutSlowInEasing)) }
        textScale.animateTo(2.0f, tween(350, easing = FastOutSlowInEasing))

        // Phase 3: SLAM DOWN (400ms)
        launch { textOffsetY.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
        launch { impactRing.animateTo(1f, tween(600)) }
        launch {
            glowAlpha.animateTo(0.8f, tween(60))
            glowAlpha.animateTo(0f, tween(400))
        }
        textScale.animateTo(0.85f, tween(160))
        textScale.animateTo(1f, tween(250, easing = FastOutSlowInEasing))

        // Phase 4: Emoji elements fly in — staggered (150ms apart)
        delay(150)

        // Element 1: Teacher flies in from left
        launch {
            launch { elem1Alpha.animateTo(1f, tween(350)) }
            elem1OffsetX.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }

        delay(200)

        // Element 2: Notebook flies up from bottom
        launch {
            launch { elem2Alpha.animateTo(1f, tween(350)) }
            elem2OffsetY.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }

        delay(200)

        // Element 3: Professor flies in from right
        launch {
            launch { elem3Alpha.animateTo(1f, tween(350)) }
            elem3OffsetX.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        }

        // Hold everything visible
        delay(1200)
        onSplashFinished(viewModel.isUserSignedIn())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // --- Particle canvas ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = sqrt(centerX * centerX + centerY * centerY)
            val progress = convergence.value
            val pAlpha = particleAlpha.value

            if (pAlpha > 0.01f) {
                rainParticles.forEach { rain ->
                    val rainY = (-0.1f + progress * (1.2f + rain.speed)) * size.height
                    val rainX = rain.x * size.width
                    if (rainY in 0f..size.height) {
                        val rSize = rain.size * density
                        drawCircle(rain.color.copy(alpha = rain.color.alpha * pAlpha * 0.6f), rSize, Offset(rainX, rainY))
                        drawCircle(rain.color.copy(alpha = rain.color.alpha * pAlpha * 0.2f), rSize * 0.6f, Offset(rainX, rainY - rSize * 3f))
                    }
                }

                particles.filter { it.depth < 0.8f }.forEach { drawSpiralParticle(it, progress, pAlpha, centerX, centerY, maxRadius) }
                particles.filter { it.depth >= 0.8f }.forEach { drawSpiralParticle(it, progress, pAlpha, centerX, centerY, maxRadius) }
            }

            val gA = glowAlpha.value
            if (gA > 0.01f) {
                drawCircle(NeonCyan.copy(alpha = gA * 0.2f), 160f * density, Offset(centerX, centerY))
                drawCircle(NeonViolet.copy(alpha = gA * 0.12f), 100f * density, Offset(centerX, centerY))
            }

            val ringProg = impactRing.value
            if (ringProg in 0.01f..0.99f) {
                drawCircle(NeonCyan.copy(alpha = (1f - ringProg) * 0.5f), ringProg * maxRadius * 0.4f, Offset(centerX, centerY), style = Stroke(5f * (1f - ringProg) * density))
                val r2 = (ringProg - 0.06f).coerceAtLeast(0f)
                if (r2 > 0.01f) drawCircle(NeonViolet.copy(alpha = (1f - r2) * 0.3f), r2 * maxRadius * 0.4f, Offset(centerX, centerY), style = Stroke(3f * (1f - r2) * density))
            }
        }

        // --- "Best Journal" title ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-40).dp)
                .graphicsLayer {
                    scaleX = textScale.value
                    scaleY = textScale.value
                    translationY = textOffsetY.value * density
                    shadowElevation = textScale.value * 24f
                }
        ) {
            Text(
                text = "Best",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value)
            )
            Text(
                text = "Journal",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value)
            )
        }

        // --- Element 1: Teacher emoji with speech bubble — flies from left ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 20.dp, y = 60.dp)
                .graphicsLayer {
                    alpha = elem1Alpha.value
                    translationX = elem1OffsetX.value * density
                }
        ) {
            Text("🤓☝️", fontSize = 28.sp)
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NeonCyan.copy(alpha = 0.15f),
                shadowElevation = 4.dp
            ) {
                Text(
                    "Geniale Erkenntnisse",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // --- Element 2: Notebook — flies up from bottom ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 120.dp)
                .graphicsLayer {
                    alpha = elem2Alpha.value
                    translationY = elem2OffsetY.value * density
                }
        ) {
            Text("📓", fontSize = 24.sp)
            Spacer(Modifier.width(4.dp))
            Text("📓", fontSize = 20.sp)
            Spacer(Modifier.width(4.dp))
            Text("📓", fontSize = 16.sp)
        }

        // --- Element 3: Professor with speech bubble — flies from right ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-20).dp, y = 170.dp)
                .graphicsLayer {
                    alpha = elem3Alpha.value
                    translationX = elem3OffsetX.value * density
                }
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = NeonViolet.copy(alpha = 0.15f),
                shadowElevation = 4.dp
            ) {
                Text(
                    "Aus Eintr\u00e4gen lernen",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonViolet,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("\uD83D\uDC68\u200D\uD83C\uDF93", fontSize = 28.sp)
        }
    }
}

private fun DrawScope.drawSpiralParticle(
    particle: SplashParticle, progress: Float, pAlpha: Float,
    centerX: Float, centerY: Float, maxRadius: Float
) {
    val currentDist = particle.distance * (1f - progress) * maxRadius
    val currentAngle = particle.angle + progress * particle.spiralSpeed * PI.toFloat()
    val waveOffset = sin(progress * particle.waveFreq * PI.toFloat() * 2f).toFloat() *
            particle.waveAmp * (1f - progress) * 40f * density
    val perpAngle = currentAngle + PI.toFloat() / 2f
    val baseX = centerX + cos(currentAngle.toDouble()).toFloat() * currentDist
    val baseY = centerY + sin(currentAngle.toDouble()).toFloat() * currentDist
    val x = baseX + cos(perpAngle.toDouble()).toFloat() * waveOffset
    val y = baseY + sin(perpAngle.toDouble()).toFloat() * waveOffset
    val pSize = particle.size * (0.4f + 0.6f * progress) * density

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
        drawCircle(particle.color.copy(alpha = (1f - t.toFloat() / particle.trailCount) * 0.25f * pAlpha),
            pSize * (1f - t * 0.12f).coerceAtLeast(0.2f), Offset(tx, ty))
    }

    drawCircle(particle.color.copy(alpha = particle.color.alpha * pAlpha), pSize, Offset(x, y))
    if (particle.depth > 1.0f) {
        drawCircle(particle.color.copy(alpha = particle.color.alpha * pAlpha * 0.15f), pSize * 2.5f, Offset(x, y))
    }
}
