package de.frank.ziele.text

import java.net.URI

/**
 * Trennt einen Recherche-Bericht in den reinen Text und seine Quellen.
 *
 * Die Websuche setzt Belege mitten in die Sätze, meist als `([seite.de](https://…?utm_source=openai))`,
 * dazu kommt am Ende ein Abschnitt QUELLEN. Beides wandert hier aus dem Text in eine kurze Liste:
 * Titel oder Seitenname zum Anzeigen, die Adresse nur zum Antippen. Der übrige Text bleibt Wort für Wort,
 * nur die Belege, die Leerstellen um sie herum und Markdown-Zeichen verschwinden. Rein und wiederholbar:
 * Auch alte, gespeicherte Berichte werden so beim Anzeigen und Vorlesen aufgeräumt, ohne dass sich an der
 * Datenbank etwas ändert.
 */
object Quellen {

    /** [url] ist leer, wenn die Quelle keine Adresse hat (etwa ein Buch); dann ist sie nicht antippbar. */
    data class Quelle(val titel: String, val seite: String, val url: String)
    data class Recherche(val text: String, val quellen: List<Quelle>)

    // Satzzeichen und Anführungszeichen am Ende gehören zum Satz, nicht zur Adresse.
    private const val URL_ZEICHEN = """[^\s()<>\[\]"'“”„»«*]"""
    private const val URL = """https?://(?:$URL_ZEICHEN|\([^\s()<>]*\))*(?:[^\s()<>\[\]"'“”„»«*.,;:!?]|\([^\s()<>]*\))"""
    private const val HOST = """(?:www\.)?[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[a-z]{2,}"""
    private val LINK = Regex("""\[([^\]\n]*)]\(($URL)\)""")
    private val NACKTE_URL = Regex(URL)
    private val DOMAIN = Regex("""(?<![\w@./-])$HOST(?:/[^\s,;()]*[^\s,;().:!?])?""")
    private val NUR_DOMAIN = Regex("""^$HOST/?$""")
    /** Eine Klammer, auch mit Links darin. */
    private val KLAMMER = Regex("""[ \t]*\(((?:\[[^\]\n]*]\($URL\)|[^()\n])*)\)""")
    private val BELEGWORT = Regex("""(?i)\b(quellen?|sources?|vgl\.?|siehe|laut|nach|und|sowie|bzw\.?|u\.\s?a\.)(?!\w)|[,;:•–—\-\s]""")
    private val QUELLEN_KOPF = Regex("""^[#*\s]*(QUELLEN|Quellen|Quellenangaben|Sources)(\s*\([^)]*\))?\s*:?[*\s]*$""")
    private val NUR_BELEGWORT = Regex("""^[-•*\s]*(?i:quellen?|sources?)\s*:?\s*$""")
    private val TRENNSTRICH = Regex("""^\s*([-_*—])\1{2,}\s*$""")
    private val AUFZAEHLUNG = Regex("""^\s*(?:[-•*]|\d+[.)])\s+""")
    private val RAUTE = Regex("""^\s*#+\s*""")
    private val LEERE_KLAMMER = Regex("""\(\s*[,;]?\s*\)""")
    private val DOPPELTE_LEERE = Regex("""[ \t]{2,}""")
    private val LEER_VOR_ZEICHEN = Regex(""" +([.,;:!?])""")
    private val ZU_VIELE_ZEILEN = Regex("""\n{3,}""")
    private val SPITZE_KLAMMERN = Regex("""[<>]""")
    private val PFAD_TRENNER = Regex("""[-_+ ]""")

    private val SEITENNAMEN = mapOf(
        "pubmed.ncbi.nlm.nih.gov" to "PubMed",
        "ncbi.nlm.nih.gov" to "NCBI",
        "pmc.ncbi.nlm.nih.gov" to "PubMed Central",
        "de.wikipedia.org" to "Wikipedia",
        "en.wikipedia.org" to "Wikipedia",
        "youtube.com" to "YouTube",
    )

    fun teilen(bericht: String): Recherche {
        val gesammelt = LinkedHashMap<String, Eintrag>()
        val zeilen = bericht.replace("\r\n", "\n").lines()

        // Abschließender Abschnitt QUELLEN: Belege wandern in die Liste, ein Hinweis in ganzen Sätzen bleibt Text.
        val kopf = zeilen.indexOfLast { QUELLEN_KOPF.matches(it) }
        val textZeilen = if (kopf >= 0 && zeilen.drop(kopf + 1).any { it.isNotBlank() }) {
            val zurueck = zeilen.drop(kopf + 1).filterNot { it.isBlank() || quellenZeile(it, gesammelt) }
            zeilen.take(kopf) + (if (zurueck.isEmpty()) emptyList() else listOf("") + zurueck)
        } else zeilen

        var text = textZeilen.joinToString("\n")
        // Klammern, die nur Belege enthalten (Links, Adressen, Seitennamen, „Quelle:“), verschwinden ganz;
        // inhaltliche Klammern bleiben.
        text = KLAMMER.replace(text) { m ->
            val innen = m.groupValues[1]
            val links = LINK.findAll(innen).toList()
            val ohneLinks = LINK.replace(innen, " ")
            val urls = NACKTE_URL.findAll(ohneLinks).map { it.value }.toList()
            val ohneUrls = NACKTE_URL.replace(ohneLinks, " ")
            val beleg = links.isNotEmpty() || urls.isNotEmpty() || DOMAIN.containsMatchIn(ohneUrls)
            if (beleg && BELEGWORT.replace(DOMAIN.replace(ohneUrls, ""), "").isEmpty()) {
                links.forEach { merke(it.groupValues[2], it.groupValues[1], gesammelt) }
                urls.forEach { merke(it, "", gesammelt) }
                ""
            } else m.value
        }
        // Links im Satz: Der Linktext gehört zum Satz und bleibt, nur die Adresse geht in die Liste.
        text = LINK.replace(text) { m ->
            val (anzeige, url) = m.destructured
            merke(url, anzeige, gesammelt)
            if (anzeige.trim().startsWith("http")) "" else anzeige
        }
        text = NACKTE_URL.replace(text) { m -> merke(m.value, "", gesammelt); "" }

        val sauber = text.lines()
            .map { z ->
                // Markdown-Reste: fett und Rauten-Überschriften (die werden zur Großbuchstaben-Überschrift wie die übrigen).
                var t = z.replace("**", "").replace("__", "")
                if (RAUTE.containsMatchIn(t)) t = RAUTE.replace(t, "").uppercase()
                t.replace(LEERE_KLAMMER, "").replace(DOPPELTE_LEERE, " ").replace(LEER_VOR_ZEICHEN, "$1").trimEnd()
            }
            .filterNot { NUR_BELEGWORT.matches(it) }
            .dropLastWhile { it.isBlank() || TRENNSTRICH.matches(it) }
            .joinToString("\n")
            .replace(ZU_VIELE_ZEILEN, "\n\n")
            .trim()
        return Recherche(sauber, gesammelt.values.map { Quelle(it.titel, it.seite, it.url) })
    }

    private class Eintrag(var titel: String, val seite: String, val url: String, var ausdruecklich: Boolean)

    /** Liest eine Zeile unter QUELLEN. false = die Zeile ist kein Beleg, sondern ein Satz, und gehört zurück in den Text. */
    private fun quellenZeile(zeile: String, ziel: LinkedHashMap<String, Eintrag>): Boolean {
        val links = LINK.findAll(zeile).toList()
        if (links.isNotEmpty()) { links.forEach { merke(it.groupValues[2], it.groupValues[1], ziel) }; return true }
        val url = NACKTE_URL.find(zeile)?.value ?: DOMAIN.find(zeile)?.value
        val ohneAdresse = if (url != null) zeile.replace(url, " ") else zeile
        val titel = ohneAdresse
            .replace(AUFZAEHLUNG, "")
            .replace(SPITZE_KLAMMERN, "")
            .replace(LEERE_KLAMMER, "")
            .replace("**", "")
            .trim().trim('–', '—', '-', ':', '|', ',', ';', '"', '„', '“').trim()
        if (url != null) { merke(if (url.startsWith("http")) url else "https://$url", titel, ziel); return true }
        // Ohne Adresse: ein ganzer Satz ist ein Hinweis zum Text, ein kurzer Eintrag eine Quelle ohne Link (etwa ein Buch).
        val satz = !AUFZAEHLUNG.containsMatchIn(zeile) && (titel.endsWith('.') || titel.endsWith('!') || titel.endsWith('?')) && titel.split(' ').size >= 5
        if (satz || titel.isEmpty()) return titel.isEmpty()
        val schluessel = "ohne:" + titel.lowercase()
        if (schluessel !in ziel) ziel[schluessel] = Eintrag(kuerzen(titel), kuerzen(titel), "", true)
        return true
    }

    private fun merke(roheUrl: String, anzeige: String, ziel: LinkedHashMap<String, Eintrag>) {
        val url = ohneVerfolgung(roheUrl)
        val uri = runCatching { URI(url) }.getOrNull() ?: return
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return
        // Nur der Rechnername ist unabhängig von Groß- und Kleinschreibung, der Pfad nicht.
        val schluessel = host + (uri.rawPath ?: "").trimEnd('/') + (uri.rawQuery?.let { "?$it" } ?: "")
        val seite = SEITENNAMEN[host] ?: host
        val roh = anzeige.trim()
        val ausdruecklich = roh.isNotEmpty() && !roh.startsWith("http") && !NUR_DOMAIN.matches(roh)
        val titel = if (ausdruecklich) kuerzen(roh) else ausPfad(uri) ?: seite
        val da = ziel[schluessel]
        if (da == null) ziel[schluessel] = Eintrag(titel, seite, url, ausdruecklich)
        else if (ausdruecklich && !da.ausdruecklich) { da.titel = titel; da.ausdruecklich = true }
    }

    /** Ein lesbarer Titel aus dem letzten Pfadstück, etwa „Dysarthria in Adults“ statt „Dysarthria-in-Adults“. */
    private fun ausPfad(uri: URI): String? {
        val stueck = uri.path?.trimEnd('/')?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: return null
        val worte = stueck.split(PFAD_TRENNER).filter { it.isNotBlank() }
        val buchstaben = stueck.count { it.isLetter() }
        if (buchstaben < 4 || buchstaben < stueck.length * 0.6 || stueck.length > 70) return null
        if (worte.size == 1 && worte[0].any { it.isDigit() }) return null
        return worte.joinToString(" ").replaceFirstChar { it.uppercase() }
    }

    private fun ohneVerfolgung(url: String): String {
        val frage = url.indexOf('?').takeIf { it >= 0 } ?: return url
        val anker = url.indexOf('#', frage).takeIf { it >= 0 }
        val teile = url.substring(frage + 1, anker ?: url.length).split('&').filter { it.isNotBlank() && !it.startsWith("utm_") }
        return url.substring(0, frage) + (if (teile.isEmpty()) "" else "?" + teile.joinToString("&")) + (anker?.let { url.substring(it) } ?: "")
    }

    private fun kuerzen(t: String) = if (t.length <= 80) t else t.take(78).trimEnd() + " …"
}
