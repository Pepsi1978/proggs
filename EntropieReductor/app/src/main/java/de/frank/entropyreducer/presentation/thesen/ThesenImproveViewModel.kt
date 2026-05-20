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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Text-Verbesserung fuer Theseneintraege via Gemini.
 *
 * Frank-Wunsch 2026-05-18: Nach der Sprach-Aufnahme via Groq Whisper soll
 * der Roh-Text optional von Gemini stilistisch und grammatikalisch
 * verbessert werden — Inhalt bleibt erhalten, Tonfall bleibt persoenlich.
 * Funktioniert analog zur Text-Verbesserung in BestJournalFrank.
 */
enum class ImproveState { IDLE, RUNNING }

@HiltViewModel
class ThesenImproveViewModel @Inject constructor(
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
                                parts = listOf(GeminiPart("Hier ist mein Theseneintrag (Roh-Transkript):\n\n$text")),
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
Du bist ein Lektor fuer persoenliche Theseneintraege. Du verbesserst den Text
behutsam und behaeltst die persoenliche Stimme komplett bei.

Pflicht-Regeln:
- Sprache: Deutsch mit echten Umlauten (ae->ä, oe->ö, ue->ü, ss->ß wo orthographisch korrekt).
- Verbessere Rechtschreibung, Grammatik, Zeichensetzung und stilistische Holprigkeiten.
- Aenderungen NUR an Form, NIE am Inhalt. Du fuegst NICHTS hinzu, kuerzt NICHTS, deutest NICHTS.
- Du gibst NUR den verbesserten Text zurueck — KEINE Anfuehrungszeichen, keine Einleitung, keine Erklaerungen.
- Falls der Eintrag mehrere Absaetze hat, behalte die Absatzstruktur.
- Halte den Tonfall persoenlich (Du-/Ich-Form wird beibehalten).
"""
    }
}
