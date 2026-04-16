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
    /** Entropy analysis prompt — fully localized via string resources. */
    private fun buildEntropyAnalysisSystemPrompt(): String {
        val langOverride = context.getString(R.string.ai_prompt_language_override)
        val prompt =
            listOf(
                    context.getString(R.string.ai_prompt_entropy_intro),
                    context.getString(R.string.ai_prompt_entropy_entry_rules),
                    context.getString(R.string.ai_prompt_entropy_rules),
                    context.getString(R.string.ai_prompt_entropy_schema_header),
                    context.getString(R.string.ai_prompt_entropy_schema_fields),
                    context.getString(R.string.ai_prompt_entropy_schema_categories),
                    context.getString(R.string.ai_prompt_entropy_output),
                )
                .joinToString("\n\n")
        return if (langOverride.isNotBlank()) "$langOverride\n\n$prompt" else prompt
    }

    /** Summary analysis prompt — fully localized via string resources. */
    private fun buildSummaryAnalysisSystemPrompt(): String {
        val langOverride = context.getString(R.string.ai_prompt_language_override)
        val prompt =
            listOf(
                    context.getString(R.string.ai_prompt_summary_intro),
                    context.getString(R.string.ai_prompt_summary_entry_rules),
                    context.getString(R.string.ai_prompt_summary_rules),
                    context.getString(R.string.ai_prompt_summary_schema_header),
                    context.getString(R.string.ai_prompt_summary_schema_fields),
                    context.getString(R.string.ai_prompt_summary_schema_categories),
                    context.getString(R.string.ai_prompt_summary_output),
                )
                .joinToString("\n\n")
        return if (langOverride.isNotBlank()) "$langOverride\n\n$prompt" else prompt
    }

    /** Goals analysis prompt — fully localized via string resources. */
    private fun buildGoalsAnalysisSystemPrompt(): String {
        val langOverride = context.getString(R.string.ai_prompt_language_override)
        val langStyle = context.getString(R.string.ai_prompt_language_style)
        val prompt =
            listOf(
                    context.getString(R.string.ai_prompt_goals_intro),
                    context.getString(R.string.ai_prompt_goals_definition),
                    context.getString(R.string.ai_prompt_goals_entry_rules),
                    context.getString(R.string.ai_prompt_goals_rules, langStyle),
                    context.getString(R.string.ai_prompt_goals_schema),
                    context.getString(R.string.ai_prompt_goals_output),
                )
                .joinToString("\n\n")
        return if (langOverride.isNotBlank()) "$langOverride\n\n$prompt" else prompt
    }

    /** Self-insight analysis prompt — fully localized via string resources. */
    private fun buildSelfInsightAnalysisSystemPrompt(): String {
        val langOverride = context.getString(R.string.ai_prompt_language_override)
        val prompt =
            listOf(
                    context.getString(R.string.ai_prompt_insight_intro),
                    context.getString(R.string.ai_prompt_insight_attitude),
                    context.getString(R.string.ai_prompt_insight_entry_rules),
                    context.getString(R.string.ai_prompt_insight_rules),
                    context.getString(R.string.ai_prompt_insight_schema_header),
                    context.getString(R.string.ai_prompt_insight_schema_fields),
                    context.getString(R.string.ai_prompt_insight_schema_categories),
                    context.getString(R.string.ai_prompt_insight_output),
                )
                .joinToString("\n\n")
        return if (langOverride.isNotBlank()) "$langOverride\n\n$prompt" else prompt
    }

    /** Custom analysis prompt — fully localized via string resources. */
    private fun buildCustomAnalysisPrompt(userFocus: String): String {
        val langOverride = context.getString(R.string.ai_prompt_language_override)
        val prompt =
            listOf(
                    context.getString(R.string.ai_prompt_custom_intro, userFocus),
                    context.getString(R.string.ai_prompt_custom_headings),
                    context.getString(R.string.ai_prompt_custom_rules),
                    context.getString(R.string.ai_prompt_custom_schema),
                    context.getString(R.string.ai_prompt_custom_output),
                )
                .joinToString("\n\n")
        return if (langOverride.isNotBlank()) "$langOverride\n\n$prompt" else prompt
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

    private fun parseAdviceJson(jsonString: String, entryCount: Int): List<AdviceBlockEntity> {
        val json = JSONObject(jsonString)
        val overallAnalysis = json.getString("gesamtanalyse")
        val topActionsJson = json.optJSONArray("top_massnahmen")?.toString() ?: "[]"
        val categories = json.getJSONArray("kategorien")
        val now = System.currentTimeMillis()

        // Save dynamic headers for custom analysis (scenario 4)
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        if (scenario == 4) {
            encryptedPrefs
                .edit()
                .putString("custom_header_top5", json.optString("ueberschrift_top5", ""))
                .putString("custom_header_analyse", json.optString("ueberschrift_analyse", ""))
                .putString(
                    "custom_header_ergebnisse",
                    json.optString("ueberschrift_ergebnisse", ""),
                )
                .apply()
        }

        return (0 until categories.length()).map { i ->
            val cat = categories.getJSONObject(i)
            val adviceArray = cat.getJSONArray("ratschlaege")

            AdviceBlockEntity(
                categoryName = cat.getString("name"),
                categoryIcon = cat.getString("icon"),
                categoryColor = cat.getString("farbe"),
                entropyLevel = cat.getDouble("entropie_level").toFloat(),
                categorySummary = cat.getString("zusammenfassung"),
                adviceJson = adviceArray.toString(),
                overallAnalysis = overallAnalysis,
                topActionsJson = topActionsJson,
                lastUpdated = now,
                basedOnEntryCount = entryCount,
            )
        }
    }
}

private fun AdviceBlockEntity.toDomain(): AdviceBlock {
    val advices =
        try {
            val array = JSONArray(adviceJson)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val derivation =
                    try {
                        val herleitungArray = obj.optJSONArray("herleitung")
                        if (herleitungArray != null) {
                            (0 until herleitungArray.length()).map { j ->
                                val h = herleitungArray.getJSONObject(j)
                                DerivationEntry(
                                    date = h.optString("datum", ""),
                                    summary = h.optString("zusammenfassung", ""),
                                )
                            }
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                Advice(
                    title = obj.getString("titel"),
                    description = obj.getString("beschreibung"),
                    priority =
                        when (obj.optString("prioritaet", "mittel").lowercase()) {
                            "hoch", "high" -> AdvicePriority.HIGH
                            "niedrig", "low" -> AdvicePriority.LOW
                            else -> AdvicePriority.MEDIUM
                        },
                    connection = obj.optString("verknuepfung", ""),
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
                    title = obj.getString("titel"),
                    description = obj.getString("beschreibung"),
                    detailedDescription = obj.optString("erklaerung", ""),
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
