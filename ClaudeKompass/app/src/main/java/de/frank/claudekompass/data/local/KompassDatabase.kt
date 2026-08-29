package de.frank.claudekompass.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        EintragEntity::class,
        ErklaerungHistorieEntity::class,
        FrageEntity::class,
        ChatSitzungEntity::class,
        ChatNachrichtEntity::class,
        AktualisierungEntity::class,
        SucheFtsEntity::class,
        SuchVerlaufEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KompassDatabase : RoomDatabase() {

    abstract fun eintragDao(): EintragDao
    abstract fun erklaerungDao(): ErklaerungDao
    abstract fun frageDao(): FrageDao
    abstract fun chatDao(): ChatDao
    abstract fun aktualisierungDao(): AktualisierungDao
    abstract fun sucheDao(): SucheDao

    companion object {
        @Volatile
        private var instanz: KompassDatabase? = null

        fun hole(context: Context): KompassDatabase = instanz ?: synchronized(this) {
            instanz ?: Room.databaseBuilder(
                context.applicationContext,
                KompassDatabase::class.java,
                "claude-kompass.db",
            )
                // Bewusst KEIN fallbackToDestructiveMigration: die selbst gestellten Fragen und
                // die eigenen Gespräche sind nicht wiederherstellbar. Kommt eine neue Version,
                // gehört hierhin eine echte Migration.
                .build()
                .also { instanz = it }
        }
    }
}

/**
 * Vereinheitlicht Text für die Suche: klein geschrieben, Umlaute aufgelöst, alles andere als
 * Trennung.
 *
 * Damit findet „Uber", „über" und „ueber" dasselbe. Wichtig: Genau diese Funktion muss beim
 * Indizieren UND beim Suchen benutzt werden — sonst passen Index und Anfrage nicht zusammen
 * und die Suche findet stillschweigend nichts.
 */
fun normalisiereFuerSuche(text: String): String {
    val ausgetauscht = StringBuilder(text.length + 8)
    for (zeichen in text.lowercase()) {
        when (zeichen) {
            'ä' -> ausgetauscht.append("ae")
            'ö' -> ausgetauscht.append("oe")
            'ü' -> ausgetauscht.append("ue")
            'ß' -> ausgetauscht.append("ss")
            'é', 'è', 'ê' -> ausgetauscht.append('e')
            'á', 'à', 'â' -> ausgetauscht.append('a')
            'í', 'ì', 'î' -> ausgetauscht.append('i')
            'ó', 'ò', 'ô' -> ausgetauscht.append('o')
            'ú', 'ù', 'û' -> ausgetauscht.append('u')
            else -> if (zeichen.isLetterOrDigit()) ausgetauscht.append(zeichen) else ausgetauscht.append(' ')
        }
    }
    return ausgetauscht.toString().split(' ').filter(String::isNotEmpty).joinToString(" ")
}

/**
 * Baut aus einer Eingabe eine FTS4-Anfrage.
 *
 * Jedes Wort bekommt ein Sternchen, damit auch Wortanfänge treffen. Leere Eingabe liefert
 * einen leeren Text — der Aufrufer sucht dann gar nicht erst, statt eine ungültige Anfrage
 * an SQLite zu schicken (die würde eine Ausnahme werfen).
 */
fun baueSuchAnfrage(eingabe: String): String {
    val woerter = normalisiereFuerSuche(eingabe).split(' ').filter(String::isNotEmpty)
    if (woerter.isEmpty()) return ""
    return woerter.joinToString(" ") { "suchtext:$it*" }
}
