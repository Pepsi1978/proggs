package de.frank.perfectmoment.auth

internal data class ResolvedSessionPrompt(
    val topic: String,
    val introContext: String,
)

internal object IntroQuestionPolicy {
    const val QUESTION = "Welche Frage hast du und welches Gefühl möchtest du damit verstärken?"
    private val normalizedQuestion = QuestionResponseValidator.normalizeQuestion(QUESTION)

    fun isEntranceQuestion(value: String): Boolean =
        QuestionResponseValidator.normalizeQuestion(value) == normalizedQuestion

    fun requiresAnswer(topic: String): Boolean {
        val normalizedTopic = QuestionResponseValidator.normalizeQuestion(topic)
        val questionIndex = normalizedTopic.indexOf(normalizedQuestion)
        if (questionIndex < 0) return false
        val instruction = normalizedTopic.take(questionIndex)
        return instruction.contains("frage mich") && instruction.contains("zuerst")
    }

    fun resolve(topic: String, introContext: String): ResolvedSessionPrompt {
        val cleanTopic = topic.trim()
        val cleanContext = introContext.trim()
        val isEntrancePrompt = requiresAnswer(cleanTopic)
        return if (isEntrancePrompt && cleanContext.isNotEmpty()) {
            ResolvedSessionPrompt(topic = cleanContext, introContext = "")
        } else {
            ResolvedSessionPrompt(topic = cleanTopic, introContext = cleanContext)
        }
    }
}
