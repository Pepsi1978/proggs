package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.data.Gespraeche
import de.frank.jarvis.data.GespeicherteNachricht
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Bringt Meldungen eines Programmierauftrags in das Jarvis-Gespräch, aus dem er gestartet wurde.
 *
 * [aktivesGespraech] setzt das Gespräch in der App, solange Jarvis auf eine Frage von Frank antwortet; startet in dieser
 * Zeit ein Programmierauftrag, merkt sich das Auftragsbuch das Gespräch. Aufträge aus ChatGPT haben kein Gespräch.
 * Ist das Gespräch gerade in der App offen, fügt die App die Nachricht selbst an ([empfaenger]); sonst wird sie an die
 * gespeicherte Gesprächsdatei gehängt und erscheint beim nächsten Öffnen.
 */
object ProgrammierChat {
    /** Nachrichtenart einer kurzen Statusmeldung der Sitzung (App gefunden, Bau, Push, ✅ Fertig, ❌ Problem): Punkt, keine Leiste. */
    const val ART_STATUS = "programmieren"

    /** Nachrichtenart des ausführlichen Abschlussberichts am Zugende (Stand „info“ vom Hook): volle Aktionsleiste. */
    const val ART_BERICHT = "programmierbericht"

    @Volatile var aktivesGespraech: String? = null

    /**
     * Die Kennung des Gesprächs im Koroutinen-Kontext der laufenden Antwort. Arbeiten mehrere Gespräche gleichzeitig, findet
     * ein Programmierauftrag so sein eigenes Gespräch; [aktivesGespraech] bleibt der Ersatz für Aufrufer ohne Kennung.
     */
    class GespraechKennung(val id: String) : kotlin.coroutines.AbstractCoroutineContextElement(GespraechKennung) {
        companion object Key : kotlin.coroutines.CoroutineContext.Key<GespraechKennung>
    }

    /** Ob die Sitzung eines Auftrags gerade arbeitet, je Gespräch: für die drehende Anzeige unter der letzten Nachricht. */
    data class Aktivitaet(val app: String, val arbeitet: Boolean, val zuletzt: Long, val schritt: String, val start: Long = 0L)

    private val _aktivitaet = MutableStateFlow<Map<String, Aktivitaet>>(emptyMap())
    val aktivitaet: StateFlow<Map<String, Aktivitaet>> = _aktivitaet

    fun setze(gespraech: String, a: Aktivitaet) {
        if (gespraech.isEmpty()) return
        _aktivitaet.value = _aktivitaet.value + (gespraech to a)
    }

    /** Setzt die offene App: true = sie hat die Nachricht übernommen (Gespräch ist gerade offen). */
    @Volatile var empfaenger: ((gespraech: String, nachricht: GespeicherteNachricht) -> Boolean)? = null

    fun poste(context: Context, gespraech: String, text: String, art: String, kontext: String = "") {
        val nachricht = GespeicherteNachricht(false, text, art, kontext)
        if (runCatching { empfaenger?.invoke(gespraech, nachricht) }.getOrNull() == true) return
        Gespraeche.anhaengen(context, gespraech, nachricht)
    }

    /** Markiert eine Rückfrage im Gespräch als beantwortet (Knöpfe verschwinden, die Antwort steht darunter). */
    @Volatile var beantwortet: ((gespraech: String, frageId: String, antwort: String) -> Boolean)? = null

    fun alsBeantwortet(context: Context, gespraech: String, frageId: String, antwort: String) {
        if (runCatching { beantwortet?.invoke(gespraech, frageId, antwort) }.getOrNull() == true) return
        Gespraeche.rueckfrageBeantwortet(context, gespraech, frageId, antwort)
    }
}
