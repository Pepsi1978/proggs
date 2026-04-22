package com.entropyjournal.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.entropyjournal.data.local.whisper.LocalWhisperTranscriber
import com.entropyjournal.data.remote.groq.GroqApi
import com.entropyjournal.data.remote.groq.GroqTranscriptionRequest
import com.entropyjournal.util.Constants
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class TranscriptionEngine { GROQ, LOCAL }

data class TranscriptionOutcome(
    val text: String,
    val engine: TranscriptionEngine,
    // Set when Groq was configured and tried but failed, and Local handled the audio.
    // Callers can surface this so the user knows the fallback kicked in.
    val groqError: String? = null,
)

@Singleton
class TranscriptionRepository @Inject constructor(
    private val groqApi: GroqApi,
    private val localWhisper: LocalWhisperTranscriber,
    private val encryptedPrefs: SharedPreferences
) {
    suspend fun transcribeAudio(audioFile: File): Result<TranscriptionOutcome> {
        val groqKey = encryptedPrefs.getString(Constants.PREF_GROQ_API_KEY, "") ?: ""

        // Try Groq whisper-large-v3-turbo first if the user configured a key.
        var groqFailureReason: String? = null
        if (groqKey.isNotBlank()) {
            try {
                val response = groqApi.transcribe(
                    file = GroqTranscriptionRequest.createFilePart(audioFile),
                    model = GroqTranscriptionRequest.createModelPart(),
                    language = GroqTranscriptionRequest.createLanguagePart(),
                    responseFormat = GroqTranscriptionRequest.createResponseFormatPart()
                )
                return Result.success(
                    TranscriptionOutcome(text = response.text, engine = TranscriptionEngine.GROQ)
                )
            } catch (e: Exception) {
                // Never swallow silently (Direktive #3) — log and remember the reason
                // so the caller can tell the user why Groq was skipped.
                groqFailureReason = e.message ?: e.javaClass.simpleName
                Log.w(TAG, "Groq transcription failed, falling back to local Whisper", e)
            }
        }

        // Fallback: Local Whisper via sherpa-onnx.
        return localWhisper.transcribe(audioFile).map { text ->
            TranscriptionOutcome(
                text = text,
                engine = TranscriptionEngine.LOCAL,
                groqError = groqFailureReason,
            )
        }
    }

    private companion object {
        private const val TAG = "TranscriptionRepo"
    }
}
