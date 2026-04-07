package com.bestjournal.app.ui.screens.journal

import android.Manifest
import androidx.compose.material3.Surface
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bestjournal.app.ui.components.AnimatedMicButton
import com.bestjournal.app.ui.components.ShimmerLoadingEffect
import com.bestjournal.app.ui.components.SunMoonToggle
import com.bestjournal.app.ui.components.TimelineItem
import com.bestjournal.app.ui.components.TimelinePosition
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.util.DateTimeFormatter as DTFormatter
import kotlinx.coroutines.delay

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onEntryClick: (Long, String) -> Unit,
    onNavigateToPaywall: (String) -> Unit = {},
) {
    val allEntries by viewModel.entries.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val searchResults by
        viewModel.searchEntries(uiState.searchQuery).collectAsState(initial = emptyList())
    val entries =
        if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
            searchResults
        } else {
            allEntries
        }
    val amplitude by viewModel.amplitude.collectAsState()
    val duration by viewModel.durationSeconds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted
            ->
            if (isGranted) {
                viewModel.toggleRecording()
            }
        }

    // Observe review event — trigger in-app review when signaled
    val activity = context as? android.app.Activity
    LaunchedEffect(Unit) {
        viewModel.reviewEvent.collect { count ->
            if (count != null && activity != null) {
                viewModel.consumeReviewEvent()
                viewModel.triggerInAppReview(activity, count)
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val onMicClick: () -> Unit = {
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

    // Auto-focus search field when search opens (opens keyboard)
    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            delay(100) // Wait for AnimatedVisibility to render
            searchFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        .focusRequester(searchFocusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
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
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Sync status + search toggle
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Tagebuch",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SunMoonToggle()
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showSyncLegend = true }) {
                        Icon(
                            imageVector =
                                when (uiState.syncStatus) {
                                    SyncStatus.ERROR -> Icons.Rounded.CloudOff
                                    SyncStatus.SYNCING -> Icons.Rounded.Cloud
                                    else -> Icons.Rounded.CloudDone
                                },
                            contentDescription = "Sync-Status",
                            tint =
                                when (uiState.syncStatus) {
                                    SyncStatus.SYNCING -> NeonCyan
                                    SyncStatus.ERROR -> NeonRed
                                    else -> NeonEmerald
                                },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Surface(
                        onClick = { viewModel.toggleSearch() },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Search,
                                "Suchen",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Suche",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "${allEntries.size} Eintr\u00e4ge",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (uiState.currentStreak > 0) {
                        val streakColor = if (uiState.currentStreak > 7) NeonAmber
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    streakColor.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { showStreakDialog = true }
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

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    groupedEntries.forEach { (sectionLabel, sectionEntries) ->
                        // Section header
                        item(key = "header_$sectionLabel") {
                            Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                                Text(
                                    text = sectionLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            .height(1.dp)
                                            .background(MaterialTheme.colorScheme.outlineVariant)
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
                            TimelineItem(
                                entry = sectionEntries[index],
                                onClick = { onEntryClick(sectionEntries[index].id, if (uiState.isSearchActive) uiState.searchQuery else "") },
                                position = position,
                                modifier = Modifier.padding(vertical = 6.dp),
                                searchQuery = if (uiState.isSearchActive) uiState.searchQuery else "",
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
                transcriptionModel = uiState.transcriptionModel,
            )
        }

        // Text entry + Mic buttons
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Text entry button (left)
            FloatingActionButton(
                onClick = { viewModel.startTextEntry() },
                modifier = Modifier.size(64.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Text eingeben",
                    modifier = Modifier.size(28.dp),
                )
            }

            // Divider
            Box(
                modifier =
                    Modifier.height(32.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
            )

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
                transcriptionModel = uiState.transcriptionModel,
                showTextUpsellBanner = uiState.showTextUpsellBanner,
                onImproveClick = { viewModel.improveText() },
                onToggleVersion = { useImproved -> viewModel.setUseImprovedText(useImproved) },
                onTextEdit = { viewModel.updatePreviewText(it) },
                onSave = { viewModel.saveEntry() },
                onDismiss = { viewModel.dismissPreview() },
                onUpsellClick = {
                    viewModel.onTextUpsellClicked()
                    onNavigateToPaywall("first_text")
                },
                onUpsellDismiss = { viewModel.dismissTextUpsellBanner() },
            )
        }

        // AI limit reached — navigate to fullscreen PaywallScreen
        LaunchedEffect(uiState.showAiLimitReached) {
            if (uiState.showAiLimitReached) {
                onNavigateToPaywall("limit_reached")
                viewModel.dismissAiLimitReached()
            }
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
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text("Google Drive Backup", color = MaterialTheme.colorScheme.onSurface)
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
                                Icons.Rounded.Cloud,
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
private fun PreviewDialog(
    rawText: String,
    improvedText: String?,
    isImproving: Boolean,
    isUsingImproved: Boolean,
    transcriptionModel: String = "",
    showTextUpsellBanner: Boolean = false,
    onImproveClick: () -> Unit,
    onToggleVersion: (Boolean) -> Unit,
    onTextEdit: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onUpsellClick: () -> Unit = {},
    onUpsellDismiss: () -> Unit = {},
) {
    val showingImproved = isUsingImproved && improvedText != null
    val displayText = if (showingImproved) improvedText!! else rawText
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var lastEditTime by remember { mutableLongStateOf(0L) }
    var isFocused by remember { mutableStateOf(false) }
    var hadFocusOnce by remember { mutableStateOf(false) }

    // Auto-focus only for text entry mode (empty text = user tapped the edit button)
    LaunchedEffect(Unit) {
        if (rawText.isBlank()) {
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

    // Clear focus when keyboard is dismissed (back gesture/swipe)
    // If text is empty, also close the dialog
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (!imeVisible && isFocused) {
            focusManager.clearFocus()
        }
        if (!imeVisible && hadFocusOnce && displayText.isBlank()) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Neuer Eintrag", color = MaterialTheme.colorScheme.onSurface)
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
                        text = "\u270F\uFE0F",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        },
        text = {
            Column {
                TextField(
                    value = displayText,
                    onValueChange = { newText ->
                        lastEditTime = System.currentTimeMillis()
                        onTextEdit(newText)
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                isFocused = state.isFocused
                                if (state.isFocused) hadFocusOnce = true
                                if (state.isFocused) lastEditTime = System.currentTimeMillis()
                            },
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
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
                            "Tippe hier, um den Text zu bearbeiten...",
                            color = MaterialTheme.colorScheme.outline,
                        )
                    },
                )

                if (transcriptionModel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Transkribiert mit $transcriptionModel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (improvedText != null && !isImproving) {
                    // Toggle between versions
                    OutlinedButton(
                        onClick = { onToggleVersion(!showingImproved) },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                    ) {
                        Text(
                            text =
                                if (showingImproved) "\u21A9 Original anzeigen"
                                else "\u2728 Verbesserte Version anzeigen"
                        )
                    }

                    // Contextual upsell banner after first text improvement
                    if (showTextUpsellBanner) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Beeindruckend, oder? Mit Premium kannst du jeden Eintrag verbessern.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Premium entdecken",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { onUpsellClick() },
                                    )
                                    Text(
                                        text = "Sp\u00e4ter",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.clickable { onUpsellDismiss() },
                                    )
                                }
                            }
                        }
                    }
                }

                if (improvedText == null && !isImproving) {
                    Button(
                        onClick = onImproveClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    ) {
                        Text("\u2728 Text verbessern")
                    }
                }

                if (isImproving) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ShimmerLoadingEffect(height = 60.dp, cornerRadius = 12.dp)
                        Text(
                            "KI verbessert Text \u2014 bitte warten",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            ) {
                Text(if (showingImproved) "Verbessert speichern" else "Speichern")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
            ) {
                Text("Verwerfen")
            }
        },
    )
}

@Composable
private fun StreakDialog(
    currentStreak: Int,
    longestStreak: Int,
    totalEntries: Int,
    onDismiss: () -> Unit,
) {
    // Find next milestone
    val milestones = listOf(7, 14, 30, 60, 90, 180, 365)
    val nextMilestone = milestones.firstOrNull { it > currentStreak } ?: (currentStreak + 30)
    val prevMilestone = milestones.lastOrNull { it <= currentStreak } ?: 0
    val progress = if (nextMilestone > prevMilestone) {
        (currentStreak - prevMilestone).toFloat() / (nextMilestone - prevMilestone)
    } else 0f

    // Emotional headline based on streak length
    val headline = when {
        currentStreak >= 365 -> "Ein ganzes Jahr!"
        currentStreak >= 180 -> "Unglaubliche Disziplin!"
        currentStreak >= 90 -> "Du bist unstoppbar!"
        currentStreak >= 30 -> "Ein ganzer Monat!"
        currentStreak >= 14 -> "Zwei Wochen stark!"
        currentStreak >= 7 -> "Eine ganze Woche!"
        currentStreak >= 3 -> "Du bleibst dran!"
        else -> "Jeder Tag z\u00e4hlt!"
    }

    val motivationText = when {
        currentStreak >= 30 -> "Was als kleine Gewohnheit begann, ist jetzt ein fester Teil deines Lebens. Dein Tagebuch kennt dich besser als je zuvor."
        currentStreak >= 14 -> "Zwei Wochen am St\u00fcck, das schaffen die wenigsten. Dein zuk\u00fcnftiges Ich wird dir daf\u00fcr danken."
        currentStreak >= 7 -> "Eine Woche voller Gedanken, Gef\u00fchle und Erinnerungen. Du baust gerade etwas Wertvolles auf."
        currentStreak >= 3 -> "Drei Tage in Folge sind der Anfang einer echten Gewohnheit. Bleib dran, es lohnt sich!"
        else -> "Jeder Eintrag ist ein kleines Geschenk an dein zuk\u00fcnftiges Ich. Schreib morgen wieder!"
    }

    val isDarkTheme = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    val accentColor = if (currentStreak > 7) NeonAmber else MaterialTheme.colorScheme.primary
    val daysToNext = nextMilestone - currentStreak

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(40.dp),
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "$currentStreak Tage in Folge",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Motivation text
                Text(
                    motivationText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Progress to next milestone
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "N\u00e4chstes Ziel: $nextMilestone Tage",
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

                // Stats row
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
                        label = "Eintr\u00e4ge",
                        tint = NeonEmerald,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
