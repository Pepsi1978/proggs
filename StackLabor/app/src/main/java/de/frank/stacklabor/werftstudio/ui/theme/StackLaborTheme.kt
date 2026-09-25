package de.frank.stacklabor.werftstudio.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.stacklabor.werftstudio.R

private val LightBackground = Color(0xFFF3F5FA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightElevated = Color(0xFFEEF1F8)
private val LightBorder = Color(0xFFE1E5EE)
private val LightText = Color(0xFF111827)
private val LightMuted = Color(0xFF64748B)
private val LightAccent = Color(0xFF4F46E5)

private val DarkBackground = Color(0xFF0B1020)
private val DarkSurface = Color(0xFF151B2E)
private val DarkElevated = Color(0xFF1D2540)
private val DarkBorder = Color(0xFF2A3350)
private val DarkText = Color(0xFFE8ECF5)
private val DarkMuted = Color(0xFF98A3BA)
private val DarkAccent = Color(0xFF8B93FF)

/** Text- und Symbolfarbe auf der Akzent-Fläche der Hauptaktionen (Knöpfe, Kopfbereich). */
val OnActionColor = Color.White

private val Inter = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold),
)

@Immutable
data class StackLaborColors(
    val background: Color,
    val surface: Color,
    val elevated: Color,
    val border: Color,
    val textStrong: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val green: Color,
    val yellow: Color,
    val yellowText: Color,
    val red: Color,
    val gray: Color,
    val water: Color,
    val fat: Color,
    val fatBorder: Color,
    val disabled: Color,
    val glass: Color,
)

@Immutable
data class StackLaborDimens(
    val screenPadding: androidx.compose.ui.unit.Dp = 16.dp,
    val cardRadius: androidx.compose.ui.unit.Dp = 16.dp,
    val sheetRadius: androidx.compose.ui.unit.Dp = 28.dp,
    val minTouch: androidx.compose.ui.unit.Dp = 44.dp,
    val headerHeight: androidx.compose.ui.unit.Dp = 56.dp,
    val medicineHeight: androidx.compose.ui.unit.Dp = 56.dp,
    val stackHeight: androidx.compose.ui.unit.Dp = 76.dp,
)

private val LocalStackLaborColors = compositionLocalOf { lightPalette() }
private val LocalStackLaborDimens = compositionLocalOf { StackLaborDimens() }
private val LocalStackLaborMotionEnabled = compositionLocalOf { true }
private val LocalStackLaborDark = compositionLocalOf { false }

object StackLaborTheme {
    val colors: StackLaborColors
        @Composable @ReadOnlyComposable get() = LocalStackLaborColors.current
    val dimens: StackLaborDimens
        @Composable @ReadOnlyComposable get() = LocalStackLaborDimens.current
    val motionEnabled: Boolean
        @Composable @ReadOnlyComposable get() = LocalStackLaborMotionEnabled.current
    val dark: Boolean
        @Composable @ReadOnlyComposable get() = LocalStackLaborDark.current
}

val StackLaborTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 21.sp, letterSpacing = (-0.1).sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.2.sp),
)

@Composable
fun StackLaborTheme(
    darkTheme: Boolean,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    val target = remember(darkTheme) { if (darkTheme) darkPalette() else lightPalette() }
    val duration = if (reducedMotion) 0 else 420
    val palette = StackLaborColors(
        background = target.background.animated(duration),
        surface = target.surface.animated(duration),
        elevated = target.elevated.animated(duration),
        border = target.border.animated(duration),
        textStrong = target.textStrong.animated(duration),
        textMuted = target.textMuted.animated(duration),
        accent = target.accent.animated(duration),
        onAccent = target.onAccent.animated(duration),
        green = target.green.animated(duration),
        yellow = target.yellow.animated(duration),
        yellowText = target.yellowText.animated(duration),
        red = target.red.animated(duration),
        gray = target.gray.animated(duration),
        water = target.water.animated(duration),
        fat = target.fat.animated(duration),
        fatBorder = target.fatBorder.animated(duration),
        disabled = target.disabled.animated(duration),
        glass = target.glass.animated(duration),
    )
    val scheme = remember(darkTheme, palette) {
        if (darkTheme) {
            darkColorScheme(
                primary = palette.accent,
                onPrimary = palette.onAccent,
                background = palette.background,
                onBackground = palette.textStrong,
                surface = palette.surface,
                onSurface = palette.textStrong,
                outline = palette.border,
            )
        } else {
            lightColorScheme(
                primary = palette.accent,
                onPrimary = palette.onAccent,
                background = palette.background,
                onBackground = palette.textStrong,
                surface = palette.surface,
                onSurface = palette.textStrong,
                outline = palette.border,
            )
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalStackLaborColors provides palette,
        LocalStackLaborDimens provides remember { StackLaborDimens() },
        LocalStackLaborMotionEnabled provides !reducedMotion,
        LocalStackLaborDark provides darkTheme,
    ) {
        MaterialTheme(colorScheme = scheme, typography = StackLaborTypography, content = content)
    }
}

/** Früher ein eigenes Gold-Dunkelthema; heute nutzen alle Bildschirme dieselbe ruhige Palette. */
@Composable
fun GoldDarkContent(content: @Composable () -> Unit) = content()

@Composable
private fun Color.animated(duration: Int): Color = animateColorAsState(
    targetValue = this,
    animationSpec = tween(duration),
    label = "themeColor",
).value

@Composable
fun rememberSystemAnimationsEnabled(): State<Boolean> {
    val context = LocalContext.current
    val enabled = remember {
        mutableStateOf(Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f)
    }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled.value = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return enabled
}

private fun lightPalette() = StackLaborColors(
    background = LightBackground,
    surface = LightSurface,
    elevated = LightElevated,
    border = LightBorder,
    textStrong = LightText,
    textMuted = LightMuted,
    accent = LightAccent,
    onAccent = Color.White,
    green = Color(0xFF059669),
    yellow = Color(0xFFF59E0B),
    yellowText = Color(0xFFB45309),
    red = Color(0xFFE11D48),
    gray = Color(0xFF94A3B8),
    water = Color(0xFF10B981),
    fat = Color.White,
    fatBorder = LightMuted,
    disabled = Color(0xFFCBD5E1),
    glass = Color(0xF2FFFFFF),
)

private fun darkPalette() = StackLaborColors(
    background = DarkBackground,
    surface = DarkSurface,
    elevated = DarkElevated,
    border = DarkBorder,
    textStrong = DarkText,
    textMuted = DarkMuted,
    accent = DarkAccent,
    onAccent = DarkBackground,
    green = Color(0xFF34D399),
    yellow = Color(0xFFFBBF24),
    yellowText = Color(0xFFFBBF24),
    red = Color(0xFFFB7185),
    gray = Color(0xFF64748B),
    water = Color(0xFF34D399),
    fat = Color.White,
    fatBorder = Color.Transparent,
    disabled = Color(0xFF334155),
    glass = Color(0xF2151B2E),
)

