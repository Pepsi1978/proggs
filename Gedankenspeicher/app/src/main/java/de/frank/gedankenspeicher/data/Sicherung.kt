package de.frank.gedankenspeicher.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import de.frank.gedankenspeicher.data.settings.Einstellungen
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject

/**
 * **F-17 — was eine Sicherung enthält und wie sie zurückkommt.**
 *
 * Vorher war die Sicherung eine blosse Kopie von `gedankenspeicher.db`. Das war aus drei
 * Gründen zu wenig, und alle drei kosteten echte Daten:
 *
 *  1. **Room läuft im WAL-Modus.** Frisch Geschriebenes steht zuerst in
 *     `gedankenspeicher.db-wal` und wandert erst später in die eigentliche Datei. Eine
 *     Kopie der `.db` allein enthält deshalb genau das nicht, was man zuletzt getan hat —
 *     die letzten Notizen und Auswertungen fehlten, und die Sicherung meldete trotzdem
 *     „Gesichert". Deshalb steht vor jedem Packen ein `wal_checkpoint(TRUNCATE)`.
 *  2. **Die Anhänge liegen als Dateien in `files/anhaenge`**, nicht in der Datenbank. Ohne
 *     sie kamen zwar die Notizen zurück, aber jedes Bild, jedes PDF und jede Sprachaufnahme
 *     darin war fort.
 *  3. **Die Einstellungen liegen in `EncryptedSharedPreferences`** — darunter der Auftrag
 *     der Textverbesserung, die Codex-Wahl und die Schlüssel. Nach einer Wiederherstellung
 *     stand die App wieder im Auslieferungszustand.
 *
 * Eine Sicherung ist deshalb jetzt ein **ZIP** mit Datenbank, Anhängen, Einstellungen und
 * einem Steckbrief. Die Dateinamen im Sicherungsordner bleiben dieselben wie bisher; welches
 * Format vorliegt, wird am Inhalt erkannt und nicht am Namen — eine alte Sicherung aus der
 * Zeit der blossen Datenbankkopie lässt sich damit weiterhin einspielen.
 */
object Sicherung {

    const val EINTRAG_DATENBANK = "datenbank.db"
    const val EINTRAG_EINSTELLUNGEN = "einstellungen.json"
    const val EINTRAG_CODEX = "codex.json"
    const val EINTRAG_STECKBRIEF = "steckbrief.json"
    const val ORDNER_ANHAENGE = "anhaenge/"

    /** Die Fassung des Sicherungsformats. Steigt sie, weiss eine ältere App Bescheid. */
    const val FORMAT = 1

    /** Was in einer Sicherung steckt — für die Prüfung vor dem Wiederherstellen. */
    data class Steckbrief(
        val format: Int,
        val erstelltAm: Long,
        val notizen: Int,
        val sitzungen: Int,
        val antworten: Int,
        val profile: Int,
        val anhaenge: Int,
        val hatEinstellungen: Boolean,
        val hatCodex: Boolean,
    ) {
        /** Ein Satz für die Rückfrage vor dem Wiederherstellen. */
        fun beschreibung(): String = buildString {
            append("$notizen Notizen, $sitzungen Sitzungen, $antworten Auswertungen")
            if (anhaenge > 0) append(", $anhaenge Anhänge")
            if (hatEinstellungen) append(", Einstellungen")
            if (hatCodex) append(", Codex-Anmeldung")
        }
    }

    // --- Packen ----------------------------------------------------------------------------

    /**
     * Schreibt eine vollständige Sicherung in [ziel].
     *
     * Der Checkpoint läuft **vor** dem Öffnen der Datei: erst danach steht in
     * `gedankenspeicher.db` wirklich alles, was die App weiss.
     *
     * @return der Steckbrief dessen, was geschrieben wurde
     */
    fun packe(
        ctx: Context,
        datenbank: Datenbank,
        einstellungen: Einstellungen,
        codexWerte: Map<String, Any?>,
        ziel: OutputStream,
    ): Steckbrief {
        checkpoint(datenbank)

        val db = ctx.getDatabasePath(Datenbank.DATEINAME)
        require(db.exists() && db.length() > 0) { "Die Datenbank ist leer." }

        val anhangordner = File(ctx.filesDir, Anhangsspeicher.ORDNER)
        val anhangdateien = anhangordner.listFiles()?.filter { it.isFile }.orEmpty()
        val einstellungswerte = einstellungen.alleWerte()

        val steckbrief = Steckbrief(
            format = FORMAT,
            erstelltAm = System.currentTimeMillis(),
            notizen = zaehle(datenbank, "notiz"),
            sitzungen = zaehle(datenbank, "sitzung"),
            antworten = zaehle(datenbank, "ki_antwort"),
            profile = zaehle(datenbank, "auswertungsprofil"),
            anhaenge = anhangdateien.size,
            hatEinstellungen = einstellungswerte.isNotEmpty(),
            hatCodex = codexWerte.isNotEmpty(),
        )

        ZipOutputStream(ziel.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(EINTRAG_STECKBRIEF))
            zip.write(steckbriefAlsJson(steckbrief).toByteArray())
            zip.closeEntry()

            zip.putNextEntry(ZipEntry(EINTRAG_DATENBANK))
            db.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()

            if (einstellungswerte.isNotEmpty()) {
                zip.putNextEntry(ZipEntry(EINTRAG_EINSTELLUNGEN))
                zip.write(werteAlsJson(einstellungswerte).toByteArray())
                zip.closeEntry()
            }

            if (codexWerte.isNotEmpty()) {
                zip.putNextEntry(ZipEntry(EINTRAG_CODEX))
                zip.write(werteAlsJson(codexWerte).toByteArray())
                zip.closeEntry()
            }

            anhangdateien.forEach { datei ->
                zip.putNextEntry(ZipEntry(ORDNER_ANHAENGE + datei.name))
                datei.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return steckbrief
    }

    /**
     * Schreibt den WAL-Puffer in die Datenbankdatei.
     *
     * `TRUNCATE` statt `PASSIVE`: nur so ist danach garantiert **alles** in der `.db`, auch
     * wenn nebenher noch jemand liest. Genau dieser eine Aufruf fehlte, und deshalb war
     * jede Sicherung um die zuletzt geschriebenen Einträge ärmer.
     */
    fun checkpoint(datenbank: Datenbank) {
        runCatching {
            datenbank.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { es ->
                es.moveToFirst()
            }
        }
    }

    private fun zaehle(datenbank: Datenbank, tabelle: String): Int = runCatching {
        datenbank.openHelper.writableDatabase.query("SELECT COUNT(*) FROM $tabelle").use { es ->
            if (es.moveToFirst()) es.getInt(0) else 0
        }
    }.getOrDefault(0)

    // --- Prüfen ----------------------------------------------------------------------------

    /** Was beim Prüfen einer Sicherungsdatei herauskommt. */
    sealed interface Befund {
        /** Ein vollständiges Archiv, entpackt nach [ordner]. */
        data class Archiv(val ordner: File, val steckbrief: Steckbrief) : Befund

        /** Eine alte Sicherung: nur die Datenbank, ohne Anhänge und Einstellungen. */
        data class NurDatenbank(val datei: File) : Befund

        data class Untauglich(val grund: String) : Befund
    }

    /**
     * Packt die gewählte Datei in [arbeitsordner] aus und prüft sie, **bevor** irgendetwas
     * am echten Bestand angefasst wird.
     *
     * Das ist der wichtigste Teil des Wiederherstellens: vorher wurde die laufende Datenbank
     * geschlossen und ersetzt und erst danach zeigte sich, ob die gewählte Datei überhaupt
     * etwas taugt. Griff man daneben, war der alte Stand mit fort.
     */
    fun pruefe(quelle: InputStream, arbeitsordner: File): Befund {
        arbeitsordner.deleteRecursively()
        arbeitsordner.mkdirs()

        val roh = File(arbeitsordner, "sicherung.roh")
        quelle.use { ein -> roh.outputStream().use { aus -> ein.copyTo(aus) } }
        if (roh.length() == 0L) return Befund.Untauglich("Die Sicherungsdatei ist leer.")

        return when (kopfzeile(roh)) {
            Kopf.ZIP -> pruefeArchiv(roh, arbeitsordner)
            Kopf.SQLITE -> {
                val fehler = pruefeDatenbank(roh)
                if (fehler != null) Befund.Untauglich(fehler) else Befund.NurDatenbank(roh)
            }
            Kopf.UNBEKANNT -> Befund.Untauglich(
                "Das ist keine Sicherung des Gedankenspeichers.",
            )
        }
    }

    private fun pruefeArchiv(roh: File, arbeitsordner: File): Befund {
        val ausgepackt = File(arbeitsordner, "inhalt").apply { mkdirs() }
        runCatching {
            ZipInputStream(roh.inputStream().buffered()).use { zip ->
                var eintrag: ZipEntry? = zip.nextEntry
                while (eintrag != null) {
                    val name = eintrag.name
                    // Ein Eintragsname darf nie aus dem Zielordner herausführen.
                    if (!name.contains("..") && !name.startsWith("/")) {
                        val ziel = File(ausgepackt, name)
                        if (eintrag.isDirectory) {
                            ziel.mkdirs()
                        } else {
                            ziel.parentFile?.mkdirs()
                            ziel.outputStream().use { aus -> zip.copyTo(aus) }
                        }
                    }
                    zip.closeEntry()
                    eintrag = zip.nextEntry
                }
            }
        }.onFailure { return Befund.Untauglich("Die Sicherung liess sich nicht öffnen.") }

        val db = File(ausgepackt, EINTRAG_DATENBANK)
        if (!db.exists()) return Befund.Untauglich("In der Sicherung fehlt die Datenbank.")
        pruefeDatenbank(db)?.let { return Befund.Untauglich(it) }

        val steckbrief = File(ausgepackt, EINTRAG_STECKBRIEF)
            .takeIf { it.exists() }
            ?.let { runCatching { steckbriefAusJson(it.readText()) }.getOrNull() }
            ?: Steckbrief(
                format = FORMAT,
                erstelltAm = 0L,
                notizen = 0,
                sitzungen = 0,
                antworten = 0,
                profile = 0,
                anhaenge = File(ausgepackt, ORDNER_ANHAENGE).listFiles()?.size ?: 0,
                hatEinstellungen = File(ausgepackt, EINTRAG_EINSTELLUNGEN).exists(),
                hatCodex = File(ausgepackt, EINTRAG_CODEX).exists(),
            )

        if (steckbrief.format > FORMAT) {
            return Befund.Untauglich(
                "Diese Sicherung stammt aus einer neueren Fassung der App.",
            )
        }
        return Befund.Archiv(ausgepackt, steckbrief)
    }

    /**
     * Öffnet die Datei als SQLite und lässt sie sich selbst prüfen.
     *
     * @return null, wenn alles stimmt, sonst der Satz, der Frank angezeigt wird
     */
    private fun pruefeDatenbank(datei: File): String? {
        if (kopfzeile(datei) != Kopf.SQLITE) return "Die Datei ist keine Datenbank."
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                datei.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            val heil = db.rawQuery("PRAGMA integrity_check", null).use { es ->
                es.moveToFirst() && es.getString(0).equals("ok", ignoreCase = true)
            }
            if (!heil) return "Die Sicherung ist beschädigt."
            val hatNotizen = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='notiz'",
                null,
            ).use { it.moveToFirst() }
            if (!hatNotizen) return "Das ist keine Sicherung des Gedankenspeichers."
            val fassung = db.rawQuery("PRAGMA user_version", null).use { es ->
                if (es.moveToFirst()) es.getInt(0) else 0
            }
            if (fassung > SCHEMA_FASSUNG) {
                return "Diese Sicherung stammt aus einer neueren Fassung der App."
            }
            return null
        } catch (fehler: Exception) {
            return "Die Sicherung liess sich nicht lesen: ${fehler.message}"
        } finally {
            runCatching { db?.close() }
        }
    }

    private enum class Kopf { ZIP, SQLITE, UNBEKANNT }

    private fun kopfzeile(datei: File): Kopf {
        val kopf = ByteArray(16)
        val gelesen = datei.inputStream().use { it.read(kopf) }
        if (gelesen < 4) return Kopf.UNBEKANNT
        if (kopf[0] == 'P'.code.toByte() && kopf[1] == 'K'.code.toByte()) return Kopf.ZIP
        if (gelesen >= 15 && String(kopf, 0, 15) == "SQLite format 3") return Kopf.SQLITE
        return Kopf.UNBEKANNT
    }

    // --- Werte als JSON --------------------------------------------------------------------

    /**
     * Die Einstellungen kommen als Map heraus und müssen mit ihrem Typ zurückkommen — ein
     * `Boolean`, der als Zeichenkette zurückgeschrieben wird, lässt
     * `SharedPreferences.getBoolean` beim nächsten Lesen fliegen.
     */
    fun werteAlsJson(werte: Map<String, Any?>): String {
        val wurzel = JSONObject()
        werte.forEach { (schluessel, wert) ->
            val eintrag = JSONObject()
            when (wert) {
                is Boolean -> { eintrag.put("typ", "boolean"); eintrag.put("wert", wert) }
                is Int -> { eintrag.put("typ", "int"); eintrag.put("wert", wert) }
                is Long -> { eintrag.put("typ", "long"); eintrag.put("wert", wert) }
                is Float -> { eintrag.put("typ", "float"); eintrag.put("wert", wert.toDouble()) }
                is String -> { eintrag.put("typ", "string"); eintrag.put("wert", wert) }
                else -> return@forEach
            }
            wurzel.put(schluessel, eintrag)
        }
        return wurzel.toString()
    }

    fun werteAusJson(roh: String): Map<String, Any> = runCatching {
        val wurzel = JSONObject(roh)
        buildMap {
            wurzel.keys().forEach { schluessel ->
                val eintrag = wurzel.optJSONObject(schluessel) ?: return@forEach
                when (eintrag.optString("typ")) {
                    "boolean" -> put(schluessel, eintrag.optBoolean("wert"))
                    "int" -> put(schluessel, eintrag.optInt("wert"))
                    "long" -> put(schluessel, eintrag.optLong("wert"))
                    "float" -> put(schluessel, eintrag.optDouble("wert").toFloat())
                    "string" -> put(schluessel, eintrag.optString("wert"))
                }
            }
        }
    }.getOrDefault(emptyMap())

    private fun steckbriefAlsJson(s: Steckbrief): String = JSONObject().apply {
        put("format", s.format)
        put("erstelltAm", s.erstelltAm)
        put("notizen", s.notizen)
        put("sitzungen", s.sitzungen)
        put("antworten", s.antworten)
        put("profile", s.profile)
        put("anhaenge", s.anhaenge)
        put("hatEinstellungen", s.hatEinstellungen)
        put("hatCodex", s.hatCodex)
    }.toString()

    private fun steckbriefAusJson(roh: String): Steckbrief {
        val o = JSONObject(roh)
        return Steckbrief(
            format = o.optInt("format", FORMAT),
            erstelltAm = o.optLong("erstelltAm"),
            notizen = o.optInt("notizen"),
            sitzungen = o.optInt("sitzungen"),
            antworten = o.optInt("antworten"),
            profile = o.optInt("profile"),
            anhaenge = o.optInt("anhaenge"),
            hatEinstellungen = o.optBoolean("hatEinstellungen"),
            hatCodex = o.optBoolean("hatCodex"),
        )
    }

    /** Muss mit der `version` in [Datenbank] übereinstimmen. */
    private const val SCHEMA_FASSUNG = 5
}
