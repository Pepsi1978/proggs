package de.frank.claudekompass.ai

/** Wie schwer ein Anmelde- oder Anfragefehler wiegt. */
enum class CodexFehlerArt {
    /** Neu anmelden nötig. */
    ANMELDUNG,

    /** Kontingent erschöpft. */
    KONTINGENT,

    /** Verbindungsproblem. */
    NETZ,
}

class CodexFehler(
    val art: CodexFehlerArt,
    meldung: String,
    ursache: Throwable? = null,
    /** true bei Störungen, die sich von allein erledigen (5xx, Zeitüberschreitung). */
    val wiederholbar: Boolean = false,
) : Exception(meldung, ursache)

/** Was die Anmeldung dem Benutzer zeigt, während sie auf die Bestätigung wartet. */
data class GeraeteAnmeldung(
    val benutzerCode: String,
    val bestaetigungsAdresse: String,
)

data class AnmeldeErgebnis(val email: String?)

/**
 * Zerlegt den Gerätecode in die Gruppen, die auf dem Anmeldebildschirm stehen.
 *
 * Die Länge bestimmt der Server: Heute kommen neun Zeichen, die die Webseite als vier plus
 * fünf abfragt. Die App darf deshalb NIE von einer eigenen Länge ausgehen — jedes gesendete
 * Zeichen muss auf den Bildschirm, sonst tippt man einen unvollständigen Code ein und die
 * Anmeldung scheitert.
 *
 * Der Reihe nach gilt:
 *  - Hat der Server selbst getrennt (Strich, Gedankenstrich, Leerzeichen), gilt seine Aufteilung.
 *  - Sonst wird in der Mitte geteilt, kürzere Hälfte zuerst (9 wird zu 4 plus 5).
 *  - Vier Zeichen oder weniger bleiben am Stück.
 *  - Abgeschnitten wird nie, egal wie lang der Code ist.
 */
fun geraeteCodeGruppen(code: String): List<String> {
    val bereinigt = code.trim()
    if (bereinigt.isEmpty()) return listOf(PLATZHALTER_KOPF, PLATZHALTER_SCHWANZ)

    val getrennt = bereinigt.split('-', '–', '—', ' ', '_').filter { it.isNotBlank() }
    if (getrennt.size > 1) return getrennt

    val zeichen = bereinigt.filter(Char::isLetterOrDigit)
    if (zeichen.isEmpty()) return listOf(bereinigt)
    if (zeichen.length <= 4) return listOf(zeichen)

    val kopf = zeichen.length / 2
    return listOf(zeichen.take(kopf), zeichen.drop(kopf))
}

private const val PLATZHALTER_KOPF = "––––"
private const val PLATZHALTER_SCHWANZ = "–––––"
