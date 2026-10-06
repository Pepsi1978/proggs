package de.frank.aufgaben.ki

import de.frank.aufgaben.auth.ChatTurn
import de.frank.aufgaben.auth.CodexAuthManager
import de.frank.aufgaben.data.Einstellungen
import de.frank.aufgaben.data.TITEL_MAX
import de.frank.aufgaben.data.kurzerTitel

/** Die zwei KI-Aufgaben der App: Diktat sauber formulieren und einen kurzen Titel finden. */
class AufgabenKi(private val auth: CodexAuthManager, private val einstellungen: Einstellungen) {

    /**
     * Formt diktierten Text zu einer Aufgabe in der Befehlsform („Verkaufe die Grafikkarte …“), ohne den Inhalt zu ändern. [bisher] sind die schon
     * gezeigten Fassungen — jeder weitere Tipp liefert eine neue Formulierung.
     */
    suspend fun verbessere(text: String, bisher: List<String>): String {
        val anweisung = buildString {
            append(
                "Der Text ist eine diktierte Aufgabe aus einer Spracherkennung und deshalb unsauber. " +
                    "Erkenne die Absicht dahinter und formuliere sie als klare Aufgabe in der Befehlsform, die den " +
                    "Sprecher direkt mit „du“ anspricht (Imperativ, zweite Person Singular), in sehr gutem Deutsch. " +
                    "Beispiele: „Ich möchte noch eine CD brennen für Papa mit den Solo-Liedern“ wird zu " +
                    "„Brenne die Solo-CD für Papa.“ – „Ich möchte die Grafikkarte bei Kleinanzeigen verkaufen“ wird zu " +
                    "„Verkaufe die Grafikkarte bei Kleinanzeigen.“ " +
                    "Beginne mit dem Verb im Imperativ. Kurz und direkt, meist ein Satz; nur wenn der Text mehrere " +
                    "Schritte oder wichtige Details enthält, folgen weitere kurze Sätze, ebenfalls in Befehlsform. " +
                    "Entferne Versprecher, Füllwörter, Wiederholungen und Formulierungen wie „ich möchte“, „ich muss“ " +
                    "oder „ich sollte“. Füge nichts hinzu und lass nichts Inhaltliches weg. Behalte Zeitangaben, Namen " +
                    "und Zahlen exakt bei. Antworte nur mit der Aufgabe, ohne Vorrede und ohne Anführungszeichen.",
            )
            if (bisher.isNotEmpty()) {
                append("\n\nDiese Fassungen gab es schon. Liefere eine deutlich andere Formulierung bei gleichem Inhalt:\n")
                bisher.forEach { append("- ").append(it.take(600)).append('\n') }
            }
        }
        return frage(anweisung, text).trim().trim('„', '“', '"')
    }

    /**
     * Ein kurzer Titel für eine Aufgabe ohne eigene Überschrift, höchstens [TITEL_MAX] Zeichen samt Leerzeichen
     * (sonst passt er im Widget nicht neben die Uhrzeit). Ist die erste Antwort zu lang, gibt es einen zweiten
     * Versuch; bleibt sie zu lang, wird an einer Wortgrenze gekürzt.
     */
    suspend fun titel(text: String): String {
        val anweisung = "Formuliere für die folgende Aufgabe einen sehr kurzen, prägnanten Titel, der sagt, was zu tun ist " +
            "(z. B. „Arzttermin vereinbaren“). Höchstens $TITEL_MAX Zeichen einschließlich Leerzeichen, lieber kürzer; " +
            "kürze lange Wörter nicht ab, sondern wähle knappere. Keine Anführungszeichen, kein Punkt, keine Uhrzeiten. " +
            "Antworte nur mit dem Titel."
        fun bereinigt(antwort: String) = antwort.lineSequence().firstOrNull().orEmpty().trim().trim('„', '“', '"', '.').trim()
        val erster = bereinigt(frage(anweisung, text))
        if (erster.length <= TITEL_MAX) return erster
        val zweiter = runCatching {
            bereinigt(frage("$anweisung\n\nDein Vorschlag „$erster“ hat ${erster.length} Zeichen und ist zu lang. Finde einen kürzeren.", text))
        }.getOrDefault("")
        return kurzerTitel(zweiter.takeIf { it.isNotBlank() && it.length <= TITEL_MAX } ?: erster)
    }

    private suspend fun frage(anweisung: String, text: String): String = auth.streamChat(
        instructions = anweisung,
        turns = listOf(ChatTurn("user", text.trim())),
        model = einstellungen.modell,
        reasoningEffort = einstellungen.denkstufe,
    )
}

/** Titel ohne KI: so viele erste Wörter des Textes, wie in [TITEL_MAX] Zeichen passen. */
fun notTitel(text: String): String = kurzerTitel(text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty())
