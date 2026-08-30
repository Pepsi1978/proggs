package de.frank.genialeideen.backup

import android.content.Context
import android.net.Uri
import de.frank.genialeideen.data.local.GenialeIdeenDatabase
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.data.local.Kategorieart
import de.frank.genialeideen.data.local.NachrichtEntity
import de.frank.genialeideen.observability.IdeenLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Was beim Einlesen drinsteckt — für die Vorschau vor dem Überschreiben. */
data class SicherungsVorschau(
    val ideen: Int,
    val nachrichten: Int,
    val bestehende: Int,
    val erstelltAm: String,
)

/**
 * Die Sicherung: ein Abzug des ganzen Bestands als Datei in einem selbst gewählten Ordner
 * (Baustein J) — in der Praxis ein Google-Drive-Ordner.
 *
 * API-Schlüssel gehören ausdrücklich **nicht** in die Sicherung.
 */
class Sicherung(
    private val context: Context,
    private val datenbank: GenialeIdeenDatabase,
) {
    private val datei = DateiSicherung(context)

    /** Der gemerkte Sicherungsordner — null, solange keiner gewählt wurde. */
    val sicherungsOrdner: Uri? get() = datei.ordner

    fun ordnerName(): String? = datei.ordnerName()

    fun merkeOrdner(uri: Uri) = datei.merkeOrdner(uri)

    fun vergissOrdner() = datei.vergissOrdner()

    suspend fun alsJson(): String = withContext(Dispatchers.IO) {
        val ideen = JSONArray()
        datenbank.ideenDao().alleEinmal().forEach { idee ->
            ideen.put(
                JSONObject()
                    .put("id", idee.id)
                    .put("titel", idee.titel)
                    .put("text", idee.text)
                    .put("status", idee.status)
                    .put("reihenfolge", idee.reihenfolge)
                    .put("angelegtAm", idee.angelegtAm)
                    .put("geaendertAm", idee.geaendertAm)
                    .put("umgesetztAm", idee.umgesetztAm ?: JSONObject.NULL)
                    .put("originalText", idee.originalText ?: JSONObject.NULL)
                    .put("kategorieId", idee.kategorieId ?: JSONObject.NULL),
            )
        }
        val kategorien = JSONArray()
        datenbank.kategorienDao().alleEinmal().forEach { kategorie ->
            kategorien.put(
                JSONObject()
                    .put("id", kategorie.id)
                    .put("name", kategorie.name)
                    .put("reihenfolge", kategorie.reihenfolge)
                    .put("art", kategorie.art.name),
            )
        }
        val nachrichten = JSONArray()
        datenbank.nachrichtenDao().alle().forEach { nachricht ->
            nachrichten.put(
                JSONObject()
                    .put("id", nachricht.id)
                    .put("ideeId", nachricht.ideeId)
                    .put("rolle", nachricht.rolle)
                    .put("text", nachricht.text)
                    .put("zeitpunkt", nachricht.zeitpunkt)
                    .put("unvollstaendig", nachricht.unvollstaendig),
            )
        }
        // Schlüssel bleiben aussen vor — sie gehören nicht in eine Sicherung (Baustein J.1).
        JSONObject()
            .put("schemaVersion", SCHEMA_VERSION)
            .put("erstelltAm", ZEIT.format(Date()))
            .put("ideen", ideen)
            .put("kategorien", kategorien)
            .put("nachrichten", nachrichten)
            .toString(2)
    }

    private suspend fun spieleEin(json: JSONObject, ersetzen: Boolean): Int {
        if (ersetzen) {
            datenbank.nachrichtenDao().alleLoeschen()
            datenbank.ideenDao().alleLoeschen()
        }
        // Die Kategorien zuerst — die Ideen zeigen mit ihrer Kennung auf sie.
        val kategorien = json.optJSONArray("kategorien") ?: JSONArray()
        val faecher = mutableListOf<KategorieEntity>()
        for (index in 0 until kategorien.length()) {
            val eintrag = kategorien.optJSONObject(index) ?: continue
            faecher += KategorieEntity(
                id = eintrag.optLong("id"),
                name = eintrag.optString("name"),
                reihenfolge = eintrag.optInt("reihenfolge"),
                art = runCatching {
                    Kategorieart.valueOf(eintrag.optString("art", Kategorieart.MENTAL.name))
                }.getOrDefault(Kategorieart.MENTAL),
            )
        }
        if (faecher.isNotEmpty()) datenbank.kategorienDao().einfuegenAlle(faecher)

        val ideen = json.optJSONArray("ideen") ?: JSONArray()
        val eingelesen = mutableListOf<IdeeEntity>()
        for (index in 0 until ideen.length()) {
            val eintrag = ideen.optJSONObject(index) ?: continue
            eingelesen += IdeeEntity(
                id = if (ersetzen) eintrag.optLong("id") else 0L,
                titel = eintrag.optString("titel"),
                text = eintrag.optString("text"),
                status = eintrag.optString("status"),
                reihenfolge = eintrag.optInt("reihenfolge"),
                angelegtAm = eintrag.optLong("angelegtAm", System.currentTimeMillis()),
                geaendertAm = eintrag.optLong("geaendertAm", System.currentTimeMillis()),
                umgesetztAm = eintrag.opt("umgesetztAm") as? Long,
                originalText = eintrag.optString("originalText").takeIf(String::isNotBlank),
                kategorieId = eintrag.optLong("kategorieId").takeIf { it > 0L },
            )
        }
        datenbank.ideenDao().einfuegenAlle(eingelesen)

        if (ersetzen) {
            val nachrichten = json.optJSONArray("nachrichten") ?: JSONArray()
            val liste = mutableListOf<NachrichtEntity>()
            for (index in 0 until nachrichten.length()) {
                val eintrag = nachrichten.optJSONObject(index) ?: continue
                liste += NachrichtEntity(
                    id = eintrag.optLong("id"),
                    ideeId = eintrag.optLong("ideeId"),
                    rolle = eintrag.optString("rolle"),
                    text = eintrag.optString("text"),
                    zeitpunkt = eintrag.optLong("zeitpunkt", System.currentTimeMillis()),
                    unvollstaendig = eintrag.optBoolean("unvollstaendig"),
                )
            }
            datenbank.nachrichtenDao().einfuegenAlle(liste)
        }
        IdeenLog.info(
            "Sicherung",
            "importiere",
            "Sicherung eingespielt",
            mapOf("ideen" to eingelesen.size, "ersetzt" to ersetzen),
        )
        return eingelesen.size
    }

    /** Eine unbekannte, höhere Fassung wird abgelehnt statt halb eingelesen. */
    private fun pruefeSchema(json: JSONObject) {
        val version = json.optInt("schemaVersion", 0)
        if (version > SCHEMA_VERSION) {
            error(
                "Diese Sicherung stammt aus einer neueren Fassung der App (Schema $version). " +
                    "Aktualisiere die App, bevor du sie einspielst.",
            )
        }
        if (!json.has("ideen")) error("In der Datei fehlt der Abschnitt „ideen“.")
    }

    // ---- Der eine Sicherungsweg: eine Datei im gemerkten Ordner ----

    /**
     * Schreibt den ganzen Bestand als neue Datei in den gemerkten Ordner und räumt dabei auf:
     * Es bleiben nur die aktuelle Sicherung und die eine davor.
     */
    suspend fun sichere(): String {
        val geschrieben = datei.schreibe(alsJson())
        IdeenLog.info("Sicherung", "sichere", "Sicherung geschrieben", mapOf("name" to geschrieben.name))
        return BackupStatus.describe(context)
    }

    /** Die jüngste Sicherung im Ordner — sie wird beim Wiederherstellen genommen. */
    suspend fun neuesteSicherung(): Sicherungsdatei? = datei.sicherungen().firstOrNull()

    /** Wie viele Sicherungen gerade im Ordner liegen. */
    suspend fun anzahlSicherungen(): Int = datei.sicherungen().size

    suspend fun stelleWiederHerAus(quelle: Uri, ersetzen: Boolean): Int {
        val json = JSONObject(datei.lies(quelle))
        pruefeSchema(json)
        return spieleEin(json, ersetzen)
    }

    /** Was in der Sicherung steckt — für die Rückfrage vor dem Einspielen. */
    suspend fun vorschauVon(quelle: Uri): SicherungsVorschau = withContext(Dispatchers.IO) {
        val json = JSONObject(datei.lies(quelle))
        pruefeSchema(json)
        SicherungsVorschau(
            ideen = json.optJSONArray("ideen")?.length() ?: 0,
            nachrichten = json.optJSONArray("nachrichten")?.length() ?: 0,
            bestehende = datenbank.ideenDao().alleEinmal().size,
            erstelltAm = json.optString("erstelltAm"),
        )
    }

    fun standText(): String = BackupStatus.describe(context)

    companion object {
        const val SCHEMA_VERSION = 3
        private val ZEIT = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)
    }
}
