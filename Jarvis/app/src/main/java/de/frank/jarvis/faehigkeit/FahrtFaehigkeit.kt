package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.fahrt.Abfahrt
import de.frank.jarvis.fahrt.Ort
import de.frank.jarvis.fahrt.Routen
import de.frank.jarvis.fahrt.RoutenFehler
import de.frank.jarvis.fahrt.Standort
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONObject

/**
 * Fahrzeiten mit dem Auto über Google Maps (Routes API, mit aktuellem Verkehr) und die Losfahr-Meldung vor dem Dienst
 * (siehe [Abfahrt]). Start ist der Standort des Handys, ersatzweise Franks Zuhause.
 */
class FahrtFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "fahrt"
    override val name = "Fahrzeit und Abfahrt"
    override val beschreibung = "Fahrzeit mit dem Auto über Google Maps mit aktuellem Verkehr; meldet an Arbeitstagen rechtzeitig, wann Frank losfahren muss."
    override val hinweise =
        "Jarvis kennt über Google Maps die Fahrzeit mit dem Auto samt aktuellem Verkehr (Stau, Sperrungen). Nutze fahrzeit_lesen für „Wie lange brauche ich zur Arbeit?“, " +
            "„Wie lange brauche ich nach Hause?“, „Wie lange fahre ich zum Alexanderplatz?“ und „Wann muss ich los, um um 17 Uhr da zu sein?“. Start ist Franks aktueller Standort, " +
            "wenn er nichts anderes nennt. An Arbeitstagen meldet sich Jarvis von selbst kurz vor der nötigen Abfahrt; die Ankunftszeiten und Adressen ändert abfahrt_einstellen. " +
            "Jede Abfrage zählt gegen ein kostenloses Monatskontingent: Frage nur ab, wenn Frank eine Fahrzeit wissen will."

    private val e get() = Einstellungen.get(context)

    override fun stoerung(): String? = if (e.mapsSchluessel.isBlank()) "Der Google-Maps-Schlüssel fehlt. In Jarvis unter Einstellungen → Pünktlich losfahren eintragen." else null

    /** „arbeit“ und „zuhause“ sind Franks gespeicherte Adressen, alles andere eine Adresse oder ein Ort. */
    private fun benannt(text: String): String? = text.trim().lowercase(Locale.GERMAN).let {
        when {
            it in setOf("arbeit", "auf arbeit", "zur arbeit", "dienst", "museumsinsel") -> e.adresseArbeit
            it in setOf("zuhause", "zu hause", "nach hause", "heim", "wohnung") -> e.adresseZuhause
            else -> null
        }
    }

    private fun zeit(text: String): LocalTime? = Regex("^\\s*([01]?\\d|2[0-3])(?:[:.]([0-5]\\d))?").find(text)?.takeIf { text.isNotBlank() }
        ?.let { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toIntOrNull() ?: 0) }

    /** Die Zeit heute; ist sie schon vorbei, morgen. */
    private fun naechste(zeit: LocalTime): ZonedDateTime = ZonedDateTime.now().let { jetzt -> jetzt.with(zeit).let { if (it.isAfter(jetzt)) it else it.plusDays(1) } }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "fahrzeit_lesen",
            titel = "Fahrzeit mit Verkehr",
            beschreibung = "Jarvis: Fahrzeit mit dem Auto über Google Maps mit aktuellem Verkehr, Stau und Sperrungen. ziel ist „arbeit“, „zuhause“ oder eine beliebige Adresse bzw. ein Ort. " +
                "Ohne start gilt Franks aktueller Standort (ersatzweise sein Zuhause). Mit ankunft (HH:MM) kommt zusätzlich, wann er losfahren muss; mit abfahrt (HH:MM) die Fahrzeit zu dieser Abfahrtszeit. " +
                "Beispiele: „Wie lange brauche ich jetzt zur Arbeit?“ → ziel arbeit. „Wann muss ich los, um um 17 Uhr auf Arbeit zu sein?“ → ziel arbeit, ankunft 17:00. " +
                "Die Antwort nennt außerdem den Stand der Losfahr-Meldung für den nächsten Dienst.",
            schema = schema(
                "ziel" to text("„arbeit“, „zuhause“ oder eine Adresse bzw. ein Ort, zum Beispiel „Alexanderplatz, Berlin“."),
                "start" to text("Nur wenn Frank einen anderen Start nennt: „zuhause“, „arbeit“ oder eine Adresse. Weglassen = aktueller Standort."),
                "ankunft" to text("Gewünschte Ankunft HH:MM, wenn Frank wissen will, wann er losfahren muss."),
                "abfahrt" to text("Geplante Abfahrt HH:MM, wenn er zu einer bestimmten Zeit losfahren will."),
                pflicht = listOf("ziel"),
            ),
            nurLesen = true,
        ) { a -> runCatching { Ergebnis(fahrzeit(a)) }.getOrElse { Ergebnis(if (it is RoutenFehler) it.message.orEmpty() else "Die Fahrzeit ließ sich nicht berechnen: ${it.message ?: it.javaClass.simpleName}", fehler = true) } },
        Werkzeug(
            name = "abfahrt_einstellen",
            titel = "Losfahr-Meldung einstellen",
            beschreibung = "Jarvis: stellt die Losfahr-Meldung ein, die Frank an Arbeitstagen rechtzeitig vor der nötigen Abfahrt bekommt (Benachrichtigung, auf Wunsch vorgelesen). " +
                "Nur die genannten Angaben ändern sich. Ohne Angaben kommt der aktuelle Stand. Beispiele: „Ich will zur Nachtschicht um 17:15 da sein“ → ankunft_nacht 17:15. " +
                "„Sag mir 15 Minuten vorher Bescheid“ → vorlauf 15. „Schalte die Losfahr-Meldung aus“ → an false.",
            schema = schema(
                "an" to schalter("Meldung ein- oder ausschalten."),
                "ankunft_nacht" to text("Gewünschte Ankunft auf Arbeit vor dem Nachtdienst, HH:MM."),
                "ankunft_tag" to text("Gewünschte Ankunft auf Arbeit vor dem Tagdienst, HH:MM."),
                "vorlauf" to zahl("So viele Minuten vor der nötigen Abfahrt kommt die Meldung (0 bis 60)."),
                "vorlesen" to schalter("Meldung mit der eingerichteten Stimme vorlesen."),
                "lautstaerke" to zahl("Lautstärke der vorgelesenen Meldung in Prozent (10 bis 100, Vorgabe 100)."),
                "zuhause" to text("Neue Zuhause-Adresse, nur wenn Frank sie ändern will."),
                "arbeit" to text("Neue Arbeits-Adresse, nur wenn Frank sie ändern will."),
            ),
            nurLesen = false,
        ) { a -> Ergebnis(stelleEin(a)) },
    )

    private suspend fun fahrzeit(a: JSONObject): String {
        val zielText = a.optString("ziel").trim().ifEmpty { throw RoutenFehler("Wohin soll es gehen? Nenne „arbeit“, „zuhause“ oder eine Adresse.") }
        val ziel = benannt(zielText) ?: zielText
        val startText = a.optString("start").trim()
        val vomStandort = startText.isEmpty() || startText.lowercase(Locale.GERMAN) in setOf("standort", "hier", "aktueller standort", "aktuell")
        var startName: String
        val von: Ort
        if (vomStandort) {
            val ort = Standort.aktuell(context)
            if (ort != null) { von = Ort.Punkt(ort.latitude, ort.longitude); startName = "deinem Standort" }
            else {
                von = Ort.Adresse(e.adresseZuhause)
                startName = "zu Hause (" + (if (Standort.erlaubt(context)) "der Standort ist gerade nicht verfügbar" else "der Standort ist für Jarvis noch nicht erlaubt") + ")"
            }
        } else {
            val adresse = benannt(startText) ?: startText
            von = Ort.Adresse(adresse); startName = adresse
        }
        val uhr = DateTimeFormatter.ofPattern("HH:mm")
        val ankunft = zeit(a.optString("ankunft"))?.let(::naechste)
        val abfahrt = zeit(a.optString("abfahrt"))?.let(::naechste)
        return buildString {
            append("Mit dem Auto von ").append(startName).append(" nach ").append(ziel).append(": ")
            val route = when {
                ankunft != null -> {
                    val (r, los) = Routen.fuerAnkunft(context, von, Ort.Adresse(ziel), ankunft)
                    append(r.minuten).append(" Minuten. Für die Ankunft um ").append(ankunft.format(uhr)).append(" Uhr musst du um ").append(los.format(uhr)).append(" Uhr losfahren")
                    if (los.isBefore(ZonedDateTime.now())) append(" – das ist schon vorbei, frühestens bist du um ").append(ZonedDateTime.now().plusSeconds(r.sekunden.toLong()).format(uhr)).append(" Uhr da")
                    append(". ")
                    r
                }
                abfahrt != null -> Routen.berechne(context, von, Ort.Adresse(ziel), abfahrt).also { r ->
                    append(r.minuten).append(" Minuten bei Abfahrt um ").append(abfahrt.format(uhr)).append(" Uhr, Ankunft etwa ").append(abfahrt.plusSeconds(r.sekunden.toLong()).format(uhr)).append(" Uhr. ")
                }
                else -> Routen.berechne(context, von, Ort.Adresse(ziel)).also { r ->
                    append(r.minuten).append(" Minuten mit dem Verkehr von jetzt, Ankunft etwa ").append(ZonedDateTime.now().plusSeconds(r.sekunden.toLong()).format(uhr)).append(" Uhr. ")
                }
            }
            val mehr = route.minuten - route.minutenOhneVerkehr
            append(if (mehr >= 3) "Das sind $mehr Minuten mehr als bei freier Strecke (${route.minutenOhneVerkehr} Minuten). " else "Die Strecke ist frei. ")
            append(route.km).append(" km")
            if (route.ueber.isNotBlank()) append(" über ").append(route.ueber)
            append(".\nLosfahr-Meldung: ").append(Abfahrt.status(context))
        }
    }

    private fun stelleEin(a: JSONObject): String {
        val geaendert = mutableListOf<String>()
        fun uhrzeit(feld: String, was: String, setze: (String) -> Unit) {
            if (!a.gesetzt(feld)) return
            val z = zeit(a.optString(feld)) ?: return Unit.also { geaendert += "$was nicht verstanden (${a.optString(feld)}), Format HH:MM" }
            setze("%02d:%02d".format(z.hour, z.minute)); geaendert += "$was ${"%02d:%02d".format(z.hour, z.minute)} Uhr"
        }
        if (a.has("an") && !a.isNull("an")) { e.abfahrtAn = a.optBoolean("an"); geaendert += if (e.abfahrtAn) "Meldung eingeschaltet" else "Meldung ausgeschaltet" }
        uhrzeit("ankunft_nacht", "Ankunft vor dem Nachtdienst") { e.ankunftNacht = it }
        uhrzeit("ankunft_tag", "Ankunft vor dem Tagdienst") { e.ankunftTag = it }
        if (a.has("vorlauf") && !a.isNull("vorlauf")) { e.abfahrtVorlauf = a.optInt("vorlauf", 10).coerceIn(0, 60); geaendert += "Meldung ${e.abfahrtVorlauf} Minuten vor der Abfahrt" }
        if (a.has("vorlesen") && !a.isNull("vorlesen")) { e.abfahrtVorlesen = a.optBoolean("vorlesen"); geaendert += if (e.abfahrtVorlesen) "wird vorgelesen" else "wird nicht vorgelesen" }
        if (a.has("lautstaerke") && !a.isNull("lautstaerke")) { e.abfahrtLautstaerke = a.optInt("lautstaerke", 100); geaendert += "Lautstärke ${e.abfahrtLautstaerke} Prozent" }
        if (a.gesetzt("zuhause")) { e.adresseZuhause = a.optString("zuhause"); geaendert += "Zuhause: ${e.adresseZuhause}" }
        if (a.gesetzt("arbeit")) { e.adresseArbeit = a.optString("arbeit"); geaendert += "Arbeit: ${e.adresseArbeit}" }
        if (geaendert.isNotEmpty()) {
            // Die laufende Planung gilt für die alten Angaben: neu rechnen lassen.
            e.abfahrtZustand = ""
            Abfahrt.stelle(context)
        }
        return buildString {
            if (geaendert.isNotEmpty()) append("Geändert: ").append(geaendert.joinToString("; ")).append(".\n")
            append("Losfahr-Meldung ").append(if (e.abfahrtAn) "an" else "aus").append(": Ankunft vor dem Nachtdienst ").append(e.ankunftNacht).append(" Uhr, vor dem Tagdienst ").append(e.ankunftTag)
            append(" Uhr, Meldung ").append(e.abfahrtVorlauf).append(" Minuten vor der Abfahrt, ").append(if (e.abfahrtVorlesen) "mit Vorlesen" else "ohne Vorlesen").append(".\n")
            append("Zuhause: ").append(e.adresseZuhause).append(". Arbeit: ").append(e.adresseArbeit).append(".\n")
            append("Stand: ").append(Abfahrt.status(context)).append('\n')
            append("Google-Maps-Abfragen diesen Monat: ").append(Routen.verbraucht(context)).append(" von ").append(Routen.FREI_IM_MONAT).append(" kostenlosen.")
        }
    }
}
