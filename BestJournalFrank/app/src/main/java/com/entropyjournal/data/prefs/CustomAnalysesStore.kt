package com.entropyjournal.data.prefs

import android.content.SharedPreferences
import android.util.Log
import com.entropyjournal.util.Constants
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * One named custom analysis profile.
 *
 * A list of these replaces the single-string [Constants.PREF_CUSTOM_PROMPT]. Each
 * entry represents one user-editable dashboard scenario with its own name and prompt,
 * addressable by stable [id] so UI renames/reorders don't break references.
 */
data class CustomAnalysis(
    val id: String,
    val name: String,
    val prompt: String,
)

/**
 * Single source of truth for the list of custom dashboard analyses.
 *
 * All reads/writes go through this helper so the JSON serialisation and lazy
 * migration from the legacy [Constants.PREF_CUSTOM_PROMPT] key stay in one place.
 *
 * Scenario-index mapping (also used by [DashboardViewModel] etc.):
 * - 0..3 = fixed built-in scenarios
 * - 4    = first custom analysis (= list index 0)
 * - 5    = second custom analysis (= list index 1)
 * - ...
 *
 * The list always contains at least one entry (the original "Individuelle Analyse").
 * That first entry cannot be deleted in the UI.
 */
object CustomAnalysesStore {

    private const val TAG = "CustomAnalysesStore"

    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_PROMPT = "prompt"

    // ── Loading ─────────────────────────────────────────────────────────────

    /**
     * Loads the current list. Performs one-time migration from the legacy
     * single-string [Constants.PREF_CUSTOM_PROMPT] key if the JSON key is empty.
     * Always returns a list with at least one entry.
     */
    fun load(prefs: SharedPreferences): List<CustomAnalysis> {
        val json = prefs.getString(Constants.PREF_CUSTOM_ANALYSES_JSON, null)
        if (json.isNullOrBlank()) {
            val migrated = migrateFromLegacy(prefs)
            save(prefs, migrated)
            return migrated
        }
        return try {
            parse(json).ifEmpty { listOf(defaultFirst(prefs)) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse custom analyses JSON — falling back to default", e)
            val fallback = listOf(defaultFirst(prefs))
            save(prefs, fallback)
            fallback
        }
    }

    /**
     * Parses a JSON array string into a list. Skips entries without an id;
     * ids are generated here only if the caller forgot to set one.
     */
    fun parse(json: String): List<CustomAnalysis> {
        val arr = JSONArray(json)
        val out = ArrayList<CustomAnalysis>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString(KEY_ID).ifEmpty { UUID.randomUUID().toString() }
            val name = obj.optString(KEY_NAME, Constants.DEFAULT_CUSTOM_ANALYSIS_NAME)
            val prompt = obj.optString(KEY_PROMPT, "")
            out.add(CustomAnalysis(id = id, name = name, prompt = prompt))
        }
        return out
    }

    /** Serialises the list to JSON. Used by Drive-sync code for backups too. */
    fun serialise(list: List<CustomAnalysis>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put(KEY_ID, item.id)
            obj.put(KEY_NAME, item.name)
            obj.put(KEY_PROMPT, item.prompt)
            arr.put(obj)
        }
        return arr.toString()
    }

    // ── Writing ─────────────────────────────────────────────────────────────

    /** Persists the list to encrypted prefs using commit() for atomicity. */
    fun save(prefs: SharedPreferences, list: List<CustomAnalysis>) {
        val ensured = if (list.isEmpty()) listOf(defaultFirst(prefs)) else list
        prefs.edit()
            .putString(Constants.PREF_CUSTOM_ANALYSES_JSON, serialise(ensured))
            .commit()
    }

    /** Adds a new empty entry, persists, and returns the new entry. */
    fun add(prefs: SharedPreferences): CustomAnalysis {
        val list = load(prefs).toMutableList()
        val newEntry = CustomAnalysis(
            id = UUID.randomUUID().toString(),
            name = Constants.DEFAULT_CUSTOM_ANALYSIS_NAME,
            prompt = "",
        )
        list.add(newEntry)
        save(prefs, list)
        return newEntry
    }

    /**
     * Removes the entry with the given id. Refuses to remove the first entry —
     * that one is required to keep the scenario list non-empty.
     * Returns true if removed, false otherwise.
     */
    fun remove(prefs: SharedPreferences, id: String): Boolean {
        val list = load(prefs)
        if (list.size <= 1) return false
        if (list.first().id == id) return false
        val filtered = list.filterNot { it.id == id }
        if (filtered.size == list.size) return false
        save(prefs, filtered)
        return true
    }

    /** Renames an entry. Empty names fall back to the default. */
    fun rename(prefs: SharedPreferences, id: String, newName: String) {
        val sanitised = newName.trim().ifEmpty { Constants.DEFAULT_CUSTOM_ANALYSIS_NAME }
        val list = load(prefs)
        val updated = list.map { if (it.id == id) it.copy(name = sanitised) else it }
        if (updated != list) save(prefs, updated)
    }

    /** Updates the prompt text for the entry with the given id. */
    fun setPrompt(prefs: SharedPreferences, id: String, newPrompt: String) {
        val list = load(prefs)
        val updated = list.map { if (it.id == id) it.copy(prompt = newPrompt) else it }
        if (updated != list) save(prefs, updated)
    }

    // ── Scenario resolution ─────────────────────────────────────────────────

    /**
     * Resolves a scenario index to the backing custom analysis.
     * For scenarios < 4 returns null. For scenarios that point past the end of
     * the list (e.g. after a delete) returns the first custom entry instead so
     * callers never see a stale empty prompt.
     */
    fun resolve(prefs: SharedPreferences, scenario: Int): CustomAnalysis? {
        if (scenario < Constants.FIRST_CUSTOM_SCENARIO_INDEX) return null
        val list = load(prefs)
        if (list.isEmpty()) return null
        val localIndex = scenario - Constants.FIRST_CUSTOM_SCENARIO_INDEX
        return list.getOrNull(localIndex) ?: list.first()
    }

    /** Prompt string for the currently-selected custom scenario, or "". */
    fun activePromptOrEmpty(prefs: SharedPreferences, scenario: Int): String =
        resolve(prefs, scenario)?.prompt ?: ""

    /** Name for the currently-selected custom scenario, or the default label. */
    fun activeNameOrDefault(prefs: SharedPreferences, scenario: Int): String =
        resolve(prefs, scenario)?.name ?: Constants.DEFAULT_CUSTOM_ANALYSIS_NAME

    // ── Migration from legacy single-prompt key ─────────────────────────────

    private fun migrateFromLegacy(prefs: SharedPreferences): List<CustomAnalysis> {
        val legacy = prefs.getString(Constants.PREF_CUSTOM_PROMPT, "")?.trim().orEmpty()
        return listOf(
            CustomAnalysis(
                id = UUID.randomUUID().toString(),
                name = Constants.DEFAULT_CUSTOM_ANALYSIS_NAME,
                prompt = legacy,
            )
        )
    }

    private fun defaultFirst(prefs: SharedPreferences): CustomAnalysis =
        migrateFromLegacy(prefs).first()
}
