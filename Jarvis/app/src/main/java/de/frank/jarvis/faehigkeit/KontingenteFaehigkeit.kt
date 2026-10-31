package de.frank.jarvis.faehigkeit

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import de.frank.jarvis.MainActivity
import de.frank.jarvis.R
import de.frank.jarvis.auth.CodexAuthManager
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.meldung.Meldungen
import de.frank.jarvis.meldung.Meldungsarten
import de.frank.jarvis.observability.Logbuch
import de.frank.jarvis.speech.GespraechsFokus
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Nutzungslimits der KI-Abos: Claude, ChatGPT/Codex, Kimi und Gemini, jedes Limit mit Rest, Zurücksetzen (deutsche Zeit)
 * und Hochrechnung des Tempos. Die Anbieter haben dafür keine offiziellen Schnittstellen:
 * - Claude: `claude.ai/api/organizations/<org>/usage`, aus der Seite heraus (eigene Anmeldung, die Bot-Abwehr lässt nur einen Browser durch).
 * - Codex: `chatgpt.com/backend-api/wham/usage` mit der ChatGPT-Anmeldung von Jarvis.
 * - Kimi: Text der Seite `kimi.ai/settings/subscription` in einem unsichtbaren WebView.
 * - Gemini: Limits der Antigravity-CLI. `agy -p /usage` läuft nur am Rechner: Server und Mac melden die Werte alle drei Stunden über `kontingente_melden`.
 * Ändert ein Anbieter etwas, steht bei ihm ein Hinweis und der letzte Stand bleibt; die anderen Karten bleiben unberührt.
 */
class KontingenteFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "kontingente"
    override val name = "Kontingente"
    override val beschreibung = "Nutzungslimits der KI-Abos (Claude, ChatGPT/Codex, Kimi, Gemini) mit Zurücksetzen und Tempo-Hochrechnung."
    override val hinweise =
        "Fragt Frank nach seinen Limits oder Kontingenten bei Claude, ChatGPT/Codex, Kimi oder Gemini, nutze kontingente_lesen. Steht dort ZU SCHNELL, weise ihn von dir aus darauf hin, wenn es zum Gespräch passt."
    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "kontingente_lesen",
            titel = "Kontingente lesen",
            beschreibung = "Jarvis: Franks aktuelle Nutzungslimits bei Claude, ChatGPT/Codex, Kimi und Gemini, je nach Abo über 5 Stunden, eine Woche oder einen Monat: " +
                "genutzt, übrig, wann zurückgesetzt wird (deutsche Zeit) und ob das Limit beim jetzigen Tempo vorher aufgebraucht wäre. Holt höchstens alle 15 Minuten frisch.",
            schema = schema(
                "frisch" to schalter("true = jetzt neu bei den Anbietern abfragen statt des Stands der letzten 15 Minuten."),
                "json" to schalter("true = Ausgabe als JSON für die Mac-Oberfläche."),
            ),
            nurLesen = true,
        ) { a ->
            // Vor der Frist des Relays antworten: lieber der letzte Stand als ein Fehler, während das Handy noch arbeitet.
            val s = withTimeoutOrNull(WERKZEUG_FRIST_MS) { stand(context, erzwingen = a.optBoolean("frisch")) } ?: gespeichert(context.applicationContext) ?: Stand(emptyList(), 0L)
            Ergebnis(if (a.optBoolean("json")) s.alsJson().toString() else s.text(), fehler = s.anbieter.all { it.limits.isEmpty() })
        },
        Werkzeug(
            name = "kontingente_melden",
            titel = "Antigravity-Limits melden",
            beschreibung = "Technisch, nur für die Rechner-Brücken: übergibt die Limits aus `agy -p /usage`. Nicht im Gespräch benutzen; zum Lesen dient kontingente_lesen.",
            schema = schema(
                "quelle" to text("Woher die Werte kommen.", listOf("server", "mac")),
                "limits" to text("JSON-Liste mit name, genutzt (Prozent), fenster (Sekunden) und reset (Sekunden seit 1970)."),
                pflicht = listOf("quelle", "limits"),
            ),
            nurLesen = false,
        ) { a -> Ergebnis(antigravityMelden(context, a.optString("quelle"), a.optString("limits"))) },
    )

    /** Ein Limit. [genutzt] in Prozent, wie zuletzt gelesen; [reset] in Sekunden seit 1970 (0 = beginnt erst mit der nächsten Nutzung). */
    class Limit(val name: String, val genutzt: Double, val fensterSek: Long, val reset: Long) {
        fun abgelaufen(jetzt: Long) = reset in 1..jetzt
        /** Ist das Fenster seit der letzten Abfrage abgelaufen, gilt das Limit wieder als frei. */
        fun genutztJetzt(jetzt: Long): Double = if (abgelaufen(jetzt)) 0.0 else genutzt
        fun uebrigJetzt(jetzt: Long): Double = (100.0 - genutztJetzt(jetzt)).coerceAtLeast(0.0)
        private fun vergangen(jetzt: Long) = fensterSek - (reset - jetzt)
        private fun rechenbar(jetzt: Long) = reset > jetzt && fensterSek > 0 && genutzt > 0 && vergangen(jetzt) >= fensterSek / 10

        /** Prozent, bei denen das Limit beim bisherigen Tempo am Ende des Fensters stünde; null = noch zu früh. */
        fun hochrechnung(jetzt: Long): Double? = if (rechenbar(jetzt)) genutzt * fensterSek / vergangen(jetzt) else null

        /** Wann das Limit beim bisherigen Tempo leer wäre; null, wenn das nicht vor dem Zurücksetzen passiert. */
        fun leerAm(jetzt: Long): Long? = if (zuSchnell(jetzt)) reset - fensterSek + (vergangen(jetzt) * 100 / genutzt).toLong() else null

        fun zuSchnell(jetzt: Long): Boolean = genutzt < 100 && (hochrechnung(jetzt) ?: 0.0) > 100.0

        /** Ganzer Satzteil: „Zurückgesetzt: heute, 22:01 Uhr“. */
        fun resetSatz(jetzt: Long): String = when {
            reset <= 0 -> "Das Zeitfenster beginnt mit der nächsten Nutzung"
            abgelaufen(jetzt) -> "Inzwischen zurückgesetzt"
            else -> "Zurückgesetzt: " + zeitText(reset)
        }

        fun tempoText(jetzt: Long): String {
            if (reset <= 0 || abgelaufen(jetzt)) return ""
            if (genutzt >= 100) return "Aufgebraucht bis zum Zurücksetzen."
            val leer = leerAm(jetzt)
            if (leer != null) return "Bei diesem Tempo wäre es ${zeitText(leer)} leer, ${dauerText(reset - leer)} vor dem Zurücksetzen."
            val h = hochrechnung(jetzt) ?: return "Für eine Hochrechnung ist es noch zu früh."
            return "Tempo passt: hochgerechnet etwa ${h.toInt()} % bis zum Zurücksetzen."
        }

        fun json() = JSONObject().put("name", name).put("genutzt", genutzt).put("fenster", fensterSek).put("reset", reset)
    }

    /** Ein Anbieter. [anmeldenNoetig] = nur eine neue Anmeldung hilft; [hinweis] = warum der Stand fehlt, unvollständig oder alt ist. */
    class Anbieter(val id: String, val titel: String, val konto: String, val limits: List<Limit>, val hinweis: String?, val anmeldenNoetig: Boolean, val geholt: Long) {
        fun standText(): String = if (geholt <= 0) "" else "Stand " + STAND.format(Instant.ofEpochMilli(geholt).atZone(BERLIN))
        /** Ohne E-Mail-Adresse: Dieser Teil geht auch an die Modelle. */
        val tarif: String get() = konto.split(" · ").lastOrNull { !it.contains('@') }.orEmpty()
    }

    class Stand(val anbieter: List<Anbieter>, val geholt: Long) {
        fun von(id: String) = anbieter.firstOrNull { it.id == id }

        /** Für Jarvis' Gedächtnis und die Modelle. */
        fun text(): String = buildString {
            val jetzt = sekunden()
            if (anbieter.isEmpty()) { append("Noch kein Stand: Der erste Abruf läuft erst nach dem Gespräch."); return@buildString }
            append("Zeiten in deutscher Zeit.")
            anbieter.forEach { a ->
                append("\n").append(a.titel).append(if (a.tarif.isNotEmpty()) " (${a.tarif})" else "").append(": ")
                if (a.limits.isEmpty()) { append(a.hinweis ?: "Noch nicht abgefragt."); return@forEach }
                append(a.standText()).append('.')
                a.limits.forEach { l ->
                    append("\n- ").append(l.name).append(": ").append(prozent(l.genutztJetzt(jetzt))).append(" % genutzt, ").append(prozent(l.uebrigJetzt(jetzt)))
                        .append(" % übrig. ").append(l.resetSatz(jetzt)).append('.')
                    l.tempoText(jetzt).takeIf { it.isNotEmpty() }?.let { append(if (l.zuSchnell(jetzt)) " ZU SCHNELL: " else " ").append(it) }
                }
                a.hinweis?.let { append("\n  Hinweis: ").append(it) }
            }
        }

        fun alsJson(): JSONObject {
            val jetzt = sekunden()
            return JSONObject().put("anbieter", JSONArray(anbieter.map { a ->
                JSONObject().put("id", a.id).put("titel", a.titel).put("konto", a.konto).put("hinweis", a.hinweis ?: "").put("anmelden", a.anmeldenNoetig).put("stand", a.standText())
                    .put("limits", JSONArray(a.limits.map { l ->
                        JSONObject().put("name", l.name).put("genutzt", l.genutztJetzt(jetzt)).put("uebrig", l.uebrigJetzt(jetzt)).put("resetSatz", l.resetSatz(jetzt))
                            .put("resetAm", if (l.reset > 0) TAG.format(Instant.ofEpochSecond(l.reset).atZone(BERLIN)) else "").put("resetSek", l.reset)
                            .put("tempo", l.tempoText(jetzt)).put("zuSchnell", l.zuSchnell(jetzt))
                    }))
            }))
        }
    }

    private class NichtAngemeldet(meldung: String) : IllegalStateException(meldung)

    companion object {
        /** Knapp unter dem 15-Minuten-Takt des Wächters: Sein nächster Abruf findet den Stand dann sicher fällig. */
        private const val FRISCH_MS = 14 * 60_000L
        /** Nach einer Störung früher neu versuchen. Abgemeldete Anbieter zählen nicht, dort hilft Warten nicht. */
        private const val FRISCH_NACH_FEHLER_MS = 3 * 60_000L
        /** Abgemeldete Anbieter nur selten neu laden: Jeder Versuch kostet ein Browserfenster. */
        private const val ABGEMELDET_MS = 6 * 3_600_000L
        private const val ANBIETER_FRIST_MS = 50_000L
        /** Der Server vor dem Relay bricht nach 90 Sekunden ab. */
        private const val WERKZEUG_FRIST_MS = 75_000L
        /** So lange darf eine Seite ihre Anmeldung nachladen, bevor ein „Anmelden“ darauf zählt. */
        private const val ANMELDEN_ERST_NACH_MS = 12_000L
        private const val STUNDEN5 = 5 * 3600L
        private const val WOCHE = 7 * 86400L
        /** Ab hier warnt Jarvis: Darunter sagt eine Hochrechnung am Anfang eines Fensters zu wenig. */
        private const val WARNEN_AB = 30.0
        private const val CLAUDE = "https://claude.ai"
        const val CLAUDE_LOGIN = "$CLAUDE/login"
        const val KIMI_SEITE = "https://www.kimi.ai/settings/subscription"
        private const val GEMINI_TITEL = "Gemini / Antigravity (Google)"
        /** Zwei ausgefallene Meldungen im Drei-Stunden-Takt plus Spielraum: Ab hier sagt die Karte, dass der Stand alt ist. */
        private const val ANTIGRAVITY_ALT_MS = 7 * 3_600_000L
        /** Der Server läuft immer und ist die Hauptquelle. Der Mac zählt nur, wenn der Server einen Takt und mehr ausgelassen hat. */
        private const val SERVER_VORRANG_MS = 4 * 3_600_000L
        val BERLIN: ZoneId = ZoneId.of("Europe/Berlin")
        private val STAND = DateTimeFormatter.ofPattern("dd.MM., HH:mm 'Uhr'", Locale.GERMAN)
        private val UHR = DateTimeFormatter.ofPattern("HH:mm 'Uhr'", Locale.GERMAN)
        private val TAG = DateTimeFormatter.ofPattern("EEEE, dd.MM., HH:mm 'Uhr'", Locale.GERMAN)
        private val sperre = Mutex()
        private val letzterVersuch = ConcurrentHashMap<String, Long>()
        private val _frisch = MutableStateFlow<Stand?>(null)
        /** Jeder neue Stand, auch aus dem Wächter: Der offene Reiter übernimmt ihn sofort. */
        val frisch: StateFlow<Stand?> = _frisch
        /** Der laufende Abruf kam ausdrücklich von Frank oder einem Modell: Dann bricht ihn ein Gespräch nicht ab. */
        @Volatile private var laufErzwungen = false
        /** Der Anmelde-Browser von claude.ai ist offen: Die gesicherte Sitzung darf ihn nicht vorzeitig „fertig“ machen. */
        @Volatile private var claudeAnmeldungLaeuft = false

        private fun sekunden() = System.currentTimeMillis() / 1000
        private fun datei(context: Context) = File(context.filesDir, "kontingente.json")
        fun prozent(w: Double): String = if (w % 1.0 == 0.0) w.toInt().toString() else String.format(Locale.GERMAN, "%.1f", w)

        /** „heute, 22:01 Uhr“, „morgen, 02:10 Uhr“ oder „Mittwoch, 04.11., 12:00 Uhr“; immer deutsche Zeit. */
        fun zeitText(s: Long): String {
            val z = Instant.ofEpochSecond(s).atZone(BERLIN)
            val heute = LocalDate.now(BERLIN)
            return when (z.toLocalDate()) {
                heute -> "heute, " + UHR.format(z)
                heute.plusDays(1) -> "morgen, " + UHR.format(z)
                else -> TAG.format(z)
            }
        }

        private fun dauerText(s: Long): String = when {
            s >= 2 * 86400 -> "${s / 86400} Tage"
            s >= 2 * 3600 -> "${s / 3600} Stunden"
            else -> "${(s / 60).coerceAtLeast(1)} Minuten"
        }

        fun claudeSitzungImBrowser(): String? = runCatching { CookieManager.getInstance().getCookie(CLAUDE) }.getOrNull().orEmpty()
            .split(';').map { it.trim() }.firstOrNull { it.startsWith("sessionKey=") }?.substringAfter('=')?.takeIf { it.isNotEmpty() }

        /** Vor der Anmeldung bei claude.ai: Eine abgelehnte Sitzung im Browser ließe den Anmelde-Dialog sofort wieder zugehen. Die gesicherte bleibt für einen Abbruch. */
        fun claudeAnmeldungVorbereiten() {
            claudeAnmeldungLaeuft = true
            cookiesLoeschen(CLAUDE)
        }

        fun claudeAnmeldungBeendet() { claudeAnmeldungLaeuft = false }

        private fun cookiesLoeschen(adresse: String) {
            val cm = CookieManager.getInstance()
            val host = adresse.removePrefix("https://")
            runCatching { cm.getCookie(adresse) }.getOrNull().orEmpty().split(';').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }.distinct().forEach { name ->
                listOf("", "; Domain=$host", "; Domain=." + host.removePrefix("www.")).forEach { d ->
                    cm.setCookie(adresse, "$name=; Max-Age=0; Path=/; Secure$d")
                    cm.setCookie(adresse, "$name=; Max-Age=0; Path=/$d")
                }
            }
            cm.flush()
        }

        /** Gesicherte Claude-Sitzung zurück in den Browser, falls sein Cookie abgelaufen ist. */
        private fun claudeSitzung(app: Context): Boolean {
            if (claudeAnmeldungLaeuft) throw IllegalStateException("Die Anmeldung bei claude.ai läuft gerade.")
            val s = SecureSettings.get(app)
            val browser = claudeSitzungImBrowser()
            if (browser != null) { if (browser != s.claudeAiSitzung) s.claudeAiSitzung = browser; return true }
            val gesichert = s.claudeAiSitzung.takeIf { it.isNotEmpty() } ?: return false
            CookieManager.getInstance().setCookie(CLAUDE, "sessionKey=$gesichert; Path=/; Secure; HttpOnly; Max-Age=2592000")
            CookieManager.getInstance().flush()
            return true
        }

        /**
         * Meldet Jarvis bei claude.ai oder Kimi ab und nimmt dessen Zahlen aus dem Stand. Wartet eine laufende Abfrage ab, sonst könnte sie
         * die Sitzung zurückschreiben. Die Console und das Google-Konto bleiben angemeldet.
         */
        suspend fun abmelden(context: Context, id: String) = sperre.withLock {
            val app = context.applicationContext
            withContext(Dispatchers.Main) {
                val adresse = if (id == "claude") CLAUDE else "https://www.kimi.ai"
                cookiesLoeschen(adresse)
                WebStorage.getInstance().deleteOrigin(adresse)
            }
            withContext(Dispatchers.IO) {
                if (id == "claude") SecureSettings.get(app).claudeAiSitzung = ""
                val alt = gespeichert(app) ?: return@withContext
                val neu = Stand(alt.anbieter.map { a -> if (a.id == id) Anbieter(a.id, a.titel, "", emptyList(), "Abgemeldet.", true, 0L) else a }, 0L)
                speichern(app, neu, gewarntLesen(app))
                _frisch.value = neu
            }
        }

        /** Der zuletzt gespeicherte Stand, ohne Netz. */
        fun gespeichert(context: Context): Stand? = runCatching {
            val j = JSONObject(datei(context).readText())
            val liste = j.getJSONArray("anbieter")
            Stand((0 until liste.length()).map { i ->
                val a = liste.getJSONObject(i)
                val l = a.getJSONArray("limits")
                Anbieter(a.getString("id"), a.getString("titel"), a.optString("konto"),
                    (0 until l.length()).map { k -> l.getJSONObject(k).let { Limit(it.getString("name"), it.getDouble("genutzt"), it.getLong("fenster"), it.getLong("reset")) } },
                    a.optString("hinweis").takeIf { it.isNotEmpty() }, a.optBoolean("anmelden"), a.optLong("geholt"))
            }, j.optLong("geholt"))
        }.getOrNull()

        private fun gewarntLesen(context: Context): JSONObject = runCatching { JSONObject(datei(context).readText()).optJSONObject("gewarnt") }.getOrNull() ?: JSONObject()

        /** Über eine Zwischendatei: Bricht der Prozess beim Schreiben ab, bleiben Stand und Warnzustand erhalten. */
        private fun speichern(context: Context, s: Stand, gewarnt: JSONObject) {
            val inhalt = JSONObject().put("geholt", s.geholt).put("gewarnt", gewarnt).put("anbieter", JSONArray(s.anbieter.map { a ->
                JSONObject().put("id", a.id).put("titel", a.titel).put("konto", a.konto).put("hinweis", a.hinweis ?: "").put("anmelden", a.anmeldenNoetig).put("geholt", a.geholt)
                    .put("limits", JSONArray(a.limits.map { it.json() }))
            })).toString()
            val neu = File(context.filesDir, "kontingente.json.neu")
            neu.writeText(inhalt)
            if (!neu.renameTo(datei(context))) { datei(context).writeText(inhalt); neu.delete() }
        }

        /**
         * Der aktuelle Stand: der gespeicherte, wenn er noch frisch ist (außer [erzwingen]), sonst neu abgefragt.
         * Während eines Gesprächs bleibt es beim gespeicherten: Die Stimme geht vor, die unsichtbaren Seiten kosten Rechenzeit.
         */
        suspend fun stand(context: Context, erzwingen: Boolean = false): Stand = withContext(Dispatchers.IO) {
            val app = context.applicationContext
            sperre.withLock {
                val alt = gespeichert(app)
                if (!erzwingen && GespraechsFokus.stilleHinweise) return@withLock alt ?: Stand(emptyList(), 0L)
                if (!erzwingen && alt != null) {
                    // Gemini füllen Server und Mac: Ein neuer Abruf hier ändert an dessen Hinweis nichts.
                    val frist = if (alt.anbieter.any { it.id != "gemini" && it.hinweis != null && !it.anmeldenNoetig }) FRISCH_NACH_FEHLER_MS else FRISCH_MS
                    if (System.currentTimeMillis() - alt.geholt < frist) return@withLock alt
                }
                laufErzwungen = erzwingen
                val beginn = System.currentTimeMillis()
                val abfragen: List<Triple<String, String, suspend () -> Pair<String, List<Limit>>>> = listOf(
                    Triple("claude", "Claude (Anthropic)") { claude(app) },
                    Triple("codex", "ChatGPT / Codex (OpenAI)") { codex(app) },
                    Triple("kimi", "Kimi") { kimi(app) },
                )
                val liste = coroutineScope {
                    abfragen.map { (id, titel, abfrage) -> async { anbieterLesen(id, titel, abfrage, alt?.von(id), erzwingen) } }.awaitAll()
                } + antigravityAnbieter(app)
                val s = Stand(liste, beginn)
                val gewarnt = warnen(app, s)
                runCatching { speichern(app, s, gewarnt) }
                _frisch.value = s
                s
            }
        }

        private suspend fun anbieterLesen(id: String, titel: String, abfrage: suspend () -> Pair<String, List<Limit>>, vorher: Anbieter?, erzwingen: Boolean): Anbieter {
            val jetzt = System.currentTimeMillis()
            if (vorher?.anmeldenNoetig == true && !erzwingen && jetzt - (letzterVersuch[id] ?: 0L) < ABGEMELDET_MS) return vorher
            letzterVersuch[id] = jetzt
            fun fehlschlag(grund: String, abmeldung: Boolean): Anbieter {
                Logbuch.netz("Kontingent $id nicht abrufbar", grund)
                val alte = vorher?.limits.orEmpty()
                return Anbieter(id, titel, vorher?.konto.orEmpty(), alte, if (alte.isNotEmpty()) "Letzter Stand. $grund" else grund, abmeldung, vorher?.geholt ?: 0L)
            }
            return try {
                val (konto, limits) = withTimeout(ANBIETER_FRIST_MS) { abfrage() }
                // Fehlt nur ein Limit, bleibt dessen letzter Stand: Es darf nicht ohne Hinweis verschwinden.
                val fehlen = vorher?.limits.orEmpty().filter { alt -> limits.none { it.name == alt.name } }
                val erwartet = if (id == "kimi") 2 else 1
                val hinweis = if (fehlen.isNotEmpty() || limits.size < erwartet) "Ein Limit ließ sich gerade nicht lesen" + (if (fehlen.isNotEmpty()) ", dort steht der letzte Stand." else ".") else null
                Anbieter(id, titel, konto, limits + fehlen, hinweis, false, System.currentTimeMillis())
            } catch (f: TimeoutCancellationException) {
                fehlschlag("Der Anbieter hat nicht rechtzeitig geantwortet.", false)
            } catch (f: kotlinx.coroutines.CancellationException) {
                throw f
            } catch (f: NichtAngemeldet) {
                fehlschlag(f.message.orEmpty(), true)
            } catch (f: Exception) {
                fehlschlag(f.message ?: "Abfrage gerade nicht möglich.", false)
            }
        }

        /** Meldet jedes Limit, das zu schnell schrumpft, einmal je Fenster. Gilt erst als gemeldet, wenn die Meldung wirklich gezeigt wurde. */
        private fun warnen(app: Context, s: Stand): JSONObject {
            val gewarnt = gewarntLesen(app)
            val jetzt = sekunden()
            s.anbieter.filter { it.hinweis == null }.forEach { a ->
                a.limits.filter { it.zuSchnell(jetzt) && it.genutzt >= WARNEN_AB }.forEach { l ->
                    val schluessel = a.id + "|" + l.name
                    // Die Anbieter nennen das Ende nicht auf die Sekunde gleich: Innerhalb eines Zehntels des Fensters ist es dasselbe.
                    if (Math.abs(gewarnt.optLong(schluessel) - l.reset) > l.fensterSek / 10) {
                        val titel = "${a.titel}: ${l.name} schrumpft zu schnell"
                        val text = "${prozent(l.genutzt)} % genutzt. " + l.tempoText(jetzt) + " " + l.resetSatz(jetzt) + "."
                        val gezeigt = runCatching {
                            Meldungen.zeige(app, Meldungsarten.KONTINGENT, schluessel.hashCode(),
                                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle(titel).setContentText(text)
                                    .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
                                    .setContentIntent(PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)),
                                titel, sprechtext = "${a.titel}: ${l.name}. $text")
                        }.getOrDefault(false)
                        if (gezeigt) { gewarnt.put(schluessel, l.reset); Logbuch.netz(titel, text) }
                    }
                }
            }
            return gewarnt
        }

        // ---------------------------------------------------------------- Anbieter

        /** 200 und die Ablehnungen von claude.ai selbst stehen fest. Alles andere (auch ein 403 der Bot-Abwehr) kann sich noch lösen. */
        private fun claudeEndgueltig(s: String): Boolean {
            val j = runCatching { JSONObject(s) }.getOrNull() ?: return false
            val c = j.optInt("s")
            return c == 200 || c == 401 || (c == 403 && j.optString("b").trimStart().startsWith("{"))
        }

        private suspend fun claude(app: Context): Pair<String, List<Limit>> {
            if (!claudeSitzung(app)) throw NichtAngemeldet("Noch nicht bei claude.ai angemeldet.")
            // Bleibt die Antwort offen (Prüfung der Bot-Abwehr läuft), fragt die Seite nach drei Sekunden noch einmal.
            val js = """(function(){ if(location.host!=='claude.ai') return null;
                if(window.__jk===undefined){ window.__jk=null; (async()=>{
                    const fertig=(s,b)=>{ window.__jk=JSON.stringify({s:s,b:b}); if(s!==200&&s!==401) setTimeout(()=>{window.__jk=undefined},3000); };
                    try {
                        const a=await fetch('/api/organizations',{credentials:'include',headers:{Accept:'application/json'}});
                        if(!a.ok){ fertig(a.status,(await a.text()).slice(0,200)); return; }
                        const o=await a.json(); const org=o.find(x=>(x.capabilities||[]).includes('chat'))||o[0];
                        const r=await fetch('/api/organizations/'+org.uuid+'/usage',{credentials:'include',headers:{Accept:'application/json'}});
                        fertig(r.status,await r.text());
                    } catch(e){ fertig(0,String(e)); } })(); }
                return window.__jk===undefined?null:window.__jk; })()"""
            // Zuerst die leichte Datei; lässt die Bot-Abwehr das nicht zu, die echte Seite, die deren Prüfung löst.
            var antwort: JSONObject? = null
            for ((adresse, frist) in listOf("$CLAUDE/robots.txt" to 8_000L, "$CLAUDE/new" to 30_000L)) {
                val s = seiteLesen(app, adresse, js, frist, ::claudeEndgueltig)
                runCatching { JSONObject(s) }.getOrNull()?.let { antwort = it }
                if (claudeEndgueltig(s)) break
            }
            val code = antwort?.optInt("s") ?: 0
            val text = antwort?.optString("b").orEmpty()
            if (code == 401 || (code == 403 && text.trimStart().startsWith("{"))) throw NichtAngemeldet("claude.ai lehnt die Anmeldung ab ($code). Bitte neu anmelden.")
            if (code != 200) throw IllegalStateException(if (code == 0) "claude.ai antwortet gerade nicht." else "claude.ai antwortet gerade nicht (Fehler $code).")
            val j = JSONObject(text)
            val limits = listOf("five_hour" to ("5-Stunden-Limit" to STUNDEN5), "seven_day" to ("Wochenlimit" to WOCHE),
                "seven_day_opus" to ("Wochenlimit Opus" to WOCHE), "seven_day_sonnet" to ("Wochenlimit Sonnet" to WOCHE)).mapNotNull { (feld, art) ->
                val o = j.optJSONObject(feld) ?: return@mapNotNull null
                val reset = o.optString("resets_at").takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { OffsetDateTime.parse(it).toEpochSecond() }.getOrNull() } ?: 0L
                Limit(art.first, o.optDouble("utilization", 0.0), art.second, reset)
            }
            if (limits.isEmpty()) throw IllegalStateException("claude.ai nennt keine Limits (Schnittstelle geändert?).")
            return "" to limits
        }

        private suspend fun codex(app: Context): Pair<String, List<Limit>> {
            val auth = CodexAuthManager(app)
            if (!auth.isConnected) throw NichtAngemeldet("Noch nicht bei OpenAI angemeldet (Einstellungen → Jarvis).")
            val j = auth.nutzungLesen()
            val rl = j.optJSONObject("rate_limit") ?: throw IllegalStateException("OpenAI nennt keine Limits (Schnittstelle geändert?).")
            val limits = ArrayList<Limit>()
            listOf("primary_window", "secondary_window").mapNotNull { rl.optJSONObject(it) }.forEach { w ->
                val fenster = w.optLong("limit_window_seconds", WOCHE)
                val grund = when { fenster == WOCHE -> "Wochenlimit"; fenster == STUNDEN5 -> "5-Stunden-Limit"; else -> "Limit über ${fenster / 3600} Stunden" }
                // Der Name ist zugleich die Kennung: Zwei gleich lange Fenster dürfen ihn nicht teilen.
                val name = if (limits.any { it.name == grund }) "$grund (2)" else grund
                limits += Limit(name, w.optDouble("used_percent", 0.0), fenster, w.optLong("reset_at"))
            }
            if (limits.isEmpty()) throw IllegalStateException("OpenAI nennt keine Limits (Schnittstelle geändert?).")
            return listOf(j.optString("email"), j.optString("plan_type")).filter { it.isNotBlank() }.joinToString(" · ") to limits
        }

        private const val SEITENTEXT = "(function(){ return document.body ? (document.body.innerText || document.body.textContent || '') : null; })()"
        private val KIMI_GESAMT = Regex("Gesamtnutzung|Total usage|Overall usage", RegexOption.IGNORE_CASE)
        private val KIMI_5H = Regex("Nutzung in 5 Stunden|(?<![\\d.,])5[- ]?(?:hours?|h)\\b", RegexOption.IGNORE_CASE)
        private val KIMI_ANMELDEN = Regex("\\b(Anmelden|Einloggen|Log in|Sign in)\\b")
        private val PROZENT = Regex("(\\d+(?:[.,]\\d+)?)\\s*%")
        private val KIMI_ZEIT = Regex("(?:(\\d{4})-)?(\\d{1,2})-(\\d{1,2})\\s+(\\d{1,2}):(\\d{2})")

        private suspend fun kimi(app: Context): Pair<String, List<Limit>> {
            val beginn = System.currentTimeMillis()
            val text = seiteLesen(app, KIMI_SEITE, SEITENTEXT) {
                kimiLesen(it).size == 2 || (System.currentTimeMillis() - beginn > ANMELDEN_ERST_NACH_MS && kimiLesen(it).isEmpty() && KIMI_ANMELDEN.containsMatchIn(it))
            }
            val limits = kimiLesen(text)
            if (limits.isNotEmpty()) return "" to limits
            throw when {
                text.isBlank() -> IllegalStateException("Die Kimi-Seite hat nicht geladen.")
                KIMI_ANMELDEN.containsMatchIn(text) -> NichtAngemeldet("Noch nicht bei Kimi angemeldet.")
                else -> IllegalStateException("Die Kimi-Seite ließ sich nicht lesen (Seite geändert?).")
            }
        }

        /** „Gesamtnutzung 0.57% … Wird in 2026-11-28 19:08 zurückgesetzt“ und „Nutzung in 5 Stunden Code 6% Wird in 10-31 22:01 zurückgesetzt“. */
        private fun kimiLesen(text: String): List<Limit> {
            val gesamt = KIMI_GESAMT.findAll(text).lastOrNull()?.range?.first ?: return emptyList()
            val stunden = KIMI_5H.findAll(text).firstOrNull { it.range.first > gesamt }?.range?.first ?: text.length
            val monat = kimiLimit(text.substring(gesamt, stunden), "Monatliche Gesamtnutzung", 0L)
            val fuenf = if (stunden < text.length) kimiLimit(text.substring(stunden), "5-Stunden-Limit", STUNDEN5) else null
            return listOfNotNull(fuenf, monat)
        }

        private fun kimiLimit(teil: String, name: String, fenster: Long): Limit? {
            val p = PROZENT.find(teil)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: return null
            val z = KIMI_ZEIT.find(teil)?.groupValues ?: return null
            val heute = LocalDate.now()
            var d = runCatching { LocalDateTime.of(z[1].toIntOrNull() ?: heute.year, z[2].toInt(), z[3].toInt(), z[4].toInt(), z[5].toInt()) }.getOrNull() ?: return null
            if (z[1].isEmpty() && d.toLocalDate().isBefore(heute.minusDays(1))) d = d.plusYears(1)
            val ende = d.atZone(ZoneId.systemDefault())
            // Der Monat ist unterschiedlich lang: Sein Fenster beginnt einen Kalendermonat vor dem Zurücksetzen.
            val f = if (fenster > 0) fenster else ende.toEpochSecond() - ende.minusMonths(1).toEpochSecond()
            return Limit(name, p, f, ende.toEpochSecond())
        }

        private fun antigravityDatei(context: Context) = File(context.filesDir, "kontingente-antigravity.json")

        private fun antigravityLimits(l: JSONArray): List<Limit> = (0 until l.length()).map { i ->
            l.getJSONObject(i).let { Limit(it.getString("name"), it.getDouble("genutzt"), it.getLong("fenster"), it.getLong("reset")) }
        }

        /** Die Gemini-Karte aus der letzten Meldung von Server oder Mac. Ihr Stand ist die Zeit der Meldung, nicht die der Abfrage hier. */
        private fun antigravityAnbieter(app: Context): Anbieter {
            val j = runCatching { JSONObject(antigravityDatei(app).readText()) }.getOrNull()
                ?: return Anbieter("gemini", GEMINI_TITEL, "", emptyList(), "Noch kein Stand: Server und Mac melden die Werte alle drei Stunden.", false, 0L)
            val geholt = j.optLong("geholt")
            val hinweis = if (System.currentTimeMillis() - geholt > ANTIGRAVITY_ALT_MS) "Alter Stand: Server und Mac haben länger nichts gemeldet." else null
            val quelle = if (j.optString("quelle") == "server") "über den Server" else "über den Mac"
            return Anbieter("gemini", GEMINI_TITEL, "Antigravity-CLI $quelle", antigravityLimits(j.getJSONArray("limits")), hinweis, false, geholt)
        }

        /** Nimmt die Werte aus `agy -p /usage` an und zeigt sie sofort. Das sonst gespeicherte Abrufdatum bleibt, damit die anderen Anbieter im Takt bleiben. */
        suspend fun antigravityMelden(context: Context, quelle: String, limitsJson: String): String = withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val limits = JSONArray(limitsJson)
            require(antigravityLimits(limits).isNotEmpty()) { "Keine Limits gemeldet." }
            sperre.withLock {
                val jetzt = System.currentTimeMillis()
                val alt = runCatching { JSONObject(antigravityDatei(app).readText()) }.getOrNull()
                if (quelle != "server" && alt?.optString("quelle") == "server" && jetzt - alt.optLong("geholt") < SERVER_VORRANG_MS) return@withLock "Nicht übernommen: Der Server hat frischere Werte."
                val inhalt = JSONObject().put("quelle", quelle).put("geholt", jetzt).put("limits", limits).toString()
                val neu = File(app.filesDir, "kontingente-antigravity.json.neu")
                neu.writeText(inhalt)
                if (!neu.renameTo(antigravityDatei(app))) { antigravityDatei(app).writeText(inhalt); neu.delete() }
                val stand = gespeichert(app) ?: Stand(emptyList(), 0L)
                val karte = antigravityAnbieter(app)
                val s = Stand(stand.anbieter.filter { it.id != "gemini" } + karte, stand.geholt)
                runCatching { speichern(app, s, gewarntLesen(app)) }
                _frisch.value = s
                Logbuch.netz("Antigravity-Limits gemeldet", "quelle=$quelle anzahl=${karte.limits.size}")
                "Übernommen."
            }
        }

        /**
         * Lädt [url] in einem unsichtbaren WebView mit Franks Anmeldung und wertet [js] jede Sekunde aus, bis [fertig] das Ergebnis
         * annimmt oder die Frist endet. Ein echter Browser kommt durch die Bot-Abwehr und liest, was die Seite erst per Skript aufbaut.
         * Ein Gespräch beendet einen Abruf aus dem Hintergrund sofort: Die Stimme hat Vorrang.
         */
        private suspend fun seiteLesen(app: Context, url: String, js: String, fristMs: Long = 30_000L, fertig: (String) -> Boolean): String = withContext(Dispatchers.Main) {
            val w = WebView(app)
            var rendererTot = false
            try {
                w.settings.javaScriptEnabled = true
                w.settings.domStorageEnabled = true
                w.settings.userAgentString = AnthropicGuthabenFaehigkeit.chromeKennung(w.settings.userAgentString)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
                w.webViewClient = object : WebViewClient() {
                    // Ohne dieses true beendet Android bei einem Renderer-Absturz die ganze App samt Dienst und Gespräch.
                    override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                        rendererTot = true
                        return true
                    }
                }
                // Ohne Größe baut manche Seite nichts auf.
                val breite = android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY)
                val hoehe = android.view.View.MeasureSpec.makeMeasureSpec(2400, android.view.View.MeasureSpec.EXACTLY)
                w.measure(breite, hoehe)
                w.layout(0, 0, 1080, 2400)
                w.loadUrl(url)
                var letzter = ""
                val ende = System.currentTimeMillis() + fristMs
                while (System.currentTimeMillis() < ende) {
                    delay(1_000L)
                    if (rendererTot) throw IllegalStateException("Der Browser für die Abfrage ist abgestürzt.")
                    if (!laufErzwungen && GespraechsFokus.stilleHinweise) throw IllegalStateException("Abruf wegen eines Gesprächs verschoben.")
                    val roh = withTimeoutOrNull(5_000L) { suspendCancellableCoroutine<String?> { k -> w.evaluateJavascript(js) { v -> if (k.isActive) k.resume(v) } } }
                    if (roh == null || roh == "null") continue
                    val s = runCatching { JSONArray("[$roh]").getString(0) }.getOrDefault("")
                    if (s.isNotEmpty()) { letzter = s; if (fertig(s)) return@withContext s }
                }
                letzter
            } finally {
                if (!rendererTot) w.stopLoading()
                w.destroy()
            }
        }
    }
}
