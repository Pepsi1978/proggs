package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Anthropic-Guthaben, genau wie es die Claude Console oben rechts zeigt („Organisationscredits“): Rest, monatlich
 * gewährter Betrag (z. B. 100 $ aus dem Max-Abo) und Verfallsdatum. Die Console liest es aus
 * `/api/organizations/<org>/prepaid/credits`; das geht nur mit Franks Console-Anmeldung (Cookie), nicht mit einem
 * API-Schlüssel. Frank meldet sich dafür einmal in Jarvis an ([LOGIN_URL], eingebetteter Browser); Jarvis nutzt danach
 * dessen Cookies. Inoffizielle Schnittstelle: ändert Anthropic sie, kommt ein Hinweis statt einer Zahl.
 * Umgerechnet wird mit dem Tageskurs der EZB (Frankfurter, ohne Schlüssel).
 */
class AnthropicGuthabenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "anthropic_guthaben"
    override val name = "Anthropic-Guthaben"
    override val beschreibung = "Restguthaben der Anthropic-API (Organisationscredits der Claude Console)."
    override val hinweise =
        "Fragt Frank nach seinem Anthropic- oder Claude-Guthaben, seinem API-Verbrauch oder was von seinen Credits übrig ist, nutze anthropic_guthaben."
    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "anthropic_guthaben",
            titel = "Anthropic-Guthaben lesen",
            beschreibung = "Jarvis: Franks Restguthaben der Anthropic-API (Claude Console, Organisationscredits): übrig, gewährt, verbraucht und Verfallsdatum, mit Stand der Abfrage. " +
                "Für „Wie viel Anthropic-Guthaben habe ich noch?“, „Wie viel von meinen Claude-Credits ist verbraucht?“. Holt höchstens alle 15 Minuten frisch.",
            schema = schema("frisch" to schalter("true = jetzt neu bei Anthropic abfragen statt des Stands der letzten 15 Minuten.")),
            nurLesen = true,
        ) { a ->
            val s = stand(context, erzwingen = a.optBoolean("frisch"))
            Ergebnis(s.satz(), fehler = s.restUsd == null)
        },
    )

    /** Ergebnis einer Abfrage. Ohne [restUsd] steht in [hinweis], warum es keine Zahl gibt. */
    class Stand(
        val restUsd: Double?,
        /** Summe der gewährten Credits (z. B. 100 $ im Monat); null = Anthropic nennt keine. */
        val gewaehrtUsd: Double?,
        /** Frühestes Verfallsdatum der Credits, ISO; leer = keins bekannt. */
        val verfaellt: String,
        /** Euro je Dollar; null = Kurs nie abrufbar, dann nur in Dollar. */
        val kurs: Double?,
        val kursDatum: String,
        val geholt: Long,
        val hinweis: String? = null,
        /** Wann das jüngste Abo-Guthaben gewährt wurde, ISO; leer = unbekannt. */
        val gewaehrtAm: String = "",
        /** Selbst gekauftes Guthaben (echtes Geld) in Dollar; null = unbekannt. */
        val eigenesUsd: Double? = null,
        /** Betrag der jüngsten monatlichen Abo-Gewährung (z. B. 100 $); null = unbekannt. */
        val abobetragUsd: Double? = null,
    ) {
        /** Voraussichtlich nächstes Abo-Guthaben: einen Monat nach der letzten Gewährung. */
        val naechstes: java.time.LocalDate? get() = runCatching {
            var d = OffsetDateTime.parse(gewaehrtAm).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate().plusMonths(1)
            while (d.isBefore(java.time.LocalDate.now())) d = d.plusMonths(1)
            d
        }.getOrNull()

        /** Frühestes Verfallsdatum als Datum; null = keins bekannt. */
        val verfallDatum: java.time.LocalDate? get() = runCatching { OffsetDateTime.parse(verfaellt).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }
            .recoverCatching { Instant.parse(verfaellt).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()

        val verbrauchtUsd: Double? get() = gewaehrtUsd?.let { g -> restUsd?.let { (g - it).coerceAtLeast(0.0) } }

        private fun eur(usd: Double): String = kurs?.let { " (≈ ${betrag(usd * it)} €)" } ?: ""

        private fun verfallText(): String = runCatching {
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN).format(OffsetDateTime.parse(verfaellt).atZoneSameInstant(ZoneId.systemDefault()))
        }.getOrDefault("")

        /** Die Zeile für die Einstellungen, z. B. „Übrig: 74,00 $ (≈ 63,50 €) von 100,00 $“. */
        fun zeile(): String {
            val rest = restUsd ?: return hinweis ?: "Noch nicht abgefragt."
            return "Übrig: ${betrag(rest)} $${eur(rest)}" + (gewaehrtUsd?.let { " von ${betrag(it)} $" } ?: "")
        }

        /** Zweite Zeile: verbraucht, Verfall, Stand. */
        fun details(): String = listOfNotNull(
            verbrauchtUsd?.let { "Verbraucht ${betrag(it)} $" },
            verfallText().takeIf { it.isNotEmpty() }?.let { "verfällt am $it" },
            standText().takeIf { it.isNotEmpty() },
        ).joinToString(" · ")

        fun standText(): String = if (geholt <= 0) "" else "Stand " + DateTimeFormatter.ofPattern("dd.MM., HH:mm 'Uhr'", Locale.GERMAN)
            .format(Instant.ofEpochMilli(geholt).atZone(ZoneId.systemDefault()))

        /** Für Jarvis im Gespräch: Zahl, Stand und, falls nötig, der Hinweis. */
        fun satz(): String = buildString {
            append(zeile()).append('.')
            if (restUsd != null) {
                append(" ").append(details()).append('.')
                kurs?.let { append(" Kurs 1 $ = ").append(String.format(Locale.GERMAN, "%.4f", it)).append(" € (EZB ").append(kursDatum).append(").") }
                hinweis?.let { append(" ").append(it) }
            }
        }
    }

    companion object {
        private const val TAG = "JarvisAnthropicGuthaben"
        private const val FRISCH_MS = 15 * 60_000L
        private const val CONSOLE = "https://platform.claude.com"
        /** Anmeldeseite der Claude Console für den eingebetteten Browser. */
        const val LOGIN_URL = "$CONSOLE/login"
        private val CLIENT = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
        private val sperre = Mutex()
        private val hintergrund = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
        private val _frisch = kotlinx.coroutines.flow.MutableStateFlow<Stand?>(null)
        /** Jeder frisch geholte Stand, auch aus dem Dienst (Tagesauswertung): Die Anzeige unten übernimmt ihn sofort. */
        val frisch: kotlinx.coroutines.flow.StateFlow<Stand?> = _frisch

        /**
         * Nach einer Hintergrund-Aufgabe mit einem Anthropic-Modell (Tagesauswertung): Guthaben sofort und nach einer Minute
         * noch einmal holen (die Console bucht verzögert), ohne die Aufgabe aufzuhalten.
         */
        fun nachAufgabeHolen(context: Context) {
            val app = context.applicationContext
            hintergrund.launch {
                stand(app, erzwingen = true)
                kotlinx.coroutines.delay(60_000L)
                stand(app, erzwingen = true)
            }
        }

        private fun datei(context: Context) = File(context.filesDir, "anthropic_guthaben.json")

        fun betrag(w: Double): String = String.format(Locale.GERMAN, "%,.2f", w)

        private fun cookies(): String = runCatching { CookieManager.getInstance().getCookie(CONSOLE) }.getOrNull().orEmpty()

        /** true, wenn Frank in Jarvis bei der Claude Console angemeldet ist (Sitzungs-Cookie vorhanden). */
        fun angemeldet(): Boolean = cookies().contains("sessionKey=")

        /** Meldet Jarvis bei der Console ab: alle Cookies weg, gespeicherter Stand weg. Wartet eine laufende Abfrage ab. */
        suspend fun abmelden(context: Context) = withContext(Dispatchers.IO) { sperre.withLock { abmeldenOhneSperre(context.applicationContext) } }

        private fun abmeldenOhneSperre(context: Context) {
            // Nur die Cookies der Console löschen: Die Google-Anmeldung für das Google-Guthaben bleibt bestehen.
            val cm = CookieManager.getInstance()
            // Mit Secure, sonst lehnt Chromium das Überschreiben der __Secure-/__Host-Cookies ab.
            cookies().split(';').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }.forEach { name ->
                listOf("", "; Domain=.claude.com", "; Domain=claude.com", "; Domain=platform.claude.com").forEach { d ->
                    cm.setCookie(CONSOLE, "$name=; Max-Age=0; Path=/; Secure$d")
                    cm.setCookie(CONSOLE, "$name=; Max-Age=0; Path=/$d")
                }
            }
            CookieManager.getInstance().flush()
            datei(context).delete()
        }

        /** Sitzung bei der Console abgelaufen: Cookies löschen, damit die Einstellungen wieder „Anmelden“ anbieten. */
        private class SitzungAbgelaufen(meldung: String) : IllegalStateException(meldung)

        /** ISO-Zeitpunkt mit „Z“ oder Versatz (auch mit Mikrosekunden) als Instant; null, wenn unlesbar. */
        private fun zeitpunkt(text: String): Instant? =
            runCatching { OffsetDateTime.parse(text).toInstant() }.getOrNull() ?: runCatching { Instant.parse(text) }.getOrNull()

        /** Betrag aus einem Objekt `{credits: {amount_minor, exponent}}`, sonst aus dem Cent-Feld [cent]. */
        private fun betragAus(o: JSONObject?, cent: Long): Double =
            o?.optJSONObject("credits")?.takeIf { it.has("amount_minor") }?.let { it.getLong("amount_minor") / Math.pow(10.0, it.optInt("exponent", 2).toDouble()) }
                ?: (cent / 100.0)

        /** Der zuletzt gespeicherte Stand, ohne Netz; null, wenn es keinen gibt. */
        fun gespeichert(context: Context): Stand? = runCatching {
            val j = JSONObject(datei(context).readText())
            if (!j.has("rest")) return null
            Stand(j.getDouble("rest"), if (j.has("gewaehrt")) j.getDouble("gewaehrt") else null, j.optString("verfaellt"),
                if (j.has("kurs")) j.getDouble("kurs") else null, j.optString("kursDatum"), j.optLong("geholt"),
                gewaehrtAm = j.optString("gewaehrtAm"), eigenesUsd = if (j.has("eigenes")) j.getDouble("eigenes") else null,
                abobetragUsd = if (j.has("abobetrag")) j.getDouble("abobetrag").takeIf { it > 0 } else null)
        }.getOrNull()

        /**
         * Der aktuelle Stand: der gespeicherte, wenn er jünger als 15 Minuten ist (außer [erzwingen]), sonst frisch abgefragt.
         * Scheitert die Abfrage, kommt der Grund, bei einem älteren Stand dieser mit dem Grund als Hinweis.
         */
        suspend fun stand(context: Context, erzwingen: Boolean = false): Stand = withContext(Dispatchers.IO) {
            val app = context.applicationContext
            if (!angemeldet()) return@withContext Stand(null, null, "", null, "", 0,
                "Für das Guthaben bitte unter Einstellungen → Jarvis einmal bei der Claude Console anmelden.")
            sperre.withLock {
                val alt = gespeichert(app)
                if (!erzwingen && alt != null && System.currentTimeMillis() - alt.geholt < FRISCH_MS) return@withLock alt
                try {
                    val ua = runCatching { WebSettings.getDefaultUserAgent(app) }.getOrDefault("Mozilla/5.0")
                    val org = organisation(app, ua)
                    val c = holen("$CONSOLE/api/organizations/$org/prepaid/credits", ua) as JSONObject
                    val rest = betragAus(c.optJSONObject("balance"), c.getLong("amount"))
                    // Nur gültige Tranchen mit Rest zählen; abgelaufene oder aufgebrauchte Vormonate verfälschen sonst
                    // „gewährt“, „verbraucht“ und das Verfallsdatum.
                    val jetztI = Instant.now()
                    var gewaehrt = 0.0
                    var verfaelltI: Instant? = null
                    var gewaehrtAmI: Instant? = null
                    // Selbst gekaufte Credits stehen in „tranches“, das Abo-Guthaben in „promo_tranches“.
                    var eigenes = 0.0
                    var abobetrag = 0.0
                    listOf("tranches", "promo_tranches").forEach { feld ->
                        val t = c.optJSONArray(feld) ?: return@forEach
                        for (i in 0 until t.length()) {
                            val p = t.getJSONObject(i)
                            val ende = p.optString("expires_at").takeIf { it.isNotBlank() && it != "null" }?.let(::zeitpunkt)
                            // Die jüngste Abo-Gewährung zählt auch aufgebraucht: Von ihr aus kommt die nächste.
                            val am = p.optString("granted_at").takeIf { it.isNotBlank() && it != "null" }?.let(::zeitpunkt)
                            if (feld == "promo_tranches" && am != null && (gewaehrtAmI == null || am > gewaehrtAmI)) {
                                gewaehrtAmI = am
                                abobetrag = betragAus(p.optJSONObject("granted"), p.optLong("granted_amount_minor_units"))
                            }
                            if (ende != null && ende <= jetztI) continue
                            val restTranche = betragAus(p.optJSONObject("remaining"), p.optLong("remaining_amount_minor_units"))
                            if (restTranche <= 0.0) continue
                            if (feld == "tranches") eigenes += restTranche
                            gewaehrt += betragAus(p.optJSONObject("granted"), p.optLong("granted_amount_minor_units"))
                            if (ende != null && (verfaelltI == null || ende < verfaelltI)) verfaelltI = ende
                        }
                    }
                    val verfaellt = verfaelltI?.toString().orEmpty()
                    if (!angemeldet()) throw SitzungAbgelaufen("Inzwischen abgemeldet.")
                    // Der EZB-Kurs ändert sich nur einmal am Tag: ein höchstens sechs Stunden alter genügt.
                    val (kurs, kursDatum) = alt?.kurs?.takeIf { System.currentTimeMillis() - alt.geholt < 6 * 3_600_000L }
                        ?.let { it to alt.kursDatum } ?: kurs(app)
                    val jetzt = System.currentTimeMillis()
                    val j = JSONObject().put("org", org).put("rest", rest).put("verfaellt", verfaellt).put("geholt", jetzt).put("kursDatum", kursDatum)
                    if (gewaehrt > 0) j.put("gewaehrt", gewaehrt)
                    kurs?.let { j.put("kurs", it) }
                    val gewaehrtAm = gewaehrtAmI?.toString().orEmpty()
                    j.put("gewaehrtAm", gewaehrtAm).put("eigenes", eigenes).put("abobetrag", abobetrag)
                    datei(app).writeText(j.toString())
                    Stand(rest, gewaehrt.takeIf { it > 0 }, verfaellt, kurs, kursDatum, jetzt, gewaehrtAm = gewaehrtAm, eigenesUsd = eigenes,
                        abobetragUsd = abobetrag.takeIf { it > 0 }).also { _frisch.value = it }
                } catch (f: SitzungAbgelaufen) {
                    abmeldenOhneSperre(app)
                    Stand(null, null, "", null, "", 0, f.message)
                } catch (f: Exception) { // no-cancellation-rethrow
                    Log.w(TAG, "Abfrage fehlgeschlagen", f)
                    Logbuch.netz("Anthropic-Guthaben nicht abrufbar", f.message ?: f.javaClass.simpleName)
                    val grund = f.message ?: "Abfrage gerade nicht möglich (${f.javaClass.simpleName})."
                    if (alt != null) Stand(alt.restUsd, alt.gewaehrtUsd, alt.verfaellt, alt.kurs, alt.kursDatum, alt.geholt, "Neue Abfrage gescheitert, das ist der letzte Stand: $grund", alt.gewaehrtAm, alt.eigenesUsd, alt.abobetragUsd)
                    else Stand(null, null, "", null, "", 0, grund)
                }
            }
        }

        /** Die Organisation mit API-Zugang (nicht die Abo-Organisation von claude.ai); gemerkt in der Stand-Datei. */
        private fun organisation(context: Context, ua: String): String {
            runCatching { JSONObject(datei(context).readText()).optString("org") }.getOrNull()?.takeIf { it.isNotBlank() }?.let { return it }
            val liste = holen("$CONSOLE/api/organizations", ua) as JSONArray
            for (i in 0 until liste.length()) {
                val o = liste.getJSONObject(i)
                val kann = o.optJSONArray("capabilities") ?: continue
                if ((0 until kann.length()).any { kann.optString(it) == "api" }) return o.getString("uuid")
            }
            throw IllegalStateException("In der Claude Console gibt es keine Organisation mit API-Zugang.")
        }

        private fun holen(url: String, ua: String): Any =
            CLIENT.newCall(
                Request.Builder().url(url).header("Cookie", cookies()).header("User-Agent", ua)
                    .header("Accept", "application/json").header("Referer", "$CONSOLE/dashboard").build(),
            ).execute().use { a ->
                val text = a.body?.string().orEmpty()
                val json = text.trimStart().let { it.startsWith("{") || it.startsWith("[") }
                // 401 oder ein 403 mit JSON-Antwort kommt von der Console selbst: Sitzung weg. Ein 403 als HTML-Seite ist die
                // Bot-Abwehr (Cloudflare) vor der Console; neu anmelden hilft dort nicht.
                if (a.code == 401 || (a.code == 403 && json)) throw SitzungAbgelaufen(
                    "Die Anmeldung bei der Claude Console ist abgelaufen (${a.code}). Bitte unter Einstellungen → Jarvis neu anmelden.")
                if (!a.isSuccessful) throw IllegalStateException(when (a.code) {
                    403 -> "Die Bot-Abwehr der Claude Console lässt die Abfrage vom Handy gerade nicht durch (403); neu anmelden hilft hier nicht."
                    429 -> "Anthropic bremst gerade (429); bitte gleich noch einmal versuchen."
                    else -> "Die Claude Console antwortet mit Fehler ${a.code}."
                })
                runCatching { if (text.trimStart().startsWith("[")) JSONArray(text) else JSONObject(text) }
                    .getOrElse { throw IllegalStateException("Die Claude Console lieferte keine lesbare Antwort (Schnittstelle geändert?).") }
            }

        /** Euro je Dollar und Datum des Kurses; scheitert der Abruf, der zuletzt gespeicherte Kurs, sonst null. */
        private fun kurs(context: Context): Pair<Double?, String> {
            listOf("https://api.frankfurter.dev/v1/latest?base=USD&symbols=EUR", "https://api.frankfurter.app/latest?from=USD&to=EUR").forEach { adresse ->
                runCatching {
                    CLIENT.newCall(Request.Builder().url(adresse).build()).execute().use { a ->
                        check(a.isSuccessful) { "Kurs-Abruf ${a.code}" }
                        val j = JSONObject(a.body?.string().orEmpty())
                        val k = j.getJSONObject("rates").getDouble("EUR")
                        if (k > 0) return k to j.optString("date")
                    }
                }.onFailure { Log.w(TAG, "Kurs von $adresse nicht abrufbar", it) }
            }
            return runCatching { JSONObject(datei(context).readText()) }.getOrNull()
                ?.takeIf { it.has("kurs") }?.let { it.getDouble("kurs") to it.optString("kursDatum") } ?: (null to "")
        }
    }
}
