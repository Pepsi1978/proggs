package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebSettings
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Franks Google-Cloud-Guthaben aus dem Google Developer Program, mit dem die Gemini-Stimmen (Flash und Flash-Lite TTS)
 * über den bezahlten Schlüssel laufen: jede Gutschrift mit Betrag, Verbrauch, Rest und Verfall, dazu die monatliche
 * Gewährung. AI Studio liest das über seine eigene (inoffizielle) Schnittstelle; das geht nur mit Franks Google-Anmeldung
 * (Cookie, Anmeldung einmal im eingebetteten Browser), nicht mit dem Gemini-Schlüssel. Ändert Google sie, kommt ein
 * Hinweis statt einer Zahl. Die Vorauszahlung (echtes Geld) zeigt AI Studio nur in einem Zahlungsrahmen von Google
 * Payments; sie wird erst belastet, wenn das Guthaben aufgebraucht ist.
 */
class GoogleGuthabenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "google_guthaben"
    override val name = "Google-Guthaben"
    override val beschreibung = "Google-Cloud-Guthaben für die Gemini-Stimmen (AI Studio, Google Developer Program)."
    override val hinweise =
        "Fragt Frank nach seinem Google-, Gemini- oder AI-Studio-Guthaben oder was seine Vorlesestimme kostet, nutze google_guthaben."
    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "google_guthaben",
            titel = "Google-Guthaben lesen",
            beschreibung = "Jarvis: Franks Google-Cloud-Guthaben aus dem Google Developer Program (bezahlt die Gemini-Stimmen Flash und Flash-Lite TTS): " +
                "übrig, verbraucht, jede Gutschrift mit Verfallsdatum und wann voraussichtlich das nächste Guthaben kommt. Holt höchstens alle 15 Minuten frisch.",
            schema = schema("frisch" to schalter("true = jetzt neu bei Google abfragen statt des Stands der letzten 15 Minuten.")),
            nurLesen = true,
        ) { a ->
            val s = stand(context, erzwingen = a.optBoolean("frisch"))
            Ergebnis(s.satz(), fehler = s.restEur == null)
        },
    )

    /** Eine Gutschrift; Beträge in der Währung des Rechnungskontos, [ende] in Sekunden. */
    class Gutschrift(val betrag: Double, val verbraucht: Double, val rest: Double, val ende: Long)

    /** Ergebnis einer Abfrage. Ohne [restEur] steht in [hinweis], warum es keine Zahl gibt. */
    class Stand(
        val gutschriften: List<Gutschrift>,
        val waehrung: String,
        /** Beginn der letzten monatlichen Gewährung in Sekunden; 0 = unbekannt. */
        val letzteGewaehrung: Long,
        /** Betrag der letzten monatlichen Gewährung; null = unbekannt. */
        val letzterBetrag: Double?,
        val geholt: Long,
        val hinweis: String? = null,
    ) {
        val restEur: Double? get() = if (hinweis != null && gutschriften.isEmpty()) null else gutschriften.sumOf { it.rest }
        val gewaehrt: Double get() = gutschriften.sumOf { it.betrag }
        val verbraucht: Double get() = gutschriften.sumOf { it.verbraucht }

        /** Nach Verfall sortiert, nur die mit Rest. */
        val offen: List<Gutschrift> get() = gutschriften.filter { it.rest > 0.004 }.sortedBy { it.ende }

        /** Voraussichtlich nächste Gewährung: einen Monat nach der letzten (bei Google bisher immer um den 15.). */
        val naechste: LocalDate? get() {
            if (letzteGewaehrung <= 0) return null
            var d = Instant.ofEpochSecond(letzteGewaehrung).atZone(ZoneId.systemDefault()).toLocalDate().plusMonths(1)
            val heute = LocalDate.now()
            while (d.isBefore(heute)) d = d.plusMonths(1)
            return d
        }

        fun standText(): String = if (geholt <= 0) "" else "Stand " + DateTimeFormatter.ofPattern("dd.MM., HH:mm 'Uhr'", Locale.GERMAN)
            .format(Instant.ofEpochMilli(geholt).atZone(ZoneId.systemDefault()))

        fun satz(): String = buildString {
            val rest = restEur ?: return hinweis ?: "Noch nicht abgefragt."
            append("Google-Guthaben für die Gemini-Stimmen: übrig ${betrag(rest)} $waehrung von ${betrag(gewaehrt)} $waehrung, verbraucht ${betrag(verbraucht)} $waehrung.")
            offen.firstOrNull()?.let { append(" Als Nächstes verfallen ${betrag(it.rest)} $waehrung am ${datum(it.ende)} (${tageText(tageBis(it.ende))}).") }
            naechste?.let { append(" Nächstes Guthaben voraussichtlich am ${DATUM.format(it)} (${tageText(ChronoUnit.DAYS.between(LocalDate.now(), it))})") }
            letzterBetrag?.let { append(", etwa ${betrag(it)} $waehrung") }
            if (naechste != null) append('.')
            append(" Die Vorauszahlung (echtes Geld) wird erst belastet, wenn das Guthaben aufgebraucht ist. ").append(standText()).append('.')
            hinweis?.let { append(" ").append(it) }
        }
    }

    companion object {
        private const val TAG = "JarvisGoogleGuthaben"
        private const val FRISCH_MS = 15 * 60_000L
        private const val STUDIO = "https://aistudio.google.com"
        private const val RPC = "https://alkalimakersuite-pa.clients6.google.com/\$rpc/google.internal.alkali.applications.makersuite.v1.MakerSuiteService/"
        /** Öffentlicher Browser-Schlüssel der AI-Studio-Webseite (steht in deren Skript, kein Geheimnis von Frank). */
        private const val STUDIO_SCHLUESSEL = "AIzaSyDdP816MREB3SkjZO04QXbjsigfcI0GWOs"
        /** Anmeldeseite für den eingebetteten Browser; danach landet er in der Abrechnung von AI Studio. */
        const val LOGIN_URL = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Faistudio.google.com%2Fbilling"
        private val CLIENT = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
        private val JSON_PB = "application/json+protobuf".toMediaType()
        private val sperre = Mutex()
        private val DATUM: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)
        private val _frisch = kotlinx.coroutines.flow.MutableStateFlow<Stand?>(null)
        val frisch: kotlinx.coroutines.flow.StateFlow<Stand?> = _frisch

        fun betrag(w: Double): String = String.format(Locale.GERMAN, "%,.2f", w)
        fun datum(sek: Long): String = DATUM.format(Instant.ofEpochSecond(sek).atZone(ZoneId.systemDefault()))
        fun tageBis(sek: Long): Long = ChronoUnit.DAYS.between(LocalDate.now(), Instant.ofEpochSecond(sek).atZone(ZoneId.systemDefault()).toLocalDate())
        fun tageText(t: Long): String = when { t <= 0L -> "heute"; t == 1L -> "morgen"; else -> "in $t Tagen" }

        private fun datei(context: Context) = File(context.filesDir, "google_guthaben.json")

        private fun cookies(url: String): String = runCatching { CookieManager.getInstance().getCookie(url) }.getOrNull().orEmpty()

        private fun sapisid(): String? = cookies(STUDIO).split(';').map { it.trim() }
            .firstOrNull { it.startsWith("SAPISID=") }?.substringAfter('=')?.takeIf { it.isNotBlank() }

        /** true, wenn Frank in Jarvis bei Google angemeldet ist (Sitzungs-Cookie SAPISID vorhanden). */
        fun angemeldet(): Boolean = sapisid() != null

        /** Löscht nur die Google-Cookies (die Anmeldung bei der Claude Console bleibt) und den gespeicherten Stand. */
        suspend fun abmelden(context: Context) = withContext(Dispatchers.IO) {
            sperre.withLock {
                val cm = CookieManager.getInstance()
                listOf("https://google.com", "https://accounts.google.com", STUDIO).forEach { u ->
                    cookies(u).split(';').map { it.substringBefore('=').trim() }.filter { it.isNotEmpty() }.forEach { name ->
                        cm.setCookie(u, "$name=; Max-Age=0; Path=/; Domain=.google.com")
                        cm.setCookie(u, "$name=; Max-Age=0; Path=/")
                    }
                }
                cm.flush()
                datei(context.applicationContext).delete()
            }
        }

        private fun geld(a: JSONArray?): Double {
            if (a == null) return 0.0
            val einheiten = a.optString(1).toLongOrNull() ?: 0L
            return einheiten + a.optLong(2) / 1_000_000_000.0
        }

        fun gespeichert(context: Context): Stand? = runCatching { lies(JSONObject(datei(context).readText())) }.getOrNull()

        private fun lies(j: JSONObject): Stand {
            val g = j.getJSONArray("gutschriften")
            return Stand(
                (0 until g.length()).map { g.getJSONObject(it).let { o -> Gutschrift(o.getDouble("betrag"), o.getDouble("verbraucht"), o.getDouble("rest"), o.getLong("ende")) } },
                j.optString("waehrung", "€"), j.optLong("letzteGewaehrung"), if (j.has("letzterBetrag")) j.getDouble("letzterBetrag") else null, j.optLong("geholt"),
            )
        }

        private fun schreib(context: Context, s: Stand) {
            val g = JSONArray()
            s.gutschriften.forEach { g.put(JSONObject().put("betrag", it.betrag).put("verbraucht", it.verbraucht).put("rest", it.rest).put("ende", it.ende)) }
            val j = JSONObject().put("gutschriften", g).put("waehrung", s.waehrung).put("letzteGewaehrung", s.letzteGewaehrung).put("geholt", s.geholt)
            s.letzterBetrag?.let { j.put("letzterBetrag", it) }
            datei(context).writeText(j.toString())
        }

        /** Wie bei Anthropic: der gespeicherte Stand, wenn jünger als 15 Minuten (außer [erzwingen]), sonst frisch. */
        suspend fun stand(context: Context, erzwingen: Boolean = false): Stand = withContext(Dispatchers.IO) {
            val app = context.applicationContext
            if (!angemeldet()) return@withContext Stand(emptyList(), "€", 0, null, 0,
                "Für das Google-Guthaben bitte unter Einstellungen → Guthaben einmal bei Google anmelden.")
            sperre.withLock {
                val alt = gespeichert(app)
                if (!erzwingen && alt != null && System.currentTimeMillis() - alt.geholt < FRISCH_MS) return@withLock alt
                try {
                    val ua = runCatching { WebSettings.getDefaultUserAgent(app) }.getOrDefault("Mozilla/5.0")
                    val konto = rpc("ListBillingAccounts", "[]", ua).optJSONArray(0)?.optJSONArray(0)
                        ?: throw IllegalStateException("Bei Google ist kein Rechnungskonto für AI Studio zu sehen.")
                    val kontoName = konto.getString(0)
                    val waehrung = konto.optString(2).let { if (it == "EUR" || it.isBlank()) "€" else it }
                    val zeilen = rpc("BatchGetCredits", JSONArray().put(JSONArray().put(kontoName)).toString(), ua).optJSONArray(0) ?: JSONArray()
                    val jetzt = System.currentTimeMillis() / 1000
                    val gutschriften = (0 until zeilen.length()).mapNotNull { i ->
                        val z = zeilen.optJSONArray(i) ?: return@mapNotNull null
                        val ende = z.optJSONArray(8)?.optString(0)?.toLongOrNull() ?: 0L
                        if (ende in 1 until jetzt) return@mapNotNull null
                        Gutschrift(geld(z.optJSONArray(2)), geld(z.optJSONArray(3)), geld(z.optJSONArray(4)), ende)
                    }
                    // Die monatlichen Gewährungen: jeweils Beginn (Index 5) und Betrag (Index 3); die jüngste zählt.
                    var letzte = 0L
                    var letzterBetrag: Double? = null
                    runCatching {
                        val gdp = rpc("GetGdpProfileAndBenefits", "[]", ua).optJSONArray(2) ?: JSONArray()
                        for (i in 0 until gdp.length()) {
                            val b = gdp.optJSONArray(i) ?: continue
                            val beginn = b.optJSONArray(5)?.optString(0)?.toLongOrNull() ?: continue
                            if (beginn > letzte) { letzte = beginn; letzterBetrag = geld(b.optJSONArray(3)) }
                        }
                    }.onFailure { Log.w(TAG, "Gewährungen nicht lesbar", it) }
                    Stand(gutschriften, waehrung, letzte, letzterBetrag, System.currentTimeMillis()).also { schreib(app, it); _frisch.value = it }
                } catch (f: Exception) { // no-cancellation-rethrow
                    Log.w(TAG, "Abfrage fehlgeschlagen", f)
                    Logbuch.netz("Google-Guthaben nicht abrufbar", f.message ?: f.javaClass.simpleName)
                    val grund = f.message ?: "Abfrage gerade nicht möglich (${f.javaClass.simpleName})."
                    if (alt != null) Stand(alt.gutschriften, alt.waehrung, alt.letzteGewaehrung, alt.letzterBetrag, alt.geholt, "Neue Abfrage gescheitert, das ist der letzte Stand: $grund")
                    else Stand(emptyList(), "€", 0, null, 0, grund)
                }
            }
        }

        /** Ein Aufruf der AI-Studio-Schnittstelle mit der Google-Sitzung (SAPISIDHASH wie im Browser). */
        private fun rpc(methode: String, koerper: String, ua: String): JSONArray {
            val sap = sapisid() ?: throw IllegalStateException("Die Google-Anmeldung in Jarvis fehlt. Bitte unter Einstellungen → Guthaben anmelden.")
            val ts = System.currentTimeMillis() / 1000
            val hash = MessageDigest.getInstance("SHA-1").digest("$ts $sap $STUDIO".toByteArray()).joinToString("") { "%02x".format(it) }
            val anfrage = Request.Builder().url(RPC + methode)
                .header("Cookie", cookies(RPC))
                .header("Authorization", "SAPISIDHASH ${ts}_$hash")
                .header("X-Goog-Api-Key", STUDIO_SCHLUESSEL)
                .header("X-Goog-AuthUser", "0")
                .header("Origin", STUDIO).header("Referer", "$STUDIO/")
                .header("User-Agent", ua)
                .post(koerper.toRequestBody(JSON_PB)).build()
            return CLIENT.newCall(anfrage).execute().use { a ->
                val text = a.body?.string().orEmpty()
                if (a.code == 401 || a.code == 403) throw IllegalStateException(
                    "Die Google-Anmeldung in Jarvis ist abgelaufen (${a.code}). Bitte unter Einstellungen → Guthaben neu anmelden.")
                if (!a.isSuccessful) throw IllegalStateException("AI Studio antwortet mit Fehler ${a.code}.")
                runCatching { JSONArray(text) }.getOrElse { throw IllegalStateException("AI Studio lieferte keine lesbare Antwort (Schnittstelle geändert?).") }
            }
        }
    }
}
