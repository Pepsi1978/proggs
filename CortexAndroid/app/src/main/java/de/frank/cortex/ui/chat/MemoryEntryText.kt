package de.frank.cortex.ui.chat

/**
 * Ermittelt den Text, den der Editor zeigen und der spaeter gespeichert werden soll.
 *
 * Erste Wahl ist das Serverfeld `memory_text`. Fehlt es oder enthaelt es nur die Ueberschrift der
 * Rueckfrage ("Vorgesehener Eintrag"), wird der Eintrag aus der Antwort herausgeschnitten — sonst
 * stand die Ueberschrift statt des Eintrags im Editor (Frank-Bug 2026-08-05).
 */
private val ENTRY_BODY_RE = Regex(
    """(?:^|\n)(?:Eintrag|Wortwörtlicher Text):[ \t]*\n(.+?)(?:\n[ \t]*\n|$)""",
    RegexOption.DOT_MATCHES_ALL
)

internal fun editableEntryText(memoryText: String?, reply: String): String {
    val fromServer = memoryText?.trim().orEmpty()
    val answer = reply.trim()
    val looksLikeHeadline = fromServer.length < 40 && fromServer.startsWith("Vorgesehener Eintrag")
    if (fromServer.isNotBlank() && !looksLikeHeadline) return fromServer
    val extracted = ENTRY_BODY_RE.find(answer)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    return extracted.ifBlank { fromServer.ifBlank { answer } }
}
