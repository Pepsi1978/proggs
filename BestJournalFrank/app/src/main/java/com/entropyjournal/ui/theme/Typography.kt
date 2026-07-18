package com.entropyjournal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.entropyjournal.R

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val playfairDisplayFont = GoogleFont("Playfair Display")
private val sourceSans3Font = GoogleFont("Source Sans 3")
private val jetBrainsMonoFont = GoogleFont("JetBrains Mono")
private val caveatFont = GoogleFont("Caveat")

const val DEFAULT_HEADING_FONT_NAME = "Playfair Display"
const val DEFAULT_BODY_FONT_NAME = "Source Sans 3"

val PlayfairDisplay = FontFamily(
    Font(googleFont = playfairDisplayFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = playfairDisplayFont, fontProvider = fontProvider, weight = FontWeight.Bold),
    Font(
        googleFont = playfairDisplayFont,
        fontProvider = fontProvider,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
    ),
)

val SourceSans3 = FontFamily(
    Font(googleFont = sourceSans3Font, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = sourceSans3Font, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = sourceSans3Font, fontProvider = fontProvider, weight = FontWeight.SemiBold),
)

val Caveat = FontFamily(
    Font(googleFont = caveatFont, fontProvider = fontProvider, weight = FontWeight.Medium),
    Font(googleFont = caveatFont, fontProvider = fontProvider, weight = FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(googleFont = jetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.Normal),
    Font(googleFont = jetBrainsMonoFont, fontProvider = fontProvider, weight = FontWeight.Medium),
)

private fun googleFontFamily(
    name: String,
    weights: List<FontWeight> = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.Bold),
): FontFamily =
    FontFamily(
        weights.map { weight ->
            Font(googleFont = GoogleFont(name), fontProvider = fontProvider, weight = weight)
        }
    )

private val Lora = googleFontFamily("Lora")
private val Manrope = googleFontFamily("Manrope")
private val Sora = googleFontFamily("Sora")
private val SpaceGrotesk = googleFontFamily("Space Grotesk")
private val IbmPlexSans = googleFontFamily("IBM Plex Sans")
private val Nunito = googleFontFamily("Nunito")
private val NunitoSans = googleFontFamily("Nunito Sans")
private val GreatVibes = googleFontFamily("Great Vibes")

val SourceSansPro = SourceSans3
val Exo2 = PlayfairDisplay

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PlayfairDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SourceSans3,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

fun headingFontFamily(name: String): FontFamily =
    when (name) {
        "Great Vibes" -> GreatVibes
        "Caveat" -> Caveat
        "Lora" -> Lora
        "Sora" -> Sora
        "Space Grotesk" -> SpaceGrotesk
        "Nunito" -> Nunito
        else -> PlayfairDisplay
    }

fun bodyFontFamily(name: String): FontFamily =
    when (name) {
        "Manrope" -> Manrope
        "IBM Plex Sans" -> IbmPlexSans
        "Nunito Sans" -> NunitoSans
        "Lora" -> Lora
        "Caveat" -> Caveat
        else -> SourceSans3
    }

fun appTypography(headingFontName: String, bodyFontName: String): Typography {
    val heading = headingFontFamily(headingFontName)
    val body = bodyFontFamily(bodyFontName)

    return AppTypography.copy(
        displayLarge = AppTypography.displayLarge.copy(fontFamily = heading),
        displayMedium = AppTypography.displayMedium.copy(fontFamily = heading),
        displaySmall = AppTypography.displaySmall.copy(fontFamily = heading),
        headlineLarge = AppTypography.headlineLarge.copy(fontFamily = heading),
        headlineMedium = AppTypography.headlineMedium.copy(fontFamily = heading),
        headlineSmall = AppTypography.headlineSmall.copy(fontFamily = heading),
        titleLarge = AppTypography.titleLarge.copy(fontFamily = heading),
        titleMedium = AppTypography.titleMedium.copy(fontFamily = heading),
        titleSmall = AppTypography.titleSmall.copy(fontFamily = heading),
        bodyLarge = AppTypography.bodyLarge.copy(fontFamily = body),
        bodyMedium = AppTypography.bodyMedium.copy(fontFamily = body),
        bodySmall = AppTypography.bodySmall.copy(fontFamily = body),
        labelLarge = AppTypography.labelLarge.copy(fontFamily = body),
    )
}
