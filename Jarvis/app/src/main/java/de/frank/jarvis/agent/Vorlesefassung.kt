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
 * geschrieben fürs Ohr. Der Bericht in der Ablage bleibt unverändert.
 */
object Vorlesefassung {
    /** [inhalt] = die Ablage-Datei (Überschrift mit Auftrag plus Bericht); [grund] = warum der Lauf nicht vollständig ist, sonst null. */
    suspend fun erstelle(context: Context, agent: String, titel: String, inhalt: String, grund: String?): String {
        // Ohne Modell nie der ganze Bericht: der sprengt jedes Vorleselimit. Dann nur, dass er da ist (und ob vollständig).
        val ersatz = if (grund == null) "Der Agent $agent ist fertig. „$titel“ liegt in der Ablage."
        else "Der Agent $agent ist nicht vollständig fertig geworden. Was er geschafft hat, liegt als „$titel“ in der Ablage."
        val auth = CodexAuthManager(context.applicationContext)
        if (!auth.isConnected) return ersatz
        val text = "AGENT: $agent\n" + (grund?.let { "HINWEIS: Der Lauf ist nicht vollständig: $it\n" } ?: "") +
            "\nABGELEGTER BERICHT:\n" + inhalt.take(MAX_ZEICHEN)
        // Niedrige Denkstufe wie bei den anderen kurzen Helfern: Mit Franks eingestellter (hoher) Stufe reißt ein langer Bericht das Zeitlimit.
        val antwort = withTimeoutOrNull(75_000L) {
            runCatching { auth.streamChat(ANWEISUNG, listOf(ChatTurn("user", text)), Einstellungen.get(context).modell, ReasoningEffort.LOW).trim() }
                .onFailure { if (it is CancellationException) throw it; Logbuch.fehler("Vorlesefassung: Modell", it) }.getOrNull()
        }
        return antwort?.takeIf { it.isNotBlank() } ?: ersatz
    }

    private const val MAX_ZEICHEN = 60_000

    private val ANWEISUNG = """
        Ein Hintergrund-Agent von Jarvis hat einen Auftrag erledigt. Du bekommst seinen abgelegten Bericht (oben steht der Auftrag).
        Schreibe daraus eine kurze Zusammenfassung, die Jarvis Frank mit einer Sprachausgabe vorliest.
        Inhalt, in dieser Reihenfolge: in einem Satz, was der Agent gemacht hat (nicht den Auftrag wiederholen), dann die wichtigsten
        Ergebnisse, Antworten und Empfehlungen. Konkrete Zahlen, Preise und Namen, wenn sie das Ergebnis ausmachen. Ist der Lauf
        laut HINWEIS nicht vollständig, sage das kurz. Letzter Satz: Der ausführliche Bericht liegt in der Ablage.
        Form: gesprochenes Deutsch in ganzen Sätzen, du sprichst Frank direkt an, etwa 60 bis 120 Wörter, höchstens 150.
        Keine Überschriften, keine Aufzählungen, kein Markdown, keine Tabellen, keine Links oder Adressen, keine Dateinamen, kein Code,
        keine Emojis, keine Klammern mit Quellenangaben. Abkürzungen ausschreiben, Zahlen mit Einheit so, wie man sie spricht
        (zum Beispiel „rund 1.200 Euro“). Gib nur den Vorlesetext aus, ohne Einleitung.
    """.trimIndent()
}
