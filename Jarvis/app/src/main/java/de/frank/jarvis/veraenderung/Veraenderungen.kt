package de.frank.jarvis.veraenderung

import android.content.Context
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import kotlin.random.Random

/** Eine Veränderung, die Frank in seinem Leben festhalten will: kurze Einzeile ([titel]) und genaue Beschreibung samt Grund. */
data class Veraenderung(
    val id: Long,
    val titel: String,
    val beschreibung: String,
    val erledigt: Boolean = false,
    val erledigtAm: Long = 0L,
    val erstellt: Long = System.currentTimeMillis(),
    val geaendert: Long = System.currentTimeMillis(),
    val geloescht: Boolean = false,
)

/**
 * Franks Veränderungen: je Eintrag eine kleine JSON-Datei unter `filesDir/veraenderungen/` (`v_<id>.json`), abgeglichen mit dem
 * Tablet über [de.frank.jarvis.abgleich.GedaechtnisAbgleich] wie die Listen. Gelöscht wird nur als Grabstein in der Datei.
 */
object Veraenderungen {
    const val ORDNER = "veraenderungen"
    private val sperre = Any()
    @Volatile private var geladen = false
    private val alle = HashMap<Long, Veraenderung>()
    private val _stand = MutableStateFlow<List<Veraenderung>>(emptyList())
    /** Nicht gelöschte, neueste zuerst. */
    val stand: StateFlow<List<Veraenderung>> = _stand.asStateFlow()
    private val letzteId = java.util.concurrent.atomic.AtomicLong(0)

    private fun neueId(): Long {
        val vorschlag = System.currentTimeMillis() * 1000 + Random.nextInt(1000)
        return letzteId.updateAndGet { maxOf(it + 1, vorschlag) }
    }

    private fun ordner(app: Context) = File(app.filesDir, ORDNER).apply { mkdirs() }

    fun sicher(context: Context): List<Veraenderung> {
        if (geladen) return _stand.value
        synchronized(sperre) { if (!geladen) { einlesen(context.applicationContext); geladen = true } }
        return _stand.value
    }

    /** Nach dem Abgleich: neu einlesen und das Live-Gedächtnis nachziehen. */
    fun neuLaden(context: Context) {
        val app = context.applicationContext
        synchronized(sperre) { einlesen(app); geladen = true }
        auffrischen(app, "Geräteabgleich")
    }

    fun vonAussen(block: () -> Unit) = synchronized(sperre) { block() }

    private fun einlesen(app: Context) {
        alle.clear()
        ordner(app).listFiles { f -> f.isFile && f.name.startsWith("v_") && f.name.endsWith(".json") }?.forEach { f ->
            runCatching { aus(JSONObject(f.readText())).let { alle[it.id] = it } }.onFailure { Logbuch.fehler("Veränderungen", it, "${f.name} unlesbar") }
        }
        veroeffentlichen()
    }

    private fun veroeffentlichen() {
        _stand.value = alle.values.filter { !it.geloescht }.sortedByDescending { it.erstellt }
    }

    private fun auffrischen(app: Context, anlass: String) {
        runCatching { de.frank.jarvis.gedaechtnis.Lagebild.anstossen(app, listOf(de.frank.jarvis.gedaechtnis.Bereich.VERAENDERUNGEN), anlass) }
    }

    private fun schreibe(context: Context, v: Veraenderung): Veraenderung {
        val app = context.applicationContext
        sicher(app)
        val n = v.copy(geaendert = System.currentTimeMillis())
        synchronized(sperre) {
            alle[n.id] = n
            val ziel = File(ordner(app), "v_${n.id}.json")
            val teil = File(ziel.path + ".teil")
            teil.writeText(json(n).toString())
            if (!teil.renameTo(ziel)) { ziel.delete(); teil.renameTo(ziel) }
            veroeffentlichen()
        }
        runCatching { de.frank.jarvis.abgleich.GedaechtnisAbgleich.anstossen() }
        auffrischen(app, "Veränderungen geändert")
        return n
    }

    fun finde(context: Context, id: Long): Veraenderung? = sicher(context).firstOrNull { it.id == id }

    fun anlegen(context: Context, titel: String, beschreibung: String): Veraenderung =
        schreibe(context, Veraenderung(neueId(), titel.trim(), beschreibung.trim()))

    fun aendern(context: Context, id: Long, aendern: (Veraenderung) -> Veraenderung): Veraenderung? {
        val alt = finde(context, id) ?: return null
        val neu = aendern(alt)
        return if (neu == alt) alt else schreibe(context, neu)
    }

    fun erledigt(context: Context, id: Long, erledigt: Boolean): Veraenderung? =
        aendern(context, id) { if (it.erledigt == erledigt) it else it.copy(erledigt = erledigt, erledigtAm = if (erledigt) System.currentTimeMillis() else 0L) }

    fun loeschen(context: Context, id: Long): Veraenderung? = aendern(context, id) { it.copy(geloescht = true) }

    /** Treffer über id oder ungefähren Titel. */
    fun suche(context: Context, text: String): List<Veraenderung> {
        val s = sicher(context)
        text.trim().toLongOrNull()?.let { id -> s.firstOrNull { it.id == id }?.let { return listOf(it) } }
        val t = glatt(text)
        if (t.isEmpty()) return emptyList()
        s.filter { glatt(it.titel) == t }.takeIf { it.isNotEmpty() }?.let { return it }
        return s.filter { glatt(it.titel).contains(t) || t.contains(glatt(it.titel)) || glatt(it.beschreibung).contains(t) }
    }

    private fun glatt(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()

    private fun json(v: Veraenderung) = JSONObject()
        .put("id", v.id).put("titel", v.titel).put("beschreibung", v.beschreibung).put("erledigt", v.erledigt).put("erledigtAm", v.erledigtAm)
        .put("erstellt", v.erstellt).put("geaendert", v.geaendert).put("geloescht", v.geloescht)

    private fun aus(j: JSONObject) = Veraenderung(
        j.getLong("id"), j.optString("titel"), j.optString("beschreibung"), j.optBoolean("erledigt"), j.optLong("erledigtAm"),
        j.optLong("erstellt"), j.optLong("geaendert"), j.optBoolean("geloescht"),
    )

    /** Datum kurz für Anzeige und Gedächtnis. */
    fun datum(ms: Long): String = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.of("Europe/Berlin")).toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}
