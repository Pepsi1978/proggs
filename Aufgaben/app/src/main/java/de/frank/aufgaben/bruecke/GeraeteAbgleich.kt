package de.frank.aufgaben.bruecke

import android.app.ActivityManager
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Geräteabgleich zwischen Franks Geräten (Handy ↔ Tablet). In allen angebundenen Apps dieselbe Datei, nur die package-Zeile
 * unterscheidet sich; Änderungen immer in allen Kopien nachziehen (Aufgaben, Ziele, Geniale Ideen, Epigenetik, Stack-Labor).
 *
 * Die App selbst spricht mit keinem Server. Jarvis holt über die Brücke den ganzen Datenstand als Paket ab und spielt den
 * Stand des anderen Geräts ein; es gewinnt jeweils der jüngere Stand, und zwar als Ganzes (kein zeilenweises Mischen:
 * Die Kennungen sind hochgezählt und würden zwischen zwei Geräten kollidieren).
 *
 * Brücken-Methoden (über [rufe], Antwort im Bundle unter "json"):
 * - `abgleich_stand`: billiger Fingerabdruck (Größe und Änderungszeit der Dateien), Schema und App-Version. Ändert er sich
 *   nicht, braucht Jarvis nichts zu tun.
 * - `abgleich_export`: schreibt das Paket (ZIP: Datenbanken als bereinigte Kopie, Zusatzdateien, meta.json) und nennt
 *   `inhalt` (Prüfsumme über den Inhalt der Tabellen, unabhängig von der Dateiablage) und `sha256` des Pakets.
 *   Abholen: `openFile(content://<Brücke>/abgleich/export, "r")`.
 * - `abgleich_import` (`sha256`): prüft das zuvor über `openFile(…/abgleich/import, "w")` abgelegte Paket (Prüfsumme, Schema
 *   nicht neuer als diese App), sichert den eigenen Stand als Rückfall (`files/abgleich-rueckfall/`, die letzten fünf) und
 *   legt das Paket bereit. Eingespielt wird es beim nächsten Prozessstart ([wendeAn], bevor die Datenbank offen ist);
 *   dafür beendet sich der Prozess kurz danach selbst und öffnet die App wieder, falls sie gerade vorn war.
 */
class GeraeteAbgleich(private val ctx: Context, private val datenbanken: List<String>, private val dateien: List<String> = emptyList()) {

    private val ordner get() = File(ctx.cacheDir, "abgleich").apply { mkdirs() }
    private val exportDatei get() = File(ordner, "export.zip")
    private val importDatei get() = File(ordner, "import.zip")

    private fun appVersion(): Long = runCatching {
        @Suppress("DEPRECATION") val info = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
    }.getOrDefault(0L)

    private fun dbDatei(name: String) = ctx.getDatabasePath(name)

    /** Zweite Verbindung neben der der App; WAL bleibt an, sonst stellte Android das Journal der laufenden App um. */
    private fun <T> mitDb(datei: File, block: (SQLiteDatabase) -> T): T {
        val db = SQLiteDatabase.openDatabase(datei.path, null, SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)
        try { return block(db) } finally { db.close() }
    }

    private fun schema(datei: File): Int = if (!datei.exists()) 0 else runCatching {
        SQLiteDatabase.openDatabase(datei.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
    }.getOrElse { mitDb(datei) { it.version } }

    fun stand(): JSONObject {
        val teile = StringBuilder()
        var zuletzt = 0L
        fun nimm(name: String, f: File) {
            teile.append(name).append(':').append(if (f.exists()) "${f.length()}:${f.lastModified()}" else "-").append('|')
            if (f.exists()) zuletzt = maxOf(zuletzt, f.lastModified())
        }
        for (name in datenbanken) for (endung in listOf("", "-wal")) nimm(name + endung, File(dbDatei(name).path + endung))
        for (rel in dateien) nimm(rel, File(ctx.filesDir, rel))
        // geaendert_um: wann hier zuletzt geschrieben wurde. Damit entscheidet Jarvis, welcher Stand der jüngere ist, wenn beide Geräte geändert haben.
        return JSONObject().put("stempel", teile.toString()).put("geaendert_um", zuletzt).put("schema", schemaListe()).put("version", appVersion())
            .put("bereit", bereitDatei(ctx).exists())
    }

    private fun schemaListe() = JSONObject().apply { datenbanken.forEach { put(it, schema(dbDatei(it))) } }

    /** Prüfsumme über den Inhalt aller Tabellen und Zusatzdateien: gleich bei gleichem Inhalt, egal wie die Datei aufgebaut ist. */
    private fun inhalt(dbs: Map<String, File>): String {
        val md = MessageDigest.getInstance("SHA-256")
        fun text(s: String) { val b = s.toByteArray(); md.update(b.size.toString().toByteArray()); md.update(0); md.update(b) }
        for ((name, datei) in dbs.toSortedMap()) {
            text("db:$name")
            if (!datei.exists()) continue
            SQLiteDatabase.openDatabase(datei.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                // Volltext-Tabellen (FTS) und ihre Hilfstabellen zählen nicht: Sie sind aus den echten Tabellen abgeleitet, und ihr
                // innerer Aufbau hängt von der Schreibgeschichte ab, nicht vom Inhalt.
                val volltext = mutableListOf<String>()
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND sql LIKE 'CREATE VIRTUAL TABLE%'", null).use { c -> while (c.moveToNext()) volltext += c.getString(0) }
                val tabellen = mutableListOf<String>()
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table','android_metadata') ORDER BY name", null)
                    .use { c -> while (c.moveToNext()) c.getString(0).let { n -> if (volltext.none { v -> n == v || n.startsWith(v + "_") }) tabellen += n } }
                for (t in tabellen) {
                    text("t:$t")
                    db.rawQuery("SELECT * FROM \"$t\" ORDER BY rowid", null).use { c ->
                        text(c.columnNames.joinToString(","))
                        while (c.moveToNext()) for (i in 0 until c.columnCount) when (c.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> md.update(1)
                            Cursor.FIELD_TYPE_INTEGER -> { md.update(2); text(c.getLong(i).toString()) }
                            Cursor.FIELD_TYPE_FLOAT -> { md.update(3); text(c.getDouble(i).toString()) }
                            Cursor.FIELD_TYPE_BLOB -> { md.update(4); val b = c.getBlob(i); md.update(b.size.toString().toByteArray()); md.update(b) }
                            else -> { md.update(5); text(c.getString(i)) }
                        }
                    }
                }
            }
        }
        for (rel in dateien.sorted()) {
            text("datei:$rel")
            val f = File(ctx.filesDir, rel)
            if (f.exists()) f.inputStream().use { ein -> val puffer = ByteArray(65536); while (true) { val n = ein.read(puffer); if (n < 0) break; md.update(puffer, 0, n) } }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    /** Bereinigte, in sich stimmige Kopie der laufenden Datenbank (enthält auch, was noch im WAL steht). */
    private fun kopiere(name: String, ziel: File) {
        ziel.delete()
        val quelle = dbDatei(name)
        if (!quelle.exists()) return
        try {
            mitDb(quelle) { it.execSQL("VACUUM INTO '" + ziel.path.replace("'", "''") + "'") }
        } catch (e: Exception) {
            // Ältere SQLite-Fassung ohne VACUUM INTO: WAL in die Datei zurückschreiben und sie kopieren.
            ziel.delete()
            mitDb(quelle) { db -> db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { it.moveToFirst() } }
            quelle.copyTo(ziel, overwrite = true)
        }
    }

    @Synchronized
    fun exportiere(ziel: File = exportDatei): JSONObject {
        val arbeit = File(ordner, "arbeit").apply { deleteRecursively(); mkdirs() }
        try {
            val kopien = datenbanken.associateWith { File(arbeit, it).also { k -> kopiere(it, k) } }
            val summe = inhalt(kopien)
            val teil = File(ziel.path + ".teil")
            ZipOutputStream(teil.outputStream().buffered()).use { zip ->
                fun eintrag(pfad: String, daten: () -> Unit) { zip.putNextEntry(ZipEntry(pfad).apply { time = 0 }); daten(); zip.closeEntry() }
                eintrag("meta.json") {
                    zip.write(JSONObject().put("paket", ctx.packageName).put("version", appVersion()).put("schema", schemaListe()).put("inhalt", summe)
                        .put("datenbanken", JSONArray(datenbanken)).put("dateien", JSONArray(dateien)).toString().toByteArray())
                }
                for ((name, kopie) in kopien) if (kopie.exists()) eintrag("db/$name") { kopie.inputStream().use { it.copyTo(zip) } }
                for (rel in dateien) File(ctx.filesDir, rel).takeIf { it.isFile }?.let { f -> eintrag("datei/$rel") { f.inputStream().use { it.copyTo(zip) } } }
            }
            if (!teil.renameTo(ziel)) { ziel.delete(); check(teil.renameTo(ziel)) { "Paket ließ sich nicht ablegen" } }
            return JSONObject().put("inhalt", summe).put("sha256", sha256(ziel)).put("groesse", ziel.length()).put("schema", schemaListe()).put("version", appVersion())
                .put("stempel", stand().getString("stempel"))
        } finally {
            arbeit.deleteRecursively()
        }
    }

    @Synchronized
    fun importiere(sha: String): JSONObject {
        val paket = importDatei
        if (!paket.exists()) return fehler("Kein Paket abgelegt.")
        if (sha256(paket) != sha) { paket.delete(); return fehler("Das Paket ist beschädigt angekommen (Prüfsumme passt nicht).") }
        val arbeit = File(ordner, "pruefung").apply { deleteRecursively(); mkdirs() }
        try {
            ZipFile(paket).use { zip ->
                val meta = zip.getEntry("meta.json")?.let { JSONObject(zip.getInputStream(it).readBytes().toString(Charsets.UTF_8)) } ?: return fehler("Paket ohne Beschreibung.")
                if (meta.optString("paket") != ctx.packageName) return fehler("Das Paket gehört zu einer anderen App (${meta.optString("paket")}).")
                for (name in datenbanken) {
                    val e = zip.getEntry("db/$name") ?: continue
                    val probe = File(arbeit, name)
                    zip.getInputStream(e).use { ein -> probe.outputStream().use { ein.copyTo(it) } }
                    val neu = schema(probe)
                    val eigen = schema(dbDatei(name))
                    // Ein neueres Schema kann diese App-Fassung nicht lesen: erst die App aktualisieren, dann abgleichen.
                    if (eigen in 1 until neu) return fehler("Der Stand des anderen Geräts stammt von einer neueren App-Fassung (Schema $neu, hier $eigen). Bitte die App hier aktualisieren.").put("app_zu_alt", true)
                    // Lesbar und mit Tabellen: Die Übertragung ist über die Prüfsumme gesichert, und Jarvis vergleicht nach dem Einspielen den Inhalt.
                    // Kein quick_check: Bei Volltext-Tabellen (FTS) meldet er auf einer nur lesbaren Kopie Fehler, obwohl die Datenbank heil ist.
                    val tabellen = runCatching { SQLiteDatabase.openDatabase(probe.path, null, SQLiteDatabase.OPEN_READONLY).use { db -> db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table'", null).use { if (it.moveToFirst()) it.getInt(0) else 0 } } }.getOrDefault(0)
                    if (tabellen == 0) return fehler("Die Datenbank $name im Paket ist nicht lesbar.")
                }
            }
            // Der eigene Stand bleibt als Rückfall liegen: Haben beide Geräte geändert, geht so nichts endgültig verloren.
            val rueckfall = File(ctx.filesDir, "abgleich-rueckfall").apply { mkdirs() }
            runCatching { exportiere(File(rueckfall, "${System.currentTimeMillis()}.zip")) }.onFailure { Log.w(TAG, "Rückfall nicht geschrieben", it) }
            rueckfall.listFiles()?.sortedByDescending { it.name }?.drop(5)?.forEach { it.delete() }
            val bereit = bereitDatei(ctx)
            bereit.delete()
            paket.copyTo(bereit, overwrite = true)
            paket.delete()
            neustart()
            return JSONObject().put("ok", true).put("neustart", true)
        } finally {
            arbeit.deleteRecursively()
        }
    }

    /** Die Datenbank ist in diesem Prozess offen und lässt sich nicht unter der laufenden App austauschen: Prozess neu starten. */
    private fun neustart() {
        val vorn = runCatching {
            val info = ActivityManager.RunningAppProcessInfo().also { ActivityManager.getMyMemoryState(it) }
            info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        }.getOrDefault(false)
        Handler(Looper.getMainLooper()).postDelayed({
            if (vorn) runCatching {
                ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
                    ctx.startActivity(it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK))
                }
            }
            Process.killProcess(Process.myPid())
        }, 500)
    }

    /** Für [android.content.ContentProvider.openFile]: Paket abholen (`…/abgleich/export`, lesen) oder ablegen (`…/abgleich/import`, schreiben). */
    fun oeffne(uri: Uri, modus: String): ParcelFileDescriptor = when (uri.path) {
        "/abgleich/export" -> ParcelFileDescriptor.open(exportDatei.also { if (!it.exists()) throw FileNotFoundException("Kein Export") }, ParcelFileDescriptor.MODE_READ_ONLY)
        "/abgleich/import" -> ParcelFileDescriptor.open(importDatei, ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE)
        else -> throw FileNotFoundException(uri.toString())
    }

    /** Beantwortet die Abgleich-Methoden; null = keine davon, die Brücke macht normal weiter. */
    fun rufe(methode: String, extras: Bundle?): Bundle? {
        if (!methode.startsWith("abgleich_")) return null
        val antwort = runCatching {
            val anfrage = extras?.getString("json")?.takeIf { it.isNotBlank() }?.let(::JSONObject) ?: JSONObject()
            when (methode) {
                "abgleich_stand" -> stand()
                "abgleich_export" -> exportiere()
                "abgleich_import" -> importiere(anfrage.optString("sha256"))
                else -> fehler("Unbekannte Abgleich-Methode: $methode")
            }
        }.getOrElse { e -> Log.w(TAG, methode, e); fehler(e.message ?: e.javaClass.simpleName) }
        return Bundle().apply { putString("json", antwort.toString()) }
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    companion object {
        private const val TAG = "GeraeteAbgleich"

        private fun bereitDatei(ctx: Context) = File(ctx.filesDir, "abgleich-bereit.zip")

        private fun sha256(datei: File): String {
            val md = MessageDigest.getInstance("SHA-256")
            datei.inputStream().use { ein -> val puffer = ByteArray(65536); while (true) { val n = ein.read(puffer); if (n < 0) break; md.update(puffer, 0, n) } }
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        /** True, wenn in diesem Prozess gerade ein Stand des anderen Geräts eingespielt wurde (zum Neuplanen von Erinnerungen und Widgets). */
        @Volatile var geradeUebernommen = false
            private set

        /**
         * Beim Prozessstart, bevor irgendetwas die Datenbank öffnet (im onCreate der Brücke): spielt ein bereitliegendes Paket ein.
         * Erst vollständig in Nachbardateien entpacken, dann umbenennen: Bricht es mittendrin ab, bleibt der alte Stand ganz.
         */
        @Synchronized
        fun wendeAn(ctx: Context): Boolean {
            val bereit = bereitDatei(ctx)
            if (!bereit.exists()) return false
            return try {
                val neu = mutableListOf<Pair<File, File>>()
                ZipFile(bereit).use { zip ->
                    for (e in zip.entries()) {
                        val ziel = when {
                            e.name.startsWith("db/") && !e.name.contains("..") -> ctx.getDatabasePath(e.name.removePrefix("db/"))
                            e.name.startsWith("datei/") && !e.name.contains("..") -> File(ctx.filesDir, e.name.removePrefix("datei/"))
                            else -> null
                        } ?: continue
                        ziel.parentFile?.mkdirs()
                        val teil = File(ziel.path + ".abgleich")
                        zip.getInputStream(e).use { ein -> teil.outputStream().use { ein.copyTo(it) } }
                        neu += teil to ziel
                    }
                }
                for ((teil, ziel) in neu) {
                    if (ziel.parentFile == ctx.getDatabasePath("x").parentFile) listOf("-wal", "-shm", "-journal").forEach { File(ziel.path + it).delete() }
                    ziel.delete()
                    check(teil.renameTo(ziel)) { "${ziel.name} ließ sich nicht ersetzen" }
                }
                bereit.delete()
                geradeUebernommen = true
                Log.i(TAG, "Stand des anderen Geräts eingespielt (${neu.size} Dateien)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Einspielen gescheitert, der bisherige Stand bleibt", e)
                bereit.renameTo(File(ctx.filesDir, "abgleich-bereit.defekt"))
                false
            }
        }
    }
}
