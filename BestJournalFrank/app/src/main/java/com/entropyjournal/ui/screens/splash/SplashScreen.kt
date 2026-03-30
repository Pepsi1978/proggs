package com.entropyjournal.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    // 4 smiley elements — each has alpha, position offset, and scale (big→small)
    val elem1Alpha = remember { Animatable(0f) }
    val elem1OffsetX = remember { Animatable(-300f) }
    val elem1Scale = remember { Animatable(3.5f) }
    val elem2Alpha = remember { Animatable(0f) }
    val elem2OffsetX = remember { Animatable(300f) }
    val elem2Scale = remember { Animatable(3.5f) }
    val elem3Alpha = remember { Animatable(0f) }
    val elem3OffsetX = remember { Animatable(-300f) }
    val elem3Scale = remember { Animatable(3.5f) }
    val elem4Alpha = remember { Animatable(0f) }
    val elem4OffsetX = remember { Animatable(300f) }
    val elem4Scale = remember { Animatable(3.5f) }
    val startBtnAlpha = remember { Animatable(0f) }
    val startBtnOffsetY = remember { Animatable(150f) }
    val startBtnScale = remember { Animatable(0f) }

    // Continuous animations
    val infiniteTransition = rememberInfiniteTransition(label = "notebooks")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(600000, easing = LinearEasing), RepeatMode.Restart),
        label = "time"
    )
    // Heartbeat pulse for Best Journal card: bum-bum... bum-bum...
    val heartbeat by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 1400
            1.0f at 0
            1.06f at 120    // first beat — expand
            1.0f at 250     // contract
            1.04f at 400    // second beat — smaller expand
            1.0f at 550     // settle
            1.0f at 1400    // pause before next cycle
        }),
        label = "heartbeat"
    )

    // 5 flying notebooks with different movement patterns
    val notebooks = remember {
        listOf(
            FlyingNotebook(0.15f, 0.2f, 0.08f, 0.06f, 0.7f, 0.5f, 15f, 0.3f, 3.5f, 105f, 0f),
            FlyingNotebook(0.78f, 0.35f, 0.06f, 0.09f, 0.5f, 0.8f, 20f, 0.4f, 4.0f, 70f, 1.2f),
            FlyingNotebook(0.5f, 0.65f, 0.1f, 0.05f, 0.9f, 0.6f, 12f, 0.5f, 3.0f, 95f, 2.5f),
            FlyingNotebook(0.22f, 0.78f, 0.07f, 0.08f, 0.6f, 0.7f, 18f, 0.35f, 4.5f, 60f, 3.8f),
            FlyingNotebook(0.82f, 0.12f, 0.09f, 0.07f, 0.8f, 0.4f, 22f, 0.45f, 3.2f, 85f, 5.0f)
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

        // Phase 4: Start button flies in FIRST
        delay(300)
        launch { startBtnAlpha.animateTo(1f, tween(400)) }
        launch { startBtnOffsetY.animateTo(0f, tween(500, easing = FastOutSlowInEasing)) }
        startBtnScale.animateTo(1.3f, tween(300, easing = FastOutSlowInEasing))
        startBtnScale.animateTo(1f, tween(200))

        // Phase 5: Smileys zoom in SEQUENTIALLY — each waits for previous to land
        delay(400)

        // 1. "Geniale Erkenntnisse" — zooms from full screen to landing spot
        launch { elem1Alpha.animateTo(1f, tween(300)) }
        launch { elem1OffsetX.animateTo(0f, tween(1200, easing = FastOutSlowInEasing)) }
        elem1Scale.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))

        // 2. "Aus Einträgen lernen" — starts after 1 has landed
        delay(200)
        launch { elem2Alpha.animateTo(1f, tween(300)) }
        launch { elem2OffsetX.animateTo(0f, tween(1200, easing = FastOutSlowInEasing)) }
        elem2Scale.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))

        // 3. "KI-Zusammenfassung" — starts after 2 has landed
        delay(200)
        launch { elem3Alpha.animateTo(1f, tween(300)) }
        launch { elem3OffsetX.animateTo(0f, tween(1200, easing = FastOutSlowInEasing)) }
        elem3Scale.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))

        // 4. "Tiefe Tagebuchanalyse" — starts after 3 has landed
        delay(200)
        launch { elem4Alpha.animateTo(1f, tween(300)) }
        launch { elem4OffsetX.animateTo(0f, tween(1200, easing = FastOutSlowInEasing)) }
        elem4Scale.animateTo(1f, tween(1200, easing = FastOutSlowInEasing))
        // No auto-navigation — user presses Start button
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

            // Heartbeat glow rings — pulse outward during each beat
            if (textScale.value >= 0.99f) {
                val hb = heartbeat
                if (hb > 1.005f) {
                    val glowStrength = (hb - 1f) * 8f  // 0..0.48
                    drawCircle(NeonCyan.copy(alpha = glowStrength * 0.15f),
                        200f * density * hb, Offset(centerX, centerY - 20f * density))
                    drawCircle(NeonViolet.copy(alpha = glowStrength * 0.1f),
                        280f * density * hb, Offset(centerX, centerY - 20f * density))
                    drawCircle(NeonMagenta.copy(alpha = glowStrength * 0.06f),
                        350f * density * hb, Offset(centerX, centerY - 20f * density),
                        style = Stroke(2f * density))
                }
            }
        }

        // --- "Best Journal" on papyrus scroll — CENTER, always on top ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-20).dp)
                .graphicsLayer {
                    val pulse = if (textScale.value >= 0.99f) heartbeat else 1f
                    scaleX = textScale.value * pulse
                    scaleY = textScale.value * pulse
                    translationY = textOffsetY.value * density
                }
        ) {
            // Papyrus scroll image
            Image(
                painter = painterResource(id = com.entropyjournal.R.drawable.papyrus_scroll),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(280.dp)
                    .graphicsLayer { alpha = textAlpha.value }
            )
            // "Best Journal" in elegant handwriting (Dancing Script)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset(y = (-5).dp)
            ) {
                Text("Best", style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = com.entropyjournal.ui.theme.Caveat,
                    fontWeight = FontWeight.Bold, fontSize = 52.sp, letterSpacing = 1.sp
                ), color = Color(0xFF3A1800).copy(alpha = textAlpha.value))
                Text("Journal", style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = com.entropyjournal.ui.theme.Caveat,
                    fontWeight = FontWeight.Bold, fontSize = 52.sp, letterSpacing = 1.sp
                ), color = Color(0xFF3A1800).copy(alpha = textAlpha.value))
            }
        }

        // --- Smiley 1: "Geniale Erkenntnisse" — bottom-left, from left ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 16.dp, y = (-190).dp)
                .graphicsLayer {
                    alpha = elem1Alpha.value
                    translationX = elem1OffsetX.value * density
                    scaleX = elem1Scale.value
                    scaleY = elem1Scale.value
                }
        ) {
            Text("\uD83E\uDDD9\u200D♂\uFE0F", fontSize = 36.sp) // Mage/wizard
            Spacer(Modifier.width(6.dp))
            SpeechBubble("Geniale\nErkenntnisse")
        }

        // --- Smiley 2: "Aus Einträgen lernen" — bottom-right, from right ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-16).dp, y = (-120).dp)
                .graphicsLayer {
                    alpha = elem2Alpha.value
                    translationX = elem2OffsetX.value * density
                    scaleX = elem2Scale.value
                    scaleY = elem2Scale.value
                }
        ) {
            SpeechBubble("Aus Eintr\u00e4gen\nlernen")
            Spacer(Modifier.width(6.dp))
            Text("\uD83E\uDDD1\u200D\uD83D\uDCBB", fontSize = 36.sp) // Technologist
        }

        // --- Smiley 3: "KI-Zusammenfassung" — top-left, from left ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = 90.dp)
                .graphicsLayer {
                    alpha = elem3Alpha.value
                    translationX = elem3OffsetX.value * density
                    scaleX = elem3Scale.value
                    scaleY = elem3Scale.value
                }
        ) {
            Text("\u26A1", fontSize = 34.sp) // Lightning bolt — KI power
            Spacer(Modifier.width(6.dp))
            SpeechBubble("KI-\nZusammenfassung")
        }

        // --- Smiley 4: "Tiefe Tagebuchanalyse" — top-right, from right ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 185.dp)
                .graphicsLayer {
                    alpha = elem4Alpha.value
                    translationX = elem4OffsetX.value * density
                    scaleX = elem4Scale.value
                    scaleY = elem4Scale.value
                }
        ) {
            SpeechBubble("Tiefe\nTagebuchanalyse")
            Spacer(Modifier.width(6.dp))
            Text("\uD83C\uDF00", fontSize = 34.sp) // Cyclone — deep analysis spiral
        }

        // --- Start button — bottom center ---
        Button(
            onClick = { onSplashFinished(viewModel.isUserSignedIn()) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-48).dp)
                .graphicsLayer {
                    alpha = startBtnAlpha.value
                    scaleX = startBtnScale.value
                    scaleY = startBtnScale.value
                    translationY = startBtnOffsetY.value * density
                    shadowElevation = 16f
                }
                .border(2.dp, Color.Black, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 8.dp,
                pressedElevation = 2.dp
            )
        ) {
            Text(
                "Start",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// --- Speech bubble with papyrus background ---
@Composable
private fun SpeechBubble(text: String) {
    Box(contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = com.entropyjournal.R.drawable.papyrus_bubble),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        Text(text, Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold,
                fontSize = 15.sp, lineHeight = 20.sp), color = Color(0xFF3E2000))
    }
}

// --- Flying notebook with angel wings ---
private fun DrawScope.drawFlyingNotebook(
    cx: Float, cy: Float, nbSize: Float, rotation: Float, wingAngle: Float
) {
    val a = 0.13f  // base alpha
    val nbW = nbSize * 0.7f
    val nbH = nbSize * 0.9f
    val coverColor = Color(0xFF8B6914).copy(alpha = a)
    val pageColor = Color(0xFFFFFDE7).copy(alpha = a * 1.3f)
    val lineColor = Color(0xFF90CAF9).copy(alpha = a)
    val textAlpha = (a * 255 * 2f).toInt().coerceAtMost(255)
    // Wings SAME strength as cover
    val wingFill = Color(0xFFD4A54A).copy(alpha = a)
    val wingEdge = Color(0xFFBB8833).copy(alpha = a * 1.2f)
    val penBodyColor = Color(0xFF444444).copy(alpha = a * 2f)

    rotate(rotation, Offset(cx, cy)) {
        val left = cx - nbW / 2
        val top = cy - nbH / 2
        val right = left + nbW

        // --- Wings (same opacity as notebook!) ---
        val wingSpan = nbW * 0.85f
        val wingH = nbH * 0.45f

        // Left wing
        rotate(wingAngle, Offset(left, cy)) {
            val lw = Path().apply {
                moveTo(left, cy - wingH * 0.35f)
                cubicTo(left - wingSpan * 0.3f, cy - wingH * 0.9f,
                    left - wingSpan * 0.7f, cy - wingH * 0.4f,
                    left - wingSpan, cy - wingH * 0.05f)
                cubicTo(left - wingSpan * 0.85f, cy + wingH * 0.25f,
                    left - wingSpan * 0.4f, cy + wingH * 0.55f,
                    left, cy + wingH * 0.35f)
                close()
            }
            drawPath(lw, wingFill)
            drawPath(lw, wingEdge, style = Stroke(1.5f * density))
            // Feather lines
            for (i in 1..4) {
                val fx = left - wingSpan * (i * 0.2f)
                val fy1 = cy - wingH * (0.3f - i * 0.04f)
                val fy2 = cy + wingH * (0.1f + i * 0.04f)
                drawLine(wingEdge, Offset(left, fy1), Offset(fx, fy2), 1.2f * density, StrokeCap.Round)
            }
        }

        // Right wing
        rotate(-wingAngle, Offset(right, cy)) {
            val rw = Path().apply {
                moveTo(right, cy - wingH * 0.35f)
                cubicTo(right + wingSpan * 0.3f, cy - wingH * 0.9f,
                    right + wingSpan * 0.7f, cy - wingH * 0.4f,
                    right + wingSpan, cy - wingH * 0.05f)
                cubicTo(right + wingSpan * 0.85f, cy + wingH * 0.25f,
                    right + wingSpan * 0.4f, cy + wingH * 0.55f,
                    right, cy + wingH * 0.35f)
                close()
            }
            drawPath(rw, wingFill)
            drawPath(rw, wingEdge, style = Stroke(1.5f * density))
            for (i in 1..4) {
                val fx = right + wingSpan * (i * 0.2f)
                val fy1 = cy - wingH * (0.3f - i * 0.04f)
                val fy2 = cy + wingH * (0.1f + i * 0.04f)
                drawLine(wingEdge, Offset(right, fy1), Offset(fx, fy2), 1.2f * density, StrokeCap.Round)
            }
        }

        // --- Notebook body ---
        drawRoundRect(Color.Black.copy(alpha = a * 0.4f),
            Offset(left + 3f, top + 4f), Size(nbW, nbH), CornerRadius(6f, 6f))
        drawRoundRect(coverColor, Offset(left, top), Size(nbW, nbH), CornerRadius(6f, 6f))
        // Spine strip
        drawRoundRect(coverColor.copy(alpha = a * 0.8f),
            Offset(left, top), Size(nbW * 0.1f, nbH), CornerRadius(6f, 6f))
        // Page
        val pi = nbW * 0.13f
        val pageTop = top + nbH * 0.05f
        val pageH = nbH * 0.9f
        val pageW = nbW - pi - 4f
        drawRoundRect(pageColor, Offset(left + pi, pageTop), Size(pageW, pageH), CornerRadius(3f, 3f))

        // Ruled lines
        for (i in 1..6) {
            val ly = pageTop + (pageH / 7) * i
            drawLine(lineColor, Offset(left + pi + 6f, ly), Offset(left + pi + pageW - 6f, ly), 0.8f * density)
        }

        // Handwriting scribbles on lines
        val scribbleColor = Color(0xFF333366).copy(alpha = a * 1.5f)
        for (i in 1..4) {
            val ly = pageTop + (pageH / 7) * i
            val startX = left + pi + 10f + (i * 7f)
            val endX = left + pi + pageW - 14f - (i * 11f)
            val scribble = Path().apply {
                moveTo(startX, ly - 2f)
                cubicTo(startX + (endX - startX) * 0.25f, ly - 4f + (i % 2) * 6f,
                    startX + (endX - startX) * 0.55f, ly + 3f - (i % 3) * 4f,
                    endX, ly - 1f + (i % 2) * 3f)
            }
            drawPath(scribble, scribbleColor, style = Stroke(0.8f * density, cap = StrokeCap.Round))
        }

        // "Tagebuch" title
        drawContext.canvas.nativeCanvas.drawText(
            "Tagebuch", cx + pi * 0.2f, pageTop + pageH * 0.12f,
            android.graphics.Paint().apply {
                color = android.graphics.Color.argb(textAlpha, 50, 35, 15)
                textSize = nbSize * 0.15f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
            }
        )

        // Pen ✏️ — visible pencil shape
        val penLen = nbH * 0.55f
        val penX1 = cx + nbW * 0.15f
        val penY1 = cy + nbH * 0.3f
        val penX2 = penX1 + penLen * 0.55f
        val penY2 = penY1 - penLen * 0.45f
        // Pencil body
        drawLine(penBodyColor, Offset(penX1, penY1), Offset(penX2, penY2), 3.5f * density, StrokeCap.Round)
        // Pencil eraser (pink)
        drawCircle(Color(0xFFE88B8B).copy(alpha = a * 2f), 2.5f * density, Offset(penX2, penY2))
        // Pencil tip (dark)
        drawCircle(Color(0xFF222222).copy(alpha = a * 2f), 1.8f * density, Offset(penX1 - 1f, penY1 + 1f))
        // Pencil ferrule (gold band)
        val midX = (penX1 + penX2) * 0.72f
        val midY = (penY1 + penY2) * 0.72f
        drawCircle(Color(0xFFDAA520).copy(alpha = a * 2f), 2f * density, Offset(midX, midY))
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
