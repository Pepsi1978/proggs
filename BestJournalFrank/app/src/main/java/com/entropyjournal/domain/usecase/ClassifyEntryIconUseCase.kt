package com.entropyjournal.domain.usecase

import android.content.SharedPreferences
import com.entropyjournal.data.remote.gemini.GeminiApi
import com.entropyjournal.data.remote.gemini.GeminiRequestBuilder
import com.entropyjournal.ui.components.LucideIconPool
import com.entropyjournal.util.Constants
import javax.inject.Inject

/**
 * KI-basierter Symbol-Klassifikator fuer Tagebucheintraege.
 *
 * Bekommt den Eintragstext + den Pool aller verfuegbaren Symbole mit deutschen
 * Beschreibungen und gibt das am besten passende Symbol als iconKey zurueck.
 *
 * Nutzt das billigste Gemini-Modell (gemini-2.5-flash-lite) mit niedriger
 * Temperatur fuer deterministische Klassifikation.
 *
 * Fehlerverhalten: gibt null zurueck wenn API-Key fehlt, Internet weg, oder
 * Antwort kein gueltiger iconKey ist. Der Aufrufer faellt dann auf das
 * Stichwort-Mapping zurueck.
 */
class ClassifyEntryIconUseCase
@Inject
constructor(
    private val geminiApi: GeminiApi,
    private val encryptedPrefs: SharedPreferences,
) {
    companion object {
        private const val TEMPERATURE = 0.2f
        private const val MAX_OUTPUT_TOKENS = 50
        // Fuer Klassifikation immer das billigste/schnellste Modell — nicht das vom User gewaehlte.
        private const val CLASSIFIER_MODEL = "gemini-2.5-flash-lite"
        private const val MAX_INPUT_CHARS = 2000  // Eintrags-Text bei Bedarf truncen
    }

    suspend fun classify(entryTitle: String?, entryBody: String): Result<String?> {
        return try {
            val apiKey = encryptedPrefs.getString(Constants.PREF_GEMINI_API_KEY, "") ?: ""
            if (apiKey.isBlank()) {
                return Result.success(null) // kein Key → still failen, Fallback uebernimmt
            }

            val combinedText = buildString {
                if (!entryTitle.isNullOrBlank()) append(entryTitle).append("\n\n")
                append(entryBody)
            }.take(MAX_INPUT_CHARS)

            if (combinedText.isBlank()) return Result.success(null)

            val prompt = buildPrompt(combinedText)
            val request = GeminiRequestBuilder.build(
                userText = prompt,
                temperature = TEMPERATURE,
                maxOutputTokens = MAX_OUTPUT_TOKENS,
            )
            val response = geminiApi.generateContent(
                model = CLASSIFIER_MODEL,
                apiKey = apiKey,
                request = request,
            )

            val raw = response.extractText()?.trim()?.lowercase() ?: ""
            val cleanedKey = raw
                .removePrefix("\"")
                .removeSuffix("\"")
                .removePrefix("`")
                .removeSuffix("`")
                .trim()

            val matchedKey = LucideIconPool.byKey.keys.firstOrNull { it == cleanedKey }
            Result.success(matchedKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(entryText: String): String {
        val poolList = LucideIconPool.all.joinToString("\n") { entry ->
            "- ${entry.key}: ${entry.description}"
        }
        return """
Du bist ein Symbol-Klassifikator fuer ein Tagebuch. Lies den folgenden Tagebucheintrag und waehle aus der unteren Liste das EINE Symbol das am besten zum Inhalt passt.

TAGEBUCHEINTRAG:
$entryText

VERFUEGBARE SYMBOLE (Format "schluessel: Beschreibung"):
$poolList

REGELN:
- Gib nur den Schluessel zurueck (z.B. "glass_water" oder "brain"), keine Anfuehrungszeichen, keinen Punkt, keine Erklaerung.
- Waehle das thematisch passendste Symbol — nicht nur ein zufaellig erwaehntes Wort, sondern den Kern des Eintrags.
- Wenn der Eintrag z.B. von "Wasser trinken" handelt, waehle "glass_water" — nicht "sun" wegen "Morgen".
- Wenn der Eintrag von "Konzentration" oder "Sprachkompetenz" handelt, waehle "brain" — nicht "utensils" wegen "Mittagessen".
- Antworte NUR mit dem Schluessel.
        """.trimIndent()
    }
}
