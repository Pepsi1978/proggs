package de.frank.entropyreducer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.entropyreducer.data.remote.drive.BackupMental
import de.frank.entropyreducer.data.remote.drive.BackupTaskSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Zentrale DataStore-Delegates fuer die Vorschlag-Speicherung.
 * Jede Datei darf NUR HIER definiert sein — sonst gibt es den
 * "multiple DataStores active for the same file" Crash.
 */
val Context.kiTaskSuggestionStore by preferencesDataStore(name = "ki_task_suggestions")
val Context.gewohnheitSuggestionStore by preferencesDataStore(name = "gewohnheit_suggestions")

// Frank-Wunsch 2026-06-19 (Schema v15): offene KI-Vorschlaege ins Drive-Backup. Die Read-/Restore-
// Helper hier sind die einzige Backup-Schnittstelle zu den Vorschlags-Stores (der SyncCoordinator
// und der SyncEntriesUseCase rufen sie auf). Das JSON-Format ist 1:1 das der jeweiligen ViewModels
// (KiTaskSuggestViewModel / GewohnheitSuggestViewModel) — gleicher Key, gleiche Feldnamen, damit
// gesicherte Vorschlaege ohne Konvertierung wieder vom ViewModel gelesen werden.
private val SUGGESTIONS_KEY = stringPreferencesKey("suggestions_json")

/** Aktuelle Aufgabenvorschlaege (ki_task_suggestions) als Backup-DTO-Liste. */
fun taskSuggestionsForBackup(context: Context): Flow<List<BackupTaskSuggestion>> =
    context.kiTaskSuggestionStore.data.map { prefs -> parseTaskSuggestions(prefs[SUGGESTIONS_KEY]) }

/** Aktuelle Gewohnheitsvorschlaege (gewohnheit_suggestions) als id+text-Liste. */
fun gewohnheitSuggestionsForBackup(context: Context): Flow<List<BackupMental>> =
    context.gewohnheitSuggestionStore.data.map { prefs ->
        parseGewohnheitSuggestions(prefs[SUGGESTIONS_KEY])
    }

/**
 * Spielt Aufgabenvorschlaege aus dem Backup ein. Existenz-Strategie wie bei Mental/Ideen: nur
 * fehlende IDs werden ergaenzt, lokale gewinnen. Gibt die Zahl der ergaenzten Vorschlaege zurueck.
 */
suspend fun restoreTaskSuggestions(context: Context, incoming: List<BackupTaskSuggestion>): Int {
    if (incoming.isEmpty()) return 0
    var added = 0
    context.kiTaskSuggestionStore.edit { prefs ->
        val existing = parseTaskSuggestions(prefs[SUGGESTIONS_KEY])
        val existingIds = existing.mapTo(HashSet()) { it.id }
        val toAdd = incoming.filterNot { it.id in existingIds }
        added = toAdd.size
        if (toAdd.isNotEmpty()) prefs[SUGGESTIONS_KEY] = serializeTaskSuggestions(existing + toAdd)
    }
    return added
}

/** Spielt Gewohnheitsvorschlaege aus dem Backup ein (Existenz-Strategie). */
suspend fun restoreGewohnheitSuggestions(context: Context, incoming: List<BackupMental>): Int {
    if (incoming.isEmpty()) return 0
    var added = 0
    context.gewohnheitSuggestionStore.edit { prefs ->
        val existing = parseGewohnheitSuggestions(prefs[SUGGESTIONS_KEY])
        val existingIds = existing.mapTo(HashSet()) { it.id }
        val toAdd = incoming.filterNot { it.id in existingIds }
        added = toAdd.size
        if (toAdd.isNotEmpty()) prefs[SUGGESTIONS_KEY] = serializeGewohnheitSuggestions(existing + toAdd)
    }
    return added
}

private fun parseTaskSuggestions(raw: String?): List<BackupTaskSuggestion> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val title = o.optString("title").takeIf { it.isNotBlank() } ?: continue
                    add(BackupTaskSuggestion(id = id, title = title, description = o.optString("description")))
                }
            }
        }
        .getOrDefault(emptyList())
}

private fun serializeTaskSuggestions(items: List<BackupTaskSuggestion>): String {
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

private fun parseGewohnheitSuggestions(raw: String?): List<BackupMental> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(BackupMental(id = id, text = o.optString("text")))
                }
            }
        }
        .getOrDefault(emptyList())
}

private fun serializeGewohnheitSuggestions(items: List<BackupMental>): String {
    val arr = JSONArray()
    for (item in items) {
        arr.put(JSONObject().put("id", item.id).put("text", item.text))
    }
    return arr.toString()
}
