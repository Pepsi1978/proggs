package de.frank.entropyreducer.data.remote

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Groq Whisper Transkriptions-API.
 * Endpoint: https://api.groq.com/openai/v1/audio/transcriptions
 * Doku: https://console.groq.com/docs/speech-to-text
 */
interface GroqWhisperApi {

    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribe(
        @Header("Authorization") bearer: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody,
        @Part("language") language: RequestBody,
        @Part("response_format") responseFormat: RequestBody,
    ): TranscriptionResponse
}

@Serializable
data class TranscriptionResponse(
    val text: String,
)
