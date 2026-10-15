package de.frank.jarvis.faehigkeit

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import de.frank.jarvis.R
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.dienst.JarvisDienst
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
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
 */
class ProgrammierFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "programmieren"
    override val name = "Programmieren"
    override val beschreibung = "Programmieraufträge an Franks Apps an Claude Code übergeben; die fertige App kommt über UpdateStation aufs Handy."
    override val hinweise =
        "Fragt Frank, ob sein PC (Rechner, Computer) an oder erreichbar ist oder wohin ein Programmierauftrag gehen würde, rufe pc_status auf. " +
            "Fragt er, wie weit ein Programmierauftrag ist, was die Cloud-Sitzung gerade macht oder ob sie fertig ist, rufe programmierauftraege auf. " +
            "Steht im Gespräch eine offene Rückfrage einer Programmier-Sitzung (❓ mit frage_id und Optionen) und Frank antwortet darauf, gib die Antwort mit " +
            "programmier_antwort weiter: ordne sie der passenden Option zu (genau deren label), sonst gib Franks Worte als Freitext weiter. " +
            "Sagt Frank „programmiere“, „bau in App … ein“, „ändere in … dass …“ oder „mach in … einen Knopf/Filter/…“, rufst du programmieren auf. " +
            "Schreib den Auftrag vollständig aus, mit allem, was Frank im Gespräch gesagt hat: welche App, was genau anders werden soll, wie es aussehen und sich verhalten soll, " +
            "woran man erkennt, dass es fertig ist. Ist unklar, welche App gemeint ist, frag Frank kurz, bevor du startest. Programmiere nie selbst im Gespräch."

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
            val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/pc").header("X-Jarvis-Token", e.serverToken).build()
            STATUS_HTTP.newCall(anfrage).execute().use { antwort ->
                if (!antwort.isSuccessful) return@use "PC: unbekannt, der Jarvis-Server antwortet mit HTTP ${antwort.code}."
                val daten = JSONObject(antwort.body?.string().orEmpty())
                val rechner = daten.optString("rechner").ifEmpty { "Franks PC" }
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

    private fun zeitspanne(sekunden: Int): String = when {
        sekunden < 120 -> "$sekunden Sekunden"
        sekunden < 7_200 -> "${sekunden / 60} Minuten"
        else -> "${sekunden / 3_600} Stunden"
    }

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = "pc_status",
                titel = "Ist Franks PC erreichbar?",
                beschreibung = "Jarvis: prüft jetzt, ob Franks PC an und für Programmieraufträge erreichbar ist (die PC-Brücke meldet sich beim Jarvis-Server), " +
                    "wann er sich zuletzt gemeldet hat, und ob der Cloud-Weg eingerichtet ist. Nutze es bei „Ist mein Rechner an?“, „Ist mein PC erreichbar?“, " +
                    "„Läuft die PC-Brücke?“ oder „Wohin geht ein Programmierauftrag gerade?“.",
                schema = schema(),
                nurLesen = true,
            ) { Ergebnis(zustand()) },
            Werkzeug(
                name = "programmierauftraege",
                titel = "Programmieraufträge und ihr Fortschritt",
                beschreibung = "Jarvis: zeigt die letzten Programmieraufträge mit Stand (läuft, Rückfrage, fertig, Problem), dem zuletzt gemeldeten Schritt, wann er kam, " +
                    "wohin der Auftrag ging (PC oder Cloud) und dem Link zur Sitzung. Nutze es bei „Wie weit ist mein Programmierauftrag?“, „Was macht die Cloud-Sitzung?“, " +
                    "„Ist die App schon fertig?“ oder „Gib mir den Link zur Sitzung“.",
                schema = schema("anzahl" to zahl("Wie viele Aufträge, neueste zuerst; Vorgabe 5.")),
                nurLesen = true,
            ) { a -> Ergebnis(Auftragsbuch.uebersicht(app, a.optInt("anzahl", 5).coerceIn(1, 20))) },
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
                val eintrag = Auftragsbuch.melde(app, a.optString("auftrag_id").trim(), a.optString("stand"), a.optString("schritt").trim().take(400), a.optString("link").trim())
                if (eintrag == null) Ergebnis("Unbekannte auftrag_id.", fehler = true)
                else { verteile(eintrag, null); Ergebnis("Gemeldet.") }
            },
            Werkzeug(
                name = "programmier_antwort",
                titel = "Rückfrage einer Programmier-Sitzung beantworten",
                beschreibung = "Jarvis: gibt Franks Antwort auf eine Rückfrage einer laufenden Programmier-Sitzung weiter (die Rückfrage steht im Gespräch mit ❓, " +
                    "frage_id und Optionen). Die Sitzung wartet darauf und arbeitet danach weiter. Ordne Franks Worte der passenden Option zu und gib genau deren label an; " +
                    "passt keine, gib Franks Worte als Antwort weiter.",
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
                name = "programmieren",
                titel = "Programmierauftrag starten",
                beschreibung = "Jarvis: programmiert etwas in einer von Franks Apps („Jarvis, programmiere in Geniale Aufgaben einen Filter für …“, „bau in den Wecker ein, dass …“, " +
                    "„ändere in Jarvis …“). Der Auftrag geht an Claude Code: an Franks PC, wenn er an ist (schnell, die App kommt per WLAN aufs Handy), sonst in die Cloud " +
                    "(etwa zehn bis zwanzig Minuten, dann meldet UpdateStation die neue Version). Es findet die App in Franks Repo und programmiert die Änderung. Dieser Aufruf kehrt sofort zurück. Formuliere den Auftrag selbst vollständig aus, mit allem Wissen aus dem " +
                    "Gespräch; die Cloud-Sitzung kennt nur diesen Text. Ein fertiger Auftrag vom Agenten Code-Analyse oder aus repo_lesen kann unverändert hinein. " +
                    "Ist unklar, welche App gemeint ist, frag Frank vorher.",
                schema = schema(
                    "app" to text("Name der App, wie Frank sie nennt, zum Beispiel „Geniale Aufgaben“, „Genialer Wecker“, „Jarvis“; gern mit Ordner im Repo, wenn bekannt."),
                    "auftrag" to text("Der vollständige Programmierauftrag: Ziel, was genau anders werden soll (Bildschirm, Knopf, Verhalten, Texte), Randbedingungen, was Frank wichtig ist, woran man erkennt, dass es fertig ist."),
                    "review" to schalter("true = vor der Auslieferung ein Review laufen lassen (dauert länger). Vorgabe false."),
                    "ziel" to text("Nur wenn Frank es ausdrücklich sagt: pc oder cloud. Vorgabe automatisch (PC, wenn er an ist, sonst Cloud).", listOf("automatisch", "pc", "cloud")),
                    pflicht = listOf("app", "auftrag"),
                ),
                nurLesen = false,
            ) { a ->
                val appName = a.optString("app").trim()
                val auftrag = a.optString("auftrag").trim()
                val wahl = when (a.optString("ziel").lowercase()) {
                    "pc" -> ziele.filterIsInstance<PcBruecke>()
                    "cloud" -> ziele.filterIsInstance<CloudRoutine>()
                    else -> ziele
                }.filter { it.bereit() }
                when {
                    appName.isEmpty() -> Ergebnis("Welche App ist gemeint? Bitte Frank fragen.", fehler = true)
                    auftrag.length < 20 -> Ergebnis("Der Auftrag ist zu knapp. Formuliere ihn vollständig aus.", fehler = true)
                    wahl.isEmpty() -> Ergebnis(ziele.last().fehlt(), fehler = true)
                    else -> starteErstes(wahl, appName, auftrag, a.optBoolean("review"), Auftragsbuch.neueNummer())
                }
            },
        )
    }

    /** Versucht die Ziele der Reihe nach; [NichtDa] heißt: nichts gestartet, das nächste Ziel ist dran. */
    private suspend fun starteErstes(wahl: List<ProgrammierZiel>, appName: String, auftrag: String, review: Boolean, nummer: String): Ergebnis {
        val uebersprungen = mutableListOf<String>()
        val gespraech = kotlinx.coroutines.currentCoroutineContext()[ProgrammierChat.GespraechKennung]?.id ?: ProgrammierChat.aktivesGespraech.orEmpty()
        for (ziel in wahl) {
            val mitFortschritt = auftrag + "\n\n" + fortschrittsAnweisung(nummer, pc = ziel is PcBruecke)
            val start = try {
                ziel.starte(appName, mitFortschritt, review, nummer)
            } catch (nichtDa: NichtDa) {
                uebersprungen += nichtDa.message.orEmpty()
                continue
            } catch (fehler: Exception) {
                return Ergebnis("Der Auftrag ist nicht gestartet (${ziel.name}): ${fehler.message ?: fehler.javaClass.simpleName}", fehler = true)
            }
            val eintrag = Auftragsbuch.neu(app, nummer, appName, ziel.name, start.link, "Gestartet: ${ziel.name}. ${start.hinweis}", gespraech)
            verteile(eintrag, null)
            return Ergebnis("Der Auftrag für $appName (Nummer $nummer) läuft jetzt: ${ziel.name}. ${start.hinweis} " +
                "Die Sitzung meldet ihre Schritte in dieses Gespräch; eine Benachrichtigung kommt erst am Ende oder bei einer Rückfrage, und programmierauftraege nennt jederzeit den Stand." +
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
        "FORTSCHRITT (AUFTRAG-ID: $nummer): Frank verfolgt diesen Auftrag in Jarvis. Zwischenschritte (geänderte Dateien, Bau, Commit, Push, Installation) " +
            "und Rückfragen meldet diese Sitzung automatisch an Jarvis; stell Rückfragen also ganz normal mit AskUserQuestion, Frank beantwortet sie in Jarvis. " +
            "Melde mit dem Werkzeug programmier_fortschritt des MCP-Servers Jarvis (auftrag_id=$nummer) nur, wenn die App gefunden ist (stand=laeuft), und am " +
            "Ende stand=fertig mit einem Satz, was jetzt neu ist und welche Version, oder stand=fehler mit dem Grund."
    else
        "FORTSCHRITT (AUFTRAG-ID: $nummer): Frank verfolgt diesen Auftrag am Handy. Melde jeden wichtigen Schritt mit dem Werkzeug programmier_fortschritt " +
            "des MCP-Servers Jarvis (auftrag_id=$nummer, ein kurzer Satz auf Deutsch): sofort zu Beginn (stand=laeuft), wenn die App gefunden ist, wenn die " +
            "Änderung programmiert ist, beim Commit oder Pull Request, beim Bau oder der Installation, und am Ende stand=fertig mit einem Satz, was jetzt neu ist " +
            "und welche Version, oder stand=fehler mit dem Grund. Bevor du Frank mit AskUserQuestion etwas fragst, melde stand=frage mit der Frage, damit er eine " +
            "Benachrichtigung bekommt. Ist das Werkzeug nicht verfügbar, arbeite ohne Meldungen weiter."

    /**
     * Eine Meldung des Hooks vom PC (Signal „programmieren“ über den Relay): ins Auftragsbuch, als Benachrichtigung und in
     * das Gespräch, aus dem der Auftrag kam. Mit „frage“ ist es eine Rückfrage, auf die der Hook wartet.
     */
    fun meldungVomServer(daten: JSONObject) {
        if (daten.optString("stand") == "puls") {
            // Lebenszeichen: nur die drehende Anzeige auffrischen, keine Nachricht, keine Benachrichtigung.
            Auftragsbuch.puls(app, daten.optString("auftrag_id"))?.let { aktivitaetMelden(it) }
            return
        }
        val frage = daten.optJSONObject("frage")
        val eintrag = Auftragsbuch.melde(app, daten.optString("auftrag_id"), daten.optString("stand"), daten.optString("schritt").take(4000), "", frage) ?: return
        verteile(eintrag, frage)
    }

    private fun aktivitaetMelden(eintrag: JSONObject) = ProgrammierChat.setze(
        eintrag.optString("gespraech"),
        ProgrammierChat.Aktivitaet(eintrag.optString("app"), eintrag.optBoolean("arbeitet"), eintrag.optLong("aktiv", eintrag.optLong("zeit")), eintrag.optString("schritt")),
    )

    /** Beim Start der App: die drehende Anzeige für Aufträge, die noch arbeiten, aus dem Auftragsbuch herstellen. */
    fun aktivitaetLaden() = Auftragsbuch.juengste(app).forEach { aktivitaetMelden(it) }

    /** Benachrichtigung aktualisieren und die Meldung ins Gespräch des Auftrags schreiben. */
    private fun verteile(eintrag: JSONObject, frage: JSONObject?) {
        aktivitaetMelden(eintrag)
        // Zwischenschritte stehen vollständig im Gespräch (sie kommen über den Tunnel in die App). Eine Benachrichtigung gibt es
        // nur, wenn Frank gebraucht wird oder der Auftrag zu Ende ist: Rückfrage, fertig (App installiert) oder Problem.
        val letzter = eintrag.optString("letzterStand", eintrag.optString("stand"))
        if (frage != null || letzter == "fertig" || letzter == "fehler") benachrichtige(eintrag)
        val gespraech = eintrag.optString("gespraech")
        if (gespraech.isEmpty()) return
        val appName = eintrag.optString("app")
        val schritt = eintrag.optString("schritt")
        if (frage != null) {
            val fragen = frage.optJSONArray("fragen") ?: JSONArray()
            val kurz = (0 until fragen.length()).joinToString(" / ") { fragen.optJSONObject(it)?.optString("question").orEmpty() }.trim()
            val kontext = JSONObject().put("frage_id", frage.optString("frage_id")).put("auftrag", eintrag.optString("id")).put("app", appName)
                .put("fragen", fragen).put("frei", frage.optBoolean("frei")).put("beantwortet", false)
            ProgrammierChat.poste(app, gespraech, "❓ Rückfrage zu $appName: " + (if (frage.optBoolean("frei")) schritt.removePrefix("❓").trim() else kurz), "rueckfrage", kontext.toString())
            return
        }
        val zeichen = when (eintrag.optString("letzterStand")) { "fertig" -> "✅"; "fehler" -> "❌"; "info" -> ""; else -> "⚙️" }
        val mitZeichen = if (zeichen.isEmpty() || schritt.firstOrNull()?.isLetterOrDigit() == false) schritt else "$zeichen $schritt"
        ProgrammierChat.poste(app, gespraech, "$appName · $mitZeichen", "programmieren")
    }

    /**
     * Gibt Franks Antwort auf eine Rückfrage an den wartenden Hook weiter. [antworten] in der Reihenfolge der Fragen (label der
     * Option, bei Mehrfachauswahl mit Komma), [text] als freie Antwort. Liefert einen Satz für Frank; Fehler als Ausnahme.
     */
    suspend fun beantworte(frageId: String, antworten: List<String>, text: String): String = withContext(Dispatchers.IO) {
        if (frageId.isEmpty()) throw IllegalArgumentException("Die frage_id fehlt.")
        if (antworten.none { it.isNotEmpty() } && text.isEmpty()) throw IllegalArgumentException("Die Antwort ist leer.")
        val offen = Auftragsbuch.offeneFrage(app, frageId)
        val fragen = offen?.second?.optJSONArray("fragen") ?: JSONArray()
        val zuordnung = JSONObject()
        antworten.forEachIndexed { i, antwort ->
            val frage = fragen.optJSONObject(i)?.optString("question").orEmpty()
            if (frage.isNotEmpty() && antwort.isNotEmpty()) zuordnung.put(frage, antwort)
        }
        val rumpf = JSONObject().put("frage_id", frageId).put("antworten", zuordnung).put("text", text.ifEmpty { if (zuordnung.length() == 0) antworten.joinToString("; ") else "" })
        val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/antwort").header("X-Jarvis-Token", e.serverToken)
            .post(rumpf.toString().toRequestBody("application/json".toMediaType())).build()
        val (code, roh) = HTTP.newCall(anfrage).execute().use { it.code to it.body?.string().orEmpty() }
        if (code !in 200..299) throw IllegalStateException(runCatching { JSONObject(roh).optString("grund") }.getOrNull()?.takeIf { it.isNotEmpty() } ?: "Der Jarvis-Server hat die Antwort nicht angenommen (HTTP $code).")
        val antwortText = if (zuordnung.length() > 0) antworten.filter { it.isNotEmpty() }.joinToString("; ") + (if (text.isNotEmpty()) " ($text)" else "") else rumpf.optString("text")
        Auftragsbuch.frageErledigt(app, frageId)
        offen?.first?.optString("gespraech")?.takeIf { it.isNotEmpty() }?.let { ProgrammierChat.alsBeantwortet(app, it, frageId, antwortText) }
        "Die Antwort „$antwortText“ ist bei der Sitzung angekommen; sie arbeitet jetzt weiter."
    }

    /**
     * Das Gespräch eines noch laufenden Auftrags (läuft oder wartet auf eine Antwort, letzte Meldung höchstens zwei Stunden
     * alt), oder null. Die App öffnet es beim Start: Nach einem Update von Jarvis selbst (die Sitzung installiert die neue
     * Version, Android beendet dabei die App) geht es dort weiter, wo Frank war.
     */
    fun laufendesGespraech(): String? = Auftragsbuch.laufendesGespraech(app)

    /** Eine Benachrichtigung je Auftrag, nur bei Rückfrage, Fertig und Problem; ein Tipp öffnet die Sitzung oder die Claude-App. */
    private fun benachrichtige(eintrag: JSONObject) {
        runCatching {
            JarvisDienst.kanalAnlegen(app)
            val link = eintrag.optString("link")
            val oeffnen = if (link.startsWith("https://")) Intent(Intent.ACTION_VIEW, Uri.parse(link))
                else app.packageManager.getLaunchIntentForPackage(CLAUDE_APP) ?: app.packageManager.getLaunchIntentForPackage(app.packageName)
            val stand = eintrag.optString("stand")
            val appName = eintrag.optString("app")
            val titel = when (stand) {
                "frage" -> "Rückfrage: $appName"
                "fertig" -> "Fertig programmiert: $appName"
                "fehler" -> "Problem beim Programmieren: $appName"
                else -> "Jarvis programmiert: $appName"
            }
            val text = eintrag.optString("schritt")
            val nummer = 300_000 + (eintrag.optString("id").hashCode() and 0xFFFF)
            app.getSystemService(NotificationManager::class.java).notify(
                nummer,
                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis)
                    .setContentTitle(titel)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n${eintrag.optString("ziel")} · Auftrag ${eintrag.optString("id")}"))
                    // Zwischenschritte still aktualisieren; Rückfrage, Fertig und Problem melden sich hörbar.
                    .setOnlyAlertOnce(stand == "laeuft")
                    .setAutoCancel(stand == "fertig")
                    .apply { oeffnen?.let { setContentIntent(PendingIntent.getActivity(app, nummer, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)) } }
                    .build(),
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
            val gekuerzt = JSONArray().apply { for (i in maxOf(0, liste.length() - 20) until liste.length()) put(liste.get(i)) }
            context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).edit().putString(SCHLUESSEL, gekuerzt.toString()).apply()
        }

        @Synchronized
        fun neu(context: Context, id: String, app: String, ziel: String, link: String, schritt: String, gespraech: String): JSONObject {
            val jetzt = System.currentTimeMillis()
            val eintrag = JSONObject().put("id", id).put("app", app).put("ziel", ziel).put("link", link).put("start", jetzt).put("gespraech", gespraech)
                .put("stand", "laeuft").put("schritt", schritt).put("zeit", jetzt).put("aktiv", jetzt).put("arbeitet", true)
                .put("schritte", JSONArray().put(JSONObject().put("zeit", jetzt).put("stand", "laeuft").put("text", schritt)))
            schreib(context, lies(context).put(eintrag))
            return eintrag
        }

        @Synchronized
        fun melde(context: Context, id: String, stand: String, schritt: String, link: String, frage: JSONObject? = null): JSONObject? {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return null
            val jetzt = System.currentTimeMillis()
            val gueltig = if (stand in listOf("laeuft", "frage", "fertig", "fehler", "info")) stand else "laeuft"
            // „info“ (Zwischenstand am Zugende) ändert den Stand nicht: Ein fertiger Auftrag bleibt fertig.
            if (gueltig != "info") eintrag.put("stand", gueltig)
            eintrag.put("letzterStand", gueltig).put("schritt", schritt).put("zeit", jetzt).put("aktiv", jetzt)
                // Arbeitet die Sitzung? Nicht nach dem Zugende (info), am Ende und solange sie auf Frank wartet.
                .put("arbeitet", gueltig == "laeuft")
            if (frage != null) eintrag.put("offeneFrage", frage)
            if (link.startsWith("https://") && eintrag.optString("link").isEmpty()) eintrag.put("link", link)
            val schritte = eintrag.optJSONArray("schritte") ?: JSONArray().also { eintrag.put("schritte", it) }
            schritte.put(JSONObject().put("zeit", jetzt).put("stand", gueltig).put("text", schritt))
            while (schritte.length() > 30) schritte.remove(0)
            schreib(context, liste)
            return eintrag
        }

        fun laufendesGespraech(context: Context): String? {
            val liste = lies(context)
            val grenze = System.currentTimeMillis() - 2 * 3_600_000L
            for (i in liste.length() - 1 downTo 0) {
                val e = liste.getJSONObject(i)
                if (e.optString("gespraech").isNotEmpty() && e.optString("stand") in listOf("laeuft", "frage") && e.optLong("zeit") >= grenze) return e.optString("gespraech")
            }
            return null
        }

        /** Lebenszeichen der Sitzung (Puls vom Hook): Sie arbeitet. */
        @Synchronized
        fun puls(context: Context, id: String): JSONObject? {
            val liste = lies(context)
            val eintrag = (0 until liste.length()).map { liste.getJSONObject(it) }.lastOrNull { it.optString("id").equals(id, ignoreCase = true) } ?: return null
            eintrag.put("aktiv", System.currentTimeMillis()).put("arbeitet", true)
            schreib(context, liste)
            return eintrag
        }

        /** Aufträge der letzten zwei Stunden mit Gespräch. */
        fun juengste(context: Context): List<JSONObject> {
            val liste = lies(context)
            val grenze = System.currentTimeMillis() - 2 * 3_600_000L
            return (0 until liste.length()).map { liste.getJSONObject(it) }.filter { it.optString("gespraech").isNotEmpty() && it.optLong("zeit") >= grenze }
        }

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

        fun uebersicht(context: Context, anzahl: Int): String {
            val liste = lies(context)
            if (liste.length() == 0) return "Es gibt noch keine Programmieraufträge."
            val jetzt = System.currentTimeMillis()
            return buildString {
                append("Programmieraufträge, neueste zuerst:\n")
                (liste.length() - 1 downTo maxOf(0, liste.length() - anzahl)).forEach { i ->
                    val e = liste.getJSONObject(i)
                    val standText = when (e.optString("stand")) { "frage" -> "wartet auf Franks Antwort"; "fertig" -> "fertig"; "fehler" -> "Problem"; else -> "läuft" }
                    val minuten = (jetzt - e.optLong("zeit")) / 60_000
                    append("- ").append(e.optString("app")).append(" (Auftrag ").append(e.optString("id")).append(", ").append(e.optString("ziel"))
                        .append(", gestartet ").append(ZEIT.format(Instant.ofEpochMilli(e.optLong("start")).atZone(ZoneId.systemDefault()))).append("): ")
                        .append(standText).append(". Zuletzt vor ").append(minuten).append(" Minuten: ").append(e.optString("schritt"))
                    e.optString("link").takeIf { it.isNotEmpty() }?.let { append(" Link: ").append(it) }
                    if (e.optString("stand") == "laeuft" && minuten >= 30) append(" (seit über einer halben Stunde keine Meldung; Sitzung über den Link prüfen)")
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
        override val name = "Claude Code auf deinem PC"
        override fun bereit() = e.serverToken.isNotBlank()
        override fun fehlt() = "Jarvis ist nicht mit seinem Server verbunden."

        override suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String): Start = withContext(Dispatchers.IO) {
            val rumpf = JSONObject().put("app", app).put("auftrag", auftrag.take(60_000)).put("review", review).put("nummer", nummer)
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
        override val name = "Claude Code in der Cloud"
        override fun bereit() = e.routineUrl.startsWith("https://") && e.routineToken.isNotBlank()
        override fun fehlt() = "Die Claude-Code-Routine ist in Jarvis nicht eingerichtet: Auslöse-Adresse und Token unter Einstellungen → Konten → Programmieren in der Cloud eintragen."

        override suspend fun starte(app: String, auftrag: String, review: Boolean, nummer: String): Start = withContext(Dispatchers.IO) {
            val text = buildString {
                append("APP: ").append(app).append('\n')
                append("CODEX-REVIEW: ").append(if (review) "ja" else "nein").append('\n')
                append("AUFTRAG:\n").append(auftrag)
            }.take(60_000)
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

    private companion object {
        const val CLAUDE_APP = "com.anthropic.claude"
        val STATUS_HTTP: OkHttpClient = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
        val HTTP: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    }
}
