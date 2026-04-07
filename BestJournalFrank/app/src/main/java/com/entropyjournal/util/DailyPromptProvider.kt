package com.entropyjournal.util

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

object DailyPromptProvider {

    private val prompts: List<DailyPrompt> = buildList {
        add(DailyPrompt("Was hat dich heute zum Lächeln gebracht?", PromptCategory.SELF_REFLECTION, "self_0"))
        add(DailyPrompt("Welchen Gedanken konntest du heute nicht loslassen?", PromptCategory.SELF_REFLECTION, "self_1"))
        add(DailyPrompt("Was hast du heute über dich selbst gelernt?", PromptCategory.SELF_REFLECTION, "self_2"))
        add(DailyPrompt("Welcher Moment war heute am intensivsten?", PromptCategory.SELF_REFLECTION, "self_3"))
        add(DailyPrompt("Was würdest du heute anders machen, wenn du könntest?", PromptCategory.SELF_REFLECTION, "self_4"))
        add(DailyPrompt("Welches Gefühl hat dich heute am meisten begleitet?", PromptCategory.SELF_REFLECTION, "self_5"))
        add(DailyPrompt("Was hat dich heute überrascht?", PromptCategory.SELF_REFLECTION, "self_6"))
        add(DailyPrompt("Worüber hast du heute am längsten nachgedacht?", PromptCategory.SELF_REFLECTION, "self_7"))
        add(DailyPrompt("Was hat dir heute Energie gegeben?", PromptCategory.SELF_REFLECTION, "self_8"))
        add(DailyPrompt("Was hat dir heute Energie geraubt?", PromptCategory.SELF_REFLECTION, "self_9"))
        add(DailyPrompt("Welche Gewohnheit möchtest du verändern?", PromptCategory.SELF_REFLECTION, "self_10"))
        add(DailyPrompt("Was bedeutet Erfolg für dich gerade?", PromptCategory.SELF_REFLECTION, "self_11"))
        add(DailyPrompt("Welche Angst steht dir gerade im Weg?", PromptCategory.SELF_REFLECTION, "self_12"))
        add(DailyPrompt("Was würde dein jüngeres Ich zu deinem heutigen Leben sagen?", PromptCategory.SELF_REFLECTION, "self_13"))
        add(DailyPrompt("Welchen Rat gibst du dir selbst gerade?", PromptCategory.SELF_REFLECTION, "self_14"))
        add(DailyPrompt("Was macht dich einzigartig?", PromptCategory.SELF_REFLECTION, "self_15"))

        add(DailyPrompt("Wofür bist du heute dankbar?", PromptCategory.GRATITUDE, "grat_0"))
        add(DailyPrompt("Welche Person hat heute deinen Tag bereichert?", PromptCategory.GRATITUDE, "grat_1"))
        add(DailyPrompt("Welches kleine Detail hat dich heute erfreut?", PromptCategory.GRATITUDE, "grat_2"))
        add(DailyPrompt("Welche Fähigkeit besitzt du, die du oft für selbstverständlich hältst?", PromptCategory.GRATITUDE, "grat_3"))
        add(DailyPrompt("Was ist das Beste, das dir diese Woche passiert ist?", PromptCategory.GRATITUDE, "grat_4"))
        add(DailyPrompt("Wem würdest du gerne danken, und wofür?", PromptCategory.GRATITUDE, "grat_5"))
        add(DailyPrompt("Welcher Ort gibt dir ein Gefühl von Zuhause?", PromptCategory.GRATITUDE, "grat_6"))
        add(DailyPrompt("Welche Erinnerung macht dich sofort glücklich?", PromptCategory.GRATITUDE, "grat_7"))
        add(DailyPrompt("Was an deinem Körper verdient mehr Wertschätzung?", PromptCategory.GRATITUDE, "grat_8"))
        add(DailyPrompt("Welches Essen hat dir heute richtig gut getan?", PromptCategory.GRATITUDE, "grat_9"))
        add(DailyPrompt("Welche Musik hat dich zuletzt berührt?", PromptCategory.GRATITUDE, "grat_10"))
        add(DailyPrompt("Was ist das Wertvollste, das du besitzt, und kein Geld kosten würde?", PromptCategory.GRATITUDE, "grat_11"))
        add(DailyPrompt("Welche Erfahrung aus der Vergangenheit hat dich stärker gemacht?", PromptCategory.GRATITUDE, "grat_12"))
        add(DailyPrompt("Was an deinem Alltag ist eigentlich ein Privileg?", PromptCategory.GRATITUDE, "grat_13"))
        add(DailyPrompt("Wer ist dein heimlicher Held im Alltag?", PromptCategory.GRATITUDE, "grat_14"))
        add(DailyPrompt("Welche Jahreszeit genießt du gerade am meisten, und warum?", PromptCategory.GRATITUDE, "grat_15"))

        add(DailyPrompt("Was möchtest du in einem Jahr erreicht haben?", PromptCategory.FUTURE, "future_0"))
        add(DailyPrompt("Wie sieht dein perfekter Tag in fünf Jahren aus?", PromptCategory.FUTURE, "future_1"))
        add(DailyPrompt("Welches Ziel verfolgst du gerade am leidenschaftlichsten?", PromptCategory.FUTURE, "future_2"))
        add(DailyPrompt("Was würdest du tun, wenn du wüsstest, dass du nicht scheitern kannst?", PromptCategory.FUTURE, "future_3"))
        add(DailyPrompt("Welche neue Fähigkeit möchtest du lernen?", PromptCategory.FUTURE, "future_4"))
        add(DailyPrompt("Wohin möchtest du als Nächstes reisen?", PromptCategory.FUTURE, "future_5"))
        add(DailyPrompt("Was für ein Mensch möchtest du in zehn Jahren sein?", PromptCategory.FUTURE, "future_6"))
        add(DailyPrompt("Welchen Traum hast du aufgegeben, den du wiederbeleben könntest?", PromptCategory.FUTURE, "future_7"))
        add(DailyPrompt("Was ist der mutigste Schritt, den du gerade machen könntest?", PromptCategory.FUTURE, "future_8"))
        add(DailyPrompt("Welches Projekt begeistert dich, das du noch nicht begonnen hast?", PromptCategory.FUTURE, "future_9"))
        add(DailyPrompt("Was müsste passieren, damit du sagst: Jetzt bin ich zufrieden?", PromptCategory.FUTURE, "future_10"))
        add(DailyPrompt("Welche Veränderung würdest du in der Welt gerne sehen?", PromptCategory.FUTURE, "future_11"))
        add(DailyPrompt("Was steht auf deiner Bucket List ganz oben?", PromptCategory.FUTURE, "future_12"))
        add(DailyPrompt("Welche schlechte Gewohnheit möchtest du bis nächsten Monat ablegen?", PromptCategory.FUTURE, "future_13"))
        add(DailyPrompt("Wie möchtest du in Erinnerung behalten werden?", PromptCategory.FUTURE, "future_14"))
        add(DailyPrompt("Was ist ein kleiner Schritt, den du morgen machen kannst?", PromptCategory.FUTURE, "future_15"))

        add(DailyPrompt("Wem würdest du gerne etwas sagen, was du bisher nicht gesagt hast?", PromptCategory.RELATIONSHIPS, "rel_0"))
        add(DailyPrompt("Welche Beziehung in deinem Leben verdient mehr Aufmerksamkeit?", PromptCategory.RELATIONSHIPS, "rel_1"))
        add(DailyPrompt("Was schätzt du an deinem besten Freund am meisten?", PromptCategory.RELATIONSHIPS, "rel_2"))
        add(DailyPrompt("Wann hast du dich zuletzt wirklich verstanden gefühlt?", PromptCategory.RELATIONSHIPS, "rel_3"))
        add(DailyPrompt("Was würdest du einer Person sagen, die dir wichtig ist?", PromptCategory.RELATIONSHIPS, "rel_4"))
        add(DailyPrompt("Welcher Mensch hat dein Leben am stärksten geprägt?", PromptCategory.RELATIONSHIPS, "rel_5"))
        add(DailyPrompt("Wie zeigst du anderen, dass du sie liebst?", PromptCategory.RELATIONSHIPS, "rel_6"))
        add(DailyPrompt("Welchen Konflikt würdest du gerne lösen?", PromptCategory.RELATIONSHIPS, "rel_7"))
        add(DailyPrompt("Was bedeutet Freundschaft für dich?", PromptCategory.RELATIONSHIPS, "rel_8"))
        add(DailyPrompt("Wer hat dir zuletzt etwas Wichtiges beigebracht?", PromptCategory.RELATIONSHIPS, "rel_9"))
        add(DailyPrompt("Welche Eigenschaft bewunderst du an anderen am meisten?", PromptCategory.RELATIONSHIPS, "rel_10"))
        add(DailyPrompt("Mit wem würdest du gerne einen ganzen Tag verbringen?", PromptCategory.RELATIONSHIPS, "rel_11"))
        add(DailyPrompt("Was ist das schönste Kompliment, das du je bekommen hast?", PromptCategory.RELATIONSHIPS, "rel_12"))
        add(DailyPrompt("Welche Grenzen möchtest du in Beziehungen besser setzen?", PromptCategory.RELATIONSHIPS, "rel_13"))
        add(DailyPrompt("Wie hat sich deine Vorstellung von Liebe im Laufe der Zeit verändert?", PromptCategory.RELATIONSHIPS, "rel_14"))
        add(DailyPrompt("Wer verdient heute einen Anruf oder eine Nachricht von dir?", PromptCategory.RELATIONSHIPS, "rel_15"))
    }

    fun getTodaysPrompt(): DailyPrompt {
        val daysSinceEpoch = LocalDate.now().toEpochDay()
        val index = abs((daysSinceEpoch % prompts.size).toInt())
        return prompts[index]
    }

    val totalPrompts: Int get() = prompts.size
}
