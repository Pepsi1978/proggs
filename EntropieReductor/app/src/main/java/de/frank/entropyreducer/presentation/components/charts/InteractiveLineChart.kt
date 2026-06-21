package de.frank.entropyreducer.presentation.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Interaktiver Linien-Chart mit Y-Achse, X-Achse und Tap-zu-Tooltip.
 *
 * Frank-Wunsch 2026-05-08: "Y-Skala muss da sein, Tap auf Punkt zeigt den Wert, Datum sichtbar".
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
    /**
     * Frank-Wunsch 2026-05-09: bei Metriken wo niedriger besser ist (RHR, Schlafdefizit etc.)
     * faerbt die Trendlinie semantisch — fallend = Verbesserung = gruen. Bei normalen Metriken
     * (HRV, Schlafdauer) bleibt steigend = besser = gruen.
     */
    lowerIsBetter: Boolean = false,
    /**
     * Frank-Wunsch 2026-05-09: bei Schlafdauer sollen die Y-Achsen-Beschriftungen und der Tooltip
     * in Stunden statt Minuten formatiert werden. Wenn null wird der Standard-Formatter `formatY`
     * verwendet und die Einheit angehaengt.
     */
    valueFormatter: ((Double) -> String)? = null,
    /**
     * Frank-Wunsch 2026-05-09 (Abend): Tap auf den Chart soll die Detail-Seite oeffnen — nicht mehr
     * nur einen Tooltip auf dem Punkt zeigen. Wenn gesetzt, ruft jede Tap-Geste sofort onClick()
     * auf und der Tooltip-Modus ist deaktiviert. So wird der ganze Graph durchklickbar, statt dass
     * die innere pointerInput die Klicks der umgebenden Card frisst.
     */
    onClick: (() -> Unit)? = null,
) {
    val cosmos = LocalCosmos.current
    // Performance-Audit Loop 1 (2026-05-10): alle CPU-/Allokations-intensiven
    // Berechnungen ueber `points` in remember(points) vorberechnen, damit sie
    // nicht bei jeder Recomposition (z.B. durch animateXxxAsState) neu laufen.
    val derived =
        remember(points) {
            val safeList = points.filter { it.second.isFinite() }.sortedBy { it.first }
            if (safeList.isEmpty()) ChartDerived.EMPTY
            else {
                val minVal = safeList.minOf { it.second }
                val maxVal = safeList.maxOf { it.second }
                val range = (maxVal - minVal).coerceAtLeast(1.0)
                val rawValues = safeList.map { it.second }
                val smaList = computeSma(rawValues, window = 14)
                val smaSlopeVal = if (smaList.size >= 2) linearSlope(smaList) else 0.0
                // Lineare Regression einmalig vorberechnen — frueher im Canvas-Lambda
                val rawSlopeVal = if (safeList.size >= 2) linearSlope(rawValues) else 0.0
                val rawMeanVal = if (rawValues.isNotEmpty()) rawValues.average() else 0.0
                val xMeanVal = (safeList.size - 1) / 2.0
                val interceptVal = rawMeanVal - rawSlopeVal * xMeanVal
                ChartDerived(
                    safe = safeList,
                    minY = minVal,
                    maxY = maxVal,
                    rangeY = range,
                    sma = smaList,
                    smaSlope = smaSlopeVal,
                    rawSlope = rawSlopeVal,
                    intercept = interceptVal,
                )
            }
        }
    val safe = derived.safe
    if (safe.isEmpty()) {
        Text(
            text = "Keine Daten — sync dein Whoop-Armband.",
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        return
    }
    val minY = derived.minY
    val maxY = derived.maxY
    val rangeY = derived.rangeY
    val firstDate =
        remember(safe) {
            Instant.ofEpochMilli(safe.first().first).atZone(ZoneId.systemDefault()).toLocalDate()
        }
    val lastDate =
        remember(safe) {
            Instant.ofEpochMilli(safe.last().first).atZone(ZoneId.systemDefault()).toLocalDate()
        }

    // 9 Y-Achsen-Werte (Frank-Wunsch 2026-05-11: feiner als die alten 5 Ticks —
    // Sprung von 6h39 auf 9h03 war zu gross, Zwischenwerte fehlten). Reihenfolge
    // ist oben→unten: max, 87.5%, 75%, ..., 12.5%, min.
    val yLabels =
        remember(minY, maxY, rangeY) {
            listOf(
                maxY,
                minY + rangeY * 0.875,
                minY + rangeY * 0.75,
                minY + rangeY * 0.625,
                minY + rangeY * 0.5,
                minY + rangeY * 0.375,
                minY + rangeY * 0.25,
                minY + rangeY * 0.125,
                minY,
            )
        }

    // Werte-Formatter: wenn extern gesetzt (z.B. fuer Schlafstunden) verwenden,
    // sonst Standard formatY + Einheit anhaengen.
    val format: (Double) -> String =
        valueFormatter ?: { v -> formatY(v) + if (unit.isNotBlank()) " $unit" else "" }

    val sma = derived.sma
    val smaSlope = derived.smaSlope
    val semanticSlope = if (lowerIsBetter) -smaSlope else smaSlope
    val trendColor =
        when {
            semanticSlope > 0.0 -> LocalCosmos.current.ok
            semanticSlope < 0.0 -> LocalCosmos.current.crit
            else -> cosmos.textSecondary
        }
    // Lineare Regression: Slope wird im Composable-Scope verwendet,
    // Berechnung jetzt in derived.
    val rawSemanticSlope = if (lowerIsBetter) -derived.rawSlope else derived.rawSlope
    val rawTrendColor =
        when {
            rawSemanticSlope > 0.0 -> LocalCosmos.current.ok
            rawSemanticSlope < 0.0 -> LocalCosmos.current.crit
            else -> cosmos.textSecondary
        }
    // PathEffect einmalig allokieren — vorher 5x pro Frame im Canvas-Lambda.
    val gridDashEffect = remember { PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) }

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
        Box(modifier = Modifier.height(height.dp).fillMaxWidth()) {
            Canvas(
                modifier =
                    Modifier.fillMaxWidth().height(height.dp).pointerInput(safe, onClick) {
                        detectTapGestures { tap ->
                            if (onClick != null) {
                                // Frank-Wunsch 2026-05-09 (Abend): Tap auf irgendeine
                                // Stelle des Graphs oeffnet sofort die Detail-Seite.
                                onClick()
                            } else {
                                // Fallback: alter Tooltip-Modus — Index mit kleinster
                                // X-Distanz selektieren und Tooltip einblenden.
                                val w = size.width.toFloat()
                                val stepX = if (safe.size <= 1) w else w / (safe.size - 1).toFloat()
                                val idx = (tap.x / stepX).toInt().coerceIn(0, safe.size - 1)
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val gridColor = cosmos.glassBorder
                // Horizontale Hilfslinien (9 Stueck — passt zu den 9 Y-Labels).
                // Frank-Wunsch 2026-05-11: feinere Unterteilung damit grosse Spannen
                // (z.B. 5h vs. 9h) Zwischenmarken haben.
                val gridLineColor = gridColor.copy(alpha = 0.3f)
                for (n in 0..8) {
                    val yTick = h * (n / 8f)
                    drawLine(
                        gridLineColor,
                        Offset(0f, yTick),
                        Offset(w, yTick),
                        1f,
                        pathEffect = gridDashEffect,
                    )
                }
                // Datenlinie + Punkte
                // Performance-Audit 2026-06-21: Datenlinie als einzelner Path
                // statt N einzelner drawLine-Aufrufe (bis zu 70 Segmente).
                // Bei 11 MetricHistoryCards im Biomarker-Tab reduziert das die
                // Draw-Aufrufe von ~770 auf ~11 pro Frame.
                if (safe.size >= 2) {
                    val stepX = w / (safe.size - 1).toFloat()
                    val dataPath = Path()
                    for (i in safe.indices) {
                        val x = i * stepX
                        val y = h - ((safe[i].second - minY) / rangeY * h).toFloat()
                        if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                    }
                    drawPath(path = dataPath, color = accent, style = Stroke(width = 4f))
                }
                // Trendlinie 1 (SMA-14) — kurzfristige Glaettung, leicht wellig.
                // Frank-Wunsch 2026-05-09 Update: SMA bleibt drin, aber etwas duenner
                // weil sie jetzt der Sekundaer-Indikator ist. Die gerade lineare
                // Regressionslinie (unten) ist der primaere Gesamttrend.
                // Performance-Audit 2026-06-21: SMA als Path statt N drawLine.
                if (sma.size >= 2 && safe.size >= 2) {
                    val stepXSma = w / (safe.size - 1).toFloat()
                    val firstSmaIdx = safe.size - sma.size
                    val smaPath = Path()
                    for (i in sma.indices) {
                        val x = (firstSmaIdx + i) * stepXSma
                        val y = h - ((sma[i] - minY) / rangeY * h).toFloat()
                        if (i == 0) smaPath.moveTo(x, y) else smaPath.lineTo(x, y)
                    }
                    drawPath(path = smaPath, color = trendColor, style = Stroke(width = 6f))
                }
                // Trendlinie 2 (Lineare Regression) — komplett gerade, ueber alle
                // Roh-Datenpunkte gefittet. Frank-Wunsch 2026-05-09: "wirklich eine
                // lineare gerade Linie, die zeigt steigen oder fallen die Werte
                // generell über den gesamten Blockverlauf". Dicker als die SMA-
                // Linie damit sie als Primaer-Trend dominant ist.
                if (safe.size >= 2) {
                    // Performance-Fix Loop 1 (2026-05-10): rawSlope, rawMean, xMean,
                    // intercept werden in remember(points) vorberechnet (siehe oben).
                    // Hier nur noch die Pixel-Koordinaten ableiten.
                    val intercept = derived.intercept
                    val rawSlope = derived.rawSlope
                    val stepXLin = w / (safe.size - 1).toFloat()
                    val yStart = h - ((intercept - minY) / rangeY * h).toFloat()
                    val yEnd =
                        h - ((intercept + rawSlope * (safe.size - 1) - minY) / rangeY * h).toFloat()
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
                    modifier =
                        Modifier.align(Alignment.TopStart)
                            .padding(4.dp)
                            .background(
                                color =
                                    if (cosmos.isDark) CosmosColors.BgDarkAccent
                                    else CosmosColors.BgLightAccent,
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
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

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM", Locale.GERMANY)

/**
 * Einfacher gleitender Durchschnitt (SMA) ueber ein festes Fenster. Liefert eine Liste der Laenge
 * `values.size - window + 1` — bei zu wenig Datenpunkten wird eine leere Liste zurueckgegeben.
 *
 * Frank-Wunsch 2026-05-09: 14-Tage-Glaettung als Trendlinien-Basis.
 */
/**
 * Vorberechnete Chart-Daten — Performance-Audit Loop 1 (2026-05-10). Wird einmalig in
 * remember(points) berechnet statt bei jeder Recomposition neu. Sa fe ist die gefilterte+sortierte
 * Punktliste, sma die 14-Tage- Glaettung, intercept+slope die lineare Regression.
 */
private data class ChartDerived(
    val safe: List<Pair<Long, Double>>,
    val minY: Double,
    val maxY: Double,
    val rangeY: Double,
    val sma: List<Double>,
    val smaSlope: Double,
    val rawSlope: Double,
    val intercept: Double,
) {
    companion object {
        val EMPTY =
            ChartDerived(
                safe = emptyList(),
                minY = 0.0,
                maxY = 0.0,
                rangeY = 1.0,
                sma = emptyList(),
                smaSlope = 0.0,
                rawSlope = 0.0,
                intercept = 0.0,
            )
    }
}

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
 * Steigung (Slope) einer linearen Regression nach kleinste-Quadrate-Verfahren. x-Werte sind Indizes
 * (0, 1, 2, ...), y-Werte sind die uebergebenen Werte. Vorzeichen entscheidet ueber Trend: positiv
 * = steigend, negativ = fallend.
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
