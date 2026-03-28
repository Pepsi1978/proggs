package com.entropyjournal.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonMagenta
import com.entropyjournal.ui.theme.NeonViolet
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class SplashParticle(
    val startX: Float,
    val startY: Float,
    val angle: Float,
    val color: Color
)

@Composable
fun SplashScreen(
    onSplashFinished: (isSignedIn: Boolean) -> Unit,
    viewModel: SplashViewModel
) {
    val convergence = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    val particles = remember {
        val colors = listOf(NeonCyan, NeonViolet, NeonMagenta)
        List(100) {
            SplashParticle(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                angle = Random.nextFloat() * 6.28f,
                color = colors[it % 3].copy(alpha = 0.6f + Random.nextFloat() * 0.4f)
            )
        }
    }

    LaunchedEffect(Unit) {
        convergence.animateTo(
            targetValue = 1f,
            animationSpec = tween(2000, easing = FastOutSlowInEasing)
        )
        textAlpha.animateTo(1f, tween(500))
        delay(500)
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
            val progress = convergence.value

            particles.forEach { particle ->
                val startPx = particle.startX * size.width
                val startPy = particle.startY * size.height

                val spiralRadius = (1f - progress) * size.width * 0.5f
                val spiralAngle = particle.angle + progress * 4f * 3.14159f

                val targetX = centerX + cos(spiralAngle.toDouble()).toFloat() * spiralRadius * (1f - progress)
                val targetY = centerY + sin(spiralAngle.toDouble()).toFloat() * spiralRadius * (1f - progress)

                val x = startPx + (targetX - startPx) * progress
                val y = startPy + (targetY - startPy) * progress

                val particleRadius = (3f - progress * 2f).coerceAtLeast(1f)

                drawCircle(
                    color = particle.color,
                    radius = particleRadius * density,
                    center = Offset(x, y)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "E",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary.copy(alpha = convergence.value)
            )
            Text(
                text = "Entropy Journal",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = textAlpha.value),
                modifier = Modifier.offset(y = 8.dp)
            )
        }
    }
}
