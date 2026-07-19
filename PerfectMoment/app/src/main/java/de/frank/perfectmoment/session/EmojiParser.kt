package de.frank.perfectmoment.session

object EmojiParser {
    const val FALLBACK_EMOJI = "✨"

    data class ParsedQuestion(
        val emoji: String,
        val text: String,
    )

    fun parse(raw: String): ParsedQuestion {
        val value = raw.trim()
        if (value.isEmpty()) return ParsedQuestion(FALLBACK_EMOJI, "")

        val emojiEnd = emojiClusterEnd(value)
        if (emojiEnd == 0) return ParsedQuestion(FALLBACK_EMOJI, value)

        return ParsedQuestion(
            emoji = value.substring(0, emojiEnd),
            text = value.substring(emojiEnd).trimStart(),
        )
    }

    private fun emojiClusterEnd(value: String): Int {
        var offset = 0
        val first = value.codePointAt(offset)

        if (isRegionalIndicator(first)) {
            offset += Character.charCount(first)
            if (offset < value.length) {
                val second = value.codePointAt(offset)
                if (isRegionalIndicator(second)) offset += Character.charCount(second)
            }
            return offset
        }

        if (isKeycapBase(first)) {
            offset += Character.charCount(first)
            offset = consumeVariationSelectors(value, offset)
            if (offset < value.length && value.codePointAt(offset) == KEYCAP) {
                return offset + Character.charCount(KEYCAP)
            }
            return 0
        }

        if (!isEmojiBase(first)) return 0
        offset += Character.charCount(first)
        offset = consumeEmojiSuffix(value, offset)

        while (offset < value.length && value.codePointAt(offset) == ZERO_WIDTH_JOINER) {
            val joinerOffset = offset
            offset += Character.charCount(ZERO_WIDTH_JOINER)
            if (offset >= value.length) return joinerOffset
            val joined = value.codePointAt(offset)
            if (!isEmojiBase(joined)) return joinerOffset
            offset += Character.charCount(joined)
            offset = consumeEmojiSuffix(value, offset)
        }

        return offset
    }

    private fun consumeEmojiSuffix(value: String, initialOffset: Int): Int {
        var offset = consumeVariationSelectors(value, initialOffset)
        if (offset < value.length && isSkinTone(value.codePointAt(offset))) {
            offset += Character.charCount(value.codePointAt(offset))
        }
        if (offset < value.length && value.codePointAt(offset) == KEYCAP) {
            offset += Character.charCount(KEYCAP)
        }
        if (offset < value.length && isTagCharacter(value.codePointAt(offset))) {
            while (offset < value.length && isTagCharacter(value.codePointAt(offset))) {
                offset += Character.charCount(value.codePointAt(offset))
            }
            if (offset < value.length && value.codePointAt(offset) == CANCEL_TAG) {
                offset += Character.charCount(CANCEL_TAG)
            }
        }
        return offset
    }

    private fun consumeVariationSelectors(value: String, initialOffset: Int): Int {
        var offset = initialOffset
        while (offset < value.length && isVariationSelector(value.codePointAt(offset))) {
            offset += Character.charCount(value.codePointAt(offset))
        }
        return offset
    }

    private fun isEmojiBase(codePoint: Int): Boolean =
        codePoint in 0x1F000..0x1FAFF ||
            codePoint in 0x2600..0x27BF ||
            codePoint in 0x2190..0x21FF ||
            codePoint in 0x2300..0x23FF ||
            codePoint in 0x2B00..0x2BFF ||
            codePoint in setOf(
                0x00A9,
                0x00AE,
                0x203C,
                0x2049,
                0x2122,
                0x2139,
                0x3030,
                0x303D,
                0x3297,
                0x3299,
            )

    private fun isRegionalIndicator(codePoint: Int): Boolean = codePoint in 0x1F1E6..0x1F1FF
    private fun isSkinTone(codePoint: Int): Boolean = codePoint in 0x1F3FB..0x1F3FF
    private fun isVariationSelector(codePoint: Int): Boolean =
        codePoint == 0xFE0E || codePoint == 0xFE0F || codePoint in 0xE0100..0xE01EF
    private fun isKeycapBase(codePoint: Int): Boolean = codePoint == '#'.code ||
        codePoint == '*'.code || codePoint in '0'.code..'9'.code
    private fun isTagCharacter(codePoint: Int): Boolean = codePoint in 0xE0020..0xE007E

    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val KEYCAP = 0x20E3
    private const val CANCEL_TAG = 0xE007F
}
