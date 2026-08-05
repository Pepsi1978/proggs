package com.entropyjournal.data.remote.ai

import android.content.SharedPreferences
import android.util.Log
import com.entropyjournal.data.remote.codex.AuthErrorKind
import com.entropyjournal.data.remote.codex.CodexAuthException
import com.entropyjournal.data.remote.codex.CodexAuthManager
import com.entropyjournal.data.remote.codex.CodexModel
import com.entropyjournal.data.remote.codex.ReasoningEffort
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiCandidate
import com.entropyjournal.data.remote.gemini.GeminiCandidateContent
import com.entropyjournal.data.remote.gemini.GeminiPart
import com.entropyjournal.data.remote.gemini.GeminiRequest
import com.entropyjournal.data.remote.gemini.GeminiResponse
import com.entropyjournal.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Eine Tuer fuer die gesamte KI-Arbeit der App.
 *
 * Der Schalter in den Einstellungen entscheidet, ob eine Anfrage zu Gemini oder zu ChatGPT/Codex
 * geht. Die Aufrufer bleiben unveraendert: sie stellen weiterhin eine [GeminiRequest] und bekommen
 * eine [GeminiResponse] zurueck, damit alle Auswertungen ohne Umbau beide Anbieter nutzen koennen.
 */
@Singleton
class AiGateway @Inject constructor(
    private val geminiApi: GeminiApi,
    private val codexAuthManager: CodexAuthManager,
    private val encryptedPrefs: SharedPreferences,
) {

    suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequest,
    ): GeminiResponse {
        if (!useCodex()) {
            return geminiApi.generateContent(model = model, apiKey = apiKey, request = request)
        }
        if (!codexAuthManager.isConnected) {
            throw CodexAuthException(
                AuthErrorKind.REAUTH,
                "ChatGPT ist als KI ausgewählt, aber noch nicht angemeldet. " +
                    "Bitte in den Einstellungen bei ChatGPT anmelden.",
            )
        }
        val instructions = request.systemInstruction?.parts.orEmpty().joinToString("\n") { it.text }
        val input = request.contents.flatMap { it.parts }.joinToString("\n") { it.text }
        Log.d(TAG, "Codex request: ${codexModel().apiId} / ${effort().apiValue}, ${input.length} chars")
        val answer = codexAuthManager.generateText(
            instructions = instructions,
            input = input,
            model = codexModel(),
            reasoningEffort = effort(),
        )
        return GeminiResponse(
            candidates = listOf(
                GeminiCandidate(GeminiCandidateContent(listOf(GeminiPart(text = answer)))),
            ),
        )
    }

    /** true, wenn der gewaehlte Anbieter einsatzbereit ist (Gemini-Schluessel bzw. ChatGPT-Anmeldung). */
    fun isConfigured(): Boolean =
        if (useCodex()) codexAuthManager.isConnected else geminiKey().isNotBlank()

    /** Die Meldung fuer den Nutzer, wenn [isConfigured] false ist. */
    fun configurationError(): String =
        if (useCodex()) {
            "ChatGPT ist als KI ausgewählt, aber noch nicht angemeldet. " +
                "Bitte in den Einstellungen bei ChatGPT anmelden."
        } else {
            "Bitte Gemini API-Key in den Einstellungen eingeben"
        }

    private fun geminiKey(): String =
        encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""

    private fun useCodex(): Boolean =
        encryptedPrefs.getString(Constants.PREF_AI_PROVIDER, Constants.AI_PROVIDER_GEMINI) ==
            Constants.AI_PROVIDER_CODEX

    private fun codexModel(): CodexModel =
        CodexModel.fromId(encryptedPrefs.getString(Constants.PREF_CODEX_MODEL, null))

    private fun effort(): ReasoningEffort =
        ReasoningEffort.fromValue(encryptedPrefs.getString(Constants.PREF_CODEX_EFFORT, null))

    private companion object {
        const val TAG = "AiGateway"
    }
}
