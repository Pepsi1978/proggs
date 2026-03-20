package com.quizverse.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quizverse.app.data.database.dao.AchievementDao
import com.quizverse.app.data.database.dao.HighScoreDao
import com.quizverse.app.data.database.dao.ProgressDao
import com.quizverse.app.data.database.dao.QuestionDao
import com.quizverse.app.data.database.entities.Achievement
import com.quizverse.app.data.database.entities.Category
import com.quizverse.app.data.database.entities.HighScore
import com.quizverse.app.data.database.entities.Question
import com.quizverse.app.data.database.entities.UserProgress
import com.quizverse.app.data.prepopulate.QuestionSeeder
import androidx.room.migration.Migration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Question::class,
        Category::class,
        UserProgress::class,
        HighScore::class,
        Achievement::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QuizDatabase : RoomDatabase() {

    abstract fun questionDao(): QuestionDao
    abstract fun progressDao(): ProgressDao
    abstract fun highScoreDao(): HighScoreDao
    abstract fun achievementDao(): AchievementDao

    companion object {

        @Volatile
        private var INSTANCE: QuizDatabase? = null

        /**
         * Returns the singleton database instance, creating it if necessary.
         * Thread-safe via double-checked locking on [INSTANCE].
         */
        fun getDatabase(context: Context): QuizDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Migration from version 1 to 2: no-op (schema unchanged, version bumped
         * during development). Keeps all user data intact on update.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Schema was identical between v1 and v2 — nothing to alter.
            }
        }

        private fun buildDatabase(context: Context): QuizDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                QuizDatabase::class.java,
                "quiz_database"
            )
                .addMigrations(MIGRATION_1_2)
                .addCallback(DatabaseCallback())
                .build()
        }
    }

    /**
     * Room callback invoked the very first time the database is created.
     * Populates the database with seed data via [QuestionSeeder].
     */
    private class DatabaseCallback : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    QuestionSeeder.seedDatabase(database)
                }
            }
        }
    }
}
