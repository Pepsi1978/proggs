package de.frank.entropyreducer.presentation.mental

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.remote.GeminiApi
import de.frank.entropyreducer.data.remote.GeminiContent
import de.frank.entropyreducer.data.remote.GeminiGenerationConfig
import de.frank.entropyreducer.data.remote.GeminiPart
import de.frank.entropyreducer.data.remote.GeminiRequest
import de.frank.entropyreducer.data.settings.AppSettings
import de.frank.entropyreducer.data.settings.EncryptedSecretsStore
import de.frank.entropyreducer.presentation.ideen.ideenEntriesFlow
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private fun parseSuggestionsJson(raw: String?): List<Mental> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                add(Mental(id = id, text = o.optString("text")))
            }
        }
    }.getOrDefault(emptyList())
}

private fun serializeSuggestionsJson(mentals: List<Mental>): String {
    val arr = JSONArray()
    for (m in mentals) {
        arr.put(JSONObject().put("id", m.id).put("text", m.text))
    }
    return arr.toString()
}

private val Context.suggestionStore by preferencesDataStore(name = "gewohnheit_suggestions")
private val KEY_SUGGESTIONS = stringPreferencesKey("suggestions_json")

enum class SuggestState { IDLE, LOADING }

@HiltViewModel
class GewohnheitSuggestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gemini: GeminiApi,
    private val secrets: EncryptedSecretsStore,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(SuggestState.IDLE)
    val state: StateFlow<SuggestState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val suggestions: StateFlow<List<Mental>> =
        context.suggestionStore.data.map { prefs -> parseSuggestionsJson(prefs[KEY_SUGGESTIONS]) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissError() { _error.value = null }

    fun generateSuggestions() {
        if (_state.value == SuggestState.LOADING) return
        viewModelScope.launch {
            val apiKey = secrets.geminiApiKey
            if (apiKey.isNullOrBlank()) {
                _error.value = "Bitte Gemini-API-Key in den Einstellungen hinterlegen."
                return@launch
            }
            _state.value = SuggestState.LOADING
            _error.value = null
            runCatching {
                val ideas = ideenEntriesFlow(context).first()
                if (ideas.isEmpty()) {
                    _error.value = "Keine Ideen vorhanden — zuerst Ideen eingeben."
                    _state.value = SuggestState.IDLE
                    return@launch
                }
                val ideenText = ideas.joinToString("\n") { "- ${it.text}" }
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
                                parts = listOf(GeminiPart("Hier sind meine Ideen:\n\n$ideenText")),
                            ),
                        ),
                        generationConfig = GeminiGenerationConfig(
                            temperature = 0.6,
                            responseMimeType = "application/json",
                        ),
                    ),
                )
                val json = response.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text
                    ?.trim()
                    ?: throw IllegalStateException("Leere Antwort von Gemini")
                storeSuggestions(json)
            }.onFailure { ex ->
                _error.value = ex.message ?: "Vorschlag-Generierung fehlgeschlagen"
            }
            _state.value = SuggestState.IDLE
        }
    }

    fun acceptSuggestion(id: String) {
        viewModelScope.launch {
            context.suggestionStore.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing.filterNot { it.id == id })
            }
        }
    }

    fun deleteSuggestion(id: String) {
        viewModelScope.launch {
            context.suggestionStore.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing.filterNot { it.id == id })
            }
        }
    }

    private suspend fun storeSuggestions(json: String) {
        val arr = runCatching { JSONArray(json) }.getOrNull()
            ?: runCatching { JSONArray("[$json]") }.getOrNull()
            ?: return
        val newSuggestions = buildList {
            for (i in 0 until arr.length()) {
                val text = arr.optString(i).trim()
                if (text.isNotBlank()) add(Mental.create(text))
            }
        }
        context.suggestionStore.edit { prefs ->
            prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(newSuggestions)
        }
    }

    companion object {
        private const val SYSTEM_PROMPT = """
Du bist ein Coach fuer Gewohnheitsbildung. Analysiere die folgenden Ideen und
identifiziere jene, die sich eignen, daraus eine taegliche oder regelmaessige
Gewohnheit zu machen.

Fuer jede geeignete Idee formuliere EINEN Gewohnheitsvorschlag als kurzen,
praezisen Satz. Format: "Was ich wann am besten mache" — maximal 3 Zeilen.
Beispiel: "Jeden Morgen nach dem Aufstehen 5 Minuten meditieren, bevor ich
das Handy checke."

Regeln:
- Gib NUR jene Ideen zurueck, die wirklich als Gewohnheit umsetzbar sind.
- Maximal 5 Vorschlaege.
- Jeder Vorschlag ist ein einzelner String, maximal 3 Zeilen lang.
- Antworte NUR mit einem JSON-Array von Strings, z.B.:
  ["Vorschlag 1", "Vorschlag 2", "Vorschlag 3"]
- Keine Einleitung, keine Erklaerung, nur das JSON-Array.
"""
    }
}
