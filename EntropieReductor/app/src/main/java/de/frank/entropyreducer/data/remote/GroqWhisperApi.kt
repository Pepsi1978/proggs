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
        @Part("temperature") temperature: RequestBody,
    ): TranscriptionResponse

    /**
     * Listet verfuegbare Modelle auf — wird für Verbindungs-Tests genutzt
     * (Bearer-Auth prüfen ohne Audio-Upload).
     */
    @retrofit2.http.GET("models")
    suspend fun listModels(
        @Header("Authorization") bearer: String,
    ): ModelsListResponse
}

/**
 * `verbose_json`-Antwort der Groq-Transkription (bugs/desktop/groq-transkription.md §2.3/§3.1):
 * Segment-Metadaten kosten bei Groq KEINE Mehrlatenz/-kosten und liefern die Confidence-
 * Felder fuer das Anti-Halluzinations-Gate. Ultrakurze Clips liefern oft GAR KEINE
 * `segments` (nur top-level [text]) — darum nullable mit Default.
 */
@Serializable
data class TranscriptionResponse(
    val text: String = "",
    val segments: List<TranscriptionSegment> = emptyList(),
)

/**
 * Ein Whisper-Segment mit Confidence-Feldern. Defaults sind bewusst "unverdaechtig"
 * (0.0) — fehlt ein Feld in der Antwort, filtert das Gate NICHT (funktionserhaltend).
 */
@Serializable
data class TranscriptionSegment(
    val start: Double = 0.0,
    val end: Double = 0.0,
    val text: String = "",
    @kotlinx.serialization.SerialName("no_speech_prob") val noSpeechProb: Double = 0.0,
    @kotlinx.serialization.SerialName("avg_logprob") val avgLogprob: Double = 0.0,
    @kotlinx.serialization.SerialName("compression_ratio") val compressionRatio: Double = 0.0,
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
