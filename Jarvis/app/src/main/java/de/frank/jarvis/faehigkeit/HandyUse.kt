package de.frank.jarvis.faehigkeit

/**
 * Skill `handy-use`: Die CLI bedient das Handy über adb. Jarvis gibt ihr Aufgaben auf dem Handy weiter, die seine eigenen Werkzeuge
 * nicht können, vor allem Einstellungen, die Frank nicht findet (Franks Wunsch vom 02.11.2026). Die Cloud erreicht das Handy nicht,
 * darum gehen solche Aufträge nur an einen Rechner. Rein und ohne Android (HandyUseTest).
 */
object HandyUse {
    const val EINSTIEG = "Nutze den Skill handy-use:"

    const val HINWEIS = "HANDY-USE: Die CLI kann mit dem Skill handy-use Franks Handy bedienen: Bildschirm lesen, tippen, Einstellungen finden, prüfen und setzen. " +
        "Sucht Frank eine Einstellung auf dem Handy außerhalb von Jarvis (Android, Samsung, eine andere App), weiß er nicht, wo oder wie sie stehen muss, " +
        "oder soll etwas auf dem Handy nachgesehen oder geprüft werden, was deine Werkzeuge nicht können, dann übergib das mit programmieren, ohne app und ohne Rückfrage. " +
        "Der Auftrag beginnt mit „$EINSTIEG“ und nennt, was zu finden oder zu prüfen ist, welcher Zustand richtig wäre und ob die CLI ihn setzen soll. Setzen nur, wenn Frank es will; " +
        "sonst findet und meldet sie nur. Das ist die einzige Ausnahme von „keine Lösungswege“. Jarvis' eigene Einstellungen bleiben beim Werkzeug einstellungen, und Termine, Aufgaben und Mails bei deinen Werkzeugen. " +
        "Nie für Banking, PIN, Passwörter oder Codes. Handy-Use läuft nur am Mac, am Windows-PC und auf dem Hostinger-Server, nicht in der Cloud."

    const val KEIN_RECHNER = "Der Handy-Use-Auftrag ist nicht gestartet: Er braucht den Mac, den Windows-PC oder den Hostinger-Server, weil die Cloud das Handy nicht erreicht. Gerade ist keiner davon bereit."

    private val SKILL = Regex("(?i)\\bhandy[- ]?use\\b")

    /** Verlangt [auftrag] den Skill? */
    fun verlangt(auftrag: String): Boolean = SKILL.containsMatchIn(auftrag)

    /** Für einen Handy-Use-Auftrag bleiben aus [ziele] nur die, die das Handy erreichen ([istRechner]); sonst alle unverändert. */
    fun <Z> erreichbar(auftrag: String, ziele: List<Z>, istRechner: (Z) -> Boolean): List<Z> =
        if (verlangt(auftrag)) ziele.filter(istRechner) else ziele
}
