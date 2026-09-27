package de.frank.newskompass.ai

import de.frank.newskompass.data.EinstellungenStore
import de.frank.newskompass.data.model.Thema
import de.frank.newskompass.observability.KompassLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Bildet für jedes Thema eine kurze Überschrift aus höchstens drei Wörtern, etwa „KI-News“ oder
 * „Borussia Dortmund“, damit die zugeklappte Themenkarte in eine Zeile passt.
 *
 * Läuft im Hintergrund: Sobald sich ein Thementext eine Weile nicht mehr ändert, fragt es die KI
 * einmal ohne Websuche. Ohne Anmeldung oder bei einem Fehler bleibt die Ersatzüberschrift aus den
 * ersten Wörtern stehen; ein gescheiterter Text wird in dieser Sitzung nicht erneut versucht.
 */
class ThemenUeberschriften(
    private val codex: CodexClient,
    private val einstellungen: EinstellungenStore,
    private val bereich: CoroutineScope,
) {
    private val sperre = Mutex()
    private val gescheitert = mutableSetOf<Pair<String, String>>()

    @OptIn(FlowPreview::class)
    fun starte() {
        bereich.launch {
            einstellungen.stand
                .map { stand -> stand.themen.filter { it.ueberschriftVeraltet }.map { it.id to it.text } }
                .distinctUntilChanged()
                .debounce(WARTEZEIT_MS)
                .collect { pruefe() }
        }
    }

    /** Erzeugt fehlende oder veraltete Überschriften — auch nach einer frischen Anmeldung aufrufbar. */
    fun pruefe() {
        bereich.launch {
            sperre.withLock {
                if (!codex.istVerbunden) return@withLock
                val offen = einstellungen.stand.value.themen.filter { it.ueberschriftVeraltet && (it.id to it.text) !in gescheitert }
                offen.forEach { erzeuge(it) }
            }
        }
    }

    private suspend fun erzeuge(thema: Thema) {
        val text = thema.text
        val ueberschrift = try {
            val stand = einstellungen.stand.value
            val antwort = codex.frage(
                anweisung = "Du gibst Nachrichtenthemen eine kurze deutsche Überschrift für eine App. " +
                    "Antworte nur mit der Überschrift: höchstens drei Wörter, keine Anführungszeichen, kein Punkt, kein Zusatztext. " +
                    "Beispiele: „KI-News“, „Borussia Dortmund“, „Raumfahrt aktuell“.",
                eingabe = text.trim(),
                modellId = stand.modellId,
                // Wenig Nachdenken genügt — aber nur, wenn das gewählte Modell diese Stufe kennt.
                denktiefe = stand.modelle.firstOrNull { it.id == stand.modellId }?.stufen
                    ?.let { stufen -> "low".takeIf { it in stufen } ?: stand.denktiefe } ?: stand.denktiefe,
            )
            Thema.saeubereUeberschrift(antwort.text)
        } catch (abbruch: CancellationException) {
            throw abbruch
        } catch (fehler: Exception) {
            KompassLog.warn("ThemenUeberschriften", "erzeuge", "Überschrift gescheitert", mapOf("grund" to fehler.message))
            ""
        }
        if (ueberschrift.isBlank()) {
            gescheitert += thema.id to text
            return
        }
        // Auf dem Hauptthread schreiben, wo auch die Oberfläche schreibt — so überholt keiner den anderen.
        // Nur übernehmen, wenn der Text inzwischen nicht weiter bearbeitet wurde.
        val gesetzt = withContext(Dispatchers.Main) {
            val aktuell = einstellungen.stand.value.themen
            if (aktuell.none { it.id == thema.id && it.text == text }) return@withContext false
            einstellungen.setzeThemen(
                aktuell.map { if (it.id == thema.id && it.text == text) it.copy(ueberschrift = ueberschrift, ueberschriftFuer = text) else it },
            )
            true
        }
        if (!gesetzt) return
        KompassLog.info("ThemenUeberschriften", "erzeuge", "Überschrift gesetzt", mapOf("woerter" to ueberschrift.split(' ').size))
    }

    private companion object {
        const val WARTEZEIT_MS = 2_500L
    }
}
