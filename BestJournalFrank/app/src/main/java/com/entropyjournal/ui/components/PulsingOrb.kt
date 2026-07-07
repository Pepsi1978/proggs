package com.entropyjournal.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonViolet

@Composable
fun PulsingOrb(
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    secondaryColor: Color = NeonViolet,
    size: Dp = 48.dp,
    entropyLevel: Float = 0.5f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (2000 - (entropyLevel * 1000).toInt()).coerceAtLeast(500),
                easing = EaseInOutSine
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val secondaryPulse by infiniteTransition.animateFloat(
        initialValue = 1.1f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondaryPulse"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val baseRadius = this.size.minDimension / 4f

        // Outer glow
        drawCircle(
            color = color.copy(alpha = 0.1f * pulse),
            radius = baseRadius * 2f * pulse,
            center = center
        )

        // Secondary ring
        drawCircle(
            color = secondaryColor.copy(alpha = 0.15f * secondaryPulse),
            radius = baseRadius * 1.5f * secondaryPulse,
            center = center
        )

        // Core
        drawCircle(
            color = color.copy(alpha = 0.6f + entropyLevel * 0.3f),
            radius = baseRadius * pulse,
            center = center
        )

        // Bright center
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = baseRadius * 0.3f,
            center = center
        )
    }
}
