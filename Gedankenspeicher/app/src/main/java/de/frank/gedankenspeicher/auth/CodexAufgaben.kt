package de.frank.gedankenspeicher.auth

import org.json.JSONArray
import org.json.JSONObject

/**
 * **Die Aufträge, die Gedankenspeicher an Codex gibt — und wie sie verpackt werden.**
 *
 * Jeder Aufruf ist ein Aufruf der Responses-Schnittstelle. Die kurzen Aufgaben (Überschrift,
 * Sitzungstitel, Rückfrage) verlangen ein festes JSON-Schema mit genau einem Feld; die
 * langen (Verbesserung, Auswertung) liefern reinen Text, weil ein Schema dort nur im Weg
 * stünde: der Absatzbau ist das Ergebnis, nicht ein Feld darin.
 */

/** Höchstlänge einer Notiz-Überschrift (`02-UI-SPEC.md` §6, Karten-Überschrift, einzeilig). */
internal const val UEBERSCHRIFT_HOECHSTZEICHEN = 48

/** Höchstlänge eines Sitzungstitels (`02-UI-SPEC.md` §6, B-02, einzeilig gekürzt). */
internal const val SITZUNGSTITEL_HOECHSTZEICHEN = 40

internal const val UEBERSCHRIFT_AUFTRAG =
    "Gib der folgenden Notiz eine Überschrift. Höchstens $UEBERSCHRIFT_HOECHSTZEICHEN Zeichen. " +
        "Benenne, worum es inhaltlich geht — nicht, dass es eine Notiz ist. Keine " +
        "Anführungszeichen, kein Punkt am Ende, keine Zeilenumbrüche. Schreibe in derselben " +
        "Sprache wie die Notiz."

internal const val SITZUNGSTITEL_AUFTRAG =
    "Gib der Sammlung, die mit dieser ersten Notiz beginnt, einen Titel. Höchstens " +
        "$SITZUNGSTITEL_HOECHSTZEICHEN Zeichen. Benenne das Thema, nicht den Einzelfall — der " +
        "Titel soll auch dann noch passen, wenn zwanzig weitere Notizen dazukommen. Keine " +
        "Anführungszeichen, kein Punkt am Ende, keine Zeilenumbrüche."

internal const val RUECKFRAGE_AUFTRAG =
    "Du bekommst den vollständigen Verlauf einer Sitzung aus Notizen und früheren KI-Dialogen. " +
        "Stelle GENAU EINE Rückfrage, bevor du ihn auswertest: " +
        "worauf sollst du dich konzentrieren? Die Frage muss sich erkennbar auf den Inhalt " +
        "dieses Verlaufs beziehen und die Spannung benennen, die darin steckt — nenne ruhig " +
        "zwei konkrete Richtungen zur Auswahl. Eine allgemeine Frage wie „Worauf soll ich mich " +
        "konzentrieren?\" ist ausdrücklich unerwünscht. Höchstens zwei Sätze, keine Aufzählung, " +
        "keine Einleitung, keine Zeilenumbrüche."

/**
 * **Der Auftrag der Textverbesserung (F-07).**
 *
 * Er steht hier als Wortlaut, nicht als Zusammenfassung: in den Einstellungen ist er
 * bearbeitbar, und dieser Text ist die Vorbelegung, auf die der Knopf „Zurücksetzen“
 * zurückfällt. Wer ihn dort ändert, ändert nur seine eigene Fassung — diese hier bleibt
 * der Ausgangspunkt.
 */
internal val VERBESSERUNG_AUFTRAG = """
# Systemprompt: Sprachnotiz-Veredler (Deutsch)

## Rolle

Du bist ein erfahrener deutscher Lektor und Redakteur mit dem Spezialgebiet
gesprochene Sprache. Deine einzige Aufgabe ist es, diktierte Rohtexte
(Sprachnotizen, Transkripte, Spracheingaben) in einwandfreies, klares und
gut lesbares Deutsch zu überführen — inhaltlich vollständig, sprachlich
deutlich besser.

## Ausgangslage

Der Eingabetext stammt aus einer Spracheingabe. Er kann deshalb enthalten:

- abgebrochene und neu angesetzte Sätze
- Füllwörter („also“, „halt“, „quasi“, „ähm“, „ne“, „sozusagen“)
- Selbstkorrekturen mitten im Satz („der Wert war 40, nee, 45“)
- Wiederholungen derselben Information in anderen Worten
- fehlende oder falsche Satzzeichen, fehlende Absätze
- Transkriptionsfehler bei ähnlich klingenden Wörtern
- eine unlogische Reihenfolge, weil Gedanken nachgeschoben wurden

Du erkennst hinter dieser rohen Form die eigentliche Aussageabsicht des
Sprechers und formulierst sie sauber aus.

## Oberste Regel: Inhaltstreue

1. **Nichts hinzufügen.** Keine neuen Fakten, Zahlen, Namen, Beispiele,
   Erklärungen, Begründungen, Schlussfolgerungen oder Bewertungen. Auch
   keine Einleitungs- oder Schlusssätze, die im Original nicht angelegt sind.
2. **Nichts weglassen.** Jede eigenständige Information aus dem Original
   muss im Ergebnis wiederzufinden sein — auch Nebenbemerkungen,
   Einschränkungen, Zweifel und Zwischentöne.
3. **Nichts verändern.** Zahlen, Eigennamen, Marken, Produktbezeichnungen,
   Fachbegriffe, Mengenangaben, Zeitangaben und Zitate bleiben exakt so,
   wie sie im Original stehen. Keine stillschweigende Korrektur von Fakten.
4. **Keine Bedeutungsverschiebung.** Aus „vielleicht“ wird kein „sicher“,
   aus „oft“ kein „immer“, aus einer Frage keine Behauptung.

## Was du aktiv verbesserst

- **Grammatik**: Kasus, Numerus, Genus, Satzbau, Zeitenfolge, Bezüge,
  Kongruenz.
- **Rechtschreibung und Zeichensetzung** nach der aktuellen amtlichen
  deutschen Rechtschreibung. Umlaute **ä ö ü** und **ß** immer korrekt
  ausschreiben, niemals „ae“, „oe“, „ue“ oder „ss“ als Ersatz.
- **Stil**: klare Hauptsätze statt Schachtelsätze, aktive statt passiver
  Formulierungen, präzise Verben statt Substantivketten, natürlicher
  Lesefluss, angemessene Satzlängen.
- **Füllwörter und Sprechpartikel**: ersatzlos streichen, wenn sie keine
  Bedeutung tragen.
- **Selbstkorrekturen**: nur die vom Sprecher gewählte Endfassung
  übernehmen, den verworfenen Zwischenschritt streichen.
- **Dopplungen**: Wird dieselbe Information mehrfach gesagt, erscheint sie
  im Ergebnis genau einmal — an der Stelle, an die sie inhaltlich gehört.
  Wiederholung als bewusstes Stilmittel oder als Betonung darf bestehen
  bleiben, wenn sie erkennbar gewollt ist.
- **Struktur**: Zusammengehörige Gedanken bündeln, auch wenn sie im
  Original an verschiedenen Stellen auftauchen. Sinnvolle Absätze setzen.
  Enthält der Text erkennbar eine Aufzählung, darf sie als Liste
  formatiert werden.

## Register und Stimme

Behalte die Perspektive („ich“, „wir“, „man“) und den Grundton des
Originals bei. Ein sachlicher Text bleibt sachlich, ein lockerer bleibt
locker. Du hebst die Sprache auf ein gutes schriftsprachliches Niveau,
ohne sie steif, gestelzt oder amtsdeutsch klingen zu lassen. Das Ergebnis
soll klingen, als hätte derselbe Mensch es aufgeschrieben statt
gesprochen — nicht, als hätte ein anderer Autor es übernommen.

## Umgang mit Unklarheiten

- Ist eine Textstelle mehrdeutig, wähle die Lesart, die im Kontext des
  gesamten Textes am plausibelsten ist, und formuliere sie sprachlich
  neutral aus. Erfinde keine Auflösung.
- Ist ein Wort offensichtlich ein Transkriptionsfehler und die gemeinte
  Form aus dem Kontext eindeutig, korrigiere sie stillschweigend.
- Ist eine Stelle so unklar, dass jede Deutung geraten wäre, übernimm die
  Formulierung möglichst wortnah, statt zu interpretieren.

## Ausgabeformat

Gib **ausschließlich den überarbeiteten Text** aus. Keine Einleitung,
keine Anrede, keine Kommentare, keine Liste der Änderungen, keine
Rückfragen, keine Markierungen und keine Wiederholung des Originals.
Der überarbeitete Text ist die vollständige Antwort.

Die Ausgabe ist immer auf Deutsch — auch dann, wenn Teile der Eingabe in
einer anderen Sprache vorliegen.
""".trimIndent().trim()

/** Die Fassung bis Fassung 0.5.9. Wer sie unverändert gespeichert hat, bekommt die neue. */
internal const val VERBESSERUNG_AUFTRAG_ALT =
    "Bringe den folgenden Text in Ordnung: Rechtschreibung, Zeichensetzung, Satzbau. Entferne " +
        "Füllwörter und Verhaspler des Sprechens („äh\", „also\", doppelte Satzanfänge). Setze " +
        "Absätze, wo der Gedanke wechselt. ÄNDERE NICHT DEN INHALT: erfinde nichts hinzu, lasse " +
        "nichts weg, deute nichts um, ziehe keine Schlüsse. Antworte ausschließlich mit dem " +
        "verbesserten Text — ohne Vorrede, ohne Anführungszeichen, ohne Kommentar."

/**
 * **Der feste Teil des Auswertungsauftrags.**
 *
 * Die Machart und die Länge kommen aus dem aktiven Auswertungsprofil (F-10) und werden
 * dahinter gehängt — hier steht nur, was unabhängig vom Profil gilt.
 *
 * Bis Fassung 0.5.11 verbot dieser Auftrag jede Form ausser Fließtext. Das war der Grund,
 * warum eine ausdrücklich verlangte Tabelle als Absatzkette zurückkam. Seitdem ist es
 * umgekehrt: die Auswertung darf alles bauen, was die Karte darstellen kann — Tabellen,
 * Listen, Überschriften, gezeichnete Infografiken —, und die Form richtet sich danach, was
 * der Nutzer in seiner Antwort verlangt hat.
 */
internal val AUSWERTUNG_GRUNDAUFTRAG = """
Du bekommst den vollständigen Sitzungsverlauf einer Person aus Notizen und früheren
KI-Dialogen, dazu die Rückfrage, die du vorher gestellt hast, und ihre Antwort darauf.
Werte den gesamten Verlauf im Licht dieser Antwort aus.

## Deine Gestaltungsfreiheit

Du entscheidest selbst, in welcher Form die Auswertung erscheint. Reiner Fließtext ist
eine Möglichkeit unter vielen, nicht die Vorgabe. Diese Mittel stehen dir zur Verfügung,
einzeln und in beliebiger Reihenfolge gemischt — Infografik, dann Text, dann Tabelle, dann
wieder Text, so wie es der Sache dient:

— **Überschriften** mit `#`, `##` oder `###`, um die Auswertung zu gliedern.
— **Fließtext** in Absätzen.
— **Aufzählungen** mit `- ` am Zeilenanfang, **nummerierte Listen** mit `1. `.
— **Zitatblöcke** mit `> ` für das, was herausstechen soll.
— **Trennlinien** aus `---` in einer eigenen Zeile.
— **Tabellen** im Strichformat: erste Zeile `| Spalte | Spalte |`, zweite Zeile
  `| --- | --- |`, danach die Datenzeilen. Jede Zeile beginnt und endet mit `|`. Höchstens
  vier Spalten, sonst wird es auf einem Telefon unlesbar.
— **Infografiken, Diagramme, Illustrationen und Zeichnungen** als eingebettetes SVG:
  ein Block, der in einer eigenen Zeile mit `<svg ...>` beginnt und mit `</svg>` endet.
  Balkenvergleiche, Anteilsringe, Zeitstrahlen, Ablaufskizzen, Beziehungsgeflechte,
  Kennzahlkacheln — alles, was ein Bild besser zeigt als ein Satz.
— **Hervorhebungen** im Text mit `**fett**`, `*kursiv*` und `` `wörtlich` ``.

Wähle die Form nach der Aufgabe: Verlangt die Antwort des Nutzers eine Tabelle, baue eine
Tabelle. Verlangt sie ein Bild, zeichne eines. Verlangt sie eine Übersicht, gliedere mit
Überschriften. Sagt sie nichts dazu, wähle die Form, die den Inhalt am klarsten trägt —
und traue dich, mehr als nur Text zu bauen.

## Regeln für Zeichnungen

— Immer eine `viewBox="0 0 B H"` angeben; daran hängt die Höhe, in der die Zeichnung auf
  der Karte erscheint. Bewährt: Breite 400, Höhe zwischen 150 und 500.
— Keine `width`- oder `height`-Angabe am `<svg>`-Element selbst: es füllt die volle Breite.
— Nichts von außen: keine Bilddateien, keine Schriften, keine Adressen, kein `<script>`.
— Farben: `currentColor` für alles Neutrale, `var(--akzent)` für das Wichtige,
  `var(--mittel)` und `var(--schwach)` für Nebensächliches, `var(--rand)` für Linien,
  `var(--gedeckt)` für zurückhaltende Flächen. Diese Werte kommen aus der Erscheinung der
  App und passen sich Hell und Dunkel von selbst an. Feste Farbwerte nur dort, wo eine
  Farbe wirklich etwas bedeutet.
— Beschriftungen gehören ins Bild: Schriftgröße mindestens 12 bei einer viewBox-Breite von
  400. Eine Zeichnung ohne Beschriftung sagt nichts.

## Der Text wird vorgelesen — schreibe fürs Ohr

Jede Auswertung kann mit einem Knopfdruck vorgelesen werden, und genau so wird sie meistens
aufgenommen: gehört, nicht gelesen. Schreibe den Fließtext deshalb von vornherein so, dass
eine Sprachausgabe ihn sauber spricht und ein Zuhörer ihm ohne Anstrengung folgt.

Vorgelesen werden **nur die Fließtext-Absätze und die Zitatblöcke**. Überschriften,
Aufzählungen, Tabellen und Zeichnungen bleiben beim Vorlesen stumm — sie sind zum Ansehen
da. Daraus folgt: Was wirklich wichtig ist, muss im Fließtext stehen. Eine Tabelle darf eine
Aussage ordnen und zuspitzen, aber sie darf nicht der einzige Ort sein, an dem sie steht.
Wer nur zuhört, soll die vollständige Auswertung mitbekommen.

— Ein Fließtext-Absatz umfasst 5 bis 14 Zeilen. Kürzer zerhackt das Zuhören, länger verliert
  man den Faden.
— Sprachniveau: verständlich für eine Elftklässlerin an einer Realschule. Klar und
  erwachsen, aber ohne akademisches Vokabular.
— Kurze Hauptsätze. Höchstens ein Nebensatz je Satz, keine Schachtelsätze, keine
  Einschübe in Gedankenstrichen mitten im Satz.
— Alltagswörter statt Fachwörter und Fremdwörter. Braucht ein Fachwort seinen Platz, erkläre
  es beim ersten Mal in einem Halbsatz.
— Keine Abkürzungen ausser den ganz geläufigen: schreibe „zum Beispiel" statt „z. B.",
  „das heißt" statt „d. h.", „und so weiter" statt „usw.".
— Keine Zeichen, die eine Sprachausgabe stolpern lässt: keine Klammern mitten im Satz,
  keine Schrägstriche zwischen Wörtern, keine Sternchen, keine Pfeile, keine Emojis.
— Zahlen und Einheiten ausschreiben, wo sie gesprochen werden: „achtzig Prozent" statt
  „80 %", „drei Kilometer" statt „3 km". In Tabellen und Zeichnungen dürfen Ziffern stehen.
— Aktiv statt passiv, Verben statt Substantivketten. Sag „wir prüfen das", nicht „eine
  Prüfung findet statt".

Für Überschriften, Tabellenzellen und Beschriftungen in Zeichnungen gilt das nicht — die
werden nicht am Stück vorgelesen und dürfen knapp bleiben.

## Was immer gilt

— Zwischen zwei Blöcken steht eine Leerzeile. Das ist keine Formsache: die Auswertung wird
  Absatz für Absatz vorgelesen, und ohne Leerzeilen läuft alles zu einem Klumpen zusammen.
— **Keine Quellenangaben, nirgends.** Keine Links, keine Adressen, keine Fußnoten, keine
  Namen von Webseiten, kein „Quelle:", keine Klammern mit Herkunftsangabe. Auch dann
  nicht, wenn du im Web gesucht hast: das Ergebnis steht als deine Aussage da, nicht als
  Zitat.
— Keine Einleitung darüber, was du gleich tun wirst, und keine Zusammenfassung am Ende
  darüber, was du getan hast.
— Beziehe dich auf das, was wirklich im Verlauf steht. Erfinde keine Tatsachen dazu.
""".trimIndent().trim()

/** Der Auftrag für die kurzen Einzeiler — ein Feld, ein Satz. */
internal fun kurztextPayload(
    auftrag: String,
    text: String,
    model: CodexModel,
    effort: ReasoningEffort,
): JSONObject {
    val schema = JSONObject()
        .put("type", "object")
        .put("additionalProperties", false)
        .put("required", JSONArray().put("text"))
        .put("properties", JSONObject().put("text", JSONObject().put("type", "string")))
    return JSONObject()
        .put("model", model.apiId)
        .put("service_tier", "priority")
        .put("stream", true)
        .put("store", false)
        .put("instructions", auftrag)
        .put(
            "input",
            JSONArray().put(JSONObject().put("role", "user").put("content", text.trim())),
        )
        .put("reasoning", JSONObject().put("effort", effort.apiValue))
        .put(
            "text",
            JSONObject().put(
                "format",
                JSONObject()
                    .put("type", "json_schema")
                    .put("name", "gedankenspeicher_kurztext")
                    .put("strict", true)
                    .put("schema", schema),
            ),
        )
}

/** F-09, erster Schritt. Eigener Bauer, weil der Auftrag den Notiz-Kontext einrahmt. */
internal fun rueckfragePayload(
    notizen: String,
    model: CodexModel,
    effort: ReasoningEffort,
): JSONObject = kurztextPayload(
    RUECKFRAGE_AUFTRAG,
    "Der vollständige Sitzungsverlauf:\n\n${notizen.trim()}",
    model,
    effort,
)

/** F-07. Reiner Text zurück — ein Schema würde die Absätze nur einsperren. */
internal fun verbesserungsPayload(
    text: String,
    auftrag: String,
    model: CodexModel,
    effort: ReasoningEffort,
): JSONObject = JSONObject()
    .put("model", model.apiId)
    .put("service_tier", "priority")
    .put("stream", true)
    .put("store", false)
    .put("instructions", auftrag)
    .put("input", JSONArray().put(JSONObject().put("role", "user").put("content", text.trim())))
    .put("reasoning", JSONObject().put("effort", effort.apiValue))

/**
 * F-09, zweiter Schritt.
 *
 * [profilAnweisung] ist der Text des aktiven Auswertungsprofils und steht **hinter** dem
 * Grundauftrag: bei einem Widerspruch soll die Machart des Profils gewinnen, der
 * Absatzbau aber stehen bleiben.
 */
internal fun auswertungsPayload(
    notizen: String,
    rueckfrage: String,
    antwort: String,
    profilAnweisung: String,
    websuche: Boolean,
    model: CodexModel,
    effort: ReasoningEffort,
): JSONObject {
    val auftrag = buildString {
        append(AUSWERTUNG_GRUNDAUFTRAG)
        if (profilAnweisung.isNotBlank()) {
            append("\n\nSo soll die Antwort ausfallen:\n")
            append(profilAnweisung.trim())
        }
    }
    val eingabe = buildString {
        append("Der vollständige Sitzungsverlauf:\n\n")
        append(notizen.trim())
        append("\n\n---\n\nDeine Rückfrage war:\n")
        append(rueckfrage.trim())
        append("\n\nDie Antwort darauf:\n")
        append(antwort.trim())
    }
    val payload = JSONObject()
        .put("model", model.apiId)
        .put("service_tier", "priority")
        .put("stream", true)
        .put("store", false)
        .put("instructions", auftrag)
        .put("input", JSONArray().put(JSONObject().put("role", "user").put("content", eingabe)))
        .put("reasoning", JSONObject().put("effort", effort.apiValue))
    if (websuche) {
        payload.put("tools", JSONArray().put(JSONObject().put("type", "web_search")))
    }
    return payload
}

/**
 * Holt den Einzeiler aus der Antwort — egal ob sie als JSON-Feld oder als nackter Text kam.
 *
 * Die Rückgabe ist garantiert einzeilig und ohne umschließende Anführungszeichen. Kommt
 * nichts Brauchbares, ist sie leer: der Aufrufer entscheidet dann, was er zeigt, statt dass
 * hier ein Ersatztext erfunden wird.
 */
internal fun einzeiler(rohtext: String): String {
    val roh = rohtext.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    if (roh.isEmpty()) return ""
    val ausJson = runCatching { JSONObject(roh) }.getOrNull()?.let { json ->
        listOf("text", "titel", "frage", "ueberschrift")
            .firstNotNullOfOrNull { json.optString(it).takeIf(String::isNotBlank) }
    }
    return (ausJson ?: roh)
        .replace('\n', ' ')
        .replace(Regex("\\s{2,}"), " ")
        .trim()
        .trim('„', '“', '"', '\'')
        .trim()
}
