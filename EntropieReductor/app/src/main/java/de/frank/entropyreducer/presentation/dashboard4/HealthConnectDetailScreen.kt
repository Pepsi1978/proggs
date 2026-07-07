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
import androidx.compose.material.icons.outlined.Tune
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
 * Detail-Screen fuer Health-Connect-Body-Composition-Werte (Frank-Wunsch 2026-05-10).
 *
 * Tap auf eine der Mini-Karten Gewicht / Koerperfett / Magermasse / Wasser / Knochen oeffnet diesen
 * Screen — analog zum Oura-Detail-Screen. Zeigt:
 * - Header mit aktuellem Wert + Datum + Plus/Minus zum 30-Tage-Mittel
 * - Range-Switcher 7T / 30T / 90T / Alle
 * - Linien-Verlauf der gefilterten Werte (InteractiveLineChart)
 * - Vollstaendige Liste aller Werte mit Datum + Plus/Minus zum 30-Tage-Mittel
 * - Refresh-Button im Header (zieht aktuelle Daten aus Health Connect)
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
private fun specFor(metricKey: String): HcMetricSpec =
    when (metricKey) {
        HealthConnectMetricKey.WEIGHT ->
            HcMetricSpec(
                title = "Gewicht",
                unit = "kg",
                accent = LocalCosmos.current.accent,
                lowerIsBetter = true,
            )
        HealthConnectMetricKey.BODY_FAT ->
            HcMetricSpec(
                title = "Körperfett",
                unit = "%",
                accent = LocalCosmos.current.warn,
                lowerIsBetter = true,
            )
        HealthConnectMetricKey.LEAN_BODY_MASS ->
            HcMetricSpec(
                title = "Magermasse",
                unit = "kg",
                accent = LocalCosmos.current.ok,
                lowerIsBetter = false,
            )
        HealthConnectMetricKey.BODY_WATER ->
            HcMetricSpec(
                title = "Körperwasser",
                unit = "kg",
                accent = LocalCosmos.current.accentForscher,
                lowerIsBetter = false,
            )
        HealthConnectMetricKey.BONE_MASS ->
            HcMetricSpec(
                title = "Knochenmasse",
                unit = "kg",
                accent = LocalCosmos.current.accent,
                lowerIsBetter = false,
            )
        HealthConnectMetricKey.MUSCLE_MASS ->
            HcMetricSpec(
                title = "Muskelmasse",
                unit = "kg",
                accent = LocalCosmos.current.ok,
                lowerIsBetter = false,
            )
        else -> HcMetricSpec("Health Connect", "", LocalCosmos.current.accent, false)
    }

@Composable
fun HealthConnectDetailScreen(
    metricKey: String,
    onBack: () -> Unit,
    vm: BiomarkerViewModel = hiltViewModel(),
) {
    val weight by vm.weight.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val spec = specFor(metricKey)

    // Datenquelle pro Metrik. history30d ist bereits in WeightState verfuegbar —
    // 30 Tage reichen fuer den Default-Range. Fuer 90T / Alle laden wir on-demand
    // hier nicht nach (wuerde ein eigenes ViewModel-Feld brauchen) — Frank kriegt
    // die letzten 30 Tage als Default und kann das spaeter erweitern.
    //
    // Performance-Audit Loop 2 (2026-05-10): when-Block + .associate + .map werden
    // einmalig in remember(metricKey, weight) berechnet statt bei jeder Recomposition
    // (z.B. bei Range-Filter-Click). Bei MUSCLE_MASS sparen wir O(N) Map-Aufbau +
    // O(M) List-Transform pro Recomposition.
    val history: List<Pair<Long, Double>> =
        remember(metricKey, weight) {
            when (metricKey) {
                HealthConnectMetricKey.WEIGHT -> weight.history30d
                HealthConnectMetricKey.BODY_FAT -> weight.bodyFatHistory30d
                HealthConnectMetricKey.LEAN_BODY_MASS -> weight.leanBodyMassHistory30d
                HealthConnectMetricKey.BODY_WATER -> weight.bodyWaterMassHistory30d
                HealthConnectMetricKey.BONE_MASS -> weight.boneMassHistory30d
                HealthConnectMetricKey.MUSCLE_MASS -> {
                    // Muskelmasse-History ≈ LeanBodyMass-History minus BoneMass-History
                    // mit Zeitstempel-Matching. Wenn keine BoneMass-Werte: fallback Lean.
                    val boneByMs = weight.boneMassHistory30d.associate { it.first to it.second }
                    weight.leanBodyMassHistory30d.map { (ts, lean) ->
                        val bone =
                            boneByMs[ts]
                                ?: weight.boneMassHistory30d
                                    .minByOrNull { kotlin.math.abs(it.first - ts) }
                                    ?.second
                        ts to (if (bone != null) lean - bone else lean)
                    }
                }
                else -> emptyList()
            }
        }
    val latest: Double? =
        when (metricKey) {
            HealthConnectMetricKey.WEIGHT -> weight.latestKg
            HealthConnectMetricKey.BODY_FAT -> weight.latestBodyFatPercent
            HealthConnectMetricKey.LEAN_BODY_MASS -> weight.latestLeanBodyMassKg
            HealthConnectMetricKey.BODY_WATER -> weight.latestBodyWaterMassKg
            HealthConnectMetricKey.BONE_MASS -> weight.latestBoneMassKg
            HealthConnectMetricKey.MUSCLE_MASS -> {
                val lean = weight.latestLeanBodyMassKg
                val bone = weight.latestBoneMassKg
                if (lean == null) null else if (bone != null) lean - bone else lean
            }
            else -> null
        }
    val avg30: Double? =
        when (metricKey) {
            HealthConnectMetricKey.WEIGHT -> weight.avg30dKg
            HealthConnectMetricKey.BODY_FAT -> weight.avg30dBodyFatPercent
            HealthConnectMetricKey.LEAN_BODY_MASS -> weight.avg30dLeanBodyMassKg
            HealthConnectMetricKey.BODY_WATER -> weight.avg30dBodyWaterMassKg
            HealthConnectMetricKey.BONE_MASS -> weight.avg30dBoneMassKg
            HealthConnectMetricKey.MUSCLE_MASS -> {
                val avgLean = weight.avg30dLeanBodyMassKg
                val avgBone = weight.avg30dBoneMassKg
                if (avgLean == null) null else if (avgBone != null) avgLean - avgBone else avgLean
            }
            else -> null
        }

    // Frank-Wunsch 2026-05-10 abend: Default-Filter "Alle" statt "30T", damit
    // der ganze HC-Verlauf sofort sichtbar ist. 30T war frueher der Default
    // weil HC ohne PERMISSION_READ_HEALTH_DATA_HISTORY nur 30 Tage lieferte —
    // mit erteilter Permission ist diese Begrenzung weg.
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
            // Frank-Wunsch 2026-05-10: Einzelne Permissions im Nachhinein
            // bearbeiten koennen. Dieser Button oeffnet die Health-Connect-
            // spezifische Permissions-UI fuer unsere App (mit Fallbacks bei
            // alten HC-Versionen, siehe HealthConnectManager).
            IconButton(onClick = vm::openHealthConnectPermissionsEditor) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Berechtigungen verwalten",
                    tint = cosmos.textPrimary,
                )
            }
            IconButton(onClick = vm::refreshWeight) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Werte neu laden",
                    tint = cosmos.textPrimary,
                )
            }
        },
    ) { padding ->
        // Frank-Wunsch 2026-05-18 Folgeauftrag: Farbpalette oben in jedem
        // Gewichts-Detail-Screen. Mapping HC-MetricKey -> CardId fuer die
        // jeweilige Mini-Karte im Uebersichts-Screen.
        val cardColorAccess = rememberCardColors()
        val targetCardId =
            when (metricKey) {
                HealthConnectMetricKey.WEIGHT -> BiomarkerCardId.MINI_WEIGHT
                HealthConnectMetricKey.BODY_FAT -> BiomarkerCardId.MINI_BODY_FAT
                HealthConnectMetricKey.LEAN_BODY_MASS -> BiomarkerCardId.MINI_LEAN_BODY_MASS
                HealthConnectMetricKey.BODY_WATER -> BiomarkerCardId.MINI_BODY_WATER
                HealthConnectMetricKey.BONE_MASS -> BiomarkerCardId.MINI_BONE_MASS
                HealthConnectMetricKey.MUSCLE_MASS -> BiomarkerCardId.MINI_MUSCLE_MASS
                else -> metricKey
            }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ColorPaletteBar(
                    selectedIndex = cardColorAccess.colors[targetCardId] ?: 0,
                    onPick = { idx -> cardColorAccess.setColor(targetCardId, idx) },
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
                items(sortedItems, key = { it.first }) { (ts, value) ->
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
                                    "Tippe oben rechts auf das Refresh-Symbol oder wechsle den Zeitraum.",
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
