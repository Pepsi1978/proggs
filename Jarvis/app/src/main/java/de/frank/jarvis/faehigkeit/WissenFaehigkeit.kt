package de.frank.jarvis.faehigkeit

import android.content.Context
import android.util.Log
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * Franks Wissens-Datenbank: der Ordner `Datenbank` im Repository Pepsi1978/proggs. Dort legt ChatGPT die
 * wichtigen Inhalte langer Gespräche ab (Ziele, JARVIS, weitere Themen), je Thema ein Unterordner mit
 * Inhaltsverzeichnis. Jarvis holt den Ordner bei jeder Synchronisierung aufs Handy und liest dann lokal.
 *
 * Das Repository ist öffentlich lesbar, deshalb braucht der Abruf keinen Schlüssel.
 */
class WissenFaehigkeit(private val context: Context) : Faehigkeit {
    override val id = "wissen"
    override val name = "Wissens-Datenbank"
    override val beschreibung = "Franks gespeichertes Wissen aus langen Gesprächen (Ordner Datenbank im Repository): Ziele, JARVIS und weitere Themen."
    override val hinweise =
        "Die Wissens-Datenbank enthält die ausführlich aufbereiteten Inhalte wichtiger Gespräche, nach Themenordnern sortiert (zum Beispiel Ziele, JARVIS). " +
            "Sagt Frank „in der Datenbank steht etwas zu …“ oder fragt er nach früher Besprochenem, sieh zuerst mit wissen_inhalt nach, was es gibt, und lies dann die passende Datei mit wissen_lesen. " +
            "Die Datenbank ist nur lesbar; neue Einträge entstehen in ChatGPT über den Datenbank-Skill."

    private val ordner get() = File(context.filesDir, "wissen").apply { mkdirs() }

    override fun stoerung(): String? = null

    private fun dateien(): List<File> = ordner.walkTopDown().filter { it.isFile && it.extension.lowercase(Locale.ROOT) in setOf("md", "txt") }.sortedBy { it.path }.toList()
    private fun pfad(datei: File): String = datei.relativeTo(ordner).path.replace('\\', '/')

    /** Kurzer Überblick für die Tagesdatenbank: das Haupt-Inhaltsverzeichnis und die Liste der Dateien. */
    fun ueberblick(): String {
        val alle = dateien()
        if (alle.isEmpty()) return "NICHT VERFÜGBAR: noch nicht synchronisiert."
        val haupt = File(ordner, "INHALTSVERZEICHNIS.md").takeIf { it.exists() }?.readText()?.take(3000).orEmpty()
        return (haupt + "\n\nDateien:\n" + alle.joinToString("\n") { "- " + pfad(it) }).trim()
    }

    /**
     * Holt den Ordner frisch aus dem Repository. Erst wenn alles geladen ist, wird der alte Stand ersetzt —
     * bricht der Abruf ab, bleibt der bisherige Bestand lesbar. Rückgabe: Anzahl der Dateien oder null bei Fehler.
     */
    suspend fun synchronisiere(): Int? = withContext(Dispatchers.IO) {
        runCatching {
            val neu = File(context.cacheDir, "wissen_neu").apply { deleteRecursively(); mkdirs() }
            var anzahl = 0
            fun lade(repoPfad: String, ziel: File, tiefe: Int) {
                if (tiefe > 5) return
                val liste = JSONArray(hole("https://api.github.com/repos/$REPO/contents/$repoPfad?ref=main"))
                for (i in 0 until liste.length()) {
                    val eintrag = liste.getJSONObject(i)
                    val name = eintrag.getString("name")
                    if (name.contains("..") || name.contains('/') || name.contains('\\')) continue
                    when (eintrag.optString("type")) {
                        "dir" -> lade("$repoPfad/" + java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20"), File(ziel, name).apply { mkdirs() }, tiefe + 1)
                        "file" -> if (name.substringAfterLast('.').lowercase(Locale.ROOT) in setOf("md", "txt") && eintrag.optLong("size") < 2_000_000) {
                            File(ziel, name).writeText(hole(eintrag.getString("download_url")))
                            anzahl++
                        }
                    }
                }
            }
            lade(WURZEL, neu, 0)
            check(anzahl > 0) { "Der Ordner ist leer." }
            ordner.deleteRecursively()
            neu.copyRecursively(ordner, overwrite = true)
            neu.deleteRecursively()
            anzahl
        }.onFailure { Log.w("JarvisWissen", "Synchronisierung fehlgeschlagen", it) }.getOrNull()
    }

    private fun hole(adresse: String): String =
        CLIENT.newCall(Request.Builder().url(adresse).header("User-Agent", "Jarvis").header("Accept", "application/vnd.github+json").build()).execute().use { antwort ->
            check(antwort.isSuccessful) { "GitHub antwortet mit ${antwort.code}" }
            antwort.body?.string().orEmpty()
        }

    /** Beim ersten Zugriff ohne Bestand einmal holen. */
    private suspend fun stelleSicher() { if (dateien().isEmpty()) synchronisiere() }

    override val werkzeuge: List<Werkzeug> = listOf(
        Werkzeug(
            name = "wissen_inhalt",
            titel = "Wissens-Datenbank ansehen",
            beschreibung = "Jarvis: zeigt, was in Franks Wissens-Datenbank liegt: das Inhaltsverzeichnis mit den Themenordnern und alle Dateien. " +
                "Erster Schritt, wenn Frank nach früher Besprochenem fragt („Was steht in der Datenbank zu kognitiver Leistung?“, „Was hatten wir zu meinen Zielen festgehalten?“).",
            schema = schema("ordner" to text("Optional: nur dieser Themenordner, zum Beispiel Ziele oder JARVIS. Dann kommt dessen Inhaltsverzeichnis.")),
            nurLesen = true,
        ) { a ->
            stelleSicher()
            val thema = a.optString("ordner").trim()
            val alle = dateien()
            if (alle.isEmpty()) return@Werkzeug Ergebnis("Die Wissens-Datenbank ließ sich nicht laden (kein Internet?).", fehler = true)
            if (thema.isEmpty()) return@Werkzeug Ergebnis(ueberblick())
            val treffer = alle.filter { pfad(it).lowercase(Locale.GERMAN).startsWith(thema.lowercase(Locale.GERMAN)) || thema.lowercase(Locale.GERMAN) in pfad(it).substringBeforeLast('/').lowercase(Locale.GERMAN) }
            if (treffer.isEmpty()) return@Werkzeug Ergebnis("Keinen Ordner „$thema“ gefunden. Vorhanden:\n" + ueberblick(), fehler = true)
            val verzeichnis = treffer.firstOrNull { it.name.equals("INHALTSVERZEICHNIS.md", true) }?.readText()?.take(6000).orEmpty()
            Ergebnis((verzeichnis + "\n\nDateien:\n" + treffer.joinToString("\n") { "- " + pfad(it) + " (" + (it.length() / 1000 + 1) + " kB)" }).trim())
        },
        Werkzeug(
            name = "wissen_lesen",
            titel = "Wissens-Datei lesen",
            beschreibung = "Jarvis: liest eine Datei aus Franks Wissens-Datenbank vollständig. Der Pfad stammt aus wissen_inhalt; ein eindeutiger Teil des Namens genügt.",
            schema = schema("datei" to text("Pfad oder eindeutiger Teil des Dateinamens, zum Beispiel „Kognitive_Leistungssteigerung“."), pflicht = listOf("datei")),
            nurLesen = true,
        ) { a ->
            stelleSicher()
            val gesucht = a.optString("datei").trim().lowercase(Locale.GERMAN)
            val alle = dateien()
            val treffer = alle.filter { pfad(it).lowercase(Locale.GERMAN) == gesucht }.ifEmpty { alle.filter { gesucht in pfad(it).lowercase(Locale.GERMAN) } }
            // Die Inhaltsverzeichnisse sind nur dann gemeint, wenn sonst nichts passt.
            val kandidaten = treffer.filterNot { it.name.equals("INHALTSVERZEICHNIS.md", true) }.ifEmpty { treffer }
            when {
                gesucht.isEmpty() || kandidaten.isEmpty() -> Ergebnis("Keine Datei passt zu „${a.optString("datei")}“. Mit wissen_inhalt nachsehen.", fehler = true)
                kandidaten.size > 1 -> Ergebnis("Mehrere Dateien passen:\n" + kandidaten.joinToString("\n") { "- " + pfad(it) } + "\nBitte genauer angeben.", fehler = true)
                else -> kandidaten.first().let { Ergebnis("Datei ${pfad(it)}:\n\n" + it.readText().take(60_000)) }
            }
        },
        Werkzeug(
            name = "wissen_suchen",
            titel = "In der Wissens-Datenbank suchen",
            beschreibung = "Jarvis: durchsucht alle Dateien in Franks Wissens-Datenbank nach einem Begriff und zeigt die Fundstellen mit etwas Umgebung. " +
                "Nutze es, wenn nicht klar ist, in welchem Ordner etwas steht.",
            schema = schema("suche" to text("Begriff oder Wortgruppe."), pflicht = listOf("suche")),
            nurLesen = true,
        ) { a ->
            stelleSicher()
            val suche = a.optString("suche").trim().lowercase(Locale.GERMAN)
            if (suche.length < 2) return@Werkzeug Ergebnis("Der Suchbegriff ist zu kurz.", fehler = true)
            val funde = dateien().mapNotNull { datei ->
                val text = datei.readText()
                val klein = text.lowercase(Locale.GERMAN)
                val stellen = generateSequence(klein.indexOf(suche)) { klein.indexOf(suche, it + suche.length).takeIf { n -> n >= 0 } }.takeWhile { it >= 0 }.take(3).toList()
                if (stellen.isEmpty()) null else "- " + pfad(datei) + ":\n" + stellen.joinToString("\n") { s -> "    … " + text.substring(maxOf(0, s - 160), minOf(text.length, s + 240)).replace(Regex("\\s+"), " ") + " …" }
            }
            if (funde.isEmpty()) Ergebnis("Nichts zu „${a.optString("suche")}“ gefunden.") else Ergebnis("Fundstellen in ${funde.size} Datei(en):\n" + funde.take(12).joinToString("\n"))
        },
    )

    companion object {
        private const val REPO = "Pepsi1978/proggs"
        private const val WURZEL = "Datenbank"
        private val CLIENT = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    }
}
