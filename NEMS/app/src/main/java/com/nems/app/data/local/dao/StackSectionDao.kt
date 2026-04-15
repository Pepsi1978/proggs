package com.nems.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nems.app.data.local.entity.StackSectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StackSectionDao {

    @Query("SELECT * FROM stack_sections ORDER BY sortOrder")
    fun getAll(): Flow<List<StackSectionEntity>>

    @Query("SELECT * FROM stack_sections WHERE id = :id")
    suspend fun getById(id: String): StackSectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<StackSectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: StackSectionEntity)

    @Update
    suspend fun update(section: StackSectionEntity)

    @Delete
    suspend fun delete(section: StackSectionEntity)

    @Query("SELECT COUNT(*) FROM stack_sections")
    suspend fun count(): Int
}
