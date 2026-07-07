package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.frank.entropyreducer.domain.kiquestion.KiQuestion
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Kontextrelevante Frage der KI auf Dashboard 1 (Spec §10.4). Frank-Wunsch 2026-05-08: sichtbares
 * Antwort-Feld + Whisper-Mic + Send-Button. Antwort wird durch submitAnswer-Callback an den
 * ViewModel weitergereicht der sie deduplizierungs-aware verarbeitet (kein Doppel-Eintrag wenn
 * Antwort eine existierende Aufgabe nennt).
 */
@Composable
fun KiQuestionCard(
    question: KiQuestion,
    onSubmitAnswer: (String) -> Unit,
    onSnooze: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    var answer by remember(question.triggerKey) { mutableStateOf("") }
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = LocalCosmos.current.accentForscher,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "KI-Frage des Moments",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalCosmos.current.accentForscher,
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
            Spacer(Modifier.height(10.dp))
            // Antwort-Feld + Whisper-Mic + Send (Frank-Wunsch 2026-05-08).
            OutlinedTextField(
                value = answer,
                onValueChange = { answer = it },
                placeholder = {
                    Text("Tippe oder sprich deine Antwort …", color = cosmos.textSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cosmos.textPrimary,
                        unfocusedTextColor = cosmos.textPrimary,
                        focusedBorderColor = LocalCosmos.current.accentForscher,
                        unfocusedBorderColor = cosmos.glassBorder,
                    ),
                maxLines = 3,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WhisperMicButton(
                    onTranscript = { transcript ->
                        answer = if (answer.isBlank()) transcript else "$answer $transcript"
                    },
                    size = 44.dp,
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (answer.isNotBlank()) {
                            onSubmitAnswer(answer)
                            answer = ""
                        }
                    },
                    enabled = answer.isNotBlank(),
                    modifier =
                        Modifier.size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (answer.isNotBlank()) LocalCosmos.current.accentForscher
                                else LocalCosmos.current.accentForscher.copy(alpha = 0.3f)
                            ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = "Antwort senden",
                        tint = androidx.compose.ui.graphics.Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                // Aktualisieren-Button: neue clevere Frage anfordern (Frank-Wunsch 2026-05-08).
                Text(
                    text = "Aktualisieren",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalCosmos.current.accent,
                    fontWeight = FontWeight.Medium,
                    modifier =
                        Modifier.clip(RoundedCornerShape(50))
                            .clickable(onClick = onRefresh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Text(
                    text = "Später",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalCosmos.current.accentForscher,
                    fontWeight = FontWeight.Medium,
                    modifier =
                        Modifier.clip(RoundedCornerShape(50))
                            .clickable(onClick = onSnooze)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
