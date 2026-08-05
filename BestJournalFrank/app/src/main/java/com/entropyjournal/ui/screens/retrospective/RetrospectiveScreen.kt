package com.entropyjournal.ui.screens.retrospective

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.entropyjournal.util.rememberHapticAction
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity
import com.entropyjournal.ui.components.SunMoonToggle
import com.entropyjournal.ui.components.JournalShareAttachment
import com.entropyjournal.ui.components.JournalSharePayload
import com.entropyjournal.ui.components.JournalShareSheet
import com.entropyjournal.ui.theme.LocalIsDarkTheme
import com.entropyjournal.ui.theme.scaledBody
import com.entropyjournal.ui.theme.scaledHeading
import com.entropyjournal.util.TtsManager
import java.util.Calendar

object RetrospectiveColors {
    // Theme-aware: Card-Hintergrund kommt aus MaterialTheme.colorScheme.surface,
    // dadurch tragen die Karten denselben Profil-Hauch wie der Rest der App
    // (siehe profileColorScheme in Theme.kt).
    private val cardDark: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    private val cardLight: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val weekColors: List<Color>
        @Composable get() {
            val c = if (LocalIsDarkTheme.current) cardDark else cardLight
            return List(4) { c }
        }

    val monthDividerColor: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary

    // --- Theme-aware UI colors for the retrospective start page ---
    // Header-Gradient bekommt jetzt die Profil-Akzentfarbe oben und blendet
    // dezent in den App-Hintergrund unten — dadurch faerbt sich der Rueckblick
    // bei jedem Profil-Wechsel mit um.
    val headerGradient: List<Color>
        @Composable
        get() {
            val accent = MaterialTheme.colorScheme.primary
            val surface = MaterialTheme.colorScheme.surface
            val background = MaterialTheme.colorScheme.background
            return if (LocalIsDarkTheme.current) {
                listOf(
                    accent.copy(alpha = 0.35f).compositeOver(background),
                    accent.copy(alpha = 0.22f).compositeOver(background),
                    accent.copy(alpha = 0.10f).compositeOver(background),
                    surface,
                )
            } else {
                listOf(
                    accent.copy(alpha = 0.25f).compositeOver(background),
                    accent.copy(alpha = 0.15f).compositeOver(background),
                    accent.copy(alpha = 0.06f).compositeOver(background),
                    surface,
                )
            }
        }

    val categoryCardColor: Color
        @Composable get() = if (LocalIsDarkTheme.current) cardDark else cardLight

    val categoryIconCircle: Color
        @Composable
        get() =
            MaterialTheme.colorScheme.primary
                .copy(alpha = if (LocalIsDarkTheme.current) 0.18f else 0.14f)
                .compositeOver(MaterialTheme.colorScheme.surface)

    val categoryButtonGradient: List<Color>
        @Composable
        get() {
            val accent = MaterialTheme.colorScheme.primary
            val surface = MaterialTheme.colorScheme.surface
            return if (LocalIsDarkTheme.current) {
                listOf(accent.copy(alpha = 0.22f).compositeOver(surface), surface)
            } else {
                listOf(accent.copy(alpha = 0.12f).compositeOver(surface), surface)
            }
        }

    val monthColors: List<Color>
        @Composable get() = List(12) { if (LocalIsDarkTheme.current) cardDark else cardLight }

    val yearColor: Color
        @Composable get() = if (LocalIsDarkTheme.current) cardDark else cardLight

    @Composable fun forWeek(weekOfMonth: Int): Color = weekColors[(weekOfMonth - 1).coerceIn(0, 3)]

    @Composable fun forMonth(month: Int): Color = monthColors[(month - 1).coerceIn(0, 11)]
}

@Composable
private fun GoldenHairline() {
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.tertiary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                            Color.Transparent,
                        )
                    )
                )
    )
}

@Composable
private fun RetrospectiveInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "So entstehen Rückblicke",
                    modifier = Modifier.weight(1f),
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RetrospectiveInfoRow(
                    icon = Icons.Rounded.CalendarToday,
                    text = "Wochenrückblick: sonntags, ab 2 Einträgen pro Woche",
                )
                RetrospectiveInfoRow(
                    icon = Icons.Rounded.DateRange,
                    text = "Monatsrückblick: am Monatsende aus den Wochenrückblicken",
                )
                RetrospectiveInfoRow(
                    icon = Icons.Rounded.CalendarMonth,
                    text = "Jahresrückblick: am 31. Dezember aus allen Monaten",
                )
                GoldenHairline()
                Text(
                    "Die KI fasst zusammen, was dich bewegt hat. Tippe einen Rückblick an, " +
                        "um den ganzen Text, Medien, Vorlesen und Teilen zu öffnen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Verstanden", color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}

@Composable
private fun RetrospectiveInfoRow(icon: ImageVector, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun RetrospectiveScreen(viewModel: RetrospectiveViewModel) {
    val doHaptic = rememberHapticAction()
    val weekly by viewModel.weeklySummaries.collectAsStateWithLifecycle()
    val monthly by viewModel.monthlySummaries.collectAsStateWithLifecycle()
    val yearly by viewModel.yearlySummaries.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val isWaitingForRestore by viewModel.isWaitingForRestore.collectAsStateWithLifecycle()
    val isProfileSwitch by viewModel.isProfileSwitch.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var selectedSummary by remember { mutableStateOf<RetrospectiveSummaryEntity?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var weeklyExpanded by rememberSaveable { mutableStateOf(true) }
    var monthlyExpanded by rememberSaveable { mutableStateOf(false) }
    var yearlyExpanded by rememberSaveable { mutableStateOf(false) }

    selectedSummary?.let { summary ->
        SummaryDetailDialog(
            summary = summary,
            viewModel = viewModel,
            onDismiss = { selectedSummary = null },
        )
    }

    if (showInfoDialog) {
        RetrospectiveInfoDialog(onDismiss = { showInfoDialog = false })
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Fixed title bar (does not scroll)
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Rückblick",
                            style =
                                MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 27.sp.scaledHeading,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        SunMoonToggle()
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                GoldenHairline()
                val lastUpdated = remember(weekly, monthly, yearly) {
                    viewModel.getLastUpdatedText()
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (lastUpdated != null) {
                        Text(
                            text = lastUpdated,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border =
                            BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            ),
                    ) {
                        IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                showInfoDialog = true
                            },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Info,
                                contentDescription = "So entstehen Rückblicke",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }
            }

            // Scrollable content
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isWaitingForRestore || isGenerating) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        com.entropyjournal.ui.components.ShimmerLoadingEffect(
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(colors = RetrospectiveColors.headerGradient)
                            )
                            .border(
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                ),
                                RoundedCornerShape(22.dp),
                            )
                            .padding(horizontal = 22.dp, vertical = 26.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            border =
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                ),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(72.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = RetrospectiveColors.monthDividerColor,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Dein persönlicher Rückblick",
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 22.sp.scaledHeading,
                                    fontWeight = FontWeight.Bold,
                                ),
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
                            color = RetrospectiveColors.monthDividerColor,
                            textAlign = TextAlign.Center,
                        )
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
                                        containerColor = RetrospectiveColors.monthDividerColor
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

                Spacer(modifier = Modifier.height(20.dp))

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
                            // Wochen werden chronologisch nach (Monat, Jahr) gruppiert.
                            // Pro Monatsgruppe wird eine eigene ContinuousTimelineSection
                            // gerendert, sodass die Linie nicht ueber Monatsgrenzen
                            // hinausgeht.
                            val weeklyGroups = groupSummariesByMonth(weekly)
                            weeklyGroups.forEachIndexed { groupIndex, group ->
                                if (groupIndex > 0) {
                                    val cal =
                                        Calendar.getInstance().apply {
                                            timeInMillis = group.first().endDate
                                        }
                                    val name = monthNames[cal.get(Calendar.MONTH)]
                                    val year = cal.get(Calendar.YEAR)
                                    MonthDivider(label = "$name $year")
                                }
                                ContinuousTimelineSection(
                                    entryCount = group.size,
                                    lineColor =
                                        RetrospectiveColors.monthDividerColor.copy(alpha = 0.2f),
                                    dotColor = RetrospectiveColors.monthDividerColor,
                                ) {
                                    group.forEachIndexed { index, summary ->
                                        if (index > 0) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                        SummaryEntryCard(
                                            summary = summary,
                                            color =
                                                RetrospectiveColors.forWeek(summary.periodIndex),
                                            onClick = { selectedSummary = summary },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            // Monate werden chronologisch nach Jahr gruppiert: Linie laeuft
                            // von Januar bis Dezember eines Jahres durch — beim Jahres-
                            // wechsel beginnt eine neue Section. Quartalstrenner bleiben
                            // als sichtbare Beschriftung INNERHALB der Section, die Linie
                            // laeuft durch sie hindurch.
                            val monthlyGroups = groupSummariesByYear(monthly)
                            monthlyGroups.forEachIndexed { groupIndex, group ->
                                if (groupIndex > 0) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                ContinuousTimelineSection(
                                    entryCount = group.size,
                                    lineColor =
                                        RetrospectiveColors.monthDividerColor.copy(alpha = 0.2f),
                                    dotColor = RetrospectiveColors.monthDividerColor,
                                ) {
                                    group.forEachIndexed { index, summary ->
                                        if (index > 0) {
                                            val prevQuarter =
                                                (group[index - 1].periodIndex - 1) / 3
                                            val curQuarter = (summary.periodIndex - 1) / 3
                                            if (prevQuarter != curQuarter) {
                                                val year =
                                                    Calendar.getInstance()
                                                        .apply {
                                                            timeInMillis = summary.startDate
                                                        }
                                                        .get(Calendar.YEAR)
                                                MonthDivider(
                                                    label = "${curQuarter + 1}. Quartal $year"
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(10.dp))
                                            }
                                        }
                                        SummaryEntryCard(
                                            summary = summary,
                                            color =
                                                RetrospectiveColors.forMonth(summary.periodIndex),
                                            onClick = { selectedSummary = summary },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            ContinuousTimelineSection(
                                entryCount = yearly.size,
                                lineColor =
                                    RetrospectiveColors.monthDividerColor.copy(alpha = 0.2f),
                                dotColor = RetrospectiveColors.monthDividerColor,
                            ) {
                                yearly.forEachIndexed { index, summary ->
                                    if (index > 0) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    SummaryEntryCard(
                                        summary = summary,
                                        color = RetrospectiveColors.yearColor,
                                        onClick = { selectedSummary = summary },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Gruppiert eine chronologisch sortierte Liste von Wochen-Summaries konsekutiv
 * nach (Monat, Jahr). Aufeinanderfolgende Eintraege im selben Monat landen in
 * derselben Untergruppe — sobald der Monat wechselt, beginnt eine neue Gruppe.
 * Wir gehen ueber [endDate] (Sonntag der Woche), damit Cross-Month-Wochen
 * unter dem spaeteren Monat erscheinen.
 */
private fun groupSummariesByMonth(
    items: List<RetrospectiveSummaryEntity>,
): List<List<RetrospectiveSummaryEntity>> {
    val result = mutableListOf<MutableList<RetrospectiveSummaryEntity>>()
    items.forEach { summary ->
        val cal = Calendar.getInstance().apply { timeInMillis = summary.endDate }
        val curKey = cal.get(Calendar.MONTH) to cal.get(Calendar.YEAR)
        val lastEntry = result.lastOrNull()?.lastOrNull()
        val sameGroup =
            if (lastEntry == null) false
            else {
                val lastCal =
                    Calendar.getInstance().apply { timeInMillis = lastEntry.endDate }
                val lastKey = lastCal.get(Calendar.MONTH) to lastCal.get(Calendar.YEAR)
                lastKey == curKey
            }
        if (sameGroup) {
            result.last().add(summary)
        } else {
            result.add(mutableListOf(summary))
        }
    }
    return result
}

/**
 * Gruppiert eine chronologisch sortierte Liste von Monats-/Jahres-Summaries
 * konsekutiv nach Jahr (basierend auf [startDate]).
 */
private fun groupSummariesByYear(
    items: List<RetrospectiveSummaryEntity>,
): List<List<RetrospectiveSummaryEntity>> {
    val result = mutableListOf<MutableList<RetrospectiveSummaryEntity>>()
    items.forEach { summary ->
        val cal = Calendar.getInstance().apply { timeInMillis = summary.startDate }
        val curYear = cal.get(Calendar.YEAR)
        val lastEntry = result.lastOrNull()?.lastOrNull()
        val sameGroup =
            if (lastEntry == null) false
            else {
                val lastCal =
                    Calendar.getInstance().apply { timeInMillis = lastEntry.startDate }
                lastCal.get(Calendar.YEAR) == curYear
            }
        if (sameGroup) {
            result.last().add(summary)
        } else {
            result.add(mutableListOf(summary))
        }
    }
    return result
}

/**
 * Wickelt eine Liste von Eintragskarten in eine durchgehende Timeline-Rail.
 *
 * Die Rail (links, 24dp breit) zeichnet eine durchgehende vertikale Linie von
 * 10 % bis 90 % der Section-Hoehe und N gleichmaessig verteilte Punkte:
 *   - N=1: ein Punkt mittig (50 %)
 *   - N=2: 10 % und 90 %
 *   - N=3: 10 %, 50 %, 90 %
 *   - N=4: 10 %, 36,7 %, 63,3 %, 90 %
 *   - allgemein: pos_i = 10 % + i * 80 % / (N-1)
 *
 * Die Karten + Spacer/Divider werden als [content] uebergeben und liegen rechts
 * neben der Rail in einer Column. Damit ist die Linie nicht mehr unterbrochen,
 * auch wenn zwischen den Karten Spacer oder MonthDivider stehen.
 */
@Composable
private fun ContinuousTimelineSection(
    entryCount: Int,
    lineColor: Color,
    dotColor: Color,
    modifier: Modifier = Modifier,
    railWidth: Dp = 20.dp,
    dotSize: Dp = 8.dp,
    lineThickness: Dp = 2.dp,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 12.dp).height(IntrinsicSize.Min),
    ) {
        Canvas(
            modifier = Modifier.width(railWidth).fillMaxHeight(),
        ) {
            if (entryCount <= 0) return@Canvas
            val cx = size.width / 2f
            val topY = size.height * 0.1f
            val bottomY = size.height * 0.9f
            val strokePx = lineThickness.toPx()
            val dotRadiusPx = dotSize.toPx() / 2f
            if (entryCount > 1) {
                drawLine(
                    color = lineColor,
                    start = Offset(cx, topY),
                    end = Offset(cx, bottomY),
                    strokeWidth = strokePx,
                )
            }
            for (i in 0 until entryCount) {
                val frac =
                    if (entryCount == 1) 0.5f
                    else 0.1f + 0.8f * i / (entryCount - 1)
                drawCircle(
                    color = dotColor,
                    radius = dotRadiusPx,
                    center = Offset(cx, size.height * frac),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            content()
        }
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = if (expanded) 0.45f else 0.28f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .background(Brush.verticalGradient(RetrospectiveColors.categoryButtonGradient))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(44.dp)
                        .clip(CircleShape)
                        .background(RetrospectiveColors.categoryIconCircle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(21.dp),
                    tint = RetrospectiveColors.monthDividerColor,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp.scaledBody),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
                tint =
                    if (expanded) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        border =
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summary.summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        GoldenHairline()
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = label,
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                ),
            color = RetrospectiveColors.monthDividerColor,
        )
        Spacer(modifier = Modifier.height(10.dp))
        GoldenHairline()
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
    val context = LocalContext.current
    val doHaptic = rememberHapticAction()
    var isSpeaking by remember { mutableStateOf(false) }
    var isTtsLoading by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var fullScreenPhotoPath by remember { mutableStateOf<String?>(null) }
    val tts = remember { TtsManager(context) }
    val photos by viewModel.currentPhotos.collectAsStateWithLifecycle()
    val parsed = remember(summary.summaryText) { parseRetrospectiveText(summary.summaryText) }

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
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    RetrospectiveColors.categoryButtonGradient
                                )
                            )
                            .padding(start = 18.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(end = 44.dp)) {
                        Text(
                            text = summary.title,
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 21.sp.scaledHeading,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary.periodLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = "Schließen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                GoldenHairline()

                // Body — structured rendering with bullet summary + timeline sections
                val bodyScrollState = rememberScrollState()
                Column(
                    modifier =
                        Modifier.weight(1f)
                            .fillMaxWidth()
                            .drawVerticalScrollbar(
                                bodyScrollState,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            )
                            .verticalScroll(bodyScrollState)
                            .padding(horizontal = 18.dp)
                            .padding(top = 24.dp, bottom = 120.dp)
                ) {
                    // Bullet point summary card
                    if (parsed.bulletPoints.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                            border =
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                ),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Auf einen Blick",
                                    style =
                                        MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    color = RetrospectiveColors.monthDividerColor,
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
                                        style =
                                            MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                            ),
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

                    // Photos & Videos section
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
                                Box {
                                    coil3.compose.AsyncImage(
                                        model = photo.filePath,
                                        contentDescription = if (photo.isVideo) "Video" else "Foto",
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier =
                                            Modifier.size(120.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { fullScreenPhotoPath = photo.filePath },
                                    )
                                    if (photo.isVideo) {
                                        Icon(
                                            Icons.Rounded.PlayCircle,
                                            contentDescription = "Video abspielen",
                                            modifier = Modifier.size(40.dp).align(Alignment.Center),
                                            tint = Color.White.copy(alpha = 0.9f),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Divider + action buttons
                    GoldenHairline()

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action buttons: Speaker + Share
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            border =
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                ),
                        ) {
                            IconButton(
                            onClick = {
                                doHaptic(HapticFeedbackType.LongPress)
                                if (isSpeaking || isTtsLoading) {
                                    tts.stop()
                                    isSpeaking = false
                                    isTtsLoading = false
                                } else {
                                    isTtsLoading = true
                                    isSpeaking = true
                                    val speakText =
                                        if (parsed.sections.isNotEmpty())
                                            parsed.sections.joinToString("\n\n") {
                                                "${it.heading}.\n${it.body}"
                                            }
                                        else summary.summaryText
                                    val started = tts.speak(
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
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.VolumeUp,
                                contentDescription = if (isSpeaking) "Stoppen" else "Vorlesen",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                            border =
                                BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                ),
                        ) {
                            IconButton(
                            onClick = { doHaptic(HapticFeedbackType.LongPress); showShareDialog = true },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                contentDescription = "Teilen",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        }
                    }

                    // TTS loading indicator
                    if (isTtsLoading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Bitte warten, Text-to-Speech wird erzeugt\u2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        var shareText by remember { mutableStateOf(true) }
        val selectedPhotos =
            remember(photos.map { it.id }) { List(photos.size) { true }.toMutableStateList() }
        val selectedMedia = photos.filterIndexed { index, _ -> selectedPhotos[index] }
        JournalShareSheet(
            payload =
                JournalSharePayload(
                    title = summary.title,
                    subtitle = "Rückblick · ${summary.periodLabel}",
                    text = if (shareText) buildShareText(summary, parsed) else "",
                    attachments =
                        selectedMedia.map { photo ->
                            JournalShareAttachment(
                                uri =
                                    androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        java.io.File(photo.filePath),
                                    ),
                                isVideo = photo.isVideo,
                            )
                        },
                ),
            onDismiss = { showShareDialog = false },
            selectionContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { shareText = !shareText },
                ) {
                    androidx.compose.material3.Checkbox(
                        checked = shareText,
                        onCheckedChange = { shareText = it },
                    )
                    Text("Rückblick-Text", color = MaterialTheme.colorScheme.onSurface)
                }
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
                        coil3.compose.AsyncImage(
                            model = photo.filePath,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (photo.isVideo) "Video ${index + 1}" else "Foto ${index + 1}",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
                            coil3.compose.AsyncImage(
                                model = java.io.File(photos[page].filePath),
                                contentDescription = "Foto ${page + 1}",
                                modifier =
                                    Modifier.fillMaxSize().graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    },
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
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
    summary: com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity,
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
