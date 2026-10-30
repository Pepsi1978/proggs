package de.frank.jarvis.faehigkeit

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import de.frank.jarvis.R
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.Gespraeche
import de.frank.jarvis.dienst.JarvisDienst
import de.frank.jarvis.observability.Logbuch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Programmieraufträge an Franks Apps: Jarvis formuliert den Auftrag und gibt ihn an ein Programmier-Ziel ab, das im
 * Repo programmiert und die App über UpdateStation ausliefert. Jarvis selbst ändert nie Code (siehe [RepoFaehigkeit]).
 *
 * Zwei Ziele, in dieser Reihenfolge: Franks PC (Claude Code mit Remote Control, schnell, Installation per WLAN-adb),
 * solange dort die PC-Brücke läuft (Jarvis/werkzeuge/pc-bruecke), sonst die Claude-Code-Routine in der Cloud.
 * Beides läuft über Franks Claude-Abo, nicht über bezahlte API-Aufrufe.
 *
 * Fortschritt: Jeder Auftrag bekommt eine Nummer. Am PC melden Hooks der Sitzung (Jarvis/werkzeuge/pc-bruecke/jarvis_hook.py)
 * Schritte und Rückfragen über den Relay ([meldungVomServer]); die Cloud-Sitzung meldet über programmier_fortschritt.
 * Jarvis hält alles im [Auftragsbuch], zeigt je Auftrag eine mitwachsende Benachrichtigung und schreibt jede Meldung in
 * das Jarvis-Gespräch, aus dem der Auftrag kam ([ProgrammierChat]). Rückfragen beantwortet Frank dort per Knopf oder
 * Sprache ([beantworte]); die Antwort geht über den Relay an den wartenden Hook zurück.
 *
 * Anmeldung: Rechner erreichbar (Brücke fragt ab), Claude Code angemeldet und Auftrag aktiv sind getrennte Zustände. Die
 * Brücke meldet den lokalen Anmeldestand (`claude auth status`, nur „Anmeldedaten liegen vor“), die Sitzungen beweisen ihn:
 * Ein „Login expired“ meldet der Hook als Stand [STAND_ANMELDUNG] (Auftrag gesperrt, Benachrichtigung), jede spätere Arbeit
 * hebt die Sperre auf. Eine abgemeldete Brücke startet keine Sitzung; der Auftrag geht dann nicht ungefragt in die Cloud.
 *
 * Am PC oder Mac direkt gestartete Sitzungen (nicht über Jarvis) melden mit dem Feld „lokal“ ([lokaleSitzung]): eigener
 * Eintrag mit fester Nummer aus der session_id, eigenes Gespräch „Gestartet auf dem PC“, lila Anzeige. Löschen des Gesprächs
 * entfernt sie nur aus Jarvis ([lokaleEntfernen]).
 */
/** Was das Auftragsbuch über die Programmieraufträge eines Gesprächs weiß: App, Stand (laeuft, frage, fertig, fehler), letztes Lebenszeichen, Inhalt. */
data class AuftragsLage(val app: String, val stand: String, val aktiv: Long, val inhalt: String)

class ProgrammierFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "programmieren"
    override val name = "Programmieren"
    override val beschreibung = "Programmieraufträge an Franks Apps an Claude Code übergeben; die fertige App kommt über UpdateStation aufs Handy."
    override val hinweise =
        "Wechsel zwischen Gesprächen (Sessions) erledigt die App selbst: „Öffne eine neue Session“, „Gehe in die letzte Session zurück“, " +
            "„Gehe in die Whoop-Session zurück“, „Geh in die andere Programmier-Session“, „Ich möchte in die andere Programmier-Session“. Erreicht dich so ein Wunsch trotzdem, ist das nie ein Programmierauftrag: Starte nichts und bitte Frank, " +
            "den Wechsel genau so zu sagen. " +
            "Fragt Frank, ob sein PC, sein Mac (Rechner, Computer) an oder erreichbar ist oder wohin ein Programmierauftrag gehen würde, rufe pc_status auf. " +
            "Fragt er, wie weit ein Programmierauftrag ist, was die Cloud-Sitzung gerade macht oder ob sie fertig ist, rufe programmierauftraege auf. " +
            "Steht im Gespräch eine offene Rückfrage einer Programmier-Sitzung (❓ mit frage_id und Optionen) und Frank antwortet darauf, gib die Antwort mit " +
            "programmier_antwort weiter: ordne sie der passenden Option zu (genau deren label), sonst gib Franks Worte als Freitext weiter. " +
            "GESPRÄCH MIT DEM PROGRAMMIERER: Spricht Frank den Programmierer der geöffneten Programmier-Session an („Frag doch mal den Programmierer, wie der Stand ist“, " +
            "„frag ihn …“, „sag ihm, er soll …“, „er soll noch …“), nimm programmierer_sprechen (art=frage bzw. wunsch); Ziel ist automatisch die geöffnete Session, " +
            "ein ausdrückliches „Nachtrag“ braucht es dafür nicht. Sag vorher kurz, dass du fragst bzw. es weitergibst, und gib die Antwort danach wieder. " +
            "Läuft im geöffneten Gespräch keine Session, frag Frank, statt eine zu wählen oder neu zu starten. " +
            "NACHTRÄGE nur auf ausdrücklichen Wunsch: programmier_nachtrag nur, wenn Frank selbst einen Nachtrag in eine bestehende Sitzung verlangt („Schick einen Nachtrag " +
            "in die gleiche Session“, „sag dem Programmierer noch …“), und nur in genau die Sitzung, die er bezeichnet. Ordne einen Wunsch nie selbst als Nachtrag ein und " +
            "wähle nie selbst eine Sitzung: gleiche App, ähnliches Thema, ein gerade fertiger Auftrag oder eine vorhandene Sitzung sind kein Grund. Ist unklar, ob Frank einen " +
            "neuen Auftrag oder einen Nachtrag will oder welche Sitzung er meint, frag kurz und sende nichts. Ohne Nachtragswunsch ist ein Programmierwunsch ein neuer Auftrag (außer Frank spricht den Programmierer der geöffneten Session direkt an, siehe oben). " +
            "Bevor du für Jarvis selbst eine neue Funktion vorschlägst oder beauftragst, prüfe mit funktionen_lesen, ob es sie schon gibt. " +
            "Sagt Frank „programmiere“, „bau in App … ein“, „ändere in … dass …“ oder „mach in … einen Knopf/Filter/…“, rufst du programmieren auf. " +
            "Nennt Frank dabei ein Modell (etwa „GPT-6 Astra“, „Kimi K3“), einen Effort, ein Profil, einen Modus oder ein CLI (Claude Code, Codex, OpenCode, Kimi CLI, Antigravity), " +
            "gib genau das in modell, effort, profil, modus und cli mit und lass Fehlendes weg; Jarvis prüft es gegen OpenLauncher und fragt bei Unklarem zurück. " +
            "Den Auftrag schreibst du nach den Regeln am Feld auftrag. Frag nie nach der App: Nennt Frank keine (etwa bei einem Test), lass app weg; die Modellwahl kommt trotzdem und der Auftrag geht so an den Programmierer. Programmiere nie selbst im Gespräch."

    private val app = context.applicationContext
    private val e get() = Einstellungen.get(app)

    // Offene Auswahlkarten von vor einem Neustart (Update aus einer parallelen Sitzung) gelten weiter.
    init { wahlenLaden(app) }

    /** In dieser Reihenfolge versucht; das erste bereite Ziel bekommt den Auftrag. */
    private val ziele: List<ProgrammierZiel> by lazy { listOf(PcBruecke(), CloudRoutine()) }
    /** Sol 6.1 ohne Windows-PC: Codex Cloud mit Franks ChatGPT-Konto (nur für modell=sol, nie automatisch). */
    private val codex by lazy { CodexCloud() }

    override fun stoerung(): String? = if (ziele.none { it.bereit() }) ziele.last().fehlt() else null

    /**
     * Zustand beider Wege in einem Absatz: ob die PC-Brücke gerade abfragt (dann gehen Aufträge an den PC) und ob die
     * Cloud-Routine eingerichtet ist. Steht auch in jarvis_status.
     */
    suspend fun zustand(): String = withContext(Dispatchers.IO) {
        val pc = if (e.serverToken.isBlank()) "PC: unbekannt, Jarvis ist nicht mit seinem Server verbunden." else runCatching {
            STATUS_HTTP.newCall(pcAnfrage()).execute().use { antwort ->
                if (!antwort.isSuccessful) return@use "PC: unbekannt, der Jarvis-Server antwortet mit HTTP ${antwort.code}."
                val daten = JSONObject(antwort.body?.string().orEmpty())
                val rechner = daten.optString("rechner").ifEmpty { "Franks PC" }
                val alle = daten.optJSONArray("rechner_alle")
                if (alle != null) return@use rechnerZustand(alle, daten.optBoolean("pc"), rechner)
                val vor = if (daten.isNull("zuletzt_vor_s")) -1 else daten.optInt("zuletzt_vor_s", -1)
                if (daten.optBoolean("pc")) "PC: $rechner ist an und erreichbar (letzte Meldung vor $vor Sekunden). Programmieraufträge gehen an den PC."
                else "PC: nicht erreichbar" + (if (vor >= 0) " (letzte Meldung von $rechner vor ${zeitspanne(vor)})" else "") +
                    ". Entweder ist der PC aus, im Standby oder die PC-Brücke läuft nicht. Programmieraufträge gehen in die Cloud."
            }
        }.getOrElse { "PC: unbekannt, der Jarvis-Server ist nicht erreichbar (${it.message ?: it.javaClass.simpleName})." }
        val cloud = if (e.routineUrl.startsWith("https://") && e.routineToken.isNotBlank()) "Cloud: eingerichtet (Claude-Code-Routine mit Token)."
            else "Cloud: nicht eingerichtet, der Token fehlt (Einstellungen → Konten → Programmieren in der Cloud)."
        val sol = if (codex.bereit()) "Sol 6.1 ohne Windows-PC: Codex Cloud über das ChatGPT-Konto (Umgebung „${e.repoName.substringAfterLast('/')}“ aus der Codex-App)."
            else "Sol 6.1 ohne Windows-PC: nicht möglich, Jarvis ist nicht mit dem ChatGPT-Konto verbunden."
        "$pc $cloud $sol"
    }

    /** Franks Reihenfolge aus den Einstellungen für den Relay: „mac“ oder „windows“ zuerst. */
    private fun vorrang() = if (e.macZuerst) "mac" else "windows"

    /**
     * Beide Brücken einzeln: Der Relay meldet den Windows-PC als Rang „haupt“ und den Mac als „ersatz“ (nur die Namen: wer bei
     * Opus Vorrang hat, stellt Frank in den Einstellungen ein, Standard Mac; Sol bleibt am Windows-PC). Ein Rechner, der sich seit
     * dem letzten Serverstart nie gemeldet hat, fehlt in der Liste.
     */
    private fun rechnerZustand(alle: JSONArray, pcDa: Boolean, naechster: String): String {
        val eintraege = (0 until alle.length()).mapNotNull { alle.optJSONObject(it) }
        val haupt = eintraege.firstOrNull { it.optString("rang") == "haupt" }
        val ersatz = eintraege.firstOrNull { it.optString("rang") == "ersatz" }
        fun zeile(r: JSONObject?, art: String): String {
            if (r == null) return "$art: hat sich seit dem letzten Serverstart nicht gemeldet (aus oder die Brücke läuft nicht). Claude-Anmeldung: unbekannt."
            val name = r.optString("rechner").ifEmpty { art }
            val vor = if (r.isNull("zuletzt_vor_s")) -1 else r.optInt("zuletzt_vor_s", -1)
            val seit = if (vor >= 0) " (letzte Meldung vor ${zeitspanne(vor)})" else ""
            val da = r.optBoolean("da")
            return (if (da) "$art $name: an und erreichbar$seit." else "$art $name: nicht erreichbar$seit; aus, im Standby oder die Brücke läuft nicht.") +
                " " + anmeldungZeile(r, da)
        }
        // „rechner“ ist der Rechner, den der Relay für den nächsten Auftrag wählt (der Vorrangrechner nur mit frischer Abfrage).
        val macZuerst = e.macZuerst
        val erster = if (macZuerst) ersatz else haupt
        val zweiter = if (macZuerst) haupt else ersatz
        val (ersterName, zweiterName) = if (macZuerst) "Mac" to "Windows-PC" else "Windows-PC" to "Mac"
        val wohin = when {
            !pcDa -> "Programmieraufträge gehen in die Cloud."
            erster?.optBoolean("da") == true && zweiter?.optBoolean("da") == true && naechster != erster.optString("rechner") ->
                "Der nächste Auftrag ginge an $naechster: Der $ersterName fragt gerade nicht ab, deshalb springt der $zweiterName ein."
            zweiter?.optBoolean("da") == true -> "Der nächste Auftrag ginge an $naechster; der $ersterName hat Vorrang (Einstellungen), ist er aus, springt der $zweiterName ein. Sol 6.1 läuft immer am Windows-PC."
            else -> "Der nächste Auftrag ginge an $naechster."
        }
        return "${zeile(haupt, "Windows-PC")} ${zeile(ersatz, "Mac")} $wohin"
    }

    /**
     * Der Anmeldestand eines Rechners in einem Satz, mit Prüfzeit. Eine erreichbare Brücke ist kein Nachweis: „lokal“ heißt nur,
     * dass Anmeldedaten vorliegen; gültig ist erst, was eine Sitzung bewiesen hat. Ohne Prüfung heißt es „unbekannt“.
     */
    private fun anmeldungZeile(r: JSONObject, da: Boolean): String {
        val b = r.optJSONObject("anmeldung_bruecke")
        val s = r.optJSONObject("anmeldung_sitzung")
        val bStand = b?.optString("stand").orEmpty()
        val bVor = if (b == null || b.isNull("geprueft_vor_s")) -1 else b.optInt("geprueft_vor_s", -1)
        val sVor = s?.optInt("vor_s", -1) ?: -1
        val geprueft = if (bVor >= 0) "geprüft vor ${zeitspanne(bVor)}" else "nie geprüft"
        val veraltet = if (!da && bVor >= 0) " (Stand von vor ${zeitspanne(bVor)}, der Rechner ist gerade nicht erreichbar)" else ""
        return "Claude-Anmeldung: " + when {
            bStand == "abgemeldet" -> "NICHT angemeldet ($geprueft)$veraltet. Claude Code auf dem Rechner öffnen und /login ausführen."
            s?.optString("stand") == "abgelaufen" -> "ABGELAUFEN, gemeldet von Auftrag ${s.optString("auftrag_id")} vor ${zeitspanne(sVor)} („Login expired“) " +
                "und seitdem von keiner Sitzung wieder bestätigt" + (if (bStand == "lokal") "; Anmeldedaten liegen lokal vor ($geprueft), das allein beweist keine gültige Anmeldung" else "") +
                ". Claude Code auf dem Rechner öffnen und /login ausführen."
            s?.optString("stand") == "bestaetigt" -> "gültig, zuletzt bestätigt durch Auftrag ${s.optString("auftrag_id")} vor ${zeitspanne(sVor)}" +
                (if (bStand == "lokal") ", Anmeldedaten lokal $geprueft" else "") + "$veraltet."
            bStand == "lokal" -> "Anmeldedaten liegen vor ($geprueft)$veraltet; ob sie noch gelten, zeigt erst der nächste Auftrag, ein Ablauf wird dann sofort gemeldet."
            else -> "unbekannt, nicht prüfbar (die Brücke meldet keinen Anmeldestand oder die Prüfung schlug fehl)$veraltet."
        }
    }

    /** Lebenszeichen der PC-Sitzungen je Auftrag vom Jarvis-Server (kontakt = letzte Arbeit, warter = Sitzung lebt), oder null. */
    private suspend fun sitzungsLage(): JSONObject? = withContext(Dispatchers.IO) {
        if (e.serverToken.isBlank()) return@withContext null
        runCatching { SITZUNGS_HTTP.newCall(pcAnfrage()).execute().use { a -> if (!a.isSuccessful) null else JSONObject(a.body?.string().orEmpty()).optJSONObject("sitzungen") } }.getOrNull()
    }

    private fun pcAnfrage() = Request.Builder().url("https://${e.serverHost}/geraet/pc?vorrang=${vorrang()}").header("X-Jarvis-Token", e.serverToken).build()

    private fun zeitspanne(sekunden: Int): String = when {
        sekunden < 120 -> "$sekunden Sekunden"
        sekunden < 7_200 -> "${sekunden / 60} Minuten"
        else -> "${sekunden / 3_600} Stunden"
    }

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = "pc_status",
                titel = "Sind Franks Windows-PC und Mac erreichbar und bei Claude angemeldet?",
                beschreibung = "Jarvis: prüft jetzt einzeln, ob Franks Windows-PC und sein Mac an und für Programmieraufträge erreichbar sind (beide Brücken melden sich beim Jarvis-Server), " +
                    "wann sich jeder zuletzt gemeldet hat, ob Claude Code dort angemeldet ist (getrennt: nicht angemeldet, abgelaufen, gültig bestätigt durch eine Sitzung, nur Anmeldedaten " +
                    "vorhanden oder unbekannt, je mit Prüfzeit), wohin der nächste Auftrag ginge und ob der Cloud-Weg eingerichtet ist. Erreichbar heißt nicht angemeldet; sag Frank nie " +
                    "„angemeldet“, wenn hier nur „Anmeldedaten liegen vor“ oder „unbekannt“ steht. Nutze es bei „Ist mein Rechner an?“, „Ist mein PC erreichbar?“, „Ist mein Mac online?“, " +
                    "„Bin ich bei Claude angemeldet?“, „Läuft die PC-Brücke?“ oder „Wohin geht ein Programmierauftrag gerade?“.",
                schema = schema(),
                nurLesen = true,
            ) { Ergebnis(zustand()) },
            Werkzeug(
                name = "programmierauftraege",
                titel = "Programmieraufträge und ihr Fortschritt",
                beschreibung = "Jarvis: zeigt die letzten Programmieraufträge, je mit auftrag_id, App, Ziel (PC oder Cloud), Stand (läuft, Rückfrage, blockiert wegen Anmeldung, " +
                    "angehalten, fertig, Problem), zuletzt gemeldetem Schritt, letzter echter Fortschrittsmeldung und letztem Lebenszeichen getrennt (am PC frisch vom Jarvis-Server: lebt die Sitzung noch?), " +
                    "veraltete Stände gekennzeichnet, Kurzfassung des ursprünglichen Auftrags, einer offenen Rückfrage mit frage_id und Optionen, den Nachträgen mit ihrem Stand " +
                    "(angenommen, zugestellt, bearbeitet) und ob die Sitzung noch Nachträge annehmen kann. Grundlage, um Rückfragen selbst zu beantworten und Nachträge dem " +
                    "richtigen Auftrag zuzuordnen. Nutze es bei „Wie weit ist mein Programmierauftrag?“, „Was macht die Cloud-Sitzung?“, „Ist die App schon fertig?“ oder „Gib mir den Link zur Sitzung“.",
                schema = schema("anzahl" to zahl("Wie viele Aufträge, neueste zuerst; Vorgabe 5.")),
                nurLesen = true,
            ) { a ->
                val anzahl = a.optInt("anzahl", 5).coerceIn(1, 20)
                // Den Server nur fragen, wenn ein PC-Auftrag offen ist (kurzes Zeitlimit; Live-Gespräch hat Vorrang).
                Ergebnis(Auftragsbuch.uebersicht(app, anzahl, if (Auftragsbuch.offenAmPc(app, anzahl)) sitzungsLage() else null))
            },
            Werkzeug(
                name = "programmier_fortschritt",
                titel = "Fortschritt eines Programmierauftrags melden",
                beschreibung = "Nur für die Claude-Code-Sitzung, die einen Programmierauftrag von Jarvis bearbeitet (sie kennt die auftrag_id aus ihrem Auftrag): " +
                    "meldet Frank einen Schritt. Jarvis schreibt ihn ins Gespräch des Auftrags; eine Benachrichtigung gibt es nur bei frage, fertig und fehler. stand=laeuft für Zwischenschritte, stand=frage bevor die Sitzung " +
                    "Frank etwas fragt, stand=fertig oder stand=fehler am Ende mit einer Zusammenfassung in einem Satz. Nicht im Gespräch mit Frank benutzen.",
                schema = schema(
                    "auftrag_id" to text("Die Nummer aus dem Auftrag (AUFTRAG-ID)."),
                    "stand" to text("laeuft, frage, fertig oder fehler.", listOf("laeuft", "frage", "fertig", "fehler")),
                    "schritt" to text("Ein kurzer Satz auf Deutsch, was gerade passiert oder was gefragt wird, zum Beispiel „App gefunden: Aufgaben“ oder „Pull Request #212 gemergt, GitHub baut die APK“."),
                    "link" to text("Optional: Adresse der eigenen Sitzung oder des Pull Requests."),
                    "antwort_auf" to text("Nur als Antwort auf eine Nachricht von Jarvis (FRAGE oder WUNSCH mit Nachricht G…): deren Nummer. schritt ist dann deine Antwort, kurz und gut vorlesbar."),
                    pflicht = listOf("auftrag_id", "stand", "schritt"),
                ),
                nurLesen = false,
            ) { a ->
                val id = a.optString("auftrag_id").trim()
                // Sitzungen von vor dem Update kennen das Feld antwort_auf nicht: Sie beginnen schritt mit „Antwort auf G…:“.
                val vorspann = ANTWORT_VORSPANN.find(a.optString("schritt").trim())
                val bisher = Auftragsbuch.finde(app, id)
                // Nur eine Gesprächsnachricht genau dieses Auftrags zählt (keine N-Nachtragsnummer, keine frage_id, keine fremde G-Nummer).
                // Ohne Angabe: Wartet Jarvis gerade auf die Antwort einer zugestellten Frage, ist die nächste laeuft-Meldung die Antwort.
                val antwortAuf = a.optString("antwort_auf").trim().ifEmpty { vorspann?.groupValues?.get(1).orEmpty() }
                    .takeIf { Auftragsbuch.nachricht(bisher, it) != null }
                    ?: Auftragsbuch.letzteNachricht(bisher)?.takeIf { n -> a.optString("stand") == "laeuft" && n.optString("stand") == "zugestellt" &&
                        n.optString("antwort").isEmpty() && n.optString("text").startsWith(FRAGE_KOPF) && gespraechsAntworten.containsKey(n.optString("id")) }
                        ?.optString("id").orEmpty()
                // Früher hart nach 400 Zeichen abgeschnitten (mitten im Satz); jetzt länger und sichtbar gekürzt.
                val schritt = ProgrammierAbschluss.kuerzen(a.optString("schritt").trim().let { t -> vorspann?.let { t.removeRange(it.range).trim() } ?: t },
                    ProgrammierAbschluss.MAX_FORTSCHRITT)
                val eintrag = Auftragsbuch.melde(app, id, a.optString("stand"), if (antwortAuf.isEmpty()) schritt else "💬 $schritt", a.optString("link").trim())
                if (eintrag == null) Ergebnis("Unbekannte auftrag_id.", fehler = true)
                else {
                    if (antwortAuf.isNotEmpty()) gespraechsAntwort(eintrag, antwortAuf, schritt)
                    // Die Cloud-Sitzung erreicht Jarvis nur über ihre eigenen Meldungen: Nachträge reisen mit der Antwort zurück.
                    // Am PC stellt der Hook zu (sonst käme ein Nachtrag doppelt an).
                    // Nicht bei frage (die Rückfrage bliebe sonst unsichtbar) und nicht bei fehler: dann bleibt er für die nächste Meldung liegen.
                    val nachtraege = if (inDerCloud(eintrag.optString("ziel")) && a.optString("stand") in listOf("laeuft", "fertig"))
                        Auftragsbuch.setzeNachtraege(app, id, null, setOf("angenommen"), "zugestellt") else emptyList()
                    // Geht gerade ein Nachtrag an die Sitzung, arbeitet sie weiter: dann kein „Fertig programmiert“ als Benachrichtigung.
                    verteile(eintrag, null, benachrichtigen = nachtraege.isEmpty())
                    // Wirklich zu Ende (kein Nachtrag unterwegs): im Sprachgespräch gestellt, dann die kurze Ansage. PC (Windows, Mac)
                    // und Cloud melden das Ende gleichermaßen hier; der Aufruf wartet nicht auf das Vorlesen.
                    if (nachtraege.isEmpty()) sprachAbschluss(eintrag)
                    if (nachtraege.isEmpty()) Ergebnis("Gemeldet.")
                    else {
                        Auftragsbuch.melde(app, id, "laeuft", if (nachtraege.all { istNachricht(it.optString("id")) }) "✓ An den Programmierer übergeben."
                            else "📨 Nachtrag an die Sitzung übergeben: " + nachtraege.joinToString(" · ") { anzeige(it).replace('\n', ' ').take(200) }, "")
                            ?.let { verteile(it, null, von = ProgrammierChat.VON_JARVIS) }
                        Ergebnis("Gemeldet.\n\n" + (if (nachtraege.all { istNachricht(it.optString("id")) })
                            "JARVIS SPRICHT MIT DIR zu diesem Auftrag (AUFTRAG-ID ${eintrag.optString("id")}): Reagiere genau wie in der Nachricht beschrieben und arbeite danach an deiner Aufgabe weiter.\n\n"
                        else "NACHTRAG VON FRANK zu diesem Auftrag (AUFTRAG-ID ${eintrag.optString("id")}), über Jarvis. Er gehört zu genau diesem Auftrag: " +
                            "Setze ihn in dieser Sitzung um, schließe danach wieder vollständig ab und melde erneut stand=fertig.\n\n") +
                            nachtraege.joinToString("\n\n") { "NACHTRAG ${it.optString("id")}:\n${it.optString("text")}" })
                    }
                }
            },
            Werkzeug(
                name = "programmier_antwort",
                titel = "Rückfrage einer Programmier-Sitzung beantworten",
                beschreibung = "Jarvis: gibt die Antwort auf eine Rückfrage einer laufenden Programmier-Sitzung weiter (die Rückfrage steht im Gespräch mit ❓ oder in " +
                    "programmierauftraege, mit frage_id und Optionen). Die frage_id bestimmt Auftrag und Sitzung; die Sitzung wartet darauf und arbeitet danach weiter. " +
                    "Beantworte die Rückfrage selbst aus dem Gesprächskontext, dem ursprünglichen Auftrag (Kurzfassung in programmierauftraege) und den verfügbaren Informationen, " +
                    "wenn die Antwort ableitbar ist oder eine Routineentscheidung betrifft, und beschreib die Anforderung dabei bei Bedarf genauer. Frag Frank nur kurz, wenn " +
                    "eine notwendige Angabe oder eine echte Entscheidung von ihm fehlt und sich nicht ableiten lässt; erfinde nie eine Antwort. Ordne die Antwort der passenden " +
                    "Option zu und gib genau deren label an; passt keine, gib sie als Freitext weiter. Für einen Wunsch ohne offene Rückfrage gibt es programmier_nachtrag.",
                schema = schema(
                    "frage_id" to text("Die frage_id der Rückfrage."),
                    "antworten" to textListe("Eine Antwort je Frage, in der Reihenfolge der Fragen: das label der gewählten Option, bei Mehrfachauswahl mehrere labels mit Komma, sonst Franks Worte."),
                    "text" to text("Optional: Franks Antwort als freier Text (bei freien Rückfragen ohne Optionen genügt das)."),
                    pflicht = listOf("frage_id"),
                ),
                nurLesen = false,
            ) { a ->
                val liste = a.optJSONArray("antworten")
                val antworten = (0 until (liste?.length() ?: 0)).map { liste!!.optString(it).trim() }
                runCatching { Ergebnis(beantworte(a.optString("frage_id").trim(), antworten, a.optString("text").trim())) }
                    .getOrElse { Ergebnis(it.message ?: "Die Antwort ging nicht raus.", fehler = true) }
            },
            Werkzeug(
                name = "programmier_nachtrag",
                titel = "Nachtrag zu einem bestehenden Programmierauftrag",
                beschreibung = "Jarvis: schickt einen Nachtrag an einen BESTEHENDEN Programmierauftrag, in genau dessen Jarvis-Session und dieselbe Claude-Code-Sitzung, " +
                    "auch nachdem sie fertig gemeldet hat. NUR wenn Frank selbst ausdrücklich einen Nachtrag in eine bestehende Sitzung verlangt („Schick einen Nachtrag in die " +
                    "gleiche Session an den Programmierer“, „sag dem Programmierer noch …“, „in die laufende Sitzung von Auftrag BD2XW“), und nur in genau die Sitzung, die er " +
                    "bezeichnet. Ordne einen Wunsch nie selbst als Nachtrag ein und wähle nie selbst die Sitzung: gleiche App, ähnliches Thema, ein zuletzt fertiger Auftrag oder " +
                    "eine vorhandene Sitzung sind kein Grund. Ohne ausdrücklichen Nachtragswunsch ist ein Programmierwunsch ein neuer Auftrag (programmieren); ist die Abgrenzung " +
                    "oder die gemeinte Sitzung unklar, frag Frank kurz und sende nichts. Jarvis prüft Franks Worte selbst und sendet ohne ausdrücklichen Wunsch oder bei mehrdeutigem " +
                    "Ziel nichts. Startet nie einen neuen Auftrag und nie eine neue Sitzung, auch nicht als Ersatz. Wartet die Sitzung auf eine Antwort zu ihrer Rückfrage " +
                    "(frage_id), nimm stattdessen programmier_antwort. Das Ergebnis unterscheidet ehrlich: zugestellt, angenommen und eingereiht oder nicht zustellbar; " +
                    "nicht zustellbar sagst du Frank offen, ohne Ersatzauftrag. Derselbe Nachtrag ein zweites Mal wird nicht doppelt zugestellt. " +
                    "Spricht Frank den Programmierer der gerade geöffneten Session direkt an („frag ihn …“, „sag ihm, er soll …“), nimm programmierer_sprechen.",
                schema = schema(
                    "auftrag_id" to text("Die Nummer des Auftrags, den Frank bezeichnet hat (fünf Zeichen, zum Beispiel BD2XW). Über das Plugin Pflicht; in einem Jarvis-Gespräch darf sie fehlen, wenn Frank „diese/die gleiche Session“ sagt und das Gespräch genau einen Auftrag hat."),
                    "text" to text("Der Nachtrag, vollständig und für sich verständlich: was zusätzlich oder anders werden soll, mit dem nötigen Kontext aus dem Gespräch."),
                    "franks_worte" to text("Franks eigene Aufforderung zum Nachtrag, wörtlich (zum Beispiel „Schick einen Nachtrag in die gleiche Session an den Programmierer“). Nie umformuliert, nie erfunden."),
                    pflicht = listOf("text", "franks_worte"),
                ),
                nurLesen = false,
            ) { a -> nachtrag(a.optString("auftrag_id").trim(), DiktatTrenner.auftrag(a.optString("text")), a.optString("franks_worte").trim()) },
            Werkzeug(
                name = "programmierer_sprechen",
                titel = "Mit dem Programmierer der geöffneten Session sprechen",
                beschreibung = "Jarvis: spricht direkt mit dem Programmierer (Claude Code) der gerade geöffneten Programmier-Session, während sie läuft. " +
                    "art=frage: Jarvis stellt dort eine Frage („Frag doch mal den Programmierer, wie der Stand ist“, „frag ihn, warum …“), wartet auf seine Antwort " +
                    "und liefert sie zurück; gib sie Frank kurz und natürlich wieder. art=wunsch: Franks Wunsch an den Programmierer geht direkt in diese Session " +
                    "(„Sag ihm, er soll …“, „er soll noch …“, „sag dem Programmierer, dass …“), auch ohne dass Frank „Nachtrag“ sagt; der Programmierer bestätigt " +
                    "kurz, was er jetzt tut, und setzt es um. art=warten: holt die noch ausstehende Antwort auf die letzte Frage ab (ohne neuen Text). " +
                    "Sag Frank vor dem Aufruf in einem kurzen Satz, dass du fragst bzw. es weitergibst. Ziel ist immer die geöffnete Session (bzw. der Auftrag, der " +
                    "aus dem geöffneten Gespräch gestartet wurde); nie eine andere, nie eine neue. Stellt der Programmierer eine Gegenfrage, kommt sie im Ergebnis " +
                    "(mit frage_id: beantworten mit programmier_antwort, sonst mit einer neuen Nachricht hier): selbst beantworten, wenn sie sich aus Gespräch und " +
                    "Auftrag ergibt, sonst Frank kurz fragen. Eigene Gegenfragen stellst du einfach mit einer weiteren art=frage. Ein ganz neuer Programmierauftrag " +
                    "geht weiter nur über programmieren, eine neue Session nur auf Franks ausdrücklichen Wunsch.",
                schema = schema(
                    "art" to text("frage, wunsch oder warten.", listOf(ART_FRAGE, ART_WUNSCH, ART_WARTEN)),
                    "text" to text("Bei frage die Frage, bei wunsch Franks Wunsch: vollständig und für sich verständlich, mit dem nötigen Kontext aus dem Gespräch. Bei warten leer."),
                    "auftrag_id" to text("Nur über das Plugin und nur, wenn Frank eine bestimmte Session nennt (fünf Zeichen). In Jarvis weglassen: Dort gilt immer die geöffnete Session."),
                    pflicht = listOf("art"),
                ),
                nurLesen = false,
            ) { a -> mitProgrammierer(a.optString("art").trim().lowercase(), DiktatTrenner.auftrag(a.optString("text")).trim(), a.optString("auftrag_id").trim()) },
            Werkzeug(
                name = "programmieren",
                titel = WERKZEUG_TITEL,
                beschreibung = "Jarvis: programmiert etwas in einer von Franks Apps („Jarvis, programmiere in Geniale Aufgaben einen Filter für …“, „bau in den Wecker ein, dass …“, " +
                    "„ändere in Jarvis …“, „schicke bitte folgenden Auftrag an den Programmierer: …“, „schick das an den Programmierer“, „lass das programmieren“). " +
                    "Aus ChatGPT bekommt jeder Auftrag in Jarvis eine eigene Programmier-Session, in der Frank Schritte und Rückfragen sieht. Der Auftrag geht an Claude Code: an Franks PC, wenn er an ist (schnell, die App kommt per WLAN aufs Handy), sonst in die Cloud " +
                    "(etwa zehn bis zwanzig Minuten, dann meldet UpdateStation die neue Version). Es findet die App in Franks Repo und programmiert die Änderung. Dieser Aufruf kehrt sofort zurück. Die Sitzung kennt nur den Auftragstext (Regeln am Feld auftrag). " +
                    "Franks Programmierregeln sind nur für dich: Richte den Auftrag nach ihnen aus " +
                    "(im Plugin vorher mit jarvis_gemerktes_lesen lesen), schreibe ihren Text aber nicht hinein; Jarvis hängt sie nicht an. Ein fertiger Auftrag vom Agenten Code-Analyse oder aus repo_lesen kann unverändert hinein. " +
                    "Frag nie nach der App: Nennt Frank keine eindeutige (etwa bei einem Test), lass app weg; der Auftrag geht trotzdem raus, die Modellwahl kommt wie immer. Jeder Aufruf ist ein NEUER Auftrag mit eigener Sitzung, auch wenn zur selben App oder zum selben " +
                    "Thema schon einer läuft oder gerade fertig wurde; er hängt sich nie an einen alten. Hat das Gespräch schon einen Auftrag, bekommt der neue eine eigene, " +
                    "getrennte Jarvis-Session. In eine bestehende Sitzung geht ein Wunsch nur, wenn Frank ausdrücklich einen Nachtrag dorthin verlangt (programmier_nachtrag) " +
                    "oder den Programmierer der geöffneten Session direkt anspricht („sag ihm, er soll …“: programmierer_sprechen); " +
                    "ist unklar, ob er einen neuen Auftrag oder einen Nachtrag will, frag ihn kurz, bevor du etwas startest. " +
                    "„ ; “ (Leerzeichen, Semikolon, Leerzeichen; früher auch „;“ auf eigener Zeile) in Franks Nachrichten trennt seine einzelnen Spracheingaben und löst allein nie einen Auftrag aus. " +
                    "Verlangt Frank ausdrücklich die Übergabe solcher Abschnitte, ist jeder nicht leere Abschnitt eine Aufgabe: alle zusammen in EINEM Aufruf " +
                    "(eine Sitzung), im Auftrag durch „ ; “ getrennt; ein Trenner am Ende ist keine weitere Aufgabe.",
                schema = schema(
                    "app" to text("Name der App, wie Frank sie nennt, zum Beispiel „Geniale Aufgaben“, „Genialer Wecker“, „Jarvis“; gern mit Ordner im Repo, wenn bekannt. Weglassen, wenn Frank keine nennt."),
                    "auftrag" to text(AUFTRAGS_REGELN),
                    "review" to schalter("Nur bei modell=sol und cli=codex oder bei einem frei gewählten Modell im Codex CLI, Kimi Code CLI oder Antigravity CLI: Franks Antwort, ob ein Review laufen soll, wenn die App oder das APK-Update fertig programmiert ist " +
                        "(Codex kann nicht selbst fragen). Sonst weglassen: Opus und OpenCode fragen am Ende selbst."),
                    "ziel" to text("Nur wenn Frank es ausdrücklich sagt: pc oder cloud. Vorgabe automatisch (PC, wenn er an ist, sonst Cloud).", listOf("automatisch", "pc", "cloud")),
                    "neu" to schalter("true, wenn Frank ausdrücklich eine neue, getrennte Session will: Der Auftrag bekommt dann in jedem Fall eine eigene Jarvis-Session. Vorgabe false."),
                    "modell" to text("Nur wenn Frank es gewählt oder gesagt hat: opus (Claude Opus 5.5, wie bisher) oder sol (Sol 6.1: am Windows-PC im Codex CLI, " +
                        "ist er aus, in Codex Cloud). Nennt Frank ein anderes Modell aus dem OpenLauncher-Angebot (etwa „GPT-6 Astra“, „Kimi K3“, „Claude Fable 5.1“, " +
                        "„Gemini 4 Argon“) oder zu Opus/Sol Effort, Profil, Modus oder ein anderes CLI: dessen Namen so, wie Frank ihn sagt; Jarvis prüft ihn gegen " +
                        "OpenLauncher und startet genau dieses Modell am Mac, ohne Frage nach Opus oder Sol. Weglassen: Jarvis fragt Frank per Knopf."),
                    "effort" to text("Nur wenn Frank ihn nennt: low, medium, high, xhigh oder max (auch „hoch“, „mittel“, „maximal“). Weglassen: high."),
                    "profil" to text("Nur wenn Frank es nennt: minimal, standard oder strikt. Weglassen: minimal."),
                    "modus" to text("Nur wenn Frank ihn nennt: frei, schnell, normal oder gruendlich. Weglassen: schnell."),
                    "cli" to text("Nur wenn Frank es gewählt oder gesagt hat: claude (Claude Code), codex (Codex CLI), opencode (OpenCode), kimi (Kimi Code CLI) oder " +
                        "antigravity (Antigravity CLI). Weglassen: Bietet OpenLauncher dem Modell mehrere CLIs an, fragt Jarvis zurück.",
                        listOf(CLI_CLAUDE, CLI_CODEX, CLI_OPENCODE, CLI_KIMI, CLI_ANTIGRAVITY)),
                    pflicht = listOf("auftrag"),
                ),
                nurLesen = false,
            ) { a ->
                // Franks Wahl vom 24.10.2026: Ohne genannte App geht der Auftrag trotzdem raus (etwa ein reiner Test), mit Modellwahl.
                val appName = a.optString("app").trim().ifEmpty { APP_OHNE }
                // Mehrere Aufgaben einheitlich getrennt und ohne Trenner am Ende (siehe DiktatTrenner.auftrag).
                val auftrag = DiktatTrenner.auftrag(a.optString("auftrag"))
                // Antwortet Frank auf eine offene Modellwahl kurz mit genau einem Modell („Opus 5.5“, „Sol bitte“), gilt das als Wahl,
                // auch wenn das Modell modell nicht mitgab: keine zweite Nachfrage.
                val worte = kotlinx.coroutines.currentCoroutineContext()[ProgrammierChat.FranksWorte]
                // Franks Wahl vom 24.10.2026: In Jarvis kommen bei jedem neuen Auftrag die Knöpfe, auch wenn das Modell modell mitgibt.
                // Früher genügte ein Modellname in Franks letzten zwei Nachrichten (etwa „für Opus 5.5“ als Antwort auf „Für welche
                // App?“), und der Auftrag startete ohne Karte. modell zählt nur noch aus ChatGPT (dort gibt es keine Karte) oder als
                // Antwort auf eine offene Modellkarte dieses Gesprächs.
                val (ausChatGpt, gespraech) = herkunft()
                val modellWert = when (a.optString("modell").trim().lowercase()) { MODELL_SOL -> MODELL_SOL; MODELL_OPUS -> MODELL_OPUS; else -> "" }
                val modell = when {
                    ausChatGpt || gespraech.isEmpty() -> modellWert
                    offeneWahlen.values.any { it.gespraech == gespraech && it.stufe == STUFE_MODELL } ->
                        modellAusAntwort(worte?.aktuell).ifEmpty { modellWert.takeIf { modellGenannt(worte?.aktuell.orEmpty()) }.orEmpty() }
                    else -> ""
                }
                val cli = when (a.optString("cli").trim().lowercase()) { CLI_OPENCODE -> CLI_OPENCODE; CLI_CODEX -> CLI_CODEX; else -> "" }
                val ziel = a.optString("ziel").lowercase()
                // Früher lehnte Jarvis hier ab, sobald zum Gespräch oder zur App schon ein Auftrag lief, und schickte das Modell zu
                // programmier_nachtrag: So landeten neue Wünsche ungefragt als Nachtrag in alten Sitzungen. Jetzt ist jeder Aufruf
                // ein neuer Auftrag mit eigener Sitzung (siehe NachtragsRegel.eigenesGespraech).
                val wahl = zieleFuer(ziel, modell, cli)
                val neu = a.optBoolean("neu")
                // Über den Menüpunkt „Programmierer“ ist es ausdrücklich ein neuer Auftrag: dann nie „schon per Knopf gestartet“.
                val knopf = if (neu || ProgrammiererVorlage.aufgabe(worte?.aktuell.orEmpty()) != null) null else knopfStartIn(herkunft().second, worte?.aktuell.orEmpty())
                // Franks Wahl vom 25.10.2026: Nennt er ein Modell aus dem OpenLauncher-Angebot (oder zu Opus/Sol Effort, Profil, Modus,
                // ein anderes CLI), startet genau das am Mac. Ohne solche Angaben bleibt alles wie bisher (Knöpfe Opus 5.5 / Sol 6.1).
                val katalog = if (auftrag.length < 20 || knopf != null) null else katalogWahl(a, ausChatGpt, worte?.aktuell, worte?.angebot)
                when {
                    auftrag.length < 20 -> Ergebnis("Der Auftrag ist zu knapp. Formuliere ihn vollständig aus.", fehler = true)
                    // Frank hat eben per Knopf gewählt und der Auftrag läuft; schiebt er gleich danach etwas nach, ruft das Modell
                    // programmieren oft noch einmal auf: dann weder die Modellfrage ein zweites Mal noch ein doppelter Auftrag.
                    knopf != null -> schonPerKnopfGestartet(knopf)
                    katalog is Ergebnis -> katalog
                    katalog is OpenLauncherKatalog.Wahl -> {
                        val pc = ziele.filterIsInstance<PcBruecke>().filter { it.bereit() }
                        if (pc.isEmpty()) Ergebnis(ziele.first().fehlt(), fehler = true)
                        else withContext(KatalogStart(katalog)) {
                            // Ohne Hooks (Codex, Kimi, Antigravity) gilt Franks Review-Angabe als REVIEW-Zeile; die anderen fragen am Ende selbst.
                            starteErstes(pc, appName, auftrag, a.optBoolean("review") && katalog.cli !in listOf(CLI_CLAUDE, CLI_OPENCODE),
                                Auftragsbuch.neueNummer(), neu, MODELL_KATALOG, katalog.cli)
                        }.also { ergebnis ->
                            // Erst nach dem Start haben offene Knöpfe ausgedient; nach einem Fehlstart bleiben sie (Opus/Sol als Ausweg).
                            if (!ergebnis.fehler || ergebnis.text.startsWith("Unklar")) herkunft().second.takeIf { it.isNotEmpty() }?.let { wahlenSchliessen(it) }
                        }
                    }
                    // Franks Wahl vom 24.10.2026: Ohne genanntes Modell kommen bei jedem Auftrag die Knöpfe, auch wenn Sol gerade nicht
                    // erreichbar scheint (früher entfielen sie dann still, etwa wenn die PC-Abfrage länger als 4 s brauchte).
                    // Scheitert das gewählte Modell, steht der Grund im Gespräch und die Knöpfe sind wieder offen.
                    modell.isEmpty() -> modellWahlAnbieten(appName, auftrag, a.optBoolean("review"), neu, ziel, worte?.aktuell.orEmpty())
                    wahl.isEmpty() -> Ergebnis(fehltFuer(modell), fehler = true)
                    // Danach bei Sol am Windows-PC: Codex CLI oder OpenCode CLI, bei Codex die Review-Frage (Codex kann nicht selbst fragen).
                    modell == MODELL_SOL && cli.isEmpty() && ziel != "cloud" && rechnerDa() ->
                        wahlAnbieten(OffeneWahl(appName, auftrag, false, neu, herkunft().second, null, ziel, MODELL_SOL, stufe = STUFE_CLI, runde = worte?.aktuell.orEmpty()))
                    modell == MODELL_SOL && cli == CLI_CODEX && !a.has("review") && ziel != "cloud" && rechnerDa() ->
                        wahlAnbieten(OffeneWahl(appName, auftrag, false, neu, herkunft().second, null, ziel, MODELL_SOL, CLI_CODEX, STUFE_REVIEW, runde = worte?.aktuell.orEmpty()))
                    else -> {
                        // Per Sprache gewählt: Die Knöpfe im selben Gespräch haben ausgedient (sonst startete ein Tipp ein zweites Mal).
                        herkunft().second.takeIf { it.isNotEmpty() }?.let { wahlenSchliessen(it) }
                        // OpenCode fragt am Ende selbst nach dem Review (wie Claude Code): dann nie eine REVIEW-Zeile im Auftrag.
                        starteErstes(wahl, appName, auftrag, a.optBoolean("review") && cli != CLI_OPENCODE, Auftragsbuch.neueNummer(), neu,
                            modell.ifEmpty { MODELL_OPUS }, cli.ifEmpty { CLI_CODEX })
                    }
                }
            },
        )
    }

    /**
     * Franks freie Modellwahl aus den Angaben des Aufrufs: null = keine (der bisherige Weg mit Opus/Sol gilt), [OpenLauncherKatalog.Wahl]
     * = geprüft und startbereit, [Ergebnis] = Rückfrage an Frank (mehrdeutig, nicht im Angebot) oder Fehler beim Lesen des Angebots.
     * Opus und Sol ohne weitere Angaben bleiben beim bisherigen Weg. In der App zählt ein Modell nur, wenn Franks eigene Worte es
     * nennen (sonst kämen wie bisher die Knöpfe).
     */
    private suspend fun katalogWahl(a: JSONObject, ausChatGpt: Boolean, aktuell: String?, rueckfrage: String?): Any? {
        val modellText = a.optString("modell").trim()
        if (modellText.isEmpty()) return null
        val cliText = a.optString("cli").trim()
        // Antwortet Frank auf Jarvis' CLI-Rückfrage nur mit dem CLI („OpenCode“), steht das Modell in dieser Rückfrage.
        val worte = if (!aktuell.isNullOrBlank() && !rueckfrage.isNullOrBlank() && OpenLauncherKatalog.cliErwaehnt(aktuell))
            aktuell + "\n" + rueckfrage else aktuell
        val zusatz = listOf("effort", "profil", "modus").any { a.optString(it).isNotBlank() } ||
            OpenLauncherKatalog.cliAus(cliText).let { it != null && it !in listOf(CLI_CODEX, CLI_OPENCODE) }
        val klein = modellText.lowercase(Locale.ROOT)
        if (klein in listOf(MODELL_OPUS, MODELL_SOL) && !zusatz) return null
        // In der App zählt ein vom Modell eingesetzter Name nur, wenn Franks eigene Worte ihn nennen; sonst wie bisher die Knöpfe
        // (auch kein Laden des Angebots und keine Rückfrage, die den Auftrag aufhielte).
        if (!ausChatGpt && !worte.isNullOrBlank() && !OpenLauncherKatalog.textGenannt(modellText, worte)) return null
        val suche = when (klein) { MODELL_OPUS -> "Claude Opus 5.5"; MODELL_SOL -> "GPT-6.1 Sol"; else -> modellText }
        val alle = try { OpenLauncherKatalog.modelle(app) } catch (fehler: Exception) {
            return Ergebnis("Noch nichts gestartet: ${fehler.message} Sag Frank das und biete an, mit Opus 5.5 oder Sol 6.1 zu starten (programmieren ohne modell).", fehler = true)
        }
        return when (val l = OpenLauncherKatalog.aufloesen(alle, suche, a.optString("effort"), a.optString("profil"), a.optString("modus"), cliText)) {
            // Auch die CLI-Frage nur zu einem Modell, das Frank selbst genannt hat (sonst wie bisher die Knöpfe).
            is OpenLauncherKatalog.Aufloesung.Frage -> if (l.modell != null && !ausChatGpt && !worte.isNullOrBlank() && !OpenLauncherKatalog.genannt(l.modell, worte)) null else Ergebnis("Noch nichts gestartet: ${l.text} Frag Frank kurz, was er meint, und rufe programmieren danach mit " +
                "demselben Auftrag und den genauen Angaben erneut auf. Rate nicht.")
            is OpenLauncherKatalog.Aufloesung.Start ->
                if (ausChatGpt || worte.isNullOrBlank() || OpenLauncherKatalog.genannt(l.wahl.modell, worte)) l.wahl else null
        }
    }

    /**
     * Die bereiten Ziele für [ziel] (pc, cloud, sonst automatisch). Sol 6.1 läuft am Windows-PC, ist der aus, in Codex Cloud;
     * nie still als Opus in der Claude-Code-Routine. Opus nie in Codex Cloud.
     */
    private fun zieleFuer(ziel: String, modell: String, cli: String = ""): List<ProgrammierZiel> = when {
        // OpenCode läuft nur am Windows-PC: nie Codex Cloud als Ersatz.
        modell == MODELL_SOL && cli == CLI_OPENCODE -> ziele.filterIsInstance<PcBruecke>()
        modell == MODELL_SOL -> when (ziel) {
            "pc" -> ziele.filterIsInstance<PcBruecke>()
            "cloud" -> listOf(codex)
            else -> ziele.filterIsInstance<PcBruecke>() + codex
        }
        ziel == "pc" -> ziele.filterIsInstance<PcBruecke>()
        ziel == "cloud" -> ziele.filterIsInstance<CloudRoutine>()
        else -> ziele
    }.filter { it.bereit() }

    /**
     * Fragt gerade der Windows-PC (Rang „haupt“) beim Relay ab? Nur dort läuft Sol; ist nur der Mac da, startet Opus wie bisher
     * ohne Rückfrage. Kurz (höchstens 4 s); im Zweifel nein.
     */
    private suspend fun rechnerDa(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            SITZUNGS_HTTP.newCall(pcAnfrage()).execute().use { a ->
                if (!a.isSuccessful) return@use false
                val alle = JSONObject(a.body?.string().orEmpty()).optJSONArray("rechner_alle") ?: return@use false
                (0 until alle.length()).mapNotNull { alle.optJSONObject(it) }.any { it.optString("rang") == "haupt" && it.optBoolean("da") }
            }
        }.getOrDefault(false)
    }

    /** Was fehlt, wenn keins der Ziele für [modell] bereit ist. */
    private fun fehltFuer(modell: String) = if (modell == MODELL_SOL) codex.fehlt() else ziele.last().fehlt()

    /**
     * Ein Start, der auf Franks Wahl per Knopf wartet. [ursprung]: im Sprachgespräch gestellt (dann wird das Ende vorgelesen).
     * [stufe]: was gerade gefragt ist (Modell, bei Sol das CLI, bei Codex das Review); [modell] und [cli] sind die Antworten davor.
     */
    private data class OffeneWahl(val app: String, val auftrag: String, val review: Boolean, val neu: Boolean, val gespraech: String,
                                  val ursprung: de.frank.jarvis.speech.SprachUrsprung?, val ziel: String = "",
                                  val modell: String = "", val cli: String = "", val stufe: String = STUFE_MODELL,
                                  val seit: Long = System.currentTimeMillis(), val runde: String = "")

    /** Ein per Knopf gestarteter Auftrag, gemerkt je Gespräch für [KNOPF_FRIST_MS] (siehe [knopfStartIn]). */
    private class KnopfStart(val nummer: String, val app: String, val modell: String, val cli: String, val runde: String = "", val zeit: Long = System.currentTimeMillis())

    /**
     * Der Auftrag, den Frank in [gespraech] vor höchstens [KNOPF_FRIST_MS] per Knopf gestartet hat, nur aus derselben Runde
     * ([runde] = Franks Nachricht): Ein neuer Auftrag in einer späteren Nachricht ist eine eigene Session und bekommt die Knöpfe.
     */
    private fun knopfStartIn(gespraech: String, runde: String): KnopfStart? =
        gespraech.takeIf { it.isNotEmpty() }?.let { knopfStarts[it] }?.takeIf { System.currentTimeMillis() - it.zeit < KNOPF_FRIST_MS }
            ?.takeIf { runde.isEmpty() || it.runde.isEmpty() || it.runde == runde }

    private fun modellName(modell: String, cli: String) =
        if (modell == MODELL_SOL) "Sol 6.1 (" + (if (cli == CLI_OPENCODE) "OpenCode CLI" else "Codex CLI") + ")" else "Opus 5.5"

    private fun schonPerKnopfGestartet(k: KnopfStart) = Ergebnis("Schon gestartet: Frank hat eben per Knopf „${modellName(k.modell, k.cli)}“ gewählt, " +
        "der Auftrag für ${k.app} (Nummer ${k.nummer}) läuft damit bereits. Frag NICHT noch einmal nach dem Modell und starte nichts neu; sag Frank " +
        "in einem kurzen Satz, dass der Auftrag mit ${modellName(k.modell, k.cli)} läuft. Bringt seine Nachricht etwas Neues für diesen Auftrag, " +
        "biete an, es mit programmier_nachtrag an Nummer ${k.nummer} zu geben. Will er ausdrücklich einen weiteren, eigenen Auftrag, rufe programmieren mit neu=true auf.")

    /**
     * Sol möglich, Modell offen: In der App kommt eine Karte mit „Opus 5.5“ und „Sol 6.1“ ins Gespräch, der Start folgt
     * auf den Tipp ([modellGewaehlt]). Aus ChatGPT gibt es keine Karte: Das Modell fragt Frank und ruft mit modell erneut auf.
     */
    private suspend fun modellWahlAnbieten(appName: String, auftrag: String, review: Boolean, neu: Boolean, ziel: String, runde: String): Ergebnis {
        val (ausChatGpt, gespraech) = herkunft()
        if (ausChatGpt || gespraech.isEmpty()) return Ergebnis("Noch nichts gestartet: Frank wählt das Modell (Sol 6.1 läuft am Windows-PC oder, ist der aus, in Codex Cloud). Frag ihn kurz: " +
            "„Opus 5.5 oder Sol 6.1?“ und rufe programmieren danach mit demselben Auftrag und modell=opus bzw. modell=sol erneut auf.")
        // Ruft das Modell in derselben Runde ([runde] = Franks Nachricht) noch einmal auf, keine zweite Karte: Die frische übernimmt
        // den neu gefassten Auftrag, ein schon per Knopf gewähltes Sol wird nicht noch einmal erfragt. Eine offene Karte aus einer
        // früheren Runde gehört zu einem eigenen Auftrag: Sie bleibt offen, unten kommt eine neue.
        offeneWahlen.entries.firstOrNull { it.value.gespraech == gespraech && System.currentTimeMillis() - it.value.seit < KNOPF_FRIST_MS &&
            (runde.isEmpty() || it.value.runde == runde) }?.let { (id, alt) ->
            val neuGefasst = alt.copy(app = appName, auftrag = auftrag)
            offeneWahlen[id] = neuGefasst
            // Gilt als Karte dieser Runde: Am Ende fragt sie (bzw. „Übernommen: …“) statt einer freien Antwort des Modells.
            karten[id]?.let { karten[id] = it.copy(frage = frageUndKnoepfe(neuGefasst).first, zeit = System.currentTimeMillis(), ausRunde = true) }
            wahlenSichern(app)
            return Ergebnis("Noch nicht gestartet: " + when (alt.stufe) {
                STUFE_CLI -> "Frank hat schon Sol 6.1 gewählt; im Chat stehen die Knöpfe „Codex CLI“ und „OpenCode CLI“."
                STUFE_REVIEW -> "Frank hat schon Sol 6.1 im Codex CLI gewählt; im Chat stehen die Knöpfe „Ja“ und „Nein“ zum Review."
                else -> "Im Chat stehen schon die Knöpfe „Opus 5.5“ und „Sol 6.1“."
            } + " Frag NICHT noch einmal nach dem Modell; der Tipp startet den Auftrag in der neuen Fassung. $KARTE_FRAGT")
        }
        karte(OffeneWahl(appName, auftrag, review, neu, gespraech, kotlinx.coroutines.currentCoroutineContext()[de.frank.jarvis.speech.SprachUrsprung], ziel, runde = runde))
        return Ergebnis("Noch nicht gestartet: Im Chat stehen die Knöpfe „Opus 5.5“ und „Sol 6.1“. $KARTE_FRAGT " +
            "Sagt Frank das Modell später, rufe programmieren mit demselben Auftrag und modell=opus bzw. modell=sol auf; sonst starte nichts.")
    }

    /**
     * Folgefrage nach Sol 6.1 am Windows-PC (Franks Wahl vom 22.10.2026): „Codex CLI oder OpenCode CLI?“, bei Codex danach
     * „Review: Ja/Nein?“ (Codex kann nicht selbst fragen, die Antwort steht dann als REVIEW-Zeile im Auftrag). In der App als
     * Karte im Chat, aus ChatGPT fragt das Modell Frank und ruft programmieren mit cli bzw. review erneut auf.
     */
    private suspend fun wahlAnbieten(w: OffeneWahl): Ergebnis {
        val (ausChatGpt, gespraech) = herkunft()
        val review = w.stufe == STUFE_REVIEW
        if (ausChatGpt || gespraech.isEmpty()) return Ergebnis(if (review)
            "Noch nichts gestartet: Sol 6.1 im Codex CLI kann keine Rückfragen stellen, deshalb fragt Jarvis vorher nach dem Review (diese Frage gehört zum " +
                "Codex-Ablauf und gilt nicht als ungefragt angefordertes Review). Frag Frank kurz: „Soll ein Review laufen, wenn die App oder das APK-Update fertig " +
                "programmiert ist? Ja oder Nein?“ und rufe programmieren danach mit demselben Auftrag, modell=sol, cli=codex und review=true bzw. review=false erneut auf."
        else "Noch nichts gestartet: Sol 6.1 läuft am Windows-PC im Codex CLI oder im OpenCode CLI. Frag Frank kurz: „Codex CLI oder OpenCode CLI?“ und rufe " +
            "programmieren danach mit demselben Auftrag, modell=sol und cli=codex bzw. cli=opencode erneut auf.")
        // Das Modell ist per Sprache oder Text schon gewählt: keine Erinnerung an eine Modellwahl-Karte dieses Gesprächs mehr.
        offeneWahlen.filterValues { it.gespraech == gespraech && it.stufe == STUFE_MODELL }.keys.forEach { erinnerungen.remove(it)?.cancel() }
        karte(w.copy(gespraech = gespraech, ursprung = kotlinx.coroutines.currentCoroutineContext()[de.frank.jarvis.speech.SprachUrsprung]))
        return Ergebnis(if (review) "Noch nicht gestartet: Im Chat stehen die Knöpfe „Ja“ und „Nein“ zum Review. $KARTE_FRAGT " +
            "Sagt Frank es später, rufe programmieren mit demselben Auftrag, modell=sol, cli=codex und review=true bzw. review=false auf; sonst starte nichts."
        else "Noch nicht gestartet: Im Chat stehen die Knöpfe „Codex CLI“ und „OpenCode CLI“. $KARTE_FRAGT " +
            "Sagt Frank es später, rufe programmieren mit demselben Auftrag, modell=sol und cli=codex bzw. cli=opencode auf; sonst starte nichts.")
    }

    /**
     * Stellt die Karte zu [w] ins Gespräch: Frage und Knöpfe je nach Stufe. Die Karte ist der einzige Weg der Frage (fester
     * Wortlaut, siehe [wahlInRunde]): Sie steht sofort antippbar im Chat, noch bevor das Modell seine Runde beendet.
     */
    // [vorgaenger]: die Karte, deren Tipp diese Folgekarte gestellt hat (dann gehört sie zu keiner Runde des Modells).
    private suspend fun karte(w: OffeneWahl, vorgaenger: String? = null) {
        val id = java.util.UUID.randomUUID().toString()
        val (frage, optionen) = frageUndKnoepfe(w)
        val jetzt = System.currentTimeMillis()
        karten.entries.removeIf { jetzt - it.value.zeit > KARTEN_HALTEN_MS }
        karten[id] = Karte(w.gespraech, frage, optionen, jetzt, ausRunde = vorgaenger == null, vorgaenger = vorgaenger)
        offeneWahlen[id] = w
        // Eine neue Modellfrage löst die Knöpfe älterer Wahlen desselben Gesprächs ab.
        val aelter = if (vorgaenger == null) karten.filter { it.key != id && it.value.gespraech == w.gespraech }.keys else emptySet()
        frischeWahlen.update { it - aelter + id }
        wahlStand.update { it + (id to WAHL_OFFEN) }
        wahlenSichern(app)
        ProgrammierChat.poste(app, w.gespraech, frage, ProgrammierChat.ART_MODELLWAHL, kartenKontext(id, w.app, optionen))
        if (w.stufe == STUFE_MODELL) modellErinnerung(id)
    }

    /**
     * Franks Wunsch vom 24.10.2026: Ist die Modellwahl nach 30 Sekunden noch offen, fragt Jarvis einmal hörbar nach (etwa im
     * Auto, ohne Blick aufs Handy). Ein Tipp, eine Wahl per Sprache oder eine nicht mehr offene Karte verhindern sie.
     */
    private fun modellErinnerung(id: String) {
        erinnerungen[id] = ansagen.launch {
            kotlinx.coroutines.delay(ERINNERUNG_NACH_MS)
            // Ein laufendes Gespräch hat Vorrang: abwarten, solange die Wahl offen bleibt.
            kotlinx.coroutines.withTimeoutOrNull(KNOPF_FRIST_MS) {
                while (de.frank.jarvis.speech.GespraechsFokus.stilleHinweise && wahlNochOffen(id)) kotlinx.coroutines.delay(1_000L)
            } ?: return@launch
            if (!wahlNochOffen(id) || id !in frischeWahlen.value) return@launch
            Logbuch.system("Modellwahl-Erinnerung", id)
            val p = de.frank.jarvis.meldung.Meldungen.profil(app, de.frank.jarvis.meldung.Meldungsarten.RUECKFRAGE)
            de.frank.jarvis.speech.Ansage.sprichUndWarte(app, "modellwahl", "Modellwahl", ERINNERUNG_TEXT, p.lautstaerke, p.stimmWahl)
        }.also { job -> job.invokeOnCompletion { erinnerungen.remove(id, job) } }
    }

    /** Fester Fragetext und Knöpfe einer Karte je Stufe. */
    private fun frageUndKnoepfe(w: OffeneWahl): Pair<String, List<Pair<String, String>>> = when (w.stufe) {
        STUFE_CLI -> "Programmierauftrag${fuer(w.app)}: Sol 6.1 mit Codex CLI oder OpenCode CLI?" to listOf(CLI_CODEX to "Codex CLI", CLI_OPENCODE to "OpenCode CLI")
        STUFE_REVIEW -> "Programmierauftrag${fuer(w.app)}: Soll Codex ein Review machen, wenn die App oder das APK-Update fertig programmiert ist?" to listOf(REVIEW_JA to "Ja", REVIEW_NEIN to "Nein")
        else -> "Programmierauftrag${fuer(w.app)}: Opus 5.5 oder Sol 6.1?" to listOf(MODELL_OPUS to "Opus 5.5", MODELL_SOL to "Sol 6.1")
    }

    /** Die offenen Modellwahl-Knöpfe eines Gesprächs gelten nicht mehr (Frank hat per Sprache gewählt). */
    private fun wahlenSchliessen(gespraech: String) {
        val weg = offeneWahlen.filterValues { it.gespraech == gespraech }.keys
        weg.forEach { offeneWahlen.remove(it); erinnerungen.remove(it)?.cancel() }
        if (weg.isNotEmpty()) wahlStand.update { stand -> stand + weg.associateWith { WAHL_ERLEDIGT } }
        if (weg.isNotEmpty()) wahlenSichern(app)
    }

    /**
     * Tipp auf „Opus 5.5“ oder „Sol 6.1“: startet den gemerkten Auftrag genau einmal. Scheitert der Start, steht der Grund im
     * Gespräch und die Knöpfe sind wieder offen (Frank kann das andere Modell nehmen, ohne den Auftrag neu zu sagen).
     * Ein unklarer Start (Zeitüberschreitung) bleibt gewählt: Er läuft womöglich.
     */
    suspend fun modellGewaehlt(wahlId: String, wert: String) {
        val w = offeneWahlen.remove(wahlId) ?: return
        erinnerungen.remove(wahlId)?.cancel()
        wahlStand.update { it + (wahlId to wert) }
        wahlenSichern(app)
        // Bis Folgekarte bzw. Start feststehen, wartet [wahlInRunde] mit seiner Ansage.
        tippInArbeit += wahlId
        try { tippVerarbeiten(wahlId, w, wert) } finally { tippInArbeit -= wahlId }
    }

    private suspend fun tippVerarbeiten(wahlId: String, w: OffeneWahl, wert: String) {
        val umgebung = ProgrammierChat.GespraechKennung(w.gespraech) + (w.ursprung ?: kotlin.coroutines.EmptyCoroutineContext)
        // Folgefragen: Sol am Windows-PC → CLI, Codex CLI → Review. Ist der PC aus, geht Sol wie bisher ohne Frage weiter.
        val naechste = when {
            w.stufe == STUFE_MODELL && wert == MODELL_SOL && w.ziel != "cloud" && rechnerDa() -> w.copy(modell = MODELL_SOL, stufe = STUFE_CLI)
            w.stufe == STUFE_CLI && wert == CLI_CODEX -> w.copy(cli = CLI_CODEX, stufe = STUFE_REVIEW)
            else -> null
        }
        if (naechste != null) {
            karte(naechste, vorgaenger = wahlId)
            return
        }
        val modell = if (w.stufe == STUFE_MODELL) wert else w.modell
        val cli = when (w.stufe) { STUFE_CLI -> wert; STUFE_REVIEW -> CLI_CODEX; else -> CLI_CODEX }
        val review = if (w.stufe == STUFE_REVIEW) wert == REVIEW_JA else w.review && cli != CLI_OPENCODE
        val nummer = Auftragsbuch.neueNummer()
        val ergebnis = withContext(umgebung) {
            val wahl = zieleFuer(w.ziel, modell, cli)
            if (wahl.isEmpty()) Ergebnis(fehltFuer(modell), fehler = true)
            else starteErstes(wahl, w.app, w.auftrag, review, nummer, w.neu, modell, cli)
        }
        // Gestartet (oder unklar, läuft womöglich): Ein erneuter Aufruf aus diesem Gespräch fragt nicht noch einmal (siehe knopfStartIn).
        if (!ergebnis.fehler || ergebnis.text.startsWith("Unklar")) knopfStarts[w.gespraech] = KnopfStart(nummer, w.app, modell, cli, w.runde)
        if (!ergebnis.fehler) return
        ProgrammierChat.poste(app, w.gespraech, "⚠️ " + ergebnis.text, ProgrammierChat.ART_STATUS)
        if (!ergebnis.text.startsWith("Unklar")) {
            offeneWahlen[wahlId] = w
            wahlStand.update { it + (wahlId to WAHL_OFFEN) }
            wahlenSichern(app)
        }
    }

    /** Versucht die Ziele der Reihe nach; [NichtDa] heißt: nichts gestartet, das nächste Ziel ist dran. */
    private suspend fun starteErstes(wahl: List<ProgrammierZiel>, appName: String, auftrag: String, review: Boolean, nummer: String, neu: Boolean,
                                     modell: String = MODELL_OPUS, cli: String = CLI_CODEX): Ergebnis {
        val uebersprungen = mutableListOf<String>()
        val (ausChatGpt, aufrufGespraech) = herkunft()
        // Im Sprachgespräch gestellt (Hey Jarvis mit Vorlesen oder Live): dauerhaft im Auftragsbuch, damit der Abschluss später
        // vorgelesen wird, auch nach Stille-Ende, App-Neustart oder aus dem Hintergrund. Aus ChatGPT nie.
        val ursprung = if (ausChatGpt) null else kotlinx.coroutines.currentCoroutineContext()[de.frank.jarvis.speech.SprachUrsprung]
        // Aus ChatGPT nie das gerade offene App-Gespräch, in der App nie ein Gespräch, das schon einen Auftrag hat, und bei
        // ausdrücklich neuer Session immer: Der Auftrag bekommt nach dem Start sein eigenes (siehe unten). Ausnahme: Begann das
        // Gespräch mit genau diesem „Programmierer“-Auftrag, ist es selbst die neue Session (ein Auftrag, ein Chat).
        val leer = !ausChatGpt && neu && aufrufGespraech.isNotEmpty() && withContext(Dispatchers.IO) {
            runCatching { Gespraeche.laden(app, aufrufGespraech)?.second?.let { NachtragsRegel.ausLeeremGespraech(it) } }.getOrNull() == true
        }
        val eigenes = NachtragsRegel.eigenesGespraech(ausChatGpt, neu, if (aufrufGespraech.isEmpty()) 0 else Auftragsbuch.alleImGespraech(app, aufrufGespraech).size, leer)
        var gespraech = if (eigenes) "" else aufrufGespraech
        // Franks Programmierregeln gehen bewusst nicht mit: Sie sind reiner Jarvis-Kontext (wie Jarvis Aufträge steuert), keine Anweisung an die Sitzung.
        for (ziel in wahl) {
            val mitFortschritt = auftragMitFortschritt(auftrag, when {
                ziel is CodexCloud -> fortschrittCodex(nummer)
                // Frei gewähltes Modell am Mac: Claude Code mit Hooks, OpenCode mit dem Plugin, Codex, Kimi und Antigravity melden selbst.
                modell == MODELL_KATALOG && cli == CLI_CLAUDE -> fortschrittsAnweisung(nummer, pc = true)
                modell == MODELL_KATALOG && cli == CLI_OPENCODE -> fortschrittsAnweisung(nummer, pc = true, frageWerkzeug = "dem Werkzeug question")
                modell == MODELL_KATALOG -> fortschrittSol(nummer)
                // OpenCode: Hooks über das Plugin jarvis-auftrag.js, Jarvis-MCP global, Rückfragen mit dem Werkzeug question.
                modell == MODELL_SOL && cli == CLI_OPENCODE -> fortschrittsAnweisung(nummer, pc = true, frageWerkzeug = "dem Werkzeug question")
                modell == MODELL_SOL -> fortschrittSol(nummer)
                else -> fortschrittsAnweisung(nummer, pc = ziel is PcBruecke)
            })
            // Start womöglich gelungen, aber unbestätigt: als „unklar“ eintragen (nie „nicht gestartet“, sonst startet ihn Frank ein
            // zweites Mal). Meldet sich die Sitzung oder reicht der Relay die Quittung nach, wird daraus ein normaler Auftrag.
            suspend fun unklarEintragen(schritt: String) {
                if (eigenes) gespraech = withContext(Dispatchers.IO) { runCatching { ProgrammierChat.neuesGespraech(app, appName, auftrag, ueberPlugin = ausChatGpt, ausGespraech = aufrufGespraech) }.getOrDefault("") }
                val eintrag = Auftragsbuch.neu(app, nummer, appName, ziel.name, "", schritt, gespraech, auftrag, stand = STAND_UNKLAR, ursprung = ursprung)
                if (eigenes) Auftragsbuch.herkunftMerken(app, nummer, aufrufGespraech)
                verteile(eintrag, null, von = ProgrammierChat.VON_JARVIS)
            }
            val start = try {
                withContext(CliWahl(cli)) { ziel.starte(appName, mitFortschritt, review, nummer, modell) }
            } catch (nichtDa: NichtDa) {
                uebersprungen += nichtDa.message.orEmpty()
                continue
            } catch (offen: Unbestaetigt) {
                // Abgeholt, Bestätigung steht aus: kein Fehler (die Modell-Knöpfe bleiben zu, kein zweiter Start, keine Cloud).
                unklarEintragen("Der Auftrag ist bei ${offen.rechner} angekommen, die Startbestätigung steht noch aus. Er läuft vermutlich; " +
                    "sobald der PC bestätigt oder sich die Sitzung meldet, steht es hier.")
                return Ergebnis("Der Auftrag für $appName (Nummer $nummer) ist bei ${offen.rechner} angekommen; nur die Startbestätigung kam nicht rechtzeitig, " +
                    "er läuft vermutlich. Starte ihn NICHT neu und NICHT in der Cloud: Die Bestätigung oder die ersten Schritte der Sitzung erscheinen von selbst " +
                    "im Gespräch, programmierauftraege nennt den Stand. Sag Frank das so, nicht „nicht gestartet“.")
            } catch (zeit: java.net.SocketTimeoutException) {
                if (vorDemSenden(zeit)) {
                    // Die Verbindung kam gar nicht zustande, also kam auch nichts an: wie „nicht erreichbar“, das nächste Ziel (die Cloud) übernimmt.
                    uebersprungen += "${ziel.name} war nicht erreichbar (Zeitlimit beim Verbindungsaufbau)" + if (ziel is PcBruecke) ", deshalb die Cloud." else "."
                    continue
                }
                // Keine Antwort mehr nach dem Senden: Der Auftrag kam womöglich an und läuft.
                unklarEintragen("Unklar: ${ziel.name} hat den Start nicht rechtzeitig bestätigt (Zeitüberschreitung). Womöglich läuft der Auftrag trotzdem.")
                return Ergebnis("Unklar, ob der Auftrag für $appName (Nummer $nummer) gestartet ist: ${ziel.name} hat nach dem Senden nicht rechtzeitig geantwortet " +
                    "(Zeitüberschreitung), er läuft womöglich trotzdem. Starte ihn NICHT neu: Prüfe erst mit programmierauftraege, ob sich die Sitzung meldet, und sag Frank das so.", fehler = true)
            } catch (fehler: Exception) {
                return Ergebnis("Der Auftrag ist nicht gestartet (${ziel.name}): ${fehler.message ?: fehler.javaClass.simpleName}", fehler = true)
            }
            // Erst jetzt, nach dem gelungenen Start: Bei „nicht erreichbar“ bliebe sonst eine leere Session zurück.
            if (eigenes) gespraech = withContext(Dispatchers.IO) { runCatching { ProgrammierChat.neuesGespraech(app, appName, auftrag, ueberPlugin = ausChatGpt, ausGespraech = aufrufGespraech) }.getOrDefault("") }
            val eintrag = Auftragsbuch.neu(app, nummer, appName, ziel.name, start.link, "Gestartet: ${ziel.name}. ${start.hinweis}", gespraech, auftrag, ursprung = ursprung)
            // Bekommt der Auftrag ein eigenes Gespräch, merkt er sich das, aus dem er kam: Dort meint „in dieser neuen Session“ ihn.
            if (eigenes) Auftragsbuch.herkunftMerken(app, nummer, aufrufGespraech)
            verteile(eintrag, null, von = ProgrammierChat.VON_JARVIS)
            val titel = "Programmieren: $appName" + if (ausChatGpt) " (über ChatGPT/MCP)" else ""
            return Ergebnis("Der Auftrag für $appName (Nummer $nummer) läuft jetzt als neuer, eigener Auftrag: ${ziel.name}. ${start.hinweis} " +
                (if (!eigenes) "" else if (gespraech.isNotEmpty()) "In Jarvis steht er als eigene, getrennte Programmier-Session „$titel“. "
                    else "Eine eigene Session ließ sich in Jarvis nicht anlegen; die Schritte stehen dann nur in programmierauftraege und vorgaenge. ") +
                "Die Sitzung meldet ihre Schritte ${if (eigenes && gespraech.isEmpty()) "an Jarvis" else if (eigenes) "in diese eigene Session" else "in dieses Gespräch"}; eine Benachrichtigung kommt erst am Ende oder bei einer Rückfrage, und programmierauftraege nennt jederzeit den Stand." +
                (if (start.link.isNotEmpty()) " Link zur Sitzung: ${start.link}" else "") +
                (if (uebersprungen.isEmpty()) "" else " (${uebersprungen.joinToString(" ")})"))
        }
        return Ergebnis("Der Auftrag ist nicht gestartet: ${uebersprungen.joinToString(" ")}", fehler = true)
    }

    /**
     * Was die Sitzung zum Melden wissen muss. Am PC übernehmen Hooks die Zwischenschritte und Rückfragen; die Sitzung meldet
     * dort nur Anfang und Ende. In der Cloud meldet sie alles selbst.
     */
    private fun fortschrittsAnweisung(nummer: String, pc: Boolean, frageWerkzeug: String = "AskUserQuestion") = if (pc)
        "FORTSCHRITT (AUFTRAG-ID: $nummer): Zwischenschritte und Rückfragen meldet die Sitzung automatisch an Jarvis; frag normal mit $frageWerkzeug, " +
            "Frank antwortet in Jarvis. Selbst melden mit programmier_fortschritt (MCP-Server Jarvis, auftrag_id=$nummer): stand=laeuft, sobald die App gefunden ist, " +
            "am Ende stand=fertig oder stand=fehler mit dem Grund. $ABSCHLUSS_MELDUNG " +
            "NACHTRÄGE kommen hier als Rückmeldung „Frank hat über Jarvis einen Nachtrag …“ an, auch nach dem Abschluss: umsetzen, wieder vollständig abschließen, erneut stand=fertig."
    else
        "FORTSCHRITT (AUFTRAG-ID: $nummer): Melde jeden wichtigen Schritt mit programmier_fortschritt (MCP-Server Jarvis, auftrag_id=$nummer, ein kurzer Satz): " +
            "zu Beginn und wenn die App gefunden ist (stand=laeuft), bei Programmierung, Commit oder Pull Request und Bau, am Ende stand=fertig oder stand=fehler mit dem Grund. " +
            "$ABSCHLUSS_MELDUNG Vor jeder Frage mit AskUserQuestion stand=frage mit der Frage melden. Ohne das Werkzeug ohne Meldungen weiterarbeiten. " +
            "NACHTRÄGE: Antwortet programmier_fortschritt mit „NACHTRAG VON FRANK“, gehört er zu diesem Auftrag: umsetzen und erneut melden. " +
            "Unmittelbar vor stand=fertig noch einmal stand=laeuft melden, damit ein später Nachtrag ankommt; kommt einer, ihn abarbeiten statt fertig zu melden."

    /**
     * Sol 6.1 im Codex CLI: keine Hooks, kein Jarvis-MCP, keine Rückfragen und Nachträge. Die Sitzung meldet selbst mit dem
     * MELDEN-Befehl, den die PC-Brücke über den Auftrag schreibt (jarvis_hook.py melde).
     */
    private fun fortschrittSol(nummer: String) =
        "FORTSCHRITT (AUFTRAG-ID: $nummer): Melde mit dem MELDEN-Befehl oben: laeuft, sobald die App gefunden ist, und bei jedem wichtigen Schritt " +
            "(Programmierung, Bau, Commit, Installation), am Ende fertig oder fehler mit dem Grund. $ABSCHLUSS_MELDUNG " +
            "Rückfragen an Frank sind in dieser Sitzung nicht möglich: Kleinigkeiten selbst entscheiden; fehlt eine echte Entscheidung von ihm, " +
            "melde sie mit laeuft als Frage und arbeite mit der sinnvollsten Annahme weiter."

    /**
     * Sol 6.1 in Codex Cloud: keine Hooks, kein Jarvis-MCP, keine Rückfragen. Gemeldet wird über `.claude/hooks/jarvis.sh`
     * (braucht JARVIS_MCP_URL in der Umgebung); Nachträge reisen wie in der Claude-Code-Cloud mit der Antwort zurück.
     */
    private fun fortschrittCodex(nummer: String): String {
        val melden = "bash .claude/hooks/jarvis.sh programmier_fortschritt '{\"auftrag_id\":\"$nummer\",\"stand\":\"laeuft\",\"schritt\":\"…\"}'"
        return "FORTSCHRITT (AUFTRAG-ID: $nummer): Arbeite nach den Cloud-Regeln (OpenLauncher/Profiles/ClaudeCode/sources/cloud.md), bei einer Android-App " +
            "bis zum Ende des Skills apk-update-cloud (PR, selbst mergen, GitHub baut, UpdateStation). Kein Codex-Review, nicht danach fragen. " +
            "Melde jeden wichtigen Schritt an Jarvis mit: $melden (stand laeuft, fertig oder fehler; schritt ein kurzer Satz ohne Anführungszeichen): " +
            "sobald die App gefunden ist, bei Programmierung, Pull Request und Bau, am Ende fertig oder fehler mit dem Grund. $ABSCHLUSS_MELDUNG " +
            "Rückfragen an Frank sind nicht möglich: Kleinigkeiten selbst entscheiden; fehlt eine echte Entscheidung, melde sie mit laeuft als Frage und " +
            "arbeite mit der sinnvollsten Annahme weiter. NACHTRÄGE: Antwortet die Meldung mit „NACHTRAG VON FRANK“, gehört er zu diesem Auftrag: umsetzen " +
            "und erneut melden. Unmittelbar vor fertig noch einmal laeuft melden, damit ein später Nachtrag ankommt."
    }

    /**
     * Eine Meldung des Hooks vom PC (Signal „programmieren“ über den Relay): ins Auftragsbuch, als Benachrichtigung und in
     * das Gespräch, aus dem der Auftrag kam. Mit „frage“ ist es eine Rückfrage, auf die der Hook wartet.
     */
    fun meldungVomServer(daten: JSONObject) {
        val auftragId = daten.optString("auftrag_id")
        daten.optJSONObject("lokal")?.let { if (!lokaleSitzung(auftragId, it)) return }
        if (daten.optString("stand") == "puls") {
            // Lebenszeichen: nur die drehende Anzeige auffrischen, keine Nachricht, keine Benachrichtigung. Ein gesperrter Auftrag
            // (Anmeldung, Störung) arbeitet nachweislich wieder, wenn der Puls eine Minute nach der Sperre kommt (ein früherer
            // stammt womöglich noch von vor dem Fehler); normalerweise meldet der Hook das selbst mit „anmeldung_ok“.
            val gesperrt = Auftragsbuch.finde(app, auftragId)?.takeIf { it.optString("stand") in GESPERRT }
            if (gesperrt != null && nachSperre(gesperrt, daten)) {
                Auftragsbuch.melde(app, auftragId, "laeuft", "▶️ Die Sitzung arbeitet wieder.", "")?.let { verteile(it, null, von = ProgrammierChat.VON_JARVIS); benachrichtige(it) }
                return
            }
            Auftragsbuch.puls(app, auftragId, daten.optLong("hook_beginn"))?.let { aktivitaetMelden(it) }
            return
        }
        if (daten.optString("stand") in listOf("anmeldung_ok", "weiter")) {
            // Die Sperre ist aufgehoben (wieder angemeldet bzw. weiter nach einer Störung): läuft, und die Benachrichtigung
            // „Anmeldung erforderlich“ wird still durch den neuen Stand ersetzt.
            // Ohne Sperre in Jarvis (deren Meldung kam nie an) gibt es nichts aufzuheben.
            if (Auftragsbuch.finde(app, auftragId)?.optString("stand") !in GESPERRT) return
            Auftragsbuch.melde(app, auftragId, "laeuft", ProgrammierAbschluss.kuerzen(daten.optString("schritt"), ProgrammierAbschluss.MAX_MELDUNG), "")?.let { verteile(it, null); benachrichtige(it) }
            return
        }
        if (daten.optString("stand") == "nachtrag") {
            // Der Hook hat Nachträge in die Sitzung gegeben: zugestellt, die Sitzung arbeitet wieder. Eine Nachquittung für
            // schon Zugestelltes ändert nichts und erscheint nicht noch einmal im Gespräch.
            val ids = daten.optJSONArray("nachtrag_ids")?.let { l -> (0 until l.length()).map { l.optString(it) }.toSet() } ?: return
            alsZugestellt(daten.optString("auftrag_id"), ids, daten.optString("schritt"))
            return
        }
        if (daten.optString("stand") == "frage_status") {
            daten.optJSONObject("frage")?.let { frageAusgang(daten.optString("auftrag_id"), it) }
            return
        }
        if (daten.optBoolean("spaet")) {
            spaeteQuittung(auftragId, daten)
            return
        }
        val frage = daten.optJSONObject("frage")
        // Dieselbe Rückfrage zweimal (der Hook wiederholt, wenn die Antwort des Servers ausblieb): nur einmal zeigen und melden.
        if (frage != null && Auftragsbuch.kenntFrage(app, frage.optString("frage_id"))) return
        // Ein Hook, der vor dem Fehler begann (async, spät dran), hebt eine Sperre nicht auf: Text zeigen, Stand bleibt gesperrt.
        val bisher = Auftragsbuch.finde(app, auftragId)
        val stand = daten.optString("stand").let { neu ->
            if (bisher != null && bisher.optString("stand") in GESPERRT && neu !in GESPERRT && frage == null && !nachSperre(bisher, daten)) bisher.optString("stand") else neu
        }
        // Der Abschlussbericht am Zugende (info) kommt ganz an: Früher schnitt take(4000) ihn wie der Hook mitten im Wort ab.
        val grenze = if (daten.optString("stand") == "info") ProgrammierAbschluss.MAX_BERICHT else ProgrammierAbschluss.MAX_MELDUNG
        val eintrag = Auftragsbuch.melde(app, auftragId, stand, ProgrammierAbschluss.kuerzen(daten.optString("schritt"), grenze), "", frage,
            sperrSeit = if (daten.optString("stand") in GESPERRT) daten.optLong("seit", System.currentTimeMillis() / 1000) else 0L,
            leerlauf = daten.optBoolean("leerlauf"), hookBeginn = daten.optLong("hook_beginn"))
        if (eintrag == null) {
            // Unbekannter Auftrag: nie still verwerfen, sondern sichtbar im Logbuch (Grund für „Frage kam nicht an“).
            Logbuch.system("Meldung ohne Auftrag", "${daten.optString("stand")} für ${daten.optString("auftrag_id")}: ${daten.optString("schritt").take(120)}")
            return
        }
        // Wiederholte Sperre (gleicher Stand) oder alte Meldung während der Sperre: keine zweite Benachrichtigung.
        verteile(eintrag, frage, benachrichtigen = bisher?.optString("stand") != eintrag.optString("stand") || stand !in GESPERRT)
    }

    /**
     * Vom Relay nachgereichte Startbestätigung (oder ihr Ausbleiben) für einen Auftrag mit „Start unbestätigt“. Gilt nur, solange
     * der Eintrag noch unbestätigt ist: Hat sich die Sitzung inzwischen selbst gemeldet (läuft, Rückfrage, fertig …), gilt ihr Stand.
     */
    private fun spaeteQuittung(auftragId: String, daten: JSONObject) {
        if (Auftragsbuch.finde(app, auftragId)?.optString("stand") != STAND_UNKLAR) return
        val stand = daten.optString("stand")
        // Doch nicht gestartet: Ein neuer Start aus dem Gespräch darf nicht mehr als „läuft schon“ abgewiesen werden.
        if (stand == "fehler") knopfStarts.entries.removeIf { it.value.nummer.equals(auftragId, ignoreCase = true) }
        val eintrag = Auftragsbuch.melde(app, auftragId, stand, ProgrammierAbschluss.kuerzen(daten.optString("schritt"), ProgrammierAbschluss.MAX_MELDUNG), "") ?: return
        verteile(eintrag, null, von = ProgrammierChat.VON_JARVIS)
    }

    /**
     * Eine am PC direkt gestartete Claude-Code-Sitzung (Feld „lokal“: Name, Ordner, Rechner, Claude-App-Link, hook, auftrag).
     * Beim ersten Lebenszeichen bekommt sie einen Eintrag im Auftragsbuch und ein eigenes Gespräch unter „Programmierung“;
     * die Nummer kommt fest aus ihrer session_id, dieselbe Sitzung landet also immer im selben Eintrag. false = Frank hat sie in
     * Jarvis entfernt: Dann bleibt sie weg, und der Relay erfährt es noch einmal (falls ihn die Entfernung nicht erreicht hatte).
     */
    private fun lokaleSitzung(id: String, info: JSONObject): Boolean {
        if (id.isBlank()) return false
        synchronized(lokalSperre) {
            if (id.uppercase() in entfernteLokale(app)) {
                lokalEntfernenSenden(app, id)
                return false
            }
            if (Auftragsbuch.finde(app, id) == null) {
                val name = info.optString("name").ifBlank { "Sitzung" }
                val rechner = info.optString("rechner")
                val auftrag = info.optString("auftrag")
                val gespraech = ProgrammierChat.lokalesGespraech(app, name, rechner)
                Auftragsbuch.neu(app, id, name, ZIEL_PC, info.optString("link"), "💻 Gestartet auf dem PC", gespraech, auftrag)
                Logbuch.system("Sitzung vom PC erkannt", "$id: $name auf $rechner" + if (info.optBoolean("hook")) "" else " (nur mitlesen)")
            }
            Auftragsbuch.lokalMerken(app, id, info)
        }
        return true
    }

    /**
     * Begann der Hook dieser Meldung nach dem Fehler, der den Auftrag gesperrt hat? Ältere Hooks schicken kein hook_beginn:
     * dann gilt eine Minute Abstand zur Sperre (ein früherer Puls stammt womöglich noch von vor dem Fehler).
     */
    private fun nachSperre(eintrag: JSONObject, daten: JSONObject): Boolean {
        val seit = eintrag.optLong("gesperrtSeit")
        val beginn = daten.optLong("hook_beginn")
        return if (seit > 0 && beginn > 0) beginn > seit else System.currentTimeMillis() - eintrag.optLong("zeit") >= 60_000L
    }

    /** Stand einer Rückfrage aus dem Wort des Servers bzw. des Hooks. */
    private fun standVomServer(status: String): String? = when (status) {
        "angenommen" -> RueckfrageStand.ANGENOMMEN
        "beantwortet" -> RueckfrageStand.BEANTWORTET
        "beantwortet_am_pc" -> RueckfrageStand.AM_PC_BEANTWORTET
        "am_pc", "freigegeben" -> RueckfrageStand.AM_PC
        "erledigt" -> RueckfrageStand.ERLEDIGT
        "verwaist", "unbekannt" -> RueckfrageStand.VERWAIST
        else -> null
    }

    /** Ausgang einer Rückfrage vom Hook (stand „frage_status“): überall als beantwortet bzw. am Rechner zeigen. */
    private fun frageAusgang(auftragId: String, frage: JSONObject) {
        val frageId = frage.optString("frage_id")
        val stand = standVomServer(frage.optString("status")) ?: return
        val eintrag = Auftragsbuch.zuFrage(app, frageId) ?: Auftragsbuch.finde(app, auftragId) ?: return
        val gespraech = eintrag.optString("gespraech")
        Auftragsbuch.frageErledigt(app, frageId)
        rueckfrageAnsageAbbrechen(frageId)
        if (stand in RueckfrageStand.BEANTWORTET_ALLE) rueckfrageMeldungWeg(eintrag.optString("id"), frageId)
        aktivitaetMelden(Auftragsbuch.finde(app, eintrag.optString("id")) ?: eintrag)
        if (gespraech.isNotEmpty()) ProgrammierChat.rueckfrageStand(app, gespraech, frageId, stand, frage.optString("antwort"), frage.optString("grund"))
    }

    /**
     * Gleicht die Rückfragen mit dem Jarvis-Server ab: Eine Rückfrage, deren Meldung über die WebSocket verloren ging (halb
     * tote Verbindung, Ruhezustand), erscheint nachträglich im richtigen Gespräch; ein Ausgang, den Jarvis verpasst hat
     * (am Rechner beantwortet, Sitzung geschlossen), wird nachgetragen. Beim Verbinden mit dem Server und solange die App offen ist.
     */
    suspend fun fragenAbgleichen() = withContext(Dispatchers.IO) {
        if (e.serverHost.isBlank() || e.serverToken.isBlank()) return@withContext
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/fragen").header("X-Jarvis-Token", e.serverToken).get().build()
        val liste = runCatching {
            HTTP.newCall(anfrage).execute().use { a -> if (!a.isSuccessful) null else JSONObject(a.body?.string().orEmpty()).optJSONArray("fragen") }
        }.getOrNull() ?: return@withContext
        val jetzt = System.currentTimeMillis() / 1000
        for (i in 0 until liste.length()) {
            val f = liste.optJSONObject(i) ?: continue
            val frageId = f.optString("frage_id").takeIf { it.isNotEmpty() } ?: continue
            val eintrag = Auftragsbuch.finde(app, f.optString("auftrag_id")) ?: continue
            val gespraech = eintrag.optString("gespraech")
            val status = f.optString("status")
            val wartet = f.optBoolean("wartet")
            if (status == "offen") {
                val daSchon = gespraech.isNotEmpty() && ProgrammierChat.rueckfrageAendern(app, gespraech, frageId) { false }
                when {
                    !daSchon && wartet && !Auftragsbuch.kenntFrage(app, frageId) -> {
                        Logbuch.system("Rückfrage nachgereicht", "Auftrag ${eintrag.optString("id")}: Meldung kam nicht über die Verbindung, Abgleich mit dem Server")
                        val frage = f.optJSONObject("frage") ?: continue
                        Auftragsbuch.melde(app, eintrag.optString("id"), "frage", ProgrammierAbschluss.kuerzen(f.optString("schritt"), ProgrammierAbschluss.MAX_MELDUNG), "", frage)?.let { verteile(it, frage) }
                    }
                    daSchon && !wartet && jetzt - f.optLong("zeit") > 120 -> {
                        Auftragsbuch.frageErledigt(app, frageId)
                        ProgrammierChat.rueckfrageStand(app, gespraech, frageId, RueckfrageStand.VERWAIST)
                    }
                }
                continue
            }
            val stand = standVomServer(status) ?: continue
            if (gespraech.isNotEmpty()) ProgrammierChat.rueckfrageStand(app, gespraech, frageId, stand, f.optString("antwort_text"), f.optString("grund"))
            if (stand in RueckfrageStand.BEANTWORTET_ALLE) rueckfrageMeldungWeg(eintrag.optString("id"), frageId)
            if (RueckfrageStand.endgueltig(stand)) {
                Auftragsbuch.frageErledigt(app, frageId)
                rueckfrageAnsageAbbrechen(frageId)
            }
        }
    }

    /** Woher der Aufruf kommt: aus ChatGPT/MCP (ohne Jarvis-Gespräch) und das Jarvis-Gespräch dazu (leer = keins). */
    private suspend fun herkunft(): Pair<Boolean, String> {
        val ctx = kotlinx.coroutines.currentCoroutineContext()
        val ausChatGpt = ctx[ProgrammierChat.GespraechKennung] == null && ctx[ProgrammierChat.AusChatGpt] != null
        return ausChatGpt to (ctx[ProgrammierChat.GespraechKennung]?.id ?: if (ausChatGpt) "" else ProgrammierChat.aktivesGespraech.orEmpty())
    }

    /** Nachträge sind in der Sitzung angekommen: Stand zugestellt, die Sitzung arbeitet wieder, ein Satz ins Gespräch (je Nachtrag nur einmal). */
    private fun alsZugestellt(auftragId: String, ids: Set<String>, schritt: String) {
        val neu = Auftragsbuch.setzeNachtraege(app, auftragId, ids, setOf("angenommen", "fehler"), "zugestellt")
        if (neu.isEmpty()) return
        val text = schritt.ifBlank {
            if (neu.all { istNachricht(it.optString("id")) }) "✓ Beim Programmierer angekommen."
            else "📨 Nachtrag in der Sitzung angekommen: " + neu.joinToString(" · ") { anzeige(it).replace('\n', ' ').take(300) }
        }
        // Den Satz des Hooks meldet der Programmierer; ohne ihn schreibt Jarvis selbst, dass der Nachtrag angekommen ist.
        Auftragsbuch.melde(app, auftragId, "laeuft", ProgrammierAbschluss.kuerzen(text, ProgrammierAbschluss.MAX_MELDUNG), "")
            ?.let { verteile(it, null, von = if (schritt.isBlank()) ProgrammierChat.VON_JARVIS else ProgrammierChat.VON_PROGRAMMIERER) }
    }

    /**
     * Knopf „Nachtrag“ der Eingabeblase im Programmiergespräch [gespraech]: [text] geht ohne Modell als Nachtrag in die Sitzung
     * dieses Gesprächs. Der Knopf ist Franks ausdrücklicher Wunsch; Ziel ist der noch laufende Auftrag, sonst der jüngste.
     */
    suspend fun nachtragPerKnopf(gespraech: String, text: String): Ergebnis {
        val auftraege = Auftragsbuch.alleImGespraech(app, gespraech)
        val id = (auftraege.lastOrNull { it.optString("stand") in listOf("laeuft", "frage") } ?: auftraege.lastOrNull())?.optString("id")
            ?: return Ergebnis("In diesem Gespräch läuft keine Programmier-Sitzung, der Nachtrag wurde nicht gesendet.", fehler = true)
        val worte = "Nachtrag in die gleiche Session $id"
        return withContext(ProgrammierChat.GespraechKennung(gespraech) + ProgrammierChat.FranksWorte(worte, streng = true)) {
            nachtrag(id, DiktatTrenner.auftrag(text), worte)
        }
    }

    /**
     * Ein Nachtrag an einen bestehenden Auftrag: nur, wenn Frank ihn ausdrücklich verlangt, und nur in die Sitzung, die er
     * bezeichnet ([NachtragsRegel.ziel]); sonst wird nichts gesendet und das Modell fragt Frank. Nie ein neuer Auftrag, nie eine
     * andere Sitzung. Am PC über den Relay an den Hook genau dieser Sitzung, in der Cloud mit der nächsten Fortschrittsmeldung.
     */
    private suspend fun nachtrag(auftragId: String, text: String, worte: String): Ergebnis {
        val (ausChatGpt, gespraech) = herkunft()
        if (text.length < 3) return Ergebnis("Der Nachtrag ist leer. Formuliere ihn vollständig aus.", fehler = true)
        // Im Standard-Chat zählen Franks eigene Worte (getippt oder eingesprochen), nicht die Deutung des Modells.
        val franks = kotlinx.coroutines.currentCoroutineContext()[ProgrammierChat.FranksWorte]
        // Seine aktuelle Nachricht entscheidet; ein bloßes „Ja“ zählt mit der Rückfrage davor (NachtragsRegel.pruefWorte). Früher
        // entschieden die letzten zwei Nachrichten zusammen: Ein darin zitiertes „neuer … Auftrag“ blockierte so jedes „Nachtrag“.
        val pruefWorte = when {
            franks == null -> worte
            franks.streng -> NachtragsRegel.pruefWorte(franks.aktuell, franks.vorher, franks.angebot)
            else -> NachtragsRegel.pruefWorte(franks.aktuell, null, franks.angebot) + "\n" + worte
        }
        val imGespraech = if (gespraech.isEmpty()) emptyList() else Auftragsbuch.alleImGespraech(app, gespraech)
        val gestartet = if (gespraech.isEmpty()) emptyList() else Auftragsbuch.gestartetAus(app, gespraech)
        val ziel = NachtragsRegel.ziel(auftragId, pruefWorte, Auftragsbuch.alleIds(app), imGespraech.map { it.optString("id") },
            imGespraech.filter { it.optString("stand") in listOf("laeuft", "frage") }.map { it.optString("id") }, gestartet.map { it.optString("id") })
        if (ziel is NachtragsRegel.Ziel.Nachfrage) {
            Logbuch.system("programmier_nachtrag nicht gesendet", "${ziel.grund}, auftrag_id „$auftragId“")
            val auswahl = (imGespraech + gestartet).reversed().joinToString("\n") { "- " + Auftragsbuch.kurzZeile(it) }
                .ifEmpty { Auftragsbuch.kandidatenText(app, "") }
            return Ergebnis("Nichts wurde gesendet. " + when (ziel.grund) {
                NachtragsRegel.KEIN_WUNSCH -> "Frank hat keinen Nachtrag in eine bestehende Sitzung ausdrücklich verlangt. Ordne seinen Wunsch nicht selbst zu: Frag ihn kurz, " +
                    "ob das ein neuer, eigener Auftrag werden soll (dann programmieren) oder ein Nachtrag in eine bestehende Sitzung, und in welche."
                NachtragsRegel.WIDERSPRUCH -> "Die angegebene auftrag_id passt nicht zu der Sitzung, die Frank bezeichnet hat. Wähle nicht selbst: Frag Frank, welche Sitzung er meint."
                NachtragsRegel.MEHRDEUTIG -> "Die gemeinte Sitzung ist nicht eindeutig. Wähle nicht selbst: Frag Frank kurz, welche er meint."
                else -> "Es ist keine Sitzung bezeichnet. Frag Frank kurz, welche er meint; wähle nicht selbst."
            } + "\n" + auswahl, fehler = true)
        }
        val zielId = (ziel as NachtragsRegel.Ziel.Auftrag).id
        val eintrag = Auftragsbuch.finde(app, zielId) ?: return Ergebnis("Einen Programmierauftrag $zielId kennt Jarvis nicht. Nichts wurde gesendet. " +
            "Frag Frank, welche Sitzung er meint. " + Auftragsbuch.kandidatenText(app, ""), fehler = true)
        val id = eintrag.optString("id")
        val appName = eintrag.optString("app")
        // Nur solange sie wirklich wartet: Eine im Terminal beantwortete oder verfallene Rückfrage bleibt als offeneFrage stehen
        // und sperrte sonst jeden Nachtrag an diesen Auftrag für immer.
        eintrag.optJSONObject("offeneFrage")?.takeIf { eintrag.optString("stand") == "frage" }?.let { frage ->
            return Ergebnis("Die Sitzung von Auftrag $id ($appName) wartet gerade auf eine Antwort zu ihrer Rückfrage (frage_id ${frage.optString("frage_id")}): " +
                Auftragsbuch.frageText(frage) + " Ist das die Antwort darauf, nimm programmier_antwort. Ist es ein zusätzlicher Wunsch, beantworte zuerst die Rückfrage " +
                "und schick den Nachtrag danach. Nichts wurde gesendet.", fehler = true)
        }
        val cloud = inDerCloud(eintrag.optString("ziel"))
        if (!NachtragsRegel.zustellbar(cloud, eintrag.optString("stand"))) {
            return Ergebnis("Nicht zustellbar: Die Cloud-Sitzung von Auftrag $id ($appName) ist beendet (${Auftragsbuch.standText(eintrag.optString("stand"))}) und lässt sich " +
                "nicht fortsetzen. Der Nachtrag wurde nicht gesendet. Sag Frank das offen und starte keinen Ersatzauftrag; einen neuen Auftrag nur, wenn Frank ihn danach " +
                "ausdrücklich verlangt.", fehler = true)
        }
        // Am PC gestartet, aber ohne Jarvis-Hooks (lief schon vor dem Update): Jarvis liest nur mit, nichts kommt hinein.
        if (eintrag.optBoolean("lokal") && !eintrag.optBoolean("lokalHook")) {
            return Ergebnis("Nicht zustellbar: Die Sitzung $id ($appName) wurde am PC gestartet, bevor Jarvis sie begleiten konnte; Jarvis liest dort nur mit. " +
                "Der Nachtrag wurde nicht gesendet. Sag Frank das offen: Setzt er die Sitzung am PC mit claude --resume fort, gehen Nachträge und Rückfragen " +
                "unter derselben Nummer direkt hinein.", fehler = true)
        }
        val (n, doppelt) = Auftragsbuch.nachtragNeu(app, id, text, if (ausChatGpt) "ChatGPT/MCP" else "Jarvis")
            ?: return Ergebnis("Den Auftrag $id gibt es nicht mehr im Auftragsbuch. Nichts wurde gesendet.", fehler = true)
        val nummer = n.optString("id")
        if (doppelt && n.optString("stand") !in listOf("angenommen", "fehler")) {
            return Ergebnis("Diesen Nachtrag hat Auftrag $id ($appName) schon (Nachtrag $nummer, ${n.optString("stand")}). Nichts wurde doppelt gesendet.")
        }
        val zielGespraech = eintrag.optString("gespraech")
        if (!doppelt && zielGespraech.isNotEmpty() && zielGespraech != gespraech) {
            ProgrammierChat.poste(app, zielGespraech, "📝 ${ProgrammierChat.VON_JARVIS} · Nachtrag ${if (ausChatGpt) "über ChatGPT/MCP" else "aus einem anderen Gespräch"}: $text", ProgrammierChat.ART_STATUS)
        }
        if (cloud) {
            return Ergebnis("Angenommen: Nachtrag $nummer für Auftrag $id ($appName) liegt bereit. Die Cloud-Sitzung bekommt ihn mit ihrer nächsten Fortschrittsmeldung; " +
                "zugestellt ist er erst, wenn programmierauftraege das zeigt. Meldet sie sich nicht mehr, kommt er nicht an; eine neue Sitzung startet Jarvis nicht von selbst.")
        }
        val antwort = runCatching { anSitzung(id, nummer, text) }.getOrElse { fehler ->
            Auftragsbuch.setzeNachtraege(app, id, setOf(nummer), setOf("angenommen"), "fehler")
            return Ergebnis("Nachtrag $nummer ist nicht beim Jarvis-Server angekommen (${fehler.message ?: fehler.javaClass.simpleName}). Er ist im Auftrag $id vermerkt, " +
                "aber nicht zugestellt; ein erneuter Aufruf mit demselben Text schickt ihn noch einmal, ohne ihn zu verdoppeln.", fehler = true)
        }
        if (antwort.optBoolean("zugestellt")) {
            alsZugestellt(id, setOf(nummer), "")
            return Ergebnis("Zugestellt: Nachtrag $nummer ist in derselben Sitzung von Auftrag $id ($appName) angekommen, sie arbeitet ihn jetzt ab. " +
                "Umgesetzt ist er, wenn programmierauftraege ihn als bearbeitet zeigt.")
        }
        val vor = kontaktVor(antwort)
        val arbeitet = arbeitet(antwort)
        return when {
            // Der Relay hat schon bis zu 8 s auf die Übernahme gewartet: Kam sie nicht, ist die Sitzung womöglich gerade geschlossen worden.
            antwort.optBoolean("wartet") -> Ergebnis("Angenommen, aber noch NICHT zugestellt: Nachtrag $nummer für Auftrag $id ($appName). Die Sitzung hat eben noch auf " +
                "Nachträge gewartet, ihn aber nicht sofort übernommen; vielleicht wurde sie gerade geschlossen. Er bleibt eingereiht und geht nur in genau diese Sitzung; " +
                "ob er ankommt, zeigt programmierauftraege. Eine neue Sitzung startet Jarvis nicht von selbst.")
            arbeitet -> Ergebnis("Angenommen und eingereiht: Nachtrag $nummer für Auftrag $id ($appName). Die Sitzung arbeitet gerade (letztes Lebenszeichen vor " +
                "${zeitspanne(vor)}) und bekommt ihn spätestens am Ende ihres aktuellen Schritts; zugestellt ist er, wenn programmierauftraege das zeigt.")
            else -> Ergebnis("Angenommen, aber NICHT zugestellt: Die Sitzung von Auftrag $id ($appName) ist gerade nicht erreichbar (" +
                (if (vor < 0) "seit dem letzten Neustart des Jarvis-Servers kein Lebenszeichen" else "letztes Lebenszeichen vor ${zeitspanne(vor)}") + "; " +
                (if (antwort.optBoolean("pc")) "der PC ist an" else "der PC ist aus oder die PC-Brücke läuft nicht") + "). Vermutlich ist die Sitzung geschlossen oder " +
                "wartet ohne Nachtrags-Empfang. Nachtrag $nummer bleibt sieben Tage eingereiht und geht nur in genau diese Sitzung, sobald sie sich wieder meldet. " +
                "Sag Frank das offen; einen Ersatzauftrag startet Jarvis nicht.")
        }
    }

    /** Sekunden seit der letzten Arbeit der Sitzung laut Relay (-1 = unbekannt). */
    private fun kontaktVor(antwort: JSONObject) = if (antwort.isNull("kontakt_vor_s")) -1 else antwort.optInt("kontakt_vor_s", -1)

    /** Arbeitet: kürzlich eine Meldung, und danach hat kein Warter mehr im Leerlauf abgefragt (sonst ist die Sitzung seitdem weg). */
    private fun arbeitet(antwort: JSONObject): Boolean {
        val vor = kontaktVor(antwort)
        val warterVor = if (antwort.isNull("warter_vor_s")) -1 else antwort.optInt("warter_vor_s", -1)
        return vor in 0..600 && (warterVor < 0 || warterVor > vor)
    }

    /** Legt [text] als Nachtrag [nummer] beim Relay für die Sitzung von Auftrag [id] ab; Antwort: zugestellt, wartet, kontakt_vor_s, pc. */
    private suspend fun anSitzung(id: String, nummer: String, text: String): JSONObject = withContext(Dispatchers.IO) {
        val rumpf = JSONObject().put("auftrag_id", id).put("nachtrag_id", nummer).put("text", text)
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/nachtrag").header("X-Jarvis-Token", e.serverToken)
            .post(rumpf.toString().toRequestBody("application/json".toMediaType())).build()
        HTTP.newCall(anfrage).execute().use { a ->
            if (!a.isSuccessful) throw IllegalStateException("HTTP ${a.code} vom Jarvis-Server")
            JSONObject(a.body?.string().orEmpty())
        }
    }

    /**
     * Die Session, mit der Jarvis spricht: der Auftrag des geöffneten Gesprächs (der laufende, sonst der jüngste), sonst der eine
     * noch laufende Auftrag, der aus diesem Gespräch gestartet wurde. Über das Plugin ohne Gespräch: [auftragId], sonst das in der
     * App geöffnete Gespräch. Liefert den Auftrag oder den Grund, warum keiner eindeutig ist.
     */
    private fun gespraechsZiel(gespraech: String, auftragId: String): Pair<JSONObject?, String> {
        fun laufend(e: JSONObject) = e.optString("stand") in listOf("laeuft", "frage", STAND_UNKLAR) + GESPERRT
        fun ausGespraech(g: String): Pair<JSONObject?, String>? {
            val hier = Auftragsbuch.alleImGespraech(app, g)
            hier.lastOrNull { laufend(it) }?.let { return it to "" }
            val gestartet = Auftragsbuch.gestartetAus(app, g).filter { laufend(it) }
            return when (gestartet.size) {
                0 -> hier.lastOrNull()?.let { it to "" }
                1 -> gestartet.first() to ""
                else -> null to "Aus diesem Gespräch laufen mehrere Programmier-Sessions; welche gemeint ist, ist nicht eindeutig. Frag Frank kurz, welche er meint " +
                    "(oder er öffnet sie in Jarvis).\n" + gestartet.reversed().joinToString("\n") { "- " + Auftragsbuch.kurzZeile(it) }
            }
        }
        if (gespraech.isNotEmpty()) ausGespraech(gespraech)?.let { return it }
        if (auftragId.isNotEmpty()) return (Auftragsbuch.finde(app, auftragId) ?: return null to "Einen Programmierauftrag $auftragId kennt Jarvis nicht.") to ""
        if (gespraech.isEmpty()) offenesGespraech()?.let { ausGespraech(it) }?.let { return it }
        return null to "Im geöffneten Gespräch läuft keine Programmier-Session."
    }

    /** Das in der App zuletzt geöffnete Gespräch (für Aufrufe über das Plugin, die kein eigenes Gespräch haben). */
    private fun offenesGespraech(): String? = ProgrammierChat.aktivesGespraech
        ?: app.getSharedPreferences("jarvis_offenes_gespraech", Context.MODE_PRIVATE).getString("id", null)?.takeIf { it.isNotBlank() }

    /** Die Nachricht so, wie sie in der Sitzung ankommt: Franks bzw. Jarvis' Text vorn (erste Zeile = Anzeige), danach die Anleitung. */
    private fun gespraechsText(art: String, nummer: String, id: String, text: String): String {
        val antworten = "programmier_fortschritt (MCP-Server Jarvis; kennt es kein Feld antwort_auf, beginne schritt mit „Antwort auf $nummer: “) " +
            "mit auftrag_id=$id, stand=laeuft, antwort_auf=$nummer und schritt="
        return if (art == ART_FRAGE)
            "$FRAGE_KOPF (Nachricht $nummer): $text\n\nJarvis (Franks Sprachassistent) wartet auf deine Antwort, um sie Frank vorzulesen. Antworte sofort, " +
                "bevor du weiterarbeitest: kurz, in ganzen Sätzen, gut vorlesbar und ohne Markdown, mit ${antworten}deiner Antwort. Brauchst du selbst eine Klärung, " +
                "stell die Gegenfrage genau so als deine Antwort; Jarvis antwortet dir mit einer neuen Nachricht. Danach arbeitest du an deiner bisherigen Aufgabe weiter."
        else
            "$WUNSCH_KOPF (Nachricht $nummer): $text\n\nEr gehört zu dieser Sitzung und ihrem Auftrag. Bestätige sofort in einem kurzen, gut " +
                "vorlesbaren Satz ohne Markdown mit ${antworten}was du jetzt tust (ist er unklar, stell stattdessen dort deine Gegenfrage). Setze ihn dann hier um, " +
                "schließe danach wieder nach den Profilregeln ab (bauen, Version, Commit, Push, Installation) und melde stand=fertig."
    }

    /**
     * Gespräch mit dem Programmierer der geöffneten Session (Franks Wunsch vom 25.10.2026): Frage oder Wunsch geht ohne Nachtrags-Prüfung
     * in genau diese Sitzung (über denselben Weg wie ein Nachtrag), danach wartet Jarvis kurz auf die Antwort (programmier_fortschritt
     * mit antwort_auf). Kommt sie später, steht sie im Gespräch und wird vorgelesen. Nie eine andere oder neue Sitzung.
     */
    private suspend fun mitProgrammierer(art: String, text: String, auftragId: String): Ergebnis {
        if (art !in listOf(ART_FRAGE, ART_WUNSCH, ART_WARTEN)) return Ergebnis("art muss frage, wunsch oder warten sein.", fehler = true)
        val (ausChatGpt, gespraech) = herkunft()
        val (ziel, grund) = gespraechsZiel(gespraech, auftragId)
        val eintrag = ziel ?: return Ergebnis("Nichts wurde gesendet: $grund Wähle nicht selbst eine Session und starte keine neue: Frag Frank kurz, welche Session er " +
            "meint oder ob er einen neuen Auftrag will (dann programmieren).", fehler = true)
        val id = eintrag.optString("id")
        val appName = eintrag.optString("app")
        if (art == ART_WARTEN) {
            val letzte = Auftragsbuch.letzteNachricht(eintrag)
                ?: return Ergebnis("Jarvis hat dem Programmierer von Auftrag $id ($appName) noch nichts geschrieben. Zuletzt gemeldet: ${eintrag.optString("schritt").take(500)}")
            letzte.optString("antwort").takeIf { it.isNotEmpty() }?.let { return antwortErgebnis(id, appName, it) }
            if (letzte.optString("stand") !in listOf("angenommen", "zugestellt"))
                return Ergebnis("Auf die letzte Nachricht an Auftrag $id ($appName) kommt keine Antwort mehr (${letzte.optString("stand")}). Zuletzt gemeldet: ${eintrag.optString("schritt").take(500)}")
            // seit = 0: Eine Rückfrage, die schon vor diesem Aufruf kam, meldet sich sofort.
            return warteAufAntwort(id, appName, letzte.optString("id"), 0L)
        }
        if (text.length < 3) return Ergebnis("Der Text ist leer. Formuliere die Frage bzw. den Wunsch vollständig aus.", fehler = true)
        eintrag.optJSONObject("offeneFrage")?.takeIf { eintrag.optString("stand") == "frage" }?.let { frage ->
            return Ergebnis("Nichts wurde gesendet: Der Programmierer von Auftrag $id ($appName) wartet gerade selbst auf eine Antwort (Rückfrage, frage_id " +
                "${frage.optString("frage_id")}): ${Auftragsbuch.frageText(frage)} Beantworte sie zuerst mit programmier_antwort (selbst, wenn sie sich aus dem Gespräch " +
                "ergibt, sonst frag Frank kurz) und sprich ihn danach an.", fehler = true)
        }
        val cloud = inDerCloud(eintrag.optString("ziel"))
        if (!NachtragsRegel.zustellbar(cloud, eintrag.optString("stand")))
            return Ergebnis("Nicht zustellbar: Die Cloud-Sitzung von Auftrag $id ($appName) ist beendet und lässt sich nicht mehr ansprechen. Nichts wurde gesendet; " +
                "sag Frank das offen und starte keine neue Session.", fehler = true)
        if (eintrag.optBoolean("lokal") && !eintrag.optBoolean("lokalHook"))
            return Ergebnis("Nicht zustellbar: Die Sitzung $id ($appName) wurde am PC ohne Jarvis-Anbindung gestartet; Jarvis liest dort nur mit. Nichts wurde gesendet. " +
                "Setzt Frank sie am PC mit claude --resume fort, kann Jarvis mit ihr sprechen.", fehler = true)
        val nummer = NACHRICHT + Auftragsbuch.neueNummer()
        val inhalt = gespraechsText(art, nummer, id, text)
        Auftragsbuch.nachtragNeu(app, id, inhalt, if (ausChatGpt) "ChatGPT/MCP" else "Jarvis", nachricht = nummer)
            ?: return Ergebnis("Den Auftrag $id gibt es nicht mehr im Auftragsbuch. Nichts wurde gesendet.", fehler = true)
        // Im Verlauf der Session sichtbar: Jarvis' Frage bzw. Franks Wunsch, darunter später die Antwort des Programmierers.
        eintrag.optString("gespraech").takeIf { it.isNotEmpty() }?.let {
            ProgrammierChat.poste(app, it, ProgrammierChat.zeile(ProgrammierChat.VON_JARVIS,
                (if (art == ART_FRAGE) "Frage an den Programmierer: " else "Franks Wunsch an den Programmierer: ") + text, "💬"), ProgrammierChat.ART_STATUS)
        }
        val seit = System.currentTimeMillis()
        if (cloud) return Ergebnis("Angenommen: Die Cloud-Sitzung von Auftrag $id ($appName) bekommt die ${if (art == ART_FRAGE) "Frage" else "Nachricht"} mit ihrer nächsten " +
            "Fortschrittsmeldung. Ihre Antwort erscheint dann im Gespräch und wird vorgelesen; abholen kannst du sie mit art=warten. Sag Frank das kurz.")
        // Vor dem Senden anmelden: Eine wartende Sitzung antwortet womöglich schneller, als der Relay zurückkommt.
        val wartend = CompletableDeferred<String>().also { gespraechsAntworten[nummer] = it }
        val antwort = runCatching { anSitzung(id, nummer, inhalt) }.getOrElse { fehler ->
            gespraechsAntworten.remove(nummer)
            Auftragsbuch.setzeNachtraege(app, id, setOf(nummer), setOf("angenommen"), "fehler")
            return Ergebnis("Die Nachricht ist nicht beim Jarvis-Server angekommen (${fehler.message ?: fehler.javaClass.simpleName}); nichts wurde zugestellt. Sag Frank das kurz.", fehler = true)
        }
        if (antwort.optBoolean("zugestellt")) alsZugestellt(id, setOf(nummer), "")
        if (!antwort.optBoolean("zugestellt") && !antwort.optBoolean("wartet") && !arbeitet(antwort)) {
            gespraechsAntworten.remove(nummer)
            return Ergebnis("Angenommen, aber NICHT zugestellt: Die Sitzung von Auftrag $id ($appName) ist gerade nicht erreichbar" +
                (if (antwort.optBoolean("pc")) "" else ", der Rechner ist aus oder die PC-Brücke läuft nicht") + ". Die Nachricht bleibt eingereiht und geht nur in genau diese " +
                "Sitzung, sobald sie sich meldet; die Antwort erscheint dann im Gespräch und wird vorgelesen. Sag Frank das offen.")
        }
        return warteAufAntwort(id, appName, nummer, seit, wartend)
    }

    /**
     * Wartet bis [GESPRAECH_WARTEN_MS] auf die Antwort des Programmierers auf [nummer]. Stellt er stattdessen eine Rückfrage
     * (AskUserQuestion), kommt sie sofort zurück. Danach gilt die Antwort als „spät“: Sie wird beim Eintreffen vorgelesen.
     */
    private suspend fun warteAufAntwort(id: String, appName: String, nummer: String, seit: Long,
                                        wartend: CompletableDeferred<String> = gespraechsAntworten.getOrPut(nummer) { CompletableDeferred() }): Ergebnis {
        try {
            val ende = System.currentTimeMillis() + GESPRAECH_WARTEN_MS
            while (System.currentTimeMillis() < ende) {
                val e = Auftragsbuch.finde(app, id) ?: break
                Auftragsbuch.nachricht(e, nummer)?.optString("antwort")?.takeIf { it.isNotEmpty() }?.let { return antwortErgebnis(id, appName, it) }
                val frage = e.optJSONObject("offeneFrage")
                if (frage != null && e.optString("stand") == "frage" && frage.optLong("eingang") >= seit)
                    return Ergebnis("GEGENFRAGE des Programmierers (Auftrag $id, $appName, frage_id ${frage.optString("frage_id")}): ${Auftragsbuch.frageText(frage)}\n\n" +
                        "Beantworte sie mit programmier_antwort: selbst, wenn sie sich aus Gespräch und Auftrag ergibt, sonst frag Frank kurz. Seine Antwort auf deine " +
                        "Nachricht holst du danach mit programmierer_sprechen art=warten ab.")
                withTimeoutOrNull(3_000L) { wartend.await() }?.let { return antwortErgebnis(id, appName, it) }
            }
        } finally {
            // Hat gespraechsAntwort das Deferred schon genommen, kommt die Antwort gleich: dann sie liefern statt „steht noch aus“.
            if (!gespraechsAntworten.remove(nummer, wartend)) withTimeoutOrNull(2_000L) { wartend.await() }?.let { return antwortErgebnis(id, appName, it) }
        }
        return Ergebnis("Die Nachricht ist beim Programmierer von Auftrag $id ($appName), seine Antwort steht noch aus (er ist mitten in einem Arbeitsschritt). " +
            "Sag Frank das kurz: Sobald sie kommt, erscheint sie im Gespräch und Jarvis liest sie vor. Abholen kannst du sie mit art=warten.")
    }

    private fun antwortErgebnis(id: String, appName: String, antwort: String) =
        Ergebnis("Antwort des Programmierers (Auftrag $id, $appName): $antwort\n\nGib sie Frank kurz und natürlich wieder. Stellt sie eine Gegenfrage, beantworte " +
            "sie mit einer neuen Nachricht (programmierer_sprechen): selbst, wenn sie sich aus Gespräch und Auftrag ergibt, sonst frag Frank kurz.")

    /**
     * Der Programmierer hat auf die Gesprächsnachricht [nachricht] geantwortet (programmier_fortschritt mit antwort_auf): an die
     * wartende Frage weitergeben; wartet niemand mehr, liest Jarvis die Antwort vor (im Gespräch steht sie schon über verteile).
     */
    private fun gespraechsAntwort(eintrag: JSONObject, nachricht: String, antwort: String) {
        val id = eintrag.optString("id")
        val nummer = nachricht.uppercase()
        Auftragsbuch.nachrichtBeantwortet(app, id, nummer, antwort)
        if (gespraechsAntworten.remove(nummer)?.complete(antwort) == true) return
        val art = de.frank.jarvis.meldung.Meldungsarten.PROGRAMMIEREN
        if (de.frank.jarvis.meldung.Meldungen.vorlesenAn(app, art))
            de.frank.jarvis.meldung.Meldungen.vorlesenSpaeter(app, art, "Antwort vom Programmierer: ${eintrag.optString("app")}",
                "Der Programmierer antwortet: $antwort", "gespraech-$nummer")
    }

    private fun aktivitaetMelden(eintrag: JSONObject) = ProgrammierChat.setze(
        eintrag.optString("gespraech"),
        ProgrammierChat.Aktivitaet(eintrag.optString("app"), eintrag.optBoolean("arbeitet"), eintrag.optLong("aktiv", eintrag.optLong("zeit")), eintrag.optString("schritt"), eintrag.optLong("start"),
            lokal = eintrag.optBoolean("lokal")),
    )

    /** Beim Start der App: die drehende Anzeige für Aufträge, die noch arbeiten, aus dem Auftragsbuch herstellen. */
    fun aktivitaetLaden() = Auftragsbuch.juengste(app).forEach { aktivitaetMelden(it) }

    /**
     * Wird der Abschluss dieses Auftrags gleich vorgelesen? Liefert den Sprachweg, sonst null: nicht im Sprachgespräch gestellt,
     * noch nicht fertig, für diesen Abschluss schon angesagt, ein Nachtrag wartet noch (die Sitzung arbeitet weiter) oder die
     * Regeln der Sprachzustellung sprechen dagegen (bewusst beendet, Vorlesen aus, Ruhezeit „Programmieraufträge“).
     */
    private fun sprachAbschlussGeplant(eintrag: JSONObject): de.frank.jarvis.speech.SprachModus? {
        val modus = de.frank.jarvis.speech.SprachModus.aus(eintrag.optString("sprache")) ?: return null
        if (eintrag.optString("stand") !in listOf("fertig", "fehler")) return null
        if (eintrag.optString("sprachAngesagt") == ansageSchluessel(eintrag)) return null
        if (Auftragsbuch.nachtragOffen(eintrag)) return null
        val ausgang = de.frank.jarvis.speech.SprachZustellung.ausgang(app, modus, eintrag.optLong("sprachSeit"), de.frank.jarvis.meldung.Meldungsarten.PROGRAMMIEREN)
        if (ausgang != de.frank.jarvis.speech.SprachZustellung.Ausgang.SPRECHEN) {
            Logbuch.sprache("Sprachzustellung", "auftrag:${eintrag.optString("id")}: ${ausgang.text}")
            return null
        }
        return modus
    }

    /** Für diesen Abschluss schon angesagt (ein wiederholtes fertig): auch die Benachrichtigung bleibt dann stumm. */
    private fun schonAngesagt(eintrag: JSONObject): Boolean = eintrag.optString("sprachAngesagt") == ansageSchluessel(eintrag)

    /** Je Abschluss einmal: Ein wiederholtes fertig spricht nichts doppelt, ein Abschluss nach bearbeitetem Nachtrag bekommt seine eigene Ansage. */
    private fun ansageSchluessel(eintrag: JSONObject): String {
        val l = eintrag.optJSONArray("nachtraege")
        val bearbeitet = (0 until (l?.length() ?: 0)).count { l!!.optJSONObject(it)?.optString("stand") == "bearbeitet" }
        return "auftrag:${eintrag.optString("id")}:$bearbeitet:${eintrag.optString("stand")}"
    }

    /**
     * Ein im Sprachgespräch gestellter Auftrag ist zu Ende: kurze Zusammenfassung (was eingebaut ist, Commit, Push, Installation
     * mit Weg, APK bei UpdateStation, je nur aus Belegen) ins Gespräch, in dem Frank gesprochen hat, und über die Sprachzustellung
     * vorlesen. Die Zustellung wartet auf einen freien Moment (keine zweite Stimme), danach geht es im Sprachweg des Auftrags weiter
     * (Live nur, wenn „Live nach einem Ergebnis automatisch neu verbinden“ an ist). Der vollständige Bericht bleibt in der Sitzung.
     */
    private fun sprachAbschluss(eintrag: JSONObject) {
        val modus = sprachAbschlussGeplant(eintrag) ?: return
        val id = eintrag.optString("id")
        val schluessel = ansageSchluessel(eintrag)
        if (!Auftragsbuch.ansageVormerken(app, id, schluessel)) return
        val fertig = eintrag.optString("stand") == "fertig"
        val texte = eintrag.optJSONArray("schritte")?.let { l -> (0 until l.length()).map { l.optJSONObject(it)?.optString("text").orEmpty() } }.orEmpty() +
            eintrag.optString("schritt")
        val text = ProgrammierAbschluss.vorlesetext(eintrag.optString("schritt"), ProgrammierAbschluss.fakten(texte),
            cloud = inDerCloud(eintrag.optString("ziel")), fertig = fertig)
        val appName = eintrag.optString("app")
        val gespraech = eintrag.optString("sprachGespraech").ifEmpty { eintrag.optString("gespraech") }
        // Im Auftragsgespräch steht der Abschluss schon (✅ und Bericht); nur ein anderes Sprachgespräch bekommt die Kurzfassung dazu.
        if (gespraech.isNotEmpty() && gespraech != eintrag.optString("gespraech"))
            ProgrammierChat.poste(app, gespraech, "🔊 Programmierauftrag $id ($appName), kurz:\n\n$text", "antwort")
        Logbuch.sprache("Sprachzustellung", "auftrag:$id: Abschluss angenommen (${modus.anzeige}, ${text.length} Zeichen)")
        ansagen.launch {
            runCatching {
                de.frank.jarvis.ui.AppViewModel.zustellen(app, de.frank.jarvis.speech.Zustellung(schluessel, modus, gespraech, eintrag.optLong("sprachSeit"),
                    ProgrammierAbschluss.ansage(appName, fertig), text, ruhe = de.frank.jarvis.meldung.Meldungsarten.PROGRAMMIEREN))
            }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it; Logbuch.fehler("Sprachzustellung Programmierauftrag", it) }
        }
    }

    /** Benachrichtigung aktualisieren und die Meldung ins Gespräch des Auftrags schreiben. */
    private fun verteile(eintrag: JSONObject, frage: JSONObject?, benachrichtigen: Boolean = true, von: String = ProgrammierChat.VON_PROGRAMMIERER) {
        aktivitaetMelden(eintrag)
        // Zwischenschritte stehen vollständig im Gespräch (sie kommen über den Tunnel in die App). Eine Benachrichtigung gibt es
        // nur, wenn Frank gebraucht wird oder der Auftrag zu Ende ist: Rückfrage, fertig (App installiert) oder Problem.
        val letzter = eintrag.optString("letzterStand", eintrag.optString("stand"))
        // Spricht gleich die Sprachzustellung den Abschluss, liest die Benachrichtigung nichts vor (keine zweite, konkurrierende Stimme).
        if (benachrichtigen && (frage != null || letzter == "fertig" || letzter == "fehler" || letzter in GESPERRT))
            // Eine Rückfrage liest rueckfrageVorn vor (auch bei ausgeschaltetem Handy), nicht die Benachrichtigung.
            benachrichtige(eintrag, vorlesen = frage == null && sprachAbschlussGeplant(eintrag) == null && !schonAngesagt(eintrag))
        val gespraech = eintrag.optString("gespraech")
        val fragen = frage?.optJSONArray("fragen") ?: JSONArray()
        val kurz = (0 until fragen.length()).joinToString(" / ") { fragen.optJSONObject(it)?.optString("question").orEmpty() }.trim()
        if (gespraech.isEmpty()) {
            // Ohne Gespräch kein Vollbild, aber die Rückfrage wird trotzdem angesagt.
            if (frage != null && benachrichtigen) rueckfrageVorn(eintrag, frage, kurz)
            return
        }
        val appName = eintrag.optString("app")
        val schritt = eintrag.optString("schritt")
        if (frage != null) {
            // „aufgabe“: die Aufgabe der Sitzung, kurz, damit Karte und Vollbild zeigen, welcher von mehreren Aufträgen fragt.
            val kontext = JSONObject().put("frage_id", frage.optString("frage_id")).put("auftrag", eintrag.optString("id")).put("app", appName)
                .put("aufgabe", RueckfrageAufgabe.aus(eintrag)).put("fragen", fragen).put("frei", frage.optBoolean("frei")).put("beantwortet", false)
                .put("status", RueckfrageStand.OFFEN).put("bis", frage.optLong("bis")).put("eingang", System.currentTimeMillis() / 1000)
            ProgrammierChat.poste(app, gespraech, "❓ ${ProgrammierChat.VON_PROGRAMMIERER} · Rückfrage zu $appName: " + (if (frage.optBoolean("frei")) schritt.removePrefix("❓").trim() else kurz), "rueckfrage", kontext.toString())
            if (benachrichtigen) rueckfrageVorn(eintrag, frage, kurz)
            return
        }
        val zeichen = when (letzter) { "fertig" -> "✅"; "fehler" -> "❌"; "info" -> "📋"; in GESPERRT -> "⚠️"; STAND_UNKLAR -> "⏳"; else -> if (von == ProgrammierChat.VON_JARVIS) "🤖" else "⚙️" }
        // Stand „info“ schickt der Hook nur am Zugende mit der ganzen letzten Nachricht der Sitzung: der ausführliche
        // Abschlussbericht. Er bekommt eine eigene Art (volle Aktionsleiste) und bleibt ein Ganzes; alles andere ist Statusmeldung,
        // je Teil eine Zeile mit Symbol und Absender.
        if (letzter == "info") {
            ProgrammierChat.poste(app, gespraech, ProgrammierChat.zeile(von, schritt, zeichen), ProgrammierChat.ART_BERICHT)
            return
        }
        ProgrammierChat.zeilen(von, schritt, zeichen).forEach { ProgrammierChat.poste(app, gespraech, it, ProgrammierChat.ART_STATUS) }
    }

    /**
     * Gibt Franks Antwort auf eine Rückfrage an den wartenden Hook weiter. [antworten] in der Reihenfolge der Fragen (label der
     * Option, bei Mehrfachauswahl mit Komma, bei „Eigene Antwort“ Franks Text), [text] als freie Antwort. Mit [freigeben] statt
     * einer Antwort: Die Sitzung wartet nicht länger auf Jarvis, die Frage erscheint sofort am Rechner und in der Claude-App.
     * Der Stand steht danach an der Rückfrage im Gespräch. Liefert einen Satz für Frank; Fehler als Ausnahme (mit Grund).
     */
    suspend fun beantworte(frageId: String, antworten: List<String>, text: String, freigeben: Boolean = false): String = withContext(Dispatchers.IO) {
        if (frageId.isEmpty()) throw IllegalArgumentException("Die frage_id fehlt.")
        if (!freigeben && antworten.none { it.isNotEmpty() } && text.isEmpty()) throw IllegalArgumentException("Die Antwort ist leer.")
        val eintrag = Auftragsbuch.zuFrage(app, frageId)
        val gespraech = eintrag?.optString("gespraech").orEmpty()
        val fragen = Auftragsbuch.offeneFrage(app, frageId)?.second?.optJSONArray("fragen")
            ?: (if (gespraech.isNotEmpty()) de.frank.jarvis.data.Gespraeche.rueckfrageKontext(app, gespraech, frageId)?.optJSONArray("fragen") else null)
            ?: JSONArray()
        val zuordnung = JSONObject()
        antworten.forEachIndexed { i, antwort ->
            val frage = fragen.optJSONObject(i)?.optString("question").orEmpty()
            if (frage.isNotEmpty() && antwort.isNotEmpty()) zuordnung.put(frage, antwort)
        }
        val rumpf = JSONObject().put("frage_id", frageId).put("antworten", zuordnung).put("text", text.ifEmpty { if (zuordnung.length() == 0) antworten.joinToString("; ") else "" })
        val antwortText = if (freigeben) "" else if (zuordnung.length() > 0) antworten.filter { it.isNotEmpty() }.joinToString("; ") + (if (text.isNotEmpty()) " ($text)" else "") else rumpf.optString("text")
        // Gleicher Inhalt = gleiche Kennung: Ein zweiter Versuch nach verlorener Antwort des Servers gilt als derselbe, nie doppelt.
        val antwortId = java.security.MessageDigest.getInstance("SHA-256").digest((frageId + "|" + freigeben + "|" + rumpf.toString()).toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }
        rumpf.put("antwort_id", antwortId).put("freigeben", freigeben)
        fun stand(s: String, antwort: String = antwortText, grund: String = "") {
            if (gespraech.isNotEmpty()) ProgrammierChat.rueckfrageStand(app, gespraech, frageId, s, antwort, grund)
        }
        // Ohne Antworttext: Der steht erst an der Rückfrage, wenn der Server sie angenommen hat (sonst zeigte sie nach einem
        // Fehler oder einer früher angenommenen Antwort einen Text, der nie ankam).
        stand(RueckfrageStand.SENDET, antwort = "")
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/antwort").header("X-Jarvis-Token", e.serverToken)
            .post(rumpf.toString().toRequestBody("application/json".toMediaType())).build()
        val (code, roh) = runCatching { HTTP.newCall(anfrage).execute().use { it.code to it.body?.string().orEmpty() } }.getOrElse { fehler ->
            val grund = "Keine Verbindung zum Jarvis-Server (${fehler.message ?: fehler.javaClass.simpleName}). Die Antwort ist nicht angekommen; erneut senden."
            stand(RueckfrageStand.FEHLER, grund = grund)
            throw IllegalStateException(grund)
        }
        val json = runCatching { JSONObject(roh) }.getOrDefault(JSONObject())
        if (code !in 200..299) {
            val grund = json.optString("grund").takeIf { it.isNotEmpty() } ?: "Der Jarvis-Server hat die Antwort nicht angenommen (HTTP $code)."
            // Endgültig nur, was der Relay selbst als Ausgang nennt (status im Rumpf). Ein nacktes 404 (Schlüssel abgelehnt,
            // Proxy beim Neustart) heißt nicht, dass die Sitzung weg ist: dann Fehler mit „Erneut senden“.
            val ausgang = if (code in setOf(404, 409, 410)) standVomServer(json.optString("status")) else null
            if (ausgang != null) {
                stand(ausgang, json.optString("antwort_text").ifEmpty { if (ausgang in RueckfrageStand.BEANTWORTET_ALLE) "eine frühere Antwort" else "" }, grund)
                Auftragsbuch.frageErledigt(app, frageId)
                if (ausgang in RueckfrageStand.BEANTWORTET_ALLE) rueckfrageMeldungWeg(eintrag?.optString("id").orEmpty(), frageId)
            } else stand(RueckfrageStand.FEHLER, grund = grund)
            throw IllegalStateException(grund)
        }
        Auftragsbuch.frageErledigt(app, frageId)
        eintrag?.let { Auftragsbuch.finde(app, it.optString("id"))?.let { e -> aktivitaetMelden(e) } }
        if (freigeben) {
            stand(RueckfrageStand.AM_PC, "", "freigegeben")
            return@withContext "Die Frage liegt jetzt am Rechner und in der Claude-App; beantworte sie dort."
        }
        // Angenommen heißt: Der Server hat sie, die Sitzung holt sie in Sekunden ab. Ihre Bestätigung setzt „beantwortet“.
        stand(RueckfrageStand.ANGENOMMEN)
        rueckfrageMeldungWeg(eintrag?.optString("id").orEmpty(), frageId)
        "Die Antwort „$antwortText“ ist beim Jarvis-Server; die Sitzung übernimmt sie jetzt."
    }

    /**
     * Das Gespräch eines noch laufenden Auftrags (läuft oder wartet auf eine Antwort, letzte Meldung höchstens zwei Stunden
     * alt), oder null. Die App öffnet es beim Start: Nach einem Update von Jarvis selbst (die Sitzung installiert die neue
     * Version, Android beendet dabei die App) geht es dort weiter, wo Frank war.
     */
    fun laufendesGespraech(): String? = Auftragsbuch.laufendesGespraech(app)

    /** Je Gespräch die App und das Ziel seiner Programmieraufträge, für die Suche „die Whoop-Session“. */
    fun auftraegeJeGespraech(): Map<String, String> = Auftragsbuch.jeGespraech(app)

    /** Je Gespräch Stand, App und Inhalt seiner Programmieraufträge, frisch aus dem Auftragsbuch: für den Wechsel per Sprache. */
    fun auftragsLagen(): Map<String, AuftragsLage> = Auftragsbuch.lagen(app)

    /** Offene und zuletzt beendete Programmieraufträge als Zeilen für vorgaenge und den Startkontext. */
    fun vorgangsZeilen(alle: Boolean = false): List<String> = Auftragsbuch.vorgangsZeilen(app, alle)

    /** Die letzten Programmieraufträge mit Stand, letztem Schritt und Auftragstext (für die Aktualitätsprüfung des Lernlaufs; nur lesen). */
    fun auftragsUebersicht(anzahl: Int = 20): String = Auftragsbuch.uebersicht(app, anzahl)

    /**
     * Beantwortete Rückfrage: Ihre Benachrichtigung verschwindet, aber nur, solange sie noch genau diese Frage zeigt (eine
     * neuere Frage oder ein neuer Stand desselben Auftrags bleibt stehen, Meldungen anderer Aufträge sowieso).
     */
    private fun rueckfrageMeldungWeg(auftragId: String, frageId: String) {
        if (auftragId.isEmpty() || frageId.isEmpty()) return
        val nummer = 300_000 + (auftragId.hashCode() and 0xFFFF)
        runCatching {
            val nm = app.getSystemService(android.app.NotificationManager::class.java)
            if (nm.activeNotifications.any { it.id == nummer && it.notification.extras.getString(EXTRA_RUECKFRAGE) == frageId }) nm.cancel(nummer)
        }
    }

    /** Eine Benachrichtigung je Auftrag, nur bei Rückfrage, Fertig und Problem; ein Tipp öffnet die Sitzung oder die Claude-App. */
    private fun benachrichtige(eintrag: JSONObject, vorlesen: Boolean = true) {
        runCatching {
            JarvisDienst.kanalAnlegen(app)
            val link = eintrag.optString("link")
            val stand = eintrag.optString("stand")
            val frageId = eintrag.optJSONObject("offeneFrage")?.optString("frage_id").orEmpty()
            // Eine Rückfrage öffnet Jarvis im Gespräch des Auftrags mit der Frage im Vollbild; sonst die Sitzung bzw. die Claude-App.
            val oeffnen = if (stand == "frage" && frageId.isNotEmpty() && eintrag.optString("gespraech").isNotEmpty())
                Intent(app, de.frank.jarvis.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(EXTRA_GESPRAECH, eintrag.optString("gespraech")).putExtra(EXTRA_RUECKFRAGE, frageId)
            else if (link.startsWith("https://")) Intent(Intent.ACTION_VIEW, Uri.parse(link))
                else app.packageManager.getLaunchIntentForPackage(CLAUDE_APP) ?: app.packageManager.getLaunchIntentForPackage(app.packageName)
            val appName = eintrag.optString("app")
            val titel = when (stand) {
                "frage" -> "Rückfrage: $appName"
                "fertig" -> "Fertig programmiert: $appName"
                "fehler" -> "Problem beim Programmieren: $appName"
                STAND_ANMELDUNG -> "Anmeldung erforderlich: $appName"
                STAND_STOERUNG -> "Programmieren angehalten: $appName"
                else -> "Jarvis programmiert: $appName"
            }
            // In der Benachrichtigung kein Markdown: Fettungen gibt es nur im Chat.
            val text = eintrag.optString("schritt").replace("**", "")
            val nummer = 300_000 + (eintrag.optString("id").hashCode() and 0xFFFF)
            // Rückfragen sind eine eigene Meldung mit eigenem Schalter (Franks Wunsch vom 23.10.2026), getrennt von Fertig und Problem.
            val rueckfrage = stand == "frage"
            val art = if (rueckfrage) de.frank.jarvis.meldung.Meldungsarten.RUECKFRAGE else de.frank.jarvis.meldung.Meldungsarten.PROGRAMMIEREN
            // Ist die Folgemeldung abgeschaltet, bliebe sonst die alte Rückfrage-Benachrichtigung stehen.
            if (!rueckfrage && !de.frank.jarvis.meldung.Meldungen.an(app, art))
                app.getSystemService(android.app.NotificationManager::class.java).cancel(nummer)
            de.frank.jarvis.meldung.Meldungen.zeige(
                app, art,
                nummer,
                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis)
                    .setContentTitle(titel)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n${eintrag.optString("ziel")} · Auftrag ${eintrag.optString("id")}"))
                    // Zwischenschritte still aktualisieren; Rückfrage, Fertig und Problem melden sich hörbar.
                    .setOnlyAlertOnce(stand == "laeuft")
                    .setAutoCancel(stand == "fertig")
                    .apply {
                        oeffnen?.let {
                            val tippen = PendingIntent.getActivity(app, nummer, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                            setContentIntent(tippen)
                            // Rückfrage bei gesperrtem Bildschirm: als Vollbild, nicht nur als Zeile in der Leiste. Android löst das
                            // nur auf einem Kanal mit hoher Wichtigkeit aus (jarvis_ergebnisse ist DEFAULT), daher ein eigener Kanal.
                            if (stand == "frage" && frageId.isNotEmpty()) {
                                de.frank.jarvis.meldung.Meldungen.rueckfrageKanalAnlegen(app)
                                setChannelId(de.frank.jarvis.meldung.Meldungen.KANAL_RUECKFRAGE).setCategory(NotificationCompat.CATEGORY_CALL)
                                    .setPriority(NotificationCompat.PRIORITY_HIGH).setFullScreenIntent(tippen, true)
                                    // Woran rueckfrageMeldungWeg erkennt, dass die Meldung noch genau diese Frage zeigt.
                                    .addExtras(android.os.Bundle().apply { putString(EXTRA_RUECKFRAGE, frageId) })
                                if (android.os.Build.VERSION.SDK_INT >= 34 &&
                                    !app.getSystemService(android.app.NotificationManager::class.java).canUseFullScreenIntent())
                                    Logbuch.system("Rückfrage ohne Vollbild-Benachrichtigung", "Vollbild-Benachrichtigungen sind für Jarvis nicht erlaubt")
                            }
                        }
                    },
                // Zwischenschritte still; Rückfrage, Fertig und Problem nach Franks Einstellung (Ton, Wecker, Vorlesen).
                // Eine Sperre stört kein Live-Gespräch (still, aber sichtbar); Rückfrage, Fertig und Problem wie bisher auch dort mit Ton.
                titel, if (vorlesen) "$titel. $text" else null, laut = stand != "laeuft", imGespraechStill = stand in GESPERRT,
            )
        }
    }

    /**
     * Franks Vorgabe vom 23.10.2026: Eine Rückfrage hat Vorrang vor allem. Sie geht sofort als Vollbild auf, egal ob Frank in
     * einem anderen Gespräch, in den Einstellungen oder in einer anderen App ist (aus dem Hintergrund darf Jarvis das mit
     * „Über anderen Apps einblenden“, wie beim Weckwort); nach dem Entsperren steht das Vollbild schon vorn. Ansage und
     * Benachrichtigung folgen der eigenen Meldung „Rückfragen vom Programmierer“ (an: kurzer Ton, dann vorgelesen).
     * Erst nach dem Posten ins Gespräch aufrufen: Das Vollbild sucht die Frage dort.
     */
    private fun rueckfrageVorn(eintrag: JSONObject, frage: JSONObject, kurz: String) {
        val appName = eintrag.optString("app")
        val gespraech = eintrag.optString("gespraech")
        val frageId = frage.optString("frage_id")
        if (gespraech.isNotEmpty() && frageId.isNotEmpty()) {
            val darf = android.os.Build.VERSION.SDK_INT < 29 || android.provider.Settings.canDrawOverlays(app) || de.frank.jarvis.MainActivity.sichtbar
            if (darf) {
                val absicht = Intent(app, de.frank.jarvis.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(EXTRA_GESPRAECH, gespraech).putExtra(EXTRA_RUECKFRAGE, frageId)
                runCatching { app.startActivity(absicht) }.onFailure { Logbuch.fehler("Rückfrage ließ sich nicht nach vorn holen", it) }
            } else {
                Logbuch.system("Rückfrage nur als Benachrichtigung", "„Über anderen Apps einblenden“ ist für Jarvis nicht erlaubt")
            }
        }
        // Eine freie Rückfrage trägt die ganze letzte Nachricht der Sitzung: Die eigentliche Frage steht an ihrem Ende.
        val frageText = if (frage.optBoolean("frei")) kurz.lines().map { it.trim() }.filter { it.isNotEmpty() }.takeLast(2).joinToString(" ").takeLast(300)
            else kurz.take(300)
        // „bis 100 Befunde“ gehört zur Antwortmöglichkeit „Ja, sehr ausführlich“ der Review-Frage, nicht zu einer Frage nach Befunden.
        val ohneUmfang = frageText.replace(Regex("(?i)(?:bis(?:\\s+zu)?|max\\.?|maximal|höchstens)\\s+\\d+\\s+befund\\w*"), "")
        val review = Regex("(?i)code[- ]?review").containsMatchIn(ohneUmfang) && !Regex("(?i)\\b(?:befund|beheb|fix)").containsMatchIn(ohneUmfang)
        // Ein kurzer fester Satz gleich beim Eintreffen (= Beginn des Countdowns), nach dem kurzen Ton der Benachrichtigung;
        // die Frage selbst steht im Vollbild. Franks Vorgabe vom 23.10.2026: Die Code-Review-Frage nur allgemein ansagen,
        // ohne Projekt, App, Ziel oder Inhalt der Frage.
        val satz = if (review) "Eine Code-Review-Frage liegt vor, der Programmierer wartet auf deine Antwort."
            else "Eine Rückfrage vom Programmierer zu $appName muss beantwortet werden."
        val art = de.frank.jarvis.meldung.Meldungsarten.RUECKFRAGE
        if (de.frank.jarvis.meldung.Meldungen.vorlesenAn(app, art)) {
            Logbuch.system("Rückfrage-Ansage eingereiht", "Auftrag ${eintrag.optString("id")}")
            de.frank.jarvis.meldung.Meldungen.vorlesenSpaeter(app, art, if (review) "Code-Review-Frage" else "Rückfrage: $appName", satz, "rueckfrage-$frageId")
        }
    }

    /** Eine erledigte Rückfrage (beantwortet, abgelaufen, am Rechner) wird nicht mehr nachträglich angesagt. */
    private fun rueckfrageAnsageAbbrechen(frageId: String) =
        de.frank.jarvis.meldung.Meldungen.vorlesenAbbrechen(de.frank.jarvis.meldung.Meldungsarten.RUECKFRAGE, "rueckfrage-$frageId")

    /** Die letzten Programmieraufträge mit ihren Schritten, dauerhaft in den App-Einstellungen (höchstens 20). */
    private object Auftragsbuch {
        private const val DATEI = "programmieren"
        private const val SCHLUESSEL = "auftraege"
        private val ZEIT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M. HH:mm", Locale.GERMAN)

        fun neueNummer(): String {
            val zeichen = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return (1..5).map { zeichen[java.security.SecureRandom().nextInt(zeichen.length)] }.joinToString("")
        }

        private fun lies(context: Context): JSONArray =
            runCatching { JSONArray(context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).getString(SCHLUESSEL, "[]")) }.getOrDefault(JSONArray())

        private fun schreib(context: Context, liste: JSONArray) {
            // Die letzten 60 Aufträge, dazu ältere, die noch laufen oder auf Frank warten (höchstens drei Tage alt): Sonst fiel
            // eine lange Sitzung mit vielen neueren Aufträgen heraus, und ihre Meldungen und Rückfragen kamen nirgends mehr an.
            val grenze = System.currentTimeMillis() - 3 * 86_400_000L
            val gekuerzt = JSONArray().apply {
                for (i in 0 until liste.length()) {
                    val e = liste.optJSONObject(i) ?: continue
                    if (i >= liste.length() - 60 || (offen(e) && zuletzt(e) >= grenze)) put(e)
                }
            }
            context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit().putString(SCHLUESSEL, gekuerzt.toString()).apply()
        }

        @Synchronized
        fun neu(context: Context, id: String, app: String, ziel: String, link: String, schritt: String, gespraech: String, auftrag: String, stand: String = "laeuft",
                ursprung: de.frank.jarvis.speech.SprachUrsprung? = null): JSONObject {
            val jetzt = System.currentTimeMillis()
            val eintrag = JSONObject().put("id", id).put("app", app).put("ziel", ziel).put("link", link).put("start", jetzt).put("gespraech", gespraech)
                .put("auftrag", auftrag.take(1_500))
                .put("stand", stand).put("schritt", schritt).put("zeit", jetzt).put("aktiv", jetzt).put("arbeitet", stand == "laeuft")
                .put("schritte", JSONArray().put(JSONObject().put("zeit", jetzt).put("stand", stand).put("text", schritt)))
            // Sprachursprung: Weg (weckwort/live), Gespräch, in dem Frank gesprochen hat, und Beginn der Anfrage (für ein bewusstes Ende).
            if (ursprung != null) eintrag.put("sprache", ursprung.modus.kennung).put("sprachGespraech", ursprung.gespraech).put("sprachSeit", ursprung.seit)
            schreib(context, lies(context).put(eintrag))
            AuftragsVolltext.speichern(context, id, auftrag)
            // Auch ein normales Gespräch, aus dem der Auftrag kam, gehört ab jetzt in den Reiter „Programmierung“.
            Gespraeche.markieren(context, gespraech, jetzt)
            return eintrag
        }

        /** Gespräche mit Auftrag und wann ihr erster Auftrag abgeschickt wurde (0 = Eintrag ohne Start). */
        fun gespraechStarts(context: Context): Map<String, Long> = Gespraeche.fruehesteStarts(alle(context).map { it.optString("gespraech") to it.optLong("start") })

        @Synchronized
        fun melde(context: Context, id: String, stand: String, schritt: String, link: String, frage: JSONObject? = null, sperrSeit: Long = 0L,
                  leerlauf: Boolean = false, hookBeginn: Long = 0L): JSONObject? {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return null
            val jetzt = System.currentTimeMillis()
            val gueltig = if (stand in listOf("laeuft", "frage", "fertig", "fehler", "info", STAND_ANMELDUNG, STAND_STOERUNG)) stand else "laeuft"
            // „info“ (Zwischenstand am Zugende) ändert den Stand nicht: Ein fertiger Auftrag bleibt fertig.
            if (gueltig != "info") eintrag.put("stand", gueltig)
            // Seit wann gesperrt (Sekunden seit 1970, Zeit des Fehlers): nur was danach begann, hebt die Sperre auf.
            if (sperrSeit > 0) eintrag.put("gesperrtSeit", sperrSeit)
            // Gespeichert gekürzt (das Auftragsbuch wird bei jedem Lebenszeichen gelesen und geschrieben); der volle Text geht
            // mit dem zurückgegebenen Eintrag ins Gespräch (verteile) und steht dort vollständig.
            eintrag.put("letzterStand", gueltig).put("schritt", ProgrammierAbschluss.kuerzen(schritt, ProgrammierAbschluss.MAX_MELDUNG)).put("zeit", jetzt).put("aktiv", jetzt)
                // Arbeitet die Sitzung? Nicht nach dem Zugende (info), am Ende, solange sie auf Frank wartet und nach einer
                // Leerlauf-Meldung einer Sitzung vom PC (Anmeldung in Jarvis, wartet auf Eingabe, beendet).
                .put("arbeitet", gueltig == "laeuft" && !leerlauf)
            // Seit wann ruht die Sitzung (Sekunden, Uhr des Hooks): Ein später eintreffender Puls, dessen Hook davor begann, weckt sie nicht.
            if (!eintrag.optBoolean("arbeitet")) eintrag.put("ruheSeit", if (hookBeginn > 0) hookBeginn else jetzt / 1000)
            if (frage != null) {
                // Eingang: Reihenfolge der offenen Rückfragen über alle Sessions (die älteste steht vorn).
                if (!frage.has("eingang")) frage.put("eingang", jetzt)
                eintrag.put("offeneFrage", frage)
                // Alle Rückfragen des Auftrags: Antwort und Ausgang finden ihr Gespräch auch, wenn längst eine neuere offen ist.
                val ids = eintrag.optJSONArray("frageIds") ?: JSONArray().also { eintrag.put("frageIds", it) }
                ids.put(frage.optString("frage_id"))
                while (ids.length() > 30) ids.remove(0)
            }
            // Zugende oder fertig: Was die Sitzung bis dahin an Nachträgen bekommen hatte, hat sie in diesem Zug bearbeitet.
            if (gueltig == "fertig" || gueltig == "info") eintrag.optJSONArray("nachtraege")?.let { l ->
                for (i in 0 until l.length()) l.optJSONObject(i)?.takeIf { it.optString("stand") == "zugestellt" }?.put("stand", "bearbeitet")?.put("bearbeitet", jetzt)
            }
            if (link.startsWith("https://") && eintrag.optString("link").isEmpty()) eintrag.put("link", link)
            val schritte = eintrag.optJSONArray("schritte") ?: JSONArray().also { eintrag.put("schritte", it) }
            schritte.put(JSONObject().put("zeit", jetzt).put("stand", gueltig).put("text", ProgrammierAbschluss.kuerzen(schritt, ProgrammierAbschluss.MAX_MELDUNG)))
            while (schritte.length() > 30) schritte.remove(0)
            schreib(context, liste)
            return eintrag.put("schritt", schritt)
        }

        /** Angaben einer am PC gestarteten Sitzung: lokal, ob sie die Jarvis-Hooks hat, Rechner, Link und Auftrag (die beiden nur, wenn noch leer). */
        @Synchronized
        fun lokalMerken(context: Context, id: String, info: JSONObject) {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return
            val hook = info.optBoolean("hook")
            val link = info.optString("link").takeIf { it.startsWith("https://") && eintrag.optString("link").isEmpty() }
            val auftrag = info.optString("auftrag").takeIf { it.isNotBlank() && eintrag.optString("auftrag").isBlank() }
            if (eintrag.optBoolean("lokal") && eintrag.optBoolean("lokalHook") == hook && link == null && auftrag == null) return
            eintrag.put("lokal", true).put("lokalHook", hook).put("rechner", info.optString("rechner"))
            link?.let { eintrag.put("link", it) }
            auftrag?.let { eintrag.put("auftrag", it.take(1_500)) }
            schreib(context, liste)
        }

        fun lokaleGespraeche(context: Context): Set<String> =
            alle(context).filter { it.optBoolean("lokal") }.map { it.optString("gespraech") }.filter { it.isNotEmpty() }.toSet()

        /** Nimmt einen Eintrag ganz aus dem Auftragsbuch (eine in Jarvis entfernte Sitzung vom PC). */
        @Synchronized
        fun vergiss(context: Context, id: String) {
            val liste = lies(context)
            schreib(context, JSONArray().apply {
                for (i in 0 until liste.length()) liste.optJSONObject(i)?.takeUnless { it.optString("id").equals(id, ignoreCase = true) }?.let { put(it) }
            })
        }

        /** Wartet ein Nachtrag noch auf die Sitzung (angenommen oder zugestellt, nicht bearbeitet)? Dann arbeitet sie gleich weiter. */
        fun nachtragOffen(eintrag: JSONObject): Boolean {
            val l = eintrag.optJSONArray("nachtraege") ?: return false
            return (0 until l.length()).any { l.optJSONObject(it)?.optString("stand") in listOf("angenommen", "zugestellt") }
        }

        /** Merkt die Ansage dauerhaft vor (überlebt einen Neustart); false = für diesen Abschluss schon geschehen. */
        @Synchronized
        fun ansageVormerken(context: Context, id: String, schluessel: String): Boolean {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return false
            if (eintrag.optString("sprachAngesagt") == schluessel) return false
            eintrag.put("sprachAngesagt", schluessel)
            schreib(context, liste)
            return true
        }

        fun laufendesGespraech(context: Context): String? {
            val liste = lies(context)
            val grenze = System.currentTimeMillis() - 2 * 3_600_000L
            for (i in liste.length() - 1 downTo 0) {
                val e = liste.getJSONObject(i)
                // Sitzungen vom PC nicht: Sie laufen neben Jarvis her, und ein Neustart von Jarvis soll nicht dorthin springen.
                if (e.optString("gespraech").isNotEmpty() && offen(e) && !e.optBoolean("lokal") && e.optLong("zeit") >= grenze) return e.optString("gespraech")
            }
            return null
        }

        /**
         * Lebenszeichen der Sitzung (Puls vom Hook): Sie arbeitet. Nicht, wenn sie auf Frank wartet (frage) oder der Puls von
         * einem Hook stammt, der vor dem Zugende begann ([hookBeginn] ≤ ruheSeit): Der asynchrone Puls kommt oft erst nach dem
         * Zugende an und ließ die Sitzung sonst dauerhaft als arbeitend stehen (oranger Punkt ohne laufende Sitzung).
         */
        @Synchronized
        fun puls(context: Context, id: String, hookBeginn: Long = 0L): JSONObject? {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return null
            if (eintrag.optString("stand") == "frage" || (hookBeginn > 0 && hookBeginn <= eintrag.optLong("ruheSeit"))) return null
            eintrag.put("aktiv", System.currentTimeMillis()).put("arbeitet", eintrag.optString("stand") !in GESPERRT)
            schreib(context, liste)
            return eintrag
        }

        fun jeGespraech(context: Context): Map<String, String> {
            val liste = lies(context)
            return (0 until liste.length()).map { liste.getJSONObject(it) }.filter { it.optString("gespraech").isNotEmpty() }
                .groupBy { it.optString("gespraech") }
                .mapValues { (_, l) -> l.joinToString(" ") { it.optString("app") + " " + it.optString("ziel") + " " + it.optString("schritt") } }
        }

        /**
         * Je Gespräch: Ist einer seiner Aufträge noch offen (läuft oder wartet auf Franks Antwort), zählt der offene mit seinem
         * letzten Lebenszeichen, sonst der jüngste. Der Inhalt sammelt App, Auftragstext und die letzten Schritte aller Aufträge.
         */
        fun lagen(context: Context): Map<String, AuftragsLage> {
            val liste = lies(context)
            return (0 until liste.length()).map { liste.getJSONObject(it) }.filter { it.optString("gespraech").isNotEmpty() }
                .groupBy { it.optString("gespraech") }
                .mapValues { (_, l) ->
                    val massgeblich = l.lastOrNull { offen(it) } ?: l.last()
                    val inhalt = l.joinToString(" ") { e ->
                        val schritte = e.optJSONArray("schritte")
                        val letzte = if (schritte == null) "" else (maxOf(0, schritte.length() - 5) until schritte.length()).joinToString(" ") { schritte.getJSONObject(it).optString("text").take(300) }
                        e.optString("app") + " " + e.optString("auftrag") + " " + e.optString("schritt").take(300) + " " + letzte
                    }
                    AuftragsLage(
                        app = massgeblich.optString("app").substringBefore(" (").trim(),
                        stand = massgeblich.optString("stand"),
                        aktiv = maxOf(massgeblich.optLong("aktiv"), massgeblich.optLong("zeit")),
                        inhalt = inhalt,
                    )
                }
        }

        /** Aufträge der letzten zwei Stunden mit Gespräch. */
        fun juengste(context: Context): List<JSONObject> {
            val liste = lies(context)
            val grenze = System.currentTimeMillis() - 2 * 3_600_000L
            return (0 until liste.length()).map { liste.getJSONObject(it) }.filter { it.optString("gespraech").isNotEmpty() && it.optLong("zeit") >= grenze }
        }

        /** Der Auftrag, zu dem diese Rückfrage gehört (auch nach ihrem Ausgang), oder null. */
        fun zuFrage(context: Context, frageId: String): JSONObject? = if (frageId.isEmpty()) null else alle(context).lastOrNull { e ->
            e.optJSONObject("offeneFrage")?.optString("frage_id") == frageId ||
                e.optJSONArray("frageIds")?.let { l -> (0 until l.length()).any { l.optString(it) == frageId } } == true
        }

        /** Gespräch und frage_id aller Rückfragen, auf die eine Sitzung gerade wartet, die älteste zuerst. */
        fun offeneRueckfragen(context: Context): List<Pair<String, String>> = alle(context)
            .filter { it.optString("stand") == "frage" && it.optString("gespraech").isNotEmpty() && it.optJSONObject("offeneFrage")?.optString("frage_id").orEmpty().isNotEmpty() }
            .sortedBy { it.optJSONObject("offeneFrage")!!.optLong("eingang", it.optLong("zeit")) }
            .map { it.optString("gespraech") to it.optJSONObject("offeneFrage")!!.optString("frage_id") }

        /** Kennt Jarvis diese Rückfrage schon (gezeigt oder erledigt)? */
        fun kenntFrage(context: Context, frageId: String): Boolean = zuFrage(context, frageId) != null

        /** Die offene Rückfrage mit dieser frage_id und ihr Auftrag, oder null. */
        @Synchronized
        fun offeneFrage(context: Context, frageId: String): Pair<JSONObject, JSONObject>? {
            val liste = lies(context)
            for (i in liste.length() - 1 downTo 0) {
                val e = liste.getJSONObject(i)
                val frage = e.optJSONObject("offeneFrage") ?: continue
                if (frage.optString("frage_id") == frageId) return e to frage
            }
            return null
        }

        @Synchronized
        fun frageErledigt(context: Context, frageId: String) {
            val liste = lies(context)
            for (i in 0 until liste.length()) {
                val e = liste.getJSONObject(i)
                if (e.optJSONObject("offeneFrage")?.optString("frage_id") == frageId) {
                    e.remove("offeneFrage")
                    if (e.optString("stand") == "frage") e.put("stand", "laeuft")
                }
            }
            schreib(context, liste)
        }

        /** Der Stand eines Auftrags in Worten, für alle Übersichten gleich. */
        fun standText(stand: String) = when (stand) {
            "frage" -> "Rückfrage, wartet auf Franks Antwort"; "fertig" -> "fertig"; "fehler" -> "fehlgeschlagen"
            STAND_ANMELDUNG -> "BLOCKIERT, Anmeldung erforderlich: Claude Code auf dem Rechner öffnen und /login ausführen, danach arbeitet die Sitzung von selbst weiter"
            STAND_STOERUNG -> "angehalten nach einem API-Fehler (etwa Nutzungslimit), arbeitet erst weiter, wenn die Sitzung wieder antwortet"
            STAND_UNKLAR -> "Start noch unbestätigt (der PC hat nicht rechtzeitig bestätigt; läuft vermutlich, nicht neu starten, erst abwarten, ob sich die Sitzung meldet)"
            else -> "läuft"
        }

        private fun alle(context: Context): List<JSONObject> = lies(context).let { l -> (0 until l.length()).map { l.getJSONObject(it) } }

        private fun offen(e: JSONObject) = e.optString("stand") in listOf("laeuft", "frage") + GESPERRT

        private fun zuletzt(e: JSONObject) = maxOf(e.optLong("zeit"), e.optLong("aktiv"))

        /** Ist unter den [anzahl] neuesten Aufträgen einer am PC noch offen? */
        fun offenAmPc(context: Context, anzahl: Int): Boolean = alle(context).takeLast(anzahl).any { offen(it) && it.optString("ziel") == ZIEL_PC }

        fun finde(context: Context, id: String): JSONObject? = alle(context).lastOrNull { it.optString("id").equals(id, ignoreCase = true) }

        /** Fertig gemeldete Aufträge an eine App seit [seit]: Auftrag und Abschlussmeldung, je eine Zeile (für die Funktionsübersicht). */
        fun fertige(context: Context, app: String, seit: Long): List<String> = alle(context)
            .filter { it.optString("stand") == "fertig" && de.frank.jarvis.gedaechtnis.Funktionsdatei.appPasst(it.optString("app"), app) && zuletzt(it) >= seit }
            .map { "- [${it.optString("id")}] Auftrag: " + it.optString("auftrag").replace('\n', ' ').take(300) + " → gemeldet: " + it.optString("schritt").replace('\n', ' ').take(300) }

        /** Alle Aufträge eines Jarvis-Gesprächs, neueste zuletzt, offene wie beendete: Frank bezeichnet mit „dieser Session“ das ganze Gespräch. */
        fun alleImGespraech(context: Context, gespraech: String): List<JSONObject> = alle(context).filter { it.optString("gespraech") == gespraech }

        /** Aufträge, die aus [gespraech] gestartet wurden und ein eigenes Gespräch haben, älteste zuerst („in dieser neuen Session“). */
        fun gestartetAus(context: Context, gespraech: String): List<JSONObject> =
            alle(context).filter { it.optString("ausGespraech") == gespraech && it.optString("gespraech") != gespraech }.sortedBy { it.optLong("start") }

        /** Merkt am Auftrag [id], aus welchem Gespräch er gestartet wurde (nur, wenn er ein eigenes bekam). */
        @Synchronized
        fun herkunftMerken(context: Context, id: String, gespraech: String) {
            if (gespraech.isEmpty()) return
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return
            if (eintrag.optString("gespraech") == gespraech) return
            eintrag.put("ausGespraech", gespraech)
            schreib(context, liste)
        }

        /** Alle Auftragsnummern im Auftragsbuch: Nennt Frank eine davon, bezeichnet er genau diese Sitzung. */
        fun alleIds(context: Context): List<String> = alle(context).map { it.optString("id") }.filter { it.isNotEmpty() }

        /** Eine Zeile je Auftrag für Auswahl und Rückfragen: Nummer, App, Ziel, Stand, Zeit, Kurzfassung. */
        fun kurzZeile(e: JSONObject): String =
            "[${e.optString("id")}] ${e.optString("app")} · ${e.optString("ziel")} · ${standText(e.optString("stand"))} · gestartet " +
                ZEIT.format(Instant.ofEpochMilli(e.optLong("start")).atZone(ZoneId.systemDefault())) + " · Auftrag: " +
                e.optString("auftrag").replace('\n', ' ').take(160)

        /** Die Aufträge der letzten 24 Stunden als Auswahl (für Fehlermeldungen ohne eindeutiges Ziel). */
        fun kandidatenText(context: Context, ohne: String): String {
            val jetzt = System.currentTimeMillis()
            val liste = alle(context).filter { it.optString("id") != ohne && (offen(it) || jetzt - zuletzt(it) < 24 * 3_600_000L) }.reversed().take(8)
            return if (liste.isEmpty()) "Es gibt keine Programmieraufträge der letzten 24 Stunden." else "Infrage kommen:\n" + liste.joinToString("\n") { "- " + kurzZeile(it) }
        }

        /** Eine offene Rückfrage in Worten: Fragen und ihre Optionen (labels). */
        fun frageText(frage: JSONObject): String {
            val fragen = frage.optJSONArray("fragen") ?: return ""
            return (0 until fragen.length()).mapNotNull { fragen.optJSONObject(it) }.joinToString(" / ") { f ->
                val optionen = f.optJSONArray("options")?.let { o -> (0 until o.length()).mapNotNull { o.optJSONObject(it)?.optString("label")?.takeIf { l -> l.isNotEmpty() } } }.orEmpty()
                f.optString("question").replace('\n', ' ').take(600) + (if (optionen.isEmpty()) " (freie Antwort)" else " — Optionen: " + optionen.joinToString(" | "))
            }
        }

        /**
         * Legt einen Nachtrag an (Stand angenommen). Derselbe Text an denselben Auftrag binnen 30 Minuten ist kein neuer:
         * dann der vorhandene und true. null = Auftrag unbekannt.
         */
        @Synchronized
        fun nachtragNeu(context: Context, id: String, text: String, quelle: String, nachricht: String? = null): Pair<JSONObject, Boolean>? {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return null
            val nachtraege = eintrag.optJSONArray("nachtraege") ?: JSONArray().also { eintrag.put("nachtraege", it) }
            val jetzt = System.currentTimeMillis()
            // Gesprächsnachrichten ([nachricht], eigene Nummer) dürfen sich wiederholen: „Wie ist der Stand?“ zweimal ist zweimal gefragt.
            if (nachricht == null) for (i in nachtraege.length() - 1 downTo 0) {
                val n = nachtraege.getJSONObject(i)
                if (NachtragsRegel.doppelt(n.optString("text"), n.optLong("zeit"), text, jetzt)) return n to true
            }
            val n = JSONObject().put("id", nachricht ?: ("N" + neueNummer())).put("text", text.take(4_000)).put("zeit", jetzt).put("quelle", quelle).put("stand", "angenommen")
            nachtraege.put(n)
            while (nachtraege.length() > 20) nachtraege.remove(0)
            schreib(context, liste)
            return n to false
        }

        /** Antwort des Programmierers auf die Gesprächsnachricht [nachricht] von Jarvis: Stand beantwortet, Antwort am Nachtrag. */
        @Synchronized
        fun nachrichtBeantwortet(context: Context, id: String, nachricht: String, antwort: String): Boolean {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return false
            val n = eintrag.optJSONArray("nachtraege")?.let { l -> (0 until l.length()).map { l.getJSONObject(it) } }
                ?.lastOrNull { it.optString("id").equals(nachricht, ignoreCase = true) } ?: return false
            // Ein Wunsch ist mit der Bestätigung nicht erledigt: Er bleibt zugestellt und zählt beim nächsten fertig als bearbeitet
            // (eigene Abschluss-Ansage); eine Frage ist mit der Antwort beantwortet.
            if (!n.optString("text").startsWith(WUNSCH_KOPF)) n.put("stand", "beantwortet")
            n.put("beantwortet", System.currentTimeMillis()).put("antwort", antwort.take(2_000))
            schreib(context, liste)
            return true
        }

        /** Die Gesprächsnachricht [nachricht] am Auftrag [e] (null = keine, etwa eine fremde Nummer). */
        fun nachricht(e: JSONObject?, nachricht: String): JSONObject? = e?.optJSONArray("nachtraege")
            ?.let { l -> (0 until l.length()).map { l.getJSONObject(it) } }?.lastOrNull { istNachricht(it.optString("id")) && it.optString("id").equals(nachricht, ignoreCase = true) }

        /** Die jüngste Gesprächsnachricht von Jarvis an Auftrag [e] (dauerhaft, auch nach einem Neustart). */
        fun letzteNachricht(e: JSONObject?): JSONObject? = e?.optJSONArray("nachtraege")
            ?.let { l -> (0 until l.length()).map { l.getJSONObject(it) } }?.lastOrNull { istNachricht(it.optString("id")) }

        /** Setzt Nachträge ([ids], null = alle) von einem der Stände [von] auf [nach]; liefert die geänderten. */
        @Synchronized
        fun setzeNachtraege(context: Context, id: String, ids: Set<String>?, von: Set<String>, nach: String): List<JSONObject> {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return emptyList()
            val nachtraege = eintrag.optJSONArray("nachtraege") ?: return emptyList()
            val geaendert = (0 until nachtraege.length()).map { nachtraege.getJSONObject(it) }
                .filter { (ids == null || it.optString("id") in ids) && it.optString("stand") in von }
            if (geaendert.isEmpty()) return emptyList()
            val jetzt = System.currentTimeMillis()
            geaendert.forEach { it.put("stand", nach).put(nach, jetzt) }
            schreib(context, liste)
            return geaendert
        }

        /** Für vorgaenge: Aufträge mit Lebenszeichen der letzten 24 Stunden (offene und beendete), neueste zuerst, je eine Zeile. */
        fun vorgangsZeilen(context: Context, alle: Boolean): List<String> {
            val liste = lies(context)
            val jetzt = System.currentTimeMillis()
            // Auch offene nur mit Lebenszeichen der letzten Stunden: Eine Sitzung, die nie „fertig“ meldete, steht sonst ewig als laufend da.
            // Kurz (Startkontext): sechs Stunden, höchstens sechs; alle: 24 Stunden.
            val fenster = (if (alle) 24 else 6) * 3_600_000L
            return (liste.length() - 1 downTo 0).map { liste.getJSONObject(it) }
                .filter { jetzt - maxOf(it.optLong("zeit"), it.optLong("aktiv")) < fenster }
                .take(if (alle) 20 else 6)
                .map { e ->
                    val still = e.optString("stand") == "laeuft" && jetzt - maxOf(e.optLong("zeit"), e.optLong("aktiv")) >= 30 * 60_000L
                    "[" + e.optString("id") + "] Programmierauftrag " + e.optString("app") + ": " + standText(e.optString("stand")) +
                        (if (still) " (VERALTET: seit über einer halben Stunde weder Fortschritt noch Lebenszeichen; ob die Sitzung noch lebt, zeigt programmierauftraege)" else "") + " · zuletzt " +
                        ZEIT.format(Instant.ofEpochMilli(e.optLong("zeit")).atZone(ZoneId.systemDefault())) + ": " + e.optString("schritt").replace('\n', ' ').take(200) +
                        (e.optJSONObject("offeneFrage")?.optString("frage_id")?.takeIf { it.isNotEmpty() }?.let { " · frage_id $it (Antwort mit programmier_antwort)" } ?: "") +
                        (e.optJSONArray("nachtraege")?.let { l -> (0 until l.length()).count { l.optJSONObject(it)?.optString("stand") in listOf("angenommen", "zugestellt", "fehler") } }
                            ?.takeIf { it > 0 }?.let { " · $it Nachtrag/Nachträge noch nicht bearbeitet" } ?: "")
                }
        }

        /**
         * Für einen laufenden PC-Auftrag: letzter echter Fortschritt (zeit), letztes Lebenszeichen (aktiv) und, frisch vom Server
         * ([sitzungen]), ob die Sitzung noch lebt (ihr Warter fragt alle 25 s ab). Schweigen allein heißt weder Stillstand noch
         * abgelaufene Anmeldung: Eine lebende Sitzung kann lange an einem Schritt arbeiten.
         */
        private fun lebenszeichen(e: JSONObject, sitzungen: JSONObject?, jetzt: Long): String {
            if (e.optString("stand") !in listOf("laeuft", "frage") + GESPERRT) return ""
            val fortschritt = (jetzt - e.optLong("zeit")) / 60_000
            val puls = (jetzt - maxOf(e.optLong("zeit"), e.optLong("aktiv"))) / 60_000
            val teile = mutableListOf("letzter Fortschritt vor $fortschritt min", "letztes Lebenszeichen vor $puls min")
            val s = sitzungen?.optJSONObject(e.optString("id").uppercase(Locale.ROOT))
            if (e.optString("ziel") == ZIEL_PC) teile += when {
                sitzungen == null -> "ob die Sitzung lebt, ist gerade nicht prüfbar (Jarvis-Server nicht erreichbar)"
                s == null -> "der Jarvis-Server kennt seit seinem letzten Neustart kein Lebenszeichen dieser Sitzung (vermutlich beendet)"
                s.optBoolean("lebt") || (!s.isNull("kontakt_vor_s") && s.optInt("kontakt_vor_s") < 120) -> "die Sitzung lebt (frisch vom Server geprüft)"
                else -> "die Sitzung meldet sich seit " + (if (s.isNull("warter_vor_s")) "dem Serverstart" else "${s.optInt("warter_vor_s") / 60} min") +
                    " nicht mehr beim Server: beendet, Rechner im Ruhezustand oder aus"
            }
            val veraltet = e.optString("stand") == "laeuft" && fortschritt >= 30
            return (if (veraltet) "\n  VERALTET: seit $fortschritt min keine Fortschrittsmeldung; " else "\n  Lebenszeichen: ") + teile.joinToString(", ") + "."
        }

        fun uebersicht(context: Context, anzahl: Int, sitzungen: JSONObject? = null): String {
            val liste = lies(context)
            if (liste.length() == 0) return "Es gibt noch keine Programmieraufträge."
            val jetzt = System.currentTimeMillis()
            return buildString {
                append("Programmieraufträge, neueste zuerst:\n")
                (liste.length() - 1 downTo maxOf(0, liste.length() - anzahl)).forEach { i ->
                    val e = liste.getJSONObject(i)
                    val standText = standText(e.optString("stand"))
                    val minuten = (jetzt - e.optLong("zeit")) / 60_000
                    append("- [auftrag_id ").append(e.optString("id")).append("] ").append(e.optString("app")).append(" (").append(e.optString("ziel"))
                        .append(", gestartet ").append(ZEIT.format(Instant.ofEpochMilli(e.optLong("start")).atZone(ZoneId.systemDefault())))
                        .append(if (e.optString("gespraech").isNotEmpty()) ", eigene Jarvis-Session" else "").append("): ")
                        .append(standText).append(". Zuletzt vor ").append(minuten).append(" Minuten: ").append(e.optString("schritt").take(600))
                    e.optString("link").takeIf { it.isNotEmpty() }?.let { append(" Link: ").append(it) }
                    append(lebenszeichen(e, sitzungen, jetzt))
                    e.optString("auftrag").takeIf { it.isNotEmpty() }?.let { append("\n  Ursprünglicher Auftrag (Kurzfassung): ").append(it.replace('\n', ' ').take(500)) }
                    e.optJSONObject("offeneFrage")?.let { f ->
                        append("\n  OFFENE RÜCKFRAGE (frage_id ").append(f.optString("frage_id")).append(", Antwort mit programmier_antwort): ").append(frageText(f))
                    }
                    e.optJSONArray("nachtraege")?.takeIf { it.length() > 0 }?.let { l ->
                        append("\n  Nachträge: ").append((0 until l.length()).mapNotNull { l.optJSONObject(it) }.joinToString("; ") { n ->
                            n.optString("id") + " " + n.optString("stand") + " („" + n.optString("text").replace('\n', ' ').take(80) + "“)"
                        })
                    }
                    append("\n  Nachträge möglich: ").append(
                        if (inDerCloud(e.optString("ziel")) && e.optString("stand") in listOf("fertig", "fehler")) "nein, die Cloud-Sitzung ist beendet"
                        else "ja, aber nur auf Franks ausdrücklichen Wunsch (programmier_nachtrag)")
                    append('\n')
                }
            }.trim()
        }
    }

    /** Ergebnis eines Starts: Link zur laufenden Sitzung (leer, wenn das Ziel keinen liefert) und ein Satz für Frank. */
    data class Start(val link: String, val hinweis: String)

    /** Das Ziel ist gerade nicht erreichbar; es wurde nichts gestartet. */
    class NichtDa(grund: String) : Exception(grund)

    /** Der PC hat den Auftrag abgeholt, den Start aber noch nicht bestätigt: läuft vermutlich, nicht neu starten. */
    class Unbestaetigt(val rechner: String) : Exception("Start auf $rechner noch unbestätigt")

    /** Wohin ein Programmierauftrag gehen kann. */
    interface ProgrammierZiel {
        val name: String
        fun bereit(): Boolean
        /** Ein Satz, was fehlt, solange das Ziel nicht bereit ist. */
        fun fehlt(): String
        /** [modell]: [MODELL_OPUS] oder [MODELL_SOL]; die Cloud kennt nur Opus. */
        suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String, modell: String = MODELL_OPUS): Start
    }

    /**
     * Franks PC über den Jarvis-Relay: Die PC-Brücke holt den Auftrag ab und öffnet eine Claude-Code-Sitzung mit Remote
     * Control (in der Claude-App unter Code zu sehen). 409 vom Relay = Brücke fragt gerade nicht ab, der PC ist aus.
     */
    private inner class PcBruecke : ProgrammierZiel {
        override val name = ZIEL_PC
        override fun bereit() = e.serverToken.isNotBlank()
        override fun fehlt() = "Jarvis ist nicht mit seinem Server verbunden."

        override suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String, modell: String): Start = withContext(Dispatchers.IO) {
            val sol = modell == MODELL_SOL
            // Womit Sol läuft (Codex CLI oder OpenCode CLI); steht im Kontext von starteErstes.
            val openCode = sol && kotlinx.coroutines.currentCoroutineContext()[CliWahl]?.cli == CLI_OPENCODE
            val rumpf = JSONObject().put("app", app).put("auftrag", auftrag.take(HOECHSTENS)).put("review", review).put("nummer", nummer).put("modell", modell).put("vorrang", vorrang())
                .put("cli", if (openCode) CLI_OPENCODE else CLI_CODEX)
            // Frei gewähltes Modell: genau Franks geprüfte Wahl (nur der Mac startet sie, siehe Relay).
            val frei = if (modell == MODELL_KATALOG) kotlinx.coroutines.currentCoroutineContext()[KatalogStart]?.wahl else null
            if (modell == MODELL_KATALOG && frei == null) throw IllegalStateException("Die Modellwahl fehlt.")
            frei?.let { w ->
                rumpf.put("slug", w.modell.slug).put("anbieter", w.modell.anbieter).put("name", w.modell.name).put("effort", w.effort)
                    .put("profil", w.profil).put("modus", w.modus).put("cli", w.cli)
            }
            val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/pc-auftrag").header("X-Jarvis-Token", e.serverToken)
                .post(rumpf.toString().toRequestBody("application/json".toMediaType())).build()
            val (code, roh) = try {
                HTTP.newCall(anfrage).execute().use { it.code to it.body?.string().orEmpty() }
            } catch (fehler: java.net.UnknownHostException) {
                // Kein Netz zum Server: Der Auftrag kam nirgends an, also darf die Cloud übernehmen.
                throw NichtDa("Der Jarvis-Server war nicht erreichbar, deshalb die Cloud.")
            } catch (fehler: java.net.ConnectException) {
                throw NichtDa("Der Jarvis-Server war nicht erreichbar, deshalb die Cloud.")
            }
            val antwort = runCatching { JSONObject(roh) }.getOrDefault(JSONObject())
            // Abgeholt, aber die Startbestätigung kam nicht in der Frist des Relays (Brücke langsam, am 24.10.2026 51 s): läuft
            // vermutlich. Nie „nicht gestartet“ und nie die Cloud; die späte Quittung reicht der Relay als Meldung nach.
            if (code in 200..299 && antwort.optBoolean("unbestaetigt")) throw Unbestaetigt(antwort.optString("rechner").ifEmpty { "dem PC" })
            when {
                // Frei gewählte Modelle startet nur der Mac: kein Ersatz am Windows-PC und keiner in der Cloud.
                frei != null && code == 409 -> throw IllegalStateException("Der Mac fragt gerade nicht ab; ein frei gewähltes Modell (${frei.modell.name}) startet nur dort. " +
                    "Bitte den Mac und die Jarvis-Brücke prüfen, oder ohne Modellangabe mit Opus 5.5 bzw. Sol 6.1 starten.")
                frei != null && code == 504 -> throw IllegalStateException("Der Mac hat den Auftrag nicht abgeholt, er ist nicht gestartet. Bitte am Mac nachsehen.")
                frei != null && code in 200..299 && !antwort.optBoolean("ok") -> throw IllegalStateException(antwort.optString("text").take(300).ifEmpty { "Der Mac konnte die Sitzung nicht starten." })
                // OpenCode gibt es nur am Windows-PC: kein Ersatz in Codex Cloud.
                openCode && code == 409 -> throw IllegalStateException("Der Windows-PC fragt gerade nicht ab; OpenCode CLI läuft nur dort. Mit Codex CLI ginge Sol 6.1 in Codex Cloud.")
                // Sol nie still als Opus in die Claude-Code-Routine: Ist der Windows-PC aus, übernimmt Codex Cloud (nächstes Ziel).
                sol && code == 409 -> throw NichtDa("Der Windows-PC fragt gerade nicht ab, deshalb Sol 6.1 in Codex Cloud.")
                // 504 heißt seit Relay-Stand 24.10.2026: nicht abgeholt, also sicher nicht gestartet (und startet auch nicht mehr).
                sol && code == 504 -> throw IllegalStateException("Der Windows-PC hat den Sol-Auftrag nicht abgeholt, er ist nicht gestartet. Bitte am PC nachsehen; mit ziel=cloud ginge Sol in Codex Cloud.")
                sol && code in 200..299 && !antwort.optBoolean("ok") -> throw IllegalStateException(antwort.optString("text").take(300).ifEmpty { "Der Rechner konnte die Sol-Sitzung nicht starten." })
                code == 409 -> throw NichtDa("Der PC ist nicht an, deshalb die Cloud.")
                code == 504 -> throw IllegalStateException("Der PC hat den Auftrag nicht abgeholt, er ist nicht gestartet. Bitte am PC nachsehen; mit ziel=cloud geht der Auftrag in die Cloud.")
                code !in 200..299 -> throw IllegalStateException("HTTP $code vom Jarvis-Server")
                // Nicht angemeldet: kein Start und bewusst keine Cloud (Frank soll sich anmelden, nicht unbemerkt woanders landen).
                antwort.optBoolean("anmeldung_fehlt") -> {
                    val text = antwort.optString("text").take(300).ifEmpty { "Claude Code auf dem Rechner ist nicht angemeldet. Claude Code dort öffnen und /login ausführen." }
                    benachrichtige(JSONObject().put("id", nummer).put("app", app).put("stand", STAND_ANMELDUNG).put("ziel", ZIEL_PC).put("schritt", "🔐 Nicht gestartet: $text"))
                    throw IllegalStateException("Nicht angemeldet: $text Der Auftrag ist NICHT gestartet und geht ohne Franks ausdrücklichen Wunsch auch nicht in die Cloud (mit ziel=cloud ginge er dorthin).")
                }
                // Sicher nicht gestartet (die Brücke hat es gemeldet): Dann übernimmt die Cloud, ohne doppelte Sitzung.
                !antwort.optBoolean("ok") -> throw NichtDa("Der PC konnte die Sitzung nicht starten (${antwort.optString("text").take(160)}), deshalb die Cloud.")
            }
            val rechner = antwort.optString("rechner").ifEmpty { "dem PC" }
            if (frei != null) Start("", "Die Sitzung „Jarvis: $app (${frei.modell.name}${if (frei.cli == CLI_CLAUDE) "" else ", ${frei.cli}"})“ läuft auf $rechner: " +
                "${frei.beschreibung()}. " + when (frei.cli) {
                    CLI_CLAUDE -> "Sie ist in der Claude-App unter Code zu sehen; Schritte und Rückfragen laufen über Jarvis."
                    CLI_OPENCODE -> "Schritte, Rückfragen und Nachträge laufen wie bei Claude Code über Jarvis."
                    else -> "Sie meldet ihre Schritte selbst; Rückfragen und Nachträge gehen dort nicht."
                })
            else if (openCode) Start("", "Die Sitzung „Jarvis: $app (Sol 6.1, OpenCode)“ läuft auf $rechner im OpenCode CLI mit Sol 6.1 (Minimalprofil, Schnellmodus, Effort High). " +
                "Schritte, Rückfragen und Nachträge laufen wie bei Claude Code über Jarvis; meist kommt die App danach per WLAN aufs Handy.")
            else if (sol) Start("", "Die Sitzung „Jarvis: $app (Sol 6.1)“ läuft auf $rechner im Codex CLI mit Sol 6.1 (Minimalprofil, Schnellmodus, Effort High). " +
                "Sie meldet ihre Schritte selbst; Rückfragen und Nachträge gehen dort nicht.")
            else Start("", "Die Sitzung „Jarvis: $app“ läuft auf $rechner und ist in der Claude-App unter Code zu sehen. " +
                "Meist dauert es wenige Minuten, dann wird die App per WLAN installiert.")
        }
    }

    /**
     * Claude-Code-Routine mit API-Trigger (Doku: platform.claude.com/docs/en/api/claude-code/routines-fire). Der Text
     * kommt in der Sitzung als `<routine-fire-payload>` an; der gespeicherte Prompt der Routine sagt, was damit zu tun ist.
     * Kein automatischer Neuversuch: Jeder erfolgreiche Aufruf legt eine neue Sitzung an.
     */
    private inner class CloudRoutine : ProgrammierZiel {
        override val name = ZIEL_CLOUD
        override fun bereit() = e.routineUrl.startsWith("https://") && e.routineToken.isNotBlank()
        override fun fehlt() = "Die Claude-Code-Routine ist in Jarvis nicht eingerichtet: Auslöse-Adresse und Token unter Einstellungen → Konten → Programmieren in der Cloud eintragen."

        override suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String, modell: String): Start = withContext(Dispatchers.IO) {
            val text = buildString {
                append("APP: ").append(app).append('\n')
                append("CODEX-REVIEW: ").append(if (review) "ja" else "nein").append('\n')
                append("AUFTRAG:\n").append(auftrag)
            }.take(HOECHSTENS)
            val anfrage = Request.Builder().url(e.routineUrl)
                .header("Authorization", "Bearer ${e.routineToken}")
                .header("anthropic-version", "2023-06-01")
                .header("anthropic-beta", "experimental-cc-routine-2026-04-01")
                .post(JSONObject().put("text", text).toString().toRequestBody("application/json".toMediaType()))
                .build()
            HTTP.newCall(anfrage).execute().use { antwort ->
                val roh = antwort.body?.string().orEmpty()
                if (!antwort.isSuccessful) throw IllegalStateException(when (antwort.code) {
                    401, 403 -> "Claude lehnt den Token ab (HTTP ${antwort.code}). Bitte auf claude.ai/code/routines einen neuen Token erzeugen und in Jarvis eintragen."
                    404 -> "Die Routine gibt es unter dieser Adresse nicht (HTTP 404)."
                    429 -> "Zu viele Aufträge in kurzer Zeit (HTTP 429). Bitte später noch einmal."
                    else -> "HTTP ${antwort.code}: ${roh.take(200)}"
                })
                // Die API liefert den Link mit der internen id (…/code/cse_…); Claude-App und Web öffnen die Sitzung nur als
                // …/code/session_… (gleiche Nummer, anderer Vorsatz).
                val link = runCatching { JSONObject(roh).optString("claude_code_session_url") }.getOrDefault("").replace("/code/cse_", "/code/session_")
                Start(link,
                    "Meist dauert es zehn bis zwanzig Minuten, dann meldet UpdateStation die neue Version.")
            }
        }
    }

    /**
     * Sol 6.1 in Codex Cloud über Franks ChatGPT-Konto, wie in der Codex-App: Die Umgebung kommt aus `environment/config/list`
     * (Name wie das Repo, sonst die einzige bereite), dann `thread/start` mit gpt-6.1-sol und `turn/start` mit dem Auftrag.
     * Der Zug läuft in der Cloud weiter, wenn Jarvis die Verbindung schließt. Kein automatischer Neuversuch (jeder Start ist ein Thread).
     */
    private inner class CodexCloud : ProgrammierZiel {
        private val auth by lazy { de.frank.jarvis.auth.CodexAuthManager(app) }
        override val name = ZIEL_CODEX
        override fun bereit() = auth.isConnected
        override fun fehlt() = "Sol 6.1 geht ohne Windows-PC nur in Codex Cloud, und dafür muss Jarvis mit dem ChatGPT-Konto verbunden sein (Einstellungen → Konten)."

        override suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String, modell: String): Start {
            val repo = e.repoName.substringAfterLast('/')
            val text = buildString {
                append("APP: ").append(app).append('\n')
                append("CODEX-REVIEW: nein\n")
                append("AUFTRAG:\n").append(auftrag)
            }.take(HOECHSTENS)
            val thread = auth.codexCloudRpc { rpc ->
                val liste = rpc("environment/config/list", JSONObject()).optJSONArray("data") ?: JSONArray()
                val umgebungen = (0 until liste.length()).mapNotNull { liste.optJSONObject(it) }
                val umgebung = (umgebungen.firstOrNull { it.optString("name").equals(repo, ignoreCase = true) }
                    ?: umgebungen.singleOrNull { it.optString("status") == "ready" })?.optString("id").orEmpty()
                if (umgebung.isEmpty()) throw IllegalStateException("In Codex Cloud gibt es keine Umgebung „$repo“. Frank legt sie in der Codex-App unter Cloud → Umgebung erstellen an.")
                val start = rpc("thread/start", JSONObject()
                    .put("model", SOL_CLOUD_MODELL)
                    .put("approvalPolicy", "never")
                    .put("serviceName", "codex_cloud")
                    .put("environments", JSONArray().put(JSONObject().put("environmentConfigId", umgebung)))
                    .put("deferredEnvironment", true)
                    .put("config", JSONObject().put("model_reasoning_effort", "high")))
                val id = start.optJSONObject("thread")?.optString("id").orEmpty()
                if (id.isEmpty()) throw IllegalStateException("Codex Cloud hat keinen Thread angelegt.")
                rpc("turn/start", JSONObject().put("threadId", id)
                    .put("input", JSONArray().put(JSONObject().put("type", "text").put("text", text))))
                runCatching { rpc("thread/name/set", JSONObject().put("threadId", id).put("name", "Jarvis: $app")) }
                id
            }
            return Start("https://chatgpt.com/local/$thread",
                "Sol 6.1 arbeitet jetzt in Codex Cloud (Thread „Jarvis: $app“ in der Codex-App unter Cloud) und meldet seine Schritte selbst; " +
                    "Rückfragen gehen dort nicht. Meist dauert es zehn bis zwanzig Minuten, dann meldet UpdateStation die neue Version.")
        }
    }

    companion object {
        /** Der ganze Text zur gekürzten Aufgabe [angezeigt] einer Rückfrage; leer, wenn er nicht mehr vorliegt. Liest Dateien: nicht im Hauptfaden. */
        fun rueckfrageVolltext(context: Context, auftragId: String, angezeigt: String): String {
            val eintrag = Auftragsbuch.finde(context, auftragId)
            val auftrag = AuftragsVolltext.lesen(context, auftragId) ?: eintrag?.optString("auftrag").orEmpty()
            return RueckfrageAufgabe.vollZu(eintrag, auftrag, angezeigt)
        }

        // Gespräch mit dem Programmierer (programmierer_sprechen): Nachrichten tragen die Nummer „G…“, damit Hook und Cloud-Antwort
        // sie von Nachträgen unterscheiden. Wartende Fragen und offene Nachrichten gelten über alle Instanzen (App und Plugin).
        const val ART_FRAGE = "frage"
        const val ART_WUNSCH = "wunsch"
        const val ART_WARTEN = "warten"
        private const val NACHRICHT = "G"
        /** So lange wartet eine Frage auf die Antwort; darunter bleibt das Plugin-Zeitlimit (120 s) und das Live-Gespräch hängt nicht ewig. */
        private const val GESPRAECH_WARTEN_MS = 45_000L
        private val gespraechsAntworten = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<String>>()
        internal fun istNachricht(nachtragId: String) = nachtragId.startsWith(NACHRICHT)
        private const val FRAGE_KOPF = "FRAGE VON JARVIS"
        private const val WUNSCH_KOPF = "WUNSCH VON FRANK über Jarvis"
        /** Was Frank von einem Nachtrag sieht: bei Gesprächsnachrichten nur der erste Absatz (ohne die Anleitung an die Sitzung). */
        private fun anzeige(n: JSONObject) = n.optString("text").let { if (istNachricht(n.optString("id"))) it.substringBefore("\n\n") else it }
        private val ANTWORT_VORSPANN = Regex("^(?:💬\\s*)?Antwort auf (G[A-Z0-9]{5})\\s*:", RegexOption.IGNORE_CASE)

        /** Gespräche, denen das Auftragsbuch einen Programmierauftrag zuordnet (für den Reiter „Programmierung“). */
        fun gespraecheMitAuftrag(context: Context): Map<String, Long> = Auftragsbuch.gespraechStarts(context.applicationContext)

        /** Offene Rückfragen aller Sessions (Gespräch, frage_id), die älteste zuerst: die Reihe der Vollbilder. */
        fun offeneRueckfragen(context: Context): List<Pair<String, String>> = Auftragsbuch.offeneRueckfragen(context.applicationContext)

        private val lokalSperre = Any()
        private const val LOKAL_DATEI = "programmieren"
        private const val LOKAL_ENTFERNT = "lokal_entfernt_liste"

        /** In Jarvis entfernte Sitzungen vom PC (Nummern, älteste zuerst): Ihre Meldungen legen nie wieder einen Eintrag an. */
        private fun entfernteLokale(context: Context): List<String> = runCatching {
            val l = JSONArray(context.getSharedPreferences(LOKAL_DATEI, Context.MODE_PRIVATE).getString(LOKAL_ENTFERNT, "[]"))
            (0 until l.length()).map { l.optString(it) }
        }.getOrDefault(emptyList())

        /** Gespräche, deren Sitzung am PC gestartet wurde (für Lila und Kennzeichen in der Leiste, auch nach dem Umbenennen). */
        fun lokaleGespraeche(context: Context): Set<String> = Auftragsbuch.lokaleGespraeche(context.applicationContext)

        /**
         * Frank löscht ein Gespräch (langes Drücken, „Löschen“): Gehört eine am PC direkt gestartete Sitzung dazu, vergisst Jarvis sie
         * dauerhaft, auch während sie noch läuft, und sagt es dem Relay; ihr Hook schweigt danach. Die Sitzung am Rechner läuft
         * unberührt weiter und bleibt dort offen. Über Jarvis gestartete Aufträge bleiben, wie sie sind.
         */
        fun lokaleEntfernen(context: Context, gespraech: String) {
            val app = context.applicationContext
            val ids = Auftragsbuch.alleImGespraech(app, gespraech).filter { it.optBoolean("lokal") }.map { it.optString("id").uppercase() }
            if (ids.isEmpty()) return
            // Unter derselben Sperre wie lokaleSitzung: Eine gleichzeitige Meldung legt die Sitzung sonst gleich wieder an.
            synchronized(lokalSperre) {
                val liste = (entfernteLokale(app).filter { it !in ids } + ids).takeLast(300)
                app.getSharedPreferences(LOKAL_DATEI, Context.MODE_PRIVATE).edit().putString(LOKAL_ENTFERNT, JSONArray(liste).toString()).commit()
                ids.forEach { Auftragsbuch.vergiss(app, it) }
            }
            Logbuch.system("Lokale Sitzung entfernt", ids.joinToString(", ") + " (nur in Jarvis, am PC läuft sie weiter)")
            ids.forEach { lokalEntfernenSenden(app, it) }
        }

        /** Sagt dem Relay, dass Frank die Sitzung [id] in Jarvis entfernt hat (im Hintergrund; ein Fehlschlag wird beim nächsten Lebenszeichen wiederholt). */
        internal fun lokalEntfernenSenden(context: Context, id: String) {
            val e = Einstellungen.get(context.applicationContext)
            if (e.serverHost.isBlank() || e.serverToken.isBlank()) return
            kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/lokal-entfernen").header("X-Jarvis-Token", e.serverToken)
                        .post(JSONObject().put("auftrag_id", id).toString().toRequestBody("application/json".toMediaType())).build()
                    HTTP.newCall(anfrage).execute().use { if (!it.isSuccessful) throw IllegalStateException("HTTP ${it.code}") }
                }.onFailure { Logbuch.system("Entfernen nicht beim Server", "$id: ${it.message ?: it.javaClass.simpleName}") }
            }
        }

        /** Fertig gemeldete Aufträge an eine App (nur Zusatzinfo: fertig gemeldet ist noch nicht installiert). */
        fun fertigeAuftraege(context: Context, app: String, seit: Long): List<String> = Auftragsbuch.fertige(context.applicationContext, app, seit)

        /**
         * Wie die Sitzung ihr Ende meldet: Jarvis liest diese Meldung nach einem im Sprachgespräch gestellten Auftrag kurz vor
         * ([ProgrammierAbschluss.vorlesetext]); die Statusangaben am Schluss liest es aus, nicht vor.
         */
        const val ABSCHLUSS_MELDUNG = "Die Fertig-Meldung: ein bis drei kurze, gut vorlesbare Sätze ohne Markdown, was jetzt neu ist, wie es funktioniert " +
            "und welche Version, danach genau „Committet: ja/nein · Gepusht: ja/nein · Installiert: ja (WLAN/Tunnel/Kabel)/nein · APK-Update: ja/nein“ " +
            "(fehlgeschlagen statt ja/nein, wenn es scheiterte; nur was wirklich gelaufen ist)."

        /**
         * Wie der Auftrag selbst aussieht (Beschreibung des Felds „auftrag“, für Jarvis' Modelle und ChatGPT über das Plugin).
         * Die „ ; “-Bündelung muss bei jeder künftigen Änderung erhalten bleiben (DiktatTrenner.auftrag).
         */
        const val AUFTRAGS_REGELN = "Der Auftrag, so kurz wie möglich, so vollständig wie nötig: Ziel und gewünschtes Ergebnis; Franks Beobachtungen, " +
            "Anforderungen und bestätigte Entscheidungen vollständig; woran man erkennt, dass es fertig ist. Will Frank erkennbar ein bestehendes Verhalten " +
            "unbedingt behalten, nenne es knapp und konkret als Schutzanforderung („Unverändert lassen: …“). Keine Lösungswege, Umsetzungsschritte oder " +
            "Dateipfade vorgeben, keine Wiederholungen, keine verworfenen Ideen. Vermutete Ursachen nur, wenn sie wirklich helfen, und als „Vermutung:“ markiert. " +
            "Mehrere Aufgaben durch „ ; “ getrennt (siehe Beschreibung)."

        /**
         * App-Feld eines Auftrags, in dem Frank keine App genannt hat. Nur Jarvis fragt dann nicht nach; der Programmierer bekommt
         * „APP: ohne App“ und klärt die App nach seinem Ablauf selbst, bei Bedarf per Rückfrage.
         */
        const val APP_OHNE = "ohne App"
        private fun fuer(app: String) = if (app == APP_OHNE) "" else " für $app"

        /** So lang darf der ganze Text an ein Programmier-Ziel höchstens werden. */
        const val HOECHSTENS = 60_000

        /** Die Namen der Ziele, so stehen sie im Auftragsbuch (Feld „ziel“). */
        const val ZIEL_PC = "Claude Code auf deinem PC"
        const val ZIEL_CLOUD = "Claude Code in der Cloud"
        const val ZIEL_CODEX = "Sol 6.1 in Codex Cloud"
        /** Modell der Codex-Cloud-Threads (wie Sol am Windows-PC). */
        private const val SOL_CLOUD_MODELL = "gpt-6.1-sol"

        /** Läuft der Auftrag in einer Cloud-Sitzung (Claude Code oder Codex)? Sie meldet selbst, Nachträge reisen mit der Antwort. */
        fun inDerCloud(ziel: String) = ziel == ZIEL_CLOUD || ziel == ZIEL_CODEX

        /** Modellwahl beim Start am Rechner: Claude Opus 5.5 (wie bisher) oder Sol 6.1 im Codex CLI. */
        const val MODELL_OPUS = "opus"
        const val MODELL_SOL = "sol"

        /** Womit Sol 6.1 am Windows-PC läuft (Franks Wahl per Knopf): Codex CLI (wie bisher) oder OpenCode CLI. */
        const val CLI_CODEX = "codex"
        const val CLI_OPENCODE = "opencode"
        const val CLI_CLAUDE = "claude"
        const val CLI_KIMI = "kimi"
        const val CLI_ANTIGRAVITY = "antigravity"
        /** Ein frei gewähltes Modell aus dem OpenLauncher-Angebot (Relay und Mac-Brücke kennen es unter diesem Namen). */
        const val MODELL_KATALOG = "katalog"
        /** Antworten auf die Review-Frage vor einem Codex-Auftrag. */
        const val REVIEW_JA = "ja"
        const val REVIEW_NEIN = "nein"
        /** Was eine Auswahlkarte gerade fragt. */
        private const val STUFE_MODELL = "modell"
        private const val STUFE_CLI = "cli"
        private const val STUFE_REVIEW = "review"

        /** Trägt ein frei gewähltes Modell (Effort, Profil, Modus, CLI) bis PcBruecke.starte. */
        private class KatalogStart(val wahl: OpenLauncherKatalog.Wahl) : kotlin.coroutines.AbstractCoroutineContextElement(KatalogStart) {
            companion object Key : kotlin.coroutines.CoroutineContext.Key<KatalogStart>
        }

        /** Trägt die CLI-Wahl von starteErstes bis PcBruecke.starte (die Ziele kennen nur das Modell). */
        private class CliWahl(val cli: String) : kotlin.coroutines.AbstractCoroutineContextElement(CliWahl) {
            companion object Key : kotlin.coroutines.CoroutineContext.Key<CliWahl>
        }

        /** Stand der Modellwahl-Karten je wahl_id: offen, erledigt (per Sprache gewählt) oder das gewählte Modell. Nur im Speicher. */
        const val WAHL_OFFEN = "offen"
        const val WAHL_ERLEDIGT = "erledigt"
        val wahlStand = kotlinx.coroutines.flow.MutableStateFlow<Map<String, String>>(emptyMap())
        private val offeneWahlen = java.util.concurrent.ConcurrentHashMap<String, OffeneWahl>()
        /** Per Knopf gestartete Aufträge je Gespräch (nur im Speicher) und wie lange ein erneuter Aufruf als derselbe Auftrag gilt. */
        private val knopfStarts = java.util.concurrent.ConcurrentHashMap<String, KnopfStart>()
        private const val KNOPF_FRIST_MS = 5 * 60_000L

        /** Was das Modell nach einer Auswahlkarte tun soll: nichts selbst fragen, die Karte fragt (siehe [wahlInRunde]). */
        private const val KARTE_FRAGT = "Jarvis zeigt und sagt die Frage selbst mit festem Wortlaut; stell KEINE eigene Rückfrage dazu und beende deine Antwort knapp."

        /** Titel des Werkzeugs programmieren (so steht es in den Änderungen einer Runde). */
        const val WERKZEUG_TITEL = "Programmierauftrag starten"

        /**
         * Jede Auswahlkarte mit Gespräch, festem Fragetext, Knöpfen und Zeitpunkt; nur im Speicher, einen Tag lang. [ausRunde]: vom
         * Werkzeug in einer Runde des Modells gestellt; Folgekarten nach einem Tipp tragen ihren [vorgaenger].
         */
        private data class Karte(val gespraech: String, val frage: String, val optionen: List<Pair<String, String>>, val zeit: Long,
                                 val ausRunde: Boolean = true, val vorgaenger: String? = null)
        private val karten = java.util.concurrent.ConcurrentHashMap<String, Karte>()
        private const val KARTEN_HALTEN_MS = 24 * 60 * 60_000L
        /** Karten, deren Tipp gerade verarbeitet wird (Rechner prüfen, Folgekarte, Start), und wie lange die Ansage darauf wartet. */
        private val tippInArbeit: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
        private const val TIPP_WARTEN_MS = 8_000L
        /** Hörbare Erinnerung an eine noch offene Modellwahl (siehe modellErinnerung), je wahl_id höchstens eine. */
        private val erinnerungen = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Job>()
        private const val ERINNERUNG_NACH_MS = 30_000L
        private const val ERINNERUNG_TEXT = "Welches Modell möchtest du verwenden: Claude Opus 5.5 oder Sol 6.1?"
        /**
         * Wahlen, die unten Knöpfe bekommen (Franks Wunsch vom 24.10.2026: erst nach dem Abschicken): nur die jüngste Modellfrage je
         * Gespräch samt ihren Folgefragen. Nach einem Neustart nur, was höchstens [FRISCH_NACH_NEUSTART_MS] alt ist; liegengebliebene
         * Wahlen bleiben per Sprache wählbar, zeigen aber keine Knöpfe und erinnern nicht mehr hörbar.
         */
        val frischeWahlen = kotlinx.coroutines.flow.MutableStateFlow<Set<String>>(emptySet())
        private const val FRISCH_NACH_NEUSTART_MS = 10 * 60_000L
        private fun wahlNochOffen(id: String) = wahlStand.value[id] == WAHL_OFFEN && offeneWahlen.containsKey(id) && id !in tippInArbeit

        /** Das Gespräch, in dem die Karte [wahlId] steht (leer, wenn unbekannt). */
        fun gespraechDerKarte(wahlId: String): String = karten[wahlId]?.gespraech.orEmpty()

        /** Kontext einer Karte im Gespräch: wahl_id, App und Knöpfe (so liest sie die Oberfläche). */
        private fun kartenKontext(id: String, app: String, optionen: List<Pair<String, String>>): String =
            JSONObject().put("wahl_id", id).put("app", app)
                .put("optionen", JSONArray().apply { optionen.forEach { (wert, label) -> put(JSONObject().put("wert", wert).put("label", label)) } }).toString()

        /** Eine noch offene Auswahlkarte, wie sie im Gespräch steht. */
        data class OffeneKarte(val id: String, val frage: String, val kontext: String)

        /**
         * Die offenen Karten von [gespraech], älteste zuerst. Die App trägt jede davon ins offene Gespräch nach, die dort fehlt:
         * Ging die Karte beim Zustellen verloren (Neustart, ein überholender Speicherstand), stehen die Knöpfe trotzdem da.
         */
        fun offeneKarten(gespraech: String): List<OffeneKarte> {
            if (gespraech.isEmpty()) return emptyList()
            val stand = wahlStand.value
            return karten.entries.filter { it.value.gespraech == gespraech && stand[it.key] == WAHL_OFFEN }.sortedBy { it.value.zeit }
                .mapNotNull { (id, k) -> offeneWahlen[id]?.let { w -> OffeneKarte(id, k.frage, kartenKontext(id, w.app, k.optionen)) } }
        }

        /** Franks kurze Antwort nennt genau ein Modell: dessen Kennung, sonst leer (lang, beide oder keins). */
        internal fun modellAusAntwort(worte: String?): String {
            val t = worte?.trim()?.lowercase(Locale.ROOT).orEmpty()
            if (t.isEmpty() || t.length > 60) return ""
            val opus = Regex("\\bopus\\b").containsMatchIn(t)
            val sol = Regex("\\bsol\\b").containsMatchIn(t)
            return when { opus && !sol -> MODELL_OPUS; sol && !opus -> MODELL_SOL; else -> "" }
        }

        /** Nennt Frank selbst ein Modell (Opus, Sol oder GPT)? Nur dann gilt ein vom Modell eingesetztes modell ohne Knöpfe. */
        internal fun modellGenannt(worte: String): Boolean = Regex("\\b(opus|sol|gpt)\\b").containsMatchIn(worte.lowercase(Locale.ROOT))

        private const val WAHL_PREFS = "jarvis_auswahlkarten"
        @Volatile private var wahlenGeladen = false

        /**
         * Hält Karten, ihren Stand und offene Aufträge in den Prefs: Ein Update von Jarvis (oft aus einer parallelen Sitzung)
         * beendet die App ohne Vorwarnung, danach waren die Knöpfe sonst weg („Diese Auswahl ist nicht mehr aktiv“).
         */
        private fun wahlenSichern(context: Context) {
            val jetzt = System.currentTimeMillis()
            val stand = wahlStand.value
            val liste = JSONArray()
            karten.forEach { (id, k) ->
                val s = stand[id] ?: return@forEach
                if (jetzt - k.zeit > KARTEN_HALTEN_MS) return@forEach
                liste.put(JSONObject().put("id", id).put("gespraech", k.gespraech).put("frage", k.frage).put("zeit", k.zeit).put("stand", s)
                    .put("vorgaenger", k.vorgaenger.orEmpty())
                    .put("optionen", JSONArray().apply { k.optionen.forEach { (wert, label) -> put(JSONObject().put("wert", wert).put("label", label)) } })
                    .apply {
                        offeneWahlen[id]?.let { w ->
                            put("wahl", JSONObject().put("app", w.app).put("auftrag", w.auftrag).put("review", w.review).put("neu", w.neu)
                                .put("gespraech", w.gespraech).put("ziel", w.ziel).put("modell", w.modell).put("cli", w.cli).put("stufe", w.stufe).put("seit", w.seit))
                        }
                    })
            }
            runCatching { context.getSharedPreferences(WAHL_PREFS, Context.MODE_PRIVATE).edit().putString("karten", liste.toString()).apply() }
        }

        /** Einmal je Prozess: die gesicherten Karten zurück (ohne Sprachursprung; sie gehören zu keiner laufenden Runde mehr). */
        private fun wahlenLaden(context: Context) {
            synchronized(this) { if (wahlenGeladen) return; wahlenGeladen = true }
            val roh = runCatching { context.getSharedPreferences(WAHL_PREFS, Context.MODE_PRIVATE).getString("karten", null) }.getOrNull() ?: return
            runCatching {
                val jetzt = System.currentTimeMillis()
                val liste = JSONArray(roh)
                val stand = HashMap<String, String>()
                val frisch = HashSet<String>()
                for (i in 0 until liste.length()) {
                    val o = liste.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    val zeit = o.optLong("zeit")
                    if (id.isEmpty() || jetzt - zeit > KARTEN_HALTEN_MS) continue
                    val l = o.optJSONArray("optionen")
                    val optionen = if (l == null) emptyList() else (0 until l.length()).mapNotNull { l.optJSONObject(it) }.map { it.optString("wert") to it.optString("label") }
                    val w = o.optJSONObject("wahl")?.let {
                        OffeneWahl(it.optString("app"), it.optString("auftrag"), it.optBoolean("review"), it.optBoolean("neu"), it.optString("gespraech"), null,
                            it.optString("ziel"), it.optString("modell"), it.optString("cli"), it.optString("stufe", STUFE_MODELL), it.optLong("seit", zeit))
                    }
                    val s = o.optString("stand")
                    // Offen ohne gemerkten Auftrag ließe sich nicht starten: dann nicht mehr aktiv.
                    if (s == WAHL_OFFEN && w == null) continue
                    karten.putIfAbsent(id, Karte(o.optString("gespraech"), o.optString("frage"), optionen, zeit, ausRunde = false,
                        vorgaenger = o.optString("vorgaenger").ifEmpty { null }))
                    if (s == WAHL_OFFEN && w != null) offeneWahlen.putIfAbsent(id, w)
                    if (s == WAHL_OFFEN && w != null && jetzt - w.seit in 0 until FRISCH_NACH_NEUSTART_MS) frisch += id
                    stand[id] = s
                }
                frischeWahlen.update { it + frisch }
                wahlStand.update { stand + it }
            }.onFailure { Logbuch.fehler("Auswahlkarten laden", it) }
        }

        /**
         * Hat die Runde in [gespraech] (begonnen [seit]) eine Auswahlkarte gestellt, ersetzt das hier die Antwort des Modells, damit
         * die Frage nur einen festen Weg hat: Ist die Karte noch offen, ihr Fragetext; hat Frank schon getippt (auch schneller,
         * als das Modell antworten konnte), nichts (leer) bzw. nur die Frage einer Folgekarte.
         * null = keine Karte in der Runde oder per Sprache gewählt.
         */
        suspend fun wahlInRunde(gespraech: String, seit: Long): String? {
            if (gespraech.isEmpty()) return null
            val (id, k) = karten.entries.filter { it.value.ausRunde && it.value.gespraech == gespraech && it.value.zeit >= seit }
                .maxByOrNull { it.value.zeit }?.toPair() ?: return null
            return ansage(id, k)
        }

        private suspend fun ansage(id: String, k: Karte): String? {
            kotlinx.coroutines.withTimeoutOrNull(TIPP_WARTEN_MS) { while (id in tippInArbeit) kotlinx.coroutines.delay(100) }
            // Läuft der Start nach der Wartezeit noch (Rechner langsam), steht weder Erfolg noch Folgekarte fest: nur kurz vertrösten,
            // statt still zu bleiben oder einen Start zu behaupten. Scheitert er, steht der Grund im Gespräch.
            if (id in tippInArbeit) return "Einen Moment."
            return when (val s = wahlStand.value[id]) {
                null, WAHL_ERLEDIGT -> null
                // Offen, auch nach einem Fehlstart (der Grund steht dann schon im Gespräch).
                WAHL_OFFEN -> k.frage
                // Schon getippt: keine gesprochene Bestätigung der Wahl (Frank-Wunsch 2026-10-24), nur eine Folgekarte fragt noch.
                // Leer = Karte erledigt, nichts sagen; null hieße „keine Karte“ und brächte die freie Antwort des Modells zurück.
                else -> karten.entries.firstOrNull { it.value.vorgaenger == id }?.let { (fid, f) -> ansage(fid, f) }.orEmpty()
            }
        }

        /** Stand eines Auftrags, dessen Start nach dem Senden ins Zeitlimit lief: vielleicht gestartet. Die erste Meldung der Sitzung ersetzt ihn. */
        const val STAND_UNKLAR = "unklar"

        /** Gesperrt, bis die Sitzung wieder arbeitet: nicht angemeldet („Login expired“) bzw. ein anderer API-Fehler. */
        const val STAND_ANMELDUNG = "anmeldung"
        const val STAND_STOERUNG = "stoerung"
        val GESPERRT = listOf(STAND_ANMELDUNG, STAND_STOERUNG)

        /**
         * Lief das Zeitlimit ab, bevor die Anfrage gesendet war? OkHttp meldet auch den Verbindungsaufbau („failed to connect … after“,
         * „connect timed out“) und den TLS-Handschlag als SocketTimeoutException; dann kam sicher nichts an. Nur ein Zeitlimit beim
         * Warten auf die Antwort („timeout“, „Read timed out“) heißt: womöglich angekommen.
         */
        internal fun vorDemSenden(fehler: java.net.SocketTimeoutException): Boolean =
            fehler.message.orEmpty().lowercase(Locale.ROOT).let { "connect" in it || "handshake" in it }

        /**
         * Der Text, den PC und Cloud gleichermaßen bekommen: Auftrag, dann die Fortschritts-Anweisung. Franks Programmierregeln
         * gehören nicht hinein (reiner Jarvis-Kontext). Wird es zu lang, kürzt es nur den Auftrag; der Fortschritt kommt immer ganz an.
         */
        fun auftragMitFortschritt(auftrag: String, fortschritt: String): String {
            val rest = "\n\n" + fortschritt
            // Platz für den Kopf der Cloud-Routine (APP, CODEX-REVIEW, AUFTRAG).
            val platz = HOECHSTENS - 300 - rest.length
            val kopf = if (auftrag.length <= platz) auftrag else auftrag.take(maxOf(0, platz)) + " … (gekürzt)"
            return kopf + rest
        }

        private const val CLAUDE_APP = "com.anthropic.claude"

        /** Für die Sprachzustellung fertiger Aufträge: läuft unabhängig vom Werkzeugaufruf, der sofort „Gemeldet.“ zurückgibt. */
        private val ansagen = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Default)

        /** Extras der Benachrichtigung einer Rückfrage: welches Gespräch und welche Rückfrage die App öffnet. */
        const val EXTRA_GESPRAECH = "de.frank.jarvis.rueckfrage_gespraech"
        const val EXTRA_RUECKFRAGE = "de.frank.jarvis.rueckfrage_id"
        private val STATUS_HTTP: OkHttpClient = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
        /** Nur Zusatzinfo zur Auftragsübersicht: kurz, damit eine Sprachantwort nie lange darauf wartet. */
        private val SITZUNGS_HTTP: OkHttpClient = STATUS_HTTP.newBuilder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS).callTimeout(4, TimeUnit.SECONDS).build()
        private val HTTP: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    }
}
