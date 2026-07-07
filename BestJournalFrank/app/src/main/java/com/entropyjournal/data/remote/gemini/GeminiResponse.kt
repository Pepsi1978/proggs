package com.entropyjournal.data.remote.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
) {
    fun extractText(): String? {
        return candidates?.firstOrNull()
            ?.content
            ?.parts
            ?.firstOrNull()
            ?.text
    }
}

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiCandidateContent?
)

@JsonClass(generateAdapter = true)
data class GeminiCandidateContent(
    @Json(name = "parts") val parts: List<GeminiPart>?
)
