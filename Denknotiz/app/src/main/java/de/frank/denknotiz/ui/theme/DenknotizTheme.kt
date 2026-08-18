package de.frank.denknotiz.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.denknotiz.data.AppTheme

private val GoldDark = darkColorScheme(
    primary = Color(0xFFE3C475), onPrimary = Color(0xFF211A08), primaryContainer = Color(0xFF413719),
    onPrimaryContainer = Color(0xFFFFE7A4), secondary = Color(0xFFA7B9CE), background = Color(0xFF090B10),
    onBackground = Color(0xFFE8E6DF), surface = Color(0xFF11141B), onSurface = Color(0xFFE8E6DF),
    surfaceVariant = Color(0xFF1B202A), onSurfaceVariant = Color(0xFFBEC3CD), outline = Color(0xFF746A4E),
    error = Color(0xFFFFB4AB),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9FCBFF), onPrimary = Color(0xFF003258), primaryContainer = Color(0xFF174B72),
    secondary = Color(0xFFBBC7DB), background = Color(0xFF0A0D12), surface = Color(0xFF12161D),
    surfaceVariant = Color(0xFF1B222C), outline = Color(0xFF667383), error = Color(0xFFFFB4AB),
)
private val Light = lightColorScheme(
    primary = Color(0xFF275F89), onPrimary = Color.White, primaryContainer = Color(0xFFCDE5FF),
    secondary = Color(0xFF50606F), background = Color(0xFFF7F8FA), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EEF3), outline = Color(0xFF72777E), error = Color(0xFFBA1A1A),
)
private val GoldLight = lightColorScheme(
    primary = Color(0xFF755B00), onPrimary = Color.White, primaryContainer = Color(0xFFFFDF83),
    onPrimaryContainer = Color(0xFF251A00), secondary = Color(0xFF695D3E), background = Color(0xFFFFF9EE),
    surface = Color(0xFFFFFBF4), surfaceVariant = Color(0xFFF2EAD8), outline = Color(0xFF82755A), error = Color(0xFFBA1A1A),
)

private val DenkTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 0.6.sp),
)

@Composable
fun DenknotizTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val colors = when (theme) {
        AppTheme.GOLD_DARK -> GoldDark
        AppTheme.DARK -> Dark
        AppTheme.LIGHT -> Light
        AppTheme.GOLD_LIGHT -> GoldLight
    }
    MaterialTheme(
        colorScheme = colors,
        typography = DenkTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(10.dp),
            small = RoundedCornerShape(16.dp),
            medium = RoundedCornerShape(22.dp),
            large = RoundedCornerShape(28.dp),
            extraLarge = RoundedCornerShape(32.dp),
        ),
        content = content,
    )
}
