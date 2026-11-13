package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Wetter an Franks Wohnort, als kleine eigene Datenbank: stündliche Vorhersage für sieben Tage, ausschließlich aus dem
 * DWD-Modell ICON (Open-Meteo-Endpunkt dwd-icon, icon_seamless: ICON-D2, ICON-EU, ICON global, Regenrisiko aus ICON-EPS),
 * kostenlos und ohne Schlüssel. Aufgefrischt bei jeder Synchronisierung und vor dem Lesen, sobald der Stand älter als
 * 45 Minuten ist (ab 90 Minuten mit Warten), damit immer der neueste DWD-Lauf gilt. Kann nicht aufgefrischt werden und ist
 * der Stand älter als drei Stunden, steht VERALTET davor.
 * So kann Jarvis das Wetter jederzeit mitbedenken, ohne erst fragen zu müssen.
 */
class WetterFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "wetter"
    override val name = "Wetter"
    override val beschreibung = "Wettervorhersage für die nächsten sieben Tage, stundengenau."
    override val hinweise =
        "Jarvis kennt die Wettervorhersage an Franks Wohnort für sieben Tage, stundengenau aus dem DWD-Modell ICON (Temperatur, Sonne, Bewölkung, Regen, Wind mit Böen) und nennt den Stand des Abrufs. Steht VERALTET dabei, sag Frank das ausdrücklich. Nutze wetter_lesen für „Wie wird das Wetter?“ und für " +
            "„Kann ich morgen um 15 Uhr laufen gehen?“ – mit datum und uhrzeit prüft es zugleich Dienstplan, Schlafzeiten und Termine. " +
            "Bei Vorhaben im Freien (laufen, Wald, Rad, Garten) nenne das Wetter von dir aus, wenn es dagegen spricht."

    private val datei get() = File(context.filesDir, "wetter.json")
    private val e get() = Einstellungen.get(context)

    override fun stoerung(): String? = null

    /** Eine Stunde der Vorhersage. */
    class Stunde(
        val zeit: LocalDateTime, val temperatur: Double, val regenRisiko: Int, val regenMm: Double, val wind: Double, val code: Int,
        val wolken: Int = -1, val sonneMinuten: Int = -1, val boeen: Double = -1.0,
    ) {
        val lage: String get() = lageText(code)
    }

    private fun lade(): JSONObject? = runCatching { JSONObject(datei.readText()) }.getOrNull()

    /** Immer nur ein Abruf zur Zeit; ein zweiter wartet und übernimmt dessen Ergebnis. */
    private val sperre = Mutex()
    /** Für das Auffrischen im Hintergrund, während schon mit dem gespeicherten Stand geantwortet wird. */
    private val hintergrund = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Holt die Vorhersage frisch. true = gelungen. */
    suspend fun synchronisiere(): Boolean = withContext(Dispatchers.IO) {
        val beginn = System.currentTimeMillis()
        sperre.withLock {
            // Hat ein gleichzeitiger Aufruf inzwischen frisch geholt, gilt das auch für diesen.
            if (lade()?.let { it.optString("ort") == e.wetterOrt && it.optLong("geholt") >= beginn } == true) return@withLock true
            runCatching {
                // Nur DWD: eigener Endpunkt dwd-icon mit icon_seamless (ICON-D2 2 km für zwei Tage, dann ICON-EU und ICON global).
                val adresse = "https://api.open-meteo.com/v1/dwd-icon?latitude=${e.wetterBreite}&longitude=${e.wetterLaenge}&timezone=auto&forecast_days=7&models=icon_seamless" +
                    "&hourly=temperature_2m,precipitation_probability,precipitation,weather_code,wind_speed_10m,wind_gusts_10m,cloud_cover,sunshine_duration" +
                    "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max,sunrise,sunset,sunshine_duration"
                val antwort = CLIENT.newCall(Request.Builder().url(adresse).build()).execute().use { a ->
                    check(a.isSuccessful) { "Open-Meteo (DWD) antwortet mit ${a.code}" }
                    JSONObject(a.body?.string().orEmpty())
                }
                check(antwort.has("hourly")) { "Antwort ohne Vorhersage" }
                // Startzeit des neuesten verfügbaren ICON-D2-Laufs (Sekunden), damit der Stand des Modells erkennbar ist.
                val lauf = runCatching {
                    CLIENT.newCall(Request.Builder().url("https://api.open-meteo.com/data/dwd_icon_d2/static/meta.json").build()).execute().use { a ->
                        JSONObject(a.body?.string().orEmpty()).optLong("last_run_initialisation_time")
                    }
                }.getOrDefault(0L)
                // Erst daneben schreiben, dann umbenennen: Ein gleichzeitiges Lesen sieht nie eine halbe Datei.
                val inhalt = antwort.put("geholt", System.currentTimeMillis()).put("ort", e.wetterOrt).put("quelle", QUELLE).put("lauf", lauf).toString()
                val neu = File(context.filesDir, "wetter.json.neu")
                neu.writeText(inhalt)
                if (!neu.renameTo(datei)) { datei.writeText(inhalt); neu.delete() }
                Logbuch.dauer("Wetter abgerufen", System.currentTimeMillis() - beginn)
                true
            }.onFailure { Log.w("JarvisWetter", "Abruf fehlgeschlagen", it); Logbuch.netz("Wetter-Abruf fehlgeschlagen", it.message ?: it.javaClass.simpleName) }.getOrDefault(false)
        }
    }

    /**
     * Vor dem Lesen nachholen, wenn nichts da ist, der Ort gewechselt hat, der Stand noch nicht vom DWD stammt oder älter
     * als 90 Minuten ist (dann mit Warten). Ist er 45 bis 90 Minuten alt, kommt die Antwort sofort, und aufgefrischt wird im
     * Hintergrund. So gilt immer der neueste DWD-Lauf (ICON-D2 rechnet alle drei Stunden neu).
     */
    private suspend fun frisch(): JSONObject? {
        val stand = lade()
        val alter = System.currentTimeMillis() - (stand?.optLong("geholt") ?: 0L)
        when {
            stand == null || stand.optString("ort") != e.wetterOrt || stand.optString("quelle") != QUELLE || alter >= 90 * 60_000L -> synchronisiere()
            alter > 45 * 60_000L -> if (!sperre.isLocked) hintergrund.launch { synchronisiere() }
        }
        return lade()
    }

    /** Ein Stand gilt als veraltet, wenn er nicht vom DWD stammt oder älter als drei Stunden ist. */
    private fun veraltet(stand: JSONObject): Boolean =
        stand.optString("quelle") != QUELLE || System.currentTimeMillis() - stand.optLong("geholt") > 3 * 3_600_000L

    private fun stunden(stand: JSONObject): List<Stunde> {
        val h = stand.getJSONObject("hourly")
        val zeiten = h.getJSONArray("time")
        return (0 until zeiten.length()).map { i ->
            Stunde(
                LocalDateTime.parse(zeiten.getString(i)), h.getJSONArray("temperature_2m").optDouble(i, 0.0), h.getJSONArray("precipitation_probability").optInt(i, 0),
                h.getJSONArray("precipitation").optDouble(i, 0.0), h.getJSONArray("wind_speed_10m").optDouble(i, 0.0), h.getJSONArray("weather_code").optInt(i, 0),
                h.optJSONArray("cloud_cover")?.optInt(i, -1) ?: -1, h.optJSONArray("sunshine_duration")?.optDouble(i, -60.0)?.let { (it / 60).roundToInt() } ?: -1,
                h.optJSONArray("wind_gusts_10m")?.optDouble(i, -1.0) ?: -1.0,
            )
        }
    }

    /** Das Wetter zu einem Zeitpunkt aus dem gespeicherten Stand, ohne Netz. null, wenn der Stand das nicht abdeckt oder veraltet ist. */
    fun zurZeit(tag: LocalDate, minuten: Int): Stunde? = lade()?.takeUnless(::veraltet)?.let { stand ->
        runCatching { stunden(stand).firstOrNull { it.zeit.toLocalDate() == tag && it.zeit.hour == minuten / 60 } }.getOrNull()
    }

    /** Ein Satz, wenn das Wetter gegen ein Vorhaben im Freien spricht oder klar dafür; sonst null. */
    fun einschaetzung(s: Stunde): String = buildString {
        append(s.temperatur.roundToInt()).append(" Grad, ").append(s.lage)
        append(", ").append(sonneText(s))
        if (s.wolken >= 0) append(", Bewölkung ").append(s.wolken).append(" Prozent")
        append(", ").append(if (s.regenMm >= 0.1) "Regen ${zahl(s.regenMm)} mm in der Stunde" else "trocken").append(" (Regenrisiko ").append(s.regenRisiko).append(" Prozent)")
        append(", ").append(windText(s))
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

    /** Wie [vorschau], frischt aber vorher auf (fürs Live-Gedächtnis): ab einer Stunde im Hintergrund, ab sechs Stunden mit Warten. */
    suspend fun aktuell(tage: Int = 3): String { frisch(); return vorschau(tage) }

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
            append(standText(stand, "Wetter für " + stand.optString("ort").ifEmpty { "den Wohnort" })).append(":\n")
            for (i in 0 until zeiten.length()) {
                val tag = LocalDate.parse(zeiten.getString(i))
                if (tag.isBefore(ab) || ChronoUnit.DAYS.between(ab, tag) >= tage) continue
                append("- ").append(tagName(tag, heute)).append(": ").append(lageText(d.getJSONArray("weather_code").optInt(i)))
                append(", ").append(d.getJSONArray("temperature_2m_min").optDouble(i).roundToInt()).append(" bis ").append(d.getJSONArray("temperature_2m_max").optDouble(i).roundToInt()).append(" Grad")
                append(", Regen ").append(zahl(d.getJSONArray("precipitation_sum").optDouble(i))).append(" mm (Risiko bis ").append(d.getJSONArray("precipitation_probability_max").optInt(i)).append(" %)")
                append(", Wind bis ").append(d.getJSONArray("wind_speed_10m_max").optDouble(i).roundToInt()).append(" km/h")
                d.optJSONArray("sunshine_duration")?.optDouble(i, -1.0)?.takeIf { it >= 0 }?.let { append(", Sonnenschein ").append(zahl(Math.round(it / 360.0) / 10.0)).append(" Std.") }
                append(", hell ").append(d.getJSONArray("sunrise").optString(i).takeLast(5)).append("–").append(d.getJSONArray("sunset").optString(i).takeLast(5)).append('\n')
                if (mitStunden) {
                    // Alle drei Stunden über den Tag, damit sich trockene Fenster erkennen lassen.
                    val zeile = alle.filter { it.zeit.toLocalDate() == tag && it.zeit.hour in 6..21 && it.zeit.hour % 3 == 0 }
                        .joinToString("; ") { kurz(it) }
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
                "dauer" to zahl("Wie lange das Vorhaben dauert, in Minuten, nur wenn bekannt (Vorgabe 60)."),
            ),
            nurLesen = true,
        ) { a ->
            val stand = frisch() ?: return@Werkzeug Ergebnis("Die Wettervorhersage ließ sich nicht abrufen (kein Internet?).", fehler = true)
            val heute = LocalDate.now()
            val tag = datum(a.optString("datum")) ?: heute
            // Gemeinsamer Uhrzeit-Leser: „21:00“ bleibt 21 Uhr (früher wurde daraus 2 Uhr nachts).
            val minuten = Mitdenken.uhrzeitMinuten(a.optString("uhrzeit"))
            val dauer = a.optInt("dauer", 60).coerceIn(1, 24 * 60)
            if (ChronoUnit.DAYS.between(heute, tag) !in 0..6) {
                // Außerhalb der Vorhersage: kein Wetter, aber Dienst und Termine lassen sich trotzdem prüfen.
                val rest = Mitdenken.hinweise(context, tag, minuten, a.optString("vorhaben"), mitWetter = false, dauer = dauer)
                return@Werkzeug Ergebnis(
                    "Für ${tagName(tag, heute)} gibt es noch keine Wettervorhersage; sie reicht sieben Tage, bis ${tagName(heute.plusDays(6), heute)}.\n" +
                        if (rest.isEmpty()) "Dienstplan, Schlafzeiten und Termine sprechen nicht dagegen." else rest.joinToString("\n") { "Zu beachten: $it" },
                )
            }
            if (minuten == null) return@Werkzeug Ergebnis(tagesText(stand, tag, a.optInt("tage", 3).coerceIn(1, 7), mitStunden = true))
            val umher = stunden(stand).filter { it.zeit.toLocalDate() == tag && it.zeit.hour in (minuten / 60 - 1)..(minuten / 60 + 2) }
            Ergebnis(buildString {
                append(standText(stand, "Wetter")).append('\n')
                append(tagName(tag, heute)).append(" um ").append("%d:%02d".format(minuten / 60, minuten % 60)).append(" Uhr")
                a.optString("vorhaben").takeIf { it.isNotBlank() }?.let { append(", Vorhaben: ").append(it) }
                append(".\nWetter: ").append(umher.firstOrNull { it.zeit.hour == minuten / 60 }?.let(::einschaetzung) ?: "keine Angabe für diese Stunde").append('\n')
                append("Stunden drumherum: ").append(umher.joinToString("; ") { kurz(it) }).append('\n')
                // Dienst, Schlaf und Termine dazu: derselbe Blick, den Jarvis auch beim Anlegen von Aufgaben wirft.
                Mitdenken.hinweise(context, tag, minuten, a.optString("vorhaben"), mitWetter = false, dauer = dauer).takeIf { it.isNotEmpty() }?.let { append(it.joinToString("\n") { h -> "Zu beachten: $h" }) }
                    ?: append("Dienstplan, Schlafzeiten und Termine sprechen nicht dagegen.")
            }.trim())
        },
    )

    private fun alter(stand: JSONObject): String {
        val minuten = (System.currentTimeMillis() - stand.optLong("geholt")) / 60_000
        return if (minuten < 90) "vor $minuten Minuten" else "vor ${minuten / 60} Stunden"
    }

    /** Kopfzeile mit Quelle, Modelllauf und Abrufzeit; ist der Stand zu alt, steht VERALTET davor. */
    private fun standText(stand: JSONObject, titel: String): String = buildString {
        if (veraltet(stand)) append("VERALTET, nicht als aktuell nennen: ")
        append(titel).append(", ")
        append(if (stand.optString("quelle") == QUELLE) "DWD-Modell ICON" else "alte Vorhersage, nicht vom DWD")
        stand.optLong("lauf").takeIf { it > 0 }?.let {
            append(", Lauf ").append(java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d.M. HH:mm 'Uhr'", Locale.GERMAN)))
        }
        val geholt = java.time.Instant.ofEpochMilli(stand.optLong("geholt")).atZone(java.time.ZoneId.systemDefault())
        append(", abgerufen ").append(geholt.format(DateTimeFormatter.ofPattern("d.M. HH:mm 'Uhr'", Locale.GERMAN))).append(" (").append(alter(stand)).append(')')
    }

    private fun sonneText(s: Stunde): String = when {
        s.sonneMinuten < 0 -> "Sonne unbekannt"
        s.sonneMinuten >= 50 -> "sonnig"
        s.sonneMinuten >= 10 -> "zeitweise Sonne (${s.sonneMinuten} Minuten)"
        else -> "keine Sonne"
    }

    private fun windText(s: Stunde): String = buildString {
        append(
            when {
                s.wind >= 50 -> "stürmisch"
                s.wind >= 30 -> "windig"
                s.wind >= 15 -> "mäßiger Wind"
                s.wind >= 5 -> "schwacher Wind"
                else -> "kaum Wind"
            },
        ).append(' ').append(s.wind.roundToInt()).append(" km/h")
        if (s.boeen >= 0) append(", Böen ").append(s.boeen.roundToInt()).append(" km/h")
    }

    /** Eine Stunde in einer Zeile: Temperatur, Lage, Sonne, Wolken, Regen, Wind. */
    private fun kurz(s: Stunde): String = buildString {
        append(s.zeit.hour).append(" Uhr ").append(s.temperatur.roundToInt()).append("° ").append(s.lage)
        append(", ").append(sonneText(s))
        if (s.wolken >= 0) append(", Wolken ").append(s.wolken).append(" %")
        append(", ").append(if (s.regenMm >= 0.1) "Regen ${zahl(s.regenMm)} mm" else "trocken").append(" (").append(s.regenRisiko).append(" %)")
        append(", Wind ").append(s.wind.roundToInt()).append(if (s.boeen >= 0) "/${s.boeen.roundToInt()}" else "").append(" km/h")
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
        /** Kennung im gespeicherten Stand: nur Stände mit dieser Quelle gelten als DWD-Vorhersage. */
        private const val QUELLE = "dwd_icon_seamless"
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
