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

internal const val VERBESSERUNG_AUFTRAG =
    "Bringe den folgenden Text in Ordnung: Rechtschreibung, Zeichensetzung, Satzbau. Entferne " +
        "Füllwörter und Verhaspler des Sprechens („äh\", „also\", doppelte Satzanfänge). Setze " +
        "Absätze, wo der Gedanke wechselt. ÄNDERE NICHT DEN INHALT: erfinde nichts hinzu, lasse " +
        "nichts weg, deute nichts um, ziehe keine Schlüsse. Antworte ausschließlich mit dem " +
        "verbesserten Text — ohne Vorrede, ohne Anführungszeichen, ohne Kommentar."

/**
 * Der feste Teil des Auswertungsauftrags. Die Machart und die Länge kommen aus dem aktiven
 * Auswertungsprofil (F-10) und werden dahinter gehängt — deshalb steht hier nur, was
 * unabhängig vom Profil gilt.
 */
internal const val AUSWERTUNG_GRUNDAUFTRAG =
    "Du bekommst den vollständigen Sitzungsverlauf einer Person aus Notizen und früheren " +
        "KI-Dialogen, dazu die Rückfrage, die du vorher gestellt hast, und ihre Antwort darauf. " +
        "Werte den gesamten Verlauf im Licht dieser Antwort aus.\n\n" +
        "Aufbau der Antwort, unabhängig von allem Weiteren:\n" +
        "— Schreibe in Absätzen. Jeder Absatz umfasst 6 bis 15 Zeilen und steht durch eine " +
        "Leerzeile vom nächsten getrennt. Das ist keine Formsache: die Antwort wird Absatz für " +
        "Absatz vorgelesen, und ein einzelner Block ohne Leerzeilen lässt sich nicht vorlesen.\n" +
        "— Keine Aufzählungszeichen, keine Überschriften, keine Nummerierung, kein Markdown. " +
        "Fließtext.\n" +
        "— Keine Einleitung darüber, was du gleich tun wirst, und keine Zusammenfassung am " +
        "Ende darüber, was du getan hast.\n" +
        "— Beziehe dich auf das, was wirklich im Verlauf steht. Erfinde keine Tatsachen dazu."

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
    model: CodexModel,
    effort: ReasoningEffort,
): JSONObject = JSONObject()
    .put("model", model.apiId)
    .put("service_tier", "priority")
    .put("stream", true)
    .put("store", false)
    .put("instructions", VERBESSERUNG_AUFTRAG)
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
