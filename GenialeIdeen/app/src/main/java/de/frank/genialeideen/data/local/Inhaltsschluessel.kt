package de.frank.genialeideen.data.local

/**
 * Titel und Text ohne Unterschiede in Groß-/Kleinschreibung und Leerraum.
 *
 * Zwei Ideen gelten damit als dieselbe, auch wenn eine davon aus einer Sicherung stammt und
 * unterwegs anders eingerückt wurde. Genutzt vom Einspielen (was fehlt wirklich?) und vom
 * Zusammenlegen doppelter Ideen.
 */
fun IdeeEntity.inhaltsSchluessel(): String {
    fun glatt(text: String) = text.trim().replace(Regex("""\s+"""), " ").lowercase()
    // Zeilenumbruch als Trenner: glatt() lässt keinen übrig, Titel und Text vermischen sich nie.
    return glatt(titel) + "\n" + glatt(text)
}
