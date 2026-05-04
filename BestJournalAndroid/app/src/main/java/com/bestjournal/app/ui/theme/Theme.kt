package com.bestjournal.app.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalIsDarkTheme = staticCompositionLocalOf { true }

// ────────────────────────────────────────────────────────────────────────────
// Neutral theme — original static schemes (WarmDark + LightColor)
// ────────────────────────────────────────────────────────────────────────────

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

// ────────────────────────────────────────────────────────────────────────────
// Profile theme — derived from active dashboard profile (1:1 from BestJournalFrank)
// ────────────────────────────────────────────────────────────────────────────

/** Akzentfarbe pro KI-Dashboard-Profil. */
fun profileAccent(profileIndex: Int): Color =
    when (profileIndex) {
        0 -> SummaryPalette.accent // Teal — Zusammenfassung
        1 -> WarmCopper // Copper — Räume dein Leben auf
        2 -> InsightPalette.primary // Violet — Selbsterkenntnis
        3 -> GoalPalette.primary // Emerald — Persönliche Ziele
        else -> CustomPalette.primary // Amber — Custom
    }

private fun Color.lighten(amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * a,
        green = green + (1f - green) * a,
        blue = blue + (1f - blue) * a,
        alpha = alpha,
    )
}

private fun Color.darken(amount: Float): Color {
    val a = (1f - amount).coerceIn(0f, 1f)
    return Color(red = red * a, green = green * a, blue = blue * a, alpha = alpha)
}

private fun Color.mix(other: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - r) + other.red * r,
        green = green * (1f - r) + other.green * r,
        blue = blue * (1f - r) + other.blue * r,
        alpha = alpha,
    )
}

/**
 * Baut ein vollständiges Material3 ColorScheme aus EINER Akzentfarbe + Mode. Background und Surface
 * bekommen einen sehr dezenten Hauch der Akzentfarbe (3-7%).
 */
fun profileColorScheme(accent: Color, isDark: Boolean): ColorScheme {
    return if (isDark) {
        val background = Color(0xFF121212).mix(accent, 0.04f)
        val surface = Color(0xFF181818).mix(accent, 0.05f)
        val surfaceVariant = Color(0xFF222222).mix(accent, 0.07f)
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.darken(0.55f),
            onPrimaryContainer = accent.lighten(0.55f),
            secondary = accent.lighten(0.25f),
            onSecondary = Color(0xFF101010),
            secondaryContainer = accent.darken(0.65f),
            onSecondaryContainer = accent.lighten(0.65f),
            tertiary = accent.mix(Color(0xFFE0DCD4), 0.35f),
            onTertiary = Color(0xFF101010),
            tertiaryContainer = accent.darken(0.7f),
            onTertiaryContainer = accent.lighten(0.7f),
            error = NeonRed,
            onError = Color.White,
            errorContainer = Color(0xFF3B1010),
            onErrorContainer = NeonRed,
            background = background,
            onBackground = TextPrimary,
            surface = surface,
            onSurface = TextPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = TextSecondary,
            outline = TextMuted,
            outlineVariant = Color(0xFF2A2A2A),
            inverseSurface = TextPrimary,
            inverseOnSurface = background,
            surfaceTint = accent,
        )
    } else {
        val background = Color(0xFFF8F8FC).mix(accent, 0.05f)
        val surface = Color(0xFFFFFFFF).mix(accent, 0.03f)
        val surfaceVariant = Color(0xFFF0F0F5).mix(accent, 0.08f)
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.lighten(0.7f),
            onPrimaryContainer = accent.darken(0.5f),
            secondary = accent.darken(0.15f),
            onSecondary = Color.White,
            secondaryContainer = accent.lighten(0.78f),
            onSecondaryContainer = accent.darken(0.55f),
            tertiary = accent.mix(Color(0xFF5E35B1), 0.5f),
            onTertiary = Color.White,
            tertiaryContainer = accent.lighten(0.82f),
            onTertiaryContainer = accent.darken(0.6f),
            error = Color(0xFFD32F2F),
            onError = Color.White,
            errorContainer = Color(0xFFFFCDD2),
            onErrorContainer = Color(0xFFB71C1C),
            background = background,
            onBackground = LightTextPrimary,
            surface = surface,
            onSurface = LightTextPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = LightTextSecondary,
            outline = LightTextMuted,
            outlineVariant = Color(0xFFD8D8E0),
            inverseSurface = LightTextPrimary,
            inverseOnSurface = background,
            surfaceTint = accent,
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Composable entry point
// ────────────────────────────────────────────────────────────────────────────

@Composable
fun BestJournalTheme(
    darkTheme: Boolean = true,
    // Default = true to match the ORIGINAL pre-themes-manager behavior: on Android 12+ the app
    // picked up the user's wallpaper-based Material You colors. The Profile theme bypasses this
    // intentionally so the dashboard accent color always wins.
    dynamicColor: Boolean = true,
    appTheme: AppTheme = AppTheme.Neutral,
    profileIndex: Int = 0,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        when (appTheme) {
            // Profile theme wins over dynamicColor — its whole purpose is to drive the look from
            // the active dashboard profile.
            AppTheme.Profile -> profileColorScheme(profileAccent(profileIndex), darkTheme)
            AppTheme.Solarized -> if (darkTheme) SolarizedDarkScheme else SolarizedLightScheme
            AppTheme.Dracula -> if (darkTheme) DraculaDarkScheme else DraculaLightScheme
            AppTheme.OneDark -> if (darkTheme) OneDarkScheme else OneLightScheme
            AppTheme.Nord -> if (darkTheme) NordDarkScheme else NordLightScheme
            AppTheme.Gruvbox -> if (darkTheme) GruvboxDarkScheme else GruvboxLightScheme
            AppTheme.Neutral -> {
                if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (darkTheme) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)
                } else if (darkTheme) {
                    NeutralDarkScheme
                } else {
                    NeutralLightScheme
                }
            }
        }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
