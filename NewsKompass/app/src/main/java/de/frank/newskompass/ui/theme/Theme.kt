package de.frank.newskompass.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Hell = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0FF),
    onPrimaryContainer = Color(0xFF1E1B6B),
    secondary = Color(0xFFDB2777),
    secondaryContainer = Color(0xFFFFD9E6),
    tertiary = Color(0xFF0891B2),
    background = Color(0xFFF4F2FB),
    onBackground = Color(0xFF15142B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15142B),
    surfaceVariant = Color(0xFFEDEAF7),
    onSurfaceVariant = Color(0xFF55536E),
    surfaceContainer = Color(0xFFF9F8FE),
    surfaceContainerHigh = Color(0xFFF0EEF9),
    outline = Color(0xFFCAC6DC),
    outlineVariant = Color(0xFFE3E0EF),
)

private val Dunkel = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B6B),
    primaryContainer = Color(0xFF2E2A7A),
    onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFFF9A8D4),
    secondaryContainer = Color(0xFF5B1438),
    tertiary = Color(0xFF67E8F9),
    background = Color(0xFF0D0F1C),
    onBackground = Color(0xFFE8E6F5),
    surface = Color(0xFF161932),
    onSurface = Color(0xFFE8E6F5),
    surfaceVariant = Color(0xFF22254A),
    onSurfaceVariant = Color(0xFFB3B0CC),
    surfaceContainer = Color(0xFF1A1D3A),
    surfaceContainerHigh = Color(0xFF232748),
    outline = Color(0xFF4A4D72),
    outlineVariant = Color(0xFF2C2F55),
)

/** Jeder Themenblock bekommt seinen eigenen Farbverlauf — so erkennt man den Block sofort. */
val BlockVerlaeufe = listOf(
    listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
    listOf(Color(0xFF0EA5E9), Color(0xFF14B8A6)),
    listOf(Color(0xFFF97316), Color(0xFFE11D48)),
    listOf(Color(0xFF10B981), Color(0xFF65A30D)),
    listOf(Color(0xFFEAB308), Color(0xFFF97316)),
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
)

fun blockVerlauf(index: Int): Brush = Brush.linearGradient(BlockVerlaeufe[index.mod(BlockVerlaeufe.size)])
fun blockFarbe(index: Int): Color = BlockVerlaeufe[index.mod(BlockVerlaeufe.size)][0]

val LocalIstDunkel = staticCompositionLocalOf { false }

private val Schrift = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.6.sp),
)

@Composable
fun NewsTheme(dunkel: Boolean, inhalt: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIstDunkel provides dunkel) {
        MaterialTheme(colorScheme = if (dunkel) Dunkel else Hell, typography = Schrift, content = inhalt)
    }
}
