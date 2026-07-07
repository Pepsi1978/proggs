package com.bestjournal.app.data.remote.ai

import android.content.Context
import android.util.Log
import com.bestjournal.app.R
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAiService @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        const val MODEL_FLASH = "gemini-3.1-flash-lite"
        const val MODEL_FLASH_LITE = "gemini-2.5-flash-lite"
        // Auto-fallback target whenever MODEL_FLASH fails.
        // "-latest" aliases always resolve to the most recent generally-available
        // flash-lite model — chosen as a safe net against model rename or
        // temporary unavailability of the primary channel.
        const val MODEL_FLASH_FALLBACK = "gemini-flash-lite-latest"
        private const val TAG = "FirebaseAiService"
    }

    private data class ModelCacheKey(
        val modelName: String,
        val temperature: Float,
        val maxOutputTokens: Int?,
        val systemPrompt: String?,
    )

    private val modelCache = ConcurrentHashMap<ModelCacheKey, GenerativeModel>()

    private fun createModel(
        modelName: String = MODEL_FLASH_LITE,
        temperature: Float = 0.4f,
        maxOutputTokens: Int? = null,
        systemPrompt: String? = null,
    ): GenerativeModel {
        val key = ModelCacheKey(modelName, temperature, maxOutputTokens, systemPrompt)
        // computeIfAbsent is atomic in ConcurrentHashMap — prevents duplicate model creation
        return modelCache.computeIfAbsent(key) {
            Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = modelName,
                    generationConfig =
                        generationConfig {
                            this.temperature = temperature
                            if (maxOutputTokens != null) this.maxOutputTokens = maxOutputTokens
                        },
                    systemInstruction = systemPrompt?.let { content { text(it) } },
                )
        }
    }

    suspend fun generateContent(
        prompt: String,
        modelName: String = MODEL_FLASH_LITE,
        temperature: Float = 0.4f,
        maxOutputTokens: Int? = null,
        systemPrompt: String? = null,
    ): Result<String> {
        // Primary attempt with the requested model.
        val primaryResult =
            generateContentSingle(prompt, modelName, temperature, maxOutputTokens, systemPrompt)

        // Auto-fallback only for MODEL_FLASH (the -preview channel that can be
        // deprecated, renamed, or temporarily unavailable). Other models are
        // returned as-is so we don't loop or silently downgrade explicit choices.
        if (primaryResult.isSuccess || modelName != MODEL_FLASH) return primaryResult

        Log.w(
            TAG,
            "Primary model $modelName failed " +
                "(${primaryResult.exceptionOrNull()?.javaClass?.simpleName}: " +
                "${primaryResult.exceptionOrNull()?.message}) — " +
                "retrying with fallback $MODEL_FLASH_FALLBACK",
        )
        return generateContentSingle(
            prompt,
            MODEL_FLASH_FALLBACK,
            temperature,
            maxOutputTokens,
            systemPrompt,
        )
    }

    private suspend fun generateContentSingle(
        prompt: String,
        modelName: String,
        temperature: Float,
        maxOutputTokens: Int?,
        systemPrompt: String?,
    ): Result<String> {
        return try {
            Log.d(
                TAG,
                "generateContent start: model=$modelName, promptChars=${prompt.length}, systemChars=${systemPrompt?.length ?: 0}",
            )
            val t0 = System.currentTimeMillis()
            val model = createModel(modelName, temperature, maxOutputTokens, systemPrompt)
            val response = model.generateContent(prompt)
            val elapsed = System.currentTimeMillis() - t0
            Log.d(TAG, "generateContent returned after ${elapsed}ms (model=$modelName)")
            val text =
                try {
                    response.text
                } catch (_: Exception) {
                    response.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.filterIsInstance<com.google.firebase.ai.type.TextPart>()
                        ?.joinToString("") { it.text }
                }
            if (text != null) {
                Log.d(
                    TAG,
                    "generateContent success: ${text.length} chars returned (model=$modelName)",
                )
                Result.success(text)
            } else {
                Log.w(
                    TAG,
                    "generateContent: response.text is null, candidates=${response.candidates?.size}",
                )
                Result.failure(Exception(context.getString(R.string.dashboard_gemini_unavailable)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "generateContent FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }
}
