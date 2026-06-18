package de.frank.entropyreducer.presentation.mental

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.gewohnheitSuggestionStore
import de.frank.entropyreducer.domain.usecase.GenerateSuggestionsUseCase
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

private val KEY_SUGGESTIONS = stringPreferencesKey("suggestions_json")

enum class SuggestState { IDLE, LOADING }

@HiltViewModel
class GewohnheitSuggestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generateSuggestions: GenerateSuggestionsUseCase,
) : ViewModel() {

    private val store = context.gewohnheitSuggestionStore

    private val _state = MutableStateFlow(SuggestState.IDLE)
    val state: StateFlow<SuggestState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val suggestions: StateFlow<List<Mental>> =
        store.data.map { prefs -> parseSuggestionsJson(prefs[KEY_SUGGESTIONS]) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissError() { _error.value = null }

    fun generateSuggestions() {
        if (_state.value == SuggestState.LOADING) return
        viewModelScope.launch {
            _state.value = SuggestState.LOADING
            _error.value = null
            runCatching {
                val ideas = ideenEntriesFlow(context).first()
                val newSuggestions = generateSuggestions
                    .generateHabitSuggestions(ideas)
                    .getOrThrow()
                storeSuggestions(newSuggestions)
                if (newSuggestions.isEmpty() && ideas.isNotEmpty()) {
                    _error.value = "Keine Ideen geeignet für Gewohnheitsvorschläge."
                } else if (newSuggestions.isEmpty()) {
                    _error.value = "Keine Ideen vorhanden — zuerst Ideen eingeben."
                }
            }.onFailure { ex ->
                _error.value = ex.message ?: "Vorschlag-Generierung fehlgeschlagen"
            }
            _state.value = SuggestState.IDLE
        }
    }

    fun acceptSuggestion(id: String) {
        viewModelScope.launch {
            store.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing.filterNot { it.id == id })
            }
        }
    }

    fun deleteSuggestion(id: String) {
        viewModelScope.launch {
            store.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing.filterNot { it.id == id })
            }
        }
    }

    fun addSuggestion(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            store.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing + Mental.create(clean))
            }
        }
    }

    private fun storeSuggestions(newSuggestions: List<Mental>) {
        viewModelScope.launch {
            store.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing + newSuggestions)
            }
        }
    }

    fun resetProcessedIdeas() {
        viewModelScope.launch {
            _error.value = null
        }
    }
}
