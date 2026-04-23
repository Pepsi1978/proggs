package com.bestjournal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bestjournal.app.data.local.entity.EntryFollowUpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryFollowUpDao {

    @Query("SELECT * FROM entry_follow_ups WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun observeForEntry(entryId: Long): Flow<List<EntryFollowUpEntity>>

    @Query("SELECT * FROM entry_follow_ups WHERE entryId = :entryId ORDER BY createdAt ASC")
    suspend fun getForEntryOnce(entryId: Long): List<EntryFollowUpEntity>

    @Query("SELECT * FROM entry_follow_ups ORDER BY createdAt ASC")
    suspend fun getAllOnce(): List<EntryFollowUpEntity>

    @Query("SELECT COUNT(*) FROM entry_follow_ups WHERE entryId = :entryId")
    suspend fun countForEntry(entryId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(followUp: EntryFollowUpEntity): Long

    @Update
    suspend fun update(followUp: EntryFollowUpEntity)

    @Delete
    suspend fun delete(followUp: EntryFollowUpEntity)

    @Query("DELETE FROM entry_follow_ups WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: Long)
}
