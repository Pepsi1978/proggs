package com.entropyjournal.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.entropyjournal.ui.theme.LocalIsDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropyjournal.domain.model.JournalEntry
import com.entropyjournal.ui.theme.TextMuted
import com.entropyjournal.ui.theme.scaledBody
import com.entropyjournal.ui.theme.scaledHeading
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

    // Symbol via KI-Klassifikator + Stichwort-Fallback. Sofort wird das Stichwort-
    // Mapping angezeigt, im Hintergrund laeuft ein Gemini-Call und ersetzt das Icon
    // sobald die KI-Antwort da ist (mit Cache, also nur einmal pro Eintrag).
    val iconKey = rememberEntryIconKey(entry)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline rail follows the compact 46dp Goldener-Faden layout.
        // ONLY = keine Linie (einziger Eintrag in seiner Sektion), sonst durchgehende Linie.
        Box(
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                if (position == TimelinePosition.ONLY) return@Canvas
                val cx = size.width / 2f
                val midY = size.height / 2f
                val gap = 19.dp.toPx() + 1.dp.toPx()
                val strokePx = 1.dp.toPx()

                // FIRST: keine Linie ueber dem Badge — Bereich beginnt mit dem Symbol
                // LAST: keine Linie unter dem Badge — Bereich endet mit dem Symbol
                // MIDDLE: Linie auf beiden Seiten (durchgehend zur Nachbar-Card)
                val drawAbove = position == TimelinePosition.MIDDLE || position == TimelinePosition.LAST
                val drawBelow = position == TimelinePosition.MIDDLE || position == TimelinePosition.FIRST

                if (drawAbove && midY - gap > 0f) {
                    drawLine(
                        color = lineColor,
                        start = Offset(cx, 0f),
                        end = Offset(cx, midY - gap),
                        strokeWidth = strokePx,
                    )
                }
                if (drawBelow && midY + gap < size.height) {
                    drawLine(
                        color = lineColor,
                        start = Offset(cx, midY + gap),
                        end = Offset(cx, size.height),
                        strokeWidth = strokePx,
                    )
                }
            }
            CircleIconBadge(
                iconKey = iconKey,
                size = 38.dp,
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Entry card — internes vertikales Padding statt aeusserem Spacer,
        // damit die Linie zwischen Cards durchgehend laeuft.
        GlassCard(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
                .clickable(onClick = onClick),
            glowColor = dotColor,
            glowIntensity = 0.1f,
            cornerRadius = 18.dp,
            contentPadding = 16.dp,
        ) {
            Column {
                if (!entry.title.isNullOrBlank()) {
                    val highlightedTitle = remember(entry.title, searchQuery, highlightColor) { highlightMatches(entry.title, searchQuery, highlightColor) }
                    Text(
                        text = highlightedTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp.scaledHeading),
                        color = MaterialTheme.colorScheme.onSurface,
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
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp.scaledBody, lineHeight = 24.sp.scaledBody),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = entry.displayText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp.scaledBody, lineHeight = 24.sp.scaledBody),
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
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier.border(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(50),
                                ),
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
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
