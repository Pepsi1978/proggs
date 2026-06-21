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
 * - ueber dem Durchschnitt                -> Gruen
 * - bis zu 5 ms unter dem Durchschnitt    -> Gelb
 * - mehr als 5 ms unter dem Durchschnitt  -> Rot
 */
@Composable
internal fun HrvGraphCard(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = LocalCosmos.current.accent

    val derived =
        remember(selectedSnapshot, history) {
            hrvDerived(selectedSnapshot = selectedSnapshot, history = history)
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
                Text(
                    text = derived.currentMs?.let { "%.1f".format(it).replace('.', ',') + " ms" } ?: "—",
                    color = accent,
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
    val last30Ms: List<Double>,
)

private fun hrvDerived(
    selectedSnapshot: BiomarkerSnapshotEntity?,
    history: List<BiomarkerSnapshotEntity>,
): HrvDerived {
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

    val current = selectedSnapshot?.hrvMs ?: all.lastOrNull()
    val last30 = all.takeLast(30)
    val avgAll = all.takeIf { it.isNotEmpty() }?.average()

    return HrvDerived(
        currentMs = current,
        avgAllMs = avgAll,
        last30Ms = last30,
    )
}

/* ------------------------- UI-Bausteine ------------------------- */

@Composable
private fun HrvBars(values: List<Double>, avg: Double?) {
    val cosmos = LocalCosmos.current
    val yMax = remember(values) { (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0) }
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
                    text = "Noch keine HRV-Daten",
                    style = MaterialTheme.typography.labelSmall,
                    color = cosmos.textSecondary,
                    modifier = Modifier.padding(4.dp),
                )
            } else {
                values.forEach { value ->
                    val ratio = (value / yMax).coerceIn(0.05, 1.0).toFloat()
                    Box(
                        modifier =
                            Modifier.weight(1f)
                                .fillMaxHeight(ratio)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    hrvBarColor(
                                        value = value,
                                        avg = avg,
                                        okColor = cosmos.ok,
                                        warnColor = cosmos.warn,
                                        critColor = cosmos.crit,
                                        accentColor = cosmos.accent,
                                    )
                                )
                    )
                }
            }
        }
    }
}

private fun hrvBarColor(
    value: Double,
    avg: Double?,
    okColor: Color,
    warnColor: Color,
    critColor: Color,
    accentColor: Color,
): Color {
    val avgSafe = avg ?: return accentColor
    return when {
        value >= avgSafe -> okColor
        value > avgSafe - 5.0 -> warnColor
        else -> critColor
    }
}
