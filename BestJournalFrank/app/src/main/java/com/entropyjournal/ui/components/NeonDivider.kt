package com.entropyjournal.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun NeonDivider(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE8B547),
    thickness: Dp = 2.dp,
    horizontalPadding: Dp = 16.dp,
) {
    val transition = rememberInfiniteTransition(label = "goldHairline")
    val shimmer by
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)),
            label = "goldHairlineOffset",
        )
    Box(
        modifier =
            modifier.fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .height(thickness)
                .background(
                    Brush.horizontalGradient(
                        colors =
                            listOf(
                                Color.Transparent,
                                color,
                                Color(0xFFDF741E),
                                color,
                                Color.Transparent,
                            ),
                        startX = (shimmer * 800f) - 400f,
                        endX = (shimmer * 800f) + 400f,
                    )
                )
    )
}
