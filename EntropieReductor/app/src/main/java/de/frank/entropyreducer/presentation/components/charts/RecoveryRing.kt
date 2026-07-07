package de.frank.entropyreducer.presentation.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.whoopRecoveryColor

/**
 * Whoop-stiliger Recovery-Ring (Spec §13.1). Score 0-100, Farbverlauf rot/gelb/grün, 360°-Bogen mit
 * weichen Spring-Aenderungen.
 */
@Composable
fun RecoveryRing(
    score: Int?,
    modifier: Modifier = Modifier,
    label: String = "Recovery",
    diameter: Int = 220,
) {
    val cosmos = LocalCosmos.current
    val target = (score ?: 0).coerceIn(0, 100)
    val animated by
        animateFloatAsState(
            targetValue = target / 100f,
            animationSpec = tween(durationMillis = 800),
            label = "recoveryRing",
        )
    // Performance-Fix Loop 4.3: ringColor + Brush sind reine Funktionen von target.
    // Vorher wurde bei jedem Animations-Frame (60 fps × 800 ms tween = ~48 Frames)
    // listOf+ringColor.copy+Brush.sweepGradient neu allokiert. Mit remember(target)
    // bleiben sie ueber die Animation stabil; animated triggert nur das Canvas-Lambda.
    // Frank-Wunsch 2026-06-01: offizielle WHOOP-Recovery-Ampel (67/34-Schwellen).
    val ringColor = remember(target) { whoopRecoveryColor(target.toDouble()) }
    val ringBrush =
        remember(ringColor) { Brush.sweepGradient(listOf(ringColor.copy(alpha = 0.4f), ringColor)) }

    Box(modifier = modifier.size(diameter.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter.dp)) {
            val stroke = 18.dp.toPx()
            // Hintergrundring (matt)
            drawArc(
                color = Color.White.copy(alpha = 0.06f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Score-Bogen (gradient)
            drawArc(
                brush = ringBrush,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = Offset(stroke / 2, stroke / 2),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (score != null) "$score" else "—",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = ringColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = cosmos.textSecondary,
            )
        }
    }
}
