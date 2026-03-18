package com.quizverse.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Circular countdown timer with a thick ring, glow at low time, and pulse at ≤5s.
 *
 * Colour transitions:
 *   green  (> 50 %)  →  yellow  (20–50 %)  →  red  (≤ 20 %)
 *
 * A glow layer appears when time is running low.
 * A pulsing scale animation activates in the last 5 seconds.
 *
 * @param remainingSeconds  How many seconds are left.
 * @param totalSeconds      The full duration of the timer.
 * @param size              Diameter of the circular widget.
 * @param modifier          Optional layout modifier.
 */
@Composable
fun AnimatedTimer(
    remainingSeconds: Int,
    totalSeconds: Int,
    size: Dp = 80.dp,
    modifier: Modifier = Modifier,
) {
    // Fraction of time remaining [0..1]
    val fraction = (remainingSeconds.toFloat() / totalSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)

    // Smooth arc progress animation
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "timerArc",
    )

    // Colour: green → yellow → red
    val arcColor by animateColorAsState(
        targetValue = when {
            fraction > 0.5f -> Color(0xFF4ADE80)   // green
            fraction > 0.2f -> Color(0xFFFBBF24)   // yellow
            else            -> Color(0xFFF87171)    // red
        },
        animationSpec = tween(durationMillis = 400),
        label = "timerColor",
    )

    // Glow intensity: increases as time runs low
    val isLow = fraction <= 0.35f
    val isUrgent = remainingSeconds in 1..5

    // Glow pulse animation when time is low
    val glowAnim = rememberInfiniteTransition(label = "timerGlow")
    val glowAlpha by glowAnim.animateFloat(
        initialValue = 0f,
        targetValue = if (isLow) 0.55f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "timerGlowAlpha",
    )

    // Pulsing scale in last 5 seconds
    val pulseAnim = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (isUrgent) 1.14f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    val trackColor = Color.White.copy(alpha = 0.12f)
    // Thick stroke: 8.dp equivalent as fraction of size
    val strokeWidthDp = 8.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .scale(pulseScale),
    ) {
        // Glow layer behind the arc (soft blurred halo)
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidthDp.toPx()
            val glowStroke = strokePx * 3.5f
            val padding = glowStroke / 2f

            if (isLow && glowAlpha > 0f) {
                drawArc(
                    color = arcColor.copy(alpha = glowAlpha * 0.4f),
                    startAngle = -90f,
                    sweepAngle = animatedFraction * 360f,
                    useCenter = false,
                    style = Stroke(width = glowStroke, cap = StrokeCap.Round),
                    topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
                    size = this.size.copy(
                        width = this.size.width - glowStroke,
                        height = this.size.height - glowStroke,
                    ),
                )
            }
        }

        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidthDp.toPx()
            val padding = strokePx / 2f

            // Background track ring
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
                size = this.size.copy(
                    width = this.size.width - strokePx,
                    height = this.size.height - strokePx,
                ),
            )

            // Foreground progress arc (thick, prominent)
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = animatedFraction * 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(padding, padding),
                size = this.size.copy(
                    width = this.size.width - strokePx,
                    height = this.size.height - strokePx,
                ),
            )
        }

        // Centre time label
        Text(
            text = remainingSeconds.toString(),
            fontSize = (size.value * 0.28f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = arcColor,
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}
