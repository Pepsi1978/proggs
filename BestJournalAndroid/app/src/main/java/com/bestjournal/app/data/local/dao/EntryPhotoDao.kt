package com.bestjournal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bestjournal.app.data.local.entity.EntryPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryPhotoDao {

    @Query("SELECT * FROM entry_photos WHERE entryId = :entryId ORDER BY timestamp ASC")
    fun getPhotosForEntry(entryId: Long): Flow<List<EntryPhotoEntity>>

    @Insert suspend fun insert(photo: EntryPhotoEntity): Long

    @Query("DELETE FROM entry_photos WHERE id = :photoId") suspend fun deleteById(photoId: Long)

    @Query("SELECT filePath FROM entry_photos WHERE id = :photoId")
    suspend fun getFilePath(photoId: Long): String?

    @Query(
        "SELECT p.* FROM entry_photos p INNER JOIN journal_entries e ON p.entryId = e.id WHERE e.timestamp BETWEEN :startDate AND :endDate ORDER BY p.timestamp ASC"
    )
    suspend fun getPhotosForDateRange(startDate: Long, endDate: Long): List<EntryPhotoEntity>

    @Query("SELECT filePath FROM entry_photos") suspend fun getAllFilePaths(): List<String>
}
