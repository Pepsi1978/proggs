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

    /**
     * Frank-Wunsch 2026-05-15: Haengt die Anti-Datums-Regel an jeden System-Prompt
     * an. Damit erscheinen in den sichtbaren Textfeldern (erklaerung, beschreibung,
     * zusammenfassung, gesamtanalyse) keine "3.4."/"23.04."-Verweise mehr, weil
     * die das TTS-Vorlesen stören und den Erkenntnisfluss brechen.
     */
    private fun appendNoDatesRule(prompt: String): String {
        val rule = context.getString(R.string.ai_prompt_no_dates_rule)
        return "$prompt\n\n$rule"
    }

    private fun finalizePrompt(prompt: String): String =
        appendNoDatesRule(appendResponseLanguageInstruction(localizeSchemaKeys(prompt)))

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
        return when {
            // Goals prompt is fully localized via string resources
            scenario == 3 -> buildGoalsAnalysisSystemPrompt()
            // Self-insight prompt is fully localized via string resources
            scenario == 2 -> buildSelfInsightAnalysisSystemPrompt()
            // Summary prompt is fully localized via string resources
            scenario == 0 -> buildSummaryAnalysisSystemPrompt()
            scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX -> {
                val custom =
                    com.bestjournal.app.data.prefs.CustomAnalysesStore
                        .activePromptOrEmpty(encryptedPrefs, scenario)
                if (custom.isNotBlank()) buildCustomAnalysisPrompt(custom)
                else buildEntropyAnalysisSystemPrompt()
            }
            else -> buildEntropyAnalysisSystemPrompt()
        }
    }



    private fun getActiveUserPromptPrefix(freshAnalysis: Boolean): String {
        if (!freshAnalysis) return ""
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        val text = when {
            scenario == 0 -> context.getString(R.string.ai_prompt_fresh_summary)
            scenario == 2 -> context.getString(R.string.ai_prompt_fresh_insight)
            scenario == 3 -> context.getString(R.string.ai_prompt_fresh_goals)
            scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX ->
                context.getString(R.string.ai_prompt_fresh_custom)
            else -> context.getString(R.string.ai_prompt_fresh_default)
        }
        return "=== $text ==="
    }

    private fun getActiveUserPromptSuffix(entryCount: Int): String {
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        val text = when {
            scenario == 0 -> context.getString(R.string.ai_prompt_check_summary, entryCount)
            scenario == 2 -> context.getString(R.string.ai_prompt_check_insight, entryCount)
            scenario == 3 -> context.getString(R.string.ai_prompt_check_goals, entryCount)
            scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX ->
                context.getString(R.string.ai_prompt_check_custom, entryCount)
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
                result.getOrNull()
                    ?: return Result.failure(
                        Exception(context.getString(R.string.dashboard_gemini_unavailable))
                    )

            val cleanJson =
                jsonText
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .replace("—", ", ")
                    .trim()
            val blocks = parseAdviceJson(cleanJson, entryCount)

            // B2 — Profil-Re-Ranking: bei Custom-Profilen einen zweiten Gemini-Call,
            // der das top_massnahmen-Array auf Profil-Bezug priorisiert. Der erste
            // Call generiert die Analyse, der zweite optimiert sie auf den Benutzer-
            // Fokus. Schlaegt der Re-Ranking-Call fehl, bleiben die Original-Bloecke
            // unveraendert (defensive Fehlerbehandlung).
            val scenarioForRerank = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
            val finalBlocks =
                if (scenarioForRerank >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
                    val customForRerank =
                        com.bestjournal.app.data.prefs.CustomAnalysesStore
                            .activePromptOrEmpty(encryptedPrefs, scenarioForRerank)
                    if (customForRerank.isNotBlank() && blocks.isNotEmpty()) {
                        reRankTopActionsForProfile(
                            blocks = blocks,
                            userFocus = customForRerank,
                            allEntriesText = allEntriesText,
                            modelName = modelName,
                        )
                    } else blocks
                } else blocks

            adviceDashboardDao.deleteAll()
            adviceDashboardDao.upsertAll(finalBlocks)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * B2 — Profil-Re-Ranking: zweiter Gemini-Call, der das `topActionsJson`-Array
     * auf den Benutzer-Fokus priorisiert. Sortiert profilrelevante Punkte nach oben
     * und ersetzt bis zu 2 profilfremde Eintraege durch profilstaerkere aus den
     * Tagebucheintraegen. Schlaegt der Call fehl, werden die Original-Bloecke
     * unveraendert zurueckgegeben — kein harter Fehler.
     */
    private suspend fun reRankTopActionsForProfile(
        blocks: List<AdviceBlockEntity>,
        userFocus: String,
        allEntriesText: String,
        modelName: String,
    ): List<AdviceBlockEntity> {
        try {
            val originalTopActions = blocks.firstOrNull()?.topActionsJson ?: return blocks
            if (originalTopActions.isBlank() || originalTopActions == "[]") return blocks

            val systemPrompt = context.getString(R.string.ai_prompt_rerank_system)
            val userText = buildString {
                appendLine(context.getString(R.string.ai_prompt_rerank_user_focus_header))
                appendLine(userFocus)
                appendLine()
                appendLine(context.getString(R.string.ai_prompt_rerank_actions_header))
                appendLine(originalTopActions)
                appendLine()
                appendLine(context.getString(R.string.ai_prompt_rerank_entries_header))
                // Maximal 6000 Zeichen Tagebuch um Token-Eskalation zu vermeiden
                appendLine(allEntriesText.take(6000))
                appendLine()
                appendLine(context.getString(R.string.ai_prompt_rerank_instruction))
            }

            val rerankResult =
                firebaseAiService.generateContent(
                    prompt = userText,
                    modelName = modelName,
                    systemPrompt = systemPrompt,
                )
            val rawText = rerankResult.getOrNull() ?: return blocks
            val cleaned =
                rawText
                    .trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .replace("—", ", ")
                    .trim()

            if (!cleaned.startsWith("[") || !cleaned.endsWith("]")) {
                android.util.Log.w("Rerank", "Re-Ranker liefert kein JSON-Array, behalte Original")
                return blocks
            }
            val parsed = try {
                JSONArray(cleaned)
            } catch (e: Exception) {
                android.util.Log.w("Rerank", "Re-Ranker liefert ungueltiges JSON: ${e.message}")
                return blocks
            }
            if (parsed.length() != 5) {
                android.util.Log.w(
                    "Rerank",
                    "Re-Ranker liefert ${parsed.length()} statt 5 Eintraege, behalte Original",
                )
                return blocks
            }

            android.util.Log.d(
                "Rerank",
                "Profil-Re-Ranking erfolgreich, ${parsed.length()} Top-Massnahmen aktualisiert",
            )
            return blocks.map { it.copy(topActionsJson = cleaned) }
        } catch (e: Exception) {
            android.util.Log.w(
                "Rerank",
                "Re-Ranking fehlgeschlagen: ${e.message}, behalte Original-Bloecke",
            )
            return blocks
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
            "=== ALLE $entryCount TAGEBUCHEINTRÄGE (JEDEN EINZELNEN $scanLabel!) ==="
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
            else ->
                throw org.json.JSONException(
                    context.getString(R.string.error_json_key_missing, primaryKey, legacyKey)
                )
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

        // Save dynamic headers for any custom analysis (scenario >= 4)
        val scenario = encryptedPrefs.getInt(Constants.PREF_DASHBOARD_SCENARIO, 0)
        if (scenario >= Constants.FIRST_CUSTOM_SCENARIO_INDEX) {
            val top5 = json.optString(keys.headingTop5, json.optString("ueberschrift_top5", ""))
            val analyse = json.optString(keys.headingAnalysis, json.optString("ueberschrift_analyse", ""))
            val ergebnisse = json.optString(keys.headingResults, json.optString("ueberschrift_ergebnisse", ""))
            // A2 — fokus_kern + fokus_zitate sind die neuen Profil-Anker. Sie werden
            // in den Prefs gehalten und vom DashboardScreen oben als Profil-Header
            // gerendert (C1). Defensiv eingelesen — bei alten Prompts oder fehlender
            // KI-Antwort bleiben die Felder leer.
            val fokusKern = json.optString("fokus_kern", "")
            val fokusZitate = json.optJSONArray("fokus_zitate")?.toString() ?: "[]"
            encryptedPrefs
                .edit()
                .putString("custom_header_top5", top5)
                .putString("custom_header_analyse", analyse)
                .putString("custom_header_ergebnisse", ergebnisse)
                .putString("custom_fokus_kern", fokusKern)
                .putString("custom_fokus_zitate_json", fokusZitate)
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

