package de.frank.entropyreducer.presentation.dashboard1

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.data.kiTaskSuggestionStore
import de.frank.entropyreducer.domain.model.EntrySource
import de.frank.entropyreducer.domain.usecase.GenerateSuggestionsUseCase
import de.frank.entropyreducer.domain.usecase.ProcessEntryUseCase
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

data class KiTaskSuggestion(
    val id: String,
    val title: String,
    val description: String,
) {
    companion object {
        fun create(title: String, description: String): KiTaskSuggestion =
            KiTaskSuggestion(
                id = java.util.UUID.randomUUID().toString(),
                title = title.take(60),
                description = description.take(500),
            )
    }
}

enum class KiTaskSuggestState { IDLE, LOADING }

private fun parseSuggestionsJson(raw: String?): List<KiTaskSuggestion> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue
                val description = o.optString("description")
                add(KiTaskSuggestion(id = id, title = title, description = description))
            }
        }
    }.getOrDefault(emptyList())
}

private fun serializeSuggestionsJson(items: List<KiTaskSuggestion>): String {
    val arr = JSONArray()
    for (item in items) {
        arr.put(
            JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("description", item.description),
        )
    }
    return arr.toString()
}

private val KEY_SUGGESTIONS = stringPreferencesKey("suggestions_json")
private val KEY_PROCESSED_IDEAS = stringPreferencesKey("processed_idea_ids")

@HiltViewModel
class KiTaskSuggestViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val generateSuggestions: GenerateSuggestionsUseCase,
    private val process: ProcessEntryUseCase,
) : ViewModel() {

    private val store = context.kiTaskSuggestionStore

    private val _state = MutableStateFlow(KiTaskSuggestState.IDLE)
    val state: StateFlow<KiTaskSuggestState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _acceptingId = MutableStateFlow<String?>(null)
    val acceptingId: StateFlow<String?> = _acceptingId.asStateFlow()

    val suggestions: StateFlow<List<KiTaskSuggestion>> =
        store.data
            .map { prefs -> parseSuggestionsJson(prefs[KEY_SUGGESTIONS]) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissError() { _error.value = null }

    fun generateSuggestions() {
        if (_state.value == KiTaskSuggestState.LOADING) return
        viewModelScope.launch {
            _state.value = KiTaskSuggestState.LOADING
            _error.value = null
            runCatching {
                val ideas = ideenEntriesFlow(context).first()
                val processedIds = loadProcessedIdeaIds()
                val (result, updatedProcessedIds) = generateSuggestions
                    .generateSuggestions(ideas, processedIds)
                    .getOrThrow()
                if (result.tasks.isNotEmpty()) {
                    storeSuggestions(result.tasks.map { KiTaskSuggestion(id = it.id, title = it.title, description = it.description) })
                }
                saveProcessedIdeaIds(updatedProcessedIds)
                if (result.tasks.isEmpty() && ideas.isNotEmpty()) {
                    _error.value = "Alle Ideen wurden bereits als Vorschlag verarbeitet."
                }
            }.onFailure { ex ->
                _error.value = ex.message ?: "Vorschlag-Generierung fehlgeschlagen"
            }
            _state.value = KiTaskSuggestState.IDLE
        }
    }

    fun acceptSuggestion(suggestion: KiTaskSuggestion) {
        viewModelScope.launch {
            _acceptingId.value = suggestion.id
            val text = "${suggestion.title}. ${suggestion.description}"
            process(text, EntrySource.NUTZER_TEXT)
                .onSuccess {
                    removeSuggestion(suggestion.id)
                }
                .onFailure { ex ->
                    _error.value = ex.message ?: "Aufgabe konnte nicht uebernommen werden"
                }
            _acceptingId.value = null
        }
    }

    fun deleteSuggestion(id: String) {
        removeSuggestion(id)
    }

    private fun removeSuggestion(id: String) {
        viewModelScope.launch {
            store.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing.filterNot { it.id == id })
            }
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Aufgabenvorschlag: entfernt")
        }
    }

    private fun storeSuggestions(newSuggestions: List<KiTaskSuggestion>) {
        viewModelScope.launch {
            store.edit { prefs ->
                val existing = parseSuggestionsJson(prefs[KEY_SUGGESTIONS])
                prefs[KEY_SUGGESTIONS] = serializeSuggestionsJson(existing + newSuggestions)
            }
            de.frank.entropyreducer.data.remote.drive.triggerDriveBackup(
                context, "Aufgabenvorschlag: generiert")
        }
    }

    private suspend fun loadProcessedIdeaIds(): Set<String> {
        return store.data.first().let { prefs ->
            val raw = prefs[KEY_PROCESSED_IDEAS] ?: return@let emptySet()
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

    private suspend fun saveProcessedIdeaIds(ids: Set<String>) {
        store.edit { prefs ->
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            prefs[KEY_PROCESSED_IDEAS] = arr.toString()
        }
    }

    fun resetProcessedIdeas() {
        viewModelScope.launch {
            store.edit { prefs ->
                prefs[KEY_PROCESSED_IDEAS] = "[]"
            }
            _error.value = null
        }
    }
}
