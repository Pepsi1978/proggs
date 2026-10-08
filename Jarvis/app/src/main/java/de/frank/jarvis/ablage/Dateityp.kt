package de.frank.jarvis.ablage

import java.util.Locale

/** Wie die Ablage eine Datei zeigt. Bestimmt die Vorschau, nicht das Speichern: Gespeichert wird jede Datei. */
enum class Art(val anzeige: String, val kategorie: Kategorie) {
    TEXT("Text", Kategorie.TEXTE),
    MARKDOWN("Markdown", Kategorie.TEXTE),
    CODE("Code", Kategorie.TEXTE),
    DATEN("Strukturierte Daten", Kategorie.TEXTE),
    HTML("HTML", Kategorie.TEXTE),
    TABELLE_TEXT("Tabelle (CSV/TSV)", Kategorie.TABELLEN),
    TABELLE("Tabelle", Kategorie.TABELLEN),
    BILD("Bild", Kategorie.BILDER),
    ANIMATION("Animiertes Bild (GIF)", Kategorie.BILDER),
    SVG("Vektorgrafik (SVG)", Kategorie.BILDER),
    PDF("PDF", Kategorie.DOKUMENTE),
    DOKUMENT("Dokument", Kategorie.DOKUMENTE),
    PRAESENTATION("Präsentation", Kategorie.PRAESENTATIONEN),
    AUDIO("Audio", Kategorie.AUDIO),
    VIDEO("Video", Kategorie.VIDEO),
    ARCHIV("Archiv", Kategorie.SONSTIGE),
    SONSTIGE("Datei", Kategorie.SONSTIGE),
}

/** Die Filter der Ablage-Übersicht. */
enum class Kategorie(val anzeige: String) {
    TEXTE("Texte"), BILDER("Bilder"), DOKUMENTE("Dokumente"), TABELLEN("Tabellen"),
    PRAESENTATIONEN("Präsentationen"), AUDIO("Audio"), VIDEO("Video"), SONSTIGE("Sonstige"),
}

/**
 * Erkennt Dateityp, Endung und MIME-Typ. Reines Kotlin ohne Android, damit es in Unit-Tests läuft.
 * Reihenfolge: Inhalt (Signatur der ersten Bytes) vor Dateiendung vor angegebenem MIME-Typ, denn eine
 * falsche Endung oder ein pauschales „application/octet-stream“ sind bei Downloads häufig.
 */
object Dateityp {
    /** Endung → MIME-Typ für alles, was die Ablage kennt. */
    private val MIME = mapOf(
        "txt" to "text/plain", "log" to "text/plain", "md" to "text/markdown", "markdown" to "text/markdown",
        "json" to "application/json", "xml" to "application/xml", "yaml" to "application/yaml", "yml" to "application/yaml",
        "toml" to "application/toml", "ini" to "text/plain", "conf" to "text/plain", "properties" to "text/plain",
        "csv" to "text/csv", "tsv" to "text/tab-separated-values",
        "html" to "text/html", "htm" to "text/html",
        "kt" to "text/x-kotlin", "kts" to "text/x-kotlin", "java" to "text/x-java", "py" to "text/x-python", "js" to "text/javascript",
        "ts" to "text/x-typescript", "css" to "text/css", "sh" to "text/x-shellscript", "ps1" to "text/plain", "c" to "text/x-c",
        "h" to "text/x-c", "cpp" to "text/x-c++", "cs" to "text/plain", "swift" to "text/x-swift", "go" to "text/x-go", "rs" to "text/x-rust",
        "sql" to "application/sql", "gradle" to "text/plain", "bat" to "text/plain", "rb" to "text/x-ruby", "php" to "text/x-php",
        "png" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "webp" to "image/webp", "gif" to "image/gif",
        "bmp" to "image/bmp", "heic" to "image/heic", "heif" to "image/heif", "avif" to "image/avif", "svg" to "image/svg+xml",
        "pdf" to "application/pdf",
        "doc" to "application/msword", "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "odt" to "application/vnd.oasis.opendocument.text", "rtf" to "application/rtf", "epub" to "application/epub+zip",
        "xls" to "application/vnd.ms-excel", "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "ods" to "application/vnd.oasis.opendocument.spreadsheet",
        "ppt" to "application/vnd.ms-powerpoint", "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "odp" to "application/vnd.oasis.opendocument.presentation", "key" to "application/vnd.apple.keynote",
        "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "aac" to "audio/aac", "wav" to "audio/wav", "ogg" to "audio/ogg", "oga" to "audio/ogg",
        "opus" to "audio/opus", "flac" to "audio/flac", "amr" to "audio/amr", "mid" to "audio/midi",
        "mp4" to "video/mp4", "m4v" to "video/mp4", "webm" to "video/webm", "mkv" to "video/x-matroska", "3gp" to "video/3gpp",
        "mov" to "video/quicktime", "avi" to "video/x-msvideo",
        "zip" to "application/zip", "7z" to "application/x-7z-compressed", "rar" to "application/vnd.rar", "gz" to "application/gzip",
        "tar" to "application/x-tar", "apk" to "application/vnd.android.package-archive",
    )

    private val CODE = setOf("kt", "kts", "java", "py", "js", "ts", "css", "sh", "ps1", "c", "h", "cpp", "cs", "swift", "go", "rs", "sql", "gradle", "bat", "rb", "php")
    private val DATEN = setOf("json", "xml", "yaml", "yml", "toml", "ini", "conf", "properties")

    fun mimeFuer(endung: String): String = MIME[endung.lowercase(Locale.ROOT)] ?: "application/octet-stream"

    fun endungFuerMime(mime: String): String? {
        val m = mime.substringBefore(';').trim().lowercase(Locale.ROOT)
        return when (m) {
            "image/jpeg" -> "jpg"
            "text/plain" -> "txt"
            "text/markdown" -> "md"
            "text/html" -> "html"
            "audio/mpeg" -> "mp3"
            "video/mp4" -> "mp4"
            "audio/mp4" -> "m4a"
            "application/xml", "text/xml" -> "xml"
            "application/yaml", "text/yaml" -> "yaml"
            "audio/x-wav", "audio/wave" -> "wav"
            else -> MIME.entries.firstOrNull { it.value == m }?.key
        }
    }

    fun endung(name: String): String = name.substringAfterLast('/').let { n -> if ('.' in n && !n.endsWith(".")) n.substringAfterLast('.').lowercase(Locale.ROOT) else "" }

    /** Endung aus den ersten Bytes, wenn die Signatur eindeutig ist. */
    fun endungAusInhalt(kopf: ByteArray): String? {
        fun beginnt(vararg b: Int, ab: Int = 0) = kopf.size >= ab + b.size && b.indices.all { kopf[ab + it] == b[it].toByte() }
        fun text(von: Int, bis: Int) = if (kopf.size >= bis) String(kopf, von, bis - von, Charsets.ISO_8859_1) else ""
        return when {
            beginnt(0x89, 0x50, 0x4E, 0x47) -> "png"
            beginnt(0xFF, 0xD8, 0xFF) -> "jpg"
            text(0, 4) == "GIF8" -> "gif"
            text(0, 4) == "RIFF" && text(8, 12) == "WEBP" -> "webp"
            text(0, 4) == "RIFF" && text(8, 12) == "WAVE" -> "wav"
            text(0, 4) == "RIFF" && text(8, 12) == "AVI " -> "avi"
            text(0, 5) == "%PDF-" -> "pdf"
            text(0, 4) == "OggS" -> "ogg"
            text(0, 4) == "fLaC" -> "flac"
            text(0, 3) == "ID3" -> "mp3"
            beginnt(0x1A, 0x45, 0xDF, 0xA3) -> "mkv"
            text(4, 8) == "ftyp" -> when (text(8, 12).trim()) {
                "M4A", "M4B" -> "m4a"
                "qt" -> "mov"
                "heic", "heix", "mif1" -> "heic"
                "avif" -> "avif"
                else -> "mp4"
            }
            else -> {
                val anfang = String(kopf, 0, minOf(kopf.size, 512), Charsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r', '\t').lowercase(Locale.ROOT)
                when {
                    anfang.startsWith("<svg") || (anfang.startsWith("<?xml") && "<svg" in anfang) -> "svg"
                    anfang.startsWith("<!doctype html") || anfang.startsWith("<html") -> "html"
                    else -> null
                }
            }
        }
    }

    /**
     * Bestimmt Endung und MIME-Typ einer Datei. [name] ist der gewünschte oder ursprüngliche Dateiname,
     * [mimeAngabe] der vom Absender genannte Typ (darf leer sein), [kopf] die ersten Bytes des Inhalts.
     */
    fun bestimme(name: String, mimeAngabe: String?, kopf: ByteArray): Pair<String, String> {
        val ausInhalt = endungAusInhalt(kopf)
        val ausName = endung(name).takeIf { it.isNotEmpty() }
        val ausMime = mimeAngabe?.takeIf { it.isNotBlank() && !it.startsWith("application/octet-stream") }?.let(::endungFuerMime)
        // Container-Formate (ZIP → docx/xlsx/pptx/odt …) erkennt die Signatur nicht eindeutig: dann gilt der Name.
        val endung = when {
            ausInhalt != null && ausName != null && gleicheFamilie(ausInhalt, ausName) -> ausName
            ausInhalt != null -> ausInhalt
            ausName != null -> ausName
            ausMime != null -> ausMime
            istText(kopf) -> "txt"
            else -> "bin"
        }
        val mime = MIME[endung] ?: mimeAngabe?.substringBefore(';')?.trim()?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        return endung to mime
    }

    /** jpg/jpeg, mp4/m4v, ogg/oga/opus usw. gelten als dasselbe Format; dann bleibt die Endung des Namens. */
    private fun gleicheFamilie(a: String, b: String): Boolean {
        if (a == b) return true
        val familien = listOf(setOf("jpg", "jpeg"), setOf("mp4", "m4v", "m4a", "3gp", "mov"), setOf("ogg", "oga", "opus"), setOf("mkv", "webm"), setOf("heic", "heif"))
        return familien.any { a in it && b in it }
    }

    /** Sieht der Anfang wie Text aus (UTF-8 ohne Steuerzeichen)? */
    fun istText(kopf: ByteArray): Boolean {
        if (kopf.isEmpty()) return true
        val probe = kopf.copyOf(minOf(kopf.size, 4096))
        if (probe.any { it == 0.toByte() }) return false
        val zeichen = String(probe, Charsets.UTF_8)
        val steuer = zeichen.count { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }
        return steuer <= zeichen.length / 100
    }

    fun art(endung: String, mime: String): Art {
        val e = endung.lowercase(Locale.ROOT)
        val m = mime.lowercase(Locale.ROOT)
        return when {
            e == "md" || e == "markdown" -> Art.MARKDOWN
            e == "csv" || e == "tsv" -> Art.TABELLE_TEXT
            e == "svg" -> Art.SVG
            e == "gif" -> Art.ANIMATION
            e == "html" || e == "htm" -> Art.HTML
            e in CODE -> Art.CODE
            e in DATEN -> Art.DATEN
            e == "pdf" -> Art.PDF
            e in setOf("doc", "docx", "odt", "rtf", "epub") -> Art.DOKUMENT
            e in setOf("xls", "xlsx", "ods") -> Art.TABELLE
            e in setOf("ppt", "pptx", "odp", "key") -> Art.PRAESENTATION
            e in setOf("zip", "7z", "rar", "gz", "tar") -> Art.ARCHIV
            m.startsWith("image/") -> Art.BILD
            m.startsWith("audio/") -> Art.AUDIO
            m.startsWith("video/") -> Art.VIDEO
            e == "txt" || e == "log" || m.startsWith("text/") -> Art.TEXT
            else -> Art.SONSTIGE
        }
    }

    /** Größe lesbar, deutsch: „840 B“, „12,4 kB“, „3,1 MB“. */
    fun groesse(bytes: Long): String = when {
        bytes < 1000 -> "$bytes B"
        bytes < 1_000_000 -> String.format(Locale.GERMAN, "%.1f kB", bytes / 1000.0)
        bytes < 1_000_000_000 -> String.format(Locale.GERMAN, "%.1f MB", bytes / 1_000_000.0)
        else -> String.format(Locale.GERMAN, "%.2f GB", bytes / 1_000_000_000.0)
    }
}
