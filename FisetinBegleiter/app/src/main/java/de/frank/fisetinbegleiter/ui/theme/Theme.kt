package de.frank.fisetinbegleiter.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.fisetinbegleiter.R
import java.util.Locale

enum class AppThemeVariant(val persistenceValue: String) {
    AURORA("aurora"),
    VITAL("vital"),
    EMBER("ember");

    companion object {
        val DEFAULT = EMBER

        fun fromPersistenceValue(value: String?): AppThemeVariant =
            entries.firstOrNull { it.persistenceValue == value?.trim()?.lowercase(Locale.ROOT) } ?: DEFAULT
    }
}

enum class AppThemeMode(val persistenceValue: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        val DEFAULT = LIGHT

        fun fromPersistenceValue(value: String?): AppThemeMode =
            entries.firstOrNull { it.persistenceValue == value?.trim()?.lowercase(Locale.ROOT) } ?: DEFAULT
    }
}

data class AppThemeShadow(
    val offsetX: Dp,
    val offsetY: Dp,
    val blurRadius: Dp,
    val color: Color,
)

data class AppColors(
    val bg0: Color,
    val bg1: Color,
    val card: Color,
    val cardBorder: Color,
    val text: Color,
    val sub: Color,
    val line: Color,
    val chip: Color,
    val accent: Color,
    val accent2: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val glow: Color,
    val ok: Color,
    val okBg: Color,
    val warn: Color,
    val warnBg: Color,
    val bad: Color,
    val badBg: Color,
    val navBg: Color,
    val shadow: AppThemeShadow,
)

private val LightShadow = AppThemeShadow(
    offsetX = 0.dp,
    offsetY = 14.dp,
    blurRadius = 40.dp,
    color = Color(0x1A38308C),
)

private val DarkShadow = AppThemeShadow(
    offsetX = 0.dp,
    offsetY = 18.dp,
    blurRadius = 48.dp,
    color = Color(0x73000000),
)

private val AuroraLight = AppColors(
    bg0 = Color(0xFFF7F6FE),
    bg1 = Color(0xFFECEFFB),
    card = Color(0xB8FFFFFF),
    cardBorder = Color(0xE6FFFFFF),
    text = Color(0xFF191634),
    sub = Color(0xFF615E80),
    line = Color(0x17191634),
    chip = Color(0x0F191634),
    accent = Color(0xFF6A5CFF),
    accent2 = Color(0xFFB04DF0),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0x216A5CFF),
    glow = Color(0x526A5CFF),
    ok = Color(0xFF0D7A50),
    okBg = Color(0x1F109660),
    warn = Color(0xFF8F6400),
    warnBg = Color(0x29F2B200),
    bad = Color(0xFFC22F3E),
    badBg = Color(0x1CE23E50),
    navBg = Color(0xC7FFFFFF),
    shadow = LightShadow,
)

private val AuroraDark = AppColors(
    bg0 = Color(0xFF0F0D20),
    bg1 = Color(0xFF191434),
    card = Color(0x99211C40),
    cardBorder = Color(0x17FFFFFF),
    text = Color(0xFFEFEDFC),
    sub = Color(0xFFA29FC8),
    line = Color(0x17FFFFFF),
    chip = Color(0x12FFFFFF),
    accent = Color(0xFF8F7BFF),
    accent2 = Color(0xFFC66BFF),
    onAccent = Color(0xFF120E2C),
    accentSoft = Color(0x2E8F7BFF),
    glow = Color(0x808F7BFF),
    ok = Color(0xFF43E09C),
    okBg = Color(0x2143E09C),
    warn = Color(0xFFFFCB57),
    warnBg = Color(0x21FFCB57),
    bad = Color(0xFFFF6E7E),
    badBg = Color(0x24FF6E7E),
    navBg = Color(0xB8181332),
    shadow = DarkShadow,
)

private val VitalLight = AppColors(
    bg0 = Color(0xFFF2FAF6),
    bg1 = Color(0xFFE7F3EC),
    card = Color(0xB8FFFFFF),
    cardBorder = Color(0xE6FFFFFF),
    text = Color(0xFF132019),
    sub = Color(0xFF5A6D62),
    line = Color(0x17191634),
    chip = Color(0x0F191634),
    accent = Color(0xFF0C8F74),
    accent2 = Color(0xFF39B788),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0x210C8F74),
    glow = Color(0x4D0C8F74),
    ok = Color(0xFF0D7A50),
    okBg = Color(0x1F109660),
    warn = Color(0xFF8F6400),
    warnBg = Color(0x29F2B200),
    bad = Color(0xFFC22F3E),
    badBg = Color(0x1CE23E50),
    navBg = Color(0xC7FFFFFF),
    shadow = LightShadow,
)

private val VitalDark = AppColors(
    bg0 = Color(0xFF08130E),
    bg1 = Color(0xFF0E211A),
    card = Color(0x99102A21),
    cardBorder = Color(0x17FFFFFF),
    text = Color(0xFFE8F6EF),
    sub = Color(0xFF8FAB9D),
    line = Color(0x17FFFFFF),
    chip = Color(0x12FFFFFF),
    accent = Color(0xFF35D9AE),
    accent2 = Color(0xFF7CE3C3),
    onAccent = Color(0xFF042019),
    accentSoft = Color(0x2935D9AE),
    glow = Color(0x6B35D9AE),
    ok = Color(0xFF43E09C),
    okBg = Color(0x2143E09C),
    warn = Color(0xFFFFCB57),
    warnBg = Color(0x21FFCB57),
    bad = Color(0xFFFF6E7E),
    badBg = Color(0x24FF6E7E),
    navBg = Color(0xB80B1D17),
    shadow = DarkShadow,
)

private val EmberLight = AppColors(
    bg0 = Color(0xFFFBF6EF),
    bg1 = Color(0xFFF6ECE0),
    card = Color(0xB8FFFFFF),
    cardBorder = Color(0xE6FFFFFF),
    text = Color(0xFF261A10),
    sub = Color(0xFF7C6A58),
    line = Color(0x17191634),
    chip = Color(0x0F191634),
    accent = Color(0xFFDF5A2C),
    accent2 = Color(0xFFF0A03A),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0x1FDF5A2C),
    glow = Color(0x4DDF5A2C),
    ok = Color(0xFF0D7A50),
    okBg = Color(0x1F109660),
    warn = Color(0xFF8F6400),
    warnBg = Color(0x29F2B200),
    bad = Color(0xFFC22F3E),
    badBg = Color(0x1CE23E50),
    navBg = Color(0xC7FFFFFF),
    shadow = LightShadow,
)

private val EmberDark = AppColors(
    bg0 = Color(0xFF160F08),
    bg1 = Color(0xFF231710),
    card = Color(0x94301F14),
    cardBorder = Color(0x17FFFFFF),
    text = Color(0xFFF9EFE7),
    sub = Color(0xFFB5A190),
    line = Color(0x17FFFFFF),
    chip = Color(0x12FFFFFF),
    accent = Color(0xFFFF8A56),
    accent2 = Color(0xFFFFC058),
    onAccent = Color(0xFF2B1204),
    accentSoft = Color(0x29FF8A56),
    glow = Color(0x6BFF8A56),
    ok = Color(0xFF43E09C),
    okBg = Color(0x2143E09C),
    warn = Color(0xFFFFCB57),
    warnBg = Color(0x21FFCB57),
    bad = Color(0xFFFF6E7E),
    badBg = Color(0x24FF6E7E),
    navBg = Color(0xB824170F),
    shadow = DarkShadow,
)

val LocalAppColors = staticCompositionLocalOf { EmberLight }

val AllowedGreen: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.ok

val AllowedGreenContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.okBg

val WarningYellow: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.warn

val WarningYellowContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.warnBg

val BlockedRed: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.bad

val BlockedRedContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.badBg

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val OutfitGoogleFont = GoogleFont("Outfit")

val OutfitFontFamily = FontFamily(
    Font(OutfitGoogleFont, GoogleFontsProvider, FontWeight.Light),
    Font(OutfitGoogleFont, GoogleFontsProvider, FontWeight.Normal),
    Font(OutfitGoogleFont, GoogleFontsProvider, FontWeight.Medium),
    Font(OutfitGoogleFont, GoogleFontsProvider, FontWeight.SemiBold),
    Font(OutfitGoogleFont, GoogleFontsProvider, FontWeight.Bold),
    Font(OutfitGoogleFont, GoogleFontsProvider, FontWeight.ExtraBold),
)

private val FisetinTypography = Typography(
    displayLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 39.2.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 39.2.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.4.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 36.4.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 26.6.sp, letterSpacing = (-0.2).sp),
    headlineSmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 25.2.sp),
    titleLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 26.6.sp),
    titleMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.8.sp),
    titleSmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 20.3.sp),
    bodyLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 20.25.sp),
    bodySmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 19.375.sp),
    labelLarge = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, lineHeight = 20.3.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 17.4.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = OutfitFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp),
)

private val FisetinShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun appColors(variant: AppThemeVariant, mode: AppThemeMode): AppColors = when (variant to mode) {
    AppThemeVariant.AURORA to AppThemeMode.LIGHT -> AuroraLight
    AppThemeVariant.AURORA to AppThemeMode.DARK -> AuroraDark
    AppThemeVariant.VITAL to AppThemeMode.LIGHT -> VitalLight
    AppThemeVariant.VITAL to AppThemeMode.DARK -> VitalDark
    AppThemeVariant.EMBER to AppThemeMode.LIGHT -> EmberLight
    AppThemeVariant.EMBER to AppThemeMode.DARK -> EmberDark
    else -> EmberLight
}

private fun materialColorScheme(colors: AppColors, mode: AppThemeMode) =
    if (mode == AppThemeMode.LIGHT) {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.accent,
            inversePrimary = colors.accent2,
            secondary = colors.accent2,
            onSecondary = colors.onAccent,
            secondaryContainer = colors.chip,
            onSecondaryContainer = colors.text,
            tertiary = colors.ok,
            onTertiary = Color.White,
            tertiaryContainer = colors.okBg,
            onTertiaryContainer = colors.ok,
            background = colors.bg0,
            onBackground = colors.text,
            surface = colors.card,
            onSurface = colors.text,
            surfaceVariant = colors.chip,
            onSurfaceVariant = colors.sub,
            surfaceTint = colors.accent,
            inverseSurface = colors.text,
            inverseOnSurface = colors.bg0,
            error = colors.bad,
            onError = Color.White,
            errorContainer = colors.badBg,
            onErrorContainer = colors.bad,
            outline = colors.line,
            outlineVariant = colors.cardBorder,
            scrim = Color(0x73080614),
            surfaceBright = colors.card,
            surfaceDim = colors.bg1,
            surfaceContainer = colors.card,
            surfaceContainerHigh = colors.chip,
            surfaceContainerHighest = colors.cardBorder,
            surfaceContainerLow = colors.bg1,
            surfaceContainerLowest = colors.bg0,
        )
    } else {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.accent,
            inversePrimary = colors.accent2,
            secondary = colors.accent2,
            onSecondary = colors.onAccent,
            secondaryContainer = colors.chip,
            onSecondaryContainer = colors.text,
            tertiary = colors.ok,
            onTertiary = colors.bg0,
            tertiaryContainer = colors.okBg,
            onTertiaryContainer = colors.ok,
            background = colors.bg0,
            onBackground = colors.text,
            surface = colors.card,
            onSurface = colors.text,
            surfaceVariant = colors.chip,
            onSurfaceVariant = colors.sub,
            surfaceTint = colors.accent,
            inverseSurface = colors.text,
            inverseOnSurface = colors.bg0,
            error = colors.bad,
            onError = colors.bg0,
            errorContainer = colors.badBg,
            onErrorContainer = colors.bad,
            outline = colors.line,
            outlineVariant = colors.cardBorder,
            scrim = Color(0x73080614),
            surfaceBright = colors.card,
            surfaceDim = colors.bg1,
            surfaceContainer = colors.card,
            surfaceContainerHigh = colors.chip,
            surfaceContainerHighest = colors.cardBorder,
            surfaceContainerLow = colors.bg1,
            surfaceContainerLowest = colors.bg0,
        )
    }

@Composable
fun FisetinTheme(
    variant: AppThemeVariant,
    mode: AppThemeMode,
    content: @Composable () -> Unit,
) {
    val colors = appColors(variant, mode)
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = materialColorScheme(colors, mode),
            typography = FisetinTypography,
            shapes = FisetinShapes,
        ) {
            ProvideTextStyle(value = FisetinTypography.bodyLarge, content = content)
        }
    }
}
