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

    const val KEIN_RECHNER = "Der Handy-Use-Auftrag ist nicht gestartet: Weder der Mac noch der Windows-PC noch der Hostinger-Server nehmen ihn gerade an, und die Cloud erreicht das Handy nicht."

    const val NICHT_IN_CLOUD = "Der Handy-Use-Auftrag ist nicht gestartet: Die Cloud erreicht das Handy nicht. Er läuft nur am Mac, am Windows-PC oder auf dem Hostinger-Server."

    /**
     * Soll der Auftrag das Handy bedienen? Der Name allein genügt nicht: „Ändere im Skill handy-use die Wartezeit“ ist normale
     * Programmierarbeit und darf in die Cloud.
     */
    private val BEDIENEN = Regex("(?i)\\b(nutze|nimm|benutze|verwende|mit|über|per)\\b[^.;:\\n]{0,30}\\bhandy[- ]?use\\b(?!-\\p{L})")

    fun verlangt(auftrag: String): Boolean = BEDIENEN.containsMatchIn(auftrag)
}
