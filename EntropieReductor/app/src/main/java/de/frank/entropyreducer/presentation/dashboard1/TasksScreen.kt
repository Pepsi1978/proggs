package de.frank.entropyreducer.presentation.dashboard1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import de.frank.entropyreducer.data.local.entities.EntropyEntryEntity
import de.frank.entropyreducer.data.remote.drive.SyncStatus
import de.frank.entropyreducer.domain.model.EntropyCategory
import de.frank.entropyreducer.domain.model.EntryStatus
import de.frank.entropyreducer.domain.model.TimeBucket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.EntropyCategoryPill
import de.frank.entropyreducer.presentation.components.GlassCard
import de.frank.entropyreducer.presentation.components.StatusBar
import de.frank.entropyreducer.presentation.components.ThemeToggleIcon
import de.frank.entropyreducer.presentation.components.rememberMicPermissionState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import de.frank.entropyreducer.presentation.theme.color
import de.frank.entropyreducer.presentation.theme.label

/**
 * Dashboard 1 — Aufgaben (Spec §10, Referenzbild 11/21).
 */
@Composable
fun TasksScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    currentTab: String,
    vm: TasksViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val cosmos = LocalCosmos.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Lokaler State fuer den Bucket-Picker — speichert nur die Entry-ID, der
    // tatsaechliche Eintrag wird aus dem aktuellen State frisch nachgelesen damit
    // die Anzeige immer den neusten manualBucket/timeBucket-Stand zeigt.
    var bucketPickerEntryId by remember { mutableStateOf<String?>(null) }

    val micPerm = rememberMicPermissionState(
        onAllGranted = { vm.onMicClick() },
        onDenied = { denied ->
            val msg = if (Manifest.permission.RECORD_AUDIO in denied) {
                "Mikrofon-Zugriff wurde abgelehnt. Aktiviere ihn in den System-Einstellungen, damit du Einträge per Sprache erfassen kannst."
            } else {
                "Benachrichtigungs-Zugriff fehlt — die Aufnahme braucht ihn für die Foreground-Notification."
            }
            scope.launch { snackbar.showSnackbar(msg) }
        },
    )

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            vm.dismissError()
        }
    }

    val themeVm: ThemeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val themeMode by themeVm.themeMode.collectAsState()

    CosmosScaffold(
        title = "Entropie Reduktor",
        actions = {
            ThemeToggleIcon(current = themeMode, onCycle = themeVm::cycle)
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Einstellungen",
                    tint = cosmos.textPrimary,
                )
            }
        },
        bottomBar = {
            CosmosBottomBar(
                currentTab = currentTab,
                micState = state.micState,
                onTabSelected = onSwitchTab,
                onMicClick = {
                    if (micPerm.check()) vm.onMicClick() else micPerm.request()
                },
            )
        },
        // Frank-Wunsch 2026-05-09 (vierte Praezisierung): Backup-Statuszeile
        // soll direkt unter dem Titel "Entropie Reduktor" sitzen ohne unsichtbaren
        // Spacer dazwischen. Die ~18dp "unsichtbare Luft" unter dem vertikal
        // zentrierten Titel kamen aus der Material-3-TopAppBar (64dp Default-
        // Hoehe). compactHeader=true reduziert die Hoehe auf 44dp -> Luft
        // schrumpft auf ~8dp. Andere Screens bleiben unangetastet.
        compactHeader = true,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Backup-Statuszeile DIREKT unter dem Titel "Entropie Reduktor"
                // — Frank-Wunsch 2026-05-09 (praezisiert): kommt ueber die
                // StatusBar, ist die allererste Zeile unter dem Titel. Funktionen
                // identisch (Cloud-Icon + Status-Label). Wird nur angezeigt
                // wenn Drive-Backup aktiviert ist.
                if (state.driveBackupEnabled) {
                    // Frank-Wunsch 2026-05-09 (dritte Praezisierung): Zeile MUSS
                    // direkt am Boden des Titels "Entropie Reduktor" kleben —
                    // kein Spacer davor, kein Spacer danach, vertikales Padding
                    // in der Row selbst auf 0 reduziert (siehe BackupStatusBadge).
                    BackupStatusBadge(state.syncStatus, state.lastBackupAtMs)
                }
                StatusBar(percent = state.statusPercent, breakdown = state.statusBreakdown)
                Spacer(Modifier.height(8.dp))

                // Re-Score-Banner: laeuft eine Re-Bewertung aller offenen Aufgaben
                // mit der aktuellen priorityScore-Doktrin? Frank-Wunsch 2026-05-09:
                // beim ersten Start nach Doktrin-Update sollen die Aufgaben EINMAL
                // automatisch neu bewertet werden, damit die farbige Prio-Zahl
                // auch bei alten Eintraegen die richtige Farbe trifft. Der Banner
                // zeigt Fortschritt (X von Y) — kein Spinner-Modal, damit Frank
                // weiter mit der App arbeiten kann waehrend es laeuft.
                state.rescoreProgress?.let { rp ->
                    RescoreBanner(rp)
                    Spacer(Modifier.height(8.dp))
                }

                state.processingMessage?.let {
                    GlassCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth()) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cosmos.textPrimary,
                        )
                    }
                }

                // PERFORMANCE 2026-05-09: derivedStateOf cached den all-empty Check —
                // wird sonst bei jedem State-Update neu berechnet.
                val isEmpty by remember(state.entriesByBucket, state.resolvedEntries) {
                    derivedStateOf {
                        state.entriesByBucket.values.all { it.isEmpty() } && state.resolvedEntries.isEmpty()
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "briefing", contentType = "briefing") {
                        de.frank.entropyreducer.presentation.briefing.BriefingPanel()
                    }
                    state.kiQuestion?.let { q ->
                        item(key = "ki-question", contentType = "ki-question") {
                            de.frank.entropyreducer.presentation.components.KiQuestionCard(
                                question = q,
                                onSubmitAnswer = { answer -> vm.submitKiQuestionAnswer(answer) },
                                onSnooze = vm::snoozeKiQuestion,
                                onRefresh = vm::refreshKiQuestion,
                            )
                        }
                    }
                    item(key = "category-filter", contentType = "category-filter") {
                        CategoryFilterRow(
                            active = state.activeCategories,
                            onToggle = vm::toggleCategory,
                            onClearAll = vm::clearCategoryFilter,
                        )
                    }

                    if (isEmpty) {
                        item(key = "empty", contentType = "empty") { EmptyState() }
                    } else {
                        // Aktive Eintraege gruppiert nach Time-Bucket. Frank-Wunsch
                        // 2026-05-09: HEUTE-Limit wird im ViewModel via
                        // autoBalanceBuckets() durchgesetzt — die DB enthaelt also
                        // schon nur max 5 in HEUTE. Restliche Eintraege wurden auf
                        // MORGEN/FREIBLOCK/SPAETER verteilt. Wir zeigen alle Buckets
                        // sortiert nach priorityScore desc damit Frank ALLE Aufgaben
                        // sieht.
                        // PERFORMANCE 2026-05-09: Sortierung+Filter laufen jetzt im
                        // ViewModel (TasksViewModel.kt), nicht mehr hier — die Lists
                        // sind beim Eintreffen schon sortiert und gefiltert.
                        TimeBucket.values().forEach { bucket ->
                            val list = state.entriesByBucket[bucket].orEmpty()
                            if (list.isNotEmpty()) {
                                item(
                                    key = "header-${bucket.name}",
                                    contentType = "bucket-header",
                                ) { BucketHeader(bucket, list.size, list.sumOf { it.severity }) }
                                items(
                                    items = list,
                                    key = { it.id },
                                    contentType = { "entry" },
                                ) { entry ->
                                    // PERFORMANCE 2026-05-09: Lambdas mit remember(entry.id)
                                    // stabilisieren, damit EntropyEntryCard skippable bleibt
                                    // (zusammen mit @Immutable auf der Entity). Ohne diese
                                    // Stabilisierung erzeugt jede Recomposition neue Lambda-
                                    // Instanzen → alle sichtbaren Karten recomposen → Jank.
                                    val onClick = remember(entry.id) {
                                        { vm.openEntryDetail(entry.id) }
                                    }
                                    val onResolve = remember(entry.id, entry.title) {
                                        {
                                            vm.markEntryResolved(entry.id)
                                            scope.launch {
                                                val result = snackbar.showSnackbar(
                                                    message = "Eintrag erledigt: ${entry.title}",
                                                    actionLabel = "Rückgängig",
                                                    duration = androidx.compose.material3.SnackbarDuration.Short,
                                                )
                                                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                                    vm.reopenEntry(entry.id)
                                                }
                                            }
                                            Unit
                                        }
                                    }
                                    val onPickBucket = remember(entry.id) {
                                        { bucketPickerEntryId = entry.id }
                                    }
                                    EntropyEntryCard(
                                        entry = entry,
                                        onClick = onClick,
                                        onResolve = onResolve,
                                        onPickBucket = onPickBucket,
                                    )
                                }
                            }
                        }
                        // Erledigt-Sektion am Ende
                        if (state.resolvedEntries.isNotEmpty()) {
                            item(key = "resolved-header", contentType = "resolved-header") {
                                ResolvedHeader(state.resolvedEntries.size)
                            }
                            items(
                                items = state.resolvedEntries,
                                key = { "resolved-${it.id}" },
                                contentType = { "entry" },
                            ) { entry ->
                                val onClick = remember(entry.id) {
                                    { vm.openEntryDetail(entry.id) }
                                }
                                val onResolve = remember(entry.id) {
                                    { vm.reopenEntry(entry.id) }
                                }
                                EntropyEntryCard(
                                    entry = entry,
                                    onClick = onClick,
                                    onResolve = onResolve,
                                )
                            }
                        }
                    }
                    item(key = "bottom-spacer", contentType = "spacer") {
                        Spacer(Modifier.height(120.dp))  // Platz für Bottom-Nav
                    }
                }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            ) { Snackbar(it) }
        }
    }

    // Detail-Bottom-Sheet — wird durch Tap auf eine Eintrag-Card geoeffnet.
    state.detailEntry?.let { entry ->
        EntryDetailSheet(
            entry = entry,
            onClose = { vm.closeEntryDetail() },
            onSetStatus = { st -> vm.setEntryStatus(entry.id, st) },
            onDelete = { vm.deleteEntry(entry.id) },
            onAddFollowup = { transcript ->
                vm.addFollowupAndReprocess(entry.id, transcript)
            },
        )
    }

    // Proaktiver Forscher: nach dem Erledigen eines Eintrags fragt die App
    // direkt wie der Eintrag geloest wurde. Das Insight-Board lernt daraus
    // (Frank-Wunsch 2026-05-08).
    state.pendingMethodFor?.let { entry ->
        MethodPromptDialog(
            entry = entry,
            onSubmit = { notes -> vm.submitMethod(notes) },
            onDismiss = { vm.dismissMethodPrompt() },
        )
    }

    // Bucket-Picker-Sheet (Frank-Wunsch 2026-05-09): aktiv wenn der Pill-Button
    // unten rechts in einer Card geklickt wurde. Eintrag wird live aus dem State
    // gezogen, damit Aenderungen ohne Sheet-Neuoeffnen sichtbar sind.
    bucketPickerEntryId?.let { id ->
        val allActive: List<EntropyEntryEntity> =
            state.entriesByBucket.values.flatten() + state.resolvedEntries
        val entry = allActive.firstOrNull { it.id == id }
        if (entry != null) {
            BucketPickerSheet(
                entry = entry,
                onPick = { bucket -> vm.setManualBucket(id, bucket) },
                onClearManual = { vm.clearManualBucket(id) },
                onClose = { bucketPickerEntryId = null },
            )
        } else {
            // Eintrag nicht mehr da — Sheet einfach schliessen.
            bucketPickerEntryId = null
        }
    }
}

@Composable
private fun MethodPromptDialog(
    entry: EntropyEntryEntity,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val notesState = remember(entry.id) { androidx.compose.runtime.mutableStateOf("") }
    val notes = notesState.value
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sparkle-Icon-Kreis links — visueller Anker fuer "Forscher fragt"
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(CosmosColors.AccentSecondary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚛", color = CosmosColors.AccentSecondary, fontSize = 22.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Wie hast du das gelöst?",
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "\"${entry.title}\"",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "Der Forscher merkt sich deine Methode und legt sie als " +
                        "bestätigte Vorgehensweise im Insight-Board ab.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                Box {
                    androidx.compose.material3.OutlinedTextField(
                        value = notes,
                        onValueChange = { notesState.value = it },
                        placeholder = { Text("z.B. Früh schlafen + 20 min Spaziergang", color = cosmos.textSecondary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = cosmos.textPrimary,
                            unfocusedTextColor = cosmos.textPrimary,
                            focusedBorderColor = CosmosColors.AccentPrimary,
                            unfocusedBorderColor = cosmos.glassBorder,
                        ),
                    )
                    // Mic-Button rechts unten — Whisper Large V3 Turbo (Frank-Wunsch
                    // 2026-05-08, ersetzt System-SpeechRecognizer).
                    de.frank.entropyreducer.presentation.components.WhisperMicButton(
                        onTranscript = { transcript ->
                            notesState.value = if (notesState.value.isBlank()) transcript
                            else "${notesState.value} $transcript"
                        },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onSubmit(notes); notesState.value = "" },
                enabled = notes.isNotBlank(),
            ) {
                Text("Speichern", color = CosmosColors.AccentPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Überspringen", color = cosmos.textSecondary)
            }
        },
    )
}

/**
 * Detail-Bottom-Sheet (Bild 12/22). Zeigt Eintrag im Detail mit Icon-Kreis,
 * Title, Beschreibung, Schweregrad-Hinweis, 4 Status-Buttons (Offen / In Arbeit /
 * Reduziert / Archiviert), Tags, KI-Begruendung + KI-Notizen, sowie ein
 * "Löschen"-Button. Aus dem Sheet kann der Status direkt umgestellt werden.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun EntryDetailSheet(
    entry: EntropyEntryEntity,
    onClose: () -> Unit,
    onSetStatus: (EntryStatus) -> Unit,
    onDelete: () -> Unit,
    onAddFollowup: (String) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // windowInsets = WindowInsets(0): Sheet uebernimmt die Insets selbst nicht — die
    // Column unten kompensiert mit eigenem Bottom-Padding. So bleibt der ganze Sheet
    // bis zum unteren Bildschirmrand sichtbar (kein doppeltes Inset-Padding).
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = bottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: Title + X
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Eintrag im Detail",
                    style = MaterialTheme.typography.titleLarge,
                    color = cosmos.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.Close,
                        contentDescription = "Schließen",
                        tint = cosmos.textSecondary,
                    )
                }
            }
            // Hero-Card mit Icon-Kreis + Title + Score
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        CategoryIconCircle(category = entry.category, tint = entry.category.color())
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            EntropyCategoryPill(entry.category)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = cosmos.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cosmos.textSecondary,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${entry.priorityScore.toInt()}",
                                color = priorityColor(entry.priorityScore),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text("Prio", color = cosmos.textSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Schweregrad: ${severityLabel(entry.severity)} (${entry.severity}/10)",
                        style = MaterialTheme.typography.labelMedium,
                        color = cosmos.textSecondary,
                    )
                    Spacer(Modifier.height(6.dp))
                    SeverityRainbowBar(severity = entry.severity)
                }
            }
            // Status-Section: 4 Buttons
            Text(
                "Status",
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusButton("Offen", EntryStatus.OFFEN, entry.status, onSetStatus, modifier = Modifier.weight(1f))
                StatusButton("In Arbeit", EntryStatus.IN_ARBEIT, entry.status, onSetStatus, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusButton("Reduziert", EntryStatus.REDUZIERT, entry.status, onSetStatus, modifier = Modifier.weight(1f))
                StatusButton("Archiviert", EntryStatus.ARCHIVIERT, entry.status, onSetStatus, modifier = Modifier.weight(1f))
            }
            // Nachtrag-per-Sprache (Frank-Wunsch 2026-05-08): Mic-Button startet
            // System-SpeechRecognizer, Transkript wird an die Beschreibung
            // angehaengt und der Eintrag durch ProcessEntryUseCase neu bewertet
            // (Prio + Bucket + Dauer landen automatisch an der richtigen Stelle).
            FollowupMicButton(onTranscript = onAddFollowup)
            // Tags
            if (entry.tags.isNotEmpty()) {
                Text("Tags", style = MaterialTheme.typography.titleSmall, color = cosmos.textPrimary, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(cosmos.glassBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            // KI-Begruendung
            if (entry.priorityReason.isNotBlank() || !entry.aiNotes.isNullOrBlank()) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "KI-Begruendung",
                            style = MaterialTheme.typography.titleSmall,
                            color = CosmosColors.AccentSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.priorityReason.ifBlank { "(keine Begruendung)" },
                            style = MaterialTheme.typography.bodySmall,
                            color = cosmos.textPrimary,
                        )
                        if (!entry.aiNotes.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "KI-Notizen",
                                style = MaterialTheme.typography.titleSmall,
                                color = CosmosColors.AccentSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = entry.aiNotes!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = cosmos.textPrimary,
                            )
                        }
                    }
                }
            }
            // Löschen-Button — gefuellt rot, klar sichtbar, fixe Hoehe damit Text
            // immer lesbar ist; horizontales Default-Padding entfernt damit Icon+Text
            // bei jeder Sheet-Breite mittig sitzen.
            androidx.compose.material3.Button(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = CosmosColors.Critical,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Löschen",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FollowupMicButton(onTranscript: (String) -> Unit) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmosColors.AccentSecondary.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Whisper Large V3 Turbo statt System-SpeechRecognizer (Frank-Wunsch 2026-05-08).
        de.frank.entropyreducer.presentation.components.WhisperMicButton(
            onTranscript = onTranscript,
            size = 40.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Nachtrag einsprechen",
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Whisper Large V3 Turbo — KI bewertet die Aufgabe mit Nachtrag neu.",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun StatusButton(
    label: String,
    status: EntryStatus,
    current: EntryStatus,
    onClick: (EntryStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    val selected = status == current
    val accent = if (selected) CosmosColors.AccentPrimary else cosmos.textSecondary
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) CosmosColors.AccentPrimary.copy(alpha = 0.18f) else cosmos.glassBg)
            .clickable { onClick(status) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun severityLabel(severity: Int): String = when {
    severity <= 3 -> "Niedrig"
    severity <= 6 -> "Mittel"
    severity <= 8 -> "Hoch"
    else -> "Sehr hoch"
}

/**
 * Mappt einen priorityScore (0.0-100.0) auf eine Farbe fuer die grosse Prio-Zahl
 * auf der Aufgabenkarte. Skala (Frank-Wunsch 2026-05-10):
 *   80-100 -> Rot       (sehr wichtig)
 *   60-80  -> Orange
 *   40-60  -> Gelb
 *   20-40  -> Gruen
 *    0-20  -> Blau      (geringste Prio — kuehlste Farbe)
 * Achtung: Bewusst andersherum als die Severity-Bar (dort ist Rot schlecht).
 */
private fun priorityColor(score: Double): Color = when {
    score >= 80.0 -> CosmosColors.PriorityRed
    score >= 60.0 -> CosmosColors.PriorityOrange
    score >= 40.0 -> CosmosColors.PriorityYellow
    score >= 20.0 -> CosmosColors.PriorityGreen
    else -> CosmosColors.PriorityBlue
}

@Composable
private fun CategoryFilterRow(
    active: Set<EntropyCategory>,
    onToggle: (EntropyCategory) -> Unit,
    onClearAll: () -> Unit,
) {
    // values() allociert ein neues Array bei jedem Aufruf — einmal cachen.
    val categories = remember { EntropyCategory.values().toList() }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            val isAll = active.isEmpty()
            CategoryFilterChip(
                label = "Alle",
                icon = Icons.Outlined.GridView,
                tint = CosmosColors.AccentPrimary,
                selected = isAll,
                onClick = onClearAll,
            )
        }
        items(
            items = categories,
            key = { it.name },
        ) { cat ->
            val on = cat in active
            CategoryFilterChip(
                label = cat.label(),
                icon = iconForCategory(cat),
                tint = cat.color(),
                selected = on,
                onClick = { onToggle(cat) },
            )
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    // PERFORMANCE 2026-05-09: clip() entfernt — background mit Shape uebernimmt das.
    val pillShape = remember { RoundedCornerShape(50) }
    val bg = if (selected) tint.copy(alpha = 0.20f) else cosmos.glassBg
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bg, pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) tint else cosmos.textSecondary,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) tint else cosmos.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun iconForCategory(category: EntropyCategory): androidx.compose.ui.graphics.vector.ImageVector =
    when (category) {
        EntropyCategory.KOERPERLICH -> Icons.Outlined.Bolt
        EntropyCategory.MENTAL -> Icons.Outlined.Psychology
        EntropyCategory.ZEITLICH -> Icons.Outlined.AccessTime
        EntropyCategory.EMOTIONAL -> Icons.Outlined.FavoriteBorder
        EntropyCategory.GESUNDHEITLICH -> Icons.Outlined.MedicalServices
        EntropyCategory.UMGEBUNG -> Icons.Outlined.Home
        EntropyCategory.SONSTIGES -> Icons.Outlined.MoreHoriz
    }

@Composable
private fun BucketHeader(bucket: TimeBucket, count: Int, sumSeverity: Int) {
    val cosmos = LocalCosmos.current
    val label = when (bucket) {
        TimeBucket.HEUTE -> "HEUTE"
        TimeBucket.MORGEN -> "MORGEN"
        TimeBucket.FREIBLOCK -> "FREIBLOCK"
        TimeBucket.SPAETER -> "SPÄTER"
    }
    val accent = bucketAccent(bucket)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon-Pille mit Calendar-Icon (Soll-Design)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = bucketIcon(bucket),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        // Count-Pill rechts
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ResolvedHeader(count: Int) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CosmosColors.Success.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = CosmosColors.Success,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "ERLEDIGT",
            style = MaterialTheme.typography.labelLarge,
            color = cosmos.textPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(CosmosColors.Success.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelMedium,
                color = CosmosColors.Success,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun bucketIcon(bucket: TimeBucket): androidx.compose.ui.graphics.vector.ImageVector = when (bucket) {
    TimeBucket.HEUTE -> Icons.Outlined.Today
    TimeBucket.MORGEN -> Icons.Outlined.Event
    TimeBucket.FREIBLOCK -> Icons.Outlined.DateRange
    TimeBucket.SPAETER -> Icons.Outlined.HourglassEmpty
}

@Composable
private fun bucketAccent(bucket: TimeBucket): Color = when (bucket) {
    TimeBucket.HEUTE -> CosmosColors.AccentPrimary
    TimeBucket.MORGEN -> CosmosColors.AccentSecondary
    TimeBucket.FREIBLOCK -> CosmosColors.CatHealth
    TimeBucket.SPAETER -> LocalCosmos.current.textSecondary
}

/**
 * Liefert die ganz leichte Hintergrund-Toenung der Aufgabenkarte je nach Bucket
 * (Frank-Wunsch 2026-05-10, zweite Iteration). GlassCard rendert die Farbe als
 * Linear-Gradient von oben-links (transparent) nach unten-rechts (voller Tint).
 * Frank wollte das Orange/Gelb/Gruen/Blau dezenter — daher hier zusaetzlich der
 * Endwert-Alpha um ~25% reduziert (light: 0.18→0.14, dark: 0.12→0.10).
 * Zusammen mit dem Verlauf wirkt das Orange jetzt sehr zurueckhaltend und nur
 * in der unteren rechten Card-Ecke schwach erkennbar.
 *  - HEUTE      = Orange-Stich
 *  - MORGEN     = Gelb-Stich
 *  - FREIBLOCK  = Gruen-Stich
 *  - SPAETER    = Blau-Stich
 */
private fun bucketCardTint(bucket: TimeBucket, isDark: Boolean): Color {
    val base = when (bucket) {
        TimeBucket.HEUTE -> CosmosColors.BucketHeuteTint
        TimeBucket.MORGEN -> CosmosColors.BucketMorgenTint
        TimeBucket.FREIBLOCK -> CosmosColors.BucketFreiblockTint
        TimeBucket.SPAETER -> CosmosColors.BucketSpaeterTint
    }
    return base.copy(alpha = if (isDark) 0.10f else 0.14f)
}

@Composable
private fun EntropyEntryCard(
    entry: EntropyEntryEntity,
    onClick: () -> Unit = {},
    onResolve: () -> Unit = {},
    onSeverityHint: () -> Unit = {},
    onPickBucket: () -> Unit = {},
) {
    val cosmos = LocalCosmos.current
    val catColor = entry.category.color()
    val isResolved = entry.status == EntryStatus.REDUZIERT || entry.status == EntryStatus.ARCHIVIERT
    // PERFORMANCE 2026-05-09: graphicsLayer statt Modifier.alpha — bei alpha = 1f
    // wird der Layer komplett uebersprungen (compositingStrategy = ModulateAlpha
    // sorgt dafuer dass nur die Pixel-Alpha moduliert wird, ohne separate
    // Off-Screen-Buffer-Allocation). Vorher: Modifier.alpha erzeugte auch bei
    // alpha = 1f gelegentlich einen Layer.
    val cardAlpha = if (isResolved) 0.55f else 1f
    // Frank-Wunsch 2026-05-10: Karten-Hintergrund je nach Bucket dezent toenen,
    // damit Frank auf einen Blick sieht in welchem Bucket eine Aufgabe steckt.
    // Wird auch bei verschobenen Karten reaktiv neu berechnet, weil entry.timeBucket
    // bei jedem Bucket-Wechsel im DB-State frisch geliefert wird.
    val bucketTint = bucketCardTint(entry.timeBucket, cosmos.isDark)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .graphicsLayer {
                alpha = cardAlpha
                compositingStrategy = CompositingStrategy.ModulateAlpha
            },
        tintColor = bucketTint,
    ) {
        Column {
            // Top-Row: Icon-Kreis links | Title+Beschreibung | Score+"Prio"+Haken
            Row(verticalAlignment = Alignment.Top) {
                CategoryIconCircle(category = entry.category, tint = catColor)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EntropyCategoryPill(entry.category)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = cosmos.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = cosmos.textSecondary,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${entry.priorityScore.toInt()}",
                        color = priorityColor(entry.priorityScore),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Prio",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    // Checkbox-Stil: leeres Quadrat wenn offen, ausgefuelltes Haekchen wenn erledigt.
                    // Tap toggelt — offen → erledigt (REDUZIERT), erledigt → wieder OFFEN.
                    val checkBg = if (isResolved) CosmosColors.Success.copy(alpha = 0.85f) else androidx.compose.ui.graphics.Color.Transparent
                    val checkBorder = if (isResolved) CosmosColors.Success else cosmos.glassBorder
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(checkBg)
                            .border(
                                androidx.compose.foundation.BorderStroke(2.dp, checkBorder),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(onClick = onResolve),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isResolved) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Erledigt",
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Severity-Bar im Regenbogen-Stil (Soll-Design): 5 Segmente in Status-Farben,
            // gefuellt nach severity (1-10 -> 0..1)
            SeverityRainbowBar(severity = entry.severity)

            // Tag-Pills
            // PERFORMANCE 2026-05-09: tagsToShow + pillShape mit remember cachen.
            // entry.tags.take(3) allociert sonst pro Recompose eine neue List.
            // clip() entfernt — background(color, shape) zeichnet die Pille direkt
            // ohne separaten GraphicsLayer, kein Overflow weil Tag-Strings kurz sind.
            val tagsToShow = remember(entry.id, entry.tags) { entry.tags.take(3) }
            if (tagsToShow.isNotEmpty()) {
                val pillShape = remember { RoundedCornerShape(50) }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tagsToShow.forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = cosmos.textSecondary,
                            modifier = Modifier
                                .background(cosmos.glassBg, pillShape)
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Meta-Row unten: Bucket-Hinweis | Empfohlen-Badge | Wearable-Badge | Picker-Button
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EntryMetaRow(entry = entry, modifier = Modifier.weight(1f))
                BucketPickerButton(
                    isManual = entry.manualBucket != null,
                    bucket = entry.timeBucket,
                    onClick = onPickBucket,
                )
            }
        }
    }
}

/**
 * Kleiner Button unten rechts in der Card der das Bucket-Auswahl-Sheet oeffnet
 * (Frank-Wunsch 2026-05-09). Zeigt das Bucket-Icon mit aktiver Farbe wenn
 * Frank den Bucket manuell zugewiesen hat (manualBucket != null), sonst nur
 * dezenter Outline-Style — die KI hat entschieden.
 */
@Composable
private fun BucketPickerButton(
    isManual: Boolean,
    bucket: TimeBucket,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = bucketAccent(bucket)
    val bg = if (isManual) accent.copy(alpha = 0.22f) else cosmos.glassBg
    val tint = if (isManual) accent else cosmos.textSecondary
    // PERFORMANCE 2026-05-09: clip() entfernt — background(color, shape) clippt
    // visuell (zeichnet abgerundete Form), Inhalte sind kurz und passen rein.
    val pillShape = remember { RoundedCornerShape(50) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bg, pillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(
            imageVector = bucketIcon(bucket),
            contentDescription = "Bucket ändern",
            tint = tint,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = if (isManual) "manuell" else "KI",
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = if (isManual) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Bottom-Sheet zur manuellen Bucket-Zuordnung (Frank-Wunsch 2026-05-09). Zeigt
 * vier Bucket-Optionen + "KI bestimmt" als Reset. Aktive Auswahl wird in der
 * Bucket-Akzent-Farbe hervorgehoben.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun BucketPickerSheet(
    entry: EntropyEntryEntity,
    onPick: (TimeBucket) -> Unit,
    onClearManual: () -> Unit,
    onClose: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = bottomInset + 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Wann erledigen?",
                style = MaterialTheme.typography.titleLarge,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "\"${entry.title}\"",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            // Vier Bucket-Optionen
            TimeBucket.values().forEach { bucket ->
                val isActive = entry.manualBucket == bucket ||
                    (entry.manualBucket == null && entry.timeBucket == bucket)
                BucketOptionRow(
                    bucket = bucket,
                    label = bucketLabelLong(bucket),
                    description = bucketDescription(bucket),
                    isActive = isActive,
                    isManual = entry.manualBucket == bucket,
                    onClick = { onPick(bucket); onClose() },
                )
            }
            // Reset auf KI
            if (entry.manualBucket != null) {
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = { onClearManual(); onClose() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = CosmosColors.AccentSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "KI entscheiden lassen",
                        color = CosmosColors.AccentSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BucketOptionRow(
    bucket: TimeBucket,
    label: String,
    description: String,
    isActive: Boolean,
    isManual: Boolean,
    onClick: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val accent = bucketAccent(bucket)
    val bg = if (isActive) accent.copy(alpha = 0.18f) else cosmos.glassBg
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = bucketIcon(bucket),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = cosmos.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = cosmos.textSecondary,
            )
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "aktiv",
                tint = accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun bucketLabelLong(bucket: TimeBucket): String = when (bucket) {
    TimeBucket.HEUTE -> "Heute"
    TimeBucket.MORGEN -> "Morgen"
    TimeBucket.FREIBLOCK -> "Freiblock"
    TimeBucket.SPAETER -> "Später"
}

private fun bucketDescription(bucket: TimeBucket): String = when (bucket) {
    TimeBucket.HEUTE -> "max. 5 Eintraege — schwächste Aufgabe rückt nach Morgen"
    TimeBucket.MORGEN -> "rückt morgen automatisch in Heute"
    TimeBucket.FREIBLOCK -> "nächster freier Schichtblock"
    TimeBucket.SPAETER -> "kein Datum — Sammelbecken"
}

/** Farbiger Kreis mit Material-Icon basierend auf der Entropie-Kategorie. */
@Composable
private fun CategoryIconCircle(
    category: de.frank.entropyreducer.domain.model.EntropyCategory,
    tint: androidx.compose.ui.graphics.Color,
) {
    val icon = when (category) {
        de.frank.entropyreducer.domain.model.EntropyCategory.KOERPERLICH -> Icons.Outlined.Bolt
        de.frank.entropyreducer.domain.model.EntropyCategory.MENTAL -> Icons.Outlined.Psychology
        de.frank.entropyreducer.domain.model.EntropyCategory.ZEITLICH -> Icons.Outlined.AccessTime
        de.frank.entropyreducer.domain.model.EntropyCategory.EMOTIONAL -> Icons.Outlined.FavoriteBorder
        de.frank.entropyreducer.domain.model.EntropyCategory.GESUNDHEITLICH -> Icons.Outlined.MedicalServices
        de.frank.entropyreducer.domain.model.EntropyCategory.UMGEBUNG -> Icons.Outlined.Home
        de.frank.entropyreducer.domain.model.EntropyCategory.SONSTIGES -> Icons.Outlined.MoreHoriz
    }
    // PERFORMANCE 2026-05-09: clip() entfernt — background(color, CircleShape)
    // zeichnet den Kreis direkt, das Icon ist innerhalb der size(44.dp) und
    // hat selbst nur 22.dp, kein Overflow moeglich.
    val circleShape = remember { RoundedCornerShape(50) }
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(tint.copy(alpha = 0.15f), circleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * 5-Segment-Severity-Bar im Regenbogen-Stil (Soll-Design). Die Segmente sind
 * gleich groß, der "ausgefuellte" Anteil ergibt sich aus severity/10. Nicht
 * gefuellte Segmente sind ausgegraut, gefuellte zeigen ihre Status-Farbe.
 */
@Composable
private fun SeverityRainbowBar(severity: Int) {
    val cosmos = LocalCosmos.current
    val sev = severity.coerceIn(1, 10)
    // PERFORMANCE 2026-05-09: Komplette Neuimplementierung mit Canvas — vorher
    // 5 einzelne Box-Composables mit clip+background, was 5 zusaetzliche
    // GraphicsLayer pro Karte erzeugte. Bei 10 sichtbaren Karten = 50 Layer
    // nur fuer die Severity-Skala. Canvas zeichnet alle 5 Segmente in EINEM
    // Layer mit drawRoundRect — kein clip, kein Layer-Compositing.
    val palette = remember {
        listOf(
            CosmosColors.StatusGreen,
            CosmosColors.StatusLightGreen,
            CosmosColors.StatusYellow,
            CosmosColors.StatusOrange,
            CosmosColors.StatusRed,
        )
    }
    val emptyColor = cosmos.glassBg
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp),
    ) {
        val segmentCount = 5
        val gap = 3.dp.toPx()
        val totalGap = gap * (segmentCount - 1)
        val segmentWidth = (size.width - totalGap) / segmentCount
        val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        for (i in 0 until segmentCount) {
            val filled = sev >= (i + 1) * 2
            val x = i * (segmentWidth + gap)
            drawRoundRect(
                color = if (filled) palette[i] else emptyColor,
                topLeft = Offset(x, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = cornerRadius,
            )
        }
    }
}

@Composable
private fun EntryMetaRow(entry: EntropyEntryEntity, modifier: Modifier = Modifier) {
    val cosmos = LocalCosmos.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Bucket-Time-Label (TimeBucket)
        val bucketLabel = when (entry.timeBucket) {
            de.frank.entropyreducer.domain.model.TimeBucket.HEUTE -> "heute"
            de.frank.entropyreducer.domain.model.TimeBucket.MORGEN -> "morgen"
            de.frank.entropyreducer.domain.model.TimeBucket.FREIBLOCK -> "Freiblock"
            de.frank.entropyreducer.domain.model.TimeBucket.SPAETER -> "später"
        }
        val durationHint = entry.estimatedDurationMinutes?.let {
            when {
                it < 60 -> "$it min"
                it < 24 * 60 -> "${it / 60} h"
                else -> "${it / (24 * 60)} d"
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = cosmos.textSecondary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = if (durationHint != null) "$bucketLabel, $durationHint" else bucketLabel,
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        // Empfohlen-Badge (immer wenn priorityScore > 70)
        // PERFORMANCE 2026-05-09: clip() entfernt — background mit Shape clippt visuell.
        if (entry.priorityScore > 70) {
            val pillShape = remember { RoundedCornerShape(50) }
            Text(
                text = "Empfohlen",
                color = CosmosColors.Success,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(CosmosColors.Success.copy(alpha = 0.15f), pillShape)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        // Wearable-Indikator (wenn ein Biomarker-Snapshot verlinkt ist)
        if (entry.biomarkerSnapshotId != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.MonitorHeart,
                    contentDescription = "Wearable-Bezug",
                    tint = CosmosColors.AccentSecondary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "Wearable",
                    color = CosmosColors.AccentSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val cosmos = LocalCosmos.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = null,
            tint = CosmosColors.AccentPrimary,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Tippe auf das Mikrofon und sprich aus, was deine Energie kostet.",
            style = MaterialTheme.typography.titleMedium,
            color = cosmos.textPrimary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Die KI ordnet es ein, priorisiert es und plant es in deinen Schichtkalender ein.",
            style = MaterialTheme.typography.bodyMedium,
            color = cosmos.textSecondary,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

/**
 * Kleine, dezente Statuszeile direkt unter dem Titel "Entropie Reduktor".
 * Zeigt visuell ob Drive-Backup gerade laeuft, fertig ist oder fehlgeschlagen ist
 * — und seit wann. Frank-Wunsch 2026-05-09: er will nach jedem neuen Eintrag
 * sofort sehen "okay, das ist im Backup".
 */
@Composable
private fun BackupStatusBadge(syncStatus: SyncStatus, lastBackupAtMs: Long) {
    val cosmos = LocalCosmos.current
    val (icon, tint, label) = when (syncStatus) {
        SyncStatus.Idle -> Triple(
            Icons.Outlined.CloudDone,
            CosmosColors.Success,
            if (lastBackupAtMs > 0L) "Backup: ${formatBackupTime(lastBackupAtMs)}" else "Backup eingerichtet",
        )
        SyncStatus.Pending -> Triple(
            Icons.Outlined.CloudSync,
            CosmosColors.AccentSecondary,
            "Aenderung erfasst — Backup startet gleich",
        )
        SyncStatus.Running -> Triple(
            Icons.Outlined.CloudSync,
            CosmosColors.AccentPrimary,
            "Backup laeuft …",
        )
        is SyncStatus.Synced -> Triple(
            Icons.Outlined.CloudDone,
            CosmosColors.Success,
            "Im Backup gesichert: ${formatBackupTime(syncStatus.atEpochMs)}",
        )
        is SyncStatus.Failed -> Triple(
            Icons.Outlined.CloudOff,
            CosmosColors.Critical,
            "Backup fehlgeschlagen",
        )
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = cosmos.textSecondary,
        )
    }
}

/**
 * Banner direkt unter dem Titel "Entropie Reduktor" wenn gerade alle offenen
 * Aufgaben mit der aktualisierten priorityScore-Doktrin neu bewertet werden
 * (Frank-Wunsch 2026-05-09 — neue 5-Farben-Skala basiert auf Entropie-
 * Reduktion). Zeigt Fortschritt "X von Y", einen schmalen Balken und am Ende
 * "Fertig: X von Y neu bewertet" fuer 3 Sekunden bevor der Banner verschwindet.
 */
@Composable
private fun RescoreBanner(progress: RescoreProgress) {
    val cosmos = LocalCosmos.current
    val isFinished = progress.done + progress.failed >= progress.total
    val accent = if (isFinished) CosmosColors.Success else CosmosColors.AccentPrimary
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            val label = when {
                isFinished && progress.failed == 0 ->
                    "Aufgaben mit neuer Skala neu bewertet (${progress.done} von ${progress.total})"
                isFinished && progress.failed > 0 ->
                    "Neu bewertet: ${progress.done} von ${progress.total} — ${progress.failed} fehlgeschlagen"
                else ->
                    "Aufgaben werden mit neuer Skala neu bewertet … ${progress.done} von ${progress.total}"
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = cosmos.textSecondary,
            )
        }
        Spacer(Modifier.height(4.dp))
        if (!isFinished && progress.total > 0) {
            LinearProgressIndicator(
                progress = { (progress.done + progress.failed).toFloat() / progress.total.toFloat() },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = accent,
                trackColor = cosmos.glassBg,
            )
        }
    }
}

/** Formatiert einen Epoch-ms-Zeitstempel in "HH:mm" (heute) oder "dd.MM. HH:mm" (sonst). */
private fun formatBackupTime(epochMs: Long): String {
    val now = System.currentTimeMillis()
    val today0 = Date(now).run {
        // 0:00 Lokalzeit
        java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val pattern = if (epochMs >= today0) "HH:mm" else "dd.MM. HH:mm"
    return SimpleDateFormat(pattern, Locale.GERMAN).format(Date(epochMs))
}
