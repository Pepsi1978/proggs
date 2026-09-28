package de.frank.newskompass.tts

import de.frank.newskompass.data.Archiv
import de.frank.newskompass.data.model.Ausgabe
import de.frank.newskompass.data.model.Block
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Einleitung und Ausleitung, wenn ein ganzer Block vorgelesen wird — etwa „Willkommen zur
 * Morgenausgabe von KI-News vom 28. September 2026.“ und „Danke fürs Zuhören, das war …“.
 *
 * Die Sätze bildet die KI bei der Recherche; fehlen sie (ältere Ausgaben, Rückblicke, eine
 * Antwort ohne JSON), springt eine feste Vorlage ein.
 */
object Moderation {
    /** Obergrenze für einen Satz der KI — länger ist kein kurzer Begrüßungssatz mehr. */
    const val MAX_ZEICHEN = 300

    private val DATUM = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMANY)

    /** Der ganze Vorlesetext eines Blocks: Einleitung, alle Meldungen, Ausleitung. */
    fun blockText(ausgabe: Ausgabe?, block: Block, zone: ZoneId = ZoneId.systemDefault()): String {
        val meldungen = block.meldungen.map { it.vorleseText }
        // Ohne Ausgabe oder ohne Meldungen gibt es nichts anzusagen — dann wie bisher nur der Titel.
        if (ausgabe == null || meldungen.isEmpty()) return (listOf("${block.titel}.") + meldungen).joinToString("\n\n")
        return (listOf(anmoderation(ausgabe, block, zone)) + meldungen + abmoderation(ausgabe, block, zone)).joinToString("\n\n")
    }

    fun anmoderation(ausgabe: Ausgabe, block: Block, zone: ZoneId = ZoneId.systemDefault()): String =
        passend(block.anmoderation, ausgabe, block).ifBlank {
            val datum = datum(ausgabe, zone)
            when {
                block.frage != null -> "Hier ist die Antwort auf deine Frage zu ${block.titel}, Stand $datum."
                ausgabe.id.startsWith(Archiv.RUECKBLICK_PRAEFIX) -> "Willkommen zum ${rueckblick(ausgabe)} von ${block.titel}."
                istAusgabe(ausgabe) -> "Willkommen zur ${ausgabe.slot} von ${block.titel} vom $datum."
                else -> "Willkommen zu ${block.titel} vom $datum."
            }
        }

    fun abmoderation(ausgabe: Ausgabe, block: Block, zone: ZoneId = ZoneId.systemDefault()): String =
        passend(block.abmoderation, ausgabe, block).ifBlank {
            val datum = datum(ausgabe, zone)
            when {
                block.frage != null -> "Danke fürs Zuhören, das war die Antwort auf deine Frage zu ${block.titel}."
                ausgabe.id.startsWith(Archiv.RUECKBLICK_PRAEFIX) -> "Danke fürs Zuhören, das war der ${rueckblick(ausgabe)} von ${block.titel}."
                istAusgabe(ausgabe) -> "Danke fürs Zuhören, das war die ${ausgabe.slot} von ${block.titel} vom $datum."
                else -> "Danke fürs Zuhören, das war ${block.titel} vom $datum."
            }
        }

    /**
     * Säubert einen Satz der KI: erste nicht leere Zeile, ohne Anführungszeichen und Markdown,
     * mit Satzzeichen am Ende. Zu lang oder mit Internetadresse: leer, dann gilt die Vorlage.
     */
    fun saeubere(roh: String): String {
        val zeile = roh.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }.orEmpty()
            .trim('"', '„', '“', '\'', '*', '#', ' ')
        if (zeile.isBlank() || zeile.length > MAX_ZEICHEN || zeile.contains("http", ignoreCase = true)) return ""
        return if (zeile.last() in ".!?") zeile else "$zeile."
    }

    /**
     * Der Satz der KI, wenn er zum Block passt: Er nennt den Blocktitel und bei einer regulären Ausgabe
     * auch ihren Namen. Sonst leer — lieber die Vorlage als eine falsche Ansage.
     */
    private fun passend(roh: String, ausgabe: Ausgabe, block: Block): String {
        val satz = saeubere(roh)
        if (satz.isBlank() || !satz.contains(block.titel, ignoreCase = true)) return ""
        if (block.frage == null && istAusgabe(ausgabe) && !satz.contains(ausgabe.slot, ignoreCase = true)) return ""
        return satz
    }

    /** Morgen-, Mittags- oder Abendausgabe — nicht „Deine Fragen“ oder ein leerer Name alter Dateien. */
    private fun istAusgabe(ausgabe: Ausgabe): Boolean = ausgabe.slot.endsWith("ausgabe") && !ausgabe.slot.contains(' ')

    /** „Rückblick September 2026“ aus „Rückblick September 2026 · 1. bis 28. September“. */
    private fun rueckblick(ausgabe: Ausgabe): String = ausgabe.slot.substringBefore(" · ").trim().ifBlank { "Rückblick" }

    private fun datum(ausgabe: Ausgabe, zone: ZoneId): String = Instant.ofEpochMilli(ausgabe.erstelltUm).atZone(zone).format(DATUM)
}
