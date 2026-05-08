package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.charts.InteractiveLineChart
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Detail-Screen für eine einzelne Whoop-Metrik (HRV, RHR, Sleep-Performance, ...).
 *
 * Frank-Wunsch 2026-05-08: "Wenn ich auf HRV drücke, möchte ich die gesamte
 * Historie sehen, in Zahlen und als interaktiver Chart, mit Range-Switcher".
 *
 * Zeigt:
 * - Header mit Metrik-Name + Statistiken (Min/Max/Mittel, Anzahl Werte)
 * - Range-Switcher (7T / 30T / 90T / Alle)
 * - Großer interaktiver Chart (Y-Achse, X-Achse, Tap-Tooltip)
 * - Liste ALLER Werte mit Datum + Wert + Delta zum Vortag
 */
@Composable
fun BiomarkerDetailScreen(
    metricKey: String,
    onBack: () -> Unit,
    vm: BiomarkerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val cosmos = LocalCosmos.current
    val spec = metricSpecFor(metricKey)
    var range by remember { mutableStateOf(DetailRange.ALL) }

    val now = System.currentTimeMillis()
    val cutoff = when (range) {
        DetailRange.SEVEN -> now - 7L * 24 * 60 * 60 * 1000
        DetailRange.THIRTY -> now - 30L * 24 * 60 * 60 * 1000
        DetailRange.NINETY -> now - 90L * 24 * 60 * 60 * 1000
        DetailRange.ALL -> Long.MIN_VALUE
    }

    val filtered = state.history.filter { it.capturedAt >= cutoff }
    val pointsAll = filtered.mapNotNull { snap ->
        spec.extract(snap)?.let { snap.capturedAt to it }
    }
    val values = pointsAll.map { it.second }
    val minV = values.minOrNull()
    val maxV = values.maxOrNull()
    val avgV = values.takeIf { it.isNotEmpty() }?.average()

    CosmosScaffold(
        title = spec.title,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, "Zurück", tint = cosmos.textPrimary)
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // Range-Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DetailRange.values().forEach { r ->
                        FilterChip(
                            selected = range == r,
                            onClick = { range = r },
                            label = { Text(r.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = spec.accent.copy(alpha = 0.2f),
                                selectedLabelColor = cosmos.textPrimary,
                                containerColor = cosmos.glassBg,
                                labelColor = cosmos.textSecondary,
                            ),
                        )
                    }
                }
            }
            item {
                // Statistik-Card: Min/Max/Mittel/Anzahl
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("Anzahl", values.size.toString(), spec.accent, Modifier.weight(1f))
                            StatCell("Min", minV?.let { spec.format(it) } ?: "—", spec.accent, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("Mittel", avgV?.let { spec.format(it) } ?: "—", spec.accent, Modifier.weight(1f))
                            StatCell("Max", maxV?.let { spec.format(it) } ?: "—", spec.accent, Modifier.weight(1f))
                        }
                    }
                }
            }
            item {
                // Großer Chart
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "${spec.title} — Verlauf",
                            color = cosmos.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (pointsAll.isEmpty()) {
                            Text(
                                text = "Keine Daten in diesem Zeitraum.",
                                color = cosmos.textSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            InteractiveLineChart(
                                points = pointsAll,
                                accent = spec.accent,
                                unit = spec.unit,
                                height = 220,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Alle Werte (${pointsAll.size})",
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            // Werte-Liste (juengster zuerst). Mit Delta zum Vortag.
            val sortedDescending = pointsAll.sortedByDescending { it.first }
            items(sortedDescending.size) { idx ->
                val (ts, value) = sortedDescending[idx]
                val previousValue = sortedDescending.getOrNull(idx + 1)?.second
                ValueRow(
                    timestamp = ts,
                    value = value,
                    previousValue = previousValue,
                    spec = spec,
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(cosmos.glassBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = cosmos.textSecondary, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = accent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ValueRow(
    timestamp: Long,
    value: Double,
    previousValue: Double?,
    spec: MetricSpec,
) {
    val cosmos = LocalCosmos.current
    val date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    val delta = previousValue?.let { value - it }
    val deltaText = delta?.let {
        val sign = if (it >= 0) "+" else ""
        // Bei RHR ist niedriger besser — Pfeil-Logik invertiert.
        "$sign${spec.format(it)}"
    } ?: ""
    val deltaPositive = if (delta == null) true
    else if (spec.lowerIsBetter) delta < 0
    else delta > 0
    val deltaColor = if (delta == null) cosmos.textSecondary
    else if (deltaPositive) CosmosColors.Success
    else CosmosColors.Critical

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cosmos.glassBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date.format(DATE_FORMAT),
            color = cosmos.textPrimary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = spec.format(value),
            color = spec.accent,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (deltaText.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Box(modifier = Modifier.padding(start = 8.dp).fillMaxWidth(0.25f)) {
                Text(
                    text = deltaText,
                    color = deltaColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** Spezifikation pro Metrik — wie wird der Wert extrahiert, formatiert, beschriftet. */
private data class MetricSpec(
    val title: String,
    val unit: String,
    val accent: Color,
    val extract: (BiomarkerSnapshotEntity) -> Double?,
    val format: (Double) -> String,
    val lowerIsBetter: Boolean = false,
)

@Composable
private fun metricSpecFor(key: String): MetricSpec = when (key) {
    MetricKey.HRV -> MetricSpec(
        title = "HRV",
        unit = "ms",
        accent = CosmosColors.AccentPrimary,
        extract = { it.hrvMs },
        format = { "%.1f ms".format(it) },
    )
    MetricKey.RHR -> MetricSpec(
        title = "Resting Heart Rate",
        unit = "bpm",
        accent = CosmosColors.Critical,
        extract = { it.restingHeartRate?.toDouble() },
        format = { "%.0f bpm".format(it) },
        lowerIsBetter = true,
    )
    MetricKey.SLEEP_PERF -> MetricSpec(
        title = "Schlaf-Performance",
        unit = "%",
        accent = CosmosColors.Success,
        extract = { it.sleepPerformance?.toDouble() },
        format = { "%.0f %%".format(it) },
    )
    MetricKey.SLEEP_TOTAL -> MetricSpec(
        title = "Schlafdauer",
        unit = "min",
        accent = CosmosColors.AccentSecondary,
        extract = { it.sleepTotalMinutes?.toDouble() },
        format = { v ->
            val m = v.toInt()
            "${m / 60}h ${(m % 60).toString().padStart(2, '0')}min"
        },
    )
    MetricKey.SLEEP_REM -> MetricSpec(
        title = "REM-Schlaf",
        unit = "min",
        accent = CosmosColors.AccentSecondary,
        extract = { it.sleepRemMinutes?.toDouble() },
        format = { "%.0f min".format(it) },
    )
    MetricKey.SLEEP_DEEP -> MetricSpec(
        title = "Tiefschlaf",
        unit = "min",
        accent = CosmosColors.AccentPrimary,
        extract = { it.sleepDeepMinutes?.toDouble() },
        format = { "%.0f min".format(it) },
    )
    MetricKey.SLEEP_LIGHT -> MetricSpec(
        title = "Leichtschlaf",
        unit = "min",
        accent = CosmosColors.Warning,
        extract = { it.sleepLightMinutes?.toDouble() },
        format = { "%.0f min".format(it) },
    )
    MetricKey.SLEEP_AWAKE -> MetricSpec(
        title = "Wachzeit",
        unit = "min",
        accent = CosmosColors.Critical,
        extract = { it.sleepAwakeMinutes?.toDouble() },
        format = { "%.0f min".format(it) },
        lowerIsBetter = true,
    )
    MetricKey.SLEEP_DISTURBANCES -> MetricSpec(
        title = "Störungen",
        unit = "x",
        accent = CosmosColors.Warning,
        extract = { it.sleepDisturbances?.toDouble() },
        format = { "%.0f x".format(it) },
        lowerIsBetter = true,
    )
    MetricKey.STRAIN -> MetricSpec(
        title = "Strain",
        unit = "",
        accent = CosmosColors.Warning,
        extract = { it.dayStrain },
        format = { "%.1f".format(it) },
    )
    MetricKey.KILOJOULES -> MetricSpec(
        title = "Tagesumsatz",
        unit = "kcal",
        accent = CosmosColors.AccentPrimary,
        // Frank-Wunsch 2026-05-09: Anzeige in Kilokalorien statt Kilojoule
        // (Whoop liefert kJ, Faktor 4.184 fuer kcal). DB bleibt unveraendert in kJ.
        extract = { it.dayKilojoules?.div(4.184) },
        format = { "%.0f kcal".format(it) },
    )
    MetricKey.RECOVERY -> MetricSpec(
        title = "Recovery",
        unit = "%",
        accent = CosmosColors.Success,
        extract = { it.recoveryScore?.toDouble() },
        format = { "%.0f %%".format(it) },
    )
    // Phase 11 — neue Whoop-Felder (Frank-Wunsch 2026-05-08).
    MetricKey.RESPIRATORY -> MetricSpec(
        title = "Atemfrequenz",
        unit = "/min",
        accent = CosmosColors.AccentPrimary,
        extract = { it.respiratoryRate },
        format = { "%.1f /min".format(it) },
    )
    MetricKey.SLEEP_CONSISTENCY -> MetricSpec(
        title = "Schlafregelmäßigkeit",
        unit = "%",
        accent = CosmosColors.Success,
        extract = { it.sleepConsistencyPercent?.toDouble() },
        format = { "%.0f %%".format(it) },
    )
    MetricKey.SLEEP_EFFICIENCY -> MetricSpec(
        title = "Schlafeffizienz",
        unit = "%",
        accent = CosmosColors.Success,
        extract = { it.sleepEfficiencyPercent?.toDouble() },
        format = { "%.0f %%".format(it) },
    )
    MetricKey.SLEEP_NEED -> MetricSpec(
        title = "Schlafbedarf",
        unit = "min",
        accent = CosmosColors.AccentSecondary,
        extract = { it.sleepNeedMinutes?.toDouble() },
        format = { v ->
            val m = v.toInt()
            "${m / 60}h ${(m % 60).toString().padStart(2, '0')}min"
        },
    )
    MetricKey.SLEEP_DEBT -> MetricSpec(
        title = "Schlafdefizit",
        unit = "min",
        accent = CosmosColors.Warning,
        extract = { it.sleepDebtMinutes?.toDouble() },
        format = { v ->
            val m = v.toInt()
            if (m == 0) "0 min"
            else "${m / 60}h ${(m % 60).toString().padStart(2, '0')}min"
        },
        lowerIsBetter = true,
    )
    MetricKey.SPO2 -> MetricSpec(
        title = "Sauerstoffsättigung",
        unit = "%",
        accent = CosmosColors.Success,
        extract = { it.spo2Percent },
        format = { "%.1f %%".format(it) },
    )
    MetricKey.SKIN_TEMP -> MetricSpec(
        title = "Hauttemperatur",
        unit = "°C",
        accent = CosmosColors.Warning,
        extract = { it.skinTempCelsius },
        format = { "%.1f °C".format(it) },
    )
    MetricKey.AVG_HR -> MetricSpec(
        title = "Durchschnittliche Herzfrequenz",
        unit = "bpm",
        accent = CosmosColors.Critical,
        extract = { it.averageHeartRate?.toDouble() },
        format = { "%.0f bpm".format(it) },
        lowerIsBetter = true,
    )
    MetricKey.MAX_HR -> MetricSpec(
        title = "Max. Herzfrequenz",
        unit = "bpm",
        accent = CosmosColors.Critical,
        extract = { it.maxHeartRate?.toDouble() },
        format = { "%.0f bpm".format(it) },
    )
    else -> MetricSpec(
        title = "Unbekannt",
        unit = "",
        accent = CosmosColors.AccentPrimary,
        extract = { null },
        format = { "%.1f".format(it) },
    )
}

private enum class DetailRange(val label: String) {
    SEVEN("7T"),
    THIRTY("30T"),
    NINETY("90T"),
    ALL("Alle"),
}

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", Locale.GERMANY)
