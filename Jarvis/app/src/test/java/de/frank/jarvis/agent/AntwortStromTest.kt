package de.frank.jarvis.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AntwortStromTest {
    private fun strom(text: String, schritt: Int = 3): String? {
        val s = AntwortStrom()
        return text.chunked(schritt).firstNotNullOfOrNull { s.neu(it) }
    }

    @Test fun meldetDenErstenSatzSobaldDerNaechsteBeginnt() {
        assertEquals("Heute bleibt es in Neuenhagen trocken und ziemlich mild.",
            strom("""{"antwort": "Heute bleibt es in Neuenhagen trocken und ziemlich mild. Morgen regnet es"""))
    }

    @Test fun werkzeugaufrufErgibtNieEinenSatz() {
        assertNull(strom("""{"werkzeug": "wetter_lesen", "argumente": {"text": "Heute bleibt es in Neuenhagen trocken und ziemlich mild. Morgen"}}"""))
    }

    @Test fun freierTextErgibtKeinenSatz() {
        assertNull(strom("Heute bleibt es in Neuenhagen trocken und ziemlich mild. Morgen regnet es."))
    }

    @Test fun escapesWerdenEntschluesselt() {
        assertEquals("Das \"Wetter\" in München bleibt heute trocken und mild.",
            strom("""{"antwort":"Das \"Wetter\" in München bleibt heute trocken und mild. Morgen nicht."}""", 1))
    }

    @Test fun kurzeOderHalboffeneSaetzeWarten() {
        assertNull(strom("""{"antwort": "Ja. Klar. Gut."}"""))
        assertNull(strom("""{"antwort": "Der **wichtigste Termin ist heute um fünfzehn Uhr. Dann folgt"""))
    }
}
