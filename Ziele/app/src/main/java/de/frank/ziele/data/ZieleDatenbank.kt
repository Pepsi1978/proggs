package de.frank.ziele.data

import android.content.Context
import android.net.Uri
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ZieleDao {
    @Query("SELECT * FROM ziele ORDER BY rang, erstellt")
    fun alleZiele(): Flow<List<Ziel>>

    @Query("SELECT * FROM ziele ORDER BY rang, erstellt")
    suspend fun alleZieleEinmal(): List<Ziel>

    @Query("SELECT * FROM ziele WHERE id = :id")
    suspend fun ziel(id: Long): Ziel?

    @Insert
    suspend fun neu(z: Ziel): Long

    @Update
    suspend fun speichere(z: Ziel)

    @Delete
    suspend fun loesche(z: Ziel)

    @Query("SELECT * FROM vorschlaege ORDER BY zielId, id")
    fun alleVorschlaege(): Flow<List<Vorschlag>>

    @Query("SELECT * FROM vorschlaege WHERE zielId = :zielId ORDER BY id")
    suspend fun vorschlaege(zielId: Long): List<Vorschlag>

    @Query("SELECT * FROM vorschlaege ORDER BY zielId, id")
    suspend fun alleVorschlaegeEinmal(): List<Vorschlag>

    @Query("SELECT * FROM vorschlaege WHERE id = :id")
    suspend fun vorschlag(id: Long): Vorschlag?

    @Insert
    suspend fun neueVorschlaege(v: List<Vorschlag>)

    @Insert
    suspend fun neuerVorschlag(v: Vorschlag): Long

    @Update
    suspend fun speichere(v: Vorschlag)

    @Query("DELETE FROM vorschlaege WHERE zielId = :zielId")
    suspend fun vorschlaegeLoeschen(zielId: Long)

    @Query("DELETE FROM vorschlaege WHERE zielId = :zielId AND aufgabeId IS NULL AND verworfen = 0")
    suspend fun offeneVorschlaegeLoeschen(zielId: Long)

    /** Neue Auswertung: übernommene und verworfene Vorschläge bleiben, die übrigen werden ersetzt. */
    @Transaction
    suspend fun vorschlaegeErsetzen(zielId: Long, neu: List<Vorschlag>) {
        offeneVorschlaegeLoeschen(zielId)
        neueVorschlaege(neu)
    }

    @Query("DELETE FROM vorschlaege WHERE id = :id")
    suspend fun vorschlagLoeschen(id: Long)

    // ---- Abgelehnt ----
    @Query("SELECT * FROM abgelehnt ORDER BY zielId, id")
    fun alleAbgelehnt(): Flow<List<Abgelehnt>>

    @Query("SELECT * FROM abgelehnt WHERE zielId = :zielId ORDER BY id")
    suspend fun abgelehnt(zielId: Long): List<Abgelehnt>

    @Insert
    suspend fun neuAbgelehnt(a: List<Abgelehnt>)

    @Query("DELETE FROM abgelehnt WHERE id = :id")
    suspend fun abgelehntLoeschen(id: Long)

    @Query("DELETE FROM abgelehnt WHERE zielId = :zielId")
    suspend fun abgelehnteLoeschen(zielId: Long)

    /** Ablehnen: der Vorschlag wandert in die Sperrliste. */
    @Transaction
    suspend fun ablehnen(v: Vorschlag) {
        neuAbgelehnt(listOf(Abgelehnt(zielId = v.zielId, schluessel = v.schluessel, titel = v.titel, text = v.text)))
        vorschlagLoeschen(v.id)
    }

    // ---- Gespräch zur Auswertung ----
    @Query("SELECT * FROM rueckmeldungen ORDER BY zielId, id")
    fun alleRueckmeldungen(): Flow<List<Rueckmeldung>>

    @Query("SELECT * FROM rueckmeldungen WHERE zielId = :zielId ORDER BY id")
    suspend fun rueckmeldungen(zielId: Long): List<Rueckmeldung>

    @Insert
    suspend fun neueRueckmeldungen(r: List<Rueckmeldung>)

    @Query("DELETE FROM rueckmeldungen WHERE zielId = :zielId")
    suspend fun rueckmeldungenLoeschen(zielId: Long)
}

@Database(entities = [Ziel::class, Vorschlag::class, Abgelehnt::class, Rueckmeldung::class], version = 2, exportSchema = false)
abstract class ZieleDatenbank : RoomDatabase() {
    abstract fun dao(): ZieleDao
}

/** Version 2: Sperrliste der abgelehnten Vorschläge und das Gespräch zur Auswertung. Bestehende Tabellen bleiben unberührt. */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `abgelehnt` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `zielId` INTEGER NOT NULL, `schluessel` TEXT NOT NULL, `titel` TEXT NOT NULL, `text` TEXT NOT NULL, `erstellt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_abgelehnt_zielId` ON `abgelehnt` (`zielId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `rueckmeldungen` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `zielId` INTEGER NOT NULL, `vonFrank` INTEGER NOT NULL, `text` TEXT NOT NULL, `erstellt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rueckmeldungen_zielId` ON `rueckmeldungen` (`zielId`)")
    }
}

/** Einziger Zugang zu den Daten. Jede Änderung meldet Jarvis und dem Wecker ein Signal, damit sie frisch lesen. */
class ZieleRepository private constructor(private val app: Context) {
    private val dao = Room.databaseBuilder(app, ZieleDatenbank::class.java, "ziele.db").addMigrations(MIGRATION_1_2).build().dao()

    val ziele: Flow<List<Ziel>> = dao.alleZiele()
    val vorschlaege: Flow<List<Vorschlag>> = dao.alleVorschlaege()
    val abgelehnt: Flow<List<Abgelehnt>> = dao.alleAbgelehnt()
    val rueckmeldungen: Flow<List<Rueckmeldung>> = dao.alleRueckmeldungen()

    suspend fun zieleEinmal() = dao.alleZieleEinmal()
    suspend fun ziel(id: Long) = dao.ziel(id)
    suspend fun vorschlaege(zielId: Long) = dao.vorschlaege(zielId)
    suspend fun alleVorschlaegeEinmal() = dao.alleVorschlaegeEinmal()
    suspend fun vorschlag(id: Long) = dao.vorschlag(id)
    suspend fun abgelehnt(zielId: Long) = dao.abgelehnt(zielId)
    suspend fun rueckmeldungen(zielId: Long) = dao.rueckmeldungen(zielId)

    suspend fun ablehnen(v: Vorschlag) { dao.ablehnen(v); signal() }

    /** Rückgängig nach dem Ablehnen: Sperreintrag weg, der Vorschlag kommt wie er war zurück. */
    suspend fun ablehnenZuruecknehmen(v: Vorschlag) {
        dao.abgelehnt(v.zielId).lastOrNull { it.schluessel == v.schluessel && it.titel == v.titel }?.let { dao.abgelehntLoeschen(it.id) }
        if (dao.ziel(v.zielId) != null && dao.vorschlag(v.id) == null) dao.neuerVorschlag(v)
        signal()
    }

    /** Hebt eine Ablehnung auf: Das Thema darf wieder vorgeschlagen werden. */
    suspend fun ablehnungAufheben(a: Abgelehnt) { dao.abgelehntLoeschen(a.id); signal() }

    suspend fun neueVorschlaege(v: List<Vorschlag>) { dao.neueVorschlaege(v); signal() }

    suspend fun neueRueckmeldung(r: Rueckmeldung) { dao.neueRueckmeldungen(listOf(r)); signal() }

    suspend fun neu(z: Ziel): Long {
        val rang = (dao.alleZieleEinmal().minOfOrNull { it.rang } ?: 1) - 1
        return dao.neu(z.copy(rang = rang)).also { signal() }
    }

    suspend fun speichere(z: Ziel) { dao.speichere(z.copy(geaendert = System.currentTimeMillis())); signal() }

    suspend fun loesche(z: Ziel) {
        dao.vorschlaegeLoeschen(z.id); dao.abgelehnteLoeschen(z.id); dao.rueckmeldungenLoeschen(z.id); dao.loesche(z); signal()
    }

    suspend fun wiederherstellen(z: Ziel, v: List<Vorschlag>, abgelehnt: List<Abgelehnt> = emptyList(), gespraech: List<Rueckmeldung> = emptyList()) {
        dao.neu(z); dao.neueVorschlaege(v); dao.neuAbgelehnt(abgelehnt); dao.neueRueckmeldungen(gespraech); signal()
    }

    suspend fun speichere(v: Vorschlag) { dao.speichere(v); signal() }

    suspend fun neuerVorschlag(v: Vorschlag): Long = dao.neuerVorschlag(v).also { signal() }

    suspend fun vorschlaegeErsetzen(zielId: Long, neu: List<Vorschlag>) { dao.vorschlaegeErsetzen(zielId, neu); signal() }

    suspend fun reihenfolge(ids: List<Long>) {
        val alle = dao.alleZieleEinmal().associateBy { it.id }
        ids.forEachIndexed { i, id -> alle[id]?.let { if (it.rang != i) dao.speichere(it.copy(rang = i)) } }
        signal()
    }

    private fun signal() {
        runCatching { app.contentResolver.notifyChange(Uri.parse("content://${app.packageName}.jarvis"), null) }
        runCatching { app.contentResolver.notifyChange(Uri.parse("content://${app.packageName}.wecker"), null) }
    }

    companion object {
        @Volatile private var instanz: ZieleRepository? = null
        fun get(context: Context): ZieleRepository = instanz ?: synchronized(this) {
            instanz ?: ZieleRepository(context.applicationContext).also { instanz = it }
        }
    }
}
