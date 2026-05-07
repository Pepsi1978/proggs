package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.domain.kiquestion.KiQuestion
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Kontextrelevante Frage der KI auf Dashboard 1 (Spec §10.4).
 * Soll-Design: Sparkle-Icon + Header-Pill, Frage als grosser Text,
 * unten ein Mic-Kreis-Button (lila Glass-Hintergrund) + "Spaeter"-Link rechts.
 */
@Composable
fun KiQuestionCard(
    question: KiQuestion,
    onMicAnswer: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = CosmosColors.AccentSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "KI-Frage des Moments",
                    style = MaterialTheme.typography.labelMedium,
                    color = CosmosColors.AccentSecondary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = question.text,
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Mic-Kreis-Button (lila Glass)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CosmosColors.AccentSecondary.copy(alpha = 0.18f))
                        .clickable(onClick = onMicAnswer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Antworten",
                        tint = CosmosColors.AccentSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                // "Spaeter" als Text-Link
                Text(
                    text = "Spaeter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmosColors.AccentSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onSnooze)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
