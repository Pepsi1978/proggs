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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            "NACHTRÄGE nur auf ausdrücklichen Wunsch: programmier_nachtrag nur, wenn Frank selbst einen Nachtrag in eine bestehende Sitzung verlangt („Schick einen Nachtrag " +
            "in die gleiche Session“, „sag dem Programmierer noch …“), und nur in genau die Sitzung, die er bezeichnet. Ordne einen Wunsch nie selbst als Nachtrag ein und " +
            "wähle nie selbst eine Sitzung: gleiche App, ähnliches Thema, ein gerade fertiger Auftrag oder eine vorhandene Sitzung sind kein Grund. Ist unklar, ob Frank einen " +
            "neuen Auftrag oder einen Nachtrag will oder welche Sitzung er meint, frag kurz und sende nichts. Ohne Nachtragswunsch ist ein Programmierwunsch ein neuer Auftrag. " +
            "Bevor du für Jarvis selbst eine neue Funktion vorschlägst oder beauftragst, prüfe mit funktionen_lesen, ob es sie schon gibt. " +
            "Sagt Frank „programmiere“, „bau in App … ein“, „ändere in … dass …“ oder „mach in … einen Knopf/Filter/…“, rufst du programmieren auf. " +
            "Den Auftrag schreibst du nach den Regeln am Feld auftrag. Ist unklar, welche App gemeint ist, frag Frank kurz, bevor du startest. Programmiere nie selbst im Gespräch."

    private val app = context.applicationContext
    private val e get() = Einstellungen.get(app)

    /** In dieser Reihenfolge versucht; das erste bereite Ziel bekommt den Auftrag. */
    private val ziele: List<ProgrammierZiel> by lazy { listOf(PcBruecke(), CloudRoutine()) }

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
        "$pc $cloud"
    }

    /**
     * Beide Brücken einzeln: Der Relay meldet den Windows-PC als Rang „haupt“ und den Mac als „ersatz“. Ein Rechner, der sich
     * seit dem letzten Serverstart nie gemeldet hat, fehlt in der Liste.
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
        // „rechner“ ist der Rechner, den der Relay für den nächsten Auftrag wählt (Hauptrechner nur mit frischer Abfrage).
        val wohin = when {
            !pcDa -> "Programmieraufträge gehen in die Cloud."
            haupt?.optBoolean("da") == true && ersatz?.optBoolean("da") == true && naechster != haupt.optString("rechner") ->
                "Der nächste Auftrag ginge an $naechster: Der Windows-PC fragt gerade nicht ab, deshalb springt der Mac ein."
            ersatz?.optBoolean("da") == true -> "Der nächste Auftrag ginge an $naechster; der Windows-PC hat Vorrang, ist er aus, springt der Mac ein."
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

    private fun pcAnfrage() = Request.Builder().url("https://${e.serverHost}/geraet/pc").header("X-Jarvis-Token", e.serverToken).build()

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
                    pflicht = listOf("auftrag_id", "stand", "schritt"),
                ),
                nurLesen = false,
            ) { a ->
                val id = a.optString("auftrag_id").trim()
                // Früher hart nach 400 Zeichen abgeschnitten (mitten im Satz); jetzt länger und sichtbar gekürzt.
                val eintrag = Auftragsbuch.melde(app, id, a.optString("stand"),
                    ProgrammierAbschluss.kuerzen(a.optString("schritt").trim(), ProgrammierAbschluss.MAX_FORTSCHRITT), a.optString("link").trim())
                if (eintrag == null) Ergebnis("Unbekannte auftrag_id.", fehler = true)
                else {
                    // Die Cloud-Sitzung erreicht Jarvis nur über ihre eigenen Meldungen: Nachträge reisen mit der Antwort zurück.
                    // Am PC stellt der Hook zu (sonst käme ein Nachtrag doppelt an).
                    // Nicht bei frage (die Rückfrage bliebe sonst unsichtbar) und nicht bei fehler: dann bleibt er für die nächste Meldung liegen.
                    val nachtraege = if (eintrag.optString("ziel") == ZIEL_CLOUD && a.optString("stand") in listOf("laeuft", "fertig"))
                        Auftragsbuch.setzeNachtraege(app, id, null, setOf("angenommen"), "zugestellt") else emptyList()
                    // Geht gerade ein Nachtrag an die Sitzung, arbeitet sie weiter: dann kein „Fertig programmiert“ als Benachrichtigung.
                    verteile(eintrag, null, benachrichtigen = nachtraege.isEmpty())
                    // Wirklich zu Ende (kein Nachtrag unterwegs): im Sprachgespräch gestellt, dann die kurze Ansage. PC (Windows, Mac)
                    // und Cloud melden das Ende gleichermaßen hier; der Aufruf wartet nicht auf das Vorlesen.
                    if (nachtraege.isEmpty()) sprachAbschluss(eintrag)
                    if (nachtraege.isEmpty()) Ergebnis("Gemeldet.")
                    else {
                        Auftragsbuch.melde(app, id, "laeuft", "📨 Nachtrag an die Sitzung übergeben: " + nachtraege.joinToString(" · ") { it.optString("text").replace('\n', ' ').take(200) }, "")
                            ?.let { verteile(it, null, von = ProgrammierChat.VON_JARVIS) }
                        Ergebnis("Gemeldet.\n\nNACHTRAG VON FRANK zu diesem Auftrag (AUFTRAG-ID ${eintrag.optString("id")}), über Jarvis. Er gehört zu genau diesem Auftrag: " +
                            "Setze ihn in dieser Sitzung um, schließe danach wieder vollständig ab und melde erneut stand=fertig.\n\n" +
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
                    "nicht zustellbar sagst du Frank offen, ohne Ersatzauftrag. Derselbe Nachtrag ein zweites Mal wird nicht doppelt zugestellt.",
                schema = schema(
                    "auftrag_id" to text("Die Nummer des Auftrags, den Frank bezeichnet hat (fünf Zeichen, zum Beispiel BD2XW). Über das Plugin Pflicht; in einem Jarvis-Gespräch darf sie fehlen, wenn Frank „diese/die gleiche Session“ sagt und das Gespräch genau einen Auftrag hat."),
                    "text" to text("Der Nachtrag, vollständig und für sich verständlich: was zusätzlich oder anders werden soll, mit dem nötigen Kontext aus dem Gespräch."),
                    "franks_worte" to text("Franks eigene Aufforderung zum Nachtrag, wörtlich (zum Beispiel „Schick einen Nachtrag in die gleiche Session an den Programmierer“). Nie umformuliert, nie erfunden."),
                    pflicht = listOf("text", "franks_worte"),
                ),
                nurLesen = false,
            ) { a -> nachtrag(a.optString("auftrag_id").trim(), DiktatTrenner.auftrag(a.optString("text")), a.optString("franks_worte").trim()) },
            Werkzeug(
                name = "programmieren",
                titel = "Programmierauftrag starten",
                beschreibung = "Jarvis: programmiert etwas in einer von Franks Apps („Jarvis, programmiere in Geniale Aufgaben einen Filter für …“, „bau in den Wecker ein, dass …“, " +
                    "„ändere in Jarvis …“, „schicke bitte folgenden Auftrag an den Programmierer: …“, „schick das an den Programmierer“, „lass das programmieren“). " +
                    "Aus ChatGPT bekommt jeder Auftrag in Jarvis eine eigene Programmier-Session, in der Frank Schritte und Rückfragen sieht. Der Auftrag geht an Claude Code: an Franks PC, wenn er an ist (schnell, die App kommt per WLAN aufs Handy), sonst in die Cloud " +
                    "(etwa zehn bis zwanzig Minuten, dann meldet UpdateStation die neue Version). Es findet die App in Franks Repo und programmiert die Änderung. Dieser Aufruf kehrt sofort zurück. Die Sitzung kennt nur den Auftragstext (Regeln am Feld auftrag). " +
                    "Franks Programmierregeln sind nur für dich: Richte den Auftrag nach ihnen aus " +
                    "(im Plugin vorher mit jarvis_gemerktes_lesen lesen), schreibe ihren Text aber nicht hinein; Jarvis hängt sie nicht an. Ein fertiger Auftrag vom Agenten Code-Analyse oder aus repo_lesen kann unverändert hinein. " +
                    "Ist unklar, welche App gemeint ist, frag Frank vorher. Jeder Aufruf ist ein NEUER Auftrag mit eigener Sitzung, auch wenn zur selben App oder zum selben " +
                    "Thema schon einer läuft oder gerade fertig wurde; er hängt sich nie an einen alten. Hat das Gespräch schon einen Auftrag, bekommt der neue eine eigene, " +
                    "getrennte Jarvis-Session. In eine bestehende Sitzung geht ein Wunsch nur, wenn Frank ausdrücklich einen Nachtrag dorthin verlangt (programmier_nachtrag); " +
                    "ist unklar, ob er einen neuen Auftrag oder einen Nachtrag will, frag ihn kurz, bevor du etwas startest. " +
                    "„ ; “ (Leerzeichen, Semikolon, Leerzeichen; früher auch „;“ auf eigener Zeile) in Franks Nachrichten trennt seine einzelnen Spracheingaben und löst allein nie einen Auftrag aus. " +
                    "Verlangt Frank ausdrücklich die Übergabe solcher Abschnitte, ist jeder nicht leere Abschnitt eine Aufgabe: alle zusammen in EINEM Aufruf " +
                    "(eine Sitzung), im Auftrag durch „ ; “ getrennt; ein Trenner am Ende ist keine weitere Aufgabe.",
                schema = schema(
                    "app" to text("Name der App, wie Frank sie nennt, zum Beispiel „Geniale Aufgaben“, „Genialer Wecker“, „Jarvis“; gern mit Ordner im Repo, wenn bekannt."),
                    "auftrag" to text(AUFTRAGS_REGELN),
                    "review" to schalter("true = vor der Auslieferung ein Review laufen lassen (dauert länger). Vorgabe false."),
                    "ziel" to text("Nur wenn Frank es ausdrücklich sagt: pc oder cloud. Vorgabe automatisch (PC, wenn er an ist, sonst Cloud).", listOf("automatisch", "pc", "cloud")),
                    "neu" to schalter("true, wenn Frank ausdrücklich eine neue, getrennte Session will: Der Auftrag bekommt dann in jedem Fall eine eigene Jarvis-Session. Vorgabe false."),
                    pflicht = listOf("app", "auftrag"),
                ),
                nurLesen = false,
            ) { a ->
                val appName = a.optString("app").trim()
                // Mehrere Aufgaben einheitlich getrennt und ohne Trenner am Ende (siehe DiktatTrenner.auftrag).
                val auftrag = DiktatTrenner.auftrag(a.optString("auftrag"))
                // Früher lehnte Jarvis hier ab, sobald zum Gespräch oder zur App schon ein Auftrag lief, und schickte das Modell zu
                // programmier_nachtrag: So landeten neue Wünsche ungefragt als Nachtrag in alten Sitzungen. Jetzt ist jeder Aufruf
                // ein neuer Auftrag mit eigener Sitzung (siehe NachtragsRegel.eigenesGespraech).
                val wahl = when (a.optString("ziel").lowercase()) {
                    "pc" -> ziele.filterIsInstance<PcBruecke>()
                    "cloud" -> ziele.filterIsInstance<CloudRoutine>()
                    else -> ziele
                }.filter { it.bereit() }
                when {
                    appName.isEmpty() -> Ergebnis("Welche App ist gemeint? Bitte Frank fragen.", fehler = true)
                    auftrag.length < 20 -> Ergebnis("Der Auftrag ist zu knapp. Formuliere ihn vollständig aus.", fehler = true)
                    wahl.isEmpty() -> Ergebnis(ziele.last().fehlt(), fehler = true)
                    else -> starteErstes(wahl, appName, auftrag, a.optBoolean("review"), Auftragsbuch.neueNummer(), a.optBoolean("neu"))
                }
            },
        )
    }

    /** Versucht die Ziele der Reihe nach; [NichtDa] heißt: nichts gestartet, das nächste Ziel ist dran. */
    private suspend fun starteErstes(wahl: List<ProgrammierZiel>, appName: String, auftrag: String, review: Boolean, nummer: String, neu: Boolean): Ergebnis {
        val uebersprungen = mutableListOf<String>()
        val (ausChatGpt, aufrufGespraech) = herkunft()
        // Im Sprachgespräch gestellt (Hey Jarvis mit Vorlesen oder Live): dauerhaft im Auftragsbuch, damit der Abschluss später
        // vorgelesen wird, auch nach Stille-Ende, App-Neustart oder aus dem Hintergrund. Aus ChatGPT nie.
        val ursprung = if (ausChatGpt) null else kotlinx.coroutines.currentCoroutineContext()[de.frank.jarvis.speech.SprachUrsprung]
        // Aus ChatGPT nie das gerade offene App-Gespräch, in der App nie ein Gespräch, das schon einen Auftrag hat, und bei
        // ausdrücklich neuer Session immer: Der Auftrag bekommt nach dem Start sein eigenes (siehe unten).
        val eigenes = NachtragsRegel.eigenesGespraech(ausChatGpt, neu, if (aufrufGespraech.isEmpty()) 0 else Auftragsbuch.alleImGespraech(app, aufrufGespraech).size)
        var gespraech = if (eigenes) "" else aufrufGespraech
        // Franks Programmierregeln gehen bewusst nicht mit: Sie sind reiner Jarvis-Kontext (wie Jarvis Aufträge steuert), keine Anweisung an die Sitzung.
        for (ziel in wahl) {
            val mitFortschritt = auftragMitFortschritt(auftrag, fortschrittsAnweisung(nummer, pc = ziel is PcBruecke))
            val start = try {
                ziel.starte(appName, mitFortschritt, review, nummer)
            } catch (nichtDa: NichtDa) {
                uebersprungen += nichtDa.message.orEmpty()
                continue
            } catch (zeit: java.net.SocketTimeoutException) {
                if (vorDemSenden(zeit)) {
                    // Die Verbindung kam gar nicht zustande, also kam auch nichts an: wie „nicht erreichbar“, das nächste Ziel (die Cloud) übernimmt.
                    uebersprungen += "${ziel.name} war nicht erreichbar (Zeitlimit beim Verbindungsaufbau)" + if (ziel is PcBruecke) ", deshalb die Cloud." else "."
                    continue
                }
                // Keine Antwort mehr nach dem Senden: Der Auftrag kam womöglich an und läuft. Nicht als gescheitert melden (sonst
                // startet ihn Frank ein zweites Mal), sondern als „unklar“ eintragen; meldet sich die Sitzung, wird daraus ein normaler Auftrag.
                if (eigenes) gespraech = withContext(Dispatchers.IO) { runCatching { ProgrammierChat.neuesGespraech(app, appName, auftrag, ueberPlugin = ausChatGpt, ausGespraech = aufrufGespraech) }.getOrDefault("") }
                val eintrag = Auftragsbuch.neu(app, nummer, appName, ziel.name, "", "Unklar: ${ziel.name} hat den Start nicht rechtzeitig bestätigt (Zeitüberschreitung). " +
                    "Womöglich läuft der Auftrag trotzdem.", gespraech, auftrag, stand = STAND_UNKLAR, ursprung = ursprung)
                if (eigenes) Auftragsbuch.herkunftMerken(app, nummer, aufrufGespraech)
                verteile(eintrag, null, von = ProgrammierChat.VON_JARVIS)
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
    private fun fortschrittsAnweisung(nummer: String, pc: Boolean) = if (pc)
        "FORTSCHRITT (AUFTRAG-ID: $nummer): Zwischenschritte und Rückfragen meldet die Sitzung automatisch an Jarvis; frag normal mit AskUserQuestion, " +
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
     * Eine Meldung des Hooks vom PC (Signal „programmieren“ über den Relay): ins Auftragsbuch, als Benachrichtigung und in
     * das Gespräch, aus dem der Auftrag kam. Mit „frage“ ist es eine Rückfrage, auf die der Hook wartet.
     */
    fun meldungVomServer(daten: JSONObject) {
        val auftragId = daten.optString("auftrag_id")
        if (daten.optString("stand") == "puls") {
            // Lebenszeichen: nur die drehende Anzeige auffrischen, keine Nachricht, keine Benachrichtigung. Ein gesperrter Auftrag
            // (Anmeldung, Störung) arbeitet nachweislich wieder, wenn der Puls eine Minute nach der Sperre kommt (ein früherer
            // stammt womöglich noch von vor dem Fehler); normalerweise meldet der Hook das selbst mit „anmeldung_ok“.
            val gesperrt = Auftragsbuch.finde(app, auftragId)?.takeIf { it.optString("stand") in GESPERRT }
            if (gesperrt != null && nachSperre(gesperrt, daten)) {
                Auftragsbuch.melde(app, auftragId, "laeuft", "▶️ Die Sitzung arbeitet wieder.", "")?.let { verteile(it, null, von = ProgrammierChat.VON_JARVIS); benachrichtige(it) }
                return
            }
            Auftragsbuch.puls(app, auftragId)?.let { aktivitaetMelden(it) }
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
            sperrSeit = if (daten.optString("stand") in GESPERRT) daten.optLong("seit", System.currentTimeMillis() / 1000) else 0L)
        if (eintrag == null) {
            // Unbekannter Auftrag: nie still verwerfen, sondern sichtbar im Logbuch (Grund für „Frage kam nicht an“).
            Logbuch.system("Meldung ohne Auftrag", "${daten.optString("stand")} für ${daten.optString("auftrag_id")}: ${daten.optString("schritt").take(120)}")
            return
        }
        // Wiederholte Sperre (gleicher Stand) oder alte Meldung während der Sperre: keine zweite Benachrichtigung.
        verteile(eintrag, frage, benachrichtigen = bisher?.optString("stand") != eintrag.optString("stand") || stand !in GESPERRT)
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
            if (RueckfrageStand.endgueltig(stand)) Auftragsbuch.frageErledigt(app, frageId)
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
        val text = schritt.ifBlank { "📨 Nachtrag in der Sitzung angekommen: " + neu.joinToString(" · ") { it.optString("text").replace('\n', ' ').take(300) } }
        // Den Satz des Hooks meldet der Programmierer; ohne ihn schreibt Jarvis selbst, dass der Nachtrag angekommen ist.
        Auftragsbuch.melde(app, auftragId, "laeuft", ProgrammierAbschluss.kuerzen(text, ProgrammierAbschluss.MAX_MELDUNG), "")
            ?.let { verteile(it, null, von = if (schritt.isBlank()) ProgrammierChat.VON_JARVIS else ProgrammierChat.VON_PROGRAMMIERER) }
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
        val cloud = eintrag.optString("ziel") == ZIEL_CLOUD
        if (!NachtragsRegel.zustellbar(cloud, eintrag.optString("stand"))) {
            return Ergebnis("Nicht zustellbar: Die Cloud-Sitzung von Auftrag $id ($appName) ist beendet (${Auftragsbuch.standText(eintrag.optString("stand"))}) und lässt sich " +
                "nicht fortsetzen. Der Nachtrag wurde nicht gesendet. Sag Frank das offen und starte keinen Ersatzauftrag; einen neuen Auftrag nur, wenn Frank ihn danach " +
                "ausdrücklich verlangt.", fehler = true)
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
        val antwort = runCatching {
            withContext(Dispatchers.IO) {
                val rumpf = JSONObject().put("auftrag_id", id).put("nachtrag_id", nummer).put("text", text)
                val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/nachtrag").header("X-Jarvis-Token", e.serverToken)
                    .post(rumpf.toString().toRequestBody("application/json".toMediaType())).build()
                HTTP.newCall(anfrage).execute().use { a ->
                    if (!a.isSuccessful) throw IllegalStateException("HTTP ${a.code} vom Jarvis-Server")
                    JSONObject(a.body?.string().orEmpty())
                }
            }
        }.getOrElse { fehler ->
            Auftragsbuch.setzeNachtraege(app, id, setOf(nummer), setOf("angenommen"), "fehler")
            return Ergebnis("Nachtrag $nummer ist nicht beim Jarvis-Server angekommen (${fehler.message ?: fehler.javaClass.simpleName}). Er ist im Auftrag $id vermerkt, " +
                "aber nicht zugestellt; ein erneuter Aufruf mit demselben Text schickt ihn noch einmal, ohne ihn zu verdoppeln.", fehler = true)
        }
        if (antwort.optBoolean("zugestellt")) {
            alsZugestellt(id, setOf(nummer), "")
            return Ergebnis("Zugestellt: Nachtrag $nummer ist in derselben Sitzung von Auftrag $id ($appName) angekommen, sie arbeitet ihn jetzt ab. " +
                "Umgesetzt ist er, wenn programmierauftraege ihn als bearbeitet zeigt.")
        }
        val vor = if (antwort.isNull("kontakt_vor_s")) -1 else antwort.optInt("kontakt_vor_s", -1)
        val warterVor = if (antwort.isNull("warter_vor_s")) -1 else antwort.optInt("warter_vor_s", -1)
        // Arbeitet: kürzlich eine Meldung, und danach hat kein Warter mehr im Leerlauf abgefragt (sonst ist die Sitzung seitdem weg).
        val arbeitet = vor in 0..600 && (warterVor < 0 || warterVor > vor)
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

    private fun aktivitaetMelden(eintrag: JSONObject) = ProgrammierChat.setze(
        eintrag.optString("gespraech"),
        ProgrammierChat.Aktivitaet(eintrag.optString("app"), eintrag.optBoolean("arbeitet"), eintrag.optLong("aktiv", eintrag.optLong("zeit")), eintrag.optString("schritt"), eintrag.optLong("start")),
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
            cloud = eintrag.optString("ziel") == ZIEL_CLOUD, fertig = fertig)
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
            benachrichtige(eintrag, vorlesen = frage != null || (sprachAbschlussGeplant(eintrag) == null && !schonAngesagt(eintrag)))
        val gespraech = eintrag.optString("gespraech")
        if (gespraech.isEmpty()) return
        val appName = eintrag.optString("app")
        val schritt = eintrag.optString("schritt")
        if (frage != null) {
            val fragen = frage.optJSONArray("fragen") ?: JSONArray()
            val kurz = (0 until fragen.length()).joinToString(" / ") { fragen.optJSONObject(it)?.optString("question").orEmpty() }.trim()
            val kontext = JSONObject().put("frage_id", frage.optString("frage_id")).put("auftrag", eintrag.optString("id")).put("app", appName)
                .put("fragen", fragen).put("frei", frage.optBoolean("frei")).put("beantwortet", false)
                .put("status", RueckfrageStand.OFFEN).put("bis", frage.optLong("bis")).put("eingang", System.currentTimeMillis() / 1000)
            ProgrammierChat.poste(app, gespraech, "❓ ${ProgrammierChat.VON_PROGRAMMIERER} · Rückfrage zu $appName: " + (if (frage.optBoolean("frei")) schritt.removePrefix("❓").trim() else kurz), "rueckfrage", kontext.toString())
            return
        }
        val zeichen = when (letzter) { "fertig" -> "✅"; "fehler" -> "❌"; "info" -> "📋"; in GESPERRT, STAND_UNKLAR -> "⚠️"; else -> if (von == ProgrammierChat.VON_JARVIS) "🤖" else "⚙️" }
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
            de.frank.jarvis.meldung.Meldungen.zeige(
                app, de.frank.jarvis.meldung.Meldungsarten.PROGRAMMIEREN,
                nummer,
                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis)
                    .setContentTitle(titel)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n${eintrag.optString("ziel")} · Auftrag ${eintrag.optString("id")}"))
                    // Zwischenschritte still aktualisieren; Rückfrage, Fertig und Problem melden sich hörbar.
                    .setOnlyAlertOnce(stand == "laeuft")
                    .setAutoCancel(stand == "fertig")
                    .apply { oeffnen?.let { setContentIntent(PendingIntent.getActivity(app, nummer, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)) } },
                // Zwischenschritte still; Rückfrage, Fertig und Problem nach Franks Einstellung (Ton, Wecker, Vorlesen).
                // Eine Sperre stört kein Live-Gespräch (still, aber sichtbar); Rückfrage, Fertig und Problem wie bisher auch dort mit Ton.
                titel, if (vorlesen) "$titel. $text" else null, laut = stand != "laeuft", imGespraechStill = stand in GESPERRT,
            )
        }
    }

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
            // Auch ein normales Gespräch, aus dem der Auftrag kam, gehört ab jetzt in den Reiter „Programmierung“.
            Gespraeche.markieren(context, gespraech, jetzt)
            return eintrag
        }

        /** Gespräche mit Auftrag und wann ihr erster Auftrag abgeschickt wurde (0 = Eintrag ohne Start). */
        fun gespraechStarts(context: Context): Map<String, Long> = Gespraeche.fruehesteStarts(alle(context).map { it.optString("gespraech") to it.optLong("start") })

        @Synchronized
        fun melde(context: Context, id: String, stand: String, schritt: String, link: String, frage: JSONObject? = null, sperrSeit: Long = 0L): JSONObject? {
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
                // Arbeitet die Sitzung? Nicht nach dem Zugende (info), am Ende und solange sie auf Frank wartet.
                .put("arbeitet", gueltig == "laeuft")
            if (frage != null) {
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
                if (e.optString("gespraech").isNotEmpty() && offen(e) && e.optLong("zeit") >= grenze) return e.optString("gespraech")
            }
            return null
        }

        /** Lebenszeichen der Sitzung (Puls vom Hook): Sie arbeitet. */
        @Synchronized
        fun puls(context: Context, id: String): JSONObject? {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return null
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
            STAND_UNKLAR -> "unklar, ob gestartet (Zeitüberschreitung beim Start; nicht neu starten, erst abwarten, ob sich die Sitzung meldet)"
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
        fun nachtragNeu(context: Context, id: String, text: String, quelle: String): Pair<JSONObject, Boolean>? {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return null
            val nachtraege = eintrag.optJSONArray("nachtraege") ?: JSONArray().also { eintrag.put("nachtraege", it) }
            val jetzt = System.currentTimeMillis()
            for (i in nachtraege.length() - 1 downTo 0) {
                val n = nachtraege.getJSONObject(i)
                if (NachtragsRegel.doppelt(n.optString("text"), n.optLong("zeit"), text, jetzt)) return n to true
            }
            val n = JSONObject().put("id", "N" + neueNummer()).put("text", text.take(4_000)).put("zeit", jetzt).put("quelle", quelle).put("stand", "angenommen")
            nachtraege.put(n)
            while (nachtraege.length() > 20) nachtraege.remove(0)
            schreib(context, liste)
            return n to false
        }

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
                        if (e.optString("ziel") == ZIEL_CLOUD && e.optString("stand") in listOf("fertig", "fehler")) "nein, die Cloud-Sitzung ist beendet"
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

    /** Wohin ein Programmierauftrag gehen kann. */
    interface ProgrammierZiel {
        val name: String
        fun bereit(): Boolean
        /** Ein Satz, was fehlt, solange das Ziel nicht bereit ist. */
        fun fehlt(): String
        suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String): Start
    }

    /**
     * Franks PC über den Jarvis-Relay: Die PC-Brücke holt den Auftrag ab und öffnet eine Claude-Code-Sitzung mit Remote
     * Control (in der Claude-App unter Code zu sehen). 409 vom Relay = Brücke fragt gerade nicht ab, der PC ist aus.
     */
    private inner class PcBruecke : ProgrammierZiel {
        override val name = ZIEL_PC
        override fun bereit() = e.serverToken.isNotBlank()
        override fun fehlt() = "Jarvis ist nicht mit seinem Server verbunden."

        override suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String): Start = withContext(Dispatchers.IO) {
            val rumpf = JSONObject().put("app", app).put("auftrag", auftrag.take(HOECHSTENS)).put("review", review).put("nummer", nummer)
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
            when {
                code == 409 -> throw NichtDa("Der PC ist nicht an, deshalb die Cloud.")
                code == 504 -> throw IllegalStateException("Der PC hat den Start nicht bestätigt. Bitte am PC nachsehen; mit ziel=cloud geht der Auftrag in die Cloud.")
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
            Start("", "Die Sitzung „Jarvis: $app“ läuft auf ${antwort.optString("rechner").ifEmpty { "dem PC" }} und ist in der Claude-App unter Code zu sehen. " +
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

        override suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String): Start = withContext(Dispatchers.IO) {
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

    companion object {
        /** Gespräche, denen das Auftragsbuch einen Programmierauftrag zuordnet (für den Reiter „Programmierung“). */
        fun gespraecheMitAuftrag(context: Context): Map<String, Long> = Auftragsbuch.gespraechStarts(context.applicationContext)

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

        /** So lang darf der ganze Text an ein Programmier-Ziel höchstens werden. */
        const val HOECHSTENS = 60_000

        /** Die Namen der Ziele, so stehen sie im Auftragsbuch (Feld „ziel“). */
        const val ZIEL_PC = "Claude Code auf deinem PC"
        const val ZIEL_CLOUD = "Claude Code in der Cloud"

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
