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

/**
 * Text-Verbesserung fuer Ideeneintraege via Gemini.
 *
 * Frank-Wunsch 2026-05-18: Nach der Sprach-Aufnahme via Groq Whisper soll
 * der Roh-Text optional von Gemini stilistisch und grammatikalisch
 * verbessert werden — Inhalt bleibt erhalten, Tonfall bleibt persoenlich.
 * Funktioniert analog zur Text-Verbesserung in BestJournalFrank.
 */
enum class ImproveState { IDLE, RUNNING }

@HiltViewModel
class IdeenImproveViewModel @Inject constructor(
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(ImproveState.IDLE)
    val state: StateFlow<ImproveState> = _state.asStateFlow()

    private val _improvedText = MutableStateFlow<String?>(null)
    val improvedText: StateFlow<String?> = _improvedText.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearImproved() {
        _improvedText.value = null
        _error.value = null
    }

    /**
     * Frank-Wunsch 2026-05-23: Eintrag/Followup-spezifische Verbesserung — Ergebnis
     * geht direkt an onResult ohne den geteilten _improvedText-State zu beeinflussen.
     * Erlaubt parallele Verbesserung mehrerer Texte (Eintrag plus N Followups).
     * onResult bekommt entweder den verbesserten Text oder null bei Fehler.
     */
    fun improveOnce(original: String, onResult: (String?) -> Unit) {
        val text = original.trim()
        if (text.isEmpty()) {
            onResult(null)
            return
        }
        viewModelScope.launch {
            val apiKey = secrets.geminiApiKey
            if (apiKey.isNullOrBlank()) {
                _error.value = "Bitte Gemini-API-Key in den Einstellungen hinterlegen."
                onResult(null)
                return@launch
            }
            runCatching {
                val model = settings.geminiModelFlow.first()
                val response = gemini.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = GeminiRequest(
                        systemInstruction = GeminiContent(
                            parts = listOf(GeminiPart(SYSTEM_PROMPT)),
                        ),
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart("Hier ist mein Ideeneintrag (Roh-Transkript):\n\n$text")),
                            ),
                        ),
                        generationConfig = GeminiGenerationConfig(
                            temperature = 0.35,
                            responseMimeType = "text/plain",
                        ),
                    ),
                )
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
            }
                .onSuccess { improved ->
                    if (improved.isNullOrBlank()) onResult(null) else onResult(improved)
                }
                .onFailure { ex ->
                    _error.value = ex.message ?: "Text-Verbesserung fehlgeschlagen"
                    onResult(null)
                }
        }
    }

    fun improve(original: String) {
        if (_state.value == ImproveState.RUNNING) return
        val text = original.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            val apiKey = secrets.geminiApiKey
            if (apiKey.isNullOrBlank()) {
                _error.value = "Bitte Gemini-API-Key in den Einstellungen hinterlegen."
                return@launch
            }
            _state.value = ImproveState.RUNNING
            _error.value = null
            runCatching {
                val model = settings.geminiModelFlow.first()
                val response = gemini.generateContent(
                    model = model,
                    apiKey = apiKey,
                    request = GeminiRequest(
                        systemInstruction = GeminiContent(
                            parts = listOf(GeminiPart(SYSTEM_PROMPT)),
                        ),
                        contents = listOf(
                            GeminiContent(
                                role = "user",
                                parts = listOf(GeminiPart("Hier ist mein Ideeneintrag (Roh-Transkript):\n\n$text")),
                            ),
                        ),
                        generationConfig = GeminiGenerationConfig(
                            temperature = 0.35,
                            responseMimeType = "text/plain",
                        ),
                    ),
                )
                response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                    ?: text
            }
                .onSuccess { improved ->
                    _improvedText.value = improved
                    _state.value = ImproveState.IDLE
                }
                .onFailure { ex ->
                    _error.value = ex.message ?: "Text-Verbesserung fehlgeschlagen"
                    _state.value = ImproveState.IDLE
                }
        }
    }

    private companion object {
        const val SYSTEM_PROMPT = """
Du bist ein Lektor fuer persoenliche Ideen- und Entropie-Eintraege. Frank-Wunsch
2026-05-23: Du darfst den Text deutlich umschreiben, damit er in sehr gutem Deutsch
mit klarer Logik dasteht — nicht nur Rechtschreibung, sondern auch Grammatik, Satzbau,
Strukturierung. Die BOTSCHAFT muss exakt erhalten bleiben, aber das Wie darf umgeformt
werden.

Pflicht-Regeln:
- Sprache: Deutsch mit echten Umlauten (ae->ä, oe->ö, ue->ü, ss->ß wo orthographisch korrekt).
- Korrigiere Rechtschreibung, Grammatik und Zeichensetzung vollstaendig.
- Vereinfache komplizierte oder verschachtelte Saetze. Trenne Saetze wenn sie zu lang sind.
- Sortiere Gedanken logisch um wenn der Originaltext spruenge macht (z.B. Wiederaufnahme eines Themas).
- Du darfst Worte ersetzen damit der Text eleganter klingt — aber NIEMALS den Inhalt deuten oder ergaenzen.
- Erhalte JEDE inhaltliche Aussage. Nichts darf wegfallen, nichts darf erfunden werden.
- Halte den persoenlichen Tonfall bei (Du-/Ich-Form, Emotionen, Schimpfwoerter wenn vorhanden).
- Falls der Eintrag mehrere Absaetze hat, darfst du die Absatzstruktur klarer machen.
- Du gibst NUR den verbesserten Text zurueck — KEINE Anfuehrungszeichen, keine Einleitung, keine Erklaerungen.

Beispiel (Original → Verbessert):
Original: "war heut beim arzt halt wegen mein rücken und der hat gesagt naja muss man halt mal mri machen aber ist halt nichts dringendes nur wenns weiterhin wehtut"
Verbessert: "Heute war ich beim Arzt wegen meines Ruecken. Er meinte, ein MRT waere sinnvoll, aber nicht dringend — nur falls die Schmerzen anhalten."
"""
    }
}
