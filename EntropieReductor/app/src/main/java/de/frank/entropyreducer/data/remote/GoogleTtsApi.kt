package de.frank.entropyreducer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Query

/**
 * Google Cloud Text-to-Speech v1 API mit API-Key-Auth (kein OAuth).
 * Doku: https://cloud.google.com/text-to-speech/docs/reference/rest (Stand 2025).
 *
 * API-Key wird als Query-Parameter `key` uebergeben — Standard für
 * Google-Cloud-APIs ohne Service-Account.
 */
interface GoogleTtsApi {

    /**
     * Listet verfuegbare Voices. Wird für Verbindungs-Tests + Voice-Auswahl
     * (Spec §6.2 / Stufe 4) genutzt.
     */
    @GET("v1/voices")
    suspend fun listVoices(
        @Query("key") apiKey: String,
        @Query("languageCode") languageCode: String? = null,
    ): VoicesListResponse

    /**
     * Synthetisiert Text zu Audio (MP3 base64). Stufe 4 — für Tagesbriefing.
     */
    @POST("v1/text:synthesize")
    suspend fun synthesize(
        @Query("key") apiKey: String,
        @Body request: SynthesizeRequest,
    ): SynthesizeResponse
}

@Serializable
data class VoicesListResponse(
    val voices: List<VoiceInfo> = emptyList(),
)

@Serializable
data class VoiceInfo(
    val name: String,
    val languageCodes: List<String> = emptyList(),
    val ssmlGender: String? = null,
    val naturalSampleRateHertz: Int? = null,
)

@Serializable
data class SynthesizeRequest(
    val input: TtsInput,
    val voice: TtsVoice,
    val audioConfig: TtsAudioConfig,
)

@Serializable
data class TtsInput(
    val text: String? = null,
    val ssml: String? = null,
)

@Serializable
data class TtsVoice(
    val languageCode: String,
    val name: String? = null,
)

@Serializable
data class TtsAudioConfig(
    val audioEncoding: String,                    // MP3, OGG_OPUS, LINEAR16
    val speakingRate: Double? = null,
    val pitch: Double? = null,
)

@Serializable
data class SynthesizeResponse(
    @SerialName("audioContent") val audioContentBase64: String,
)
