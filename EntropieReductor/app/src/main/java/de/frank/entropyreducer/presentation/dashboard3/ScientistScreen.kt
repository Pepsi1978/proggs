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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.domain.model.HypothesisStatus
import de.frank.entropyreducer.domain.model.ScientistRole
import de.frank.entropyreducer.presentation.ThemeViewModel
import de.frank.entropyreducer.presentation.components.CosmosScaffold
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
            CosmosBottomBar(
                currentTab = currentTab,
                micState = de.frank.entropyreducer.presentation.components.MicState.IDLE,
                onTabSelected = onSwitchTab,
                onMicClick = {},
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        when (msg.role) {
                            ScientistRole.KI -> KiBubble(
                                message = msg,
                                hypotheses = state.hypothesesByMessageId[msg.id].orEmpty(),
                                onTryHypothesis = { hypothesisToStart = it },
                            )
                            ScientistRole.NUTZER -> NutzerBubble(msg)
                        }
                    }
                    if (state.isThinking) {
                        item {
                            ThinkingIndicator()
                        }
                    }
                    // 200dp damit das letzte Item (oft eine Hypothese-Card mit
                    // viel Inhalt) nicht von der ChatInputBar verdeckt wird.
                    item { Spacer(Modifier.height(200.dp)) }
                }

                ChatInputBar(
                    draft = state.draftText,
                    onDraftChange = vm::setDraft,
                    micState = state.micState,
                    onMicClick = {
                        if (micPerm.check()) vm.onMicClick() else micPerm.request()
                    },
                    onSend = vm::send,
                    canSend = state.draftText.isNotBlank() && !state.isThinking,
                )
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 110.dp),
            ) { Snackbar(it) }
        }
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
}

@Composable
private fun KiBubble(
    message: ScientistMessageEntity,
    hypotheses: List<HypothesisEntity>,
    onTryHypothesis: (HypothesisEntity) -> Unit,
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
                        HypothesisCardInChat(h, onTry = { onTryHypothesis(h) })
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
private fun HypothesisCardInChat(h: HypothesisEntity, onTry: () -> Unit) {
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
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Hypothese",
                color = CosmosColors.AccentPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
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
            if (isProposed) {
                TextButton(onClick = onTry) {
                    Text("Diese Hypothese ausprobieren", color = CosmosColors.AccentPrimary)
                }
            } else {
                Text(
                    text = "Status: ${h.status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    color = CosmosColors.Success,
                    style = MaterialTheme.typography.labelMedium,
                )
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

@Composable
private fun ChatInputBar(
    draft: String,
    onDraftChange: (String) -> Unit,
    micState: de.frank.entropyreducer.presentation.components.MicState,
    onMicClick: () -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,
) {
    val cosmos = LocalCosmos.current
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(
        Modifier
            .fillMaxWidth()
            .imePadding()
            // BottomBar ist Overlay (Frank-Wunsch 2026-05-08): ChatInputBar muss
            // über der BottomBar bleiben — bottom-Padding = 72dp BottomBar +
            // bottomInset System-Nav.
            .padding(bottom = 72.dp + bottomInset)
            .background(cosmos.glassBg)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Schreib oder sprich …", color = cosmos.textSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = cosmos.textPrimary,
                    unfocusedTextColor = cosmos.textPrimary,
                    focusedBorderColor = CosmosColors.AccentPrimary,
                    unfocusedBorderColor = cosmos.glassBorder,
                ),
                shape = RoundedCornerShape(20.dp),
                maxLines = 4,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
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
                    contentDescription = "Mikrofon",
                    tint = if (micState == de.frank.entropyreducer.presentation.components.MicState.RECORDING) {
                        CosmosColors.Critical
                    } else {
                        CosmosColors.AccentSecondary
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (canSend) CosmosColors.AccentPrimary
                        else CosmosColors.AccentPrimary.copy(alpha = 0.30f),
                    ),
            ) {
                Icon(Icons.Outlined.Send, "Senden", tint = CosmosColors.BgDark)
            }
        }
    }
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
                    text = "Waehle einen Startzeitpunkt — du kannst ihn später im Experiment-Kalender korrigieren.",
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

private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatRelative(ms: Long): String =
    Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime().format(FORMATTER)

