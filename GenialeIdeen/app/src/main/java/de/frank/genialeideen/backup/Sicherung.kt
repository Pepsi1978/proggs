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
import okhttp3.OkHttpClient
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
 * Datei-Export und -Import sowie die Google-Drive-Sicherung (Baustein J).
 *
 * API-Schlüssel gehören ausdrücklich **nicht** in die Sicherung.
 */
class Sicherung(
    private val context: Context,
    private val datenbank: GenialeIdeenDatabase,
) {
    private val driveAuth = DriveAuth(context)
    private val driveClient = DriveClient(OkHttpClient())

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

    suspend fun exportiere(ziel: Uri): Int = withContext(Dispatchers.IO) {
        val inhalt = alsJson()
        context.contentResolver.openOutputStream(ziel)?.use { strom ->
            strom.write(inhalt.toByteArray(Charsets.UTF_8))
        } ?: error("Die Datei konnte nicht zum Schreiben geöffnet werden.")
        val anzahl = JSONObject(inhalt).optJSONArray("ideen")?.length() ?: 0
        IdeenLog.info("Sicherung", "exportiere", "Datei geschrieben", mapOf("ideen" to anzahl))
        anzahl
    }

    suspend fun vorschau(quelle: Uri): SicherungsVorschau = withContext(Dispatchers.IO) {
        val json = lese(quelle)
        pruefeSchema(json)
        SicherungsVorschau(
            ideen = json.optJSONArray("ideen")?.length() ?: 0,
            nachrichten = json.optJSONArray("nachrichten")?.length() ?: 0,
            bestehende = datenbank.ideenDao().alleEinmal().size,
            erstelltAm = json.optString("erstelltAm"),
        )
    }

    /** [ersetzen] = true wirft den bestehenden Bestand weg, sonst wird zusammengeführt. */
    suspend fun importiere(quelle: Uri, ersetzen: Boolean): Int = withContext(Dispatchers.IO) {
        val json = lese(quelle)
        pruefeSchema(json)
        spieleEin(json, ersetzen)
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

    private fun lese(quelle: Uri): JSONObject {
        val text = context.contentResolver.openInputStream(quelle)?.use { strom ->
            strom.readBytes().toString(Charsets.UTF_8)
        } ?: error("Die Datei konnte nicht gelesen werden.")
        return JSONObject(text)
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

    // ---- Google Drive, ausschliesslich im appDataFolder ----

    suspend fun driveVerbunden(): Boolean = driveAuth.isConnected()

    suspend fun nachDriveSichern(): String {
        val token = driveAuth.accessToken()
        val vorhanden = driveClient.findBackup(token)
        driveClient.upload(token, vorhanden?.id, alsJson())
        BackupStatus.markBackedUp(context)
        IdeenLog.info("Sicherung", "nachDriveSichern", "In den appDataFolder geschrieben")
        return BackupStatus.describe(context)
    }

    suspend fun vonDriveHolen(ersetzen: Boolean): Int {
        val token = driveAuth.accessToken()
        val datei = driveClient.findBackup(token)
            ?: error("Im Google-Konto liegt noch keine Sicherung dieser App.")
        val json = JSONObject(driveClient.download(token, datei.id))
        pruefeSchema(json)
        return spieleEin(json, ersetzen)
    }

    suspend fun driveTrennen() = driveAuth.disconnect()

    /**
     * Nimmt Googles Antwort auf den Freigabe-Bildschirm entgegen. Erst danach liegt ein Zugang
     * bereit — der Rueckgabewert der Activity allein sagt darueber nichts aus.
     */
    fun driveFreigabeErgebnis(daten: android.content.Intent?): Result<Boolean> =
        driveAuth.readConsentResult(daten)

    fun standText(): String = BackupStatus.describe(context)

    companion object {
        const val SCHEMA_VERSION = 3
        private val ZEIT = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY)
        private val DATEI_ZEIT = SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.GERMANY)

        fun dateiName(): String = "geniale-ideen-sicherung-${DATEI_ZEIT.format(Date())}.json"
    }
}
