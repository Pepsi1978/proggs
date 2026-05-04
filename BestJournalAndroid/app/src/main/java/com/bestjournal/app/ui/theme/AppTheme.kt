package com.bestjournal.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Neutral theme — extracted from the original WarmDarkScheme + LightColorScheme.
private val NeutralDarkScheme: ColorScheme =
    darkColorScheme(
        primary = WarmCopper,
        onPrimary = Color.White,
        primaryContainer = Color(0xFF3D2800),
        onPrimaryContainer = Color(0xFFFFDDB3),
        secondary = WarmSand,
        onSecondary = CosmosBlack,
        secondaryContainer = Color(0xFF2A2A2A),
        onSecondaryContainer = WarmSand,
        tertiary = WarmGold,
        onTertiary = CosmosBlack,
        tertiaryContainer = Color(0xFF2A2200),
        onTertiaryContainer = Color(0xFFFFE08A),
        error = NeonRed,
        onError = Color.White,
        errorContainer = Color(0xFF3B1010),
        onErrorContainer = NeonRed,
        background = CosmosBlack,
        onBackground = TextPrimary,
        surface = CosmosBlack,
        onSurface = TextPrimary,
        surfaceVariant = CosmosDeep,
        onSurfaceVariant = TextSecondary,
        outline = TextMuted,
        outlineVariant = Color(0xFF2A2A2A),
        inverseSurface = TextPrimary,
        inverseOnSurface = CosmosBlack,
        surfaceTint = WarmCopper,
    )

private val NeutralLightScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF00796B),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFA7D8D0),
        onPrimaryContainer = Color(0xFF00363D),
        secondary = Color(0xFF5E35B1),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEDE7F6),
        onSecondaryContainer = Color(0xFF311B92),
        tertiary = Color(0xFFC2185B),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFF8BBD0),
        onTertiaryContainer = Color(0xFF880E4F),
        error = Color(0xFFD32F2F),
        onError = Color.White,
        errorContainer = Color(0xFFFFCDD2),
        onErrorContainer = Color(0xFFB71C1C),
        background = LightBackground,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightTextSecondary,
        outline = LightTextMuted,
        outlineVariant = Color(0xFFD8D8E0),
        inverseSurface = LightTextPrimary,
        inverseOnSurface = LightBackground,
        surfaceTint = Color(0xFF00796B),
    )

/**
 * Named app themes. Each theme bundles a dark + light ColorScheme. New themes can be added by
 * declaring more enum entries — the dropdown in SettingsScreen iterates AppTheme.entries.
 */
enum class AppTheme(
    val storageKey: String,
    val darkScheme: ColorScheme,
    val lightScheme: ColorScheme,
) {
    Neutral("neutral", NeutralDarkScheme, NeutralLightScheme);

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.storageKey == key } ?: Neutral
    }
}
