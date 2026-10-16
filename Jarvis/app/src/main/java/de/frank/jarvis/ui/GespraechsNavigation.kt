package de.frank.jarvis.ui

import de.frank.jarvis.data.Sitzung

/** Ein Navigationsbefehl zwischen Gesprächen („Sessions“): Er bleibt in der App und geht nie ans Modell. */
sealed class NaviBefehl {
    /** „Öffne eine neue Session“ */
    object Neu : NaviBefehl()
    /** „Gehe in die letzte Session zurück“; [programmiert] bei „die letzte Programmier-Session“. */
    class Zurueck(val programmiert: Boolean = false) : NaviBefehl()
    /** „Gehe in die Whoop-Session zurück, wo wir programmiert haben“: Stichwörter und ob es ums Programmieren ging. */
    class Thema(val stichworte: List<String>, val programmiert: Boolean) : NaviBefehl()
    /** „Geh in die andere Programmier-Session“: eine andere noch offene Session, optional mit Stichwörtern („die andere Whoop-Session“). */
    class Andere(val stichworte: List<String>, val programmiert: Boolean) : NaviBefehl()
}

/** Franks Antwort auf die Rückfrage „Welche Session meinst du?“ */
sealed class NaviWahl {
    class Treffer(val sitzung: Sitzung) : NaviWahl()
    object Abbruch : NaviWahl()
    /** Keine Antwort auf die Rückfrage: Der Satz ist etwas anderes und läuft normal weiter. */
    object Keine : NaviWahl()
}

/**
 * Was Jarvis über eine Session weiß, frisch zusammengestellt bei jedem Wechsel: das gespeicherte Gespräch und, falls aus ihm
 * programmiert wird, App, Stand (laeuft, frage, fertig, fehler), letztes Lebenszeichen und Inhalt seiner Aufträge.
 */
data class SessionLage(
    val sitzung: Sitzung,
    val app: String = "",
    val stand: String = "",
    val aktiv: Long = 0L,
    val auftrag: String = "",
) {
    val programmiert: Boolean get() = stand.isNotEmpty()

    /**
     * Läuft hier noch eine Programmier-Sitzung? Eine Rückfrage wartet auf Frank und bleibt offen; fertige und gescheiterte Aufträge
     * sind abgeschlossen. Ohne Lebenszeichen über viele Stunden gilt auch „läuft“ als liegen geblieben (nie „fertig“ gemeldet).
     */
    fun offen(jetzt: Long): Boolean = when (stand) {
        "laeuft" -> jetzt - aktiv < LAUF_FRIST
        "frage" -> jetzt - aktiv < FRAGE_FRIST
        else -> false
    }

    /** Verständliche Bezeichnung für Rückfragen: App und Titel, bei einer offenen Rückfrage mit Hinweis. */
    fun bezeichnung(): String {
        val titel = sitzung.titel.takeIf { it.isNotBlank() } ?: "ohne Titel"
        val mitApp = if (app.isNotBlank() && !titel.contains(app, ignoreCase = true)) "$app: „$titel“" else "„$titel“"
        return if (stand == "frage") "$mitApp, wartet auf deine Antwort" else mitApp
    }

    companion object {
        const val LAUF_FRIST = 6 * 3_600_000L
        const val FRAGE_FRIST = 48 * 3_600_000L
    }
}

/** Ergebnis der Zielsuche: öffnen, nachfragen oder mit Begründung nichts tun. */
sealed class NaviZiel {
    class Oeffnen(val lage: SessionLage) : NaviZiel()
    class Rueckfrage(val kandidaten: List<SessionLage>) : NaviZiel()
    class Keins(val grund: String) : NaviZiel()
}

/**
 * Erkennt Navigationsbefehle zwischen Gesprächen und findet das gemeinte Gespräch. Rein in der App und ohne Modell:
 * So kann ein Navigationsbefehl nie einen Programmierauftrag oder ein anderes Werkzeug auslösen.
 */
object GespraechsNavigation {

    fun worte(text: String): List<String> =
        text.lowercase(java.util.Locale.GERMAN).replace(Regex("[^a-zäöüß0-9]+"), " ").split(' ').filter { it.isNotBlank() }

    /** „Programmiersession“, „Whoopsession“ als ein Wort: in Vorderteil und Nomen trennen. „Sprachsession“ bleibt ganz. */
    private fun zerlege(wort: String): List<String> {
        if (wort in NOMEN) return listOf(wort)
        val nomen = NOMEN.filter { it.length >= 4 && wort.endsWith(it) && wort.length >= it.length + 3 }.maxByOrNull { it.length } ?: return listOf(wort)
        val vorn = wort.removeSuffix(nomen).removeSuffix("s").removeSuffix("-")
        if (vorn.startsWith("sprach")) return listOf(wort)
        return listOf(vorn, nomen)
    }

    fun erkenne(text: String): NaviBefehl? {
        val alle = worte(text).flatMap { zerlege(it) }
        if (alle.isEmpty() || alle.size > 24) return null
        val kern = alle.filter { it !in FUELL }
        val nomen = kern.indices.filter { kern[it] in NOMEN }
        if (nomen.isEmpty()) return null
        // Ein Satz, der etwas bauen oder ändern lassen will, ist ein Auftrag und keine Navigation.
        if (kern.any { w -> w == "programmiere" || AUFTRAG_VERBEN.any { w.startsWith(it) } }) return null
        // Fragen („Was war das letzte Gespräch über Whoop?“) beantwortet Jarvis selbst.
        if (alle.first() in FRAGE || alle.any { it == "wissen" || it == "weißt" }) return null
        val bewegt = kern.any { it in VERBEN || it == "zurück" }
        // „Ich möchte in die andere Session“, „Ich will zur Whoop-Session“: Wunsch und Richtung ersetzen das Verb.
        val wunsch = alle.any { it in WUNSCH } && alle.any { it in RICHTUNG }
        val hatVerb = bewegt || wunsch
        // „Geh in die Session mit dem Whoop-Auftrag“ beschreibt ein Ziel; „Starte einen Programmierauftrag …“ ist ein Auftrag.
        val wechselt = kern.any { it in VERBEN && it !in ANLEGEN || it == "zurück" } || wunsch
        if (!wechselt && kern.any { w -> AUFTRAG_NOMEN.any { w.startsWith(it) } }) return null
        // „neue Session“, „ein neues Gespräch“: „neu“ direkt vor dem Nomen.
        if (nomen.any { i -> (1..2).any { d -> kern.getOrNull(i - d) in NEU } } && (hatVerb || kern.size <= 3)) return NaviBefehl.Neu
        val programmiert = kern.any { it.startsWith("programm") || it in CODE }
        val stichworte = kern.filter {
            it !in STOPP && it !in VERBEN && it !in NOMEN && it !in NEU && it !in ZURUECK && it !in ANDERE && it !in CODE && it.length >= 3 &&
                !it.startsWith("programm") && AUFTRAG_NOMEN.none { a -> it.startsWith(a) }
        }.distinct()
        // „die andere (Whoop-Programmier-)Session“: „andere“ bis zu drei Wörter vor dem Nomen.
        val andere = nomen.any { i -> (1..3).any { d -> kern.getOrNull(i - d) in ANDERE } }
        if (andere && (hatVerb || alle.any { it in RICHTUNG } || kern.size <= 3)) return NaviBefehl.Andere(stichworte, programmiert)
        if (!hatVerb) return null
        return when {
            stichworte.isNotEmpty() -> NaviBefehl.Thema(stichworte, programmiert)
            kern.any { it in ZURUECK } -> NaviBefehl.Zurueck(programmiert)
            else -> null
        }
    }

    /**
     * Live-Gespräch: Ist dieser Werkzeugaufruf ein Sessionbefehl? Zuerst zählt der Auftrag, den das Live-Modell weitergibt, sonst
     * Franks eigene letzte Äußerung im Wortlaut (das Modell formuliert beim Delegieren oft um). Bei offener Rückfrage
     * „Welche Session meinst du?“ zählt auch eine Auswahl wie „die erste“. null = normaler Werkzeugaufruf, er geht an Jarvis.
     */
    fun liveBefehl(auftrag: String, aeusserung: String?, rueckfrage: List<SessionLage>?): String? =
        listOfNotNull(auftrag.takeIf { it.isNotBlank() }, aeusserung?.takeIf { it.isNotBlank() }).firstOrNull { t ->
            erkenne(t) != null || rueckfrage != null && waehle(t, rueckfrage) !is NaviWahl.Keine
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
    fun <T> auswahl(bewertet: List<Pair<T, Double>>): List<T> {
        val sortiert = bewertet.filter { it.second > 0.0 }.sortedByDescending { it.second }
        val beste = sortiert.firstOrNull()?.second ?: return emptyList()
        return sortiert.filter { it.second >= beste * 0.7 }.take(3).map { it.first }
    }

    /** Noch offene Programmier-Sessions außer der geöffneten, die zuletzt aktive zuerst. */
    fun offeneAndere(lagen: List<SessionLage>, aktuell: String?, jetzt: Long): List<SessionLage> =
        lagen.filter { it.sitzung.id != aktuell && it.offen(jetzt) }.sortedByDescending { maxOf(it.aktiv, it.sitzung.geaendert) }

    /**
     * „Geh in die andere Programmier-Session“: nur noch offene Programmier-Sessions, nie die geöffnete, nie abgeschlossene. Genau eine =
     * öffnen; mehrere = erst über die Stichwörter, dann über den Gesprächskontext (genau eine App wird dort genannt) auflösen, sonst
     * nachfragen. null = kein Programmier-Bezug („die andere Session“ in einem Gespräch ohne Auftrag): wie „zurück“ oder,
     * mit Stichwörtern, wie eine Themensuche behandeln.
     */
    fun andereZiel(befehl: NaviBefehl.Andere, lagen: List<SessionLage>, aktuell: String?, kontext: String, inhalte: (SessionLage) -> String, jetzt: Long): NaviZiel? {
        val hierProgrammiert = lagen.firstOrNull { it.sitzung.id == aktuell }?.offen(jetzt) == true
        val kandidaten = offeneAndere(lagen, aktuell, jetzt)
        if (!befehl.programmiert && !hierProgrammiert) return null
        if (kandidaten.isEmpty()) {
            return NaviZiel.Keins(if (hierProgrammiert) "Außer dieser läuft gerade keine andere Programmier-Session." else "Gerade läuft keine andere Programmier-Session.")
        }
        if (kandidaten.size == 1) return NaviZiel.Oeffnen(kandidaten.first())
        if (befehl.stichworte.isNotEmpty()) {
            val thema = NaviBefehl.Thema(befehl.stichworte, true)
            val vorn = auswahl(kandidaten.map { it to punkte(thema, it.sitzung.titel, inhalte(it), it.app + " " + it.auftrag) })
            if (vorn.size == 1) return NaviZiel.Oeffnen(vorn.first())
            if (vorn.isNotEmpty()) return NaviZiel.Rueckfrage(vorn)
        }
        val genannt = worte(kontext).toSet()
        val imKontext = kandidaten.filter { k -> k.app.isNotBlank() && worte(k.app).filter { it !in FUELL && it.length >= 3 }.let { w -> w.isNotEmpty() && w.all { it in genannt } } }
        val appsImKontext = imKontext.map { it.app.lowercase() }.distinct()
        if (imKontext.size == 1 && appsImKontext.size == 1) return NaviZiel.Oeffnen(imKontext.first())
        return NaviZiel.Rueckfrage(kandidaten.take(3))
    }

    /**
     * „Gehe in die letzte Session zurück“: die vorher geöffnete, sonst die zuletzt geänderte andere. Bei „die letzte
     * Programmier-Session“ zuerst offene Programmier-Sessions, sonst die zuletzt bearbeitete Programmier-Session.
     */
    fun zurueckZiel(befehl: NaviBefehl.Zurueck, lagen: List<SessionLage>, aktuell: String?, vorherige: String?, jetzt: Long): SessionLage? {
        val andere = lagen.filter { it.sitzung.id != aktuell }
        val auswahl = if (!befehl.programmiert) andere else offeneAndere(lagen, aktuell, jetzt).ifEmpty {
            andere.filter { it.programmiert }.sortedByDescending { maxOf(it.aktiv, it.sitzung.geaendert) }
        }
        return auswahl.firstOrNull { it.sitzung.id == vorherige } ?: auswahl.maxByOrNull { if (befehl.programmiert) maxOf(it.aktiv, it.sitzung.geaendert) else it.sitzung.geaendert }
    }

    /**
     * „Geh in die Whoop-Session“: bewertet alle Sessions nach Titel, Inhalt und Aufträgen. Ging es ums Programmieren und passt
     * genau eine noch offene Programmier-Session, gewinnt sie. Die geöffnete Session fällt heraus, solange es andere Treffer gibt.
     */
    fun themaZiel(befehl: NaviBefehl.Thema, lagen: List<SessionLage>, aktuell: String?, inhalte: (SessionLage) -> String, jetzt: Long): List<SessionLage> {
        // Eine noch laufende Session ist das wahrscheinlichere Ziel als ein altes Gespräch zum selben Thema.
        val bewertet = lagen.map { l ->
            val p = punkte(befehl, l.sitzung.titel, inhalte(l), if (l.programmiert) l.app + " " + l.auftrag else "")
            l to if (p > 0.0 && l.offen(jetzt)) p + 2.0 else p
        }.filter { it.second > 0.0 }
        val offene = bewertet.filter { it.first.offen(jetzt) }
        val basis = if (befehl.programmiert && offene.isNotEmpty()) offene else bewertet
        val ohneHier = basis.filter { it.first.sitzung.id != aktuell }
        return auswahl(ohneHier.ifEmpty { basis })
    }

    /** Wertet Franks Antwort auf die Rückfrage aus: Nummer, Ordinalzahl oder ein Wort aus Titel oder App. */
    fun waehle(text: String, kandidaten: List<SessionLage>): NaviWahl {
        val w = worte(text).filter { it !in FUELL }
        if (w.isEmpty()) return NaviWahl.Keine
        if (w.size <= 4 && w.any { it in ABBRUCH }) return NaviWahl.Abbruch
        if (w.size <= 5) {
            val nummer = w.firstNotNullOfOrNull { ORDNUNG[it] }
            if (nummer != null && nummer < kandidaten.size) return NaviWahl.Treffer(kandidaten[nummer].sitzung)
            if (w.any { it == "letzte" || it == "letzten" } && w.size <= 3) return NaviWahl.Treffer(kandidaten.last().sitzung)
        }
        if (w.size > 10) return NaviWahl.Keine
        val stichworte = w.filter { it !in STOPP && it !in NOMEN && it !in VERBEN && it.length >= 3 && !it.startsWith("programm") }
        if (stichworte.isEmpty()) return NaviWahl.Keine
        val bewertet = kandidaten.map { k ->
            val titel = worte(k.sitzung.titel)
            val app = worte(k.app)
            k to stichworte.sumOf { s -> (if (treffer(titel, s) > 0) 2 else 0) + (if (treffer(app, s) > 0) 1 else 0) }
        }.filter { it.second > 0 }
        val beste = bewertet.maxOfOrNull { it.second } ?: return NaviWahl.Keine
        val vorn = bewertet.filter { it.second == beste }
        return if (vorn.size == 1) NaviWahl.Treffer(vorn.first().first.sitzung) else NaviWahl.Keine
    }

    private val NOMEN = setOf(
        "session", "sessions", "sitzung", "sitzungen", "gespräch", "gespräche", "gesprächs", "chat", "chats", "unterhaltung",
        "konversation", "unterhaltungen", "sesion", "sesschen", "sessionen",
    )
    private val NEU = setOf("neue", "neues", "neuen", "neuer", "neu", "frische", "frisches", "leere", "leeres")
    private val ZURUECK = setOf("letzte", "letzten", "vorherige", "vorherigen", "vorige", "vorigen", "zurück", "davor", "eben")
    private val ANDERE = setOf("andere", "anderen", "anderer", "anderes")
    /** Verben, die etwas Neues anlegen statt zu wechseln: Mit ihnen bleibt ein Programmierauftrag ein Auftrag. */
    private val ANLEGEN = setOf("mach", "mache", "machen", "starte", "starten", "startest", "beginne", "beginnen", "leg", "lege", "anlegen", "auf")
    private val CODE = setOf("code", "coding", "codings", "gecodet", "gebaut")
    private val WUNSCH = setOf("möchte", "möchten", "will", "wollen", "würde", "würden", "mag", "muss", "müssen")
    private val RICHTUNG = setOf("in", "ins", "zur", "zum", "rüber", "hin", "dahin", "zurück")
    private val VERBEN = setOf(
        "öffne", "öffnen", "öffnest", "mach", "mache", "machen", "starte", "starten", "startest", "beginne", "beginnen", "leg", "lege", "anlegen",
        "geh", "gehe", "gehen", "wechsel", "wechsle", "wechseln", "wechsele", "spring", "springe", "springen", "bring", "bringe",
        "zeig", "zeige", "zeigen", "hol", "hole", "holen", "nimm", "zurückgehen", "zurückwechseln", "zurückspringen", "lade", "laden", "auf",
        "rüber", "rübergehen", "rüberwechseln", "schalte", "schalt", "umschalten",
    )
    private val FUELL = setOf(
        "jarvis", "frank", "franks", "hey", "okay", "ok", "bitte", "also", "und", "dann", "jetzt", "mal", "kannst", "könntest", "würdest", "du", "lass", "lasst",
        "uns", "wir", "ich", "mich", "mir", "möchte", "will", "würde", "gern", "gerne", "eine", "ein", "einen", "einem", "einer", "die", "der",
        "das", "den", "dem", "des", "in", "ins", "zur", "zum", "zu", "noch", "mal", "doch",
    )
    private val STOPP = FUELL + WUNSCH + setOf(
        "wo", "mit", "haben", "hatten", "hat", "habe", "hast", "über", "von", "vom", "im", "an", "am", "auf", "um", "für", "gearbeitet",
        "gesprochen", "geredet", "worden", "wurde", "es", "ging", "geht", "wieder", "nochmal", "bei", "gemacht", "diskutiert", "besprochen",
        "thema", "nach", "dort", "da", "als", "letztes", "gestern", "heute", "vorhin", "gebaut", "gecodet", "angefangen", "sind", "waren",
        "war", "ist", "da", "drin", "darin", "dazu", "worum", "welche", "welcher", "welches", "diese", "dieser", "dieses", "zurück", "hin",
        "app", "sowie", "oder", "aus", "alte", "alten", "andere", "anderen", "läuft", "laufende", "laufenden", "offene", "offenen", "rüber",
        "gerade", "aktuell", "aktuelle", "aktuellen", "dahin",
    )
    private val AUFTRAG_VERBEN = listOf("ergänz", "implementier", "baue", "einbau", "ändere", "fixe", "behebe", "erstelle", "repariere", "verbesser", "optimier", "entwickl")
    private val AUFTRAG_NOMEN = listOf("programmierauftrag", "auftrag", "aufträge", "funktion", "feature", "änderung")
    private val FRAGE = setOf("was", "wie", "wann", "welche", "welches", "welcher", "warum", "wieso", "weshalb", "hast", "gibt", "gab", "weißt", "wer", "wieviele", "läuft", "laufen", "ist")
    private val ABBRUCH = setOf("abbrechen", "abbruch", "keine", "keins", "keinen", "nein", "vergiss", "egal", "hierbleiben", "bleiben")
    private val ORDNUNG = mapOf(
        "1" to 0, "eins" to 0, "erste" to 0, "ersten" to 0, "erstes" to 0,
        "2" to 1, "zwei" to 1, "zweite" to 1, "zweiten" to 1, "zweites" to 1,
        "3" to 2, "drei" to 2, "dritte" to 2, "dritten" to 2, "drittes" to 2,
    )
}
