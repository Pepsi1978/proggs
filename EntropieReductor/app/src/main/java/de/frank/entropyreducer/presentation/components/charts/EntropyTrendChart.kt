package de.frank.entropyreducer.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.ShiftCode
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import de.frank.entropyreducer.presentation.theme.label

/**
 * Multi-Linien-Trend-Chart fuer Dashboard 2 (Spec §11.1.4).
 *
 * - X-Achse: Tage (timeline-aligniert).
 * - Y-Achse: Entropie-Last pro Kategorie (0..100 normalisiert).
 * - Schichtblock-Hintergrund: TAGDIENST gelblich, NACHTDIENST violett, FREI gruen,
 *   URLAUB cyan — als duenne, halb-transparente vertikale Streifen.
 *
 * Erwartet [series] in Kategorien-Bukets, jeweils ein Punkt pro Tag (gleich lang
 * wie [shifts]). Werte aus Severity-Summen, normalisiert auf [0..100].
 */
@Composable
fun EntropyTrendChart(
    series: Map<EntropyCategory, List<Double>>,
    shifts: List<ShiftCode>,
    modifier: Modifier = Modifier,
    height: Int = 220,
) {
    val cosmos = LocalCosmos.current
    val days = shifts.size
    val empty = series.isEmpty() || days < 2

    Box(modifier = modifier.fillMaxWidth().height(height.dp)) {
        if (empty) {
            Text(
                text = "Noch keine Trend-Daten — sammle ein paar Tage Eintraege.",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp).align(Alignment.Center),
            )
            return@Box
        }

        val maxValue = series.values.flatten().filter { it.isFinite() }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

        Canvas(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            val padX = 12f
            val padY = 12f
            val w = size.width - padX * 2
            val h = size.height - padY * 2

            // 1. Schichtblock-Hintergrund
            val stripeWidth = w / days
            shifts.forEachIndexed { idx, shift ->
                val x0 = padX + idx * stripeWidth
                val color = when (shift) {
                    ShiftCode.TAGDIENST -> Color(0x14FBBF24)     // gelblich
                    ShiftCode.NACHTDIENST -> Color(0x14A78BFA)   // violett
                    ShiftCode.FREI -> Color(0x1234D399)          // gruen
                    ShiftCode.URLAUB -> Color(0x1422D3EE)        // cyan
                    ShiftCode.UNBEKANNT -> Color.Transparent
                }
                if (color != Color.Transparent) {
                    drawRect(
                        color = color,
                        topLeft = Offset(x0, padY),
                        size = Size(stripeWidth, h),
                    )
                }
            }

            // 2. Linien je Kategorie
            series.forEach { (cat, values) ->
                if (values.isEmpty()) return@forEach
                val path = Path()
                val stepX = if (values.size > 1) w / (values.size - 1) else w
                values.forEachIndexed { idx, v ->
                    val x = padX + idx * stepX
                    val y = padY + h - ((v / maxValue) * h).toFloat()
                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = cat.color(),
                    style = Stroke(width = 2.5f),
                )
            }
        }
    }
}

/**
 * Legende fuer [EntropyTrendChart] mit den dargestellten Kategorien.
 */
@Composable
fun EntropyTrendLegend(
    categories: List<EntropyCategory>,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        categories.forEach { cat ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(cat.color()),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = cat.label(),
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
