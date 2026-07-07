package de.frank.entropyreducer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.entropyreducer.data.local.entities.HabitSuggestionEntity
import de.frank.entropyreducer.data.local.entities.TaskSuggestionEntity
import de.frank.entropyreducer.data.local.dao.EntropyEntryDao
import de.frank.entropyreducer.data.local.dao.HabitDao
import de.frank.entropyreducer.data.local.entropyEntryDaoFrom
import de.frank.entropyreducer.data.local.habitDaoFrom
import de.frank.entropyreducer.data.local.habitSuggestionDaoFrom
import de.frank.entropyreducer.data.local.taskSuggestionDaoFrom
import de.frank.entropyreducer.data.remote.drive.BackupMental
import de.frank.entropyreducer.data.remote.drive.BackupTaskSuggestion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

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

// Live-Sonde (Frank-Wunsch 2026-06-20): jede Heal-Entfernung eines Vorschlags landet mit Grund in
// logcat -> live mitlesbar via `adb logcat -s EREVorschlagHeal` (Diag schreibt bei aktivem Logger
// NICHT in logcat, darum hier bewusst android.util.Log direkt). Macht den Verwaist-/Heal-Fall in
// Sekunden sichtbar statt per DB-Pull.
private const val SUGGESTION_HEAL_TAG = "EREVorschlagHeal"

/** Aktuelle Aufgabenvorschlaege als Backup-DTO-Liste — ab Etappe 2c/2e aus Room (task_suggestions). */
fun taskSuggestionsForBackup(context: Context): Flow<List<BackupTaskSuggestion>> =
    taskSuggestionDaoFrom(context).getAll().map { rows ->
        // Schema v18 (2026-06-20): Herkunft (originId/originType/rootId) mitsichern, damit der
        // Ketten-Dedup (countByOriginId) auch nach Drive-Sync/Restore auf einem 2. Geraet greift.
        rows.map {
            BackupTaskSuggestion(
                id = it.id,
                title = it.title,
                description = it.description,
                originId = it.originId,
                originType = it.originType,
                rootId = it.rootId,
            )
        }
    }

/**
 * Liest die Bestands-Aufgabenvorschlaege aus dem alten DataStore-JSON ("ki_task_suggestions"). NUR
 * fuer den einmaligen Room-Migrator (IdeaTaskRoomMigrator, Etappe 2b) — NICHT fuer Backup/Sync/UI.
 */
fun taskSuggestionsFromJson(context: Context): Flow<List<BackupTaskSuggestion>> =
    context.kiTaskSuggestionStore.data.map { prefs -> parseTaskSuggestions(prefs[SUGGESTIONS_KEY]) }

/** Aktuelle Gewohnheitsvorschlaege als Backup-DTO-Liste — ab Etappe 3c/3e aus Room (habit_suggestions). */
fun gewohnheitSuggestionsForBackup(context: Context): Flow<List<BackupMental>> =
    habitSuggestionDaoFrom(context).getAll().map { rows ->
        // Schema v18 (2026-06-20): Herkunft mitsichern (analog Aufgaben-Vorschlaege) — sonst waere
        // der Ketten-Dedup fuer Gewohnheits-Vorschlaege nach Drive-Sync/Restore blind.
        rows.map {
            BackupMental(
                id = it.id,
                text = it.text,
                originId = it.originId,
                originType = it.originType,
                rootId = it.rootId,
            )
        }
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
suspend fun restoreTaskSuggestions(
    context: Context,
    incoming: List<BackupTaskSuggestion>,
    // Frank-Wunsch 2026-06-20: Vorschlags-Tombstones (angenommene/verworfene Vorschlaege). Eine ID
    // hier -> der Vorschlag ist geloescht und darf NICHT wieder eingespielt werden.
    deletedAt: Map<String, Long> = emptyMap(),
    // Bugfix 2026-06-20: IDs per Tombstone GELOESCHTER Ideen. Ein Vorschlag, dessen Quell-Idee
    // (rootId) geloescht wurde, ist VERWAIST -> genau wie ein angenommener Vorschlag verbraucht.
    ideaDeletedAt: Set<String> = emptySet(),
): Int {
    val dao = taskSuggestionDaoFrom(context)
    val entryDao = entropyEntryDaoFrom(context)
    val habitDao = habitDaoFrom(context)
    // Heal (Frank-Wunsch 2026-06-20, Direktive #3): bestehende LOKALE Vorschlaege loeschen, die per
    // Tombstone als geloescht markiert sind, deren Idee schon eine angenommene Aufgabe/Gewohnheit
    // hat (Kette weiter -> verbraucht) ODER deren Quell-Idee geloescht wurde (verwaist). Raeumt
    // Alt-Vorschlaege weg, die ueber ein Remote-Backup zurueckkamen (laeuft auch bei leerem incoming).
    val keptExisting = HashSet<String>()
    for (ex in dao.getAllForBackup()) {
        val reason = when {
            ex.id in deletedAt -> "Tombstone (geloescht/angenommen)"
            isIdeaAlreadyAccepted(entryDao, habitDao, ex.rootId) -> "Idee angenommen (Kette weiter)"
            isRootIdeaDeleted(ex.rootId, ideaDeletedAt) -> "Idee geloescht (verwaist)"
            else -> null
        }
        if (reason != null) {
            dao.deleteById(ex.id)
            android.util.Log.i(
                SUGGESTION_HEAL_TAG,
                "Aufgaben-Vorschlag ${ex.id} entfernt: $reason (rootId=${ex.rootId})",
            )
        } else {
            keptExisting.add(ex.id)
        }
    }
    if (incoming.isEmpty()) return 0
    // Additiv: nur fehlende IDs, NICHT getombstonete, NICHT schon-angenommene, NICHT verwaiste.
    val toAdd = incoming.filterNot {
        it.id in keptExisting ||
            it.id in deletedAt ||
            isIdeaAlreadyAccepted(entryDao, habitDao, it.rootId) ||
            isRootIdeaDeleted(it.rootId, ideaDeletedAt)
    }
    if (toAdd.isEmpty()) return 0
    // Backup-DTO hat keinen Zeitstempel — Reihenfolge stabil halten (wie der Migrator). Schema v18
    // (2026-06-20): Herkunft aus dem Backup wiederherstellen. Aeltere Backups (v15-v17) tragen sie
    // nicht -> dort bleibt originId=null (Alt-Vorschlaege ohne Herkunft, wie bisher).
    val nowMs = System.currentTimeMillis()
    dao.upsertAll(
        toAdd.mapIndexed { index, s ->
            TaskSuggestionEntity(
                id = s.id,
                title = s.title,
                description = s.description,
                createdAt = nowMs + index,
                originId = s.originId,
                originType = s.originType,
                rootId = s.rootId,
            )
        }
    )
    return toAdd.size
}

/**
 * Chain-Check (Frank-Wunsch 2026-06-20): Hat die Quell-Idee dieses Vorschlags (rootId) schon eine
 * angenommene Aufgabe (entropy_entries) ODER Gewohnheit (habits)? Dann ist die Kette weiter und der
 * Vorschlag verbraucht — er darf nicht (wieder) als Vorschlag existieren. rootId==null (Alt-Vorschlag
 * ohne Herkunft) -> nicht filterbar (false), wie bisher.
 */
private suspend fun isIdeaAlreadyAccepted(
    entryDao: EntropyEntryDao,
    habitDao: HabitDao,
    rootId: String?,
): Boolean {
    if (rootId.isNullOrBlank()) return false
    return entryDao.countByRootId(rootId) > 0 || habitDao.countByRootId(rootId) > 0
}

/**
 * Verwaist-Check (Bugfix 2026-06-20): Wurde die Quell-Idee dieses Vorschlags (rootId) per Tombstone
 * geloescht? Dann ist der Vorschlag verwaist und darf nicht (wieder) als Vorschlag existieren —
 * symmetrisch zu [isIdeaAlreadyAccepted]. Deckt den Fall ab, dass die abgeleitete Gewohnheit/Aufgabe
 * VOR der Idee geloescht wurde (dann ist countByRootId=0 und nur der Idee-Tombstone zeigt es noch an).
 * rootId==null (Alt-Vorschlag ohne Herkunft) -> nicht filterbar (false), wie bisher.
 */
private fun isRootIdeaDeleted(rootId: String?, ideaDeletedAt: Set<String>): Boolean =
    !rootId.isNullOrBlank() && rootId in ideaDeletedAt

/** Spielt Gewohnheitsvorschlaege aus dem Backup in Room ein (nur fehlende IDs, lokale gewinnen). */
suspend fun restoreGewohnheitSuggestions(
    context: Context,
    incoming: List<BackupMental>,
    deletedAt: Map<String, Long> = emptyMap(),
    // Bugfix 2026-06-20: IDs per Tombstone GELOESCHTER Ideen (siehe restoreTaskSuggestions).
    ideaDeletedAt: Set<String> = emptySet(),
): Int {
    val dao = habitSuggestionDaoFrom(context)
    val entryDao = entropyEntryDaoFrom(context)
    val habitDao = habitDaoFrom(context)
    // Heal (analog restoreTaskSuggestions): getombstonete + ketten-verbrauchte + verwaiste (Quell-Idee
    // geloescht) LOKALE Vorschlaege weg.
    val keptExisting = HashSet<String>()
    for (ex in dao.getAllForBackup()) {
        val reason = when {
            ex.id in deletedAt -> "Tombstone (geloescht/angenommen)"
            isIdeaAlreadyAccepted(entryDao, habitDao, ex.rootId) -> "Idee angenommen (Kette weiter)"
            isRootIdeaDeleted(ex.rootId, ideaDeletedAt) -> "Idee geloescht (verwaist)"
            else -> null
        }
        if (reason != null) {
            dao.deleteById(ex.id)
            android.util.Log.i(
                SUGGESTION_HEAL_TAG,
                "Gewohnheits-Vorschlag ${ex.id} entfernt: $reason (rootId=${ex.rootId})",
            )
        } else {
            keptExisting.add(ex.id)
        }
    }
    if (incoming.isEmpty()) return 0
    val toAdd = incoming.filterNot {
        it.id in keptExisting ||
            it.id in deletedAt ||
            isIdeaAlreadyAccepted(entryDao, habitDao, it.rootId) ||
            isRootIdeaDeleted(it.rootId, ideaDeletedAt)
    }
    if (toAdd.isEmpty()) return 0
    // Reihenfolge stabil halten. Schema v18 (2026-06-20): Herkunft aus dem Backup wiederherstellen
    // (BackupMental traegt sie jetzt). Aeltere Backups (v12-v17) ohne Herkunft -> origin bleibt null.
    val nowMs = System.currentTimeMillis()
    dao.upsertAll(
        toAdd.mapIndexed { index, s ->
            HabitSuggestionEntity(
                id = s.id,
                text = s.text,
                createdAt = nowMs + index,
                originId = s.originId,
                originType = s.originType,
                rootId = s.rootId,
            )
        }
    )
    return toAdd.size
}

/**
 * Sofort-Heal (Frank-Wunsch 2026-06-20): raeumt verwaiste/verbrauchte Vorschlaege OHNE Drive-Restore
 * weg — beim Oeffnen des Gewohnheits-/Aufgaben-Reiters aufgerufen. Damit verschwindet ein verwaister
 * Vorschlag sofort, auch wenn gerade kein Sync laeuft (der Restore-Heal greift sonst erst beim
 * naechsten ForegroundSync). Nutzt dieselben Kriterien wie der Restore-Heal: eigener Tombstone,
 * Idee angenommen (countByRootId), Idee geloescht (verwaist). Quelle der Loeschungen: lokaler
 * TombstoneStore.
 */
suspend fun healOrphanedSuggestions(context: Context) {
    val tombstones = tombstonesForBackup(context)
    val ideaDeleted = tombstones.filter { it.type == TombstoneType.IDEE }.map { it.id }.toSet()
    val taskSugDeleted =
        tombstones.filter { it.type == TombstoneType.TASK_SUGGESTION }.map { it.id }.toSet()
    val habitSugDeleted =
        tombstones.filter { it.type == TombstoneType.HABIT_SUGGESTION }.map { it.id }.toSet()
    val entryDao = entropyEntryDaoFrom(context)
    val habitDao = habitDaoFrom(context)

    val taskDao = taskSuggestionDaoFrom(context)
    for (ex in taskDao.getAllForBackup()) {
        val reason = when {
            ex.id in taskSugDeleted -> "Tombstone (geloescht/angenommen)"
            isIdeaAlreadyAccepted(entryDao, habitDao, ex.rootId) -> "Idee angenommen (Kette weiter)"
            isRootIdeaDeleted(ex.rootId, ideaDeleted) -> "Idee geloescht (verwaist)"
            else -> null
        }
        if (reason != null) {
            taskDao.deleteById(ex.id)
            android.util.Log.i(
                SUGGESTION_HEAL_TAG,
                "Aufgaben-Vorschlag ${ex.id} entfernt (Reiter-Heal): $reason (rootId=${ex.rootId})",
            )
        }
    }

    val habitSugDao = habitSuggestionDaoFrom(context)
    for (ex in habitSugDao.getAllForBackup()) {
        val reason = when {
            ex.id in habitSugDeleted -> "Tombstone (geloescht/angenommen)"
            isIdeaAlreadyAccepted(entryDao, habitDao, ex.rootId) -> "Idee angenommen (Kette weiter)"
            isRootIdeaDeleted(ex.rootId, ideaDeleted) -> "Idee geloescht (verwaist)"
            else -> null
        }
        if (reason != null) {
            habitSugDao.deleteById(ex.id)
            android.util.Log.i(
                SUGGESTION_HEAL_TAG,
                "Gewohnheits-Vorschlag ${ex.id} entfernt (Reiter-Heal): $reason (rootId=${ex.rootId})",
            )
        }
    }
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

// serializeGewohnheitSuggestions entfaellt (Etappe 3c/3e): Gewohnheits-Vorschlaege werden nicht mehr
// als JSON geschrieben — restoreGewohnheitSuggestions schreibt jetzt Room.
