package com.bestjournal.app.data.repository

import android.content.SharedPreferences
import android.util.Log
import com.bestjournal.app.data.local.whisper.LocalWhisperTranscriber
import com.bestjournal.app.data.remote.ai.AiPhase
import com.bestjournal.app.data.remote.ai.AiUsageTracker
import com.bestjournal.app.data.remote.groq.GroqApi
import com.bestjournal.app.util.Constants
import com.bestjournal.app.util.DeviceLocale
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
    private val encryptedPrefs: SharedPreferences,
    private val aiUsageTracker: AiUsageTracker
) {
    suspend fun transcribeAudio(audioFile: File): Result<TranscriptionResult> {
        // Use Groq for subscribers AND trial users (free tier covers trial usage)
        val isSubscribed = encryptedPrefs.getBoolean("is_subscribed", false)
        val isTrialActive = aiUsageTracker.getCurrentPhase() == AiPhase.TRIAL
        if (isSubscribed || isTrialActive) {
            try {
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                remoteConfig.fetchAndActivate()
                val groqKey = remoteConfig.getString(Constants.REMOTE_CONFIG_GROQ_KEY)

                if (groqKey.isNotBlank()) {
                    val userType = if (isSubscribed) "premium subscriber" else "trial user"
                    Log.d("Transcription", "Using Groq API for $userType")
                    val filePart = MultipartBody.Part.createFormData(
                        "file", audioFile.name,
                        audioFile.asRequestBody("audio/wav".toMediaType())
                    )
                    val response = groqApi.transcribe(
                        authorization = "Bearer $groqKey",
                        file = filePart,
                        model = Constants.GROQ_TRANSCRIPTION_MODEL.toRequestBody("text/plain".toMediaType()),
                        language = DeviceLocale.languageCode.toRequestBody("text/plain".toMediaType()),
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

        // Fallback: local offline Whisper via sherpa-onnx
        // Used for: expired trial (freemium) users, OR when Groq API fails/rate-limited
        return localWhisper.transcribe(audioFile).map {
            TranscriptionResult(it, "Lokales Whisper-Modell")
        }
    }
}
