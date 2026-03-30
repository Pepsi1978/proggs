package com.entropyjournal.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.nativeCanvas
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

// --- Data classes ---

private data class SplashParticle(
    val angle: Float, val distance: Float, val spiralSpeed: Float,
    val size: Float, val color: Color, val trailCount: Int,
    val waveAmp: Float, val waveFreq: Float, val depth: Float
)

private data class RainParticle(
    val x: Float, val speed: Float, val size: Float, val color: Color
)

private data class FlyingNotebook(
    val baseX: Float,           // base position 0..1
    val baseY: Float,
    val driftSpeedX: Float,     // drift amplitude
    val driftSpeedY: Float,
    val driftFreqX: Float,      // drift frequency
    val driftFreqY: Float,
    val rotationAmp: Float,     // tilt amplitude (degrees)
    val rotationFreq: Float,
    val wingSpeed: Float,       // wing flap speed
    val nbSize: Float,          // notebook size in dp
    val phase: Float            // animation phase offset
)

@Composable
fun SplashScreen(
    onSplashFinished: (isSignedIn: Boolean) -> Unit,
    viewModel: SplashViewModel
) {
    // Phase-based animations
    val convergence = remember { Animatable(0f) }
    val textScale = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val impactRing = remember { Animatable(0f) }
    val particleAlpha = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0f) }
    val elem1Alpha = remember { Animatable(0f) }
    val elem1OffsetX = remember { Animatable(-300f) }
    val elem3Alpha = remember { Animatable(0f) }
    val elem3OffsetX = remember { Animatable(300f) }

    // Continuous animation for flying notebooks
    val infiniteTransition = rememberInfiniteTransition(label = "notebooks")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 100f,
        animationSpec = infiniteRepeatable(tween(100000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )

    // 5 flying notebooks with different movement patterns
    val notebooks = remember {
        listOf(
            FlyingNotebook(0.15f, 0.2f, 0.08f, 0.06f, 0.7f, 0.5f, 15f, 0.3f, 3.5f, 95f, 0f),
            FlyingNotebook(0.78f, 0.35f, 0.06f, 0.09f, 0.5f, 0.8f, 20f, 0.4f, 4.0f, 85f, 1.2f),
            FlyingNotebook(0.5f, 0.65f, 0.1f, 0.05f, 0.9f, 0.6f, 12f, 0.5f, 3.0f, 90f, 2.5f),
            FlyingNotebook(0.22f, 0.78f, 0.07f, 0.08f, 0.6f, 0.7f, 18f, 0.35f, 4.5f, 80f, 3.8f),
            FlyingNotebook(0.82f, 0.12f, 0.09f, 0.07f, 0.8f, 0.4f, 22f, 0.45f, 3.2f, 88f, 5.0f)
        )
    }

    // Particles
    val particles = remember {
        val colors = listOf(NeonCyan, NeonViolet, NeonMagenta)
        List(250) { i ->
            val depth = 0.4f + Random.nextFloat() * 1.2f
            SplashParticle(
                (i.toFloat() / 250f) * 2f * PI.toFloat() + Random.nextFloat() * 0.5f,
                0.6f + Random.nextFloat() * 0.6f, 1.2f + Random.nextFloat() * 3.5f,
                (1f + Random.nextFloat() * 2.5f) * depth,
                colors[i % 3].copy(alpha = (0.3f + depth * 0.35f).coerceAtMost(1f)),
                2 + Random.nextInt(5), 0.3f + Random.nextFloat() * 0.7f,
                2f + Random.nextFloat() * 4f, depth
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

        // Phase 2: Text jumps up (350ms)
        launch { particleAlpha.animateTo(0f, tween(350)) }
        launch { textAlpha.animateTo(1f, tween(120)) }
        launch { textOffsetY.animateTo(-130f, tween(350, easing = FastOutSlowInEasing)) }
        textScale.animateTo(2.0f, tween(350, easing = FastOutSlowInEasing))

        // Phase 3: Slam down (400ms)
        launch { textOffsetY.animateTo(0f, tween(200, easing = FastOutSlowInEasing)) }
        launch { impactRing.animateTo(1f, tween(600)) }
        launch { glowAlpha.animateTo(0.8f, tween(60)); glowAlpha.animateTo(0f, tween(400)) }
        textScale.animateTo(0.85f, tween(160))
        textScale.animateTo(1f, tween(250, easing = FastOutSlowInEasing))

        // Phase 4: Speech bubbles fly in
        delay(400)
        launch {
            launch { elem1Alpha.animateTo(1f, tween(500)) }
            elem1OffsetX.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        }
        delay(500)
        launch {
            launch { elem3Alpha.animateTo(1f, tween(500)) }
            elem3OffsetX.animateTo(0f, tween(600, easing = FastOutSlowInEasing))
        }

        // Hold 3.5 seconds
        delay(3500)
        onSplashFinished(viewModel.isUserSignedIn())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- Canvas: flying notebooks (background) + particles + effects ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val maxRadius = sqrt(centerX * centerX + centerY * centerY)

            // Layer 0: Flying notebooks (always visible, behind everything)
            notebooks.forEach { nb ->
                val t = time + nb.phase
                val x = (nb.baseX + sin(t * nb.driftFreqX) * nb.driftSpeedX) * size.width
                val y = (nb.baseY + sin(t * nb.driftFreqY + 0.7f) * nb.driftSpeedY) * size.height
                val rotation = sin(t * nb.rotationFreq) * nb.rotationAmp
                val wingAngle = sin(t * nb.wingSpeed) * 25f
                drawFlyingNotebook(x, y, nb.nbSize * density, rotation, wingAngle)
            }

            // Layer 1: Rain particles
            val progress = convergence.value
            val pAlpha = particleAlpha.value
            if (pAlpha > 0.01f) {
                rainParticles.forEach { rain ->
                    val ry = (-0.1f + progress * (1.2f + rain.speed)) * size.height
                    if (ry in 0f..size.height) {
                        drawCircle(rain.color.copy(alpha = rain.color.alpha * pAlpha * 0.6f),
                            rain.size * density, Offset(rain.x * size.width, ry))
                    }
                }
                // Layer 2: Spiral particles
                particles.sortedBy { it.depth }.forEach {
                    drawSpiralParticle(it, progress, pAlpha, centerX, centerY, maxRadius)
                }
            }

            // Impact effects
            val gA = glowAlpha.value
            if (gA > 0.01f) {
                drawCircle(NeonCyan.copy(alpha = gA * 0.2f), 160f * density, Offset(centerX, centerY))
                drawCircle(NeonViolet.copy(alpha = gA * 0.12f), 100f * density, Offset(centerX, centerY))
            }
            val rp = impactRing.value
            if (rp in 0.01f..0.99f) {
                drawCircle(NeonCyan.copy(alpha = (1f - rp) * 0.5f), rp * maxRadius * 0.4f,
                    Offset(centerX, centerY), style = Stroke(5f * (1f - rp) * density))
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
            Text("Best", style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold, fontSize = 48.sp, letterSpacing = 2.sp
            ), color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value))
            Text("Journal", style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold, fontSize = 48.sp, letterSpacing = 2.sp
            ), color = MaterialTheme.colorScheme.primary.copy(alpha = textAlpha.value))
        }

        // --- Speech bubble 1: Teacher, bottom-left ---
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
            SpeechBubble("Geniale\nErkenntnisse")
        }

        // --- Speech bubble 2: Professor, bottom-right ---
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
            SpeechBubble("Aus Eintr\u00e4gen\nlernen")
            Spacer(Modifier.width(8.dp))
            Text("\uD83D\uDC68\u200D\uD83C\uDF93", fontSize = 42.sp)
        }
    }
}

// --- Speech bubble ---
@Composable
private fun SpeechBubble(text: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 8.dp) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp, lineHeight = 22.sp), color = Color.Black)
    }
}

// --- Flying notebook with angel wings ---
private fun DrawScope.drawFlyingNotebook(
    cx: Float, cy: Float, nbSize: Float, rotation: Float, wingAngle: Float
) {
    val alpha = 0.10f  // 90% transparent
    val nbW = nbSize * 0.7f
    val nbH = nbSize * 0.9f
    val coverColor = Color(0xFF8B6914).copy(alpha = alpha)
    val pageColor = Color(0xFFFFFDE7).copy(alpha = alpha * 1.2f)
    val lineColor = Color(0xFF90CAF9).copy(alpha = alpha * 0.8f)
    val textColor = android.graphics.Color.argb((alpha * 255 * 1.5f).toInt().coerceAtMost(255), 60, 40, 20)
    val wingColor = Color(0xFFE0E0E0).copy(alpha = alpha * 1.5f)
    val penColor = Color(0xFF333333).copy(alpha = alpha * 1.3f)

    rotate(rotation, Offset(cx, cy)) {
        val left = cx - nbW / 2
        val top = cy - nbH / 2

        // --- Wings (behind notebook) ---
        val wingSpan = nbW * 0.7f
        val wingH = nbH * 0.4f

        // Left wing
        rotate(wingAngle, Offset(left, cy)) {
            val lw = Path().apply {
                moveTo(left, cy - wingH * 0.4f)
                cubicTo(left - wingSpan * 0.4f, cy - wingH * 0.8f,
                    left - wingSpan * 0.8f, cy - wingH * 0.3f,
                    left - wingSpan, cy)
                cubicTo(left - wingSpan * 0.8f, cy + wingH * 0.3f,
                    left - wingSpan * 0.4f, cy + wingH * 0.5f,
                    left, cy + wingH * 0.4f)
                close()
            }
            drawPath(lw, wingColor)
            // Feather lines
            for (i in 1..3) {
                val fx = left - wingSpan * (i * 0.22f)
                drawLine(Color.White.copy(alpha = alpha * 0.5f),
                    Offset(left - 2f, cy - wingH * 0.1f * i),
                    Offset(fx, cy + wingH * 0.05f * i), 1f * density)
            }
        }

        // Right wing
        val right = left + nbW
        rotate(-wingAngle, Offset(right, cy)) {
            val rw = Path().apply {
                moveTo(right, cy - wingH * 0.4f)
                cubicTo(right + wingSpan * 0.4f, cy - wingH * 0.8f,
                    right + wingSpan * 0.8f, cy - wingH * 0.3f,
                    right + wingSpan, cy)
                cubicTo(right + wingSpan * 0.8f, cy + wingH * 0.3f,
                    right + wingSpan * 0.4f, cy + wingH * 0.5f,
                    right, cy + wingH * 0.4f)
                close()
            }
            drawPath(rw, wingColor)
            for (i in 1..3) {
                val fx = right + wingSpan * (i * 0.22f)
                drawLine(Color.White.copy(alpha = alpha * 0.5f),
                    Offset(right + 2f, cy - wingH * 0.1f * i),
                    Offset(fx, cy + wingH * 0.05f * i), 1f * density)
            }
        }

        // --- Notebook body ---
        // Shadow
        drawRoundRect(Color.Black.copy(alpha = alpha * 0.3f),
            Offset(left + 2f, top + 3f), Size(nbW, nbH), CornerRadius(4f, 4f))
        // Cover
        drawRoundRect(coverColor, Offset(left, top), Size(nbW, nbH), CornerRadius(4f, 4f))
        // Spine
        drawRoundRect(coverColor.copy(alpha = alpha * 0.7f),
            Offset(left, top), Size(nbW * 0.1f, nbH), CornerRadius(4f, 4f))
        // Page
        val pageInset = nbW * 0.12f
        drawRoundRect(pageColor, Offset(left + pageInset, top + nbH * 0.05f),
            Size(nbW - pageInset - 4f, nbH * 0.9f), CornerRadius(2f, 2f))

        // Ruled lines
        for (i in 1..5) {
            val ly = top + nbH * 0.05f + (nbH * 0.9f / 6) * i
            drawLine(lineColor, Offset(left + pageInset + 4f, ly),
                Offset(left + nbW - 8f, ly), 0.5f * density)
        }

        // "Tagebuch" text via native canvas
        drawContext.canvas.nativeCanvas.drawText(
            "Tagebuch", cx + pageInset * 0.3f, top + nbH * 0.3f,
            android.graphics.Paint().apply {
                color = textColor
                textSize = nbSize * 0.14f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            }
        )

        // Pen (diagonal across lower right)
        val penStartX = cx + nbW * 0.05f
        val penStartY = cy + nbH * 0.2f
        val penEndX = cx + nbW * 0.3f
        val penEndY = cy - nbH * 0.05f
        drawLine(penColor, Offset(penStartX, penStartY), Offset(penEndX, penEndY),
            2f * density, StrokeCap.Round)
        drawCircle(Color(0xFF1565C0).copy(alpha = alpha * 1.5f), 1.5f * density,
            Offset(penStartX - 1f, penStartY + 1f))
    }
}

// --- Spiral particle ---
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
