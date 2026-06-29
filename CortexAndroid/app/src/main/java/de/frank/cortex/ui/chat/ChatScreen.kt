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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.frank.cortex.data.SettingsStore
import de.frank.cortex.data.model.ChatOption
import de.frank.cortex.observability.CortexLog
import de.frank.cortex.ui.theme.*
import de.frank.cortex.vpn.TunnelState
import de.frank.cortex.vpn.WireGuardManager

@Composable
fun ChatScreen(vm: ChatViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val vpnState by WireGuardManager.state.collectAsState()
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
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
                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(message = message, onOptionClick = vm::sendOption)
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

@Composable
private fun ChatBubble(message: ChatMessage, onOptionClick: (ChatOption) -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == DarkBg

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
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    fontSize = 14.5.sp,
                    lineHeight = 21.7.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    fontSize = 14.5.sp,
                    lineHeight = 22.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Iris.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, if (isDark) DarkUserBorder else LightUserBorder)
                    ) {
                        Text(
                            text = option.label,
                            modifier = Modifier
                                .clickable { onOptionClick(option) }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Iris
                        )
                    }
                }
            }
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

                // TTS toggle (38x38, orange when on)
                ActionButton(
                    icon = {
                        Icon(
                            if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp
                            else Icons.AutoMirrored.Filled.VolumeOff,
                            null, Modifier.size(21.dp)
                        )
                    },
                    bg = actionOrangeBg,
                    border = actionOrangeBorder,
                    tint = Orange,
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
    isDark: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf(
            Triple(SettingsStore.CONTEXT_MODE_SEARCH, Icons.Default.Search, "Suchen"),
            Triple(SettingsStore.CONTEXT_MODE_SAVE, Icons.Default.Save, "Speichern"),
            Triple(SettingsStore.CONTEXT_MODE_SMALLTALK, Icons.Default.Forum, "Smalltalk"),
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
