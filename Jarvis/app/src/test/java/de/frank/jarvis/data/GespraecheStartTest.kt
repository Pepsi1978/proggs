package de.frank.jarvis.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Startzeit eines Programmierauftrags in der Leiste: fest, der früheste gewinnt, nie erfunden. */
class GespraecheStartTest {
    @Test fun fruehesterOhneNullen() {
        assertEquals(100L, Gespraeche.fruehesterStart(300L, 0L, 100L, 200L))
        assertEquals(0L, Gespraeche.fruehesterStart(0L, 0L))
        assertEquals(0L, Gespraeche.fruehesterStart())
    }

    @Test fun spaetererAuftragAendertNichts() = assertNull(Gespraeche.neuerStart(gespeichert = 100L, bekannt = 500L))
    @Test fun gleicherStartAendertNichts() = assertNull(Gespraeche.neuerStart(gespeichert = 100L, bekannt = 100L))
    @Test fun ohneBekanntenStartNichts() = assertNull(Gespraeche.neuerStart(gespeichert = 0L, bekannt = 0L))
    @Test fun ersterStartWirdGespeichert() = assertEquals(500L, Gespraeche.neuerStart(gespeichert = 0L, bekannt = 500L))
    @Test fun frueherAusDemBuchKorrigiert() = assertEquals(50L, Gespraeche.neuerStart(gespeichert = 100L, bekannt = 50L))

    @Test fun mehrereAuftraegeJeEigenerStart() {
        val starts = Gespraeche.fruehesteStarts(listOf("a" to 300L, "b" to 200L, "a" to 100L, "c" to 0L, "" to 50L, "b" to 0L))
        assertEquals(mapOf("a" to 100L, "b" to 200L, "c" to 0L), starts)
    }
}
