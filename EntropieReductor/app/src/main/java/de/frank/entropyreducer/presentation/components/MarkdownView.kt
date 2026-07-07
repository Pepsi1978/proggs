package de.frank.entropyreducer.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.frank.entropyreducer.presentation.theme.LocalCosmos

/**
 * Leichtgewichtiger Markdown-Renderer für die KI-Antworten.
 *
 * Unterstuetzt:
 *  - `## Headline` (h2)
 *  - `### Headline` (h3)
 *  - `- Listenpunkt`
 *  - `**fett**` und `*kursiv*` inline
 *  - Absatzleerzeilen
 *
 * Bewusst ohne externe Library — minimal, schnell, theme-konform.
 */
@Composable
fun MarkdownView(
    text: String,
    modifier: Modifier = Modifier,
) {
    val cosmos = LocalCosmos.current
    val blocks = remember(text) { parse(text) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.H2 -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = block.text,
                        color = cosmos.textPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
                is MdBlock.H3 -> Text(
                    text = block.text,
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                is MdBlock.Bullet -> Text(
                    text = renderInline(block.text, cosmos.textPrimary.toString()),
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
                is MdBlock.Paragraph -> Text(
                    text = renderInline(block.text, cosmos.textPrimary.toString()),
                    color = cosmos.textPrimary,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                )
                MdBlock.Empty -> Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private sealed interface MdBlock {
    data class H2(val text: String) : MdBlock
    data class H3(val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data object Empty : MdBlock
}

private fun parse(src: String): List<MdBlock> {
    val out = mutableListOf<MdBlock>()
    val paragraphBuf = StringBuilder()

    fun flushParagraph() {
        if (paragraphBuf.isNotEmpty()) {
            out += MdBlock.Paragraph(paragraphBuf.toString().trim())
            paragraphBuf.clear()
        }
    }

    src.lines().forEach { rawLine ->
        val line = rawLine.trimEnd()
        when {
            line.isBlank() -> {
                flushParagraph()
                if (out.isNotEmpty() && out.last() !is MdBlock.Empty) out += MdBlock.Empty
            }
            line.startsWith("## ") -> { flushParagraph(); out += MdBlock.H2(line.removePrefix("## ").trim()) }
            line.startsWith("### ") -> { flushParagraph(); out += MdBlock.H3(line.removePrefix("### ").trim()) }
            line.startsWith("- ") || line.startsWith("* ") -> {
                flushParagraph()
                out += MdBlock.Bullet(line.drop(2).trim())
            }
            else -> {
                if (paragraphBuf.isNotEmpty()) paragraphBuf.append(" ")
                paragraphBuf.append(line.trim())
            }
        }
    }
    flushParagraph()
    return out
}

/**
 * Rendert Inline-Markup `**fett**` und `*kursiv*` zu AnnotatedString.
 * Der Farbe-Parameter ist nur ein Marker — die tatsaechliche Farbe kommt vom
 * `color`-Parameter des Text-Composable.
 */
private fun renderInline(text: String, @Suppress("UNUSED_PARAMETER") color: String): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end < 0) {
                        append(text.substring(i)); i = text.length
                    } else {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    }
                }
                text[i] == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end < 0) {
                        append(text[i]); i++
                    } else {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    }
                }
                else -> {
                    val nextSpecial = nextSpecialIndex(text, i)
                    val end = if (nextSpecial < 0) text.length else nextSpecial
                    append(text.substring(i, end))
                    i = end
                }
            }
        }
    }

private fun nextSpecialIndex(s: String, fromInclusive: Int): Int {
    var i = fromInclusive
    while (i < s.length) {
        if (s[i] == '*') return i
        i++
    }
    return -1
}

