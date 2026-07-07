package com.nems.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = staticCompositionLocalOf { true }

// ═══════════════════════════════════════════════════════════════
// DARK MODE — Spotify-Stil (exakte Hex-Codes aus BestJournal)
// ═══════════════════════════════════════════════════════════════
private val WarmDarkScheme = darkColorScheme(
    primary = WarmCopper,                          // #C25E00
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3D2800),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = WarmSand,                          // #E0DCD4
    onSecondary = CosmosBlack,                     // #121212
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = WarmSand,
    tertiary = WarmGold,                           // #8B6914
    onTertiary = CosmosBlack,
    tertiaryContainer = Color(0xFF2A2200),
    onTertiaryContainer = Color(0xFFFFE08A),
    error = NeonRed,                               // #FF5252
    onError = Color.White,
    errorContainer = Color(0xFF3B1010),
    onErrorContainer = NeonRed,
    background = CosmosBlack,                      // #121212
    onBackground = TextPrimary,                    // #E6E1E5
    surface = CosmosBlack,                         // #121212
    onSurface = TextPrimary,                       // #E6E1E5
    surfaceVariant = CosmosDeep,                   // #181818
    onSurfaceVariant = TextSecondary,              // #CAC4D0
    outline = TextMuted,                           // #938F99
    outlineVariant = Color(0xFF2A2A2A),
    inverseSurface = TextPrimary,
    inverseOnSurface = CosmosBlack,
    surfaceTint = WarmCopper,                      // #C25E00
)

// ═══════════════════════════════════════════════════════════════
// LIGHT MODE (exakte Hex-Codes aus BestJournal)
// ═══════════════════════════════════════════════════════════════
private val NmsLightScheme = lightColorScheme(
    primary = Color(0xFF00796B),                   // Teal
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA7D8D0),
    onPrimaryContainer = Color(0xFF00363D),
    secondary = Color(0xFF5E35B1),                 // Violett
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7F6),
    onSecondaryContainer = Color(0xFF311B92),
    tertiary = Color(0xFFC2185B),                  // Pink
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8BBD0),
    onTertiaryContainer = Color(0xFF880E4F),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFB71C1C),
    background = LightBackground,                  // #F8F8FC
    onBackground = LightTextPrimary,               // #1A1A2E
    surface = LightSurface,                        // #FFFFFF
    onSurface = LightTextPrimary,                  // #1A1A2E
    surfaceVariant = LightSurfaceVariant,          // #F0F0F5
    onSurfaceVariant = LightTextSecondary,         // #5A5A70
    outline = LightTextMuted,                      // #6E6E86
    outlineVariant = Color(0xFFD8D8E0),
    inverseSurface = LightTextPrimary,
    inverseOnSurface = LightBackground,
    surfaceTint = Color(0xFF00796B),               // Teal
)

enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Composable
fun NmsTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Use exact color palette — no dynamic/wallpaper colors
    val colorScheme = if (darkTheme) WarmDarkScheme else NmsLightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        LaunchedEffect(darkTheme) {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NmsTypography,
            shapes = NmsShapes,
            content = content,
        )
    }
}
