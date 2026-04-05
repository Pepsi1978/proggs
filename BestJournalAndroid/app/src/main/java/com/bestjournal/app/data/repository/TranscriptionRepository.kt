package com.bestjournal.app.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.bestjournal.app.data.local.whisper.LocalWhisperTranscriber
import com.bestjournal.app.data.remote.groq.GroqApi
import com.bestjournal.app.util.Constants
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class TranscriptionResult(
    val text: String,
    val model: String
)

@Singleton
class TranscriptionRepository @Inject constructor(
    private val localWhisper: LocalWhisperTranscriber,
    private val groqApi: GroqApi,
    private val encryptedPrefs: SharedPreferences
) {
    suspend fun transcribeAudio(audioFile: File): Result<TranscriptionResult> {
        // Only use Groq for subscribed users — key comes from Firebase Remote Config
        // TODO: Re-enable subscription check before release
        val isSubscribed = true // TEMP: encryptedPrefs.getBoolean("is_subscribed", false)
        if (isSubscribed) {
            try {
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                remoteConfig.fetchAndActivate()
                val groqKey = remoteConfig.getString(Constants.REMOTE_CONFIG_GROQ_KEY)

                if (groqKey.isNotBlank()) {
                    Log.d("Transcription", "Using Groq API for premium subscriber")
                    val filePart = MultipartBody.Part.createFormData(
                        "file", audioFile.name,
                        audioFile.asRequestBody("audio/wav".toMediaType())
                    )
                    val response = groqApi.transcribe(
                        authorization = "Bearer $groqKey",
                        file = filePart,
                        model = Constants.GROQ_TRANSCRIPTION_MODEL.toRequestBody("text/plain".toMediaType()),
                        language = Constants.GROQ_LANGUAGE.toRequestBody("text/plain".toMediaType()),
                        responseFormat = "json".toRequestBody("text/plain".toMediaType())
                    )
                    if (response.text.isNotBlank()) {
                        return Result.success(TranscriptionResult(response.text, "Groq Whisper Large V3"))
                    }
                }
            } catch (e: Exception) {
                Log.w("Transcription", "Groq failed, falling back to local Whisper: ${e.message}")
            }
        }

        // Fallback: ALWAYS works — local offline Whisper via sherpa-onnx
        return localWhisper.transcribe(audioFile).map {
            TranscriptionResult(it, "Lokales Whisper-Modell")
        }
    }
}
