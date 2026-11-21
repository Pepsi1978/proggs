package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.GespeicherteNachricht
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Programmier-Sessions auf Handy und Tablet gleich und live (Franks Wunsch vom 21.11.2026). Die Meldungen der Sitzung schickt der
 * Server ohnehin an beide Geräte; was nur auf einem Gerät entsteht, geht hier über `/geraet/spiegel` an das andere (eigenes
 * Signal „programmier_spiegel“): der neue Auftrag samt Gespräch („sitzung“), Franks und Jarvis' eigene Zeilen („zeile“),
 * Nachträge („nachtrag“), das Löschen einer Session („weg“) und die Bitte um den ganzen Verlauf („verlauf“). Empfangen wird in
 * [ProgrammierFaehigkeit.meldungVomServer]. Was nicht ankam, bleibt in einer Datei und geht beim nächsten Verbinden noch einmal.
 */
object ProgrammierSpiegel {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val netz = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()
    /** Zwei Schlangen: Zeilen, Nachträge und neue Aufträge sofort und in Franks Reihenfolge, der große Verlauf dahinter. */
    private val eilig = Channel<Pair<Context, JSONObject>>(Channel.UNLIMITED)
    private val verlauf = Channel<Pair<Context, JSONObject>>(Channel.UNLIMITED)
    /** Was über den Spiegel hereinkam (Gespräch und Text): Speichert die App es danach, geht es nicht zurück (sonst ein Echo). */
    private val eingang = java.util.concurrent.ConcurrentHashMap<String, Long>()
    /** Der Server nimmt höchstens 1 MB; darüber geht eine Session ohne Verlauf, nur mit dem Auftrag. */
    const val HOECHSTENS_BYTES = 950_000
    private const val DATEI = "programmier_spiegel"
    private const val WEG_LISTE = "geloeschte_gespraeche"
    private const val OFFEN = "spiegel-offen.json"
    private val dateiSperre = Any()

    init {
        bereich.launch { for ((context, rumpf) in eilig) if (!sende(context, rumpf)) merkeOffen(context, rumpf) }
        bereich.launch { for ((context, rumpf) in verlauf) if (!sende(context, rumpf)) merkeOffen(context, rumpf) }
    }

    fun senden(context: Context, rumpf: JSONObject) {
        val aus = if (rumpf.optString("art") == "sitzung" && !rumpf.optBoolean("live")) verlauf else eilig
        aus.trySend(context.applicationContext to rumpf)
    }

    /** Die Größe, wie sie der Server sieht (UTF-8, Umlaute und Emoji mehrbyteig). */
    fun bytes(rumpf: JSONObject): Int = rumpf.toString().toByteArray(Charsets.UTF_8).size

    /** Passt den Rumpf unter die Grenze: erst ohne Gesprächsverlauf, dann ohne Volltext. false = auch so zu groß. */
    fun einpassen(rumpf: JSONObject): Boolean {
        for (feld in listOf("gespraech_datei", "volltext")) {
            if (bytes(rumpf) < HOECHSTENS_BYTES) return true
            rumpf.remove(feld)
        }
        return bytes(rumpf) < HOECHSTENS_BYTES
    }

    private fun sende(context: Context, rumpf: JSONObject): Boolean {
        val e = Einstellungen.get(context)
        if (e.serverHost.isBlank() || e.serverToken.isBlank()) return true
        return runCatching {
            val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/spiegel").header("X-Jarvis-Token", e.serverToken)
                .header(de.frank.jarvis.data.Geraet.KOPFZEILE, de.frank.jarvis.data.Geraet.kennung)
                .post(rumpf.toString().toRequestBody("application/json".toMediaType())).build()
            netz.newCall(anfrage).execute().use { a ->
                // 400/413: Diesen Rumpf nimmt der Server nie; ihn aufzuheben, hielte nur die Schlange auf.
                if (a.code in 400..499) { Logbuch.system("Spiegel abgelehnt", "${rumpf.optString("art")} ${rumpf.optString("auftrag_id")}: HTTP ${a.code}"); return@runCatching true }
                if (!a.isSuccessful) throw IllegalStateException("HTTP ${a.code}")
            }
            true
        }.getOrElse {
            Logbuch.system("Spiegel nicht gesendet", "${rumpf.optString("art")} ${rumpf.optString("auftrag_id")}: ${it.message ?: it.javaClass.simpleName}, später noch einmal")
            false
        }
    }

    private fun offenDatei(context: Context) = File(context.filesDir, OFFEN)

    /** Nicht angekommen: in die Datei (höchstens 300 Einträge und 8 MB, der älteste Verlauf fällt zuerst weg). */
    private fun merkeOffen(context: Context, rumpf: JSONObject) = synchronized(dateiSperre) {
        val liste = runCatching { JSONArray(offenDatei(context).readText()) }.getOrDefault(JSONArray())
        val alle = (0 until liste.length()).mapNotNull { liste.optJSONObject(it) }.toMutableList()
        alle += rumpf
        while (alle.size > 300 || alle.sumOf { bytes(it) } > 8_000_000) {
            val i = alle.indexOfFirst { it.optString("art") == "sitzung" }.takeIf { it >= 0 } ?: 0
            alle.removeAt(i)
        }
        runCatching { offenDatei(context).writeText(JSONArray(alle).toString()) }
    }

    /** Beim Verbinden mit dem Server: was offen blieb, noch einmal senden. */
    fun nachholen(context: Context) {
        val app = context.applicationContext
        val alle = synchronized(dateiSperre) {
            val d = offenDatei(app)
            if (!d.isFile) return
            val l = runCatching { JSONArray(d.readText()) }.getOrDefault(JSONArray())
            d.delete()
            (0 until l.length()).mapNotNull { l.optJSONObject(it) }
        }
        if (alle.isNotEmpty()) Logbuch.system("Spiegel nachgeholt", "${alle.size} Einträge noch einmal gesendet")
        alle.forEach { senden(app, it) }
    }

    private fun schluessel(gespraech: String, text: String) = gespraech + "|" + text.hashCode()

    /** Vom anderen Gerät übernommen: nicht zurückspiegeln, wenn die App die Zeile gleich speichert (eine Minute lang). */
    fun eingegangen(gespraech: String, text: String) {
        val jetzt = System.currentTimeMillis()
        eingang.entries.removeIf { jetzt - it.value > 60_000L }
        eingang[schluessel(gespraech, text)] = jetzt
    }

    /** Die Zeile ging direkt in die Datei (Gespräch nicht offen): Es kommt kein Speichern mehr, das Echo-Merkmal fällt weg. */
    fun eingangErledigt(gespraech: String, text: String) { eingang.remove(schluessel(gespraech, text)) }

    /**
     * [Gespraeche.speichern] hat einem Programmier-Gespräch Nachrichten angefügt. Franks Zeilen und Jarvis' Antworten gehen ans
     * andere Gerät; die Meldungen der Sitzung nicht, die bekommt es selbst vom Server.
     */
    fun neueZeilen(context: Context, gespraech: String, neue: List<GespeicherteNachricht>) {
        val zeilen = neue.filter { it.vonMir || it.art == "antwort" }.filter { eingang.remove(schluessel(gespraech, it.text)) == null }
        if (zeilen.isEmpty()) return
        val auftrag = ProgrammierFaehigkeit.auftragImGespraech(context, gespraech) ?: return
        zeilen.forEach { n ->
            senden(context, JSONObject().put("art", "zeile").put("auftrag_id", auftrag).put("gespraech", gespraech).put("nachricht",
                JSONObject().put("mir", n.vonMir).put("text", n.text).put("art", n.art).put("kontext", n.kontext).put("anhaenge", JSONArray(n.anhaenge))))
        }
    }

    /** Auf einem Gerät gelöschte Sessions: Ein später eintreffender Spiegel oder Verlauf legt sie nie wieder an. */
    fun geloescht(context: Context): Set<String> = runCatching {
        val l = JSONArray(context.getSharedPreferences(DATEI, Context.MODE_PRIVATE).getString(WEG_LISTE, "[]"))
        (0 until l.length()).map { l.optString(it) }.toSet()
    }.getOrDefault(emptySet())

    fun alsGeloeschtMerken(context: Context, gespraech: String) {
        val p = context.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
        val bisher = runCatching { JSONArray(p.getString(WEG_LISTE, "[]")).let { l -> (0 until l.length()).map { l.optString(it) } } }.getOrDefault(emptyList())
        p.edit().putString(WEG_LISTE, JSONArray((bisher.filter { it != gespraech } + gespraech).takeLast(500)).toString()).commit()
    }

    /** Verlauf einmal je Version und Gerät abgleichen: Beide Geräte schicken ihre Sessions, jedes übernimmt, was ihm fehlt. */
    fun verlaufFaellig(context: Context): Boolean {
        val p = context.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
        val marke = "verlauf_" + de.frank.jarvis.BuildConfig.VERSION_CODE
        if (p.getBoolean(marke, false)) return false
        p.edit().putBoolean(marke, true).apply()
        return true
    }

    /** Bittet das andere Gerät um seinen Verlauf; es antwortet höchstens einmal je Stunde. */
    fun verlaufErbitten(context: Context) = senden(context, JSONObject().put("art", "verlauf").put("auftrag_id", ""))

    fun verlaufAntwortErlaubt(context: Context): Boolean {
        val p = context.getSharedPreferences(DATEI, Context.MODE_PRIVATE)
        val jetzt = System.currentTimeMillis()
        if (jetzt - p.getLong("verlauf_geantwortet", 0L) < 3_600_000L) return false
        p.edit().putLong("verlauf_geantwortet", jetzt).apply()
        return true
    }
}
