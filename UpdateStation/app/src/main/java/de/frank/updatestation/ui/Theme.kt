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
    val Violett = Color(0xFF5B4BFF)
    val Indigo = Color(0xFF3B2FD9)
    val Tuerkis = Color(0xFF00C2A8)
    val Minze = Color(0xFF2EE6C5)
    val Bernstein = Color(0xFFFFB547)
    val Koralle = Color(0xFFFF6B6B)

    val kopfVerlauf = Brush.linearGradient(listOf(Color(0xFF6A5BFF), Indigo, Color(0xFF0A9E8E)))
    val randVerlauf = Brush.linearGradient(listOf(Violett, Tuerkis))
}

private val Dunkel = darkColorScheme(
    primary = Color(0xFF8C80FF),
    onPrimary = Color(0xFF14104A),
    primaryContainer = Color(0xFF2B2470),
    onPrimaryContainer = Color(0xFFE3DFFF),
    secondary = Farben.Minze,
    onSecondary = Color(0xFF00382F),
    tertiary = Farben.Bernstein,
    error = Farben.Koralle,
    background = Color(0xFF0F1226),
    onBackground = Color(0xFFE6E7F5),
    surface = Color(0xFF0F1226),
    onSurface = Color(0xFFE6E7F5),
    surfaceVariant = Color(0xFF232849),
    onSurfaceVariant = Color(0xFFA9ADCB),
    surfaceContainer = Color(0xFF191D38),
    surfaceContainerHigh = Color(0xFF20254A),
    surfaceContainerLow = Color(0xFF151932),
    outline = Color(0xFF3A406B),
    outlineVariant = Color(0xFF2A2F55),
)

private val Hell = lightColorScheme(
    primary = Farben.Violett,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E3FF),
    onPrimaryContainer = Color(0xFF1B1464),
    secondary = Color(0xFF00A38D),
    onSecondary = Color.White,
    tertiary = Color(0xFFB77400),
    error = Color(0xFFD64545),
    background = Color(0xFFF5F6FC),
    onBackground = Color(0xFF15182E),
    surface = Color(0xFFF5F6FC),
    onSurface = Color(0xFF15182E),
    surfaceVariant = Color(0xFFE7E9F5),
    onSurfaceVariant = Color(0xFF5B607E),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F1FA),
    surfaceContainerLow = Color(0xFFFAFBFF),
    outline = Color(0xFFCDD0E3),
    outlineVariant = Color(0xFFE2E4F0),
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
fun UpdateStationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dunkel else Hell,
        typography = Schrift,
        content = content,
    )
}
