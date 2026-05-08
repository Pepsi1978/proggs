package de.frank.entropyreducer.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlin.math.roundToInt

/**
 * Schmaler Linien-Chart für Zeitreihen wie HRV oder Strain.
 * Bewusst leichtgewichtig — kein Vico-Overhead für 30 Tage.
 *
 * Gestrichelte Median-Linie hilft beim Vergleich zum 30-Tage-Mittel.
 */
@Composable
fun HrvLineChart(
    values: List<Double>,
    modifier: Modifier = Modifier,
    accent: Color = CosmosColors.AccentPrimary,
    title: String = "HRV (30 Tage)",
    unit: String = "ms",
    height: Int = 140,
) {
    val cosmos = LocalCosmos.current
    val safe = values.filter { it.isFinite() }
    val median = safe.takeIf { it.isNotEmpty() }?.sorted()?.let { it[it.size / 2] }
    val min = safe.minOrNull() ?: 0.0
    val max = safe.maxOrNull() ?: 1.0
    val range = (max - min).takeIf { it > 0 } ?: 1.0

    Box(modifier = modifier.fillMaxWidth().height(height.dp)) {
        if (safe.isEmpty()) {
            Text(
                text = "Noch keine Daten — verbinde Whoop um $title zu sehen.",
                modifier = Modifier.padding(12.dp).align(Alignment.Center),
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            return@Box
        }

        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val padding = 12f
            val w = size.width - padding * 2
            val h = size.height - padding * 2
            val stepX = w / (safe.size - 1).coerceAtLeast(1)

            // Median-Linie (gestrichelt)
            median?.let { m ->
                val y = padding + h - ((m - min) / range * h).toFloat()
                drawLine(
                    color = cosmos.textSecondary.copy(alpha = 0.6f),
                    start = Offset(padding, y),
                    end = Offset(padding + w, y),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                )
            }

            // Datenpfad
            val path = Path()
            safe.forEachIndexed { idx, v ->
                val x = padding + idx * stepX
                val y = padding + h - ((v - min) / range * h).toFloat()
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = accent,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
            )

            // Punkte am letzten Wert
            val lastIdx = safe.size - 1
            val lastX = padding + lastIdx * stepX
            val lastY = padding + h - ((safe.last() - min) / range * h).toFloat()
            drawCircle(color = accent, radius = 5f, center = Offset(lastX, lastY))
            drawCircle(color = accent.copy(alpha = 0.3f), radius = 12f, center = Offset(lastX, lastY))
        }
    }
}
