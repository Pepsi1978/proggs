package de.frank.jarvis.ablage

import java.io.File
import java.util.zip.ZipFile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Reine Lese-Helfer für Vorschauen: Textauszug aus Office- und OpenDocument-Dateien, CSV/TSV als Tabelle,
 * JSON hübsch gesetzt, Syntax-Bereiche für Code. Nichts davon führt Inhalte aus; die Originaldatei bleibt
 * unverändert. Reines Kotlin, damit es in Unit-Tests läuft.
 */
object Auszug {
    /** Höchstens so viel wird für eine Vorschau gelesen; die Datei selbst wird nie gekürzt. */
    const val VORSCHAU_BYTES = 512 * 1024

    data class TextVorschau(val text: String, val gekuerzt: Boolean)

    fun lies(datei: File, hoechstens: Int = VORSCHAU_BYTES): TextVorschau {
        val laenge = datei.length()
        val bytes = datei.inputStream().use { s ->
            val puffer = ByteArray(minOf(laenge, hoechstens.toLong()).toInt())
            var n = 0
            while (n < puffer.size) { val r = s.read(puffer, n, puffer.size - n); if (r < 0) break; n += r }
            puffer.copyOf(n)
        }
        // Ein abgeschnittenes UTF-8-Zeichen am Ende ergäbe ein Ersatzzeichen: dann nur bis zum letzten Zeilenende.
        var text = String(bytes, Charsets.UTF_8)
        val gekuerzt = laenge > bytes.size
        if (gekuerzt) text.lastIndexOf('\n').takeIf { it > text.length / 2 }?.let { text = text.substring(0, it) }
        return TextVorschau(text.removePrefix("\uFEFF"), gekuerzt)
    }

    // ---- Tabellen ----

    data class Tabelle(val zeilen: List<List<String>>, val gekuerzt: Boolean)

    /** CSV (Komma oder Semikolon, erkannt an der ersten Zeile) oder TSV, mit Anführungszeichen nach RFC 4180. */
    fun tabelle(text: String, tsv: Boolean, maxZeilen: Int = 500, maxSpalten: Int = 40): Tabelle {
        val trenner = when {
            tsv -> '\t'
            else -> text.lineSequence().firstOrNull().orEmpty().let { z -> if (z.count { it == ';' } > z.count { it == ',' }) ';' else ',' }
        }
        val zeilen = mutableListOf<List<String>>()
        var feld = StringBuilder()
        var zeile = mutableListOf<String>()
        var inAnf = false
        var i = 0
        var gekuerzt = false
        while (i < text.length) {
            val c = text[i]
            when {
                inAnf && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { feld.append('"'); i++ }
                c == '"' && (inAnf || feld.isEmpty()) -> inAnf = !inAnf
                !inAnf && c == trenner -> { zeile += feld.toString(); feld = StringBuilder() }
                !inAnf && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    zeile += feld.toString(); feld = StringBuilder()
                    zeilen += zeile.take(maxSpalten); if (zeile.size > maxSpalten) gekuerzt = true
                    zeile = mutableListOf()
                    if (zeilen.size >= maxZeilen) { gekuerzt = gekuerzt || i + 1 < text.length; break }
                }
                else -> feld.append(c)
            }
            i++
        }
        if (zeilen.size < maxZeilen && (feld.isNotEmpty() || zeile.isNotEmpty())) { zeile += feld.toString(); zeilen += zeile.take(maxSpalten) }
        return Tabelle(zeilen, gekuerzt)
    }

    // ---- JSON ----

    fun jsonHuebsch(text: String): String? = runCatching {
        val t = text.trim()
        if (t.startsWith("[")) JSONArray(t).toString(2) else JSONObject(t).toString(2)
    }.getOrNull()?.replace("\\/", "/")

    // ---- Office und OpenDocument ----

    /**
     * Liest den reinen Text aus DOCX, PPTX, XLSX, ODT, ODP und ODS (alles ZIP-Container mit XML). Formatierung,
     * Bilder und Formeln fehlen; es ist ein Textauszug zur Orientierung, kein Ersatz für die Datei.
     * Schutz vor „ZIP-Bomben“: Jede gelesene Teildatei ist auf [maxTeil] Bytes begrenzt.
     */
    fun office(datei: File, endung: String, maxTeil: Long = 8L * 1024 * 1024): String? = runCatching {
        ZipFile(datei).use { zip ->
            fun teil(name: String): String? = zip.getEntry(name)?.let { e ->
                zip.getInputStream(e).use { s ->
                    val aus = java.io.ByteArrayOutputStream()
                    val p = ByteArray(32 * 1024)
                    var gesamt = 0L
                    while (true) { val n = s.read(p); if (n < 0) break; gesamt += n; if (gesamt > maxTeil) break; aus.write(p, 0, n) }
                    aus.toString("UTF-8")
                }
            }
            when (endung) {
                "docx" -> teil("word/document.xml")?.let { xmlText(it, absatz = "</w:p>", zelle = "</w:tc>", tab = "<w:tab/>") }
                "pptx" -> zip.entries().asSequence().map { it.name }.filter { Regex("ppt/slides/slide\\d+\\.xml").matches(it) }
                    .sortedBy { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
                    .mapIndexed { i, n -> "— Folie ${i + 1} —\n" + (teil(n)?.let { xmlText(it, absatz = "</a:p>") }.orEmpty()) }
                    .joinToString("\n\n").ifBlank { null }
                "xlsx" -> xlsx(::teil)
                "odt", "odp", "ods" -> teil("content.xml")?.let {
                    xmlText(it, absatz = "</text:p>", zelle = "</table:table-cell>", zeile = "</table:table-row>", folie = "</draw:page>")
                }
                else -> null
            }
        }
    }.getOrNull()?.trim()?.ifBlank { null }

    private fun xlsx(teil: (String) -> String?): String? {
        val geteilt = teil("xl/sharedStrings.xml")?.let { xml ->
            Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(xml).map { m ->
                Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL).findAll(m.groupValues[1]).joinToString("") { entities(it.groupValues[1]) }
            }.toList()
        }.orEmpty()
        val blatt = teil("xl/worksheets/sheet1.xml") ?: return null
        val zeilen = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL).findAll(blatt).take(500).map { row ->
            Regex("<c([^>]*?)(?:/>|>(.*?)</c>)", RegexOption.DOT_MATCHES_ALL).findAll(row.groupValues[1]).map { c ->
                val attr = c.groupValues[1]
                val inhalt = c.groupValues[2]
                val v = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL).find(inhalt)?.groupValues?.get(1)
                when {
                    "t=\"s\"" in attr -> v?.toIntOrNull()?.let { geteilt.getOrNull(it) }.orEmpty()
                    "t=\"inlineStr\"" in attr -> Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL).find(inhalt)?.groupValues?.get(1)?.let(::entities).orEmpty()
                    else -> v?.let(::entities).orEmpty()
                }
            }.joinToString("\t")
        }.toList()
        return zeilen.joinToString("\n").ifBlank { null }
    }

    private fun xmlText(xml: String, absatz: String, zelle: String? = null, zeile: String? = null, tab: String? = null, folie: String? = null): String {
        var t = xml
        folie?.let { t = t.replace(it, "\n\n") }
        zeile?.let { t = t.replace(it, "\n") }
        zelle?.let { t = t.replace(it, "\t") }
        tab?.let { t = t.replace(it, "\t") }
        t = t.replace(absatz, "\n").replace(Regex("<text:line-break/>|<w:br/>|<a:br/>"), "\n")
        t = t.replace(Regex("<[^>]+>"), "")
        return entities(t).lines().joinToString("\n") { it.trimEnd() }.replace(Regex("\n{3,}"), "\n\n")
    }

    private fun entities(s: String) = s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
        .replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value }
        .replace("&amp;", "&")

    // ---- Syntax-Hervorhebung ----

    enum class Klasse { KOMMENTAR, ZEICHENKETTE, ZAHL, SCHLUESSELWORT, MARKE }

    data class Bereich(val von: Int, val bis: Int, val klasse: Klasse)

    private val SCHLUESSEL = setOf(
        "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for", "while", "return", "import", "package", "private", "public",
        "def", "from", "as", "in", "is", "not", "and", "or", "true", "false", "null", "None", "True", "False", "function", "const", "let", "new",
        "static", "void", "int", "string", "bool", "try", "catch", "finally", "throw", "async", "await", "select", "insert", "update", "delete",
        "where", "from", "create", "table", "SELECT", "FROM", "WHERE", "INSERT", "UPDATE", "DELETE", "CREATE", "TABLE", "echo", "then", "fi", "do", "done",
    )

    /**
     * Einfache, robuste Hervorhebung für Code, JSON, XML, YAML und Logs: Kommentare, Zeichenketten, Zahlen,
     * Schlüsselwörter und bei XML die Tags. Ein einziger Durchlauf von links nach rechts (die Muster stehen als
     * Alternativen in einem Ausdruck), damit etwa „//“ in einer Adresse nicht als Kommentar gilt. Kein Parser.
     */
    fun hervorheben(text: String, endung: String): List<Bereich> {
        if (text.length > 200_000) return emptyList()
        val zeichenkette = "\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'"
        val zahl = "\\b\\d+(?:\\.\\d+)?\\b"
        val wort = "\\b(?:" + SCHLUESSEL.joinToString("|") + ")\\b"
        val muster: List<Pair<String, Klasse>> = when (endung) {
            "xml", "html", "htm", "svg" -> listOf("<!--[\\s\\S]*?-->" to Klasse.KOMMENTAR, "\"[^\"\\n]*\"" to Klasse.ZEICHENKETTE, "</?[A-Za-z][\\w:.-]*" to Klasse.MARKE)
            "yaml", "yml", "toml", "ini", "conf", "properties", "py", "sh", "rb" ->
                listOf("#[^\\n]*" to Klasse.KOMMENTAR, zeichenkette to Klasse.ZEICHENKETTE, "(?m)^[ \\t-]*[\\w.-]+(?=\\s*[:=])" to Klasse.MARKE, zahl to Klasse.ZAHL, wort to Klasse.SCHLUESSELWORT)
            "log", "txt" -> listOf("\\b(?:ERROR|FATAL|FEHLER|Exception|WARN|WARNING)\\b" to Klasse.SCHLUESSELWORT, "\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d+)?" to Klasse.ZAHL)
            "json" -> listOf("\"(?:\\\\.|[^\"\\\\\\n])*\"(?=\\s*:)" to Klasse.MARKE, zeichenkette to Klasse.ZEICHENKETTE, "-?" + zahl to Klasse.ZAHL, "\\b(?:true|false|null)\\b" to Klasse.SCHLUESSELWORT)
            else -> listOfNotNull(
                "/\\*[\\s\\S]*?\\*/" to Klasse.KOMMENTAR, "//[^\\n]*" to Klasse.KOMMENTAR, if (endung == "sql") "--[^\\n]*" to Klasse.KOMMENTAR else null,
                zeichenkette to Klasse.ZEICHENKETTE, zahl to Klasse.ZAHL, wort to Klasse.SCHLUESSELWORT,
            )
        }
        val gesamt = Regex(muster.joinToString("|") { "(" + it.first + ")" })
        return gesamt.findAll(text).mapNotNull { m ->
            val g = (1..muster.size).firstOrNull { m.groups[it] != null } ?: return@mapNotNull null
            if (m.range.isEmpty()) null else Bereich(m.range.first, m.range.last + 1, muster[g - 1].second)
        }.toList()
    }
}
