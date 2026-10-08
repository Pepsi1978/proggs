package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Log
import de.frank.jarvis.data.Einstellungen
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Franks Tagebuch: Markdown-Dateien `JJJJ-MM-TT_Tagebucheintrag.md` in einem Google-Drive-Ordner.
 * Der eigene Server holt sie mit einem Nur-Lese-Zugang aus Drive (siehe `Jarvis/server`), Jarvis holt sie
 * von dort bei jeder Synchronisierung aufs Handy und liest dann lokal. Nur lesend.
 */
class TagebuchFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "tagebuch"
    override val name = "Tagebuch"
    override val beschreibung = "Franks Tagebucheinträge nach Datum lesen und durchsuchen."
    override val hinweise =
        "Das Tagebuch enthält je Tag einen Eintrag, in dem Frank festhält, was er gemacht, erlebt und gedacht hat. Nutze es für „Was habe ich gestern gemacht?“, " +
            "„Was war letzte Woche los?“, „Wann habe ich zuletzt …?“. Die Einträge sind persönlich: gib sie sachlich wieder, ohne zu werten. Das Tagebuch ist nur lesbar; " +
            "neue Einträge entstehen in ChatGPT, nicht über Jarvis. Für manche Tage gibt es keinen Eintrag."

    private val ordner get() = File(context.filesDir, "tagebuch").apply { mkdirs() }
    private val e get() = Einstellungen.get(context)
    private var zuletztGeholt = 0L
    /** Was der Server zuletzt über den Drive-Zugang gemeldet hat; leer = alles in Ordnung. */
    @Volatile var driveFehler = ""
        private set

    override fun stoerung(): String? = when {
        e.serverHost.isBlank() || e.serverToken.isBlank() -> "Der Server-Schlüssel fehlt in dieser Jarvis-Fassung. In Jarvis unter Einstellungen den Server-Schlüssel eintragen " +
            "(steht am PC in ~/SK/Jarvis/relay.properties unter token) oder Jarvis einmal am PC bauen; danach bleibt er gespeichert."
        driveFehler.isNotEmpty() -> "Der Server kann Google Drive nicht lesen. In Jarvis unter Einstellungen → Tagebuch auf „Berechtigung erneuern“ tippen."
        else -> null
    }

    /** Datum → Datei, neueste zuerst. Das Datum steht am Anfang des Dateinamens. */
    private fun eintraege(): List<Pair<LocalDate, File>> = ordner.listFiles { f -> f.isFile && f.name.endsWith(".md") }.orEmpty()
        .mapNotNull { f -> runCatching { LocalDate.parse(f.name.take(10)) to f }.getOrNull() }.sortedByDescending { it.first }

    /**
     * Holt die Einträge frisch vom Server. Bricht der Abruf ab, bleibt der bisherige Bestand lesbar.
     * Rückgabe: Anzahl der Einträge oder null bei Fehler.
     */
    suspend fun synchronisiere(tage: Int = 4000): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/tagebuch?tage=$tage").header("X-Jarvis-Token", e.serverToken).build()
            val antwort = CLIENT.newCall(anfrage).execute().use { a ->
                check(a.isSuccessful) { "Server antwortet mit ${a.code}" }
                JSONObject(a.body?.string().orEmpty())
            }
            val dateien = antwort.getJSONArray("dateien")
            val namen = HashSet<String>()
            for (i in 0 until dateien.length()) {
                val d = dateien.getJSONObject(i)
                val name = d.getString("name")
                if (!NAME.matches(name)) continue
                namen += name
                File(ordner, name).let { ziel -> if (!ziel.exists() || ziel.readText() != d.getString("text")) ziel.writeText(d.getString("text")) }
            }
            // Was im Drive-Ordner gelöscht wurde, verschwindet auch hier — aber nur, wenn der Server sauber gelesen hat.
            if (antwort.optString("fehler").isEmpty() && tage >= 4000 && namen.isNotEmpty()) ordner.listFiles()?.filter { it.name !in namen }?.forEach { it.delete() }
            zuletztGeholt = System.currentTimeMillis()
            driveFehler = antwort.optString("fehler")
            if (driveFehler.isNotEmpty()) Log.w("JarvisTagebuch", driveFehler)
            eintraege().size
        }.onFailure { Log.w("JarvisTagebuch", "Synchronisierung fehlgeschlagen", it) }.getOrNull()
    }

    /** Vor dem Lesen kurz nachholen, wenn der Bestand leer oder älter als 20 Minuten ist. */
    private suspend fun frisch() { if (eintraege().isEmpty() || System.currentTimeMillis() - zuletztGeholt > 20 * 60_000L) synchronisiere() }

    /** Die letzten Tage im Volltext (begrenzt), für Tagesdatenbank und Tagesauswertung. */
    fun rueckblick(tage: Int = 7, zeichenJeTag: Int = 2500): String {
        val ab = LocalDate.now().minusDays(tage.toLong())
        val liste = eintraege().filter { !it.first.isBefore(ab) }
        if (liste.isEmpty()) return "Kein Tagebucheintrag in den letzten $tage Tagen."
        return liste.joinToString("\n\n") { (tag, datei) -> "### " + lang(tag) + "\n" + datei.readText().trim().let { if (it.length > zeichenJeTag) it.take(zeichenJeTag) + " …" else it } }
    }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "tagebuch_lesen",
            titel = "Tagebuch lesen",
            beschreibung = "Jarvis: liest Franks Tagebucheinträge im Volltext, für einen Tag oder einen Zeitraum. Nutze es für „Was habe ich gestern gemacht?“, " +
                "„Was war in den letzten Tagen los?“, „Was stand am 6. Oktober im Tagebuch?“. Ohne Angaben kommen die letzten 7 Tage. Mit suche nur Einträge, die das Wort enthalten.",
            schema = schema(
                "datum" to text("Ein einzelner Tag: heute, gestern, vorgestern oder JJJJ-MM-TT."),
                "von" to text("Erster Tag eines Zeitraums JJJJ-MM-TT."),
                "bis" to text("Letzter Tag eines Zeitraums JJJJ-MM-TT (Vorgabe: heute)."),
                "suche" to text("Wort oder Wortgruppe; durchsucht dann alle Einträge, wenn kein Zeitraum angegeben ist."),
            ),
            nurLesen = true,
        ) { a ->
            stoerung()?.let { return@Werkzeug Ergebnis(it, fehler = true) }
            frisch()
            val alle = eintraege()
            if (alle.isEmpty()) return@Werkzeug Ergebnis("Es liegen keine Tagebucheinträge vor. " + (stoerung() ?: "Server oder Drive sind gerade nicht erreichbar."), fehler = true)
            val heute = LocalDate.now()
            val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
            val einzeln = datum(a.optString("datum"))
            val bis = einzeln ?: datum(a.optString("bis")) ?: heute
            val von = einzeln ?: datum(a.optString("von")) ?: if (suche.isEmpty()) bis.minusDays(6) else LocalDate.MIN
            var treffer = alle.filter { !it.first.isBefore(von) && !it.first.isAfter(bis) }
            if (suche.isNotEmpty()) treffer = treffer.filter { suche in it.second.readText().lowercase(Locale.GERMAN) }
            if (treffer.isEmpty()) return@Werkzeug Ergebnis(
                "Kein Tagebucheintrag " + (if (einzeln != null) "für ${lang(einzeln)}" else "im Zeitraum") + (if (suche.isEmpty()) "" else " mit „$suche“") +
                    ". Vorhanden sind Einträge von ${lang(alle.last().first)} bis ${lang(alle.first().first)} (${alle.size} Tage).",
            )
            // Viele Tage auf einmal: je Eintrag kürzen, damit die Antwort handhabbar bleibt.
            val jeTag = (60_000 / treffer.size).coerceIn(1500, 20_000)
            Ergebnis("Heute ist ${lang(heute)}. ${treffer.size} Tagebucheintrag/-einträge:\n\n" + treffer.take(40).joinToString("\n\n") { (tag, datei) ->
                "### " + lang(tag) + "\n" + datei.readText().trim().let { if (it.length > jeTag) it.take(jeTag) + " … (gekürzt)" else it }
            })
        },
    )

    private fun datum(text: String): LocalDate? = when (text.trim().lowercase(Locale.GERMAN)) {
        "" -> null
        "heute" -> LocalDate.now()
        "gestern" -> LocalDate.now().minusDays(1)
        "vorgestern" -> LocalDate.now().minusDays(2)
        else -> runCatching { LocalDate.parse(text.trim().take(10)) }.getOrNull()
    }

    private fun lang(tag: LocalDate): String = tag.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM yyyy", Locale.GERMAN))

    companion object {
        /** Nur einfache Dateinamen: kein Pfad, keine Sonderzeichen, Endung .md. */
        private val NAME = Regex("[\\w.\\- äöüÄÖÜß]{1,120}\\.md")
        private val CLIENT = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(110, TimeUnit.SECONDS).build()
    }
}
