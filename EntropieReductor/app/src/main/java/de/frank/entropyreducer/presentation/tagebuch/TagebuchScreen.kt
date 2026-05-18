package de.frank.entropyreducer.presentation.tagebuch

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.components.VoiceCaptureState
import de.frank.entropyreducer.presentation.components.VoiceCaptureViewModel
import de.frank.entropyreducer.presentation.components.rememberMicPermissionState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.runtime.rememberCoroutineScope

/**
 * Tagebuch-Bereich (Frank-Wunsch 2026-05-18) — Sub-Bereich 1 des Aufgaben-Tabs.
 *
 * Erste Iteration (MVP): Texteingabe ueber Stift-Button, Persistierung im
 * DataStore als JSON-Liste, Liste der Eintraege nach Datum sortiert,
 * Detail-Sheet beim Tap auf einen Eintrag. Mikrofon-Button ist als
 * Platzhalter vorhanden — Whisper-Aufnahme, Gemini-Text-Verbesserung,
 * TTS-Vorlesen und Symbol-Kategorien folgen in einer naechsten Iteration
 * (1:1-Portierung der BestJournalFrank-Tagebuchfunktion ist umfangreich).
 *
 * Die App-Farbe folgt der Aufgaben-Akzentfarbe (orange) damit der Tab
 * visuell in den Aufgaben-Bereich passt. CosmosBottomBar laeuft im
 * Sub-Mode mit forcedSubMode=TASKS.
 */
@Composable
fun TagebuchScreen(
    onBack: () -> Unit,
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by tagebuchEntriesFlow(context).collectAsState(initial = emptyList())

    var inputDialogOpen by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<TagebuchEntry?>(null) }
    // Frank-Wunsch 2026-05-18 Folgeauftrag: Erst nur Mic-Button anzeigen.
    // Klick auf Mic legt rechts "Aufnehmen" und links "Schreiben" frei.
    var actionsExpanded by remember { mutableStateOf(false) }
    // A12: echte Whisper-Aufnahme via Groq + Gemini-Text-Verbesserung.
    val voiceVm: VoiceCaptureViewModel = hiltViewModel()
    val voiceState by voiceVm.state.collectAsState()
    val voiceError by voiceVm.error.collectAsState()
    val improveVm: TagebuchImproveViewModel = hiltViewModel()
    val improveState by improveVm.state.collectAsState()
    val improvedText by improveVm.improvedText.collectAsState()
    val improveError by improveVm.error.collectAsState()
    var pendingTranscript by remember { mutableStateOf<String?>(null) }
    val micPermission = rememberMicPermissionState(
        onAllGranted = {
            voiceVm.toggle { transcript ->
                pendingTranscript = transcript
                actionsExpanded = false
            }
        },
    )

    if (inputDialogOpen) {
        TextInputDialog(
            onDismiss = { inputDialogOpen = false },
            onSave = { text ->
                scope.launch {
                    addTagebuchEntry(context, TagebuchEntry.create(text))
                }
                inputDialogOpen = false
            },
        )
    }
    // A12: Transkript-Dialog nach Whisper-Aufnahme — Frank kann den Text
    // editieren, optional via Gemini verbessern lassen und speichern.
    val pending = pendingTranscript
    if (pending != null) {
        TranscriptDialog(
            initialText = pending,
            improvedText = improvedText,
            isImproving = improveState == ImproveState.RUNNING,
            errorMessage = improveError ?: voiceError,
            onImprove = { text -> improveVm.improve(text) },
            onSave = { text ->
                scope.launch {
                    addTagebuchEntry(context, TagebuchEntry.create(text))
                }
                pendingTranscript = null
                improveVm.clearImproved()
            },
            onDismiss = {
                pendingTranscript = null
                improveVm.clearImproved()
            },
        )
    }
    val current = selectedEntry
    if (current != null) {
        EntryDetailDialog(
            entry = current,
            onDismiss = { selectedEntry = null },
            onDelete = {
                scope.launch { deleteTagebuchEntry(context, current.id) }
                selectedEntry = null
            },
        )
    }

    CosmosScaffold(
        title = "Tagebuch",
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState = MicState.IDLE,
                onTabSelected = { route -> onSwitchTab(route) },
                onMicClick = { /* siehe FAB unten */ },
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = Routes.TASKS,
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Book,
                        contentDescription = null,
                        tint = TagebuchAccent,
                        modifier = Modifier.size(64.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Dein Tagebuch",
                        style = MaterialTheme.typography.titleLarge,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text =
                            "Hier kannst du innere, mentale und emotionale Entropie aufschreiben. " +
                                "Tippe auf den Stift um deinen ersten Eintrag zu erstellen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cosmos.textSecondary,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 120.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items = entries, key = { it.id }) { e ->
                        EntryCard(entry = e, onClick = { selectedEntry = e })
                    }
                }
            }

            // Frank-Wunsch 2026-05-18 Folgeauftrag:
            // - Standard: nur Mikrofon-Button zentriert sichtbar.
            // - Klick auf Mic blendet "Schreiben" (links) und "Aufnehmen"
            //   (rechts) ein. Schreiben oeffnet den Text-Dialog,
            //   Aufnehmen startet die Whisper-Aufnahme (siehe A12).
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (actionsExpanded) {
                    FabIconButton(
                        icon = Icons.Outlined.Edit,
                        label = "Schreiben",
                        backgroundColor = TagebuchAccent.copy(alpha = 0.18f),
                        iconTint = TagebuchAccent,
                        onClick = {
                            inputDialogOpen = true
                            actionsExpanded = false
                        },
                    )
                }
                FabIconButton(
                    icon = Icons.Outlined.Mic,
                    label = "Mikrofon",
                    backgroundColor = TagebuchAccent.copy(alpha = 0.18f),
                    iconTint = TagebuchAccent,
                    onClick = { actionsExpanded = !actionsExpanded },
                )
                if (actionsExpanded) {
                    // A12: echte Whisper-Aufnahme via Groq Large V3 Turbo.
                    // Permission wird beim ersten Tap erfragt; Voice-State
                    // (IDLE/RECORDING/PROCESSING) bestimmt Icon + Verhalten.
                    val (recordIcon, recordLabel) = when (voiceState) {
                        VoiceCaptureState.IDLE ->
                            Icons.Outlined.Mic to "Aufnehmen"
                        VoiceCaptureState.RECORDING ->
                            Icons.Outlined.Stop to "Stop"
                        VoiceCaptureState.PROCESSING ->
                            Icons.Outlined.Mic to "Transkribiere…"
                    }
                    FabIconButton(
                        icon = recordIcon,
                        label = recordLabel,
                        backgroundColor =
                            if (voiceState == VoiceCaptureState.RECORDING)
                                Color(0xFFE53935).copy(alpha = 0.22f)
                            else TagebuchAccent.copy(alpha = 0.18f),
                        iconTint =
                            if (voiceState == VoiceCaptureState.RECORDING)
                                Color(0xFFE53935)
                            else TagebuchAccent,
                        onClick = {
                            when (voiceState) {
                                VoiceCaptureState.IDLE -> {
                                    if (micPermission.check()) {
                                        voiceVm.toggle { transcript ->
                                            pendingTranscript = transcript
                                            actionsExpanded = false
                                        }
                                    } else {
                                        micPermission.request()
                                    }
                                }
                                VoiceCaptureState.RECORDING -> {
                                    voiceVm.toggle { transcript ->
                                        pendingTranscript = transcript
                                        actionsExpanded = false
                                    }
                                }
                                VoiceCaptureState.PROCESSING -> Unit
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: TagebuchEntry, onClick: () -> Unit) {
    val cosmos = LocalCosmos.current
    GlassCard(modifier = Modifier.fillMaxWidth().clickable { onClick() }) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier.size(36.dp)
                            .clip(CircleShape)
                            .background(TagebuchAccent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Book,
                        contentDescription = null,
                        tint = TagebuchAccent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = entry.title.ifBlank { "Tagebucheintrag" },
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatTimestamp(entry.timestampMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = cosmos.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = entry.text,
                style = MaterialTheme.typography.bodyMedium,
                color = cosmos.textPrimary,
                maxLines = 4,
            )
        }
    }
}

@Composable
private fun FabIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier.size(56.dp)
                    .clip(CircleShape)
                    .background(backgroundColor)
                    .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
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
private fun TextInputDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Tagebucheintrag") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = {
                    Text(
                        "Beschreibe deine innere, mentale oder emotionale Entropie ...",
                    )
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onSave(text.trim()) },
                enabled = text.isNotBlank(),
            ) {
                Text("Speichern", color = TagebuchAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun TranscriptDialog(
    initialText: String,
    improvedText: String?,
    isImproving: Boolean,
    errorMessage: String?,
    onImprove: (String) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editableText by remember(initialText) { mutableStateOf(initialText) }
    var useImproved by remember { mutableStateOf(false) }
    val activeText = if (useImproved && improvedText != null) improvedText else editableText
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag von der Aufnahme") },
        text = {
            Column {
                if (improvedText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (useImproved) "Verbessert" else "Original",
                            style = MaterialTheme.typography.labelMedium,
                            color = TagebuchAccent,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.size(8.dp))
                        TextButton(onClick = { useImproved = !useImproved }) {
                            Text(if (useImproved) "Original zeigen" else "Verbessert zeigen")
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedTextField(
                    value = activeText,
                    onValueChange = {
                        if (useImproved) {
                            // Verbesserter Text war bisher read-only — bei Tippen
                            // schalten wir zurueck auf Original-Edit.
                            useImproved = false
                            editableText = it
                        } else {
                            editableText = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )
                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE53935),
                    )
                }
                if (improvedText == null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { onImprove(editableText) },
                            enabled = !isImproving && editableText.isNotBlank(),
                        ) {
                            if (isImproving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Wird verbessert…")
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(6.dp))
                                Text("Text verbessern (Gemini)")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (activeText.isNotBlank()) onSave(activeText.trim()) },
                enabled = activeText.isNotBlank(),
            ) {
                Text("Speichern", color = TagebuchAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun EntryDetailDialog(
    entry: TagebuchEntry,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.title.ifBlank { "Tagebucheintrag" }) },
        text = {
            Column {
                Text(
                    text = formatTimestamp(entry.timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalCosmos.current.textSecondary,
                )
                Spacer(Modifier.height(8.dp))
                Text(text = entry.text, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("Löschen", color = Color(0xFFE53935))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Schließen") } },
    )
}

/* ============================== Datenmodell ============================== */

data class TagebuchEntry(
    val id: String,
    val timestampMs: Long,
    val title: String,
    val text: String,
) {
    companion object {
        fun create(text: String): TagebuchEntry {
            // Title = erste 30-40 Zeichen oder erste Zeile, ohne Punkt-Suffix.
            val firstLine = text.lineSequence().firstOrNull().orEmpty()
            val title =
                if (firstLine.length <= 40) firstLine.trim()
                else firstLine.take(40).trim() + "…"
            return TagebuchEntry(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis(),
                title = title,
                text = text,
            )
        }
    }
}

private val Context.tagebuchStore by preferencesDataStore(name = "tagebuch_entries")
private val KEY_ENTRIES = stringPreferencesKey("entries_json")

private fun tagebuchEntriesFlow(context: Context): Flow<List<TagebuchEntry>> =
    context.tagebuchStore.data.map { prefs ->
        val raw = prefs[KEY_ENTRIES] ?: return@map emptyList()
        runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        TagebuchEntry(
                            id = o.optString("id"),
                            timestampMs = o.optLong("ts"),
                            title = o.optString("title"),
                            text = o.optString("text"),
                        ),
                    )
                }
            }
        }
            .getOrDefault(emptyList())
            .sortedByDescending { it.timestampMs }
    }

private suspend fun addTagebuchEntry(context: Context, entry: TagebuchEntry) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing + entry
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
}

private suspend fun deleteTagebuchEntry(context: Context, id: String) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.filterNot { it.id == id }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
}

private fun parseEntries(raw: String?): List<TagebuchEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    TagebuchEntry(
                        id = o.optString("id"),
                        timestampMs = o.optLong("ts"),
                        title = o.optString("title"),
                        text = o.optString("text"),
                    ),
                )
            }
        }
    }
        .getOrDefault(emptyList())
}

private fun serializeEntries(entries: List<TagebuchEntry>): String {
    val arr = JSONArray()
    for (e in entries) {
        val o = JSONObject()
        o.put("id", e.id)
        o.put("ts", e.timestampMs)
        o.put("title", e.title)
        o.put("text", e.text)
        arr.put(o)
    }
    return arr.toString()
}

private fun formatTimestamp(ts: Long): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", Locale.GERMANY))
}

/** Akzentfarbe — Frank-Wunsch: gleiche Farbe wie der Aufgaben-Tab (Orange). */
private val TagebuchAccent: Color = Color(0xFFFF9800)
