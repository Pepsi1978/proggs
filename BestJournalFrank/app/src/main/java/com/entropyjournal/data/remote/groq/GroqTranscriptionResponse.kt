package com.entropyjournal.data.remote.groq

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroqTranscriptionResponse(
    @Json(name = "text") val text: String
)
