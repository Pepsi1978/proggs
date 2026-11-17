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
    /** Platz in „Aktuell“ (klein = oben). Ohne eigene Reihenfolge: neueste zuerst über −[erstellt]. */
    val rang: Double = -erstellt.toDouble(),
    /** true = Reiter „Aktuell“ (wird gerade umgesetzt), false = Reiter „Offen“ (steht noch an). Alte Einträge ohne Feld gelten als angenommen. */
    val angenommen: Boolean = true,
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
    /** Nicht gelöschte, nach [Veraenderung.rang] (von Frank per Ziehen geordnet, sonst neueste zuerst). */
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
        _stand.value = alle.values.filter { !it.geloescht }.sortedWith(compareBy<Veraenderung> { it.rang }.thenByDescending { it.erstellt })
    }

    private fun auffrischen(app: Context, anlass: String) {
        runCatching { de.frank.jarvis.gedaechtnis.Lagebild.anstossen(app, listOf(de.frank.jarvis.gedaechtnis.Bereich.VERAENDERUNGEN), anlass) }
    }

    /**
     * Lesen, Ändern und Schreiben in einem Zug unter der Sperre: Der Stand kommt aus der Datei (der Abgleich kann sie eben ersetzt
     * haben, bevor [neuLaden] lief), sonst aus dem Speicher. Erst die Datei, dann der Speicher; ein Schreibfehler ergibt null statt Absturz.
     * Unverändert (ohne Zeitstempel) wird nichts geschrieben, sonst schlüge eine neue Zeit jüngere Änderungen des anderen Geräts.
     */
    private fun schreibe(context: Context, id: Long, neuAnlegen: Veraenderung? = null, aendern: (Veraenderung) -> Veraenderung): Veraenderung? {
        val app = context.applicationContext
        sicher(app)
        val ergebnis: Veraenderung
        synchronized(sperre) {
            val ziel = File(ordner(app), "v_$id.json")
            val alt = neuAnlegen ?: (runCatching { aus(JSONObject(ziel.readText())) }.getOrNull() ?: alle[id])?.takeIf { !it.geloescht } ?: return null
            val neu = aendern(alt)
            if (neuAnlegen == null && neu.copy(geaendert = 0) == alt.copy(geaendert = 0)) { alle[id] = alt; veroeffentlichen(); return alt }
            val n = neu.copy(geaendert = System.currentTimeMillis())
            if (!ablegen(ziel, n)) return null
            alle[n.id] = n
            veroeffentlichen()
            ergebnis = n
        }
        runCatching { de.frank.jarvis.abgleich.GedaechtnisAbgleich.anstossen() }
        auffrischen(app, "Veränderungen geändert")
        return ergebnis
    }

    private fun ablegen(ziel: File, n: Veraenderung): Boolean = try {
        val teil = File(ziel.path + ".teil")
        teil.writeText(json(n).toString())
        if (!teil.renameTo(ziel)) { ziel.delete(); check(teil.renameTo(ziel)) { "${ziel.name} ließ sich nicht ersetzen" } }
        true
    } catch (e: Exception) {
        Logbuch.fehler("Veränderungen", e, "Speichern")
        false
    }

    /** Neue Reihenfolge von „Aktuell“ nach Ziehen: [ids] von oben nach unten; nur geänderte Plätze werden geschrieben, ein Abgleich am Ende. */
    fun reihenfolge(context: Context, ids: List<Long>) {
        val app = context.applicationContext
        sicher(app)
        var geschrieben = false
        synchronized(sperre) {
            ids.forEachIndexed { i, id ->
                val ziel = File(ordner(app), "v_$id.json")
                val alt = (runCatching { aus(JSONObject(ziel.readText())) }.getOrNull() ?: alle[id])?.takeIf { !it.geloescht } ?: return@forEachIndexed
                val rang = i.toDouble()
                if (alt.rang == rang) { alle[id] = alt; return@forEachIndexed }
                val n = alt.copy(rang = rang, geaendert = System.currentTimeMillis())
                if (ablegen(ziel, n)) { alle[id] = n; geschrieben = true }
            }
            veroeffentlichen()
        }
        if (!geschrieben) return
        runCatching { de.frank.jarvis.abgleich.GedaechtnisAbgleich.anstossen() }
        auffrischen(app, "Reihenfolge der Veränderungen geändert")
    }

    fun finde(context: Context, id: Long): Veraenderung? = sicher(context).firstOrNull { it.id == id }

    /** Neue Veränderungen landen zuerst unter „Offen“; erst [annehmen] macht sie zur laufenden Veränderung. */
    fun anlegen(context: Context, titel: String, beschreibung: String): Veraenderung? {
        val v = Veraenderung(neueId(), titel.trim(), beschreibung.trim(), angenommen = false)
        return schreibe(context, v.id, neuAnlegen = v) { it }
    }

    fun aendern(context: Context, id: Long, aendern: (Veraenderung) -> Veraenderung): Veraenderung? = schreibe(context, id, aendern = aendern)

    fun erledigt(context: Context, id: Long, erledigt: Boolean): Veraenderung? =
        aendern(context, id) { if (it.erledigt == erledigt) it else it.copy(erledigt = erledigt, erledigtAm = if (erledigt) System.currentTimeMillis() else 0L) }

    /** „Annehmen“: von „Offen“ nach „Aktuell“, dort ganz oben. */
    fun annehmen(context: Context, id: Long): Veraenderung? {
        val oben = (sicher(context).filter { it.angenommen && !it.erledigt }.minOfOrNull { it.rang } ?: 0.0) - 1.0
        return aendern(context, id) { if (it.angenommen) it else it.copy(angenommen = true, rang = oben) }
    }

    /** Zurück von „Aktuell“ nach „Offen“, dort ganz oben. */
    fun nachOffen(context: Context, id: Long): Veraenderung? {
        val oben = (sicher(context).filter { !it.angenommen && !it.erledigt }.minOfOrNull { it.rang } ?: 0.0) - 1.0
        return aendern(context, id) { if (!it.angenommen) it else it.copy(angenommen = false, rang = oben) }
    }

    /** Live-Gedächtnis von außen nachziehen, etwa beim Wechsel zwischen den Reitern „Aktuell“ und „Offen“. */
    fun gedaechtnisNachziehen(context: Context, anlass: String) = auffrischen(context.applicationContext, anlass)

    fun loeschen(context: Context, id: Long): Veraenderung? = aendern(context, id) { it.copy(geloescht = true) }

    /** Treffer über id oder ungefähren Titel. */
    fun suche(context: Context, text: String): List<Veraenderung> {
        val s = sicher(context)
        text.trim().toLongOrNull()?.let { id -> s.firstOrNull { it.id == id }?.let { return listOf(it) } }
        val t = glatt(text)
        if (t.isEmpty()) return emptyList()
        s.filter { glatt(it.titel) == t }.takeIf { it.isNotEmpty() }?.let { return it }
        return s.filter { v -> val g = glatt(v.titel); (g.isNotEmpty() && (g.contains(t) || t.contains(g))) || glatt(v.beschreibung).contains(t) }
    }

    private fun glatt(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()

    private fun json(v: Veraenderung) = JSONObject()
        .put("id", v.id).put("titel", v.titel).put("beschreibung", v.beschreibung).put("erledigt", v.erledigt).put("erledigtAm", v.erledigtAm)
        .put("erstellt", v.erstellt).put("geaendert", v.geaendert).put("geloescht", v.geloescht).put("rang", v.rang).put("angenommen", v.angenommen)

    private fun aus(j: JSONObject): Veraenderung {
        val erstellt = j.optLong("erstellt")
        return Veraenderung(
            j.getLong("id"), j.optString("titel"), j.optString("beschreibung"), j.optBoolean("erledigt"), j.optLong("erledigtAm"),
            erstellt, j.optLong("geaendert"), j.optBoolean("geloescht"), rang = if (j.has("rang")) j.optDouble("rang") else -erstellt.toDouble(),
            angenommen = j.optBoolean("angenommen", true),
        )
    }

    /** Datum kurz für Anzeige und Gedächtnis. */
    fun datum(ms: Long): String = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.of("Europe/Berlin")).toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
}
