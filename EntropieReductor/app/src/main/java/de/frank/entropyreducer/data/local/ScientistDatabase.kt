package de.frank.entropyreducer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.frank.entropyreducer.data.local.dao.HypothesisDao
import de.frank.entropyreducer.data.local.dao.HypothesisMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistMessageDao
import de.frank.entropyreducer.data.local.dao.ScientistSessionDao
import de.frank.entropyreducer.data.local.entities.HypothesisEntity
import de.frank.entropyreducer.data.local.entities.HypothesisMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistMessageEntity
import de.frank.entropyreducer.data.local.entities.ScientistSessionEntity

/**
 * Frank-Wunsch 2026-05-09: Forscher-Daten leben in einer EIGENEN Datenbank-Datei,
 * unabhaengig von der Haupt-AppDatabase. So koennen Sessions, Hypothesen und
 * Diskussions-Verlaeufe ueber App-Updates persistent bleiben — auch wenn die
 * Haupt-DB durch Schema-Aenderungen destruktiv resettet wird.
 *
 * Hier nur die rein-Forscher-spezifischen Tabellen. Insights und Memory bleiben
 * absichtlich in AppDatabase, weil sie cross-cutting genutzt werden (Briefing,
 * Repertoire, Aufgaben-Antworten) und bei einem destruktiven Reset von Memory/
 * Insights kommen sie sowieso ueber Gemini wieder zurueck. Sessions/Hypothesen
 * dagegen sind Frank's eigentliches Tagebuch — die muessen ueberleben.
 *
 * Schema-Stabilitaet ist hier hoechste Prioritaet: KEIN destructive fallback in
 * DatabaseModule fuer diese DB. Wenn das Schema sich aendert, MUSS eine echte
 * Migration geschrieben werden — sonst crasht die App und ich werde gezwungen
 * sauber zu arbeiten (Poka-Yoke Stufe 3).
 */
@Database(
    entities = [
        ScientistSessionEntity::class,
        ScientistMessageEntity::class,
        HypothesisEntity::class,
        HypothesisMessageEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(EntropyTypeConverters::class)
abstract class ScientistDatabase : RoomDatabase() {
    abstract fun scientistSessionDao(): ScientistSessionDao
    abstract fun scientistMessageDao(): ScientistMessageDao
    abstract fun hypothesisDao(): HypothesisDao
    abstract fun hypothesisMessageDao(): HypothesisMessageDao

    companion object {
        const val DB_NAME = "entropy_reducer_scientist.db"
    }
}
