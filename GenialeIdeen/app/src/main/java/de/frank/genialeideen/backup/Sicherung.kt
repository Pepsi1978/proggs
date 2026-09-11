package de.frank.genialeideen.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import de.frank.genialeideen.data.local.GenialeIdeenDatabase
import de.frank.genialeideen.data.local.IdeeEntity
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.KategorieEntity
import de.frank.genialeideen.data.local.Kategorieart
import de.frank.genialeideen.data.local.NachrichtEntity
import de.frank.genialeideen.data.local.alleKategorieIds
import de.frank.genialeideen.data.local.weitereKategorieIds
import de.frank.genialeideen.data.local.weitereKategorienText
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
    /** So viele Ideen der Sicherung fehlen gerade in der App — nur sie werden eingespielt. */
    val neu: Int,
    val nachrichten: Int,
    val bestehende: Int,
    val erstelltAm: String,
)

/** Was das Einspielen bewirkt hat. */
data class Ergebnis(val neu: Int, val schonDa: Int)

/** Titel und Text ohne Unterschiede in Gross-/Kleinschreibung und Leerraum. */
private fun inhaltsSchluessel(idee: IdeeEntity): String {
    fun glatt(text: String) = text.trim().replace(Regex("""\s+"""), " ").lowercase()
    // Zeilenumbruch als Trenner: glatt() lässt keinen übrig, Titel und Text vermischen sich nie.
    return glatt(idee.titel) + "\n" + glatt(idee.text)
}

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
                    .put("kategorieId", idee.kategorieId ?: JSONObject.NULL)
                    .put("weitereKategorien", idee.weitereKategorien),
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

    /** Eine Idee aus der Sicherung, noch mit den Kennungen der Sicherung. */
    private class GesicherteIdee(val alteId: Long, val idee: IdeeEntity)

    private fun liesIdeen(json: JSONObject): List<GesicherteIdee> {
        val ideen = json.optJSONArray("ideen") ?: JSONArray()
        val gueltig = IdeenStatus.entries.map { it.name }.toSet()
        val jetzt = System.currentTimeMillis()
        return (0 until ideen.length()).mapNotNull { index ->
            val eintrag = ideen.optJSONObject(index) ?: return@mapNotNull null
            GesicherteIdee(
                alteId = eintrag.optLong("id"),
                idee = IdeeEntity(
                    titel = eintrag.optString("titel"),
                    text = eintrag.optString("text"),
                    status = eintrag.optString("status").takeIf { it in gueltig } ?: IdeenStatus.OFFEN.name,
                    reihenfolge = eintrag.optInt("reihenfolge"),
                    // Ohne eigenen Zeitpunkt bekäme jede Idee dasselbe „jetzt“ und der Abgleich
                    // hielte alle bis auf die erste für schon vorhanden — darum je Eintrag einer.
                    angelegtAm = eintrag.optLong("angelegtAm", -1L).takeIf { it > 0 } ?: (jetzt + index),
                    geaendertAm = eintrag.optLong("geaendertAm", -1L).takeIf { it > 0 } ?: (jetzt + index),
                    umgesetztAm = if (eintrag.isNull("umgesetztAm")) null else eintrag.optLong("umgesetztAm"),
                    originalText = if (eintrag.isNull("originalText")) {
                        null
                    } else {
                        eintrag.optString("originalText").takeIf(String::isNotBlank)
                    },
                    kategorieId = if (eintrag.isNull("kategorieId")) null else eintrag.optLong("kategorieId").takeIf { it > 0L },
                    weitereKategorien = eintrag.optString("weitereKategorien"),
                ),
            )
        }
    }

    /**
     * Wer schon da ist, kommt nicht noch einmal dazu: gleicher Anlagezeitpunkt (dieselbe Idee,
     * auch wenn sie seither bearbeitet wurde) oder gleicher Titel und Text.
     */
    private class Abgleich(bestehende: List<IdeeEntity>) {
        private val zeitpunkte = bestehende.map { it.angelegtAm }.toMutableSet()
        private val inhalte = bestehende.map { inhaltsSchluessel(it) }.toMutableSet()

        fun istNeu(idee: IdeeEntity): Boolean =
            idee.angelegtAm !in zeitpunkte && inhaltsSchluessel(idee) !in inhalte

        fun merke(idee: IdeeEntity) {
            zeitpunkte += idee.angelegtAm
            inhalte += inhaltsSchluessel(idee)
        }
    }

    private suspend fun spieleEin(json: JSONObject, ersetzen: Boolean): Ergebnis = datenbank.withTransaction {
        val ideenDao = datenbank.ideenDao()
        val kategorienDao = datenbank.kategorienDao()
        val nachrichtenDao = datenbank.nachrichtenDao()
        if (ersetzen) {
            nachrichtenDao.alleLoeschen()
            ideenDao.alleLoeschen()
        }

        // Die Kategorien zuerst und über Name + Art zugeordnet — die Kennungen der Sicherung
        // passen auf einem anderen Gerät (oder nach dem Umbenennen) nicht zu den eigenen.
        val kategorieZuordnung = mutableMapOf<Long, Long>()
        val kategorien = json.optJSONArray("kategorien") ?: JSONArray()
        for (index in 0 until kategorien.length()) {
            val eintrag = kategorien.optJSONObject(index) ?: continue
            val name = eintrag.optString("name").trim()
            if (name.isBlank()) continue
            val art = runCatching {
                Kategorieart.valueOf(eintrag.optString("art", Kategorieart.MENTAL.name))
            }.getOrDefault(Kategorieart.MENTAL)
            val vorhanden = kategorienDao.nachNameUndArt(name, art)?.id
                ?: kategorienDao.einfuegen(
                    KategorieEntity(name = name, reihenfolge = kategorienDao.anzahl(art), art = art),
                ).takeIf { it > 0 }
                ?: kategorienDao.nachNameUndArt(name, art)?.id
                ?: continue
            kategorieZuordnung[eintrag.optLong("id")] = vorhanden
        }

        val abgleich = Abgleich(ideenDao.alleEinmal())
        val ideenZuordnung = mutableMapOf<Long, Long>()
        var uebersprungen = 0
        // In der Reihenfolge der Sicherung unten an die jeweilige Liste anhängen.
        val gesichert = liesIdeen(json).sortedBy { it.idee.reihenfolge }
        for (eintrag in gesichert) {
            val idee = eintrag.idee
            if (!abgleich.istNeu(idee)) {
                uebersprungen++
                continue
            }
            abgleich.merke(idee)
            val haupt = idee.kategorieId?.let(kategorieZuordnung::get)
            val weitere = idee.weitereKategorieIds().mapNotNull(kategorieZuordnung::get).filter { it != haupt }
            val neueId = ideenDao.einfuegen(
                idee.copy(
                    reihenfolge = ideenDao.naechsteReihenfolgeUnten(idee.status),
                    kategorieId = haupt,
                    weitereKategorien = weitereKategorienText(weitere),
                ),
            )
            ideenZuordnung[eintrag.alteId] = neueId
        }

        // Gespräche nur für die Ideen, die gerade neu dazugekommen sind.
        val nachrichten = json.optJSONArray("nachrichten") ?: JSONArray()
        val liste = mutableListOf<NachrichtEntity>()
        for (index in 0 until nachrichten.length()) {
            val eintrag = nachrichten.optJSONObject(index) ?: continue
            val ideeId = ideenZuordnung[eintrag.optLong("ideeId")] ?: continue
            liste += NachrichtEntity(
                ideeId = ideeId,
                rolle = eintrag.optString("rolle"),
                text = eintrag.optString("text"),
                zeitpunkt = eintrag.optLong("zeitpunkt", System.currentTimeMillis()),
                unvollstaendig = eintrag.optBoolean("unvollstaendig"),
            )
        }
        if (liste.isNotEmpty()) nachrichtenDao.einfuegenAlle(liste)

        IdeenLog.info(
            "Sicherung",
            "importiere",
            "Sicherung eingespielt",
            mapOf("neu" to ideenZuordnung.size, "schonDa" to uebersprungen, "ersetzt" to ersetzen),
        )
        Ergebnis(neu = ideenZuordnung.size, schonDa = uebersprungen)
    }

    /**
     * Räumt Doppel auf, die frühere Wiederherstellungen hinterlassen haben: Ideen mit gleichem
     * Titel und Text werden zu der ältesten zusammengelegt. Gespräche und Kategorien der Doppel
     * wandern zu ihr, es geht nichts verloren. Bearbeitete Kopien bleiben stehen.
     */
    suspend fun entferneDoppelte(): Int = datenbank.withTransaction {
        val ideenDao = datenbank.ideenDao()
        val nachrichtenDao = datenbank.nachrichtenDao()
        var entfernt = 0
        ideenDao.alleEinmal()
            .groupBy { it.status + " " + inhaltsSchluessel(it) }
            .values
            .filter { it.size > 1 }
            .forEach { gruppe ->
                val bleibt = gruppe.minBy { it.id }
                val doppel = gruppe.filter { it.id != bleibt.id }
                val haupt = bleibt.kategorieId ?: doppel.firstNotNullOfOrNull { it.kategorieId }
                val weitere = gruppe.flatMap { it.alleKategorieIds() }.distinct().filter { it != haupt }
                ideenDao.setzeKategorien(bleibt.id, haupt, weitereKategorienText(weitere))
                doppel.forEach { kopie ->
                    nachrichtenDao.verschiebe(kopie.id, bleibt.id)
                    ideenDao.loeschen(kopie)
                    entfernt++
                }
            }
        IdeenLog.info("Sicherung", "entferneDoppelte", "Doppelte Ideen zusammengelegt", mapOf("anzahl" to entfernt))
        entfernt
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

    suspend fun stelleWiederHerAus(quelle: Uri, ersetzen: Boolean): Ergebnis {
        val json = JSONObject(datei.lies(quelle))
        pruefeSchema(json)
        return spieleEin(json, ersetzen)
    }

    /** Was in der Sicherung steckt — für die Rückfrage vor dem Einspielen. */
    suspend fun vorschauVon(quelle: Uri): SicherungsVorschau = withContext(Dispatchers.IO) {
        val json = JSONObject(datei.lies(quelle))
        pruefeSchema(json)
        val bestehende = datenbank.ideenDao().alleEinmal()
        val abgleich = Abgleich(bestehende)
        val gesichert = liesIdeen(json)
        val neu = gesichert.count { eintrag ->
            abgleich.istNeu(eintrag.idee).also { if (it) abgleich.merke(eintrag.idee) }
        }
        SicherungsVorschau(
            ideen = gesichert.size,
            neu = neu,
            nachrichten = json.optJSONArray("nachrichten")?.length() ?: 0,
            bestehende = bestehende.size,
            erstelltAm = json.optString("erstelltAm"),
        )
    }

    fun standText(): String = BackupStatus.describe(context)

    companion object {
        const val SCHEMA_VERSION = 3
        private val ZEIT = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)
    }
}
