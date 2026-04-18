package com.bestjournal.app.util

import android.content.Context
import com.bestjournal.app.R
import java.time.LocalDate
import kotlin.math.abs

data class DailyPrompt(val text: String, val category: PromptCategory, val id: String)

enum class PromptCategory(val displayNameResId: Int) {
    SELF_REFLECTION(R.string.prompt_cat_self_reflection),
    GRATITUDE(R.string.prompt_cat_gratitude),
    FUTURE(R.string.prompt_cat_future),
    RELATIONSHIPS(R.string.prompt_cat_relationships),
}

/**
 * Provides a deterministic daily writing prompt based on the current date. All users see the same
 * prompt on the same day (no server required). The prompt changes at midnight local time. Prompts
 * are loaded from string-array resources to support localization.
 */
object DailyPromptProvider {

    /**
     * Returns today's writing prompt. Deterministic: same date = same prompt for all users. Uses
     * epoch day modulo to cycle through all prompts over ~64 days.
     */
    fun getTodaysPrompt(context: Context): DailyPrompt {
        val allPrompts = buildPromptList(context)
        if (allPrompts.isEmpty()) return DailyPrompt("", PromptCategory.SELF_REFLECTION, "empty")
        val daysSinceEpoch = LocalDate.now().toEpochDay()
        val index = abs((daysSinceEpoch % allPrompts.size).toInt())
        return allPrompts[index]
    }

    /** Total number of available prompts. */
    fun getTotalPrompts(context: Context): Int = buildPromptList(context).size

    private fun buildPromptList(context: Context): List<DailyPrompt> {
        val res = context.resources
        val categories =
            listOf(
                PromptCategory.SELF_REFLECTION to
                    res.getStringArray(R.array.prompts_self_reflection),
                PromptCategory.GRATITUDE to res.getStringArray(R.array.prompts_gratitude),
                PromptCategory.FUTURE to res.getStringArray(R.array.prompts_future),
                PromptCategory.RELATIONSHIPS to res.getStringArray(R.array.prompts_relationships),
            )
        return categories.flatMap { (category, strings) ->
            strings.mapIndexed { i, text ->
                DailyPrompt(
                    text = text,
                    category = category,
                    id = "${category.name.lowercase()}_$i",
                )
            }
        }
    }
}
