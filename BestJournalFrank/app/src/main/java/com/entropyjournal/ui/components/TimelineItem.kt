package com.entropyjournal.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.entropyjournal.ui.theme.LocalIsDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.ui.theme.TextMuted
import com.entropyjournal.util.DateTimeFormatter

enum class TimelinePosition { FIRST, MIDDLE, LAST, ONLY }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineItem(
    entry: JournalEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    position: TimelinePosition = TimelinePosition.ONLY,
    dotVerticalBias: Float = -1f,
    searchQuery: String = ""
) {
    val highlightColor = if (LocalIsDarkTheme.current) Color(0x44FFFFFF) else Color(0xFFFFEB3B)
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline rail: leerer Platzhalter — der Stromfluss + die Punkte werden vom
        // EnergyOverlay-Composable ueber der LazyColumn gezeichnet, nicht hier.
        // dotVerticalBias bleibt als Parameter erhalten fuer Rueckwaertskompatibilitaet,
        // wird aber nicht mehr verwendet.
        Spacer(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Entry card — internes vertikales Padding statt aeusserem Spacer,
        // damit die Linie zwischen Cards durchgehend laeuft.
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .clickable(onClick = onClick),
            glowColor = dotColor,
            glowIntensity = 0.1f
        ) {
            Column {
                if (!entry.title.isNullOrBlank()) {
                    val highlightedTitle = remember(entry.title, searchQuery, highlightColor) { highlightMatches(entry.title, searchQuery, highlightColor) }
                    Text(
                        text = highlightedTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = "${DateTimeFormatter.formatFull(entry.timestamp)} · ${DateTimeFormatter.formatRelative(entry.timestamp)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (searchQuery.isNotBlank()) {
                    val snippetText = remember(entry.displayText, searchQuery, highlightColor) {
                        val matchIndex = entry.displayText.lowercase().indexOf(searchQuery.lowercase())
                        val snippetStart = if (matchIndex > 50) {
                            val lineStart = entry.displayText.lastIndexOf('\n', matchIndex - 1)
                            if (lineStart >= 0) lineStart + 1 else (matchIndex - 50).coerceAtLeast(0)
                        } else 0
                        if (snippetStart > 0) "\u2026 " + entry.displayText.substring(snippetStart) else entry.displayText
                    }
                    val highlighted = remember(snippetText, searchQuery, highlightColor) { highlightMatches(snippetText, searchQuery, highlightColor) }
                    Text(
                        text = highlighted,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = entry.displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
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
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
