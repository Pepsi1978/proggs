package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import de.frank.entropyreducer.data.local.entities.IdeaEntity
import de.frank.entropyreducer.data.local.entities.IdeaFollowupEntity
import kotlinx.coroutines.flow.Flow

/**
 * Idee samt ihren Nachtraegen (Room-@Relation). Quelle des reaktiven UI-/Backup-Flows ab Etappe 2c.
 * Die Followups werden NICHT von Room sortiert (@Relation kann das nicht) — die Reihenfolge stellt
 * der Mapper nach `createdAtMs` her.
 */
data class IdeaWithFollowups(
    @Embedded val idea: IdeaEntity,
    @Relation(parentColumn = "id", entityColumn = "ideaId")
    val followups: List<IdeaFollowupEntity>,
)

/** DAO fuer Ideen (Tabelle ideas) und ihre Nachtraege (idea_followups). ID-Architektur Etappe 2. */
@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY timestampMs DESC")
    fun getAllIdeas(): Flow<List<IdeaEntity>>

    /**
     * Reaktiver Flow aller Ideen MIT Nachtraegen — die Lesequelle des Ideen-Reiters ab Etappe 2c
     * (loest `ideenEntriesFlow` auf DataStore-JSON ab). `@Transaction` ist bei `@Relation` Pflicht
     * (Room/BP §5: sonst inkonsistente Eltern/Kinder).
     */
    @Transaction
    @Query("SELECT * FROM ideas ORDER BY timestampMs DESC")
    fun getAllIdeasWithFollowups(): Flow<List<IdeaWithFollowups>>

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

    // Bugfix 2026-07-03 (Room-Almanach K1): @Upsert — REPLACE loeschte via CASCADE die
    // idea_followups des Eintrags bei jedem Update/Sync.
    @Upsert
    suspend fun upsertIdea(idea: IdeaEntity)

    @Upsert
    suspend fun upsertIdeas(ideas: List<IdeaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFollowups(followups: List<IdeaFollowupEntity>)

    @Update suspend fun updateIdea(idea: IdeaEntity)

    @Query("DELETE FROM ideas WHERE id = :id")
    suspend fun deleteIdeaById(id: String)

    @Query("DELETE FROM idea_followups WHERE id = :id")
    suspend fun deleteFollowupById(id: String)

    /** Idee + alle Nachtraege atomar schreiben (Migration/Restore). */
    @Transaction
    suspend fun upsertIdeaWithFollowups(idea: IdeaEntity, followups: List<IdeaFollowupEntity>) {
        upsertIdea(idea)
        if (followups.isNotEmpty()) upsertFollowups(followups)
    }
}
