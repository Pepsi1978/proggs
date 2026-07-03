package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import de.frank.entropyreducer.data.local.entities.GenieCodexVersionEntity
import de.frank.entropyreducer.data.local.entities.MemoryEntryEntity
import de.frank.entropyreducer.data.local.entities.SavedPromptEntity
import de.frank.entropyreducer.domain.model.MemorySource
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPromptDao {
    @Query("SELECT * FROM saved_prompts ORDER BY createdAt ASC")
    fun getAll(): Flow<List<SavedPromptEntity>>

    @Query("SELECT * FROM saved_prompts WHERE isActive = 1 ORDER BY createdAt ASC")
    fun getActive(): Flow<List<SavedPromptEntity>>

    @Query(
        "SELECT * FROM saved_prompts WHERE isActive = 1 AND category = :cat ORDER BY createdAt ASC"
    )
    fun getActiveByCategory(
        cat: de.frank.entropyreducer.domain.model.PromptCategory
    ): Flow<List<SavedPromptEntity>>

    @Query("SELECT * FROM saved_prompts WHERE id = :id")
    suspend fun getById(id: String): SavedPromptEntity?

    // Bugfix 2026-07-03 (Room-Almanach K1): @Upsert — REPLACE loeschte via CASCADE die
    // Tool-Permissions/Daily-Usage/Trigger des Prompts bei jeder Prompt-Aenderung.
    @Upsert suspend fun upsert(prompt: SavedPromptEntity)

    @Update suspend fun update(prompt: SavedPromptEntity)

    @Delete suspend fun delete(prompt: SavedPromptEntity)

    @Query("SELECT COUNT(*) FROM saved_prompts") suspend fun count(): Int

    // Agentic-AI: schmale Updates fuer Modell, Token-Limit, Trust-Modus.
    // Vermeidet das Lesen-Aendern-Schreiben-Pattern bei UI-Toggles.

    @Query("UPDATE saved_prompts SET model = :model, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateModel(id: String, model: String, updatedAt: Long)

    @Query(
        "UPDATE saved_prompts SET tokenLimitPerDay = :limit, updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateTokenLimit(id: String, limit: Int?, updatedAt: Long)

    @Query(
        "UPDATE saved_prompts SET trustModeDefault = :trust, updatedAt = :updatedAt WHERE id = :id"
    )
    suspend fun updateTrustMode(id: String, trust: Boolean, updatedAt: Long)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entries ORDER BY createdAt DESC")
    fun getAll(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActive(): Flow<List<MemoryEntryEntity>>

    @Query("SELECT * FROM memory_entries WHERE source = :src ORDER BY createdAt DESC")
    fun getBySource(src: MemorySource): Flow<List<MemoryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(memory: MemoryEntryEntity)

    @Update suspend fun update(memory: MemoryEntryEntity)

    @Delete suspend fun delete(memory: MemoryEntryEntity)

    @Query("DELETE FROM memory_entries") suspend fun deleteAll()

    @Query("DELETE FROM memory_entries WHERE source = :src")
    suspend fun deleteBySource(src: MemorySource)
}

@Dao
interface GenieCodexDao {
    @Query("SELECT * FROM genie_codex_versions ORDER BY createdAt DESC LIMIT 1")
    fun getLatest(): Flow<GenieCodexVersionEntity?>

    @Query("SELECT * FROM genie_codex_versions ORDER BY createdAt DESC LIMIT 10")
    fun getRecentVersions(): Flow<List<GenieCodexVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(version: GenieCodexVersionEntity)

    @Query(
        "DELETE FROM genie_codex_versions WHERE id NOT IN (SELECT id FROM genie_codex_versions ORDER BY createdAt DESC LIMIT 10)"
    )
    suspend fun pruneOldVersions()
}
