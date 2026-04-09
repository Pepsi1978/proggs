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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.theme.NeonAmber
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.util.DateTimeFormatter
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryDetailScreen(
    viewModel: EntryDetailViewModel,
    onBack: () -> Unit,
    searchQuery: String = "",
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
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
    var cameraFile by remember { mutableStateOf<java.io.File?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
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
            title = { Text("Eintrag", color = MaterialTheme.colorScheme.onBackground) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        "Zur\u00fcck",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        uiState.entry?.let { entry ->
                            val hasImproved =
                                entry.isImproved && !entry.improvedText.isNullOrBlank()
                            val photos = uiState.photos
                            if (!hasImproved && photos.size <= 1) {
                                val shareText = buildShareText(entry, useImproved = false)
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
                        }
                    }
                ) {
                    Icon(
                        Icons.Rounded.IosShare,
                        "Teilen",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(onClick = { viewModel.showDeleteDialog(true) }) {
                    Icon(Icons.Rounded.Delete, "L\u00f6schen", tint = NeonRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        )

        uiState.entry?.let { entry ->
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "${DateTimeFormatter.formatFull(entry.timestamp)} \u00b7 ${DateTimeFormatter.formatRelative(entry.timestamp)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        if (!entry.summary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Zusammenfassung",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
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
                    }
                }

                entry.entropyScore?.let { score ->
                    GlassCard(
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
                                "Verbessert",
                                modifier = Modifier.padding(8.dp),
                                color =
                                    if (selectedTab == 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                            )
                        }
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                            Text(
                                "Original",
                                modifier = Modifier.padding(8.dp),
                                color =
                                    if (selectedTab == 1) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                            )
                        }
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

                GlassCard(
                    modifier =
                        if (hasImproved)
                            Modifier.pointerInput(Unit) {
                                detectHorizontalDragGestures { _, dragAmount ->
                                    if (dragAmount < -40) selectedTab = 1
                                    if (dragAmount > 40) selectedTab = 0
                                }
                            }
                        else Modifier
                ) {
                    Column {
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
                    }
                }

                if (!hasImproved) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        glowColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Button(
                                onClick = { viewModel.improveTextWithAi() },
                                enabled = !uiState.isImproving,
                                shape = RoundedCornerShape(14.dp),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor =
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        disabledContentColor =
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                    ),
                            ) {
                                if (uiState.isImproving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Wird verbessert\u2026",
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                } else {
                                    Icon(
                                        Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Mit KI nachtr\u00e4glich verbessern",
                                        style = MaterialTheme.typography.labelLarge,
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
                    }
                }

                // Photos section
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Fotos/Videos",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Button(
                                onClick = { showPhotoSourceDialog = true },
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Hinzuf\u00fcgen",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }

                        if (uiState.photos.isNotEmpty()) {
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
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Noch keine Fotos/Videos hinzugef\u00fcgt",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
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

                Text(
                    text =
                        "Aufnahmedauer: ${DateTimeFormatter.formatDuration(entry.audioDurationSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )

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
                                                        Color(0xFF4CAF50),
                                                        Color(0xFFBDBDBD),
                                                        flow,
                                                    ),
                                                    lerp(
                                                        Color(0xFF81C784),
                                                        Color(0xFF4CAF50),
                                                        flow2,
                                                    ),
                                                    lerp(Color(0xFFBDBDBD), Color(0xFF81C784), flow),
                                                ),
                                            start = Offset(0f, 300f * flow),
                                            end = Offset(300f, 300f * (1f - flow)),
                                        )
                                    )
                                    .clickable {
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
                                                        Color(0xFFFF9800),
                                                        Color(0xFFBDBDBD),
                                                        flow2,
                                                    ),
                                                    lerp(
                                                        Color(0xFFFFB74D),
                                                        Color(0xFFFF9800),
                                                        flow,
                                                    ),
                                                    lerp(
                                                        Color(0xFFBDBDBD),
                                                        Color(0xFFFFB74D),
                                                        flow2,
                                                    ),
                                                ),
                                            start = Offset(300f * flow2, 0f),
                                            end = Offset(300f * (1f - flow2), 300f),
                                        )
                                    )
                                    .clickable {
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
                photos = uiState.photos,
                context = context,
                onDismiss = { showShareDialog = false },
            )
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Eintrag l\u00f6schen?", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Diesen Eintrag unwiderruflich l\u00f6schen?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteEntry() },
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
