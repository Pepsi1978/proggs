package de.frank.gedankenspeicher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

class Wandler {
    @TypeConverter fun zustandHin(v: Notizzustand): String = v.name
    @TypeConverter fun zustandHer(v: String): Notizzustand =
        runCatching { Notizzustand.valueOf(v) }.getOrDefault(Notizzustand.FERTIG)

    @TypeConverter fun quelleHin(v: Notizquelle): String = v.name
    @TypeConverter fun quelleHer(v: String): Notizquelle =
        runCatching { Notizquelle.valueOf(v) }.getOrDefault(Notizquelle.GETIPPT)
}

@Database(
    entities = [Sitzung::class, Notiz::class, KiAntwort::class, Auswertungsprofil::class, Ordner::class],
    version = 5,
    exportSchema = false,
)
@TypeConverters(Wandler::class)
abstract class Datenbank : RoomDatabase() {
    abstract fun sitzungen(): SitzungDao
    abstract fun notizen(): NotizDao
    abstract fun antworten(): KiAntwortDao
    abstract fun profile(): ProfilDao
    abstract fun suche(): SucheDao
    abstract fun ordner(): OrdnerDao

    companion object {
        @Volatile private var vorhanden: Datenbank? = null

        fun hole(ctx: Context): Datenbank = vorhanden ?: synchronized(this) {
            vorhanden ?: Room.databaseBuilder(
                ctx.applicationContext,
                Datenbank::class.java,
                DATEINAME,
            ).addMigrations(WANDERUNG_1_2, WANDERUNG_2_3, WANDERUNG_3_4, WANDERUNG_4_5).build().also { vorhanden = it }
        }

        /** Die Anhänge kamen mit dem Plus-Menü dazu; alte Notizen haben schlicht keine. */
        private val WANDERUNG_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notiz ADD COLUMN anhaengeJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * Favoriten, Schutz, Papierkorb und Ordner kamen mit der Seitenleiste dazu.
         * Alte Sitzungen sind schlicht keins von beidem und liegen in keinem Ordner.
         */
        private val WANDERUNG_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sitzung ADD COLUMN favorit INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sitzung ADD COLUMN geschuetzt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sitzung ADD COLUMN geloeschtAm INTEGER")
                db.execSQL("ALTER TABLE sitzung ADD COLUMN ordnerId INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS ordner (" +
                        "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL, erstelltAm INTEGER NOT NULL)",
                )
            }
        }

        /**
         * Die Sortierung haengt jetzt am Aendern, nicht mehr am Oeffnen. Bestehende
         * Sitzungen starten mit dem Zeitpunkt, den sie bisher hatten — damit bleibt die
         * gewohnte Reihenfolge beim ersten Start nach dem Umbau erhalten.
         */
        private val WANDERUNG_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sitzung ADD COLUMN zuletztGeaendert INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE sitzung SET zuletztGeaendert = zuletztGeoeffnet")
            }
        }

        /**
         * Nachtraege bekamen ihre eigenen Zeitpunkte. Alte Notizen haben schlicht keine —
         * die Liste bleibt leer, und der Zeitstempel oben rechts bleibt der alte.
         */
        private val WANDERUNG_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notiz ADD COLUMN nachtragzeitenJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * Schliesst die Datenbank, damit ihre Datei ersetzt werden kann (F-17,
         * Wiederherstellen).
         *
         * Ohne das Schliessen schreibt Room seinen Journal-Puffer nach dem Austausch in die
         * **neue** Datei und macht sie damit unbrauchbar — der Wiederherstellungsversuch
         * zerstörte dann genau den Stand, den er retten sollte.
         */
        fun schliesse() = synchronized(this) {
            vorhanden?.close()
            vorhanden = null
        }

        /** Für die Sicherung nach Drive (F-17) — sie kopiert genau diese Datei. */
        const val DATEINAME = "gedankenspeicher.db"
    }
}

/**
 * Der Auslieferungszustand der sechs Auswertungsprofile (`01-FUNKTIONS-SPEC.md` F-10).
 *
 * Die Zahl sechs ist fest: keine Profile hinzufügbar, keine löschbar. Genau eines trägt das
 * Häkchen — beim ersten Start ist es „Normal".
 */
object Auslieferungsprofile {

    const val ANZAHL = 6

    val texte: List<Auswertungsprofil> = listOf(
        Auswertungsprofil(
            nummer = 1,
            name = "Kurz",
            anweisung = "Antworte knapp: höchstens zwei Absätze, ersatzweise eine kleine " +
                "Tabelle. Nur das Wesentliche, keine Einleitung.",
            istAktiv = false,
        ),
        Auswertungsprofil(
            nummer = 2,
            name = "Normal",
            anweisung = "Antworte in drei bis fünf Absätzen zu je 6–10 Zeilen. Ordne die " +
                "Notizen, benenne Zusammenhänge. Setze eine Tabelle oder eine Zeichnung " +
                "dort ein, wo sie mehr sagt als ein weiterer Absatz.",
            istAktiv = true,
        ),
        Auswertungsprofil(
            nummer = 3,
            name = "Ausführlich",
            anweisung = "Denke gründlich nach. Antworte in mindestens sechs Absätzen zu je " +
                "8–14 Zeilen, mit Herleitung, Gegenargumenten und konkreten nächsten " +
                "Schritten. Gliedere mit Überschriften und baue mindestens eine Übersicht " +
                "als Tabelle oder als Infografik ein.",
            istAktiv = false,
        ),
        Auswertungsprofil(nummer = 4, name = "Eigenes Profil 1", anweisung = "", istAktiv = false),
        Auswertungsprofil(nummer = 5, name = "Eigenes Profil 2", anweisung = "", istAktiv = false),
        Auswertungsprofil(nummer = 6, name = "Eigenes Profil 3", anweisung = "", istAktiv = false),
    )

    /** Der Auslieferungstext eines Profils — für den Zurücksetzen-Knopf in B-06. */
    fun vorlage(nummer: Int): Auswertungsprofil =
        texte.firstOrNull { it.nummer == nummer } ?: texte[1]
}
