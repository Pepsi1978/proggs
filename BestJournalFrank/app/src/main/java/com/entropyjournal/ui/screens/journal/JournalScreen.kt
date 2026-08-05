package com.entropyjournal.ui.screens.journal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import com.entropyjournal.util.rememberHapticAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.components.AnimatedMicButton
import com.entropyjournal.ui.components.ShimmerLoadingEffect
import com.entropyjournal.ui.components.SunMoonToggle
import com.entropyjournal.ui.components.TimelineItem
import com.entropyjournal.ui.components.TimelinePosition
import com.entropyjournal.ui.theme.NeonAmber
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.ui.theme.LocalJournalDesignTokens
import com.entropyjournal.ui.theme.scaledHeading
import com.entropyjournal.util.DateTimeFormatter as DTFormatter
import kotlinx.coroutines.delay

@Composable
fun JournalScreen(viewModel: JournalViewModel, onEntryClick: (Long, String) -> Unit) {
    val allEntries by viewModel.entries.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by
        viewModel.searchEntries(uiState.searchQuery).collectAsStateWithLifecycle(initialValue = emptyList())
    val entries =
        if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
            searchResults
        } else {
            allEntries
        }
    val amplitude by viewModel.amplitude.collectAsStateWithLifecycle()
    val duration by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val doHaptic = rememberHapticAction()
    val context = LocalContext.current
    val designTokens = LocalJournalDesignTokens.current

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            if (isGranted) {
                viewModel.toggleRecording()
            }
        }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val onMicClick: () -> Unit = {
        doHaptic(HapticFeedbackType.LongPress)
        if (uiState.recordingState == RecordingState.RECORDING) {
            viewModel.toggleRecording()
        } else {
            val hasPermission =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.toggleRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    var showSyncLegend by remember { mutableStateOf(false) }
    var showStreakDialog by remember { mutableStateOf(false) }

    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Auto-focus search field when search opens
    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            delay(100)
            searchFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(designTokens.backgroundBrush)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sync status + search toggle
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tagebuch",
                            style =
                                MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 27.sp.scaledHeading,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SunMoonToggle()
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                if (uiState.syncStatus == SyncStatus.ERROR) {
                                    viewModel.retrySyncNow()
                                } else {
                                    showSyncLegend = true
                                }
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector =
                                        when (uiState.syncStatus) {
                                            SyncStatus.ERROR -> Icons.Rounded.CloudOff
                                            SyncStatus.NOT_SIGNED_IN -> Icons.Rounded.CloudOff
                                            SyncStatus.SYNCING -> Icons.Rounded.Cloud
                                            SyncStatus.UPLOADING -> Icons.Filled.CloudUpload
                                            SyncStatus.DOWNLOADING -> Icons.Filled.CloudDownload
                                            else -> Icons.Rounded.CloudDone
                                        },
                                    contentDescription = "Sync-Status",
                                    tint =
                                        when (uiState.syncStatus) {
                                            SyncStatus.SYNCING -> NeonCyan
                                            SyncStatus.UPLOADING -> NeonCyan
                                            SyncStatus.DOWNLOADING -> NeonCyan
                                            SyncStatus.ERROR -> NeonRed
                                            SyncStatus.NOT_SIGNED_IN -> Color(0xFFFF9800)
                                            else -> NeonEmerald
                                        },
                                    modifier = Modifier.size(20.dp),
                                )
                                if (
                                    (uiState.syncStatus == SyncStatus.DOWNLOADING ||
                                        uiState.syncStatus == SyncStatus.UPLOADING) &&
                                        uiState.downloadTotal > 0
                                ) {
                                    Text(
                                        text =
                                            "${uiState.downloadCurrent}/${uiState.downloadTotal}",
                                        fontSize = 9.sp,
                                        color = NeonCyan,
                                        lineHeight = 10.sp,
                                    )
                                }
                            }
                        }
                        Surface(
                            onClick = { viewModel.toggleSearch() },
                            shape = RoundedCornerShape(20.dp),
                            color = designTokens.chipBackground,
                            border =
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    designTokens.chipBorder,
                                ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    "Suchen",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Suche",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                GoldenHairline(modifier = Modifier.padding(top = 10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        text = "${allEntries.size} Einträge",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (uiState.currentStreak > 0) {
                        val streakColor =
                            if (uiState.currentStreak > 7) NeonAmber
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier.background(
                                        streakColor.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .border(
                                        1.dp,
                                        streakColor.copy(alpha = 0.4f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable { doHaptic(HapticFeedbackType.LongPress); showStreakDialog = true }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "Tage in Folge",
                                tint = streakColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${uiState.currentStreak} Tage",
                                style = MaterialTheme.typography.labelSmall,
                                color = streakColor,
                            )
                        }
                    }
                }
            }

            // Transcribing state — no visual indicator needed (transcription is near-instant)

            if (entries.isEmpty() && uiState.recordingState == RecordingState.IDLE) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Noch keine Eintr\u00e4ge",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tippe auf das Mikrofon um zu starten",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                // Group entries by time period
                val groupedEntries =
                    remember(entries) {
                        entries.groupBy { DTFormatter.getSectionLabel(it.timestamp) }
                    }

                // Fixed search bar (does not scroll with entries)
                AnimatedVisibility(visible = uiState.isSearchActive) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                "Eintr\u00e4ge durchsuchen...",
                                color = MaterialTheme.colorScheme.outline,
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 4.dp)
                                .border(
                                    1.dp,
                                    designTokens.chipBorder,
                                    RoundedCornerShape(14.dp),
                                )
                                .focusRequester(searchFocusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions =
                            KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = designTokens.card,
                                unfocusedContainerColor = designTokens.card,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    "Suche schlie\u00dfen",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                ) {
                    // Daily writing prompt banner — always visible, no animation
                    if (uiState.showPromptBanner && uiState.dailyPromptText.isNotBlank()) {
                        item(key = "writing_prompt") {
                            WritingPromptBanner(
                                promptText = uiState.dailyPromptText,
                                promptCategory = uiState.dailyPromptCategory,
                                onWriteClick = {
                                    viewModel.startPromptEntry(uiState.dailyPromptText)
                                },
                                onDismiss = { viewModel.dismissPromptBanner() },
                            )
                        }
                    }

                    groupedEntries.forEach { (sectionLabel, sectionEntries) ->
                        // Section header
                        item(key = "header_$sectionLabel") {
                            Row(
                                modifier = Modifier.animateItem().padding(top = 10.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = sectionLabel,
                                    style =
                                        MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 15.sp.scaledHeading,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Box(
                                    modifier =
                                        Modifier.weight(1f)
                                            .height(1.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                                        Color.Transparent,
                                                    )
                                                )
                                            )
                                )
                            }
                        }

                        // Entries with timeline position
                        items(count = sectionEntries.size, key = { sectionEntries[it].id }) { index
                            ->
                            val position =
                                when {
                                    sectionEntries.size == 1 -> TimelinePosition.ONLY
                                    index == 0 -> TimelinePosition.FIRST
                                    index == sectionEntries.lastIndex -> TimelinePosition.LAST
                                    else -> TimelinePosition.MIDDLE
                                }
                            val dotBias =
                                if (sectionEntries.size <= 1) 0f
                                else -0.8f + 1.6f * index / (sectionEntries.size - 1)
                            TimelineItem(
                                entry = sectionEntries[index],
                                onClick = {
                                    onEntryClick(
                                        sectionEntries[index].id,
                                        if (uiState.isSearchActive) uiState.searchQuery else "",
                                    )
                                },
                                position = position,
                                dotVerticalBias = dotBias,
                                // Kein aeusserer Vertikal-Padding — der 6dp-Abstand wird im
                                // TimelineItem auf die GlassCard gelegt, sodass die Timeline-
                                // Linie zwischen aufeinanderfolgenden Cards durchgehend laeuft.
                                modifier = Modifier.animateItem(),
                                searchQuery =
                                    if (uiState.isSearchActive) uiState.searchQuery else "",
                            )
                        }
                    }

                    // Bottom padding for buttons
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Recording overlay
        AnimatedVisibility(
            visible = uiState.recordingState == RecordingState.RECORDING,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            RecordingOverlay(
                amplitude = amplitude,
                durationSeconds = duration,
                transcriptionModel =
                    if (viewModel.isGroqActive()) "Groq API" else "Lokales Whisper-Modell",
            )
        }

        // Text entry + Mic buttons
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Text entry button (left)
            Surface(
                onClick = {
                    doHaptic(HapticFeedbackType.LongPress)
                    viewModel.startTextEntry()
                },
                modifier = Modifier.size(60.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                border =
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        designTokens.chipBorder,
                    ),
                shadowElevation = 8.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Text eingeben",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            // Mic button (right)
            AnimatedMicButton(
                isRecording = uiState.recordingState == RecordingState.RECORDING,
                onClick = onMicClick,
            )
        }

        // Preview dialog
        if (uiState.showPreviewDialog) {
            PreviewDialog(
                rawText = uiState.rawText,
                improvedText = uiState.improvedText,
                isImproving = uiState.recordingState == RecordingState.IMPROVING,
                isUsingImproved = uiState.isImproveEnabled,
                activePrompt = uiState.activePrompt,
                onImproveClick = { viewModel.improveText() },
                onToggleVersion = { useImproved -> viewModel.setUseImprovedText(useImproved) },
                onTextEdit = { viewModel.updatePreviewText(it) },
                onSave = { viewModel.saveEntry() },
                onDismiss = { viewModel.dismissPreview() },
                onRecordClick = {
                    viewModel.dismissPreview()
                    onMicClick()
                },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (showStreakDialog) {
            StreakDialog(
                currentStreak = uiState.currentStreak,
                longestStreak = uiState.longestStreak,
                totalEntries = allEntries.size,
                onDismiss = { showStreakDialog = false },
            )
        }

        if (showSyncLegend) {
            AlertDialog(
                onDismissRequest = { showSyncLegend = false },
                modifier =
                    Modifier.border(
                        1.dp,
                        designTokens.chipBorder,
                        RoundedCornerShape(22.dp),
                    ),
                shape = RoundedCornerShape(22.dp),
                containerColor = designTokens.card,
                title = {
                    Text(
                        "Google Drive Backup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CloudDone,
                                null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Backup aktuell \u2014 alle Eintr\u00e4ge gesichert",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CloudUpload,
                                null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Backup wird hochgeladen\u2026",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CloudDownload,
                                null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Fotos/Videos werden heruntergeladen\u2026",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CloudOff,
                                null,
                                tint = NeonRed,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Backup fehlgeschlagen!",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CloudOff,
                                null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Nicht angemeldet",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (uiState.lastSyncTimestamp > 0L) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                "Letzte Synchronisierung: ${DTFormatter.formatFull(uiState.lastSyncTimestamp)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(onClick = { showSyncLegend = false }) { Text("OK") }
                },
            )
        }
    }
}

@Composable
private fun GoldenHairline(modifier: Modifier = Modifier) {
    val designTokens = LocalJournalDesignTokens.current
    val transition = rememberInfiniteTransition(label = "gold_hairline")
    val shimmer by
        transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2500, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "gold_hairline_alpha",
        )
    Box(
        modifier =
            modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(1.dp)).background(
                designTokens.hairlineBrush,
                alpha = shimmer,
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewDialog(
    rawText: String,
    improvedText: String?,
    isImproving: Boolean,
    isUsingImproved: Boolean,
    activePrompt: String = "",
    onImproveClick: () -> Unit,
    onToggleVersion: (Boolean) -> Unit,
    onTextEdit: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRecordClick: () -> Unit = {},
) {
    val designTokens = LocalJournalDesignTokens.current
    val showingImproved = isUsingImproved && improvedText != null
    val displayText = if (showingImproved) improvedText!! else rawText
    val hasPrompt = activePrompt.isNotBlank()
    var inputModeChosen by remember { mutableStateOf(rawText.isNotBlank() || !hasPrompt) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var lastEditTime by remember { mutableLongStateOf(0L) }
    var isFocused by remember { mutableStateOf(false) }
    var hadFocusOnce by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (rawText.isBlank() && !hasPrompt) {
            delay(300)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(inputModeChosen) {
        if (inputModeChosen && hasPrompt && rawText.isBlank()) {
            delay(300)
            focusRequester.requestFocus()
        }
    }

    // Auto-clear focus after 5 seconds of inactivity
    LaunchedEffect(lastEditTime) {
        if (lastEditTime > 0 && isFocused) {
            delay(5000)
            focusManager.clearFocus()
        }
    }

    // Clear focus when keyboard is dismissed; close dialog if text still blank
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (!imeVisible && isFocused) {
            focusManager.clearFocus()
        }
        if (!imeVisible && hadFocusOnce && displayText.isBlank() && !hasPrompt) {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        containerColor = designTokens.card,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(designTokens.cardBorder)
            )
        },
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasPrompt) Icons.Rounded.Lightbulb else Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (hasPrompt) "Schreibimpuls" else "Neuer Eintrag",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (improvedText != null) {
                        Text(
                            text = if (showingImproved) "Verbessert" else "Original",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Heute",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            if (hasPrompt) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tagesimpuls: $activePrompt",
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        ),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            if (hasPrompt && !inputModeChosen) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        onClick = { inputModeChosen = true },
                        shape = CircleShape,
                        color = designTokens.chipBackground,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            designTokens.chipBorder,
                        ),
                    ) {
                        Icon(
                            Icons.Rounded.Edit,
                            "Schreiben",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(16.dp).size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    AnimatedMicButton(isRecording = false, onClick = onRecordClick)
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else {
                TextField(
                    value = displayText,
                    onValueChange = { newText ->
                        lastEditTime = System.currentTimeMillis()
                        onTextEdit(newText)
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 132.dp, max = 300.dp)
                            .border(
                                1.dp,
                                designTokens.cardBorder,
                                RoundedCornerShape(14.dp),
                            )
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                isFocused = state.isFocused
                                if (state.isFocused) hadFocusOnce = true
                                if (state.isFocused) lastEditTime = System.currentTimeMillis()
                            },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor =
                                designTokens.chipBackground,
                            unfocusedContainerColor =
                                designTokens.chipBackground,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                    placeholder = {
                        Text(
                            if (hasPrompt) "Schreibe hier deine Gedanken…"
                            else "Was beschäftigt dich gerade?",
                            color = MaterialTheme.colorScheme.outline,
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "KI-Verbesserung",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        onClick = onRecordClick,
                        shape = RoundedCornerShape(20.dp),
                        color = designTokens.chipBackground,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Mic,
                                "Einsprechen",
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

                if (improvedText != null && !isImproving) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onToggleVersion(!showingImproved) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            if (showingImproved) "↩ Original anzeigen"
                            else "✨ Verbesserte Version anzeigen"
                        )
                    }
                } else if (!isImproving && displayText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onImproveClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(999.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    ) {
                        Text("✨ Text verbessern")
                    }
                }

                if (isImproving) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        ShimmerLoadingEffect(height = 60.dp, cornerRadius = 12.dp)
                        Text(
                            "KI verbessert Text, bitte warten",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(999.dp)) {
                        Text(if (displayText.isBlank()) "Abbrechen" else "Verwerfen")
                    }
                    if (displayText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = onSave,
                            shape = RoundedCornerShape(999.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        ) {
                            Text(if (showingImproved) "Verbessert speichern" else "Speichern")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WritingPromptBanner(
    promptText: String,
    promptCategory: String,
    onWriteClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        glowColor = NeonAmber,
        glowIntensity = 0.1f,
        cornerRadius = 20.dp,
        contentPadding = 16.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier =
                            Modifier.size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(NeonAmber, NeonAmber.copy(alpha = 0.6f))
                                    )
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Schreibimpuls des Tages",
                            style =
                                MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                ),
                            color = NeonAmber,
                        )
                        Text(
                            promptCategory,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        "Schließen",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "\u201E$promptText\u201C",
                style =
                    MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onWriteClick,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = NeonAmber,
                            contentColor = Color.White,
                        ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Darüber schreiben", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun StreakDialog(
    currentStreak: Int,
    longestStreak: Int,
    totalEntries: Int,
    onDismiss: () -> Unit,
) {
    val designTokens = LocalJournalDesignTokens.current
    val milestones = listOf(7, 14, 30, 60, 90, 180, 365)
    val nextMilestone = milestones.firstOrNull { it > currentStreak } ?: (currentStreak + 30)
    val prevMilestone = milestones.lastOrNull { it <= currentStreak } ?: 0
    val progress =
        if (nextMilestone > prevMilestone) {
            (currentStreak - prevMilestone).toFloat() / (nextMilestone - prevMilestone)
        } else 0f

    val headline =
        when {
            currentStreak >= 365 -> "Ein ganzes Jahr!"
            currentStreak >= 180 -> "Unglaubliche Disziplin!"
            currentStreak >= 90 -> "Du bist unstoppbar!"
            currentStreak >= 30 -> "Ein ganzer Monat!"
            currentStreak >= 14 -> "Zwei Wochen stark!"
            currentStreak >= 7 -> "Eine ganze Woche!"
            currentStreak >= 3 -> "Du bleibst dran!"
            else -> "Jeder Tag zählt!"
        }

    val motivationText =
        when {
            currentStreak >= 30 ->
                "Was als kleine Gewohnheit begann, ist jetzt ein fester Teil deines Lebens. Dein Tagebuch kennt dich besser als je zuvor."
            currentStreak >= 14 ->
                "Zwei Wochen am Stück, das schaffen die wenigsten. Dein zukünftiges Ich wird dir dafür danken."
            currentStreak >= 7 ->
                "Eine Woche voller Gedanken, Gefühle und Erinnerungen. Du baust gerade etwas Wertvolles auf."
            currentStreak >= 3 ->
                "Drei Tage in Folge sind der Anfang einer echten Gewohnheit. Bleib dran, es lohnt sich!"
            else ->
                "Jeder Eintrag ist ein kleines Geschenk an dein zukünftiges Ich. Schreib morgen wieder!"
        }

    val isDarkTheme = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    val accentColor = if (currentStreak > 7) designTokens.warning else MaterialTheme.colorScheme.primary
    val daysToNext = nextMilestone - currentStreak

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier =
            Modifier.border(
                1.dp,
                designTokens.chipBorder,
                RoundedCornerShape(22.dp),
            ),
        shape = RoundedCornerShape(22.dp),
        containerColor = designTokens.card,
        icon = {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
                shadowElevation = 8.dp,
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$currentStreak Tage in Folge",
                    color = accentColor,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 26.sp.scaledHeading),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    headline,
                    color = accentColor,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    motivationText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Nächstes Ziel: $nextMilestone Tage",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "noch $daysToNext",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    StatColumn(
                        icon = Icons.Rounded.LocalFireDepartment,
                        value = "$currentStreak",
                        label = "Aktuell",
                        tint = accentColor,
                    )
                    StatColumn(
                        icon = Icons.Rounded.EmojiEvents,
                        value = "$longestStreak",
                        label = "Rekord",
                        tint = NeonAmber,
                    )
                    StatColumn(
                        icon = Icons.Rounded.Edit,
                        value = "$totalEntries",
                        label = "Einträge",
                        tint = NeonEmerald,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isDarkTheme) Color.Black else Color.White,
                    ),
            ) {
                Text("Weiter so!")
            }
        },
    )
}

@Composable
private fun StatColumn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color,
) {
    val designTokens = LocalJournalDesignTokens.current
    Surface(
        modifier = Modifier.width(78.dp),
        shape = RoundedCornerShape(14.dp),
        color = designTokens.chipBackground,
        border =
            androidx.compose.foundation.BorderStroke(
                1.dp,
                designTokens.cardBorder,
            ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = tint)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}
