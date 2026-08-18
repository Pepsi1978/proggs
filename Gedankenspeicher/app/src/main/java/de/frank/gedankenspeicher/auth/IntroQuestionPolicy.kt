package de.frank.gedankenspeicher.auth

internal data class ResolvedSessionPrompt(
    val topic: String,
    val introContext: String,
)

internal object IntroQuestionPolicy {
    const val QUESTION = "Welche Frage hast du und welches Gefühl möchtest du damit verstärken?"
    private val normalizedQuestion = QuestionResponseValidator.normalizeQuestion(QUESTION)

    fun isEntranceQuestion(value: String): Boolean =
        QuestionResponseValidator.normalizeQuestion(value) == normalizedQuestion

    fun resolve(topic: String, introContext: String, answerRequired: Boolean): ResolvedSessionPrompt {
        val cleanTopic = topic.trim()
        val cleanContext = introContext.trim()
        return if (answerRequired && cleanContext.isNotEmpty()) {
            ResolvedSessionPrompt(topic = cleanContext, introContext = "")
        } else {
            ResolvedSessionPrompt(topic = cleanTopic, introContext = cleanContext)
        }
    }
}
