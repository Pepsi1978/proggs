package de.frank.jarvis.data

import de.frank.jarvis.faehigkeit.ProgrammierChat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammierZuordnungTest {

    private fun gespraech(vararg nachrichten: JSONObject, markiert: Boolean = false) = JSONObject().put("id", "x").put("titel", "T")
        .put("nachrichten", JSONArray().apply { nachrichten.forEach { put(it) } })
        .apply { if (markiert) put("programmierung", true) }

    private fun frank(text: String) = JSONObject().put("mir", true).put("text", text)
    private fun jarvis(text: String, art: String? = null) = JSONObject().put("mir", false).put("text", text).apply { art?.let { put("art", it) } }

    @Test fun normalesGespraechBleibtNormal() {
        assertFalse(Gespraeche.istProgrammierung(gespraech(frank("Wie ist das Wetter?"), jarvis("Sonnig."))))
        assertFalse(Gespraeche.istProgrammierung(gespraech()))
    }

    @Test fun markierungZaehlt() {
        assertTrue(Gespraeche.istProgrammierung(gespraech(frank("Hallo"), markiert = true)))
    }

    @Test fun altesAuftragsGespraechAmVorspannErkannt() {
        assertTrue(Gespraeche.istProgrammierung(gespraech(frank(ProgrammierChat.AUFTRAG_VORSPANN + "Jarvis):\n\nBau etwas"))))
        assertTrue(Gespraeche.istProgrammierung(gespraech(frank("Über das Jarvis-Plugin (ChatGPT/MCP) an den Programmierer (Jarvis):\n\nBau etwas"))))
    }

    @Test fun normalesGespraechMitAuftragAnMeldungenErkannt() {
        for (art in listOf(ProgrammierChat.ART_STATUS, ProgrammierChat.ART_BERICHT, "rueckfrage")) {
            assertTrue(art, Gespraeche.istProgrammierung(gespraech(frank("Lass das programmieren"), jarvis("Jarvis · ⚙️ Gestartet", art))))
        }
    }

    @Test fun andereArtenMachenKeinProgrammiergespraech() {
        for (art in listOf("fehler", "abgebrochen", ProgrammierChat.ART_BILD, ProgrammierChat.ART_AGENT)) {
            assertFalse(art, Gespraeche.istProgrammierung(gespraech(frank("Hallo"), jarvis("…", art))))
        }
    }
}
