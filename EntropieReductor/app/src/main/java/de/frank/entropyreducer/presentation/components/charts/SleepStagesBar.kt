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
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Gestapelter horizontaler Bar fuer Schlafstadien (REM/Tief/Leicht/Wach).
 * Spec §13.1 Punkt 6.
 */
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

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(50))
                .background(cosmos.glassBg),
        ) {
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                StageSegment(weight = deep, total = total, color = Color(0xFF6366F1))
                StageSegment(weight = rem, total = total, color = CosmosColors.AccentSecondary)
                StageSegment(weight = light, total = total, color = CosmosColors.AccentPrimary)
                StageSegment(weight = awake, total = total, color = CosmosColors.Warning)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Legend("Tief", deep, Color(0xFF6366F1))
            Legend("REM", rem, CosmosColors.AccentSecondary)
            Legend("Leicht", light, CosmosColors.AccentPrimary)
            Legend("Wach", awake, CosmosColors.Warning)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StageSegment(
    weight: Int,
    total: Int,
    color: Color,
) {
    val ratio = (weight.toFloat() / total).coerceIn(0f, 1f)
    if (ratio <= 0f) return
    Box(
        modifier = Modifier
            .weight(ratio)
            .fillMaxHeight()
            .background(color),
    )
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
