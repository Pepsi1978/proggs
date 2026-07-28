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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
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
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.LocalReducedMotion
import de.frank.perfectmoment.ui.theme.PmColors
import de.frank.perfectmoment.ui.theme.PmTextStyles
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

fun Modifier.pmClickable(
    enabled: Boolean = true,
    role: Role? = null,
    onClick: () -> Unit,
): Modifier = clickable(enabled = enabled, role = role, onClick = onClick)

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pmCombinedClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)

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
    val flow = if (enabled && !reduced) {
        val transition = rememberInfiniteTransition(label = "Goldfluss")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(4_000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                RepeatMode.Reverse,
            ),
            label = "Goldposition",
        ).value
    } else {
        0f
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
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = shape,
                ambientColor = colors.gold.copy(alpha = 0.28f),
                spotColor = colors.gold.copy(alpha = 0.28f),
            )
            .clip(shape)
            .pmClickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.matchParentSize()) {
            if (enabled) {
                val startX = -size.width * 0.9f * flow
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(colors.gold, colors.goldHi, colors.gold),
                        start = Offset(startX, 0f),
                        end = Offset(startX + size.width * 1.9f, 0f),
                    ),
                )
                drawLine(
                    Color.White.copy(alpha = 0.28f),
                    Offset(radius.dp.toPx(), 1.dp.toPx()),
                    Offset(size.width - radius.dp.toPx(), 1.dp.toPx()),
                    1.dp.toPx(),
                )
            } else {
                drawRect(colors.surface2)
            }
        }
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
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape((height / 2).dp))
            .pmClickable(role = Role.Button, onClick = onClick),
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
    PmCard(modifier.pmClickable(onClick = onClick), radius = radius) {
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
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().height(if (supporting == null) 60.dp else 72.dp)
                .then(if (onClick != null) Modifier.pmClickable(onClick = onClick) else Modifier)
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
            .semantics { this.selected = selected }
            .pmClickable(role = Role.RadioButton, onClick = onClick),
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
    val pulse: Float
    val shimmer: Float
    val ringPhase: Float
    val spinnerRotation: Float
    when {
        reduced -> {
            pulse = 0.45f
            shimmer = 0f
            ringPhase = 0f
            spinnerRotation = 0f
        }
        state != RecordingState.PROCESSING -> {
            val transition = rememberInfiniteTransition(label = "Aufnahme")
            pulse = transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    tween(1_600, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                    RepeatMode.Reverse,
                ),
                label = "Aufnahmeglühen",
            ).value
            shimmer = transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(4_500, easing = LinearEasing), RepeatMode.Restart),
                label = "Aufnahmeschimmer",
            ).value
            ringPhase = if (state == RecordingState.RECORDING) {
                transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing), RepeatMode.Restart),
                    label = "Aufnahmeringe",
                ).value
            } else {
                0f
            }
            spinnerRotation = 0f
        }
        else -> {
            val transition = rememberInfiniteTransition(label = "Verarbeitung")
            pulse = 0.45f
            shimmer = 0f
            ringPhase = 0f
            spinnerRotation = transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing), RepeatMode.Restart),
                label = "Verarbeitung",
            ).value
        }
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
            if (!reduced && state != RecordingState.PROCESSING) {
                Canvas(Modifier.size(outerSize)) {
                    drawCircle(colors.gold.copy(alpha = 0.13f * pulse), radius = size.minDimension * 0.5f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, colors.goldHi, Color.Transparent),
                        ),
                        startAngle = shimmer,
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
                        if (!reduced) {
                            Canvas(Modifier.size(outerSize)) {
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
                            startAngle = spinnerRotation,
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

@Composable
fun LoadingDots(modifier: Modifier = Modifier) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "Ladepunkte")
    val phase by transition.animateFloat(
        0.2f,
        if (reduced) 0.2f else 1f,
        infiniteRepeatable(tween(700, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)), RepeatMode.Reverse),
        label = "Punkte",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val localPhase = (phase - index * 0.16f).coerceIn(0.2f, 1f)
            Box(
                Modifier.size(8.dp).graphicsLayer { translationY = -2.dp.toPx() * localPhase }
                    .shadow(
                        5.dp,
                        CircleShape,
                        ambientColor = colors.gold.copy(alpha = 0.40f),
                        spotColor = colors.gold.copy(alpha = 0.40f),
                    )
                    .background(colors.gold.copy(alpha = localPhase), CircleShape),
            )
        }
    }
}

@Composable
fun OrbitRing(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "Orbit")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(
            tween(2_000, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
            RepeatMode.Reverse,
        ),
        label = "Orbitpuls",
    )
    Box(
        modifier = modifier.graphicsLayer {
            scaleX = 1f + phase * 0.045f
            scaleY = 1f + phase * 0.045f
            alpha = if (reduced) 1f else 0.72f + phase * 0.28f
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

@Composable
fun GoldWordmark(text: String, modifier: Modifier = Modifier) {
    val colors = LocalPmColors.current
    Box(modifier) {
        val density = LocalDensity.current
        Text(
            text,
            style = TextStyle(
                brush = Brush.horizontalGradient(
                    listOf(colors.gold, colors.goldHi, colors.gold),
                ),
                shadow = Shadow(colors.gold.copy(alpha = 0.16f), blurRadius = with(density) { 24.dp.toPx() }),
            ),
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
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

@Composable
fun SpeakerButton(speaking: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "Lautsprecher")
    val pulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (reduced) 0.25f else 0.5f,
        animationSpec = infiniteRepeatable(
            tween(1_200, easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
            RepeatMode.Reverse,
        ),
        label = "Lautsprecherpuls",
    )
    Box(
        Modifier.size(50.dp).pmClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (speaking) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(colors.amber.copy(alpha = pulse), Color.Transparent),
                        center = center,
                        radius = size.minDimension / 2f,
                    ),
                    radius = size.minDimension / 2f,
                )
            }
        }
        Icon(
            if (speaking) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
            if (speaking) "Lautsprecher ausschalten" else "Lautsprecher einschalten",
            tint = if (speaking) colors.amber else colors.text1.copy(alpha = 0.7f),
            modifier = Modifier.size(26.dp),
        )
    }
}
