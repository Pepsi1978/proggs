package de.frank.jarvis.faehigkeit

/**
 * Abschluss eines Programmierauftrags: ehrlich gekürzte Meldungen und die kurze Ansage, die Jarvis nach einem im Sprachgespräch
 * gestellten Auftrag vorliest (Sprachzustellung). Rein und ohne Android (ProgrammierAbschlussTest).
 *
 * Die Ansage stützt sich nur auf Bestätigtes: die Meldungen des Hooks am PC (📦 Committet, ⬆️ Gepusht, 📲 installiert,
 * ☁️ APK-Update, 🔴 … fehlgeschlagen), die aus den tatsächlich gelaufenen Befehlen entstehen, und die ausdrücklichen Angaben
 * der Sitzung („Committet: ja“ …). Was nirgends steht, heißt „nicht bestätigt“, nie „erledigt“.
 */
object ProgrammierAbschluss {
    /** Zwischenmeldungen und Rückfragen; so lang wie bisher. */
    const val MAX_MELDUNG = 4_000
    /** Der ausführliche Abschlussbericht am Zugende (Stand „info“). Früher galt auch hier 4.000: Der Bericht endete mitten im Wort. */
    const val MAX_BERICHT = 60_000
    /** Die Fertig-, Fehler- oder Zwischenmeldung über programmier_fortschritt (früher 400 Zeichen). */
    const val MAX_FORTSCHRITT = 2_000
    /** Steht am Ende jeder gekürzten Meldung: Nie ein unmarkiertes Satzfragment. */
    const val GEKUERZT = "… [gekürzt, der vollständige Text steht in der Sitzung]"
    /** Länge des vorgelesenen Kerns („was ist neu, wie funktioniert es“); der Rest steht im Bericht. */
    private const val MAX_KERN = 700

    /**
     * Kürzt [text] auf höchstens [max] Zeichen: am letzten Absatz-, Satz- oder Wortende vor der Grenze, dahinter [hinweis].
     * Kürzer als [max] bleibt der Text unverändert. Zerteilt kein Emoji (Ersatzpaar).
     */
    fun kuerzen(text: String, max: Int, hinweis: String = GEKUERZT): String {
        if (text.length <= max) return text
        val platz = (max - hinweis.length - 2).coerceAtLeast(1)
        val kopf = text.substring(0, platz)
        val mindestens = platz * 6 / 10
        val absatz = kopf.lastIndexOf('\n')
        val satz = SATZENDE.findAll(kopf).lastOrNull()?.let { it.range.first + 1 } ?: -1
        var schnitt = maxOf(absatz, satz).takeIf { it >= mindestens } ?: kopf.lastIndexOf(' ').takeIf { it >= mindestens } ?: platz
        if (schnitt in 1..kopf.length && Character.isHighSurrogate(kopf[schnitt - 1])) schnitt--
        return kopf.substring(0, schnitt).trimEnd() + (if (hinweis.isEmpty()) "" else "\n" + hinweis)
    }

    private val SATZENDE = Regex("""[.!?](?=\s)""")

    /** Ein Punkt des Abschlusses, wie er belegt ist. */
    enum class Stand { JA, FEHLGESCHLAGEN, NEIN, UNBEKANNT }

    /** [weg] = wie aufs Handy installiert wurde: „WLAN“, „Tunnel“ oder „Kabel“; null = nicht bekannt. */
    data class Fakten(val commit: Stand, val push: Stand, val installiert: Stand, val weg: String?, val apk: Stand)

    private enum class Punkt { COMMIT, PUSH, INSTALL, APK }

    /** Hook-Meldungen (aus den gelaufenen Befehlen) und ausdrückliche Angaben der Sitzung, in der Reihenfolge des Textes. */
    private val BELEG = Regex(
        """(📦 Committet)|(🔴 Commit fehlgeschlagen)|(⬆️ Gepusht)|(🔴 Push fehlgeschlagen)|(📲 Auf dem Handy installiert(?: \((WLAN|Tunnel|Kabel)\))?)""" +
            """|(🔴 Installation fehlgeschlagen)|(☁️ APK-Update für UpdateStation bereit)|(🔴 APK-Update fehlgeschlagen)""" +
            """|(?i:\b(committet|gepusht|installiert|apk-update)\*{0,2}\s*:\s*\*{0,2}\s*(ja|nein|fehlgeschlagen)\b([^\n·;]*))""",
    )

    /**
     * Was über Commit, Push, Installation und APK-Update belegt ist. [texte] = die Meldungen des Auftrags, älteste zuerst; die
     * jüngste Angabe je Punkt gilt (ein wiederholter Bau nach einem Fehler hebt den Fehler auf).
     */
    fun fakten(texte: List<String>): Fakten {
        val stand = mutableMapOf<Punkt, Stand>()
        var weg: String? = null
        for (text in texte) for (m in BELEG.findAll(text)) {
            val g = m.groupValues
            when {
                g[1].isNotEmpty() -> stand[Punkt.COMMIT] = Stand.JA
                g[2].isNotEmpty() -> stand[Punkt.COMMIT] = Stand.FEHLGESCHLAGEN
                g[3].isNotEmpty() -> stand[Punkt.PUSH] = Stand.JA
                g[4].isNotEmpty() -> stand[Punkt.PUSH] = Stand.FEHLGESCHLAGEN
                g[5].isNotEmpty() -> { stand[Punkt.INSTALL] = Stand.JA; weg = g[6].ifEmpty { null } }
                g[7].isNotEmpty() -> stand[Punkt.INSTALL] = Stand.FEHLGESCHLAGEN
                g[8].isNotEmpty() -> stand[Punkt.APK] = Stand.JA
                g[9].isNotEmpty() -> stand[Punkt.APK] = Stand.FEHLGESCHLAGEN
                g[10].isNotEmpty() -> {
                    val punkt = when (g[10].lowercase()) { "committet" -> Punkt.COMMIT; "gepusht" -> Punkt.PUSH; "installiert" -> Punkt.INSTALL; else -> Punkt.APK }
                    val s = when (g[11].lowercase()) { "ja" -> Stand.JA; "nein" -> Stand.NEIN; else -> Stand.FEHLGESCHLAGEN }
                    stand[punkt] = s
                    if (punkt == Punkt.INSTALL) weg = if (s == Stand.JA) wegAus(g[12]) ?: weg else null
                }
            }
        }
        fun von(p: Punkt) = stand[p] ?: Stand.UNBEKANNT
        return Fakten(von(Punkt.COMMIT), von(Punkt.PUSH), von(Punkt.INSTALL), weg.takeIf { von(Punkt.INSTALL) == Stand.JA }, von(Punkt.APK))
    }

    private fun wegAus(rest: String): String? = when {
        Regex("(?i)tunnel").containsMatchIn(rest) -> "Tunnel"
        Regex("(?i)wlan|wi-?fi|:5555").containsMatchIn(rest) -> "WLAN"
        Regex("(?i)kabel|usb").containsMatchIn(rest) -> "Kabel"
        else -> null
    }

    /** Die ausdrücklichen Angaben („Committet: ja · …“) und Hook-Zeichen gehören nicht in den vorgelesenen Kern. */
    private val STATUS_ZEILE = Regex("""(?im)^\W*(stand\s*:\s*)?(committet|gepusht|installiert|apk-update)\*{0,2}\s*:.*$""")
    private val STATUS_INLINE = Regex("""(?i)\s*(stand\s*:\s*)?\b(committet|gepusht|installiert|apk-update)\s*:\s*(ja|nein|fehlgeschlagen)\b[^·;\n]*[·;]?""")

    /** Der vorlesbare Kern einer Fertig- oder Fehlermeldung: ohne Statusangaben, Fettungen und Zeichen am Anfang, höchstens [MAX_KERN] am Satzende. */
    fun kern(text: String): String {
        val ohne = text.replace(STATUS_ZEILE, "").replace(STATUS_INLINE, " ").replace("**", "").replace("`", "")
            .replace(Regex("""^[^\p{L}\p{N}]+"""), "").replace(Regex("""[ \t]+"""), " ").replace(Regex("""\s*\n\s*"""), "\n").trim().trimEnd('·', ';', ',', ' ')
        return kuerzen(ohne, MAX_KERN, "Mehr steht im Bericht.")
    }

    /** Die Ansage davor (Zustellung.ansage). */
    fun ansage(app: String, fertig: Boolean): String =
        if (fertig) "Frank, dein Programmierauftrag für $app ist fertig." else "Frank, zu deinem Programmierauftrag für $app gibt es ein Problem."

    /**
     * Was Jarvis vorliest: der Kern der Fertigmeldung (was eingebaut ist, wie es jetzt funktioniert), danach je Punkt, ob er
     * bestätigt, fehlgeschlagen, noch nicht erledigt oder unbekannt ist, und wo der vollständige Bericht steht.
     * [cloud] = die Sitzung lief in der Cloud: Sie installiert nie selbst, aufs Handy kommt es über UpdateStation.
     */
    fun vorlesetext(meldung: String, f: Fakten, cloud: Boolean, fertig: Boolean): String = buildString {
        val k = kern(meldung)
        append(if (k.isNotBlank()) k.trimEnd() else if (fertig) "Eine Zusammenfassung hat die Sitzung nicht geschickt." else "Einen Grund hat die Sitzung nicht genannt.")
        if (last() !in ".!?") append('.')
        append("\n\n")
        if (f.commit == Stand.JA && f.push == Stand.JA) append("Committet und gepusht ist es.")
        else append(satz(f.commit, "Committet ist es.", "Der Commit ist fehlgeschlagen.", "Committet ist es noch nicht.", "Ob es committet ist, ist nicht bestätigt."))
            .append(' ').append(satz(f.push, "Gepusht ist es.", "Der Push ist fehlgeschlagen.", "Gepusht ist es noch nicht.", "Ob es gepusht ist, ist nicht bestätigt."))
        append(' ')
        append(when (f.installiert) {
            Stand.JA -> "Auf deinem Handy ist es installiert, " + when (f.weg) {
                "WLAN" -> "über WLAN."; "Tunnel" -> "über den Tunnel."; "Kabel" -> "per Kabel."; else -> "über welchen Weg, ist nicht bekannt."
            }
            Stand.FEHLGESCHLAGEN -> "Die Installation auf dem Handy ist fehlgeschlagen."
            Stand.NEIN -> if (cloud) "Direkt installiert ist es nicht, aufs Handy kommt es über UpdateStation." else "Auf dem Handy ist es noch nicht installiert."
            Stand.UNBEKANNT -> if (cloud) "Direkt installiert ist es nicht, aufs Handy kommt es über UpdateStation." else "Ob es auf dem Handy installiert ist, ist nicht bestätigt."
        })
        append(' ')
        append(satz(f.apk, "Die APK liegt bei UpdateStation bereit.", "Das APK-Update für UpdateStation ist fehlgeschlagen.",
            "Bei UpdateStation liegt noch keine neue APK.", "Ob eine APK bei UpdateStation bereitliegt, ist nicht bestätigt."))
        append("\n\nDen vollständigen Bericht findest du im Chat der Programmier-Session.")
    }

    private fun satz(s: Stand, ja: String, fehl: String, nein: String, unbekannt: String) = when (s) {
        Stand.JA -> ja; Stand.FEHLGESCHLAGEN -> fehl; Stand.NEIN -> nein; Stand.UNBEKANNT -> unbekannt
    }
}
