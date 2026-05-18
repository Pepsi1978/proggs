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
import de.frank.entropyreducer.presentation.components.rememberCardColors
import de.frank.entropyreducer.presentation.components.charts.SleepStageColors
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Wachzeit-Verlaufs-Pattern (Frank-Wunsch 2026-05-17).
 *
 * Schwester-Karte zu [DeepSleepGraphCard]: zeigt den prozentualen Wachzeit-Anteil
 * der letzten ~30 Tage als Balken-Graph. Tap auf den Graphen oeffnet eine Bottom-
 * Sheet mit ALLEN historischen Wachzeit-Werten und der Abweichung zum jeweiligen
 * Vortag. Unter dem Graphen werden — wie bei allen anderen Karten — der 30-Tage-
 * Durchschnitt UND die aktuelle Abweichung zum Durchschnitt angezeigt.
 *
 * Prozent = Wachminuten / (REM + Tief + Leicht + Wach) * 100
 *
 * Die Farbgebung folgt SleepStageColors.Awake — damit Graph, Bar-Segment und
 * Beschreibungs-Chip oben in der Schlafphasen-Card visuell zusammengehoeren.
 *
 * Ampel-Logik (Frank-Vorgabe 2026-05-17, INVERS zur Tiefschlaf-Ampel — wenig Wachzeit
 * ist GUT, viel Wachzeit ist SCHLECHT):
 *   - 0–5 %  Wachzeit → Gruen (ungestoerter Schlaf)
 *   - 5–10 % Wachzeit → Gelb (grenzwertig)
 *   - > 10 % Wachzeit → Rot  (haeufiges Aufwachen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WakeTimeGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
) {
    val cosmos = LocalCosmos.current
    val accent = SleepStageColors.Awake

    val derived =
        remember(selectedSnapshot, history) {
            wakeTimeDerived(selectedSnapshot = selectedSnapshot, history = history)
        }

    // Frank-Wunsch 2026-05-17: Header-Zahl bekommt die gleiche Ampel-Farbe wie
    // der aktuelle Tagesbalken (gruen/gelb/rot). Einheitlich erkennbar.
    val headerColor = derived.currentPercent?.let { wakeTimeBarColor(it) } ?: accent

    var sheetOpen by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { sheetOpen = true }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wachzeit-Verlauf",
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
                    text = derived.currentPercent?.let { "%.0f %%".format(it) } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            WakeTimeBars(values = derived.last30Percent)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        "30-Tage-Schnitt: ${derived.avg30Percent?.let { "%.0f %%".format(it) } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (derived.deltaVsAvg != null) {
                    WakeTimeTrendBadgePercent(delta = derived.deltaVsAvg)
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
            // Auswahl persistiert auf SLEEP_WAKE_GRAPH.
            val cardColors = rememberCardColors()
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ColorPaletteBar(
                    selectedIndex = cardColors.colors[BiomarkerCardId.SLEEP_WAKE_GRAPH] ?: 0,
                    onPick = { idx ->
                        cardColors.setColor(BiomarkerCardId.SLEEP_WAKE_GRAPH, idx)
                    },
                )
                Spacer(Modifier.height(12.dp))
            }
            WakeTimeHistorySheetContent(rows = derived.historyRows)
        }
    }
}

/* ------------------------- Datenaufbereitung ------------------------- */

private data class WakeTimeDerived(
    val currentPercent: Double?,
    val avg30Percent: Double?,
    val deltaVsAvg: Double?,
    val last30Percent: List<Double>,
    val historyRows: List<WakeTimeRow>,
)

internal data class WakeTimeRow(
    val date: LocalDate,
    val percent: Double,
    val deltaToPrevDay: Double?,
)

private fun wakeTimeDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
): WakeTimeDerived {
    val zone = ZoneId.systemDefault()
    val all =
        history
            .mapNotNull { snap ->
                val pct = snap.wakePercent() ?: return@mapNotNull null
                val date = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
                date to pct
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.average() }
            .toSortedMap()
            .map { (date, pct) -> date to pct }

    val current = selectedSnapshot?.wakePercent() ?: all.lastOrNull()?.second
    val last30 = all.takeLast(30).map { it.second }
    val avg30 = if (last30.size >= 3) last30.average() else null
    val delta = if (current != null && avg30 != null) current - avg30 else null

    val rows = mutableListOf<WakeTimeRow>()
    all.forEachIndexed { idx, (date, pct) ->
        val prev = if (idx > 0) all[idx - 1].second else null
        val deltaPrev = if (prev != null) pct - prev else null
        rows += WakeTimeRow(date = date, percent = pct, deltaToPrevDay = deltaPrev)
    }
    return WakeTimeDerived(
        currentPercent = current,
        avg30Percent = avg30,
        deltaVsAvg = delta,
        last30Percent = last30,
        historyRows = rows.reversed(),
    )
}

private fun BiomarkerSnapshotEntity.wakePercent(): Double? {
    val awake = sleepAwakeMinutes ?: return null
    val deep = sleepDeepMinutes ?: 0
    val rem = sleepRemMinutes ?: 0
    val light = sleepLightMinutes ?: 0
    val total = deep + rem + light + awake
    if (total <= 0) return null
    return awake.toDouble() / total.toDouble() * 100.0
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun WakeTimeBars(values: List<Double>) {
    val cosmos = LocalCosmos.current
    // Y-Achse auf 20 % skalieren — Wachzeit-Anteile liegen typischerweise 1-12 %,
    // selten ueber 15 %. Mit 20 % als Max sind selbst extreme Naechte sichtbar
    // ohne dass die Norm-Werte zu winzigen Strichen werden.
    val yMax = 20.0
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(cosmos.glassBg)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (values.isEmpty()) {
                Text(
                    text = "Noch keine Wachzeit-Daten",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                values.forEach { pct ->
                    val ratio = (pct / yMax).coerceIn(0.05, 1.0).toFloat()
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight(ratio)
                                .clip(RoundedCornerShape(2.dp))
                                .background(wakeTimeBarColor(pct))
                    )
                }
            }
        }
    }
}

/**
 * Farbe pro Balken im Wachzeit-Graph (Frank-Vorgabe 2026-05-17, INVERS zur
 * Tiefschlaf-Ampel: wenig Wachzeit = gut):
 *   0 – 5 %  → Gruen  (ungestoerter Schlaf)
 *   5 – 10 % → Gelb   (grenzwertig)
 *   > 10 %   → Rot    (haeufiges Aufwachen)
 */
private fun wakeTimeBarColor(pct: Double): Color =
    when {
        pct < 5.0 -> CosmosColors.Success
        pct < 10.0 -> CosmosColors.Warning
        else -> CosmosColors.Critical
    }

@Composable
private fun WakeTimeTrendBadgePercent(delta: Double) {
    // INVERS zur Tiefschlaf-Trend-Logik: positive Delta (mehr Wachzeit als
    // Schnitt) ist SCHLECHT (rot), negative Delta (weniger Wachzeit als
    // Schnitt) ist GUT (gruen).
    val color =
        when {
            delta > 0.5 -> CosmosColors.Critical
            delta < -0.5 -> CosmosColors.Success
            else -> CosmosColors.AccentPrimary
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

private val WAKE_SHEET_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", Locale.GERMANY)

@Composable
private fun WakeTimeHistorySheetContent(rows: List<WakeTimeRow>) {
    val cosmos = LocalCosmos.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Wachzeit-Historie",
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
            items(rows, key = { it.date.toEpochDay() }) { row -> WakeTimeHistoryRow(row = row) }
        }
    }
}

@Composable
private fun WakeTimeHistoryRow(row: WakeTimeRow) {
    val cosmos = LocalCosmos.current
    // INVERS zur Tiefschlaf-Delta-Logik: positive Delta (mehr Wachzeit als Vortag)
    // ist SCHLECHT (rot), negative Delta (weniger Wachzeit) ist GUT (gruen).
    val deltaColor =
        when {
            row.deltaToPrevDay == null -> cosmos.textSecondary
            row.deltaToPrevDay > 0.5 -> CosmosColors.Critical
            row.deltaToPrevDay < -0.5 -> CosmosColors.Success
            else -> CosmosColors.AccentPrimary
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.date.format(WAKE_SHEET_DATE_FMT),
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%.0f %%".format(row.percent),
            style = MaterialTheme.typography.bodyMedium,
            color = SleepStageColors.Awake,
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
