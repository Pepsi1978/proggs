package de.frank.jarvis.text

import org.json.JSONArray
import org.json.JSONObject

/**
 * Werkzeugaufrufe, die ein Modell als Text statt im vereinbarten JSON-Format schreibt: Claude-Markup
 * (`<invoke name="…"><parameter name="…">…</parameter></invoke>`), `<tool_call>{…}</tool_call>` oder
 * JSON wie `{"name": "…", "arguments": {…}}`. [aufrufe] macht daraus echte Aufrufe, [entferne] hält solches
 * Markup aus jeder Antwort und jedem Vorlesetext heraus. Beides greift nur bei Werkzeug-Markup: Code,
 * spitze Klammern im Satz, erbetenes JSON und Pfeil-Listen bleiben unangetastet.
 */
object WerkzeugMarkup {

    private const val NS = """(?:[\w-]+:)?"""
    private const val WERKZEUG_TAGS = "function_calls|invoke|parameter|tool_call|tool_use|function_call|tool_result|function_results"

    /** Ein Aufruf: selbstschließend, geschlossen oder offen bis zum nächsten invoke bzw. zum Textende. */
    private val INVOKE = Regex(
        """<${NS}invoke\s+name\s*=\s*["']([^"']+)["']\s*(?:/>|>([\s\S]*?)(?:</${NS}invoke>|(?=<${NS}invoke\b)|$))""",
        RegexOption.IGNORE_CASE,
    )
    private val PARAMETER = Regex("""<${NS}parameter\s+name\s*=\s*["']([^"']+)["'][^>]*>([\s\S]*?)</${NS}parameter>""", RegexOption.IGNORE_CASE)
    /** Geschlossene Werkzeug-Blöcke samt Inhalt. */
    private val BLOECKE = Regex("""<$NS($WERKZEUG_TAGS)\b[^>]*>[\s\S]*?</$NS\1>""", RegexOption.IGNORE_CASE)
    /** Übrig gebliebene einzelne Werkzeug-Tags; nur das Tag selbst, nie der Text dahinter. */
    private val EINZELTAG = Regex("""</?$NS(?:$WERKZEUG_TAGS)\b[^<>\n]*>""", RegexOption.IGNORE_CASE)
    private val MERKMALE = Regex("""</?$NS(?:$WERKZEUG_TAGS)\b""", RegexOption.IGNORE_CASE)
    /** JSON-Schlüssel, die nur in Aufrufen vorkommen. */
    private val AUFRUF_SCHLUESSEL = Regex(""""(?:werkzeug|werkzeuge|argumente|arguments|tool_calls)"\s*:""")
    private val NAME_SCHLUESSEL = Regex("""^\{\s*"(?:name|tool|function)"\s*:""")
    private val ARGUMENT_SCHLUESSEL = Regex(""""(?:arguments|argumente|input)"\s*:""")
    private val LEERRAUM = Regex("[ \\t]{2,}")
    private val LEERZEILEN = Regex("\\n{3,}")

    /** true = der Text enthält Werkzeug-Tags oder Aufruf-JSON. */
    fun enthaelt(text: String): Boolean =
        MERKMALE.containsMatchIn(text) || ('{' in text && jsonBloecke(text).any { (s, e) -> istAufruf(text.substring(s, e + 1)) })

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
            val kandidaten = o.optJSONArray("tool_calls")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }
                ?: o.optJSONArray("werkzeuge")?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }
                ?: listOf(o)
            kandidaten.forEach { k ->
                val f = k.optJSONObject("function") ?: k
                val name = listOf("werkzeug", "name", "tool").firstNotNullOfOrNull { f.optString(it).trim().ifEmpty { null } }.orEmpty()
                if (name !in bekannt) return@forEach
                val arg = listOf("argumente", "arguments", "parameters", "input", "args").firstNotNullOfOrNull { schluessel ->
                    f.optJSONObject(schluessel) ?: f.optString(schluessel).takeIf { it.startsWith("{") }?.let { runCatching { JSONObject(it) }.getOrNull() }
                } ?: JSONObject()
                liste += JSONObject().put("werkzeug", name).put("argumente", arg)
            }
        }
        return liste
    }

    /** Entfernt Werkzeug-Tags samt Inhalt und Aufruf-JSON; ohne solches Markup bleibt der Text Zeichen für Zeichen gleich. */
    fun entferne(text: String): String {
        if (!enthaelt(text)) return text
        var t = BLOECKE.replace(text, " ")
        t = EINZELTAG.replace(t, " ")
        jsonBloecke(t).filter { (s, e) -> istAufruf(t.substring(s, e + 1)) }.asReversed().forEach { (s, e) -> t = t.removeRange(s, e + 1) }
        return LEERZEILEN.replace(LEERRAUM.replace(t, " "), "\n\n").trim()
    }

    /** Aufruf-JSON: eigene Aufrufschlüssel oder name/tool/function zusammen mit arguments/input. Erbetenes JSON wie {"name": "Frank"} bleibt. */
    private fun istAufruf(block: String): Boolean =
        AUFRUF_SCHLUESSEL.containsMatchIn(block) || (NAME_SCHLUESSEL.containsMatchIn(block) && ARGUMENT_SCHLUESSEL.containsMatchIn(block))

    private fun wert(roh: String): Any {
        val w = roh.trim()
        if (w.startsWith("[")) runCatching { return JSONArray(w) }
        if (w.startsWith("{")) runCatching { return JSONObject(w) }
        return w.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&#39;", "'").replace("&amp;", "&")
    }

    /**
     * Anfang und Ende jedes äußersten {…}-Blocks, Klammern in Zeichenketten zählen nicht. Bleibt eine Klammer im
     * Fließtext offen, wird ab dem Zeichen danach weitergesucht, damit ein späterer Block trotzdem gefunden wird.
     */
    fun jsonBloecke(t: String, ab: Int = 0): List<Pair<Int, Int>> {
        val ergebnis = mutableListOf<Pair<Int, Int>>()
        var tiefe = 0
        var start = -1
        var inText = false
        var i = ab
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
        if (tiefe > 0 && start >= 0) ergebnis += jsonBloecke(t, start + 1)
        return ergebnis
    }
}
