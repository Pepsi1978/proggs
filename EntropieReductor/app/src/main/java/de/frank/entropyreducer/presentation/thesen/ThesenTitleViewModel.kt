package de.frank.entropyreducer.presentation.thesen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Auto-Ueberschrift fuer Theseneintraege via Gemini (Frank-Wunsch 2026-05-19).
 *
 * Nach jedem neuen Eintrag (per Sprache oder Tastatur) wird Gemini gebeten eine sehr kurze
 * Ueberschrift (max 3 Woerter) aus dem Eintrags-Text zu erzeugen. Der Eintrag wird sofort mit
 * Fallback-Titel gespeichert; sobald Gemini antwortet, wird der Titel asynchron im DataStore
 * aktualisiert.
 *
 * Schlaegt der Aufruf fehl (kein Internet, kein API-Key, Quote ueberschritten), bleibt der
 * Fallback-Titel bestehen — keine Fehlermeldung im UI.
 */
@HiltViewModel
class ThesenTitleViewModel
@Inject
constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
) : ViewModel() {

    /**
     * Erzeugt einen Auto-Titel fuer den uebergebenen Eintrag und ruft [onResult] mit dem neuen
     * Titel auf. [onResult] wird nur dann aufgerufen, wenn Gemini einen sinnvollen Titel liefert —
     * bei Fehler passiert nichts (der Fallback-Titel im Eintrag bleibt).
     */
    fun generateTitle(text: String, onResult: (String) -> Unit) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val apiKey = secrets.geminiApiKey
            if (apiKey.isNullOrBlank()) return@launch
            runCatching {
                    val model = settings.geminiModelFlow.first()
                    val response =
                        gemini.generateContent(
                            model = model,
                            apiKey = apiKey,
                            request =
                                GeminiRequest(
                                    systemInstruction =
                                        GeminiContent(parts = listOf(GeminiPart(SYSTEM_PROMPT))),
                                    contents =
                                        listOf(
                                            GeminiContent(
                                                role = "user",
                                                parts = listOf(GeminiPart(trimmed)),
                                            )
                                        ),
                                    generationConfig =
                                        GeminiGenerationConfig(
                                            temperature = 0.25,
                                            responseMimeType = "text/plain",
                                            maxOutputTokens = 32,
                                        ),
                                ),
                        )
                    response.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text
                        ?.let(::sanitizeTitle)
                }
                .onSuccess { title -> if (!title.isNullOrBlank()) onResult(title) }
        }
    }

    private fun sanitizeTitle(raw: String): String {
        // Entferne Anfuehrungszeichen, Punkt am Ende, Markdown, Praefixe.
        val cleaned =
            raw.trim()
                .removePrefix("**")
                .removeSuffix("**")
                .removePrefix("\"")
                .removeSuffix("\"")
                .removePrefix("„")
                .removeSuffix("“")
                .removePrefix("'")
                .removeSuffix("'")
                .removeSuffix(".")
                .removeSuffix("…")
                .trim()
        // Max 3 Woerter erzwingen — falls Gemini doch mehr liefert.
        val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.take(3).joinToString(" ")
    }

    private companion object {
        const val SYSTEM_PROMPT =
            """
Du erzeugst eine sehr kurze Ueberschrift fuer einen Theseneintrag.

Pflicht-Regeln:
- MAXIMAL 3 Woerter. Lieber 1-2 Woerter als 3.
- Sprache: Deutsch mit echten Umlauten (ae->ä, oe->ö, ue->ü, ss->ß wo orthographisch korrekt).
- Erste Buchstaben gross schreiben (Substantive, Eigennamen).
- KEINE Anfuehrungszeichen, KEINE Punkte am Ende, KEINE Praefixe wie "Titel:" oder "Eintrag:".
- KEINE Ausrufezeichen, KEINE Fragezeichen.
- Gib NUR die Ueberschrift zurueck — sonst nichts.
- Trifft den Kern des Eintrags (Thema, Gefuehl, Ereignis).

Beispiele:
- "Heute morgen war ich total angespannt..." -> "Morgendliche Anspannung"
- "Ich habe mich heute mit Mama gestritten..." -> "Streit mit Mama"
- "Schoener Tag im Park gewesen, Sonne, ..." -> "Sonniger Parktag"
- "Schlecht geschlafen, Gedanken kreisten..." -> "Schlechter Schlaf"
"""
    }
}
