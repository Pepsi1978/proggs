package de.frank.jarvis.faehigkeit

import java.time.LocalDate
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GedaechtnisGeltungTest {
    private val heute = LocalDate.of(2026, 11, 7)
    private fun pruefe(art: Merkart, text: String, geltung: String? = null, bis: String? = null) =
        pruefeGeltung(art, text, JSONObject().apply { geltung?.let { put("geltung", it) }; bis?.let { put("bis", it) } }, heute)

    @Test fun erlaubnisErkannt() {
        assertTrue(Merkdatei.istErlaubnis("Hier ohne Rückfrage deployen."))
        assertTrue(Merkdatei.istErlaubnis("Jarvis soll Wecker ohne zu fragen stellen."))
        assertTrue(Merkdatei.istErlaubnis("Frag nicht nach, sondern antworte direkt."))
        assertFalse(Merkdatei.istErlaubnis("Bei Fragen nach dem Schlaf immer auch HRV nennen."))
    }

    @Test fun verboteSindKeineErlaubnis() {
        assertFalse(Merkdatei.istErlaubnis("Niemals ungefragt den Spielstand verraten."))
        assertFalse(Merkdatei.istErlaubnis("Lösche nie ohne Rückfrage einen Termin."))
        assertFalse(Merkdatei.istErlaubnis("Du darfst nie Mails ohne mein Okay senden."))
        assertNull(pruefe(Merkart.REGEL, "Lösche nie ohne Rückfrage einen Termin."))
    }

    @Test fun situativUndDauerhaft() {
        assertEquals("jetzt", Merkdatei.situativ("Mach das jetzt schnell."))
        assertEquals("hier", Merkdatei.situativ("Hier ohne Rückfrage."))
        assertNull(Merkdatei.situativ("Ab jetzt Antworten in Stichpunkten."))
        assertNull(Merkdatei.situativ("Wenn ich frage, was heute ansteht, nenne Dienst und Wetter."))
        assertNull(Merkdatei.situativ("Antworte jetzt immer in Stichpunkten."))
    }

    @Test fun erlaubnisBrauchtGeltung() {
        assertNotNull(pruefe(Merkart.REGEL, "Programmieraufträge ohne Rückfrage deployen."))
        assertNull(pruefe(Merkart.REGEL, "Programmieraufträge ohne Rückfrage deployen.", geltung = "nur Programmieraufträge an Jarvis"))
        assertNotNull(pruefe(Merkart.REGEL, "Programmieraufträge ohne Rückfrage deployen.", geltung = GELTUNG_AUFHEBEN))
        // Aus Vorlieben folgen keine Rechte, auch mit Geltung nicht.
        assertNotNull(pruefe(Merkart.INFO, "Frank mag es, wenn Jarvis ohne Rückfrage handelt.", geltung = "Wecker"))
    }

    @Test fun situativesNurMitGeltungOderFrist() {
        assertNotNull(pruefe(Merkart.REGEL, "Hier direkt deployen und installieren."))
        assertNull(pruefe(Merkart.REGEL, "Hier direkt deployen und installieren.", geltung = "Auftrag B4ML9"))
        assertNull(pruefe(Merkart.REGEL, "Vorerst kürzere Antworten geben.", bis = "30.11.2026"))
    }

    @Test fun fristPruefung() {
        assertNotNull(pruefe(Merkart.REGEL, "Vorerst kürzere Antworten geben.", bis = "03.11.2026"))
        assertNotNull(pruefe(Merkart.REGEL, "Kürzere Antworten geben.", bis = "irgendwann"))
        assertNull(pruefe(Merkart.REGEL, "Kürzere Antworten geben.", bis = BIS_AUFHEBEN))
    }

    @Test fun alteErlaubnisOhneGeltungKeinePauschalfreigabe() {
        val alt = Regel(3, "Programmieraufträge ohne Rückfrage deployen.", "01.10.2026")
        assertTrue("keine pauschale Freigabe" in alt.zusatz())
        val begrenzt = alt.copy(geltung = "nur Programmieraufträge an Jarvis", bis = "01.01.2020")
        assertTrue("ERLAUBNIS nur hierfür" in begrenzt.zusatz())
        assertTrue(begrenzt.abgelaufen())
        assertEquals("", Regel(1, "Antworte kurz.", "").zusatz())
    }

    @Test fun symbioseUndRohlisteOhneZusatz() {
        val r = Regel(1, "Frag nicht nach, wenn alles klar ist.", "17.10.2026", quelle = "Gespräch")
        assertFalse("Erlaubnis" in Symbiose.liste(listOf(r.copy(quelle = ""))))
        assertEquals("[1] Frag nicht nach, wenn alles klar ist.", Regeln.listeRoh(listOf(r)))
    }
}
