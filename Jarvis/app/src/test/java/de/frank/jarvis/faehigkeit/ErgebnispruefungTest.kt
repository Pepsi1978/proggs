package de.frank.jarvis.faehigkeit

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErgebnispruefungTest {
    private val soll = listOf(Ergebnispruefung.Soll("uhrzeit", Ergebnispruefung.uhrzeit("7:30"), "Uhrzeit"), Ergebnispruefung.Soll("aktiv", "true", "Eingeschaltet"))

    @Test fun bestaetigtOhneZusatzaufruf() = runBlocking {
        var gelesen = 0
        val antwort = JSONObject().put("wecker", JSONObject().put("uhrzeit", "07:30").put("aktiv", true))
        val r = Ergebnispruefung.pruefe(antwort, "wecker", soll, "wecker_lesen", { gelesen++; null }) { "ok" }
        assertFalse(r.fehler)
        assertEquals(0, gelesen)
    }

    @Test fun abweichungUnbestaetigt() = runBlocking {
        val antwort = JSONObject().put("wecker", JSONObject().put("uhrzeit", "08:30").put("aktiv", true))
        val r = Ergebnispruefung.pruefe(antwort, "wecker", soll, "wecker_lesen") { "ok" }
        assertTrue(r.fehler)
        assertTrue(r.text.startsWith("NICHT BESTÄTIGT"))
        assertTrue("Nicht blind erneut" in r.text)
    }

    @Test fun zeitlimitLiestGenauEinmalNach() = runBlocking {
        var gelesen = 0
        val antwort = JSONObject().put("fehler", "antwortet nicht rechtzeitig").put(AppBruecke.ZEITLIMIT, true)
        val r = Ergebnispruefung.pruefe(antwort, "wecker", soll, "wecker_lesen", { gelesen++; JSONObject().put("uhrzeit", "07:30").put("aktiv", true) }) { "ok" }
        assertFalse(r.fehler)
        assertEquals(1, gelesen)
        assertTrue("Nachlesen" in r.text)
    }

    @Test fun antwortOhneZustandUnbestaetigt() = runBlocking {
        val r = Ergebnispruefung.pruefe(JSONObject(), "aufgabe", emptyList(), "aufgaben_lesen") { "ok" }
        assertTrue(r.fehler)
        assertTrue(r.text.startsWith("NICHT BESTÄTIGT"))
    }

    @Test fun klarerFehlerBleibtFehler() = runBlocking {
        val r = Ergebnispruefung.pruefe(JSONObject().put("fehler", "Keine Aufgabe mit dieser id."), "aufgabe", emptyList(), "aufgaben_lesen") { "ok" }
        assertEquals("Keine Aufgabe mit dieser id.", r.text)
    }

    @Test fun eingabenNormalisiert() {
        assertEquals("18:00", Ergebnispruefung.uhrzeit("18 Uhr"))
        assertEquals("", Ergebnispruefung.uhrzeit("ohne"))
        assertNull(Ergebnispruefung.uhrzeit("abends"))
        assertEquals("2026-11-08", Ergebnispruefung.datum("morgen", java.time.LocalDate.of(2026, 11, 7)))
    }

    /** Nachweis der Zusatzlatenz im Normalfall: reiner Vergleich im Speicher, weit unter einer Millisekunde je Aufruf. */
    @Test fun latenzImNormalfall() = runBlocking {
        val antwort = JSONObject().put("wecker", JSONObject().put("uhrzeit", "07:30").put("aktiv", true))
        repeat(200) { Ergebnispruefung.pruefe(antwort, "wecker", soll, "wecker_lesen") { "ok" } }
        val n = 2_000
        val beginn = System.nanoTime()
        repeat(n) { Ergebnispruefung.pruefe(antwort, "wecker", soll, "wecker_lesen") { "ok" } }
        val mikro = (System.nanoTime() - beginn) / 1_000.0 / n
        println("Ergebnisprüfung: %.1f µs je Aufruf".format(mikro))
        assertTrue("zu langsam: $mikro µs", mikro < 1_000)
    }
}

class GedaechtnisGeltungTest {
    private val faehigkeit get() = RegelnFaehigkeit::class.java

    @Test fun erlaubnisErkannt() {
        assertTrue(Merkdatei.istErlaubnis("Hier ohne Rückfrage deployen."))
        assertTrue(Merkdatei.istErlaubnis("Jarvis soll Wecker ohne zu fragen stellen."))
        assertFalse(Merkdatei.istErlaubnis("Bei Fragen nach dem Schlaf immer auch HRV nennen."))
    }

    @Test fun situativErkanntDauerhaftNicht() {
        assertEquals("jetzt", Merkdatei.situativ("Mach das jetzt schnell."))
        assertEquals("hier", Merkdatei.situativ("Hier ohne Rückfrage."))
        assertNull(Merkdatei.situativ("Ab jetzt Antworten in Stichpunkten."))
    }

    @Test fun alteErlaubnisOhneGeltungKeinePauschalfreigabe() {
        val alt = Regel(3, "Programmieraufträge ohne Rückfrage deployen.", "01.10.2026")
        assertTrue("keine pauschale Freigabe" in alt.zusatz())
        val begrenzt = alt.copy(geltung = "nur Programmieraufträge an Jarvis", bis = "01.01.2020")
        assertTrue("ERLAUBNIS nur hierfür" in begrenzt.zusatz())
        assertTrue(begrenzt.abgelaufen())
        assertEquals("", Regel(1, "Antworte kurz.", "").zusatz())
    }

    @Test fun symbioseOhneZusatz() {
        val r = Regel(1, "Frag nicht nach, wenn alles klar ist.", "17.10.2026")
        assertFalse("Erlaubnis" in Symbiose.liste(listOf(r)))
        assertNotNull(faehigkeit)
    }
}
