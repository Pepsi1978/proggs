package de.frank.perfectmoment.ui

internal fun appendDictation(existing: String, transcript: String): String {
    val spokenText = transcript.trim()
    if (spokenText.isEmpty()) return existing

    val currentText = existing.trimEnd()
    return buildString {
        if (currentText.isNotEmpty()) {
            append(currentText)
            append(' ')
        }
        append(spokenText)
        append(' ')
    }
}
