package de.frank.voicekey.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.voicekey.R

/**
 * Design-Tokens 1:1 aus dem freigegebenen Mockup (design/mockup-v1.html):
 * warmes Schwarz, Orange #F97316 nur fuer Voice-Elemente, Outfit fuer Headlines.
 */
object VK {
    val BgPage = Color(0xFF0C0A09)
    val Surface = Color(0xFF161311)
    val Surface1 = Color(0xFF1D1916)
    val Surface2 = Color(0xFF25201C)
    val Surface3 = Color(0xFF2E2823)
    val Outline = Color(0xFF4A4039)
    val OutlineSoft = Color(0xFF332D28)
    val Primary = Color(0xFFF97316)
    val PrimarySoft = Color(0xFFFB8C3C)
    val OnPrimary = Color(0xFF2B1200)
    val PrimaryContainer = Color(0xFF4A2407)
    val OnPrimaryContainer = Color(0xFFFFDCC2)
    val TextHi = Color(0xFFF3ECE6)
    val TextMid = Color(0xFFCBBFB5)
    val TextLo = Color(0xFF948A81)
    val Green = Color(0xFF4ADE80)
    val GreenDim = Color(0xFF14351F)

    // Sprach-Badges
    val DeBg = Color(0xFF312214)
    val DeFg = Color(0xFFFBBF77)
    val DeBorder = Color(0xFF5B3A17)
    val EnBg = Color(0xFF14253B)
    val EnFg = Color(0xFF93C5FD)
    val EnBorder = Color(0xFF1E3A5F)

    // Orb-Verlauf (ChatGPT-Kugel-Anmutung)
    val OrbLight = Color(0xFFFFD9B8)
    val OrbMid = Color(0xFFFB8C3C)
    val OrbDeep = Color(0xFFEA580C)
    val OrbDark = Color(0xFF9A3412)
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun outfit(weight: FontWeight) = Font(
    R.font.outfit_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val Outfit = FontFamily(
    outfit(FontWeight.Normal),
    outfit(FontWeight.Medium),
    outfit(FontWeight.SemiBold),
    outfit(FontWeight.Bold),
    outfit(FontWeight.ExtraBold),
)

private val DarkScheme = darkColorScheme(
    primary = VK.Primary,
    onPrimary = VK.OnPrimary,
    primaryContainer = VK.PrimaryContainer,
    onPrimaryContainer = VK.OnPrimaryContainer,
    secondary = VK.PrimarySoft,
    onSecondary = VK.OnPrimary,
    background = VK.BgPage,
    onBackground = VK.TextHi,
    surface = VK.BgPage,
    onSurface = VK.TextHi,
    surfaceVariant = VK.Surface2,
    onSurfaceVariant = VK.TextMid,
    surfaceContainer = VK.Surface1,
    surfaceContainerHigh = VK.Surface2,
    surfaceContainerHighest = VK.Surface3,
    outline = VK.Outline,
    outlineVariant = VK.OutlineSoft,
    error = Color(0xFFF87171),
)

private val VKTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = VK.TextHi),
    headlineSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = VK.TextHi),
    titleLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 23.sp, color = VK.TextHi),
    titleMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = VK.TextHi),
    titleSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = VK.TextHi),
    bodyMedium = TextStyle(fontSize = 13.sp, color = VK.TextMid, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.sp, color = VK.TextLo, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.8.sp),
)

@Composable
fun VoiceKeyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = VKTypography,
        content = content,
    )
}
