package de.frank.experimente.ai

import de.frank.experimente.auth.CodexModell
import de.frank.experimente.auth.CodexZugang
import de.frank.experimente.auth.Effort
import de.frank.experimente.data.local.Experiment
import de.frank.experimente.data.local.Insight
import de.frank.experimente.data.local.Stufe
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Die acht Aufgaben, die Codex für diese App erledigt: F-02, F-03/F-04, F-09, F-11, F-14,
 * F-15, F-17 und die Dauer-/Stufen-Schätzung aus F-18.
 *
 * Jede Aufgabe hat ihre eigene Anweisung und, wo die Antwort weiterverarbeitet wird, ein
 * strenges Schema — damit keine Antwort geraten werden muss.
 */
class Aufgaben(private val codex: CodexZugang) {

    /** Ein Vorschlag, wie die KI ihn liefert (F-03 Schritt 4). */
    data class RoherVorschlag(
        val titel: String,
        val beschreibung: String,
        val tage: Int,
        val stufe: Stufe,
        val vonMerkliste: Boolean,
        val aufgabenJeTag: List<List<String>>,
    )

    // --- F-02 ---------------------------------------------------------------------------

    /**
     * F-02 — Text mit KI verbessern.
     *
     * Verfahren aus `CodexAuthManager.improveWish()`: die bisherigen Fassungen gehen mit,
     * damit jeder Druck eine **neue** Formulierung liefert und keine Wiederholung.
     */
    suspend fun verbessere(
        text: String,
        bisherigeFassungen: List<String>,
        modell: CodexModell,
        effort: Effort,
    ): String {
        val schema = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("required", JSONArray().put("fassung"))
            .put("properties", JSONObject().put("fassung", JSONObject().put("type", "string")))

        val anweisung = buildString {
            append(
                "Der Text kommt aus einer Spracherkennung und ist deshalb unsauber. Erkenne seine " +
                    "Absicht und gib genau diese Absicht in sehr gutem Deutsch wieder. Korrigiere " +
                    "Grammatik, Satzbau und Wortwahl, löse Versprecher, Füllwörter und " +
                    "Wiederholungen auf. Füge NICHTS hinzu, was nicht im Text steht. Lass NICHTS " +
                    "weg und schwäche nichts ab. Behalte die Aussageform, die Sprache, die Person " +
                    "und die Anrede des Originals bei. Antworte nur mit der umgeschriebenen " +
                    "Fassung, ohne Vorrede und ohne Anführungszeichen.",
            )
            if (bisherigeFassungen.isNotEmpty()) {
                append(
                    "\n\nDiese Fassungen wurden bereits vorgeschlagen. Liefere eine deutlich andere " +
                        "Formulierung — anderer Aufbau, andere Wortwahl — bei gleichem Inhalt:\n",
                )
                append(JSONArray(bisherigeFassungen).toString())
            }
        }

        val antwort = codex.frage(anweisung, "Text:\n${text.trim()}", modell, effort, schema, "fassung")
        return JSONObject(saeubere(antwort)).optString("fassung").trim().trim('„', '“', '“')
            .ifBlank { text }
    }

    // --- F-03 / F-04 --------------------------------------------------------------------

    /**
     * F-03 — fünf Vorschläge, F-04 — fünf andere.
     *
     * Zusammensetzung: zwei zur Lage passend, zwei völlig neu, einer von der Merkliste. Ist
     * die Merkliste leer, wird dieser Platz ein weiterer neuer Vorschlag.
     */
    suspend fun fuenfVorschlaege(
        kontext: Kontext,
        verworfeneTitel: List<String>,
        modell: CodexModell,
        effort: Effort,
    ): List<RoherVorschlag> {
        val vorschlag = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put(
                "required",
                JSONArray().put("titel").put("beschreibung").put("tage").put("stufe")
                    .put("vonMerkliste").put("aufgabenJeTag"),
            )
            .put(
                "properties",
                JSONObject()
                    .put("titel", JSONObject().put("type", "string"))
                    .put("beschreibung", JSONObject().put("type", "string"))
                    .put("tage", JSONObject().put("type", "integer"))
                    .put(
                        "stufe",
                        JSONObject().put("type", "string")
                            .put("enum", JSONArray().put("leicht").put("mittel").put("fordernd")),
                    )
                    .put("vonMerkliste", JSONObject().put("type", "boolean"))
                    .put(
                        "aufgabenJeTag",
                        JSONObject().put("type", "array").put(
                            "items",
                            JSONObject().put("type", "array")
                                .put("items", JSONObject().put("type", "string")),
                        ),
                    ),
            )

        val schema = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("required", JSONArray().put("vorschlaege"))
            .put(
                "properties",
                JSONObject().put(
                    "vorschlaege",
                    JSONObject().put("type", "array").put("items", vorschlag),
                ),
            )

        val anweisung = buildString {
            append(
                "Du schlägst Frank jeden Tag fünf persönliche Experimente vor — Dinge, die er so " +
                    "noch NIE gemacht hat. Du kennst ihn aus dem Kontext unten: Selbstbild, Ziele, " +
                    "Logbuch, Langzeit-Log, Erkenntnisse. Nutze das wirklich; im Zweifel gewinnt " +
                    "der Kontext.\n\n",
            )
            append("Liefere GENAU FÜNF Vorschläge in dieser Zusammensetzung:\n")
            append("- ZWEI, die zur heutigen Lage und zu Franks Geschichte passen\n")
            append("- ZWEI, die völlig neu sind — aus Bereichen, mit denen er noch nichts zu tun hatte\n")
            append(
                "- EINEN von der Merkliste (`vonMerkliste` = true, Titel und Beschreibung genau " +
                    "von dort übernehmen). Ist die Merkliste leer, mach daraus einen weiteren " +
                    "völlig neuen Vorschlag mit `vonMerkliste` = false.\n\n",
            )
            append(
                "Es gibt KEINE feste Bereichseinteilung des Lebens. Zieh frei aus allem, was zu " +
                    "einem Menschenleben gehört: Körper, Geld, Ernährung, Umgang mit Menschen, " +
                    "Fähigkeiten, Lernen, Bewegung, Sinn, Alltag, Gewohnheiten, Kunst, Ordnung, " +
                    "Ruhe, Wagnis. Das Langzeit-Log ist die Prüfliste dagegen: was dort steht, " +
                    "hat er schon gemacht — schlag es nicht wieder vor.\n\n",
            )
            append(
                "DOSIERE DIE STUFE aus der Chronik: anfangs leicht; mit jedem abgeschlossenen " +
                    "Experiment ein Stück mutiger; nach einem nicht umgesetzten wieder sanfter. " +
                    "Herausfordern, aber nicht überfordern.\n\n",
            )
            append(
                "Je Vorschlag: Titel, Beschreibung (was genau zu tun ist und warum es interessant " +
                    "sein könnte), Dauer in Tagen (1 oder mehr), Stufe, und die Aufgabenliste JE " +
                    "TAG — `aufgabenJeTag` hat genau so viele Einträge wie `tage`.\n\n",
            )
            append("Bewerte nicht, vergib keine Punkte, ermahne nicht. Schreib deutsch, ruhig und klar.")
            if (verworfeneTitel.isNotEmpty()) {
                append(
                    "\n\nDiese Vorschläge hat Frank heute schon gesehen und verworfen. Liefere " +
                        "fünf andere aus ANDEREN Bereichen — nicht fünf Abwandlungen desselben " +
                        "Gedankens:\n",
                )
                append(JSONArray(verworfeneTitel).toString())
            }
        }

        val antwort = codex.frage(anweisung, kontext.alsText(), modell, effort, schema, "vorschlaege")
        val liste = JSONObject(saeubere(antwort)).optJSONArray("vorschlaege") ?: JSONArray()
        return (0 until liste.length()).mapNotNull { i ->
            val o = liste.optJSONObject(i) ?: return@mapNotNull null
            val tage = o.optInt("tage", 1).coerceAtLeast(1)
            val aufgaben = o.optJSONArray("aufgabenJeTag")
            RoherVorschlag(
                titel = o.optString("titel").trim(),
                beschreibung = o.optString("beschreibung").trim(),
                tage = tage,
                stufe = alsStufe(o.optString("stufe")),
                vonMerkliste = o.optBoolean("vonMerkliste", false),
                aufgabenJeTag = (0 until (aufgaben?.length() ?: 0)).map { tag ->
                    val eintraege = aufgaben?.optJSONArray(tag)
                    (0 until (eintraege?.length() ?: 0)).mapNotNull {
                        eintraege?.optString(it)?.trim()?.takeIf(String::isNotBlank)
                    }
                },
            ).takeIf { it.titel.isNotBlank() }
        }
    }

    // --- F-09 ---------------------------------------------------------------------------

    /** F-09 — die Antwort im Gespräch zu einem laufenden Experiment. */
    suspend fun gespraechsantwort(
        kontext: Kontext,
        experiment: Experiment,
        faden: String,
        frage: String,
        modell: CodexModell,
        effort: Effort,
    ): String {
        val anweisung =
            "Du sprichst mit Frank über sein laufendes Experiment „${experiment.title}“. Du kennst " +
                "ihn aus dem Kontext. Antworte kurz, ruhig und konkret — wie ein aufmerksamer " +
                "Gesprächspartner, nicht wie ein Ratgeber. Keine Listen, keine Überschriften, " +
                "keine Bewertung. Zwei bis fünf Sätze."
        val eingabe = buildString {
            append(kontext.alsText())
            append("\n\n## DAS EXPERIMENT\n")
            append("${experiment.title}: ${experiment.description}")
            if (faden.isNotBlank()) append("\n\n## BISHERIGES GESPRÄCH\n").append(faden)
            append("\n\n## FRANK SAGT JETZT\n").append(frage)
        }
        return codex.frage(anweisung, eingabe, modell, effort).trim()
    }

    // --- F-11 ---------------------------------------------------------------------------

    /**
     * F-11 — die KI-Auswertung.
     *
     * @param istLetzterTag am letzten Tag (oder bei einem eintägigen Experiment) die
     *        vollständige Auswertung; an einem Zwischentag nur zwei bis drei Sätze.
     */
    suspend fun auswertung(
        kontext: Kontext,
        experiment: Experiment,
        faden: String,
        hakenStand: String,
        eigenerText: String,
        istLetzterTag: Boolean,
        modell: CodexModell,
        effort: Effort,
    ): String {
        val anweisung = if (istLetzterTag) {
            "Schreib die vollständige Auswertung dieses Experiments: was du über den Verlauf " +
                "denkst, was Frank gemacht und was er nicht gemacht hat, wo er sich womöglich " +
                "nicht getraut hat, was das Gute und Positive daran war, und wie es ins " +
                "Gesamtbild passt. Bewerte NICHT mit Noten, Punkten oder gut/schlecht — " +
                "beschreibe, ordne ein und benenne, was du siehst. „Nicht gemacht“ ist ein " +
                "Ergebnis, kein Versagen. Schreib deutsch, in Absätzen, ohne Überschriften."
        } else {
            "Das Experiment läuft noch. Schreib nur einen kurzen Zwischenstand von zwei bis drei " +
                "Sätzen — keine vollständige Auswertung, keine Bewertung."
        }
        val eingabe = buildString {
            append(kontext.alsText())
            append("\n\n## DAS EXPERIMENT\n")
            append("${experiment.title}: ${experiment.description}\n")
            append("Stufe ${experiment.level.name.lowercase()}, ${experiment.days} Tag(e), begonnen am ${experiment.startedAt}")
            append("\n\n## HEUTIGE AUFGABEN\n").append(hakenStand)
            if (faden.isNotBlank()) append("\n\n## GESPRÄCH DAZU\n").append(faden)
            append("\n\n## FRANKS EIGENE AUSWERTUNG\n").append(eigenerText)
        }
        return codex.frage(anweisung, eingabe, modell, effort).trim()
    }

    // --- F-14 ---------------------------------------------------------------------------

    /** F-14 — den ausführlichen Eintrag des Tages fortschreiben (20–30 Zeilen). */
    suspend fun logbuchFortschreiben(
        tag: LocalDate,
        bisheriger: String?,
        neuerStoff: String,
        modell: CodexModell,
        effort: Effort,
    ): String {
        val anweisung =
            "Du führst Franks Logbuch. Schreib den Eintrag für den $tag fort: Lage, laufende " +
                "Experimente, was getan wurde, Gesprächsinhalte, Auswertungen. Zielumfang 20 bis " +
                "30 Zeilen. EIN Tag = EIN Eintrag: ergänze den bestehenden Text, statt einen " +
                "zweiten anzulegen, und wiederhole nichts doppelt. Sachlich, vollständig, ohne " +
                "Bewertung. Antworte nur mit dem Eintrag selbst."
        val eingabe = buildString {
            if (!bisheriger.isNullOrBlank()) append("## BISHERIGER EINTRAG\n").append(bisheriger).append("\n\n")
            append("## NEU HINZUGEKOMMEN\n").append(neuerStoff)
        }
        return codex.frage(anweisung, eingabe, modell, effort).trim()
    }

    // --- F-15 ---------------------------------------------------------------------------

    /** F-15 — einen Tag verdichten (höchstens 7 Zeilen). Unumkehrbar, darum streng geprüft. */
    suspend fun verdichte(
        tag: LocalDate,
        ausfuehrlich: String,
        modell: CodexModell,
        effort: Effort,
    ): String {
        val anweisung =
            "Verdichte diesen Logbuch-Tag auf HÖCHSTENS SIEBEN ZEILEN. Es muss erhalten bleiben: " +
                "Datum, welches Experiment, wie es durchgeführt wurde, die Auswertung und die " +
                "wichtigen Punkte. Alles andere fällt weg. Dieser verdichtete Text bleibt " +
                "dauerhaft — der ausführliche wird danach gelöscht, also darf nichts Wesentliches " +
                "verlorengehen. Antworte nur mit dem verdichteten Text."
        return codex.frage(anweisung, "Tag $tag:\n$ausfuehrlich", modell, effort).trim()
    }

    // --- F-17 ---------------------------------------------------------------------------

    /**
     * F-17 — Erkenntnisse fortschreiben.
     *
     * Die KI **ergänzt einen neuen Satz oder schärft einen bestehenden** — sie hängt nicht
     * stumpf an. Doppelungen werden zusammengeführt.
     */
    suspend fun erkenntnisseFortschreiben(
        bisherige: List<Insight>,
        neueAuswertung: String,
        modell: CodexModell,
        effort: Effort,
    ): List<String> {
        val schema = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("required", JSONArray().put("erkenntnisse"))
            .put(
                "properties",
                JSONObject().put(
                    "erkenntnisse",
                    JSONObject().put("type", "array").put("items", JSONObject().put("type", "string")),
                ),
            )
        val anweisung =
            "Das ist die Liste dessen, was Frank über sich gelernt hat. Arbeite die neue " +
                "Auswertung ein: ergänze EINEN neuen Satz oder schärfe einen bestehenden. Häng " +
                "nicht stumpf an, führe Doppelungen zusammen. Es sind Sätze über Frank, nicht über " +
                "seine Leistung — keine Bewertung, keine Punkte. Die Liste wächst langsam und " +
                "bleibt am Stück lesbar. Gib die VOLLSTÄNDIGE neue Liste zurück."
        val eingabe = buildString {
            append("## BISHERIGE ERKENNTNISSE\n")
            append(if (bisherige.isEmpty()) "(noch keine)" else bisherige.joinToString("\n") { "- ${it.text}" })
            append("\n\n## NEUE AUSWERTUNG\n").append(neueAuswertung)
        }
        val antwort = codex.frage(anweisung, eingabe, modell, effort, schema, "erkenntnisse")
        val liste = JSONObject(saeubere(antwort)).optJSONArray("erkenntnisse") ?: JSONArray()
        return (0 until liste.length()).mapNotNull { liste.optString(it).trim().takeIf(String::isNotBlank) }
    }

    // --- F-18 ---------------------------------------------------------------------------

    /** F-18 — Dauer und Stufe eines eigenen Experiments schätzt die KI beim Speichern mit. */
    /**
     * F-18 / F-35 — aus einer eingesprochenen Idee ein Experiment machen.
     *
     * [tageVorgabe] ist die von Frank **selbst gewählte** Dauer. Ist sie gesetzt, hat die KI
     * hier nichts zu entscheiden: sie verteilt die Aufgaben auf genau diese Tage. Vorher
     * schätzte sie die Dauer immer selbst — aus „die nächsten sechs, sieben Tage" wurden
     * zwei, und danach ließ sich das nirgends mehr richtigstellen.
     */
    suspend fun schaetzeEigenes(
        text: String,
        modell: CodexModell,
        effort: Effort,
        tageVorgabe: Int? = null,
    ): RoherVorschlag {
        val schema = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put(
                "required",
                JSONArray().put("titel").put("beschreibung").put("tage").put("stufe").put("aufgabenJeTag"),
            )
            .put(
                "properties",
                JSONObject()
                    .put("titel", JSONObject().put("type", "string"))
                    .put("beschreibung", JSONObject().put("type", "string"))
                    .put("tage", JSONObject().put("type", "integer"))
                    .put(
                        "stufe",
                        JSONObject().put("type", "string")
                            .put("enum", JSONArray().put("leicht").put("mittel").put("fordernd")),
                    )
                    .put(
                        "aufgabenJeTag",
                        JSONObject().put("type", "array").put(
                            "items",
                            JSONObject().put("type", "array")
                                .put("items", JSONObject().put("type", "string")),
                        ),
                    ),
            )
        val anweisung = buildString {
            append(
                "Frank hat eine eigene Experiment-Idee eingesprochen. Mach daraus einen kurzen Titel, " +
                    "eine Beschreibung (was genau zu tun ist), eine Stufe und die Aufgabenliste je Tag. " +
                    "Erfinde nichts dazu, was nicht in seinem Text steht — nur ordnen und ergänzen, was " +
                    "zum Durchführen nötig ist.",
            )
            if (tageVorgabe != null) {
                append(
                    "\n\nDie Dauer steht bereits fest: GENAU $tageVorgabe Tage. Gib im Feld „tage" +
                        "\" exakt $tageVorgabe zurück und verteile die Aufgaben auf genau $tageVorgabe " +
                        "Tage — jeder Tag bekommt seine eigenen. Steigert sich das Vorhaben, verteile " +
                        "die Steigerung gleichmäßig über diese Tage.",
                )
            } else {
                append(" Schätze auch die Dauer in Tagen.")
            }
        }
        val antwort = codex.frage(anweisung, text.trim(), modell, effort, schema, "eigenes")
        val o = JSONObject(saeubere(antwort))
        val aufgaben = o.optJSONArray("aufgabenJeTag")
        return RoherVorschlag(
            titel = o.optString("titel").trim().ifBlank { text.take(60) },
            beschreibung = o.optString("beschreibung").trim().ifBlank { text },
            // Franks Vorgabe gewinnt immer — auch wenn die KI etwas anderes zurückgibt.
            tage = tageVorgabe ?: o.optInt("tage", 1).coerceAtLeast(1),
            stufe = alsStufe(o.optString("stufe")),
            vonMerkliste = false,
            aufgabenJeTag = (0 until (aufgaben?.length() ?: 0)).map { tag ->
                val eintraege = aufgaben?.optJSONArray(tag)
                (0 until (eintraege?.length() ?: 0)).mapNotNull {
                    eintraege?.optString(it)?.trim()?.takeIf(String::isNotBlank)
                }
            },
        )
    }

    /**
     * Aufgaben für Tage nachliefern, die noch keine haben.
     *
     * Fällt an, wenn Frank ein laufendes Experiment verlängert: die neuen Tage stünden sonst
     * leer da. Die bisherigen Tage gehen mit in die Anfrage, damit der Faden weitergesponnen
     * und nicht neu erfunden wird — bei „schrittweise früher aufstehen" muss der siebte Tag
     * an den sechsten anschließen.
     *
     * @return je Tagesnummer die Aufgaben dieses Tages
     */
    suspend fun weitereTage(
        experiment: Experiment,
        bisherJeTag: List<Pair<Int, List<String>>>,
        fehlendeTage: List<Int>,
        modell: CodexModell,
        effort: Effort,
    ): Map<Int, List<String>> {
        if (fehlendeTage.isEmpty()) return emptyMap()

        val eintrag = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("required", JSONArray().put("tag").put("aufgaben"))
            .put(
                "properties",
                JSONObject()
                    .put("tag", JSONObject().put("type", "integer"))
                    .put(
                        "aufgaben",
                        JSONObject().put("type", "array")
                            .put("items", JSONObject().put("type", "string")),
                    ),
            )
        val schema = JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("required", JSONArray().put("tage"))
            .put(
                "properties",
                JSONObject().put("tage", JSONObject().put("type", "array").put("items", eintrag)),
            )

        val anweisung =
            "Ein laufendes Experiment wurde verlängert. Für die genannten Tage fehlen noch die " +
                "Aufgaben. Schreibe sie so, dass sie den bisherigen Verlauf FORTSETZEN und " +
                "aufeinander aufbauen — nicht von vorn beginnen und nichts wiederholen. Steigert " +
                "sich das Vorhaben schrittweise, führe die Steigerung folgerichtig weiter. " +
                "Liefere ausschließlich die angefragten Tagesnummern, je 1 bis 4 Aufgaben."

        val frage = buildString {
            append("Experiment: ${experiment.title}\n")
            append("Beschreibung: ${experiment.description}\n")
            append("Gesamtdauer neu: ${experiment.days} Tage\n\n")
            append("Bisherige Tage:\n")
            if (bisherJeTag.isEmpty()) {
                append("(noch keine)\n")
            } else {
                bisherJeTag.forEach { (tag, aufgaben) ->
                    append("Tag $tag:\n")
                    aufgaben.forEach { append("  - $it\n") }
                }
            }
            append("\nFehlende Tage: ${fehlendeTage.joinToString(", ")}")
        }

        val antwort = codex.frage(anweisung, frage, modell, effort, schema, "weitereTage")
        val tage = JSONObject(saeubere(antwort)).optJSONArray("tage") ?: return emptyMap()
        val ergebnis = mutableMapOf<Int, List<String>>()
        for (i in 0 until tage.length()) {
            val satz = tage.optJSONObject(i) ?: continue
            val nr = satz.optInt("tag", -1)
            // Nur die angefragten Tage übernehmen — sonst überschriebe eine übereifrige
            // Antwort die Aufgaben bestehender Tage.
            if (nr !in fehlendeTage) continue
            val liste = satz.optJSONArray("aufgaben") ?: continue
            val texte = (0 until liste.length()).mapNotNull {
                liste.optString(it).trim().takeIf(String::isNotBlank)
            }
            if (texte.isNotEmpty()) ergebnis[nr] = texte
        }
        return ergebnis
    }

    private fun alsStufe(wert: String): Stufe = when (wert.trim().lowercase()) {
        "leicht" -> Stufe.LEICHT
        "fordernd" -> Stufe.FORDERND
        else -> Stufe.MITTEL
    }

    /** Manche Antworten kommen in einem Code-Zaun — den nimmt das hier weg. */
    private fun saeubere(roh: String): String = roh.trim()
        .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}
