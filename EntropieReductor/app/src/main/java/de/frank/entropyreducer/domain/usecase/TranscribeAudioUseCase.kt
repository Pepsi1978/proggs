package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.remote.GroqWhisperApi
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

/**
 * Whisper-Transkription via Groq. Spec §8.2.
 */
class TranscribeAudioUseCase @Inject constructor(
    private val api: GroqWhisperApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
) {

    suspend operator fun invoke(audioFile: File): Result<String> {
        val key = secrets.groqApiKey
            ?: return Result.failure(IllegalStateException("Kein Groq-Key hinterlegt"))
        val model = settings.whisperModelFlow.first()
        val language = settings.transcriptionLanguageFlow.first()

        return runCatching {
            val requestFile = audioFile.asRequestBody("audio/m4a".toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val plain = "text/plain".toMediaType()
            val response = api.transcribe(
                bearer = "Bearer $key",
                file = filePart,
                model = model.toRequestBody(plain),
                language = language.toRequestBody(plain),
                responseFormat = "json".toRequestBody(plain),
            )
            response.text.trim()
        }.also {
            // Audio-Datei nach erfolgreicher Transkription loeschen (Spec §20).
            audioFile.delete()
        }
    }
}
