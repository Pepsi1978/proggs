package com.bestjournal.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bestjournal.app.data.local.entity.AdviceBlockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdviceDashboardDao {

    @Query("SELECT * FROM advice_blocks ORDER BY entropyLevel DESC, categoryName ASC")
    fun getAll(): Flow<List<AdviceBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(blocks: List<AdviceBlockEntity>)

    @Query("DELETE FROM advice_blocks")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM advice_blocks")
    suspend fun getBlockCount(): Int

    @Query("SELECT * FROM advice_blocks ORDER BY entropyLevel DESC")
    suspend fun getAllSync(): List<AdviceBlockEntity>
}
