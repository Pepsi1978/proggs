package de.frank.jarvis.ui

import java.util.Locale

/**
 * Erkennt, ob ein gesprochener Satz die Sprachsession beenden soll. Läuft vor allem anderen in der App, ohne Modell:
 * Was hier sicher erkannt wird, beendet sofort. Was frei formuliert und nicht eindeutig ist, geht ans Modell, das dafür
 * das Werkzeug `sprachsession_beenden` hat.
 */
object SprachBefehle {

    /** „Ende“, „Stopp“, „Danke, das war's“ …: beendet den Gesprächsmodus. Nur wenn der ganze Satz ein solcher Befehl ist. */
    fun istAusbefehl(text: String): Boolean = satz(text) in AUSBEFEHLE

    /** Kleingeschrieben, ohne Satzzeichen und Füllwörter, als ein Satz (für [AUSBEFEHLE] und [HOEFLICH]). */
    private fun satz(text: String): String = text.lowercase(Locale.GERMAN).replace("’", "'").replace(Regex("[^a-zäöüß' ]"), " ")
        .split(' ').filter { it.isNotBlank() && it !in FUELLWORTE }.joinToString(" ").replace("'", "")

    /**
     * Ein höflicher Abschluss („Danke“, „Das war's“, „Tschüss“): beendet die Sprachschleife, ist aber kein Verzicht auf ein noch
     * ausstehendes Ergebnis. Frank wartet danach oft genau darauf; nur ein ausdrückliches Beenden verwirft die Sprachzustellung.
     */
    fun istHoeflichesEnde(text: String): Boolean = satz(text) in HOEFLICH

    /**
     * Soll der Satz die Sprachsession beenden? [imGespraechsmodus] = die über das Weckwort gestartete Sprachschleife
     * läuft: Dann genügt auch ein bloßes „Beenden“ oder „Session beenden“. Sonst muss ausdrücklich die Sprachsession,
     * der Sprachmodus oder das Mikrofon genannt sein.
     */
    fun istSessionEnde(text: String, imGespraechsmodus: Boolean): Boolean {
        if (imGespraechsmodus && istAusbefehl(text)) return true
        val worte = normiert(text)
        if (worte.isEmpty() || worte.size > 8) return false
        // Fragen und Verneinungen beenden nie: „Wie beende ich die Sprachsession?“, „Beende die Sprachsession nicht“.
        if (worte.any { it in SPERRWORTE }) return false
        val rest = worte.filter { it !in FUELLWORTE && it !in ARTIKEL && it !in HELFER }
        if (rest.isEmpty() || rest.any { it !in ENDE_VERBEN && it !in NOMEN_IMMER && it !in NOMEN_IM_MODUS }) return false
        if (rest.none { it in ENDE_VERBEN }) return false
        if (rest.any { it in NOMEN_IMMER }) return true
        if (!imGespraechsmodus) return false
        // In der Sprachschleife: „Session beenden“ oder ein bloßes „Bitte beenden jetzt“, aber nie ein einzelnes „zu“ oder „ab“.
        return rest.any { it in NOMEN_IM_MODUS } || rest.any { it in KERN_VERBEN } || ("hör" in rest && "auf" in rest)
    }

    /**
     * Soll der Satz das laufende Live-Gespräch beenden („Bitte das Gespräch beenden“, „Beende das Live-Gespräch“, „Leg auf“)?
     * Strenger als [istSessionEnde]: Ein bloßes „Stopp“ unterbricht im Live-Gespräch nur die Antwort, und „Danke“ ist dort
     * meist Höflichkeit. Ohne Nomen zählen nur „beenden“ und „auflegen“.
     */
    fun istLiveEnde(text: String): Boolean {
        val worte = normiert(text)
        if (worte.isEmpty() || worte.size > 8) return false
        if (worte.any { it in SPERRWORTE }) return false
        val rest = worte.filter { it !in FUELLWORTE && it !in ARTIKEL && it !in HELFER && it !in LIVE_HELFER }
        val nomen = NOMEN_IMMER + NOMEN_IM_MODUS + LIVE_NOMEN
        if (rest.isEmpty() || rest.any { it !in ENDE_VERBEN && it !in nomen && it != "leg" && it != "auflegen" }) return false
        if (rest.any { it in nomen }) return rest.any { it in ENDE_VERBEN }
        return rest.any { it in setOf("beenden", "beende", "beendet", "beend", "auflegen") } ||
            ("leg" in rest && "auf" in rest) || ("hör" in rest && "auf" in rest && "zuzuhören" in rest)
    }

    /** Kleinschreibung, ohne Satzzeichen; „Sprach-Session“, „Sprach Session“ und „Voice Session“ werden zu „sprachsession“. */
    private fun normiert(text: String): List<String> {
        var s = " " + text.lowercase(Locale.GERMAN).replace(Regex("[^a-zäöüß ]"), " ").replace(Regex(" +"), " ").trim() + " "
        for ((alt, neu) in ZUSAMMEN) s = s.replace(" $alt ", " $neu ")
        return s.split(' ').filter { it.isNotBlank() }
    }

    /** Wörter, die vor oder nach dem Befehl nicht zählen („Okay Jarvis, Ende bitte“). */
    private val FUELLWORTE = setOf("jarvis", "okay", "ok", "bitte", "hey", "also", "gut", "dann", "ja", "nein", "nee", "so")

    /** Wie [FUELLWORTE], nur für die freiere Erkennung der Sprachsession („bitte beenden jetzt“). */
    private val HELFER = setOf(
        "jetzt", "mal", "doch", "einfach", "endlich", "sofort", "gleich", "nun", "erstmal", "danke", "vielen", "dank", "gerne", "gern",
        "kannst", "könntest", "würdest", "du", "wir", "können", "kann", "ich", "will", "möchte", "möchtest", "sollst", "lass", "lasst",
        "uns", "mach", "mache", "machen", "schalt", "schalte", "hiermit", "wieder", "komplett", "ganz", "für", "heute", "hier",
    )

    /** Artikel und Begleiter, die beim „Beende die Sprachsession“ nicht zählen. */
    private val ARTIKEL = setOf(
        "die", "den", "das", "der", "dem", "diese", "diesen", "dieses", "unsere", "meine", "deine", "dein", "mein",
        "aktuelle", "aktive", "laufende", "aktuellen", "aktiven", "laufenden",
    )

    private val ENDE_VERBEN = setOf(
        "beenden", "beende", "beendet", "beend", "stoppen", "stoppe", "stopp", "stop", "aus", "ausschalten", "ausmachen", "abschalten",
        "abstellen", "schließen", "schließe", "schließ", "zu", "zumachen", "ende", "schluss", "aufhören", "hör", "höre", "auf",
        "abbrechen", "brich", "ab", "zuzuhören",
    )

    /** Ohne Nomen zählen nur diese eindeutigen Verben. */
    private val KERN_VERBEN = setOf("beenden", "beende", "beendet", "stoppen", "stoppe", "stopp", "stop", "aufhören", "abbrechen", "schluss", "ende")

    /** Diese Nomen meinen immer die Sprachsession. */
    private val NOMEN_IMMER = setOf(
        "sprachsession", "sprachsitzung", "sprachmodus", "sprachschleife", "gesprächsmodus", "sprachdialog", "sprachsteuerung",
        "mikrofon", "mikro",
    )

    /** Allgemeine Nomen: nur, wenn gerade die Sprachschleife läuft („Session beenden“). */
    private val NOMEN_IM_MODUS = setOf("session", "sitzung", "gespräch", "unterhaltung")

    /** Nur fürs Live-Gespräch: was dort das laufende Gespräch meint. */
    private val LIVE_NOMEN = setOf("livegespräch", "livesession", "livemodus", "liveverbindung", "live", "verbindung", "telefonat", "anruf", "zuhören")

    /** Wie [HELFER], für Aufträge, die das Live-Modell umformuliert weitergibt („Frank möchte das Gespräch beenden“). */
    private val LIVE_HELFER = setOf("frank", "er", "bittet", "wünscht", "möchten", "soll", "sollen", "werden", "wird", "bitten")

    private val SPERRWORTE = setOf(
        "nicht", "kein", "keine", "nie", "wie", "warum", "wieso", "weshalb", "was", "wann", "wo", "ob", "erklär", "erkläre",
        "programmier", "programmiersession", "programmierauftrag", "auftrag", "app",
    )

    private val ZUSAMMEN = listOf(
        "sprach session" to "sprachsession", "voice session" to "sprachsession", "sprach sitzung" to "sprachsitzung",
        "sprach modus" to "sprachmodus", "gesprächs modus" to "gesprächsmodus", "sprach schleife" to "sprachschleife",
        "live gespräch" to "livegespräch", "live session" to "livesession", "live modus" to "livemodus", "live verbindung" to "liveverbindung",
        "live sitzung" to "livesession",
    )

    /** Die höflichen unter den [AUSBEFEHLE]n: Sie schließen die Schleife, verzichten aber nicht auf ausstehende Ergebnisse. */
    private val HOEFLICH = setOf(
        "danke", "danke schön", "dankeschön", "vielen dank", "tschüss", "tschüs", "ciao", "fertig", "das wars", "danke das wars", "das wars danke",
        "das ist alles", "danke das ist alles", "das reicht", "mehr nicht", "nichts mehr", "nichts weiter", "sonst nichts", "danke sonst nichts",
    )

    /** Ganze Sätze, die den Gesprächsmodus beenden (nach Kleinschreibung, ohne Satzzeichen und Füllwörter). */
    private val AUSBEFEHLE = HOEFLICH + setOf(
        "ende", "stopp", "stop", "schluss", "aus",
        "sprachsitzung beenden", "sprachsitzung beendet", "sitzung beenden", "gespräch beenden", "beenden", "beende", "aufhören", "hör auf",
        "ende der sitzung", "session beenden", "schluss für jetzt", "erstmal nicht", "erst mal nicht",
    )
}
