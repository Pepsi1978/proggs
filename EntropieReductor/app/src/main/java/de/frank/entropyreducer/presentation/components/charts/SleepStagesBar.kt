package de.frank.entropyreducer.presentation.components.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Geteilte Schlafphasen-Farben — wird sowohl vom Stage-Bar als auch von den Beschreibungs-Chips
 * unter dem Bar verwendet, damit Farben 1:1 uebereinstimmen. Frank-Wunsch 2026-05-13: Reihenfolge
 * in Beschreibung = Bar (Tief → REM → Leicht → Wach).
 */
object SleepStageColors {
    val Deep: Color = Color(0xFF6366F1)
    val Rem: Color = Color(0xFFA78BFA)
    val Light: Color = Color(0xFF38BDF8)
    val Awake: Color = Color(0xFFFBBF24)
}

/** Gestapelter horizontaler Bar für Schlafstadien (REM/Tief/Leicht/Wach). Spec §13.1 Punkt 6. */
@Composable
fun SleepStagesBar(
    remMinutes: Int?,
    deepMinutes: Int?,
    lightMinutes: Int?,
    awakeMinutes: Int?,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    val rem = remMinutes ?: 0
    val deep = deepMinutes ?: 0
    val light = lightMinutes ?: 0
    val awake = awakeMinutes ?: 0
    val total = (rem + deep + light + awake).coerceAtLeast(1)
    // Frank-Wunsch 2026-05-16: Wach-Prozent erst ab 5 % anzeigen — bei kuerzeren
    // Wachphasen wuerde die Zahl optisch nur stoeren. Schwelle inklusive, also
    // ab genau 5 %.
    val awakePercent = (awake.toFloat() / total * 100f).toInt()
    val showAwakePercent = awakePercent >= 5

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(50))
                    .background(cosmos.glassBg)
        ) {
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                // Frank-Wunsch 2026-05-09: Prozentwerte mittig in den Phasen-Segmenten
                // anzeigen — REM, Tiefschlaf, Leichtschlaf bekommen Prozent, Wach
                // ab 2026-05-16 auch, aber nur wenn Wachzeit-Anteil >= 5 %.
                StageSegment(
                    weight = deep,
                    total = total,
                    color = SleepStageColors.Deep,
                    showPercent = true,
                )
                StageSegment(
                    weight = rem,
                    total = total,
                    color = SleepStageColors.Rem,
                    showPercent = true,
                )
                StageSegment(
                    weight = light,
                    total = total,
                    color = SleepStageColors.Light,
                    showPercent = true,
                )
                StageSegment(
                    weight = awake,
                    total = total,
                    color = SleepStageColors.Awake,
                    showPercent = showAwakePercent,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Legend("Tief", deep, SleepStageColors.Deep)
            Legend("REM", rem, SleepStageColors.Rem)
            Legend("Leicht", light, SleepStageColors.Light)
            Legend("Wach", awake, SleepStageColors.Awake)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StageSegment(
    weight: Int,
    total: Int,
    color: Color,
    showPercent: Boolean = false,
) {
    val ratio = (weight.toFloat() / total).coerceIn(0f, 1f)
    if (ratio <= 0f) return
    // Frank-Wunsch 2026-05-25: eine Nachkommastelle (mit Komma) statt abgeschnittener
    // Ganzzahl. Vorher schnitt der Bar ab (16,7 → 16), waehrend der Tiefschlaf-/REM-/
    // Wach-Verlauf rundete (16,7 → 17) — daher die 16-vs-17-Diskrepanz. Mit identischer
    // Eine-Stelle-Formatierung stimmen beide Anzeigen ueberein.
    val percentText = "%.1f".format(ratio * 100f).replace('.', ',')
    Box(
        modifier = Modifier.weight(ratio).fillMaxHeight().background(color),
        contentAlignment = Alignment.Center,
    ) {
        // Prozent nur anzeigen wenn Segment breit genug (sonst wird's gequetscht)
        // und das Anzeigen erlaubt ist (Wach-Segment bewusst ohne Beschriftung).
        if (showPercent && ratio >= 0.07f) {
            Text(
                text = "$percentText %",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Legend(label: String, minutes: Int, color: Color) {
    val cosmos = LocalCosmos.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$label ${minutes / 60}h${minutes % 60}m",
            style = MaterialTheme.typography.labelSmall,
            color = cosmos.textSecondary,
        )
    }
}
