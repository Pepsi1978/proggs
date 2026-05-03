package com.entropyjournal.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalIsDarkTheme = staticCompositionLocalOf { true }

/**
 * Liefert die Akzentfarbe fuer ein KI-Dashboard-Profil. Identisch zu der
 * Logik in SettingsScreen und Info-Dialog (1:1 wie BestJournalAndroid).
 */
fun profileAccent(profileIndex: Int): Color =
    when (profileIndex) {
        0 -> SummaryPalette.accent       // Teal — Zusammenfassung
        1 -> WarmCopper                  // Copper — Raeume dein Leben auf
        2 -> InsightPalette.primary      // Violet — Selbsterkenntnis
        3 -> GoalPalette.primary         // Emerald — Persoenliche Ziele
        else -> CustomPalette.primary    // Amber — Custom
    }

// HSL-Helfer: Farben aufhellen / abdunkeln / mit Weiss bzw. Schwarz mischen.
// Wir nutzen das, um aus EINER Akzentfarbe ein vollstaendiges 14-Slot-
// ColorScheme abzuleiten (primary, secondary, tertiary, container, surface, ...).
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
 * Baut ein vollstaendiges Material3 ColorScheme aus EINER Akzentfarbe + Mode.
 * Die Akzentfarbe geht in primary, primaryContainer und surfaceTint. Background
 * und Surface bekommen einen sehr dezenten Hauch der Akzentfarbe (3-6%), damit
 * die ganze App im Profil-Look erscheint, ohne dass Text unleserlich wird.
 */
fun profileColorScheme(accent: Color, isDark: Boolean): ColorScheme {
    return if (isDark) {
        // Dark mode: dunkler Hintergrund mit minimalem Akzent-Hauch.
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
        // Light mode: helle Flaechen mit einem Hauch der Akzentfarbe.
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

@Composable
fun EntropyJournalTheme(
    darkTheme: Boolean = true,
    profileIndex: Int = ProfileTheme.currentProfileIndex.intValue,
    content: @Composable () -> Unit,
) {
    // Dynamic Color (Material You) wuerde unsere Profilfarbe ueberschreiben.
    // Deshalb bauen wir das Schema strikt aus der Profil-Akzentfarbe.
    val colorScheme = profileColorScheme(profileAccent(profileIndex), darkTheme)

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
