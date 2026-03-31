package com.bestjournal.app.data.remote.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.content
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAiService @Inject constructor() {

    companion object {
        private const val MODEL_NAME = "gemini-2.5-flash"
    }

    private fun createModel(
        temperature: Float = 0.4f,
        maxOutputTokens: Int = 8192,
        systemPrompt: String? = null
    ) = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                this.temperature = temperature
                this.maxOutputTokens = maxOutputTokens
            },
            systemInstruction = systemPrompt?.let {
                content { text(it) }
            }
        )

    suspend fun generateContent(
        prompt: String,
        temperature: Float = 0.4f,
        maxOutputTokens: Int = 8192,
        systemPrompt: String? = null
    ): Result<String> {
        return try {
            val model = createModel(temperature, maxOutputTokens, systemPrompt)
            val response = model.generateContent(prompt)
            val text = response.text
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("No response text from Gemini"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
