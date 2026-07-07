package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.EntropyEntryFollowupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntropyEntryFollowupDao {

    @Query("SELECT * FROM entropy_entry_followups WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun observeByEntryId(entryId: String): Flow<List<EntropyEntryFollowupEntity>>

    @Query("SELECT * FROM entropy_entry_followups WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun getByEntryId(entryId: String): List<EntropyEntryFollowupEntity>

    @Query("SELECT * FROM entropy_entry_followups WHERE id = :id")
    suspend fun getById(id: String): EntropyEntryFollowupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(followup: EntropyEntryFollowupEntity)

    @Update suspend fun update(followup: EntropyEntryFollowupEntity)

    @Delete suspend fun delete(followup: EntropyEntryFollowupEntity)

    @Query("DELETE FROM entropy_entry_followups WHERE id = :id") suspend fun deleteById(id: String)

    @Query("DELETE FROM entropy_entry_followups WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("SELECT * FROM entropy_entry_followups ORDER BY createdAt ASC")
    suspend fun getAllForBackup(): List<EntropyEntryFollowupEntity>
}
