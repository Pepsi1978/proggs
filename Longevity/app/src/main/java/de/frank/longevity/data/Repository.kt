package de.frank.longevity.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Zugriff auf die Faktoren. Die Liste bleibt immer lückenlos nach Rang 1, 2, 3 … nummeriert und nach [ordnen] sortiert. */
class Repository private constructor(context: Context) {
    private val dao = Datenbank.get(context).faktoren()
    private val sperre = Mutex()

    val alle: Flow<List<Faktor>> = dao.alle()

    suspend fun startinhalteAnlegen() = sperre.withLock {
        if (dao.anzahl() == 0) dao.neu(ordnen(START_FAKTOREN))
    }

    suspend fun einer(id: Long) = dao.einer(id)
    suspend fun alleJetzt() = dao.alleJetzt()
    suspend fun liste() = dao.alleJetzt().filter { !it.vorschlag }

    suspend fun speichere(f: Faktor) = sperre.withLock {
        val alt = dao.einer(f.id)
        dao.speichere(f.copy(geaendertAm = System.currentTimeMillis()))
        // Hat sich die Wirkung eines Lebenszeit-Räubers geändert (oder das Vorzeichen), rutscht er an seinen neuen Platz.
        if (!f.vorschlag && alt != null && alt.jahre != f.jahre && (alt.raeuber || f.raeuber)) {
            dao.speichere(ordnen(dao.alleJetzt().filter { !it.vorschlag }))
        }
    }

    /**
     * Einmalig beim Update: frühere Verbots-Faktoren („Nicht rauchen +10 J.“) werden zu Lebenszeit-Räubern
     * mit Minus-Jahren („Rauchen −10 J.“) und rutschen unter die Null-Linie. Häkchen, Ziel-Status und
     * eigene Notizen bleiben erhalten; vertiefte Erklärungen der KI auch.
     */
    suspend fun verboteUmstellen() = sperre.withLock {
        val raeuber = START_FAKTOREN.filter { it.raeuber }
        dao.alleJetzt().filter { !it.vorschlag && !it.raeuber }.forEach { alt ->
            val t = alt.titel.lowercase()
            val neu = VERBOTE.firstOrNull { (woerter, _) -> woerter.any { it in t } }
                ?.let { (_, anfang) -> raeuber.firstOrNull { it.titel.startsWith(anfang) } } ?: return@forEach
            dao.speichere(
                alt.copy(
                    titel = neu.titel, kurz = neu.kurz, jahre = neu.jahre, begruendung = neu.begruendung,
                    erklaerung = if (alt.vertieft) alt.erklaerung else neu.erklaerung,
                ),
            )
        }
        dao.speichere(ordnen(dao.alleJetzt().filter { !it.vorschlag }).map { it.copy(vorherRang = null) })
    }

    /** Fügt [f] an Rang [rang] ein und schiebt alles darunter eins nach unten. */
    suspend fun einfuegen(f: Faktor, rang: Int): Long = sperre.withLock {
        val liste = dao.alleJetzt().filter { !it.vorschlag }.toMutableList()
        val pos = (rang - 1).coerceIn(0, liste.size)
        val id = dao.neu(f.copy(rang = pos + 1, vorschlag = false))
        val neu = dao.einer(id)!!
        liste.add(pos, neu)
        dao.speichere(ordnen(liste))
        id
    }

    /** Nimmt einen Vorschlag der KI in die Liste auf (an seinem vorgeschlagenen Rang). */
    suspend fun vorschlagAnnehmen(f: Faktor) = sperre.withLock {
        val liste = dao.alleJetzt().filter { !it.vorschlag && it.id != f.id }.toMutableList()
        val pos = (f.rang - 1).coerceIn(0, liste.size)
        liste.add(pos, f.copy(vorschlag = false, neu = true))
        dao.speichere(ordnen(liste))
    }

    suspend fun loesche(f: Faktor) = sperre.withLock {
        dao.loesche(f)
        if (!f.vorschlag) dao.speichere(ordnen(dao.alleJetzt().filter { !it.vorschlag }))
    }

    suspend fun wiederherstellen(f: Faktor) {
        if (f.vorschlag) dao.neu(f.copy(id = 0)) else einfuegen(f.copy(id = 0), f.rang)
    }

    /** Verschiebt einen Faktor von Hand an einen neuen Rang – nie über die Null-Linie (Räuber ordnen sich nach Jahren). */
    suspend fun verschiebe(f: Faktor, neuerRang: Int) = sperre.withLock {
        val liste = dao.alleJetzt().filter { !it.vorschlag }.toMutableList()
        val alt = liste.indexOfFirst { it.id == f.id }
        if (alt < 0) return@withLock
        val x = liste.removeAt(alt)
        liste.add((neuerRang - 1).coerceIn(0, liste.size), x)
        dao.speichere(ordnen(liste))
    }

    /** Übernimmt eine komplette neue Reihenfolge (bereits gemischt mit dem Altbestand). */
    suspend fun uebernimm(liste: List<Faktor>, vorschlaege: List<Faktor>, alteVorschlaegeBehalten: Boolean = false) = sperre.withLock {
        dao.ersetzeAlle(ordnen(liste))
        if (!alteVorschlaegeBehalten) dao.alleJetzt().filter { it.vorschlag }.forEach { dao.loesche(it) }
        if (vorschlaege.isNotEmpty()) dao.neu(vorschlaege.map { it.copy(id = 0, vorschlag = true) })
    }

    companion object {
        /** Stichwörter alter Verbots-Titel → Anfang des neuen Räuber-Titels in den Startinhalten. */
        private val VERBOTE = listOf(
            listOf("rauch", "tabak") to "Rauchen",
            listOf("alkohol") to "Regelmäßig Alkohol",
            listOf("umweltgift", "saubere luft", "feinstaub") to "Feinstaub",
        )

        @Volatile private var instanz: Repository? = null
        fun get(context: Context): Repository = instanz ?: synchronized(this) {
            instanz ?: Repository(context).also { instanz = it }
        }
    }
}
