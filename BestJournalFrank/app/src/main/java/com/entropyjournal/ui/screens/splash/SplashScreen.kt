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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    val angle: Float, val distance: Float, val spiralSpeed: Float,
    val size: Float, val color: Color, val trailCount: Int,
    val waveAmp: Float, val waveFreq: Float, val depth: Float
)

private data class RainParticle(
    val x: Float, val speed: Float, val size: Float, val color: Color
)

@Composable
fun SplashScreen(
    onSplashFinished: (isSignedIn: Boolean) -> Unit,
    viewModel: SplashViewModel
) {
    // Animation state
    val convergence = remember { Animatable(0f) }
    val textScale = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val impactRing = remember { Animatable(0f) }
    val particleAlpha = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }

    // Emoji element animations
    val elem1Alpha = remember { Animatable(0f) }
    val elem1OffsetX = remember { Animatable(-300f) }
    val elem2Alpha = remember { Animatable(0f) }
    val elem2OffsetY = remember { Animatable(150f) }
    val elem3Alpha = remember { Animatable(0f) }
    val elem3OffsetX = remember { Animatable(300f) }

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
            RainParticle(Random.nextFloat(), 0.3f + Random.nextFloat() * 0.7f,
                (0.8f + Random.nextFloat() * 1.5f) * (0.3f + Random.nextFloat() * 0.8f),
                colors[i % 3].copy(alpha = 0.15f + Random.nextFloat() * 0.15f))
        }
    }

    LaunchedEffect(Unit) {
        // Phase 1: Particles (1600ms)
        convergence.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))

        // Phase 2: Text JUMPS UP (350ms)
        launch { particleAlpha.animateTo(0f, tween(350)) }
        launch { textAlpha.animateTo(1f, tween(120)) }
        launch { textOffsetY.animateTo(-130f, tween(350, easing = FastOutSlowInEasing)) }
        textScale.animateTo(2.0f, tween(350, easing = FastOutSlowInEasing))

        // Phase 3: SLAM DOWN (400ms)
        launch { textOffsetY.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
        launch { impactRing.animateTo(1f, tween(600)) }
        launch { glowAlpha.animateTo(0.8f, tween(60)); glowAlpha.animateTo(0f, tween(400)) }
        textScale.animateTo(0.85f, tween(160))
        textScale.animateTo(1f, tween(250, easing = FastOutSlowInEasing))

        // Phase 4: Elements fly in — notebook first, then speech bubbles below
        delay(200)

        // Notebook flies down from above into top area
        launch {
            launch { elem2Alpha.animateTo(1f, tween(500)) }
            elem2OffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        }
        delay(400)

        // Speech bubble 1 (teacher) flies in from left
        launch {
            launch { elem1Alpha.animateTo(1f, tween(500)) }
            elem1OffsetX.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        }
        delay(400)

        // Speech bubble 2 (professor) flies in from right
        launch {
            launch { elem3Alpha.animateTo(1f, tween(500)) }
            elem3OffsetX.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        }

        // Hold everything visible for 3.5 seconds
        delay(3500)
        onSplashFinished(viewModel.isUserSignedIn())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                    val ry = (-0.1f + progress * (1.2f + rain.speed)) * size.height
                    if (ry in 0f..size.height) {
                        drawCircle(rain.color.copy(alpha = rain.color.alpha * pAlpha * 0.6f), rain.size * density, Offset(rain.x * size.width, ry))
                    }
                }
                particles.sortedBy { it.depth }.forEach { drawSpiralParticle(it, progress, pAlpha, centerX, centerY, maxRadius) }
            }

            val gA = glowAlpha.value
            if (gA > 0.01f) {
                drawCircle(NeonCyan.copy(alpha = gA * 0.2f), 160f * density, Offset(centerX, centerY))
                drawCircle(NeonViolet.copy(alpha = gA * 0.12f), 100f * density, Offset(centerX, centerY))
            }

            val rp = impactRing.value
            if (rp in 0.01f..0.99f) {
                drawCircle(NeonCyan.copy(alpha = (1f - rp) * 0.5f), rp * maxRadius * 0.4f, Offset(centerX, centerY), style = Stroke(5f * (1f - rp) * density))
            }
        }

        // --- Element 2: Notebook — top area, flies down from above ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)
                .graphicsLayer {
                    alpha = elem2Alpha.value
                    translationY = elem2OffsetY.value * density
                }
        ) {
            Canvas(modifier = Modifier.size(120.dp, 140.dp)) {
                drawNotebook()
            }
        }

        // --- "Best Journal" title — CENTER ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-20).dp)
                .graphicsLayer {
                    scaleX = textScale.value
                    scaleY = textScale.value
                    translationY = textOffsetY.value * density
                    shadowElevation = textScale.value * 24f
                }
        ) {
            Text(
                "Best",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold, fontSize = 48.sp, letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value)
            )
            Text(
                "Journal",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold, fontSize = 48.sp, letterSpacing = 2.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value)
            )
        }

        // --- Element 1: Teacher with speech bubble — below center left, from left ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 16.dp, y = (-160).dp)
                .graphicsLayer {
                    alpha = elem1Alpha.value
                    translationX = elem1OffsetX.value * density
                }
        ) {
            Text("\uD83E\uDDD0☝\uFE0F", fontSize = 42.sp)
            Spacer(Modifier.width(8.dp))
            SpeechBubble(text = "Geniale\nErkenntnisse", pointLeft = true)
        }

        // --- Element 3: Professor with speech bubble — bottom right, from right ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-80).dp)
                .graphicsLayer {
                    alpha = elem3Alpha.value
                    translationX = elem3OffsetX.value * density
                }
        ) {
            SpeechBubble(text = "Aus Eintr\u00e4gen\nlernen", pointLeft = false)
            Spacer(Modifier.width(8.dp))
            Text("\uD83D\uDC68\u200D\uD83C\uDF93", fontSize = 42.sp)
        }
    }
}

// --- Speech bubble composable ---
@Composable
private fun SpeechBubble(text: String, pointLeft: Boolean) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp
            ),
            color = Color.Black
        )
    }
}

// --- Hand-drawn notebook illustration ---
private fun DrawScope.drawNotebook() {
    val w = size.width
    val h = size.height
    val coverColor = Color(0xFF8B4513).copy(alpha = 0.55f)  // leather brown, semi-transparent
    val pageColor = Color(0xFFFFFEF0).copy(alpha = 0.7f)   // cream white, slightly transparent
    val ringColor = Color(0xFFC0C0C0).copy(alpha = 0.6f)   // silver, semi-transparent
    val lineColor = Color(0xFFB0D4F1)       // ruled line blue
    val penColor = Color(0xFF2B2B2B)        // dark pen
    val penTip = Color(0xFF1565C0)          // blue ink

    // Shadow
    drawRoundRect(Color.Black.copy(alpha = 0.15f), Offset(4f, 6f), Size(w - 4f, h - 4f), CornerRadius(12f, 12f))

    // Cover
    drawRoundRect(coverColor, Offset(0f, 0f), Size(w, h), CornerRadius(10f, 10f))

    // Cover edge detail (darker strip on left)
    drawRoundRect(coverColor.copy(alpha = 0.6f), Offset(0f, 0f), Size(w * 0.12f, h), CornerRadius(10f, 10f))

    // Page area (slightly inset)
    val pageLeft = w * 0.15f
    val pageTop = h * 0.06f
    val pageW = w * 0.78f
    val pageH = h * 0.88f
    drawRoundRect(pageColor, Offset(pageLeft, pageTop), Size(pageW, pageH), CornerRadius(4f, 4f))

    // Ruled lines
    val lineCount = 8
    for (i in 1..lineCount) {
        val ly = pageTop + (pageH / (lineCount + 1)) * i
        drawLine(lineColor, Offset(pageLeft + 8f, ly), Offset(pageLeft + pageW - 8f, ly), 1.5f)
    }

    // Red margin line
    drawLine(Color(0xFFE57373), Offset(pageLeft + pageW * 0.18f, pageTop + 4f),
        Offset(pageLeft + pageW * 0.18f, pageTop + pageH - 4f), 1.5f)

    // Spiral rings along left edge
    val ringCount = 6
    for (i in 0 until ringCount) {
        val ry = h * 0.1f + (h * 0.8f / ringCount) * (i + 0.5f)
        val rx = w * 0.08f
        val ringR = w * 0.04f
        // Ring circle
        drawCircle(ringColor, ringR, Offset(rx, ry), style = Stroke(3f))
        // Ring shadow
        drawCircle(Color.Black.copy(alpha = 0.1f), ringR, Offset(rx + 1f, ry + 1f), style = Stroke(2f))
    }

    // Pen — drawn diagonally across bottom-right
    rotate(degrees = -35f, pivot = Offset(w * 0.85f, h * 0.85f)) {
        val penStartX = w * 0.55f
        val penStartY = h * 0.85f
        val penLength = w * 0.5f
        // Pen body
        drawLine(penColor, Offset(penStartX, penStartY), Offset(penStartX + penLength, penStartY), 6f, StrokeCap.Round)
        // Pen clip
        drawLine(Color(0xFFFFD700), Offset(penStartX + penLength * 0.7f, penStartY - 2f),
            Offset(penStartX + penLength * 0.85f, penStartY - 2f), 2.5f, StrokeCap.Round)
        // Pen tip
        drawLine(penTip, Offset(penStartX - 2f, penStartY), Offset(penStartX - 12f, penStartY), 4f, StrokeCap.Round)
        // Tip point
        drawCircle(penTip, 2.5f, Offset(penStartX - 14f, penStartY))
    }
}

private fun DrawScope.drawSpiralParticle(
    p: SplashParticle, progress: Float, pAlpha: Float,
    cx: Float, cy: Float, maxR: Float
) {
    val dist = p.distance * (1f - progress) * maxR
    val angle = p.angle + progress * p.spiralSpeed * PI.toFloat()
    val wave = sin(progress * p.waveFreq * PI.toFloat() * 2f).toFloat() * p.waveAmp * (1f - progress) * 40f * density
    val perp = angle + PI.toFloat() / 2f
    val bx = cx + cos(angle.toDouble()).toFloat() * dist
    val by = cy + sin(angle.toDouble()).toFloat() * dist
    val x = bx + cos(perp.toDouble()).toFloat() * wave
    val y = by + sin(perp.toDouble()).toFloat() * wave
    val sz = p.size * (0.4f + 0.6f * progress) * density

    for (t in 1..p.trailCount) {
        val tp = (progress - t * 0.012f).coerceAtLeast(0f)
        val td = p.distance * (1f - tp) * maxR
        val ta = p.angle + tp * p.spiralSpeed * PI.toFloat()
        val tw = sin(tp * p.waveFreq * PI.toFloat() * 2f).toFloat() * p.waveAmp * (1f - tp) * 40f * density
        val tperp = ta + PI.toFloat() / 2f
        val tx = cx + cos(ta.toDouble()).toFloat() * td + cos(tperp.toDouble()).toFloat() * tw
        val ty = cy + sin(ta.toDouble()).toFloat() * td + sin(tperp.toDouble()).toFloat() * tw
        drawCircle(p.color.copy(alpha = (1f - t.toFloat() / p.trailCount) * 0.25f * pAlpha),
            sz * (1f - t * 0.12f).coerceAtLeast(0.2f), Offset(tx, ty))
    }
    drawCircle(p.color.copy(alpha = p.color.alpha * pAlpha), sz, Offset(x, y))
    if (p.depth > 1.0f) drawCircle(p.color.copy(alpha = p.color.alpha * pAlpha * 0.15f), sz * 2.5f, Offset(x, y))
}
