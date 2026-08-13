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
    version = 4,
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

        /**
         * 2 → 3: der Logbuch-Reiter *Auswertungen*.
         *
         * `auswertungen` bekommt `createdAt`, damit jede Auswertung mit Datum **und Uhrzeit**
         * dasteht. Die Spalte darf leer sein — bestehende Auswertungen haben keine Uhrzeit,
         * und eine erfundene wäre schlimmer als keine. Für sie wird der Kalendertag angezeigt.
         *
         * Reines Hinzufügen einer Spalte: `ALTER TABLE ADD COLUMN` genügt, die Tabelle wird
         * nicht neu gebaut, kein Satz wird angefasst.
         */
        val VON_2_NACH_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auswertungen ADD COLUMN createdAt TEXT")
            }
        }

        /**
         * 3 → 4: jede Aufnahme bleibt für sich stehen.
         *
         * Zwei reine Hinzufügungen, keine Zeile wird angefasst:
         *
         * - `auswertungen.dayIndex` — der wievielte Versuchstag eine Auswertung war. Für den
         *   Bestand wird er aus `date` und dem Starttag des Experiments errechnet; das geht
         *   nur hier, weil die Dauer später verändert werden kann. Ohne Starttag (oder ohne
         *   Experiment) bleibt er leer, und die Oberfläche zeigt dann schlicht das Datum.
         * - `gespraech.art` — trennt Gesprächsrunden von Auswertungsrunden. Der Bestand wird
         *   nachgezogen: eine eigene Runde, die mit „Auswertung Tag" beginnt, ist eine
         *   Auswertung, und die unmittelbar darauf folgende Antwort der KI zum selben
         *   Experiment gehört dazu. Beides bleibt im Faden, nur die Anzeige unterscheidet.
         */
        val VON_3_NACH_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auswertungen ADD COLUMN dayIndex INTEGER")
                db.execSQL(
                    """
                    UPDATE auswertungen SET dayIndex = (
                        SELECT MAX(
                            1,
                            CAST(julianday(auswertungen.date) - julianday(e.startedAt) AS INTEGER) + 1
                        )
                        FROM experimente e
                        WHERE e.id = auswertungen.experimentId AND e.startedAt IS NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "ALTER TABLE gespraech ADD COLUMN art TEXT NOT NULL DEFAULT 'GESPRAECH'",
                )
                db.execSQL(
                    """
                    UPDATE gespraech SET art = 'AUSWERTUNG'
                    WHERE role = 'ICH' AND text LIKE 'Auswertung Tag%'
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE gespraech SET art = 'AUSWERTUNG'
                    WHERE role = 'KI' AND EXISTS (
                        SELECT 1 FROM gespraech eigen
                        WHERE eigen.role = 'ICH'
                          AND eigen.art = 'AUSWERTUNG'
                          AND eigen.experimentId = gespraech.experimentId
                          AND eigen.id = gespraech.id - 1
                    )
                    """.trimIndent(),
                )
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
                ).addMigrations(VON_1_NACH_2, VON_2_NACH_3, VON_3_NACH_4)
                    .build().also { vorhanden = it }
            }
    }
}
