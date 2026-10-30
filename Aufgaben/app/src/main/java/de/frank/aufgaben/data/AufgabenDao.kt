package de.frank.aufgaben.data

import android.content.Context
import android.net.Uri
import androidx.room.AutoMigration
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.InvalidationTracker
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface AufgabenDao {
    @Query("SELECT * FROM aufgaben ORDER BY erstellt DESC")
    fun alle(): Flow<List<Aufgabe>>

    @Query("SELECT * FROM aufgaben")
    suspend fun alleEinmal(): List<Aufgabe>

    @Query("SELECT * FROM aufgaben WHERE id = :id")
    suspend fun eine(id: Long): Aufgabe?

    /** Ein Tag; mit [mitUeberfaelligen] = 1 zusätzlich alles Offene aus vergangenen Tagen. */
    @Query("SELECT * FROM aufgaben WHERE tag = :tag OR (:mitUeberfaelligen = 1 AND tag < :tag AND erledigt = 0)")
    suspend fun fuerTag(tag: Long, mitUeberfaelligen: Int): List<Aufgabe>

    @Query("SELECT * FROM aufgaben WHERE wiederholung = 'TAEGLICH' AND erledigt = 0")
    suspend fun taeglichOffen(): List<Aufgabe>

    @Insert
    suspend fun neu(aufgabe: Aufgabe): Long

    @Update
    suspend fun aendere(aufgabe: Aufgabe)

    @Delete
    suspend fun loesche(aufgabe: Aufgabe)
}

@Database(
    entities = [Aufgabe::class],
    version = 4,
    exportSchema = true,
    // 1 → 2: Spalten vorlesen und alsWecker (beide mit Standard 0), von Room erzeugt.
    // 2 → 3: Spalten zielId, zielTitel, zielSymbol (Aufgaben aus der App „Ziele“).
    // 3 → 4: Spalte folgeVon (Verknüpfung einer Wiederholung mit ihrem Vorgänger).
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
abstract class AufgabenDatenbank : RoomDatabase() {
    abstract fun dao(): AufgabenDao

    companion object {
        @Volatile private var instanz: AufgabenDatenbank? = null

        fun get(context: Context): AufgabenDatenbank = instanz ?: synchronized(this) {
            instanz ?: Room.databaseBuilder(context.applicationContext, AufgabenDatenbank::class.java, "aufgaben.db")
                .build().also { instanz = it; jarvisMelden(context.applicationContext, it) }
        }

        // Live-Gedächtnis: Jede Änderung an der Tabelle (Oberfläche, Erinnerungen, Jarvis-Brücke) meldet
        // Jarvis ein Signal auf die Wurzel der Brücke. Nur Schreibzugriffe lösen Room-Invalidierungen aus,
        // Lesen über die Brücke also nicht. Mehrere Änderungen in Folge ergeben nach 700 ms ein Signal.
        private val jarvisScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var jarvisSignal: Job? = null
        private var jarvisBeobachter: InvalidationTracker.Observer? = null

        private fun jarvisMelden(app: Context, db: AufgabenDatenbank) {
            val uri = Uri.parse("content://${app.packageName}.jarvis")
            val beobachter = object : InvalidationTracker.Observer("aufgaben") {
                override fun onInvalidated(tables: Set<String>) {
                    synchronized(this@Companion) {
                        jarvisSignal?.cancel()
                        jarvisSignal = jarvisScope.launch {
                            delay(700)
                            runCatching { app.contentResolver.notifyChange(uri, null) }
                        }
                    }
                }
            }
            jarvisBeobachter = beobachter
            runCatching { db.invalidationTracker.addObserver(beobachter) }
        }
    }
}
