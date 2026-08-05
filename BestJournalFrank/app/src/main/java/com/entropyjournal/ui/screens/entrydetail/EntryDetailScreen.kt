package com.entropyjournal.ui.screens.entrydetail

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import com.entropyjournal.util.rememberHapticAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.entropyjournal.ui.components.AnimatedMicButton
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.theme.NeonAmber
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.ui.theme.LocalJournalDesignTokens
import com.entropyjournal.ui.theme.scaledBody
import com.entropyjournal.ui.screens.journal.RecordingState
import com.entropyjournal.ui.screens.journal.GoldenWaveform
import com.entropyjournal.util.DateTimeFormatter
import com.entropyjournal.util.TtsManager
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryDetailScreen(
    viewModel: EntryDetailViewModel,
    onBack: () -> Unit,
    searchQuery: String = "",
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val designTokens = LocalJournalDesignTokens.current
    val focusManager = LocalFocusManager.current
    val doHaptic = rememberHapticAction()
    val isDark = com.entropyjournal.ui.theme.LocalIsDarkTheme.current
    val highlightColor = if (isDark) Color(0x44FFFFFF) else Color(0xFFFFEB3B)
    val searchHighlight =
        if (searchQuery.isNotBlank()) {
            androidx.compose.ui.text.input.VisualTransformation { text ->
                androidx.compose.ui.text.input.TransformedText(
                    com.entropyjournal.ui.components.highlightMatches(
                        text.text,
                        searchQuery,
                        highlightColor,
                    ),
                    androidx.compose.ui.text.input.OffsetMapping.Identity,
                )
            }
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        }
    val focusRequester = remember { FocusRequester() }
    var lastEditTime by remember { mutableLongStateOf(0L) }
    var isFocused by remember { mutableStateOf(false) }

    var fullScreenPhotoPath by remember { mutableStateOf<String?>(null) }
    var fullScreenIsVideo by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSpeaking by remember { mutableStateOf(false) }
    var isTtsLoading by remember { mutableStateOf(false) }
    var pendingFollowUpMicStart by remember { mutableStateOf(false) }
    var pendingInlineFollowUpDeletion by remember {
        mutableStateOf<Pair<Long, Int>?>(null)
    }
    var cameraFile by remember { mutableStateOf<java.io.File?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val tts = remember { TtsManager(context) }
    val followUpAmplitude by viewModel.followUpAmplitude.collectAsStateWithLifecycle()
    val followUpDuration by viewModel.followUpDurationSeconds.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    val appContext = context.applicationContext
    val videoImageLoader = remember {
        coil3.ImageLoader.Builder(appContext)
            .components { add(coil3.video.VideoFrameDecoder.Factory()) }
            .build()
    }

    val photoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) { uris ->
            if (uris.isNotEmpty()) {
                viewModel.addMedia(uris)
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                cameraFile?.let { viewModel.onCameraPhotoTaken(it) }
            } else {
                cameraFile?.delete()
            }
            cameraFile = null
        }

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            granted ->
            if (granted) {
                val (uri, file) = viewModel.createCameraUri()
                cameraFile = file
                val intent =
                    android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        putExtra(android.provider.MediaStore.EXTRA_OUTPUT, uri)
                        putExtra("android.intent.extras.CAMERA_FACING", 0)
                        putExtra("android.intent.extras.LENS_FACING_FRONT", 0)
                        putExtra("android.intent.extra.USE_FRONT_CAMERA", false)
                    }
                cameraLauncher.launch(intent)
            }
        }

    val audioPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            granted ->
            if (granted && pendingFollowUpMicStart) {
                viewModel.toggleFollowUpRecording()
            }
            pendingFollowUpMicStart = false
        }

    val onFollowUpMicClick: () -> Unit = {
        doHaptic(HapticFeedbackType.LongPress)
        if (uiState.followUpRecordingState == RecordingState.RECORDING) {
            viewModel.toggleFollowUpRecording()
        } else {
            val hasPermission =
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.toggleFollowUpRecording()
            } else {
                pendingFollowUpMicStart = true
                audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(lastEditTime) {
        if (lastEditTime > 0 && isFocused) {
            delay(5000)
            focusManager.clearFocus()
        }
    }

    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (!imeVisible && isFocused) {
            focusManager.clearFocus()
            viewModel.saveNow()
        }
    }

    LaunchedEffect(uiState.followUpError) {
        uiState.followUpError?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearFollowUpError()
        }
    }

    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onBack() }

    Column(
        modifier =
            Modifier.fillMaxSize().background(designTokens.backgroundBrush).clickable(
                interactionSource =
                    remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) {
                focusManager.clearFocus()
            }
    ) {
        TopAppBar(
            title = {},
            navigationIcon = {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.padding(start = 12.dp).size(40.dp),
                    shape = CircleShape,
                    color = designTokens.chipBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        designTokens.chipBorder,
                    ),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            "Zur\u00fcck",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            actions = {
                Surface(
                    onClick = {
                        doHaptic(HapticFeedbackType.LongPress)
                        showShareDialog = true
                    },
                    modifier = Modifier.padding(end = 12.dp).size(40.dp),
                    shape = CircleShape,
                    color = designTokens.chipBackground,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        designTokens.chipBorder,
                    ),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Share,
                            "Eintrag teilen",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        uiState.entry?.let { entry ->
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (!entry.summary.isNullOrBlank()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        glowColor = MaterialTheme.colorScheme.primary,
                        glowIntensity = 0.1f,
                        cornerRadius = 18.dp,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Zusammenfassung",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            entry.summary
                                .lines()
                                .filter { it.trimStart().startsWith("\u2022") }
                                .forEachIndexed { index, line ->
                                    val bulletText = line.trimStart().removePrefix("\u2022").trim()
                                    Row(
                                        modifier = Modifier.padding(bottom = 6.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Box(
                                            Modifier.padding(top = 7.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (index % 3) {
                                                        0 -> MaterialTheme.colorScheme.primary
                                                        1 -> designTokens.warning
                                                        else -> designTokens.success
                                                    }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            bulletText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                        }
                    }
                }

                entry.entropyScore?.let { score ->
                    GlassCard(
                        cornerRadius = 18.dp,
                        glowColor =
                            when {
                                score < 0.33f -> NeonEmerald
                                score < 0.66f -> NeonAmber
                                else -> NeonRed
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Entropie-Score: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = String.format("%.0f%%", score * 100),
                                style = MaterialTheme.typography.titleMedium,
                                color =
                                    when {
                                        score < 0.33f -> NeonEmerald
                                        score < 0.66f -> NeonAmber
                                        else -> NeonRed
                                    },
                            )
                        }
                    }
                }

                val hasImproved = entry.isImproved && entry.improvedText != null
                val isShowingOriginal = selectedTab == 1 && hasImproved
                val toggleSpeech: () -> Unit = {
                    doHaptic(HapticFeedbackType.LongPress)
                    if (isSpeaking || isTtsLoading) {
                        tts.stop()
                        isSpeaking = false
                        isTtsLoading = false
                    } else {
                        isTtsLoading = true
                        isSpeaking = true
                        val baseText = if (isShowingOriginal) entry.rawText else entry.displayText
                        val speakText =
                            buildString {
                                append(baseText)
                                uiState.followUps.forEachIndexed { index, followUp ->
                                    append("\n\nNachtrag ")
                                    append(germanNumberWord(index + 1))
                                    append(". ")
                                    append(followUp.text)
                                }
                            }
                        val started =
                            tts.speak(
                                speakText,
                                onPlaybackStart = { isTtsLoading = false },
                            ) {
                                isSpeaking = false
                                isTtsLoading = false
                            }
                        if (!started) {
                            isSpeaking = false
                            isTtsLoading = false
                            android.widget.Toast.makeText(
                                    context,
                                    "Stimmen in den Einstellungen einschalten",
                                    android.widget.Toast.LENGTH_SHORT,
                                )
                                .show()
                        }
                    }
                }

                if (hasImproved) {
                    GoldenDetailTabs(selectedIndex = selectedTab, onSelected = { selectedTab = it })
                }

                val textFieldColors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    )

                GlassCard(
                    modifier =
                        if (hasImproved)
                            Modifier.pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount < -40) selectedTab = 1
                                    if (dragAmount > 40) selectedTab = 0
                                }
                            }
                        else Modifier,
                    glowColor = MaterialTheme.colorScheme.primary,
                    glowIntensity = 0.12f,
                    cornerRadius = 18.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Tagebucheintrag",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.showDeleteDialog(true)
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Tagebucheintrag l\u00f6schen",
                                    tint = NeonRed,
                                )
                            }
                        }
                        // Date + time + relative time — aligned like the follow-up timestamp:
                        // starts at the Book icon X-position (plus ~1mm right) and pulled up ~2mm.
                        Text(
                            "${DateTimeFormatter.formatFull(entry.timestamp)} \u00b7 ${DateTimeFormatter.formatRelative(entry.timestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 4.dp).offset(y = (-8).dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedContent(
                            targetState = isShowingOriginal,
                            transitionSpec = {
                                if (targetState) {
                                    slideInHorizontally { it } togetherWith
                                        slideOutHorizontally { -it }
                                } else {
                                    slideInHorizontally { -it } togetherWith
                                        slideOutHorizontally { it }
                                }
                            },
                            label = "tab_slide",
                        ) { showOriginal ->
                            if (showOriginal) {
                                var editedRawText by
                                    remember(entry.rawText) { mutableStateOf(entry.rawText) }
                                TextField(
                                    value = editedRawText,
                                    onValueChange = { newText ->
                                        editedRawText = newText
                                        lastEditTime = System.currentTimeMillis()
                                        viewModel.updateRawText(newText)
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .onFocusChanged { state ->
                                                isFocused = state.isFocused
                                                if (state.isFocused)
                                                    lastEditTime = System.currentTimeMillis()
                                            },
                                    textStyle =
                                        MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 15.sp.scaledBody,
                                            lineHeight = 24.sp.scaledBody,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                    colors = textFieldColors,
                                    visualTransformation = searchHighlight,
                                )
                            } else {
                                TextField(
                                    value = uiState.editedDisplayText,
                                    onValueChange = {
                                        lastEditTime = System.currentTimeMillis()
                                        viewModel.updateDisplayText(it)
                                    },
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .focusRequester(focusRequester)
                                            .onFocusChanged { state ->
                                                isFocused = state.isFocused
                                                if (state.isFocused)
                                                    lastEditTime = System.currentTimeMillis()
                                            },
                                    textStyle =
                                        MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 15.sp.scaledBody,
                                            lineHeight = 24.sp.scaledBody,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                    colors = textFieldColors,
                                    visualTransformation = searchHighlight,
                                )
                            }
                        }
                        if (uiState.isSaving) {
                            Text(
                                "Wird gespeichert...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (!hasImproved) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.improveTextWithAi() },
                                enabled = !uiState.isImproving,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                if (uiState.isImproving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Wird verbessert\u2026",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Mit KI nachtr\u00e4glich verbessern",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                            uiState.improveError?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    error,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonRed,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = toggleSpeech,
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = designTokens.chipBackground,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    designTokens.chipBorder,
                                ),
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                        contentDescription = if (isSpeaking) "Stoppen" else "Vorlesen",
                                        tint = designTokens.warning,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Aufnahmedauer: ${DateTimeFormatter.formatDuration(entry.audioDurationSeconds)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = designTokens.textMuted,
                            )
                        }
                        if (isTtsLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Text-to-Speech wird erzeugt…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                uiState.followUps.forEachIndexed { index, followUp ->
                    val fuHasImproved =
                        followUp.isImproved && !followUp.improvedText.isNullOrBlank()
                    var selectedTabFu by
                        remember(followUp.id, fuHasImproved) { mutableIntStateOf(0) }
                    val isShowingOriginalFu = selectedTabFu == 1 && fuHasImproved

                    val fuFieldColors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        )

                    GlassCard(
                        modifier =
                            Modifier.fillMaxWidth().then(
                                if (fuHasImproved)
                                    Modifier.pointerInput(followUp.id) {
                                        detectHorizontalDragGestures { _, dragAmount ->
                                            // Local UI state only — same mechanic as
                                            // the main entry: the swipe just flips the
                                            // visible tab, it does NOT change the stored
                                            // isImproved flag.
                                            if (dragAmount < -40) selectedTabFu = 1
                                            if (dragAmount > 40) selectedTabFu = 0
                                        }
                                    }
                                else Modifier
                            ),
                        glowColor = designTokens.warning,
                        glowIntensity = 0.1f,
                        cornerRadius = 18.dp,
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // ── Header row: title + delete ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.EditNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = designTokens.warning,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Nachtrag ${germanNumberWord(index + 1)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = designTokens.warning,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        pendingInlineFollowUpDeletion =
                                            followUp.id to (index + 1)
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Nachtrag löschen",
                                        tint = NeonRed,
                                    )
                                }
                            }
                            // ── Date + time + relative time row (thin, under title) ──
                            // Aligned to the Book icon's X-position (plus ~1mm right),
                            // pulled up ~2mm to sit close under the title row.
                            Text(
                                "${DateTimeFormatter.formatFull(followUp.createdAt)} · ${DateTimeFormatter.formatRelative(followUp.updatedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier =
                                    Modifier.padding(start = 4.dp).offset(y = (-8).dp),
                            )

                            if (fuHasImproved) {
                                GoldenDetailTabs(
                                    selectedIndex = selectedTabFu,
                                    onSelected = { selectedTabFu = it },
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            AnimatedContent(
                                targetState = isShowingOriginalFu,
                                transitionSpec = {
                                    if (targetState) {
                                        slideInHorizontally { it } togetherWith
                                            slideOutHorizontally { -it }
                                    } else {
                                        slideInHorizontally { -it } togetherWith
                                            slideOutHorizontally { it }
                                    }
                                },
                                label = "followup_tab_slide",
                            ) { showOriginal ->
                                if (showOriginal) {
                                    TextField(
                                        value = followUp.rawText,
                                        onValueChange = {
                                            viewModel.updateInlineFollowUpRaw(followUp.id, it)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle =
                                            MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                        colors = fuFieldColors,
                                    )
                                } else {
                                    val displayValue =
                                        if (fuHasImproved)
                                            (followUp.improvedText ?: followUp.text)
                                        else followUp.rawText
                                    TextField(
                                        value = displayValue,
                                        onValueChange = {
                                            if (fuHasImproved)
                                                viewModel.updateInlineFollowUpImproved(
                                                    followUp.id,
                                                    it,
                                                )
                                            else
                                                viewModel.updateInlineFollowUpRaw(followUp.id, it)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle =
                                            MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                        colors = fuFieldColors,
                                    )
                                }
                            }

                            if (!fuHasImproved && followUp.rawText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { viewModel.improveInlineFollowUp(followUp.id) },
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Mit KI nachträglich verbessern",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                // Gallery card: shows photos/videos under the main entry (or
                // under the last follow-up when any exist). Only rendered when
                // at least one item exists. The separate add-media card below
                // is purely a control surface so the button always sits at the
                // very bottom of the entry stack.
                if (uiState.photos.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Fotos und Videos",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.photos, key = { it.id }) { photo ->
                                    Box {
                                        AsyncImage(
                                            model = File(photo.filePath),
                                            contentDescription =
                                                if (photo.isVideo) "Video" else "Foto",
                                            imageLoader =
                                                if (photo.isVideo) videoImageLoader
                                                else coil3.ImageLoader(appContext),
                                            modifier =
                                                Modifier.size(74.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .clickable {
                                                        fullScreenPhotoPath = photo.filePath
                                                        fullScreenIsVideo = photo.isVideo
                                                    },
                                            contentScale = ContentScale.Crop,
                                        )
                                        if (photo.isVideo) {
                                            Icon(
                                                Icons.Rounded.PlayCircle,
                                                contentDescription = "Video abspielen",
                                                modifier =
                                                    Modifier.size(40.dp).align(Alignment.Center),
                                                tint = Color.White.copy(alpha = 0.9f),
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deletePhoto(photo.id) },
                                            modifier =
                                                Modifier.align(Alignment.TopEnd)
                                                    .size(28.dp)
                                                    .background(
                                                        Color.Black.copy(alpha = 0.5f),
                                                        CircleShape,
                                                    ),
                                        ) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Entfernen",
                                                modifier = Modifier.size(16.dp),
                                                tint = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = designTokens.warning,
                    glowIntensity = 0.1f,
                    cornerRadius = 18.dp,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Nachtrag",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Button(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.openNewFollowUpDialog()
                                },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    "Hinzufügen",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }

                // Add-media card: always at the bottom of the entry stack,
                // purely a control surface with the add button.
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Fotos/Videos hinzufügen",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Button(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                showPhotoSourceDialog = true
                            },
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                "Hinzufügen",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }

                if (!entry.adviceCategoryTags.isNullOrBlank()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        entry.adviceCategoryTags
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        tag,
                                        modifier =
                                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = {
                            doHaptic(HapticFeedbackType.LongPress)
                            viewModel.openNewFollowUpDialog()
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = designTokens.chipBackground,
                        border =
                            androidx.compose.foundation.BorderStroke(
                                1.dp,
                                designTokens.chipBorder,
                            ),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(19.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Schreiben", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        onClick = {
                            viewModel.openNewFollowUpDialog()
                            onFollowUpMicClick()
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shadowElevation = 6.dp,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(19.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Einsprechen",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (uiState.showFollowUpDialog) {
        FollowUpDialog(
            rawText = uiState.followUpDraftText,
            improvedText = uiState.followUpImprovedText,
            recordingState = uiState.followUpRecordingState,
            amplitude = followUpAmplitude,
            durationSeconds = followUpDuration,
            isImproving = uiState.followUpRecordingState == RecordingState.IMPROVING,
            isUsingImproved = uiState.isUsingImprovedFollowUp,
            canDelete = uiState.activeFollowUpId != null,
            engineLabel =
                if (viewModel.isGroqActive()) "Groq API" else "Lokales Whisper-Modell",
            onImproveClick = { viewModel.improveFollowUp() },
            onToggleVersion = { useImproved -> viewModel.setUseImprovedFollowUp(useImproved) },
            onTextEdit = { viewModel.updateFollowUpDraft(it) },
            onSave = { viewModel.saveFollowUp() },
            onDismiss = { viewModel.dismissFollowUpDialog() },
            onRecordClick = onFollowUpMicClick,
            onDeleteClick = { viewModel.requestFollowUpDeletion() },
        )
    }

    if (uiState.showDeleteFollowUpDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteFollowUpDialog(false) },
            modifier =
                Modifier.border(
                    1.dp,
                    designTokens.chipBorder,
                    RoundedCornerShape(22.dp),
                ),
            shape = RoundedCornerShape(22.dp),
            containerColor = designTokens.card,
            title = { Text("Nachtrag löschen?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Möchtest du diesen Nachtrag wirklich löschen?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        doHaptic(HapticFeedbackType.LongPress)
                        viewModel.deleteActiveFollowUp()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.showDeleteFollowUpDialog(false) }) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    pendingInlineFollowUpDeletion?.let { (followUpId, number) ->
        val nachtragTitle = "Nachtrag ${germanNumberWord(number)}"
        AlertDialog(
            onDismissRequest = { pendingInlineFollowUpDeletion = null },
            modifier =
                Modifier.border(
                    1.dp,
                    designTokens.chipBorder,
                    RoundedCornerShape(22.dp),
                ),
            shape = RoundedCornerShape(22.dp),
            containerColor = designTokens.card,
            title = {
                Text("$nachtragTitle löschen?", color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "Möchtest du $nachtragTitle wirklich unwiderruflich löschen?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        doHaptic(HapticFeedbackType.LongPress)
                        viewModel.deleteInlineFollowUp(followUpId)
                        pendingInlineFollowUpDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                ) {
                    Text("Ja")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingInlineFollowUpDeletion = null }) {
                    Text("Nein", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    // Full-screen photo viewer with pinch-to-zoom and horizontal paging
    fullScreenPhotoPath?.let { initialPath ->
        val photos = uiState.photos
        val initialPage = photos.indexOfFirst { it.filePath == initialPath }.coerceAtLeast(0)
        val pagerState = rememberPagerState(initialPage = initialPage) { photos.size }
        var currentPageZoomed by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { fullScreenPhotoPath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = !currentPageZoomed,
                ) { page ->
                    var scale by remember { mutableStateOf(1f) }
                    var offsetX by remember { mutableStateOf(0f) }
                    var offsetY by remember { mutableStateOf(0f) }

                    LaunchedEffect(scale, pagerState.currentPage) {
                        if (page == pagerState.currentPage) {
                            currentPageZoomed = scale > 1f
                        }
                    }

                    LaunchedEffect(pagerState.currentPage) {
                        if (page != pagerState.currentPage) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }

                    Box(
                        modifier =
                            Modifier.fillMaxSize()
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        do {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.count { it.pressed }
                                            if (pressed >= 2) {
                                                val zoom = event.calculateZoom()
                                                val pan = event.calculatePan()
                                                scale = (scale * zoom).coerceIn(1f, 5f)
                                                if (scale > 1f) {
                                                    offsetX += pan.x
                                                    offsetY += pan.y
                                                    val maxX = 1000f * (scale - 1)
                                                    val maxY = 1500f * (scale - 1)
                                                    offsetX = offsetX.coerceIn(-maxX, maxX)
                                                    offsetY = offsetY.coerceIn(-maxY, maxY)
                                                } else {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                                event.changes.forEach { it.consume() }
                                            } else if (pressed == 1 && scale > 1f) {
                                                val pan = event.calculatePan()
                                                offsetX += pan.x
                                                offsetY += pan.y
                                                val maxX = 1000f * (scale - 1)
                                                val maxY = 1500f * (scale - 1)
                                                offsetX = offsetX.coerceIn(-maxX, maxX)
                                                offsetY = offsetY.coerceIn(-maxY, maxY)
                                                event.changes.forEach { it.consume() }
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1f) {
                                                scale = 1f
                                                offsetX = 0f
                                                offsetY = 0f
                                            } else {
                                                scale = 2.5f
                                            }
                                        },
                                        onTap = { fullScreenPhotoPath = null },
                                    )
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (photos[page].isVideo) {
                            androidx.compose.ui.viewinterop.AndroidView(
                                factory = { ctx ->
                                    android.widget.VideoView(ctx).apply {
                                        setVideoPath(photos[page].filePath)
                                        setMediaController(
                                            android.widget.MediaController(ctx).also {
                                                it.setAnchorView(this)
                                            }
                                        )
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = false
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            AsyncImage(
                                model = File(photos[page].filePath),
                                contentDescription = "Foto ${page + 1}",
                                modifier =
                                    Modifier.fillMaxSize().graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    },
                                contentScale = ContentScale.Fit,
                            )
                        }
                    }
                }

                if (photos.size > 1) {
                    Text(
                        "${pagerState.currentPage + 1} / ${photos.size}",
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp),
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                IconButton(
                    onClick = { fullScreenPhotoPath = null },
                    modifier =
                        Modifier.align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Schlie\u00dfen",
                        tint = Color.White,
                    )
                }
            }
        }
    }

    if (showPhotoSourceDialog) {
        Dialog(onDismissRequest = { showPhotoSourceDialog = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Foto/Video hinzuf\u00fcgen",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    val infiniteTransition = rememberInfiniteTransition(label = "photo_source")
                    val flow by
                        infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(3000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                            label = "color_flow",
                        )
                    val flow2 by
                        infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0f,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(2200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                            label = "color_flow2",
                        )
                    val iconScale by
                        infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.15f,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                            label = "icon_pulse",
                        )
                    val tileScale by
                        infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.03f,
                            animationSpec =
                                infiniteRepeatable(
                                    animation = tween(2000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                            label = "tile_breathe",
                        )

                    val tilePrimary = MaterialTheme.colorScheme.primary
                    val tileSecondary = MaterialTheme.colorScheme.primaryContainer
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier =
                                Modifier.weight(1f)
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        scaleX = tileScale
                                        scaleY = tileScale
                                    }
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors =
                                                listOf(
                                                    lerp(
                                                        if (isDark) tilePrimary else Color(0xFF1565C0),
                                                        if (isDark) tileSecondary else Color(0xFFBDBDBD),
                                                        flow,
                                                    ),
                                                    lerp(
                                                        if (isDark) tilePrimary.copy(alpha = 0.8f) else Color(0xFF1976D2),
                                                        if (isDark) tilePrimary else Color(0xFF1565C0),
                                                        flow2,
                                                    ),
                                                    lerp(if (isDark) tileSecondary else Color(0xFFBDBDBD), if (isDark) tilePrimary.copy(alpha = 0.8f) else Color(0xFF1976D2), flow),
                                                ),
                                            start = Offset(0f, 300f * flow),
                                            end = Offset(300f, 300f * (1f - flow)),
                                        )
                                    )
                                    .clickable {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        showPhotoSourceDialog = false
                                        cameraPermissionLauncher.launch(
                                            android.Manifest.permission.CAMERA
                                        )
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.CameraAlt,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(40.dp).graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        },
                                    tint = Color.White,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Kamera",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                )
                            }
                        }
                        Box(
                            modifier =
                                Modifier.weight(1f)
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        scaleX = tileScale
                                        scaleY = tileScale
                                    }
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors =
                                                listOf(
                                                    lerp(
                                                        if (isDark) tilePrimary.copy(alpha = 0.9f) else Color(0xFF0D47A1),
                                                        if (isDark) tileSecondary else Color(0xFFBDBDBD),
                                                        flow2,
                                                    ),
                                                    lerp(
                                                        if (isDark) tilePrimary.copy(alpha = 0.7f) else Color(0xFF1565C0),
                                                        if (isDark) tilePrimary.copy(alpha = 0.9f) else Color(0xFF0D47A1),
                                                        flow,
                                                    ),
                                                    lerp(
                                                        if (isDark) tileSecondary else Color(0xFFBDBDBD),
                                                        if (isDark) tilePrimary.copy(alpha = 0.7f) else Color(0xFF1565C0),
                                                        flow2,
                                                    ),
                                                ),
                                            start = Offset(300f * flow2, 0f),
                                            end = Offset(300f * (1f - flow2), 300f),
                                        )
                                    )
                                    .clickable {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        showPhotoSourceDialog = false
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia
                                                    .ImageAndVideo
                                            )
                                        )
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.size(40.dp).graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        },
                                    tint = Color.White,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Galerie",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        uiState.entry?.let { entry ->
            ShareEntryDialog(
                entry = entry,
                followUps = uiState.followUps,
                photos = uiState.photos,
                context = context,
                useImproved = selectedTab == 0,
                onDismiss = { showShareDialog = false },
            )
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            modifier =
                Modifier.border(
                    1.dp,
                    designTokens.chipBorder,
                    RoundedCornerShape(22.dp),
                ),
            shape = RoundedCornerShape(22.dp),
            containerColor = designTokens.card,
            title = { Text("Eintrag l\u00f6schen?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Diesen Eintrag unwiderruflich l\u00f6schen?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        doHaptic(HapticFeedbackType.LongPress)
                        viewModel.deleteEntry()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                ) {
                    Text("L\u00f6schen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.showDeleteDialog(false) }) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

@Composable
private fun FollowUpDialog(
    rawText: String,
    improvedText: String?,
    recordingState: RecordingState,
    amplitude: Float,
    durationSeconds: Int,
    isImproving: Boolean,
    isUsingImproved: Boolean,
    canDelete: Boolean,
    engineLabel: String,
    onImproveClick: () -> Unit,
    onToggleVersion: (Boolean) -> Unit,
    onTextEdit: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRecordClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val designTokens = LocalJournalDesignTokens.current
    val isRecording = recordingState == RecordingState.RECORDING
    val isTranscribing = recordingState == RecordingState.TRANSCRIBING
    val showingImproved = isUsingImproved && improvedText != null
    val displayText = if (showingImproved) improvedText!! else rawText
    var inputModeChosen by remember(rawText) { mutableStateOf(rawText.isNotBlank()) }
    val dialogFocusManager = LocalFocusManager.current
    val dialogFocusRequester = remember { FocusRequester() }
    var dialogLastEditTime by remember { mutableLongStateOf(0L) }
    var dialogIsFocused by remember { mutableStateOf(false) }

    LaunchedEffect(inputModeChosen, rawText, isRecording, isTranscribing) {
        if (inputModeChosen && rawText.isBlank() && !isRecording && !isTranscribing) {
            delay(300)
            dialogFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(dialogLastEditTime) {
        if (dialogLastEditTime > 0 && dialogIsFocused) {
            delay(5000)
            dialogFocusManager.clearFocus()
        }
    }

    val dialogImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(dialogImeVisible) {
        if (!dialogImeVisible && dialogIsFocused) {
            dialogFocusManager.clearFocus()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isRecording && !isTranscribing) {
                dialogFocusManager.clearFocus()
                onDismiss()
            }
        },
        modifier =
            Modifier.border(
                1.dp,
                designTokens.chipBorder,
                RoundedCornerShape(22.dp),
            ),
        shape = RoundedCornerShape(22.dp),
        containerColor = designTokens.card,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Book,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Nachtrag",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (canDelete && !isRecording && !isTranscribing) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Nachtrag löschen",
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        },
        text = {
            Column {
                when {
                    isTranscribing -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                "Nachtrag wird transkribiert…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    isRecording -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Sprich deinen Nachtrag ein.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                color =
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        DateTimeFormatter.formatDuration(durationSeconds),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    GoldenWaveform(amplitude = amplitude)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        engineLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            AnimatedMicButton(
                                isRecording = true,
                                onClick = onRecordClick,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tippe auf Stop, wenn du fertig bist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    !inputModeChosen -> {
                    Text(
                        "Wie möchtest du deinen Nachtrag hinzufügen?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FloatingActionButton(
                                onClick = { inputModeChosen = true },
                                modifier = Modifier.size(56.dp),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape,
                                elevation =
                                    FloatingActionButtonDefaults.elevation(
                                        defaultElevation = 8.dp,
                                    ),
                            ) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = "Schreiben",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Schreiben",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))
                        Box(
                            modifier =
                                Modifier.height(32.dp)
                                    .width(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                        Spacer(modifier = Modifier.width(16.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedMicButton(
                                isRecording = false,
                                onClick = onRecordClick,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Einsprechen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Dein Nachtrag:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Surface(
                                onClick = onRecordClick,
                                shape = RoundedCornerShape(20.dp),
                                color =
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                ) {
                                    Icon(
                                        Icons.Rounded.Mic,
                                        contentDescription = "Einsprechen",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "Einsprechen",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextField(
                            value = displayText,
                            onValueChange = { newText ->
                                dialogLastEditTime = System.currentTimeMillis()
                                onTextEdit(newText)
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 300.dp)
                                    .focusRequester(dialogFocusRequester)
                                    .onFocusChanged { state ->
                                        dialogIsFocused = state.isFocused
                                        if (state.isFocused) {
                                            dialogLastEditTime = System.currentTimeMillis()
                                        }
                                    },
                            textStyle =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                ),
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                ),
                            placeholder = {
                                Text(
                                    "Schreibe hier deinen Nachtrag…",
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            },
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (improvedText != null && !isImproving) {
                            OutlinedButton(
                                onClick = { onToggleVersion(!showingImproved) },
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) {
                                Text(
                                    if (showingImproved) "↩ Original anzeigen"
                                    else "✨ Verbesserte Version anzeigen",
                                )
                            }
                        }

                        if (improvedText == null && !isImproving && displayText.isNotBlank()) {
                            Button(
                                onClick = onImproveClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor =
                                            MaterialTheme.colorScheme.onSecondaryContainer,
                                    ),
                            ) {
                                Text("✨ Text verbessern")
                            }
                        }

                        if (isImproving) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text(
                                    "KI verbessert den Nachtrag…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (displayText.isNotBlank() && !isRecording && !isTranscribing) {
                Button(onClick = onSave) {
                    Text(if (showingImproved) "Verbessert speichern" else "Speichern")
                }
            }
        },
        dismissButton = {
            if (!isRecording && !isTranscribing) {
                OutlinedButton(onClick = onDismiss) {
                    Text(
                        if (displayText.isBlank()) "Abbrechen" else "Verwerfen",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

@Composable
private fun GoldenDetailTabs(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val designTokens = LocalJournalDesignTokens.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(999.dp),
        color = designTokens.chipBackground,
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                designTokens.cardBorder,
            ),
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            listOf("Verbessert", "Original").forEachIndexed { index, label ->
                Box(
                    modifier =
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (selectedIndex == index) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { onSelected(index) }
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color =
                            if (selectedIndex == index) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// Spells a positive integer as a capitalised German word for display in
// the yellow follow-up label ("Nachtrag Eins", "Nachtrag Zwei", ...).
// Falls back to digits for values above 99.
private fun germanNumberWord(n: Int): String {
    if (n < 1 || n > 99) return n.toString()
    val ones = listOf(
        "", "eins", "zwei", "drei", "vier", "fünf",
        "sechs", "sieben", "acht", "neun",
    )
    val teens = listOf(
        "zehn", "elf", "zwölf", "dreizehn", "vierzehn",
        "fünfzehn", "sechzehn", "siebzehn", "achtzehn", "neunzehn",
    )
    val tens = listOf(
        "", "", "zwanzig", "dreißig", "vierzig", "fünfzig",
        "sechzig", "siebzig", "achtzig", "neunzig",
    )
    val word = when {
        n < 10 -> ones[n]
        n < 20 -> teens[n - 10]
        n % 10 == 0 -> tens[n / 10]
        else -> {
            val o = n % 10
            val onesPart = if (o == 1) "ein" else ones[o]
            "${onesPart}und${tens[n / 10]}"
        }
    }
    return word.replaceFirstChar { it.uppercase() }
}
