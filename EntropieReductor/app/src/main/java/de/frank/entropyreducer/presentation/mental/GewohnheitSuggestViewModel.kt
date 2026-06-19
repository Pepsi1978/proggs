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
private val HABIT_PROCESSED_KEY = stringPreferencesKey("habit_processed_idea_ids")

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
                val processedIds = loadProcessedIds()
                val (result, updatedProcessedIds) = generateSuggestions
                    .generateSuggestions(ideas, processedIds)
                    .getOrThrow()
                storeSuggestions(result.habits)
                saveProcessedIds(updatedProcessedIds)
                if (result.habits.isEmpty() && ideas.isNotEmpty()) {
                    _error.value = "Alle Ideen wurden bereits als Vorschläge verarbeitet."
                } else if (result.habits.isEmpty()) {
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
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Gewohnheitsvorschlag: angenommen")
        }
    }

    fun deleteSuggestion(id: String) {
        viewModelScope.launch {
            store.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing.filterNot { it.id == id })
            }
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Gewohnheitsvorschlag: verworfen")
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
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Gewohnheitsvorschlag: hinzugefuegt")
        }
    }

    private fun storeSuggestions(newSuggestions: List<Mental>) {
        viewModelScope.launch {
            store.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing + newSuggestions)
            }
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Gewohnheitsvorschlag: generiert")
        }
    }

    fun resetProcessedIdeas() {
        viewModelScope.launch {
            store.edit { prefs ->
                prefs[HABIT_PROCESSED_KEY] = "[]"
            }
            _error.value = null
        }
    }

    private suspend fun loadProcessedIds(): Set<String> {
        return store.data.first().let { prefs ->
            val raw = prefs[HABIT_PROCESSED_KEY] ?: return@let emptySet()
            runCatching {
                val arr = JSONArray(raw)
                buildSet(arr.length()) {
                    for (i in 0 until arr.length()) {
                        arr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
            }.getOrDefault(emptySet())
        }
    }

    private suspend fun saveProcessedIds(ids: Set<String>) {
        store.edit { prefs ->
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            prefs[HABIT_PROCESSED_KEY] = arr.toString()
        }
    }
}
