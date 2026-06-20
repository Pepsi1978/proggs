package de.frank.entropyreducer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.entropyreducer.data.remote.drive.BackupTombstone
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loesch-Protokoll (Tombstones) — Sync-Etappe 1.2 (Frank-Wunsch 2026-06-19).
 *
 * Damit eine Loeschung auf Geraet A beim Restore auch auf Geraet B ausgefuehrt wird (statt dass der
 * additive Restore den Eintrag "auferstehen" laesst), haelt dieser zentrale DataStore fuer JEDE vom
 * Nutzer geloeschte Entitaet einen Tombstone: (Typ, ID, Loeschzeitpunkt). Er gilt typuebergreifend
 * (Aufgaben, Loop-Vorlagen, Gewohnheit, Mental, ...), weil die Quell-Daten teils in Room, teils in
 * DataStore liegen — ein zentrales Protokoll ist unabhaengig davon, wo die Entitaet selbst lebt.
 *
 * Konflikt-Regel (delete-wins-only-if-newer): Beim Restore wird eine lokale Entitaet nur geloescht,
 * wenn der Tombstone-Zeitstempel NEUER ist als ihr lokaler updatedAt-Stand. So gewinnt eine spaetere
 * Bearbeitung gegen eine fruehere Loeschung. KEIN Sicherheitsnetz / keine Max-Delete-Schwelle
 * (Frank-Entscheidung 2026-06-19): jede echte Loeschung wird sofort und vollstaendig propagiert.
 *
 * WICHTIG: Nur dieser DataStore-Delegate darf die Datei "deletion_tombstones" definieren (sonst
 * "multiple DataStores active for the same file"-Crash).
 */
val Context.deletionTombstoneStore by preferencesDataStore(name = "deletion_tombstones")

private val TOMBSTONES_KEY = stringPreferencesKey("tombstones_json")

/**
 * Tombstones, deren Loeschung laenger als diese Spanne her ist, werden beim Merge entfernt — nach so
 * langer Zeit haben alle Geraete die Loeschung laengst uebernommen. Reine Speicher-Hygiene, KEIN
 * Loesch-Schutz (das von Frank abgelehnte Sicherheitsnetz betraf das Blockieren echter Loeschungen).
 */
private const val TOMBSTONE_RETENTION_MS = 180L * 24L * 60L * 60L * 1000L

/** Zentrale Entitaetstyp-Konstanten — Loesch-Stelle und Restore MUESSEN denselben String nutzen. */
object TombstoneType {
    const val AUFGABE = "aufgabe"
    const val LOOP_TEMPLATE = "loop_template"
    const val GEWOHNHEIT = "gewohnheit"
    const val MENTAL = "mental"
    const val IDEE = "idee"
    const val TAGEBUCH = "tagebuch"
    const val THESE = "these"
    const val FOLLOWUP = "followup"
    const val PRIORITY_MEMORY = "priority_memory"

    // Frank-Wunsch 2026-06-20: Auch das Loeschen eines VORSCHLAGS (Annehmen/Verwerfen) muss
    // propagieren — sonst behaelt ein 2. Geraet seine Kopie und laedt sie wieder hoch, und der
    // angenommene/verworfene Vorschlag taucht beim Restore erneut auf.
    const val TASK_SUGGESTION = "task_suggestion"
    const val HABIT_SUGGESTION = "habit_suggestion"
}

/** Markiert eine Entitaet als geloescht. Behaelt pro (type,id) den NEUESTEN Loeschzeitpunkt. */
suspend fun markDeleted(
    context: Context,
    type: String,
    id: String,
    deletedAt: Long = System.currentTimeMillis(),
) = markManyDeleted(context, type, listOf(id), deletedAt)

/** Wie [markDeleted], aber fuer mehrere IDs desselben Typs in EINER DataStore-Transaktion (deleteAll). */
suspend fun markManyDeleted(
    context: Context,
    type: String,
    ids: List<String>,
    deletedAt: Long = System.currentTimeMillis(),
) {
    if (ids.isEmpty()) return
    context.deletionTombstoneStore.edit { prefs ->
        val map =
            parseTombstones(prefs[TOMBSTONES_KEY]).associateByTo(mutableMapOf()) { it.type to it.id }
        for (id in ids) {
            val key = type to id
            val prev = map[key]
            if (prev == null || deletedAt > prev.deletedAt) {
                map[key] = BackupTombstone(type = type, id = id, deletedAt = deletedAt)
            }
        }
        prefs[TOMBSTONES_KEY] = serializeTombstones(map.values.toList())
    }
}

/** Alle aktuellen Tombstones fuer das Drive-Backup (Upload). */
suspend fun tombstonesForBackup(context: Context): List<BackupTombstone> =
    parseTombstones(context.deletionTombstoneStore.data.first()[TOMBSTONES_KEY])

/** Vereinigt zwei Tombstone-Listen — pro (type,id) gewinnt der NEUESTE deletedAt. */
fun unionTombstones(a: List<BackupTombstone>, b: List<BackupTombstone>): List<BackupTombstone> {
    val map = a.associateByTo(mutableMapOf()) { it.type to it.id }
    for (t in b) {
        val key = t.type to t.id
        val prev = map[key]
        if (prev == null || t.deletedAt > prev.deletedAt) map[key] = t
    }
    return map.values.toList()
}

/**
 * Merge Remote-Tombstones in den lokalen Store (neuester deletedAt pro (type,id) gewinnt) und
 * entfernt dabei abgelaufene Tombstones (> [TOMBSTONE_RETENTION_MS]). Gibt die gemergte Gesamtliste
 * zurueck, damit der Restore sie direkt anwenden kann.
 */
suspend fun mergeRemoteTombstones(
    context: Context,
    remote: List<BackupTombstone>,
): List<BackupTombstone> {
    val now = System.currentTimeMillis()
    var merged: List<BackupTombstone> = emptyList()
    context.deletionTombstoneStore.edit { prefs ->
        val combined = unionTombstones(parseTombstones(prefs[TOMBSTONES_KEY]), remote)
        // Pruning: laengst von allen Geraeten uebernommene Loeschungen entfernen.
        val pruned = combined.filter { now - it.deletedAt <= TOMBSTONE_RETENTION_MS }
        merged = pruned
        prefs[TOMBSTONES_KEY] = serializeTombstones(pruned)
    }
    return merged
}

private fun parseTombstones(raw: String?): List<BackupTombstone> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val type = o.optString("type").takeIf { it.isNotBlank() } ?: continue
                    val id = o.optString("id").takeIf { it.isNotBlank() } ?: continue
                    add(BackupTombstone(type = type, id = id, deletedAt = o.optLong("deletedAt")))
                }
            }
        }
        .getOrDefault(emptyList())
}

private fun serializeTombstones(items: List<BackupTombstone>): String {
    val arr = JSONArray()
    for (t in items) {
        arr.put(JSONObject().put("type", t.type).put("id", t.id).put("deletedAt", t.deletedAt))
    }
    return arr.toString()
}
