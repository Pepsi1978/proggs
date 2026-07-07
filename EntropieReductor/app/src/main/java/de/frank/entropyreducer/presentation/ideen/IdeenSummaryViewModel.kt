package de.frank.entropyreducer.presentation.ideen

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class SummaryState {
    IDLE,
    RUNNING,
    ERROR,
}

/**
 * Erzeugt eine KI-Bullet-Point-Zusammenfassung eines Ideen-Eintrags via Gemini (Frank-Wunsch
 * 2026-05-20). 1:1 Pendant zu BestJournalFrank's SummarizeEntryUseCase — Output ist ein
 * mehrzeiliger String, jede Zeile beginnt mit "• ".
 *
 * Bei Fehler (kein API-Key, kein Netz) wird der Status auf ERROR gesetzt; der Detail-Screen zeigt
 * dann den Knopf "Zusammenfassung erstellen" weiter an statt eine leere Liste.
 */
@HiltViewModel
class IdeenSummaryViewModel
@Inject
constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(SummaryState.IDLE)
    val state: StateFlow<SummaryState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Generiert eine Bullet-Point-Zusammenfassung für [text]. Bei Erfolg wird [onResult] mit dem
     * fertigen Multi-Line-String aufgerufen (jede Zeile beginnt mit "• "). Bei Fehler bleibt
     * [onResult] ungerufen und der Status wechselt auf ERROR.
     */
    fun generateSummary(text: String, onResult: (String) -> Unit) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _state.value = SummaryState.RUNNING
        _error.value = null
        viewModelScope.launch {
            val apiKey = secrets.geminiApiKey
            if (apiKey.isNullOrBlank()) {
                _state.value = SummaryState.ERROR
                _error.value = "Kein Gemini-API-Schlüssel hinterlegt"
                return@launch
            }
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
                                            temperature = 0.3,
                                            responseMimeType = "text/plain",
                                            maxOutputTokens = 300,
                                        ),
                                ),
                        )
                    response.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.firstOrNull()
                        ?.text
                        ?.let(::sanitizeProse)
                }
                .onSuccess { prose ->
                    if (!prose.isNullOrBlank()) {
                        _state.value = SummaryState.IDLE
                        onResult(prose)
                    } else {
                        _state.value = SummaryState.ERROR
                        _error.value = "Leere Antwort von Gemini"
                    }
                }
                .onFailure { ex ->
                    _state.value = SummaryState.ERROR
                    _error.value = ex.message ?: "Zusammenfassung fehlgeschlagen"
                }
        }
    }

    fun clearError() {
        _error.value = null
        if (_state.value == SummaryState.ERROR) _state.value = SummaryState.IDLE
    }

    /**
     * Frank-Wunsch 2026-05-23: Zusammenfassung ist ein Mini-Fliesstext (max 6 Zeilen),
     * keine Bullet-Points mehr. Wir saeubern Markdown-Reste, Bullet-Praefixe
     * (falls die KI doch welche liefert), Einleitungs-/Schlussfloskeln und joinen
     * Zeilen zu einem zusammenhaengenden Absatz. Wenn die KI mehrere kurze Saetze
     * untereinander geliefert hat, werden sie mit Leerzeichen verbunden — das
     * Detail-Layout begrenzt das Ganze dann via maxLines auf 6 Zeilen.
     */
    private fun sanitizeProse(raw: String): String {
        val cleaned =
            raw.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { line ->
                    line
                        .removePrefix("•")
                        .removePrefix("-")
                        .removePrefix("*")
                        .removePrefix("·")
                        .removePrefix("→")
                        .trim()
                        .let { stripped -> stripped.removePrefix("**").removeSuffix("**").trim() }
                        .replace(Regex("^\\d+[.)]\\s+"), "")
                }
                .filter { it.isNotBlank() && !it.endsWith(":") }
        // Saetze mit Leerzeichen verbinden — die UI begrenzt auf 6 Zeilen.
        return cleaned.joinToString(separator = " ").trim()
    }

    private companion object {
        const val SYSTEM_PROMPT =
            """
Du erzeugst eine kurze Mini-Fliesstext-Zusammenfassung eines Ideeneintrags.

Pflicht-Regeln:
- Ein zusammenhaengender Absatz, KEINE Bullet-Points, KEINE Aufzaehlung.
- Maximal 6 Zeilen — lieber knapper. Insgesamt nicht mehr als ca. 70 Woerter.
- Sprache: Deutsch mit echten Umlauten (ä ö ü ß).
- KEINE Einleitung wie "Hier ist die Zusammenfassung:". KEINE Schlussbemerkung.
- KEINE Anfuehrungszeichen. KEIN Markdown (kein **fett**, kein _kursiv_, keine Listen).
- Trifft den Kern: was passiert ist, was gefuehlt wurde, was wichtig ist.
- Schreibe in dritter Person oder neutral — KEIN "Ich".

Beispiel-Output:
Es gab Streit mit Mama wegen der unaufgeraeumten Wohnung. Gefuehle von Schuld und Wut traten gleichzeitig auf. Am Abend kam es zur Versoehnung bei einer Tasse Tee. Erkenntnis: Konflikte besser frueher ansprechen, dann eskalieren sie nicht so stark.
"""
    }
}
