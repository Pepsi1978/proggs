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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/**
 * Eigene Theme-Erweiterungen — Brushes, Glas-Farben, etc. Statt Material You / dynamicColor, weil
 * das Design fix ist (siehe Spec §2).
 */
data class CosmosThemeExt(
    val isDark: Boolean,
    val backgroundBrush: Brush,
    val glassBg: androidx.compose.ui.graphics.Color,
    val glassBorder: androidx.compose.ui.graphics.Color,
    val textPrimary: androidx.compose.ui.graphics.Color,
    val textSecondary: androidx.compose.ui.graphics.Color,
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
        onPrimary = CosmosColors.BgDark,
        secondary = CosmosColors.AccentSecondary,
        onSecondary = CosmosColors.BgDark,
        tertiary = CosmosColors.Success,
        background = CosmosColors.BgDark,
        onBackground = CosmosColors.TextPrimaryDark,
        surface = CosmosColors.BgDarkMid,
        onSurface = CosmosColors.TextPrimaryDark,
        surfaceVariant = CosmosColors.BgDarkAccent,
        onSurfaceVariant = CosmosColors.TextSecondaryDark,
        error = CosmosColors.Critical,
        onError = CosmosColors.TextPrimaryDark,
    )

private val LightScheme =
    lightColorScheme(
        primary = CosmosColors.AccentPrimary,
        onPrimary = CosmosColors.BgDark,
        secondary = CosmosColors.AccentSecondary,
        onSecondary = CosmosColors.BgDark,
        tertiary = CosmosColors.Success,
        background = CosmosColors.BgLight,
        onBackground = CosmosColors.TextPrimaryLight,
        surface = CosmosColors.BgLight,
        onSurface = CosmosColors.TextPrimaryLight,
        surfaceVariant = CosmosColors.BgLightAccent,
        onSurfaceVariant = CosmosColors.TextSecondaryLight,
        error = CosmosColors.Critical,
        onError = CosmosColors.TextPrimaryDark,
    )

@Composable
fun EntropieReductorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme

    // Galaxy-Gradient-Radius dynamisch an Bildschirmbreite koppeln:
    // 1.5x die Bildschirmbreite wirkt auf S23 Ultra (~412dp), Fold 6 (~720dp)
    // und Tab S9 (~840dp+) konsistent — der Roentgen-Bericht hatte den
    // hardcoded 1500f-Pixel-Wert als Problem markiert.
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val gradientRadiusPx = with(density) { (config.screenWidthDp.dp * 1.5f).toPx() }

    val ext =
        CosmosThemeExt(
            isDark = darkTheme,
            backgroundBrush =
                if (darkTheme) {
                    // Radialer Verlauf von oben-mitte aus, Galaxy-Effekt
                    Brush.radialGradient(
                        0f to CosmosColors.BgDarkMid,
                        0.6f to CosmosColors.BgDark,
                        1f to CosmosColors.BgDark,
                        center = Offset.Unspecified,
                        radius = gradientRadiusPx,
                    )
                } else {
                    Brush.verticalGradient(
                        0f to CosmosColors.BgLight,
                        1f to CosmosColors.BgLightMid,
                    )
                },
            glassBg = if (darkTheme) CosmosColors.GlassDark else CosmosColors.GlassLight,
            glassBorder =
                if (darkTheme) CosmosColors.GlassDarkBorder else CosmosColors.GlassLightBorder,
            textPrimary =
                if (darkTheme) CosmosColors.TextPrimaryDark else CosmosColors.TextPrimaryLight,
            textSecondary =
                if (darkTheme) CosmosColors.TextSecondaryDark else CosmosColors.TextSecondaryLight,
        )

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
