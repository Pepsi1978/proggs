package de.frank.gedankenspeicher.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import de.frank.gedankenspeicher.auth.CodexAuthManager
import de.frank.gedankenspeicher.data.settings.Einstellungen
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** App-eigene M1.1-Anbindung: ergänzt den Bestand statt eine offene Room-Datei auszutauschen.
 * Fremde numerische IDs werden auf lokale IDs abgebildet. Vorhandene Gedanken bleiben stehen.
 * Die Rücknahme kennt nur diesen Import und lässt spätere Bearbeitungen unangetastet.
 */
class BestandsImport(
    private val ctx: Context,
    private val db: Datenbank,
    private val einstellungen: Einstellungen,
    private val codex: CodexAuthManager,
) {
    data class Satz(val tabelle: String, val schluessel: String, val id: Long, val danach: ContentValues, val davor: ContentValues? = null)
    data class Spur(
        val saetze: MutableList<Satz> = mutableListOf(),
        val dateien: MutableList<File> = mutableListOf(),
        val einstellungenDavor: Map<String, Any?>,
        val codexDavor: Map<String, Any?>,
        var einstellungenDanach: Map<String, Any> = emptyMap(),
        var codexDanach: Map<String, Any> = emptyMap(),
    )

    suspend fun spieleEin(archiv: File): Spur = withContext(Dispatchers.IO) {
        val ordner = File.createTempFile("m11-bestand-", "", ctx.cacheDir).apply { delete(); mkdirs() }
        val spur = Spur(einstellungenDavor = einstellungen.alleWerte(), codexDavor = codex.alleWerte())
        try {
            val befund = archiv.inputStream().use { Sicherung.pruefe(it, ordner, ctx) }
            require(befund !is Sicherung.Befund.Untauglich) { (befund as Sicherung.Befund.Untauglich).grund }
            val inhalt = (befund as? Sicherung.Befund.Archiv)?.ordner
            val quelle = when (befund) {
                is Sicherung.Befund.NurDatenbank -> befund.datei
                is Sicherung.Befund.Archiv -> File(befund.ordner, Sicherung.EINTRAG_DATENBANK).takeIf { befund.steckbrief.hatBestand }
                else -> null
            }
            val einstellungswerte = inhalt?.let { File(it, Sicherung.EINTRAG_EINSTELLUNGEN) }?.takeIf(File::exists)
                ?.let { Sicherung.werteAusJson(it.readText()) }.orEmpty()
                .filterKeys { it !in setOf("drive_ordner", "drive_zeit", "drive_groesse", "drive_an", "offene_sitzung") }
            val codexwerte = inhalt?.let { File(it, Sicherung.EINTRAG_CODEX) }?.takeIf(File::exists)
                ?.let { Sicherung.werteAusJson(it.readText()) }.orEmpty()
            einstellungen.pruefeWerte(einstellungswerte)
            codex.pruefeWerte(codexwerte)
            val pfade = mutableMapOf<String, String>()
            if (quelle != null && inhalt != null) {
                listOf("anhaenge", "wartend").forEach { name ->
                    val zielordner = File(ctx.filesDir, name).apply { mkdirs() }
                    File(inhalt, name).listFiles()?.filter(File::isFile)?.forEach { datei ->
                        var ziel = File(zielordner, datei.name)
                        if (ziel.exists() && digest(ziel) != digest(datei)) {
                            ziel = File(zielordner, "${digest(datei).take(24)}-${datei.name}")
                        }
                        if (!ziel.exists()) {
                            spur.dateien.add(ziel)
                            datei.copyTo(ziel)
                        }
                        pfade["$name/${datei.name}"] = ziel.absolutePath
                    }
                }
            }
            // Keine suspend-Aufrufe innerhalb der Room-Transaktion.
            db.runInTransaction {
                if (quelle != null) SQLiteDatabase.openDatabase(quelle.path, null, SQLiteDatabase.OPEN_READONLY).use { alt ->
                    val ordnerIds = importiere(alt, "ordner", spur) { }
                    val sitzungsIds = importiere(alt, "sitzung", spur) { werte ->
                        werte.getAsLong("ordnerId")?.let { id -> werte.put("ordnerId", ordnerIds[id]) }
                    }
                    listOf("notiz", "ki_antwort").forEach { tabelle ->
                        importiere(alt, tabelle, spur) { werte ->
                            werte.put("sitzungId", sitzungsIds[werte.getAsLong("sitzungId")] ?: error("Die zugehörige Sitzung fehlt."))
                            if (tabelle == "notiz") {
                                werte.getAsString("audioPfad")?.let { werte.put("audioPfad", neuerPfad(it, pfade)) }
                                werte.put("anhaengeJson", anhaengeAusJson(werte.getAsString("anhaengeJson")).map {
                                    it.copy(pfad = neuerPfad(it.pfad, pfade), vorschauPfad = neuerPfad(it.vorschauPfad, pfade))
                                }.alsJson())
                            }
                        }
                    }
                    alt.rawQuery("SELECT * FROM auswertungsprofil ORDER BY nummer", null).use { c ->
                        while (c.moveToNext()) {
                            val werte = werte(c)
                            val nummer = werte.getAsLong("nummer")
                            require(nummer in 1L..6L) { "Ungültige Profilnummer." }
                            val davor = hole("auswertungsprofil", "nummer", nummer)
                            if (davor != null) db.openHelper.writableDatabase.update("auswertungsprofil", SQLiteDatabase.CONFLICT_ABORT, werte, "nummer=?", arrayOf(nummer))
                            else db.openHelper.writableDatabase.insert("auswertungsprofil", SQLiteDatabase.CONFLICT_ABORT, werte)
                            spur.saetze += Satz("auswertungsprofil", "nummer", nummer, werte, davor)
                        }
                    }
                }
                spur.einstellungenDanach = einstellungswerte
                spur.codexDanach = codexwerte
                einstellungen.uebernimm(einstellungswerte)
                codex.uebernimm(codexwerte)
            }
            db.invalidationTracker.refreshVersionsAsync()
            spur
        } catch (fehler: Exception) {
            stelleWerteZurueck(spur)
            spur.dateien.forEach(File::delete)
            throw fehler
        } finally { ordner.deleteRecursively() }
    }

    private fun importiere(alt: SQLiteDatabase, tabelle: String, spur: Spur, anpassen: (ContentValues) -> Unit): Map<Long, Long> {
        val ids = mutableMapOf<Long, Long>()
        alt.rawQuery("SELECT * FROM $tabelle ORDER BY id", null).use { cursor ->
            while (cursor.moveToNext()) {
                val werte = werte(cursor)
                val alteId = werte.getAsLong("id")
                anpassen(werte)
                val zeit = werte.getAsLong("erstelltAm")
                val sitzung = werte.getAsLong("sitzungId")
                val bedingung = "erstelltAm=?" + if (sitzung != null) " AND sitzungId=?" else ""
                val argumente: Array<Any> = if (sitzung != null) arrayOf(zeit, sitzung) else arrayOf(zeit)
                val vorhanden = db.openHelper.readableDatabase.query("SELECT * FROM $tabelle WHERE $bedingung ORDER BY id", argumente).use { c ->
                    var treffer: Long? = null
                    while (c.moveToNext()) {
                        val kandidat = werte(c)
                        // Dieselbe ursprüngliche ID + Entstehung oder ein bereits importierter Satz.
                        if (kandidat.getAsLong("id") == alteId || gleichOhneId(kandidat, werte)) {
                            treffer = kandidat.getAsLong("id"); break
                        }
                    }
                    treffer
                }
                if (vorhanden != null) ids[alteId] = vorhanden
                else {
                    werte.remove("id")
                    val neueId = db.openHelper.writableDatabase.insert(tabelle, SQLiteDatabase.CONFLICT_ABORT, werte)
                    werte.put("id", neueId)
                    ids[alteId] = neueId
                    spur.saetze += Satz(tabelle, "id", neueId, ContentValues(werte))
                }
            }
        }
        return ids
    }

    suspend fun nimmZurueck(spur: Spur): Int = withContext(Dispatchers.IO) {
        var zahl = 0
        db.runInTransaction {
            spur.saetze.asReversed().forEach { satz ->
                val jetzt = hole(satz.tabelle, satz.schluessel, satz.id)
                if (jetzt != null && gleich(jetzt, satz.danach)) {
                    // Keine seit dem Import neu hinzugekommenen Kinder kaskadierend löschen.
                    val kinder = when (satz.tabelle) {
                        "sitzung" -> "SELECT id FROM notiz WHERE sitzungId=${satz.id} UNION ALL SELECT id FROM ki_antwort WHERE sitzungId=${satz.id}"
                        "ordner" -> "SELECT id FROM sitzung WHERE ordnerId=${satz.id}"
                        else -> null
                    }
                    val hatKinder = kinder?.let { db.openHelper.readableDatabase.query(it).use(Cursor::moveToFirst) } ?: false
                    if (!hatKinder) {
                        if (satz.davor == null) db.openHelper.writableDatabase.delete(satz.tabelle, "${satz.schluessel}=?", arrayOf(satz.id))
                        else db.openHelper.writableDatabase.update(satz.tabelle, SQLiteDatabase.CONFLICT_ABORT, satz.davor, "${satz.schluessel}=?", arrayOf(satz.id))
                        zahl++
                    }
                }
            }
            stelleWerteZurueck(spur)
        }
        spur.dateien.forEach { datei ->
            val benutzt = db.openHelper.readableDatabase.query(
                "SELECT id FROM notiz WHERE audioPfad=? OR instr(anhaengeJson,?)>0 LIMIT 1", arrayOf(datei.path, datei.name),
            ).use(Cursor::moveToFirst)
            if (!benutzt) datei.delete()
        }
        db.invalidationTracker.refreshVersionsAsync()
        zahl
    }

    private fun stelleWerteZurueck(spur: Spur) {
        fun passende(davor: Map<String, Any?>, danach: Map<String, Any>, jetzt: Map<String, Any?>) =
            danach.filter { (key, value) -> jetzt[key] == value }.keys.associateWith { davor[it] }
        val e = passende(spur.einstellungenDavor, spur.einstellungenDanach, einstellungen.alleWerte())
        val c = passende(spur.codexDavor, spur.codexDanach, codex.alleWerte())
        einstellungen.entferneSicherungswerte(e.filterValues { it == null }.keys)
        codex.entferneSicherungswerte(c.filterValues { it == null }.keys)
        einstellungen.uebernimm(e.mapNotNull { (k, v) -> v?.let { k to it } }.toMap())
        codex.uebernimm(c.mapNotNull { (k, v) -> v?.let { k to it } }.toMap())
    }

    private fun hole(tabelle: String, key: String, id: Long): ContentValues? =
        db.openHelper.readableDatabase.query("SELECT * FROM $tabelle WHERE $key=?", arrayOf(id)).use { if (it.moveToFirst()) werte(it) else null }

    private fun neuerPfad(pfad: String, pfade: Map<String, String>): String {
        if (pfad.isBlank()) return pfad
        val datei = File(pfad)
        val ordner = datei.parentFile?.name ?: return pfad
        return pfade["$ordner/${datei.name}"] ?: if (ordner in listOf("anhaenge", "wartend")) File(ctx.filesDir, "$ordner/${datei.name}").path else pfad
    }

    private fun gleichOhneId(a: ContentValues, b: ContentValues) =
        b.keySet().filter { it != "id" }.all { a.getAsString(it) == b.getAsString(it) }
    private fun gleich(a: ContentValues, b: ContentValues) = a.keySet() == b.keySet() && a.keySet().all { a.getAsString(it) == b.getAsString(it) }

    private fun werte(c: Cursor) = ContentValues().apply {
        c.columnNames.forEachIndexed { i, name ->
            when (c.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> putNull(name)
                Cursor.FIELD_TYPE_INTEGER -> put(name, c.getLong(i))
                Cursor.FIELD_TYPE_FLOAT -> put(name, c.getDouble(i))
                Cursor.FIELD_TYPE_BLOB -> put(name, c.getBlob(i))
                else -> put(name, c.getString(i))
            }
        }
    }

    private fun digest(file: File): String {
        val summe = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { ein ->
            val puffer = ByteArray(65536)
            while (true) { val n = ein.read(puffer); if (n < 0) break; summe.update(puffer, 0, n) }
        }
        return summe.digest().joinToString("") { "%02x".format(it) }
    }
}
