package de.frank.stacklabor.werftstudio.service.tts

import de.frank.stacklabor.werftstudio.domain.model.mitDeutschenUmlauten
import java.nio.charset.StandardCharsets

object TtsText {
    fun paragraphs(markdown: String, maxUtf8Bytes: Int = 3_800): List<String> = markdown.mitDeutschenUmlauten()
        .replace(Regex("```[\\s\\S]*?```"), " ")
        .replace(Regex("!\\[[^]]*]\\([^)]*\\)"), " ")
        .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "\$1")
        .replace(Regex("(?m)^#{1,6}\\s*"), "")
        .replace(Regex("[*_`>~]"), "")
        .replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), " ")
        .split(Regex("\\n\\s*\\n+"))
        .flatMap { splitToUtf8Limit(it.replace(Regex("\\s+"), " ").trim(), maxUtf8Bytes) }
        .filter { it.any(Char::isLetterOrDigit) }

    private fun splitToUtf8Limit(text: String, maxBytes: Int): List<String> {
        if (text.isBlank()) return emptyList()
        if (text.toByteArray(StandardCharsets.UTF_8).size <= maxBytes) return listOf(text)
        val result = mutableListOf<String>()
        var remaining = text
        while (remaining.toByteArray(StandardCharsets.UTF_8).size > maxBytes) {
            var end = remaining.length.coerceAtMost(maxBytes)
            while (end > 1 && remaining.substring(0, end).toByteArray(StandardCharsets.UTF_8).size > maxBytes) end--
            val sentenceBreak = remaining.lastIndexOfAny(charArrayOf('.', '!', '?', ';'), end - 1)
            val whitespaceBreak = remaining.lastIndexOf(' ', end - 1)
            val split = maxOf(sentenceBreak + 1, whitespaceBreak).takeIf { it > 0 } ?: end
            result += remaining.substring(0, split).trim()
            remaining = remaining.substring(split).trim()
        }
        if (remaining.isNotBlank()) result += remaining
        return result
    }
}
