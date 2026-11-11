package de.frank.jarvis.bio

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.frank.jarvis.bio.data.local.AppDatabase
import java.io.File

/**
 * Kopiert die Biomarker-Tabellen aus einer fremden Datenbankdatei (Schnappschuss von Entropie Reductor oder Spiegel vom Handy)
 * in Jarvis' Biomarker-Datenbank: spaltengenau (nur Spalten, die beide Seiten kennen), die Zieltabellen vorher geleert, alles in
 * EINER Transaktion. Bewusst ohne ATTACH auf Rooms Verbindung (Android schaltet dabei das WAL-Journal ab); die Quelle wird
 * getrennt und nur lesend geöffnet. Gibt die Zeilenzahlen danach zurück.
 */
object BioTabellenKopie {
    fun kopiere(quelle: File): Map<String, Long> {
        val ziel = BioModul.datenbank.openHelper.writableDatabase
        SQLiteDatabase.openDatabase(quelle.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { q ->
            fun spalten(t: String, lies: (String) -> Cursor): List<String> =
                lies("PRAGMA table_info(`$t`)").use { c -> buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) } }
            ziel.beginTransaction()
            try {
                AppDatabase.TABELLEN.forEach { t ->
                    val gemeinsam = spalten(t) { sql -> q.rawQuery(sql, null) }.let { quellSpalten ->
                        spalten(t) { sql -> ziel.query(sql) }.filter { it in quellSpalten }
                    }
                    ziel.execSQL("DELETE FROM `$t`")
                    if (gemeinsam.isEmpty()) return@forEach
                    q.rawQuery("SELECT ${gemeinsam.joinToString(", ") { "`$it`" }} FROM `$t`", null).use { c ->
                        while (c.moveToNext()) {
                            val werte = ContentValues(gemeinsam.size)
                            gemeinsam.forEachIndexed { i, name ->
                                when (c.getType(i)) {
                                    Cursor.FIELD_TYPE_NULL -> werte.putNull(name)
                                    Cursor.FIELD_TYPE_INTEGER -> werte.put(name, c.getLong(i))
                                    Cursor.FIELD_TYPE_FLOAT -> werte.put(name, c.getDouble(i))
                                    Cursor.FIELD_TYPE_BLOB -> werte.put(name, c.getBlob(i))
                                    else -> werte.put(name, c.getString(i))
                                }
                            }
                            ziel.insert(t, SQLiteDatabase.CONFLICT_REPLACE, werte)
                        }
                    }
                }
                ziel.setTransactionSuccessful()
            } finally {
                ziel.endTransaction()
            }
        }
        BioModul.datenbank.invalidationTracker.refreshVersionsAsync()
        return AppDatabase.TABELLEN.associateWith { t -> ziel.query("SELECT COUNT(*) FROM `$t`").use { c -> c.moveToFirst(); c.getLong(0) } }
    }
}
