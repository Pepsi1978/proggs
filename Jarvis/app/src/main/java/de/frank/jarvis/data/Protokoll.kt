package de.frank.jarvis.data

import android.content.Context
import de.frank.jarvis.observability.Logbuch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

/** Wer etwas ausgelöst hat. */
enum class Quelle(val anzeige: String) { CHATGPT("ChatGPT"), JARVIS("Jarvis"), SYSTEM("System") }

data class Eintrag(val zeit: Long, val quelle: Quelle, val werkzeug: String, val text: String, val ok: Boolean)

/**
 * Was Jarvis getan hat — sichtbar im Reiter „Aktivität“, die letzten [MAX] Einträge überleben einen Neustart.
 * Jeder Eintrag geht zusätzlich ins Logbuch. Geschrieben wird auf einem eigenen Faden, gebündelt (höchstens alle
 * 300 ms der letzte Stand) und atomar: Der Aufrufer wartet nie auf die Platte, ein Abbruch hinterlässt keine halbe Datei.
 */
object Protokoll {
    private const val MAX = 200
    private const val BUENDELN_MS = 300L
    private val _eintraege = MutableStateFlow<List<Eintrag>>(emptyList())
    val eintraege: StateFlow<List<Eintrag>> = _eintraege.asStateFlow()
    @Volatile private var datei: File? = null

    private val schreiber = Executors.newSingleThreadScheduledExecutor { r -> Thread(r, "jarvis-protokoll").apply { isDaemon = true } }
    private val ausstehend = AtomicBoolean(false)

    /** Liest die gespeicherten Einträge einmal ein. Synchron: Aufrufer lesen danach sofort [eintraege]. */
    @Synchronized
    fun lade(context: Context) {
        if (datei != null) return
        val f = File(context.filesDir, "protokoll.json")
        val geladen = if (!f.isFile) emptyList() else runCatching {
            val array = JSONArray(f.readText())
            (0 until array.length()).map { i ->
                array.getJSONObject(i).let {
                    Eintrag(it.optLong("z"), Quelle.entries.firstOrNull { q -> q.name == it.optString("q") } ?: Quelle.SYSTEM, it.optString("w"), it.optString("t"), it.optBoolean("ok", true))
                }
            }
        }.getOrElse { fehler ->
            // Beschädigt: zur Untersuchung beiseitelegen, statt sie beim nächsten Eintrag still zu überschreiben.
            Logbuch.fehler("Protokoll: Datei beschädigt", fehler)
            runCatching { f.renameTo(File(f.parentFile, "protokoll.defekt-${System.currentTimeMillis()}.json")) }
            emptyList()
        }
        // Was vor dem Laden schon gemeldet wurde, bleibt erhalten.
        _eintraege.update { schon -> (schon + geladen).sortedByDescending { it.zeit }.take(MAX) }
        datei = f
        if (_eintraege.value.size != geladen.size) planeSchreiben()
    }

    fun melde(quelle: Quelle, werkzeug: String, text: String, ok: Boolean = true) {
        val neu = Eintrag(System.currentTimeMillis(), quelle, werkzeug, text.take(600), ok)
        _eintraege.update { (listOf(neu) + it).take(MAX) }
        // Spiegel ins Logbuch, gekürzt: Das Protokoll enthält nur Kurzbeschreibungen, keine Inhalte.
        val ereignis = "${quelle.anzeige}: $werkzeug"
        if (ok) Logbuch.aktion(ereignis, text.take(300)) else Logbuch.fehler(ereignis, null, text.take(300))
        planeSchreiben()
    }

    fun leere() {
        _eintraege.value = emptyList()
        val f = datei ?: return
        runCatching { schreiber.execute(Runnable { runCatching { f.delete() } }) }
    }

    /** Schreibt gebündelt: Mehrere Einträge kurz hintereinander ergeben einen Schreibvorgang mit dem letzten Stand. */
    private fun planeSchreiben() {
        // Vor dem Laden nicht schreiben, sonst überschriebe der leere Anfangsstand die gespeicherten Einträge.
        if (datei == null) return
        if (!ausstehend.compareAndSet(false, true)) return
        runCatching {
            schreiber.schedule(Runnable {
                ausstehend.set(false)
                schreibe()
            }, BUENDELN_MS, TimeUnit.MILLISECONDS)
        }.onFailure { ausstehend.set(false) }
    }

    private fun schreibe() {
        val f = datei ?: return
        val stand = _eintraege.value
        if (stand.isEmpty()) { runCatching { f.delete() }; return }
        runCatching {
            val json = JSONArray(stand.map { JSONObject().put("z", it.zeit).put("q", it.quelle.name).put("w", it.werkzeug).put("t", it.text).put("ok", it.ok) }).toString()
            val tmp = File(f.parentFile, f.name + ".tmp")
            FileOutputStream(tmp).use { it.write(json.toByteArray()); it.fd.sync() }
            if (!tmp.renameTo(f)) { f.delete(); tmp.renameTo(f) }
        }.onFailure { Logbuch.fehler("Protokoll: Schreiben fehlgeschlagen", it) }
    }
}
