package com.bestjournal.app.util

import java.time.LocalDate
import kotlin.math.abs

data class DailyPrompt(
    val text: String,
    val category: PromptCategory,
    val id: String,
)

enum class PromptCategory(val displayName: String) {
    SELF_REFLECTION("Selbstreflexion"),
    GRATITUDE("Dankbarkeit"),
    FUTURE("Zukunft"),
    RELATIONSHIPS("Beziehungen"),
}

/**
 * Provides a deterministic daily writing prompt based on the current date.
 * All users see the same prompt on the same day (no server required).
 * The prompt changes at midnight local time.
 */
object DailyPromptProvider {

    private val prompts: List<DailyPrompt> = buildList {
        // ── Selbstreflexion (16) ────────────────────────────────────────
        add(DailyPrompt("Was hat dich heute zum L\u00e4cheln gebracht?", PromptCategory.SELF_REFLECTION, "self_0"))
        add(DailyPrompt("Welchen Gedanken konntest du heute nicht loslassen?", PromptCategory.SELF_REFLECTION, "self_1"))
        add(DailyPrompt("Was hast du heute \u00fcber dich selbst gelernt?", PromptCategory.SELF_REFLECTION, "self_2"))
        add(DailyPrompt("Welcher Moment war heute am intensivsten?", PromptCategory.SELF_REFLECTION, "self_3"))
        add(DailyPrompt("Was w\u00fcrdest du heute anders machen, wenn du k\u00f6nntest?", PromptCategory.SELF_REFLECTION, "self_4"))
        add(DailyPrompt("Welches Gef\u00fchl hat dich heute am meisten begleitet?", PromptCategory.SELF_REFLECTION, "self_5"))
        add(DailyPrompt("Was hat dich heute \u00fcberrascht?", PromptCategory.SELF_REFLECTION, "self_6"))
        add(DailyPrompt("Wor\u00fcber hast du heute am l\u00e4ngsten nachgedacht?", PromptCategory.SELF_REFLECTION, "self_7"))
        add(DailyPrompt("Was hat dir heute Energie gegeben?", PromptCategory.SELF_REFLECTION, "self_8"))
        add(DailyPrompt("Was hat dir heute Energie geraubt?", PromptCategory.SELF_REFLECTION, "self_9"))
        add(DailyPrompt("Welche Gewohnheit m\u00f6chtest du ver\u00e4ndern?", PromptCategory.SELF_REFLECTION, "self_10"))
        add(DailyPrompt("Was bedeutet Erfolg f\u00fcr dich gerade?", PromptCategory.SELF_REFLECTION, "self_11"))
        add(DailyPrompt("Welche Angst steht dir gerade im Weg?", PromptCategory.SELF_REFLECTION, "self_12"))
        add(DailyPrompt("Was w\u00fcrde dein j\u00fcngeres Ich zu deinem heutigen Leben sagen?", PromptCategory.SELF_REFLECTION, "self_13"))
        add(DailyPrompt("Welchen Rat gibst du dir selbst gerade?", PromptCategory.SELF_REFLECTION, "self_14"))
        add(DailyPrompt("Was macht dich einzigartig?", PromptCategory.SELF_REFLECTION, "self_15"))

        // ── Dankbarkeit (16) ───────────────────────────────────────────
        add(DailyPrompt("Wof\u00fcr bist du heute dankbar?", PromptCategory.GRATITUDE, "grat_0"))
        add(DailyPrompt("Welche Person hat heute deinen Tag bereichert?", PromptCategory.GRATITUDE, "grat_1"))
        add(DailyPrompt("Welches kleine Detail hat dich heute erfreut?", PromptCategory.GRATITUDE, "grat_2"))
        add(DailyPrompt("Welche F\u00e4higkeit besitzt du, die du oft f\u00fcr selbstverst\u00e4ndlich h\u00e4ltst?", PromptCategory.GRATITUDE, "grat_3"))
        add(DailyPrompt("Was ist das Beste, das dir diese Woche passiert ist?", PromptCategory.GRATITUDE, "grat_4"))
        add(DailyPrompt("Wem w\u00fcrdest du gerne danken, und wof\u00fcr?", PromptCategory.GRATITUDE, "grat_5"))
        add(DailyPrompt("Welcher Ort gibt dir ein Gef\u00fchl von Zuhause?", PromptCategory.GRATITUDE, "grat_6"))
        add(DailyPrompt("Welche Erinnerung macht dich sofort gl\u00fccklich?", PromptCategory.GRATITUDE, "grat_7"))
        add(DailyPrompt("Was an deinem K\u00f6rper verdient mehr Wertsch\u00e4tzung?", PromptCategory.GRATITUDE, "grat_8"))
        add(DailyPrompt("Welches Essen hat dir heute richtig gut getan?", PromptCategory.GRATITUDE, "grat_9"))
        add(DailyPrompt("Welche Musik hat dich zuletzt ber\u00fchrt?", PromptCategory.GRATITUDE, "grat_10"))
        add(DailyPrompt("Was ist das Wertvollste, das du besitzt \u2014 und kein Geld kosten w\u00fcrde?", PromptCategory.GRATITUDE, "grat_11"))
        add(DailyPrompt("Welche Erfahrung aus der Vergangenheit hat dich st\u00e4rker gemacht?", PromptCategory.GRATITUDE, "grat_12"))
        add(DailyPrompt("Was an deinem Alltag ist eigentlich ein Privileg?", PromptCategory.GRATITUDE, "grat_13"))
        add(DailyPrompt("Wer ist dein heimlicher Held im Alltag?", PromptCategory.GRATITUDE, "grat_14"))
        add(DailyPrompt("Welche Jahreszeit genie\u00dft du gerade am meisten, und warum?", PromptCategory.GRATITUDE, "grat_15"))

        // ── Zukunft (16) ───────────────────────────────────────────────
        add(DailyPrompt("Was m\u00f6chtest du in einem Jahr erreicht haben?", PromptCategory.FUTURE, "future_0"))
        add(DailyPrompt("Wie sieht dein perfekter Tag in f\u00fcnf Jahren aus?", PromptCategory.FUTURE, "future_1"))
        add(DailyPrompt("Welches Ziel verfolgst du gerade am leidenschaftlichsten?", PromptCategory.FUTURE, "future_2"))
        add(DailyPrompt("Was w\u00fcrdest du tun, wenn du w\u00fcsstest, dass du nicht scheitern kannst?", PromptCategory.FUTURE, "future_3"))
        add(DailyPrompt("Welche neue F\u00e4higkeit m\u00f6chtest du lernen?", PromptCategory.FUTURE, "future_4"))
        add(DailyPrompt("Wohin m\u00f6chtest du als N\u00e4chstes reisen?", PromptCategory.FUTURE, "future_5"))
        add(DailyPrompt("Was f\u00fcr ein Mensch m\u00f6chtest du in zehn Jahren sein?", PromptCategory.FUTURE, "future_6"))
        add(DailyPrompt("Welchen Traum hast du aufgegeben, den du wiederbeleben k\u00f6nntest?", PromptCategory.FUTURE, "future_7"))
        add(DailyPrompt("Was ist der mutigste Schritt, den du gerade machen k\u00f6nntest?", PromptCategory.FUTURE, "future_8"))
        add(DailyPrompt("Welches Projekt begeistert dich, das du noch nicht begonnen hast?", PromptCategory.FUTURE, "future_9"))
        add(DailyPrompt("Was m\u00fcsste passieren, damit du sagst: Jetzt bin ich zufrieden?", PromptCategory.FUTURE, "future_10"))
        add(DailyPrompt("Welche Ver\u00e4nderung w\u00fcrdest du in der Welt gerne sehen?", PromptCategory.FUTURE, "future_11"))
        add(DailyPrompt("Was steht auf deiner Bucket List ganz oben?", PromptCategory.FUTURE, "future_12"))
        add(DailyPrompt("Welche schlechte Gewohnheit m\u00f6chtest du bis n\u00e4chsten Monat ablegen?", PromptCategory.FUTURE, "future_13"))
        add(DailyPrompt("Wie m\u00f6chtest du in Erinnerung behalten werden?", PromptCategory.FUTURE, "future_14"))
        add(DailyPrompt("Was ist ein kleiner Schritt, den du morgen machen kannst?", PromptCategory.FUTURE, "future_15"))

        // ── Beziehungen (16) ───────────────────────────────────────────
        add(DailyPrompt("Wem w\u00fcrdest du gerne etwas sagen, was du bisher nicht gesagt hast?", PromptCategory.RELATIONSHIPS, "rel_0"))
        add(DailyPrompt("Welche Beziehung in deinem Leben verdient mehr Aufmerksamkeit?", PromptCategory.RELATIONSHIPS, "rel_1"))
        add(DailyPrompt("Was sch\u00e4tzt du an deinem besten Freund am meisten?", PromptCategory.RELATIONSHIPS, "rel_2"))
        add(DailyPrompt("Wann hast du dich zuletzt wirklich verstanden gef\u00fchlt?", PromptCategory.RELATIONSHIPS, "rel_3"))
        add(DailyPrompt("Was w\u00fcrdest du einer Person sagen, die dir wichtig ist?", PromptCategory.RELATIONSHIPS, "rel_4"))
        add(DailyPrompt("Welcher Mensch hat dein Leben am st\u00e4rksten gepr\u00e4gt?", PromptCategory.RELATIONSHIPS, "rel_5"))
        add(DailyPrompt("Wie zeigst du anderen, dass du sie liebst?", PromptCategory.RELATIONSHIPS, "rel_6"))
        add(DailyPrompt("Welchen Konflikt w\u00fcrdest du gerne l\u00f6sen?", PromptCategory.RELATIONSHIPS, "rel_7"))
        add(DailyPrompt("Was bedeutet Freundschaft f\u00fcr dich?", PromptCategory.RELATIONSHIPS, "rel_8"))
        add(DailyPrompt("Wer hat dir zuletzt etwas Wichtiges beigebracht?", PromptCategory.RELATIONSHIPS, "rel_9"))
        add(DailyPrompt("Welche Eigenschaft bewunderst du an anderen am meisten?", PromptCategory.RELATIONSHIPS, "rel_10"))
        add(DailyPrompt("Mit wem w\u00fcrdest du gerne einen ganzen Tag verbringen?", PromptCategory.RELATIONSHIPS, "rel_11"))
        add(DailyPrompt("Was ist das sch\u00f6nste Kompliment, das du je bekommen hast?", PromptCategory.RELATIONSHIPS, "rel_12"))
        add(DailyPrompt("Welche Grenzen m\u00f6chtest du in Beziehungen besser setzen?", PromptCategory.RELATIONSHIPS, "rel_13"))
        add(DailyPrompt("Wie hat sich deine Vorstellung von Liebe im Laufe der Zeit ver\u00e4ndert?", PromptCategory.RELATIONSHIPS, "rel_14"))
        add(DailyPrompt("Wer verdient heute einen Anruf oder eine Nachricht von dir?", PromptCategory.RELATIONSHIPS, "rel_15"))
    }

    /**
     * Returns today's writing prompt. Deterministic: same date = same prompt for all users.
     * Uses epoch day modulo to cycle through all prompts over ~64 days.
     */
    fun getTodaysPrompt(): DailyPrompt {
        val daysSinceEpoch = LocalDate.now().toEpochDay()
        val index = abs((daysSinceEpoch % prompts.size).toInt())
        return prompts[index]
    }

    /** Total number of available prompts. */
    val totalPrompts: Int get() = prompts.size
}
