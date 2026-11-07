package de.frank.jarvis.faehigkeit

import java.time.LocalDate
import java.util.Locale
import org.json.JSONObject

/**
 * Schlanke Ergebnisprüfung für Aktionen über die Brücken der Partner-Apps (Wecker stellen, Aufgabe ändern …):
 * „Erledigt“ gibt es nur, wenn die Antwort der App den angeforderten Zustand zurückmeldet.
 *
 * - Normalfall ohne Zusatzkosten: Die Brücken melden das gespeicherte Objekt ohnehin zurück; verglichen werden nur die
 *   angeforderten Felder im Speicher (Mikrosekunden, kein weiterer Aufruf).
 * - Nur bei Bedarf (Zeitlimit oder Antwort ohne Zustand) genau eine gezielte Zustandsabfrage über das Lese-Werkzeug.
 * - Unbestätigt oder abweichend: Ergebnis mit fehler=true und klarem Hinweis, nicht blind erneut auszuführen.
 */
object Ergebnispruefung {
    /**
     * Angeforderter Wert eines Feldes: [feld] im Objekt der App, [soll] normalisiert, [wort] für Frank.
     * [pruefbar] = false für das gemeldete Objekt, wenn die App dieses Feld bewusst anders meldet (etwa tägliche Aufgaben mit heutigem Tag).
     */
    data class Soll(val feld: String, val soll: String?, val wort: String, val pruefbar: (JSONObject) -> Boolean = { true })

    /** Gleichbedeutende Schreibweisen angleichen: Groß/Klein, Leerraum, Uhrzeit „7:30“ = „07:30“, JSON-null = leer. */
    fun normal(wert: Any?): String {
        if (wert == null || wert == JSONObject.NULL) return ""
        val t = wert.toString().trim().lowercase(Locale.GERMAN).replace(Regex("\\s+"), " ")
        return UHRZEIT.matchEntire(t)?.let { "%02d:%s".format(it.groupValues[1].toInt(), it.groupValues[2]) } ?: t
    }

    /** heute/morgen/uebermorgen/JJJJ-MM-TT → JJJJ-MM-TT, „ohne“ → leer, Unlesbares → null (nicht prüfbar). */
    fun datum(eingabe: String, heute: LocalDate = LocalDate.now()): String? = when (val e = eingabe.trim().lowercase(Locale.GERMAN)) {
        "heute" -> heute.toString()
        "morgen" -> heute.plusDays(1).toString()
        "uebermorgen", "übermorgen" -> heute.plusDays(2).toString()
        "ohne", "" -> ""
        else -> runCatching { LocalDate.parse(e).toString() }.getOrNull()
    }

    /** „7:30“, „07.30“, „18“, „18 Uhr“ → „HH:MM“; „ohne“ → leer; Unlesbares → null (nicht prüfbar). */
    fun uhrzeit(eingabe: String): String? {
        val e = eingabe.trim().lowercase(Locale.GERMAN).removeSuffix("uhr").trim()
        if (e == "ohne" || e.isEmpty()) return ""
        val t = ZEIT_EINGABE.matchEntire(e) ?: return null
        val h = t.groupValues[1].toInt()
        val m = t.groupValues[2].ifEmpty { "00" }.toInt()
        return if (h in 0..23 && m in 0..59) "%02d:%02d".format(h, m) else null
    }

    /** Abweichungen zwischen Angefordertem und dem, was die App meldet; leer = bestätigt. Felder mit soll null werden nicht geprüft. */
    fun abweichungen(soll: List<Soll>, ist: JSONObject): List<String> = soll.filter { it.soll != null && it.pruefbar(ist) }.mapNotNull { s ->
        val gemeldet = if (ist.has(s.feld)) normal(ist.opt(s.feld)) else return@mapNotNull "${s.wort}: nicht zurückgemeldet"
        if (gemeldet == normal(s.soll)) null else "${s.wort}: angefordert „${s.soll.orEmpty().ifEmpty { "ohne" }}“, gemeldet „${gemeldet.ifEmpty { "ohne" }}“"
    }

    /**
     * Prüft die Antwort einer schreibenden Brückenmethode. [objekt] = Schlüssel des gemeldeten Objekts („wecker“, „aufgabe“),
     * [lesen] = Name des Lese-Werkzeugs für Frank und Modell, [nachlesen] = gezielte Abfrage des aktuellen Zustands (nur bei Bedarf),
     * [extra] = zusätzliche Abweichung aus dem Objekt (etwa „nicht geplant“), [neu] = die Aktion legt etwas an (dann ist es bei einer
     * Abweichung schon gespeichert und wird korrigiert, nie neu angelegt), [gut] = Text für den bestätigten Zustand.
     */
    suspend fun pruefe(
        antwort: JSONObject,
        objekt: String,
        soll: List<Soll>,
        lesen: String,
        nachlesen: (suspend () -> JSONObject?)? = null,
        extra: (JSONObject) -> String? = { null },
        neu: Boolean = false,
        gut: (JSONObject) -> String,
    ): Ergebnis {
        val fehler = antwort.optString("fehler")
        val zeitlimit = antwort.optBoolean(AppBruecke.ZEITLIMIT)
        // Ein klarer Fehler der App: nichts geändert, so sagen.
        if (fehler.isNotEmpty() && !zeitlimit) return Ergebnis(fehler, fehler = true)
        val gemeldet = if (zeitlimit) null else antwort.optJSONObject(objekt)
        // Unklar (Zeitlimit oder Antwort ohne Zustand): genau einmal gezielt nachlesen, nie die Änderung wiederholen.
        val ist = gemeldet ?: nachlesen?.let { lies ->
            // Ein Abbruch (Frank unterbricht) geht weiter nach oben, nur echte Lesefehler zählen als „nicht nachlesbar“.
            try { lies() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { null }
        }
            ?: return Ergebnis(
                (if (zeitlimit) "$fehler " else "") + "NICHT BESTÄTIGT: Die App hat keinen Zustand zurückgemeldet, ob die Änderung angekommen ist, ist offen. " +
                    "Nicht erneut ausführen; Frank sagen, dass es unbestätigt ist, und bei Bedarf mit $lesen nachsehen.", fehler = true,
            )
        val abweichend = abweichungen(soll, ist) + listOfNotNull(extra(ist))
        val nachgelesen = if (gemeldet == null) " (durch Nachlesen bestätigt)" else ""
        if (abweichend.isEmpty()) return Ergebnis(gut(ist) + nachgelesen)
        return Ergebnis(
            "NICHT BESTÄTIGT: " + abweichend.joinToString("; ") + ". Stand laut App: " + gut(ist) +
                if (neu) " Der Eintrag ist schon gespeichert: nicht noch einmal anlegen; Frank die Abweichung in einem Satz nennen und ihn bei Bedarf über seine id ändern."
                else " Nicht blind erneut ausführen; Frank die Abweichung in einem Satz nennen und fragen, ob du es noch einmal versuchen sollst.",
            fehler = true,
        )
    }

    /** Das Objekt mit dieser id aus einer Liste der App (für die gezielte Zustandsabfrage); null = nicht dabei. */
    fun nachId(liste: org.json.JSONArray?, id: String): JSONObject? =
        liste?.let { l -> (0 until l.length()).mapNotNull { l.optJSONObject(it) }.firstOrNull { it.optString("id") == id } }

    private val UHRZEIT = Regex("(\\d{1,2}):(\\d{2})")
    private val ZEIT_EINGABE = Regex("(\\d{1,2})(?:[:.](\\d{2}))?")
}
