package de.frank.fisetinbegleiter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AllowedGreen = Color(0xFF217A4A)
val AllowedGreenContainer = Color(0xFFD8F4E1)
val WarningYellow = Color(0xFF8A6300)
val WarningYellowContainer = Color(0xFFFFEFB8)
val BlockedRed = Color(0xFFB3261E)
val BlockedRedContainer = Color(0xFFFFDAD6)

private val LightColors = lightColorScheme(
    primary = AllowedGreen,
    onPrimary = Color.White,
    primaryContainer = AllowedGreenContainer,
    onPrimaryContainer = Color(0xFF082F1B),
    secondary = Color(0xFF506354),
    background = Color(0xFFF7FAF6),
    surface = Color(0xFFF7FAF6),
    error = BlockedRed,
    errorContainer = BlockedRedContainer,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF79D49A),
    primaryContainer = Color(0xFF145C37),
    secondary = Color(0xFFB8CCBC),
    error = Color(0xFFFFB4AB),
)

@Composable
fun FisetinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = MaterialTheme.typography.copy(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)),
        ),
        content = content,
    )
}
