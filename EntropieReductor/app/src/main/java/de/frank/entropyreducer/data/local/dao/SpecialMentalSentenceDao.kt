package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.SpecialMentalEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO fuer spezielle Mental-Saetze (Tabelle special_mental_sentences, Frank-Wunsch 2026-07-15).
 * Reihenfolge = Einfuege-Reihenfolge ueber `position`; alle Lese-Queries sortieren danach.
 */
@Dao
interface SpecialMentalSentenceDao {
    @Query("SELECT * FROM special_mental_sentences ORDER BY position ASC")
    fun getAll(): Flow<List<SpecialMentalEntity>>

    @Query("SELECT * FROM special_mental_sentences WHERE id = :id")
    suspend fun getById(id: String): SpecialMentalEntity?

    /** Hoechste vergebene Position (oder -1, wenn leer) — fuer "neuer Satz ans Ende". */
    @Query("SELECT COALESCE(MAX(position), -1) FROM special_mental_sentences")
    suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(mental: SpecialMentalEntity)

    @Update suspend fun update(mental: SpecialMentalEntity)

    @Query("UPDATE special_mental_sentences SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM special_mental_sentences WHERE id = :id")
    suspend fun deleteById(id: String)
}
