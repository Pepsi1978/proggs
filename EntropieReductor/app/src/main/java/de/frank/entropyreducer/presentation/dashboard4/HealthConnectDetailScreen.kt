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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
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
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.charts.InteractiveLineChart
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detail-Screen fuer Health-Connect-Body-Composition-Werte (Frank-Wunsch 2026-05-10).
 *
 * Tap auf eine der Mini-Karten Gewicht / Koerperfett / Magermasse / Wasser /
 * Knochen oeffnet diesen Screen — analog zum Oura-Detail-Screen. Zeigt:
 *  - Header mit aktuellem Wert + Datum + Plus/Minus zum 30-Tage-Mittel
 *  - Range-Switcher 7T / 30T / 90T / Alle
 *  - Linien-Verlauf der gefilterten Werte (InteractiveLineChart)
 *  - Vollstaendige Liste aller Werte mit Datum + Plus/Minus zum 30-Tage-Mittel
 *  - Refresh-Button im Header (zieht aktuelle Daten aus Health Connect)
 *
 * Frank-Vorgabe: bei Gewicht/Koerperfett ist niedriger besser (Diaet-Phase),
 * bei Magermasse/Wasser/Knochen ist hoeher besser (Muskelaufbau, Hydration).
 */
object HealthConnectMetricKey {
    const val WEIGHT = "hc_weight"
    const val BODY_FAT = "hc_body_fat"
    const val LEAN_BODY_MASS = "hc_lean_body_mass"
    const val BODY_WATER = "hc_body_water"
    const val BONE_MASS = "hc_bone_mass"
}

private data class HcMetricSpec(
    val title: String,
    val unit: String,
    val accent: androidx.compose.ui.graphics.Color,
    val lowerIsBetter: Boolean,
)

private fun specFor(metricKey: String): HcMetricSpec = when (metricKey) {
    HealthConnectMetricKey.WEIGHT -> HcMetricSpec(
        title = "Gewicht",
        unit = "kg",
        accent = CosmosColors.AccentPrimary,
        lowerIsBetter = true,
    )
    HealthConnectMetricKey.BODY_FAT -> HcMetricSpec(
        title = "Körperfett",
        unit = "%",
        accent = CosmosColors.Warning,
        lowerIsBetter = true,
    )
    HealthConnectMetricKey.LEAN_BODY_MASS -> HcMetricSpec(
        title = "Magermasse",
        unit = "kg",
        accent = CosmosColors.Success,
        lowerIsBetter = false,
    )
    HealthConnectMetricKey.BODY_WATER -> HcMetricSpec(
        title = "Körperwasser",
        unit = "kg",
        accent = CosmosColors.AccentSecondary,
        lowerIsBetter = false,
    )
    HealthConnectMetricKey.BONE_MASS -> HcMetricSpec(
        title = "Knochenmasse",
        unit = "kg",
        accent = CosmosColors.AccentPrimary,
        lowerIsBetter = false,
    )
    else -> HcMetricSpec("Health Connect", "", CosmosColors.AccentPrimary, false)
}

@Composable
fun HealthConnectDetailScreen(
    metricKey: String,
    onBack: () -> Unit,
    vm: BiomarkerViewModel = hiltViewModel(),
) {
    val weight by vm.weight.collectAsState()
    val cosmos = LocalCosmos.current
    val spec = specFor(metricKey)

    // Datenquelle pro Metrik. history30d ist bereits in WeightState verfuegbar —
    // 30 Tage reichen fuer den Default-Range. Fuer 90T / Alle laden wir on-demand
    // hier nicht nach (wuerde ein eigenes ViewModel-Feld brauchen) — Frank kriegt
    // die letzten 30 Tage als Default und kann das spaeter erweitern.
    val history: List<Pair<Long, Double>> = when (metricKey) {
        HealthConnectMetricKey.WEIGHT -> weight.history30d
        HealthConnectMetricKey.BODY_FAT -> weight.bodyFatHistory30d
        HealthConnectMetricKey.LEAN_BODY_MASS -> weight.leanBodyMassHistory30d
        HealthConnectMetricKey.BODY_WATER -> weight.bodyWaterMassHistory30d
        HealthConnectMetricKey.BONE_MASS -> weight.boneMassHistory30d
        else -> emptyList()
    }
    val latest: Double? = when (metricKey) {
        HealthConnectMetricKey.WEIGHT -> weight.latestKg
        HealthConnectMetricKey.BODY_FAT -> weight.latestBodyFatPercent
        HealthConnectMetricKey.LEAN_BODY_MASS -> weight.latestLeanBodyMassKg
        HealthConnectMetricKey.BODY_WATER -> weight.latestBodyWaterMassKg
        HealthConnectMetricKey.BONE_MASS -> weight.latestBoneMassKg
        else -> null
    }
    val avg30: Double? = when (metricKey) {
        HealthConnectMetricKey.WEIGHT -> weight.avg30dKg
        HealthConnectMetricKey.BODY_FAT -> weight.avg30dBodyFatPercent
        HealthConnectMetricKey.LEAN_BODY_MASS -> weight.avg30dLeanBodyMassKg
        HealthConnectMetricKey.BODY_WATER -> weight.avg30dBodyWaterMassKg
        HealthConnectMetricKey.BONE_MASS -> weight.avg30dBoneMassKg
        else -> null
    }

    var range by remember { mutableStateOf(HcDetailRange.THIRTY) }
    val cutoffDays = when (range) {
        HcDetailRange.SEVEN -> 7
        HcDetailRange.THIRTY -> 30
        HcDetailRange.NINETY -> 90
        HcDetailRange.ALL -> Int.MAX_VALUE
    }
    val cutoffMs = if (cutoffDays == Int.MAX_VALUE) 0L
        else System.currentTimeMillis() - cutoffDays.toLong() * 24L * 60L * 60L * 1000L
    val filtered = history.filter { it.first >= cutoffMs }
    val values = filtered.map { it.second }
    val minV = values.minOrNull()
    val maxV = values.maxOrNull()
    val avgV = values.takeIf { it.isNotEmpty() }?.average()
    val latestTs = history.maxByOrNull { it.first }?.first

    CosmosScaffold(
        title = spec.title,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
        actions = {
            IconButton(onClick = vm::refreshWeight) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Werte neu laden",
                    tint = cosmos.textPrimary,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                items(filtered.sortedByDescending { it.first }) { (ts, value) ->
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
                                text = "Tippe oben rechts auf das Refresh-Symbol oder wechsle den Zeitraum.",
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
                val color = when {
                    diff == 0.0 -> cosmos.textSecondary
                    isImprovement -> CosmosColors.Success
                    else -> CosmosColors.Critical
                }
                val sign = if (diff >= 0) "+" else "−"
                val unitSuffix = if (unit.isNotBlank()) " $unit" else ""
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "$sign${"%.1f".format(kotlin.math.abs(diff))}$unitSuffix vs. 30-Tage-Mittel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private enum class HcDetailRange { SEVEN, THIRTY, NINETY, ALL }

@Composable
private fun HcRangeSwitcher(current: HcDetailRange, onChange: (HcDetailRange) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HcDetailRange.values().forEach { range ->
            val label = when (range) {
                HcDetailRange.SEVEN -> "7T"
                HcDetailRange.THIRTY -> "30T"
                HcDetailRange.NINETY -> "90T"
                HcDetailRange.ALL -> "Alle"
            }
            FilterChip(
                selected = range == current,
                onClick = { onChange(range) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CosmosColors.AccentPrimary.copy(alpha = 0.20f),
                    selectedLabelColor = CosmosColors.AccentPrimary,
                ),
            )
        }
    }
}

@Composable
private fun HcStatsRow(
    min: Double?,
    max: Double?,
    avg: Double?,
    count: Int,
    unit: String,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            HcStatCell(label = "Min", value = min, unit = unit, color = cosmos.textPrimary, modifier = Modifier.weight(1f))
            HcStatCell(label = "Mittel", value = avg, unit = unit, color = cosmos.textPrimary, modifier = Modifier.weight(1f))
            HcStatCell(label = "Max", value = max, unit = unit, color = cosmos.textPrimary, modifier = Modifier.weight(1f))
            HcStatCell(label = "Anzahl", value = count.toDouble(), unit = "", color = cosmos.textPrimary, modifier = Modifier.weight(1f), formatter = { "${it.toInt()}" })
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
    val deltaColor = if (deltaText != null && average != null) {
        val diff = value - average
        val isImprovement = if (lowerIsBetter) diff < 0 else diff > 0
        if (isImprovement) CosmosColors.Success else CosmosColors.Critical
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

private fun formatHcDate(ms: Long): String {
    val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)
    return fmt.format(Date(ms))
}
