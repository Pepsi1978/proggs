package de.frank.cortex.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkScheme = darkColorScheme(
    primary = Iris,
    onPrimary = DarkText,
    secondary = Mint,
    onSecondary = DarkText,
    tertiary = Amber,
    onTertiary = DarkText,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkRaised,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorderSoft,
    error = Rose,
    onError = DarkText,
    errorContainer = DarkField,
    onErrorContainer = Rose,
    inverseSurface = DarkText,
    inverseOnSurface = DarkBg,
)

private val LightScheme = lightColorScheme(
    primary = IrisLight,
    onPrimary = LightText,
    secondary = MintLight,
    onSecondary = LightText,
    tertiary = AmberLight,
    onTertiary = LightText,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightRaised,
    onSurfaceVariant = LightMuted,
    outline = LightBorder,
    outlineVariant = LightBorderSoft,
    error = RoseLight,
    onError = LightText,
    errorContainer = LightField,
    onErrorContainer = RoseLight,
    inverseSurface = LightText,
    inverseOnSurface = LightBg,
)

@Composable
fun CortexTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CortexTypography,
        content = content
    )
}
