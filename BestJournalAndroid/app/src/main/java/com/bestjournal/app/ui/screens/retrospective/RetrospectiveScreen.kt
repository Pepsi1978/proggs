package com.bestjournal.app.ui.screens.retrospective

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity
import com.bestjournal.app.ui.components.SunMoonToggle
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.util.EdgeTtsPlayer
import java.util.Calendar

object RetrospectiveColors {
    // Dark: #181818 (CardSurface), Light: White — matches journal entry backgrounds
    private val cardDark = Color(0xFF181818)
    private val cardLight = Color.White

    val weekColors: List<Color>
        @Composable get() = List(4) { if (LocalIsDarkTheme.current) cardDark else cardLight }

    val monthDividerColor: Color
        @Composable
        get() =
            if (LocalIsDarkTheme.current) {
                Color(0xFFE07830) // Bold orange for dark mode
            } else {
                Color(0xFF0097A7) // Teal primary — matches journal entry AI titles
            }

    // --- Theme-aware UI colors for the retrospective start page ---
    val headerGradient: List<Color>
        @Composable
        get() =
            if (LocalIsDarkTheme.current) {
                listOf(Color(0xFFE07830), cardDark) // Orange → CardSurface
            } else {
                listOf(Color(0xFFE0F7FA), Color(0xFFFFF8E1)) // Soft teal → warm cream
            }

    val categoryCardColor: Color
        @Composable get() = if (LocalIsDarkTheme.current) cardDark else cardLight

    val categoryIconCircle: Color
        @Composable
        get() =
            if (LocalIsDarkTheme.current) {
                Color(0xFF3D2800) // Deep warm brown (matches primaryContainer)
            } else {
                Color(0xFFB2EBF2) // Light cyan (matches primaryContainer)
            }

    val categoryButtonGradient: List<Color>
        @Composable
        get() =
            if (LocalIsDarkTheme.current) {
                listOf(Color(0xFF4A2810), Color(0xFF181818)) // Subtle warm brown → dark
            } else {
                listOf(categoryCardColor, categoryCardColor) // Solid (no visible gradient)
            }

    val monthColors: List<Color>
        @Composable get() = List(12) { if (LocalIsDarkTheme.current) cardDark else cardLight }

    val yearColor: Color
        @Composable get() = if (LocalIsDarkTheme.current) cardDark else cardLight

    @Composable fun forWeek(weekOfMonth: Int): Color = weekColors[(weekOfMonth - 1).coerceIn(0, 3)]

    @Composable fun forMonth(month: Int): Color = monthColors[(month - 1).coerceIn(0, 11)]
}

@Composable
fun RetrospectiveScreen(viewModel: RetrospectiveViewModel) {
    val weekly by viewModel.weeklySummaries.collectAsState()
    val monthly by viewModel.monthlySummaries.collectAsState()
    val yearly by viewModel.yearlySummaries.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isWaitingForRestore by viewModel.isWaitingForRestore.collectAsState()
    val isProfileSwitch by viewModel.isProfileSwitch.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedSummary by remember { mutableStateOf<RetrospectiveSummaryEntity?>(null) }
    var weeklyExpanded by rememberSaveable { mutableStateOf(false) }
    var monthlyExpanded by rememberSaveable { mutableStateOf(false) }
    var yearlyExpanded by rememberSaveable { mutableStateOf(false) }

    selectedSummary?.let { summary ->
        SummaryDetailDialog(
            summary = summary,
            viewModel = viewModel,
            onDismiss = { selectedSummary = null },
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            // Fixed title bar (does not scroll)
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
                            text = "Rückblick",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SunMoonToggle()
                    }
                }
            }

            // Scrollable content
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(colors = RetrospectiveColors.headerGradient)
                            )
                            .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Dein persönlicher Rückblick",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text =
                                "Manchmal vergessen wir im Alltag, wie viel wir eigentlich erlebt haben. " +
                                    "Dein Tagebuch erinnert sich an alles, an die großen Momente und die kleinen, " +
                                    "stillen Augenblicke, die dein Leben ausmachen.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Schau zurück und entdecke, was dich bewegt hat.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (isWaitingForRestore || isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        com.bestjournal.app.ui.components.ShimmerLoadingEffect(
                            height = 80.dp,
                            cornerRadius = 16.dp,
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Text(
                                "Bitte warten",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                when {
                                    isWaitingForRestore ->
                                        "Backup wird geladen\u2026 Rückblicke starten danach automatisch."
                                    isProfileSwitch ->
                                        "Rückblicke werden nach Profilwechsel automatisch aktualisiert"
                                    else -> "Rückblicke werden erstellt\u2026"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                if (errorMessage != null && !isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text =
                                    "Die KI ist gerade nicht erreichbar \u2014 bitte versuch es später nochmal.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.Button(
                                onClick = { viewModel.retryGeneration() },
                                colors =
                                    androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                            ) {
                                Text("Nochmal versuchen")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            androidx.compose.material3.TextButton(
                                onClick = { viewModel.clearError() }
                            ) {
                                Text("Später", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Wochenrückblick button + expandable entries
                CategoryButton(
                    title = "Wochenrückblick",
                    subtitle = "Die letzten 7 Tage im Überblick",
                    icon = Icons.Rounded.CalendarToday,
                    expanded = weeklyExpanded,
                    onClick = { weeklyExpanded = !weeklyExpanded },
                )
                AnimatedVisibility(
                    visible = weeklyExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        if (weekly.isEmpty()) {
                            EmptyHint(
                                "Noch keine Wochenrückblicke vorhanden.\nMindestens 2 Tagebucheinträge pro Woche nötig."
                            )
                        } else {
                            val monthNames =
                                listOf(
                                    "Januar",
                                    "Februar",
                                    "März",
                                    "April",
                                    "Mai",
                                    "Juni",
                                    "Juli",
                                    "August",
                                    "September",
                                    "Oktober",
                                    "November",
                                    "Dezember",
                                )
                            weekly.forEachIndexed { index, summary ->
                                if (index > 0) {
                                    val prevCal =
                                        Calendar.getInstance().apply {
                                            timeInMillis = weekly[index - 1].startDate
                                        }
                                    val curCal =
                                        Calendar.getInstance().apply {
                                            timeInMillis = summary.startDate
                                        }
                                    if (prevCal.get(Calendar.MONTH) != curCal.get(Calendar.MONTH)) {
                                        val name = monthNames[curCal.get(Calendar.MONTH)]
                                        val year = curCal.get(Calendar.YEAR)
                                        MonthDivider(label = "$name $year")
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                                TimelineSummaryEntry(
                                    summary = summary,
                                    color = RetrospectiveColors.forWeek(summary.periodIndex),
                                    isLast = index == weekly.lastIndex,
                                    onClick = { selectedSummary = summary },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Monatsrückblick button + expandable entries
                CategoryButton(
                    title = "Monatsrückblick",
                    subtitle = "Dein vergangener Monat auf einen Blick",
                    icon = Icons.Rounded.DateRange,
                    expanded = monthlyExpanded,
                    onClick = { monthlyExpanded = !monthlyExpanded },
                )
                AnimatedVisibility(
                    visible = monthlyExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        if (monthly.isEmpty()) {
                            EmptyHint(
                                "Noch keine Monatsrückblicke vorhanden.\nWird aus Wochenrückblicken am Monatsende erstellt."
                            )
                        } else {
                            monthly.forEachIndexed { index, summary ->
                                if (index > 0) {
                                    val prevQuarter = (monthly[index - 1].periodIndex - 1) / 3
                                    val curQuarter = (summary.periodIndex - 1) / 3
                                    if (prevQuarter != curQuarter) {
                                        val year =
                                            Calendar.getInstance()
                                                .apply { timeInMillis = summary.startDate }
                                                .get(Calendar.YEAR)
                                        MonthDivider(label = "${curQuarter + 1}. Quartal $year")
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                }
                                TimelineSummaryEntry(
                                    summary = summary,
                                    color = RetrospectiveColors.forMonth(summary.periodIndex),
                                    isLast = index == monthly.lastIndex,
                                    onClick = { selectedSummary = summary },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Jahresrückblick button + expandable entries
                CategoryButton(
                    title = "Jahresrückblick",
                    subtitle = "Ein ganzes Jahr voller Erinnerungen",
                    icon = Icons.Rounded.CalendarMonth,
                    expanded = yearlyExpanded,
                    onClick = { yearlyExpanded = !yearlyExpanded },
                )
                AnimatedVisibility(
                    visible = yearlyExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        if (yearly.isEmpty()) {
                            EmptyHint(
                                "Noch keine Jahresrückblicke vorhanden.\nWird aus Monatsrückblicken am Jahresende erstellt."
                            )
                        } else {
                            yearly.forEachIndexed { index, summary ->
                                TimelineSummaryEntry(
                                    summary = summary,
                                    color = RetrospectiveColors.yearColor,
                                    isLast = index == yearly.lastIndex,
                                    onClick = { selectedSummary = summary },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/** Wraps a SummaryEntryCard with a timeline dot + connecting line on the left. */
@Composable
private fun TimelineSummaryEntry(
    summary: RetrospectiveSummaryEntity,
    color: Color,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline dot + vertical connecting line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp).padding(top = 8.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(8.dp)
                        .clip(CircleShape)
                        .background(RetrospectiveColors.monthDividerColor)
            )
            if (!isLast) {
                // Connecting line stretches to fill remaining height of the card
                Box(
                    modifier =
                        Modifier.width(2.dp)
                            .height(80.dp) // approximate card height; line is visual only
                            .background(RetrospectiveColors.monthDividerColor.copy(alpha = 0.2f))
                )
            }
        }
        // The actual card fills the remaining width
        SummaryEntryCard(summary = summary, color = color, onClick = onClick)
    }
}

@Composable
private fun CategoryButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isDark) Color.Transparent else RetrospectiveColors.categoryCardColor
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .then(
                        if (isDark)
                            Modifier.background(
                                Brush.verticalGradient(RetrospectiveColors.categoryButtonGradient)
                            )
                        else Modifier
                    )
                    .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(52.dp)
                        .clip(CircleShape)
                        .background(RetrospectiveColors.categoryIconCircle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(28.dp),
                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        if (isDark) Color.White.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
                tint =
                    if (isDark) Color.White.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun SummaryEntryCard(
    summary: RetrospectiveSummaryEntity,
    color: Color,
    onClick: () -> Unit,
) {
    val textColor = if (color.luminance() > 0.4f) Color.Black else Color.White
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = summary.periodLabel,
                style = MaterialTheme.typography.labelMedium,
                color = RetrospectiveColors.monthDividerColor,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summary.title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summary.summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MonthDivider(label: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(2.5.dp)
                    .background(RetrospectiveColors.monthDividerColor)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = RetrospectiveColors.monthDividerColor,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(2.5.dp)
                    .background(RetrospectiveColors.monthDividerColor)
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
    )
}

private fun Modifier.drawVerticalScrollbar(scrollState: ScrollState, color: Color): Modifier =
    drawWithContent {
        drawContent()
        val viewHeight = size.height
        val contentHeight = scrollState.maxValue.toFloat() + viewHeight
        if (contentHeight > viewHeight && scrollState.maxValue > 0) {
            val barWidth = 4.dp.toPx()
            val scrollbarHeight =
                (viewHeight * viewHeight / contentHeight).coerceIn(32.dp.toPx(), viewHeight * 0.15f)
            val scrollbarY =
                scrollState.value.toFloat() / scrollState.maxValue * (viewHeight - scrollbarHeight)
            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - barWidth - 2.dp.toPx(), scrollbarY),
                size = Size(barWidth, scrollbarHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }

@Composable
private fun SummaryDetailDialog(
    summary: RetrospectiveSummaryEntity,
    viewModel: RetrospectiveViewModel,
    onDismiss: () -> Unit,
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var fullScreenPhotoPath by remember { mutableStateOf<String?>(null) }
    val tts = remember { EdgeTtsPlayer(context) }
    val photos by viewModel.currentPhotos.collectAsState()
    val parsed = remember(summary.summaryText) { parseRetrospectiveText(summary.summaryText) }

    // Load photos for this retrospective period when the dialog opens
    LaunchedEffect(summary.startDate, summary.endDate) {
        viewModel.loadPhotosForPeriod(summary.startDate, summary.endDate)
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    Dialog(
        onDismissRequest = {
            tts.stop()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Colored header with gradient in dark mode
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .then(
                                if (isDark)
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            RetrospectiveColors.categoryButtonGradient
                                        )
                                    )
                                else Modifier.background(Color.White)
                            )
                            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 20.dp)
                ) {
                    val detailTextColor = if (isDark) Color.White else Color.Black
                    Column(modifier = Modifier.padding(end = 44.dp)) {
                        Text(
                            text = summary.periodLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = RetrospectiveColors.monthDividerColor,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = detailTextColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Schließen",
                            tint = detailTextColor.copy(alpha = 0.8f),
                        )
                    }
                }

                // Body — structured rendering with bullet summary + timeline sections
                val bodyScrollState = rememberScrollState()
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .drawVerticalScrollbar(
                                bodyScrollState,
                                color =
                                    if (isDark) Color.White.copy(alpha = 0.4f)
                                    else Color.Black.copy(alpha = 0.3f),
                            )
                            .verticalScroll(bodyScrollState)
                            .padding(horizontal = 20.dp)
                            .padding(top = 24.dp, bottom = 120.dp)
                ) {
                    // Bullet point summary card
                    if (parsed.bulletPoints.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor =
                                        if (isDark) Color(0xFF1E1E1E) else Color(0xFFF5F5F5)
                                ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Auf einen Blick",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = RetrospectiveColors.monthDividerColor,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                parsed.bulletPoints.forEach { point ->
                                    Row(modifier = Modifier.padding(bottom = 6.dp)) {
                                        Text(
                                            "\u2022 ",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = RetrospectiveColors.monthDividerColor,
                                        )
                                        Text(
                                            point,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Timeline sections with icons
                    if (parsed.sections.isNotEmpty()) {
                        parsed.sections.forEachIndexed { index, section ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Timeline: icon circle + vertical line
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(44.dp),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier.size(36.dp)
                                                .clip(CircleShape)
                                                .background(RetrospectiveColors.categoryIconCircle),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            section.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = RetrospectiveColors.monthDividerColor,
                                        )
                                    }
                                    if (index < parsed.sections.lastIndex) {
                                        Box(
                                            modifier =
                                                Modifier.width(2.dp)
                                                    .height(20.dp)
                                                    .background(
                                                        RetrospectiveColors.monthDividerColor.copy(
                                                            alpha = 0.3f
                                                        )
                                                    )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Section content
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        section.heading,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RetrospectiveColors.monthDividerColor,
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        section.body,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color =
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                                    )
                                    if (index < parsed.sections.lastIndex) {
                                        Spacer(modifier = Modifier.height(20.dp))
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback: plain text if parsing found no sections (old format)
                        Text(
                            text = summary.summaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                        )
                    }

                    // Photos & Videos section — shown after timeline, before divider
                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            "Fotos & Videos",
                            style = MaterialTheme.typography.titleSmall,
                            color = RetrospectiveColors.monthDividerColor,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier =
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            photos.forEach { photo ->
                                AsyncImage(
                                    model = photo.filePath,
                                    contentDescription = if (photo.isVideo) "Video" else "Foto",
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier.size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { fullScreenPhotoPath = photo.filePath },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divider + action buttons
                    Box(
                        modifier =
                            Modifier.fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons: Speaker + Share
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                if (isSpeaking) {
                                    tts.stop()
                                    isSpeaking = false
                                } else {
                                    isSpeaking = true
                                    val speakText =
                                        if (parsed.sections.isNotEmpty())
                                            parsed.sections.joinToString("\n\n") {
                                                "${it.heading}.\n${it.body}"
                                            }
                                        else summary.summaryText
                                    tts.speak(speakText) { isSpeaking = false }
                                }
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                contentDescription = if (isSpeaking) "Stoppen" else "Vorlesen",
                                tint = Color(0xFFE07830),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        IconButton(
                            onClick = { showShareDialog = true },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                contentDescription = "Teilen",
                                tint = Color(0xFFE07830),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // Share dialog with checkboxes — like diary entry sharing
    if (showShareDialog) {
        var shareText by remember { mutableStateOf(true) }
        val selectedPhotos = remember { List(photos.size) { true }.toMutableStateList() }

        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Rückblick teilen", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Text checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { shareText = !shareText },
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = shareText,
                            onCheckedChange = { shareText = it },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rückblick-Text", color = MaterialTheme.colorScheme.onSurface)
                    }

                    // Photo/Video checkboxes
                    if (photos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Fotos & Videos:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        photos.forEachIndexed { index, photo ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier =
                                    Modifier.fillMaxWidth().clickable {
                                        selectedPhotos[index] = !selectedPhotos[index]
                                    },
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = selectedPhotos[index],
                                    onCheckedChange = { selectedPhotos[index] = it },
                                )
                                AsyncImage(
                                    model = photo.filePath,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier.size(48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .padding(end = 8.dp),
                                )
                                Text(
                                    if (photo.isVideo) "Video" else "Foto",
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        val textContent = if (shareText) buildShareText(summary, parsed) else null
                        val photoUris =
                            photos
                                .filterIndexed { i, _ ->
                                    i < selectedPhotos.size && selectedPhotos[i]
                                }
                                .map { photo ->
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        java.io.File(photo.filePath),
                                    )
                                }
                        val intent =
                            if (photoUris.isNotEmpty()) {
                                android.content
                                    .Intent(android.content.Intent.ACTION_SEND_MULTIPLE)
                                    .apply {
                                        type = "image/*"
                                        putParcelableArrayListExtra(
                                            android.content.Intent.EXTRA_STREAM,
                                            ArrayList(photoUris),
                                        )
                                        if (textContent != null) {
                                            putExtra(android.content.Intent.EXTRA_TEXT, textContent)
                                        }
                                        addFlags(
                                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                        )
                                    }
                            } else if (textContent != null) {
                                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, textContent)
                                }
                            } else null

                        if (intent != null) {
                            context.startActivity(
                                android.content.Intent.createChooser(intent, "Rückblick teilen")
                            )
                        }
                        showShareDialog = false
                    },
                    colors =
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE07830)
                        ),
                ) {
                    Text("Teilen")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showShareDialog = false }) {
                    Text("Abbrechen", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }

    // Full-screen photo/video viewer with pinch-to-zoom and paging
    fullScreenPhotoPath?.let { initialPath ->
        val initialPage = photos.indexOfFirst { it.filePath == initialPath }.coerceAtLeast(0)
        val pagerState =
            androidx.compose.foundation.pager.rememberPagerState(initialPage = initialPage) {
                photos.size
            }
        var currentPageZoomed by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = { fullScreenPhotoPath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                androidx.compose.foundation.pager.HorizontalPager(
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
                                                    val mx = 1000f * (scale - 1)
                                                    val my = 1500f * (scale - 1)
                                                    offsetX = offsetX.coerceIn(-mx, mx)
                                                    offsetY = offsetY.coerceIn(-my, my)
                                                } else {
                                                    offsetX = 0f
                                                    offsetY = 0f
                                                }
                                                event.changes.forEach { it.consume() }
                                            } else if (pressed == 1 && scale > 1f) {
                                                val pan = event.calculatePan()
                                                offsetX += pan.x
                                                offsetY += pan.y
                                                val mx = 1000f * (scale - 1)
                                                val my = 1500f * (scale - 1)
                                                offsetX = offsetX.coerceIn(-mx, mx)
                                                offsetY = offsetY.coerceIn(-my, my)
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
                                model = java.io.File(photos[page].filePath),
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
                    Icon(Icons.Rounded.Close, "Schließen", tint = Color.White)
                }
            }
        }
    }
}

private fun buildShareText(
    summary: com.bestjournal.app.data.local.entity.RetrospectiveSummaryEntity,
    parsed: ParsedRetrospective,
): String = buildString {
    append("Rückblick von der BestJournal App")
    append("\n")
    append(summary.periodLabel)
    append(" \u2014 ")
    append(summary.title)
    append(
        "\n\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"
    )
    if (parsed.bulletPoints.isNotEmpty()) {
        append("\n\nAuf einen Blick:")
        parsed.bulletPoints.forEach { append("\n\u2022 $it") }
    }
    if (parsed.sections.isNotEmpty()) {
        parsed.sections.forEach { section ->
            append("\n\n\u2728 ${section.heading}")
            append("\n${section.body}")
        }
    } else {
        append("\n\n${summary.summaryText}")
    }
}
