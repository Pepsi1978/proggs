package de.frank.ziele.text

import java.net.URI

/**
 * Trennt einen Recherche-Bericht in den reinen Text und seine Quellen.
 *
 * Die Websuche setzt Belege mitten in die Sätze, meist als `([seite.de](https://…?utm_source=openai))`,
 * dazu kommt am Ende ein Abschnitt QUELLEN. Beides wandert hier aus dem Text in eine kurze Liste:
 * Titel oder Seitenname zum Anzeigen, die Adresse nur zum Antippen. Der übrige Text bleibt Wort für Wort,
 * nur die Belege und die Leerstellen um sie herum verschwinden. Rein und wiederholbar: Auch alte,
 * gespeicherte Berichte werden so beim Anzeigen und Vorlesen aufgeräumt, ohne dass sich an der
 * Datenbank etwas ändert.
 */
object Quellen {

    data class Quelle(val titel: String, val seite: String, val url: String)
    data class Recherche(val text: String, val quellen: List<Quelle>)

    private const val URL = """https?://(?:[^\s()<>\[\]]|\([^\s()<>]*\))+"""
    private val LINK = Regex("""\[([^\]\n]*)]\(($URL)\)""")
    private val BELEGGRUPPE = Regex("""[ \t]*\(\s*\[[^\]\n]*]\($URL\)(?:\s*[,;]?\s*\[[^\]\n]*]\($URL\))*\s*\)""")
    private val NACKTE_URL = Regex(URL)
    private val DOMAIN = Regex("""(?<![\w@./-])(?:www\.)?[a-z0-9-]+(?:\.[a-z0-9-]+)*\.[a-z]{2,}(?:/[^\s,;()]*)?""")
    private val KLAMMER = Regex("""[ \t]*\(([^()\n]*)\)""")
    private val BELEGWORT = Regex("""(?i)\b(quellen?|sources?|vgl\.?|siehe|laut|und|sowie|bzw\.?)\b|[,;:•–—\-\s]""")
    private val QUELLEN_KOPF = Regex("""^[#*\s]*(QUELLEN|Quellen|Quellenangaben|Sources)(\s*\([^)]*\))?\s*:?[*\s]*$""")
    private val NUR_BELEGWORT = Regex("""^[-•*\s]*(?i:quellen?|sources?)\s*:?\s*$""")
    private val TRENNSTRICH = Regex("""^\s*([-_*—])\1{2,}\s*$""")

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

        // Abschließender Abschnitt QUELLEN: alles darunter sind Belege, kein Text.
        val kopf = zeilen.indexOfLast { QUELLEN_KOPF.matches(it) }
        val textZeilen = if (kopf >= 0 && zeilen.drop(kopf + 1).any { NACKTE_URL.containsMatchIn(it) }) {
            zeilen.drop(kopf + 1).forEach { quellenZeile(it, gesammelt) }
            zeilen.take(kopf)
        } else zeilen

        var text = textZeilen.joinToString("\n")
        text = BELEGGRUPPE.replace(text) { m -> LINK.findAll(m.value).forEach { merke(it.groupValues[2], it.groupValues[1], gesammelt) }; "" }
        text = LINK.replace(text) { m ->
            val (anzeige, url) = m.destructured
            merke(url, anzeige, gesammelt)
            if (urlArtig(anzeige)) "" else anzeige
        }
        // Klammern, die nur Adressen, Seitennamen oder „Quelle:“ enthalten; inhaltliche Klammern bleiben.
        text = KLAMMER.replace(text) { m ->
            val innen = m.groupValues[1]
            val belege = NACKTE_URL.findAll(innen).map { it.value }.toList()
            val rest = DOMAIN.replace(NACKTE_URL.replace(innen, ""), "")
            if ((belege.isNotEmpty() || DOMAIN.containsMatchIn(innen)) && BELEGWORT.replace(rest, "").isEmpty()) {
                belege.forEach { merke(it, "", gesammelt) }
                ""
            } else m.value
        }
        text = NACKTE_URL.replace(text) { m -> merke(m.value, "", gesammelt); "" }

        val sauber = text.lines()
            .map { z -> z.replace(Regex("""\(\s*[,;]?\s*\)"""), "").replace(Regex("""[ \t]{2,}"""), " ").replace(Regex(""" +([.,;:!?])"""), "$1").trimEnd() }
            .filterNot { NUR_BELEGWORT.matches(it) }
            .dropLastWhile { it.isBlank() || TRENNSTRICH.matches(it) }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
        return Recherche(sauber, gesammelt.values.map { Quelle(it.titel, it.seite, it.url) })
    }

    private class Eintrag(var titel: String, val seite: String, val url: String, var ausdruecklich: Boolean)

    private fun quellenZeile(zeile: String, ziel: LinkedHashMap<String, Eintrag>) {
        LINK.find(zeile)?.let { merke(it.groupValues[2], it.groupValues[1], ziel); return }
        val url = NACKTE_URL.find(zeile)?.value ?: return
        val titel = zeile.replace(url, " ")
            .replace(Regex("""^\s*(?:[-•*]|\d+[.)])\s*"""), "")
            .replace(Regex("""[<>]"""), "")
            .replace(Regex("""\(\s*\)"""), "")
            .trim().trim('–', '—', '-', ':', '|', ',', ';', '"', '„', '“').trim()
        merke(url, titel, ziel)
    }

    private fun merke(roheUrl: String, anzeige: String, ziel: LinkedHashMap<String, Eintrag>) {
        val url = ohneVerfolgung(roheUrl.trimEnd('.', ',', ';', ':'))
        val host = runCatching { URI(url).host }.getOrNull()?.lowercase()?.removePrefix("www.") ?: return
        val schluessel = url.lowercase().removePrefix("https://").removePrefix("http://").removePrefix("www.").trimEnd('/')
        val seite = SEITENNAMEN[host] ?: host
        val ausdruecklich = anzeige.isNotBlank() && !urlArtig(anzeige)
        val titel = if (ausdruecklich) kuerzen(anzeige.trim()) else ausPfad(url) ?: seite
        val da = ziel[schluessel]
        if (da == null) ziel[schluessel] = Eintrag(titel, seite, url, ausdruecklich)
        else if (ausdruecklich && !da.ausdruecklich) { da.titel = titel; da.ausdruecklich = true }
    }

    /** Ein lesbarer Titel aus dem letzten Pfadstück, etwa „Dysarthria in Adults“ statt „Dysarthria-in-Adults“. */
    private fun ausPfad(url: String): String? {
        val stueck = runCatching { URI(url).path }.getOrNull()?.trimEnd('/')?.substringAfterLast('/')
            ?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: return null
        val worte = stueck.split('-', '_', '+', ' ').filter { it.isNotBlank() }
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

    private fun urlArtig(t: String): Boolean {
        val s = t.trim()
        return s.isEmpty() || s.startsWith("http") || (!s.contains(' ') && s.contains('.'))
    }

    private fun kuerzen(t: String) = if (t.length <= 80) t else t.take(78).trimEnd() + " …"
}
