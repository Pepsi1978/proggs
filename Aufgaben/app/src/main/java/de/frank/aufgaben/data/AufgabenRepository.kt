package de.frank.aufgaben.data

import android.content.Context
import android.content.Intent
import de.frank.aufgaben.erinnerung.Planer
import de.frank.aufgaben.widget.HeuteWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Einziger Schreibweg. Nach jeder Änderung ziehen Erinnerungen, Widget und die Wecker-Brücke nach —
 * so kann keine Stelle der App das vergessen.
 */
class AufgabenRepository private constructor(context: Context) {
    private val app = context.applicationContext
    private val dao = AufgabenDatenbank.get(app).dao()
    private val nebenbei = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val alle: Flow<List<Aufgabe>> = dao.alle()

    suspend fun eine(id: Long): Aufgabe? = dao.eine(id)
    suspend fun alleEinmal(): List<Aufgabe> = dao.alleEinmal()
    suspend fun fuerTag(tag: Long, mitUeberfaelligen: Boolean) = dao.fuerTag(tag, if (mitUeberfaelligen) 1 else 0)

    suspend fun neu(aufgabe: Aufgabe): Long {
        val id = dao.neu(aufgabe)
        geaendert()
        return id
    }

    suspend fun speichere(aufgabe: Aufgabe) {
        dao.aendere(aufgabe.copy(geaendert = System.currentTimeMillis()))
        geaendert()
    }

    suspend fun loesche(aufgabe: Aufgabe) {
        Planer.storniere(app, aufgabe.id)
        dao.loesche(aufgabe)
        geaendert()
    }

    /** Erledigt oder wieder offen. Bei Wiederholungen entsteht beim Erledigen der nächste Termin. */
    suspend fun setzeErledigt(aufgabe: Aufgabe, erledigt: Boolean): Aufgabe? {
        dao.aendere(aufgabe.copy(erledigt = erledigt, erledigtAm = if (erledigt) System.currentTimeMillis() else null, geaendert = System.currentTimeMillis()))
        var folge: Aufgabe? = null
        if (erledigt && aufgabe.wdh != Wiederholung.KEINE) {
            val basis = aufgabe.tag ?: Tage.heute()
            Tage.naechster(maxOf(basis, Tage.heute() - 1), aufgabe.wdh)?.let { naechster ->
                val neu = aufgabe.copy(
                    id = 0, tag = naechster, erledigt = false, erledigtAm = null,
                    erstellt = System.currentTimeMillis(), geaendert = System.currentTimeMillis(),
                    schritteJson = Aufgabe.schritteAlsJson(aufgabe.schritte.map { it.copy(erledigt = false) }),
                )
                folge = neu.copy(id = dao.neu(neu))
            }
        }
        geaendert()
        return folge
    }

    /** Stellt eine gelöschte Aufgabe mit derselben ID wieder her (Rückgängig). */
    suspend fun wiederherstellen(aufgabe: Aufgabe) {
        dao.neu(aufgabe)
        geaendert()
    }

    private fun geaendert() {
        nebenbei.launch {
            runCatching { Planer.planeAlle(app) }
            runCatching { HeuteWidget.aktualisiere(app) }
            runCatching {
                // Die Wecker-App hört darauf und liest die Aufgaben neu (siehe README, Brücke).
                app.sendBroadcast(Intent(AKTION_GEAENDERT).setPackage("de.frank.genialerwecker"))
            }
        }
    }

    companion object {
        const val AKTION_GEAENDERT = "de.frank.aufgaben.AUFGABEN_GEAENDERT"
        @Volatile private var instanz: AufgabenRepository? = null
        fun get(context: Context): AufgabenRepository = instanz ?: synchronized(this) {
            instanz ?: AufgabenRepository(context).also { instanz = it }
        }
    }
}
