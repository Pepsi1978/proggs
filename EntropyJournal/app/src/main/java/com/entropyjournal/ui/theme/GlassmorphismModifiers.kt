package com.entropyjournal.ui.theme

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
): Modifier = this
    .shadow(
        elevation = if (isDark) 8.dp else 2.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = if (isDark) glowColor.copy(alpha = glowIntensity) else Color.Black.copy(alpha = 0.06f),
        spotColor = if (isDark) glowColor.copy(alpha = glowIntensity) else Color.Black.copy(alpha = 0.04f)
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        if (isDark) {
            Brush.verticalGradient(listOf(GlassWhite, GlassHighlight))
        } else {
            Brush.verticalGradient(listOf(Color.White, Color(0xFFFAFAFC)))
        }
    )
    .border(
        width = 1.dp,
        brush = if (isDark) {
            Brush.linearGradient(listOf(GlassBorder, Color.Transparent))
        } else {
            Brush.linearGradient(listOf(Color(0x18000000), Color.Transparent))
        },
        shape = RoundedCornerShape(cornerRadius)
    )

fun Modifier.glassCardElevated(
    glowColor: Color = NeonCyan,
    glowIntensity: Float = 0.25f,
    cornerRadius: Dp = 20.dp,
    isDark: Boolean = true
): Modifier = this
    .shadow(
        elevation = if (isDark) 16.dp else 4.dp,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = if (isDark) glowColor.copy(alpha = glowIntensity) else Color.Black.copy(alpha = 0.08f),
        spotColor = if (isDark) glowColor.copy(alpha = glowIntensity) else Color.Black.copy(alpha = 0.06f)
    )
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        if (isDark) {
            Brush.verticalGradient(
                listOf(GlassWhite.copy(alpha = 0.15f), GlassHighlight)
            )
        } else {
            Brush.verticalGradient(listOf(Color.White, Color(0xFFF8F8FC)))
        }
    )
    .border(
        width = 1.dp,
        brush = if (isDark) {
            Brush.linearGradient(
                listOf(GlassBorder.copy(alpha = 0.3f), Color.Transparent, GlassBorder.copy(alpha = 0.1f))
            )
        } else {
            Brush.linearGradient(listOf(Color(0x20000000), Color.Transparent, Color(0x08000000)))
        },
        shape = RoundedCornerShape(cornerRadius)
    )
