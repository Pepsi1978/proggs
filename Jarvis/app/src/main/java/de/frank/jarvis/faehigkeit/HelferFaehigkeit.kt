package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Base64
import de.frank.jarvis.ablage.AblageZentrale
import de.frank.jarvis.agent.JarvisAgent
import de.frank.jarvis.helfer.BauStand
import de.frank.jarvis.helfer.Helfer
import de.frank.jarvis.helfer.HelferAblauf
import de.frank.jarvis.helfer.HelferKatalog
import de.frank.jarvis.helfer.HelferLauf
import de.frank.jarvis.helfer.HelferZustand
import de.frank.jarvis.helfer.KontenRegister
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.util.UUID
import kotlinx.coroutines.currentCoroutineContext
import org.json.JSONArray
import org.json.JSONObject

/** Die eigene Werkzeuglandschaft als Werkzeuge für Jarvis, Plugin und den Mac. Oberfläche: Einstellungen → Eigene Tools. */
class HelferFaehigkeit(context: Context) : Faehigkeit {
    private val app = context.applicationContext

    init { HelferAblauf.start(app) }

    override val id = "helfer"
    override val name = "Eigene Tools"
    override val beschreibung = "Der eigene Katalog von Helfern auf Handy und Mac: suchen, benutzen, bauen lassen, verbessern."
    override val hinweise =
        "EIGENE TOOLS: Fehlt dir für eine Aufgabe eine Fähigkeit, bleib nicht bei „das kann ich nicht“ stehen. Prüfe zuerst deine normalen Werkzeuge, dann helfer_suchen. Reicht etwas davon, nimm es (helfer_ausfuehren). " +
            "Nur wenn nichts passt und die Aufgabe es wirklich braucht, helfer_bauen mit der kleinsten tragfähigen Ergänzung; vorhandenen Helfer eher verbessern als einen zweiten bauen. Frank muss das nicht erst beauftragen, " +
            "sag kurz und natürlich „Dafür baue ich dir einen Helfer, etwa … Minuten“. Für normale Fragen nach Aktuellem bleibt die direkte Websuche; hier gilt die Recherchebefugnis nur innerhalb eines Werkzeugbaus. " +
            "Unmögliches, fehlende Zugänge und nötige persönliche Freigaben sagst du ehrlich samt kleinstem nächsten Schritt. Zustände nie vermischen: Ein Mac-Helfer kann fertig sein und der Mac trotzdem aus."

    override fun stoerung(): String? = null

    private fun kurz(h: Helfer, macDa: Boolean?): String = buildString {
        append("- ").append(h.kennung).append(" (").append(if (h.ort == "mac") "Mac" else "Handy").append(", ").append(HelferZustand.text(h.zustand, h.aktiv))
        if (h.ort == "mac" && macDa == false && h.einsatzbereit) append(", Mac gerade nicht erreichbar")
        append("): ").append(h.zweck)
    }

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = "helfer_suchen",
                titel = "Eigene Helfer suchen",
                beschreibung = "Jarvis: durchsucht den Katalog der eigenen Helfer auf Handy und Mac nach einer Fähigkeit, ohne Suchwort kommt alles. Vor jedem helfer_bauen. Dazu Stand der laufenden Bauten.",
                schema = schema("suche" to text("Worum es geht, zum Beispiel „Word Datei“.")),
                nurLesen = true,
            ) { a ->
                val beginn = System.currentTimeMillis()
                val treffer = HelferKatalog.suche(app, a.optString("suche"))
                val macDa = if (treffer.any { it.ort == "mac" }) runCatching { mac().macErreichbar() }.getOrNull() else null
                val laufend = HelferAblauf.auftraege(app).filter { it.optString("stand") in BauStand.OFFEN }
                Logbuch.dauer("Helfer: Suche", System.currentTimeMillis() - beginn, "${treffer.size} Treffer")
                Ergebnis((if (treffer.isEmpty()) "Kein passender Helfer." else treffer.joinToString("\n") { kurz(it, macDa) }) +
                    (if (laufend.isEmpty()) "" else "\nIm Bau: " + laufend.joinToString("; ") { "${it.optString("kennung")} (${BauStand.text(it.optString("stand"))})" }))
            },
            Werkzeug(
                name = "helfer_info",
                titel = "Einen Helfer genau ansehen",
                beschreibung = "Jarvis: alles zu einem Helfer: Zweck, Fähigkeiten, Eingaben, Ergebnis, Ort, Voraussetzungen, Rechte, Grenzen, Zustand, Version, letzte Prüfung. Für Franks Fragen zu einem Helfer und vor dem ersten Aufruf.",
                schema = schema("kennung" to text("Kennung aus helfer_suchen."), pflicht = listOf("kennung")),
                nurLesen = true,
            ) { a ->
                val h = HelferKatalog.finde(app, a.optString("kennung")) ?: return@Werkzeug Ergebnis("Diesen Helfer gibt es nicht.", fehler = true)
                val macDa = if (h.ort == "mac") runCatching { mac().macErreichbar() }.getOrDefault(false) else null
                Ergebnis(info(h, macDa))
            },
            Werkzeug(
                name = "helfer_ausfuehren",
                titel = "Einen Helfer benutzen",
                beschreibung = "Jarvis: führt einen einsatzbereiten Helfer aus, auf dem Handy direkt, auf dem Mac über Jarvis Mac OS. Das Ergebnis ist belegt; erst damit darfst du Erfolg melden. Erzeugte Dateien erscheinen im Gespräch. " +
                    "in_ablage=true speichert sie zusätzlich in der Ablage, nur wenn Frank das verlangt hat.",
                schema = schema(
                    "kennung" to text("Kennung des Helfers."),
                    "eingabe" to JSONObject().put("type", "object").put("description", "Eingabe laut helfer_info.").put("additionalProperties", true),
                    "in_ablage" to schalter("Nur auf Franks Wunsch: Dateien dauerhaft in die Ablage."),
                    pflicht = listOf("kennung"),
                ),
                nurLesen = false,
            ) { a -> ausfuehren(a) },
            Werkzeug(
                name = "helfer_bauen",
                titel = "Fehlenden Helfer bauen lassen",
                beschreibung = "Jarvis: beauftragt im von Frank freigegebenen Rahmen den Programmierer (Claude Opus 5.5 über die vorhandenen Wege), einen fehlenden Helfer zu bauen oder einen vorhandenen zu verbessern, und erledigt danach die ursprüngliche Aufgabe selbst. " +
                    "Kehrt sofort zurück und nennt die geschätzte Dauer; sag sie Frank als Schätzung. Doppelte Bauten verhindert Jarvis. Nur bei wirklich fehlender Fähigkeit, nach helfer_suchen. " +
                    "Recherche nach Bedarf: keine bei vorhandenem Wissen, programmierer für große Fragen (Vorgabe, wenn ein Programmierer da ist), handy oder mac nur für Vorwissen oder wenn der Programmierer nur über die Cloud kommt. " +
                    "auswahl nur bei wirklich verschiedenen Wegen, die Frank betreffen; erste Option ist deine Empfehlung, Vor- und Nachteile knapp in description. Ohne Antwort gilt nach 150 Sekunden die Empfehlung.",
                schema = schema(
                    "aufgabe" to text("Franks ursprüngliche Aufgabe, vollständig: Was am Ende für ihn erledigt sein muss."),
                    "faehigkeit" to text("Die fehlende Fähigkeit: benötigter Funktionsumfang, eingegrenzt auf diese Aufgabe."),
                    "erfolgskriterien" to text("Woran überprüfbar ist, dass der Helfer funktioniert."),
                    "kennung" to text("Kennung, bei Verbesserung die vorhandene. Sonst weglassen."),
                    "name" to text("Kurzer verständlicher Name."),
                    "ort" to text("handy (Vorgabe, möglichst selbstständig nutzbar) oder mac (wenn seine Programme oder Rechenkraft nötig sind).", listOf("handy", "mac")),
                    "umfang" to text("klein, mittel oder gross: für die Zeitschätzung.", listOf("klein", "mittel", "gross")),
                    "recherche" to text("keine, programmierer, handy oder mac.", listOf("keine", "programmierer", "handy", "mac")),
                    "recherche_fragen" to textListe("Bei handy oder mac: getrennte, für sich verständliche Fragen."),
                    "auswahl" to JSONObject().put("type", "object").put("description", "Optional: {frage, optionen:[{label, description}]}.").put("additionalProperties", true),
                    pflicht = listOf("aufgabe", "faehigkeit"),
                ),
                nurLesen = false,
            ) { a -> HelferAblauf.bauen(app, a) },
            Werkzeug(
                name = "helfer_registrieren",
                titel = "Fertigen Helfer registrieren",
                beschreibung = "Nur für die Programmier-Sitzung eines Werkzeugbaus: übergibt den fertigen Helfer. Jarvis führt die Tests selbst aus; nur bei Erfolg wird er einsatzbereit. Nicht im Gespräch mit Frank benutzen.",
                schema = schema(
                    "auftrag_id" to text("Nummer des Werkzeugbaus."),
                    "bau_schluessel" to text("Der Bauschlüssel aus dem Auftrag."),
                    "manifest" to text("Inhalt von helfer.json als Text."),
                    "dateien" to JSONObject().put("type", "object").put("description", "Dateiname → Inhalt als Text.").put("additionalProperties", true),
                    "review" to JSONObject().put("type", "object").put("description", "{befunde, kritisch_offen, zusammenfassung}").put("additionalProperties", true),
                    pflicht = listOf("auftrag_id", "bau_schluessel", "manifest", "dateien", "review"),
                ),
                nurLesen = false,
            ) { a -> HelferAblauf.registrieren(app, a) },
            Werkzeug(
                name = "helfer_verwalten",
                titel = "Helfer verwalten",
                beschreibung = "Jarvis: aktivieren, deaktivieren, in den Papierkorb legen, aus dem Papierkorb holen oder auf die letzte geprüfte Fassung zurücksetzen. Nur auf Franks Wunsch. Rechte freigeben und endgültig löschen geht bewusst nur in den Einstellungen.",
                schema = schema(
                    "kennung" to text("Kennung des Helfers."),
                    "aktion" to text("aktivieren, deaktivieren, papierkorb, wiederherstellen oder rueckrollen.", listOf("aktivieren", "deaktivieren", "papierkorb", "wiederherstellen", "rueckrollen")),
                    pflicht = listOf("kennung", "aktion"),
                ),
                nurLesen = false,
            ) { a -> verwalten(a.optString("kennung"), a.optString("aktion")) },
            Werkzeug(
                name = "helfer_auftraege",
                titel = "Laufende Werkzeugbauten",
                beschreibung = "Jarvis: Stand aller Werkzeugbauten: Fähigkeit, Stand, Schätzung, tatsächliche Zeit, automatisch getroffene Entscheidungen, Ergebnis. aktion=abbrechen bricht einen ab, nur auf Franks Wunsch. Dazu die Grenzen der Automatik.",
                schema = schema("aktion" to text("lesen (Vorgabe) oder abbrechen.", listOf("lesen", "abbrechen")), "auftrag_id" to text("Bei abbrechen: die Nummer.")),
                nurLesen = false,
            ) { a ->
                if (a.optString("aktion") == "abbrechen") Ergebnis(HelferAblauf.abbrechen(app, a.optString("auftrag_id").trim()))
                else Ergebnis(uebersicht())
            },
            Werkzeug(
                name = "konten_lesen",
                titel = "Konten und APIs ansehen",
                beschreibung = "Jarvis: nennt die Einträge unter Konten und APIs mit Stand und nutzenden Helfern, nie einen Schlüssel. Neue Schlüssel trägt Frank selbst in den Einstellungen ein; lege nie selbst Konten bei fremden Diensten an.",
                schema = schema(),
                nurLesen = true,
            ) { Ergebnis(KontenRegister.uebersicht(app)) },
        )
    }

    private fun mac() = Register.alle(app).filterIsInstance<MacJarvisFaehigkeit>().first()

    private fun info(h: Helfer, macDa: Boolean?): String = buildString {
        appendLine("${h.name} (${h.kennung}), Version ${h.version}, ${if (h.ort == "mac") "läuft am Mac" else "läuft auf dem Handy"}")
        appendLine("Zustand: ${HelferZustand.text(h.zustand, h.aktiv)}" + if (macDa == false) "; der Mac ist gerade nicht erreichbar" else "")
        appendLine("Zweck: ${h.zweck}")
        if (h.faehigkeiten.isNotEmpty()) appendLine("Fähigkeiten: ${h.faehigkeiten.joinToString("; ")}")
        appendLine("Aufruf: helfer_ausfuehren mit kennung=${h.kennung}. Eingabe: ${h.eingabe}")
        appendLine("Ergebnis: ${h.ausgabe}")
        if (h.voraussetzungen.isNotEmpty()) appendLine("Voraussetzungen: ${h.voraussetzungen.joinToString("; ")}")
        appendLine("Rechte: ${h.rechte.alsText()}")
        if (h.grenzen.isNotEmpty()) appendLine("Grenzen: ${h.grenzen.joinToString("; ")}")
        if (h.brauchtAppUpdate) appendLine("Braucht ein App-Update.")
        append("Letzte Prüfung: ${h.pruefung.optString("ergebnis", "keine")}")
    }

    private suspend fun ausfuehren(a: JSONObject): Ergebnis {
        val kennung = a.optString("kennung").trim()
        val h = HelferKatalog.finde(app, kennung) ?: return Ergebnis("Diesen Helfer gibt es nicht.", fehler = true)
        when {
            h.zustand == HelferZustand.WARTET_AUF_FREIGABE -> return Ergebnis("„${h.name}“ wartet auf Franks Freigabe (Einstellungen → Eigene Tools): ${h.rechte.alsText()}. Das kann nur er.", fehler = true)
            !h.einsatzbereit -> return Ergebnis("„${h.name}“ ist nicht einsatzbereit: ${HelferZustand.text(h.zustand, h.aktiv)}.", fehler = true)
        }
        val eingabe = a.optJSONObject("eingabe") ?: JSONObject()
        val beginn = System.currentTimeMillis()
        val r = if (h.ort == "mac") amMac(h, eingabe) else when (h.art) {
            "agent" -> alsAgent(h, eingabe)
            else -> HelferLauf.ausfuehren(app, h, eingabe)
        }
        Logbuch.dauer("Helfer ${h.kennung}: ${if (h.ort == "mac") "Lauf am Mac samt Kanal" else "Lauf"}", System.currentTimeMillis() - beginn, "ok=${r.ok}, eigentliche Ausführung ${r.dauerMs} ms")
        if (!r.ok) return Ergebnis("„${h.name}“ ist gescheitert: ${r.fehler}", fehler = true)
        currentCoroutineContext()[HelferAblauf.ImLauf]?.let { it.nachweise++ }
        val (ausChatGpt, gespraech) = Register.alle(app).filterIsInstance<ProgrammierFaehigkeit>().first().herkunftFuerHelfer()
        val belegt = r.dateien.filter { File(it.second).let { d -> d.exists() && d.length() > 0 } }
        if (belegt.size != r.dateien.size) return Ergebnis("„${h.name}“ meldet Dateien, die nicht vorhanden oder leer sind: nicht erledigt.", fehler = true)
        if (belegt.isNotEmpty() && gespraech.isNotEmpty() && !ausChatGpt)
            ProgrammierChat.poste(app, gespraech, "📎 ${h.name}: " + belegt.joinToString(", ") { it.first }, "antwort", anhaenge = belegt.map { it.second })
        val abgelegt = if (a.optBoolean("in_ablage") && belegt.isNotEmpty()) {
            val titel = eingabe.optString("titel").ifEmpty { h.name }
            belegt.forEach { (name, pfad) -> AblageZentrale.speicher(app).uebernimmBytes(titel, File(pfad).readBytes(), name, herkunft = "Helfer ${h.name}") }
            " In der Ablage unter „$titel“."
        } else ""
        return Ergebnis(r.text.ifEmpty { "Fertig." } + (if (r.daten.length() > 0) "\nDaten: ${r.daten.toString().take(20_000)}" else "") +
            (if (belegt.isEmpty()) "" else "\nDateien erzeugt und geprüft: " + belegt.joinToString(", ") { "${it.first} (${File(it.second).length()} Byte)" }) + abgelegt +
            "\n(Dauer ${(System.currentTimeMillis() - beginn)} ms, ${if (h.ort == "mac") "am Mac" else "auf dem Handy"})")
    }

    private suspend fun alsAgent(h: Helfer, eingabe: JSONObject): de.frank.jarvis.helfer.HelferErgebnis {
        val beginn = System.currentTimeMillis()
        val agent = JarvisAgent(app)
        var grund: String? = null
        val t = runCatching { agent.versuche(eingabe.toString(), zeitlimitMs = 300_000L, rolle = h.rolle, mitInternet = h.rechte.netz.isNotEmpty(), abschluss = true, beiAbbruch = { grund = it }) }.getOrNull()
        return if (t != null) de.frank.jarvis.helfer.HelferErgebnis(true, KontenRegister.schwaerzen(app, t), dauerMs = System.currentTimeMillis() - beginn)
        else de.frank.jarvis.helfer.HelferErgebnis(false, "", fehler = grund ?: "Keine Antwort", dauerMs = System.currentTimeMillis() - beginn)
    }

    private suspend fun amMac(h: Helfer, eingabe: JSONObject): de.frank.jarvis.helfer.HelferErgebnis {
        if (!mac().macErreichbar()) return de.frank.jarvis.helfer.HelferErgebnis(false, "", fehler = "Der Mac ist gerade nicht erreichbar. Nichts wurde ausgeführt." +
            (HelferKatalog.suche(app, h.schlagworte.joinToString(" ")).firstOrNull { it.ort == "handy" && it.einsatzbereit }?.let { " Auf dem Handy gäbe es ${it.kennung}." } ?: ""))
        val antwort = mac().helferAnfrage("helfer_aufruf", JSONObject().put("helfer", h.kennung).put("eingabe", eingabe), warteMs = 65_000L)
            ?: return de.frank.jarvis.helfer.HelferErgebnis(false, "", fehler = "Der Mac arbeitet noch oder hat die Antwort nicht rechtzeitig geliefert. Starte es NICHT neu; das Ergebnis kommt als Nachricht, sobald es da ist.")
        val j = runCatching { JSONObject(antwort) }.getOrDefault(JSONObject())
        if (!j.optBoolean("ok")) return de.frank.jarvis.helfer.HelferErgebnis(false, "", fehler = j.optString("fehler").ifEmpty { "Der Mac meldet einen Fehler." })
        val ausgabe = HelferKatalog.ausgabe(app, h.kennung)
        val alleDateien = j.optJSONArray("dateien")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()
        val zuGross = alleDateien.filter { it.optBoolean("zu_gross") }
        val hinweis = if (zuGross.isEmpty()) "" else "\nZu groß für die Übertragung, liegt am Mac: " + zuGross.joinToString(", ") { it.optString("pfad") }
        val dateien = alleDateien.filter { !it.optBoolean("zu_gross") }.map { d ->
            val name = File(d.optString("name")).name
            val ziel = File(ausgabe, UUID.randomUUID().toString().take(6) + "-" + name)
            ziel.writeBytes(Base64.decode(d.optString("base64"), Base64.DEFAULT))
            name to ziel.absolutePath
        }
        return de.frank.jarvis.helfer.HelferErgebnis(true, j.optString("text") + hinweis, j.optJSONObject("daten") ?: JSONObject(), dateien, dauerMs = j.optLong("dauer_ms"))
    }

    suspend fun verwalten(kennung: String, aktion: String): Ergebnis {
        val h = HelferKatalog.finde(app, kennung) ?: return Ergebnis("Diesen Helfer gibt es nicht.", fehler = true)
        if (h.ort == "mac") {
            if (!mac().macErreichbar()) return Ergebnis("Der Mac ist gerade nicht erreichbar; der Helfer liegt dort und bleibt unverändert.", fehler = true)
            val j = runCatching { JSONObject(mac().helferAnfrage("helfer_verwalten", JSONObject().put("helfer", kennung).put("aktion", aktion), warteMs = 30_000L).orEmpty()) }.getOrDefault(JSONObject())
            return if (j.optBoolean("ok")) Ergebnis(j.optString("text").ifEmpty { "Am Mac erledigt." }) else Ergebnis(j.optString("fehler").ifEmpty { "Der Mac hat nicht bestätigt." }, fehler = true)
        }
        return when (aktion) {
            "aktivieren" -> { HelferKatalog.aktivieren(app, kennung, true); Ergebnis("Aktiviert.") }
            "deaktivieren" -> { HelferKatalog.aktivieren(app, kennung, false); Ergebnis("Deaktiviert.") }
            "papierkorb" -> { HelferKatalog.inPapierkorb(app, kennung); Ergebnis("Im Papierkorb. Konten und andere Helfer bleiben unberührt.") }
            "wiederherstellen" -> { HelferKatalog.wiederherstellen(app, kennung); Ergebnis("Wiederhergestellt.") }
            "rueckrollen" -> if (HelferKatalog.rueckrollen(app, kennung) != null) Ergebnis("Die letzte geprüfte Fassung gilt wieder.") else Ergebnis("Es gibt keine frühere Fassung.", fehler = true)
            else -> Ergebnis("Unbekannte Aktion.", fehler = true)
        }
    }

    private fun uebersicht(): String {
        val e = de.frank.jarvis.data.Einstellungen.get(app)
        val kopf = "Automatik ${if (e.helferAutomatik) "an" else "pausiert"}, höchstens ${e.helferParallel} Bauten gleichzeitig und ${e.helferProTag} je Tag."
        val l = HelferAblauf.auftraege(app).take(10)
        if (l.isEmpty()) return "$kopf Noch keine Werkzeugbauten."
        return kopf + "\n" + l.joinToString("\n") { x ->
            val s = x.optJSONObject("schaetzung")
            val ent = x.optJSONArray("entscheidungen")?.let { a -> (0 until a.length()).joinToString("; ") { a.optJSONObject(it)?.optString("text").orEmpty() } }.orEmpty()
            "- ${x.optString("id")} ${x.optString("kennung")}: ${BauStand.text(x.optString("stand"))}. ${x.optString("schritt")} (Schätzung ${s?.optInt("von")}–${s?.optInt("bis")} min)" + if (ent.isEmpty()) "" else ". Entscheidungen: $ent"
        }
    }
}
