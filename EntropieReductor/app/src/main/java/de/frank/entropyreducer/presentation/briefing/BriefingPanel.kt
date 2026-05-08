package de.frank.entropyreducer.presentation.briefing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.DateFormat
import java.util.Date

/**
 * Briefing-Panel — eine kompakte Karte mit drei Tabs:
 *  - Heute: Tagesbriefing
 *  - Woche: Wochenrueckblick
 *  - Monat: Monatsrueckblick
 *
 * Pro Tab: Text-Vorschau (max ~500 Zeichen sichtbar, scrollbar wenn mehr),
 * Anhoeren-Button (TTS), Aktualisieren-Button (manuelle Generierung).
 */
@Composable
fun BriefingPanel(
    modifier: Modifier = Modifier,
    vm: BriefingViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var selected by rememberSaveable { mutableStateOf(PlayingKind.DAILY) }
    val cosmos = LocalCosmos.current

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    null,
                    tint = CosmosColors.AccentPrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Briefing",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmosColors.AccentPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BriefingTabChip(
                    label = "Heute",
                    selected = selected == PlayingKind.DAILY,
                    onClick = { selected = PlayingKind.DAILY },
                )
                BriefingTabChip(
                    label = "Woche",
                    selected = selected == PlayingKind.WEEKLY,
                    onClick = { selected = PlayingKind.WEEKLY },
                )
                BriefingTabChip(
                    label = "Monat",
                    selected = selected == PlayingKind.MONTHLY,
                    onClick = { selected = PlayingKind.MONTHLY },
                )
            }
            Spacer(Modifier.height(10.dp))

            val (text, atMs, regen) = when (selected) {
                PlayingKind.DAILY -> Triple(state.dailyText, state.dailyAtMs, vm::regenerateDaily)
                PlayingKind.WEEKLY -> Triple(state.weeklyText, state.weeklyAtMs, vm::regenerateWeekly)
                PlayingKind.MONTHLY -> Triple(state.monthlyText, state.monthlyAtMs, vm::regenerateMonthly)
                PlayingKind.NONE -> Triple("", 0L, {})
            }

            if (text.isBlank()) {
                Text(
                    text = when (selected) {
                        PlayingKind.DAILY -> "Noch kein Tagesbriefing vorhanden. Tippe auf Aktualisieren — das Genie braucht ca. 10 Sekunden."
                        PlayingKind.WEEKLY -> "Dein Wochenrückblick wird sonntags 19:00 erstellt. Du kannst ihn jederzeit manuell anstoßen."
                        PlayingKind.MONTHLY -> "Der Monatsrückblick wird am 1. des Folgemonats 19:00 erstellt."
                        PlayingKind.NONE -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textSecondary,
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cosmos.textPrimary,
                )
                if (atMs > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Erstellt: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(atMs))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
            }

            AnimatedVisibility(visible = state.errorMessage != null) {
                Column {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmosColors.Critical,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isLoadingThis = state.loading == selected
                val isPlayingThis = state.playing == selected
                if (isPlayingThis) {
                    Button(
                        onClick = { vm.stop() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CosmosColors.AccentPrimary,
                            contentColor = Color.Black,
                        ),
                    ) {
                        Icon(Icons.Outlined.Stop, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Stoppen")
                    }
                } else if (isLoadingThis) {
                    OutlinedButton(
                        onClick = { vm.stop() },
                        modifier = Modifier.weight(1f),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CosmosColors.AccentPrimary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Lade Audio …")
                    }
                } else {
                    OutlinedButton(
                        onClick = { vm.speak(selected) },
                        modifier = Modifier.weight(1f),
                        enabled = text.isNotBlank(),
                    ) {
                        Icon(Icons.Outlined.Headphones, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Anhören")
                    }
                }
                IconButton(onClick = regen) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Aktualisieren",
                        tint = cosmos.textSecondary,
                    )
                }
            }

            // Briefing-Antwort-Feld (Frank-Wunsch 2026-05-08): Frank kann auf
            // das Briefing antworten — per Text oder per Sprache. Antwort
            // wird als Eintrag mit Tag "Briefing-Antwort" verarbeitet, KI
            // lernt daraus.
            if (text.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                BriefingResponseInput(
                    onSubmit = { resp -> vm.submitBriefingResponse(selected, resp) },
                )
            }
        }
    }
}

@Composable
private fun BriefingResponseInput(onSubmit: (String) -> Unit) {
    val cosmos = LocalCosmos.current
    val draftState = remember { mutableStateOf("") }
    Text(
        text = "Antwort auf das Briefing — die KI lernt daraus (Whisper Large V3 Turbo).",
        style = MaterialTheme.typography.labelMedium,
        color = cosmos.textSecondary,
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.OutlinedTextField(
            value = draftState.value,
            onValueChange = { draftState.value = it },
            placeholder = { Text("Tippe oder sprich deine Antwort …", color = cosmos.textSecondary) },
            modifier = Modifier.weight(1f),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedTextColor = cosmos.textPrimary,
                unfocusedTextColor = cosmos.textPrimary,
                focusedBorderColor = CosmosColors.AccentPrimary,
                unfocusedBorderColor = cosmos.glassBorder,
            ),
            maxLines = 3,
        )
        Spacer(Modifier.size(6.dp))
        // Whisper-Mic statt System-SpeechRecognizer (Frank-Wunsch 2026-05-08).
        de.frank.entropyreducer.presentation.components.WhisperMicButton(
            onTranscript = { transcript ->
                draftState.value = if (draftState.value.isBlank()) transcript
                else "${draftState.value} $transcript"
            },
        )
        Spacer(Modifier.size(6.dp))
        IconButton(
            onClick = {
                onSubmit(draftState.value)
                draftState.value = ""
            },
            enabled = draftState.value.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = "Antwort senden",
                tint = if (draftState.value.isNotBlank()) CosmosColors.AccentPrimary else cosmos.textSecondary,
            )
        }
    }
}

@Composable
private fun BriefingTabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            labelColor = cosmos.textSecondary,
            selectedContainerColor = CosmosColors.AccentPrimary.copy(alpha = 0.2f),
            selectedLabelColor = CosmosColors.AccentPrimary,
        ),
    )
}
