package com.nems.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NeonCosmosDarkScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CosmosBackground,
    primaryContainer = Color(0xFF003544),
    onPrimaryContainer = Color(0xFFB0F0FF),
    secondary = NeonPurple,
    onSecondary = CosmosBackground,
    secondaryContainer = Color(0xFF3D2060),
    onSecondaryContainer = Color(0xFFE8D0FF),
    tertiary = NeonGreen,
    onTertiary = CosmosBackground,
    tertiaryContainer = Color(0xFF0A3A1A),
    onTertiaryContainer = Color(0xFFA0F0B0),
    background = CosmosBackground,
    onBackground = TextPrimary,
    surface = CosmosSurface,
    onSurface = TextPrimary,
    surfaceVariant = CosmosSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = Color(0xFF1F1F35),
    error = StatusRed,
    onError = Color.White,
    errorContainer = Color(0xFF3D0A0A),
    onErrorContainer = Color(0xFFFFB0A0),
)

private val NeonCosmosLightScheme = lightColorScheme(
    primary = Color(0xFF006878),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB0F0FF),
    onPrimaryContainer = Color(0xFF001F26),
    secondary = Color(0xFF7B3FAF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8D0FF),
    onSecondaryContainer = Color(0xFF2A0050),
    tertiary = Color(0xFF1A7A2E),
    onTertiary = Color.White,
    background = Color(0xFFF8F8FC),
    onBackground = Color(0xFF1A1A2E),
    surface = Color.White,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFFE8E8F0),
    onSurfaceVariant = Color(0xFF48485A),
    outline = Color(0xFFB0B0C0),
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

    val colorScheme = if (darkTheme) NeonCosmosDarkScheme else NeonCosmosLightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NmsTypography,
        shapes = NmsShapes,
        content = content,
    )
}
