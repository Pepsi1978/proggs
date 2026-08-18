package de.frank.gedankenspeicher.auth

import java.text.Normalizer
import java.util.Locale

object QuestionResponseValidator {
    const val REQUIRED_INPUT_COUNT = 30
    const val MIN_ACCEPTED_COUNT = 20
    const val FALLBACK_EMOJI = "✨"

    fun validate(
        rawQuestions: List<String>,
        previousQuestions: Collection<String> = emptyList(),
        excludedQuestions: Collection<String> = emptyList(),
    ): List<CodexQuestion> {
        if (rawQuestions.size != REQUIRED_INPUT_COUNT) {
            throw QuestionValidationException(
                "OpenAI hat ${rawQuestions.size} statt genau $REQUIRED_INPUT_COUNT Fragen geliefert.",
            )
        }

        val validator = IncrementalQuestionValidator(previousQuestions, excludedQuestions)
        val accepted = rawQuestions.flatMap(validator::accept)

        if (accepted.size < MIN_ACCEPTED_COUNT) {
            throw QuestionValidationException(
                "Nach dem Entfernen leerer oder doppelter Einträge sind nur ${accepted.size} gültige Fragen übrig.",
            )
        }
        return accepted
    }

    /**
     * Zerlegt einen Modelleintrag in die Fragen, die wirklich darin stecken.
     *
     * Das Modell liefert gelegentlich einen ganzen Block statt einer einzelnen Frage: mehrere
     * Fragen aneinandergehängt, die Emojis mitten im Text, dazu ein eingestreuter Fremdsatz. Als
     * eine Frage ist so ein Block unbrauchbar — vorgelesen ergibt er Unsinn.
     *
     * Deshalb wird hier zurückzerlegt: An jedem Emoji, das auf ein Fragezeichen folgt, beginnt
     * eine neue Frage, und jede Frage endet an ihrem eigenen ersten Fragezeichen. Alles dahinter
     * — Fremdsätze, Nachklapp, Aufzählungsreste — fällt weg. Was danach keine Frage von
     * vernünftiger Länge ist, wird verworfen.
     */
    internal fun splitIntoQuestions(raw: String): List<CodexQuestion> =
        splitAtEmbeddedQuestions(raw.trim()).mapNotNull { segment ->
            val question = splitLeadingEmoji(segment) ?: return@mapNotNull null
            val end = question.text.indexOf(QUESTION_MARK)
            if (end < 0) return@mapNotNull null
            val text = question.text.substring(0, end + 1).trim()
            if (text.length !in MIN_QUESTION_LENGTH..MAX_QUESTION_LENGTH) return@mapNotNull null
            CodexQuestion(question.emoji, text)
        }

    /** Schneidet vor jedem Emoji, das einer abgeschlossenen Frage folgt. */
    private fun splitAtEmbeddedQuestions(value: String): List<String> {
        val cuts = mutableListOf(0)
        var index = 0
        var lastVisible = ' '
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            val width = Character.charCount(codePoint)
            if (index > 0 && lastVisible == QUESTION_MARK && leadingEmojiEnd(value.substring(index)) > 0) {
                cuts += index
            }
            if (!Character.isWhitespace(codePoint)) lastVisible = value[index]
            index += width
        }
        return cuts.mapIndexed { position, start ->
            value.substring(start, cuts.getOrElse(position + 1) { value.length })
        }
    }

    internal fun splitLeadingEmoji(raw: String): CodexQuestion? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        val emojiEnd = leadingEmojiEnd(value)
        val emoji = if (emojiEnd > 0) value.substring(0, emojiEnd) else FALLBACK_EMOJI
        val text = value.substring(if (emojiEnd > 0) emojiEnd else 0).trim()
        return text.takeIf(String::isNotEmpty)?.let { CodexQuestion(emoji, it) }
    }

    internal fun normalizeQuestion(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        return buildString(normalized.length) {
            var previousWasSpace = true
            normalized.codePoints().forEach { codePoint ->
                if (Character.isLetterOrDigit(codePoint)) {
                    appendCodePoint(codePoint)
                    previousWasSpace = false
                } else if (!previousWasSpace) {
                    append(' ')
                    previousWasSpace = true
                }
            }
        }.trim()
    }

    private fun leadingEmojiEnd(value: String): Int {
        var index = 0
        val first = value.codePointAt(index)
        if (isRegionalIndicator(first)) {
            index += Character.charCount(first)
            if (index < value.length && isRegionalIndicator(value.codePointAt(index))) {
                index += Character.charCount(value.codePointAt(index))
            }
            return index
        }
        if (isKeycapStart(first)) {
            val end = consumeVariationSelectors(value, Character.charCount(first))
            return if (end < value.length && value.codePointAt(end) == KEYCAP) {
                end + Character.charCount(KEYCAP)
            } else {
                0
            }
        }
        if (!isEmojiBase(first)) return 0

        index = consumeEmojiSuffix(value, Character.charCount(first))
        while (index < value.length && value.codePointAt(index) == ZERO_WIDTH_JOINER) {
            val joinerEnd = index + Character.charCount(ZERO_WIDTH_JOINER)
            if (joinerEnd >= value.length) break
            val next = value.codePointAt(joinerEnd)
            if (!isEmojiBase(next) && !isRegionalIndicator(next)) break
            index = consumeEmojiSuffix(value, joinerEnd + Character.charCount(next))
        }
        return index
    }

    private fun consumeEmojiSuffix(value: String, start: Int): Int {
        var index = consumeVariationSelectors(value, start)
        if (index < value.length && isSkinTone(value.codePointAt(index))) {
            index += Character.charCount(value.codePointAt(index))
            index = consumeVariationSelectors(value, index)
        }
        while (index < value.length && isEmojiTag(value.codePointAt(index))) {
            index += Character.charCount(value.codePointAt(index))
        }
        if (index < value.length && value.codePointAt(index) == CANCEL_TAG) {
            index += Character.charCount(CANCEL_TAG)
        }
        return index
    }

    private fun consumeVariationSelectors(value: String, start: Int): Int {
        var index = start
        while (index < value.length && isVariationSelector(value.codePointAt(index))) {
            index += Character.charCount(value.codePointAt(index))
        }
        return index
    }

    private fun isEmojiBase(codePoint: Int): Boolean =
        codePoint in 0x1F000..0x1FAFF ||
            codePoint in 0x2600..0x27BF ||
            codePoint in 0x2190..0x21FF ||
            codePoint in 0x2300..0x23FF ||
            codePoint in 0x2B00..0x2BFF ||
            codePoint in setOf(0x00A9, 0x00AE, 0x203C, 0x2049, 0x2122, 0x2139, 0x3030, 0x303D, 0x3297, 0x3299)

    private fun isRegionalIndicator(codePoint: Int): Boolean = codePoint in 0x1F1E6..0x1F1FF

    private fun isSkinTone(codePoint: Int): Boolean = codePoint in 0x1F3FB..0x1F3FF

    private fun isVariationSelector(codePoint: Int): Boolean = codePoint == 0xFE0E || codePoint == 0xFE0F

    private fun isEmojiTag(codePoint: Int): Boolean = codePoint in 0xE0020..0xE007E

    private fun isKeycapStart(codePoint: Int): Boolean = codePoint == '#'.code || codePoint == '*'.code || codePoint in '0'.code..'9'.code

    private const val QUESTION_MARK = '?'
    private const val MIN_QUESTION_LENGTH = 10
    private const val MAX_QUESTION_LENGTH = 300
    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val KEYCAP = 0x20E3
    private const val CANCEL_TAG = 0xE007F
}

internal class IncrementalQuestionValidator(
    previousQuestions: Collection<String> = emptyList(),
    excludedQuestions: Collection<String> = emptyList(),
) {
    private val seen = (previousQuestions + excludedQuestions + IntroQuestionPolicy.QUESTION)
        .mapTo(mutableSetOf(), QuestionResponseValidator::normalizeQuestion)
        .apply { remove("") }

    /**
     * Nimmt einen Modelleintrag an und gibt die brauchbaren, noch nicht gestellten Fragen daraus
     * zurück — meist genau eine, bei einem zusammengeklumpten Eintrag auch mehrere, bei Unsinn
     * keine.
     */
    fun accept(raw: String): List<CodexQuestion> =
        QuestionResponseValidator.splitIntoQuestions(raw).filter { question ->
            val normalized = QuestionResponseValidator.normalizeQuestion(question.text)
            normalized.isNotEmpty() && seen.add(normalized)
        }
}
