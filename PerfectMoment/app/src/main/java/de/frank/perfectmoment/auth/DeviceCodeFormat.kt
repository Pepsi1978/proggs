package de.frank.perfectmoment.auth

/** Placeholder shown while the server has not delivered a code yet. */
private const val PLACEHOLDER_HEAD = "––––"
private const val PLACEHOLDER_TAIL = "–––––"

/**
 * Splits a device code into the groups shown on the login screen.
 *
 * The server owns the length: OpenAI currently returns nine characters, which the
 * browser page asks for as four plus five. The app must therefore never assume a
 * length of its own - every character the server sent has to reach the screen, or
 * the user types an incomplete code and the login fails.
 *
 * Rules, in order:
 *  - a separator sent by the server (dash, en dash, space) decides the grouping,
 *  - otherwise the code is split in the middle, the shorter half first (9 -> 4 + 5),
 *  - a code of four characters or less stays in one piece,
 *  - nothing is ever truncated, whatever the length.
 */
fun deviceCodeGroups(code: String): List<String> {
    val cleaned = code.trim()
    if (cleaned.isEmpty()) return listOf(PLACEHOLDER_HEAD, PLACEHOLDER_TAIL)

    val separated = cleaned.split('-', '–', '—', ' ', '_').filter { it.isNotBlank() }
    if (separated.size > 1) return separated

    val characters = cleaned.filter(Char::isLetterOrDigit)
    if (characters.isEmpty()) return listOf(cleaned)
    if (characters.length <= 4) return listOf(characters)

    val head = characters.length / 2
    return listOf(characters.take(head), characters.drop(head))
}
