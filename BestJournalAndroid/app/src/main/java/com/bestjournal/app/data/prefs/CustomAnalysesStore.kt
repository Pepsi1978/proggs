package com.bestjournal.app.data.prefs

import android.content.SharedPreferences
import android.util.Log
import com.bestjournal.app.util.Constants
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
    /**
     * True once the user has manually renamed this entry. When false, UI code
     * should ignore [name] and render the current `R.string.profile_custom`
     * instead — so the default name follows the device language even after
     * the entry was created in a different locale.
     */
    val isRenamed: Boolean = false,
)

/**
 * Single source of truth for the list of custom dashboard analyses.
 *
 * All reads/writes go through this helper so the JSON serialisation and lazy
 * migration from the legacy [Constants.PREF_CUSTOM_PROMPT] key stay in one place.
 *
 * Scenario-index mapping (also used by DashboardViewModel etc.):
 * - 0..3 = fixed built-in scenarios
 * - 4    = first custom analysis (= list index 0)
 * - 5    = second custom analysis (= list index 1)
 * - ...
 *
 * The list always contains at least one entry (the original "Individuelle Analyse").
 * That first entry cannot be deleted in the UI.
 *
 * The default name for the first entry is supplied by the caller (usually from a
 * `stringResource(R.string.profile_custom)` so it stays localised). Callers on the
 * ViewModel/UseCase layer pass it in explicitly; the store never hard-codes the
 * German text itself.
 */
object CustomAnalysesStore {

    private const val TAG = "CustomAnalysesStore"

    private const val KEY_ID = "id"
    private const val KEY_NAME = "name"
    private const val KEY_PROMPT = "prompt"
    private const val KEY_IS_RENAMED = "isRenamed"

    /**
     * Every known translation of `R.string.profile_custom`. Used for lazy
     * migration: if a stored entry's name matches one of these, we can safely
     * treat it as the untranslated default and flip `isRenamed` to false — so
     * the display name follows the current device language again.
     *
     * Must stay in sync with `values[-*]/strings.xml` `profile_custom`. When a
     * translation changes there, add the old value here so legacy entries keep
     * migrating correctly.
     */
    private val KNOWN_DEFAULT_NAMES: Set<String> = setOf(
        "Individuelle Analyse",          // de
        "Custom Analysis",                // en
        "Analyse personnalisée",          // fr
        "Análisis personalizado",         // es
        "Análise personalizada",          // pt-BR / pt-PT
        "Analisi personalizzata",         // it
        "Aangepaste analyse",             // nl
        "Analiza własna",                 // pl
        "Власний аналіз",                 // uk
        "Kişisel analiz",                 // tr
        "カスタム分析",                    // ja
        "맞춤 분석",                      // ko
        "自定义分析",                      // zh-Hans
        "自定義分析",                      // zh-Hant
        "تحليل مخصّص",                    // ar
        "व्यक्तिगत विश्लेषण",              // hi
        "การวิเคราะห์เฉพาะ",              // th
        "Analisis kustom",                 // id
        "ব্যক্তিগত বিশ্লেষণ",            // bn
        "వ్యక్తిగత విశ్లేషణ",             // te
        "वैयक्तिक विश्लेषण",              // mr
        "தனிப்பட்ட பகுப்பாய்வு",          // ta
        "انفرادی تجزیہ",                   // ur
        "વ્યક્તિગત વિશ્લેષણ",             // gu
        "ವೈಯಕ್ತಿಕ ವಿಶ್ಲೇಷಣೆ",             // kn
        "വ്യക്തിഗത വിശകലനം",              // ml
    )

    /**
     * Hard fallback name used when the caller cannot supply a localised default
     * (e.g. pure data-layer code). UI code should pass [defaultName] to [load]
     * instead so the default matches the device language.
     */
    private const val FALLBACK_DEFAULT_NAME = "Individuelle Analyse"

    // ── Loading ─────────────────────────────────────────────────────────────

    /**
     * Loads the current list. Performs one-time migration from the legacy
     * single-string [Constants.PREF_CUSTOM_PROMPT] key if the JSON key is empty.
     * Always returns a list with at least one entry.
     *
     * @param defaultName Localised default name for freshly-created entries. Pass
     * the value of `R.string.profile_custom` from UI/ViewModel code. Pure data-layer
     * callers can omit it — the fallback is a constant German string.
     */
    fun load(
        prefs: SharedPreferences,
        defaultName: String = FALLBACK_DEFAULT_NAME,
    ): List<CustomAnalysis> {
        val json = prefs.getString(Constants.PREF_CUSTOM_ANALYSES_JSON, null)
        if (json.isNullOrBlank()) {
            val migrated = migrateFromLegacy(prefs, defaultName)
            save(prefs, migrated)
            return migrated
        }
        return try {
            parse(json).ifEmpty { listOf(defaultFirst(prefs, defaultName)) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse custom analyses JSON — falling back to default", e)
            val fallback = listOf(defaultFirst(prefs, defaultName))
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
            val name = obj.optString(KEY_NAME, FALLBACK_DEFAULT_NAME)
            val prompt = obj.optString(KEY_PROMPT, "")
            // Lazy migration: legacy JSON has no isRenamed flag. Derive it from
            // the stored name — if it matches a known default translation we
            // treat the entry as not-renamed so display falls back to the
            // current R.string.profile_custom. Any non-default name is assumed
            // to be a user rename and stays stable.
            val isRenamed =
                if (obj.has(KEY_IS_RENAMED)) obj.optBoolean(KEY_IS_RENAMED, false)
                else name !in KNOWN_DEFAULT_NAMES
            out.add(
                CustomAnalysis(id = id, name = name, prompt = prompt, isRenamed = isRenamed)
            )
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
            obj.put(KEY_IS_RENAMED, item.isRenamed)
            arr.put(obj)
        }
        return arr.toString()
    }

    // ── Writing ─────────────────────────────────────────────────────────────

    /** Persists the list to encrypted prefs using commit() for atomicity. */
    fun save(prefs: SharedPreferences, list: List<CustomAnalysis>) {
        val ensured =
            if (list.isEmpty()) listOf(defaultFirst(prefs, FALLBACK_DEFAULT_NAME)) else list
        prefs.edit()
            .putString(Constants.PREF_CUSTOM_ANALYSES_JSON, serialise(ensured))
            .commit()
    }

    /** Adds a new empty entry, persists, and returns the new entry. */
    fun add(
        prefs: SharedPreferences,
        defaultName: String = FALLBACK_DEFAULT_NAME,
    ): CustomAnalysis {
        val list = load(prefs, defaultName).toMutableList()
        val newEntry = CustomAnalysis(
            id = UUID.randomUUID().toString(),
            name = defaultName,
            prompt = "",
            isRenamed = false,
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

    /** Renames an entry. Empty names fall back to the supplied default. */
    fun rename(
        prefs: SharedPreferences,
        id: String,
        newName: String,
        defaultName: String = FALLBACK_DEFAULT_NAME,
    ) {
        val sanitised = newName.trim().ifEmpty { defaultName }
        // A rename to the exact current default (or empty, which resolves to it)
        // is treated as "reset to default" — isRenamed stays false so the entry
        // keeps following the device language.
        val isRealRename = sanitised != defaultName
        val list = load(prefs, defaultName)
        val updated =
            list.map {
                if (it.id == id) it.copy(name = sanitised, isRenamed = isRealRename) else it
            }
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

    /**
     * Name for the currently-selected custom scenario, or the supplied fallback.
     * If the entry has not been renamed by the user, returns [defaultName] so
     * the display name follows the current device language.
     */
    fun activeNameOrDefault(
        prefs: SharedPreferences,
        scenario: Int,
        defaultName: String = FALLBACK_DEFAULT_NAME,
    ): String {
        val entry = resolve(prefs, scenario) ?: return defaultName
        return if (entry.isRenamed) entry.name else defaultName
    }

    /**
     * Display name for an entry: its stored name if the user renamed it,
     * otherwise the supplied [defaultName] (usually the current
     * `R.string.profile_custom`). Lets UI code stay language-reactive without
     * rewriting the stored JSON every time the device language changes.
     */
    fun displayName(entry: CustomAnalysis, defaultName: String): String =
        if (entry.isRenamed) entry.name else defaultName

    // ── Migration from legacy single-prompt key ─────────────────────────────

    private fun migrateFromLegacy(
        prefs: SharedPreferences,
        defaultName: String,
    ): List<CustomAnalysis> {
        val legacy = prefs.getString(Constants.PREF_CUSTOM_PROMPT, "")?.trim().orEmpty()
        return listOf(
            CustomAnalysis(
                id = UUID.randomUUID().toString(),
                name = defaultName,
                prompt = legacy,
                isRenamed = false,
            )
        )
    }

    private fun defaultFirst(prefs: SharedPreferences, defaultName: String): CustomAnalysis =
        migrateFromLegacy(prefs, defaultName).first()
}
