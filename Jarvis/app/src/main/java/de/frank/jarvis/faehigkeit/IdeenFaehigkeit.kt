package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anbindung an „Geniale Ideen“ (de.frank.genialeideen) über deren Jarvis-Brücke (Signatur-Erlaubnis):
 * Ideen lesen, neue anlegen (die App glättet Text und Titel mit ihrer KI), ändern, als umgesetzt markieren, löschen.
 */
class IdeenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "ideen"
    override val name = "Geniale Ideen"
    override val beschreibung = "Ideen lesen, neue speichern (mit KI-Glättung), ändern, als umgesetzt markieren und löschen."
    override val hinweise =
        "Geniale Ideen ist Franks Ideensammlung. Eine Idee hat einen kurzen Titel (höchstens drei Wörter, bildet die App selbst), einen Text, " +
            "optional Kategorien und den Status offen oder umgesetzt. Neue Ideen übergibst du in Franks eigenen Worten; die App glättet den Text selbst " +
            "und behält das Original. Ideen sind etwas anderes als Aufgaben: Eine Idee ist ein Einfall zum Aufheben, eine Aufgabe etwas zu Erledigendes. " +
            "Sagt Frank „geniale Idee“ oder „Idee“, gehört es hierher. Vor dem Löschen kurz bestätigen lassen."

    /** Schützt vor doppelt angelegten Ideen, wenn ChatGPT einen Aufruf nach einer Zeitüberschreitung wiederholt. */
    private var letzteAnlage: Triple<String, Long, String>? = null
    private val anlageSperre = Mutex()

    override fun stoerung(): String? = rufeDirekt("kategorien", JSONObject()).optString("fehler").takeIf { it.isNotEmpty() }

    /** Alle offenen Ideen als kompakter Text, für die Tagesdatenbank. */
    suspend fun ueberblick(): String = rufe("lesen", JSONObject().put("status", "offen").put("limit", 200)).let { antwort ->
        antwort.optString("fehler").takeIf { it.isNotEmpty() }?.let { "NICHT VERFÜGBAR: $it" } ?: liste(antwort, 160).take(14_000)
    }

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            w("ideen_lesen").als(
                beschreibung = "Jarvis: liest Franks geniale Ideen aus der App Geniale Ideen. Nutze es für „Welche Ideen habe ich?“, „Was war meine Idee zu …?“, " +
                    "„Welche Ideen sind umgesetzt?“. In der Liste sind lange Texte gekürzt; mit id kommt eine einzelne Idee vollständig, samt dem bisherigen Gespräch dazu.",
                schema = w("ideen_lesen").schema.mit("id" to zahl("id einer Idee aus der Liste: dann nur diese, im Volltext.")),
            ) { a -> (if (a.gesetzt("id")) w("idee_lesen") else w("ideen_lesen")).ausfuehren(a) },
            Werkzeug(
                name = "idee_speichern",
                titel = "Idee speichern oder ändern",
                beschreibung = "Jarvis: speichert eine neue geniale Idee in der App Geniale Ideen oder ändert eine vorhandene. " +
                    "NEU (ohne id und ohne suche): bei „Jarvis, ich habe eine Idee …“, „speichere als geniale Idee …“. Übergib den Inhalt vollständig in Franks Worten; die App glättet den Text sprachlich und bildet den Titel selbst. " +
                    "ÄNDERN (mit id oder suche): neuer Text oder Titel, andere Kategorie, oder status umgesetzt bzw. wieder offen.",
                schema = schema(
                    "id" to zahl("Nur beim Ändern: id der Idee aus ideen_lesen."),
                    "suche" to text("Nur beim Ändern, statt id: Wort aus Titel oder Text der Idee."),
                    "text" to text("Die Idee vollständig, so wie Frank sie gesagt hat, ohne die Anrede an Jarvis. Pflicht bei einer neuen Idee."),
                    "titel" to text("Nur wenn Frank ausdrücklich einen Titel nennt (höchstens drei Wörter)."),
                    "kategorie" to text("Nur wenn Frank eine Kategorie nennt. Unbekannte Namen werden neu angelegt; ohne = Kategorie entfernen."),
                    "status" to text("Nur beim Ändern: umgesetzt oder offen.", listOf("offen", "umgesetzt")),
                    "verbessern" to schalter("Nur bei einer neuen Idee: Text von der KI glätten lassen (Vorgabe: an)."),
                ),
                nurLesen = false,
            ) { a ->
                // „Markiere die Idee X als umgesetzt“ kommt oft ohne id und suche, nur mit Titel oder Text der Idee.
                // Das ist eine Änderung, keine neue Idee: sonst entstünde ein Doppel oder der Fehler „braucht einen Text“.
                val nurAenderung = a.gesetzt("status") || (!a.gesetzt("text") && (a.gesetzt("titel") || a.has("kategorie")))
                when {
                    a.gesetzt("id") || a.gesetzt("suche") -> w("idee_aendern").ausfuehren(a)
                    nurAenderung -> {
                        val suche = a.optString("titel").trim().ifEmpty { a.optString("text").trim() }
                        if (suche.isEmpty()) Ergebnis("Zum Ändern fehlt die id oder ein Suchwort (suche) für die Idee. Hole die id mit ideen_lesen.", fehler = true)
                        else w("idee_aendern").ausfuehren(JSONObject(a.toString()).put("suche", suche).apply { remove("titel"); remove("text") })
                    }
                    else -> w("idee_anlegen").ausfuehren(a)
                }
            },
            w("idee_loeschen"),
        )
    }

    private fun w(name: String): Werkzeug = einzeln.first { it.name == name }

    private val einzeln: List<Werkzeug> = listOf(
        Werkzeug(
            name = "ideen_lesen",
            titel = "Ideen lesen",
            beschreibung = "Jarvis: liest Franks geniale Ideen aus der App Geniale Ideen. Nutze es für „Welche Ideen habe ich?“, „Was war meine Idee zu …?“, " +
                "„Welche Ideen sind umgesetzt?“. Lange Texte sind gekürzt; den vollen Text einer Idee liefert idee_lesen.",
            schema = schema(
                "status" to text("offen (Vorgabe), umgesetzt oder alle.", listOf("offen", "umgesetzt", "alle")),
                "suche" to text("Suchwort oder Wortgruppe; durchsucht Titel und Text."),
                "kategorie" to text("Nur Ideen dieser Kategorie (Name oder Teil davon)."),
            ),
            nurLesen = true,
        ) { a -> rufe("lesen", a).fehlerOder(::liste) },
        Werkzeug(
            name = "idee_lesen",
            titel = "Idee im Volltext",
            beschreibung = "Jarvis: liest eine einzelne Idee aus Geniale Ideen vollständig, samt dem bisherigen Gespräch dazu. Über id ODER Suchwort.",
            schema = schema("id" to zahl("id der Idee aus ideen_lesen."), "suche" to text("Statt id: Wort aus Titel oder Text.")),
            nurLesen = true,
        ) { a ->
            mitIdee(a) { id ->
                rufe("idee", JSONObject().put("id", id)).fehlerOder { antwort ->
                    val i = antwort.getJSONObject("idee")
                    buildString {
                        append(kopf(i)).append('\n').append(i.optString("text"))
                        val gespraech = antwort.optJSONArray("gespraech") ?: JSONArray()
                        if (gespraech.length() > 0) {
                            append("\n\nBisheriges Gespräch zur Idee:\n")
                            for (n in 0 until gespraech.length()) gespraech.getJSONObject(n).let { append(if (it.optString("rolle") == "user") "Frank: " else "KI: ").append(it.optString("text")).append('\n') }
                        }
                    }.trim()
                }
            }
        },
        Werkzeug(
            name = "idee_anlegen",
            titel = "Idee speichern",
            beschreibung = "Jarvis: speichert eine neue geniale Idee in der App Geniale Ideen. Nutze es bei „Jarvis, ich habe eine Idee …“, „speichere als geniale Idee …“, " +
                "„notiere die Idee …“. Übergib den Inhalt vollständig in Franks Worten; die App glättet den Text sprachlich und bildet den Titel selbst.",
            schema = schema(
                "text" to text("Die Idee vollständig, so wie Frank sie gesagt hat, ohne die Anrede an Jarvis."),
                "titel" to text("Nur wenn Frank ausdrücklich einen Titel nennt (höchstens drei Wörter). Sonst weglassen."),
                "kategorie" to text("Nur wenn Frank eine Kategorie nennt. Unbekannte Namen werden als neue Kategorie angelegt."),
                "verbessern" to schalter("Text von der KI glätten lassen (Vorgabe: an). Aus = wörtlich speichern."),
                pflicht = listOf("text"),
            ),
            nurLesen = false,
        ) { a ->
            val schluessel = a.optString("text").trim().lowercase()
            // Gesperrt, weil der Tunnel Aufrufe parallel abarbeitet: Ein wiederholter Aufruf wartet, bis der erste fertig ist.
            anlageSperre.withLock {
                letzteAnlage?.let { (alt, zeit, antwort) ->
                    if (alt == schluessel && System.currentTimeMillis() - zeit < 90_000) return@Werkzeug Ergebnis("$antwort (War bereits gespeichert, nicht doppelt angelegt.)")
                }
                rufe("anlegen", a).fehlerOder { "Idee gespeichert: „${it.getJSONObject("idee").optString("titel")}“ (id ${it.getJSONObject("idee").optLong("id")}). ${it.optString("hinweis")}" }.also {
                    if (!it.fehler) letzteAnlage = Triple(schluessel, System.currentTimeMillis(), it.text)
                }
            }
        },
        Werkzeug(
            name = "idee_aendern",
            titel = "Idee ändern",
            beschreibung = "Jarvis: ändert eine Idee in Geniale Ideen: neuer Text oder Titel, andere Kategorie, oder Status umgesetzt bzw. wieder offen. Über id ODER Suchwort.",
            schema = schema(
                "id" to zahl("id der Idee aus ideen_lesen."),
                "suche" to text("Statt id: Wort aus Titel oder Text."),
                "text" to text("Neuer vollständiger Text."),
                "titel" to text("Neuer Titel."),
                "kategorie" to text("Neue Kategorie; ohne = Kategorie entfernen."),
                "status" to text("umgesetzt oder offen.", listOf("offen", "umgesetzt")),
            ),
            nurLesen = false,
        ) { a ->
            mitIdee(a) { id -> rufe("aendern", JSONObject(a.toString()).put("id", id).apply { remove("suche") }).fehlerOder { "Geändert: " + kopf(it.getJSONObject("idee")) } }
        },
        Werkzeug(
            name = "idee_loeschen",
            titel = "Idee löschen",
            beschreibung = "Jarvis: löscht eine Idee endgültig aus Geniale Ideen. Nur aufrufen, wenn Frank das Löschen eindeutig verlangt hat. Über id ODER Suchwort.",
            schema = schema("id" to zahl("id der Idee aus ideen_lesen."), "suche" to text("Statt id: Wort aus Titel oder Text.")),
            nurLesen = false,
            loeschend = true,
        ) { a -> mitIdee(a) { id -> rufe("loeschen", JSONObject().put("id", id)).fehlerOder { "Gelöscht: „${it.getJSONObject("geloescht").optString("titel")}“." } } },
    )

    private suspend fun mitIdee(a: JSONObject, dann: suspend (Long) -> Ergebnis): Ergebnis {
        if (a.has("id") && !a.isNull("id")) return dann(a.optLong("id"))
        val suche = a.optString("suche").trim()
        if (suche.isEmpty()) return Ergebnis("Es fehlt die id oder ein Suchwort für die Idee.", fehler = true)
        val treffer = rufe("lesen", JSONObject().put("status", "alle").put("suche", suche))
        treffer.optString("fehler").takeIf { it.isNotEmpty() }?.let { return Ergebnis(it, fehler = true) }
        val ideen = treffer.optJSONArray("ideen") ?: JSONArray()
        return when (ideen.length()) {
            0 -> Ergebnis("Keine Idee gefunden, die zu „$suche“ passt.", fehler = true)
            1 -> dann(ideen.getJSONObject(0).getLong("id"))
            else -> Ergebnis("Mehrere Ideen passen zu „$suche“. Frage Frank, welche gemeint ist, und rufe das Werkzeug dann mit der id auf:\n" + liste(treffer), fehler = true)
        }
    }

    private fun kopf(i: JSONObject): String = buildString {
        append("[id ").append(i.optLong("id")).append("] ").append(i.optString("titel")).append(" (").append(i.optString("status"))
        val kategorien = i.optJSONArray("kategorien")
        if (kategorien != null && kategorien.length() > 0) append(", ").append((0 until kategorien.length()).joinToString("/") { kategorien.optString(it) })
        append(", angelegt ").append(i.optString("angelegt")).append(")")
    }

    private fun liste(antwort: JSONObject, laenge: Int = Int.MAX_VALUE): String {
        val ideen = antwort.optJSONArray("ideen") ?: JSONArray()
        if (ideen.length() == 0) return "Keine Ideen gefunden."
        return buildString {
            append(antwort.optInt("anzahl")).append(" Idee(n)")
            if (ideen.length() < antwort.optInt("anzahl")) append(", gezeigt die ersten ").append(ideen.length())
            append(":\n")
            for (n in 0 until ideen.length()) {
                val i = ideen.getJSONObject(n)
                append("- ").append(kopf(i)).append(": ").append(i.optString("text").replace('\n', ' ').let { if (it.length > laenge) it.take(laenge) + " …" else it }).append('\n')
            }
        }.trim()
    }

    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject = withContext(Dispatchers.IO) { rufeDirekt(methode, anfrage) }

    private fun rufeDirekt(methode: String, anfrage: JSONObject): JSONObject = try {
        val antwort = context.contentResolver.call(BRUECKE, methode, null, Bundle().apply { putString("json", anfrage.toString()) })
        antwort?.getString("json")?.let(::JSONObject) ?: fehler("Geniale Ideen hat nicht geantwortet.")
    } catch (_: SecurityException) {
        fehler("Geniale Ideen verweigert den Zugriff. Beide Apps müssen mit demselben Schlüssel signiert sein; danach Jarvis neu installieren.")
    } catch (_: IllegalArgumentException) {
        fehler("Geniale Ideen ist nicht installiert oder zu alt (die Jarvis-Brücke fehlt).")
    } catch (e: Exception) {
        fehler("Geniale Ideen ist nicht erreichbar: ${e.message ?: e.javaClass.simpleName}")
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    private inline fun JSONObject.fehlerOder(gut: (JSONObject) -> String): Ergebnis =
        optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(gut(this))

    companion object {
        private val BRUECKE: Uri = Uri.parse("content://de.frank.genialeideen.jarvis")
    }
}
