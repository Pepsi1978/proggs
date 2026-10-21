package de.frank.jarvis.mcp

import de.frank.jarvis.faehigkeit.Ergebnis
import de.frank.jarvis.faehigkeit.SuchRegel
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die Grundregeln der Zusammenarbeit zwischen ChatGPT und Jarvis, an EINER Stelle und versioniert. Sie stehen in der
 * Anleitung des Plugins (initialize), in der Beschreibung von jarvis_kontext und am Anfang seines Ergebnisses. Sicher beim
 * Modell an kommen nur Werkzeugbeschreibungen und -ergebnisse; ob ChatGPT die Anleitung aus initialize liest, ist nicht belegt.
 * Bei jeder inhaltlichen Änderung der Regeln [VERSION] erhöhen und [STAND] mitziehen.
 */
object Integration {
    const val VERSION = "4"
    const val STAND = "21.10.2026"

    /** Die Kurzfassung für Werkzeugbeschreibungen (wenige Sätze). */
    const val KURZ = "Zu Beginn jedes Gesprächs einmal jarvis_kontext aufrufen (kurzer Startkontext mit den Symbiose-Regeln, Franks Regeln, Zielen, Entscheidungen, Neuem, Tageslage und offenen Hintergrundaufträgen); " +
        "Details gezielt nachladen statt alles; neue dauerhafte Angaben und Entscheidungen von Frank mit jarvis_merken zurückgeben; aktuelle Fragen sofort selbst mit Websuche beantworten, " +
        "agent_starten nur auf Franks ausdrücklichen Recherche-Wunsch, Ergebnisse mit vorgaenge."

    /**
     * Die technischen Ergänzungen. Die Regeln der Zusammenarbeit selbst stehen im Block SYMBIOSE (Merkdatei [de.frank.jarvis.faehigkeit.Symbiose],
     * von Frank in der App bearbeitet und 1:1 in ChatGPT eingetragen); hier nur, was dort aus Platzgründen fehlt.
     */
    val REGELN: String = """
        JARVIS-INTEGRATION v$VERSION (Stand $STAND): Die Regeln der Zusammenarbeit stehen im Block SYMBIOSE CHATGPT LIVE ⇄ JARVIS (Franks benutzerdefinierte Anweisungen, nach Wichtigkeit geordnet); er geht allem hier vor, außer der Regel WEBSUCHE STATT AGENT unten (Franks Vorgabe vom 21.10.2026; sie gilt auch gegen ältere Fassungen der Symbiose). Fest, auch wenn dort etwas fehlt:
        - Zu Beginn jedes Gesprächs einmal jarvis_kontext (Vorgabe kurz). Erledigt ist nur, was ein Werkzeug bestätigt; angenommen heißt nicht fertig. Nach jeder Aktion kurz sagen, was erledigt ist und was noch läuft.
        - Neue dauerhafte Angaben und ausdrückliche Entscheidungen von Frank mit jarvis_merken zurückgeben; eigene Vorschläge erst nach seinem Ja. Hintergrund-Agenten (agent_starten) nur auf Franks ausdrücklichen Wunsch.
        - ${SuchRegel.REGEL}
        Technisch dazu:
        - Datenstand: Jeder Bereich trägt [Stand …]. In längeren Gesprächen holt jarvis_kontext mit seit (die STAND-MARKE des letzten Abrufs) nur das Geänderte; umfang voll liefert alles, bereiche einzelne Bereiche vollständig.
        - Vorgänge: agent_starten liefert eine Kennung; vorgaenge (aktion lesen) holt das Ergebnis, aktion mitgeteilt bestätigt erst, wenn du es Frank gesagt hast. Abbrechen nur mit vorgang_abbrechen. Während etwas läuft, nicht wiederholt abfragen.
        - Wiederholung: Ein identischer schreibender Aufruf kurz hintereinander wird nicht doppelt ausgeführt („WIEDERHOLUNG ERKANNT“). Meldet ein Ergebnis, dass die Antwort nicht ankam, erst mit dem passenden Lese-Werkzeug prüfen.
    """.trimIndent()
}

/**
 * Schutz vor doppelter Ausführung schreibender Aufrufe. ChatGPT wiederholt einen Aufruf, wenn die Antwort nicht ankam
 * (Funkloch, Zeitlimit am Relay), oft mit genau denselben Angaben, während der erste noch läuft oder gerade fertig wurde.
 * - Läuft derselbe Aufruf noch (der Relay gibt nach 75 Sekunden auf, der Aufruf läuft auf dem Handy weiter), wartet die
 *   Wiederholung auf sein Ergebnis, statt ihn ein zweites Mal zu starten.
 * - Ist er innerhalb von [fensterMs] erfolgreich gewesen, bekommt die Wiederholung dasselbe Ergebnis (die Antwort ging auf
 *   dem Rückweg verloren). Mehrere verschiedene Aufrufe desselben Werkzeugs (Aufgabe A und B) werden je für sich gemerkt.
 *   Ein erfolgreicher Aufruf eines ANDEREN Werkzeugs desselben Bereichs (aufgabe_anlegen, aufgabe_loeschen, wieder
 *   aufgabe_anlegen) hebt das Gemerkte dieses Bereichs auf; fremde Bereiche (eine Mail neben einer Aufgabe) bleiben geschützt.
 * Das Fenster ist länger als Zeitlimit des Relays (75 s) plus Wartezeit auf eine neue Verbindung (15 s): Sonst käme die
 * Wiederholung eines langen Aufrufs (Mail mit großem Anhang) erst nach Ablauf an und liefe doppelt.
 * Fehler werden nicht gemerkt: Nach einem Fehler darf wiederholt werden. Grenze: Löscht Frank etwas in der App selbst und
 * lässt es binnen [fensterMs] mit genau denselben Angaben wieder anlegen, gilt das als Wiederholung (das Ergebnis sagt es dazu).
 */
class Wiederholschutz(private val fensterMs: Long = 180_000L, private val uhr: () -> Long = System::currentTimeMillis) {
    private val laufend = HashMap<String, CompletableDeferred<Ergebnis>>()
    /** Erfolgreiche Aufrufe der letzten [fensterMs]: Schlüssel → (Zeit, Ergebnis). */
    private val zuletzt = LinkedHashMap<String, Pair<Long, Ergebnis>>()

    private fun werkzeug(schluessel: String) = schluessel.substringBefore('|')
    /** Der Bereich eines Werkzeugs: der Teil vor dem ersten Unterstrich (aufgabe_anlegen → aufgabe, mail_senden → mail). */
    private fun bereich(schluessel: String) = werkzeug(schluessel).substringBefore('_')

    /** Rückgabe: Ergebnis und ob es von einem früheren, gleichen Aufruf stammt (true = nichts neu ausgeführt). */
    suspend fun fuehreAus(schluessel: String, arbeit: suspend () -> Ergebnis): Pair<Ergebnis, Boolean> {
        var warte: CompletableDeferred<Ergebnis>? = null
        var eigen: CompletableDeferred<Ergebnis>? = null
        var frueher: Ergebnis? = null
        synchronized(this) {
            val jetzt = uhr()
            zuletzt.entries.removeAll { jetzt - it.value.first >= fensterMs }
            when {
                zuletzt[schluessel] != null -> frueher = zuletzt.getValue(schluessel).second
                laufend[schluessel] != null -> warte = laufend[schluessel]
                else -> eigen = CompletableDeferred<Ergebnis>().also { laufend[schluessel] = it }
            }
        }
        frueher?.let { return it to true }
        warte?.let { return it.await() to true }
        val meins = eigen!!
        try {
            val r = arbeit()
            synchronized(this) {
                if (!r.fehler) {
                    val name = werkzeug(schluessel)
                    val gebiet = bereich(schluessel)
                    zuletzt.entries.removeAll { werkzeug(it.key) != name && bereich(it.key) == gebiet }
                    zuletzt[schluessel] = uhr() to r
                }
            }
            meins.complete(r)
            return r to false
        } catch (t: Throwable) {
            meins.completeExceptionally(t)
            throw t
        } finally {
            synchronized(this) { laufend.remove(schluessel) }
        }
    }

    companion object {
        /** Name und Angaben in fester Reihenfolge der Schlüssel, damit gleiche Aufrufe gleich aussehen. */
        fun schluessel(name: String, argumente: JSONObject): String = name + "|" + kanonisch(argumente)

        private fun kanonisch(wert: Any?): String = when (wert) {
            is JSONObject -> wert.keys().asSequence().toList().sorted().joinToString(",", "{", "}") { k -> JSONObject.quote(k) + ":" + kanonisch(wert.opt(k)) }
            is JSONArray -> (0 until wert.length()).joinToString(",", "[", "]") { kanonisch(wert.opt(it)) }
            is String -> JSONObject.quote(wert.trim())
            null, JSONObject.NULL -> "null"
            else -> wert.toString()
        }
    }
}
