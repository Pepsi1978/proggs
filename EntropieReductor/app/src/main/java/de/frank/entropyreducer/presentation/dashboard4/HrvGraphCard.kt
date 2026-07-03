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
 * HRV-Verlauf im Erholungsverlauf-Pattern (Frank-Wunsch 2026-06-21).
 *
 * Zeigt den HRV-Wert der letzten ~30 Tage als Balken-Graph. Tap auf die Karte oeffnet den
 * bestehenden HRV-Detail-Screen (1:1 wie bisher). Unter dem Graphen wird der Durchschnitt
 * ueber ALLE jemals gespeicherten HRV-Werte angezeigt — nicht nur der letzten 30 Tage.
 *
 * Farb-Logik (relativ zum persoenlichen Durchschnitt):
 * - mindestens 3 ms ueber dem Durchschnitt  -> Gruen
 * - innerhalb von +/- 3 ms um den Schnitt   -> Gelb
 * - mehr als 3 ms unter dem Durchschnitt    -> Rot
 *
 * Farbtöne 1:1 identisch zum Erholungsverlauf (WHOOP-Recovery-Ampel):
 * [CosmosColors.WhoopRecoveryGreen] / [CosmosColors.WhoopRecoveryYellow] /
 * [CosmosColors.WhoopRecoveryRed].
 */
@Composable
internal fun HrvGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.accent

    // Performance-Fix 2026-07-03 (#47449): Schwerarbeit haengt nur an history — beim
    // Chart-Scrubbing (selectedSnapshot aendert sich pro Move-Event) lief die komplette
    // groupBy-Pipeline sonst in JEDEM Frame neu (gleiches Muster wie InteractiveLineChart-Audit).
    val heavy = remember(history) { hrvHeavy(history) }
    val derived =
        remember(selectedSnapshot, heavy) {
            hrvDerived(selectedSnapshot = selectedSnapshot, heavy = heavy)
        }

    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "HRV",
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Herzfrequenzvariabilitaet",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
                val headerColor =
                    derived.currentMs?.let { current ->
                        hrvBarColor(value = current, avg = derived.avgAllMs, accentColor = accent)
                    } ?: accent
                Text(
                    text = derived.currentMs?.let { "%.1f".format(it).replace('.', ',') + " ms" } ?: "—",
                    color = headerColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))

            HrvBars(values = derived.last30Ms, avg = derived.avgAllMs)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text =
                        "Durchschnitt: ${derived.avgAllMs?.let { "%.1f".format(it).replace('.', ',') + " ms" } ?: "—"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = cosmos.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                // Frank-Wunsch 2026-06-21: Abweichungs-Badge wie im Erholungsverlauf
                // (RecoveryGraphCard) — Plus = ueber Schnitt = Gruen, Minus = darunter
                // = Rot, innerhalb +/- 0.5 ms = Accent (neutral). Gleiche
                // Hintergrundfarben (ok/crit/accent mit alpha 0.18) wie Erholungsverlauf.
                if (derived.deltaVsAvg != null) {
                    HrvTrendBadgeMs(delta = derived.deltaVsAvg)
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

private data class HrvDerived(
    val currentMs: Double?,
    val avgAllMs: Double?,
    val deltaVsAvg: Double?,
    val last30Ms: List<Double>,
)

/** History-abhaengige Schwerarbeit — aendert sich beim Scrubbing NICHT, daher separat memoiziert. */
private data class HrvHeavy(
    val all: List<Double>,
    val last30: List<Double>,
    val avgAll: Double?,
)

private fun hrvHeavy(history: List<BiomarkerSnapshotEntity>): HrvHeavy {
    val zone = ZoneId.systemDefault()
    val all =
        history
            .mapNotNull { snap ->
                val hrv = snap.hrvMs ?: return@mapNotNull null
                val date = Instant.ofEpochMilli(snap.capturedAt).atZone(zone).toLocalDate()
                date to hrv
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.average() }
            .toSortedMap()
            .map { (_, hrv) -> hrv }
    return HrvHeavy(
        all = all,
        last30 = all.takeLast(30),
        avgAll = all.takeIf { it.isNotEmpty() }?.average(),
    )
}

private fun hrvDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    heavy: HrvHeavy,
): HrvDerived {
    val current = selectedSnapshot?.hrvMs ?: heavy.all.lastOrNull()
    // Frank-Wunsch 2026-06-21: Abweichung zum Gesamt-Durchschnitt fuer das
    // Trend-Badge im Footer (Plus = ueber Schnitt = Gruen, Minus = Rot).
    val delta = if (current != null && heavy.avgAll != null) current - heavy.avgAll else null

    return HrvDerived(
        currentMs = current,
        avgAllMs = heavy.avgAll,
        deltaVsAvg = delta,
        last30Ms = heavy.last30,
    )
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun HrvBars(values: List<Double>, avg: Double?) {
    val accent = LocalCosmos.current.accent
    val yMin = remember(values) { (values.minOrNull() ?: 0.0) - 5.0 }
    val yMax = remember(values) { values.maxOrNull() ?: 1.0 }
    MiniBarsCanvas(
        values = values,
        barColor = { hrvBarColor(it, avg, accent) },
        yMin = yMin,
        yMax = yMax,
        emptyText = "Noch keine HRV-Daten",
    )
}

private fun hrvBarColor(
    value: Double,
    avg: Double?,
    accentColor: Color,
): Color {
    val avgSafe = avg ?: return accentColor
    // Frank-Wunsch 2026-06-21: Farb-Bereiche relativ zum persoenlichen Durchschnitt.
    // Farbtöne 1:1 identisch zum Erholungsverlauf (WHOOP-Recovery-Ampel).
    return when {
        value >= avgSafe + 3.0 -> CosmosColors.WhoopRecoveryGreen
        value <= avgSafe - 3.0 -> CosmosColors.WhoopRecoveryRed
        else -> CosmosColors.WhoopRecoveryYellow
    }
}

/**
 * Frank-Wunsch 2026-06-21: Trend-Badge fuer HRV — Plus (ueber Durchschnitt) = Gruen,
 * Minus (darunter) = Rot, innerhalb +/- 0.5 ms = Accent (neutral). 1:1 die gleiche
 * Logik + Hintergrundfarben wie [RecoveryGraphCard.RecoveryTrendBadgePercent], nur
 * Format in ms statt Prozent.
 */
@Composable
private fun HrvTrendBadgeMs(delta: Double) {
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
            text = "%+.1f ms".format(delta).replace('.', ','),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
