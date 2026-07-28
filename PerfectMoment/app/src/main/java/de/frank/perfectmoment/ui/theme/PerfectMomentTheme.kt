package de.frank.perfectmoment.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.perfectmoment.R
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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

/**
 * False while an overlay (sheet, dialog, message) covers the screen. Endless animations below the
 * overlay stop, so the blurred layer stays static and can be cached instead of being re-rendered
 * offscreen every frame. Purely a scheduling hint — no effect is removed.
 */
val LocalMotionActive = compositionLocalOf { true }

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

/**
 * Screen backdrop of the design: static gold gradient plus two corner glows, the slowly breathing
 * gold shimmer (`pm-fx-breathe`, 20s / 30s in a session) and the aurora sweep (`pm-fx-aurora`, 18s)
 * that sits *above* the content with soft-light blending, exactly like the `::after` layer in the
 * design.
 *
 * Every shader is built once per size/palette in [drawWithCache]; the animations only move the draw
 * matrix and alpha, so a frame costs a matrix update instead of rebuilding full-screen gradients.
 * Animated values are read inside the draw lambdas (draw phase), never in the composition body.
 */
@Composable
fun BreathingBackground(
    modifier: Modifier = Modifier,
    session: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val progressState: State<Float>
    val auroraProgressState: State<Float>
    if (reduced) {
        progressState = remember { mutableFloatStateOf(0f) }
        auroraProgressState = remember { mutableFloatStateOf(0f) }
    } else {
        val transition = rememberInfiniteTransition(label = "Atemhintergrund")
        progressState = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = if (session) 30_000 else 20_000,
                    easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f),
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Atemhelligkeit",
        )
        auroraProgressState = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(18_000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "Aurora",
        )
    }
    Box(
        modifier.fillMaxSize().drawWithCache {
            val backgroundLine = gradientLine(size, 145f)
            val base = Brush.linearGradient(
                0f to mix(colors.background, colors.gold, 0.04f),
                0.52f to colors.background,
                1f to mix(colors.background, colors.goldHi, 0.03f),
                start = backgroundLine.first,
                end = backgroundLine.second,
            )
            val topGlowRadius = 300.dp.toPx()
            val topGlowCenter = Offset(size.width * 1.08f, size.height * -0.04f)
            val topGlow = Brush.radialGradient(
                0f to colors.goldHi.copy(alpha = 0.16f),
                0.72f to Color.Transparent,
                1f to Color.Transparent,
                center = topGlowCenter,
                radius = topGlowRadius,
            )
            val bottomGlowRadius = 360.dp.toPx()
            val bottomGlowCenter = Offset(size.width * -0.14f, size.height * 0.82f)
            val bottomGlow = Brush.radialGradient(
                0f to colors.gold.copy(alpha = 0.09f),
                0.74f to Color.Transparent,
                1f to Color.Transparent,
                center = bottomGlowCenter,
                radius = bottomGlowRadius,
            )
            val breathCenter = Offset(size.width * 0.5f, size.height * 0.35f)
            val breathRadius = size.maxDimension * 0.72f
            val breath = Brush.radialGradient(
                0f to colors.breath,
                1f to Color.Transparent,
                center = breathCenter,
                radius = breathRadius,
            )
            val driftX = 6.dp.toPx()
            val driftY = 8.dp.toPx()
            onDrawBehind {
                val progress = progressState.value
                drawRect(base)
                drawCircle(topGlow, radius = topGlowRadius, center = topGlowCenter)
                drawCircle(bottomGlow, radius = bottomGlowRadius, center = bottomGlowCenter)
                // pm-fx-breathe: opacity 0.15 -> 0.72 (50%) -> 1.0, drift -6px/+8px, scale 1.02
                val breathOpacity = when {
                    reduced -> 0.45f
                    progress < 0.5f -> 0.15f + 0.57f * progress * 2f
                    else -> 0.72f + 0.28f * (progress - 0.5f) * 2f
                }
                if (reduced) {
                    drawCircle(breath, radius = breathRadius, center = breathCenter, alpha = breathOpacity)
                } else {
                    withTransform({
                        translate(-driftX * progress, driftY * progress)
                        scale(1f + 0.02f * progress, 1f + 0.02f * progress, breathCenter)
                    }) {
                        drawCircle(breath, radius = breathRadius, center = breathCenter, alpha = breathOpacity)
                    }
                }
            }
        },
    ) {
        content()
        // ::after — aurora sweep above the content, soft-light, non-interactive
        Box(
            Modifier.matchParentSize().drawWithCache {
                val sweep = Brush.linearGradient(
                    0f to Color.Transparent,
                    0.38f to Color.Transparent,
                    0.48f to colors.goldHi.copy(alpha = 0.08f),
                    0.58f to Color.Transparent,
                    1f to Color.Transparent,
                    start = Offset(-size.width, size.height),
                    end = Offset(size.width * 1.4f, 0f),
                )
                val haloCenter = Offset(size.width * 0.5f, size.height * 0.35f)
                val haloRadius = size.maxDimension * 0.66f
                val halo = Brush.radialGradient(
                    0f to colors.gold.copy(alpha = 0.07f),
                    0.66f to Color.Transparent,
                    1f to Color.Transparent,
                    center = haloCenter,
                    radius = haloRadius,
                )
                val haloDriftX = size.width * 0.02f
                val haloDriftY = size.height * 0.02f
                onDrawBehind {
                    val auroraProgress = auroraProgressState.value
                    // pm-fx-aurora: opacity 0.10 -> 0.22 (50%) -> 0.14
                    val auroraOpacity = when {
                        reduced -> 0.10f
                        auroraProgress < 0.5f -> 0.10f + 0.12f * auroraProgress * 2f
                        else -> 0.22f - 0.08f * (auroraProgress - 0.5f) * 2f
                    }
                    // background-size 240% -> travels 140% of the width from 0% to 100%
                    withTransform({ translate(size.width * 1.4f * auroraProgress, 0f) }) {
                        drawRect(
                            brush = sweep,
                            topLeft = Offset(-size.width * 1.4f, 0f),
                            size = Size(size.width * 2.8f, size.height),
                            alpha = auroraOpacity,
                            blendMode = BlendMode.Softlight,
                        )
                    }
                    withTransform({ translate(haloDriftX * auroraProgress, -haloDriftY * auroraProgress) }) {
                        drawCircle(
                            brush = halo,
                            radius = haloRadius,
                            center = haloCenter,
                            alpha = auroraOpacity,
                            blendMode = BlendMode.Softlight,
                        )
                    }
                }
            },
        )
    }
}

private fun mix(from: Color, to: Color, amount: Float): Color = Color(
    red = from.red + (to.red - from.red) * amount,
    green = from.green + (to.green - from.green) * amount,
    blue = from.blue + (to.blue - from.blue) * amount,
    alpha = from.alpha + (to.alpha - from.alpha) * amount,
)

private fun gradientLine(size: Size, cssAngleDegrees: Float): Pair<Offset, Offset> {
    val radians = Math.toRadians(cssAngleDegrees.toDouble()).toFloat()
    val x = sin(radians)
    val y = -cos(radians)
    val halfLength = (abs(size.width * x) + abs(size.height * y)) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    return center - Offset(x * halfLength, y * halfLength) to
        center + Offset(x * halfLength, y * halfLength)
}
