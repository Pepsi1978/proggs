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
import org.json.JSONObject

/**
 * Programmieraufträge an Franks Apps: Jarvis formuliert den Auftrag und gibt ihn an ein Programmier-Ziel ab, das im
 * Repo programmiert und die App über UpdateStation ausliefert. Jarvis selbst ändert nie Code (siehe [RepoFaehigkeit]).
 *
 * Zwei Ziele, in dieser Reihenfolge: Franks PC (Claude Code mit Remote Control, schnell, Installation per WLAN-adb),
 * solange dort die PC-Brücke läuft (Jarvis/werkzeuge/pc-bruecke), sonst die Claude-Code-Routine in der Cloud.
 * Beides läuft über Franks Claude-Abo, nicht über bezahlte API-Aufrufe.
 */
class ProgrammierFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "programmieren"
    override val name = "Programmieren"
    override val beschreibung = "Programmieraufträge an Franks Apps an Claude Code übergeben; die fertige App kommt über UpdateStation aufs Handy."
    override val hinweise =
        "Sagt Frank „programmiere“, „bau in App … ein“, „ändere in … dass …“ oder „mach in … einen Knopf/Filter/…“, rufst du programmieren auf. " +
            "Schreib den Auftrag vollständig aus, mit allem, was Frank im Gespräch gesagt hat: welche App, was genau anders werden soll, wie es aussehen und sich verhalten soll, " +
            "woran man erkennt, dass es fertig ist. Ist unklar, welche App gemeint ist, frag Frank kurz, bevor du startest. Programmiere nie selbst im Gespräch."

    private val app = context.applicationContext
    private val e get() = Einstellungen.get(app)

    /** In dieser Reihenfolge versucht; das erste bereite Ziel bekommt den Auftrag. */
    private val ziele: List<ProgrammierZiel> by lazy { listOf(PcBruecke(), CloudRoutine()) }

    override fun stoerung(): String? = if (ziele.none { it.bereit() }) ziele.last().fehlt() else null

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
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
                    else -> starteErstes(wahl, appName, auftrag, a.optBoolean("review"))
                }
            },
        )
    }

    /** Versucht die Ziele der Reihe nach; [NichtDa] heißt: nichts gestartet, das nächste Ziel ist dran. */
    private suspend fun starteErstes(wahl: List<ProgrammierZiel>, appName: String, auftrag: String, review: Boolean): Ergebnis {
        val uebersprungen = mutableListOf<String>()
        for (ziel in wahl) {
            val start = try {
                ziel.starte(appName, auftrag, review)
            } catch (nichtDa: NichtDa) {
                uebersprungen += nichtDa.message.orEmpty()
                continue
            } catch (fehler: Exception) {
                return Ergebnis("Der Auftrag ist nicht gestartet (${ziel.name}): ${fehler.message ?: fehler.javaClass.simpleName}", fehler = true)
            }
            melde(appName, ziel, start)
            return Ergebnis("Der Auftrag für $appName läuft jetzt: ${ziel.name}. ${start.hinweis}" +
                (if (start.link.isNotEmpty()) " Link zur Sitzung: ${start.link}" else "") +
                (if (uebersprungen.isEmpty()) "" else " (${uebersprungen.joinToString(" ")})"))
        }
        return Ergebnis("Der Auftrag ist nicht gestartet: ${uebersprungen.joinToString(" ")}", fehler = true)
    }

    /** Benachrichtigung; ein Tipp öffnet die Sitzung (Link) oder die Claude-App. */
    private fun melde(appName: String, ziel: ProgrammierZiel, start: Start) {
        runCatching {
            JarvisDienst.kanalAnlegen(app)
            val oeffnen = if (start.link.isNotEmpty()) Intent(Intent.ACTION_VIEW, Uri.parse(start.link))
                else app.packageManager.getLaunchIntentForPackage(CLAUDE_APP) ?: app.packageManager.getLaunchIntentForPackage(app.packageName)
            app.getSystemService(NotificationManager::class.java).notify(
                (System.currentTimeMillis() % 100_000).toInt() + 300_000,
                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis)
                    .setContentTitle("Jarvis programmiert: $appName")
                    .setContentText("${ziel.name}. ${start.hinweis}")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("${ziel.name}. ${start.hinweis}"))
                    .setAutoCancel(true)
                    .apply { oeffnen?.let { setContentIntent(PendingIntent.getActivity(app, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)) } }
                    .build(),
            )
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
        suspend fun starte(app: String, auftrag: String, review: Boolean): Start
    }

    /**
     * Franks PC über den Jarvis-Relay: Die PC-Brücke holt den Auftrag ab und öffnet eine Claude-Code-Sitzung mit Remote
     * Control (in der Claude-App unter Code zu sehen). 409 vom Relay = Brücke fragt gerade nicht ab, der PC ist aus.
     */
    private inner class PcBruecke : ProgrammierZiel {
        override val name = "Claude Code auf deinem PC"
        override fun bereit() = e.serverToken.isNotBlank()
        override fun fehlt() = "Jarvis ist nicht mit seinem Server verbunden."

        override suspend fun starte(app: String, auftrag: String, review: Boolean): Start = withContext(Dispatchers.IO) {
            val rumpf = JSONObject().put("app", app).put("auftrag", auftrag.take(60_000)).put("review", review)
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

        override suspend fun starte(app: String, auftrag: String, review: Boolean): Start = withContext(Dispatchers.IO) {
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
        val HTTP: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    }
}
