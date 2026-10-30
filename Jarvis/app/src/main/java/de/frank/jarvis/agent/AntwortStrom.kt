package de.frank.jarvis.agent

import de.frank.jarvis.speech.SprechText

/**
 * Liest die gestreamte Modellantwort mit, solange sie die Form {"antwort": "…"} hat, und meldet den ersten fertigen Satz,
 * damit die Stimme schon loslegt, während das Modell noch schreibt. Ein Werkzeugaufruf oder freier Text ergibt nie einen Satz.
 */
internal class AntwortStrom {
    private val roh = StringBuilder()
    private val text = StringBuilder()
    /** Lesestelle in [roh]; null = der Anfang des JSON ist noch nicht gefunden. */
    private var pos: Int? = null
    private var tot = false
    private var gemeldet = false
    private var geschlossen = false

    /** Liefert genau einmal den ersten fertigen Satz, sonst null. */
    fun neu(delta: String): String? {
        if (tot || gemeldet) return null
        roh.append(delta)
        if (pos == null) {
            val anfang = ANFANG.find(roh)
            if (anfang == null) {
                if (!MOEGLICH.matches(roh)) tot = true
                return null
            }
            pos = anfang.range.last + 1
        }
        lies()
        val satz = SprechText.ersterSatz(text.toString())
        if (satz == null) {
            // Es kommt keiner mehr: nicht bei jedem weiteren Stück den wachsenden Text neu prüfen.
            if (geschlossen || text.length > MAX_ZEICHEN) tot = true
            return null
        }
        gemeldet = true
        return satz
    }

    /** Entschlüsselt den JSON-Text bis zum Ende dessen, was schon da ist; ein halbes Escape bleibt für das nächste Stück liegen. */
    private fun lies() {
        var i = pos ?: return
        while (i < roh.length) {
            val c = roh[i]
            if (c == '"') { geschlossen = true; pos = i; return }
            if (c != '\\') { text.append(c); i++; continue }
            val art = roh.getOrNull(i + 1) ?: break
            if (art == 'u') {
                if (i + 6 > roh.length) break
                val code = roh.substring(i + 2, i + 6).toIntOrNull(16)
                if (code == null) { tot = true; return }
                text.append(code.toChar())
                i += 6
                continue
            }
            text.append(when (art) { 'n' -> '\n'; 't' -> '\t'; 'r' -> '\r'; 'b' -> '\b'; 'f' -> '\u000C'; else -> art })
            i += 2
        }
        pos = i
    }

    private companion object {
        const val MAX_ZEICHEN = 400
        val ANFANG = Regex("""^\s*(?:```(?:json)?\s*)?\{\s*"antwort"\s*:\s*"""")
        /** Kann daraus noch der Anfang werden? Ein Werkzeugaufruf fällt hiermit sofort heraus. */
        val MOEGLICH = Regex("""^\s*(?:`{0,3}(?:j|js|jso|json)?\s*)?(?:\{\s*(?:"(?:a|an|ant|antw|antwo|antwor|antwort)?"?\s*:?\s*)?)?""")
    }
}
