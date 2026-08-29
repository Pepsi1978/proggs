package de.frank.claudekompass

import de.frank.claudekompass.audio.FilterSchalter
import de.frank.claudekompass.audio.GroqAbschnitt
import de.frank.claudekompass.audio.GroqAntwort
import de.frank.claudekompass.audio.HalluzinationsFilter
import de.frank.claudekompass.audio.SprachAnalyse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prüft die drei Schichten, die im Filter sitzen.
 *
 * Der wichtigste Test ist [floskelBleibtWennGesprochen]: Die Sperrliste darf NIEMALS allein
 * wegen des Wortlauts löschen. Genau daran scheitern naive Umsetzungen — sie schlucken ein
 * bewusst gesagtes „Vielen Dank" mit.
 */
class HalluzinationsFilterTest {

    private fun analyse(gesprochenMs: Int, lauteRahmen: BooleanArray = BooleanArray(0)) =
        SprachAnalyse(gesprochenMs, 20, lauteRahmen)

    @Test
    fun schicht2VerwirftUnsicherenAbschnitt() {
        val antwort = GroqAntwort(
            text = "Vielen Dank fürs Zuschauen",
            abschnitte = listOf(
                GroqAbschnitt(0.0, 2.0, "Vielen Dank fürs Zuschauen", 0.9, -1.8, 1.0),
            ),
        )
        val ergebnis = HalluzinationsFilter().filtere(antwort, analyse(2000))
        assertEquals("", ergebnis)
    }

    @Test
    fun schicht2VerwirftWiederholungsschleife() {
        val antwort = GroqAntwort(
            text = "ja ja ja ja ja",
            abschnitte = listOf(GroqAbschnitt(0.0, 3.0, "ja ja ja ja ja", 0.1, -0.2, 3.1)),
        )
        assertEquals("", HalluzinationsFilter().filtere(antwort, analyse(3000)))
    }

    @Test
    fun sichererAbschnittBleibt() {
        val antwort = GroqAntwort(
            text = "Was macht der Befehl compact",
            abschnitte = listOf(
                GroqAbschnitt(0.0, 2.0, "Was macht der Befehl compact", 0.02, -0.2, 1.2),
            ),
        )
        val ergebnis = HalluzinationsFilter().filtere(antwort, analyse(2000))
        assertEquals("Was macht der Befehl compact", ergebnis)
    }

    @Test
    fun schicht3BehaeltAllesBeiZeitversatz() {
        // Alle Abschnitte lägen in der Stille. Das ist eher ein Zeitstempel-Versatz als eine
        // Erfindung — sonst würde ein ganzes Diktat verschwinden.
        val stilleRahmen = BooleanArray(200) { false }
        val antwort = GroqAntwort(
            text = "Ein ganz normaler Satz",
            abschnitte = listOf(GroqAbschnitt(0.0, 3.0, "Ein ganz normaler Satz", 0.05, -0.3, 1.1)),
        )
        val ergebnis = HalluzinationsFilter().filtere(antwort, analyse(3000, stilleRahmen))
        assertEquals("Ein ganz normaler Satz", ergebnis)
    }

    @Test
    fun floskelWirdImStilleUmfeldVerworfen() {
        val antwort = GroqAntwort(
            text = "Vielen Dank",
            abschnitte = listOf(GroqAbschnitt(0.0, 0.9, "Vielen Dank", 0.1, -0.4, 1.0)),
        )
        // Nur 200 ms laute Zeit: klarer Stille-Kontext.
        assertEquals("", HalluzinationsFilter().filtere(antwort, analyse(200)))
    }

    @Test
    fun floskelBleibtWennGesprochen() {
        val antwort = GroqAntwort(
            text = "Vielen Dank",
            abschnitte = listOf(GroqAbschnitt(0.0, 1.2, "Vielen Dank", 0.1, -0.4, 1.0)),
        )
        // 4 Sekunden laute Zeit: Der Satz wurde bewusst gesprochen und muss bleiben.
        assertEquals("Vielen Dank", HalluzinationsFilter().filtere(antwort, analyse(4000)))
    }

    @Test
    fun langerSatzMitFloskelBleibt() {
        val text = "Vielen Dank für den Hinweis, ich probiere den Befehl gleich einmal aus"
        val antwort = GroqAntwort(
            text = text,
            abschnitte = listOf(GroqAbschnitt(0.0, 4.0, text, 0.05, -0.3, 1.1)),
        )
        // Kurz genug ist er nicht — die Sperrliste darf hier gar nicht erst greifen.
        assertEquals(text, HalluzinationsFilter().filtere(antwort, analyse(300)))
    }

    @Test
    fun abgeschalteteSchichtenLassenAllesDurch() {
        val antwort = GroqAntwort(
            text = "Vielen Dank",
            abschnitte = listOf(GroqAbschnitt(0.0, 0.9, "Vielen Dank", 0.9, -1.8, 3.0)),
        )
        val ohneFilter = FilterSchalter(
            schicht1Stille = false,
            schicht2Kennzahlen = false,
            schicht3Zeitstempel = false,
            schicht4Floskeln = false,
        )
        val ergebnis = HalluzinationsFilter(ohneFilter).filtere(antwort, analyse(200))
        assertTrue(ergebnis.contains("Vielen Dank"))
    }

    @Test
    fun ohneAnalyseWirdKeineFloskelGeloescht() {
        val antwort = GroqAntwort(text = "Vielen Dank", abschnitte = null)
        // Ohne Analyse fehlt die dritte Bedingung — dann darf nichts verworfen werden.
        assertEquals("Vielen Dank", HalluzinationsFilter().filtere(antwort, null))
    }
}
