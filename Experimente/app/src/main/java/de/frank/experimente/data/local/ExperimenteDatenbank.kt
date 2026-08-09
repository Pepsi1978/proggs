package de.frank.experimente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant
import java.time.LocalDate

class Konverter {
    @TypeConverter fun zuDatum(wert: String?): LocalDate? = wert?.let(LocalDate::parse)
    @TypeConverter fun vonDatum(wert: LocalDate?): String? = wert?.toString()

    @TypeConverter fun zuZeitpunkt(wert: Long?): Instant? = wert?.let(Instant::ofEpochMilli)
    @TypeConverter fun vonZeitpunkt(wert: Instant?): Long? = wert?.toEpochMilli()

    @TypeConverter fun zuStufe(wert: String?): Stufe? = wert?.let(Stufe::valueOf)
    @TypeConverter fun vonStufe(wert: Stufe?): String? = wert?.name

    @TypeConverter fun zuZustand(wert: String?): ExperimentZustand? = wert?.let(ExperimentZustand::valueOf)
    @TypeConverter fun vonZustand(wert: ExperimentZustand?): String? = wert?.name

    @TypeConverter fun zuQuelle(wert: String?): MerkQuelle? = wert?.let(MerkQuelle::valueOf)
    @TypeConverter fun vonQuelle(wert: MerkQuelle?): String? = wert?.name

    @TypeConverter fun zuSprecher(wert: String?): Sprecher? = wert?.let(Sprecher::valueOf)
    @TypeConverter fun vonSprecher(wert: Sprecher?): String? = wert?.name
}

@Database(
    entities = [
        SelbstbildSatz::class,
        Ziel::class,
        LogTag::class,
        Lage::class,
        Vorschlag::class,
        Experiment::class,
        Aufgabe::class,
        Auswertung::class,
        GespraechsRunde::class,
        MerkEintrag::class,
        Erkenntnis::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Konverter::class)
abstract class ExperimenteDatenbank : RoomDatabase() {

    abstract fun selbstbild(): SelbstbildDao
    abstract fun ziele(): ZielDao
    abstract fun log(): LogDao
    abstract fun lage(): LageDao
    abstract fun vorschlaege(): VorschlagDao
    abstract fun experimente(): ExperimentDao
    abstract fun aufgaben(): AufgabeDao
    abstract fun auswertungen(): AuswertungDao
    abstract fun gespraeche(): GespraechDao
    abstract fun merkliste(): MerklisteDao
    abstract fun erkenntnisse(): ErkenntnisDao

    companion object {
        @Volatile private var instanz: ExperimenteDatenbank? = null

        fun hole(context: Context): ExperimenteDatenbank =
            instanz ?: synchronized(this) {
                instanz ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExperimenteDatenbank::class.java,
                    "experimente.db",
                ).build().also { instanz = it }
            }
    }
}
