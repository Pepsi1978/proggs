package com.entropyjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.ui.theme.CosmosLayer
import com.entropyjournal.ui.theme.NeonCyan
import com.entropyjournal.ui.theme.NeonEmerald
import com.entropyjournal.ui.theme.NeonAmber
import com.entropyjournal.ui.theme.NeonRed
import com.entropyjournal.ui.theme.TextMuted
import com.entropyjournal.ui.theme.TextSecondary
import com.entropyjournal.util.DateTimeFormatter

enum class TimelinePosition { FIRST, MIDDLE, LAST, ONLY }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineItem(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: TimelinePosition = TimelinePosition.ONLY,
    searchQuery: String = ""
) {
    val highlightColor = if (isSystemInDarkTheme()) Color(0x44FFFFFF) else Color(0xFFFFEB3B)
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = when (entry.moodTag) {
        "positiv" -> NeonEmerald
        "belastend" -> NeonRed
        else -> NeonCyan
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline rail: dot + vertical line
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Vertical line
            if (position != TimelinePosition.ONLY) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(
                            top = if (position == TimelinePosition.FIRST) 6.dp else 0.dp,
                            bottom = if (position == TimelinePosition.LAST) 6.dp else 0.dp
                        )
                        .background(lineColor)
                )
            }
            // Dot
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(10.dp)
                    .background(dotColor, CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Entry card
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick),
            glowColor = dotColor,
            glowIntensity = 0.1f
        ) {
            Column {
                if (!entry.title.isNullOrBlank()) {
                    Text(
                        text = highlightMatches(entry.title, searchQuery, highlightColor),
                        style = MaterialTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = DateTimeFormatter.formatFull(entry.timestamp),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = DateTimeFormatter.formatRelative(entry.timestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = highlightMatches(entry.displayText, searchQuery, highlightColor),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )

                if (!entry.adviceCategoryTags.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        entry.adviceCategoryTags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CosmosLayer
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun highlightMatches(text: String, query: String, highlightColor: Color) = buildAnnotatedString {
    if (query.isBlank()) { append(text); return@buildAnnotatedString }
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var start = 0
    while (start < text.length) {
        val index = lowerText.indexOf(lowerQuery, start)
        if (index == -1) { append(text.substring(start)); break }
        append(text.substring(start, index))
        withStyle(SpanStyle(background = highlightColor)) {
            append(text.substring(index, index + query.length))
        }
        start = index + query.length
    }
}
