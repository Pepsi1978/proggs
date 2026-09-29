package de.frank.aufgaben.ki

import de.frank.aufgaben.auth.ChatTurn
import de.frank.aufgaben.auth.CodexAuthManager
import de.frank.aufgaben.data.Einstellungen

/** Die zwei KI-Aufgaben der App: Diktat sauber formulieren und einen kurzen Titel finden. */
class AufgabenKi(private val auth: CodexAuthManager, private val einstellungen: Einstellungen) {

    /**
     * Bringt diktierten Text in gutes Deutsch, ohne den Inhalt zu ändern. [bisher] sind die schon
     * gezeigten Fassungen — jeder weitere Tipp liefert eine neue Formulierung.
     */
    suspend fun verbessere(text: String, bisher: List<String>): String {
        val anweisung = buildString {
            append(
                "Der Text ist eine diktierte Aufgabe aus einer Spracherkennung und deshalb unsauber. " +
                    "Erkenne die Absicht und gib genau diese Aufgabe in klarem, sehr gutem Deutsch wieder. " +
                    "Korrigiere Grammatik, Rechtschreibung, Satzbau und Wortwahl, entferne Versprecher, Füllwörter und " +
                    "Wiederholungen. Füge NICHTS hinzu, lass nichts Inhaltliches weg. Behalte Zeitangaben, Namen und Zahlen " +
                    "exakt bei. Antworte nur mit dem verbesserten Text, ohne Vorrede und ohne Anführungszeichen.",
            )
            if (bisher.isNotEmpty()) {
                append("\n\nDiese Fassungen gab es schon. Liefere eine deutlich andere Formulierung bei gleichem Inhalt:\n")
                bisher.forEach { append("- ").append(it.take(600)).append('\n') }
            }
        }
        return frage(anweisung, text).trim().trim('„', '“', '"')
    }

    /** Ein kurzer Titel (2–6 Wörter) für eine Aufgabe ohne eigene Überschrift. */
    suspend fun titel(text: String): String {
        val antwort = frage(
            "Formuliere für die folgende Aufgabe einen sehr kurzen, prägnanten Titel mit 2 bis 6 Wörtern, " +
                "der sagt, was zu tun ist (z. B. „Arzttermin vereinbaren“). Keine Anführungszeichen, kein Punkt, " +
                "keine Uhrzeiten. Antworte nur mit dem Titel.",
            text,
        )
        return antwort.lineSequence().firstOrNull().orEmpty().trim().trim('„', '“', '"', '.').take(80)
    }

    private suspend fun frage(anweisung: String, text: String): String = auth.streamChat(
        instructions = anweisung,
        turns = listOf(ChatTurn("user", text.trim())),
        model = einstellungen.modell,
        reasoningEffort = einstellungen.denkstufe,
    )
}

/** Titel ohne KI: die ersten Wörter des Textes. */
fun notTitel(text: String): String {
    val woerter = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val kurz = woerter.take(6).joinToString(" ").trimEnd('.', ',', ';', ':')
    return if (woerter.size > 6) "$kurz …" else kurz
}
