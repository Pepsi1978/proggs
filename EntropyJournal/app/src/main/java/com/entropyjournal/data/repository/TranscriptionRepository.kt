package com.entropyjournal.data.repository

import android.content.SharedPreferences
import com.entropyjournal.data.local.whisper.LocalWhisperTranscriber
import com.entropyjournal.data.remote.groq.GroqApi
import com.entropyjournal.data.remote.groq.GroqTranscriptionRequest
import com.entropyjournal.util.Constants
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptionRepository @Inject constructor(
    private val groqApi: GroqApi,
    private val localWhisper: LocalWhisperTranscriber,
    private val encryptedPrefs: SharedPreferences
) {
    suspend fun transcribeAudio(audioFile: File): Result<String> {
        val groqKey = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: ""

        // Try Groq API first if key is configured
        if (groqKey.isNotBlank()) {
            try {
                val response = groqApi.transcribe(
                    file = GroqTranscriptionRequest.createFilePart(audioFile),
                    model = GroqTranscriptionRequest.createModelPart(),
                    language = GroqTranscriptionRequest.createLanguagePart(),
                    responseFormat = GroqTranscriptionRequest.createResponseFormatPart()
                )
                return Result.success(response.text)
            } catch (_: Exception) {
                // Groq failed — fall through to local Whisper
            }
        }

        // Fallback: Local Whisper via sherpa-onnx
        return localWhisper.transcribe(audioFile)
    }
}
