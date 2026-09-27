package de.frank.wecker

import org.junit.Assert.*
import org.junit.Test

class FreischaltungTest {
    private val tag = 24L * 60 * 60 * 1000
    private val installiert = 1_800_000_000_000L

    // ---- Testzeitraum ----

    @Test fun frischInstalliertSiebenTageVollerUmfang() {
        val stand = Testzeitraum.berechne(installiert, null, installiert + 1000, 0)
        assertTrue(stand.aktiv)
        assertEquals(7, stand.restTage)
        assertEquals(installiert + 7 * tag, stand.ende)
    }

    @Test fun nachSiebenTagenBeendetGenauAnDerGrenze() {
        assertTrue(Testzeitraum.berechne(installiert, installiert, installiert + 7 * tag - 1, 0).aktiv)
        val ende = Testzeitraum.berechne(installiert, installiert, installiert + 7 * tag, 0)
        assertFalse(ende.aktiv)
        assertEquals(0, ende.restTage)
    }

    @Test fun gesicherterAnkerGewinntNachNeuinstallation() {
        // Neuinstallation nach 10 Tagen: firstInstallTime springt, der per Backup zurückgeholte Anker nicht.
        val neu = installiert + 10 * tag
        assertFalse(Testzeitraum.berechne(neu, installiert, neu + 1000, 0).aktiv)
    }

    @Test fun zurueckgestellteUhrVerlaengertNichts() {
        val hoechste = installiert + 8 * tag
        val stand = Testzeitraum.berechne(installiert, installiert, installiert + 2 * tag, hoechste)
        assertFalse(stand.aktiv)
        assertEquals(hoechste, stand.wirksameZeit)
    }

    // ---- Kauf: nur bestätigte Käufe schalten frei ----

    private fun kauf(gekauft: Boolean = true, ausstehend: Boolean = false, bestaetigt: Boolean = true, produkt: String = Freischaltung.PRODUKT) =
        KaufLogik.Kauf(listOf(produkt), gekauft, ausstehend, bestaetigt, "token-$gekauft-$bestaetigt")

    @Test fun nurGekauftUndBestaetigtSchaltetFrei() {
        assertEquals(KaufLogik.Zustand.GEKAUFT, KaufLogik.bewerte(listOf(kauf()), Freischaltung.PRODUKT).first)
        val (zustand, offen) = KaufLogik.bewerte(listOf(kauf(bestaetigt = false)), Freischaltung.PRODUKT)
        assertEquals(KaufLogik.Zustand.BESTAETIGUNG_NOETIG, zustand)
        assertEquals(1, offen.size)
    }

    @Test fun ausstehendeZahlungSchaltetNichtFrei() {
        val (zustand, offen) = KaufLogik.bewerte(listOf(kauf(gekauft = false, ausstehend = true, bestaetigt = false)), Freischaltung.PRODUKT)
        assertEquals(KaufLogik.Zustand.AUSSTEHEND, zustand)
        assertTrue(offen.isEmpty())
    }

    @Test fun fremdesProduktZaehltNicht() {
        assertEquals(KaufLogik.Zustand.KEINER, KaufLogik.bewerte(listOf(kauf(produkt = "etwas_anderes")), Freischaltung.PRODUKT).first)
        assertEquals(KaufLogik.Zustand.KEINER, KaufLogik.bewerte(emptyList(), Freischaltung.PRODUKT).first)
    }

    @Test fun darfBearbeitenMitKaufAuchNachDemTest() {
        val abgelaufen = Testzeitraum.berechne(installiert, installiert, installiert + 30 * tag, 0)
        assertFalse(FreischaltungsZustand(abgelaufen, gekauft = false).darfBearbeiten)
        assertTrue(FreischaltungsZustand(abgelaufen, gekauft = true).darfBearbeiten)
        assertEquals("genialer_wecker_lifetime", Freischaltung.PRODUKT)
    }
}
