package de.frank.jarvis.ui

import de.frank.jarvis.data.Sitzung

/** Ein Navigationsbefehl zwischen Gesprächen („Sessions“): Er bleibt in der App und geht nie ans Modell. */
sealed class NaviBefehl {
    /** „Öffne eine neue Session“ */
    object Neu : NaviBefehl()
    /** „Gehe in die letzte Session zurück“ */
    object Zurueck : NaviBefehl()
    /** „Gehe in die Whoop-Session zurück, wo wir programmiert haben“: Stichwörter und ob es ums Programmieren ging. */
    class Thema(val stichworte: List<String>, val programmiert: Boolean) : NaviBefehl()
}

/** Franks Antwort auf die Rückfrage „Welche Session meinst du?“ */
sealed class NaviWahl {
    class Treffer(val sitzung: Sitzung) : NaviWahl()
    object Abbruch : NaviWahl()
    /** Keine Antwort auf die Rückfrage: Der Satz ist etwas anderes und läuft normal weiter. */
    object Keine : NaviWahl()
}

/**
 * Erkennt Navigationsbefehle zwischen Gesprächen und findet das gemeinte Gespräch. Rein in der App und ohne Modell:
 * So kann ein Navigationsbefehl nie einen Programmierauftrag oder ein anderes Werkzeug auslösen.
 */
object GespraechsNavigation {

    fun worte(text: String): List<String> =
        text.lowercase(java.util.Locale.GERMAN).replace(Regex("[^a-zäöüß0-9]+"), " ").split(' ').filter { it.isNotBlank() }

    fun erkenne(text: String): NaviBefehl? {
        val alle = worte(text)
        if (alle.isEmpty() || alle.size > 24) return null
        val kern = alle.filter { it !in FUELL }
        val nomen = kern.indices.filter { kern[it] in NOMEN }
        if (nomen.isEmpty()) return null
        // Ein Satz, der etwas bauen oder ändern lassen will, ist ein Auftrag und keine Navigation.
        if (kern.any { w -> AUFTRAG.any { w.startsWith(it) } }) return null
        // Fragen („Was war das letzte Gespräch über Whoop?“) beantwortet Jarvis selbst.
        if (alle.first() in FRAGE) return null
        val hatVerb = kern.any { it in VERBEN || it == "zurück" }
        // „neue Session“, „ein neues Gespräch“: „neu“ direkt vor dem Nomen.
        if (nomen.any { i -> (1..2).any { d -> kern.getOrNull(i - d) in NEU } } && (hatVerb || kern.size <= 3)) return NaviBefehl.Neu
        if (!hatVerb) return null
        val stichworte = kern.filter { it !in STOPP && it !in VERBEN && it !in NOMEN && it !in NEU && it !in ZURUECK && it.length >= 3 && !it.startsWith("programm") }
        val programmiert = kern.any { it.startsWith("programm") || it == "gebaut" || it == "gecodet" }
        return when {
            stichworte.isNotEmpty() -> NaviBefehl.Thema(stichworte.distinct(), programmiert)
            kern.any { it in ZURUECK || it == "zurück" } -> NaviBefehl.Zurueck
            else -> null
        }
    }

    /** Stimmt ein Wort mit einem Stichwort überein? Großzügig bei Endungen: „Kalender“ findet „Kalendern“, „Whoops“. */
    private fun passt(wort: String, stichwort: String): Boolean {
        val stamm = if (stichwort.length <= 4) stichwort else stichwort.take(maxOf(4, stichwort.length - 2))
        return wort.startsWith(stamm)
    }

    private fun treffer(woerter: Collection<String>, stichwort: String) = woerter.count { passt(it, stichwort) }

    /**
     * Punkte eines Gesprächs für die Stichwörter: Titel zählt am meisten, dann die Programmieraufträge des Gesprächs, dann der
     * Inhalt. 0 = kein Stichwort kommt vor. Ging es ausdrücklich ums Programmieren, zählen Gespräche ohne Auftrag nur halb.
     */
    fun punkte(befehl: NaviBefehl.Thema, titel: String, inhalt: String, auftraege: String): Double {
        val t = worte(titel)
        val a = worte(auftraege)
        val i = worte(inhalt)
        var summe = 0.0
        for (s in befehl.stichworte) {
            if (treffer(t, s) > 0) summe += 6.0
            if (treffer(a, s) > 0) summe += 4.0
            val n = treffer(i, s)
            if (n > 0) summe += 1.0 + minOf(n, 10) * 0.2
        }
        if (summe == 0.0) return 0.0
        if (befehl.programmiert) {
            val programmiert = auftraege.isNotBlank() || i.any { it.startsWith("programmier") }
            summe = if (programmiert) summe + 4.0 else summe * 0.5
        }
        return summe
    }

    /** Kandidaten nach Punkten: einer klar vorn = Treffer, sonst alle, die nahe am Besten liegen (höchstens drei). */
    fun auswahl(bewertet: List<Pair<Sitzung, Double>>): List<Sitzung> {
        val sortiert = bewertet.filter { it.second > 0.0 }.sortedByDescending { it.second }
        val beste = sortiert.firstOrNull()?.second ?: return emptyList()
        return sortiert.filter { it.second >= beste * 0.7 }.take(3).map { it.first }
    }

    /** Wertet Franks Antwort auf die Rückfrage aus: Nummer, Ordinalzahl oder ein Wort aus dem Titel. */
    fun waehle(text: String, kandidaten: List<Sitzung>): NaviWahl {
        val w = worte(text).filter { it !in FUELL }
        if (w.isEmpty()) return NaviWahl.Keine
        if (w.size <= 4 && w.any { it in ABBRUCH }) return NaviWahl.Abbruch
        if (w.size <= 5) {
            val nummer = w.firstNotNullOfOrNull { ORDNUNG[it] }
            if (nummer != null && nummer < kandidaten.size) return NaviWahl.Treffer(kandidaten[nummer])
            if (w.any { it == "letzte" || it == "letzten" } && w.size <= 3) return NaviWahl.Treffer(kandidaten.last())
        }
        if (w.size > 10) return NaviWahl.Keine
        val stichworte = w.filter { it !in STOPP && it !in NOMEN && it !in VERBEN && it.length >= 3 }
        if (stichworte.isEmpty()) return NaviWahl.Keine
        val bewertet = kandidaten.map { k -> k to stichworte.count { s -> treffer(worte(k.titel), s) > 0 } }.filter { it.second > 0 }
        val beste = bewertet.maxOfOrNull { it.second } ?: return NaviWahl.Keine
        val vorn = bewertet.filter { it.second == beste }
        return if (vorn.size == 1) NaviWahl.Treffer(vorn.first().first) else NaviWahl.Keine
    }

    private val NOMEN = setOf(
        "session", "sessions", "sitzung", "sitzungen", "gespräch", "gespräche", "gesprächs", "chat", "chats", "unterhaltung",
        "konversation", "unterhaltungen", "sesion", "sesschen",
    )
    private val NEU = setOf("neue", "neues", "neuen", "neuer", "neu", "frische", "frisches", "leere", "leeres")
    private val ZURUECK = setOf("letzte", "letzten", "vorherige", "vorherigen", "vorige", "vorigen", "zurück", "davor", "eben")
    private val VERBEN = setOf(
        "öffne", "öffnen", "öffnest", "mach", "mache", "machen", "starte", "starten", "startest", "beginne", "beginnen", "leg", "lege", "anlegen",
        "geh", "gehe", "gehen", "wechsel", "wechsle", "wechseln", "wechsele", "spring", "springe", "springen", "bring", "bringe",
        "zeig", "zeige", "zeigen", "hol", "hole", "holen", "nimm", "zurückgehen", "zurückwechseln", "zurückspringen", "lade", "laden", "auf",
    )
    private val FUELL = setOf(
        "jarvis", "hey", "okay", "ok", "bitte", "also", "und", "dann", "jetzt", "mal", "kannst", "könntest", "würdest", "du", "lass", "lasst",
        "uns", "wir", "ich", "mich", "mir", "möchte", "will", "würde", "gern", "gerne", "eine", "ein", "einen", "einem", "einer", "die", "der",
        "das", "den", "dem", "des", "in", "ins", "zur", "zum", "zu", "noch", "mal", "doch",
    )
    private val STOPP = FUELL + setOf(
        "wo", "mit", "haben", "hatten", "hat", "habe", "hast", "über", "von", "vom", "im", "an", "am", "auf", "um", "für", "gearbeitet",
        "gesprochen", "geredet", "worden", "wurde", "es", "ging", "geht", "wieder", "nochmal", "bei", "gemacht", "diskutiert", "besprochen",
        "thema", "nach", "dort", "da", "als", "letztes", "gestern", "heute", "vorhin", "gebaut", "gecodet", "angefangen", "sind", "waren",
        "war", "ist", "da", "drin", "darin", "dazu", "worum", "welche", "welcher", "welches", "diese", "dieser", "dieses", "zurück", "hin",
        "app", "sowie", "oder", "aus", "alte", "alten", "andere", "anderen",
    )
    private val AUFTRAG = listOf(
        "programmierauftrag", "auftrag", "funktion", "feature", "ergänz", "implementier", "baue", "einbau", "programmiere", "ändere",
        "änderung", "fixe", "behebe", "erstelle",
    )
    private val FRAGE = setOf("was", "wie", "wann", "welche", "welches", "welcher", "warum", "wieso", "weshalb", "hast", "gibt", "gab", "weißt", "wer", "wieviele")
    private val ABBRUCH = setOf("abbrechen", "abbruch", "keine", "keins", "keinen", "nein", "vergiss", "egal", "hierbleiben", "bleiben")
    private val ORDNUNG = mapOf(
        "1" to 0, "eins" to 0, "erste" to 0, "ersten" to 0, "erstes" to 0,
        "2" to 1, "zwei" to 1, "zweite" to 1, "zweiten" to 1, "zweites" to 1,
        "3" to 2, "drei" to 2, "dritte" to 2, "dritten" to 2, "drittes" to 2,
    )
}
