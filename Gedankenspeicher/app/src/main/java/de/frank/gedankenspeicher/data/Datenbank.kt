package de.frank.gedankenspeicher.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Wandler {
    @TypeConverter fun zustandHin(v: Notizzustand): String = v.name
    @TypeConverter fun zustandHer(v: String): Notizzustand =
        runCatching { Notizzustand.valueOf(v) }.getOrDefault(Notizzustand.FERTIG)

    @TypeConverter fun quelleHin(v: Notizquelle): String = v.name
    @TypeConverter fun quelleHer(v: String): Notizquelle =
        runCatching { Notizquelle.valueOf(v) }.getOrDefault(Notizquelle.GETIPPT)
}

@Database(
    entities = [Sitzung::class, Notiz::class, KiAntwort::class, Auswertungsprofil::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Wandler::class)
abstract class Datenbank : RoomDatabase() {
    abstract fun sitzungen(): SitzungDao
    abstract fun notizen(): NotizDao
    abstract fun antworten(): KiAntwortDao
    abstract fun profile(): ProfilDao
    abstract fun suche(): SucheDao

    companion object {
        @Volatile private var vorhanden: Datenbank? = null

        fun hole(ctx: Context): Datenbank = vorhanden ?: synchronized(this) {
            vorhanden ?: Room.databaseBuilder(
                ctx.applicationContext,
                Datenbank::class.java,
                DATEINAME,
            ).build().also { vorhanden = it }
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
            anweisung = "Antworte in höchstens zwei Absätzen. Nur das Wesentliche, keine Einleitung.",
            istAktiv = false,
        ),
        Auswertungsprofil(
            nummer = 2,
            name = "Normal",
            anweisung = "Antworte in drei bis fünf Absätzen zu je 6–10 Zeilen. Ordne die Notizen, " +
                "benenne Zusammenhänge.",
            istAktiv = true,
        ),
        Auswertungsprofil(
            nummer = 3,
            name = "Ausführlich",
            anweisung = "Denke gründlich nach. Antworte in mindestens sechs Absätzen zu je 8–15 " +
                "Zeilen, mit Herleitung, Gegenargumenten und konkreten nächsten Schritten.",
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
