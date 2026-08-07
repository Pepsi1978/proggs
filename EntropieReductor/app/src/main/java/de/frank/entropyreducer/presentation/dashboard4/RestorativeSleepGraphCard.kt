package de.frank.entropyreducer.presentation.dashboard4

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.presentation.components.ColorPaletteBar
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.charts.InteractiveLineChart
import de.frank.entropyreducer.presentation.components.charts.MiniBarsCanvas
import de.frank.entropyreducer.presentation.components.rememberCardColors
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Erholsamer-Schlaf-Verlaufs-Pattern (Frank-Wunsch 2026-06-18).
 *
 * Zeigt den prozentualen Anteil erholsamen Schlafs (REM + Tiefschlaf) der letzten
 * ~30 Tage als Balken-Graph. Tap auf den Graphen oeffnet eine Bottom-Sheet mit
 * ALLEN historischen Werten und der Abweichung zum jeweiligen Vortag. Unter dem
 * Graphen werden — wie bei allen anderen Karten — der 30-Tage-Durchschnitt UND
 * die aktuelle Abweichung zum Durchschnitt angezeigt.
 *
 * Prozent = (REM + Tiefschlaf) / (REM + Tief + Leicht + Wach) * 100
 *
 * Ampel-Logik (Frank-Vorgabe 2026-06-18):
 *   - unter 40 % → Rot   (kritisch wenig Erholung)
 *   - 40 – 50 %  → Gelb  (grenzwertig)
 *   - ueber 55 % → Gruen (gute Erholung)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestorativeSleepGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    precomputed: RestorativeSleepDerived? = null,
    // Poka-Yoke 2026-08-07: KEIN Default — sonst kompiliert ein Aufrufer, der den Tap
    // vergisst, klaglos durch und die Karte reagiert stumm nicht (genau der Fehler,
    // den Frank an Erholungs-/Tiefschlaf-/REM-/Wachzeit-/Erholsamer-Schlaf-Verlauf sah).
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.accent

    val derived =
        precomputed
            ?: remember(selectedSnapshot, history) {
                restorativeSleepDerived(selectedSnapshot = selectedSnapshot, history = history)
            }

    val headerColor = derived.currentPercent?.let { restorativeSleepBarColor(it) } ?: accent

    var sheetOpen by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Erholsamer Schlaf",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Prozentualer Anteil pro Nacht",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                Text(
                    text = derived.currentPercent?.let { "%.1f".format(it).replace('.', ',') + " %" } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            RestorativeSleepBars(values = derived.last30Percent)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        "30-Tage-Schnitt: ${derived.avg30Percent?.let { "%.1f".format(it).replace('.', ',') + " %" } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (derived.deltaVsAvg != null) {
                    RestorativeTrendBadgePercent(delta = derived.deltaVsAvg)
                }
            }
            Text(
                text = "Tippen fuer komplette Historie",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary.copy(alpha = 0.6f),
            )
        }
    }

    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }, sheetState = sheetState) {
            val cardColors = rememberCardColors()
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ColorPaletteBar(
                    selectedIndex = cardColors.colorFor(BiomarkerCardId.SLEEP_RESTORATIVE_GRAPH, cosmos.isDark),
                    onPick = { idx ->
                        cardColors.setColor(BiomarkerCardId.SLEEP_RESTORATIVE_GRAPH, idx, cosmos.isDark)
                    },
                )
                Spacer(Modifier.height(12.dp))
                InteractiveLineChart(
                    points = derived.chartPoints,
                    accent = LocalCosmos.current.accent,
                    unit = "%",
                    height = 200,
                    valueFormatter = { "%.1f".format(it).replace('.', ',') + " %" },
                    lowerIsBetter = false,
                )
                Spacer(Modifier.height(16.dp))
            }
            RestorativeHistorySheetContent(rows = derived.historyRows)
        }
    }
}

/* ------------------------- Datenaufbereitung ------------------------- */

@androidx.compose.runtime.Immutable
data class RestorativeSleepDerived(
    val currentPercent: Double?,
    val avg30Percent: Double?,
    val deltaVsAvg: Double?,
    val last30Percent: List<Double>,
    val historyRows: List<RestorativeSleepRow>,
    val chartPoints: List<Pair<Long, Double>>,
)

data class RestorativeSleepRow(
    val date: LocalDate,
    val percent: Double,
    val deltaToPrevDay: Double?,
)

internal fun restorativeSleepDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
): RestorativeSleepDerived {
    val zone = ZoneId.systemDefault()
    val all =
        history
            .mapNotNull { snap ->
                val pct = snap.restorativePercent() ?: return@mapNotNull null
                val date = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
                date to pct
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.average() }
            .toSortedMap()
            .map { (date, pct) -> date to pct }

    val current = selectedSnapshot?.restorativePercent() ?: all.lastOrNull()?.second
    val last30 = all.takeLast(30).map { it.second }
    val avg30 = if (last30.size >= 3) last30.average() else null
    val delta = if (current != null && avg30 != null) current - avg30 else null

    val rows = mutableListOf<RestorativeSleepRow>()
    all.forEachIndexed { idx, (date, pct) ->
        val prev = if (idx > 0) all[idx - 1].second else null
        val deltaPrev = if (prev != null) pct - prev else null
        rows += RestorativeSleepRow(date = date, percent = pct, deltaToPrevDay = deltaPrev)
    }
    val chartPoints =
        all.map { (date, pct) -> date.atStartOfDay(zone).toInstant().toEpochMilli() to pct }
    return RestorativeSleepDerived(
        currentPercent = current,
        avg30Percent = avg30,
        deltaVsAvg = delta,
        last30Percent = last30,
        historyRows = rows.reversed(),
        chartPoints = chartPoints,
    )
}

private fun BiomarkerSnapshotEntity.restorativePercent(): Double? {
    val rem = sleepRemMinutes ?: return null
    val deep = sleepDeepMinutes ?: 0
    val light = sleepLightMinutes ?: 0
    val awake = sleepAwakeMinutes ?: 0
    val total = deep + rem + light + awake
    if (total <= 0) return null
    return (rem + deep).toDouble() / total.toDouble() * 100.0
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun RestorativeSleepBars(values: List<Double>) {
    val yMin = remember(values) { (values.minOrNull() ?: 0.0) - 5.0 }
    val yMax = remember(values) { values.maxOrNull() ?: 70.0 }
    MiniBarsCanvas(
        values = values,
        barColor = { restorativeSleepBarColor(it) },
        yMin = yMin,
        yMax = yMax,
        emptyText = "Noch keine Erholsam-Schlaf-Daten",
    )
}

/**
 * Farbe pro Balken im Erholsamer-Schlaf-Graph (Frank-Vorgabe 2026-06-18):
 *   unter 40 % → Rot   (kritisch wenig Erholung)
 *   40 – 50 %  → Gelb  (grenzwertig)
 *   ueber 55 % → Gruen (gute Erholung)
 *
 * Frank-Wunsch 2026-06-21: Gleiche WHOOP-Farbpalette wie im Erholungsverlauf
 * (RecoveryGraphCard) — vorher dezente CosmosColors.Success/Warning/Critical,
 * jetzt knallig WhoopRecoveryGreen/Yellow/Red fuer visuelle Konsistenz.
 */
private fun restorativeSleepBarColor(pct: Double): Color =
    when {
        pct < 40.0 -> CosmosColors.WhoopRecoveryRed
        pct < 50.0 -> CosmosColors.WhoopRecoveryYellow
        else -> CosmosColors.WhoopRecoveryGreen
    }

@Composable
private fun RestorativeTrendBadgePercent(delta: Double) {
    val color =
        when {
            delta > 0.5 -> LocalCosmos.current.ok
            delta < -0.5 -> LocalCosmos.current.crit
            else -> LocalCosmos.current.accent
        }
    Box(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "%+.1f %%".format(delta),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ------------------------- Bottom-Sheet ------------------------- */

private val RESTORATIVE_SHEET_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", Locale.GERMANY)

@Composable
private fun RestorativeHistorySheetContent(rows: List<RestorativeSleepRow>) {
    val cosmos = LocalCosmos.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Erholsamer-Schlaf-Historie",
            style = MaterialTheme.typography.titleLarge,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Prozent pro Nacht · Abweichung zum Vortag",
            style = MaterialTheme.typography.labelSmall,
            color = cosmos.textSecondary,
        )
        Spacer(Modifier.height(12.dp))
        if (rows.isEmpty()) {
            Text(
                text = "Noch keine Daten gespeichert.",
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textSecondary,
            )
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(rows, key = { it.date.toEpochDay() }) { row -> RestorativeHistoryRow(row = row) }
        }
    }
}

@Composable
private fun RestorativeHistoryRow(row: RestorativeSleepRow) {
    val cosmos = LocalCosmos.current
    val deltaColor =
        when {
            row.deltaToPrevDay == null -> cosmos.textSecondary
            row.deltaToPrevDay > 0.5 -> LocalCosmos.current.ok
            row.deltaToPrevDay < -0.5 -> LocalCosmos.current.crit
            else -> LocalCosmos.current.accent
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.date.format(RESTORATIVE_SHEET_DATE_FMT),
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%.1f".format(row.percent).replace('.', ',') + " %",
            style = MaterialTheme.typography.bodyMedium,
            color = restorativeSleepBarColor(row.percent),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = row.deltaToPrevDay?.let { "%+.1f %%".format(it) } ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = deltaColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
