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
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.hilt.navigation.compose.hiltViewModel
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
import de.frank.entropyreducer.data.safety.PhoneContentGuard
import de.frank.entropyreducer.data.tts.TtsUsage
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

/**
 * Mentalboard (Frank-Wunsch 2026-06-09): eine vom Benutzer frei sortierbare Liste kurzer Saetze
 * ("Mentals"). Liegt im Aufgaben-Bereich auf Sub-Reiter 2 ("Mental").
 *
 * Bedienung:
 * - Der Mic-Button in der BottomBar oeffnet die Auswahl "Schreiben" / "Aufnehmen" (Frank-Wunsch
 *   2026-06-11: der fruehere "+ Neues Mental"-Button ist entfernt, alles laeuft ueber das
 *   Mikrofon). Nach dem Speichern erscheint der Satz nummeriert (1., 2., 3., …) in der Liste.
 * - Tap auf einen Satz oeffnet einen Dialog zum Editieren ODER Loeschen.
 * - Langes Druecken auf einen Satz startet Drag & Drop: die Reihenfolge laesst sich beliebig
 *   umsortieren (Position 7 kann auf Position 1 wandern).
 *
 * Persistenz: DataStore (Datei "mental_board"), exakt analog zum Tagebuch/Thesen-Muster — bewusst
 * KEIN Room, weil die Haupt-DB mit destructive-fallback laeuft (eine fehlerhafte Room-Migration
 * wuerde alle echten Daten loeschen). Die Reihenfolge IST die Listenreihenfolge (kein sortBy) — so
 * ueberlebt das manuelle Sortieren App-Neustarts. Jede Aenderung stoesst automatisch ein
 * Drive-Backup an (triggerDriveBackup), damit das Board ins Cloud-Backup wandert.
 *
 * Die Akzentfarbe folgt dem Aufgaben-Bereich (Orange #EA580C), weil der Reiter dort liegt.
 * CosmosBottomBar laeuft im Sub-Mode mit forcedSubMode=TASKS, selectedSubIndex=2.
 */

/* ============================== Datenmodell ============================== */

/** Ein einzelner Mental-Eintrag: ID + Satz (+ Aenderungs-Zeitstempel). Reihenfolge = Listen-Position. */
data class Mental(
    val id: String,
    val text: String,
    // Sync-Etappe 1.4: Zeitstempel der letzten Aenderung — fuer Last-Write-Wins beim Multi-Device-
    // Restore. Default 0L haelt aeltere serialisierte Daten/Backups lesbar (sie gelten als "uralt"
    // und verlieren damit gegen jede echte Bearbeitung).
    val updatedAt: Long = 0L,
) {
    companion object {
        fun create(text: String): Mental =
            Mental(
                id = UUID.randomUUID().toString(),
                text = text,
                updatedAt = System.currentTimeMillis(),
            )
    }
}

private val Context.mentalStore by preferencesDataStore(name = "mental_board")
private val KEY_MENTALS = stringPreferencesKey("mentals_json")

/**
 * Liste in GESPEICHERTER Reihenfolge (NICHT sortiert — das manuelle Sortieren ist die Reihenfolge).
 */
// ID-Architektur Etappe 4c (Frank-Wunsch 2026-06-19): Mental-Saetze liegen jetzt in Room (Tabelle
// mental_sentences, sortiert nach `position`) statt im DataStore-JSON. Signaturen bleiben 1:1 — UI,
// Backup (mentalsFlow) und Sync (restoreMentals) laufen unveraendert weiter; DAO per Hilt-@EntryPoint.
internal fun mentalsFlow(context: Context): Flow<List<Mental>> =
    de.frank.entropyreducer.data.local
        .mentalSentenceDaoFrom(context)
        .getAll()
        .map { rows -> rows.map { Mental(id = it.id, text = it.text, updatedAt = it.updatedAt) } }
        .distinctUntilChanged()

/** JSON-Lesequelle der Bestands-Mentals NUR fuer den einmaligen Migrator (MentalRoomMigrator, 4b). */
internal fun mentalsFromJsonFlow(context: Context): Flow<List<Mental>> =
    context.mentalStore.data.map { prefs -> parseMentals(prefs[KEY_MENTALS]) }

internal suspend fun addMental(context: Context, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    if (PhoneContentGuard.isSecondBrainWorkArtifact(null, clean)) return
    val dao = de.frank.entropyreducer.data.local.mentalSentenceDaoFrom(context)
    val m = Mental.create(clean)
    dao.upsert(
        de.frank.entropyreducer.data.local.entities.MentalEntity(
            id = m.id,
            text = m.text,
            updatedAt = m.updatedAt,
            position = dao.maxPosition() + 1, // neuer Satz ans Ende
        )
    )
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Mental-Reiter: Aenderung")
}

internal suspend fun updateMental(context: Context, id: String, text: String) {
    val clean = text.trim()
    if (clean.isEmpty()) return
    if (PhoneContentGuard.isSecondBrainWorkArtifact(null, clean)) return
    val dao = de.frank.entropyreducer.data.local.mentalSentenceDaoFrom(context)
    val current = dao.getById(id) ?: return
    dao.update(current.copy(text = clean, updatedAt = System.currentTimeMillis()))
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Mental-Reiter: Aenderung")
}

internal suspend fun deleteMental(context: Context, id: String, propagate: Boolean = true) {
    de.frank.entropyreducer.data.local.mentalSentenceDaoFrom(context).deleteById(id)
    // Frank-Wunsch 2026-06-20: Loeschung propagieren (Tombstone), sonst kommt der Mental-Satz ueber
    // ein 2. Geraet beim Restore wieder. propagate=false: roher Restore-Cleanup ohne neuen Tombstone.
    if (propagate) {
        de.frank.entropyreducer.data.markDeleted(
            context, de.frank.entropyreducer.data.TombstoneType.MENTAL, id)
        de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Mental-Reiter: Aenderung")
    }
}

/** Speichert die per Drag & Drop geaenderte Reihenfolge ueber das position-Feld. */
internal suspend fun reorderMentals(context: Context, newOrder: List<Mental>) {
    val dao = de.frank.entropyreducer.data.local.mentalSentenceDaoFrom(context)
    newOrder.forEachIndexed { index, m ->
        dao.getById(m.id)?.let { current ->
            if (current.position != index) dao.update(current.copy(position = index))
        }
    }
    de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(context, "Mental-Reiter: Aenderung")
}

/**
 * Spielt Mentals aus einem Drive-Backup ein (Frank-Wunsch 2026-06-09). Nur fehlende IDs werden
 * ergaenzt — lokale Edits/Reihenfolge gewinnen (konservativ, wie beim Tagebuch-Restore). Neue ans Ende.
 */
internal suspend fun restoreMentals(
    context: Context,
    // v19 (2026-06-20): Backup-DTO MIT Herkunft direkt (analog restoreGewohnheiten) statt das
    // herkunftslose Mental-UI-Modell — die Herkunft ueberlebt so den Restore.
    incoming: List<de.frank.entropyreducer.data.remote.drive.BackupMental>,
    // Frank-Wunsch 2026-06-20: Mental-Tombstones anwenden (delete-wins-only-if-newer; Mental hat updatedAt).
    deletedAt: Map<String, Long> = emptyMap(),
): Int {
    val dao = de.frank.entropyreducer.data.local.mentalSentenceDaoFrom(context)
    var deleted = 0
    // Tombstone: lokale getombstonete Mental-Saetze loeschen (nur wenn die Loeschung neuer ist als
    // der lokale Stand). Laeuft auch bei leerem incoming -> raeumt Alt-Eintraege weg.
    if (deletedAt.isNotEmpty()) {
        for (ex in dao.getAllForBackup()) {
            val ts = deletedAt[ex.id]
            if (ts != null && ts > ex.updatedAt) {
                dao.deleteById(ex.id)
                deleted++
            }
        }
    }
    if (incoming.isEmpty()) return deleted
    val existingIds = dao.getAllForBackup().mapTo(HashSet()) { it.id }
    val toAdd = incoming.filterNot { m ->
        m.id in existingIds ||
            (deletedAt[m.id]?.let { it > m.updatedAt } == true) ||
            PhoneContentGuard.isSecondBrainWorkArtifact(null, m.text)
    }
    if (toAdd.isEmpty()) return deleted
    var nextPos = dao.maxPosition() + 1
    dao.upsertAll(
        toAdd.map { m ->
            de.frank.entropyreducer.data.local.entities.MentalEntity(
                id = m.id,
                text = m.text,
                updatedAt = m.updatedAt,
                position = nextPos++,
                originId = m.originId,
                originType = m.originType,
                rootId = m.rootId,
            )
        }
    )
    return toAdd.size + deleted
}

private fun parseMentals(raw: String?): List<Mental> {
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
        }
        .getOrDefault(emptyList())
}

/* Akzentfarbe — Blau (Frank-Wunsch 2026-06-18): Sub-Tabs unter Aufgaben sind blau. TTS bleibt Orange. */
internal val MentalAccent: Color
    @Composable get() = LocalCosmos.current.accentTasksSub

/* ============================== UI ============================== */

@Composable
fun MentalBoardScreen(
    onSwitchSub: (parentTab: String, index: Int) -> Unit,
    onSwitchTab: (route: String) -> Unit,
    showBottomBar: Boolean = true,
) {
    val cosmos = LocalCosmos.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val brainSyncVm: de.frank.entropyreducer.presentation.ideen.IdeenBrainSyncViewModel = hiltViewModel()
    LaunchedEffect(Unit) { brainSyncVm.pullNow("mental") }

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

    // Vorlese-System: Das ViewModel ist UI-Adapter; der laufende Job liegt im Singleton-Controller,
    // damit Vorlesen beim Reiterwechsel weiterläuft und nur per Lautsprecher oder Timeout stoppt.
    val ttsVm: MentalTtsViewModel = hiltViewModel()
    val ttsState by ttsVm.uiState.collectAsStateWithLifecycle()
    // Live-Zaehler des TTS-Monatskontingents — wird unten als letztes Listenelement gezeigt.
    val ttsUsage by ttsVm.usage.collectAsStateWithLifecycle()
    // Gewohnheiten fuer das optionale Mitlesen (G-Haekchen, Frank-Wunsch 2026-07-03). Flow stabil
    // per remember(context) halten (gleiche Instanz — sonst Emission-Verlust, siehe mentalsStream).
    val gewohnheitenStream = remember(context) { gewohnheitenFlow(context) }
    val gewohnheiten by gewohnheitenStream.collectAsStateWithLifecycle(initialValue = emptyList())

    // Fehler (z.B. kein TTS-Schluessel hinterlegt) als Toast zeigen, danach quittieren.
    LaunchedEffect(ttsState.error) {
        ttsState.error?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            ttsVm.dismissError()
        }
    }

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
    val micActions = LocalMicActionsOpen.current

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
        showBottomBar = showBottomBar,
        actions = {
            MentalTtsControls(
                state = ttsState,
                onToggle = { ttsVm.togglePlayback(displayed, gewohnheiten) },
                onAnkerChange = ttsVm::setAnkerCount,
                onFolgeChange = ttsVm::setFolgeCount,
                onLoopChange = ttsVm::setLoop,
                onIncludeHabitsChange = ttsVm::setIncludeHabits,
            )
        },
        bottomBar = {
            CosmosBottomBar(
                currentTab = Routes.TASKS,
                micState = MicState.IDLE,
                onTabSelected = { route -> onSwitchTab(route) },
                onMicClick = { micActions.toggle() },
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
                    .background(if (cosmos.isDark) Color(0xFF12100D) else Color(0xFFFAF7F3))
                    .padding(padding)
        ) {
            if (displayed.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (ttsState.isPlaying) {
                        item(key = "tts_playback_controls") {
                            MentalPlaybackControls(
                                isPaused = ttsState.isPaused,
                                onPlay = ttsVm::resume,
                                onPause = ttsVm::pause,
                            )
                        }
                        item(key = "tts_auto_stop_info") {
                            TtsAutoStopInfo(ttsState.autoStopEndsAtWallClockMs)
                        }
                    }
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
                    // Frank-Wunsch 2026-07-03: Immer UNTER dem letzten Satz — egal wie viele es sind
                    // — der TTS-Verbrauch des Monats (Chirp 3 HD, 1-Mio-Gratis-Kontingent).
                    item(key = "tts_usage_footer") {
                        TtsUsageFooter(usage = ttsUsage)
                    }
                }
            }

            // Schreiben/Aufnehmen-Auswahl wie bei Ideen/Loop (Frank-Wunsch 2026-06-11):
            // "Schreiben" oeffnet den bisherigen "Neues Mental"-Dialog (orange Diskette),
            // "Aufnehmen" transkribiert per Groq Whisper und speichert direkt als Mental.
            MicCaptureActions(
                visible = micActions.isOpen,
                accent = MentalAccent,
                onTextCommit = { text, _ -> scope.launch { addMental(context, text) } },
                onClose = { micActions.close() },
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
private fun MentalPlaybackControls(
    isPaused: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val orange = Color(0xFFFF9800)
    val cardBg = if (cosmos.isDark) Color(0xFF1D1A16) else Color.White
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPlay,
            enabled = isPaused,
            modifier =
                Modifier.size(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(if (!isPaused) orange.copy(alpha = 0.18f) else Color.Transparent),
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Vorlesen fortsetzen",
                tint = if (!isPaused) orange else cosmos.textSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.size(22.dp))
        IconButton(
            onClick = onPause,
            enabled = !isPaused,
            modifier =
                Modifier.size(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(if (isPaused) orange.copy(alpha = 0.18f) else Color.Transparent),
        ) {
            Icon(
                imageVector = Icons.Outlined.Pause,
                contentDescription = "Vorlesen pausieren",
                tint = if (isPaused) orange else cosmos.textSecondary,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * Zeigt den laufenden Monatsverbrauch des Google-TTS-Kontingents (Chirp 3 HD). Steht als letztes
 * Element der Mental-Liste immer unter dem letzten Satz. Orange = Audio/Vorlesen (Farbkonvention).
 * Eigener Box-Balken statt Material3-`LinearProgressIndicator` (dessen Signatur sich in 1.4.0
 * aenderte — so kann der Build nicht daran brechen).
 */
@Composable
private fun TtsUsageFooter(usage: TtsUsage) {
    val cosmos = LocalCosmos.current
    val used = usage.charsThisMonth.coerceAtMost(usage.limit)
    val fraction = if (usage.limit > 0L) used.toFloat() / usage.limit.toFloat() else 0f
    val orange = Color(0xFFFF9800)
    val trackBg = if (cosmos.isDark) Color(0xFF2A2620) else Color(0xFFECE6DE)
    val textColor = if (cosmos.isDark) Color(0xFFB9B2A6) else Color(0xFF6B6459)
    val usedStr = String.format(java.util.Locale.GERMANY, "%,d", usage.charsThisMonth)
    val limitStr = String.format(java.util.Locale.GERMANY, "%,d", usage.limit)
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            text = "Chirp 3 HD: $usedStr von $limitStr Zeichen · ${usage.percentFree} % frei",
            color = textColor,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(trackBg)
        ) {
            Box(
                modifier =
                    Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(orange)
            )
        }
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
    val cardBg = if (cosmos.isDark) Color(0xFF1D1A16) else Color.White
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cardBg)
                .then(
                    if (isDragging) {
                        Modifier.background(
                            MentalAccent.copy(alpha = 0.12f),
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
                // "Neues Mental"-Dialog (vorher bei leerem Text abgeschwaecht/durchsichtig).
                // Gleiche
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

/* ============================== Vorlese-Steuerung (Top-Bar) ============================== */

/**
 * Steuerleiste oben neben dem Titel "Mental" (Frank-Wunsch 2026-06-16):
 * Lautsprecher-Toggle · Dropdown "Erster Satz" (Anker) · Dropdown "Folgesatz" · Endlos-Haekchen.
 * Reine Darstellung — State runter, Events rauf (UDF); die ganze Logik liegt im MentalTtsViewModel.
 */
@Composable
private fun MentalTtsControls(
    state: MentalTtsUiState,
    onToggle: () -> Unit,
    onAnkerChange: (Int) -> Unit,
    onFolgeChange: (Int) -> Unit,
    onLoopChange: (Boolean) -> Unit,
    onIncludeHabitsChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // "G"-Haekchen: Gewohnheiten am Ende des Mentalblocks mitlesen (Frank-Wunsch 2026-07-03).
        Text(
            text = "G",
            fontWeight = FontWeight.Bold,
            color = MentalAccent,
            style = MaterialTheme.typography.labelLarge,
        )
        Checkbox(
            checked = state.includeHabits,
            onCheckedChange = onIncludeHabitsChange,
            colors = CheckboxDefaults.colors(checkedColor = MentalAccent),
        )
        // Neue Anordnung: Häkchen · Dropdown 1 · Dropdown 2 · Lautsprecher
        Checkbox(
            checked = state.loop,
            onCheckedChange = onLoopChange,
            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E)),
        )
        // Dropdown 1 — wie oft der erste Satz (Anker) vor jedem Folgesatz vorgelesen wird.
        NumberDropdown(
            value = state.ankerCount,
            leadingIcon = Icons.Outlined.LooksOne,
            menuTitle = "Erster Satz – wie oft",
            onSelect = onAnkerChange,
        )
        Spacer(Modifier.size(4.dp))
        // Dropdown 2 — wie oft jeder Folgesatz vorgelesen wird.
        NumberDropdown(
            value = state.folgeCount,
            leadingIcon = Icons.Outlined.Repeat,
            menuTitle = "Folgesatz – wie oft",
            onSelect = onFolgeChange,
        )
        // Lautsprecher = Start/Stop-Toggle. Ganz rechts.
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Outlined.Stop else Icons.Outlined.VolumeUp,
                contentDescription = if (state.isPlaying) "Vorlesen stoppen" else "Vorlesen",
                tint = LocalCosmos.current.accent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/**
 * Kompaktes Zahlen-Dropdown (1..10) fuer die Top-Bar. Zeigt ein kleines Symbol, den aktuellen Wert
 * und einen Pfeil; beim Antippen erscheint ein Menue mit erklaerendem Titel und den Zahlen 1–10.
 */
@Composable
private fun NumberDropdown(
    value: Int,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    menuTitle: String,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(MentalAccent.copy(alpha = 0.12f))
                    .clickable { expanded = true }
                    .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MentalAccent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = "$value",
                style = MaterialTheme.typography.labelLarge,
                color = MentalAccent,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = Icons.Outlined.ArrowDropDown,
                contentDescription = null,
                tint = MentalAccent,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Nicht-klickbarer Titel erklaert, worauf sich die Zahl bezieht.
            Text(
                text = menuTitle,
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
                                tint = MentalAccent,
                            )
                        }
                    },
                )
            }
        }
    }
}
