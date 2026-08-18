package de.frank.gedankenspeicher.tts

/**
 * **Die Vorlese-Einheiten.** Ein langer Text wird nicht am Stück an die Sprach-KI gegeben,
 * sondern in Absätzen — genau wie in der Cortex-App.
 *
 * Der Grund ist handfest: eine lange Auswertung überforderte die Sprachausgabe, und statt
 * eines Vorlesens kam gar nichts. Jeder Absatz wird einzeln synthetisiert und einzeln
 * vorgelesen; der nächste entsteht schon, während der laufende noch spricht (siehe
 * `Vorleser`).
 *
 * Die Regel dahinter: **immer ganze Absätze**. [HOECHSTZEICHEN] ist keine Wunschgrösse,
 * sondern nur der Zaun der Schnittstelle — nur ein übermässig langer Absatz wird zusätzlich
 * an Satzgrenzen geteilt, damit die Anfrage nicht abgelehnt wird.
 */
object Absaetze {

    /** Der Zaun der Schnittstelle, nicht die Wunschlänge eines Absatzes. */
    const val HOECHSTZEICHEN = 1000

    /** So viele Absätze werden im Voraus synthetisiert, während der laufende spricht. */
    const val VORAUS = 2

    private val mehrfachLeerraum = Regex("[ \t]+")
    private val absatzTrenner = Regex("\n{2,}")
    private val satzTrenner = Regex("(?<=[.!?])\\s+")

    /** Zerlegt einen Text in die Reihenfolge, in der er vorgelesen wird. */
    fun teile(text: String): List<String> {
        val geglaettet = text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(mehrfachLeerraum, " ")
            .trim()
        if (geglaettet.isBlank()) return emptyList()

        return geglaettet
            .split(absatzTrenner)
            .map { it.replace("\n", " ").trim() }
            .filter { it.isNotBlank() }
            .flatMap { teileLangenAbsatz(it) }
    }

    /** Nur der Notfall: ein Absatz, der über den Zaun geht, wird an Satzgrenzen geteilt. */
    private fun teileLangenAbsatz(absatz: String): List<String> {
        if (absatz.length <= HOECHSTZEICHEN) return listOf(absatz)
        val saetze = absatz.split(satzTrenner).filter { it.isNotBlank() }
        val ergebnis = mutableListOf<String>()
        var laufend = ""

        fun ablegen() {
            if (laufend.isNotBlank()) {
                ergebnis.add(laufend.trim())
                laufend = ""
            }
        }

        saetze.forEach { satz ->
            if (satz.length > HOECHSTZEICHEN) {
                ablegen()
                ergebnis += teileAnWortgrenzen(satz, HOECHSTZEICHEN)
            } else {
                val versuch = if (laufend.isBlank()) satz else "$laufend $satz"
                if (versuch.length <= HOECHSTZEICHEN) {
                    laufend = versuch
                } else {
                    ablegen()
                    laufend = satz
                }
            }
        }
        ablegen()
        return ergebnis
    }

    /** Der letzte Ausweg: ein einzelner Satz ohne Punkt, der länger ist als der Zaun. */
    private fun teileAnWortgrenzen(text: String, hoechstens: Int): List<String> {
        val ergebnis = mutableListOf<String>()
        var rest = text.trim()
        while (rest.length > hoechstens) {
            val schnitt = rest.take(hoechstens).indexOfLast { it == ' ' }
                .let { if (it > hoechstens / 2) it else hoechstens }
            ergebnis.add(rest.take(schnitt).trim())
            rest = rest.drop(schnitt).trim()
        }
        if (rest.isNotBlank()) ergebnis.add(rest)
        return ergebnis
    }
}
