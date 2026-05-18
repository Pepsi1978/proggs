package com.bestjournal.app.ui.screens.entrydetail

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.bestjournal.app.R
import com.bestjournal.app.ui.components.AiImprovedSuffixHelper
import com.bestjournal.app.ui.components.GlassCard
import com.bestjournal.app.ui.components.highlightMatches
import com.bestjournal.app.ui.screens.journal.RecordingState
import com.bestjournal.app.ui.theme.FeatureAccentOrange
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.util.DateTimeFormatter
import com.bestjournal.app.ui.components.PrivacyGateHost
import com.bestjournal.app.ui.components.rememberPrivacyGateState
import com.bestjournal.app.util.EdgeTtsPlayer
import com.bestjournal.app.util.PrivacyGateHelper
import com.bestjournal.app.util.rememberHapticAction
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryDetailScreen(
    viewModel: EntryDetailViewModel,
    onBack: () -> Unit,
    searchQuery: String = "",
    onNavigateToPaywall: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var lastEditTime by remember { mutableLongStateOf(0L) }
    var isFocused by remember { mutableStateOf(false) }
    val isDark = com.bestjournal.app.ui.theme.LocalIsDarkTheme.current
    val highlightColor = if (isDark) Color(0x44FFFFFF) else Color(0xFFFFEB3B)
    val searchHighlight =
        if (searchQuery.isNotBlank()) {
            VisualTransformation { text ->
                TransformedText(
                    highlightMatches(text.text, searchQuery, highlightColor),
                    OffsetMapping.Identity,
                )
            }
        } else {
            VisualTransformation.None
        }

    var fullScreenPhotoPath by remember { mutableStateOf<String?>(null) }
    var fullScreenIsVideo by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    // Pending inline-delete: (followUpId, 1-based index for the confirm dialog title).
    var pendingFollowUpDeletion by remember { mutableStateOf<Pair<Long, Int>?>(null) }
    // Mic-permission + record launcher state for the Nachtrag dialog.
    var pendingFollowUpMicStart by remember { mutableStateOf(false) }
    val followUpAmplitude by viewModel.followUpAmplitude.collectAsStateWithLifecycle()
    val followUpDuration by viewModel.followUpDurationSeconds.collectAsStateWithLifecycle()
    var isSpeaking by remember { mutableStateOf(false) }
    var isTtsLoading by remember { mutableStateOf(false) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    val doHaptic = rememberHapticAction()
    val tts = remember { EdgeTtsPlayer(context) }
    val ttsPrefs = remember {
        try {
            com.bestjournal.app.util.EncryptedPrefsProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }

    // NK1: Per-service consent gates (EDSA 03/2023, BGH Planet49).
    val geminiGate = rememberPrivacyGateState(PrivacyGateHelper.CloudService.Gemini)
    val edgeTtsGate = rememberPrivacyGateState(PrivacyGateHelper.CloudService.EdgeTts)
    PrivacyGateHost(
        state = geminiGate,
        titleRes = R.string.privacy_gate_gemini_title,
        bodyRes = R.string.privacy_gate_gemini_body,
        acceptRes = R.string.privacy_gate_gemini_accept,
        declineRes = R.string.privacy_gate_gemini_cancel,
    )
    PrivacyGateHost(
        state = edgeTtsGate,
        titleRes = R.string.privacy_gate_tts_title,
        bodyRes = R.string.privacy_gate_tts_body,
        acceptRes = R.string.privacy_gate_tts_accept,
        declineRes = R.string.privacy_gate_tts_cancel,
    )

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
    // Remember-stable ImageLoader for photos — without `remember` a new instance
    // would be allocated on every recomposition, bypassing the Coil memory cache
    // and increasing GC pressure in photo-heavy entries.
    val photoImageLoader = remember { coil3.ImageLoader(appContext) }

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

    val micPermissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) {
            granted ->
            if (granted && pendingFollowUpMicStart) {
                viewModel.toggleFollowUpRecording()
            }
            pendingFollowUpMicStart = false
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
                        putExtra("android.intent.extras.CAMERA_FACING", 0) // rear
                        putExtra("android.intent.extras.LENS_FACING_FRONT", 0) // 0 = not front
                        putExtra("android.intent.extra.USE_FRONT_CAMERA", false)
                    }
                cameraLauncher.launch(intent)
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

    LaunchedEffect(uiState.isDeleted) { if (uiState.isDeleted) onBack() }

    Column(
        modifier =
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).clickable(
                interactionSource =
                    remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
            ) {
                focusManager.clearFocus()
            }
    ) {
        TopAppBar(
            title = {
                Text(
                    stringResource(R.string.entry_title),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            // Delete icon moved into the yellow entry bubble itself so it sits
            // next to the "Tagebucheintrag"-header and the timestamp, mirroring
            // the Nachtrag bubble layout.
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        uiState.entry?.let { entry ->
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Summary card \u2014 Frank-style: only rendered when a summary exists.
                // Centered "Zusammenfassung" label in primary color with titleMedium
                // typography and a 16dp spacer to the bullet list. Bullets are bold.
                // No timestamp here \u2014 the timestamp lives inside the yellow entry
                // bubble below, not in the summary card.
                if (!entry.summary.isNullOrBlank()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            Text(
                                stringResource(R.string.entry_summary),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // EU AI Act Art. 50 — KI-generiert Label (Pflicht ab 02.08.2026)
                            com.bestjournal.app.ui.components.AiGeneratedBadge(compact = true)
                            Spacer(modifier = Modifier.height(12.dp))
                            entry.summary
                                .lines()
                                .filter { it.trimStart().startsWith("\u2022") }
                                .forEach { line ->
                                    val bulletText = line.trimStart().removePrefix("\u2022").trim()
                                    Row(modifier = Modifier.padding(bottom = 2.dp)) {
                                        Text(
                                            "\u2022 ",
                                            style =
                                                MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight =
                                                        androidx.compose.ui.text.font.FontWeight
                                                            .Bold
                                                ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        if (searchQuery.isNotBlank()) {
                                            Text(
                                                text =
                                                    highlightMatches(
                                                        bulletText,
                                                        searchQuery,
                                                        highlightColor,
                                                    ),
                                                style =
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight =
                                                            androidx.compose.ui.text.font.FontWeight
                                                                .Bold
                                                    ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        } else {
                                            Text(
                                                bulletText,
                                                style =
                                                    MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight =
                                                            androidx.compose.ui.text.font.FontWeight
                                                                .Bold
                                                    ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                    }
                                }
                            // HWG §3 — Kein Therapie-Ersatz
                            com.bestjournal.app.ui.components.AiOutputDisclaimer()
                        }
                    }
                }

                val hasImproved = entry.isImproved && entry.improvedText != null
                var selectedTab by remember { mutableIntStateOf(0) }
                val isShowingOriginal = selectedTab == 1 && hasImproved

                if (hasImproved) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                            Text(
                                stringResource(R.string.label_improved),
                                modifier = Modifier.padding(8.dp),
                                color =
                                    if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                            )
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text(
                                stringResource(R.string.label_original),
                                modifier = Modifier.padding(8.dp),
                                color =
                                    if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    // EU AI Act Art. 50 — KI-generiert Badge nur wenn Verbesserter-Tab aktiv
                    if (hasImproved && selectedTab == 0) {
                        com.bestjournal.app.ui.components.AiGeneratedBadge(
                            compact = true,
                            modifier = androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }

                val textFieldColors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    )

                // Main journal-entry bubble — Frank 1:1: yellow "Tagebucheintrag"
                // header with Book icon, in-bubble delete IconButton (NeonRed),
                // timestamp row, saved indicator, and the "Mit KI nachtraeglich
                // verbessern" OutlinedButton INSIDE the bubble (only when the
                // entry has not been improved yet).
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
                    glowColor = NeonAmber,
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
                                    tint = NeonAmber,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.entry_journal_entry_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = NeonAmber,
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
                                    contentDescription =
                                        stringResource(R.string.entry_delete_entry_cd),
                                    tint = NeonRed,
                                )
                            }
                        }
                        // Date + relative time, aligned under the Book icon with
                        // a small negative offset so it sits tight under the
                        // header row — mirrors FollowUpInlineCard.
                        Text(
                            "${DateTimeFormatter.formatFull(entry.timestamp)} · ${DateTimeFormatter.formatRelative(context, entry.timestamp)}",
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
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                    visualTransformation = searchHighlight,
                                    colors = textFieldColors,
                                )
                            } else {
                                // KI-Kennzeichnung erfolgt rein ueber den Tab-Header
                                // ("✨ Mit KI verbessert") — kein Inline-Label im Text.
                                // Falls bestehende Eintraege noch einen alten Plain-Text-
                                // Suffix in der DB haben, wird er hier vor der Anzeige
                                // weggestripped, damit der Eintrag sauber editierbar ist.
                                val ctx = LocalContext.current
                                val displayedValue =
                                    AiImprovedSuffixHelper.strip(ctx, uiState.editedDisplayText)
                                TextField(
                                    value = displayedValue,
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
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                    visualTransformation = searchHighlight,
                                    colors = textFieldColors,
                                )
                            }
                        }
                        if (uiState.isSaving) {
                            Text(
                                stringResource(R.string.entry_saving),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        if (!hasImproved) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { geminiGate.run { viewModel.improveTextWithAi() } },
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
                                        stringResource(R.string.entry_improving),
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
                                        stringResource(R.string.entry_improve_with_ai),
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
                        // HWG §3 — Kein Therapie-Ersatz (nur wenn KI-Inhalt sichtbar)
                        if (hasImproved && selectedTab == 0) {
                            com.bestjournal.app.ui.components.AiOutputDisclaimer()
                        }
                    }
                }

                // Nachtraege — inline cards for every existing follow-up. Each
                // card carries its own tab switch between Verbessert/Original
                // when an improved version exists, inline editing, and a
                // per-card delete-confirmation flow driven from the ViewModel.
                uiState.followUps.forEachIndexed { index, followUp ->
                    FollowUpInlineCard(
                        index = index,
                        followUp = followUp,
                        onRawTextChanged = { viewModel.updateInlineFollowUpRaw(followUp.id, it) },
                        onImprovedTextChanged = {
                            viewModel.updateInlineFollowUpImproved(followUp.id, it)
                        },
                        onImproveClick = { viewModel.improveInlineFollowUp(followUp.id) },
                        onDeleteRequested = {
                            pendingFollowUpDeletion = followUp.id to (index + 1)
                        },
                    )
                }

                // Photos section — Frank-style: only rendered when at least one
                // item exists, and it sits under the last Nachtrag (or right
                // under the main entry when there are none). The add button is
                // NOT here — it lives in its own card at the very bottom so
                // the gallery card is a pure display surface.
                if (uiState.photos.isNotEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                    stringResource(R.string.entry_photos_videos),
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
                                                if (photo.isVideo)
                                                    stringResource(R.string.label_video)
                                                else stringResource(R.string.label_photo),
                                            imageLoader =
                                                if (photo.isVideo) videoImageLoader
                                                else photoImageLoader,
                                            modifier =
                                                Modifier.size(120.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        fullScreenPhotoPath = photo.filePath
                                                        fullScreenIsVideo = photo.isVideo
                                                    },
                                            contentScale = ContentScale.Crop,
                                        )
                                        if (photo.isVideo) {
                                            Icon(
                                                Icons.Rounded.PlayCircle,
                                                contentDescription =
                                                    stringResource(R.string.entry_cd_play_video),
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
                                                contentDescription =
                                                    stringResource(R.string.entry_cd_remove),
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

                // "Nachtrag hinzufuegen" card. Carries the "ab dem zweiten
                // Nachtrag" Premium hint. The ViewModel gates the second
                // Nachtrag and flips showFollowUpPremiumDialog instead of
                // opening the editor when the user is on the Free tier.
                AddFollowUpCard(onAddClick = { viewModel.openNewFollowUpDialog() })

                // Add-media card — Frank parity: always at the bottom of the
                // entry stack. Pure control surface, shown even when no media
                // exists yet. Tapping opens the camera-or-gallery picker.
                GlassCard(modifier = Modifier.fillMaxWidth()) {
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
                                stringResource(R.string.entry_photos_videos_add_title),
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
                                stringResource(R.string.entry_add),
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

                // Divider + TTS & Share actions
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                )

                // Action icons + recording duration directly below divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.padding(start = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                if (isSpeaking || isTtsLoading) {
                                    tts.stop()
                                    isSpeaking = false
                                    isTtsLoading = false
                                } else {
                                    val ttsOn =
                                        ttsPrefs?.getBoolean(
                                            com.bestjournal.app.util.Constants.PREF_TTS_ENABLED,
                                            false,
                                        ) ?: false
                                    if (!ttsOn) {
                                        android.widget.Toast.makeText(
                                                context,
                                                context.getString(R.string.entry_tts_enable_hint),
                                                android.widget.Toast.LENGTH_SHORT,
                                            )
                                            .show()
                                    } else {
                                        edgeTtsGate.run {
                                            isTtsLoading = true
                                            isSpeaking = true
                                            // TTS labels come from strings.xml so they
                                            // match the device language. Comma instead
                                            // of a period after the journal label keeps
                                            // the phrase inside one spoken sentence so
                                            // Azure multilingual voices (Seraphina,
                                            // Ava, ...) classify the whole phrase via
                                            // sentence context — isolating the compound
                                            // word behind a period made them mispronounce
                                            // German "Tagebucheintrag" as "Tagebuchchen-
                                            // Track" (English detector kicking in on a
                                            // stand-alone token).
                                            val baseText =
                                                if (isShowingOriginal) entry.rawText
                                                else entry.displayText
                                            // German uses spelled-out number words
                                            // ("eins", "zwei", ...). Other languages get
                                            // plain digits — Azure-TTS reads "1" as
                                            // "one"/"uno"/"un" via the voice locale.
                                            val isGermanLocale =
                                                context.resources.configuration.locales
                                                    .get(0)
                                                    .language == "de"
                                            val journalLabel =
                                                context.getString(R.string.entry_tts_journal_label)
                                            val speakText = buildString {
                                                append(journalLabel)
                                                append(", ")
                                                append(baseText)
                                                uiState.followUps.forEachIndexed { index, fu ->
                                                    val n = index + 1
                                                    val numberStr =
                                                        if (isGermanLocale) germanNumberWord(n)
                                                        else n.toString()
                                                    append("\n\n")
                                                    append(
                                                        context.getString(
                                                            R.string.entry_tts_followup_label,
                                                            numberStr,
                                                        )
                                                    )
                                                    append(". ")
                                                    val fuText =
                                                        if (fu.isImproved &&
                                                            !fu.improvedText.isNullOrBlank()
                                                        )
                                                            fu.improvedText!!
                                                        else fu.text
                                                    append(fuText)
                                                }
                                            }
                                            val voice =
                                                ttsPrefs?.getString(
                                                    com.bestjournal.app.util.Constants
                                                        .PREF_EDGE_TTS_VOICE,
                                                    com.bestjournal.app.util.TtsVoiceRegistry
                                                        .getLocaleVoices()
                                                        .defaultVoiceId,
                                                )
                                                    ?: com.bestjournal.app.util.TtsVoiceRegistry
                                                        .getLocaleVoices()
                                                        .defaultVoiceId
                                            tts.speak(
                                                speakText,
                                                voice = voice,
                                                onPlaybackStart = { isTtsLoading = false },
                                            ) {
                                                isSpeaking = false
                                                isTtsLoading = false
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                contentDescription =
                                    if (isSpeaking) stringResource(R.string.entry_tts_stop)
                                    else stringResource(R.string.entry_tts_read),
                                tint = FeatureAccentOrange,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                val hasImprovedForShare =
                                    entry.isImproved && !entry.improvedText.isNullOrBlank()
                                val photos = uiState.photos
                                if (!hasImprovedForShare && photos.size <= 1) {
                                    val shareText =
                                        buildShareText(entry, useImproved = false, context)
                                    val photoUris =
                                        if (photos.size == 1) {
                                            listOf(getPhotoUri(context, photos[0]))
                                        } else {
                                            emptyList()
                                        }
                                    executeShare(context, shareText, photoUris)
                                } else {
                                    showShareDialog = true
                                }
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                contentDescription = stringResource(R.string.action_share),
                                tint = FeatureAccentOrange,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Text(
                        text =
                            stringResource(
                                R.string.entry_recording_duration,
                                DateTimeFormatter.formatDuration(entry.audioDurationSeconds),
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 34.dp),
                    )
                }

                if (isTtsLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = if (isDark) Color(0xFF5C7AA3) else Color(0xFF1976D2),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.entry_tts_generating),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
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
                            AndroidView(
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
                                contentDescription =
                                    stringResource(R.string.entry_cd_photo_page, page + 1),
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

                // Page indicator
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
                        contentDescription = stringResource(R.string.action_close),
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
                        stringResource(R.string.entry_add_photo_video),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    val infiniteTransition = rememberInfiniteTransition(label = "photo_source")
                    // Flowing color shift
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
                    // Second wave (offset timing for depth)
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
                    // Icon pulse
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
                    // Tile breathing
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
                        // Camera tile — flowing green/gray
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
                                                        if (isDark) tilePrimary
                                                        else Color(0xFF1565C0),
                                                        if (isDark) tileSecondary
                                                        else Color(0xFFBDBDBD),
                                                        flow,
                                                    ),
                                                    lerp(
                                                        if (isDark) tilePrimary.copy(alpha = 0.8f)
                                                        else Color(0xFF1976D2),
                                                        if (isDark) tilePrimary
                                                        else Color(0xFF1565C0),
                                                        flow2,
                                                    ),
                                                    lerp(
                                                        if (isDark) tileSecondary
                                                        else Color(0xFFBDBDBD),
                                                        if (isDark) tilePrimary.copy(alpha = 0.8f)
                                                        else Color(0xFF1976D2),
                                                        flow,
                                                    ),
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
                                    stringResource(R.string.entry_camera),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                )
                            }
                        }
                        // Gallery tile
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
                                                        if (isDark) tilePrimary.copy(alpha = 0.9f)
                                                        else Color(0xFF0D47A1),
                                                        if (isDark) tileSecondary
                                                        else Color(0xFFBDBDBD),
                                                        flow2,
                                                    ),
                                                    lerp(
                                                        if (isDark) tilePrimary.copy(alpha = 0.7f)
                                                        else Color(0xFF1565C0),
                                                        if (isDark) tilePrimary.copy(alpha = 0.9f)
                                                        else Color(0xFF0D47A1),
                                                        flow,
                                                    ),
                                                    lerp(
                                                        if (isDark) tileSecondary
                                                        else Color(0xFFBDBDBD),
                                                        if (isDark) tilePrimary.copy(alpha = 0.7f)
                                                        else Color(0xFF1565C0),
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
                                    stringResource(R.string.entry_gallery),
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
                photos = uiState.photos,
                context = context,
                onDismiss = { showShareDialog = false },
                followUps = uiState.followUps,
            )
        }
    }

    // Nachtrag create / edit dialog (Schreiben / Einsprechen / Verbessern).
    if (uiState.showFollowUpDialog) {
        // Engine badge: after a successful transcription it shows whichever
        // engine actually handled the audio. BEFORE transcription (while the
        // mic is still recording or the dialog is typed-only) we fall back to
        // the Groq model name — same string the journal flow displays — so
        // the user sees the expected model immediately, instead of a generic
        // "Whisper" label that never changes.
        val engineLabel = uiState.followUpTranscriptionModel
            ?: stringResource(R.string.transcription_model_groq)
        FollowUpDialog(
            rawText = uiState.followUpDraftText,
            improvedText = uiState.followUpImprovedText,
            recordingState = uiState.followUpRecordingState,
            amplitude = followUpAmplitude,
            durationSeconds = followUpDuration,
            isImproving = uiState.followUpRecordingState == RecordingState.IMPROVING,
            isUsingImproved = uiState.isUsingImprovedFollowUp,
            canDelete = uiState.activeFollowUpId != null,
            engineLabel = engineLabel,
            onImproveClick = { geminiGate.run { viewModel.improveFollowUp() } },
            onToggleVersion = { viewModel.setUseImprovedFollowUp(it) },
            onTextEdit = { viewModel.updateFollowUpDraft(it) },
            onSave = {
                doHaptic(HapticFeedbackType.LongPress)
                viewModel.saveFollowUp()
            },
            onDismiss = { viewModel.dismissFollowUpDialog() },
            onRecordClick = {
                doHaptic(HapticFeedbackType.LongPress)
                if (uiState.followUpRecordingState == RecordingState.RECORDING) {
                    viewModel.toggleFollowUpRecording()
                } else {
                    val granted =
                        androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.RECORD_AUDIO,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.toggleFollowUpRecording()
                    } else {
                        pendingFollowUpMicStart = true
                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            onDeleteClick = { viewModel.requestFollowUpDeletion() },
        )
    }

    // Confirm delete for an inline Nachtrag (triangle icon in the inline card).
    pendingFollowUpDeletion?.let { (followUpId, number) ->
        FollowUpDeleteConfirmDialog(
            followUpNumber = number,
            onConfirm = {
                viewModel.deleteInlineFollowUp(followUpId)
                pendingFollowUpDeletion = null
            },
            onDismiss = { pendingFollowUpDeletion = null },
        )
    }

    // Confirm delete for the Nachtrag that is currently open in the dialog.
    if (uiState.showDeleteFollowUpDialog && uiState.activeFollowUpId != null) {
        val number =
            uiState.followUps.indexOfFirst { it.id == uiState.activeFollowUpId }.let { idx ->
                if (idx >= 0) idx + 1 else 1
            }
        FollowUpDeleteConfirmDialog(
            followUpNumber = number,
            onConfirm = { viewModel.deleteActiveFollowUp() },
            onDismiss = { viewModel.showDeleteFollowUpDialog(false) },
        )
    }

    // Premium upsell for the second Nachtrag. "Abo starten" routes the user
    // into the full PaywallScreen via the navigation callback so the purchase
    // flow is identical to every other upsell entry point (limit reached,
    // retrospective, profiles, etc.). The source parameter "nachtrag_upsell"
    // shows up in analytics so we can measure how many subscriptions come in
    // from the follow-up gate specifically.
    if (uiState.showFollowUpPremiumDialog) {
        FollowUpPremiumUpsellDialog(
            onStartSubscription = {
                viewModel.dismissFollowUpPremiumDialog()
                onNavigateToPaywall("nachtrag_upsell")
            },
            onDismiss = { viewModel.dismissFollowUpPremiumDialog() },
        )
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    stringResource(R.string.entry_delete_title),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            text = {
                Text(
                    stringResource(R.string.entry_delete_message),
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
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.showDeleteDialog(false) }) {
                    Text(
                        stringResource(R.string.action_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}
