// Anbindung für Modul M1.1 — Sicherung.
// Diese Datei gehört Gedankenspeicher und wird beim Nachziehen NICHT überschrieben.
package de.frank.module.sicherung

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.JsonReader
import android.util.JsonWriter
import de.frank.gedankenspeicher.auth.CodexAuthManager
import de.frank.gedankenspeicher.data.Datenbank
import de.frank.gedankenspeicher.data.Sicherung
import de.frank.gedankenspeicher.data.settings.Einstellungen
import de.frank.gedankenspeicher.data.BestandsImport
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SpeicherTeil(
    override val id: String,
    override val titel: String,
    override val erklaerung: String,
) : SicherungsTeil {
    BESTAND("bestand", "Gedanken und Anhänge", "Sitzungen, Kategorien, Notizen, Auswertungen, Profile, Papierkorb und alle Anhangsdateien einschließlich wartender Aufnahmen."),
    EINSTELLUNGEN("einstellungen", "Einstellungen", "Erscheinung, Modelle, Prompts, Stimmenwahl und hinterlegte API-Schlüssel."),
    CODEX("codex", "Codex-Anmeldung", "Die gespeicherte Anmeldung, damit Codex nach dem Einspielen weiter benutzt werden kann."),
}

/** Der alte ZIP-Inhalt bleibt app-eigene Nutzlast, der JSON-Umschlag gehört M1.1.
 * Kleine Base64-Blöcke vermeiden, dass Anhänge komplett im Arbeitsspeicher landen.
 * Erst schliesseAb darf den vollständig geprüften Inhalt in die App übernehmen.
 */
class SpeicherInhalt(
    private val ctx: Context,
    private val db: Datenbank,
    private val einstellungen: Einstellungen,
    private val codex: CodexAuthManager,
) : SicherungsInhalt, SicherungsRuecknahme {
    override val teile: List<SicherungsTeil> = SpeicherTeil.entries
    override val produkt = "Gedankenspeicher"
    override val datenmodellVersion = 6
    private val import = BestandsImport(ctx, db, einstellungen, codex)
    private var vorbereitet: File? = null
    private class Spur(var importiert: BestandsImport.Spur? = null) : Einspielspur

    override suspend fun schreibeNutzlast(
        schreiber: JsonWriter, umfang: Set<SicherungsTeil>, pruefsumme: Inhaltspruefsumme,
    ): Nutzlastzahlen {
        val archiv = File.createTempFile("m11-export-", ".zip", ctx.cacheDir)
        try {
            archiv.outputStream().use {
                Sicherung.packe(ctx, db, einstellungen, codex.alleWerte(), it,
                    bestand = SpeicherTeil.BESTAND in umfang,
                    mitEinstellungen = SpeicherTeil.EINSTELLUNGEN in umfang,
                    mitCodex = SpeicherTeil.CODEX in umfang)
            }
            return schreibeArchiv(archiv, schreiber, pruefsumme)
        } finally { archiv.delete() }
    }

    private fun schreibeArchiv(archiv: File, schreiber: JsonWriter, summe: Inhaltspruefsumme): Nutzlastzahlen {
        var bloecke = 0
        schreiber.name("archiv").beginArray()
        archiv.inputStream().buffered().use { ein ->
            val puffer = ByteArray(48 * 1024)
            while (true) {
                val n = ein.read(puffer)
                if (n < 0) break
                val text = Base64.encodeToString(puffer, 0, n, Base64.NO_WRAP)
                schreiber.value(text)
                summe.nimm(text)
                bloecke++
            }
        }
        schreiber.endArray()
        return Nutzlastzahlen(mapOf("Archivblöcke" to bloecke))
    }

    override suspend fun liesNutzlast(
        feld: String, leser: JsonReader, pruefsumme: Inhaltspruefsumme, einspielen: Boolean,
    ): Nutzlastzahlen? {
        if (feld != "archiv") return null
        val archiv = File.createTempFile("m11-import-", ".zip", ctx.cacheDir)
        var behalten = false
        try {
            var bloecke = 0
            archiv.outputStream().buffered().use { aus ->
                leser.beginArray()
                while (leser.hasNext()) {
                    val text = leser.nextString()
                    require(text.length <= 65536) { "Ungültiger Archivblock." }
                    pruefsumme.nimm(text)
                    aus.write(Base64.decode(text, Base64.NO_WRAP))
                    bloecke++
                }
                leser.endArray()
            }
            // Auch die innere Datenbank und Einstellungswerte prüfen, nicht nur den Umschlag.
            beschreibeArchiv(archiv)
            if (einspielen) {
                vorbereitet?.delete()
                vorbereitet = archiv
                behalten = true
            }
            return Nutzlastzahlen(mapOf("Archivblöcke" to bloecke))
        } finally { if (!behalten) archiv.delete() }
    }

    fun beschreibeArchiv(archiv: File): String {
        val ordner = File.createTempFile("m11-pruefung-", "", ctx.cacheDir).apply { delete(); mkdirs() }
        try {
            return when (val befund = archiv.inputStream().use { Sicherung.pruefe(it, ordner, ctx) }) {
                is Sicherung.Befund.Untauglich -> error(befund.grund)
                is Sicherung.Befund.NurDatenbank -> "Ältere Datenbanksicherung (ohne Anhänge und Einstellungen)"
                is Sicherung.Befund.Archiv -> {
                    File(befund.ordner, Sicherung.EINTRAG_EINSTELLUNGEN).takeIf(File::exists)?.let {
                        einstellungen.pruefeWerte(Sicherung.werteAusJson(it.readText()))
                    }
                    File(befund.ordner, Sicherung.EINTRAG_CODEX).takeIf(File::exists)?.let {
                        codex.pruefeWerte(Sicherung.werteAusJson(it.readText()))
                    }
                    befund.steckbrief.beschreibung()
                }
            }
        } finally { ordner.deleteRecursively() }
    }

    /** Lokale, unveränderliche Kopie für Vorschau UND Einspielen; alte Formate werden umhüllt. */
    suspend fun bereiteVor(uri: Uri): Pair<File, String> = withContext(Dispatchers.IO) {
        val roh = File.createTempFile("m11-auswahl-", ".json", ctx.cacheDir)
        try {
            ctx.contentResolver.openInputStream(uri)?.use { ein -> roh.outputStream().use { ein.copyTo(it) } }
                ?: error("Die Sicherung ließ sich nicht öffnen.")
            val kopf = roh.inputStream().use { it.read() }
            if (kopf == 'P'.code || kopf == 'S'.code) {
                val beschreibung = beschreibeArchiv(roh)
                val json = File.createTempFile("m11-alt-", ".json", ctx.cacheDir)
                try {
                    val alt = object : SicherungsInhalt by this@SpeicherInhalt {
                        override suspend fun schreibeNutzlast(schreiber: JsonWriter, umfang: Set<SicherungsTeil>, pruefsumme: Inhaltspruefsumme) =
                            schreibeArchiv(roh, schreiber, pruefsumme)
                    }
                    json.bufferedWriter().use { Sicherungsrahmen(alt).schreibe(it, "älteres Format", teile.toSet()) }
                    roh.delete()
                    json to beschreibung
                } catch (fehler: Exception) { json.delete(); throw fehler }
            } else {
                val vorschau = roh.bufferedReader().use { Sicherungsrahmen(this@SpeicherInhalt).pruefe(it) }
                roh to fasseZusammen(vorschau)
            }
        } catch (fehler: Exception) { roh.delete(); throw fehler }
    }

    override suspend fun zaehle(umfang: Set<SicherungsTeil>): Nutzlastzahlen = withContext(Dispatchers.IO) {
        val zahlen = linkedMapOf<String, Int>()
        if (SpeicherTeil.BESTAND in umfang) {
            mapOf("notiz" to "Notizen", "sitzung" to "Sitzungen", "ki_antwort" to "Auswertungen", "ordner" to "Kategorien", "auswertungsprofil" to "Profile").forEach { (tabelle, name) ->
                db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $tabelle").use {
                    check(it.moveToFirst()); zahlen[name] = it.getInt(0)
                }
            }
            zahlen["Anhangsdateien"] = File(ctx.filesDir, "anhaenge").listFiles()?.count(File::isFile) ?: 0
            zahlen["wartende Aufnahmen"] = File(ctx.filesDir, "wartend").listFiles()?.count(File::isFile) ?: 0
        }
        if (SpeicherTeil.EINSTELLUNGEN in umfang) zahlen["Einstellungen"] = einstellungen.alleWerte().size
        if (SpeicherTeil.CODEX in umfang) zahlen["Codex-Anmeldung"] = if (codex.isConnected) 1 else 0
        Nutzlastzahlen(zahlen)
    }

    override fun schaetzeGroesse(zahlen: Nutzlastzahlen): Long =
        if ("Notizen" in zahlen.anzahl) {
            (ctx.getDatabasePath(Datenbank.DATEINAME).length() +
                listOf("anhaenge", "wartend").sumOf { name -> File(ctx.filesDir, name).listFiles()?.sumOf(File::length) ?: 0L }) * 4 / 3
        } else 4096L

    override fun fasseZusammen(vorschau: SicherungsVorschau) =
        "Sicherung vom ${vorschau.erstelltAm}: " + teile.filter { it.id in vorschau.umfang }.joinToString { it.titel } +
            ". Fehlende Gedanken werden ergänzt, vorhandene bleiben erhalten. Enthaltene Einstellungen und Profile werden übernommen."

    override suspend fun beginne(): Einspielspur {
        vorbereitet?.delete(); vorbereitet = null
        return Spur()
    }

    override suspend fun schliesseAb(spur: Einspielspur) {
        val archiv = vorbereitet ?: error("Der Sicherungsinhalt fehlt.")
        try { (spur as Spur).importiert = import.spieleEin(archiv) }
        finally { archiv.delete(); vorbereitet = null }
    }

    override suspend fun nimmZurueck(spur: Einspielspur): Int =
        import.nimmZurueck((spur as Spur).importiert ?: error("Keine Einspielspur vorhanden."))

    fun aufraeumen() { vorbereitet?.delete(); vorbereitet = null }
}
