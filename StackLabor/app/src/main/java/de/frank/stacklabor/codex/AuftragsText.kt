package de.frank.stacklabor.codex

// PLAN (F-12/F-13/F-02): Der Auftragstext an Codex und die Nutzlast der Anfrage.
// 1. Schlanke Datenklassen als Eingabe — bewusst ohne Room-Bezug, damit die Datenschicht
//    unabhängig davon gebaut werden kann; die Kennungen (id) müssen mit, weil die Antwort auf sie
//    verweist (Feld nem/ziel/frage).
// 2. Der Systemtext trägt die wörtlichen Auflagen aus F-12 und beschreibt das dünn besetzte Format.
// 3. Die Stammdaten gehen als JSON-Blöcke mit — so bleiben Kennungen und Zahlen unverfälscht.
// 4. Die Nutzlast erzwingt das Antwortformat über ein striktes json_schema; damit kommt in aller
//    Regel gar kein Text um das JSON herum an.

import org.json.JSONArray
import org.json.JSONObject

/** Umfang einer Auswertung: ein einzelner Stack (F-12) oder alle Stacks zusammen (F-13). */
enum class Auswertungsbereich { STACK, TAG }

/**
 * Ein aktives Mittel, so wie es in die Anfrage geht.
 *
 * [gesamtdosis] ist der fertig gerechnete Text (Stückzahl × Menge je Stück samt Einheit),
 * [tagesgesamtdosis] und [stacks] werden nur bei [Auswertungsbereich.TAG] gefüllt (F-13).
 */
data class MittelAngabe(
    val id: String,
    val name: String,
    val gesamtdosis: String,
    val darreichungsform: String,
    val loeslichkeit: String,
    val frequenz: String,
    val alterniertMit: List<String> = emptyList(),
    val hersteller: String = "",
    val durchfallrisiko: Boolean = false,
    val zusatztext: String = "",
    val gruppe: String = "",
    val tagesgesamtdosis: String = "",
    val stacks: List<String> = emptyList(),
)

/** Ein Ziel des Stacks mit seinem Rang (Rang 1 ist das wichtigste). */
data class ZielAngabe(val id: String, val rang: Int, val text: String)

/** Eine eigene Frage des Stacks. */
data class FrageAngabe(val id: String, val text: String)

/** Alles, was für eine Auswertung mitgeschickt wird. */
data class AuswertungsAnfrage(
    val stackName: String = "",
    val zeitpunkt: String = "",
    val einnahmeHinweis: String = "",
    val mittel: List<MittelAngabe> = emptyList(),
    val ziele: List<ZielAngabe> = emptyList(),
    val fragen: List<FrageAngabe> = emptyList(),
    val bereich: Auswertungsbereich = Auswertungsbereich.STACK,
)

/** Baut Systemtext, Eingabetext und Nutzlast der Codex-Anfrage. */
object AuftragsText {

    /** Wörtlich verbindlich laut F-12 — steht in jedem Auftrag. */
    const val PFLICHT_SATZ: String =
        "Nur dieses JSON, kein Text davor oder danach. Nicht genannte Mittel-Ziel-Paare gelten als " +
            "neutral. Als „alterniert mit\" gekennzeichnete Paare nie als Konkurrenz melden."

    /** Zusatz für den einen Wiederholversuch nach kaputtem JSON (F-12). */
    const val NUR_JSON_ZUSATZ: String = "nur JSON, kein Text davor oder danach"

    /**
     * Der Systemtext (Feld `instructions`).
     *
     * @param nurJson setzt den Zusatz für den Wiederholversuch nach kaputtem JSON.
     * @param neueMittel schränkt die Antwort auf die frisch hinzugefügten Mittel ein (F-02).
     */
    fun systemText(
        anfrage: AuswertungsAnfrage,
        nurJson: Boolean = false,
        neueMittel: List<String> = emptyList(),
    ): String = buildString {
        append("Du bist ein nüchterner Fachberater für Nahrungsergänzung. Du bewertest, wie die ")
        append("genannten Mittel auf die genannten Ziele wirken, und welche Mittel sich gegenseitig ")
        append("behindern. Du schreibst auf Deutsch, sachlich, ohne Werbeton und ohne Heilversprechen.\n\n")

        append("Bewertungsregeln:\n")
        append("- Melde ausschliesslich Mittel-Ziel-Paare, die nicht neutral sind. Die Tabelle ist ")
        append("bewusst dünn besetzt.\n")
        append("- „wirkung\" ist stuetzt oder stoert, „staerke\" ist 1 (schwach), 2 (deutlich) oder ")
        append("3 (stark).\n")
        append("- „grund\" ist ein Satz mit höchstens 140 Zeichen.\n")
        append("- Verweise auf Mittel und Ziele ausschliesslich über die mitgelieferten Kennungen ")
        append("(Feld „id\"), niemals über den Namen.\n")
        append("- Konkurrenzen: „art\" ist aufnahme, wirkung oder zeitpunkt; „schwere\" ist 1 bis 3.\n")
        append("- Beziehe Zeitpunkt, Einnahme-Hinweis, Darreichungsform, Löslichkeit, Frequenz, ")
        append("Hersteller, Durchfallrisiko und den Zusatztext in die Bewertung ein.\n")
        if (anfrage.bereich == Auswertungsbereich.TAG) {
            append("- Dies ist die Prüfung über alle Stacks zusammen: achte besonders auf die ")
            append("Tagesgesamtdosis je Wirkstoff und auf Konkurrenzen über Stack-Grenzen hinweg.\n")
        }
        append("- Beantworte jede eigene Frage über ihre Kennung im Feld „antworten\".\n")
        append("- „gesamt\" ist der Fließtext in Markdown; er wird vorgelesen und enthält deshalb ")
        append("keine Tabellen, keine Aufzählungszeichen und keine Zahlenkolonnen.\n")
        append("- „hinweise\" enthält Einnahme- und Dosis-Warnungen, je ein Satz.\n")

        if (neueMittel.isNotEmpty()) {
            append("\nDies ist eine Konkurrenzprüfung für frisch hinzugefügte Mittel. Melde nur ")
            append("Zellen und Konkurrenzen, an denen mindestens eines dieser Mittel beteiligt ist: ")
            append(JSONArray(neueMittel).toString())
            append(". Halte „gesamt\" auf höchstens drei Sätze.\n")
        }

        append("\n")
        append(PFLICHT_SATZ)
        if (nurJson) {
            append("\nZur Klarstellung: $NUR_JSON_ZUSATZ. Keine Einleitung, keine Erklärung, keine ")
            append("Code-Zäune.")
        }
    }

    /** Der Eingabetext (Feld `input`) mit allen Stammdaten. */
    fun eingabeText(anfrage: AuswertungsAnfrage): String = buildString {
        if (anfrage.bereich == Auswertungsbereich.TAG) {
            append("Geprüft werden alle Stacks eines Tages zusammen.\n")
        } else {
            append("Geprüft wird ein einzelner Stack.\n")
        }
        if (anfrage.stackName.isNotBlank()) append("Stack: ${anfrage.stackName.trim()}\n")
        append("Zeitpunkt: ${anfrage.zeitpunkt.trim().ifEmpty { "nicht angegeben" }}\n")
        append("Einnahme-Hinweis: ${anfrage.einnahmeHinweis.trim().ifEmpty { "nicht angegeben" }}\n\n")

        append("Aktive Mittel (JSON):\n")
        append(mittelListe(anfrage.mittel).toString())
        append("\n\nZiele nach Rang, Rang 1 ist das wichtigste (JSON):\n")
        append(zielListe(anfrage.ziele).toString())
        append("\n\nEigene Fragen (JSON):\n")
        append(frageListe(anfrage.fragen).toString())
    }

    /**
     * Die vollständige Nutzlast für `chatgpt.com/backend-api/codex/responses`.
     * Die Antwort wird über ein striktes json_schema erzwungen und gestreamt.
     */
    internal fun nutzlast(
        anfrage: AuswertungsAnfrage,
        modell: CodexModell,
        denkstufe: Denkstufe,
        nurJson: Boolean = false,
        neueMittel: List<String> = emptyList(),
    ): JSONObject = JSONObject()
        .put("model", modell.apiId)
        .put("stream", true)
        .put("store", false)
        .put("instructions", systemText(anfrage, nurJson, neueMittel))
        .put(
            "input",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", eingabeText(anfrage)),
            ),
        )
        .put("reasoning", JSONObject().put("effort", denkstufe.apiWert))
        .put(
            "text",
            JSONObject().put(
                "format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("name", "stacklabor_bewertung")
                    .put("strict", true)
                    .put("schema", antwortSchema()),
            ),
        )

    /** Das erzwungene Antwortformat aus F-12 als JSON-Schema. */
    internal fun antwortSchema(): JSONObject {
        val zelle = objektSchema(
            "nem" to JSONObject().put("type", "string"),
            "ziel" to JSONObject().put("type", "string"),
            "wirkung" to JSONObject().put("type", "string").put("enum", JSONArray().put("stuetzt").put("stoert")),
            "staerke" to JSONObject().put("type", "integer").put("enum", JSONArray().put(1).put(2).put(3)),
            "grund" to JSONObject().put("type", "string"),
        )
        val konkurrenz = objektSchema(
            "nem_a" to JSONObject().put("type", "string"),
            "nem_b" to JSONObject().put("type", "string"),
            "art" to JSONObject().put("type", "string")
                .put("enum", JSONArray().put("aufnahme").put("wirkung").put("zeitpunkt")),
            "schwere" to JSONObject().put("type", "integer").put("enum", JSONArray().put(1).put(2).put(3)),
            "grund" to JSONObject().put("type", "string"),
        )
        val antwort = objektSchema(
            "frage" to JSONObject().put("type", "string"),
            "text" to JSONObject().put("type", "string"),
        )
        return objektSchema(
            "zellen" to JSONObject().put("type", "array").put("items", zelle),
            "konkurrenzen" to JSONObject().put("type", "array").put("items", konkurrenz),
            "antworten" to JSONObject().put("type", "array").put("items", antwort),
            "gesamt" to JSONObject().put("type", "string"),
            "hinweise" to JSONObject().put("type", "array").put("items", JSONObject().put("type", "string")),
        )
    }

    private fun objektSchema(vararg felder: Pair<String, JSONObject>): JSONObject {
        val eigenschaften = JSONObject()
        val pflicht = JSONArray()
        felder.forEach { (name, schema) ->
            eigenschaften.put(name, schema)
            pflicht.put(name)
        }
        return JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("required", pflicht)
            .put("properties", eigenschaften)
    }

    private fun mittelListe(mittel: List<MittelAngabe>): JSONArray {
        val liste = JSONArray()
        mittel.forEach { eintrag ->
            val objekt = JSONObject()
                .put("id", eintrag.id)
                .put("name", eintrag.name)
                .put("gesamtdosis", eintrag.gesamtdosis)
                .put("darreichungsform", eintrag.darreichungsform)
                .put("loeslichkeit", eintrag.loeslichkeit)
                .put("frequenz", eintrag.frequenz)
                .put("durchfallrisiko", eintrag.durchfallrisiko)
            if (eintrag.alterniertMit.isNotEmpty()) {
                objekt.put("alterniert_mit", JSONArray(eintrag.alterniertMit))
            }
            if (eintrag.hersteller.isNotBlank()) objekt.put("hersteller", eintrag.hersteller.trim())
            if (eintrag.zusatztext.isNotBlank()) objekt.put("zusatztext", eintrag.zusatztext.trim())
            if (eintrag.gruppe.isNotBlank()) objekt.put("kombi_gruppe", eintrag.gruppe.trim())
            if (eintrag.tagesgesamtdosis.isNotBlank()) objekt.put("tagesgesamtdosis", eintrag.tagesgesamtdosis.trim())
            if (eintrag.stacks.isNotEmpty()) objekt.put("stacks", JSONArray(eintrag.stacks))
            liste.put(objekt)
        }
        return liste
    }

    private fun zielListe(ziele: List<ZielAngabe>): JSONArray {
        val liste = JSONArray()
        ziele.sortedBy { it.rang }.forEach { ziel ->
            liste.put(
                JSONObject()
                    .put("id", ziel.id)
                    .put("rang", ziel.rang)
                    .put("text", ziel.text.trim()),
            )
        }
        return liste
    }

    private fun frageListe(fragen: List<FrageAngabe>): JSONArray {
        val liste = JSONArray()
        fragen.forEach { frage ->
            liste.put(JSONObject().put("id", frage.id).put("text", frage.text.trim()))
        }
        return liste
    }
}
