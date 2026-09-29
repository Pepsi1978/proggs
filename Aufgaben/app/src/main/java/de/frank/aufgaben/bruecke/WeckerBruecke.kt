package de.frank.aufgaben.bruecke

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import de.frank.aufgaben.data.Aufgabe
import de.frank.aufgaben.data.AufgabenRepository
import de.frank.aufgaben.data.Tage
import kotlinx.coroutines.runBlocking

/**
 * Brücke zur App „Genialer Wecker“ (de.frank.genialerwecker), nur lesend.
 *
 * Adressen (Authority `de.frank.aufgaben.wecker`):
 *  - `content://de.frank.aufgaben.wecker/heute`  — Aufgaben des heutigen Tages (plus Überfälliges)
 *  - `content://de.frank.aufgaben.wecker/morgen` — Aufgaben von morgen
 *  - `content://de.frank.aufgaben.wecker/tag/<epochDay>` — Aufgaben eines bestimmten Tages
 *
 * Für den Wecker gilt: Wird der Wecker abends gestellt, sind die Morgen-Aufgaben am Folgetag die
 * Heute-Aufgaben. Deshalb fragt der Wecker am besten mit dem Klingeldatum über `/tag/<epochDay>`.
 *
 * Spalten: _id, titel, text, uhrzeit (Minuten nach Mitternacht, -1 = ohne), zeit ("HH:MM" oder leer),
 * prioritaet (HOCH/MITTEL/GERING/SPAETER), erledigt (0/1), tag (Epochentag), vorlesetext.
 * Sortierung: Termine nach Uhrzeit, danach die übrigen nach Priorität. Nur offene Aufgaben.
 */
class WeckerBruecke : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val ctx = context ?: return MatrixCursor(SPALTEN)
        val segmente = uri.pathSegments
        val heute = Tage.heute()
        val tag = when (segmente.firstOrNull()) {
            "heute" -> heute
            "morgen" -> heute + 1
            "tag" -> segmente.getOrNull(1)?.toLongOrNull() ?: heute
            else -> heute
        }
        val liste = runBlocking { AufgabenRepository.get(ctx).fuerTag(tag, tag == heute) }
            .filter { !it.erledigt }
            .sortedWith(compareBy<Aufgabe>({ it.minuten == null }, { it.minuten ?: 0 }, { it.prio.rang }))
        val cursor = MatrixCursor(SPALTEN)
        liste.forEach { a ->
            val zeit = a.minuten?.let { Tage.zeit(it) }.orEmpty()
            val vorlesen = if (zeit.isNotEmpty()) "Um $zeit Uhr: ${a.titel}." else "${a.titel}."
            cursor.addRow(arrayOf<Any?>(a.id, a.titel, a.text, a.minuten ?: -1, zeit, a.prio.name, if (a.erledigt) 1 else 0, a.tag ?: tag, vorlesen))
        }
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.de.frank.aufgaben.aufgabe"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        val SPALTEN = arrayOf("_id", "titel", "text", "uhrzeit", "zeit", "prioritaet", "erledigt", "tag", "vorlesetext")
    }
}
