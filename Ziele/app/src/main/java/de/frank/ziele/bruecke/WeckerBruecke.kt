package de.frank.ziele.bruecke

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import de.frank.ziele.data.Kategorie
import de.frank.ziele.data.ZielStatus
import de.frank.ziele.data.ZieleRepository
import java.time.LocalDate
import kotlinx.coroutines.runBlocking

/**
 * Brücke zur App „Genialer Wecker“ (de.frank.genialerwecker.app), nur lesend und ohne Erlaubnis:
 * Der Wecker signiert mit einem eigenen Schlüssel, eine Signatur-Erlaubnis käme dort nie durch (wie bei Geniale Aufgaben).
 *
 * Adressen (Authority `de.frank.ziele.wecker`):
 *  - `content://de.frank.ziele.wecker/aktuell` — alle aktiven Ziele, wichtigste (mit Frist) zuerst
 *  - `content://de.frank.ziele.wecker/vorlesen` — eine einzige Zeile mit einem fertigen Vorlesetext aller aktiven Ziele
 *
 * Spalten: _id, titel, symbol, strategie, frist (Epochentag, -1 = ohne), tage_bis_frist (-1 = ohne),
 * notwendig_offen, notwendig_gesamt, vorlesetext.
 */
class WeckerBruecke : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val ctx = context ?: return MatrixCursor(SPALTEN)
        val heute = LocalDate.now().toEpochDay()
        val repo = ZieleRepository.get(ctx)
        val (ziele, vorschlaege) = runBlocking { repo.zieleEinmal() to repo.alleVorschlaegeEinmal().groupBy { it.zielId } }
        val aktiv = ziele.filter { it.zustand == ZielStatus.AKTIV }.sortedWith(compareBy({ it.frist ?: Long.MAX_VALUE }, { it.rang }))
        val zeilen = aktiv.map { z ->
            val notwendig = vorschlaege[z.id].orEmpty().filter { !it.verworfen && it.gruppe == Kategorie.NOTWENDIG }
            val offen = notwendig.count { !it.erledigt }
            val tage = z.frist?.let { it - heute }
            val text = buildString {
                append(z.titel).append('.')
                when {
                    tage == null -> Unit
                    tage < 0 -> append(" Die Frist ist seit ${-tage} ${if (tage == -1L) "Tag" else "Tagen"} vorbei.")
                    tage == 0L -> append(" Heute ist der Stichtag.")
                    else -> append(" Noch $tage ${if (tage == 1L) "Tag" else "Tage"}.")
                }
                if (z.strategie.isNotBlank()) append(' ').append(z.strategie.trim())
            }
            arrayOf<Any?>(z.id, z.titel, z.symbol, z.strategie, z.frist ?: -1, tage ?: -1, offen, notwendig.size, text)
        }
        val cursor = MatrixCursor(SPALTEN)
        if (uri.pathSegments.firstOrNull() == "vorlesen") {
            val gesamt = if (zeilen.isEmpty()) "Du hast gerade keine aktiven Ziele."
            else (if (zeilen.size == 1) "Dein Ziel: " else "Deine ${zeilen.size} Ziele. ") + zeilen.joinToString(" ") { it.last() as String }
            cursor.addRow(arrayOf<Any?>(0L, "Deine Ziele", "🎯", "", -1, -1, 0, 0, gesamt))
        } else {
            zeilen.forEach(cursor::addRow)
        }
        return cursor
    }

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.de.frank.ziele.ziel"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        val SPALTEN = arrayOf("_id", "titel", "symbol", "strategie", "frist", "tage_bis_frist", "notwendig_offen", "notwendig_gesamt", "vorlesetext")
    }
}
