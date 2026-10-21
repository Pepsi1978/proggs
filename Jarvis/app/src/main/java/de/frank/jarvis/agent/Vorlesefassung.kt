package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.auth.ReasoningEffort
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Was Jarvis von einem fertigen Agenten vorliest: kein Auftrag und kein ganzer Bericht, sondern ein eigener Schritt obendrauf.
 * Das Modell liest den abgelegten Bericht und fasst in wenigen Sätzen zusammen, was der Agent gemacht hat und was herauskam,
 * geschrieben fürs Ohr. Der Bericht selbst bleibt unverändert.
 */
object Vorlesefassung {
    /**
     * [inhalt] = der Bericht; [grund] = warum der Lauf nicht vollständig ist, sonst null; [inAblage] = Frank wollte ihn in der Ablage,
     * sonst steht er nur im Gespräch bzw. als Ergebnis bereit.
     */
    suspend fun erstelle(context: Context, agent: String, titel: String, inhalt: String, grund: String?, inAblage: Boolean = false): String {
        val wo = if (inAblage) "in der Ablage" else "im Gespräch"
        // Ohne Modell nie der ganze Bericht: der sprengt jedes Vorleselimit. Dann nur, dass er da ist (und ob vollständig).
        val ersatz = if (grund == null) "Der Agent $agent ist fertig. „$titel“ steht $wo."
        else "Der Agent $agent ist nicht vollständig fertig geworden. Was er geschafft hat, steht als „$titel“ $wo."
        val auth = CodexAuthManager(context.applicationContext)
        if (!auth.isConnected) return ersatz
        val text = "AGENT: $agent\n" + (grund?.let { "HINWEIS: Der Lauf ist nicht vollständig: $it\n" } ?: "") +
            "ORT: Der ausführliche Bericht steht $wo.\n" + "\nBERICHT:\n" + inhalt.take(MAX_ZEICHEN)
        // Niedrige Denkstufe wie bei den anderen kurzen Helfern: Mit Franks eingestellter (hoher) Stufe reißt ein langer Bericht das Zeitlimit.
        val antwort = withTimeoutOrNull(75_000L) {
            runCatching { auth.streamChat(ANWEISUNG, listOf(ChatTurn("user", text)), Einstellungen.get(context).modell, ReasoningEffort.LOW).trim() }
                .onFailure { if (it is CancellationException) throw it; Logbuch.fehler("Vorlesefassung: Modell", it) }.getOrNull()
        }
        return antwort?.takeIf { it.isNotBlank() } ?: ersatz
    }

    /**
     * Für eine im Sprachgespräch begonnene Anfrage: der ganze Bericht fürs Ohr umgeschrieben, ohne wichtige Inhalte wegzulassen
     * (Ergebnisse, Zahlen, Empfehlungen, offene Punkte); nur Adressen, Tabellenformen und Quellenlisten fallen weg. Ohne Modell
     * oder bei Zeitüberschreitung der Bericht selbst (der Vorleser entfernt Markdown und Adressen).
     */
    suspend fun vollstaendig(context: Context, agent: String, inhalt: String, grund: String?): String {
        val ersatz = inhalt + (grund?.let { "\n\nHinweis: Der Lauf ist nicht vollständig: $it" } ?: "")
        val auth = CodexAuthManager(context.applicationContext)
        if (!auth.isConnected) return ersatz
        val text = "AGENT: $agent\n" + (grund?.let { "HINWEIS: Der Lauf ist nicht vollständig: $it\n" } ?: "") + "\nBERICHT:\n" + inhalt.take(MAX_ZEICHEN)
        val antwort = withTimeoutOrNull(120_000L) {
            runCatching { auth.streamChat(ANWEISUNG_VOLL, listOf(ChatTurn("user", text)), Einstellungen.get(context).modell, ReasoningEffort.LOW).trim() }
                .onFailure { if (it is CancellationException) throw it; Logbuch.fehler("Vorlesefassung (vollständig): Modell", it) }.getOrNull()
        }
        return antwort?.takeIf { it.length >= 80 } ?: ersatz
    }

    private const val MAX_ZEICHEN = 60_000

    private val ANWEISUNG_VOLL = """
        Ein Hintergrund-Agent von Jarvis hat einen Auftrag erledigt, den Frank im Sprachgespräch gestellt hat. Frank hört das
        Ergebnis jetzt, vielleicht in der Küche, ohne auf den Bildschirm zu schauen. Schreibe den Bericht so um, dass Jarvis ihn
        vollständig vorlesen kann: alle Ergebnisse, Antworten, Zahlen, Preise, Namen, Empfehlungen und offenen Punkte, in der
        Reihenfolge des Berichts; nichts Wichtiges weglassen. Beginne mit einem Satz, worum es ging und was die Kernaussage ist.
        Tabellen und Aufzählungen in ganze Sätze fassen, Gliederung durch kurze Übergänge („Zum zweiten Punkt …“). Ist der Lauf
        laut HINWEIS nicht vollständig, sage das. Letzter Satz: Der ausführliche Bericht mit Quellen steht im Chat.
        Form: gesprochenes Deutsch, du sprichst Frank direkt an, Absätze durch Leerzeilen. Keine Überschriften, kein Markdown,
        keine Links oder Adressen, keine Dateinamen, keine Emojis, keine Quellenklammern. Abkürzungen ausschreiben, Zahlen mit
        Einheit so, wie man sie spricht. Keine Einleitung wie „Frank, die Ergebnisse sind da“ (die sagt Jarvis selbst davor).
        Gib nur den Vorlesetext aus.
    """.trimIndent()

    private val ANWEISUNG = """
        Ein Hintergrund-Agent von Jarvis hat einen Auftrag erledigt. Du bekommst seinen Bericht.
        Schreibe daraus eine kurze Zusammenfassung, die Jarvis Frank mit einer Sprachausgabe vorliest.
        Inhalt, in dieser Reihenfolge: in einem Satz, was der Agent gemacht hat (nicht den Auftrag wiederholen), dann die wichtigsten
        Ergebnisse, Antworten und Empfehlungen. Konkrete Zahlen, Preise und Namen, wenn sie das Ergebnis ausmachen. Ist der Lauf
        laut HINWEIS nicht vollständig, sage das kurz. Letzter Satz: wo der ausführliche Bericht steht, genau wie unter ORT angegeben.
        Form: gesprochenes Deutsch in ganzen Sätzen, du sprichst Frank direkt an, etwa 60 bis 120 Wörter, höchstens 150.
        Keine Überschriften, keine Aufzählungen, kein Markdown, keine Tabellen, keine Links oder Adressen, keine Dateinamen, kein Code,
        keine Emojis, keine Klammern mit Quellenangaben. Abkürzungen ausschreiben, Zahlen mit Einheit so, wie man sie spricht
        (zum Beispiel „rund 1.200 Euro“). Gib nur den Vorlesetext aus, ohne Einleitung.
    """.trimIndent()
}
