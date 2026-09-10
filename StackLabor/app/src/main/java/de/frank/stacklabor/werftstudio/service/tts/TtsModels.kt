package de.frank.stacklabor.werftstudio.service.tts

import java.io.File

enum class TtsProviderId {
    EDGE,
    GOOGLE_CLOUD,
    QWEN,
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
}

open class TtsException(message: String, cause: Throwable? = null) : Exception(message, cause)

sealed interface TtsPlaybackState {
    data object Idle : TtsPlaybackState
    data class Preparing(val paragraphIndex: Int, val paragraphCount: Int) : TtsPlaybackState
    data class Playing(val paragraphIndex: Int, val paragraphCount: Int) : TtsPlaybackState
    data class Paused(val paragraphIndex: Int, val paragraphCount: Int) : TtsPlaybackState
    data class Error(val message: String) : TtsPlaybackState
}
