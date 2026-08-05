package com.entropyjournal.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.entropyjournal.data.remote.qwen.ClonedVoice
import com.entropyjournal.util.Constants
import com.entropyjournal.util.VoiceSampleScript

/**
 * Der Abschnitt „Meine Stimme“ in den Stimmen-Einstellungen.
 *
 * Er zeigt die eigene, bei Alibaba geklonte Stimme, laesst zwischen mehreren gespeicherten
 * Stimmen waehlen und nimmt ueber das Plus eine neue auf.
 */
@Composable
fun OwnVoiceSection(viewModel: SettingsViewModel, uiState: SettingsUiState) {
    val isActive = uiState.ttsProvider == Constants.TTS_PROVIDER_QWEN
    val hasKey = uiState.qwenApiKey.isNotBlank()
    var showVoices by remember { mutableStateOf(false) }
    var showRecorder by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .alpha(if (isActive) 1f else 0.4f),
    ) {
        Text(
            if (isActive) "◉ Meine Stimme" else "○ Meine Stimme",
            style = MaterialTheme.typography.titleSmall,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            if (hasKey) "Deine eigene, geklonte Stimme • Alibaba Qwen3-TTS"
            else "Alibaba API-Schlüssel erforderlich",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasKey) {
            Spacer(modifier = Modifier.height(6.dp))
            val selectedTitle = uiState.qwenVoiceNames[uiState.qwenVoiceId]
                ?: uiState.qwenVoices.firstOrNull { it.id == uiState.qwenVoiceId }?.name
                ?: uiState.qwenVoiceId.takeIf { it.isNotBlank() }?.let(::bakedVoiceName)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showVoices = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        selectedTitle ?: "Noch keine eigene Stimme",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        viewModel.openVoiceRecorder()
                        showRecorder = true
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        "Neue Stimme aufzeichnen",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (showVoices) {
        MyVoicesDialog(
            viewModel = viewModel,
            uiState = uiState,
            onRecordNew = {
                viewModel.openVoiceRecorder()
                showRecorder = true
            },
            onDismiss = { showVoices = false },
        )
    }
    if (showRecorder) {
        VoiceRecorderDialog(
            viewModel = viewModel,
            uiState = uiState,
            onDismiss = {
                viewModel.cancelVoiceRecording()
                showRecorder = false
            },
        )
    }
}

/** Zieht den bei Alibaba eingebrannten Namen aus einer Stimm-Kennung. */
private fun bakedVoiceName(voiceId: String): String =
    Regex("^qwen-tts-vc-(.+?)-voice-").find(voiceId)?.groupValues?.get(1) ?: voiceId

@Composable
private fun MyVoicesDialog(
    viewModel: SettingsViewModel,
    uiState: SettingsUiState,
    onRecordNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    var renameVoice by remember { mutableStateOf<ClonedVoice?>(null) }
    var deleteVoice by remember { mutableStateOf<ClonedVoice?>(null) }

    LaunchedEffect(Unit) { viewModel.refreshQwenVoices() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Meine Stimmen", modifier = Modifier.weight(1f))
                IconButton(onClick = onRecordNew) {
                    Icon(
                        Icons.Rounded.Add,
                        "Neue Stimme aufzeichnen",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                when {
                    uiState.qwenVoicesLoading ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Stimmen werden geladen …")
                        }
                    uiState.qwenVoices.isEmpty() ->
                        Text(
                            "Noch keine eigene Stimme. Tippe oben auf das Plus und lies etwa " +
                                "50 Sekunden vor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    else ->
                        uiState.qwenVoices.forEach { voice ->
                            val selected = voice.id == uiState.qwenVoiceId
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                        else Color.Transparent,
                                    )
                                    .clickable { viewModel.selectQwenVoice(voice) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        viewModel.voiceTitle(voice),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        voice.createdAt,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { renameVoice = voice },
                                    enabled = !uiState.voiceBusy,
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        "Namen ändern",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(
                                    onClick = { deleteVoice = voice },
                                    enabled = !uiState.voiceBusy,
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        "Stimme löschen",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                                if (selected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        "Ausgewählt",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                }
                uiState.voiceMessage?.let { note ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
    )

    renameVoice?.let { voice ->
        var text by remember(voice.id) { mutableStateOf(viewModel.voiceTitle(voice)) }
        AlertDialog(
            onDismissRequest = { renameVoice = null },
            title = { Text("Name der Stimme") },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Der Name gilt in dieser App. Bei Alibaba behält die Stimme ihre Kennung.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameQwenVoice(voice.id, text)
                    renameVoice = null
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { renameVoice = null }) { Text("Abbrechen") }
            },
        )
    }

    deleteVoice?.let { voice ->
        AlertDialog(
            onDismissRequest = { deleteVoice = null },
            title = { Text("Stimme löschen") },
            text = {
                Text(
                    "„${viewModel.voiceTitle(voice)}“ endgültig löschen? Die Stimme wird " +
                        "bei Alibaba entfernt und lässt sich nur durch eine neue Aufnahme zurückholen.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteQwenVoice(voice)
                    deleteVoice = null
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { deleteVoice = null }) { Text("Abbrechen") }
            },
        )
    }
}

/**
 * Nimmt eine Sprachprobe auf und macht daraus eine geklonte Stimme.
 *
 * Der Vorlesetext steht vor dem Start da, damit er in einem Zug gelesen werden kann — das ist
 * es, was die Probe nach ruhigem Tagebuchton klingen laesst statt nach einem Testsatz.
 */
@Composable
private fun VoiceRecorderDialog(
    viewModel: SettingsViewModel,
    uiState: SettingsUiState,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }

    val recording = uiState.voiceRecorderState == VoiceRecorderState.RECORDING
    val saving = uiState.voiceRecorderState == VoiceRecorderState.SAVING

    // Nach dem erfolgreichen Anlegen schliesst sich der Aufnahmedialog von selbst.
    LaunchedEffect(uiState.voiceRecorderDone) {
        if (uiState.voiceRecorderDone) onDismiss()
    }

    AlertDialog(
        onDismissRequest = { if (!recording && !saving) onDismiss() },
        title = { Text("Neue Stimme aufzeichnen") },
        text = {
            Column(modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = uiState.voiceRecorderName,
                    onValueChange = viewModel::updateVoiceRecorderName,
                    label = { Text("Name der Stimme") },
                    placeholder = { Text("z. B. Frank Abend") },
                    singleLine = true,
                    enabled = !recording && !saving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Das liest du vor",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Ruhig und warm, gleichmäßig bis zum Schluss — etwa 50 Sekunden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                VoiceSampleScript.sentences.forEachIndexed { index, sentence ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(24.dp),
                        )
                        Text(
                            sentence,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                uiState.voiceRecorderMessage?.let { note ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        when {
                            saving -> "Die Stimme wird erstellt …"
                            recording -> "%d:%02d".format(
                                uiState.voiceRecorderSeconds / 60,
                                uiState.voiceRecorderSeconds % 60,
                            )
                            else -> "Bereit"
                        },
                        style = if (recording) MaterialTheme.typography.headlineSmall
                        else MaterialTheme.typography.bodyMedium,
                        color = if (recording) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (recording) MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            )
                            .clickable(enabled = !saving) {
                                viewModel.onVoiceRecorderTapped(permissionGranted) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (saving) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        } else {
                            Icon(
                                if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                if (recording) "Aufnahme stoppen" else "Aufnahme starten",
                                tint = if (recording) MaterialTheme.colorScheme.onError
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Schließen") }
        },
    )
}
