package de.frank.entropyreducer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity
import de.frank.entropyreducer.data.local.taskSuggestionDaoFrom
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
// und der SyncEntriesUseCase rufen sie auf).
//
// ID-Architektur Etappe 2c/2e (2026-06-19): Die AUFGABEN-Vorschlaege liegen jetzt in Room (Tabelle
// task_suggestions) statt im DataStore-JSON. taskSuggestionsForBackup/restoreTaskSuggestions lesen/
// schreiben deshalb Room -> Drive-Backup und Sync ziehen automatisch nach. Die GEWOHNHEITS-
// Vorschlaege bleiben vorerst DataStore-JSON (kommen in Etappe 3 nach Room). Der einmalige Migrator
// nutzt taskSuggestionsFromJson (eingefrorener Alt-Stand), NICHT taskSuggestionsForBackup.
private val SUGGESTIONS_KEY = stringPreferencesKey("suggestions_json")

/** Aktuelle Aufgabenvorschlaege als Backup-DTO-Liste — ab Etappe 2c/2e aus Room (task_suggestions). */
fun taskSuggestionsForBackup(context: Context): Flow<List<BackupTaskSuggestion>> =
    taskSuggestionDaoFrom(context).getAll().map { rows ->
        rows.map { BackupTaskSuggestion(id = it.id, title = it.title, description = it.description) }
    }

/**
 * Liest die Bestands-Aufgabenvorschlaege aus dem alten DataStore-JSON ("ki_task_suggestions"). NUR
 * fuer den einmaligen Room-Migrator (IdeaTaskRoomMigrator, Etappe 2b) — NICHT fuer Backup/Sync/UI.
 */
fun taskSuggestionsFromJson(context: Context): Flow<List<BackupTaskSuggestion>> =
    context.kiTaskSuggestionStore.data.map { prefs -> parseTaskSuggestions(prefs[SUGGESTIONS_KEY]) }

/** Aktuelle Gewohnheitsvorschlaege (gewohnheit_suggestions) als id+text-Liste. */
fun gewohnheitSuggestionsForBackup(context: Context): Flow<List<BackupMental>> =
    context.gewohnheitSuggestionStore.data.map { prefs ->
        parseGewohnheitSuggestions(prefs[SUGGESTIONS_KEY])
    }

/**
 * Liest die Bestands-Gewohnheitsvorschlaege aus dem alten DataStore-JSON ("gewohnheit_suggestions").
 * NUR fuer den einmaligen Room-Migrator (HabitRoomMigrator, Etappe 3b) — NICHT fuer Backup/Sync/UI.
 * Bleibt JSON-basiert, auch wenn gewohnheitSuggestionsForBackup in Etappe 3e auf Room umgestellt wird.
 */
fun gewohnheitSuggestionsFromJson(context: Context): Flow<List<BackupMental>> =
    context.gewohnheitSuggestionStore.data.map { prefs ->
        parseGewohnheitSuggestions(prefs[SUGGESTIONS_KEY])
    }

/**
 * Spielt Aufgabenvorschlaege aus dem Backup in Room ein. Existenz-Strategie wie bisher: nur fehlende
 * IDs werden ergaenzt, lokale gewinnen. Gibt die Zahl der ergaenzten Vorschlaege zurueck. Da nur
 * NEUE IDs geschrieben werden, ist das REPLACE-Upsert kollisionsfrei (kein Ueberschreiben lokaler).
 */
suspend fun restoreTaskSuggestions(context: Context, incoming: List<BackupTaskSuggestion>): Int {
    if (incoming.isEmpty()) return 0
    val dao = taskSuggestionDaoFrom(context)
    val existingIds = dao.getAllForBackup().mapTo(HashSet()) { it.id }
    val toAdd = incoming.filterNot { it.id in existingIds }
    if (toAdd.isEmpty()) return 0
    // Backup-DTO hat keinen Zeitstempel — Reihenfolge stabil halten (wie der Migrator). Bestandsdaten
    // ohne Herkunft (originId/originType/rootId = null) — korrekt fuer eingespielte Alt-Vorschlaege.
    val nowMs = System.currentTimeMillis()
    dao.upsertAll(
        toAdd.mapIndexed { index, s ->
            TaskSuggestionEntity(
                id = s.id,
                title = s.title,
                description = s.description,
                createdAt = nowMs + index,
            )
        }
    )
    return toAdd.size
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
