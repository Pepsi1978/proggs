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
 * Ruhepuls-Verlauf im HRV-Pattern (Frank-Wunsch 2026-06-21).
 *
 * Zeigt den Ruhepuls der letzten ~30 Tage als Balken-Graph. Tap auf die Karte oeffnet den
 * bestehenden RHR-Detail-Screen (1:1 wie bisher). Unter dem Graphen wird der Durchschnitt
 * ueber ALLE jemals gespeicherten Ruhepuls-Werte angezeigt — nicht nur der letzten 30 Tage.
 *
 * Farb-Logik (absolute Schwellen):
 * - bis einschliesslich 58 bpm -> Gruen
 * - ueber 58 bis 62 bpm        -> Gelb
 * - ueber 62 bpm               -> Rot
 *
 * Farbtoene 1:1 identisch zum HRV-Pattern:
 * [CosmosColors.WhoopRecoveryGreen] / [CosmosColors.WhoopRecoveryYellow] /
 * [CosmosColors.WhoopRecoveryRed].
 */
@Composable
internal fun RhrGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.accent

    val derived =
        remember(selectedSnapshot, history) {
            rhrDerived(selectedSnapshot = selectedSnapshot, history = history)
        }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Ruhepuls",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Ruheherzfrequenz",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                val headerColor =
                    derived.currentBpm?.let { current ->
                        rhrBarColor(value = current.toDouble())
                    } ?: accent
                Text(
                    text = derived.currentBpm?.let { "$it bpm" } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            RhrBars(values = derived.last30Bpm)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        "Durchschnitt: ${derived.avgAllBpm?.let { "%.1f".format(it).replace('.', ',') + " bpm" } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (derived.deltaVsAvg != null) {
                    RhrTrendBadgeBpm(delta = derived.deltaVsAvg)
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

private data class RhrDerived(
    val currentBpm: Int?,
    val avgAllBpm: Double?,
    val deltaVsAvg: Double?,
    val last30Bpm: List<Double>,
)

private fun rhrDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
): RhrDerived {
    val zone = ZoneId.systemDefault()
    val all =
        history
            .mapNotNull { snap ->
                val rhr = snap.restingHeartRate ?: return@mapNotNull null
                val date = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
                date to rhr.toDouble()
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.average() }
            .toSortedMap()
            .map { (_, rhr) -> rhr }

    val current = selectedSnapshot?.restingHeartRate ?: all.lastOrNull()?.toInt()
    val last30 = all.takeLast(30)
    val avgAll = all.takeIf { it.isNotEmpty() }?.average()
    val delta = if (current != null && avgAll != null) current.toDouble() - avgAll else null

    return RhrDerived(
        currentBpm = current,
        avgAllBpm = avgAll,
        deltaVsAvg = delta,
        last30Bpm = last30,
    )
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun RhrBars(values: List<Double>) {
    val yMin = remember(values) { (values.minOrNull() ?: 0.0) - 5.0 }
    val yMax = remember(values) { values.maxOrNull() ?: 1.0 }
    MiniBarsCanvas(
        values = values,
        barColor = { rhrBarColor(it) },
        yMin = yMin,
        yMax = yMax,
        emptyText = "Noch keine Ruhepuls-Daten",
    )
}

private fun rhrBarColor(value: Double): Color {
    return when {
        value > 62.0 -> CosmosColors.WhoopRecoveryRed
        value > 58.0 -> CosmosColors.WhoopRecoveryYellow
        else -> CosmosColors.WhoopRecoveryGreen
    }
}

/**
 * Trend-Badge fuer Ruhepuls — Minus (unter Durchschnitt) = Gruen (besser),
 * Plus (ueber Durchschnitt) = Rot (schlechter), innerhalb +/- 0.5 bpm = Accent (neutral).
 * 1:1 die gleiche Logik + Hintergrundfarben wie [HrvTrendBadgeMs], nur invertiert.
 */
@Composable
private fun RhrTrendBadgeBpm(delta: Double) {
    val color =
        when {
            delta < -0.5 -> LocalCosmos.current.ok      // Gruen (niedriger = besser)
            delta > 0.5 -> LocalCosmos.current.crit      // Rot (hoeher = schlechter)
            else -> LocalCosmos.current.accent            // Neutral
        }
    Box(
        modifier =
            Modifier.clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.18f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "%+.1f bpm".format(delta).replace('.', ','),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
