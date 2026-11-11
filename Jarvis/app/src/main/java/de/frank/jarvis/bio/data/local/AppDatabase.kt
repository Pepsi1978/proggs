package de.frank.jarvis.bio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import de.frank.jarvis.bio.data.local.dao.AmazfitDailyDao
import de.frank.jarvis.bio.data.local.dao.AmazfitWorkoutDao
import de.frank.jarvis.bio.data.local.dao.BiomarkerSnapshotDao
import de.frank.jarvis.bio.data.local.dao.HealthConnectValueDao
import de.frank.jarvis.bio.data.local.dao.OuraActivityDao
import de.frank.jarvis.bio.data.local.dao.OuraDailySleepDao
import de.frank.jarvis.bio.data.local.dao.OuraPersonalInfoDao
import de.frank.jarvis.bio.data.local.dao.OuraReadinessDao
import de.frank.jarvis.bio.data.local.dao.OuraResilienceDao
import de.frank.jarvis.bio.data.local.dao.OuraSleepDetailDao
import de.frank.jarvis.bio.data.local.dao.WhoopWorkoutDao
import de.frank.jarvis.bio.data.local.entities.AmazfitDailyEntity
import de.frank.jarvis.bio.data.local.entities.AmazfitWorkoutEntity
import de.frank.jarvis.bio.data.local.entities.BiomarkerSnapshotEntity
import de.frank.jarvis.bio.data.local.entities.HealthConnectValueEntity
import de.frank.jarvis.bio.data.local.entities.OuraActivityEntity
import de.frank.jarvis.bio.data.local.entities.OuraDailySleepEntity
import de.frank.jarvis.bio.data.local.entities.OuraPersonalInfoEntity
import de.frank.jarvis.bio.data.local.entities.OuraReadinessEntity
import de.frank.jarvis.bio.data.local.entities.OuraResilienceEntity
import de.frank.jarvis.bio.data.local.entities.OuraSleepDetailEntity
import de.frank.jarvis.bio.data.local.entities.WhoopWorkoutEntity

/**
 * Die Biomarker-Datenbank von Jarvis: genau die Biomarker-Tabellen aus Entropie Reductor (dort `entropy_reducer.db`,
 * Schema 37), mit denselben Tabellen- und Spaltennamen. So übernimmt [de.frank.jarvis.bio.BioUebernahme] die Zeilen
 * spaltengenau, und der übernommene Code arbeitet unverändert.
 */
@Database(
    entities = [
        BiomarkerSnapshotEntity::class,
        WhoopWorkoutEntity::class,
        AmazfitDailyEntity::class,
        AmazfitWorkoutEntity::class,
        OuraReadinessEntity::class,
        OuraDailySleepEntity::class,
        OuraActivityEntity::class,
        OuraResilienceEntity::class,
        OuraSleepDetailEntity::class,
        OuraPersonalInfoEntity::class,
        HealthConnectValueEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun biomarkerSnapshotDao(): BiomarkerSnapshotDao
    abstract fun whoopWorkoutDao(): WhoopWorkoutDao
    abstract fun amazfitDailyDao(): AmazfitDailyDao
    abstract fun amazfitWorkoutDao(): AmazfitWorkoutDao
    abstract fun ouraReadinessDao(): OuraReadinessDao
    abstract fun ouraDailySleepDao(): OuraDailySleepDao
    abstract fun ouraActivityDao(): OuraActivityDao
    abstract fun ouraResilienceDao(): OuraResilienceDao
    abstract fun ouraSleepDetailDao(): OuraSleepDetailDao
    abstract fun ouraPersonalInfoDao(): OuraPersonalInfoDao
    abstract fun healthConnectValueDao(): HealthConnectValueDao

    companion object {
        const val NAME = "jarvis_biomarker.db"
        /** Die Tabellen, die die Übernahme kopiert, in dieser Reihenfolge. */
        val TABELLEN = listOf(
            "biomarker_snapshots", "whoop_workouts", "amazfit_daily", "amazfit_workouts",
            "oura_daily_readiness", "oura_daily_sleep", "oura_daily_activity", "oura_daily_resilience",
            "oura_sleep_detail", "oura_personal_info", "hc_value_cache",
        )

        fun baue(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
