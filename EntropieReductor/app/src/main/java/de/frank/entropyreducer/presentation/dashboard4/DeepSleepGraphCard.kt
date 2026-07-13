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
import de.frank.entropyreducer.presentation.components.charts.SleepStageColors
import de.frank.entropyreducer.presentation.components.rememberCardColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import de.frank.entropyreducer.presentation.theme.CosmosColors

/**
 * Tiefschlaf-Verlaufs-Pattern (Frank-Wunsch 2026-05-13).
 *
 * Zeigt den prozentualen Tiefschlafanteil der letzten ~30 Tage als Balken-Graph. Tap auf den
 * Graphen oeffnet eine Bottom-Sheet mit ALLEN historischen Tiefschlaf- werten und der Abweichung
 * zum jeweiligen Vortag. Unter dem Graphen werden — wie bei allen anderen Karten — der
 * 30-Tage-Durchschnitt UND die aktuelle Abweichung zum Durchschnitt angezeigt.
 *
 * Prozent = Tiefschlafminuten / (REM + Tief + Leicht + Wach) * 100
 *
 * Die Farbgebung folgt SleepStageColors.Deep — damit Graph, Bar-Segment und Beschreibungs-Chip oben
 * in der Schlafphasen-Card visuell zusammengehoeren.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeepSleepGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    precomputed: DeepSleepDerived? = null,
    onClick: () -> Unit = {},
) {
    val cosmos = LocalCosmos.current
    val accent = SleepStageColors.Deep

    // Performance-Fix 2026-07-03 (#47449): Schwerarbeit haengt nur an history — beim
    // Chart-Scrubbing (selectedSnapshot aendert sich pro Move-Event) lief die komplette
    // groupBy-Pipeline sonst in JEDEM Frame neu.
    val heavy = if (precomputed == null) remember(history) { deepSleepHeavy(history) } else null
    val derived =
        precomputed
            ?: remember(selectedSnapshot, heavy) {
                deepSleepDerived(selectedSnapshot = selectedSnapshot, heavy = heavy!!)
            }

    // Frank-Wunsch 2026-05-17: Header-Zahl bekommt die gleiche Ampel-Farbe wie
    // der aktuelle Tagesbalken (rot/gelb/gruen). Einheitlich erkennbar.
    val headerColor = derived.currentPercent?.let { deepSleepBarColor(it) } ?: accent

    var sheetOpen by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            // Kopfzeile: Titel + grosser aktueller Prozent-Wert.
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tiefschlaf-Verlauf",
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
                    text =
                        derived.currentPercent?.let { "%.1f".format(it).replace('.', ',') + " %" }
                            ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            // Balken-Graph der letzten 30 Tage — pro Balken eine Ampel-Farbe.
            DeepSleepBars(values = derived.last30Percent)

            Spacer(Modifier.height(10.dp))

            // Footer: 30-Tage-Schnitt + Abweichung zum Schnitt (wie andere Karten).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        "30-Tage-Schnitt: ${derived.avg30Percent?.let { "%.1f".format(it).replace('.', ',') + " %" } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (derived.deltaVsAvg != null) {
                    TrendBadgePercent(delta = derived.deltaVsAvg)
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
            // Frank-Wunsch 2026-05-18 Folgeauftrag: Farbpalette oben im Sheet,
            // Auswahl persistiert auf SLEEP_DEEP_GRAPH.
            val cardColors = rememberCardColors()
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ColorPaletteBar(
                    selectedIndex = cardColors.colorFor(BiomarkerCardId.SLEEP_DEEP_GRAPH, cosmos.isDark),
                    onPick = { idx -> cardColors.setColor(BiomarkerCardId.SLEEP_DEEP_GRAPH, idx, cosmos.isDark) },
                )
                Spacer(Modifier.height(12.dp))
                // Interaktiver Linien-Verlauf wie beim HRV-Verlauf (Frank-Wunsch
                // 2026-05-23): Werte + Durchschnitts-/Trendlinie, Tap zeigt Tooltip.
                // Tiefschlaf: mehr ist besser -> lowerIsBetter = false.
                InteractiveLineChart(
                    points = derived.chartPoints,
                    accent = SleepStageColors.Deep,
                    unit = "%",
                    height = 200,
                    valueFormatter = { "%.1f".format(it).replace('.', ',') + " %" },
                    lowerIsBetter = false,
                )
                Spacer(Modifier.height(16.dp))
            }
            DeepSleepHistorySheetContent(rows = derived.historyRows)
        }
    }
}

/* ------------------------- Datenaufbereitung ------------------------- */

@androidx.compose.runtime.Immutable
data class DeepSleepDerived(
    val currentPercent: Double?,
    val avg30Percent: Double?,
    val deltaVsAvg: Double?,
    val last30Percent: List<Double>,
    val historyRows: List<DeepSleepRow>,
    /**
     * Alle Naechte als (epochMs, Prozent) fuer den interaktiven Linien-Chart im Detail-Sheet
     * (Frank-Wunsch 2026-05-23, analog zum HRV-Verlauf).
     */
    val chartPoints: List<Pair<Long, Double>>,
)

data class DeepSleepRow(val date: LocalDate, val percent: Double, val deltaToPrevDay: Double?)

/** History-abhaengige Schwerarbeit — aendert sich beim Scrubbing NICHT, daher separat memoiziert. */
internal data class DeepSleepHeavy(
    val all: List<Pair<LocalDate, Double>>,
    val last30: List<Double>,
    val avg30: Double?,
    val rows: List<DeepSleepRow>,
    val chartPoints: List<Pair<Long, Double>>,
)

internal fun deepSleepHeavy(history: List<BiomarkerSnapshotEntity>): DeepSleepHeavy {
    // Historie in chronologischer Reihenfolge ASC mit gueltigen Werten.
    val zone = ZoneId.systemDefault()
    val all =
        history
            .mapNotNull { snap ->
                val pct = snap.percent() ?: return@mapNotNull null
                val date = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
                date to pct
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.average() }
            .toSortedMap()
            .map { (date, pct) -> date to pct }
    val last30 = all.takeLast(30).map { it.second }
    // Historie als Zeilen mit Delta zum jeweiligen Vortag — neuester Eintrag oben.
    val rows = mutableListOf<DeepSleepRow>()
    all.forEachIndexed { idx, (date, pct) ->
        val prev = if (idx > 0) all[idx - 1].second else null
        val deltaPrev = if (prev != null) pct - prev else null
        rows += DeepSleepRow(date = date, percent = pct, deltaToPrevDay = deltaPrev)
    }
    return DeepSleepHeavy(
        all = all,
        last30 = last30,
        avg30 = if (last30.size >= 3) last30.average() else null,
        rows = rows.reversed(),
        chartPoints = all.map { (date, pct) ->
            date.atStartOfDay(zone).toInstant().toEpochMilli() to pct
        },
    )
}

internal fun deepSleepDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    heavy: DeepSleepHeavy,
): DeepSleepDerived {
    val current = selectedSnapshot?.percent() ?: heavy.all.lastOrNull()?.second
    val delta = if (current != null && heavy.avg30 != null) current - heavy.avg30 else null
    return DeepSleepDerived(
        currentPercent = current,
        avg30Percent = heavy.avg30,
        deltaVsAvg = delta,
        last30Percent = heavy.last30,
        historyRows = heavy.rows,
        chartPoints = heavy.chartPoints,
    )
}

/** Kompatibilitaets-Wrapper (BiomarkerViewModel berechnet off-UI vor) — Verhalten identisch. */
internal fun deepSleepDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
): DeepSleepDerived = deepSleepDerived(selectedSnapshot, deepSleepHeavy(history))

private fun BiomarkerSnapshotEntity.percent(): Double? {
    val deep = sleepDeepMinutes ?: return null
    val rem = sleepRemMinutes ?: 0
    val light = sleepLightMinutes ?: 0
    val awake = sleepAwakeMinutes ?: 0
    val total = deep + rem + light + awake
    if (total <= 0) return null
    return deep.toDouble() / total.toDouble() * 100.0
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun DeepSleepBars(values: List<Double>) {
    val yMin = remember(values) { (values.minOrNull() ?: 0.0) - 5.0 }
    val yMax = remember(values) { values.maxOrNull() ?: 30.0 }
    MiniBarsCanvas(
        values = values,
        barColor = { deepSleepBarColor(it) },
        yMin = yMin,
        yMax = yMax,
        emptyText = "Noch keine Tiefschlaf-Daten",
    )
}

/**
 * Farbe pro Balken im Tiefschlaf-Graph (Frank-Wunsch 2026-05-13): 0 – 10 % → Rot (kritisch wenig)
 * 10 – 20 % → Gelb (grenzwertig) 20 – 30 % → Gruen (gesund)
 * > > 30 % → Gruen (extra viel)
 *
 * Frank-Wunsch 2026-06-21: Gleiche WHOOP-Farbpalette wie im Erholungsverlauf
 * (RecoveryGraphCard) — vorher dezente CosmosColors.Success/Warning/Critical,
 * jetzt knallig WhoopRecoveryGreen/Yellow/Red fuer visuelle Konsistenz.
 */
private fun deepSleepBarColor(pct: Double): Color =
    when {
        pct < 10.0 -> CosmosColors.WhoopRecoveryRed
        pct < 20.0 -> CosmosColors.WhoopRecoveryYellow
        else -> CosmosColors.WhoopRecoveryGreen
    }

@Composable
private fun TrendBadgePercent(delta: Double) {
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

private val SHEET_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", Locale.GERMANY)

@Composable
private fun DeepSleepHistorySheetContent(rows: List<DeepSleepRow>) {
    val cosmos = LocalCosmos.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Tiefschlaf-Historie",
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
            items(rows, key = { it.date.toEpochDay() }) { row -> DeepSleepHistoryRow(row = row) }
        }
    }
}

@Composable
private fun DeepSleepHistoryRow(row: DeepSleepRow) {
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
            text = row.date.format(SHEET_DATE_FMT),
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%.1f".format(row.percent).replace('.', ',') + " %",
            style = MaterialTheme.typography.bodyMedium,
            color = SleepStageColors.Deep,
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
