package de.frank.newskompass.news

import de.frank.newskompass.tts.TextSaeuberer
import org.junit.Assert.assertEquals
import org.junit.Test

class QuellenFilterTest {

    @Test
    fun linkZitatInKlammernFaelltWeg() {
        assertEquals(
            "Die Inflation ist gesunken.",
            QuellenFilter.entferne("Die Inflation ist gesunken ([tagesschau.de](https://www.tagesschau.de/a.html?utm_source=openai)).")
        )
    }

    @Test
    fun mehrereLinksInEinerKlammerFallenWeg() {
        assertEquals(
            "Der Streik endet am Montag.",
            QuellenFilter.entferne("Der Streik endet am Montag ([spiegel.de](https://spiegel.de/x), [zeit.de](https://zeit.de/y)).")
        )
    }

    @Test
    fun quellenKlammerFussnoteUndAdresseFallenWeg() {
        assertEquals(
            "Der Kanzler reist nach Paris. Das Treffen ist am Freitag.",
            QuellenFilter.entferne("Der Kanzler reist nach Paris (Quelle: Reuters). Das Treffen ist am Freitag [1] (reuters.com, apnews.com).")
        )
    }

    @Test
    fun linkMitWortBehaeltDasWort() {
        assertEquals(
            "Die Bundesregierung hat entschieden.",
            QuellenFilter.entferne("Die [Bundesregierung](https://bundesregierung.de) hat entschieden.")
        )
    }

    @Test
    fun normaleKlammernBleiben() {
        val text = "Die Quellensteuer (rund fünf Prozent) steigt (auch für die EU)."
        assertEquals(text, QuellenFilter.entferne(text))
    }

    @Test
    fun reineQuellenAbsaetzeVerschwinden() {
        assertEquals(
            listOf("Erster Absatz."),
            QuellenFilter.entferne(listOf("Erster Absatz.", "Quellen: tagesschau.de, spiegel.de"))
        )
    }

    @Test
    fun vorlesenLiestKeineQuellen() {
        assertEquals(
            listOf("Neues Modell.", "Es ist schneller."),
            TextSaeuberer.teileInAbsaetze("Neues Modell.\n\nEs ist schneller ([heise.de](https://heise.de/n)) [2].")
        )
    }

    @Test
    fun punktNachAdresseBleibt() {
        assertEquals("Details dazu. Danach mehr.", QuellenFilter.entferne("Details dazu https://example.com. Danach mehr."))
    }
}
