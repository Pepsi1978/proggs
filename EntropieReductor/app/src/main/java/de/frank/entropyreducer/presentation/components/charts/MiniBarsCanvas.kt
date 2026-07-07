package de.frank.entropyreducer.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Performance-optimierte Balken-Graph-Komponente (Performance-Audit 2026-06-21).
 *
 * Ersetzt das bisherige Pattern aus 30 einzelnen Box-Composables pro Graph-Card
 * (Row + forEach { Box(weight, fillMaxHeight, clip, background) }) durch EIN
 * Canvas das alle Balken mit drawRoundRect zeichnet.
 *
 * Vorteil: 1 Composable + 1 Layout-Messung + 30 drawRect statt
 *          31 Composables + 31 Layout-Messungen + 31 Draw-Passes.
 * Beim langsamen Scrollen des Biomarker-Tabs (10 GraphCards sichtbar)
 * reduziert das die Compose-Node-Zahl um ~300 und eliminert den Frame-Drop
 * der beim Rein-Scrollen neuer Cards auftrat.
 *
 * Visuell IDENTISCH zum bisherigen Box-Pattern:
 * - Container: 80dp hoch, RoundedCornerShape(8dp), glassBg-Hintergrund
 * - Innen: 6dp Padding, 2dp spacing zwischen Balken
 * - Balken: gleiche Breite (weight 1f), unten ausgerichtet,
 *   RoundedCornerShape(2dp) Ecken, ratio coerced 0.05–1.0
 *
 * @param values   Datenpunkte (typischerweise 30 Tageswerte)
 * @param barColor Farbe pro Wert — Caller reicht seine Schwellen-Logik durch
 * @param yMin     Untere Y-Grenze (bereits vom Caller aus values oder fest vorgegeben)
 * @param yMax     Obere Y-Grenze
 * @param emptyText Text der bei leerer Liste angezeigt wird (z.B. "Noch keine HRV-Daten")
 */
@Composable
internal fun MiniBarsCanvas(
    values: List<Double>,
    barColor: (Double) -> Color,
    yMin: Double,
    yMax: Double,
    emptyText: String,
) {
    val cosmos = LocalCosmos.current
    val yRange = remember(yMin, yMax) { (yMax - yMin).coerceAtLeast(1.0) }

    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(cosmos.glassBg)
    ) {
        if (values.isEmpty()) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
                modifier = Modifier.padding(4.dp),
            )
        } else {
            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val padPx = 6.dp.toPx()
                val spacingPx = 2.dp.toPx()
                val cornerPx = 2.dp.toPx()
                val availW = size.width - 2f * padPx
                val availH = size.height - 2f * padPx
                val n = values.size
                if (n <= 0 || availW <= 0f || availH <= 0f) return@Canvas
                val barW = (availW - (n - 1) * spacingPx) / n.toFloat()
                if (barW <= 0f) return@Canvas
                for (i in 0 until n) {
                    val value = values[i]
                    val ratio =
                        ((value - yMin) / yRange).coerceIn(0.05, 1.0).toFloat()
                    val barH = availH * ratio
                    val x = padPx + i * (barW + spacingPx)
                    val y = padPx + (availH - barH)
                    drawRoundRect(
                        color = barColor(value),
                        topLeft = Offset(x, y),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                    )
                }
            }
        }
    }
}
