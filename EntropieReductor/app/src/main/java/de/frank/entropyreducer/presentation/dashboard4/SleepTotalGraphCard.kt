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
 * Schlafzeit-Verlaufs-Pattern (Frank-Wunsch 2026-06-21, gross gezogen 2026-08-07).
 *
 * Zeigt die effektive Schlafzeit (ohne Wachzeit) der letzten ~30 Tage als Balken-Graph. Aufbau
 * 1:1 wie der Erholungsverlauf ([RecoveryGraphCard]): Kopfzeile mit aktuellem Wert, Balken-Graph
 * ueber die volle Breite, darunter 30-Tage-Schnitt + Abweichungs-Badge. Tap auf die Karte oeffnet
 * den bestehenden Schlafzeit-Detail-Screen.
 *
 * Ampel-Schwellen (Frank-Vorgabe 2026-08-07):
 * - ab 8 Stunden (480 min)          -> Gruen
 * - 6,5 bis 8 Stunden (390-480 min) -> Gelb
 * - unter 6,5 Stunden (390 min)     -> Rot
 */
@Composable
internal fun SleepTotalGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.accent

    // Performance-Fix wie in RecoveryGraphCard (#47449): Schwerarbeit haengt nur an history —
    // beim Chart-Scrubbing (selectedSnapshot pro Move-Event) sonst jede Frame neu.
    val heavy = remember(history) { sleepTotalHeavy(history) }
    val derived =
        remember(selectedSnapshot, heavy) {
            sleepTotalDerived(selectedSnapshot = selectedSnapshot, heavy = heavy)
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
                        "30-Tage-Schnitt: ${derived.avg30Minutes?.let { formatSleepTime(it) } ?: "—"}",
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
    val avg30Minutes: Double?,
    val deltaVsAvg: Double?,
    val last30Minutes: List<Double>,
)

/** History-abhaengige Schwerarbeit — aendert sich beim Scrubbing NICHT, daher separat memoiziert. */
private data class SleepTotalHeavy(
    val lastMinutes: Double?,
    val last30: List<Double>,
    val avg30: Double?,
)

private fun sleepTotalHeavy(history: List<BiomarkerSnapshotEntity>): SleepTotalHeavy {
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
    val last30 = all.takeLast(30)
    return SleepTotalHeavy(
        lastMinutes = all.lastOrNull(),
        last30 = last30,
        // Schwelle >= 3 Werte 1:1 vom Erholungsverlauf uebernommen — darunter ist ein
        // "30-Tage-Schnitt" nicht aussagekraeftig.
        avg30 = if (last30.size >= 3) last30.average() else null,
    )
}

private fun sleepTotalDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    heavy: SleepTotalHeavy,
): SleepTotalDerived {
    val current = selectedSnapshot?.let { snap ->
        val total = snap.sleepTotalMinutes ?: return@let null
        val awake = snap.sleepAwakeMinutes ?: 0
        (total - awake).coerceAtLeast(0).takeIf { it > 0 }
    } ?: heavy.lastMinutes?.toInt()
    val delta = if (current != null && heavy.avg30 != null) current.toDouble() - heavy.avg30 else null

    return SleepTotalDerived(
        currentMinutes = current,
        avg30Minutes = heavy.avg30,
        deltaVsAvg = delta,
        last30Minutes = heavy.last30,
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
    // Feste Skala ab 0 wie im Erholungsverlauf (dort 0-100 %), hier 0-10 h. Dadurch sind
    // Balkenhoehe und Ampel ueber alle Tage vergleichbar; laengere Naechte heben die
    // Obergrenze mit an, damit kein Balken visuell gedeckelt wird.
    val yMax = remember(values) { maxOf(600.0, values.maxOrNull() ?: 0.0) }
    MiniBarsCanvas(
        values = values,
        barColor = { sleepTotalBarColor(it.toInt()) },
        yMin = 0.0,
        yMax = yMax,
        emptyText = "Noch keine Schlafzeit-Daten",
    )
}

/**
 * Frank-Vorgabe 2026-08-07 — gleiche Farbtoene wie der Erholungsverlauf:
 * - ab 8 h (480 min)          -> Gruen
 * - 6,5 bis 8 h (390-480 min) -> Gelb
 * - unter 6,5 h (390 min)     -> Rot
 */
private fun sleepTotalBarColor(minutes: Int): Color =
    when {
        minutes >= 480 -> CosmosColors.WhoopRecoveryGreen
        minutes >= 390 -> CosmosColors.WhoopRecoveryYellow
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
