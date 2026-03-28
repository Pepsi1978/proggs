package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.util.Constants
import javax.inject.Inject

class ImproveTextUseCase @Inject constructor(
    private val geminiApi: GeminiApi,
    private val encryptedPrefs: SharedPreferences
) {
    private fun buildPrompt(text: String): String = """
Du bist ein deutscher Textredakteur für diktierte Spracheingaben.

AUFGABE:
Du erhältst einen diktierten Text (Speech-to-Text). Deine Aufgabe ist es, die **Intention** des Sprechers zu erkennen und den Text so umzuformulieren, dass diese Intention **klar, präzise und sprachlich hochwertig** zum Ausdruck kommt. Der Text soll wie natürlich formuliertes, flüssiges Deutsch klingen.

VORGEHEN (in dieser Reihenfolge):
1) Erkenne die Absicht: Was will der Sprecher mitteilen, fragen, anweisen oder ausdrücken?
2) Entferne Diktat-Artefakte: Fülllaute ("äh", "ähm", "öhm"), Stotterer, Wortwiederholungen, sinnlose Fragmente.
3) Formuliere Sätze so um, dass die erkannte Intention **klar und gut lesbar** wird.
   - Sätze dürfen umstrukturiert werden.
   - Wortwahl darf verbessert werden — verwende natürliche, flüssige Formulierungen.
   - Satzgrenzen dürfen neu gesetzt werden.
4) Korrigiere Grammatik, Zeichensetzung und Groß-/Kleinschreibung.
5) Der verbesserte Text MUSS **ungefähr gleich lang** sein wie das Original.
   Kürze den Text NICHT drastisch. Fasse NICHT zusammen. Streiche KEINE ganzen Gedanken.

GRENZEN (strikt):
- Keine neuen Informationen, Fakten oder Inhalte hinzufügen.
- Keine Vermutungen über nicht Gesagtes.
- Die Intention und alle Inhalte des Originals müssen vollständig erhalten bleiben.
- Behalte die Ich-Perspektive bei.
- Sprache: Deutsch.

REGEL:
Gib AUSSCHLIESSLICH den überarbeiteten Text zurück.
Keine Kommentare. Keine Erklärungen. Kein Präfix.

TEXT:
$text
    """.trim()

    private fun getSelectedModel(): String {
        return encryptedPrefs.getString(Constants.PREF_GEMINI_MODEL, Constants.DEFAULT_GEMINI_MODEL)
            ?: Constants.DEFAULT_GEMINI_MODEL
    }

    suspend operator fun invoke(rawText: String): Result<String> {
        return try {
            val apiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
            if (apiKey.isBlank()) return Result.failure(IllegalStateException("Gemini API-Key nicht konfiguriert"))

            val request = GeminiRequestBuilder.build(
                userText = buildPrompt(rawText)
            )
            val response = geminiApi.generateContent(
                model = getSelectedModel(),
                apiKey = apiKey,
                request = request
            )
            val improvedText = response.extractText()
                ?: return Result.failure(Exception("Keine Antwort von Gemini"))

            Result.success(improvedText.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
