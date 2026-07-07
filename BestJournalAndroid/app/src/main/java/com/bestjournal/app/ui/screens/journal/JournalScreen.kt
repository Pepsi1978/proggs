package com.bestjournal.app.ui.screens.journal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import com.bestjournal.app.BuildConfig
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bestjournal.app.R
import com.bestjournal.app.ui.components.AnimatedMicButton
import com.bestjournal.app.ui.components.EvolvingStreakIcon
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.components.ParticleBackground
import com.bestjournal.app.ui.components.PrivacyGateDialog
import com.bestjournal.app.ui.components.ShimmerLoadingEffect
import com.bestjournal.app.ui.components.SuccessAnimation
import com.bestjournal.app.ui.components.SunMoonToggle
import com.bestjournal.app.ui.components.TimelineItem
import com.bestjournal.app.ui.components.TimelinePosition
import com.bestjournal.app.ui.components.TwinklingStars
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.util.DateTimeFormatter as DTFormatter
import com.bestjournal.app.util.PrivacyGateHelper
import com.bestjournal.app.util.rememberHapticAction
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onEntryClick: (Long, String) -> Unit,
    onNavigateToPaywall: (String) -> Unit = {},
) {
    val allEntries by viewModel.entries.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchResults by
        viewModel.searchEntries(uiState.searchQuery).collectAsStateWithLifecycle(emptyList())
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
                viewModel.triggerInAppReview(activity, count)
                viewModel.consumeReviewEvent()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.seedToastMessage) {
        uiState.seedToastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeSeedToast()
        }
    }

    // DEBUG-only: Test-Daten-Dialog (Hinzufuegen / Alle loeschen / Abbrechen)
    if (BuildConfig.DEBUG && uiState.showSeedDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissSeedDialog() },
            title = { Text(stringResource(R.string.dev_seed_dialog_title)) },
            text = { Text(stringResource(R.string.dev_seed_dialog_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.seedTestData() }) {
                    Text(stringResource(R.string.dev_seed_add_action))
                }
            },
            dismissButton = {
                Row {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.deleteAllEntriesNow() }
                    ) {
                        Text(stringResource(R.string.dev_seed_delete_all_action), color = NeonRed)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.dismissSeedDialog() }
                    ) {
                        Text(stringResource(R.string.dev_seed_cancel_action))
                    }
                }
            },
        )
    }

    // Achievement unlock Snackbar
    val achievementTitle by viewModel.achievementUnlocked.collectAsStateWithLifecycle()
    var showAchievementSnackbar by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(achievementTitle) {
        achievementTitle?.let {
            showAchievementSnackbar = it
            viewModel.clearAchievementEvent()
            kotlinx.coroutines.delay(4000)
            showAchievementSnackbar = null
        }
    }

    var showGroqPrivacyGate by remember { mutableStateOf(false) }

    val startRecordingIfAllowed: () -> Unit = {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.toggleRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val onMicClick: () -> Unit = {
        doHaptic(HapticFeedbackType.LongPress)
        if (uiState.recordingState == RecordingState.RECORDING) {
            viewModel.toggleRecording()
        } else if (!PrivacyGateHelper.hasConsented(context, PrivacyGateHelper.CloudService.Groq)) {
            // K4: First-use consent for USA transfer (Groq Whisper). TDDDG/EDSA 03/2023.
            showGroqPrivacyGate = true
        } else {
            startRecordingIfAllowed()
        }
    }

    if (showGroqPrivacyGate) {
        PrivacyGateDialog(
            titleRes = R.string.privacy_gate_groq_title,
            bodyRes = R.string.privacy_gate_groq_body,
            acceptRes = R.string.privacy_gate_groq_accept,
            declineRes = R.string.privacy_gate_groq_local,
            onAccept = {
                PrivacyGateHelper.setConsent(context, PrivacyGateHelper.CloudService.Groq, true)
                showGroqPrivacyGate = false
                startRecordingIfAllowed()
            },
            onDecline = {
                // User chose local transcription: still allow recording, TranscriptionRepository
                // falls back to local sherpa-onnx if Groq is unavailable. We do NOT persist
                // a "declined" state so the dialog reappears on next tap (explicit decision each time
                // until accepted).
                showGroqPrivacyGate = false
                startRecordingIfAllowed()
            },
        )
    }

    var showSyncLegend by remember { mutableStateOf(false) }
    var showStreakDialog by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Auto-focus search field when search opens (opens keyboard).
    // Robust against "FocusRequester is not initialized" — this crash hit users in
    // some locales (e.g. nl-NL) because layout timing shifts when UI strings differ
    // in length and the FocusRequester may not be attached to its TextField yet
    // when the 100ms delay elapses. We retry up to 3 times with exponential backoff
    // and silently give up (the user can still tap the field manually).
    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) {
            var attempt = 0
            var delayMs = 100L
            while (attempt < 3) {
                delay(delayMs)
                try {
                    searchFocusRequester.requestFocus()
                    break
                } catch (e: IllegalStateException) {
                    // FocusRequester not yet attached — TextField still animating in.
                    android.util.Log.w(
                        "JournalSearch",
                        "FocusRequester attempt ${attempt + 1} failed: ${e.message}",
                    )
                    attempt++
                    delayMs *= 2 // 100ms, 200ms, 400ms
                } catch (e: Exception) {
                    // Any other failure: log and give up — do NOT crash the app.
                    android.util.Log.e(
                        "JournalSearch",
                        "FocusRequester unexpected failure",
                        e,
                    )
                    break
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (LocalIsDarkTheme.current) {
            ParticleBackground()
            TwinklingStars()
        }
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed title bar (does not scroll) — same pattern as DashboardScreen
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.nav_journal),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SunMoonToggle()
                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    doHaptic(HapticFeedbackType.LongPress)
                                    viewModel.onSeedButtonClicked()
                                },
                                enabled = !uiState.seedRunning,
                            ) {
                                if (uiState.seedRunning) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                        contentDescription =
                                            stringResource(R.string.dev_seed_dialog_title),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
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
                                            SyncStatus.NOT_SIGNED_IN -> Icons.Rounded.CloudOff
                                            SyncStatus.ERROR -> Icons.Rounded.CloudOff
                                            SyncStatus.SYNCING -> Icons.Rounded.Cloud
                                            SyncStatus.UPLOADING -> Icons.Filled.CloudUpload
                                            SyncStatus.DOWNLOADING -> Icons.Filled.CloudDownload
                                            else -> Icons.Rounded.CloudDone
                                        },
                                    contentDescription = stringResource(R.string.cd_sync_status),
                                    tint =
                                        when (uiState.syncStatus) {
                                            SyncStatus.NOT_SIGNED_IN -> NeonRed
                                            SyncStatus.SYNCING -> NeonCyan
                                            SyncStatus.UPLOADING -> NeonCyan
                                            SyncStatus.DOWNLOADING -> NeonCyan
                                            SyncStatus.ERROR -> NeonRed
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
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border =
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                ),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Search,
                                    stringResource(R.string.journal_search),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.journal_search_hint),
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
                        text = stringResource(R.string.journal_entry_count, allEntries.size),
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
                                    .clickable {
                                        doHaptic(HapticFeedbackType.LongPress)
                                        showStreakDialog = true
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = stringResource(R.string.journal_cd_streak),
                                tint = streakColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text =
                                    stringResource(
                                        R.string.journal_streak_days,
                                        uiState.currentStreak,
                                    ),
                                style = MaterialTheme.typography.labelSmall,
                                color = streakColor,
                            )
                        }
                    }
                }
            }
            // Transcribing state — no visual indicator needed (transcription is near-instant)

            if (entries.isEmpty() && uiState.recordingState == RecordingState.IDLE) {
                // Writing prompt banner in empty state
                if (uiState.showPromptBanner && uiState.dailyPromptText.isNotBlank()) {
                    WritingPromptBanner(
                        promptText = uiState.dailyPromptText,
                        promptCategory = uiState.dailyPromptCategory,
                        onWriteClick = {
                            viewModel.startTextEntryWithPrompt(uiState.dailyPromptText)
                        },
                        onDismiss = { viewModel.dismissPromptBanner() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.journal_no_entries),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.journal_tap_mic_to_start),
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
                        entries.groupBy { DTFormatter.getSectionLabel(context, it.timestamp) }
                    }

                // Fixed search bar (does not scroll with entries)
                AnimatedVisibility(visible = uiState.isSearchActive) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = {
                            Text(
                                stringResource(R.string.journal_search_placeholder),
                                color = MaterialTheme.colorScheme.outline,
                            )
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
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
                                    stringResource(R.string.journal_search_close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    // Daily writing prompt banner — always visible, no animation
                    if (uiState.showPromptBanner && uiState.dailyPromptText.isNotBlank()) {
                        item(key = "writing_prompt") {
                            WritingPromptBanner(
                                promptText = uiState.dailyPromptText,
                                promptCategory = uiState.dailyPromptCategory,
                                onWriteClick = {
                                    viewModel.startTextEntryWithPrompt(uiState.dailyPromptText)
                                },
                                onDismiss = { viewModel.dismissPromptBanner() },
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    groupedEntries.forEach { (sectionLabel, sectionEntries) ->
                        // Section header
                        item(key = "header_$sectionLabel") {
                            Column(
                                modifier =
                                    Modifier.animateItem().padding(top = 12.dp, bottom = 4.dp)
                            ) {
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
                onClick = {
                    doHaptic(HapticFeedbackType.LongPress)
                    viewModel.startTextEntry()
                },
                modifier = Modifier.size(64.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.journal_cd_text_input),
                    modifier = Modifier.size(28.dp),
                )
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
                transcriptionModel = uiState.transcriptionModel,
                showTextUpsellBanner = uiState.showTextUpsellBanner,
                activePrompt = uiState.activePrompt,
                onImproveClick = { viewModel.improveText() },
                onToggleVersion = { useImproved -> viewModel.setUseImprovedText(useImproved) },
                onTextEdit = { viewModel.updatePreviewText(it) },
                onSave = { viewModel.saveEntry() },
                onDismiss = { viewModel.dismissPreview() },
                onRecordClick = {
                    viewModel.dismissPreviewForRecording()
                    onMicClick()
                },
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

        // Gold Achievement unlock Snackbar
        AnimatedVisibility(
            visible = showAchievementSnackbar != null,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp),
        ) {
            showAchievementSnackbar?.let { title ->
                Snackbar(
                    containerColor = Color(0xFF8B6914),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                stringResource(R.string.journal_achievement_unlocked),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f),
                            )
                            Text(
                                title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }

        if (showStreakDialog) {
            StreakDialog(
                currentStreak = uiState.currentStreak,
                longestStreak = uiState.longestStreak,
                totalEntries = allEntries.size,
                onDismiss = { showStreakDialog = false },
                onNavigateToPaywall = onNavigateToPaywall,
            )
        }

        if (showSyncLegend) {
            AlertDialog(
                onDismissRequest = { showSyncLegend = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        stringResource(R.string.journal_drive_backup_dialog),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Warning,
                                null,
                                tint = NeonAmber,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.journal_drive_not_signed_in),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CloudDone,
                                null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.journal_drive_backup_current),
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
                                stringResource(R.string.journal_drive_uploading),
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
                                stringResource(R.string.journal_drive_downloading),
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
                                stringResource(R.string.journal_drive_failed),
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (uiState.lastSyncTimestamp > 0L) {
                            androidx.compose.material3.HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            Text(
                                stringResource(
                                    R.string.journal_drive_last_sync,
                                    DTFormatter.formatFull(uiState.lastSyncTimestamp),
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                confirmButton = {
                    OutlinedButton(onClick = { showSyncLegend = false }) {
                        Text(stringResource(R.string.action_ok))
                    }
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
    activePrompt: String = "",
    onImproveClick: () -> Unit,
    onToggleVersion: (Boolean) -> Unit,
    onTextEdit: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onRecordClick: () -> Unit = {},
    onUpsellClick: () -> Unit = {},
    onUpsellDismiss: () -> Unit = {},
) {
    val showingImproved = isUsingImproved && improvedText != null
    val displayText = if (showingImproved) improvedText ?: rawText else rawText
    val hasPrompt = activePrompt.isNotBlank()
    var showSuccess by remember { mutableStateOf(false) }
    val doHaptic = rememberHapticAction()

    if (showSuccess) {
        LaunchedEffect(Unit) {
            delay(800)
            showSuccess = false
            onSave()
        }
    }
    var inputModeChosen by remember { mutableStateOf(rawText.isNotBlank() || !hasPrompt) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var lastEditTime by remember { mutableLongStateOf(0L) }
    var isFocused by remember { mutableStateOf(false) }
    var hadFocusOnce by remember { mutableStateOf(false) }

    // Auto-focus for text entry mode (not for prompt choice phase)
    LaunchedEffect(Unit) {
        if (rawText.isBlank() && !hasPrompt) {
            delay(300)
            focusRequester.requestFocus()
        }
    }

    // Focus text field after user chooses "Schreiben"
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

    // Clear focus when keyboard is dismissed (back gesture/swipe)
    // If text is empty and no prompt, also close the dialog
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (!imeVisible && isFocused) {
            focusManager.clearFocus()
        }
        if (!imeVisible && hadFocusOnce && displayText.isBlank() && !hasPrompt) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus()
            onDismiss()
        },
        modifier = if (hasPrompt) Modifier.fillMaxWidth(0.95f) else Modifier,
        properties =
            if (hasPrompt) DialogProperties(usePlatformDefaultWidth = false)
            else DialogProperties(),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasPrompt) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = NeonAmber,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        if (hasPrompt) stringResource(R.string.journal_new_entry_prompt)
                        else stringResource(R.string.journal_new_entry),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (improvedText != null) {
                        Text(
                            text =
                                if (showingImproved) stringResource(R.string.label_improved)
                                else stringResource(R.string.label_original),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    if (!hasPrompt) {
                        Text(
                            text = "\u270F\uFE0F",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        },
        text = {
            Column {
                // Inspirational prompt card
                if (hasPrompt) {
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .background(
                                    brush =
                                        Brush.linearGradient(
                                            listOf(
                                                NeonAmber.copy(alpha = 0.10f),
                                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                                    alpha = 0.4f
                                                ),
                                            )
                                        ),
                                    shape = RoundedCornerShape(16.dp),
                                )
                                .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "\u201E$activePrompt\u201C",
                                style =
                                    MaterialTheme.typography.titleMedium.copy(
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.journal_freeflow_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    if (!inputModeChosen) {
                        // Phase 1: User chooses input method — FAB style like bottom bar
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Pen button (left)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = { inputModeChosen = true },
                                    modifier = Modifier.size(56.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                    elevation =
                                        FloatingActionButtonDefaults.elevation(
                                            defaultElevation = 8.dp
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.journal_write),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    stringResource(R.string.journal_write),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Divider
                            Box(
                                modifier =
                                    Modifier.height(32.dp)
                                        .width(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Mic button (right)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                FloatingActionButton(
                                    onClick = onRecordClick,
                                    modifier = Modifier.size(56.dp),
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                    elevation =
                                        FloatingActionButtonDefaults.elevation(
                                            defaultElevation = 8.dp
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = stringResource(R.string.journal_speak),
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    stringResource(R.string.journal_speak),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        // Phase 2: Input active — show small mic chip + label
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.journal_your_answer),
                                style =
                                    MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
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
                                        stringResource(R.string.journal_speak),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        stringResource(R.string.journal_speak),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (inputModeChosen) {
                    TextField(
                        value = displayText,
                        onValueChange = { newText ->
                            lastEditTime = System.currentTimeMillis()
                            onTextEdit(newText)
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                                .heightIn(min = if (hasPrompt) 120.dp else 0.dp, max = 300.dp)
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
                                if (hasPrompt) stringResource(R.string.journal_placeholder_prompt)
                                else stringResource(R.string.journal_placeholder_edit),
                                color = MaterialTheme.colorScheme.outline,
                            )
                        },
                        keyboardOptions =
                            androidx.compose.foundation.text.KeyboardOptions(
                                capitalization =
                                    androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                            ),
                    )

                    if (transcriptionModel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text =
                                stringResource(
                                    R.string.journal_transcribed_with,
                                    transcriptionModel,
                                ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (inputModeChosen && improvedText != null && !isImproving) {
                    // Toggle between versions
                    OutlinedButton(
                        onClick = { onToggleVersion(!showingImproved) },
                        modifier = Modifier.align(Alignment.End),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                    ) {
                        Text(
                            text =
                                if (showingImproved) stringResource(R.string.journal_show_original)
                                else stringResource(R.string.journal_show_improved)
                        )
                    }

                    // Contextual upsell banner after first text improvement
                    if (showTextUpsellBanner) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                        ) {
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .background(
                                            brush =
                                                Brush.linearGradient(
                                                    listOf(
                                                        NeonAmber.copy(alpha = 0.12f),
                                                        MaterialTheme.colorScheme.surfaceVariant
                                                            .copy(alpha = 0.6f),
                                                    )
                                                ),
                                            shape = RoundedCornerShape(16.dp),
                                        )
                                        .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(30.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(
                                                            NeonAmber,
                                                            NeonAmber.copy(alpha = 0.6f),
                                                        )
                                                    )
                                                ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text =
                                                stringResource(R.string.journal_text_upsell_title),
                                            style =
                                                MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                            color = NeonAmber,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text =
                                                stringResource(R.string.journal_text_upsell_body),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = stringResource(R.string.journal_later),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.clickable { onUpsellDismiss() },
                                            )
                                            Text(
                                                text =
                                                    stringResource(
                                                        R.string.journal_discover_premium
                                                    ),
                                                style =
                                                    MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                color = NeonAmber,
                                                modifier = Modifier.clickable { onUpsellClick() },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (
                    inputModeChosen &&
                        improvedText == null &&
                        !isImproving &&
                        displayText.isNotBlank()
                ) {
                    Button(
                        onClick = onImproveClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                    ) {
                        Text(stringResource(R.string.journal_improve_text))
                    }
                }

                if (isImproving) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        ShimmerLoadingEffect(height = 60.dp, cornerRadius = 12.dp)
                        Text(
                            stringResource(R.string.journal_ai_improving),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (displayText.isNotBlank()) {
                if (showSuccess) {
                    SuccessAnimation()
                } else {
                    Button(
                        onClick = {
                            doHaptic(HapticFeedbackType.LongPress)
                            showSuccess = true
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Text(
                            if (showingImproved) stringResource(R.string.journal_save_improved)
                            else stringResource(R.string.journal_save)
                        )
                    }
                }
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
                Text(
                    if (displayText.isBlank()) stringResource(R.string.action_cancel)
                    else stringResource(R.string.action_discard)
                )
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
    onNavigateToPaywall: (String) -> Unit = {},
) {
    // Find next milestone
    val milestones = listOf(7, 14, 30, 60, 90, 180, 365)
    val nextMilestone = milestones.firstOrNull { it > currentStreak } ?: (currentStreak + 30)
    val prevMilestone = milestones.lastOrNull { it <= currentStreak } ?: 0
    val progress =
        if (nextMilestone > prevMilestone) {
            (currentStreak - prevMilestone).toFloat() / (nextMilestone - prevMilestone)
        } else 0f

    // Emotional headline based on streak length
    val headline =
        when {
            currentStreak >= 365 -> stringResource(R.string.streak_365)
            currentStreak >= 180 -> stringResource(R.string.streak_180)
            currentStreak >= 90 -> stringResource(R.string.streak_90)
            currentStreak >= 30 -> stringResource(R.string.streak_30)
            currentStreak >= 14 -> stringResource(R.string.streak_14)
            currentStreak >= 7 -> stringResource(R.string.streak_7)
            currentStreak >= 3 -> stringResource(R.string.streak_3)
            else -> stringResource(R.string.streak_default)
        }

    val motivationText =
        when {
            currentStreak >= 30 -> stringResource(R.string.streak_desc_365)
            currentStreak >= 14 -> stringResource(R.string.streak_desc_14)
            currentStreak >= 7 -> stringResource(R.string.streak_desc_7)
            currentStreak >= 3 -> stringResource(R.string.streak_desc_3)
            else -> stringResource(R.string.streak_desc_1)
        }

    val isDarkTheme = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    val accentColor = if (currentStreak > 7) NeonAmber else MaterialTheme.colorScheme.primary
    val daysToNext = nextMilestone - currentStreak

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = { EvolvingStreakIcon(streakDays = currentStreak) },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.journal_streak_days_in_row, currentStreak),
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                            stringResource(R.string.journal_streak_next_goal, nextMilestone),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            stringResource(R.string.journal_streak_remaining, daysToNext),
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
                        label = stringResource(R.string.label_current),
                        tint = accentColor,
                    )
                    StatColumn(
                        icon = Icons.Rounded.EmojiEvents,
                        value = "$longestStreak",
                        label = stringResource(R.string.label_record),
                        tint = NeonAmber,
                    )
                    StatColumn(
                        icon = Icons.Rounded.Edit,
                        value = "$totalEntries",
                        label = stringResource(R.string.journal_entries_label),
                        tint = NeonEmerald,
                    )
                }

                // Streak freeze premium hint (only for streaks > 7)
                if (currentStreak > 7) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onNavigateToPaywall("streak_freeze")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.journal_streak_premium),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                Text(stringResource(R.string.journal_keep_going))
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
                                    Brush.linearGradient(
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
                            stringResource(R.string.journal_daily_prompt),
                            style =
                                MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
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
                        stringResource(R.string.action_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "\u201E$promptText\u201C",
                style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
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
                    Text(
                        stringResource(R.string.journal_write_about),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
