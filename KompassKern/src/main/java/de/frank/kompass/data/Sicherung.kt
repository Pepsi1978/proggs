package de.frank.kompass.data

import de.frank.kompass.AppProfil

import de.frank.kompass.data.local.ChatNachrichtEntity
import de.frank.kompass.data.local.ChatSitzungEntity
import de.frank.kompass.data.local.EintragEntity
import de.frank.kompass.data.local.FrageEntity
import de.frank.kompass.data.model.SicherungsTeil
import de.frank.kompass.observability.KompassLog
import org.json.JSONArray
import org.json.JSONObject

class SicherungsFehler(meldung: String, ursache: Throwable? = null) : Exception(meldung, ursache)

/** Was in einer Sicherungsdatei steht — und was der Benutzer vor dem Einspielen sieht. */
data class SicherungsVorschau(
    val schema: Int,
    val erstelltAm: String,
    val eintraege: Int,
    val fragen: Int,
    val sitzungen: Int,
    val nachrichten: Int,
    /** Wie viele Einträge je Wissensbereich in der Datei stehen. */
    val jeBereich: Map<SicherungsTeil, Int> = emptyMap(),
    /** Was beim Sichern angehakt war. Leer bei Dateien aus der Zeit vor der Auswahl. */
    val umfang: Set<SicherungsTeil> = emptySet(),
) {
    /** Ein Satz, der sagt, was in der Datei steckt — vor dem Einspielen. */
    fun alsText(): String {
        val teile = buildList {
            SicherungsTeil.entries.forEach { teil ->
                jeBereich[teil]?.takeIf { it > 0 }?.let { add("$it ${teil.titel}") }
            }
            if (fragen > 0) add("$fragen Fragen")
            if (sitzungen > 0) add("$sitzungen Gespräche")
        }
        val kern = if (teile.isEmpty()) "Die Datei enthält keine Einträge." else teile.joinToString(", ")
        return "Sicherung vom $erstelltAm: $kern. " +
            "Ergänzt wird nur, was hier fehlt — Vorhandenes bleibt unverändert."
    }
}

/**
 * Sichert die Inhalte und spielt sie wieder ein (Referenz, Baustein J.1).
 *
 * Was hineinkommt, bestimmt der Umfang: die drei Wissensbereiche jeweils VOLLSTÄNDIG, dazu die
 * selbst gestellten Fragen und die Gespräche. Die Wissensbereiche vollständig zu sichern ist der
 * Unterschied zu früher: Wer den Aktualisieren-Knopf benutzt hat, trägt einen neueren Stand in
 * der Datenbank als in der mitgelieferten Wissensbasis — ohne ihn stand auf einem zweiten Gerät
 * wieder nur der Auslieferungsstand.
 *
 * API-Schlüssel, die Anmeldung und die Einstellungen der App gehören ausdrücklich NICHT hinein.
 */
object Sicherung {

    /**
     * 1: nur der selbst erarbeitete Anteil. 2: wählbarer Umfang, Wissensbereiche vollständig.
     *
     * Dateien nach Schema 1 bleiben lesbar. Sie tragen kein Feld „umfang"; für sie gilt, was
     * damals drinstand — eigene Beiträge, Fragen und Gespräche.
     */
    const val SCHEMA_VERSION = 2

    fun baueDateiname(zeitstempel: String): String = "${AppProfil.DATEI_PRAEFIX}-sicherung-$zeitstempel.json"

    fun schreibe(
        eintraege: List<EintragEntity>,
        fragen: List<FrageEntity>,
        sitzungen: List<ChatSitzungEntity>,
        nachrichten: List<ChatNachrichtEntity>,
        erstelltAm: String,
        umfang: Set<SicherungsTeil> = SicherungsTeil.ALLE,
    ): String {
        val bereiche = umfang.mapNotNull { it.bereich?.id }.toSet()
        val gewaehlte = eintraege.filter { it.bereich in bereiche }
        val eigeneFragen = if (SicherungsTeil.FRAGEN in umfang) fragen else emptyList()
        val eigeneSitzungen = if (SicherungsTeil.GESPRAECHE in umfang) sitzungen else emptyList()

        val json = JSONObject()
            .put("schema", SCHEMA_VERSION)
            .put("erstelltAm", erstelltAm)
            .put("app", AppProfil.PRODUKT)
            .put("umfang", JSONArray().apply { umfang.forEach { put(it.id) } })
            .put(
                "eintraege",
                JSONArray().apply {
                    // Vollständig, mit allen Angaben: Auf einem neuen Gerät muss der Eintrag
                    // allein aus der Datei entstehen können, ohne die Wissensbasis der App.
                    gewaehlte.forEach { eintrag ->
                        put(
                            JSONObject()
                                .put("id", eintrag.id)
                                .put("erklaerung", eintrag.erklaerung)
                                .put("stufe", eintrag.stufe)
                                .put("bereich", eintrag.bereich)
                                .put("name", eintrag.name)
                                .put("kurz", eintrag.kurz)
                                .put("kategorie", eintrag.kategorie)
                                .put("art", eintrag.art)
                                .put("quelleEnglisch", eintrag.quelleEnglisch)
                                .put("seitVersion", eintrag.seitVersion)
                                .put("sortierName", eintrag.sortierName)
                                .put("entfernt", eintrag.entfernt)
                                .put("entferntInVersion", eintrag.entferntInVersion)
                                .put("ersatz", eintrag.ersatz),
                        )
                    }
                },
            )
            .put(
                "fragen",
                JSONArray().apply {
                    eigeneFragen.forEach { frage ->
                        put(
                            JSONObject()
                                .put("eintragId", frage.eintragId)
                                .put("frage", frage.frage)
                                .put("antwort", frage.antwort)
                                .put("erstelltAm", frage.erstelltAm),
                        )
                    }
                },
            )
            .put(
                "sitzungen",
                JSONArray().apply {
                    eigeneSitzungen.forEach { sitzung ->
                        val eigeneNachrichten = nachrichten.filter { it.sitzungId == sitzung.id }
                        put(
                            JSONObject()
                                .put("titel", sitzung.titel)
                                .put("erstelltAm", sitzung.erstelltAm)
                                .put(
                                    "nachrichten",
                                    JSONArray().apply {
                                        eigeneNachrichten.forEach { nachricht ->
                                            put(
                                                JSONObject()
                                                    .put("rolle", nachricht.rolle)
                                                    .put("text", nachricht.text)
                                                    .put("erstelltAm", nachricht.erstelltAm),
                                            )
                                        }
                                    },
                                ),
                        )
                    }
                },
            )
        KompassLog.info(
            "Sicherung",
            "schreibe",
            "Sicherung erstellt",
            mapOf(
                "umfang" to umfang.joinToString(",") { it.id },
                "eintraege" to gewaehlte.size,
                "fragen" to eigeneFragen.size,
                "sitzungen" to eigeneSitzungen.size,
            ),
        )
        return json.toString(1)
    }

    /**
     * Liest eine Sicherungsdatei und beschreibt, was darin steht — VOR dem Einspielen.
     *
     * Eine unbekannte, höhere Schema-Version wird abgelehnt statt halb eingelesen. Halb
     * eingelesene Daten sind schlimmer als gar keine, weil man ihnen nicht ansieht, was fehlt.
     */
    fun lies(inhalt: String): Pair<SicherungsVorschau, JSONObject> {
        val json = runCatching { JSONObject(inhalt) }.getOrElse {
            throw SicherungsFehler("Die Datei ist keine gültige Sicherung von ${AppProfil.PRODUKT}.", it)
        }
        if (json.optString("app") != AppProfil.PRODUKT) {
            throw SicherungsFehler("Diese Sicherung gehört nicht zu ${AppProfil.PRODUKT}.")
        }
        val schema = json.optInt("schema", -1)
        if (schema < 0) {
            throw SicherungsFehler("In der Datei fehlt die Angabe, nach welchem Schema sie erstellt wurde.")
        }
        if (schema > SCHEMA_VERSION) {
            throw SicherungsFehler(
                "Diese Sicherung stammt aus einer neueren Fassung der App (Schema $schema, " +
                    "diese App kennt $SCHEMA_VERSION). Aktualisier zuerst die App — " +
                    "eine halb eingelesene Sicherung wäre schlimmer als keine.",
            )
        }
        val sitzungen = json.optJSONArray("sitzungen") ?: JSONArray()
        var nachrichten = 0
        for (index in 0 until sitzungen.length()) {
            nachrichten += sitzungen.optJSONObject(index)?.optJSONArray("nachrichten")?.length() ?: 0
        }

        val eintraege = json.optJSONArray("eintraege") ?: JSONArray()
        val jeBereich = mutableMapOf<SicherungsTeil, Int>()
        for (index in 0 until eintraege.length()) {
            val bereich = eintraege.optJSONObject(index)?.optString("bereich") ?: continue
            val teil = SicherungsTeil.entries.firstOrNull { it.bereich?.id == bereich } ?: continue
            jeBereich[teil] = (jeBereich[teil] ?: 0) + 1
        }

        // Schema 1 kennt kein Feld „umfang". Dort stand immer dasselbe drin.
        val umfang = json.optJSONArray("umfang")?.let { feld ->
            (0 until feld.length()).mapNotNull { SicherungsTeil.fromId(feld.optString(it)) }.toSet()
        } ?: setOf(SicherungsTeil.FRAGEN, SicherungsTeil.GESPRAECHE)

        val vorschau = SicherungsVorschau(
            schema = schema,
            erstelltAm = json.optString("erstelltAm"),
            eintraege = eintraege.length(),
            fragen = json.optJSONArray("fragen")?.length() ?: 0,
            sitzungen = sitzungen.length(),
            nachrichten = nachrichten,
            jeBereich = jeBereich,
            umfang = umfang,
        )
        return vorschau to json
    }
}
