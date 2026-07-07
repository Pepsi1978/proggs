package com.nems.app.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nems.app.data.local.dao.DailyCompletionStat
import com.nems.app.data.local.dao.MissedSupplement
import com.nems.app.ui.theme.NeonEmerald
import com.nems.app.ui.theme.StatusRed
import com.nems.app.ui.theme.StatusYellow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val weeklyStats by viewModel.weeklyStats.collectAsState()
    val mostMissed by viewModel.mostMissed.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Screen title
        Text(
            text = "Statistik",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Streak card
        StreakCard(streak = streak)

        // Weekly bar chart
        WeeklyChartCard(stats = weeklyStats)

        // Most missed supplements
        if (mostMissed.isNotEmpty()) {
            MostMissedCard(supplements = mostMissed)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun StreakCard(streak: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Aktuelle Serie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "$streak",
            fontSize = 56.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 60.sp,
        )

        Text(
            text = if (streak == 1) "Tag in Folge mit 100%" else "Tage in Folge mit 100%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (streak == 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nimm heute alle Supplemente — starte deine Serie!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeeklyChartCard(stats: List<DailyCompletionStat>) {
    // Build a map of date -> stat for the last 7 days
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val today = LocalDate.now()
    val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val statByDate = stats.associateBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Text(
            text = "Letzte 7 Tage",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            last7Days.forEach { date ->
                val stat = statByDate[date.format(formatter)]
                val pct = if (stat != null && stat.total > 0) {
                    stat.takenCount.toFloat() / stat.total.toFloat()
                } else {
                    0f
                }
                val isToday = date == today

                DayBar(
                    modifier = Modifier.weight(1f),
                    dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.GERMAN)
                        .take(2)
                        .replaceFirstChar { it.uppercase() },
                    completionPct = pct,
                    hasStat = stat != null,
                    isToday = isToday,
                )
            }
        }

        // Legend
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LegendItem(color = NeonEmerald, label = "100%")
            LegendItem(color = StatusYellow, label = "50-99%")
            LegendItem(color = StatusRed, label = "<50%")
            LegendItem(color = MaterialTheme.colorScheme.outlineVariant, label = "Keine Daten")
        }
    }
}

@Composable
private fun DayBar(
    modifier: Modifier = Modifier,
    dayLabel: String,
    completionPct: Float,
    hasStat: Boolean,
    isToday: Boolean,
) {
    val barColor = when {
        !hasStat -> MaterialTheme.colorScheme.outlineVariant
        completionPct >= 1.0f -> NeonEmerald
        completionPct >= 0.5f -> StatusYellow
        else -> StatusRed
    }
    val maxBarHeight = 80.dp
    val minBarHeight = 4.dp
    val barHeight = if (hasStat) {
        maxBarHeight * completionPct.coerceAtLeast(0.05f)
    } else {
        minBarHeight
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Percentage label above bar
        if (hasStat && completionPct > 0f) {
            Text(
                text = "${(completionPct * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = barColor,
                fontSize = 9.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(
                    if (hasStat) {
                        Brush.verticalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.6f)),
                        )
                    } else {
                        Brush.verticalGradient(colors = listOf(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.outlineVariant))
                    },
                ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Day label
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            fontSize = 10.sp,
        )

        // "Heute" indicator
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
        )
    }
}

@Composable
private fun MostMissedCard(supplements: List<MissedSupplement>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = StatusYellow,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Am haeufigsten verpasst",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = StatusYellow,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Text(
            text = "Letzte 30 Tage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        supplements.forEachIndexed { index, supplement ->
            MissedSupplementRow(
                rank = index + 1,
                name = supplement.name,
                missedCount = supplement.missedCount,
                isLast = index == supplements.lastIndex,
            )
        }
    }
}

@Composable
private fun MissedSupplementRow(
    rank: Int,
    name: String,
    missedCount: Int,
    isLast: Boolean,
) {
    val rankColor = when (rank) {
        1 -> StatusRed
        2 -> MaterialTheme.colorScheme.tertiary
        3 -> StatusYellow
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Rank badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = rankColor,
            )
        }

        // Supplement name
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        // Missed count
        Text(
            text = "$missedCount x verpasst",
            style = MaterialTheme.typography.bodySmall,
            color = rankColor,
            fontWeight = FontWeight.SemiBold,
        )
    }

    if (!isLast) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}
