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
            Brush.verticalGradient(
                0.0f to GlassWhite,
                0.3f to GlassWhite.copy(alpha = 0.07f),
                0.7f to GlassHighlight.copy(alpha = 0.04f),
                1.0f to GlassHighlight
            )
        } else {
            Brush.verticalGradient(listOf(Color.White, Color(0xFFFAFAFC)))
        }
    )
    .border(
        width = 0.5.dp,
        brush = if (isDark) {
            Brush.verticalGradient(
                0.0f to GlassBorder.copy(alpha = 0.12f),
                0.5f to GlassBorder.copy(alpha = 0.06f),
                1.0f to Color.Transparent
            )
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
                0.0f to GlassWhite.copy(alpha = 0.12f),
                0.3f to GlassWhite.copy(alpha = 0.08f),
                0.7f to GlassHighlight.copy(alpha = 0.04f),
                1.0f to GlassHighlight
            )
        } else {
            Brush.verticalGradient(listOf(Color.White, Color(0xFFF8F8FC)))
        }
    )
    .border(
        width = 0.5.dp,
        brush = if (isDark) {
            Brush.verticalGradient(
                0.0f to GlassBorder.copy(alpha = 0.18f),
                0.5f to GlassBorder.copy(alpha = 0.08f),
                1.0f to Color.Transparent
            )
        } else {
            Brush.linearGradient(listOf(Color(0x20000000), Color.Transparent, Color(0x08000000)))
        },
        shape = RoundedCornerShape(cornerRadius)
    )
