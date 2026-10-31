package de.frank.jarvis.faehigkeit

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
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
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Nutzungslimits der KI-Abos: Claude, ChatGPT/Codex, Kimi und Gemini, jedes Limit mit Rest, Zurücksetzen (deutsche Zeit)
 * und Hochrechnung des Tempos. Die Anbieter haben dafür keine offiziellen Schnittstellen:
 * - Claude: `claude.ai/api/organizations/<org>/usage`, aus der Seite heraus (eigene Anmeldung, die Bot-Abwehr lässt nur einen Browser durch).
 * - Codex: `chatgpt.com/backend-api/wham/usage` mit der ChatGPT-Anmeldung von Jarvis.
 * - Kimi und Gemini: Text der Seiten `kimi.ai/settings/subscription` und `gemini.google.com/usage` in einem unsichtbaren WebView.
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
            beschreibung = "Jarvis: Franks aktuelle Nutzungslimits bei Claude (5 Stunden, Woche), ChatGPT/Codex (Woche), Kimi (5 Stunden, Monat) und Gemini (aktuell, Woche): " +
                "genutzt, übrig, wann zurückgesetzt wird (deutsche Zeit) und ob das Limit beim jetzigen Tempo vorher aufgebraucht wäre. Holt höchstens alle 15 Minuten frisch.",
            schema = schema(
                "frisch" to schalter("true = jetzt neu bei den Anbietern abfragen statt des Stands der letzten 15 Minuten."),
                "json" to schalter("true = Ausgabe als JSON für die Mac-Oberfläche."),
            ),
            nurLesen = true,
        ) { a ->
            val s = stand(context, erzwingen = a.optBoolean("frisch"))
            Ergebnis(if (a.optBoolean("json")) s.alsJson().toString() else s.text(), fehler = s.anbieter.all { it.limits.isEmpty() })
        },
    )

    /** Ein Limit. [genutzt] in Prozent, [reset] in Sekunden seit 1970 (0 = beginnt erst mit der nächsten Nutzung). */
    class Limit(val name: String, val genutzt: Double, val fensterSek: Long, val reset: Long) {
        val uebrig: Double get() = (100.0 - genutzt).coerceAtLeast(0.0)
        fun abgelaufen(jetzt: Long) = reset in 1..jetzt
        private fun vergangen(jetzt: Long) = fensterSek - (reset - jetzt)
        private fun rechenbar(jetzt: Long) = reset > jetzt && fensterSek > 0 && genutzt > 0 && vergangen(jetzt) >= fensterSek / 10

        /** Prozent, bei denen das Limit beim bisherigen Tempo am Ende des Fensters stünde; null = noch zu früh. */
        fun hochrechnung(jetzt: Long): Double? = if (rechenbar(jetzt)) genutzt * fensterSek / vergangen(jetzt) else null

        /** Wann das Limit beim bisherigen Tempo leer wäre; null, wenn das nicht vor dem Zurücksetzen passiert. */
        fun leerAm(jetzt: Long): Long? = if (zuSchnell(jetzt)) reset - fensterSek + (vergangen(jetzt) * 100 / genutzt).toLong() else null

        fun zuSchnell(jetzt: Long): Boolean = genutzt < 100 && (hochrechnung(jetzt) ?: 0.0) > 100.0

        fun resetText(jetzt: Long): String = when {
            reset <= 0 -> "beginnt mit der nächsten Nutzung"
            abgelaufen(jetzt) -> "inzwischen zurückgesetzt"
            else -> zeitText(reset)
        }

        fun tempoText(jetzt: Long): String {
            if (reset <= 0 || abgelaufen(jetzt)) return ""
            if (genutzt >= 100) return "Aufgebraucht bis zum Zurücksetzen."
            val leer = leerAm(jetzt)
            if (leer != null) return "ZU SCHNELL: Bei diesem Tempo wäre es ${zeitText(leer)} leer, ${dauerText(reset - leer)} vor dem Zurücksetzen."
            val h = hochrechnung(jetzt) ?: return "Für eine Hochrechnung ist es noch zu früh."
            return "Tempo passt: hochgerechnet etwa ${h.toInt()} % bis zum Zurücksetzen."
        }

        fun json() = JSONObject().put("name", name).put("genutzt", genutzt).put("fenster", fensterSek).put("reset", reset)
    }

    /** Ein Anbieter. [anmeldenNoetig] = nur eine neue Anmeldung hilft; [hinweis] = warum der Stand fehlt oder alt ist. */
    class Anbieter(val id: String, val titel: String, val konto: String, val limits: List<Limit>, val hinweis: String?, val anmeldenNoetig: Boolean, val geholt: Long) {
        fun standText(): String = if (geholt <= 0) "" else "Stand " + STAND.format(Instant.ofEpochMilli(geholt).atZone(BERLIN))
    }

    class Stand(val anbieter: List<Anbieter>, val geholt: Long) {
        fun von(id: String) = anbieter.firstOrNull { it.id == id }

        /** Für Jarvis' Gedächtnis und die Modelle. */
        fun text(): String = buildString {
            val jetzt = sekunden()
            append("Zeiten in deutscher Zeit.")
            anbieter.forEach { a ->
                append("\n").append(a.titel).append(if (a.konto.isNotEmpty()) " (${a.konto})" else "").append(": ")
                if (a.limits.isEmpty()) { append(a.hinweis ?: "Noch nicht abgefragt."); return@forEach }
                append(a.standText()).append('.')
                a.limits.forEach { l ->
                    append("\n- ").append(l.name).append(": ").append(prozent(l.genutzt)).append(" % genutzt, ").append(prozent(l.uebrig))
                        .append(" % übrig, zurückgesetzt ").append(l.resetText(jetzt)).append('.')
                    l.tempoText(jetzt).takeIf { it.isNotEmpty() }?.let { append(' ').append(it) }
                }
                a.hinweis?.let { append("\n  Hinweis: ").append(it) }
            }
        }

        fun alsJson(): JSONObject {
            val jetzt = sekunden()
            return JSONObject().put("anbieter", JSONArray(anbieter.map { a ->
                JSONObject().put("id", a.id).put("titel", a.titel).put("konto", a.konto).put("hinweis", a.hinweis ?: "").put("anmelden", a.anmeldenNoetig).put("stand", a.standText())
                    .put("limits", JSONArray(a.limits.map { l ->
                        JSONObject().put("name", l.name).put("genutzt", l.genutzt).put("uebrig", l.uebrig).put("reset", l.resetText(jetzt))
                            .put("tempo", l.tempoText(jetzt)).put("zuSchnell", l.zuSchnell(jetzt))
                    }))
            }))
        }
    }

    private class NichtAngemeldet(meldung: String) : IllegalStateException(meldung)

    companion object {
        private const val FRISCH_MS = 15 * 60_000L
        private const val STUNDEN5 = 5 * 3600L
        private const val WOCHE = 7 * 86400L
        /** Ab hier warnt Jarvis: Darunter sagt eine Hochrechnung am Anfang eines Fensters zu wenig. */
        private const val WARNEN_AB = 30.0
        private const val CLAUDE = "https://claude.ai"
        const val CLAUDE_LOGIN = "$CLAUDE/login"
        const val KIMI_SEITE = "https://www.kimi.ai/settings/subscription"
        private const val GEMINI_SEITE = "https://gemini.google.com/usage?hl=de"
        val BERLIN: ZoneId = ZoneId.of("Europe/Berlin")
        private val STAND = DateTimeFormatter.ofPattern("dd.MM., HH:mm 'Uhr'", Locale.GERMAN)
        private val UHR = DateTimeFormatter.ofPattern("HH:mm 'Uhr'", Locale.GERMAN)
        private val TAG = DateTimeFormatter.ofPattern("EEEE, dd.MM., HH:mm 'Uhr'", Locale.GERMAN)
        private val sperre = Mutex()
        private val _frisch = MutableStateFlow<Stand?>(null)
        /** Jeder neue Stand, auch aus dem Wächter: Der offene Reiter übernimmt ihn sofort. */
        val frisch: StateFlow<Stand?> = _frisch

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

        /** Gesicherte Claude-Sitzung zurück in den Browser, falls sein Cookie abgelaufen ist. */
        private fun claudeSitzung(app: Context): Boolean {
            val s = SecureSettings.get(app)
            val browser = claudeSitzungImBrowser()
            if (browser != null) { if (browser != s.claudeAiSitzung) s.claudeAiSitzung = browser; return true }
            val gesichert = s.claudeAiSitzung.takeIf { it.isNotEmpty() } ?: return false
            CookieManager.getInstance().setCookie(CLAUDE, "sessionKey=$gesichert; Path=/; Secure; HttpOnly; Max-Age=2592000")
            CookieManager.getInstance().flush()
            return true
        }

        /** Meldet Jarvis bei claude.ai oder Kimi ab. Die Console und das Google-Konto bleiben angemeldet. */
        suspend fun abmelden(context: Context, id: String) = withContext(Dispatchers.Main) {
            val app = context.applicationContext
            val adresse = if (id == "claude") CLAUDE else "https://www.kimi.ai"
            val cm = CookieManager.getInstance()
            runCatching { cm.getCookie(adresse) }.getOrNull().orEmpty().split(';').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }.forEach { name ->
                listOf("", "; Domain=" + adresse.removePrefix("https://"), "; Domain=." + adresse.removePrefix("https://www.").removePrefix("https://")).forEach { d ->
                    cm.setCookie(adresse, "$name=; Max-Age=0; Path=/; Secure$d")
                }
            }
            cm.flush()
            WebStorage.getInstance().deleteOrigin(adresse)
            if (id == "claude") withContext(Dispatchers.IO) { SecureSettings.get(app).claudeAiSitzung = "" }
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

        private fun speichern(context: Context, s: Stand, gewarnt: JSONObject) {
            datei(context).writeText(JSONObject().put("geholt", s.geholt).put("gewarnt", gewarnt).put("anbieter", JSONArray(s.anbieter.map { a ->
                JSONObject().put("id", a.id).put("titel", a.titel).put("konto", a.konto).put("hinweis", a.hinweis ?: "").put("anmelden", a.anmeldenNoetig).put("geholt", a.geholt)
                    .put("limits", JSONArray(a.limits.map { it.json() }))
            })).toString())
        }

        /**
         * Der aktuelle Stand: der gespeicherte, wenn er jünger als 15 Minuten ist (außer [erzwingen]), sonst frisch.
         * Während eines Gesprächs bleibt es beim gespeicherten: Die Stimme geht vor, die unsichtbaren Seiten kosten Rechenzeit.
         */
        suspend fun stand(context: Context, erzwingen: Boolean = false): Stand = withContext(Dispatchers.IO) {
            val app = context.applicationContext
            sperre.withLock {
                val alt = gespeichert(app)
                if (alt != null && !erzwingen && (System.currentTimeMillis() - alt.geholt < FRISCH_MS || GespraechsFokus.stilleHinweise)) return@withLock alt
                val abfragen: List<Triple<String, String, suspend () -> Pair<String, List<Limit>>>> = listOf(
                    Triple("claude", "Claude (Anthropic)") { claude(app) },
                    Triple("codex", "ChatGPT / Codex (OpenAI)") { codex(app) },
                    Triple("kimi", "Kimi") { kimi(app) },
                    Triple("gemini", "Gemini (Google)") { gemini(app) },
                )
                val liste = coroutineScope {
                    abfragen.map { (id, titel, abfrage) ->
                        async {
                            val vorher = alt?.von(id)
                            try {
                                val (konto, limits) = abfrage()
                                Anbieter(id, titel, konto, limits, null, false, System.currentTimeMillis())
                            } catch (f: kotlinx.coroutines.CancellationException) {
                                throw f
                            } catch (f: Exception) {
                                Logbuch.netz("Kontingent $id nicht abrufbar", f.message ?: f.javaClass.simpleName)
                                val grund = f.message ?: "Abfrage gerade nicht möglich."
                                Anbieter(id, titel, vorher?.konto.orEmpty(), vorher?.limits.orEmpty(),
                                    if (vorher?.limits?.isNotEmpty() == true) "Letzter Stand. $grund" else grund, f is NichtAngemeldet, vorher?.geholt ?: 0L)
                            }
                        }
                    }.awaitAll()
                }
                val s = Stand(liste, System.currentTimeMillis())
                val gewarnt = warnen(app, s)
                runCatching { speichern(app, s, gewarnt) }
                _frisch.value = s
                s
            }
        }

        /** Meldet jedes Limit, das zu schnell schrumpft, einmal je Fenster. */
        private fun warnen(app: Context, s: Stand): JSONObject {
            val gewarnt = runCatching { JSONObject(datei(app).readText()).optJSONObject("gewarnt") }.getOrNull() ?: JSONObject()
            val jetzt = sekunden()
            s.anbieter.filter { it.hinweis == null }.forEach { a ->
                a.limits.filter { it.zuSchnell(jetzt) && it.genutzt >= WARNEN_AB }.forEach { l ->
                    val schluessel = a.id + "|" + l.name
                    // Die Anbieter nennen das Ende nicht auf die Sekunde gleich: Innerhalb eines Zehntels des Fensters ist es dasselbe.
                    if (Math.abs(gewarnt.optLong(schluessel) - l.reset) > l.fensterSek / 10) {
                        gewarnt.put(schluessel, l.reset)
                        val titel = "${a.titel}: ${l.name} schrumpft zu schnell"
                        val text = "${prozent(l.genutzt)} % genutzt. " + l.tempoText(jetzt).removePrefix("ZU SCHNELL: ") + " Zurückgesetzt wird ${l.resetText(jetzt)}."
                        Logbuch.netz(titel, text)
                        runCatching {
                            Meldungen.zeige(app, Meldungsarten.KONTINGENT, schluessel.hashCode(),
                                NotificationCompat.Builder(app, "jarvis_ergebnisse").setSmallIcon(R.drawable.ic_stat_jarvis).setContentTitle(titel).setContentText(text)
                                    .setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true)
                                    .setContentIntent(PendingIntent.getActivity(app, 0, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)),
                                titel, sprechtext = "${a.titel}: ${l.name}. $text")
                        }
                    }
                }
            }
            return gewarnt
        }

        // ---------------------------------------------------------------- Anbieter

        private suspend fun claude(app: Context): Pair<String, List<Limit>> {
            if (!claudeSitzung(app)) throw NichtAngemeldet("Noch nicht bei claude.ai angemeldet.")
            val js = """(function(){ if(location.host!=='claude.ai') return null;
                if(window.__jk===undefined){ window.__jk=null; (async()=>{ try {
                    const a=await fetch('/api/organizations',{credentials:'include',headers:{Accept:'application/json'}});
                    if(!a.ok){ window.__jk=JSON.stringify({s:a.status,b:(await a.text()).slice(0,200)}); return; }
                    const o=await a.json(); const org=o.find(x=>(x.capabilities||[]).includes('chat'))||o[0];
                    const r=await fetch('/api/organizations/'+org.uuid+'/usage',{credentials:'include',headers:{Accept:'application/json'}});
                    window.__jk=JSON.stringify({s:r.status,b:await r.text()});
                } catch(e){ window.__jk=JSON.stringify({s:0,b:String(e)}); } })(); }
                return window.__jk; })()"""
            // Zuerst die leichte Datei; lässt die Bot-Abwehr das nicht zu, die echte Seite (die löst deren Prüfung).
            var antwort: JSONObject? = null
            for (adresse in listOf("$CLAUDE/robots.txt", "$CLAUDE/new")) {
                antwort = runCatching { JSONObject(seiteLesen(app, adresse, js) { it.isNotEmpty() }) }.getOrNull()
                if (antwort?.optInt("s") == 200) break
            }
            val code = antwort?.optInt("s") ?: 0
            val text = antwort?.optString("b").orEmpty()
            if (code == 401 || (code == 403 && text.trimStart().startsWith("{"))) throw NichtAngemeldet("claude.ai lehnt die Anmeldung ab ($code). Bitte neu anmelden.")
            if (code != 200) throw IllegalStateException("claude.ai antwortet gerade nicht (Fehler $code).")
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
            val limits = listOf("primary_window", "secondary_window").mapNotNull { rl.optJSONObject(it) }.map { w ->
                val fenster = w.optLong("limit_window_seconds", WOCHE)
                val name = when { fenster == WOCHE -> "Wochenlimit"; fenster == STUNDEN5 -> "5-Stunden-Limit"; else -> "Limit über ${fenster / 3600} Stunden" }
                Limit(name, w.optDouble("used_percent", 0.0), fenster, w.optLong("reset_at"))
            }
            if (limits.isEmpty()) throw IllegalStateException("OpenAI nennt keine Limits (Schnittstelle geändert?).")
            return listOf(j.optString("email"), j.optString("plan_type")).filter { it.isNotBlank() }.joinToString(" · ") to limits
        }

        private const val SEITENTEXT = "(function(){ return document.body ? (document.body.innerText || document.body.textContent || '') : null; })()"
        private val KIMI_GESAMT = Regex("Gesamtnutzung|Total usage|Overall usage", RegexOption.IGNORE_CASE)
        private val KIMI_5H = Regex("Nutzung in 5 Stunden|5[- ]?hours?( usage)?|5h", RegexOption.IGNORE_CASE)
        private val PROZENT = Regex("(\\d+(?:[.,]\\d+)?)\\s*%")
        private val KIMI_ZEIT = Regex("(?:(\\d{4})-)?(\\d{1,2})-(\\d{1,2})\\s+(\\d{1,2}):(\\d{2})")

        private suspend fun kimi(app: Context): Pair<String, List<Limit>> {
            val text = seiteLesen(app, KIMI_SEITE, SEITENTEXT) { kimiLesen(it).size == 2 }
            val limits = kimiLesen(text)
            if (limits.isNotEmpty()) return "" to limits
            throw if (Regex("Anmelden|Einloggen|Log ?in|Sign ?in", RegexOption.IGNORE_CASE).containsMatchIn(text) || text.isBlank()) NichtAngemeldet("Noch nicht bei Kimi angemeldet.")
            else IllegalStateException("Die Kimi-Seite ließ sich nicht lesen (Seite geändert?).")
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

        private val GEMINI_MONATE = listOf("jan", "feb", "mär", "apr", "mai", "jun", "jul", "aug", "sep", "okt", "nov", "dez")
        private val GEMINI_GENUTZT = Regex("(\\d+)\\s*%\\s*genutzt")
        private val GEMINI_RESET = Regex("Wird\\s+(morgen\\s+)?(?:am\\s+(\\d{1,2})\\.\\s*([^\\s.]+)\\.?\\s+)?um\\s+(\\d{1,2}):(\\d{2})")

        private suspend fun gemini(app: Context): Pair<String, List<Limit>> {
            if (!GoogleGuthabenFaehigkeit.angemeldet()) throw NichtAngemeldet("Noch nicht bei Google angemeldet.")
            val text = seiteLesen(app, GEMINI_SEITE, SEITENTEXT) { geminiLesen(it).size == 2 }
            val limits = geminiLesen(text)
            if (limits.isNotEmpty()) return "" to limits
            throw if (text.contains("accounts.google.com") || Regex("Anmelden|Sign in").containsMatchIn(text)) NichtAngemeldet("Google verlangt für Gemini eine neue Anmeldung.")
            else IllegalStateException("Die Gemini-Seite ließ sich nicht lesen (Seite geändert?).")
        }

        /** „Aktuelle Nutzung … 0 % genutzt Wird um 21:21 zurückgesetzt Wöchentliches Limit Wird am 2. Nov. um 14:21 zurückgesetzt 0 % genutzt“. */
        private fun geminiLesen(text: String): List<Limit> {
            val aktuell = text.lastIndexOf("Aktuelle Nutzung")
            val woche = text.lastIndexOf("Wöchentliches Limit")
            if (aktuell < 0 || woche < aktuell) return emptyList()
            return listOfNotNull(
                geminiLimit(text.substring(aktuell, woche), "Aktuelles Limit (5 Stunden)", STUNDEN5),
                geminiLimit(text.substring(woche).substringBefore("Upgraden"), "Wochenlimit", WOCHE),
            )
        }

        private fun geminiLimit(teil: String, name: String, fenster: Long): Limit? {
            val p = GEMINI_GENUTZT.find(teil)?.groupValues?.get(1)?.toDoubleOrNull() ?: return null
            val z = GEMINI_RESET.find(teil)?.groupValues ?: return null
            val jetzt = LocalDateTime.now()
            val monat = GEMINI_MONATE.indexOfFirst { z[3].lowercase(Locale.GERMAN).startsWith(it) } + 1
            var d = if (z[2].isNotEmpty() && monat > 0) LocalDateTime.of(jetzt.year, monat, z[2].toInt(), z[4].toInt(), z[5].toInt())
            else jetzt.toLocalDate().plusDays(if (z[1].isNotEmpty()) 1 else 0).atTime(z[4].toInt(), z[5].toInt())
            if (d.isBefore(jetzt)) d = if (z[2].isNotEmpty()) d.plusYears(1) else d.plusDays(1)
            return Limit(name, p, fenster, d.atZone(ZoneId.systemDefault()).toEpochSecond())
        }

        /**
         * Lädt [url] in einem unsichtbaren WebView mit Franks Anmeldung und wertet [js] jede Sekunde aus, bis [fertig] das Ergebnis
         * annimmt oder die Frist endet. Ein echter Browser kommt durch die Bot-Abwehr und liest, was die Seite erst per Skript aufbaut.
         */
        private suspend fun seiteLesen(app: Context, url: String, js: String, fristMs: Long = 30_000L, fertig: (String) -> Boolean): String = withContext(Dispatchers.Main) {
            val w = WebView(app)
            try {
                w.settings.javaScriptEnabled = true
                w.settings.domStorageEnabled = true
                w.settings.userAgentString = AnthropicGuthabenFaehigkeit.chromeKennung(w.settings.userAgentString)
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
                w.webViewClient = WebViewClient()
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
                    val roh = withTimeoutOrNull(5_000L) { suspendCancellableCoroutine<String?> { k -> w.evaluateJavascript(js) { v -> if (k.isActive) k.resume(v) } } }
                    if (roh == null || roh == "null") continue
                    val s = runCatching { JSONArray("[$roh]").getString(0) }.getOrDefault("")
                    if (s.isNotEmpty()) { letzter = s; if (fertig(s)) return@withContext s }
                }
                letzter
            } finally {
                w.stopLoading()
                w.destroy()
            }
        }
    }
}
