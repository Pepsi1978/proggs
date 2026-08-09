package de.frank.experimente.ai

import de.frank.experimente.auth.CodexModel
import de.frank.experimente.auth.ReasoningEffort
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die Nutzlasten für alle KI-Aufgaben der App. Jede Antwort ist über ein festes Schema
 * gebunden, damit das Auslesen nicht raten muss.
 *
 * Welches Modell und welcher Effort verwendet wird, entscheidet der Aufrufer — für
 * Experimente und fürs Logbuch sind das getrennte Einstellungen (F-22).
 */
object Aufgaben {

    private fun basis(
        model: CodexModel,
        effort: ReasoningEffort,
        anweisung: String,
        eingabe: String,
        schemaName: String,
        schema: JSONObject,
    ): JSONObject = JSONObject()
        .put("model", model.apiId)
        .put("service_tier", "priority")
        .put("stream", true)
        .put("store", false)
        .put("instructions", anweisung)
        .put("input", JSONArray().put(JSONObject().put("role", "user").put("content", eingabe)))
        .put("reasoning", JSONObject().put("effort", effort.apiValue))
        .put(
            "text",
            JSONObject().put(
                "format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("name", schemaName)
                    .put("strict", true)
                    .put("schema", schema),
            ),
        )

    private fun objekt(vararg felder: Pair<String, JSONObject>): JSONObject {
        val eigenschaften = JSONObject()
        val pflicht = JSONArray()
        felder.forEach { (name, typ) ->
            eigenschaften.put(name, typ)
            pflicht.put(name)
        }
        return JSONObject()
            .put("type", "object")
            .put("additionalProperties", false)
            .put("required", pflicht)
            .put("properties", eigenschaften)
    }

    private fun text() = JSONObject().put("type", "string")
    private fun zahl() = JSONObject().put("type", "integer")
    private fun liste(von: JSONObject) = JSONObject().put("type", "array").put("items", von)

    // -----------------------------------------------------------------------
    // F-03 / F-04 — fünf Vorschläge erzeugen
    // -----------------------------------------------------------------------

    private const val WAS_EIN_EXPERIMENT_IST = """
Du schlägst Frank persönliche Experimente vor. Ein Experiment ist immer etwas, das er so
NOCH NIE GEMACHT hat — nichts, was er ohnehin tut, und keine Gewohnheit, die er nur
fortsetzen soll. Es soll ihm neue Wege öffnen und zu Verhaltens- oder Einstellungsänderungen
führen können.

Es gibt KEIN festes Raster von Lebensbereichen. Zieh frei aus allem, was zu einem
Menschenleben gehört: Körper, Geld, Ernährung, Umgang mit anderen, Fähigkeiten, Lernen,
Bewegung, Sinn, Alltag, Gewohnheiten, Schöpferisches, Ordnung, Ruhe, Wagnis, Aufmerksamkeit,
Sprache, Schlaf, Umgebung, Zeit, Besitz — und was dir sonst einfällt. Ein Mensch besteht
nicht aus sechs Schubladen. Sei richtig kreativ.

Prüfe jeden Vorschlag gegen das Langzeit-Log: Was dort schon steht, ist verbraucht.

DOSIERUNG: Fordernd, aber nicht überfordernd. Lies aus der Chronik ab, was Frank
durchgezogen hat und was er hat liegen lassen. Am Anfang eher leicht; mit jedem
abgeschlossenen Experiment ein Stück mutiger; nach einem nicht umgesetzten wieder sanfter.
"""

    fun vorschlaege(
        gedaechtnis: Gedaechtnis,
        verworfeneTitel: List<String>,
        merklistenTitel: String?,
        model: CodexModel,
        effort: ReasoningEffort,
    ): JSONObject {
        val vorschlag = objekt(
            "titel" to text(),
            "beschreibung" to text(),
            "tage" to zahl(),
            "stufe" to JSONObject().put("type", "string")
                .put("enum", JSONArray().put("leicht").put("mittel").put("fordernd")),
            "aufgaben_je_tag" to liste(liste(text())),
            "von_merkliste" to JSONObject().put("type", "boolean"),
        )
        val schema = objekt("vorschlaege" to liste(vorschlag))

        val anweisung = buildString {
            append(WAS_EIN_EXPERIMENT_IST.trim())
            append("\n\nLiefere GENAU FÜNF Vorschläge in genau dieser Zusammensetzung:\n")
            append("- ZWEI, die zu seiner heutigen Lage und seiner Geschichte passen.\n")
            append("- ZWEI, die völlig neu sind — aus Bereichen, mit denen er noch nichts zu tun hatte. ")
            append("Etwas, bei dem er denkt: darüber habe ich noch nie nachgedacht.\n")
            if (merklistenTitel != null) {
                append("- EINEN von seiner Merkliste, und zwar genau diesen: „$merklistenTitel“. ")
                append("Setze bei ihm `von_merkliste` auf true, bei allen anderen auf false.\n")
            } else {
                append("- Seine Merkliste ist leer, also einen FÜNFTEN völlig neuen Vorschlag. ")
                append("Setze `von_merkliste` überall auf false.\n")
            }
            append("\nJe Vorschlag:\n")
            append("- `titel`: kurz, konkret, keine Überschrift-Floskel.\n")
            append("- `beschreibung`: zwei bis vier Sätze — was genau zu tun ist und warum ")
            append("gerade das für ihn interessant sein könnte. Sprich ihn mit „du“ an.\n")
            append("- `tage`: 1, wenn es an einem Tag erledigt ist. Mehr nur, wenn sich die ")
            append("Wirkung erst später zeigt (z. B. heute etwas tun, morgen prüfen, was sich ")
            append("verändert hat). Höchstens 5.\n")
            append("- `stufe`: leicht, mittel oder fordernd.\n")
            append("- `aufgaben_je_tag`: eine Liste je Tag, darin die konkreten Schritte für ")
            append("diesen Tag. Bei `tage` = 1 also genau eine innere Liste. Zwei bis fünf ")
            append("Schritte je Tag, jeder in einem knappen Satz, abhakbar formuliert.\n")
            if (verworfeneTitel.isNotEmpty()) {
                append("\nDIESE HAT ER HEUTE SCHON WEGGEKLICKT — schlag nichts Ähnliches erneut vor, ")
                append("geh in ANDERE Bereiche:\n")
                append(verworfeneTitel.joinToString("\n") { "- $it" })
            }
            append("\n\nAntworte auf Deutsch.")
        }

        return basis(model, effort, anweisung, gedaechtnis.alsText(), "experimente_vorschlaege", schema)
    }

    // -----------------------------------------------------------------------
    // F-09 — Gespräch zum Experiment
    // -----------------------------------------------------------------------

    fun gespraech(
        gedaechtnis: Gedaechtnis,
        experimentTitel: String,
        experimentBeschreibung: String,
        faden: String,
        frage: String,
        model: CodexModel,
        effort: ReasoningEffort,
    ): JSONObject {
        val schema = objekt("antwort" to text())
        val anweisung = """
Frank hat sich für ein Experiment entschieden und spricht jetzt mit dir darüber. Typisch
fragt er, wie du dir die Durchführung vorstellst, welche Schritte sinnvoll sind und worauf
er sich gedanklich einstellen sollte.

Antworte als jemand, der ihn kennt — nutze sein Selbstbild, seine letzten Tage und seine
Erkenntnisse. Sprich ihn mit „du“ an. Sei konkret statt allgemein: lieber ein handfester
Hinweis als drei Ratgeber-Sätze.

Deine Antwort wird vorgelesen. Schreib deshalb in ganzen Sätzen, ohne Aufzählungszeichen,
ohne Überschriften, ohne Klammern und ohne Sonderzeichen, die man nicht spricht. Halte dich
kurz — zwei bis sechs Sätze, außer er fragt ausdrücklich nach mehr.
""".trim()

        val eingabe = buildString {
            append(gedaechtnis.alsText())
            append("## DAS EXPERIMENT, UM DAS ES GEHT\n")
            append(experimentTitel).append('\n').append(experimentBeschreibung).append("\n\n")
            append("## BISHERIGES GESPRÄCH\n").append(faden).append("\n\n")
            append("## SEINE FRAGE JETZT\n").append(frage)
        }

        return basis(model, effort, anweisung, eingabe, "experimente_gespraech", schema)
    }

    // -----------------------------------------------------------------------
    // F-11 — die eigene Auswertung der KI
    // -----------------------------------------------------------------------

    fun auswertung(
        gedaechtnis: Gedaechtnis,
        experimentTitel: String,
        experimentBeschreibung: String,
        tagNummer: Int,
        tageGesamt: Int,
        abschliessend: Boolean,
        hakenStand: String,
        faden: String,
        verlauf: String,
        eigenerText: String,
        model: CodexModel,
        effort: ReasoningEffort,
    ): JSONObject {
        val schema = objekt("auswertung" to text())

        val anweisung = if (!abschliessend) {
            """
Das Experiment läuft noch (Tag $tagNummer von $tageGesamt). Frank hat gerade eingesprochen,
wie der Tag lief. Gib einen KURZEN Zwischenstand: zwei bis drei Sätze, mehr nicht. Kein
Fazit, keine Bewertung — nur was dir an diesem Tag auffällt und woran er morgen anknüpfen
kann. Sprich ihn mit „du“ an. Ganze Sätze, keine Aufzählung: der Text wird vorgelesen.
""".trim()
        } else {
            """
Das Experiment ist zu Ende. Schreib jetzt deine eigene Auswertung — deine Sicht darauf, nicht
eine Zusammenfassung seiner Worte.

Geh darauf ein:
- Was er tatsächlich gemacht hat und was er nicht gemacht hat.
- Wo er sich womöglich nicht getraut hat. Sag das freundlich, aber sag es.
- Was daran gut und positiv war — und zwar konkret, nicht als Lob-Floskel.
- Wie das Ganze in sein Gesamtbild passt: zu seinem Selbstbild, seinen Zielen, dem, was in
  den letzten Tagen war, und dem, was er schon über sich gelernt hat.

Bewerte NICHT mit Noten, Punkten, Prozenten oder „gut/schlecht“. Beschreibe, ordne ein,
benenne, was du siehst. „Nicht gemacht“ ist ein Ergebnis, kein Versagen.

Sprich ihn mit „du“ an. Ganze Sätze, keine Aufzählungszeichen, keine Überschriften — der
Text wird vorgelesen. Fünf bis zehn Sätze.
""".trim()
        }

        val eingabe = buildString {
            append(gedaechtnis.alsText())
            append("## DAS EXPERIMENT\n")
            append(experimentTitel).append('\n').append(experimentBeschreibung).append('\n')
            append("Tag $tagNummer von $tageGesamt\n\n")
            append("## SEINE AUFGABEN HEUTE\n").append(hakenStand).append("\n\n")
            append("## EUER GESPRÄCH DAZU\n").append(faden).append("\n\n")
            append("## FRÜHERE TAGE DIESES EXPERIMENTS\n").append(verlauf).append("\n\n")
            append("## WAS ER GERADE EINGESPROCHEN HAT\n").append(eigenerText)
        }

        return basis(model, effort, anweisung, eingabe, "experimente_auswertung", schema)
    }

    // -----------------------------------------------------------------------
    // F-14 — Logbuch fortschreiben (Logbuch-Modell)
    // -----------------------------------------------------------------------

    fun logbuch(
        bisher: String,
        neuesMaterial: String,
        model: CodexModel,
        effort: ReasoningEffort,
    ): JSONObject {
        val schema = objekt("eintrag" to text())
        val anweisung = """
Du führst Franks Logbuch. Schreib den Eintrag für den heutigen Tag fort: erfasse das Neue,
fasse es zusammen und trag es ein.

Das Logbuch ist ein Gedächtnis, kein Tagebuch für ihn — es wird später wieder eingelesen,
damit du weißt, wie seine Lage war. Schreib deshalb sachlich in der dritten Person („Frank
hat …", „ihm fiel auf …“), dicht und ohne Ausschmückung.

Nimm auf: seine Lage, welche Experimente laufen, was er getan und was er gelassen hat,
worüber ihr gesprochen habt, und seine Auswertungen.

UMFANG: 20 bis 30 Zeilen. Nicht kürzer, nicht länger.

Wenn schon ein Eintrag für heute existiert, ERGÄNZE und verdichte ihn zu einem einzigen
Text — leg keinen zweiten daneben und wiederhole nichts.
""".trim()

        val eingabe = buildString {
            append("## BISHERIGER EINTRAG VON HEUTE\n")
            append(bisher.ifBlank { "(noch keiner)" }).append("\n\n")
            append("## WAS SEITHER DAZUGEKOMMEN IST\n").append(neuesMaterial)
        }

        return basis(model, effort, anweisung, eingabe, "experimente_logbuch", schema)
    }

    // -----------------------------------------------------------------------
    // F-15 — einen Tag verdichten (Logbuch-Modell)
    // -----------------------------------------------------------------------

    fun verdichtung(
        datum: String,
        ausfuehrlich: String,
        model: CodexModel,
        effort: ReasoningEffort,
    ): JSONObject {
        val schema = objekt("verdichtet" to text())
        val anweisung = """
Dieser Tag ist älter als 15 Tage und wandert jetzt ins Langzeit-Gedächtnis. Verdichte ihn.

Behalte: das Datum, welches Experiment lief, wie Frank es durchgeführt hat, die Auswertung,
und die wichtigen Punkte — alles, was in einem Jahr noch etwas über ihn aussagt.
Wirf weg: Tagesrauschen, Wiederholungen, Nebensächliches.

UMFANG: höchstens 7 Zeilen. Stichpunktartig, nicht ausformuliert.

Es darf NICHTS verlorengehen, was später erklären könnte, warum ein Vorschlag zu ihm passt
oder nicht. Im Zweifel behalten.
""".trim()

        return basis(
            model, effort, anweisung,
            "## TAG $datum\n$ausfuehrlich",
            "experimente_verdichtung", schema,
        )
    }

    // -----------------------------------------------------------------------
    // F-17 — Erkenntnisse fortschreiben (Logbuch-Modell)
    // -----------------------------------------------------------------------

    fun erkenntnisse(
        bisherige: List<String>,
        neueAuswertung: String,
        model: CodexModel,
        effort: ReasoningEffort,
    ): JSONObject {
        val schema = objekt("erkenntnisse" to liste(text()))
        val anweisung = """
Aus Franks Auswertungen wächst eine Liste dessen, was er über sich gelernt hat: was ihm gut
tut, was weniger, wie er tickt.

Du bekommst die bisherige Liste und eine neue Auswertung. Gib die VOLLSTÄNDIGE neue Liste
zurück — ergänze höchstens einen Satz, oder schärfe einen vorhandenen, wenn die neue
Auswertung ihn genauer macht. Häng nicht stumpf an. Führe Doppelungen zusammen.

Jede Erkenntnis ist EIN Satz über Frank, in der dritten Person. Keine Bewertung seiner
Leistung, keine Ratschläge, keine Punkte. Beispiele für die Form:
„Frank zieht Vorhaben durch, die morgens beginnen, und lässt abends Begonnenes eher liegen.“
„Zwischenmenschliches meidet er, wenn der Tag ohnehin voll ist.“

Bringt die neue Auswertung nichts Neues, gib die Liste unverändert zurück.
""".trim()

        val eingabe = buildString {
            append("## BISHERIGE LISTE\n")
            append(if (bisherige.isEmpty()) "(noch leer)" else bisherige.joinToString("\n") { "- $it" })
            append("\n\n## NEUE AUSWERTUNG\n").append(neueAuswertung)
        }

        return basis(model, effort, anweisung, eingabe, "experimente_erkenntnisse", schema)
    }

    // -----------------------------------------------------------------------
    // F-18 — eine eigene Idee zu einem Experiment ausbauen
    // -----------------------------------------------------------------------

    fun eigeneIdee(
        idee: String,
        gedaechtnis: Gedaechtnis,
        model: CodexModel,
        effort: ReasoningEffort,
    ): JSONObject {
        val schema = objekt(
            "titel" to text(),
            "beschreibung" to text(),
            "tage" to zahl(),
            "stufe" to JSONObject().put("type", "string")
                .put("enum", JSONArray().put("leicht").put("mittel").put("fordernd")),
            "aufgaben_je_tag" to liste(liste(text())),
        )
        val anweisung = """
Frank hat eine eigene Experiment-Idee eingesprochen. Mach daraus einen Eintrag für seine
Merkliste — mit Titel, Beschreibung, Dauer, Stufe und den konkreten Schritten.

Ändere seine Idee NICHT. Du formst sie nur aus: Was hat er gemeint, was wären die Schritte,
wie viele Tage braucht das, wie fordernd ist es für ihn? Die Stufe schätzt du aus dem, was
du über ihn weißt.

Sprich ihn in der Beschreibung mit „du“ an. Zwei bis fünf Schritte je Tag.
""".trim()

        val eingabe = "## SEINE IDEE\n$idee\n\n${gedaechtnis.alsText()}"
        return basis(model, effort, anweisung, eingabe, "experimente_eigene_idee", schema)
    }
}
