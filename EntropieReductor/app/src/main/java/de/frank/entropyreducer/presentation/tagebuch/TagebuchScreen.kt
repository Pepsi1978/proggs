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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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

/**
 * Tagebuch-Bereich (Frank-Wunsch 2026-05-18) — Sub-Bereich 1 des Aufgaben-Tabs.
 *
 * Erste Iteration (MVP): Texteingabe ueber Stift-Button, Persistierung im DataStore als JSON-Liste,
 * Liste der Eintraege nach Datum sortiert, Detail-Sheet beim Tap auf einen Eintrag. Mikrofon-Button
 * ist als Platzhalter vorhanden — Whisper-Aufnahme, Gemini-Text-Verbesserung, TTS-Vorlesen und
 * Symbol-Kategorien folgen in einer naechsten Iteration (1:1-Portierung der
 * BestJournalFrank-Tagebuchfunktion ist umfangreich).
 *
 * Die App-Farbe folgt der Aufgaben-Akzentfarbe (orange) damit der Tab visuell in den
 * Aufgaben-Bereich passt. CosmosBottomBar laeuft im Sub-Mode mit forcedSubMode=TASKS.
 */
@Composable
fun TagebuchScreen(
    onBack: () -> Unit,
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
    onOpenEntry: (entryId: String) -> Unit = {},
) {
    val cosmos = LocalCosmos.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by tagebuchEntriesFlow(context).collectAsState(initial = emptyList())

    var inputDialogOpen by remember { mutableStateOf(false) }
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
    // Frank-Wunsch 2026-05-19: Auto-Ueberschrift via Gemini (max 3 Woerter).
    // Wird nach jedem neuen Eintrag und nach jedem Edit asynchron gesetzt.
    val titleVm: TagebuchTitleViewModel = hiltViewModel()
    // Frank-Wunsch 2026-05-20: Auto-Zusammenfassung (3-5 Bullet-Points) via Gemini.
    // Wird nach jedem neuen Eintrag asynchron erzeugt — Detail-Screen zeigt sie als
    // Bullet-Points statt eines leeren Platzhalters.
    val summaryVm: TagebuchSummaryViewModel = hiltViewModel()
    var pendingTranscript by remember { mutableStateOf<String?>(null) }
    val micPermission =
        rememberMicPermissionState(
            onAllGranted = {
                voiceVm.toggle { transcript ->
                    pendingTranscript = transcript
                    actionsExpanded = false
                }
            }
        )

    // Helper: Eintrag speichern und parallel den Auto-Titel via Gemini erzeugen.
    // Fallback-Titel bleibt bei Fehler (kein API-Key, kein Netz) bestehen.
    val saveNewEntry: (String) -> Unit = { rawText ->
        val entry = TagebuchEntry.create(rawText)
        scope.launch { addTagebuchEntry(context, entry) }
        titleVm.generateTitle(rawText) { newTitle ->
            scope.launch { updateTagebuchEntry(context, entry.id, title = newTitle) }
        }
        summaryVm.generateSummary(rawText) { bullets ->
            scope.launch { updateTagebuchEntry(context, entry.id, summary = bullets) }
        }
    }

    if (inputDialogOpen) {
        TextInputDialog(
            onDismiss = { inputDialogOpen = false },
            onSave = { text ->
                saveNewEntry(text)
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
                saveNewEntry(text)
                pendingTranscript = null
                improveVm.clearImproved()
            },
            onDismiss = {
                pendingTranscript = null
                improveVm.clearImproved()
            },
        )
    }
    // Detail wird jetzt als Vollbild-Screen ueber den NavGraph aufgerufen
    // (Frank-Wunsch 2026-05-20). Tap auf eine Eintrag-Karte navigiert direkt
    // dorthin — kein AlertDialog mehr.

    CosmosScaffold(
        title = "Tagebuch",
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState =
                    when (voiceState) {
                        VoiceCaptureState.RECORDING -> MicState.RECORDING
                        VoiceCaptureState.PROCESSING -> MicState.PROCESSING
                        VoiceCaptureState.IDLE -> MicState.IDLE
                    },
                onTabSelected = { route -> onSwitchTab(route) },
                // Frank-Wunsch 2026-05-18 (Folgeauftrag 3): Der untere Mic-
                // Button in der BottomBar ist der EINZIGE Mic-Knopf. Sein
                // Klick faltet die zwei Aktions-Buttons (Stift + Aufnehmen-Mic)
                // direkt darueber auf — vorher ist im Tagebuch-Bereich
                // KEIN Mikrofon zu sehen.
                onMicClick = { actionsExpanded = !actionsExpanded },
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
                        EntryCard(entry = e, onClick = { onOpenEntry(e.id) })
                    }
                }
            }

            // Frank-Wunsch 2026-05-18 (Folgeauftrag 3):
            // KEIN eigener Mic-FAB im Tagebuch-Bereich. Der BottomBar-Mic-
            // Button (in der Statusleiste unten) ist der einzige Mic-Trigger.
            // Sein Klick setzt actionsExpanded=true → es erscheinen direkt
            // ueber der BottomBar (rechts: Aufnehmen-Mic, links: Schreiben).
            // Standardmaessig ist im Tagebuch-Bereich NICHTS zu sehen.
            if (actionsExpanded) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
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
                    // A12: echte Whisper-Aufnahme via Groq Large V3 Turbo.
                    val (recordIcon, recordLabel) =
                        when (voiceState) {
                            VoiceCaptureState.IDLE -> Icons.Outlined.Mic to "Aufnehmen"
                            VoiceCaptureState.RECORDING -> Icons.Outlined.Stop to "Stop"
                            VoiceCaptureState.PROCESSING -> Icons.Outlined.Mic to "Transkribiere…"
                        }
                    FabIconButton(
                        icon = recordIcon,
                        label = recordLabel,
                        backgroundColor =
                            if (voiceState == VoiceCaptureState.RECORDING)
                                Color(0xFFE53935).copy(alpha = 0.22f)
                            else TagebuchAccent.copy(alpha = 0.18f),
                        iconTint =
                            if (voiceState == VoiceCaptureState.RECORDING) Color(0xFFE53935)
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
                Modifier.size(56.dp).clip(CircleShape).background(backgroundColor).clickable {
                    onClick()
                },
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
                    Text("Beschreibe deine innere, mentale oder emotionale Entropie ...")
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

/**
 * Detail-Dialog fuer einen Tagebucheintrag.
 *
 * Frank-Wunsch 2026-05-19:
 * - Text ist bearbeitbar (OutlinedTextField) — Fehler im Eintrag koennen nachtraeglich korrigiert
 *   werden.
 * - Rechts oben ein 3-Punkte-Menue mit "Eintrag loeschen".
 * - Speichern-Button erscheint nur wenn der Text geaendert wurde.
 */
@Composable
private fun EntryDetailDialog(
    entry: TagebuchEntry,
    onDismiss: () -> Unit,
    onSave: (updatedText: String) -> Unit,
    onDelete: () -> Unit,
) {
    var editableText by remember(entry.id) { mutableStateOf(entry.text) }
    var menuOpen by remember { mutableStateOf(false) }
    val hasChanges = editableText.trim() != entry.text.trim() && editableText.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.title.ifBlank { "Tagebucheintrag" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                )
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "Mehr")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Eintrag löschen") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFE53935),
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
        text = {
            Column {
                Text(
                    text = formatTimestamp(entry.timestampMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalCosmos.current.textSecondary,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editableText,
                    onValueChange = { editableText = it },
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
        },
        confirmButton = {
            if (hasChanges) {
                TextButton(
                    onClick = { onSave(editableText.trim()) },
                    enabled = editableText.isNotBlank(),
                ) {
                    Text("Speichern", color = TagebuchAccent)
                }
            } else {
                TextButton(onClick = onDismiss) { Text("Schließen") }
            }
        },
        dismissButton = {
            if (hasChanges) {
                TextButton(onClick = onDismiss) { Text("Abbrechen") }
            }
        },
    )
}

/* ============================== Datenmodell ============================== */

data class TagebuchEntry(
    val id: String,
    val timestampMs: Long,
    val title: String,
    val text: String,
    /**
     * Nachträge zum Eintrag (Frank-Wunsch 2026-05-20). Werden im Vollbild- Detail-Screen als eigene
     * Karten angezeigt. Bei alten Einträgen ohne dieses Feld liefert der JSON-Parser eine leere
     * Liste.
     */
    val followups: List<TagebuchFollowup> = emptyList(),
    /**
     * KI-generierte Bullet-Point-Zusammenfassung (Frank-Wunsch 2026-05-20). Eine Zeile pro
     * Bullet-Point, beginnt mit "• ". `null` = noch keine Zusammenfassung erstellt — der Detail-
     * Screen zeigt dann einen Knopf "Mit KI zusammenfassen". Wird automatisch bei neuen Einträgen
     * erzeugt sobald Gemini antwortet.
     */
    val summary: String? = null,
) {
    companion object {
        fun create(text: String): TagebuchEntry {
            // Title = erste 30-40 Zeichen oder erste Zeile, ohne Punkt-Suffix.
            val firstLine = text.lineSequence().firstOrNull().orEmpty()
            val title =
                if (firstLine.length <= 40) firstLine.trim() else firstLine.take(40).trim() + "…"
            return TagebuchEntry(
                id = UUID.randomUUID().toString(),
                timestampMs = System.currentTimeMillis(),
                title = title,
                text = text,
                followups = emptyList(),
            )
        }
    }
}

/** Einzelner Nachtrag zu einem [TagebuchEntry]. */
data class TagebuchFollowup(val id: String, val createdAtMs: Long, val text: String)

private val Context.tagebuchStore by preferencesDataStore(name = "tagebuch_entries")
private val KEY_ENTRIES = stringPreferencesKey("entries_json")

internal fun tagebuchEntriesFlow(context: Context): Flow<List<TagebuchEntry>> =
    context.tagebuchStore.data.map { prefs ->
        val raw = prefs[KEY_ENTRIES] ?: return@map emptyList()
        runCatching {
                val arr = JSONArray(raw)
                buildList(arr.length()) {
                    for (i in 0 until arr.length()) add(jsonToEntry(arr.getJSONObject(i)))
                }
            }
            .getOrDefault(emptyList())
            .sortedByDescending { it.timestampMs }
    }

/** Beobachtbarer Flow eines einzelnen Eintrags — für den Vollbild-Detail-Screen. */
internal fun tagebuchEntryFlow(context: Context, id: String): Flow<TagebuchEntry?> =
    tagebuchEntriesFlow(context).map { list -> list.firstOrNull { it.id == id } }

internal suspend fun addTagebuchEntry(context: Context, entry: TagebuchEntry) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing + entry
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
}

internal suspend fun deleteTagebuchEntry(context: Context, id: String) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.filterNot { it.id == id }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
}

/**
 * Aktualisiert Text und/oder Titel eines bestehenden Eintrags. Felder die als `null` uebergeben
 * werden, bleiben unveraendert. Wird sowohl vom Edit-Dialog (Text-Aenderung) als auch vom
 * Gemini-Auto-Titel (Title-Aenderung) genutzt — daher die optionalen Parameter.
 */
internal suspend fun updateTagebuchEntry(
    context: Context,
    id: String,
    text: String? = null,
    title: String? = null,
    summary: String? = null,
) {
    if (text == null && title == null && summary == null) return
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id == id) {
                e.copy(
                    text = text ?: e.text,
                    title = title ?: e.title,
                    summary = summary ?: e.summary,
                )
            } else {
                e
            }
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
}

/**
 * Hängt einen Nachtrag an einen Tagebucheintrag an (Frank-Wunsch 2026-05-20). Der Nachtrag wird im
 * Detail-Screen als eigene Karte angezeigt.
 */
internal suspend fun addTagebuchFollowup(
    context: Context,
    entryId: String,
    followup: TagebuchFollowup,
) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id == entryId) e.copy(followups = e.followups + followup) else e
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
}

internal suspend fun updateTagebuchFollowup(
    context: Context,
    entryId: String,
    followupId: String,
    newText: String,
) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id != entryId) return@map e
            e.copy(
                followups =
                    e.followups.map { f -> if (f.id == followupId) f.copy(text = newText) else f }
            )
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
}

internal suspend fun deleteTagebuchFollowup(context: Context, entryId: String, followupId: String) {
    context.tagebuchStore.edit { prefs ->
        val existing = parseEntries(prefs[KEY_ENTRIES])
        val updated = existing.map { e ->
            if (e.id != entryId) e
            else e.copy(followups = e.followups.filterNot { it.id == followupId })
        }
        prefs[KEY_ENTRIES] = serializeEntries(updated)
    }
}

private fun parseEntries(raw: String?): List<TagebuchEntry> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) add(jsonToEntry(arr.getJSONObject(i)))
            }
        }
        .getOrDefault(emptyList())
}

private fun jsonToEntry(o: JSONObject): TagebuchEntry =
    TagebuchEntry(
        id = o.optString("id"),
        timestampMs = o.optLong("ts"),
        title = o.optString("title"),
        text = o.optString("text"),
        followups = jsonToFollowups(o.optJSONArray("followups")),
        summary = o.optString("summary").takeIf { it.isNotBlank() },
    )

private fun jsonToFollowups(arr: JSONArray?): List<TagebuchFollowup> {
    if (arr == null) return emptyList()
    return buildList(arr.length()) {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            add(
                TagebuchFollowup(
                    id = o.optString("id"),
                    createdAtMs = o.optLong("ts"),
                    text = o.optString("text"),
                )
            )
        }
    }
}

private fun serializeEntries(entries: List<TagebuchEntry>): String {
    val arr = JSONArray()
    for (e in entries) {
        val o = JSONObject()
        o.put("id", e.id)
        o.put("ts", e.timestampMs)
        o.put("title", e.title)
        o.put("text", e.text)
        if (e.followups.isNotEmpty()) {
            val fArr = JSONArray()
            for (f in e.followups) {
                val fo = JSONObject()
                fo.put("id", f.id)
                fo.put("ts", f.createdAtMs)
                fo.put("text", f.text)
                fArr.put(fo)
            }
            o.put("followups", fArr)
        }
        if (!e.summary.isNullOrBlank()) {
            o.put("summary", e.summary)
        }
        arr.put(o)
    }
    return arr.toString()
}

internal fun formatTagebuchTimestamp(ts: Long): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
    return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", Locale.GERMANY))
}

private fun formatTimestamp(ts: Long): String = formatTagebuchTimestamp(ts)

/** Akzentfarbe — Frank-Wunsch: gleiche Farbe wie der Aufgaben-Tab (Orange). */
private val TagebuchAccent: Color = Color(0xFFFF9800)
