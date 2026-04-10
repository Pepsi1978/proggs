package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.util.Constants
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ImproveTextUseCase
@Inject
constructor(private val geminiApi: GeminiApi, private val encryptedPrefs: SharedPreferences) {
    companion object {
        private const val TEMPERATURE = 0.4f
        private const val MAX_OUTPUT_TOKENS = 8192
        private const val TRUNCATION_RATIO = 0.85
        private const val CHUNK_MAX_CHARS = 3500
    }

    private fun buildPrompt(text: String): String =
        """
Du bist ein deutscher Textredakteur für diktierte Spracheingaben.

AUFGABE:
Du erhältst einen diktierten Text (Speech-to-Text). Deine Aufgabe ist es, die **Intention** des Sprechers zu erkennen und den Text so umzuformulieren, dass diese Intention **klar, präzise und sprachlich hochwertig** zum Ausdruck kommt.

VORGEHEN (in dieser Reihenfolge):
1) Erkenne die Absicht: Was will der Sprecher mitteilen, fragen, anweisen oder ausdrücken?
2) Entferne Diktat-Artefakte: Fülllaute ("äh", "ähm", "öhm"), Stotterer, Wortwiederholungen, sinnlose Fragmente.
3) Formuliere Sätze so um, dass die erkannte Intention **klar und gut lesbar** wird.
   - Sätze dürfen umstrukturiert werden.
   - Wortwahl darf verbessert werden.
   - Satzgrenzen dürfen neu gesetzt werden.
4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.

GRENZEN (strikt):
- Keine neuen Informationen, Fakten oder Inhalte hinzufügen.
- Keine Vermutungen über nicht Gesagtes.
- Die Intention des Originals muss vollständig erhalten bleiben.
- Keine langen Gedankenstriche (—). Nutze Kommas oder kurze Sätze.
- Sprache: Deutsch.

REGEL:
Gib AUSSCHLIESSLICH den überarbeiteten Text zurück.
Keine Kommentare. Keine Erklärungen. Kein Präfix.

TEXT:
$text
    """
            .trim()

    private fun getSelectedModel(): String {
        return encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL)
            ?: Constants.DEFAULT_GEMINI_MODEL
    }

    private fun getApiKey(): String {
        return encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
    }

    private suspend fun rewriteChunk(text: String): String {
        val request =
            GeminiRequestBuilder.build(
                userText = buildPrompt(text),
                temperature = TEMPERATURE,
                maxOutputTokens = MAX_OUTPUT_TOKENS,
            )
        val response =
            geminiApi.generateContent(
                model = getSelectedModel(),
                apiKey = getApiKey(),
                request = request,
            )
        return response.extractText()?.trim()?.replace("—", ", ") ?: text
    }

    /**
     * Split text into chunks by paragraphs, respecting sentence boundaries. Mirrors the
     * Tampermonkey chatgpt.user.js splitIntoChunksByParagraphs logic.
     */
    private fun splitIntoChunks(text: String): List<String> {
        if (text.length <= CHUNK_MAX_CHARS) return listOf(text)

        val paragraphs = text.split(Regex("\n{2,}"))
        val chunks = mutableListOf<String>()
        var buffer = ""

        for (paragraph in paragraphs) {
            val p = paragraph.trim()
            if (p.isEmpty()) continue

            if (p.length > CHUNK_MAX_CHARS) {
                if (buffer.isNotBlank()) {
                    chunks.add(buffer)
                    buffer = ""
                }
                // Split long paragraph at sentence boundaries
                var start = 0
                while (start < p.length) {
                    var end = minOf(start + CHUNK_MAX_CHARS, p.length)
                    if (end < p.length) {
                        val windowStart = maxOf(start, end - (CHUNK_MAX_CHARS / 2))
                        val slice = p.substring(windowStart, end)
                        val lastBreak =
                            maxOf(
                                slice.lastIndexOf('.'),
                                slice.lastIndexOf('!'),
                                slice.lastIndexOf('?'),
                                slice.lastIndexOf(';'),
                            )
                        if (lastBreak > -1) end = windowStart + lastBreak + 1
                    }
                    chunks.add(p.substring(start, end).trim())
                    start = end
                }
                continue
            }

            val candidate = if (buffer.isNotBlank()) "$buffer\n\n$p" else p
            if (candidate.length > CHUNK_MAX_CHARS) {
                if (buffer.isNotBlank()) chunks.add(buffer)
                buffer = p
            } else {
                buffer = candidate
            }
        }
        if (buffer.isNotBlank()) chunks.add(buffer)
        return if (chunks.isNotEmpty()) chunks else listOf(text)
    }

    suspend operator fun invoke(rawText: String): Result<String> {
        return try {
            val apiKey = getApiKey()
            if (apiKey.isBlank())
                return Result.failure(IllegalStateException("Gemini API-Key nicht konfiguriert"))

            // First attempt: one-shot
            val oneShot = rewriteChunk(rawText)
            val ratio = oneShot.length.toDouble() / maxOf(1, rawText.length)

            // If result is long enough, accept it
            if (ratio >= TRUNCATION_RATIO) {
                return Result.success(oneShot)
            }

            // Text was truncated — split into chunks and process in parallel
            val chunks = splitIntoChunks(rawText)
            val results = coroutineScope {
                chunks.map { chunk -> async { rewriteChunk(chunk) } }.map { it.await() }
            }

            Result.success(results.joinToString("\n\n").trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
