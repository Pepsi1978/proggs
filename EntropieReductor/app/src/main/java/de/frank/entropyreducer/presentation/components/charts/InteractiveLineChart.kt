package de.frank.entropyreducer.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Interaktiver Linien-Chart mit Y-Achse, X-Achse und Tap-zu-Tooltip.
 *
 * Frank-Wunsch 2026-05-08: "Y-Skala muss da sein, Tap auf Punkt zeigt den Wert,
 * Datum sichtbar".
 *
 * Zeichnet:
 * - Y-Achsen-Beschriftung links (Min/Mid/Max)
 * - Datenlinie + Punkte
 * - X-Achsen-Datums-Labels (erstes/letztes Datum)
 * - Tooltip bei Tap: zeigt Datum + Wert ueber dem getroffenen Punkt
 *
 * Punkt mit kleinster Distanz zum Tap-X wird selektiert.
 */
@Composable
fun InteractiveLineChart(
    points: List<Pair<Long, Double>>,
    accent: Color,
    unit: String,
    modifier: Modifier = Modifier,
    height: Int = 180,
) {
    val cosmos = LocalCosmos.current
    val safe = points.filter { it.second.isFinite() }.sortedBy { it.first }
    if (safe.isEmpty()) {
        Text(
            text = "Keine Daten — sync dein Whoop-Armband.",
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    val minY = safe.minOf { it.second }
    val maxY = safe.maxOf { it.second }
    val rangeY = (maxY - minY).coerceAtLeast(1.0)
    val midY = (minY + maxY) / 2.0
    val firstDate = Instant.ofEpochMilli(safe.first().first).atZone(ZoneId.systemDefault()).toLocalDate()
    val lastDate = Instant.ofEpochMilli(safe.last().first).atZone(ZoneId.systemDefault()).toLocalDate()

    var selectedIndex by remember(safe.size) { mutableStateOf<Int?>(null) }

    Row(modifier = modifier.fillMaxWidth()) {
        // Y-Achse links
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.height(height.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = formatY(maxY) + if (unit.isNotBlank()) " $unit" else "",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = formatY(midY) + if (unit.isNotBlank()) " $unit" else "",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = formatY(minY) + if (unit.isNotBlank()) " $unit" else "",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .height(height.dp)
                .fillMaxWidth(),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height.dp)
                    .pointerInput(safe) {
                        detectTapGestures { tap ->
                            // Index mit kleinster X-Distanz finden
                            val w = size.width.toFloat()
                            val stepX = if (safe.size <= 1) w else w / (safe.size - 1).toFloat()
                            val idx = (tap.x / stepX).toInt().coerceIn(0, safe.size - 1)
                            selectedIndex = idx
                        }
                    },
            ) {
                val w = size.width
                val h = size.height
                val gridColor = cosmos.glassBorder
                // Horizontale Hilfslinien (3 Stueck)
                listOf(0f, h / 2f, h).forEach { y ->
                    drawLine(
                        color = gridColor.copy(alpha = 0.3f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                    )
                }
                // Datenlinie + Punkte
                if (safe.size >= 2) {
                    val stepX = w / (safe.size - 1).toFloat()
                    for (i in 0 until safe.size - 1) {
                        val x1 = i * stepX
                        val x2 = (i + 1) * stepX
                        val y1 = h - ((safe[i].second - minY) / rangeY * h).toFloat()
                        val y2 = h - ((safe[i + 1].second - minY) / rangeY * h).toFloat()
                        drawLine(
                            color = accent,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 4f,
                        )
                    }
                }
                // Punkte
                val stepX = if (safe.size <= 1) 0f else w / (safe.size - 1).toFloat()
                safe.forEachIndexed { i, (_, v) ->
                    val x = i * stepX
                    val y = h - ((v - minY) / rangeY * h).toFloat()
                    val isSelected = selectedIndex == i
                    drawCircle(
                        color = if (isSelected) accent else accent.copy(alpha = 0.85f),
                        radius = if (isSelected) 8f else 4f,
                        center = Offset(x, y),
                    )
                    if (isSelected) {
                        drawCircle(
                            color = Color.White,
                            radius = 8f,
                            center = Offset(x, y),
                            style = Stroke(width = 2f),
                        )
                    }
                }
            }
            // Tooltip oberhalb des selektierten Punkts
            selectedIndex?.let { idx ->
                val (ts, value) = safe[idx]
                val date = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(
                            color = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "${date.format(SHORT_DATE)}: ${formatY(value)} $unit",
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
    // X-Achse: erstes und letztes Datum unten
    Spacer(Modifier.height(2.dp))
    Row(modifier = Modifier.fillMaxWidth().padding(start = 40.dp)) {
        Text(
            text = firstDate.format(SHORT_DATE),
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = lastDate.format(SHORT_DATE),
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun formatY(value: Double): String {
    val abs = kotlin.math.abs(value)
    return when {
        abs >= 1000 -> "%.0f".format(value)
        abs >= 100 -> "%.0f".format(value)
        abs >= 10 -> "%.1f".format(value)
        else -> "%.1f".format(value)
    }
}

private val SHORT_DATE: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM", Locale.GERMANY)
