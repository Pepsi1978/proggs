package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Wetter an Franks Wohnort, als kleine eigene Datenbank: stündliche Vorhersage für sieben Tage von Open-Meteo
 * (kostenlos, ohne Schlüssel), bei jeder Synchronisierung und sonst höchstens stündlich aufgefrischt und lokal abgelegt.
 * So kann Jarvis das Wetter jederzeit mitbedenken, ohne erst fragen zu müssen.
 */
class WetterFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "wetter"
    override val name = "Wetter"
    override val beschreibung = "Wettervorhersage für die nächsten sieben Tage, stundengenau."
    override val hinweise =
        "Jarvis kennt die Wettervorhersage an Franks Wohnort für sieben Tage, stundengenau (Temperatur, Regen, Wind). Nutze wetter_lesen für „Wie wird das Wetter?“ und für " +
            "„Kann ich morgen um 15 Uhr laufen gehen?“ – mit datum und uhrzeit prüft es zugleich Dienstplan, Schlafzeiten und Termine. " +
            "Bei Vorhaben im Freien (laufen, Wald, Rad, Garten) nenne das Wetter von dir aus, wenn es dagegen spricht."

    private val datei get() = File(context.filesDir, "wetter.json")
    private val e get() = Einstellungen.get(context)

    override fun stoerung(): String? = null

    /** Eine Stunde der Vorhersage. */
    class Stunde(val zeit: LocalDateTime, val temperatur: Double, val regenRisiko: Int, val regenMm: Double, val wind: Double, val code: Int) {
        val lage: String get() = lageText(code)
    }

    private fun lade(): JSONObject? = runCatching { JSONObject(datei.readText()) }.getOrNull()

    /** Holt die Vorhersage frisch. true = gelungen. */
    suspend fun synchronisiere(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val adresse = "https://api.open-meteo.com/v1/forecast?latitude=${e.wetterBreite}&longitude=${e.wetterLaenge}&timezone=auto&forecast_days=7" +
                "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,sunrise,sunset"
            val antwort = CLIENT.newCall(Request.Builder().url(adresse).build()).execute().use { a ->
                check(a.isSuccessful) { "Open-Meteo antwortet mit ${a.code}" }
                JSONObject(a.body?.string().orEmpty())
            }
            check(antwort.has("hourly")) { "Antwort ohne Vorhersage" }
            datei.writeText(antwort.put("geholt", System.currentTimeMillis()).put("ort", e.wetterOrt).toString())
            true
        }.onFailure { Log.w("JarvisWetter", "Abruf fehlgeschlagen", it) }.getOrDefault(false)
    }

    /** Vor dem Lesen nachholen, wenn nichts da ist, der Ort gewechselt hat oder der Stand älter als eine Stunde ist. */
    private suspend fun frisch(): JSONObject? {
        val stand = lade()
        if (stand == null || stand.optString("ort") != e.wetterOrt || System.currentTimeMillis() - stand.optLong("geholt") > 60 * 60_000L) synchronisiere()
        return lade()
    }

    private fun stunden(stand: JSONObject): List<Stunde> {
        val h = stand.getJSONObject("hourly")
        val zeiten = h.getJSONArray("time")
        return (0 until zeiten.length()).map { i ->
            Stunde(
                LocalDateTime.parse(zeiten.getString(i)), h.getJSONArray("temperature_2m").optDouble(i, 0.0), h.getJSONArray("precipitation_probability").optInt(i, 0),
                h.getJSONArray("precipitation").optDouble(i, 0.0), h.getJSONArray("wind_speed_10m").optDouble(i, 0.0), h.getJSONArray("weather_code").optInt(i, 0),
            )
        }
    }

    /** Das Wetter zu einem Zeitpunkt aus dem gespeicherten Stand, ohne Netz. null, wenn der Stand das nicht abdeckt. */
    fun zurZeit(tag: LocalDate, minuten: Int): Stunde? = lade()?.let { stand ->
        runCatching { stunden(stand).firstOrNull { it.zeit.toLocalDate() == tag && it.zeit.hour == minuten / 60 } }.getOrNull()
    }

    /** Ein Satz, wenn das Wetter gegen ein Vorhaben im Freien spricht oder klar dafür; sonst null. */
    fun einschaetzung(s: Stunde): String = buildString {
        append(s.temperatur.roundToInt()).append(" Grad, ").append(s.lage)
        if (s.regenRisiko >= 30 || s.regenMm >= 0.2) append(", Regenrisiko ").append(s.regenRisiko).append(" Prozent").append(if (s.regenMm >= 0.2) " (${zahl(s.regenMm)} mm in der Stunde)" else "")
        if (s.wind >= 30) append(", Wind ").append(s.wind.roundToInt()).append(" km/h")
        append(
            when {
                s.code >= 95 -> " – Gewitter, draußen besser nicht."
                s.regenMm >= 2.0 || (s.regenRisiko >= 80 && s.regenMm >= 0.5) -> " – deutlich nass, für draußen ungünstig."
                s.regenRisiko >= 50 -> " – kann nass werden."
                s.wind >= 50 -> " – stürmisch."
                s.temperatur >= 30 -> " – sehr warm, Belastung anpassen."
                s.temperatur <= -5 -> " – sehr kalt."
                else -> " – für draußen gut."
            },
        )
    }

    /** Tagesübersicht ab heute, für Tagesdatenbank und Tagesauswertung. */
    fun vorschau(tage: Int = 3): String {
        val stand = lade() ?: return "NICHT VERFÜGBAR: noch nicht abgerufen."
        return runCatching { tagesText(stand, LocalDate.now(), tage, mitStunden = true) }.getOrDefault("NICHT VERFÜGBAR: Vorhersage nicht lesbar.")
    }

    private fun tagesText(stand: JSONObject, ab: LocalDate, tage: Int, mitStunden: Boolean): String {
        val d = stand.getJSONObject("daily")
        val zeiten = d.getJSONArray("time")
        val alle = stunden(stand)
        val heute = LocalDate.now()
        return buildString {
            append("Wetter für ").append(stand.optString("ort").ifEmpty { "den Wohnort" }).append(", Stand ").append(alter(stand)).append(":\n")
            for (i in 0 until zeiten.length()) {
                val tag = LocalDate.parse(zeiten.getString(i))
                if (tag.isBefore(ab) || ChronoUnit.DAYS.between(ab, tag) >= tage) continue
                append("- ").append(tagName(tag, heute)).append(": ").append(lageText(d.getJSONArray("weather_code").optInt(i)))
                append(", ").append(d.getJSONArray("temperature_2m_min").optDouble(i).roundToInt()).append(" bis ").append(d.getJSONArray("temperature_2m_max").optDouble(i).roundToInt()).append(" Grad")
                append(", Regen ").append(zahl(d.getJSONArray("precipitation_sum").optDouble(i))).append(" mm (Risiko bis ").append(d.getJSONArray("precipitation_probability_max").optInt(i)).append(" %)")
                append(", Wind bis ").append(d.getJSONArray("wind_speed_10m_max").optDouble(i).roundToInt()).append(" km/h")
                append(", Sonne ").append(d.getJSONArray("sunrise").optString(i).takeLast(5)).append("–").append(d.getJSONArray("sunset").optString(i).takeLast(5)).append('\n')
                if (mitStunden) {
                    // Alle drei Stunden über den Tag, damit sich trockene Fenster erkennen lassen.
                    val zeile = alle.filter { it.zeit.toLocalDate() == tag && it.zeit.hour in 6..21 && it.zeit.hour % 3 == 0 }
                        .joinToString("; ") { "${it.zeit.hour} Uhr ${it.temperatur.roundToInt()}° ${it.lage}" + if (it.regenRisiko >= 30) " (${it.regenRisiko} %)" else "" }
                    if (zeile.isNotEmpty()) append("  Verlauf: ").append(zeile).append('\n')
                }
            }
        }.trim()
    }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "wetter_lesen",
            titel = "Wetter und Zeitpunkt prüfen",
            beschreibung = "Jarvis: Wettervorhersage an Franks Wohnort für die nächsten sieben Tage. Ohne Angaben kommen heute, morgen und übermorgen mit Tagesverlauf. " +
                "Mit datum und uhrzeit kommt das Wetter genau zu diesem Zeitpunkt UND die Prüfung gegen Dienstplan, Schlafzeiten und Termine – für Fragen wie " +
                "„Kann ich morgen um 15 Uhr laufen gehen?“, „Passt Samstagvormittag für den Garten?“. Gib bei vorhaben an, was Frank vorhat.",
            schema = schema(
                "datum" to text("heute, morgen, uebermorgen oder JJJJ-MM-TT."),
                "uhrzeit" to text("HH:MM, wenn es um einen bestimmten Zeitpunkt geht."),
                "tage" to zahl("Wie viele Tage ab datum (Vorgabe 3, höchstens 7), wenn keine uhrzeit angegeben ist."),
                "vorhaben" to text("Was Frank zu dem Zeitpunkt vorhat, zum Beispiel „Laufen im Wald“."),
            ),
            nurLesen = true,
        ) { a ->
            val stand = frisch() ?: return@Werkzeug Ergebnis("Die Wettervorhersage ließ sich nicht abrufen (kein Internet?).", fehler = true)
            val heute = LocalDate.now()
            val tag = datum(a.optString("datum")) ?: heute
            val minuten = Regex("^\\s*([01]?\\d|2[0-3])[:.]?([0-5]\\d)?").find(a.optString("uhrzeit"))?.takeIf { a.optString("uhrzeit").isNotBlank() }
                ?.let { it.groupValues[1].toInt() * 60 + (it.groupValues[2].toIntOrNull() ?: 0) }
            if (ChronoUnit.DAYS.between(heute, tag) !in 0..6) {
                // Außerhalb der Vorhersage: kein Wetter, aber Dienst und Termine lassen sich trotzdem prüfen.
                val rest = Mitdenken.hinweise(context, tag, minuten, a.optString("vorhaben"), mitWetter = false)
                return@Werkzeug Ergebnis(
                    "Für ${tagName(tag, heute)} gibt es noch keine Wettervorhersage; sie reicht sieben Tage, bis ${tagName(heute.plusDays(6), heute)}.\n" +
                        if (rest.isEmpty()) "Dienstplan, Schlafzeiten und Termine sprechen nicht dagegen." else rest.joinToString("\n") { "Zu beachten: $it" },
                )
            }
            if (minuten == null) return@Werkzeug Ergebnis(tagesText(stand, tag, a.optInt("tage", 3).coerceIn(1, 7), mitStunden = true))
            val umher = stunden(stand).filter { it.zeit.toLocalDate() == tag && it.zeit.hour in (minuten / 60 - 1)..(minuten / 60 + 2) }
            Ergebnis(buildString {
                append(tagName(tag, heute)).append(" um ").append("%d:%02d".format(minuten / 60, minuten % 60)).append(" Uhr")
                a.optString("vorhaben").takeIf { it.isNotBlank() }?.let { append(", Vorhaben: ").append(it) }
                append(".\nWetter: ").append(umher.firstOrNull { it.zeit.hour == minuten / 60 }?.let(::einschaetzung) ?: "keine Angabe für diese Stunde").append('\n')
                append("Stunden drumherum: ").append(umher.joinToString("; ") { "${it.zeit.hour} Uhr ${it.temperatur.roundToInt()}° ${it.lage}, Regenrisiko ${it.regenRisiko} %" }).append('\n')
                // Dienst, Schlaf und Termine dazu: derselbe Blick, den Jarvis auch beim Anlegen von Aufgaben wirft.
                Mitdenken.hinweise(context, tag, minuten, a.optString("vorhaben"), mitWetter = false).takeIf { it.isNotEmpty() }?.let { append(it.joinToString("\n") { h -> "Zu beachten: $h" }) }
                    ?: append("Dienstplan, Schlafzeiten und Termine sprechen nicht dagegen.")
            }.trim())
        },
    )

    private fun alter(stand: JSONObject): String {
        val minuten = (System.currentTimeMillis() - stand.optLong("geholt")) / 60_000
        return if (minuten < 90) "vor $minuten Minuten" else "vor ${minuten / 60} Stunden"
    }

    private fun tagName(tag: LocalDate, heute: LocalDate): String = when (ChronoUnit.DAYS.between(heute, tag)) {
        0L -> "Heute"; 1L -> "Morgen"; 2L -> "Übermorgen"
        else -> tag.format(DateTimeFormatter.ofPattern("EEEE, d.M.", Locale.GERMAN))
    } + tag.format(DateTimeFormatter.ofPattern(" (EE)", Locale.GERMAN)).takeIf { ChronoUnit.DAYS.between(heute, tag) in 0..2 }.orEmpty()

    private fun datum(text: String): LocalDate? = when (text.trim().lowercase(Locale.GERMAN)) {
        "" -> null
        "heute" -> LocalDate.now()
        "morgen" -> LocalDate.now().plusDays(1)
        "uebermorgen", "übermorgen" -> LocalDate.now().plusDays(2)
        else -> runCatching { LocalDate.parse(text.trim().take(10)) }.getOrNull()
    }

    private fun zahl(wert: Double): String = if (wert == Math.floor(wert)) wert.toLong().toString() else ((wert * 10).roundToInt() / 10.0).toString().replace('.', ',')

    companion object {
        private val CLIENT = OkHttpClient.Builder().connectTimeout(12, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

        /** Wetterlage aus dem WMO-Code, als ein Wort. */
        fun lageText(code: Int): String = when (code) {
            0 -> "klar"; 1 -> "heiter"; 2 -> "wolkig"; 3 -> "bedeckt"
            45, 48 -> "Nebel"
            51, 53, 55, 56, 57 -> "Nieselregen"
            61, 80 -> "leichter Regen"; 63, 81 -> "Regen"; 65, 82 -> "starker Regen"; 66, 67 -> "gefrierender Regen"
            71, 73, 75, 77, 85, 86 -> "Schnee"
            95, 96, 99 -> "Gewitter"
            else -> "wechselhaft"
        }
    }
}
