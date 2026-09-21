package de.frank.wecker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import de.frank.genialeideen.data.settings.SecureSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class OpenIdea(val id: Long, val title: String, val text: String)
class IdeasBridge(private val context: Context) {
    private val store = AlarmStore.get(context)
    suspend fun refresh(): List<OpenIdea> = withContext(Dispatchers.IO) {
        val rows: List<OpenIdea> = context.contentResolver.query(URI, null, null, null, null)?.use { cursor ->
            buildList<OpenIdea> {
                while (cursor.moveToNext()) add(OpenIdea(cursor.getLong(0), cursor.getString(1), cursor.getString(2)))
            }
        } ?: error("Geniale Ideen ist nicht installiert oder braucht das Brücken-Update.")
        check(store.prefs.edit().putString("ideas", JSONArray(rows.map {
            JSONObject().put("id", it.id).put("title", it.title).put("text", it.text)
        }).toString()).putLong("ideasAt", System.currentTimeMillis()).commit())
        rows
    }
    fun cached(): List<OpenIdea> {
        val array = JSONArray(store.prefs.getString("ideas", "[]"))
        return (0 until array.length()).map { array.getJSONObject(it).let { j -> OpenIdea(j.getLong("id"), j.getString("title"), j.getString("text")) } }
    }
    /** Abgewählte Ideen werden beim Wecken nicht vorgelesen. Gemerkt wird die Abwahl, damit neue Ideen automatisch aktiv sind. */
    fun deaktiviert(): Set<Long> = store.prefs.getStringSet("ideasDisabled", emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
    fun setzeAktiv(id: Long, aktiv: Boolean) {
        val neu = if (aktiv) deaktiviert() - id else deaktiviert() + id
        store.prefs.edit().putStringSet("ideasDisabled", neu.map { it.toString() }.toSet()).commit()
    }
    suspend fun copySettings(settings: SecureSettings) = withContext(Dispatchers.IO) {
        val data = context.contentResolver.call(URI, "speechSettings", null, null)
            ?: error("Geniale Ideen hat keine Spracheinstellungen geliefert.")
        settings.ttsProvider = data.getString("provider").orEmpty()
        settings.edgeTtsVoice = data.getString("edgeVoice").orEmpty()
        settings.googleTtsVoice = data.getString("googleVoice").orEmpty()
        settings.googleTtsApiKey = data.getString("googleKey").orEmpty()
        settings.qwenTtsVoiceId = data.getString("qwenVoice").orEmpty()
        settings.qwenStandardVoice = data.getString("qwenStandardVoice").orEmpty()
        settings.qwenTtsApiKey = data.getString("qwenKey").orEmpty()
        settings.groqApiKey = data.getString("groqKey").orEmpty()
        settings.ttsSpeechRate = data.getFloat("rate", 1f)
    }
    companion object { val URI: Uri = Uri.parse("content://de.frank.genialeideen.wecker/offen") }
}

class IdeasChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "de.frank.genialeideen.IDEAS_CHANGED") PreparationWorker.enqueue(context)
    }
}
