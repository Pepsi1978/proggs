package de.frank.entropyreducer.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.frank.entropyreducer.presentation.dashboard4.BiomarkerCardId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistiert die individuelle Reihenfolge der Biomarker-Karten.
 *
 * Frank-Wunsch 2026-05-10: Alle Daten-Karten im Biomarker-Bereich sollen per
 * Drag & Drop verschiebbar sein. Die gewaehlte Reihenfolge muss App-Neustarts
 * und Sync-Vorgaenge ueberleben.
 *
 * Speicherformat: Eine pipe-separierte Liste von Card-IDs in einem einzigen
 * String-Preferences-Key. Beispiel: "hrv|rhr|sleep_total|...".
 *
 * Selbstheilende Logik:
 * - Wenn keine Reihenfolge gespeichert ist: [BiomarkerCardId.DEFAULT_ORDER] zurueckgeben.
 * - Wenn neue Card-IDs in spaeteren App-Versionen dazukommen, die noch nicht in
 *   der gespeicherten Reihenfolge stehen: Diese werden automatisch ans Ende
 *   angehaengt — der Benutzer behaelt seine Reihenfolge, sieht aber neue Karten.
 * - Wenn die gespeicherte Reihenfolge IDs enthaelt die nicht mehr existieren
 *   (z.B. weil eine Karte in einer Update entfernt wurde): Diese werden beim
 *   Lesen ignoriert.
 *
 * Kein Migrations-Code noetig: Der Default-Pfad (kein Eintrag im DataStore)
 * liefert immer die aktuelle Default-Reihenfolge — also auch fuer Bestands-User
 * die noch nie etwas verschoben haben.
 */
private val Context.biomarkerCardOrderStore by preferencesDataStore(name = "biomarker_card_order")

@Singleton
class BiomarkerCardOrderRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Liefert die aktuelle Reihenfolge als Liste von Card-IDs. Garantiert,
     * dass alle Karten aus [BiomarkerCardId.DEFAULT_ORDER] enthalten sind —
     * neue Karten werden ans Ende angehaengt, geloeschte herausgefiltert.
     */
    val orderedCardIds: Flow<List<String>> = context.biomarkerCardOrderStore.data.map { prefs ->
        val raw = prefs[KEY_ORDER]
        val savedOrder = raw?.split('|')?.filter { it.isNotBlank() }.orEmpty()
        mergeWithDefaults(savedOrder)
    }

    /**
     * Liefert die ROHE gespeicherte Reihenfolge ohne Merge mit DEFAULT_ORDER.
     * Wird vom Drive-Backup (Frank-Wunsch 2026-05-10) genutzt, damit nur die
     * User-Anpassungen gesichert werden — wenn der Benutzer noch nichts
     * verschoben hat (DataStore leer), wird auch eine leere Liste gesichert,
     * was beim Restore nichts ueberschreibt.
     */
    val rawSavedOrder: Flow<List<String>> = context.biomarkerCardOrderStore.data.map { prefs ->
        val raw = prefs[KEY_ORDER]
        raw?.split('|')?.filter { it.isNotBlank() }.orEmpty()
    }

    /**
     * Speichert eine neue Reihenfolge. Wird vom ViewModel nach jedem Reorder
     * aufgerufen. Filtert IDs heraus die nicht in [BiomarkerCardId.DEFAULT_ORDER]
     * stehen (Sicherheits-Check gegen Datenmuell).
     */
    suspend fun saveOrder(newOrder: List<String>) {
        val cleaned = newOrder.filter { it in BiomarkerCardId.DEFAULT_ORDER }
            .distinct()
        context.biomarkerCardOrderStore.edit { prefs ->
            prefs[KEY_ORDER] = cleaned.joinToString("|")
        }
    }

    /**
     * Setzt die Reihenfolge auf die Werks-Einstellung zurueck. Wird durch
     * den "Reihenfolge zuruecksetzen"-Eintrag im Settings-Menue ausgeloest
     * (kann in einer spaeteren Version ergaenzt werden).
     */
    suspend fun resetToDefault() {
        context.biomarkerCardOrderStore.edit { prefs ->
            prefs.remove(KEY_ORDER)
        }
    }

    /**
     * Verschmilzt die gespeicherte Reihenfolge mit der aktuellen Default-Liste:
     * - Bekannte gespeicherte IDs behalten ihre vom Benutzer gewaehlte Reihenfolge
     * - Neue Default-IDs werden an ihrer DEFAULT-Nachbarposition eingefuegt,
     *   nicht stumpf ans Ende — sonst landen z.B. neue Mini-Karten weit unten
     *   in der Liste statt bei den anderen Mini-Karten oben (Frank-Befund 2026-05-10).
     * - IDs aus dem gespeicherten String die nicht mehr existieren werden
     *   herausgefiltert.
     *
     * Algorithmus fuer "neue ID einfuegen":
     *  1. Default-Index der neuen ID bestimmen.
     *  2. Vom Default-Index nach hinten den naechsten Vorgaenger suchen, der
     *     bereits in validSaved steht.
     *  3. Neue ID direkt nach diesem Vorgaenger einfuegen. Wenn kein Vorgaenger
     *     existiert (neue ID war erste in DEFAULT_ORDER), an Position 0 setzen.
     */
    private fun mergeWithDefaults(saved: List<String>): List<String> {
        if (saved.isEmpty()) return BiomarkerCardId.DEFAULT_ORDER
        val validSaved = saved.filter { it in BiomarkerCardId.DEFAULT_ORDER }
            .distinct()
            .toMutableList()
        val missing = BiomarkerCardId.DEFAULT_ORDER.filterNot { it in validSaved }
        for (newId in missing) {
            val defaultIdx = BiomarkerCardId.DEFAULT_ORDER.indexOf(newId)
            var insertAfter: String? = null
            for (i in (defaultIdx - 1) downTo 0) {
                val prevId = BiomarkerCardId.DEFAULT_ORDER[i]
                if (prevId in validSaved) {
                    insertAfter = prevId
                    break
                }
            }
            if (insertAfter == null) {
                validSaved.add(0, newId)
            } else {
                val insertIdx = validSaved.indexOf(insertAfter) + 1
                validSaved.add(insertIdx, newId)
            }
        }
        return validSaved
    }

    private companion object {
        val KEY_ORDER = stringPreferencesKey("ordered_card_ids")
    }
}
