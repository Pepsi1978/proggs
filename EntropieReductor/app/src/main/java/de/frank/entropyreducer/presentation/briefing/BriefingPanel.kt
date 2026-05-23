package de.frank.entropyreducer.presentation.briefing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.domain.kiquestion.KiQuestion
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.WhisperMicButton
import de.frank.entropyreducer.presentation.dashboard1.TasksViewModel
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.text.DateFormat
import java.util.Date

/**
 * Aufgaben-Tab-Akzent — orange (matched die BottomBar-Sub-Mode-Farbe des Aufgaben-Tabs
 * sowie TagebuchAccent in TagebuchScreen). Genutzt fuer den Lautsprecher-Knopf und die
 * Dropdown-Pfeile damit das Briefing visuell zum Aufgaben-Bereich gehoert.
 */
private val AufgabenAccent: Color = Color(0xFFEA580C)

/**
 * Briefing-Panel — kompakte Karte mit Tagesbriefing + zwei eingeklappten Sub-Bereichen
 * (Frank-Wunsch 2026-05-23).
 *
 * Aufbau:
 *  1. Header "Briefing" mit Accent-Icon
 *  2. Briefing-Text (oder Leerhinweis)
 *  3. Mini-Toolbar: kleiner oranger Lautsprecher (Anhoeren/Stoppen) + Aktualisieren-Icon
 *  4. Dropdown "Antwort an die KI" — eingeklappt, expandiert auf Pfeil-Klick.
 *     Enthaelt das vorhandene Antwort-TextField + Whisper-Mic + Senden-Button.
 *  5. Dropdown "KI-Frage des Moments" — eingeklappt, expandiert auf Pfeil-Klick.
 *     Zieht state.kiQuestion vom TasksViewModel und nutzt dessen submit/snooze/refresh.
 *
 * Damit hat die Aufgaben-Liste deutlich mehr optisches Gewicht (frueher hat das
 * fixe Antwort-TextField + die KI-Frage-Karte mehrere Bildschirmhoehen belegt).
 */
@Composable
fun BriefingPanel(
    modifier: Modifier = Modifier,
    vm: BriefingViewModel = hiltViewModel(),
    tasksVm: TasksViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val tasksState by tasksVm.state.collectAsState()
    val selected = PlayingKind.DAILY
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

            val text = state.dailyText
            val atMs = state.dailyAtMs
            val regen: () -> Unit = vm::regenerateDaily

            if (text.isBlank()) {
                Text(
                    text =
                        "Noch kein Tagesbriefing vorhanden. Tippe auf Aktualisieren — das Genie braucht ca. 10 Sekunden.",
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
                        text =
                            "Erstellt: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(atMs))}",
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

            // ── Mini-Toolbar: kleiner Lautsprecher + Aktualisieren ──
            // Frank-Wunsch 2026-05-23: Der grosse "Anhoeren"-Button wird zu einem
            // kleinen orangen Lautsprecher-IconButton direkt neben dem Refresh-Icon.
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val isLoadingThis = state.loading == selected
                val isPlayingThis = state.playing == selected
                IconButton(
                    onClick = {
                        when {
                            isPlayingThis || isLoadingThis -> vm.stop()
                            text.isNotBlank() -> vm.speak(selected)
                        }
                    },
                    enabled = text.isNotBlank() || isPlayingThis || isLoadingThis,
                ) {
                    when {
                        isLoadingThis ->
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = AufgabenAccent,
                            )
                        isPlayingThis ->
                            Icon(
                                imageVector = Icons.Outlined.Stop,
                                contentDescription = "Vorlesen stoppen",
                                tint = AufgabenAccent,
                                modifier = Modifier.size(22.dp),
                            )
                        else ->
                            Icon(
                                imageVector = Icons.Outlined.VolumeUp,
                                contentDescription = "Briefing vorlesen",
                                tint =
                                    if (text.isNotBlank()) AufgabenAccent
                                    else AufgabenAccent.copy(alpha = 0.35f),
                                modifier = Modifier.size(22.dp),
                            )
                    }
                }
                IconButton(onClick = regen) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Briefing aktualisieren",
                        tint = cosmos.textSecondary,
                    )
                }
            }

            // ── Dropdown 1: Antwort an die KI (Frank-Wunsch 2026-05-23) ──
            if (text.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                CollapsibleSection(
                    label = "Antwort an die KI",
                    accent = AufgabenAccent,
                ) {
                    BriefingResponseInput(
                        onSubmit = { resp -> vm.submitBriefingResponse(selected, resp) },
                    )
                }
            }

            // ── Dropdown 2: KI-Frage des Moments (Frank-Wunsch 2026-05-23) ──
            tasksState.kiQuestion?.let { q ->
                Spacer(Modifier.height(6.dp))
                CollapsibleSection(
                    label = "KI-Frage des Moments",
                    accent = AufgabenAccent,
                ) {
                    KiQuestionContent(
                        question = q,
                        onSubmitAnswer = { answer -> tasksVm.submitKiQuestionAnswer(answer) },
                        onSnooze = tasksVm::snoozeKiQuestion,
                        onRefresh = tasksVm::refreshKiQuestion,
                    )
                }
            }
        }
    }
}

/**
 * Wiederverwendbares Dropdown — Pfeil rechts, Klick auf Kopfzeile klappt auf/zu.
 * Default = collapsed. Per remember-State, lebt nur in der Komponente.
 */
@Composable
private fun CollapsibleSection(
    label: String,
    accent: Color,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    val cosmos = LocalCosmos.current
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val arrowRotation by
        animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrowRotation")

    Column {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = cosmos.textSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
                tint = accent,
                modifier = Modifier.size(20.dp).rotate(arrowRotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 4.dp)) { content() }
        }
    }
}

@Composable
private fun BriefingResponseInput(onSubmit: (String) -> Unit) {
    val cosmos = LocalCosmos.current
    val draftState = remember { mutableStateOf("") }
    Text(
        text = "Antwort auf das Briefing — die KI lernt daraus.",
        style = MaterialTheme.typography.labelMedium,
        color = cosmos.textSecondary,
    )
    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draftState.value,
            onValueChange = { draftState.value = it },
            placeholder = {
                Text("Tippe oder sprich deine Antwort …", color = cosmos.textSecondary)
            },
            modifier = Modifier.weight(1f),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = cosmos.textPrimary,
                    unfocusedTextColor = cosmos.textPrimary,
                    focusedBorderColor = CosmosColors.AccentPrimary,
                    unfocusedBorderColor = cosmos.glassBorder,
                ),
            maxLines = 3,
        )
        Spacer(Modifier.size(6.dp))
        WhisperMicButton(
            onTranscript = { transcript ->
                draftState.value =
                    if (draftState.value.isBlank()) transcript
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
                tint =
                    if (draftState.value.isNotBlank()) CosmosColors.AccentPrimary
                    else cosmos.textSecondary,
            )
        }
    }
}

/**
 * Inhalt der KI-Frage (ohne aeussere Karte/Header — das macht CollapsibleSection).
 * Im Wesentlichen die alte KiQuestionCard ohne den Wrapper.
 */
@Composable
private fun KiQuestionContent(
    question: KiQuestion,
    onSubmitAnswer: (String) -> Unit,
    onSnooze: () -> Unit,
    onRefresh: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    var answer by remember(question.triggerKey) { mutableStateOf("") }
    Column {
        Text(
            text = question.text,
            style = MaterialTheme.typography.titleSmall,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(10.dp))
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
                    focusedBorderColor = CosmosColors.AccentSecondary,
                    unfocusedBorderColor = cosmos.glassBorder,
                ),
            maxLines = 3,
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                            if (answer.isNotBlank()) CosmosColors.AccentSecondary
                            else CosmosColors.AccentSecondary.copy(alpha = 0.3f)
                        ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Antwort senden",
                    tint = Color.White,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "Aktualisieren",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmosColors.AccentPrimary,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier.clip(RoundedCornerShape(50))
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Text(
                text = "Später",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmosColors.AccentSecondary,
                fontWeight = FontWeight.Medium,
                modifier =
                    Modifier.clip(RoundedCornerShape(50))
                        .clickable(onClick = onSnooze)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
