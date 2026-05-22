package de.frank.entropyreducer.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.frank.entropyreducer.data.local.entities.RecurringTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTemplateDao {

    /** Reactive Liste aller Vorlagen (sowohl aktiv als auch pausiert) sortiert nach Titel. */
    @Query("SELECT * FROM recurring_templates ORDER BY isActive DESC, title COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<RecurringTemplateEntity>>

    /** Liste aller AKTIVEN Vorlagen — fuer den Recurrence-Generator beim App-Start. */
    @Query("SELECT * FROM recurring_templates WHERE isActive = 1")
    suspend fun getActive(): List<RecurringTemplateEntity>

    @Query("SELECT * FROM recurring_templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: RecurringTemplateEntity)

    @Update
    suspend fun update(template: RecurringTemplateEntity)

    @Delete
    suspend fun delete(template: RecurringTemplateEntity)

    @Query("DELETE FROM recurring_templates WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Fuer Backup/Restore. */
    @Query("SELECT * FROM recurring_templates")
    suspend fun getAllForBackup(): List<RecurringTemplateEntity>
}
