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
 * - Tooltip bei Tap: zeigt Datum + Wert über dem getroffenen Punkt
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
    /** Frank-Wunsch 2026-05-09: bei Metriken wo niedriger besser ist (RHR, Schlafdefizit
     *  etc.) faerbt die Trendlinie semantisch — fallend = Verbesserung = gruen.
     *  Bei normalen Metriken (HRV, Schlafdauer) bleibt steigend = besser = gruen. */
    lowerIsBetter: Boolean = false,
    /** Frank-Wunsch 2026-05-09: bei Schlafdauer sollen die Y-Achsen-Beschriftungen und
     *  der Tooltip in Stunden statt Minuten formatiert werden. Wenn null wird der
     *  Standard-Formatter `formatY` verwendet und die Einheit angehaengt. */
    valueFormatter: ((Double) -> String)? = null,
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
    val firstDate = Instant.ofEpochMilli(safe.first().first).atZone(ZoneId.systemDefault()).toLocalDate()
    val lastDate = Instant.ofEpochMilli(safe.last().first).atZone(ZoneId.systemDefault()).toLocalDate()

    // Frank-Wunsch 2026-05-09: detailliertere Y-Achse — 5 Werte statt 3
    // (Min, 25%, Mitte, 75%, Max) damit man Werte besser ablesen kann.
    val yLabels = listOf(
        maxY,
        minY + rangeY * 0.75,
        minY + rangeY * 0.5,
        minY + rangeY * 0.25,
        minY,
    )

    // Werte-Formatter: wenn extern gesetzt (z.B. fuer Schlafstunden) verwenden,
    // sonst Standard formatY + Einheit anhaengen.
    val format: (Double) -> String = valueFormatter ?: { v ->
        formatY(v) + if (unit.isNotBlank()) " $unit" else ""
    }

    // Frank-Wunsch 2026-05-09: 14-Tage-SMA + lineare Regression als Trendlinie.
    // SMA glaettet Tagesausreisser, Slope der Regression entscheidet die Farbe:
    // semantisch "Verbesserung = grün" — bei lowerIsBetter wird der Slope invertiert.
    val sma = computeSma(safe.map { it.second }, window = 14)
    val smaSlope = if (sma.size >= 2) linearSlope(sma) else 0.0
    val semanticSlope = if (lowerIsBetter) -smaSlope else smaSlope
    // Frank-Wunsch 2026-05-09 Update: Epsilon entfernt — jede fallende Linie soll
    // rot sein, jede steigende gruen, auch bei minimalen Slopes. Vorher hat ein
    // Epsilon=0.1% des Wertebereichs leichte Trends als 'neutral' eingestuft, was
    // viele real-fallende Linien schwarz/grau gefaerbt hat.
    val trendColor = when {
        semanticSlope > 0.0 -> CosmosColors.Success
        semanticSlope < 0.0 -> CosmosColors.Critical
        else -> cosmos.textSecondary
    }

    var selectedIndex by remember(safe.size) { mutableStateOf<Int?>(null) }

    Row(modifier = modifier.fillMaxWidth()) {
        // Y-Achse links — 5 Werte (Frank-Wunsch 2026-05-09: detaillierter)
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.height(height.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            yLabels.forEach { v ->
                Text(
                    text = format(v),
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
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
                // Horizontale Hilfslinien (5 Stueck — passt zu den 5 Y-Labels)
                listOf(0f, h * 0.25f, h * 0.5f, h * 0.75f, h).forEach { y ->
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
                // Trendlinie 1 (SMA-14) — kurzfristige Glaettung, leicht wellig.
                // Frank-Wunsch 2026-05-09 Update: SMA bleibt drin, aber etwas duenner
                // weil sie jetzt der Sekundaer-Indikator ist. Die gerade lineare
                // Regressionslinie (unten) ist der primaere Gesamttrend.
                if (sma.size >= 2 && safe.size >= 2) {
                    val stepXSma = w / (safe.size - 1).toFloat()
                    val firstSmaIdx = safe.size - sma.size
                    for (i in 0 until sma.size - 1) {
                        val x1 = (firstSmaIdx + i) * stepXSma
                        val x2 = (firstSmaIdx + i + 1) * stepXSma
                        val y1 = h - ((sma[i] - minY) / rangeY * h).toFloat()
                        val y2 = h - ((sma[i + 1] - minY) / rangeY * h).toFloat()
                        drawLine(
                            color = trendColor,
                            start = Offset(x1, y1),
                            end = Offset(x2, y2),
                            strokeWidth = 6f,
                        )
                    }
                }
                // Trendlinie 2 (Lineare Regression) — komplett gerade, ueber alle
                // Roh-Datenpunkte gefittet. Frank-Wunsch 2026-05-09: "wirklich eine
                // lineare gerade Linie, die zeigt steigen oder fallen die Werte
                // generell über den gesamten Blockverlauf". Dicker als die SMA-
                // Linie damit sie als Primaer-Trend dominant ist.
                if (safe.size >= 2) {
                    val rawSlope = linearSlope(safe.map { it.second })
                    val rawMean = safe.map { it.second }.average()
                    val xMean = (safe.size - 1) / 2.0
                    val intercept = rawMean - rawSlope * xMean
                    val rawSemanticSlope = if (lowerIsBetter) -rawSlope else rawSlope
                    val rawTrendColor = when {
                        rawSemanticSlope > 0.0 -> CosmosColors.Success
                        rawSemanticSlope < 0.0 -> CosmosColors.Critical
                        else -> cosmos.textSecondary
                    }
                    val stepXLin = w / (safe.size - 1).toFloat()
                    val yStart = h - ((intercept - minY) / rangeY * h).toFloat()
                    val yEnd = h - ((intercept + rawSlope * (safe.size - 1) - minY) / rangeY * h).toFloat()
                    drawLine(
                        color = rawTrendColor,
                        start = Offset(0f, yStart),
                        end = Offset((safe.size - 1) * stepXLin, yEnd),
                        strokeWidth = 10f,
                    )
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
                        text = "${date.format(SHORT_DATE)}: ${format(value)}",
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

/**
 * Einfacher gleitender Durchschnitt (SMA) ueber ein festes Fenster.
 * Liefert eine Liste der Laenge `values.size - window + 1` — bei zu wenig
 * Datenpunkten wird eine leere Liste zurueckgegeben.
 *
 * Frank-Wunsch 2026-05-09: 14-Tage-Glaettung als Trendlinien-Basis.
 */
private fun computeSma(values: List<Double>, window: Int): List<Double> {
    if (values.size < window) return emptyList()
    val out = ArrayList<Double>(values.size - window + 1)
    var sum = 0.0
    for (i in 0 until window) sum += values[i]
    out += sum / window
    for (i in window until values.size) {
        sum += values[i] - values[i - window]
        out += sum / window
    }
    return out
}

/**
 * Steigung (Slope) einer linearen Regression nach kleinste-Quadrate-Verfahren.
 * x-Werte sind Indizes (0, 1, 2, ...), y-Werte sind die uebergebenen Werte.
 * Vorzeichen entscheidet ueber Trend: positiv = steigend, negativ = fallend.
 */
private fun linearSlope(values: List<Double>): Double {
    val n = values.size
    if (n < 2) return 0.0
    var sumX = 0.0
    var sumY = 0.0
    var sumXY = 0.0
    var sumXX = 0.0
    for (i in 0 until n) {
        val x = i.toDouble()
        val y = values[i]
        sumX += x
        sumY += y
        sumXY += x * y
        sumXX += x * x
    }
    val denominator = n * sumXX - sumX * sumX
    if (denominator == 0.0) return 0.0
    return (n * sumXY - sumX * sumY) / denominator
}
