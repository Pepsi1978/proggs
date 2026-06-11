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
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.MicCaptureActions
import de.frank.entropyreducer.presentation.components.MicState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.navigation.Routes
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Mentalboard (Frank-Wunsch 2026-06-09): eine vom Benutzer frei sortierbare Liste kurzer
 * Saetze ("Mentals"). Liegt im Aufgaben-Bereich auf Sub-Reiter 2 ("Mental").
 *
 * Bedienung:
 * - Der Mic-Button in der BottomBar oeffnet die Auswahl "Schreiben" / "Aufnehmen"
 *   (Frank-Wunsch 2026-06-11: der fruehere "+ Neues Mental"-Button ist entfernt, alles
 *   laeuft ueber das Mikrofon). Nach dem Speichern erscheint der Satz nummeriert
 *   (1., 2., 3., …) in der Liste.
 * - Tap auf einen Satz oeffnet einen Dialog zum Editieren ODER Loeschen.
 * - Langes Druecken auf einen Satz startet Drag & Drop: die Reihenfolge laesst sich
 *   beliebig umsortieren (Position 7 kann auf Position 1 wandern).
 *
 * Persistenz: DataStore (Datei "mental_board"), exakt analog zum Tagebuch/Thesen-Muster —
 * bewusst KEIN Room, weil die Haupt-DB mit destructive-fallback laeuft (eine fehlerhafte
 * Room-Migration wuerde alle echten Daten loeschen). Die Reihenfolge IST die Listenreihenfolge
 * (kein sortBy) — so ueberlebt das manuelle Sortieren App-Neustarts. Jede Aenderung stoesst
 * automatisch ein Drive-Backup an (triggerDriveBackup), damit das Board ins Cloud-Backup wandert.
 *
 * Die Akzentfarbe folgt dem Aufgaben-Bereich (Orange #EA580C), weil der Reiter dort liegt.
 * CosmosBottomBar laeuft im Sub-Mode mit forcedSubMode=TASKS, selectedSubIndex=2.
 */

/* ============================== Datenmodell ============================== */

/** Ein einzelner Mental-Eintrag: nur ID + Satz. Reihenfolge = Position in der Liste. */
data class Mental(val id: String, val text: String) {
    companion object {
        fun create(text: String): Mental = Mental(id = UUID.randomUUID().toString(), text = text)
    }
}

private val Context.mentalStore by preferencesDataStore(name = "mental_board")
private val KEY_MENTALS = stringPreferencesKey("mentals_json")

/** Liste in GESPEICHERTER Reihenfolge (NICHT sortiert — das manuelle Sortieren ist die Reihenfolge). */
internal fun mentalsFlow(context: Context): Flow<List<Mental>> =
    context.mentalStore.data.map { prefs -> parseMentals(prefs[KEY_MENTALS]) }

internal suspend fun addMental(context: Context, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    context.mentalStore.edit { prefs ->
        val existing = parseMentals(prefs[KEY_MENTALS])
        prefs[KEY_MENTALS] = serializeMentals(existing + Mental.create(clean))
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context)
}

internal suspend fun updateMental(context: Context, id: String, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    context.mentalStore.edit { prefs ->
        val existing = parseMentals(prefs[KEY_MENTALS])
        prefs[KEY_MENTALS] = serializeMentals(existing.map { if (it.id == id) it.copy(text = clean) else it })
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context)
}

internal suspend fun deleteMental(context: Context, id: String) {
    context.mentalStore.edit { prefs ->
        val existing = parseMentals(prefs[KEY_MENTALS])
        prefs[KEY_MENTALS] = serializeMentals(existing.filterNot { it.id == id })
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context)
}

/** Speichert die per Drag & Drop geaenderte Reihenfolge 1:1. */
internal suspend fun reorderMentals(context: Context, newOrder: List<Mental>) {
    context.mentalStore.edit { prefs -> prefs[KEY_MENTALS] = serializeMentals(newOrder) }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context)
}

/**
 * Spielt Mentals aus einem Drive-Backup ein (Frank-Wunsch 2026-06-09). Nur fehlende IDs werden
 * ergaenzt — lokale Edits/Reihenfolge gewinnen (konservativ, wie beim Tagebuch-Restore).
 */
internal suspend fun restoreMentals(context: Context, incoming: List<Mental>): Int {
    if (incoming.isEmpty()) return 0
    var added = 0
    context.mentalStore.edit { prefs ->
        val existing = parseMentals(prefs[KEY_MENTALS])
        val existingIds = existing.mapTo(HashSet()) { it.id }
        val toAdd = incoming.filterNot { it.id in existingIds }
        added = toAdd.size
        if (toAdd.isNotEmpty()) prefs[KEY_MENTALS] = serializeMentals(existing + toAdd)
    }
    return added
}

private fun parseMentals(raw: String?): List<Mental> {
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

private fun serializeMentals(mentals: List<Mental>): String {
    val arr = JSONArray()
    for (m in mentals) {
        arr.put(JSONObject().put("id", m.id).put("text", m.text))
    }
    return arr.toString()
}

/* Akzentfarbe — Aufgaben-Bereich (Orange #EA580C), weil das Mentalboard dort als Sub-Reiter liegt. */
internal val MentalAccent: Color = Color(0xFFEA580C)

/* ============================== UI ============================== */

@Composable
fun MentalBoardScreen(
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    // Bugfix 2026-06-10 (Frank, 2. Versuch — Root Cause KORRIGIERT): Der erste Fix war an der
    // falschen Stelle. Echte Ursache: `mentalsFlow(context)` wurde bei JEDER Recomposition NEU
    // erzeugt → `collectAsStateWithLifecycle` bekam jedes Mal ein neues Flow-Objekt → die laufende
    // DataStore-Subscription war instabil und verpasste die Emission nach add/update/delete. Erst
    // eine unabhaengige Recomposition (Tap auf einen anderen Eintrag) startete eine frische
    // Subscription, die den aktuellen Stand frisch las — exakt Franks Symptom. Das Tagebuch hat
    // denselben Code, dort wird der Bug nur durch viele andere States + KI-Folge-Updates verdeckt.
    // Fix (verifiziert 2026-06-10 per Logcat: emit folgt sofort auf jeden Write): Flow EINMAL per
    // remember(context) stabil halten (Bug-Almanach kotlin.md §4.4 + jetpack-compose.md §2.14 +
    // developer.android.com/develop/ui/compose/state).
    val mentalsStream = remember(context) { mentalsFlow(context) }
    val stored by mentalsStream.collectAsStateWithLifecycle(initialValue = emptyList())

    // Lokale Drag-Reihenfolge: NUR waehrend eines aktiven Drag-Vorgangs gesetzt. Ausserhalb des
    // Ziehens wird direkt `stored` angezeigt -> add/update/delete sind sofort sichtbar.
    var dragOrder by remember { mutableStateOf<List<Mental>?>(null) }
    val displayed = dragOrder ?: stored

    // `dragOrder` zuruecksetzen, sobald (a) der gespeicherte Stand die gezogene Reihenfolge
    // erreicht hat (gleiche IDs in gleicher Folge), ODER (b) sich der Eintrags-BESTAND geaendert
    // hat (add/delete) — dann hat `stored` Vorrang und die Aenderung wird sofort sichtbar.
    androidx.compose.runtime.LaunchedEffect(stored) {
        val d = dragOrder ?: return@LaunchedEffect
        val dIds = d.map { it.id }
        val sIds = stored.map { it.id }
        if (dIds == sIds || dIds.toSet() != sIds.toSet()) {
            dragOrder = null
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Mental?>(null) }
    // Bugfix 2026-06-11 (Frank): "+ Neues Mental" und der Mic-Button oeffneten nur den
    // Tipp-Dialog — die Sprachaufnahme (Groq Whisper Large V3 Turbo) fehlte. Beide Wege
    // oeffnen jetzt wie bei Ideen/Loop die Auswahl "Schreiben" / "Aufnehmen".
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
        title = "Mental",
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState = MicState.IDLE,
                onTabSelected = { route -> onSwitchTab(route) },
                onMicClick = { micActionsOpen = !micActionsOpen },
                onSubAreaSelected = { parent, index -> onSwitchSub(parent, index) },
                forcedSubMode = Routes.TASKS,
                // Mentalboard ist Sub-Bereich 2 unter Aufgaben → dauerhaft hervorheben.
                selectedSubIndex = 2,
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(if (cosmos.isDark) Color(0xFF15182A) else Color(0xFFF6F7FB))
                    .padding(padding)
        ) {
            if (displayed.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(displayed, key = { it.id }) { mental ->
                        ReorderableItem(reorderState, key = mental.id) { isDragging ->
                            val position = displayed.indexOfFirst { it.id == mental.id } + 1
                            MentalRow(
                                position = position,
                                text = mental.text,
                                isDragging = isDragging,
                                onClick = { editTarget = mental },
                                dragModifier =
                                    Modifier.longPressDraggableHandle(
                                        onDragStopped = {
                                            // Gezogene Reihenfolge persistieren; der Flow liefert
                                            // sie danach als `stored` zurueck (siehe LaunchedEffect
                                            // oben, der dragOrder dann freigibt).
                                            dragOrder?.let { order ->
                                                scope.launch { reorderMentals(context, order) }
                                            }
                                        }
                                    ),
                            )
                        }
                    }
                }
            }

            // Schreiben/Aufnehmen-Auswahl wie bei Ideen/Loop (Frank-Wunsch 2026-06-11):
            // "Schreiben" oeffnet den bisherigen "Neues Mental"-Dialog (orange Diskette),
            // "Aufnehmen" transkribiert per Groq Whisper und speichert direkt als Mental.
            MicCaptureActions(
                visible = micActionsOpen,
                accent = MentalAccent,
                onTextCommit = { text, _ -> scope.launch { addMental(context, text) } },
                onClose = { micActionsOpen = false },
                onWriteClick = { showAddDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showAddDialog) {
        MentalEditDialog(
            initialText = "",
            title = "Neues Mental",
            onDismiss = { showAddDialog = false },
            onSave = { text ->
                scope.launch { addMental(context, text) }
                showAddDialog = false
            },
            onDelete = null,
        )
    }

    editTarget?.let { target ->
        MentalEditDialog(
            initialText = target.text,
            title = "Mental bearbeiten",
            onDismiss = { editTarget = null },
            onSave = { text ->
                scope.launch { updateMental(context, target.id, text) }
                editTarget = null
            },
            onDelete = {
                scope.launch { deleteMental(context, target.id) }
                editTarget = null
            },
        )
    }
}

@Composable
private fun MentalRow(
    position: Int,
    text: String,
    isDragging: Boolean,
    onClick: () -> Unit,
    dragModifier: Modifier,
) {
    val cosmos = LocalCosmos.current
    val cardBg =
        if (cosmos.isDark) Color(0xFF1E2336) else Color.White
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .then(
                    if (isDragging) {
                        Modifier.background(MentalAccent.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    } else {
                        Modifier
                    }
                )
                .clickable { onClick() }
                .then(dragModifier)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Nummern-Kreis in Akzentfarbe
        Box(
            modifier =
                Modifier.size(30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MentalAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$position",
                style = MaterialTheme.typography.labelLarge,
                color = MentalAccent,
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
private fun EmptyState() {
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
                    .background(MentalAccent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Psychology,
                contentDescription = null,
                tint = MentalAccent,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Noch keine Mentals",
            style = MaterialTheme.typography.headlineSmall,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tippe unten auf das Mikrofon und waehle Schreiben oder Aufnehmen. " +
                "Spaeter kannst du Saetze per langem Druecken frei sortieren.",
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MentalEditDialog(
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
        // Frank-Wunsch 2026-06-10: Statt Text-Buttons nur Icons — Diskette zum Speichern
        // (rechts), Papierkorb zum Loeschen (links). KEIN "Abbrechen" mehr: ein Tipp in den
        // leeren Raum schliesst den Dialog ueber onDismissRequest.
        confirmButton = {
            // Frank-Wunsch 2026-06-10: groessere Icons. Diskette IMMER orange — bei leerem Text
            // nur abgeschwaecht (statt grau), damit sie auch im "Neues Mental"-Dialog klar sichtbar
            // ist. Icon 32dp im 56dp-Touch-Target.
            IconButton(
                onClick = { if (text.isNotBlank()) onSave(text) },
                enabled = text.isNotBlank(),
                modifier = Modifier.size(56.dp),
            ) {
                // Frank-Wunsch 2026-06-10: Diskette IMMER voll orange in voller Staerke — auch im
                // "Neues Mental"-Dialog (vorher bei leerem Text abgeschwaecht/durchsichtig). Gleiche
                // Farbe und Staerke wie im Bearbeiten-Dialog.
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = "Speichern",
                    tint = MentalAccent,
                    modifier = Modifier.size(32.dp),
                )
            }
        },
        dismissButton = {
            // Papierkorb nur beim Bearbeiten (onDelete != null), nicht beim Neuanlegen. Groesser.
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
