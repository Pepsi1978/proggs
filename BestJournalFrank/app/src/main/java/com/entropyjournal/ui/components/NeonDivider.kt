package com.entropyjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.entropyjournal.ui.theme.NeonCyan

@Composable
fun NeonDivider(
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    thickness: Dp = 1.dp,
    horizontalPadding: Dp = 16.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .height(thickness)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        color.copy(alpha = 0.6f),
                        color,
                        color.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
    )
}
