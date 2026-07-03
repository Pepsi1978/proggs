package de.frank.entropyreducer.presentation.mental

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.LocalMicActionsOpen
import de.frank.entropyreducer.presentation.components.MicCaptureActions
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val SEPARATOR_KEY = "__separator__"

private val Context.gewohnheitStore by preferencesDataStore(name = "gewohnheit_board")
private val KEY_GEWOHNHEITEN = stringPreferencesKey("gewohnheiten_json")

// ID-Architektur Etappe 3c (Frank-Wunsch 2026-06-19): Gewohnheiten liegen jetzt in Room (Tabelle
// habits, sortiert nach `position`) statt im DataStore-JSON. Signaturen bleiben 1:1 — UI, Backup
// (gewohnheitenFlow) und Sync (restoreGewohnheiten) laufen unveraendert weiter; der DAO kommt per
// Hilt-@EntryPoint (habitDaoFrom). Der DataStore-JSON bleibt als Fallback fuer den Migrator (3b).
internal fun gewohnheitenFlow(context: Context): Flow<List<Mental>> =
    de.frank.entropyreducer.data.local
        .habitDaoFrom(context)
        .getAll()
        .map { rows -> rows.map { Mental(id = it.id, text = it.text, updatedAt = it.updatedAt) } }
        .distinctUntilChanged()

/**
 * Liest die Bestands-Gewohnheiten aus dem alten DataStore-JSON ("gewohnheit_board"). NUR fuer den
 * einmaligen Room-Migrator (HabitRoomMigrator, Etappe 3b) — NICHT fuer UI/Backup/Sync. Bleibt
 * JSON-basiert, auch wenn gewohnheitenFlow in Etappe 3c auf Room umgestellt wird (eingefrorener
 * Alt-Stand, sonst laese der Migrator seine eigene Zielquelle).
 */
internal fun gewohnheitenFromJsonFlow(context: Context): Flow<List<Mental>> =
    context.gewohnheitStore.data.map { prefs -> parseGewohnheiten(prefs[KEY_GEWOHNHEITEN]) }

internal suspend fun addGewohnheit(
    context: Context,
    text: String,
    // ID-Architektur Etappe 3d: Herkunft, falls die Gewohnheit aus einem Vorgaenger entsteht
    // (angenommener Gewohnheits-Vorschlag). Default null = direkt eingegebene Gewohnheit.
    originId: String? = null,
    originType: String? = null,
    rootId: String? = null,
) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    val dao = de.frank.entropyreducer.data.local.habitDaoFrom(context)
    val m = Mental.create(clean)
    dao.upsert(
        de.frank.entropyreducer.data.local.entities.HabitEntity(
            id = m.id,
            text = m.text,
            updatedAt = m.updatedAt,
            position = dao.maxPosition() + 1, // neue Gewohnheit ans Ende
            originId = originId,
            originType = originType,
            rootId = rootId,
        )
    )
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Gewohnheit-Reiter: Aenderung")
}

/**
 * Nimmt einen Gewohnheits-Vorschlag an -> legt eine Gewohnheit MIT Herkunft an (Etappe 3d), damit
 * die Kette Idee -> Gewohnheits-Vorschlag -> Gewohnheit luckenlos ist. rootId erbt die urspruengliche
 * Idee. Den Vorschlag selbst entfernt der Aufrufer separat (suggestVm.acceptSuggestion).
 */
internal suspend fun addGewohnheitFromSuggestion(context: Context, suggestionId: String, text: String) {
    val sug = de.frank.entropyreducer.data.local.habitSuggestionDaoFrom(context).getById(suggestionId)
    val expectedRoot = sug?.rootId ?: sug?.id
    addGewohnheit(
        context,
        text,
        originId = sug?.id,
        originType =
            sug?.let { de.frank.entropyreducer.data.local.entities.OriginType.HABIT_SUGGESTION },
        rootId = expectedRoot,
    )
    // Accept-Live-Sonde (Frank-Wunsch 2026-06-20): bestaetigt den Herkunfts-Uebergang
    // Gewohnheits-Vorschlag -> Gewohnheit live (erwartet vs. tatsaechlich). ok=false faellt im Log auf.
    de.frank.entropyreducer.data.diagnostics.Diag.i(
        de.frank.entropyreducer.data.diagnostics.DiagnosticArea.AGENTIC,
        "AcceptLineage",
        "CHECKPOINT Annehmen Gewohnheit '${text.take(28)}': erwartet rootId=${expectedRoot ?: "-"} " +
            "tatsaechlich originId=${sug?.id ?: "NULL!"} originType=HABIT_SUGGESTION " +
            "rootId=${expectedRoot ?: "NULL!"} ok=${expectedRoot != null}",
    )
}

internal suspend fun updateGewohnheit(context: Context, id: String, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    val dao = de.frank.entropyreducer.data.local.habitDaoFrom(context)
    val current = dao.getById(id) ?: return
    // copy erhaelt position + Herkunft, aktualisiert nur Text + Zeitstempel.
    dao.update(current.copy(text = clean, updatedAt = System.currentTimeMillis()))
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Gewohnheit-Reiter: Aenderung")
}

internal suspend fun deleteGewohnheit(context: Context, id: String) {
    de.frank.entropyreducer.data.local.habitDaoFrom(context).deleteById(id)
    // Sync-Etappe 1.4: Tombstone, damit die Loeschung beim Restore auf andere Geraete propagiert
    // (sonst laesst der additive Restore die geloeschte Gewohnheit dort "auferstehen").
    de.frank.entropyreducer.data.markDeleted(
        context,
        de.frank.entropyreducer.data.TombstoneType.GEWOHNHEIT,
        id,
    )
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Gewohnheit-Reiter: Aenderung")
}

internal suspend fun reorderGewohnheiten(context: Context, newOrder: List<Mental>) {
    val dao = de.frank.entropyreducer.data.local.habitDaoFrom(context)
    // position = neue Listen-Position. Bestehende Entity laden, damit Herkunft + Zeitstempel
    // erhalten bleiben (nicht aus dem positionslosen Mental-UI-Modell neu bauen).
    newOrder.forEachIndexed { index, m ->
        dao.getById(m.id)?.let { current ->
            if (current.position != index) dao.update(current.copy(position = index))
        }
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Gewohnheit-Reiter: Aenderung")
}

/**
 * Sync-Etappe 1.4: Spielt Gewohnheiten aus dem Drive-Backup ein — jetzt Last-Write-Wins per
 * updatedAt (frueher reine Existenz-Strategie -> Text-Edits propagierten nie) + Tombstone-Loeschung
 * ([deletedAt] = Map id->Loeschzeitpunkt der GEWOHNHEIT-Tombstones). Die lokale Reihenfolge bleibt
 * erhalten (bestehende Eintraege an ihrer Position aktualisiert, neue ans Ende, geloeschte raus).
 * Gibt die Anzahl der Aenderungen (eingespielt/aktualisiert/geloescht) zurueck.
 */
internal suspend fun restoreGewohnheiten(
    context: Context,
    // v19 (2026-06-20, Direktive #3 robust): direkt das Backup-DTO (mit Herkunft) statt das
    // herkunftslose Mental-UI-Modell — sonst geht die Kette beim Restore auf einem 2. Geraet verloren.
    incoming: List<de.frank.entropyreducer.data.remote.drive.BackupMental>,
    deletedAt: Map<String, Long> = emptyMap(),
): Int {
    if (incoming.isEmpty() && deletedAt.isEmpty()) return 0
    val dao = de.frank.entropyreducer.data.local.habitDaoFrom(context)
    var changed = 0
    val existing = dao.getAllForBackup() // ORDER BY position (Reihenfolge!)
    val existingIds = existing.mapTo(HashSet()) { it.id }
    val incomingById = incoming.associateBy { it.id }
    // 1. Bestehende: per Tombstone loeschen ODER per LWW (updatedAt) aktualisieren.
    for (ex in existing) {
        val ts = deletedAt[ex.id]
        if (ts != null && ts > ex.updatedAt) {
            dao.deleteById(ex.id)
            changed++
            continue
        }
        val inc = incomingById[ex.id]
        if (inc != null && inc.updatedAt > ex.updatedAt) {
            // copy erhaelt position + Herkunft; nur Text + Zeitstempel aktualisieren.
            dao.update(ex.copy(text = inc.text, updatedAt = inc.updatedAt))
            changed++
        }
    }
    // 2. Neue (im Backup, nicht lokal) ans Ende — ausser frisch getombstonet.
    var nextPos = (existing.maxOfOrNull { it.position } ?: -1) + 1
    for (inc in incoming) {
        if (inc.id in existingIds) continue
        val ts = deletedAt[inc.id]
        if (ts != null && ts > inc.updatedAt) continue
        dao.upsert(
            de.frank.entropyreducer.data.local.entities.HabitEntity(
                id = inc.id,
                text = inc.text,
                updatedAt = inc.updatedAt,
                position = nextPos++,
                // v19 (Direktive #3 robust): Herkunft aus dem Backup uebernehmen, sonst bricht die
                // Kette beim Restore und eine angenommene Gewohnheit wird auf diesem Geraet erneut
                // vorgeschlagen (countByRootId(habits) faende sie ohne rootId nicht).
                originId = inc.originId,
                originType = inc.originType,
                rootId = inc.rootId,
            )
        )
        changed++
    }
    return changed
}

private fun parseGewohnheiten(raw: String?): List<Mental> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                add(Mental(id = id, text = o.optString("text"), updatedAt = o.optLong("updatedAt")))
            }
        }
    }.getOrDefault(emptyList())
}

private val Accent: Color
    @Composable get() = LocalCosmos.current.accentTasksSub

/* ============================== UI ============================== */

@Composable
fun GewohnheitBoardScreen(
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
    showBottomBar: Boolean = true,
) {
    val cosmos = LocalCosmos.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val suggestVm: GewohnheitSuggestViewModel = hiltViewModel()
    val suggestions by suggestVm.suggestions.collectAsStateWithLifecycle()
    val suggestState by suggestVm.state.collectAsStateWithLifecycle()

    // TTS-System (Vorlesen der Gewohnheiten ueber dem Separator)
    val ttsVm: GewohnheitTtsViewModel = hiltViewModel()
    val ttsState by ttsVm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(ttsState.error) {
        ttsState.error?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            ttsVm.dismissError()
        }
    }

    // Vorlesen stoppen wenn Screen verlassen wird
    DisposableEffect(Unit) { onDispose { ttsVm.stop() } }

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
    val micActions = LocalMicActionsOpen.current

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
        showBottomBar = showBottomBar,
        actions = {
            if (suggestState == SuggestState.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Accent,
                )
                Spacer(Modifier.width(8.dp))
            }
            // KI-Button: Tap = Vorschläge generieren, Long-Press (2s) = Index zurücksetzen + Vibration
            val longPressJob = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // Long-Press-Job starten: nach 2 Sekunden vibrieren + Reset
                                longPressJob.value = scope.launch {
                                    kotlinx.coroutines.delay(2000L)
                                    try {
                                        @Suppress("DEPRECATION")
                                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                        vibrator?.vibrate(VibrationEffect.createOneShot(500, 255))
                                    } catch (cancellation: kotlinx.coroutines.CancellationException) {
                                        throw cancellation
                                    } catch (_: Exception) { /* Vibration nicht verfuegbar */ }
                                    suggestVm.resetProcessedIdeas()
                                }
                                tryAwaitRelease()
                                // Finger angehoben -> Job abbrechen (war kein Long-Press)
                                longPressJob.value?.cancel()
                                longPressJob.value = null
                            },
                            onTap = { suggestVm.generateSuggestions() },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Gewohnheitsvorschläge aus Ideen",
                    tint = Accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            // Neue Anordnung: Häkchen · Dropdown · Lautsprecher
            Checkbox(
                checked = ttsState.loop,
                onCheckedChange = { ttsVm.setLoop(it) },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E)),
            )
            GewohnheitNumberDropdown(
                value = ttsState.repeatCount,
                onSelect = { ttsVm.setRepeatCount(it) },
            )
            IconButton(onClick = { ttsVm.togglePlayback(stored) }) {
                Icon(
                    imageVector = if (ttsState.isPlaying) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = if (ttsState.isPlaying) "Vorlesen stoppen" else "Vorlesen",
                    tint = LocalCosmos.current.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState = MicState.IDLE,
                onTabSelected = { route -> onSwitchTab(route) },
                onMicClick = { micActions.toggle() },
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
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayed, key = { it.key }) { item ->
                        when (item) {
                            is DisplayItem.UserMental -> {
                                val userIndex = displayed.takeWhile { it != item }
                                    .count { it is DisplayItem.UserMental } + 1
                                ReorderableItem(reorderState, key = item.key) { isDragging ->
                                    UserRow(
                                        position = userIndex,
                                        mental = item.mental,
                                        isDragging = isDragging,
                                        onClick = { editTarget = item.mental },
                                        dragModifier = Modifier.longPressDraggableHandle(
                                            onDragStopped = {
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
                                                    } else {
                                                        // Innerhalb der Section umsortiert → persistieren.
                                                        val storedIds = stored.map { it.id }.toSet()
                                                        val reordered = finalOrder
                                                            .filter { it in storedIds }
                                                            .mapNotNull { id -> stored.firstOrNull { it.id == id } }
                                                        if (reordered.size == stored.size) {
                                                            scope.launch { reorderGewohnheiten(context, reordered) }
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
                                                addGewohnheitFromSuggestion(context, item.mental.id, item.mental.text)
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
                                                            addGewohnheitFromSuggestion(context, item.mental.id, item.mental.text)
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
                visible = micActions.isOpen,
                accent = Accent,
                onTextCommit = { text, _ -> scope.launch { addGewohnheit(context, text) } },
                onClose = { micActions.close() },
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
    position: Int,
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
                text = "$position",
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

/* ============================== Vorlese-Steuerung (Top-Bar) ============================== */

@Composable
private fun GewohnheitTtsControls(
    state: GewohnheitTtsUiState,
    onToggle: () -> Unit,
    onRepeatChange: (Int) -> Unit,
    onLoopChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                contentDescription = if (state.isPlaying) "Vorlesen stoppen" else "Vorlesen",
                tint = LocalCosmos.current.accent,
                modifier = Modifier.size(24.dp),
            )
        }
        GewohnheitNumberDropdown(
            value = state.repeatCount,
            onSelect = onRepeatChange,
        )
        Checkbox(
            checked = state.loop,
            onCheckedChange = onLoopChange,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E)),
        )
    }
}

@Composable
private fun GewohnheitNumberDropdown(
    value: Int,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Accent.copy(alpha = 0.12f))
                .clickable { expanded = true }
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Repeat,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = "$value",
                style = MaterialTheme.typography.labelLarge,
                color = Accent,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = Accent,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = "Jeder Satz — wie oft",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
            (1..10).forEach { n ->
                DropdownMenuItem(
                    text = { Text("$n") },
                    onClick = {
                        onSelect(n)
                        expanded = false
                    },
                    trailingIcon = {
                        if (n == value) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Accent,
                            )
                        }
                    },
                )
            }
        }
    }
}
