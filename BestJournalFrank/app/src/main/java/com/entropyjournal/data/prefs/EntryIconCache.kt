package com.entropyjournal.data.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/**
 * Persistenter Cache fuer KI-klassifizierte Symbol-Schluessel pro Tagebucheintrag.
 * Speichert ein einfaches Map<entryId, iconKey> als JSON in einer eigenen
 * SharedPreferences-Datei.
 *
 * Voll separat von den verschluesselten Prefs — die Cache-Daten sind nicht
 * sensibel (nur Icon-Schluessel wie "glass_water"), und wir wollen die
 * encryptedPrefs nicht mit hochfrequenten Schreibzugriffen belasten.
 */
@Singleton
class EntryIconCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Liefert den gecachten iconKey fuer einen Eintrag, oder null wenn nicht gecacht. */
    fun get(entryId: Long): String? {
        val raw = prefs.getString(KEY_MAP, null) ?: return null
        return try {
            val json = JSONObject(raw)
            json.optString(entryId.toString()).takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    /** Speichert den iconKey fuer einen Eintrag. */
    fun put(entryId: Long, iconKey: String) {
        val raw = prefs.getString(KEY_MAP, null)
        val json = try {
            if (raw != null) JSONObject(raw) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }
        json.put(entryId.toString(), iconKey)
        prefs.edit { putString(KEY_MAP, json.toString()) }
    }

    /** Entfernt den Cache-Eintrag (z.B. wenn der Tagebucheintrag geaendert wurde). */
    fun invalidate(entryId: Long) {
        val raw = prefs.getString(KEY_MAP, null) ?: return
        try {
            val json = JSONObject(raw)
            json.remove(entryId.toString())
            prefs.edit { putString(KEY_MAP, json.toString()) }
        } catch (_: Exception) {
            // Bei korruptem JSON: kompletten Cache neu starten
            prefs.edit { remove(KEY_MAP) }
        }
    }

    companion object {
        private const val PREFS_NAME = "entry_icon_cache"
        private const val KEY_MAP = "icon_map_json"
    }
}
