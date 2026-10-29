package de.frank.genialeideen.text

import de.frank.genialeideen.auth.ChatTurn
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.data.local.IdeenStatus
import de.frank.genialeideen.data.local.begrenzeWoerter
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.observability.IdeenLog
import de.frank.genialeideen.ui.IdeenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Erzeugt fehlende Kurzfassungen im Hintergrund: für neue Ideen, für von Jarvis angelegte, für
 * von Hand geänderte (deren Kurzfassung beim Ändern geleert wird) und einmalig für den Altbestand.
 *
 * Die leere Spalte selbst ist die Warteschlange — ein beendeter Prozess verliert nichts, der
 * nächste Start holt es nach. Ohne KI-Zugang bleibt sie leer; vorgelesen werden dann die ersten
 * 35 Wörter.
 */
object Kurzfassung {
    private val nebenbei = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fehlversuche je Idee und Text in diesem Prozess — ein Netzfehler soll nicht endlos kreisen. */
    private val fehlversuche = mutableMapOf<Pair<Long, Int>, Int>()

    suspend fun erzeuge(c: AppContainer, text: String): String {
        val modell = CodexModel.fromLabel(c.settings.model)
        val stufe = ReasoningEffort.fromLabel(c.settings.reasoning)
        val antwort = c.codexAuthManager.streamChat(
            IdeenViewModel.KURZFASSUNG,
            listOf(ChatTurn("user", text.take(12_000))),
            modell,
            stufe,
        )
        return begrenzeWoerter(UmlautKorrektur.korrigiere(antwort.trim().trim('"', '„', '“')))
    }

    @OptIn(FlowPreview::class)
    fun starte(c: AppContainer) {
        nebenbei.launch {
            c.database.ideenDao().alle().debounce(1_500).collect { ideen ->
                if (!c.codexAuthManager.kannDenken(CodexModel.fromLabel(c.settings.model))) return@collect
                val fehlend = ideen.filter {
                    it.kurzfassung.isBlank() && it.text.isNotBlank() && it.status != IdeenStatus.ENTWURF.name &&
                        (fehlversuche[it.id to it.text.hashCode()] ?: 0) < 2
                }
                for (idee in fehlend) {
                    val schluessel = idee.id to idee.text.hashCode()
                    runCatching { erzeuge(c, idee.text) }
                        .onSuccess { kurz ->
                            if (kurz.isNotBlank()) c.ideenRepository.setzeKurzfassung(idee.id, idee.text, kurz)
                        }
                        .onFailure { fehler ->
                            fehlversuche[schluessel] = (fehlversuche[schluessel] ?: 0) + 1
                            IdeenLog.info("Kurzfassung", "starte", "Kurzfassung fehlgeschlagen", mapOf("id" to idee.id, "fehler" to (fehler.message ?: "")))
                        }
                }
            }
        }
    }
}
