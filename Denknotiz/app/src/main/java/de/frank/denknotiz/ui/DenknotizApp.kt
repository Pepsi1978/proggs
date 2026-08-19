package de.frank.denknotiz.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import de.frank.denknotiz.BuildConfig
import de.frank.denknotiz.data.AppTheme
import de.frank.denknotiz.data.BackupPayload
import de.frank.denknotiz.data.CodexModel
import de.frank.denknotiz.data.DenknotizRepository
import de.frank.denknotiz.data.ReasoningEffort
import de.frank.denknotiz.data.SettingsSnapshot
import de.frank.denknotiz.data.TtsProvider
import de.frank.denknotiz.data.attachmentsFromJson
import de.frank.denknotiz.data.local.EntryEntity
import de.frank.denknotiz.data.local.EntryType
import de.frank.denknotiz.data.local.EvaluationSnapshotEntity
import de.frank.denknotiz.data.local.FolderEntity
import de.frank.denknotiz.data.local.SessionEntity
import de.frank.denknotiz.data.local.SnapshotStatus
import de.frank.denknotiz.domain.AnalysisProfiles
import de.frank.denknotiz.domain.AnalysisProfile
import de.frank.denknotiz.domain.profileInstruction
import de.frank.denknotiz.domain.profileLabel
import de.frank.denknotiz.tts.SelectableVoice
import de.frank.denknotiz.tts.VoiceCatalog
import de.frank.denknotiz.ui.theme.DenknotizTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import org.json.JSONArray

private val MotionEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** Gelb des Favoritensterns – in beiden Themes gleich gut lesbar. */
private val FavoriteGold = Color(0xFFF5B72A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenknotizApp(
    viewModel: DenknotizViewModel,
    requestMicrophone: (() -> Unit) -> Unit,
    requestNotifications: () -> Unit,
    createBackup: () -> Unit,
    openBackup: () -> Unit,
    @Suppress("UNUSED_PARAMETER") requestFingerprint: (String, () -> Unit) -> Unit = { _, aktion -> aktion() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DenknotizTheme(state.settings.theme) {
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val snackbar = remember { SnackbarHostState() }
        BackHandler(enabled = state.interaction.section == AppSection.SETTINGS) {
            viewModel.selectSection(AppSection.WORKBENCH)
        }
        LaunchedEffect(state.interaction.message) {
            state.interaction.message?.let { snackbar.showSnackbar(it); viewModel.consumeMessage() }
        }
        LaunchedEffect(state.speech.error) {
            if (state.speech.error.isNotBlank()) snackbar.showSnackbar(state.speech.error)
        }
        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                SessionDrawer(
                    state = state,
                    vm = viewModel,
                    requestFingerprint = requestFingerprint,
                    selectSession = { id -> viewModel.selectSession(id); scope.launch { drawer.close() } },
                    openSettings = { viewModel.selectSection(AppSection.SETTINGS); scope.launch { drawer.close() } },
                    closeDrawer = { scope.launch { drawer.close() } },
                )
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
                            requestFingerprint = requestFingerprint,
                        )
                        AppSection.SETTINGS -> SettingsScreen(
                            state, viewModel, openDrawer = { scope.launch { drawer.open() } },
                            requestMicrophone = requestMicrophone, requestNotifications = requestNotifications,
                            createBackup = createBackup, openBackup = openBackup,
                            requestFingerprint = requestFingerprint,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionDrawer(
    state: DenknotizUiState,
    vm: DenknotizViewModel,
    requestFingerprint: (String, () -> Unit) -> Unit,
    selectSession: (String) -> Unit,
    openSettings: () -> Unit,
    closeDrawer: () -> Unit,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(340.dp),
    ) {
        NoteLibrary(
            state = state,
            vm = vm,
            requestFingerprint = requestFingerprint,
            selectSession = selectSession,
            openSettings = openSettings,
            closeDrawer = closeDrawer,
            modifier = Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

/**
 * Zweigeteilte Bibliothek: oben die Reiter (Alle Notizen, Favoriten, Geschützte Notizen,
 * Papierkorb, Ordner), darunter hinter einem Trennstrich die Notizen der gewählten Ansicht,
 * die neueste zuoberst.
 */
@Composable
private fun NoteLibrary(
    state: DenknotizUiState,
    vm: DenknotizViewModel,
    requestFingerprint: (String, () -> Unit) -> Unit,
    selectSession: (String) -> Unit,
    openSettings: (() -> Unit)? = null,
    closeDrawer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var search by rememberSaveable { mutableStateOf("") }
    var foldersOpen by rememberSaveable { mutableStateOf(false) }
    var manageFolders by remember { mutableStateOf(false) }
    var moveTarget by remember { mutableStateOf<SessionEntity?>(null) }
    var emptyTrash by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<SessionEntity?>(null) }

    val view = state.interaction.drawerView
    val unlocked = state.interaction.securedUnlocked
    // „Alle Notizen“ heißt alle: geschützte sind mitgezählt und mit aufgelistet, nur ihr
    // Inhalt bleibt bis zum Fingerabdruck zu. Draußen ist einzig, was im Papierkorb liegt.
    val alleNotizen = state.sessions.filter { it.deletedAt == null }
    val favoriten = alleNotizen.filter { it.favorite }
    val geschuetzte = alleNotizen.filter { it.secured }
    val papierkorb = state.sessions.filter { it.deletedAt != null }
    val ordner = state.folders
    val gewaehlterOrdner = ordner.firstOrNull { it.id == state.interaction.selectedFolderId }

    val liste = when (view) {
        DrawerView.ALL -> alleNotizen
        DrawerView.FAVORITES -> favoriten
        DrawerView.SECURED -> geschuetzte
        DrawerView.TRASH -> papierkorb
        DrawerView.FOLDER -> alleNotizen.filter { it.folderId == gewaehlterOrdner?.id }
    }.filter { search.isBlank() || it.title.contains(search, true) }

    val ueberschrift = when (view) {
        DrawerView.ALL -> "Alle Notizen"
        DrawerView.FAVORITES -> "Favoriten"
        DrawerView.SECURED -> "Geschützte Notizen"
        DrawerView.TRASH -> "Papierkorb"
        DrawerView.FOLDER -> gewaehlterOrdner?.name ?: "Ordner"
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("DENKNOTIZ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text("Notizen", style = MaterialTheme.typography.headlineSmall)
            }
            if (openSettings != null) IconButton(openSettings) { Icon(Icons.Default.Settings, "Einstellungen") }
        }
        GoldActionButton(
            text = "Neue Notiz",
            icon = Icons.Default.Add,
            reducedMotion = state.settings.reducedMotion,
            onClick = { vm.newSession(); closeDrawer() },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            label = { Text("Notizen durchsuchen") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        )
        LibraryTab(Icons.Default.Notes, "Alle Notizen", alleNotizen.size, view == DrawerView.ALL) {
            vm.selectView(DrawerView.ALL)
        }
        LibraryTab(Icons.Default.Star, "Favoriten", favoriten.size, view == DrawerView.FAVORITES,
            iconTint = FavoriteGold) { vm.selectView(DrawerView.FAVORITES) }
        LibraryTab(Icons.Default.Lock, "Geschützte Notizen", geschuetzte.size, view == DrawerView.SECURED) {
            vm.selectView(DrawerView.SECURED)
        }
        LibraryTab(Icons.Default.Delete, "Papierkorb", papierkorb.size, view == DrawerView.TRASH) {
            vm.selectView(DrawerView.TRASH)
        }
        Divider(
            Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        )
        LibraryTab(
            icon = Icons.Default.Folder,
            label = "Ordner",
            count = ordner.size,
            selected = view == DrawerView.FOLDER,
            trailing = {
                Icon(
                    if (foldersOpen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        ) { foldersOpen = !foldersOpen }
        if (foldersOpen) {
            Column(Modifier.padding(start = 14.dp)) {
                TextButton({ manageFolders = true }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CreateNewFolder, null, modifier = Modifier.size(18.dp))
                    Text("Ordner verwalten", Modifier.padding(start = 8.dp).weight(1f))
                }
                if (ordner.isEmpty()) {
                    Text(
                        "Noch kein Ordner angelegt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 12.dp, bottom = 6.dp),
                    )
                }
                ordner.forEach { folder ->
                    LibraryTab(
                        icon = Icons.Default.FolderOpen,
                        label = folder.name,
                        count = alleNotizen.count { it.folderId == folder.id },
                        selected = view == DrawerView.FOLDER && gewaehlterOrdner?.id == folder.id,
                    ) { vm.selectFolder(folder.id) }
                }
            }
        }
        Divider(
            Modifier.padding(top = 10.dp, bottom = 8.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        )
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                ueberschrift.uppercase(Locale.GERMAN),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            if (view == DrawerView.TRASH && papierkorb.isNotEmpty()) {
                TextButton({ emptyTrash = true }) { Text("Leeren") }
            }
        }
        run {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(liste, key = SessionEntity::id) { session ->
                    SessionItem(
                        session = session,
                        selected = state.interaction.selectedSessionId == session.id,
                        expanded = true,
                        inTrash = view == DrawerView.TRASH,
                        folderName = state.folders.firstOrNull { it.id == session.folderId }?.name,
                        locked = session.secured && !unlocked,
                        onClick = {
                            // Eine geschützte Notiz öffnet sich erst nach dem Fingerabdruck —
                            // aus jedem Reiter heraus, nicht nur aus „Geschützte Notizen“.
                            if (view == DrawerView.TRASH) Unit
                            else if (session.secured && !unlocked) {
                                requestFingerprint("Geschützte Notiz öffnen") {
                                    vm.unlockSecured(); selectSession(session.id)
                                }
                            } else selectSession(session.id)
                        },
                        onToggleFavorite = { vm.toggleFavorite(session) },
                        // Schützen und Freigeben gehen beide nur über den Fingerabdruck —
                        // sonst nähme ihn jeder, der das Gerät in der Hand hält, in zwei
                        // Tipps wieder ab.
                        canSecure = session.secured || state.settings.fingerprintLock,
                        onSecure = {
                            val schuetzen = !session.secured
                            requestFingerprint(if (schuetzen) "Notiz schützen" else "Schutz aufheben") {
                                vm.setSecured(session, schuetzen)
                            }
                        },
                        onMove = { moveTarget = session },
                        onRename = { renameTarget = session },
                        onTrash = { vm.trashSession(session) },
                        onRestore = { vm.restoreSession(session) },
                        onDeleteForever = { vm.deleteSession(session) },
                    )
                }
                if (liste.isEmpty()) {
                    item {
                        Text(
                            if (search.isBlank()) "Hier liegt noch keine Notiz." else "Keine Notiz gefunden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
        }
    }

    if (manageFolders) FolderManagerDialog(state, vm) { manageFolders = false }
    moveTarget?.let { session -> MoveToFolderDialog(session, state, vm) { moveTarget = null } }
    renameTarget?.let { session ->
        RenameDialog(session.title, { renameTarget = null }) { vm.renameSession(session, it); renameTarget = null }
    }
    if (emptyTrash) ConfirmDialog(
        "Papierkorb leeren?",
        "Alle Notizen im Papierkorb werden endgültig gelöscht.",
        { emptyTrash = false },
    ) { vm.emptyTrash(); emptyTrash = false }
}

@Composable
private fun LibraryTab(
    icon: ImageVector,
    label: String,
    count: Int,
    selected: Boolean,
    iconTint: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon, null, modifier = Modifier.size(20.dp),
                tint = iconTint
                    ?: if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
            if (count > 0) Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            trailing?.invoke()
        }
    }
}

@Composable
private fun FolderManagerDialog(state: DenknotizUiState, vm: DenknotizViewModel, dismiss: () -> Unit) {
    var neu by remember { mutableStateOf("") }
    var umbenennen by remember { mutableStateOf<FolderEntity?>(null) }
    var loeschen by remember { mutableStateOf<FolderEntity?>(null) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Ordner verwalten") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = neu,
                        onValueChange = { neu = it },
                        singleLine = true,
                        label = { Text("Neuer Ordner") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton({ if (neu.isNotBlank()) { vm.createFolder(neu); neu = "" } }) {
                        Icon(Icons.Default.Add, "Ordner anlegen")
                    }
                }
                Divider(Modifier.padding(vertical = 10.dp))
                if (state.folders.isEmpty()) {
                    Text(
                        "Noch kein Ordner angelegt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    state.folders.forEach { folder ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, null, modifier = Modifier.size(18.dp))
                            Text(
                                folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(start = 10.dp),
                            )
                            IconButton({ umbenennen = folder }) { Icon(Icons.Default.Edit, "Umbenennen", Modifier.size(18.dp)) }
                            IconButton({ loeschen = folder }) { Icon(Icons.Default.Delete, "Löschen", Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(dismiss) { Text("Fertig") } },
    )
    umbenennen?.let { folder ->
        EditDialog(folder.name, { umbenennen = null }, { vm.renameFolder(folder, it); umbenennen = null }, "Ordner umbenennen")
    }
    loeschen?.let { folder ->
        ConfirmDialog(
            "Ordner löschen?",
            "Die Notizen bleiben erhalten und liegen danach in keinem Ordner.",
            { loeschen = null },
        ) { vm.deleteFolder(folder); loeschen = null }
    }
}

@Composable
private fun MoveToFolderDialog(session: SessionEntity, state: DenknotizUiState, vm: DenknotizViewModel, dismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("In Ordner verschieben") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                LibraryTab(Icons.Default.Notes, "Kein Ordner", 0, session.folderId == null) {
                    vm.moveToFolder(session, null); dismiss()
                }
                state.folders.forEach { folder ->
                    LibraryTab(Icons.Default.Folder, folder.name, 0, session.folderId == folder.id) {
                        vm.moveToFolder(session, folder.id); dismiss()
                    }
                }
                if (state.folders.isEmpty()) {
                    Text(
                        "Erst über „Ordner verwalten“ einen Ordner anlegen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(dismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun Workbench(
    state: DenknotizUiState,
    vm: DenknotizViewModel,
    openDrawer: () -> Unit,
    requestMicrophone: (() -> Unit) -> Unit,
    requestNotifications: () -> Unit,
    requestFingerprint: (String, () -> Unit) -> Unit,
) {
    LaunchedEffect(state.speech.active) { if (state.speech.active) requestNotifications() }
    var focus by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding()) {
        val expanded = maxWidth >= 840.dp
        Row(Modifier.fillMaxSize()) {
            if (expanded) SessionRail(state, vm, requestFingerprint)
            Column(Modifier.weight(1f).fillMaxHeight()) {
                WorkbenchTopBar(state, vm, openDrawer, onToggleTheme = vm::toggleTheme, requestFingerprint = requestFingerprint)
                val bundle = state.bundle
                if (bundle == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Timeline(
                        bundle.entries,
                        bundle.snapshots,
                        bundle.boundary?.lastIncludedOrdinal ?: 0,
                        vm,
                        evaluating = state.interaction.evaluating,
                        onEvaluate = { focus = true },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                    SpeechBar(state, vm)
                    RejectedAudioBar(state, vm)
                    Composer(state, vm, requestMicrophone, requestNotifications)
                }
            }
        }
    }
    if (focus) FocusDialog(state, vm, { focus = false }) { vm.evaluate(); focus = false }
    // Die offene Sitzung frisch aus der Liste holen: der Kopf im Zustand ist ein Abzug von
    // vorhin, und ohne das bliebe die Sperrschicht aus, wenn man die gerade offene Notiz
    // eben erst geschützt hat.
    val offeneFrisch = state.sessions.firstOrNull { it.id == state.interaction.selectedSessionId }
    if (offeneFrisch?.secured == true && !state.interaction.securedUnlocked) {
        LockedOverlay(
            title = offeneFrisch.title,
            reducedMotion = state.settings.reducedMotion,
            onUnlock = { requestFingerprint("Geschützte Notiz öffnen") { vm.unlockSecured() } },
            onOverview = openDrawer,
        )
    }
}

/**
 * Die Sperrschicht über der Werkbank einer geschützten Notiz.
 *
 * Sie deckt den Inhalt zu, statt die Notiz zu schließen: wer den Fingerabdruck gibt, steht
 * danach genau da, wo er hinwollte. Der zweite Weg führt in die Seitenleiste zurück, damit
 * man aus einer zugesperrten Notiz auch ohne Fingerabdruck wieder herauskommt.
 */
@Composable
private fun LockedOverlay(title: String, reducedMotion: Boolean, onUnlock: () -> Unit, onOverview: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            Icon(Icons.Default.Lock, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(
                "Diese Notiz ist geschützt. Ihr Inhalt erscheint erst nach dem Fingerabdruck.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            GoldActionButton(
                text = "Mit Fingerabdruck öffnen",
                icon = Icons.Default.LockOpen,
                reducedMotion = reducedMotion,
                onClick = onUnlock,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onOverview) { Text("Zu den Notizen") }
        }
    }
}

@Composable
private fun SessionRail(state: DenknotizUiState, vm: DenknotizViewModel, requestFingerprint: (String, () -> Unit) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 5.dp,
        modifier = Modifier.width(300.dp).fillMaxHeight(),
    ) {
        NoteLibrary(
            state = state,
            vm = vm,
            requestFingerprint = requestFingerprint,
            selectSession = vm::selectSession,
            modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp, vertical = 14.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionItem(
    session: SessionEntity,
    selected: Boolean,
    expanded: Boolean,
    inTrash: Boolean = false,
    folderName: String? = null,
    locked: Boolean = false,
    canSecure: Boolean = true,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onSecure: () -> Unit = {},
    onMove: () -> Unit = {},
    onRename: () -> Unit = {},
    onTrash: () -> Unit = {},
    onRestore: () -> Unit = {},
    onDeleteForever: () -> Unit = {},
) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { menu = true }),
    ) {
        Row(Modifier.padding(if (expanded) 13.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(34.dp).background(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    session.title.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (expanded) {
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (session.favorite) Icon(
                            Icons.Default.Star, "Favorit",
                            modifier = Modifier.size(15.dp).padding(end = 3.dp), tint = FavoriteGold,
                        )
                        if (session.secured) Icon(
                            if (locked) Icons.Default.Lock else Icons.Default.LockOpen,
                            if (locked) "Geschützt" else "Geschützt und freigegeben",
                            modifier = Modifier.size(15.dp).padding(end = 3.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            session.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f),
                        )
                    }
                    val untertitel = listOfNotNull(folderName, session.deletedAt?.let { "Im Papierkorb" }).joinToString(" · ")
                    if (untertitel.isNotBlank()) Text(
                        untertitel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (session.pinned) Icon(Icons.Default.PushPin, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(
                menu, { menu = false },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp, shadowElevation = 12.dp, shape = RoundedCornerShape(16.dp),
            ) {
                if (inTrash) {
                    DropdownMenuItem({ Text("Wiederherstellen") }, { menu = false; onRestore() },
                        leadingIcon = { Icon(Icons.Default.Restore, null) })
                    DropdownMenuItem({ Text("Endgültig löschen") }, { menu = false; onDeleteForever() },
                        leadingIcon = { Icon(Icons.Default.DeleteForever, null) })
                } else {
                    DropdownMenuItem(
                        { Text(if (session.favorite) "Favorit entfernen" else "Als Favorit markieren") },
                        { menu = false; onToggleFavorite() },
                        leadingIcon = {
                            Icon(if (session.favorite) Icons.Default.Star else Icons.Default.StarBorder, null, tint = FavoriteGold)
                        },
                    )
                    if (canSecure) DropdownMenuItem(
                        { Text(if (session.secured) "Schutz aufheben" else "Notiz schützen") },
                        { menu = false; onSecure() },
                        leadingIcon = { Icon(if (session.secured) Icons.Default.LockOpen else Icons.Default.Lock, null) },
                    )
                    DropdownMenuItem({ Text("In Ordner verschieben") }, { menu = false; onMove() },
                        leadingIcon = { Icon(Icons.Default.DriveFileMove, null) })
                    DropdownMenuItem({ Text("Umbenennen") }, { menu = false; onRename() },
                        leadingIcon = { Icon(Icons.Default.Edit, null) })
                    DropdownMenuItem({ Text("In den Papierkorb") }, { menu = false; onTrash() },
                        leadingIcon = { Icon(Icons.Default.Delete, null) })
                }
            }
        }
    }
}

@Composable
private fun WorkbenchTopBar(
    state: DenknotizUiState,
    vm: DenknotizViewModel,
    openDrawer: () -> Unit,
    onToggleTheme: () -> Unit,
    requestFingerprint: (String, () -> Unit) -> Unit,
) {
    val session = state.bundle?.session
    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var move by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(openDrawer) { Icon(Icons.Default.Menu, "Menü") }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(session?.title ?: "Denknotiz", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("CHRONOLOGISCHE WERKBANK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        IconButton({ vm.selectSection(AppSection.SETTINGS) }) { Icon(Icons.Default.Settings, "Einstellungen") }
        val theme = state.settings.theme
        val istHell = theme == AppTheme.LIGHT || theme == AppTheme.GOLD_LIGHT
        IconButton(onClick = onToggleTheme) {
            Icon(if (istHell) Icons.Default.DarkMode else Icons.Default.LightMode, "Zwischen Hell- und Dunkelmodus wechseln")
        }
        Box {
            IconButton(onClick = { menu = true }, enabled = session != null) { Icon(Icons.Default.MoreVert, "Sitzungsmenü") }
            DropdownMenu(menu, { menu = false }, containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp, shadowElevation = 12.dp, shape = RoundedCornerShape(16.dp)) {
                DropdownMenuItem({ Text("Umbenennen") }, { menu = false; rename = true }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                DropdownMenuItem(
                    { Text(if (session?.favorite == true) "Favorit entfernen" else "Als Favorit markieren") },
                    { menu = false; session?.let(vm::toggleFavorite) },
                    leadingIcon = {
                        Icon(if (session?.favorite == true) Icons.Default.Star else Icons.Default.StarBorder, null, tint = FavoriteGold)
                    },
                )
                if (session?.secured == true || state.settings.fingerprintLock) DropdownMenuItem(
                    { Text(if (session?.secured == true) "Schutz aufheben" else "Notiz schützen") },
                    {
                        menu = false
                        session?.let { aktuelle ->
                            val schuetzen = !aktuelle.secured
                            requestFingerprint(if (schuetzen) "Notiz schützen" else "Schutz aufheben") {
                                vm.setSecured(aktuelle, schuetzen)
                            }
                        }
                    },
                    leadingIcon = { Icon(if (session?.secured == true) Icons.Default.LockOpen else Icons.Default.Lock, null) },
                )
                DropdownMenuItem({ Text("In Ordner verschieben") }, { menu = false; move = true },
                    leadingIcon = { Icon(Icons.Default.DriveFileMove, null) })
                DropdownMenuItem({ Text(if (session?.pinned == true) "Lösen" else "Anpinnen") }, { menu = false; session?.let(vm::togglePin) }, leadingIcon = { Icon(Icons.Default.PushPin, null) })
                DropdownMenuItem({ Text("In den Papierkorb") }, { menu = false; session?.let(vm::trashSession) },
                    leadingIcon = { Icon(Icons.Default.Delete, null) })
                DropdownMenuItem({ Text("Endgültig löschen") }, { menu = false; delete = true }, leadingIcon = { Icon(Icons.Default.DeleteForever, null) })
            }
        }
    }
    if (rename && session != null) RenameDialog(session.title, { rename = false }) { vm.renameSession(session, it); rename = false }
    if (move && session != null) MoveToFolderDialog(session, state, vm) { move = false }
    if (delete && session != null) ConfirmDialog("Sitzung löschen?", "Alle Notizen und Auswertungen dieser Sitzung werden gelöscht.", { delete = false }) {
        vm.deleteSession(session); delete = false
    }
}

@Composable
private fun Timeline(
    entries: List<EntryEntity>,
    snapshots: List<EvaluationSnapshotEntity>,
    boundary: Long,
    vm: DenknotizViewModel,
    evaluating: Boolean,
    onEvaluate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty() && snapshots.none { it.status == SnapshotStatus.FAILED }) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                "Schreibe oder diktiere eine erste Notiz.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        item("auswerten") {
            OutlinedButton(
                onClick = onEvaluate,
                enabled = !evaluating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (evaluating) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.AutoAwesome, null)
                }
                Text("Auswerten", modifier = Modifier.padding(start = 7.dp))
            }
        }
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
                    DropdownMenu(menu, { menu = false }, containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 10.dp, shadowElevation = 12.dp, shape = RoundedCornerShape(16.dp)) {
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
            if (entry.text.isNotBlank()) {
                SelectionContainer { Text(entry.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 10.dp)) }
            }
            AttachmentGallery(attachmentsFromJson(entry.attachmentsJson), Modifier.padding(top = 10.dp))
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
            PendingAttachments(state.interaction.attachments, vm::removeAttachment)
            OutlinedTextField(
                value = state.interaction.draft, onValueChange = vm::setDraft,
                placeholder = { Text("Gedanke, Beobachtung oder offene Frage …") },
                shape = RoundedCornerShape(16.dp), minLines = 2, maxLines = 7, modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                AttachmentMenuButton(
                    onAttachment = vm::addAttachment,
                    onError = vm::reportMessage,
                    requestMicrophone = requestMicrophone,
                )
                TextButton(onClick = vm::improveDraft, enabled = state.interaction.draft.isNotBlank() && !state.interaction.improving) {
                    if (state.interaction.improving) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoAwesome, null)
                    Text("Verbessern", modifier = Modifier.padding(start = 5.dp))
                }
                if (state.interaction.undoDraft != null) TextButton(vm::undoImprovement) { Icon(Icons.Default.Undo, null); Text("Undo") }
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    onClick = {
                        requestNotifications()
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
                    onClick = vm::sendDraft,
                    enabled = state.interaction.draft.isNotBlank() || state.interaction.attachments.isNotEmpty(),
                    modifier = Modifier.padding(start = 9.dp),
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
    val boundary = state.bundle?.boundary?.lastIncludedOrdinal ?: 0
    val chars = state.bundle?.entries?.filter { it.type == EntryType.NOTE && it.ordinal > boundary }?.sumOf { it.text.length } ?: 0
    val chunks = ((chars + DenknotizRepository.MODEL_CHUNK_CHARS - 1) / DenknotizRepository.MODEL_CHUNK_CHARS).coerceAtLeast(1)
    val selectedProfile = AnalysisProfiles.firstOrNull { it.id == state.settings.profileId } ?: AnalysisProfiles.first()
    val activeInstruction = profileInstruction(selectedProfile, state.settings.profileInstructions)
    AlertDialog(
        onDismissRequest = dismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Auswertungsfokus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(state.interaction.focusQuestion, vm::setFocus,
                    label = { Text("Lokale Fokusfrage") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                StyledDropdownChoice(
                    label = "Analyseprofil",
                    selected = selectedProfile,
                    values = AnalysisProfiles,
                    text = { profileLabel(it, state.settings.profileNames) },
                    onSelect = { vm.setProfile(it.id) },
                    selectedKey = { it.id },
                )
                ProfileDescription(selectedProfile, state.settings)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Websuche"); Text("Quellen werden als Links gespeichert.", style = MaterialTheme.typography.bodyMedium) }
                    Switch(state.interaction.webEnabled, vm::setWeb)
                }
                Text("Modelllimit: $chunks Chunk(s), keine Kürzung", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { Button(confirm, enabled = chars > 0 && activeInstruction.isNotBlank()) { Text("Snapshot auswerten") } },
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
    requestNotifications: () -> Unit,
    createBackup: () -> Unit,
    openBackup: () -> Unit,
    requestFingerprint: (String, () -> Unit) -> Unit,
) {
    var showSecrets by rememberSaveable { mutableStateOf(false) }
    var groq by remember(state.settings.groqKey) { mutableStateOf(state.settings.groqKey) }
    var google by remember(state.settings.googleKey) { mutableStateOf(state.settings.googleKey) }
    var qwen by remember(state.settings.qwenKey) { mutableStateOf(state.settings.qwenKey) }
    var editProfile by remember { mutableStateOf<AnalysisProfile?>(null) }
    var enrollmentName by rememberSaveable { mutableStateOf("Meine Stimme") }
    val selectedProfile = AnalysisProfiles.firstOrNull { it.id == state.settings.profileId } ?: AnalysisProfiles.first()
    LaunchedEffect(state.settings.ttsProvider, state.settings.qwenKey) {
        if (state.settings.ttsProvider == TtsProvider.QWEN && state.settings.qwenKey.isNotBlank()) vm.loadQwenVoices()
    }
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
                StyledDropdownChoice("Modell", state.settings.model, CodexModel.entries.toList(), { it.label }, vm::setModel)
                StyledDropdownChoice("Reasoning", state.settings.reasoning, ReasoningEffort.entries.toList(), { it.label }, vm::setReasoning)
            }
            SettingsCard("Analyseprofile") {
                StyledDropdownChoice(
                    "Aktives Profil",
                    selectedProfile,
                    AnalysisProfiles,
                    { profileLabel(it, state.settings.profileNames) },
                    { vm.setProfile(it.id) },
                    selectedKey = { it.id },
                )
                ProfileDescription(selectedProfile, state.settings)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button({ editProfile = selectedProfile }) { Icon(Icons.Default.Edit, null); Text("Profil bearbeiten") }
                    OutlinedButton({ vm.resetProfile(selectedProfile.id) }) { Text("Zurücksetzen") }
                }
            }
            SettingsCard("Zugänge") {
                SecretField("Groq API-Key", groq, { groq = it }, showSecrets)
                SecretField("Google Cloud TTS-Key", google, { google = it }, showSecrets)
                SecretField("Qwen / DashScope-Key", qwen, { qwen = it }, showSecrets)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Schlüssel anzeigen", modifier = Modifier.weight(1f)); Switch(showSecrets, { showSecrets = it })
                }
                Button({ vm.setKeys(groq, google, qwen) }) { Icon(Icons.Default.Save, null); Text("Lokal verschlüsselt speichern") }
            }
            SettingsCard("Sprachausgabe") {
                StyledDropdownChoice("Anbieter", state.settings.ttsProvider, TtsProvider.entries.toList(), { it.label }, vm::setTtsProvider)
                when (state.settings.ttsProvider) {
                    TtsProvider.CHIRP -> VoiceDropdown(
                        label = "Chirp-Stimme",
                        selectedId = state.settings.chirpVoice,
                        voices = VoiceCatalog.chirp,
                        onSelect = { vm.setVoices(it.id, state.settings.edgeVoice, state.settings.qwenVoiceId) },
                    )
                    TtsProvider.EDGE -> VoiceDropdown(
                        label = "Edge-Stimme",
                        selectedId = state.settings.edgeVoice,
                        voices = VoiceCatalog.edge,
                        onSelect = { vm.setVoices(state.settings.chirpVoice, it.id, state.settings.qwenVoiceId) },
                    )
                    TtsProvider.QWEN -> {
                        val ownVoices = state.interaction.qwenVoices.map { voice ->
                            SelectableVoice(voice.id, state.settings.qwenVoiceNames[voice.id] ?: voice.name)
                        }
                        VoiceDropdown(
                            label = "Eigene Stimme",
                            selectedId = state.settings.qwenVoiceId,
                            voices = ownVoices,
                            onSelect = { vm.setVoices(state.settings.chirpVoice, state.settings.edgeVoice, it.id) },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(vm::loadQwenVoices, enabled = !state.interaction.loadingVoices) {
                                Icon(Icons.Default.Refresh, null); Text("Stimmen neu laden")
                            }
                            if (state.interaction.loadingVoices) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                        OutlinedTextField(enrollmentName, { enrollmentName = it }, label = { Text("Name der neuen Stimme") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                requestNotifications()
                                requestMicrophone {
                                    if (state.interaction.enrollingVoice) vm.stopVoiceEnrollmentRecording(enrollmentName)
                                    else vm.startVoiceEnrollmentRecording()
                                }
                            },
                            enabled = state.settings.qwenKey.isNotBlank() && !state.interaction.loadingVoices,
                        ) {
                            Icon(if (state.interaction.enrollingVoice) Icons.Default.Stop else Icons.Default.Mic, null)
                            Text(if (state.interaction.enrollingVoice) "Aufnahme beenden und anlegen" else "Neue eigene Stimme aufnehmen")
                        }
                    }
                }
                if (state.settings.ttsProvider == TtsProvider.QWEN) {
                    Text("Das Tempo der eigenen Stimme wird vom Qwen-Modell bestimmt.", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Tempo ${(state.settings.speechRate * 100).toInt()} %")
                    Slider(state.settings.speechRate, vm::setSpeechRate, valueRange = 0.7f..1.3f)
                }
                Text("Anbieter wechseln nie automatisch.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            SettingsCard("Darstellung") {
                StyledDropdownChoice("Theme", state.settings.theme, AppTheme.entries.toList(), { it.label }, vm::setTheme)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Reduzierte Bewegung"); Text("Animationen werden auf 0 ms gesetzt.", style = MaterialTheme.typography.bodyMedium) }
                    Switch(state.settings.reducedMotion, vm::setReducedMotion)
                }
            }
            SettingsCard("Sicherheit") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fingerprint, null, modifier = Modifier.padding(end = 10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Fingerabdruck")
                        Text(
                            "Erlaubt es, Notizen zu schützen. Öffnen, Schützen und Freigeben gehen dann nur noch über den Fingerabdruck.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Switch(
                        checked = state.settings.fingerprintLock,
                        onCheckedChange = { an ->
                            if (an) requestFingerprint("Fingerabdruck einrichten") { vm.setFingerprintLock(true) }
                            else requestFingerprint("Fingerabdruck abschalten") { vm.setFingerprintLock(false); vm.lockSecured() }
                        },
                    )
                }
                Text(
                    "Bereits geschützte Notizen bleiben geschützt und brauchen weiter den Fingerabdruck — nur neue lassen sich nicht mehr schützen.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            SettingsCard("Daten") {
                Text("JSON enthält Sitzungen, Notizen, Ordner, Snapshots und Grenzen, aber keine Zugangsdaten.")
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
    editProfile?.let { profile ->
        ProfileEditorDialog(
            profile = profile,
            settings = state.settings,
            dismiss = { editProfile = null },
            save = { name, instruction -> vm.updateProfile(profile.id, name, instruction); editProfile = null },
        )
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
private fun <T> StyledDropdownChoice(
    label: String,
    selected: T,
    values: List<T>,
    text: (T) -> String,
    onSelect: (T) -> Unit,
    selectedKey: (T) -> Any = { it as Any },
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val menuWidth = maxWidth
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.72f else 0.35f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { expanded = true },
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(values.firstOrNull { selectedKey(it) == selectedKey(selected) }?.let(text) ?: text(selected),
                        style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(menuWidth),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(16.dp),
        ) {
            values.forEach { item ->
                val chosen = selectedKey(item) == selectedKey(selected)
                DropdownMenuItem(
                    text = { Text(text(item), color = MaterialTheme.colorScheme.onSurface) },
                    onClick = { onSelect(item); expanded = false },
                    leadingIcon = { if (chosen) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary) },
                    colors = androidx.compose.material3.MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = if (chosen) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun VoiceDropdown(label: String, selectedId: String, voices: List<SelectableVoice>, onSelect: (SelectableVoice) -> Unit) {
    val selected = voices.firstOrNull { it.id == selectedId }
        ?: SelectableVoice(selectedId, if (selectedId.isBlank()) "Noch keine Stimme gewählt" else selectedId)
    StyledDropdownChoice(
        label = label,
        selected = selected,
        values = voices,
        text = SelectableVoice::name,
        onSelect = onSelect,
        selectedKey = SelectableVoice::id,
        enabled = voices.isNotEmpty(),
    )
    if (voices.isEmpty()) {
        Text("Für diesen Anbieter wurden noch keine Stimmen geladen.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ProfileDescription(profile: AnalysisProfile, settings: SettingsSnapshot) {
    val effectiveInstruction = profileInstruction(profile, settings.profileInstructions)
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(profile.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(
                effectiveInstruction.ifBlank { "Dieses Profil hat noch keine eigene Anweisung. Öffne ‚Profil bearbeiten‘ und beschreibe dort, wie die KI auswerten soll." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProfileEditorDialog(
    profile: AnalysisProfile,
    settings: SettingsSnapshot,
    dismiss: () -> Unit,
    save: (String, String) -> Unit,
) {
    var name by rememberSaveable(profile.id) { mutableStateOf(profileLabel(profile, settings.profileNames)) }
    var instruction by rememberSaveable(profile.id) { mutableStateOf(profileInstruction(profile, settings.profileInstructions)) }
    AlertDialog(
        onDismissRequest = dismiss,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Analyseprofil bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    enabled = profile.customName,
                    label = { Text(if (profile.customName) "Profilname" else "Fester Profilname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("So soll die KI auswerten") },
                    minLines = 7,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Dieser Text wird bei jeder Auswertung mit diesem Profil direkt an Codex übergeben.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button({ save(name, instruction) }, enabled = instruction.isNotBlank()) { Text("Speichern") } },
        dismissButton = { TextButton(dismiss) { Text("Abbrechen") } },
    )
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
