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
 * Schlaf-Performance-Verlaufs-Pattern (Frank-Wunsch 2026-06-21, gross gezogen 2026-08-07).
 *
 * Zeigt die Schlaf-Performance (%) der letzten ~30 Tage als Balken-Graph. Aufbau 1:1 wie der
 * Erholungsverlauf ([RecoveryGraphCard]): Kopfzeile mit aktuellem Wert, Balken-Graph ueber die
 * volle Breite, darunter 30-Tage-Schnitt + Abweichungs-Badge. Tap auf die Karte oeffnet den
 * bestehenden Performance-Detail-Screen.
 *
 * Ampel-Schwellen (Frank-Vorgabe 2026-08-07, absolut statt relativ zum Schnitt):
 * - 80-100 %      -> Gruen
 * - 65 bis 80 %   -> Gelb
 * - unter 65 %    -> Rot
 */
@Composable
internal fun SleepPerformanceGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.accent

    // Performance-Fix wie in RecoveryGraphCard (#47449): Schwerarbeit haengt nur an history —
    // beim Chart-Scrubbing (selectedSnapshot pro Move-Event) sonst jede Frame neu.
    val heavy = remember(history) { sleepPerformanceHeavy(history) }
    val derived =
        remember(selectedSnapshot, heavy) {
            sleepPerformanceDerived(selectedSnapshot = selectedSnapshot, heavy = heavy)
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
                    derived.currentPercent?.let { sleepPerfBarColor(it) } ?: accent
                Text(
                    text = derived.currentPercent?.let { "%.0f %%".format(it) } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            SleepPerfBars(values = derived.last30Percent)

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
    val avg30Percent: Double?,
    val deltaVsAvg: Double?,
    val last30Percent: List<Double>,
)

/** History-abhaengige Schwerarbeit — aendert sich beim Scrubbing NICHT, daher separat memoiziert. */
private data class SleepPerfHeavy(
    val lastPercent: Double?,
    val last30: List<Double>,
    val avg30: Double?,
)

private fun sleepPerformanceHeavy(history: List<BiomarkerSnapshotEntity>): SleepPerfHeavy {
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
    val last30 = all.takeLast(30)
    return SleepPerfHeavy(
        lastPercent = all.lastOrNull(),
        last30 = last30,
        // Schwelle >= 3 Werte 1:1 vom Erholungsverlauf uebernommen — darunter ist ein
        // "30-Tage-Schnitt" nicht aussagekraeftig.
        avg30 = if (last30.size >= 3) last30.average() else null,
    )
}

private fun sleepPerformanceDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    heavy: SleepPerfHeavy,
): SleepPerfDerived {
    val current = selectedSnapshot?.sleepPerformance?.toDouble() ?: heavy.lastPercent
    val delta = if (current != null && heavy.avg30 != null) current - heavy.avg30 else null

    return SleepPerfDerived(
        currentPercent = current,
        avg30Percent = heavy.avg30,
        deltaVsAvg = delta,
        last30Percent = heavy.last30,
    )
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun SleepPerfBars(values: List<Double>) {
    // Feste 0-100-Skala wie im Erholungsverlauf — dadurch sind Balkenhoehe und Ampel
    // ueber alle Tage hinweg direkt vergleichbar.
    MiniBarsCanvas(
        values = values,
        barColor = { sleepPerfBarColor(it) },
        yMin = 0.0,
        yMax = 100.0,
        emptyText = "Noch keine Performance-Daten",
    )
}

/**
 * Frank-Vorgabe 2026-08-07 — feste Schwellen, gleiche Farbtoene wie der Erholungsverlauf:
 * - 80-100 %    -> Gruen
 * - 65 bis 80 % -> Gelb
 * - unter 65 %  -> Rot
 */
private fun sleepPerfBarColor(pct: Double): Color =
    when {
        pct >= 80.0 -> CosmosColors.WhoopRecoveryGreen
        pct >= 65.0 -> CosmosColors.WhoopRecoveryYellow
        else -> CosmosColors.WhoopRecoveryRed
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
