package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.IdeaEntity
import de.frank.entropyreducer.data.local.entities.IdeaFollowupEntity
import kotlinx.coroutines.flow.Flow

/** DAO fuer Ideen (Tabelle ideas) und ihre Nachtraege (idea_followups). ID-Architektur Etappe 2. */
@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY timestampMs DESC")
    fun getAllIdeas(): Flow<List<IdeaEntity>>

    @Query("SELECT * FROM ideas")
    suspend fun getAllIdeasForBackup(): List<IdeaEntity>

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun getIdeaById(id: String): IdeaEntity?

    @Query("SELECT COUNT(*) FROM ideas")
    suspend fun countIdeas(): Int

    @Query("SELECT * FROM idea_followups WHERE ideaId = :ideaId ORDER BY createdAtMs ASC")
    suspend fun getFollowupsForIdea(ideaId: String): List<IdeaFollowupEntity>

    @Query("SELECT * FROM idea_followups")
    suspend fun getAllFollowupsForBackup(): List<IdeaFollowupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIdea(idea: IdeaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIdeas(ideas: List<IdeaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowups(followups: List<IdeaFollowupEntity>)

    @Update suspend fun updateIdea(idea: IdeaEntity)

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteIdeaById(id: String)

    /** Idee + alle Nachtraege atomar schreiben (Migration/Restore). */
    @Transaction
    suspend fun upsertIdeaWithFollowups(idea: IdeaEntity, followups: List<IdeaFollowupEntity>) {
        upsertIdea(idea)
        if (followups.isNotEmpty()) upsertFollowups(followups)
    }
}
