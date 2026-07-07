package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.MentalEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO fuer Mental-Board-Saetze (Tabelle mental_sentences). ID-Architektur Etappe 4.
 *
 * Die manuelle Reihenfolge liegt in `position` (Drag-and-Drop) — alle Lese-Queries sortieren danach.
 */
@Dao
interface MentalSentenceDao {
    @Query("SELECT * FROM mental_sentences ORDER BY position ASC")
    fun getAll(): Flow<List<MentalEntity>>

    @Query("SELECT * FROM mental_sentences ORDER BY position ASC")
    suspend fun getAllForBackup(): List<MentalEntity>

    @Query("SELECT * FROM mental_sentences WHERE id = :id")
    suspend fun getById(id: String): MentalEntity?

    @Query("SELECT COUNT(*) FROM mental_sentences")
    suspend fun count(): Int

    /** Hoechste vergebene Position (oder -1, wenn leer) — fuer "neuer Satz ans Ende". */
    @Query("SELECT COALESCE(MAX(position), -1) FROM mental_sentences")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mental: MentalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(mentals: List<MentalEntity>)

    @Update suspend fun update(mental: MentalEntity)

    /**
     * Backfill (Frank-Wunsch 2026-06-20): Altbestand-Saetze ohne Herkunft selbst-verwurzeln
     * (originId/rootId = eigene id, originType=MENTAL). Idempotent — trifft nur Zeilen ohne Herkunft.
     */
    @Query(
        "UPDATE mental_sentences SET originId = id, originType = 'MENTAL', rootId = id " +
            "WHERE originId IS NULL OR originId = ''"
    )
    suspend fun backfillSelfRoot(): Int

    @Query("DELETE FROM mental_sentences WHERE id = :id")
    suspend fun deleteById(id: String)
}
