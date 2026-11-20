package de.frank.jarvis.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRegelnTest {
    private val anthropic = NewsMeldung("a1/m1", "KI Nachrichten", "Anthropic schließt ersten Handelstag mit siebzehn Prozent Plus", quelle = "https://www.marketscreener.com/news/anthropic-debut-123")
    private val senat = NewsMeldung("a1/m2", "KI Nachrichten", "US-Senat will Rechenzentren an Netzkosten beteiligen", quelle = "https://apnews.com/article/energy-data-center")
    private val gladbach = NewsMeldung("a1/m3", "Fußball kompakt", "Vier Dortmunder Einsätze gegen Gladbach offen", quelle = "https://www.bvb.de/news/1")

    @Test fun genanntWirdWeggelassenNeuesKommtDazu() {
        // Erster Überblick: Jarvis nennt nur die Anthropic-Meldung.
        val antwort = "Bei den KI-News: Anthropic hat seinen ersten Börsentag mit 17 Prozent Plus abgeschlossen."
        assertTrue(NewsRegeln.genanntIn(antwort, anthropic))
        assertFalse(NewsRegeln.genanntIn(antwort, senat))
        val genannt = listOf(Genannt(anthropic.titel, anthropic.quelle, 0L))
        // Erneute Abfrage vor neuen Meldungen: nur das Ungenannte.
        assertEquals(listOf(senat, gladbach), NewsRegeln.neu(listOf(anthropic, senat, gladbach), genannt))
        // Nach dem 17-Uhr-Lauf: dieselbe Geschichte aus dem anderen Gerät (andere id, gleiche Adresse) bleibt weg, Neues kommt.
        val tablet = anthropic.copy(id = "b9/x1", titel = "Anthropic: Kurssprung am ersten Handelstag")
        val neu = NewsMeldung("a2/m1", "KI Nachrichten", "OpenAI stellt neues Modell vor", quelle = "https://openai.com/blog/neu")
        assertEquals(listOf(neu), NewsRegeln.neu(listOf(tablet, neu), genannt))
    }

    @Test fun dortmundErgebnisseBleibenVerborgen() {
        assertFalse(NewsRegeln.spoiler(gladbach))
        assertTrue(NewsRegeln.spoiler(NewsMeldung("x", "Fußball", "Dortmund gewinnt 3:1 in Mönchengladbach")))
        assertTrue(NewsRegeln.spoiler(NewsMeldung("x", "Fußball", "BVB: Spiel in Gladbach", kurz = "Borussia Dortmund trennt sich 2:2 von Gladbach.")))
        assertFalse(NewsRegeln.spoiler(NewsMeldung("x", "Fußball", "Bayern gewinnt 3:0")))
        assertEquals("Kobel fehlt.", NewsRegeln.ohneErgebnis("Dortmund gewinnt 3:1. Kobel fehlt."))
    }

    /** Echte Schlagzeilen vom 20.11.2026: Nennt die Antwort alle außer einer, gilt die ausgelassene nicht als genannt. */
    @Test fun ausgelasseneSchlagzeileBleibtUngenannt() {
        val titel = listOf(
        "Anthropic schließt ersten Handelstag mit siebzehn Prozent Plus",
        "US-Senat will Rechenzentren an Netzkosten beteiligen",
        "Kaliforniens Justiz verlangt Auskunft von Nvidia",
        "US-Ausschuss legt Genehmigungen für KI-Chips offen",
        "Britische Abgeordnete fordern Aufsicht für Spitzen-KI",
        "Meta überprüft nach Emissionsanstieg sein Klimaziel",
        "Google öffnet Genie vier für Entwickler",
        "OpenAI gibt neues Modell für Sprachgespräche frei",
        "Gemini kann jetzt Programme auf dem Mac bedienen",
        "Blue Origin soll Googles KI-Satelliten starten",
        "EU-Staaten wollen den Umgang mit KI stärker vermitteln",
        "Apple-Forschung erprobt KI-Videos mit kürzerer Wartezeit",
        "Moonshot überarbeitet die Arbeitsoberfläche von Kimi",
        "Spanien, Belgien oder Norwegen für Deutschland",
        "Deutschland klettert auf Weltranglistenplatz elf",
        "Vier Dortmunder Einsätze gegen Gladbach offen",
        "Union muss gegen Leipzig auf vier Spieler verzichten",
        "Kovač erklärt Debatte um Nmechas Torjubel für beendet",
        "Dortmund kündigt Fantreffen vor dem Arsenal-Spiel an"
        )
        titel.forEachIndexed { n, weg ->
            val antwort = titel.filterIndexed { i, _ -> i != n }.joinToString(". ") + "."
            assertFalse("fälschlich genannt: $weg", NewsRegeln.genanntIn(antwort, NewsMeldung("x", "r", weg)))
            assertTrue(NewsRegeln.genanntIn(antwort, NewsMeldung("y", "r", titel[(n + 1) % titel.size])))
        }
    }
}
