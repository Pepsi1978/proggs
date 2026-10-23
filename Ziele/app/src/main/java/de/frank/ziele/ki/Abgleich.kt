package de.frank.ziele.ki

import android.content.Context
import de.frank.ziele.bruecke.GenialeAufgaben
import de.frank.ziele.data.Vorschlag
import de.frank.ziele.data.ZieleRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Zusammenspiel mit Geniale Aufgaben: übernehmen (Plus) und den Erledigt-Stand zurückholen. Gemeinsam für App und Jarvis-Brücke. */
object Abgleich {
    private val sperre = Mutex()

    /** Übernimmt den Vorschlag als Aufgabe. Schon übernommen: liefert die vorhandene id, ohne ein Doppel anzulegen. */
    suspend fun uebernehmen(context: Context, vorschlagId: Long): Vorschlag = sperre.withLock {
        val repo = ZieleRepository.get(context)
        val v = repo.vorschlag(vorschlagId) ?: throw GenialeAufgaben.Fehler("Diesen Vorschlag gibt es nicht mehr.")
        if (v.aufgabeId != null) return v
        val ziel = repo.ziel(v.zielId) ?: throw GenialeAufgaben.Fehler("Das Ziel gibt es nicht mehr.")
        val id = GenialeAufgaben.uebernehmen(context, ziel, v)
        v.copy(aufgabeId = id, uebernommenAm = System.currentTimeMillis(), verworfen = false).also { repo.speichere(it) }
    }

    /** Holt den Stand aus Geniale Aufgaben. In der App gelöschte Aufgaben werden wieder zu offenen Vorschlägen. */
    suspend fun stand(context: Context): Boolean = sperre.withLock {
        val repo = ZieleRepository.get(context)
        val uebernommen = repo.alleVorschlaegeEinmal().filter { it.aufgabeId != null }
        if (uebernommen.isEmpty()) return true
        val stand = GenialeAufgaben.stand(context) ?: return false
        uebernommen.forEach { v ->
            val erledigt = stand[v.aufgabeId]
            when {
                erledigt == null -> repo.speichere(v.copy(aufgabeId = null, uebernommenAm = null, erledigt = false))
                erledigt != v.erledigt -> repo.speichere(v.copy(erledigt = erledigt))
            }
        }
        true
    }
}
