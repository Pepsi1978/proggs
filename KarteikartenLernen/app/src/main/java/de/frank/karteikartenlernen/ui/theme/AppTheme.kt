package de.frank.karteikartenlernen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.karteikartenlernen.R

@Immutable
data class AppPalette(
    val name: String,
    val dark: Boolean,
    val surface: Color,
    val surfaceSolid: Color,
    val raised: Color,
    val border: Color,
    val borderHigh: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val green: Color,
    val red: Color,
    val neutral: Color,
    val field: Color,
    val tab: Color,
    val chip: Color,
    val onAccent: Color,
    val accent: Color,
    val accent2: Color,
    val background0: Color,
    val background1: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
)

private val darkBase = listOf(
    AppPalette("Mitternacht", true, Color(0x9E1E202E), Color(0xFF191B28), Color(0xCC2A2D40), Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0xFFF4F5FB), Color(0xFFA3A7BD), Color(0xFF6A6E85), Color(0xFF43E79D), Color(0xFFFF5D74), Color(0xFF5B5F76), Color(0x0DFFFFFF), Color(0xB80E0F18), Color(0x0FFFFFFF), Color(0xFF0B0C12), Color(0xFFD9A95B), Color(0xFF7FA8C9), Color(0xFF0C0E14), Color(0xFF171B28), Color(0x33D9A95B), Color(0x297FA8C9), Color(0x1AA078C8)),
    AppPalette("Bordeaux", true, Color(0x9E1E202E), Color(0xFF191B28), Color(0xCC2A2D40), Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0xFFF4F5FB), Color(0xFFA3A7BD), Color(0xFF6A6E85), Color(0xFF43E79D), Color(0xFFFF5D74), Color(0xFF5B5F76), Color(0x0DFFFFFF), Color(0xB80E0F18), Color(0x0FFFFFFF), Color(0xFF0B0C12), Color(0xFFD98098), Color(0xFFC9A06B), Color(0xFF130C11), Color(0xFF251622), Color(0x33D98098), Color(0x24C9A06B), Color(0x1A786EC8)),
    AppPalette("Tanne", true, Color(0x9E1E202E), Color(0xFF191B28), Color(0xCC2A2D40), Color(0x17FFFFFF), Color(0x29FFFFFF), Color(0xFFF4F5FB), Color(0xFFA3A7BD), Color(0xFF6A6E85), Color(0xFF43E79D), Color(0xFFFF5D74), Color(0xFF5B5F76), Color(0x0DFFFFFF), Color(0xB80E0F18), Color(0x0FFFFFFF), Color(0xFF0B0C12), Color(0xFF8FBF9F), Color(0xFFC9B06B), Color(0xFF0B100D), Color(0xFF15221B), Color(0x2E8FBF9F), Color(0x21C9B06B), Color(0x176E96C8)),
)

private val lightBase = listOf(
    AppPalette("Elfenbein", false, Color(0xB8FFFFFF), Color.White, Color(0xE6FFFFFF), Color(0x14181A30), Color(0x24181A30), Color(0xFF171930), Color(0xFF5B5F74), Color(0xFF9195A8), Color(0xFF0FAE72), Color(0xFFE23A54), Color(0xFFA7ABBD), Color(0x0A14162D), Color(0xD1FFFFFF), Color(0x0D181A30), Color.White, Color(0xFFA87C2F), Color(0xFF5B7FA6), Color(0xFFEFEADF), Color(0xFFFAF7EF), Color(0x29B8862F), Color(0x1F5B7FA6), Color(0x14A078C8)),
    AppPalette("Salbei", false, Color(0xB8FFFFFF), Color.White, Color(0xE6FFFFFF), Color(0x14181A30), Color(0x24181A30), Color(0xFF171930), Color(0xFF5B5F74), Color(0xFF9195A8), Color(0xFF0FAE72), Color(0xFFE23A54), Color(0xFFA7ABBD), Color(0x0A14162D), Color(0xD1FFFFFF), Color(0x0D181A30), Color.White, Color(0xFF537F64), Color(0xFFC07A4A), Color(0xFFE8EFE9), Color(0xFFF5FAF5), Color(0x295D8A6F), Color(0x1FC98A5B), Color(0x146E82BE)),
    AppPalette("Porzellan", false, Color(0xB8FFFFFF), Color.White, Color(0xE6FFFFFF), Color(0x14181A30), Color(0x24181A30), Color(0xFF171930), Color(0xFF5B5F74), Color(0xFF9195A8), Color(0xFF0FAE72), Color(0xFFE23A54), Color(0xFFA7ABBD), Color(0x0A14162D), Color(0xD1FFFFFF), Color(0x0D181A30), Color.White, Color(0xFF4A5FB5), Color(0xFF8A6FC0), Color(0xFFE9ECF2), Color(0xFFF7F8FB), Color(0x244A5FB5), Color(0x1C8A6FC0), Color(0x173C96AA)),
)

val LocalAppPalette = staticCompositionLocalOf { darkBase.first() }

val SchibstedGrotesk = FontFamily(
    Font(R.font.schibsted_grotesk, FontWeight.Normal),
    Font(R.font.schibsted_grotesk, FontWeight.Medium),
    Font(R.font.schibsted_grotesk, FontWeight.SemiBold),
    Font(R.font.schibsted_grotesk, FontWeight.Bold),
    Font(R.font.schibsted_grotesk, FontWeight.ExtraBold),
)

val Newsreader = FontFamily(
    Font(R.font.newsreader, FontWeight.Normal),
    Font(R.font.newsreader, FontWeight.Medium),
    Font(R.font.newsreader, FontWeight.SemiBold),
    Font(R.font.newsreader_italic, FontWeight.Normal, FontStyle.Italic),
)

fun appPalette(dark: Boolean, profile: Int): AppPalette =
    (if (dark) darkBase else lightBase)[profile.coerceIn(0, 2)]

@Composable
fun KarteikartenTheme(
    dark: Boolean = isSystemInDarkTheme(),
    profile: Int = 0,
    content: @Composable () -> Unit,
) {
    val palette = appPalette(dark, profile)
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            secondary = palette.accent2,
            background = palette.background0,
            surface = palette.surfaceSolid,
            onPrimary = palette.onAccent,
            onBackground = palette.text,
            onSurface = palette.text,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            secondary = palette.accent2,
            background = palette.background0,
            surface = palette.surfaceSolid,
            onPrimary = palette.onAccent,
            onBackground = palette.text,
            onSurface = palette.text,
        )
    }
    val typography = MaterialTheme.typography.copy(
        bodyLarge = TextStyle(fontFamily = SchibstedGrotesk, fontSize = 16.sp),
        bodyMedium = TextStyle(fontFamily = SchibstedGrotesk, fontSize = 14.sp),
        labelLarge = TextStyle(fontFamily = SchibstedGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        titleLarge = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
        headlineMedium = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
