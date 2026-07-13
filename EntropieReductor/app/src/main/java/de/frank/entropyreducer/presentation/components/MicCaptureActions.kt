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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlinx.coroutines.launch

/**
 * Frank-Wunsch 2026-05-22: Einheitliches Mikrofon-Erlebnis ueber ALLE Reiter
 * der App. Der Mic-Tap in der BottomBar oeffnet keine Aufnahme mehr direkt
 * und kein eckiges BottomSheet — stattdessen erscheinen genau wie im Entropie-
 * Reiter zwei runde Aktions-Buttons "Schreiben" und "Aufnehmen" inline ueber
 * der BottomBar. 1:1 Optik aus TagebuchScreen, parametrisierbare Akzentfarbe
 * pro Reiter (Aufgaben orange, Forscher lila, Analyse gruen, Biomarker rosé).
 *
 * Verwendung:
 *
 * ```
 * Box(modifier = Modifier.fillMaxSize()) {
 *     // ... Screen-Inhalt ...
 *     MicCaptureActions(
 *         visible = micActionsOpen,
 *         accent = MyAccentColor,
 *         onTextCommit = { text, source -> vm.processCapturedText(text, source) },
 *         onClose = { micActionsOpen = false },
 *         modifier = Modifier.align(Alignment.BottomCenter),
 *     )
 * }
 * ```
 *
 * Die Composable verwaltet selbst:
 *  - TextInputDialog (Schreiben-Pfad)
 *  - VoiceCaptureViewModel-Toggle (Aufnehmen-Pfad)
 *  - Mic-Permission-Anforderung
 *
 * Sobald ein Text via Schreiben oder Aufnehmen fertiggestellt ist, wird
 * [onTextCommit] mit Text + Quelle aufgerufen, dann [onClose].
 */
@Composable
fun MicCaptureActions(
    visible: Boolean,
    accent: Color,
    onTextCommit: (text: String, source: EntrySource) -> Unit,
    onTaskTextCommit: ((text: String, title: String?) -> Unit)? = null,
    onImproveText: (suspend (String) -> Result<String>)? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 110.dp,
    // Frank-Wunsch 2026-05-31: wenn gesetzt, wird ein per Mikrofon transkribierter
    // Text NICHT sofort gespeichert, sondern an diesen Callback uebergeben (der
    // Aufgaben-Reiter oeffnet damit ein Review-Fenster zum Pruefen/Verbessern/
    // Benennen). Ist er null, bleibt das alte Verhalten (Direkt-Speichern).
    onReviewTranscript: ((transcript: String) -> Unit)? = null,
    // Frank-Wunsch 2026-06-11 (Mentalboard): wenn gesetzt, oeffnet der "Schreiben"-Button
    // NICHT den eingebauten TextInputDialog, sondern ruft diesen Callback auf — das
    // Mentalboard zeigt damit seinen eigenen "Neues Mental"-Dialog (orange Diskette).
    // null = altes Verhalten (eingebauter Dialog), alle anderen Reiter unveraendert.
    onWriteClick: (() -> Unit)? = null,
    voiceVm: VoiceCaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val voiceState by voiceVm.state.collectAsStateWithLifecycle()
    val voiceError by voiceVm.error.collectAsStateWithLifecycle()
    val actionAccent = LocalCosmos.current.accent
    var inputDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(voiceError) {
        voiceError?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            voiceVm.clearError()
        }
    }

    // Liefert ein fertiges Transkript entweder ans Review-Fenster (wenn gesetzt)
    // oder direkt an onTextCommit (altes Verhalten der anderen Reiter).
    val deliverTranscript: (String) -> Unit = { raw ->
        val t = raw.trim()
        if (t.isNotEmpty()) {
            if (onReviewTranscript != null) onReviewTranscript(t)
            else onTextCommit(t, EntrySource.NUTZER_MIC)
        }
    }

    val micPermission = rememberMicPermissionState(
        onAllGranted = {
            voiceVm.toggle { transcript ->
                deliverTranscript(transcript)
                onClose()
            }
        },
    )

    // Texteingabe-Dialog fuer den Schreiben-Pfad. Erscheint wenn der Benutzer
    // den linken Button waehlt.
    if (inputDialogOpen) {
        TextInputDialog(
            accent = accent,
            onDismiss = { inputDialogOpen = false },
            onSave = { text ->
                inputDialogOpen = false
                onTextCommit(text, EntrySource.NUTZER_TEXT)
                onClose()
            },
            onTaskSave = onTaskTextCommit?.let { save ->
                { text, title ->
                    inputDialogOpen = false
                    save(text, title)
                    onClose()
                }
            },
            onImprove = onImproveText,
        )
    }

    if (!visible) return

    val recording = voiceState == VoiceCaptureState.RECORDING
    val processing = voiceState == VoiceCaptureState.PROCESSING

    val (recordIcon, recordLabel) = when {
        recording -> Icons.Outlined.Stop to "Stop"
        processing -> Icons.Outlined.Mic to "Transkribiere…"
        else -> Icons.Outlined.Mic to "Aufnehmen"
    }

    Row(
        modifier = modifier.padding(bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        FabActionButton(
            icon = Icons.Outlined.Edit,
            label = "Schreiben",
            background = actionAccent.copy(alpha = 0.7f),
            // Frank-Wunsch 2026-05-22 (dritte Iteration): Icon-Tint immer Schwarz,
            // analog zum schwarzen Mic-Symbol im zentralen BottomBar-Button.
            tint = Color.Black,
            onClick = {
                if (onWriteClick != null) {
                    onWriteClick()
                    onClose()
                } else {
                    inputDialogOpen = true
                }
            },
        )
        FabActionButton(
            icon = recordIcon,
            label = recordLabel,
            background = actionAccent.copy(alpha = 0.7f),
            tint = Color.Black,
            onClick = {
                when (voiceState) {
                    VoiceCaptureState.IDLE -> {
                        if (micPermission.check()) {
                            voiceVm.toggle { transcript ->
                                deliverTranscript(transcript)
                                onClose()
                            }
                        } else {
                            micPermission.request()
                        }
                    }
                    VoiceCaptureState.RECORDING -> {
                        voiceVm.toggle { transcript ->
                            deliverTranscript(transcript)
                            onClose()
                        }
                    }
                    VoiceCaptureState.PROCESSING -> Unit
                }
            },
        )
    }
}

@Composable
private fun FabActionButton(
    icon: ImageVector,
    label: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(background)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = LocalCosmos.current.textSecondary,
        )
    }
}

@Composable
private fun TextInputDialog(
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onTaskSave: ((text: String, title: String?) -> Unit)? = null,
    onImprove: (suspend (String) -> Result<String>)? = null,
) {
    var text by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var textBeforeImprovement by remember { mutableStateOf<String?>(null) }
    var improving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Eintrag") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onTaskSave != null) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Titel") },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    placeholder = { Text("Was steht an?") },
                )
                if (onImprove != null) {
                    Row {
                        TextButton(
                            onClick = {
                                if (improving) return@TextButton
                                error = null
                                improving = true
                                scope.launch {
                                    onImprove(text)
                                        .onSuccess {
                                            textBeforeImprovement = text
                                            text = it
                                        }
                                        .onFailure {
                                            error = it.message ?: "Verbesserung fehlgeschlagen"
                                        }
                                    improving = false
                                }
                            },
                            enabled = !improving && text.isNotBlank(),
                        ) {
                            if (improving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = accent,
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.size(6.dp))
                            Text(if (improving) "Verbessere …" else "Mit KI verbessern", color = accent)
                        }
                        TextButton(
                            onClick = {
                                text = textBeforeImprovement.orEmpty()
                                textBeforeImprovement = null
                            },
                            enabled = !improving && textBeforeImprovement != null,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.Undo,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text("Rückgängig", color = accent)
                        }
                    }
                    error?.let { Text(it, color = LocalCosmos.current.crit) }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (text.isNotBlank()) {
                        if (onTaskSave != null) onTaskSave(text.trim(), title.trim().takeIf { it.isNotBlank() })
                        else onSave(text.trim())
                    }
                },
                enabled = text.isNotBlank() && !improving,
            ) {
                Text("Speichern", color = accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
