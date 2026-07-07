package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.diagnostics.Diag
import de.frank.entropyreducer.data.diagnostics.DiagnosticArea
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.domain.model.EntropyCategory
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Wandelt einen rohen Whisper-Transkript-Text einer manuell hinzugefuegten Methode
 * in eine inhaltlich klare, prägnante Methoden-Beschreibung um.
 *
 * Frank-Wunsch 2026-05-09: Beim manuellen Hinzufuegen einer bestätigten Methode
 * im Insight Board soll der Sprachtext per KI verbessert werden — pragnant,
 * inhaltlich klar, deutsche Umlaute (ä ö ü ß), kein Floskel. Zusätzlich soll
 * die KI ALLE passenden Kategorien benennen, nicht nur eine — z.B. "Laufen"
 * wirkt mental + koerperlich + emotional gleichzeitig (BDNF/NGF, mentale
 * Klarheit, Schmerzreduktion).
 *
 * Output:
 *  - title: max 60 Zeichen, Methoden-Kern als Substantiv-Phrase
 *  - description: 2-4 Sätze, "wie und warum" der Methode, Du-Anrede
 *  - primaryCategory: die zentrale Kategorie
 *  - additionalCategories: alle weiteren Kategorien die die Methode adressiert
 *
 * Fallback bei API-Fehler oder fehlendem Key: roher Text wird Description, erste
 * 60 Zeichen Title, primary = SONSTIGES, additionals = leer. Damit der Save-Flow
 * niemals blockiert ist — Frank kann den Text danach im Detail-Sheet manuell
 * korrigieren.
 */
class PolishMethodTextUseCase @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
    private val json: Json,
) {

    data class PolishedMethod(
        val title: String,
        val description: String,
        val primaryCategory: EntropyCategory,
        val additionalCategories: List<EntropyCategory>,
        val usedAi: Boolean,
    )

    suspend operator fun invoke(rawTranscript: String): PolishedMethod {
        val key = secrets.geminiApiKey
        if (key.isNullOrBlank() || rawTranscript.isBlank()) {
            return fallback(rawTranscript)
        }
        val model = settings.geminiModelFlow.first()
        return try {
            val response = gemini.generateContent(
                model = model,
                apiKey = key,
                request = GeminiRequest(
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(SYSTEM_PROMPT))),
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart("Hier ist meine eingesprochene Methoden-Notiz, transkribiert: $rawTranscript")),
                        ),
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.4,
                        responseMimeType = "application/json",
                    ),
                ),
            )
            val rawJson = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.let { stripCodeFence(it) }
                ?: return fallback(rawTranscript)

            val dto = runCatching {
                json.decodeFromString(PolishedMethodDto.serializer(), rawJson)
            }.getOrNull() ?: return fallback(rawTranscript)

            val primary = parseCategory(dto.primaryCategory) ?: EntropyCategory.SONSTIGES
            val additionals = dto.additionalCategories
                .mapNotNull { parseCategory(it) }
                .filter { it != primary }
                .distinct()

            PolishedMethod(
                title = dto.title.trim().take(60).ifBlank { rawTranscript.take(60) },
                description = dto.description.trim().ifBlank { rawTranscript.trim() },
                primaryCategory = primary,
                additionalCategories = additionals,
                usedAi = true,
            )
        } catch (t: Throwable) {
            Diag.e(DiagnosticArea.GEMINI, TAG, "Polish-API-Call fehlgeschlagen — Fallback auf rohen Text", t)
            fallback(rawTranscript)
        }
    }

    private fun fallback(raw: String): PolishedMethod {
        val cleaned = raw.trim()
        return PolishedMethod(
            title = cleaned.take(60).ifBlank { "Neue Methode" },
            description = cleaned.ifBlank { "(Keine Beschreibung)" },
            primaryCategory = EntropyCategory.SONSTIGES,
            additionalCategories = emptyList(),
            usedAi = false,
        )
    }

    private fun parseCategory(s: String?): EntropyCategory? {
        if (s.isNullOrBlank()) return null
        return runCatching { EntropyCategory.valueOf(s.trim().uppercase()) }.getOrNull()
    }

    private fun stripCodeFence(s: String): String {
        val trimmed = s.trim()
        return when {
            trimmed.startsWith("```json") -> trimmed.removePrefix("```json").removeSuffix("```").trim()
            trimmed.startsWith("```") -> trimmed.removePrefix("```").removeSuffix("```").trim()
            else -> trimmed
        }
    }

    @Serializable
    data class PolishedMethodDto(
        val title: String,
        val description: String,
        val primaryCategory: String,
        val additionalCategories: List<String> = emptyList(),
    )

    companion object {
        private const val TAG = "PolishMethodText"
        private val SYSTEM_PROMPT = """
Du verarbeitest eine eingesprochene Notiz von Frank, in der er eine bestätigte Methode zur Reduktion persönlicher Entropie beschreibt — eine Maßnahme, die für ihn nachweislich gut funktioniert.

Deine Aufgabe: den rohen Sprachtext in eine prägnante, klar formulierte Methoden-Karte umwandeln und ALLE passenden Kategorien benennen — nicht nur eine.

Stil-Regeln (PFLICHT):
- Echtes Deutsch mit Umlauten ä ö ü ß. NIEMALS ae/oe/ue/ss als Ersatz.
- Du-Anrede.
- Keine Floskeln, kein "es ist wichtig zu", kein "man sollte". Direkt und konkret.
- Behalte Frank's eigene Wörter/Bezeichnungen wenn er sie nutzt (z.B. "Laufen", "BDNF", "Borreliose").
- Keine Anführungszeichen um den Inhalt.

Inhaltliche Regeln:
- title: max 60 Zeichen, Methoden-Kern als kurze Substantiv-Phrase oder Imperativ. Kein Endpunkt.
- description: 2-4 Sätze, beantwortet WIE die Methode angewendet wird und WARUM sie wirkt (z.B. biochemisch, psychologisch, körperlich). Behalte Frank's eigene Beobachtungen.
- primaryCategory: die zentrale Kategorie aus genau diesen 7 Werten — KOERPERLICH, MENTAL, ZEITLICH, EMOTIONAL, GESUNDHEITLICH, UMGEBUNG, SONSTIGES.
- additionalCategories: ALLE weiteren Kategorien die die Methode ebenfalls adressiert. So viele wie zutreffen — eine Methode wie Laufen kann gleichzeitig MENTAL (Klarheit, BDNF), KOERPERLICH (Ausdauer), GESUNDHEITLICH (Symptomreduktion) und EMOTIONAL (Stimmung) sein. Nicht nur ankreuzen wenn der Effekt eindeutig ist. Lass primaryCategory NICHT in additionalCategories auftauchen.

Antworte AUSSCHLIESSLICH als JSON, ohne Markdown-Codeblock, ohne Einleitung, ohne Schluss:

{
  "title": "Kurzer prägnanter Titel max 60 Zeichen",
  "description": "2-4 Sätze klar formulierte Methoden-Beschreibung",
  "primaryCategory": "KOERPERLICH | MENTAL | ZEITLICH | EMOTIONAL | GESUNDHEITLICH | UMGEBUNG | SONSTIGES",
  "additionalCategories": ["MENTAL", "EMOTIONAL"]
}
        """.trimIndent()
    }
}
