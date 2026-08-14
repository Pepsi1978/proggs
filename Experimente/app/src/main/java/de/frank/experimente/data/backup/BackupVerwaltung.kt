package de.frank.experimente.data.backup

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import androidx.room.withTransaction
import de.frank.experimente.BuildConfig
import de.frank.experimente.data.local.ExperimenteDatenbank
import de.frank.experimente.data.settings.Einstellungen
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Vollständiger, portabler Export der Room-Daten und Einstellungen. */
class BackupVerwaltung(
    context: Context,
    private val db: ExperimenteDatenbank,
    private val einstellungen: Einstellungen,
) {
    private val resolver = context.applicationContext.contentResolver
    private val importJournal = context.applicationContext.getSharedPreferences(
        IMPORT_JOURNAL,
        Context.MODE_PRIVATE,
    )

    /** Repariert den winzigen Zwischenraum zwischen Preferences- und SQLite-Commit. */
    fun stelleImportKonsistenzHer() {
        val token = importJournal.getString(JOURNAL_TOKEN, null)
        val alteEinstellungen = importJournal.getString(JOURNAL_SETTINGS, null)
        val sqlite = db.openHelper.writableDatabase
        if (token != null && alteEinstellungen != null && !istDatenbankImportiert(sqlite, token)) {
            einstellungen.importiereVollstaendig(JSONObject(alteEinstellungen))
        }
        // Erst das Journal dauerhaft entfernen. Bleibt danach durch Prozessabbruch nur die
        // Marker-Tabelle übrig, ist das harmlos; die umgekehrte Reihenfolge wäre mehrdeutig.
        if (importJournal.edit().clear().commit()) {
            sqlite.execSQL("DROP TABLE IF EXISTS `$IMPORT_MARKER_TABLE`")
        }
    }

    suspend fun exportiereIn(ordner: Uri): String = withContext(Dispatchers.IO) {
        val dateiname = DATEIFORMAT.format(java.time.ZonedDateTime.now())
            .let { "Experimente-Backup-$it.json" }
        val ordnerDokument = DocumentsContract.buildDocumentUriUsingTree(
            ordner,
            DocumentsContract.getTreeDocumentId(ordner),
        )
        val ziel = DocumentsContract.createDocument(
            resolver,
            ordnerDokument,
            MIME_JSON,
            dateiname,
        ) ?: throw IllegalStateException("Im gewählten Google-Drive-Ordner ließ sich keine Datei anlegen.")

        try {
            val payload = db.withTransaction {
                JSONObject()
                    .put("databaseSchema", db.openHelper.writableDatabase.version)
                    .put("tables", exportiereTabellen())
                    .put("settings", einstellungen.exportiereVollstaendig())
            }
            val bytes = payload.toString().toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_PAYLOAD_BYTES) { "Das Backup ist ungewöhnlich groß." }
            val dokument = JSONObject()
                .put("format", FORMAT)
                .put("backupVersion", BACKUP_VERSION)
                .put("appVersion", BuildConfig.VERSION_NAME)
                .put("createdAt", Instant.now().toString())
                .put("sha256", sha256(bytes))
                .put("payloadBase64", Base64.encodeToString(bytes, Base64.NO_WRAP))

            resolver.openOutputStream(ziel, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(dokument.toString(2))
            } ?: throw IllegalStateException("Die Backup-Datei ließ sich nicht öffnen.")
            dateiname
        } catch (fehler: Exception) {
            runCatching { resolver.delete(ziel, null, null) }
            throw fehler
        }
    }

    suspend fun importiereVon(quelle: Uri): Int = withContext(Dispatchers.IO) {
        val dokument = JSONObject(liesBegrenzt(quelle))
        require(dokument.getString("format") == FORMAT) { "Das ist kein Experimente-Backup." }
        require(dokument.getInt("backupVersion") == BACKUP_VERSION) {
            "Diese Backup-Version wird von der App nicht unterstützt."
        }
        val bytes = Base64.decode(dokument.getString("payloadBase64"), Base64.DEFAULT)
        require(sha256(bytes).equals(dokument.getString("sha256"), ignoreCase = true)) {
            "Die Prüfsumme stimmt nicht. Das Backup ist unvollständig oder beschädigt."
        }
        val payload = JSONObject(bytes.toString(Charsets.UTF_8))
        val schema = payload.getInt("databaseSchema")
        require(schema == db.openHelper.writableDatabase.version) {
            if (schema > db.openHelper.writableDatabase.version) {
                "Das Backup stammt aus einer neueren App-Version. Bitte zuerst die App aktualisieren."
            } else {
                "Das Backup nutzt ein älteres Datenformat, das diese App nicht direkt importieren kann."
            }
        }

        val tabellen = payload.getJSONObject("tables")
        TABELLEN.forEach { tabellen.getJSONArray(it) }
        val neueEinstellungen = payload.getJSONObject("settings")
        val alteEinstellungen = einstellungen.exportiereVollstaendig()
        val importToken = UUID.randomUUID().toString()
        check(
            importJournal.edit()
                .putString(JOURNAL_TOKEN, importToken)
                .putString(JOURNAL_SETTINGS, alteEinstellungen.toString())
                .commit(),
        ) { "Der bisherige Einstellungsstand ließ sich nicht absichern." }

        try {
            db.withTransaction {
                val sqlite = db.openHelper.writableDatabase
                TABELLEN.asReversed().forEach { sqlite.execSQL("DELETE FROM `$it`") }
                TABELLEN.forEach { tabelle ->
                    val zeilen = tabellen.getJSONArray(tabelle)
                    val erlaubteSpalten = spaltenVon(sqlite.query("PRAGMA table_info(`$tabelle`)"))
                    for (index in 0 until zeilen.length()) {
                        val werte = inhaltVon(zeilen.getJSONObject(index), erlaubteSpalten)
                        check(sqlite.insert(tabelle, SQLiteDatabase.CONFLICT_ABORT, werte) != -1L) {
                            "Eine Zeile in $tabelle ließ sich nicht wiederherstellen."
                        }
                    }
                }
                // Ein einzelner verschlüsselter-Preferences-Commit unmittelbar vor dem
                // Room-Commit. Scheitert er, rollt auch die Datenbanktransaktion zurück.
                einstellungen.importiereVollstaendig(neueEinstellungen)
                sqlite.execSQL(
                    "CREATE TABLE IF NOT EXISTS `$IMPORT_MARKER_TABLE` " +
                        "(token TEXT NOT NULL PRIMARY KEY)",
                )
                sqlite.execSQL("DELETE FROM `$IMPORT_MARKER_TABLE`")
                sqlite.execSQL(
                    "INSERT INTO `$IMPORT_MARKER_TABLE` (token) VALUES (?)",
                    arrayOf(importToken),
                )
            }
        } catch (fehler: Exception) {
            val zurueckgestellt = runCatching {
                einstellungen.importiereVollstaendig(alteEinstellungen)
            }.isSuccess
            if (zurueckgestellt) importJournal.edit().clear().commit()
            throw fehler
        }
        check(importJournal.edit().clear().commit()) { "Das Import-Journal ließ sich nicht abschließen." }
        db.openHelper.writableDatabase.execSQL("DROP TABLE IF EXISTS `$IMPORT_MARKER_TABLE`")
        db.invalidationTracker.refreshAsync()
        TABELLEN.sumOf { tabellen.getJSONArray(it).length() }
    }

    private fun exportiereTabellen(): JSONObject {
        val ergebnis = JSONObject()
        val sqlite = db.openHelper.readableDatabase
        TABELLEN.forEach { tabelle ->
            val zeilen = JSONArray()
            sqlite.query("SELECT * FROM `$tabelle`").use { cursor ->
                while (cursor.moveToNext()) {
                    val zeile = JSONObject()
                    cursor.columnNames.forEachIndexed { index, name ->
                        zeile.put(
                            name,
                            when (cursor.getType(index)) {
                                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                                Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
                                Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(cursor.getBlob(index), Base64.NO_WRAP)
                                else -> throw IllegalStateException("Unbekannter SQLite-Typ in $tabelle.$name")
                            },
                        )
                    }
                    zeilen.put(zeile)
                }
            }
            ergebnis.put(tabelle, zeilen)
        }
        return ergebnis
    }

    private fun spaltenVon(cursor: Cursor): Set<String> = cursor.use {
        buildSet {
            val nameIndex = it.getColumnIndexOrThrow("name")
            while (it.moveToNext()) add(it.getString(nameIndex))
        }
    }

    private fun inhaltVon(json: JSONObject, erlaubteSpalten: Set<String>): ContentValues {
        val ergebnis = ContentValues(json.length())
        val schluessel = json.keys()
        while (schluessel.hasNext()) {
            val name = schluessel.next()
            require(name in erlaubteSpalten) { "Unbekannte Spalte im Backup: $name" }
            when (val wert = json.get(name)) {
                JSONObject.NULL -> ergebnis.putNull(name)
                is Int -> ergebnis.put(name, wert)
                is Long -> ergebnis.put(name, wert)
                is Double -> ergebnis.put(name, wert)
                is String -> ergebnis.put(name, wert)
                else -> throw IllegalArgumentException("Ungültiger Wert für $name")
            }
        }
        return ergebnis
    }

    private fun liesBegrenzt(uri: Uri): String {
        val ausgabe = ByteArrayOutputStream()
        resolver.openInputStream(uri)?.use { eingabe ->
            val puffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var gesamt = 0
            while (true) {
                val anzahl = eingabe.read(puffer)
                if (anzahl < 0) break
                gesamt += anzahl
                require(gesamt <= MAX_BACKUP_BYTES) { "Die Backup-Datei ist ungewöhnlich groß." }
                ausgabe.write(puffer, 0, anzahl)
            }
        } ?: throw IllegalStateException("Die gewählte Backup-Datei ließ sich nicht öffnen.")
        return ausgabe.toString(Charsets.UTF_8.name())
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun istDatenbankImportiert(
        sqlite: androidx.sqlite.db.SupportSQLiteDatabase,
        token: String,
    ): Boolean {
        val hatTabelle = sqlite.query(
            "SELECT 1 FROM sqlite_master WHERE type='table' AND name=? LIMIT 1",
            arrayOf(IMPORT_MARKER_TABLE),
        ).use { it.moveToFirst() }
        if (!hatTabelle) return false
        return sqlite.query(
            "SELECT 1 FROM `$IMPORT_MARKER_TABLE` WHERE token=? LIMIT 1",
            arrayOf(token),
        ).use { it.moveToFirst() }
    }

    companion object {
        private const val FORMAT = "de.frank.experimente.vollbackup"
        private const val BACKUP_VERSION = 1
        private const val MIME_JSON = "application/json"
        private const val MAX_PAYLOAD_BYTES = 12 * 1024 * 1024
        private const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
        private const val IMPORT_JOURNAL = "backup_import_journal"
        private const val JOURNAL_TOKEN = "token"
        private const val JOURNAL_SETTINGS = "alte_einstellungen"
        private const val IMPORT_MARKER_TABLE = "backup_import_marker"
        private val DATEIFORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneId.systemDefault())
        private val TABELLEN = listOf(
            "selbstbild",
            "ziele",
            "logtage",
            "vorschlaege",
            "experimente",
            "aufgaben",
            "auswertungen",
            "gespraech",
            "merkliste",
            "erkenntnisse",
            "lage",
        )
    }
}
