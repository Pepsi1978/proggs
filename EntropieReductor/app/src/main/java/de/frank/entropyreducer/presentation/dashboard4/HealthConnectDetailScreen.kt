package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.ColorPaletteBar
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.charts.InteractiveLineChart
import de.frank.entropyreducer.presentation.components.rememberCardColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Body composition details from Zepp, including retained HC history.
 * Screen and route names remain stable for saved navigation state.
 *
 * Tap auf eine der Mini-Karten Gewicht / Koerperfett / Magermasse / Wasser / Knochen oeffnet diesen
 * Screen — analog zum Oura-Detail-Screen. Zeigt:
 * - Header mit aktuellem Wert + Datum + Plus/Minus zum 30-Tage-Mittel
 * - Range-Switcher 7T / 30T / 90T / Alle
 * - Linien-Verlauf der gefilterten Werte (InteractiveLineChart)
 * - Vollstaendige Liste aller Werte mit Datum + Plus/Minus zum 30-Tage-Mittel
 * - Refresh-Button im Header (zieht aktuelle Daten direkt aus Zepp)
 *
 * Frank-Vorgabe: bei Gewicht/Koerperfett ist niedriger besser (Diaet-Phase), bei
 * Magermasse/Wasser/Knochen ist hoeher besser (Muskelaufbau, Hydration).
 */
object HealthConnectMetricKey {
    const val WEIGHT = "hc_weight"
    const val BODY_FAT = "hc_body_fat"
    const val LEAN_BODY_MASS = "hc_lean_body_mass"
    const val BODY_WATER = "hc_body_water"
    const val BONE_MASS = "hc_bone_mass"
    const val MUSCLE_MASS = "hc_muscle_mass"
}

private data class HcMetricSpec(
    val title: String,
    val unit: String,
    val accent: androidx.compose.ui.graphics.Color,
    val lowerIsBetter: Boolean,
)

@Composable
private fun specFor(metric: BodyMetric?): HcMetricSpec {
    val cosmos = LocalCosmos.current
    val accent = when (metric) {
        BodyMetric.BODY_FAT, BodyMetric.VISCERAL_FAT -> cosmos.warn
        BodyMetric.LEAN, BodyMetric.MUSCLE, BodyMetric.SKELETAL_MUSCLE -> cosmos.ok
        BodyMetric.WATER, BodyMetric.WATER_PERCENT -> cosmos.accentForscher
        else -> cosmos.accent
    }
    return HcMetricSpec(
        metric?.title ?: "Körperdaten",
        metric?.unit.orEmpty(),
        accent,
        metric?.lowerIsBetter ?: false,
    )
}

@Composable
fun HealthConnectDetailScreen(
    metricKey: String,
    onBack: () -> Unit,
    vm: BiomarkerViewModel = hiltViewModel(),
) {
    val weight by vm.weight.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val metric = remember(metricKey) { BodyMetric.entries.firstOrNull { it.routeKey == metricKey } }
    val spec = specFor(metric)
    val history = metric?.let { weight.history(it) }.orEmpty()
    val latest = metric?.let { weight.latest(it) }
    val avg30 = metric?.let { weight.average(it) }

    // All cached measurements remain available, independently of HC permissions.
    var range by remember { mutableStateOf(HcDetailRange.ALL) }
    val cutoffDays =
        when (range) {
            HcDetailRange.SEVEN -> 7
            HcDetailRange.THIRTY -> 30
            HcDetailRange.NINETY -> 90
            HcDetailRange.ALL -> Int.MAX_VALUE
        }
    val cutoffMs =
        if (cutoffDays == Int.MAX_VALUE) 0L
        else System.currentTimeMillis() - cutoffDays.toLong() * 24L * 60L * 60L * 1000L
    // Performance-Audit Loop 2 (2026-05-10): 6 Listenoperationen pro Recomposition
    // ueber bis zu 200 Datenpunkten — jetzt einmalig in remember(history, cutoffMs).
    val stats =
        remember(history, cutoffMs) {
            val filteredList = history.filter { it.first >= cutoffMs }
            val valuesList = filteredList.map { it.second }
            DetailStats(
                filtered = filteredList,
                values = valuesList,
                minV = valuesList.minOrNull(),
                maxV = valuesList.maxOrNull(),
                avgV = valuesList.takeIf { it.isNotEmpty() }?.average(),
                latestTs = history.maxByOrNull { it.first }?.first,
            )
        }
    val filtered = stats.filtered
    val values = stats.values
    val minV = stats.minV
    val maxV = stats.maxV
    val avgV = stats.avgV
    val latestTs = stats.latestTs
    // Performance-Audit Loop 2 (2026-05-10): sortedByDescending in
    // remember(filtered) statt pro Recomposition. key = it.first im items{}.
    val sortedItems = remember(filtered) { filtered.sortedByDescending { it.first } }

    CosmosScaffold(
        title = spec.title,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
        actions = {
            IconButton(onClick = vm::refreshWeight, enabled = !weight.isLoading) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Körperwerte direkt aus Zepp aktualisieren",
                    tint = cosmos.textPrimary,
                )
            }
        },
    ) { padding ->
        // Frank-Wunsch 2026-05-18 Folgeauftrag: Farbpalette oben in jedem
        // Gewichts-Detail-Screen. Mapping HC-MetricKey -> CardId fuer die
        // jeweilige Mini-Karte im Uebersichts-Screen.
        val cardColorAccess = rememberCardColors()
        val targetCardId = metric?.cardId ?: metricKey

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ColorPaletteBar(
                    selectedIndex = cardColorAccess.colorFor(targetCardId, cosmos.isDark),
                    onPick = { idx -> cardColorAccess.setColor(targetCardId, idx, cosmos.isDark) },
                )
            }
            item {
                HcDetailHeader(
                    title = spec.title,
                    latest = latest,
                    latestTs = latestTs,
                    avg30 = avg30,
                    unit = spec.unit,
                    lowerIsBetter = spec.lowerIsBetter,
                )
            }
            item("zepp_source") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Quelle: Zepp direkt", style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary)
                    Text(
                        "Zepp liefert die letzte Messung mit Datum, ohne Messuhrzeit. " +
                            "Der Verlauf wird ab jetzt gespeichert; frühere HC-Werte bleiben erhalten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                    )
                    if (metric == BodyMetric.BODY_FAT) {
                        Text(
                            "Neue Körperfettwerte sind Näherungswerte aus Gewicht und Magermasse, " +
                                "auf eine Nachkommastelle gerundet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textSecondary,
                        )
                    }
                    val statusText = when {
                        weight.isLoading -> "Körperwerte werden direkt aus Zepp aktualisiert …"
                        weight.error != null -> weight.error
                        !weight.zeppAvailable -> "Zepp ist nicht installiert. Gespeicherte Werte bleiben sichtbar."
                        else -> null
                    }
                    if (statusText != null) {
                        Text(statusText, style = MaterialTheme.typography.bodySmall,
                            color = if (weight.error != null) cosmos.warn else cosmos.textSecondary)
                    }
                }
            }
            item { HcRangeSwitcher(current = range, onChange = { range = it }) }
            if (filtered.isNotEmpty()) {
                item {
                    HcStatsRow(
                        min = minV,
                        max = maxV,
                        avg = avgV,
                        count = values.size,
                        unit = spec.unit,
                    )
                }
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Verlauf",
                                style = MaterialTheme.typography.titleSmall,
                                color = cosmos.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(8.dp))
                            InteractiveLineChart(
                                points = filtered,
                                accent = spec.accent,
                                unit = spec.unit,
                                lowerIsBetter = spec.lowerIsBetter,
                                onClick = {},
                            )
                        }
                    }
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
                // Crash-Fix 2026-08-05 (Almanach jetpack-compose §4.2, gleiche Fehlerklasse wie
                // die Sport-Chips): der Timestamp allein ist NICHT eindeutig — zwei Quellen
                // koennen denselben Messzeitpunkt liefern. Index im Key macht ihn garantiert
                // eindeutig, ohne einen Messwert zu verstecken.
                itemsIndexed(sortedItems, key = { index, it -> "${index}_${it.first}" }) { _,
                    (ts, value) ->
                    HcValueRow(
                        timestampMs = ts,
                        value = value,
                        unit = spec.unit,
                        average = avgV,
                        lowerIsBetter = spec.lowerIsBetter,
                    )
                }
            } else {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = "Keine Werte im Bereich verfügbar",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text =
                                    "Zepp öffnen und erneut aktualisieren oder den Zeitraum wechseln. " +
                                        "Nicht gelieferte Körperwerte bleiben leer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HcDetailHeader(
    title: String,
    latest: Double?,
    latestTs: Long?,
    avg30: Double?,
    unit: String,
    lowerIsBetter: Boolean,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = cosmos.textSecondary)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = latest?.let { "%.1f".format(it) } ?: "—",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = cosmos.textPrimary,
                )
                if (unit.isNotBlank() && latest != null) {
                    Spacer(Modifier.padding(start = 6.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            if (latestTs != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "vom ${formatHcDate(latestTs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cosmos.textSecondary,
                )
            }
            if (latest != null && avg30 != null) {
                val diff = latest - avg30
                val isImprovement = if (lowerIsBetter) diff < 0 else diff > 0
                val color =
                    when {
                        diff == 0.0 -> cosmos.textSecondary
                        isImprovement -> LocalCosmos.current.ok
                        else -> LocalCosmos.current.crit
                    }
                val sign = if (diff >= 0) "+" else "−"
                val unitSuffix = if (unit.isNotBlank()) " $unit" else ""
                Spacer(Modifier.height(6.dp))
                Text(
                    text =
                        "$sign${"%.1f".format(kotlin.math.abs(diff))}$unitSuffix vs. 30-Tage-Mittel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private enum class HcDetailRange {
    SEVEN,
    THIRTY,
    NINETY,
    ALL,
}

// Performance-Audit Loop 8 (2026-05-10): Top-level Liste statt .values()-Array.
private val ALL_HC_DETAIL_RANGES: List<HcDetailRange> = HcDetailRange.entries.toList()

@Composable
private fun HcRangeSwitcher(current: HcDetailRange, onChange: (HcDetailRange) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ALL_HC_DETAIL_RANGES.forEach { range ->
            val label =
                when (range) {
                    HcDetailRange.SEVEN -> "7T"
                    HcDetailRange.THIRTY -> "30T"
                    HcDetailRange.NINETY -> "90T"
                    HcDetailRange.ALL -> "Alle"
                }
            FilterChip(
                selected = range == current,
                onClick = { onChange(range) },
                label = { Text(label) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LocalCosmos.current.accent.copy(alpha = 0.20f),
                        selectedLabelColor = LocalCosmos.current.accent,
                    ),
            )
        }
    }
}

@Composable
private fun HcStatsRow(min: Double?, max: Double?, avg: Double?, count: Int, unit: String) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            HcStatCell(
                label = "Min",
                value = min,
                unit = unit,
                color = cosmos.textPrimary,
                modifier = Modifier.weight(1f),
            )
            HcStatCell(
                label = "Mittel",
                value = avg,
                unit = unit,
                color = cosmos.textPrimary,
                modifier = Modifier.weight(1f),
            )
            HcStatCell(
                label = "Max",
                value = max,
                unit = unit,
                color = cosmos.textPrimary,
                modifier = Modifier.weight(1f),
            )
            HcStatCell(
                label = "Anzahl",
                value = count.toDouble(),
                unit = "",
                color = cosmos.textPrimary,
                modifier = Modifier.weight(1f),
                formatter = { "${it.toInt()}" },
            )
        }
    }
}

@Composable
private fun HcStatCell(
    label: String,
    value: Double?,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    formatter: (Double) -> String = { "%.1f".format(it) },
) {
    val cosmos = LocalCosmos.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = cosmos.textSecondary)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value?.let { formatter(it) + (if (unit.isNotBlank()) " $unit" else "") } ?: "—",
            style = MaterialTheme.typography.titleSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HcValueRow(
    timestampMs: Long,
    value: Double,
    unit: String,
    average: Double?,
    lowerIsBetter: Boolean,
) {
    val cosmos = LocalCosmos.current
    val deltaText: String? = average?.let { avg ->
        val diff = value - avg
        if (kotlin.math.abs(diff) < 0.05) null
        else {
            val sign = if (diff >= 0) "+" else "−"
            "$sign${"%.1f".format(kotlin.math.abs(diff))}"
        }
    }
    val deltaColor =
        if (deltaText != null && average != null) {
            val diff = value - average
            val isImprovement = if (lowerIsBetter) diff < 0 else diff > 0
            if (isImprovement) LocalCosmos.current.ok else LocalCosmos.current.crit
        } else cosmos.textSecondary
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatHcDate(timestampMs),
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "%.1f".format(value) + (if (unit.isNotBlank()) " $unit" else ""),
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            if (deltaText != null) {
                Spacer(Modifier.padding(start = 8.dp))
                Text(
                    text = deltaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = deltaColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * Vorberechnete Filter-Statistik fuer den HealthConnect-Detail-Screen. Performance-Audit Loop 2
 * (2026-05-10): bundelt 6 Listenoperationen die sonst pro Recomposition ueber bis zu 200
 * Datenpunkten liefen.
 */
private data class DetailStats(
    val filtered: List<Pair<Long, Double>>,
    val values: List<Double>,
    val minV: Double?,
    val maxV: Double?,
    val avgV: Double?,
    val latestTs: Long?,
)

// Performance-Audit Loop 2 (2026-05-10): SimpleDateFormat ist nicht thread-safe,
// aber teuer zu allokieren (Pattern-Kompilierung + Locale-Lookup). ThreadLocal
// gibt jedem Thread eine eigene Instanz — keine Allokation pro Listenzeile mehr.
private val HC_DATE_FMT: ThreadLocal<SimpleDateFormat> = ThreadLocal.withInitial {
    SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
}

private fun formatHcDate(ms: Long): String = HC_DATE_FMT.get()!!.format(Date(ms))
