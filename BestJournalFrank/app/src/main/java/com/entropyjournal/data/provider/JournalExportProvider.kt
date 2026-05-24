package com.entropyjournal.data.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.entropyjournal.data.local.AppDatabase

/**
 * Nur-lesende Daten-Durchreiche fuer die Tagebucheintraege (Frank-Wunsch 2026-05-24).
 * Gibt journal_entries + entry_follow_ups an die App "Entropie Reductor" frei, damit
 * deren Journal-Reiter die Eintraege spiegeln kann.
 *
 * Liest direkt aus der bestehenden Room-DB (entropy_journal_db) ueber das vorhandene
 * Singleton AppDatabase.getDatabase(context). Schreibt/aendert NICHTS — alle
 * Schreiboperationen werfen UnsupportedOperationException.
 *
 * Geschuetzt durch die Berechtigung com.entropyjournal.permission.READ_JOURNAL
 * (Manifest, protectionLevel "normal"). Authority ueber ${applicationId}.journalexport,
 * d.h. com.entropyjournal.debug.journalexport (Debug) bzw. com.entropyjournal.journalexport.
 */
class JournalExportProvider : ContentProvider() {

    private val authority: String by lazy { "${requireCtx().packageName}.journalexport" }

    private val matcher: UriMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(authority, "entries", CODE_ENTRIES)
            addURI(authority, "followups", CODE_FOLLOWUPS)
        }
    }

    private fun requireCtx() = context ?: error("ContentProvider ohne Context")

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val db = AppDatabase.getDatabase(requireCtx()).openHelper.readableDatabase
        val sql =
            when (matcher.match(uri)) {
                CODE_ENTRIES ->
                    "SELECT id, timestamp, title, displayText, rawText, improvedText, " +
                        "isImproved, summary FROM journal_entries ORDER BY timestamp DESC"
                CODE_FOLLOWUPS ->
                    "SELECT id, entryId, createdAt, text, rawText, improvedText, isImproved " +
                        "FROM entry_follow_ups ORDER BY createdAt ASC"
                else -> throw IllegalArgumentException("Unbekannte URI: $uri")
            }
        return db.query(SimpleSQLiteQuery(sql))
    }

    override fun getType(uri: Uri): String =
        when (matcher.match(uri)) {
            CODE_ENTRIES -> "vnd.android.cursor.dir/vnd.$authority.entries"
            CODE_FOLLOWUPS -> "vnd.android.cursor.dir/vnd.$authority.followups"
            else -> throw IllegalArgumentException("Unbekannte URI: $uri")
        }

    // Read-only: jede Schreiboperation ist nicht erlaubt.
    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException("read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("read-only")

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = throw UnsupportedOperationException("read-only")

    private companion object {
        const val CODE_ENTRIES = 1
        const val CODE_FOLLOWUPS = 2
    }
}
