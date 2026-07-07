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
 * Schlafzeit-Verlaufs-Pattern (Frank-Wunsch 2026-06-21).
 *
 * Zeigt die effektive Schlafzeit (ohne Wachzeit) der letzten ~30 Tage als Balken-Graph.
 * Tap auf die Karte oeffnet den bestehenden Schlafzeit-Detail-Screen.
 *
 * Farb-Logik (relativ zu festen Schwellen):
 * - ab 8,5 Stunden (510 min)     -> Gruen (guter Schlaf)
 * - 7 bis 8,5 Stunden (420-510)  -> Gelb (grenzwertig)
 * - unter 7 Stunden (420 min)    -> Rot (zu wenig Schlaf)
 */
@Composable
internal fun SleepTotalGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.accent

    val derived =
        remember(selectedSnapshot, history) {
            sleepTotalDerived(selectedSnapshot = selectedSnapshot, history = history)
        }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Schlafzeit",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Effektive Schlafzeit (ohne Wachzeit)",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                val headerColor =
                    derived.currentMinutes?.let { sleepTotalBarColor(it) } ?: accent
                Text(
                    text = derived.currentMinutes?.let { formatSleepTime(it) } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            SleepTotalBars(values = derived.last30Minutes)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        "Durchschnitt: ${derived.avgAllMinutes?.let { formatSleepTime(it) } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (derived.deltaVsAvg != null) {
                    SleepTotalTrendBadge(delta = derived.deltaVsAvg)
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

private data class SleepTotalDerived(
    val currentMinutes: Int?,
    val avgAllMinutes: Double?,
    val deltaVsAvg: Double?,
    val last30Minutes: List<Double>,
)

private fun sleepTotalDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
): SleepTotalDerived {
    val zone = ZoneId.systemDefault()
    val all =
        history
            .mapNotNull { snap ->
                val total = snap.sleepTotalMinutes ?: return@mapNotNull null
                val awake = snap.sleepAwakeMinutes ?: 0
                val effective = (total - awake).coerceAtLeast(0)
                if (effective <= 0) return@mapNotNull null
                val date = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
                date to effective.toDouble()
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.average() }
            .toSortedMap()
            .map { (_, minutes) -> minutes }

    val current = selectedSnapshot?.let { snap ->
        val total = snap.sleepTotalMinutes ?: return@let null
        val awake = snap.sleepAwakeMinutes ?: 0
        (total - awake).coerceAtLeast(0).takeIf { it > 0 }
    } ?: all.lastOrNull()?.toInt()
    val last30 = all.takeLast(30)
    val avgAll = all.takeIf { it.isNotEmpty() }?.average()
    val delta = if (current != null && avgAll != null) current.toDouble() - avgAll else null

    return SleepTotalDerived(
        currentMinutes = current,
        avgAllMinutes = avgAll,
        deltaVsAvg = delta,
        last30Minutes = last30,
    )
}

/* ------------------------- UI-Bausteine ------------------------- */

private fun formatSleepTime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return "${h}h ${m.toString().padStart(2, '0')}min"
}

private fun formatSleepTime(minutes: Double): String {
    return formatSleepTime(minutes.toInt())
}

@Composable
private fun SleepTotalBars(values: List<Double>) {
    val yMin = remember(values) { (values.minOrNull() ?: 0.0) - 60.0 }
    val yMax = remember(values) { values.maxOrNull() ?: 510.0 }
    MiniBarsCanvas(
        values = values,
        barColor = { sleepTotalBarColor(it.toInt()) },
        yMin = yMin,
        yMax = yMax,
        emptyText = "Noch keine Schlafzeit-Daten",
    )
}

/**
 * Farbe pro Balken:
 * - ueber 8h (480 min)   -> Gruen
 * - 7h-8h (420-480)      -> Gelb
 * - unter 7h (420)       -> Rot
 */
private fun sleepTotalBarColor(minutes: Int): Color =
    when {
        minutes >= 480 -> CosmosColors.WhoopRecoveryGreen
        minutes >= 420 -> CosmosColors.WhoopRecoveryYellow
        else -> CosmosColors.WhoopRecoveryRed
    }

/**
 * Trend-Badge: Plus (ueber Durchschnitt) = Gruen (besser),
 * Minus (unter Durchschnitt) = Rot (schlechter).
 */
@Composable
private fun SleepTotalTrendBadge(delta: Double) {
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
        val totalDelta = delta.toInt()
        val h = totalDelta / 60
        val m = kotlin.math.abs(totalDelta % 60)
        val sign = if (delta >= 0) "+" else "-"
        Text(
            text = "${sign}${kotlin.math.abs(h)}h ${m.toString().padStart(2, '0')}min",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
