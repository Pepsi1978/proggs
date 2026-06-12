package de.frank.entropyreducer.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    accent: Color = LocalCosmos.current.accent,
    title: String = "HRV (30 Tage)",
    unit: String = "ms",
    height: Int = 140,
) {
    val cosmos = LocalCosmos.current
    // Performance-Fix Loop 4.2: filter+sorted+min+max haengen nur von values ab.
    // Vorher: bei jeder Recomposition wurden filter (Listen-Allokation), sorted
    // (O(N log N) und Listen-Allokation), min, max neu berechnet. Bei 30-Tage-
    // HRV-Werten sind das je vier separate Iterationen plus eine sortierte Kopie.
    // Mit remember(values) wird das nur bei tatsaechlicher Aenderung neu gemacht.
    val stats = remember(values) {
        val safe = values.filter { it.isFinite() }
        val median = safe.takeIf { it.isNotEmpty() }?.sorted()?.let { it[it.size / 2] }
        val min = safe.minOrNull() ?: 0.0
        val max = safe.maxOrNull() ?: 1.0
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        ChartStats(safe = safe, median = median, min = min, max = max, range = range)
    }
    val safe = stats.safe
    val median = stats.median
    val min = stats.min
    val range = stats.range

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

        // Performance-Audit Loop 1 (2026-05-10): Path + PathEffect einmalig
        // allokieren statt 60x/Sekunde im Canvas-Lambda.
        val dataPath = remember { Path() }
        val medianDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f)) }
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
                    pathEffect = medianDashEffect,
                )
            }

            // Datenpfad — sharedPath wird pro Frame zurueckgesetzt
            dataPath.reset()
            val path = dataPath
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

/** Pre-computed Chart-Statistik fuer HrvLineChart — gehalten ueber Recomposition
 *  durch remember(values) damit filter+sorted+min+max+range nur bei
 *  Werte-Aenderung neu laufen, nicht bei jeder parent-getriggerten Recomposition. */
private data class ChartStats(
    val safe: List<Double>,
    val median: Double?,
    val min: Double,
    val max: Double,
    val range: Double,
)
