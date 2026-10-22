package de.frank.stacklabor.werftstudio.service.tts

import java.io.File

enum class TtsProviderId {
    EDGE,
    GOOGLE_CLOUD,
    QWEN,
    GEMINI_FLASH,
    GEMINI_LITE,
    AZURE,
}

data class TtsConfiguration(
    val provider: TtsProviderId = TtsProviderId.EDGE,
    val voiceId: String = DEFAULT_EDGE_VOICE,
    val speechRate: Float = 1f,
    val paragraphPauseMillis: Long = 450L,
    val autoStopMillis: Long = 30 * 60_000L,
) {
    init {
        require(speechRate in 0.7f..1.3f)
        require(paragraphPauseMillis in 0L..10_000L)
        require(autoStopMillis >= 0L)
    }

    companion object {
        const val DEFAULT_EDGE_VOICE = "de-DE-SeraphinaMultilingualNeural"
        const val DEFAULT_GOOGLE_VOICE = "de-DE-Chirp3-HD-Kore"
    }
}

data class TtsSynthesisRequest(
    val text: String,
    val voiceId: String,
    val speechRate: Float,
)

data class SynthesizedAudio(
    val file: File,
    val mimeType: String,
    val cacheHit: Boolean,
    val billedCharacters: Int,
)

interface TtsProvider {
    val id: TtsProviderId
    suspend fun synthesize(request: TtsSynthesisRequest): SynthesizedAudio
}

interface TtsCredentials {
    fun googleApiKey(): String?
    fun qwenApiKey(): String?
    fun qwenVoiceId(): String?

    /** Gemini 3.8 TTS: key of the AI Studio project without billing (free) and with billing (paid). */
    fun geminiFreeKey(): String? = null
    fun geminiPaidKey(): String? = null

    /** One of [GeminiKeyChoice.FREE_THEN_PAID], [GeminiKeyChoice.FREE_ONLY], [GeminiKeyChoice.PAID_ONLY]. */
    fun geminiKeyChoice(): String = GeminiKeyChoice.FREE_THEN_PAID

    /** If the free Gemini key fails, read on with the same voice via Google Chirp 3 HD instead of the paid key. */
    fun geminiChirpFallback(): Boolean = false
    fun azureKey(): String? = null
    fun azureRegion(): String = TtsVoices.DEFAULT_AZURE_REGION
}

object GeminiKeyChoice {
    const val FREE_THEN_PAID = "frei_dann_bezahlt"
    const val FREE_ONLY = "nur_frei"
    const val PAID_ONLY = "nur_bezahlt"
    val CHOICES = listOf(
        FREE_THEN_PAID to "Erst kostenlos, dann bezahlt",
        FREE_ONLY to "Nur kostenlos",
        PAID_ONLY to "Nur bezahlt",
    )

    fun label(id: String): String = CHOICES.firstOrNull { it.first == id }?.second ?: CHOICES.first().second
}

open class TtsException(message: String, cause: Throwable? = null) : Exception(message, cause)

sealed interface TtsPlaybackState {
    data object Idle : TtsPlaybackState
    data class Preparing(val paragraphIndex: Int, val paragraphCount: Int) : TtsPlaybackState
    data class Playing(val paragraphIndex: Int, val paragraphCount: Int) : TtsPlaybackState
    data class Paused(val paragraphIndex: Int, val paragraphCount: Int) : TtsPlaybackState
    data class Error(val message: String) : TtsPlaybackState
}
