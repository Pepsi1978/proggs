package de.frank.perfectmoment.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SessionEntity::class,
        QuestionEntity::class,
        SkillEntity::class,
        HookEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class PerfectMomentDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun skillDao(): SkillDao
    abstract fun hookDao(): HookDao

    companion object {
        const val DATABASE_NAME = "perfect_moment.db"

        @Volatile
        private var instance: PerfectMomentDatabase? = null

        fun getInstance(context: Context): PerfectMomentDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    PerfectMomentDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(Migration1To2, Migration2To3)
                    .addCallback(SeedCallback)
                    .build()
                    .also { instance = it }
            }

        private val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN resumeQuestionIndex INTEGER")
                db.execSQL("ALTER TABLE sessions ADD COLUMN resumeRepetition INTEGER")
                db.execSQL("ALTER TABLE sessions ADD COLUMN resumeRemainingMs INTEGER")
            }
        }

        private val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN introContext TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sessions ADD COLUMN entranceQuestion TEXT NOT NULL DEFAULT ''")
            }
        }
    }

    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val now = System.currentTimeMillis()
            db.insert(
                "skills",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("id", PreinstalledContent.RESEARCH_TEAM_SKILL_ID)
                    put("name", PreinstalledContent.RESEARCH_TEAM_SKILL_NAME)
                    put("text", PreinstalledContent.researchTeamSkillText)
                    put("createdAt", now)
                },
            )
            PreinstalledContent.hooks.forEachIndexed { index, (emoji, text) ->
                db.insert(
                    "hooks",
                    SQLiteDatabase.CONFLICT_ABORT,
                    ContentValues().apply {
                        put("id", index + 1L)
                        put("emoji", emoji)
                        put("text", text)
                        put("sortIndex", index)
                    },
                )
            }
        }
    }
}
