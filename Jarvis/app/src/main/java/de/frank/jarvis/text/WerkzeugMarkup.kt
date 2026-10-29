package de.frank.jarvis.text

import org.json.JSONArray
import org.json.JSONObject

/**
 * Werkzeugaufrufe, die ein Modell als Text statt im vereinbarten JSON-Format schreibt: Claude-Markup
 * (`<invoke name="…"><parameter name="…">…</parameter></invoke>`), `<tool_call>{…}</tool_call>` oder
 * JSON wie `{"name": "…", "arguments": {…}}`. [aufrufe] macht daraus echte Aufrufe, [entferne] hält solches
 * Markup aus jeder Antwort und jedem Vorlesetext heraus.
 */
object WerkzeugMarkup {

    private val INVOKE = Regex("""<(?:[\w-]+:)?invoke\s+name\s*=\s*["']([^"']+)["']\s*/?>([\s\S]*?)(?:</(?:[\w-]+:)?invoke>|$)""", RegexOption.IGNORE_CASE)
    private val PARAMETER = Regex("""<(?:[\w-]+:)?parameter\s+name\s*=\s*["']([^"']+)["'][^>]*>([\s\S]*?)</(?:[\w-]+:)?parameter>""", RegexOption.IGNORE_CASE)
    private val BLOECKE = Regex(
        """<(?:[\w-]+:)?(function_calls|invoke|parameter|tool_call|tool_use|function_call|tool_result|function_results|thinking)\b[^>]*>[\s\S]*?</(?:[\w-]+:)?\1>""",
        RegexOption.IGNORE_CASE,
    )
    /** Übrige Tags, auch abgeschnittene am Ende. Ein „<“ ohne Buchstaben dahinter („< 5“, „<3“) bleibt. */
    private val TAG = Regex("""</?[A-Za-z][\w:-]*(?:\s[^<>]*)?(?:>|$)""")
    /** Attribute und Zuweisungen wie name="einstellungen" oder kennung=wert. */
    private val ZUWEISUNG = Regex("""\b[\w-]+\s*=\s*(?:"[^"]*"|'[^']*'|[\w.,:-]+)""")
    /** Zeilen, die nur einen Aufruf zeigen: „→ einstellungen(…)“, „-> aendern {…}“. */
    private val PFEIL_ZEILE = Regex("""(?m)^[ \t]*(?:→|->|=>|⇒)[ \t]*[\w.-]+[ \t]*[({:].*$""")
    private val MERKMALE = Regex(
        """<(?:[\w-]+:)?(?:invoke|parameter|function_calls|tool_call|tool_use)\b|</(?:[\w-]+:)?(?:invoke|parameter)>|\bname\s*=\s*["']|"(?:werkzeug|werkzeuge|argumente|arguments|parameters)"\s*:""",
        RegexOption.IGNORE_CASE,
    )

    /** {"name": …, "arguments"/"input"/"parameters": …} wie bei nativen Aufrufen anderer Anbieter. */
    private val NAME_JSON = Regex("""^\{\s*"(?:name|tool|function|tool_calls)"\s*:""")

    /** true = der Text enthält etwas, das nach Werkzeugaufruf oder Markup aussieht. */
    fun enthaelt(text: String): Boolean = MERKMALE.containsMatchIn(text)

    /**
     * Die als Text geschriebenen Aufrufe bekannter Werkzeuge, je als {"werkzeug": …, "argumente": {…}};
     * unbekannte Namen fallen weg, damit kein beliebiger Text als Aufruf gilt.
     */
    fun aufrufe(roh: String, bekannt: Set<String>): List<JSONObject> {
        val liste = mutableListOf<JSONObject>()
        INVOKE.findAll(roh).forEach { m ->
            val name = m.groupValues[1].trim()
            if (name !in bekannt) return@forEach
            val argumente = JSONObject()
            PARAMETER.findAll(m.groupValues[2]).forEach { p -> argumente.put(p.groupValues[1].trim(), wert(p.groupValues[2])) }
            liste += JSONObject().put("werkzeug", name).put("argumente", argumente)
        }
        if (liste.isNotEmpty()) return liste
        jsonBloecke(roh).forEach { (start, ende) ->
            val o = runCatching { JSONObject(roh.substring(start, ende + 1)) }.getOrNull() ?: return@forEach
            val kandidaten = o.optJSONArray("tool_calls")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: listOf(o)
            kandidaten.forEach { k ->
                val f = k.optJSONObject("function") ?: k
                val name = (f.optString("name").ifBlank { f.optString("tool") }).trim()
                if (name !in bekannt) return@forEach
                val arg = listOf("arguments", "argumente", "parameters", "input", "args").firstNotNullOfOrNull { schluessel ->
                    f.optJSONObject(schluessel) ?: f.optString(schluessel).takeIf { it.startsWith("{") }?.let { runCatching { JSONObject(it) }.getOrNull() }
                } ?: JSONObject()
                liste += JSONObject().put("werkzeug", name).put("argumente", arg)
            }
        }
        return liste
    }

    /** Entfernt Werkzeug-Markup, Tags, JSON und Zuweisungen; Klartext bleibt unverändert stehen. */
    fun entferne(text: String): String {
        if (!enthaelt(text) && !TAG.containsMatchIn(text) && '{' !in text && !PFEIL_ZEILE.containsMatchIn(text)) return text
        var t = BLOECKE.replace(text, " ")
        t = INVOKE.replace(t, " ")
        // Nur JSON, das nach Werkzeugaufruf aussieht; erbetenes JSON in einer Antwort bleibt stehen.
        val bloecke = jsonBloecke(t).filter { (s, e) -> MERKMALE.containsMatchIn(t.substring(s, e + 1)) || NAME_JSON.containsMatchIn(t.substring(s, e + 1)) }
        bloecke.asReversed().forEach { (s, e) -> t = t.removeRange(s, e + 1) }
        t = PFEIL_ZEILE.replace(t, "")
        t = TAG.replace(t, " ")
        if (enthaelt(text)) t = ZUWEISUNG.replace(t, " ")
        return t.replace(Regex("[ \\t]{2,}"), " ").replace(Regex("\\n{3,}"), "\n\n").replace(Regex("(?m)^[ \\t]+|[ \\t]+$"), "").trim()
    }

    private fun wert(roh: String): Any {
        val w = roh.trim()
        if (w.startsWith("[")) runCatching { return JSONArray(w) }
        if (w.startsWith("{")) runCatching { return JSONObject(w) }
        return w
    }

    /** Anfang und Ende jedes äußersten {…}-Blocks, Klammern in Zeichenketten zählen nicht. */
    private fun jsonBloecke(t: String): List<Pair<Int, Int>> {
        val ergebnis = mutableListOf<Pair<Int, Int>>()
        var tiefe = 0
        var start = -1
        var inText = false
        var i = 0
        while (i < t.length) {
            val c = t[i]
            when {
                inText && c == '\\' -> i++
                c == '"' && tiefe > 0 -> inText = !inText
                inText -> {}
                c == '{' -> { if (tiefe == 0) start = i; tiefe++ }
                c == '}' && tiefe > 0 -> { tiefe--; if (tiefe == 0) ergebnis += start to i }
            }
            i++
        }
        return ergebnis
    }
}
