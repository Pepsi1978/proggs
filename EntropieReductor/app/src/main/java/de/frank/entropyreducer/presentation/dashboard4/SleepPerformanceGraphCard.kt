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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.data.local.entities.BiomarkerSnapshotEntity
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.charts.MiniBarsCanvas
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.ZoneId

/**
 * Schlaf-Performance-Verlaufs-Pattern (Frank-Wunsch 2026-06-21).
 *
 * Zeigt die Schlaf-Performance (%) der letzten ~30 Tage als Balken-Graph.
 * Tap auf die Karte oeffnet den bestehenden Performance-Detail-Screen.
 *
 * Farb-Logik (relativ zum persoenlichen Durchschnitt):
 * - ueber dem Durchschnitt             -> Gruen (besser als Schnitt)
 * - bis 10% unter dem Durchschnitt     -> Gelb (grenzwertig)
 * - mehr als 10% unter dem Durchschnitt -> Rot (schlecht)
 */
@Composable
internal fun SleepPerformanceGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.accent

    val derived =
        remember(selectedSnapshot, history) {
            sleepPerformanceDerived(selectedSnapshot = selectedSnapshot, history = history)
        }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Schlaf Performance",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Schlafqualitaet pro Nacht",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                val headerColor =
                    derived.currentPercent?.let {
                        sleepPerfBarColor(it, derived.avgAllPercent)
                    } ?: accent
                Text(
                    text = derived.currentPercent?.let { "%.1f %%".format(it) } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            SleepPerfBars(
                values = derived.last30Percent,
                avg = derived.avgAllPercent,
            )

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        "Durchschnitt: ${derived.avgAllPercent?.let { "%.1f".format(it).replace('.', ',') + " %" } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (derived.deltaVsAvg != null) {
                    SleepPerfTrendBadge(delta = derived.deltaVsAvg)
                }
            }
            Text(
                text = "Tippen fuer komplette Historie",
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary.copy(alpha = 0.6f),
            )
        }
    }
}

/* ------------------------- Datenaufbereitung ------------------------- */

private data class SleepPerfDerived(
    val currentPercent: Double?,
    val avgAllPercent: Double?,
    val deltaVsAvg: Double?,
    val last30Percent: List<Double>,
)

private fun sleepPerformanceDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
): SleepPerfDerived {
    val zone = ZoneId.systemDefault()
    val all =
        history
            .mapNotNull { snap ->
                val pct = snap.sleepPerformance?.toDouble() ?: return@mapNotNull null
                val date = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
                date to pct
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.average() }
            .toSortedMap()
            .map { (_, pct) -> pct }

    val current = selectedSnapshot?.sleepPerformance?.toDouble() ?: all.lastOrNull()
    val last30 = all.takeLast(30)
    val avgAll = all.takeIf { it.isNotEmpty() }?.average()
    val delta = if (current != null && avgAll != null) current - avgAll else null

    return SleepPerfDerived(
        currentPercent = current,
        avgAllPercent = avgAll,
        deltaVsAvg = delta,
        last30Percent = last30,
    )
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun SleepPerfBars(values: List<Double>, avg: Double?) {
    val yMin = remember(values) { (values.minOrNull() ?: 0.0) - 3.0 }
    val yMax = remember(values) { values.maxOrNull() ?: 100.0 }
    MiniBarsCanvas(
        values = values,
        barColor = { sleepPerfBarColor(it, avg) },
        yMin = yMin,
        yMax = yMax,
        emptyText = "Noch keine Performance-Daten",
    )
}

/**
 * Farbe pro Balken relativ zum persoenlichen Durchschnitt:
 * - ueber Durchschnitt              -> Gruen
 * - bis 10% unter Durchschnitt      -> Gelb
 * - mehr als 10% unter Durchschnitt -> Rot
 */
private fun sleepPerfBarColor(pct: Double, avg: Double?): Color {
    val avgSafe = avg ?: return CosmosColors.WhoopRecoveryYellow
    return when {
        pct >= avgSafe -> CosmosColors.WhoopRecoveryGreen
        pct >= avgSafe - 10.0 -> CosmosColors.WhoopRecoveryYellow
        else -> CosmosColors.WhoopRecoveryRed
    }
}

/**
 * Trend-Badge: Plus (ueber Durchschnitt) = Gruen, Minus (unter Durchschnitt) = Rot.
 */
@Composable
private fun SleepPerfTrendBadge(delta: Double) {
    val color =
        when {
            delta > 0.05 -> LocalCosmos.current.ok
            delta < -0.05 -> LocalCosmos.current.crit
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
