package com.nems.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nems.app.data.local.entity.SupplementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplementDao {

    @Query("SELECT * FROM supplements WHERE isActive = 1 ORDER BY sortOrder")
    fun getAllActive(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements ORDER BY sortOrder")
    fun getAll(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE id = :id")
    suspend fun getById(id: String): SupplementEntity?

    @Query("SELECT * FROM supplements WHERE defaultStackSectionId = :sectionId AND isActive = 1 ORDER BY sortOrder")
    fun getBySectionId(sectionId: String): Flow<List<SupplementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(supplements: List<SupplementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplement: SupplementEntity)

    @Update
    suspend fun update(supplement: SupplementEntity)

    @Delete
    suspend fun delete(supplement: SupplementEntity)

    @Query("SELECT COUNT(*) FROM supplements")
    suspend fun count(): Int
}
