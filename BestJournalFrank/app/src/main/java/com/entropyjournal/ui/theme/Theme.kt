package com.entropyjournal.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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

/**
 * Neutrales ColorScheme — wenn der Benutzer im Themes Manager "Neutral" waehlt.
 * Verwendet die Frank-Default-Farben (CosmosBlack/Light) ohne Profil-Akzent.
 */
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

@Composable
fun EntropyJournalTheme(
    darkTheme: Boolean = true,
    profileIndex: Int = ProfileTheme.currentProfileIndex.intValue,
    appTheme: AppTheme = ProfileTheme.currentAppTheme.value,
    content: @Composable () -> Unit,
) {
    // Theme-Auswahl 1:1 wie BestJournalAndroid. Profilfarbe (Default) leitet die ganze App-Farbe
    // vom aktiven KI-Dashboard-Profil ab. Die fuenf Klassiker-Themes nutzen ihre offiziellen
    // Light/Dark Specs. Neutral nutzt Frank's Default-Cosmos-Farben.
    val colorScheme =
        when (appTheme) {
            AppTheme.Profile -> profileColorScheme(profileAccent(profileIndex), darkTheme)
            AppTheme.Solarized -> if (darkTheme) SolarizedDarkScheme else SolarizedLightScheme
            AppTheme.Dracula -> if (darkTheme) DraculaDarkScheme else DraculaLightScheme
            AppTheme.OneDark -> if (darkTheme) OneDarkScheme else OneLightScheme
            AppTheme.Nord -> if (darkTheme) NordDarkScheme else NordLightScheme
            AppTheme.Gruvbox -> if (darkTheme) GruvboxDarkScheme else GruvboxLightScheme
            AppTheme.Cosmos -> if (darkTheme) CosmosDarkScheme else CosmosLightScheme
            AppTheme.Neutral -> if (darkTheme) NeutralDarkScheme else NeutralLightScheme
            AppTheme.Aurora -> if (darkTheme) AuroraDarkScheme else AuroraLightScheme
        }

    // Cosmos-Theme: 1:1 wie EntropieReductor — radialer (Dark) bzw. vertikaler (Light)
    // Background-Brush als Box-Wrapper, plus transparente StatusBar/NavBar.
    // Aurora-Theme: gleicher Mechanismus, aber mit 3-Stop-Diagonalgradient (Pastel-Aquarell).
    val isCosmos = appTheme == AppTheme.Cosmos
    val isAurora = appTheme == AppTheme.Aurora
    val needsTransparentSystemBars = isCosmos || isAurora
    if (needsTransparentSystemBars) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window
                if (window != null) {
                    window.statusBarColor = android.graphics.Color.TRANSPARENT
                    window.navigationBarColor = android.graphics.Color.TRANSPARENT
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
            }
        }
    }

    val cosmosBrush: Brush? =
        if (isCosmos) {
            if (darkTheme) {
                Brush.radialGradient(
                    0f to ERBgDarkMid,
                    0.6f to ERBgDark,
                    1f to ERBgDark,
                    center = Offset.Unspecified,
                    radius = 1500f,
                )
            } else {
                Brush.verticalGradient(
                    0f to ERBgLight,
                    1f to ERBgLightMid,
                )
            }
        } else {
            null
        }

    // Aurora: 3-Stop Linear-Gradient von oben-links nach unten-rechts.
    // Light = pastellig (Mint → Lavendel → Rosa), Dark = Nachthimmel (Tiefblaugruen → Indigo → Bordeaux).
    val auroraBrush: Brush? =
        if (isAurora) {
            if (darkTheme) {
                Brush.linearGradient(
                    0f to AuroraDarkGradientStart,
                    0.5f to AuroraDarkGradientMid,
                    1f to AuroraDarkGradientEnd,
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            } else {
                Brush.linearGradient(
                    0f to AuroraLightGradientStart,
                    0.5f to AuroraLightGradientMid,
                    1f to AuroraLightGradientEnd,
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            }
        } else {
            null
        }

    val backgroundBrush: Brush? = cosmosBrush ?: auroraBrush

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = {
                if (backgroundBrush != null) {
                    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
                        content()
                    }
                } else {
                    content()
                }
            },
        )
    }
}
