package de.frank.karteikartenlernen.transcription

data class GroqTranscriptionResponse(
    val text: String,
    val segments: List<GroqSegment>? = null,
)

data class GroqSegment(
    val start: Double?,
    val end: Double?,
    val text: String?,
    val noSpeechProbability: Double?,
    val averageLogProbability: Double?,
    val compressionRatio: Double?,
)

class WhisperHallucinationFilter {
    fun filter(result: GroqTranscriptionResponse, analysis: SpeechAnalysis?): String {
        val segments = result.segments
        if (segments.isNullOrEmpty()) return blockIfFloskel(result.text.trim(), analysis)

        val confident = segments.filterNot(::isHallucination)
        if (confident.isEmpty()) return ""

        val aligned = if (analysis == null) {
            confident
        } else {
            confident.filter { segment ->
                val start = segment.start ?: return@filter true
                val end = segment.end ?: return@filter true
                analysis.segmentHasSpeech(start, end)
            }
        }

        // Whisper-Timestamps können driften. Nie den gesamten erkannten Satz allein deshalb verlieren.
        val kept = aligned.ifEmpty { confident }
        val text = kept.joinToString(" ") { it.text?.trim().orEmpty() }.trim()
        return blockIfFloskel(text, analysis)
    }

    private fun isHallucination(segment: GroqSegment): Boolean {
        val noSpeech = segment.noSpeechProbability ?: 0.0
        val averageLogProbability = segment.averageLogProbability ?: 0.0
        val compression = segment.compressionRatio ?: 0.0
        if (noSpeech > NO_SPEECH_THRESHOLD && averageLogProbability < AVG_LOGPROB_THRESHOLD) return true
        if (compression > COMPRESSION_RATIO_THRESHOLD) return true
        val duration = (segment.end ?: 0.0) - (segment.start ?: 0.0)
        return duration > 0 && duration < MINI_NOISE_MAX_SECONDS && noSpeech > NO_SPEECH_THRESHOLD
    }

    private fun blockIfFloskel(text: String, analysis: SpeechAnalysis?): String =
        if (isBlocklistedFloskel(text, analysis)) "" else text

    private fun isBlocklistedFloskel(text: String, analysis: SpeechAnalysis?): Boolean {
        if (text.isEmpty() || text.length > FLOSKEL_MAX_CHARS || analysis == null) return false
        val normalized = normalizeFloskel(text)
        if (normalized.isEmpty()) return false
        if (normalized.split(' ').count { it.isNotEmpty() } > FLOSKEL_MAX_WORDS) return false
        if (normalized !in FLOSKEL_BLOCKLIST) return false
        return analysis.voicedMs < SILENCE_CONTEXT_MAX_VOICED_MS
    }

    private fun normalizeFloskel(text: String): String = buildString(text.length) {
        text.lowercase().forEach { character -> append(if (character.isLetter()) character else ' ') }
    }.split(' ').filter { it.isNotEmpty() }.joinToString(" ")

    companion object {
        private const val NO_SPEECH_THRESHOLD = 0.6
        private const val AVG_LOGPROB_THRESHOLD = -1.0
        private const val COMPRESSION_RATIO_THRESHOLD = 2.4
        private const val MINI_NOISE_MAX_SECONDS = 0.4
        private const val FLOSKEL_MAX_WORDS = 8
        private const val FLOSKEL_MAX_CHARS = 64
        private const val SILENCE_CONTEXT_MAX_VOICED_MS = 600

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
