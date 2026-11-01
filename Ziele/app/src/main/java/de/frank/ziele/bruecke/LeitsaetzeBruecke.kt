package de.frank.ziele.bruecke

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import de.frank.ziele.data.ZieleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/** Signaturgeschützte, rein lesende Verbindung zu Perfect Moment. Alle Zielstatus zählen. */
class LeitsaetzeBruecke : ContentProvider() {
    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val ctx = requireNotNull(context)
        val ziele = runBlocking(Dispatchers.IO) { ZieleRepository.get(ctx).zieleEinmal() }
        return MatrixCursor(arrayOf("_id", "symbol", "leitsatz")).apply {
            ziele.filter { it.leitsatz.isNotBlank() }.forEach { addRow(arrayOf<Any>(it.id, it.symbol, it.leitsatz)) }
            setNotificationUri(ctx.contentResolver, Uri.parse("content://de.frank.ziele.leitsaetze"))
        }
    }

    override fun getType(uri: Uri) = "vnd.android.cursor.dir/vnd.de.frank.ziele.leitsatz"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException()
}
