package de.frank.jarvis.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoSperreTest {
    /** Ton offen bis [ende], danach nur noch Prüfungen ohne Ton; letzter hörbarer Ton beim Ende geschrieben. */
    private fun nachEnde(s: EchoSperre, ende: Long, nach: Long, bluetooth: Boolean): Boolean {
        s.zu(ende, true, ende, bluetooth)
        return s.zu(ende + nach, false, ende, bluetooth)
    }

    // ---- Handy-Lautsprecher: unverändert wie gemessen (Nachlauf 400 ms) ----
    @Test fun lautsprecherNachlaufWieBisher() {
        assertTrue(nachEnde(EchoSperre(), 10_000, 399, bluetooth = false))
        assertFalse(nachEnde(EchoSperre(), 10_000, 400, bluetooth = false))
    }

    @Test fun lautsprecherIgnoriertGemesseneLatenz() {
        val s = EchoSperre().apply { latenzMessen(kopf = 48_000, ausgegeben = 24_000, rate = 24_000) }
        assertFalse(nachEnde(s, 10_000, 400, bluetooth = false))
    }

    // ---- Bluetooth: Die Sperre wartet das hörbare Ende ab ----
    @Test fun bluetoothOhneMessungMindestlatenz() {
        val voll = EchoSperre.NACHLAUF_MS + EchoSperre.BLOCK_MS + EchoSperre.BLUETOOTH_MIN_LATENZ_MS
        assertTrue(nachEnde(EchoSperre(), 10_000, voll - 1, bluetooth = true))
        assertFalse(nachEnde(EchoSperre(), 10_000, voll, bluetooth = true))
    }

    /** Befund im Auto: Die letzten 0,6–0,9 s kamen nach den alten 400 ms noch ins Mikrofon. Jetzt ist es dann noch zu. */
    @Test fun bluetoothAntwortendeBleibtGesperrt() {
        for (nach in listOf(400L, 700L, 1_000L, 1_300L)) assertTrue("nach $nach ms", nachEnde(EchoSperre(), 10_000, nach, bluetooth = true))
    }

    @Test fun bluetoothGroessereMessungZaehlt() {
        // Kopf 1,5 s vor dem ausgegebenen Frame (24 kHz).
        val s = EchoSperre().apply { latenzMessen(kopf = 60_000, ausgegeben = 24_000, rate = 24_000) }
        assertEquals(1_500L, s.latenzMs)
        assertTrue(nachEnde(s, 10_000, 1_999, bluetooth = true))
        assertFalse(nachEnde(s, 10_000, 2_000, bluetooth = true))
    }

    @Test fun kleinereMessungUnterschreitetMindestlatenzNicht() {
        val s = EchoSperre().apply { latenzMessen(kopf = 31_200, ausgegeben = 24_000, rate = 24_000) }
        assertEquals(300L, s.latenzMs)
        assertEquals(EchoSperre.BLUETOOTH_MIN_LATENZ_MS, s.bluetoothLatenzMs())
    }

    @Test fun fehlmessungenZaehlenNicht() {
        val s = EchoSperre()
        s.latenzMessen(kopf = 1_000, ausgegeben = 2_000, rate = 24_000)
        s.latenzMessen(kopf = 200_000, ausgegeben = 0, rate = 24_000)
        assertEquals(-1L, s.latenzMs)
    }

    @Test fun messungWirdGeglaettet() {
        val s = EchoSperre()
        s.latenzMessen(kopf = 48_000, ausgegeben = 24_000, rate = 24_000)
        s.latenzMessen(kopf = 24_000 + 1_200 * 24, ausgegeben = 24_000, rate = 24_000)
        assertEquals(1_050L, s.latenzMs)
    }

    /** Nach dem Leeren (Antippen, Dazwischenreden) ist kein Ton mehr offen; was auf der Funkstrecke war, ist trotzdem noch zu hören. */
    @Test fun nachLeerenWartetBluetoothLatenz() {
        val s = EchoSperre()
        s.zu(10_000, true, 10_000, true)
        assertTrue(s.zu(10_100, false, 10_000, true))
        assertTrue(s.zu(11_000, false, 10_000, true))
        assertFalse(s.zu(11_500, false, 10_000, true))
    }

    @Test fun deckelOeffnetBeiHaengenderWiedergabe() {
        val s = EchoSperre()
        // Ton gilt dauernd als offen, aber seit über 3 s wurde nichts Hörbares geschrieben.
        assertTrue(s.zu(10_000, true, 7_100, false))
        assertFalse(s.zu(10_000, true, 7_000, false))
        assertTrue(s.zu(10_000, true, 6_100, true))
        assertFalse(s.zu(10_000, true, 6_000, true))
    }

    // ---- Echoverdacht (nur Diagnose): echte Fälle vom 20.10.2026 ----
    @Test fun echteEchosAusDemGespraech() {
        assertTrue(EchoSperre.echoVerdacht("bin für dich da.", "Hallo Frank, ich bin für dich da."))
        assertTrue(EchoSperre.echoVerdacht("noch etwas?", "Gibt es dazu noch etwas?"))
        assertTrue(EchoSperre.echoVerdacht("direkt nach", "Moment, ich schaue direkt nach."))
    }

    /** Wortlaut der Antworten nachgestellt, nur die Echos selbst sind belegt. */
    @Test fun weitereGemeldeteEchos() {
        assertTrue(EchoSperre.echoVerdacht("Garnelen hinzu", "Alles klar, ich füge dem Nachtrag die Garnelen hinzu."))
        assertTrue(EchoSperre.echoVerdacht("Garnelen meinst", "Welche Garnelen meinst du?"))
        assertTrue(EchoSperre.echoVerdacht("weiterleite", "Sag Bescheid, wenn ich das an Jarvis weiterleite."))
        assertTrue(EchoSperre.echoVerdacht("übermittelt", "Dein Nachtrag wurde übermittelt."))
    }

    @Test fun echteAntwortenSindKeinEcho() {
        assertFalse(EchoSperre.echoVerdacht("Ja", "Soll ich das eintragen, ja oder nein?"))
        assertFalse(EchoSperre.echoVerdacht("nein", "Soll ich das eintragen, ja oder nein?"))
        assertFalse(EchoSperre.echoVerdacht("Okay", "Okay, Moment."))
        assertFalse(EchoSperre.echoVerdacht("Was habe ich morgen für Aufgaben?", "Hallo Frank, ich bin für dich da."))
        assertFalse(EchoSperre.echoVerdacht("Tee", "Möchtest du Kaffee oder Tee?"))
        assertFalse(EchoSperre.echoVerdacht("da bin für dich", "Hallo Frank, ich bin für dich da."))
        assertFalse(EchoSperre.echoVerdacht("", "Hallo Frank."))
    }
}
