package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anbindung an „Genialer Wecker“ (de.frank.genialerwecker) über dessen Jarvis-Brücke (Signatur-Erlaubnis):
 * Wecker lesen, stellen, ändern, schalten, den nächsten Termin auslassen und löschen.
 */
class WeckerFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "wecker"
    override val name = "Genialer Wecker"
    override val beschreibung = "Wecker lesen, stellen, ändern, aus- und einschalten und löschen."
    override val hinweise =
        "Genialer Wecker ist Franks Wecker-App. Ein Wecker hat einen Namen, eine Uhrzeit, eine Wiederholung (einmalig, täglich, Wochentage, alle X Tage, monatlich, jährlich) " +
            "und einen Weckablauf aus Schritten in fester Reihenfolge: ton (kurzes Klingelzeichen, der Standard-Piepton), aufgaben (liest die Aufgaben des Tages vor), " +
            "ideen (liest offene Ideen vor), text (liest einen eigenen Text vor), musik (Weckton oder die in der App gewählte MP3). " +
            "Nennt Frank beim Stellen keinen Weckablauf, frage kurz nach und schlage „Klingelzeichen und danach die Aufgaben vorlesen“ vor; fragt er nach den Möglichkeiten, zähle die fünf Schritte auf. " +
            "Eine MP3 wählt Frank in der App selbst aus. Wecker sind etwas anderes als Aufgaben-Erinnerungen: Sagt Frank „Wecker“, gehört es hierher."

    override fun stoerung(): String? = rufeDirekt("optionen", JSONObject()).optString("fehler").takeIf { it.isNotEmpty() }

    /** Alle Wecker als kurzer Text, für die Tagesdatenbank. */
    suspend fun ueberblick(): String = rufe("lesen", JSONObject()).let { a -> a.optString("fehler").takeIf { it.isNotEmpty() }?.let { "NICHT VERFÜGBAR: $it" } ?: liste(a) }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "wecker_lesen",
            titel = "Wecker lesen",
            beschreibung = "Jarvis: liest alle Wecker aus Franks App Genialer Wecker: Name, Uhrzeit, ob eingeschaltet, wann er das nächste Mal klingelt, Wiederholung und Weckablauf. " +
                "Nutze es für „Welche Wecker sind gestellt?“, „Wann klingelt mein Wecker?“ und um vor dem Ändern die id zu finden. Mit optionen=true kommen stattdessen die möglichen Einstellungen.",
            schema = schema("optionen" to schalter("true = die möglichen Weckschritte, Töne und Wiederholungen zeigen statt der Wecker.")),
            nurLesen = true,
        ) { a ->
            if (a.optBoolean("optionen")) rufe("optionen", JSONObject()).fehlerOder { o ->
                val schritte = o.getJSONArray("ablauf")
                "Mögliche Schritte im Weckablauf (in beliebiger Reihenfolge kombinierbar):\n" +
                    (0 until schritte.length()).joinToString("\n") { n -> schritte.getJSONObject(n).let { "- ${it.optString("id")}: ${it.optString("name")} – ${it.optString("beschreibung")}" } } +
                    "\nWiederholungen: einmalig (auch an einem bestimmten Datum), täglich, werktags, bestimmte Wochentage, alle X Tage, monatlich, jährlich." +
                    "\nÜblicher Ablauf: Klingelzeichen, danach die Aufgaben vorlesen."
            } else rufe("lesen", JSONObject()).fehlerOder(::liste)
        },
        Werkzeug(
            name = "wecker_stellen",
            titel = "Wecker stellen oder ändern",
            beschreibung = "Jarvis: stellt einen neuen Wecker in Genialer Wecker oder ändert einen vorhandenen (dann mit id oder suche; nur die genannten Felder ändern sich). " +
                "Beispiele: „Stelle einen Wecker ‚Erinnerung an Manu‘ für morgen 8:30 Uhr“ → name, uhrzeit 08:30, datum morgen. „Jeden Tag um 7:30 Uhr“ → wiederholung taeglich. " +
                "Zum Aus- oder Einschalten nur aktiv angeben, zum Auslassen des nächsten Termins naechsten_auslassen. " +
                "WICHTIG bei einem NEUEN Wecker: Nennt Frank keinen Weckablauf, frage zuerst nach („Wie soll der Wecker dich wecken? Üblich ist Klingelzeichen und danach die Aufgaben vorlesen.“) und rufe erst dann auf.",
            schema = schema(
                "id" to text("Nur beim Ändern: id des Weckers aus wecker_lesen."),
                "suche" to text("Nur beim Ändern, statt id: Wort aus dem Namen des Weckers."),
                "name" to text("Name des Weckers, zum Beispiel „Erinnerung an Manu“."),
                "uhrzeit" to text("Weckzeit als HH:MM (24 Stunden). Pflicht bei einem neuen Wecker."),
                "datum" to text("Für einen einmaligen Wecker an einem bestimmten Tag: heute, morgen, uebermorgen oder JJJJ-MM-TT. Auch Startdatum bei intervall, monatlich, jaehrlich."),
                "wiederholung" to text("einmalig (Vorgabe), taeglich, werktags, wochenende, wochentage, intervall, monatlich, jaehrlich.", listOf("einmalig", "taeglich", "werktags", "wochenende", "wochentage", "intervall", "monatlich", "jaehrlich")),
                "wochentage" to textListe("Bei wiederholung=wochentage: Montag, Dienstag, … (oder 1 bis 7, 1 = Montag)."),
                "intervall_tage" to zahl("Bei wiederholung=intervall: alle wie viele Tage."),
                "ablauf" to textListe("Weckablauf in der gewünschten Reihenfolge aus: ton, aufgaben, ideen, text, musik. Beispiel: [\"ton\", \"aufgaben\"]."),
                "text" to text("Nur für den Schritt text: was der Wecker vorlesen soll."),
                "lautstaerke" to zahl("Lautstärke 1 bis 100, nur wenn genannt."),
                "schlummer_minuten" to zahl("Länge der Schlummerpause in Minuten, nur wenn genannt."),
                "aktiv" to schalter("false = ausschalten, true = einschalten. Beim Stellen ist er an."),
                "naechsten_auslassen" to schalter("true = nur den nächsten Termin eines wiederholenden Weckers auslassen."),
            ),
            nurLesen = false,
        ) { a ->
            val aendert = (a.has("id") && !a.isNull("id")) || a.optString("suche").isNotBlank()
            if (!aendert) return@Werkzeug rufe("stellen", a).fehlerOder { "Wecker gestellt: " + satz(it.getJSONObject("wecker")) + planung(it) }
            mitWecker(a) { id ->
                val felder = JSONObject(a.toString()).apply { remove("suche"); remove("naechsten_auslassen"); put("id", id) }
                when {
                    a.optBoolean("naechsten_auslassen") -> rufe("auslassen", JSONObject().put("id", id)).fehlerOder { "Nächster Termin ausgelassen: " + satz(it.getJSONObject("wecker")) }
                    // Nur an oder aus: der schlanke Weg, der den Rest unberührt lässt.
                    a.has("aktiv") && felder.length() == 2 -> rufe("schalten", felder).fehlerOder { (if (a.optBoolean("aktiv")) "Eingeschaltet: " else "Ausgeschaltet: ") + satz(it.getJSONObject("wecker")) }
                    else -> rufe("stellen", felder).fehlerOder { "Wecker geändert: " + satz(it.getJSONObject("wecker")) + planung(it) }
                }
            }
        },
        Werkzeug(
            name = "wecker_loeschen",
            titel = "Wecker löschen",
            beschreibung = "Jarvis: löscht einen Wecker endgültig aus Genialer Wecker. Nur wenn Frank das Löschen eindeutig verlangt; zum Pausieren reicht wecker_stellen mit aktiv=false.",
            schema = schema("id" to text("id des Weckers aus wecker_lesen."), "suche" to text("Statt id: Wort aus dem Namen des Weckers.")),
            nurLesen = false,
            loeschend = true,
        ) { a -> mitWecker(a) { id -> rufe("loeschen", JSONObject().put("id", id)).fehlerOder { "Gelöscht: Wecker „${it.getJSONObject("geloescht").optString("name")}“." } } },
    )

    private fun planung(antwort: JSONObject): String =
        (if (!antwort.optBoolean("geplant", true)) " ACHTUNG: gespeichert, aber nicht geplant – in der Wecker-App nachsehen." else "") +
            antwort.optString("hinweis").takeIf { it.isNotEmpty() }?.let { " $it" }.orEmpty()

    private suspend fun mitWecker(a: JSONObject, dann: suspend (String) -> Ergebnis): Ergebnis {
        a.optString("id").takeIf { it.isNotBlank() && !a.isNull("id") }?.let { return dann(it) }
        val suche = a.optString("suche").trim().lowercase()
        if (suche.isEmpty()) return Ergebnis("Es fehlt die id oder ein Suchwort für den Wecker.", fehler = true)
        val alle = rufe("lesen", JSONObject())
        alle.optString("fehler").takeIf { it.isNotEmpty() }?.let { return Ergebnis(it, fehler = true) }
        val wecker = alle.optJSONArray("wecker") ?: JSONArray()
        val treffer = (0 until wecker.length()).map { wecker.getJSONObject(it) }.filter { suche in it.optString("name").lowercase() || suche in it.optString("uhrzeit") }
        return when (treffer.size) {
            0 -> Ergebnis("Kein Wecker passt zu „$suche“.\n" + liste(alle), fehler = true)
            1 -> dann(treffer.first().getString("id"))
            else -> Ergebnis("Mehrere Wecker passen zu „$suche“. Frage Frank, welcher gemeint ist, und rufe dann mit der id auf:\n" + treffer.joinToString("\n") { "- " + satz(it) }, fehler = true)
        }
    }

    private fun satz(w: JSONObject): String = buildString {
        append("„").append(w.optString("name")).append("“ um ").append(w.optString("uhrzeit")).append(" Uhr, ").append(w.optString("wiederholung"))
        append(if (w.optBoolean("aktiv")) ", eingeschaltet" else ", AUSGESCHALTET")
        if (!w.isNull("naechstes_klingeln")) append(", klingelt als Nächstes ").append(w.optString("naechstes_klingeln").replace('T', ' '))
        val ablauf = w.optJSONArray("ablauf")
        if (ablauf != null && ablauf.length() > 0) append(", Ablauf: ").append((0 until ablauf.length()).joinToString(" → ") { ablauf.optString(it) })
        if (w.optString("text").isNotBlank()) append(", Text: „").append(w.optString("text").take(120)).append("“")
        if (w.optString("ausgelassen_bis").isNotBlank()) append(", ausgelassen bis ").append(w.optString("ausgelassen_bis"))
        if (w.optString("hinweis").isNotBlank()) append(", Hinweis: ").append(w.optString("hinweis"))
        append(" [id ").append(w.optString("id")).append("]")
    }

    private fun liste(antwort: JSONObject): String {
        val wecker = antwort.optJSONArray("wecker") ?: JSONArray()
        if (wecker.length() == 0) return "Es ist kein Wecker angelegt."
        return "Jetzt ist ${antwort.optString("jetzt").replace('T', ' ')}. ${wecker.length()} Wecker:\n" + (0 until wecker.length()).joinToString("\n") { "- " + satz(wecker.getJSONObject(it)) }
    }

    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject = withContext(Dispatchers.IO) { rufeDirekt(methode, anfrage) }

    private fun rufeDirekt(methode: String, anfrage: JSONObject): JSONObject = try {
        val antwort = context.contentResolver.call(BRUECKE, methode, null, Bundle().apply { putString("json", anfrage.toString()) })
        antwort?.getString("json")?.let(::JSONObject) ?: fehler("Genialer Wecker hat nicht geantwortet.")
    } catch (_: SecurityException) {
        fehler("Genialer Wecker verweigert den Zugriff. Beide Apps müssen mit demselben Schlüssel signiert sein; danach Jarvis neu installieren.")
    } catch (_: IllegalArgumentException) {
        fehler("Genialer Wecker ist nicht installiert oder zu alt (die Jarvis-Brücke fehlt).")
    } catch (e: Exception) {
        fehler("Genialer Wecker ist nicht erreichbar: ${e.message ?: e.javaClass.simpleName}")
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    private inline fun JSONObject.fehlerOder(gut: (JSONObject) -> String): Ergebnis =
        optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(gut(this))

    companion object {
        private val BRUECKE: Uri = Uri.parse("content://de.frank.genialerwecker.jarvis")
    }
}
