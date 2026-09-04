package de.frank.codexkompass.data

import de.frank.codexkompass.data.local.ChatNachrichtEntity
import de.frank.codexkompass.data.local.ChatSitzungEntity
import de.frank.codexkompass.data.local.EintragEntity
import de.frank.codexkompass.data.local.FrageEntity
import de.frank.codexkompass.observability.KompassLog
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
)

/**
 * Sichert die eigenen Inhalte und spielt sie wieder ein (Referenz, Baustein J.1).
 *
 * Gesichert wird nur, was sich nicht wiederherstellen lässt: die selbst gestellten Fragen samt
 * Antworten, die vertieften Erklärungen und die Gespräche. Die mitgelieferte Wissensbasis
 * bleibt draussen — sie steckt ohnehin in der App und würde die Datei nur aufblähen.
 *
 * API-Schlüssel gehören ausdrücklich NICHT in die Sicherung. Eine Sicherungsdatei landet
 * schnell in einer Cloud oder in einem Chat; ein Schlüssel darin wäre offengelegt.
 */
object Sicherung {

    const val SCHEMA_VERSION = 1

    fun baueDateiname(zeitstempel: String): String = "codex-kompass-sicherung-$zeitstempel.json"

    fun schreibe(
        eintraege: List<EintragEntity>,
        fragen: List<FrageEntity>,
        sitzungen: List<ChatSitzungEntity>,
        nachrichten: List<ChatNachrichtEntity>,
        erstelltAm: String,
    ): String {
        // Nur Einträge mit eigenem Zutun. Ein unveränderter Eintrag steht identisch in der
        // App und muss nicht mitgeschleppt werden.
        val eigene = eintraege.filter { it.stufe > 0 }
        val json = JSONObject()
            .put("schema", SCHEMA_VERSION)
            .put("erstelltAm", erstelltAm)
            .put("app", "Codex Kompass")
            .put(
                "eintraege",
                JSONArray().apply {
                    eigene.forEach { eintrag ->
                        put(
                            JSONObject()
                                .put("id", eintrag.id)
                                .put("erklaerung", eintrag.erklaerung)
                                .put("stufe", eintrag.stufe),
                        )
                    }
                },
            )
            .put(
                "fragen",
                JSONArray().apply {
                    fragen.forEach { frage ->
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
                    sitzungen.forEach { sitzung ->
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
                "eintraege" to eigene.size,
                "fragen" to fragen.size,
                "sitzungen" to sitzungen.size,
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
            throw SicherungsFehler("Die Datei ist keine gültige Sicherung von Codex Kompass.", it)
        }
        if (json.optString("app") != "Codex Kompass") {
            throw SicherungsFehler("Diese Sicherung gehört nicht zu Codex Kompass.")
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
        val vorschau = SicherungsVorschau(
            schema = schema,
            erstelltAm = json.optString("erstelltAm"),
            eintraege = json.optJSONArray("eintraege")?.length() ?: 0,
            fragen = json.optJSONArray("fragen")?.length() ?: 0,
            sitzungen = sitzungen.length(),
            nachrichten = nachrichten,
        )
        return vorschau to json
    }
}
