package de.frank.experimente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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
        /**
         * 1 → 2: der Monitor (B-10).
         *
         * Die Tabelle `experimente` wird neu aufgebaut, weil `startedAt` von *Pflicht* auf
         * *darf leer sein* wechselt — das lässt sich in SQLite nicht nachträglich ändern.
         * Bestehende Sätze werden vollständig übernommen: `OFFEN` heißt jetzt `LAEUFT`,
         * alles Bestehende kam aus einem KI-Vorschlag (die einzige Quelle vor F-35), und
         * `addedAt` wird auf den Starttag zurückdatiert. Es geht kein Satz verloren.
         */
        val VON_1_NACH_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE experimente_neu (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        days INTEGER NOT NULL,
                        level TEXT NOT NULL,
                        origin TEXT NOT NULL,
                        addedAt TEXT NOT NULL,
                        order_index INTEGER NOT NULL,
                        startedAt TEXT,
                        state TEXT NOT NULL,
                        closedAt TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO experimente_neu
                        (id, title, description, days, level, origin, addedAt, order_index,
                         startedAt, state, closedAt)
                    SELECT id, title, description, days, level,
                           'KI_VORSCHLAG',
                           startedAt || 'T00:00:00Z',
                           0,
                           startedAt,
                           CASE state WHEN 'OFFEN' THEN 'LAEUFT' ELSE state END,
                           closedAt
                    FROM experimente
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE experimente")
                db.execSQL("ALTER TABLE experimente_neu RENAME TO experimente")
            }
        }

        @Volatile
        private var vorhanden: ExperimenteDatenbank? = null

        fun hol(ctx: Context): ExperimenteDatenbank =
            vorhanden ?: synchronized(this) {
                vorhanden ?: Room.databaseBuilder(
                    ctx.applicationContext,
                    ExperimenteDatenbank::class.java,
                    "experimente.db",
                ).addMigrations(VON_1_NACH_2).build().also { vorhanden = it }
            }
    }
}
