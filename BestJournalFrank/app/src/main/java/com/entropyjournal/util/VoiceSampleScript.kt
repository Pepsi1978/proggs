package com.entropyjournal.util

/**
 * The sentences read aloud while a voice is being recorded for cloning.
 *
 * Alibaba takes at most 60 seconds of reference audio, so the script is cut to a word budget
 * rather than a sentence count: read calmly, [TARGET_WORDS] words take roughly 50 seconds.
 */
object VoiceSampleScript {
    const val TARGET_WORDS = 105

    val sentences: List<String> = listOf(
        "Ich schreibe auf, was mich heute bewegt hat, ruhig und ohne Eile.",
        "Manche Tage tragen sich leicht, andere brauchen ein paar Zeilen, bis sie klar werden.",
        "Wenn ich zurückblicke, erkenne ich Muster, die mir im Alltag entgehen.",
        "Was mir gut getan hat, will ich behalten, und was mich müde macht, will ich verstehen.",
        "Ich lese meine Gedanken später noch einmal, in meiner eigenen Stimme.",
        "So wird aus einem gewöhnlichen Abend eine kleine Erinnerung, die bleibt.",
    )
}
