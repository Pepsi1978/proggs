package de.frank.claudekompass

import de.frank.claudekompass.tts.TextSaeuberer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prüft, dass die Vorlese-Einheiten wirklich Absätze sind — und keine gestückelten Sätze. */
class TextSaeubererTest {

    @Test
    fun jederAbsatzWirdEineEinheit() {
        val text = "Erster Absatz.\n\nZweiter Absatz.\n\nDritter Absatz."
        val teile = TextSaeuberer.teileInAbsaetze(text)
        assertEquals(3, teile.size)
        assertEquals("Erster Absatz.", teile[0])
        assertEquals("Dritter Absatz.", teile[2])
    }

    @Test
    fun kurzeAbsaetzeWerdenNichtZusammengelegt() {
        val text = "Kurz.\n\nAuch kurz.\n\nEbenfalls."
        // Ein Zusammenlegen wäre bequemer für die Anzahl der Anfragen, würde aber die
        // Sprechpausen zwischen den Absätzen zerstören.
        assertEquals(3, TextSaeuberer.teileInAbsaetze(text).size)
    }

    @Test
    fun einzeilenumbruchTeiltNicht() {
        val text = "Eine Zeile\nund noch eine Zeile im selben Absatz."
        val teile = TextSaeuberer.teileInAbsaetze(text)
        assertEquals(1, teile.size)
        assertEquals("Eine Zeile und noch eine Zeile im selben Absatz.", teile[0])
    }

    @Test
    fun ueberlangerAbsatzWirdAnSatzgrenzenGeteilt() {
        val satz = "Dies ist ein Satz mit einer gewissen Laenge, damit die Grenze greift. "
        val text = satz.repeat(40)
        val teile = TextSaeuberer.teileInAbsaetze(text)
        assertTrue("Es muss geteilt worden sein", teile.size > 1)
        assertTrue(
            "Kein Teil darf über der Dienstgrenze liegen",
            teile.all { it.length <= TextSaeuberer.MAX_ZEICHEN },
        )
        // An Satzgrenzen geteilt: Kein Teil beginnt mitten in einem Wort.
        assertTrue(teile.all { it.trim() == it })
    }

    @Test
    fun auszeichnungenVerschwinden() {
        val gereinigt = TextSaeuberer.saeubere(
            "## Überschrift\n\n**fett** und `code` und - Aufzählung",
        )
        assertFalse(gereinigt.contains("#"))
        assertFalse(gereinigt.contains("*"))
        assertFalse(gereinigt.contains("`"))
        assertTrue(gereinigt.contains("fett"))
        assertTrue(gereinigt.contains("code"))
    }

    @Test
    fun adressenWerdenNichtBuchstabiert() {
        val gereinigt = TextSaeuberer.saeubere("Siehe https://code.claude.com/docs für mehr.")
        assertFalse(gereinigt.contains("https"))
        assertTrue(gereinigt.contains("Internetadresse"))
    }

    @Test
    fun codeBlockWirdErsetztStattGeloescht() {
        // Ersatzlos gestrichen fehlte im Gehörten der Zusammenhang.
        val gereinigt = TextSaeuberer.saeubere("Vorher\n\n```\nkonsole --hilfe\n```\n\nNachher")
        assertTrue(gereinigt.contains("Codebeispiel"))
        assertFalse(gereinigt.contains("konsole"))
        assertTrue(gereinigt.contains("Vorher"))
        assertTrue(gereinigt.contains("Nachher"))
    }

    @Test
    fun emojiFallenWeg() {
        val gereinigt = TextSaeuberer.saeubere("Fertig ✅ und gut 🎉")
        assertFalse(gereinigt.contains("✅"))
        assertTrue(gereinigt.contains("Fertig"))
        assertTrue(gereinigt.contains("gut"))
    }

    @Test
    fun leererTextGibtLeereListe() {
        assertTrue(TextSaeuberer.teileInAbsaetze("   \n\n  ").isEmpty())
    }
}
