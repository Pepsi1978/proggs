package de.frank.perfectmoment.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.perfectmoment.ui.theme.Inter
import de.frank.perfectmoment.ui.theme.LocalMotionActive
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.LocalReducedMotion
import de.frank.perfectmoment.ui.theme.PmColors
import de.frank.perfectmoment.ui.theme.PmTextStyles
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Touch feedback of the design. On a phone the finger takes the role the mouse pointer has in the
 * design prototype, so pressing an element plays what the design shows on hover *and* on active:
 *
 * - cards ([lift]): `translate: 0 -2px` and `box-shadow: 0 18px 44px accent 13%` — the card rises
 *   and glows,
 * - buttons: `scale: 0.985`,
 * - everything: `filter: brightness(1.08) saturate(1.06)`,
 * - the `:active` outline in gold (or [pressBorder] amber), 3px,
 *
 * all over `transition: 240ms cubic-bezier(0.2, 0, 0, 1)`.
 *
 * Put this BEFORE the surface modifiers in a chain (`Modifier.size(…).pmClickable(…).pmGlassSurface(…)`),
 * otherwise it only transforms the inner content instead of the whole card.
 */
@Composable
fun Modifier.pmClickable(
    enabled: Boolean = true,
    role: Role? = null,
    shape: Shape = RectangleShape,
    lift: Boolean = false,
    pressBorder: Color? = null,
    source: MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = source ?: remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press = animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "Druck",
    )
    return clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        role = role,
        onClick = onClick,
    ).pmPressEffect(press, shape, lift, pressBorder)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.pmCombinedClickable(
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    lift: Boolean = false,
    pressBorder: Color? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val press = animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "Druck",
    )
    return combinedClickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick,
        onLongClick = onLongClick,
    ).pmPressEffect(press, shape, lift, pressBorder)
}

@Composable
private fun Modifier.pmPressEffect(
    press: State<Float>,
    shape: Shape,
    lift: Boolean,
    pressBorder: Color?,
): Modifier {
    val colors = LocalPmColors.current
    val glow = colors.gold
    val border = pressBorder ?: colors.goldHi
    return graphicsLayer {
        val progress = press.value
        if (progress <= 0f) return@graphicsLayer
        if (lift) {
            translationY = -2.dp.toPx() * progress
            shadowElevation = 18.dp.toPx() * progress
            ambientShadowColor = glow
            spotShadowColor = glow
            this.shape = shape
            clip = false
        } else {
            val factor = 1f - 0.015f * progress
            scaleX = factor
            scaleY = factor
        }
    }.drawWithCache {
        val outlinePath = when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Rectangle -> null
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
        }
        val paint = Paint()
        val stroke = Stroke(3.dp.toPx())
        onDrawWithContent {
            val progress = press.value
            if (progress <= 0f) {
                drawContent()
                return@onDrawWithContent
            }
            paint.colorFilter = pressColorFilter(progress)
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), paint)
                drawContent()
                canvas.restore()
            }
            // :active outline — 3px in gold / amber
            val outlineColor = border.copy(alpha = border.alpha * progress)
            if (outlinePath != null) {
                drawPath(outlinePath, outlineColor, style = stroke)
            } else {
                drawRect(outlineColor, style = stroke)
            }
        }
    }
}

/**
 * The scrim backdrop of every sheet and dialog: `backdrop-filter: blur(8px) saturate(0.82)`.
 *
 * Combine with `LocalMotionActive provides false` for the content underneath — a still tree lets the
 * blur layer be cached instead of being re-rendered offscreen on every animation frame.
 */
fun Modifier.pmScrimBackdrop(): Modifier = blur(8.dp).drawWithCache {
    val paint = Paint().apply {
        colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.82f) })
    }
    val bounds = Rect(Offset.Zero, size)
    onDrawWithContent {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(bounds, paint)
            drawContent()
            canvas.restore()
        }
    }
}

/** `filter: brightness(1.06) saturate(1.05)` at full press, interpolated by [progress]. */
private fun pressColorFilter(progress: Float): ColorFilter {
    val brightness = 1f + 0.06f * progress
    val matrix = ColorMatrix().apply { setToSaturation(1f + 0.05f * progress) }
    for (row in 0..2) {
        for (column in 0..4) {
            matrix[row, column] = matrix[row, column] * brightness
        }
    }
    return ColorFilter.colorMatrix(matrix)
}

fun Modifier.pmHeaderSurface(colors: PmColors): Modifier = drawWithCache {
    val background = Brush.verticalGradient(
        0f to colors.background.copy(alpha = 0.92f),
        0.72f to colors.background.copy(alpha = 0.54f),
        1f to Color.Transparent,
    )
    onDrawBehind {
        drawRect(background)
        val stroke = 1.dp.toPx()
        drawLine(
            colors.gold.copy(alpha = 0.08f),
            Offset(0f, size.height - stroke / 2f),
            Offset(size.width, size.height - stroke / 2f),
            strokeWidth = stroke,
        )
    }
}

fun Modifier.pmGlassSurface(
    colors: PmColors,
    radius: Int,
    color: Color = colors.surface,
): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    val shadowColor = if (colors.dark) Color.Black.copy(alpha = 0.22f) else Color(0x1F785418)
    return shadow(
        elevation = if (colors.dark) 14.dp else 12.dp,
        shape = shape,
        ambientColor = shadowColor,
        spotColor = shadowColor,
    ).clip(shape).drawWithCache {
        val corner = CornerRadius(radius.dp.toPx())
        val highlight = if (colors.dark) colors.goldHi.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.48f)
        val edge = colors.gold.copy(alpha = if (colors.dark) 0.18f else 0.20f)
        val sheenLine = gradientLine(size, 145f)
        val sheen = Brush.linearGradient(
            0f to highlight,
            0.38f to Color.Transparent,
            1f to Color.Transparent,
            start = sheenLine.first,
            end = sheenLine.second,
        )
        val accent = Brush.radialGradient(
            0f to colors.gold.copy(alpha = 0.08f),
            0.42f to Color.Transparent,
            1f to Color.Transparent,
            center = Offset(size.width * 0.92f, size.height * 0.04f),
            radius = size.maxDimension * 0.72f,
        )
        onDrawBehind {
            drawRoundRect(color, cornerRadius = corner)
            drawRoundRect(sheen, cornerRadius = corner)
            drawRoundRect(accent, cornerRadius = corner)
            drawRoundRect(edge, cornerRadius = corner, style = Stroke(1.dp.toPx()))
            drawLine(
                if (colors.dark) colors.goldHi.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.64f),
                Offset(radius.dp.toPx(), 1.dp.toPx()),
                Offset(size.width - radius.dp.toPx(), 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    titleSize: Int = 26,
    horizontalPadding: Int = 24,
) {
    val colors = LocalPmColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).pmHeaderSurface(colors)
            .padding(horizontal = horizontalPadding.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Zurück",
                tint = colors.text1,
                modifier = Modifier.size(24.dp).pmClickable(onClick = onBack),
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            color = colors.text1,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = titleSize.sp,
            letterSpacing = (-0.3).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, decorated: Boolean = false) {
    val colors = LocalPmColors.current
    val glow = with(LocalDensity.current) { 18.dp.toPx() }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text.uppercase(),
            style = PmTextStyles.section.copy(
                shadow = if (decorated) Shadow(colors.gold.copy(alpha = 0.16f), blurRadius = glow) else null,
            ),
            color = colors.text2,
        )
        if (decorated) {
            Box(
                Modifier.weight(1f).padding(start = 10.dp).height(1.dp).background(
                    Brush.horizontalGradient(listOf(colors.gold.copy(alpha = 0.24f), Color.Transparent)),
                ),
            )
        }
    }
}

@Composable
fun PmCard(
    modifier: Modifier = Modifier,
    radius: Int = 32,
    color: Color = LocalPmColors.current.surface,
    content: @Composable () -> Unit,
) {
    val colors = LocalPmColors.current
    Box(
        modifier = modifier.pmGlassSurface(colors, radius, color),
    ) { content() }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Int = 56,
    textSize: Int = 16,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val motionActive = LocalMotionActive.current
    // .pm-start__primary:active — the gold flow gives way to the plain surface with a gold rim
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressState = animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = tween(240, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)),
        label = "Knopfdruck",
    )
    // pm-fx-gold-flow 8s ease-in-out infinite -> 4s each way with RepeatMode.Reverse
    val flowState: State<Float> = if (enabled && !reduced && motionActive) {
        val transition = rememberInfiniteTransition(label = "Goldfluss")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(4_000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                RepeatMode.Reverse,
            ),
            label = "Goldposition",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    val radius = when {
        height >= 60 -> 32
        height >= 56 -> 28
        else -> 26
    }
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .pmClickable(
                enabled = enabled,
                role = Role.Button,
                shape = shape,
                source = interactionSource,
                onClick = onClick,
            )
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                ambientColor = colors.gold.copy(alpha = 0.28f),
                spotColor = colors.gold.copy(alpha = 0.28f),
            )
            .clip(shape)
            .drawWithCache {
                // background-size: 190% 100%, position sweeps the remaining 90%
                val gradient = Brush.linearGradient(
                    colors = listOf(colors.gold, colors.goldHi, colors.gold),
                    start = Offset.Zero,
                    end = Offset(size.width * 1.9f, 0f),
                )
                val gradientSize = Size(size.width * 1.9f, size.height)
                val travel = size.width * 0.9f
                val cornerPx = radius.dp.toPx()
                val linePx = 1.dp.toPx()
                val highlight = Color.White.copy(alpha = 0.28f)
                val disabled = colors.surface2
                val pressedSurface = colors.surface
                val rim = colors.goldHi
                onDrawBehind {
                    if (!enabled) {
                        drawRect(disabled)
                        return@onDrawBehind
                    }
                    withTransform({ translate(-travel * flowState.value, 0f) }) {
                        drawRect(gradient, topLeft = Offset.Zero, size = gradientSize)
                    }
                    drawLine(
                        highlight,
                        Offset(cornerPx, linePx),
                        Offset(size.width - cornerPx, linePx),
                        linePx,
                    )
                    // :active — background: surface, box-shadow: 0 0 0 1px gold-hi
                    val press = pressState.value
                    if (press > 0f) {
                        drawRect(pressedSurface.copy(alpha = press))
                        drawRect(
                            rim.copy(alpha = press),
                            style = Stroke(1.dp.toPx()),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) colors.background else colors.text3,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = textSize.sp,
        )
    }
}

@Composable
fun OutlineButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 52,
) {
    Box(
        modifier = modifier.fillMaxWidth().height(height.dp)
            .pmClickable(role = Role.Button, shape = RoundedCornerShape((height / 2).dp), onClick = onClick)
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape((height / 2).dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    }
}

@Composable
fun ParameterCard(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    radius: Int = 28,
    horizontalPadding: Int = 12,
) {
    val colors = LocalPmColors.current
    // .pm-start__parameter — rises and glows on touch
    PmCard(
        modifier.pmClickable(
            shape = RoundedCornerShape(radius.dp),
            lift = true,
            onClick = onClick,
        ),
        radius = radius,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                horizontal = horizontalPadding.dp,
                vertical = if (compact) 12.dp else 14.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label.uppercase(),
                color = colors.text2,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = if (compact) 10.sp else 11.sp,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                value,
                color = colors.text1,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 16.sp else 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = if (compact) 3.dp else 4.dp),
            )
        }
    }
}

/**
 * Sweep the design runs across a settings row on interaction:
 * `background-image: linear-gradient(90deg, transparent 0 45%, accent 8% 100%)`,
 * `background-size: 220% 100%`, moving from `0 0` to `100% 0` over 360ms.
 */
@Composable
private fun Modifier.pmRowSweep(press: State<Float>): Modifier {
    val colors = LocalPmColors.current
    return drawWithCache {
        val gradient = Brush.linearGradient(
            0f to Color.Transparent,
            0.45f to Color.Transparent,
            1f to colors.gold.copy(alpha = 0.08f),
            start = Offset.Zero,
            end = Offset(size.width * 2.2f, 0f),
        )
        val sweepSize = Size(size.width * 2.2f, size.height)
        val travel = size.width * 1.2f
        onDrawBehind {
            val progress = press.value
            if (progress <= 0f) return@onDrawBehind
            withTransform({ translate(-travel * progress, 0f) }) {
                drawRect(gradient, topLeft = Offset.Zero, size = sweepSize)
            }
        }
    }
}

/** Slider handle of the design: `box-shadow: 0 0 0 6px accent 9%, 0 6px 18px accent 30%`. */
@Composable
fun PmSliderThumb() {
    val colors = LocalPmColors.current
    Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(32.dp).background(colors.gold.copy(alpha = 0.09f), CircleShape))
        Box(
            Modifier.size(20.dp).shadow(
                6.dp,
                CircleShape,
                ambientColor = colors.gold.copy(alpha = 0.30f),
                spotColor = colors.gold.copy(alpha = 0.30f),
            ).background(colors.goldHi, CircleShape),
        )
    }
}

@Composable
fun PmSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalPmColors.current
    Box(
        modifier = Modifier.size(width = 52.dp, height = 32.dp)
            .background(if (checked) colors.gold else colors.surface2, CircleShape)
            .semantics { stateDescription = if (checked) "Ein" else "Aus" }
            .pmClickable(role = Role.Switch) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier.padding(3.dp).size(26.dp)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .background(colors.text1, CircleShape),
        )
    }
}

@Composable
fun SettingRow(
    label: String,
    value: String? = null,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    divider: Boolean = true,
    showChevron: Boolean = false,
    statusColor: Color? = null,
    valueColor: Color? = null,
    valueFontFamily: FontFamily = Inter,
    valueFontSize: Int = 14,
) {
    val colors = LocalPmColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val sweep = animateFloatAsState(
        targetValue = if (pressed && onClick != null) 1f else 0f,
        animationSpec = tween(360),
        label = "Zeilenwisch",
    )
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().height(if (supporting == null) 60.dp else 72.dp)
                .then(
                    if (onClick != null) {
                        Modifier.pmClickable(source = interactionSource, onClick = onClick)
                    } else {
                        Modifier
                    },
                )
                .pmRowSweep(sweep)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, color = colors.text1, fontFamily = Inter, fontSize = 15.sp)
                supporting?.let {
                    Text(it, color = colors.text3, fontFamily = Inter, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (value != null) {
                statusColor?.let {
                    Box(Modifier.padding(start = 12.dp).size(8.dp).background(it, CircleShape))
                }
                Text(
                    value,
                    color = valueColor ?: colors.text2,
                    fontFamily = valueFontFamily,
                    fontSize = valueFontSize.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = if (statusColor == null) 12.dp else 8.dp),
                )
            }
            trailing?.invoke()
            if (showChevron) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colors.goldDim,
                    modifier = Modifier.padding(start = 10.dp).size(18.dp),
                )
            }
        }
        if (divider) {
            Box(
                Modifier.fillMaxWidth().height(1.dp).background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            mix(colors.surface2, colors.gold, 0.20f).copy(alpha = 0.56f),
                            colors.surface2.copy(alpha = 0.56f),
                            Color.Transparent,
                        ),
                    ),
                ).align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
fun Segment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPmColors.current
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier.height(44.dp)
            .pmClickable(role = Role.RadioButton, shape = shape, lift = true, onClick = onClick)
            .then(
                if (selected) {
                    Modifier.shadow(
                        12.dp,
                        shape,
                        ambientColor = colors.gold.copy(alpha = 0.24f),
                        spotColor = colors.gold.copy(alpha = 0.24f),
                    ).border(3.dp, colors.goldHi, shape)
                } else {
                    Modifier
                },
            )
            .background(if (selected) colors.gold else colors.surface2, shape)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (selected) colors.background else colors.text2,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
        )
    }
}

@Composable
fun RecorderControl(
    state: RecordingState,
    message: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val motionActive = LocalMotionActive.current
    val animate = !reduced && motionActive
    val still = remember { mutableFloatStateOf(0f) }
    val restingPulse = remember { mutableFloatStateOf(0.45f) }
    val pulseState: State<Float>
    val shimmerState: State<Float>
    val ringState: State<Float>
    val spinnerState: State<Float>
    when {
        !animate -> {
            pulseState = restingPulse
            shimmerState = still
            ringState = still
            spinnerState = still
        }
        state != RecordingState.PROCESSING -> {
            val transition = rememberInfiniteTransition(label = "Aufnahme")
            // pm-fx-recorder 3200ms ease-in-out infinite -> 1600ms each way
            pulseState = transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    tween(1_600, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                    RepeatMode.Reverse,
                ),
                label = "Aufnahmeglühen",
            )
            shimmerState = transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(4_500, easing = LinearEasing), RepeatMode.Restart),
                label = "Aufnahmeschimmer",
            )
            ringState = if (state == RecordingState.RECORDING) {
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Restart),
                    label = "Aufnahmeringe",
                )
            } else {
                still
            }
            spinnerState = still
        }
        else -> {
            val transition = rememberInfiniteTransition(label = "Verarbeitung")
            pulseState = restingPulse
            shimmerState = still
            ringState = still
            spinnerState = transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing), RepeatMode.Restart),
                label = "Verarbeitung",
            )
        }
    }
    val shimmerBrush = remember(colors.goldHi) {
        Brush.sweepGradient(listOf(Color.Transparent, colors.goldHi, Color.Transparent))
    }
    val recorderBrush = when (state) {
        RecordingState.IDLE -> Brush.linearGradient(listOf(colors.gold, colors.goldHi))
        RecordingState.RECORDING -> Brush.linearGradient(listOf(colors.amber, colors.amber))
        RecordingState.PROCESSING -> Brush.linearGradient(listOf(colors.surface2, colors.surface2))
    }
    val outerSize = (104f * scale).dp
    val buttonSize = (72f * scale).dp
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(outerSize), contentAlignment = Alignment.Center) {
            if (animate && state != RecordingState.PROCESSING) {
                Canvas(Modifier.size(outerSize)) {
                    drawCircle(
                        colors.gold.copy(alpha = 0.13f * pulseState.value),
                        radius = size.minDimension * 0.5f,
                    )
                    drawArc(
                        brush = shimmerBrush,
                        startAngle = shimmerState.value,
                        sweepAngle = 105f,
                        useCenter = false,
                        style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
            Box(
                Modifier.size(buttonSize).shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = (if (state == RecordingState.RECORDING) colors.amber else colors.gold)
                        .copy(alpha = 0.35f),
                    spotColor = (if (state == RecordingState.RECORDING) colors.amber else colors.gold)
                        .copy(alpha = 0.35f),
                ).background(recorderBrush, CircleShape)
                    .pmClickable(enabled = state != RecordingState.PROCESSING, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    RecordingState.IDLE -> Icon(Icons.Outlined.Mic, "Aufnehmen", tint = colors.background, modifier = Modifier.size((28f * scale).dp))
                    RecordingState.RECORDING -> {
                        if (animate) {
                            Canvas(Modifier.size(outerSize)) {
                                val ringPhase = ringState.value
                                repeat(3) { index ->
                                    val phase = (ringPhase + index / 3f) % 1f
                                    drawCircle(
                                        colors.amber.copy(alpha = 0.35f * (1f - phase)),
                                        radius = (36f * scale).dp.toPx() + (40f * scale).dp.toPx() * phase,
                                        style = Stroke(1.5.dp.toPx()),
                                    )
                                }
                            }
                        }
                        Icon(Icons.Outlined.Stop, "Aufnahme stoppen", tint = colors.background, modifier = Modifier.size((24f * scale).dp))
                    }
                    RecordingState.PROCESSING -> Canvas(Modifier.size((32f * scale).dp)) {
                        drawArc(
                            colors.gold,
                            startAngle = spinnerState.value,
                            sweepAngle = 250f,
                            useCenter = false,
                            style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            }
        }
        Text(
            when (state) {
                RecordingState.IDLE -> "Antippen zum Sprechen"
                RecordingState.RECORDING -> "Ich höre zu…"
                RecordingState.PROCESSING -> "Einen Moment…"
            },
            color = colors.text2,
            fontFamily = Inter,
            fontSize = 13.sp,
        )
        message?.let {
            Text(
                it,
                color = colors.text3,
                fontFamily = Inter,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 20.dp, end = 20.dp),
            )
        }
    }
}

/** `pm-fx-dots` — three gold dots lighting up one after another, 700ms, alternating. */
@Composable
fun LoadingDots(modifier: Modifier = Modifier) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val motionActive = LocalMotionActive.current
    val phaseState: State<Float> = if (reduced || !motionActive) {
        remember { mutableFloatStateOf(0.2f) }
    } else {
        val transition = rememberInfiniteTransition(label = "Ladepunkte")
        transition.animateFloat(
            0.2f,
            1f,
            infiniteRepeatable(tween(700, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)), RepeatMode.Reverse),
            label = "Punkte",
        )
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            Box(
                Modifier.size(8.dp)
                    .graphicsLayer {
                        val localPhase = (phaseState.value - index * 0.16f).coerceIn(0.2f, 1f)
                        translationY = -2.dp.toPx() * localPhase
                        alpha = localPhase
                    }
                    .shadow(
                        5.dp,
                        CircleShape,
                        ambientColor = colors.gold.copy(alpha = 0.40f),
                        spotColor = colors.gold.copy(alpha = 0.40f),
                    )
                    .background(colors.gold, CircleShape),
            )
        }
    }
}

/** `pm-fx-orbit 4s ease-in-out infinite` — scale 1 → 1.045, opacity 0.72 → 1. */
@Composable
fun OrbitRing(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val motionActive = LocalMotionActive.current
    val animate = !reduced && motionActive
    val phaseState: State<Float> = if (animate) {
        val transition = rememberInfiniteTransition(label = "Orbit")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(2_000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                RepeatMode.Reverse,
            ),
            label = "Orbitpuls",
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }
    Box(
        modifier = modifier.graphicsLayer {
            val phase = phaseState.value
            scaleX = 1f + phase * 0.045f
            scaleY = 1f + phase * 0.045f
            alpha = if (animate) 0.72f + phase * 0.28f else 1f
        }.shadow(
            12.dp,
            CircleShape,
            ambientColor = colors.gold.copy(alpha = 0.18f),
            spotColor = colors.gold.copy(alpha = 0.18f),
        ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * `.pm-start__title` — gold gradient clipped to the glyphs (`background-clip: text`) that flows with
 * `pm-fx-gold-flow 10s`, plus `text-shadow: 0 0 24px gold 16%`.
 *
 * The gradient is painted over the rendered text with [BlendMode.SrcIn] inside an offscreen layer,
 * so the flow only moves a draw matrix — the text is never laid out or recomposed again.
 */
@Composable
fun GoldWordmark(text: String, modifier: Modifier = Modifier) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val motionActive = LocalMotionActive.current
    // pm-fx-gold-flow 10s -> 5s each way with RepeatMode.Reverse
    val flowState: State<Float> = if (reduced || !motionActive) {
        remember { mutableFloatStateOf(0f) }
    } else {
        val transition = rememberInfiniteTransition(label = "Wortmarke")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(5_000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                RepeatMode.Reverse,
            ),
            label = "Goldfluss Wortmarke",
        )
    }
    Box(modifier) {
        val density = LocalDensity.current
        Text(
            text,
            style = TextStyle(
                color = colors.gold,
                shadow = Shadow(colors.gold.copy(alpha = 0.16f), blurRadius = with(density) { 24.dp.toPx() }),
            ),
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            modifier = Modifier
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithCache {
                    // background-size: 200% 100%, sweeping the remaining 100%
                    val gradient = Brush.linearGradient(
                        colors = listOf(colors.gold, colors.goldHi, colors.gold),
                        start = Offset.Zero,
                        end = Offset(size.width * 2f, 0f),
                    )
                    val gradientSize = Size(size.width * 2f, size.height)
                    onDrawWithContent {
                        drawContent()
                        withTransform({ translate(-size.width * flowState.value, 0f) }) {
                            drawRect(
                                gradient,
                                topLeft = Offset.Zero,
                                size = gradientSize,
                                blendMode = BlendMode.SrcIn,
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

@Composable
fun CheckMark(visible: Boolean) {
    val colors = LocalPmColors.current
    Box(
        Modifier.size(24.dp).background(if (visible) colors.gold else colors.surface2, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Check,
            contentDescription = if (visible) "Ausgewählt" else null,
            tint = colors.background.copy(alpha = if (visible) 1f else 0f),
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The most important button of the app. While speaking, a soft amber glow pulses around the icon
 * (opacity 0.25 → 0.5 → 0.25 over 2.4s). The transition itself only exists while speaking, so a
 * muted speaker costs no animation frames at all.
 */
@Composable
fun SpeakerButton(speaking: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val motionActive = LocalMotionActive.current
    Box(
        Modifier.size(50.dp).pmClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (speaking) {
            val pulseState: State<Float> = if (reduced || !motionActive) {
                remember { mutableFloatStateOf(0.25f) }
            } else {
                val transition = rememberInfiniteTransition(label = "Lautsprecher")
                transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 0.5f,
                    animationSpec = infiniteRepeatable(
                        tween(1_200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                        RepeatMode.Reverse,
                    ),
                    label = "Lautsprecherpuls",
                )
            }
            Spacer(
                Modifier.matchParentSize().drawWithCache {
                    val radius = size.minDimension / 2f
                    val middle = Offset(size.width / 2f, size.height / 2f)
                    val glow = Brush.radialGradient(
                        listOf(colors.amber, Color.Transparent),
                        center = middle,
                        radius = radius,
                    )
                    onDrawBehind {
                        drawCircle(glow, radius = radius, center = middle, alpha = pulseState.value)
                    }
                },
            )
        }
        Icon(
            if (speaking) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
            if (speaking) "Lautsprecher ausschalten" else "Lautsprecher einschalten",
            tint = if (speaking) colors.amber else colors.text1.copy(alpha = 0.7f),
            modifier = Modifier.size(26.dp),
        )
    }
}
