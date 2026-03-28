package com.entropyjournal.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { true }

private val WarmDarkScheme = darkColorScheme(
    primary = WarmCopper,
    onPrimary = Color.White,
    primaryContainer = CosmosSurface,
    onPrimaryContainer = WarmSand,
    secondary = WarmSand,
    onSecondary = CosmosBlack,
    secondaryContainer = CosmosSurface.copy(alpha = 0.7f),
    onSecondaryContainer = WarmSand,
    tertiary = WarmGold,
    onTertiary = CosmosBlack,
    tertiaryContainer = CosmosSurface.copy(alpha = 0.5f),
    onTertiaryContainer = WarmGold,
    error = NeonRed,
    onError = Color.White,
    errorContainer = NeonRed.copy(alpha = 0.15f),
    onErrorContainer = NeonRed,
    background = CosmosBlack,
    onBackground = TextPrimary,
    surface = CosmosDeep,
    onSurface = TextPrimary,
    surfaceVariant = CosmosLayer,
    onSurfaceVariant = TextSecondary,
    outline = TextMuted,
    outlineVariant = GlassHighlight,
    inverseSurface = TextPrimary,
    inverseOnSurface = CosmosBlack,
    surfaceTint = WarmCopper
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0097A7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBF2),
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
    surfaceTint = Color(0xFF0097A7)
)

@Composable
fun EntropyJournalTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WarmDarkScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
