package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.settings.SecureSettings
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Franks Anthropic-API-Verbrauch im laufenden Monat gegen seinen monatlichen Freibetrag. Anthropic nennt das Restguthaben
 * nicht; der Kostenbericht der Admin-API (`/v1/organizations/cost_report`, USD in Cent als Dezimaltext) liefert aber den
 * Verbrauch seit Monatsanfang (UTC). Umgerechnet wird mit dem Tageskurs der EZB (Frankfurter, ohne Schlüssel).
 * Braucht einen eigenen Admin-Schlüssel (sk-ant-admin…); der normale API-Schlüssel wird dort abgelehnt.
 */
class AnthropicGuthabenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "anthropic_guthaben"
    override val name = "Anthropic-Guthaben"
    override val beschreibung = "Verbrauch der Anthropic-API im laufenden Monat und Rest zum Freibetrag."
    override val hinweise =
        "Fragt Frank nach seinem Anthropic- oder Claude-Guthaben, seinem API-Verbrauch oder was vom Freibetrag übrig ist, nutze anthropic_guthaben."
    override fun stoerung(): String? = null

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "anthropic_guthaben",
            titel = "Anthropic-Guthaben lesen",
            beschreibung = "Jarvis: Franks Verbrauch der Anthropic-API (Claude) im laufenden Monat, sein monatlicher Freibetrag und der Rest, mit Stand der Abfrage. " +
                "Für „Wie viel Anthropic-Guthaben habe ich noch?“, „Was hat Claude diesen Monat gekostet?“. Holt höchstens alle 15 Minuten frisch.",
            schema = schema("frisch" to schalter("true = jetzt neu bei Anthropic abfragen statt des Stands der letzten 15 Minuten.")),
            nurLesen = true,
        ) { a ->
            val s = stand(context, erzwingen = a.optBoolean("frisch"))
            Ergebnis(s.satz(), fehler = s.verbrauchtUsd == null)
        },
    )

    /** Ergebnis einer Abfrage. Ohne [verbrauchtUsd] steht in [hinweis], warum es keine Zahl gibt. */
    class Stand(
        val verbrauchtUsd: Double?,
        /** Euro je Dollar; null = Kurs nie abrufbar, dann nur in Dollar. */
        val kurs: Double?,
        val kursDatum: String,
        val freibetrag: Int,
        val geholt: Long,
        val hinweis: String? = null,
    ) {
        val verbrauchtEur: Double? get() = verbrauchtUsd?.let { u -> kurs?.let { u * it } }
        val restEur: Double? get() = verbrauchtEur?.let { freibetrag - it }

        /** Die Zeile für die Einstellungen, z. B. „Verbraucht diesen Monat: 37,12 € von 100 € · Rest 62,88 €“. */
        fun zeile(): String {
            val usd = verbrauchtUsd ?: return hinweis ?: "Noch nicht abgefragt."
            val eur = verbrauchtEur ?: return "Verbraucht diesen Monat: ${betrag(usd)} $ (Umrechnung in Euro gerade nicht möglich)"
            val rest = freibetrag - eur
            return "Verbraucht diesen Monat: ${betrag(eur)} € von $freibetrag € · " + if (rest >= 0) "Rest ${betrag(rest)} €" else "${betrag(-rest)} € über dem Freibetrag"
        }

        fun standText(): String = if (geholt <= 0) "" else "Stand " + DateTimeFormatter.ofPattern("dd.MM., HH:mm 'Uhr'", Locale.GERMAN)
            .format(Instant.ofEpochMilli(geholt).atZone(ZoneId.systemDefault()))

        /** Für Jarvis im Gespräch: Zahl, Stand und, falls nötig, der Hinweis. */
        fun satz(): String = buildString {
            append(zeile()).append('.')
            if (verbrauchtUsd != null) {
                append(" ").append(standText()).append(". Laut Anthropic ").append(betrag(verbrauchtUsd)).append(" $")
                kurs?.let { append(", Kurs 1 $ = ").append(String.format(Locale.GERMAN, "%.4f", it)).append(" € (EZB ").append(kursDatum).append(")") }
                append("; Kosten seit Monatsanfang (UTC) ohne Steuern, Anthropic meldet mit wenigen Minuten Verzug.")
                hinweis?.let { append(" ").append(it) }
            }
        }
    }

    companion object {
        private const val TAG = "JarvisAnthropicGuthaben"
        private const val FRISCH_MS = 15 * 60_000L
        private val CLIENT = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(25, TimeUnit.SECONDS).build()
        private val sperre = Mutex()

        private fun datei(context: Context) = File(context.filesDir, "anthropic_guthaben.json")
        private fun monat(): String = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM"))

        fun betrag(w: Double): String = String.format(Locale.GERMAN, "%,.2f", w)

        /** Der zuletzt gespeicherte Stand dieses Monats, ohne Netz; null, wenn es keinen gibt. */
        fun gespeichert(context: Context): Stand? = runCatching {
            val j = JSONObject(datei(context).readText())
            if (j.optString("monat") != monat()) return null
            Stand(j.getDouble("usd"), if (j.has("kurs")) j.getDouble("kurs") else null, j.optString("kursDatum"),
                Einstellungen.get(context).anthropicFreibetrag, j.optLong("geholt"))
        }.getOrNull()

        /**
         * Der aktuelle Stand: der gespeicherte, wenn er jünger als 15 Minuten ist (außer [erzwingen]), sonst frisch abgefragt.
         * Scheitert die Abfrage, kommt der Grund, bei einem älteren Stand dieses Monats dieser mit dem Grund als Hinweis.
         */
        suspend fun stand(context: Context, erzwingen: Boolean = false): Stand = withContext(Dispatchers.IO) {
            val app = context.applicationContext
            val freibetrag = Einstellungen.get(app).anthropicFreibetrag
            val schluessel = SecureSettings.get(app).anthropicAdminKey.trim()
            if (schluessel.isEmpty()) return@withContext Stand(null, null, "", freibetrag, 0,
                "Für den Verbrauch bitte unter Einstellungen → Jarvis einen Anthropic-Admin-Schlüssel (sk-ant-admin…) eintragen.")
            if (!schluessel.startsWith("sk-ant-admin")) return@withContext Stand(null, null, "", freibetrag, 0,
                "Der eingetragene Schlüssel ist kein Admin-Schlüssel. Nötig ist einer, der mit sk-ant-admin beginnt, aus der Anthropic-Konsole unter Einstellungen → Admin-Schlüssel.")
            sperre.withLock {
                val alt = gespeichert(app)
                if (!erzwingen && alt != null && System.currentTimeMillis() - alt.geholt < FRISCH_MS) return@withLock alt
                try {
                    val usd = verbrauchUsd(schluessel)
                    val (kurs, kursDatum) = kurs(app)
                    val jetzt = System.currentTimeMillis()
                    val j = JSONObject().put("monat", monat()).put("usd", usd).put("geholt", jetzt).put("kursDatum", kursDatum)
                    kurs?.let { j.put("kurs", it) }
                    datei(app).writeText(j.toString())
                    Stand(usd, kurs, kursDatum, freibetrag, jetzt)
                } catch (f: Exception) { // no-cancellation-rethrow
                    Log.w(TAG, "Abfrage fehlgeschlagen", f)
                    Logbuch.netz("Anthropic-Verbrauch nicht abrufbar", f.message ?: f.javaClass.simpleName)
                    val grund = f.message ?: "Abfrage gerade nicht möglich (${f.javaClass.simpleName})."
                    if (alt != null) Stand(alt.verbrauchtUsd, alt.kurs, alt.kursDatum, freibetrag, alt.geholt, "Neue Abfrage gescheitert, das ist der letzte Stand: $grund")
                    else Stand(null, null, "", freibetrag, 0, grund)
                }
            }
        }

        /** Summe aller Kosten seit Monatsanfang (UTC) in Dollar, über alle Seiten des Berichts. */
        private fun verbrauchUsd(schluessel: String): Double {
            val anfang = ZonedDateTime.now(ZoneOffset.UTC).withDayOfMonth(1).toLocalDate().atStartOfDay(ZoneOffset.UTC)
            var cent = 0.0
            var seite: String? = null
            repeat(10) {
                val url = "https://api.anthropic.com/v1/organizations/cost_report".toHttpUrl().newBuilder()
                    .addQueryParameter("starting_at", anfang.format(DateTimeFormatter.ISO_INSTANT))
                    .addQueryParameter("bucket_width", "1d")
                    .addQueryParameter("limit", "31")
                    .apply { seite?.let { addQueryParameter("page", it) } }
                    .build()
                val antwort = CLIENT.newCall(
                    Request.Builder().url(url).header("x-api-key", schluessel).header("anthropic-version", "2023-06-01").build(),
                ).execute().use { a ->
                    val text = a.body?.string().orEmpty()
                    if (!a.isSuccessful) {
                        val meldung = runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull().orEmpty()
                        throw IllegalStateException(when (a.code) {
                            401 -> "Anthropic lehnt den Admin-Schlüssel ab (401). Bitte einen gültigen Admin-Schlüssel eintragen."
                            403, 404 -> "Anthropic erlaubt die Kostenabfrage nicht (${a.code}). Die Admin-API gibt es nur für Organisationen, nicht für Einzelkonten." +
                                if (meldung.isNotBlank()) " Anthropic sagt: $meldung" else ""
                            429 -> "Anthropic bremst gerade (429); bitte gleich noch einmal versuchen."
                            else -> "Anthropic antwortet mit Fehler ${a.code}" + if (meldung.isNotBlank()) ": $meldung" else "."
                        })
                    }
                    JSONObject(text)
                }
                val tage = antwort.optJSONArray("data") ?: throw IllegalStateException("Der Kostenbericht von Anthropic war leer oder unlesbar.")
                for (i in 0 until tage.length()) {
                    val posten = tage.getJSONObject(i).optJSONArray("results") ?: continue
                    for (k in 0 until posten.length()) {
                        val p = posten.getJSONObject(k)
                        if (p.optString("currency", "USD") != "USD") throw IllegalStateException("Anthropic meldet die Kosten in ${p.optString("currency")}, erwartet war USD.")
                        cent += p.optString("amount", "0").toDoubleOrNull() ?: 0.0
                    }
                }
                seite = antwort.optString("next_page").takeIf { antwort.optBoolean("has_more") && it.isNotBlank() && it != "null" } ?: return cent / 100.0
            }
            return cent / 100.0
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
