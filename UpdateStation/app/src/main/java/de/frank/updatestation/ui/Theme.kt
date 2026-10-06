package de.frank.updatestation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Farben {
    val kopfVerlauf: Brush
        @Composable get() = Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainerHigh),
        )
    val randVerlauf: Brush
        @Composable get() = Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
        )
}

private val Dunkel = darkColorScheme(
    primary = Color(0xFFFF9A3C),
    onPrimary = Color(0xFF291300),
    primaryContainer = Color(0xFF482508),
    onPrimaryContainer = Color(0xFFFFDFC1),
    secondary = Color(0xFFFFB877),
    onSecondary = Color(0xFF301800),
    secondaryContainer = Color(0xFF3D2816),
    onSecondaryContainer = Color(0xFFFFDFC1),
    tertiary = Color(0xFFFFC078),
    onTertiary = Color(0xFF2D1900),
    tertiaryContainer = Color(0xFF432B0E),
    onTertiaryContainer = Color(0xFFFFDFC1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color.Black,
    onBackground = Color(0xFFF5F0EB),
    surface = Color.Black,
    onSurface = Color(0xFFF5F0EB),
    surfaceVariant = Color(0xFF302A25),
    onSurfaceVariant = Color(0xFFD3C3B6),
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF35302C),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF101010),
    surfaceContainer = Color(0xFF181818),
    surfaceContainerHigh = Color(0xFF24211E),
    surfaceContainerHighest = Color(0xFF302A25),
    outline = Color(0xFF9E8E81),
    outlineVariant = Color(0xFF51453B),
    inverseSurface = Color(0xFFF5EDE6),
    inverseOnSurface = Color(0xFF302A25),
    inversePrimary = Color(0xFFA84300),
    surfaceTint = Color(0xFFFF9A3C),
)

private val Hell = lightColorScheme(
    primary = Color(0xFFA84300),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1C4),
    onPrimaryContainer = Color(0xFF351600),
    secondary = Color(0xFF91501B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDFC1),
    onSecondaryContainer = Color(0xFF301800),
    tertiary = Color(0xFF87520B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDEAE),
    onTertiaryContainer = Color(0xFF2D1900),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFF9F4),
    onBackground = Color(0xFF241A12),
    surface = Color(0xFFFFF9F4),
    onSurface = Color(0xFF241A12),
    surfaceVariant = Color(0xFFF1E2D6),
    onSurfaceVariant = Color(0xFF6C5849),
    surfaceDim = Color(0xFFE5D8CD),
    surfaceBright = Color(0xFFFFF9F4),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF3E9),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFFFEBDC),
    surfaceContainerHighest = Color(0xFFF5E3D4),
    outline = Color(0xFF887363),
    outlineVariant = Color(0xFFDBC7B6),
    inverseSurface = Color(0xFF302A25),
    inverseOnSurface = Color(0xFFF5EDE6),
    inversePrimary = Color(0xFFFF9A3C),
    surfaceTint = Color(0xFFA84300),
)

private val Schrift = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

val TextStyle.fett get() = copy(fontWeight = FontWeight.SemiBold)

@Composable
fun UpdateStationTheme(dunkel: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dunkel) Dunkel else Hell,
        typography = Schrift,
        content = content,
    )
}
