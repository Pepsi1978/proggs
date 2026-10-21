package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.data.Gespraeche
import de.frank.jarvis.data.GespeicherteNachricht
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Bringt Meldungen eines Programmierauftrags in das Jarvis-Gespräch, aus dem er gestartet wurde.
 *
 * [aktivesGespraech] setzt das Gespräch in der App, solange Jarvis auf eine Frage von Frank antwortet; startet in dieser
 * Zeit ein Programmierauftrag, merkt sich das Auftragsbuch das Gespräch. Aufträge aus ChatGPT bekommen ein eigenes ([neuesGespraech]).
 * Ist das Gespräch gerade in der App offen, fügt die App die Nachricht selbst an ([empfaenger]); sonst wird sie an die
 * gespeicherte Gesprächsdatei gehängt und erscheint beim nächsten Öffnen.
 */
object ProgrammierChat {
    /** Nachrichtenart einer kurzen Statusmeldung der Sitzung (App gefunden, Bau, Push, ✅ Fertig, ❌ Problem): Punkt, keine Leiste. */
    const val ART_STATUS = "programmieren"

    /** Nachrichtenart des ausführlichen Abschlussberichts am Zugende (Stand „info“ vom Hook): volle Aktionsleiste. */
    const val ART_BERICHT = "programmierbericht"

    /** Nachrichtenart eines Bildes, das Jarvis im Gespräch erzeugt hat: groß im Chat, mit „In Ablage verschieben“. */
    const val ART_BILD = "bild"

    /** Nachrichtenart des Ergebnisses eines Hintergrund-Agenten, das in sein Gespräch zurückkommt (Bericht samt Bildern). */
    const val ART_AGENT = "agentenergebnis"

    @Volatile var aktivesGespraech: String? = null

    /**
     * Die Kennung des Gesprächs im Koroutinen-Kontext der laufenden Antwort. Arbeiten mehrere Gespräche gleichzeitig, findet
     * ein Programmierauftrag so sein eigenes Gespräch; [aktivesGespraech] bleibt der Ersatz für Aufrufer ohne Kennung.
     */
    class GespraechKennung(val id: String) : kotlin.coroutines.AbstractCoroutineContextElement(GespraechKennung) {
        companion object Key : kotlin.coroutines.CoroutineContext.Key<GespraechKennung>
    }

    /**
     * Markiert einen Aufruf, der über das Plugin kommt (ChatGPT, aber auch jeder andere MCP-Client wie Claude; der Relay unterscheidet sie nicht). Ein Programmierauftrag von dort hat kein Jarvis-Gespräch;
     * er bekommt dann ein eigenes neues, wie eine Programmier-Session aus der App (nicht das gerade offene Gespräch).
     */
    class AusChatGpt : kotlin.coroutines.AbstractCoroutineContextElement(AusChatGpt) {
        companion object Key : kotlin.coroutines.CoroutineContext.Key<AusChatGpt>
    }

    /**
     * Franks eigene Worte der laufenden Runde im Koroutinen-Kontext: daran prüft programmier_nachtrag, ob Frank einen Nachtrag
     * wirklich ausdrücklich verlangt hat, statt der Deutung des Modells zu folgen. [streng] im Standard-Chat (getippt oder
     * eingesprochen): nur diese Worte zählen. Im Live-Modus ist die Mitschrift unsicher; dort zählen auch die Worte des Modells.
     * [juengste] = nur Franks aktuelle Äußerung; daran prüft agent_starten den Recherche-Wunsch ([SuchRegel.darfStarten]).
     */
    class FranksWorte(val text: String, val streng: Boolean, val juengste: String = text) : kotlin.coroutines.AbstractCoroutineContextElement(FranksWorte) {
        companion object Key : kotlin.coroutines.CoroutineContext.Key<FranksWorte>
    }

    /** Anfang der ersten Nachricht eines eigenen Auftrags-Gesprächs: Sie ist der Auftrag, nicht Franks Wort an Jarvis. */
    const val AUFTRAG_VORSPANN = "An den Programmierer ("
    private const val PLUGIN_VORSPANN = "Über das Jarvis-Plugin (ChatGPT/MCP) an den Programmierer ("

    fun istAuftragsVorspann(text: String) = text.startsWith(AUFTRAG_VORSPANN) || text.startsWith(PLUGIN_VORSPANN)

    /**
     * Legt für einen Programmierauftrag ein eigenes Gespräch an: Franks Auftrag als erste Nachricht, fester Titel. Aus ChatGPT
     * immer, in der App für eine ausdrücklich neue Session oder wenn das Gespräch schon einen Auftrag hat ([ueberPlugin] false).
     * Leer = ließ sich nicht anlegen (dann läuft der Auftrag ohne Gespräch, wie bisher). [ausGespraech]: das Gespräch, aus dem
     * der Auftrag kam; gehört es zu einem Projekt, kommt das neue Gespräch in dasselbe Projekt.
     */
    fun neuesGespraech(context: Context, app: String, auftrag: String, ueberPlugin: Boolean = true, ausGespraech: String = ""): String {
        val id = Gespraeche.neueId()
        val erste = GespeicherteNachricht(true, (if (ueberPlugin) PLUGIN_VORSPANN else AUFTRAG_VORSPANN) + "$app):\n\n$auftrag")
        val projekt = if (ausGespraech.isEmpty()) "" else Gespraeche.laden(context, ausGespraech)?.first?.projekt.orEmpty()
        Gespraeche.speichern(context, id, System.currentTimeMillis(), listOf(erste), projekt = projekt, programmierung = true) ?: return ""
        Gespraeche.umbenennen(context, id, "Programmieren: $app" + if (ueberPlugin) " (über ChatGPT/MCP)" else "")
        return id
    }

    /** Ob die Sitzung eines Auftrags gerade arbeitet, je Gespräch: für die drehende Anzeige unter der letzten Nachricht. */
    data class Aktivitaet(val app: String, val arbeitet: Boolean, val zuletzt: Long, val schritt: String, val start: Long = 0L)

    private val _aktivitaet = MutableStateFlow<Map<String, Aktivitaet>>(emptyMap())
    val aktivitaet: StateFlow<Map<String, Aktivitaet>> = _aktivitaet

    fun setze(gespraech: String, a: Aktivitaet) {
        if (gespraech.isEmpty()) return
        // Atomar: Puls vom Relay und Meldungen der Werkzeuge kommen auf verschiedenen Fäden.
        _aktivitaet.update { it + (gespraech to a) }
    }

    /** Setzt die offene App: true = sie hat die Nachricht übernommen (Gespräch ist gerade offen). */
    @Volatile var empfaenger: ((gespraech: String, nachricht: GespeicherteNachricht) -> Boolean)? = null

    fun poste(context: Context, gespraech: String, text: String, art: String, kontext: String = "", anhaenge: List<String> = emptyList()) {
        val nachricht = GespeicherteNachricht(false, text, art, kontext, anhaenge)
        if (runCatching { empfaenger?.invoke(gespraech, nachricht) }.getOrNull() == true) return
        Gespraeche.anhaengen(context, gespraech, nachricht)
    }

    /**
     * Ändert eine Rückfrage im offenen Gespräch der App (Stand, Antwort, Entwurf): null = das Gespräch ist gerade nicht offen,
     * sonst true, wenn die Rückfrage darin steht. [aenderung] bekommt eine Kopie des Kontexts und liefert false für „nichts ändern“.
     */
    @Volatile var aendern: ((gespraech: String, frageId: String, aenderung: (org.json.JSONObject) -> Boolean) -> Boolean?)? = null

    /** Ändert eine Rückfrage, im offenen Gespräch oder in der Datei. true = sie steht im Gespräch. */
    fun rueckfrageAendern(context: Context, gespraech: String, frageId: String, aenderung: (org.json.JSONObject) -> Boolean): Boolean {
        runCatching { aendern?.invoke(gespraech, frageId, aenderung) }.getOrNull()?.let { return it }
        return Gespraeche.rueckfrageAendern(context, gespraech, frageId, aenderung)
    }

    /** Setzt den Stand einer Rückfrage (siehe [RueckfrageStand]) samt Antworttext und Grund. */
    fun rueckfrageStand(context: Context, gespraech: String, frageId: String, stand: String, antwort: String = "", grund: String = ""): Boolean =
        rueckfrageAendern(context, gespraech, frageId) { k ->
            // Ein endgültiger Ausgang wird nie zurückgedreht (spät eintreffende Meldungen, Abgleich mit dem Server).
            if (RueckfrageStand.endgueltig(k.optString("status")) && !RueckfrageStand.endgueltig(stand)) return@rueckfrageAendern false
            if (k.optString("status") == stand && k.optString("antwort") == antwort.ifEmpty { k.optString("antwort") }) return@rueckfrageAendern false
            k.put("status", stand).put("beantwortet", stand in RueckfrageStand.BEANTWORTET_ALLE).put("seit", System.currentTimeMillis() / 1000)
            if (antwort.isNotEmpty()) k.put("antwort", antwort)
            if (grund.isNotEmpty()) k.put("grund", grund) else k.remove("grund")
            true
        }
}

/**
 * Stände einer Rückfrage im Gespräch (Kontext „status“). Offen: in Jarvis beantwortbar. Sendet/Fehler: Franks Antwort ist
 * unterwegs bzw. nicht angekommen. Angenommen: der Server hat sie, die Sitzung holt sie ab. Beantwortet: in der Sitzung
 * angekommen (über Jarvis), beantwortet_am_pc: am Rechner oder in der Claude-App. am_pc: Jarvis gab die Frage an den Rechner
 * weiter (Frist um oder „In der Claude-App beantworten“). Erledigt: die Sitzung arbeitete ohne Antwort weiter. Verwaist: die
 * Sitzung wartet nicht mehr.
 */
object RueckfrageStand {
    const val OFFEN = "offen"
    const val SENDET = "sendet"
    const val FEHLER = "fehler"
    const val ANGENOMMEN = "angenommen"
    const val BEANTWORTET = "beantwortet"
    const val AM_PC_BEANTWORTET = "beantwortet_am_pc"
    const val AM_PC = "am_pc"
    const val ERLEDIGT = "erledigt"
    const val VERWAIST = "verwaist"

    val BEANTWORTET_ALLE = setOf(ANGENOMMEN, BEANTWORTET, AM_PC_BEANTWORTET)

    /** Danach ist die Frage in Jarvis nicht mehr beantwortbar. */
    fun endgueltig(stand: String) = stand in setOf(BEANTWORTET, AM_PC_BEANTWORTET, AM_PC, ERLEDIGT, VERWAIST)

    /** In Jarvis noch zu beantworten (auch nach einem Fehler beim Senden). */
    fun beantwortbar(stand: String) = stand.isEmpty() || stand == OFFEN || stand == FEHLER

    /**
     * Stand eines Kontexts; ältere Rückfragen kannten nur „beantwortet“. Ein „sendet“, das länger als 90 Sekunden steht (App
     * beim Senden beendet), gilt als Fehler: Frank kann erneut senden, die gleiche Antwort zählt beim Server nie doppelt.
     */
    fun von(k: org.json.JSONObject): String {
        val stand = k.optString("status").ifEmpty { if (k.optBoolean("beantwortet")) BEANTWORTET else OFFEN }
        return if (stand == SENDET && System.currentTimeMillis() / 1000 - k.optLong("seit") > 90) FEHLER else stand
    }
}
