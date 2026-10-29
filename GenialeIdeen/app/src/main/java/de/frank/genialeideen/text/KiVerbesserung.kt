package de.frank.genialeideen.text

import de.frank.genialeideen.auth.ChatTurn
import de.frank.genialeideen.auth.CodexModel
import de.frank.genialeideen.auth.EigeneAnfrage
import de.frank.genialeideen.auth.ReasoningEffort
import de.frank.genialeideen.di.AppContainer
import de.frank.genialeideen.ui.IdeenViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * „Mit KI verbessern“ für Einzelansicht und Jarvis-Brücke an einer Stelle: Beide sehen dieselben laufenden
 * Verbesserungen, deshalb läuft je Idee höchstens eine. Die Arbeit läuft im eigenen Bereich zu Ende, auch wenn
 * der Bildschirm geschlossen wird oder Jarvis nicht mehr wartet.
 */
object KiVerbesserung {
    sealed interface Ausgang {
        /** [bereits]: Dieselbe Verbesserung war gerade erst fertig — nicht noch einmal über den verbesserten Text. */
        data class Fertig(val bereits: Boolean = false) : Ausgang
        data object Leer : Ausgang
        data object Geaendert : Ausgang
        data class Fehler(val ursache: Exception) : Ausgang
        /** Für diese Idee läuft gerade die andere Art (alles oder nur die Kurzfassung). */
        data object AndereLaeuft : Ausgang
    }

    /** So lange gilt ein Wiederholungsaufruf von Jarvis als derselbe Auftrag. */
    private const val WIEDERHOLUNG_MS = 3 * 60_000L

    private val nebenbei = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auftraege = mutableMapOf<Long, Pair<Boolean, Deferred<Ausgang>>>()
    private val zuletzt = mutableMapOf<Long, Pair<Boolean, Long>>()
    private val _laeuft = MutableStateFlow<Set<Long>>(emptySet())

    /** Die Ideen, deren Fassungen die KI gerade verbessert. */
    val laeuft: StateFlow<Set<Long>> = _laeuft.asStateFlow()

    /**
     * Startet die Verbesserung oder hängt sich an eine laufende derselben Art. Mit [nurKurz] bleibt der Text, nur die
     * Kurzfassung wird neu (ohne „Rückgängig“-Stand, der Text hat sich ja nicht geändert). Mit [wiederholschutz]
     * gibt ein Aufruf kurz nach einer fertigen gleichen Verbesserung deren Ergebnis zurück, statt erneut zu verbessern.
     */
    fun starte(c: AppContainer, id: Long, nurKurz: Boolean, wiederholschutz: Boolean = false): Deferred<Ausgang> = synchronized(auftraege) {
        auftraege[id]?.let { (art, auftrag) ->
            if (auftrag.isActive) return if (art == nurKurz) auftrag else CompletableDeferred<Ausgang>(Ausgang.AndereLaeuft)
        }
        if (wiederholschutz) zuletzt[id]?.let { (art, zeit) ->
            if (art == nurKurz && System.currentTimeMillis() - zeit < WIEDERHOLUNG_MS) return CompletableDeferred<Ausgang>(Ausgang.Fertig(bereits = true))
        }
        val auftrag = nebenbei.async { fuehreAus(c, id, nurKurz) }
        auftraege[id] = nurKurz to auftrag
        _laeuft.value = _laeuft.value + id
        auftrag.invokeOnCompletion {
            synchronized(auftraege) {
                if (auftraege[id]?.second !== auftrag) return@synchronized
                auftraege.remove(id)
                _laeuft.value = _laeuft.value - id
            }
        }
        auftrag
    }

    private suspend fun fuehreAus(c: AppContainer, id: Long, nurKurz: Boolean): Ausgang =
        verbessere(c, id, nurKurz).also { if (it is Ausgang.Fertig) synchronized(auftraege) { zuletzt[id] = nurKurz to System.currentTimeMillis() } }

    private suspend fun verbessere(c: AppContainer, id: Long, nurKurz: Boolean): Ausgang = try {
        val idee = c.ideenRepository.lade(id)
        if (idee == null || idee.text.isBlank()) {
            Ausgang.Leer
        } else {
            val modell = CodexModel.fromLabel(c.settings.model)
            // Eine Zusammenfassung braucht kein tiefes Nachdenken — wie im Hintergrund-Durchgang der Kurzfassungen.
            val stufe = if (nurKurz) ReasoningEffort.LOW else ReasoningEffort.fromLabel(c.settings.reasoning)
            if (nurKurz) {
                val kurz = Kurzfassung.erzeuge(c.codexAuthManager, modell, stufe, idee.text)
                when {
                    kurz.isBlank() -> Ausgang.Leer
                    c.ideenRepository.setzeKurzfassung(idee.id, idee.text, kurz) -> Ausgang.Fertig()
                    else -> Ausgang.Geaendert
                }
            } else {
                // Vom Stopp-Knopf des Chats unabhängig: Die Verbesserung läuft zu Ende.
                val text = UmlautKorrektur.korrigiere(
                    withContext(EigeneAnfrage) {
                        c.codexAuthManager.streamChat(IdeenViewModel.VERBESSERUNG, listOf(ChatTurn("user", idee.text)), modell, stufe)
                    }.trim(),
                )
                if (text.isBlank()) {
                    Ausgang.Leer
                } else {
                    // Scheitert nur die Kurzfassung, bleibt der verbesserte Text trotzdem stehen —
                    // die leere Kurzfassung zieht der Hintergrund nach.
                    val kurz = runCatching { Kurzfassung.erzeuge(c.codexAuthManager, modell, stufe, text) }.getOrDefault("")
                    if (c.ideenRepository.setzeVerbessert(idee.id, idee.text, text, kurz)) Ausgang.Fertig() else Ausgang.Geaendert
                }
            }
        }
    } catch (e: Exception) {
        if (e is kotlinx.coroutines.CancellationException) throw e
        Ausgang.Fehler(e)
    }
}
