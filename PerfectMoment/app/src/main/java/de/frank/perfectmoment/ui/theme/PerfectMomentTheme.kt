package de.frank.perfectmoment.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.frank.perfectmoment.R

@Immutable
data class PmColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val gold: Color,
    val goldHi: Color,
    val goldDim: Color,
    val amber: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val warning: Color,
    val breath: Color,
    val success: Color = Color(0xFF6FA860),
    val dark: Boolean,
)

val DarkPmColors = PmColors(
    background = Color(0xFF181209),
    surface = Color(0xFF251C10),
    surface2 = Color(0xFF332717),
    gold = Color(0xFFD4A24C),
    goldHi = Color(0xFFF0C97A),
    goldDim = Color(0xFF9A7C40),
    amber = Color(0xFFE8873B),
    text1 = Color(0xFFF5EEE2),
    text2 = Color(0xFFB3A68F),
    text3 = Color(0xFF786A57),
    warning = Color(0xFFC4634A),
    breath = Color(0x21D4A24C),
    dark = true,
)

val LightPmColors = PmColors(
    background = Color(0xFFFBF6EC),
    surface = Color(0xFFF3EAD9),
    surface2 = Color(0xFFEDE1CA),
    gold = Color(0xFFA87A2A),
    goldHi = Color(0xFF7A5518),
    goldDim = Color(0xFFC7AE7E),
    amber = Color(0xFFC4661F),
    text1 = Color(0xFF241D12),
    text2 = Color(0xFF6B5D48),
    text3 = Color(0xFFA2947C),
    warning = Color(0xFFA33F28),
    breath = Color(0x12A87A2A),
    dark = false,
)

val LocalPmColors = compositionLocalOf { DarkPmColors }
val LocalReducedMotion = compositionLocalOf { false }

val Inter = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
)
val Newsreader = FontFamily(
    Font(R.font.newsreader, FontWeight.Light),
    Font(R.font.newsreader, FontWeight.Normal),
)
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
)

object PmTextStyles {
    val screenTitle = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.5.sp,
        letterSpacing = (-0.3).sp,
    )
    val section = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.8.sp,
    )
    val body = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.25.sp,
    )
    val question = TextStyle(
        fontFamily = Newsreader,
        fontWeight = FontWeight.Light,
        fontSize = 20.sp,
        lineHeight = 31.sp,
    )
    val activeQuestion = question.copy(fontSize = 32.sp, lineHeight = 49.6.sp)
    val mono = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.8.sp,
    )
}

@Composable
fun PerfectMomentTheme(
    appearance: String,
    content: @Composable () -> Unit,
) {
    val isDark = when (appearance) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    val colors = if (isDark) DarkPmColors else LightPmColors
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    val scheme = if (isDark) {
        darkColorScheme(
            primary = colors.gold,
            onPrimary = colors.background,
            background = colors.background,
            onBackground = colors.text1,
            surface = colors.surface,
            onSurface = colors.text1,
            error = colors.warning,
        )
    } else {
        lightColorScheme(
            primary = colors.gold,
            onPrimary = colors.background,
            background = colors.background,
            onBackground = colors.text1,
            surface = colors.surface,
            onSurface = colors.text1,
            error = colors.warning,
        )
    }
    CompositionLocalProvider(
        LocalPmColors provides colors,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
    }
}

@Composable
fun BreathingBackground(
    modifier: Modifier = Modifier,
    session: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val progress = if (reduced) {
        0.45f
    } else {
        val transition = rememberInfiniteTransition(label = "Atemhintergrund")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (session) 15_000 else 10_000,
                    easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f),
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Atemhelligkeit",
        ).value
    }
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(colors.background)
            val center = Offset(
                x = size.width * 0.5f - 6f * density * progress,
                y = size.height * 0.35f + 8f * density * progress,
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.breath.copy(alpha = colors.breath.alpha * progress),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = size.maxDimension * 0.72f,
                ),
            )
        }
        content()
    }
}
