package de.frank.longevity.ki

import de.frank.longevity.auth.ChatTurn
import de.frank.longevity.auth.CodexAuthManager
import de.frank.longevity.auth.CodexModel
import de.frank.longevity.auth.ReasoningEffort
import de.frank.longevity.data.Einstellungen
import de.frank.longevity.data.Evidenz
import de.frank.longevity.data.Faktor
import de.frank.longevity.data.Kategorie
import de.frank.longevity.data.Punkt
import de.frank.longevity.data.ordnen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/** Ergebnis der Auswertung einer eigenen Idee. */
data class Auswertung(val faktor: Faktor, val rang: Int, val bewertung: String, val duplikatVon: Long?)

/** Ergebnis einer Aktualisierung: neue Reihenfolge (mit Altbestand gemischt) und Vorschläge. */
data class Aktualisierung(
    val liste: List<Faktor>,
    val vorschlaege: List<Faktor>,
    val zusammenfassung: String,
    val veraendert: Int,
    /** Beim Mitdiskutieren: die Einordnung des Nutzer-Beitrags durch die Gutachterin. */
    val einordnung: String = "",
)

/**
 * Alle KI-Arbeiten der App. Die Hauptarbeit läuft mit dem Modell aus den Einstellungen
 * (Standard GPT-6 Astra, Hoch), die Textkorrektur mit einem eigenen, meist schnelleren Modell.
 */
class LongevityKi(
    private val auth: CodexAuthManager,
    private val e: Einstellungen,
    /** Der Standard-Prompt der Aktualisierung (assets/aktualisierung.md). */
    private val standardVorlage: () -> String,
) {

    // ================= Textkorrektur =================

    suspend fun korrigiere(text: String, bisher: List<String>): String {
        val anweisung = buildString {
            append(
                "Der Text ist eine Eingabe für eine Langlebigkeits-App – diktiert per Spracherkennung oder schnell getippt – und " +
                    "deshalb unsauber. Erkenne die Absicht und gib genau diesen Inhalt in klarem, sehr gutem Deutsch wieder. " +
                    "Korrigiere Grammatik, Rechtschreibung, Satzbau und Wortwahl, entferne Versprecher, Füllwörter und Wiederholungen. " +
                    "Fachbegriffe (z. B. VO2max, Kalorienrestriktion, Rapamycin, ApoB, Omega-3) richtig schreiben. " +
                    "Füge NICHTS Inhaltliches hinzu und lass nichts weg. Zahlen und Namen exakt behalten. " +
                    "Antworte nur mit dem verbesserten Text, ohne Vorrede und ohne Anführungszeichen.",
            )
            if (bisher.isNotEmpty()) {
                append("\n\nDiese Fassungen gab es schon. Liefere eine deutlich andere Formulierung bei gleichem Inhalt:\n")
                bisher.forEach { append("- ").append(it.take(600)).append('\n') }
            }
        }
        return frage(anweisung, text, e.korrekturModell, e.korrekturDenkstufe).trim().trim('„', '“', '"')
    }

    // ================= Eigene Idee auswerten =================

    suspend fun auswerten(idee: String, liste: List<Faktor>, fortschritt: Fortschritt): Auswertung {
        fortschritt.band(0f, 1f, erwarteteZeichen = 3500, erwarteteSekunden = 45f, schritt = "Die KI prüft deine Idee …")
        val antwort = frage(
            grundanweisung(),
            """
            |AKTUELLE LISTE (id | Rang | Titel | Kategorie | Evidenz | Jahre; oben Plus-Faktoren, unter der Null-Linie die
            |Lebenszeit-Räuber mit negativen Jahren, der schädlichste ganz unten):
            |${listeKompakt(liste)}
            |
            |NEUE IDEE DES NUTZERS:
            |${idee.trim()}
            |
            |AUFGABE: Prüfe die Idee kritisch und ehrlich nach aktuellem Forschungsstand und nach Logik. Formuliere daraus
            |einen Faktor für die Liste und ordne ihn nach seiner Wichtigkeit (Gewinn an gesunder Lebenszeit) in die Liste ein:
            |"rang" ist die Position, an der er eingefügt wird (1 bis ${liste.size + 1}). Vergleiche dazu direkt mit den
            |Nachbarn darüber und darunter. Beschreibt die Idee ein schädliches Verhalten oder ein Verbot („nicht rauchen“,
            |„weniger Zucker“), formuliere den Faktor als das schädliche Verhalten selbst („Rauchen“, „Viel Zucker essen“) mit
            |NEGATIVEN Jahren – er wird dann automatisch unter der Null-Linie nach verlorenen Jahren einsortiert. Ist die Idee im Kern schon als Faktor vorhanden, setze "duplikatVon" auf dessen id
            |und liefere in "ergaenzung" nur das, was die Idee Neues beiträgt.
            |
            |Antworte NUR mit einem JSON-Objekt, ohne Text davor oder danach:
            |{"bewertung": "2–3 Sätze: Stimmt die Idee? Wie gut belegt? Was ist dran, was nicht?",
            | "duplikatVon": null, "ergaenzung": "",
            | ${FAKTOR_SCHEMA},
            | "rang": 5}
            """.trimMargin(),
            e.modell, e.denkstufe, fortschritt,
        )
        val o = jsonAus(antwort)
        val f = faktorAus(o).copy(eigen = true, notiz = idee.trim(), neu = true, vertieft = true)
        val dup = o.optLong("duplikatVon", -1L).takeIf { id -> id > 0 && liste.any { it.id == id } }
        val bewertung = o.optString("bewertung").trim()
        val ergaenzung = o.optString("ergaenzung").trim()
        return Auswertung(
            faktor = if (dup != null) f.copy(erklaerung = ergaenzung.ifBlank { f.erklaerung }) else f,
            rang = o.optInt("rang", liste.size + 1).coerceIn(1, liste.size + 1),
            bewertung = bewertung,
            duplikatVon = dup,
        )
    }

    // ================= Einen Faktor vertiefen =================

    suspend fun vertiefen(f: Faktor, liste: List<Faktor>, fortschritt: Fortschritt): Faktor {
        fortschritt.band(0f, 1f, erwarteteZeichen = 5000, erwarteteSekunden = 60f, schritt = "Die KI recherchiert „${f.titel}“ …")
        val antwort = frage(
            grundanweisung(),
            """
            |GESAMTLISTE zur Einordnung:
            |${listeKompakt(liste)}
            |
            |ZU VERTIEFEN: Rang ${f.rang} – ${f.titel}
            |Bisherige Erklärung: ${f.erklaerung}
            |Bisheriges Ziel: ${f.ziel}
            |Bisheriger Aufgabenplan:
            |${f.punkte.joinToString("\n") { "- ${it.titel}: ${it.text}" + if (it.erledigt) " (vom Nutzer erledigt)" else "" }}
            |${if (f.notiz.isNotBlank()) "Ursprüngliche Eingabe des Nutzers: ${f.notiz}" else ""}
            |
            |AUFGABE: Vertiefe diesen Faktor sehr gründlich. Verbessere den Bestand, statt ihn zu verwerfen: Was stimmt, bleibt
            |sinngemäß erhalten; ergänze neue Erkenntnisse und korrigiere Veraltetes. Erkläre genau, welche Verhaltensweise gemeint
            |ist, warum sie wirkt (Mechanismus) und was die Forschung zeigt (mit Zahlen). Begründe, warum der Faktor genau auf
            |${if (f.raeuber) "seinem Platz unter der Null-Linie (Lebenszeit-Räuber, ${"%.1f".format(Locale.US, f.jahre)} Jahre)" else "Rang ${f.rang}"} steht.
            |Ist der Faktor ein Lebenszeit-Räuber, beschreibt der Titel das schädliche Verhalten, "jahre" bleibt negativ, und Ziel
            |und Aufgabenplan zeigen, wie man es abstellt. Leite daraus ein klares, messbares persönliches Ziel ab und einen Aufgabenplan mit 5–9 Punkten,
            |sortiert nach Wichtigkeit (was man sofort umsetzen sollte, zuerst). Ist der Faktor eine Sammelkategorie (z. B.
            |Supplements, Lebensmittel, Übungen), sind die Punkte die einzelnen Elemente (z. B. die einzelnen Supplements, 10–16 Stück),
            |breit gestreut von gut belegt bis logisch plausibel, jeweils ehrlich eingeordnet, das wichtigste zuerst.
            |
            |Antworte NUR mit einem JSON-Objekt:
            |{ ${FAKTOR_SCHEMA} }
            """.trimMargin(),
            e.modell, e.denkstufe, fortschritt,
        )
        val neu = faktorAus(jsonAus(antwort))
        return f.copy(
            titel = neu.titel.ifBlank { f.titel },
            kurz = neu.kurz.ifBlank { f.kurz },
            kategorie = neu.kategorie,
            evidenz = neu.evidenz,
            jahre = if (neu.jahre != 0f) neu.jahre else f.jahre,
            wirkung = neu.wirkung,
            erklaerung = neu.erklaerung.ifBlank { f.erklaerung },
            begruendung = neu.begruendung.ifBlank { f.begruendung },
            ziel = neu.ziel.ifBlank { f.ziel },
            punkteJson = Faktor.punkteAlsJson(punkteMischen(f.punkte, neu.punkte)),
            vertieft = true,
        )
    }

    // ================= Aktualisierung mit zwei diskutierenden Agenten =================

    /** Der große Lauf: Forscherin und Skeptiker diskutieren zwei Runden, die Gutachterin entscheidet. */
    suspend fun aktualisieren(liste: List<Faktor>, fortschritt: Fortschritt): Aktualisierung {
        val v = vorlage()
        val lauf = Lauf(v, listeKompakt(liste), "", StringBuilder(), fortschritt)
        lauf.sprich(PRO, "runde 1 forscherin", 0.00f, 0.22f, 4200, 70f)
        lauf.sprich(CONTRA, "runde 1 skeptiker", 0.22f, 0.44f, 4200, 70f)
        lauf.sprich(PRO, "runde 2 forscherin", 0.44f, 0.58f, 2800, 50f)
        lauf.sprich(CONTRA, "runde 2 skeptiker", 0.58f, 0.70f, 2200, 45f)
        val richter = lauf.sprich(RICHTER, "entscheidung", 0.70f, 0.99f, 900 + liste.size * 330, 60f + liste.size * 2f)
        return mischen(liste, jsonAus(richter))
    }

    /**
     * Der Nutzer redet mit: Sein Beitrag läuft durch alle Agenten (Forscherin, Skeptiker, Forscherin, Skeptiker),
     * die Gutachterin bildet daraus einen Konsens, ordnet den Beitrag ein und passt die Rangliste an.
     */
    suspend fun einwand(liste: List<Faktor>, text: String, bisher: List<Beitrag>, fortschritt: Fortschritt): Aktualisierung {
        val v = vorlage()
        val verlauf = StringBuilder()
        bisher.forEach { b -> verlauf.append("\n### ").append(b.name).append(":\n").append(beitragKurz(b)).append('\n') }
        fortschritt.beitragFertig(NUTZER, text)
        verlauf.append("\n### ").append(NUTZER).append(" (der Nutzer):\n").append(text).append('\n')
        val lauf = Lauf(v, listeKompakt(liste), text, verlauf, fortschritt)
        lauf.sprich(PRO, "einwand forscherin", 0.00f, 0.22f, 3000, 60f)
        lauf.sprich(CONTRA, "einwand skeptiker", 0.22f, 0.44f, 3000, 60f)
        lauf.sprich(PRO, "einwand forscherin antwort", 0.44f, 0.58f, 2200, 45f)
        lauf.sprich(CONTRA, "einwand skeptiker schlusswort", 0.58f, 0.70f, 1900, 40f)
        val richter = lauf.sprich(RICHTER, "einwand entscheidung", 0.70f, 0.99f, 1200 + liste.size * 330, 60f + liste.size * 2f)
        val o = jsonAus(richter)
        return mischen(liste, o).copy(einordnung = o.optString("einordnung").trim())
    }

    /** Ein Diskussionslauf: baut je Agent Systemanweisung und Nachricht aus der Vorlage und sammelt den Verlauf. */
    private inner class Lauf(
        val v: Map<String, String>,
        val liste: String,
        val einwand: String,
        val verlauf: StringBuilder,
        val fortschritt: Fortschritt,
    ) {
        suspend fun sprich(name: String, auftrag: String, von: Float, bis: Float, zeichen: Int, sekunden: Float): String {
            val werte = mapOf(
                "PRO" to PRO, "CONTRA" to CONTRA, "RICHTER" to RICHTER, "EINWAND" to einwand,
                "FAKTOR_SCHEMA" to FAKTOR_SCHEMA, "PROFIL" to profilZeile(), "LISTE" to liste,
                "DISKUSSION" to verlauf.toString().trim().ifBlank { "(noch keine – du beginnst)" },
            )
            fun teil(k: String) = fuelle(v[k].orEmpty(), werte)
            val rolle = when (name) { PRO -> "rolle forscherin"; CONTRA -> "rolle skeptiker"; else -> "rolle gutachterin" }
            val system = fuelle(v[AUFBAU_SYSTEM].orEmpty(), werte + mapOf("GRUNDANWEISUNG" to teil("grundanweisung"), "ROLLE" to teil(rolle)))
            val nachricht = fuelle(v[AUFBAU_NACHRICHT].orEmpty(), werte + ("AUFTRAG" to teil(auftrag)))
            fortschritt.band(von, bis, zeichen, sekunden, "$name ${if (name == RICHTER) "wägt ab" else "argumentiert"} …")
            fortschritt.beitragBeginnt(name)
            val text = frage(system.trim(), nachricht.trim(), e.modell, e.denkstufe, fortschritt).trim()
            fortschritt.beitragFertig(name, text)
            verlauf.append("\n### ").append(name).append(":\n").append(text).append('\n')
            return text
        }
    }

    /** Die wirksame Vorlage: eigene Abschnitte aus den Einstellungen, fehlende oder leere aus dem Standard. */
    private fun vorlage(): Map<String, String> {
        val standard = abschnitte(standardVorlage())
        val eigen = abschnitte(e.aktualisierungsPrompt)
        return standard.keys.associateWith { k -> eigen[k]?.takeIf { it.isNotBlank() } ?: standard.getValue(k) }
    }

    private fun profilZeile(): String {
        val p = e.profilText()
        return if (p.isBlank()) "" else "PROFIL DES NUTZERS (berücksichtige es bei Rang, Ziel und Aufgaben): $p"
    }

    /** Für den Verlauf: Die Entscheidungen der Gutachterin nur als Zusammenfassung, nicht als ganzes JSON. */
    private fun beitragKurz(b: Beitrag): String = if (b.name != RICHTER) b.text else runCatching {
        val o = jsonAus(b.text)
        listOf(o.optString("einordnung"), o.optString("zusammenfassung")).filter { it.isNotBlank() }.joinToString("\n")
    }.getOrNull()?.takeIf { it.isNotBlank() }?.let { "Entscheidung: $it" } ?: "Entscheidung gefällt."

    /** Übernimmt die Entscheidung des Richters, ohne je etwas aus dem Altbestand zu verlieren. */
    private fun mischen(liste: List<Faktor>, o: JSONObject): Aktualisierung {
        val nachId = liste.associateBy { it.id }
        val datum = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date())
        val reihenfolge = o.optJSONArray("reihenfolge") ?: JSONArray()
        val gesehen = mutableSetOf<Long>()
        val neueListe = mutableListOf<Faktor>()
        for (i in 0 until reihenfolge.length()) {
            val r = reihenfolge.optJSONObject(i) ?: continue
            val alt = nachId[r.optLong("id", -1)] ?: continue
            if (!gesehen.add(alt.id)) continue
            val ergaenzung = r.optString("ergaenzung").trim()
            val jahre = r.optDouble("jahre", Double.NaN)
            val wirkung = r.optInt("wirkung", -1)
            neueListe += alt.copy(
                titel = r.optString("titel").trim().trim('.').take(72).ifBlank { alt.titel },
                kurz = r.optString("kurz").trim().ifBlank { alt.kurz },
                ziel = r.optString("ziel").trim().ifBlank { alt.ziel },
                begruendung = r.optString("begruendung").trim().ifBlank { alt.begruendung },
                erklaerung = if (ergaenzung.isBlank()) alt.erklaerung else alt.erklaerung.trimEnd() + "\n\nNeu ($datum): " + ergaenzung,
                evidenz = r.optString("evidenz").takeIf { s -> Evidenz.entries.any { it.name == s } } ?: alt.evidenz,
                jahre = if (!jahre.isNaN() && jahre != 0.0) jahre.toFloat().coerceIn(-20f, 20f) else alt.jahre,
                wirkung = if (wirkung in 0..100) wirkung else alt.wirkung,
            )
        }
        // Was der Richter vergessen hat, bleibt erhalten – in seiner bisherigen Reihenfolge dahinter.
        liste.filter { it.id !in gesehen }.forEach { neueListe += it }
        val jetzt = System.currentTimeMillis()
        // Die Null-Linie setzt der Code durch: Plus-Faktoren in der Reihenfolge des Richters, Räuber nach verlorenen Jahren.
        val ergebnis = ordnen(neueListe).map { f ->
            f.copy(vorherRang = nachId[f.id]?.rang, neu = false, geaendertAm = jetzt)
        }
        val veraendert = ergebnis.count { it.rang != it.vorherRang }
        val neu = o.optJSONArray("neu") ?: JSONArray()
        val vorschlaege = (0 until neu.length()).mapNotNull { i ->
            val n = neu.optJSONObject(i) ?: return@mapNotNull null
            faktorAus(n).takeIf { it.titel.isNotBlank() }?.copy(rang = n.optInt("rang", ergebnis.size + 1), vertieft = true, neu = true)
        }.take(3)
        return Aktualisierung(ergebnis, vorschlaege, o.optString("zusammenfassung").trim(), veraendert)
    }

    // ================= Gemeinsames =================

    private fun grundanweisung(): String = buildString {
        append(
            "Du bist ein weltweit führender Experte für Langlebigkeitsforschung (Geroscience, Epidemiologie, Sportmedizin, " +
                "Ernährungswissenschaft, Schlafforschung, Psychologie, Präventivmedizin). Du betrachtest den Menschen ganzheitlich " +
                "in allen Lebensbereichen: Bewegung, Fitness, Kraft, Ernährung, Schlaf, Supplements, Stress, Geist, Beziehungen, " +
                "Sinn, Vorsorge, Umwelt, Genussmittel. Du berücksichtigst nicht nur gesicherte Evidenz (RCTs, Metaanalysen, " +
                "Mendel-Randomisierung, große Kohorten), sondern auch sehr wahrscheinliche und logisch gut begründete Faktoren – " +
                "und ordnest ehrlich ein: BELEGT, WAHRSCHEINLICH oder LOGISCH. Die Rangliste hat eine Null-Linie: Oben stehen " +
                "förderliche Verhaltensweisen mit POSITIVEN Jahren – der Gewinn an gesunder Lebenszeit durch konsequente Umsetzung " +
                "gegenüber dem Unterlassen. Unten stehen schädliche Verhaltensweisen (Lebenszeit-Räuber) mit NEGATIVEN Jahren – " +
                "die verlorene Lebenszeit gegenüber dem Unterlassen, der schädlichste ganz unten. Ein Verbot ist nie ein Plus-Faktor: " +
                "Man wird als Nichtraucher geboren, Nichtrauchen schenkt keine Jahre, Rauchen kostet sie. Der Titel nennt deshalb " +
                "das schädliche Verhalten selbst („Rauchen“, nicht „Nicht rauchen“) mit negativen Jahren. Denke sehr " +
                "gründlich, detailliert und durchdacht. Schreibe auf Deutsch, klar und konkret, ohne Heilversprechen.",
        )
        val p = e.profilText()
        if (p.isNotBlank()) append("\n\nPROFIL DES NUTZERS (berücksichtige es bei Rang, Ziel und Aufgaben): ").append(p)
    }

    private fun listeKompakt(liste: List<Faktor>) = liste.joinToString("\n") {
        "${it.id} | ${it.rang} | ${it.titel} | ${it.kat.name} | ${it.ev.name} | ${"%.1f".format(Locale.US, it.jahre)}" + if (it.zielErreicht) (if (it.raeuber) " | trifft beim Nutzer nicht zu bzw. abgestellt" else " | vom Nutzer bereits umgesetzt") else ""
    }

    private suspend fun frage(
        anweisung: String,
        text: String,
        modell: CodexModel,
        stufe: ReasoningEffort,
        fortschritt: Fortschritt? = null,
    ): String = auth.streamChat(
        instructions = anweisung,
        turns = listOf(ChatTurn("user", text.trim())),
        model = modell,
        reasoningEffort = stufe,
        onDelta = { stueck -> fortschritt?.zeichen(stueck) },
    )

    companion object {
        const val PRO = "Forscherin Vita"
        const val CONTRA = "Skeptiker Kron"
        const val RICHTER = "Gutachterin Aeon"
        const val NUTZER = "Du"

        const val AUFBAU_SYSTEM = "aufbau systemanweisung"
        const val AUFBAU_NACHRICHT = "aufbau nachricht"

        /** Zerlegt die Vorlage an den Überschriften „## Name“; alles vor der ersten ist Erklärung. */
        fun abschnitte(text: String): Map<String, String> {
            val ergebnis = linkedMapOf<String, String>()
            var name: String? = null
            val puffer = StringBuilder()
            fun ablegen() { name?.let { ergebnis[it] = puffer.toString().trim() } }
            text.lineSequence().forEach { zeile ->
                if (zeile.startsWith("## ")) {
                    ablegen()
                    name = zeile.removePrefix("## ").trim().lowercase()
                    puffer.clear()
                } else if (name != null) {
                    puffer.append(zeile).append('\n')
                }
            }
            ablegen()
            return ergebnis
        }

        /** Ersetzt {{NAME}} in einem Durchgang; unbekannte Platzhalter bleiben stehen. */
        fun fuelle(text: String, werte: Map<String, String>): String =
            Regex("\\{\\{([A-Z_]+)\\}\\}").replace(text) { m -> werte[m.groupValues[1]] ?: m.value }

        private val KATEGORIEN = Kategorie.entries.joinToString("|") { it.name }

        val FAKTOR_SCHEMA = """"titel": "max. 60 Zeichen, beschreibt das Verhalten komplett", "kurz": "1 Satz Kernaussage",
            | "kategorie": "$KATEGORIEN", "evidenz": "BELEGT|WAHRSCHEINLICH|LOGISCH",
            | "jahre": "Zahl: positiv (z. B. 2.5) bei förderlichem Verhalten, negativ (z. B. -10) bei schädlichem", "wirkung": 0-100 (Stärke, auch bei schädlichem Verhalten positiv), "erklaerung": "4–7 Sätze", "begruendung": "2–3 Sätze, warum genau dieser Rang",
            | "ziel": "konkretes, messbares Ziel", "punkte": [{"titel": "max. 50 Zeichen", "text": "1–2 Sätze, konkret mit Dosis/Häufigkeit", "evidenz": "BELEGT|WAHRSCHEINLICH|LOGISCH"}]""".trimMargin()

        fun faktorAus(o: JSONObject): Faktor {
            val punkte = o.optJSONArray("punkte") ?: JSONArray()
            val liste = (0 until punkte.length()).mapNotNull { i ->
                val p = punkte.optJSONObject(i) ?: return@mapNotNull null
                Punkt(p.optString("titel").trim().take(80), p.optString("text").trim(), Evidenz.von(p.optString("evidenz")).name)
            }.filter { it.titel.isNotBlank() }
            return Faktor(
                titel = o.optString("titel").trim().trim('.').take(72),
                kurz = o.optString("kurz").trim(),
                kategorie = Kategorie.von(o.optString("kategorie")).name,
                evidenz = Evidenz.von(o.optString("evidenz")).name,
                jahre = o.optDouble("jahre", 0.0).toFloat().coerceIn(-20f, 20f),
                wirkung = o.optInt("wirkung", 50).coerceIn(0, 100),
                erklaerung = o.optString("erklaerung").trim(),
                begruendung = o.optString("begruendung").trim(),
                ziel = o.optString("ziel").trim(),
                punkteJson = Faktor.punkteAlsJson(liste),
            )
        }

        /** Neue Punkte übernehmen, Häkchen des Nutzers behalten, erledigte alte Punkte nie verlieren. */
        fun punkteMischen(alt: List<Punkt>, neu: List<Punkt>): List<Punkt> {
            if (neu.isEmpty()) return alt
            fun schluessel(p: Punkt) = p.titel.lowercase().filter { it.isLetterOrDigit() }.take(18)
            val erledigt = alt.filter { it.erledigt }.map(::schluessel).toSet()
            val gemischt = neu.map { if (schluessel(it) in erledigt) it.copy(erledigt = true) else it }
            val fehlend = alt.filter { it.erledigt && gemischt.none { n -> schluessel(n) == schluessel(it) } }
            return gemischt + fehlend
        }
    }
}

/** Zieht das erste vollständige JSON-Objekt aus einer Modellantwort (auch mit Vorrede oder ```-Zaun). */
fun jsonAus(text: String): JSONObject {
    val s = text.replace("```json", "").replace("```", "")
    val start = s.indexOf('{')
    if (start < 0) throw IllegalStateException("Die KI hat keine auswertbare Antwort geliefert.")
    var tiefe = 0
    var inString = false
    var escape = false
    for (i in start until s.length) {
        val c = s[i]
        if (inString) {
            when {
                escape -> escape = false
                c == '\\' -> escape = true
                c == '"' -> inString = false
            }
            continue
        }
        when (c) {
            '"' -> inString = true
            '{' -> tiefe++
            '}' -> if (--tiefe == 0) return JSONObject(s.substring(start, i + 1))
        }
    }
    throw IllegalStateException("Die Antwort der KI war unvollständig.")
}
