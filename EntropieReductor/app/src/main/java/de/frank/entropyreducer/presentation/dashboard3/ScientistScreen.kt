package de.frank.entropyreducer.presentation.dashboard3

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.model.ScientistRole
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
import de.frank.entropyreducer.presentation.components.KiQuestionCard
import de.frank.entropyreducer.presentation.components.MarkdownView
import de.frank.entropyreducer.presentation.components.ThemeToggleIcon
import de.frank.entropyreducer.presentation.components.rememberMicPermissionState
import de.frank.entropyreducer.presentation.navigation.CosmosBottomBar
import de.frank.entropyreducer.presentation.theme.CosmosColors
import de.frank.entropyreducer.presentation.theme.LocalCosmos
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

/**
 * Dashboard 3 — Wissenschaftler-Chat (Spec §12).
 */
@Composable
fun ScientistScreen(
    onOpenSettings: () -> Unit,
    onSwitchTab: (String) -> Unit,
    currentTab: String,
    vm: ScientistViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val cosmos = LocalCosmos.current
    val snackbar = remember { SnackbarHostState() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val listState = rememberLazyListState()

    val micPerm = rememberMicPermissionState(
        onAllGranted = { vm.onMicClick() },
        onDenied = { denied ->
            val msg = if (Manifest.permission.RECORD_AUDIO in denied) {
                "Mikrofon-Zugriff wurde abgelehnt. Aktiviere ihn in den System-Einstellungen."
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

    LaunchedEffect(state.currentSessionId) {
        vm.maybeKickoff()
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    val themeVm: ThemeViewModel = hiltViewModel()
    val themeMode by themeVm.themeMode.collectAsState()

    var hypothesisToStart by remember { mutableStateOf<HypothesisEntity?>(null) }

    CosmosScaffold(
        title = "Wissenschaftler",
        actions = {
            SessionPicker(
                sessions = state.sessions,
                currentId = state.currentSessionId,
                onSelect = vm::selectSession,
                onNew = vm::newSession,
                onArchive = vm::archiveCurrent,
            )
            ThemeToggleIcon(current = themeMode, onCycle = themeVm::cycle)
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, "Einstellungen", tint = cosmos.textPrimary)
            }
        },
        bottomBar = {
            // Frank-Wunsch 2026-05-09: Das zentrale Mic in der BottomBar ist
            // kontextabhaengig — wenn der Forscher-Tab aktiv ist, schiebt es
            // Whisper-Transkripte in den globalen Forscher-Chat-Draft. Damit gibt
            // es nur EIN Mic auf dem Screen statt zwei (das ChatInputBar-Mic
            // entfaellt darunter).
            CosmosBottomBar(
                currentTab = currentTab,
                micState = state.micState,
                onTabSelected = onSwitchTab,
                onMicClick = {
                    if (micPerm.check()) vm.onMicClick() else micPerm.request()
                },
            )
        },
    ) { padding ->
        // Frank-Wunsch 2026-05-09: ChatInputBar (Eingabefeld + Send) komplett entfernt.
        // Mic-Aufnahme erfolgt ueber das zentrale BottomBar-Mic. Nach erfolgreicher
        // Transkription oeffnet sich ein TranscriptEditDialog (Pop-up) mit dem Text drin,
        // den Frank editieren oder direkt absenden kann.
        // Der Bildschirm scrollt jetzt vollstaendig durch — kein abgeschnittener Bereich
        // ueber der BottomBar mehr (analog zu TasksScreen).
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.scientistQuestion?.let { question ->
                    item(key = "scientist_question_${question.triggerKey}") {
                        KiQuestionCard(
                            question = question,
                            onSubmitAnswer = vm::submitScientistQuestionAnswer,
                            onSnooze = vm::snoozeScientistQuestion,
                            onRefresh = vm::refreshScientistQuestion,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                // Performance-Audit Loop 2 (2026-05-10): contentType ergaenzt —
                // unterschiedliche Bubble-Typen (KI vs USER) duerfen sonst Compose-
                // Slot-Reuse durcheinanderbringen.
                items(state.messages, key = { it.id }, contentType = { it.role }) { msg ->
                    when (msg.role) {
                        ScientistRole.KI -> KiBubble(
                            message = msg,
                            hypotheses = state.hypothesesByMessageId[msg.id].orEmpty(),
                            onTryHypothesis = { hypothesisToStart = it },
                            onOpenHypothesis = { vm.openHypothesisDetail(it.id) },
                            onDeleteHypothesis = { vm.requestDeleteHypothesis(it.id) },
                        )
                        ScientistRole.NUTZER -> NutzerBubble(msg)
                    }
                }
                if (state.isThinking) {
                    item { ThinkingIndicator() }
                }
                // Bottom-Spacer fuer die BottomBar-Hoehe (72dp + System-Nav-Inset).
                // Damit das letzte Item nicht hinter der BottomBar verschwindet,
                // genauso wie auf dem Aufgaben-Screen.
                item { Spacer(Modifier.height(100.dp)) }
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            ) { Snackbar(it) }
        }
    }

    // TranscriptEditDialog: erscheint nach erfolgreicher Whisper-Transkription
    // mit dem aufgenommenen Text als editierbarem Vorlagewert.
    state.pendingTranscript?.let { text ->
        TranscriptEditDialog(
            text = text,
            onTextChange = vm::editPendingTranscript,
            onSend = vm::sendPendingTranscript,
            onDismiss = vm::dismissPendingTranscript,
        )
    }

    hypothesisToStart?.let { h ->
        StartHypothesisDialog(
            hypothesis = h,
            onDismiss = { hypothesisToStart = null },
            onConfirm = { startMs ->
                vm.startHypothesis(h, startMs)
                hypothesisToStart = null
            },
        )
    }

    // Loesch-Confirm-Dialog
    state.pendingDeleteHypothesisId?.let { _ ->
        AlertDialog(
            onDismissRequest = vm::dismissDeleteConfirmation,
            title = { Text("Hypothese löschen?") },
            text = {
                Text(
                    "Die Hypothese und der gesamte Chat-Verlauf zu ihr werden dauerhaft entfernt. " +
                        "Das laesst sich nicht rueckgaengig machen.",
                    color = cosmos.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = vm::confirmDeleteHypothesis) {
                    Text("Löschen", color = CosmosColors.Critical)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDeleteConfirmation) { Text("Abbrechen") }
            },
            containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
        )
    }

    // Inline-Hypothesen-Detail-Sheet
    state.hypothesisDetail?.let { h ->
        HypothesisDetailSheet(
            hypothesis = h,
            messages = state.hypothesisMessages,
            draft = state.hypothesisDraftText,
            onDraftChange = vm::setHypothesisDraft,
            isThinking = state.hypothesisIsThinking,
            micState = state.hypothesisMicState,
            onMicClick = {
                if (micPerm.check()) vm.onHypothesisMicClick() else micPerm.request()
            },
            onSend = vm::sendInHypothesisChat,
            onDismiss = vm::closeHypothesisDetail,
            onTryHypothesis = { hypothesisToStart = h },
            onDeleteHypothesis = { vm.requestDeleteHypothesis(h.id) },
        )
    }
}

@Composable
private fun KiBubble(
    message: ScientistMessageEntity,
    hypotheses: List<HypothesisEntity>,
    onTryHypothesis: (HypothesisEntity) -> Unit,
    onOpenHypothesis: (HypothesisEntity) -> Unit,
    onDeleteHypothesis: (HypothesisEntity) -> Unit,
) {
    val cosmos = LocalCosmos.current
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(0.85f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CosmosColors.AccentSecondary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⚛", color = CosmosColors.AccentSecondary, fontSize = 12.sp)
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Wissenschaftler",
                    color = CosmosColors.AccentSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatRelative(message.createdAt),
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                    .background(cosmos.glassBg)
                    .border(
                        BorderStroke(1.dp, CosmosColors.AccentPrimary.copy(alpha = 0.30f)),
                        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                    )
                    .padding(12.dp),
            ) {
                Column {
                    if (message.content.isNotBlank()) {
                        MarkdownView(text = message.content)
                    }
                    hypotheses.forEach { h ->
                        Spacer(Modifier.height(8.dp))
                        HypothesisCardInChat(
                            h = h,
                            onTry = { onTryHypothesis(h) },
                            onOpen = { onOpenHypothesis(h) },
                            onDelete = { onDeleteHypothesis(h) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(0.15f))
    }
}

@Composable
private fun NutzerBubble(message: ScientistMessageEntity) {
    val cosmos = LocalCosmos.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Spacer(Modifier.weight(0.15f))
        Box(
            Modifier
                .weight(0.85f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
                .background(CosmosColors.AccentSecondary.copy(alpha = 0.20f))
                .padding(12.dp),
        ) {
            Text(
                text = message.content,
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun HypothesisCardInChat(
    h: HypothesisEntity,
    onTry: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val isProposed = h.status == HypothesisStatus.VORGESCHLAGEN
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmosColors.AccentPrimary.copy(alpha = 0.10f))
            .border(
                BorderStroke(1.dp, CosmosColors.AccentPrimary.copy(alpha = 0.40f)),
                RoundedCornerShape(12.dp),
            )
            // Karte ist klickbar — oeffnet den Inline-Detail-Chat dieser Hypothese.
            // Loesch-Icon hat eigenen Click-Handler, daher kein Konflikt.
            .clickable(onClick = onOpen)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Hypothese",
                    color = CosmosColors.AccentPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Hypothese löschen",
                        tint = cosmos.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = h.title,
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (h.description.isNotBlank()) {
                Text(
                    text = h.description,
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (h.rationale.isNotBlank()) {
                Text(
                    text = "Begruendung: ${h.rationale}",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val days = ((h.plannedEndDate - h.plannedStartDate) / TimeUnit.DAYS.toMillis(1)).toInt()
                .coerceAtLeast(1)
            Text(
                text = "Geplante Dauer: $days Tage",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isProposed) {
                    TextButton(onClick = onTry) {
                        Text("Ausprobieren", color = CosmosColors.AccentPrimary)
                    }
                } else {
                    Text(
                        text = "Status: ${h.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        color = CosmosColors.Success,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onOpen) {
                    Text("Diskutieren", color = CosmosColors.AccentSecondary)
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val cosmos = LocalCosmos.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = CosmosColors.AccentSecondary,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Wissenschaftler denkt nach …",
            color = cosmos.textSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Frank-Wunsch 2026-05-09: Pop-up nach erfolgreicher Whisper-Transkription.
 * Zeigt das aufgenommene Transkript als editierbares Textfeld — Frank kann nochmal
 * korrigieren bevor er auf Senden drueckt. Cancel-Button verwirft die Aufnahme.
 */
@Composable
private fun TranscriptEditDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Transkript pruefen",
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                Text(
                    text = "Du kannst den Text noch anpassen, bevor er an den Wissenschaftler geht.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cosmos.textPrimary,
                        unfocusedTextColor = cosmos.textPrimary,
                        focusedBorderColor = CosmosColors.AccentPrimary,
                        unfocusedBorderColor = cosmos.glassBorder,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 8,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSend,
                enabled = text.isNotBlank(),
            ) {
                Text("Senden", color = CosmosColors.AccentPrimary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = cosmos.textSecondary)
            }
        },
        containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
    )
}

@Composable
private fun SessionPicker(
    sessions: List<de.frank.entropyreducer.data.local.entities.ScientistSessionEntity>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onNew: () -> Unit,
    onArchive: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    var expanded by remember { mutableStateOf(false) }
    val current = sessions.firstOrNull { it.id == currentId }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(
                text = current?.title?.take(18) ?: "Session",
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
            Icon(Icons.Outlined.ArrowDropDown, null, tint = cosmos.textPrimary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sessions.forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.title.ifBlank { "Session ${s.id.take(6)}" }) },
                    onClick = {
                        onSelect(s.id)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Neue Session") },
                leadingIcon = { Icon(Icons.Outlined.Add, null) },
                onClick = {
                    onNew()
                    expanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Aktuelle archivieren") },
                leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                onClick = {
                    onArchive()
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun StartHypothesisDialog(
    hypothesis: HypothesisEntity,
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long) -> Unit,
) {
    val cosmos = LocalCosmos.current
    val now = System.currentTimeMillis()
    val tomorrow = now + TimeUnit.DAYS.toMillis(1)
    val nextFreeBlock = now + TimeUnit.DAYS.toMillis(3) // einfache Heuristik

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Wann soll das Experiment starten?") },
        text = {
            Column {
                Text(
                    text = hypothesis.title,
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Wähle einen Startzeitpunkt — du kannst ihn später im Experiment-Kalender korrigieren.",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Column {
                TextButton(onClick = { onConfirm(now) }) { Text("Heute") }
                TextButton(onClick = { onConfirm(tomorrow) }) { Text("Morgen") }
                TextButton(onClick = { onConfirm(nextFreeBlock) }) { Text("Nächster Frei-Block") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
        containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HypothesisDetailSheet(
    hypothesis: HypothesisEntity,
    messages: List<HypothesisMessageEntity>,
    draft: String,
    onDraftChange: (String) -> Unit,
    isThinking: Boolean,
    micState: de.frank.entropyreducer.presentation.components.MicState,
    onMicClick: () -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit,
    onTryHypothesis: () -> Unit,
    onDeleteHypothesis: () -> Unit,
) {
    val cosmos = LocalCosmos.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (cosmos.isDark) CosmosColors.BgDarkAccent else CosmosColors.BgLightAccent,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Kopf — Hypothese im Detail
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Hypothese im Detail",
                        color = CosmosColors.AccentPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = hypothesis.title,
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(onClick = onDeleteHypothesis) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Hypothese löschen",
                        tint = CosmosColors.Critical,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Beschreibung + Begruendung + Status (kompakt, scrollbar wenn lang)
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .background(cosmos.glassBg, RoundedCornerShape(12.dp))
                    .padding(12.dp),
            ) {
                if (hypothesis.description.isNotBlank()) {
                    Text(
                        text = hypothesis.description,
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (hypothesis.rationale.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Begruendung: ${hypothesis.rationale}",
                        color = cosmos.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val days = ((hypothesis.plannedEndDate - hypothesis.plannedStartDate) / TimeUnit.DAYS.toMillis(1))
                    .toInt().coerceAtLeast(1)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Geplante Dauer: $days Tage  ·  Status: ${hypothesis.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    color = cosmos.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (hypothesis.status == HypothesisStatus.VORGESCHLAGEN) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onTryHypothesis) {
                        Text("Ausprobieren", color = CosmosColors.AccentPrimary)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Diskussion",
                color = cosmos.textSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(4.dp))

            // Verlauf der Diskussion zu dieser Hypothese
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    when (msg.role) {
                        ScientistRole.KI -> HypothesisKiBubble(msg)
                        ScientistRole.NUTZER -> HypothesisNutzerBubble(msg)
                    }
                }
                if (isThinking) {
                    item { ThinkingIndicator() }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Eingabe-Zeile (Mic + Text + Send) speziell fuer diese Hypothese
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Diskutiere diese Hypothese …", color = cosmos.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = cosmos.textPrimary,
                        unfocusedTextColor = cosmos.textPrimary,
                        focusedBorderColor = CosmosColors.AccentPrimary,
                        unfocusedBorderColor = cosmos.glassBorder,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4,
                )
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (micState == de.frank.entropyreducer.presentation.components.MicState.RECORDING)
                                CosmosColors.Critical.copy(alpha = 0.30f)
                            else CosmosColors.AccentSecondary.copy(alpha = 0.20f),
                        ),
                ) {
                    Icon(
                        imageVector = if (micState == de.frank.entropyreducer.presentation.components.MicState.RECORDING) {
                            Icons.Outlined.Stop
                        } else {
                            Icons.Outlined.Mic
                        },
                        contentDescription = "Mikrofon (Hypothese)",
                        tint = if (micState == de.frank.entropyreducer.presentation.components.MicState.RECORDING) {
                            CosmosColors.Critical
                        } else {
                            CosmosColors.AccentSecondary
                        },
                    )
                }
                Spacer(Modifier.width(6.dp))
                val canSend = draft.isNotBlank() && !isThinking
                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            if (canSend) CosmosColors.AccentPrimary
                            else CosmosColors.AccentPrimary.copy(alpha = 0.30f),
                        ),
                ) {
                    Icon(Icons.Outlined.Send, "Senden", tint = CosmosColors.BgDark)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HypothesisKiBubble(message: HypothesisMessageEntity) {
    val cosmos = LocalCosmos.current
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(0.85f)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                    .background(cosmos.glassBg)
                    .border(
                        BorderStroke(1.dp, CosmosColors.AccentPrimary.copy(alpha = 0.30f)),
                        RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp),
                    )
                    .padding(10.dp),
            ) {
                MarkdownView(text = message.content)
            }
        }
        Spacer(Modifier.weight(0.15f))
    }
}

@Composable
private fun HypothesisNutzerBubble(message: HypothesisMessageEntity) {
    val cosmos = LocalCosmos.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Spacer(Modifier.weight(0.15f))
        Box(
            Modifier
                .weight(0.85f)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                .background(CosmosColors.AccentSecondary.copy(alpha = 0.20f))
                .padding(10.dp),
        ) {
            Text(
                text = message.content,
                color = cosmos.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatRelative(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime().format(FORMATTER)

