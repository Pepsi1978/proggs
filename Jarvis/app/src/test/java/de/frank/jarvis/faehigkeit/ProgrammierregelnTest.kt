package de.frank.jarvis.faehigkeit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgrammierregelnTest {
    private val regeln = "FRANKS PROGRAMMIERREGELN (…):\n[1] Vor jeder Änderung Projekt und Paketkennung prüfen.\n[2] Nie einen eigenen Keystore anlegen.\n"
    private val pc = "FORTSCHRITT (AUFTRAG-ID: ABCDE): PC"
    private val cloud = "FORTSCHRITT (AUFTRAG-ID: ABCDE): Cloud"

    @Test fun programmierregelnSindEigeneDateiImKontextUndAlsArtMerkbar() {
        // merkKontext() baut aus genau dieser Liste: Jarvis' eigene Modelle (Chat, Agenten) haben die Programmierregeln vor Augen.
        assertTrue(Programmierregeln in merkdateien)
        assertEquals(listOf(Regeln, Ziele, UeberFrank, Programmierregeln, StilRegeln, Entscheidungen, Symbiose), merkdateien)
        assertEquals(Programmierregeln, Merkart.von("programmierregel")?.datei)
        // Die bestehenden Dateien bleiben unverändert erreichbar.
        assertEquals(Regeln, Merkart.von("regel")?.datei)
        assertEquals(Ziele, Merkart.von("ziel")?.datei)
        assertEquals(UeberFrank, Merkart.von("info")?.datei)
    }

    @Test fun listeGibtJedenEintragVollstaendigWieder() {
        val lang = "Beim Programmieren immer " + "sehr ".repeat(250) + "gründlich bleiben."
        val text = Programmierregeln.liste(listOf(Regel(2, "Zweite Regel.", ""), Regel(1, lang, "")))
        assertEquals("[1] $lang\n[2] Zweite Regel.", text)
    }

    @Test fun pcUndCloudBekommenNurAuftragUndFortschrittOhneProgrammierregeln() {
        val marke = "TESTEINTRAG-E838F-ZEBRA"
        for (fortschritt in listOf(pc, cloud)) {
            val text = ProgrammierFaehigkeit.auftragMitFortschritt("Baue einen Filter in Geniale Aufgaben ein.", fortschritt)
            assertEquals("Baue einen Filter in Geniale Aufgaben ein.\n\n$fortschritt", text)
            assertFalse(text.contains(marke))
            assertFalse(text.contains("PROGRAMMIERREGELN"))
        }
    }

    @Test fun pluginUndLiveGedaechtnisLassenNurDieProgrammierregelnWeg() {
        // Anleitung, nachgereichte Blöcke und jarvis_kontext lesen auch Programmier-Sitzungen: dort nie die Programmierregeln.
        assertEquals(listOf(Regeln, Ziele, UeberFrank, StilRegeln, Entscheidungen, Symbiose), merkKontextAusser(merkdateien, ohneSymbiose = false))
        assertEquals(listOf(Regeln, Ziele, UeberFrank, StilRegeln, Entscheidungen), merkKontextAusser(merkdateien, ohneSymbiose = true))
    }

    @Test fun zuLangerAuftragWirdGekuerztNieDerFortschritt() {
        val text = ProgrammierFaehigkeit.auftragMitFortschritt("A".repeat(80_000), cloud)
        assertTrue(text.length <= ProgrammierFaehigkeit.HOECHSTENS)
        assertTrue(text.endsWith(cloud))
        assertTrue(text.contains("(gekürzt)"))
    }
}
