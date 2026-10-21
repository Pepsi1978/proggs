package de.frank.jarvis.faehigkeit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Ein Auftrag ohne Trenner verlässt das Handy zeilengenau: keine zusätzlichen Leerzeilen, keine verlorenen Absätze. Die Trenner
 * selbst prüft DiktatTrennerTest. (Die Leerzeilen am Mac vom 21.10.2026 entstanden erst im Terminal, siehe
 * werkzeuge/pc-bruecke/test_auftragstext.py und bugs/desktop/terminal-app-rollbalken-leerzeilen.md.)
 */
class AuftragsTextTest {
    private val metadaten = "APP: Jarvis (Ordner Jarvis)\nREVIEW: ja\nAUFTRAG:\nKurz."
    private val absaetze = "Erster Absatz,\nzweite Zeile.\n\nZweiter Absatz.\n\nAbnahme: Tests."
    private val liste = "Anforderungen:\n- Punkt eins\n- Punkt zwei\n1. Schritt\n2. Schritt"
    private val code = "Beispiel:\n```kotlin\nfor (;;) {\n\n    tu()\n}\n```\nDanach weiter."

    @Test fun auftragOhneTrennerBleibtZeilengenau() {
        for (text in listOf(metadaten, absaetze, liste, code)) assertEquals(text, DiktatTrenner.auftrag(text))
    }

    @Test fun fortschrittKommtNurMitEinerLeerzeileDahinter() {
        for (text in listOf(metadaten, absaetze, liste, code)) {
            val gesamt = ProgrammierFaehigkeit.auftragMitFortschritt(text, "FORTSCHRITT (AUFTRAG-ID: X): …")
            assertEquals("$text\n\nFORTSCHRITT (AUFTRAG-ID: X): …", gesamt)
            assertFalse(gesamt.contains("\n\n\n"))
        }
    }
}
