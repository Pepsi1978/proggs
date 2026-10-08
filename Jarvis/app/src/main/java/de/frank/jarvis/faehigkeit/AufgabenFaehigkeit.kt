package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anbindung an „Geniale Aufgaben“ (de.frank.aufgaben) über deren Jarvis-Brücke
 * (`content://de.frank.aufgaben.jarvis`, Signatur-Erlaubnis). Alles, was man in der App mit einer
 * Aufgabe tun kann, steht hier als Werkzeug bereit.
 */
class AufgabenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "aufgaben"
    override val name = "Geniale Aufgaben"
    override val beschreibung = "Aufgaben und Termine lesen, anlegen, ändern, verschieben, erledigen und löschen."
    override val hinweise =
        "Geniale Aufgaben ist Franks Aufgaben- und Termin-App. Eine Aufgabe hat Text, automatisch gebildeten Titel, " +
            "optional Tag und Uhrzeit, Priorität, Erinnerung (wird standardmäßig vorgelesen), Wiederholung und Checkliste. " +
            "Nennt Frank eine Uhrzeit, aber keinen Tag, und ist der Tag nicht eindeutig: frage nach „heute oder morgen?“, " +
            "rate nie. Ohne Uhrzeit und ohne Tag landet die Aufgabe im Eingang („Später“). " +
            "Zum Ändern, Erledigen oder Löschen reicht ein Suchwort aus dem Titel; bei mehreren Treffern nachfragen. " +
            "Vor dem Löschen kurz bestätigen lassen, außer Frank hat es eindeutig verlangt."

    override fun stoerung(): String? {
        val antwort = rufeDirekt("info", JSONObject())
        return antwort.optString("fehler").takeIf { it.isNotEmpty() }
    }

    /** Schützt vor doppelt angelegten Aufgaben, wenn ChatGPT einen Aufruf nach einer Zeitüberschreitung wiederholt. */
    private var letzteAnlage: Triple<String, Long, String>? = null

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            w("aufgaben_lesen"), w("aufgabe_anlegen"),
            w("aufgabe_aendern").als(
                beschreibung = "Jarvis: ändert, verschiebt oder hakt eine vorhandene Aufgabe in Geniale Aufgaben ab. Zum Abhaken nur erledigt=true angeben (false öffnet sie wieder). " +
                    "Sonst die Felder angeben, die sich ändern: Tag, Uhrzeit, Text, Priorität, Erinnerung, Wiederholung, Checkliste. Die Aufgabe wird über id ODER ein Suchwort (suche) bestimmt.",
                schema = w("aufgabe_aendern").schema.mit("erledigt" to schalter("true = als erledigt abhaken, false = wieder öffnen.")),
            ) { a ->
                val nurAbhaken = a.has("erledigt") && a.keys().asSequence().all { it in setOf("id", "suche", "erledigt") }
                (if (nurAbhaken) w("aufgabe_erledigen") else w("aufgabe_aendern")).ausfuehren(a)
            },
            w("aufgabe_loeschen"),
        )
    }

    private fun w(name: String): Werkzeug = einzeln.first { it.name == name }

    private val einzeln: List<Werkzeug> = listOf(
        Werkzeug(
            name = "aufgaben_lesen",
            titel = "Aufgaben lesen",
            beschreibung = "Jarvis: liest Franks Aufgaben und Termine aus der App Geniale Aufgaben. " +
                "Nutze es für Fragen wie „Was steht heute an?“, „Was habe ich morgen um 17 Uhr?“, „Welche Aufgaben sind offen?“ " +
                "und um vor dem Ändern die passende Aufgabe zu finden. Die Antwort nennt je Aufgabe die id.",
            schema = schema(
                "bereich" to text(
                    "heute = heute plus Überfälliges; morgen; tag = ein bestimmtes Datum (dann datum angeben); " +
                        "demnaechst = ab übermorgen; ohne_tag = Eingang ohne Tag; offen = alles Offene; erledigt; alle.",
                    listOf("heute", "morgen", "tag", "demnaechst", "ohne_tag", "offen", "erledigt", "alle"),
                ),
                "datum" to text("Nur bei bereich=tag: Datum als JJJJ-MM-TT."),
                "suche" to text("Optionales Suchwort; filtert Titel und Text."),
                pflicht = listOf("bereich"),
            ),
            nurLesen = true,
        ) { a ->
            val antwort = rufe("lesen", a)
            antwort.fehlerOder { liste(it) }
        },
        Werkzeug(
            name = "aufgabe_anlegen",
            titel = "Aufgabe speichern",
            beschreibung = "Jarvis: speichert eine neue Aufgabe oder einen Termin in Geniale Aufgaben. " +
                "Nutze es, wenn Frank sagt „Jarvis, speichere …“, „merk dir …“, „ich möchte um 18 Uhr …“, „erinnere mich …“. " +
                "WICHTIG: Nennt Frank eine Uhrzeit, aber keinen Tag, und ist der Tag nicht eindeutig, rufe das Werkzeug NICHT auf, " +
                "sondern frage zuerst kurz nach („Heute oder morgen?“). Fehlen andere Angaben, nimm die Vorgaben. " +
                "Den Titel bildet die App selbst; Erinnerung mit Vorlesen ist standardmäßig an.",
            schema = schema(
                "text" to text("Der Aufgabentext in Franks Worten, sauber ausformuliert, ohne die Anrede an Jarvis. Beispiel: „Bratkartoffeln braten und dafür die neuen Kartoffeln nutzen.“"),
                "datum" to text("Tag der Aufgabe: heute, morgen, uebermorgen, ein Datum JJJJ-MM-TT oder ohne (kein Tag, landet im Eingang)."),
                "uhrzeit" to text("Uhrzeit als HH:MM (24 Stunden), z. B. 18:00. Weglassen, wenn keine genannt wurde."),
                "titel" to text("Nur wenn Frank ausdrücklich einen Titel nennt (höchstens 35 Zeichen). Sonst weglassen."),
                "prioritaet" to text("Nur wenn genannt.", listOf("hoch", "mittel", "gering", "spaeter")),
                "dauer" to zahl("Dauer in Minuten, nur wenn genannt (Vorgabe 30)."),
                "erinnerung" to schalter("Erinnerung zur Uhrzeit (Vorgabe: an)."),
                "vorlauf" to zahl("Wie viele Minuten vorher erinnert wird, nur wenn genannt."),
                "vorlesen" to schalter("Erinnerung liest die Aufgabe vor (Vorgabe: an)."),
                "als_wecker" to schalter("Erinnerung läuft als Wecker, bis sie ausgeschaltet wird (Vorgabe: aus)."),
                "wiederholung" to text("Nur wenn genannt.", listOf("keine", "taeglich", "werktags", "woechentlich", "monatlich")),
                "schritte" to textListe("Checkliste: einzelne Teilschritte, nur wenn Frank welche aufzählt."),
                pflicht = listOf("text", "datum"),
            ),
            nurLesen = false,
        ) { a ->
            val schluessel = listOf(a.optString("text").trim().lowercase(), a.optString("datum"), a.optString("uhrzeit")).joinToString("|")
            letzteAnlage?.let { (alt, zeit, antwort) ->
                if (alt == schluessel && System.currentTimeMillis() - zeit < 90_000) return@Werkzeug Ergebnis("$antwort (War bereits gespeichert, nicht doppelt angelegt.)")
            }
            val antwort = rufe("anlegen", a)
            antwort.fehlerOder { "Gespeichert: " + satz(it.getJSONObject("aufgabe")) + mitgedacht(it.getJSONObject("aufgabe")) }.also {
                if (!it.fehler) letzteAnlage = Triple(schluessel, System.currentTimeMillis(), it.text)
            }
        },
        Werkzeug(
            name = "aufgabe_aendern",
            titel = "Aufgabe ändern",
            beschreibung = "Jarvis: ändert oder verschiebt eine vorhandene Aufgabe in Geniale Aufgaben (anderer Tag, andere Uhrzeit, " +
                "neuer Text, Priorität, Erinnerung, Wiederholung, Checkliste). Gib nur die Felder an, die sich ändern. " +
                "Die Aufgabe wird über id ODER über ein Suchwort (suche) bestimmt.",
            schema = schema(
                "id" to zahl("id der Aufgabe aus aufgaben_lesen."),
                "suche" to text("Statt id: Wort oder Wortgruppe aus Titel oder Text der Aufgabe."),
                "text" to text("Neuer Aufgabentext."),
                "titel" to text("Neuer Titel (höchstens 35 Zeichen)."),
                "datum" to text("Neuer Tag: heute, morgen, uebermorgen, JJJJ-MM-TT oder ohne (Tag entfernen)."),
                "uhrzeit" to text("Neue Uhrzeit HH:MM oder ohne (Uhrzeit entfernen)."),
                "prioritaet" to text("Neue Priorität.", listOf("hoch", "mittel", "gering", "spaeter")),
                "dauer" to zahl("Dauer in Minuten."),
                "erinnerung" to schalter("Erinnerung an oder aus."),
                "vorlauf" to zahl("Minuten vor dem Termin."),
                "vorlesen" to schalter("Vorlesen an oder aus."),
                "als_wecker" to schalter("Als Wecker an oder aus."),
                "wiederholung" to text("Wiederholung.", listOf("keine", "taeglich", "werktags", "woechentlich", "monatlich")),
                "schritte" to textListe("Ersetzt die Checkliste vollständig."),
            ),
            nurLesen = false,
        ) { a ->
            mitAufgabe(a, nurOffene = true) { id ->
                rufe("aendern", JSONObject(a.toString()).put("id", id).apply { remove("suche") })
                    .fehlerOder { "Geändert: " + satz(it.getJSONObject("aufgabe")) + mitgedacht(it.getJSONObject("aufgabe")) }
            }
        },
        Werkzeug(
            name = "aufgabe_erledigen",
            titel = "Aufgabe abhaken",
            beschreibung = "Jarvis: hakt eine Aufgabe in Geniale Aufgaben als erledigt ab (oder öffnet sie mit erledigt=false wieder). " +
                "Die Aufgabe wird über id ODER über ein Suchwort bestimmt.",
            schema = schema(
                "id" to zahl("id der Aufgabe aus aufgaben_lesen."),
                "suche" to text("Statt id: Wort oder Wortgruppe aus Titel oder Text der Aufgabe."),
                "erledigt" to schalter("true = abhaken (Vorgabe), false = wieder öffnen."),
            ),
            nurLesen = false,
        ) { a ->
            val erledigt = a.optBoolean("erledigt", true)
            mitAufgabe(a, nurOffene = erledigt) { id ->
                rufe("erledigen", JSONObject().put("id", id).put("erledigt", erledigt)).fehlerOder {
                    val titel = it.getJSONObject("aufgabe").optString("titel")
                    val folge = it.optJSONObject("naechste_wiederholung")?.let { n -> " Nächster Termin der Wiederholung: ${n.optString("tag_anzeige")}." }.orEmpty()
                    (if (erledigt) "Erledigt: „$titel“." else "Wieder offen: „$titel“.") + folge
                }
            }
        },
        Werkzeug(
            name = "aufgabe_loeschen",
            titel = "Aufgabe löschen",
            beschreibung = "Jarvis: löscht eine Aufgabe endgültig aus Geniale Aufgaben. Nur aufrufen, wenn Frank das Löschen " +
                "eindeutig verlangt hat. Die Aufgabe wird über id ODER über ein Suchwort bestimmt.",
            schema = schema(
                "id" to zahl("id der Aufgabe aus aufgaben_lesen."),
                "suche" to text("Statt id: Wort oder Wortgruppe aus Titel oder Text der Aufgabe."),
            ),
            nurLesen = false,
            loeschend = true,
        ) { a ->
            mitAufgabe(a, nurOffene = false) { id ->
                rufe("loeschen", JSONObject().put("id", id)).fehlerOder { "Gelöscht: „${it.getJSONObject("geloescht").optString("titel")}“." }
            }
        },
    )

    /** Bestimmt die gemeinte Aufgabe über id oder Suchwort. Mehrere Treffer → Rückfrage statt Raten. */
    private suspend fun mitAufgabe(a: JSONObject, nurOffene: Boolean, dann: suspend (Long) -> Ergebnis): Ergebnis {
        if (a.has("id") && !a.isNull("id")) return dann(a.optLong("id"))
        val suche = a.optString("suche").trim()
        if (suche.isEmpty()) return Ergebnis("Es fehlt die id oder ein Suchwort für die Aufgabe.", fehler = true)
        val treffer = rufe("lesen", JSONObject().put("bereich", if (nurOffene) "offen" else "alle").put("suche", suche))
        treffer.optString("fehler").takeIf { it.isNotEmpty() }?.let { return Ergebnis(it, fehler = true) }
        val aufgaben = treffer.optJSONArray("aufgaben") ?: JSONArray()
        return when (aufgaben.length()) {
            0 -> Ergebnis("Keine Aufgabe gefunden, die zu „$suche“ passt.", fehler = true)
            1 -> dann(aufgaben.getJSONObject(0).getLong("id"))
            else -> Ergebnis("Mehrere Aufgaben passen zu „$suche“. Frage Frank, welche gemeint ist, und rufe das Werkzeug dann mit der id auf:\n" + liste(treffer), fehler = true)
        }
    }

    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject = withContext(Dispatchers.IO) { rufeDirekt(methode, anfrage) }

    private fun rufeDirekt(methode: String, anfrage: JSONObject): JSONObject = try {
        val antwort = context.contentResolver.call(BRUECKE, methode, null, Bundle().apply { putString("json", anfrage.toString()) })
        antwort?.getString("json")?.let(::JSONObject) ?: fehler("Geniale Aufgaben hat nicht geantwortet.")
    } catch (_: SecurityException) {
        fehler("Geniale Aufgaben verweigert den Zugriff. Beide Apps müssen mit demselben Schlüssel signiert sein; danach Jarvis neu installieren.")
    } catch (_: IllegalArgumentException) {
        fehler("Geniale Aufgaben ist nicht installiert oder zu alt (die Jarvis-Brücke fehlt).")
    } catch (e: Exception) {
        fehler("Geniale Aufgaben ist nicht erreichbar: ${e.message ?: e.javaClass.simpleName}")
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    private inline fun JSONObject.fehlerOder(gut: (JSONObject) -> String): Ergebnis =
        optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(gut(this))

    /** Was Dienstplan, Termine und Wetter zu dieser Aufgabe sagen (leer, wenn sie keinen Tag hat oder nichts dagegen spricht). */
    private fun mitgedacht(a: JSONObject): String =
        if (a.isNull("datum")) "" else Mitdenken.anhang(context, a.optString("datum"), if (a.isNull("uhrzeit")) null else a.optString("uhrzeit"), a.optString("titel") + " " + a.optString("text"))

    /** Eine Aufgabe als gesprochener Satz. */
    private fun satz(a: JSONObject): String = buildString {
        append("„").append(a.optString("titel")).append("“")
        if (a.isNull("datum")) append(", ohne Tag (Eingang)") else append(", ").append(a.optString("tag_anzeige"))
        if (!a.isNull("uhrzeit")) append(" um ").append(a.optString("uhrzeit")).append(" Uhr")
        if (a.optBoolean("erinnerung")) {
            append(", Erinnerung ")
            append(if (a.optInt("vorlauf") > 0) "${a.optInt("vorlauf")} Minuten vorher" else "pünktlich")
            if (a.optBoolean("vorlesen")) append(" mit Vorlesen")
            if (a.optBoolean("als_wecker")) append(" als Wecker")
        }
        if (a.optString("wiederholung") != "KEINE") append(", Wiederholung ").append(a.optString("wiederholung").lowercase())
        append(". (id ").append(a.optLong("id")).append(")")
    }

    private fun liste(antwort: JSONObject): String {
        val aufgaben = antwort.optJSONArray("aufgaben") ?: JSONArray()
        if (aufgaben.length() == 0) return "Keine Aufgaben. (Heute ist ${antwort.optString("heute")}.)"
        return buildString {
            append("Heute ist ").append(antwort.optString("heute")).append(". ").append(antwort.optInt("anzahl")).append(" Aufgabe(n):\n")
            for (i in 0 until aufgaben.length()) {
                val a = aufgaben.getJSONObject(i)
                append("- [id ").append(a.optLong("id")).append("] ")
                append(a.optString("tag_anzeige"))
                if (!a.isNull("uhrzeit")) append(" ").append(a.optString("uhrzeit")).append(" Uhr")
                append(": ").append(a.optString("titel"))
                val text = a.optString("text")
                if (text.isNotBlank() && text != a.optString("titel")) append(" — ").append(text.take(240))
                append(" (Priorität ").append(a.optString("prioritaet").lowercase())
                if (a.optBoolean("ueberfaellig")) append(", überfällig")
                if (a.optBoolean("erledigt")) append(", erledigt")
                if (a.optString("wiederholung") != "KEINE") append(", ").append(a.optString("wiederholung").lowercase())
                append(")")
                val schritte = a.optJSONArray("schritte")
                if (schritte != null && schritte.length() > 0) {
                    append(" Checkliste: ")
                    append((0 until schritte.length()).joinToString("; ") { s -> schritte.getJSONObject(s).let { (if (it.optBoolean("erledigt")) "✓ " else "○ ") + it.optString("text") } })
                }
                append('\n')
            }
        }.trim()
    }

    companion object {
        private val BRUECKE: Uri = Uri.parse("content://de.frank.aufgaben.jarvis")
    }
}
