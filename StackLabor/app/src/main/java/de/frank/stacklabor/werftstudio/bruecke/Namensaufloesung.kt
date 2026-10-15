package de.frank.stacklabor.werftstudio.bruecke

import java.util.Locale

/** Ein Name, den Jarvis auflösen soll: ein Stack oder ein Mittel. */
data class Kandidat(val id: String, val name: String)

/** Ergebnis einer Namensauflösung. */
sealed interface Aufloesung {
    data class Eindeutig(val kandidat: Kandidat) : Aufloesung
    /** Mehrere passen gleich gut: Jarvis muss nachfragen, nie raten. */
    data class Mehrdeutig(val kandidaten: List<Kandidat>) : Aufloesung
    /** Nichts passt; [aehnlich] sind Vorschläge für die Rückfrage. */
    data class Keiner(val aehnlich: List<Kandidat>) : Aufloesung
}

/**
 * Löst gesprochene Namen sicher auf: „Sportmodus 2“, „Sport-Stacks 2“, „zweiter Sport-Stack“ → „Sport 2“,
 * „Nach dem Aufstehen 1“ → „Nach Aufstehen 1“, „Morgen 1“ → „Morgen-Stack Teil 1“.
 *
 * Stufen, die erste eindeutige gewinnt: gleiche id, gleicher Name, gleiche Wörter nach dem Glätten, alle Wörter der Anfrage
 * im Namen enthalten (Zahlen müssen stimmen), zuletzt Tippfehler (Abstand höchstens 1 je 6 Zeichen). Passen auf einer Stufe
 * mehrere, ist das Ergebnis mehrdeutig. Bei Stacks gelten Zahlen streng: „Sport 2“ passt nie zu „Sport 3“ und nie zu „Sport“.
 */
object Namensaufloesung {
    fun stack(anfrage: String, stacks: List<Kandidat>): Aufloesung = loese(anfrage, stacks, zahlenStreng = true)

    fun mittel(anfrage: String, mittel: List<Kandidat>): Aufloesung = loese(anfrage, mittel, zahlenStreng = false)

    /** Zwei Namen meinen dasselbe (für den Doppel-Schutz beim Anlegen). */
    fun gleich(a: String, b: String): Boolean = woerter(a) == woerter(b) && woerter(a).isNotEmpty()

    /**
     * Sehr ähnliche Namen (Tippfehler, Hörfehler der Spracherkennung), aber nicht gleich. Andere Zahlen machen einen Namen
     * nie „ähnlich“: „Sport 4“ ist kein Tippfehler für „Sport 1“.
     */
    fun aehnlich(anfrage: String, kandidaten: List<Kandidat>): List<Kandidat> {
        val qw = woerter(anfrage)
        val q = qw.joinToString(" ")
        if (q.isEmpty()) return emptyList()
        return kandidaten.filter { k ->
            val nw = woerter(k.name)
            val n = nw.joinToString(" ")
            n != q && qw.filter { it.zahl() }.sorted() == nw.filter { it.zahl() }.sorted() && abstand(q, n) <= erlaubterAbstand(maxOf(q.length, n.length))
        }
    }

    private fun loese(anfrage: String, kandidaten: List<Kandidat>, zahlenStreng: Boolean): Aufloesung {
        val roh = anfrage.trim()
        if (roh.isEmpty() || kandidaten.isEmpty()) return Aufloesung.Keiner(emptyList())
        stufe(kandidaten.filter { it.id == roh })?.let { return it }
        stufe(kandidaten.filter { it.name.trim().equals(roh, ignoreCase = true) })?.let { return it }
        val q = woerter(roh)
        if (q.isEmpty()) return Aufloesung.Keiner(emptyList())
        stufe(kandidaten.filter { woerter(it.name) == q })?.let { return it }
        stufe(kandidaten.filter { enthalten(q, woerter(it.name), zahlenStreng) })?.let { return it }
        val qText = q.joinToString(" ")
        stufe(kandidaten.filter { k ->
            val n = woerter(k.name)
            zahlenPassen(q, n, zahlenStreng) && abstand(qText, n.joinToString(" ")) <= erlaubterAbstand(maxOf(qText.length, n.joinToString(" ").length))
        })?.let { return it }
        // Nichts passt: Vorschläge nach gemeinsamen Wörtern.
        val vorschlaege = kandidaten
            .map { it to woerter(it.name).count { w -> q.any { a -> w.startsWith(a) || a.startsWith(w) } } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(6)
            .map { it.first }
        return Aufloesung.Keiner(vorschlaege)
    }

    /** null = auf dieser Stufe nichts gefunden, weiter zur nächsten. */
    private fun stufe(treffer: List<Kandidat>): Aufloesung? = when (treffer.distinctBy { it.id }.size) {
        0 -> null
        1 -> Aufloesung.Eindeutig(treffer.first())
        else -> Aufloesung.Mehrdeutig(treffer.distinctBy { it.id })
    }

    /** Alle Wörter der Anfrage stecken im Namen (Wortanfang genügt), und die Zahlen passen. */
    private fun enthalten(q: List<String>, name: List<String>, zahlenStreng: Boolean): Boolean {
        if (!zahlenPassen(q, name, zahlenStreng)) return false
        return q.filter { !it.zahl() }.all { a -> name.any { w -> !w.zahl() && w.startsWith(a) } }
    }

    /**
     * Streng (Stacks): Nennt die Anfrage Zahlen, muss der Name genau diese Zahlen haben. Nennt sie keine, passt jeder Name
     * (gibt es dann mehrere, wird nachgefragt). Locker (Mittel): Die Zahlen der Anfrage müssen im Namen vorkommen (D3 in „D3 + K2“).
     */
    private fun zahlenPassen(q: List<String>, name: List<String>, streng: Boolean): Boolean {
        val qz = q.filter { it.zahl() }
        if (qz.isEmpty()) return true
        val nz = name.filter { it.zahl() }
        return if (streng) qz.sorted() == nz.sorted() else nz.containsAll(qz)
    }

    private fun String.zahl(): Boolean = isNotEmpty() && all(Char::isDigit)

    /** Geglättete Wörter: klein, Umlaute ausgeschrieben, Zahlwörter als Ziffern, Füllwörter und „-stack“/„-modus“ entfernt. */
    fun woerter(text: String): List<String> {
        val klein = text.lowercase(Locale.GERMAN)
            .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
            .replace(Regex("[^a-z0-9]+"), " ")
            // „sport2“ → „sport 2“, „2er“ bleibt eine Zahl mit Rest.
            .replace(Regex("(?<=[a-z])(?=[0-9])|(?<=[0-9])(?=[a-z])"), " ")
        return klein.split(' ').filter { it.isNotBlank() }
            .map { w -> ENDUNGEN.fold(w) { acc, e -> if (acc.length > e.length + 2 && acc.endsWith(e)) acc.removeSuffix(e) else acc } }
            .map { ZAHLWOERTER[it] ?: it }
            .filter { it !in FUELLWOERTER }
    }

    private fun erlaubterAbstand(laenge: Int): Int = when {
        laenge < 5 -> 0
        else -> maxOf(1, laenge / 6)
    }

    /** Levenshtein-Abstand. */
    private fun abstand(a: String, b: String): Int {
        if (a == b) return 0
        var vorher = IntArray(b.length + 1) { it }
        var jetzt = IntArray(b.length + 1)
        for (i in 1..a.length) {
            jetzt[0] = i
            for (j in 1..b.length) {
                val kosten = if (a[i - 1] == b[j - 1]) 0 else 1
                jetzt[j] = minOf(jetzt[j - 1] + 1, vorher[j] + 1, vorher[j - 1] + kosten)
            }
            val t = vorher; vorher = jetzt; jetzt = t
        }
        return vorher[b.length]
    }

    /** Wortenden, die beim Sprechen dazukommen: „Sportmodus“, „Sportstack“, „Abendstacks“. */
    private val ENDUNGEN = listOf("stacks", "stack", "modus")

    private val FUELLWOERTER = setOf(
        "stack", "stacks", "modus", "teil", "der", "die", "das", "dem", "den", "des", "im", "in", "zum", "zur", "mein", "meine",
        "meinen", "meinem", "mit", "nummer", "nr", "no",
    )

    private val ZAHLWOERTER = mapOf(
        "eins" to "1", "ein" to "1", "eine" to "1", "erste" to "1", "erster" to "1", "ersten" to "1", "erstes" to "1", "i" to "1",
        "zwei" to "2", "zwo" to "2", "zweite" to "2", "zweiter" to "2", "zweiten" to "2", "zweites" to "2", "ii" to "2",
        "drei" to "3", "dritte" to "3", "dritter" to "3", "dritten" to "3", "drittes" to "3", "iii" to "3",
        "vier" to "4", "vierte" to "4", "vierter" to "4", "vierten" to "4", "viertes" to "4", "iv" to "4",
        "fuenf" to "5", "fuenfte" to "5", "fuenfter" to "5", "fuenften" to "5", "v" to "5",
        "sechs" to "6", "sechste" to "6", "sechster" to "6", "sechsten" to "6",
    )
}
