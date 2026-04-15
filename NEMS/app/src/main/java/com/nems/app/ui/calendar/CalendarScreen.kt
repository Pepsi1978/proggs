package com.nems.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nems.app.data.local.dao.DailyCompletionStat
import com.nems.app.ui.theme.CardSurface
import com.nems.app.ui.theme.GlassBorder
import com.nems.app.ui.theme.GlassWhite
import com.nems.app.ui.theme.NeonCyan
import com.nems.app.ui.theme.StatusGray
import com.nems.app.ui.theme.StatusGreen
import com.nems.app.ui.theme.StatusRed
import com.nems.app.ui.theme.StatusYellow
import com.nems.app.ui.theme.TextSecondary
import com.nems.app.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    onDayClick: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val completionStats by viewModel.completionStats.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.goToToday() },
                containerColor = NeonCyan,
                contentColor = MaterialTheme.colorScheme.background,
            ) {
                Icon(imageVector = Icons.Filled.Today, contentDescription = "Heute")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Month/year header with navigation
            MonthHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { viewModel.navigateMonth(-1) },
                onNextMonth = { viewModel.navigateMonth(1) },
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Glass card wrapping the calendar grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassWhite)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
            ) {
                Column {
                    // Weekday headers
                    WeekDayHeaderRow()

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar day grid
                    CalendarGrid(
                        currentMonth = currentMonth,
                        completionStats = completionStats,
                        onDayClick = onDayClick,
                        getCompletion = viewModel::getCompletionForDate,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Vorheriger Monat",
                tint = NeonCyan,
            )
        }

        Text(
            text = DateUtils.formatMonthYear(currentMonth),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Naechster Monat",
                tint = NeonCyan,
            )
        }
    }
}

@Composable
private fun WeekDayHeaderRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        DateUtils.weekDayHeaders().forEach { dayHeader ->
            Text(
                text = dayHeader,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    completionStats: List<DailyCompletionStat>,
    onDayClick: (String) -> Unit,
    getCompletion: (LocalDate, List<DailyCompletionStat>) -> Float,
) {
    val days = DateUtils.getMonthDays(currentMonth)
    val today = LocalDate.now()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        userScrollEnabled = false,
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(days) { date ->
            if (date == null) {
                // Empty cell for leading/trailing padding
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .padding(2.dp),
                )
            } else {
                val completion = getCompletion(date, completionStats)
                val isFuture = date.isAfter(today)
                val isToday = date == today

                DayCell(
                    date = date,
                    completion = completion,
                    isFuture = isFuture,
                    isToday = isToday,
                    onClick = {
                        onDayClick(date.format(DateUtils.isoDateFormatter))
                    },
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    completion: Float,
    isFuture: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val indicatorColor = when {
        isFuture || completion < 0f -> Color.Transparent
        completion >= 1.0f -> StatusGreen
        completion >= 0.5f -> StatusYellow
        completion > 0f -> StatusRed
        else -> StatusGray
    }

    val showOutline = isFuture || completion < 0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        // Status indicator circle
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    if (showOutline) {
                        Modifier
                            .background(Color.Transparent)
                            .border(1.dp, GlassBorder, CircleShape)
                    } else {
                        Modifier.background(indicatorColor.copy(alpha = 0.25f))
                    },
                ),
        )

        // Today highlight ring
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(2.dp, NeonCyan, CircleShape),
            )
        }

        // Day number text
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isToday -> NeonCyan
                isFuture -> TextSecondary
                else -> MaterialTheme.colorScheme.onBackground
            },
            textAlign = TextAlign.Center,
        )

        // Small dot indicator at the bottom of the cell for non-future days with data
        if (!isFuture && completion >= 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(indicatorColor),
            )
        }
    }
}
