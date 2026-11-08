package de.frank.jarvis.faehigkeit

import java.util.Locale

/**
 * Franks Antwort auf eine offene Auswahlfrage (Modell, CLI, Review), so wie die Mitschrift sie liefert: Er spricht fast immer ein,
 * deshalb zählt der Klang, nicht die Schreibweise („Oppos 5.5“, „Kimmi“, „Aragon“, „Kodex“, „Open Kot“). Vergleich je Wort über
 * die Kölner Phonetik und kleine Tippabstände, dazu die gesprochene Versionsnummer („… 5.5“ = Opus, „… 6.1“ = Sol).
 *
 * [antwort] ist streng: Als Wahl gilt nur eine reine Antwort aus dem Namen und Füllwörtern („nimm bitte Opus 5.5“). Jedes andere
 * Wort („Wie viele Tokens hat Opus verbraucht?“, „keinen Codex“) macht daraus keine Wahl. [modell] ist locker (für den Weg über das Modell).
 */
object ModellGehoert {
    private const val OPUS = ProgrammiererVorlage.MODELL_OPUS
    private const val SOL = ProgrammiererVorlage.MODELL_SOL
    private const val KIMI = ProgrammiererVorlage.MODELL_KIMI
    private const val ARGON = ProgrammiererVorlage.MODELL_ARGON

    private val WORT = Regex("[a-zäöüß]+|\\d+")
    private val KIMI_K3 = Regex("\\bk\\s*3\\b(?![.,]\\s*1)")
    private val PRO = Regex("\\bpro\\b")
    private val OPUS_ZAHL = Regex("\\b5\\s*[.,]\\s*5\\b|fünf\\s*(punkt|komma)?\\s*fünf")
    private val SOL_ZAHL = Regex("\\b6\\s*[.,]\\s*1\\b|sechs\\s*(punkt|komma)?\\s*eins")
    private val KIMI_ZAHL = Regex("\\b3\\s*[.,]\\s*1\\b|drei\\s*(punkt|komma)?\\s*eins")

    /** Wörter, die eine reine Antwort begleiten dürfen (Höflichkeit, „nimm“, Versionsangaben, Herstellernamen). */
    private val FUELLWOERTER = setOf(
        "ja", "jo", "okay", "ok", "oke", "gut", "bitte", "danke", "gerne", "gern", "nimm", "nehm", "nehme", "nehmen", "mit", "das", "den", "dem",
        "die", "der", "modell", "model", "version", "punkt", "komma", "fünf", "sechs", "eins", "drei", "vier", "pro", "k", "claude", "gpt",
        "google", "kimmi", "mach", "machs", "starte", "start", "starten", "los", "wir", "ich", "es", "ist", "jetzt", "einfach", "lieber",
        "dann", "also", "mal", "will", "möchte", "hätte", "für", "auftrag", "jarvis", "hey", "hallo", "und", "im", "in", "cli", "bitteschön",
        "genau", "richtig", "klar", "sicher", "natürlich", "doch", "dafür", "nimmst", "nimmt", "verwende", "verwenden", "benutze", "benutzen",
    )

    /** Knopfwert, wenn [worte] eine reine Antwort auf die Frage der [stufe] ist (modell, cli, review, review_umfang), sonst leer. */
    fun antwort(worte: String?, stufe: String): String {
        val t = worte?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (t.isEmpty() || t.length > 60) return ""
        val woerter = WORT.findAll(t).map { it.value }.toList()
        if (woerter.isEmpty()) return ""
        val treffer = LinkedHashSet<String>()
        // „soll“ ist meist das Verb („Opus soll es sein“), als Modell zählt es nur allein.
        val soll = (stufe == "modell" || stufe.isEmpty()) && "soll" in woerter
        var i = 0
        var unbekannt = 0
        while (i < woerter.size) {
            val w = woerter[i]
            if (soll && (w == "soll" || w == "sein")) { i++; continue }
            val naechstes = woerter.getOrNull(i + 1)
            val wert = when (stufe) {
                "cli" -> when {
                    // „Open Code“ in zwei Wörtern.
                    naechstes != null && klingtWie(w, "open") && klingtWie(naechstes, "code") -> { i++; ProgrammierFaehigkeit.CLI_OPENCODE }
                    klingtWie(w, "opencode") -> ProgrammierFaehigkeit.CLI_OPENCODE
                    klingtWie(w, "codex") -> ProgrammierFaehigkeit.CLI_CODEX
                    else -> null
                }
                "review" -> when (w) {
                    "ja", "jo", "jawohl", "gerne", "gern", "mit" -> "ja"
                    "nein", "nee", "ne", "kein", "keins", "keines", "keinen", "ohne", "nicht" -> "nein"
                    "standard", "normal" -> "standard"
                    "ausführlich", "ausführliches", "gründlich", "100", "hundert" -> "sehr_ausfuehrlich"
                    else -> null
                }
                "review_umfang" -> when (w) {
                    "standard", "normal", "normales" -> "standard"
                    "ausführlich", "ausführliches", "gründlich", "100", "hundert", "sehr" -> "sehr_ausfuehrlich"
                    else -> null
                }
                else -> wortModell(w)
            }
            when {
                wert != null -> treffer += wert
                w in FUELLWOERTER || w.all { it.isDigit() } -> Unit
                stufe == "review" || stufe == "review_umfang" -> if (w !in setOf("review", "reviews", "code", "bitte", "sehr", "machen", "mach")) return ""
                stufe == "modell" || stufe.isEmpty() -> unbekannt++
                else -> return ""
            }
            i++
        }
        // Ein einziges unverständliches Wort neben der Versionsnummer („Hoppers 5.5“) ist noch eine Antwort, sonst nicht.
        if (unbekannt > 0) return if (unbekannt == 1 && treffer.isEmpty()) zahlModell(t).orEmpty() else ""
        if (stufe == "review") {
            // „Ja, sehr ausführlich“: der Umfang zählt; „Ja“ und „Nein“ zugleich ist keine Antwort.
            treffer.remove("ja").let { if (it && treffer.isEmpty()) treffer += "ja" }
            if ("nein" in treffer && treffer.size > 1) return ""
        }
        if (stufe == "modell" || stufe.isEmpty()) {
            if (KIMI in treffer && KIMI_K3.containsMatchIn(t) && !PRO.containsMatchIn(t)) treffer -= KIMI
            if (treffer.isEmpty()) zahlModell(t)?.let { treffer += it }
            if (treffer.isEmpty() && soll) treffer += SOL
        }
        return treffer.singleOrNull().orEmpty()
    }

    /** Locker: opus, sol, kimi oder argon, wenn die kurze Antwort genau ein Modell nennt (auch zwischen anderen Wörtern), sonst leer. */
    fun modell(worte: String?): String {
        val t = worte?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (t.isEmpty() || t.length > 60) return ""
        val woerter = WORT.findAll(t).map { it.value }.toList()
        val treffer = woerter.mapNotNull { if (it == "soll") null else wortModell(it) }.toMutableSet()
        if (treffer.isEmpty() && "soll" in woerter) treffer += SOL
        if (KIMI in treffer && KIMI_K3.containsMatchIn(t) && !PRO.containsMatchIn(t)) treffer -= KIMI
        if (treffer.isEmpty()) zahlModell(t)?.let { treffer += it }
        return treffer.singleOrNull().orEmpty()
    }

    private fun zahlModell(t: String): String? = when {
        OPUS_ZAHL.containsMatchIn(t) -> OPUS
        SOL_ZAHL.containsMatchIn(t) -> SOL
        KIMI_ZAHL.containsMatchIn(t) -> KIMI
        else -> null
    }

    /** Das Modell, nach dem ein einzelnes Wort klingt, sonst null. */
    private fun wortModell(w: String): String? {
        if (w.length < 3 || w.all { it.isDigit() }) return null
        val code = koelner(w)
        return when {
            aehnlich(w, "gemini", 2) || aehnlich(w, "argon", 1) || (code == "0746" && w[0] in "aeo") -> ARGON
            code == "018" && w.length <= 7 && (w[0] == 'o' || w.startsWith("ho")) -> OPUS
            code == "85" && w.length <= 4 && w[0] in "sz" -> SOL
            code == "46" && w.length <= 5 && w[0] in "kcgq" -> KIMI
            else -> null
        }
    }

    private fun klingtWie(w: String, ziel: String) = w == ziel || koelner(w) == koelner(ziel) || (w.length >= 4 && aehnlich(w, ziel, 1))

    /** Höchstens [grenze] Änderungen (Levenshtein) zwischen [a] und [b]. */
    private fun aehnlich(a: String, b: String, grenze: Int): Boolean {
        if (kotlin.math.abs(a.length - b.length) > grenze) return false
        var vorher = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val jetzt = IntArray(b.length + 1)
            jetzt[0] = i
            for (j in 1..b.length) jetzt[j] = minOf(vorher[j] + 1, jetzt[j - 1] + 1, vorher[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            vorher = jetzt
        }
        return vorher[b.length] <= grenze
    }

    /** Kölner Phonetik eines Worts (Ziffernfolge, gleiche Nachbarn zusammengefasst, Nullen außer am Anfang entfernt). */
    internal fun koelner(wort: String): String {
        val w = wort.lowercase(Locale.ROOT).replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "s").filter { it in 'a'..'z' }
        val roh = StringBuilder()
        for (i in w.indices) {
            val c = w[i]
            val vor = w.getOrNull(i - 1)
            val nach = w.getOrNull(i + 1)
            roh.append(when (c) {
                'a', 'e', 'i', 'j', 'o', 'u', 'y' -> "0"
                'h' -> ""
                'b' -> "1"
                'p' -> if (nach == 'h') "3" else "1"
                'd', 't' -> if (nach != null && nach in "csz") "8" else "2"
                'f', 'v', 'w' -> "3"
                'g', 'k', 'q' -> "4"
                'c' -> when {
                    i == 0 -> if (nach != null && nach in "ahkloqrux") "4" else "8"
                    vor != null && vor in "sz" -> "8"
                    nach != null && nach in "ahkoqux" -> "4"
                    else -> "8"
                }
                'x' -> if (vor != null && vor in "ckq") "8" else "48"
                'l' -> "5"
                'm', 'n' -> "6"
                'r' -> "7"
                's', 'z' -> "8"
                else -> ""
            })
        }
        val ohneDoppelte = StringBuilder()
        for (ch in roh) if (ohneDoppelte.isEmpty() || ohneDoppelte.last() != ch) ohneDoppelte.append(ch)
        if (ohneDoppelte.isEmpty()) return ""
        return ohneDoppelte.first().toString() + ohneDoppelte.substring(1).replace("0", "")
    }
}
