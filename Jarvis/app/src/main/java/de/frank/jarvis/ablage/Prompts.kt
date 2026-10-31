package de.frank.jarvis.ablage

import de.frank.jarvis.faehigkeit.DiktatTrenner
import android.content.Context
import android.util.AtomicFile
import de.frank.jarvis.observability.Logbuch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/** Ein gespeicherter Prompt: vorformulierter Auftrag an Jarvis. [nummer] ist der Platz in der Liste (oben 1, lückenlos); [erstellt] kennzeichnet ihn dauerhaft. */
data class Prompt(val nummer: Int, val text: String, val erstellt: Long, val titel: String = "", val quelle: String = "", val geaendert: Long = 0L) {
    /** Leer = aus der Zeit vor dem Abgleich, also vom Handy. */
    val quelleName: String get() = quelle.ifBlank { AblageQuellen.HANDY }
}

/**
 * Der Reiter „Prompts“ der Ablage: eigene Datei `prompts.json`, getrennt von den Ablage-Einträgen (die bleiben unberührt).
 * Geschrieben wird atomar; eine unlesbare Datei wird gesichert und nie überschrieben.
 */
object Prompts {
    private const val DATEI = "prompts.json"
    private val _stand = MutableStateFlow(0L)
    /** Zählt jedes gelungene Schreiben; die Oberfläche lädt darauf neu (auch wenn Jarvis per Sprache speichert). */
    val stand: StateFlow<Long> = _stand.asStateFlow()

    /** [ordnungZeit]: wann zuletzt umsortiert wurde. [geloescht]: hier gelöschte Prompts (Kennung → Zeit), die der Server noch nicht kennt. */
    private class Stand(val liste: List<Prompt>, val defekt: Boolean = false, val ordnungZeit: Long = 0L, val geloescht: Map<Long, Long> = emptyMap())

    /** [Prompt.erstellt] dient als feste Kennung: fehlende (0) oder doppelte Werte bekommen beim Lesen den nächsten freien. */
    private fun List<Prompt>.eindeutig(): List<Prompt> {
        val belegt = HashSet<Long>()
        return map { p ->
            var e = p.erstellt.coerceAtLeast(1)
            while (!belegt.add(e)) e++
            if (e == p.erstellt) p else p.copy(erstellt = e)
        }
    }

    /** Nummeriert lückenlos von oben: Prompt 1, 2, 3 … in der gegebenen Reihenfolge. */
    private fun List<Prompt>.durchnummeriert(): List<Prompt> = mapIndexed { i, p -> p.copy(nummer = i + 1) }

    private fun datei(c: Context) = File(c.applicationContext.filesDir, DATEI)

    @Synchronized
    private fun lies(c: Context): Stand {
        val f = datei(c)
        val text = try { String(AtomicFile(f).readFully(), Charsets.UTF_8) } catch (_: FileNotFoundException) { return Stand(emptyList()) }
            catch (e: Exception) { Logbuch.fehler("Prompts lesen", e); return Stand(emptyList(), defekt = true) }
        if (text.isBlank()) return Stand(emptyList())
        return try {
            val json = JSONObject(text)
            val arr = json.getJSONArray("prompts")
            val liste = (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { Prompt(it.optInt("nummer"), it.optString("text").trimStart(), it.optLong("erstellt"), it.optString("titel").trim(), it.optString("quelle"), it.optLong("geaendert").takeIf { g -> g > 0 } ?: it.optLong("erstellt")) }?.takeIf { it.nummer > 0 && it.text.isNotEmpty() }
            }.sortedBy { it.nummer }.eindeutig().durchnummeriert()
            val weg = json.optJSONObject("geloescht")
            Stand(liste, false, json.optLong("ordnung_zeit"), weg?.keys()?.asSequence()?.mapNotNull { k -> k.toLongOrNull()?.let { it to weg.optLong(k) } }?.toMap().orEmpty())
        } catch (e: Exception) {
            runCatching { f.copyTo(File(f.path + ".defekt-" + f.lastModified())) }
            Logbuch.fehler("Prompts defekt", e)
            Stand(emptyList(), defekt = true)
        }
    }

    @Synchronized
    private fun schreibe(c: Context, liste: List<Prompt>, ordnungZeit: Long? = null, geloescht: Map<Long, Long>? = null): Boolean {
        val alt = lies(c)
        val inhalt = JSONObject().put("format", 1).put("ordnung_zeit", ordnungZeit ?: alt.ordnungZeit)
            .put("geloescht", JSONObject().apply { (geloescht ?: alt.geloescht).forEach { (k, v) -> put(k.toString(), v) } })
            .put("prompts", JSONArray(liste.map { JSONObject().put("nummer", it.nummer).put("text", it.text).put("erstellt", it.erstellt).put("titel", it.titel).put("quelle", it.quelle).put("geaendert", it.geaendert) })).toString(2)
        val atomar = AtomicFile(datei(c))
        val aus = try { atomar.startWrite() } catch (e: Exception) { Logbuch.fehler("Prompts schreiben", e); return false }
        return try {
            aus.write(inhalt.toByteArray(Charsets.UTF_8)); atomar.finishWrite(aus); _stand.update { it + 1 }; true
        } catch (e: Exception) {
            atomar.failWrite(aus); Logbuch.fehler("Prompts schreiben", e); false
        }
    }

    /** true = die Datei ist unlesbar (als `.defekt-…` gesichert); dann wird nichts geschrieben. */
    fun defekt(c: Context): Boolean = lies(c).defekt

    /** Alle Prompts, aufsteigend nach Nummer. */
    fun alle(c: Context): List<Prompt> = lies(c).liste

    /** Hängt einen neuen Prompt unten an (nächste Nummer). null = nicht gespeichert (leer oder Datei defekt). */
    @Synchronized
    fun speichere(c: Context, text: String): Prompt? {
        val sauber = DiktatTrenner.gespeichert(text)
        if (sauber.isEmpty()) return null
        val s = lies(c)
        if (s.defekt) return null
        val jetzt = System.currentTimeMillis()
        val neu = Prompt(s.liste.size + 1, sauber, jetzt, quelle = AblageQuellen.HANDY, geaendert = jetzt)
        return neu.takeIf { schreibe(c, s.liste + neu) }
    }

    /**
     * Ersetzt den Text eines vorhandenen Prompts. Mit [erstellt] wird genau dieser Prompt gesucht, egal auf welchem Platz er
     * inzwischen steht (nach Löschen oder Verschieben trägt er womöglich eine andere [nummer]).
     */
    @Synchronized
    fun aendere(c: Context, nummer: Int, text: String, erstellt: Long? = null): Boolean {
        val s = lies(c)
        val ziel = s.liste.firstOrNull { if (erstellt != null) it.erstellt == erstellt else it.nummer == nummer }
        val sauber = DiktatTrenner.gespeichert(text)
        if (s.defekt || sauber.isEmpty() || ziel == null) return false
        return schreibe(c, s.liste.map { if (it.erstellt == ziel.erstellt) it.copy(text = sauber, titel = if (it.text == sauber) it.titel else "", geaendert = System.currentTimeMillis()) else it })
    }

    /** Setzt die KI-Überschrift, aber nur, wenn der Prompt noch genau diesen [text] hat (sonst gehört sie zu einer alten Fassung). */
    @Synchronized
    fun setzeTitel(c: Context, erstellt: Long, text: String, titel: String): Boolean {
        val s = lies(c)
        if (s.defekt || s.liste.none { it.erstellt == erstellt && it.text == text }) return false
        return schreibe(c, s.liste.map { if (it.erstellt == erstellt && it.text == text) it.copy(titel = titel.trim(), geaendert = System.currentTimeMillis()) else it })
    }

    @Synchronized
    /**
     * Löscht den Prompt auf Platz [nummer], oder mit [erstellt] genau diesen Prompt, egal auf welchem Platz er steht.
     * Mit [nurBeiText] nur, wenn er noch diesen Text hat: Eine inzwischen geänderte Fassung bleibt stehen.
     */
    fun loesche(c: Context, nummer: Int, erstellt: Long? = null, nurBeiText: String? = null): Prompt? {
        val s = lies(c)
        val weg = s.liste.firstOrNull { if (erstellt != null) it.erstellt == erstellt else it.nummer == nummer } ?: return null
        if (nurBeiText != null && weg.text.trim() != nurBeiText.trim()) return null
        return weg.takeIf { !s.defekt && schreibe(c, (s.liste - weg).durchnummeriert(), geloescht = s.geloescht + (weg.erstellt to System.currentTimeMillis())) }
    }

    /** Neue Reihenfolge nach dem Ziehen (Anlagezeitpunkte von oben nach unten); danach wieder Prompt 1, 2, 3 … von oben. */
    @Synchronized
    fun ordne(c: Context, reihenfolge: List<Long>): Boolean {
        val s = lies(c)
        if (s.defekt) return false
        val nach = s.liste.associateBy { it.erstellt }
        // Inzwischen neu dazugekommene bleiben unten, inzwischen gelöschte fallen weg.
        val gezogen = reihenfolge.distinct()
        val drin = gezogen.toSet()
        val neu = gezogen.mapNotNull(nach::get) + s.liste.filter { it.erstellt !in drin }
        return neu.map { it.erstellt } == s.liste.map { it.erstellt } || schreibe(c, neu.durchnummeriert(), ordnungZeit = System.currentTimeMillis())
    }

    // ---- Abgleich mit dem Server (AblageSync) ----

    fun ordnungZeit(c: Context): Long = lies(c).ordnungZeit

    fun offeneLoeschungen(c: Context): Map<Long, Long> = lies(c).geloescht

    @Synchronized
    fun quittiereLoeschung(c: Context, erstellt: Long) { val s = lies(c); if (!s.defekt) schreibe(c, s.liste, geloescht = s.geloescht - erstellt) }

    /** Stand eines Prompts vom Server: ändert ihn an seinem Platz, ein neuer kommt unten dazu. */
    @Synchronized
    fun setzeFern(c: Context, p: Prompt): Boolean {
        val s = lies(c)
        if (s.defekt) return false
        val da = s.liste.any { it.erstellt == p.erstellt }
        return schreibe(c, (if (da) s.liste.map { if (it.erstellt == p.erstellt) p.copy(nummer = it.nummer) else it } else mitNeuem(s.liste, p)).durchnummeriert())
    }

    /** Neue stehen unten, untereinander das Älteste zuerst: So kommen alle Jarvis-Versionen auf dieselben Nummern, egal in welcher Folge sie eintreffen. */
    private fun mitNeuem(liste: List<Prompt>, p: Prompt): List<Prompt> {
        var platz = liste.size
        while (platz > 0 && liste[platz - 1].erstellt > p.erstellt) platz--
        return liste.subList(0, platz) + p + liste.subList(platz, liste.size)
    }

    @Synchronized
    fun loescheFern(c: Context, erstellt: Long): Boolean {
        val s = lies(c)
        if (s.defekt || s.liste.none { it.erstellt == erstellt }) return false
        return schreibe(c, s.liste.filter { it.erstellt != erstellt }.durchnummeriert())
    }

    @Synchronized
    fun ordneFern(c: Context, reihenfolge: List<Long>, zeit: Long): Boolean {
        val s = lies(c)
        if (s.defekt) return false
        val nach = s.liste.associateBy { it.erstellt }
        val drin = reihenfolge.toSet()
        return schreibe(c, (reihenfolge.distinct().mapNotNull(nach::get) + s.liste.filter { it.erstellt !in drin }).durchnummeriert(), ordnungZeit = zeit)
    }
}
