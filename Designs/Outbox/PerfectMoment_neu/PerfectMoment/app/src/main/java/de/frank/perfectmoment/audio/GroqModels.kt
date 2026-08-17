package de.frank.perfectmoment.audio

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
