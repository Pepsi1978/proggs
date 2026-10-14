package de.frank.jarvis.observability

import android.content.Context
import android.util.Log
import de.frank.jarvis.BuildConfig
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import org.json.JSONObject

/**
 * Das Logbuch von Jarvis: ein Mitschreiber für die ganze App, damit sich Jarvis später gezielt verbessern lässt
 * („schau dir die Logs an und optimiere“). Je Tag eine Datei `filesDir/logbuch/JJJJ-MM-TT.jsonl`, eine JSON-Zeile
 * je Ereignis: `{"z": Zeit, "k": Art, "e": Ereignis, "d": Einzelheiten, "ms": Dauer, "v": Version}`.
 *
 * - Arten: aktion, fehler, dauer, gedaechtnis, netz, sprache, system.
 * - Geschrieben wird auf einem eigenen Faden, nie auf dem Hauptfaden; ein Fehler beim Schreiben bleibt folgenlos.
 * - 14 Tage, höchstens 6 MB je Tag. Abstürze landen mit Stapel noch vor dem Beenden darin.
 * - Nie hinein: Schlüssel, Passwörter, Token, Mail-Inhalte, vollständige Prompts. Statt Inhalt die Länge.
 *
 * Abholen am PC: `Jarvis/werkzeuge/logbuch-holen.ps1` (über adb). In der App: Einstellungen → Logbuch, oder das
 * Werkzeug logbuch_lesen.
 */
object Logbuch {
    private const val TAG = "JarvisLogbuch"
    private const val TAGE = 14L
    private const val HOECHSTENS_BYTES = 6_000_000L

    enum class Art(val kennung: String) { AKTION("aktion"), FEHLER("fehler"), DAUER("dauer"), GEDAECHTNIS("gedaechtnis"), NETZ("netz"), SPRACHE("sprache"), SYSTEM("system") }

    private val schreiber = Executors.newSingleThreadExecutor { r -> Thread(r, "jarvis-logbuch").apply { isDaemon = true } }
    @Volatile private var ordner: File? = null
    @Volatile private var abstuerzeAngemeldet = false

    fun starte(context: Context) {
        if (ordner != null) return
        synchronized(this) {
            if (ordner != null) return
            val o = File(context.applicationContext.filesDir, "logbuch").apply { mkdirs() }
            ordner = o
            if (!abstuerzeAngemeldet) {
                abstuerzeAngemeldet = true
                val vorher = Thread.getDefaultUncaughtExceptionHandler()
                Thread.setDefaultUncaughtExceptionHandler { faden, fehler ->
                    // Synchron schreiben: Nach dem Absturz gibt es keinen Schreibfaden mehr.
                    runCatching { zeile(o, Art.FEHLER, "Absturz in ${faden.name}", Log.getStackTraceString(fehler).take(6_000), null) }
                    vorher?.uncaughtException(faden, fehler)
                }
            }
            schreiber.execute {
                runCatching {
                    val grenze = LocalDate.now().minusDays(TAGE)
                    o.listFiles { f -> f.name.endsWith(".jsonl") }?.filter { runCatching { LocalDate.parse(it.name.take(10)).isBefore(grenze) }.getOrDefault(false) }?.forEach { it.delete() }
                }
            }
        }
    }

    fun schreibe(art: Art, ereignis: String, details: String = "", ms: Long? = null) {
        val o = ordner ?: return
        val z = System.currentTimeMillis()
        runCatching { schreiber.execute { runCatching { zeile(o, art, ereignis, details, ms, z) } } }
    }

    private fun zeile(o: File, art: Art, ereignis: String, details: String, ms: Long?, z: Long = System.currentTimeMillis()) {
        val tag = Instant.ofEpochMilli(z).atZone(ZoneId.systemDefault()).toLocalDate()
        val f = File(o, "$tag.jsonl")
        if (f.length() > HOECHSTENS_BYTES) return
        val json = JSONObject().put("z", ZEIT.format(Instant.ofEpochMilli(z).atZone(ZoneId.systemDefault()))).put("k", art.kennung).put("e", ereignis.take(200))
        if (details.isNotEmpty()) json.put("d", details.take(2_000))
        if (ms != null) json.put("ms", ms)
        json.put("v", BuildConfig.VERSION_NAME)
        f.appendText(json.toString() + "\n")
    }

    fun aktion(ereignis: String, details: String = "") = schreibe(Art.AKTION, ereignis, details)
    fun dauer(ereignis: String, ms: Long, details: String = "") = schreibe(Art.DAUER, ereignis, details, ms)
    fun gedaechtnis(ereignis: String, details: String = "", ms: Long? = null) = schreibe(Art.GEDAECHTNIS, ereignis, details, ms)
    fun netz(ereignis: String, details: String = "") = schreibe(Art.NETZ, ereignis, details)
    fun sprache(ereignis: String, details: String = "", ms: Long? = null) = schreibe(Art.SPRACHE, ereignis, details, ms)
    fun system(ereignis: String, details: String = "") = schreibe(Art.SYSTEM, ereignis, details)
    fun fehler(ereignis: String, fehler: Throwable? = null, details: String = "") =
        schreibe(Art.FEHLER, ereignis, listOfNotNull(details.takeIf { it.isNotEmpty() }, fehler?.let { "${it.javaClass.simpleName}: ${it.message.orEmpty()}" + (it.stackTrace.firstOrNull()?.let { s -> " @ $s" } ?: "") }).joinToString(" | "))

    /** Misst die Dauer eines Blocks und schreibt sie als Art „dauer“. */
    inline fun <T> messe(ereignis: String, details: String = "", block: () -> T): T {
        val beginn = System.currentTimeMillis()
        try { return block() } finally { dauer(ereignis, System.currentTimeMillis() - beginn, details) }
    }

    /** Alle Tagesdateien, neueste zuerst. */
    fun dateien(): List<File> = ordner?.listFiles { f -> f.name.endsWith(".jsonl") }.orEmpty().sortedByDescending { it.name }

    /**
     * Ein lesbarer Auszug für das Werkzeug logbuch_lesen und die Oberfläche: Zusammenfassung (Anzahl je Art, häufigste
     * Fehler, langsamste Abläufe im Schnitt) und die neuesten passenden Einträge.
     */
    fun auszug(tage: Int = 2, art: String? = null, suche: String? = null, hoechstens: Int = 80): String {
        val grenze = LocalDate.now().minusDays((tage - 1).coerceAtLeast(0).toLong())
        val zeilen = dateien().filter { runCatching { !LocalDate.parse(it.name.take(10)).isBefore(grenze) }.getOrDefault(false) }
            .flatMap { f -> runCatching { f.readLines() }.getOrDefault(emptyList()).reversed() }
            .mapNotNull { runCatching { JSONObject(it) }.getOrNull() }
        if (zeilen.isEmpty()) return "Das Logbuch der letzten $tage Tage ist leer."
        val gefiltert = zeilen.filter { j -> (art.isNullOrBlank() || j.optString("k") == art) && (suche.isNullOrBlank() || j.toString().contains(suche, ignoreCase = true)) }
        return buildString {
            append("LOGBUCH der letzten ").append(tage).append(" Tage: ").append(zeilen.size).append(" Einträge (")
            append(zeilen.groupingBy { it.optString("k") }.eachCount().entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key} ${it.value}" }).append(").\n")
            val fehler = zeilen.filter { it.optString("k") == "fehler" }.groupingBy { it.optString("e") }.eachCount().entries.sortedByDescending { it.value }.take(8)
            if (fehler.isNotEmpty()) append("Häufigste Fehler: ").append(fehler.joinToString("; ") { "${it.key} (${it.value}×)" }).append(".\n")
            val dauern = zeilen.filter { it.has("ms") }.groupBy { it.optString("e") }.mapValues { (_, l) -> l.map { it.optLong("ms") } }
                .entries.sortedByDescending { it.value.average() }.take(10)
            if (dauern.isNotEmpty()) append("Langsamste Abläufe (Schnitt / höchster Wert / Anzahl): ")
                .append(dauern.joinToString("; ") { (e, l) -> "$e ${l.average().toLong()} ms / ${l.max()} ms / ${l.size}×" }).append(".\n")
            append("\nNeueste Einträge").append(if (!art.isNullOrBlank()) " der Art $art" else "").append(if (!suche.isNullOrBlank()) " mit „$suche“" else "").append(":\n")
            gefiltert.take(hoechstens).forEach { j ->
                append(j.optString("z")).append(" [").append(j.optString("k")).append("] ").append(j.optString("e"))
                if (j.has("ms")) append(" (").append(j.optLong("ms")).append(" ms)")
                j.optString("d").takeIf { it.isNotEmpty() }?.let { append(": ").append(it.replace('\n', ' ').take(300)) }
                append('\n')
            }
        }.trim()
    }

    /** Alle Tage zu einer Datei zusammengefasst, zum Teilen oder Herunterladen. */
    fun export(context: Context): File {
        val ziel = File(context.cacheDir, "jarvis-logbuch-${LocalDate.now()}.jsonl")
        ziel.writeText("")
        dateien().reversed().forEach { f -> runCatching { ziel.appendText(f.readText()) } }
        return ziel
    }

    private val ZEIT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

    /** Für Stellen, die nur eine Zeile ins Android-Log schreiben wollen und zugleich ins Logbuch. */
    fun warnung(tag: String, ereignis: String, fehler: Throwable? = null) {
        Log.w(tag, ereignis, fehler)
        fehler(ereignis, fehler)
    }

    init { Log.d(TAG, "Logbuch bereit") }
}
