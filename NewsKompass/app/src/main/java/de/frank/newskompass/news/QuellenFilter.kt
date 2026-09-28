package de.frank.newskompass.news

/**
 * Nimmt Quellenangaben aus dem Fließtext einer Meldung.
 *
 * Die Websuche hängt trotz Verbot im Auftrag gern Zitate an Sätze, etwa
 * „… gesunken ([tagesschau.de](https://…))“, „(Quelle: Reuters)“ oder „[1]“. Die Quellen gehören
 * nur als anklickbare Knöpfe unter den Text — im Text selbst stören sie beim Lesen und werden
 * sonst mit vorgelesen. Der Filter läuft beim Anlegen einer Meldung, bei der Anzeige und vor dem
 * Vorlesen, damit auch schon gespeicherte Ausgaben sauber sind.
 */
object QuellenFilter {

    private const val LINK = "\\[[^\\]\\n]*]\\([^)\\s]+(?:\\s+\"[^\"]*\")?\\)"
    private const val DOMAIN = "(?:https?://)?(?:www\\.)?[\\w-]+(?:\\.[\\w-]+)*\\.[a-zA-Z]{2,}(?:/[^\\s),;]*)?"

    /** Private Zitatmarken der Websuche (Zeichen U+E200 bis U+E201, etwa um „cite turn0search0“). */
    private val zitatMarkeRegex = Regex("\\uE200[^\\uE201]*\\uE201")
    private val privateZeichenRegex = Regex("[\\uE000-\\uF8FF]")
    private val eckigeZitatRegex = Regex("【[^】]*】")

    /** Eine Klammer, die nur Links enthält: „([spiegel.de](…), [zeit.de](…))“. */
    private val linkKlammerRegex = Regex("[ \\t]*\\(\\s*$LINK(?:\\s*[,;]\\s*$LINK)*\\s*\\)")

    /**
     * Ein angehängter Link, dessen Beschriftung nur eine Adresse ist: „… gesunken [tagesschau.de](…).“
     * Nur vor Satzzeichen oder Zeilenende — mitten im Satz („Laut [x.de](…) sank …“) bliebe sonst ein Loch.
     */
    private val adressLinkRegex = Regex("[ \\t]*\\[\\s*$DOMAIN\\s*]\\([^)\\s]+(?:\\s+\"[^\"]*\")?\\)(?=\\s*(?:[.,;:!?)]|$))", RegexOption.MULTILINE)
    private val linkRegex = Regex(LINK)

    /** „(Quelle: Reuters)“, „(Quellen: dpa, AFP)“. */
    private val quellenKlammerRegex = Regex("[ \\t]*\\(\\s*(?:Quellen?|Sources?)\\b\\s*:?[^()]*\\)", RegexOption.IGNORE_CASE)

    /** Eine Klammer, die nur Adressen enthält: „(reuters.com, apnews.com)“. */
    private val adressKlammerRegex = Regex("[ \\t]*\\(\\s*$DOMAIN(?:\\s*[,;]\\s*$DOMAIN)*\\s*\\)")

    /** Fußnoten wie „[1]“, „[2, 3]“ oder „[4–6]“. */
    private val fussnoteRegex = Regex("[ \\t]*\\[\\d+(?:\\s*[,–-]\\s*\\d+)*]")

    private val nackteAdresseRegex = Regex("[ \\t]*\\b(?:https?://|www\\.)[^\\s<>()]+", RegexOption.IGNORE_CASE)

    /** Eine ganze Zeile „Quellen: …“ am Ende eines Absatzes. */
    private val quellenZeileRegex = Regex("(?im)^[ \\t]*(?:Quellen?|Sources?)\\s*:.*$")

    private val leerraumVorSatzzeichenRegex = Regex("[ \\t]+([,.;:!?])")
    private val leereKlammerRegex = Regex("[ \\t]*\\(\\s*\\)")

    fun entferne(text: String): String {
        if (text.isBlank()) return text
        return text
            .replace(zitatMarkeRegex, "")
            .replace(privateZeichenRegex, "")
            .replace(eckigeZitatRegex, "")
            .replace(linkKlammerRegex, "")
            .replace(adressLinkRegex, "")
            // Übrige Links mit echtem Wort als Beschriftung: das Wort bleibt, die Adresse geht.
            .replace(linkRegex) { it.value.substringAfter('[').substringBeforeLast("](") }
            .replace(quellenKlammerRegex, "")
            .replace(adressKlammerRegex, "")
            .replace(fussnoteRegex, "")
            .replace(nackteAdresseRegex, "")
            .replace(quellenZeileRegex, "")
            .replace(leereKlammerRegex, "")
            .replace(leerraumVorSatzzeichenRegex, "$1")
            .replace(Regex("[ \\t]{2,}"), " ")
            .replace(Regex("(?m)[ \\t]+$"), "")
            .trim()
    }

    /** Filtert jeden Absatz und lässt Absätze weg, die nur aus Quellen bestanden. */
    fun entferne(absaetze: List<String>): List<String> = absaetze.map(::entferne).filter(String::isNotBlank)
}
