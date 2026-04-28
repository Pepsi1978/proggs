package com.bestjournal.app.domain.usecase

import android.content.Context
import com.bestjournal.app.R
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.util.DeviceLocale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ImproveTextUseCase @Inject constructor(
    private val firebaseAiService: FirebaseAiService,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TEMPERATURE = 0.4f
        private const val MAX_OUTPUT_TOKENS = 8192
        private const val TRUNCATION_RATIO = 0.85
        private const val CHUNK_MAX_CHARS = 3500
    }

    private fun buildPrompt(text: String): String {
        val lang = DeviceLocale.promptLanguage
        return """
Du bist ein erfahrener Textredakteur für diktierte Spracheingaben.

AUFGABE:
Du erhältst einen diktierten Text (Speech-to-Text). Deine Aufgabe ist es, die **Intention** des Sprechers zu erkennen und den Text so umzuformulieren, dass diese Intention **klar, präzise und sprachlich hochwertig** zum Ausdruck kommt.

VORGEHEN (in dieser Reihenfolge):
1) Erkenne die Absicht: Was will der Sprecher mitteilen, fragen, anweisen oder ausdrücken?
2) Entferne Diktat-Artefakte: Fülllaute, Stotterer, Wortwiederholungen, sinnlose Fragmente.
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
REGEL:
Gib AUSSCHLIESSLICH den überarbeiteten Text zurück.
Keine Kommentare. Keine Erklärungen. Kein Präfix.

${context.getString(R.string.ai_prompt_response_language)}

TEXT:
$text
    """
            .trim()
    }

    private suspend fun rewriteChunk(text: String, modelName: String): String {
        val result =
            firebaseAiService.generateContent(
                prompt = buildPrompt(text),
                modelName = modelName,
                temperature = TEMPERATURE,
                maxOutputTokens = MAX_OUTPUT_TOKENS,
            )
        return result.getOrNull()?.trim()?.replace("—", ", ") ?: text
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
                                // CJK full-width sentence-ending punctuation
                                slice.lastIndexOf('\u3002'), // 。Chinese/Japanese period
                                slice.lastIndexOf('\uFF01'), // ！full-width exclamation
                                slice.lastIndexOf('\uFF1F'), // ？full-width question mark
                                slice.lastIndexOf('\uFF1B'), // ；full-width semicolon
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

    suspend operator fun invoke(
        rawText: String,
        modelName: String = FirebaseAiService.MODEL_FLASH,
    ): Result<String> {
        return try {
            // First attempt: one-shot
            val oneShot = rewriteChunk(rawText, modelName)
            val ratio = oneShot.length.toDouble() / maxOf(1, rawText.length)

            // If result is long enough, accept it. KI-Kennzeichnung erfolgt rein ueber
            // den Tab-Header in der UI ("✨ Mit KI verbessert") — kein Plain-Text-Suffix
            // mehr im DB-Inhalt, weil die isImproved-Kennzeichnung am Datensatz schon
            // ausreicht und die UI das Label transparent darueber rendert. Erfuellt
            // EU AI Act Art. 50 Abs. 1 (Transparenz bei AI-Interaktion).
            if (ratio >= TRUNCATION_RATIO) {
                return Result.success(oneShot)
            }

            // Text was truncated — split into chunks and process in parallel
            val chunks = splitIntoChunks(rawText)
            val results = coroutineScope {
                chunks.map { chunk -> async { rewriteChunk(chunk, modelName) } }.map { it.await() }
            }

            Result.success(results.joinToString("\n\n").trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
