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
data class Prompt(val nummer: Int, val text: String, val erstellt: Long, val titel: String = "")

/**
 * Der Reiter „Prompts“ der Ablage: eigene Datei `prompts.json`, getrennt von den Ablage-Einträgen (die bleiben unberührt).
 * Geschrieben wird atomar; eine unlesbare Datei wird gesichert und nie überschrieben.
 */
object Prompts {
    private const val DATEI = "prompts.json"
    private val _stand = MutableStateFlow(0L)
    /** Zählt jedes gelungene Schreiben; die Oberfläche lädt darauf neu (auch wenn Jarvis per Sprache speichert). */
    val stand: StateFlow<Long> = _stand.asStateFlow()

    private class Stand(val liste: List<Prompt>, val defekt: Boolean = false)

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
                arr.optJSONObject(i)?.let { Prompt(it.optInt("nummer"), it.optString("text").trimStart(), it.optLong("erstellt"), it.optString("titel").trim()) }?.takeIf { it.nummer > 0 && it.text.isNotEmpty() }
            }.sortedBy { it.nummer }.eindeutig().durchnummeriert()
            Stand(liste)
        } catch (e: Exception) {
            runCatching { f.copyTo(File(f.path + ".defekt-" + f.lastModified())) }
            Logbuch.fehler("Prompts defekt", e)
            Stand(emptyList(), defekt = true)
        }
    }

    private fun schreibe(c: Context, liste: List<Prompt>): Boolean {
        val inhalt = JSONObject().put("format", 1)
            .put("prompts", JSONArray(liste.map { JSONObject().put("nummer", it.nummer).put("text", it.text).put("erstellt", it.erstellt).put("titel", it.titel) })).toString(2)
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
        val neu = Prompt(s.liste.size + 1, sauber, System.currentTimeMillis())
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
        return schreibe(c, s.liste.map { if (it.erstellt == ziel.erstellt) it.copy(text = sauber, titel = if (it.text == sauber) it.titel else "") else it })
    }

    /** Setzt die KI-Überschrift, aber nur, wenn der Prompt noch genau diesen [text] hat (sonst gehört sie zu einer alten Fassung). */
    @Synchronized
    fun setzeTitel(c: Context, erstellt: Long, text: String, titel: String): Boolean {
        val s = lies(c)
        if (s.defekt || s.liste.none { it.erstellt == erstellt && it.text == text }) return false
        return schreibe(c, s.liste.map { if (it.erstellt == erstellt && it.text == text) it.copy(titel = titel.trim()) else it })
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
        return weg.takeIf { !s.defekt && schreibe(c, (s.liste - weg).durchnummeriert()) }
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
        return neu.map { it.erstellt } == s.liste.map { it.erstellt } || schreibe(c, neu.durchnummeriert())
    }
}
