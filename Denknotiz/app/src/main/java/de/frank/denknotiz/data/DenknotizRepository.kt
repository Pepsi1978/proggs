package de.frank.denknotiz.data

import androidx.room.withTransaction
import de.frank.denknotiz.data.local.ContextBoundaryEntity
import de.frank.denknotiz.data.local.DenknotizDatabase
import de.frank.denknotiz.data.local.EntryEntity
import de.frank.denknotiz.data.local.EntryType
import de.frank.denknotiz.data.local.EvaluationSnapshotEntity
import de.frank.denknotiz.data.local.SessionBundle
import de.frank.denknotiz.data.local.SessionEntity
import de.frank.denknotiz.data.local.SnapshotStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject

class DenknotizRepository(private val db: DenknotizDatabase) {
    val sessions: Flow<List<SessionEntity>> = db.sessionDao().observeAll()

    fun observeBundle(sessionId: String): Flow<SessionBundle?> = combine(
        db.sessionDao().observe(sessionId),
        db.entryDao().observeForSession(sessionId),
        db.evaluationDao().observeForSession(sessionId),
        db.boundaryDao().observe(sessionId),
    ) { session, entries, snapshots, boundary ->
        session?.let { SessionBundle(it, entries, snapshots, boundary) }
    }

    suspend fun createSession(): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        db.sessionDao().insert(SessionEntity(id, "Neue Denknotiz", now, now))
        return id
    }

    suspend fun updateSession(session: SessionEntity) = db.sessionDao().update(session.copy(updatedAt = System.currentTimeMillis()))
    suspend fun togglePinned(id: String) = db.sessionDao().togglePinned(id, System.currentTimeMillis())
    suspend fun setArchived(id: String, archived: Boolean) = db.sessionDao().setArchived(id, archived, System.currentTimeMillis())
    suspend fun firstVisibleSessionExcept(id: String): SessionEntity? = db.sessionDao().firstVisibleExcept(id)
    suspend fun deleteSession(id: String) = db.sessionDao().delete(id)

    suspend fun addNote(sessionId: String, text: String): EntryEntity = db.withTransaction {
        val clean = text.trim()
        require(clean.isNotBlank())
        val now = System.currentTimeMillis()
        val ordinal = db.entryDao().maxOrdinal(sessionId) + 1
        val localTitle = localTitle(clean)
        val entry = EntryEntity(UUID.randomUUID().toString(), sessionId, ordinal, EntryType.NOTE, localTitle, clean, now, now)
        db.entryDao().insert(entry)
        val session = db.sessionDao().get(sessionId) ?: error("Sitzung nicht gefunden")
        db.sessionDao().update(session.copy(
            title = if (session.title == "Neue Denknotiz" && !session.titleManual) localTitle else session.title,
            updatedAt = now,
        ))
        entry
    }

    suspend fun editNote(id: String, text: String) = db.withTransaction {
        val current = db.entryDao().get(id) ?: return@withTransaction
        require(current.type == EntryType.NOTE)
        db.entryDao().update(current.copy(text = text.trim(), updatedAt = System.currentTimeMillis()))
        db.entryDao().markResponsesHistorical(id)
    }

    suspend fun editNoteTitle(id: String, title: String) {
        val current = db.entryDao().get(id) ?: return
        require(current.type == EntryType.NOTE)
        db.entryDao().update(current.copy(
            title = title.trim().ifBlank { localTitle(current.text) },
            titleManual = true,
            updatedAt = System.currentTimeMillis(),
        ))
    }

    suspend fun duplicateNote(id: String): EntryEntity {
        val current = db.entryDao().get(id) ?: error("Notiz nicht gefunden")
        require(current.type == EntryType.NOTE)
        return addNote(current.sessionId, current.text)
    }

    suspend fun improveNote(id: String, improved: String) = db.withTransaction {
        val current = db.entryDao().get(id) ?: return@withTransaction
        require(current.type == EntryType.NOTE)
        db.entryDao().update(current.copy(
            text = improved.trim(),
            originalText = current.originalText ?: current.text,
            updatedAt = System.currentTimeMillis(),
        ))
        db.entryDao().markResponsesHistorical(id)
    }

    suspend fun restoreNote(id: String) = db.withTransaction {
        val current = db.entryDao().get(id) ?: return@withTransaction
        val original = current.originalText ?: return@withTransaction
        db.entryDao().update(current.copy(text = original, originalText = null, updatedAt = System.currentTimeMillis()))
        db.entryDao().markResponsesHistorical(id)
    }

    suspend fun deleteEntry(id: String) = db.withTransaction {
        val entry = db.entryDao().get(id) ?: return@withTransaction
        if (entry.type == EntryType.NOTE) db.entryDao().markResponsesHistorical(id)
        db.entryDao().delete(id)
    }

    suspend fun createSnapshot(
        sessionId: String,
        focus: String,
        profileId: String,
        web: Boolean,
        model: String,
        reasoning: String,
        profileInstruction: String,
        maxChunkChars: Int,
    ): EvaluationSnapshotEntity = db.withTransaction {
        val lower = db.boundaryDao().get(sessionId)?.lastIncludedOrdinal ?: 0
        val upper = db.entryDao().maxOrdinal(sessionId)
        val notes = db.entryDao().notesInRange(sessionId, lower, upper)
        require(notes.isNotEmpty()) { "Seit der letzten Auswertung gibt es keine neue Notiz." }
        val sourceJson = JSONArray(notes.map(EntryEntity::id)).toString()
        val sourceNotesJson = JSONArray(notes.map { note ->
            JSONObject().put("id", note.id).put("ordinal", note.ordinal).put("title", note.title).put("text", note.text)
        }).toString()
        val count = chunkNotes(notes, maxChunkChars).size
        EvaluationSnapshotEntity(
            id = UUID.randomUUID().toString(), sessionId = sessionId, lowerOrdinalExclusive = lower,
            upperOrdinalInclusive = upper, sourceNoteIdsJson = sourceJson, focusQuestion = focus.trim(), profileId = profileId,
            webEnabled = web, model = model, reasoning = reasoning, chunkCount = count, status = SnapshotStatus.RUNNING,
            createdAt = System.currentTimeMillis(), sourceNotesJson = sourceNotesJson, profileInstruction = profileInstruction,
        ).also { db.evaluationDao().insert(it) }
    }

    suspend fun snapshotInput(snapshot: EvaluationSnapshotEntity): List<String> {
        val frozen = runCatching {
            val array = JSONArray(snapshot.sourceNotesJson)
            (0 until array.length()).mapNotNull { index -> array.optJSONObject(index) }.map { note ->
                "[Notiz ${note.optLong("ordinal")} – ${note.optString("title")} ]\n${note.optString("text")}\n\n"
            }
        }.getOrDefault(emptyList())
        if (frozen.isNotEmpty()) return chunkBlocks(frozen, MODEL_CHUNK_CHARS)
        val sourceIds = runCatching {
            val array = JSONArray(snapshot.sourceNoteIdsJson)
            (0 until array.length()).mapNotNull(array::optString).toSet()
        }.getOrDefault(emptySet())
        val notes = db.entryDao().notesInRange(snapshot.sessionId, snapshot.lowerOrdinalExclusive, snapshot.upperOrdinalInclusive)
            .filter { it.id in sourceIds }
        return chunkNotes(notes, MODEL_CHUNK_CHARS)
    }

    suspend fun markSnapshotFailed(snapshot: EvaluationSnapshotEntity, message: String) {
        db.evaluationDao().update(snapshot.copy(status = SnapshotStatus.FAILED, error = message.take(500)))
    }

    suspend fun beginRetry(snapshotId: String): EvaluationSnapshotEntity {
        val snapshot = db.evaluationDao().get(snapshotId) ?: error("Snapshot nicht gefunden")
        val running = snapshot.copy(status = SnapshotStatus.RUNNING, error = "")
        db.evaluationDao().update(running)
        return running
    }

    suspend fun recoverInterruptedEvaluations() = db.evaluationDao().failRunning("Die App wurde während der Auswertung beendet. Bitte erneut versuchen.")

    suspend fun completeSnapshot(snapshot: EvaluationSnapshotEntity, text: String, citationsJson: String): EntryEntity =
        db.withTransaction {
            db.entryDao().responseForSnapshot(snapshot.id)?.let { existing ->
                val now = System.currentTimeMillis()
                db.evaluationDao().update(snapshot.copy(status = SnapshotStatus.COMPLETED, error = "", completedAt = snapshot.completedAt ?: now))
                advanceBoundary(snapshot, existing.id, now)
                return@withTransaction existing
            }
            val now = System.currentTimeMillis()
            val response = EntryEntity(
                id = UUID.randomUUID().toString(), sessionId = snapshot.sessionId,
                ordinal = db.entryDao().maxOrdinal(snapshot.sessionId) + 1, type = EntryType.AI_RESPONSE,
                title = "KI-Auswertung", text = text.trim(), createdAt = now, updatedAt = now,
                snapshotId = snapshot.id, citationsJson = citationsJson,
            )
            db.entryDao().insert(response)
            db.evaluationDao().update(snapshot.copy(status = SnapshotStatus.COMPLETED, error = "", completedAt = now))
            advanceBoundary(snapshot, response.id, now)
            db.sessionDao().get(snapshot.sessionId)?.let { db.sessionDao().update(it.copy(updatedAt = now)) }
            response
        }

    suspend fun responseAsNote(responseId: String): EntryEntity {
        val response = db.entryDao().get(responseId) ?: error("Antwort nicht gefunden")
        return addNote(response.sessionId, response.text)
    }

    suspend fun setGeneratedTitles(sessionId: String, entryId: String, title: String) {
        val session = db.sessionDao().get(sessionId) ?: return
        if (!session.titleManual && !session.titleGenerated && title.isNotBlank()) {
            db.sessionDao().update(session.copy(title = title.trim().take(100), titleGenerated = true, updatedAt = System.currentTimeMillis()))
        }
        val entry = db.entryDao().get(entryId) ?: return
        if (!entry.titleManual && !entry.titleGenerated && title.isNotBlank()) {
            db.entryDao().update(entry.copy(title = title.trim().take(100), titleGenerated = true, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun backup(profileNames: Map<String, String>, profileInstructions: Map<String, String>): BackupPayload = db.withTransaction {
        BackupPayload(
            db.sessionDao().all(), db.entryDao().all(), db.evaluationDao().all(), db.boundaryDao().all(),
            profileNames, profileInstructions,
        )
    }

    suspend fun merge(payload: BackupPayload): Int = db.withTransaction {
        payload.entries.forEach { incoming ->
            val conflict = db.entryDao().getByOrdinal(incoming.sessionId, incoming.ordinal)
            require(conflict == null || conflict.id == incoming.id) {
                "Die Sicherung enthält für dieselbe Sitzung unterschiedliche Einträge an Position ${incoming.ordinal}. Der Import wurde ohne Änderungen abgebrochen."
            }
        }
        var imported = 0
        payload.sessions.forEach { if (db.sessionDao().insert(it) != -1L) imported++ }
        payload.entries.forEach { if (db.entryDao().insert(it) != -1L) imported++ }
        payload.snapshots.forEach { if (db.evaluationDao().insert(it) != -1L) imported++ }
        payload.boundaries.forEach { boundary ->
            val current = db.boundaryDao().get(boundary.sessionId)
            if (current == null || boundary.lastIncludedOrdinal > current.lastIncludedOrdinal) db.boundaryDao().upsert(boundary)
        }
        imported
    }

    private fun chunkNotes(notes: List<EntryEntity>, maxChars: Int): List<String> {
        val blocks = notes.map { note -> "[Notiz ${note.ordinal}]\n${note.text}\n\n" }
        return chunkBlocks(blocks, maxChars)
    }

    private fun chunkBlocks(blocks: List<String>, maxChars: Int): List<String> {
        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        blocks.forEach { block ->
            if (current.isNotEmpty() && current.length + block.length > maxChars) {
                chunks += current.toString()
                current = StringBuilder()
            }
            if (block.length <= maxChars) current.append(block) else {
                block.chunked(maxChars).forEach { part ->
                    if (current.isNotEmpty()) { chunks += current.toString(); current = StringBuilder() }
                    chunks += part
                }
            }
        }
        if (current.isNotEmpty()) chunks += current.toString()
        return chunks
    }

    private suspend fun advanceBoundary(snapshot: EvaluationSnapshotEntity, responseId: String, now: Long) {
        val boundary = ContextBoundaryEntity(snapshot.sessionId, snapshot.upperOrdinalInclusive, responseId, now)
        if (db.boundaryDao().insertIfMissing(boundary) == -1L) {
            db.boundaryDao().advance(snapshot.sessionId, snapshot.upperOrdinalInclusive, responseId, now)
        }
    }

    private fun localTitle(text: String): String = text.lineSequence().first().trim()
        .split(Regex("\\s+")).take(8).joinToString(" ").take(72).ifBlank { "Neue Notiz" }

    companion object { const val MODEL_CHUNK_CHARS = 48_000 }
}
