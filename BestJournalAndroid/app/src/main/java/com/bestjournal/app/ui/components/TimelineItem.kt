package com.bestjournal.app.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.bestjournal.app.domain.model.JournalEntry
import com.bestjournal.app.ui.theme.LocalIsDarkTheme
import com.bestjournal.app.ui.theme.CosmosLayer
import com.bestjournal.app.ui.theme.NeonCyan
import com.bestjournal.app.ui.theme.NeonEmerald
import com.bestjournal.app.ui.theme.NeonAmber
import com.bestjournal.app.ui.theme.NeonRed
import com.bestjournal.app.ui.theme.TextMuted
import com.bestjournal.app.ui.theme.TextSecondary
import com.bestjournal.app.util.DateTimeFormatter

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
    val highlightColor = if (LocalIsDarkTheme.current) Color(0x44FFFFFF) else Color(0xFFFFEB3B)
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
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = highlightMatches(entry.title, searchQuery, highlightColor),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = "${DateTimeFormatter.formatFull(entry.timestamp)} · ${DateTimeFormatter.formatRelative(entry.timestamp)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (searchQuery.isNotBlank()) {
                    // Show text starting from the first match so the highlight is visible
                    val matchIndex = entry.displayText.lowercase().indexOf(searchQuery.lowercase())
                    val snippetStart = if (matchIndex > 50) {
                        // Find the start of the line or go back ~50 chars
                        val lineStart = entry.displayText.lastIndexOf('\n', matchIndex - 1)
                        if (lineStart >= 0) lineStart + 1 else (matchIndex - 50).coerceAtLeast(0)
                    } else 0
                    val snippetText = if (snippetStart > 0) "\u2026 " + entry.displayText.substring(snippetStart) else entry.displayText
                    Text(
                        text = highlightMatches(snippetText, searchQuery, highlightColor),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = entry.displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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

/**
 * Highlights all occurrences of [query] in [text] with [highlightColor] background.
 * Case-insensitive matching.
 */
internal fun highlightMatches(text: String, query: String, highlightColor: Color) = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var start = 0
    while (start < text.length) {
        val index = lowerText.indexOf(lowerQuery, start)
        if (index == -1) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, index))
        withStyle(SpanStyle(background = highlightColor)) {
            append(text.substring(index, index + query.length))
        }
        start = index + query.length
    }
}
