package de.frank.fisetinbegleiter.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface FisetinDao {
    @Query("SELECT * FROM protocol_templates WHERE id = 1")
    fun observeProtocol(): Flow<ProtocolTemplateEntity?>

    @Query("SELECT * FROM protocol_templates WHERE id = 1")
    suspend fun getProtocol(): ProtocolTemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProtocol(protocol: ProtocolTemplateEntity)

    @Query("SELECT * FROM ingredients ORDER BY phase, sortOrder")
    fun observeIngredients(): Flow<List<IngredientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIngredient(ingredient: IngredientEntity): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)

    @Query("SELECT COUNT(*) FROM ingredients")
    suspend fun ingredientCount(): Int

    @Query("SELECT * FROM stack_items ORDER BY category, sortOrder")
    fun observeStackItems(): Flow<List<StackItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStackItem(item: StackItemEntity): Long

    @Insert
    suspend fun insertStackItems(items: List<StackItemEntity>)

    @Delete
    suspend fun deleteStackItem(item: StackItemEntity)

    @Query("SELECT COUNT(*) FROM stack_items")
    suspend fun stackItemCount(): Int

    @Insert
    suspend fun insertCure(cure: CureEntity): Long

    @Update
    suspend fun updateCure(cure: CureEntity)

    @Query("DELETE FROM cures WHERE status != 'ABGESCHLOSSEN'")
    suspend fun deleteAllActiveCures()

    @Insert
    suspend fun insertCureDays(days: List<CureDayEntity>): List<Long>

    @Update
    suspend fun updateCureDay(day: CureDayEntity)

    @Query("SELECT * FROM cure_days WHERE id = :id")
    suspend fun getCureDay(id: Long): CureDayEntity?

    @Query("DELETE FROM cure_days WHERE cureId = :cureId AND dayNumber > :lastDayNumber")
    suspend fun deleteCureDaysAfter(cureId: Long, lastDayNumber: Int)

    @Transaction
    @Query("SELECT * FROM cures WHERE status != 'ABGESCHLOSSEN' ORDER BY id DESC LIMIT 1")
    fun observeActiveCure(): Flow<CureWithDays?>

    @Transaction
    @Query("SELECT * FROM cures WHERE status != 'ABGESCHLOSSEN' ORDER BY id DESC")
    suspend fun getActiveCures(): List<CureWithDays>

    @Transaction
    @Query("SELECT * FROM cures ORDER BY startEpochDay DESC, id DESC")
    fun observeCureHistory(): Flow<List<CureWithDays>>
}

@Database(
    entities = [
        ProtocolTemplateEntity::class,
        IngredientEntity::class,
        StackItemEntity::class,
        CureEntity::class,
        CureDayEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class FisetinDatabase : RoomDatabase() {
    abstract fun dao(): FisetinDao

    companion object {
        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `cures` ADD COLUMN `isTestRun` INTEGER NOT NULL DEFAULT 0")
            }
        }

        // Vorstufe vor T0 und feste Drink-Basis: immer Wasser und Olivenöl.
        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `protocol_templates` ADD COLUMN `preBlockMinutes` INTEGER NOT NULL DEFAULT 720")
                database.execSQL("UPDATE `ingredients` SET `name` = 'Olivenöl' WHERE `name` = 'MCT-Öl oder Olivenöl'")
                database.execSQL("UPDATE `ingredients` SET `name` = 'Lauwarmes Wasser' WHERE `name` = 'Lauwarmes Wasser oder grüner Tee'")
                database.execSQL("UPDATE `cures` SET `carrierLiquid` = 'Wasser', `fatCarrier` = 'Olivenöl' WHERE `status` != 'ABGESCHLOSSEN'")
            }
        }

        fun create(context: Context): FisetinDatabase = Room.databaseBuilder(
            context.applicationContext,
            FisetinDatabase::class.java,
            "fisetin-begleiter.db",
        ).addMigrations(migration1To2, migration2To3).build()
    }
}
