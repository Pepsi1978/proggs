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
import de.frank.entropyreducer.presentation.components.charts.MiniBarsCanvas
import de.frank.entropyreducer.presentation.components.rememberCardColors
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.whoopRecoveryColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Erholungsverlauf-Pattern (Frank-Wunsch 2026-05-16).
 *
 * Zeigt den Recovery-Score (0-100) der letzten ~30 Tage als Balken-Graph. Tap auf den Graphen
 * oeffnet eine Bottom-Sheet mit ALLEN historischen Erholungswerten und der Abweichung zum
 * jeweiligen Vortag. Unter dem Graphen werden — wie bei allen anderen Karten — der
 * 30-Tage-Durchschnitt UND die aktuelle Abweichung zum Durchschnitt angezeigt.
 *
 * Pattern uebernommen 1:1 vom Tiefschlaf-Verlauf (DeepSleepGraphCard) — nur die Berechnung +
 * Ampel-Schwellen wurden gegen die Recovery-Doktrin getauscht:
 * - 80-100 % → Gruen (Success)
 * - 60-80 % → Gelb (Warning)
 * - unter 60 % → Rot (Critical)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecoveryGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    // Poka-Yoke 2026-08-07: KEIN Default — sonst kompiliert ein Aufrufer, der den Tap
    // vergisst, klaglos durch und die Karte reagiert stumm nicht (genau der Fehler,
    // den Frank an Erholungs-/Tiefschlaf-/REM-/Wachzeit-/Erholsamer-Schlaf-Verlauf sah).
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.ok

    // Performance-Fix 2026-07-03 (#47449): Schwerarbeit haengt nur an history — beim
    // Chart-Scrubbing (selectedSnapshot aendert sich pro Move-Event) lief die komplette
    // groupBy-Pipeline sonst in JEDEM Frame neu.
    val heavy = remember(history) { recoveryHeavy(history) }
    val derived =
        remember(selectedSnapshot, heavy) {
            recoveryDerived(selectedSnapshot = selectedSnapshot, heavy = heavy)
        }

    var sheetOpen by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Erholungsverlauf",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Recovery-Score pro Tag",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                Text(
                    text = derived.currentPercent?.let { "%.0f %%".format(it) } ?: "—",
                    color = derived.currentPercent?.let { recoveryBarColor(it) } ?: accent,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            RecoveryBars(values = derived.last30Percent)

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
                    RecoveryTrendBadgePercent(delta = derived.deltaVsAvg)
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
            // Auswahl persistiert auf RECOVERY_GRAPH.
            val cardColors = rememberCardColors()
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ColorPaletteBar(
                    selectedIndex = cardColors.colorFor(BiomarkerCardId.RECOVERY_GRAPH, cosmos.isDark),
                    onPick = { idx -> cardColors.setColor(BiomarkerCardId.RECOVERY_GRAPH, idx, cosmos.isDark) },
                )
                Spacer(Modifier.height(12.dp))
            }
            RecoveryHistorySheetContent(rows = derived.historyRows)
        }
    }
}

/* ------------------------- Datenaufbereitung ------------------------- */

private data class RecoveryDerived(
    val currentPercent: Double?,
    val avg30Percent: Double?,
    val deltaVsAvg: Double?,
    val last30Percent: List<Double>,
    val historyRows: List<RecoveryRow>,
)

internal data class RecoveryRow(
    val date: LocalDate,
    val percent: Double,
    val deltaToPrevDay: Double?,
)

/** History-abhaengige Schwerarbeit — aendert sich beim Scrubbing NICHT, daher separat memoiziert. */
private data class RecoveryHeavy(
    val all: List<Pair<java.time.LocalDate, Double>>,
    val last30: List<Double>,
    val avg30: Double?,
    val rows: List<RecoveryRow>,
)

private fun recoveryHeavy(history: List<BiomarkerSnapshotEntity>): RecoveryHeavy {
    val zone = ZoneId.systemDefault()
    val all =
        history
            .mapNotNull { snap ->
                val pct = snap.recoveryScore?.toDouble() ?: return@mapNotNull null
                val date = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
                date to pct
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.average() }
            .toSortedMap()
            .map { (date, pct) -> date to pct }
    val last30 = all.takeLast(30).map { it.second }
    val rows = mutableListOf<RecoveryRow>()
    all.forEachIndexed { idx, (date, pct) ->
        val prev = if (idx > 0) all[idx - 1].second else null
        val deltaPrev = if (prev != null) pct - prev else null
        rows += RecoveryRow(date = date, percent = pct, deltaToPrevDay = deltaPrev)
    }
    return RecoveryHeavy(
        all = all,
        last30 = last30,
        avg30 = if (last30.size >= 3) last30.average() else null,
        // Neuester Eintrag oben — reversed schon hier, damit das Finalize pro Scrub-Frame
        // keine neue Liste alloziert.
        rows = rows.reversed(),
    )
}

private fun recoveryDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    heavy: RecoveryHeavy,
): RecoveryDerived {
    val all = heavy.all
    val last30 = heavy.last30
    val avg30 = heavy.avg30
    val current = selectedSnapshot?.recoveryScore?.toDouble() ?: all.lastOrNull()?.second
    val delta = if (current != null && avg30 != null) current - avg30 else null

    return RecoveryDerived(
        currentPercent = current,
        avg30Percent = avg30,
        deltaVsAvg = delta,
        last30Percent = last30,
        historyRows = heavy.rows,
    )
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun RecoveryBars(values: List<Double>) {
    val yMax = 100.0
    val yMin = 0.0
    MiniBarsCanvas(
        values = values,
        barColor = { recoveryBarColor(it) },
        yMin = yMin,
        yMax = yMax,
        emptyText = "Noch keine Recovery-Daten",
    )
}

/**
 * Frank-Wunsch 2026-06-01: offizielle WHOOP-Recovery-Ampel (vorher Doktrin-Schwellen 80/60).
 * Delegiert an die zentrale whoopRecoveryColor() — eine Quelle der Wahrheit fuer alle
 * Recovery-Faerbungen:
 * - 67-100 % → Gruen
 * - 34-66 % → Gelb
 * - 0-33 % → Rot
 */
private fun recoveryBarColor(pct: Double): Color = whoopRecoveryColor(pct)

@Composable
private fun RecoveryTrendBadgePercent(delta: Double) {
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

private val RECOVERY_SHEET_DATE_FMT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE dd.MM.yyyy", Locale.GERMANY)

@Composable
private fun RecoveryHistorySheetContent(rows: List<RecoveryRow>) {
    val cosmos = LocalCosmos.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "Erholungs-Historie",
            style = MaterialTheme.typography.titleLarge,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Recovery-Score pro Tag · Abweichung zum Vortag",
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
            items(rows, key = { it.date.toEpochDay() }) { row -> RecoveryHistoryRow(row = row) }
        }
    }
}

@Composable
private fun RecoveryHistoryRow(row: RecoveryRow) {
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
            text = row.date.format(RECOVERY_SHEET_DATE_FMT),
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%.0f %%".format(row.percent),
            style = MaterialTheme.typography.bodyMedium,
            color = recoveryBarColor(row.percent),
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
