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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.perfectmoment.ui.theme.Inter
import de.frank.perfectmoment.ui.theme.LocalPmColors
import de.frank.perfectmoment.ui.theme.LocalReducedMotion
import de.frank.perfectmoment.ui.theme.PmTextStyles

fun Modifier.pmClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = null,
    indication = null,
    enabled = enabled,
    onClick = onClick,
)

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.pmCombinedClickable(
    enabled: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
): Modifier = combinedClickable(
    interactionSource = null,
    indication = null,
    enabled = enabled,
    onClick = onClick,
    onLongClick = onLongClick,
)

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    titleSize: Int = 26,
) {
    val colors = LocalPmColors.current
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 20.dp),
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
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = PmTextStyles.section,
        color = LocalPmColors.current.text2,
        modifier = modifier,
    )
}

@Composable
fun PmCard(
    modifier: Modifier = Modifier,
    radius: Int = 20,
    color: Color = LocalPmColors.current.surface,
    content: @Composable () -> Unit,
) {
    val colors = LocalPmColors.current
    val shape = RoundedCornerShape(radius.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = if (colors.dark) 0.dp else 2.dp,
                shape = shape,
                ambientColor = Color(0x14785418),
                spotColor = Color(0x14785418),
            )
            .background(color, shape),
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
    val background = if (enabled) {
        Brush.horizontalGradient(listOf(colors.gold, colors.goldHi))
    } else {
        Brush.linearGradient(listOf(colors.surface2, colors.surface2))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .shadow(
                elevation = if (enabled) 6.dp else 0.dp,
                shape = RoundedCornerShape((height / 2).dp),
                ambientColor = colors.gold.copy(alpha = 0.3f),
                spotColor = colors.gold.copy(alpha = 0.3f),
            )
            .background(background, RoundedCornerShape((height / 2).dp))
            .then(if (enabled) Modifier.pmClickable(onClick = onClick) else Modifier),
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
            .border(1.dp, color, RoundedCornerShape((height / 2).dp))
            .pmClickable(onClick = onClick),
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
) {
    val colors = LocalPmColors.current
    PmCard(modifier.pmClickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = if (compact) 10.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label.uppercase(),
                color = colors.text2,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = if (compact) 10.sp else 11.sp,
                letterSpacing = 0.8.sp,
                maxLines = 1,
            )
            Text(
                value,
                color = colors.text1,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 16.sp else 18.sp,
                modifier = Modifier.padding(top = if (compact) 3.dp else 4.dp),
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
            .pmClickable { onCheckedChange(!checked) },
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
            modifier = Modifier.fillMaxWidth().height(if (supporting == null) 60.dp else 68.dp)
                .then(if (onClick != null) Modifier.pmClickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp),
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
                    modifier = Modifier.padding(start = if (statusColor == null) 12.dp else 8.dp)
                        .weight(0.8f, fill = false),
                )
            }
            trailing?.invoke()
            if (showChevron) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colors.goldDim,
                    modifier = Modifier.padding(start = 6.dp).size(18.dp),
                )
            }
        }
        if (divider) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.surface2).align(Alignment.BottomCenter))
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
    Box(
        modifier = modifier.height(44.dp).background(
            if (selected) colors.gold else colors.surface2,
            RoundedCornerShape(22.dp),
        ).pmClickable(onClick = onClick),
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
fun RecorderControl(state: RecordingState, message: String?, onClick: () -> Unit) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "Aufnahme")
    val pulse by transition.animateFloat(
        initialValue = if (reduced) 0.45f else 0.3f,
        targetValue = if (reduced) 0.45f else 0.95f,
        animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Reverse),
        label = "Aufnahmeglühen",
    )
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(4_500), RepeatMode.Restart),
        label = "Aufnahmeschimmer",
    )
    val ringPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Restart),
        label = "Aufnahmeringe",
    )
    val spinnerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 360f,
        animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing), RepeatMode.Restart),
        label = "Verarbeitung",
    )
    val recorderBrush = when (state) {
        RecordingState.IDLE -> Brush.linearGradient(listOf(colors.gold, colors.goldHi))
        RecordingState.RECORDING -> Brush.linearGradient(listOf(colors.amber, colors.amber))
        RecordingState.PROCESSING -> Brush.linearGradient(listOf(colors.surface2, colors.surface2))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            if (!reduced && state != RecordingState.PROCESSING) {
                Canvas(Modifier.size(104.dp)) {
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
                Modifier.size(72.dp).background(recorderBrush, CircleShape)
                    .pmClickable(enabled = state != RecordingState.PROCESSING, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                when (state) {
                    RecordingState.IDLE -> Icon(Icons.Outlined.Mic, "Aufnehmen", tint = colors.background, modifier = Modifier.size(28.dp))
                    RecordingState.RECORDING -> {
                        if (!reduced) {
                            Canvas(Modifier.size(104.dp)) {
                                repeat(3) { index ->
                                    val phase = (ringPhase + index / 3f) % 1f
                                    drawCircle(
                                        colors.amber.copy(alpha = 0.35f * (1f - phase)),
                                        radius = 36.dp.toPx() + 40.dp.toPx() * phase,
                                        style = Stroke(1.5.dp.toPx()),
                                    )
                                }
                            }
                        }
                        Icon(Icons.Outlined.Stop, "Aufnahme stoppen", tint = colors.background, modifier = Modifier.size(24.dp))
                    }
                    RecordingState.PROCESSING -> Canvas(Modifier.size(32.dp)) {
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
            Box(Modifier.size(8.dp).background(colors.gold.copy(alpha = (phase - index * 0.16f).coerceIn(0.2f, 1f)), CircleShape))
        }
    }
}

@Composable
fun CheckMark(visible: Boolean) {
    Icon(
        Icons.Outlined.Check,
        contentDescription = if (visible) "Ausgewählt" else null,
        tint = LocalPmColors.current.gold.copy(alpha = if (visible) 1f else 0.12f),
        modifier = Modifier.size(24.dp),
    )
}

@Composable
fun SpeakerButton(speaking: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalPmColors.current
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "Lautsprecher")
    val pulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (reduced) 0.25f else 0.5f,
        animationSpec = infiniteRepeatable(tween(1_200), RepeatMode.Reverse),
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
