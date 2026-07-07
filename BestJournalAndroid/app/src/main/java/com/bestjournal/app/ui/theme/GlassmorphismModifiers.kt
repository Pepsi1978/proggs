package com.bestjournal.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassCard(
    glowColor: Color = NeonCyan,
    glowIntensity: Float = 0.15f,
    cornerRadius: Dp = 20.dp,
    isDark: Boolean = true
): Modifier = if (isDark) {
    // Dark mode: solid card color, no gradient, no border — Spotify style
    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(CardSurface)
} else {
    this
        .shadow(
            elevation = 2.dp,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = Color.Black.copy(alpha = 0.06f),
            spotColor = Color.Black.copy(alpha = 0.04f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFFAFAFC))))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(Color(0x18000000), Color.Transparent)),
            shape = RoundedCornerShape(cornerRadius)
        )
}

fun Modifier.glassCardElevated(
    glowColor: Color = NeonCyan,
    glowIntensity: Float = 0.25f,
    cornerRadius: Dp = 20.dp,
    isDark: Boolean = true
): Modifier = if (isDark) {
    // Dark mode: slightly brighter solid card, no gradient, no border
    this
        .clip(RoundedCornerShape(cornerRadius))
        .background(CardElevated)
} else {
    this
        .shadow(
            elevation = 4.dp,
            shape = RoundedCornerShape(cornerRadius),
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.06f)
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFF8F8FC))))
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(listOf(Color(0x20000000), Color.Transparent, Color(0x08000000))),
            shape = RoundedCornerShape(cornerRadius)
        )
}
