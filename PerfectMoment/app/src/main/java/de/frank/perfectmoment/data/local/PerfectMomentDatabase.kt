package de.frank.perfectmoment.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SessionEntity::class,
        QuestionEntity::class,
        SkillEntity::class,
        HookEntity::class,
    ],
    version = 1,
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
                ).addCallback(SeedCallback).build().also { instance = it }
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
