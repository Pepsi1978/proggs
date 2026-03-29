package com.entropyjournal.ui.screens.journal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import com.entropyjournal.ui.components.AnimatedMicButton
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.components.ShimmerLoadingEffect
import com.entropyjournal.ui.components.SunMoonToggle
import com.entropyjournal.ui.components.TimelineItem
import com.entropyjournal.ui.components.TimelinePosition
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.util.Constants
import com.entropyjournal.util.DateTimeFormatter as DTFormatter

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onEntryClick: (Long) -> Unit
) {
    val entries by viewModel.entries.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val amplitude by viewModel.amplitude.collectAsState()
    val duration by viewModel.durationSeconds.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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
        if (uiState.recordingState == RecordingState.RECORDING) {
            viewModel.toggleRecording()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                viewModel.toggleRecording()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search bar
            AnimatedVisibility(visible = uiState.isSearchActive) {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Eintr\u00e4ge durchsuchen...", color = MaterialTheme.colorScheme.outline) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(Icons.Rounded.Close, "Suche schlie\u00dfen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Sync status + search toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Tagebuch",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SunMoonToggle()
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (uiState.syncStatus) {
                            SyncStatus.SYNCED -> Icons.Rounded.CloudDone
                            SyncStatus.ERROR -> Icons.Rounded.CloudOff
                            else -> Icons.Rounded.Cloud
                        },
                        contentDescription = "Sync-Status",
                        tint = when (uiState.syncStatus) {
                            SyncStatus.SYNCED -> NeonEmerald
                            SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                            SyncStatus.ERROR -> NeonRed
                            else -> MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(Icons.Rounded.Search, "Suchen", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Transcribing state
            if (uiState.recordingState == RecordingState.TRANSCRIBING) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ShimmerLoadingEffect(height = 20.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ShimmerLoadingEffect(height = 16.dp, modifier = Modifier.fillMaxWidth(0.7f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Transkribiere...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (entries.isEmpty() && uiState.recordingState == RecordingState.IDLE) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Noch keine Eintr\u00e4ge", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tippe auf das Mikrofon um zu starten", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                    }
                }
            } else {
                // Group entries by time period
                val groupedEntries = remember(entries) {
                    entries.groupBy { DTFormatter.getSectionLabel(it.timestamp) }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    groupedEntries.forEach { (sectionLabel, sectionEntries) ->
                        // Section header
                        item(key = "header_$sectionLabel") {
                            Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                                Text(
                                    text = sectionLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }

                        // Entries with timeline position
                        items(
                            count = sectionEntries.size,
                            key = { sectionEntries[it].id }
                        ) { index ->
                            val position = when {
                                sectionEntries.size == 1 -> TimelinePosition.ONLY
                                index == 0 -> TimelinePosition.FIRST
                                index == sectionEntries.lastIndex -> TimelinePosition.LAST
                                else -> TimelinePosition.MIDDLE
                            }
                            TimelineItem(
                                entry = sectionEntries[index],
                                onClick = { onEntryClick(sectionEntries[index].id) },
                                position = position,
                                modifier = Modifier.padding(vertical = 6.dp)
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
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            RecordingOverlay(
                amplitude = amplitude,
                durationSeconds = duration
            )
        }

        // Text entry + Mic buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Text entry button (left)
            FloatingActionButton(
                onClick = { viewModel.startTextEntry() },
                modifier = Modifier.size(64.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Text eingeben",
                    modifier = Modifier.size(28.dp)
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // Mic button (right)
            AnimatedMicButton(
                isRecording = uiState.recordingState == RecordingState.RECORDING,
                onClick = onMicClick
            )
        }

        // Preview dialog
        if (uiState.showPreviewDialog) {
            PreviewDialog(
                rawText = uiState.rawText,
                improvedText = uiState.improvedText,
                isImproving = uiState.recordingState == RecordingState.IMPROVING,
                isUsingImproved = uiState.isImproveEnabled,
                onImproveClick = { viewModel.improveText() },
                onToggleVersion = { useImproved -> viewModel.setUseImprovedText(useImproved) },
                onTextEdit = { viewModel.updatePreviewText(it) },
                onSave = { viewModel.saveEntry() },
                onDismiss = { viewModel.dismissPreview() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
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
    onImproveClick: () -> Unit,
    onToggleVersion: (Boolean) -> Unit,
    onTextEdit: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val showingImproved = isUsingImproved && improvedText != null
    val displayText = if (showingImproved) improvedText!! else rawText
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var lastEditTime by remember { mutableLongStateOf(0L) }
    var isFocused by remember { mutableStateOf(false) }
    var hadFocusOnce by remember { mutableStateOf(false) }

    // Auto-focus the text field when dialog opens (keyboard pops up immediately)
    LaunchedEffect(Unit) {
        delay(300) // wait for dialog animation
        focusRequester.requestFocus()
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Neuer Eintrag", color = MaterialTheme.colorScheme.onSurface)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (improvedText != null) {
                        Text(
                            text = if (showingImproved) "Verbessert" else "Original",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "\u270F\uFE0F",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            isFocused = state.isFocused
                            if (state.isFocused) hadFocusOnce = true
                            if (state.isFocused) lastEditTime = System.currentTimeMillis()
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    placeholder = {
                        Text(
                            "Tippe hier, um den Text zu bearbeiten...",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (improvedText != null && !isImproving) {
                    // Toggle between versions
                    OutlinedButton(
                        onClick = { onToggleVersion(!showingImproved) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (showingImproved) "\u21A9 Original anzeigen" else "\u2728 Verbesserte Version anzeigen"
                        )
                    }
                }

                if (improvedText == null && !isImproving) {
                    Button(
                        onClick = onImproveClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("\u2728 Text verbessern")
                    }
                }

                if (isImproving) {
                    ShimmerLoadingEffect(height = 16.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Optimiere Text...", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(if (showingImproved) "Verbessert speichern" else "Speichern")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) { Text("Verwerfen") }
        }
    )
}
