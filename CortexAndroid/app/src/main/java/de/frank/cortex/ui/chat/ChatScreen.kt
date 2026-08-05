package de.frank.cortex.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.cortex.data.ChatSessionSummary
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.UserContextPrompt
import de.frank.cortex.data.model.ChatOption
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import java.util.Locale

private enum class TranscriptionTarget { ChatInput, ContextPrompt }

// Saver fuers rememberSaveable: bei Rotation waehrend einer laufenden Aufnahme fiel das Ziel
// sonst auf ChatInput zurueck und das Transkript landete im falschen Feld.
private val transcriptionTargetSaver = androidx.compose.runtime.saveable.Saver<TranscriptionTarget, String>(
    save = { it.name },
    restore = { name -> TranscriptionTarget.entries.firstOrNull { it.name == name } ?: TranscriptionTarget.ChatInput }
)

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val vpnState by WireGuardManager.state.collectAsStateWithLifecycle()
    var inputText by rememberSaveable { mutableStateOf("") }
    var isContextPromptDialogOpen by rememberSaveable { mutableStateOf(false) }
    // rememberSaveable: der Dialog-Offen-Zustand ueberlebte die Rotation, der getippte
    // Entwurf und das Diktier-Ziel aber nicht — ein langer Prompt-Entwurf war einfach weg.
    var contextPromptDraft by rememberSaveable { mutableStateOf("") }
    var transcriptionTarget by rememberSaveable(stateSaver = transcriptionTargetSaver) {
        mutableStateOf(TranscriptionTarget.ChatInput)
    }
    var pendingMicTarget by rememberSaveable(stateSaver = transcriptionTargetSaver) {
        mutableStateOf(TranscriptionTarget.ChatInput)
    }
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed,
        confirmStateChange = { value ->
            if (value == DrawerValue.Closed) vm.closeSessionsPanel()
            true
        }
    )
    val context = LocalContext.current
    val toneVolume = SettingsStore.recordingToneVolume
    // try/catch: der ToneGenerator-Konstruktor kann bei Ressourcenmangel eine RuntimeException
    // werfen — ungefangen crashte das die Komposition. Ohne Ton geht die Aufnahme trotzdem.
    val recordingTone = remember(toneVolume) {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, Math.round(toneVolume * 100f))
        } catch (e: Exception) {
            CortexLog.warn("ChatScreen", "recordingTone", "ToneGenerator nicht verfügbar: ${e.message}")
            null
        }
    }
    val emptyTitle = when (uiState.contextMode) {
        SettingsStore.CONTEXT_MODE_SMALLTALK -> "Smalltalk-Modus ist aktiv."
        SettingsStore.CONTEXT_MODE_SAVE -> "Speichermodus ist aktiv."
        SettingsStore.CONTEXT_MODE_RULE -> "Regelmodus ist aktiv."
        SettingsStore.CONTEXT_MODE_SEARCH -> "Suchmodus ist aktiv."
        else -> "Dein Speicher hört zu."
    }
    val emptySubtitle = when (uiState.contextMode) {
        SettingsStore.CONTEXT_MODE_SMALLTALK -> "Sag oder tippe etwas — ich plaudere nur mit dir und speichere oder suche nichts."
        SettingsStore.CONTEXT_MODE_SAVE -> "Sag oder tippe etwas — ich behandle es als Information zum Ablegen."
        SettingsStore.CONTEXT_MODE_RULE -> "Sag oder tippe eine Regel — nur die Regeldatei des Hauptagenten wird vorbereitet."
        SettingsStore.CONTEXT_MODE_SEARCH -> "Frag oder tippe ein Thema — ich suche gezielt in deinem Gedächtnis."
        else -> "Sag oder tippe etwas — ich lege es ab oder schlage es nach."
    }

    fun playRecordingTone() {
        if (!SettingsStore.recordingToneEnabled) return
        try {
            recordingTone?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        } catch (e: Exception) {
            CortexLog.warn("ChatScreen", "recordingTone", "Aufnahmeton fehlgeschlagen: ${e.message}")
        }
    }

    fun appendTranscription(existing: String, text: String): String =
        if (existing.isBlank()) text else "$existing $text"

    DisposableEffect(recordingTone) {
        onDispose { recordingTone?.release() }
    }

    // Berechtigung für Mikrofon
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            transcriptionTarget = pendingMicTarget
            playRecordingTone()
            vm.toggleRecording()
        }
    }

    // Transkribierten Text ins Eingabefeld übernehmen
    LaunchedEffect(Unit) {
        vm.transcribedText.collect { text ->
            if (transcriptionTarget == TranscriptionTarget.ContextPrompt && isContextPromptDialogOpen) {
                contextPromptDraft = appendTranscription(contextPromptDraft, text)
            } else {
                inputText = appendTranscription(inputText, text)
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.improvedText.collect { text ->
            inputText = text
        }
    }

    LaunchedEffect(Unit) {
        ChatCommands.newChat.collect {
            inputText = ""
            vm.startNewChat()
        }
    }

    LaunchedEffect(Unit) {
        ChatCommands.openSessions.collect {
            vm.openSessionsPanel()
        }
    }

    LaunchedEffect(uiState.isSessionsPanelOpen) {
        if (uiState.isSessionsPanelOpen && drawerState.isClosed) {
            drawerState.open()
        } else if (!uiState.isSessionsPanelOpen && drawerState.isOpen) {
            drawerState.close()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            vm.clearError()
        }
    }

    // Auch bei SESSION-Wechsel ans Ende scrollen: haengt der Key nur an der Anzahl, blieb die
    // Liste beim Wechsel zwischen zwei gleich langen Sessions mitten in der alten Position stehen.
    LaunchedEffect(uiState.sessionId, uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.isSessionsPanelOpen,
        drawerContent = {
            SessionDrawer(
                sessions = uiState.sessions,
                currentSessionId = uiState.sessionId,
                onSessionClick = { session ->
                    inputText = ""
                    vm.selectSession(session.id)
                },
                onSessionDelete = { session -> vm.deleteSession(session.id) },
                onClose = vm::closeSessionsPanel
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(Modifier.fillMaxSize()) {
            // Messages OR Empty State
            if (uiState.messages.isEmpty()) {
            // Empty State with breathing brain
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "breathe")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.5f, targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse
                    ), label = "breathe_alpha"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(horizontal = 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .scale(alpha)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Iris.copy(alpha = 0.28f), Mint.copy(alpha = 0.16f))
                                )
                            )
                            .border(1.dp, Iris.copy(alpha = 0.34f), RoundedCornerShape(22.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = "Cortex Speicher",
                            tint = Iris,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    Text(
                        text = emptyTitle,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = emptySubtitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                }
            }
            } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                items(uiState.messages, key = { it.id }, contentType = { it.isUser }) { message ->
                    ChatBubble(
                        message = message,
                        isSpeaking = uiState.speakingMessageId == message.id,
                        onSpeakClick = { vm.toggleMessageSpeech(message.id, message.text) },
                        onShareClick = { vm.prepareMessageShare(message.id) },
                        onOptionClick = vm::sendOption,
                        onEditedStoreSave = vm::saveEditedStoreConfirmation
                    )
                }

                // Typing indicator
                if (uiState.isLoading) {
                    item { TypingIndicator() }
                }
            }
            }

            // Input Block
            ChatInputBlock(
                text = inputText,
                onTextChange = { inputText = it },
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategoryChange = vm::updateSelectedCategory,
                onCreateCategory = vm::createCategory,
                onOpenCategories = vm::loadCategories,
                contextMode = uiState.contextMode,
                onContextModeChange = vm::updateContextMode,
                responseSize = uiState.responseSize,
                onResponseSizeChange = vm::updateResponseSize,
                titleOverride = uiState.titleOverride,
                onTitleChange = vm::updateTitleOverride,
                isGeneratingTitle = uiState.isGeneratingTitle,
                onGenerateTitle = { vm.generateTitleFromText(inputText) },
                ttsEnabled = uiState.ttsEnabled,
                onToggleTts = vm::toggleTts,
                isRecording = uiState.isRecording,
                isTranscribing = uiState.isTranscribing,
                isImproving = uiState.isImproving,
                onOpenContextPrompts = { isContextPromptDialogOpen = true },
                onMicToggle = {
                    pendingMicTarget = TranscriptionTarget.ChatInput
                    transcriptionTarget = TranscriptionTarget.ChatInput
                    CortexLog.info("ChatScreen", "micClick", "Mikrofon angeklickt")
                    val hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                    CortexLog.info("ChatScreen", "micClick", "Permission: $hasPermission")
                    if (hasPermission) {
                        if (!uiState.isRecording && !uiState.isTranscribing) {
                            playRecordingTone()
                        }
                        vm.toggleRecording()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onSend = {
                    // Nur leeren, wenn sendMessage die Nachricht angenommen hat. Abgelehnt
                    // (zu lang / offline im Nicht-Outbox-Modus) bleibt der Entwurf stehen.
                    if (vm.sendMessage(inputText)) inputText = ""
                },
                onClear = {
                    inputText = ""
                    vm.updateTitleOverride("")
                },
                onImprove = { vm.improveText(inputText) }
            )
        }

        if (isContextPromptDialogOpen) {
            ContextPromptDialog(
                prompts = uiState.userContextPrompts,
                draft = contextPromptDraft,
                onDraftChange = { contextPromptDraft = it },
                improvingPromptId = uiState.improvingContextPromptId,
                isRecording = uiState.isRecording && transcriptionTarget == TranscriptionTarget.ContextPrompt,
                isTranscribing = uiState.isTranscribing && transcriptionTarget == TranscriptionTarget.ContextPrompt,
                onMicToggle = {
                    pendingMicTarget = TranscriptionTarget.ContextPrompt
                    transcriptionTarget = TranscriptionTarget.ContextPrompt
                    val hasPermission = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        if (!uiState.isRecording && !uiState.isTranscribing) {
                            playRecordingTone()
                        }
                        vm.toggleRecording()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onSend = {
                    vm.addUserContextPrompt(contextPromptDraft)
                    contextPromptDraft = ""
                },
                onPromptChange = vm::updateUserContextPrompt,
                onPromptEnabledChange = vm::toggleUserContextPrompt,
                onPromptImprove = vm::improveUserContextPrompt,
                onPromptRestoreOriginal = vm::restoreUserContextPromptOriginal,
                onPromptDelete = vm::deleteUserContextPrompt,
                onClose = { isContextPromptDialogOpen = false }
            )
        }

        if (uiState.isRecording) {
            RecordingOverlay(onStop = { vm.toggleRecording() })
        }
        }
    }
}

@Composable
private fun SessionDrawer(
    sessions: List<ChatSessionSummary>,
    currentSessionId: String,
    onSessionClick: (ChatSessionSummary) -> Unit,
    onSessionDelete: (ChatSessionSummary) -> Unit,
    onClose: () -> Unit
) {
    var deleteCandidate by remember { mutableStateOf<ChatSessionSummary?>(null) }

    deleteCandidate?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Session löschen?") },
            text = {
                Text("„${session.title}“ und alle Nachrichten darin werden dauerhaft aus dieser App gelöscht.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                        onSessionDelete(session)
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Eigene, klar abgesetzte Seitenleisten-Optik (Frank-Wunsch 2026-07-02): hellerer Panel-
    // Hintergrund (surfaceVariant statt surface), abgerundete rechte Kante + Iris-Akzentrahmen,
    // damit sich die Leiste sichtbar vom Rest der App abhebt.
    val drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(322.dp)
            .border(1.dp, Iris.copy(alpha = 0.35f), drawerShape),
        drawerShape = drawerShape,
        drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-10).dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Sessions schließen",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            // Der "Neuer Chat"-Button lebt in der Top-Bar rechts neben dem Sessions-Knopf —
            // hier war er doppelt und wurde entfernt (Frank-Wunsch 2026-07-02).
            if (sessions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Noch keine gespeicherten Sessions. Sobald du eine Nachricht sendest, erscheint die Unterhaltung hier.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 12.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        val active = session.id == currentSessionId
                        SessionRow(
                            session = session,
                            active = active,
                            onClick = { onSessionClick(session) },
                            onLongClick = { deleteCandidate = session }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SessionRow(
    session: ChatSessionSummary,
    active: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = if (active) Iris.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
    val background = if (active) Iris.copy(alpha = 0.13f) else MaterialTheme.colorScheme.background
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = session.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.5.sp,
                lineHeight = 21.sp,
                // KEIN Abschneiden (Frank-Wunsch 2026-07-02): Der KI-Intentions-Titel wird IMMER
                // vollstaendig angezeigt — braucht er 4-6 Zeilen, waechst die Kachel einfach mit.
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${formatSessionTimestamp(session.updatedAt)} · ${session.messageCount} Nachrichten",
                fontFamily = JetBrainsMono,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// DateTimeFormatter statt SimpleDateFormat: thread-safe UND wiederverwendbar — vorher wurde pro
// SessionRow-Aufruf (jede Drawer-Recomposition) ein neues SimpleDateFormat gebaut (teuer).
private val sessionTimestampFormatter =
    java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm 'Uhr'", Locale.GERMANY)

private fun formatSessionTimestamp(timestamp: Long): String =
    java.time.Instant.ofEpochMilli(timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .format(sessionTimestampFormatter)

/** Wortzahl einer Nachricht (Whitespace-getrennt) — fuer die Anzeige "(N Wörter)" in der Bubble.
 *  Regex einmalig kompiliert (lief vorher pro Bubble-Recomposition neu — beim Streaming pro Delta). */
private val wordSplitRegex = Regex("\\s+")

private fun countWords(text: String): Int =
    text.trim().split(wordSplitRegex).count { it.isNotBlank() }

private fun formatResponseTime(responseTimeMs: Long): String {
    val seconds = responseTimeMs / 1_000.0
    return if (seconds < 60) {
        String.format(Locale.GERMANY, "%.1f s", seconds)
    } else {
        val minutes = (seconds / 60).toInt()
        String.format(Locale.GERMANY, "%d min %.1f s", minutes, seconds % 60)
    }
}

internal fun formatSourceUsage(sourceUsage: List<String>?): String? {
    if (sourceUsage == null) return null
    val memoryUsed = "memory" in sourceUsage
    val internetUsed = "internet" in sourceUsage
    return when {
        memoryUsed && internetUsed -> "Gedächtnis + Internet"
        memoryUsed -> "Gedächtnis"
        internetUsed -> "Internet"
        sourceUsage.isEmpty() -> "Ohne Suche"
        else -> null
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isSpeaking: Boolean,
    onSpeakClick: () -> Unit,
    onShareClick: () -> Unit,
    onOptionClick: (ChatOption) -> Unit,
    onEditedStoreSave: (ChatMessage, String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val canEditStoreText = !message.isUser && message.memoryEditable && !message.memoryText.isNullOrBlank()
    val canEditRuleText = !message.isUser && message.action == "rule_confirm"
    val initialEditableText = remember(message.memoryText, message.text) {
        message.memoryText?.trim().orEmpty().ifBlank { message.text.trim() }
    }
    var isEditingStoreText by rememberSaveable(message.id) { mutableStateOf(false) }
    var editedStoreText by rememberSaveable(message.id) { mutableStateOf(initialEditableText) }
    var isEditingRuleText by rememberSaveable(message.id) { mutableStateOf(false) }
    var editedRuleText by rememberSaveable(message.id) { mutableStateOf(message.text.trim()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        if (message.isUser) {
            // User bubble: iris border, iris background
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp),
                color = if (isDark) DarkUserBubble else LightUserBubble,
                border = BorderStroke(1.dp, if (isDark) DarkUserBorder else LightUserBorder)
            ) {
                SelectionContainer {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        fontSize = 14.5.sp,
                        lineHeight = 21.7.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            fontSize = 14.5.sp,
                            lineHeight = 22.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MessageBubbleActionIcon(
                            icon = {
                                Icon(
                                    if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSpeaking) "Vorlesen stoppen" else "Nachricht vorlesen",
                                    modifier = Modifier.size(if (isSpeaking) 18.dp else 21.dp)
                                )
                            },
                            onClick = onSpeakClick
                        )
                        MessageBubbleActionIcon(
                            icon = {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Nachricht teilen",
                                    modifier = Modifier.size(19.dp)
                                )
                            },
                            onClick = onShareClick
                        )
                        Spacer(Modifier.weight(1f))
                        // Wortzahl der Antwort, rechtsbuendig (Frank-Wunsch 2026-07-02): auf einen
                        // Blick sehen, wie lang die Antwort wirklich war (S/M/XL-Kontrolle).
                        // remember(text): der O(n)-Split laeuft nur bei Text-Aenderung, nicht bei
                        // jeder Recomposition (z.B. isSpeaking-Wechsel).
                        val wordCount = remember(message.text) { countWords(message.text) }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "($wordCount Wörter)",
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val sourceAndTime = listOfNotNull(
                                formatSourceUsage(message.sourceUsage),
                                message.responseTimeMs?.let(::formatResponseTime)
                            ).joinToString(" · ")
                            if (sourceAndTime.isNotBlank()) {
                                Text(
                                    text = sourceAndTime,
                                    fontFamily = JetBrainsMono,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Meta line
        if (!message.isUser && message.action != null) {
            val metaText = when (message.action) {
                "store", "memory_updated", "memory_show" -> {
                    val categories = message.categories.ifEmpty { listOfNotNull(message.category) }
                    val place = categories.joinToString(", ").ifBlank { "Unkategorisiert" }
                    "\u21B3 ${if (message.action == "memory_updated") "aktualisiert" else if (message.action == "memory_show") "aktueller Stand" else "abgelegt"} in \u201E$place\u201C · \u201E${message.title ?: "Ohne Titel"}\u201C"
                }
                "recall" -> "\u21B3 nachgeschlagen \u00B7 ${message.recallHits ?: 0} Treffer"
                "save_confirm", "store_clarify", "memory_edit_confirm", "memory_dialog" -> "\u21B3 Rückfrage\u2026"
                "rule_confirm" -> "\u21B3 Regel-Vorschlag · Ja / Nein / Bearbeiten"
                "rule_saved" -> "\u21B3 Regel übernommen"
                "rule_cancelled" -> "\u21B3 keine Regel angelegt"
                "cancel" -> "\u21B3 nicht gespeichert"
                else -> null
            }
            val metaColor = when (message.action) {
                "store", "memory_updated" -> Mint
                "recall" -> Iris
                "save_confirm", "store_clarify", "memory_edit_confirm", "memory_dialog" -> Amber
                "rule_confirm" -> Amber
                "rule_saved" -> Mint
                "cancel" -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            if (metaText != null) {
                Text(
                    text = metaText,
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 0.3.sp,
                    color = metaColor,
                    modifier = Modifier.padding(start = 4.dp, top = 5.dp)
                )
            }
        }

        // Options chips
        if (!message.isUser && message.options != null && message.options.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                message.options.forEach { option ->
                    OptionChip(label = option.label, isDark = isDark) {
                        if (canEditRuleText && option.send == "regel bearbeiten") {
                            editedRuleText = message.text.trim()
                            isEditingRuleText = true
                        } else {
                            onOptionClick(option)
                        }
                    }
                }
            }
        }

        // Beide Editoren laufen ueber denselben Vollbild-Dialog. Inline in der Chatliste war der Text
        // bei offener Tastatur nicht mehr sichtbar: unter der Liste steht die hohe Eingabeleiste,
        // durch adjustResize + IME blieb fuer die Liste fast keine Hoehe uebrig (Frank-Bug 2026-08-05,
        // gleiche Fehlerklasse wie bugs/android/jetpack-compose.md §8.8).
        if (canEditRuleText && isEditingRuleText) {
            MemoryTextEditDialog(
                title = "Regel bearbeiten",
                initialValue = editedRuleText,
                onValueChange = { editedRuleText = it },
                confirmLabel = "Bearbeitung bestätigen",
                isDark = isDark,
                maxChars = 240,
                onDismiss = { isEditingRuleText = false },
                onConfirm = {
                    val text = editedRuleText.trim()
                    if (text.isNotEmpty()) {
                        onOptionClick(ChatOption("Regel speichern", "regel speichern: $text"))
                        isEditingRuleText = false
                    }
                }
            )
        }

        if (canEditStoreText) {
            Row(modifier = Modifier.padding(top = 5.dp)) {
                // Bewusst OHNE Zuruecksetzen auf den Vorschlagstext: wer den Editor schliesst (auch
                // per Zurueckwischen) und ihn wieder oeffnet, findet seine Bearbeitung unveraendert
                // vor, statt von vorn anfangen zu muessen.
                OptionChip(label = "Editieren", isDark = isDark, tint = Amber) {
                    isEditingStoreText = true
                }
            }
        }

        if (canEditStoreText && isEditingStoreText) {
            MemoryTextEditDialog(
                title = "Eintrag bearbeiten",
                initialValue = editedStoreText,
                onValueChange = { editedStoreText = it },
                confirmLabel = "Speichern",
                isDark = isDark,
                onDismiss = { isEditingStoreText = false },
                onConfirm = {
                    onEditedStoreSave(message, editedStoreText)
                    isEditingStoreText = false
                }
            )
        }
    }
}

/**
 * Vollbild-Editor fuer den vorgeschlagenen Eintrag bzw. eine Regel.
 *
 * Bewusst ein Dialog und kein Inline-Feld: Er blendet die hohe Eingabeleiste aus. Das Fenster ist
 * KEIN Vollbild, sondern eine kompakte Karte, die unmittelbar ueber der Tastatur sitzt — Text UND
 * Speichern-Knopf bleiben dadurch immer gleichzeitig sichtbar. Der Cursor sitzt beim Oeffnen am
 * Textende, die Tastatur kommt von selbst; Frank muss nicht mehr blind ins Feld tippen.
 *
 * `decorFitsSystemWindows = false` ist dabei PFLICHT: Ohne dieses Flag bekommt das eigene Fenster
 * eines Compose-Dialogs die Tastatur-Insets gar nicht, `imePadding()` bleibt wirkungslos und die
 * Knopfreihe verschwindet hinter der Tastatur (genau Franks Fund 2026-08-05).
 */
@Composable
private fun MemoryTextEditDialog(
    title: String,
    initialValue: String,
    onValueChange: (String) -> Unit,
    confirmLabel: String,
    isDark: Boolean,
    maxChars: Int = Int.MAX_VALUE,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    // Cursor deterministisch ans Textende: bei BasicTextField(String) staende er sonst am Anfang und
    // Frank muesste wieder blind ins Feld tippen. Der Dialog haelt die Cursor-/Auswahlposition,
    // nach aussen geht weiter nur der reine Text.
    var field by remember { mutableStateOf(TextFieldValue(initialValue, TextRange(initialValue.length))) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = title,
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) DarkField else LightField,
                        border = BorderStroke(1.dp, Amber.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .fillMaxWidth()
                    ) {
                        BasicTextField(
                            value = field,
                            onValueChange = {
                                // Ueberlange Eingaben werden nicht uebernommen, statt den Text zu kappen —
                                // so bleiben Feldinhalt und aeusserer Zustand immer identisch.
                                if (it.text.length <= maxChars) {
                                    field = it
                                    onValueChange(it.text)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                // Feste Ober-/Untergrenze: das Feld waechst mit dem Text, draengt die
                                // Knopfreihe aber NIE aus dem sichtbaren Bereich; darueber hinaus
                                // scrollt es in sich selbst.
                                .heightIn(min = 96.dp, max = 220.dp)
                                .padding(12.dp)
                                .focusRequester(focusRequester),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 21.7.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(Amber)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OptionChip(
                            label = "Abbrechen",
                            isDark = isDark,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onDismiss
                        )
                        Spacer(Modifier.weight(1f))
                        OptionChip(
                            label = confirmLabel,
                            isDark = isDark,
                            tint = Mint,
                            onClick = onConfirm
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionChip(
    label: String,
    isDark: Boolean,
    tint: Color = Iris,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = tint.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, if (isDark) DarkUserBorder else LightUserBorder)
    ) {
        Text(
            text = label,
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 8.dp),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = tint
        )
    }
}

@Composable
private fun MessageBubbleActionIcon(icon: @Composable () -> Unit, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Orange) {
            icon()
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { i ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot_$i")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = i * 200, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot_offset_$i"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.45f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, delayMillis = i * 200, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot_alpha_$i"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .offset(y = offsetY.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
            )
        }
    }
}

@Composable
private fun RecordingOverlay(onStop: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(onClick = onStop),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shadowElevation = 18.dp,
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 36.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Orange)
                    )
                    Text(
                        "AUFNAHME",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = Orange
                    )
                }

                Row(
                    modifier = Modifier.height(54.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(13) { index ->
                        val infinite = rememberInfiniteTransition(label = "rec_bar_$index")
                        val scale by infinite.animateFloat(
                            initialValue = 0.28f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 650 + (index % 4) * 90,
                                    delayMillis = index * 45,
                                    easing = EaseInOut
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "rec_bar_scale_$index"
                        )
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height((46 * scale).dp.coerceAtLeast(10.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Orange, Orange.copy(alpha = 0.45f))
                                    )
                                )
                        )
                    }
                }

                Text(
                    "Ich höre zu… ▍",
                    minLines = 2,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Orange)
                        .clickable(onClick = onStop),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Aufnahme stoppen", tint = Color.White, modifier = Modifier.size(30.dp))
                }

                Text("Tippen zum Stoppen", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBlock(
    text: String,
    onTextChange: (String) -> Unit,
    categories: List<de.frank.cortex.data.model.CategoryInfo>,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    onCreateCategory: (String) -> Unit,
    onOpenCategories: () -> Unit,
    contextMode: String,
    onContextModeChange: (String) -> Unit,
    responseSize: String,
    onResponseSizeChange: (String) -> Unit,
    titleOverride: String,
    onTitleChange: (String) -> Unit,
    isGeneratingTitle: Boolean,
    onGenerateTitle: () -> Unit,
    ttsEnabled: Boolean,
    onToggleTts: () -> Unit,
    isRecording: Boolean,
    isTranscribing: Boolean,
    isImproving: Boolean,
    onOpenContextPrompts: () -> Unit,
    onMicToggle: () -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
    onImprove: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val actionOrangeBg = Orange.copy(alpha = 0.20f)
    val actionOrangeBorder = Orange.copy(alpha = 0.42f)

    Surface(
        modifier = Modifier.imePadding(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            ContextModeBar(
                selectedMode = contextMode,
                onModeChange = onContextModeChange,
                responseSize = responseSize,
                onResponseSizeChange = onResponseSizeChange,
                isDark = isDark
            )

            Spacer(Modifier.height(8.dp))

            // Titel und Kategorie gehören nur zu Gedächtniseinträgen, nie zur Regeldatei.
            if (contextMode != SettingsStore.CONTEXT_MODE_RULE) Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Mint.copy(alpha = 0.13f))
                        .border(1.dp, Mint.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
                        .clickable(enabled = text.isNotBlank() && !isGeneratingTitle, onClick = onGenerateTitle),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGeneratingTitle) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Mint
                        )
                    } else {
                        Text("T", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
                            fontSize = 16.sp, color = Mint)
                    }
                }
                BasicTextField(
                    value = titleOverride,
                    onValueChange = { if (it.length <= 200) onTitleChange(it) },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    minLines = 1,
                    maxLines = 2,
                    decorationBox = { inner ->
                        if (titleOverride.isEmpty()) {
                            Text("Titel (optional)", fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    }
                )

                // Category pill — hierarchischer Drilldown wie im Web-Cortex
                CategoryPickerPill(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategoryChange = onCategoryChange,
                    onCreateCategory = onCreateCategory,
                    onOpen = onOpenCategories,
                    isDark = isDark
                )
            }

            if (contextMode != SettingsStore.CONTEXT_MODE_RULE) Spacer(Modifier.height(8.dp))

            // Textarea
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) DarkField else LightField,
                border = BorderStroke(1.dp,
                    if (text.isNotEmpty()) Iris
                    else if (isDark) DarkFieldBorder else LightFieldBorder)
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 92.dp)
                        .padding(12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 21.7.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    minLines = 3,
                    maxLines = 5,
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                if (contextMode == SettingsStore.CONTEXT_MODE_RULE) "Regel für den Hauptagenten\u2026"
                                else "Ablegen oder nachschlagen\u2026",
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(9.dp))

            // Action row
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val actionRowWidth = maxWidth
                val actionGap = ((actionRowWidth - 288.dp) / 5).coerceIn(0.dp, 9.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(actionGap)
                ) {
                // Clear (dunkles Orange wie Lautsprecher/Mikro)
                ActionButton(
                    icon = { Icon(Icons.AutoMirrored.Filled.Backspace, null, Modifier.size(20.dp)) },
                    bg = actionOrangeBg,
                    border = actionOrangeBorder,
                    tint = Orange,
                    onClick = onClear
                )

                // G - Improve (mint, 38x38, radius 11)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(enabled = text.isNotBlank() && !isImproving, onClick = onImprove),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Mint.copy(alpha = 0.13f))
                            .border(1.dp, Mint.copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImproving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Mint
                            )
                        } else {
                            Text("G", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
                                fontSize = 18.sp, color = Mint)
                        }
                    }
                }

                // K - Kontext-Prompts. Bewusst neben G statt in der Modusleiste, damit dort
                // Lupe, R, Diskette und Automatik als fachlich getrennte Modi zusammenstehen.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            contentDescription = "Kontext-Prompts öffnen"
                            role = Role.Button
                        }
                        .clickable(onClick = onOpenContextPrompts),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(if (isDark) DarkField else LightField)
                            .border(
                                1.dp,
                                if (isDark) DarkFieldBorder else LightFieldBorder,
                                RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "K",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (actionRowWidth >= 342.dp) Spacer(Modifier.weight(1f))

                // TTS toggle (38x38): AN = orange, AUS = grau — so ist auf einen Blick erkennbar,
                // dass das Vorlesen ausgeschaltet ist (Frank-Wunsch 2026-07-02).
                val ttsOffColor = MaterialTheme.colorScheme.onSurfaceVariant
                ActionButton(
                    icon = {
                        Icon(
                            if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp
                            else Icons.AutoMirrored.Filled.VolumeOff,
                            null, Modifier.size(21.dp)
                        )
                    },
                    bg = if (ttsEnabled) actionOrangeBg else ttsOffColor.copy(alpha = 0.14f),
                    border = if (ttsEnabled) actionOrangeBorder else ttsOffColor.copy(alpha = 0.35f),
                    tint = if (ttsEnabled) Orange else ttsOffColor,
                    onClick = onToggleTts
                )

                // Mic (38x38, orange, highlighted when recording)
                MicActionButton(
                    isRecording = isRecording,
                    isTranscribing = isTranscribing,
                    onClick = onMicToggle
                )

                // Send (44x44, radius 13, solid orange + shadow)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(enabled = text.isNotBlank(), onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Orange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Senden",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun ContextPromptDialog(
    prompts: List<UserContextPrompt>,
    draft: String,
    onDraftChange: (String) -> Unit,
    improvingPromptId: String?,
    isRecording: Boolean,
    isTranscribing: Boolean,
    onMicToggle: () -> Unit,
    onSend: () -> Unit,
    onPromptChange: (String, String) -> Unit,
    onPromptEnabledChange: (String, Boolean) -> Unit,
    onPromptImprove: (String) -> Unit,
    onPromptRestoreOriginal: (String) -> Unit,
    onPromptDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Kontext-Prompts",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Schließen")
                    }
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDark) DarkField else LightField,
                    border = BorderStroke(1.dp, if (draft.isNotBlank()) Iris else if (isDark) DarkFieldBorder else LightFieldBorder)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        BasicTextField(
                            value = draft,
                            onValueChange = onDraftChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 120.dp),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            minLines = 4,
                            maxLines = 8,
                            decorationBox = { inner ->
                                if (draft.isEmpty()) {
                                    Text("Prompt", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                inner()
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MicActionButton(
                                isRecording = isRecording,
                                isTranscribing = isTranscribing,
                                onClick = onMicToggle
                            )
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(if (draft.isNotBlank()) Orange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
                                    .clickable(enabled = draft.isNotBlank(), onClick = onSend),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Prompt speichern",
                                    tint = if (draft.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                "Mikrofon transkribiert direkt in dieses Prompt-Feld.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                Spacer(Modifier.height(10.dp))

                if (prompts.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            "Noch keine Kontext-Prompts gespeichert.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(prompts, key = { it.id }) { prompt ->
                            ContextPromptRow(
                                prompt = prompt,
                                isImproving = improvingPromptId == prompt.id,
                                onTextChange = { onPromptChange(prompt.id, it) },
                                onEnabledChange = { onPromptEnabledChange(prompt.id, it) },
                                onImprove = { onPromptImprove(prompt.id) },
                                onRestoreOriginal = { onPromptRestoreOriginal(prompt.id) },
                                onDelete = { onPromptDelete(prompt.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextPromptRow(
    prompt: UserContextPrompt,
    isImproving: Boolean,
    onTextChange: (String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onImprove: () -> Unit,
    onRestoreOriginal: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) DarkField else LightField,
        border = BorderStroke(1.dp, if (prompt.enabled) Iris.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = prompt.enabled,
                    onCheckedChange = onEnabledChange
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Mint.copy(alpha = 0.13f))
                        .border(1.dp, Mint.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
                        .clickable(enabled = prompt.text.isNotBlank() && !isImproving, onClick = onImprove),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImproving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Mint
                        )
                    } else {
                        Text("G", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, color = Mint)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.13f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f), RoundedCornerShape(11.dp))
                        .clickable(enabled = !prompt.originalText.isNullOrBlank(), onClick = onRestoreOriginal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Original wiederherstellen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    contextPromptTitle(prompt.text),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (prompt.enabled) Iris else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                BasicTextField(
                    value = prompt.text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    minLines = 2,
                    maxLines = Int.MAX_VALUE,
                    decorationBox = { inner ->
                        if (prompt.text.isEmpty()) {
                            Text("Prompt", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    }
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Prompt löschen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun contextPromptTitle(text: String): String {
    val title = Regex("[\\p{L}\\p{N}]+")
        .findAll(text.trim())
        .map { it.value }
        .take(3)
        .joinToString(" ")
    return title.ifBlank { "Prompt" }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextModeBar(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    responseSize: String,
    onResponseSizeChange: (String) -> Unit,
    isDark: Boolean
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                SettingsStore.RESPONSE_SIZE_AUTO to "A",
                SettingsStore.RESPONSE_SIZE_SHORT to "S",
                SettingsStore.RESPONSE_SIZE_MEDIUM to "M",
                SettingsStore.RESPONSE_SIZE_XL to "XL"
            ).forEach { (size, label) ->
                val active = responseSize == size
                val tint = if (active) Orange else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .width(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (active) Orange.copy(alpha = 0.22f) else if (isDark) DarkField else LightField)
                        .border(
                            1.dp,
                            if (active) Orange.copy(alpha = 0.52f) else if (isDark) DarkFieldBorder else LightFieldBorder,
                            RoundedCornerShape(11.dp)
                        )
                        .clickable { onResponseSizeChange(size) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = tint)
                }
            }
        }

        // Smalltalk-Knopf bewusst ENTFERNT (Frank-Wunsch 2026-07-02): Smalltalk erkennt der
        // Router im Auto-Modus selbst. Regeln laufen ausschließlich über den R-Modus.
        val items = listOf(
            SettingsStore.CONTEXT_MODE_SEARCH to "Suchen",
            SettingsStore.CONTEXT_MODE_RULE to "Regeln",
            SettingsStore.CONTEXT_MODE_SAVE to "Speichern",
            SettingsStore.CONTEXT_MODE_AUTO to "Automatisch"
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (mode, label) ->
                val active = selectedMode == mode
                val tint = if (active) Orange else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (active) Orange.copy(alpha = 0.22f) else if (isDark) DarkField else LightField)
                        .border(
                            1.dp,
                            if (active) Orange.copy(alpha = 0.52f) else if (isDark) DarkFieldBorder else LightFieldBorder,
                            RoundedCornerShape(11.dp)
                        )
                        .semantics {
                            contentDescription = label
                            role = Role.Button
                            selected = active
                        }
                        .clickable { onModeChange(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    when (mode) {
                        SettingsStore.CONTEXT_MODE_SEARCH ->
                            Icon(Icons.Default.Search, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        SettingsStore.CONTEXT_MODE_SAVE ->
                            Icon(Icons.Default.Save, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        SettingsStore.CONTEXT_MODE_RULE ->
                            Text("R", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = tint)
                        else ->
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MicActionButton(
    isRecording: Boolean,
    isTranscribing: Boolean,
    onClick: () -> Unit
) {
    val actionOrangeBg = Orange.copy(alpha = 0.20f)
    val actionOrangeBorder = Orange.copy(alpha = 0.42f)
    val recordingRed = Color(0xFFFF3B30)
    ActionButton(
        icon = {
            if (isTranscribing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(21.dp),
                    strokeWidth = 2.dp,
                    color = Amber
                )
            } else {
                Icon(Icons.Default.Mic, null, Modifier.size(21.dp))
            }
        },
        bg = if (isRecording) recordingRed.copy(alpha = 0.28f) else actionOrangeBg,
        border = if (isRecording) recordingRed else actionOrangeBorder,
        tint = if (isRecording) recordingRed else Orange,
        onClick = onClick
    )
}

@Composable
private fun ActionButton(
    icon: @Composable () -> Unit,
    bg: Color,
    border: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides tint) {
                ProvideTextStyle(LocalTextStyle.current.copy(color = tint)) {
                    icon()
                }
            }
        }
    }
}
