package de.frank.genialeideen.text

import de.frank.genialeideen.auth.AuthErrorKind
import de.frank.genialeideen.auth.ChatTurn
import de.frank.genialeideen.auth.CodexAuthException
import de.frank.genialeideen.auth.CodexAuthManager
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.EigeneAnfrage
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.begrenzeWoerter
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.observability.IdeenLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Erzeugt fehlende Kurzfassungen im Hintergrund: für neue Ideen, für von Jarvis angelegte, für
 * von Hand geänderte (deren Kurzfassung beim Ändern geleert wird) und einmalig für den Altbestand.
 *
 * Die leere Spalte selbst ist die Warteschlange — ein beendeter Prozess verliert nichts, der
 * nächste Start holt es nach. Ohne KI-Zugang bleibt sie leer; vorgelesen werden dann die ersten
 * 35 Wörter.
 */
object Kurzfassung {
    /** Die Kurzfassung wird vorgelesen (etwa im Genialen Wecker) — deshalb in gesprochenen Sätzen. */
    const val AUFTRAG =
        "Du bekommst eine Idee. Fasse sie in HÖCHSTENS 35 WÖRTERN zusammen und nenne dabei " +
            "alle Kernpunkte der Idee.\n\n" +
            "Regeln:\n" +
            "- Höchstens 35 Wörter, ein bis drei ganze Sätze.\n" +
            "- Jeder Kernpunkt kommt vor; Beispiele und Nebensächliches lässt du weg.\n" +
            "- Erfinde nichts hinzu und behalte die Sichtweise des Originals bei.\n" +
            "- Gib nur die Kurzfassung zurück, ohne Vorrede, ohne Titel, ohne Anführungszeichen.\n\n" +
            "Dieser Text wird vorgelesen. Schreib ihn in ganzen, gesprochenen Sätzen. " +
            "Verzichte auf Markdown, Aufzählungszeichen, Sternchen, Rauten, Klammern, " +
            "Emoji und Abkürzungen. Antworte auf Deutsch mit echten Umlauten (ä ö ü Ä Ö Ü ß)."

    private val nebenbei = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fehlversuche je Idee und Text in diesem Prozess — ein Fehler soll nicht endlos kreisen. */
    private val fehlversuche = mutableMapOf<Pair<Long, Int>, Int>()

    /** Nach Anmeldung oder neuem Schlüssel einen neuen Durchgang anstoßen. */
    private val anstoss = MutableStateFlow(0)

    fun anstossen() {
        anstoss.value++
    }

    /** Eine Kurzfassung für [text]; wird vom Stopp-Knopf des Chats nicht mit abgebrochen. */
    suspend fun erzeuge(codex: CodexAuthManager, modell: CodexModel, stufe: ReasoningEffort, text: String): String {
        val antwort = withContext(EigeneAnfrage) {
            codex.streamChat(AUFTRAG, listOf(ChatTurn("user", text.take(12_000))), modell, stufe)
        }
        return begrenzeWoerter(UmlautKorrektur.korrigiere(antwort.trim().trim('"', '\'', '„', '“', '”', '«', '»').trim()))
    }

    @OptIn(FlowPreview::class)
    fun starte(c: AppContainer) {
        nebenbei.launch {
            combine(c.database.ideenDao().alle(), anstoss) { ideen, _ -> ideen }.debounce(1_500).collect { ideen ->
                val modell = CodexModel.fromLabel(c.settings.model)
                if (!c.codexAuthManager.kannDenken(modell)) return@collect
                // Eine Zusammenfassung braucht kein tiefes Nachdenken — schont Kontingent und Akku.
                val stufe = ReasoningEffort.LOW
                // Was vorgelesen wird, zuerst: angenommene, dann offene, zuletzt umgesetzte Ideen.
                val rang = mapOf(IdeenStatus.ANGENOMMEN.name to 0, IdeenStatus.OFFEN.name to 1)
                val fehlend = ideen.filter {
                    it.kurzfassung.isBlank() && it.text.isNotBlank() && it.status != IdeenStatus.ENTWURF.name &&
                        (fehlversuche[it.id to it.text.hashCode()] ?: 0) < 2
                }.sortedBy { rang[it.status] ?: 2 }
                for (idee in fehlend) {
                    val schluessel = idee.id to idee.text.hashCode()
                    try {
                        val kurz = erzeuge(c.codexAuthManager, modell, stufe, idee.text)
                        if (kurz.isBlank()) {
                            fehlversuche[schluessel] = (fehlversuche[schluessel] ?: 0) + 1
                        } else {
                            c.ideenRepository.setzeKurzfassung(idee.id, idee.text, kurz)
                        }
                    } catch (fehler: Exception) {
                        if (fehler is kotlinx.coroutines.CancellationException) throw fehler
                        fehlversuche[schluessel] = (fehlversuche[schluessel] ?: 0) + 1
                        IdeenLog.info("Kurzfassung", "starte", "Kurzfassung fehlgeschlagen", mapOf("id" to idee.id, "fehler" to (fehler.message ?: "")))
                        // Kontingent erschöpft oder Anmeldung abgelaufen: nicht Idee für Idee weiterversuchen.
                        if (fehler is CodexAuthException && fehler.kind != AuthErrorKind.NETWORK) break
                    }
                }
            }
        }
    }
}
