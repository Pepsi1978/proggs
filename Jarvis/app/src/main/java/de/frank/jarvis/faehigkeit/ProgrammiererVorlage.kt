package de.frank.jarvis.faehigkeit

/**
 * Plus-Menü „Programmierer“: setzt den festen Vorspann in die Eingabeblase; Frank ergänzt die Aufgabe und sendet selbst.
 * Gesendet geht der Text den normalen Weg zu Jarvis, der ihn mit [hinweis] als neuen Auftrag über `programmieren` übergibt
 * (App-Erkennung, Rückfragen und Review wie bei jedem Programmierauftrag). Rein und ohne Android (ProgrammiererVorlageTest).
 */
object ProgrammiererVorlage {
    const val VORSPANN = "Übergib bitte folgende Aufgabe an den Programmierer:"

    /** Der Entwurf mit Vorspann vorn und Leerzeichen dahinter; schon Getipptes bleibt als Aufgabe dahinter stehen. */
    fun einsetzen(entwurf: String): String = when {
        entwurf.trimStart().startsWith(VORSPANN) -> entwurf
        entwurf.isBlank() -> "$VORSPANN "
        else -> "$VORSPANN ${entwurf.trim()}"
    }

    /** Die Aufgabe hinter dem Vorspann (getrimmt, leer = noch keine); null, wenn [text] nicht mit dem Vorspann beginnt. */
    fun aufgabe(text: String): String? = text.trimStart().takeIf { it.startsWith(VORSPANN) }?.removePrefix(VORSPANN)?.trim()

    /** Steht hinter dem Vorspann nichts Inhaltliches (nur Leerzeichen oder Satzzeichen), startet kein Auftrag. */
    fun ohneAufgabe(text: String): Boolean = aufgabe(text)?.none { it.isLetterOrDigit() } == true

    /** Geht zusätzlich ans Modell, damit der Wunsch zuverlässig als Programmierauftrag übergeben und nicht im Chat beantwortet wird. */
    const val HINWEIS = "\n\n(Frank hat das über den Menüpunkt „Programmierer“ gestellt: Der Text nach dem Doppelpunkt ist ausdrücklich ein NEUER " +
        "Programmierauftrag. Übergib ihn mit programmieren, vollständig ausformuliert mit allem Wissen aus dem Gespräch; beantworte oder " +
        "programmiere ihn nicht selbst und nimm nicht programmier_nachtrag, außer Frank verlangt darin ausdrücklich einen Nachtrag. " +
        "Leite die App aus Auftrag und Gespräch ab; nennt Frank keine, lass app weg und frag nicht danach: Der Auftrag geht trotzdem sofort raus.)"
}
