package de.frank.entropyreducer.presentation.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Eigene Theme-Erweiterungen — "Glut"-Design (2026-06-12, Mockup docs/mockup-neues-design.html).
 * Statt Material You / dynamicColor, weil das Design fix ist.
 *
 * NEU (Glut): Die Akzente sind THEME-ABHAENGIG — Dark nutzt die helleren, Light die tieferen
 * Varianten (exakt wie im Mockup). Composables greifen ueber LocalCosmos.current.accent /
 * accentTasks / accentAnalyse / accentForscher / accentBio / ok / warn / crit zu.
 */
data class CosmosThemeExt(
    val isDark: Boolean,
    val backgroundBrush: Brush,
    val glassBg: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    // Glut: theme-abhaengige Akzente
    val accent: Color, // Orange-Glut (Primaer)
    val accentTasks: Color, // Tab-Klasse Aufgaben (= accent)
    val accentTasksSub: Color, // Sub-Tabs unter Aufgaben (Blau)
    val accentAnalyse: Color, // Tab-Klasse Analyse (Smaragd)
    val accentForscher: Color, // Tab-Klasse Forscher (Violett)
    val accentBio: Color, // Tab-Klasse Biomarker (Rosé)
    val ok: Color,
    val warn: Color,
    val crit: Color,
    val barBg: Color, // BottomBar / erhoehte Flaechen (Mockup bg2)
    val dialogBg: Color, // Dialoge / Sheets (Mockup card2)
)

val LocalCosmos =
    staticCompositionLocalOf<CosmosThemeExt> {
        error(
            "CosmosThemeExt nicht im Composition vorhanden — wickele deinen Code in EntropieReductorTheme {}."
        )
    }

private val DarkScheme =
    darkColorScheme(
        primary = CosmosColors.AccentPrimary,
        onPrimary = Color(0xFF1C0E03),
        secondary = CosmosColors.AccentSecondary,
        onSecondary = CosmosColors.BgDark,
        tertiary = CosmosColors.Success,
        background = CosmosColors.BgDarkMid,
        onBackground = CosmosColors.TextPrimaryDark,
        surface = CosmosColors.GlassDark,
        onSurface = CosmosColors.TextPrimaryDark,
        surfaceVariant = CosmosColors.BgDarkAccent,
        onSurfaceVariant = CosmosColors.TextSecondaryDark,
        error = CosmosColors.Critical,
        onError = CosmosColors.TextPrimaryDark,
    )

private val LightScheme =
    lightColorScheme(
        primary = CosmosColors.AccentPrimaryLight,
        onPrimary = Color(0xFFFFF7F0),
        secondary = CosmosColors.AccentSecondaryLight,
        onSecondary = Color(0xFFFFF7F0),
        tertiary = CosmosColors.SuccessLight,
        background = CosmosColors.BgLight,
        onBackground = CosmosColors.TextPrimaryLight,
        surface = CosmosColors.GlassLight,
        onSurface = CosmosColors.TextPrimaryLight,
        surfaceVariant = CosmosColors.BgLightAccent,
        onSurfaceVariant = CosmosColors.TextSecondaryLight,
        error = CosmosColors.CriticalLight,
        onError = Color(0xFFFFF7F0),
    )

@Composable
fun EntropieReductorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme

    // Glut: flacher, warmer Hintergrund (Mockup --bg) statt Galaxy-Radialverlauf.
    val ext =
        if (darkTheme) {
            CosmosThemeExt(
                isDark = true,
                backgroundBrush = SolidColor(CosmosColors.BgDarkMid),
                glassBg = CosmosColors.GlassDark,
                glassBorder = CosmosColors.GlassDarkBorder,
                textPrimary = CosmosColors.TextPrimaryDark,
                textSecondary = CosmosColors.TextSecondaryDark,
                accent = CosmosColors.AccentPrimary,
                accentTasks = CosmosColors.TabTasks,
                accentTasksSub = CosmosColors.TabTasksSub,
                accentAnalyse = CosmosColors.TabAnalyse,
                accentForscher = CosmosColors.TabForscher,
                accentBio = CosmosColors.TabBio,
                ok = CosmosColors.Success,
                warn = CosmosColors.Warning,
                crit = CosmosColors.Critical,
                barBg = CosmosColors.BgDarkBar,
                dialogBg = CosmosColors.BgDarkAccent,
            )
        } else {
            CosmosThemeExt(
                isDark = false,
                backgroundBrush = SolidColor(CosmosColors.BgLight),
                glassBg = CosmosColors.GlassLight,
                glassBorder = CosmosColors.GlassLightBorder,
                textPrimary = CosmosColors.TextPrimaryLight,
                textSecondary = CosmosColors.TextSecondaryLight,
                accent = CosmosColors.AccentPrimaryLight,
                accentTasks = CosmosColors.TabTasksLight,
                accentTasksSub = CosmosColors.TabTasksSubLight,
                accentAnalyse = CosmosColors.TabAnalyseLight,
                accentForscher = CosmosColors.TabForscherLight,
                accentBio = CosmosColors.TabBioLight,
                ok = CosmosColors.SuccessLight,
                warn = CosmosColors.WarningLight,
                crit = CosmosColors.CriticalLight,
                barBg = CosmosColors.BgLightAccent,
                dialogBg = Color.White,
            )
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalCosmos provides ext) {
        MaterialTheme(
            colorScheme = scheme,
            typography = CosmosTypography,
            content = {
                Box(modifier = Modifier.fillMaxSize().background(ext.backgroundBrush)) { content() }
            },
        )
    }
}
