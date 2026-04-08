package com.entropyjournal.ui.screens.retrospective

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.entropyjournal.data.local.entity.RetrospectiveSummaryEntity
import com.entropyjournal.ui.components.SunMoonToggle
import com.entropyjournal.ui.theme.LocalIsDarkTheme
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

    var selectedSummary by remember { mutableStateOf<RetrospectiveSummaryEntity?>(null) }
    var weeklyExpanded by rememberSaveable { mutableStateOf(false) }
    var monthlyExpanded by rememberSaveable { mutableStateOf(false) }
    var yearlyExpanded by rememberSaveable { mutableStateOf(false) }

    selectedSummary?.let { summary ->
        SummaryDetailDialog(summary = summary, onDismiss = { selectedSummary = null })
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

                Spacer(modifier = Modifier.height(28.dp))

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
                            EmptyHint("Noch keine Wochenrückblicke vorhanden.")
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
                                SummaryEntryCard(
                                    summary = summary,
                                    color = RetrospectiveColors.forWeek(summary.periodIndex),
                                    onClick = { selectedSummary = summary },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            EmptyHint("Noch keine Monatsrückblicke vorhanden.")
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
                                SummaryEntryCard(
                                    summary = summary,
                                    color = RetrospectiveColors.forMonth(summary.periodIndex),
                                    onClick = { selectedSummary = summary },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            EmptyHint("Noch keine Jahresrückblicke vorhanden.")
                        } else {
                            yearly.forEachIndexed { index, summary ->
                                SummaryEntryCard(
                                    summary = summary,
                                    color = RetrospectiveColors.yearColor,
                                    onClick = { selectedSummary = summary },
                                )
                                if (index < yearly.lastIndex) {
                                    Spacer(modifier = Modifier.height(10.dp))
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RetrospectiveColors.categoryCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
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
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }

            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Zuklappen" else "Aufklappen",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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

@Composable
private fun SummaryDetailDialog(summary: RetrospectiveSummaryEntity, onDismiss: () -> Unit) {
    val color =
        when (summary.type) {
            "WEEKLY" -> RetrospectiveColors.forWeek(summary.periodIndex)
            "MONTHLY" -> RetrospectiveColors.forMonth(summary.periodIndex)
            else -> RetrospectiveColors.yearColor
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(color)
                            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)
                ) {
                    val detailTextColor = if (color.luminance() > 0.4f) Color.Black else Color.White
                    Column(modifier = Modifier.padding(top = 8.dp, end = 36.dp)) {
                        Text(
                            text = summary.periodLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = RetrospectiveColors.monthDividerColor,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = summary.title,
                            style = MaterialTheme.typography.titleLarge,
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

                Column(
                    modifier =
                        Modifier.verticalScroll(rememberScrollState()).padding(20.dp).fillMaxWidth()
                ) {
                    Text(
                        text = summary.summaryText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    )
                }
            }
        }
    }
}
