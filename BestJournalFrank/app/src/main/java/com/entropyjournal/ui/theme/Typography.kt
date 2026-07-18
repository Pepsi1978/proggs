package com.entropyjournal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.entropyjournal.R

const val DEFAULT_HEADING_FONT_NAME = "Playfair Display"
const val DEFAULT_BODY_FONT_NAME = "Source Sans 3"

val PlayfairDisplay = FontFamily(
    Font(R.font.playfair_display_variable, weight = FontWeight.Medium),
    Font(R.font.playfair_display_variable, weight = FontWeight.Bold),
    Font(
        R.font.playfair_display_italic_variable,
        weight = FontWeight.Medium,
        style = FontStyle.Italic,
    ),
)

val SourceSans3 = FontFamily(
    Font(R.font.source_sans_3_variable, weight = FontWeight.Normal),
    Font(R.font.source_sans_3_variable, weight = FontWeight.Medium),
    Font(R.font.source_sans_3_variable, weight = FontWeight.SemiBold),
)

val Caveat = FontFamily(
    Font(R.font.caveat_variable, weight = FontWeight.Medium),
    Font(R.font.caveat_variable, weight = FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_variable, weight = FontWeight.Normal),
    Font(R.font.jetbrains_mono_variable, weight = FontWeight.Medium),
)

private fun localFontFamily(resourceId: Int): FontFamily =
    FontFamily(
        Font(resourceId, weight = FontWeight.Normal),
        Font(resourceId, weight = FontWeight.Medium),
        Font(resourceId, weight = FontWeight.Bold),
    )

private val Lora = localFontFamily(R.font.lora_variable)
private val Manrope = localFontFamily(R.font.manrope_variable)
private val Sora = localFontFamily(R.font.sora_variable)
private val SpaceGrotesk = localFontFamily(R.font.space_grotesk_variable)
private val IbmPlexSans = localFontFamily(R.font.ibm_plex_sans_variable)
private val Nunito = localFontFamily(R.font.nunito_variable)
private val NunitoSans = localFontFamily(R.font.nunito_sans_variable)
private val GreatVibes = localFontFamily(R.font.great_vibes_regular)

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
