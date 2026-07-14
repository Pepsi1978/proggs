package de.frank.entropyreducer.domain.usecase

import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Frank-Wunsch 2026-05-31: wertet einen diktierten Aufgaben-Text per Gemini auf.
 *
 * Ziel (Franks Worte): Das was Frank sagen wollte soll grammatikalisch und
 * rechtschreiblich sauber, klar und etwa GLEICH LANG wiedergegeben werden. Die
 * Wortwahl darf sich aendern, der Inhalt/die Intention MUSS vollstaendig erhalten
 * bleiben. KEINE neuen Fakten erfinden, nichts weglassen.
 *
 * Nutzt die bereits vorhandene Gemini-Infrastruktur (GeminiApi + geheimer Key +
 * Modell aus den Settings) — kein neuer Client noetig. Bei fehlendem Key oder
 * Fehler wird ein Result.failure zurueckgegeben; der Aufrufer behaelt dann den
 * Originaltext.
 */
class ImproveTextUseCase
@Inject
constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
) {
    suspend operator fun invoke(rawText: String): Result<String> {
        val text = rawText.trim()
        if (text.isBlank()) return Result.success(text)
        val key =
            secrets.geminiApiKey
                ?: return Result.failure(IllegalStateException("Kein Gemini-Key hinterlegt"))
        val model = settings.geminiModelFlow.first()
        return try {
            val response =
                gemini.generateContent(
                    model = model,
                    apiKey = key,
                    request =
                        GeminiRequest(
                            systemInstruction =
                                GeminiContent(parts = listOf(GeminiPart(SYSTEM_PROMPT))),
                            contents =
                                listOf(
                                    GeminiContent(
                                        role = "user",
                                        parts = listOf(GeminiPart(text)),
                                    ),
                                ),
                            generationConfig = GeminiGenerationConfig(temperature = 0.4),
                        ),
                )
            val improved =
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: return Result.failure(IllegalStateException("Leere Antwort von Gemini"))
            Result.success(improved)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    companion object {
        private val SYSTEM_PROMPT =
            """
Du bist ein deutscher Textredakteur fuer diktierte Spracheingaben.

AUFGABE:
Du erhaeltst einen diktierten Text (Speech-to-Text). Erkenne die INTENTION des
Sprechers und gib genau das, was er ausdruecken wollte, klar, praezise und
sprachlich hochwertig wieder.

VORGEHEN (in dieser Reihenfolge):
1) Erkenne die Absicht: Was will der Sprecher mitteilen, anweisen oder festhalten?
2) Entferne Diktat-Artefakte: Fuelllaute ("aeh", "aehm"), Stotterer, Wort-
   wiederholungen, sinnlose Fragmente.
3) Formuliere die Saetze so um, dass die Intention klar und gut lesbar wird.
   - Saetze duerfen umstrukturiert werden.
   - Die Wortwahl darf verbessert/veraendert werden.
4) Korrigiere Grammatik, Zeichensetzung und Gross-/Kleinschreibung.

LAENGE:
- Der ueberarbeitete Text soll ETWA GLEICH LANG sein wie das Original.
- Den Inhalt gleich ausfuehrlich wiedergeben — nichts kuerzen, nichts aufblaehen.

GRENZEN (strikt):
- KEINE neuen Informationen, Fakten oder Inhalte hinzufuegen.
- Keine Vermutungen ueber nicht Gesagtes.
- Die Intention und der Inhalt des Originals muessen vollstaendig erhalten bleiben.
- Sprache: Deutsch, mit echten Umlauten.
- Keine langen Gedankenstriche — nutze Kommas oder Punkte.

AUSGABE:
Gib AUSSCHLIESSLICH den ueberarbeiteten Text zurueck. Keine Kommentare, keine
Erklaerungen, kein Praefix, keine Anfuehrungszeichen.
            """
                .trim()
    }
}
