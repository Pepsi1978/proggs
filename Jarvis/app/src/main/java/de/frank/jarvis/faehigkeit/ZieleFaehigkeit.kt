package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anbindung an die App „Ziele“ (de.frank.ziele) über deren Jarvis-Brücke (Signatur-Erlaubnis):
 * Ziele lesen, anlegen, ändern und löschen, Auswertungen anstoßen, Vorschläge in Geniale Aufgaben übernehmen,
 * die Kontextdatei pflegen und die Einstellungen der App steuern.
 */
class ZieleFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "ziele"
    override val name = "Ziele"
    override val beschreibung = "Franks Ziele lesen und pflegen, auswerten lassen (Recherche + Aufgabenvorschläge), Vorschläge in Geniale Aufgaben übernehmen, Kontextdatei, Einstellungen."
    override val hinweise =
        "Ziele ist Franks App für seine persönlichen Ziele. Ein Ziel hat Titel, Beschreibung (möglichst messbar), Warum, optional eine Frist und den Status aktiv, pausiert oder erreicht. " +
            "Die Auswertung (ziel_auswerten) recherchiert im Hintergrund, wie das Ziel am besten erreicht wird, und schlägt Aufgaben vor, getrennt in notwendig und unterstützend, " +
            "mit Wichtigkeit (1 bis 5), Aufwand, Phase und Voraussetzungen. Mit ziel_vorschlag übernimmst du einen Vorschlag als Aufgabe in Geniale Aufgaben. " +
            "Will Frank ein Ziel ausarbeiten oder planen, gehört es hierher (ziel_speichern); jarvis_merken mit art ziel ist nur die kurze Notiz für dein Gedächtnis. " +
            "Nennt Frank ein neues ernsthaftes Ziel, biete an, es in der Ziele-App anzulegen und auswerten zu lassen. Vor dem Löschen kurz bestätigen lassen."

    override fun stoerung(): String? = AppBruecke.rufe(context, name, BRUECKE, "info", JSONObject(), lesend = true).optString("fehler").takeIf { it.isNotEmpty() }

    /** Alle Ziele fürs Live-Gedächtnis: aktive mit Strategie und Fortschritt, pausierte und erreichte kurz. */
    suspend fun gedaechtnis(): String {
        val antwort = rufe("lesen", JSONObject().put("status", "alle"))
        antwort.optString("fehler").takeIf { it.isNotEmpty() }?.let { return "NICHT VERFÜGBAR: $it" }
        val ziele = antwort.optJSONArray("ziele") ?: JSONArray()
        if (ziele.length() == 0) return "Noch keine Ziele in der App."
        val liste = (0 until ziele.length()).map { ziele.getJSONObject(it) }
        return buildString {
            listOf("aktiv" to "Aktiv", "pausiert" to "Pausiert", "erreicht" to "Erreicht").forEach { (status, name) ->
                val teil = liste.filter { it.optString("status") == status }
                if (teil.isEmpty()) return@forEach
                append(name).append(":\n")
                teil.forEach { z ->
                    append("- ").append(kopf(z))
                    if (status == "aktiv") {
                        z.optString("beschreibung").takeIf { it.isNotBlank() }?.let { append(" — ").append(it.replace(Regex("\\s+"), " ").take(220)) }
                        z.optString("strategie").takeIf { it.isNotBlank() }?.let { append(" Weg: ").append(it.replace(Regex("\\s+"), " ").take(320)) }
                    }
                    append('\n')
                }
            }
        }.trim().let { if (it.length > 7_000) it.take(7_000).substringBeforeLast('\n') + "\n… (weitere mit ziele_lesen)" else it }
    }

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = "ziele_lesen",
                titel = "Ziele lesen",
                beschreibung = "Jarvis: liest Franks Ziele aus der App Ziele. Ohne id die Liste (Vorgabe aktive Ziele) mit Frist, Strategie und Fortschritt der notwendigen Aufgaben. " +
                    "Mit id ein Ziel vollständig: Recherche-Bericht, alle Aufgabenvorschläge (notwendig und unterstützend, in der gewählten Sortierung) mit ids, " +
                    "ob sie schon in Geniale Aufgaben stehen oder dort erledigt sind. Nutze es für „Welche Ziele habe ich?“, „Wie stehe ich bei meinem Ziel …?“, „Was sollte ich als Nächstes für … tun?“.",
                schema = schema(
                    "id" to zahl("id eines Ziels: dann dieses vollständig mit allen Vorschlägen."),
                    "status" to text("Ohne id: aktiv (Vorgabe), pausiert, erreicht oder alle.", listOf("aktiv", "pausiert", "erreicht", "alle")),
                    "suche" to text("Ohne id: Wort aus Titel oder Beschreibung."),
                    "sortierung" to text("Mit id: Reihenfolge der Vorschläge (Vorgabe: wie in der App).", listOf("wichtigkeit", "reihenfolge", "aufwand", "hebel", "phase")),
                ),
                nurLesen = true,
            ) { a -> if (a.idGesetzt()) mitZiel(a) { id -> rufe("lesen", JSONObject(a.toString()).put("id", id).apply { remove("suche") }).fehlerOder(::zielVoll) } else rufe("lesen", a).fehlerOder(::liste) },
            Werkzeug(
                name = "ziel_speichern",
                titel = "Ziel anlegen oder ändern",
                beschreibung = "Jarvis: legt in der App Ziele ein neues Ziel an oder ändert eines. NEU (ohne id und suche): titel Pflicht, dazu was Frank gesagt hat in beschreibung und warum; " +
                    "auswerten=true startet gleich die Recherche mit Aufgabenvorschlägen. ÄNDERN (mit id oder suche): neue Angaben oder status (aktiv, pausiert, erreicht).",
                schema = schema(
                    "id" to zahl("Nur beim Ändern: id aus ziele_lesen."),
                    "suche" to text("Nur beim Ändern, statt id: Wort aus dem Titel."),
                    "titel" to text("Kurz und klar als Ziel formuliert, z. B. „Halbmarathon unter 2 Stunden“."),
                    "beschreibung" to text("Was genau erreicht werden soll, möglichst messbar, in Franks Worten."),
                    "warum" to text("Warum es Frank wichtig ist, nur wenn er es gesagt hat."),
                    "frist" to text("Zieldatum JJJJ-MM-TT oder ohne."),
                    "symbol" to text("Ein passendes Emoji."),
                    "status" to text("Nur beim Ändern.", listOf("aktiv", "pausiert", "erreicht")),
                    "auswerten" to schalter("Nur bei einem neuen Ziel: gleich auswerten (Vorgabe: aus)."),
                ),
                nurLesen = false,
            ) { a ->
                if (a.idGesetzt() || a.gesetzt("suche")) mitZiel(a) { id -> rufe("aendern", JSONObject(a.toString()).put("id", id).apply { remove("suche"); remove("auswerten") }).fehlerOder { "Geändert: " + kopf(it.getJSONObject("ziel")) } }
                else rufe("anlegen", a).fehlerOder { "Ziel angelegt: " + kopf(it.getJSONObject("ziel")) + if (it.optBoolean("auswertung_gestartet")) ". Die Auswertung läuft im Hintergrund." else "" }
            },
            Werkzeug(
                name = "ziel_loeschen",
                titel = "Ziel löschen",
                beschreibung = "Jarvis: löscht ein Ziel mit allen Vorschlägen aus der App Ziele (Aufgaben in Geniale Aufgaben bleiben). Nur, wenn Frank das Löschen eindeutig verlangt hat.",
                schema = schema("id" to zahl("id aus ziele_lesen."), "suche" to text("Statt id: Wort aus dem Titel.")),
                nurLesen = false,
                loeschend = true,
            ) { a -> mitZiel(a) { id -> rufe("loeschen", JSONObject().put("id", id)).fehlerOder { "Gelöscht: „${it.getJSONObject("geloescht").optString("titel")}“." } } },
            Werkzeug(
                name = "ziel_auswerten",
                titel = "Ziel auswerten",
                beschreibung = "Jarvis: startet in der App Ziele die Auswertung eines Ziels: Recherche, wie es am besten erreicht wird, danach notwendige und unterstützende Aufgabenvorschläge, " +
                    "zugeschnitten auf Frank (Kontextdatei und dein Live-Gedächtnis). Läuft im Hintergrund, meist zwei bis acht Minuten; übernommene und ausgeblendete Vorschläge bleiben. " +
                    "nur_stand=true fragt nur ab, ob sie noch läuft. Danach mit ziele_lesen und id die Vorschläge holen.",
                schema = schema(
                    "id" to zahl("id aus ziele_lesen."),
                    "suche" to text("Statt id: Wort aus dem Titel."),
                    "wunsch" to text("Optional: besonderer Schwerpunkt für diese Auswertung, z. B. „nur Aufgaben für zu Hause“."),
                    "nur_stand" to schalter("Nur den Stand der laufenden Auswertung abfragen."),
                ),
                nurLesen = false,
            ) { a ->
                mitZiel(a) { id ->
                    if (a.optBoolean("nur_stand")) rufe("auswertung_stand", JSONObject().put("id", id)).fehlerOder { s ->
                        when {
                            s.optBoolean("laeuft") -> "Die Auswertung läuft noch: ${s.optString("schritt")}"
                            !s.isNull("fehler") -> "Die letzte Auswertung ist gescheitert: ${s.optString("fehler")}"
                            s.isNull("ausgewertet_am") -> "Dieses Ziel wurde noch nicht ausgewertet."
                            else -> "Die Auswertung ist fertig. Die Vorschläge liefert ziele_lesen mit id $id."
                        }
                    } else rufe("auswerten", JSONObject().put("id", id).put("wunsch", a.optString("wunsch"))).fehlerOder { it.optString("hinweis") }
                }
            },
            Werkzeug(
                name = "ziel_vorschlag",
                titel = "Vorschlag übernehmen oder ausblenden",
                beschreibung = "Jarvis: übernimmt einen Aufgabenvorschlag aus der App Ziele als Aufgabe in Geniale Aufgaben (aktion uebernehmen, wie das Plus in der App) " +
                    "oder blendet ihn aus (ausblenden) bzw. wieder ein (einblenden). vorschlag_id aus ziele_lesen mit id.",
                schema = schema(
                    "vorschlag_id" to zahl("id des Vorschlags aus ziele_lesen mit id."),
                    "aktion" to text("uebernehmen (Vorgabe), ausblenden oder einblenden.", listOf("uebernehmen", "ausblenden", "einblenden")),
                    pflicht = listOf("vorschlag_id"),
                ),
                nurLesen = false,
            ) { a ->
                val vid = a.optLong("vorschlag_id", -1)
                when (a.optString("aktion", "uebernehmen")) {
                    "ausblenden", "einblenden" -> rufe("verwerfen", JSONObject().put("vorschlag_id", vid).put("verworfen", a.optString("aktion") == "ausblenden"))
                        .fehlerOder { "${if (a.optString("aktion") == "ausblenden") "Ausgeblendet" else "Wieder eingeblendet"}: „${it.getJSONObject("vorschlag").optString("titel")}“." }
                    else -> rufe("uebernehmen", JSONObject().put("vorschlag_id", vid))
                        .fehlerOder { "In Geniale Aufgaben übernommen: „${it.getJSONObject("vorschlag").optString("titel")}“ (Aufgabe id ${it.optLong("aufgabe_id")})." }
                }
            },
            Werkzeug(
                name = "ziele_kontextdatei",
                titel = "Kontextdatei der Ziele-App",
                beschreibung = "Jarvis: liest oder pflegt die Kontextdatei der App Ziele, die jede Auswertung mitliest (was die KI über Frank wissen soll, zusätzlich zu deinem Gedächtnis). " +
                    "aktion lesen, ergaenzen (hängt einen datierten Absatz an) oder ersetzen (ganzer neuer Text, nur auf Franks ausdrücklichen Wunsch).",
                schema = schema(
                    "aktion" to text("lesen (Vorgabe), ergaenzen oder ersetzen.", listOf("lesen", "ergaenzen", "ersetzen")),
                    "text" to text("Bei ergaenzen der neue Absatz in Franks Worten, bei ersetzen der ganze neue Text."),
                ),
                nurLesen = false,
            ) { a ->
                when (a.optString("aktion", "lesen")) {
                    "ergaenzen" -> rufe("kontext_ergaenzen", a).fehlerOder { "In die Kontextdatei übernommen. Sie hat jetzt ${it.optString("kontext").length} Zeichen." }
                    "ersetzen" -> rufe("kontext_ersetzen", a).fehlerOder { "Kontextdatei ersetzt (${it.optString("kontext").length} Zeichen)." }
                    else -> rufe("kontext_lesen", JSONObject()).fehlerOder { it.optString("kontext").ifBlank { "Die Kontextdatei ist leer." } }
                }
            },
            Werkzeug(
                name = "ziele_einstellungen",
                titel = "Einstellungen der Ziele-App",
                beschreibung = "Jarvis: liest oder ändert die Einstellungen der App Ziele: KI-Modell (OpenAI GPT oder Claude), Denkstufe, Websuche bei der Auswertung, " +
                    "dein Live-Gedächtnis als Kontext, Sortierung der Vorschläge, Menge der Vorschläge und Hell/Dunkel/Automatik. Ohne Angaben kommt der Stand mit allen möglichen Werten.",
                schema = schema(
                    "modell" to text("z. B. GPT-6.1 Sol oder Claude Opus 5.5."),
                    "denkstufe" to text("Niedrig, Mittel, Hoch, Sehr hoch, Maximal (Ultra nur bei manchen GPT-Modellen)."),
                    "recherche" to schalter("Vor den Vorschlägen im Internet recherchieren."),
                    "jarvis_kontext" to schalter("Dein Live-Gedächtnis als Kontext mitschicken."),
                    "sortierung" to text("Sortierung der Vorschläge.", listOf("wichtigkeit", "reihenfolge", "aufwand", "hebel", "phase")),
                    "menge" to text("Wie viele Vorschläge eine Auswertung liefert.", listOf("knapp", "normal", "ausfuehrlich")),
                    "modus" to text("Darstellung.", listOf("hell", "dunkel", "automatisch")),
                ),
                nurLesen = false,
            ) { a ->
                rufe("einstellungen", a).fehlerOder { e ->
                    "Ziele-App: Modell ${e.optString("modell")}, Denkstufe ${e.optString("denkstufe")}, Recherche ${an(e.optBoolean("recherche"))}, Jarvis-Kontext ${an(e.optBoolean("jarvis_kontext"))}, " +
                        "Sortierung ${e.optString("sortierung")}, Menge ${e.optString("menge")}, Darstellung ${e.optString("modus")}.\n" +
                        "Mögliche Modelle: ${e.optJSONArray("modelle")?.join(", ")?.replace("\"", "")}. Denkstufen: ${e.optJSONArray("denkstufen")?.join(", ")?.replace("\"", "")}."
                }
            },
        )
    }

    private fun an(wert: Boolean) = if (wert) "an" else "aus"

    private suspend fun mitZiel(a: JSONObject, dann: suspend (Long) -> Ergebnis): Ergebnis {
        if (a.idGesetzt()) return dann(a.optLong("id"))
        val suche = a.optString("suche").trim()
        if (suche.isEmpty()) return Ergebnis("Es fehlt die id oder ein Suchwort für das Ziel. Hole die id mit ziele_lesen.", fehler = true)
        val treffer = rufe("lesen", JSONObject().put("status", "alle").put("suche", suche))
        treffer.optString("fehler").takeIf { it.isNotEmpty() }?.let { return Ergebnis(it, fehler = true) }
        val ziele = treffer.optJSONArray("ziele") ?: JSONArray()
        return when (ziele.length()) {
            0 -> Ergebnis("Kein Ziel gefunden, das zu „$suche“ passt.", fehler = true)
            1 -> dann(ziele.getJSONObject(0).getLong("id"))
            else -> Ergebnis("Mehrere Ziele passen zu „$suche“. Frage Frank, welches gemeint ist, und rufe das Werkzeug dann mit der id auf:\n" + liste(treffer), fehler = true)
        }
    }

    private fun kopf(z: JSONObject): String = buildString {
        append("[id ").append(z.optLong("id")).append("] ").append(z.optString("symbol")).append(' ').append(z.optString("titel"))
        append(" (").append(z.optString("status"))
        if (!z.isNull("frist") && z.optString("frist").isNotBlank()) {
            append(", Frist ").append(z.optString("frist"))
            if (!z.isNull("tage_bis_frist")) append(", noch ").append(z.optLong("tage_bis_frist")).append(" Tage")
        }
        if (z.optInt("vorschlaege") > 0) append(", notwendig erledigt ").append(z.optString("notwendig_erledigt")).append(", ").append(z.optInt("uebernommen")).append(" in Geniale Aufgaben")
        else if (z.isNull("ausgewertet_am")) append(", noch nicht ausgewertet")
        if (!z.isNull("auswertung_laeuft")) append(", Auswertung läuft: ").append(z.optString("auswertung_laeuft"))
        append(")")
    }

    private fun liste(antwort: JSONObject): String {
        val ziele = antwort.optJSONArray("ziele") ?: JSONArray()
        if (ziele.length() == 0) return "Keine Ziele gefunden."
        return buildString {
            append(antwort.optInt("anzahl")).append(" Ziel(e):\n")
            for (n in 0 until ziele.length()) {
                val z = ziele.getJSONObject(n)
                append("- ").append(kopf(z))
                z.optString("strategie").takeIf { it.isNotBlank() }?.let { append(" Weg: ").append(it.replace('\n', ' ')) }
                append('\n')
            }
        }.trim()
    }

    private fun zielVoll(antwort: JSONObject): String {
        val z = antwort.getJSONObject("ziel")
        return buildString {
            append(kopf(z)).append('\n')
            z.optString("beschreibung").takeIf { it.isNotBlank() }?.let { append("Genauer: ").append(it).append('\n') }
            z.optString("warum").takeIf { it.isNotBlank() }?.let { append("Warum: ").append(it).append('\n') }
            z.optString("strategie").takeIf { it.isNotBlank() }?.let { append("Weg: ").append(it).append('\n') }
            listOf("notwendig" to "NOTWENDIGE AUFGABEN", "unterstuetzend" to "UNTERSTÜTZENDE AUFGABEN").forEach { (feld, titel) ->
                val v = z.optJSONArray(feld) ?: JSONArray()
                if (v.length() == 0) return@forEach
                append('\n').append(titel).append(" (Sortierung ").append(z.optString("sortierung")).append("):\n")
                for (i in 0 until v.length()) {
                    val o = v.getJSONObject(i)
                    append("- [vorschlag_id ").append(o.optLong("id")).append("] ").append(o.optString("titel"))
                    append(" (Wichtigkeit ").append(o.optInt("wichtigkeit")).append("/5, ").append(o.optString("aufwand")).append(", ").append(o.optInt("minuten")).append(" Min.")
                    append(when { o.optBoolean("erledigt") -> ", erledigt"; o.optBoolean("in_aufgaben") -> ", steht in Geniale Aufgaben"; else -> ", offen" })
                    append("): ").append(o.optString("text").replace('\n', ' '))
                    o.optString("begruendung").takeIf { it.isNotBlank() }?.let { append(" Warum: ").append(it) }
                    append('\n')
                }
            }
            z.optString("bericht").takeIf { it.isNotBlank() }?.let { append("\nRECHERCHE-BERICHT:\n").append(it.take(8_000)) }
        }.trim()
    }

    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject = AppBruecke.unterbrechbar {
        AppBruecke.rufe(context, name, BRUECKE, methode, anfrage, lesend = methode in LESEND)
    }

    private inline fun JSONObject.fehlerOder(gut: (JSONObject) -> String): Ergebnis =
        optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(gut(this))

    companion object {
        private val BRUECKE: Uri = Uri.parse("content://de.frank.ziele.jarvis")
        private val LESEND = setOf("info", "lesen", "auswertung_stand", "kontext_lesen")
    }
}
