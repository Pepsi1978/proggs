package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.ColorPaletteBar
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.rememberCardColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.CosmosColors

/**
 * Oura-Ring-Detail-Screen pro Metrik (Frank-Wunsch 2026-05-10, Etappe F).
 *
 * Zeigt fuer einen der vier Oura-Werte (Readiness, Schlaf-Score, Aktivität, Resilienz) die
 * komplette Historie als interaktiver Verlauf:
 * - Header mit aktuellem Wert + Plus/Minus zum 30-Tage-Mittel
 * - Range-Switcher 7T / 30T / 90T / Alle
 * - Grosser Verlaufs-Balkenchart der gefilterten Werte
 * - Vollstaendige Liste aller Werte mit Datum + Plus/Minus zum Vortag
 *
 * Frank-Vorgabe: 30-Tage-Schnitt ist die Vergleichsbasis fuer das Plus/Minus.
 */
object OuraMetricKey {
    const val READINESS = "oura_readiness"
    const val SLEEP_SCORE = "oura_sleep_score"
    const val ACTIVITY = "oura_activity"
    const val RESILIENCE = "oura_resilience"
}

@Composable
fun OuraDetailScreen(
    metricKey: String,
    onBack: () -> Unit,
    vm: BiomarkerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current

    val title =
        when (metricKey) {
            OuraMetricKey.READINESS -> "Readiness"
            OuraMetricKey.SLEEP_SCORE -> "Schlaf-Score"
            OuraMetricKey.ACTIVITY -> "Aktivität"
            OuraMetricKey.RESILIENCE -> "Resilienz"
            else -> "Oura"
        }

    // Historie auf einheitliches Format (day, score) abbilden — Resilience nutzt
    // den Rang 0..4, Score-Karten den 0..100-Score.
    val allPoints: List<Pair<String, Double>> =
        when (metricKey) {
            OuraMetricKey.READINESS ->
                state.ouraReadinessHistory.mapNotNull {
                    it.score?.let { s -> it.day to s.toDouble() }
                }
            OuraMetricKey.SLEEP_SCORE ->
                state.ouraSleepHistory.mapNotNull { it.score?.let { s -> it.day to s.toDouble() } }
            OuraMetricKey.ACTIVITY ->
                state.ouraActivityHistory.mapNotNull {
                    it.score?.let { s -> it.day to s.toDouble() }
                }
            OuraMetricKey.RESILIENCE ->
                state.ouraResilienceHistory.mapNotNull {
                    levelToRank(it.level)?.let { r -> it.day to r }
                }
            else -> emptyList()
        }
    val maxValue = if (metricKey == OuraMetricKey.RESILIENCE) 4.0 else 100.0
    val unit = if (metricKey == OuraMetricKey.RESILIENCE) "" else " /100"

    var range by remember { mutableStateOf(DetailRangeOura.THIRTY) }
    val cutoffDays =
        when (range) {
            DetailRangeOura.SEVEN -> 7
            DetailRangeOura.THIRTY -> 30
            DetailRangeOura.NINETY -> 90
            DetailRangeOura.ALL -> Int.MAX_VALUE
        }
    val filtered = allPoints.takeLast(cutoffDays)
    val values = filtered.map { it.second }
    val minV = values.minOrNull()
    val maxV = values.maxOrNull()
    val avgV = values.takeIf { it.isNotEmpty() }?.average()
    val current = allPoints.lastOrNull()?.second

    // 30-Tage-Mittel als Vergleichsbasis fuer den Trend.
    val last30 = allPoints.takeLast(30).map { it.second }
    val avg30 = last30.takeIf { it.isNotEmpty() }?.average()
    val trendDelta = if (current != null && avg30 != null) current - avg30 else null

    // Frank-Wunsch 2026-05-18 Folgeauftrag: Farbpalette oben auf jedem
    // Oura-Detail-Screen (Readiness, Schlaf-Score, Aktivitaet, Resilienz).
    val cardColorAccess = rememberCardColors()
    val ouraCardId = metricKey
    val ouraColorIndex = cardColorAccess.colorFor(ouraCardId, cosmos.isDark)

    CosmosScaffold(
        title = title,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ColorPaletteBar(
                    selectedIndex = ouraColorIndex,
                    onPick = { idx -> cardColorAccess.setColor(ouraCardId, idx, cosmos.isDark) },
                )
            }
            item {
                OuraDetailHeader(
                    title = title,
                    current = current,
                    unit = unit,
                    trendDelta = trendDelta,
                    isResilience = metricKey == OuraMetricKey.RESILIENCE,
                )
            }
            item { OuraRangeSwitcher(current = range, onChange = { range = it }) }
            if (filtered.isNotEmpty()) {
                item {
                    OuraStatsRow(
                        min = minV,
                        max = maxV,
                        avg = avgV,
                        count = values.size,
                        isResilience = metricKey == OuraMetricKey.RESILIENCE,
                    )
                }
                item {
                    OuraVerlaufsChart(
                        points = filtered,
                        colorFor = colorForBar(metricKey),
                        maxValue = maxValue,
                        yLabels = yAxisLabelsFor(metricKey, maxValue),
                    )
                }
                item {
                    Text(
                        text = "Alle Werte",
                        style = MaterialTheme.typography.titleSmall,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                // Frank-Wunsch 2026-05-16: Liste 1:1 wie Recovery-Detail —
                // Wochentag + dd.MM.yyyy + Wert + Delta zum VORTAG. Reversed
                // damit der juengste Tag oben steht; Vortag ist dann der naechste
                // Index (idx+1) in dieser absteigenden Reihenfolge.
                val sortedDescending = filtered.reversed()
                itemsIndexed(sortedDescending, key = { _, (day, _) -> day }) { idx, (day, value) ->
                    val previous = sortedDescending.getOrNull(idx + 1)?.second
                    OuraValueRow(
                        day = day,
                        value = value,
                        isResilience = metricKey == OuraMetricKey.RESILIENCE,
                        previousValue = previous,
                        accent = pickAccent(metricKey, value),
                    )
                }
            } else {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Noch keine Daten in diesem Zeitraum.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cosmos.textSecondary,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun OuraDetailHeader(
    title: String,
    current: Double?,
    unit: String,
    trendDelta: Double?,
    isResilience: Boolean,
) {
    val cosmos = LocalCosmos.current
    val accent =
        if (isResilience) {
            levelToColorFromRank(current, cosmos.textSecondary)
        } else {
            scoreColor(current?.toInt())
        }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cosmos.textPrimary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text =
                        if (current == null) "—"
                        else if (isResilience) levelToGerman(rankToLevel(current))
                        else current.toInt().toString(),
                    color = accent,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (current != null && !isResilience) {
                    Text(
                        unit,
                        color = accent,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
                    )
                }
            }
            if (trendDelta != null) {
                Spacer(Modifier.height(8.dp))
                val trendColor =
                    when {
                        trendDelta > 0.5 -> CosmosColors.Success
                        trendDelta < -0.5 -> CosmosColors.Critical
                        else -> CosmosColors.AccentPrimary
                    }
                val trendLabel =
                    when {
                        trendDelta > 0.5 -> "besser als 30-Tage-Mittel"
                        trendDelta < -0.5 -> "schlechter als 30-Tage-Mittel"
                        else -> "wie 30-Tage-Mittel"
                    }
                Text(
                    text = "${"%+.1f".format(trendDelta)} — $trendLabel",
                    color = trendColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun OuraRangeSwitcher(current: DetailRangeOura, onChange: (DetailRangeOura) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailRangeOura.entries.forEach { r ->
            FilterChip(
                selected = current == r,
                onClick = { onChange(r) },
                label = { Text(r.label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CosmosColors.Success.copy(alpha = 0.2f),
                        selectedLabelColor = CosmosColors.Success,
                    ),
            )
        }
    }
}

@Composable
private fun OuraStatsRow(
    min: Double?,
    max: Double?,
    avg: Double?,
    count: Int,
    isResilience: Boolean,
) {
    val cosmos = LocalCosmos.current
    val format: (Double) -> String =
        if (isResilience) {
            { levelToGerman(rankToLevel(it)) }
        } else {
            { it.toInt().toString() }
        }
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatCol("Mittel", avg?.let(format) ?: "—")
            StatCol("Min", min?.let(format) ?: "—")
            StatCol("Max", max?.let(format) ?: "—")
            StatCol("Werte", count.toString())
        }
    }
}

@Composable
private fun StatCol(label: String, value: String) {
    val cosmos = LocalCosmos.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * Verlaufs-Balkenchart fuer den Detail-Screen. Hoehe der Balken proportional zum Wert.
 * Frank-Vorgabe 2026-05-10: jeder Balken bekommt seine eigene Farbe abhaengig vom Wert (gleiche
 * 80/60-Schwellen wie auf der Karte). `colorFor` mappt einen Balkenwert auf seine Farbe. Damit
 * zeigt der ganze Verlauf auf einen Blick wo gute (gruen), mittlere (gelb) und schwache (rot) Tage
 * waren.
 */
@Composable
private fun OuraVerlaufsChart(
    points: List<Pair<String, Double>>,
    colorFor: (Double) -> Color,
    maxValue: Double,
    yLabels: List<String>,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                "Verlauf",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            // Chart-Bereich: Y-Achsen-Beschriftung links, Balken rechts.
            // Beide haben dieselbe feste Hoehe damit Achsen-Labels und Balken
            // bottom-aligned bleiben. Frank-Vorgabe 2026-05-10: Y-Achse damit
            // sichtbar ist welcher Wert ein Balken anzeigt.
            Row(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(end = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    yLabels.forEach { lbl ->
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                        )
                    }
                }
                // Box mit zwei Layern: Hilfslinien im Hintergrund, Balken davor.
                // Frank-Vorgabe 2026-05-10: Linien auf Hoehe 0/25/50/75/100 damit
                // erkennbar ist 'da verlaeuft 50, da 75'. Linien sind sehr dezent
                // (alpha 0.35) damit die farbigen Balken im Vordergrund bleiben.
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        repeat(5) {
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .height(1.dp)
                                        .background(cosmos.glassBorder.copy(alpha = 0.55f))
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        points.forEach { (_, v) ->
                            val frac = (v / maxValue).coerceIn(0.0, 1.0).toFloat()
                            Box(
                                modifier =
                                    Modifier.weight(1f)
                                        .height((140.dp * frac).coerceAtLeast(2.dp))
                                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                        .background(colorFor(v).copy(alpha = 0.85f))
                            )
                        }
                    }
                }
            }
            // Datums-Labels nur am Anfang und Ende damit's nicht ueberladen wirkt.
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val first = points.firstOrNull()?.first ?: ""
                val last = points.lastOrNull()?.first ?: ""
                Text(
                    shortDate(first),
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
                Text(
                    shortDate(last),
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun OuraValueRow(
    day: String,
    value: Double,
    isResilience: Boolean,
    previousValue: Double?,
    accent: Color,
) {
    val cosmos = LocalCosmos.current
    val display = if (isResilience) levelToGerman(rankToLevel(value)) else value.toInt().toString()
    // Frank-Wunsch 2026-05-16: Delta zum VORTAG, nicht zum Durchschnitt — 1:1
    // wie Recovery-Detail. Bei Score-Karten ist hoeher = besser, also Plus
    // = gruen, Minus = rot. Bei Resilienz wird die Differenz uebersprungen
    // weil Levels nicht sauber als Plus/Minus-Differenz abbildbar sind.
    val delta: Double? = if (!isResilience && previousValue != null) value - previousValue else null
    val deltaText: String =
        delta?.let {
            val sign = if (it >= 0) "+" else ""
            "$sign${it.toInt()}"
        } ?: ""
    val deltaColor: Color =
        when {
            delta == null -> cosmos.textSecondary
            delta > 0 -> CosmosColors.Success
            delta < 0 -> CosmosColors.Critical
            else -> cosmos.textSecondary
        }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(cosmos.glassBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Frank-Wunsch 2026-05-16: Layout 1:1 wie ValueRow in BiomarkerDetailScreen —
        // Datum (Wochentag + dd.MM.yyyy) links als Weight, Wert in Accent-Farbe
        // rechts, Delta zum Vortag in einer Box am rechten Rand.
        Text(
            text = formatRecoveryStyleDate(day),
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = display,
            style = MaterialTheme.typography.titleSmall,
            color = accent,
            fontWeight = FontWeight.SemiBold,
        )
        if (deltaText.isNotBlank()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(start = 8.dp).width(56.dp)
            ) {
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = deltaColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Frank-Wunsch 2026-05-16: gleiches Datums-Format wie Recovery-Detail — "Sa 16.05.2026" mit
 * Wochentag in deutscher Locale.
 */
private fun formatRecoveryStyleDate(day: String): String {
    if (day.length != 10) return day
    return runCatching {
            val date = java.time.LocalDate.parse(day)
            date.format(OURA_VALUE_DATE_FMT)
        }
        .getOrDefault(day)
}

private val OURA_VALUE_DATE_FMT: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", java.util.Locale.GERMANY)

private fun pickAccent(metricKey: String, current: Double?): Color =
    if (metricKey == OuraMetricKey.RESILIENCE) {
        levelToColorFromRank(current, CosmosColors.AccentPrimary)
    } else {
        scoreColor(current?.toInt())
    }

/**
 * Y-Achsen-Beschriftungen fuer den Verlaufs-Chart (Frank-Vorgabe 2026-05-10). Werden von oben nach
 * unten ausgegeben (hoechster Wert oben). Score-Karten nutzen 100/75/50/25/0, Resilienz nutzt
 * 4/3/2/1/0 (Rang-Skala).
 */
private fun yAxisLabelsFor(metricKey: String, maxValue: Double): List<String> =
    if (metricKey == OuraMetricKey.RESILIENCE) {
        listOf("4", "3", "2", "1", "0")
    } else {
        // 5 Stufen gleichmaessig verteilt zwischen 0 und maxValue (typisch 100).
        val step = maxValue / 4.0
        (4 downTo 0).map { i -> (i * step).toInt().toString() }
    }

/**
 * Farbgebung pro Balken im Verlaufs-Chart (Frank-Vorgabe 2026-05-10): jeder Balken bekommt seine
 * eigene Farbe abhaengig vom Wert.
 * - Score-Karten (Readiness, Schlaf-Score, Aktivität): scoreColor mit 80/60-Schwellen (>=80 gruen,
 *   60-79 gelb, <60 rot).
 * - Resilienz: Rang-Mapping (0=rot, 1=gelb, 2=blau, 3-4=gruen) wie auf der Karte selbst.
 */
private fun colorForBar(metricKey: String): (Double) -> Color =
    if (metricKey == OuraMetricKey.RESILIENCE) {
        { rank ->
            when (rank.toInt()) {
                0 -> CosmosColors.Critical
                1 -> CosmosColors.Warning
                2 -> CosmosColors.AccentPrimary
                3,
                4 -> CosmosColors.Success
                else -> CosmosColors.AccentPrimary
            }
        }
    } else {
        { v -> scoreColor(v.toInt()) }
    }

private fun levelToColorFromRank(rank: Double?, fallback: Color): Color =
    levelToColor(rankToLevel(rank), fallback)

private fun rankToLevel(rank: Double?): String? =
    when (rank?.toInt()) {
        0 -> "limited"
        1 -> "adequate"
        2 -> "solid"
        3 -> "strong"
        4 -> "exceptional"
        else -> null
    }

private fun shortDate(day: String): String =
    if (day.length == 10) "${day.substring(8, 10)}.${day.substring(5, 7)}" else day

private enum class DetailRangeOura(val label: String) {
    SEVEN("7T"),
    THIRTY("30T"),
    NINETY("90T"),
    ALL("Alle"),
}
