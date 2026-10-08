package de.frank.jarvis.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Wer etwas ausgelöst hat. */
enum class Quelle(val anzeige: String) { CHATGPT("ChatGPT"), JARVIS("Jarvis"), SYSTEM("System") }

data class Eintrag(val zeit: Long, val quelle: Quelle, val werkzeug: String, val text: String, val ok: Boolean)

/** Was Jarvis getan hat — sichtbar im Reiter „Aktivität“, die letzten [MAX] Einträge überleben einen Neustart. */
object Protokoll {
    private const val MAX = 200
    private val _eintraege = MutableStateFlow<List<Eintrag>>(emptyList())
    val eintraege: StateFlow<List<Eintrag>> = _eintraege.asStateFlow()
    private var datei: File? = null

    @Synchronized
    fun lade(context: Context) {
        if (datei != null) return
        val f = File(context.filesDir, "protokoll.json").also { datei = it }
        _eintraege.value = runCatching {
            val array = JSONArray(f.readText())
            (0 until array.length()).map { i ->
                array.getJSONObject(i).let {
                    Eintrag(it.optLong("z"), Quelle.entries.firstOrNull { q -> q.name == it.optString("q") } ?: Quelle.SYSTEM, it.optString("w"), it.optString("t"), it.optBoolean("ok", true))
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun melde(quelle: Quelle, werkzeug: String, text: String, ok: Boolean = true) {
        val neu = (listOf(Eintrag(System.currentTimeMillis(), quelle, werkzeug, text.take(600), ok)) + _eintraege.value).take(MAX)
        _eintraege.value = neu
        runCatching {
            datei?.writeText(JSONArray(neu.map { JSONObject().put("z", it.zeit).put("q", it.quelle.name).put("w", it.werkzeug).put("t", it.text).put("ok", it.ok) }).toString())
        }
    }

    @Synchronized
    fun leere() {
        _eintraege.value = emptyList()
        runCatching { datei?.delete() }
    }
}
