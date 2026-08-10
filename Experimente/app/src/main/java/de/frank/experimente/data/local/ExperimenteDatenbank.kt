package de.frank.experimente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.time.Instant
import java.time.LocalDate

/**
 * `Instant` und `LocalDate` als Text ablegen — lesbar, sortierbar, zeitzonenfrei.
 * `LocalDate` im ISO-Format sortiert lexikografisch korrekt, darum reichen die
 * `ORDER BY date`-Abfragen in den DAOs.
 */
class Wandler {
    @TypeConverter
    fun instantNach(wert: Instant?): String? = wert?.toString()

    @TypeConverter
    fun instantVon(wert: String?): Instant? = wert?.let(Instant::parse)

    @TypeConverter
    fun datumNach(wert: LocalDate?): String? = wert?.toString()

    @TypeConverter
    fun datumVon(wert: String?): LocalDate? = wert?.let(LocalDate::parse)
}

@Database(
    entities = [
        SelfImage::class,
        Goal::class,
        LogDay::class,
        Suggestion::class,
        Experiment::class,
        Task::class,
        Evaluation::class,
        ChatTurn::class,
        WatchlistItem::class,
        Insight::class,
        Lage::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Wandler::class)
abstract class ExperimenteDatenbank : RoomDatabase() {
    abstract fun selbstbild(): SelbstbildDao
    abstract fun ziele(): ZieleDao
    abstract fun logbuch(): LogbuchDao
    abstract fun vorschlaege(): VorschlaegeDao
    abstract fun experimente(): ExperimenteDao
    abstract fun aufgaben(): AufgabenDao
    abstract fun auswertungen(): AuswertungenDao
    abstract fun gespraech(): GespraechDao
    abstract fun merkliste(): MerklisteDao
    abstract fun erkenntnisse(): ErkenntnisseDao
    abstract fun lage(): LageDao

    companion object {
        @Volatile
        private var vorhanden: ExperimenteDatenbank? = null

        fun hol(ctx: Context): ExperimenteDatenbank =
            vorhanden ?: synchronized(this) {
                vorhanden ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    ExperimenteDatenbank::class.java,
                    "experimente.db",
                ).build().also { vorhanden = it }
            }
    }
}
