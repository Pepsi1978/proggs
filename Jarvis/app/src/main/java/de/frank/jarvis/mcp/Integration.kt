package de.frank.jarvis.mcp

import de.frank.jarvis.faehigkeit.Ergebnis
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
    const val VERSION = "2"
    const val STAND = "17.10.2026"

    /** Die Kurzfassung für Werkzeugbeschreibungen (wenige Sätze). */
    const val KURZ = "Zu Beginn jedes Gesprächs einmal jarvis_kontext aufrufen (kurzer Startkontext mit Franks Regeln, Zielen, Entscheidungen, Neuem, Tageslage und offenen Hintergrundaufträgen); " +
        "Details gezielt nachladen statt alles; neue dauerhafte Angaben und Entscheidungen von Frank mit jarvis_merken zurückgeben; lange Arbeit mit agent_starten, Ergebnisse mit vorgaenge."

    /** Die Grundregeln im Wortlaut. */
    val REGELN: String = """
        JARVIS-INTEGRATION v$VERSION (Stand $STAND), Grundregeln für die Zusammenarbeit:
        1. START: Ist Jarvis in einem Gespräch verfügbar, rufe vor deiner ersten Antwort einmal jarvis_kontext auf (Vorgabe: kurz). Er enthält Franks Regeln, Ziele, Entscheidungen, Neues, die Tageslage und offene Hintergrundaufträge. Verlass dich nicht auf frühere Chats.
        2. NACHLADEN: Details nur bei Bedarf: jarvis_kontext mit bereiche (zum Beispiel tagebuch, biodaten, ideen), gedaechtnis_suchen für Früheres und frühere Entscheidungen, die App-Werkzeuge für Aktuelles und Änderungen. Jeder Bereich nennt seinen Datenstand; prüfe ihn, bevor du Werte als aktuell darstellst. In längeren Gesprächen holt jarvis_kontext mit seit (STAND-MARKE des letzten Abrufs) nur das Geänderte.
        3. ZURÜCKGEBEN: Neue dauerhafte Angaben, Regeln, Ziele und ausdrückliche Entscheidungen von Frank sofort mit jarvis_merken festhalten; eine Korrektur ersetzt den alten Eintrag über seine id. Nur was Frank sagt oder bestätigt: eigene Vorschläge erst nach seinem Ja, offene Gedanken mit idee_speichern. Nie etwas als beschlossen oder erledigt darstellen, was es nicht ist.
        4. LANGE ARBEIT: Recherchen, Ausarbeitungen und alles, was länger als etwa eine Minute dauert, mit agent_starten. Du bekommst eine Vorgangskennung; angenommen ist nicht fertig. Sag es Frank kurz und rede weiter.
        5. ERGEBNISSE: Jarvis kann ChatGPT nicht von sich aus ansprechen. Fertiges erfährst du über den Startkontext und über „VORGANG FERTIG“ an Werkzeug-Ergebnissen: dann mit vorgaenge (aktion lesen) holen, Frank an passender Stelle sagen und mit vorgaenge (aktion mitgeteilt) bestätigen. Nicht wiederholt abfragen, während etwas läuft.
        6. VERLÄSSLICHKEIT: Nach jeder Aktion kurz sagen, was erledigt ist und was noch läuft. Meldet ein Ergebnis, dass die Antwort nicht ankam, erst mit dem passenden Lese-Werkzeug prüfen. Ein identischer schreibender Aufruf kurz hintereinander wird nicht doppelt ausgeführt.
    """.trimIndent()
}

/**
 * Schutz vor doppelter Ausführung schreibender Aufrufe. ChatGPT wiederholt einen Aufruf, wenn die Antwort nicht ankam
 * (Funkloch, Zeitlimit am Relay), oft mit genau denselben Angaben, während der erste noch läuft oder gerade fertig wurde.
 * - Läuft derselbe Aufruf noch (der Relay gibt nach 75 Sekunden auf, der Aufruf läuft auf dem Handy weiter), wartet die
 *   Wiederholung auf sein Ergebnis, statt ihn ein zweites Mal zu starten.
 * - Ist er innerhalb von [fensterMs] erfolgreich gewesen, bekommt die Wiederholung dasselbe Ergebnis (die Antwort ging auf
 *   dem Rückweg verloren). Mehrere verschiedene Aufrufe desselben Werkzeugs (Aufgabe A und B) werden je für sich gemerkt.
 *   Ein erfolgreicher Aufruf eines ANDEREN Werkzeugs (anlegen, löschen, wieder anlegen) hebt das Gemerkte auf.
 * Fehler werden nicht gemerkt: Nach einem Fehler darf wiederholt werden. Grenze: Löscht Frank etwas in der App selbst und
 * lässt es binnen [fensterMs] mit genau denselben Angaben wieder anlegen, gilt das als Wiederholung (das Ergebnis sagt es dazu).
 */
class Wiederholschutz(private val fensterMs: Long = 45_000L, private val uhr: () -> Long = System::currentTimeMillis) {
    private val laufend = HashMap<String, CompletableDeferred<Ergebnis>>()
    /** Erfolgreiche Aufrufe der letzten [fensterMs]: Schlüssel → (Zeit, Ergebnis). */
    private val zuletzt = LinkedHashMap<String, Pair<Long, Ergebnis>>()

    private fun werkzeug(schluessel: String) = schluessel.substringBefore('|')

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
                    zuletzt.entries.removeAll { werkzeug(it.key) != name }
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
