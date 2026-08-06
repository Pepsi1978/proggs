package de.frank.cortex.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import de.frank.cortex.audio.MicRecorder
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.tts.ClonedVoice
import de.frank.cortex.tts.QwenVoiceDirectory
import de.frank.cortex.tts.QwenVoiceEnrollment
import de.frank.cortex.tts.VoiceSampleScript
import de.frank.cortex.ui.theme.DarkBg
import de.frank.cortex.ui.theme.DarkField
import de.frank.cortex.ui.theme.DarkFieldBorder
import de.frank.cortex.ui.theme.DarkRaised
import de.frank.cortex.ui.theme.JetBrainsMono
import de.frank.cortex.ui.theme.LightBg
import de.frank.cortex.ui.theme.LightField
import de.frank.cortex.ui.theme.LightFieldBorder
import de.frank.cortex.ui.theme.LightSurface
import de.frank.cortex.ui.theme.Orange
import de.frank.cortex.ui.theme.Rose
import de.frank.cortex.ui.theme.SpaceGrotesk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Alibaba nimmt hoechstens eine Minute Referenz-Ton an — die Aufnahme stoppt vorher selbst. */
private const val MAX_SAMPLE_SECONDS = 58
private const val MIN_SAMPLE_SECONDS = 10

private enum class RecorderState { IDLE, RECORDING, SAVING }

/**
 * Die eigenen geklonten Stimmen des Alibaba-Kontos: auswaehlen, umbenennen, loeschen — und ueber
 * das Plus eine neue aufnehmen.
 *
 * Der Name einer Stimme lebt NUR in der App: Alibaba baeckt ihn beim Anlegen in die Kennung ein
 * und kennt kein Umbenennen.
 */
@Composable
fun MyVoicesDialog(
    apiKey: String,
    currentVoiceId: String,
    onVoiceSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val scope = rememberCoroutineScope()
    val directory = remember { QwenVoiceDirectory() }
    val enrollment = remember { QwenVoiceEnrollment() }

    var voices by remember { mutableStateOf<List<ClonedVoice>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var names by remember { mutableStateOf(SettingsStore.qwenVoiceNames) }
    var message by remember { mutableStateOf<String?>(null) }
    var renameTarget by remember { mutableStateOf<ClonedVoice?>(null) }
    var deleteTarget by remember { mutableStateOf<ClonedVoice?>(null) }
    var showRecorder by remember { mutableStateOf(false) }

    suspend fun refresh() {
        loading = true
        voices = directory.list(apiKey)
        loading = false
    }

    LaunchedEffect(apiKey) { refresh() }

    fun titleOf(voice: ClonedVoice): String = names[voice.id] ?: voice.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Meine Stimmen", fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showRecorder = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, "Neue Stimme aufzeichnen", tint = Orange,
                        modifier = Modifier.size(22.dp))
                }
            }
        },
        text = {
            Column {
                Text(
                    "Qwen3-TTS · Alibaba Model Studio",
                    fontFamily = JetBrainsMono, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                when {
                    loading -> Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Orange)
                        Text("Stimmen werden geladen …", fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    voices.isEmpty() -> Text(
                        "Noch keine eigene Stimme. Tippe oben auf das Plus und lies etwa 50 Sekunden vor.",
                        fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> LazyColumn(
                        modifier = Modifier.heightIn(min = 100.dp, max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(voices, key = ClonedVoice::id) { voice ->
                            val selected = currentVoiceId == voice.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Orange.copy(alpha = 0.14f) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) Orange.copy(alpha = 0.40f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Orange.copy(alpha = 0.15f)) {
                                        Icon(Icons.Default.RecordVoiceOver, null, tint = Orange,
                                            modifier = Modifier.size(22.dp).padding(4.dp))
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f).clickable { onVoiceSelected(voice.id) }
                                    ) {
                                        Text(
                                            titleOf(voice), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                            color = if (selected) Orange else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(voice.createdAt, fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = { renameTarget = voice },
                                        enabled = !busy,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, "Namen ändern",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp))
                                    }
                                    IconButton(
                                        onClick = { deleteTarget = voice },
                                        enabled = !busy,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Stimme löschen", tint = Rose,
                                            modifier = Modifier.size(17.dp))
                                    }
                                    if (selected) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Orange,
                                            modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                message?.let { note ->
                    Spacer(Modifier.height(10.dp))
                    Text(note, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fertig") } },
        containerColor = if (isDark) DarkRaised else LightSurface,
        shape = RoundedCornerShape(22.dp)
    )

    renameTarget?.let { voice ->
        RenameVoiceDialog(
            initialName = titleOf(voice),
            onConfirm = { newName ->
                names = names + (voice.id to newName)
                SettingsStore.qwenVoiceNames = names
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }

    deleteTarget?.let { voice ->
        DeleteVoiceDialog(
            title = titleOf(voice),
            onConfirm = {
                deleteTarget = null
                busy = true
                scope.launch {
                    try {
                        enrollment.delete(apiKey, voice.id)
                        names = names - voice.id
                        SettingsStore.qwenVoiceNames = names
                        if (currentVoiceId == voice.id) onVoiceSelected("")
                        message = "Die Stimme „${titleOf(voice)}“ wurde gelöscht."
                        refresh()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        CortexLog.error("Settings", "deleteVoice", "Löschen fehlgeschlagen: ${e.message}")
                        message = e.message ?: "Die Stimme konnte nicht gelöscht werden."
                    } finally {
                        busy = false
                    }
                }
            },
            onDismiss = { deleteTarget = null }
        )
    }

    if (showRecorder) {
        VoiceRecorderDialog(
            apiKey = apiKey,
            onCreated = { voiceId, name ->
                showRecorder = false
                names = names + (voiceId to name)
                SettingsStore.qwenVoiceNames = names
                onVoiceSelected(voiceId)
                message = "Die Stimme „$name“ ist fertig und ausgewählt."
                scope.launch { refresh() }
            },
            onDismiss = { showRecorder = false }
        )
    }
}

@Composable
private fun RenameVoiceDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Name der Stimme", fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        },
        text = {
            Column {
                DialogTextField(value = text, onValueChange = { text = it }, hint = "Name", isDark = isDark)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Der Name gilt in dieser App. Bei Alibaba behält die Stimme ihre Kennung.",
                    fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.isNotBlank()
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        containerColor = if (isDark) DarkRaised else LightSurface,
        shape = RoundedCornerShape(22.dp)
    )
}

@Composable
private fun DeleteVoiceDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Stimme löschen", fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        },
        text = {
            Text(
                "„$title“ endgültig löschen? Die Stimme wird bei Alibaba entfernt und lässt sich " +
                    "nur durch eine neue Aufnahme zurückholen.",
                fontSize = 13.sp
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Löschen", color = Rose) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        containerColor = if (isDark) DarkRaised else LightSurface,
        shape = RoundedCornerShape(22.dp)
    )
}

/**
 * Nimmt eine Stimmprobe auf und macht daraus eine geklonte Stimme.
 *
 * Der Vorlesetext steht VOR dem Aufnehmen da, damit er in einem Zug durchgelesen werden kann —
 * genau das macht die Probe zur eigenen Sprechweise statt zu einem Testsatz.
 */
@Composable
private fun VoiceRecorderDialog(
    apiKey: String,
    onCreated: (voiceId: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val recorder = remember { MicRecorder() }
    val enrollment = remember { QwenVoiceEnrollment() }

    var name by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(RecorderState.IDLE) }
    var seconds by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf<String?>(null) }

    // Das Mikrofon muss auch dann frei werden, wenn der Dialog waehrend der Aufnahme verschwindet.
    DisposableEffect(Unit) { onDispose { recorder.release() } }

    fun startRecording() {
        note = null
        if (!recorder.start(scope, MicRecorder.CLONING_SAMPLE_RATE)) {
            note = "Die Aufnahme konnte nicht gestartet werden."
            return
        }
        seconds = 0
        state = RecorderState.RECORDING
    }

    fun finishRecording() {
        val recordedSeconds = seconds
        val chosenName = name.trim()
        state = RecorderState.SAVING
        scope.launch {
            try {
                val wav = recorder.stop()
                if (wav == null) {
                    note = "Die Aufnahme ist leer geblieben."
                    return@launch
                }
                if (recordedSeconds < MIN_SAMPLE_SECONDS) {
                    note = "Die Aufnahme ist zu kurz. Lies bitte mindestens $MIN_SAMPLE_SECONDS Sekunden vor."
                    return@launch
                }
                val voiceId = enrollment.create(apiKey, chosenName, wav)
                onCreated(voiceId, chosenName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                CortexLog.error("Settings", "createVoice", "Stimme anlegen fehlgeschlagen: ${e.message}")
                note = e.message ?: "Die Stimme konnte nicht erstellt werden."
            } finally {
                state = RecorderState.IDLE
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else note = "Die Mikrofonberechtigung wurde nicht erteilt."
    }

    LaunchedEffect(state) {
        while (state == RecorderState.RECORDING) {
            delay(1_000)
            if (state != RecorderState.RECORDING) return@LaunchedEffect
            seconds++
            if (seconds >= MAX_SAMPLE_SECONDS) {
                finishRecording()
                return@LaunchedEffect
            }
        }
    }

    // Statusleiste und Gestenleiste HIER AUSSERHALB des Dialogs auslesen: an dieser Stelle liegt
    // das randlose Fenster der Activity, das die echten Werte meldet. INNERHALB des Dialogs
    // lieferte systemBarsPadding() null — deshalb sass der Aufnahmeknopf halb unter der
    // Gestenleiste. Die Werte werden unten als feste Polsterung angelegt, statt sich auf die
    // Inset-Weitergabe des Dialog-Fensters zu verlassen.
    val systemBars = WindowInsets.systemBars.asPaddingValues()
    val topInset = systemBars.calculateTopPadding()
    // Mindestens 28 dp, auch wenn das Geraet unten gar keine Leiste meldet: der Knopf soll nie
    // auf der Bildschirmkante kleben.
    val bottomInset = systemBars.calculateBottomPadding().coerceAtLeast(28.dp)

    // Vollbild statt Blase (usePlatformDefaultWidth = false): der Vorlesetext ist der Kern dieses
    // Fensters und wird in grosser Schrift gelesen, waehrend gesprochen wird — in einer Dialog-
    // Blase blieben davon nur wenige Zeilen sichtbar. decorFitsSystemWindows = false sorgt dafuer,
    // dass das Fenster wirklich die ganze Anzeige bedeckt, passend zur Polsterung von oben.
    Dialog(
        onDismissRequest = { if (state == RecorderState.IDLE) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDark) DarkBg else LightBg
        ) {
            Column(Modifier.fillMaxSize().padding(top = topInset, bottom = bottomInset)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Neue Stimme aufzeichnen",
                        fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = state == RecorderState.IDLE,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Close, "Schließen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp))
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        "Name der Stimme",
                        fontFamily = JetBrainsMono, fontSize = 12.sp, letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    DialogTextField(
                        value = name,
                        onValueChange = { name = it },
                        hint = "z. B. Frank Abend",
                        isDark = isDark,
                        textSize = 18
                    )
                    Spacer(Modifier.height(22.dp))
                    Text(
                        "Das liest du vor",
                        fontFamily = JetBrainsMono, fontSize = 12.sp, letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Ruhig und gleichmäßig bis zum Schluss — etwa 50 Sekunden.",
                        fontSize = 14.sp, lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    VoiceSampleScript.lines.forEachIndexed { index, line ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("${index + 1}", fontFamily = JetBrainsMono, fontSize = 17.sp,
                                color = Orange, modifier = Modifier.width(24.dp))
                            Text(line, fontSize = 21.sp, lineHeight = 31.sp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Column(
                    // 64 dp fester Abstand nach unten, KEINE Inset-Rechnung: das Dialog-Fenster
                    // reicht auf diesem Geraet unter den Bildschirmrand, dort kam jede aus den
                    // Insets berechnete Polsterung nicht an und der Knopf lag halb unter der
                    // Gestenleiste. Ein fester Wert hebt ihn zuverlaessig darueber.
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    note?.let { text ->
                        Text(
                            text, fontSize = 15.sp, lineHeight = 21.sp, color = Rose,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                    Text(
                        when (state) {
                            RecorderState.SAVING -> "Die Stimme wird erstellt …"
                            RecorderState.RECORDING -> "%d:%02d".format(seconds / 60, seconds % 60)
                            RecorderState.IDLE -> "Bereit"
                        },
                        fontFamily = JetBrainsMono,
                        fontSize = if (state == RecorderState.RECORDING) 30.sp else 16.sp,
                        color = if (state == RecorderState.RECORDING) Orange
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (state == RecorderState.RECORDING) Orange
                                else if (isDark) DarkField else LightField
                            )
                            .clickable(enabled = state != RecorderState.SAVING) {
                                when (state) {
                                    RecorderState.IDLE -> when {
                                        name.isBlank() ->
                                            note = "Bitte zuerst einen Namen für die Stimme eingeben."
                                        ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED -> startRecording()
                                        else -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                    RecorderState.RECORDING -> finishRecording()
                                    RecorderState.SAVING -> Unit
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (state == RecorderState.RECORDING) Icons.Default.Stop else Icons.Default.Mic,
                            if (state == RecorderState.RECORDING) "Aufnahme stoppen" else "Aufnahme starten",
                            tint = if (state == RecorderState.RECORDING) Color.White else Orange,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    isDark: Boolean,
    textSize: Int = 14
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (isDark) DarkField else LightField,
        border = BorderStroke(1.dp, if (isDark) DarkFieldBorder else LightFieldBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.padding(12.dp),
            textStyle = LocalTextStyle.current.copy(
                fontSize = textSize.sp,
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(hint, fontSize = textSize.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                inner()
            }
        )
    }
}
