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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import de.frank.entropyreducer.presentation.components.ColorPaletteBar
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.charts.InteractiveLineChart
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.whoopRecoveryColor
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
    val state by vm.state.collectAsStateWithLifecycle()
    val cardColors by vm.cardColors.collectAsStateWithLifecycle()
    val cosmos = LocalCosmos.current
    val spec = metricSpecFor(metricKey)
    var range by remember { mutableStateOf(DetailRange.ALL) }

    // Frank-Wunsch 2026-05-18: Farb-Auswahl pro Pattern. Wirkt sich auf BEIDE
    // Karten (Mini + volle) aus, weil Mini- und volle Karte denselben Pattern
    // visuell darstellen — eine gemeinsame Farbe haelt das Layout konsistent.
    val cardIdsForMetric = cardIdsForMetricKey(metricKey)
    val selectedColorIndex =
        cardIdsForMetric.firstNotNullOfOrNull { id ->
            de.frank.entropyreducer.data.repository.cardColorIndexForTheme(cardColors, id, cosmos.isDark)
                .takeIf { it != 0 }
        } ?: 0

    val now = System.currentTimeMillis()
    val cutoff = when (range) {
        DetailRange.SEVEN -> now - 7L * 24 * 60 * 60 * 1000
        DetailRange.THIRTY -> now - 30L * 24 * 60 * 60 * 1000
        DetailRange.NINETY -> now - 90L * 24 * 60 * 60 * 1000
        DetailRange.ALL -> Long.MIN_VALUE
    }

    // Frank-Wunsch 2026-05-18: VO2max-Werte kommen nicht aus dem
    // BiomarkerSnapshot, sondern aus AmazfitWorkouts × Whoop-Snapshots —
    // vorberechnet im VM. Spezialfall: precomputed-Liste aus chartData
    // nutzen statt extract-Lookup ueber state.history.
    val pointsAll: List<Pair<Long, Double>> =
        if (metricKey == MetricKey.VO2MAX) {
            (state.chartData.fullPoints["vo2max"] ?: emptyList())
                .filter { it.first >= cutoff }
        } else {
            state.history
                .filter { it.capturedAt >= cutoff }
                .mapNotNull { snap -> spec.extract(snap)?.let { snap.capturedAt to it } }
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
                // Frank-Wunsch 2026-05-18: Farbpalette oben — Auswahl wirkt
                // sich auf den Karten-Hintergrund im Uebersichts-Screen aus.
                ColorPaletteBar(
                    selectedIndex = selectedColorIndex,
                    onPick = { idx ->
                        cardIdsForMetric.forEach { id -> vm.setCardColor(id, idx, cosmos.isDark) }
                    },
                )
            }
            item {
                // Range-Switcher
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ALL_DETAIL_RANGES.forEach { r ->
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
                        // Frank-Wunsch 2026-06-01: Min/Mittel/Max wert-abhaengig faerben
                        // (z.B. Recovery nach WHOOP-Ampel). Faellt auf spec.accent zurueck
                        // wenn die Metrik keine wert-abhaengige Faerbung definiert.
                        val statColor: (Double?) -> Color = { v ->
                            v?.let { spec.valueColor?.invoke(it) } ?: spec.accent
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("Anzahl", values.size.toString(), spec.accent, Modifier.weight(1f))
                            StatCell("Min", minV?.let { spec.format(it) } ?: "—", statColor(minV), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCell("Mittel", avgV?.let { spec.format(it) } ?: "—", statColor(avgV), Modifier.weight(1f))
                            StatCell("Max", maxV?.let { spec.format(it) } ?: "—", statColor(maxV), Modifier.weight(1f))
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
                                // Frank-Wunsch 2026-05-11: Y-Achse und Tooltip mit der
                                // gleichen Formatierung wie die Werte-Liste/Statistik —
                                // Schlafdauer also "7h 23min" statt nackter Minutenzahl.
                                valueFormatter = spec.format,
                                // Frank-Wunsch 2026-05-16: Trendlinien-Farbe muss
                                // semantisch sein. Bei RHR/Atemfrequenz/Hauttemperatur
                                // ist niedriger besser — fallend = gruen, steigend = rot.
                                lowerIsBetter = spec.lowerIsBetter,
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
            // Performance-Audit Loop 3 (2026-05-10): sortedByDescending pro
            // Recomposition vermeiden + key=ts fuer strukturelles Diffing der Items.
            val sortedDescending = pointsAll.sortedByDescending { it.first }
            itemsIndexed(sortedDescending, key = { _, (ts, _) -> ts }) { idx, (ts, value) ->
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
    else if (deltaPositive) LocalCosmos.current.ok
    else LocalCosmos.current.crit

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
            // Frank-Wunsch 2026-06-01: wert-abhaengige Faerbung (Recovery nach WHOOP-Ampel),
            // sonst einheitlich spec.accent.
            color = spec.valueColor?.invoke(value) ?: spec.accent,
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
    /**
     * Frank-Wunsch 2026-06-01: optionale wert-abhaengige Faerbung der Zahlen
     * (Min/Mittel/Max + Werte-Liste). null = einheitlich spec.accent (Standard fuer
     * die meisten Metriken). Bei Recovery liefert sie die offizielle WHOOP-Ampel,
     * damit die Werte nicht mehr alle gruen sind, sondern je nach Prozent rot/gelb/gruen.
     */
    val valueColor: ((Double) -> Color)? = null,
)

@Composable
private fun metricSpecFor(key: String): MetricSpec = when (key) {
    MetricKey.HRV -> MetricSpec(
        title = "HRV",
        unit = "ms",
        accent = LocalCosmos.current.accent,
        extract = { it.hrvMs },
        format = { "%.1f ms".format(it) },
    )
    MetricKey.RHR -> MetricSpec(
        title = "Ruhepuls",
        unit = "bpm",
        accent = LocalCosmos.current.crit,
        extract = { it.restingHeartRate?.toDouble() },
        format = { "%.0f bpm".format(it) },
        lowerIsBetter = true,
    )
    MetricKey.SLEEP_PERF -> MetricSpec(
        title = "Schlaf-Performance",
        unit = "%",
        accent = LocalCosmos.current.ok,
        extract = { it.sleepPerformance?.toDouble() },
        format = { "%.0f %%".format(it) },
    )
    MetricKey.SLEEP_TOTAL -> MetricSpec(
        title = "Schlafdauer",
        unit = "min",
        accent = LocalCosmos.current.accentForscher,
        // Frank-Wunsch 2026-05-16: Schlafdauer-Liste ohne Wachzeit anzeigen
        // (Tief + REM + Leicht). Whoop liefert totalInBedMilli — Wachzeit
        // wird hier aktiv subtrahiert damit Wert konsistent mit Hero+Pattern ist.
        extract = { snap ->
            snap.sleepTotalMinutes?.let { total ->
                val awake = snap.sleepAwakeMinutes ?: 0
                (total - awake).coerceAtLeast(0).toDouble().takeIf { it > 0 }
            }
        },
        format = { v ->
            val m = v.toInt()
            "${m / 60}h ${(m % 60).toString().padStart(2, '0')}min"
        },
    )
    MetricKey.SLEEP_REM -> MetricSpec(
        title = "REM-Schlaf",
        unit = "min",
        accent = LocalCosmos.current.accentForscher,
        extract = { it.sleepRemMinutes?.toDouble() },
        format = { "%.0f min".format(it) },
    )
    MetricKey.SLEEP_DEEP -> MetricSpec(
        title = "Tiefschlaf",
        unit = "min",
        accent = LocalCosmos.current.accent,
        extract = { it.sleepDeepMinutes?.toDouble() },
        format = { "%.0f min".format(it) },
    )
    MetricKey.SLEEP_LIGHT -> MetricSpec(
        title = "Leichtschlaf",
        unit = "min",
        accent = LocalCosmos.current.warn,
        extract = { it.sleepLightMinutes?.toDouble() },
        format = { "%.0f min".format(it) },
    )
    MetricKey.SLEEP_AWAKE -> MetricSpec(
        title = "Wachzeit",
        unit = "min",
        accent = LocalCosmos.current.crit,
        extract = { it.sleepAwakeMinutes?.toDouble() },
        format = { "%.0f min".format(it) },
        lowerIsBetter = true,
    )
    MetricKey.SLEEP_DISTURBANCES -> MetricSpec(
        title = "Störungen",
        unit = "x",
        accent = LocalCosmos.current.warn,
        extract = { it.sleepDisturbances?.toDouble() },
        format = { "%.0f x".format(it) },
        lowerIsBetter = true,
    )
    MetricKey.STRAIN -> MetricSpec(
        title = "Belastung",
        unit = "",
        accent = LocalCosmos.current.warn,
        extract = { it.dayStrain },
        format = { "%.1f".format(it) },
    )
    MetricKey.KILOJOULES -> MetricSpec(
        title = "Tagesumsatz",
        unit = "kcal",
        accent = LocalCosmos.current.accent,
        // Frank-Wunsch 2026-05-09: Anzeige in Kilokalorien statt Kilojoule
        // (Whoop liefert kJ, Faktor 4.184 fuer kcal). DB bleibt unveraendert in kJ.
        extract = { it.dayKilojoules?.div(4.184) },
        format = { "%.0f kcal".format(it) },
    )
    MetricKey.RECOVERY -> MetricSpec(
        title = "Recovery",
        unit = "%",
        accent = LocalCosmos.current.ok,
        extract = { it.recoveryScore?.toDouble() },
        format = { "%.0f %%".format(it) },
        // Frank-Wunsch 2026-06-01: Werte nach offizieller WHOOP-Ampel faerben
        // (vorher alle gruen). 67-100 gruen, 34-66 gelb, 0-33 rot.
        valueColor = { whoopRecoveryColor(it) },
    )
    // Phase 11 — neue Whoop-Felder (Frank-Wunsch 2026-05-08).
    MetricKey.RESPIRATORY -> MetricSpec(
        title = "Atemfrequenz",
        unit = "/min",
        accent = LocalCosmos.current.accent,
        extract = { it.respiratoryRate },
        format = { "%.1f /min".format(it) },
        // Frank-Wunsch 2026-05-16: steigende Atemfrequenz ist negativ — Trendlinie rot.
        lowerIsBetter = true,
    )
    MetricKey.SLEEP_CONSISTENCY -> MetricSpec(
        title = "Schlafregelmäßigkeit",
        unit = "%",
        accent = LocalCosmos.current.ok,
        extract = { it.sleepConsistencyPercent?.toDouble() },
        format = { "%.0f %%".format(it) },
    )
    MetricKey.SLEEP_EFFICIENCY -> MetricSpec(
        title = "Schlafeffizienz",
        unit = "%",
        accent = LocalCosmos.current.ok,
        extract = { it.sleepEfficiencyPercent?.toDouble() },
        format = { "%.0f %%".format(it) },
    )
    MetricKey.SLEEP_NEED -> MetricSpec(
        title = "Schlafbedarf",
        unit = "min",
        accent = LocalCosmos.current.accentForscher,
        extract = { it.sleepNeedMinutes?.toDouble() },
        format = { v ->
            val m = v.toInt()
            "${m / 60}h ${(m % 60).toString().padStart(2, '0')}min"
        },
    )
    MetricKey.SLEEP_DEBT -> MetricSpec(
        title = "Schlafdefizit",
        unit = "min",
        accent = LocalCosmos.current.warn,
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
        accent = LocalCosmos.current.ok,
        extract = { it.spo2Percent },
        format = { "%.1f %%".format(it) },
    )
    MetricKey.SKIN_TEMP -> MetricSpec(
        title = "Hauttemperatur",
        unit = "°C",
        accent = LocalCosmos.current.warn,
        extract = { it.skinTempCelsius },
        format = { "%.1f °C".format(it) },
        // Frank-Wunsch 2026-05-16: niedrigere Hauttemperatur ist im Schlafkontext
        // positiv — fallend = gruen, steigend = rot.
        lowerIsBetter = true,
    )
    MetricKey.MAX_HR -> MetricSpec(
        title = "Max. Herzfrequenz",
        unit = "bpm",
        accent = LocalCosmos.current.crit,
        extract = { it.maxHeartRate?.toDouble() },
        format = { "%.0f bpm".format(it) },
    )
    MetricKey.SLEEP_RESTORATIVE -> MetricSpec(
        title = "Erholsamer Schlaf",
        unit = "%",
        accent = LocalCosmos.current.ok,
        extract = { snap ->
            val rem = snap.sleepRemMinutes ?: 0
            val deep = snap.sleepDeepMinutes ?: 0
            val light = snap.sleepLightMinutes ?: 0
            val awake = snap.sleepAwakeMinutes ?: 0
            val timeInBed = rem + deep + light + awake
            if (timeInBed > 0) (rem + deep).toDouble() / timeInBed.toDouble() * 100.0 else null
        },
        format = { "%.1f %%".format(it) },
    )
    MetricKey.SLEEP_CYCLES -> MetricSpec(
        title = "Schlafzyklen",
        unit = "x",
        accent = LocalCosmos.current.accentForscher,
        extract = { it.sleepCycleCount?.toDouble() },
        format = { "%.0f x".format(it) },
    )
    // Frank-Wunsch 2026-05-18: VO2max wird NICHT aus dem BiomarkerSnapshot
    // gelesen — die Werte kommen aus chartData["vo2max"], berechnet aus
    // VO2-faehigen Workouts. extract liefert hier null, der DetailScreen
    // hat einen Spezialfall der die precomputed-Liste direkt nutzt.
    MetricKey.VO2MAX -> MetricSpec(
        title = "VO2max",
        unit = "ml/(kg·min)",
        accent = LocalCosmos.current.accentForscher,
        extract = { null },
        format = { "${"%.1f".format(it).replace('.', ',')} ml/min" },
    )
    else -> MetricSpec(
        title = "Unbekannt",
        unit = "",
        accent = LocalCosmos.current.accent,
        extract = { null },
        format = { "%.1f".format(it) },
    )
}

/**
 * Frank-Wunsch 2026-05-18: Mapping MetricKey -> Card-IDs. Wenn Frank im
 * Detail-Screen eine Farbe waehlt, soll diese sowohl die Mini-Karte (z.B.
 * `BiomarkerCardId.MINI_HRV`) als auch die ggf. vorhandene volle Karte
 * (z.B. `BiomarkerCardId.HRV`) im Uebersichts-Screen einfaerben — ein
 * Pattern, eine Farbe, ueberall sichtbar.
 */
internal fun cardIdsForMetricKey(metricKey: String): List<String> = when (metricKey) {
    MetricKey.HRV -> listOf(BiomarkerCardId.HRV, BiomarkerCardId.MINI_HRV)
    MetricKey.RHR -> listOf(BiomarkerCardId.RHR, BiomarkerCardId.MINI_RHR)
    // Schlafdauer und Schlafphasen teilen sich den Detail-Screen — Farbe
    // gilt fuer Mini-, volle Schlafdauer- und Schlafphasen-Karte gemeinsam
    // (Frank-Wunsch 2026-05-18 Folgeauftrag).
    MetricKey.SLEEP_TOTAL ->
        listOf(
            BiomarkerCardId.SLEEP_TOTAL,
            BiomarkerCardId.MINI_SLEEP_TOTAL,
            BiomarkerCardId.SLEEP_STAGES,
        )
    MetricKey.SLEEP_PERF ->
        listOf(BiomarkerCardId.SLEEP_PERFORMANCE, BiomarkerCardId.MINI_SLEEP_PERFORMANCE)
    MetricKey.VO2MAX -> listOf(BiomarkerCardId.MINI_VO2MAX)
    MetricKey.SLEEP_DEEP -> listOf(BiomarkerCardId.SLEEP_DEEP_GRAPH)
    MetricKey.SLEEP_REM -> listOf(BiomarkerCardId.SLEEP_REM_GRAPH)
    MetricKey.SLEEP_AWAKE -> listOf(BiomarkerCardId.SLEEP_WAKE_GRAPH)
    MetricKey.RECOVERY -> listOf(BiomarkerCardId.GESAMTERHOLUNG, BiomarkerCardId.RECOVERY_GRAPH)
    MetricKey.SLEEP_RESTORATIVE -> listOf(BiomarkerCardId.SLEEP_RESTORATIVE_GRAPH)
    // Skin-Temp und Skin-Temp-Delta teilen den Detail-Screen — beide Karten
    // werden gemeinsam eingefaerbt.
    MetricKey.SKIN_TEMP ->
        listOf(BiomarkerCardId.SKIN_TEMP, BiomarkerCardId.SKIN_TEMP_DELTA)
    MetricKey.SKIN_TEMP_DELTA ->
        listOf(BiomarkerCardId.SKIN_TEMP, BiomarkerCardId.SKIN_TEMP_DELTA)
    // Fallback: der metricKey selbst ist auch die Card-ID (gilt fuer
    // RESPIRATORY, SPO2, SLEEP_EFFICIENCY, SLEEP_CONSISTENCY,
    // SLEEP_DEBT, STRAIN, KILOJOULES, ...).
    else -> listOf(metricKey)
}

private enum class DetailRange(val label: String) {
    SEVEN("7T"),
    THIRTY("30T"),
    NINETY("90T"),
    ALL("Alle"),
}

// Performance-Audit Loop 8 (2026-05-10): Top-level Liste statt .values()-
// Allokation pro Recomposition.
private val ALL_DETAIL_RANGES: List<DetailRange> = DetailRange.entries.toList()

private val DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", Locale.GERMANY)
