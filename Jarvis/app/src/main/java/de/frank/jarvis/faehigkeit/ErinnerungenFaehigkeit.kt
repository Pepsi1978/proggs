package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.ablage.ErinnerungPlaner
import de.frank.jarvis.ablage.ErinnerungStimme
import de.frank.jarvis.ablage.Erinnerung
import de.frank.jarvis.ablage.Erinnerungen
import de.frank.jarvis.ablage.Faellig
import de.frank.jarvis.data.Einstellungen
import org.json.JSONObject
import java.time.LocalDate

/** Der Reiter „Erinnerung“ der Ablage per Sprache: datierte Erinnerungen mit mehreren Auslösungen und allgemeine ohne Termin. */
class ErinnerungenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "erinnerungen"
    override val name = "Erinnerungen"
    override val beschreibung = "Erinnerungen mit Datum und allgemeine Erinnerungen ohne Termin anlegen, lesen, bearbeiten, Erinnerungszeitpunkte und Form einstellen, bestätigen."
    override val hinweise =
        "„Erinnere mich am 12.8.2027 an …“ und „Erinnere mich später an …“ gehören NUR hierher (erinnerung_speichern, aktion anlegen), nie zusätzlich in Aufgaben, Kalender oder Wecker. " +
            "Aufgaben, Kalender und Wecker nur, wenn Frank das ausdrücklich so sagt („leg eine Aufgabe an“, „trag einen Termin ein“, „stell einen Wecker“). " +
            "Beim Anlegen schreibst du drei Fassungen: text (vollständig in Franks Sinn, jeder Punkt, jeder Name, jede Zahl), kurz (höchstens 35 Wörter, für sich allein verständlich, zum Vorlesen, ohne Anrede) und zeile (etwa sechs Wörter für die Liste). " +
            "Eine nicht genannte Uhrzeit nie erfinden: uhrzeit dann weglassen. Ohne Datum wird sie eine allgemeine Erinnerung; dafür NICHT nach einem Termin fragen, Jarvis entscheidet selbst, wann ein Hinweis passt. " +
            "Form: Beim Anlegen fragst du vor dem Speichern immer ausdrücklich „Benachrichtigung oder Wecker?“ und speicherst Franks Auswahl in form; ohne seine Antwort legst du die Erinnerung nicht an. Vorgelesen wird immer. " +
            "Allgemeine offene Erinnerungen sprichst du von dir aus an, wenn sie gerade wirklich passen (Gesprächsthema, freie Zeit), nie mehrfach kurz hintereinander und nicht, wenn zuletzt_erwähnt von heute ist; danach aktion erwaehnt. " +
            "Bestätigen betrifft genau eine Auslösung, die übrigen bleiben geplant; Bearbeiten ist keine Bestätigung. ids nie vorlesen."

    override fun stoerung(): String? = null

    private fun finde(a: JSONObject): Pair<Erinnerung?, String?> {
        val alle = Erinnerungen.alle(context)
        if (a.gesetzt("id")) return (alle.firstOrNull { it.id == a.optString("id") } to "Diese Erinnerung gibt es nicht.").let { (e, f) -> e to (if (e == null) f else null) }
        val wort = a.optString("suche").trim().lowercase()
        if (wort.isEmpty()) return null to "Es fehlt id oder suche."
        val treffer = alle.filter { wort in it.zeile.lowercase() || wort in it.kurz.lowercase() || wort in it.text.lowercase() }
        return when {
            treffer.size == 1 -> treffer[0] to null
            treffer.isEmpty() -> null to "Keine Erinnerung passt zu „$wort“."
            else -> null to "Mehrere passen, bitte genauer: " + treffer.take(8).joinToString("; ") { it.zeile }
        }
    }

    private fun datumAus(t: String): LocalDate? {
        val s = t.trim()
        runCatching { return LocalDate.parse(s) }
        Regex("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2,4})").find(s)?.destructured?.let { (d, m, j) ->
            return runCatching { LocalDate.of(if (j.length == 2) 2000 + j.toInt() else j.toInt(), m.toInt(), d.toInt()) }.getOrNull()
        }
        return null
    }

    private fun uhrzeitAus(t: String) = Mitdenken.uhrzeitMinuten(t)?.let { String.format(java.util.Locale.GERMAN, "%02d:%02d", it / 60, it % 60) }

    private fun vorabAus(t: String) = t.split(',', ';').mapNotNull { it.trim().toIntOrNull()?.takeIf { m -> m >= 0 } }.distinct()

    private fun kopf(e: Erinnerung) = "${e.datumAnzeige()}: ${e.zeile} [id ${e.id}]" + (if (e.wecker) " (Wecker)" else "") + (if (e.erledigt != 0L) " (erledigt)" else "")

    private fun stand(f: Faellig, jetzt: Long): String {
        val q = Erinnerungen.quittung(context, f.quittung)
        return "- ${f.anzeige}: ${Erinnerungen.zeitText(f.zeit)} — " + when {
            q != null -> "bestätigt auf ${q.von}"
            f.zeit > jetzt -> "geplant"
            !Erinnerungen.meldbar(f, jetzt) -> "verpasst oder beim Einstellen schon vorbei"
            else -> "fällig, noch nicht bestätigt"
        } + (f.ausloesung?.let { " [ausloesung_id ${it.id}]" } ?: "")
    }

    private fun voll(e: Erinnerung): String {
        val jetzt = System.currentTimeMillis()
        return buildString {
            append(kopf(e)).append('\n')
            append("Kurzfassung: ").append(e.kurz).append("\n\n").append(e.text)
            if (e.allgemein) {
                append("\n\nAllgemeine Erinnerung ohne Termin.")
                if (e.zuletztErwaehnt > 0) append(" Zuletzt erwähnt: ").append(Erinnerungen.zeitText(e.zuletztErwaehnt)).append('.')
            }
            val t = e.termine()
            if (t.isNotEmpty()) append("\n\nZeitpunkte:\n").append(t.joinToString("\n") { stand(it, jetzt) })
            append("\nSprachausgabe offline: ").append(if (ErinnerungStimme.bereit(context, e.kurz)) "bereit" else "noch nicht erzeugt")
        }
    }

    /** Für Live-Gedächtnis: bevorstehende und offene Erinnerungen, je eine Zeile. */
    fun gedaechtnis(): String {
        val jetzt = System.currentTimeMillis()
        val faellig = Erinnerungen.faellig(context, jetzt)
        val kommend = Erinnerungen.naechste(context, jetzt)
        val datiert = Erinnerungen.datiert(context, jetzt).filter { it.erledigt == 0L && (it.termin() ?: 0L) >= jetzt }
        val allgemein = Erinnerungen.allgemeine(context).filter { it.erledigt == 0L }
        if (faellig.isEmpty() && datiert.isEmpty() && allgemein.isEmpty()) return "Keine offenen Erinnerungen."
        return buildString {
            if (faellig.isNotEmpty()) append("Fällig und noch nicht bestätigt:\n").append(faellig.joinToString("\n") { "- ${it.erinnerung.zeile}: ${it.erinnerung.kurz} [id ${it.erinnerung.id}]" }).append('\n')
            if (datiert.isNotEmpty()) append("Mit Datum:\n").append(datiert.take(40).joinToString("\n") { e ->
                "- ${e.datumAnzeige()}: ${e.kurz}" + (kommend.firstOrNull { it.erinnerung.id == e.id }?.let { " (nächste Erinnerung ${Erinnerungen.zeitText(it.zeit)})" } ?: "") + (if (e.wecker) " (Wecker)" else "") + " [id ${e.id}]"
            }).append('\n')
            if (allgemein.isNotEmpty()) append("Allgemein, ohne Termin (ansprechen, wenn es passt):\n").append(allgemein.take(40).joinToString("\n") { e ->
                "- ${e.kurz}" + (if (e.zuletztErwaehnt > 0) " (zuletzt erwähnt ${Erinnerungen.zeitText(e.zuletztErwaehnt)})" else "") + " [id ${e.id}]"
            }).append('\n')
        }.trim()
    }

    /** Für die Tagesauswertung: Fälliges, die nächsten 14 Tage und die allgemeinen. */
    fun ueberblick(): String {
        val jetzt = System.currentTimeMillis()
        val grenze = jetzt + 14 * 86_400_000L
        val faellig = Erinnerungen.faellig(context, jetzt)
        val bald = Erinnerungen.datiert(context, jetzt).filter { it.erledigt == 0L && (it.termin() ?: 0L) in jetzt..grenze }
        val spaeter = Erinnerungen.datiert(context, jetzt).count { it.erledigt == 0L && (it.termin() ?: 0L) > grenze }
        val allgemein = Erinnerungen.allgemeine(context).filter { it.erledigt == 0L }
        return buildString {
            faellig.forEach { append("- FÄLLIG, unbestätigt: ").append(it.erinnerung.datumAnzeige()).append(": ").append(it.erinnerung.kurz).append('\n') }
            bald.forEach { append("- ").append(it.datumAnzeige()).append(": ").append(it.kurz).append('\n') }
            if (spaeter > 0) append("- Später als 14 Tage: ").append(spaeter).append(" weitere\n")
            allgemein.forEach { append("- Allgemein, ohne Termin: ").append(it.kurz).append('\n') }
        }.trim().ifEmpty { "Keine offenen Erinnerungen." }
    }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "erinnerung_speichern",
            titel = "Erinnerung speichern",
            beschreibung = "Jarvis: Reiter „Erinnerung“ der Ablage. „Erinnere mich am 12.8.2027 daran, mein Abo zu kündigen“ → aktion anlegen mit datum (Uhrzeit nur, wenn genannt). „Erinnere mich später an …“ → aktion anlegen ohne datum: allgemeine Erinnerung. " +
                "Nicht zusätzlich als Aufgabe oder Termin anlegen. Beim Anlegen immer ausdrücklich „Benachrichtigung oder Wecker?“ fragen und erst nach Franks Antwort mit seiner Auswahl in form speichern. aendern: nur die genannten Felder. ausloesung_hinzufuegen: „erinnere mich zusätzlich zwei Tage vorher“ (minuten, hier 2880). ausloesung_entfernen mit ausloesung_id. " +
                "form: meldung oder wecker. bestaetigen: genau eine Auslösung (mit ausloesung_id oder die älteste fällige), die übrigen bleiben. erledigt: die ganze Erinnerung ist erledigt, wieder_offen macht das rückgängig. erwaehnt: du hast eine allgemeine Erinnerung gerade angesprochen. loeschen: nur auf Franks ausdrücklichen Wunsch.",
            schema = schema(
                "aktion" to text("Was geschehen soll.", listOf("anlegen", "aendern", "loeschen", "ausloesung_hinzufuegen", "ausloesung_entfernen", "bestaetigen", "erledigt", "wieder_offen", "erwaehnt")),
                "id" to text("Kennung aus erinnerungen_lesen ([id …]); sonst suche."),
                "suche" to text("Eindeutiger Wortteil aus der Erinnerung, statt id."),
                "text" to text("Der vollständige ausführliche Inhalt."),
                "kurz" to text("Kurzfassung zum Vorlesen: höchstens 35 Wörter, für sich allein gut verständlich."),
                "zeile" to text("Listenzeile: etwa sechs Wörter."),
                "datum" to text("Termin als JJJJ-MM-TT. Weglassen = allgemeine Erinnerung ohne Termin."),
                "uhrzeit" to text("Nur wenn Frank eine Uhrzeit ausdrücklich nennt, HH:MM. Beim Ändern: „keine“ entfernt sie."),
                "form" to text("meldung (Benachrichtigung) oder wecker (klingelt, bis ausgeschaltet).", listOf("meldung", "wecker")),
                "vorab_minuten" to text("Beim Anlegen, optional: Zeitpunkte als Minuten vor dem Termin, mit Komma getrennt, 0 = zum Termin; 1 Stunde = 60, 1 Tag = 1440. Vorgabe: 0."),
                "minuten" to zahl("Bei ausloesung_hinzufuegen: so viele Minuten vor dem Termin."),
                "ausloesung_uhrzeit" to text("Bei ausloesung_hinzufuegen, nur für Termine ohne Uhrzeit: um diese Zeit, HH:MM."),
                "ausloesung_id" to text("Kennung der Auslösung aus erinnerungen_lesen."),
                pflicht = listOf("aktion"),
            ),
            nurLesen = false,
        ) { a -> ausfuehren(a) },
        Werkzeug(
            name = "erinnerungen_lesen",
            titel = "Erinnerungen lesen",
            beschreibung = "Jarvis: liest den Reiter „Erinnerung“. Ohne id und suche die Liste (nächste zuerst). Mit id oder suche eine vollständig, mit Kurzfassung und jedem Zeitpunkt samt Bestätigungsstand. [id …] nicht vorlesen.",
            schema = schema(
                "id" to text("Kennung."),
                "suche" to text("Wortteil."),
                "art" to text("Nur diese Liste, Vorgabe alle.", listOf("alle", "datiert", "allgemein")),
                "mit_erledigten" to schalter("Auch erledigte und vergangene zeigen."),
            ),
            nurLesen = true,
        ) { a ->
            if (Erinnerungen.defekt(context)) return@Werkzeug Ergebnis("Die Erinnerungsdatei ist beschädigt und wurde gesichert; Details unter Aktivität.", fehler = true)
            if (a.gesetzt("id") || a.gesetzt("suche")) {
                val (e, f) = finde(a)
                return@Werkzeug if (e != null) Ergebnis(voll(e)) else Ergebnis(f.orEmpty(), fehler = true)
            }
            val art = a.optString("art", "alle")
            val jetzt = System.currentTimeMillis()
            val mit = a.optBoolean("mit_erledigten")
            val datiert = if (art == "allgemein") emptyList() else Erinnerungen.datiert(context, jetzt).filter { mit || (it.erledigt == 0L && (it.termin() ?: 0L) >= jetzt) || it.termine().any { t -> Erinnerungen.quittung(context, t.quittung) == null && t.zeit <= jetzt && Erinnerungen.meldbar(t, jetzt) } }
            val allgemein = if (art == "datiert") emptyList() else Erinnerungen.allgemeine(context).filter { mit || it.erledigt == 0L }
            if (datiert.isEmpty() && allgemein.isEmpty()) Ergebnis("Keine Erinnerungen.")
            else Ergebnis(buildString {
                if (datiert.isNotEmpty()) append("Mit Datum:\n").append(datiert.joinToString("\n") { "- " + kopf(it) }).append('\n')
                if (allgemein.isNotEmpty()) append("Allgemein:\n").append(allgemein.joinToString("\n") { "- " + it.zeile + " [id ${it.id}]" + (if (it.erledigt != 0L) " (erledigt)" else "") })
            }.trim())
        },
    )

    private fun vorbeiHinweis(e: Erinnerung) = if (Erinnerungen.meldetSichNoch(e)) "" else " ACHTUNG: Alle Zeitpunkte sind schon vorbei, von selbst meldet sie sich nicht. Sag das Frank und frag nach einer Uhrzeit."

    private fun ausfuehren(a: JSONObject): Ergebnis {
        if (Erinnerungen.defekt(context)) return Ergebnis("Die Erinnerungsdatei ist beschädigt und wurde gesichert; nichts gespeichert.", fehler = true)
        val aktion = a.optString("aktion")
        if (aktion == "anlegen") {
            val text = a.optString("text").trim()
            if (text.isEmpty()) return Ergebnis("Der Inhalt fehlt.", fehler = true)
            val datum = if (a.gesetzt("datum")) datumAus(a.optString("datum")) ?: return Ergebnis("„${a.optString("datum")}“ ist kein Datum.", fehler = true) else null
            val uhrzeit = if (datum != null && a.gesetzt("uhrzeit")) uhrzeitAus(a.optString("uhrzeit")) ?: return Ergebnis("„${a.optString("uhrzeit")}“ ist keine Uhrzeit.", fehler = true) else null
            val vorab = vorabAus(a.optString("vorab_minuten")).map { Erinnerungen.neueAusloesung(it) }
            val form = if (a.optString("form") == Erinnerung.FORM_WECKER) Erinnerung.FORM_WECKER else Erinnerung.FORM_MELDUNG
            val e = Erinnerungen.anlegen(context, text, a.optString("zeile"), a.optString("kurz"), datum?.toString(), uhrzeit, form, vorab, Einstellungen.get(context).erinnerungOhneUhrzeit)
                ?: return Ergebnis("Die Erinnerung ließ sich nicht speichern.", fehler = true)
            return Ergebnis(if (e.allgemein) "Als allgemeine Erinnerung gespeichert: ${e.zeile}. Jarvis meldet sich, wenn es passt." else "Gespeichert für ${e.datumAnzeige()}: ${e.zeile}. Form: ${if (e.wecker) "Wecker" else "Benachrichtigung"}. Zeitpunkte: " + e.ausloesungen.joinToString { e.vorlaufText(it) } + "." + vorbeiHinweis(e))
        }
        val (e, f) = finde(a)
        if (e == null) return Ergebnis(f.orEmpty(), fehler = true)
        return when (aktion) {
            "aendern" -> {
                val datum = if (a.gesetzt("datum")) datumAus(a.optString("datum")) ?: return Ergebnis("„${a.optString("datum")}“ ist kein Datum.", fehler = true) else e.tag()
                val uhrzeit = when {
                    !a.gesetzt("uhrzeit") -> e.uhrzeit
                    a.optString("uhrzeit").trim().lowercase() == "keine" -> null
                    else -> uhrzeitAus(a.optString("uhrzeit")) ?: return Ergebnis("„${a.optString("uhrzeit")}“ ist keine Uhrzeit.", fehler = true)
                }
                val form = when (a.optString("form")) { Erinnerung.FORM_WECKER -> Erinnerung.FORM_WECKER; Erinnerung.FORM_MELDUNG -> Erinnerung.FORM_MELDUNG; else -> e.form }
                val neu = Erinnerungen.aendere(context, e.id) { alt ->
                    alt.copy(text = a.optString("text").trim().ifEmpty { alt.text }, kurz = a.optString("kurz").trim().ifEmpty { alt.kurz }, zeile = a.optString("zeile").trim().ifEmpty { alt.zeile },
                        datum = datum?.toString(), uhrzeit = uhrzeit, form = form, allgemein = datum == null,
                        // Nur beim Wechsel von allgemein zu datiert: Bewusst entfernte Zeitpunkte bleiben entfernt.
                        ausloesungen = if (datum != null && alt.allgemein) listOf(Erinnerungen.neueAusloesung(0)) else alt.ausloesungen)
                } ?: return Ergebnis("Nicht gespeichert.", fehler = true)
                Ergebnis("Geändert: ${neu.datumAnzeige()}, ${neu.zeile}, Form ${if (neu.wecker) "Wecker" else "Benachrichtigung"}. Das ist keine Bestätigung." + if (neu.ausloesungen.isNotEmpty()) vorbeiHinweis(neu) else "")
            }
            "loeschen" -> if (Erinnerungen.loesche(context, e.id)) Ergebnis("Gelöscht: ${e.zeile}.") else Ergebnis("Nicht gelöscht.", fehler = true)
            "ausloesung_hinzufuegen" -> {
                if (e.allgemein) return Ergebnis("Das ist eine allgemeine Erinnerung ohne Termin; dafür gibt es keine Zeitpunkte. Gib ihr erst ein Datum (aendern).", fehler = true)
                val u = if (a.gesetzt("ausloesung_uhrzeit")) uhrzeitAus(a.optString("ausloesung_uhrzeit")) ?: return Ergebnis("Keine Uhrzeit: ${a.optString("ausloesung_uhrzeit")}", fehler = true) else null
                if (!a.has("minuten")) return Ergebnis("Es fehlt minuten: wie lange vor dem Termin.", fehler = true)
                val neu = Erinnerungen.neueAusloesung(a.optInt("minuten", 0).coerceAtLeast(0), u)
                val z = e.zeitVon(neu) ?: return Ergebnis("Der Termin ist nicht lesbar.", fehler = true)
                if (z <= System.currentTimeMillis()) return Ergebnis("Dieser Zeitpunkt (${Erinnerungen.zeitText(z)}) ist schon vorbei, nichts eingestellt.", fehler = true)
                if (Erinnerungen.doppelt(e, neu)) return Ergebnis("Dieser Zeitpunkt (${Erinnerungen.zeitText(z)}) ist schon eingestellt.")
                Erinnerungen.aendere(context, e.id) { it.copy(ausloesungen = it.ausloesungen + neu) } ?: return Ergebnis("Nicht gespeichert.", fehler = true)
                Ergebnis("Zusätzlich erinnere ich dich ${e.vorlaufText(neu)}: ${Erinnerungen.zeitText(z)}.")
            }
            "ausloesung_entfernen" -> {
                val id = a.optString("ausloesung_id")
                if (e.ausloesungen.none { it.id == id }) return Ergebnis("Diesen Zeitpunkt gibt es nicht.", fehler = true)
                Erinnerungen.aendere(context, e.id) { alt -> alt.copy(ausloesungen = alt.ausloesungen.filter { it.id != id }) } ?: return Ergebnis("Nicht gespeichert.", fehler = true)
                Ergebnis("Zeitpunkt entfernt.")
            }
            "bestaetigen" -> {
                val jetzt = System.currentTimeMillis()
                val offen = e.termine().filter { Erinnerungen.quittung(context, it.quittung) == null }
                // Ohne Angabe nur, was gerade wirklich fällig ist: Nie eine tote oder eine künftige Auslösung statt des klingelnden Weckers.
                val ziel = if (a.gesetzt("ausloesung_id")) offen.firstOrNull { it.ausloesung?.id == a.optString("ausloesung_id") } ?: return Ergebnis("Dieser Zeitpunkt ist nicht offen.", fehler = true)
                    else offen.firstOrNull { it.zeit <= jetzt && Erinnerungen.meldbar(it, jetzt) } ?: return Ergebnis("Bei dieser Erinnerung ist gerade nichts fällig. Einen künftigen Zeitpunkt bestätigst du nur mit ausloesung_id.")
                if (!ErinnerungPlaner.bestaetige(context, ziel)) return Ergebnis("Die Bestätigung ließ sich nicht speichern.", fehler = true)
                val rest = offen.count { it.zeit > jetzt }
                Ergebnis("Bestätigt: ${ziel.anzeige}. " + if (rest > 0) "$rest weitere Zeitpunkte bleiben geplant." else "Weitere Zeitpunkte sind nicht geplant.")
            }
            "erledigt" -> if (Erinnerungen.aendere(context, e.id) { it.copy(erledigt = System.currentTimeMillis()) } != null) Ergebnis("Als erledigt markiert: ${e.zeile}.") else Ergebnis("Nicht gespeichert.", fehler = true)
            "wieder_offen" -> if (Erinnerungen.aendere(context, e.id) { it.copy(erledigt = 0L) } != null) Ergebnis("Wieder offen: ${e.zeile}. Schon vergangene Zeitpunkte melden sich nicht nachträglich.") else Ergebnis("Nicht gespeichert.", fehler = true)
            "erwaehnt" -> if (Erinnerungen.aendere(context, e.id) { it.copy(zuletztErwaehnt = System.currentTimeMillis()) } != null) Ergebnis("Vermerkt.") else Ergebnis("Nicht gespeichert.", fehler = true)
            else -> Ergebnis("Unbekannte aktion „$aktion“.", fehler = true)
        }
    }
}
