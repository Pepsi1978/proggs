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
    version = 14,
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
                ).addMigrations(
                    Migration1To2,
                    Migration2To3,
                    Migration3To4,
                    Migration4To5,
                    Migration5To6,
                    Migration6To7,
                    Migration7To8,
                    Migration8To9,
                    Migration9To10,
                    Migration10To11,
                    Migration11To12,
                    Migration12To13,
                    Migration13To14,
                )
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

        private val Migration3To4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN playCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE sessions ADD COLUMN lastPlayedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE sessions SET lastPlayedAt = startedAt")
            }
        }

        private val Migration4To5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                insertAssumptionQuestionsSkill(db, System.currentTimeMillis())
            }
        }

        private val Migration5To6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                insertConsciousnessImageSkill(db, System.currentTimeMillis())
            }
        }

        private val Migration6To7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN summary TEXT NOT NULL DEFAULT ''")
            }
        }

        private val Migration7To8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN voiceProviderOverride TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sessions ADD COLUMN voiceOverride TEXT NOT NULL DEFAULT ''")
            }
        }

        private val Migration8To9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                insertAssumptionBoostSkill(db, System.currentTimeMillis())
            }
        }

        private val Migration9To10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val updated = db.update(
                    "skills",
                    SQLiteDatabase.CONFLICT_ABORT,
                    ContentValues().apply {
                        put("text", PreinstalledContent.assumptionBoostSkillText)
                    },
                    "name = ?",
                    arrayOf(PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME),
                )
                if (updated == 0) {
                    insertAssumptionBoostSkill(db, System.currentTimeMillis())
                }
            }
        }

        private val Migration10To11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                insertAssumptionBoost2Skill(db, System.currentTimeMillis())
            }
        }

        private val Migration11To12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN summaryManual INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val Migration12To13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN custom INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val Migration13To14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN resumeQuestionOrder TEXT NOT NULL DEFAULT ''")
            }
        }

        private fun insertAssumptionQuestionsSkill(db: SupportSQLiteDatabase, createdAt: Long) {
            db.insert(
                "skills",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("name", PreinstalledContent.ASSUMPTION_QUESTIONS_SKILL_NAME)
                    put("text", PreinstalledContent.assumptionQuestionsSkillText)
                    put("createdAt", createdAt)
                },
            )
        }

        private fun insertConsciousnessImageSkill(db: SupportSQLiteDatabase, createdAt: Long) {
            db.insert(
                "skills",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("name", PreinstalledContent.CONSCIOUSNESS_IMAGE_SKILL_NAME)
                    put("text", PreinstalledContent.consciousnessImageSkillText)
                    put("createdAt", createdAt)
                },
            )
        }

        private fun insertAssumptionBoostSkill(db: SupportSQLiteDatabase, createdAt: Long) {
            db.insert(
                "skills",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("name", PreinstalledContent.ASSUMPTION_BOOST_SKILL_NAME)
                    put("text", PreinstalledContent.assumptionBoostSkillText)
                    put("createdAt", createdAt)
                },
            )
        }

        private fun insertAssumptionBoost2Skill(db: SupportSQLiteDatabase, createdAt: Long) {
            db.insert(
                "skills",
                SQLiteDatabase.CONFLICT_ABORT,
                ContentValues().apply {
                    put("name", PreinstalledContent.ASSUMPTION_BOOST_2_SKILL_NAME)
                    put("text", PreinstalledContent.assumptionBoost2SkillText)
                    put("createdAt", createdAt)
                },
            )
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
            insertAssumptionQuestionsSkill(db, now + 1)
            insertConsciousnessImageSkill(db, now + 2)
            insertAssumptionBoostSkill(db, now + 3)
            insertAssumptionBoost2Skill(db, now + 4)
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
