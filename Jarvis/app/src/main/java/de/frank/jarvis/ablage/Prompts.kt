package de.frank.jarvis.ablage

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

/** Ein gespeicherter Prompt: vorformulierter Auftrag an Jarvis. Ein neuer Prompt bekommt immer die kleinste freie [nummer]; gelöschte Nummern werden wieder vergeben. */
data class Prompt(val nummer: Int, val text: String, val erstellt: Long)

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

    /** Kleinste Nummer ab 1, die kein Prompt belegt. */
    private fun Stand.freieNummer(): Int = generateSequence(1) { it + 1 }.first { n -> liste.none { it.nummer == n } }

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
                arr.optJSONObject(i)?.let { Prompt(it.optInt("nummer"), it.optString("text").trim(), it.optLong("erstellt")) }?.takeIf { it.nummer > 0 && it.text.isNotEmpty() }
            }.sortedBy { it.nummer }
            Stand(liste)
        } catch (e: Exception) {
            runCatching { f.copyTo(File(f.path + ".defekt-" + f.lastModified())) }
            Logbuch.fehler("Prompts defekt", e)
            Stand(emptyList(), defekt = true)
        }
    }

    private fun schreibe(c: Context, liste: List<Prompt>): Boolean {
        val inhalt = JSONObject().put("format", 1)
            .put("prompts", JSONArray(liste.map { JSONObject().put("nummer", it.nummer).put("text", it.text).put("erstellt", it.erstellt) })).toString(2)
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

    /** Legt einen neuen Prompt mit der kleinsten freien Nummer an. null = nicht gespeichert (leer oder Datei defekt). */
    @Synchronized
    fun speichere(c: Context, text: String): Prompt? {
        val sauber = text.trim()
        if (sauber.isEmpty()) return null
        val s = lies(c)
        if (s.defekt) return null
        val neu = Prompt(s.freieNummer(), sauber, System.currentTimeMillis())
        return neu.takeIf { schreibe(c, (s.liste + neu).sortedBy { it.nummer }) }
    }

    /** Ersetzt den Text eines vorhandenen Prompts. */
    @Synchronized
    fun aendere(c: Context, nummer: Int, text: String): Boolean {
        val s = lies(c)
        if (s.defekt || text.isBlank() || s.liste.none { it.nummer == nummer }) return false
        return schreibe(c, s.liste.map { if (it.nummer == nummer) it.copy(text = text.trim()) else it })
    }

    @Synchronized
    fun loesche(c: Context, nummer: Int): Prompt? {
        val s = lies(c)
        val weg = s.liste.firstOrNull { it.nummer == nummer } ?: return null
        return weg.takeIf { !s.defekt && schreibe(c, s.liste - weg) }
    }
}
