package de.frank.denknotiz.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.frank.denknotiz.BuildConfig
import de.frank.denknotiz.data.AppTheme
import de.frank.denknotiz.data.BackupPayload
import de.frank.denknotiz.data.CodexModel
import de.frank.denknotiz.data.DenknotizRepository
import de.frank.denknotiz.data.ReasoningEffort
import de.frank.denknotiz.data.SettingsSnapshot
import de.frank.denknotiz.data.TtsProvider
import de.frank.denknotiz.data.local.EntryEntity
import de.frank.denknotiz.data.local.EntryType
import de.frank.denknotiz.data.local.EvaluationSnapshotEntity
import de.frank.denknotiz.data.local.SessionEntity
import de.frank.denknotiz.data.local.SnapshotStatus
import de.frank.denknotiz.domain.AnalysisProfiles
import de.frank.denknotiz.tts.QwenVoice
import de.frank.denknotiz.ui.theme.DenknotizTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONArray

private val MotionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenknotizApp(
    viewModel: DenknotizViewModel,
    requestMicrophone: (() -> Unit) -> Unit,
    requestNotifications: () -> Unit,
    createBackup: () -> Unit,
    openBackup: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DenknotizTheme(state.settings.theme) {
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(state.interaction.message) {
            state.interaction.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
        }
        LaunchedEffect(state.speech.error) {
            if (state.speech.error.isNotBlank()) snackbar.showSnackbar(state.speech.error)
        }
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(310.dp)) {
                    Column(Modifier.fillMaxHeight().statusBarsPadding().padding(18.dp)) {
                        Text("DENKNOTIZ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("Gedankenwerkbank", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))
                        NavigationDrawerItem(
                            label = { Text("Werkbank") }, icon = { Icon(Icons.Default.GraphicEq, null) },
                            selected = state.interaction.section == AppSection.WORKBENCH,
                            onClick = { viewModel.selectSection(AppSection.WORKBENCH); scope.launch { drawer.close() } },
                        )
                        NavigationDrawerItem(
                            label = { Text("Einstellungen") }, icon = { Icon(Icons.Default.Settings, null) },
                            selected = state.interaction.section == AppSection.SETTINGS,
                            onClick = { viewModel.selectSection(AppSection.SETTINGS); scope.launch { drawer.close() } },
                        )
                        Spacer(Modifier.weight(1f))
                        Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                snackbarHost = { SnackbarHost(snackbar) },
            ) { padding ->
                Box(
                    Modifier.fillMaxSize().padding(padding).background(
                        Brush.radialGradient(
                            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.09f), MaterialTheme.colorScheme.background),
                            radius = 1400f,
                        ),
                    ),
                ) {
                    when (state.interaction.section) {
                        AppSection.WORKBENCH -> Workbench(
                            state, viewModel, openDrawer = { scope.launch { drawer.open() } },
                            requestMicrophone = requestMicrophone, requestNotifications = requestNotifications,
                        )
                        AppSection.SETTINGS -> SettingsScreen(
                            state, viewModel, openDrawer = { scope.launch { drawer.open() } },
                            requestMicrophone = requestMicrophone, createBackup = createBackup, openBackup = openBackup,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Workbench(
    state: DenknotizUiState,
    vm: DenknotizViewModel,
    openDrawer: () -> Unit,
    requestMicrophone: (() -> Unit) -> Unit,
    requestNotifications: () -> Unit,
) {
    LaunchedEffect(state.speech.active) { if (state.speech.active) requestNotifications() }
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val expanded = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize()) {
            SessionRail(state, vm, expanded)
            Column(Modifier.weight(1f).fillMaxHeight()) {
                WorkbenchTopBar(state, vm, openDrawer)
                val bundle = state.bundle
                if (bundle == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Timeline(bundle.entries, bundle.snapshots, bundle.boundary?.lastIncludedOrdinal ?: 0, vm,
                        modifier = Modifier.weight(1f).fillMaxWidth())
                    SpeechBar(state, vm)
                    RejectedAudioBar(state, vm)
                    Composer(state, vm, requestMicrophone, requestNotifications)
                }
            }
        }
    }
}

@Composable
private fun SessionRail(state: DenknotizUiState, vm: DenknotizViewModel, expanded: Boolean) {
    var search by rememberSaveable { mutableStateOf("") }
    val sessions = state.sessions.filter { !it.archived && (search.isBlank() || it.title.contains(search, true)) }
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 5.dp,
        modifier = Modifier.width(if (expanded) 286.dp else 86.dp).fillMaxHeight(),
    ) {
        Column(Modifier.padding(horizontal = if (expanded) 14.dp else 9.dp, vertical = 16.dp)) {
            GoldActionButton(
                text = if (expanded) "Neue Sitzung" else null,
                icon = Icons.Default.Add,
                reducedMotion = state.settings.reducedMotion,
                onClick = vm::newSession,
                modifier = Modifier.fillMaxWidth(),
            )
            if (expanded) {
                OutlinedTextField(
                    value = search, onValueChange = { search = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Suchen") },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                )
            } else Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sessions, key = SessionEntity::id) { session ->
                    SessionItem(session, state.interaction.selectedSessionId == session.id, expanded) { vm.selectSession(session.id) }
                }
            }
        }
    }
}

@Composable
private fun SessionItem(session: SessionEntity, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(if (expanded) 13.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) { Text(session.title.take(1).uppercase(), fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
            if (expanded) {
                Text(session.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f).padding(start = 10.dp))
                if (session.pinned) Icon(Icons.Default.PushPin, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun WorkbenchTopBar(state: DenknotizUiState, vm: DenknotizViewModel, openDrawer: () -> Unit) {
    val session = state.bundle?.session
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var focus by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(openDrawer) { Icon(Icons.Default.Menu, "Menü") }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(session?.title ?: "Denknotiz", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("CHRONOLOGISCHE WERKBANK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        OutlinedButton(onClick = { focus = true }, enabled = !state.interaction.evaluating) {
            if (state.interaction.evaluating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.AutoAwesome, null)
            Text("Auswerten", modifier = Modifier.padding(start = 7.dp))
        }
        Box {
            IconButton(onClick = { menu = true }, enabled = session != null) { Icon(Icons.Default.MoreVert, "Sitzungsmenü") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem({ Text("Umbenennen") }, { menu = false; rename = true }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                DropdownMenuItem({ Text(if (session?.pinned == true) "Lösen" else "Anpinnen") }, { menu = false; session?.let(vm::togglePin) }, leadingIcon = { Icon(Icons.Default.PushPin, null) })
                DropdownMenuItem({ Text("Archivieren") }, { menu = false; session?.let(vm::toggleArchive) }, leadingIcon = { Icon(Icons.Default.Archive, null) })
                DropdownMenuItem({ Text("Löschen") }, { menu = false; delete = true }, leadingIcon = { Icon(Icons.Default.Delete, null) })
            }
        }
    }
    if (rename && session != null) RenameDialog(session.title, { rename = false }) { vm.renameSession(session, it); rename = false }
    if (delete && session != null) ConfirmDialog("Sitzung löschen?", "Alle Notizen und Auswertungen dieser Sitzung werden gelöscht.", { delete = false }) {
        vm.deleteSession(session); delete = false
    }
    if (focus) FocusDialog(state, vm, { focus = false }) { vm.evaluate(); focus = false }
}

@Composable
private fun Timeline(
    entries: List<EntryEntity>,
    snapshots: List<EvaluationSnapshotEntity>,
    boundary: Long,
    vm: DenknotizViewModel,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty() && snapshots.none { it.status == SnapshotStatus.FAILED }) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ein ruhiger Ort für unfertige Gedanken.", style = MaterialTheme.typography.headlineMedium)
                Text("Schreibe oder diktiere eine erste Notiz.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
        return
    }
    val failures = snapshots.filter { it.status == SnapshotStatus.FAILED }
    LazyColumn(modifier, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(entries, key = EntryEntity::id, contentType = { it.type.name }) { entry ->
            EntryCard(entry, vm)
            if (boundary > 0 && entry.ordinal == boundary) BoundaryMarker()
        }
        items(failures, key = EvaluationSnapshotEntity::id) { snapshot -> FailedSnapshot(snapshot, vm) }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun EntryCard(entry: EntryEntity, vm: DenknotizViewModel) {
    val ai = entry.type == EntryType.AI_RESPONSE
    var menu by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val uri = LocalUriHandler.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (ai) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        ),
        border = BorderStroke(1.dp, if (ai) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (ai) "KI-AUSWERTUNG" else entry.title, style = MaterialTheme.typography.labelSmall,
                    color = if (ai) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                if (entry.historical) AssistChip(onClick = {}, label = { Text("Historisch") }, modifier = Modifier.padding(start = 10.dp))
                Spacer(Modifier.weight(1f))
                IconButton({ vm.read(entry.text) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PlayArrow, "${entry.title} vorlesen")
                }
                Box {
                    IconButton({ menu = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.MoreVert, "Aktionen") }
                    DropdownMenu(menu, { menu = false }) {
                        if (!ai) {
                            DropdownMenuItem({ Text("Überschrift bearbeiten") }, { menu = false; editTitle = true }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                            DropdownMenuItem({ Text("Text bearbeiten") }, { menu = false; edit = true }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                            DropdownMenuItem({ Text("Mit KI verbessern") }, { menu = false; vm.improveNote(entry) }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null) })
                            if (entry.originalText != null) DropdownMenuItem({ Text("Original wiederherstellen") }, { menu = false; vm.restoreNote(entry) }, leadingIcon = { Icon(Icons.Default.Undo, null) })
                            DropdownMenuItem({ Text("Duplizieren") }, { menu = false; vm.duplicateNote(entry) }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                        }
                        DropdownMenuItem({ Text("Kopieren") }, { menu = false; clipboard.setText(AnnotatedString(entry.text)) }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
                        if (ai) {
                            DropdownMenuItem({ Text("Als Notiz übernehmen") }, { menu = false; vm.responseAsNote(entry) }, leadingIcon = { Icon(Icons.Default.Add, null) })
                        }
                        DropdownMenuItem({ Text("Löschen") }, { menu = false; delete = true }, leadingIcon = { Icon(Icons.Default.Delete, null) })
                    }
                }
            }
            Text(formatTimestamp(entry.createdAt), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            SelectionContainer { Text(entry.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp)) }
            citationList(entry.citationsJson).forEach { source ->
                Text(source.second, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { uri.openUri(source.first) })
            }
        }
    }
    if (edit) EditDialog(entry.text, { edit = false }, confirm = { vm.editNote(entry, it); edit = false })
    if (editTitle) EditDialog(entry.title, { editTitle = false }, confirm = {
        vm.editNoteTitle(entry, it); editTitle = false
    }, title = "Überschrift bearbeiten")
    if (delete) ConfirmDialog(if (ai) "KI-Antwort löschen?" else "Notiz löschen?", "Dieser Eintrag wird dauerhaft entfernt.", { delete = false }) {
        vm.deleteEntry(entry); delete = false
    }
}

@Composable
private fun BoundaryMarker() {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Divider(Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
        Text(" AUSWERTUNGSGRENZE ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Divider(Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
    }
}

@Composable
private fun FailedSnapshot(snapshot: EvaluationSnapshotEntity, vm: DenknotizViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auswertung nicht abgeschlossen", fontWeight = FontWeight.SemiBold)
                Text(snapshot.error, style = MaterialTheme.typography.bodyMedium)
                Text("Snapshot: ${snapshot.chunkCount} Chunk(s), ${if (snapshot.webEnabled) "mit Web" else "ohne Web"}", style = MaterialTheme.typography.labelSmall)
            }
            TextButton({ vm.retry(snapshot.id) }) { Icon(Icons.Default.Refresh, null); Text("Erneut") }
        }
    }
}

@Composable
private fun Composer(
    state: DenknotizUiState,
    vm: DenknotizViewModel,
    requestMicrophone: (() -> Unit) -> Unit,
    requestNotifications: () -> Unit,
) {
    Surface(tonalElevation = 8.dp, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(14.dp)) {
            OutlinedTextField(
                value = state.interaction.draft, onValueChange = vm::setDraft,
                placeholder = { Text("Gedanke, Beobachtung oder offene Frage …") },
                shape = RoundedCornerShape(16.dp), minLines = 2, maxLines = 7, modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = vm::improveDraft, enabled = state.interaction.draft.isNotBlank() && !state.interaction.improving) {
                    if (state.interaction.improving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoAwesome, null)
                    Text("Verbessern", modifier = Modifier.padding(start = 5.dp))
                }
                if (state.interaction.undoDraft != null) TextButton(vm::undoImprovement) { Icon(Icons.Default.Undo, null); Text("Undo") }
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    onClick = {
                        requestMicrophone {
                            if (state.interaction.recording) vm.stopRecording() else vm.startRecording()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (state.interaction.recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    enabled = !state.interaction.transcribing,
                ) {
                    if (state.interaction.transcribing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(if (state.interaction.recording) Icons.Default.Stop else Icons.Default.Mic, "Diktat")
                }
                GoldActionButton(
                    text = "Senden", icon = Icons.Default.Send, reducedMotion = state.settings.reducedMotion,
                    onClick = vm::sendDraft, enabled = state.interaction.draft.isNotBlank(), modifier = Modifier.padding(start = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun RejectedAudioBar(state: DenknotizUiState, vm: DenknotizViewModel) {
    if (state.interaction.rejectedAudioPath == null) return
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Vollständig gefilterte Aufnahme", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(vm::playRejectedAudio) { Icon(Icons.Default.PlayArrow, null); Text("Prüfen") }
            TextButton(vm::retryRejectedAudio) { Icon(Icons.Default.Refresh, null); Text("Erneut") }
            IconButton(vm::deleteRejectedAudio) { Icon(Icons.Default.Delete, "Verwerfen") }
        }
    }
}

@Composable
private fun SpeechBar(state: DenknotizUiState, vm: DenknotizViewModel) {
    if (!state.speech.active) return
    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Absatz ${state.speech.paragraphIndex + 1}/${state.speech.paragraphCount}", style = MaterialTheme.typography.labelSmall)
                Text(state.speech.paragraphText, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(vm::speechPrevious) { Icon(Icons.Default.KeyboardArrowLeft, "Vorheriger Absatz") }
            IconButton(vm::speechToggle) { Icon(if (state.speech.paused) Icons.Default.PlayArrow else Icons.Default.Pause, "Pause oder weiter") }
            IconButton(vm::speechNext) { Icon(Icons.Default.KeyboardArrowRight, "Nächster Absatz") }
            IconButton(vm::speechStop) { Icon(Icons.Default.Stop, "Stopp") }
        }
    }
}

@Composable
private fun FocusDialog(state: DenknotizUiState, vm: DenknotizViewModel, dismiss: () -> Unit, confirm: () -> Unit) {
    var profileMenu by remember { mutableStateOf(false) }
    val boundary = state.bundle?.boundary?.lastIncludedOrdinal ?: 0
    val chars = state.bundle?.entries?.filter { it.type == EntryType.NOTE && it.ordinal > boundary }?.sumOf { it.text.length } ?: 0
    val chunks = ((chars + DenknotizRepository.MODEL_CHUNK_CHARS - 1) / DenknotizRepository.MODEL_CHUNK_CHARS).coerceAtLeast(1)
    AlertDialog(
        onDismissRequest = dismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Auswertungsfokus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(state.interaction.focusQuestion, vm::setFocus,
                    label = { Text("Lokale Fokusfrage") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton({ profileMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(AnalysisProfiles.firstOrNull { it.id == state.settings.profileId }?.label ?: "Profil", modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(profileMenu, { profileMenu = false }) {
                        AnalysisProfiles.forEach { profile -> DropdownMenuItem({ Text(profile.label) }, {
                            vm.setProfile(profile.id); profileMenu = false
                        }) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Websuche"); Text("Quellen werden als Links gespeichert.", style = MaterialTheme.typography.bodyMedium) }
                    Switch(state.interaction.webEnabled, vm::setWeb)
                }
                Text("Modelllimit: $chunks Chunk(s), keine Kürzung", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { Button(confirm, enabled = chars > 0) { Text("Snapshot auswerten") } },
        dismissButton = { TextButton(dismiss) { Text("Abbrechen") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(
    state: DenknotizUiState,
    vm: DenknotizViewModel,
    openDrawer: () -> Unit,
    requestMicrophone: (() -> Unit) -> Unit,
    createBackup: () -> Unit,
    openBackup: () -> Unit,
) {
    var showSecrets by rememberSaveable { mutableStateOf(false) }
    var groq by remember(state.settings.groqKey) { mutableStateOf(state.settings.groqKey) }
    var google by remember(state.settings.googleKey) { mutableStateOf(state.settings.googleKey) }
    var qwen by remember(state.settings.qwenKey) { mutableStateOf(state.settings.qwenKey) }
    var codex by remember(state.settings.codexToken) { mutableStateOf(state.settings.codexToken) }
    var chirpVoice by remember(state.settings.chirpVoice) { mutableStateOf(state.settings.chirpVoice) }
    var edgeVoice by remember(state.settings.edgeVoice) { mutableStateOf(state.settings.edgeVoice) }
    var qwenVoice by remember(state.settings.qwenVoiceId) { mutableStateOf(state.settings.qwenVoiceId) }
    var enrollmentName by rememberSaveable { mutableStateOf("Meine Stimme") }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(openDrawer) { Icon(Icons.Default.Menu, "Menü") }
            Text("Einstellungen", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 8.dp))
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SettingsCard("Codex / ChatGPT") {
                Text(if (state.codexConnected) "Verbunden: ${state.codexEmail ?: "Konto erkannt"}" else "Nicht verbunden")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.codexConnected) OutlinedButton(vm::disconnectCodex) { Text("Trennen") }
                    else Button(vm::connectCodex, enabled = !state.interaction.connectingCodex) {
                        if (state.interaction.connectingCodex) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Device-Code verbinden", modifier = Modifier.padding(start = 5.dp))
                    }
                }
                state.interaction.deviceCode?.let { code ->
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Code: ${code.code}", style = MaterialTheme.typography.titleLarge)
                            Text(code.verificationUrl, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                EnumChoice("Modell", state.settings.model, CodexModel.entries.toList(), { it.label }, vm::setModel)
                EnumChoice("Reasoning", state.settings.reasoning, ReasoningEffort.entries.toList(), { it.label }, vm::setReasoning)
                val selectedProfile = AnalysisProfiles.firstOrNull { it.id == state.settings.profileId } ?: AnalysisProfiles.first()
                EnumChoice("Analyseprofil", selectedProfile, AnalysisProfiles, { it.label }, { vm.setProfile(it.id) }, selectedKey = { it.id })
            }
            SettingsCard("Zugänge") {
                SecretField("Groq API-Key", groq, { groq = it }, showSecrets)
                SecretField("Google Cloud TTS-Key", google, { google = it }, showSecrets)
                SecretField("Qwen / DashScope-Key", qwen, { qwen = it }, showSecrets)
                SecretField("Codex Token (optional)", codex, { codex = it }, showSecrets)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Schlüssel anzeigen", modifier = Modifier.weight(1f)); Switch(showSecrets, { showSecrets = it })
                }
                Button({ vm.setKeys(groq, google, qwen, codex) }) { Icon(Icons.Default.Save, null); Text("Lokal verschlüsselt speichern") }
            }
            SettingsCard("Sprachausgabe") {
                EnumChoice("Anbieter", state.settings.ttsProvider, TtsProvider.entries.toList(), { it.label }, vm::setTtsProvider)
                OutlinedTextField(chirpVoice, { chirpVoice = it }, label = { Text("Chirp-Stimme") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(edgeVoice, { edgeVoice = it }, label = { Text("Edge-Stimme") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(qwenVoice, { qwenVoice = it }, label = { Text("Qwen-Stimm-ID") }, modifier = Modifier.fillMaxWidth())
                Button({ vm.setVoices(chirpVoice, edgeVoice, qwenVoice) }) { Text("Stimmen speichern") }
                Text("Tempo ${(state.settings.speechRate * 100).toInt()} %")
                Slider(state.settings.speechRate, vm::setSpeechRate, valueRange = 0.7f..1.3f)
                Text("Anbieter wechseln nie automatisch.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            SettingsCard("Qwen-Stimmverwaltung") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(vm::loadQwenVoices, enabled = !state.interaction.loadingVoices) { Icon(Icons.Default.Refresh, null); Text("Stimmen laden") }
                    if (state.interaction.loadingVoices) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                state.interaction.qwenVoices.forEach { rawVoice ->
                    val voice = rawVoice.copy(name = state.settings.qwenVoiceNames[rawVoice.id] ?: rawVoice.name)
                    VoiceRow(
                        voice, state.settings.qwenVoiceId == voice.id,
                        select = { vm.setVoices(state.settings.chirpVoice, state.settings.edgeVoice, voice.id) },
                        delete = { vm.deleteQwenVoice(voice) },
                        rename = { vm.renameQwenVoice(voice, it) },
                    )
                }
                OutlinedTextField(enrollmentName, { enrollmentName = it }, label = { Text("Name der neuen Stimme") }, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        requestMicrophone {
                            if (state.interaction.enrollingVoice) vm.stopVoiceEnrollmentRecording(enrollmentName)
                            else vm.startVoiceEnrollmentRecording()
                        }
                    },
                    enabled = state.settings.qwenKey.isNotBlank() && !state.interaction.loadingVoices,
                ) {
                    Icon(if (state.interaction.enrollingVoice) Icons.Default.Stop else Icons.Default.Mic, null)
                    Text(if (state.interaction.enrollingVoice) "Aufnahme beenden und anlegen" else "Stimmprobe aufnehmen")
                }
            }
            SettingsCard("Darstellung") {
                EnumChoice("Theme", state.settings.theme, AppTheme.entries.toList(), { it.label }, vm::setTheme)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Reduzierte Bewegung"); Text("Animationen werden auf 0 ms gesetzt.", style = MaterialTheme.typography.bodyMedium) }
                    Switch(state.settings.reducedMotion, vm::setReducedMotion)
                }
            }
            SettingsCard("Daten") {
                Text("JSON enthält Sitzungen, Notizen, Snapshots und Grenzen, aber keine Zugangsdaten.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(createBackup) { Text("Exportieren") }
                    OutlinedButton(openBackup) { Text("Importieren und mergen") }
                }
            }
            SettingsCard("Version") {
                Text("Denknotiz ${BuildConfig.VERSION_NAME}")
                Text("Version gesetzt: ${BuildConfig.VERSION_BUMPED_AT}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)), modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
private fun SecretField(label: String, value: String, onChange: (String) -> Unit, visible: Boolean) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
}

@Composable
private fun <T> EnumChoice(
    label: String,
    selected: T,
    values: List<T>,
    text: (T) -> String,
    onSelect: (T) -> Unit,
    selectedKey: (T) -> Any = { it as Any },
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall)
                Text(values.firstOrNull { selectedKey(it) == selectedKey(selected) }?.let(text) ?: selected.toString())
            }
        }
        DropdownMenu(expanded, { expanded = false }) {
            values.forEach { item -> DropdownMenuItem({ Text(text(item)) }, { onSelect(item); expanded = false },
                trailingIcon = { if (selectedKey(item) == selectedKey(selected)) Icon(Icons.Default.Check, null) }) }
        }
    }
}

@Composable
private fun VoiceRow(voice: QwenVoice, selected: Boolean, select: () -> Unit, delete: () -> Unit, rename: (String) -> Unit = {}) {
    var editing by remember { mutableStateOf(false) }
    Surface(color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = select)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(voice.name, fontWeight = FontWeight.SemiBold); Text(voice.createdAt, style = MaterialTheme.typography.labelSmall) }
            if (selected) Icon(Icons.Default.Check, "Gewählt", tint = MaterialTheme.colorScheme.primary)
            IconButton({ editing = true }) { Icon(Icons.Default.Edit, "Stimme lokal umbenennen") }
            IconButton(delete) { Icon(Icons.Default.Delete, "Stimme löschen") }
        }
    }
    if (editing) EditDialog(voice.name, { editing = false }, { rename(it); editing = false }, "Stimme umbenennen")
}

@Composable
private fun GoldActionButton(
    text: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f,
        tween(if (reducedMotion) 0 else 240, easing = MotionEasing), label = "3dPress")
    Button(
        onClick = onClick, enabled = enabled, interactionSource = source,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
        modifier = modifier.scale(scale).border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(20.dp)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(icon, null)
        text?.let { Text(it, modifier = Modifier.padding(start = 7.dp)) }
    }
}

@Composable
private fun RenameDialog(current: String, dismiss: () -> Unit, confirm: (String) -> Unit) = EditDialog(current, dismiss, confirm, "Sitzung umbenennen")

@Composable
private fun EditDialog(current: String, dismiss: () -> Unit, confirm: (String) -> Unit, title: String = "Notiz bearbeiten") {
    var value by rememberSaveable(current) { mutableStateOf(current) }
    AlertDialog(onDismissRequest = dismiss, shape = RoundedCornerShape(28.dp), title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, minLines = 3, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button({ confirm(value) }, enabled = value.isNotBlank()) { Text("Speichern") } },
        dismissButton = { TextButton(dismiss) { Text("Abbrechen") } })
}

@Composable
private fun ConfirmDialog(title: String, text: String, dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, shape = RoundedCornerShape(28.dp), title = { Text(title) }, text = { Text(text) },
        confirmButton = { Button(confirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Löschen") } },
        dismissButton = { TextButton(dismiss) { Text("Abbrechen") } })
}

private fun citationList(raw: String): List<Pair<String, String>> = runCatching {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { index -> array.optJSONObject(index)?.let { json ->
        json.optString("url").takeIf(String::isNotBlank)?.let { it to json.optString("title").ifBlank { it } }
    } }
}.getOrDefault(emptyList())

private fun formatTimestamp(value: Long): String = DateFormat.getDateTimeInstance(
    DateFormat.SHORT,
    DateFormat.SHORT,
    Locale.GERMANY,
).format(Date(value))
