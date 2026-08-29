package de.frank.genialeideen.audio

import java.util.Locale
import java.util.logging.Logger

class WhisperHallucinationFilter {
    fun filter(result: GroqTranscriptionResponse, analysis: SpeechAnalysis?): String {
        val segments = result.segments
        if (segments.isNullOrEmpty()) return blockIfFloskel(result.text.trim(), analysis)

        val confident = segments.filter { segment ->
            val drop = isHallucination(segment)
            if (drop) {
                logger.info(
                    String.format(
                        Locale.ROOT,
                        "Layer 2: segment rejected ('%s', no_speech=%.2f, logprob=%.2f, compression=%.2f)",
                        segment.text?.trim()?.take(LOG_TEXT_LIMIT),
                        segment.noSpeechProbability ?: 0.0,
                        segment.averageLogProbability ?: 0.0,
                        segment.compressionRatio ?: 0.0,
                    ),
                )
            }
            !drop
        }
        if (confident.isEmpty()) {
            logger.info("Layer 2: all ${segments.size} segments rejected")
            return ""
        }

        val aligned = if (analysis == null) {
            confident
        } else {
            confident.filter { segment ->
                val start = segment.start ?: return@filter true
                val end = segment.end ?: return@filter true
                val hasSpeech = analysis.segmentHasSpeech(start, end)
                if (!hasSpeech) {
                    // The user text is deliberately a %s argument, never part of the format string.
                    logger.info(
                        String.format(
                            Locale.ROOT,
                            "Layer 3: segment without audio rejected ('%s', %.1f-%.1f s)",
                            segment.text?.trim()?.take(LOG_TEXT_LIMIT),
                            start,
                            end,
                        ),
                    )
                }
                hasSpeech
            }
        }

        val kept = if (aligned.isEmpty()) {
            logger.warning(
                "Layer 3: all ${confident.size} segments would be rejected; " +
                    "keeping layer-2 result because timestamp drift is likely",
            )
            confident
        } else {
            aligned
        }
        return blockIfFloskel(
            kept.joinToString(" ") { it.text?.trim().orEmpty() }.trim(),
            analysis,
        )
    }

    private fun isHallucination(segment: GroqSegment): Boolean {
        val noSpeech = segment.noSpeechProbability ?: 0.0
        val averageLogProbability = segment.averageLogProbability ?: 0.0
        val compression = segment.compressionRatio ?: 0.0
        if (noSpeech > NO_SPEECH_THRESHOLD && averageLogProbability < AVG_LOGPROB_THRESHOLD) {
            return true
        }
        if (compression > COMPRESSION_RATIO_THRESHOLD) return true
        val duration = (segment.end ?: 0.0) - (segment.start ?: 0.0)
        return duration > 0 && duration < MINI_NOISE_MAX_SECONDS &&
            noSpeech > NO_SPEECH_THRESHOLD
    }

    private fun blockIfFloskel(text: String, analysis: SpeechAnalysis?): String {
        if (!isBlocklistedFloskel(text, analysis)) return text
        logger.info(
            String.format(
                Locale.ROOT,
                "Layer 4: short exact phrase rejected in silence context ('%s')",
                text.take(LOG_TEXT_LIMIT),
            ),
        )
        return ""
    }

    private fun isBlocklistedFloskel(text: String, analysis: SpeechAnalysis?): Boolean {
        if (text.isEmpty() || text.length > FLOSKEL_MAX_CHARS || analysis == null) return false
        val normalized = normalizeFloskel(text)
        if (normalized.isEmpty()) return false
        if (normalized.split(' ').count(String::isNotEmpty) > FLOSKEL_MAX_WORDS) return false
        if (normalized !in FLOSKEL_BLOCKLIST) return false
        return analysis.voicedMs < SILENCE_CONTEXT_MAX_VOICED_MS
    }

    private fun normalizeFloskel(text: String): String = buildString(text.length) {
        text.lowercase().forEach { character ->
            append(if (character.isLetter()) character else ' ')
        }
    }.split(' ').filter(String::isNotEmpty).joinToString(" ")

    companion object {
        private const val NO_SPEECH_THRESHOLD = 0.6
        private const val AVG_LOGPROB_THRESHOLD = -1.0
        private const val COMPRESSION_RATIO_THRESHOLD = 2.4
        private const val MINI_NOISE_MAX_SECONDS = 0.4
        private const val FLOSKEL_MAX_WORDS = 8
        private const val FLOSKEL_MAX_CHARS = 64
        private const val SILENCE_CONTEXT_MAX_VOICED_MS = 600
        private const val LOG_TEXT_LIMIT = 60

        private val logger = Logger.getLogger(WhisperHallucinationFilter::class.java.name)
        private val FLOSKEL_BLOCKLIST = setOf(
            "vielen dank",
            "vielen dank fürs zuschauen",
            "vielen dank fuers zuschauen",
            "vielen dank für eure aufmerksamkeit",
            "vielen dank für ihre aufmerksamkeit",
            "vielen dank für die aufmerksamkeit",
            "bis zum nächsten mal",
            "bis zum nächsten video",
            "untertitel",
            "untertitel des zdf",
            "untertitelung des zdf für funk",
            "untertitel im auftrag des zdf für funk",
            "untertitel von stephanie geiges",
            "untertitel der amara org community",
            "der text ist nicht auf deutsch",
            "thank you",
            "thank you for watching",
            "thanks for watching",
            "please subscribe",
            "subtitles by the amara org community",
        )
    }
}
