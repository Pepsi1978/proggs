package de.frank.entropyreducer.presentation.mental

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.MicCaptureActions
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Gewohnheitsboard (Frank-Wunsch 2026-06-17): 1:1-Klon des Mentalboards OHNE Vorlesefunktion
 * (kein Lautsprecher, kein "Erster Satz", kein "Folgesatz", kein Endlos-Haekchen).
 * Liegt im Aufgaben-Bereich auf Sub-Reiter 1 ("Gewohnheit").
 *
 * Bedienung identisch zum Mentalboard:
 * - Mic-Button in der BottomBar oeffnet die Auswahl "Schreiben" / "Aufnehmen".
 * - Tap auf einen Satz oeffnet einen Dialog zum Editieren ODER Loeschen.
 * - Langes Druecken startet Drag & Drop zum Umsortieren.
 *
 * Persistenz: DataStore (Datei "gewohnheit_board"), exakt analog zum Mentalboard.
 */

private val Context.gewohnheitStore by preferencesDataStore(name = "gewohnheit_board")
private val KEY_GEWOHNHEITEN = stringPreferencesKey("gewohnheiten_json")

internal fun gewohnheitenFlow(context: Context): Flow<List<Mental>> =
    context.gewohnheitStore.data.map { prefs -> parseGewohnheiten(prefs[KEY_GEWOHNHEITEN]) }

internal suspend fun addGewohnheit(context: Context, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    context.gewohnheitStore.edit { prefs ->
        val existing = parseGewohnheiten(prefs[KEY_GEWOHNHEITEN])
        prefs[KEY_GEWOHNHEITEN] = serializeGewohnheiten(existing + Mental.create(clean))
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context)
}

internal suspend fun updateGewohnheit(context: Context, id: String, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    context.gewohnheitStore.edit { prefs ->
        val existing = parseGewohnheiten(prefs[KEY_GEWOHNHEITEN])
        prefs[KEY_GEWOHNHEITEN] =
            serializeGewohnheiten(existing.map { if (it.id == id) it.copy(text = clean) else it })
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context)
}

internal suspend fun deleteGewohnheit(context: Context, id: String) {
    context.gewohnheitStore.edit { prefs ->
        val existing = parseGewohnheiten(prefs[KEY_GEWOHNHEITEN])
        prefs[KEY_GEWOHNHEITEN] = serializeGewohnheiten(existing.filterNot { it.id == id })
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context)
}

internal suspend fun reorderGewohnheiten(context: Context, newOrder: List<Mental>) {
    context.gewohnheitStore.edit { prefs -> prefs[KEY_GEWOHNHEITEN] = serializeGewohnheiten(newOrder) }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context)
}

internal suspend fun restoreGewohnheiten(context: Context, incoming: List<Mental>): Int {
    if (incoming.isEmpty()) return 0
    var added = 0
    context.gewohnheitStore.edit { prefs ->
        val existing = parseGewohnheiten(prefs[KEY_GEWOHNHEITEN])
        val existingIds = existing.mapTo(HashSet()) { it.id }
        val toAdd = incoming.filterNot { it.id in existingIds }
        added = toAdd.size
        if (toAdd.isNotEmpty()) prefs[KEY_GEWOHNHEITEN] = serializeGewohnheiten(existing + toAdd)
    }
    return added
}

private fun parseGewohnheiten(raw: String?): List<Mental> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(Mental(id = id, text = o.optString("text")))
                }
            }
        }
        .getOrDefault(emptyList())
}

private fun serializeGewohnheiten(mentals: List<Mental>): String {
    val arr = JSONArray()
    for (m in mentals) {
        arr.put(JSONObject().put("id", m.id).put("text", m.text))
    }
    return arr.toString()
}

/* Akzentfarbe — identisch zum Mentalboard (Aufgaben-Orange). */
internal val GewohnheitAccent: Color
    @Composable get() = LocalCosmos.current.accentTasks

/* ============================== UI ============================== */

@Composable
fun GewohnheitBoardScreen(
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val gewohnheitenStream = remember(context) { gewohnheitenFlow(context) }
    val stored by gewohnheitenStream.collectAsStateWithLifecycle(initialValue = emptyList())

    var dragOrder by remember { mutableStateOf<List<Mental>?>(null) }
    val displayed = dragOrder ?: stored

    LaunchedEffect(stored) {
        val d = dragOrder ?: return@LaunchedEffect
        val dIds = d.map { it.id }
        val sIds = stored.map { it.id }
        if (dIds == sIds || dIds.toSet() != sIds.toSet()) {
            dragOrder = null
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Mental?>(null) }
    var micActionsOpen by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val reorderState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromId = from.key as? String ?: return@rememberReorderableLazyListState
            val toId = to.key as? String ?: return@rememberReorderableLazyListState
            val list = (dragOrder ?: stored).toMutableList()
            val fromIdx = list.indexOfFirst { it.id == fromId }
            val toIdx = list.indexOfFirst { it.id == toId }
            if (fromIdx in list.indices && toIdx in list.indices) {
                list.add(toIdx, list.removeAt(fromIdx))
                dragOrder = list
            }
        }

    CosmosScaffold(
        title = "Gewohnheit",
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState = MicState.IDLE,
                onTabSelected = { route -> onSwitchTab(route) },
                onMicClick = { micActionsOpen = !micActionsOpen },
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = Routes.TASKS,
                selectedSubIndex = 1,
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(if (cosmos.isDark) Color(0xFF12100D) else Color(0xFFFAF7F3))
                    .padding(padding)
        ) {
            if (displayed.isEmpty()) {
                GewohnheitEmptyState()
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayed, key = { it.id }) { gewohnheit ->
                        ReorderableItem(reorderState, key = gewohnheit.id) { isDragging ->
                            val position = displayed.indexOfFirst { it.id == gewohnheit.id } + 1
                            GewohnheitRow(
                                position = position,
                                text = gewohnheit.text,
                                isDragging = isDragging,
                                onClick = { editTarget = gewohnheit },
                                dragModifier =
                                    Modifier.longPressDraggableHandle(
                                        onDragStopped = {
                                            dragOrder?.let { order ->
                                                scope.launch { reorderGewohnheiten(context, order) }
                                            }
                                        }
                                    ),
                            )
                        }
                    }
                }
            }

            MicCaptureActions(
                visible = micActionsOpen,
                accent = GewohnheitAccent,
                onTextCommit = { text, _ -> scope.launch { addGewohnheit(context, text) } },
                onClose = { micActionsOpen = false },
                onWriteClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showAddDialog) {
        GewohnheitEditDialog(
            initialText = "",
            title = "Neue Gewohnheit",
            onDismiss = { showAddDialog = false },
            onSave = { text ->
                scope.launch { addGewohnheit(context, text) }
                showAddDialog = false
            },
            onDelete = null,
        )
    }

    editTarget?.let { target ->
        GewohnheitEditDialog(
            initialText = target.text,
            title = "Gewohnheit bearbeiten",
            onDismiss = { editTarget = null },
            onSave = { text ->
                scope.launch { updateGewohnheit(context, target.id, text) }
                editTarget = null
            },
            onDelete = {
                scope.launch { deleteGewohnheit(context, target.id) }
                editTarget = null
            },
        )
    }
}

@Composable
private fun GewohnheitRow(
    position: Int,
    text: String,
    isDragging: Boolean,
    onClick: () -> Unit,
    dragModifier: Modifier,
) {
    val cosmos = LocalCosmos.current
    val cardBg = if (cosmos.isDark) Color(0xFF1D1A16) else Color.White
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .then(
                    if (isDragging) {
                        Modifier.background(
                            GewohnheitAccent.copy(alpha = 0.12f),
                            RoundedCornerShape(14.dp),
                        )
                    } else {
                        Modifier
                    }
                )
                .clickable { onClick() }
                .then(dragModifier)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.size(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(GewohnheitAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$position",
                style = MaterialTheme.typography.labelLarge,
                color = GewohnheitAccent,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GewohnheitEmptyState() {
    val cosmos = LocalCosmos.current
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier.size(96.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(GewohnheitAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = GewohnheitAccent,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Noch keine Gewohnheiten",
            style = MaterialTheme.typography.headlineSmall,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                "Tippe unten auf das Mikrofon und waehle Schreiben oder Aufnehmen. " +
                    "Spaeter kannst du Saetze per langem Druecken frei sortieren.",
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GewohnheitEditDialog(
    initialText: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var text by remember { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Satz eintippen …") },
                singleLine = false,
                minLines = 2,
            )
        },
        confirmButton = {
            IconButton(
                onClick = { if (text.isNotBlank()) onSave(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = "Speichern",
                    tint = GewohnheitAccent,
                    modifier = Modifier.size(32.dp),
                )
            }
        },
        dismissButton = {
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Loeschen",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        },
    )
}
