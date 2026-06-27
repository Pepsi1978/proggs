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
    val recordingTone = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }

    DisposableEffect(Unit) {
        onDispose { recordingTone.release() }
    }

    // Berechtigung für Mikrofon
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            recordingTone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header: "Heute" + "Neu"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Heute",
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.clickable { /* reset chat */ },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Default.AddComment, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(15.dp))
                Text("Neu", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

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
                    Text(
                        text = "\uD83E\uDDE0",
                        fontSize = 54.sp,
                        modifier = Modifier.scale(alpha)
                    )
                    Text(
                        text = "Dein Gehirn hört zu.",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Sag oder tippe etwas — ich lege es ab oder schlage es nach.",
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
            titleOverride = uiState.titleOverride,
            onTitleChange = vm::updateTitleOverride,
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
                        recordingTone.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
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
            onClear = { inputText = "" },
            onImprove = { vm.improveText(inputText) }
        )
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
            // Bot bubble: surface bg, glow when speaking
            val speaking = false // TODO: TTS state
            val borderColor = if (speaking) Orange.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outline

            Surface(
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = if (speaking) 8.dp else 0.dp
            ) {
                Box {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        fontSize = 14.5.sp,
                        lineHeight = 22.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Speaker icon (bottom-right, 26x26 circle)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 9.dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Vorlesen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
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
    titleOverride: String,
    onTitleChange: (String) -> Unit,
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
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title + Category row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Title, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = titleOverride,
                    onValueChange = { if (it.length <= 200) onTitleChange(it) },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
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
                        .defaultMinSize(minHeight = 44.dp)
                        .padding(12.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.5.sp,
                        lineHeight = 21.7.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
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
