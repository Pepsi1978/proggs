package com.bestjournal.app.data.repository

import com.bestjournal.app.data.local.dao.AdviceDashboardDao
import com.bestjournal.app.data.local.entity.AdviceBlockEntity
import com.bestjournal.app.data.remote.ai.FirebaseAiService
import com.bestjournal.app.domain.model.Advice
import com.bestjournal.app.domain.model.AdviceBlock
import com.bestjournal.app.domain.model.AdvicePriority
import com.bestjournal.app.domain.model.DerivationEntry
import com.bestjournal.app.domain.model.TopAction
import com.bestjournal.app.util.Constants
import com.bestjournal.app.R
import com.bestjournal.app.util.DeviceLocale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AdviceRepository
@Inject
constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val firebaseAiService: FirebaseAiService,
    private val adviceDashboardDao: AdviceDashboardDao,
    private val encryptedPrefs: android.content.SharedPreferences,
) {
    /**
     * Replaces the German JSON-key templates in prompt schemas with their
     * device-language equivalents. The schema XMLs still contain the German
     * key names as placeholders (e.g. "gesamtanalyse"); at runtime they get
     * rewritten to whatever the current locale's json_key_* resources resolve to
     * (e.g. "overall_analysis" on English, "全体分析" on Japanese).
     *
     * Matching is done on the quoted form ("key") to avoid touching free prose.
     */
    private fun localizeSchemaKeys(prompt: String): String {
        val keys = loadJsonKeys()
        val replacements = listOf(
            "\"gesamtanalyse\"" to "\"${keys.overallAnalysis}\"",
            "\"top_massnahmen\"" to "\"${keys.topActions}\"",
            "\"kategorien\"" to "\"${keys.categories}\"",
            "\"ueberschrift_top5\"" to "\"${keys.headingTop5}\"",
            "\"ueberschrift_analyse\"" to "\"${keys.headingAnalysis}\"",
            "\"ueberschrift_ergebnisse\"" to "\"${keys.headingResults}\"",
            "\"fortschritte\"" to "\"${keys.progress}\"",
            "\"gesamt_entropie\"" to "\"${keys.overallEntropy}\"",
            "\"entropie_level\"" to "\"${keys.categoryEntropyLevel}\"",
            "\"ratschlaege\"" to "\"${keys.categoryAdvices}\"",
            "\"zusammenfassung\"" to "\"${keys.categorySummary}\"",
            "\"farbe\"" to "\"${keys.categoryColor}\"",
            "\"titel\"" to "\"${keys.itemTitle}\"",
            "\"beschreibung\"" to "\"${keys.itemDescription}\"",
            "\"erklaerung\"" to "\"${keys.itemExplanation}\"",
            "\"bezug\"" to "\"${keys.itemReference}\"",
            "\"prioritaet\"" to "\"${keys.itemPriority}\"",
            "\"verknuepfung\"" to "\"${keys.itemConnection}\"",
            "\"herleitung\"" to "\"${keys.itemDerivation}\"",
            "\"datum\"" to "\"${keys.derivationDate}\"",
            "\"hoch|mittel|niedrig\"" to
                "\"${keys.priorityHigh}|${keys.priorityMedium}|${keys.priorityLow}\"",
        )
        var result = prompt
        for ((from, to) in replacements) result = result.replace(from, to)
        return result
    }

    /**
     * Appends the response-language instruction so Gemini answers in the device locale.
     * The instruction is pulled from strings.xml (per locale), so for a Japanese user
     * the final instruction is in Japanese, for a Russian user in Russian, etc.
     */
    private fun appendResponseLanguageInstruction(prompt: String): String {
        val instruction = context.getString(R.string.ai_prompt_response_language)
        return "$prompt\n\n$instruction"
    }

    private fun finalizePrompt(prompt: String): String =
        appendResponseLanguageInstruction(localizeSchemaKeys(prompt))

    /** Entropy analysis prompt — fully localized via string resources. */
    private fun buildEntropyAnalysisSystemPrompt(): String {
        val prompt = listOf(
            context.getString(R.string.ai_prompt_entropy_intro),
            context.getString(R.string.ai_prompt_entropy_entry_rules),
            context.getString(R.string.ai_prompt_entropy_rules),
            context.getString(R.string.ai_prompt_entropy_schema_header),
            context.getString(R.string.ai_prompt_entropy_schema_fields),
            context.getString(R.string.ai_prompt_entropy_schema_categories),
            context.getString(R.string.ai_prompt_entropy_output),
        ).joinToString("\n\n")
        return finalizePrompt(prompt)
    }

    /** Summary analysis prompt — fully localized via string resources. */
    private fun buildSummaryAnalysisSystemPrompt(): String {
        val prompt = listOf(
            context.getString(R.string.ai_prompt_summary_intro),
            context.getString(R.string.ai_prompt_summary_entry_rules),
            context.getString(R.string.ai_prompt_summary_rules),
            context.getString(R.string.ai_prompt_summary_schema_header),
            context.getString(R.string.ai_prompt_summary_schema_fields),
            context.getString(R.string.ai_prompt_summary_schema_categories),
            context.getString(R.string.ai_prompt_summary_output),
        ).joinToString("\n\n")
        return finalizePrompt(prompt)
    }

    /** Goals analysis prompt — fully localized via string resources. */
    private fun buildGoalsAnalysisSystemPrompt(): String {
        val langStyle = context.getString(R.string.ai_prompt_language_style)
        val prompt = listOf(
            context.getString(R.string.ai_prompt_goals_intro),
            context.getString(R.string.ai_prompt_goals_definition),
            context.getString(R.string.ai_prompt_goals_entry_rules),
            context.getString(R.string.ai_prompt_goals_rules, langStyle),
            context.getString(R.string.ai_prompt_goals_schema),
            context.getString(R.string.ai_prompt_goals_output),
        ).joinToString("\n\n")
        return finalizePrompt(prompt)
    }

    /** Self-insight analysis prompt — fully localized via string resources. */
    private fun buildSelfInsightAnalysisSystemPrompt(): String {
        val prompt = listOf(
            context.getString(R.string.ai_prompt_insight_intro),
            context.getString(R.string.ai_prompt_insight_attitude),
            context.getString(R.string.ai_prompt_insight_entry_rules),
            context.getString(R.string.ai_prompt_insight_rules),
            context.getString(R.string.ai_prompt_insight_schema_header),
            context.getString(R.string.ai_prompt_insight_schema_fields),
            context.getString(R.string.ai_prompt_insight_schema_categories),
            context.getString(R.string.ai_prompt_insight_output),
        ).joinToString("\n\n")
        return finalizePrompt(prompt)
    }

    /** Custom analysis prompt — fully localized via string resources. */
    private fun buildCustomAnalysisPrompt(userFocus: String): String {
        val prompt = listOf(
            context.getString(R.string.ai_prompt_custom_intro, userFocus),
            context.getString(R.string.ai_prompt_custom_headings),
            context.getString(R.string.ai_prompt_custom_rules),
            context.getString(R.string.ai_prompt_custom_schema),
            context.getString(R.string.ai_prompt_custom_output),
        ).joinToString("\n\n")
        return finalizePrompt(prompt)
    }

    private fun getActiveSystemPrompt(): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        return when (scenario) {
            // Goals prompt is fully localized via string resources
            3 -> buildGoalsAnalysisSystemPrompt()
            // Self-insight prompt is fully localized via string resources
            2 -> buildSelfInsightAnalysisSystemPrompt()
            // Summary prompt is fully localized via string resources
            0 -> buildSummaryAnalysisSystemPrompt()
            4 -> {
                val custom = encryptedPrefs.getString(Constants.PREF_CUSTOM_PROMPT, "") ?: ""
                if (custom.isNotBlank()) buildCustomAnalysisPrompt(custom)
                else buildEntropyAnalysisSystemPrompt()
            }
            else -> buildEntropyAnalysisSystemPrompt()
        }
    }



    private fun getActiveUserPromptPrefix(freshAnalysis: Boolean): String {
        if (!freshAnalysis) return ""
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        val text = when (scenario) {
            0 -> context.getString(R.string.ai_prompt_fresh_summary)
            2 -> context.getString(R.string.ai_prompt_fresh_insight)
            3 -> context.getString(R.string.ai_prompt_fresh_goals)
            4 -> context.getString(R.string.ai_prompt_fresh_custom)
            else -> context.getString(R.string.ai_prompt_fresh_default)
        }
        return "=== $text ==="
    }

    private fun getActiveUserPromptSuffix(entryCount: Int): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        val text = when (scenario) {
            0 -> context.getString(R.string.ai_prompt_check_summary, entryCount)
            2 -> context.getString(R.string.ai_prompt_check_insight, entryCount)
            3 -> context.getString(R.string.ai_prompt_check_goals, entryCount)
            4 -> context.getString(R.string.ai_prompt_check_custom, entryCount)
            else -> context.getString(R.string.ai_prompt_check_default, entryCount)
        }
        return "=== $text ==="
    }

    // Undo support: store previous state in memory
    private var previousBlocks: List<AdviceBlockEntity>? = null

    val canUndo: Boolean
        get() = previousBlocks != null

    suspend fun undoLastRefresh(): Boolean {
        val prev = previousBlocks ?: return false
        adviceDashboardDao.deleteAll()
        // Reset IDs to 0 so Room auto-generates fresh IDs
        val freshEntities = prev.map { it.copy(id = 0) }
        adviceDashboardDao.upsertAll(freshEntities)
        previousBlocks = null
        return true
    }

    suspend fun clearDashboard() {
        adviceDashboardDao.deleteAll()
    }

    /** Number of advice blocks currently in the dashboard database. */
    suspend fun getBlockCount(): Int = adviceDashboardDao.getBlockCount()

    fun getAllAdviceBlocks(): Flow<List<AdviceBlock>> {
        return adviceDashboardDao.getAll().map { entities -> entities.map { it.toDomain() } }
    }

    suspend fun analyzeEntropy(
        allEntriesText: String,
        entryCount: Int,
        freshAnalysis: Boolean = false,
        modelName: String = FirebaseAiService.MODEL_FLASH_LITE,
    ): Result<Unit> {
        return try {
            // Save current state for undo before refreshing
            val existingBlocks = adviceDashboardDao.getAllSync()
            if (existingBlocks.isNotEmpty()) {
                previousBlocks = existingBlocks
            }

            // Only use previous context for automatic updates, NOT for manual refresh
            val previousContext = if (freshAnalysis) "" else buildPreviousContext(existingBlocks)

            val userText = buildUserText(allEntriesText, previousContext, entryCount, freshAnalysis)

            val result =
                firebaseAiService.generateContent(
                    prompt = userText,
                    modelName = modelName,
                    systemPrompt = getActiveSystemPrompt(),
                )
            val jsonText =
                result.getOrNull() ?: return Result.failure(Exception("Keine Antwort von Gemini"))

            val cleanJson =
                jsonText
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .replace("—", ", ")
                    .trim()
            val blocks = parseAdviceJson(cleanJson, entryCount)

            adviceDashboardDao.deleteAll()
            adviceDashboardDao.upsertAll(blocks)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPreviousContext(existingBlocks: List<AdviceBlockEntity>): String {
        if (existingBlocks.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("=== BISHERIGE ANALYSE (baue darauf auf, überschreibe sie nicht) ===")

        val overallAnalysis = existingBlocks.firstOrNull()?.overallAnalysis ?: ""
        if (overallAnalysis.isNotBlank()) {
            sb.appendLine("Bisherige Gesamtanalyse: $overallAnalysis")
            sb.appendLine()
        }

        existingBlocks.forEach { block ->
            sb.appendLine(
                "Kategorie '${block.categoryName}': Belastung=${block.entropyLevel}, ${block.categorySummary}"
            )
        }

        sb.appendLine("=== ENDE BISHERIGE ANALYSE ===")
        sb.appendLine()
        sb.appendLine("Aktualisiere und ERWEITERE die bisherige Analyse mit den neuen Einträgen.")
        sb.appendLine("Behalte wichtige Erkenntnisse bei und ergänze neue Muster.")
        sb.appendLine()

        return sb.toString()
    }

    private fun buildUserText(
        allEntriesText: String,
        previousContext: String,
        entryCount: Int,
        freshAnalysis: Boolean = false,
    ): String {
        val sb = StringBuilder()
        if (previousContext.isNotBlank()) {
            sb.appendLine(previousContext)
        } else {
            sb.appendLine(getActiveUserPromptPrefix(freshAnalysis))
            sb.appendLine()
        }
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        val scanLabel = if (scenario == 3) "AUF ZIELE DURCHSUCHEN" else "ANALYSIEREN"
        sb.appendLine(
            "=== ALLE $entryCount TAGEBUCHEINTR\u00c4GE (JEDEN EINZELNEN $scanLabel!) ==="
        )
        sb.appendLine(allEntriesText)
        sb.appendLine()
        sb.appendLine(getActiveUserPromptSuffix(entryCount))
        return sb.toString()
    }

    /**
     * Snapshot of all localized JSON keys the AI should use.
     * Resolved once per parse via context.getString() so the keys follow the
     * current device language (de, en, ja, fr, …). The AI sees the same keys
     * in the prompt schema and the parser reads the same keys from the response.
     */
    private data class JsonKeys(
        val overallAnalysis: String,
        val topActions: String,
        val categories: String,
        val headingTop5: String,
        val headingAnalysis: String,
        val headingResults: String,
        val progress: String,
        val overallEntropy: String,
        val trend: String,
        val categoryName: String,
        val categoryIcon: String,
        val categoryColor: String,
        val categoryEntropyLevel: String,
        val categorySummary: String,
        val categoryAdvices: String,
        val itemTitle: String,
        val itemDescription: String,
        val itemExplanation: String,
        val itemReference: String,
        val itemPriority: String,
        val itemConnection: String,
        val itemDerivation: String,
        val derivationDate: String,
        val derivationSummary: String,
        val priorityHigh: String,
        val priorityMedium: String,
        val priorityLow: String,
    )

    private fun loadJsonKeys(): JsonKeys = JsonKeys(
        overallAnalysis = context.getString(R.string.json_key_overall_analysis),
        topActions = context.getString(R.string.json_key_top_actions),
        categories = context.getString(R.string.json_key_categories),
        headingTop5 = context.getString(R.string.json_key_heading_top5),
        headingAnalysis = context.getString(R.string.json_key_heading_analysis),
        headingResults = context.getString(R.string.json_key_heading_results),
        progress = context.getString(R.string.json_key_progress),
        overallEntropy = context.getString(R.string.json_key_overall_entropy),
        trend = context.getString(R.string.json_key_trend),
        categoryName = context.getString(R.string.json_key_category_name),
        categoryIcon = context.getString(R.string.json_key_category_icon),
        categoryColor = context.getString(R.string.json_key_category_color),
        categoryEntropyLevel = context.getString(R.string.json_key_category_entropy_level),
        categorySummary = context.getString(R.string.json_key_category_summary),
        categoryAdvices = context.getString(R.string.json_key_category_advices),
        itemTitle = context.getString(R.string.json_key_item_title),
        itemDescription = context.getString(R.string.json_key_item_description),
        itemExplanation = context.getString(R.string.json_key_item_explanation),
        itemReference = context.getString(R.string.json_key_item_reference),
        itemPriority = context.getString(R.string.json_key_item_priority),
        itemConnection = context.getString(R.string.json_key_item_connection),
        itemDerivation = context.getString(R.string.json_key_item_derivation),
        derivationDate = context.getString(R.string.json_key_derivation_date),
        derivationSummary = context.getString(R.string.json_key_derivation_summary),
        priorityHigh = context.getString(R.string.json_value_priority_high),
        priorityMedium = context.getString(R.string.json_value_priority_medium),
        priorityLow = context.getString(R.string.json_value_priority_low),
    )

    /** Reads a JSON string with fallback to the legacy German key (for old DB entries). */
    private fun JSONObject.getStringWithFallback(primaryKey: String, legacyKey: String): String {
        return when {
            has(primaryKey) && !isNull(primaryKey) -> getString(primaryKey)
            has(legacyKey) && !isNull(legacyKey) -> getString(legacyKey)
            else -> throw org.json.JSONException("Neither '$primaryKey' nor '$legacyKey' found")
        }
    }

    private fun parseAdviceJson(jsonString: String, entryCount: Int): List<AdviceBlockEntity> {
        val keys = loadJsonKeys()
        val json = JSONObject(jsonString)
        val overallAnalysis = json.getStringWithFallback(keys.overallAnalysis, "gesamtanalyse")
        val topActionsArray = json.optJSONArray(keys.topActions) ?: json.optJSONArray("top_massnahmen")
        val topActionsJson = topActionsArray?.toString() ?: "[]"
        val categories = json.optJSONArray(keys.categories) ?: json.getJSONArray("kategorien")
        val now = System.currentTimeMillis()

        // Save dynamic headers for custom analysis (scenario 4)
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        if (scenario == 4) {
            val top5 = json.optString(keys.headingTop5, json.optString("ueberschrift_top5", ""))
            val analyse = json.optString(keys.headingAnalysis, json.optString("ueberschrift_analyse", ""))
            val ergebnisse = json.optString(keys.headingResults, json.optString("ueberschrift_ergebnisse", ""))
            encryptedPrefs
                .edit()
                .putString("custom_header_top5", top5)
                .putString("custom_header_analyse", analyse)
                .putString("custom_header_ergebnisse", ergebnisse)
                .apply()
        }

        return (0 until categories.length()).map { i ->
            val cat = categories.getJSONObject(i)
            val adviceArray = cat.optJSONArray(keys.categoryAdvices) ?: cat.getJSONArray("ratschlaege")

            AdviceBlockEntity(
                categoryName = cat.getStringWithFallback(keys.categoryName, "name"),
                categoryIcon = cat.getStringWithFallback(keys.categoryIcon, "icon"),
                categoryColor = cat.getStringWithFallback(keys.categoryColor, "farbe"),
                entropyLevel = (cat.opt(keys.categoryEntropyLevel) as? Number
                    ?: cat.opt("entropie_level") as? Number
                    ?: 0).toFloat(),
                categorySummary = cat.getStringWithFallback(keys.categorySummary, "zusammenfassung"),
                adviceJson = adviceArray.toString(),
                overallAnalysis = overallAnalysis,
                topActionsJson = topActionsJson,
                lastUpdated = now,
                basedOnEntryCount = entryCount,
            )
        }
    }

    /** Converts an advice-block entity to its domain representation. */
    internal fun AdviceBlockEntity.toDomain(): AdviceBlock {
        val keys = loadJsonKeys()
        val advices =
            try {
                val array = JSONArray(adviceJson)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    val derivationArray = obj.optJSONArray(keys.itemDerivation)
                        ?: obj.optJSONArray("herleitung")
                    val derivation =
                        if (derivationArray != null) {
                            try {
                                (0 until derivationArray.length()).map { j ->
                                    val h = derivationArray.getJSONObject(j)
                                    DerivationEntry(
                                        date = h.optString(keys.derivationDate,
                                            h.optString("datum", "")),
                                        summary = h.optString(keys.derivationSummary,
                                            h.optString("zusammenfassung", "")),
                                    )
                                }
                            } catch (_: Exception) {
                                emptyList()
                            }
                        } else emptyList()

                    Advice(
                        title = obj.optString(keys.itemTitle, obj.optString("titel", "")),
                        description = obj.optString(keys.itemDescription,
                            obj.optString("beschreibung", "")),
                        priority = mapPriority(
                            obj.optString(keys.itemPriority,
                                obj.optString("prioritaet", keys.priorityMedium)),
                            keys,
                        ),
                        connection = parseConnection(obj, keys),
                        derivation = derivation,
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }

        val topActions =
            try {
                val array = JSONArray(topActionsJson)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    TopAction(
                        title = obj.optString(keys.itemTitle, obj.optString("titel", "")),
                        description = obj.optString(keys.itemDescription,
                            obj.optString("beschreibung", "")),
                        detailedDescription = obj.optString(keys.itemExplanation,
                            obj.optString("erklaerung", "")),
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }

        return AdviceBlock(
            id = id,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColor = categoryColor,
            entropyLevel = entropyLevel,
            categorySummary = categorySummary,
            advices = advices,
            overallAnalysis = overallAnalysis,
            topActions = topActions,
            lastUpdated = lastUpdated,
            basedOnEntryCount = basedOnEntryCount,
        )
    }

    private fun mapPriority(raw: String, keys: JsonKeys): AdvicePriority {
        val normalized = raw.trim().lowercase()
        return when (normalized) {
            keys.priorityHigh.lowercase(), "high", "hoch" -> AdvicePriority.HIGH
            keys.priorityLow.lowercase(), "low", "niedrig" -> AdvicePriority.LOW
            else -> AdvicePriority.MEDIUM
        }
    }

    /**
     * Parses the connection/verknuepfung field. The AI sometimes returns 0, "null",
     * or a numeric value when there is no meaningful link — treat those as empty.
     */
    private fun parseConnection(obj: JSONObject, keys: JsonKeys): String {
        val key = when {
            obj.has(keys.itemConnection) -> keys.itemConnection
            obj.has("verknuepfung") -> "verknuepfung"
            else -> return ""
        }
        if (obj.isNull(key)) return ""
        val raw = obj.opt(key) ?: return ""
        if (raw is Number) return ""
        val str = raw.toString().trim()
        if (str.isEmpty()) return ""
        if (str.equals("null", ignoreCase = true)) return ""
        if (str.toDoubleOrNull() != null) return ""
        return str
    }
}

