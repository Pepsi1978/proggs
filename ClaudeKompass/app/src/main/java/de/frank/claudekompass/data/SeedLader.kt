package de.frank.claudekompass.data

import android.content.Context
import de.frank.claudekompass.data.local.EintragEntity
import de.frank.claudekompass.data.model.Bereich
import de.frank.claudekompass.observability.KompassLog
import org.json.JSONArray
import org.json.JSONObject

/** Ein Eintrag, wie er aus einer Beigabe oder aus einer Aktualisierung kommt. */
data class RohEintrag(
    val bereich: Bereich,
    val name: String,
    val kategorie: String,
    val art: String,
    val kurz: String,
    val englisch: String,
    val erklaerung: String,
    val seit: String,
    val seitBeleg: String,
    val sortierName: String,
    val entfernt: Boolean,
    val entferntIn: String,
    val ersatz: String,
) {
    /** Die Kennung setzt sich aus Bereich und Name zusammen und bleibt über Läufe stabil. */
    val id: String get() = "${bereich.id}:$name"

    fun zuEntity(neuImLauf: Long = 0L): EintragEntity = EintragEntity(
        id = id,
        bereich = bereich.id,
        name = name,
        kurz = kurz,
        erklaerung = erklaerung,
        stufe = 0,
        seitVersion = seit,
        kategorie = kategorie,
        art = art,
        entfernt = entfernt,
        entferntInVersion = entferntIn,
        ersatz = ersatz,
        neuImLauf = neuImLauf,
        quelleEnglisch = englisch,
        sortierName = sortierName,
    )
}

/**
 * Liest die mitgelieferte Wissensbasis aus den Beigaben.
 *
 * Die drei JSON-Dateien werden beim Bauen erzeugt und tragen bereits die Versionsangaben, die
 * aus dem offiziellen Änderungsprotokoll stammen. Deshalb ist die App auch ohne Netz und ohne
 * Anmeldung sofort vollständig benutzbar — der Aktualisieren-Knopf hebt sie später an.
 */
object SeedLader {

    private const val DATEI_SLASH = "slash_befehle.json"
    private const val DATEI_CONFIG = "config_einstellungen.json"
    private const val DATEI_PRAXIS = "best_practices.json"

    fun ladeAlles(context: Context): List<RohEintrag> =
        lade(context, DATEI_SLASH, Bereich.SLASH) +
            lade(context, DATEI_CONFIG, Bereich.CONFIG) +
            lade(context, DATEI_PRAXIS, Bereich.PRAXIS)

    /** Die Claude-Code-Version, für die die Beigaben erzeugt wurden. */
    fun standVersion(context: Context): String = runCatching {
        JSONObject(leseDatei(context, DATEI_SLASH)).optString("standVersion")
    }.getOrElse { "" }

    private fun lade(context: Context, datei: String, bereich: Bereich): List<RohEintrag> = try {
        val json = JSONObject(leseDatei(context, datei))
        val feld: JSONArray = json.optJSONArray("eintraege") ?: JSONArray()
        val ergebnis = (0 until feld.length()).mapNotNull { index ->
            feld.optJSONObject(index)?.let { zuRoh(it, bereich) }
        }
        KompassLog.info(
            "SeedLader",
            "lade",
            "Wissensbasis eingelesen",
            mapOf("datei" to datei, "eintraege" to ergebnis.size),
        )
        ergebnis
    } catch (fehler: Exception) {
        // Ohne die Beigaben wäre die App leer. Das muss auffallen, nicht still passieren.
        KompassLog.error(
            "SeedLader",
            "lade",
            "Wissensbasis konnte nicht gelesen werden",
            mapOf("datei" to datei, "grund" to fehler.message),
        )
        emptyList()
    }

    private fun leseDatei(context: Context, datei: String): String =
        context.assets.open(datei).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun zuRoh(json: JSONObject, bereich: Bereich): RohEintrag? {
        val name = json.optString("name").takeIf(String::isNotBlank) ?: return null
        return RohEintrag(
            bereich = bereich,
            name = name,
            kategorie = json.optString("kategorie"),
            art = json.optString("art"),
            kurz = json.optString("kurz"),
            englisch = json.optString("englisch"),
            erklaerung = json.optString("erklaerung"),
            seit = json.optString("seit"),
            seitBeleg = json.optString("seitBeleg"),
            sortierName = json.optString("sortierName").takeIf(String::isNotBlank)
                ?: name.removePrefix("/").lowercase(),
            entfernt = json.optBoolean("entfernt", false),
            entferntIn = json.optString("entferntIn"),
            ersatz = json.optString("ersatz"),
        )
    }
}
