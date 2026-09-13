package de.frank.genialeideen.audio

/**
 * The questions Frank reads aloud while his voice is being recorded for cloning.
 *
 * Alibaba takes at most 60 seconds of reference audio, so the script is cut to a word budget
 * rather than a question count: read calmly, [TARGET_WORDS] words take roughly 50 seconds.
 */
object VoiceSampleScript {
    const val TARGET_WORDS = 105
    private const val MIN_QUESTIONS = 3

    /** Used when no questions could be generated, so a recording is always possible. */
    val fallback: List<String> = listOf(
        "Warum fühlt sich mein Leben gerade so leicht und klar an?",
        "Woran merke ich schon beim Aufwachen, dass dieser Tag gut wird?",
        "Wie fühlt sich meine Zuversicht an, während sie mich durch den Tag trägt?",
        "Was genau macht diesen ganz gewöhnlichen Tag für mich so kostbar?",
        "Wofür bin ich heute still dankbar, ganz für mich allein?",
        "Wieso fällt mir das Lächeln in letzter Zeit so mühelos?",
        "Welche Wärme spüre ich in den Schultern, sobald ich zur Ruhe komme?",
        "Was daran ist schön, dass ich mir selbst so tief vertraue?",
        "Und während der Abend leise wird — welcher Augenblick von heute leuchtet dann noch einmal in mir auf?",
    )

    /** Cuts [questions] down to about [TARGET_WORDS] words of reading material. */
    fun script(questions: List<String>): List<String> {
        val usable = questions.map(String::trim).filter(String::isNotBlank)
        if (usable.isEmpty()) return fallback

        val chosen = mutableListOf<String>()
        var words = 0
        for (question in usable) {
            if (words >= TARGET_WORDS && chosen.size >= MIN_QUESTIONS) break
            chosen += question
            words += wordCount(question)
        }
        return chosen
    }

    private fun wordCount(text: String): Int = text.split(' ', '\n', '\t').count(String::isNotBlank)
}
