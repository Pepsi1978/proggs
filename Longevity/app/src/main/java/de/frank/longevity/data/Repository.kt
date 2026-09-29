package de.frank.longevity.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Zugriff auf die Faktoren. Die Liste bleibt immer lückenlos nach Rang 1, 2, 3 … nummeriert. */
class Repository private constructor(context: Context) {
    private val dao = Datenbank.get(context).faktoren()
    private val sperre = Mutex()

    val alle: Flow<List<Faktor>> = dao.alle()

    suspend fun startinhalteAnlegen() = sperre.withLock {
        if (dao.anzahl() == 0) dao.neu(START_FAKTOREN.mapIndexed { i, f -> f.copy(rang = i + 1) })
    }

    suspend fun einer(id: Long) = dao.einer(id)
    suspend fun alleJetzt() = dao.alleJetzt()
    suspend fun liste() = dao.alleJetzt().filter { !it.vorschlag }

    suspend fun speichere(f: Faktor) = dao.speichere(f.copy(geaendertAm = System.currentTimeMillis()))

    /** Fügt [f] an Rang [rang] ein und schiebt alles darunter eins nach unten. */
    suspend fun einfuegen(f: Faktor, rang: Int): Long = sperre.withLock {
        val liste = dao.alleJetzt().filter { !it.vorschlag }.toMutableList()
        val pos = (rang - 1).coerceIn(0, liste.size)
        val id = dao.neu(f.copy(rang = pos + 1, vorschlag = false))
        val neu = dao.einer(id)!!
        liste.add(pos, neu)
        dao.speichere(liste.mapIndexed { i, x -> x.copy(rang = i + 1) })
        id
    }

    /** Nimmt einen Vorschlag der KI in die Liste auf (an seinem vorgeschlagenen Rang). */
    suspend fun vorschlagAnnehmen(f: Faktor) = sperre.withLock {
        val liste = dao.alleJetzt().filter { !it.vorschlag && it.id != f.id }.toMutableList()
        val pos = (f.rang - 1).coerceIn(0, liste.size)
        liste.add(pos, f.copy(vorschlag = false, neu = true))
        dao.speichere(liste.mapIndexed { i, x -> x.copy(rang = i + 1) })
    }

    suspend fun loesche(f: Faktor) = sperre.withLock {
        dao.loesche(f)
        if (!f.vorschlag) dao.speichere(dao.alleJetzt().filter { !it.vorschlag }.mapIndexed { i, x -> x.copy(rang = i + 1) })
    }

    suspend fun wiederherstellen(f: Faktor) {
        if (f.vorschlag) dao.neu(f.copy(id = 0)) else einfuegen(f.copy(id = 0), f.rang)
    }

    /** Verschiebt einen Faktor von Hand an einen neuen Rang. */
    suspend fun verschiebe(f: Faktor, neuerRang: Int) = sperre.withLock {
        val liste = dao.alleJetzt().filter { !it.vorschlag }.toMutableList()
        val alt = liste.indexOfFirst { it.id == f.id }
        if (alt < 0) return@withLock
        val x = liste.removeAt(alt)
        liste.add((neuerRang - 1).coerceIn(0, liste.size), x)
        dao.speichere(liste.mapIndexed { i, y -> y.copy(rang = i + 1) })
    }

    /** Übernimmt eine komplette neue Reihenfolge (bereits gemischt mit dem Altbestand). */
    suspend fun uebernimm(liste: List<Faktor>, vorschlaege: List<Faktor>) = sperre.withLock {
        dao.ersetzeAlle(liste)
        dao.alleJetzt().filter { it.vorschlag }.forEach { dao.loesche(it) }
        if (vorschlaege.isNotEmpty()) dao.neu(vorschlaege.map { it.copy(id = 0, vorschlag = true) })
    }

    companion object {
        @Volatile private var instanz: Repository? = null
        fun get(context: Context): Repository = instanz ?: synchronized(this) {
            instanz ?: Repository(context).also { instanz = it }
        }
    }
}
