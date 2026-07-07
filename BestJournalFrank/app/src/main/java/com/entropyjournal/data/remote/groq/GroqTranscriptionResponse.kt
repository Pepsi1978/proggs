package com.entropyjournal.data.remote.groq

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * `verbose_json`-Antwort der Groq-Transkription (bugs/desktop/groq-transkription.md §2.3/§3.1):
 * Segment-Metadaten kosten bei Groq KEINE Mehrlatenz/-kosten und liefern die Confidence-Felder
 * fuer das Anti-Halluzinations-Gate. Ultrakurze Clips liefern oft GAR KEINE [segments] (nur
 * top-level [text]) — darum nullable mit Default.
 */
@JsonClass(generateAdapter = true)
data class GroqTranscriptionResponse(
    @Json(name = "text") val text: String = "",
    @Json(name = "segments") val segments: List<GroqTranscriptionSegment> = emptyList(),
)

/**
 * Ein Whisper-Segment mit Confidence-Feldern. Defaults sind bewusst "unverdaechtig" (0.0) —
 * fehlt ein Feld in der Antwort, filtert das Gate NICHT (funktionserhaltend).
 */
@JsonClass(generateAdapter = true)
data class GroqTranscriptionSegment(
    @Json(name = "start") val start: Double = 0.0,
    @Json(name = "end") val end: Double = 0.0,
    @Json(name = "text") val text: String = "",
    @Json(name = "no_speech_prob") val noSpeechProb: Double = 0.0,
    @Json(name = "avg_logprob") val avgLogprob: Double = 0.0,
    @Json(name = "compression_ratio") val compressionRatio: Double = 0.0,
)
