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
 * Heute gibt es genau ein Ziel, die Claude-Code-Routine in der Cloud. Ein zweites Ziel (Claude Code am PC, schneller,
 * Installation per WLAN-adb) kommt später als weitere [ProgrammierZiel]-Implementierung vor die Cloud in [ziele].
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
    private val ziele: List<ProgrammierZiel> by lazy { listOf(CloudRoutine()) }

    override fun stoerung(): String? = if (ziele.none { it.bereit() }) ziele.first().fehlt() else null

    override val werkzeuge: List<Werkzeug> by lazy {
        listOf(
            Werkzeug(
                name = "programmieren",
                titel = "Programmierauftrag starten",
                beschreibung = "Jarvis: programmiert etwas in einer von Franks Apps („Jarvis, programmiere in Geniale Aufgaben einen Filter für …“, „bau in den Wecker ein, dass …“, " +
                    "„ändere in Jarvis …“). Der Auftrag geht an Claude Code in der Cloud: Es findet die App in Franks Repo, programmiert die Änderung, und die neue Version kommt nach " +
                    "etwa zehn bis zwanzig Minuten über UpdateStation aufs Handy. Dieser Aufruf kehrt sofort zurück. Formuliere den Auftrag selbst vollständig aus, mit allem Wissen aus dem " +
                    "Gespräch; die Cloud-Sitzung kennt nur diesen Text. Ein fertiger Auftrag vom Agenten Code-Analyse oder aus repo_lesen kann unverändert hinein. " +
                    "Ist unklar, welche App gemeint ist, frag Frank vorher.",
                schema = schema(
                    "app" to text("Name der App, wie Frank sie nennt, zum Beispiel „Geniale Aufgaben“, „Genialer Wecker“, „Jarvis“; gern mit Ordner im Repo, wenn bekannt."),
                    "auftrag" to text("Der vollständige Programmierauftrag: Ziel, was genau anders werden soll (Bildschirm, Knopf, Verhalten, Texte), Randbedingungen, was Frank wichtig ist, woran man erkennt, dass es fertig ist."),
                    "review" to schalter("true = vor der Auslieferung ein Codex-Review laufen lassen (dauert länger). Vorgabe false."),
                    pflicht = listOf("app", "auftrag"),
                ),
                nurLesen = false,
            ) { a ->
                val appName = a.optString("app").trim()
                val auftrag = a.optString("auftrag").trim()
                val ziel = ziele.firstOrNull { it.bereit() }
                when {
                    appName.isEmpty() -> Ergebnis("Welche App ist gemeint? Bitte Frank fragen.", fehler = true)
                    auftrag.length < 20 -> Ergebnis("Der Auftrag ist zu knapp. Formuliere ihn vollständig aus.", fehler = true)
                    ziel == null -> Ergebnis(ziele.first().fehlt(), fehler = true)
                    else -> runCatching { ziel.starte(appName, auftrag, a.optBoolean("review")) }.fold(
                        onSuccess = { start ->
                            melde(appName, start)
                            Ergebnis("Der Auftrag für $appName läuft jetzt (${ziel.name}). Meist dauert es zehn bis zwanzig Minuten, dann meldet UpdateStation die neue Version. " +
                                "Frank hat eine Benachrichtigung mit dem Link zur Sitzung bekommen" + (if (start.link.isNotEmpty()) ": ${start.link}" else "."))
                        },
                        onFailure = { Ergebnis("Der Auftrag ist nicht gestartet: ${it.message ?: it.javaClass.simpleName}", fehler = true) },
                    )
                }
            },
        )
    }

    /** Benachrichtigung; ein Tipp öffnet die Sitzung (Claude-App oder Browser). */
    private fun melde(appName: String, start: Start) {
        runCatching {
            JarvisDienst.kanalAnlegen(app)
            val oeffnen = if (start.link.isNotEmpty()) Intent(Intent.ACTION_VIEW, Uri.parse(start.link)) else app.packageManager.getLaunchIntentForPackage(app.packageName)
            app.getSystemService(NotificationManager::class.java).notify(
                (System.currentTimeMillis() % 100_000).toInt() + 300_000,
                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis)
                    .setContentTitle("Jarvis programmiert: $appName")
                    .setContentText("Läuft in der Cloud. Tippen öffnet die Sitzung; UpdateStation meldet die neue Version.")
                    .setAutoCancel(true)
                    .apply { oeffnen?.let { setContentIntent(PendingIntent.getActivity(app, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)) } }
                    .build(),
            )
        }
    }

    /** Ergebnis eines Starts: Link zur laufenden Sitzung (leer, wenn das Ziel keinen liefert). */
    data class Start(val link: String)

    /** Wohin ein Programmierauftrag gehen kann. */
    interface ProgrammierZiel {
        val name: String
        fun bereit(): Boolean
        /** Ein Satz, was fehlt, solange das Ziel nicht bereit ist. */
        fun fehlt(): String
        suspend fun starte(app: String, auftrag: String, review: Boolean): Start
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
                Start(runCatching { JSONObject(roh).optString("claude_code_session_url") }.getOrDefault(""))
            }
        }
    }

    private companion object {
        val HTTP: OkHttpClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    }
}
