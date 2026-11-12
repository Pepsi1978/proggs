package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.listen.Eintrag
import de.frank.jarvis.listen.Gruppierung
import de.frank.jarvis.listen.Liste
import de.frank.jarvis.listen.ListenArt
import de.frank.jarvis.listen.Listen
import de.frank.jarvis.listen.Prioritaet
import de.frank.jarvis.listen.Sortierung
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Listen in Jarvis (nach dem Vorbild von TickTick): Einkaufsliste, Packliste, Checklisten … mit Unterpunkten, Prioritäten,
 * Abschnitten, Sortierung. Die Werkzeuge lösen den Listennamen selbst auf: genau eine passende Liste → sofort schreiben, mehrere →
 * Rückfrage mit den Kandidaten. Jede Änderung steht sofort im Widget und gleicht sich mit dem Tablet ab.
 */
class ListenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "listen"
    override val name = "Listen"
    override val beschreibung = "Franks Listen in Jarvis (Einkaufsliste, Packliste, Checklisten …): Einträge hinzufügen, abhaken, Unterpunkte, Prioritäten, Sortierung."
    override val hinweise =
        "Geht es um eine Liste („Füge Zahnpasta meiner Einkaufsliste hinzu“, „Setz Milch auf die Einkaufsliste“, „Was steht auf der Packliste?“, „Hake Brot ab“), " +
            "nimm liste_eintrag bzw. listen_lesen, nie aufgabe_anlegen: Aufgaben mit Termin gehören in Geniale Aufgaben, Dinge auf einer Liste hierher. " +
            "Gib den Listennamen so weiter, wie Frank ihn sagt (auch „Einkaufsliste“); das Werkzeug findet die passende Liste selbst und schreibt sofort. " +
            "Nur wenn es mehrere passende Listen meldet, frag Frank kurz, welche er meint. Mehrere Dinge in einem Satz = ein Aufruf mit allen in eintraege. " +
            "Antworte danach in einem kurzen Satz, etwa: „Zahnpasta steht auf deiner Einkaufsliste.“"

    override fun stoerung(): String? = null

    private fun stand() = Listen.sicher(context)

    /** Löst den Listennamen auf. Kein Treffer und keine Liste dieses Namens: mit [anlegen] neu anlegen (Art aus dem Namen). */
    private fun liste(name: String, anlegen: Boolean): Pair<Liste?, Ergebnis?> {
        val s = stand()
        return when (val t = Listen.finde(s, name)) {
            is Listen.Treffer.Eine -> t.liste to null
            is Listen.Treffer.Mehrere -> null to Ergebnis(
                "Mehrere Listen passen zu „$name“: ${t.listen.joinToString { "„${it.name}“" }}. Frag Frank kurz, welche er meint, und rufe dann mit dem genauen Namen erneut auf.",
                fehler = true,
            )
            Listen.Treffer.Keine -> if (anlegen && name.isNotBlank()) {
                val art = Listen.artAusName(name) ?: ListenArt.SONSTIGES
                Listen.listeAnlegen(context, schoenerName(name, art), art.kennung) to null
            } else null to Ergebnis(
                if (s.listen.isEmpty()) "Es gibt noch keine Listen." else "Eine Liste „$name“ gibt es nicht. Vorhanden: ${s.listen.joinToString { "„${it.name}“" }}.",
                fehler = true,
            )
        }
    }

    /** „meine einkaufsliste“ → „Einkaufsliste“. */
    private fun schoenerName(name: String, art: ListenArt): String {
        val roh = name.trim().replace(Regex("^(meine[rnms]?|die|der|das|zur|zum|auf die|auf der)\\s+", RegexOption.IGNORE_CASE), "")
        if (art == ListenArt.EINKAUF && Listen.glatt(roh).replace(" ", "") in setOf("einkauf", "einkaufs", "einkaufen")) return "Einkaufsliste"
        return roh.replaceFirstChar { it.uppercase() }
    }

    private fun zeileText(e: Eintrag, tiefe: Int, fortschritt: String): String {
        val teile = ArrayList<String>()
        if (e.prioritaet > 0) teile += "Priorität ${Prioritaet.name(e.prioritaet)}"
        if (e.faellig.isNotEmpty()) teile += "fällig ${e.faellig}"
        if (e.tags.isNotEmpty()) teile += e.tags.joinToString(" ") { "#$it" }
        if (e.angeheftet) teile += "angeheftet"
        if (fortschritt.isNotEmpty()) teile += fortschritt
        if (e.notiz.isNotEmpty()) teile += "Notiz: ${e.notiz}"
        return "  ".repeat(tiefe) + (if (e.erledigt) "[x] " else "[ ] ") + e.titel + (if (teile.isEmpty()) "" else " (${teile.joinToString(", ")})") + " [id ${e.id}]"
    }

    private fun listeLesen(l: Liste, erledigteZeigen: Boolean): String {
        val s = stand()
        val zeilen = Listen.ansicht(s, l, erledigteZeigen)
        val kopf = "Liste „${l.name}“ (${ListenArt.von(l.art).anzeige}, ${s.offen(l.id)} offen, sortiert nach ${Sortierung.von(l.sortierung).anzeige}" +
            (if (l.gruppierung != Gruppierung.KEINE.kennung) ", gruppiert nach ${Gruppierung.von(l.gruppierung).anzeige}" else "") + ")"
        if (zeilen.isEmpty()) return "$kopf: leer."
        return kopf + ":\n" + zeilen.joinToString("\n") { z ->
            z.kopf?.let { "— $it (${z.kopfAnzahl}) —" } ?: zeileText(z.eintrag!!, z.tiefe, if (z.kinderGesamt > 0) "${z.kinderErledigt}/${z.kinderGesamt} Unterpunkte erledigt" else "")
        }
    }

    private fun eintragFinden(l: Liste, text: String, erledigteZuerst: Boolean = false): Pair<Eintrag?, Ergebnis?> {
        val t = Listen.findeEintrag(stand(), l.id, text, erledigteZuerst)
        return when {
            t.size == 1 -> t[0] to null
            t.isEmpty() -> null to Ergebnis("Auf „${l.name}“ steht nichts wie „$text“.", fehler = true)
            else -> null to Ergebnis("Auf „${l.name}“ passen mehrere Einträge zu „$text“: ${t.joinToString { "„${it.titel}“ [id ${it.id}]" }}. Frag Frank, welcher gemeint ist, oder nimm die id.", fehler = true)
        }
    }

    private fun JSONObject.liste(name: String): List<String> {
        val a = optJSONArray(name) ?: return optString(name).takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
        return (0 until a.length()).map { a.optString(it).trim() }.filter { it.isNotEmpty() }
    }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "listen_lesen",
            titel = "Listen lesen",
            beschreibung = "Jarvis: liest Franks Listen (Einkaufsliste, Packliste, Checklisten …). Ohne liste: alle Listen mit Art und Zahl offener Einträge. " +
                "Mit liste: deren Einträge so sortiert wie in der App, Unterpunkte eingerückt, [x] = erledigt. Das [id …] ist die feste Kennung, nicht vorlesen.",
            schema = schema(
                "liste" to text("Optional: Name der Liste, wie Frank ihn sagt (etwa „Einkaufsliste“)."),
                "erledigte" to schalter("Auch erledigte Einträge zeigen (Vorgabe: nein)."),
            ),
            nurLesen = true,
            taetigkeit = "schaut in die Listen",
        ) { a ->
            val s = stand()
            if (!a.gesetzt("liste")) {
                if (s.listen.isEmpty()) Ergebnis("Es gibt noch keine Listen.")
                else Ergebnis("Listen:\n" + s.listen.joinToString("\n") { "- ${it.name} (${ListenArt.von(it.art).anzeige}, ${s.offen(it.id)} offen) [id ${it.id}]" })
            } else {
                val (l, fehler) = liste(a.optString("liste"), anlegen = false)
                fehler ?: Ergebnis(listeLesen(l!!, a.optBoolean("erledigte", false)))
            }
        },
        Werkzeug(
            name = "liste_eintrag",
            titel = "Listeneintrag",
            beschreibung = "Jarvis: ändert Einträge einer Liste. „Füge Zahnpasta meiner Einkaufsliste hinzu“ → aktion hinzufuegen, liste „Einkaufsliste“, eintraege [„Zahnpasta“] " +
                "(fehlt die Liste ganz, legt Jarvis sie an). „Setz Milch und Brot auf die Einkaufsliste“ → eintraege [„Milch“, „Brot“]. " +
                "Unterpunkt: „Füge bei Grillen Kohle hinzu“ → hinzufuegen mit unter „Grillen“. „Hake Brot ab“ → abhaken (eintrag „Brot“). " +
                "„Brot doch nicht erledigt“ → wieder_oeffnen. „Mach Milch wichtig“ → aendern mit prioritaet hoch. „Streich Brot von der Liste“ → loeschen. " +
                "„Schieb Kohle auf die Packliste“ → verschieben mit ziel_liste. „Heft Milch oben an“ → aendern mit angeheftet an. Nur Felder mitschicken, die sich ändern sollen; „ohne“ leert Notiz, Tags, Fälligkeit oder Abschnitt. " +
                "Ist liste nicht angegeben und gibt es nur eine Liste, gilt diese. Meldet das Werkzeug mehrere passende Listen oder Einträge, frag Frank kurz nach.",
            schema = schema(
                "aktion" to text("hinzufuegen, abhaken, wieder_oeffnen, aendern, loeschen oder verschieben.", listOf("hinzufuegen", "abhaken", "wieder_oeffnen", "aendern", "loeschen", "verschieben")),
                "liste" to text("Name der Liste, wie Frank ihn sagt (etwa „Einkaufsliste“, „Packliste“)."),
                "eintraege" to textListe("Beim Hinzufügen: die neuen Einträge, je Ding einer, kurz und richtig geschrieben („Zahnpasta“). Bei abhaken/loeschen auch mehrere vorhandene."),
                "eintrag" to text("Bei abhaken, wieder_oeffnen, aendern, loeschen, verschieben: der vorhandene Eintrag (Titel oder id)."),
                "unter" to text("Optional beim Hinzufügen und Ändern: Titel oder id des Oberpunkts, unter den die Einträge als Unterpunkte kommen; „oben“ = oberste Ebene."),
                "prioritaet" to text("Optional: keine, niedrig, mittel oder hoch.", listOf("keine", "niedrig", "mittel", "hoch")),
                "notiz" to text("Optional: Notiz zum Eintrag; „ohne“ löscht sie."),
                "faellig" to text("Optional: Fälligkeit JJJJ-MM-TT, heute, morgen oder ohne."),
                "tags" to textListe("Optional: Tags ohne #; [„ohne“] löscht alle."),
                "abschnitt" to text("Optional: Abschnitt innerhalb der Liste (etwa „Drogerie“); „ohne“ entfernt ihn."),
                "angeheftet" to text("Nur wenn Frank es verlangt: an = oben anheften, aus = lösen.", listOf("an", "aus")),
                "neuer_titel" to text("Beim Ändern: neuer Titel."),
                "ziel_liste" to text("Beim Verschieben: Name der Zielliste."),
            ),
            nurLesen = false,
            taetigkeit = "schreibt in eine Liste",
        ) { a -> eintragAusfuehren(a) },
        Werkzeug(
            name = "liste_verwalten",
            titel = "Listen verwalten",
            beschreibung = "Jarvis: legt Listen an und stellt sie ein. „Leg eine Packliste für Schweden an“ → anlegen (name, art packliste). „Benenn die Packliste in Schweden um“ → umbenennen. " +
                "„Lösch die Packliste“ → loeschen (nur auf ausdrücklichen Wunsch). „Sortier die Einkaufsliste nach Priorität“ → sortierung. „Gruppier nach Abschnitten“ → gruppierung. " +
                "„Blende Erledigte aus“ → erledigte_zeigen wert false. „Räum die Erledigten von der Einkaufsliste weg“ → erledigte_loeschen. „Mach daraus eine Einkaufsliste“ → art.",
            schema = schema(
                "aktion" to text("anlegen, umbenennen, loeschen, sortierung, gruppierung, erledigte_zeigen, erledigte_loeschen, art oder abschnitte.",
                    listOf("anlegen", "umbenennen", "loeschen", "sortierung", "gruppierung", "erledigte_zeigen", "erledigte_loeschen", "art", "abschnitte")),
                "name" to text("Name der Liste (beim Anlegen der neue Name)."),
                "neuer_name" to text("Beim Umbenennen: der neue Name."),
                "art" to text("Beim Anlegen oder bei aktion art.", ListenArt.entries.map { it.kennung }),
                "sortierung" to text("Bei aktion sortierung.", Sortierung.entries.map { it.kennung }),
                "gruppierung" to text("Bei aktion gruppierung.", Gruppierung.entries.map { it.kennung }),
                "wert" to schalter("Bei erledigte_zeigen: true = zeigen, false = ausblenden."),
                "abschnitte" to textListe("Bei aktion abschnitte: alle Abschnitte der Liste in der gewünschten Reihenfolge."),
            ),
            nurLesen = false,
            loeschend = true,
            taetigkeit = "richtet eine Liste ein",
        ) { a -> verwalten(a) },
    )

    private fun prioAus(a: JSONObject): Int? = if (a.gesetzt("prioritaet")) Prioritaet.von(a.optString("prioritaet")) else null
    private fun faelligAus(a: JSONObject): String? = if (a.gesetzt("faellig")) Ergebnispruefung.datum(a.optString("faellig")) else null
    /** „ohne“ in beliebiger Schreibweise (auch „#ohne“, „keine“): ausdrücklich leeren. */
    private fun ohne(text: String) = text.trim().removePrefix("#").lowercase() in setOf("ohne", "keine", "keiner", "leer")

    private fun eintragAusfuehren(a: JSONObject): Ergebnis {
        val aktion = a.optString("aktion")
        val s = stand()
        // Ohne Listenangabe: die einzige Liste, sonst nachfragen.
        val name = a.optString("liste").takeIf { a.gesetzt("liste") } ?: when (s.listen.size) {
            0 -> if (aktion == "hinzufuegen") "Einkaufsliste" else return Ergebnis("Es gibt noch keine Listen.", fehler = true)
            1 -> s.listen[0].name
            else -> return Ergebnis("Welche Liste ist gemeint? Vorhanden: ${s.listen.joinToString { "„${it.name}“" }}. Frag Frank kurz.", fehler = true)
        }
        // Ein Datum, das sich nicht lesen lässt, nie still weglassen: Frank verließe sich sonst darauf.
        if (a.gesetzt("faellig") && faelligAus(a) == null) return Ergebnis("Das Datum „${a.optString("faellig")}“ verstehe ich nicht; bitte als JJJJ-MM-TT, heute, morgen oder ohne angeben.", fehler = true)
        val (l, fehler) = liste(name, anlegen = aktion == "hinzufuegen")
        if (fehler != null) return fehler
        l!!
        val neuAngelegt = stand().eintraegeVon(l.id).isEmpty() && s.listen.none { it.id == l.id }
        fun oberpunkt(): Pair<Long, Ergebnis?> {
            if (!a.gesetzt("unter")) return 0L to null
            val u = a.optString("unter")
            if (Listen.glatt(u) in setOf("oben", "oberste ebene", "keiner", "ohne")) return 0L to null
            val (e, f) = eintragFinden(l, u)
            return (e?.id ?: 0L) to f
        }
        return when (aktion) {
            "hinzufuegen" -> {
                val titel = a.liste("eintraege").ifEmpty { a.liste("eintrag") }.map { DiktatTrenner.ohneSchluss(it).trim() }.filter { it.isNotEmpty() }
                if (titel.isEmpty()) return Ergebnis("Nichts zum Hinzufügen angegeben.", fehler = true)
                val (vater, f) = oberpunkt()
                if (f != null) return f
                val neu = titel.mapNotNull { t ->
                    Listen.hinzufuegen(context, l.id, t, vater, prioAus(a) ?: 0, a.optString("notiz").takeIf { a.gesetzt("notiz") }.orEmpty(),
                        faelligAus(a).orEmpty(), a.liste("tags"), a.optString("abschnitt").trim().takeIf { a.gesetzt("abschnitt") && !ohne(it) }.orEmpty())
                }
                val wer = neu.joinToString(", ") { it.titel }
                val unter = if (vater != 0L) " als Unterpunkt von „${stand().eintraege.firstOrNull { it.id == vater }?.titel}“" else ""
                Ergebnis((if (neuAngelegt) "Liste „${l.name}“ neu angelegt. " else "") + "$wer${unter} auf „${l.name}“ eingetragen; steht sofort im Widget." +
                    " Jetzt ${stand().offen(l.id)} offen.")
            }
            "abhaken", "wieder_oeffnen", "loeschen" -> {
                val namen = a.liste("eintrag").ifEmpty { a.liste("eintraege") }
                if (namen.isEmpty()) return Ergebnis("Welcher Eintrag ist gemeint?", fehler = true)
                val erledigt = ArrayList<String>()
                for (n in namen) {
                    val (e, f) = eintragFinden(l, n, erledigteZuerst = aktion == "wieder_oeffnen")
                    if (f != null) return if (erledigt.isEmpty()) f else Ergebnis("Erledigt: ${erledigt.joinToString()}. Dann: ${f.text}", fehler = true)
                    when (aktion) {
                        "abhaken" -> Listen.abhaken(context, e!!.id, true)
                        "wieder_oeffnen" -> Listen.abhaken(context, e!!.id, false)
                        else -> Listen.loeschen(context, e!!.id)
                    }
                    erledigt += e.titel
                }
                val wort = when (aktion) { "abhaken" -> "abgehakt"; "wieder_oeffnen" -> "wieder offen"; else -> "gelöscht" }
                Ergebnis("${erledigt.joinToString()} auf „${l.name}“ $wort. Noch ${stand().offen(l.id)} offen.")
            }
            "aendern" -> {
                val (e, f) = eintragFinden(l, a.optString("eintrag"))
                if (f != null) return f
                e!!
                if (a.gesetzt("unter")) {
                    val (vater, f2) = oberpunkt()
                    if (f2 != null) return f2
                    Listen.einhaengen(context, e.id, vater) ?: return Ergebnis("„${e.titel}“ lässt sich nicht unter sich selbst hängen.", fehler = true)
                }
                val n = Listen.aendern(context, e.id) { x ->
                    x.copy(
                        titel = a.optString("neuer_titel").takeIf { a.gesetzt("neuer_titel") }?.trim() ?: x.titel,
                        prioritaet = prioAus(a) ?: x.prioritaet,
                        // Leere Felder, die das Modell nur mitschickt, ändern nichts: „ohne“ löscht ausdrücklich.
                        notiz = if (a.gesetzt("notiz")) a.optString("notiz").trim().takeIf { !ohne(it) }.orEmpty() else x.notiz,
                        faellig = faelligAus(a) ?: x.faellig,
                        tags = a.liste("tags").takeIf { it.isNotEmpty() }?.let { t -> if (t.all(::ohne)) emptyList() else t.map { it.removePrefix("#") } } ?: x.tags,
                        abschnitt = if (a.gesetzt("abschnitt") && x.eltern == 0L) a.optString("abschnitt").trim().takeIf { !ohne(it) }.orEmpty() else x.abschnitt,
                        angeheftet = when (a.optString("angeheftet").trim().lowercase()) { "an", "true", "ja" -> true; "aus", "false", "nein" -> false; else -> x.angeheftet },
                    )
                }
                if (a.gesetzt("abschnitt") && !ohne(a.optString("abschnitt"))) Listen.listeAendern(context, l.id) { x ->
                    if (a.optString("abschnitt").trim() in x.abschnitte) x else x.copy(abschnitte = x.abschnitte + a.optString("abschnitt").trim())
                }
                Ergebnis("„${n?.titel}“ auf „${l.name}“ geändert (Priorität ${Prioritaet.name(n?.prioritaet ?: 0)}${n?.faellig?.takeIf { it.isNotEmpty() }?.let { ", fällig $it" }.orEmpty()}).")
            }
            "verschieben" -> {
                val (e, f) = eintragFinden(l, a.optString("eintrag"))
                if (f != null) return f
                if (!a.gesetzt("ziel_liste")) return Ergebnis("In welche Liste soll „${e!!.titel}“?", fehler = true)
                val (z, f2) = liste(a.optString("ziel_liste"), anlegen = false)
                if (f2 != null) return f2
                if (z!!.id == l.id) return Ergebnis("„${e!!.titel}“ steht schon auf „${l.name}“.", fehler = true)
                Listen.verschieben(context, e!!.id, z.id) ?: return Ergebnis("„${e.titel}“ ließ sich nicht verschieben.", fehler = true)
                Ergebnis("„${e.titel}“ von „${l.name}“ nach „${z.name}“ verschoben.")
            }
            else -> Ergebnis("Unbekannte Aktion „$aktion“.", fehler = true)
        }
    }

    private fun verwalten(a: JSONObject): Ergebnis {
        val aktion = a.optString("aktion")
        if (aktion == "anlegen") {
            val name = a.optString("name").trim()
            if (name.isEmpty()) return Ergebnis("Wie soll die Liste heißen?", fehler = true)
            stand().listen.firstOrNull { Listen.glatt(it.name) == Listen.glatt(name) }?.let { return Ergebnis("Die Liste „${it.name}“ gibt es schon.") }
            val art = if (a.gesetzt("art")) ListenArt.von(a.optString("art")) else Listen.artAusName(name) ?: ListenArt.SONSTIGES
            val l = Listen.listeAnlegen(context, name.replaceFirstChar { it.uppercase() }, art.kennung)
            return Ergebnis("Liste „${l.name}“ (${art.anzeige}) angelegt; im Widget wählbar.")
        }
        // Löschen nur mit ausdrücklichem Namen: Ein leerer Name träfe sonst die einzige Liste.
        if (aktion == "loeschen" && Listen.glatt(a.optString("name")).isEmpty()) return Ergebnis("Welche Liste soll gelöscht werden? Bitte den Namen nennen.", fehler = true)
        val (l, fehler) = liste(a.optString("name"), anlegen = false)
        if (fehler != null) return fehler
        l!!
        return when (aktion) {
            "umbenennen" -> {
                val neu = a.optString("neuer_name").trim()
                if (neu.isEmpty()) Ergebnis("Wie soll die Liste heißen?", fehler = true)
                else { Listen.listeAendern(context, l.id) { it.copy(name = neu) }; Ergebnis("„${l.name}“ heißt jetzt „$neu“.") }
            }
            "loeschen" -> { Listen.listeLoeschen(context, l.id); Ergebnis("Liste „${l.name}“ mit allen Einträgen gelöscht.") }
            "sortierung" -> {
                val s = Sortierung.von(a.optString("sortierung"))
                Listen.listeAendern(context, l.id) { it.copy(sortierung = s.kennung) }
                Ergebnis("„${l.name}“ ist jetzt nach ${s.anzeige} sortiert.")
            }
            "gruppierung" -> {
                val g = Gruppierung.von(a.optString("gruppierung"))
                Listen.listeAendern(context, l.id) { it.copy(gruppierung = g.kennung) }
                Ergebnis(if (g == Gruppierung.KEINE) "„${l.name}“ ist nicht mehr gruppiert." else "„${l.name}“ ist jetzt nach ${g.anzeige} gruppiert.")
            }
            "erledigte_zeigen" -> {
                val an = a.optBoolean("wert", true)
                Listen.listeAendern(context, l.id) { it.copy(erledigteZeigen = an) }
                Ergebnis(if (an) "Erledigte Einträge auf „${l.name}“ werden angezeigt." else "Erledigte Einträge auf „${l.name}“ sind ausgeblendet.")
            }
            "erledigte_loeschen" -> Ergebnis("${Listen.erledigteLoeschen(context, l.id)} erledigte Einträge von „${l.name}“ gelöscht.")
            "art" -> {
                val art = ListenArt.von(a.optString("art"))
                Listen.listeAendern(context, l.id) { it.copy(art = art.kennung) }
                Ergebnis("„${l.name}“ ist jetzt eine ${art.anzeige}-Liste.")
            }
            "abschnitte" -> {
                // Ohne Angabe nur lesen: Ein fehlendes Feld darf nie alle Abschnitte entfernen; leeren mit [„ohne“].
                if (!a.has("abschnitte")) return Ergebnis(if (l.abschnitte.isEmpty()) "„${l.name}“ hat keine Abschnitte." else "Abschnitte von „${l.name}“: ${l.abschnitte.joinToString()}.")
                val neu = a.liste("abschnitte").filterNot(::ohne).map { it.trim() }.distinct()
                Listen.listeAendern(context, l.id) { it.copy(abschnitte = neu) }
                Ergebnis(if (neu.isEmpty()) "Abschnitte von „${l.name}“ entfernt." else "Abschnitte von „${l.name}“: ${neu.joinToString()}.")
            }
            else -> Ergebnis("Unbekannte Aktion „$aktion“.", fehler = true)
        }
    }

    /**
     * Für das Live-Gedächtnis: je Liste die offenen Einträge (Unterpunkte eingerückt, Priorität, Fälligkeit), knapp und ohne ids.
     * Jede Zeile eines Eintrags beginnt mit „- “, damit neue Einträge im Abschnitt NEU erscheinen.
     */
    fun gedaechtnis(): String {
        val s = stand()
        if (s.listen.isEmpty()) return "Noch keine Listen angelegt."
        return s.listen.joinToString("\n") { l ->
            val offen = Listen.ansicht(s, l, erledigteZeigen = false, gruppen = false).filter { it.eintrag?.erledigt == false }
            val kopf = "${l.name} (${ListenArt.von(l.art).anzeige}): " + if (offen.isEmpty()) "nichts offen" else "${offen.size} offen"
            kopf + offen.take(40).joinToString("") { z ->
                val e = z.eintrag!!
                val dazu = listOfNotNull(
                    e.prioritaet.takeIf { it > 0 }?.let { "Priorität ${Prioritaet.name(it)}" }, e.faellig.takeIf { it.isNotEmpty() }?.let { "fällig $it" },
                ).joinToString(", ")
                "\n" + "  ".repeat(z.tiefe) + "- " + e.titel + (if (dazu.isEmpty()) "" else " ($dazu)")
            } + (if (offen.size > 40) "\n  … und ${offen.size - 40} weitere (listen_lesen)" else "")
        }
    }
}
