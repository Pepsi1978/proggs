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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.hilt.navigation.compose.hiltViewModel
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

private const val SEPARATOR_KEY = "__separator__"

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
    }.getOrDefault(emptyList())
}

private fun serializeGewohnheiten(mentals: List<Mental>): String {
    val arr = JSONArray()
    for (m in mentals) {
        arr.put(JSONObject().put("id", m.id).put("text", m.text))
    }
    return arr.toString()
}

private val Accent: Color
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

    val suggestVm: GewohnheitSuggestViewModel = hiltViewModel()
    val suggestions by suggestVm.suggestions.collectAsStateWithLifecycle()
    val suggestState by suggestVm.state.collectAsStateWithLifecycle()

    val error by suggestVm.error.collectAsStateWithLifecycle()
    LaunchedEffect(error) {
        error?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            suggestVm.dismissError()
        }
    }

    val gewohnheitenStream = remember(context) { gewohnheitenFlow(context) }
    val stored by gewohnheitenStream.collectAsStateWithLifecycle(initialValue = emptyList())

    // Drag-Reihenfolge: ALLE IDs inkl. Separator (user + SEPARATOR_KEY + suggestions).
    var dragOrder by remember { mutableStateOf<List<String>?>(null) }

    // displayed = flache Liste: Reihenfolge aus dragOrder oder Default.
    val displayed: List<DisplayItem> = remember(stored, suggestions, dragOrder) {
        val storedIds = stored.map { it.id }.toSet()
        val allById = (stored + suggestions).associateBy { it.id }
        val order = dragOrder ?: (
            stored.map { it.id } + listOf(SEPARATOR_KEY) + suggestions.map { it.id }
        )
        order.mapNotNull { key ->
            when {
                key == SEPARATOR_KEY -> DisplayItem.Separator
                key in storedIds -> allById[key]?.let { DisplayItem.UserMental(it) }
                else -> allById[key]?.let { DisplayItem.SuggestionMental(it) }
            }
        }
    }

    // Reset dragOrder wenn sich stored oder suggestions ändern (nach Promotion/Demotion).
    LaunchedEffect(stored, suggestions) { dragOrder = null }

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Mental?>(null) }
    var micActionsOpen by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    // Separator-Swaps SIND erlaubt — der Separator bewegt sich mit. Erst in
    // onDragStopped wird geprueft ob ein Item den Separator tatsaechlich ueberquert hat.
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
        val toKey = to.key as? String ?: return@rememberReorderableLazyListState

        val currentIds = dragOrder ?: (
            stored.map { it.id } + listOf(SEPARATOR_KEY) + suggestions.map { it.id }
        )
        val list = currentIds.toMutableList()
        val fi = list.indexOf(fromKey)
        val ti = list.indexOf(toKey)
        if (fi in list.indices && ti in list.indices) {
            list.add(ti, list.removeAt(fi))
            dragOrder = list
        }
    }

    CosmosScaffold(
        title = "Gewohnheit",
        actions = {
            if (suggestState == SuggestState.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Accent,
                )
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = { suggestVm.generateSuggestions() }) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Gewohnheitsvorschläge aus Ideen",
                    tint = Accent,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
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
            modifier = Modifier.fillMaxSize()
                .background(if (cosmos.isDark) Color(0xFF12100D) else Color(0xFFFAF7F3))
                .padding(padding)
        ) {
            if (stored.isEmpty() && suggestions.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayed, key = { it.key }) { item ->
                        when (item) {
                            is DisplayItem.UserMental -> {
                                ReorderableItem(reorderState, key = item.key) { isDragging ->
                                    UserRow(
                                        mental = item.mental,
                                        isDragging = isDragging,
                                        onClick = { editTarget = item.mental },
                                        dragModifier = Modifier.longPressDraggableHandle(
                                            onDragStopped = {
                                                // Crossing-Detection: pruefe ob die Gewohnheit
                                                // nach dem Drag RECHTS vom Separator steht.
                                                val finalOrder = dragOrder
                                                if (finalOrder != null) {
                                                    val sepIdx = finalOrder.indexOf(SEPARATOR_KEY)
                                                    val itemIdx = finalOrder.indexOf(item.mental.id)
                                                    if (sepIdx >= 0 && itemIdx > sepIdx) {
                                                        // Demotion: Gewohnheit → Vorschlag
                                                        scope.launch {
                                                            deleteGewohnheit(context, item.mental.id)
                                                            suggestVm.addSuggestion(item.mental.text)
                                                        }
                                                    }
                                                }
                                                dragOrder = null
                                            },
                                        ),
                                    )
                                }
                            }
                            is DisplayItem.Separator -> {
                                // Separator als ReorderableItem damit Items hindurchgezogen werden
                                // koennen. Kein eigener Drag-Handle — er wird nur passiv verschoben.
                                ReorderableItem(reorderState, key = SEPARATOR_KEY) {
                                    SuggestionDivider()
                                }
                            }
                            is DisplayItem.SuggestionMental -> {
                                ReorderableItem(reorderState, key = item.key) { isDragging ->
                                    SuggestionRow(
                                        mental = item.mental,
                                        isDragging = isDragging,
                                        onAccept = {
                                            scope.launch {
                                                addGewohnheit(context, item.mental.text)
                                                suggestVm.acceptSuggestion(item.mental.id)
                                            }
                                        },
                                        onDelete = {
                                            scope.launch { suggestVm.deleteSuggestion(item.mental.id) }
                                        },
                                        dragModifier = Modifier.longPressDraggableHandle(
                                            onDragStopped = {
                                                // Crossing-Detection: pruefe ob der Vorschlag
                                                // nach dem Drag LINKS vom Separator steht.
                                                val finalOrder = dragOrder
                                                if (finalOrder != null) {
                                                    val sepIdx = finalOrder.indexOf(SEPARATOR_KEY)
                                                    val itemIdx = finalOrder.indexOf(item.mental.id)
                                                    if (sepIdx >= 0 && itemIdx in 0 until sepIdx) {
                                                        // Promotion: Vorschlag → Gewohnheit
                                                        scope.launch {
                                                            addGewohnheit(context, item.mental.text)
                                                            suggestVm.acceptSuggestion(item.mental.id)
                                                        }
                                                    }
                                                }
                                                dragOrder = null
                                            },
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MicCaptureActions(
                visible = micActionsOpen,
                accent = Accent,
                onTextCommit = { text, _ -> scope.launch { addGewohnheit(context, text) } },
                onClose = { micActionsOpen = false },
                onWriteClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showAddDialog) {
        EditDialog(
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
        EditDialog(
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

/* ============================== Display-Modell ============================== */

private sealed class DisplayItem {
    abstract val key: String
    data class UserMental(val mental: Mental) : DisplayItem() { override val key = mental.id }
    data object Separator : DisplayItem() { override val key = SEPARATOR_KEY }
    data class SuggestionMental(val mental: Mental) : DisplayItem() { override val key = mental.id }
}

/* ============================== Zeilen ============================== */

@Composable
private fun UserRow(
    mental: Mental,
    isDragging: Boolean,
    onClick: () -> Unit,
    dragModifier: Modifier,
) {
    val cosmos = LocalCosmos.current
    val cardBg = if (cosmos.isDark) Color(0xFF1D1A16) else Color.White
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .then(
                if (isDragging) Modifier.background(Accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable { onClick() }
            .then(dragModifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "●",
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = mental.text,
            style = MaterialTheme.typography.bodyLarge,
            color = cosmos.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SuggestionDivider() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(1.dp).background(Accent.copy(alpha = 0.35f))
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Gewohnheitsvorschläge — erkannte Vorschläge",
            style = MaterialTheme.typography.labelMedium,
            color = Accent,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun SuggestionRow(
    mental: Mental,
    isDragging: Boolean,
    onAccept: () -> Unit,
    onDelete: () -> Unit,
    dragModifier: Modifier,
) {
    val cosmos = LocalCosmos.current
    val cardBg = if (cosmos.isDark) Color(0xFF1D1A16) else Color.White
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .then(
                if (isDragging) Modifier.background(Accent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                else Modifier
            )
            .then(dragModifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "○",
                style = MaterialTheme.typography.labelLarge,
                color = Accent.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = mental.text,
            style = MaterialTheme.typography.bodyLarge,
            color = cosmos.textPrimary.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onAccept, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = "Als Gewohnheit übernehmen",
                tint = Accent,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Vorschlag verwerfen",
                tint = Color(0xFFE53935).copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    val cosmos = LocalCosmos.current
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(96.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(Accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = Accent,
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
            text = "Tippe unten auf das Mikrofon und waehle Schreiben oder Aufnehmen. " +
                "Oder tippe oben auf den Funken, um aus deinen Ideen Gewohnheitsvorschläge zu erhalten.",
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EditDialog(
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
                Icon(Icons.Outlined.Save, "Speichern", tint = Accent, modifier = Modifier.size(32.dp))
            }
        },
        dismissButton = {
            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Outlined.Delete, "Loeschen", tint = Color(0xFFE53935), modifier = Modifier.size(32.dp))
                }
            }
        },
    )
}
