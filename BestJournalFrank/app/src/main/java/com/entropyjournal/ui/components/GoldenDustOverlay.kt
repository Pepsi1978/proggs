package com.entropyjournal.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Composable
fun GoldenDustOverlay(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    val transition = rememberInfiniteTransition(label = "goldenDust")
    @Composable
    fun alpha(duration: Int, initial: Float): androidx.compose.runtime.State<Float> =
        transition.animateFloat(
            initialValue = initial,
            targetValue = 0.85f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(duration / 2, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "dust$duration",
        )

    val alpha1 by alpha(4000, 0.12f)
    val alpha2 by alpha(5000, 0.22f)
    val alpha3 by alpha(4400, 0.32f)
    val alpha4 by alpha(3600, 0.18f)
    val alpha5 by alpha(4800, 0.38f)
    Canvas(modifier = modifier) {
        val points =
            listOf(
                Triple(Offset(size.width * 0.14f, size.height * 0.12f), 2f, alpha1),
                Triple(Offset(size.width * 0.82f, size.height * 0.24f), 2f, alpha2),
                Triple(Offset(size.width * 0.08f, size.height * 0.55f), 1.5f, alpha3),
                Triple(Offset(size.width * 0.88f, size.height * 0.70f), 1.5f, alpha4),
                Triple(Offset(size.width * 0.40f, size.height * 0.86f), 2f, alpha5),
            )
        points.forEach { (center, radius, pointAlpha) ->
            drawCircle(color = color.copy(alpha = pointAlpha), radius = radius, center = center)
        }
    }
}
