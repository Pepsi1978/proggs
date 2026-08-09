package de.frank.experimente.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SelbstbildDao {
    @Query("SELECT * FROM selbstbild WHERE id = 1")
    fun beobachte(): Flow<SelbstbildSatz?>

    @Query("SELECT * FROM selbstbild WHERE id = 1")
    suspend fun lies(): SelbstbildSatz?

    @Upsert
    suspend fun schreib(satz: SelbstbildSatz)
}

@Dao
interface ZielDao {
    @Query("SELECT * FROM ziel ORDER BY angelegtAm DESC")
    fun beobachte(): Flow<List<Ziel>>

    @Query("SELECT * FROM ziel ORDER BY angelegtAm DESC")
    suspend fun alle(): List<Ziel>

    @Upsert
    suspend fun schreib(ziel: Ziel): Long

    @Delete
    suspend fun loesche(ziel: Ziel)
}

@Dao
interface LogDao {
    @Query("SELECT * FROM logtag WHERE ausfuehrlich IS NOT NULL ORDER BY datum DESC")
    fun beobachteAusfuehrlich(): Flow<List<LogTag>>

    @Query("SELECT * FROM logtag WHERE verdichtet IS NOT NULL ORDER BY datum DESC")
    fun beobachteLangzeit(): Flow<List<LogTag>>

    @Query("SELECT * FROM logtag WHERE ausfuehrlich IS NOT NULL ORDER BY datum DESC")
    suspend fun alleAusfuehrlich(): List<LogTag>

    @Query("SELECT * FROM logtag WHERE verdichtet IS NOT NULL ORDER BY datum DESC")
    suspend fun alleLangzeit(): List<LogTag>

    @Query("SELECT * FROM logtag WHERE datum = :datum")
    suspend fun lies(datum: LocalDate): LogTag?

    /** Tage, die älter als [grenze] sind und noch ausführlich vorliegen (F-15). */
    @Query("SELECT * FROM logtag WHERE ausfuehrlich IS NOT NULL AND datum < :grenze ORDER BY datum")
    suspend fun zuVerdichten(grenze: LocalDate): List<LogTag>

    @Upsert
    suspend fun schreib(tag: LogTag)

    @Delete
    suspend fun loesche(tag: LogTag)
}

@Dao
interface LageDao {
    @Query("SELECT * FROM lage WHERE datum = :datum")
    fun beobachte(datum: LocalDate): Flow<Lage?>

    @Query("SELECT * FROM lage WHERE datum = :datum")
    suspend fun lies(datum: LocalDate): Lage?

    @Upsert
    suspend fun schreib(lage: Lage)
}

@Dao
interface VorschlagDao {
    @Query("SELECT * FROM vorschlag WHERE datum = :datum AND verworfenAm IS NULL ORDER BY id")
    fun beobachteHeute(datum: LocalDate): Flow<List<Vorschlag>>

    @Query("SELECT * FROM vorschlag WHERE datum = :datum ORDER BY id")
    suspend fun alleDesTages(datum: LocalDate): List<Vorschlag>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun schreibAlle(vorschlaege: List<Vorschlag>)

    /** Beim Aktualisieren (F-04): die gezeigten fünf als verworfen markieren. */
    @Query("UPDATE vorschlag SET verworfenAm = :zeitpunkt WHERE datum = :datum AND verworfenAm IS NULL")
    suspend fun verwirfOffene(datum: LocalDate, zeitpunkt: java.time.Instant)

    @Query("DELETE FROM vorschlag WHERE datum < :grenze")
    suspend fun raeumeAlteAuf(grenze: LocalDate)
}

@Dao
interface ExperimentDao {
    @Query("SELECT * FROM experiment WHERE zustand = 'OFFEN' ORDER BY begonnenAm, id")
    fun beobachteOffene(): Flow<List<Experiment>>

    @Query("SELECT * FROM experiment WHERE zustand = 'OFFEN' ORDER BY begonnenAm, id")
    suspend fun offene(): List<Experiment>

    @Query("SELECT COUNT(*) FROM experiment WHERE zustand = 'OFFEN'")
    suspend fun anzahlOffene(): Int

    @Query("SELECT * FROM experiment WHERE id = :id")
    suspend fun lies(id: Long): Experiment?

    @Query("SELECT * FROM experiment WHERE id = :id")
    fun beobachte(id: Long): Flow<Experiment?>

    @Query("SELECT * FROM experiment ORDER BY begonnenAm DESC, id DESC")
    suspend fun alle(): List<Experiment>

    @Insert
    suspend fun schreib(experiment: Experiment): Long

    @Update
    suspend fun aktualisiere(experiment: Experiment)
}

@Dao
interface AufgabeDao {
    @Query("SELECT * FROM aufgabe WHERE experimentId = :experimentId AND tagNummer = :tagNummer ORDER BY reihenfolge")
    fun beobachteTag(experimentId: Long, tagNummer: Int): Flow<List<Aufgabe>>

    @Query("SELECT * FROM aufgabe WHERE experimentId = :experimentId AND tagNummer = :tagNummer ORDER BY reihenfolge")
    suspend fun desTages(experimentId: Long, tagNummer: Int): List<Aufgabe>

    @Query("SELECT * FROM aufgabe WHERE experimentId = :experimentId ORDER BY tagNummer, reihenfolge")
    fun beobachteAlle(experimentId: Long): Flow<List<Aufgabe>>

    @Insert
    suspend fun schreibAlle(aufgaben: List<Aufgabe>)

    @Update
    suspend fun aktualisiere(aufgabe: Aufgabe)
}

@Dao
interface AuswertungDao {
    @Query("SELECT * FROM auswertung WHERE experimentId = :experimentId ORDER BY datum")
    fun beobachte(experimentId: Long): Flow<List<Auswertung>>

    @Query("SELECT * FROM auswertung WHERE experimentId = :experimentId ORDER BY datum")
    suspend fun zumExperiment(experimentId: Long): List<Auswertung>

    @Query("SELECT * FROM auswertung WHERE experimentId = :experimentId AND datum = :datum")
    suspend fun desTages(experimentId: Long, datum: LocalDate): Auswertung?

    @Query("SELECT * FROM auswertung ORDER BY datum DESC")
    suspend fun alle(): List<Auswertung>

    @Upsert
    suspend fun schreib(auswertung: Auswertung): Long

    @Update
    suspend fun aktualisiere(auswertung: Auswertung)
}

@Dao
interface GespraechDao {
    @Query("SELECT * FROM gespraech WHERE experimentId = :experimentId ORDER BY angelegtAm, id")
    fun beobachte(experimentId: Long): Flow<List<GespraechsRunde>>

    @Query("SELECT * FROM gespraech WHERE experimentId = :experimentId ORDER BY angelegtAm, id")
    suspend fun zumExperiment(experimentId: Long): List<GespraechsRunde>

    @Insert
    suspend fun schreib(runde: GespraechsRunde): Long
}

@Dao
interface MerklisteDao {
    @Query("SELECT * FROM merkliste ORDER BY angelegtAm DESC")
    fun beobachte(): Flow<List<MerkEintrag>>

    @Query("SELECT * FROM merkliste ORDER BY angelegtAm")
    suspend fun alle(): List<MerkEintrag>

    @Query("SELECT COUNT(*) FROM merkliste")
    suspend fun anzahl(): Int

    @Query("SELECT * FROM merkliste WHERE titel = :titel LIMIT 1")
    suspend fun mitTitel(titel: String): MerkEintrag?

    @Insert
    suspend fun schreib(eintrag: MerkEintrag): Long

    @Delete
    suspend fun loesche(eintrag: MerkEintrag)
}

@Dao
interface ErkenntnisDao {
    @Query("SELECT * FROM erkenntnis ORDER BY geaendertAm DESC")
    fun beobachte(): Flow<List<Erkenntnis>>

    @Query("SELECT * FROM erkenntnis ORDER BY geaendertAm DESC")
    suspend fun alle(): List<Erkenntnis>

    @Upsert
    suspend fun schreib(erkenntnis: Erkenntnis)

    @Query("DELETE FROM erkenntnis")
    suspend fun leere()
}
