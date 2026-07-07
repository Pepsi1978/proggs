package de.frank.entropyreducer.presentation.components

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Sprint 6 (Frank-Wunsch 2026-05-22 abend): Einheitlicher Erfassungs-Dialog fuer neue Aufgaben —
 * kein Mic-Tap startet mehr sofort die Aufnahme. Statt dessen oeffnet sich dieser Sheet mit zwei
 * grossen Wahl-Tasten "Aufnehmen" und "Schreiben", genau wie schon im Entropie-Reiter (Tagebuch).
 *
 * Drei Modi:
 * - CHOOSE: 2 grosse Buttons zur Auswahl
 * - RECORDING: Roter Stop-Button + "Sprich jetzt"-Hinweis (Whisper laeuft)
 * - PROCESSING: Spinner waehrend Transkription
 * - WRITING: OutlinedTextField + Speichern-Button
 *
 * Nach erfolgreicher Erfassung liefert [onCommit] das Ergebnis mit der passenden EntrySource
 * (NUTZER_MIC fuer Aufnahme, NUTZER_TEXT fuer Schreiben).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryCaptureSheet(
    onDismiss: () -> Unit,
    onCommit: (text: String, source: EntrySource) -> Unit,
    title: String = "Neue Aufgabe",
    accent: Color = LocalCosmos.current.accent,
    voiceVm: VoiceCaptureViewModel = hiltViewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val cosmos = LocalCosmos.current
    val voiceState by voiceVm.state.collectAsStateWithLifecycle()
    val voiceError by voiceVm.error.collectAsStateWithLifecycle()

    // Lokaler UI-Mode: CHOOSE / WRITING — RECORDING/PROCESSING kommen aus voiceState.
    var mode by remember { mutableStateOf(CaptureMode.CHOOSE) }
    var writeText by remember { mutableStateOf("") }
    val micPermission =
        rememberMicPermissionState(
            onAllGranted = {
                voiceVm.toggle { transcript -> onCommit(transcript, EntrySource.NUTZER_MIC) }
            }
        )

    // Bei Fehler kurz anzeigen, dann auf CHOOSE zurueck.
    LaunchedEffect(voiceError) {
        voiceError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            voiceVm.clearError()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            // Wenn gerade aufgenommen wird, abbrechen + Sheet schliessen.
            if (voiceState == VoiceCaptureState.RECORDING) voiceVm.toggle { /* discard */ }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = if (cosmos.isDark) Color(0xFF26211B) else Color.White,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )

            when {
                voiceState == VoiceCaptureState.RECORDING -> {
                    BigActionTile(
                        icon = Icons.Outlined.Stop,
                        label = "Aufnahme stoppen",
                        sublabel = "Sprich, ich höre zu. Tippe zum Beenden.",
                        bg = LocalCosmos.current.crit,
                        fg = Color.White,
                        onClick = {
                            voiceVm.toggle { transcript ->
                                onCommit(transcript, EntrySource.NUTZER_MIC)
                            }
                        },
                    )
                }
                voiceState == VoiceCaptureState.PROCESSING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = accent, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.size(16.dp))
                        Text(
                            "Transkribiere…",
                            style = MaterialTheme.typography.titleMedium,
                            color = cosmos.textPrimary,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
                mode == CaptureMode.WRITING -> {
                    OutlinedTextField(
                        value = writeText,
                        onValueChange = { writeText = it },
                        label = { Text("Was steht an?") },
                        minLines = 3,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { mode = CaptureMode.CHOOSE }) { Text("Zurück") }
                        Spacer(Modifier.size(8.dp))
                        Button(
                            onClick = { onCommit(writeText.trim(), EntrySource.NUTZER_TEXT) },
                            enabled = writeText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Speichern")
                        }
                    }
                }
                else -> {
                    // CHOOSE — zwei grosse Tap-Targets
                    BigActionTile(
                        icon = Icons.Outlined.Mic,
                        label = "Aufnehmen",
                        sublabel = "Sprich rein, Whisper transkribiert.",
                        bg = accent,
                        fg = Color.White,
                        onClick = {
                            if (micPermission.check()) {
                                voiceVm.toggle { transcript ->
                                    onCommit(transcript, EntrySource.NUTZER_MIC)
                                }
                            } else {
                                micPermission.request()
                            }
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    BigActionTile(
                        icon = Icons.Outlined.Edit,
                        label = "Schreiben",
                        sublabel = "Tippe deinen Text ein.",
                        bg = accent.copy(alpha = 0.18f),
                        fg = accent,
                        onClick = { mode = CaptureMode.WRITING },
                    )
                }
            }
        }
    }
}

private enum class CaptureMode {
    CHOOSE,
    WRITING,
}

@Composable
private fun BigActionTile(
    icon: ImageVector,
    label: String,
    sublabel: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(bg)
                .clickable { onClick() }
                .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier.size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(fg.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = fg,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg.copy(alpha = 0.85f),
                )
            }
        }
    }
}
