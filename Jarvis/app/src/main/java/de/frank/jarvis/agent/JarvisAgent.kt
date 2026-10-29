package de.frank.jarvis.agent

import android.content.Context
import de.frank.jarvis.ablage.AgentenKontext
import de.frank.jarvis.auth.ChatTurn
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Protokoll
import de.frank.jarvis.data.Quelle
import de.frank.jarvis.faehigkeit.Programmierregeln
import de.frank.jarvis.faehigkeit.merkKontext
import de.frank.jarvis.faehigkeit.merkKontextOhneSymbiose
import de.frank.jarvis.faehigkeit.ohneNull
import de.frank.jarvis.faehigkeit.Register
import de.frank.jarvis.gedaechtnis.Lagebild
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.text.WerkzeugMarkup
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Der geprüfte Zustand eines Agentenlaufs, wenn sein Zeitfenster abgelaufen ist. Nur Fakten, die Jarvis wirklich kennt;
 * was er nicht wissen kann (wie viel Arbeit noch fehlt), steht in [bericht] ausdrücklich als unbekannt.
 * [unterbrochen] = was beim Zeitablauf gerade lief (null = zwischen zwei Schritten), [fehlerFolge] = zuletzt gescheiterte Werkzeugaufrufe in Folge.
 */
data class Zeitablauf(
    val zeitfenster: Int,
    val minuten: Long,
    val schritte: Int,
    val maxSchritte: Int,
    val genutzt: Map<String, Int>,
    val fehler: Int,
    val fehlerFolge: Int,
    val letzterFehler: String?,
    val aenderungen: List<String>,
    val unterbrochen: String?,
    val unterbrochenSeitMs: Long,
    val letzterSchrittVorMs: Long?,
    /** > 0 = nicht die Zeit, sondern Franks Tokenlimit ist erreicht: so viele Token je Durchgang, [tokenVerbraucht] davon verbraucht. */
    val tokenGrenze: Int = 0,
    val tokenVerbraucht: Int = 0,
) {
    /** Welche Grenze erreicht ist, als Satzteil („nach Erreichen …“). */
    val anlass: String get() = if (tokenGrenze > 0) "des Tokenlimits (${Einstellungen.tokenlimitLabel(tokenGrenze)})" else "des internen Zeitlimits ($minuten Minuten)"

    /** Läuft die Arbeit normal, hängt sie oder ist sie gestört? Eine Zeile. */
    val zustand: String get() = when {
        tokenGrenze > 0 && fehlerFolge < 3 -> "Läuft: Der letzte Schritt ist fertig, nur das Tokenlimit ist ausgeschöpft."
        fehlerFolge >= 3 -> "Gestört: Die letzten $fehlerFolge Werkzeugaufrufe sind gescheitert (zuletzt $letzterFehler). Mehr Zeit allein hilft hier vermutlich nicht."
        unterbrochen != null && unterbrochenSeitMs >= HAENGT_MS ->
            "Hängt vermutlich: „$unterbrochen“ lief seit ${unterbrochenSeitMs / 60_000} Minuten ohne Ergebnis und wurde unterbrochen. Ob es beim nächsten Versuch klappt, ist unbekannt."
        schritte == 0 -> "Unklar: Es ist noch kein einziger Schritt fertig geworden."
        else -> "Läuft und braucht nur mehr Zeit: Der letzte Schritt ist vor ${minutenText(letzterSchrittVorMs ?: 0L)} fertig geworden" +
            (if (unterbrochen != null) ", gerade lief „$unterbrochen“ (wird beim Weiterarbeiten wiederholt)." else ".")
    }

    /** Kurzfassung für Vorgangsbuch und Startseite. */
    fun kurz(): String = (if (tokenGrenze > 0) "Tokenlimit erreicht (${tokenText(tokenVerbraucht)} von ${tokenText(tokenGrenze)} Token) nach $minuten Minuten, "
        else "Zeitlimit erreicht nach $minuten Minuten, ") + "$schritte " + (if (maxSchritte > 0) "von $maxSchritte " else "") + "Schritten. " + zustand

    /** Der vollständige Statusbericht für Frank, mit der Frage am Ende. */
    fun bericht(agent: String): String = if (tokenGrenze > 0) buildString {
        append("Das Tokenlimit ist erreicht: ${tokenText(tokenVerbraucht)} von ${tokenText(tokenGrenze)} Token verbraucht. Aufgabe weitermachen oder abbrechen?\n\n")
        append("**Zustand:** ").append(zustand).append("\n\n")
        append("**Bisher erledigt** (").append(minuten).append(" Minuten):\n")
        append("- ").append(schritte).append(if (maxSchritte > 0) " von $maxSchritte Schritten genutzt" else " Schritte (Schrittlimit unbegrenzt)").append('\n')
        if (genutzt.isNotEmpty()) append("- Werkzeuge: ").append(genutzt.entries.joinToString { (t, n) -> if (n > 1) "$t ($n×)" else t }).append('\n')
        if (aenderungen.isNotEmpty()) append("- Änderungen: ").append(aenderungen.joinToString("; ").take(600)).append('\n')
        append("\n**Weiterarbeiten:** Der Agent $agent setzt denselben Auftrag mit seinem Arbeitsstand fort, mit weiteren ${tokenText(tokenGrenze)} Token.")
        append(" **Abbrechen:** Er schreibt einen Zwischenstand aus dem bisher Gesammelten.")
    } else buildString {
        append("Das interne Zeitlimit ist erreicht. Abbrechen oder weiterarbeiten?\n\n")
        append("**Zustand:** ").append(zustand).append("\n\n")
        append("**Bisher erledigt** (").append(minuten).append(" Minuten, ").append(zeitfenster).append(if (zeitfenster == 1) ". Zeitfenster" else ". Zeitfenster in Folge").append("):\n")
        append("- ").append(schritte).append(if (maxSchritte > 0) " von $maxSchritte Schritten genutzt" else " Schritte (Schrittlimit unbegrenzt)").append('\n')
        if (genutzt.isEmpty()) append("- noch kein Werkzeug fertig benutzt\n")
        else append("- Werkzeuge: ").append(genutzt.entries.joinToString { (t, n) -> if (n > 1) "$t ($n×)" else t }).append('\n')
        if (aenderungen.isNotEmpty()) append("- Änderungen: ").append(aenderungen.joinToString("; ").take(600)).append('\n')
        if (fehler > 0) append("- ").append(fehler).append(if (fehler == 1) " Werkzeugaufruf gescheitert" else " Werkzeugaufrufe gescheitert").append(letzterFehler?.let { ", zuletzt $it" }.orEmpty()).append('\n')
        append("\n**Unbekannt:** wie viel Arbeit noch fehlt und ob weitere ${Agenten.ZEITLIMIT_MINUTEN} Minuten reichen.\n\n")
        append("**Weiterarbeiten:** Der Agent $agent setzt denselben Auftrag mit seinem Arbeitsstand ${Agenten.ZEITLIMIT_MINUTEN} Minuten fort")
        append(if (maxSchritte > 0) ", höchstens noch ${(maxSchritte - schritte).coerceAtLeast(0)} Schritte." else ".")
        append(" **Abbrechen:** Er schreibt einen Zwischenstand aus dem bisher Gesammelten.")
    }

    private fun minutenText(ms: Long): String = if (ms < 60_000) "${ms / 1000} Sekunden" else "${ms / 60_000} Minuten"
    private fun tokenText(n: Int): String = Einstellungen.tokenText(n)

    companion object {
        /** So lange ohne Ergebnis gilt ein einzelnes Werkzeug oder ein Modellschritt als vermutlich hängend. */
        const val HAENGT_MS = 10 * 60_000L
        /** Ob ein Text aus [kurz] oder [bericht] zum Tokenlimit gehört; für Stellen, die nur diesen Text sehen (Benachrichtigung, Vorgangsbuch). */
        fun istTokenlimit(text: String): Boolean = text.startsWith("Tokenlimit erreicht") || text.startsWith("Das Tokenlimit")
        /** „Tokenlimit“ oder „Zeitlimit“, passend zu [text]. */
        fun grenze(text: String): String = if (istTokenlimit(text)) "Tokenlimit" else "Zeitlimit"
        /** Die Frage an Frank, passend zu [text]. */
        fun frage(text: String): String = if (istTokenlimit(text)) "Das Tokenlimit ist erreicht. Abbrechen oder weiterarbeiten?" else "Das interne Zeitlimit ist erreicht. Abbrechen oder weiterarbeiten?"
    }
}

/**
 * Das eigene Denken von Jarvis: ein Modell aus der ChatGPT-Anmeldung, das die Werkzeuge aller angebundenen
 * Apps selbst bedient. Die Schleife läuft auf dem Handy: Modell entscheidet → Werkzeug läuft → Ergebnis
 * geht zurück ans Modell, bis eine Antwort steht.
 *
 * Werkzeugaufrufe laufen über ein festes JSON-Format im Antworttext. Das hält die Anbindung unabhängig
 * vom Modell: Jedes Modell, das Text liefert, kann Jarvis antreiben.
 */
class JarvisAgent(context: Context) {
    private val app = context.applicationContext
    val auth = CodexAuthManager(app)
    private val einstellungen = Einstellungen.get(app)

    /** Der Satz, den Frank liest, wenn keine Antwort zustande kam (nach [versuche] mit null); [grund] aus deren beiAbbruch. */
    fun ersatzText(grund: String?): String =
        "Ich bin nicht fertig geworden" + (grund?.let { " ($it)" } ?: "") + ". Für große Aufgaben wie Recherchen oder Berichte sag mir, " +
            "ich soll den Recherche-Agenten starten; der arbeitet im Hintergrund weiter. Was genau passiert ist, steht unter Aktivität."

    val verbunden: Boolean get() = auth.kannDenken(einstellungen.modell)

    /**
     * Beantwortet einen Auftrag und liefert null, wenn keine Antwort zustande kommt (nicht verbunden, Zeitlimit, zu viele
     * Schritte); [beiAbbruch] bekommt dann den Grund. Der Grund geht bewusst nicht in ein Feld dieser Instanz: Chat und
     * Live-Gespräch teilen sich eine, und ein paralleler Lauf überschriebe ihn.
     */
    suspend fun versuche(
        auftrag: String,
        verlauf: List<ChatTurn> = emptyList(),
        zeitlimitMs: Long = 120_000,
        /** Höchstens so viele Bearbeitungsrunden, 0 = unbegrenzt. Das Zeitlimit gilt immer. Der Chat übergibt Franks Einstellung („Schrittlimit pro Anfrage“). */
        maxSchritte: Int = STANDARD_SCHRITTE,
        beiSchritt: (String) -> Unit = {},
        rolle: String? = null,
        mitInternet: Boolean = false,
        ablageTitel: String? = null,
        /** Nur mit [ablageTitel]: false = der Agent soll nichts dauerhaft ablegen (Frank hat keine Ablage verlangt). */
        ablageDauerhaft: Boolean = true,
        lernen: Boolean = false,
        /** true = reichen Schritte oder Zeit nicht, schreibt das Modell zum Schluss eine Antwort aus dem bisher Gesammelten, statt alles zu verwerfen. */
        abschluss: Boolean = false,
        /** Meldet jede gelungene Änderung durch ein schreibendes Werkzeug („Titel: Ergebnis“), damit sie auch bei einem Abbruch nicht verloren geht. */
        beiAenderung: (String) -> Unit = {},
        /** Bilder zur Frage (data-URLs: Fotos, PDF-Seiten); sie gehen nur mit dem neuesten Zug ans Modell. */
        bilder: List<String> = emptyList(),
        /** Modell und Denkstufe nur für diesen Aufruf (Live-Gespräch „Jarvis direkt“); null = wie in den Einstellungen. */
        modell: de.frank.jarvis.auth.CodexModel? = null,
        denkstufe: de.frank.jarvis.auth.ReasoningEffort? = null,
        /** true = Live-Gespräch „Jarvis direkt“: Gesamtbild so, wie es bereitliegt, ohne auf Nachlesen zu warten. */
        schnell: Boolean = false,
        /** Warum der Lauf ohne fertige Antwort endete oder nur einen Zwischenstand schrieb (Schritt- oder Zeitlimit, Modellfehler, zuletzt laufendes Werkzeug). */
        beiAbbruch: (String) -> Unit = {},
        /**
         * Nur Agenten: Ist das Zeitfenster ([zeitlimitMs]) abgelaufen, endet der Lauf nicht, sondern pausiert mit erhaltenem Arbeitsstand
         * und fragt hier nach. null = ein weiteres Zeitfenster gleicher Länge (Schritte zählen weiter, das Schrittlimit bleibt),
         * ein Text = beenden mit Zwischenstand, der Text ist der Grund. Ohne Rückruf endet der Lauf beim Zeitlimit wie bisher.
         */
        beiZeitablauf: (suspend (Zeitablauf) -> String?)? = null,
        /**
         * Franks Tokenlimit (Einstellungen → Jarvis' Kopf) gilt nur mit diesem Rückruf: Haben die Modellschritte zusammen so viele
         * Ausgabe-Token verbraucht, pausiert der Lauf zwischen zwei Schritten und fragt hier. null = noch einmal so viele Token,
         * ein Text = beenden mit Zwischenstand. Ohne Rückruf (Tagesauswertung, Live-Gespräch) gibt es kein Tokenlimit: Niemand könnte antworten.
         */
        beiTokenlimit: (suspend (Zeitablauf) -> String?)? = null,
    ): String? =
        // Nie auf dem Hauptfaden: Anweisung bauen, JSON lesen und Werkzeuge ließen sonst die Oberfläche ruckeln.
        withContext(Dispatchers.Default) {
            versucheIntern(auftrag, verlauf, zeitlimitMs, maxSchritte, beiSchritt, rolle, mitInternet, ablageTitel, ablageDauerhaft, lernen, abschluss, beiAenderung, bilder,
                modell ?: einstellungen.modell, denkstufe ?: einstellungen.denkstufe, schnell, beiAbbruch, beiZeitablauf, beiTokenlimit)
        }

    private suspend fun versucheIntern(
        auftrag: String,
        verlauf: List<ChatTurn>,
        zeitlimitMs: Long,
        maxSchritte: Int,
        beiSchritt: (String) -> Unit,
        rolle: String?,
        mitInternet: Boolean,
        ablageTitel: String?,
        ablageDauerhaft: Boolean,
        lernen: Boolean,
        abschluss: Boolean,
        beiAenderung: (String) -> Unit,
        bilder: List<String>,
        modell: de.frank.jarvis.auth.CodexModel,
        denkstufe: de.frank.jarvis.auth.ReasoningEffort,
        schnell: Boolean,
        beiAbbruch: (String) -> Unit,
        beiZeitablauf: (suspend (Zeitablauf) -> String?)?,
        beiTokenlimit: (suspend (Zeitablauf) -> String?)?,
    ): String? {
        if (!auth.kannDenken(modell)) { beiAbbruch(if (modell.istAnthropic) "Für ${modell.label} fehlt der Anthropic-API-Schlüssel" else "Jarvis ist nicht mit ChatGPT angemeldet"); return null }
        // Regeln und Notizen über Frank schreibt Jarvis nur im Gespräch mit Frank. Agenten und die Tagesauswertung lesen fremde Texte (Internet, Mails) und haben beides nur vor Augen.
        // Agenten beauftragen, programmieren lassen und das Protokoll lesen nur im Gespräch mit Frank: Agenten starten keine Agenten und keine Programmieraufträge.
        // Die Websuche haben Agenten mit Internet und jedes Gespräch mit Frank (aktuelle Fragen direkt beantworten statt einen Agenten zu starten), die Tagesauswertung nicht.
        val faehigkeiten = Register.alle(app).filter { (de.frank.jarvis.faehigkeit.SuchRegel.mitWebsuche(mitInternet, imGespraech = lernen) || it.id != "web") && (lernen || (it.id != "regeln" && it.id != "agenten" && it.id != "programmieren" && it.id != "sprachsession")) }
            // Franks Einnahmeplan ändert nur das Gespräch mit ihm; Agenten und die Tagesauswertung lesen Stack-Labor nur.
            .map { if (!lernen && it is de.frank.jarvis.faehigkeit.StackLaborFaehigkeit) it.nurLesend() else it }
        val werkzeuge = faehigkeiten.flatMap { it.werkzeuge }
        val zuege = (gekuerzt(verlauf) + ChatTurn("user", auftrag, bilder)).toMutableList()
        val beginn = System.currentTimeMillis()
        // Im Gespräch und bei Agenten denkt Jarvis mit dem ganzen Gesamtbild aus dem Live-Gedächtnis (liegt fertig bereit,
        // nur die schnellen Bereiche werden höchstens 2,5 Sekunden nachgelesen). Beim Schreiben der Tagesauswertung selbst
        // genügen Regeln und Wissen über Frank: Die Daten stehen dort im Auftrag, und die alte Auswertung soll die neue nicht färben.
        // Ohne die Symbiose-Regeln: Sie regeln die Zusammenarbeit mit ChatGPT und haben in Franks persönlicher Auswertung nichts zu suchen.
        // Live-Gespräch „Jarvis direkt“: Das Gesamtbild liegt fertig bereit (jede App frischt ihren Bereich selbst auf); nicht erst nachlesen,
        // sonst wartet Frank bei jeder Frage bis zu 2,5 Sekunden, bevor das Denk-Modell überhaupt anfängt.
        // wartenMs = 0: Die schnellen Bereiche frischen im Hintergrund für die nächste Frage auf, ohne dass diese Frage wartet;
        // nur beim allerersten Start (Bereiche fehlen ganz) wird kurz gewartet. Abbruch wird nie verschluckt.
        val gesamtbild = if (schnell) try { withContext(Dispatchers.IO) { de.frank.jarvis.gedaechtnis.Lagebild.kontext(app, wartenMs = 0L) + Programmierregeln.alsKontext(app) } }
            catch (e: CancellationException) { throw e } catch (e: Exception) { Logbuch.fehler("Gesamtbild", e); gesamtbildMitZeitlimit() }
            else if (lernen || rolle != null) gesamtbildMitZeitlimit() else withContext(Dispatchers.IO) { merkKontextOhneSymbiose(app) }
        // Die Anweisung einmal bauen statt bei jedem Schritt: Sie ist groß und ändert sich während einer Antwort nicht.
        val anweisungText = anweisung(faehigkeiten, rolle, gesamtbild)
        Logbuch.dauer("Antwort: Gesamtbild", System.currentTimeMillis() - beginn, "Anweisung ${anweisungText.length} Zeichen, Verlauf ${zuege.size} Züge")
        // Für den Abschluss bleibt ein Teil der Zeit übrig: Ein langes Werkzeug (Bild, Download) darf nicht das ganze Ergebnis verschlucken.
        val reserve = if (abschluss) (zeitlimitMs / 6).coerceIn(25_000L, 180_000L) else 0L
        var schritte = 0
        var alleSchritte = false
        var letztesWerkzeug = ""
        var modellFehler: Exception? = null
        // Für die Zustandsprüfung beim Zeitablauf: was gerade läuft (null = zwischen zwei Schritten), seit wann, was schon erledigt ist.
        var laeuftGerade: String? = null
        var laeuftSeit = 0L
        var letzterSchrittFertig = 0L
        val genutzt = LinkedHashMap<String, Int>()
        var fehlerZahl = 0
        var fehlerFolge = 0
        var letzterFehler: String? = null
        var formNachgefragt = false
        val aenderungen = java.util.Collections.synchronizedList(mutableListOf<String>())
        val meldeAenderung: (String) -> Unit = { aenderungen += it; beiAenderung(it) }
        var zeitfenster = 0
        var endeVonAussen: String? = null
        // Franks Tokenlimit je Durchgang (0 = keines); gezählt werden die Ausgabe-Token aller Modellschritte, Denken eingeschlossen.
        val tokenGrenze = if (beiTokenlimit != null) einstellungen.tokenlimit else 0
        var verbraucht = 0
        var tokenErreicht = false
        // Wo in zuege die Werkzeugergebnisse stehen: Wird das Gesamtbudget überschritten, werden die älteren gekürzt.
        val ergebnisZuege = mutableListOf<Int>()
        var ergebnis: String?
        // Je Durchgang ein Zeitfenster. Ein Fortsetzen nach Zeitablauf behält zuege und schritte: derselbe Auftrag, derselbe Arbeitsstand, dasselbe Schrittbudget.
        while (true) {
            zeitfenster++
            ergebnis = withTimeoutOrNull<String?>(zeitlimitMs - reserve) {
          try {
            // 0 = unbegrenzt: Dann beendet nur die fertige Antwort, das Zeitlimit oder Franks Abbruch die Schleife.
            while (maxSchritte <= 0 || schritte < maxSchritte) {
                schritte++
                val modellBeginn = System.currentTimeMillis()
                laeuftGerade = MODELL_DENKT
                laeuftSeit = modellBeginn
                val roh = auth.streamChat(anweisungText, zuege, modell, denkstufe, onUsage = { verbraucht += it })
                Logbuch.dauer("Antwort: Modellschritt", System.currentTimeMillis() - modellBeginn, "Schritt $schritte, ${modell.label}, ${roh.length} Zeichen")
                var schritt = lies(roh)
                var (aufrufe, verworfen) = schritt?.let { aufrufeAus(it) } ?: (emptyList<JSONObject>() to emptyList<JSONObject>())
                // Werkzeugaufruf als Markup oder fremdes JSON geschrieben: trotzdem ausführen, statt ihn als Antwort vorzulesen.
                if (aufrufe.isEmpty()) {
                    val ausText = WerkzeugMarkup.aufrufe(roh, werkzeuge.map { it.name }.toSet())
                    if (ausText.isNotEmpty()) {
                        Logbuch.aktion("Antwort: Werkzeugaufruf als Text erkannt", ausText.joinToString { it.optString("werkzeug") })
                        schritt = JSONObject().put("werkzeuge", org.json.JSONArray(ausText))
                        aufrufe = ausText.take(MAX_AUFRUFE)
                        verworfen = ausText.drop(MAX_AUFRUFE)
                    }
                }
                if (schritt == null || aufrufe.isEmpty()) {
                    val antwort = schritt?.optString("antwort")?.takeIf { it.isNotBlank() } ?: roh.trim()
                    // Markup ohne ausführbaren Aufruf: einmal um eine saubere Antwort bitten, nie das Markup ausgeben.
                    if (WerkzeugMarkup.enthaelt(antwort) && !formNachgefragt) {
                        formNachgefragt = true
                        zuege += ChatTurn("assistant", roh.take(2_000))
                        zuege += ChatTurn("user", "FORMATFEHLER: Deine letzte Nachricht enthielt technisches Markup statt des vereinbarten JSON. " +
                            "Willst du ein Werkzeug aufrufen, dann nur als {\"werkzeug\": \"<name>\", \"argumente\": { … }}. Sonst antworte mit {\"antwort\": \"…\"} " +
                            "in ganzen deutschen Sätzen, ohne Werkzeugnamen, Parameter, Tags, Pfeile oder JSON im Text.")
                        continue
                    }
                    Logbuch.dauer("Antwort: gesamt", System.currentTimeMillis() - beginn, "$schritte Schritt(e)" + (rolle?.let { ", Agent" } ?: ""))
                    return@withTimeoutOrNull WerkzeugMarkup.entferne(antwort).ifBlank { MARKUP_ERSATZ }
                }
                // Mehrere voneinander unabhängige lesende Werkzeuge laufen gleichzeitig; sobald eines schreibt, alle der Reihe nach.
                val nurLesend = aufrufe.size > 1 && aufrufe.all { c -> werkzeuge.firstOrNull { it.name == c.optString("werkzeug") }?.nurLesen == true }
                // Vor dem Start festhalten: Hängt ein Werkzeug bis zum Zeitlimit, nennt der Abbruchgrund genau dieses.
                letztesWerkzeug = aufrufe.joinToString(", ") { c -> werkzeuge.firstOrNull { it.name == c.optString("werkzeug") }?.titel ?: c.optString("werkzeug") }
                laeuftGerade = letztesWerkzeug
                laeuftSeit = System.currentTimeMillis()
                val ergebnisse = if (nurLesend) {
                    coroutineScope { aufrufe.map { c -> async { fuehreAus(c, werkzeuge, ablageTitel, ablageDauerhaft, beiSchritt, meldeAenderung) } }.awaitAll() }
                } else {
                    aufrufe.map { c -> fuehreAus(c, werkzeuge, ablageTitel, ablageDauerhaft, beiSchritt, meldeAenderung) }
                }
                ergebnisse.forEach { (titel, text) ->
                    genutzt[titel] = (genutzt[titel] ?: 0) + 1
                    if (text.startsWith("FEHLER: ")) { fehlerZahl++; fehlerFolge++; letzterFehler = "$titel: " + text.removePrefix("FEHLER: ").lineSequence().first().take(160) } else fehlerFolge = 0
                }
                laeuftGerade = null
                letzterSchrittFertig = System.currentTimeMillis()
                zuege += ChatTurn("assistant", schritt.toString())
                // Nicht ausgeführte Aufrufe (mehr als MAX_AUFRUFE in einem Schritt) ausdrücklich melden, damit das Modell sie nachholt statt sie zu bestätigen.
                val nichtAusgefuehrt = if (verworfen.isEmpty()) "" else
                    "\n\nNICHT AUSGEFÜHRT (höchstens $MAX_AUFRUFE Werkzeuge je Schritt): " +
                        verworfen.joinToString("; ") { c -> c.optString("werkzeug") + " " + (c.optJSONObject("argumente") ?: JSONObject()).toString().take(300) } +
                        " – rufe sie im nächsten Schritt erneut auf."
                zuege += if (ergebnisse.size == 1) {
                    ChatTurn("user", "WERKZEUG-ERGEBNIS (${aufrufe[0].optString("werkzeug")}):\n${ergebnisse[0].second}$nichtAusgefuehrt")
                } else {
                    ChatTurn("user", "WERKZEUG-ERGEBNISSE:\n" + aufrufe.indices.joinToString("\n\n") { i ->
                        val argumente = (aufrufe[i].optJSONObject("argumente") ?: JSONObject()).toString().take(120)
                        "(${i + 1}. ${aufrufe[i].optString("werkzeug")} $argumente):\n${ergebnisse[i].second}"
                    } + nichtAusgefuehrt)
                }
                ergebnisZuege += zuege.lastIndex
                kuerzeAeltere(zuege, ergebnisZuege, ERGEBNIS_BUDGET)
                // Tokenlimit ausgeschöpft: zwischen zwei Schritten anhalten, unten wird Frank gefragt. Eine fertige Antwort geht immer durch.
                if (tokenGrenze > 0 && verbraucht >= tokenGrenze && (maxSchritte <= 0 || schritte < maxSchritte)) { tokenErreicht = true; return@withTimeoutOrNull null }
            }
            alleSchritte = true
            null
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            // Ein Modellfehler (HTTP 400 etwa wegen der Kontextlänge) mitten in der Arbeit: Mit Abschluss wird das Gesammelte nicht
            // verworfen, sondern unten zum Zwischenstand. Ohne Abschluss entscheidet der Aufrufer wie bisher. Abgelaufene Anmeldung
            // und ausgeschöpftes Kontingent gehen immer an den Aufrufer: Der zeigt dafür gezielt „bitte anmelden“ bzw. „Kontingent“,
            // und ein Zwischenstand käme ohnehin nicht zustande.
            if (!abschluss || (e is de.frank.jarvis.auth.CodexAuthException && e.kind != de.frank.jarvis.auth.AuthErrorKind.NETWORK)) throw e
            Logbuch.fehler("Antwort: Modellschritt", e)
            modellFehler = e
            null
          }
            }
            if (ergebnis != null || alleSchritte || modellFehler != null) break
            if (tokenErreicht && beiTokenlimit != null) {
                // Tokenlimit erreicht: fragen; weiter = derselbe Arbeitsstand mit einem neuen Kontingent gleicher Größe.
                tokenErreicht = false
                val stand = Zeitablauf(
                    zeitfenster = zeitfenster, minuten = (System.currentTimeMillis() - beginn) / 60_000, schritte = schritte, maxSchritte = maxSchritte,
                    genutzt = genutzt.toMap(), fehler = fehlerZahl, fehlerFolge = fehlerFolge, letzterFehler = letzterFehler, aenderungen = aenderungen.toList(),
                    unterbrochen = null, unterbrochenSeitMs = 0L, letzterSchrittVorMs = 0L, tokenGrenze = tokenGrenze, tokenVerbraucht = verbraucht,
                )
                endeVonAussen = beiTokenlimit(stand)
                if (endeVonAussen != null) break
                verbraucht = 0
                // Kein abgelaufenes Zeitfenster: Der nächste Durchgang zählt nicht als weiteres Fenster in Folge.
                zeitfenster--
                val letzter = zuege.lastIndex
                zuege[letzter] = zuege[letzter].copy(text = zuege[letzter].text + "\n\nFORTSETZUNG: Das Tokenlimit war erreicht; Frank hat entschieden, dass du weiterarbeitest " +
                    "(weitere ${Einstellungen.tokenlimitLabel(tokenGrenze)}). Mach dort weiter, wo du warst.")
                continue
            }
            // Fertig, Schrittlimit, Modellfehler oder ohne Rückfrage (Chat, Tagesauswertung): wie bisher weiter unten.
            if (beiZeitablauf == null) break
            // Zeitfenster abgelaufen: Zustand festhalten und fragen, statt die Arbeit zu verwerfen.
            val jetzt = System.currentTimeMillis()
            val unterbrochen = laeuftGerade
            // Mitten im Denken unterbrochen: Dieser Schritt hat nichts geliefert und zählt nicht gegen das Schrittlimit.
            if (unterbrochen == MODELL_DENKT) schritte--
            laeuftGerade = null
            val stand = Zeitablauf(
                zeitfenster = zeitfenster, minuten = (jetzt - beginn) / 60_000, schritte = schritte, maxSchritte = maxSchritte,
                genutzt = genutzt.toMap(), fehler = fehlerZahl, fehlerFolge = fehlerFolge, letzterFehler = letzterFehler, aenderungen = aenderungen.toList(),
                unterbrochen = unterbrochen, unterbrochenSeitMs = if (unterbrochen == null) 0L else jetzt - laeuftSeit,
                letzterSchrittVorMs = if (letzterSchrittFertig == 0L) null else jetzt - letzterSchrittFertig,
            )
            endeVonAussen = beiZeitablauf(stand)
            if (endeVonAussen != null) break
            // Weiter: Das Modell erfährt, dass der letzte Schritt unterbrochen wurde und was schon erledigt ist (nichts doppelt ausführen).
            val rest = if (maxSchritte <= 0) "unbegrenzt viele Schritte" else "noch ${maxSchritte - schritte} von $maxSchritte Schritten"
            val notiz = "FORTSETZUNG: Das interne Zeitlimit war erreicht; Frank hat entschieden, dass du weiterarbeitest (weitere ${dauer(zeitlimitMs)}, $rest). " +
                (if (unterbrochen != null) "Der unterbrochene Schritt („$unterbrochen“) hat kein Ergebnis geliefert; rufe Nötiges erneut auf. " else "") +
                (if (aenderungen.isEmpty()) "" else "Bereits bestätigte Änderungen, NICHT noch einmal ausführen: ${aenderungen.joinToString("; ").take(1_500)}. ") +
                "Mach dort weiter, wo du warst."
            val letzter = zuege.lastIndex
            zuege[letzter] = zuege[letzter].copy(text = zuege[letzter].text + "\n\n" + notiz)
        }
        ergebnis?.let { return it }
        // Noch nichts gesammelt: Der Fehler geht wie bisher an den Aufrufer (er zeigt etwa „Kontingent ausgeschöpft“ oder „bitte anmelden“).
        modellFehler?.let { if (ergebnisZuege.isEmpty()) throw it }
        val grund = when {
            modellFehler != null -> "Fehler beim Modell: ${modellFehler?.message ?: modellFehler?.javaClass?.simpleName}"
            alleSchritte -> "Schrittlimit von $maxSchritte Schritten erreicht"
            endeVonAussen != null -> endeVonAussen.orEmpty()
            else -> "Zeitlimit von ${dauer(zeitlimitMs - reserve)} erreicht"
        } + (if (letztesWerkzeug.isEmpty()) "" else ", zuletzt: $letztesWerkzeug")
        if (!abschluss || schritte == 0) {
            beiAbbruch(grund)
            Protokoll.melde(Quelle.JARVIS, "Abbruch", grund, ok = false)
            return null
        }
        // Nach einem Modellfehler (oft die Kontextlänge) alle Ergebnisse knapp halten, damit der Zwischenstand überhaupt durchgeht.
        if (modellFehler != null) kuerzeAeltere(zuege, ergebnisZuege + Int.MAX_VALUE, 0)
        zuege += ChatTurn("user", "ABSCHLUSS: $grund. Rufe KEIN Werkzeug mehr auf. Antworte jetzt sofort mit {\"antwort\": \"…\"} und schreibe das Ergebnis aus allem, " +
            "was du bisher hast. Sag am Anfang in einem Satz, dass es ein Zwischenstand ist und was noch fehlt. Nenne nur Dateien, deren Speicherung ein Werkzeug-Ergebnis bestätigt hat.")
        val ende = withTimeoutOrNull(reserve) {
            runCatching { auth.streamChat(anweisungText, zuege, modell, denkstufe) }.getOrNull()
        }
        val antwort = ende?.let { roh -> lies(roh)?.optString("antwort")?.takeIf { it.isNotBlank() } ?: roh.trim().takeIf { it.isNotEmpty() && !it.trimStart().startsWith("{") } }
            ?.let { WerkzeugMarkup.entferne(it).ifBlank { null } }
        val endgrund = if (antwort != null) "$grund; Zwischenstand geschrieben" else "$grund; auch der Zwischenstand kam nicht zustande"
        beiAbbruch(endgrund)
        Protokoll.melde(Quelle.JARVIS, "Abbruch", endgrund, ok = false)
        return antwort
    }

    /**
     * Hält die Werkzeugergebnisse im Verlauf unter [budget] Zeichen: Von den ältesten an werden Ergebnisse auf
     * [ALT_ZEICHEN] gekürzt, bis die Summe passt. Das jüngste Ergebnis (letzter Eintrag in [indizes]) bleibt immer
     * vollständig; ein Index außerhalb von [zuege] als letzter Eintrag schließt deshalb alle in das Kürzen ein.
     */
    private fun kuerzeAeltere(zuege: MutableList<ChatTurn>, indizes: List<Int>, budget: Int) {
        val gueltig = indizes.filter { it in zuege.indices }
        var summe = gueltig.sumOf { zuege[it].text.length }
        if (summe <= budget) return
        for (i in indizes.dropLast(1)) {
            if (summe <= budget) break
            if (i !in zuege.indices) continue
            val alt = zuege[i]
            if (alt.text.length <= ALT_ZEICHEN) continue
            val kurz = alt.text.take(ALT_ZEICHEN) + "\n… (älteres Werkzeugergebnis gekürzt, vorher ${alt.text.length} Zeichen; bei Bedarf das Werkzeug erneut aufrufen)"
            summe -= alt.text.length - kurz.length
            zuege[i] = alt.copy(text = kurz)
        }
    }

    /**
     * Die Werkzeugaufrufe einer Modellantwort: ein einzelner ({"werkzeug": …}) oder mehrere ({"werkzeuge": [ … ]}).
     * Liefert die auszuführenden (höchstens [MAX_AUFRUFE]) und die verworfenen; Letztere bekommt das Modell als
     * „NICHT AUSGEFÜHRT“ zurück und ruft sie im nächsten Schritt erneut auf.
     */
    private fun aufrufeAus(schritt: JSONObject): Pair<List<JSONObject>, List<JSONObject>> {
        schritt.optJSONArray("werkzeuge")?.let { a ->
            val liste = (0 until a.length()).mapNotNull { a.optJSONObject(it) }.filter { it.optString("werkzeug").isNotEmpty() }
            if (liste.isNotEmpty()) return liste.take(MAX_AUFRUFE) to liste.drop(MAX_AUFRUFE)
        }
        return (if (schritt.optString("werkzeug").isNotEmpty()) listOf(schritt) else emptyList()) to emptyList()
    }

    /** Führt einen Aufruf aus; liefert den Titel fürs Protokoll und den Text fürs Modell. Ein Fehler bleibt beim einzelnen Werkzeug. */
    private suspend fun fuehreAus(
        aufruf: JSONObject,
        werkzeuge: List<de.frank.jarvis.faehigkeit.Werkzeug>,
        ablageTitel: String?,
        ablageDauerhaft: Boolean,
        beiSchritt: (String) -> Unit,
        beiAenderung: (String) -> Unit,
    ): Pair<String, String> {
        val name = aufruf.optString("werkzeug")
        val werkzeug = werkzeuge.firstOrNull { it.name == name }
        // JSON-null zählt als nicht angegeben (sonst läse das Werkzeug den Text „null“).
        val argumente = (aufruf.optJSONObject("argumente") ?: JSONObject()).ohneNull()
        val titel = werkzeug?.titel ?: name
        beiSchritt(titel)
        if (werkzeug == null) return titel to "FEHLER: Das Werkzeug $name gibt es nicht."
        // Werkzeuge dürfen hier auf lange Arbeit warten (Bild, Download); Dateien ohne Titel gehören zum Agenten-Eintrag.
        // Sie lesen Dateien und Kalender: deshalb auf IO, nie auf dem Hauptfaden.
        val werkzeugBeginn = System.currentTimeMillis()
        val r = try {
            withContext(Dispatchers.IO + AgentenKontext(ablageTitel, langeWarten = ablageTitel != null, dauerhaft = ablageDauerhaft)) { werkzeug.ausfuehren(argumente) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logbuch.fehler("Werkzeug $name", e)
            de.frank.jarvis.faehigkeit.Ergebnis("Werkzeug fehlgeschlagen: ${e.message}", fehler = true)
        } catch (e: Throwable) {
            // Auch ein Error (etwa OutOfMemoryError beim Dekodieren eines großen Bildes) bleibt beim Werkzeug, statt die App zu beenden.
            Logbuch.fehler("Werkzeug $name", e)
            de.frank.jarvis.faehigkeit.Ergebnis("Werkzeug fehlgeschlagen: ${e.javaClass.simpleName} ${e.message.orEmpty()}".trim(), fehler = true)
        }
        Logbuch.dauer("Werkzeug: $name", System.currentTimeMillis() - werkzeugBeginn, if (r.fehler) "Fehler: " + r.text.take(160) else "${r.text.length} Zeichen")
        Protokoll.melde(Quelle.JARVIS, werkzeug.titel, r.text, !r.fehler)
        if (!werkzeug.nurLesen && !r.fehler) {
            // Was Jarvis selbst geändert hat, steht sofort im Live-Gedächtnis.
            Lagebild.nachAktion(app, name, "Jarvis")
            beiAenderung(werkzeug.titel + ": " + r.text.lineSequence().firstOrNull().orEmpty().take(200))
        }
        // Ein einzelnes Ergebnis höchstens so lang wie die größten Werkzeuge ohnehin liefern (Ablage-Text); ältere kürzt kuerzeAeltere.
        val text = if (r.text.length <= EIN_ERGEBNIS) r.text else r.text.take(EIN_ERGEBNIS) + "\n… (gekürzt, ${r.text.length} Zeichen)"
        return titel to ((if (r.fehler) "FEHLER: " else "") + text)
    }

    /**
     * Das Gesamtbild aus dem Live-Gedächtnis, höchstens [GESAMTBILD_MS] lang abgewartet. Der Aufbau läuft abgekoppelt:
     * Hängt eine Partner-App in einem blockierenden Aufruf, kommt die Frage trotzdem frei und hat Regeln, Ziele und Wissen.
     */
    private suspend fun gesamtbildMitZeitlimit(): String {
        val beginn = System.currentTimeMillis()
        val aufbau = HINTERGRUND.async { Gehirn.kontext(app) }
        val bild = try {
            withTimeoutOrNull(GESAMTBILD_MS) { aufbau.await() }
        } catch (e: CancellationException) {
            aufbau.cancel()
            throw e
        } catch (e: Exception) {
            Logbuch.fehler("Gesamtbild", e)
            null
        }
        // Das Live-Gedächtnis enthält die Programmierregeln nicht mehr (es geht auch an Programmier-Sitzungen); Jarvis selbst braucht sie.
        if (bild != null) return bild + withContext(Dispatchers.IO) { Programmierregeln.alsKontext(app) }
        aufbau.cancel()
        Logbuch.gedaechtnis("Gesamtbild: Zeitlimit oder Fehler, nur Gemerktes", "", System.currentTimeMillis() - beginn)
        return "GESAMTBILD NICHT VERFÜGBAR (Zeitlimit): nur Regeln, Ziele und Wissen liegen vor; lies Termine und Aufgaben selbst.\n" +
            withContext(Dispatchers.IO) { merkKontext(app) }
    }

    /**
     * Der Gesprächsverlauf nach Zeichenbudget statt nach fester Anzahl: von hinten so viele Wechsel, wie in
     * [VERLAUF_ZEICHEN] passen (höchstens [VERLAUF_ZUEGE]). Fällt etwas weg, steht ein Hinweis davor.
     */
    private fun gekuerzt(verlauf: List<ChatTurn>): List<ChatTurn> {
        val letzter = verlauf.lastOrNull() ?: return emptyList()
        // Der jüngste Zug bleibt immer, notfalls auf seinen Schluss gekürzt: Ohne ihn verlöre jede Rückfrage ihren Bezug.
        val juengster = if (letzter.text.length > VERLAUF_ZEICHEN) letzter.copy(text = "(gekürzt) …" + letzter.text.takeLast(VERLAUF_ZEICHEN)) else letzter
        var summe = juengster.text.length
        val davor = verlauf.dropLast(1).asReversed().takeWhile { z -> summe += z.text.length; summe <= VERLAUF_ZEICHEN }.take(VERLAUF_ZUEGE - 1).reversed()
        val behalten = davor + juengster
        val ausgelassen = behalten.size < verlauf.size || juengster !== letzter
        return if (ausgelassen) listOf(ChatTurn("user", "(Ältere Teile dieses Gesprächs sind ausgelassen.)")) + behalten else behalten
    }

    private fun dauer(ms: Long): String = if (ms < 120_000L) "${ms / 1000} Sekunden" else "${ms / 60_000} Minuten"

    /**
     * Die Anweisung ans Modell. Vorn steht der große gleichbleibende Teil (Grundsätze, App-Hinweise, Werkzeuge,
     * Antwortformat), hinten das, was sich ändert (Uhrzeit, Gesamtbild, Rolle): So kann der Dienst den Anfang
     * über Fragen hinweg aus seinem Prompt-Cache nehmen.
     */
    private fun anweisung(faehigkeiten: List<de.frank.jarvis.faehigkeit.Faehigkeit>, rolle: String?, gesamtbild: String): String {
        val jetzt = LocalDateTime.now()
        val datum = jetzt.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy, HH:mm 'Uhr'", Locale.GERMAN))
        return buildString {
            append(festerTeil(faehigkeiten))
            append("\n\nLAGE JETZT:\n")
            append("Jetzt ist ").append(datum).append(" (ISO-Datum ").append(jetzt.toLocalDate()).append(").\n\n")
            if (gesamtbild.isNotEmpty()) append(gesamtbild).append('\n')
            if (rolle != null) append('\n').append(rolle).append('\n')
            // Das Antwortformat steht weit oben; am Ende kurz daran erinnern, ohne den gleichbleibenden Anfang zu ändern.
            append("\nAntworte jetzt wie unter ANTWORTFORMAT beschrieben: genau ein JSON-Objekt und sonst nichts.")
        }
    }

    /** Der gleichbleibende Teil der Anweisung, je Auswahl an Fähigkeiten einmal im Prozess gebaut. */
    private fun festerTeil(faehigkeiten: List<de.frank.jarvis.faehigkeit.Faehigkeit>): String =
        FESTE_TEILE.getOrPut(faehigkeiten.joinToString(",") { it.id }) {
            buildString {
                append("Du bist Jarvis, Franks persönlicher Assistent auf seinem Handy. Du sprichst Frank mit „du“ an, antwortest so kurz, wie es die Frage erlaubt, ")
                append("klar und freundlich in gutem Deutsch, ohne ids.\n")
                append("FORMAT (die App zeigt leichtes Markdown schön an, beim Vorlesen fällt es weg): Eine kurze Antwort bleibt ein oder zwei Sätze ohne Formatierung. ")
                append("Längeres gliederst du: kurze Absätze mit Leerzeile dazwischen; ab drei Punkten Stichpunkte mit „- “; bei längeren Antworten mit mehreren Teilen je Teil eine kleine Überschrift mit „### “; ")
                append("die wichtigsten Fakten (Zeiten, Zahlen, Ergebnisse) **fett**. Ein Symbol (✅ ⚠️ 📅 ⏰ 💤 ❤️ 🏃 💡 …) setzt du nur an den Anfang einer Zeile, nie mitten in einen Satz; ")
                append("zusammengehörige Angaben bleiben in einer Zeile, z. B. „✅ Genialer Wecker installiert · Version 1.1.119“. Keine Tabellen, keine verschachtelten Listen, keine Codeblöcke ohne Bitte. ")
                append("Franks Stilregeln unter GEMERKT (FRANKS ANTWORT-STIL) gehen diesen Formatvorgaben vor.\n")
                append("Datum, Uhrzeit, das Gesamtbild und gegebenenfalls deine Rolle stehen ganz am Ende unter LAGE JETZT; die Rolle geht allgemeinen Vorgaben vor.\n\n")
                append("Du arbeitest mit Franks Apps über Werkzeuge. Grundsätze:\n")
                append("- Handle selbstständig: Lies nach, bevor du fragst. Frage nur, wenn eine nötige Angabe wirklich fehlt oder mehrdeutig ist.\n")
                append("- Erfinde nie Daten. Was du über Aufgaben sagst, stammt aus einem Werkzeug-Ergebnis.\n")
                append("- Nach einer Änderung bestätigst du in einem Satz, was jetzt gilt. Scheitert ein Werkzeug oder geht eine Änderung nicht, sag das in einem Satz mit dem Grund. ")
                append("Überspringe nie still eine Bitte: Jede gewünschte Änderung rufst du wirklich auf und bestätigst sie, nichts gilt als erledigt ohne Werkzeug-Ergebnis.\n")
                append("- Antworten an Frank sind Klartext in ganzen deutschen Sätzen. Werkzeugnamen, Kennungen, Parameter, Tags wie invoke oder parameter, „name=“, Pfeile, spitze Klammern ")
                append("und JSON gehören nie in den Antworttext; ein Werkzeug rufst du ausschließlich im JSON-Format unter ANTWORTFORMAT auf.\n")
                append("- Jede Antwort richtet sich an Frank als Menschen, der sie liest oder vorgelesen hört. Versetz dich vor dem Schreiben in ihn: ")
                append("Versteht er das beim einmaligen Hören ohne Technikwissen? Dann schreib es so, natürlich und freundlich, Werkzeug-Ergebnisse in deinen eigenen Worten statt als Rohtext.\n")
                append("- Behaupte nie, eine Datei, ein Bild oder ein Dokument erzeugt oder gespeichert zu haben, wenn ein Werkzeug-Ergebnis das nicht mit „Gespeichert“ bestätigt. ")
                append("Meldet ein Werkzeug „läuft noch“ oder einen Fehler, sag genau das.\n\n")
                // Nur im Gespräch mit Frank (dort gibt es agent_starten): die feste Regel Websuche statt Agent, vor den App-Hinweisen.
                // Agenten selbst bekommen sie nicht, ihre gründliche Recherche ist ja gerade beauftragt.
                if (faehigkeiten.any { it.id == "agenten" }) append(de.frank.jarvis.faehigkeit.SuchRegel.REGEL).append("\n\n")
                faehigkeiten.forEach { append("App ").append(it.name).append(": ").append(it.hinweise).append("\n\n") }
                append("WERKZEUGE:\n")
                faehigkeiten.flatMap { it.werkzeuge }.forEach { w ->
                    append("- ").append(w.name).append(": ").append(w.beschreibung).append("\n  Eingabe (JSON-Schema): ").append(w.schema.toString()).append('\n')
                }
                append("\nANTWORTFORMAT — antworte IMMER mit genau einem JSON-Objekt und sonst nichts:\n")
                append("Ein Werkzeug aufrufen: {\"werkzeug\": \"<name>\", \"argumente\": { … }}\n")
                append("Mehrere Werkzeuge auf einmal: {\"werkzeuge\": [{\"werkzeug\": \"<name>\", \"argumente\": { … }}, …]}\n")
                append("Frank antworten oder nachfragen: {\"antwort\": \"<Text>\"}\n")
                append("Brauchst du mehrere voneinander unabhängige Angaben (etwa Termine, Aufgaben und Wetter), rufe alle lesenden Werkzeuge in EINEM Schritt über werkzeuge auf; ")
                append("sie laufen dann gleichzeitig. Hängt ein Aufruf vom Ergebnis eines anderen ab oder änderst du etwas, rufe die Werkzeuge nacheinander auf. ")
                append("Die Ergebnisse bekommst du als nächste Nachricht, die mit WERKZEUG-ERGEBNIS beginnt.")
            }
        }

    /** Holt das JSON-Objekt aus der Modellantwort, auch wenn es in Text oder Codezäune eingepackt ist. */
    private fun lies(roh: String): JSONObject? {
        val start = roh.indexOf('{')
        val ende = roh.lastIndexOf('}')
        if (start < 0 || ende <= start) return null
        return runCatching { JSONObject(roh.substring(start, ende + 1)) }.getOrNull()
            ?.takeIf { it.has("werkzeug") || it.has("werkzeuge") || it.has("antwort") }
    }

    companion object {
        /** Bearbeitungsrunden je Lauf, wenn der Aufrufer nichts anderes sagt (der Chat übergibt Franks Schrittlimit, das Live-Gespräch fest 8). */
        const val STANDARD_SCHRITTE = 8
        /** Längstes einzelnes Werkzeugergebnis im Verlauf (so viel liefert schon das größte Werkzeug, ablage_lesen). */
        private const val EIN_ERGEBNIS = 60_000
        /** Großzügiges Gesamtbudget aller Werkzeugergebnisse eines Laufs; darüber werden die älteren gekürzt. */
        private const val ERGEBNIS_BUDGET = 300_000
        /** Auf so viele Zeichen schrumpft ein älteres Ergebnis, wenn das Budget überschritten ist. */
        private const val ALT_ZEICHEN = 2_000
        private const val VERLAUF_ZEICHEN = 24_000
        private const val VERLAUF_ZUEGE = 40
        /**
         * Länger wartet eine Frage nicht auf das Gesamtbild. Liegt über der Erstlade-Wartezeit im Lagebild (12 s, wenn
         * Bereiche fehlen) plus Puffer, damit die erste Frage nach dem Start das Teilbild bekommt statt nur Gemerktes.
         */
        private const val GESAMTBILD_MS = 15_000L
        /** Höchstens so viele Werkzeugaufrufe laufen in einem Schritt; der Rest geht als „NICHT AUSGEFÜHRT“ ans Modell zurück. */
        private const val MAX_AUFRUFE = 6
        /** Was läuft, solange das Modell an einem Schritt denkt (für [Zeitablauf.unterbrochen]). */
        const val MODELL_DENKT = "Das Modell denkt nach"
        /** Bestand eine Antwort nur aus technischem Markup, sagt Jarvis das statt nichts oder Markup. */
        const val MARKUP_ERSATZ = "Das hat nicht geklappt: Meine Antwort kam technisch fehlerhaft an. Sag es bitte noch einmal."
        const val NICHT_VERBUNDEN = "Ich bin noch nicht mit ChatGPT verbunden. Bitte in Jarvis unter Einstellungen anmelden."
        /** Abgekoppelter Raum für das Gesamtbild: Ein hängender Aufbau hält die Frage nicht fest. */
        private val HINTERGRUND = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val FESTE_TEILE = java.util.concurrent.ConcurrentHashMap<String, String>()
    }
}
