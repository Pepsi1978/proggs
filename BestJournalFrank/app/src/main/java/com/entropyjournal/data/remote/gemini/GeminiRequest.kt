package com.entropyjournal.data.remote.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String
)

// Helper to build requests easily
object GeminiRequestBuilder {
    fun build(
        userText: String,
        systemPrompt: String? = null,
        temperature: Float? = null,
        maxOutputTokens: Int? = null
    ): GeminiRequest {
        val contents = listOf(
            GeminiContent(parts = listOf(GeminiPart(text = userText)))
        )
        val systemInstruction = systemPrompt?.let {
            GeminiContent(parts = listOf(GeminiPart(text = it)))
        }
        val config = if (temperature != null || maxOutputTokens != null) {
            GeminiGenerationConfig(temperature = temperature, maxOutputTokens = maxOutputTokens)
        } else null
        return GeminiRequest(contents = contents, systemInstruction = systemInstruction, generationConfig = config)
    }
}
