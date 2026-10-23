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

    /** Schreibt Vorschläge unter derselben Sperre wie das Übernehmen: Ein Plus während des Speicherns einer Auswertung verliert so nie die Verknüpfung. */
    suspend fun <T> gesperrt(block: suspend () -> T): T = sperre.withLock { block() }

    /** Übernimmt den Vorschlag als Aufgabe. Schon übernommen: liefert die vorhandene id, ohne ein Doppel anzulegen. */
    suspend fun uebernehmen(context: Context, vorschlagId: Long): Vorschlag = sperre.withLock {
        val repo = ZieleRepository.get(context)
        val v = repo.vorschlag(vorschlagId) ?: throw GenialeAufgaben.Fehler("Diesen Vorschlag gibt es nicht mehr.")
        if (v.aufgabeId != null) return v
        val ziel = repo.ziel(v.zielId) ?: throw GenialeAufgaben.Fehler("Das Ziel gibt es nicht mehr.")
        val id = GenialeAufgaben.uebernehmen(context, ziel, v)
        v.copy(aufgabeId = id, uebernommenAm = System.currentTimeMillis(), verworfen = false).also { repo.speichere(it) }
    }

    /** Holt den Stand aus Geniale Aufgaben. In der App gelöschte Aufgaben werden wieder zu offenen Vorschlägen (nur bei vollständiger Liste). */
    suspend fun stand(context: Context): Boolean = sperre.withLock {
        val repo = ZieleRepository.get(context)
        val uebernommen = repo.alleVorschlaegeEinmal().filter { it.aufgabeId != null }
        if (uebernommen.isEmpty()) return true
        val stand = GenialeAufgaben.stand(context) ?: return false
        uebernommen.forEach { v ->
            val erledigt = stand.erledigt[v.aufgabeId]
            when {
                erledigt == null -> if (stand.vollstaendig) repo.speichere(v.copy(aufgabeId = null, uebernommenAm = null, erledigt = false))
                erledigt != v.erledigt -> repo.speichere(v.copy(erledigt = erledigt))
            }
        }
        true
    }
}
