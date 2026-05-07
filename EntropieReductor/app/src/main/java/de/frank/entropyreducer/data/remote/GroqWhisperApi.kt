package de.frank.entropyreducer.data.remote

import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Groq Whisper-Transkriptions- und Modell-API.
 * Doku: https://console.groq.com/docs/api-reference (Stand 2025).
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

    /**
     * Listet verfuegbare Modelle auf — wird fuer Verbindungs-Tests genutzt
     * (Bearer-Auth pruefen ohne Audio-Upload).
     */
    @retrofit2.http.GET("models")
    suspend fun listModels(
        @Header("Authorization") bearer: String,
    ): ModelsListResponse
}

@Serializable
data class TranscriptionResponse(
    val text: String,
)

@Serializable
data class ModelsListResponse(
    val data: List<ModelInfo> = emptyList(),
    @kotlinx.serialization.SerialName("object") val obj: String? = null,
)

@Serializable
data class ModelInfo(
    val id: String,
    @kotlinx.serialization.SerialName("object") val obj: String? = null,
    val owned_by: String? = null,
)
