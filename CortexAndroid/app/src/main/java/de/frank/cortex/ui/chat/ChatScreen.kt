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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.cortex.data.ChatSessionSummary
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.model.ChatOption
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager
import java.util.Locale

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val vpnState by WireGuardManager.state.collectAsStateWithLifecycle()
    var inputText by rememberSaveable { mutableStateOf("") }
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
    val recordingTone = remember(toneVolume) { ToneGenerator(AudioManager.STREAM_MUSIC, (toneVolume * 100).toInt()) }
    val emptyTitle = when (uiState.contextMode) {
        SettingsStore.CONTEXT_MODE_SMALLTALK -> "Smalltalk-Modus ist aktiv."
        SettingsStore.CONTEXT_MODE_SAVE -> "Speichermodus ist aktiv."
        SettingsStore.CONTEXT_MODE_SEARCH -> "Suchmodus ist aktiv."
        else -> "Dein Speicher hört zu."
    }
    val emptySubtitle = when (uiState.contextMode) {
        SettingsStore.CONTEXT_MODE_SMALLTALK -> "Sag oder tippe etwas — ich plaudere nur mit dir und speichere oder suche nichts."
        SettingsStore.CONTEXT_MODE_SAVE -> "Sag oder tippe etwas — ich behandle es als Information zum Ablegen."
        SettingsStore.CONTEXT_MODE_SEARCH -> "Frag oder tippe ein Thema — ich suche gezielt in deinem Gedächtnis."
        else -> "Sag oder tippe etwas — ich lege es ab oder schlage es nach."
    }

    fun playRecordingTone() {
        if (!SettingsStore.recordingToneEnabled) return
        try {
            recordingTone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
        } catch (e: Exception) {
            CortexLog.warn("ChatScreen", "recordingTone", "Aufnahmeton fehlgeschlagen: ${e.message}")
        }
    }

    DisposableEffect(recordingTone) {
        onDispose { recordingTone.release() }
    }

    // Berechtigung für Mikrofon
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            playRecordingTone()
            vm.toggleRecording()
        }
    }

    // Transkribierten Text ins Eingabefeld übernehmen
    LaunchedEffect(Unit) {
        vm.transcribedText.collect { text ->
            inputText = if (inputText.isBlank()) text else "$inputText $text"
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

    LaunchedEffect(uiState.messages.size) {
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
                onMicToggle = {
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
                    vm.sendMessage(inputText)
                    inputText = ""
                },
                onClear = {
                    inputText = ""
                    vm.updateTitleOverride("")
                },
                onImprove = { vm.improveText(inputText) }
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
        drawerContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f),
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
                    .height(12.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-36).dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Sessions schließen")
                }
            }

            // Der "Neuer Chat"-Button lebt in der Top-Bar rechts neben dem Sessions-Knopf —
            // hier war er doppelt und wurde entfernt (Frank-Wunsch 2026-07-02).
            HorizontalDivider(color = Iris.copy(alpha = 0.22f))

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

private val quotedStoreTextRegex = Regex("[„\"]([^„“\"]+)[“\"]")

private fun extractEditableStoreText(messageText: String): String =
    quotedStoreTextRegex.findAll(messageText).map { it.groupValues[1] }.lastOrNull()?.trim()
        ?: messageText.trim()

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
    val canEditStoreText = !message.isUser && message.action in setOf("save_confirm", "store_clarify")
    val initialEditableText = remember(message.text) { extractEditableStoreText(message.text) }
    var isEditingStoreText by rememberSaveable(message.id) { mutableStateOf(false) }
    var editedStoreText by rememberSaveable(message.id) { mutableStateOf(initialEditableText) }

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
                        Text(
                            text = "($wordCount Wörter)",
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Meta line
        if (!message.isUser && message.action != null) {
            val metaText = when (message.action) {
                "store" -> "\u21B3 abgelegt in \u201E${message.category ?: "Unkategorisiert"}\u201C"
                "recall" -> "\u21B3 nachgeschlagen \u00B7 ${message.recallHits ?: 0} Treffer"
                "save_confirm", "store_clarify" -> "\u21B3 Rückfrage\u2026"
                "cancel" -> "\u21B3 nicht gespeichert"
                else -> null
            }
            val metaColor = when (message.action) {
                "store" -> Mint
                "recall" -> Iris
                "save_confirm", "store_clarify" -> Amber
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
                        onOptionClick(option)
                    }
                }
                if (canEditStoreText) {
                    OptionChip(
                        label = if (isEditingStoreText) "Speichern" else "Editieren",
                        isDark = isDark,
                        tint = if (isEditingStoreText) Mint else Amber
                    ) {
                        if (isEditingStoreText) {
                            onEditedStoreSave(message, editedStoreText)
                            isEditingStoreText = false
                        } else {
                            editedStoreText = initialEditableText
                            isEditingStoreText = true
                        }
                    }
                }
            }
        }

        if (canEditStoreText && isEditingStoreText) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) DarkField else LightField,
                border = BorderStroke(1.dp, Amber.copy(alpha = 0.55f)),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                BasicTextField(
                    value = editedStoreText,
                    onValueChange = { editedStoreText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 96.dp)
                        .padding(12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 21.7.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    minLines = 3,
                    maxLines = 8
                )
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
    onMicToggle: () -> Unit,
    onSend: () -> Unit,
    onClear: () -> Unit,
    onImprove: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val actionOrangeBg = Orange.copy(alpha = 0.20f)
    val actionOrangeBorder = Orange.copy(alpha = 0.42f)
    val recordingRed = Color(0xFFFF3B30)

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

            // Title + Category row
            Row(
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

            Spacer(Modifier.height(8.dp))

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
                            Text("Ablegen oder nachschlagen\u2026", fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    }
                )
            }

            Spacer(Modifier.height(9.dp))

            // Action row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
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
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Mint.copy(alpha = 0.13f))
                        .border(1.dp, Mint.copy(alpha = 0.30f), RoundedCornerShape(11.dp))
                        .clickable(enabled = text.isNotBlank() && !isImproving, onClick = onImprove),
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

                Spacer(Modifier.weight(1f))

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
                    onClick = onMicToggle
                )

                // Send (44x44, radius 13, solid orange + shadow)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Orange)
                        .clickable(enabled = text.isNotBlank(), onClick = onSend),
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

@Composable
private fun ContextModeBar(
    selectedMode: String,
    onModeChange: (String) -> Unit,
    responseSize: String,
    onResponseSizeChange: (String) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        .width(if (size == SettingsStore.RESPONSE_SIZE_XL) 44.dp else 38.dp)
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
        // Router im Auto-Modus selbst — es bleiben Suchen, Speichern und Automatisch (KI).
        val items = listOf(
            Triple(SettingsStore.CONTEXT_MODE_SEARCH, Icons.Default.Search, "Suchen"),
            Triple(SettingsStore.CONTEXT_MODE_SAVE, Icons.Default.Save, "Speichern"),
            Triple(SettingsStore.CONTEXT_MODE_AUTO, Icons.Default.AutoAwesome, "Automatisch")
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (mode, icon, label) ->
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
                        .clickable { onModeChange(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
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
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) {
            ProvideTextStyle(LocalTextStyle.current.copy(color = tint)) {
                icon()
            }
        }
    }
}
