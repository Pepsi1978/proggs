package de.frank.stacklabor.daten

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.frank.stacklabor.daten.dao.BewertungDao
import de.frank.stacklabor.daten.dao.FrageDao
import de.frank.stacklabor.daten.dao.MittelDao
import de.frank.stacklabor.daten.dao.StackDao
import de.frank.stacklabor.daten.dao.ZielDao
import de.frank.stacklabor.daten.entitaeten.BewertungE
import de.frank.stacklabor.daten.entitaeten.BewertungszelleE
import de.frank.stacklabor.daten.entitaeten.EigeneFrageE
import de.frank.stacklabor.daten.entitaeten.FrageAntwortE
import de.frank.stacklabor.daten.entitaeten.KonkurrenzE
import de.frank.stacklabor.daten.entitaeten.MittelE
import de.frank.stacklabor.daten.entitaeten.StackE
import de.frank.stacklabor.daten.entitaeten.StackEintragE
import de.frank.stacklabor.daten.entitaeten.StackZielE
import de.frank.stacklabor.daten.entitaeten.Wandler
import de.frank.stacklabor.daten.entitaeten.ZielE

/**
 * Die lokale Datenbank der App.
 *
 * Bewusst ohne `fallbackToDestructiveMigration`: Bestand darf bei einem Schema-Wechsel nie
 * stillschweigend verloren gehen — jede kuenftige Fassung bekommt eine echte Wanderung.
 */
@Database(
    entities = [
        MittelE::class,
        StackE::class,
        StackEintragE::class,
        ZielE::class,
        StackZielE::class,
        EigeneFrageE::class,
        BewertungE::class,
        BewertungszelleE::class,
        KonkurrenzE::class,
        FrageAntwortE::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Wandler::class)
abstract class StackLaborDatenbank : RoomDatabase() {

    abstract fun stackDao(): StackDao

    abstract fun mittelDao(): MittelDao

    abstract fun zielDao(): ZielDao

    abstract fun bewertungDao(): BewertungDao

    abstract fun frageDao(): FrageDao

    companion object {
        private const val DATEINAME = "stacklabor.db"

        @Volatile
        private var einzelstueck: StackLaborDatenbank? = null

        /** Liefert die eine Instanz der Datenbank; beim ersten Aufruf wird sie gebaut. */
        fun erzeuge(context: Context): StackLaborDatenbank =
            einzelstueck ?: synchronized(this) {
                einzelstueck ?: bauen(context.applicationContext).also { einzelstueck = it }
            }

        private fun bauen(context: Context): StackLaborDatenbank =
            Room.databaseBuilder(context, StackLaborDatenbank::class.java, DATEINAME)
                // Lesen und Schreiben behindern sich nicht: die Listen bleiben fluessig,
                // waehrend ein Auswertungslauf im Hintergrund schreibt (F-12, Abschnitt 6).
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}
