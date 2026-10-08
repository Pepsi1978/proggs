package de.frank.jarvis.faehigkeit

import android.content.Context
import android.net.Uri
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Anbindung an den Biomarker-Bereich von „Entropie Reductor“ (de.frank.entropyreducer) über dessen
 * Jarvis-Brücke (Signatur-Erlaubnis, nur lesend): Whoop, Oura, Körperwerte der Waage und Trainings.
 *
 * Gerechnet wird in Entropie Reductor selbst. Die Werkzeuge liefern fertige Kennzahlen (aktueller Wert,
 * Durchschnitte, Abweichung, Einordnung) statt roher Datenmengen — so ist eine Antwort im Sprachmodus
 * sofort da, und das Sprachmodell muss nur noch deuten und formulieren.
 */
class BiomarkerFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "biomarker"
    override val name = "Entropie Reductor · Biomarker"
    override val beschreibung = "Erholung, Schlaf, HRV, Körperwerte und Trainings lesen und mit den letzten Wochen und Monaten vergleichen."
    override val hinweise =
        "Entropie Reductor sammelt Franks Biomarker: vom Whoop-Band (Erholung, HRV, Ruhepuls, Schlaf mit Phasen, Hauttemperatur, Belastung), " +
            "vom Oura-Ring (Readiness, Schlafscore, Resilienz), von der Waage (Gewicht, Körperfett, Muskelmasse und weitere) und seine Trainings (Läufe mit Pace, Puls, VO2max). " +
            "Für Fragen wie „Wie sind meine Werte heute?“ oder „Vergleiche mit den letzten Wochen“ nimm biomarker_auswertung: Es liefert den aktuellen Wert, " +
            "die Durchschnitte und eine Einordnung fertig gerechnet. Nenne beim Antworten zuerst das Wichtigste (was auffällig besser oder schlechter ist), " +
            "nicht jede Zahl, und leite eine kurze, konkrete Empfehlung ab. Die Daten sind nur lesbar. Du stellst keine medizinischen Diagnosen."

    override fun stoerung(): String? = rufeDirekt("katalog", JSONObject()).optString("fehler").takeIf { it.isNotEmpty() }

    private val metrikListe =
        "erholung, hrv, ruhepuls, schlafperformance, schlafdauer, tiefschlaf_min, rem_min, leichtschlaf_min, wach_min, tiefschlaf_prozent, rem_prozent, " +
            "leichtschlaf_prozent, wach_prozent, erholsamer_schlaf_prozent, schlafeffizienz, schlafkonsistenz, schlafbedarf, schlafdefizit_min, schlafstoerungen, " +
            "schlafzyklen, atemfrequenz, spo2, hauttemperatur, tagesbelastung, energie_kcal, oura_readiness, oura_schlafscore, oura_resilienz, oura_aktivitaet, " +
            "schritte, oura_temperaturabweichung, vo2max, gewicht, koerperfett, muskelmasse, skelettmuskel, magermasse, knochenmasse, viszeralfett, bmi, " +
            "koerperwasser, wasseranteil, eiweiss"

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "biomarker_auswertung",
            titel = "Biomarker auswerten",
            beschreibung = "Jarvis: wertet Franks Biomarker aus Entropie Reductor aus. Vergleicht den aktuellen Wert jeder Messgröße mit dem Durchschnitt der Tage davor " +
                "(Vorgabe 30 Tage), nennt zusätzlich die Schnitte der letzten 7, 30 und 90 Tage, die Abweichung und eine Einordnung (besser, schlechter oder im üblichen Bereich). " +
                "Das erste Werkzeug für „Wie sind meine Biodaten heute?“, „Vergleiche mit den letzten Wochen oder Monaten“, „Wie ist meine Erholung?“, „Hat sich mein Schlaf verschlechtert?“. " +
                "Antwortet sofort, weil auf dem Handy gerechnet wird.",
            schema = schema(
                "tage" to zahl("Vergleichszeitraum in Tagen vor dem aktuellen Wert: 7 = letzte Woche, 30 = letzter Monat (Vorgabe), 90 = Quartal, 365 = Jahr."),
                "datum" to text("Bezugstag, falls nicht heute: heute, gestern oder JJJJ-MM-TT."),
                "metriken" to textListe("Nur wenn Frank nach bestimmten Werten fragt. Ohne Angabe kommen die wichtigsten. Erlaubte ids: $metrikListe"),
            ),
            nurLesen = true,
        ) { a -> rufe("auswertung", a).fehlerOder(::auswertungText) },
        Werkzeug(
            name = "biomarker_tag",
            titel = "Biomarker eines Tages",
            beschreibung = "Jarvis: liest ALLE Biomarker-Werte eines einzelnen Tages aus Entropie Reductor (Whoop, Oura, Waage) samt den Trainings dieses Tages. " +
                "Nutze es für „Wie war mein Schlaf heute Nacht?“, „Was waren meine Werte am 3. Oktober?“. Ohne Datum kommt der neueste Tag mit Daten.",
            schema = schema("datum" to text("heute, gestern, vorgestern oder JJJJ-MM-TT. Weglassen = neuester Tag mit Daten.")),
            nurLesen = true,
        ) { a -> rufe("tag", a).fehlerOder(::tagText) },
        Werkzeug(
            name = "biomarker_verlauf",
            titel = "Biomarker-Verlauf",
            beschreibung = "Jarvis: liest den zeitlichen Verlauf einzelner Messgrößen aus Entropie Reductor, tageweise oder als Wochen- bzw. Monatsdurchschnitt. " +
                "Nutze es für Entwicklungen über längere Zeit („Wie hat sich mein Gewicht seit Juli entwickelt?“, „HRV der letzten drei Monate“). " +
                "Für lange Zeiträume aufloesung woche oder monat wählen, damit die Antwort kurz bleibt.",
            schema = schema(
                "metriken" to textListe("Ein bis fünf ids aus: $metrikListe"),
                "von" to text("Erster Tag JJJJ-MM-TT (Vorgabe: 30 Tage vor bis)."),
                "bis" to text("Letzter Tag JJJJ-MM-TT (Vorgabe: heute)."),
                "aufloesung" to text("tag = jeder Tag, woche = Wochenschnitte, monat = Monatsschnitte.", listOf("tag", "woche", "monat")),
                pflicht = listOf("metriken"),
            ),
            nurLesen = true,
        ) { a -> rufe("verlauf", a).fehlerOder(::verlaufText) },
        Werkzeug(
            name = "trainings_lesen",
            titel = "Trainings lesen",
            beschreibung = "Jarvis: listet Franks Trainings aus Entropie Reductor (Läufe und andere Sportarten) mit Datum, Distanz, Dauer, Durchschnitts- und Maximalpace, " +
                "Durchschnitts- und Maximalpuls, Kalorien und VO2max, dazu die Summen des Zeitraums. Vorgabe: die letzten 90 Tage.",
            schema = schema(
                "von" to text("Erster Tag JJJJ-MM-TT."),
                "bis" to text("Letzter Tag JJJJ-MM-TT (Vorgabe: heute)."),
                "sport" to text("Nur eine Sportart, z. B. Laufen."),
                "limit" to zahl("Höchstzahl der Trainings (Vorgabe 30, neueste zuerst)."),
            ),
            nurLesen = true,
        ) { a -> rufe("trainings", a).fehlerOder(::trainingsText) },
        Werkzeug(
            name = "training_details",
            titel = "Training im Detail",
            beschreibung = "Jarvis: liest ein einzelnes Training aus Entropie Reductor mit allen Werten und den Kilometer-Abschnitten. Die id stammt aus trainings_lesen.",
            schema = schema("id" to text("id des Trainings aus trainings_lesen."), pflicht = listOf("id")),
            nurLesen = true,
        ) { a ->
            rufe("training", a).fehlerOder { antwort ->
                val t = antwort.getJSONObject("training")
                trainingZeile(t) + "\nWeitere Werte: " + t.toString()
            }
        },
        Werkzeug(
            name = "biomarker_katalog",
            titel = "Biomarker-Katalog",
            beschreibung = "Jarvis: nennt alle Messgrößen, die Entropie Reductor führt, mit Einheit, Quelle und dem Zeitraum, für den Daten vorliegen. " +
                "Nutze es, wenn Frank fragt, welche Daten es gibt oder seit wann.",
            schema = schema(),
            nurLesen = true,
        ) { a ->
            rufe("katalog", a).fehlerOder { antwort ->
                val liste = antwort.getJSONArray("metriken")
                buildString {
                    append("Heute ist ").append(antwort.optString("heute")).append(". Messgrößen in Entropie Reductor:\n")
                    for (i in 0 until liste.length()) {
                        val m = liste.getJSONObject(i)
                        append("- ").append(m.optString("id")).append(": ").append(m.optString("name")).append(" [").append(m.optString("einheit")).append(", ").append(m.optString("quelle")).append("] ")
                        if (m.optInt("tage") == 0) append("keine Daten") else append(m.optInt("tage")).append(" Tage, ").append(m.optString("von")).append(" bis ").append(m.optString("bis"))
                        append('\n')
                    }
                }.trim()
            }
        },
    )

    // ---------------------------------------------------------------- Texte für die Modelle

    private fun auswertungText(antwort: JSONObject): String {
        val liste = antwort.getJSONArray("metriken")
        if (liste.length() == 0) return "Für diesen Zeitraum liegen keine Biomarker vor."
        val alle = (0 until liste.length()).map { liste.getJSONObject(it) }
        fun namen(teil: String) = alle.filter { teil in it.optString("einordnung") }.joinToString(", ") { it.optString("name") }
        return buildString {
            append("Biomarker-Auswertung, Bezugstag ").append(antwort.optString("bezugstag")).append(" (heute ist ").append(antwort.optString("heute"))
            append("), verglichen mit den ").append(antwort.optInt("vergleich_tage")).append(" Tagen davor.\n")
            namen("schlechter").takeIf { it.isNotEmpty() }?.let { append("Schlechter als üblich: ").append(it).append(".\n") }
            namen("besser").takeIf { it.isNotEmpty() }?.let { append("Besser als üblich: ").append(it).append(".\n") }
            alle.forEach { m ->
                val einheit = m.optString("einheit")
                append("- ").append(m.optString("name")).append(" (").append(m.optString("quelle")).append("): ").append(zahlText(m.opt("aktuell"))).append(' ').append(einheit)
                m.optString("aktuell_text").takeIf { it.isNotEmpty() }?.let { append(" = ").append(it) }
                val alter = m.optLong("alter_tage")
                if (alter > 0) append(" (Messung vom ").append(m.optString("datum")).append(", ").append(alter).append(" Tage alt)")
                if (!m.isNull("schnitt_vergleich")) {
                    append("; Schnitt davor ").append(zahlText(m.opt("schnitt_vergleich")))
                    if (!m.isNull("abweichung_prozent")) append(", Abweichung ").append(vorzeichen(m.optDouble("abweichung"))).append(" (").append(vorzeichen(m.optDouble("abweichung_prozent"))).append(" %)")
                    append(" → ").append(m.optString("einordnung"))
                }
                append("; Schnitte 7/30/90 Tage: ").append(zahlText(m.opt("schnitt_7"))).append(" / ").append(zahlText(m.opt("schnitt_30"))).append(" / ").append(zahlText(m.opt("schnitt_90")))
                if (!m.isNull("min_vergleich")) append("; Spanne ").append(zahlText(m.opt("min_vergleich"))).append("–").append(zahlText(m.opt("max_vergleich")))
                if (!m.isNull("trend_7_tage")) append("; letzte 7 Tage gegen die 7 davor: ").append(vorzeichen(m.optDouble("trend_7_tage")))
                append(" [höher ist ").append(m.optString("hoeher_ist")).append("]\n")
            }
        }.trim()
    }

    private fun tagText(antwort: JSONObject): String {
        val werte = antwort.getJSONArray("werte")
        val trainings = antwort.optJSONArray("trainings") ?: JSONArray()
        if (werte.length() == 0 && trainings.length() == 0) return "Für den ${antwort.optString("datum")} liegen keine Biomarker vor. (Heute ist ${antwort.optString("heute")}.)"
        return buildString {
            append("Biomarker vom ").append(antwort.optString("datum")).append(" (heute ist ").append(antwort.optString("heute")).append("):\n")
            var quelle = ""
            for (i in 0 until werte.length()) {
                val w = werte.getJSONObject(i)
                if (w.optString("quelle") != quelle) { quelle = w.optString("quelle"); append(quelle).append(":\n") }
                append("- ").append(w.optString("name")).append(": ").append(zahlText(w.opt("wert"))).append(' ').append(w.optString("einheit"))
                w.optString("text").takeIf { it.isNotEmpty() }?.let { append(" = ").append(it) }
                append('\n')
            }
            if (trainings.length() > 0) {
                append("Trainings:\n")
                for (i in 0 until trainings.length()) append("- ").append(trainingZeile(trainings.getJSONObject(i))).append('\n')
            }
            antwort.optString("hinweis").takeIf { it.isNotEmpty() }?.let { append("Hinweis: ").append(it) }
        }.trim()
    }

    private fun verlaufText(antwort: JSONObject): String {
        val reihen = antwort.getJSONArray("reihen")
        return buildString {
            append("Verlauf ").append(antwort.optString("von")).append(" bis ").append(antwort.optString("bis")).append(", Auflösung ").append(antwort.optString("aufloesung")).append(":\n")
            for (i in 0 until reihen.length()) {
                val r = reihen.getJSONObject(i)
                val punkte = r.getJSONArray("punkte")
                append(r.optString("name")).append(" [").append(r.optString("einheit")).append(", ").append(r.optString("quelle")).append("], ").append(r.optInt("anzahl_tage")).append(" Tage mit Daten")
                if (punkte.length() == 0) { append(": keine Daten\n"); continue }
                val zahlen = (0 until punkte.length()).map { punkte.getJSONArray(it).optDouble(1) }
                append(", Schnitt ").append(zahlText(Math.round(zahlen.average() * 100) / 100.0)).append(", Spanne ").append(zahlText(zahlen.min())).append("–").append(zahlText(zahlen.max())).append(":\n")
                append((0 until punkte.length()).joinToString("; ") { p -> punkte.getJSONArray(p).let { it.optString(0) + " " + zahlText(it.opt(1)) } }).append('\n')
            }
        }.trim()
    }

    private fun trainingsText(antwort: JSONObject): String {
        val liste = antwort.getJSONArray("trainings")
        if (liste.length() == 0) return "Keine Trainings zwischen ${antwort.optString("von")} und ${antwort.optString("bis")}."
        return buildString {
            append(antwort.optInt("anzahl")).append(" Trainings von ").append(antwort.optString("von")).append(" bis ").append(antwort.optString("bis"))
            append(", zusammen ").append(zahlText(antwort.opt("summe_km"))).append(" km in ").append(zahlText(antwort.opt("summe_stunden"))).append(" Stunden")
            if (liste.length() < antwort.optInt("anzahl")) append(" (gezeigt: die neuesten ").append(liste.length()).append(")")
            append(":\n")
            for (i in 0 until liste.length()) append("- ").append(trainingZeile(liste.getJSONObject(i))).append('\n')
        }.trim()
    }

    private fun trainingZeile(t: JSONObject): String = buildString {
        append(t.optString("datum")).append(' ').append(t.optString("start")).append(' ').append(t.optString("sport"))
        if (!t.isNull("distanz_km")) append(", ").append(zahlText(t.opt("distanz_km"))).append(" km")
        if (!t.isNull("dauer_min")) append(", ").append(t.optLong("dauer_min")).append(" min")
        if (!t.isNull("pace_schnitt")) append(", Pace ").append(t.optString("pace_schnitt")).append(" min/km")
        if (!t.isNull("pace_max")) append(" (schnellste ").append(t.optString("pace_max")).append(")")
        if (!t.isNull("puls_schnitt")) append(", Puls ").append(t.optInt("puls_schnitt"))
        if (!t.isNull("puls_max")) append(" (max ").append(t.optInt("puls_max")).append(")")
        if (!t.isNull("kalorien")) append(", ").append(t.optLong("kalorien")).append(" kcal")
        if (!t.isNull("vo2max")) append(", VO2max ").append(zahlText(t.opt("vo2max")))
        append(" [id ").append(t.optString("id")).append("]")
    }

    /** Zahlen mit deutschem Komma und ohne überflüssige Nullen. */
    private fun zahlText(wert: Any?): String = when (wert) {
        null, JSONObject.NULL -> "–"
        is Int, is Long -> wert.toString()
        is Number -> wert.toDouble().let { if (it == Math.floor(it) && kotlin.math.abs(it) < 1e12) it.toLong().toString() else it.toString().replace('.', ',') }
        else -> wert.toString()
    }

    private fun vorzeichen(wert: Double): String = (if (wert > 0) "+" else "") + zahlText(wert)

    // ---------------------------------------------------------------- Brücke

    private suspend fun rufe(methode: String, anfrage: JSONObject): JSONObject = withContext(Dispatchers.IO) { rufeDirekt(methode, anfrage) }

    private fun rufeDirekt(methode: String, anfrage: JSONObject): JSONObject {
        var letzter = fehler("Entropie Reductor ist nicht installiert oder zu alt (die Jarvis-Brücke fehlt).")
        // Auf dem Handy liegt die Debug-Variante; die zweite Adresse greift, falls einmal die andere installiert ist.
        for (adresse in BRUECKEN) {
            try {
                val antwort = context.contentResolver.call(adresse, methode, null, Bundle().apply { putString("json", anfrage.toString()) })
                return antwort?.getString("json")?.let(::JSONObject) ?: fehler("Entropie Reductor hat nicht geantwortet.")
            } catch (_: SecurityException) {
                return fehler("Entropie Reductor verweigert den Zugriff. Beide Apps müssen mit demselben Schlüssel signiert sein; danach Jarvis neu installieren.")
            } catch (_: IllegalArgumentException) {
                // Diese Variante ist nicht installiert: nächste Adresse versuchen.
            } catch (e: Exception) {
                letzter = fehler("Entropie Reductor ist nicht erreichbar: ${e.message ?: e.javaClass.simpleName}")
            }
        }
        return letzter
    }

    private fun fehler(text: String) = JSONObject().put("fehler", text)

    private inline fun JSONObject.fehlerOder(gut: (JSONObject) -> String): Ergebnis =
        optString("fehler").takeIf { it.isNotEmpty() }?.let { Ergebnis(it, fehler = true) } ?: Ergebnis(gut(this))

    companion object {
        private val BRUECKEN = listOf(
            Uri.parse("content://de.frank.entropyreducer.debug.jarvis"),
            Uri.parse("content://de.frank.entropyreducer.jarvis"),
        )
    }
}
