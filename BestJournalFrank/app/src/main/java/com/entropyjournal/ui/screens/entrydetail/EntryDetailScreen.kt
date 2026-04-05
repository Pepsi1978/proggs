package com.entropyjournal.ui.screens.entrydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.entropyjournal.ui.components.GlassCard
import com.entropyjournal.ui.theme.NeonAmber
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.util.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryDetailScreen(
    viewModel: EntryDetailViewModel,
    onBack: () -> Unit,
    searchQuery: String = ""
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val isDark = com.entropyjournal.ui.theme.LocalIsDarkTheme.current
    val highlightColor = if (isDark) Color(0x44FFFFFF) else Color(0xFFFFEB3B)
    val searchHighlight = if (searchQuery.isNotBlank()) {
        androidx.compose.ui.text.input.VisualTransformation { text ->
            androidx.compose.ui.text.input.TransformedText(
                com.entropyjournal.ui.components.highlightMatches(text.text, searchQuery, highlightColor),
                androidx.compose.ui.text.input.OffsetMapping.Identity
            )
        }
    } else {
        androidx.compose.ui.text.input.VisualTransformation.None
    }
    val focusRequester = remember { FocusRequester() }
    var lastEditTime by remember { mutableLongStateOf(0L) }
    var isFocused by remember { mutableStateOf(false) }

    // Auto-clear focus after 5 seconds of inactivity
    LaunchedEffect(lastEditTime) {
        if (lastEditTime > 0 && isFocused) {
            delay(5000)
            focusManager.clearFocus()
        }
    }

    // Clear focus + save when keyboard is dismissed (back gesture/swipe)
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(imeVisible) {
        if (!imeVisible && isFocused) {
            focusManager.clearFocus()
            viewModel.saveNow()
        }
    }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        TopAppBar(
            title = { Text("Eintrag", color = MaterialTheme.colorScheme.onBackground) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Zur\u00fcck", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showDeleteDialog(true) }) {
                    Icon(Icons.Rounded.Delete, "L\u00f6schen", tint = NeonRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        uiState.entry?.let { entry ->
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            "${DateTimeFormatter.formatFull(entry.timestamp)} · ${DateTimeFormatter.formatRelative(entry.timestamp)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (!entry.summary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Zusammenfassung",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            entry.summary.lines().filter { it.trimStart().startsWith("\u2022") }.forEach { line ->
                                val bulletText = line.trimStart().removePrefix("\u2022").trim()
                                Row(modifier = Modifier.padding(bottom = 2.dp)) {
                                    Text(
                                        "\u2022 ",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        bulletText,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                entry.entropyScore?.let { score ->
                    GlassCard(
                        glowColor = when {
                            score < 0.33f -> NeonEmerald
                            score < 0.66f -> NeonAmber
                            else -> NeonRed
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Entropie-Score: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.0f%%", score * 100),
                                style = MaterialTheme.typography.titleMedium,
                                color = when {
                                    score < 0.33f -> NeonEmerald
                                    score < 0.66f -> NeonAmber
                                    else -> NeonRed
                                }
                            )
                        }
                    }
                }

                val hasImproved = entry.isImproved && entry.improvedText != null
                val pagerState = rememberPagerState(pageCount = { if (hasImproved) 2 else 1 })
                val coroutineScope = rememberCoroutineScope()

                if (hasImproved) {
                    TabRow(
                        selectedTabIndex = pagerState.currentPage,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        Tab(selected = pagerState.currentPage == 0, onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }) {
                            Text(
                                "Verbessert",
                                modifier = Modifier.padding(8.dp),
                                color = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        Tab(selected = pagerState.currentPage == 1, onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }) {
                            Text(
                                "Original",
                                modifier = Modifier.padding(8.dp),
                                color = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                val textFieldColors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )

                GlassCard {
                    Column {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = hasImproved,
                            beyondViewportPageCount = 1,
                        ) { page ->
                            val isOriginalPage = page == 1
                            if (isOriginalPage) {
                                var editedRawText by remember(entry.rawText) { mutableStateOf(entry.rawText) }
                                TextField(
                                    value = editedRawText,
                                    onValueChange = { newText ->
                                        editedRawText = newText
                                        lastEditTime = System.currentTimeMillis()
                                        viewModel.updateRawText(newText)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { state ->
                                            isFocused = state.isFocused
                                            if (state.isFocused) lastEditTime = System.currentTimeMillis()
                                        },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    colors = textFieldColors,
                                    visualTransformation = searchHighlight
                                )
                            } else {
                                TextField(
                                    value = uiState.editedDisplayText,
                                    onValueChange = {
                                        lastEditTime = System.currentTimeMillis()
                                        viewModel.updateDisplayText(it)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester)
                                        .onFocusChanged { state ->
                                            isFocused = state.isFocused
                                            if (state.isFocused) lastEditTime = System.currentTimeMillis()
                                        },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    colors = textFieldColors,
                                    visualTransformation = searchHighlight
                                )
                            }
                        }
                        if (uiState.isSaving) {
                            Text(
                                "Wird gespeichert...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                if (!entry.adviceCategoryTags.isNullOrBlank()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        entry.adviceCategoryTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    tag,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Aufnahmedauer: ${DateTimeFormatter.formatDuration(entry.audioDurationSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Eintrag l\u00f6schen?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Diesen Eintrag unwiderruflich l\u00f6schen?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteEntry() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed)
                ) { Text("L\u00f6schen") }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.showDeleteDialog(false) }) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
