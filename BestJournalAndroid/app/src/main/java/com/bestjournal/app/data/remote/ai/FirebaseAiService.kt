package com.bestjournal.app.data.remote.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAiService @Inject constructor() {

    companion object {
        const val MODEL_FLASH = "gemini-2.5-flash"
        const val MODEL_FLASH_LITE = "gemini-2.5-flash-lite"
    }

    private fun createModel(
        modelName: String = MODEL_FLASH,
        temperature: Float = 0.4f,
        maxOutputTokens: Int = 8192,
        systemPrompt: String? = null,
    ) =
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = modelName,
                generationConfig =
                    generationConfig {
                        this.temperature = temperature
                        this.maxOutputTokens = maxOutputTokens
                    },
                systemInstruction = systemPrompt?.let { content { text(it) } },
            )

    suspend fun generateContent(
        prompt: String,
        modelName: String = MODEL_FLASH,
        temperature: Float = 0.4f,
        maxOutputTokens: Int = 8192,
        systemPrompt: String? = null,
    ): Result<String> {
        return try {
            val model = createModel(modelName, temperature, maxOutputTokens, systemPrompt)
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
