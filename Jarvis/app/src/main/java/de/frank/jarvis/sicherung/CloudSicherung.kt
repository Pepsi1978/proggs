package de.frank.jarvis.sicherung

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import de.frank.jarvis.BuildConfig
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Geraet
import de.frank.jarvis.data.Umzug
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Cloud-Sicherung: Jarvis sichert sein ganzes Wissen als ZIP in einen Google-Drive-Ordner, den Frank einmal wählt
 * (Meine Ablage › Dokumente › Apps Backup › Jarvis, über den Drive-Anbieter des Systems, ohne eigenen Google-Zugang).
 *
 * Im ZIP: alle Dateien unter `files/` (Merkdateien, Langzeitkontext, Gespräche, Tagebuch-Kurzfassungen, Tagesauswertungen,
 * Listen, Ablage, Atem, Lernstände, Funktionen …), eine stimmige Momentaufnahme jeder Datenbank (`VACUUM INTO`, sonst Datei
 * samt -wal/-shm) und alle Einstellungen Wert für Wert. **Nie dabei:** API-Schlüssel, Tokens, Passwörter, Sitzungen und das
 * Konten-Register (Frank trägt sie aus seinem Passwortmanager selbst nach), außerdem Logbuch und Zwischendateien.
 *
 * Automatisch einmal am Tag, nur am Handy (Erstquelle; das Tablet bekommt alles über den Abgleich) und nie im Sprachgespräch.
 * Im Ordner bleiben die letzten [BEHALTEN] Sicherungen. Wiederherstellen: ZIP wählen → es wird bereitgelegt → Jarvis startet neu
 * und spielt es in [einspielenBeimStart] ein, bevor irgendetwas die Daten öffnet.
 */
object CloudSicherung {
    data class Stand(val ordnerGewaehlt: Boolean, val letzteErfolg: Long, val letzterFehler: String?, val laeuft: Boolean)

    private const val PREFS = "cloud_sicherung"
    private const val K_ORDNER = "ordner"
    private const val K_ERFOLG = "erfolg_am"
    private const val K_FEHLER = "fehler"
    private const val K_VERSUCH = "versuch_am"
    private const val VORSILBE = "Jarvis-Sicherung-"
    private const val BEHALTEN = 14
    private const val FORMAT = "de.frank.jarvis.cloudsicherung"
    private const val BEREIT = "wiederherstellung-bereit.zip"
    private const val ABSTAND_MS = 23 * 3_600_000L
    private const val NEUVERSUCH_MS = 2 * 3_600_000L
    private val NAMENSFORM = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm")

    /** Unter `files/` nie sichern: Protokolle, Modelle, Zugangsdateien und Zwischenstände. */
    private val NIE_DATEIEN = setOf("logbuch", "parakeet-bpe.vocab", "profileInstalled", "fernsteuerung-token.txt", "umzug.json", "umzug-export.json", "zugang.json")
    private val NEBENDATEI = listOf(".teil", ".tmp", ".neu", ".new")

    /** Einstellungsspeicher, die nie mitgehen: Konten-Register und verschlüsselte Geheimnis-Speicher anderer Bereiche. */
    private val NIE_SPEICHER = setOf("jarvis_konten", "bio_encrypted_secrets", "codex_oauth", "staubsauger_cloud", PREFS,
        "WebViewChromiumPrefs", "AwOriginVisitLoggerPrefs", "android.app.ActivityThread.IDS")
    private val VERSCHLUESSELT = listOf(Einstellungen.NAME, SecureSettings.STORE_NAME)

    /** Schlüsselnamen, die nach Zugangsdaten aussehen: nie in die Cloud. */
    private val GEHEIM = Regex("api_key|_key$|_key_|token|secret|geheimnis|passwort|password|sitzung|session|cookie|auffrischung|refresh|client_id|oauth|zugang|credential|auth_")
    private fun geheim(k: String) = k != "tokenlimit" && GEHEIM.containsMatchIn(k.lowercase())

    private val sperre = Mutex()
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _stand = MutableStateFlow(Stand(false, 0L, null, false))
    val stand: StateFlow<Stand> = _stand

    private fun prefs(c: Context): SharedPreferences = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun aktualisiere(c: Context) {
        val p = prefs(c)
        _stand.value = Stand(p.getString(K_ORDNER, null) != null, p.getLong(K_ERFOLG, 0L), p.getString(K_FEHLER, null), sperre.isLocked)
    }

    /** Merkt den in Drive gewählten Ordner dauerhaft (Freigabe überlebt Neustarts). */
    fun ordnerSetzen(c: Context, uri: Uri): Boolean = runCatching {
        c.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        prefs(c).edit().putString(K_ORDNER, uri.toString()).commit()
    }.getOrDefault(false).also { aktualisiere(c) }

    /** Hintergrund: prüft alle 30 Minuten, ob die tägliche Sicherung fällig ist. Nur am Handy. */
    fun starte(c: Context) {
        val app = c.applicationContext
        aktualisiere(app)
        if (Geraet.istTablet) return
        bereich.launch {
            delay(5 * 60_000L)
            while (true) {
                runCatching { if (faellig(app)) sichere(app, "täglich") }
                delay(30 * 60_000L)
            }
        }
    }

    private fun faellig(c: Context): Boolean {
        val p = prefs(c)
        if (p.getString(K_ORDNER, null) == null || de.frank.jarvis.speech.GespraechsFokus.sprachmodus) return false
        val jetzt = System.currentTimeMillis()
        return jetzt - p.getLong(K_ERFOLG, 0L) > ABSTAND_MS && jetzt - p.getLong(K_VERSUCH, 0L) > NEUVERSUCH_MS
    }

    /** Für den Knopf „Jetzt sichern“. */
    fun jetzt(c: Context, fertig: (String) -> Unit) {
        val app = c.applicationContext
        bereich.launch { fertig(sichere(app, "von Hand")) }
    }

    /** Erstellt die Sicherung und legt sie in den Drive-Ordner. Rückgabe: Meldung für Frank. */
    suspend fun sichere(c: Context, anlass: String): String {
        if (!sperre.tryLock()) return "Eine Sicherung läuft schon."
        try {
            aktualisiere(c)
            val p = prefs(c)
            p.edit().putLong(K_VERSUCH, System.currentTimeMillis()).apply()
            val ordner = p.getString(K_ORDNER, null)?.let(Uri::parse) ?: return "Wähle zuerst den Ordner in Google Drive."
            val zip = File(c.cacheDir, "cloud-sicherung.zip")
            return try {
                val anzahl = packe(c, zip)
                val name = VORSILBE + ZonedDateTime.now().format(NAMENSFORM) + ".zip"
                val ordnerDok = DocumentsContract.buildDocumentUriUsingTree(ordner, DocumentsContract.getTreeDocumentId(ordner))
                val ziel = DocumentsContract.createDocument(c.contentResolver, ordnerDok, "application/zip", name)
                    ?: error("Im Drive-Ordner ließ sich keine Datei anlegen. Ordner neu wählen?")
                try {
                    c.contentResolver.openOutputStream(ziel, "w")?.use { aus -> FileInputStream(zip).use { it.copyTo(aus, 256 * 1024) } }
                        ?: error("Die Datei in Drive ließ sich nicht öffnen.")
                    val groesse = groesse(c, ziel)
                    check(groesse == null || groesse <= 0L || groesse == zip.length()) { "Drive meldet eine andere Größe ($groesse statt ${zip.length()} Byte)." }
                } catch (e: Exception) {
                    runCatching { DocumentsContract.deleteDocument(c.contentResolver, ziel) }
                    throw e
                }
                aufraeumen(c, ordner)
                val mb = "%.1f".format(zip.length() / 1_048_576.0)
                p.edit().putLong(K_ERFOLG, System.currentTimeMillis()).remove(K_FEHLER).commit()
                Logbuch.system("Cloud-Sicherung", "$anlass: $name, $anzahl Dateien, $mb MB")
                "Gesichert: $name ($anzahl Dateien, $mb MB)."
            } catch (e: Exception) {
                val text = e.message ?: e.javaClass.simpleName
                p.edit().putString(K_FEHLER, text).commit()
                Logbuch.fehler("Cloud-Sicherung", e, anlass)
                "Sicherung fehlgeschlagen: $text"
            } finally {
                zip.delete()
            }
        } finally {
            sperre.unlock()
            aktualisiere(c)
        }
    }

    private fun groesse(c: Context, uri: Uri): Long? = runCatching {
        c.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_SIZE), null, null, null)?.use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else null }
    }.getOrNull()

    /** Behält die neuesten [BEHALTEN] Sicherungen; ältere löschen (der Name trägt Datum und Uhrzeit, sortiert also richtig). */
    private fun aufraeumen(c: Context, ordner: Uri) = runCatching {
        val kinder = DocumentsContract.buildChildDocumentsUriUsingTree(ordner, DocumentsContract.getTreeDocumentId(ordner))
        val gefunden = mutableListOf<Pair<String, String>>()
        c.contentResolver.query(kinder, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            while (it.moveToNext()) { val n = it.getString(1).orEmpty(); if (n.startsWith(VORSILBE) && n.endsWith(".zip")) gefunden += it.getString(0) to n }
        }
        gefunden.sortedByDescending { it.second }.drop(BEHALTEN).forEach { (id, _) ->
            runCatching { DocumentsContract.deleteDocument(c.contentResolver, DocumentsContract.buildDocumentUriUsingTree(ordner, id)) }
        }
    }.onFailure { Logbuch.fehler("Cloud-Sicherung: alte Sicherungen löschen", it) }

    // ------------------------------------------------------------------ Packen

    private fun packe(c: Context, zip: File): Int {
        var anzahl = 0
        ZipOutputStream(zip.outputStream().buffered()).use { z ->
            fun datei(name: String, f: File) { z.putNextEntry(ZipEntry(name).apply { time = f.lastModified() }); f.inputStream().use { it.copyTo(z) }; z.closeEntry(); anzahl++ }
            fun text(name: String, inhalt: String) { z.putNextEntry(ZipEntry(name)); z.write(inhalt.toByteArray(Charsets.UTF_8)); z.closeEntry() }

            // 1. Alle Dateien unter files/
            val basis = c.filesDir
            basis.walkTopDown().onEnter { it == basis || it.relativeTo(basis).invariantSeparatorsPath.substringBefore('/') !in NIE_DATEIEN }.forEach { f ->
                if (!f.isFile) return@forEach
                val rel = f.relativeTo(basis).invariantSeparatorsPath
                if (rel.substringBefore('/') in NIE_DATEIEN || NEBENDATEI.any { rel.endsWith(it) }) return@forEach
                runCatching { datei("files/$rel", f) }.onFailure { Logbuch.fehler("Cloud-Sicherung: $rel", it) }
            }

            // 2. Datenbanken als stimmige Momentaufnahme
            val dbOrdner = c.getDatabasePath("x").parentFile
            dbOrdner?.listFiles()?.filter { it.isFile && it.name.endsWith(".db") }?.forEach { db ->
                val kopie = File(c.cacheDir, "sicherung-${db.name}")
                kopie.delete()
                val ok = runCatching {
                    SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READWRITE).use { it.execSQL("VACUUM INTO ?", arrayOf(kopie.path)) }
                    kopie.length() > 0
                }.getOrDefault(false)
                if (ok) datei("databases/${db.name}", kopie)
                else listOf("", "-wal", "-shm").map { File(db.path + it) }.filter { it.exists() }.forEach { datei("databases/${it.name}", it) }
                kopie.delete()
            }

            // 3. Einstellungen Wert für Wert, ohne Zugangsdaten
            val einstellungen = JSONObject()
            var ausgelassen = 0
            for (name in VERSCHLUESSELT) einstellungen.put(name, werte(Umzug.speicher(c, name)) { k -> Umzug.zieht(k) && !geheim(k).also { if (it) ausgelassen++ } })
            File(c.applicationInfo.dataDir, "shared_prefs").listFiles()?.map { it.name.removeSuffix(".xml") }
                ?.filter { it !in NIE_SPEICHER && it !in VERSCHLUESSELT && !it.startsWith("jarvis_konten") }?.forEach { name ->
                    val sp = c.getSharedPreferences(name, Context.MODE_PRIVATE)
                    // Mit Schlüsselbund = verschlüsselter Speicher eines anderen Bereichs: nicht lesbar und nicht übertragbar.
                    if (sp.all.keys.any { it.startsWith("__androidx_security_crypto") }) return@forEach
                    einstellungen.put(name, werte(sp) { k -> !geheim(k).also { if (it) ausgelassen++ } })
                }
            text("einstellungen.json", einstellungen.toString())
            text("sicherung.json", JSONObject().put("format", FORMAT).put("version", 1).put("app", BuildConfig.VERSION_NAME)
                .put("erstellt", ZonedDateTime.now().toString()).put("geraet", Geraet.kennung).put("dateien", anzahl)
                .put("hinweis", "API-Schlüssel, Tokens, Passwörter und Sitzungen sind bewusst nicht enthalten ($ausgelassen Werte ausgelassen).").toString(2))
        }
        return anzahl
    }

    private fun werte(sp: SharedPreferences, nimm: (String) -> Boolean): JSONObject {
        val o = JSONObject()
        for ((k, v) in sp.all) {
            if (v == null || !nimm(k)) continue
            val e = when (v) {
                is String -> JSONObject().put("t", "s").put("v", v)
                is Boolean -> JSONObject().put("t", "b").put("v", v)
                is Int -> JSONObject().put("t", "i").put("v", v)
                is Long -> JSONObject().put("t", "l").put("v", v)
                is Float -> JSONObject().put("t", "f").put("v", v.toString())
                is Set<*> -> JSONObject().put("t", "m").put("v", JSONArray(v.map { it.toString() }))
                else -> null
            } ?: continue
            o.put(k, e)
        }
        return o
    }

    // ------------------------------------------------------------------ Wiederherstellen

    /** Prüft die gewählte Sicherung und legt sie zum Einspielen beim nächsten Start bereit. Danach startet Jarvis neu. */
    fun wiederherstellenVorbereiten(c: Context, quelle: Uri, fertig: (String) -> Unit) {
        val app = c.applicationContext
        bereich.launch {
            val ziel = File(app.noBackupFilesDir, BREIT_TEIL)
            val meldung = runCatching {
                app.contentResolver.openInputStream(quelle)?.use { ein -> ziel.outputStream().use { ein.copyTo(it) } } ?: error("Datei nicht lesbar")
                val kopf = ZipInputStream(ziel.inputStream().buffered()).use { z ->
                    generateSequence { z.nextEntry }.firstOrNull { it.name == "sicherung.json" }?.let { JSONObject(z.readBytes().toString(Charsets.UTF_8)) }
                }
                check(kopf?.optString("format") == FORMAT) { "Das ist keine Jarvis-Sicherung." }
                check(ziel.renameTo(File(app.noBackupFilesDir, BEREIT))) { "Ließ sich nicht bereitlegen." }
                "Sicherung vom ${kopf!!.optString("erstellt").take(16).replace('T', ' ')} wird eingespielt, Jarvis startet neu …"
            }.getOrElse { ziel.delete(); "Wiederherstellen nicht möglich: ${it.message}" }
            fertig(meldung)
            if (File(app.noBackupFilesDir, BEREIT).exists()) {
                delay(2_500)
                Logbuch.system("Cloud-Sicherung", "Wiederherstellung bereitgelegt, Neustart")
                app.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                    runCatching { app.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)) }
                }
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
    private const val BREIT_TEIL = "wiederherstellung.teil"

    /**
     * Ganz am Anfang von [de.frank.jarvis.JarvisApplication.onCreate]: spielt eine bereitgelegte Sicherung ein, bevor Einstellungen,
     * Datenbanken oder Gedächtnis geöffnet sind. Die verschlüsselten Einstellungen gehen über [Umzug.uebernimm] (files/umzug.json).
     */
    fun einspielenBeimStart(c: Context) {
        val bereit = File(c.noBackupFilesDir, BEREIT)
        if (!bereit.exists()) return
        var dateien = 0
        try {
            val dbOrdner = c.getDatabasePath("x").parentFile!!.apply { mkdirs() }
            var einstellungen: JSONObject? = null
            ZipInputStream(bereit.inputStream().buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    if (e.isDirectory || e.name.contains("..")) continue
                    when {
                        e.name.startsWith("files/") -> schreibe(File(c.filesDir, e.name.removePrefix("files/")), z).also { dateien++ }
                        e.name.startsWith("databases/") -> {
                            val name = e.name.removePrefix("databases/")
                            if (name.endsWith(".db")) listOf("-wal", "-shm", "-journal").forEach { File(dbOrdner, name + it).delete() }
                            schreibe(File(dbOrdner, name), z); dateien++
                        }
                        e.name == "einstellungen.json" -> einstellungen = JSONObject(z.readBytes().toString(Charsets.UTF_8))
                    }
                }
            }
            einstellungen?.let { alle ->
                // Verschlüsselte Speicher über den Umzug (Wert für Wert in den Keystore dieses Geräts), die übrigen direkt.
                val umzug = JSONObject()
                for (name in alle.keys()) {
                    val werte = alle.optJSONObject(name) ?: continue
                    if (name in VERSCHLUESSELT) { umzug.put(name, werte); continue }
                    if (name in NIE_SPEICHER) continue
                    val ed = c.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
                    for (k in werte.keys()) {
                        val w = werte.optJSONObject(k) ?: continue
                        when (w.optString("t")) {
                            "s" -> ed.putString(k, w.optString("v"))
                            "b" -> ed.putBoolean(k, w.optBoolean("v"))
                            "i" -> ed.putInt(k, w.optInt("v"))
                            "l" -> ed.putLong(k, w.optLong("v"))
                            "f" -> w.optString("v").toFloatOrNull()?.let { ed.putFloat(k, it) }
                            "m" -> w.optJSONArray("v")?.let { a -> ed.putStringSet(k, (0 until a.length()).map { a.optString(it) }.toSet()) }
                        }
                    }
                    ed.commit()
                }
                File(c.filesDir, "umzug.json").writeText(umzug.toString())
            }
            Logbuch.system("Cloud-Sicherung", "Wiederhergestellt: $dateien Dateien. API-Schlüssel und Zugänge bitte neu eintragen.")
        } catch (e: Exception) {
            Logbuch.fehler("Cloud-Sicherung: Wiederherstellen", e)
        } finally {
            bereit.delete()
        }
    }

    private fun schreibe(ziel: File, z: ZipInputStream) {
        ziel.parentFile?.mkdirs()
        val teil = File(ziel.path + ".einspielen")
        teil.outputStream().use { z.copyTo(it) }
        if (!teil.renameTo(ziel)) { ziel.delete(); check(teil.renameTo(ziel)) { "${ziel.name} nicht schreibbar" } }
    }
}
