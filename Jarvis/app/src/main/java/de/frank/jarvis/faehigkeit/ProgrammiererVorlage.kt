package de.frank.jarvis.faehigkeit

/**
 * Auftragseinstieg in der Eingabeblase; Frank ergänzt die Aufgabe und sendet selbst. Über den Programmierbutton wählt er vorher
 * das Modell ([VORSPANN_OPUS], [VORSPANN_SOL]), das dann für diesen Auftrag verbindlich gilt ([modell]); der Senden-Knopf eines
 * Prompts nimmt [VORSPANN] und die Modellfrage kommt nach dem Absenden.
 * Gesendet geht der Text den normalen Weg zu Jarvis, der ihn mit [hinweis] als neuen Auftrag über `programmieren` übergibt
 * (App-Erkennung, Rückfragen und Review wie bei jedem Programmierauftrag). Rein und ohne Android (ProgrammiererVorlageTest).
 */
object ProgrammiererVorlage {
    const val VORSPANN = "Übergib bitte folgende Aufgabe an den Programmierer:"
    const val VORSPANN_OPUS = "Übergib bitte folgende Aufgaben an den Programmierer mit dem Modell Opus 5.5:"
    const val VORSPANN_SOL = "Übergib bitte folgende Aufgaben an den Programmierer mit dem Modell Sol 6.1:"

    private val ALLE = listOf(VORSPANN, VORSPANN_OPUS, VORSPANN_SOL)

    /** Der Einstieg, mit dem [text] beginnt; null, wenn keiner. */
    private fun vorspannIn(text: String): String? = text.trimStart().let { t -> ALLE.firstOrNull { t.startsWith(it) } }

    /** Beginnt [text] mit einem Auftragseinstieg? */
    fun aktiv(text: String): Boolean = vorspannIn(text) != null

    /** Der Einstieg zum per Knopf gewählten [modell] (ProgrammierFaehigkeit.MODELL_OPUS oder MODELL_SOL). */
    fun vorspannFuer(modell: String): String = if (modell == ProgrammierFaehigkeit.MODELL_SOL) VORSPANN_SOL else VORSPANN_OPUS

    /** Das verbindlich gewählte Modell aus dem Einstieg von [text]; null ohne Modell im Einstieg. */
    fun modell(text: String): String? = when (vorspannIn(text)) {
        VORSPANN_OPUS -> ProgrammierFaehigkeit.MODELL_OPUS
        VORSPANN_SOL -> ProgrammierFaehigkeit.MODELL_SOL
        else -> null
    }

    /** Der Entwurf mit [vorspann] vorn und Leerzeichen dahinter; schon Getipptes bleibt als Aufgabe dahinter stehen, ein anderer Einstieg weicht. */
    fun einsetzen(entwurf: String, vorspann: String = VORSPANN): String {
        if (entwurf.trimStart().startsWith(vorspann)) return entwurf
        val rest = entfernen(entwurf)
        return if (rest.isBlank()) "$vorspann " else "$vorspann ${rest.trim()}"
    }

    /** Nimmt genau den Einstieg vorn wieder heraus; Getipptes dahinter bleibt unverändert. */
    fun entfernen(entwurf: String): String =
        vorspannIn(entwurf)?.let { entwurf.trimStart().removePrefix(it).trimStart() } ?: entwurf

    /** Die Aufgabe hinter dem Einstieg (getrimmt, leer = noch keine); null, wenn [text] nicht mit einem Einstieg beginnt. */
    fun aufgabe(text: String): String? = vorspannIn(text)?.let { text.trimStart().removePrefix(it).trim() }

    /** Steht hinter dem Einstieg nichts Inhaltliches (nur Leerzeichen oder Satzzeichen), startet kein Auftrag. */
    fun ohneAufgabe(text: String): Boolean = aufgabe(text)?.none { it.isLetterOrDigit() } == true

    /** Geht zusätzlich ans Modell, damit der Wunsch zuverlässig als Programmierauftrag übergeben und nicht im Chat beantwortet wird. */
    const val HINWEIS = "\n\n(Frank hat das über den Menüpunkt „Programmierer“ gestellt: Der Text nach dem Doppelpunkt ist ausdrücklich ein NEUER " +
        "Programmierauftrag. Übergib ihn mit programmieren, vollständig ausformuliert mit allem Wissen aus dem Gespräch; beantworte oder " +
        "programmiere ihn nicht selbst und nimm nicht programmier_nachtrag, außer Frank verlangt darin ausdrücklich einen Nachtrag. " +
        "Leite die App aus Auftrag und Gespräch ab; nennt Frank keine, lass app weg und frag nicht danach: Der Auftrag geht trotzdem sofort raus. " +
        "Nennt Frank ein Modell (auch gesprochen, etwa „Chemica 3.1 Pro“ für Kimi K3.1 Pro), Effort, Profil, Modus oder CLI, gib genau das in modell, " +
        "effort, profil, modus und cli mit; dann fragt Jarvis nicht nach Opus oder Sol.)"

    /** [HINWEIS] für [text]; steht das Modell im Einstieg, gilt es verbindlich und jede Frage danach entfällt. */
    fun hinweis(text: String): String = HINWEIS + (modell(text)?.let {
        "\n\n(Frank hat das Modell schon per Knopf gewählt, es gilt verbindlich für diesen Auftrag: modell=$it. Frag nicht nach Opus oder Sol und nimm kein anderes Modell.)"
    }.orEmpty())
}
