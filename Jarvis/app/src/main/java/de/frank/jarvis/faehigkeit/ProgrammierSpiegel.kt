package de.frank.jarvis.faehigkeit

import android.content.Context
import de.frank.jarvis.data.Einstellungen
import de.frank.jarvis.data.GespeicherteNachricht
import de.frank.jarvis.observability.Logbuch
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
 * Server ohnehin an beide Geräte; was nur auf einem Gerät entsteht, geht hier über `/geraet/spiegel` an das andere:
 * der neue Auftrag samt Gespräch („sitzung“), Franks und Jarvis' eigene Zeilen („zeile“), Nachträge („nachtrag“) und das
 * Löschen einer Session („weg“). Empfangen wird in [ProgrammierFaehigkeit.meldungVomServer] (stand „spiegel“).
 */
object ProgrammierSpiegel {
    private val bereich = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val netz = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()
    /** Eine Warteschlange: Zeilen kommen in der Reihenfolge an, in der Frank sie geschrieben hat. */
    private val ausgang = Channel<Pair<Context, JSONObject>>(Channel.UNLIMITED)
    /** Was über den Spiegel hereinkam (Gespräch und Text): Speichert die App es danach, geht es nicht zurück (sonst ein Echo). */
    private val eingang = java.util.concurrent.ConcurrentHashMap<String, Long>()
    /** Eine Session ist höchstens so groß unterwegs (der Server nimmt bis 1 MB); größere gehen ohne Verlauf, nur mit dem Auftrag. */
    const val GESPRAECH_HOECHSTENS = 700_000
    private const val WEG_DATEI = "programmier_spiegel"
    private const val WEG_LISTE = "geloeschte_gespraeche"

    init {
        bereich.launch { for ((context, rumpf) in ausgang) sende(context, rumpf) }
    }

    fun senden(context: Context, rumpf: JSONObject) {
        ausgang.trySend(context.applicationContext to rumpf)
    }

    private fun sende(context: Context, rumpf: JSONObject) {
        val e = Einstellungen.get(context)
        if (e.serverHost.isBlank() || e.serverToken.isBlank()) return
        runCatching {
            val anfrage = Request.Builder().url("https://${e.serverHost}/geraet/spiegel").header("X-Jarvis-Token", e.serverToken)
                .header(de.frank.jarvis.data.Geraet.KOPFZEILE, de.frank.jarvis.data.Geraet.kennung)
                .post(rumpf.toString().toRequestBody("application/json".toMediaType())).build()
            netz.newCall(anfrage).execute().use { if (!it.isSuccessful) throw IllegalStateException("HTTP ${it.code}") }
        }.onFailure { Logbuch.system("Spiegel nicht gesendet", "${rumpf.optString("art")} ${rumpf.optString("auftrag_id")}: ${it.message ?: it.javaClass.simpleName}") }
    }

    private fun schluessel(gespraech: String, text: String) = gespraech + "|" + text.hashCode()

    /** Vom anderen Gerät übernommen: nicht zurückspiegeln, wenn die App die Zeile gleich speichert (eine Minute lang). */
    fun eingegangen(gespraech: String, text: String) {
        val jetzt = System.currentTimeMillis()
        eingang.entries.removeIf { jetzt - it.value > 60_000L }
        eingang[schluessel(gespraech, text)] = jetzt
    }

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
        val l = JSONArray(context.getSharedPreferences(WEG_DATEI, Context.MODE_PRIVATE).getString(WEG_LISTE, "[]"))
        (0 until l.length()).map { l.optString(it) }.toSet()
    }.getOrDefault(emptySet())

    fun alsGeloeschtMerken(context: Context, gespraech: String) {
        val p = context.getSharedPreferences(WEG_DATEI, Context.MODE_PRIVATE)
        val bisher = runCatching { JSONArray(p.getString(WEG_LISTE, "[]")).let { l -> (0 until l.length()).map { l.optString(it) } } }.getOrDefault(emptyList())
        p.edit().putString(WEG_LISTE, JSONArray((bisher.filter { it != gespraech } + gespraech).takeLast(500)).toString()).apply()
    }

    /** Verlauf einmal je Version und Gerät abgleichen: Beide Geräte schicken ihre Sessions, jedes übernimmt, was ihm fehlt. */
    fun verlaufFaellig(context: Context): Boolean {
        val p = context.getSharedPreferences(WEG_DATEI, Context.MODE_PRIVATE)
        val marke = "verlauf_" + de.frank.jarvis.BuildConfig.VERSION_CODE
        if (p.getBoolean(marke, false)) return false
        p.edit().putBoolean(marke, true).apply()
        return true
    }
}
