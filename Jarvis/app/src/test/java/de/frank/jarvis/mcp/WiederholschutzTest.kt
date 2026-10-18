package de.frank.jarvis.mcp

import de.frank.jarvis.faehigkeit.Ergebnis
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WiederholschutzTest {
    private var jetzt = 0L
    private val schutz = Wiederholschutz(fensterMs = 120_000L, uhr = { jetzt })

    @Test fun gleicherAufrufKurzDanachWirdNichtErneutAusgefuehrt() = runBlocking {
        val laeufe = AtomicInteger()
        val (r1, w1) = schutz.fuehreAus("a") { laeufe.incrementAndGet(); Ergebnis("Aufgabe angelegt") }
        jetzt += 30_000
        val (r2, w2) = schutz.fuehreAus("a") { laeufe.incrementAndGet(); Ergebnis("Aufgabe angelegt") }
        assertEquals(1, laeufe.get())
        assertFalse(w1)
        assertTrue(w2)
        assertEquals(r1.text, r2.text)
    }

    @Test fun laufenderAufrufWirdAbgewartetStattDoppeltGestartet() = runBlocking {
        val laeufe = AtomicInteger()
        val frei = CompletableDeferred<Unit>()
        val erster = async { schutz.fuehreAus("b") { laeufe.incrementAndGet(); frei.await(); Ergebnis("fertig") } }
        yield()
        val zweiter = async { schutz.fuehreAus("b") { laeufe.incrementAndGet(); Ergebnis("doppelt") } }
        yield()
        frei.complete(Unit)
        assertEquals("fertig", erster.await().first.text)
        assertEquals("fertig", zweiter.await().first.text)
        assertTrue(zweiter.await().second)
        assertEquals(1, laeufe.get())
    }

    @Test fun nachFehlerUndNachAblaufDarfWiederholtWerden() = runBlocking {
        val laeufe = AtomicInteger()
        schutz.fuehreAus("c") { laeufe.incrementAndGet(); Ergebnis("kaputt", fehler = true) }
        schutz.fuehreAus("c") { laeufe.incrementAndGet(); Ergebnis("ok") }
        jetzt += 121_000
        schutz.fuehreAus("c") { laeufe.incrementAndGet(); Ergebnis("ok") }
        assertEquals(3, laeufe.get())
    }

    @Test fun andererAufrufDazwischenHebtDenSchutzAuf() = runBlocking {
        val laeufe = AtomicInteger()
        schutz.fuehreAus("aufgabe_anlegen|X") { laeufe.incrementAndGet(); Ergebnis("angelegt") }
        schutz.fuehreAus("aufgabe_loeschen|X") { laeufe.incrementAndGet(); Ergebnis("gelöscht") }
        val (_, wiederholt) = schutz.fuehreAus("aufgabe_anlegen|X") { laeufe.incrementAndGet(); Ergebnis("angelegt") }
        assertFalse(wiederholt)
        assertEquals(3, laeufe.get())
    }

    @Test fun aufrufAusAnderemBereichHebtDenSchutzNichtAuf() = runBlocking {
        val laeufe = AtomicInteger()
        schutz.fuehreAus("mail_senden|M") { laeufe.incrementAndGet(); Ergebnis("gesendet") }
        schutz.fuehreAus("aufgabe_anlegen|A") { laeufe.incrementAndGet(); Ergebnis("angelegt") }
        val (_, wiederholt) = schutz.fuehreAus("mail_senden|M") { laeufe.incrementAndGet(); Ergebnis("doppelt gesendet") }
        assertTrue(wiederholt)
        assertEquals(2, laeufe.get())
    }

    @Test fun zweiVerschiedeneAufrufeDesselbenWerkzeugsBleibenBeideGeschuetzt() = runBlocking {
        val laeufe = AtomicInteger()
        schutz.fuehreAus("aufgabe_anlegen|A") { laeufe.incrementAndGet(); Ergebnis("A angelegt") }
        schutz.fuehreAus("aufgabe_anlegen|B") { laeufe.incrementAndGet(); Ergebnis("B angelegt") }
        // Die Antwort zu A ging verloren, A kommt noch einmal: nicht doppelt.
        val (r, wiederholt) = schutz.fuehreAus("aufgabe_anlegen|A") { laeufe.incrementAndGet(); Ergebnis("A doppelt") }
        assertTrue(wiederholt)
        assertEquals("A angelegt", r.text)
        assertEquals(2, laeufe.get())
    }

    @Test fun schluesselIgnoriertReihenfolgeUndRandLeerraum() {
        val a = JSONObject().put("titel", "Milch kaufen").put("datum", "2026-10-18")
        val b = JSONObject().put("datum", "2026-10-18").put("titel", " Milch kaufen ")
        assertEquals(Wiederholschutz.schluessel("aufgabe_anlegen", a), Wiederholschutz.schluessel("aufgabe_anlegen", b))
        assertNotEquals(Wiederholschutz.schluessel("aufgabe_anlegen", a), Wiederholschutz.schluessel("idee_speichern", a))
    }
}
