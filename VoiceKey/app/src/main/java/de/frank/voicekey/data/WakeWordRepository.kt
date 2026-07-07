package de.frank.voicekey.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.frank.voicekey.obs.Obs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "voicekey_settings")

/**
 * DataStore-Persistenz fuer Wake-Woerter, Favoriten und Dienst-Status.
 * Woerter werden als JSON-Array gespeichert (org.json, in Android enthalten).
 */
class WakeWordRepository(private val context: Context) {

    private val keyWords = stringPreferencesKey("wake_words_json")
    private val keyServiceEnabled = booleanPreferencesKey("service_enabled")

    val words: Flow<List<WakeWord>> = context.dataStore.data.map { prefs ->
        val raw = prefs[keyWords]
        if (raw == null) DEFAULT_WORDS else parseWords(raw)
    }

    val serviceEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[keyServiceEnabled] ?: false
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[keyServiceEnabled] = enabled }
        Obs.i("WakeWordRepository", "setServiceEnabled", "Dienst-Schalter gespeichert", mapOf("enabled" to enabled))
    }

    suspend fun addWord(text: String, lang: WakeLang, favorit: Boolean) {
        val cleaned = text.trim()
        if (!Obs.probe(cleaned.isNotEmpty(), "Leeres Wake-Wort wird nicht gespeichert", "WakeWordRepository", "addWord")) return
        mutate { list ->
            // Duplikate (gleicher Text + Sprache) nicht doppelt anlegen.
            if (list.any { it.lang == lang && it.text.equals(cleaned, ignoreCase = true) }) list
            else list + WakeWord(text = cleaned, lang = lang, favorit = favorit)
        }
        Obs.i("WakeWordRepository", "addWord", "Wort hinzugefügt", mapOf("text" to cleaned, "lang" to lang, "favorit" to favorit))
    }

    suspend fun updateWord(id: String, newText: String) {
        val cleaned = newText.trim()
        if (cleaned.isEmpty()) return
        mutate { list -> list.map { if (it.id == id) it.copy(text = cleaned) else it } }
        Obs.i("WakeWordRepository", "updateWord", "Wort geändert", mapOf("id" to id, "text" to cleaned))
    }

    suspend fun removeWord(id: String) {
        mutate { list -> list.filterNot { it.id == id } }
        Obs.i("WakeWordRepository", "removeWord", "Wort gelöscht", mapOf("id" to id))
    }

    suspend fun toggleFavorit(id: String) {
        mutate { list -> list.map { if (it.id == id) it.copy(favorit = !it.favorit) else it } }
        Obs.i("WakeWordRepository", "toggleFavorit", "Favorit umgeschaltet", mapOf("id" to id))
    }

    private suspend fun mutate(transform: (List<WakeWord>) -> List<WakeWord>) {
        context.dataStore.edit { prefs ->
            val current = prefs[keyWords]?.let(::parseWords) ?: DEFAULT_WORDS
            prefs[keyWords] = serializeWords(transform(current))
        }
    }

    private fun parseWords(raw: String): List<WakeWord> = try {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    WakeWord(
                        id = o.getString("id"),
                        text = o.getString("text"),
                        lang = WakeLang.valueOf(o.getString("lang")),
                        favorit = o.getBoolean("favorit"),
                    )
                )
            }
        }
    } catch (e: Exception) {
        // Korrupte Daten duerfen die App nicht toeten — Defaults + Fehler-Log (kein stilles Schlucken).
        Obs.e("WakeWordRepository", "parseWords", "Wörter-JSON unlesbar, verwende Standardwörter", mapOf("raw" to raw.take(200)), e)
        DEFAULT_WORDS
    }

    private fun serializeWords(words: List<WakeWord>): String {
        val arr = JSONArray()
        words.forEach { w ->
            arr.put(
                JSONObject().apply {
                    put("id", w.id)
                    put("text", w.text)
                    put("lang", w.lang.name)
                    put("favorit", w.favorit)
                }
            )
        }
        return arr.toString()
    }

    companion object {
        val DEFAULT_WORDS = listOf(
            WakeWord(text = "Okay Computer", lang = WakeLang.DE, favorit = true),
            WakeWord(text = "Computer", lang = WakeLang.DE, favorit = false),
            WakeWord(text = "Jarvis", lang = WakeLang.EN, favorit = true),
            WakeWord(text = "Hey Chat", lang = WakeLang.EN, favorit = false),
        )
    }
}
