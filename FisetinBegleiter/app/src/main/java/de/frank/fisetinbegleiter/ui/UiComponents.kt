package de.frank.fisetinbegleiter.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.fisetinbegleiter.ui.theme.LocalAppColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm 'Uhr'")

fun formatTime(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(timeFormatter)

fun formatCountdown(target: Long, now: Long): String {
    val seconds = max(0L, (target - now + 999) / 1_000)
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val rest = seconds % 60
    return if (hours > 0) "%d h %02d min".format(hours, minutes) else "%d min %02d s".format(minutes, rest)
}

@Composable
fun SectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    Column(modifier = modifier) {
        Text(
            text = title,
            color = colors.text,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 26.sp,
                lineHeight = 36.4.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
        )
        subtitle?.let {
            Text(
                text = it,
                color = colors.sub,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 21.sp),
            )
        }
    }
}

fun Modifier.angledGradient(
    colors: List<Color>,
    angleDegrees: Float,
): Modifier = drawWithCache {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val directionX = sin(radians).toFloat()
    val directionY = -cos(radians).toFloat()
    val halfLength = (abs(size.width * directionX) + abs(size.height * directionY)) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val start = Offset(
        x = center.x - directionX * halfLength,
        y = center.y - directionY * halfLength,
    )
    val end = Offset(
        x = center.x + directionX * halfLength,
        y = center.y + directionY * halfLength,
    )
    val brush = Brush.linearGradient(colors = colors, start = start, end = end)
    onDrawBehind { drawRect(brush) }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    Card(
        modifier = modifier.shadow(
            elevation = colors.shadow.blurRadius * 0.5f,
            shape = shape,
            clip = false,
            ambientColor = colors.shadow.color,
            spotColor = colors.shadow.color,
        ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = colors.card,
            contentColor = colors.text,
        ),
        border = BorderStroke(1.dp, colors.cardBorder),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun AccentGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        label = "accent-button-alpha",
    )
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "accent-button-scale",
    )
    CompositionLocalProvider(LocalContentColor provides colors.onAccent) {
        Row(
            modifier = modifier
                .shadow(
                    elevation = 14.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = colors.glow,
                    spotColor = colors.glow,
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(shape)
                .background(Brush.linearGradient(listOf(colors.accent, colors.accent2)))
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .alpha(alpha)
                .padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun StatusNotice(
    title: String,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        icon?.invoke()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = text,
                color = colors.sub,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.125.sp),
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun SignalCard(
    title: String,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    StatusNotice(
        title = title,
        text = text,
        containerColor = containerColor,
        contentColor = contentColor,
        modifier = modifier,
        trailing = trailing,
    )
}
